package com.zc.bakamitai.feature.settings.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.zc.bakamitai.common.ui.navigation.Navigator
import com.zc.bakamitai.feature.settings.presentation.BackupRoute

/** Opens Backup and restore over Settings. */
fun Navigator.navigateToBackup() = navigate(BackupNavKey)

/**
 * Registers Backup and restore.
 *
 * @param onNavigateBack leaves it: the back arrow.
 */
fun EntryProviderScope<NavKey>.backupEntry(onNavigateBack: () -> Unit) {
    entry<BackupNavKey> {
        BackupRoute(onNavigateBack = onNavigateBack)
    }
}
