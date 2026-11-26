package com.zc.bakamitai.feature.settings.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A backed-up bookmark: the show and when it was saved.
 *
 * @property addedAt epoch milliseconds.
 */
@Serializable
data class BackupBookmarkDTO(
    @SerialName("slug") val slug: String?,
    @SerialName("title") val title: String?,
    @SerialName("cover_url") val coverUrl: String?,
    @SerialName("added_at") val addedAt: Long?,
)
