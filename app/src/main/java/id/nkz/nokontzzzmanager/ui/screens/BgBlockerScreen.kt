package id.nkz.nokontzzzmanager.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.ui.components.IndeterminateExpressiveLoadingIndicator
import id.nkz.nokontzzzmanager.viewmodel.AppProfilesViewModel
import id.nkz.nokontzzzmanager.viewmodel.MiscViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BgBlockerScreen(
    navController: NavController,
    miscViewModel: MiscViewModel = hiltViewModel(),
    appProfilesViewModel: AppProfilesViewModel = hiltViewModel()
) {
    val bgBlocklist by miscViewModel.bgBlocklist.collectAsStateWithLifecycle()
    val isBgBlockerAvailable by miscViewModel.isBgBlockerAvailable.collectAsStateWithLifecycle()
    val applyOnBoot by miscViewModel.applyBgBlockerOnBoot.collectAsStateWithLifecycle()
    
    val filteredApps by appProfilesViewModel.filteredApps.collectAsStateWithLifecycle()
    val isLoadingApps by appProfilesViewModel.isLoadingApps.collectAsStateWithLifecycle()
    val searchQuery by appProfilesViewModel.searchQuery.collectAsStateWithLifecycle()
    
    val sheetState = rememberModalBottomSheetState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        miscViewModel.loadInitialData()
    }

    val context = LocalContext.current
    
    val blockedPackages = remember(bgBlocklist) {
        if (bgBlocklist.isBlank()) emptyList() else bgBlocklist.split(",")
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.bg_blocker_reset_confirm_title)) },
            text = { Text(stringResource(R.string.bg_blocker_reset_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    miscViewModel.updateBgBlocklist("com.shopee.id,com.lazada.android,com.tokopedia.tkpd")
                    showResetConfirm = false
                }) {
                    Text(stringResource(R.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (isBgBlockerAvailable == true) {
                val canAdd = blockedPackages.size < MiscViewModel.MAX_BG_BLOCKLIST_APPS && 
                             bgBlocklist.length < MiscViewModel.MAX_BG_BLOCKLIST_CHARS - 10 // Warning buffer
                FloatingActionButton(
                    onClick = {
                        if (canAdd) {
                            appProfilesViewModel.loadInstalledApps()
                            showAddDialog = true
                        }
                    },
                    containerColor = if (canAdd) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (canAdd) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.bg_blocker_add_app))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(
                    bottom = padding.calculateBottomPadding(),
                    start = 16.dp,
                    end = 16.dp,
                    top = 0.dp
                )
                .fillMaxSize()
        ) {
            if (isBgBlockerAvailable == false) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                            Text(
                                stringResource(R.string.feature_not_available),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else if (isBgBlockerAvailable == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    IndeterminateExpressiveLoadingIndicator()
                }
            } else {
                // Set on Boot Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    onClick = {
                        miscViewModel.setApplyBgBlockerOnBoot(!applyOnBoot)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.bg_blocker_apply_on_boot),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Switch(
                            checked = applyOnBoot,
                            onCheckedChange = { miscViewModel.setApplyBgBlockerOnBoot(it) },
                            thumbContent = if (applyOnBoot) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            } else {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            }
                        )
                    }
                }

                // Actions Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLimitReached = blockedPackages.size >= MiscViewModel.MAX_BG_BLOCKLIST_APPS || 
                                         bgBlocklist.length >= MiscViewModel.MAX_BG_BLOCKLIST_CHARS - 10
                    Text(
                        text = "${blockedPackages.size}/${MiscViewModel.MAX_BG_BLOCKLIST_APPS} apps",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isLimitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.reset_to_default), style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (blockedPackages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.bg_blocker_no_apps))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(blockedPackages) { index, packageName ->
                            val shape = when {
                                blockedPackages.size == 1 -> RoundedCornerShape(24.dp)
                                index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                                index == blockedPackages.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                else -> RoundedCornerShape(8.dp)
                            }
                            
                            BlockedAppItem(
                                packageName = packageName,
                                onRemove = { pkg ->
                                    val newList = blockedPackages.toMutableList()
                                    newList.remove(pkg)
                                    miscViewModel.updateBgBlocklist(newList.joinToString(","))
                                },
                                shape = shape
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            ModalBottomSheet(
                onDismissRequest = {
                    showAddDialog = false
                    appProfilesViewModel.onSearchQueryChanged("")
                },
                sheetState = sheetState
            ) {
                AppPickerSheet(
                    filteredApps = filteredApps,
                    isLoading = isLoadingApps,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = appProfilesViewModel::onSearchQueryChanged,
                    onDismiss = {
                        showAddDialog = false
                        appProfilesViewModel.onSearchQueryChanged("")
                    },
                    onAppSelected = { appInfo ->
                        if (!blockedPackages.contains(appInfo.packageName)) {
                            val newList = blockedPackages.toMutableList()
                            newList.add(appInfo.packageName)
                            miscViewModel.updateBgBlocklist(newList.joinToString(","))
                        }
                        showAddDialog = false
                        appProfilesViewModel.onSearchQueryChanged("")
                    }
                )
            }
        }
    }
}

@Composable
fun BlockedAppItem(
    packageName: String,
    onRemove: (String) -> Unit,
    shape: RoundedCornerShape
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val appInfo = remember(packageName) {
        try {
            val info = pm.getApplicationInfo(packageName, 0)
            val label = info.loadLabel(pm).toString()
            val icon = info.loadIcon(pm)
            Triple(label, icon, true)
        } catch (e: Exception) {
            Triple(packageName, null, false)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (appInfo.second != null) {
                Image(
                    painter = rememberDrawablePainter(appInfo.second),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.first,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (appInfo.third) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!appInfo.third) {
                    Text(
                        text = stringResource(R.string.bg_blocker_not_installed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            IconButton(onClick = { onRemove(packageName) }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
