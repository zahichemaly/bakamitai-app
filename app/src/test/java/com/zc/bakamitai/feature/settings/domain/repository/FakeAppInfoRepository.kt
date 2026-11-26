package com.zc.bakamitai.feature.settings.domain.repository

import com.zc.bakamitai.feature.settings.domain.model.AppInfo

/** A fixed [info]. */
class FakeAppInfoRepository(
    private val info: AppInfo = AppInfo(
        versionName = "1.0",
        sourceCodeUrl = "https://github.com/zahichemaly/BakaMitai",
        isDebugBuild = true,
    ),
) : AppInfoRepository {
    override fun getAppInfo(): AppInfo = info
}
