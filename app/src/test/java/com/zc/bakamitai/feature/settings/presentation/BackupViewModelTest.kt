package com.zc.bakamitai.feature.settings.presentation

import app.cash.turbine.test
import com.zc.bakamitai.common.domain.repository.FakeSettingsRepository
import com.zc.bakamitai.common.domain.util.FixedTimeZoneProvider
import com.zc.bakamitai.common.ui.resource.PluralResource
import com.zc.bakamitai.common.ui.resource.StringResource
import com.zc.bakamitai.common.ui.text.UiText
import com.zc.bakamitai.common.util.FixedClock
import com.zc.bakamitai.common.util.MainDispatcherRule
import com.zc.bakamitai.feature.bookmarks.domain.repository.FakeBookmarkRepository
import com.zc.bakamitai.feature.bookmarks.domain.repository.FakeBookmarkSortRepository
import com.zc.bakamitai.feature.bookmarks.domain.repository.FakeReleaseCheckScheduler
import com.zc.bakamitai.feature.settings.domain.BackupTestData.NOW
import com.zc.bakamitai.feature.settings.domain.BackupTestData.backup
import com.zc.bakamitai.feature.settings.domain.BackupTestData.folder
import com.zc.bakamitai.feature.settings.domain.model.BackupError
import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.domain.model.BackupPreferences
import com.zc.bakamitai.feature.settings.domain.repository.FakeAppInfoRepository
import com.zc.bakamitai.feature.settings.domain.repository.FakeBackupFileRepository
import com.zc.bakamitai.feature.settings.domain.repository.FakeBackupPreferencesRepository
import com.zc.bakamitai.feature.settings.domain.repository.FakeBackupScheduler
import com.zc.bakamitai.feature.settings.domain.usecase.CreateBackupUseCase
import com.zc.bakamitai.feature.settings.domain.usecase.ObserveBackupPreferencesUseCase
import com.zc.bakamitai.feature.settings.domain.usecase.RestoreBackupUseCase
import com.zc.bakamitai.feature.settings.domain.usecase.SetBackupFolderUseCase
import com.zc.bakamitai.feature.settings.domain.usecase.UpdateBackupFrequencyUseCase
import com.zc.bakamitai.feature.settings.domain.usecase.UpdateMaxBackupsUseCase
import com.zc.bakamitai.feature.settings.presentation.mapper.BackupUIModelMapper
import com.zc.bakamitai.feature.settings.presentation.model.BackupFrequencyOption
import com.zc.bakamitai.feature.settings.presentation.model.BackupPicker
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeBackupPreferencesRepository()
    private val files = FakeBackupFileRepository()
    private val settings = FakeSettingsRepository()
    private val sort = FakeBookmarkSortRepository()
    private val bookmarks = FakeBookmarkRepository()
    private val backupScheduler = FakeBackupScheduler()
    private val clock = FixedClock(NOW)
    private val mapper = BackupUIModelMapper(FixedTimeZoneProvider(TimeZone.UTC))

    private fun createViewModel() = BackupViewModel(
        observeBackupPreferences = ObserveBackupPreferencesUseCase(preferences),
        setBackupFolder = SetBackupFolderUseCase(preferences, files),
        createBackup = CreateBackupUseCase(
            preferences = preferences,
            files = files,
            settings = settings,
            bookmarkSort = sort,
            bookmarks = bookmarks,
            appInfo = FakeAppInfoRepository(),
            clock = clock,
        ),
        restoreBackup = RestoreBackupUseCase(files, settings, sort, bookmarks, FakeReleaseCheckScheduler(), clock),
        updateBackupFrequency = UpdateBackupFrequencyUseCase(preferences, backupScheduler),
        updateMaxBackups = UpdateMaxBackupsUseCase(preferences),
        mapper = mapper,
    )

    /** Keeps the `WhileSubscribed` state collected, so `state.value` follows every update. */
    private fun TestScope.subscribedViewModel(): BackupViewModel = createViewModel().also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        runCurrent()
    }

    @Test
    fun `starts loading, then shows the stored backup settings`() = runTest {
        preferences.preferences.value = BackupPreferences(folder = folder, frequency = BackupFrequency.DAILY)

        createViewModel().state.test {
            assertEquals(BackupState(), awaitItem())
            assertEquals(
                BackupState(
                    isLoading = false,
                    hasFolder = true,
                    folderLabel = UiText.StringValue("Backups"),
                    lastBackupLabel = UiText.StringResource(StringResource.backup_never),
                    frequency = mapper.frequencyPicker(BackupFrequency.DAILY),
                    maxBackups = mapper.maxBackupsPicker(BackupPreferences.DEFAULT_MAX_BACKUPS),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `create with a folder writes a backup and says where`() = runTest {
        preferences.preferences.value = BackupPreferences(folder = folder)
        val viewModel = subscribedViewModel()

        viewModel.effect.test {
            viewModel.onIntent(BackupIntent.CreateClicked)
            assertEquals(
                BackupEffect.ShowMessage(UiText.StringResource(StringResource.backup_saved, listOf("Backups"))),
                awaitItem(),
            )
        }
        runCurrent()
        assertEquals(1, files.written.size)
        assertFalse(viewModel.state.value.isCreating)
        assertEquals(mapper.lastBackupLabel(NOW), viewModel.state.value.lastBackupLabel)
    }

    @Test
    fun `create without a folder asks for one, then creates once it's chosen`() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.effect.test {
            viewModel.onIntent(BackupIntent.CreateClicked)
            assertEquals(BackupEffect.OpenFolderPicker, awaitItem())
            assertTrue(files.written.isEmpty())

            viewModel.onIntent(BackupIntent.FolderChosen("content://tree/backups"))
            assertEquals(
                BackupEffect.ShowMessage(UiText.StringResource(StringResource.backup_saved, listOf("Backups"))),
                awaitItem(),
            )
        }
        assertEquals("content://tree/backups", preferences.preferences.value.folder?.uri)
        assertEquals(1, files.written.size)
    }

    @Test
    fun `a cancelled folder pick creates nothing, even after a later pick`() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.effect.test {
            viewModel.onIntent(BackupIntent.CreateClicked)
            assertEquals(BackupEffect.OpenFolderPicker, awaitItem())
            viewModel.onIntent(BackupIntent.FolderChosen(null))
            viewModel.onIntent(BackupIntent.FolderChosen("content://tree/backups"))
            runCurrent()
            expectNoEvents()
        }
        assertTrue(files.written.isEmpty())
    }

    @Test
    fun `a failed backup shows why`() = runTest {
        preferences.preferences.value = BackupPreferences(folder = folder)
        files.writeFailure = BackupError.FolderUnavailable()
        val viewModel = subscribedViewModel()

        viewModel.effect.test {
            viewModel.onIntent(BackupIntent.CreateClicked)
            assertEquals(
                BackupEffect.ShowMessage(UiText.StringResource(StringResource.backup_error_folder)),
                awaitItem(),
            )
        }
    }

    @Test
    fun `restore opens the file picker, then asks, then restores`() = runTest {
        files.files["content://backup"] = backup()
        val viewModel = subscribedViewModel()

        viewModel.effect.test {
            viewModel.onIntent(BackupIntent.RestoreClicked)
            assertEquals(BackupEffect.OpenFilePicker, awaitItem())

            viewModel.onIntent(BackupIntent.RestoreFileChosen("content://backup"))
            runCurrent()
            assertEquals("content://backup", viewModel.state.value.pendingRestore)
            assertTrue(bookmarks.restoredAll.isEmpty())

            viewModel.onIntent(BackupIntent.RestoreConfirmed)
            assertEquals(
                BackupEffect.ShowMessage(UiText.StringPlural(PluralResource.backup_restored, 2, listOf(2))),
                awaitItem(),
            )
        }
        runCurrent()
        assertNull(viewModel.state.value.pendingRestore)
        assertFalse(viewModel.state.value.isRestoring)
        assertEquals(backup().settings, settings.settings.value)
    }

    @Test
    fun `a dismissed confirmation or a cancelled file pick restores nothing`() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.onIntent(BackupIntent.RestoreFileChosen(null))
        assertNull(viewModel.state.value.pendingRestore)
        viewModel.onIntent(BackupIntent.RestoreFileChosen("content://backup"))
        viewModel.onIntent(BackupIntent.RestoreDismissed)
        viewModel.onIntent(BackupIntent.RestoreConfirmed)
        runCurrent()

        assertNull(viewModel.state.value.pendingRestore)
        assertTrue(bookmarks.restoredAll.isEmpty())
    }

    @Test
    fun `an invalid file says so`() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.effect.test {
            viewModel.onIntent(BackupIntent.RestoreFileChosen("content://not-a-backup"))
            viewModel.onIntent(BackupIntent.RestoreConfirmed)
            assertEquals(
                BackupEffect.ShowMessage(UiText.StringResource(StringResource.backup_error_invalid_file)),
                awaitItem(),
            )
        }
    }

    @Test
    fun `pickers open, save and close`() = runTest {
        preferences.preferences.value = BackupPreferences(folder = folder)
        val viewModel = subscribedViewModel()

        viewModel.onIntent(BackupIntent.PickerClicked(BackupPicker.FREQUENCY))
        runCurrent()
        assertEquals(BackupPicker.FREQUENCY, viewModel.state.value.openPicker)
        viewModel.onIntent(BackupIntent.FrequencySelected(BackupFrequencyOption.WEEKLY))
        runCurrent()
        assertNull(viewModel.state.value.openPicker)
        assertEquals(listOf(BackupFrequency.WEEKLY), backupScheduler.scheduled)

        viewModel.onIntent(BackupIntent.PickerClicked(BackupPicker.MAX_BACKUPS))
        viewModel.onIntent(BackupIntent.MaxBackupsSelected(10))
        runCurrent()
        assertNull(viewModel.state.value.openPicker)
        assertEquals(10, preferences.preferences.value.maxBackups)

        viewModel.onIntent(BackupIntent.PickerClicked(BackupPicker.MAX_BACKUPS))
        viewModel.onIntent(BackupIntent.PickerDismissed)
        runCurrent()
        assertNull(viewModel.state.value.openPicker)
    }

    @Test
    fun `turning automatic backups on without a folder asks for one`() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.effect.test {
            viewModel.onIntent(BackupIntent.FrequencySelected(BackupFrequencyOption.DAILY))
            assertEquals(BackupEffect.OpenFolderPicker, awaitItem())
            viewModel.onIntent(BackupIntent.FrequencySelected(BackupFrequencyOption.OFF))
            expectNoEvents()
        }
    }

    @Test
    fun `back and the folder row emit their effects`() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.effect.test {
            viewModel.onIntent(BackupIntent.FolderClicked)
            assertEquals(BackupEffect.OpenFolderPicker, awaitItem())
            viewModel.onIntent(BackupIntent.BackClicked)
            assertEquals(BackupEffect.NavigateBack, awaitItem())
        }
    }
}
