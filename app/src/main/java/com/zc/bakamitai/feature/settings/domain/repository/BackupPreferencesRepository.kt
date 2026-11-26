package com.zc.bakamitai.feature.settings.domain.repository

import com.zc.bakamitai.feature.settings.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.domain.model.BackupPreferences
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** The backup settings. Reads never fail (a read error gives the defaults); a failed write is a `DomainError`. */
interface BackupPreferencesRepository {

    /** The current backup settings, then every change. */
    fun observePreferences(): Flow<BackupPreferences>

    suspend fun setFolder(folder: BackupFolder): Result<Unit>

    suspend fun setFrequency(frequency: BackupFrequency): Result<Unit>

    suspend fun setMaxBackups(count: Int): Result<Unit>

    suspend fun setLastBackupAt(at: Instant): Result<Unit>
}
