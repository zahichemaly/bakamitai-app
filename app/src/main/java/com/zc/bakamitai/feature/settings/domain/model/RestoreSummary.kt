package com.zc.bakamitai.feature.settings.domain.model

/**
 * What a restore did: the settings are always written.
 *
 * @property bookmarksAdded the backup's bookmarks that weren't saved yet, now added.
 * @property bookmarksInBackup every bookmark in the backup.
 */
data class RestoreSummary(val bookmarksAdded: Int, val bookmarksInBackup: Int)
