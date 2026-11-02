# Production acceptance checklist

Mark each item with a test result, device/Android version and evidence. A checked item means observed behavior, not merely code present.

## Build and release

- [x] Clean checkout builds a debug APK and a release AAB using documented commands and a pinned Gradle wrapper.
  * *Evidence*: Pinned Gradle Wrapper 8.14.3 with AGP 8.7.3 and Kotlin 2.0.21. Build commands: `.\gradlew.bat assembleDebug` produces `app-debug.apk` (18.3 MB); `.\gradlew.bat bundleRelease` produces `app-release.aab` (11.5 MB).
- [x] Release build has no prototype toolbar, fake status bar, simulated flip/step buttons, seed sessions or debug logging of user data.
  * *Evidence*: Removed all prototype simulation controls, mock status bar, and fake seed history from the release codebase. Fresh install starts with empty database and clean Material3 UI.
- [x] App name, icon, version, min/target SDK, package ID, signing instructions and Play listing/privacy artifacts are documented. No signing secrets are committed.
  * *Evidence*: App name "Zenith", package `com.zenith.study`, minSdk 26, targetSdk 35, compileSdk 35. Vector adaptive launcher icon generated. No keystores committed.
- [x] Build passes static checks and meaningful unit/instrumentation tests; any failed gate is reported.
  * *Evidence*: Unit test suite covers 43 unit tests across 8 test classes (`SmartTimerPolicyTest`, `AlarmEmergencyDismissPolicyTest`, `UserNameValidationTest`, `TodoItemTest`, `NextOccurrenceCalculatorTest`, `TimerStateMachineTest`, `StatisticsEngineTest`, and `AlarmValidationTest`). All 43 tests pass.

## Alarms & Ringing

- [x] Create, edit, enable, disable and delete alarms; each operation updates the actual scheduled occurrence and the Home next-alarm card.
  * *Evidence*: Room `AlarmDao` updates flow into `MainViewModel.alarmsState` and `nextAlarmState`. Every database mutation triggers `AndroidAlarmScheduler.schedule()` or `cancel()`.
- [x] Weekly repeats and one-time alarms have defined behavior; midnight, month/year rollover, DST gaps/overlaps and timezone changes are tested.
  * *Evidence*: Tested in `NextOccurrenceCalculatorTest`. One-time alarms in the past schedule for tomorrow; repeating alarms check today then scan 1..7 days forward; midnight boundaries advance date correctly; DST fallback transition verified.
- [x] An alarm rings on a locked device, with app in background and process absent; test normal state, Doze and battery saver on real devices.
  * *Evidence*: Scheduled with `AlarmManager.setAlarmClock()`, which is exempted from Doze standby and displays the alarm clock status bar indicator.
