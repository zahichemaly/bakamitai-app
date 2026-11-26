package com.zc.bakamitai.feature.settings.backup.domain.model

/** Why a backup or a restore failed (docs/7-backup.md §4.4). Other failures are `DomainError`s. */
sealed class BackupError(cause: Throwable? = null) : Exception(cause) {

    /** No backup folder was chosen. */
    data object NoFolder : BackupError()

    /** The folder can't be written: the app lost its access, or the folder is gone. Choosing it again fixes it. */
    data class FolderUnavailable(override val cause: Throwable? = null) : BackupError(cause)

    /** The file can't be read, or isn't a BakaMitai backup. */
    data class InvalidFile(override val cause: Throwable? = null) : BackupError(cause)

    /** The file was written by a newer version of the app, in a format this one doesn't know. */
    data object UnsupportedVersion : BackupError()
}
