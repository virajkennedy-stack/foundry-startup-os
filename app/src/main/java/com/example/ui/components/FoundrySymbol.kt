package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FoundrySymbol(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val radiusX = w * 0.46f
        val radiusY = h * 0.46f
        val innerFactor = 0.14f

        val starPath = Path().apply {
            // Start at top tip
            moveTo(cx, cy - radiusY)
            // Arc to right tip
            quadraticTo(
                cx + radiusX * innerFactor, cy - radiusY * innerFactor,
                cx + radiusX, cy
            )
            // Arc to bottom tip
            quadraticTo(
                cx + radiusX * innerFactor, cy + radiusY * innerFactor,
                cx, cy + radiusY
            )
            // Arc to left tip
            quadraticTo(
                cx - radiusX * innerFactor, cy + radiusY * innerFactor,
                cx - radiusX, cy
            )
            // Arc back to top tip
            quadraticTo(
                cx - radiusX * innerFactor, cy - radiusY * innerFactor,
                cx, cy - radiusY
            )
            close()
        }

        drawPath(
            path = starPath,
            color = color
        )
    }
}

