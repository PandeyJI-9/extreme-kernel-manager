package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.viewmodel.TuningViewModel
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants

@Composable
fun SwappinessCard(
    vm: TuningViewModel,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    val zramEnabled by vm.zramEnabled.collectAsState()
    val zramDisksize by vm.zramDisksize.collectAsState()
    val maxZramSize by vm.maxZramSize.collectAsState()
    val swappiness by vm.swappiness.collectAsState()
    val compressionAlgorithms by vm.compressionAlgorithms.collectAsState()
    val currentCompression by vm.currentCompression.collectAsState()
    val dirtyRatio by vm.dirtyRatio.collectAsState()
    val dirtyBackgroundRatio by vm.dirtyBackgroundRatio.collectAsState()
    val dirtyWriteback by vm.dirtyWriteback.collectAsState()
    val dirtyExpireCentisecs by vm.dirtyExpireCentisecs.collectAsState()
    val minFreeMemory by vm.minFreeMemory.collectAsState()
    val zramOperationInProgress by vm.zramOperationInProgress.collectAsState()


    // Dialog visibility states
    var showCompressionDialog by remember { mutableStateOf(false) }
    var showZramSizeDialog by remember { mutableStateOf(false) }
    var showSwappinessDialog by remember { mutableStateOf(false) }
    var showDirtyRatioDialog by remember { mutableStateOf(false) }
    var showDirtyBgRatioDialog by remember { mutableStateOf(false) }
    var showDirtyWritebackDialog by remember { mutableStateOf(false) }
    var showDirtyExpireDialog by remember { mutableStateOf(false) }
    var showMinFreeMemoryDialog by remember { mutableStateOf(false) }


    Card(
        modifier = Modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // RAM Control Header Section
            RamControlHeaderSection(
                zramEnabled = zramEnabled,
                zramDisksize = zramDisksize,
                isExpanded = isExpanded,
                onExpandClick = { onExpandChange(!isExpanded) }
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ZRAM Toggle Section
                    RamZramToggleSection(
                        zramEnabled = zramEnabled,
                        isBusy = zramOperationInProgress,
                        onToggle = { vm.setZramEnabled(!zramEnabled) }
                    )

                    AnimatedVisibility(visible = zramEnabled) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // ZRAM Settings title
                            Text(
                                text = stringResource(id = R.string.zram_settings),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp)
                            )
                            
                            // ZRAM Settings - 2 items
                            RamSettingItem(
                                icon = Icons.Default.Storage,
                                title = stringResource(id = R.string.zram_size),
                                value = "${zramDisksize / (1024 * 1024)}MB",
                                description = stringResource(id = R.string.zram_size_desc),
                                color = MaterialTheme.colorScheme.primary,
                                onClick = { showZramSizeDialog = true },
                                shape = getRoundedCornerShape(0, 2) // First card in group of 2
                            )

                            RamSettingItem(
                                icon = Icons.Default.Compress,
                                title = stringResource(id = R.string.compression),
                                value = currentCompression,
                                description = stringResource(id = R.string.compression_algorithm),
                                color = MaterialTheme.colorScheme.primary,
                                onClick = { showCompressionDialog = true },
                                shape = getRoundedCornerShape(1, 2) // Second card in group of 2
                            )
                        }
                    }

                    // Memory Settings Section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Memory Settings title
                        Text(
                            text = stringResource(id = R.string.memory_settings),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        )
                        
                        RamSettingItem(
                            icon = Icons.Default.Speed,
                            title = stringResource(id = R.string.tuning_feature_swappiness_title),
                            value = "$swappiness%",
                            description = stringResource(id = R.string.swappiness_desc),
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { showSwappinessDialog = true },
                            shape = getRoundedCornerShape(0, 6)
                        )

                        RamSettingItem(
                            icon = Icons.Default.DataUsage,
                            title = stringResource(id = R.string.dirty_ratio),
                            value = "$dirtyRatio%",
                            description = stringResource(id = R.string.dirty_ratio_desc),
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { showDirtyRatioDialog = true },
                            shape = getRoundedCornerShape(1, 6)
                        )

                        RamSettingItem(
                            icon = Icons.Default.Analytics,
                            title = stringResource(id = R.string.dirty_background_ratio),
                            value = "$dirtyBackgroundRatio%",
                            description = stringResource(id = R.string.dirty_background_ratio_desc),
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { showDirtyBgRatioDialog = true },
                            shape = getRoundedCornerShape(2, 6)
                        )

                        RamSettingItem(
                            icon = Icons.Default.Timer,
                            title = stringResource(id = R.string.dirty_writeback),
                            value = "${dirtyWriteback}s",
                            description = stringResource(id = R.string.dirty_writeback_desc),
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { showDirtyWritebackDialog = true },
                            shape = getRoundedCornerShape(3, 6)
                        )

                        RamSettingItem(
                            icon = Icons.Default.Schedule,
                            title = stringResource(id = R.string.dirty_expire),
                            value = "${dirtyExpireCentisecs}cs",
                            description = stringResource(id = R.string.dirty_expire_desc),
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { showDirtyExpireDialog = true },
                            shape = getRoundedCornerShape(4, 6)
                        )

                        RamSettingItem(
                            icon = Icons.Default.Memory,
                            title = stringResource(id = R.string.min_free_memory),
                            value = "${minFreeMemory}KB",
                            description = stringResource(id = R.string.min_free_memory_desc),
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { showMinFreeMemoryDialog = true },
                            shape = getRoundedCornerShape(5, 6)
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---
    if (showZramSizeDialog && zramEnabled) {
        ZramSizeDialog(
            currentSize = zramDisksize,
            maxSize = maxZramSize,
            onDismiss = { showZramSizeDialog = false },
            onConfirm = { newSizeInBytes: Long ->
                vm.setZramDisksize(newSizeInBytes)
                showZramSizeDialog = false
            }
        )
    }

    if (showCompressionDialog && zramEnabled) {
        CompressionAlgorithmDialog(
            compressionAlgorithms = compressionAlgorithms,
            currentCompression = currentCompression,
            onDismiss = { showCompressionDialog = false },
            onAlgorithmSelected = { algo: String ->
                if (algo != currentCompression) {
                    vm.setCompression(algo)
                }
                showCompressionDialog = false
            }
        )
    }

    if (showSwappinessDialog) {
        SliderSettingDialog(
            showDialog = showSwappinessDialog,
            title = stringResource(id = R.string.set_swappiness),
            icon = Icons.Default.Speed,
            explanation = stringResource(id = R.string.swappiness_explanation),
            currentValue = swappiness,
            valueSuffix = "%",
            valueRange = 0f..100f,
            steps = 99,
            onDismissRequest = { showSwappinessDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setSwappiness(newValue)
                showSwappinessDialog = false
            }
        )
    }

    if (showDirtyRatioDialog) {
        SliderSettingDialog(
            showDialog = showDirtyRatioDialog,
            title = stringResource(id = R.string.set_dirty_ratio),
            icon = Icons.Default.DataUsage,
            explanation = stringResource(id = R.string.dirty_ratio_explanation),
            currentValue = dirtyRatio,
            valueSuffix = "%",
            valueRange = 0f..100f,
            steps = 99,
            onDismissRequest = { showDirtyRatioDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setDirtyRatio(newValue)
                showDirtyRatioDialog = false
            }
        )
    }

    if (showDirtyBgRatioDialog) {
        SliderSettingDialog(
            showDialog = showDirtyBgRatioDialog,
            title = stringResource(id = R.string.set_dirty_background_ratio),
            icon = Icons.Default.Analytics,
            explanation = stringResource(id = R.string.dirty_background_ratio_explanation),
            currentValue = dirtyBackgroundRatio,
            valueSuffix = "%",
            valueRange = 0f..100f,
            steps = 99,
            onDismissRequest = { showDirtyBgRatioDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setDirtyBackgroundRatio(newValue)
                showDirtyBgRatioDialog = false
            }
        )
    }

    if (showDirtyWritebackDialog) {
        SliderSettingDialog(
            showDialog = showDirtyWritebackDialog,
            title = stringResource(id = R.string.set_dirty_writeback),
            icon = Icons.Default.Timer,
            explanation = stringResource(id = R.string.dirty_writeback_explanation),
            currentValue = dirtyWriteback,
            valueSuffix = stringResource(id = R.string.unit_sec),
            valueRange = 0f..300f,
            steps = 299,
            onDismissRequest = { showDirtyWritebackDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setDirtyWriteback(newValue)
                showDirtyWritebackDialog = false
            }
        )
    }

    if (showDirtyExpireDialog) {
        SliderSettingDialog(
            showDialog = showDirtyExpireDialog,
            title = stringResource(id = R.string.set_dirty_expire),
            icon = Icons.Default.Schedule,
            explanation = stringResource(id = R.string.dirty_expire_explanation),
            currentValue = dirtyExpireCentisecs,
            valueSuffix = stringResource(id = R.string.unit_cs),
            valueRange = 0f..30000f,
            steps = 29999,
            onDismissRequest = { showDirtyExpireDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setDirtyExpireCentisecs(newValue)
                showDirtyExpireDialog = false
            }
        )
    }

    if (showMinFreeMemoryDialog) {
        SliderSettingDialog(
            showDialog = showMinFreeMemoryDialog,
            title = stringResource(id = R.string.set_min_free_memory),
            icon = Icons.Default.Memory,
            explanation = stringResource(id = R.string.min_free_memory_explanation),
            currentValue = minFreeMemory,
            valueSuffix = stringResource(id = R.string.unit_kb_suffix),
            valueRange = 0f..524288f,
            steps = 511,
            onDismissRequest = { showMinFreeMemoryDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setMinFreeMemory(newValue)
                showMinFreeMemoryDialog = false
            },
            additionalInfo = stringResource(id = R.string.min_free_memory_warning)
        )
    }


}

