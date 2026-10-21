package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.model.BatteryInfo
import id.nkz.nokontzzzmanager.data.model.DeepSleepInfo
import id.nkz.nokontzzzmanager.data.model.MemoryInfo
import id.nkz.nokontzzzmanager.data.model.StorageInfo
import id.nkz.nokontzzzmanager.data.model.SystemInfo
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.math.roundToInt

import androidx.compose.ui.res.stringResource

// Helper function to format time duration with seconds
private fun formatTimeWithSeconds(timeInMillis: Long): String {
    val totalSeconds = timeInMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

// Helper function to format storage size
@Composable
internal fun formatStorageSize(bytes: Long): String {
    val tb = 1_000_000_000_000L
    val gb = 1_000_000_000L
    val mb = 1_000_000L
    val kb = 1_000L

    return when {
        bytes >= tb -> stringResource(id = R.string.storage_tb, bytes.toDouble() / tb)
        bytes >= gb -> stringResource(id = R.string.storage_gb, bytes.toDouble() / gb)
        bytes >= mb -> stringResource(id = R.string.storage_mb, bytes.toDouble() / mb)
        bytes >= kb -> stringResource(id = R.string.storage_kb, bytes.toDouble() / kb)
        else -> stringResource(id = R.string.storage_b, bytes)
    }
}

@Composable
fun MemoryCard(
    memoryInfo: MemoryInfo,
    modifier: Modifier = Modifier
) {
    val usedPercentage = if (memoryInfo.total > 0) {
        ((memoryInfo.used.toDouble() / memoryInfo.total.toDouble()) * 100).roundToInt()
    } else 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Memory Header Section
            MemoryHeaderSection(memoryInfo = memoryInfo, usedPercentage = usedPercentage)

            // Memory Progress Section
            MemoryProgressSection(memoryInfo = memoryInfo, usedPercentage = usedPercentage)

            // Memory Stats Section
            MemoryStatsSection(memoryInfo = memoryInfo)
        }
    }
}

@Composable
fun StorageCard(
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    val usedPercentage = if (storageInfo.totalSpace > 0) {
        ((storageInfo.usedSpace.toDouble() / storageInfo.totalSpace.toDouble()) * 100).roundToInt()
    } else 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Header Section
            StorageHeaderSection(storageInfo = storageInfo, usedPercentage = usedPercentage)

            // Storage Progress Section
            StorageProgressSection(storageInfo = storageInfo)

            // Storage Stats Section
            StorageStatsSection(storageInfo = storageInfo)
        }
    }
}

@Composable
private fun MemoryHeaderSection(
    memoryInfo: MemoryInfo,
    usedPercentage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.memory_status),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Memory Status Box
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val totalGb = (memoryInfo.total.toDouble() / 1_000_000_000.0).roundToLong()
                val zramGb = (memoryInfo.zramTotal.toDouble() / 1_000_000_000.0).roundToLong()

                val memoryText = if (zramGb > 0) {
                    stringResource(id = R.string.memory_status_template_with_zram, usedPercentage, totalGb, zramGb)
                } else {
                    stringResource(id = R.string.memory_status_template, usedPercentage, totalGb)
                }

                Text(
                    text = memoryText,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Memory Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                painter = painterResource(id = R.drawable.memory_alt_24),
                contentDescription = stringResource(id = R.string.memory_status),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun StorageHeaderSection(
    storageInfo: StorageInfo,
    usedPercentage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.storage_status),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Storage Status Box
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.storage_status_template, usedPercentage, formatStorageSize(storageInfo.totalSpace)),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Storage Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = stringResource(id = R.string.storage_status),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun MemoryProgressSection(
    memoryInfo: MemoryInfo,
    usedPercentage: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // RAM Usage Progress Bar
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = stringResource(id = R.string.memory_status),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.ram_usage),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(id = R.string.usage_percentage, usedPercentage),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            val progressColor = when {
                usedPercentage < 60 -> MaterialTheme.colorScheme.primary
                usedPercentage < 80 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            LinearProgressIndicator(
                progress = { usedPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }

        // ZRAM Usage Progress Bar (only show if zram is available)
        if (memoryInfo.zramTotal > 0) {
            val zramUsedPercentage = ((memoryInfo.zramUsed.toDouble() / memoryInfo.zramTotal.toDouble()) * 100).roundToInt()

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compress,
                            contentDescription = stringResource(id = R.string.zram),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.zram_usage),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.usage_percentage, zramUsedPercentage),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                LinearProgressIndicator(
                    progress = { zramUsedPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }


    }
}

