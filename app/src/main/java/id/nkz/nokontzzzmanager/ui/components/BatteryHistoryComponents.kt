package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.BatteryGraphEntry
import id.nkz.nokontzzzmanager.data.model.AppUsageInfo
import id.nkz.nokontzzzmanager.data.model.BatteryStatsSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(4.dp)))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

enum class BatteryGraphMode {
    SPEED, DRAIN
}

@Composable
fun BatteryHistoryGraph(
    data: List<BatteryGraphEntry>,
    mode: BatteryGraphMode,
    primaryColor: Color,
    secondaryColor: Color
) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_data_available), style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val minTime = data.minOf { it.timestamp }
    val maxTime = data.maxOf { it.timestamp }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val (minY, maxY, labelSuffix) = if (mode == BatteryGraphMode.SPEED) {
        val min = data.minOf { it.currentMa }.coerceAtMost(0f)
        val max = data.maxOf { it.currentMa }.coerceAtLeast(0f)
        Triple(min, max, "mA")
    } else {
        val max = data.maxOf { maxOf(it.activeDrainRate, it.idleDrainRate) }.coerceAtLeast(1f)
        Triple(0f, max, "%/hr")
    }

    val steps = 4
    val yLabels = List(steps + 1) { i ->
        minY + (maxY - minY) * i / steps
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val yAxisWidth = 48.dp
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 24.dp)
                    .width(yAxisWidth),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yLabels.reversed().forEachIndexed { index, yVal ->
                    Text(
                        text = if (index == 0) "${"%.0f".format(yVal)} $labelSuffix" else "%.0f".format(yVal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(modifier = Modifier.weight(1f)) {
                Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) {
                    val width = size.width
                    val height = size.height
                    val timeRange = (maxTime - minTime).coerceAtLeast(1L)
                    val vRange = (maxY - minY).coerceAtLeast(1f)

                    // Draw Grid Lines
                    yLabels.forEach { yVal ->
                        val y = height * (1 - (yVal - minY) / vRange)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }

                    if (mode == BatteryGraphMode.SPEED) {
                        // Draw Zero Line
                        val zeroY = height * (1 - (0f - minY) / vRange)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.4f),
                            start = Offset(0f, zeroY),
                            end = Offset(width, zeroY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )

                        val chargePath = Path()
                        val dischargePath = Path()

                        data.forEachIndexed { index, entry ->
                            val x = width * (entry.timestamp - minTime) / timeRange
                            
                            // Charge Path (Positive current, else 0)
                            val chargeVal = entry.currentMa.coerceAtLeast(0f)
                            val chargeY = height * (1 - (chargeVal - minY) / vRange)
                            if (index == 0) chargePath.moveTo(x, chargeY) else chargePath.lineTo(x, chargeY)

                            // Discharge Path (Negative current, else 0)
                            val dischargeVal = entry.currentMa.coerceAtMost(0f)
                            val dischargeY = height * (1 - (dischargeVal - minY) / vRange)
                            if (index == 0) dischargePath.moveTo(x, dischargeY) else dischargePath.lineTo(x, dischargeY)
                        }

                        drawPath(
                            path = chargePath,
                            color = primaryColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = dischargePath,
                            color = secondaryColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                    } else {
                        // DRAIN MODE
                        val activePath = Path()
                        val idlePath = Path()
                        
                        data.forEachIndexed { index, entry ->
                            val x = width * (entry.timestamp - minTime) / timeRange
                            
                            val activeY = height * (1 - entry.activeDrainRate / vRange)
                            if (index == 0) activePath.moveTo(x, activeY) else activePath.lineTo(x, activeY)
                            
                            val idleY = height * (1 - entry.idleDrainRate / vRange)
                            if (index == 0) idlePath.moveTo(x, idleY) else idlePath.lineTo(x, idleY)
                        }
                        
                        drawPath(
                            path = activePath,
                            color = primaryColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = idlePath,
                            color = secondaryColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }
        
        // Time Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(52.dp)) // Aligns with Y-axis column (48 + 4)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = timeFormat.format(Date(minTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeFormat.format(Date(maxTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BatteryHistoryStatsCard(
    stats: BatteryStatsSummary,
    shape: Shape = CardDefaults.shape
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_stats_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Sync Status Badge
                if (stats.isSyncedWithMonitor) {
                    val badgeColor = MaterialTheme.colorScheme.primary
                    
                    Surface(
                        color = badgeColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = stringResource(R.string.stats_synced),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            if (stats.totalDischargeTimeMs > 0) {
                fun formatUsage(timeMs: Long, mah: Double): String {
                    val pct = if (stats.totalDischargeMah > 0) (mah / stats.totalDischargeMah * 100).toInt() else 0
                    val duration = formatDuration(timeMs)
                    return "$duration • $pct% (${mah.toInt()} mAh)"
                }

                // Group: Usage Time
                val usageItems = listOf(
                    stringResource(R.string.stats_total_active_time) to formatUsage(stats.totalDischargeTimeMs, stats.totalDischargeMah),
                    stringResource(R.string.stats_screen_on_time) to formatUsage(stats.screenOnTimeMs, stats.screenOnMah),
                    stringResource(R.string.stats_screen_off_time) to formatUsage(stats.screenOffTimeMs, stats.screenOffMah)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    usageItems.forEachIndexed { index, item ->
                        StatItemCard(
                            label = item.first,
                            value = item.second,
                            index = index,
                            totalCount = usageItems.size
                        )
                    }
                }

                if (stats.totalAwakeMs > 0 || stats.totalDeepSleepMs > 0) {
                    val totalTracked = (stats.totalAwakeMs + stats.totalDeepSleepMs).coerceAtLeast(1L)
                    val awakePct = (stats.totalAwakeMs * 100 / totalTracked)
                    val sleepPct = (stats.totalDeepSleepMs * 100 / totalTracked)

                    val uptimeItems = listOf(
                        stringResource(R.string.uptime) to "${formatDuration(stats.totalAwakeMs)} ($awakePct%)",
                        stringResource(R.string.deep_sleep) to "${formatDuration(stats.totalDeepSleepMs)} ($sleepPct%)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        uptimeItems.forEachIndexed { index, item ->
                            StatItemCard(
                                label = item.first,
                                value = item.second,
                                index = index,
                                totalCount = uptimeItems.size
                            )
                        }
                    }
                }
            }

            if (stats.chargeStartLevel > 0 || stats.chargeEndLevel > 0) {
                val sessionItems = listOf(
                    stringResource(R.string.stats_charge_range) to "${stats.chargeStartLevel}% -> ${stats.chargeEndLevel}%",
                    stringResource(R.string.stats_charge_duration) to formatDuration(stats.chargeDurationMs)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    sessionItems.forEachIndexed { index, item ->
                        StatItemCard(
                            label = item.first,
                            value = item.second,
                            index = index,
                            totalCount = sessionItems.size
                        )
                    }
                }
            }

            if (stats.avgChargeCurrent > 0) {
                val chargeItems = listOf(
                    stringResource(R.string.stats_avg_charge_speed) to "%.0f mA".format(stats.avgChargeCurrent),
                    stringResource(R.string.stats_max_charge_speed) to "%.0f mA".format(stats.maxChargeCurrent),
                    stringResource(R.string.stats_avg_charge_temp) to "%.1f °C".format(stats.avgChargeTemp),
                    stringResource(R.string.stats_max_charge_temp) to "%.1f °C".format(stats.maxChargeTemp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    chargeItems.forEachIndexed { index, item ->
                        StatItemCard(
                            label = item.first,
                            value = item.second,
                            index = index,
                            totalCount = chargeItems.size
                        )
                    }
                }
            }
            
            if (stats.avgDischargeCurrent > 0) {
                val dischargeItems = listOf(
                    stringResource(R.string.stats_avg_discharge_speed) to "-%.0f mA".format(stats.avgDischargeCurrent),
                    stringResource(R.string.stats_max_discharge_speed) to "-%.0f mA".format(stats.maxDischargeCurrent),
                    stringResource(R.string.stats_avg_discharge_temp) to "%.1f °C".format(stats.avgDischargeTemp),
                    stringResource(R.string.stats_max_discharge_temp) to "%.1f °C".format(stats.maxDischargeTemp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    dischargeItems.forEachIndexed { index, item ->
                        StatItemCard(
                            label = item.first,
                            value = item.second,
                            index = index,
                            totalCount = dischargeItems.size
                        )
                    }
                }
            }
            
            val drainItems = listOf(
                stringResource(R.string.stats_avg_active_drain) to "%.2f %%/hr".format(stats.activeDrainRate),
                stringResource(R.string.stats_avg_idle_drain) to "%.2f %%/hr".format(stats.idleDrainRate)
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                drainItems.forEachIndexed { index, item ->
                    StatItemCard(
                        label = item.first,
                        value = item.second,
                        index = index,
                        totalCount = drainItems.size
                    )
                }
            }
        }
    }
}

@Composable
fun StatItemCard(
    label: String,
    value: String,
    index: Int,
    totalCount: Int
) {
    val shape = when {
        totalCount == 1 -> RoundedCornerShape(12.dp)
        index == 0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == totalCount - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
        else -> RoundedCornerShape(4.dp)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun AppUsageCard(
    appUsageList: List<AppUsageInfo>,
    hasPermission: Boolean,
    onGrantPermission: () -> Unit,
    shape: Shape
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.app_usage_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!hasPermission) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.usage_permission_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Button(onClick = onGrantPermission) {
                        Text(text = stringResource(R.string.grant_usage_permission))
                    }
                }
            } else if (appUsageList.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_data_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    appUsageList.forEach { app ->
                        AppUsageItem(app)
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageItem(app: AppUsageInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
            )
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Top Row: Name and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = app.formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(64.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            // Bottom Row: Progress Bar and Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { app.usagePercentage / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Text(
                    text = "${app.usagePercentage}% (${"%.1f".format(app.powerConsumptionMah)} mAh)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}
