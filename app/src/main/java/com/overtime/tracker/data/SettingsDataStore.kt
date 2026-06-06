package com.overtime.tracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore 扩展属性 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * 用户设置管理器（v1.3 更新）
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        // 弹性工作制
        private val KEY_FLEX_START = stringPreferencesKey("flex_start")
        private val KEY_FLEX_END = stringPreferencesKey("flex_end")
        private val KEY_STANDARD_HOURS = intPreferencesKey("standard_hours")

        // 工作日午休
        private val KEY_LUNCH_START = stringPreferencesKey("lunch_start")
        private val KEY_LUNCH_END = stringPreferencesKey("lunch_end")

        // 工作日晚饭
        private val KEY_DINNER_DEDUCT = intPreferencesKey("dinner_deduct")

        // 周末/节假日
        private val KEY_WEEKEND_LUNCH_START = stringPreferencesKey("weekend_lunch_start")
        private val KEY_WEEKEND_LUNCH_END = stringPreferencesKey("weekend_lunch_end")
        private val KEY_WEEKEND_DINNER_DEDUCT = intPreferencesKey("weekend_dinner_deduct")

        // 请假
        private val KEY_FULL_DAY_MINUTES = intPreferencesKey("full_day_minutes")
        private val KEY_HALF_DAY_MINUTES = intPreferencesKey("half_day_minutes")

        // 其他
        private val KEY_CROSS_DAY = booleanPreferencesKey("cross_day_support")
    }

    /** 获取设置的响应式数据流 */
    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            flexStart = prefs[KEY_FLEX_START] ?: "08:30",
            flexEnd = prefs[KEY_FLEX_END] ?: "09:00",
            standardHours = prefs[KEY_STANDARD_HOURS] ?: 480,
            lunchStart = prefs[KEY_LUNCH_START] ?: "12:00",
            lunchEnd = prefs[KEY_LUNCH_END] ?: "13:00",
            dinnerDeduct = prefs[KEY_DINNER_DEDUCT] ?: 30,
            weekendLunchStart = prefs[KEY_WEEKEND_LUNCH_START] ?: "12:00",
            weekendLunchEnd = prefs[KEY_WEEKEND_LUNCH_END] ?: "13:00",
            weekendDinnerDeduct = prefs[KEY_WEEKEND_DINNER_DEDUCT] ?: 30,
            fullDayMinutes = prefs[KEY_FULL_DAY_MINUTES] ?: 480,
            halfDayMinutes = prefs[KEY_HALF_DAY_MINUTES] ?: 240,
            crossDaySupport = prefs[KEY_CROSS_DAY] ?: false
        )
    }

    /** 保存全部设置 */
    suspend fun saveSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLEX_START] = settings.flexStart
            prefs[KEY_FLEX_END] = settings.flexEnd
            prefs[KEY_STANDARD_HOURS] = settings.standardHours
            prefs[KEY_LUNCH_START] = settings.lunchStart
            prefs[KEY_LUNCH_END] = settings.lunchEnd
            prefs[KEY_DINNER_DEDUCT] = settings.dinnerDeduct
            prefs[KEY_WEEKEND_LUNCH_START] = settings.weekendLunchStart
            prefs[KEY_WEEKEND_LUNCH_END] = settings.weekendLunchEnd
            prefs[KEY_WEEKEND_DINNER_DEDUCT] = settings.weekendDinnerDeduct
            prefs[KEY_FULL_DAY_MINUTES] = settings.fullDayMinutes
            prefs[KEY_HALF_DAY_MINUTES] = settings.halfDayMinutes
            prefs[KEY_CROSS_DAY] = settings.crossDaySupport
        }
    }
}