@Composable
private fun MemoryStatsSection(
    memoryInfo: MemoryInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.system_stats_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Memory Stats Row 1 - Used and Free RAM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Used RAM
                SystemStatItem(
                    icon = Icons.Default.Memory,
                    label = stringResource(id = R.string.used_ram),
                    value = stringResource(id = R.string.mb, memoryInfo.used / (1024 * 1024)),
                    modifier = Modifier.weight(1f)
                )

                // Free RAM
                SystemStatItem(
                    icon = Icons.Default.Storage,
                    label = stringResource(id = R.string.free_ram),
                    value = stringResource(id = R.string.mb, memoryInfo.free / (1024 * 1024)),
                    modifier = Modifier.weight(1f)
                )
            }

            // Memory Stats Row 2 - Total RAM and Usage Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total RAM
                SystemStatItem(
                    icon = Icons.Default.Widgets,
                    label = stringResource(id = R.string.total_ram),
                    value = stringResource(id = R.string.mb, memoryInfo.total / (1024 * 1024)),
                    modifier = Modifier.weight(1f)
                )

                // Usage Percentage
                SystemStatItem(
                    icon = Icons.Default.Analytics,
                    label = stringResource(id = R.string.usage_percentage_label),
                    value = stringResource(id = R.string.usage_percentage, ((memoryInfo.used.toDouble() / memoryInfo.total.toDouble()) * 100).roundToInt()),
                    modifier = Modifier.weight(1f)
                )
            }

            // Memory Stats Row 3 - ZRAM Stats (only show if zram is available)
            if (memoryInfo.zramTotal > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ZRAM Used
                    SystemStatItem(
                        icon = Icons.Default.Compress,
                        label = stringResource(id = R.string.zram_used),
                        value = stringResource(id = R.string.mb, memoryInfo.zramUsed / (1024 * 1024)),
                        modifier = Modifier.weight(1f)
                    )

                    // ZRAM Total
                    SystemStatItem(
                        icon = Icons.Default.Compress,
                        label = stringResource(id = R.string.zram_total),
                        value = stringResource(id = R.string.mb, memoryInfo.zramTotal / (1024 * 1024)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageProgressSection(
    storageInfo: StorageInfo
) {
    val usedPercentage = if (storageInfo.totalSpace > 0) {
        ((storageInfo.usedSpace.toDouble() / storageInfo.totalSpace.toDouble()) * 100).roundToInt()
    } else 0

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = stringResource(id = R.string.internal_storage),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(id = R.string.internal_storage),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(id = R.string.storage_usage_template, formatStorageSize(storageInfo.usedSpace), formatStorageSize(storageInfo.totalSpace)),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        val progressColor = when {
            usedPercentage < 60 -> MaterialTheme.colorScheme.primary
            usedPercentage < 80 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        }
        LinearProgressIndicator(
            progress = { usedPercentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@Composable
private fun StorageStatsSection(
    storageInfo: StorageInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.system_stats_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SystemStatItem(
                    icon = Icons.Default.Storage,
                    label = stringResource(id = R.string.used_storage),
                    value = formatStorageSize(storageInfo.usedSpace),
                    modifier = Modifier.weight(1f)
                )
                SystemStatItem(
                    icon = Icons.Default.FolderOpen,
                    label = stringResource(id = R.string.free_storage),
                    value = formatStorageSize(storageInfo.freeSpace),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SystemStatItem(
                    icon = Icons.Default.Storage,
                    label = stringResource(id = R.string.total_storage),
                    value = formatStorageSize(storageInfo.totalSpace),
                    modifier = Modifier.weight(1f)
                )
                SystemStatItem(
                    icon = Icons.Default.Analytics,
                    label = stringResource(id = R.string.usage_percentage_label),
                    value = stringResource(id = R.string.usage_percentage, ((storageInfo.usedSpace.toDouble() / storageInfo.totalSpace.toDouble()) * 100).roundToInt()),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun SystemStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

