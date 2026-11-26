package com.zc.bakamitai.common.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles forward and back navigation by updating [state]. Lives in `app/navigation/`; features never see it,
 * they get navigation lambdas built from it in `AppEntryProvider.kt`.
 */
class Navigator(val state: NavigationState) {

    /**
     * For a tab: closes every pushed screen and selects the tab. For any other key: pushes it on the app back stack,
     * over the tab shell, unless it's already on top (a double tap opens one screen).
     */
    fun navigate(key: NavKey) {
        if (key in state.tabs) {
            popToTabShell()
            state.selectedTab = key
        } else if (state.backStack.lastOrNull() != key) {
            state.backStack.add(key)
        }
    }

    /**
     * Opens [key] over the start tab (a notification tap): closes every pushed screen, selects the start tab and
     * pushes [key]. A tab [key] is only selected, as with [navigate].
     */
    fun navigateFromStart(key: NavKey) {
        if (key in state.tabs) {
            navigate(key)
            return
        }
        popToTabShell()
        state.selectedTab = state.startTab
        state.backStack.add(key)
    }

    /**
     * Pops the app back stack. With only the tab shell left, a tab other than the start tab returns to the start tab
     * ("exit through the start tab"). On the start tab, does nothing: no `NavDisplay` handles Back there, so the
     * system leaves the app.
     */
    fun goBack() {
        when {
            state.backStack.size > 1 -> state.backStack.removeAt(state.backStack.lastIndex)
            state.selectedTab != state.startTab -> state.selectedTab = state.startTab
        }
    }

    private fun popToTabShell() {
        while (state.backStack.size > 1) state.backStack.removeAt(state.backStack.lastIndex)
    }
}
