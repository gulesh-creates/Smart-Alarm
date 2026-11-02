package com.zenith.study.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.study.domain.model.Alarm
import com.zenith.study.ui.theme.BorderMedium
import com.zenith.study.ui.theme.BorderSubtle
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
import java.io.File
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
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var hour by remember(initialAlarm) { mutableIntStateOf(initialAlarm?.hour ?: 7) }
    var minute by remember(initialAlarm) { mutableIntStateOf(initialAlarm?.minute ?: 0) }
    var label by remember(initialAlarm) { mutableStateOf(initialAlarm?.label ?: "Deep Study Block") }
    var selectedDays by remember(initialAlarm) { mutableStateOf(initialAlarm?.daysOfWeek ?: emptySet()) }
    var walkToDismiss by remember(initialAlarm) { mutableStateOf(initialAlarm?.walkToDismiss ?: true) }
    var stepTarget by remember(initialAlarm) { mutableIntStateOf(initialAlarm?.stepTarget ?: 30) }
    var customStepsText by remember(initialAlarm) { mutableStateOf("") }
    var isVibrationEnabled by remember(initialAlarm) { mutableStateOf(initialAlarm?.isVibrationEnabled ?: true) }
    var showEmergencyDismiss by remember(initialAlarm) { mutableStateOf(initialAlarm?.showEmergencyDismiss ?: true) }
    var tone by remember(initialAlarm) { mutableStateOf(initialAlarm?.tone ?: "System default") }
    var toneUri by remember(initialAlarm) { mutableStateOf(initialAlarm?.toneUri) }
    var volume by remember(initialAlarm) { mutableFloatStateOf(initialAlarm?.volume ?: 1.0f) }

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (e: Exception) {
            Log.w("AlarmEditSheet", "Error releasing preview player", e)
        }
        previewPlayer = null
        isPreviewPlaying = false
    }

    fun startPreview() {
        stopPreview()
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val uriToPlay = if (toneUri != null) {
                if (toneUri!!.startsWith("/")) Uri.fromFile(File(toneUri!!)) else Uri.parse(toneUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(context, uriToPlay)
                setVolume(volume, volume)
                isLooping = true
                prepare()
                start()
            }
            previewPlayer = player
            isPreviewPlaying = true
        } catch (e: Exception) {
            Log.e("AlarmEditSheet", "Failed to start preview audio", e)
            stopPreview()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    // System Ringtone Picker Launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                toneUri = pickedUri.toString()
                val ringtone = RingtoneManager.getRingtone(context, pickedUri)
                tone = ringtone?.getTitle(context) ?: "System Ringtone"
            } else {
                toneUri = null
                tone = "System default"
            }
            if (isPreviewPlaying) startPreview()
        }
    }

    // Audio File Picker Launcher
    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            try {
                val dir = File(context.filesDir, "alarm_tones").apply { mkdirs() }
                val destFile = File(dir, "custom_tone_${System.currentTimeMillis()}.mp3")
                context.contentResolver.openInputStream(pickedUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                toneUri = destFile.absolutePath
                tone = "Audio File (${destFile.name.take(16)}...)"
            } catch (e: Exception) {
                Log.e("AlarmEditSheet", "Failed copying custom audio, using raw URI", e)
                toneUri = pickedUri.toString()
                tone = "Custom Audio"
            }
            if (isPreviewPlaying) startPreview()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = CardCharcoal,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
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
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
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
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
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
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
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
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
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

            Spacer(modifier = Modifier.height(20.dp))

            // Sound & Ringtone Selection Section
            Text(
                text = "ALARM SOUND & VOLUME",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardElevated)
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = NeonLime,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = tone,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (toneUri == null) "Default System Sound" else "Custom Tone",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Preview / Stop Button
                        Button(
                            onClick = {
                                if (isPreviewPlaying) stopPreview() else startPreview()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPreviewPlaying) Warning else NeonLime
                            ),
                            shape = RoundedCornerShape(9999.dp)
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPreviewPlaying) "Stop Preview" else "Play Preview",
                                tint = TextInverse,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPreviewPlaying) "Stop" else "Preview",
                                color = TextInverse,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sound Source Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (toneUri == null) NeonLimeSubtle else CardSubtle)
                                .clickable {
                                    toneUri = null
                                    tone = "System default"
                                    if (isPreviewPlaying) startPreview()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Default",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (toneUri == null) NeonLime else TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardSubtle)
                                .clickable {
                                    val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    }
                                    ringtonePickerLauncher.launch(pickerIntent)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "System Tones",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardSubtle)
                                .clickable {
                                    audioFilePickerLauncher.launch("audio/*")
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Audio File",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Volume Slider
                    Text(
                        text = "Volume: ${(volume * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            previewPlayer?.setVolume(it, it)
                        },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonLime,
                            activeTrackColor = NeonLime,
                            inactiveTrackColor = CardSubtle
                        )
                    )
                    Text(
                        text = "Per-alarm volume. Output is also governed by device alarm volume and Do Not Disturb policies.",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Walk to Dismiss Challenge Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

                Spacer(modifier = Modifier.height(14.dp))

                // Show Emergency Dismiss Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Emergency Dismiss",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (showEmergencyDismiss) {
                                "Emergency dismiss button is shown with confirmation."
                            } else {
                                "Strict mode: dismiss is locked until steps are completed. (Test ring always allows dismiss)."
                            },
                            fontSize = 11.sp,
                            color = if (showEmergencyDismiss) TextMuted else Warning
                        )
                    }
                    Switch(
                        checked = showEmergencyDismiss,
                        onCheckedChange = { showEmergencyDismiss = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextInverse,
                            checkedTrackColor = NeonLime,
                            uncheckedThumbColor = TextPrimary,
                            uncheckedTrackColor = CardSubtle
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save & Cancel Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        stopPreview()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CardElevated)
                ) {
                    Text("Cancel", color = TextPrimary)
                }

                Button(
                    onClick = {
                        stopPreview()
                        val finalSteps = customStepsText.toIntOrNull() ?: stepTarget
                        val alarmToSave = (initialAlarm ?: Alarm(hour = hour, minute = minute)).copy(
                            hour = hour,
                            minute = minute,
                            label = label.ifBlank { "Study Alarm" },
                            daysOfWeek = selectedDays,
                            walkToDismiss = walkToDismiss,
                            stepTarget = finalSteps.coerceIn(5, 500),
                            isVibrationEnabled = isVibrationEnabled,
                            showEmergencyDismiss = showEmergencyDismiss,
                            tone = tone,
                            toneUri = toneUri,
                            volume = volume,
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
