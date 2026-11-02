package com.zenith.study

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.zenith.study.alarm.AlarmScheduler
import com.zenith.study.alarm.AndroidAlarmScheduler
import com.zenith.study.data.local.UserPreferencesRepository
import com.zenith.study.data.local.ZenithDatabase
import com.zenith.study.data.repository.AlarmRepositoryImpl
import com.zenith.study.data.repository.FocusSessionRepositoryImpl
import com.zenith.study.data.repository.TodoRepositoryImpl
import com.zenith.study.domain.repository.AlarmRepository
import com.zenith.study.domain.repository.FocusSessionRepository
import com.zenith.study.domain.repository.TodoRepository

class ZenithApplication : Application() {

    lateinit var database: ZenithDatabase
        private set

    lateinit var alarmRepository: AlarmRepository
        private set

    lateinit var sessionRepository: FocusSessionRepository
        private set

    lateinit var todoRepository: TodoRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var alarmScheduler: AlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()

        database = ZenithDatabase.getInstance(this)
        alarmRepository = AlarmRepositoryImpl(database.alarmDao())
        sessionRepository = FocusSessionRepositoryImpl(database.focusSessionDao())
        todoRepository = TodoRepositoryImpl(database.todoDao())
        userPreferencesRepository = UserPreferencesRepository(this)
        alarmScheduler = AndroidAlarmScheduler(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Alarm Channel: High importance, alarm sound, lock screen visibility
            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_alarm_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500, 1000)
                setSound(alarmSoundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            // Study Timer Channel: Default importance for persistent background countdown
            val timerChannel = NotificationChannel(
                TIMER_CHANNEL_ID,
                getString(R.string.channel_timer_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_timer_desc)
                enableVibration(false)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(timerChannel)
        }
    }

    companion object {
        const val ALARM_CHANNEL_ID = "zenith_alarm_channel"
        const val TIMER_CHANNEL_ID = "zenith_timer_channel"
    }
}

