package id.nkz.nokontzzzmanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.util.Log
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import id.nkz.nokontzzzmanager.data.repository.BatteryGraphRepository
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.data.database.BatteryGraphEntry
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import androidx.core.graphics.createBitmap

@AndroidEntryPoint
class BatteryMonitorService : Service() {

    @Inject
    lateinit var batteryGraphRepository: BatteryGraphRepository

    @Inject
    lateinit var systemRepository: SystemRepository

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private var batteryManager: BatteryManager? = null
    private var serviceStartedAtMs: Long = 0L

    private val channelId = "battery_monitor_channel_v2"
    private val notificationId = 1001

    private var lastUpdate = System.currentTimeMillis()
    private var lastHistorySaveTime = 0L
    private var lastCleanupTime = 0L
    private val HISTORY_RETENTION_DAYS = 7
    private val CLEANUP_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    private var lastCurrent = 0f
    private var designCapacityUah = 0L // autodetected; no hardcoded fallback
    private var realtimeUsedMah = 0.0
    // Legacy baseline fields (no longer used for drain; kept previously for transitions) removed
    // Cumulative consumption tracking
    private var lastSampleElapsedMs: Long = 0L
    private var lastSampleChargeUah: Long = 0L
    private var consumedOnUah: Long = 0L
    private var consumedOffUah: Long = 0L
    private var prevInteractiveForSample: Boolean? = null
    private var lastSamplePercent: Double = Double.NaN
    private var onPercentDrop: Double = 0.0
    private var offPercentDrop: Double = 0.0

    private var hasResetForThisChargeCycle = false

    private var isRunning = false
    private var monitoringJob: Job? = null
    private var lastManualUpdateTime = 0L
    private val MANUAL_UPDATE_THROTTLE_MS = 2000L
    private val MIN_DRAIN_WINDOW_MS = 10 * 60 * 1000L // 10 minutes minimum for stable drain rate
    
    // Screen on-time tracking
    private var screenReceiver: BroadcastReceiver? = null
    private var screenOnAccumMs: Long = 0L
    private var screenOnStartAtElapsed: Long? = null
    private var powerReceiver: BroadcastReceiver? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var windowStartElapsed: Long = -1L
    private var windowStartUptime: Long = -1L
    private var windowAccumMs: Long = 0L // Accumulated window time from previous sessions (for reboot persistence)
    private var awakeAccumMs: Long = 0L // Accumulated awake (uptime) from previous sessions
    private var nextDelayOverrideMs: Long? = null
    
    // Cache for Charging Control to reduce File I/O
    private var lastKnownBypassState: Boolean? = null

    // Persistence
    private val prefs by lazy {
        if (!isDeviceProtectedStorage) {
            createDeviceProtectedStorageContext().getSharedPreferences("battery_monitor_prefs",
                MODE_PRIVATE
            )
        } else {
            getSharedPreferences("battery_monitor_prefs", MODE_PRIVATE)
        }
    }
    private val keyScreenAccum = "screen_accum_ms"
    private val keyLastElapsed = "last_elapsed_ms"
    private val keyLastUptime = "last_uptime_ms"

    override fun onCreate() {
        super.onCreate()
        Log.d("BatteryMonitorService", "onCreate")
        serviceStartedAtMs = System.currentTimeMillis()
        batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        val initialNotification = createNotification("Starting...", "Collecting battery data...")
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(notificationId, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else
                startForeground(notificationId, initialNotification)
        } catch (e: Exception) {
            Log.e("BatteryMonitorService", "Failed to start foreground service", e)
            stopSelf()
        }
        designCapacityUah = getDesignCapacityUah()
        restoreStateIfAny()
        initScreenTracking()
        initPowerTracking()
        initBatteryTracking()
        startMonitoring()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w("BatteryMonitorService", "onTrimMemory: level=$level")
    }

    private fun computeCurrentBatteryPercent(stickyLevel: Int, chargeUah: Long): Double {
        // Combination: use OS level as base, use coulomb counter for fractional precision
        if (chargeUah > 0L && designCapacityUah > 0L) {
            val calc = (chargeUah.toDouble() * 100.0) / designCapacityUah.toDouble()
            // Ensure calculated percent is reasonably close to OS level to avoid jumps
            if (stickyLevel >= 0) {
                if (kotlin.math.abs(calc - stickyLevel) < 2.0) {
                    return calc
                }
            } else {
                return calc
            }
        }
        if (stickyLevel >= 0) return stickyLevel.toDouble()
        return 0.0
    }

    private fun formatPercent(percent: Double, hasData: Boolean): String {
        if (!hasData) return String.format(Locale.US, "%.1f", 0.0)
        return if (percent >= 99.995) String.format(Locale.US, "%.1f", 100.0)
        else String.format(Locale.US, "%.2f", percent)
    }

