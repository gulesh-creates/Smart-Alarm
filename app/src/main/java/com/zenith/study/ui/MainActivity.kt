package com.zenith.study.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.zenith.study.ZenithApplication
import com.zenith.study.domain.model.Alarm
import com.zenith.study.service.AlarmRingingService
import com.zenith.study.ui.components.AlarmEditSheet
import com.zenith.study.ui.components.PostDismissTodoSheet
import com.zenith.study.ui.components.ZenithBottomNav
import com.zenith.study.ui.components.ZenithTab
import com.zenith.study.ui.screens.AlarmsScreen
import com.zenith.study.ui.screens.HomeScreen
import com.zenith.study.ui.screens.ProgressScreen
import com.zenith.study.ui.screens.SettingsScreen
import com.zenith.study.ui.screens.TimerScreen
import com.zenith.study.ui.theme.CanvasBlack
import com.zenith.study.ui.theme.ZenithTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var currentActiveTab = ZenithTab.HOME
    private var isSheetOpenState = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = viewModel.userSettingsState.value
        val isAlarmActive = AlarmRingingService.activeRingingAlarmFlow.value != null

        if (settings.volumeButtonsEnabled && currentActiveTab == ZenithTab.TIMER && !isSheetOpenState && !isAlarmActive && event?.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    viewModel.startOrResumeTimer()
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    viewModel.pauseTimer()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenithTheme {
                val alarms by viewModel.alarmsState.collectAsState()
                val nextAlarm by viewModel.nextAlarmState.collectAsState()
                val sessions by viewModel.sessionsState.collectAsState()
                val userSettings by viewModel.userSettingsState.collectAsState()
                val timerStatus by viewModel.timerStatusState.collectAsState()
                val statisticsReport by viewModel.statisticsReportState.collectAsState()
                val todayTodos by viewModel.todayTodosState.collectAsState()

                var currentTab by remember { mutableStateOf(ZenithTab.HOME) }
                currentActiveTab = currentTab

                var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
                var isEditSheetOpen by remember { mutableStateOf(false) }

                var isTodoSheetOpen by remember {
                    mutableStateOf(intent.getBooleanExtra(EXTRA_SHOW_POST_DISMISS_TODO, false))
                }
                var todoAlarmLabel by remember {
                    mutableStateOf(intent.getStringExtra(EXTRA_TODO_ALARM_LABEL) ?: "Morning Alarm")
                }
                var todoStepCount by remember {
                    mutableIntStateOf(intent.getIntExtra(EXTRA_TODO_STEP_COUNT, 30))
                }
                var todoIsTest by remember {
                    mutableStateOf(intent.getBooleanExtra(EXTRA_TODO_IS_TEST, false))
                }

                isSheetOpenState = isEditSheetOpen || isTodoSheetOpen

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val todoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // Permission Launchers
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Notifications are recommended to see active alarms and focus timers.")
                        }
                    }
                }

                val activityRecognitionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Activity permission is required for hardware step detection.")
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    }

                    // Check exact alarms permission on Android 12+ (API 31+)
                    val app = application as ZenithApplication
                    if (!app.alarmScheduler.canScheduleExactAlarms() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val result = snackbarHostState.showSnackbar(
                            message = "Exact alarms permission is disabled. Alarms may not ring on time.",
                            actionLabel = "Enable",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val reqIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(reqIntent)
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        ZenithBottomNav(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    },
                    containerColor = CanvasBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(CanvasBlack)
                    ) {
                        when (currentTab) {
                            ZenithTab.HOME -> HomeScreen(
                                userName = userSettings.userName,
                                nextAlarm = nextAlarm,
                                statisticsReport = statisticsReport,
                                dailyGoalMinutes = userSettings.dailyGoalMinutes,
                                selectedDurationMinutes = userSettings.selectedDurationMinutes,
                                timerMode = userSettings.timerMode,
                                is24Hour = userSettings.is24HourFormat,
                                todayTodos = todayTodos,
                                onToggleTodo = { viewModel.toggleTodo(it) },
                                onAddTodo = { viewModel.addTodo(it) },
                                onDeleteTodo = { viewModel.deleteTodo(it) },
                                onStartSessionClick = {
                                    viewModel.setTimerDuration(userSettings.selectedDurationMinutes)
                                    currentTab = ZenithTab.TIMER
                                },
                                onNextAlarmToggle = { alarm, enabled ->
                                    viewModel.toggleAlarm(alarm, enabled)
                                },
                                onTestAlarmClick = { alarm ->
                                    viewModel.testAlarm(alarm)
                                },
                                onViewAllAlarmsClick = { currentTab = ZenithTab.ALARMS },
                                onNewAlarmClick = {
                                    editingAlarm = null
                                    isEditSheetOpen = true
                                }
                            )
                            ZenithTab.TIMER -> TimerScreen(
                                timerStatus = timerStatus,
                                timerMode = userSettings.timerMode,
                                selectedMinutes = userSettings.selectedDurationMinutes,
                                onSetDuration = { mins -> viewModel.setTimerDuration(mins) },
                                onSetMode = { mode -> viewModel.setTimerMode(mode) },
                                onManualToggle = { viewModel.toggleManualTimer() },
                                onReset = { viewModel.resetTimer() },
                                onEndSession = { viewModel.endSessionEarly() }
                            )
                            ZenithTab.ALARMS -> AlarmsScreen(
                                alarms = alarms,
                                is24Hour = userSettings.is24HourFormat,
                                onAddAlarmClick = {
                                    editingAlarm = null
                                    isEditSheetOpen = true
                                },
                                onToggleAlarm = { alarm, enabled -> viewModel.toggleAlarm(alarm, enabled) },
                                onEditAlarm = { alarm ->
                                    editingAlarm = alarm
                                    isEditSheetOpen = true
                                },
                                onDeleteAlarm = { alarm -> viewModel.deleteAlarm(alarm) },
                                onTestAlarm = { alarm -> viewModel.testAlarm(alarm) }
                            )
                            ZenithTab.PROGRESS -> ProgressScreen(
                                statisticsReport = statisticsReport,
                                recentSessions = sessions,
                                dailyGoalMinutes = userSettings.dailyGoalMinutes,
                                onClearHistory = { viewModel.clearAllHistory() }
                            )
                            ZenithTab.SETTINGS -> SettingsScreen(
                                userSettings = userSettings,
                                onUpdateUserName = { viewModel.updateUserName(it) },
                                onUpdateDailyGoal = { viewModel.updateDailyGoal(it) },
                                onToggleSound = { viewModel.toggleSound(it) },
                                onToggle24Hour = { viewModel.toggle24Hour(it) },
                                onToggleVibration = { viewModel.toggleVibration(it) },
                                onToggleVolumeButtons = { viewModel.toggleVolumeButtons(it) },
                                onResetAllData = { viewModel.resetAllData() }
                            )
                        }

                        // Alarm Editor Bottom Sheet
                        if (isEditSheetOpen) {
                            AlarmEditSheet(
                                sheetState = sheetState,
                                initialAlarm = editingAlarm,
                                is24Hour = userSettings.is24HourFormat,
                                onDismiss = { isEditSheetOpen = false },
                                onSave = { alarm ->
                                    viewModel.saveAlarm(alarm)
                                    isEditSheetOpen = false
                                }
                            )
                        }

                        // Post-Dismiss To-Do Sheet
                        if (isTodoSheetOpen) {
                            PostDismissTodoSheet(
                                sheetState = todoSheetState,
                                todos = todayTodos,
                                alarmLabel = todoAlarmLabel,
                                stepCount = todoStepCount,
                                isTestRing = todoIsTest,
                                onToggleTodo = { viewModel.toggleTodo(it) },
                                onAddTodo = { viewModel.addTodo(it) },
                                onDeleteTodo = { viewModel.deleteTodo(it) },
                                onDismiss = { isTodoSheetOpen = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        const val EXTRA_SHOW_POST_DISMISS_TODO = "extra_show_post_dismiss_todo"
        const val EXTRA_TODO_ALARM_ID = "extra_todo_alarm_id"
        const val EXTRA_TODO_ALARM_LABEL = "extra_todo_alarm_label"
        const val EXTRA_TODO_STEP_COUNT = "extra_todo_step_count"
        const val EXTRA_TODO_IS_TEST = "extra_todo_is_test"
    }
}
