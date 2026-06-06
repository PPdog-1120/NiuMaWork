package com.overtime.tracker

import android.app.Application
import com.overtime.tracker.data.AppDatabase
import com.overtime.tracker.data.SettingsDataStore
import com.overtime.tracker.util.ChineseHolidayUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口
 */
class OvertimeTrackerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)

        ChineseHolidayUtil.init(this)
        appScope.launch {
            ChineseHolidayUtil.refreshCurrentAndNextYear()
        }
    }
}
