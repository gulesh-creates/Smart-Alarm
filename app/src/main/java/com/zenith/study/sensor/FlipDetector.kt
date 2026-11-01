package com.zenith.study.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper

/**
 * Sensor listener that detects when the phone is placed face-down on a desk
 * and when it is lifted/turned face-up.
 * Employs a 400ms dwell time / hysteresis to ignore transient movements.
 */
class FlipDetector(
    context: Context,
    private val onFaceDown: () -> Unit,
    private val onFaceUp: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isAvailable: Boolean
        get() = gravitySensor != null

    private var isFaceDownState = false
    private val handler = Handler(Looper.getMainLooper())
    private var pendingStateRunnable: Runnable? = null

    fun startListening() {
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        pendingStateRunnable?.let { handler.removeCallbacks(it) }
        pendingStateRunnable = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val z = event.values[2]

        // Face down on a table: screen points down, gravity z is strongly negative (~ -9.8)
        val candidateFaceDown = z < -7.0f

        if (candidateFaceDown != isFaceDownState) {
            // Apply 400ms stability delay before switching state
            if (pendingStateRunnable == null) {
                pendingStateRunnable = Runnable {
                    isFaceDownState = candidateFaceDown
                    if (isFaceDownState) {
                        onFaceDown()
                    } else {
                        onFaceUp()
                    }
                    pendingStateRunnable = null
                }
                handler.postDelayed(pendingStateRunnable!!, 400L)
            }
        } else {
            // State remains steady, clear any pending flip
            pendingStateRunnable?.let {
                handler.removeCallbacks(it)
                pendingStateRunnable = null
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

