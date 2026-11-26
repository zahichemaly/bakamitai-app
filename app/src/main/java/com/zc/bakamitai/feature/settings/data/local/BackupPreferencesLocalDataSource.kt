package com.zc.bakamitai.feature.settings.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zc.bakamitai.common.data.local.enumOrNull
import com.zc.bakamitai.common.data.local.safeData
import com.zc.bakamitai.feature.settings.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.domain.model.BackupPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * The backup settings in the app's Preferences DataStore (docs/7-backup.md §4.2). A missing or unknown value reads
 * as the default; reads never fail; writes throw `IOException`.
 */
@Single
class BackupPreferencesLocalDataSource(private val dataStore: DataStore<Preferences>) {

    val preferences: Flow<BackupPreferences> = dataStore.safeData()
        .map { it.toBackupPreferences() }
        .distinctUntilChanged()

    suspend fun setFolder(folder: BackupFolder) {
        dataStore.edit {
            it[FOLDER_URI] = folder.uri
            it[FOLDER_NAME] = folder.name
        }
    }

    suspend fun setFrequency(frequency: BackupFrequency) {
        dataStore.edit { it[FREQUENCY] = frequency.name }
    }

    suspend fun setMaxBackups(count: Int) {
        dataStore.edit { it[MAX_COUNT] = count }
    }

    suspend fun setLastBackupAt(at: Instant) {
        dataStore.edit { it[LAST_AT] = at.toEpochMilliseconds() }
    }

    private fun Preferences.toBackupPreferences(): BackupPreferences {
        val defaults = BackupPreferences()
        val folderUri = this[FOLDER_URI]
        return BackupPreferences(
            folder = folderUri?.let { BackupFolder(uri = it, name = this[FOLDER_NAME].orEmpty()) },
            frequency = enumOrNull<BackupFrequency>(FREQUENCY) ?: defaults.frequency,
            maxBackups = this[MAX_COUNT]?.takeIf { it > 0 } ?: defaults.maxBackups,
            lastBackupAt = this[LAST_AT]?.let(Instant::fromEpochMilliseconds),
        )
    }

    private companion object {
        val FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        val FOLDER_NAME = stringPreferencesKey("backup_folder_name")
        val FREQUENCY = stringPreferencesKey("backup_frequency")
        val MAX_COUNT = intPreferencesKey("backup_max_count")
        val LAST_AT = longPreferencesKey("backup_last_at")
    }
}
