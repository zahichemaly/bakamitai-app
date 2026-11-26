package com.zc.bakamitai.feature.settings.backup.data.worker

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.backup.domain.repository.BackupScheduler
import org.koin.core.annotation.Single
import java.util.concurrent.TimeUnit

/** The unique work name (and tag) of the automatic backups. Persisted by WorkManager: never rename it. */
const val AUTOMATIC_BACKUP_WORK = "automatic-backup"

/**
 * Runs [BackupWorker] as one unique periodic job: local work only, so no network constraint, but not on a low
 * battery or nearly full storage. The first run comes one interval after it's scheduled.
 */
@Single
class BackupSchedulerImpl(private val workManager: WorkManager) : BackupScheduler {

    override fun schedule(frequency: BackupFrequency) {
        val interval = frequency.interval
        if (interval == null) {
            workManager.cancelUniqueWork(AUTOMATIC_BACKUP_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<BackupWorker>(interval.inWholeMinutes, TimeUnit.MINUTES)
            .setInitialDelay(interval.inWholeMinutes, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).setRequiresStorageNotLow(true).build())
            .addTag(AUTOMATIC_BACKUP_WORK)
            .build()
        workManager.enqueueUniquePeriodicWork(AUTOMATIC_BACKUP_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
