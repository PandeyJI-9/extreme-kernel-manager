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
            modifier = Modifier.fillMaxWidth()
        ) {
            // BMW M Racing Stripe Accent (Top Edge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF00A3E0))) // M Light Blue
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF002663))) // M Dark Blue
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFE21A22))) // M Red
            }

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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Profile Engine",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EXTREME++ GAMING",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                            )
                            Text(
                                text = "BMW Edition • Dynamic Profile Switcher",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (kernelInfo.isExtremeKernel) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (kernelInfo.isExtremeKernel) Icons.Default.Verified else Icons.Default.ElectricBolt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (kernelInfo.isExtremeKernel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (kernelInfo.isExtremeKernel) "BMW ACTIVE" else "STANDARD",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (kernelInfo.isExtremeKernel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Kernel Status Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (kernelInfo.isExtremeKernel) Color(0xFFFF9100) else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = kernelInfo.statusBadge,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                // Tri-Profile Switcher Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExtremeProfile.values().forEach { profile ->
                        val isSelected = activeProfile == profile

                        val (accentColor, profileIcon, targetFpsTrait) = when (profile) {
                            ExtremeProfile.GAMING -> Triple(
                                Color(0xFFFF3D00), // Vibrant Flame Red/Amber
                                Icons.Default.Speed,
                                "120 FPS Unlocked • Monster Peak"
                            )
                            ExtremeProfile.BALANCED -> Triple(
                                Color(0xFF00B0FF), // Electric Cyan/Blue
                                Icons.Default.Balance,
                                "90 FPS Sustained • Zero Throttling"
                            )
                            ExtremeProfile.BATTERY -> Triple(
                                Color(0xFF00E676), // Emerald Green
                                Icons.Default.BatterySaver,
                                "Maximum SOT • Deep Idle Preservation"
                            )
                        }

                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            animationSpec = tween(250),
                            label = "profileBorder"
                        )
                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            animationSpec = tween(250),
                            label = "profileContainer"
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(enabled = !isApplying) {
                                    if (!isSelected) {
                                        isApplying = true
                                        coroutineScope.launch {
                                            profileManager.applyProfile(profile)
                                            isApplying = false
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(18.dp),
                            color = containerColor,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top Header Row: Icon + Name/Trait + Active Indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(accentColor.copy(alpha = if (isSelected) 0.20f else 0.10f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = profileIcon,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = when (profile) {
                                                        ExtremeProfile.GAMING -> "Gaming"
                                                        ExtremeProfile.BALANCED -> "Balanced"
                                                        ExtremeProfile.BATTERY -> "Battery"
                                                    },
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                                )

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = accentColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "MODE ${profile.code}",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        ),
                                                        color = accentColor
                                                    )
                                                }
                                            }

                                            Text(
                                                text = targetFpsTrait,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = accentColor.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "ACTIVE",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        letterSpacing = 0.5.sp
                                                    ),
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

                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )

                                // 3-Column Specifications Grid (No overflow, crisp alignment)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileStatColumn(
                                        label = "PRIME PEAK",
                                        value = "${profile.primeMaxFreqKHz / 1000000}.${(profile.primeMaxFreqKHz % 1000000) / 10000} GHz",
                                        isSelected = isSelected,
                                        accentColor = accentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ProfileStatColumn(
                                        label = "GPU CLOCK",
                                        value = "${profile.gpuClockHz / 1000000} MHz",
                                        isSelected = isSelected,
                                        accentColor = accentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ProfileStatColumn(
                                        label = "BOOST TUNE",
                                        value = if (profile.schedtuneBoost > 0) "+${profile.schedtuneBoost} Sched" else "0 Saver",
                                        isSelected = isSelected,
                                        accentColor = accentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Node Telemetry & Reboot Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Text(
                            text = "/sys/kernel/extreme_profile: ${activeProfile.code}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

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
                        Text("Apply & Drop Caches", style = MaterialTheme.typography.labelMedium)
                    }
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

@Composable
private fun ProfileStatColumn(
    label: String,
    value: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Black
            ),
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
