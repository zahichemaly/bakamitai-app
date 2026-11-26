package com.zc.bakamitai.feature.settings.domain.model

/**
 * About the installed build.
 *
 * @property versionName its version ("2.0.0").
 * @property sourceCodeUrl where the app's source code is.
 * @property isDebugBuild it's a debug build (Settings shows the About section on those only).
 */
data class AppInfo(val versionName: String, val sourceCodeUrl: String, val isDebugBuild: Boolean)
