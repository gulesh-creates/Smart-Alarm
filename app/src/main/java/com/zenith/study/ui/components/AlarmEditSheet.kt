package com.zenith.study.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.study.domain.model.Alarm
import com.zenith.study.ui.theme.BorderMedium
import com.zenith.study.ui.theme.CardCharcoal
import com.zenith.study.ui.theme.CardElevated
import com.zenith.study.ui.theme.CardSubtle
import com.zenith.study.ui.theme.NeonLime
import com.zenith.study.ui.theme.TextInverse
import com.zenith.study.ui.theme.TextMuted
import com.zenith.study.ui.theme.TextPrimary
import com.zenith.study.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    sheetState: SheetState,
    initialAlarm: Alarm?,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    var hour by remember { mutableStateOf(initialAlarm?.hour ?: 7) }
    var minute by remember { mutableStateOf(initialAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "Deep Study Block") }
    var selectedDays by remember { mutableStateOf(initialAlarm?.daysOfWeek ?: emptySet()) }
    var walkToDismiss by remember { mutableStateOf(initialAlarm?.walkToDismiss ?: true) }
    var stepTarget by remember { mutableStateOf(initialAlarm?.stepTarget ?: 30) }
    var customStepsText by remember { mutableStateOf("") }
    var isVibrationEnabled by remember { mutableStateOf(initialAlarm?.isVibrationEnabled ?: true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardCharcoal,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (initialAlarm == null) "New Alarm" else "Edit Alarm",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Time Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardElevated, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Stepper
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Hour",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "-",
                            fontSize = 22.sp,
                            color = NeonLime,
                            modifier = Modifier
                                .clickable { hour = if (hour > 0) hour - 1 else 23 }
                                .padding(8.dp)
                        )
                        Text(
                            text = String.format("%02d", hour),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "+",
                            fontSize = 22.sp,
                            color = NeonLime,
                            modifier = Modifier
                                .clickable { hour = (hour + 1) % 24 }
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))
                Text(text = ":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.width(24.dp))

                // Minute Stepper
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Minute",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "-",
                            fontSize = 22.sp,
                            color = NeonLime,
                            modifier = Modifier
                                .clickable { minute = if (minute > 0) minute - 1 else 59 }
                                .padding(8.dp)
                        )
                        Text(
                            text = String.format("%02d", minute),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "+",
                            fontSize = 22.sp,
                            color = NeonLime,
                            modifier = Modifier
                                .clickable { minute = (minute + 1) % 60 }
                                .padding(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Label Input
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Alarm Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonLime,
                    unfocusedBorderColor = BorderMedium,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = NeonLime,
                    unfocusedLabelColor = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Repeat Weekdays Grid
            Text(
                text = "REPEAT DAYS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DayOfWeek.values().forEach { day ->
                    val isSelected = day in selectedDays
                    val shortName = day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) NeonLime else CardSubtle)
                            .clickable {
                                selectedDays = if (isSelected) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shortName,
                            color = if (isSelected) TextInverse else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Walk to Dismiss Challenge Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Walk to Dismiss Challenge",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Requires steps to silence ringing alarm",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = walkToDismiss,
                    onCheckedChange = { walkToDismiss = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextInverse,
                        checkedTrackColor = NeonLime,
                        uncheckedThumbColor = TextPrimary,
                        uncheckedTrackColor = CardSubtle
                    )
                )
            }

            if (walkToDismiss) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20, 30, 50).forEach { steps ->
                        val isSelected = (stepTarget == steps && customStepsText.isBlank())
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(if (isSelected) NeonLime else CardSubtle)
                                .clickable {
                                    stepTarget = steps
                                    customStepsText = ""
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$steps Steps",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TextInverse else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save & Cancel Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CardElevated)
                ) {
                    Text("Cancel", color = TextPrimary)
                }

                Button(
                    onClick = {
                        val finalSteps = customStepsText.toIntOrNull() ?: stepTarget
                        val alarmToSave = (initialAlarm ?: Alarm(hour = hour, minute = minute)).copy(
                            hour = hour,
                            minute = minute,
                            label = label.ifBlank { "Study Alarm" },
                            daysOfWeek = selectedDays,
                            walkToDismiss = walkToDismiss,
                            stepTarget = finalSteps.coerceIn(5, 500),
                            isVibrationEnabled = isVibrationEnabled,
                            isEnabled = true
                        )
                        onSave(alarmToSave)
                    },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                ) {
                    Text("Save Alarm", color = TextInverse, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

