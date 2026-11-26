package com.zc.bakamitai.feature.settings.presentation.mapper

import com.zc.bakamitai.common.ui.component.error.mapper.toErrorUIModel
import com.zc.bakamitai.common.ui.resource.StringResource
import com.zc.bakamitai.common.ui.text.UiText
import com.zc.bakamitai.feature.settings.domain.model.BackupError

/** The snackbar text for a failed backup, restore or backup setting (docs/7-backup.md §4.4). */
fun Throwable.toBackupMessage(): UiText = when (this) {
    BackupError.NoFolder -> UiText.StringResource(StringResource.backup_error_no_folder)
    is BackupError.FolderUnavailable -> UiText.StringResource(StringResource.backup_error_folder)
    is BackupError.InvalidFile -> UiText.StringResource(StringResource.backup_error_invalid_file)
    BackupError.UnsupportedVersion -> UiText.StringResource(StringResource.backup_error_version)
    else -> toErrorUIModel().message
}
