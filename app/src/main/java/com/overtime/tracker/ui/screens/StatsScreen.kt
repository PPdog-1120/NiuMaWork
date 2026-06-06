package com.overtime.tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.overtime.tracker.ui.theme.*
import com.overtime.tracker.util.OvertimeCalculator
import com.overtime.tracker.viewmodel.StatsViewModel

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

/**
 * 统计页面（v1.3 更新：净加班、请假统计）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = viewModel()
) {
    val todayOvertime by viewModel.todayOvertime.collectAsState()
    val weekOvertime by viewModel.weekOvertime.collectAsState()
    val monthOvertime by viewModel.monthOvertime.collectAsState()
    val monthLeave by viewModel.monthLeave.collectAsState()
    val monthNetOvertime by viewModel.monthNetOvertime.collectAsState()
    val yearOvertime by viewModel.yearOvertime.collectAsState()
    val yearNetOvertime by viewModel.yearNetOvertime.collectAsState()
    val monthOvertimeDays by viewModel.monthOvertimeDays.collectAsState()
    val monthWorkdayOvertime by viewModel.monthWorkdayOvertime.collectAsState()
    val monthRestDayOvertime by viewModel.monthRestDayOvertime.collectAsState()
    val dailyAvgOvertime by viewModel.dailyAvgOvertime.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.refreshAll(); navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, "刷新", tint = TextPrimary)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 概览卡片
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("今日加班", todayOvertime, Modifier.weight(1f), Icons.Default.Today, AccentBlue)
                    StatCard("本周累计", weekOvertime, Modifier.weight(1f), Icons.Default.DateRange, LightBlue)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("本月累计", monthOvertime, Modifier.weight(1f), Icons.Default.CalendarMonth, CyanAccent)
                    StatCard("本年累计", yearOvertime, Modifier.weight(1f), Icons.Default.CalendarToday, SoftBlue)
                }

                // 净加班卡片
                NetOvertimeCard(monthOvertime, monthLeave, monthNetOvertime, yearOvertime, yearNetOvertime)

                // 详细统计
                DetailStatsCard(monthOvertimeDays, monthWorkdayOvertime, monthRestDayOvertime, dailyAvgOvertime, monthLeave)

                // 加班构成
                if (monthOvertime > 0) {
                    OvertimeCompositionCard(monthOvertime, monthWorkdayOvertime, monthRestDayOvertime)
                }
            }
        }
    }
}

/**
 * 净加班卡片（v1.3 新增）
 */
@Composable
private fun NetOvertimeCard(
    monthOvertime: Int, monthLeave: Int, monthNetOvertime: Int,
    yearOvertime: Int, yearNetOvertime: Int
) {
    Box(modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = GlowBlue.copy(alpha = 0.06f))) {
        Card(modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(modifier = Modifier.fillMaxWidth().background(CardGradient)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("净加班统计", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("本月", color = TextTertiary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(OvertimeCalculator.formatOvertimeWithSign(monthNetOvertime),
                                color = if (monthNetOvertime >= 0) CyanAccent else ErrorRed,
                                fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            if (monthLeave > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("请假 ${OvertimeCalculator.formatOvertime(monthLeave)}", color = WarningOrange, fontSize = 12.sp)
                            }
                        }
                        Box(modifier = Modifier.width(1.dp).height(60.dp).background(GlowBlue.copy(alpha = 0.2f)))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("本年", color = TextTertiary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(OvertimeCalculator.formatOvertimeWithSign(yearNetOvertime),
                                color = if (yearNetOvertime >= 0) SoftBlue else ErrorRed,
                                fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: Int, modifier: Modifier = Modifier, icon: ImageVector, accentColor: Color) {
    var displayedValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) {
        if (value != displayedValue) {
            val start = displayedValue; val end = value; val duration = 600; val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                displayedValue = start + ((end - start) * EaseOutCubic.transform(progress)).toInt()
                if (progress >= 1f) break
                kotlinx.coroutines.delay(16)
            }
            displayedValue = end
        }
    }
    val displayText = OvertimeCalculator.formatOvertime(displayedValue)
    Box(modifier = modifier.shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = accentColor.copy(alpha = 0.08f), spotColor = accentColor.copy(alpha = 0.08f))) {
        Card(modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(SurfaceMedium.copy(alpha = 0.95f), SurfaceDark)))) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(40.dp).background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(title, color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(displayText, color = if (displayedValue > 0) accentColor else TextSecondary,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailStatsCard(
    monthOvertimeDays: Int, monthWorkdayOvertime: Int, monthRestDayOvertime: Int,
    dailyAvgOvertime: Int, monthLeave: Int
) {
    Box(modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = GlowBlue.copy(alpha = 0.06f))) {
        Card(modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(modifier = Modifier.fillMaxWidth().background(CardGradient)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("本月详细统计", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))
                    AnimatedStatRow("加班天数", "${monthOvertimeDays} 天")
                    Spacer(modifier = Modifier.height(14.dp))
                    AnimatedStatRow("工作日延时", OvertimeCalculator.formatOvertime(monthWorkdayOvertime))
                    Spacer(modifier = Modifier.height(14.dp))
                    AnimatedStatRow("休息日加班", OvertimeCalculator.formatOvertime(monthRestDayOvertime))
                    if (monthLeave > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        AnimatedStatRow("请假扣除", OvertimeCalculator.formatOvertime(monthLeave))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, GlowBlue.copy(alpha = 0.3f), Color.Transparent))))
                    Spacer(modifier = Modifier.height(14.dp))
                    AnimatedStatRow("日均加班", OvertimeCalculator.formatOvertime(dailyAvgOvertime))
                }
            }
        }
    }
}

