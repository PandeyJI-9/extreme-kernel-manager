package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import id.nkz.nokontzzzmanager.ui.dialog.CoreConfigDialog
import id.nkz.nokontzzzmanager.ui.dialog.SelectionDialog
import id.nkz.nokontzzzmanager.viewmodel.TuningViewModel
import kotlin.math.abs

import androidx.compose.ui.res.stringResource
import id.nkz.nokontzzzmanager.R

@Composable
fun CpuClusterCard(
    clusterName: String,
    vm: TuningViewModel,
    onGovernorClick: () -> Unit,
    onMinFrequencyClick: () -> Unit,
    onMaxFrequencyClick: () -> Unit,
    onCoreClick: () -> Unit,
    shape: RoundedCornerShape
) {
    val currentGovernor by vm.getCpuGov(clusterName).collectAsState()
    val currentFreqPair by vm.getCpuFreq(clusterName).collectAsState()
    val availableFrequenciesForCluster by vm.getAvailableCpuFrequencies(clusterName).collectAsState()
    val coreStates by vm.coreStates.collectAsState()
    
    // Map cluster identifiers to display names
    val displayClusterName = when (clusterName) {
        "cpu0" -> stringResource(id = R.string.little_cluster)
        "cpu4" -> stringResource(id = R.string.big_cluster)
        "cpu7" -> stringResource(id = R.string.prime_cluster)
        else -> clusterName.uppercase()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enhanced Header with cluster-specific styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cluster icon with themed background
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = displayClusterName.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(id = R.string.cluster_control),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Status indicator
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                ) {
                    Text(
                        text = if (currentGovernor != "..." && currentGovernor != "Error") stringResource(id = R.string.active) else stringResource(id = R.string.loading),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (currentGovernor != "..." && currentGovernor != "Error")
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Enhanced Control Sections with custom rounded corners
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Governor Section (first card - 12dp top, 4dp bottom)
                ControlSection(
                    icon = Icons.Default.Tune,
                    title = stringResource(id = R.string.cpu_governor_label),
                    value = if (currentGovernor == "..." || currentGovernor == "Error") currentGovernor else currentGovernor,
                    isLoading = currentGovernor == "..." || currentGovernor == "Error",
                    themeColor = MaterialTheme.colorScheme.primary,
                    onClick = onGovernorClick,
                    enabled = currentGovernor != "..." && currentGovernor != "Error",
                    cornerShape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    )
                )

                // Min Frequency Section (middle card - 8dp all sides)
                val minFreqText = when {
                    currentGovernor == "..." -> stringResource(id = R.string.loading_ellipsis)
                    currentGovernor == "Error" -> stringResource(id = R.string.error)
                    currentFreqPair.first > 0 -> stringResource(id = R.string.cpu_freq_mhz, currentFreqPair.first / 1000)
                    currentFreqPair.first == -1 -> stringResource(id = R.string.error)
                    else -> stringResource(id = R.string.loading_ellipsis)
                }

                ControlSection(
                    icon = Icons.Default.Speed,
                    title = stringResource(id = R.string.min_frequency),
                    value = minFreqText,
                    isLoading = minFreqText == stringResource(id = R.string.loading_ellipsis),
                    themeColor = MaterialTheme.colorScheme.primary,
                    onClick = onMinFrequencyClick,
                    enabled = availableFrequenciesForCluster.isNotEmpty(),
                    cornerShape = RoundedCornerShape(4.dp)
                )

                // Max Frequency Section (middle card - 8dp all sides)
                val maxFreqText = when {
                    currentGovernor == "..." -> stringResource(id = R.string.loading_ellipsis)
                    currentGovernor == "Error" -> stringResource(id = R.string.error)
                    currentFreqPair.second > 0 -> stringResource(id = R.string.cpu_freq_mhz, currentFreqPair.second / 1000)
                    currentFreqPair.second == -1 -> stringResource(id = R.string.error)
                    else -> stringResource(id = R.string.loading_ellipsis)
                }

                ControlSection(
                    icon = Icons.Default.Speed,
                    title = stringResource(id = R.string.max_frequency),
                    value = maxFreqText,
                    isLoading = maxFreqText == stringResource(id = R.string.loading_ellipsis),
                    themeColor = MaterialTheme.colorScheme.primary,
                    onClick = onMaxFrequencyClick,
                    enabled = availableFrequenciesForCluster.isNotEmpty(),
                    cornerShape = RoundedCornerShape(4.dp)
                )

                // Core Status Section (last card - 8dp top, 12dp bottom)
                ControlSection(
                    icon = Icons.Default.Memory,
                    title = stringResource(id = R.string.core_status),
                    value = stringResource(id = R.string.cores_online, coreStates.count { it }, coreStates.size),
                    isLoading = false,
                    themeColor = MaterialTheme.colorScheme.primary,
                    onClick = onCoreClick,
                    enabled = true,
                    cornerShape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun ControlSection(
    icon: ImageVector,
    title: String,
    value: String,
    isLoading: Boolean,
    themeColor: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    cornerShape: RoundedCornerShape = RoundedCornerShape(24.dp)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = cornerShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon with themed background
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (enabled) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Arrow indicator
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.expand),
                    tint = themeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private sealed interface CpuClusterDialogState {
    data class Governor(val cluster: String) : CpuClusterDialogState
    data class MinFreq(val cluster: String) : CpuClusterDialogState
    data class MaxFreq(val cluster: String) : CpuClusterDialogState
    data object CoreStatus : CpuClusterDialogState
}

@Composable
fun CpuGovernorCard(
    vm: TuningViewModel,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    val clusters by vm.dynamicCpuClusters.collectAsState()
    val shape = RoundedCornerShape(8.dp)

    var activeDialog by remember { mutableStateOf<CpuClusterDialogState?>(null) }
    val generalAvailableGovernors by vm.generalAvailableCpuGovernors.collectAsState()
    val coreStates by vm.coreStates.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        clusters.forEach { cluster ->
            CpuClusterCard(
                clusterName = cluster,
                vm = vm,
                onGovernorClick = { activeDialog = CpuClusterDialogState.Governor(cluster) },
                onMinFrequencyClick = { activeDialog = CpuClusterDialogState.MinFreq(cluster) },
                onMaxFrequencyClick = { activeDialog = CpuClusterDialogState.MaxFreq(cluster) },
                onCoreClick = { activeDialog = CpuClusterDialogState.CoreStatus },
                shape = shape
            )
        }
    }

    when (val dialog = activeDialog) {
        is CpuClusterDialogState.Governor -> {
            val currentGov by vm.getCpuGov(dialog.cluster).collectAsState()
            SelectionDialog(
                title = stringResource(R.string.cpu_governor_label),
                subtitle = dialog.cluster,
                items = generalAvailableGovernors,
                selectedItem = currentGov.takeIf { it != "..." && it != "Error" },
                itemLabel = { it },
                onItemSelected = { vm.setCpuGov(dialog.cluster, it); activeDialog = null },
                onDismiss = { activeDialog = null }
            )
        }
        is CpuClusterDialogState.MinFreq -> {
            val availableFreqs by vm.getAvailableCpuFrequencies(dialog.cluster).collectAsState()
            val currentFreq by vm.getCpuFreq(dialog.cluster).collectAsState()
            SelectionDialog(
                title = stringResource(R.string.set_min_frequency),
                subtitle = dialog.cluster,
                items = availableFreqs,
                selectedItem = currentFreq.first.takeIf { it > 0 },
                itemLabel = { stringResource(R.string.app_profiles_mhz_suffix, it / 1000) },
                onItemSelected = { selected ->
                    val current = vm.getCpuFreq(dialog.cluster).value
                    vm.setCpuFreq(dialog.cluster, selected, current.second)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        is CpuClusterDialogState.MaxFreq -> {
            val availableFreqs by vm.getAvailableCpuFrequencies(dialog.cluster).collectAsState()
            val currentFreq by vm.getCpuFreq(dialog.cluster).collectAsState()
            SelectionDialog(
                title = stringResource(R.string.set_max_frequency),
                subtitle = dialog.cluster,
                items = availableFreqs,
                selectedItem = currentFreq.second.takeIf { it > 0 },
                itemLabel = { stringResource(R.string.app_profiles_mhz_suffix, it / 1000) },
                onItemSelected = { selected ->
                    val current = vm.getCpuFreq(dialog.cluster).value
                    vm.setCpuFreq(dialog.cluster, current.first, selected)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        is CpuClusterDialogState.CoreStatus -> {
            CoreConfigDialog(
                coreStates = coreStates,
                configuredStates = coreStates.mapIndexed { index, enabled -> index to enabled }.toMap(),
                onCoreToggled = { index, _ -> vm.toggleCore(index) },
                onDismiss = { activeDialog = null }
            )
        }
        null -> {}
    }
}