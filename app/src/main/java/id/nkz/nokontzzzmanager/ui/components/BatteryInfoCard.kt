package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.model.BatteryInfo
import id.nkz.nokontzzzmanager.data.model.DeepSleepInfo
import java.util.Locale

/** Extracted from MergedSystemCard.kt — self-contained battery info card. */
@Composable
fun BatteryCard(
    batteryInfo: BatteryInfo,
    deepSleepInfo: DeepSleepInfo?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BatteryHeaderSection(batteryInfo = batteryInfo)
            BatteryProgressSection(batteryInfo = batteryInfo)
            BatteryStatsSection(batteryInfo = batteryInfo, deepSleepInfo = deepSleepInfo)
        }
    }
}

@Composable
internal fun BatteryHeaderSection(batteryInfo: BatteryInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.battery_status),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.battery_status_template, batteryInfo.level, batteryInfo.temp,
                        if (batteryInfo.isCharging) stringResource(id = R.string.charging) else stringResource(id = R.string.discharging)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
        Box(
            modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                contentDescription = stringResource(id = R.string.battery_toggle),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
internal fun BatteryProgressSection(batteryInfo: BatteryInfo) {
    val progressColor = when {
        batteryInfo.level > 70 -> MaterialTheme.colorScheme.primary
        batteryInfo.level > 30 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.BatteryFull, stringResource(id = R.string.battery_toggle), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(stringResource(id = R.string.charge_level), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            }
            Text(stringResource(id = R.string.usage_percentage, batteryInfo.level), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = { batteryInfo.level / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@Composable
internal fun BatteryStatsSection(batteryInfo: BatteryInfo, deepSleepInfo: DeepSleepInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.system_stats_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SystemStatItem(Icons.Default.ElectricBolt, stringResource(id = R.string.voltage), run {
                    val v = if (batteryInfo.voltage > 0) {
                        val vv = when { batteryInfo.voltage > 1_000_000 -> batteryInfo.voltage / 1_000_000f; batteryInfo.voltage > 1_000 -> batteryInfo.voltage / 1_000f; else -> batteryInfo.voltage }
                        String.format(Locale.getDefault(), "%.2f", vv).trimEnd('0').trimEnd('.')
                    } else "0"
                    stringResource(id = R.string.v, v)
                }, Modifier.weight(1f))
                SystemStatItem(Icons.Default.AccessTime, stringResource(id = R.string.uptime), deepSleepInfo?.let { formatTimeWithSeconds(it.uptime) } ?: stringResource(id = R.string.common_na), Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SystemStatItem(Icons.Default.HealthAndSafety, stringResource(id = R.string.health), if (batteryInfo.healthPercentage > 0) stringResource(id = R.string.usage_percentage, batteryInfo.healthPercentage) else batteryInfo.health, Modifier.weight(1f))
                SystemStatItem(Icons.Default.Autorenew, stringResource(id = R.string.cycles), if (batteryInfo.cycleCount > 0) "${batteryInfo.cycleCount}" else stringResource(id = R.string.common_na), Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SystemStatItem(Icons.Default.Science, stringResource(id = R.string.technology), batteryInfo.technology, Modifier.weight(1f))
                SystemStatItem(if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd, stringResource(id = R.string.status), if (batteryInfo.isCharging) stringResource(id = R.string.charging) else stringResource(id = R.string.discharging), Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SystemStatItem(Icons.Default.Battery6Bar, stringResource(id = R.string.current_cap), if (batteryInfo.currentCapacity > 0) stringResource(id = R.string.mah, batteryInfo.currentCapacity) else stringResource(id = R.string.common_na), Modifier.weight(1f))
                SystemStatItem(Icons.Default.BatterySaver, stringResource(id = R.string.design_cap), if (batteryInfo.capacity > 0) stringResource(id = R.string.mah, batteryInfo.capacity) else stringResource(id = R.string.common_na), Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SystemStatItem(Icons.Default.NightsStay, stringResource(id = R.string.deep_sleep), deepSleepInfo?.let { if (it.uptime > 0) { val pct = (it.deepSleep.toFloat() / it.uptime.toFloat()) * 100; stringResource(id = R.string.deep_sleep_percentage, pct) } else stringResource(id = R.string.deep_sleep_default) } ?: stringResource(id = R.string.common_na), Modifier.weight(1f))
                SystemStatItem(Icons.Default.ScreenLockRotation, stringResource(id = R.string.screen_on), deepSleepInfo?.let { if (it.uptime > 0) formatTimeWithSeconds(it.uptime - it.deepSleep) else stringResource(id = R.string.common_na) } ?: stringResource(id = R.string.common_na), Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SystemStatItem(if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert, stringResource(id = R.string.current), stringResource(id = R.string.ma, (batteryInfo.current / 1000f).toDouble()), Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun formatTimeWithSeconds(timeInMillis: Long): String {
    val totalSeconds = timeInMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}
