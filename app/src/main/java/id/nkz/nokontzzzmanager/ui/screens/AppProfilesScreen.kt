package id.nkz.nokontzzzmanager.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.AppProfileEntity
import id.nkz.nokontzzzmanager.viewmodel.AppInfo
import id.nkz.nokontzzzmanager.viewmodel.AppProfilesViewModel
import id.nkz.nokontzzzmanager.ui.dialog.CpuTuningDialog
import id.nkz.nokontzzzmanager.data.model.CpuProfileConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import id.nkz.nokontzzzmanager.ui.dialog.GpuTuningDialog
import id.nkz.nokontzzzmanager.data.model.GpuProfileConfig

import id.nkz.nokontzzzmanager.ui.components.IndeterminateExpressiveLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfilesScreen(
    navController: NavController,
    viewModel: AppProfilesViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isKgslFeatureAvailable by viewModel.isKgslFeatureAvailable.collectAsStateWithLifecycle()
    val isAvoidDirtyPteAvailable by viewModel.isAvoidDirtyPteAvailable.collectAsStateWithLifecycle()
    val isPowersaveAvailable by viewModel.isPowersaveAvailable.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<AppProfileEntity?>(null) }
    var showPermissionDialog by remember { mutableStateOf(!viewModel.hasUsageStatsPermission()) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPermission = viewModel.hasUsageStatsPermission()
                showPermissionDialog = !hasPermission
                if (hasPermission) {
                    viewModel.toggleService(true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        floatingActionButton = {
            if (!showPermissionDialog) {
                FloatingActionButton(
                    onClick = {
                        viewModel.loadInstalledApps()
                        showAddDialog = true
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.app_profiles_add_profile))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(
                    bottom = padding.calculateBottomPadding(),
                    start = padding.calculateStartPadding(LocalLayoutDirection.current),
                    end = padding.calculateEndPadding(LocalLayoutDirection.current),
                    top = 0.dp
                )
                .fillMaxSize()
        ) {
            if (showPermissionDialog) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning, 
                                contentDescription = null, 
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                stringResource(R.string.app_profiles_permission_required),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else when (val list = profiles) {
                null -> Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    IndeterminateExpressiveLoadingIndicator()
                }
                else -> when {
                    list.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.app_profiles_no_profiles))
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(list) { index, profile ->
                            val shape = when {
                                list.size == 1 -> RoundedCornerShape(24.dp)
                                index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                                index == list.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                else -> RoundedCornerShape(8.dp)
                            }
                            AppProfileItem(
                                profile = profile,
                                onEdit = { profileToEdit = it },
                                onDelete = { viewModel.deleteProfile(it) },
                                cardShape = shape
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
                    viewModel.onSearchQueryChanged("") // Reset search when closed
                },
                sheetState = sheetState
            ) {
                AppPickerSheet(
                    filteredApps = filteredApps,
                    isLoading = isLoadingApps,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onDismiss = {
                        showAddDialog = false
                        viewModel.onSearchQueryChanged("") // Reset search when closed
                    },
                    onAppSelected = { appInfo ->
                        viewModel.addProfile(appInfo)
                        showAddDialog = false
                        viewModel.onSearchQueryChanged("") // Reset search
                    }
                )
            }
        }

        profileToEdit?.let { profile ->
            AppProfileConfigDialog(
                profile = profile,
                isKgslFeatureAvailable = isKgslFeatureAvailable == true,
                isAvoidDirtyPteAvailable = isAvoidDirtyPteAvailable == true,
                isPowersaveAvailable = isPowersaveAvailable,
                onDismiss = { profileToEdit = null },
                onSave = { updatedProfile ->
                    viewModel.updateProfile(updatedProfile)
                    profileToEdit = null
                }
            )
        }
    }
}

@Composable
fun AppProfileItem(
    profile: AppProfileEntity,
    onEdit: (AppProfileEntity) -> Unit,
    onDelete: (AppProfileEntity) -> Unit,
    cardShape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    val context = LocalContext.current
    // Load icon helper
    val icon = remember(profile.packageName) {
        try {
            context.packageManager.getApplicationIcon(profile.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(profile) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = cardShape
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (icon != null) {
                Image(
                    painter = rememberDrawablePainter(icon),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(modifier = Modifier.size(48.dp)) // Placeholder
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.appName, style = MaterialTheme.typography.titleMedium)
                
                val summary = remember(profile) {
                    val parts = mutableListOf<String>()
                    
                    // 1. Performance Mode
                    parts.add(profile.performanceMode)
                    
                    // 2. KGSL (Only if ON)
                    if (profile.kgslSkipZeroing) {
                        parts.add("KGSL")
                    }
                    
                    // 3. Bypass (Only if ON)
                    if (profile.bypassCharging) {
                        parts.add("Bypass")
                    }
                    
                    // 4. Dirty PTE (Only if ON)
                    if (profile.allowDirtyPte) {
                        parts.add("Dirty PTE")
                    }
                    
                    // 5. CPU Tuning Indicator
                    val cpuConfig = profile.getCpuConfig()
                    if (cpuConfig.clusterConfigs.isNotEmpty() || cpuConfig.coreOnlineStatus.isNotEmpty()) {
                        parts.add("CPU")
                    }
                    
                    // 6. GPU Tuning Indicator
                    val gpuConfig = profile.getGpuConfig()
                    if (gpuConfig.governor != null || gpuConfig.minFreq != null || gpuConfig.maxFreq != null || gpuConfig.powerLevel != null || gpuConfig.throttlingEnabled != null) {
                        parts.add("GPU")
                    }
                    
                    // 7. Thermal Tuning Indicator
                    if (profile.thermalProfile != null) {
                        parts.add("Thermal")
                    }
                    
                    parts.joinToString(" • ")
                }

                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = { onDelete(profile) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.app_profiles_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}