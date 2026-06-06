package com.overtime.tracker.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * 中国法定假日工具类
 *
 * 策略：在线获取 → 本地缓存 → 内置兜底数据
 * - 优先从 timor.tech API 获取当年假日数据（免费、无需 key）
 * - 获取成功后缓存到 SharedPreferences，下次直接读缓存
 * - 获取失败时回退到内置的 2025-2026 兜底数据
 * - 每年自动刷新一次
 */
object ChineseHolidayUtil {

    private const val TAG = "ChineseHoliday"
    private const val PREFS_NAME = "chinese_holidays"
    private const val KEY_LAST_FETCH_YEAR = "last_fetch_year"
    private const val KEY_HOLIDAY_PREFIX = "holiday_"    // holiday_2025 → "date1,date2,..."
    private const val KEY_WORKDAY_PREFIX = "workday_"    // workday_2025 → "date1,date2,..."

    private var prefs: SharedPreferences? = null

    // ═══ 内置兜底数据（2025-2026） ═══

    /** 法定休息日（节假日放假） */
    private val BUILTIN_HOLIDAYS: Set<String> = setOf(
        // ═══ 2025 年 ═══
        "2025-01-01",                                                   // 元旦
        "2025-01-28", "2025-01-29", "2025-01-30", "2025-01-31",       // 春节
        "2025-02-01", "2025-02-02", "2025-02-03", "2025-02-04",
        "2025-04-04", "2025-04-05", "2025-04-06",                     // 清明节
        "2025-05-01", "2025-05-02", "2025-05-03",                     // 劳动节
        "2025-05-04", "2025-05-05",
        "2025-05-31", "2025-06-01", "2025-06-02",                     // 端午节
        "2025-10-01", "2025-10-02", "2025-10-03", "2025-10-04",       // 中秋+国庆
        "2025-10-05", "2025-10-06", "2025-10-07", "2025-10-08",

        // ═══ 2026 年 ═══
        "2026-01-01", "2026-01-02", "2026-01-03",                     // 元旦
        "2026-02-17", "2026-02-18", "2026-02-19", "2026-02-20",       // 春节
        "2026-02-21", "2026-02-22", "2026-02-23",
        "2026-04-05", "2026-04-06", "2026-04-07",                     // 清明节
        "2026-05-01", "2026-05-02", "2026-05-03",                     // 劳动节
        "2026-05-04", "2026-05-05",
        "2026-06-19", "2026-06-20", "2026-06-21",                     // 端午节
        "2026-09-25", "2026-09-26", "2026-09-27",                     // 中秋节
        "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04",       // 国庆节
        "2026-10-05", "2026-10-06", "2026-10-07"
    )

    /** 调休上班日（周末但需上班） */
    private val BUILTIN_SPECIAL_WORKDAYS: Set<String> = setOf(
        // ═══ 2025 年 ═══
        "2025-01-26", "2025-02-08",                                     // 春节调休
        "2025-04-27",                                                   // 劳动节调休
        "2025-09-28", "2025-10-11",                                     // 国庆调休

        // ═══ 2026 年 ═══
        "2026-02-15", "2026-02-28",                                     // 春节调休
        "2026-04-26",                                                   // 劳动节调休
        "2026-06-14",                                                   // 端午节调休
        "2026-09-20",                                                   // 中秋节调休
        "2026-10-10"                                                    // 国庆调休
    )

    // ═══ 运行时缓存（从 API 或内置数据加载） ═══
    private val cachedHolidays = mutableMapOf<String, MutableSet<String>>()     // year → holiday dates
    private val cachedSpecialWorkdays = mutableMapOf<String, MutableSet<String>>() // year → special workday dates
    private val fetchedYears = mutableSetOf<String>()

    /**
     * 初始化（在 Application.onCreate 中调用）
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 从缓存加载已有数据
        loadCachedData()
    }

    /**
     * 刷新指定年份的假日数据（从网络获取）
     * 应在协程中调用，获取失败时静默回退到内置数据
     */
    suspend fun refresh(year: Int) {
        if (fetchedYears.contains(year.toString())) return
        fetchFromApi(year)
    }

