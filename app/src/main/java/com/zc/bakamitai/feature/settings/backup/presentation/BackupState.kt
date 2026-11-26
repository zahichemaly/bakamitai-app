package com.zc.bakamitai.feature.settings.backup.presentation

import androidx.compose.runtime.Immutable
import com.zc.bakamitai.common.ui.text.UiText
import com.zc.bakamitai.feature.settings.backup.presentation.model.BackupFrequencyOption
import com.zc.bakamitai.feature.settings.backup.presentation.model.BackupPicker
import com.zc.bakamitai.feature.settings.presentation.model.PickerUIState

/**
 * The Backup and restore screen (docs/7-backup.md §5.1).
 *
 * @property isLoading until the backup settings are first read; the cards show skeletons.
 * @property hasFolder a backup folder is chosen.
 * @property folderLabel the folder's name, or "Not set"; `null` while loading.
 * @property lastBackupLabel "Last backup: …" or "No backups yet"; `null` while loading.
 * @property frequency the *Automatic backups* row and dialog; `null` while loading. [maxBackups] alike.
 * @property isCreating a backup is being written; [isRestoring] alike for a restore.
 * @property openPicker the picker whose dialog is open, or none (kept here so it survives rotation).
 * @property pendingRestore the chosen backup file's URI while the restore confirmation is shown.
 */
@Immutable
data class BackupState(
    val isLoading: Boolean = true,
    val hasFolder: Boolean = false,
    val folderLabel: UiText? = null,
    val lastBackupLabel: UiText? = null,
    val frequency: PickerUIState<BackupFrequencyOption>? = null,
    val maxBackups: PickerUIState<Int>? = null,
    val isCreating: Boolean = false,
    val isRestoring: Boolean = false,
    val openPicker: BackupPicker? = null,
    val pendingRestore: String? = null,
) {
    /** A backup or a restore is running: Create and Restore ignore taps. */
    val isBusy: Boolean get() = isCreating || isRestoring
}
