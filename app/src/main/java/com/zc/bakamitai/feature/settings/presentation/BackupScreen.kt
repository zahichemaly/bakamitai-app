package com.zc.bakamitai.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.window.core.layout.WindowSizeClass
import com.zc.bakamitai.common.domain.util.SystemTimeZoneProvider
import com.zc.bakamitai.common.ui.component.skeleton.SkeletonView
import com.zc.bakamitai.common.ui.resource.StringResource
import com.zc.bakamitai.common.ui.text.UiText
import com.zc.bakamitai.common.ui.theme.BakaMitaiTheme
import com.zc.bakamitai.common.ui.util.AdaptivePreviews
import com.zc.bakamitai.common.ui.util.centeredContentPadding
import com.zc.bakamitai.feature.settings.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.domain.model.BackupPreferences
import com.zc.bakamitai.feature.settings.presentation.component.pickerdialog.PickerDialogView
import com.zc.bakamitai.feature.settings.presentation.component.pickerpreference.PickerPreferenceSkeletonView
import com.zc.bakamitai.feature.settings.presentation.component.pickerpreference.PickerPreferenceView
import com.zc.bakamitai.feature.settings.presentation.component.restoredialog.RestoreDialogView
import com.zc.bakamitai.feature.settings.presentation.component.settingssection.SettingsSectionView
import com.zc.bakamitai.feature.settings.presentation.mapper.BackupUIModelMapper
import com.zc.bakamitai.feature.settings.presentation.model.BackupFrequencyOption
import com.zc.bakamitai.feature.settings.presentation.model.BackupPicker
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Instant

/**
 * Backup and restore (docs/7-backup.md §2): the backup folder, a manual backup and restore, then the automatic
 * backups' interval and how many files to keep. Same chrome as Settings: one column, centered on wide windows.
 * While the backup settings load, each card shows its own skeleton.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    state: BackupState,
    onIntent: (BackupIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { BackupTopAppBar(onIntent = onIntent, scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BackupContent(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
        BackupDialogs(state = state, onIntent = onIntent)
    }
}

/** Back and "Backup and restore"; the bar turns `surfaceContainer` once content scrolls under it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupTopAppBar(
    onIntent: (BackupIntent) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    TopAppBar(
        title = {
            Text(
                text = UiText.StringResource(StringResource.backup_title).asString(),
                modifier = Modifier.semantics { heading() },
                color = colors.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = { onIntent(BackupIntent.BackClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = UiText.StringResource(StringResource.cd_navigate_back).asString(),
                    tint = colors.onSurfaceVariant,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.surface,
            scrolledContainerColor = colors.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}

/** The three sections, in a scrolling column at most `MaxContentWidth` wide. */
@Composable
private fun BackupContent(state: BackupState, onIntent: (BackupIntent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = BakaMitaiTheme.spacing
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val margin = if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        spacing.marginTablet
    } else {
        spacing.margin
    }
    BoxWithConstraints(modifier = modifier) {
        val horizontalPadding = centeredContentPadding(maxWidth = maxWidth, margin = margin)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = horizontalPadding, top = spacing.md, end = horizontalPadding, bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            FolderSection(state = state, onIntent = onIntent)
            ManualSection(state = state, onIntent = onIntent)
            AutomaticSection(state = state, onIntent = onIntent)
        }
    }
}

/** The backup folder's row: its name, or "Not set". */
@Composable
private fun FolderSection(state: BackupState, onIntent: (BackupIntent) -> Unit, modifier: Modifier = Modifier) {
    val onFolderClick = remember(onIntent) { { onIntent(BackupIntent.FolderClicked) } }
    BackupSection(
        title = UiText.StringResource(StringResource.backup_section_folder),
        accentColor = MaterialTheme.colorScheme.primary,
        isLoading = state.isLoading,
        skeletonIcons = persistentListOf(Icons.Outlined.Folder),
        modifier = modifier,
    ) {
        state.folderLabel?.let { folder ->
            PickerPreferenceView(
                icon = Icons.Outlined.Folder,
                title = UiText.StringResource(StringResource.backup_folder_title),
                value = folder,
                onClick = onFolderClick,
            )
        }
    }
}

/** *Create backup* (with the last backup's time) and *Restore backup*; each says so while it runs. */
@Composable
private fun ManualSection(state: BackupState, onIntent: (BackupIntent) -> Unit, modifier: Modifier = Modifier) {
    val onCreateClick = remember(onIntent) { { onIntent(BackupIntent.CreateClicked) } }
    val onRestoreClick = remember(onIntent) { { onIntent(BackupIntent.RestoreClicked) } }
    BackupSection(
        title = UiText.StringResource(StringResource.backup_section_manual),
        accentColor = MaterialTheme.colorScheme.secondary,
        isLoading = state.isLoading,
        skeletonIcons = persistentListOf(Icons.Outlined.Backup, Icons.Outlined.SettingsBackupRestore),
        modifier = modifier,
    ) {
        state.lastBackupLabel?.let { lastBackup ->
            PickerPreferenceView(
                icon = Icons.Outlined.Backup,
                title = UiText.StringResource(StringResource.backup_create_title),
                value = if (state.isCreating) {
                    UiText.StringResource(StringResource.backup_create_running)
                } else {
                    lastBackup
                },
                onClick = onCreateClick,
            )
        }
        PickerPreferenceView(
            icon = Icons.Outlined.SettingsBackupRestore,
            title = UiText.StringResource(StringResource.backup_restore_title),
            value = UiText.StringResource(
                if (state.isRestoring) {
                    StringResource.backup_restore_running
                } else {
                    StringResource.backup_restore_description
                },
            ),
            onClick = onRestoreClick,
        )
    }
}

