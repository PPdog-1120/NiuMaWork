package com.overtime.tracker.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overtime.tracker.OvertimeTrackerApp
import com.overtime.tracker.data.AttendanceRecord
import com.overtime.tracker.data.UserSettings
import com.overtime.tracker.util.DateUtils
import com.overtime.tracker.util.DataExporter
import com.overtime.tracker.util.OvertimeCalculator
import com.overtime.tracker.widget.WidgetClockReceiver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * 主页 ViewModel（v1.3 更新：净加班、弹性归一化）
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OvertimeTrackerApp
    private val dao = app.database.attendanceDao()
    private val leaveDao = app.database.leaveDao()
    private val settingsStore = app.settingsDataStore

    /** 用户设置 */
    val settings: StateFlow<UserSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    /** 今日打卡记录 */
    private val _todayRecord = MutableStateFlow<AttendanceRecord?>(null)
    val todayRecord: StateFlow<AttendanceRecord?> = _todayRecord.asStateFlow()

    /** 打卡提示消息 */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** 本月累计加班（分钟） */
    private val _monthOvertime = MutableStateFlow(0)
    val monthOvertime: StateFlow<Int> = _monthOvertime.asStateFlow()

    /** 本月累计请假（分钟） */
    private val _monthLeave = MutableStateFlow(0)
    val monthLeave: StateFlow<Int> = _monthLeave.asStateFlow()

    /** 本月净加班（分钟）= 加班 - 请假 */
    private val _monthNetOvertime = MutableStateFlow(0)
    val monthNetOvertime: StateFlow<Int> = _monthNetOvertime.asStateFlow()

    /** 本月工作日加班（分钟） */
    private val _monthWorkdayOvertime = MutableStateFlow(0)
    val monthWorkdayOvertime: StateFlow<Int> = _monthWorkdayOvertime.asStateFlow()

    /** 本月休息日加班（分钟） */
    private val _monthRestDayOvertime = MutableStateFlow(0)
    val monthRestDayOvertime: StateFlow<Int> = _monthRestDayOvertime.asStateFlow()

    /** 本年累计加班（分钟） */
    private val _yearOvertime = MutableStateFlow(0)
    val yearOvertime: StateFlow<Int> = _yearOvertime.asStateFlow()

    /** 本年累计请假（分钟） */
    private val _yearLeave = MutableStateFlow(0)
    val yearLeave: StateFlow<Int> = _yearLeave.asStateFlow()

    /** 本年净加班（分钟） */
    private val _yearNetOvertime = MutableStateFlow(0)
    val yearNetOvertime: StateFlow<Int> = _yearNetOvertime.asStateFlow()

    /** 是否为休息日 */
    val isRestDay: Boolean
        get() = !settings.value.isWorkDay(DateUtils.today())

    /** Widget 打卡后自动刷新 UI */
    private val widgetClockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            refreshAccumulated()
            viewModelScope.launch {
                val record = dao.getByDate(DateUtils.today())
                _todayRecord.value = record
            }
        }
    }

    init {
        viewModelScope.launch {
            dao.getByDateFlow(DateUtils.today()).collect { record ->
                _todayRecord.value = record
            }
        }
        refreshAccumulated()

        val filter = IntentFilter(WidgetClockReceiver.ACTION_WIDGET_CLOCK_CHANGED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(widgetClockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(widgetClockReceiver, filter)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { getApplication<Application>().unregisterReceiver(widgetClockReceiver) } catch (_: Exception) { }
    }

    /** 上班打卡 */
    fun clockIn() {
        viewModelScope.launch {
            val today = DateUtils.today()
            val existing = dao.getByDate(today)

            if (existing?.clockInTime != null) {
                _message.value = "今日已打过上班卡"
                return@launch
            }

            val nowTime = DateUtils.nowTime()
            val currentSettings = settings.value
            val restDay = !currentSettings.isWorkDay(today)

            val record = AttendanceRecord(
                id = existing?.id ?: 0,
                date = today,
                clockInTime = nowTime,
                clockOutTime = existing?.clockOutTime,
                isRestDay = restDay,
                overtimeMinutes = 0,
                type = if (restDay) AttendanceRecord.OVERTIME_TYPE_REST_DAY else AttendanceRecord.OVERTIME_TYPE_WORKDAY,
                source = AttendanceRecord.SOURCE_REALTIME,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            dao.upsert(record)
            _message.value = "上班打卡成功 ✓"
        }
    }

    /** 下班打卡 */
    fun clockOut() {
        viewModelScope.launch {
            val today = DateUtils.today()
            val existing = dao.getByDate(today)

            if (existing?.clockInTime == null) {
                _message.value = "请先打上班卡"
                return@launch
            }
            if (existing.clockOutTime != null) {
                _message.value = "今日已打过下班卡"
                return@launch
            }

            val nowTime = DateUtils.nowTime()
            val currentSettings = settings.value

            val overtime = OvertimeCalculator.calculateOvertime(
                existing.clockInTime, nowTime, currentSettings, existing.isRestDay
            )

            if (overtime == -1) {
                _message.value = "检测到跨天打卡，请在设置中开启「跨天支持」"
                return@launch
            }
            if (overtime == -2) {
                _message.value = "打卡数据异常，请重新打卡"
                return@launch
            }

            val record = existing.copy(clockOutTime = nowTime, overtimeMinutes = overtime)
            dao.upsert(record)
            _message.value = "下班打卡成功 ✓"
            refreshAccumulated()
        }
    }

    /** 撤销打卡（5分钟内） */
    fun undoClockIn() {
        viewModelScope.launch {
            val today = DateUtils.today()
            val existing = dao.getByDate(today) ?: return@launch

            if (existing.clockInTime == null) return@launch
            if (existing.clockOutTime != null) {
                _message.value = "已打下班卡，无法撤销"
                return@launch
            }

            val elapsedMs = System.currentTimeMillis() - existing.createdAt
            val elapsedMinutes = elapsedMs / (60 * 1000)
            if (elapsedMinutes > 5) {
                _message.value = "已超过5分钟，无法撤销"
                return@launch
            }

            dao.deleteByDate(today)
            _todayRecord.value = null
            _message.value = "已撤销上班打卡"
        }
    }

    fun clearMessage() { _message.value = null }

    /** 刷新累计加班数据 */
    fun refreshAccumulated() {
        viewModelScope.launch {
            val (monthStart, monthEnd) = DateUtils.thisMonthRange()
            val (yearStart, yearEnd) = DateUtils.thisYearRange()

            _monthOvertime.value = dao.getTotalOvertimeMinutes(monthStart, monthEnd)
            _monthWorkdayOvertime.value = dao.getWorkdayOvertimeMinutes(monthStart, monthEnd)
            _monthRestDayOvertime.value = dao.getRestDayOvertimeMinutes(monthStart, monthEnd)
            _monthLeave.value = leaveDao.getTotalLeaveMinutes(monthStart, monthEnd)
            _monthNetOvertime.value = OvertimeCalculator.calculateNetOvertime(_monthOvertime.value, _monthLeave.value)

            _yearOvertime.value = dao.getTotalOvertimeMinutes(yearStart, yearEnd)
            _yearLeave.value = leaveDao.getTotalLeaveMinutes(yearStart, yearEnd)
            _yearNetOvertime.value = OvertimeCalculator.calculateNetOvertime(_yearOvertime.value, _yearLeave.value)
        }
    }

    // ══════════════════════════════════════════════════
    //  数据导出 / 导入
    // ══════════════════════════════════════════════════

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun exportData() {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val records = dao.getAllOnce()
                val leaveRecords = leaveDao.getAllOnce()
                if (records.isEmpty() && leaveRecords.isEmpty()) {
                    _exportState.value = ExportState.Error("没有可导出的数据")
                    return@launch
                }
                val context = getApplication<Application>()
                val file = DataExporter.exportToFile(context, records, leaveRecords)
                val shareIntent = DataExporter.createShareIntent(context, file)
                if (shareIntent == null) {
                    _exportState.value = ExportState.Error("无法创建分享，请检查存储权限")
                    return@launch
                }
                _exportState.value = ExportState.Ready(file, shareIntent, records.size + leaveRecords.size)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("导出失败: ${e.message}")
            }
        }
    }

    fun importData(uri: android.net.Uri) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            try {
                val context = getApplication<Application>()
                val result = DataExporter.importFromUri(context, uri)
                when (result) {
                    is DataExporter.ImportResult.Error -> {
                        _importState.value = ImportState.Error(result.message)
                    }
                    is DataExporter.ImportResult.Success -> {
                        if (result.records.isEmpty() && result.leaveRecords.isEmpty()) {
                            _importState.value = ImportState.Error("备份文件中没有记录")
                            return@launch
                        }

                        // 按日期去重：已存在则沿用其 id（更新），不存在则插入
                        var importedCount = 0
                        for (record in result.records) {
                            val existing = dao.getByDate(record.date)
                            val toSave = if (existing != null) record.copy(id = existing.id) else record
                            dao.upsert(toSave)
                            importedCount++
                        }
                        for (leave in result.leaveRecords) {
                            val existing = leaveDao.getByDate(leave.date)
                            val toSave = if (existing != null) leave.copy(id = existing.id) else leave
                            leaveDao.upsert(toSave)
                            importedCount++
                        }

                        refreshAccumulated()
                        _importState.value = ImportState.Success(importedCount)
                    }
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error("导入失败: ${e.message}")
            }
        }
    }

    fun resetExportState() { _exportState.value = ExportState.Idle }
    fun resetImportState() { _importState.value = ImportState.Idle }

    sealed class ExportState {
        data object Idle : ExportState()
        data object Loading : ExportState()
        data class Ready(val file: File, val shareIntent: android.content.Intent, val count: Int) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    sealed class ImportState {
        data object Idle : ImportState()
        data object Loading : ImportState()
        data class Success(val count: Int) : ImportState()
        data class Error(val message: String) : ImportState()
    }
}
