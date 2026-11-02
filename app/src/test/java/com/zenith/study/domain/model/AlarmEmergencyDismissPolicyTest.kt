package com.zenith.study.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmEmergencyDismissPolicyTest {

    /**
     * Pure evaluator mimicking AlarmRingingService.handleDismissRequest logic.
     */
    private fun evaluateDismissal(
        alarm: Alarm?,
        isTestRing: Boolean,
        isEmergencyIntent: Boolean,
        isSensorFailureIntent: Boolean,
        currentSteps: Int
    ): Boolean {
        if (isTestRing) return true
        if (alarm == null || !alarm.walkToDismiss) return true
        if (isSensorFailureIntent) return true

        if (isEmergencyIntent) {
            return alarm.showEmergencyDismiss
        }

        return currentSteps >= alarm.stepTarget
    }

    @Test
    fun testRing_alwaysAllowsDismissal_evenWhenStrictMode() {
        val strictAlarm = Alarm(
            hour = 6,
            minute = 30,
            walkToDismiss = true,
            stepTarget = 100,
            showEmergencyDismiss = false
        )

        // Test ring with 0 steps and emergency = false
        val canDismissNormal = evaluateDismissal(
            alarm = strictAlarm,
            isTestRing = true,
            isEmergencyIntent = false,
            isSensorFailureIntent = false,
            currentSteps = 0
        )
        assertTrue(canDismissNormal)

        // Test ring with emergency intent
        val canDismissEmergency = evaluateDismissal(
            alarm = strictAlarm,
            isTestRing = true,
            isEmergencyIntent = true,
            isSensorFailureIntent = false,
            currentSteps = 0
        )
        assertTrue(canDismissEmergency)
    }

    @Test
    fun standardAlarmWithoutWalkChallenge_alwaysAllowsDismissal() {
        val normalAlarm = Alarm(hour = 7, minute = 0, walkToDismiss = false)
        val canDismiss = evaluateDismissal(
            alarm = normalAlarm,
            isTestRing = false,
            isEmergencyIntent = false,
            isSensorFailureIntent = false,
            currentSteps = 0
        )
        assertTrue(canDismiss)
    }

    @Test
    fun strictModeWalkAlarm_blocksEmergencyIntent() {
        val strictAlarm = Alarm(
            hour = 7,
            minute = 0,
            walkToDismiss = true,
            stepTarget = 50,
            showEmergencyDismiss = false
        )

        // Attempt emergency dismiss
        val dismissed = evaluateDismissal(
            alarm = strictAlarm,
            isTestRing = false,
            isEmergencyIntent = true,
            isSensorFailureIntent = false,
            currentSteps = 10
        )
        assertFalse("Strict alarm must reject emergency dismiss intent", dismissed)
    }

    @Test
    fun allowedEmergencyAlarm_acceptsEmergencyIntent() {
        val lenientAlarm = Alarm(
            hour = 7,
            minute = 0,
            walkToDismiss = true,
            stepTarget = 50,
            showEmergencyDismiss = true
        )

        val dismissed = evaluateDismissal(
            alarm = lenientAlarm,
            isTestRing = false,
            isEmergencyIntent = true,
            isSensorFailureIntent = false,
            currentSteps = 0
        )
        assertTrue("Lenient alarm must allow emergency dismiss intent", dismissed)
    }

    @Test
    fun normalDismiss_checksStepTargetAccurately() {
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            walkToDismiss = true,
            stepTarget = 30,
            showEmergencyDismiss = false
        )

        // Below target (29 steps)
        assertFalse(evaluateDismissal(alarm, isTestRing = false, isEmergencyIntent = false, isSensorFailureIntent = false, currentSteps = 29))

        // Target reached (30 steps)
        assertTrue(evaluateDismissal(alarm, isTestRing = false, isEmergencyIntent = false, isSensorFailureIntent = false, currentSteps = 30))

        // Target exceeded (35 steps)
        assertTrue(evaluateDismissal(alarm, isTestRing = false, isEmergencyIntent = false, isSensorFailureIntent = false, currentSteps = 35))
    }

    @Test
    fun sensorFailureRecovery_allowsDismissal_evenInStrictMode() {
        val strictAlarm = Alarm(
            hour = 7,
            minute = 0,
            walkToDismiss = true,
            stepTarget = 50,
            showEmergencyDismiss = false
        )

        val dismissed = evaluateDismissal(
            alarm = strictAlarm,
            isTestRing = false,
            isEmergencyIntent = false,
            isSensorFailureIntent = true,
            currentSteps = 0
        )
        assertTrue("Technical sensor failure must allow recovery dismissal", dismissed)
    }
}

