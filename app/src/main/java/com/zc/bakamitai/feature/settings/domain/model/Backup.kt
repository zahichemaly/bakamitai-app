package com.zc.bakamitai.feature.settings.domain.model

import com.zc.bakamitai.common.domain.model.AppSettings
import com.zc.bakamitai.feature.bookmarks.domain.model.Bookmark
import com.zc.bakamitai.feature.bookmarks.domain.model.BookmarkSort
import kotlin.time.Instant

/**
 * What a backup file holds (docs/7-backup.md §4.1): the user's settings, the Bookmarks sort and every bookmark.
 * Bookmarks carry their show and date only; their weekly slot and alert tracking are re-derived after a restore.
 *
 * @property appVersion the version that wrote it ("2.0.0").
 */
data class Backup(
    val createdAt: Instant,
    val appVersion: String,
    val settings: AppSettings,
    val bookmarkSort: BookmarkSort,
    val bookmarks: List<Bookmark>,
)
