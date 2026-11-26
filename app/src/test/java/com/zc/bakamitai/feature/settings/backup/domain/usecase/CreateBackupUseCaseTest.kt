package com.zc.bakamitai.feature.settings.backup.domain.usecase

import com.zc.bakamitai.common.domain.model.AppSettings
import com.zc.bakamitai.common.domain.model.DomainError
import com.zc.bakamitai.common.domain.model.ThemeMode
import com.zc.bakamitai.common.domain.repository.FakeSettingsRepository
import com.zc.bakamitai.common.util.FixedClock
import com.zc.bakamitai.feature.bookmarks.domain.BookmarksTestData.bookmark
import com.zc.bakamitai.feature.bookmarks.domain.BookmarksTestData.mondayNoon
import com.zc.bakamitai.feature.bookmarks.domain.model.BookmarkSort
import com.zc.bakamitai.feature.bookmarks.domain.repository.FakeBookmarkRepository
import com.zc.bakamitai.feature.bookmarks.domain.repository.FakeBookmarkSortRepository
import com.zc.bakamitai.feature.settings.backup.domain.BackupTestData.NOW
import com.zc.bakamitai.feature.settings.backup.domain.BackupTestData.folder
import com.zc.bakamitai.feature.settings.backup.domain.model.Backup
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupError
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupPreferences
import com.zc.bakamitai.feature.settings.backup.domain.repository.FakeBackupFileRepository
import com.zc.bakamitai.feature.settings.backup.domain.repository.FakeBackupPreferencesRepository
import com.zc.bakamitai.feature.settings.domain.repository.FakeAppInfoRepository
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateBackupUseCaseTest {

    private val preferences = FakeBackupPreferencesRepository(BackupPreferences(folder = folder, maxBackups = 5))
    private val files = FakeBackupFileRepository()
    private val settings = FakeSettingsRepository(AppSettings(themeMode = ThemeMode.DARK))
    private val sort = FakeBookmarkSortRepository(BookmarkSort.NEXT_AIRING)
    private val bookmarks = FakeBookmarkRepository()
    private val useCase = CreateBackupUseCase(
        preferences = preferences,
        files = files,
        settings = settings,
        bookmarkSort = sort,
        bookmarks = bookmarks,
        appInfo = FakeAppInfoRepository(),
        clock = FixedClock(NOW),
    )

    @Test
    fun `writes the settings, the sort and every bookmark without its slot, then records and prunes`() = runTest {
        val frieren = bookmark("frieren", "2026-11-10T00:00:00Z", airing = mondayNoon())
        bookmarks.bookmarks.value = listOf(frieren)

        assertEquals(Result.success(folder), useCase())

        val expected = Backup(
            createdAt = NOW,
            appVersion = "1.0",
            settings = AppSettings(themeMode = ThemeMode.DARK),
            bookmarkSort = BookmarkSort.NEXT_AIRING,
            bookmarks = listOf(frieren.copy(airing = null)),
        )
        assertEquals(listOf(folder to expected), files.written)
        assertEquals(NOW, preferences.preferences.value.lastBackupAt)
        assertEquals(listOf(folder to 5), files.pruned)
    }

    @Test
    fun `without a folder it fails with NoFolder and writes nothing`() = runTest {
        preferences.preferences.value = BackupPreferences()

        assertEquals(BackupError.NoFolder, useCase().exceptionOrNull())
        assertTrue(files.written.isEmpty())
    }

    @Test
    fun `a failed write is returned, and nothing is recorded or pruned`() = runTest {
        files.writeFailure = BackupError.FolderUnavailable()

        assertEquals(BackupError.FolderUnavailable(), useCase().exceptionOrNull())
        assertNull(preferences.preferences.value.lastBackupAt)
        assertTrue(files.pruned.isEmpty())
    }

    @Test
    fun `a bookmarks read failure is returned`() = runTest {
        val error = DomainError.Unknown(IOException("disk"))
        bookmarks.getFailure = error

        assertEquals(error, useCase().exceptionOrNull())
        assertTrue(files.written.isEmpty())
    }

    @Test
    fun `a failed prune doesn't fail the backup`() = runTest {
        files.pruneFailure = BackupError.FolderUnavailable()

        assertEquals(Result.success(folder), useCase())
    }
}
