package com.zenith.study.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.zenith.study.ZenithApplication
import com.zenith.study.domain.engine.TimerStateMachine
import com.zenith.study.domain.engine.TimerStatus
import com.zenith.study.domain.model.FocusSession
import com.zenith.study.sensor.FlipDetector
import com.zenith.study.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusTimerService : Service() {

    private var tickerJob: Job? = null
    private var flipDetector: FlipDetector? = null
    private var startWallClockMillis = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        flipDetector = FlipDetector(
            context = this,
            onFaceDown = {
                val now = SystemClock.elapsedRealtime()
                if (timerEngine.state is TimerStatus.Ready || timerEngine.state is TimerStatus.Paused) {
                    timerEngine.startOrResume(now)
                    _timerStateFlow.value = timerEngine.state
                    startTicker()
                }
            },
            onFaceUp = {
                val now = SystemClock.elapsedRealtime()
                if (timerEngine.state is TimerStatus.Running) {
                    timerEngine.pause(now)
                    _timerStateFlow.value = timerEngine.state
                    stopTicker()
                    updateNotification("Paused — Flip face down to resume")
                }
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 25) ?: 25
                val durationMillis = durationMinutes * 60 * 1000L
                timerEngine.setDuration(durationMillis)
                _timerStateFlow.value = timerEngine.state
                startWallClockMillis = System.currentTimeMillis()

                startForeground(NOTIFICATION_ID, buildTimerNotification("Ready — Flip face down to start"))
                flipDetector?.startListening()
            }
            ACTION_MANUAL_START_RESUME -> {
                val now = SystemClock.elapsedRealtime()
                timerEngine.startOrResume(now)
                _timerStateFlow.value = timerEngine.state
                startTicker()
            }
            ACTION_MANUAL_PAUSE -> {
                val now = SystemClock.elapsedRealtime()
                timerEngine.pause(now)
                _timerStateFlow.value = timerEngine.state
                stopTicker()
                updateNotification("Paused")
            }
            ACTION_RESET -> {
                stopTicker()
                flipDetector?.stopListening()
                timerEngine.reset()
                _timerStateFlow.value = timerEngine.state
                stopSelf()
            }
            ACTION_FINISH -> {
                completeSession()
            }
        }

        return START_NOT_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(1000L)
                val now = SystemClock.elapsedRealtime()
                val isCompleted = timerEngine.tick(now)
                _timerStateFlow.value = timerEngine.state

                val remainingSecs = timerEngine.getRemainingMillis(now) / 1000
                val m = remainingSecs / 60
                val s = remainingSecs % 60
                val timeStr = String.format("%02d:%02d", m, s)

                if (isCompleted) {
                    completeSession()
                    break
                } else {
                    updateNotification("Studying: $timeStr remaining")
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun completeSession() {
        stopTicker()
        flipDetector?.stopListening()

        val plannedMins = (timerEngine.state.totalDurationMillis / 60000).toInt()
        val app = applicationContext as ZenithApplication

        CoroutineScope(Dispatchers.IO).launch {
            app.sessionRepository.insertSession(
                FocusSession(
                    subject = "Deep Work Sprint",
                    plannedMinutes = plannedMins,
                    actualMinutes = plannedMins,
                    startedAtEpochMillis = startWallClockMillis,
                    completedAtEpochMillis = System.currentTimeMillis(),
                    isCompleted = true
                )
            )
        }

        updateNotification("Focus block completed! Great job.")
        stopSelf()
    }

    private fun buildTimerNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ZenithApplication.TIMER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Zenith Study Timer")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildTimerNotification(statusText))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTicker()
        flipDetector?.stopListening()
    }

    companion object {
        const val NOTIFICATION_ID = 3001
        const val ACTION_START = "com.zenith.study.ACTION_TIMER_START"
        const val ACTION_MANUAL_START_RESUME = "com.zenith.study.ACTION_MANUAL_START_RESUME"
        const val ACTION_MANUAL_PAUSE = "com.zenith.study.ACTION_MANUAL_PAUSE"
        const val ACTION_RESET = "com.zenith.study.ACTION_TIMER_RESET"
        const val ACTION_FINISH = "com.zenith.study.ACTION_TIMER_FINISH"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

        val timerEngine = TimerStateMachine(25 * 60 * 1000L)
        private val _timerStateFlow = MutableStateFlow<TimerStatus>(timerEngine.state)
        val timerStateFlow = _timerStateFlow.asStateFlow()
    }
}

