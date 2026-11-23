package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class Dera3SemanticColors(
    val safe: Color,
    val safeContainer: Color,
    val caution: Color,
    val cautionContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val heroCardStart: Color,
    val heroCardEnd: Color,
)

private val LightSemanticColors = Dera3SemanticColors(
    safe = EmeraldSafeDark,
    safeContainer = EmeraldSoftLight,
    caution = AmberCautionDark,
    cautionContainer = AmberSoftLight,
    danger = CrimsonAlert,
    dangerContainer = CrimsonSoftLight,
    heroCardStart = Color(0xFFD32F2F),
    heroCardEnd = Color(0xFF991B1B),
)

private val DarkSemanticColors = Dera3SemanticColors(
    safe = EmeraldSafe,
    safeContainer = EmeraldSoftDark,
    caution = AmberCaution,
    cautionContainer = AmberSoftDark,
    danger = Color(0xFFEF4444),
    dangerContainer = CrimsonSoftDark,
    heroCardStart = Color(0xFFB91C1C),
    heroCardEnd = Color(0xFF7F1D1D),
)

val LocalDera3Colors = staticCompositionLocalOf { LightSemanticColors }

private val Dera3LightColorScheme = lightColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = NavyDeep,
    secondary = EmeraldSafeDark,
    onSecondary = Color.White,
    secondaryContainer = EmeraldSoftLight,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = AmberCautionDark,
    onTertiary = Color.White,
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = CrimsonSoftLight,
    onErrorContainer = Color(0xFF7F1D1D),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = Color(0xFFCBD5E1),
)

private val Dera3DarkColorScheme = darkColorScheme(
    primary = CyanBright,
    onPrimary = NavyDeep,
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = EmeraldSafe,
    onSecondary = NavyDeep,
    secondaryContainer = EmeraldSoftDark,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = AmberCaution,
    onTertiary = NavyDeep,
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = CrimsonSoftDark,
    onErrorContainer = Color(0xFFFECACA),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF284166),
)

val Dera3Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun Dera3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) Dera3DarkColorScheme else Dera3LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalDera3Colors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Dera3Typography,
            shapes = Dera3Shapes,
            content = content,
        )
    }
}
