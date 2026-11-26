package com.zc.bakamitai.feature.settings.backup.domain.usecase

import com.zc.bakamitai.feature.settings.backup.domain.model.BackupPreferences
import com.zc.bakamitai.feature.settings.backup.domain.repository.BackupPreferencesRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

/** The backup settings, then every change. */
@Factory
class ObserveBackupPreferencesUseCase(private val repository: BackupPreferencesRepository) {
    operator fun invoke(): Flow<BackupPreferences> = repository.observePreferences()
}
