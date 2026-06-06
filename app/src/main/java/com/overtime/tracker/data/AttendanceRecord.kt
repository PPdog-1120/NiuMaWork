package com.overtime.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 打卡记录实体（v1.3 更新）
 * 存储每日上下班打卡时间及加班信息
 */
@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 日期，格式：yyyy-MM-dd */
    val date: String,

    /** 上班打卡时间，格式：HH:mm */
    val clockInTime: String? = null,

    /** 下班打卡时间，格式：HH:mm */
    val clockOutTime: String? = null,

    /** 是否为休息日 */
    val isRestDay: Boolean = false,

    /** 加班时长（分钟） */
    val overtimeMinutes: Int = 0,

    /** 加班类型：WORKDAY_OVERTIME / REST_DAY_OVERTIME */
    val type: String = OVERTIME_TYPE_WORKDAY,

    /** 记录来源：REALTIME（实时打卡）/ MANUAL（手动补录） */
    val source: String = SOURCE_REALTIME,

    /** 修改历史（JSON 字符串），记录谁在什么时间改了什么 */
    val modificationHistory: String? = null,

    /** 记录创建时间戳 */
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val OVERTIME_TYPE_WORKDAY = "WORKDAY_OVERTIME"
        const val OVERTIME_TYPE_REST_DAY = "REST_DAY_OVERTIME"

        const val SOURCE_REALTIME = "REALTIME"
        const val SOURCE_MANUAL = "MANUAL"
    }
}
