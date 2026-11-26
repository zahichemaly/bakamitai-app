package com.zc.bakamitai.feature.settings.domain.repository

import com.zc.bakamitai.feature.settings.domain.model.Backup
import com.zc.bakamitai.feature.settings.domain.model.BackupError
import com.zc.bakamitai.feature.settings.domain.model.BackupFolder

/**
 * Backup files in memory: [files] maps a file URI to its backup. Folders are granted with their [folderNames]
 * (or "Backups"). Every call fails with its `…Failure` when set.
 */
class FakeBackupFileRepository : BackupFileRepository {

    val files = mutableMapOf<String, Backup>()
    val folderNames = mutableMapOf<String, String>()

    var grantFailure: Throwable? = null
    var writeFailure: Throwable? = null
    var pruneFailure: Throwable? = null

    val granted = mutableListOf<Pair<String, BackupFolder?>>()
    val written = mutableListOf<Pair<BackupFolder, Backup>>()
    val pruned = mutableListOf<Pair<BackupFolder, Int>>()

    override suspend fun grantFolder(uri: String, previous: BackupFolder?): Result<BackupFolder> {
        granted += uri to previous
        grantFailure?.let { return Result.failure(it) }
        return Result.success(BackupFolder(uri = uri, name = folderNames[uri] ?: "Backups"))
    }

    override suspend fun write(folder: BackupFolder, backup: Backup): Result<String> {
        written += folder to backup
        writeFailure?.let { return Result.failure(it) }
        return Result.success("bakamitai_backup.json")
    }

    override suspend fun read(uri: String): Result<Backup> =
        files[uri]?.let { Result.success(it) } ?: Result.failure(BackupError.InvalidFile())

    override suspend fun deleteOldBackups(folder: BackupFolder, keep: Int): Result<Unit> {
        pruned += folder to keep
        pruneFailure?.let { return Result.failure(it) }
        return Result.success(Unit)
    }
}
