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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DirectionsRun
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.study.alarm.AndroidAlarmScheduler
import com.zenith.study.sensor.StepDetector
import com.zenith.study.service.AlarmRingingService
import com.zenith.study.ui.components.CircularProgressRing
import com.zenith.study.ui.theme.BorderSubtle
import com.zenith.study.ui.theme.CanvasBlack
import com.zenith.study.ui.theme.CardCharcoal
import com.zenith.study.ui.theme.CardElevated
import com.zenith.study.ui.theme.NeonLime
import com.zenith.study.ui.theme.NeonLimeSubtle
import com.zenith.study.ui.theme.TextInverse
import com.zenith.study.ui.theme.TextMuted
import com.zenith.study.ui.theme.TextPrimary
import com.zenith.study.ui.theme.TextSecondary
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
                val stepsTaken by AlarmRingingService.currentStepsFlow.collectAsState()

                DisposableEffect(Unit) {
                    stepDetector?.startListening()
                    onDispose {
                        stepDetector?.stopListening()
                    }
                }

                RingingScreen(
                    alarmLabel = activeAlarm?.label ?: "Morning Study Alarm",
                    walkToDismiss = activeAlarm?.walkToDismiss ?: false,
                    targetSteps = activeAlarm?.stepTarget ?: 0,
                    stepsTaken = stepsTaken,
                    isStepSensorAvailable = stepDetector?.isAvailable == true,
                    onDismiss = {
                        val dismissIntent = Intent(this, AlarmRingingService::class.java).apply {
                            action = AlarmRingingService.ACTION_DISMISS_ALARM
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
    onDismiss: () -> Unit
) {
    val currentTime = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
    }

    val isGoalReached = !walkToDismiss || (stepsTaken >= targetSteps)
    var showEmergencyDialog by remember { mutableStateOf(false) }

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
                    text = "If you are unable to walk or your device lacks a step detector, you may silence the alarm immediately.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                ) {
                    Text("Silence Now", color = TextInverse, fontWeight = FontWeight.Bold)
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
                    text = alarmLabel,
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
                            "🎉 Goal reached! You may now dismiss the alarm."
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

                        // Accessible Emergency Fallback Link
                        Text(
                            text = "Need emergency dismiss?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier.clickable { showEmergencyDialog = true }
                        )
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
                onClick = onDismiss,
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
                    "Dismiss Alarm"
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

