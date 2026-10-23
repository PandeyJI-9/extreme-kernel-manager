package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nkz.nokontzzzmanager.extreme.ExtremeKernelInfo
import id.nkz.nokontzzzmanager.extreme.ExtremeProfile
import id.nkz.nokontzzzmanager.extreme.ProfileManager
import kotlinx.coroutines.launch

@Composable
fun ExtremeProfileCard(
    profileManager: ProfileManager,
    kernelInfo: ExtremeKernelInfo,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val activeProfile by profileManager.activeProfileFlow.collectAsState(initial = ExtremeProfile.GAMING)
    var isApplying by remember { mutableStateOf(false) }
    var showRebootConfirmDialog by remember { mutableStateOf<ExtremeProfile?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: BMW Edition Engine Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Profile Engine",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "BMW EDITION ENGINE 🏎️💨",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = "POCO F4 (HyperOS) • In-Kernel Dynamic Switcher",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (kernelInfo.isExtremeKernel) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (kernelInfo.isExtremeKernel) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = if (kernelInfo.isExtremeKernel) "EXTREME++" else "STANDARD",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (kernelInfo.isExtremeKernel) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Kernel Detection Status Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (kernelInfo.isExtremeKernel) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (kernelInfo.isExtremeKernel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = kernelInfo.statusBadge,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (kernelInfo.isExtremeKernel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tri-Profile Switcher Cards (Gaming [0], Balanced [1], Battery [2])
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExtremeProfile.values().forEach { profile ->
                    val isSelected = activeProfile == profile

                    val (accentColor, unselectedContainer, selectedContainer, profileIcon) = when (profile) {
                        ExtremeProfile.GAMING -> ProfileVisuals(
                            accentColor = Color(0xFFFF3D00), // Red/Amber
                            unselectedContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedContainer = Color(0xFF2E130D),
                            icon = Icons.Default.Speed
                        )
                        ExtremeProfile.BALANCED -> ProfileVisuals(
                            accentColor = Color(0xFF00E5FF), // Cyan/Blue
                            unselectedContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedContainer = Color(0xFF092432),
                            icon = Icons.Default.Balance
                        )
                        ExtremeProfile.BATTERY -> ProfileVisuals(
                            accentColor = Color(0xFF00E676), // Emerald Green
                            unselectedContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedContainer = Color(0xFF0C2917),
                            icon = Icons.Default.BatterySaver
                        )
                    }

                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) accentColor else Color.Transparent,
                        animationSpec = tween(250),
                        label = "profileBorder"
                    )
                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) selectedContainer else unselectedContainer,
                        animationSpec = tween(250),
                        label = "profileContainer"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !isApplying) {
                                if (!isSelected) {
                                    isApplying = true
                                    coroutineScope.launch {
                                        profileManager.applyProfile(profile)
                                        isApplying = false
                                    }
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = containerColor,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top Row: Icon + Title + Active Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = profileIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = profile.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = profile.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accentColor.copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = accentColor
                                            )
                                        }
                                    }
                                } else {
                                    RadioButton(
                                        selected = false,
                                        onClick = {
                                            if (!isApplying) {
                                                isApplying = true
                                                coroutineScope.launch {
                                                    profileManager.applyProfile(profile)
                                                    isApplying = false
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            // Specs Tags Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ProfileTag(
                                    label = "Core 7: ${(profile.primeMaxFreqKHz / 1000)}MHz",
                                    accentColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outline
                                )
                                ProfileTag(
                                    label = "GPU: ${profile.gpuClockHz / 1000000}MHz",
                                    accentColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outline
                                )
                                ProfileTag(
                                    label = "Schedtune: +${profile.schedtuneBoost}",
                                    accentColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outline
                                )
                                if (profile == ExtremeProfile.GAMING) {
                                    ProfileTag(
                                        label = "Boot Default",
                                        accentColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Node Telemetry & Quick Reboot Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Node: /sys/kernel/extreme_profile [${activeProfile.code}]",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedButton(
                    onClick = { showRebootConfirmDialog = activeProfile },
                    enabled = !isApplying,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply & Drop Caches (Reboot)", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    // Reboot Confirmation Dialog
    showRebootConfirmDialog?.let { targetProfile ->
        AlertDialog(
            onDismissRequest = { showRebootConfirmDialog = null },
            title = { Text("Apply & Drop Caches?") },
            text = {
                Text("This will write '${targetProfile.title}' profile to /sys/kernel/extreme_profile (${targetProfile.code}) and persistent boot storage, drop caches (drop_caches=3), and trigger a clean reboot. Continue?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRebootConfirmDialog = null
                        coroutineScope.launch {
                            profileManager.applyAndReboot(targetProfile)
                        }
                    }
                ) {
                    Text("Reboot Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private data class ProfileVisuals(
    val accentColor: Color,
    val unselectedContainer: Color,
    val selectedContainer: Color,
    val icon: ImageVector
)

@Composable
private fun ProfileTag(
    label: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accentColor.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = accentColor
        )
    }
}
