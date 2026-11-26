package com.zc.bakamitai.feature.settings.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Settings, pushed on the app back stack: it covers the tabs, bottom bar or rail included. */
@Serializable
data object SettingsNavKey : NavKey