    /**
     * 刷新当前年和下一年的数据
     */
    suspend fun refreshCurrentAndNextYear() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        refresh(currentYear)
        refresh(currentYear + 1)
    }

    /**
     * 判断指定日期是否为假日休息日
     * @return true=法定假日休息, false=调休上班日, null=无特殊规定（按正常周几判断）
     */
    fun isHolidayRestDay(year: Int, month: Int, day: Int): Boolean? {
        val dateStr = String.format("%04d-%02d-%02d", year, month, day)
        val yearStr = year.toString()

        // 优先查在线缓存
        val cachedHoliday = cachedHolidays[yearStr]
        if (cachedHoliday != null && cachedHoliday.contains(dateStr)) return true

        val cachedWorkday = cachedSpecialWorkdays[yearStr]
        if (cachedWorkday != null && cachedWorkday.contains(dateStr)) return false

        // 回退到内置数据
        if (BUILTIN_HOLIDAYS.contains(dateStr)) return true
        if (BUILTIN_SPECIAL_WORKDAYS.contains(dateStr)) return false

        return null
    }

    /**
     * 判断指定日期是否为工作日（综合考虑法定假日和正常工作日设置）
     * @param dateStr 日期字符串 yyyy-MM-dd
     * @param normalWorkDays 正常工作日映射（0=周日...6=周六）
     */
    fun isWorkDay(dateStr: String, normalWorkDays: Map<Int, Boolean>): Boolean {
        val cal = DateUtils.parseDate(dateStr)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=周日...7=周六

        val holidayResult = isHolidayRestDay(year, month, day)
        return when (holidayResult) {
            true -> false   // 法定假日 → 休息日
            false -> true   // 调休上班日 → 工作日
            null -> normalWorkDays[dayOfWeek - 1] ?: false
        }
    }

    // ═══ 内部实现 ═══

    /**
     * 从 SharedPreferences 加载缓存数据
     */
    private fun loadCachedData() {
        val p = prefs ?: return
        for (year in p.all.keys) {
            if (year.startsWith(KEY_HOLIDAY_PREFIX)) {
                val yearStr = year.removePrefix(KEY_HOLIDAY_PREFIX)
                cachedHolidays[yearStr] = (p.getString(year, null) ?: continue)
                    .split(",").filter { it.isNotBlank() }.toMutableSet()
            } else if (year.startsWith(KEY_WORKDAY_PREFIX)) {
                val yearStr = year.removePrefix(KEY_WORKDAY_PREFIX)
                cachedSpecialWorkdays[yearStr] = (p.getString(year, null) ?: continue)
                    .split(",").filter { it.isNotBlank() }.toMutableSet()
            }
        }
        // 标记已有缓存的年份为已获取
        fetchedYears.addAll(cachedHolidays.keys)
        Log.d(TAG, "Loaded cached holiday data for years: ${fetchedYears}")
    }

    /**
     * 从 timor.tech API 获取假日数据
     * API: GET https://timor.tech/api/holiday/year/{year}
     * 返回格式: { "code": 0, "holiday": { "2025-01-01": {"holiday": true, ...}, ... } }
     */
    private suspend fun fetchFromApi(year: Int) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://timor.tech/api/holiday/year/$year")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"

                if (conn.responseCode != 200) {
                    Log.w(TAG, "API returned ${conn.responseCode} for year $year")
                    return@withContext
                }

                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(body)
                if (json.optInt("code", -1) != 0) {
                    Log.w(TAG, "API returned error code for year $year")
                    return@withContext
                }

                val holidayObj = json.optJSONObject("holiday") ?: return@withContext
                val holidays = mutableSetOf<String>()
                val specialWorkdays = mutableSetOf<String>()

                for (dateKey in holidayObj.keys()) {
                    val entry = holidayObj.optJSONObject(dateKey) ?: continue
                    if (entry.optBoolean("holiday", false)) {
                        holidays.add(dateKey)
                    } else {
                        // holiday=false 在假日 API 中表示调休上班日
                        specialWorkdays.add(dateKey)
                    }
                }

                // 更新内存缓存
                val yearStr = year.toString()
                cachedHolidays[yearStr] = holidays
                cachedSpecialWorkdays[yearStr] = specialWorkdays
                fetchedYears.add(yearStr)

                // 持久化到 SharedPreferences
                prefs?.edit()?.apply {
                    putString("$KEY_HOLIDAY_PREFIX$yearStr", holidays.joinToString(","))
                    putString("$KEY_WORKDAY_PREFIX$yearStr", specialWorkdays.joinToString(","))
                    putInt(KEY_LAST_FETCH_YEAR, year)
                    apply()
                }

                Log.d(TAG, "Fetched $year: ${holidays.size} holidays, ${specialWorkdays.size} special workdays")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch holiday data for $year: ${e.message}")
                // 获取失败，静默回退到内置数据
            }
        }
    }
}
