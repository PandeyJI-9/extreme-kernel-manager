package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import id.nkz.nokontzzzmanager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedTopAppBar(
    title: String,
    navController: NavController? = null,
    showSettingsIcon: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    isAmoledMode: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val containerColor = MaterialTheme.colorScheme.surface
    val scrolledContainerColor = if (isAmoledMode) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            if (!showSettingsIcon && navController?.previousBackStackEntry != null) {
                FilledTonalIconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
        actions = {
            actions()
            if (showSettingsIcon && navController != null) {
                IconButton(onClick = { 
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    val routeWithArgs = if (currentRoute != null) {
                        "settings?fromScreen=$currentRoute"
                    } else {
                        "settings"
                    }
                    navController.navigate(routeWithArgs)
                }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(id = R.string.home_settings_button_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = scrolledContainerColor,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        scrollBehavior = scrollBehavior
    )
}
