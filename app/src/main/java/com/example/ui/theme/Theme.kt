package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PartPilotColorScheme = lightColorScheme(
    primary = WorkshopTeal,
    onPrimary = Color.White,
    primaryContainer = WorkshopTealLight,
    onPrimaryContainer = WorkshopTealDark,
    secondary = WorkshopTealDark,
    onSecondary = Color.White,
    background = WorkshopBg,
    onBackground = WorkshopText,
    surface = WorkshopSurface,
    onSurface = WorkshopText,
    surfaceVariant = WorkshopBg,
    onSurfaceVariant = WorkshopTextMuted,
    outline = WorkshopBorder
)

val WorkshopShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp), // 14px radius specified in prompt
    large = RoundedCornerShape(18.dp)
)

@Composable
fun PartPilotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PartPilotColorScheme,
        typography = WorkshopTypography,
        shapes = WorkshopShapes,
        content = content
    )
}
