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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.zenith.study.ui.theme.BorderMedium
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
    onUpdateUserName: (String) -> Unit,
    onUpdateDailyGoal: (Int) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggle24Hour: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleVolumeButtons: (Boolean) -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showGoalMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(userSettings.userName) }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = CardCharcoal,
            title = {
                Text(
                    text = "Your Display Name",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your preferred name for greetings on the home dashboard. Android does not provide a universal, permission-free owner-name API.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 30) nameInput = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("e.g. Alex or Gulesh") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonLime,
                            unfocusedBorderColor = BorderMedium,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${nameInput.length}/30 characters",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateUserName(nameInput.trim())
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                ) {
                    Text("Save", color = TextInverse, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (userSettings.userName.isNotBlank()) {
                        Button(
                            onClick = {
                                onUpdateUserName("")
                                nameInput = ""
                                showNameDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardElevated)
                        ) {
                            Text("Clear", color = Danger)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(
                        onClick = { showNameDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CardElevated)
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }
                }
            }
        )
    }

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
                    text = "This will delete all saved alarms, clear study session history, remove to-do tasks, and restore default preferences.",
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
            text = "Preferences, profile & device controls",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Profile / User Name Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardCharcoal)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        nameInput = userSettings.userName
                        showNameDialog = true
                    }
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CardSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = NeonLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Your Name",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (userSettings.userName.isNotBlank()) userSettings.userName else "Not set (Tap to configure)",
                            fontSize = 12.sp,
                            color = if (userSettings.userName.isNotBlank()) NeonLime else TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                                DropMenuItemCustom(
                                    text = "${mins / 60} Hours (${mins}m)",
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
                    subtitle = "Audible alarm ringtones and timer completion alerts",
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
                    subtitle = "Vibrate during alarm ringing and timer completion",
                    checked = userSettings.isVibrationEnabled,
                    onCheckedChange = onToggleVibration
                )

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(BorderSubtle))

                // Volume Button Timer Controls
                SettingsSwitchRow(
                    title = "Volume Button Timer Controls",
                    subtitle = "While Timer screen is active: Volume Up = Start/Resume, Volume Down = Pause. System volume will not change. (Use notification actions for background control).",
                    checked = userSettings.volumeButtonsEnabled,
                    onCheckedChange = onToggleVolumeButtons
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
private fun DropMenuItemCustom(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 13.sp
            )
        },
        onClick = onClick
    )
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

        Spacer(modifier = Modifier.width(12.dp))

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
