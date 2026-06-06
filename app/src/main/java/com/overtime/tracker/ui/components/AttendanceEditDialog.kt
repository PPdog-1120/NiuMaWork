package com.overtime.tracker.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.overtime.tracker.data.AttendanceRecord
import com.overtime.tracker.data.UserSettings
import com.overtime.tracker.ui.theme.*
import com.overtime.tracker.util.DateUtils
import com.overtime.tracker.util.OvertimeCalculator
import java.util.Calendar

/**
 * 打卡记录编辑/新增对话框（v1.3 更新：来源标记）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceEditDialog(
    record: AttendanceRecord?,
    settings: UserSettings,
    onDismiss: () -> Unit,
    onSave: (AttendanceRecord) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditMode = record != null
    var dateStr by remember { mutableStateOf(record?.date ?: DateUtils.today()) }
    var clockInTime by remember { mutableStateOf(record?.clockInTime ?: "") }
    var clockOutTime by remember { mutableStateOf(record?.clockOutTime ?: "") }
    var isRestDay by remember { mutableStateOf(record?.isRestDay ?: false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClockInPicker by remember { mutableStateOf(false) }
    var showClockOutPicker by remember { mutableStateOf(false) }

    val previewOvertime = remember(clockInTime, clockOutTime, isRestDay) {
        if (clockInTime.isNotBlank() && clockOutTime.isNotBlank()) {
            OvertimeCalculator.calculateOvertime(clockInTime, clockOutTime, settings, isRestDay)
        } else 0
    }

    val isClockInValid = clockInTime.matches(Regex("\\d{2}:\\d{2}"))
    val isClockOutValid = clockOutTime.isBlank() || clockOutTime.matches(Regex("\\d{2}:\\d{2}"))
    val isFormValid = isClockInValid && isClockOutValid

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth(0.92f).shadow(16.dp, RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(SurfaceMedium, SurfaceDark)), RoundedCornerShape(20.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)).padding(24.dp)) {
            Column {
                Text(if (isEditMode) "编辑记录" else "手动补录", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                DateSelector(dateStr, !isEditMode) { dateStr = it }
                Spacer(modifier = Modifier.height(14.dp))
                TimeInputRow("上班时间", clockInTime, "如 08:30", { clockInTime = it }) { showClockInPicker = true }
                Spacer(modifier = Modifier.height(14.dp))
                TimeInputRow("下班时间", clockOutTime, "如 17:30（可留空）", { clockOutTime = it }) { showClockOutPicker = true }
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("休息日", color = TextSecondary, fontSize = 15.sp)
                    Switch(checked = isRestDay, onCheckedChange = { isRestDay = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = WarningOrange, checkedTrackColor = WarningOrange.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextTertiary, uncheckedTrackColor = SurfaceLight))
                }

                if (clockInTime.isNotBlank() && clockOutTime.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().background(SurfaceLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("预计加班", color = TextSecondary, fontSize = 14.sp)
                            Text(OvertimeCalculator.formatOvertime(previewOvertime),
                                color = if (previewOvertime > 0) LightBlue else TextTertiary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // 来源标记
                if (record?.source == AttendanceRecord.SOURCE_MANUAL) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("来源：手动补录", color = TextTertiary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isEditMode && onDelete != null) {
                        OutlinedButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(ErrorRed.copy(alpha = 0.5f), ErrorRed.copy(alpha = 0.3f)))),
                            shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp)); Text("删除", fontSize = 14.sp)
                        }
                    }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary), shape = RoundedCornerShape(12.dp)) {
                        Text("取消", fontSize = 14.sp)
                    }
                    Button(onClick = {
                        val newRecord = AttendanceRecord(
                            id = record?.id ?: 0, date = dateStr,
                            clockInTime = clockInTime.ifBlank { null }, clockOutTime = clockOutTime.ifBlank { null },
                            isRestDay = isRestDay, overtimeMinutes = previewOvertime,
                            type = if (isRestDay) AttendanceRecord.OVERTIME_TYPE_REST_DAY else AttendanceRecord.OVERTIME_TYPE_WORKDAY,
                            source = record?.source ?: AttendanceRecord.SOURCE_MANUAL,
                            createdAt = record?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(newRecord)
                    }, modifier = Modifier.weight(1f), enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue), shape = RoundedCornerShape(12.dp)) {
                        Text("保存", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showClockInPicker) TimePickerDialog("选择上班时间", clockInTime.ifBlank { settings.flexStart }, { showClockInPicker = false }) { clockInTime = it; showClockInPicker = false }
    if (showClockOutPicker) TimePickerDialog("选择下班时间", clockOutTime.ifBlank { settings.getDefaultClockOut() }, { showClockOutPicker = false }) { clockOutTime = it; showClockOutPicker = false }

    if (showDeleteConfirm) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false }, containerColor = SurfaceMedium,
            titleContentColor = TextPrimary, textContentColor = TextSecondary,
            title = { Text("确认删除") }, text = { Text("确定要删除 ${DateUtils.displayDate(dateStr)} 的打卡记录吗？此操作不可撤销。") },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDelete?.invoke() }, colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = TextSecondary) } })
    }
}

@Composable
private fun DateSelector(dateStr: String, enabled: Boolean, onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    Column {
        Text("日期", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = if (dateStr.isNotBlank()) "${DateUtils.displayDate(dateStr)} ${DateUtils.getDayOfWeekName(dateStr)}" else "",
            onValueChange = {}, readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = TextPrimary, disabledBorderColor = CardBorder, disabledContainerColor = SurfaceLight.copy(alpha = 0.4f)),
            trailingIcon = {
                if (enabled) {
                    IconButton(onClick = {
                        val cal = DateUtils.parseDate(dateStr.ifBlank { DateUtils.today() })
                        DatePickerDialog(context, { _, y, m, d -> onDateSelected(String.format("%04d-%02d-%02d", y, m + 1, d)) },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Icon(Icons.Default.CalendarMonth, "选择日期", tint = AccentBlue) }
                }
            }
        )
    }
}

@Composable
private fun TimeInputRow(label: String, timeValue: String, placeholder: String, onValueChange: (String) -> Unit, onPickClick: () -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = timeValue, onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == ':' }.take(5)) },
            placeholder = { Text(placeholder, color = TextTertiary) }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = AccentBlue, unfocusedBorderColor = CardBorder, cursorColor = AccentBlue, focusedContainerColor = SurfaceLight.copy(alpha = 0.4f), unfocusedContainerColor = SurfaceLight.copy(alpha = 0.4f)),
            trailingIcon = { IconButton(onClick = onPickClick) { Icon(Icons.Default.Schedule, "选择时间", tint = AccentBlue) } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(title: String, initialTime: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val parts = initialTime.split(":")
    val state = rememberTimePickerState(parts.getOrNull(0)?.toIntOrNull() ?: 9, parts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true)
    AlertDialog(onDismissRequest = onDismiss, containerColor = SurfaceMedium, titleContentColor = TextPrimary,
        title = { Text(title) }, text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(String.format("%02d:%02d", state.hour, state.minute)) }) { Text("确定", color = AccentBlue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } })
}
