# Skrot

**Skrot** (Swedish for "scrap metal" and is used in gym slang for weigh lifting) is a free, open-source Android strength
training log. Built to replace payed workout apps for people who want a fast, private,
no-nonsense logger — no ads, no accounts, no cloud, no tracking. 
The app has **no network permission**,
so it is provably offline.

## Screenshots

*(coming soon)*

## Features

- **Fast workout logging** — sets pre-fill from your last session, routine targets, or a
  hybrid of both (per program); most sets are a single tap to confirm.
- **Programs with named days**, supersets/circuits, drag-to-reorder, program/day icons and
  descriptions, free-form tags.
- **Two scheduling modes**: fixed weekdays (Mon = Legs…) or rotating (A → B → C → A…).
  The home screen proposes the next workout and always waits for your confirmation —
  swapping in another day is one tap and configurable in how it affects the sequence.
- **Three measurement types per exercise**: weight (kg/lbs), unit-less machine levels, and
  bodyweight (reps with optional added weight or assistance, plus a per-exercise
  bodyweight factor for volume).
- **Set types**: warmup, standard (auto-numbered), drop set, failure (AMRAP). Warmups are
  excluded from PRs, 1RM estimates, and progression.
- **Rest timer with memory** — per-set durations stored in the routine; adjust during a
  workout and the routine remembers next time. `0` = no timer. Countdown in-app and as a
  notification with optional sound/vibration.
- **Progression suggestions** — hit your target reps on all sets and next session suggests
  a load increase (increments configurable globally and per exercise).
- **PR detection** adapted to measurement type (heaviest weight, best estimated 1RM, rep
  PRs, highest level, most reps / most added weight), celebrated with a subtle banner.
- **Gyms & exercise swapping** — mark what's available at each gym; the same routine runs
  anywhere via interchangeable-exercise groups, with per-gym overrides
  ("always use this here") and a temporary-visit mode. Machine-level history is per-gym.
- **Statistics** — load and estimated 1RM over time (Epley, capped at 12 reps), a
  GitHub-style training calendar, sets per muscle group; all filterable by time range.
- **Body metrics** — weight and measurements with a trend chart.
- **Coach comments** (off by default) — local, rule-based encouragements in four
  personalities (Cheerleader, Bro, PT, Minimal) with a frequency setting.
- **Backup** — full JSON export/import via the system file picker, CSV export of the log,
  and **JEFIT CSV import** with preview.
- **Localized** in English and Swedish; dark theme by default.
- **Configurable everywhere** — thresholds, defaults, and behaviors live in Settings.

## Building

Requirements: JDK 17 and the Android SDK (platform 35).

```bash
git clone https://github.com/Hinny/skrot.git
cd skrot
./gradlew assembleDebug     # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew test              # runs the unit tests
```

Open in Android Studio (Ladybug or newer) for development.

## Installing the APK

1. Download the latest APK from [GitHub Releases](https://github.com/Hinny/skrot/releases)
   (or build it yourself).
2. On the phone, allow installs from your browser/file manager when prompted
   ("Install unknown apps").
3. Open the APK and install. Updates install over the old version — your data is kept.

## Importing from JEFIT

1. In JEFIT, export your training log as CSV (Profile → Settings → Export data,
   or via my.jefit.com).
2. In Skrot: **More → Backup & import → Pick JEFIT CSV file**.
3. If the file doesn't say which unit it uses, Skrot asks (kg or lbs — lbs are converted
   to kg on import).
4. Review the preview (sessions, sets, new exercises) and confirm. Exercise names that
   match Skrot's library (case-insensitive, tolerant of common variants) are linked to it;
   the rest are created as custom exercises. Anything skipped is listed afterwards.

The parser detects columns by header name and handles both packed
(`"60x8,60x8"` per row) and one-row-per-set layouts, since JEFIT's format varies
between versions.

## Releases & signing

Tagging `v*` triggers the release workflow, which builds a signed release APK and attaches
it to a GitHub Release. Configure these repository secrets:

| Secret | Content |
| --- | --- |
| `SKROT_KEYSTORE_BASE64` | Your keystore file, base64-encoded (`base64 -w0 skrot.jks`) |
| `SKROT_KEYSTORE_PASSWORD` | Keystore password |
| `SKROT_KEY_ALIAS` | Key alias |
| `SKROT_KEY_PASSWORD` | Key password |

Create a keystore once with:

```bash
keytool -genkeypair -v -keystore skrot.jks -keyalias skrot \
  -keyalg RSA -keysize 4096 -validity 10000
```

Never commit the keystore; `.gitignore` already excludes `*.jks`/`*.keystore`.
Distribution is GitHub Releases only for now. Nothing in the app blocks a future
F-Droid listing (no proprietary dependencies).

## Design notes / deviations from the plan

- **Manual dependency injection** instead of Hilt: a single `AppContainer` covers this
  single-module app; it keeps the build simpler and faster.
- **Custom Compose Canvas charts** instead of a chart library: the app needs three small
  chart types (line, horizontal bars, calendar heatmap); ~200 lines of Canvas code avoids
  a heavyweight dependency and keeps F-Droid compatibility trivially clean.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Bug reports and feature requests are welcome via
the issue templates.

## License

[MIT](LICENSE)
