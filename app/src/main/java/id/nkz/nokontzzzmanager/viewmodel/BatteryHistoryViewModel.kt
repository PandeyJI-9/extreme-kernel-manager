package id.nkz.nokontzzzmanager.viewmodel

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.BatteryGraphEntry
import id.nkz.nokontzzzmanager.data.model.AppUsageInfo
import id.nkz.nokontzzzmanager.data.repository.BatteryGraphRepository
import id.nkz.nokontzzzmanager.data.repository.BatteryMonitorRepository
import id.nkz.nokontzzzmanager.data.model.BatteryMonitorStats
import id.nkz.nokontzzzmanager.data.model.BatteryStatsSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.graphics.scale

import id.nkz.nokontzzzmanager.utils.PreferenceManager

@HiltViewModel
class BatteryHistoryViewModel @Inject constructor(
    private val repository: BatteryGraphRepository,
    private val batteryMonitorRepository: BatteryMonitorRepository,
    private val preferenceManager: PreferenceManager,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.LAST_24_HOURS)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    private val _isBatteryMonitorEnabled = MutableStateFlow(false)
    val isBatteryMonitorEnabled: StateFlow<Boolean> = _isBatteryMonitorEnabled.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    private val _appUsageList = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val appUsageList: StateFlow<List<AppUsageInfo>> = _appUsageList.asStateFlow()

    private val _monitorStats = MutableStateFlow<BatteryMonitorStats?>(null)
    val monitorStats: StateFlow<BatteryMonitorStats?> = _monitorStats.asStateFlow()

    init {
        viewModelScope.launch {
            filter.collect {
                loadAppUsageStats()
                loadMonitorStats()
            }
        }
        checkBatteryMonitorState()
    }

    fun checkBatteryMonitorState() {
        _isBatteryMonitorEnabled.value = preferenceManager.isBatteryMonitorEnabled()
    }

    fun enableBatteryMonitor() {
         viewModelScope.launch {
             preferenceManager.setBatteryMonitorEnabled(true)
             id.nkz.nokontzzzmanager.service.BatteryMonitorService.start(context)
             _isBatteryMonitorEnabled.value = true
         }
    }

    fun loadMonitorStats() {
        viewModelScope.launch(Dispatchers.IO) {
            // Monitor stats represent the current session (Since Unplugged/Reset).
            // We only show them if the filter matches that context to avoid confusion.
            if (_filter.value == HistoryFilter.SINCE_UNPLUGGED) {
                _monitorStats.value = batteryMonitorRepository.getMonitorStats()
            } else {
                _monitorStats.value = null
            }
        }
    }

    fun checkUsagePermission() {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        _hasUsagePermission.value = mode == AppOpsManager.MODE_ALLOWED
    }

    fun loadAppUsageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            loadMonitorStats() // Ensure monitor stats are refreshed too
            checkUsagePermission()
            if (!_hasUsagePermission.value) return@launch

            val now = System.currentTimeMillis()
            val allEntries = repository.getAllEntries().firstOrNull() ?: emptyList()

            val startTime = when (_filter.value) {
                HistoryFilter.LAST_24_HOURS -> now - 24 * 60 * 60 * 1000
                HistoryFilter.SINCE_UNPLUGGED -> {
                    val lastChargeIndex = allEntries.indexOfLast { it.isCharging }
                    if (lastChargeIndex != -1 && lastChargeIndex < allEntries.size - 1) {
                        allEntries[lastChargeIndex + 1].timestamp
                    } else {
                        now - 24 * 60 * 60 * 1000
                    }
                }
                HistoryFilter.PER_CYCLE -> {
                    val lastFullChargeEntry = allEntries.lastOrNull { it.batteryLevel >= 90 }
                    lastFullChargeEntry?.timestamp ?: (now - 24 * 60 * 60 * 1000)
                }
            }

            // Calculate total discharge in mAh during this period
            val relevantEntries = allEntries.filter { it.timestamp in startTime..now }.sortedBy { it.timestamp }
            var totalPeriodDischargeMah = 0.0

            for (i in 0 until relevantEntries.size - 1) {
                val current = relevantEntries[i]
                val next = relevantEntries[i + 1]
                val dt = next.timestamp - current.timestamp

                // Check for valid uptime continuity (detect reboot)
                if (current.uptime > 0 && next.uptime > 0) {
                    if (next.uptime < current.uptime) continue
                } else {
                    if (dt > 60 * 60 * 1000) continue
                }

                if (current.currentMa < 0) {
                    val avgCurrent = (kotlin.math.abs(current.currentMa) + kotlin.math.abs(next.currentMa)) / 2.0
                    totalPeriodDischargeMah += (avgCurrent * dt) / 3_600_000.0
                }
            }

            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, now)

            if (stats.isNullOrEmpty()) {
                _appUsageList.value = emptyList()
                return@launch
            }

            val aggregatedStats = stats.groupBy { it.packageName }
                .mapValues { (_, entries) -> entries.sumOf { it.totalTimeInForeground } }
                .toList()
                .sortedByDescending { it.second }
            
            val totalUsageTime = aggregatedStats.sumOf { it.second }.coerceAtLeast(1L)
            
            val topApps = aggregatedStats.take(10)

            val pm = context.packageManager
            val iconSizePx = (32 * context.resources.displayMetrics.density).toInt() // Target 32dp

            val appList = topApps.mapNotNull { (packageName, totalTime) ->
                if (totalTime == 0L) return@mapNotNull null
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    
                    // Load and resize icon
                    val originalDrawable = pm.getApplicationIcon(appInfo)
                    val originalBitmap = originalDrawable.toBitmap()
                    val resizedBitmap = if (originalBitmap.width > iconSizePx || originalBitmap.height > iconSizePx) {
                        originalBitmap.scale(iconSizePx, iconSizePx)
                    } else {
                        originalBitmap
                    }

                    // Format time
                    val hours = totalTime / (1000 * 60 * 60)
                    val minutes = (totalTime / (1000 * 60)) % 60
                    val formattedTime = if (hours > 0) {
                        context.getString(R.string.usage_time_format, hours, minutes)
                    } else {
                        context.getString(R.string.usage_time_format_min, minutes)
                    }
                    
                    val percentage = ((totalTime.toDouble() / totalUsageTime.toDouble()) * 100).toInt()
                    val estimatedMah = totalPeriodDischargeMah * (totalTime.toDouble() / totalUsageTime.toDouble())

                    AppUsageInfo(packageName, appName, totalTime, formattedTime, percentage, estimatedMah, resizedBitmap)
                } catch (e: Exception) {
                    null
                }
            }
            _appUsageList.value = appList
        }
    }

    // Get history with filter applied
    val historyData: StateFlow<List<BatteryGraphEntry>> = combine(
        repository.getAllEntries(),
        _filter
    ) { entries, currentFilter ->
        val now = System.currentTimeMillis()
        when (currentFilter) {
            HistoryFilter.LAST_24_HOURS -> {
                val twentyFourHoursAgo = now - (24 * 60 * 60 * 1000)
                entries.filter { it.timestamp >= twentyFourHoursAgo }
            }
            HistoryFilter.SINCE_UNPLUGGED -> {
                // Find the last time it was charging
                val lastChargeIndex = entries.indexOfLast { it.isCharging }
                if (lastChargeIndex != -1) {
                    // Return entries starting AFTER the last charge entry (pure discharge)
                    if (lastChargeIndex + 1 < entries.size) {
                        entries.subList(lastChargeIndex + 1, entries.size)
                    } else {
                        emptyList()
                    }
                } else {
                    // Never charged in history? Show all.
                    entries
                }
            }
            HistoryFilter.PER_CYCLE -> {
                val lastFullChargeIndex = entries.indexOfLast { it.batteryLevel >= 90 }
                if (lastFullChargeIndex != -1) {
                    entries.subList(lastFullChargeIndex, entries.size)
                } else {
                    val twentyFourHoursAgo = now - (24 * 60 * 60 * 1000)
                    entries.filter { it.timestamp >= twentyFourHoursAgo }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val statsSummary: StateFlow<BatteryStatsSummary> = combine(
        repository.getAllEntries(),
        monitorStats,
        filter,
        isBatteryMonitorEnabled
    ) { allEntries, monitorStats, currentFilter, isMonitorEnabled ->
        calculateStats(allEntries, monitorStats, currentFilter, isMonitorEnabled)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = BatteryStatsSummary()
    )

    private fun calculateStats(
        allEntries: List<BatteryGraphEntry>,
        monitorStats: BatteryMonitorStats?,
        currentFilter: HistoryFilter,
        isMonitorEnabled: Boolean
    ): BatteryStatsSummary {
        if (allEntries.isEmpty() && monitorStats == null) return BatteryStatsSummary()

        val now = System.currentTimeMillis()
        val data = when (currentFilter) {
            HistoryFilter.LAST_24_HOURS -> {
                val twentyFourHoursAgo = now - (24 * 60 * 60 * 1000)
                allEntries.filter { it.timestamp >= twentyFourHoursAgo }
            }
            HistoryFilter.SINCE_UNPLUGGED -> {
                // Find the last time it was charging
                val lastChargeIndex = allEntries.indexOfLast { it.isCharging }
                if (lastChargeIndex != -1) {
                    // Return entries starting AFTER the last charge entry (pure discharge)
                    if (lastChargeIndex + 1 < allEntries.size) {
                        allEntries.subList(lastChargeIndex + 1, allEntries.size)
                    } else {
                        emptyList()
                    }
                } else {
                    // Never charged in history? Show all.
                    allEntries
                }
            }
            HistoryFilter.PER_CYCLE -> {
                val lastFullChargeIndex = allEntries.indexOfLast { it.batteryLevel >= 90 }
                if (lastFullChargeIndex != -1) {
                    allEntries.subList(lastFullChargeIndex, allEntries.size)
                } else {
                    val twentyFourHoursAgo = now - (24 * 60 * 60 * 1000)
                    allEntries.filter { it.timestamp >= twentyFourHoursAgo }
                }
            }
        }

        // For charging stats, if SINCE_UNPLUGGED, we want the LAST charging session
        val chargeEntries = if (currentFilter == HistoryFilter.SINCE_UNPLUGGED) {
            val lastChargeIndex = allEntries.indexOfLast { it.isCharging }
            if (lastChargeIndex != -1) {
                var startIndex = lastChargeIndex
                while (startIndex > 0 && allEntries[startIndex - 1].isCharging) {
                    startIndex--
                }
                allEntries.subList(startIndex, lastChargeIndex + 1)
            } else {
                emptyList()
            }
        } else {
            data.filter { it.currentMa > 0 }
        }

        val dischargeEntries = data.filter { it.currentMa < 0 }

        val avgCharge = if (chargeEntries.isNotEmpty()) chargeEntries.map { it.currentMa }.average() else 0.0
        val maxCharge = if (chargeEntries.isNotEmpty()) chargeEntries.maxOf { it.currentMa } else 0f
        val avgChargeTemp = if (chargeEntries.isNotEmpty()) chargeEntries.map { it.temperature.toDouble() }.average() else 0.0
        val maxChargeTemp = if (chargeEntries.isNotEmpty()) chargeEntries.maxOf { it.temperature } else 0f

        val chargeStartLevel = if (currentFilter == HistoryFilter.SINCE_UNPLUGGED && chargeEntries.isNotEmpty()) chargeEntries.first().batteryLevel else 0
        val chargeEndLevel = if (currentFilter == HistoryFilter.SINCE_UNPLUGGED && chargeEntries.isNotEmpty()) chargeEntries.last().batteryLevel else 0
        val chargeDurationMs = if (currentFilter == HistoryFilter.SINCE_UNPLUGGED && chargeEntries.size >= 2) chargeEntries.last().timestamp - chargeEntries.first().timestamp else 0L

        val avgDischarge = if (dischargeEntries.isNotEmpty()) dischargeEntries.map { kotlin.math.abs(it.currentMa) }.average() else 0.0
        val maxDischarge = if (dischargeEntries.isNotEmpty()) dischargeEntries.minOf { it.currentMa }.let { kotlin.math.abs(it) } else 0f
        val avgDischargeTemp = if (dischargeEntries.isNotEmpty()) dischargeEntries.map { it.temperature.toDouble() }.average() else 0.0
        val maxDischargeTemp = if (dischargeEntries.isNotEmpty()) dischargeEntries.maxOf { it.temperature } else 0f

        var avgActiveDrain = 0.0
        var avgIdleDrain = 0.0
        var totalDischargeTimeMs = 0L
        var screenOnTimeMs = 0L
        var screenOffTimeMs = 0L
        var totalAwakeMs = 0L
        var totalDeepSleepMs = 0L

        val useMonitorStats = (currentFilter == HistoryFilter.SINCE_UNPLUGGED && monitorStats != null)

        if (useMonitorStats) {
            avgActiveDrain = monitorStats.activeDrainRate.toDouble()
            avgIdleDrain = monitorStats.idleDrainRate.toDouble()
            screenOnTimeMs = monitorStats.screenOnMs
            screenOffTimeMs = monitorStats.screenOffMs
            totalDischargeTimeMs = screenOnTimeMs + screenOffTimeMs
            totalAwakeMs = monitorStats.awakeMs
            totalDeepSleepMs = monitorStats.deepSleepMs
        } else {
            avgActiveDrain = if (data.isNotEmpty()) data.map { it.activeDrainRate.toDouble() }.average() else 0.0
            avgIdleDrain = if (data.isNotEmpty()) data.map { it.idleDrainRate.toDouble() }.average() else 0.0

            val sortedData = data.sortedBy { it.timestamp }
            for (i in 0 until sortedData.size - 1) {
                val current = sortedData[i]
                val next = sortedData[i + 1]
                val dt = next.timestamp - current.timestamp

                // Check for valid uptime continuity (detect reboot)
                if (current.uptime > 0 && next.uptime > 0) {
                    if (next.uptime < current.uptime) continue
                } else {
                    if (dt > 60 * 60 * 1000) continue
                }

                if (current.currentMa < 0) {
                    totalDischargeTimeMs += dt
                    if (current.isScreenOn) screenOnTimeMs += dt else screenOffTimeMs += dt

                    if (current.uptime > 0 && next.uptime > 0) {
                        val dUptime = next.uptime - current.uptime
                        if (dUptime >= 0) {
                            val segmentAwake = dUptime.coerceAtMost(dt)
                            val segmentSleep = (dt - segmentAwake).coerceAtLeast(0)
                            totalAwakeMs += segmentAwake
                            totalDeepSleepMs += segmentSleep
                        }
                    }
                }
            }
        }

        var totalDischargeMah = 0.0
        var screenOnMah = 0.0
        var screenOffMah = 0.0

        if (data.isNotEmpty()) {
            val sortedData = data.sortedBy { it.timestamp }
            for (i in 0 until sortedData.size - 1) {
                val current = sortedData[i]
                val next = sortedData[i + 1]
                val dt = next.timestamp - current.timestamp

                // Check for valid uptime continuity (detect reboot)
                if (current.uptime > 0 && next.uptime > 0) {
                    if (next.uptime < current.uptime) continue
                } else {
                    if (dt > 60 * 60 * 1000) continue
                }

                if (current.currentMa < 0) {
                    val avgCurrentMa = (kotlin.math.abs(current.currentMa) + kotlin.math.abs(next.currentMa)) / 2.0
                    val mah = (avgCurrentMa * dt) / 3_600_000.0
                    totalDischargeMah += mah
                    if (current.isScreenOn) screenOnMah += mah else screenOffMah += mah
                }
            }
        }

        return BatteryStatsSummary(
            avgChargeCurrent = avgCharge,
            maxChargeCurrent = maxCharge,
            avgChargeTemp = avgChargeTemp,
            maxChargeTemp = maxChargeTemp,
            avgDischargeCurrent = avgDischarge,
            maxDischargeCurrent = maxDischarge,
            avgDischargeTemp = avgDischargeTemp,
            maxDischargeTemp = maxDischargeTemp,
            activeDrainRate = avgActiveDrain,
            idleDrainRate = avgIdleDrain,
            totalDischargeTimeMs = totalDischargeTimeMs,
            screenOnTimeMs = screenOnTimeMs,
            screenOffTimeMs = screenOffTimeMs,
            totalAwakeMs = totalAwakeMs,
            totalDeepSleepMs = totalDeepSleepMs,
            totalDischargeMah = totalDischargeMah,
            screenOnMah = screenOnMah,
            screenOffMah = screenOffMah,
            chargeStartLevel = chargeStartLevel,
            chargeEndLevel = chargeEndLevel,
            chargeDurationMs = chargeDurationMs,
            isSyncedWithMonitor = useMonitorStats && isMonitorEnabled
        )
    }

    fun setFilter(newFilter: HistoryFilter) {
        _filter.value = newFilter
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.deleteAllEntries()
        }
    }
}

enum class HistoryFilter {
    LAST_24_HOURS,
    SINCE_UNPLUGGED,
    PER_CYCLE
}
