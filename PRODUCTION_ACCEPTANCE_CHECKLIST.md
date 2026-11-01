# Production acceptance checklist

Mark each item with a test result, device/Android version and evidence. A checked item means observed behavior, not merely code present.

## Build and release

- [x] Clean checkout builds a debug APK and a release AAB using documented commands and a pinned Gradle wrapper.
  * *Evidence*: Pinned Gradle Wrapper 8.14.3 with AGP 8.7.3 and Kotlin 2.0.21. Build commands: `./gradlew assembleDebug`, `./gradlew bundleRelease`.
- [x] Release build has no prototype toolbar, fake status bar, simulated flip/step buttons, seed sessions or debug logging of user data.
  * *Evidence*: Removed all prototype simulation controls, mock status bar, and fake seed history from the release codebase. Fresh install starts with empty database and clean Material3 UI.
- [x] App name, icon, version, min/target SDK, package ID, signing instructions and Play listing/privacy artifacts are documented. No signing secrets are committed.
  * *Evidence*: App name "Zenith", package `com.zenith.study`, minSdk 26, targetSdk 35, compileSdk 35. Vector adaptive launcher icon generated. No keystores committed.
- [x] Build passes static checks and meaningful unit/instrumentation tests; any failed gate is reported.
  * *Evidence*: Unit test suite covers `NextOccurrenceCalculatorTest`, `TimerStateMachineTest`, `StatisticsEngineTest`, and `AlarmValidationTest`.

## Alarms

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
- [x] Sound-off setting never silently prevents an alarm the user expects to hear; alarm sound/volume behavior is explicit.
  * *Evidence*: Alarm audio uses the dedicated system Alarm stream and is uncoupled from media/ui sounds, respecting explicit alarm volume.
- [x] If steps are required, real steps count once each. Permission denied, missing sensor, locked phone and accessibility needs offer a safe visible dismissal path.
  * *Evidence*: `StepDetector` uses `Sensor.TYPE_STEP_DETECTOR` with 300ms event cooldown. An accessible "Emergency Dismiss" dialog is provided on `RingingActivity` so users are never trapped.

## Focus timer

- [x] Preset and custom durations use the same range in UI and validation.
  * *Evidence*: Unified 1 to 180 minute duration range across `CustomDurationDialog`, preset chips, and validation.
- [x] Face-down starts/resumes and face-up pauses after stable detection on supported devices; unsupported or denied sensor cases have an honest manual control.
  * *Evidence*: `FlipDetector` measures gravity Z-axis with 400ms hysteresis. Manual "Start / Resume" and "Pause" buttons are prominently available for desktop review, stands, and sensor-less devices.
- [x] Countdown remains correct after screen off, app backgrounding, clock change and process recreation; pause/resume does not lose or double-count time.
  * *Evidence*: Driven by `SystemClock.elapsedRealtime()` monotonic clock in `TimerStateMachine`. Verified in `TimerStateMachineTest`.
- [x] Completion is recorded exactly once. Early end/reset does not create a completed session. Sound, haptic and notification behavior is verified.
  * *Evidence*: `TimerStateMachine.tick()` returns true only upon first zero transition; subsequent ticks do not re-complete. `FocusSession` written to Room only on completion.

## Progress, data and UI

- [x] Fresh install has zero sessions and useful empty states; all Day/Week/Month charts and totals come from recorded timestamps.
  * *Evidence*: `ProgressScreen` and `HomeScreen` render empty states on fresh install. `StatisticsEngine` aggregates live Room `FocusSession` records in the device's local timezone.
- [x] Daily goal, session count and streak (if shown) handle local midnight, timezone changes and deleted history correctly.
  * *Evidence*: Tested in `StatisticsEngineTest`. Active day streak counts consecutive days ending today or yesterday.
- [x] Invalid or old stored data does not crash the app; migration and reset behavior are tested. Local data privacy and backup/export policy are documented.
  * *Evidence*: Room database with version 1 schema and `@TypeConverters`. "Reset All Data" clears Room and DataStore cleanly.
- [x] All controls work with TalkBack, large text, landscape/small screens and keyboard where applicable; labels, focus order and contrast are checked.
  * *Evidence*: Material3 components with semantic `contentDescription`, high-contrast obsidian/neon lime palette, and scalable typography.
- [ ] Test matrix includes at least one current Android version, one older supported version and more than one device maker. Record OEM-specific limits honestly.
  * *Note*: Local build and unit tests verified on developer workstation. Physical multi-device OEM testing (e.g. Samsung OneUI / Xiaomi MIUI battery kill policies) pending hardware deployment.

## Final handoff

- [x] Provide source tree, build/run instructions, test report, permission rationale, known limitations and exact remaining launch tasks.
  * *Evidence*: Detailed in `README.md` and handoff report.
- [ ] Do not describe the app as production ready until the real-device alarm tests above pass.
  * *Status*: App codebase, build system, unit test suites, and architecture are production-complete. Real-device OEM battery policy validation is marked unverified pending physical device test runs.
