package com.zc.bakamitai.feature.settings.data.mapper

import com.zc.bakamitai.common.data.network.appJson
import com.zc.bakamitai.common.domain.model.AppSettings
import com.zc.bakamitai.feature.bookmarks.domain.model.BookmarkSort
import com.zc.bakamitai.feature.settings.data.dto.BackupDTO
import com.zc.bakamitai.feature.settings.domain.BackupTestData.NOW
import com.zc.bakamitai.feature.settings.domain.BackupTestData.backup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class BackupDTOMapperTest {

    @Test
    fun `a backup survives a JSON round trip`() {
        val text = appJson.encodeToString(BackupDTO.serializer(), backup().toDTO())

        assertEquals(backup(), appJson.decodeFromString(BackupDTO.serializer(), text).toDomain())
    }

    @Test
    fun `the written file uses the documented keys and the current format version`() {
        val text = appJson.encodeToString(BackupDTO.serializer(), backup().toDTO())

        listOf(
            "\"version\":1",
            "\"created_at\":${NOW.toEpochMilliseconds()}",
            "\"week_start\":\"SUNDAY\"",
            "\"bookmarks_sort\":\"TITLE\"",
            "\"slug\":\"frieren\"",
        ).forEach { key -> assertTrue(key in text, "$key missing from $text") }
    }

    @Test
    fun `missing and unknown settings read as the defaults`() {
        val dto = appJson.decodeFromString(
            BackupDTO.serializer(),
            """{"version":1,"created_at":0,"settings":{"week_start":"TUESDAY","bookmarks_sort":"RATING"}}""",
        )

        val backup = dto.toDomain()

        assertEquals(AppSettings(), backup.settings)
        assertEquals(BookmarkSort.RECENTLY_ADDED, backup.bookmarkSort)
        assertEquals(emptyList(), backup.bookmarks)
        assertEquals("", backup.appVersion)
    }

    @Test
    fun `bookmarks without a slug or title are dropped, a repeated slug kept once, a missing date is the backup's`() {
        val dto = appJson.decodeFromString(
            BackupDTO.serializer(),
            """
            {"version":1,"created_at":1000,"bookmarks":[
              {"slug":" ","title":"Blank"},
              {"slug":"no-title"},
              {"slug":"frieren","title":"Frieren","cover_url":"","extra":true},
              {"slug":"frieren","title":"Frieren again","added_at":5}
            ],"future_field":{}}
            """.trimIndent(),
        )

        val bookmarks = dto.toDomain().bookmarks

        assertEquals(1, bookmarks.size)
        assertEquals("Frieren", bookmarks.single().show.title)
        assertEquals(null, bookmarks.single().show.coverUrl)
        assertEquals(Instant.fromEpochMilliseconds(1000), bookmarks.single().addedAt)
    }
}
