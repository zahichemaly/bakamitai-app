package com.zc.bakamitai.feature.settings.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zc.bakamitai.feature.settings.domain.usecase.CreateBackupUseCase
import org.koin.android.annotation.KoinWorker

/**
 * An automatic backup (docs/7-backup.md): writes one into the backup folder and deletes the oldest past *Backups
 * to keep*. A failure (no folder, the folder's access lost) waits for the next period: retrying wouldn't fix it.
 *
 * WorkManager persists this class's name: **never rename or move it**.
 */
@KoinWorker
class BackupWorker(context: Context, params: WorkerParameters, private val createBackup: CreateBackupUseCase) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = createBackup().fold(
        onSuccess = { Result.success() },
        onFailure = { Result.failure() },
    )
}
