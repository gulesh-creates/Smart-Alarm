package com.zenith.study.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenith.study.ui.theme.CardSubtle
import com.zenith.study.ui.theme.NeonLime

@Composable
fun CircularProgressRing(
    progressFraction: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 240.dp,
    strokeWidth: Dp = 16.dp,
    trackColor: Color = CardSubtle,
    fillColor: Color = NeonLime,
    content: @Composable () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "ringProgress"
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidth.toPx()
            val arcDiameter = size.width - strokeWidthPx
            val arcSize = Size(arcDiameter, arcDiameter)
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

            // Full circular track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx)
            )

            // Dynamic progress fill
            if (animatedProgress > 0f) {
                drawArc(
                    color = fillColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }

        content()
    }
}

