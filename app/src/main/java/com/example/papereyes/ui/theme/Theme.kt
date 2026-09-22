package com.example.papereyes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CopperLight,
    onPrimary = Color(0xFF55250D),
    primaryContainer = Color(0xFF713A20),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = ChalkMuted,
    onSecondary = Color(0xFF29302D),
    secondaryContainer = DarkPaperMuted,
    onSecondaryContainer = Chalk,
    tertiary = Color(0xFFA4D5B3),
    onTertiary = Color(0xFF0E3822),
    background = DarkDesk,
    onBackground = Chalk,
    surface = DarkPaper,
    onSurface = Chalk,
    surfaceVariant = DarkPaperMuted,
    onSurfaceVariant = ChalkMuted,
    outline = Color(0xFF89918C),
    outlineVariant = Color(0xFF404743),
    error = Color(0xFFFFB3B4),
    onError = Color(0xFF5F1219),
    errorContainer = Color(0xFF7D2930),
    onErrorContainer = ErrorSoft
)

private val LightColorScheme = lightColorScheme(
    primary = Copper,
    onPrimary = Color.White,
    primaryContainer = CopperSoft,
    onPrimaryContainer = CopperDark,
    secondary = InkMuted,
    onSecondary = Color.White,
    secondaryContainer = PaperMuted,
    onSecondaryContainer = Ink,
    tertiary = Success,
    onTertiary = Color.White,
    tertiaryContainer = SuccessSoft,
    onTertiaryContainer = Color(0xFF123923),
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = PaperMuted,
    onSurfaceVariant = InkMuted,
    outline = Rule,
    outlineVariant = Color(0xFFD0C9BE),
    error = ErrorInk,
    onError = Color.White,
    errorContainer = ErrorSoft,
    onErrorContainer = Color(0xFF641D22)
)

private val PaperEyesShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun PaperEyesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = PaperEyesShapes,
        content = content
    )
}
