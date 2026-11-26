package com.zc.bakamitai.feature.settings.backup.domain.repository

import com.zc.bakamitai.feature.settings.backup.domain.model.BackupFrequency

/** Records every schedule, in order. */
class FakeBackupScheduler : BackupScheduler {

    val scheduled = mutableListOf<BackupFrequency>()

    override fun schedule(frequency: BackupFrequency) {
        scheduled += frequency
    }
}
