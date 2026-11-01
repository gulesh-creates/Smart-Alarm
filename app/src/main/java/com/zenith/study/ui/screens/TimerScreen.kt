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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
    onSetDuration: (Int) -> Unit,
    onManualToggle: () -> Unit,
    onReset: () -> Unit,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showCustomDialog by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableStateOf(25) }

    val remainingSeconds = timerStatus.remainingMillis / 1000
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val progressFraction = if (timerStatus.totalDurationMillis > 0) {
        timerStatus.remainingMillis.toFloat() / timerStatus.totalDurationMillis.toFloat()
    } else 0f

    if (showCustomDialog) {
        CustomDurationDialog(
            initialMinutes = selectedMinutes,
            onDismiss = { showCustomDialog = false },
            onConfirm = { customMins ->
                selectedMinutes = customMins
                onSetDuration(customMins)
                showCustomDialog = false
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
                    text = "Flip phone face down to start",
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

        Spacer(modifier = Modifier.height(20.dp))

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
                            selectedMinutes = mins
                            onSetDuration(mins)
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
                    is TimerStatus.Ready -> Triple("READY TO FLIP", CardSubtle, TextSecondary)
                    is TimerStatus.Running -> Triple("STUDYING (FACE DOWN)", NeonLimeSubtle, NeonLime)
                    is TimerStatus.Paused -> Triple("PAUSED (FACE UP)", Warning.copy(alpha = 0.15f), Warning)
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

        // Flip Instruction Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardCharcoal)
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = NeonLime,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    val instructionTitle = when (timerStatus) {
                        is TimerStatus.Ready -> "Place phone face down on desk"
                        is TimerStatus.Running -> "Focus active — phone face down"
                        is TimerStatus.Paused -> "Phone lifted — timer paused"
                        is TimerStatus.Completed -> "Focus session complete!"
                    }
                    val instructionSub = when (timerStatus) {
                        is TimerStatus.Ready -> "Countdown will start automatically when face down."
                        is TimerStatus.Running -> "Keep phone face down. Lifting will pause."
                        is TimerStatus.Paused -> "Place face down again to resume countdown."
                        is TimerStatus.Completed -> "Minutes logged to your daily study progress."
                    }

                    Text(
                        text = instructionTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = instructionSub,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
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
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardCharcoal)
            ) {
                Text(
                    text = "End Early",
                    color = TextPrimary,
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
                    is TimerStatus.Running -> "Manual Pause" to Icons.Default.Pause
                    else -> "Start / Resume" to Icons.Default.PlayArrow
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

