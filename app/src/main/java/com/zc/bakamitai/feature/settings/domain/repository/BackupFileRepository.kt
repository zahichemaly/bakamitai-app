package com.zc.bakamitai.feature.settings.domain.repository

import com.zc.bakamitai.feature.settings.domain.model.Backup
import com.zc.bakamitai.feature.settings.domain.model.BackupFolder

/**
 * Backup files in a user-chosen folder (the Storage Access Framework). Failures are `BackupError`s.
 */
interface BackupFileRepository {

    /**
     * Keeps the app's access to the folder [uri] (just chosen by the user) across restarts, releases [previous]'s,
     * and returns the folder with its name.
     */
    suspend fun grantFolder(uri: String, previous: BackupFolder?): Result<BackupFolder>

    /** Writes [backup] as a new file in [folder] and returns the file's name. */
    suspend fun write(folder: BackupFolder, backup: Backup): Result<String>

    /** Reads the backup file [uri] (chosen by the user). */
    suspend fun read(uri: String): Result<Backup>

    /** Deletes the oldest BakaMitai backup files in [folder] so that at most [keep] remain. */
    suspend fun deleteOldBackups(folder: BackupFolder, keep: Int): Result<Unit>
}
