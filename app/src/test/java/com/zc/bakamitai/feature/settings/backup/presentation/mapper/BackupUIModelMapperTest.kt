package com.zc.bakamitai.feature.settings.backup.presentation.mapper

import com.zc.bakamitai.common.domain.model.DomainError
import com.zc.bakamitai.common.domain.util.FixedTimeZoneProvider
import com.zc.bakamitai.common.ui.resource.PluralResource
import com.zc.bakamitai.common.ui.resource.StringResource
import com.zc.bakamitai.common.ui.text.UiText
import com.zc.bakamitai.feature.settings.backup.domain.BackupTestData.folder
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupError
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.backup.domain.model.RestoreSummary
import com.zc.bakamitai.feature.settings.backup.presentation.model.BackupFrequencyOption
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class BackupUIModelMapperTest {

    private val mapper = BackupUIModelMapper(FixedTimeZoneProvider(TimeZone.of("Asia/Beirut")))

    @Test
    fun `the folder shows its name, or Not set`() {
        assertEquals(UiText.StringValue("Backups"), mapper.folderLabel(folder))
        assertEquals(UiText.StringResource(StringResource.backup_folder_not_set), mapper.folderLabel(null))
        assertEquals(
            UiText.StringResource(StringResource.backup_folder_not_set),
            mapper.folderLabel(BackupFolder(uri = "content://tree/x", name = " ")),
        )
    }

    @Test
    fun `the last backup is shown in the device's zone, or No backups yet`() {
        assertEquals(
            UiText.StringResource(
                StringResource.backup_last,
                listOf(UiText.StringValue("Nov 26, 2026"), UiText.TimeValue(LocalTime(0, 30))),
            ),
            mapper.lastBackupLabel(Instant.parse("2026-11-25T22:30:00Z")),
        )
        assertEquals(UiText.StringResource(StringResource.backup_never), mapper.lastBackupLabel(null))
    }

    @Test
    fun `the frequency picker lists every option with the stored one selected`() {
        val picker = mapper.frequencyPicker(BackupFrequency.DAILY)

        assertEquals(BackupFrequencyOption.entries, picker.options.map { it.option })
        assertEquals(listOf(BackupFrequencyOption.DAILY), picker.options.filter { it.isSelected }.map { it.option })
        assertEquals(UiText.StringResource(StringResource.backup_frequency_daily), picker.valueLabel)
        BackupFrequencyOption.entries.forEach { option ->
            assertEquals(option.name, option.toBackupFrequency().name)
        }
    }

    @Test
    fun `the count picker offers the fixed choices, plus a stored count that isn't one`() {
        assertEquals(listOf(1, 2, 3, 5, 10), mapper.maxBackupsPicker(3).options.map { it.option })
        assertEquals(listOf(1, 2, 3, 4, 5, 10), mapper.maxBackupsPicker(4).options.map { it.option })
        assertEquals(
            UiText.StringPlural(PluralResource.backup_max_count, 5, listOf(5)),
            mapper.maxBackupsPicker(5).valueLabel,
        )
    }

    @Test
    fun `messages for a saved backup and a restore`() {
        assertEquals(UiText.StringResource(StringResource.backup_saved, listOf("Backups")), folder.toSavedMessage())
        assertEquals(
            UiText.StringPlural(PluralResource.backup_restored, 2, listOf(2)),
            RestoreSummary(bookmarksAdded = 2, bookmarksInBackup = 3).toRestoredMessage(),
        )
        assertEquals(
            UiText.StringResource(StringResource.backup_restored_no_bookmarks),
            RestoreSummary(bookmarksAdded = 0, bookmarksInBackup = 3).toRestoredMessage(),
        )
    }

    @Test
    fun `each backup error has its own message, anything else the generic one`() {
        assertEquals(
            UiText.StringResource(StringResource.backup_error_no_folder),
            BackupError.NoFolder.toBackupMessage(),
        )
        assertEquals(
            UiText.StringResource(StringResource.backup_error_folder),
            BackupError.FolderUnavailable().toBackupMessage(),
        )
        assertEquals(
            UiText.StringResource(StringResource.backup_error_invalid_file),
            BackupError.InvalidFile().toBackupMessage(),
        )
        assertEquals(
            UiText.StringResource(StringResource.backup_error_version),
            BackupError.UnsupportedVersion.toBackupMessage(),
        )
        assertEquals(
            UiText.StringResource(StringResource.error_generic),
            DomainError.Unknown(IOException("disk")).toBackupMessage(),
        )
    }
}
