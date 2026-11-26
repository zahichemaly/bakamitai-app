package com.zc.bakamitai.feature.settings.backup.data.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zc.bakamitai.common.data.local.FailingPreferencesDataStore
import com.zc.bakamitai.common.data.local.InMemoryPreferencesDataStore
import com.zc.bakamitai.feature.settings.backup.domain.BackupTestData.NOW
import com.zc.bakamitai.feature.settings.backup.domain.BackupTestData.folder
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupFrequency
import com.zc.bakamitai.feature.settings.backup.domain.model.BackupPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BackupPreferencesLocalDataSourceTest {

    private val dataStore = InMemoryPreferencesDataStore()
    private val dataSource = BackupPreferencesLocalDataSource(dataStore)

    @Test
    fun `nothing saved reads as the defaults`() = runTest {
        assertEquals(BackupPreferences(), dataSource.preferences.first())
    }

    @Test
    fun `every setter round-trips`() = runTest {
        dataSource.setFolder(folder)
        dataSource.setFrequency(BackupFrequency.WEEKLY)
        dataSource.setMaxBackups(5)
        dataSource.setLastBackupAt(NOW)

        assertEquals(
            BackupPreferences(folder = folder, frequency = BackupFrequency.WEEKLY, maxBackups = 5, lastBackupAt = NOW),
            dataSource.preferences.first(),
        )
    }

    @Test
    fun `an unknown frequency or a non-positive count reads as the default`() = runTest {
        dataStore.edit {
            it[stringPreferencesKey("backup_frequency")] = "HOURLY"
            it[intPreferencesKey("backup_max_count")] = 0
        }

        assertEquals(BackupPreferences(), dataSource.preferences.first())
    }

    @Test
    fun `an unreadable file reads as the defaults, and a write throws`() = runTest {
        val failing = BackupPreferencesLocalDataSource(FailingPreferencesDataStore())

        assertEquals(BackupPreferences(), failing.preferences.first())
        assertFailsWith<IOException> { failing.setFrequency(BackupFrequency.DAILY) }
    }
}
