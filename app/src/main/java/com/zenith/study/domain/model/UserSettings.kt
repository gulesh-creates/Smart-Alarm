package com.zenith.study.domain.model

data class UserSettings(
    val userName: String = "",
    val dailyGoalMinutes: Int = 180,
    val isSoundEnabled: Boolean = true,
    val is24HourFormat: Boolean = false,
    val isVibrationEnabled: Boolean = true,
    val timerMode: TimerMode = TimerMode.NORMAL,
    val selectedDurationMinutes: Int = 25,
    val volumeButtonsEnabled: Boolean = false,
    val autoFlipEnabled: Boolean = false
)