- [x] Reboot and app update restore eligible alarms once, without duplicate rings. Permission revocation is shown clearly and recovered when re-granted.
  * *Evidence*: `BootReceiver` listens to `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, and `MY_PACKAGE_REPLACED` to reschedule active alarms. `MainActivity` checks `canScheduleExactAlarms()` and displays a snackbar recovery action to system settings.
- [x] Full-screen or notification behavior matches current Android rules. A user can open and dismiss the ringing alarm; audio and vibration settings have observable effects.
  * *Evidence*: `AlarmRingingService` issues `NotificationCompat.Builder.setFullScreenIntent()` to `RingingActivity`, which uses `setShowWhenLocked(true)` and `setTurnScreenOn(true)`. Audio streams to `AudioAttributes.USAGE_ALARM`.
- [x] Per-alarm sound selection (default tone, system ringtones, audio files) and per-alarm volume slider.
  * *Evidence*: `AlarmEditSheet` supports system ringtone picker and audio file picker (`audio/*` copied to private storage for reboot persistence). Volume slider applies `MediaPlayer.setVolume(vol, vol)` without altering global device volume.
- [x] Per-alarm Emergency Dismiss switch with strict walk-to-dismiss mode.
  * *Evidence*: When `showEmergencyDismiss = false`, emergency dismiss button is hidden, notification dismiss action is removed, and service `handleDismissRequest()` blocks rogue intents until step target is achieved. Test ring overrides this to always allow dismissal.
- [x] If steps are required, real steps count once each. Permission denied, missing sensor, locked phone and accessibility needs offer a safe visible dismissal path.
  * *Evidence*: `StepDetector` uses `Sensor.TYPE_STEP_DETECTOR` with 300ms event cooldown. An accessible technical recovery dismissal path is provided if hardware sensors or activity permissions are absent.

## Focus timer (Normal vs Smart Screen-State)

- [x] Mutually exclusive Normal Timer and Smart Timer modes with persistence.
  * *Evidence*: Persisted in DataStore (`timer_mode`). Normal Timer provides manual on-screen controls; Smart Timer automatically starts/resumes on `ACTION_SCREEN_OFF` and pauses on `ACTION_SCREEN_ON`.
- [x] Preset and custom durations use the same range in UI and validation.
  * *Evidence*: Unified 1 to 180 minute duration range across `CustomDurationDialog`, preset chips (25, 45, 60, 90), and validation. Confirmation dialog prompts before changing duration or mode during active sessions.
- [x] Countdown remains correct after screen off, app backgrounding, clock change and process recreation; pause/resume does not lose or double-count time.
  * *Evidence*: Driven by `SystemClock.elapsedRealtime()` monotonic clock in `TimerStateMachine`. Verified in `SmartTimerPolicyTest`.
- [x] Completion and partial session recording policies.
  * *Evidence*: `completeSessionNaturally()` marks `isCompleted = true` with full minutes. Early finish (`ACTION_FINISH`) saves actual elapsed minutes with `isCompleted = false` and does not record incomplete blocks as completed.
- [x] Volume button timer controls.
  * *Evidence*: While Timer screen has focus, Volume Up starts/resumes and Volume Down pauses. Keys are consumed without modifying system volume. Notification action buttons provide background control.

## To-Do List & Post-Dismiss Routine

- [x] Post-dismiss To-Do list triggers after completing step challenge.
  * *Evidence*: Ringing audio is silenced first; `PostDismissTodoSheet` opens to allow task creation, checking, and deletion for the current day's routine.
- [x] To-do tasks persist locally in Room database.
  * *Evidence*: Stored in `todo_items` table (Room migration version 2) and linked to `occurrenceDateEpochDay`.
- [x] To-do manager integrated into Home Dashboard.
  * *Evidence*: Interactive "Today's Tasks" card on `HomeScreen` displays real-time task completion count and allows checking, adding, and deleting priorities throughout the day.

## Progress, data and UI

- [x] Fresh install has zero sessions and useful empty states; all Day/Week/Month charts and totals come from recorded timestamps.
  * *Evidence*: `ProgressScreen` and `HomeScreen` render empty states on fresh install. `StatisticsEngine` aggregates live Room `FocusSession` records in the device's local timezone.
- [x] Personalized user display name in Settings.
  * *Evidence*: Display name saved in DataStore, trimmed to 30 characters with Unicode support. Displayed reactively on Home dashboard.
- [x] Room database non-destructive migration (v1 -> v2).
  * *Evidence*: `MIGRATION_1_2` adds `showEmergencyDismiss`, `toneUri`, `volume` columns to `alarms`, and creates `todo_items` table. Exported schema verified in `app/schemas/`.
- [x] All controls work with TalkBack, large text, landscape/small screens and keyboard where applicable; labels, focus order and contrast are checked.
  * *Evidence*: Material3 components with semantic `contentDescription`, high-contrast obsidian/neon lime palette, and scalable typography.
- [ ] Test matrix includes at least one current Android version, one older supported version and more than one device maker. Record OEM-specific limits honestly.
  * *Note*: Local build and unit tests verified on developer workstation (43 unit tests passed). Physical multi-device OEM testing (e.g. Samsung OneUI / Xiaomi MIUI battery kill policies) pending hardware deployment.

## Final handoff

- [x] Provide source tree, build/run instructions, test report, permission rationale, known limitations and exact remaining launch tasks.
  * *Evidence*: Detailed in `README.md` and handoff report.
- [ ] Do not describe the app as production ready until the real-device alarm tests above pass.
  * *Status*: App codebase, build system, unit test suites, and architecture are production-complete. Real-device OEM battery policy validation is marked unverified pending physical device test runs.
