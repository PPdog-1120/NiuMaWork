package com.overtime.tracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 请假记录数据访问对象（v1.3 新增）
 */
@Dao
interface LeaveDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: LeaveRecord)

    @Query("SELECT * FROM leave_records WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): LeaveRecord?

    @Query("SELECT * FROM leave_records WHERE date = :date LIMIT 1")
    fun getByDateFlow(date: String): Flow<LeaveRecord?>

    @Query("SELECT * FROM leave_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getBetween(startDate: String, endDate: String): List<LeaveRecord>

    @Query("SELECT * FROM leave_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getBetweenFlow(startDate: String, endDate: String): Flow<List<LeaveRecord>>

    @Query("SELECT COALESCE(SUM(minutes), 0) FROM leave_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalLeaveMinutes(startDate: String, endDate: String): Int

    @Query("DELETE FROM leave_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("SELECT * FROM leave_records ORDER BY date ASC")
    suspend fun getAllOnce(): List<LeaveRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<LeaveRecord>)
}
