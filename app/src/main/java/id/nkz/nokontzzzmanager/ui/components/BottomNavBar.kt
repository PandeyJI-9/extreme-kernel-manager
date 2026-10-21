package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController, items: List<String>, isAmoledMode: Boolean) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val containerColor = if (isAmoledMode) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = containerColor
    ) {
        val routes = listOf("home", "tuning", "misc")
        items.forEachIndexed { index, screen ->
            val route = routes.getOrElse(index) { "" }
            val selected = currentRoute == route
            val (filledIcon, outlinedIcon) = getNavIcons(route)
            val iconToDisplay = if (selected) filledIcon else outlinedIcon

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != route) {
                        // Pop any sub-screens of the target route before navigating
                        // to ensure we land on the root of that tab.
                        navController.popBackStack(route, inclusive = false)

                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        // Reselection: if already on the tab, pop back to its root
                        navController.popBackStack(route, inclusive = false)
                    }
                },
                icon = { Icon(imageVector = iconToDisplay, contentDescription = screen) },
                label = {
                    Text(
                        screen,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

// Helper function to get filled and outlined icons for each screen.
private fun getNavIcons(route: String): Pair<ImageVector, ImageVector> {
    return when (route) {
        "home" -> Pair(Icons.Filled.Home, Icons.Outlined.Home)
        "tuning" -> Pair(Icons.Filled.Build, Icons.Outlined.Build)
        "misc" -> Pair(Icons.Filled.Extension, Icons.Outlined.Extension)
        else -> Pair(Icons.AutoMirrored.Filled.Help, Icons.AutoMirrored.Outlined.HelpOutline)
    }
}