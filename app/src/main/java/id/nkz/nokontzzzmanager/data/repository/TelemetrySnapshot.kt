package id.nkz.nokontzzzmanager.data.repository

import kotlinx.serialization.Serializable

@Serializable
data class TelemetrySnapshot(
    val schemaVersion: Int,
    val nativeAvailable: Boolean,
    val cpu: List<CpuTelemetrySnapshot> = emptyList(),
    val gpu: GpuTelemetrySnapshot? = null,
    val thermal: List<ThermalTelemetrySnapshot> = emptyList(),
    val zram: ZramTelemetrySnapshot? = null,
    val battery: BatteryTelemetrySnapshot? = null,
    val errors: List<String> = emptyList(),
)

@Serializable
data class CpuTelemetrySnapshot(
    val core: Int,
    val online: Boolean? = null,
    val currentFreqKhz: Long? = null,
    val minFreqKhz: Long? = null,
    val maxFreqKhz: Long? = null,
    val governor: String? = null,
)

@Serializable
data class GpuTelemetrySnapshot(
    val currentFreqHz: Long? = null,
    val maxFreqHz: Long? = null,
    val usagePercent: Int? = null,
)

@Serializable
data class ThermalTelemetrySnapshot(
    val zone: String,
    val type: String? = null,
    val tempMilliCelsius: Long? = null,
)

@Serializable
data class ZramTelemetrySnapshot(
    val disksizeBytes: Long? = null,
    val usedBytes: Long? = null,
    val swapTotalBytes: Long? = null,
    val swapUsedBytes: Long? = null,
    val memTotalBytes: Long? = null,
    val memAvailableBytes: Long? = null,
)

@Serializable
data class BatteryTelemetrySnapshot(
    val levelPercent: Int? = null,
    val tempDeciCelsius: Long? = null,
    val voltageMicrovolts: Long? = null,
    val currentMicroamps: Long? = null,
    val status: String? = null,
)
