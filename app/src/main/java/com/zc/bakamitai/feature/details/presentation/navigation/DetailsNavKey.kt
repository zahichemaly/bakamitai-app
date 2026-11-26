package com.zc.bakamitai.feature.details.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** One show's Details, pushed on the app back stack: it covers the tabs, bottom bar or rail included. */
@Serializable
data class DetailsNavKey(val slug: String) : NavKey
