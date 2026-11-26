package com.zc.bakamitai.feature.settings.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Backup and restore, pushed on the app back stack over Settings. */
@Serializable
data object BackupNavKey : NavKey
