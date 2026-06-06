package com.overtime.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.overtime.tracker.ui.theme.*

/**
 * 时长选择器对话框
 * 用于选择工作时长（小时）或缓冲时间（分钟）
 *
 * @param title 对话框标题
 * @param initialMinutes 初始总分钟数
 * @param hourRange 小时可选范围
 * @param minuteStep 分钟步长（默认5分钟）
 * @param showHours 是否显示小时选择器
 * @param showMinutes 是否显示分钟选择器
 * @param onDismiss 关闭回调
 * @param onConfirm 确认回调，返回总分钟数
 */
@Composable
fun DurationPickerDialog(
    title: String,
    initialMinutes: Int,
    hourRange: IntRange = 4..16,
    minuteStep: Int = 5,
    showHours: Boolean = true,
    showMinutes: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf((initialMinutes / 60).coerceIn(hourRange)) }
    var selectedMinute by remember {
        mutableStateOf(
            if (showMinutes) {
                ((initialMinutes % 60) / minuteStep * minuteStep).coerceIn(0, 55)
            } else {
                0
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMedium)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 时长显示
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showHours) {
                        // 小时选择
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = {
                                selectedHour = if (selectedHour < hourRange.last) selectedHour + 1 else hourRange.first
                            }) {
                                Text("▲", color = AccentBlue, fontSize = 16.sp)
                            }
                            Text(
                                String.format("%02d", selectedHour),
                                color = TextPrimary,
                                fontSize = 36.sp
                            )
                            IconButton(onClick = {
                                selectedHour = if (selectedHour > hourRange.first) selectedHour - 1 else hourRange.last
                            }) {
                                Text("▼", color = AccentBlue, fontSize = 16.sp)
                            }
                        }

                        if (showMinutes) {
                            Text(":", color = TextPrimary, fontSize = 36.sp)
                        }
                    }

                    if (showMinutes) {
                        // 分钟选择
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = {
                                selectedMinute = (selectedMinute + minuteStep) % 60
                            }) {
                                Text("▲", color = AccentBlue, fontSize = 16.sp)
                            }
                            Text(
                                String.format("%02d", selectedMinute),
                                color = TextPrimary,
                                fontSize = 36.sp
                            )
                            IconButton(onClick = {
                                selectedMinute = (selectedMinute - minuteStep + 60) % 60
                            }) {
                                Text("▼", color = AccentBlue, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        onConfirm(selectedHour * 60 + selectedMinute)
                    }) {
                        Text("确定", color = AccentBlue)
                    }
                }
            }
        }
    }
}