/** *Automatic backups* and *Backups to keep*: picker rows that open their dialogs. */
@Composable
private fun AutomaticSection(state: BackupState, onIntent: (BackupIntent) -> Unit, modifier: Modifier = Modifier) {
    val onFrequencyClick = remember(onIntent) { { onIntent(BackupIntent.PickerClicked(BackupPicker.FREQUENCY)) } }
    val onMaxBackupsClick = remember(onIntent) { { onIntent(BackupIntent.PickerClicked(BackupPicker.MAX_BACKUPS)) } }
    BackupSection(
        title = UiText.StringResource(StringResource.backup_section_automatic),
        accentColor = MaterialTheme.colorScheme.tertiary,
        isLoading = state.isLoading,
        skeletonIcons = persistentListOf(Icons.Outlined.Schedule, Icons.Outlined.Inventory2),
        modifier = modifier,
    ) {
        state.frequency?.let { picker ->
            PickerPreferenceView(
                icon = Icons.Outlined.Schedule,
                title = picker.title,
                value = picker.valueLabel,
                onClick = onFrequencyClick,
            )
        }
        state.maxBackups?.let { picker ->
            PickerPreferenceView(
                icon = Icons.Outlined.Inventory2,
                title = picker.title,
                value = picker.valueLabel,
                onClick = onMaxBackupsClick,
            )
        }
    }
}

/**
 * A section card whose rows are [content], or, while loading, one placeholder row per [skeletonIcons] in its own
 * skeleton.
 */
@Composable
private fun BackupSection(
    title: UiText,
    accentColor: Color,
    isLoading: Boolean,
    skeletonIcons: ImmutableList<ImageVector>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = BakaMitaiTheme.spacing
    SettingsSectionView(title = title, accentColor = accentColor, modifier = modifier, contentPadding = spacing.xs) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            if (isLoading) {
                SkeletonView {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        skeletonIcons.forEach { icon -> PickerPreferenceSkeletonView(icon = icon) }
                    }
                }
            } else {
                content()
            }
        }
    }
}

/** The open picker's dialog, and the restore confirmation while a file waits for it. */
@Composable
private fun BackupDialogs(state: BackupState, onIntent: (BackupIntent) -> Unit) {
    val onPickerDismiss = remember(onIntent) { { onIntent(BackupIntent.PickerDismissed) } }
    val onFrequencySelect = remember(onIntent) {
        { option: BackupFrequencyOption -> onIntent(BackupIntent.FrequencySelected(option)) }
    }
    val onMaxBackupsSelect = remember(onIntent) { { count: Int -> onIntent(BackupIntent.MaxBackupsSelected(count)) } }
    val onRestoreConfirm = remember(onIntent) { { onIntent(BackupIntent.RestoreConfirmed) } }
    val onRestoreDismiss = remember(onIntent) { { onIntent(BackupIntent.RestoreDismissed) } }
    when (state.openPicker) {
        BackupPicker.FREQUENCY -> state.frequency?.let { picker ->
            PickerDialogView(
                title = picker.title,
                options = picker.options,
                onOptionClick = onFrequencySelect,
                onDismiss = onPickerDismiss,
            )
        }

        BackupPicker.MAX_BACKUPS -> state.maxBackups?.let { picker ->
            PickerDialogView(
                title = picker.title,
                options = picker.options,
                onOptionClick = onMaxBackupsSelect,
                onDismiss = onPickerDismiss,
            )
        }

        null -> Unit
    }
    if (state.pendingRestore != null) {
        RestoreDialogView(onConfirm = onRestoreConfirm, onDismiss = onRestoreDismiss)
    }
}

/** Loading; no folder yet; a folder with a backup, daily; creating; the restore confirmation open. */
private class BackupStatePreviewProvider : PreviewParameterProvider<BackupState> {
    private val mapper = BackupUIModelMapper(SystemTimeZoneProvider())
    private val loaded = BackupState(
        isLoading = false,
        folderLabel = mapper.folderLabel(null),
        lastBackupLabel = mapper.lastBackupLabel(null),
        frequency = mapper.frequencyPicker(BackupFrequency.OFF),
        maxBackups = mapper.maxBackupsPicker(BackupPreferences.DEFAULT_MAX_BACKUPS),
    )
    private val withFolder = loaded.copy(
        hasFolder = true,
        folderLabel = mapper.folderLabel(BackupFolder(uri = "content://backups", name = "Backups")),
        lastBackupLabel = mapper.lastBackupLabel(Instant.parse("2026-11-26T10:47:00Z")),
        frequency = mapper.frequencyPicker(BackupFrequency.DAILY),
    )

    override val values = sequenceOf(
        BackupState(),
        loaded,
        withFolder,
        withFolder.copy(isCreating = true),
        withFolder.copy(pendingRestore = "content://backup.json"),
    )
}

@AdaptivePreviews
@Composable
private fun BackupScreenPreview(@PreviewParameter(BackupStatePreviewProvider::class) state: BackupState) {
    BakaMitaiTheme {
        BackupScreen(state = state, onIntent = {})
    }
}
