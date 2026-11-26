package com.zc.bakamitai.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The tab shell ([MainShell]): always at the bottom of the app back stack, under every pushed screen. */
@Serializable
data object MainNavKey : NavKey
