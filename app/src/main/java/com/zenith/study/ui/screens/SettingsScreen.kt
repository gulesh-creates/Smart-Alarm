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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.study.domain.model.UserSettings
import com.zenith.study.ui.theme.BorderSubtle
import com.zenith.study.ui.theme.CanvasBlack
import com.zenith.study.ui.theme.CardCharcoal
import com.zenith.study.ui.theme.CardElevated
import com.zenith.study.ui.theme.CardSubtle
import com.zenith.study.ui.theme.Danger
import com.zenith.study.ui.theme.NeonLime
import com.zenith.study.ui.theme.TextInverse
import com.zenith.study.ui.theme.TextMuted
import com.zenith.study.ui.theme.TextPrimary
import com.zenith.study.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    userSettings: UserSettings,
    onUpdateDailyGoal: (Int) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggle24Hour: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleAutoFlip: (Boolean) -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showGoalMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = CardCharcoal,
            title = {
                Text(
                    text = "Reset All Data?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will delete all saved alarms, clear study session history, and restore factory preferences.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) {
                    Text("Reset Everything", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showResetDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CardElevated)
                ) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text(
            text = "Preferences & Android platform architecture",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Preferences Group
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardCharcoal)
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                // Daily Study Goal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Study Goal",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Target focus time per day",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(CardSubtle)
                                .clickable { showGoalMenu = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${userSettings.dailyGoalMinutes / 60} Hours",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonLime
                            )
                        }

                        DropdownMenu(
                            expanded = showGoalMenu,
                            onDismissRequest = { showGoalMenu = false },
                            modifier = Modifier.background(CardCharcoal)
                        ) {
                            listOf(60, 120, 180, 240, 300).forEach { mins ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${mins / 60} Hours (${mins}m)",
                                            color = TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        onUpdateDailyGoal(mins)
                                        showGoalMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(BorderSubtle))

                // Sound On/Off
                SettingsSwitchRow(
                    title = "Audio Synthesizer & Tones",
                    subtitle = "Audible alarm ringtones and timer completion",
                    checked = userSettings.isSoundEnabled,
                    onCheckedChange = onToggleSound
                )

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(BorderSubtle))

                // 24-Hour Time Format
                SettingsSwitchRow(
                    title = "24-Hour Time Format",
                    subtitle = "Toggle between 12h AM/PM and 24h digital time",
                    checked = userSettings.is24HourFormat,
                    onCheckedChange = onToggle24Hour
                )

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(BorderSubtle))

                // Vibration & Haptics
                SettingsSwitchRow(
                    title = "Vibration & Haptics",
                    subtitle = "Vibrate during alarm ringing and timer flips",
                    checked = userSettings.isVibrationEnabled,
                    onCheckedChange = onToggleVibration
                )

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(BorderSubtle))

                // Flip-to-start Sensor Toggle
                SettingsSwitchRow(
                    title = "Face-Down Auto Detection",
                    subtitle = "Start timer automatically when phone is placed face down",
                    checked = userSettings.autoFlipEnabled,
                    onCheckedChange = onToggleAutoFlip
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Android Production Architecture Disclosure Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardCharcoal)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Android Platform Architecture",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonLime
                )
                Text(
                    text = "Native Android features powering this production app:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                ArchitectureRow(
                    pill = "Exact Alarms",
                    desc = "Uses AlarmManager.setAlarmClock() so alarms fire reliably through Doze and device standby."
                )
                ArchitectureRow(
                    pill = "Lock Screen",
                    desc = "Uses Notification.setFullScreenIntent() + showWhenLocked to launch over keyguard."
                )
                ArchitectureRow(
                    pill = "Step Sensor",
                    desc = "Hardware Sensor.TYPE_STEP_DETECTOR counts discrete steps with ACTIVITY_RECOGNITION."
                )
                ArchitectureRow(
                    pill = "Background Services",
                    desc = "ForegroundService with persistent notification keeps alarm audio and timer reliable."
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Data Button
        Button(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(9999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CardCharcoal)
        ) {
            Text(
                text = "Reset All Data",
                color = Danger,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextInverse,
                checkedTrackColor = NeonLime,
                uncheckedThumbColor = TextPrimary,
                uncheckedTrackColor = CardSubtle
            )
        )
    }
}

@Composable
private fun ArchitectureRow(
    pill: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(CardSubtle)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = pill,
                color = NeonLime,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = desc,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