    private fun readFirstLong(paths: List<String>): Long? {
        for (p in paths) {
            try {
                val s = File(p).readText().trim()
                if (s.isNotEmpty()) {
                    val v = s.toLong()
                    return v
                }
            } catch (_: Exception) { }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BatteryMonitorService", "onDestroy")
        isRunning = false
        monitoringJob?.cancel()
        scope.cancel()
        try {
            screenReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) { }
        try {
            powerReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) { }
        try {
            batteryReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) { }
        persistState(sync = true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BatteryMonitorService", "onStartCommand: action=${intent?.action}")
        if (intent?.action == ACTION_RESET) {
            try {
                manualReset()
                triggerManualUpdate(immediate = true)
            } catch (e: Exception) {
                Log.e("BatteryMonitorService", "Reset failed", e)
            }
        } else if (intent?.action == ACTION_BOOT_START) {
            // History auto-reset
            if (preferenceManager.isAutoResetOnReboot()) {
                if (isUserUnlocked(this)) {
                    scope.launch { try { batteryGraphRepository.deleteAllEntries() } catch (_: Exception) { } }
                }
            }
            // Monitor auto-reset (Active Stats)
            if (preferenceManager.isMonitorAutoResetOnReboot()) {
                manualReset()
            }
            startMonitoring()
            triggerManualUpdate(immediate = true)
        } else if (intent?.action == ACTION_UPDATE_ICON) {
            startMonitoring() // Ensure loop is running
            triggerManualUpdate(immediate = true)
        } else {
            startMonitoring()
            triggerManualUpdate(immediate = false)
        }
        return START_STICKY
    }

    // === MAIN LOOP ===
    private fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        isRunning = true
        Log.d("BatteryMonitorService", "Starting monitoring loop")

        val pm = getSystemService(POWER_SERVICE) as PowerManager

        monitoringJob = scope.launch(Dispatchers.Default) {
            // Initial update
            try {
                val stats = withContext(Dispatchers.IO) { collectSystemStats() }
                updateNotification(stats)
            } catch (e: Exception) {
                Log.e("BatteryMonitorService", "Initial update failed", e)
            }

            while (isRunning) {
                try {
                    Log.d("BatteryMonitorService", "Heartbeat: Loop tick (Interactive=${pm.isInteractive})")
                    val stats = withContext(Dispatchers.IO) { collectSystemStats() }
                    updateNotification(stats)
                } catch (e: Exception) {
                    Log.e("BatteryMonitorService", "Error in monitoring loop", e)
                }

                // Adaptive delay: 5s if screen on, 60s if screen off
                val d = nextDelayOverrideMs ?: if (pm.isInteractive) 5_000L else 60_000L
                nextDelayOverrideMs = null
                delay(d)
            }
        }
    }

    private fun triggerManualUpdate(immediate: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!immediate && now - lastManualUpdateTime < MANUAL_UPDATE_THROTTLE_MS) return
        lastManualUpdateTime = now
        
        scope.launch(Dispatchers.IO) {
            try {
                Log.d("BatteryMonitorService", "Triggering manual update (immediate=$immediate)")
                val stats = collectSystemStats()
                updateNotification(stats)
            } catch (e: Exception) {
                Log.e("BatteryMonitorService", "Manual update failed", e)
            }
        }
    }

