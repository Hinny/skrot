# Skrot — Project Plan

**Skrot** is a free, open-source Android strength training app. Built to replace JEFIT for a user who wants a fast, private, no-nonsense logger without ads, community features, or cloud accounts.

This document is the complete specification. Build the entire app described here in one go (no phased MVP), then verify against the acceptance checklist at the end.

---

## 1. Goals & Non-Goals

**Goals**
- Log strength workouts quickly during a gym session (minimal taps).
- Create and manage custom training routines, including supersets/circuits.
- Track progression with clear statistics and graphs.
- Fully offline, all data stored locally on the device.
- Backup and restore via file export/import.
- Import historical training data from JEFIT CSV export.
- Localized in English and Swedish.
- **Guiding principle: configurability.** Behaviors should be adjustable in settings/options wherever reasonable (thresholds, defaults, toggles) rather than hardcoded. When implementing any "magic" behavior described in this spec, expose its knobs in settings.

**Non-Goals (explicitly excluded)**
- No community/social features, no sharing, no feeds.
- No ads, analytics, tracking, or telemetry of any kind.
- No accounts, login, or cloud sync.
- No paywalls or premium tiers — everything is free.

**Units:** all data is stored internally in **kg**. The display unit is **kg by default**, with **lbs available as an option** in settings (conversion at display/input time only; steppers become +/− 5 lbs). Machine levels are unit-less either way.

---

## 2. App Name: Skrot

The app is named **Skrot** (Swedish gym slang, literally "scrap metal"). Use it consistently:

- App display name: **Skrot**
- Repo name: `skrot`
- Package id: `dev.<owner>.skrot` (or similar reverse-domain matching the repo owner)

Note: an unrelated, abandoned hobby repo (`Olivierko/skrot`, a Vue workout tracker, 0 stars, dead since 2022) exists on GitHub. This is not a conflict — the repo lives under the owner's own namespace — but don't reference or reuse anything from it.

---

## 3. Tech Stack (recommended)

The stack choice is delegated to you, but the following is the recommended default for a modern, maintainable, contributor-friendly Android app:

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Database:** Room (SQLite)
- **Architecture:** MVVM with a repository layer; single-module is fine for this scope
- **DI:** Hilt (or manual DI if you judge Hilt to be overkill)
- **Charts:** Vico or a similar Compose-native charting library (avoid heavyweight/abandoned libraries)
- **Serialization:** kotlinx.serialization (for export/import JSON)
- **Min SDK:** 26 (Android 8.0) or higher; target latest stable SDK
- **Build:** Gradle with Kotlin DSL, version catalog

If you have a strong reason to deviate (e.g. a better-maintained chart library), do so and document the choice in the README.

---

## 4. Data Model (guideline)

Design the Room schema yourself, but it must support at least these concepts:

- **Exercise**: name, primary muscle group, optional secondary muscle groups, equipment type, **measurement type** (see below), flag `isCustom`, optional notes. Ship with a built-in seed list of **50–100 common exercises** (barbell/dumbbell/machine/bodyweight basics) with English and Swedish names and sensible default measurement types. Creating a custom exercise must be fast: name + muscle group is enough to save (measurement type defaults to kg).
- **Measurement type (per exercise, editable at any time):**
  - `WEIGHT_KG` — free weights and similar; decimal kg input.
  - `MACHINE_LEVEL` — pin-loaded machines, multi-gyms etc.; integer "level"/step input (1, 2, 3…) with no unit. Progression is tracked in levels, not kg.
  - `BODYWEIGHT` — reps only; the weight field is hidden. Supports optional **added weight in kg** (e.g. weighted dips/chins) and optional assistance as negative kg (assisted machines/bands). Each bodyweight exercise also has a **bodyweight factor** (% of bodyweight actually lifted, editable per exercise) used for volume calculations — e.g. pull-ups ≈ 100 %, push-ups ≈ 65 %, dips ≈ 100 %. Seed exercises ship with sensible defaults; custom exercises default to 100 %.
