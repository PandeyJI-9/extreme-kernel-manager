package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.viewmodel.TuningViewModel

@Composable
fun PerformanceModeCard(
    viewModel: TuningViewModel,
    blur: Boolean = true
) {
    val activePerformanceMode by viewModel.activePerformanceMode.collectAsState()
    val savedPerformanceMode by viewModel.performanceMode.collectAsState()
    // Show saved pref while governors are still loading (activePerformanceMode == null)
    val displayedPerformanceMode = activePerformanceMode ?: savedPerformanceMode
    val availableGovernors by viewModel.generalAvailableCpuGovernors.collectAsState()

    val isPowersaveAvailable = remember(availableGovernors) { availableGovernors.contains("powersave") }
    val isBalancedAvailable = remember(availableGovernors) { availableGovernors.contains("schedutil") }
    val isPerformanceAvailable = remember(availableGovernors) { availableGovernors.contains("performance") }

    val performanceModes = remember { listOf("Powersave", "Balanced", "Performance") }
    val governorMappings = remember {
        mapOf("Powersave" to "powersave", "Balanced" to "schedutil", "Performance" to "performance")
    }

    val balancedGreen = MaterialTheme.colorScheme.primary
    val performanceRed = MaterialTheme.colorScheme.error
    val powersaveBlue = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.tuning_feature_performance_mode_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = stringResource(id = R.string.quick_presets_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                performanceModes.forEachIndexed { index, mode ->
                    val isFirst = index == 0
                    val isLast = index == performanceModes.lastIndex
                    val isEnabled = when (mode) {
                        "Powersave" -> isPowersaveAvailable
                        "Balanced" -> isBalancedAvailable
                        "Performance" -> isPerformanceAvailable
                        else -> true
                    }
                    val shape = when {
                        isFirst -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        isLast -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                        else -> RoundedCornerShape(4.dp)
                    }
                    val baseColor = when (mode) {
                        "Powersave" -> powersaveBlue
                        "Balanced" -> balancedGreen
                        "Performance" -> performanceRed
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = isEnabled) { viewModel.onPerformanceModeChange(mode) },
                        colors = CardDefaults.cardColors(
                            containerColor = (when (mode) {
                                "Powersave" -> if (displayedPerformanceMode == mode) powersaveBlue.copy(alpha = 0.15f) else powersaveBlue.copy(alpha = 0.05f)
                                "Balanced" -> if (displayedPerformanceMode == mode) balancedGreen.copy(alpha = 0.15f) else balancedGreen.copy(alpha = 0.05f)
                                "Performance" -> if (displayedPerformanceMode == mode) performanceRed.copy(alpha = 0.15f) else performanceRed.copy(alpha = 0.05f)
                                else -> MaterialTheme.colorScheme.surface
                            }).let { if (!isEnabled) it.copy(alpha = 0.02f) else it }
                        ),
                        shape = shape
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val contentAlpha = if (isEnabled) 1f else 0.38f
                            Box(
                                modifier = Modifier.size(42.dp).background(
                                    color = baseColor.copy(alpha = if (isEnabled) 0.2f else 0.05f),
                                    shape = CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        "Powersave" -> Icons.Default.BatterySaver
                                        "Balanced" -> Icons.Default.Balance
                                        "Performance" -> Icons.Default.FlashOn
                                        else -> Icons.Default.Speed
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = baseColor.copy(alpha = contentAlpha)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (displayedPerformanceMode == mode) FontWeight.Bold else FontWeight.Medium,
                                    color = (when (mode) {
                                        "Powersave" -> if (displayedPerformanceMode == mode) powersaveBlue else MaterialTheme.colorScheme.onSurface
                                        "Balanced" -> if (displayedPerformanceMode == mode) balancedGreen else MaterialTheme.colorScheme.onSurface
                                        "Performance" -> if (displayedPerformanceMode == mode) performanceRed else MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }).copy(alpha = contentAlpha)
                                )
                                Text(
                                    text = when (mode) {
                                        "Powersave" -> stringResource(id = R.string.powersave_performance_desc, governorMappings[mode] ?: "powersave")
                                        "Balanced" -> stringResource(id = R.string.balanced_performance_desc, governorMappings[mode] ?: "schedutil")
                                        "Performance" -> stringResource(id = R.string.maximum_speed_desc, governorMappings[mode] ?: "performance")
                                        else -> stringResource(id = R.string.default_governor_desc, governorMappings[mode] ?: "default")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                                )
                            }
                            if (displayedPerformanceMode == mode) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(id = R.string.common_selected),
                                    modifier = Modifier.size(20.dp),
                                    tint = (when (mode) {
                                        "Powersave" -> powersaveBlue
                                        "Balanced" -> balancedGreen
                                        "Performance" -> performanceRed
                                        else -> MaterialTheme.colorScheme.primary
                                    }).copy(alpha = contentAlpha)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = displayedPerformanceMode != "Balanced",
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                        expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                       shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = stringResource(id = R.string.performance_mode_applied),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.changed_cpu_governor_desc, governorMappings[displayedPerformanceMode] ?: "schedutil"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroHeader(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Outlined.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            }
            Text(text = stringResource(id = R.string.system_tuning), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = stringResource(id = R.string.system_tuning_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            Text(text = stringResource(id = R.string.tap_for_more_info), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BootSettingsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
            }
            Text(text = stringResource(id = R.string.boot_settings_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(text = stringResource(id = R.string.boot_settings_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}
