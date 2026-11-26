package com.zc.bakamitai.feature.settings.backup.domain.model

import kotlin.time.Instant

/**
 * The backup settings, on this device only (a backup never restores them).
 *
 * @property folder where backups go; `null` until chosen.
 * @property maxBackups how many BakaMitai backup files stay in [folder]; older ones are deleted.
 * @property lastBackupAt when the last backup was written; `null` = never.
 */
data class BackupPreferences(
    val folder: BackupFolder? = null,
    val frequency: BackupFrequency = BackupFrequency.OFF,
    val maxBackups: Int = DEFAULT_MAX_BACKUPS,
    val lastBackupAt: Instant? = null,
) {
    companion object {
        const val DEFAULT_MAX_BACKUPS = 3

        /** The *Backups to keep* choices, in order. */
        val MAX_BACKUPS_OPTIONS: List<Int> = listOf(1, 2, 3, 5, 10)
    }
}
