package com.overtime.tracker.widget

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

abstract class OvertimeWidgetProvider : AppWidgetProvider() {

    enum class WidgetType { STANDARD, COMPACT, SQUARE }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_SCREEN_ON -> updateAllWidgets(context)
        }
    }

    companion object {
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
