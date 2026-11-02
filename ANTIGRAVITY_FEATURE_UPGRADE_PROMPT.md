# Antigravity prompt — Zenith feature upgrade (2 November 2026)

You are modifying the **existing native Kotlin/Jetpack Compose Android app** in `D:\AALRM & Smart timer`. This is a feature implementation task, not a request for another proposal, HTML prototype, WebView, or redesign. Read `README.md`, `PRODUCTION_ACCEPTANCE_CHECKLIST.md`, the source tree under `app/src`, and existing tests before editing. Inspect the current behavior, make the changes below end to end, build and test the result, and report evidence. Keep existing alarms, sessions and preferences across upgrades. Do not mark unrun device tests as passed.

## Current code to inspect first

- Timer: `TimerScreen.kt`, `FocusTimerService.kt`, `TimerStateMachine.kt`, `FlipDetector.kt`, `MainViewModel.kt`, `MainActivity.kt`.
- Check the current `ACTION_FINISH` / “End Early” path: it calls `completeSession()` and may record an early end as a completed block. Correct this while changing timer modes; preserve a clear partial-session policy.
- User name/settings: `HomeScreen.kt` currently hardcodes `Hi Alex`; `SettingsScreen.kt`, `UserSettings.kt`, `UserPreferencesRepository.kt`.
- Alarm editor and ringing: `Alarm.kt`, `AlarmEntity.kt`, `AlarmEditSheet.kt`, `AlarmRingingService.kt`, `RingingActivity.kt`, `StepDetector.kt`, alarm repository/scheduler, `ZenithDatabase.kt`.
- Existing custom duration is 1–180 minutes; presets are 25/45/60/90. Existing alarm `tone` is stored but playback currently uses the system default. The ringing notification currently has an unconditional Emergency Dismiss action; it must not bypass the new alarm-specific rule.
- Recheck alarm editor Compose state when switching between different alarms; new settings must load the selected alarm's persisted values instead of stale remembered values.

## 1. Normal Timer and Smart Timer, based on screen state

Replace the face-down/flip start-pause rule entirely. Provide two clearly labeled, mutually exclusive modes on the Timer screen, and persist the user's selected mode:

- **Normal Timer:** Start, Pause, Resume and Reset using on-screen controls. Once running, it keeps counting when the display turns off, when the user leaves the app, or when the display turns on. Screen state must never auto-pause it.
- **Smart Timer:** The selected timer is armed while the display is on. When the user turns the display off, start or resume it automatically. When the display turns on, pause it automatically. If the user arms Smart Timer while the screen is already off through a supported entry point, reconcile current state and start once. Do not automatically restart a completed or explicitly ended session. Explain this behavior in short, clear Hindi or plain English UI copy.
- Use `ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON` through a dynamically registered receiver in the active timer service; verify actual state with `PowerManager.isInteractive()` or display state where appropriate. Do not put these broadcasts in a manifest receiver. Register only for an active Smart Timer and unregister on mode switch, reset, completion and service destruction. Handle duplicate or out-of-order events idempotently. Decide and document the policy for incoming calls, proximity-only blanking, lock screen, process death and reboot; do not silently count time contrary to the policy.
- Keep the countdown derived from a monotonic time source, with state persisted/restored. A tick/display refresh is not the source of truth. Use the foreground service only for the active timer in a way allowed by current Android service-type and notification rules. Reconcile after activity/service recreation. Remove flip-specific code, notifications, settings, permission text, tests and UI; remove or retire `FlipDetector` if unused. The existing `autoFlipEnabled` preference must be migrated to the new mode without corrupting old settings; choose a clear default (Normal Timer unless there is a strong reason otherwise).
- Changing mode or duration during a running session requires an explicit rule and confirmation; never silently discard focus time.

## 2. Suggested durations and manual duration

Keep quick suggestions such as 25, 45, 60 and 90 minutes. Show which one is selected. Provide a clear **Custom** entry where the user can type their own minutes (same valid range in UI, domain validation and tests; retain 1–180 unless a documented product decision changes it). Persist the selected duration. Reject empty, nonnumeric and out-of-range values with inline error text. Switching preset/custom must update the actual timer state and display, not just a local Compose chip state. The Home “Start Session” shortcut should respect the selected duration and mode or clearly state that it launches a 25-minute session.

