package com.overtime.tracker.data

import com.overtime.tracker.util.ChineseHolidayUtil

/**
 * 用户设置数据类（v1.3 需求重构）
 * 使用 DataStore Preferences 存储
 */
data class UserSettings(
    // ── 弹性工作制 ──
    /** 弹性最早上班时间，格式 HH:mm */
    val flexStart: String = "08:30",
    /** 弹性最晚上班时间，格式 HH:mm */
    val flexEnd: String = "09:00",
    /** 标准工时（分钟），默认 480 = 8小时 */
    val standardHours: Int = 480,

    // ── 工作日午休 ─
    val lunchStart: String = "12:00",
    val lunchEnd: String = "13:00",

    // ── 工作日晚饭扣除 ──
    val dinnerDeduct: Int = 30,

    // ── 周末/节假日独立配置 ──
    val weekendLunchStart: String = "12:00",
    val weekendLunchEnd: String = "13:00",
    val weekendDinnerDeduct: Int = 30,

    // ── 请假配置 ──
    val fullDayMinutes: Int = 480,
    val halfDayMinutes: Int = 240,

    // ── 工作日设置 ──
    val workDays: Map<Int, Boolean> = mapOf(
        0 to false, // 周日
        1 to true,  // 周一
        2 to true,  // 周二
        3 to true,  // 周三
        4 to true,  // 周四
        5 to true,  // 周五
        6 to false  // 周六
    ),

    /** 是否支持跨天打卡 */
    val crossDaySupport: Boolean = false
) {
    companion object {
        /**
         * 将 HH:mm 字符串转换为从 00:00 起算的分钟数
         */
        fun timeToMinutes(time: String): Int {
            val parts = time.split(":")
            require(parts.size == 2) { "Invalid time format: $time" }
            val h = parts[0].toIntOrNull()
                ?: throw IllegalArgumentException("Non-numeric hour: $time")
            val m = parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Non-numeric minute: $time")
            require(h in 0..23 && m in 0..59) { "Time out of range: $time" }
            return h * 60 + m
        }

        /** 将分钟数格式化为 HH:mm 字符串 */
        fun minutesToTime(totalMinutes: Int): String {
            val normalized = ((totalMinutes % 1440) + 1440) % 1440
            val h = normalized / 60
            val m = normalized % 60
            return String.format("%02d:%02d", h, m)
        }
    }

    // ══════════════════════════════════════════════════
    //  工作日相关计算
    // ══════════════════════════════════════════════════

    /** 午休时长（分钟） */
    val lunchDuration: Int
        get() {
            val start = timeToMinutes(lunchStart)
            val end = timeToMinutes(lunchEnd)
            return (end - start).coerceAtLeast(0)
        }

    /** 周末午休时长（分钟） */
    val weekendLunchDuration: Int
        get() {
            val start = timeToMinutes(weekendLunchStart)
            val end = timeToMinutes(weekendLunchEnd)
            return (end - start).coerceAtLeast(0)
        }

    /**
     * 归一化上班时间
     * clamp(实际打卡, 弹性最早, 弹性最晚)
     */
    fun normalizeClockIn(actualClockIn: String): String {
        val actual = timeToMinutes(actualClockIn)
        val start = timeToMinutes(flexStart)
        val end = timeToMinutes(flexEnd)
        val clamped = actual.coerceIn(start, end)
        return minutesToTime(clamped)
    }

    /**
     * 应下班时间 = 归一化上班时间 + 标准工时 + 午休时长
     */
    fun getExpectedClockOut(actualClockIn: String): String {
        val normalized = timeToMinutes(normalizeClockIn(actualClockIn))
        return minutesToTime(normalized + standardHours + lunchDuration)
    }

    /**
     * 标准下班时间（以弹性最早为基准）
     * 用于主页显示"应下班时间"（未打卡时）
     */
    fun getDefaultClockOut(): String {
        val start = timeToMinutes(flexStart)
        return minutesToTime(start + standardHours + lunchDuration)
    }

    /** 判断指定日期是否为工作日（Calendar.DAY_OF_WEEK: 1=周日...7=周六） */
    fun isWorkDay(dayOfWeek: Int): Boolean {
        return workDays[dayOfWeek - 1] ?: false
    }

    /**
     * 判断指定日期是否为工作日（综合考虑法定假日和正常工作日设置）
     * @param dateStr 日期字符串 yyyy-MM-dd
     */
    fun isWorkDay(dateStr: String): Boolean {
        return ChineseHolidayUtil.isWorkDay(dateStr, workDays)
    }
}
