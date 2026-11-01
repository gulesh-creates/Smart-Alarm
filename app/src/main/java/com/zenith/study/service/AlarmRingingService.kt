package com.zenith.study.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zenith.study.ZenithApplication
import com.zenith.study.alarm.AndroidAlarmScheduler
import com.zenith.study.domain.model.Alarm
import com.zenith.study.ui.ringing.RingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmRingingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarm: Alarm? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Zenith:AlarmRingingWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minute safeguard limit
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_DISMISS_ALARM) {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId != -1L) {
            val app = applicationContext as ZenithApplication
            CoroutineScope(Dispatchers.IO).launch {
                val alarm = app.alarmRepository.getAlarmById(alarmId)
                currentAlarm = alarm
                _activeRingingAlarmFlow.value = alarm
                _currentStepsFlow.value = 0

                val notification = buildRingingNotification(alarmId, alarm?.label ?: "Study Alarm")
                startForeground(NOTIFICATION_ID, notification)

                startRinging(alarm)
            }
        }

        return START_STICKY
    }

    private fun startRinging(alarm: Alarm?) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // Request audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }

            // Standard alarm tone
            var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(applicationContext, alarmUri!!)
                isLooping = true
                prepare()
                start()
            }

            // Vibration pattern
            if (alarm?.isVibrationEnabled != false) {
                val pattern = longArrayOf(0, 500, 300, 500, 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm audio", e)
        }
    }

    private fun buildRingingNotification(alarmId: Long, label: String): Notification {
        // Full screen intent to show RingingActivity over lock screen
        val fullScreenIntent = Intent(this, RingingActivity::class.java).apply {
            putExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            alarmId.toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ZenithApplication.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm Ringing: $label")
            .setContentText("Tap to open or complete walk challenge")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Emergency Dismiss", dismissPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            vibrator?.cancel()

            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up alarm ringing service", e)
        }
        _activeRingingAlarmFlow.value = null
    }

    companion object {
        private const val TAG = "AlarmRingingService"
        const val NOTIFICATION_ID = 2001
        const val ACTION_DISMISS_ALARM = "com.zenith.study.ACTION_DISMISS_ALARM"

        private val _activeRingingAlarmFlow = MutableStateFlow<Alarm?>(null)
        val activeRingingAlarmFlow = _activeRingingAlarmFlow.asStateFlow()

        private val _currentStepsFlow = MutableStateFlow(0)
        val currentStepsFlow = _currentStepsFlow.asStateFlow()

        fun incrementStep() {
            _currentStepsFlow.value += 1
        }
    }
}

