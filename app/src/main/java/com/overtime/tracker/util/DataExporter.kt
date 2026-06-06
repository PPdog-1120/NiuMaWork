package com.overtime.tracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.overtime.tracker.data.AttendanceRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
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
     * 将打卡记录序列化为 JSON 字符串
     */
    fun toJson(records: List<AttendanceRecord>): String {
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
                put("createdAt", r.createdAt)
            })
        }
        root.put("records", arr)
        return root.toString(2)
    }

    // ──────────────────────────── 反序列化 ────────────────────────────

    /**
     * 从 JSON 字符串解析打卡记录
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
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                ))
            }

            ImportResult.Success(records)
        } catch (e: Exception) {
            ImportResult.Error("文件格式错误: ${e.message}")
        }
    }

    // ──────────────────────────── 文件操作 ────────────────────────────

    /**
     * 导出到缓存文件并返回 File
     */
    fun exportToFile(context: Context, records: List<AttendanceRecord>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "overtime_backup_$timestamp.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(toJson(records), Charsets.UTF_8)
        return file
    }

    /**
     * 生成分享 Intent（通过 FileProvider 分享缓存文件）
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val authority = context.packageName + AUTHORITY_SUFFIX
        val uri = FileProvider.getUriForFile(context, authority, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "加班数据备份")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * 从 Uri 读取并解析备份文件
     */
    fun importFromUri(context: Context, uri: Uri): ImportResult {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val text = InputStreamReader(stream, Charsets.UTF_8).readText()
                fromJson(text)
            } ?: ImportResult.Error("无法读取文件")
        } catch (e: Exception) {
            ImportResult.Error("读取文件失败: ${e.message}")
        }
    }

    // ──────────────────────────── 结果类型 ────────────────────────────

    sealed class ImportResult {
        data class Success(val records: List<AttendanceRecord>) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}
