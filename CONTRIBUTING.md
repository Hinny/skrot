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
3. `./gradlew test` must pass; add tests for new logic in `domain/` or `data/backup/`.
4. Open a PR with a short description of what and why.

## Project layout

- `data/model` — Room entities and enums
- `data/db` — DAOs, database, seed catalog
- `data/backup` — JSON backup, CSV export, JEFIT import
- `data/prefs` — settings (DataStore)
- `domain` — pure logic (1RM, PRs, progression, scheduling, prefill, gym resolution)
- `ui` — Compose screens, one package per feature
- `timer` — rest timer + notifications
