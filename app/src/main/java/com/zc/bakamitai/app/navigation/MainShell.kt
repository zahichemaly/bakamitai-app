package com.zc.bakamitai.app.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay

private const val NAV_INDICATOR_ALPHA = 0.2f

/**
 * The tab shell: the navigation suite (bottom bar on compact widths or heights, rail otherwise) around the tabs'
 * own `NavDisplay`. It's the bottom entry of the app back stack, so a pushed screen (Details, Settings) covers it
 * whole, bar or rail included: the bar is never hidden or shown. A tab whose feature isn't built yet is shown
 * disabled.
 *
 * @param selectedTab the selected tab's key.
 * @param tabEntries the tabs to show, bottom to top: the start tab, then the selected tab if it differs.
 * @param onTabClick selects a tab, by its key.
 * @param onBack goes back from a tab other than the start tab, to the start tab.
 */
@Composable
fun MainShell(
    selectedTab: NavKey,
    tabEntries: List<NavEntry<NavKey>>,
    onTabClick: (NavKey) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors = navigationSuiteItemColors()
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val key = destination.key
                val selected = key != null && key == selectedTab
                item(
                    selected = selected,
                    onClick = { key?.let(onTabClick) },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    enabled = key != null,
                    label = {
                        Text(text = destination.label.asString(), style = MaterialTheme.typography.labelMedium)
                    },
                    colors = itemColors,
                )
            }
        },
        modifier = modifier,
        layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo()),
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        NavDisplay(entries = tabEntries, onBack = onBack)
    }
}

/** Selected: primary icon and label on a translucent primary indicator. Unselected: on-surface-variant. */
@Composable
private fun navigationSuiteItemColors(): NavigationSuiteItemColors {
    val colors = MaterialTheme.colorScheme
    val indicator = colors.primary.copy(alpha = NAV_INDICATOR_ALPHA)
    return NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.primary,
            selectedTextColor = colors.primary,
            indicatorColor = indicator,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = colors.primary,
            selectedTextColor = colors.primary,
            indicatorColor = indicator,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedIconColor = colors.primary,
            selectedTextColor = colors.primary,
            selectedContainerColor = indicator,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        ),
    )
}
