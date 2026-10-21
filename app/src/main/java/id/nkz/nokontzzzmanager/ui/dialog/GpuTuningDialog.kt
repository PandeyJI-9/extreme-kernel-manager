package id.nkz.nokontzzzmanager.ui.dialog

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.model.GpuProfileConfig
import id.nkz.nokontzzzmanager.viewmodel.AppProfilesViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpuTuningDialog(
    appName: String,
    initialConfig: GpuProfileConfig,
    viewModel: AppProfilesViewModel,
    onDismiss: () -> Unit,
    onSave: (GpuProfileConfig) -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }
    var showResetFeedback by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    // Collect Data
    val availableGovernors by viewModel.availableGpuGovernors.collectAsStateWithLifecycle(initialValue = emptyList())
    val availableFrequencies by viewModel.availableGpuFrequencies.collectAsStateWithLifecycle(initialValue = emptyList())
    val powerLevelRange by viewModel.gpuPowerLevelRange.collectAsStateWithLifecycle(initialValue = 0f to 0f)

    // State for dialogs
    var showGovernorDialog by remember { mutableStateOf(false) }
    var showMinFreqDialog by remember { mutableStateOf(false) }
    var showMaxFreqDialog by remember { mutableStateOf(false) }

    // Temp state for slider
    var tempPowerLevel by remember(config.powerLevel) { 
        mutableFloatStateOf(config.powerLevel?.toFloat() ?: 0f) 
    }

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
                                imageVector = Icons.Default.DeveloperBoard,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_profiles_gpu_tuning),
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
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Governor
                        item {
                            GpuSettingItem(
                                icon = Icons.Default.Tune,
                                title = stringResource(R.string.gpu_governor),
                                value = config.governor ?: stringResource(R.string.default_governor_desc, stringResource(R.string.app_profiles_default)),
                                onClick = { showGovernorDialog = true },
                                isSet = config.governor != null,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                            )
                        }

                        // Frequencies
                        item {
                            GpuSettingItem(
                                icon = Icons.Default.Speed,
                                title = stringResource(R.string.min_frequency),
                                value = config.minFreq?.let { stringResource(R.string.app_profiles_mhz_suffix, it) } ?: stringResource(R.string.app_profiles_default),
                                onClick = { showMinFreqDialog = true },
                                isSet = config.minFreq != null,
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                            )
                        }
                        item {
                            GpuSettingItem(
                                icon = Icons.Default.Speed,
                                title = stringResource(R.string.max_frequency),
                                value = config.maxFreq?.let { stringResource(R.string.app_profiles_mhz_suffix, it) } ?: stringResource(R.string.app_profiles_default),
                                onClick = { showMaxFreqDialog = true },
                                isSet = config.maxFreq != null,
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                            )
                        }

                        // Power Level
                        item {
                            val minPowerLevel = minOf(powerLevelRange.first, powerLevelRange.second)
                            val maxPowerLevel = maxOf(powerLevelRange.first, powerLevelRange.second)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.power_level),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = if (config.powerLevel != null) config.powerLevel.toString() else stringResource(R.string.app_profiles_default),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (config.powerLevel != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Slider(
                                        value = if (config.powerLevel != null) tempPowerLevel else minPowerLevel,
                                        onValueChange = { 
                                            // Manual integer snapping
                                            val snappedValue = kotlin.math.round(it)
                                            if (snappedValue != tempPowerLevel) {
                                                tempPowerLevel = snappedValue
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            }
                                            
                                            // Auto-enable if dragged
                                            if (config.powerLevel == null) config = config.copy(powerLevel = snappedValue.toInt())
                                        },
                                        onValueChangeFinished = {
                                            config = config.copy(powerLevel = tempPowerLevel.toInt())
                                        },
                                        valueRange = minPowerLevel..maxPowerLevel,
                                        enabled = true
                                    )
                                }
                            }
                        }

                        // Throttling
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(id = R.string.gpu_throttling),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (config.throttlingEnabled != null) {
                                                if (config.throttlingEnabled == true) stringResource(R.string.app_profiles_on) else stringResource(R.string.app_profiles_off)
                                            } else stringResource(R.string.app_profiles_default),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (config.throttlingEnabled != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // 3-state toggle logic: Default (null) -> On (true) -> Off (false) -> Default (null) ??
                                    // Or simpler: Checkbox to enable override? 
                                    // Let's use a similar approach to Core Status: Click to toggle override/state? 
                                    // But requested was "Switch". A switch only has 2 states. 
                                    // To support "Default", we might need a way to unset it. 
                                    // Let's use a Row with Text "Override" and Switch? 
                                    // Or just cycle null -> true -> false on click?
                                    // For now, let's implement a tri-state cycle clickable on the card, and just show icon?
                                    // No, the prompt said "switch". A standard switch implies boolean.
                                    // Let's assume if user touches it, it becomes overridden. 
                                    // To unset, they use "Reset to Default".
                                    
                                    val isChecked = config.throttlingEnabled ?: true // Default to true visually if unknown, or maybe false?
                                    // Let's use false as safe default or check system default? 
                                    // For UI, if it's null (default), maybe show grayed out switch?
                                    
                                    Switch(
                                        checked = config.throttlingEnabled == true,
                                        onCheckedChange = { 
                                            config = config.copy(throttlingEnabled = it)
                                        },
                                        thumbContent = if (config.throttlingEnabled == true) {
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

                        // Reset
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (showResetFeedback) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                ),
                                onClick = { 
                                    config = GpuProfileConfig()
                                    showResetFeedback = true
                                    tempPowerLevel = 0f // Reset slider visual
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    AnimatedContent(targetState = showResetFeedback, label = "reset") { success ->
                                        Icon(
                                            imageVector = if (success) Icons.Default.Check else Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = if (success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                        )
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
    if (showGovernorDialog) {
        SelectionDialog(
            title = stringResource(R.string.gpu_governor),
            subtitle = stringResource(R.string.gpu_control_title),
            items = availableGovernors,
            selectedItem = config.governor,
            itemLabel = { it },
            onItemSelected = { 
                config = config.copy(governor = it)
                showGovernorDialog = false
            },
            onDismiss = { showGovernorDialog = false }
        )
    }

    if (showMinFreqDialog) {
        SelectionDialog(
            title = stringResource(R.string.set_min_frequency),
            subtitle = stringResource(R.string.gpu_frequency),
            items = availableFrequencies,
            selectedItem = config.minFreq,
            itemLabel = { "$it MHz" },
            onItemSelected = { 
                config = config.copy(minFreq = it)
                showMinFreqDialog = false
            },
            onDismiss = { showMinFreqDialog = false }
        )
    }

    if (showMaxFreqDialog) {
        SelectionDialog(
            title = stringResource(R.string.set_max_frequency),
            subtitle = stringResource(R.string.gpu_frequency),
            items = availableFrequencies,
            selectedItem = config.maxFreq,
            itemLabel = { "$it MHz" },
            onItemSelected = { 
                config = config.copy(maxFreq = it)
                showMaxFreqDialog = false
            },
            onDismiss = { showMaxFreqDialog = false }
        )
    }
}

@Composable
fun GpuSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    isSet: Boolean,
    shape: RoundedCornerShape
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
