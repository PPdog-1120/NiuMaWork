package com.overtime.tracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.overtime.tracker.data.AttendanceRecord
import com.overtime.tracker.data.LeaveRecord
import com.overtime.tracker.ui.components.AttendanceEditDialog
import com.overtime.tracker.ui.components.LeaveEditDialog
import com.overtime.tracker.ui.theme.*
import com.overtime.tracker.util.DateUtils
import com.overtime.tracker.util.OvertimeCalculator
import com.overtime.tracker.viewmodel.HistoryViewModel
import java.util.Calendar

/**
 * 历史记录页面（v1.3 更新：请假记录、日历显示请假标记）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = viewModel()
) {
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val filteredRecords by viewModel.filteredRecords.collectAsState()
    val monthLeaveRecords by viewModel.monthLeaveRecords.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    var showCalendar by remember { mutableStateOf(true) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var editingLeaveDate by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("历史记录", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showCalendar = !showCalendar }) {
                        Icon(
                            if (showCalendar) Icons.AutoMirrored.Filled.List else Icons.Default.CalendarMonth,
                            contentDescription = "切换视图", tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 请假补录 FAB
                FloatingActionButton(
                    onClick = { editingLeaveDate = DateUtils.today(); showLeaveDialog = true },
                    containerColor = WarningOrange.copy(alpha = 0.9f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp).shadow(6.dp, CircleShape)
                ) {
                    Icon(Icons.Default.EventBusy, contentDescription = "补录请假", modifier = Modifier.size(22.dp))
                }
                // 打卡补录 FAB
                FloatingActionButton(
                    onClick = { editingRecord = null; showEditDialog = true },
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.shadow(8.dp, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "手动补录")
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GradientDeepBlueStart, GradientDeepBlueEnd)))) {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp, vertical = 16.dp)) {
                MonthNavigator(currentYear, currentMonth, { viewModel.previousMonth() }, { viewModel.nextMonth() })
                Spacer(modifier = Modifier.height(12.dp))
                FilterChips(filterMode) { viewModel.setFilterMode(it) }
                Spacer(modifier = Modifier.height(20.dp))

                if (showCalendar) {
                    CalendarView(currentYear, currentMonth, viewModel, monthLeaveRecords,
                        onDayClick = { day ->
                            val dateStr = String.format("%04d-%02d-%02d", currentYear, currentMonth, day)
                            val existing = viewModel.getRecordForDay(day)
                            editingRecord = existing ?: AttendanceRecord(date = dateStr, isRestDay = !settings.isWorkDay(dateStr))
                            showEditDialog = true
                        },
                        onDayLongClick = { day ->
                            editingLeaveDate = String.format("%04d-%02d-%02d", currentYear, currentMonth, day)
                            showLeaveDialog = true
                        }
                    )
                } else {
                    ListView(filteredRecords, monthLeaveRecords,
                        onRecordClick = { record -> editingRecord = record; showEditDialog = true },
                        onLeaveClick = { leave -> editingLeaveDate = leave.date; showLeaveDialog = true }
                    )
                }
            }
        }
    }

    // 打卡编辑弹窗
    if (showEditDialog) {
        AttendanceEditDialog(
            record = editingRecord, settings = settings,
            onDismiss = { showEditDialog = false; editingRecord = null },
            onSave = { record -> viewModel.saveRecord(record); showEditDialog = false; editingRecord = null },
            onDelete = editingRecord?.let { { viewModel.deleteRecord(it.date); showEditDialog = false; editingRecord = null } }
        )
    }

    // 请假编辑弹窗
    if (showLeaveDialog) {
        val existingLeave = editingLeaveDate?.let { d -> monthLeaveRecords.find { it.date == d } }
        LeaveEditDialog(
            date = editingLeaveDate ?: DateUtils.today(),
            existingLeave = existingLeave,
            settings = settings,
            onDismiss = { showLeaveDialog = false; editingLeaveDate = null },
            onSave = { leave -> viewModel.saveLeaveRecord(leave); showLeaveDialog = false; editingLeaveDate = null },
            onDelete = existingLeave?.let { { viewModel.deleteLeaveRecord(it.date); showLeaveDialog = false; editingLeaveDate = null } }
        )
    }
}

@Composable
private fun MonthNavigator(year: Int, month: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    val alpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(300), label = "monthAlpha")
    Box(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = GlowBlue.copy(alpha = 0.08f))
        .background(Brush.verticalGradient(listOf(SurfaceMedium, SurfaceDark)), RoundedCornerShape(14.dp))
        .border(1.dp, CardBorder, RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, "上月", tint = AccentBlue, modifier = Modifier.size(28.dp)) }
            Text(DateUtils.displayMonth(year, month), color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(alpha))
            IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "下月", tint = AccentBlue, modifier = Modifier.size(28.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChips(currentMode: HistoryViewModel.FilterMode, onModeChange: (HistoryViewModel.FilterMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            HistoryViewModel.FilterMode.ALL to "全部",
            HistoryViewModel.FilterMode.OVERTIME_ONLY to "有加班",
            HistoryViewModel.FilterMode.WORKDAY to "工作日",
            HistoryViewModel.FilterMode.REST_DAY to "休息日"
        ).forEach { (mode, label) ->
            FilterChip(selected = currentMode == mode, onClick = { onModeChange(mode) },
                label = { Text(label, fontSize = 12.sp, fontWeight = if (currentMode == mode) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentBlue.copy(alpha = 0.85f), selectedLabelColor = Color.White, containerColor = SurfaceMedium, labelColor = TextSecondary),
                border = FilterChipDefaults.filterChipBorder(borderColor = CardBorder, selectedBorderColor = AccentBlue.copy(alpha = 0.5f)))
        }
    }
}

/**
 * 日历视图（v1.3：长按补录请假、显示请假标记）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarView(
    year: Int, month: Int, viewModel: HistoryViewModel,
    leaveRecords: List<LeaveRecord>,
    onDayClick: (Int) -> Unit, onDayLongClick: (Int) -> Unit
) {
    val daysInMonth = DateUtils.getDaysInMonth(year, month)
    val firstDayCal = DateUtils.parseDate(DateUtils.firstDayOfMonth(year, month))
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
    val today = Calendar.getInstance()
    val isCurrentMonth = today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) + 1 == month
    val todayDay = today.get(Calendar.DAY_OF_MONTH)

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathAlpha by infiniteTransition.animateFloat(0.3f, 0.8f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathAlpha")

    Box(modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = GlowBlue.copy(alpha = 0.06f))
        .background(Brush.verticalGradient(listOf(SurfaceMedium.copy(alpha = 0.9f), SurfaceDark)), RoundedCornerShape(16.dp))
        .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(day, color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            val totalCells = daysInMonth + (firstDayOfWeek - 1)
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - (firstDayOfWeek - 1) + 1
                        if (day in 1..daysInMonth) {
                            val record = viewModel.getRecordForDay(day)
                            val leave = leaveRecords.find { it.date == String.format("%04d-%02d-%02d", year, month, day) }
                            val overtimeMinutes = record?.overtimeMinutes ?: 0
                            val hasLeave = leave != null
                            val isToday = isCurrentMonth && day == todayDay
                            val bgColor = if (hasLeave) WarningOrange.copy(alpha = 0.3f) else getOvertimeColor(overtimeMinutes)
                            val glowColor = if (hasLeave) WarningOrange else getOvertimeGlowColor(overtimeMinutes)

                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape)
                                .combinedClickable(onClick = { onDayClick(day) }, onLongClick = { onDayLongClick(day) }),
                                contentAlignment = Alignment.Center) {
                                if (overtimeMinutes > 0 || hasLeave) {
                                    Box(modifier = Modifier.fillMaxSize(0.9f).clip(CircleShape).background(glowColor.copy(alpha = 0.15f)))
                                }
                                Box(modifier = Modifier.fillMaxSize(0.82f).clip(CircleShape).background(bgColor)
                                    .then(if (isToday) Modifier.border(1.5.dp, CyanAccent.copy(alpha = breathAlpha), CircleShape)
                                        else if (overtimeMinutes > 0 || hasLeave) Modifier.border(0.5.dp, glowColor.copy(alpha = 0.4f), CircleShape)
                                        else Modifier),
                                    contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$day", color = if (overtimeMinutes > 0 || hasLeave || isToday) TextPrimary else TextTertiary,
                                            fontSize = 11.sp, fontWeight = if (overtimeMinutes > 0 || hasLeave || isToday) FontWeight.Bold else FontWeight.Normal)
                                        if (hasLeave) {
                                            Text("假", color = WarningOrange, fontSize = 7.sp, fontWeight = FontWeight.Medium)
                                        } else if (overtimeMinutes > 0) {
                                            Text("${overtimeMinutes / 60}h", color = if (overtimeMinutes > 180) CyanAccent else LightBlue, fontSize = 7.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListView(
    records: List<AttendanceRecord>,
    leaveRecords: List<LeaveRecord>,
    onRecordClick: (AttendanceRecord) -> Unit,
    onLeaveClick: (LeaveRecord) -> Unit
) {
    if (records.isEmpty() && leaveRecords.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextTertiary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("暂无记录", color = TextTertiary, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("点击右下角 + 手动补录", color = TextTertiary.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 打卡记录
        items(records.size) { index ->
            AnimatedRecordCard(records[index], index) { onRecordClick(records[index]) }
        }
        // 请假记录
        items(leaveRecords.size) { index ->
            AnimatedLeaveCard(leaveRecords[index], index) { onLeaveClick(leaveRecords[index]) }
        }
    }
}

@Composable
private fun AnimatedRecordCard(record: AttendanceRecord, index: Int, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 60L); visible = true }
    val offsetY by animateDpAsState(targetValue = if (visible) 0.dp else 30.dp, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "cardOffset")
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(400), label = "cardAlpha")
    Box(modifier = Modifier.offset(y = offsetY).alpha(alpha)) { RecordCard(record, onClick) }
}

@Composable
private fun AnimatedLeaveCard(record: LeaveRecord, index: Int, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 60L); visible = true }
    val offsetY by animateDpAsState(targetValue = if (visible) 0.dp else 30.dp, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "leaveOffset")
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(400), label = "leaveAlpha")
    Box(modifier = Modifier.offset(y = offsetY).alpha(alpha)) { LeaveCard(record, onClick) }
}

@Composable
private fun RecordCard(record: AttendanceRecord, onClick: () -> Unit) {
    val accentColor = if (record.isRestDay) WarningOrange else AccentBlue
    val overtimeMinutes = record.overtimeMinutes
    Box(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = GlowBlue.copy(alpha = 0.05f)).clickable { onClick() }) {
        Card(modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(modifier = Modifier.fillMaxWidth().background(CardGradient)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f)))))
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(DateUtils.displayDate(record.date), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(DateUtils.getDayOfWeekName(record.date), color = TextTertiary, fontSize = 13.sp)
                                if (record.source == AttendanceRecord.SOURCE_MANUAL) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = TextTertiary.copy(alpha = 0.15f)) {
                                        Text("补录", color = TextTertiary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${record.clockInTime ?: "--:--"} → ${record.clockOutTime ?: "--:--"}", color = TextSecondary, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(OvertimeCalculator.formatOvertime(overtimeMinutes),
                                color = if (overtimeMinutes > 0) LightBlue else TextTertiary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.15f)) {
                                    Text(if (record.isRestDay) "休息日" else "工作日", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Edit, "编辑", tint = TextTertiary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaveCard(record: LeaveRecord, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = WarningOrange.copy(alpha = 0.05f)).clickable { onClick() }) {
        Card(modifier = Modifier.fillMaxWidth().border(1.dp, WarningOrange.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(modifier = Modifier.fillMaxWidth().background(CardGradient)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(WarningOrange, WarningOrange.copy(alpha = 0.3f)))))
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(DateUtils.displayDate(record.date), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(DateUtils.getDayOfWeekName(record.date), color = TextTertiary, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(record.type.label, color = WarningOrange, fontSize = 14.sp)
                            if (!record.reason.isNullOrBlank()) {
                                Text(record.reason, color = TextTertiary, fontSize = 12.sp)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(OvertimeCalculator.formatOvertime(record.minutes), color = WarningOrange, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = WarningOrange.copy(alpha = 0.15f)) {
                                Text("请假", color = WarningOrange, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getOvertimeColor(minutes: Int): Color = when {
    minutes <= 0 -> OvertimeNone; minutes <= 30 -> OvertimeMinimal; minutes <= 60 -> OvertimeLight
    minutes <= 120 -> OvertimeMedium; minutes <= 180 -> OvertimeHeavy; minutes <= 240 -> OvertimeIntense
    else -> OvertimeExtreme
}

private fun getOvertimeGlowColor(minutes: Int): Color = when {
    minutes <= 60 -> AccentBlue; minutes <= 180 -> LightBlue; else -> CyanAccent
}


