package com.overtime.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overtime.tracker.OvertimeTrackerApp
import com.overtime.tracker.data.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置页面 ViewModel
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OvertimeTrackerApp
    private val settingsStore = app.settingsDataStore

    /** 当前设置 */
    val settings: StateFlow<UserSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    /** 保存设置 */
    fun saveSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            settingsStore.saveSettings(newSettings)
        }
    }
}
