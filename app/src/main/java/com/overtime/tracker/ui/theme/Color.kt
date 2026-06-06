package com.overtime.tracker.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 深蓝色系主题配色 - 高端仪表盘风格
 */

// ─── 基础色 ───────────────────────────────────────────────
val DeepBlue = Color(0xFF080E1A)
val NavyBlue = Color(0xFF0D1B2A)
val SteelBlue = Color(0xFF1B2838)
val AccentBlue = Color(0xFF1E88E5)
val LightBlue = Color(0xFF42A5F5)
val SoftBlue = Color(0xFF90CAF9)
val CyanAccent = Color(0xFF00E5FF)

// ─── 表面颜色 ─────────────────────────────────────────────
val SurfaceDark = Color(0xFF0A1628)
val SurfaceMedium = Color(0xFF11203A)
val SurfaceLight = Color(0xFF1A2D4A)
val SurfaceElevated = Color(0xFF1E3456)

// ─── 文字颜色 ─────────────────────────────────────────────
val TextPrimary = Color(0xFFECF0F5)
val TextSecondary = Color(0xFF8EA4BF)
val TextTertiary = Color(0xFF5A7089)

// ─── 状态颜色 ─────────────────────────────────────────────
val SuccessGreen = Color(0xFF00E676)
val WarningOrange = Color(0xFFFFAB40)
val ErrorRed = Color(0xFFFF5252)

// ─── 发光 / 毛玻璃效果色 ──────────────────────────────────
val GlowBlue = Color(0xFF1E88E5)         // 卡片边框发光
val GlowCyan = Color(0xFF00BCD4)         // 强调发光
val GlassWhite = Color(0x14FFFFFF)        // 毛玻璃白色叠加
val GlassBorder = Color(0x22FFFFFF)       // 毛玻璃边框
val CardBorder = Color(0x3342A5F5)        // 卡片微妙边框
val InnerShadow = Color(0x18000000)       // 内阴影色

// ─── 渐变色定义 ──────────────────────────────────────────
val GradientDeepBlueStart = Color(0xFF060B14)
val GradientDeepBlueEnd = Color(0xFF0D1B2A)

val GradientCardStart = Color(0xFF11203A)
val GradientCardEnd = Color(0xFF0D1829)

val GradientButtonStart = Color(0xFF1565C0)
val GradientButtonEnd = Color(0xFF1E88E5)

val GradientButtonLightStart = Color(0xFF42A5F5)
val GradientButtonLightEnd = Color(0xFF00BCD4)

val GradientAccentStart = Color(0xFF00BCD4)
val GradientAccentEnd = Color(0xFF1E88E5)

// ─── 背景渐变 Brush（方便复用） ──────────────────────────
val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(GradientDeepBlueStart, GradientDeepBlueEnd)
)

val CardGradient = Brush.verticalGradient(
    colors = listOf(GradientCardStart, GradientCardEnd)
)

val ButtonPrimaryGradient = Brush.horizontalGradient(
    colors = listOf(GradientButtonStart, GradientButtonEnd)
)

val ButtonLightGradient = Brush.horizontalGradient(
    colors = listOf(GradientButtonLightStart, GradientButtonLightEnd)
)

val AccentGradient = Brush.horizontalGradient(
    colors = listOf(GradientAccentStart, GradientAccentEnd)
)

// ─── 加班等级热力图颜色（更有层次感的色阶） ─────────────
val OvertimeNone = Color(0xFF0E1A2B)       // 几乎与背景融合
val OvertimeMinimal = Color(0xFF0D2847)    // 极淡蓝
val OvertimeLight = Color(0xFF123966)      // 淡蓝
val OvertimeMedium = Color(0xFF1565C0)     // 中蓝
val OvertimeHeavy = Color(0xFF1E88E5)      // 亮蓝
val OvertimeIntense = Color(0xFF42A5F5)    // 更亮
val OvertimeExtreme = Color(0xFF00E5FF)    // 青色高亮 - 最高强度
