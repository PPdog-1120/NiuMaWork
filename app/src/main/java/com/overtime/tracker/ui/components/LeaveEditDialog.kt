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
import com.overtime.tracker.data.LeaveRecord
import com.overtime.tracker.data.UserSettings
import com.overtime.tracker.ui.theme.*
import com.overtime.tracker.util.DateUtils
import com.overtime.tracker.util.OvertimeCalculator
import java.util.Calendar

/**
 * 请假记录编辑/新增对话框（v1.3 新增）
 */
@Composable
fun LeaveEditDialog(
    date: String,
    existingLeave: LeaveRecord?,
    settings: UserSettings,
    onDismiss: () -> Unit,
    onSave: (LeaveRecord) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditMode = existingLeave != null
    var dateStr by remember { mutableStateOf(existingLeave?.date ?: date) }
    var leaveType by remember { mutableStateOf(existingLeave?.type ?: LeaveRecord.LeaveType.FULL_DAY) }
    var customMinutes by remember { mutableStateOf(existingLeave?.minutes?.toString() ?: "") }
    var reason by remember { mutableStateOf(existingLeave?.reason ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 自动计算时长
    val computedMinutes = when (leaveType) {
        LeaveRecord.LeaveType.FULL_DAY -> settings.fullDayMinutes
        LeaveRecord.LeaveType.HALF_DAY -> settings.halfDayMinutes
        LeaveRecord.LeaveType.CUSTOM -> customMinutes.toIntOrNull() ?: 0
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth(0.92f).shadow(16.dp, RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(SurfaceMedium, SurfaceDark)), RoundedCornerShape(20.dp))
            .border(1.dp, WarningOrange.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(24.dp)) {
            Column {
                Text(if (isEditMode) "编辑请假记录" else "补录请假", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                // 日期
                val context = LocalContext.current
                Column {
                    Text("日期", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = "${DateUtils.displayDate(dateStr)} ${DateUtils.getDayOfWeekName(dateStr)}",
                        onValueChange = {}, readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = TextPrimary, disabledBorderColor = CardBorder, disabledContainerColor = SurfaceLight.copy(alpha = 0.4f)),
                        trailingIcon = {
                            if (!isEditMode) {
                                IconButton(onClick = {
                                    val cal = DateUtils.parseDate(dateStr)
                                    DatePickerDialog(context, { _, y, m, d -> dateStr = String.format("%04d-%02d-%02d", y, m + 1, d) },
                                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                }) { Icon(Icons.Default.CalendarMonth, "选择日期", tint = AccentBlue) }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 请假类型
                Text("请假类型", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeaveRecord.LeaveType.entries.forEach { type ->
                        FilterChip(
                            selected = leaveType == type,
                            onClick = { leaveType = type },
                            label = { Text(type.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningOrange.copy(alpha = 0.8f),
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceMedium,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = CardBorder,
                                selectedBorderColor = WarningOrange.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 自定义时长输入
                if (leaveType == LeaveRecord.LeaveType.CUSTOM) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { v -> customMinutes = v.filter { it.isDigit() }.take(4) },
                        label = { Text("请假时长（分钟）") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = WarningOrange, unfocusedBorderColor = CardBorder, cursorColor = WarningOrange
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 原因（可选）
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("请假原因（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = WarningOrange, unfocusedBorderColor = CardBorder, cursorColor = WarningOrange
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 预览
                Box(modifier = Modifier.fillMaxWidth().background(SurfaceLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("请假时长", color = TextSecondary, fontSize = 14.sp)
                        Text(OvertimeCalculator.formatOvertime(computedMinutes), color = WarningOrange, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮
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
                        val record = LeaveRecord(
                            id = existingLeave?.id ?: 0,
                            date = dateStr,
                            type = leaveType,
                            minutes = computedMinutes,
                            reason = reason.ifBlank { null },
                            createdAt = existingLeave?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(record)
                    }, modifier = Modifier.weight(1f), enabled = computedMinutes > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange), shape = RoundedCornerShape(12.dp)) {
                        Text("保存", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false }, containerColor = SurfaceMedium,
            titleContentColor = TextPrimary, textContentColor = TextSecondary,
            title = { Text("确认删除") }, text = { Text("确定要删除 ${DateUtils.displayDate(dateStr)} 的请假记录吗？") },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDelete?.invoke() }, colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = TextSecondary) } })
    }
}
