package com.zenith.study.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat

/**
 * Hardware step detector listener wrapping Sensor.TYPE_STEP_DETECTOR.
 * Checks ACTIVITY_RECOGNITION permission and prevents duplicate rapid fires.
 */
class StepDetector(
    private val context: Context,
    private val onStepDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    val isAvailable: Boolean
        get() = stepSensor != null

    val hasPermission: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

    private var lastStepMonotonicMillis = 0L

    fun startListening(): Boolean {
        if (!isAvailable || !hasPermission) return false
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
            return true
        }
        return false
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_DETECTOR) return
        val now = SystemClock.elapsedRealtime()

        // Apply a 300ms cooldown between discrete step events
        if (now - lastStepMonotonicMillis > 300L) {
            lastStepMonotonicMillis = now
            onStepDetected()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

