package com.zenith.study.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.zenith.study.ZenithApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recomputes and reschedules alarms when device clock, date, or timezone changes.
 */
class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "TimeChangeReceiver received action: $action")

        val app = context.applicationContext as ZenithApplication
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabledAlarms = app.alarmRepository.getEnabledAlarms()
                Log.d(TAG, "Rescheduling ${enabledAlarms.size} alarms following time/timezone change")
                enabledAlarms.forEach { alarm ->
                    app.alarmScheduler.schedule(alarm)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms on clock change", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "TimeChangeReceiver"
    }
}