## 3. Editable user's name

Remove every hardcoded `Alex` greeting from the native app. Add **Your name** in Settings with edit, save, validation and reset. Store the preferred display name in DataStore; show it reactively on Home and anywhere else the app addresses the user. Trim whitespace, limit length reasonably, support Unicode names and show a neutral greeting if no name has been set.

The request is to show the phone user's name when available. Android does not provide a reliable, universal, permission-free owner-name API. On first use, ask the person for a preferred name. An optional **Import from device profile** action is acceptable only if a supported profile source is actually available, the user deliberately initiates it, required permission is explained/obtained, and the imported name remains editable. Never scrape accounts/contacts silently, display an email address as a name, or invent a name. Document this limitation honestly.

## 4. Start, pause and resume with volume buttons

Add an optional **Volume button timer controls** setting (default off) with a visible explanation. While the Zenith timer screen/activity has focus, map **Volume Up** to Start/Resume and **Volume Down** to Pause; ignore repeated/long-press events unless explicitly designed. Use the same ViewModel/service commands and state checks as the on-screen buttons, and provide a clear confirmation or status. Do not trigger timer controls while the alarm is ringing, a text field/dialog has focus, or another app is in front. When the feature is off, volume keys must retain normal system behavior. Do not change the device's system volume behind the user's back.

Investigate whether a documented public Android API can support these exact buttons with the screen off or app in background on the selected Android versions. Do **not** claim global/background hardware-button capture if the platform does not guarantee it, and do not add an Accessibility Service just to intercept keys. If unavailable, keep the supported focused-app control and add Start/Pause/Resume actions to the timer notification for background/screen-off use. State the supported scope clearly in Settings and the handoff report. Smart Timer screen-off auto-start remains independent of volume-key handling.

## 5. Per-alarm Emergency Dismiss choice

Add an **Show Emergency Dismiss** switch to the Create/Edit Alarm sheet, saved per alarm and retained by a real Room migration. Define the behavior consistently across the ringing screen, notification actions, lock-screen UI and service:

- For an actual walk-to-dismiss alarm, when the switch is **on**, show a clearly labeled Emergency Dismiss path with confirmation. When **off** and the step sensor/permission is working, normal dismissal is enabled only after the configured real step count. No notification action, Back gesture, stale PendingIntent or direct service intent may bypass this rule. The service must validate the alarm ID, active ringing state and step completion/allowed override; hiding a button alone is insufficient.
- **Test Ring must always show an Emergency Dismiss path**, even if the saved alarm's switch is off. Mark the event as a test from the start; isolate it from actual schedules and avoid changing the saved preference. The notification and service must use the same test override rule.
- For standard alarms without a step challenge, Dismiss remains immediately available. If a step detector or Activity Recognition permission becomes unavailable after an alarm was saved, never leave an indefinitely ringing, undismissable alarm: explain the failure and provide a deliberate recovery dismissal. Ideally warn/block step-only configuration at save time when hardware or permission is absent. This technical failure route must be distinct from the ordinary user-configured Emergency Dismiss switch.
- Default the new switch for existing alarms to **on** to preserve the current emergency path; new alarms may default on. Make the choice explicit to users. Do not allow a notification-level “Emergency Dismiss” action to bypass the off setting.

## 6. Per-alarm ringtone and volume

In Create/Edit Alarm, add a **Sound** section with: **System default alarm tone**, **Choose a system ringtone**, and **Choose an audio file**. Show the selected sound name and a Preview/Stop control. Use the Android ringtone picker or document picker with `audio/*` as appropriate. Persist the selected URI access across reboot via supported URI permission or make a private copy where needed; validate that the audio can be opened. If the file is moved, deleted, inaccessible or fails playback, fall back visibly to a known working system alarm tone and report the issue in the alarm editor. Avoid broad storage permission.

