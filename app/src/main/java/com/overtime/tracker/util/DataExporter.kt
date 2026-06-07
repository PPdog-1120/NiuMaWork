package com.overtime.tracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.overtime.tracker.data.AttendanceRecord
import com.overtime.tracker.data.LeaveRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 加班数据导出/导入工具
 *
 * 导出格式：JSON 文件，兼容跨设备、跨版本迁移
 * 文件命名：overtime_backup_yyyyMMdd_HHmmss.json
 */
object DataExporter {

    private const val EXPORT_VERSION = 1
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    // ──────────────────────────── 序列化 ────────────────────────────

    /**
     * 将打卡记录和请假记录序列化为 JSON 字符串
     */
    fun toJson(records: List<AttendanceRecord>, leaveRecords: List<LeaveRecord> = emptyList()): String {
        val root = JSONObject().apply {
            put("version", EXPORT_VERSION)
            put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()))
            put("count", records.size)
        }

        val arr = JSONArray()
        for (r in records) {
            arr.put(JSONObject().apply {
                put("date", r.date)
                put("clockInTime", r.clockInTime ?: JSONObject.NULL)
                put("clockOutTime", r.clockOutTime ?: JSONObject.NULL)
                put("isRestDay", r.isRestDay)
                put("overtimeMinutes", r.overtimeMinutes)
                put("type", r.type)
                put("source", r.source)
                put("createdAt", r.createdAt)
            })
        }
        root.put("records", arr)

        // 请假记录
        val leaveArr = JSONArray()
        for (l in leaveRecords) {
            leaveArr.put(JSONObject().apply {
                put("date", l.date)
                put("type", l.type.name)
                put("minutes", l.minutes)
                put("reason", l.reason ?: JSONObject.NULL)
                put("createdAt", l.createdAt)
            })
        }
        root.put("leaveRecords", leaveArr)

        return root.toString(2)
    }

    // ──────────────────────────── 反序列化 ────────────────────────────

    /**
     * 从 JSON 字符串解析打卡记录和请假记录
     *
     * @return 解析结果，包含记录列表和可能的错误信息
     */
    fun fromJson(json: String): ImportResult {
        return try {
            val root = JSONObject(json)
            val version = root.optInt("version", 0)
            if (version < 1 || version > EXPORT_VERSION) {
                return ImportResult.Error("不支持的备份版本: v$version")
            }

            val arr = root.getJSONArray("records")
            val records = mutableListOf<AttendanceRecord>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val date = obj.optString("date", "")
                if (date.isBlank()) continue  // 跳过无效记录

                records.add(AttendanceRecord(
                    date = date,
                    clockInTime = (obj.opt("clockInTime") as? String)?.takeUnless { it == "null" || it.isBlank() },
                    clockOutTime = (obj.opt("clockOutTime") as? String)?.takeUnless { it == "null" || it.isBlank() },
                    isRestDay = obj.optBoolean("isRestDay", false),
                    overtimeMinutes = obj.optInt("overtimeMinutes", 0),
                    type = obj.optString("type", AttendanceRecord.OVERTIME_TYPE_WORKDAY),
                    source = obj.optString("source", AttendanceRecord.SOURCE_MANUAL),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                ))
            }

            // 解析请假记录（兼容旧版无此字段的备份）
            val leaveRecords = mutableListOf<LeaveRecord>()
            val leaveArr = root.optJSONArray("leaveRecords")
            if (leaveArr != null) {
                for (i in 0 until leaveArr.length()) {
                    val obj = leaveArr.getJSONObject(i)
                    val date = obj.optString("date", "")
                    if (date.isBlank()) continue

                    val typeName = obj.optString("type", "FULL_DAY")
                    val leaveType = try { LeaveRecord.LeaveType.valueOf(typeName) } catch (_: Exception) { LeaveRecord.LeaveType.FULL_DAY }

                    leaveRecords.add(LeaveRecord(
                        date = date,
                        type = leaveType,
                        minutes = obj.optInt("minutes", 0),
                        reason = (obj.opt("reason") as? String)?.takeUnless { it == "null" || it.isBlank() },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
            }

            ImportResult.Success(records, leaveRecords)
        } catch (e: Exception) {
            ImportResult.Error("文件格式错误: ${e.message}")
        }
    }

    // ──────────────────────────── 文件操作 ────────────────────────────

    /**
     * 导出到缓存文件并返回 File
     */
    fun exportToFile(context: Context, records: List<AttendanceRecord>, leaveRecords: List<LeaveRecord> = emptyList()): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "overtime_backup_$timestamp.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(toJson(records, leaveRecords), Charsets.UTF_8)
        return file
    }

    /**
     * 生成分享 Intent（通过 FileProvider 分享缓存文件）
     * 返回 null 表示无法创建分享
     */
    fun createShareIntent(context: Context, file: File): Intent? {
        return try {
            if (!file.exists()) return null
            val authority = context.packageName + AUTHORITY_SUFFIX
            val uri = FileProvider.getUriForFile(context, authority, file)
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "加班数据备份")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从 Uri 读取并解析备份文件（限制最大 10MB 防止 OOM）
     */
    fun importFromUri(context: Context, uri: Uri): ImportResult {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                // 限制读取大小，防止恶意/异常大文件导致 OOM
                val limitedStream = stream.buffered()
                limitedStream.mark(10 * 1024 * 1024 + 1)
                val bytes = limitedStream.readBytes()
                if (bytes.size > 10 * 1024 * 1024) {
                    return ImportResult.Error("备份文件过大（最大 10MB）")
                }
                val text = String(bytes, Charsets.UTF_8)
                fromJson(text)
            } ?: ImportResult.Error("无法读取文件")
        } catch (e: OutOfMemoryError) {
            ImportResult.Error("文件过大，内存不足")
        } catch (e: Exception) {
            ImportResult.Error("读取文件失败: ${e.message}")
        }
    }

    // ──────────────────────────── 结果类型 ────────────────────────────

    sealed class ImportResult {
        data class Success(val records: List<AttendanceRecord>, val leaveRecords: List<LeaveRecord> = emptyList()) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}
