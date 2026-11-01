# Prototype audit — 31 October 2026

Source reviewed: `smart aalrm index.html` (single file, about 126 KB; inline CSS, HTML and JavaScript). This is a static code review; no Android app or real-device run exists in this folder.

## What already exists

- Five designed screens: Home, Timer, Alarms, Progress and Settings; an alarm editor and ringing overlay.
- CRUD for locally stored alarm definitions, weekday selection, walk-to-dismiss UI, timer presets, sensor listeners, synthesized sound and visual progress.
- A useful dark and lime visual reference that should guide the Android implementation.

## Release-blocking findings

| Priority | Finding and source evidence | Production requirement |
| --- | --- | --- |
| P0 | **No scheduled alarm delivery.** Alarm create/edit/toggle handlers save objects to `localStorage` (around lines 3012–3165). `triggerRingingAlarm()` (around 3205) is called only by Test Ring buttons. The only periodic timer is the study countdown and display clock; there is no scheduler that checks a due alarm. | Schedule each active alarm through Android's alarm APIs; reschedule on edits, disable/delete, reboot, clock/timezone changes and permission changes. Test locked screen and Doze. |
| P0 | **Countdown depends on a one-second JavaScript interval** (around 2732–2746). Active timer state is memory-only (around 2639–2646), so reload/process death loses it; background throttling causes drift. | Persist session state and calculate remaining time from an appropriate monotonic clock; restore correctly and use a supported background execution model only where needed. |
| P0 | **Ringing depends on an open browser and initialized Web Audio** (around 2428–2525, 3205–3240). The sound toggle also suppresses alarm tones. | Native alarm notification/audio route with distinct alarm-volume behavior, lock-screen flow, and audible/permission failure handling. |
| P0 | **Walk-to-dismiss can block dismissal without a usable sensor.** The button stays disabled until a step target (around 3250–3285); hardware step detection uses `devicemotion` only while the overlay is visible (around 3305–3329), and step simulation buttons remain in the UI. | Use Android step sensor with permission and an accessible, safe fallback when permission/sensor is unavailable or motion is not possible. Remove simulation from release UI. |
| P1 | **Progress is misleading.** Default state starts with four fake sessions (around 2568–2576). Today totals sum every session regardless of date (around 3337 and 3411). Weekly bars use `sampleHours` and always treat Sunday as today (around 3430–3440). Day/Week/Month tabs only change styling (around 3490). | Start with empty real data; derive local-day, week and month views from timestamps; handle date boundaries and timezone changes. |
| P1 | **Next alarm is the first active item, not the next chronological occurrence** (around 3332–3334). If none is active, it falls back to the first inactive alarm. | Compute and show the actual next scheduled occurrence; show a clear empty state if none is active. |
| P1 | **Alarm and timer input validation is incomplete.** No-day selection becomes `['Every day']` (around 3048/3061), which does not match the weekday model. Custom timer markup says max 180 while JavaScript accepts 300 (around 2343/2865). | Define and validate one-time versus repeating alarms, ranges, labels, duplicate handling and clear user errors. |
| P1 | **Saved data has no schema version/migration or validation** (around 2580–2601). Corrupt or old shapes can break rendering. Reset reattaches settings event listeners by calling `initSettingsUI()` again (around 3535). | Use versioned local persistence, migration and safe recovery; avoid repeated listeners and silent loss. |
| P1 | **Some settings are only visual.** `vibrationEnabled` is saved but no vibration action is called. The selected `tone` argument does not change the siren (around 2499–2514). | Make each exposed setting functional or remove it. Preview and test tones. |
| P1 | **Accessibility and responsive behavior need rework.** The viewport disables zoom with `user-scalable=no` (line 5), global CSS disables selection, several icon buttons rely on `title`, switch controls lack accessible names, and a clickable `<div>` wraps a button (around 1818). | Native semantic controls, screen-reader labels, focus/order, touch targets, scalable text, large font and narrow-screen QA. |
| P2 | **Prototype UI and claims are exposed to users.** Fake Android status bar, prototype quick bar, simulator buttons and “Android Production Architecture” explanation are part of the app UI (around 1757–1795, 2175–2204). | Remove these from release screens; keep debug tools in a debug-only build. |

## Architecture correction

The prototype's Settings screen suggests that `setExactAndAllowWhileIdle()` plus `USE_EXACT_ALARM` and a foreground service guarantee alarms and timers. That is too absolute. Android offers `setAlarmClock()` for highly visible alarm clocks and has distinct exact-alarm permission and Play policy paths; foreground service launch/type rules also vary by Android version. The implementation must choose APIs by use case and verify current official documentation rather than copy these prototype claims.

Official references: [Android alarm scheduling](https://developer.android.com/develop/background-work/services/alarms), [full-screen alarm notifications](https://developer.android.com/develop/ui/compose/notifications/create-notification), [foreground service restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start), [step detector sensor](https://developer.android.com/reference/android/hardware/Sensor), [Google Play sensitive permission policy](https://support.google.com/googleplay/android-developer/answer/16558241).