Add a per-alarm volume control with a clear scale and preview. Apply it to that alarm's player/audio path only, without setting global device alarm/media volume. State that the effective sound is still affected by device alarm volume, audio route and system policies; warn if it may be inaudible. Keep playback looping and stop preview when leaving the sheet. Ensure actual ring and Test Ring use the chosen sound/level. Existing alarms migrate to “System default” and the current effective level. Separate this from UI sound and vibration settings. Add playback cleanup, error handling and tests.

## 7. To-do list after completing steps

For a walk-to-dismiss alarm, once the real step target is reached and the user completes dismissal, stop the ringing sound/service first, then open a **To-do list** screen or sheet for that alarm occurrence. It must let the user add, edit, check/uncheck and delete multiple tasks; persist them locally in Room and display them later through a clear entry point (for example Home or a To-do tab). Associate tasks with their alarm/occurrence date so repeated alarms do not mix old and new tasks. Show an empty state. Provide Done/Close; entering tasks is optional and must not keep the alarm sounding. Standard alarms should dismiss normally without forcing this screen. For Test Ring, show the same flow for inspection but do not silently create permanent tasks unless the user explicitly saves them.

If the alarm fires above the lock screen, protect task text: present the to-do editor only after unlock or in the regular unlocked app. Persist a pending “open to-do” handoff so it survives activity/process recreation. Explain how the flow handles emergency dismissal (suggested: open the list only after step-completed dismissal, not emergency dismissal).

## Data, migration and security requirements

- Add Room migration(s) from the current schema version; never use destructive migration for existing alarms/sessions. Export Room schema and test migration with a database containing old rows. Add DataStore defaults/migration for name, timer mode, duration and volume-control preference.
- Do not trust UI-only state for an alarm's emergency setting, test status, step count, volume or ringtone. Treat notification/service intents as untrusted input, use explicit intents and unique PendingIntents, and reject stale or mismatched IDs.
- Update README, manifest/permissions, strings, relevant settings copy and `PRODUCTION_ACCEPTANCE_CHECKLIST.md`. Preserve existing visual theme and accessibility support; provide TalkBack labels for all new controls and large-text layouts.
- Do not change unrelated alarm scheduling behavior without a documented reason. Do not mark the app production ready based only on a successful Gradle build.

## Required verification and handoff

Add unit tests for Normal versus Smart Timer transitions, repeated screen broadcasts, mode/duration changes, pause/resume timing, name validation, per-alarm emergency policy (including direct notification/service attempts), test-ring override, ringtone/volume selection and to-do state. Add integration or instrumentation tests where practical for screen receiver lifecycle, Room migration, picker URI persistence, notification actions, step challenge and post-dismiss navigation. Run `assembleDebug`, unit tests and relevant lint; report exact commands and outcomes. On real devices test display off/on, lock/unlock, screen-off start, app background/process recreation, volume keys with app focused and background, alarm locked-screen dismissal with emergency on/off, test ring, unavailable sensor, custom audio after reboot, and to-do handoff after unlock. Record device/Android version and evidence; mark unavailable device tests unverified.

At completion provide a short implementation summary, list of changed files, migration version, build/test results, observed device behavior, platform limitations and remaining release work. If a feature cannot be implemented as requested with standard Android APIs, implement the nearest honest supported behavior above and explicitly identify the limitation. Do not leave a fake UI control that appears to work but is not connected to the service or persisted state.

## Official Android sources to check before implementation

- Screen interactive broadcasts (dynamic registration): https://developer.android.com/reference/android/content/Intent#ACTION_SCREEN_OFF
- Foreground service restrictions and types: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start and https://developer.android.com/develop/background-work/services/fgs/service-types
- Volume/media key scope: https://developer.android.com/reference/android/view/KeyEvent and https://developer.android.com/reference/android/media/session/MediaSession
- Device profile sensitivity: https://developer.android.com/identity/providers/contacts-provider
- System ringtone picker: https://developer.android.com/reference/android/media/RingtoneManager
- File picker and persisted URI access: https://developer.android.com/training/data-storage/shared/documents-files
- Per-player volume: https://developer.android.com/reference/android/media/MediaPlayer#setVolume(float,%20float)

Verify current platform and Play rules in these sources. Do not copy claims from the old HTML prototype.
