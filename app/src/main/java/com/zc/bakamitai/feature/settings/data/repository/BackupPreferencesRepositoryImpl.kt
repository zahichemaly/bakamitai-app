package com.zc.bakamitai.feature.settings.data.repository

import com.zc.bakamitai.common.data.util.safeDataStoreCall
import com.zc.bakamitai.feature.settings.data.local.BackupPreferencesLocalDataSource
import com.zc.bakamitai.feature.settings.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.domain.model.BackupPreferences
import com.zc.bakamitai.feature.settings.domain.repository.BackupPreferencesRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single
import kotlin.time.Instant

/** The backup settings from the local DataStore; writes are wrapped in [safeDataStoreCall]. */
@Single
class BackupPreferencesRepositoryImpl(private val localDataSource: BackupPreferencesLocalDataSource) :
    BackupPreferencesRepository {

    override fun observePreferences(): Flow<BackupPreferences> = localDataSource.preferences

    override suspend fun setFolder(folder: BackupFolder): Result<Unit> =
        safeDataStoreCall { localDataSource.setFolder(folder) }

    override suspend fun setFrequency(frequency: BackupFrequency): Result<Unit> =
        safeDataStoreCall { localDataSource.setFrequency(frequency) }

    override suspend fun setMaxBackups(count: Int): Result<Unit> =
        safeDataStoreCall { localDataSource.setMaxBackups(count) }

    override suspend fun setLastBackupAt(at: Instant): Result<Unit> =
        safeDataStoreCall { localDataSource.setLastBackupAt(at) }
}
