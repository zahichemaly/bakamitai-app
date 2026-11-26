package com.zc.bakamitai.feature.settings.backup.presentation.mapper

import com.zc.bakamitai.common.domain.util.TimeZoneProvider
import com.zc.bakamitai.common.ui.mapper.toDateLabel
import com.zc.bakamitai.common.ui.mapper.toTimeLabel
import com.zc.bakamitai.common.ui.resource.PluralResource
import com.zc.bakamitai.common.ui.resource.StringResource
import com.zc.bakamitai.common.ui.text.UiText
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupPreferences
import com.zc.bakamitai.feature.settings.backup.domain.model.RestoreSummary
import com.zc.bakamitai.feature.settings.backup.presentation.model.BackupFrequencyOption
import com.zc.bakamitai.feature.settings.presentation.mapper.pickerOf
import com.zc.bakamitai.feature.settings.presentation.model.PickerUIState
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Factory
import kotlin.time.Instant

/**
 * The Backup screen's labels and pickers (docs/7-backup.md §2). A class because the last backup's date and time
 * are shown in the device's time zone, read on every call.
 */
@Factory
class BackupUIModelMapper(private val timeZoneProvider: TimeZoneProvider) {

    /** The folder's name, or "Not set". */
    fun folderLabel(folder: BackupFolder?): UiText =
        folder?.name?.takeIf { it.isNotBlank() }?.let { UiText.StringValue(it) }
            ?: UiText.StringResource(StringResource.backup_folder_not_set)

    /** "Last backup: Nov 26, 2026, 10:47 AM", or "No backups yet". */
    fun lastBackupLabel(at: Instant?): UiText {
        if (at == null) return UiText.StringResource(StringResource.backup_never)
        val local = at.toLocalDateTime(timeZoneProvider.current())
        return UiText.StringResource(
            StringResource.backup_last,
            listOf(local.date.toDateLabel(), local.time.toTimeLabel()),
        )
    }

    /** *Automatic backups*: Off, Every 6 hours, Every 12 hours, Daily, Weekly; [frequency] selected. */
    fun frequencyPicker(frequency: BackupFrequency): PickerUIState<BackupFrequencyOption> = pickerOf(
        title = UiText.StringResource(StringResource.backup_frequency_title),
        options = BackupFrequencyOption.entries,
        selected = frequency.toOption(),
        label = BackupFrequencyOption::toLabel,
    )

    /** *Backups to keep*: 1, 2, 3, 5, 10 backups; [count] selected (added in order when it isn't a choice). */
    fun maxBackupsPicker(count: Int): PickerUIState<Int> = pickerOf(
        title = UiText.StringResource(StringResource.backup_max_title),
        options = (BackupPreferences.MAX_BACKUPS_OPTIONS + count).distinct().sorted(),
        selected = count,
        label = { UiText.StringPlural(PluralResource.backup_max_count, it, listOf(it)) },
    )
}

/** "Backup saved to Backups." */
fun BackupFolder.toSavedMessage(): UiText = UiText.StringResource(StringResource.backup_saved, listOf(name))

/** "Restored 12 bookmarks and your settings.", or that no bookmark was new. */
fun RestoreSummary.toRestoredMessage(): UiText = if (bookmarksAdded == 0) {
    UiText.StringResource(StringResource.backup_restored_no_bookmarks)
} else {
    UiText.StringPlural(PluralResource.backup_restored, bookmarksAdded, listOf(bookmarksAdded))
}

/** The domain frequency for an option of the *Automatic backups* picker. */
fun BackupFrequencyOption.toBackupFrequency(): BackupFrequency = when (this) {
    BackupFrequencyOption.OFF -> BackupFrequency.OFF
    BackupFrequencyOption.EVERY_6_HOURS -> BackupFrequency.EVERY_6_HOURS
    BackupFrequencyOption.EVERY_12_HOURS -> BackupFrequency.EVERY_12_HOURS
    BackupFrequencyOption.DAILY -> BackupFrequency.DAILY
    BackupFrequencyOption.WEEKLY -> BackupFrequency.WEEKLY
}

private fun BackupFrequency.toOption(): BackupFrequencyOption = when (this) {
    BackupFrequency.OFF -> BackupFrequencyOption.OFF
    BackupFrequency.EVERY_6_HOURS -> BackupFrequencyOption.EVERY_6_HOURS
    BackupFrequency.EVERY_12_HOURS -> BackupFrequencyOption.EVERY_12_HOURS
    BackupFrequency.DAILY -> BackupFrequencyOption.DAILY
    BackupFrequency.WEEKLY -> BackupFrequencyOption.WEEKLY
}

private fun BackupFrequencyOption.toLabel(): UiText = UiText.StringResource(
    when (this) {
        BackupFrequencyOption.OFF -> StringResource.backup_frequency_off
        BackupFrequencyOption.EVERY_6_HOURS -> StringResource.backup_frequency_6h
        BackupFrequencyOption.EVERY_12_HOURS -> StringResource.backup_frequency_12h
        BackupFrequencyOption.DAILY -> StringResource.backup_frequency_daily
        BackupFrequencyOption.WEEKLY -> StringResource.backup_frequency_weekly
    },
)
