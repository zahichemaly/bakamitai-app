package com.zc.bakamitai.feature.settings.domain

import com.zc.bakamitai.common.domain.model.AppSettings
import com.zc.bakamitai.common.domain.model.ThemeMode
import com.zc.bakamitai.common.domain.model.TimeFormat
import com.zc.bakamitai.common.domain.model.WeekStart
import com.zc.bakamitai.feature.bookmarks.domain.BookmarksTestData.bookmark
import com.zc.bakamitai.feature.bookmarks.domain.model.BookmarkSort
import com.zc.bakamitai.feature.settings.domain.model.Backup
import com.zc.bakamitai.feature.settings.domain.model.BackupFolder
import kotlin.time.Instant

/** Builders for the backup domain in tests. */
object BackupTestData {

    val NOW: Instant = Instant.parse("2026-11-26T10:47:05Z")

    val folder = BackupFolder(uri = "content://tree/backups", name = "Backups")

    /** Every setting off its default, two bookmarks. */
    fun backup(
        settings: AppSettings = AppSettings(
            weekStart = WeekStart.SUNDAY,
            timeFormat = TimeFormat.TWENTY_FOUR_HOUR,
            themeMode = ThemeMode.DARK,
            newEpisodeAlerts = false,
        ),
        bookmarkSort: BookmarkSort = BookmarkSort.TITLE,
    ) = Backup(
        createdAt = NOW,
        appVersion = "1.0",
        settings = settings,
        bookmarkSort = bookmarkSort,
        bookmarks = listOf(
            bookmark("frieren", "2026-11-10T00:00:00Z", title = "Sousou no Frieren"),
            bookmark("dandadan", "2026-11-12T00:00:00Z", title = "Dandadan"),
        ),
    )
}
