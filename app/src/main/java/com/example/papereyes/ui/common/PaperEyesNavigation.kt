package com.example.papereyes.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class PaperEyesDestination(
    val route: String,
    val label: String
) {
    SCAN("scan", "Scan"),
    LIBRARY("library", "Library"),
    PROJECTS("projects", "Projects")
}

@Composable
fun PaperEyesNavigationBar(
    currentRoute: String,
    onDestinationSelected: (PaperEyesDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        PaperEyesDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination.route == currentRoute,
                onClick = { onDestinationSelected(destination) },
                icon = { DestinationGlyph(destination) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun DestinationGlyph(destination: PaperEyesDestination) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(24.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        when (destination) {
            PaperEyesDestination.SCAN -> {
                drawCircle(color, radius = size.minDimension * 0.27f, style = stroke)
                drawLine(
                    color,
                    Offset(size.width * 0.69f, size.height * 0.69f),
                    Offset(size.width * 0.88f, size.height * 0.88f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            PaperEyesDestination.LIBRARY -> {
                drawRect(
                    color,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.14f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.72f),
                    style = stroke
                )
                drawLine(
                    color,
                    Offset(size.width * 0.34f, size.height * 0.14f),
                    Offset(size.width * 0.34f, size.height * 0.86f),
                    strokeWidth = 1.8.dp.toPx()
                )
            }
            PaperEyesDestination.PROJECTS -> {
                drawRect(
                    color,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.3f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.56f),
                    style = stroke
                )
                drawLine(
                    color,
                    Offset(size.width * 0.18f, size.height * 0.3f),
                    Offset(size.width * 0.34f, size.height * 0.15f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color,
                    Offset(size.width * 0.34f, size.height * 0.15f),
                    Offset(size.width * 0.55f, size.height * 0.15f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color,
                    Offset(size.width * 0.55f, size.height * 0.15f),
                    Offset(size.width * 0.65f, size.height * 0.3f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
