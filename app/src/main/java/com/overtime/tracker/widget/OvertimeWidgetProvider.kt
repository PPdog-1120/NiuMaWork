package com.overtime.tracker.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.overtime.tracker.MainActivity
import com.overtime.tracker.R
import com.overtime.tracker.data.AppDatabase
import com.overtime.tracker.util.DateUtils
import com.overtime.tracker.util.OvertimeCalculator
import kotlinx.coroutines.*
import java.util.Calendar

/**
 * 小组件 Provider（v1.3 更新）
 *
 * 修复（BUG #3）：增加午夜定时刷新机制
 * - 部分国产 ROM 不发送 ACTION_DATE_CHANGED 广播
 * - 通过 AlarmManager 设置午夜闹钟，确保跨天后小组件自动刷新
 * - 监听 BOOT_COMPLETED 重新设置闹钟
 */
abstract class OvertimeWidgetProvider : AppWidgetProvider() {

    enum class WidgetType { STANDARD, COMPACT, SQUARE }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        // 每次更新时确保午夜闹钟已设置
        scheduleMidnightAlarm(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 第一个小组件创建时设置午夜闹钟
        scheduleMidnightAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 最后一个小组件移除时取消闹钟
        cancelMidnightAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_SCREEN_ON,
            Intent.ACTION_BOOT_COMPLETED -> {
                updateAllWidgets(context)
                scheduleMidnightAlarm(context)
            }
            ACTION_MIDNIGHT_REFRESH -> {
                // 午夜闹钟触发：刷新小组件 + 设置下一个闹钟
                updateAllWidgets(context)
                scheduleMidnightAlarm(context)
            }
        }
    }

    companion object {
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** 午夜刷新的自定义 Action */
        private const val ACTION_MIDNIGHT_REFRESH = "com.overtime.tracker.ACTION_MIDNIGHT_REFRESH"
        private const val MIDNIGHT_REQUEST_CODE = 99999

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val standardIds = manager.getAppWidgetIds(ComponentName(context, OvertimeWidgetStandard::class.java))
            for (id in standardIds) updateWidget(context, manager, id, WidgetType.STANDARD)
            val compactIds = manager.getAppWidgetIds(ComponentName(context, OvertimeWidgetCompact::class.java))
            for (id in compactIds) updateWidget(context, manager, id, WidgetType.COMPACT)
            val squareIds = manager.getAppWidgetIds(ComponentName(context, OvertimeWidgetSquare::class.java))
            for (id in squareIds) updateWidget(context, manager, id, WidgetType.SQUARE)
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, widgetType: WidgetType = WidgetType.STANDARD) {
            widgetScope.launch {
                try {
                    val dao = AppDatabase.getInstance(context).attendanceDao()
                    val today = DateUtils.today()
                    val record = dao.getByDate(today)

                    val (monthStart, monthEnd) = DateUtils.thisMonthRange()
                    val monthOvertime = dao.getTotalOvertimeMinutes(monthStart, monthEnd)

                    val layoutId = when (widgetType) {
                        WidgetType.STANDARD -> R.layout.widget_standard
                        WidgetType.COMPACT -> R.layout.widget_compact
                        WidgetType.SQUARE -> R.layout.widget_2x2
                    }
                    val views = RemoteViews(context.packageName, layoutId)

                    val timeText = buildString {
                        append(record?.clockInTime ?: "--:--")
                        append(" -> ")
                        append(record?.clockOutTime ?: "--:--")
                    }
                    views.setTextViewText(R.id.widgetTimeText, timeText)

                    if (widgetType == WidgetType.STANDARD || widgetType == WidgetType.SQUARE) {
                        val overtimeText = "加班 ${OvertimeCalculator.formatOvertime(record?.overtimeMinutes ?: 0)}"
                        views.setTextViewText(R.id.widgetOvertimeText, overtimeText)
                    }

                    val monthText = "本月 ${OvertimeCalculator.formatOvertime(monthOvertime)}"
                    views.setTextViewText(R.id.widgetMonthOvertimeText, monthText)

                    val hasClockIn = record?.clockInTime != null
                    val hasClockOut = record?.clockOutTime != null
                    when {
                        !hasClockIn -> {
                            views.setTextViewText(R.id.widgetClockBtn, if (widgetType == WidgetType.COMPACT) "上班" else "上班打卡")
                            views.setTextColor(R.id.widgetClockBtn, 0xFFFFFFFF.toInt())
                            views.setInt(R.id.widgetClockBtn, "setBackgroundColor", 0xFF1565C0.toInt())
                            val clockInIntent = Intent(context, WidgetClockReceiver::class.java).apply {
                                action = "com.overtime.tracker.ACTION_CLOCK_IN"
                            }
                            val clockInPending = PendingIntent.getBroadcast(context, 0, clockInIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            views.setOnClickPendingIntent(R.id.widgetClockBtn, clockInPending)
                        }
                        hasClockIn && !hasClockOut -> {
                            views.setTextViewText(R.id.widgetClockBtn, if (widgetType == WidgetType.COMPACT) "下班" else "下班打卡")
                            views.setTextColor(R.id.widgetClockBtn, 0xFFFFFFFF.toInt())
                            views.setInt(R.id.widgetClockBtn, "setBackgroundColor", 0xFF42A5F5.toInt())
                            val clockOutIntent = Intent(context, WidgetClockReceiver::class.java).apply {
                                action = "com.overtime.tracker.ACTION_CLOCK_OUT"
                            }
                            val clockOutPending = PendingIntent.getBroadcast(context, 1, clockOutIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            views.setOnClickPendingIntent(R.id.widgetClockBtn, clockOutPending)
                        }
                        else -> {
                            views.setTextViewText(R.id.widgetClockBtn, "已完成")
                            views.setTextColor(R.id.widgetClockBtn, 0xFF78909C.toInt())
                            views.setInt(R.id.widgetClockBtn, "setBackgroundColor", 0x33FFFFFF)
                            val openIntent = Intent(context, MainActivity::class.java)
                            val openPending = PendingIntent.getActivity(context, 2, openIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            views.setOnClickPendingIntent(R.id.widgetClockBtn, openPending)
                        }
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * 设置午夜闹钟：计算到下一个午夜的时间，通过 AlarmManager 触发小组件刷新
         * 使用 set() — 不需要 SCHEDULE_EXACT_ALARM 权限
         * 在 Doze 模式下可能有几分钟延迟，但对"跨天刷新"场景完全够用
         */
        private fun scheduleMidnightAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, OvertimeWidgetProvider::class.java).apply {
                action = ACTION_MIDNIGHT_REFRESH
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, MIDNIGHT_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 计算下一个午夜时间
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // set() + RTC_WAKEUP：系统会自动在合适时机唤醒设备触发
            // 不需要额外权限，系统保证在非精确时间窗口内触发
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                midnight.timeInMillis,
                pendingIntent
            )
        }

        /**
         * 取消午夜闹钟
         */
        private fun cancelMidnightAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, OvertimeWidgetProvider::class.java).apply {
                action = ACTION_MIDNIGHT_REFRESH
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, MIDNIGHT_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}

class OvertimeWidgetStandard : OvertimeWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) updateWidget(context, appWidgetManager, appWidgetId, WidgetType.STANDARD)
    }
}

class OvertimeWidgetCompact : OvertimeWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) updateWidget(context, appWidgetManager, appWidgetId, WidgetType.COMPACT)
    }
}

class OvertimeWidgetSquare : OvertimeWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) updateWidget(context, appWidgetManager, appWidgetId, WidgetType.SQUARE)
    }
}
