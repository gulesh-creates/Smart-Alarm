package com.zenith.study.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.zenith.study.ZenithApplication
import com.zenith.study.alarm.AndroidAlarmScheduler
import com.zenith.study.service.AlarmRingingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, -1L)
        Log.d(TAG, "AlarmReceiver triggered with alarmId: $alarmId")
        if (alarmId == -1L) return

        val app = context.applicationContext as ZenithApplication

        // Start the AlarmRingingService as a foreground service
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // Update database: retire one-time alarm, or reschedule next occurrence if repeating
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = app.alarmRepository.getAlarmById(alarmId) ?: return@launch
            val updatedAlarm = alarm.copy(
                isEnabled = alarm.isRepeating, // Disable if one-time, keep enabled if repeating
                lastTriggeredTimeMillis = System.currentTimeMillis()
            )
            app.alarmRepository.updateAlarm(updatedAlarm)

            if (updatedAlarm.isRepeating) {
                app.alarmScheduler.schedule(updatedAlarm)
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}

