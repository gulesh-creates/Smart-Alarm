package com.zenith.study.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.study.domain.engine.StatisticsReport
import com.zenith.study.domain.model.Alarm
import com.zenith.study.ui.components.SemicircularGauge
import com.zenith.study.ui.theme.CanvasBlack
import com.zenith.study.ui.theme.CardCharcoal
import com.zenith.study.ui.theme.CardSubtle
import com.zenith.study.ui.theme.NeonLime
import com.zenith.study.ui.theme.NeonLimeSubtle
import com.zenith.study.ui.theme.TextInverse
import com.zenith.study.ui.theme.TextMuted
import com.zenith.study.ui.theme.TextPrimary
import com.zenith.study.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    nextAlarm: Alarm?,
    statisticsReport: StatisticsReport,
    dailyGoalMinutes: Int,
    is24Hour: Boolean,
    onStartSessionClick: () -> Unit,
    onNextAlarmToggle: (Alarm, Boolean) -> Unit,
    onTestAlarmClick: (Alarm) -> Unit,
    onViewAllAlarmsClick: () -> Unit,
    onNewAlarmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hi Alex 👋",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Ready to focus? $todayFormatted",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            IconButton(
                onClick = onNewAlarmClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(NeonLime)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Alarm",
                    tint = TextInverse
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Card (Neon Lime Background)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartSessionClick() },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = NeonLime)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(CanvasBlack.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DEEP WORK SPRINT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextInverse,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Start a Study\nSession",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 28.sp,
                    color = TextInverse
                )

                Text(
                    text = "25 min • Focus block with Flip-to-Start",
                    fontSize = 13.sp,
                    color = TextInverse.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(TextInverse)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Start Session",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CanvasBlack.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = TextInverse,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Today's Progress Semicircular Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CardCharcoal)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Focus Goal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(NeonLimeSubtle)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${dailyGoalMinutes / 60}h Goal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonLime
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SemicircularGauge(progressFraction = statisticsReport.goalProgressFraction)

                Spacer(modifier = Modifier.height(16.dp))

                // Metric Triad Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val hrs = statisticsReport.todayMinutes / 60
                    val mins = statisticsReport.todayMinutes % 60
                    val timeStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

                    MetricColumn(
                        icon = Icons.Default.HourglassBottom,
                        value = timeStr,
                        subLabel = "Focus Time"
                    )
                    MetricColumn(
                        icon = Icons.Default.Check,
                        value = "${statisticsReport.todaySessionsCount} done",
                        subLabel = "Sessions"
                    )
                    MetricColumn(
                        icon = Icons.Default.LocalFireDepartment,
                        value = "${statisticsReport.streakDays} days",
                        subLabel = "Streak"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Next Alarm Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Next Alarm",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "View All",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonLime,
                modifier = Modifier.clickable { onViewAllAlarmsClick() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (nextAlarm != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardCharcoal)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = nextAlarm.formatTime(is24Hour),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = nextAlarm.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(CardSubtle)
                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = nextAlarm.getDaysSummary(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                }
                                if (nextAlarm.walkToDismiss) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9999.dp))
                                            .background(NeonLimeSubtle)
                                            .padding(horizontal = 9.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "🏃 ${nextAlarm.stepTarget} Steps",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonLime
                                        )
                                    }
                                }
                            }
                        }

                        Switch(
                            checked = nextAlarm.isEnabled,
                            onCheckedChange = { onNextAlarmToggle(nextAlarm, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextInverse,
                                checkedTrackColor = NeonLime,
                                uncheckedThumbColor = TextPrimary,
                                uncheckedTrackColor = CardSubtle
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(NeonLimeSubtle)
                                .clickable { onTestAlarmClick(nextAlarm) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = NeonLime,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Test Ringing Now",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonLime
                                )
                            }
                        }

                        Text(
                            text = "Smart Alarm",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardCharcoal)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No alarms scheduled. Tap '+' to create one.",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun MetricColumn(
    icon: ImageVector,
    value: String,
    subLabel: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CardSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonLime,
                modifier = Modifier.size(18.dp)
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
            text = subLabel,
            fontSize = 10.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
    }
}

