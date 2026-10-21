package id.nkz.nokontzzzmanager.ui.screens
import id.nkz.nokontzzzmanager.ui.dialog.*
import id.nkz.nokontzzzmanager.ui.components.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.BatteryGraphEntry
import id.nkz.nokontzzzmanager.ui.MainActivity
import id.nkz.nokontzzzmanager.viewmodel.BatteryHistoryViewModel
import id.nkz.nokontzzzmanager.viewmodel.HistoryFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import id.nkz.nokontzzzmanager.data.model.AppUsageInfo
import id.nkz.nokontzzzmanager.data.model.BatteryMonitorStats
import id.nkz.nokontzzzmanager.data.model.BatteryStatsSummary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteryHistoryScreen(
    navController: NavController,
    viewModel: BatteryHistoryViewModel = hiltViewModel()
) {
    val historyData by viewModel.historyData.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val appUsageList by viewModel.appUsageList.collectAsState()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsState()
    val isBatteryMonitorEnabled by viewModel.isBatteryMonitorEnabled.collectAsState()
    val statsSummary by viewModel.statsSummary.collectAsState()
    
    var graphMode by remember { mutableStateOf(BatteryGraphMode.SPEED) }
    var showClearDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mainActivity = remember(context) { context as? MainActivity }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadAppUsageStats()
                viewModel.checkBatteryMonitorState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        mainActivity?.batteryHistoryFabVisible?.value = true
        mainActivity?.batteryHistoryFabAction?.value = { showClearDialog = true }
        viewModel.loadAppUsageStats()
        viewModel.checkBatteryMonitorState()
    }

    DisposableEffect(Unit) {
        onDispose {
            mainActivity?.batteryHistoryFabVisible?.value = false
            mainActivity?.batteryHistoryFabAction?.value = null
        }
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = {
                viewModel.clearHistory()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isBatteryMonitorEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.battery_monitor_disabled_warning),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.battery_monitor_disabled_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { viewModel.enableBatteryMonitor() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.enable_battery_monitor))
                        }
                    }
                }
            }

            // Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val modes = listOf(BatteryGraphMode.SPEED, BatteryGraphMode.DRAIN)
                modes.forEachIndexed { index, mode ->
                    val isSelected = graphMode == mode
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { graphMode = mode },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) {
                        Icon(
                            imageVector = if (mode == BatteryGraphMode.SPEED) Icons.Default.Speed else Icons.Default.BatteryStd,
                            contentDescription = null,
                            modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(
                            text = if (mode == BatteryGraphMode.SPEED) stringResource(R.string.graph_mode_speed_short) else stringResource(R.string.graph_mode_drain)
                        )
                    }
                }
            }

            // Graph Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val filterText = when (currentFilter) {
                        HistoryFilter.LAST_24_HOURS -> stringResource(R.string.filter_last_24h_short)
                        HistoryFilter.SINCE_UNPLUGGED -> stringResource(R.string.filter_since_unplugged)
                        HistoryFilter.PER_CYCLE -> stringResource(R.string.filter_per_cycle)
                    }
                    Text(
                        text = if (graphMode == BatteryGraphMode.SPEED) 
                            stringResource(R.string.graph_title_current, filterText)
                        else 
                            stringResource(R.string.graph_title_drain, filterText),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (graphMode == BatteryGraphMode.SPEED) {
                            LegendItem(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.legend_charge_speed))
                            LegendItem(color = MaterialTheme.colorScheme.tertiary, label = stringResource(R.string.legend_discharge_speed))
                        } else {
                            LegendItem(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.legend_active_drain))
                            LegendItem(color = MaterialTheme.colorScheme.tertiary, label = stringResource(R.string.legend_idle_drain))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    BatteryHistoryGraph(
                        data = historyData,
                        mode = graphMode,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Time Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val filters = listOf(HistoryFilter.LAST_24_HOURS, HistoryFilter.SINCE_UNPLUGGED, HistoryFilter.PER_CYCLE)
                filters.forEachIndexed { index, filterOption ->
                    val isSelected = currentFilter == filterOption
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { viewModel.setFilter(filterOption) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            filters.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) {
                        Text(
                            text = when (filterOption) {
                                HistoryFilter.LAST_24_HOURS -> stringResource(R.string.filter_last_24h)
                                HistoryFilter.SINCE_UNPLUGGED -> stringResource(R.string.filter_since_unplugged)
                                HistoryFilter.PER_CYCLE -> stringResource(R.string.filter_per_cycle)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Stats Card
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BatteryHistoryStatsCard(
                    stats = statsSummary,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                )

                // App Usage Card
                AppUsageCard(
                    appUsageList = appUsageList,
                    hasPermission = hasUsagePermission,
                    onGrantPermission = { 
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                )
            }
            
            // Extra spacer for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
