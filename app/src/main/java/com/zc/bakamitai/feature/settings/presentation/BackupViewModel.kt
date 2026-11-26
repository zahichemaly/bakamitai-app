package com.zc.bakamitai.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zc.bakamitai.feature.settings.domain.model.BackupError
import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.domain.usecase.CreateBackupUseCase
import com.zc.bakamitai.feature.settings.domain.usecase.ObserveBackupPreferencesUseCase
import com.zc.bakamitai.feature.settings.domain.usecase.RestoreBackupUseCase
import com.zc.bakamitai.feature.settings.domain.usecase.SetBackupFolderUseCase
import com.zc.bakamitai.feature.settings.domain.usecase.UpdateBackupFrequencyUseCase
import com.zc.bakamitai.feature.settings.domain.usecase.UpdateMaxBackupsUseCase
import com.zc.bakamitai.feature.settings.presentation.mapper.BackupUIModelMapper
import com.zc.bakamitai.feature.settings.presentation.mapper.toBackupFrequency
import com.zc.bakamitai.feature.settings.presentation.mapper.toBackupMessage
import com.zc.bakamitai.feature.settings.presentation.mapper.toRestoredMessage
import com.zc.bakamitai.feature.settings.presentation.mapper.toSavedMessage
import com.zc.bakamitai.feature.settings.presentation.model.BackupFrequencyOption
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Backup and restore (docs/7-backup.md): the folder, a manual backup or restore, and the automatic backups. The
 * screen follows the stored backup settings; a choice is saved at once. A backup without a folder opens the folder
 * picker first, then runs once a folder is chosen. Every outcome is a snackbar.
 */
@KoinViewModel
class BackupViewModel(
    observeBackupPreferences: ObserveBackupPreferencesUseCase,
    private val setBackupFolder: SetBackupFolderUseCase,
    private val createBackup: CreateBackupUseCase,
    private val restoreBackup: RestoreBackupUseCase,
    private val updateBackupFrequency: UpdateBackupFrequencyUseCase,
    private val updateMaxBackups: UpdateMaxBackupsUseCase,
    private val mapper: BackupUIModelMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> =
        combine(_state, observeBackupPreferences()) { state, preferences ->
            state.copy(
                isLoading = false,
                hasFolder = preferences.folder != null,
                folderLabel = mapper.folderLabel(preferences.folder),
                lastBackupLabel = mapper.lastBackupLabel(preferences.lastBackupAt),
                frequency = mapper.frequencyPicker(preferences.frequency),
                maxBackups = mapper.maxBackupsPicker(preferences.maxBackups),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = _state.value,
        )

    private val _effect = Channel<BackupEffect>(Channel.BUFFERED)
    val effect: Flow<BackupEffect> = _effect.receiveAsFlow()

    /** A backup waits for the folder the user is choosing. Never exposed. */
    private var createWhenFolderChosen = false

    fun onIntent(intent: BackupIntent) {
        when (intent) {
            BackupIntent.BackClicked -> sendEffect(BackupEffect.NavigateBack)

            BackupIntent.FolderClicked -> sendEffect(BackupEffect.OpenFolderPicker)

            is BackupIntent.FolderChosen -> onFolderChosen(intent.uri)

            BackupIntent.CreateClicked -> onCreateClicked()

            BackupIntent.RestoreClicked -> sendEffect(BackupEffect.OpenFilePicker)

            // A chosen file waits for the confirmation (none is open while picking); a cancelled pick shows none.
            is BackupIntent.RestoreFileChosen -> _state.update { it.copy(pendingRestore = intent.uri) }

            BackupIntent.RestoreConfirmed -> restore()

            BackupIntent.RestoreDismissed -> _state.update { it.copy(pendingRestore = null) }

            is BackupIntent.PickerClicked -> _state.update { it.copy(openPicker = intent.picker) }

            BackupIntent.PickerDismissed -> _state.update { it.copy(openPicker = null) }

            is BackupIntent.FrequencySelected -> onFrequencySelected(intent.option)

            is BackupIntent.MaxBackupsSelected -> onMaxBackupsSelected(intent.count)
        }
    }

    private fun onFolderChosen(uri: String?) {
        val createNext = createWhenFolderChosen
        createWhenFolderChosen = false
        if (uri == null) return
        viewModelScope.launch {
            setBackupFolder(uri)
                .onSuccess { if (createNext) create() }
                .onFailure { showError(it) }
        }
    }

    private fun onCreateClicked() {
        when {
            state.value.isBusy -> Unit
            state.value.hasFolder -> viewModelScope.launch { create() }
            else -> askForFolderThenCreate()
        }
    }

    private fun askForFolderThenCreate() {
        createWhenFolderChosen = true
        sendEffect(BackupEffect.OpenFolderPicker)
    }

    private suspend fun create() {
        _state.update { it.copy(isCreating = true) }
        createBackup()
            .onSuccess { folder -> _effect.send(BackupEffect.ShowMessage(folder.toSavedMessage())) }
            .onFailure { error -> if (error == BackupError.NoFolder) askForFolderThenCreate() else showError(error) }
        _state.update { it.copy(isCreating = false) }
    }

    private fun restore() {
        val uri = _state.value.pendingRestore
        if (uri == null || state.value.isBusy) return
        _state.update { it.copy(pendingRestore = null, isRestoring = true) }
        viewModelScope.launch {
            restoreBackup(uri)
                .onSuccess { summary -> _effect.send(BackupEffect.ShowMessage(summary.toRestoredMessage())) }
                .onFailure { showError(it) }
            _state.update { it.copy(isRestoring = false) }
        }
    }

    /** Saves the interval; turning automatic backups on without a folder asks for one. */
    private fun onFrequencySelected(option: BackupFrequencyOption) {
        _state.update { it.copy(openPicker = null) }
        val frequency = option.toBackupFrequency()
        viewModelScope.launch {
            updateBackupFrequency(frequency)
                .onSuccess {
                    if (frequency != BackupFrequency.OFF && !state.value.hasFolder) {
                        _effect.send(BackupEffect.OpenFolderPicker)
                    }
                }
                .onFailure { showError(it) }
        }
    }

    private fun onMaxBackupsSelected(count: Int) {
        _state.update { it.copy(openPicker = null) }
        viewModelScope.launch { updateMaxBackups(count).onFailure { showError(it) } }
    }

    private suspend fun showError(error: Throwable) {
        _effect.send(BackupEffect.ShowMessage(error.toBackupMessage()))
    }

    private fun sendEffect(effect: BackupEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
