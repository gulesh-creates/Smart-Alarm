package com.zenith.study.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.zenith.study.domain.engine.NextOccurrenceCalculator
import com.zenith.study.domain.model.Alarm
import com.zenith.study.receiver.AlarmReceiver
import com.zenith.study.ui.MainActivity
import java.time.ZoneId
import java.time.ZonedDateTime

class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun schedule(alarm: Alarm): Boolean {
        if (!alarm.isEnabled) {
            cancel(alarm.id)
            return false
        }

        if (!canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarm for ID ${alarm.id}: Permission denied or unavailable.")
            return false
        }

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val nextOccurrenceEpoch = NextOccurrenceCalculator.calculateNextEpochMillis(alarm, now)
        if (nextOccurrenceEpoch == null) {
            Log.w(TAG, "No next occurrence calculated for alarm ID ${alarm.id}")
            cancel(alarm.id)
            return false
        }

        // Cancel existing pending intent first to guarantee idempotency
        cancel(alarm.id)

        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show intent that opens MainActivity when user clicks the alarm clock icon in system UI
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(nextOccurrenceEpoch, showPendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Successfully scheduled alarm ID ${alarm.id} for epoch $nextOccurrenceEpoch")
            return true
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm ${alarm.id}: ${se.message}")
            return false
        }
    }

    override fun cancel(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm intent for ID $alarmId")
        }
    }

    companion object {
        private const val TAG = "ZenithAlarmScheduler"
        const val ACTION_ALARM_TRIGGER = "com.zenith.study.ACTION_ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}

