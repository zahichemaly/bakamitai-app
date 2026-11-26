package com.zc.bakamitai.feature.settings.data.mapper

import com.zc.bakamitai.common.domain.model.AppSettings
import com.zc.bakamitai.common.domain.model.Show
import com.zc.bakamitai.common.domain.model.ShowSlug
import com.zc.bakamitai.common.domain.model.ThemeMode
import com.zc.bakamitai.common.domain.model.TimeFormat
import com.zc.bakamitai.common.domain.model.WeekStart
import com.zc.bakamitai.feature.bookmarks.domain.model.Bookmark
import com.zc.bakamitai.feature.bookmarks.domain.model.BookmarkSort
import com.zc.bakamitai.feature.settings.data.dto.BackupBookmarkDTO
import com.zc.bakamitai.feature.settings.data.dto.BackupDTO
import com.zc.bakamitai.feature.settings.domain.model.Backup
import kotlin.time.Instant

/**
 * A read backup file. A missing or unknown setting is its default; a bookmark without a slug or a title is
 * dropped, one without a date is dated [createdAt][BackupDTO.createdAt], and a repeated slug is kept once.
 */
fun BackupDTO.toDomain(): Backup {
    val created = Instant.fromEpochMilliseconds(createdAt)
    val defaults = AppSettings()
    return Backup(
        createdAt = created,
        appVersion = appVersion.orEmpty(),
        settings = AppSettings(
            weekStart = settings?.weekStart.toEnumOrNull<WeekStart>() ?: defaults.weekStart,
            timeFormat = settings?.timeFormat.toEnumOrNull<TimeFormat>() ?: defaults.timeFormat,
            themeMode = settings?.themeMode.toEnumOrNull<ThemeMode>() ?: defaults.themeMode,
            newEpisodeAlerts = settings?.newEpisodeAlerts ?: defaults.newEpisodeAlerts,
        ),
        bookmarkSort = settings?.bookmarksSort.toEnumOrNull<BookmarkSort>() ?: BookmarkSort.RECENTLY_ADDED,
        bookmarks = bookmarks.orEmpty()
            .mapNotNull { it.toBookmark(defaultAddedAt = created) }
            .distinctBy { it.show.slug },
    )
}

private fun BackupBookmarkDTO.toBookmark(defaultAddedAt: Instant): Bookmark? {
    val slug = slug?.trim().orEmpty()
    val title = title?.trim().orEmpty()
    return if (slug.isEmpty() || title.isEmpty()) {
        null
    } else {
        Bookmark(
            show = Show(slug = ShowSlug(slug), title = title, coverUrl = coverUrl?.takeIf { it.isNotBlank() }),
            addedAt = addedAt?.let(Instant::fromEpochMilliseconds) ?: defaultAddedAt,
        )
    }
}

private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
