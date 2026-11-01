package com.zenith.study.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.study.ui.theme.CardSubtle
import com.zenith.study.ui.theme.NeonLime
import com.zenith.study.ui.theme.TextMuted
import com.zenith.study.ui.theme.TextPrimary

@Composable
fun SemicircularGauge(
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "gaugeProgress"
    )

    Box(
        modifier = modifier
            .width(220.dp)
            .height(130.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 14.dp.toPx()
            val diameter = size.width - strokeWidthPx
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

            // Background track (180 degrees from 180 to 360)
            drawArc(
                color = CardSubtle,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Neon Lime fill arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = NeonLime,
                    startAngle = 180f,
                    sweepAngle = 180f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "COMPLETED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = TextMuted
            )
        }
    }
}

