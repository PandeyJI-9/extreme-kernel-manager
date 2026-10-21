package id.nkz.nokontzzzmanager.ui.screens

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.AppProfileEntity
import id.nkz.nokontzzzmanager.data.model.CpuProfileConfig
import id.nkz.nokontzzzmanager.data.model.GpuProfileConfig
import id.nkz.nokontzzzmanager.ui.dialog.CpuTuningDialog
import id.nkz.nokontzzzmanager.ui.dialog.GpuTuningDialog
import id.nkz.nokontzzzmanager.ui.components.IndeterminateExpressiveLoadingIndicator
import id.nkz.nokontzzzmanager.viewmodel.AppInfo
import id.nkz.nokontzzzmanager.viewmodel.AppProfilesViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun AppPickerSheet(
    filteredApps: List<AppInfo>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onAppSelected: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 600.dp)
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AppRegistration,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = stringResource(R.string.app_profiles_select_app),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            placeholder = { Text(stringResource(R.string.app_profiles_search_apps)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.app_profiles_clear_search))
                    }
                }
            } else null,
            singleLine = true
        )

        // App List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IndeterminateExpressiveLoadingIndicator()
            }
        } else {
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.app_profiles_no_apps_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(filteredApps) { index, app ->
                        val shape = when {
                            filteredApps.size == 1 -> RoundedCornerShape(24.dp) // Single item
                            index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp) // First item
                            index == filteredApps.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp) // Last item
                            else -> RoundedCornerShape(8.dp) // Middle items
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = shape
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val context = LocalContext.current
                                val icon = remember(app.packageName) {
                                    try {
                                        context.packageManager.getApplicationIcon(app.packageName)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (icon != null) {
                                    Image(
                                        painter = rememberDrawablePainter(icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.appName, style = MaterialTheme.typography.bodyLarge)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

        }

        // Close Button
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.app_profiles_close))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppProfileConfigDialog(
    profile: AppProfileEntity,
    isKgslFeatureAvailable: Boolean,
    isAvoidDirtyPteAvailable: Boolean,
    isPowersaveAvailable: Boolean,
    onDismiss: () -> Unit,
    onSave: (AppProfileEntity) -> Unit,
    viewModel: AppProfilesViewModel = hiltViewModel()
) {
    var performanceMode by remember { mutableStateOf(profile.performanceMode) }
    var kgslSkipZeroing by remember { mutableStateOf(profile.kgslSkipZeroing) }
    var bypassCharging by remember { mutableStateOf(profile.bypassCharging) }
    var allowDirtyPte by remember { mutableStateOf(profile.allowDirtyPte) }
    var isEnabled by remember { mutableStateOf(profile.isEnabled) }
    
    // CPU Tuning State
    var showCpuTuningDialog by remember { mutableStateOf(false) }
    var cpuConfig by remember { mutableStateOf(profile.getCpuConfig()) }

    // GPU Tuning State
    var showGpuTuningDialog by remember { mutableStateOf(false) }
    var gpuConfig by remember { mutableStateOf(profile.getGpuConfig()) }

    // Thermal Tuning State
    var showThermalDialog by remember { mutableStateOf(false) }
    var thermalProfile by remember { mutableStateOf(profile.thermalProfile) }
    val availableThermalProfiles = viewModel.availableThermalProfiles

    val hasCustomTuning = remember(cpuConfig, gpuConfig, thermalProfile) {
        val customCpu = cpuConfig.clusterConfigs.values.any { 
            !it.governor.isNullOrBlank() || it.minFreq != null || it.maxFreq != null 
        } || cpuConfig.coreOnlineStatus.isNotEmpty()
        
        val customGpu = gpuConfig.governor != null || 
                       gpuConfig.minFreq != null || 
                       gpuConfig.maxFreq != null || 
                       gpuConfig.powerLevel != null || 
                       gpuConfig.throttlingEnabled != null
        
        val customThermal = thermalProfile != null
                       
        customCpu || customGpu || customThermal
    }

    val options = remember(isPowersaveAvailable) {
        if (isPowersaveAvailable) {
            listOf("Powersave", "Balanced", "Performance")
        } else {
            listOf("Balanced", "Performance")
        }
    }

    if (showCpuTuningDialog) {
        CpuTuningDialog(
            appName = profile.appName,
            initialConfig = cpuConfig,
            viewModel = viewModel,
            onDismiss = { showCpuTuningDialog = false },
            onSave = { newConfig ->
                cpuConfig = newConfig
                showCpuTuningDialog = false
            }
        )
    }

    if (showGpuTuningDialog) {
        GpuTuningDialog(
            appName = profile.appName,
            initialConfig = gpuConfig,
            viewModel = viewModel,
            onDismiss = { showGpuTuningDialog = false },
            onSave = { newConfig ->
                gpuConfig = newConfig
                showGpuTuningDialog = false
            }
        )
    }

    if (showThermalDialog) {
        // Prepare list with "Default" option
        val dialogItems = remember(availableThermalProfiles) {
            val defaultOption = id.nkz.nokontzzzmanager.data.repository.ThermalRepository.ThermalProfile(
                displayName = "Default", // Will be replaced by string resource in itemLabel
                index = -999 // Special index for Default (null)
            )
            listOf(defaultOption) + availableThermalProfiles
        }

        id.nkz.nokontzzzmanager.ui.dialog.SelectionDialog(
            title = stringResource(R.string.app_profiles_thermal_tuning),
            subtitle = stringResource(R.string.select_thermal_profile),
            items = dialogItems,
            selectedItem = dialogItems.find { it.index == (thermalProfile ?: -999) },
            itemLabel = { profile -> 
                if (profile.index == -999) stringResource(R.string.app_profiles_default) else profile.displayName 
            },
            onItemSelected = { selected ->
                thermalProfile = if (selected.index == -999) null else selected.index
                showThermalDialog = false
            },
            onDismiss = { showThermalDialog = false }
        )
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(min = 200.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = profile.appName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Content
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Enable/Disable Toggle
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.app_profiles_enable_profile),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { 
                                        isEnabled = it
                                        if (!it) {
                                            // Reset to defaults when disabled
                                            performanceMode = "Balanced"
                                            kgslSkipZeroing = false
                                            bypassCharging = false
                                            allowDirtyPte = false
                                            cpuConfig = id.nkz.nokontzzzmanager.data.model.CpuProfileConfig()
                                            gpuConfig = id.nkz.nokontzzzmanager.data.model.GpuProfileConfig()
                                            thermalProfile = null
                                        }
                                    },
                                    thumbContent = if (isEnabled) {
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

                        // Performance Mode
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.app_profiles_performance_mode),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                    if (hasCustomTuning) {
                                        Text(
                                            text = stringResource(R.string.app_profiles_custom_tuning_active),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                                        )
                                    }
                                }
                                
                                val selectedText = when {
                                    hasCustomTuning -> stringResource(R.string.app_profiles_custom)
                                    performanceMode == "Powersave" -> stringResource(R.string.app_profiles_powersave)
                                    performanceMode == "Balanced" -> stringResource(R.string.app_profiles_balanced)
                                    else -> stringResource(R.string.app_profiles_performance)
                                }
                                
                                AnimatedContent(targetState = selectedText, label = "modeLabel") { text ->
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (hasCustomTuning) {
                                            if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                                        } else {
                                            if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                options.forEachIndexed { index, option ->
                                    val isSelected = !hasCustomTuning && performanceMode == option
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { performanceMode = option },
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        enabled = isEnabled && !hasCustomTuning,
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = when (option) {
                                                "Powersave" -> Icons.Default.BatterySaver
                                                "Balanced" -> Icons.Default.Balance
                                                else -> Icons.Default.Speed
                                            },
                                            contentDescription = option,
                                            modifier = Modifier.size(24.dp)
                                        )
        }
    }
}

                        }

                        // Tuning Placeholders
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // CPU Tuning
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isEnabled) { showCpuTuningDialog = true },
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Memory, 
                                            contentDescription = null, 
                                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                        Column {
                                            Text(
                                                text = stringResource(R.string.app_profiles_cpu_tuning),
                                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                                        contentDescription = null, 
                                        tint = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                            }

                            // GPU Tuning
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isEnabled) { showGpuTuningDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeveloperBoard, 
                                            contentDescription = null, 
                                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                        Text(
                                            text = stringResource(R.string.app_profiles_gpu_tuning),
                                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                                        contentDescription = null, 
                                        tint = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                            }

                            // Thermal Tuning
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isEnabled) { showThermalDialog = true },
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Thermostat, 
                                            contentDescription = null, 
                                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                        Column {
                                            Text(
                                                text = stringResource(R.string.app_profiles_thermal_tuning),
                                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                            val currentProfileName = remember(thermalProfile, availableThermalProfiles) {
                                                if (thermalProfile == null) null
                                                else availableThermalProfiles.find { it.index == thermalProfile }?.displayName
                                            }
                                            Text(
                                                text = currentProfileName ?: stringResource(R.string.app_profiles_default),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                                        contentDescription = null, 
                                        tint = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                            }
                        }

                        // KGSL, Dirty PTE, and Bypass Charging
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // KGSL
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.app_profiles_kgsl_skip_zeroing),
                                        color = if (isEnabled) {
                                            if (isKgslFeatureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        }
                                    )
                                    Switch(
                                        checked = kgslSkipZeroing && isKgslFeatureAvailable,
                                        onCheckedChange = { kgslSkipZeroing = it },
                                        enabled = isKgslFeatureAvailable && isEnabled,
                                        thumbContent = if (kgslSkipZeroing && isKgslFeatureAvailable) {
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

                            // Allow Dirty PTE
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.app_profiles_allow_dirty_pte),
                                        color = if (isEnabled) {
                                            if (isAvoidDirtyPteAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        }
                                    )
                                    Switch(
                                        checked = allowDirtyPte && isAvoidDirtyPteAvailable,
                                        onCheckedChange = { allowDirtyPte = it },
                                        enabled = isAvoidDirtyPteAvailable && isEnabled,
                                        thumbContent = if (allowDirtyPte && isAvoidDirtyPteAvailable) {
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

                            // Bypass Charging
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.app_profiles_bypass_charging),
                                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                    Switch(
                                        checked = bypassCharging,
                                        onCheckedChange = { bypassCharging = it },
                                        enabled = isEnabled,
                                        thumbContent = if (bypassCharging) {
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
                        }
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.app_profiles_close))
                        }
                        Button(
                            onClick = {
                                onSave(
                                    profile.copy(
                                        performanceMode = performanceMode,
                                        kgslSkipZeroing = kgslSkipZeroing,
                                        bypassCharging = bypassCharging,
                                        allowDirtyPte = allowDirtyPte,
                                        cpuConfigJson = Json.encodeToString(cpuConfig),
                                        gpuConfigJson = Json.encodeToString(gpuConfig),
                                        thermalProfile = thermalProfile,
                                        isEnabled = isEnabled
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.app_profiles_save))
                        }
                    }
                }
            }
        }
    }
}
