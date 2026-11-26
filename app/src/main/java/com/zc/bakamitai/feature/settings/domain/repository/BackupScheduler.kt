package com.zc.bakamitai.feature.settings.domain.repository

import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency

/** Schedules automatic backups (a port: implemented with WorkManager in `data/worker/`). */
interface BackupScheduler {

    /** Runs a backup every [frequency]'s interval from now on, replacing any earlier schedule; `OFF` stops them. */
    fun schedule(frequency: BackupFrequency)
}
