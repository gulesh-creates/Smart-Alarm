# Zenith — Native Android Study Timer & Smart Alarm

Zenith is a production-grade, offline-first Android alarm clock and study focus timer built with Kotlin and Jetpack Compose. It borrows the visual design language from the initial mobile prototype: deep obsidian surfaces, elevated charcoal cards, electric neon-lime accents, bold typography, and compact circular progress gauges.

Unlike browser-based prototypes that depend on active tabs and unthrottled JavaScript intervals, Zenith utilizes native Android system APIs (`AlarmManager.setAlarmClock()`, `ForegroundService`, hardware `Sensor.TYPE_STEP_DETECTOR`, and `Notification.setFullScreenIntent()`) to guarantee reliable alarm delivery and drift-free countdowns even through locked screens, device standby, and reboot.

---

## Key Features

1. **Smart Alarms & Scheduling**
   - Scheduled via Android's `AlarmManager.setAlarmClock()`, ensuring the device wakes reliably from Doze and deep sleep.
   - Shows the system alarm icon in the Android status bar and updates the system's lock screen alarm glance.
   - Idempotent scheduling: old `PendingIntent`s are cancelled before new occurrences are set.
   - Automatically handles one-time alarms (retired after ringing) and recurring weekday patterns.
   - Re-registers alarms on device restart (`BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`) and local clock/timezone changes (`TIME_SET`, `TIMEZONE_CHANGED`).

2. **Full-Screen Ringing & Walk-to-Dismiss Challenge**
   - Launches over the keyguard on locked screens via `setFullScreenIntent()` with `showWhenLocked="true"` and `turnScreenOn="true"`.
   - Ringing audio loops through `AudioAttributes.USAGE_ALARM` with transient audio focus management and alarm volume stream routing.
   - Physical step counter powered by hardware `Sensor.TYPE_STEP_DETECTOR` and `ACTIVITY_RECOGNITION`.
   - The primary **Dismiss Alarm** button remains locked until the configured step target (e.g. 20, 30, 50 steps) is reached.
   - **Accessible Emergency Dismiss Fallback**: An immediate emergency dismissal dialog is accessible at all times so users are never trapped if physical movement is impossible, permission is denied, or the hardware sensor is absent.

3. **Study Focus Timer with Flip-to-Start**
   - Presets (25, 45, 60, 90 minutes) and custom durations (1 to 180 minutes).
   - Driven by a pure monotonic state machine (`SystemClock.elapsedRealtime()`) — immune to background throttling and system wall-clock adjustments.
   - **Flip-to-Start Detection**: Monitors `Sensor.TYPE_GRAVITY` / `Sensor.TYPE_ROTATION_VECTOR` with a 400ms dwell time/hysteresis to detect when the phone is placed face-down on a desk to begin/resume, and pauses when picked up face-up.
   - Clear manual Start/Pause/Resume fallback controls for desk stands and devices lacking sensors.
   - Supported by a low-profile `ForegroundService` with ongoing notification.

4. **Accurate Local Statistics & Progress**
   - Fresh install starts completely empty (zero fake sessions).
   - Real-time aggregation of today's focus minutes, 7-day Monday–Sunday bar chart, monthly totals, and active day streaks calculated from timestamps in the device's local timezone (`ZoneId.systemDefault()`).
   - Day / Week / Month tab filters dynamically recalculate the dataset and chart scales.

5. **Local Persistence & Data Safety**
   - **Room Database**: Versioned schema with `AlarmEntity` and `FocusSessionEntity`.
   - **Preferences DataStore**: Stores user preferences (daily focus goal, 12h/24h time format, audio and vibration toggles).
   - **100% Offline-First**: Zero external network permissions (`android.permission.INTERNET` is not requested). No third-party analytics, no tracking, no account required.

---

## Architecture & Code Structure

