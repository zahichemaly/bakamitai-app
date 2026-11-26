package com.zc.bakamitai.feature.settings.backup.data.repository

import com.zc.bakamitai.feature.settings.backup.data.local.BackupDocument
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** The backup file names and which old backups go (BackupFileRepositoryImpl's pure helpers). */
class BackupFileNamesTest {

    @Test
    fun `a backup is named after its local creation time`() {
        val createdAt = Instant.parse("2026-11-26T08:04:05Z")

        assertEquals("bakamitai_2026-11-26_08-04-05.json", backupFileName(createdAt, TimeZone.UTC))
        assertEquals("bakamitai_2026-11-26_10-04-05.json", backupFileName(createdAt, TimeZone.of("Asia/Beirut")))
    }

    @Test
    fun `only the oldest BakaMitai backups past the count are deleted`() {
        val files = listOf(
            "bakamitai_2026-11-24_10-00-00.json",
            "bakamitai_2026-11-26_10-00-00.json",
            "notes.txt",
            "bakamitai_2026-11-25_10-00-00.json",
            "bakamitai_2026-11-23_10-00-00 (1).json",
            "bakamitai_2026-11-22_10-00-00.json",
        ).map { BackupDocument(name = it, uri = "content://$it") }

        assertEquals(
            listOf("bakamitai_2026-11-24_10-00-00.json", "bakamitai_2026-11-22_10-00-00.json"),
            files.backupsToDelete(keep = 2).map { it.name },
        )
        assertEquals(emptyList(), files.backupsToDelete(keep = 10))
    }
}
