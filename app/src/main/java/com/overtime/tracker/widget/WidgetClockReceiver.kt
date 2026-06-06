package com.overtime.tracker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.overtime.tracker.data.AppDatabase
import com.overtime.tracker.data.AttendanceRecord
import com.overtime.tracker.data.SettingsDataStore
import com.overtime.tracker.util.DateUtils
import com.overtime.tracker.util.OvertimeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * 小组件打卡动作接收器（v1.3 更新：适配新计算器）
 */
class WidgetClockReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WIDGET_CLOCK_CHANGED = "com.overtime.tracker.WIDGET_CLOCK_CHANGED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    "com.overtime.tracker.ACTION_CLOCK_IN" -> handleClockIn(context)
                    "com.overtime.tracker.ACTION_CLOCK_OUT" -> handleClockOut(context)
                }
                OvertimeWidgetProvider.updateAllWidgets(context)
                notifyActivity(context)
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "打卡失败，请重试", Toast.LENGTH_SHORT).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleClockIn(context: Context) {
        val dao = AppDatabase.getInstance(context).attendanceDao()
        val settingsStore = SettingsDataStore(context)
        val today = DateUtils.today()
        val existing = dao.getByDate(today)

        if (existing?.clockInTime != null) {
            showToast(context, "今日已打过上班卡")
            return
        }

        val nowTime = DateUtils.nowTime()
        val settings = settingsStore.settingsFlow.firstOrNull() ?: return
        val restDay = !settings.isWorkDay(today)

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
        showToast(context, if (restDay) "休息日上班打卡成功 ✓" else "上班打卡成功 ✓")
    }

    private suspend fun handleClockOut(context: Context) {
        val dao = AppDatabase.getInstance(context).attendanceDao()
        val settingsStore = SettingsDataStore(context)
        val today = DateUtils.today()
        val existing = dao.getByDate(today)

        if (existing?.clockInTime == null) {
            showToast(context, "请先打上班卡")
            return
        }
        if (existing.clockOutTime != null) {
            showToast(context, "今日已打过下班卡")
            return
        }

        val nowTime = DateUtils.nowTime()
        val settings = settingsStore.settingsFlow.firstOrNull() ?: return
        val clockIn = existing.clockInTime

        val overtime = OvertimeCalculator.calculateOvertime(clockIn, nowTime, settings, existing.isRestDay)

        if (overtime == -1) {
            showToast(context, "检测到跨天打卡，请在App中开启跨天支持")
            return
        }
        if (overtime == -2) {
            showToast(context, "打卡数据异常，请在App中重新打卡")
            return
        }

        val record = existing.copy(clockOutTime = nowTime, overtimeMinutes = overtime)
        dao.upsert(record)
        showToast(context, "下班打卡成功 ✓")
    }

    private fun notifyActivity(context: Context) {
        context.sendBroadcast(Intent(ACTION_WIDGET_CLOCK_CHANGED))
    }

    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
