# BakaMitai

Android app, being rewritten from scratch. The repo holds a fresh Jetpack Compose scaffold; the previous codebase is in git history and on the `master`, `clean-architecture` and `compose-migration` branches.

## Project

- Single Gradle module `:app`, Kotlin only, Jetpack Compose + Material 3.
- Root package `com.zc.bakamitai` (`app/src/main/java/com/zc/bakamitai/`).
- `minSdk` 27, `compileSdk` / `targetSdk` 37. AGP 9.5.1 (built-in Kotlin), Gradle 9.6.0, Kotlin 2.4.20, Compose BOM 2026.09.00.
- Every dependency and plugin is declared in `gradle/libs.versions.toml`.
- Two product flavors (dimension `environment`, declared in `app/build.gradle.kts`): `development` (application id `com.zc.bakamitai.dev`, version name `-dev`, app name "BakaMitai Dev" from `app/src/development/res/values/strings.xml`, also shown in the top bar) and `production` (`com.zc.bakamitai`, "BakaMitai"). Day-to-day work uses `developmentDebug`. `developmentRelease` is signed with the repo's `debug.keystore`; `productionRelease` is unsigned until a release key is set up.
- `.github/workflows/build-release-apk.yml` (manual run) builds `assembleDevelopmentRelease` and publishes the APK as a GitHub release.
- Work happens on `develop`; `master` is the default branch for PRs.

## Commands

```bash
./gradlew assembleDevelopmentDebug
```

```bash
./gradlew testDevelopmentDebugUnitTest
```

```bash
./gradlew connectedDevelopmentDebugAndroidTest
```

```bash
android run --apks=app/build/outputs/apk/development/debug/app-development-debug.apk
```

- `connectedDevelopmentDebugAndroidTest` needs a running emulator or device, and uninstalls the app when it finishes.
- `android run` installs and launches the debug APK on the connected device.
- `./gradlew detekt` must pass with zero findings; `./gradlew detekt --auto-correct` fixes ktlint formatting.

## Skills

Project rules live in `.claude/skills/`. Load the relevant ones before writing code, and all six when generating a feature.

| Skill | Covers |
|---|---|
| `structure` | Package tree and where every file goes |
| `architecture` | Clean Architecture layers, dependency rule, MVI data flow |
| `conventions` | Naming, immutability, mappers, MVI contract, Route / Screen / View |
| `tech-stack` | Approved libraries and which layer may use each |
| `library-management` | Version catalog, `build-logic/` convention plugins, custom Gradle tasks |
| `best-practices` | Compose performance, Detekt, coroutines, error handling, previews, testing |

Agents that don't load Claude Code skills should read `.claude/skills/<name>/SKILL.md` directly.

Rules marked **MUST** / **NEVER** are non-negotiable. When code and a skill disagree, follow the skill and fix the code; when the rule itself should change, update the skill first.

## Current state

