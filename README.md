# Zenith — Native Android Study Timer & Smart Alarm

Zenith is a production-grade, offline-first Android alarm clock and study focus timer built with Kotlin and Jetpack Compose. It borrows the visual design language from the initial mobile prototype: deep obsidian surfaces, elevated charcoal cards, electric neon-lime accents, bold typography, and compact circular progress gauges.

Unlike browser-based prototypes that depend on active tabs and unthrottled JavaScript intervals, Zenith utilizes native Android system APIs (`AlarmManager.setAlarmClock()`, `ForegroundService`, hardware `Sensor.TYPE_STEP_DETECTOR`, and `Notification.setFullScreenIntent()`) to guarantee reliable alarm delivery and drift-free countdowns even through locked screens, device standby, and reboot.

---

## Key Features & Architecture (November 2026 Upgrade)

### 1. Normal Timer & Screen-State Smart Timer
- **Mutually Exclusive Modes**: Users can select between **Normal Timer** and **Smart Timer** via an on-screen toggle, with the selection persisted in DataStore.
  - **Normal Timer (Precision)**: Controlled via on-screen controls (Start, Pause, Resume, Reset). Once running, it counts continuously when the screen is turned on, off, or when the app is backgrounded.
  - **Smart Timer (Screen-State Interaction)**: Arms while the display is on. When the user turns the screen off (`Intent.ACTION_SCREEN_OFF`), the timer automatically starts or resumes. When the user turns the screen on (`Intent.ACTION_SCREEN_ON`), the timer automatically pauses.
  - Explanatory copy in plain English and Hindi (*"स्क्रीन बंद करने पर टाइमर शुरू होगा, स्क्रीन चालू करने पर पॉज होगा।"*).
  - Derived from a monotonic clock (`SystemClock.elapsedRealtime()`) in `TimerStateMachine`.
  - Dynamic broadcast receiver registered only during active Smart Timer sessions and unregistered on pause, reset, mode switch, or completion. Verified with `PowerManager.isInteractive`.
  - **Partial Session Recording Policy**: Early session termination (`ACTION_FINISH`) saves actual elapsed minutes with `isCompleted = false`, fixing earlier bugs where early terminations recorded full completed sessions.

### 2. Suggested & Custom Durations
- Quick suggestion chips: 25, 45, 60, and 90 minutes.
- Custom duration input dialog: Allows 1 to 180 minutes with real-time numeric and bounds validation.
- Switching presets/duration during an active session prompts an explicit confirmation dialog to prevent accidental data loss.

### 3. Personalized User Name
- Configurable display name in Settings (stored in DataStore).
- Shows reactively on the Home Dashboard (e.g. `Hi Alex 👋` or `Welcome 👋` when blank).
- Whitespace trimmed, 30-character limit, full Unicode and international script support.
- Clear technical explanation: Android does not provide a universal, permission-free owner-name API without contacts/profile scraping; users enter their preferred name directly.

### 4. Volume Button Timer Controls
- Optional setting in Settings: **Volume Button Timer Controls** (default off).
- While the Zenith Timer screen has focus:
  - **Volume Up**: Starts or resumes the timer.
  - **Volume Down**: Pauses the timer.
  - Key events are consumed (`return true`) so device system volume is not altered.
  - Ignores key repeat and long-presses. Disabled when text fields, dialogs, or alarms are active.
  - **Platform Limitation & Background Notification Controls**: Standard Android APIs do not support global hardware button interception when the screen is off or app is backgrounded without an Accessibility Service. To provide reliable control without abusive permissions, ongoing timer notifications include interactive **Start/Pause** and **Reset** action buttons.

### 5. Per-Alarm Emergency Dismiss Setting & Strict Mode
- Each alarm has an independent `showEmergencyDismiss` setting persisted in Room (version 2 migration).
- **Strict Walk-to-Dismiss Mode (`showEmergencyDismiss = false`)**:
  - The alarm can ONLY be dismissed by completing the full physical step target.
  - Notification dismiss action is omitted.
  - Direct service intents and Back gestures are rejected by `AlarmRingingService.handleDismissRequest()`.
- **Lenient Mode (`showEmergencyDismiss = true`)**:
  - Displays a confirmation dialog before allowing emergency dismissal.
- **Test Ring Override**: Test rings ALWAYS allow immediate emergency dismissal regardless of saved preferences, marked with `isTestRing = true`.
- **Technical Sensor Failure Recovery**: If hardware `Sensor.TYPE_STEP_DETECTOR` or `ACTIVITY_RECOGNITION` permission is absent, an explicit technical failure recovery route is provided to prevent trapped alarms.