@Composable
fun RamControlHeaderSection(
    zramEnabled: Boolean,
    zramDisksize: Long,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.ram_control),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // RAM Status Box
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (zramEnabled) stringResource(id = R.string.zram_active_template, zramDisksize / (1024 * 1024)) else stringResource(id = R.string.zram_disabled),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Animated RAM Icon with pulse effect
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.memory_alt_24),
                    contentDescription = stringResource(id = R.string.memory_status),
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) stringResource(id = R.string.collapse) else stringResource(id = R.string.expand),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun RamZramToggleSection(
    zramEnabled: Boolean,
    isBusy: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (zramEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            )
            .clickable(enabled = !isBusy, onClick = onToggle)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(id = R.string.zram_state),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isBusy) {
                Text(
                    text = if (zramEnabled) stringResource(id = R.string.disabling_zram_wait) else stringResource(id = R.string.enabling_zram_wait),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = if (zramEnabled) stringResource(id = R.string.zram_enabled_desc) else stringResource(id = R.string.zram_disabled_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(SwitchDefaults.IconSize),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Switch(
                checked = zramEnabled,
                enabled = !isBusy,
                onCheckedChange = { onToggle() },
                thumbContent = if (zramEnabled) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else {
                    {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun RamSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(id = R.string.configure),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}


private fun getRoundedCornerShape(index: Int, totalItems: Int): RoundedCornerShape {
    return when (totalItems) {
        1 -> RoundedCornerShape(12.dp) // If only one card, all corners 12dp
        2 -> {
            when (index) {
                0 -> RoundedCornerShape( // First card: 12dp top, 4dp bottom
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp
                )
                1 -> RoundedCornerShape( // Second card: 4dp top, 12dp bottom
                    topStart = 4.dp,
                    topEnd = 4.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                )
                else -> RoundedCornerShape(12.dp) // Default case
            }
        }
        else -> {
            // For groups with more than 2 items
            when (index) {
                0 -> RoundedCornerShape( // First card: 12dp top, 4dp bottom
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp
                )
                totalItems - 1 -> RoundedCornerShape( // Last card: 4dp top, 12dp bottom
                    topStart = 4.dp,
                    topEnd = 4.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                )
                else -> RoundedCornerShape(4.dp) // Middle cards: 4dp all sides
            }
        }
    }
}
