package com.zc.bakamitai.common.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/**
 * Creates the app's [NavigationState], kept across configuration changes and process death.
 *
 * @param rootRoute the key at the bottom of the app back stack: the tab shell.
 * @param startTab the tab the app opens on and exits through; must be in [tabs].
 * @param tabs one key per tab (top-level destination).
 */
@Composable
fun rememberNavigationState(rootRoute: NavKey, startTab: NavKey, tabs: Set<NavKey>): NavigationState {
    val backStack = rememberNavBackStack(rootRoute)
    val selectedTab = rememberSerializable(
        startTab,
        tabs,
        serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
        mutableStateOf(startTab)
    }

    return remember(backStack, selectedTab, startTab, tabs) {
        NavigationState(startTab = startTab, tabs = tabs, selectedTab = selectedTab, backStack = backStack)
    }
}

/**
 * The app's navigation, on two levels kept apart:
 * - the **app back stack** ([backStack]): the tab shell at the bottom, and every pushed screen above it, covering
 * the shell (its navigation bar or rail included);
 * - the **tabs**, shown inside the shell: [selectedTab], one of [tabs]. They are only selected, never pushed onto,
 * and follow the "exit through the start tab" pattern.
 *
 * Only [Navigator] changes it.
 *
 * @param startTab the tab the user exits the app through.
 * @param tabs one key per tab.
 * @param selectedTab the state backing the selected tab.
 * @param backStack the app back stack, the tab shell's key at the bottom.
 */
class NavigationState(
    val startTab: NavKey,
    val tabs: Set<NavKey>,
    selectedTab: MutableState<NavKey>,
    val backStack: NavBackStack<NavKey>,
) {

    /** The selected tab. */
    var selectedTab: NavKey by selectedTab

    /** The tabs the shell shows, bottom to top: the start tab, then the selected tab if it differs. */
    val visibleTabs: List<NavKey>
        get() = if (selectedTab == startTab) listOf(startTab) else listOf(startTab, selectedTab)

    /** The app back stack's entries for the app-level `NavDisplay`, each with its own saved state and ViewModels. */
    @Composable
    fun rememberDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> =
        rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = rememberEntryDecorators(),
            entryProvider = entryProvider,
        )

    /**
     * Every tab's entry, by tab key. Each tab has its own decorators and is never popped, so it keeps its saved
     * state and ViewModels for the app's lifetime, whichever tab or pushed screen is shown.
     *
     * Call it above the app-level `NavDisplay`: decorators created inside the tab shell would be cleared whenever a
     * pushed screen covers it (the shell then leaves the composition).
     */
    @Composable
    fun rememberDecoratedTabEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): Map<NavKey, NavEntry<NavKey>> =
        tabs.associateWith { tab ->
            key(tab) {
                rememberDecoratedNavEntries(
                    backStack = listOf(tab),
                    entryDecorators = rememberEntryDecorators(),
                    entryProvider = entryProvider,
                ).single()
            }
        }
}

/** Saved state and a ViewModel store per entry, both cleared when the entry is popped. */
@Composable
private fun rememberEntryDecorators(): List<NavEntryDecorator<NavKey>> = listOf(
    rememberSaveableStateHolderNavEntryDecorator(),
    rememberViewModelStoreNavEntryDecorator(),
)
