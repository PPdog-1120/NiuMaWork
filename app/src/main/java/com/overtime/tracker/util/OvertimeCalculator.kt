package com.overtime.tracker.util

import com.overtime.tracker.data.UserSettings

/**
 * 加班计算引擎（v1.3 需求重构）
 *
 * 工作日公式：
 *   归一化上班 = clamp(实际打卡, flex_start, flex_end)
 *   应下班     = 归一化上班 + standard_hours + lunch_duration
 *   加班时长   = max(0, 实际下班 - 应下班 - dinner_deduct)
 *
 * 周末/节假日公式：
 *   总时长 = 结束 - 开始
 *   IF 跨午休: 总时长 -= lunch_duration
 *   IF 跨晚饭(18:00起): 总时长 -= dinner_deduct
 *   加班时长 = max(0, 总时长)
 *
 * 净加班 = 总加班 - 总请假
 *
 * 修复：跨天场景下午休/晚饭扣除使用原始时间判断，避免误扣
 */
object OvertimeCalculator {

    /** 晚饭起算时间 18:00（周末判断跨晚饭用） */
    private const val DINNER_START = 18 * 60  // 1080

    /**
     * 计算工作日加班时长（分钟）
     *
     * @return 加班时长 >= 0；-1 = 跨天未开启；-2 = 数据异常
     */
    fun calculateWeekdayOvertime(
        clockInTime: String,
        clockOutTime: String,
        settings: UserSettings
    ): Int {
        val inMin: Int
        var outMin: Int
        try {
            inMin = UserSettings.timeToMinutes(clockInTime)
            outMin = UserSettings.timeToMinutes(clockOutTime)
        } catch (e: IllegalArgumentException) {
            return -2
        }

        // 跨天处理
        if (outMin < inMin) {
            if (!settings.crossDaySupport) return -1
            outMin += 24 * 60
        }

        // 归一化上班
        val flexStart = UserSettings.timeToMinutes(settings.flexStart)
        val flexEnd = UserSettings.timeToMinutes(settings.flexEnd)
        val normalizedIn = inMin.coerceIn(flexStart, flexEnd)

        // 应下班 = 归一化上班 + 标准工时 + 午休时长
        val expectedOut = normalizedIn + settings.standardHours + settings.lunchDuration

        // 加班 = 实际下班 - 应下班 - 晚饭扣除
        val overtime = outMin - expectedOut - settings.dinnerDeduct
        return overtime.coerceAtLeast(0)
    }

    /**
     * 计算周末/节假日加班时长（分钟）
     *
     * @return 加班时长 >= 0；-1 = 跨天未开启；-2 = 数据异常
     *
     * 修复说明（BUG #2）：
     * 跨天后 endMin 会 +1440，如果用加后的时间去判断是否跨越午休/晚饭，
     * 会导致所有跨天场景都被误扣。改为用原始时间（跨天前）判断重叠。
     */
    fun calculateWeekendOvertime(
        startTime: String,
        endTime: String,
        settings: UserSettings
    ): Int {
        val startMin: Int
        var endMin: Int
        try {
            startMin = UserSettings.timeToMinutes(startTime)
            endMin = UserSettings.timeToMinutes(endTime)
        } catch (e: IllegalArgumentException) {
            return -2
        }

        // 跨天处理
        if (endMin < startMin) {
            if (!settings.crossDaySupport) return -1
            endMin += 24 * 60
        }

        var total = endMin - startMin

        // ── 修复：用原始时间判断是否跨越午休/晚饭 ──
        // startMin 和 rawEndMin 都是当天 0~1440 范围内的原始值
        val rawEndMin = UserSettings.timeToMinutes(endTime)

        // 判断是否跨越午休
        val lunchS = UserSettings.timeToMinutes(settings.weekendLunchStart)
        val lunchE = UserSettings.timeToMinutes(settings.weekendLunchEnd)
        val crossesLunch = if (startMin <= rawEndMin) {
            // 正常情况（未跨天）：检查时间范围是否与午休重叠
            startMin < lunchE && rawEndMin > lunchS
        } else {
            // 跨天情况：startMin > rawEndMin（如 22:00 → 02:00）
            // 重叠条件：经过了午休时段
            // 22:00~24:00 不经过午休，00:00~02:00 也不经过午休 → 无重叠
            // 但如果 startMin < lunchE（如 10:00 → 02:00），则经过了午休
            startMin < lunchE || rawEndMin > lunchS
        }
        if (crossesLunch) {
            total -= settings.weekendLunchDuration
        }

        // 判断是否跨越晚饭（18:00 起）
        val dinnerEnd = DINNER_START + settings.weekendDinnerDeduct
        val crossesDinner = if (startMin <= rawEndMin) {
            startMin < dinnerEnd && rawEndMin > DINNER_START
        } else {
            startMin < dinnerEnd || rawEndMin > DINNER_START
        }
        if (crossesDinner) {
            total -= settings.weekendDinnerDeduct
        }

        return total.coerceAtLeast(0)
    }

    /**
     * 自动判断工作日/休息日并计算加班
     */
    fun calculateOvertime(
        clockInTime: String,
        clockOutTime: String,
        settings: UserSettings,
        isRestDay: Boolean
    ): Int {
        return if (isRestDay) {
            calculateWeekendOvertime(clockInTime, clockOutTime, settings)
        } else {
            calculateWeekdayOvertime(clockInTime, clockOutTime, settings)
        }
    }

    /**
     * 计算净加班时长 = 总加班 - 总请假
     * 可以为负数
     */
    fun calculateNetOvertime(totalOvertimeMinutes: Int, totalLeaveMinutes: Int): Int {
        return totalOvertimeMinutes - totalLeaveMinutes
    }

    /**
     * 格式化加班时长为可读字符串
     * 精确到分钟，格式：Xh Ymin
     */
    fun formatOvertime(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0h 0min"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}min"
    }

    /**
     * 格式化加班时长（带符号，用于显示正负）
     */
    fun formatOvertimeWithSign(totalMinutes: Int): String {
        return when {
            totalMinutes > 0 -> "+${formatOvertime(totalMinutes)}"
            totalMinutes < 0 -> "-${formatOvertime(-totalMinutes)}"
            else -> "0h 0min"
        }
    }

    /**
     * 获取加班类型描述
     */
    fun getOvertimeTypeName(isRestDay: Boolean): String {
        return if (isRestDay) "休息日加班" else "工作日延时"
    }
}
