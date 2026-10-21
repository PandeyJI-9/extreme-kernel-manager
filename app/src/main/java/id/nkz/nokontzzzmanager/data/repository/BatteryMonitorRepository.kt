package id.nkz.nokontzzzmanager.data.repository

import android.content.Context
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import id.nkz.nokontzzzmanager.data.model.BatteryMonitorStats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryMonitorRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs by lazy {
        // Must match the name used in BatteryMonitorService
        context.createDeviceProtectedStorageContext().getSharedPreferences("battery_monitor_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Reads the current accumulated stats from BatteryMonitorService's shared preferences.
     * Note: These values are updated every 60 seconds or on power events.
     * Logic mimics BatteryMonitorService's restoration logic to present a consistent view.
     */
    fun getMonitorStats(): BatteryMonitorStats {
        val lastElapsed = prefs.getLong("last_elapsed_ms", 0L)
        // val lastUptime = prefs.getLong("last_uptime_ms", 0L) // Not strictly needed if we assume no reboot for live calc
        val savedAccum = prefs.getLong("screen_accum_ms", 0L)
        val savedWindowStart = prefs.getLong("window_start_elapsed", -1L)
        val savedWindowAccum = prefs.getLong("window_accum_ms", 0L)
        val savedWindowStartUptime = prefs.getLong("window_start_uptime", -1L)
        val savedAwakeAccum = prefs.getLong("awake_accum_ms", 0L)
        
        // Migration heuristic: If awake history is missing (from old version bug) but we have screen on history,
        // use Screen On as the minimum Awake time.
        val effectiveSavedAwake = if (savedAwakeAccum == 0L && savedAccum > 0L) savedAccum else savedAwakeAccum

        // Drain rates
        val savedOnDrop = java.lang.Double.longBitsToDouble(prefs.getLong("on_percent_drop_bits", 0L))
        val savedOffDrop = java.lang.Double.longBitsToDouble(prefs.getLong("off_percent_drop_bits", 0L))

        val now = SystemClock.elapsedRealtime()
        val nowUptime = SystemClock.uptimeMillis()

        // Check for reboot (if lastElapsed is in the future relative to now, or 0)
        val isReboot = (lastElapsed > 0L && now < lastElapsed)

        val totalScreenOnMs = savedAccum

        // If reboot detected and service hasn't restored yet, return snapshot
        val (totalWindowMs, totalAwakeMs) = if (isReboot) {
            Pair(savedWindowAccum, effectiveSavedAwake)
        } else {
            // Live calculation assuming continuous session
            val currentSessionWindowMs = if (savedWindowStart >= 0) (now - savedWindowStart).coerceAtLeast(0L) else 0L
            val totalWindow = savedWindowAccum + currentSessionWindowMs
            
            val currentSessionAwakeMs = if (savedWindowStartUptime >= 0) (nowUptime - savedWindowStartUptime).coerceAtLeast(0L) else 0L
            val totalAwake = effectiveSavedAwake + currentSessionAwakeMs
            
            Pair(totalWindow, totalAwake)
        }

        val totalScreenOffMs = (totalWindowMs - totalScreenOnMs).coerceAtLeast(0L)
        val totalDeepSleepMs = (totalWindowMs - totalAwakeMs).coerceAtLeast(0L)
        
        // Drain Rates
        val onHours = totalScreenOnMs / 3_600_000.0
        val offHours = totalScreenOffMs / 3_600_000.0
        
        val activeRate = if (onHours > 0) (savedOnDrop / onHours).toFloat().coerceAtLeast(0f) else 0f
        val idleRate = if (offHours > 0) (savedOffDrop / offHours).toFloat().coerceAtLeast(0f) else 0f
        
        return BatteryMonitorStats(
            screenOnMs = totalScreenOnMs,
            screenOffMs = totalScreenOffMs,
            awakeMs = totalAwakeMs,
            deepSleepMs = totalDeepSleepMs,
            activeDrainRate = activeRate,
            idleDrainRate = idleRate,
            activeDrainPct = savedOnDrop.toFloat().coerceAtLeast(0f),
            idleDrainPct = savedOffDrop.toFloat().coerceAtLeast(0f),
            lastUpdateTime = if (isReboot) lastElapsed else now
        )
    }
}
