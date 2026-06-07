@file:Suppress("DEPRECATION")
package com.overtime.tracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.navigation.NavController
import com.overtime.tracker.data.AttendanceRecord
import com.overtime.tracker.data.UserSettings
import com.overtime.tracker.ui.navigation.Routes
import com.overtime.tracker.ui.theme.*
import com.overtime.tracker.util.DateUtils
import com.overtime.tracker.util.OvertimeCalculator
import com.overtime.tracker.viewmodel.MainViewModel

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

/**
 * 主页面 - 打卡页面（v1.3 更新：弹性归一化、净加班）
 *
 * 修复：ON_RESUME 时检测日期变化，自动刷新今日打卡状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val todayRecord by viewModel.todayRecord.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    val monthOvertime by viewModel.monthOvertime.collectAsState()
    val monthLeave by viewModel.monthLeave.collectAsState()
    val monthNetOvertime by viewModel.monthNetOvertime.collectAsState()
    val monthWorkdayOvertime by viewModel.monthWorkdayOvertime.collectAsState()
    val monthRestDayOvertime by viewModel.monthRestDayOvertime.collectAsState()
    val yearOvertime by viewModel.yearOvertime.collectAsState()
    val yearNetOvertime by viewModel.yearNetOvertime.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    BackHandler { (context as Activity).moveTaskToBack(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTodayIfNeeded()   // ← 跨天修复：检测日期变化
                viewModel.refreshAccumulated()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradientDeepBlueStart, GradientDeepBlueEnd, Color(0xFF0A1525))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    "加班计时器",
                    color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )

                TodayOverviewCard(todayRecord, settings, viewModel.isRestDay)
                Spacer(modifier = Modifier.height(24.dp))
                ClockButtons(
                    todayRecord = todayRecord,
                    onClockIn = { viewModel.clockIn() },
                    onClockOut = { viewModel.clockOut() },
                    onUndo = { viewModel.undoClockIn() }
                )
                Spacer(modifier = Modifier.height(24.dp))
                AccumulatedOvertimeCard(
                    monthOvertime = monthOvertime,
                    monthLeave = monthLeave,
                    monthNetOvertime = monthNetOvertime,
                    monthWorkdayOvertime = monthWorkdayOvertime,
                    monthRestDayOvertime = monthRestDayOvertime,
                    yearOvertime = yearOvertime,
                    yearNetOvertime = yearNetOvertime
                )
                Spacer(modifier = Modifier.height(28.dp))
                BottomNavRow(navController)
            }
        }
    }
}

/**
 * 今日概览卡片（v1.3：显示弹性窗口、归一化时间、应下班时间）
 */
@Composable
private fun TodayOverviewCard(
    record: AttendanceRecord?,
    settings: UserSettings,
    isRestDay: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(16.dp),
            ambientColor = GlowBlue.copy(alpha = glowAlpha), spotColor = GlowBlue.copy(alpha = glowAlpha))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(CardGradient)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (isRestDay) {
                        Surface(shape = RoundedCornerShape(6.dp), color = WarningOrange.copy(alpha = 0.15f)) {
                            Text("休息日", color = WarningOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 弹性窗口
                    OverviewRow("弹性窗口", "${settings.flexStart} ~ ${settings.flexEnd}")

                    // 上班打卡
                    OverviewRow("今日上班", record?.clockInTime ?: "--:--")

                    // 归一化上班时间
                    if (record?.clockInTime != null) {
                        val normalized = settings.normalizeClockIn(record.clockInTime)
                        if (normalized != record.clockInTime) {
                            OverviewRow("归一化上班", normalized)
                        }
                    }

                    // 下班打卡
                    OverviewRow("今日下班", record?.clockOutTime ?: "--:--")

                    // 应下班时间
                    val expectedOut = if (record?.clockInTime != null) {
                        settings.getExpectedClockOut(record.clockInTime)
                    } else {
                        settings.getDefaultClockOut()
                    }
                    OverviewRow("应下班时间", expectedOut)

                    Spacer(modifier = Modifier.height(12.dp))

                    // 分隔渐变线
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, GlowBlue.copy(alpha = 0.4f), Color.Transparent))
                    ))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 今日加班时长
                    val overtimeMinutes = record?.overtimeMinutes ?: 0
                    AnimatedOvertimeDisplay(overtimeMinutes)

                    if (record?.clockOutTime != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "类型：${OvertimeCalculator.getOvertimeTypeName(record.isRestDay)}",
                            color = TextTertiary, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedOvertimeDisplay(overtimeMinutes: Int) {
    var displayedMinutes by remember { mutableIntStateOf(0) }
    LaunchedEffect(overtimeMinutes) {
        if (overtimeMinutes != displayedMinutes) {
            val start = displayedMinutes; val end = overtimeMinutes; val duration = 500
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                displayedMinutes = start + ((end - start) * EaseOutCubic.transform(progress)).toInt()
                if (progress >= 1f) break
                kotlinx.coroutines.delay(16)
            }
            displayedMinutes = end
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("今日加班", color = TextSecondary, fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                OvertimeCalculator.formatOvertime(displayedMinutes),
                color = if (displayedMinutes > 0) LightBlue else TextSecondary,
                fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
            )
            if (displayedMinutes > 0) { Spacer(modifier = Modifier.width(6.dp)); Text("⚡", fontSize = 16.sp) }
        }
    }
}

