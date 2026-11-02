package com.zenith.study.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.study.ZenithApplication
import com.zenith.study.alarm.AndroidAlarmScheduler
import com.zenith.study.domain.engine.NextOccurrenceCalculator
import com.zenith.study.domain.engine.StatisticsEngine
import com.zenith.study.domain.engine.StatisticsReport
import com.zenith.study.domain.engine.TimerStatus
import com.zenith.study.domain.model.Alarm
import com.zenith.study.domain.model.FocusSession
import com.zenith.study.domain.model.TimerMode
import com.zenith.study.domain.model.TodoItem
import com.zenith.study.domain.model.UserSettings
import com.zenith.study.service.AlarmRingingService
import com.zenith.study.service.FocusTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ZenithApplication
    private val alarmRepo = app.alarmRepository
    private val sessionRepo = app.sessionRepository
    private val todoRepo = app.todoRepository
    private val prefsRepo = app.userPreferencesRepository
    private val scheduler = app.alarmScheduler

    val alarmsState: StateFlow<List<Alarm>> = alarmRepo.getAllAlarmsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessionsState: StateFlow<List<FocusSession>> = sessionRepo.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettingsState: StateFlow<UserSettings> = prefsRepo.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val timerStatusState: StateFlow<TimerStatus> = FocusTimerService.timerStateFlow

    val todayTodosState: StateFlow<List<TodoItem>> = todoRepo.getTodosForDate(LocalDate.now().toEpochDay())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chronological Next Alarm calculation
    val nextAlarmState: StateFlow<Alarm?> = combine(alarmsState, userSettingsState) { alarms, _ ->
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        alarms.filter { it.isEnabled }
            .mapNotNull { alarm ->
                NextOccurrenceCalculator.calculateNextOccurrence(alarm, now)?.let { occurrence ->
                    alarm to occurrence
                }
            }
            .minByOrNull { it.second }
            ?.first
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dynamic Statistics Report
    val statisticsReportState: StateFlow<StatisticsReport> = combine(
        sessionsState,
        userSettingsState
    ) { sessions, settings ->
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        StatisticsEngine.computeReport(sessions, now, settings.dailyGoalMinutes)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StatisticsEngine.computeReport(emptyList(), ZonedDateTime.now(ZoneId.systemDefault()), 180)
    )

    fun saveAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val savedId = if (alarm.id == 0L) {
                alarmRepo.insertAlarm(alarm)
            } else {
                alarmRepo.updateAlarm(alarm)
                alarm.id
            }
            val updated = alarm.copy(id = savedId)
            scheduler.schedule(updated)
        }
    }

    fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = isEnabled)
            alarmRepo.updateAlarm(updated)
            if (isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated.id)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm.id)
            alarmRepo.deleteAlarm(alarm.id)
        }
    }

    fun testAlarm(alarm: Alarm) {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmRingingService.EXTRA_IS_TEST_RING, true)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    // Timer control functions
    fun setTimerDuration(minutes: Int) {
        viewModelScope.launch {
            prefsRepo.updateSelectedDurationMinutes(minutes)
        }
        val context = getApplication<Application>()
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
            putExtra(FocusTimerService.EXTRA_DURATION_MINUTES, minutes)
            putExtra(FocusTimerService.EXTRA_TIMER_MODE, userSettingsState.value.timerMode.name)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun setTimerMode(mode: TimerMode) {
        viewModelScope.launch {
            prefsRepo.updateTimerMode(mode)
        }
        val context = getApplication<Application>()
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_SET_MODE
            putExtra(FocusTimerService.EXTRA_TIMER_MODE, mode.name)
        }
        context.startService(intent)
    }

    fun startOrResumeTimer() {
        val context = getApplication<Application>()
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_MANUAL_START_RESUME
            putExtra(FocusTimerService.EXTRA_TIMER_MODE, userSettingsState.value.timerMode.name)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun pauseTimer() {
        val context = getApplication<Application>()
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_MANUAL_PAUSE
        }
        context.startService(intent)
    }

    fun toggleManualTimer() {
        if (timerStatusState.value is TimerStatus.Running) {
            pauseTimer()
        } else {
            startOrResumeTimer()
        }
    }

    fun resetTimer() {
        val context = getApplication<Application>()
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_RESET
        }
        context.startService(intent)
    }

    fun endSessionEarly() {
        val context = getApplication<Application>()
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_FINISH
        }
        context.startService(intent)
    }

    // To-Do tasks
    fun addTodo(taskText: String, alarmId: Long = -1L) {
        viewModelScope.launch {
            todoRepo.insertTodo(
                TodoItem(
                    alarmId = alarmId,
                    occurrenceDateEpochDay = LocalDate.now().toEpochDay(),
                    taskText = taskText,
                    isCompleted = false
                )
            )
        }
    }

    fun toggleTodo(todo: TodoItem) {
        viewModelScope.launch {
            todoRepo.updateTodo(todo.copy(isCompleted = !todo.isCompleted))
        }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch {
            todoRepo.deleteTodo(id)
        }
    }

    // Settings
    fun updateUserName(name: String) = viewModelScope.launch { prefsRepo.updateUserName(name) }
    fun updateDailyGoal(minutes: Int) = viewModelScope.launch { prefsRepo.updateDailyGoalMinutes(minutes) }
    fun toggleSound(enabled: Boolean) = viewModelScope.launch { prefsRepo.updateSoundEnabled(enabled) }
    fun toggle24Hour(is24Hour: Boolean) = viewModelScope.launch { prefsRepo.update24HourFormat(is24Hour) }
    fun toggleVibration(enabled: Boolean) = viewModelScope.launch { prefsRepo.updateVibrationEnabled(enabled) }
    fun toggleVolumeButtons(enabled: Boolean) = viewModelScope.launch { prefsRepo.updateVolumeButtonsEnabled(enabled) }

    fun clearAllHistory() {
        viewModelScope.launch {
            sessionRepo.clearAll()
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            alarmsState.value.forEach { scheduler.cancel(it.id) }
            alarmRepo.clearAll()
            sessionRepo.clearAll()
            todoRepo.clearAll()
            prefsRepo.clear()
        }
    }
}