```
com.zenith.study
├── ZenithApplication.kt             # Application class, notification channels, service locator
├── alarm
│   ├── AlarmScheduler.kt           # Scheduler interface
│   └── AndroidAlarmScheduler.kt    # AlarmManager.setAlarmClock implementation
├── data
│   ├── local
│   │   ├── Converters.kt           # Room TypeConverters
│   │   ├── UserPreferencesRepository.kt # Jetpack DataStore preferences
│   │   ├── ZenithDatabase.kt       # Room SQLite database
│   │   ├── dao
│   │   │   ├── AlarmDao.kt
│   │   │   └── FocusSessionDao.kt
│   │   └── entity
│   │       ├── AlarmEntity.kt
│   │       └── FocusSessionEntity.kt
│   └── repository
│       ├── AlarmRepositoryImpl.kt
│       └── FocusSessionRepositoryImpl.kt
├── domain
│   ├── engine
│   │   ├── NextOccurrenceCalculator.kt # Pure next alarm calculation engine
│   │   ├── StatisticsEngine.kt         # Pure statistics & streak calculation engine
│   │   └── TimerStateMachine.kt        # Pure monotonic countdown state machine
│   ├── model
│   │   ├── Alarm.kt
│   │   ├── FocusSession.kt
│   │   └── UserSettings.kt
│   └── repository
│       ├── AlarmRepository.kt
│       └── FocusSessionRepository.kt
├── receiver
│   ├── AlarmReceiver.kt            # Wakes device and launches ringing service
│   ├── BootReceiver.kt             # Reschedules alarms on device boot / update
│   └── TimeChangeReceiver.kt       # Reschedules alarms on timezone / clock adjustments
├── sensor
│   ├── FlipDetector.kt             # Face-down gravity sensor with 400ms hysteresis
│   └── StepDetector.kt             # TYPE_STEP_DETECTOR hardware step counter
├── service
│   ├── AlarmRingingService.kt      # Foreground service for alarm audio, vibration & steps
│   └── FocusTimerService.kt        # Foreground service for background countdown
└── ui
    ├── MainActivity.kt             # Host Activity with bottom navigation & permissions
    ├── MainViewModel.kt            # Unifies domain engines, repositories, and state
    ├── components
    │   ├── AlarmEditSheet.kt       # Bottom sheet modal for creating & editing alarms
    │   ├── CircularProgressRing.kt # Reusable Canvas progress ring
    │   ├── CustomDurationDialog.kt # Duration dialog (1 - 180 min)
    │   ├── SemicircularGauge.kt    # Arc progress gauge matching reference design
    │   └── ZenithBottomNav.kt      # 5-tab fixed navigation bar
    ├── ringing
    │   └── RingingActivity.kt      # Full-screen lock-screen ringing UI
    ├── screens
    │   ├── AlarmsScreen.kt
    │   ├── HomeScreen.kt
    │   ├── ProgressScreen.kt
    │   ├── SettingsScreen.kt
    │   └── TimerScreen.kt
    └── theme
        ├── Color.kt                # Obsidian, Charcoal, Neon Lime color tokens
        └── Theme.kt                # Material3 Dark Theme
```

---

## Android Permissions Rationale

| Permission | Reason |
| :--- | :--- |
| `USE_EXACT_ALARM` | Required to schedule exact, minute-precise alarms. Permitted under Google Play policy for core alarm clock apps. |
| `SCHEDULE_EXACT_ALARM` | Fallback exact alarm permission for Android 12 (API 31/32). Checked dynamically via `AlarmManager.canScheduleExactAlarms()`. |
| `USE_FULL_SCREEN_INTENT` | Required on Android 14+ (API 34+) to display the ringing alarm over locked keyguard. |
| `POST_NOTIFICATIONS` | Required on Android 13+ (API 33+) to surface ongoing countdown notifications and ringing heads-up alerts. |
| `WAKE_LOCK` | Keeps the CPU awake during alarm ringing and sensor step processing. |
| `VIBRATE` | Vibrates device during alarm ringing. |
| `ACTIVITY_RECOGNITION` | Required on Android 10+ (API 29+) to access hardware `Sensor.TYPE_STEP_DETECTOR` for the walk challenge. |
| `RECEIVE_BOOT_COMPLETED` | Reschedules active alarms following device reboot or app package update. |
| `FOREGROUND_SERVICE` | Required to maintain ringing audio and focus countdown in background. |

---

## Build & Test Instructions

### Prerequisites
- JDK 21 (configured via `JAVA_HOME`)
- Android SDK with platform `android-35` and build-tools `35.0.0`
- Gradle Wrapper (included, Gradle 8.14.3)

### Running Unit Tests
```bash
./gradlew test
```

### Building Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Building Release App Bundle (AAB)
```bash
./gradlew bundleRelease
```
The release bundle will be located at:
`app/build/outputs/bundle/release/app-release.aab`