@Composable
private fun AnimatedStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 15.sp)
        Text(value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun OvertimeCompositionCard(monthOvertime: Int, monthWorkdayOvertime: Int, monthRestDayOvertime: Int) {
    val workdayPercent = if (monthOvertime > 0) (monthWorkdayOvertime * 100f / monthOvertime) else 0f
    val restDayPercent = 100f - workdayPercent
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedWorkday by animateFloatAsState(targetValue = if (animationPlayed) workdayPercent / 100f else 0f, animationSpec = tween(800, easing = EaseOutCubic), label = "workdayProgress")
    val animatedRestDay by animateFloatAsState(targetValue = if (animationPlayed) restDayPercent / 100f else 0f, animationSpec = tween(800, delayMillis = 200, easing = EaseOutCubic), label = "restDayProgress")
    LaunchedEffect(Unit) { animationPlayed = true }

    Box(modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = GlowBlue.copy(alpha = 0.06f))) {
        Card(modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(modifier = Modifier.fillMaxWidth().background(CardGradient)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("加班构成", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RingChart(animatedWorkday, animatedRestDay, Modifier.size(100.dp))
                        Spacer(modifier = Modifier.width(24.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ChartLegendItem(AccentBlue, "工作日延时", workdayPercent.toInt(), OvertimeCalculator.formatOvertime(monthWorkdayOvertime))
                            ChartLegendItem(WarningOrange, "休息日加班", restDayPercent.toInt(), OvertimeCalculator.formatOvertime(monthRestDayOvertime))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("工作日延时 ${workdayPercent.toInt()}%", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    AnimatedLinearProgress(animatedWorkday, AccentBlue, SurfaceLight)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("休息日加班 ${restDayPercent.toInt()}%", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    AnimatedLinearProgress(animatedRestDay, WarningOrange, SurfaceLight)
                }
            }
        }
    }
}

@Composable
private fun RingChart(workdayFraction: Float, restDayFraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx(); val sizeOffset = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth); val topLeft = Offset(sizeOffset, sizeOffset)
        drawArc(SurfaceLight.copy(alpha = 0.5f), -90f, 360f, false, topLeft, arcSize, Stroke(strokeWidth, cap = StrokeCap.Round))
        val workdaySweep = workdayFraction * 360f
        if (workdaySweep > 0f) drawArc(AccentBlue, -90f, workdaySweep, false, topLeft, arcSize, Stroke(strokeWidth, cap = StrokeCap.Round))
        val restDaySweep = restDayFraction * 360f
        if (restDaySweep > 0f) drawArc(WarningOrange, -90f + workdaySweep, restDaySweep, false, topLeft, arcSize, Stroke(strokeWidth, cap = StrokeCap.Round))
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String, percent: Int, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color = color, shape = RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text("$label $percent%", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(value, color = TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AnimatedLinearProgress(progress: Float, color: Color, trackColor: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(trackColor, RoundedCornerShape(4.dp))) {
        Box(modifier = Modifier.fillMaxWidth(fraction = progress.coerceIn(0f, 1f)).height(8.dp)
            .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.7f))), RoundedCornerShape(4.dp)))
    }
}
