package com.zenith.study.ui.ringing

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.study.alarm.AndroidAlarmScheduler
import com.zenith.study.sensor.StepDetector
import com.zenith.study.service.AlarmRingingService
import com.zenith.study.ui.MainActivity
import com.zenith.study.ui.components.CircularProgressRing
import com.zenith.study.ui.theme.CanvasBlack
import com.zenith.study.ui.theme.CardCharcoal
import com.zenith.study.ui.theme.CardElevated
import com.zenith.study.ui.theme.Danger
import com.zenith.study.ui.theme.NeonLime
import com.zenith.study.ui.theme.NeonLimeSubtle
import com.zenith.study.ui.theme.TextInverse
import com.zenith.study.ui.theme.TextMuted
import com.zenith.study.ui.theme.TextPrimary
import com.zenith.study.ui.theme.TextSecondary
import com.zenith.study.ui.theme.Warning
import com.zenith.study.ui.theme.ZenithTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RingingActivity : ComponentActivity() {

    private var stepDetector: StepDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and display above keyguard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val passedAlarmId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, -1L)
        val passedIsTest = intent.getBooleanExtra(AlarmRingingService.EXTRA_IS_TEST_RING, false)

        // Hardware step detector initialization
        stepDetector = StepDetector(
            context = this,
            onStepDetected = {
                AlarmRingingService.incrementStep()
            }
        )

        setContent {
            ZenithTheme {
                val activeAlarm by AlarmRingingService.activeRingingAlarmFlow.collectAsState()
                val isTestRing by AlarmRingingService.isTestRingFlow.collectAsState()
                val stepsTaken by AlarmRingingService.currentStepsFlow.collectAsState()

                val effectiveAlarmId = activeAlarm?.id ?: passedAlarmId
                val effectiveIsTest = isTestRing || passedIsTest

                DisposableEffect(Unit) {
                    stepDetector?.startListening()
                    onDispose {
                        stepDetector?.stopListening()
                    }
                }

                RingingScreen(
                    alarmLabel = activeAlarm?.label ?: "Study Alarm",
                    walkToDismiss = activeAlarm?.walkToDismiss ?: false,
                    targetSteps = activeAlarm?.stepTarget ?: 0,
                    stepsTaken = stepsTaken,
                    isStepSensorAvailable = stepDetector?.isAvailable == true,
                    showEmergencyDismiss = activeAlarm?.showEmergencyDismiss ?: true,
                    isTestRing = effectiveIsTest,
                    onDismissGoalReached = {
                        // 1. Silence audio
                        val dismissIntent = Intent(this@RingingActivity, AlarmRingingService::class.java).apply {
                            action = AlarmRingingService.ACTION_DISMISS_ALARM
                            putExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, effectiveAlarmId)
                        }
                        startService(dismissIntent)

                        // 2. Launch To-Do list on MainActivity
                        val mainIntent = Intent(this@RingingActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(MainActivity.EXTRA_SHOW_POST_DISMISS_TODO, true)
                            putExtra(MainActivity.EXTRA_TODO_ALARM_ID, effectiveAlarmId)
                            putExtra(MainActivity.EXTRA_TODO_ALARM_LABEL, activeAlarm?.label ?: "Study Alarm")
                            putExtra(MainActivity.EXTRA_TODO_STEP_COUNT, stepsTaken)
                            putExtra(MainActivity.EXTRA_TODO_IS_TEST, effectiveIsTest)
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onEmergencyDismiss = {
                        val dismissIntent = Intent(this@RingingActivity, AlarmRingingService::class.java).apply {
                            action = AlarmRingingService.ACTION_DISMISS_ALARM
                            putExtra(AlarmRingingService.EXTRA_IS_EMERGENCY, true)
                            putExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, effectiveAlarmId)
                        }
                        startService(dismissIntent)
                        finish()
                    },
                    onSensorFailureDismiss = {
                        val dismissIntent = Intent(this@RingingActivity, AlarmRingingService::class.java).apply {
                            action = AlarmRingingService.ACTION_DISMISS_ALARM
                            putExtra(AlarmRingingService.EXTRA_IS_SENSOR_FAILURE, true)
                            putExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, effectiveAlarmId)
                        }
                        startService(dismissIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun RingingScreen(
    alarmLabel: String,
    walkToDismiss: Boolean,
    targetSteps: Int,
    stepsTaken: Int,
    isStepSensorAvailable: Boolean,
    showEmergencyDismiss: Boolean,
    isTestRing: Boolean,
    onDismissGoalReached: () -> Unit,
    onEmergencyDismiss: () -> Unit,
    onSensorFailureDismiss: () -> Unit
) {
    val currentTime = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
    }

    val isGoalReached = !walkToDismiss || (stepsTaken >= targetSteps)
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showSensorFailureDialog by remember { mutableStateOf(false) }

    // Pulsing background glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // User-configured Emergency Dismiss Dialog
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            containerColor = CardCharcoal,
            title = {
                Text(
                    text = "Emergency Dismiss",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (isTestRing) {
                        "Test Ring: You can silence this test immediately."
                    } else {
                        "Are you sure you want to bypass the remaining steps and silence the alarm?"
                    },
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        onEmergencyDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) {
                    Text("Silence Now", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEmergencyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CardElevated)
                ) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }

    // Technical Sensor Failure Recovery Dialog
    if (showSensorFailureDialog) {
        AlertDialog(
            onDismissRequest = { showSensorFailureDialog = false },
            containerColor = CardCharcoal,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Warning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hardware Sensor Unavailable",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "A hardware step sensor or Activity Recognition permission is missing on this device. You may use this technical recovery route to silence the alarm.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSensorFailureDialog = false
                        onSensorFailureDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Warning)
                ) {
                    Text("Technical Recovery Silence", color = TextInverse, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSensorFailureDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CardElevated)
                ) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(NeonLimeSubtle)
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header: Bell Icon, Time, Label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(NeonLimeSubtle)
                        .border(2.dp, NeonLime, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = NeonLime,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = currentTime,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-1.5).sp
                )

                Text(
                    text = if (isTestRing) "[TEST RING] $alarmLabel" else alarmLabel,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonLime,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Center Step Challenge Ring
            if (walkToDismiss && targetSteps > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCharcoal)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val progressFraction = (stepsTaken.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f)

                        CircularProgressRing(
                            progressFraction = progressFraction,
                            diameter = 170.dp,
                            strokeWidth = 14.dp
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$stepsTaken",
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "of $targetSteps Steps",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val noticeText = if (isGoalReached) {
                            "🎉 Step goal reached! Tap below to silence alarm and view your morning tasks."
                        } else {
                            "Walk with your phone to turn off the alarm. The dismiss button unlocks when your step goal is reached."
                        }

                        Text(
                            text = noticeText,
                            fontSize = 13.sp,
                            color = if (isGoalReached) NeonLime else TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Emergency Dismiss Link (only if enabled OR if test ring!)
                        if (isTestRing || showEmergencyDismiss) {
                            Text(
                                text = if (isTestRing) "Silence Test Ring" else "Need emergency dismiss?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isTestRing) NeonLime else TextMuted,
                                modifier = Modifier.clickable { showEmergencyDialog = true }
                            )
                        } else {
                            Text(
                                text = "Strict Mode: Emergency Dismiss Disabled",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        // Technical failure recovery fallback (distinct from user switch)
                        if (!isStepSensorAvailable) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ Step sensor unavailable — Tap for technical recovery",
                                fontSize = 11.sp,
                                color = Warning,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showSensorFailureDialog = true }
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Standard Alarm Ringing",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            // Bottom Dismiss Button
            Button(
                onClick = onDismissGoalReached,
                enabled = isGoalReached,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGoalReached) NeonLime else CardElevated,
                    disabledContainerColor = CardElevated
                )
            ) {
                val buttonLabel = if (isGoalReached) {
                    if (walkToDismiss) "Dismiss & View Today's Tasks" else "Dismiss Alarm"
                } else {
                    "Walk ${targetSteps - stepsTaken} more steps to dismiss"
                }

                Text(
                    text = buttonLabel,
                    color = if (isGoalReached) TextInverse else TextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
