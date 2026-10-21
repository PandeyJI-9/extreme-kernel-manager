package id.nkz.nokontzzzmanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.repository.CustomTunableRepository
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.data.repository.ThermalRepository
import id.nkz.nokontzzzmanager.data.repository.TuningRepository
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootRestoreService : Service() {

    @Inject
    lateinit var customTunableRepository: CustomTunableRepository
    @Inject
    lateinit var systemRepository: SystemRepository
    @Inject
    lateinit var tuningRepository: TuningRepository
    @Inject
    lateinit var thermalRepository: ThermalRepository
    @Inject
    lateinit var preferenceManager: PreferenceManager
    @Inject
    lateinit var profileManager: id.nkz.nokontzzzmanager.extreme.ProfileManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "BootRestoreService"
    private val CHANNEL_ID = "boot_restore_channel"
    private val NOTIFICATION_ID = 1002

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Boot Restore Service started")
        
        // Start as Foreground to prevent system kill during boot
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        serviceScope.launch {
            // Restore standard settings first (once)
            restoreSystemSettings()
            
            // Then enter aggressive monitoring loop for custom tunables
            // We can also include critical system settings in the loop if needed, 
            // but usually custom tunables are the most fragile.
            monitorAndEnforceTunables()
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private suspend fun restoreSystemSettings() {
        Log.d(TAG, "Restoring standard system settings...")

        // Restore EXTREME Tri-Profile Engine
        try {
            val active = profileManager.activeProfileFlow.first()
            Log.i(TAG, "Restoring active EXTREME profile: ${active.title}")
            profileManager.applyProfile(active)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore extreme profile on boot: ${e.message}")
        }
        
        val batchCommands = mutableListOf<String>()

        // 1. Network & IO
        if (preferenceManager.isApplyNetworkStorageOnBoot()) {
            preferenceManager.getTcpCongestionAlgorithm()?.let { algo ->
                batchCommands.add("echo -n $algo > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null || true")
            }

            val ioPaths = listOf("/sys/block/sda/queue/scheduler", "/sys/block/mmcblk0/queue/scheduler", "/sys/block/nvme0n1/queue/scheduler")
            preferenceManager.getIoScheduler()?.let { sched ->
                ioPaths.forEach { path ->
                    batchCommands.add("echo -n $sched > $path 2>/dev/null || true")
                }
            }
        }

        // 2. Misc Settings
        systemRepository.setAvoidDirtyPte(preferenceManager.getAvoidDirtyPte())
        systemRepository.setKgslSkipZeroing(preferenceManager.getKgslSkipZeroing())
        
        // Only apply bypass charging and fast charge if the feature is actually supported/toggled
        // to avoid unnecessary root calls for basic users, but for e404/n0kz we enforce the preference.
        if (preferenceManager.getBypassCharging()) {
            systemRepository.setBypassCharging(true)
        } else {
            // If it's OFF in pref, we ensure it's OFF in kernel (important if kernel defaults to ON)
            systemRepository.setBypassCharging(false)
        }

        if (preferenceManager.getForceFastCharge()) {
            systemRepository.setForceFastCharge(true)
        } else {
            systemRepository.setForceFastCharge(false)
        }

        // 3. Background Blocker
        if (preferenceManager.isApplyBgBlockerOnBoot()) {
            val prefBlocklist = preferenceManager.getBgBlocklist()
            val blocklist = prefBlocklist ?: "com.shopee.id,com.lazada.android,com.tokopedia.tkpd"
            // Use systemRepository directly to find correct path and write with root
            systemRepository.setBgBlocklist(blocklist)
        }

        // Apply all at once
        if (batchCommands.isNotEmpty()) {
            tuningRepository.runBatchTuning(batchCommands)
        }

        // 3. Other specific modules
        restorePerformanceMode()
        restoreCpuSettings()
        restoreGpuSettings()
        restoreRamSettings()
        restoreThermalSettings()
    }

    private suspend fun monitorAndEnforceTunables() {
        val tunables = customTunableRepository.getBootTunables()
        if (tunables.isEmpty()) {
            Log.d(TAG, "No custom tunables to restore. Exiting loop.")
            return
        }

        Log.d(TAG, "Found ${tunables.size} tunables. Starting enforcement loop (60s).")

        // Duration to monitor: 60 seconds
        // Interval: 3 seconds
        val maxRetries = 20 
        
        for (i in 0 until maxRetries) {
            tunables.forEach { tunable ->
                try {
                    // Check current value
                    val current = customTunableRepository.readTunable(tunable.path)
                    
                    if (current != tunable.value) {
                        Log.w(TAG, "Mismatch detected on loop $i: ${tunable.path}. Got '$current', enforcing '${tunable.value}'")
                        
                        // Force Apply
                        customTunableRepository.applyTunable(tunable.path, tunable.value)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking ${tunable.path}", e)
                }
            }
            delay(3000)
        }
        Log.d(TAG, "Enforcement loop finished.")
    }

    private suspend fun restoreCpuSettings() {
        if (!preferenceManager.isApplyCpuOnBoot()) return

        // Restore Core Online Status
        for (coreId in 0..7) {
            preferenceManager.getCpuCoreOnline(coreId)?.let { online ->
                if (tuningRepository.setCoreOnline(coreId, online)) {
                    Log.d(TAG, "Restored Core $coreId online status: $online")
                }
            }
        }

        val clusters = tuningRepository.getClusterLeaders()
        clusters.forEach { cluster ->
            preferenceManager.getCpuGov(cluster)?.let { gov ->
                if (tuningRepository.setCpuGov(cluster, gov)) {
                    Log.d(TAG, "Restored CPU Gov for $cluster: $gov")
                }
            }

            val minFreq = preferenceManager.getCpuMinFreq(cluster)
            val maxFreq = preferenceManager.getCpuMaxFreq(cluster)
            if (minFreq != -1 && maxFreq != -1) {
                if (tuningRepository.setCpuFreq(cluster, minFreq, maxFreq)) {
                    Log.d(TAG, "Restored CPU Freqs for $cluster")
                }
            }
        }
    }

    private suspend fun restorePerformanceMode() {
        if (!preferenceManager.isApplyPerformanceModeOnBoot()) return

        val mode = preferenceManager.getPerformanceMode()
        val governor = when (mode) {
            "Performance" -> "performance"
            "Powersave" -> "powersave"
            else -> "schedutil"
        }

        val clusters = tuningRepository.getClusterLeaders()
        clusters.forEach { cluster ->
            tuningRepository.setCpuGov(cluster, governor)
        }
    }

    private suspend fun restoreGpuSettings() {
        if (!preferenceManager.isApplyGpuOnBoot()) return

        preferenceManager.getGpuGovernor()?.let { tuningRepository.setGpuGov(it) }
        
        val minFreq = preferenceManager.getGpuMinFreq()
        if (minFreq != -1) tuningRepository.setGpuMinFreq(minFreq)
        
        val maxFreq = preferenceManager.getGpuMaxFreq()
        if (maxFreq != -1) tuningRepository.setGpuMaxFreq(maxFreq)

        val powerLevel = preferenceManager.getGpuPowerLevel()
        if (powerLevel != -1) tuningRepository.setGpuPowerLevel(powerLevel.toFloat())

        preferenceManager.getGpuThrottling()?.let { systemRepository.setGpuThrottling(it) }
    }

    private suspend fun restoreRamSettings() {
        if (!preferenceManager.isApplyRamOnBoot()) return

        if (preferenceManager.isZramEnabledPref()) {
            val zramSize = preferenceManager.getZramDisksize()
            val zramAlgo = preferenceManager.getZramCompression()
            if (zramSize != -1L) {
                 tuningRepository.applyFullZramConfig(zramSize, zramAlgo)
            }
        } else {
            tuningRepository.setZramEnabled(false).first()
        }

        val swappiness = preferenceManager.getSwappiness()
        if (swappiness != -1) tuningRepository.setSwappiness(swappiness)

        val dirtyRatio = preferenceManager.getDirtyRatio()
        if (dirtyRatio != -1) tuningRepository.setDirtyRatio(dirtyRatio)

        val dirtyBg = preferenceManager.getDirtyBackgroundRatio()
        if (dirtyBg != -1) tuningRepository.setDirtyBackgroundRatio(dirtyBg)

        val dirtyWb = preferenceManager.getDirtyWriteback()
        if (dirtyWb != -1) tuningRepository.setDirtyWriteback(dirtyWb * 100)

        val dirtyExp = preferenceManager.getDirtyExpire()
        if (dirtyExp != -1) tuningRepository.setDirtyExpireCentisecs(dirtyExp)

        val minFree = preferenceManager.getMinFreeMemory()
        if (minFree != -1) tuningRepository.setMinFreeMemory(minFree * 1024)
    }

    private suspend fun restoreThermalSettings() {
        if (!preferenceManager.isApplyThermalOnBoot()) return

        val thermalPrefs = applicationContext.getSharedPreferences("thermal_settings_prefs", Context.MODE_PRIVATE)
        val lastSavedIndex = thermalPrefs.getInt("last_applied_thermal_index", -2)

        if (lastSavedIndex != -2) {
            val ok = thermalRepository.setThermalModeIndex(lastSavedIndex).first()
            if (ok && lastSavedIndex == 10) {
                // Dynamic Mode Service
                val intent = Intent(applicationContext, ThermalService::class.java)
                intent.putExtra("thermal_mode", lastSavedIndex)
                try {
                    ContextCompat.startForegroundService(applicationContext, intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start ThermalService", e)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Boot Restore Service",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Applying system settings on boot"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EKM: Restoring Settings")
            .setContentText("Applying kernel configurations...")
            .setSmallIcon(R.drawable.ic_speed)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
