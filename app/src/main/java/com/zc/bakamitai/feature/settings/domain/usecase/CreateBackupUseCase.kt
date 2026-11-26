package com.zc.bakamitai.feature.settings.domain.usecase

import com.zc.bakamitai.common.domain.repository.SettingsRepository
import com.zc.bakamitai.feature.bookmarks.domain.model.Bookmark
import com.zc.bakamitai.feature.bookmarks.domain.repository.BookmarkRepository
import com.zc.bakamitai.feature.bookmarks.domain.repository.BookmarkSortRepository
import com.zc.bakamitai.feature.settings.domain.model.Backup
import com.zc.bakamitai.feature.settings.domain.model.BackupError
import com.zc.bakamitai.feature.settings.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.domain.repository.AppInfoRepository
import com.zc.bakamitai.feature.settings.domain.repository.BackupFileRepository
import com.zc.bakamitai.feature.settings.domain.repository.BackupPreferencesRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import kotlin.time.Clock

/**
 * Writes a backup of the settings, the Bookmarks sort and every bookmark into the backup folder, records when, and
 * deletes the oldest backups past *Backups to keep* (docs/7-backup.md). Used by the screen and by the automatic
 * backups. Returns the folder written to; without one it fails with [BackupError.NoFolder].
 *
 * Once the file is written the backup has succeeded: a failure to record its time or to delete old files is
 * ignored (the next backup tries again).
 */
@Factory
class CreateBackupUseCase(
    private val preferences: BackupPreferencesRepository,
    private val files: BackupFileRepository,
    private val settings: SettingsRepository,
    private val bookmarkSort: BookmarkSortRepository,
    private val bookmarks: BookmarkRepository,
    private val appInfo: AppInfoRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(): Result<BackupFolder> {
        val current = preferences.observePreferences().first()
        val folder = current.folder ?: return Result.failure(BackupError.NoFolder)
        return bookmarks.getBookmarks().fold(
            onSuccess = { saved -> write(folder, keep = current.maxBackups, saved = saved) },
            onFailure = { Result.failure(it) },
        )
    }

    private suspend fun write(folder: BackupFolder, keep: Int, saved: List<Bookmark>): Result<BackupFolder> {
        val now = clock.now()
        val backup = Backup(
            createdAt = now,
            appVersion = appInfo.getAppInfo().versionName,
            settings = settings.observeSettings().first(),
            bookmarkSort = bookmarkSort.observeSort().first(),
            bookmarks = saved.map { it.copy(airing = null) },
        )
        return files.write(folder, backup).map {
            preferences.setLastBackupAt(now)
            files.deleteOldBackups(folder, keep)
            folder
        }
    }
}
