package id.nkz.nokontzzzmanager.data.model

data class WakelockInfo(
    val name: String,
    val activeCount: Long = 0,
    val wakeupCount: Long = 0,
    val totalTimeMs: Long = 0,
    val activeTimeMs: Long = 0,
    val maxTimeMs: Long = 0,
    val lastChangeMs: Long = 0,
    val preventSuspendTimeMs: Long = 0
)
