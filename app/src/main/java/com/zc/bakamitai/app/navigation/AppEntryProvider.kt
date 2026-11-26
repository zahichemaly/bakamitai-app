package com.zc.bakamitai.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.zc.bakamitai.common.ui.navigation.Navigator
import com.zc.bakamitai.feature.bookmarks.presentation.navigation.bookmarksEntry
import com.zc.bakamitai.feature.details.presentation.navigation.detailsEntry
import com.zc.bakamitai.feature.details.presentation.navigation.navigateToDetails
import com.zc.bakamitai.feature.home.presentation.navigation.homeEntry
import com.zc.bakamitai.feature.schedule.presentation.navigation.navigateToSchedule
import com.zc.bakamitai.feature.schedule.presentation.navigation.scheduleEntry
import com.zc.bakamitai.feature.settings.presentation.navigation.backupEntry
import com.zc.bakamitai.feature.settings.presentation.navigation.navigateToBackup
import com.zc.bakamitai.feature.settings.presentation.navigation.navigateToSettings
import com.zc.bakamitai.feature.settings.presentation.navigation.settingsEntry
import com.zc.bakamitai.feature.shows.presentation.navigation.allShowsEntry

/*
 * The only place that wires features together, with the navigator's helpers. Where a screen goes is decided here,
 * not by its key: a tab in tabEntryProvider (plus a TopLevelDestination), any other screen in appEntryProvider.
 */

/**
 * The app back stack's entries: the tab shell ([mainContent], under [MainNavKey]) and every screen pushed over it,
 * which covers the shell, bottom bar or rail included.
 */
fun appEntryProvider(navigator: Navigator, mainContent: @Composable () -> Unit): (NavKey) -> NavEntry<NavKey> =
    entryProvider {
        entry<MainNavKey> { mainContent() }
        detailsEntry(onNavigateBack = navigator::goBack)
        settingsEntry(onNavigateBack = navigator::goBack, onOpenBackup = navigator::navigateToBackup)
        backupEntry(onNavigateBack = navigator::goBack)
    }

/** The tabs' entries ([TopLevelDestination]), shown inside the tab shell. */
fun tabEntryProvider(navigator: Navigator): (NavKey) -> NavEntry<NavKey> = entryProvider {
    homeEntry(onOpenDetails = navigator::navigateToDetails, onOpenSettings = navigator::navigateToSettings)
    scheduleEntry(onOpenDetails = navigator::navigateToDetails, onOpenSettings = navigator::navigateToSettings)
    allShowsEntry(onOpenDetails = navigator::navigateToDetails, onOpenSettings = navigator::navigateToSettings)
    bookmarksEntry(
        onOpenDetails = navigator::navigateToDetails,
        onDiscoverShows = navigator::navigateToSchedule,
        onOpenSettings = navigator::navigateToSettings,
    )
}
