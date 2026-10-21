package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
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
    val activeProfile by profileManager.activeProfileFlow.collectAsState(initial = ExtremeProfile.BALANCED_CRUISE)
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
            // Header: Hardware Engine Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Profile Engine",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "EXTREME ENGINE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    )
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
                            text = if (kernelInfo.isExtremeKernel) "EXTREME++ ACTIVE" else "STANDARD SYSFS",
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

            Text(
                text = "Dynamic Tri-Profile Switcher for Snapdragon 870 (POCO F4)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tri-Profile Switcher Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExtremeProfile.values().forEach { profile ->
                    val isSelected = activeProfile == profile
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(250),
                        label = "profileBorder"
                    )
                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
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
                        border = BorderStroke(1.5.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = profile.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = "• ACTIVE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = profile.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Little: ${profile.littleMaxFreqKHz / 1000}MHz | Gold: ${profile.goldMaxFreqKHz / 1000}MHz | Prime: ${profile.primeMaxFreqKHz / 1000}MHz",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected && !isApplying) {
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
                }
            }

            // Quick Actions: Apply & Clean Flush Reboot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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
                    Text("Apply & Flush Cache (Reboot)", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    // Reboot Confirmation Dialog
    showRebootConfirmDialog?.let { targetProfile ->
        AlertDialog(
            onDismissRequest = { showRebootConfirmDialog = null },
            title = { Text("Apply & Flush Cache?") },
            text = {
                Text("This will write '${targetProfile.title}' profile to boot persistent storage, drop OS caches (drop_caches=3), and trigger a clean reboot. Continue?")
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