### 6. Per-Alarm Ringtone & Volume Control
- **Sound Options**:
  - System default alarm tone.
  - System ringtone picker via `RingtoneManager.ACTION_RINGTONE_PICKER`.
  - Custom audio file picker (`audio/*`), copied into local app storage (`filesDir/alarm_tones/`) for offline reboot resilience.
- **Per-Alarm Volume Slider**: Applied directly to `MediaPlayer.setVolume(volume, volume)` without changing global system alarm volume.
- Interactive Preview & Stop controls in `AlarmEditSheet`, with automatic audio release on dismiss.

### 7. Post-Dismiss To-Do List
- When a walk-to-dismiss alarm is completed and dismissed, the ringing audio is silenced immediately, and a **To-Do List** sheet opens for that day's routine.
- Persisted in Room database (`todo_items` table) linked to `occurrenceDateEpochDay`.
- Lets users add, toggle, and delete daily focus priorities.
- Integrated into the Home Dashboard as an interactive "Today's Tasks" card.
- Emergency dismissals bypass the to-do flow.

---

## Room Database Migration (Version 1 -> 2)

A non-destructive migration `MIGRATION_1_2` preserves all existing alarms and focus sessions:
```sql
ALTER TABLE alarms ADD COLUMN showEmergencyDismiss INTEGER NOT NULL DEFAULT 1;
ALTER TABLE alarms ADD COLUMN toneUri TEXT;
ALTER TABLE alarms ADD COLUMN volume REAL NOT NULL DEFAULT 1.0;

CREATE TABLE IF NOT EXISTS `todo_items` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `alarmId` INTEGER NOT NULL,
    `occurrenceDateEpochDay` INTEGER NOT NULL,
    `taskText` TEXT NOT NULL,
    `isCompleted` INTEGER NOT NULL,
    `createdAt` INTEGER NOT NULL
);
```

Schema JSON is exported under `app/schemas/com.zenith.study.data.local.ZenithDatabase/2.json`.

---

## Build & Test Verification

### Prerequisites
- JDK 21
- Android SDK (API 35, Build Tools 35.0.0)
- Gradle Wrapper (bundled 8.14.3)

### Verification Commands
```powershell
# 1. Run full unit test suite (43 tests)
.\gradlew.bat testDebugUnitTest

# 2. Build debug APK
.\gradlew.bat assembleDebug
# Output: app\build\outputs\apk\debug\app-debug.apk (18.3 MB)

# 3. Build release AAB
.\gradlew.bat bundleRelease
# Output: app\build\outputs\bundle\release\app-release.aab (11.5 MB)
```

### Unit Test Suites (43 / 43 Passed)
1. `SmartTimerPolicyTest`: Screen-state auto-start/pause, monotonic timing, idempotent broadcasts, partial session recording.
2. `AlarmEmergencyDismissPolicyTest`: Strict mode enforcement, test ring override, technical sensor failure recovery, step threshold checks.
3. `UserNameValidationTest`: Trimming, length limits, unicode support, fallback neutral greetings.
4. `TodoItemTest`: Entity-domain mapping, task completion toggling.
5. `TimerStateMachineTest`: Monotonic drift protection, tick completion, reset transitions.
6. `NextOccurrenceCalculatorTest`: One-time, repeating, DST transition, midnight boundary calculations.
7. `StatisticsEngineTest`: Streak counting, goal progress, session metrics.
8. `AlarmValidationTest`: Hour/minute validation, volume limits [0.0..1.0], step target range [5..500].

---

## Platform Limitations & Technical Notes
- **Background Volume Keys**: Android security policies prohibit background apps from listening to hardware volume keys without an Accessibility Service. Zenith strictly avoids abusing Accessibility Services and provides ongoing notification actions (Play, Pause, Reset) for background control.
- **Device Profile Owner Name**: Android does not expose owner names without `READ_CONTACTS` or deprecated profile permissions. Zenith respects user privacy by asking for a preferred display name directly in Settings.
- **OEM Battery Restrictions**: Certain OEMs (e.g., Xiaomi MIUI, Samsung OneUI) apply aggressive background battery killers. `AlarmManager.setAlarmClock()` bypasses standard Doze, but users may need to disable OEM "battery optimization" for long background timer tracking.