- **Routine** (a.k.a. **training program** — use "Program" in the UI): an ordered list of routine days (workouts), plus a **pre-fill mode** setting per routine: `LAST_SESSION` (previous actual weight + reps), `TARGETS` (planned weight + reps from the routine), or `HYBRID` (last session's weight, target reps). Default: `LAST_SESSION`. Also: a free-text **description/comment**, an **icon** picked from a curated built-in set (Material Symbols selection: barbell, dumbbell, run, heart, flex, etc.), and an **active flag** — exactly zero or one program is active at a time.
- **RoutineDay** (workout): name, optional **icon** from the same set, optional **description/comment**, ordered list of exercise blocks. A block is either a single exercise or a **superset/circuit group** (2+ exercises performed in alternating fashion).
- **PlannedExercise**: exercise reference, planned sets (each with a **set type** — see below, a **target reps value or range**, e.g. `8` or `8–12`, and optional target load), and a **rest timer duration per set** (in seconds, where `0 = no timer`). Targets follow the same write-back rule as rest timers: edits made from the logging screen persist to the routine.
- **Set types** (per set, both planned and logged): `WARMUP`, `STANDARD`, `DROP_SET`, `FAILURE`. Standard sets are auto-numbered 1, 2, 3… (warmup/drop/failure sets don't consume numbers). Warmup sets are excluded from PR detection, 1RM estimates, and progression logic. `FAILURE` sets have no rep target (AMRAP). Progression suggestions consider standard sets only.
- **WorkoutSession**: date/time, reference to routine day (nullable — ad-hoc workouts allowed), **gym reference** (nullable), session note.
- **ExerciseGroup**: a named group of interchangeable exercises (e.g. "Horizontal press": bench press, machine chest press, DB press, push-ups). An exercise belongs to at most one group. Seed exercises ship pre-grouped; users can create groups and reassign freely.
- **Gym**: name, plus the set of exercises available there (checkbox selection from the library, with bulk helpers like "select all barbell exercises"). One gym can be marked **default**. Each gym may also store per-exercise overrides (see §5.10).
- **Routine (additions)**: free-form **tags** (e.g. `rebuild`), used for filtering and for the comeback suggestion in §5.1.
- **Exercise (addition)**: an optional **next-time note** — a short note written during/after a session that is shown prominently the next time the exercise comes up (e.g. "lower the seat one notch"), then editable/clearable.
- **LoggedSet**: exercise, load value interpreted according to the exercise's measurement type (decimal kg, integer level, or added/assisted kg for bodyweight), reps, set index, completed flag, optional per-set note, the rest duration used.
- **BodyMetric**: date, weight (kg), optional measurements (waist, chest, arms, thighs, etc.).
- **PersonalRecord**: derivable rather than stored, but PR detection must exist (see §5.6).

**Key behavior — rest timer memory:** the rest duration is stored per planned set in the routine. When the user changes a rest duration during a workout, that change is **written back to the routine**, so the next iteration of the same routine uses the updated value. Default for new sets: 90 seconds (configurable in settings).

---

## 5. Features

### 5.1 Routines & Scheduling
- Create/edit/delete routines with named days (e.g. "Day A — Push").
- Reorder exercises and days via drag-and-drop.
- Supersets/circuits: group 2+ exercises; the logging screen alternates between them set by set.
- Two scheduling modes, both supported:
  - **Fixed weekdays:** assign routine days to weekdays (Mon = Legs, Wed = Push…).
  - **Rotating schedule:** days run in sequence (A → B → C → A…) regardless of weekday; the app tracks which day is next.
- The home screen shows "today's / next workout" based on the active schedule, with one tap to start. Starting any other day manually is always possible.
- **Active program:** the user sets one program as active. When the app opens and the previous workout is finished, the home screen automatically presents **the next workout in the program's order** — but always **waits for confirmation** before starting, with an easy option to swap in a different workout (from this or another program) instead. Swapping doesn't break the sequence logic (configurable: either the skipped workout stays next, or the sequence advances — default: skipped stays next).
- **Auto-finish:** an in-progress session with no activity (no set logged, no interaction) for **2 hours** (configurable in settings) is automatically marked as finished, with its end time set to the last activity. The next app open then proceeds with the next-workout flow above.
- Each routine (and routine day) displays **how long ago it was last performed** ("12 days ago") in lists and on the home screen.
- **Comeback suggestion:** if no session has been logged for a configurable number of days (default 14), the home screen suggests starting with routines tagged `rebuild` instead of the regular schedule. Dismissible; purely a suggestion.

### 5.2 Workout Logging
- Start from a scheduled day, any routine day, or an empty ad-hoc session.
- Per set: enter load and reps. The load input adapts to the exercise's measurement type — decimal kg field, integer level stepper, or reps-only with an optional added-weight field for bodyweight exercises.
- **Pre-fill:** what a new/unlogged set is pre-filled with is governed by the routine's pre-fill mode (last session's actuals, routine targets, or hybrid — see §4), so most sets are a single tap to confirm. Ad-hoc exercises without history pre-fill from targets if present, otherwise empty.
- **Target reps display:** each set shows its target (e.g. "8–12") as a reference next to the input fields. Targets are editable inline from the logging screen and the change is written back to the routine, exactly like rest timer durations.
- **Progression suggestion:** when the user hits the target reps (top of the range, if a range) on **all** sets of an exercise at the same load, the next session shows a non-intrusive suggestion to increase the load (default increment +2.5 kg for `WEIGHT_KG`, +1 for `MACHINE_LEVEL`; increment overridable per exercise). It's a suggestion only — accepting updates the pre-fill, dismissing keeps things as-is. Not applicable to plain bodyweight exercises without added weight (suggest +1 rep to the target instead).
- Mark sets as done; add/remove sets on the fly; swap or add exercises mid-session.
- **Rest timer:** starts automatically when a set is marked done, using that set's stored duration; `0` means no timer. Countdown visible in-app and as a notification with sound/vibration; can be skipped or adjusted (+/− 15 s). Duration edits persist back to the routine (see §4).
- **Set types in logging:** each set carries its type (warmup / standard / drop set / failure) with a small visual marker; standard sets show their auto-number. Type can be changed on the fly. Adding a drop set after a standard set is a one-tap action.
- **Next-time note:** when starting an exercise, its next-time note (if any) is shown prominently. Writing/updating one during the session takes one tap from the exercise view.
- Notes: free-text note per session and per exercise instance.
- Session summary on finish: duration, total volume, sets, any new PRs.

### 5.3 Exercise Library
- Seeded list of 50–100 common exercises, localized (EN/SV), tagged by muscle group and equipment.
- Search and filter by muscle group / equipment.
- Creating a custom exercise takes seconds and is reachable directly from the routine editor and the logging screen.
- Exercise detail view: history of all logged sets, best set, estimated 1RM trend.

### 5.4 Statistics
All charts filterable by time range (1 m / 3 m / 6 m / 1 y / all):

**Volume calculation rule** (used wherever volume appears, e.g. the session summary):
- `WEIGHT_KG`: `weight × reps`.
- `BODYWEIGHT`: `(bodyweight × factor + added weight) × reps`, where bodyweight is the **most recent body weight log** at or before the session date, falling back to **75 kg** if none exists. Assistance counts as negative added weight.
- `MACHINE_LEVEL`: excluded from kg-based volume (levels aren't kilograms); machine exercises still count in set-based stats like muscle group distribution.
- Warmup sets are excluded from PR detection, 1RM estimates, and progression logic, but shown in history.

- **Load over time per exercise** (line chart; e.g. top-set load per session). The y-axis follows the exercise's measurement type: kg, machine level, or (for bodyweight) reps of the best set / added kg where applicable.
- **Estimated 1RM over time per exercise** — use the Epley formula (`1RM = w × (1 + reps/30)`); cap the estimate for sets above ~12 reps to avoid nonsense values. Only shown for `WEIGHT_KG` exercises — it's meaningless for machine levels and plain bodyweight work.
- **Training frequency calendar heatmap** (GitHub-contribution-style, sessions per day).
- **Muscle group distribution** (sets per muscle group over the selected period, e.g. bar or donut chart).

### 5.5 Body Metrics
- Log body weight and optional measurements with date.
- Body weight line chart over time.

### 5.6 PR Detection & Notifications
- Detect PRs at set completion, adapted to measurement type: heaviest weight / best estimated 1RM / rep PR at a given weight (`WEIGHT_KG`), highest level and rep PR at a level (`MACHINE_LEVEL`), most reps or most added weight (`BODYWEIGHT`).
- Celebrate in-app immediately (subtle banner/snackbar, no confetti overload) and list PRs in the session summary.

### 5.7 Backup: Export / Import
- **Export:** full database to a single JSON file via the system file picker (Storage Access Framework). Include a schema version number in the file.
- **Import:** restore from that JSON file, with a clear warning that it replaces current data.
- Additionally: CSV export of the workout log (one row per set) for spreadsheet users.
- No automatic cloud backup.

### 5.8 JEFIT Import (important)
- Import training history from JEFIT's CSV export so the user keeps years of data.
- JEFIT's CSV format is undocumented and may vary between versions. Build the parser defensively:
  - Detect columns by header names, not fixed positions.
  - JEFIT typically stores set data as packed strings (e.g. `"weight x reps"` lists per exercise per day) — handle both packed and one-row-per-set layouts if encountered.
  - Convert lbs → kg if the export is in lbs (detect from data or ask the user during import).
  - Map JEFIT exercise names to built-in exercises where names match (case-insensitive, fuzzy on common variants); create custom exercises for the rest.
  - Show an import preview (X sessions, Y sets, Z new exercises) before committing, and a summary of anything skipped afterward.

### 5.9 Settings
The settings screen is a first-class feature — the app's guiding principle is configurability. At minimum:
- Language: follow system, or force English / Swedish.
- Display unit: kg (default) or lbs.
- Default rest duration for new sets.
- Timer sound/vibration toggles.
- Auto-finish inactivity threshold (default 2 h).
- Sequence behavior when swapping workouts (skipped stays next / advance).
- Default gym.
- Comeback suggestion threshold (days).
- Coach comments: on/off, personality, frequency (see §5.11).
- Progression suggestion increments (defaults).
- Export / import entry points.
- About screen: version, MIT license, link to the GitHub repo.

### 5.10 Gyms & Exercise Swapping
The goal: the same routine should be runnable regardless of which gym you're at.

- **Gyms:** the user creates gyms (e.g. "Campushallen", "Nordic Wellness downtown", "Home gym") and marks which exercises are available at each. One gym is the **default**. When starting a session, the default gym is pre-selected but changeable.
- **Session resolution:** when starting a routine day at a gym, every planned exercise is checked against the gym's available list:
  - Available → used as-is.
  - Not available, exactly one group-equivalent available → auto-swapped, clearly indicated.
  - Not available, several equivalents available → the user picks from the group (filtered to that gym).
  - No equivalent → keep original with a warning, skip, or pick any exercise manually.
- **Swap persistence:** a swap applies to the session only, but the picker offers "always use this exercise at this gym", which saves a **per-gym override** on the planned exercise. The routine itself stays gym-agnostic.
- **Temporary visit:** starting a session in "temporary visit" mode skips availability filtering entirely — every exercise instead presents its full equivalents group so the user picks whatever matches the equipment in front of them. Nothing is saved for future sessions.
- **Gym-aware history:** every session records its gym. For `MACHINE_LEVEL` exercises, pre-fill and progression suggestions use the last session **at the same gym** (machine levels aren't comparable across gyms). For `WEIGHT_KG` and `BODYWEIGHT`, history is global. Exercise stats can be filtered by gym; machine-level charts default to the current/default gym.
- **Manual swap anytime:** even mid-session, any exercise can be swapped to a group equivalent in two taps.

### 5.11 Coach Comments (optional, off by default)
Short, context-triggered encouragements shown as unobtrusive banners/snackbars during sessions. Entirely local and rule-based — pre-written localized string templates, no AI, no network.

- **Triggers** (examples, extendable): first session after a long break ("welcome back"); a set where hitting the target would mean a new PR ("hit target now and it's a PR"); long idle time since the previous completed set/exercise ("time to focus"); reaching the last exercise of the session ("last one — come on"); finishing a session; a streak of consistent weeks.
- **Personalities**, selectable in settings, each with its own template set in both languages: **Cheerleader** (enthusiastic), **Bro** (gym slang), **PT** (factual, professional), **Minimal** (dry one-liners).
- **Frequency** setting: low / medium / high — implemented as per-trigger cooldowns and a max-comments-per-session cap so it never gets pushy or cringe at low settings.
- Master toggle in settings; **off by default**. Never blocks input, never requires dismissal.

---

## 6. UI / UX

- **Dark theme by default** (light theme available as an option in settings).
- Material 3 components; clean, dense-but-readable layouts. This is a tool, not a social app — prioritize speed of logging over decoration.
- Big touch targets on the logging screen (usable with tired hands mid-workout).
- Keep the screen awake during an active workout session.
- All strings via string resources; provide complete `values/strings.xml` (English) and `values-sv/strings.xml` (Swedish). No hardcoded strings.
- Load input: numeric keyboard; steppers adapted to measurement type and display unit (+/− 2.5 kg or +/− 5 lbs for weights, +/− 1 for machine levels).

---

## 7. Open Source & Repository Setup

- **License:** MIT (include `LICENSE` file).
- **README.md:** description, screenshots section (placeholder), feature list, how to build, how to install the APK, JEFIT import instructions, contributing note.
- `CONTRIBUTING.md` (brief) and a couple of GitHub issue templates (bug report, feature request).
- **CI:** GitHub Actions workflow that builds the debug APK and runs unit tests on every push/PR.
- **Releases:** a workflow that builds a signed release APK on git tags and attaches it to a GitHub Release. Document the signing setup (keystore via repository secrets) in the README. Distribution is **GitHub Releases only** — no Play Store or F-Droid setup needed now, but don't do anything that would block F-Droid later (i.e. no proprietary dependencies).
- `.gitignore` appropriate for Android/Gradle; never commit keystores or local properties.

---

## 8. Quality Requirements

- Unit tests for: 1RM calculation, PR detection, rotating-schedule "next day" logic, JEFIT CSV parsing (with sample fixture files), and export/import round-trip.
- Room schema exported and migrations in place from version 1 onward.
- Handle process death gracefully during an active workout (session state must survive).
- No network permission in the manifest — the app must be provably offline.

---

## 9. Acceptance Checklist

Build is done when all of these are true:

- [ ] App builds via `./gradlew assembleDebug` and CI is green.
- [ ] I can create a routine with a superset, schedule it as rotating A/B, and the home screen shows the correct next day.
- [ ] I can log a workout where sets pre-fill according to the routine's pre-fill mode (all three modes verifiable).
- [ ] Target reps show next to each set, are editable from the logging screen, and the edit persists to the routine.
- [ ] Set types work: warmups are excluded from PRs/1RM/progression, standard sets auto-number correctly, drop sets add in one tap, failure sets have no target.
- [ ] A next-time note written during a session appears the next time that exercise comes up.
- [ ] Routines show "last performed X days ago"; after the configured idle period, `rebuild`-tagged routines are suggested.
- [ ] Starting a routine at a gym missing an exercise offers group equivalents; "always use at this gym" persists; temporary-visit mode offers the full group per exercise.
- [ ] Machine-level pre-fill is per-gym (level 7 at gym A doesn't leak to gym B).
- [ ] Coach comments are off by default; enabling them with a personality shows localized, trigger-based messages respecting the frequency setting.
- [ ] Hitting target reps on all sets triggers a load-increase suggestion next session; accepting it updates the pre-fill.
- [ ] Measurement type per exercise works: a kg exercise, a machine-level exercise, and a bodyweight exercise (with added weight) all log and chart correctly.
- [ ] Session volume for a bodyweight exercise uses `bodyweight × factor + added weight`, pulls the latest logged body weight, and falls back to 75 kg when none is logged.
- [ ] Rest timer per set works, `0` disables it, and an adjusted duration is remembered next iteration.
- [ ] PR banner appears when I beat a previous best.
- [ ] All four statistics views render with real logged data; switching display unit to lbs converts everywhere while data stays kg internally.
- [ ] I can set a program as active; on app open after a finished workout, the next workout in order is proposed, awaits confirmation, and can be swapped.
- [ ] A session idle past the configured threshold (default 2 h) auto-finishes with end time = last activity.
- [ ] Programs and workouts can have icons (from the built-in set) and descriptions; comments exist at program, workout, and exercise level.
- [ ] Body weight logging and chart work.
- [ ] Export → wipe → import restores everything identically.
- [ ] A sample JEFIT CSV imports with a correct preview and history visible in stats.
- [ ] Switching language to Swedish translates the entire UI.
- [ ] Dark theme is the default on first launch.
- [ ] No network permission, no analytics dependencies.
- [ ] README, LICENSE (MIT), CONTRIBUTING, and release workflow exist.
