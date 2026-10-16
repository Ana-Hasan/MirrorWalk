package com.mirrorwalk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mirrorwalk.R

val Navy = Color(0xFF0C131B)
val Surface = Color(0xFF16232B)
val SurfaceElevated = Color(0xFF1D3037)
val Mint = Color(0xFF78F2C2)
val Lime = Color(0xFFC7F36A)
val Ink = Color(0xFFF3FBF8)
val Muted = Color(0xFFD2E0DC)
val Danger = Color(0xFFFFAAA0)

// Manrope is bundled with the app, so this typeface renders identically on
// every Android phone rather than depending on a device-installed font.
val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Light),
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold)
)

// Space Grotesk gives supporting subtitles a sharper, sport-tech voice while
// Manrope remains the readable workhorse for the rest of the interface.
val SubtitleFont = FontFamily(
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

// Nexa is a commercial app font. Use this safe display fallback until a
// licensed Nexa .ttf/.otf is supplied and added to res/font.
val MirrorWalkBrandFont = FontFamily.SansSerif

private val MirrorColors = darkColorScheme(
    primary = Mint,
    secondary = Lime,
    background = Navy,
    surface = Surface,
    surfaceVariant = SurfaceElevated,
    onPrimary = Navy,
    onSecondary = Navy,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Ink,
    error = Danger
)

private val MirrorShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private val BaseTypography = androidx.compose.material3.Typography()

private fun TextStyle.inManrope() = copy(fontFamily = Manrope)

// This Compose Material 3 version has no defaultFontFamily constructor
// parameter, so retain every Material default text metric and replace only the
// family in each style.
private val MirrorTypography = androidx.compose.material3.Typography(
    displayLarge = BaseTypography.displayLarge.inManrope(),
    displayMedium = BaseTypography.displayMedium.inManrope(),
    displaySmall = BaseTypography.displaySmall.inManrope(),
    headlineLarge = BaseTypography.headlineLarge.inManrope(),
    headlineMedium = BaseTypography.headlineMedium.inManrope(),
    headlineSmall = BaseTypography.headlineSmall.inManrope(),
    titleLarge = BaseTypography.titleLarge.inManrope(),
    titleMedium = BaseTypography.titleMedium.inManrope(),
    titleSmall = BaseTypography.titleSmall.inManrope(),
    bodyLarge = BaseTypography.bodyLarge.inManrope(),
    bodyMedium = BaseTypography.bodyMedium.inManrope(),
    bodySmall = BaseTypography.bodySmall.copy(
        fontFamily = SubtitleFont,
        fontWeight = FontWeight.Bold
    ),
    labelLarge = BaseTypography.labelLarge.inManrope(),
    labelMedium = BaseTypography.labelMedium.inManrope().copy(fontWeight = FontWeight.SemiBold),
    labelSmall = BaseTypography.labelSmall.inManrope().copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun MirrorWalkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MirrorColors,
        shapes = MirrorShapes,
        typography = MirrorTypography,
        content = content
    )
}
