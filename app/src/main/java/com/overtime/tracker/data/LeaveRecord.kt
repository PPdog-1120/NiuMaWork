package com.overtime.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 请假记录实体（v1.3 新增）
 */
@Entity(tableName = "leave_records")
data class LeaveRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 日期，格式：yyyy-MM-dd */
    val date: String,

    /** 请假类型 */
    val type: LeaveType = LeaveType.FULL_DAY,

    /** 请假时长（分钟） */
    val minutes: Int = 0,

    /** 请假原因（可选） */
    val reason: String? = null,

    /** 记录创建时间戳 */
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class LeaveType(val label: String) {
        FULL_DAY("全天请假"),
        HALF_DAY("半天请假"),
        CUSTOM("自定义时长")
    }
}
