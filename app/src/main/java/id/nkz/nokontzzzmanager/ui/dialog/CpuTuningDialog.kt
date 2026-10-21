package id.nkz.nokontzzzmanager.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.model.ClusterConfig
import id.nkz.nokontzzzmanager.data.model.CpuProfileConfig
import id.nkz.nokontzzzmanager.viewmodel.AppProfilesViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private sealed interface CpuTuningDialogState {
    data class Governor(val cluster: String) : CpuTuningDialogState
    data class MinFreq(val cluster: String) : CpuTuningDialogState
    data class MaxFreq(val cluster: String) : CpuTuningDialogState
    data object CoreStatus : CpuTuningDialogState
    data object None : CpuTuningDialogState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuTuningDialog(
    appName: String,
    initialConfig: CpuProfileConfig,
    viewModel: AppProfilesViewModel,
    onDismiss: () -> Unit,
    onSave: (CpuProfileConfig) -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }
    val clusters = viewModel.cpuClusters
    val coreStates by viewModel.coreStates.collectAsStateWithLifecycle()

    var activeDialog: CpuTuningDialogState by remember { mutableStateOf(CpuTuningDialogState.None) }
    var showResetFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(showResetFeedback) {
        if (showResetFeedback) {
            kotlinx.coroutines.delay(1500)
            showResetFeedback = false
        }
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
                    .heightIn(min = 300.dp, max = 650.dp),
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
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_profiles_cpu_tuning),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Content
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp) // 2dp spacing as requested
                    ) {
                        itemsIndexed(clusters) { index, clusterName ->
                            val clusterConfig = config.clusterConfigs[clusterName] ?: ClusterConfig()
                            
                            // Shape logic for main cluster cards
                            val shape = when {
                                index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                                else -> RoundedCornerShape(8.dp)
                            }

                            CpuProfileClusterCard(
                                clusterName = clusterName,
                                config = clusterConfig,
                                shape = shape,
                                onGovernorClick = { activeDialog = CpuTuningDialogState.Governor(clusterName) },
                                onMinFrequencyClick = { activeDialog = CpuTuningDialogState.MinFreq(clusterName) },
                                onMaxFrequencyClick = { activeDialog = CpuTuningDialogState.MaxFreq(clusterName) }
                            )
                        }
                        
                        item {
                            // Core Status
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeDialog = CpuTuningDialogState.CoreStatus }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.GridView,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = stringResource(R.string.core_status),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (config.coreOnlineStatus.isNotEmpty()) {
                                                Text(
                                                    text = stringResource(R.string.app_profiles_cores_override, config.coreOnlineStatus.size),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        item {
                            // Reset to Default
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (showResetFeedback) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                ),
                                onClick = { 
                                    config = CpuProfileConfig()
                                    showResetFeedback = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    androidx.compose.animation.AnimatedContent(
                                        targetState = showResetFeedback,
                                        label = "resetFeedback"
                                    ) { isSuccess ->
                                        if (isSuccess) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.reset_to_default),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (showResetFeedback) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
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
                            onClick = { onSave(config) },
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

    // Sub-dialogs
    when (val dialog = activeDialog) {
        is CpuTuningDialogState.Governor -> {
            val cluster = dialog.cluster
            val availableGovernors by viewModel.getAvailableCpuGovernors(cluster).collectAsStateWithLifecycle(initialValue = emptyList())
            val current = config.clusterConfigs[cluster]?.governor ?: ""

            SelectionDialog(
                title = stringResource(R.string.cpu_governor_label),
                subtitle = cluster,
                items = availableGovernors,
                selectedItem = current,
                itemLabel = { it },
                onItemSelected = { selected ->
                    val currentConfig = config.clusterConfigs[cluster] ?: ClusterConfig()
                    val newMap = config.clusterConfigs.toMutableMap()
                    newMap[cluster] = currentConfig.copy(governor = selected)
                    config = config.copy(clusterConfigs = newMap)
                    activeDialog = CpuTuningDialogState.None
                },
                onDismiss = { activeDialog = CpuTuningDialogState.None }
            )
        }

        is CpuTuningDialogState.MinFreq -> {
            val cluster = dialog.cluster
            val availableFreqs by viewModel.getAvailableCpuFrequencies(cluster).collectAsStateWithLifecycle(initialValue = emptyList())
            val current = config.clusterConfigs[cluster]?.minFreq

            SelectionDialog(
                title = stringResource(R.string.set_min_frequency),
                subtitle = cluster,
                items = availableFreqs,
                selectedItem = current,
                itemLabel = { stringResource(R.string.app_profiles_mhz_suffix, it / 1000) },
                onItemSelected = { selected ->
                    val currentConfig = config.clusterConfigs[cluster] ?: ClusterConfig()
                    val newMap = config.clusterConfigs.toMutableMap()
                    newMap[cluster] = currentConfig.copy(minFreq = selected)
                    config = config.copy(clusterConfigs = newMap)
                    activeDialog = CpuTuningDialogState.None
                },
                onDismiss = { activeDialog = CpuTuningDialogState.None }
            )
        }

        is CpuTuningDialogState.MaxFreq -> {
            val cluster = dialog.cluster
            val availableFreqs by viewModel.getAvailableCpuFrequencies(cluster).collectAsStateWithLifecycle(initialValue = emptyList())
            val current = config.clusterConfigs[cluster]?.maxFreq

            SelectionDialog(
                title = stringResource(R.string.set_max_frequency),
                subtitle = cluster,
                items = availableFreqs,
                selectedItem = current,
                itemLabel = { stringResource(R.string.app_profiles_mhz_suffix, it / 1000) },
                onItemSelected = { selected ->
                    val currentConfig = config.clusterConfigs[cluster] ?: ClusterConfig()
                    val newMap = config.clusterConfigs.toMutableMap()
                    newMap[cluster] = currentConfig.copy(maxFreq = selected)
                    config = config.copy(clusterConfigs = newMap)
                    activeDialog = CpuTuningDialogState.None
                },
                onDismiss = { activeDialog = CpuTuningDialogState.None }
            )
        }

        is CpuTuningDialogState.CoreStatus -> CoreConfigDialog(
            coreStates = coreStates,
            configuredStates = config.coreOnlineStatus,
            onCoreToggled = { index, enabled ->
                val newMap = config.coreOnlineStatus.toMutableMap()
                newMap[index] = enabled
                config = config.copy(coreOnlineStatus = newMap)
            },
            onDismiss = { activeDialog = CpuTuningDialogState.None }
        )

        is CpuTuningDialogState.None -> { /* no dialog */ }
    }
}

@Composable
fun CpuProfileClusterCard(
    clusterName: String,
    config: ClusterConfig,
    shape: RoundedCornerShape,
    onGovernorClick: () -> Unit,
    onMinFrequencyClick: () -> Unit,
    onMaxFrequencyClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "DropdownRotation"
    )

    val displayClusterName = when (clusterName) {
        "cpu0" -> stringResource(id = R.string.little_cluster)
        "cpu4" -> stringResource(id = R.string.big_cluster)
        "cpu7" -> stringResource(id = R.string.prime_cluster)
        else -> clusterName.uppercase()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = displayClusterName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = rotationAngle },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp) // 2dp spacing inside dropdown
                ) {
                    CpuProfileSettingItem(
                        icon = Icons.Default.Tune,
                        title = stringResource(R.string.cpu_governor_label),
                        value = config.governor ?: stringResource(R.string.default_governor_desc, stringResource(R.string.app_profiles_default)),
                        onClick = onGovernorClick,
                        isSet = config.governor != null,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    )
                    CpuProfileSettingItem(
                        icon = Icons.Default.Speed,
                        title = stringResource(R.string.min_frequency),
                        value = config.minFreq?.let { stringResource(R.string.app_profiles_mhz_suffix, it / 1000) } ?: stringResource(R.string.app_profiles_default),
                        onClick = onMinFrequencyClick,
                        isSet = config.minFreq != null,
                        shape = RoundedCornerShape(4.dp)
                    )
                    CpuProfileSettingItem(
                        icon = Icons.Default.Speed,
                        title = stringResource(R.string.max_frequency),
                        value = config.maxFreq?.let { stringResource(R.string.app_profiles_mhz_suffix, it / 1000) } ?: stringResource(R.string.app_profiles_default),
                        onClick = onMaxFrequencyClick,
                        isSet = config.maxFreq != null,
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CpuProfileSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    isSet: Boolean,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSet) FontWeight.Bold else FontWeight.Normal),
                    color = if (isSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionDialog(
    title: String,
    subtitle: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: @Composable (T) -> String,
    onItemSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
         Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
             Card(
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 300.dp, max = 600.dp),
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
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(items) { index, item ->
                             val isSelected = item == selectedItem
                             
                             val shape = when {
                                items.size == 1 -> RoundedCornerShape(16.dp)
                                index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                index == items.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                                else -> RoundedCornerShape(4.dp)
                             }

                             Card(
                                modifier = Modifier.fillMaxWidth().clickable { onItemSelected(item) },
                                shape = shape, 
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                ),
                             ) {
                                 Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                     RadioButton(
                                        selected = isSelected,
                                        onClick = { onItemSelected(item) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                     Text(
                                        text = itemLabel(item),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                             }
                        }
                    }
                    
                     OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(id = R.string.close))
                    }
                }
             }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreConfigDialog(
    coreStates: List<Boolean>,
    configuredStates: Map<Int, Boolean>,
    onCoreToggled: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
             Card(
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 300.dp, max = 600.dp),
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
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.core_status),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    LazyColumn(
                         modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(coreStates) { index, _ ->
                            val isEnabled = configuredStates[index] ?: true
                            
                            val shape = when {
                                coreStates.size == 1 -> RoundedCornerShape(16.dp)
                                index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                index == coreStates.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                                else -> RoundedCornerShape(4.dp)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = shape,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.core_x, index),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { onCoreToggled(index, it) },
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
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(id = R.string.close))
                    }
                }
             }
        }
    }
}