    private fun initBatteryTracking() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    triggerManualUpdate(immediate = false)
                }
            }
        }
        batteryReceiver = receiver
        registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    // === BATTERY LOGIC ===
    private fun collectSystemStats(): BatteryData {
        val bm = batteryManager ?: return BatteryData()

        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0

        checkAutoResetAtLevel(level, charging)
        checkChargingControl(level, plugged > 0)

        // Prefer sysfs for current/voltage; fallback to BatteryManager
        val currentUaSys = readFirstLong(listOf(
            "/sys/class/power_supply/battery/current_now",
            "/sys/class/power_supply/battery/Battery_Current",
            "/sys/class/power_supply/battery/batt_current_now"
        ))
        val voltageUvSys = readFirstLong(listOf(
            "/sys/class/power_supply/battery/voltage_now",
            "/sys/class/power_supply/battery/batt_voltage_now"
        ))

        val currentUa = currentUaSys ?: bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toLong()
        val voltageV = (voltageUvSys?.toDouble()?.div(1_000_000.0))
            ?: ((intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0).toDouble() / 1000.0)
        val powerUwSys = readFirstLong(listOf(
            "/sys/class/power_supply/battery/power_avg",
            "/sys/class/power_supply/battery/power_now"
        ))

        // Map sign so: charging -> positive mA, discharging -> negative mA
        val rawMa = currentUa.toFloat() / 1000f
        var currentMa = rawMa
        if (charging && rawMa < 0f) currentMa = -rawMa
        if (!charging && rawMa > 0f) currentMa = -rawMa

        if (lastCurrent != 0f)
            currentMa = (currentMa * 0.6f) + (lastCurrent * 0.4f)
        lastCurrent = currentMa

        val computedWatt = kotlin.math.abs((voltageV * (currentMa.toDouble() / 1000.0)).toFloat())
        val powerWatt = when {
            powerUwSys != null && powerUwSys != 0L -> kotlin.math.abs(powerUwSys.toFloat() / 1_000_000f)
            else -> computedWatt
        }

        updateRealtimeUsed(currentMa)

        val nowElapsed = SystemClock.elapsedRealtime()
        val nowUptime = SystemClock.uptimeMillis()

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val interactive = pm.isInteractive

        val currentCharge = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        // Attribute consumption since last sample to previous interactive state
        if (!charging && lastSampleElapsedMs != 0L) {
            val dt = nowElapsed - lastSampleElapsedMs
            if (dt > 0) {
                val deltaUah = (lastSampleChargeUah - currentCharge)
                // Sanity check: ignore charging deltas if misreported as discharging
                val safeDeltaUah = if (deltaUah >= 0) deltaUah else 0L
                
                val prev = prevInteractiveForSample ?: interactive
                if (prev) consumedOnUah += safeDeltaUah else consumedOffUah += safeDeltaUah
                
                // Also accumulate percent drop
                val currentPercent = computeCurrentBatteryPercent(level, currentCharge)
                if (!lastSamplePercent.isNaN()) {
                    val dPct = (lastSamplePercent - currentPercent)
                    // Sanity check: ignore negative drops (charging)
                    val safeDPct = if (dPct >= 0) dPct else 0.0
                    if (prev) onPercentDrop += safeDPct else offPercentDrop += safeDPct
                }
                lastSamplePercent = currentPercent
            }
        }
        lastSampleElapsedMs = nowElapsed
        lastSampleChargeUah = currentCharge
        prevInteractiveForSample = interactive
        if (lastSamplePercent.isNaN()) {
            lastSamplePercent = computeCurrentBatteryPercent(level, currentCharge)
        }

        // Charging behavior: reset if preferred, otherwise pause window
        if (charging) {
            if (preferenceManager.isMonitorAutoResetOnCharging()) {
                screenOnAccumMs = 0L
                screenOnStartAtElapsed = null
                windowStartElapsed = -1L
                windowStartUptime = -1L
                consumedOnUah = 0L
                consumedOffUah = 0L
                onPercentDrop = 0.0
                offPercentDrop = 0.0
            } else {
                // Not resetting: just ensure we don't keep adding to the active window while charging
                if (windowStartElapsed >= 0L) {
                    val now = SystemClock.elapsedRealtime()
                    windowAccumMs += (now - windowStartElapsed)
                    windowStartElapsed = -1L
                }
                if (windowStartUptime >= 0L) windowStartUptime = -1L
                val start = screenOnStartAtElapsed
                if (start != null) {
                    screenOnAccumMs += (SystemClock.elapsedRealtime() - start)
                    screenOnStartAtElapsed = null
                }
            }
            lastSampleElapsedMs = nowElapsed
            lastSampleChargeUah = currentCharge
            prevInteractiveForSample = interactive
            lastSamplePercent = computeCurrentBatteryPercent(level, currentCharge)
        } else if (windowStartElapsed < 0L || windowStartUptime < 0L) {
            // start window on first non-charging tick
            windowStartElapsed = nowElapsed
            windowStartUptime = nowUptime
            // if screen currently on, start tracking from now
            screenOnStartAtElapsed = if (interactive) nowElapsed else null
            // also initialize sampling baselines
            lastSampleElapsedMs = nowElapsed
            lastSampleChargeUah = currentCharge
            prevInteractiveForSample = interactive
            lastSamplePercent = computeCurrentBatteryPercent(level, currentCharge)
        }

        val currentScreenOnMs = screenOnAccumMs + (screenOnStartAtElapsed?.let { nowElapsed - it } ?: 0L).coerceAtLeast(0L)
        val currentSessionWindowMs = if (windowStartElapsed >= 0L) (nowElapsed - windowStartElapsed).coerceAtLeast(0L) else 0L
        val windowMs = (windowAccumMs + currentSessionWindowMs).coerceAtLeast(0L)
        val screenOffMs = (windowMs - currentScreenOnMs).coerceAtLeast(0L)

        val currentSessionAwakeMs = if (windowStartUptime >= 0L) (nowUptime - windowStartUptime).coerceAtLeast(0L) else 0L
        val awakeMs = (awakeAccumMs + currentSessionAwakeMs).coerceAtLeast(0L)
        val deepSleepMs = (windowMs - awakeMs).coerceAtLeast(0L)
        val awakeOffMs = (awakeMs - currentScreenOnMs).coerceAtLeast(0L)
        // Deep Sleep % of screen-off time, gunakan ms agar akurat; hindari lonjakan jika total < 1s
        val offTotalMsForPct = deepSleepMs + awakeOffMs
        val hasOffWindow = offTotalMsForPct >= 1000L
        val deepSleepPct = if (hasOffWindow) (deepSleepMs * 100.0 / offTotalMsForPct) else 0.0
        val deepSleepPctStr = formatPercent(deepSleepPct, hasOffWindow)
        val deepSleepText = String.format(Locale.US, "%s (%s%%)", formatDurationAdaptive(deepSleepMs), deepSleepPctStr)
        val awakePct = if (hasOffWindow) (awakeOffMs * 100.0 / offTotalMsForPct) else 0.0
        val awakePctStr = formatPercent(awakePct, hasOffWindow)
        val awakeText = String.format(Locale.US, "%s (%s%%)", formatDurationAdaptive(awakeOffMs), awakePctStr)

        // Compute drain strings from cumulative consumption/time
        val onHours = currentScreenOnMs / 3_600_000.0
        val offHours = screenOffMs / 3_600_000.0
        val activeRate = if (onHours > 0) (onPercentDrop / onHours).coerceAtLeast(0.0) else 0.0
        val idleRate = if (offHours > 0) (offPercentDrop / offHours).coerceAtLeast(0.0) else 0.0
        
        // mAh-based drain calculation for better precision
        val activeMaDrain = if (onHours > 0) (consumedOnUah / 1000.0) / onHours else 0.0
        val idleMaDrain = if (offHours > 0) (consumedOffUah / 1000.0) / offHours else 0.0
        
        val activeDrainStr = when {
            currentScreenOnMs < MIN_DRAIN_WINDOW_MS -> "Calculating..."
            onPercentDrop <= 0.0 && activeMaDrain <= 0.0 -> "0.00% /hr"
            else -> String.format(Locale.US, "%.2f%% /hr (%.0fmA)", activeRate, activeMaDrain)
        }
        val idleDrainStr = when {
            screenOffMs < MIN_DRAIN_WINDOW_MS -> "Calculating..."
            offPercentDrop <= 0.0 && idleMaDrain <= 0.0 -> "0.00% /hr"
            else -> String.format(Locale.US, "%.2f%% /hr (%.0fmA)", idleRate, idleMaDrain)
        }

        // Save history every 1 minute
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastHistorySaveTime >= 60_000L) {
            lastHistorySaveTime = nowMs
            if (isUserUnlocked(this)) {
                scope.launch {
                    try {
                        batteryGraphRepository.insertEntry(
                            BatteryGraphEntry(
                                timestamp = nowMs,
                                batteryLevel = level,
                                currentMa = currentMa,
                                isCharging = charging,
                                isScreenOn = interactive,
                                activeDrainRate = activeRate.toFloat(),
                                idleDrainRate = idleRate.toFloat(),
                                temperature = temp,
                                uptime = SystemClock.uptimeMillis()
                            )
                        )

                        // Periodic cleanup (once every 24 hours)
                        if (nowMs - lastCleanupTime >= CLEANUP_INTERVAL_MS) {
                            val cutoff = nowMs - (HISTORY_RETENTION_DAYS * 24 * 60 * 60 * 1000L)
                            val deletedCount = batteryGraphRepository.deleteOldEntries(cutoff)
                            if (deletedCount > 0) {
                                Log.d("BatteryMonitorService", "Auto-cleanup: deleted $deletedCount entries older than $HISTORY_RETENTION_DAYS days")
                            }
                            lastCleanupTime = nowMs
                        }
                    } catch (_: Exception) { }
                }
            }
            persistState(sync = false)
        }

        return BatteryData(
            level = level,
            temperatureC = temp,
            statusText = getChargingStatus(status, plugged),
            currentMa = currentMa,
            powerWatt = powerWatt,
            isCharging = charging,
            activeDrain = activeDrainStr,
            idleDrain = idleDrainStr,
            screenOnTime = formatDurationAdaptive(currentScreenOnMs),
            elapsed = formatDurationAdaptive(screenOffMs),
            uptime = awakeText,
            deepSleep = deepSleepText
        )
    }

    private fun initScreenTracking() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isInteractive) {
            screenOnStartAtElapsed = SystemClock.elapsedRealtime()
            // Instant refresh when service starts and screen is already on
            try {
                scope.launch(Dispatchers.Default) {
                    val stats = collectSystemStats()
                    updateNotification(stats)
                }
                nextDelayOverrideMs = 5_000L
            } catch (_: Exception) { }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        if (screenOnStartAtElapsed == null) {
                            screenOnStartAtElapsed = SystemClock.elapsedRealtime()
                        }
                        // Instant refresh on screen ON
                        try {
                            scope.launch(Dispatchers.Default) {
                                val stats = collectSystemStats()
                                updateNotification(stats)
                            }
                            nextDelayOverrideMs = 5_000L
                        } catch (_: Exception) { }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        val start = screenOnStartAtElapsed
                        if (start != null) {
                            val now = SystemClock.elapsedRealtime()
                            screenOnAccumMs += (now - start)
                            screenOnStartAtElapsed = null
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        // User unlocked: ensure latest numbers shown immediately
                        try {
                            scope.launch(Dispatchers.Default) {
                                val stats = collectSystemStats()
                                updateNotification(stats)
                            }
                            nextDelayOverrideMs = 5_000L
                        } catch (_: Exception) { }
                    }
                }
            }
        }
        screenReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(receiver, filter)
    }

    private fun initPowerTracking() {
        // Set initial window depending on current charging state
        val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0
        if (isCharging) {
            onPowerConnected()
        } else {
            onPowerDisconnected()
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED -> onPowerConnected()
                    Intent.ACTION_POWER_DISCONNECTED -> onPowerDisconnected()
                }
            }
        }
        powerReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(receiver, filter)
    }

    private fun onPowerConnected() {
        Log.d("BatteryMonitorService", "onPowerConnected")
        // Legacy: check history auto-reset
        if (preferenceManager.isAutoResetOnCharging()) {
            fullReset()
        }
        
        // Monitor auto-reset on charging
        if (preferenceManager.isMonitorAutoResetOnCharging()) {
            manualReset()
        } else {
            // Even if we don't reset, we must "close" the current window
            val now = SystemClock.elapsedRealtime()
            val nowUp = SystemClock.uptimeMillis()
            if (windowStartElapsed >= 0) {
                windowAccumMs += (now - windowStartElapsed).coerceAtLeast(0L)
                windowStartElapsed = -1L
            }
            if (windowStartUptime >= 0) {
                awakeAccumMs += (nowUp - windowStartUptime).coerceAtLeast(0L)
                windowStartUptime = -1L
            }
            val start = screenOnStartAtElapsed
            if (start != null) {
                screenOnAccumMs += (now - start).coerceAtLeast(0L)
                screenOnStartAtElapsed = null
            }
            // And we stop tracking drain baselines for now
            lastSampleElapsedMs = 0L 
        }

        persistState(sync = true)
        // Tampilkan status charging seketika tanpa menunggu interval berikutnya
        try {
            lastCurrent = 0f
            scope.launch(Dispatchers.Default) {
                val stats = collectSystemStats()
                updateNotification(stats)
            }
            nextDelayOverrideMs = 5_000L
        } catch (_: Exception) { }
    }

    private fun onPowerDisconnected() {
        Log.d("BatteryMonitorService", "onPowerDisconnected")
        if (preferenceManager.isMonitorAutoResetOnCharging()) {
            windowAccumMs = 0L // Start fresh window
            awakeAccumMs = 0L
            screenOnAccumMs = 0L
            consumedOnUah = 0L
            consumedOffUah = 0L
            onPercentDrop = 0.0
            offPercentDrop = 0.0
        }

        val nowEl = SystemClock.elapsedRealtime()
        val nowUp = SystemClock.uptimeMillis()
        windowStartElapsed = nowEl
        windowStartUptime = nowUp
        
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        screenOnStartAtElapsed = if (pm.isInteractive) nowEl else null
        
        // Set sampling baseline
        val bm = batteryManager
        val charge = bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0L
        lastSampleElapsedMs = nowEl
        lastSampleChargeUah = charge
        prevInteractiveForSample = pm.isInteractive
        val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        lastSamplePercent = computeCurrentBatteryPercent(level, charge)
        persistState(sync = true)
        // Instant refresh to reflect discharging state without waiting for the next tick
        try {
            scope.launch(Dispatchers.Default) {
                val stats = collectSystemStats()
                updateNotification(stats)
            }
            nextDelayOverrideMs = 5_000L
        } catch (_: Exception) { }
    }

    private fun manualReset() {
        Log.d("BatteryMonitorService", "Manual reset triggered")
        val nowEl = SystemClock.elapsedRealtime()
        val nowUp = SystemClock.uptimeMillis()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        windowStartElapsed = nowEl
        windowStartUptime = nowUp
        windowAccumMs = 0L
        awakeAccumMs = 0L
        screenOnAccumMs = 0L
        screenOnStartAtElapsed = if (pm.isInteractive) nowEl else null
        consumedOnUah = 0L
        consumedOffUah = 0L
        onPercentDrop = 0.0
        offPercentDrop = 0.0
        val charge = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0L
        lastSampleElapsedMs = nowEl
        lastSampleChargeUah = charge
        prevInteractiveForSample = pm.isInteractive
        val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        lastSamplePercent = computeCurrentBatteryPercent(level, charge)
        persistState(sync = true)
    }

    private fun fullReset() {
        if (isUserUnlocked(this)) {
            scope.launch(Dispatchers.IO) {
                try {
                    batteryGraphRepository.deleteAllEntries()
                } catch (_: Exception) { }
            }
        }
        manualReset()
        try {
            scope.launch(Dispatchers.Default) {
                val stats = collectSystemStats()
                updateNotification(stats)
            }
        } catch (_: Exception) {}
    }

    private fun isUserUnlocked(context: Context): Boolean {
        val um = context.getSystemService(USER_SERVICE) as android.os.UserManager
        return um.isUserUnlocked
    }

    private val CHARGE_LIMIT_NOTIF_ID = 1005
    private var isChargeLimitNotified = false

    private fun checkChargingControl(level: Int, plugged: Boolean) {
        if (!preferenceManager.isChargingControlEnabled()) {
            lastKnownBypassState = null // Reset cache if disabled
            isChargeLimitNotified = false
            return
        }
        
        // Only run check if plugged in
        if (plugged) {
            val stopLevel = preferenceManager.getChargingControlStopLevel()
            val resumeLevel = preferenceManager.getChargingControlResumeLevel()
            
            var shouldEnableBypass: Boolean? = null

            // Determine desired state based on levels
            if (level >= stopLevel) {
                shouldEnableBypass = true
            } else if (level <= resumeLevel) {
                shouldEnableBypass = false
                isChargeLimitNotified = false // Reset notification flag when charging resumes
            }

            // Apply only if state needs to change or if we don't know the state yet
            if (shouldEnableBypass != null) {
                scope.launch {
                    // If cache is missing, read the actual state once to sync
                    if (lastKnownBypassState == null) {
                        lastKnownBypassState = systemRepository.getBypassCharging()
                    }

                    // Only perform I/O if the desired state differs from the current/cached state
                    if (lastKnownBypassState != shouldEnableBypass) {
                        val success = systemRepository.setBypassCharging(shouldEnableBypass)
                        if (success) {
                            lastKnownBypassState = shouldEnableBypass
                            
                            // Show/Hide charge limit notification
                            if (shouldEnableBypass) {
                                showChargeLimitNotification(level)
                            } else {
                                removeChargeLimitNotification()
                            }

                            // Force update notification to reflect status change immediately
                            try {
                               updateNotification(collectSystemStats())
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        } else {
            // Reset cache and notifications when unplugged
            lastKnownBypassState = null
            isChargeLimitNotified = false
            removeChargeLimitNotification()
        }
    }

    private fun showChargeLimitNotification(level: Int) {
        if (isChargeLimitNotified) return
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.status_limit_reached))
            .setContentText(getString(R.string.charging_control_notif_desc, level))
            .setSmallIcon(R.drawable.ic_battery_saver)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(CHARGE_LIMIT_NOTIF_ID, notification)
        isChargeLimitNotified = true
    }

    private fun removeChargeLimitNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CHARGE_LIMIT_NOTIF_ID)
    }

    private fun checkAutoResetAtLevel(level: Int, isCharging: Boolean) {
        if (isCharging) {
             // History auto-reset
             if (preferenceManager.isAutoResetAtLevel()) {
                 val target = preferenceManager.getAutoResetTargetLevel()
                 if (level >= target && !hasResetForThisChargeCycle) {
                     Log.d("BatteryMonitorService", "Auto-reset history at level $level (target $target)")
                     if (isUserUnlocked(this)) {
                         scope.launch { try { batteryGraphRepository.deleteAllEntries() } catch (_: Exception) { } }
                     }
                 }
             }
             
             // Monitor auto-reset
             if (preferenceManager.isMonitorAutoResetAtLevel()) {
                 val target = preferenceManager.getMonitorAutoResetTargetLevel()
                 if (level >= target && !hasResetForThisChargeCycle) {
                     Log.d("BatteryMonitorService", "Auto-reset monitor at level $level (target $target)")
                     manualReset()
                     hasResetForThisChargeCycle = true
                 }
             } else {
                 if (preferenceManager.isAutoResetAtLevel()) {
                     val target = preferenceManager.getAutoResetTargetLevel()
                     if (level >= target) {
                         hasResetForThisChargeCycle = true
                     }
                 }
             }
        } else {
             // Only reset the flag if the level has dropped below target - 2%
             // to prevent flip-flopping due to status noise on aggressive ROMs/buggy ports
             val target = if (preferenceManager.isMonitorAutoResetAtLevel()) 
                 preferenceManager.getMonitorAutoResetTargetLevel() 
             else preferenceManager.getAutoResetTargetLevel()
             
             if (level < target - 2) {
                 if (hasResetForThisChargeCycle) {
                    Log.d("BatteryMonitorService", "Clearing hasResetForThisChargeCycle flag (level=$level, target=$target)")
                 }
                 hasResetForThisChargeCycle = false
             }
        }
    }

    private fun formatDrainFrom(
        startTimeMs: Long,
        startChargeUah: Long,
        startLevel: Int,
        nowElapsed: Long,
        currentChargeUah: Long,
        currentLevel: Int,
    ): String {
        val elapsedMs = nowElapsed - startTimeMs
        if (elapsedMs <= 0L) return "0.0%/h"
        val hours = elapsedMs / 3_600_000.0
        if (hours <= 0.0) return "0.0%/h"

        val percentDrop = when {
            startLevel >= 0 && currentLevel >= 0 -> (startLevel - currentLevel).toDouble()
            else -> ((startChargeUah - currentChargeUah).toDouble() * 100.0) / designCapacityUah.toDouble()
        }
        if (percentDrop <= 0.0) return "0.0%/h"

        val percentPerHour = percentDrop / hours
        return String.format(Locale.US, "%.2f%%/h", percentPerHour)
    }

    private fun updateRealtimeUsed(currentMa: Float) {
        val now = System.currentTimeMillis()
        val diffSec = (now - lastUpdate) / 1000.0
        lastUpdate = now

        if (currentMa > 0f) {
            val add = (diffSec / 3600.0) * currentMa
            realtimeUsedMah += add
        }
    }

    private fun getDesignCapacityUah(): Long {
        // Try multiple sysfs paths; normalize unit to µAh
        val candidates = listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/battery/charge_full",
            "/sys/class/power_supply/max170xx_battery/charge_full_design",
            "/sys/class/power_supply/max170xx_battery/charge_full"
        )
        for (p in candidates) {
            try {
                val raw = File(p).readText().trim().toLong()
                if (raw > 0) return normalizeToUah(raw)
            } catch (_: Exception) { }
        }
        // Fallback to derive from charge counter and level (no hardcoded value)
        val bm = batteryManager ?: return 0L
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charge = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        return if (level > 0 && charge > 0) (charge * 100) / level else 0L
    }

    private fun normalizeToUah(raw: Long): Long {
        // Heuristic: some kernels expose in mAh or nAh; normalize to µAh
        return when {
            raw in 1..100_000L -> raw * 1_000L // likely mAh
            raw >= 100_000_000L -> raw / 1_000L // likely nAh
            else -> raw // assume µAh
        }
    }

    // legacy stub removed; baseline is handled via sampling attribution

    private fun getChargingStatus(status: Int, plugged: Int): String {
        // Fallback check if intent status is unknown
        val finalStatus = if (status == -1 || status == BatteryManager.BATTERY_STATUS_UNKNOWN) {
             batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: status
        } else status

        // Sanity check: if plugged is 0, it's likely not charging regardless of status (buggy kernels/ports)
        if (plugged == 0 && finalStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
            return "Discharging"
        }
        
        return when (finalStatus) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            else -> if (plugged > 0) "Plugged" else "Idle"
        }
    }

    private fun createBatteryIcon(level: Int, isCharging: Boolean): IconCompat {
        val size = 24 * 3 // 72px for xhdpi approx
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = size * 0.6f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val yPos = (canvas.height / 2) - ((paint.descent() + paint.ascent()) / 2)
        val text = if (isCharging && preferenceManager.isBatteryChargingIconEnabled()) "⚡" 
                   else if (level >= 0) "$level" 
                   else "?"
        canvas.drawText(text, canvas.width / 2f, yPos, paint)
        return IconCompat.createWithBitmap(bitmap)
    }

    // === NOTIFICATION ===
    private fun createNotification(title: String, bigText: String, batteryLevel: Int = -1, isCharging: Boolean = false): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val iconStyle = preferenceManager.getNotificationIconStyle()
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(null)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(pendingIntent)
            .setShowWhen(true)
            .setWhen(if (serviceStartedAtMs != 0L) serviceStartedAtMs else System.currentTimeMillis())
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        when (iconStyle) {
            PreferenceManager.ICON_STYLE_BATTERY_PERCENT -> {
                builder.setSmallIcon(createBatteryIcon(batteryLevel, isCharging))
            }
            PreferenceManager.ICON_STYLE_TRANSPARENT -> {
                builder.setSmallIcon(R.drawable.transparent)
            }
            else -> {
                builder.setSmallIcon(R.drawable.nkm_png)
            }
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        // Cleanup old channel
        try {
            notificationManager.deleteNotificationChannel("battery_monitor_channel")
        } catch (_: Exception) {}

        val channel = NotificationChannel(
            channelId,
            "Battery Monitor",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Menampilkan status baterai realtime"
        channel.setSound(null, null)
        channel.enableVibration(false)
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification(stats: BatteryData) {
        val isChargingLike = stats.statusText == "Charging" || stats.statusText == "Plugged" || stats.statusText == "Full"
        val isFull = stats.statusText == "Full"
        val currentAbsMa = kotlin.math.abs(stats.currentMa)
        val currentDisplayMa = kotlin.math.abs(stats.currentMa.roundToInt())
        val statusLabel = if (isChargingLike && !isFull) {
            when {
                currentAbsMa > 2000f -> "Charging rapidly"
                currentAbsMa in 0.1f..999.9f -> "Charging slowly"
                else -> "Charging"
            }
        } else stats.statusText

        val title = if (isFull) {
            "${stats.level}% · ${stats.temperatureC.roundToInt()}°C · Fully Charged"
        } else {
            val baseTitle = "${stats.level}% · ${stats.temperatureC.roundToInt()}°C · ${statusLabel} · ${currentDisplayMa}mA"
            if (isChargingLike) {
                val wattStr = String.format(Locale.US, "%.1fW", kotlin.math.abs(stats.powerWatt))
                "$baseTitle · $wattStr"
            } else baseTitle
        }
        val onUsedPctStr = "${kotlin.math.max(0, onPercentDrop.roundToInt())}%"
        val offUsedPctStr = "${kotlin.math.max(0, offPercentDrop.roundToInt())}%"
        val bigText = """
            Active drain : ${stats.activeDrain} · idle drain   : ${stats.idleDrain}
            Screen on    : ${stats.screenOnTime} (${onUsedPctStr})
            Screen off   : ${stats.elapsed} (${offUsedPctStr})
            Deep sleep   : ${stats.deepSleep}
            Awake        : ${stats.uptime}
        """.trimIndent()
        
        val notification = createNotification(title, bigText, stats.level, stats.isCharging)
        
        // Only notify — service is already foregrounded in onCreate.
        // startForeground() every tick causes periodic main-thread wakeups
        // via system_server IPC, producing frame jitter (0.9ms → 0.17ms oscillation).
        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: Exception) { }
    }

    private fun formatDurationAdaptive(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val h = (totalSec / 3600).toInt()
        val m = ((totalSec % 3600) / 60).toInt()
        val s = (totalSec % 60).toInt()
        return when {
            totalSec < 60 -> String.format(Locale.US, "%ds", s)
            totalSec < 3600 -> String.format(Locale.US, "%dm %ds", m, s)
            else -> String.format(Locale.US, "%dh %dm %ds", h, m, s)
        }
    }

    private fun persistState(sync: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        val nowUp = SystemClock.uptimeMillis()
        val updatedAccum = screenOnAccumMs + (screenOnStartAtElapsed?.let { now - it } ?: 0L)
        val editor = prefs.edit()
            .putLong(keyScreenAccum, updatedAccum)
            .putLong(keyLastElapsed, now)
            .putLong(keyLastUptime, nowUp)
            .putLong("window_start_elapsed", windowStartElapsed)
            .putLong("window_start_uptime", windowStartUptime)
            .putLong("window_accum_ms", windowAccumMs)
            .putLong("awake_accum_ms", awakeAccumMs)
            .putLong("consumed_on_uah", consumedOnUah)
            .putLong("consumed_off_uah", consumedOffUah)
            .putLong("on_percent_drop_bits", java.lang.Double.doubleToRawLongBits(onPercentDrop))
            .putLong("off_percent_drop_bits", java.lang.Double.doubleToRawLongBits(offPercentDrop))
            .putLong("last_sample_elapsed", lastSampleElapsedMs)
            .putLong("last_sample_charge", lastSampleChargeUah)
            .putLong("last_sample_percent_bits", java.lang.Double.doubleToRawLongBits(lastSamplePercent))
            .putBoolean("prev_interactive", prevInteractiveForSample ?: false)
            .putLong("last_cleanup_time", lastCleanupTime)
        
        if (sync) {
            editor.commit()
        } else {
            editor.apply()
        }
    }


    private fun restoreStateIfAny() {
        val lastElapsed = prefs.getLong(keyLastElapsed, 0L)
        val lastUptime = prefs.getLong(keyLastUptime, 0L)
        val savedAccum = prefs.getLong(keyScreenAccum, 0L)
        val savedWindowStart = prefs.getLong("window_start_elapsed", -1L)
        val savedWindowStartUptime = prefs.getLong("window_start_uptime", -1L)
        val savedWindowAccum = prefs.getLong("window_accum_ms", 0L)
        val savedAwakeAccum = prefs.getLong("awake_accum_ms", 0L)
        
        // Migration heuristic: If awake history is missing (from old version bug) but we have screen on history,
        // assume Awake >= Screen On to prevent "Deep Sleep > Screen Off" paradox.
        val effectiveSavedAwake = if (savedAwakeAccum == 0L && savedAccum > 0L) savedAccum else savedAwakeAccum
        
        // Restore drain stats
        consumedOnUah = prefs.getLong("consumed_on_uah", 0L)
        consumedOffUah = prefs.getLong("consumed_off_uah", 0L)
        onPercentDrop = java.lang.Double.longBitsToDouble(prefs.getLong("on_percent_drop_bits", 0L))
        offPercentDrop = java.lang.Double.longBitsToDouble(prefs.getLong("off_percent_drop_bits", 0L))
        
        // Restore sampling stats
        lastSampleElapsedMs = prefs.getLong("last_sample_elapsed", 0L)
        lastSampleChargeUah = prefs.getLong("last_sample_charge", 0L)
        lastSamplePercent = java.lang.Double.longBitsToDouble(prefs.getLong("last_sample_percent_bits", java.lang.Double.doubleToRawLongBits(Double.NaN)))
        if (prefs.contains("prev_interactive")) {
            prevInteractiveForSample = prefs.getBoolean("prev_interactive", false)
        }

        val now = SystemClock.elapsedRealtime()
        val isReboot = (lastElapsed == 0L || now < lastElapsed)

        if (isReboot) {
            Log.i("BatteryMonitorService", "Detected reboot, resetting transient stats")
            if (preferenceManager.isMonitorAutoResetOnReboot()) {
                // Reset everything if user requested auto-reset on reboot
                screenOnAccumMs = 0L
                windowAccumMs = 0L
                awakeAccumMs = 0L
                windowStartElapsed = -1L
                windowStartUptime = -1L
                consumedOnUah = 0L
                consumedOffUah = 0L
                onPercentDrop = 0.0
                offPercentDrop = 0.0
                lastSampleElapsedMs = 0L
                lastSamplePercent = Double.NaN
            } else {
                // Persist: Carry over stats
                screenOnAccumMs = savedAccum
                
                // For window duration: add the duration from the previous session
                val prevSessionDuration = if (savedWindowStart >= 0) (lastElapsed - savedWindowStart).coerceAtLeast(0L) else 0L
                windowAccumMs = savedWindowAccum + prevSessionDuration

                // For awake duration: add the uptime duration from the previous session
                val prevSessionAwake = if (savedWindowStartUptime >= 0 && lastUptime >= savedWindowStartUptime) 
                    (lastUptime - savedWindowStartUptime) else 0L
                awakeAccumMs = effectiveSavedAwake + prevSessionAwake
                
                // Reset start time to now for the new session
                windowStartElapsed = now
                windowStartUptime = SystemClock.uptimeMillis()
                
                // Sampling stats carry over but we reset baseline to avoid massive delta
                lastSampleElapsedMs = 0L
                lastSamplePercent = Double.NaN
            }
        } else {
            // Normal restore (service restart, no reboot)
            screenOnAccumMs = savedAccum
            windowAccumMs = savedWindowAccum
            awakeAccumMs = effectiveSavedAwake
            windowStartElapsed = savedWindowStart
            windowStartUptime = savedWindowStartUptime
        }
        lastCleanupTime = prefs.getLong("last_cleanup_time", 0L)
    }

    companion object {
        private const val ACTION_RESET = "id.nkz.nokontzzzmanager.action.RESET_BATTERY_MONITOR"
        private const val ACTION_BOOT_START = "id.nkz.nokontzzzmanager.action.BOOT_START_BATTERY_MONITOR"
        private const val ACTION_UPDATE_ICON = "id.nkz.nokontzzzmanager.action.UPDATE_ICON"

        fun start(context: Context, isBoot: Boolean = false) {
            val intent = Intent(context, BatteryMonitorService::class.java)
            if (isBoot) intent.action = ACTION_BOOT_START
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryMonitorService::class.java))
        }

        fun reset(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java).apply { action = ACTION_RESET }
            context.startForegroundService(intent)
        }

        fun updateIcon(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java).apply { action = ACTION_UPDATE_ICON }
            context.startForegroundService(intent)
        }
    }

    data class BatteryData(
        val level: Int = -1,
        val temperatureC: Float = 0f,
        val statusText: String = "Unknown",
        val currentMa: Float = 0f,
        val powerWatt: Float = 0f,
        val isCharging: Boolean = false,
        val activeDrain: String = "-",
        val idleDrain: String = "-",
        val screenOnTime: String = "-",
        val elapsed: String = "-",
        val uptime: String = "-",
        val deepSleep: String = "-",
    )
}