package com.zc.bakamitai.feature.settings.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/**
 * Registers the annotated classes of the Settings feature (use cases, `AppInfoRepositoryImpl`, the ViewModel) and of
 * its sub-feature `backup` (structure skill §1.2), which has no module of its own.
 */
@Module
@ComponentScan("com.zc.bakamitai.feature.settings")
class SettingsModule
