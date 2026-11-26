package com.zc.bakamitai.feature.settings.domain.usecase

import com.zc.bakamitai.feature.settings.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.domain.repository.BackupFileRepository
import com.zc.bakamitai.feature.settings.domain.repository.BackupPreferencesRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

/**
 * Makes the folder the user just chose ([uri]) the backup folder: keeps the app's access to it, releases the
 * previous folder's, and saves it. Returns the folder with its name.
 */
@Factory
class SetBackupFolderUseCase(
    private val preferences: BackupPreferencesRepository,
    private val files: BackupFileRepository,
) {

    suspend operator fun invoke(uri: String): Result<BackupFolder> {
        val previous = preferences.observePreferences().first().folder
        return files.grantFolder(uri, previous).fold(
            onSuccess = { folder -> preferences.setFolder(folder).map { folder } },
            onFailure = { Result.failure(it) },
        )
    }
}
