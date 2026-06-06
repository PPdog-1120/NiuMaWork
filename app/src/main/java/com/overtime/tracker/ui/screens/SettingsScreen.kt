package com.overtime.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.overtime.tracker.data.UserSettings
import com.overtime.tracker.ui.components.DurationPickerDialog
import com.overtime.tracker.ui.components.TimePickerDialog
import com.overtime.tracker.ui.theme.*
import com.overtime.tracker.viewmodel.SettingsViewModel
import com.overtime.tracker.viewmodel.MainViewModel
import com.overtime.tracker.ui.screens.ExportImportScreen
import java.util.Calendar

/**
 * 设置页面（v1.3 重构：弹性工作制、午休、晚饭、周末独立配置、请假配置）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var editingSettings by remember(settings) { mutableStateOf(settings) }
    var showSaveSuccess by remember { mutableStateOf(false) }

    // 各选择器显示状态
    var showFlexStartPicker by remember { mutableStateOf(false) }
    var showFlexEndPicker by remember { mutableStateOf(false) }
    var showStandardHoursPicker by remember { mutableStateOf(false) }
    var showLunchStartPicker by remember { mutableStateOf(false) }
    var showLunchEndPicker by remember { mutableStateOf(false) }
    var showDinnerDeductPicker by remember { mutableStateOf(false) }
    var showWeekendLunchStartPicker by remember { mutableStateOf(false) }
    var showWeekendLunchEndPicker by remember { mutableStateOf(false) }
    var showWeekendDinnerDeductPicker by remember { mutableStateOf(false) }
    var showFullDayPicker by remember { mutableStateOf(false) }
    var showHalfDayPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GradientDeepBlueStart, GradientDeepBlueEnd)))) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ═══ 弹性工作制 ═══
                SectionHeader("弹性工作制")

                SettingTimeCard("弹性最早上班", editingSettings.flexStart) { showFlexStartPicker = true }
                SettingTimeCard("弹性最晚上班", editingSettings.flexEnd) { showFlexEndPicker = true }

                // 标准工时
                val hours = editingSettings.standardHours / 60
                val mins = editingSettings.standardHours % 60
                SettingTimeCard("标准工时", "${hours}h ${mins}min") { showStandardHoursPicker = true }

                // ═══ 工作日休息 ═══
                SectionHeader("工作日休息")

                SettingTimeCard("午休开始", editingSettings.lunchStart) { showLunchStartPicker = true }
                SettingTimeCard("午休结束", editingSettings.lunchEnd) { showLunchEndPicker = true }
                SettingTimeCard("晚饭扣除", "${editingSettings.dinnerDeduct} 分钟") { showDinnerDeductPicker = true }

                // ═══ 周末/节假日 ═══
                SectionHeader("周末 / 节假日")

                SettingTimeCard("午休开始", editingSettings.weekendLunchStart) { showWeekendLunchStartPicker = true }
                SettingTimeCard("午休结束", editingSettings.weekendLunchEnd) { showWeekendLunchEndPicker = true }
                SettingTimeCard("晚饭扣除", "${editingSettings.weekendDinnerDeduct} 分钟") { showWeekendDinnerDeductPicker = true }

                // ═══ 请假配置 ═══
                SectionHeader("请假配置")

                SettingTimeCard("全天请假时长", "${editingSettings.fullDayMinutes / 60}h ${editingSettings.fullDayMinutes % 60}min") { showFullDayPicker = true }
                SettingTimeCard("半天请假时长", "${editingSettings.halfDayMinutes / 60}h ${editingSettings.halfDayMinutes % 60}min") { showHalfDayPicker = true }

                // ═══ 其他 ═══
                SectionHeader("其他")

                SettingCard {
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("自动跟随国家法定假日安排", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("已内置 ${Calendar.getInstance().get(Calendar.YEAR)} 年法定假日及调休数据", color = TextTertiary, fontSize = 13.sp)
                        }
                    }
                }

                SettingSwitchCard(
                    title = "跨天打卡支持",
                    subtitle = "适用于夜班场景，允许下班时间早于上班时间",
                    checked = editingSettings.crossDaySupport,
                    onCheckedChange = { editingSettings = editingSettings.copy(crossDaySupport = it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SaveButton(showSuccess = showSaveSuccess) {
                    viewModel.saveSettings(editingSettings)
                    showSaveSuccess = true
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExportImportScreen(viewModel = mainViewModel)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ── 时间选择器弹窗 ──
    if (showFlexStartPicker) TimePickerDialog(editingSettings.flexStart, { showFlexStartPicker = false }) { editingSettings = editingSettings.copy(flexStart = it); showFlexStartPicker = false }
    if (showFlexEndPicker) TimePickerDialog(editingSettings.flexEnd, { showFlexEndPicker = false }) { editingSettings = editingSettings.copy(flexEnd = it); showFlexEndPicker = false }
    if (showLunchStartPicker) TimePickerDialog(editingSettings.lunchStart, { showLunchStartPicker = false }) { editingSettings = editingSettings.copy(lunchStart = it); showLunchStartPicker = false }
    if (showLunchEndPicker) TimePickerDialog(editingSettings.lunchEnd, { showLunchEndPicker = false }) { editingSettings = editingSettings.copy(lunchEnd = it); showLunchEndPicker = false }
    if (showWeekendLunchStartPicker) TimePickerDialog(editingSettings.weekendLunchStart, { showWeekendLunchStartPicker = false }) { editingSettings = editingSettings.copy(weekendLunchStart = it); showWeekendLunchStartPicker = false }
    if (showWeekendLunchEndPicker) TimePickerDialog(editingSettings.weekendLunchEnd, { showWeekendLunchEndPicker = false }) { editingSettings = editingSettings.copy(weekendLunchEnd = it); showWeekendLunchEndPicker = false }

    // ── 时长选择器弹窗 ──
    if (showStandardHoursPicker) DurationPickerDialog("标准工时", editingSettings.standardHours, 4..16, showHours = true, showMinutes = true,
        onDismiss = { showStandardHoursPicker = false }) { editingSettings = editingSettings.copy(standardHours = it); showStandardHoursPicker = false }
    if (showDinnerDeductPicker) DurationPickerDialog("工作日晚饭扣除", editingSettings.dinnerDeduct, 0..2, showHours = true, showMinutes = true,
        onDismiss = { showDinnerDeductPicker = false }) { editingSettings = editingSettings.copy(dinnerDeduct = it); showDinnerDeductPicker = false }
    if (showWeekendDinnerDeductPicker) DurationPickerDialog("周末晚饭扣除", editingSettings.weekendDinnerDeduct, 0..2, showHours = true, showMinutes = true,
        onDismiss = { showWeekendDinnerDeductPicker = false }) { editingSettings = editingSettings.copy(weekendDinnerDeduct = it); showWeekendDinnerDeductPicker = false }
    if (showFullDayPicker) DurationPickerDialog("全天请假时长", editingSettings.fullDayMinutes, 4..16, showHours = true, showMinutes = true,
        onDismiss = { showFullDayPicker = false }) { editingSettings = editingSettings.copy(fullDayMinutes = it); showFullDayPicker = false }
    if (showHalfDayPicker) DurationPickerDialog("半天请假时长", editingSettings.halfDayMinutes, 2..8, showHours = true, showMinutes = true,
        onDismiss = { showHalfDayPicker = false }) { editingSettings = editingSettings.copy(halfDayMinutes = it); showHalfDayPicker = false }

    // 保存成功浮层
    if (showSaveSuccess) {
        LaunchedEffect(Unit) { kotlinx.coroutines.delay(2000); showSaveSuccess = false }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { SaveSuccessSnackbar() }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = GlowBlue.copy(alpha = 0.05f))) {
        Card(modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(modifier = Modifier.fillMaxWidth().background(CardGradient)) { content() }
        }
    }
}

@Composable
private fun SettingTimeCard(title: String, value: String, onClick: () -> Unit) {
    SettingCard {
        Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(value, color = AccentBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(Icons.Default.Edit, "修改", tint = TextTertiary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingCard {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TextTertiary, fontSize = 13.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue,
                    checkedBorderColor = AccentBlue.copy(alpha = 0.5f), uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = SurfaceLight, uncheckedBorderColor = CardBorder))
        }
    }
}

@Composable
private fun SaveButton(showSuccess: Boolean, onClick: () -> Unit) {
    val containerColor by animateColorAsState(targetValue = if (showSuccess) SuccessGreen else AccentBlue, animationSpec = tween(400), label = "saveBtnColor")
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)) {
        AnimatedVisibility(visible = !showSuccess, enter = fadeIn() + scaleIn()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text("保存设置", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        AnimatedVisibility(visible = showSuccess, enter = fadeIn() + scaleIn()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("保存成功", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SaveSuccessSnackbar() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val offsetY by animateDpAsState(targetValue = if (visible) 0.dp else 40.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "snackbarOffset")
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(300), label = "snackbarAlpha")
    Box(modifier = Modifier.padding(20.dp).offset(y = offsetY).alpha(alpha)) {
        Surface(shape = RoundedCornerShape(14.dp), color = SuccessGreen.copy(alpha = 0.95f), shadowElevation = 8.dp) {
            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("设置已保存", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
