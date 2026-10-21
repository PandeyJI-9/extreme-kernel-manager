package id.nkz.nokontzzzmanager.ui.screens
import id.nkz.nokontzzzmanager.ui.components.*

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.ui.dialog.TcpCongestionDialog
import id.nkz.nokontzzzmanager.viewmodel.MiscViewModel
import id.nkz.nokontzzzmanager.ui.dialog.IoSchedulerDialog
import id.nkz.nokontzzzmanager.ui.dialog.BatteryHistoryConfigDialog
import id.nkz.nokontzzzmanager.ui.dialog.ChargingControlDialog

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import id.nkz.nokontzzzmanager.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.core.net.toUri

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscScreen(
    navController: NavController? = null,
    viewModel: MiscViewModel = hiltViewModel(),
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    val kgslSkipZeroingEnabled by viewModel.kgslSkipZeroingEnabled.collectAsStateWithLifecycle()
    val isKgslFeatureAvailable by viewModel.isKgslFeatureAvailable.collectAsStateWithLifecycle()
    val avoidDirtyPteEnabled by viewModel.avoidDirtyPteEnabled.collectAsStateWithLifecycle()
    val isAvoidDirtyPteAvailable by viewModel.isAvoidDirtyPteAvailable.collectAsStateWithLifecycle()
    val bypassChargingEnabled by viewModel.bypassChargingEnabled.collectAsStateWithLifecycle()
    val isBypassChargingAvailable by viewModel.isBypassChargingAvailable.collectAsStateWithLifecycle()
    val forceFastChargeEnabled by viewModel.forceFastChargeEnabled.collectAsStateWithLifecycle()
    val isForceFastChargeAvailable by viewModel.isForceFastChargeAvailable.collectAsStateWithLifecycle()
    val batteryMonitorEnabled by viewModel.batteryMonitorEnabled.collectAsStateWithLifecycle()
    val tcpCongestionAlgorithm by viewModel.tcpCongestionAlgorithm.collectAsStateWithLifecycle()
    val availableTcpAlgorithms by viewModel.availableTcpCongestionAlgorithms.collectAsStateWithLifecycle()
    val ioScheduler by viewModel.ioScheduler.collectAsStateWithLifecycle()
    val availableIoSchedulers by viewModel.availableIoSchedulers.collectAsStateWithLifecycle()
    val applyNetworkStorageOnBoot by viewModel.applyNetworkStorageOnBoot.collectAsStateWithLifecycle()

    val autoResetOnReboot by viewModel.autoResetOnReboot.collectAsStateWithLifecycle()
    val autoResetOnCharging by viewModel.autoResetOnCharging.collectAsStateWithLifecycle()
    val autoResetAtLevel by viewModel.autoResetAtLevel.collectAsStateWithLifecycle()
    val autoResetTargetLevel by viewModel.autoResetTargetLevel.collectAsStateWithLifecycle()

    val monitorAutoResetOnReboot by viewModel.monitorAutoResetOnReboot.collectAsStateWithLifecycle()
    val monitorAutoResetOnCharging by viewModel.monitorAutoResetOnCharging.collectAsStateWithLifecycle()
    val monitorAutoResetAtLevel by viewModel.monitorAutoResetAtLevel.collectAsStateWithLifecycle()
    val monitorAutoResetTargetLevel by viewModel.monitorAutoResetTargetLevel.collectAsStateWithLifecycle()

    val chargingControlEnabled by viewModel.chargingControlEnabled.collectAsStateWithLifecycle()
    val chargingControlStopLevel by viewModel.chargingControlStopLevel.collectAsStateWithLifecycle()
    val chargingControlResumeLevel by viewModel.chargingControlResumeLevel.collectAsStateWithLifecycle()
    val batteryInfo by viewModel.batteryInfo.collectAsStateWithLifecycle()

    var showAutoResetDialog by remember { mutableStateOf(false) }
    var showMonitorAutoResetDialog by remember { mutableStateOf(false) }
    var showChargingControlDialog by remember { mutableStateOf(false) }

    if (showAutoResetDialog) {
        BatteryHistoryConfigDialog(
            onDismiss = { showAutoResetDialog = false },
            resetOnReboot = autoResetOnReboot,
            onResetOnRebootChange = viewModel::setAutoResetOnReboot,
            resetOnCharging = autoResetOnCharging,
            onResetOnChargingChange = viewModel::setAutoResetOnCharging,
            resetAtLevel = autoResetAtLevel,
            onResetAtLevelChange = viewModel::setAutoResetAtLevel,
            targetLevel = autoResetTargetLevel,
            onTargetLevelChange = viewModel::setAutoResetTargetLevel
        )
    }

    if (showMonitorAutoResetDialog) {
        BatteryHistoryConfigDialog(
            onDismiss = { showMonitorAutoResetDialog = false },
            title = stringResource(R.string.battery_monitor_config_title),
            description = stringResource(R.string.battery_monitor_config_desc),
            resetOnReboot = monitorAutoResetOnReboot,
            onResetOnRebootChange = viewModel::setMonitorAutoResetOnReboot,
            resetOnCharging = monitorAutoResetOnCharging,
            onResetOnChargingChange = viewModel::setMonitorAutoResetOnCharging,
            resetAtLevel = monitorAutoResetAtLevel,
            onResetAtLevelChange = viewModel::setMonitorAutoResetAtLevel,
            targetLevel = monitorAutoResetTargetLevel,
            onTargetLevelChange = viewModel::setMonitorAutoResetTargetLevel
        )
    }

    if (showChargingControlDialog) {
        ChargingControlDialog(
            onDismiss = { showChargingControlDialog = false },
            enabled = chargingControlEnabled,
            onEnabledChange = viewModel::setChargingControlEnabled,
            stopLevel = chargingControlStopLevel,
            onStopLevelChange = viewModel::setChargingControlStopLevel,
            resumeLevel = chargingControlResumeLevel,
            onResumeLevelChange = viewModel::setChargingControlResumeLevel,
            batteryInfo = batteryInfo,
            isBypassActive = bypassChargingEnabled
        )
    }

    val context = LocalContext.current

    val checkBatteryOptimizationAndEnable = {
        val pm = context.getSystemService(PowerManager::class.java)
        if (pm != null && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
            try {
                val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = ("package:" + context.packageName).toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(i)
            } catch (_: Exception) { }
        }
        viewModel.toggleBatteryMonitor(true)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkBatteryOptimizationAndEnable()
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 0.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text(stringResource(id = R.string.kernel), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            CustomTunableEntryCard(
                onClick = { navController?.navigate("custom_tunable") },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
            )
        }
        item {
            TcpCongestionControlCard(
                tcpCongestionAlgorithm = tcpCongestionAlgorithm,
                availableAlgorithms = availableTcpAlgorithms,
                onAlgorithmChange = { viewModel.updateTcpCongestionAlgorithm(it) },
                shape = RoundedCornerShape(8.dp)
            )
        }
        item {
            IoSchedulerCard(
                ioScheduler = ioScheduler,
                availableSchedulers = availableIoSchedulers,
                onSchedulerChange = { viewModel.updateIoScheduler(it) },
                shape = RoundedCornerShape(8.dp)
            )
        }
        item {
            NetworkStorageOnBootCard(
                applyOnBoot = applyNetworkStorageOnBoot,
                onToggle = { viewModel.setApplyNetworkStorageOnBoot(it) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(stringResource(id = R.string.gpu_power), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            KgslSkipZeroingCard(
                kgslSkipZeroingEnabled = kgslSkipZeroingEnabled,
                isKgslFeatureAvailable = isKgslFeatureAvailable,
                onToggleKgslSkipZeroing = { viewModel.toggleKgslSkipZeroing(it) }
            )
        }
        item {
            AvoidDirtyPteCard(
                avoidDirtyPteEnabled = avoidDirtyPteEnabled,
                isAvoidDirtyPteAvailable = isAvoidDirtyPteAvailable,
                onToggleAvoidDirtyPte = { viewModel.toggleAvoidDirtyPte(it) }
            )
        }
        item {
            BypassChargingCard(
                bypassChargingEnabled = bypassChargingEnabled,
                isBypassChargingAvailable = isBypassChargingAvailable,
                isChargingControlEnabled = chargingControlEnabled,
                onToggleBypassCharging = { viewModel.toggleBypassCharging(it) }
            )
        }
        item {
            ForceFastChargeCard(
                forceFastChargeEnabled = forceFastChargeEnabled,
                isForceFastChargeAvailable = isForceFastChargeAvailable,
                onToggleForceFastCharge = { viewModel.toggleForceFastCharge(it) }
            )
        }
        item {
            ChargingControlCard(
                enabled = chargingControlEnabled,
                isBatteryMonitorEnabled = batteryMonitorEnabled,
                isBypassChargingEnabled = bypassChargingEnabled,
                onClick = { showChargingControlDialog = true }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(stringResource(id = R.string.battery_settings), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            BatteryMonitorCard(
                enabled = batteryMonitorEnabled,
                onToggle = { enabled ->
                    if (enabled) {
                        var hasPermission = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                hasPermission = false
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        if (hasPermission) checkBatteryOptimizationAndEnable()
                    } else {
                        viewModel.toggleBatteryMonitor(false)
                    }
                }
            )
        }
        item {
            BatteryMonitorAutoResetCard(onClick = { showMonitorAutoResetDialog = true })
        }
        item {
            BatteryMonitorResetCard(onReset = { viewModel.resetBatteryMonitor() }, onEnsurePermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            })
        }
        item {
            BatteryHistoryCard(onClick = { navController?.navigate("battery_history") }, onSettingsClick = { showAutoResetDialog = true })
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(stringResource(id = R.string.automation_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item { AppProfilesCard(onClick = { navController?.navigate("app_profiles") }) }
        item { FpsMonitorCard(onClick = { navController?.navigate("fps_monitor") }) }
        item { BgBlockerCard(onClick = { navController?.navigate("bg_blocker") }) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(stringResource(id = R.string.system_stats_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item { ProcessMonitorCard(onClick = { navController?.navigate("process_monitor") }) }
        item { DexoptCard(onClick = { navController?.navigate("dexopt") }) }
        item { WakelockCard(onClick = { navController?.navigate("wakelock_monitor") }) }
        item { KernelLogCard(onClick = { navController?.navigate("kernel_log") }) }
    }
}

