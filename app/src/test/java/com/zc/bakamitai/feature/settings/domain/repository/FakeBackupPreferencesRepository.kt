package com.zc.bakamitai.feature.settings.domain.repository

import com.zc.bakamitai.common.domain.model.DomainError
import com.zc.bakamitai.feature.settings.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.domain.model.BackupPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import kotlin.time.Instant

/** The backup settings in memory; [failWrites] makes every write fail like a full disk. */
class FakeBackupPreferencesRepository(initial: BackupPreferences = BackupPreferences()) : BackupPreferencesRepository {

    val preferences = MutableStateFlow(initial)
    var failWrites = false

    override fun observePreferences(): Flow<BackupPreferences> = preferences

    override suspend fun setFolder(folder: BackupFolder): Result<Unit> = write { it.copy(folder = folder) }

    override suspend fun setFrequency(frequency: BackupFrequency): Result<Unit> =
        write { it.copy(frequency = frequency) }

    override suspend fun setMaxBackups(count: Int): Result<Unit> = write { it.copy(maxBackups = count) }

    override suspend fun setLastBackupAt(at: Instant): Result<Unit> = write { it.copy(lastBackupAt = at) }

    private fun write(transform: (BackupPreferences) -> BackupPreferences): Result<Unit> {
        if (failWrites) return Result.failure(DomainError.Unknown(IOException("disk full")))
        preferences.update(transform)
        return Result.success(Unit)
    }
}
