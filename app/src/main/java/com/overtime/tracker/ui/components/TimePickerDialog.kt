package com.overtime.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.overtime.tracker.ui.theme.*

/**
 * 时间选择器对话框
 */
@Composable
fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    var selectedHour by remember { mutableStateOf(parts[0].toIntOrNull() ?: 9) }
    var selectedMinute by remember { mutableStateOf(parts[1].toIntOrNull() ?: 0) }

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
                    "选择时间",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 时间显示
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 小时选择
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            selectedHour = (selectedHour + 1) % 24
                        }) {
                            Text("▲", color = AccentBlue, fontSize = 16.sp)
                        }
                        Text(
                            String.format("%02d", selectedHour),
                            color = TextPrimary,
                            fontSize = 36.sp
                        )
                        IconButton(onClick = {
                            selectedHour = (selectedHour - 1 + 24) % 24
                        }) {
                            Text("▼", color = AccentBlue, fontSize = 16.sp)
                        }
                    }

                    Text(":", color = TextPrimary, fontSize = 36.sp)

                    // 分钟选择
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            selectedMinute = (selectedMinute + 5) % 60
                        }) {
                            Text("▲", color = AccentBlue, fontSize = 16.sp)
                        }
                        Text(
                            String.format("%02d", selectedMinute),
                            color = TextPrimary,
                            fontSize = 36.sp
                        )
                        IconButton(onClick = {
                            selectedMinute = (selectedMinute - 5 + 60) % 60
                        }) {
                            Text("▼", color = AccentBlue, fontSize = 16.sp)
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
                        onConfirm(String.format("%02d:%02d", selectedHour, selectedMinute))
                    }) {
                        Text("确定", color = AccentBlue)
                    }
                }
            }
        }
    }
}

/**
 * 滚轮式数字选择器
 */
@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = {
            if (value > range.first) onValueChange(value - 1)
        }) {
            Text("−", color = AccentBlue, fontSize = 20.sp)
        }
        Text(
            "$value",
            color = TextPrimary,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(48.dp)
        )
        IconButton(onClick = {
            if (value < range.last) onValueChange(value + 1)
        }) {
            Text("+", color = AccentBlue, fontSize = 20.sp)
        }
    }
}
