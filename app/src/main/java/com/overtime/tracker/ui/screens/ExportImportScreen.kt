package com.overtime.tracker.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overtime.tracker.viewmodel.MainViewModel

/**
 * 数据导出/导入页面
 *
 * 用法：在 SettingsScreen 中嵌入或作为独立导航目的地
 */
@Composable
fun ExportImportScreen(viewModel: MainViewModel) {
    val exportState by viewModel.exportState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val context = LocalContext.current

    // 文件选择器（导入用）
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    // 导出成功后弹出系统分享
    val exportedIntent = remember(exportState) {
        (exportState as? MainViewModel.ExportState.Ready)?.shareIntent
    }
    LaunchedEffect(exportedIntent) {
        exportedIntent?.let {
            try {
                context.startActivity(Intent.createChooser(it, "分享加班数据备份"))
            } catch (_: Exception) {
                // 部分设备无可用分享目标
            }
            viewModel.resetExportState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "数据管理",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // ── 导出卡片 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF42A5F5),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("导出数据", fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "将全部打卡记录导出为 JSON 文件，可通过社交应用发送给自己备份",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.exportData() },
                    enabled = exportState !is MainViewModel.ExportState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    if (exportState is MainViewModel.ExportState.Loading) {
                        Text("⏳ 导出中…", color = Color.White, fontSize = 14.sp)
                    } else {
                        Text("导出全部记录")
                    }
                }

                // 错误提示
                (exportState as? MainViewModel.ExportState.Error)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 导入卡片 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Upload,
                        contentDescription = null,
                        tint = Color(0xFF42A5F5),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("导入数据", fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "从备份文件恢复打卡记录，同日期记录将被覆盖",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    enabled = importState !is MainViewModel.ImportState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFF42A5F5))
                ) {
                    if (importState is MainViewModel.ImportState.Loading) {
                        Text("⏳ 导入中…", fontSize = 14.sp)
                    } else {
                        Text("选择备份文件")
                    }
                }

                // 结果提示
                (importState as? MainViewModel.ImportState.Success)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "✓ 成功导入 ${it.count} 条记录",
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodySmall
                    )
                    LaunchedEffect(it) { viewModel.resetImportState() }
                }
                (importState as? MainViewModel.ImportState.Error)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    LaunchedEffect(it) { viewModel.resetImportState() }
                }
            }
        }
    }
}
