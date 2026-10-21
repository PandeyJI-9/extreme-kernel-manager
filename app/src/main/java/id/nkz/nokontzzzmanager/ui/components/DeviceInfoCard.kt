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
import id.nkz.nokontzzzmanager.data.model.StorageInfo
import id.nkz.nokontzzzmanager.data.model.SystemInfo
import java.util.Locale
import androidx.compose.ui.res.stringResource

@Composable
fun DeviceInfoCard(
    systemInfo: SystemInfo,
    rooted: Boolean,
    version: String,
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.device_information),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Root Status Box
                        Box(
                            modifier = Modifier
                                .background(
                                    if (rooted) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (rooted) stringResource(id = R.string.rooted) else stringResource(id = R.string.not_rooted),
                                color = if (rooted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        // Version Box
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.version_template, version),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                // Device Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = stringResource(id = R.string.device),
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Device Stats
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
                    // Device Info Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.PhoneAndroid,
                            label = stringResource(id = R.string.model),
                            value = systemInfo.model,
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.Code,
                            label = stringResource(id = R.string.codename),
                            value = systemInfo.codename,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Device Info Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.Android,
                            label = stringResource(id = R.string.android),
                            value = systemInfo.androidVersion,
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.Build,
                            label = stringResource(id = R.string.sdk),
                            value = systemInfo.sdk.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Device Info Row 3 - SoC and Fingerprint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.DeveloperBoard,
                            label = stringResource(id = R.string.cpu_soc_label),
                            value = systemInfo.soc,
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.Fingerprint,
                            label = stringResource(id = R.string.fingerprint),
                            value = systemInfo.fingerprint.substringAfterLast("/").substringBefore(":"),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Device Info Row 4 - Display Resolution and Technology
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.AspectRatio,
                            label = stringResource(id = R.string.resolution),
                            value = systemInfo.screenResolution,
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.DisplaySettings,
                            label = stringResource(id = R.string.technology),
                            value = systemInfo.displayTechnology,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Device Info Row 5 - Refresh Rate and DPI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.Speed,
                            label = stringResource(id = R.string.refresh_rate),
                            value = systemInfo.refreshRate,
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.PhotoSizeSelectSmall,
                            label = stringResource(id = R.string.dpi),
                            value = systemInfo.screenDpi,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

