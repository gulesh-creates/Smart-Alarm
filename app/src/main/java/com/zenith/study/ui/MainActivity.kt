package com.zenith.study.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.zenith.study.ZenithApplication
import com.zenith.study.domain.model.Alarm
import com.zenith.study.ui.components.AlarmEditSheet
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

                var currentTab by remember { mutableStateOf(ZenithTab.HOME) }
                var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
                var isEditSheetOpen by remember { mutableStateOf(false) }

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
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
                                nextAlarm = nextAlarm,
                                statisticsReport = statisticsReport,
                                dailyGoalMinutes = userSettings.dailyGoalMinutes,
                                is24Hour = userSettings.is24HourFormat,
                                onStartSessionClick = {
                                    viewModel.setTimerDuration(25)
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
                                onSetDuration = { mins -> viewModel.setTimerDuration(mins) },
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
                                onUpdateDailyGoal = { viewModel.updateDailyGoal(it) },
                                onToggleSound = { viewModel.toggleSound(it) },
                                onToggle24Hour = { viewModel.toggle24Hour(it) },
                                onToggleVibration = { viewModel.toggleVibration(it) },
                                onToggleAutoFlip = { viewModel.toggleAutoFlip(it) },
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
                    }
                }
            }
        }
    }
}

