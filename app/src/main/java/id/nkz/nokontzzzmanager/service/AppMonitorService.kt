package id.nkz.nokontzzzmanager.service

import android.app.Service
import android.content.Context
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import id.nkz.nokontzzzmanager.data.database.AppProfileEntity
import id.nkz.nokontzzzmanager.data.repository.AppProfileRepository
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

import id.nkz.nokontzzzmanager.data.repository.TuningRepository

import id.nkz.nokontzzzmanager.data.repository.ThermalRepository

@AndroidEntryPoint
class AppMonitorService : Service() {

    @Inject
    lateinit var appProfileRepository: AppProfileRepository

    @Inject
    lateinit var systemRepository: SystemRepository

    @Inject
    lateinit var tuningRepository: TuningRepository

    @Inject
    lateinit var thermalRepository: ThermalRepository

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var gameRepository: id.nkz.nokontzzzmanager.data.repository.GameRepository

    @Inject
    lateinit var fpsMonitorManager: id.nkz.nokontzzzmanager.manager.FpsMonitorManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null
    private var lastPackageName: String? = null
    private var isProfileApplied = false

    private val usageStatsManager by lazy {
        getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val powerManager by lazy {
        getSystemService(POWER_SERVICE) as android.os.PowerManager
    }

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
    }

