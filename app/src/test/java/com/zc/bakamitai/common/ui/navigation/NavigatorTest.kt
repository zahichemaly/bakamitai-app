package com.zc.bakamitai.common.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {

    private data object Shell : NavKey

    private data object Home : NavKey

    private data object Schedule : NavKey

    private data object Settings : NavKey

    private data class Details(val slug: String) : NavKey

    private val state = NavigationState(
        startTab = Home,
        tabs = setOf(Home, Schedule),
        selectedTab = mutableStateOf(Home),
        backStack = NavBackStack(Shell),
    )
    private val navigator = Navigator(state)

    private val backStack: List<NavKey>
        get() = state.backStack.toList()

    @Test
    fun `starts on the tab shell with the start tab`() {
        assertEquals(listOf<NavKey>(Shell), backStack)
        assertEquals(Home, state.selectedTab)
        assertEquals(listOf<NavKey>(Home), state.visibleTabs)
    }

    @Test
    fun `selecting a tab shows it above the start tab and pushes nothing`() {
        navigator.navigate(Schedule)

        assertEquals(Schedule, state.selectedTab)
        assertEquals(listOf(Home, Schedule), state.visibleTabs)
        assertEquals(listOf<NavKey>(Shell), backStack)
    }

    @Test
    fun `another key is pushed on the app back stack and the tab is kept`() {
        navigator.navigate(Schedule)
        navigator.navigate(Details("frieren"))

        assertEquals(listOf(Shell, Details("frieren")), backStack)
        assertEquals(Schedule, state.selectedTab)
    }

    @Test
    fun `pushing the key already on top does nothing`() {
        navigator.navigate(Details("frieren"))
        navigator.navigate(Details("frieren"))

        assertEquals(listOf(Shell, Details("frieren")), backStack)
        navigator.goBack()
        assertEquals(listOf<NavKey>(Shell), backStack)
    }

    @Test
    fun `pushing a different key on top of another still pushes`() {
        navigator.navigate(Details("frieren"))
        navigator.navigate(Settings)
        navigator.navigate(Details("one-piece"))

        assertEquals(listOf(Shell, Details("frieren"), Settings, Details("one-piece")), backStack)
    }

    @Test
    fun `selecting a tab closes every pushed screen`() {
        navigator.navigate(Details("frieren"))
        navigator.navigate(Settings)

        navigator.navigate(Schedule)

        assertEquals(listOf<NavKey>(Shell), backStack)
        assertEquals(Schedule, state.selectedTab)
    }

    @Test
    fun `selecting the selected tab keeps it`() {
        navigator.navigate(Schedule)
        navigator.navigate(Schedule)

        assertEquals(Schedule, state.selectedTab)
        assertEquals(listOf(Home, Schedule), state.visibleTabs)
        assertEquals(listOf<NavKey>(Shell), backStack)
    }

    @Test
    fun `navigating from start closes pushed screens, selects the start tab and pushes the key`() {
        navigator.navigate(Schedule)
        navigator.navigate(Details("dandadan"))
        navigator.navigate(Settings)

        navigator.navigateFromStart(Details("frieren"))

        assertEquals(Home, state.selectedTab)
        assertEquals(listOf(Shell, Details("frieren")), backStack)
    }

    @Test
    fun `navigating from start then back returns to the start tab`() {
        navigator.navigate(Schedule)
        navigator.navigateFromStart(Details("frieren"))

        navigator.goBack()

        assertEquals(listOf<NavKey>(Shell), backStack)
        assertEquals(listOf<NavKey>(Home), state.visibleTabs)
    }

    @Test
    fun `navigating from start to a tab only selects it`() {
        navigator.navigate(Details("frieren"))

        navigator.navigateFromStart(Schedule)

        assertEquals(Schedule, state.selectedTab)
        assertEquals(listOf<NavKey>(Shell), backStack)
    }

    @Test
    fun `back pops the app back stack first and keeps the tab`() {
        navigator.navigate(Schedule)
        navigator.navigate(Details("frieren"))
        navigator.navigate(Settings)

        navigator.goBack()
        assertEquals(listOf(Shell, Details("frieren")), backStack)

        navigator.goBack()
        assertEquals(listOf<NavKey>(Shell), backStack)
        assertEquals(Schedule, state.selectedTab)
    }

    @Test
    fun `back on the tab shell from another tab returns to the start tab`() {
        navigator.navigate(Schedule)

        navigator.goBack()

        assertEquals(Home, state.selectedTab)
        assertEquals(listOf<NavKey>(Home), state.visibleTabs)
    }

    @Test
    fun `back on the tab shell from the start tab does nothing`() {
        navigator.goBack()

        assertEquals(Home, state.selectedTab)
        assertEquals(listOf<NavKey>(Shell), backStack)
    }
}
