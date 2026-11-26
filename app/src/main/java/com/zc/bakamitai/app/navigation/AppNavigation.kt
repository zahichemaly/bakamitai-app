package com.zc.bakamitai.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.NavDisplay
import com.zc.bakamitai.common.ui.navigation.Navigator
import com.zc.bakamitai.common.ui.navigation.rememberNavigationState
import com.zc.bakamitai.common.ui.util.ObserveAsEvents
import com.zc.bakamitai.feature.details.presentation.navigation.navigateToDetailsFromStart
import com.zc.bakamitai.feature.home.presentation.navigation.HomeNavKey
import kotlinx.coroutines.flow.Flow

/**
 * The app's navigation, on two levels kept apart:
 * - the **app back stack**, drawn here, full window: [MainNavKey] (the tab shell, [MainShell]) at the bottom, and
 * every pushed screen (Details, Settings) above it, covering the shell, bottom bar or rail included;
 * - the **tabs**, selected in the shell and drawn by its own `NavDisplay`.
 *
 * The tabs' entries are decorated here, above the app-level `NavDisplay`, so each tab keeps its saved state and
 * ViewModels while a pushed screen covers the shell.
 *
 * @param showDeepLinks slugs of shows to open from notifications: each opens the show's Details over Home.
 */
@Composable
fun AppNavigation(showDeepLinks: Flow<String>, modifier: Modifier = Modifier) {
    val tabs = remember { TopLevelDestination.entries.mapNotNull { it.key }.toSet() }
    val navigationState = rememberNavigationState(rootRoute = MainNavKey, startTab = HomeNavKey, tabs = tabs)
    val navigator = remember(navigationState) { Navigator(navigationState) }
    ObserveAsEvents(showDeepLinks, navigator) { slug -> navigator.navigateToDetailsFromStart(slug) }

    val tabEntryProvider = remember(navigator) { tabEntryProvider(navigator) }
    val tabEntries by rememberUpdatedState(navigationState.rememberDecoratedTabEntries(tabEntryProvider))
    val appEntryProvider = remember(navigator) {
        appEntryProvider(navigator) {
            MainShell(
                selectedTab = navigationState.selectedTab,
                tabEntries = navigationState.visibleTabs.map { tab -> tabEntries.getValue(tab) },
                onTabClick = navigator::navigate,
                onBack = navigator::goBack,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    NavDisplay(
        entries = navigationState.rememberDecoratedEntries(appEntryProvider),
        modifier = modifier,
        onBack = navigator::goBack,
    )
}
