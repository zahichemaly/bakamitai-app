package com.zc.bakamitai.feature.settings.data.repository

import com.zc.bakamitai.BuildConfig
import com.zc.bakamitai.feature.settings.domain.model.AppInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class AppInfoRepositoryImplTest {

    @Test
    fun `the build's version and build type, and the project's repository`() {
        assertEquals(
            AppInfo(
                versionName = BuildConfig.VERSION_NAME,
                sourceCodeUrl = "https://github.com/zahichemaly/BakaMitai",
                isDebugBuild = BuildConfig.DEBUG,
            ),
            AppInfoRepositoryImpl().getAppInfo(),
        )
    }
}
