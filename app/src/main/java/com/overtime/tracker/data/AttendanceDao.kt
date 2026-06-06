package com.overtime.tracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 打卡记录数据访问对象
 */
@Dao
interface AttendanceDao {

    /** 插入或更新打卡记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AttendanceRecord)

    /** 批量插入（导入用），冲突时替换 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceRecord>)

    /** 根据日期查询打卡记录 */
    @Query("SELECT * FROM attendance_records WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): AttendanceRecord?

    /** 根据日期查询打卡记录（响应式） */
    @Query("SELECT * FROM attendance_records WHERE date = :date LIMIT 1")
    fun getByDateFlow(date: String): Flow<AttendanceRecord?>

    /** 获取指定日期范围内的记录 */
    @Query("SELECT * FROM attendance_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getBetween(startDate: String, endDate: String): List<AttendanceRecord>

    /** 获取指定日期范围内的记录（响应式） */
    @Query("SELECT * FROM attendance_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getBetweenFlow(startDate: String, endDate: String): Flow<List<AttendanceRecord>>

    /** 获取所有记录，按日期倒序 */
    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllFlow(): Flow<List<AttendanceRecord>>

    /** 获取所有记录（一次性查询，导出用） */
    @Query("SELECT * FROM attendance_records ORDER BY date ASC")
    suspend fun getAllOnce(): List<AttendanceRecord>

    /** 获取指定日期范围内有加班的记录数量 */
    @Query("SELECT COUNT(*) FROM attendance_records WHERE date BETWEEN :startDate AND :endDate AND overtimeMinutes > 0")
    suspend fun getOvertimeDaysCount(startDate: String, endDate: String): Int

    /** #6 修复：获取指定日期范围内有打卡记录的天数（用于日均加班计算） */
    @Query("SELECT COUNT(*) FROM attendance_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getRecordDaysCount(startDate: String, endDate: String): Int

    /** 获取指定日期范围内工作日加班总分钟数 */
    @Query("SELECT COALESCE(SUM(overtimeMinutes), 0) FROM attendance_records WHERE date BETWEEN :startDate AND :endDate AND type = 'WORKDAY_OVERTIME'")
    suspend fun getWorkdayOvertimeMinutes(startDate: String, endDate: String): Int

    /** 获取指定日期范围内休息日加班总分钟数 */
    @Query("SELECT COALESCE(SUM(overtimeMinutes), 0) FROM attendance_records WHERE date BETWEEN :startDate AND :endDate AND type = 'REST_DAY_OVERTIME'")
    suspend fun getRestDayOvertimeMinutes(startDate: String, endDate: String): Int

    /** 获取指定日期范围内总加班分钟数 */
    @Query("SELECT COALESCE(SUM(overtimeMinutes), 0) FROM attendance_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalOvertimeMinutes(startDate: String, endDate: String): Int

    /** 删除指定日期的记录 */
    @Query("DELETE FROM attendance_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    /** 删除所有记录 */
    @Query("DELETE FROM attendance_records")
    suspend fun deleteAll()
}
