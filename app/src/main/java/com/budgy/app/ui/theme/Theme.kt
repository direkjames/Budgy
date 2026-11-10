package com.budgy.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Budgy palette — taken from the money-bag icon:
 * deep emerald (trust, money) as primary, warm gold as the highlight.
 */
private val Emerald = Color(0xFF0E6E52)
private val EmeraldDeep = Color(0xFF094A37)
private val Gold = Color(0xFFE8A317)

private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EFE3),
    onPrimaryContainer = Color(0xFF04392A),
    secondary = Color(0xFFB57A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDF0CC),
    onSecondaryContainer = Color(0xFF4F3500),
    tertiary = Color(0xFF3E6CD8),
    background = Color(0xFFF6F7F4),
    onBackground = Color(0xFF131915),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF131915),
    surfaceVariant = Color(0xFFEFF2EE),
    onSurfaceVariant = Color(0xFF5F6A64),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFCFA),
    surfaceContainer = Color(0xFFF2F4F1),
    surfaceContainerHigh = Color(0xFFECEFEB),
    surfaceContainerHighest = Color(0xFFE6E9E5),
    outline = Color(0xFFB9C2BC),
    outlineVariant = Color(0xFFE3E7E2),
    error = Color(0xFFD9433E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5BD0A0),
    onPrimary = Color(0xFF00291C),
    primaryContainer = Color(0xFF14503C),
    onPrimaryContainer = Color(0xFFC3F0DC),
    secondary = Color(0xFFF4BE48),
    onSecondary = Color(0xFF3D2A00),
    secondaryContainer = Color(0xFF4A3602),
    onSecondaryContainer = Color(0xFFFFE3A3),
    tertiary = Color(0xFF8DA9F5),
    background = Color(0xFF0C110F),
    onBackground = Color(0xFFE9EFEB),
    surface = Color(0xFF151C19),
    onSurface = Color(0xFFE9EFEB),
    surfaceVariant = Color(0xFF1E2723),
    onSurfaceVariant = Color(0xFF9CA8A1),
    surfaceContainerLowest = Color(0xFF0A0E0C),
    surfaceContainerLow = Color(0xFF121916),
    surfaceContainer = Color(0xFF18201C),
    surfaceContainerHigh = Color(0xFF1F2824),
    surfaceContainerHighest = Color(0xFF27312C),
    outline = Color(0xFF4A5650),
    outlineVariant = Color(0xFF26302B),
    error = Color(0xFFFF7A70),
    onError = Color(0xFF3A0705),
)

/** Colors with meaning beyond Material roles. */
@Immutable
data class BudgyColors(
    val income: Color,
    val expense: Color,
    val warning: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val gold: Color,
    val cardBorder: Color,
)

private val LightBudgy = BudgyColors(
    income = Color(0xFF12A15F),
    expense = Color(0xFFDF4A43),
    warning = Color(0xFFDB8E00),
    heroStart = Color(0xFF0F7A5B),
    heroEnd = EmeraldDeep,
    gold = Gold,
    cardBorder = Color(0xFFE6EAE5),
)

private val DarkBudgy = BudgyColors(
    income = Color(0xFF52D394),
    expense = Color(0xFFFF7A70),
    warning = Color(0xFFF4BE48),
    heroStart = Color(0xFF13664D),
    heroEnd = Color(0xFF0A3A2C),
    gold = Color(0xFFF4BE48),
    cardBorder = Color(0xFF222B27),
)

val LocalBudgyColors = staticCompositionLocalOf { LightBudgy }
val LocalCurrency = staticCompositionLocalOf { "₱" }

private val BudgyTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Tabular figures so amounts line up in lists. */
val Numeric = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun BudgyTheme(dark: Boolean, currency: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalBudgyColors provides if (dark) DarkBudgy else LightBudgy,
        LocalCurrency provides currency,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = BudgyTypography,
            content = content,
        )
    }
}

object Budgy {
    val colors: BudgyColors
        @Composable get() = LocalBudgyColors.current
    val currency: String
        @Composable get() = LocalCurrency.current
}
