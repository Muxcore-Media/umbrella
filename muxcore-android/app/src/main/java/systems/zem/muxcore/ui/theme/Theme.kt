package systems.zem.muxcore.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** MuxCore brand palette — mirrors media-ui-app index.css tokens. */
object MuxColors {
    val Accent = Color(0xFF3DB8A8)
    val AccentDim = Color(0xFF2E8A7E)
    val BgBase = Color(0xFF0B0C0F)
    val BgElevated = Color(0xFF15161B)
    val BgElevated2 = Color(0xFF1D1F26)
    val BorderSubtle = Color(0xFF23252B)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFA1A1AA)
    val TextTertiary = Color(0xFF6B6B73)
    val Success = Color(0xFF3DDC84)
    val Warning = Color(0xFFF5A623)
    val Danger = Color(0xFFEF4444)

    val LightBgBase = Color(0xFFF4F4F6)
    val LightBgElevated = Color(0xFFFFFFFF)
    val LightTextPrimary = Color(0xFF14151A)
}

private val DarkScheme = darkColorScheme(
    primary = MuxColors.Accent,
    onPrimary = Color(0xFF00201B),
    primaryContainer = MuxColors.AccentDim,
    onPrimaryContainer = Color(0xFFDFFAF4),
    secondary = Color(0xFFB1CCC5),
    onSecondary = Color(0xFF1C3530),
    secondaryContainer = Color(0xFF334B46),
    onSecondaryContainer = Color(0xFFCDE8E1),
    background = MuxColors.BgBase,
    onBackground = MuxColors.TextPrimary,
    surface = MuxColors.BgBase,
    onSurface = MuxColors.TextPrimary,
    surfaceVariant = MuxColors.BgElevated2,
    onSurfaceVariant = MuxColors.TextSecondary,
    surfaceContainer = MuxColors.BgElevated,
    surfaceContainerHigh = MuxColors.BgElevated2,
    surfaceContainerHighest = Color(0xFF25272F),
    surfaceContainerLow = Color(0xFF101115),
    surfaceContainerLowest = Color(0xFF08090B),
    outline = MuxColors.TextTertiary,
    outlineVariant = MuxColors.BorderSubtle,
    error = MuxColors.Danger,
    onError = Color.White,
    inverseSurface = MuxColors.TextPrimary,
    inverseOnSurface = MuxColors.BgBase,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF2E1),
    onPrimaryContainer = Color(0xFF00201B),
    background = MuxColors.LightBgBase,
    onBackground = MuxColors.LightTextPrimary,
    surface = MuxColors.LightBgBase,
    onSurface = MuxColors.LightTextPrimary,
    surfaceContainer = MuxColors.LightBgElevated,
    surfaceContainerHigh = Color(0xFFEDEDF0),
    outlineVariant = Color(0xFFDDDDE2),
    error = MuxColors.Danger,
)

private val MuxTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.4).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
)

val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun MuxTheme(
    themePref: String = "dark",
    content: @Composable () -> Unit,
) {
    val dark = when (themePref) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalIsDarkTheme provides dark) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = MuxTypography,
            content = content,
        )
    }
}
