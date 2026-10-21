package id.nkz.nokontzzzmanager.data.model

data class BatteryMonitorStats(
    val screenOnMs: Long,
    val screenOffMs: Long,
    val awakeMs: Long,
    val deepSleepMs: Long,
    val activeDrainRate: Float, // %/hr
    val idleDrainRate: Float, // %/hr
    val activeDrainPct: Float,
    val idleDrainPct: Float,
    val lastUpdateTime: Long = 0L
)