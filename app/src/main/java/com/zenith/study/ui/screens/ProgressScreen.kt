package com.zenith.study.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.study.domain.engine.StatisticsReport
import com.zenith.study.domain.model.FocusSession
import com.zenith.study.ui.components.CircularProgressRing
import com.zenith.study.ui.theme.CanvasBlack
import com.zenith.study.ui.theme.CardCharcoal
import com.zenith.study.ui.theme.CardSubtle
import com.zenith.study.ui.theme.NeonLime
import com.zenith.study.ui.theme.TextInverse
import com.zenith.study.ui.theme.TextMuted
import com.zenith.study.ui.theme.TextPrimary
import com.zenith.study.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeRange(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month")
}

@Composable
fun ProgressScreen(
    statisticsReport: StatisticsReport,
    recentSessions: List<FocusSession>,
    dailyGoalMinutes: Int,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedRange by remember { mutableStateOf(TimeRange.DAY) }

    val displayMinutes = when (selectedRange) {
        TimeRange.DAY -> statisticsReport.todayMinutes
        TimeRange.WEEK -> statisticsReport.weekMinutes
        TimeRange.MONTH -> statisticsReport.monthMinutes
    }

    val displaySub = when (selectedRange) {
        TimeRange.DAY -> "Goal: ${dailyGoalMinutes} min (${(statisticsReport.goalProgressFraction * 100).toInt()}%)"
        TimeRange.WEEK -> "7-Day Total: ${(statisticsReport.weekMinutes / 60.0).format(1)} hrs"
        TimeRange.MONTH -> "30-Day Total: ${(statisticsReport.monthMinutes / 60.0).format(1)} hrs"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Activity",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text(
            text = "Study progress & discipline",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Segmented Control (Day / Week / Month)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9999.dp))
                .background(CardCharcoal)
                .padding(4.dp)
        ) {
            TimeRange.values().forEach { range ->
                val isSelected = (selectedRange == range)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isSelected) NeonLime else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { selectedRange = range }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) TextInverse else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big Center Circular Activity Ring
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val ringFraction = when (selectedRange) {
                TimeRange.DAY -> statisticsReport.goalProgressFraction
                TimeRange.WEEK -> (statisticsReport.weekMinutes.toFloat() / (dailyGoalMinutes * 7).toFloat()).coerceIn(0f, 1f)
                TimeRange.MONTH -> (statisticsReport.monthMinutes.toFloat() / (dailyGoalMinutes * 30).toFloat()).coerceIn(0f, 1f)
            }

            CircularProgressRing(
                progressFraction = ringFraction,
                diameter = 210.dp,
                strokeWidth = 14.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedRange.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Text(
                        text = "$displayMinutes",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Minutes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonLime
                    )
                    Text(
                        text = displaySub,
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3-Metrics Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardCharcoal)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(
                    icon = Icons.Default.Check,
                    value = "${statisticsReport.todaySessionsCount}",
                    label = "Sessions"
                )
                MetricItem(
                    icon = Icons.Default.HourglassBottom,
                    value = "${statisticsReport.longestBlockMinutes}m",
                    label = "Max Block"
                )
                MetricItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${statisticsReport.streakDays}d",
                    label = "Streak"
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 7-Day Activity Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardCharcoal)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "7-Day Focus Hours",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${(statisticsReport.weekMinutes / 60.0).format(1)} hrs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonLime
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Chart Bars Wrap
                val maxMinutes = statisticsReport.weeklyDistribution.maxOfOrNull { it.totalMinutes }?.coerceAtLeast(60) ?: 60

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    statisticsReport.weeklyDistribution.forEach { dayActivity ->
                        val barHeightFraction = (dayActivity.totalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.08f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(barHeightFraction)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(if (dayActivity.isToday) NeonLime else CardSubtle)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dayActivity.shortLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dayActivity.isToday) NeonLime else TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Sessions Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Sessions",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (recentSessions.isNotEmpty()) {
                Text(
                    text = "Clear",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonLime,
                    modifier = Modifier.clickable { onClearHistory() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (recentSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recorded sessions yet.\nComplete a timer block to see your focus log.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            recentSessions.take(6).forEach { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCharcoal)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = NeonLime,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = session.subject,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                val dateStr = remember(session.startedAtEpochMillis) {
                                    val localTime = Instant.ofEpochMilli(session.startedAtEpochMillis)
                                        .atZone(ZoneId.systemDefault())
                                    localTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault()))
                                }
                                Text(
                                    text = "${session.actualMinutes} min • $dateStr",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(NeonLime),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = TextInverse,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CardSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonLime,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextMuted
        )
    }
}

private fun Double.format(digits: Int) = String.format(Locale.getDefault(), "%.${digits}f", this)

