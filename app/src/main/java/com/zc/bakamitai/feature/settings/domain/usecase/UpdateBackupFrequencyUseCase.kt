package com.zc.bakamitai.feature.settings.domain.usecase

import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.domain.repository.BackupPreferencesRepository
import com.zc.bakamitai.feature.settings.domain.repository.BackupScheduler
import org.koin.core.annotation.Factory

/** Saves how often automatic backups run, then schedules them from now (or stops them, for `OFF`). */
@Factory
class UpdateBackupFrequencyUseCase(
    private val preferences: BackupPreferencesRepository,
    private val scheduler: BackupScheduler,
) {

    suspend operator fun invoke(frequency: BackupFrequency): Result<Unit> =
        preferences.setFrequency(frequency).onSuccess { scheduler.schedule(frequency) }
}
