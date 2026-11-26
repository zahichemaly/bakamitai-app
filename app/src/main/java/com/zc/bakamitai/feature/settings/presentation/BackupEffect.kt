package com.zc.bakamitai.feature.settings.presentation

import com.zc.bakamitai.common.ui.text.UiText

sealed interface BackupEffect {

    data object NavigateBack : BackupEffect

    /** The system folder picker (the Storage Access Framework). */
    data object OpenFolderPicker : BackupEffect

    /** The system file picker, for a backup to restore. */
    data object OpenFilePicker : BackupEffect

    /** A snackbar: a backup saved, a restore done, or why one failed. */
    data class ShowMessage(val message: UiText) : BackupEffect
}
