package com.zenith.study.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zenith.study.ZenithApplication
import com.zenith.study.domain.engine.TimerStateMachine
import com.zenith.study.domain.engine.TimerStatus
import com.zenith.study.domain.model.FocusSession
import com.zenith.study.domain.model.TimerMode
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
    private var startWallClockMillis = 0L
    private var currentMode: TimerMode = TimerMode.NORMAL
    private var isReceiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (currentMode != TimerMode.SMART) return
            val action = intent?.action ?: return
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isInteractive = powerManager?.isInteractive ?: true
            val now = SystemClock.elapsedRealtime()

            when (action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Display turned off: Start or resume Smart Timer automatically
                    if (!isInteractive && (timerEngine.state is TimerStatus.Ready || timerEngine.state is TimerStatus.Paused)) {
                        Log.d(TAG, "Screen OFF detected: starting/resuming Smart Timer")
                        timerEngine.startOrResume(now)
                        _timerStateFlow.value = timerEngine.state
                        startTicker()
                        updateNotification("Studying (Screen Off) • Smart Timer Active", showPauseAction = true)
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Display turned on: Auto-pause Smart Timer
                    if (timerEngine.state is TimerStatus.Running) {
                        Log.d(TAG, "Screen ON detected: auto-pausing Smart Timer")
                        timerEngine.pause(now)
                        _timerStateFlow.value = timerEngine.state
                        stopTicker()
                        updateNotification("Paused (Screen On) — Turn screen off to resume", showPauseAction = false)
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        val modeStr = intent?.getStringExtra(EXTRA_TIMER_MODE)
        if (modeStr != null) {
            try {
                currentMode = TimerMode.valueOf(modeStr)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown timer mode $modeStr, keeping $currentMode")
            }
        }

        when (action) {
            ACTION_START -> {
                val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 25) ?: 25
                val durationMillis = durationMinutes * 60 * 1000L
                timerEngine.setDuration(durationMillis)
                _timerStateFlow.value = timerEngine.state
                startWallClockMillis = System.currentTimeMillis()

                if (currentMode == TimerMode.SMART) {
                    registerScreenReceiver()
                    startForeground(
                        NOTIFICATION_ID,
                        buildTimerNotification("Smart Timer: Turn display off to start countdown", showPauseAction = false)
                    )
                } else {
                    unregisterScreenReceiver()
                    startForeground(
                        NOTIFICATION_ID,
                        buildTimerNotification("Ready • Tap Start to begin session", showPauseAction = false)
                    )
                }
            }
            ACTION_SET_MODE -> {
                if (currentMode == TimerMode.SMART) {
                    registerScreenReceiver()
                    val msg = if (timerEngine.state is TimerStatus.Running) {
                        "Studying • Smart Timer Active"
                    } else {
                        "Smart Timer: Turn display off to start"
                    }
                    updateNotification(msg, showPauseAction = timerEngine.state is TimerStatus.Running)
                } else {
                    unregisterScreenReceiver()
                    val msg = if (timerEngine.state is TimerStatus.Running) {
                        "Studying • Normal Timer"
                    } else {
                        "Normal Timer Ready"
                    }
                    updateNotification(msg, showPauseAction = timerEngine.state is TimerStatus.Running)
                }
            }
            ACTION_MANUAL_START_RESUME -> {
                val now = SystemClock.elapsedRealtime()
                if (timerEngine.state is TimerStatus.Ready || timerEngine.state is TimerStatus.Paused) {
                    timerEngine.startOrResume(now)
                    _timerStateFlow.value = timerEngine.state
                    startTicker()
                    if (currentMode == TimerMode.SMART) {
                        registerScreenReceiver()
                    }
                    updateNotification("Studying...", showPauseAction = true)
                }
            }
            ACTION_MANUAL_PAUSE -> {
                val now = SystemClock.elapsedRealtime()
                if (timerEngine.state is TimerStatus.Running) {
                    timerEngine.pause(now)
                    _timerStateFlow.value = timerEngine.state
                    stopTicker()
                    updateNotification("Paused", showPauseAction = false)
                }
            }
            ACTION_RESET -> {
                stopTicker()
                unregisterScreenReceiver()
                timerEngine.reset()
                _timerStateFlow.value = timerEngine.state
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_FINISH -> {
                endSessionEarly()
            }
        }

        return START_NOT_STICKY
    }

    private fun registerScreenReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(screenReceiver, filter)
            isReceiverRegistered = true
            Log.d(TAG, "Screen receiver registered for Smart Timer")
        }
    }

    private fun unregisterScreenReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(screenReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering screen receiver", e)
            }
            isReceiverRegistered = false
            Log.d(TAG, "Screen receiver unregistered")
        }
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
                    completeSessionNaturally()
                    break
                } else {
                    val modeTag = if (currentMode == TimerMode.SMART) " (Smart)" else ""
                    updateNotification("Studying: $timeStr remaining$modeTag", showPauseAction = true)
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun completeSessionNaturally() {
        stopTicker()
        unregisterScreenReceiver()

        val plannedMins = (timerEngine.state.totalDurationMillis / 60000L).toInt()
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

        updateNotification("Focus block completed! Great job.", showPauseAction = false)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun endSessionEarly() {
        stopTicker()
        unregisterScreenReceiver()

        val totalPlannedMins = (timerEngine.state.totalDurationMillis / 60000L).toInt()
        val now = SystemClock.elapsedRealtime()
        val remainingMillis = timerEngine.getRemainingMillis(now)
        val elapsedMillis = (timerEngine.state.totalDurationMillis - remainingMillis).coerceAtLeast(0L)
        val actualMinutes = (elapsedMillis / 60000L).toInt()

        val app = applicationContext as ZenithApplication
        CoroutineScope(Dispatchers.IO).launch {
            if (actualMinutes > 0) {
                app.sessionRepository.insertSession(
                    FocusSession(
                        subject = "Deep Work Sprint",
                        plannedMinutes = totalPlannedMins,
                        actualMinutes = actualMinutes,
                        startedAtEpochMillis = startWallClockMillis,
                        completedAtEpochMillis = System.currentTimeMillis(),
                        isCompleted = false // Explicitly marked as NOT completed per partial session policy
                    )
                )
            }
        }

        timerEngine.reset()
        _timerStateFlow.value = timerEngine.state
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildTimerNotification(statusText: String, showPauseAction: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_RESET
        }
        val resetPendingIntent = PendingIntent.getService(
            this,
            1,
            resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionIntent = Intent(this, FocusTimerService::class.java).apply {
            action = if (showPauseAction) ACTION_MANUAL_PAUSE else ACTION_MANUAL_START_RESUME
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            2,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionTitle = if (showPauseAction) "Pause" else "Start"
        val toggleActionIcon = if (showPauseAction) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, ZenithApplication.TIMER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Zenith Study Timer")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .addAction(toggleActionIcon, toggleActionTitle, togglePendingIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Reset", resetPendingIntent)
            .setOngoing(timerEngine.state is TimerStatus.Running)
            .build()
    }

    private fun updateNotification(statusText: String, showPauseAction: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildTimerNotification(statusText, showPauseAction))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTicker()
        unregisterScreenReceiver()
    }

    companion object {
        private const val TAG = "FocusTimerService"
        const val NOTIFICATION_ID = 3001
        const val ACTION_START = "com.zenith.study.ACTION_TIMER_START"
        const val ACTION_SET_MODE = "com.zenith.study.ACTION_TIMER_SET_MODE"
        const val ACTION_MANUAL_START_RESUME = "com.zenith.study.ACTION_MANUAL_START_RESUME"
        const val ACTION_MANUAL_PAUSE = "com.zenith.study.ACTION_MANUAL_PAUSE"
        const val ACTION_RESET = "com.zenith.study.ACTION_TIMER_RESET"
        const val ACTION_FINISH = "com.zenith.study.ACTION_TIMER_FINISH"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_TIMER_MODE = "extra_timer_mode"

        val timerEngine = TimerStateMachine(25 * 60 * 1000L)
        private val _timerStateFlow = MutableStateFlow<TimerStatus>(timerEngine.state)
        val timerStateFlow = _timerStateFlow.asStateFlow()
    }
}
