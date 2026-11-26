package com.zc.bakamitai.feature.settings.presentation

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zc.bakamitai.common.ui.resource.StringResource
import com.zc.bakamitai.common.ui.text.LocalUse24HourClock
import com.zc.bakamitai.common.ui.text.UiText
import com.zc.bakamitai.common.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Any file: some providers don't report backups as JSON. */
private val BackupFileTypes = arrayOf("*/*")

/**
 * Backup and restore's navigation destination. It opens the system folder and file pickers (the Storage Access
 * Framework) and shows snackbars, in response to effects; the pickers' answers go back as intents.
 *
 * @param onNavigateBack leaves the screen: the back arrow.
 */
@Composable
fun BackupRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val use24HourClock = LocalUse24HourClock.current
    val scope = rememberCoroutineScope()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        viewModel.onIntent(BackupIntent.FolderChosen(uri?.toString()))
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onIntent(BackupIntent.RestoreFileChosen(uri?.toString()))
    }

    fun showMessage(message: UiText) {
        scope.launch { snackbarHostState.showSnackbar(message.asString(context, use24HourClock)) }
    }

    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            BackupEffect.NavigateBack -> onNavigateBack()

            BackupEffect.OpenFolderPicker -> try {
                folderPicker.launch(null)
            } catch (ignored: ActivityNotFoundException) {
                showMessage(UiText.StringResource(StringResource.backup_no_picker))
            }

            BackupEffect.OpenFilePicker -> try {
                filePicker.launch(BackupFileTypes)
            } catch (ignored: ActivityNotFoundException) {
                showMessage(UiText.StringResource(StringResource.backup_no_picker))
            }

            is BackupEffect.ShowMessage -> showMessage(effect.message)
        }
    }

    BackupScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
