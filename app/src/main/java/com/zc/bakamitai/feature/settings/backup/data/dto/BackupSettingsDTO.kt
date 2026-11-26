package com.zc.bakamitai.feature.settings.backup.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A backup's settings, each enum by name as in the settings DataStore; a missing or unknown one is the default. */
@Serializable
data class BackupSettingsDTO(
    @SerialName("week_start") val weekStart: String?,
    @SerialName("time_format") val timeFormat: String?,
    @SerialName("theme_mode") val themeMode: String?,
    @SerialName("new_episode_alerts") val newEpisodeAlerts: Boolean?,
    @SerialName("bookmarks_sort") val bookmarksSort: String?,
)
