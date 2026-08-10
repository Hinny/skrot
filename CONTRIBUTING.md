# Contributing to Skrot

Thanks for wanting to help!

## Ground rules

- Skrot is **offline by design**: no network permission, no analytics, no accounts.
  PRs adding any of those will be declined.
- Keep it a tool: logging speed beats visual flourish.
- All user-facing strings go through string resources, in **both** `values/strings.xml`
  (English) and `values-sv/strings.xml` (Swedish). If you don't speak Swedish, mark the
  translation as needing review in the PR description.

## Workflow

1. Open an issue first for anything non-trivial.
2. Fork, branch, code. Match the existing style (Kotlin, Compose, MVVM).
3. `./gradlew verify` must pass; add tests for new logic in `domain/` or `data/backup/`.
4. Open a PR with a short description of what and why.

## Checks

```
./gradlew verify
```

That is ktlint, Android Lint, the unit tests and a debug APK — and it is the
only thing CI runs, so a green local run means a green CI run. Individual
pieces if you want a faster loop:

```
./gradlew test          # unit tests only
./gradlew ktlintCheck   # unused/wildcard imports
./gradlew lintDebug     # Android Lint
```

ktlint is configured as a targeted gate rather than a formatter: only the
import rules are on (see `.editorconfig`), so it will not reformat your code.

Requires **JDK 17**; the Android Gradle Plugin will not run on 8 or 11. If
`java -version` shows something older, point Gradle at a 17 install without
changing your system default:

```
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot"
# macOS / Linux
export JAVA_HOME=$(/usr/libexec/java_home -v17)   # or your distro's path
```

Open a **draft** PR while work is in progress: CI skips drafts and starts when
you mark it ready for review.

## Project layout

- `data/model` — Room entities and enums
- `data/db` — DAOs, database, seed catalog
- `data/backup` — JSON backup, CSV export, JEFIT import
- `data/prefs` — settings (DataStore)
- `domain` — pure logic (1RM, PRs, progression, scheduling, prefill, gym resolution)
- `ui` — Compose screens, one package per feature
- `timer` — rest timer + notifications