- Foundation in place: every approved library is in the catalog and wired into `:app`; `build-logic/` has the `bakamitai.android.application`, `bakamitai.android.room`, `bakamitai.detekt` and `bakamitai.kotlin.only` convention plugins; Koin runs through the Koin compiler plugin (`startKoin<AppKoin>`). **KSP is used for Room only** (Koin keeps its compiler plugin); the Room schema is exported to `app/schemas/`.
- Theme: `common/ui/theme/` implements `docs/DESIGN.md` ("Obsidian Signal"), with bundled Plus Jakarta Sans / Space Grotesk fonts. DESIGN.md defines dark only; the light scheme is derived from the same tonal palettes (see `Color.kt`). Extra tokens: `BakaMitaiTheme.extendedColors`, `BakaMitaiTheme.spacing`.
- Shared code in `common/`: `UiText`, `ResourceTypes.kt` (incl. `PluralResource`), `ObserveAsEvents`, `@AdaptivePreviews`, `ErrorView`, the skeleton pieces (`SkeletonView`, `SkeletonBlock`, `SkeletonTextLine`), `Navigator` / `NavigationState`, `DomainError`, `DispatcherProvider`, `TimeZoneProvider`, `safeApiCall`, the SubsPlease `HttpClient`, `CommonModule` (also provides a `kotlin.time.Clock`). Moved from Home by the Schedule plan: `Show` / `ShowSlug` / `ScheduledShow` and `GetTodayDateUseCase` (`common/domain/`), `ScheduleItemDTO` / `TodayScheduleDTO` and their mappers plus image-URL mapping (`common/data/`), the time and UTC-offset label mappers (`common/ui/mapper/`), `CoverImageView` and `TopLevelAppBarView` (`common/ui/component/`). Added by All Shows: `EmptyStateView` (`common/ui/component/emptystate/`, also used by Schedule). Added by Details: the `release_date` parser (`common/data/mapper/`) and `ContentWidth.kt` (`common/ui/util/`). Added or moved by Bookmarks: `TimeChipView` (`common/ui/component/timechip/`), the date, episode-label and day-name mappers (`common/ui/mapper/`), `ShowDeepLink.kt` (`common/ui/navigation/`, the notification → Details intent) and `NotificationPermission.kt` (`common/ui/util/`). Added by Settings: `AppSettings` / `SettingsRepository` / the `settings` Preferences DataStore (`common/domain`, `common/data/local`), `UiText.TimeValue` + `LocalUse24HourClock` (`common/ui/text/`), `safeDataStoreCall` and `OpenLink.kt`.
- Navigation, on two levels kept apart (`architecture` §Navigation), in `app/navigation/`, hosted by `app/AppRoute.kt`: `AppNavigation` draws the **app back stack**, with `MainNavKey` (the tab shell, `MainShell`: `NavigationSuiteScaffold` around the tabs' own `NavDisplay`) at the bottom and every pushed screen (Details, Settings) above it, covering the bar or rail. The bar is never hidden or shown. `AppEntryProvider` registers the tabs in `tabEntryProvider` and pushed screens in `appEntryProvider`. All four tabs are enabled, and the gear on each tab opens **Settings**. The app root (`AppViewModel`/`AppState`/`AppRoute`, with `MainActivity` holding the first draw) applies the theme before the first frame and sets the system bars' icon colors.
- Features: **Home** (`feature/home/`, plan [docs/1-home.md](docs/1-home.md)), **Schedule** (`feature/schedule/`, plan [docs/2-schedule.md](docs/2-schedule.md)), **All Shows** (`feature/shows/`, plan [docs/3-shows.md](docs/3-shows.md)) **Details** (`feature/details/`, plan [docs/4-details.md](docs/4-details.md)), the first pushed screen, which every card and row opens, and **Bookmarks** (`feature/bookmarks/`, plan [docs/5-bookmarks.md](docs/5-bookmarks.md)): a Room-backed grid plus **new-episode alerts** (one `ReleaseCheckWorker` on WorkManager, injected with `@KoinWorker`; local notifications that open Details; `POST_NOTIFICATIONS` asked on the first bookmark). **Settings** (`feature/settings/`, plan [docs/6-settings.md](docs/6-settings.md)): first day of week, time format (every time is a `UiText.TimeValue`, formatted when drawn), theme, the new-episode alerts switch, About and the version footer; Bookmarks' sort is saved too. **Backup and restore** (in `feature/settings/`, a second screen pushed from Settings' DATA row, plan [docs/7-backup.md](docs/7-backup.md)): a backup folder picked through the Storage Access Framework (persisted tree permission), manual create and restore of a JSON file (bookmarks, settings, sort; restore merges bookmarks and tracks alerts from now), automatic backups as unique periodic work (`BackupWorker`, `automatic-backup`) keeping the newest N files.
- Loading states are shimmering skeletons that mirror the content, never spinners (`best-practices` §5.3).
- Feature plans live in `docs/<n>-<feature>.md` (written by the `planner` agent, built by `implementor`, checked by `tester`). Icons come from `material-icons-extended`. HTML scraping uses **Ksoup** (`data` layer only, one `[Name]HtmlParser` per page).
