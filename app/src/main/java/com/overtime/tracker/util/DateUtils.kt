package com.overtime.tracker.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期工具类
 */
object DateUtils {

    // SimpleDateFormat 非线程安全，使用 ThreadLocal 保证每个线程独立实例
    private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    }
    private val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("HH:mm", Locale.CHINA)
    }
    private val displayDateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("M月d日", Locale.CHINA)
    }
    private val displayMonthFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("yyyy年M月", Locale.CHINA)
    }

    /** 获取今天的日期字符串 yyyy-MM-dd */
    fun today(): String = dateFormat.get()!!.format(Date())

    /** 获取当前时间字符串 HH:mm */
    fun nowTime(): String = timeFormat.get()!!.format(Date())

    /** 解析日期字符串为 Calendar */
    fun parseDate(dateStr: String): Calendar {
        val date = dateFormat.get()!!.parse(dateStr) ?: Date()
        return Calendar.getInstance().apply { time = date }
    }

    /** Calendar 转日期字符串 */
    fun formatDate(calendar: Calendar): String = dateFormat.get()!!.format(calendar.time)

    /** 格式化为显示用日期 M月d日 */
    fun displayDate(dateStr: String): String {
        val cal = parseDate(dateStr)
        return displayDateFormat.get()!!.format(cal.time)
    }

    /** 格式化为显示用月份 yyyy年M月 */
    fun displayMonth(year: Int, month: Int): String {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return displayMonthFormat.get()!!.format(cal.time)
    }

    /** 获取星期几的中文名 */
    fun getDayOfWeekName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> ""
        }
    }

    /** 获取日期对应的星期几中文名 */
    fun getDayOfWeekName(dateStr: String): String {
        val cal = parseDate(dateStr)
        return getDayOfWeekName(cal.get(Calendar.DAY_OF_WEEK))
    }

    /** 获取今天的 Calendar.DAY_OF_WEEK */
    fun todayDayOfWeek(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    }

    /** 获取指定月份的第一天 */
    fun firstDayOfMonth(year: Int, month: Int): String {
        return String.format("%04d-%02d-01", year, month)
    }

    /** 获取指定月份的最后一天 */
    fun lastDayOfMonth(year: Int, month: Int): String {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, maxDay)
    }

    /** 获取本周的起止日期 */
    fun thisWeekRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = formatDate(cal)
        cal.add(Calendar.DATE, 6)  // 修复：用 Calendar.DATE 替代 Calendar.DAY_OF_WEEK，避免年边界越界
        val end = formatDate(cal)
        return start to end
    }

    /** 获取本年的起止日期 */
    fun thisYearRange(): Pair<String, String> {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return "$year-01-01" to "$year-12-31"
    }

    /** 获取本月份的起止日期 */
    fun thisMonthRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return firstDayOfMonth(year, month) to lastDayOfMonth(year, month)
    }

    /** 获取指定日期的月份天数 */
    fun getDaysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}
