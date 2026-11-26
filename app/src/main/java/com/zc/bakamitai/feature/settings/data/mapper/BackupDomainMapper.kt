package com.zc.bakamitai.feature.settings.data.mapper

import com.zc.bakamitai.feature.bookmarks.domain.model.Bookmark
import com.zc.bakamitai.feature.settings.data.dto.BackupBookmarkDTO
import com.zc.bakamitai.feature.settings.data.dto.BackupDTO
import com.zc.bakamitai.feature.settings.data.dto.BackupSettingsDTO
import com.zc.bakamitai.feature.settings.domain.model.Backup

/** The backup file format this app writes, and the newest it reads. */
const val BACKUP_FORMAT_VERSION = 1

/** [this] backup as a file of format [BACKUP_FORMAT_VERSION]: enums by name, instants in epoch milliseconds. */
fun Backup.toDTO(): BackupDTO = BackupDTO(
    version = BACKUP_FORMAT_VERSION,
    createdAt = createdAt.toEpochMilliseconds(),
    appVersion = appVersion,
    settings = BackupSettingsDTO(
        weekStart = settings.weekStart.name,
        timeFormat = settings.timeFormat.name,
        themeMode = settings.themeMode.name,
        newEpisodeAlerts = settings.newEpisodeAlerts,
        bookmarksSort = bookmarkSort.name,
    ),
    bookmarks = bookmarks.map { it.toBackupDTO() },
)

private fun Bookmark.toBackupDTO(): BackupBookmarkDTO = BackupBookmarkDTO(
    slug = show.slug.value,
    title = show.title,
    coverUrl = show.coverUrl,
    addedAt = addedAt.toEpochMilliseconds(),
)
