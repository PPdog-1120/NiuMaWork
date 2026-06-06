package com.overtime.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overtime.tracker.OvertimeTrackerApp
import com.overtime.tracker.data.AttendanceRecord
import com.overtime.tracker.data.LeaveRecord
import com.overtime.tracker.data.UserSettings
import com.overtime.tracker.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 历史记录页面 ViewModel（v1.3 更新：支持请假记录）
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OvertimeTrackerApp
    private val dao = app.database.attendanceDao()
    private val leaveDao = app.database.leaveDao()
    private val settingsStore = app.settingsDataStore

    val settings: StateFlow<UserSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    /** 当前月份的打卡记录 */
    val monthRecords: StateFlow<List<AttendanceRecord>> = combine(
        _currentYear, _currentMonth
    ) { year, month -> year to month }.flatMapLatest { (year, month) ->
        val start = DateUtils.firstDayOfMonth(year, month)
        val end = DateUtils.lastDayOfMonth(year, month)
        dao.getBetweenFlow(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前月份的请假记录 */
    val monthLeaveRecords: StateFlow<List<LeaveRecord>> = combine(
        _currentYear, _currentMonth
    ) { year, month -> year to month }.flatMapLatest { (year, month) ->
        val start = DateUtils.firstDayOfMonth(year, month)
        val end = DateUtils.lastDayOfMonth(year, month)
        leaveDao.getBetweenFlow(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 筛选模式 */
    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    val filterMode: StateFlow<FilterMode> = _filterMode.asStateFlow()

    /** 筛选后的记录 */
    val filteredRecords: StateFlow<List<AttendanceRecord>> = combine(
        monthRecords, _filterMode
    ) { records, filter ->
        when (filter) {
            FilterMode.ALL -> records
            FilterMode.WORKDAY -> records.filter { it.type == AttendanceRecord.OVERTIME_TYPE_WORKDAY }
            FilterMode.REST_DAY -> records.filter { it.type == AttendanceRecord.OVERTIME_TYPE_REST_DAY }
            FilterMode.OVERTIME_ONLY -> records.filter { it.overtimeMinutes > 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun previousMonth() {
        if (_currentMonth.value == 1) {
            _currentYear.value -= 1
            _currentMonth.value = 12
        } else {
            _currentMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_currentMonth.value == 12) {
            _currentYear.value += 1
            _currentMonth.value = 1
        } else {
            _currentMonth.value += 1
        }
    }

    fun setFilterMode(mode: FilterMode) { _filterMode.value = mode }

    fun getRecordForDay(day: Int): AttendanceRecord? {
        val dateStr = String.format("%04d-%02d-%02d", _currentYear.value, _currentMonth.value, day)
        return monthRecords.value.find { it.date == dateStr }
    }

    fun getLeaveForDay(day: Int): LeaveRecord? {
        val dateStr = String.format("%04d-%02d-%02d", _currentYear.value, _currentMonth.value, day)
        return monthLeaveRecords.value.find { it.date == dateStr }
    }

    /** 保存打卡记录 */
    fun saveRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            if (record.date.isBlank()) { _message.value = "日期不能为空"; return@launch }
            if (record.clockInTime.isNullOrBlank()) { _message.value = "上班时间不能为空"; return@launch }
            try {
                UserSettings.timeToMinutes(record.clockInTime)
                if (!record.clockOutTime.isNullOrBlank()) UserSettings.timeToMinutes(record.clockOutTime)
            } catch (e: IllegalArgumentException) {
                _message.value = "时间格式不正确（应为 HH:mm）"; return@launch
            }
            if (!record.clockOutTime.isNullOrBlank() && record.overtimeMinutes < 0) {
                _message.value = "加班时长不能为负数"; return@launch
            }
            dao.upsert(record)
            _message.value = "保存成功 ✓"
        }
    }

    /** 保存请假记录 */
    fun saveLeaveRecord(record: LeaveRecord) {
        viewModelScope.launch {
            if (record.date.isBlank()) { _message.value = "日期不能为空"; return@launch }
            if (record.minutes <= 0) { _message.value = "请假时长必须大于0"; return@launch }
            leaveDao.upsert(record)
            _message.value = "请假记录已保存 ✓"
        }
    }

    /** 删除请假记录 */
    fun deleteLeaveRecord(date: String) {
        viewModelScope.launch {
            leaveDao.deleteByDate(date)
            _message.value = "已删除请假记录"
        }
    }

    private var pendingDeleteDate: String? = null

    fun requestDeleteRecord(date: String): Boolean {
        if (pendingDeleteDate == date) {
            pendingDeleteDate = null
            viewModelScope.launch {
                dao.deleteByDate(date)
                _message.value = "已删除 ${DateUtils.displayDate(date)} 的记录"
            }
            return true
        }
        pendingDeleteDate = date
        _message.value = "再次点击确认删除 ${DateUtils.displayDate(date)} 的记录"
        return false
    }

    fun cancelPendingDelete() { pendingDeleteDate = null }

    fun deleteRecord(date: String) { requestDeleteRecord(date) }

    fun clearMessage() { _message.value = null }

    enum class FilterMode {
        ALL, WORKDAY, REST_DAY, OVERTIME_ONLY
    }
}
