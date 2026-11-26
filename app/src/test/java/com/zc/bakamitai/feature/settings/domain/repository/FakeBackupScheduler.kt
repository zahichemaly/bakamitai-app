package com.zc.bakamitai.feature.settings.domain.repository

import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency

/** Records every schedule, in order. */
class FakeBackupScheduler : BackupScheduler {

    val scheduled = mutableListOf<BackupFrequency>()

    override fun schedule(frequency: BackupFrequency) {
        scheduled += frequency
    }
}
