package com.zc.bakamitai.feature.settings.presentation

import com.zc.bakamitai.feature.settings.presentation.model.BackupFrequencyOption
import com.zc.bakamitai.feature.settings.presentation.model.BackupPicker

sealed interface BackupIntent {

    data object BackClicked : BackupIntent

    /** The *Backup folder* row: opens the system folder picker. */
    data object FolderClicked : BackupIntent

    /** The folder picker's answer: the folder's URI, or `null` when cancelled. */
    data class FolderChosen(val uri: String?) : BackupIntent

    /** *Create backup*: writes one now, or asks for a folder first. */
    data object CreateClicked : BackupIntent

    /** *Restore backup*: opens the system file picker. */
    data object RestoreClicked : BackupIntent

    /** The file picker's answer: the file's URI, or `null` when cancelled. A file opens the confirmation. */
    data class RestoreFileChosen(val uri: String?) : BackupIntent

    /** The confirmation's *Restore*. */
    data object RestoreConfirmed : BackupIntent

    /** The confirmation's *Cancel*, Back or a tap outside it. */
    data object RestoreDismissed : BackupIntent

    /** A picker row was tapped: opens its dialog. */
    data class PickerClicked(val picker: BackupPicker) : BackupIntent

    /** The open dialog's Cancel, Back or a tap outside it. */
    data object PickerDismissed : BackupIntent

    data class FrequencySelected(val option: BackupFrequencyOption) : BackupIntent

    data class MaxBackupsSelected(val count: Int) : BackupIntent
}