@Composable
private fun OverviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun ClockButtons(todayRecord: AttendanceRecord?, onClockIn: () -> Unit, onClockOut: () -> Unit, onUndo: () -> Unit) {
    val hasClockIn = todayRecord?.clockInTime != null
    val hasClockOut = todayRecord?.clockOutTime != null
    val canUndo = hasClockIn && !hasClockOut && (System.currentTimeMillis() - (todayRecord?.createdAt ?: 0)) < 5 * 60 * 1000

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AnimatedClockButton(
            label = if (hasClockIn) "已打卡" else "上班打卡", icon = Icons.Default.Login,
            enabled = !hasClockIn, gradient = ButtonPrimaryGradient,
            disabledGradient = Brush.horizontalGradient(listOf(TextTertiary.copy(alpha = 0.2f), TextTertiary.copy(alpha = 0.15f))),
            onClick = onClockIn, modifier = Modifier.weight(1f)
        )
        AnimatedClockButton(
            label = if (hasClockOut) "已打卡" else "下班打卡", icon = Icons.Default.Logout,
            enabled = hasClockIn && !hasClockOut, gradient = ButtonLightGradient,
            disabledGradient = Brush.horizontalGradient(listOf(TextTertiary.copy(alpha = 0.2f), TextTertiary.copy(alpha = 0.15f))),
            onClick = onClockOut, modifier = Modifier.weight(1f)
        )
    }
    if (canUndo) {
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = onUndo, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("撤销上班打卡（5分钟内）", color = ErrorRed, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AnimatedClockButton(
    label: String, icon: ImageVector, enabled: Boolean, gradient: Brush,
    disabledGradient: Brush, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )
    Box(
        modifier = modifier.scale(scale).height(54.dp).clip(RoundedCornerShape(12.dp))
            .background(brush = if (enabled) gradient else disabledGradient, shape = RoundedCornerShape(12.dp))
            .border(width = if (enabled) 1.dp else 0.dp, color = if (enabled) GlowBlue.copy(alpha = 0.3f) else Color.Transparent, shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Surface(onClick = onClick, enabled = enabled, color = Color.Transparent, shape = RoundedCornerShape(12.dp),
            interactionSource = interactionSource, modifier = Modifier.fillMaxSize()) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = if (enabled) Color.White else TextTertiary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, color = if (enabled) Color.White else TextTertiary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * 累计加班卡片（v1.3：净加班 = 加班 - 请假）
 */
@Composable
private fun AccumulatedOvertimeCard(
    monthOvertime: Int, monthLeave: Int, monthNetOvertime: Int,
    monthWorkdayOvertime: Int, monthRestDayOvertime: Int,
    yearOvertime: Int, yearNetOvertime: Int
) {
    Box(modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = GlowBlue.copy(alpha = 0.1f), spotColor = GlowBlue.copy(alpha = 0.1f))) {
        Card(modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(modifier = Modifier.fillMaxWidth().background(CardGradient)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("累计加班", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 本月总加班
                    AnimatedAccumulatedRow("本月总加班", OvertimeCalculator.formatOvertime(monthOvertime), LightBlue)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 本月请假
                    if (monthLeave > 0) {
                        AnimatedAccumulatedRow("├ 请假扣除", OvertimeCalculator.formatOvertime(monthLeave), WarningOrange)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 本月净加班
                    val netColor = if (monthNetOvertime >= 0) CyanAccent else ErrorRed
                    AnimatedAccumulatedRow("本月净加班", OvertimeCalculator.formatOvertimeWithSign(monthNetOvertime), netColor)

                    Spacer(modifier = Modifier.height(12.dp))

                    // 工作日/休息日细分
                    Row(modifier = Modifier.padding(start = 16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("├ 工作日延时", color = TextTertiary, fontSize = 13.sp)
                            Text(OvertimeCalculator.formatOvertime(monthWorkdayOvertime), color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("└ 休息日加班", color = TextTertiary, fontSize = 13.sp)
                            Text(OvertimeCalculator.formatOvertime(monthRestDayOvertime), color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, GlowBlue.copy(alpha = 0.3f), Color.Transparent))
                    ))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 本年
                    AnimatedAccumulatedRow("本年累计加班", OvertimeCalculator.formatOvertime(yearOvertime), SoftBlue)
                    if (yearNetOvertime != yearOvertime) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val yearNetColor = if (yearNetOvertime >= 0) CyanAccent else ErrorRed
                        AnimatedAccumulatedRow("本年净加班", OvertimeCalculator.formatOvertimeWithSign(yearNetOvertime), yearNetColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedAccumulatedRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 15.sp)
        Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun BottomNavRow(navController: NavController) {
    Box(modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = GlowBlue.copy(alpha = 0.08f))
        .background(brush = Brush.verticalGradient(listOf(SurfaceMedium, SurfaceDark)), shape = RoundedCornerShape(16.dp))
        .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(vertical = 12.dp, horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            BottomNavButton(Icons.Default.History, "历史记录") { navController.navigate(Routes.HISTORY) }
            BottomNavButton(Icons.Default.BarChart, "统计") { navController.navigate(Routes.STATS) }
            BottomNavButton(Icons.Default.Settings, "设置") { navController.navigate(Routes.SETTINGS) }
        }
    }
}

@Composable
private fun BottomNavButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navScale"
    )
    val iconTint by animateColorAsState(targetValue = if (isPressed) CyanAccent else AccentBlue, animationSpec = tween(300), label = "navTint")
    Surface(onClick = onClick, color = Color.Transparent, interactionSource = interactionSource, shape = RoundedCornerShape(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale).padding(horizontal = 16.dp, vertical = 6.dp)) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = if (isPressed) TextPrimary else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
