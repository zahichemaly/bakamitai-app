package com.zc.bakamitai.feature.settings.domain.usecase

import com.zc.bakamitai.feature.settings.domain.BackupTestData.folder
import com.zc.bakamitai.feature.settings.domain.model.BackupError
import com.zc.bakamitai.feature.settings.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.domain.model.BackupPreferences
import com.zc.bakamitai.feature.settings.domain.repository.FakeBackupFileRepository
import com.zc.bakamitai.feature.settings.domain.repository.FakeBackupPreferencesRepository
import com.zc.bakamitai.feature.settings.domain.repository.FakeBackupScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The pass-through backup settings use cases: folder, frequency, count and observation. */
class BackupSettingsUseCasesTest {

    private val preferences = FakeBackupPreferencesRepository()
    private val files = FakeBackupFileRepository()
    private val scheduler = FakeBackupScheduler()

    @Test
    fun `a chosen folder is granted, replacing the previous one, and saved`() = runTest {
        preferences.preferences.value = BackupPreferences(folder = folder)
        files.folderNames["content://tree/new"] = "New"

        val result = SetBackupFolderUseCase(preferences, files)("content://tree/new")

        val expected = BackupFolder(uri = "content://tree/new", name = "New")
        assertEquals(Result.success(expected), result)
        assertEquals(listOf<Pair<String, BackupFolder?>>("content://tree/new" to folder), files.granted)
        assertEquals(expected, preferences.preferences.value.folder)
    }

    @Test
    fun `a folder that can't be granted isn't saved`() = runTest {
        files.grantFailure = BackupError.FolderUnavailable()

        assertTrue(SetBackupFolderUseCase(preferences, files)("content://tree/new").isFailure)
        assertNull(preferences.preferences.value.folder)
    }

    @Test
    fun `a frequency is saved, then scheduled`() = runTest {
        val useCase = UpdateBackupFrequencyUseCase(preferences, scheduler)

        assertEquals(Result.success(Unit), useCase(BackupFrequency.DAILY))
        assertEquals(Result.success(Unit), useCase(BackupFrequency.OFF))

        assertEquals(BackupFrequency.OFF, preferences.preferences.value.frequency)
        assertEquals(listOf(BackupFrequency.DAILY, BackupFrequency.OFF), scheduler.scheduled)
    }

    @Test
    fun `a frequency that can't be saved isn't scheduled`() = runTest {
        preferences.failWrites = true

        assertTrue(UpdateBackupFrequencyUseCase(preferences, scheduler)(BackupFrequency.WEEKLY).isFailure)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `the count is saved, and must be positive`() = runTest {
        val useCase = UpdateMaxBackupsUseCase(preferences)

        assertEquals(Result.success(Unit), useCase(10))
        assertEquals(10, ObserveBackupPreferencesUseCase(preferences)().first().maxBackups)
        assertFailsWith<IllegalArgumentException> { useCase(0) }
    }
}
