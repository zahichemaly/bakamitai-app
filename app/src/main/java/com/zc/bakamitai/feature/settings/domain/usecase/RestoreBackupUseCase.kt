package com.zc.bakamitai.feature.settings.domain.usecase

import com.zc.bakamitai.common.domain.repository.SettingsRepository
import com.zc.bakamitai.feature.bookmarks.domain.repository.BookmarkRepository
import com.zc.bakamitai.feature.bookmarks.domain.repository.BookmarkSortRepository
import com.zc.bakamitai.feature.bookmarks.domain.repository.ReleaseCheckScheduler
import com.zc.bakamitai.feature.settings.domain.model.Backup
import com.zc.bakamitai.feature.settings.domain.model.RestoreSummary
import com.zc.bakamitai.feature.settings.domain.repository.BackupFileRepository
import org.koin.core.annotation.Factory
import kotlin.time.Clock

/**
 * Restores the backup file [uri] (docs/7-backup.md §4.3): its bookmarks are merged into the saved ones (new slugs
 * only, with their original dates, tracking new episodes from now so nothing old notifies), then its settings and
 * Bookmarks sort replace the current ones. A release check is requested when bookmarks were added.
 */
@Factory
class RestoreBackupUseCase(
    private val files: BackupFileRepository,
    private val settings: SettingsRepository,
    private val bookmarkSort: BookmarkSortRepository,
    private val bookmarks: BookmarkRepository,
    private val scheduler: ReleaseCheckScheduler,
    private val clock: Clock,
) {

    suspend operator fun invoke(uri: String): Result<RestoreSummary> = files.read(uri).fold(
        onSuccess = { backup -> restore(backup) },
        onFailure = { Result.failure(it) },
    )

    private suspend fun restore(backup: Backup): Result<RestoreSummary> =
        bookmarks.restoreAll(backup.bookmarks, trackingSince = clock.now())
            .onSuccess { added -> if (added > 0) scheduler.requestCheck() }
            .fold(
                onSuccess = { added ->
                    restoreSettings(backup).map { RestoreSummary(added, bookmarksInBackup = backup.bookmarks.size) }
                },
                onFailure = { Result.failure(it) },
            )

    /** Writes every setting, stopping at the first failure. */
    private suspend fun restoreSettings(backup: Backup): Result<Unit> {
        val restored = backup.settings
        val writes: List<suspend () -> Result<Unit>> = listOf(
            { settings.setWeekStart(restored.weekStart) },
            { settings.setTimeFormat(restored.timeFormat) },
            { settings.setThemeMode(restored.themeMode) },
            { settings.setNewEpisodeAlerts(restored.newEpisodeAlerts) },
            { bookmarkSort.setSort(backup.bookmarkSort) },
        )
        val failure = writes.firstNotNullOfOrNull { write -> write().exceptionOrNull() }
        return if (failure == null) Result.success(Unit) else Result.failure(failure)
    }
}