    private val CHANNEL_ID = "app_monitor_service_channel"
    private val NOTIFICATION_ID = 1002

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AppMonitorService", "Service onCreate")
        createNotificationChannel()
        startForegroundService()
        startMonitoring()
    }

    private fun createNotificationChannel() {
        Log.d("AppMonitorService", "Creating notification channel")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "App Monitor Service"
            val descriptionText = "Monitors foreground apps to apply performance profiles and FPS overlay"
            val importance = android.app.NotificationManager.IMPORTANCE_LOW
            val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        Log.d("AppMonitorService", "Starting foreground service")
        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Monitor Active")
            .setContentText("Monitoring for game profiles")
            .setSmallIcon(id.nkz.nokontzzzmanager.R.drawable.ic_notification)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d("AppMonitorService", "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e("AppMonitorService", "Failed to start foreground service", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AppMonitorService", "onStartCommand received")
        if (monitorJob?.isActive != true) {
            startMonitoring()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("AppMonitorService", "Service onDestroy")
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }

    private fun startMonitoring() {
        Log.d("AppMonitorService", "Starting monitoring job")
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            Log.d("AppMonitorService", "Monitoring job loop started")
            while (isActive) {
                if (powerManager.isInteractive) {
                    checkForegroundApp()
                    delay(1000) // Check every 1 second
                } else {
                    // Screen is off, pause monitoring
                    delay(5000)
                }
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
    }

    private suspend fun checkForegroundApp() {
        val currentApp = withContext(Dispatchers.IO) { getForegroundPackageName() }
        
        if (currentApp != null && currentApp != lastPackageName) {
            Log.d("AppMonitorService", "App changed: $lastPackageName -> $currentApp")
            lastPackageName = currentApp
            handleAppChange(currentApp)
        } else if (currentApp == null) {
            // Log once in a while or only if it was not null before to avoid spam
            // But for debugging, let's log if it's null
            Log.w("AppMonitorService", "getForegroundPackageName returned null. Check Usage Access permission.")
        }
    }

    private fun getForegroundPackageName(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000 // Look back 10 seconds
        
        // 1. Try UsageEvents (most accurate for transitions)
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastResumedApp: String? = null
        var eventCount = 0

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            eventCount++
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastResumedApp = event.packageName
            }
        }
        
        if (lastResumedApp != null) {
            Log.d("AppMonitorService", "Found foreground app via UsageEvents: $lastResumedApp (from $eventCount events)")
            return lastResumedApp
        }

        // 2. Fallback to queryUsageStats (good for initial state)
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        if (!stats.isNullOrEmpty()) {
            val topApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName
            Log.d("AppMonitorService", "Found foreground app via queryUsageStats: $topApp (from ${stats.size} stats)")
            return topApp
        }

        Log.w("AppMonitorService", "No usage events or stats found. Usage Access might be missing.")
        return null
    }

    private suspend fun handleAppChange(packageName: String) {
        Log.d("AppMonitorService", "Handling app change: $packageName")
        val profile = appProfileRepository.getProfile(packageName)

        if (profile != null && profile.isEnabled) {
            Log.d("AppMonitorService", "Found profile for $packageName, applying...")
            applyProfile(profile)
            isProfileApplied = true
        } else {
            // Only revert if we previously applied a profile
            if (isProfileApplied) {
                Log.d("AppMonitorService", "No profile for $packageName, reverting to global settings")
                applyGlobalSettings()
                isProfileApplied = false
            }
        }

        // Handle FPS Overlay for games - check game repository independently
        try {
            val game = gameRepository.getGameByPackageName(packageName).first()
            if (game != null && game.isBenchmarkEnabled) {
                Log.d("AppMonitorService", "App $packageName is a registered game with overlay enabled. Starting overlay.")
                startFpsOverlay(game)
            } else {
                Log.d("AppMonitorService", "App $packageName is not a registered game or overlay is disabled.")
                stopFpsOverlay()
            }
        } catch (e: Exception) {
            Log.e("AppMonitorService", "Error checking game repository for $packageName", e)
            stopFpsOverlay()
        }
    }

    private fun startFpsOverlay(game: id.nkz.nokontzzzmanager.data.database.GameEntity) {
        fpsMonitorManager.startMonitoring(
            packageName = game.packageName,
            preferredLayerPattern = game.preferredLayerPattern,
            onLayerFound = { pattern ->
                // Update the game entity with the newly found pattern
                serviceScope.launch {
                    gameRepository.insertGame(game.copy(preferredLayerPattern = pattern))
                }
            }
        )
        val intent = Intent(this, FpsOverlayService::class.java)
        startService(intent)
    }

    private fun stopFpsOverlay() {
        fpsMonitorManager.stopMonitoring()
        val intent = Intent(this, FpsOverlayService::class.java)
        stopService(intent)
    }

    private suspend fun applyProfile(profile: AppProfileEntity) {
        Log.d("AppMonitorService", "Applying profile for ${profile.appName}: ${profile.performanceMode}")
        
        // 1. Performance Mode
        applyPerformanceMode(profile.performanceMode)

        // 2. Misc Settings (KGSL, Bypass, Dirty PTE)
        systemRepository.setKgslSkipZeroing(profile.kgslSkipZeroing)
        systemRepository.setBypassCharging(profile.bypassCharging)
        systemRepository.setAvoidDirtyPte(profile.allowDirtyPte)

        // 3. CPU Tuning
        applyCpuConfig(profile.getCpuConfig())

        // 4. GPU Tuning
        applyGpuConfig(profile.getGpuConfig())

        // 5. Thermal Profile
        if (profile.thermalProfile != null) {
            applyThermalProfile(profile.thermalProfile)
        }
    }

    private suspend fun applyGlobalSettings() {
        Log.d("AppMonitorService", "Reverting to global settings")

        // 1. Performance Mode
        val globalMode = preferenceManager.getPerformanceMode()
        applyPerformanceMode(globalMode)

        // 2. Misc Settings (KGSL, Bypass, Dirty PTE)
        systemRepository.setKgslSkipZeroing(preferenceManager.getKgslSkipZeroing())
        systemRepository.setBypassCharging(preferenceManager.getBypassCharging())
        systemRepository.setAvoidDirtyPte(preferenceManager.getAvoidDirtyPte())

        // 3. Revert CPU Tuning to Global Prefs
        revertCpuConfig()

        // 4. Revert GPU Tuning to Global Prefs
        revertGpuConfig()

        // 5. Revert Thermal Profile
        revertThermalProfile()
    }

    private fun applyThermalProfile(profileIndex: Int) {
        serviceScope.launch {
            thermalRepository.setThermalModeIndex(profileIndex).collect {}
        }
    }

    private fun revertThermalProfile() {
        serviceScope.launch {
            val savedMode = thermalRepository.getSavedThermalMode()
            thermalRepository.setThermalModeIndex(savedMode).collect {}
        }
    }

    private suspend fun applyGpuConfig(config: id.nkz.nokontzzzmanager.data.model.GpuProfileConfig) {
        if (!config.governor.isNullOrBlank()) {
            tuningRepository.setGpuGov(config.governor)
        }
        if (config.minFreq != null) {
            tuningRepository.setGpuMinFreq(config.minFreq)
        }
        if (config.maxFreq != null) {
            tuningRepository.setGpuMaxFreq(config.maxFreq)
        }
        if (config.powerLevel != null) {
            tuningRepository.setGpuPowerLevel(config.powerLevel.toFloat())
        }
        if (config.throttlingEnabled != null) {
            systemRepository.setGpuThrottling(config.throttlingEnabled)
        }
    }

    private suspend fun revertGpuConfig() {
        val globalGov = preferenceManager.getGpuGovernor()
        if (globalGov != null) tuningRepository.setGpuGov(globalGov)

        val globalMin = preferenceManager.getGpuMinFreq()
        val globalMax = preferenceManager.getGpuMaxFreq()

        if (globalMin != -1 || globalMax != -1) {
            if (globalMin != -1) tuningRepository.setGpuMinFreq(globalMin)
            if (globalMax != -1) tuningRepository.setGpuMaxFreq(globalMax)
        } else {
            tuningRepository.resetGpuFreq()
        }

        val globalPwr = preferenceManager.getGpuPowerLevel()
        if (globalPwr != -1) tuningRepository.setGpuPowerLevel(globalPwr.toFloat())

        val globalThrottling = preferenceManager.getGpuThrottling()
        // If null (not set), we might assume default is enabled (true), or just leave it. 
        // Safer to revert to preference if set.
        if (globalThrottling != null) {
            systemRepository.setGpuThrottling(globalThrottling)
        }
    }

    private fun applyCpuConfig(config: id.nkz.nokontzzzmanager.data.model.CpuProfileConfig) {
        serviceScope.launch {
            // Apply Cluster Configs
            config.clusterConfigs.forEach { (cluster, clusterConfig) ->
                if (!clusterConfig.governor.isNullOrBlank()) {
                    tuningRepository.setCpuGov(cluster, clusterConfig.governor)
                }
                
                if (clusterConfig.minFreq != null || clusterConfig.maxFreq != null) {
                    val currentFreqs = tuningRepository.getCpuFreq(cluster).first()
                    val targetMin = clusterConfig.minFreq ?: currentFreqs.first
                    val targetMax = clusterConfig.maxFreq ?: currentFreqs.second
                    tuningRepository.setCpuFreq(cluster, targetMin, targetMax)
                }
            }

            // Apply Core Online Status
            config.coreOnlineStatus.forEach { (coreId, online) ->
                tuningRepository.setCoreOnline(coreId, online)
            }
        }
    }

    private fun revertCpuConfig() {
        serviceScope.launch {
            // Revert clusters to global preferences
            val clusterNodes = tuningRepository.getClusterLeaders()
            clusterNodes.forEach { cluster ->
                val globalGov = preferenceManager.getCpuGov(cluster)
                if (globalGov != null) tuningRepository.setCpuGov(cluster, globalGov)

                val globalMin = preferenceManager.getCpuMinFreq(cluster)
                val globalMax = preferenceManager.getCpuMaxFreq(cluster)
                
                if (globalMin != -1 || globalMax != -1) {
                    val currentFreqs = tuningRepository.getCpuFreq(cluster).first()
                    val targetMin = if (globalMin != -1) globalMin else currentFreqs.first
                    val targetMax = if (globalMax != -1) globalMax else currentFreqs.second
                    tuningRepository.setCpuFreq(cluster, targetMin, targetMax)
                } else {
                    tuningRepository.resetCpuFreq(cluster)
                }
            }

            // Revert cores
            // IMPORTANT: Use getNumberOfCores() to detect all physical cores, 
            // even if they were disabled by a profile.
            val cores = tuningRepository.getNumberOfCores()
            for (i in 0 until cores) {
                val globalOnline = preferenceManager.getCpuCoreOnline(i)
                // If explicit pref exists, use it. Otherwise assume online (true) or just leave it?
                // Safer to default to true if we don't know, to avoid stuck offline cores.
                if (globalOnline != null) {
                    tuningRepository.setCoreOnline(i, globalOnline)
                } else {
                    // If no global pref, ensure it's online to be safe
                    tuningRepository.setCoreOnline(i, true)
                }
            }
        }
    }

    private suspend fun applyPerformanceMode(mode: String) {
        val targetGovernor = when (mode) {
            "Performance" -> "performance"
            "Powersave" -> "powersave"
            else -> "schedutil"
        }
        
        // Dynamically get cluster leaders instead of hardcoding
        val clusterNodes = tuningRepository.getClusterLeaders()
        clusterNodes.forEach { cluster ->
             val available = tuningRepository.getAvailableCpuGovernors(cluster).first()
             if (available.contains(targetGovernor)) {
                 tuningRepository.setCpuGov(cluster, targetGovernor)
             } else if (mode == "Balanced") {
                 // Fallback for Balanced if schedutil is missing
                 when {
                     available.contains("walt") -> tuningRepository.setCpuGov(cluster, "walt")
                     available.contains("interactive") -> tuningRepository.setCpuGov(cluster, "interactive")
                     available.contains("pixutil") -> tuningRepository.setCpuGov(cluster, "pixutil")
                     // If none match, we simply don't touch it, or we could leave it as is.
                     // But we should try to move away from performance/powersave if that was the previous state.
                 }
             }
        }
    }
}
