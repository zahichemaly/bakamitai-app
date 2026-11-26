package com.zc.bakamitai.feature.settings.backup.presentation.component.restoredialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zc.bakamitai.common.ui.resource.StringResource
import com.zc.bakamitai.common.ui.text.UiText
import com.zc.bakamitai.common.ui.theme.BakaMitaiTheme
import com.zc.bakamitai.common.ui.util.AdaptivePreviews

/**
 * Asks before a restore: the backup's bookmarks are added and its settings replace the current ones. *Restore*
 * calls [onConfirm]; *Cancel*, Back and a tap outside call [onDismiss].
 */
@Composable
fun RestoreDialogView(onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = UiText.StringResource(StringResource.backup_restore_dialog_confirm).asString())
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = UiText.StringResource(StringResource.backup_restore_dialog_cancel).asString())
            }
        },
        title = { Text(text = UiText.StringResource(StringResource.backup_restore_dialog_title).asString()) },
        text = { Text(text = UiText.StringResource(StringResource.backup_restore_dialog_text).asString()) },
    )
}

@AdaptivePreviews
@Composable
private fun RestoreDialogViewPreview() {
    BakaMitaiTheme {
        RestoreDialogView(onConfirm = {}, onDismiss = {})
    }
}
