package com.zc.bakamitai.feature.settings.backup.domain.usecase

import com.zc.bakamitai.feature.settings.backup.domain.repository.BackupPreferencesRepository
import org.koin.core.annotation.Factory

/** Saves how many backup files stay in the folder; the next backup deletes the extra ones. */
@Factory
class UpdateMaxBackupsUseCase(private val preferences: BackupPreferencesRepository) {

    suspend operator fun invoke(count: Int): Result<Unit> {
        require(count > 0) { "At least one backup is kept" }
        return preferences.setMaxBackups(count)
    }
}
