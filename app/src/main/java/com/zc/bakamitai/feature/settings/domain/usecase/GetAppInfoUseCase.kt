package com.zc.bakamitai.feature.settings.domain.usecase

import com.zc.bakamitai.feature.settings.domain.model.AppInfo
import com.zc.bakamitai.feature.settings.domain.repository.AppInfoRepository
import org.koin.core.annotation.Factory

/** The app's version, source code URL and build type, for the footer and the About section. */
@Factory
class GetAppInfoUseCase(private val repository: AppInfoRepository) {
    operator fun invoke(): AppInfo = repository.getAppInfo()
}
