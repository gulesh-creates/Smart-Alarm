package com.zenith.study.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zenith.study.domain.model.TimerMode
import com.zenith.study.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "zenith_user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val IS_SOUND_ENABLED = booleanPreferencesKey("is_sound_enabled")
        val IS_24_HOUR_FORMAT = booleanPreferencesKey("is_24_hour_format")
        val IS_VIBRATION_ENABLED = booleanPreferencesKey("is_vibration_enabled")
        val TIMER_MODE = stringPreferencesKey("timer_mode")
        val SELECTED_DURATION_MINUTES = intPreferencesKey("selected_duration_minutes")
        val VOLUME_BUTTONS_ENABLED = booleanPreferencesKey("volume_buttons_enabled")
        val AUTO_FLIP_ENABLED = booleanPreferencesKey("auto_flip_enabled")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val rawMode = prefs[PreferencesKeys.TIMER_MODE]
        val timerMode = if (rawMode != null) {
            try {
                TimerMode.valueOf(rawMode)
            } catch (e: Exception) {
                TimerMode.NORMAL
            }
        } else {
            // Default to NORMAL per requirements
            TimerMode.NORMAL
        }

        UserSettings(
            userName = prefs[PreferencesKeys.USER_NAME] ?: "",
            dailyGoalMinutes = prefs[PreferencesKeys.DAILY_GOAL_MINUTES] ?: 180,
            isSoundEnabled = prefs[PreferencesKeys.IS_SOUND_ENABLED] ?: true,
            is24HourFormat = prefs[PreferencesKeys.IS_24_HOUR_FORMAT] ?: false,
            isVibrationEnabled = prefs[PreferencesKeys.IS_VIBRATION_ENABLED] ?: true,
            timerMode = timerMode,
            selectedDurationMinutes = prefs[PreferencesKeys.SELECTED_DURATION_MINUTES] ?: 25,
            volumeButtonsEnabled = prefs[PreferencesKeys.VOLUME_BUTTONS_ENABLED] ?: false,
            autoFlipEnabled = prefs[PreferencesKeys.AUTO_FLIP_ENABLED] ?: false
        )
    }

    suspend fun updateUserName(name: String) {
        val trimmed = name.trim().take(30)
        context.dataStore.edit { it[PreferencesKeys.USER_NAME] = trimmed }
    }

    suspend fun updateDailyGoalMinutes(minutes: Int) {
        context.dataStore.edit { it[PreferencesKeys.DAILY_GOAL_MINUTES] = minutes }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_SOUND_ENABLED] = enabled }
    }

    suspend fun update24HourFormat(is24Hour: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_24_HOUR_FORMAT] = is24Hour }
    }

    suspend fun updateVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_VIBRATION_ENABLED] = enabled }
    }

    suspend fun updateTimerMode(mode: TimerMode) {
        context.dataStore.edit { it[PreferencesKeys.TIMER_MODE] = mode.name }
    }

    suspend fun updateSelectedDurationMinutes(minutes: Int) {
        val coerced = minutes.coerceIn(1, 180)
        context.dataStore.edit { it[PreferencesKeys.SELECTED_DURATION_MINUTES] = coerced }
    }

    suspend fun updateVolumeButtonsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VOLUME_BUTTONS_ENABLED] = enabled }
    }

    suspend fun updateAutoFlipEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_FLIP_ENABLED] = enabled }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
