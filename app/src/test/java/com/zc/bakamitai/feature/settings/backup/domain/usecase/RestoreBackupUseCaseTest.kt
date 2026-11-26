package com.zc.bakamitai.feature.settings.backup.domain.usecase

import com.zc.bakamitai.common.domain.repository.FakeSettingsRepository
import com.zc.bakamitai.common.util.FixedClock
import com.zc.bakamitai.feature.bookmarks.domain.BookmarksTestData.bookmark
import com.zc.bakamitai.feature.bookmarks.domain.model.BookmarkSort
import com.zc.bakamitai.feature.bookmarks.domain.repository.FakeBookmarkRepository
import com.zc.bakamitai.feature.bookmarks.domain.repository.FakeBookmarkSortRepository
import com.zc.bakamitai.feature.bookmarks.domain.repository.FakeReleaseCheckScheduler
import com.zc.bakamitai.feature.settings.backup.domain.BackupTestData.NOW
import com.zc.bakamitai.feature.settings.backup.domain.BackupTestData.backup
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupError
import com.zc.bakamitai.feature.settings.backup.domain.model.RestoreSummary
import com.zc.bakamitai.feature.settings.backup.domain.repository.FakeBackupFileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class RestoreBackupUseCaseTest {

    private val files = FakeBackupFileRepository()
    private val settings = FakeSettingsRepository()
    private val sort = FakeBookmarkSortRepository()
    private val bookmarks = FakeBookmarkRepository()
    private val scheduler = FakeReleaseCheckScheduler()
    private val restoredAt = Instant.parse("2026-11-27T09:00:00Z")
    private val useCase = RestoreBackupUseCase(
        files = files,
        settings = settings,
        bookmarkSort = sort,
        bookmarks = bookmarks,
        scheduler = scheduler,
        clock = FixedClock(restoredAt),
    )

    @Test
    fun `merges the bookmarks from now, replaces the settings and requests a check`() = runTest {
        val saved = bookmark("frieren", "2026-11-20T00:00:00Z", title = "Frieren (mine)")
        bookmarks.bookmarks.value = listOf(saved)
        files.files["content://backup"] = backup()

        assertEquals(
            Result.success(RestoreSummary(bookmarksAdded = 1, bookmarksInBackup = 2)),
            useCase("content://backup"),
        )

        assertEquals(listOf(backup().bookmarks to restoredAt), bookmarks.restoredAll)
        assertEquals(listOf(saved, backup().bookmarks[1]), bookmarks.bookmarks.value)
        assertEquals(backup().settings, settings.settings.value)
        assertEquals(BookmarkSort.TITLE, sort.sort.value)
        assertEquals<List<FakeReleaseCheckScheduler.Call>>(
            listOf(FakeReleaseCheckScheduler.Call.RequestCheck),
            scheduler.calls,
        )
    }

    @Test
    fun `restoring the same backup again adds nothing and requests no check`() = runTest {
        files.files["content://backup"] = backup()
        useCase("content://backup")
        scheduler.calls.clear()

        assertEquals(Result.success(RestoreSummary(0, 2)), useCase("content://backup"))
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test
    fun `an unreadable file changes nothing`() = runTest {
        assertEquals(BackupError.InvalidFile(), useCase("content://missing").exceptionOrNull())

        assertTrue(bookmarks.restoredAll.isEmpty())
        assertTrue(settings.writes.isEmpty())
    }

    @Test
    fun `a failed bookmarks write is returned before any setting changes`() = runTest {
        files.files["content://backup"] = backup()
        bookmarks.restoreAllFailure = IllegalStateException("db")

        assertTrue(useCase("content://backup").isFailure)
        assertTrue(settings.writes.isEmpty())
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test
    fun `a failed settings write is returned`() = runTest {
        files.files["content://backup"] = backup()
        settings.failWrites = true

        assertTrue(useCase("content://backup").isFailure)
        assertEquals(BookmarkSort.RECENTLY_ADDED, sort.sort.value)
    }

    @Test
    fun `the created time doesn't matter`() = runTest {
        files.files["content://backup"] = backup().copy(createdAt = NOW)

        assertTrue(useCase("content://backup").isSuccess)
    }
}
