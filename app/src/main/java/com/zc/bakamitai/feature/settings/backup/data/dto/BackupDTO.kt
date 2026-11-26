package com.zc.bakamitai.feature.settings.backup.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A backup file (docs/7-backup.md §4.1). [version] is the file format's: a reader refuses a higher one.
 *
 * @property createdAt epoch milliseconds.
 */
@Serializable
data class BackupDTO(
    @SerialName("version") val version: Int,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("app_version") val appVersion: String?,
    @SerialName("settings") val settings: BackupSettingsDTO?,
    @SerialName("bookmarks") val bookmarks: List<BackupBookmarkDTO>?,
)
