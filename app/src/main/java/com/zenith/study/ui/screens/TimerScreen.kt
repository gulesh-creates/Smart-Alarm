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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.zenith.study.domain.engine.TimerStatus
import com.zenith.study.domain.model.TimerMode
import com.zenith.study.ui.components.CircularProgressRing
import com.zenith.study.ui.components.CustomDurationDialog
import com.zenith.study.ui.theme.BorderSubtle
import com.zenith.study.ui.theme.CanvasBlack
import com.zenith.study.ui.theme.CardCharcoal
import com.zenith.study.ui.theme.CardElevated
import com.zenith.study.ui.theme.CardSubtle
import com.zenith.study.ui.theme.NeonLime
import com.zenith.study.ui.theme.NeonLimeSubtle
import com.zenith.study.ui.theme.TextInverse
import com.zenith.study.ui.theme.TextMuted
import com.zenith.study.ui.theme.TextPrimary
import com.zenith.study.ui.theme.TextSecondary
import com.zenith.study.ui.theme.Warning

@Composable
fun TimerScreen(
    timerStatus: TimerStatus,
    timerMode: TimerMode,
    selectedMinutes: Int,
    onSetDuration: (Int) -> Unit,
    onSetMode: (TimerMode) -> Unit,
    onManualToggle: () -> Unit,
    onReset: () -> Unit,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showCustomDialog by remember { mutableStateOf(false) }
    var pendingPendingModeChange by remember { mutableStateOf<TimerMode?>(null) }
    var pendingDurationChange by remember { mutableStateOf<Int?>(null) }

    val isSessionActive = timerStatus is TimerStatus.Running || timerStatus is TimerStatus.Paused

    val remainingSeconds = timerStatus.remainingMillis / 1000
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val progressFraction = if (timerStatus.totalDurationMillis > 0) {
        timerStatus.remainingMillis.toFloat() / timerStatus.totalDurationMillis.toFloat()
    } else 0f

    // Dialog for changing mode during active session
    if (pendingPendingModeChange != null) {
        val target = pendingPendingModeChange!!
        AlertDialog(
            onDismissRequest = { pendingPendingModeChange = null },
            containerColor = CardCharcoal,
            title = {
                Text("Change Timer Mode?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Switching to ${target.name.lowercase().replaceFirstChar { it.uppercase() }} Timer will end your current session early and record your completed minutes.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEndSession()
                        onSetMode(target)
                        pendingPendingModeChange = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                ) {
                    Text("Switch Mode", color = TextInverse, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { pendingPendingModeChange = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CardElevated)
                ) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }

    // Dialog for changing duration during active session
    if (pendingDurationChange != null) {
        val targetMins = pendingDurationChange!!
        AlertDialog(
            onDismissRequest = { pendingDurationChange = null },
            containerColor = CardCharcoal,
            title = {
                Text("Change Duration?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Setting a new duration will end your current session and reset the timer to $targetMins minutes.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEndSession()
                        onSetDuration(targetMins)
                        pendingDurationChange = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                ) {
                    Text("Apply & Reset", color = TextInverse, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { pendingDurationChange = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CardElevated)
                ) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }

    if (showCustomDialog) {
        CustomDurationDialog(
            initialMinutes = selectedMinutes,
            onDismiss = { showCustomDialog = false },
            onConfirm = { customMins ->
                showCustomDialog = false
                if (isSessionActive) {
                    pendingDurationChange = customMins
                } else {
                    onSetDuration(customMins)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Study Timer",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = if (timerMode == TimerMode.SMART) "Smart Screen-State Mode" else "Normal Precision Mode",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CardCharcoal)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Timer",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer Mode Toggle Selector (Normal vs Smart)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9999.dp))
                .background(CardCharcoal)
                .padding(4.dp)
        ) {
            // Normal Timer Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(if (timerMode == TimerMode.NORMAL) NeonLime else CardCharcoal)
                    .clickable {
                        if (timerMode != TimerMode.NORMAL) {
                            if (isSessionActive) {
                                pendingPendingModeChange = TimerMode.NORMAL
                            } else {
                                onSetMode(TimerMode.NORMAL)
                            }
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = if (timerMode == TimerMode.NORMAL) TextInverse else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Normal Timer",
                        color = if (timerMode == TimerMode.NORMAL) TextInverse else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Smart Timer Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(if (timerMode == TimerMode.SMART) NeonLime else CardCharcoal)
                    .clickable {
                        if (timerMode != TimerMode.SMART) {
                            if (isSessionActive) {
                                pendingPendingModeChange = TimerMode.SMART
                            } else {
                                onSetMode(TimerMode.SMART)
                            }
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (timerMode == TimerMode.SMART) TextInverse else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Smart Timer",
                        color = if (timerMode == TimerMode.SMART) TextInverse else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(25, 45, 60, 90).forEach { mins ->
                val isSelected = (selectedMinutes == mins && !showCustomDialog)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isSelected) NeonLime else CardCharcoal)
                        .clickable {
                            if (selectedMinutes != mins) {
                                if (isSessionActive) {
                                    pendingDurationChange = mins
                                } else {
                                    onSetDuration(mins)
                                }
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${mins}m",
                        color = if (isSelected) TextInverse else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val isCustomSelected = selectedMinutes !in listOf(25, 45, 60, 90)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(if (isCustomSelected) NeonLime else CardCharcoal)
                    .clickable { showCustomDialog = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCustomSelected) "${selectedMinutes}m" else "Custom",
                    color = if (isCustomSelected) TextInverse else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Circular Countdown Ring
        CircularProgressRing(
            progressFraction = progressFraction,
            diameter = 260.dp,
            strokeWidth = 16.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Status Badge
                val (badgeText, badgeBg, badgeTextColor) = when (timerStatus) {
                    is TimerStatus.Ready -> {
                        if (timerMode == TimerMode.SMART) {
                            Triple("ARMED • SCREEN OFF TO START", CardSubtle, NeonLime)
                        } else {
                            Triple("READY TO FOCUS", CardSubtle, TextSecondary)
                        }
                    }
                    is TimerStatus.Running -> {
                        if (timerMode == TimerMode.SMART) {
                            Triple("STUDYING (SCREEN OFF)", NeonLimeSubtle, NeonLime)
                        } else {
                            Triple("STUDYING ACTIVE", NeonLimeSubtle, NeonLime)
                        }
                    }
                    is TimerStatus.Paused -> {
                        if (timerMode == TimerMode.SMART) {
                            Triple("PAUSED (SCREEN ON)", Warning.copy(alpha = 0.15f), Warning)
                        } else {
                            Triple("PAUSED", Warning.copy(alpha = 0.15f), Warning)
                        }
                    }
                    is TimerStatus.Completed -> Triple("COMPLETED 🎉", NeonLimeSubtle, NeonLime)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(badgeBg)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeTextColor,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = timeFormatted,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-1.5).sp
                )

                Text(
                    text = "Focus Block • ${selectedMinutes} mins",
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mode Information & Behavior Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardCharcoal)
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (timerMode == TimerMode.SMART) Icons.Default.PowerSettingsNew else Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonLime,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    if (timerMode == TimerMode.SMART) {
                        Text(
                            text = "Smart Timer (Screen-State)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Turn display OFF to start/resume automatically. Turn display ON to pause.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "स्क्रीन बंद करने पर टाइमर शुरू होगा, स्क्रीन चालू करने पर पॉज होगा।",
                            fontSize = 11.sp,
                            color = NeonLime,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Normal Timer (Precision)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Manual start, pause and resume. Keeps running when display is on, off, or app is backgrounded.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onEndSession,
                enabled = isSessionActive,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardCharcoal)
            ) {
                Text(
                    text = "End Early",
                    color = if (isSessionActive) TextPrimary else TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onManualToggle,
                modifier = Modifier.weight(1.6f),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
            ) {
                val (buttonText, buttonIcon) = when (timerStatus) {
                    is TimerStatus.Running -> "Pause" to Icons.Default.Pause
                    is TimerStatus.Paused -> "Resume" to Icons.Default.PlayArrow
                    else -> {
                        if (timerMode == TimerMode.SMART) "Start Now" to Icons.Default.PlayArrow
                        else "Start" to Icons.Default.PlayArrow
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = buttonIcon,
                        contentDescription = null,
                        tint = TextInverse,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buttonText,
                        color = TextInverse,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
