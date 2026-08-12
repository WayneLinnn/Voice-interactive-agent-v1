package com.waynelinnn.voiceagent.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.waynelinnn.voiceagent.R

private val BodyFont = FontFamily(Font(R.font.manrope_flex, weight = FontWeight.Normal))
private val DisplayFont = FontFamily(Font(R.font.manrope_flex, weight = FontWeight.SemiBold))

/** Quantis brand — true black + logo gradient (coral → magenta → violet → blue). */
val QuantisBlack = Color(0xFF000000)
val QuantisVoid = Color(0xFF07070A)
val QuantisSurface = Color(0xFF121218)
val QuantisText = Color(0xFFF4F4F7)
val QuantisMuted = Color(0xFF9A9AA8)
val QuantisCoral = Color(0xFFFF3B4A)
val QuantisMagenta = Color(0xFFE100FF)
val QuantisViolet = Color(0xFF7A1BFF)
val QuantisBlue = Color(0xFF2F5BFF)

val QuantisBrandGradient = listOf(
    QuantisCoral,
    QuantisMagenta,
    QuantisViolet,
    QuantisBlue,
)

fun quantisHorizontalBrush(): Brush = Brush.horizontalGradient(QuantisBrandGradient)

fun quantisSweepBrush(): Brush = Brush.sweepGradient(QuantisBrandGradient)

fun quantisDiagonalBrush(): Brush = Brush.linearGradient(QuantisBrandGradient)

private val QuantisColors = darkColorScheme(
    primary = QuantisMagenta,
    onPrimary = Color.White,
    secondary = QuantisBlue,
    onSecondary = Color.White,
    tertiary = QuantisViolet,
    background = QuantisBlack,
    onBackground = QuantisText,
    surface = QuantisSurface,
    onSurface = QuantisText,
    surfaceVariant = Color(0xFF1C1C24),
    onSurfaceVariant = QuantisMuted,
    error = Color(0xFFFF6B6B),
    onError = Color.White,
)

private val QuantisTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
)

@Composable
fun VoiceAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuantisColors,
        typography = QuantisTypography,
        content = content,
    )
}
