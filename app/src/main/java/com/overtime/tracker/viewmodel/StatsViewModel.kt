package com.overtime.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overtime.tracker.OvertimeTrackerApp
import com.overtime.tracker.util.DateUtils
import com.overtime.tracker.util.OvertimeCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 统计页面 ViewModel（v1.3 更新：净加班）
 */
class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OvertimeTrackerApp
    private val dao = app.database.attendanceDao()
    private val leaveDao = app.database.leaveDao()

    private val _todayOvertime = MutableStateFlow(0)
    val todayOvertime: StateFlow<Int> = _todayOvertime.asStateFlow()

    private val _weekOvertime = MutableStateFlow(0)
    val weekOvertime: StateFlow<Int> = _weekOvertime.asStateFlow()

    private val _monthOvertime = MutableStateFlow(0)
    val monthOvertime: StateFlow<Int> = _monthOvertime.asStateFlow()

    private val _monthLeave = MutableStateFlow(0)
    val monthLeave: StateFlow<Int> = _monthLeave.asStateFlow()

    private val _monthNetOvertime = MutableStateFlow(0)
    val monthNetOvertime: StateFlow<Int> = _monthNetOvertime.asStateFlow()

    private val _yearOvertime = MutableStateFlow(0)
    val yearOvertime: StateFlow<Int> = _yearOvertime.asStateFlow()

    private val _yearLeave = MutableStateFlow(0)
    val yearLeave: StateFlow<Int> = _yearLeave.asStateFlow()

    private val _yearNetOvertime = MutableStateFlow(0)
    val yearNetOvertime: StateFlow<Int> = _yearNetOvertime.asStateFlow()

    private val _monthOvertimeDays = MutableStateFlow(0)
    val monthOvertimeDays: StateFlow<Int> = _monthOvertimeDays.asStateFlow()

    private val _monthWorkdayOvertime = MutableStateFlow(0)
    val monthWorkdayOvertime: StateFlow<Int> = _monthWorkdayOvertime.asStateFlow()

    private val _monthRestDayOvertime = MutableStateFlow(0)
    val monthRestDayOvertime: StateFlow<Int> = _monthRestDayOvertime.asStateFlow()

    private val _dailyAvgOvertime = MutableStateFlow(0)
    val dailyAvgOvertime: StateFlow<Int> = _dailyAvgOvertime.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            val today = DateUtils.today()
            val (weekStart, weekEnd) = DateUtils.thisWeekRange()
            val (monthStart, monthEnd) = DateUtils.thisMonthRange()
            val (yearStart, yearEnd) = DateUtils.thisYearRange()

            val todayRecord = dao.getByDate(today)
            _todayOvertime.value = todayRecord?.overtimeMinutes ?: 0

            _weekOvertime.value = dao.getTotalOvertimeMinutes(weekStart, weekEnd)

            _monthOvertime.value = dao.getTotalOvertimeMinutes(monthStart, monthEnd)
            _monthWorkdayOvertime.value = dao.getWorkdayOvertimeMinutes(monthStart, monthEnd)
            _monthRestDayOvertime.value = dao.getRestDayOvertimeMinutes(monthStart, monthEnd)
            _monthOvertimeDays.value = dao.getOvertimeDaysCount(monthStart, monthEnd)
            _monthLeave.value = leaveDao.getTotalLeaveMinutes(monthStart, monthEnd)
            _monthNetOvertime.value = OvertimeCalculator.calculateNetOvertime(_monthOvertime.value, _monthLeave.value)

            _yearOvertime.value = dao.getTotalOvertimeMinutes(yearStart, yearEnd)
            _yearLeave.value = leaveDao.getTotalLeaveMinutes(yearStart, yearEnd)
            _yearNetOvertime.value = OvertimeCalculator.calculateNetOvertime(_yearOvertime.value, _yearLeave.value)

            val recordDays = dao.getRecordDaysCount(monthStart, monthEnd)
            _dailyAvgOvertime.value = if (recordDays > 0) _monthOvertime.value / recordDays else 0
        }
    }
}
