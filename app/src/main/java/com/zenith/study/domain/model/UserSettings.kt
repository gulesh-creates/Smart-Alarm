package com.zenith.study.domain.model

data class UserSettings(
    val dailyGoalMinutes: Int = 180,
    val isSoundEnabled: Boolean = true,
    val is24HourFormat: Boolean = false,
    val isVibrationEnabled: Boolean = true,
    val autoFlipEnabled: Boolean = true
)

