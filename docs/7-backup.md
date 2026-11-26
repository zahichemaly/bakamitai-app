# 7. Backup and restore

> **Status:** Implemented (2026-11-26; built, unit-tested and Detekt-checked locally, not checked on a device or emulator: see [§10](#10-implementation-notes)) · **Design:** none (no mock; built from the Settings screen's own components) · **Planned:** 2026-11-26
> **Spec:** [context.md §3.6](context.md#36-settings), [§5](context.md#5-local-persistence) · **API:** none (local only)
> **Depends on:** [5-bookmarks.md](5-bookmarks.md) (Room bookmarks, `ReleaseCheckScheduler.requestCheck()`, the sort store), [6-settings.md](6-settings.md) (`AppSettings`, `SettingsRepository`, the `settings` DataStore, `SettingsSectionView`, `PickerPreferenceView`, `PickerDialogView`).
> **Reference:** Mihon's *Settings → Data and storage*: a backup folder chosen once through the Storage Access Framework (a persisted tree permission), *Create backup* and *Restore backup* actions, and automatic backups on an interval through WorkManager that keep the newest N files.

---

## 1. Summary

A **Backup and restore** screen, pushed from a new **DATA** section in Settings, saves the user's bookmarks and settings to a JSON file in a folder they choose, and adds them back from such a file.

| Row | Effect |
|---|---|
| **Backup folder** | Opens the system folder picker (`ACTION_OPEN_DOCUMENT_TREE`). The app keeps read/write access to the folder across restarts (`takePersistableUriPermission`) and releases the previous folder's. The row shows the folder's name, or "Not set". |
| **Create backup** | Writes `bakamitai_<yyyy-MM-dd_HH-mm-ss>.json` into the folder, then deletes the oldest BakaMitai backups past *Backups to keep*. Without a folder it opens the folder picker first, then creates the backup. The row's second line is "Last backup: Nov 26, 2026, 10:47 AM" or "No backups yet". |
| **Restore backup** | Opens the system file picker (`ACTION_OPEN_DOCUMENT`, any type), then a confirmation dialog, then restores ([§4.3](#43-restore-rules)). A snackbar reports "Restored 12 bookmarks." |
| **Automatic backups** | Off (default), Every 6 hours, Every 12 hours, Daily, Weekly. A periodic WorkManager job runs *Create backup*. Choosing an interval without a folder opens the folder picker. |
| **Backups to keep** | 1, 2, 3 (default), 5, 10: how many BakaMitai backup files stay in the folder. |

**In scope:** the screen and its Settings entry; the backup file format; SAF access; the periodic worker; the restore rules.
**Out of scope:** cloud sync, encryption, backing up the release-alert tracking state, restoring the backup preferences themselves (they're device-specific: a folder permission doesn't travel), Android Auto Backup rule changes.

## 2. Screen

Same chrome as Settings ([6-settings.md §2.0–2.2](6-settings.md#20-token-mapping)): `TopAppBar` with back arrow and "Backup and restore", one scrolling column, centered at `MaxContentWidth` on wide windows. Sections are `SettingsSectionView`s; rows are `PickerPreferenceView`s (icon, title, value line, one button for TalkBack).

| Section (accent) | Rows (icon) |
|---|---|
| **BACKUP FOLDER** (`primary`) | Backup folder (`Outlined.Folder`): folder name / "Not set" |
| **MANUAL** (`secondary`) | Create backup (`Outlined.Backup`): last backup / "Creating backup…" · Restore backup (`Outlined.SettingsBackupRestore`): "Add bookmarks and settings from a backup file" / "Restoring…" |
| **AUTOMATIC** (`tertiary`) | Automatic backups (`Outlined.Schedule`): the interval · Backups to keep (`Outlined.Inventory2`): the count |

- **Settings entry:** a **DATA** section (`secondary`) between Notifications and About, with one row: *Backup and restore* (`Outlined.Backup`), "Create, restore and automatic backups". A tap sends `BackupClicked` → `OpenBackup` → the Route's `onOpenBackup`.
- **Loading:** until the backup preferences' first emission, each card shows `PickerPreferenceSkeletonView`s in its own `SkeletonView` (as Settings).
- **Busy:** while a backup or a restore runs, its row's value line says so and taps on Create / Restore are ignored. No spinner (`best-practices` §5.3).
- **Dialogs:** the two pickers reuse `PickerDialogView` (`BackupState.openPicker`, survives rotation). The restore confirmation is a new `RestoreDialogView` (`AlertDialog`): "Restore this backup?", "Its bookmarks are added to yours (the ones you have stay) and its settings replace your current ones.", **Restore** / **Cancel**. The chosen file's URI is kept in `BackupState.pendingRestore`, so the dialog survives rotation.
- **Messages (snackbars):** "Backup saved to {folder}." · "Restored {n} bookmark(s)." (plural; "No new bookmarks; settings restored." for 0) · errors ([§4.4](#44-errors)).

## 3. Design vs. spec

There's no mock. Every visual choice reuses Settings' components, so the screen matches it in both themes and every width. The spec (context.md §3.6) gains the DATA row and the screen.

## 4. Data

### 4.1 Backup file (format version 1)

JSON through the shared `appJson` (`ignoreUnknownKeys`), so later versions can add fields; a file with a **higher** `version` is refused.

```json
{
  "version": 1,
  "created_at": 1795600000000,
  "app_version": "2.0.0",
  "settings": {
    "week_start": "MONDAY",
    "time_format": "TWELVE_HOUR",
    "theme_mode": "SYSTEM",
    "new_episode_alerts": true,
    "bookmarks_sort": "RECENTLY_ADDED"
  },
  "bookmarks": [
    { "slug": "one-piece", "title": "One Piece", "cover_url": "https://subsplease.org/…", "added_at": 1795500000000 }
  ]
}
```

- Enums are stored by name, as in the DataStore; an unknown or missing name restores as the default.
- Only what the user made is saved: a bookmark's slug, title, cover and date. The weekly slot and the alert tracking are re-derived by the next release check.
- DTOs: `BackupDTO`, `BackupSettingsDTO`, `BackupBookmarkDTO` (`feature/settings/backup/data/dto/`).

### 4.2 Backup preferences

In the shared `settings` Preferences DataStore (`BackupPreferencesLocalDataSource`, `feature/settings/backup/data/local/`):

| Key | Type | Default |
|---|---|---|
| `backup_folder_uri` | String (tree URI) | none |
| `backup_folder_name` | String (display name read when chosen) | none |
| `backup_frequency` | String (`BackupFrequency` name) | `OFF` |
| `backup_max_count` | Int | 3 |
| `backup_last_at` | Long (epoch ms) | none |

Android Auto Backup may bring these keys to a new device without the folder permission: the next write fails with *folder unavailable* and asks the user to choose the folder again.

### 4.3 Restore rules

1. Read and parse the file (`version` ≤ 1, otherwise *unsupported*).
2. Settings: each of the five values is written (`SettingsRepository` setters, `BookmarkSortRepository.setSort`). The theme and clock apply at once through the app root.
3. Bookmarks: **merged**: every backed-up slug not saved yet is inserted (`INSERT … OR IGNORE`, one transaction) with its original `added_at`, tracking new episodes **from now** (as Undo does), so a restore never sends a burst of alerts for old episodes. Bookmarks already saved are left untouched.
4. If any bookmark was added: `ReleaseCheckScheduler.requestCheck()` (refreshes their slots).

### 4.4 Errors

`BackupError` (`feature/settings/backup/domain/model/`), mapped to text by `BackupErrorUIModelMapper`:

| Error | When | Snackbar |
|---|---|---|
| `NoFolder` | a backup without a folder (the worker only; the screen opens the picker) | "Choose a backup folder first." |
| `FolderUnavailable` | the permission is gone, the folder was deleted, the provider refused | "Can't write to the backup folder. Choose it again." |
| `InvalidFile` | unreadable, not JSON, not a BakaMitai backup | "This file isn't a BakaMitai backup." |
| `UnsupportedVersion` | `version` > 1 | "This backup is from a newer version of BakaMitai." |
| anything else (`DomainError.Unknown`) | a failed DataStore or Room write | the generic error |

## 5. Architecture

All in `feature/settings/backup/`, a **sub-feature** of Settings (`structure` §1.2): only Settings opens it, and its screen reuses Settings' components. It has no `di/` (Settings' module scan registers it), and Settings never imports it.

| Layer | Files |
|---|---|
| `domain/model` | `Backup` (`createdAt`, `appVersion`, `settings: AppSettings`, `bookmarkSort`, `bookmarks: List<Bookmark>`), `BackupFrequency` (with `interval: Duration?`), `BackupFolder` (`uri`, `name`), `BackupPreferences`, `RestoreSummary`, `BackupError` |
| `domain/repository` | `BackupPreferencesRepository`, `BackupFileRepository` (grant a folder, write, read, prune), `BackupScheduler` (port) |
| `domain/usecase` | `ObserveBackupPreferencesUseCase`, `SetBackupFolderUseCase`, `UpdateBackupFrequencyUseCase` (saves, then reschedules), `UpdateMaxBackupsUseCase`, `CreateBackupUseCase`, `RestoreBackupUseCase` |
| `data/dto`, `data/mapper` | the three DTOs; `BackupDomainMapper` (`Backup.toDTO()`), `BackupDTOMapper` (`BackupDTO.toDomain()`) |
| `data/local` | `BackupPreferencesLocalDataSource` (DataStore), `BackupDocumentsLocalDataSource` (SAF: `ContentResolver` + `DocumentsContract`, on `DispatcherProvider.io`) |
| `data/repository` | `BackupPreferencesRepositoryImpl`, `BackupFileRepositoryImpl` (JSON, file naming, error mapping) |
| `data/worker` | `BackupWorker` (`@KoinWorker`, calls `CreateBackupUseCase`), `BackupSchedulerImpl` (unique periodic work `automatic-backup`, `UPDATE` policy; `OFF` cancels it) |
| `presentation` | `BackupState` / `BackupIntent` / `BackupEffect` / `BackupViewModel` / `BackupRoute` / `BackupScreen`; `model/BackupPicker`, `model/BackupFrequencyOption`; `mapper/BackupUIModelMapper` (pickers, folder and last-backup labels), `mapper/BackupErrorUIModelMapper`; `component/restoredialog/RestoreDialogView` |
| `presentation/navigation` | `BackupNavKey`, `Navigator.navigateToBackup()`, `backupEntry(onNavigateBack)` |

Changes elsewhere:
- `feature/bookmarks`: `BookmarkRepository.getBookmarks()` and `restoreAll(bookmarks, trackingSince): Result<Int>`; `BookmarkDao.insertAll` (`IGNORE`, returns row ids).
- `feature/settings`: Settings' DATA section, `SettingsIntent.BackupClicked`, `SettingsEffect.OpenBackup`, `settingsEntry(onNavigateBack, onOpenBackup)`.
- `app/navigation/AppEntryProvider.kt`: `backupEntry`, `onOpenBackup = navigator::navigateToBackup`.
- `tech-stack` skill: WorkManager allows **unique periodic work for local-only jobs** (the automatic backup); API checks stay one-time chains.

### 5.1 MVI contract

```kotlin
@Immutable
data class BackupState(
    val isLoading: Boolean = true,
    val folderLabel: UiText? = null,          // name or "Not set"
    val lastBackupLabel: UiText? = null,      // "Last backup: …" or "No backups yet"
    val frequency: PickerUIState<BackupFrequencyOption>? = null,
    val maxBackups: PickerUIState<Int>? = null,
    val isCreating: Boolean = false,
    val isRestoring: Boolean = false,
    val openPicker: BackupPicker? = null,
    val pendingRestore: String? = null,       // the chosen file's URI, awaiting confirmation
)

sealed interface BackupIntent {
    data object BackClicked; data object FolderClicked; data class FolderChosen(val uri: String?)
    data object CreateClicked; data object RestoreClicked; data class RestoreFileChosen(val uri: String?)
    data object RestoreConfirmed; data object RestoreDismissed
    data class PickerClicked(val picker: BackupPicker); data object PickerDismissed
    data class FrequencySelected(val option: BackupFrequencyOption); data class MaxBackupsSelected(val count: Int)
}

sealed interface BackupEffect {
    data object NavigateBack; data object OpenFolderPicker; data object OpenFilePicker
    data class ShowMessage(val message: UiText)
}
```

Reducer: `combine(_state, observeBackupPreferences())`; a create or a restore sets its busy flag, runs the use case, clears the flag and sends `ShowMessage`. `CreateClicked` with no folder (or a frequency other than Off with no folder) sends `OpenFolderPicker` and remembers to create once a folder is chosen.

## 6. Dependencies

None new: SAF is in the platform and `activity-compose` (`ActivityResultContracts.OpenDocumentTree` / `OpenDocument`); JSON is kotlinx.serialization; WorkManager, DataStore and Room are in place.

## 7. Testing

- **Unit:** `BackupDTOMapperTest` / `BackupDomainMapperTest` (round trip, unknown enums, blank slugs dropped), `CreateBackupUseCaseTest` (no folder, gathers everything, prunes, records the time), `RestoreBackupUseCaseTest` (settings written, bookmarks merged from now, check requested only when something was added, errors propagate), `UpdateBackupFrequencyUseCaseTest` (reschedules on success only), `SetBackupFolderUseCaseTest`, `BackupPreferencesLocalDataSourceTest`, `BackupViewModelTest`, `BackupUIModelMapperTest`, `SettingsViewModelTest` (BackupClicked), `NavKeySerializationTest` (+`BackupNavKey`), `KoinModulesTest`.
- **Instrumented (not run here, need an emulator):** none added; the SAF data source and the worker are thin platform wiring.

## 8. Implementation steps

1. [x] Plan (this file).
2. [x] Bookmarks: `getBookmarks`, `restoreAll`, `insertAll`, fakes and tests.
3. [x] Domain: models, ports, use cases, fakes, tests.
4. [x] Data: DTOs, mappers, the two local sources, repositories, worker, scheduler, tests.
5. [x] Presentation: the Backup screen, the Settings DATA row, navigation, strings, tests.
6. [x] Docs: context.md §3.6 / §5, CLAUDE.md, the `tech-stack` WorkManager line, `structure`'s current state.
7. [x] Verify: `assembleDevelopmentDebug`, `testDevelopmentDebugUnitTest`, `detekt`.

## 9. Acceptance criteria

- Choosing a folder survives an app restart; the row shows its name.
- *Create backup* writes a valid file and the folder never holds more BakaMitai backups than *Backups to keep*.
- *Restore backup* on a fresh install brings back every bookmark (with its original order under "Recently added") and every setting, and sends no alert for old episodes.
- Restoring twice adds nothing the second time.
- *Automatic backups* set to Daily creates a backup about once a day without opening the app; Off stops it.
- A revoked or deleted folder gives the "choose it again" message, never a crash.

## 10. Implementation notes

- Prune applies after every backup (manual or automatic) and only touches files matching `bakamitai_YYYY-MM-DD_HH-mm-ss.json`, newest kept by name.
- Not verified on a device: the folder and file pickers, persisted permissions after a reboot, and the periodic worker's timing.
