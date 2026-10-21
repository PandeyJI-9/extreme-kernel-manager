package id.nkz.nokontzzzmanager.data.model

import id.nkz.nokontzzzmanager.data.database.AppProfileEntity
import id.nkz.nokontzzzmanager.data.database.GameEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val tuning: TuningSettings? = null,
    val networkStorage: NetworkStorageSettings? = null,
    val battery: BatterySettings? = null,
    val other: OtherSettings? = null,
    val customTunables: List<CustomTunableBackupItem>? = null,
    val appProfiles: List<AppProfileBackupItem>? = null,
    val games: List<GameBackupItem>? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isValid: Boolean
        get() = tuning != null || networkStorage != null || battery != null || other != null || 
                customTunables != null || appProfiles != null || games != null
}

@Serializable
data class AppProfileBackupItem(
    val packageName: String,
    val appName: String,
    val performanceMode: String,
    val kgslSkipZeroing: Boolean,
    val bypassCharging: Boolean,
    val allowDirtyPte: Boolean,
    val cpuConfigJson: String? = null,
    val gpuConfigJson: String? = null,
    val thermalProfile: Int? = null,
    val isEnabled: Boolean
)

@Serializable
data class GameBackupItem(
    val packageName: String,
    val appName: String,
    val isBenchmarkEnabled: Boolean,
    val targetFps: Int,
    val preferredLayerPattern: String? = null
)

@Serializable
data class CustomTunableBackupItem(
    val path: String,
    val value: String,
    val applyOnBoot: Boolean
)

@Serializable
data class TuningSettings(
    val thermalMode: Int? = null,
    val cpuGovernor: String? = null,
    val cpuMaxFreq: Int? = null,
    // Add GPU/RAM here if/when they are persisted
)

@Serializable
data class NetworkStorageSettings(
    val tcpCongestion: String? = null,
    val ioScheduler: String? = null,
    val applyOnBoot: Boolean? = null
)

@Serializable
data class BatterySettings(
    val bypassCharging: Boolean? = null,
    val forceFastCharge: Boolean? = null,
    val chargingControlEnabled: Boolean? = null,
    val stopLevel: Int? = null,
    val resumeLevel: Int? = null,
    val batteryMonitorEnabled: Boolean? = null,
    // History Auto-Reset
    val autoResetOnReboot: Boolean? = null,
    val autoResetOnCharging: Boolean? = null,
    val autoResetAtLevel: Boolean? = null,
    val autoResetTargetLevel: Int? = null,
    // Monitor Auto-Reset
    val monitorAutoResetOnReboot: Boolean? = null,
    val monitorAutoResetOnCharging: Boolean? = null,
    val monitorAutoResetAtLevel: Boolean? = null,
    val monitorAutoResetTargetLevel: Int? = null
)

@Serializable
data class OtherSettings(
    val kgslSkipZeroing: Boolean? = null,
    val avoidDirtyPte: Boolean? = null,
    val notificationIconStyle: Int? = null
)

data class BackupPreview(
    val hasTuning: Boolean,
    val hasNetwork: Boolean,
    val hasBattery: Boolean,
    val hasOther: Boolean,
    val hasCustomTunables: Boolean,
    val hasAppProfiles: Boolean,
    val hasGames: Boolean,
    val timestamp: Long
)
