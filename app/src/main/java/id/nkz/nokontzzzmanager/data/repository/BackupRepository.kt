package id.nkz.nokontzzzmanager.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import id.nkz.nokontzzzmanager.data.model.*
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import id.nkz.nokontzzzmanager.service.BatteryMonitorService
import kotlinx.coroutines.flow.first

@Singleton
class BackupRepository @Inject constructor(
    private val context: Context,
    private val preferenceManager: PreferenceManager,
    private val persistentSettingsManager: PersistentSettingsManager,
    private val systemRepository: SystemRepository,
    private val tuningRepository: TuningRepository,
    private val customTunableRepository: CustomTunableRepository,
    private val appProfileRepository: AppProfileRepository,
    private val gameRepository: GameRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun createBackup(
        uri: Uri,
        includeTuning: Boolean,
        includeNetwork: Boolean,
        includeBattery: Boolean,
        includeOther: Boolean,
        includeCustomTunables: Boolean,
        includeAppProfiles: Boolean,
        includeGames: Boolean
    ): Result<String> {
        return try {
            val tuningSettings = if (includeTuning) {
                val (gov, freq) = persistentSettingsManager.getCpu7Settings()
                TuningSettings(
                    thermalMode = persistentSettingsManager.getLastThermalMode(),
                    cpuGovernor = gov,
                    cpuMaxFreq = freq
                )
            } else null

            val networkSettings = if (includeNetwork) {
                NetworkStorageSettings(
                    tcpCongestion = preferenceManager.getTcpCongestionAlgorithm(),
                    ioScheduler = preferenceManager.getIoScheduler(),
                    applyOnBoot = preferenceManager.isApplyNetworkStorageOnBoot()
                )
            } else null

            val batterySettings = if (includeBattery) {
                BatterySettings(
                    bypassCharging = preferenceManager.getBypassCharging(),
                    forceFastCharge = preferenceManager.getForceFastCharge(),
                    chargingControlEnabled = preferenceManager.isChargingControlEnabled(),
                    stopLevel = preferenceManager.getChargingControlStopLevel(),
                    resumeLevel = preferenceManager.getChargingControlResumeLevel(),
                    batteryMonitorEnabled = preferenceManager.isBatteryMonitorEnabled(),
                    // History Auto-Reset
                    autoResetOnReboot = preferenceManager.isAutoResetOnReboot(),
                    autoResetOnCharging = preferenceManager.isAutoResetOnCharging(),
                    autoResetAtLevel = preferenceManager.isAutoResetAtLevel(),
                    autoResetTargetLevel = preferenceManager.getAutoResetTargetLevel(),
                    // Monitor Auto-Reset
                    monitorAutoResetOnReboot = preferenceManager.isMonitorAutoResetOnReboot(),
                    monitorAutoResetOnCharging = preferenceManager.isMonitorAutoResetOnCharging(),
                    monitorAutoResetAtLevel = preferenceManager.isMonitorAutoResetAtLevel(),
                    monitorAutoResetTargetLevel = preferenceManager.getMonitorAutoResetTargetLevel()
                )
            } else null

            val otherSettings = if (includeOther) {
                OtherSettings(
                    kgslSkipZeroing = preferenceManager.getKgslSkipZeroing(),
                    avoidDirtyPte = preferenceManager.getAvoidDirtyPte(),
                    notificationIconStyle = preferenceManager.getNotificationIconStyle()
                )
            } else null
            
            val customTunables = if (includeCustomTunables) {
                customTunableRepository.getAllTunables().first().map {
                    CustomTunableBackupItem(it.path, it.value, it.applyOnBoot)
                }
            } else null
            
            val appProfiles = if (includeAppProfiles) {
                appProfileRepository.getAllProfiles().first().map {
                    AppProfileBackupItem(
                        packageName = it.packageName,
                        appName = it.appName,
                        performanceMode = it.performanceMode,
                        kgslSkipZeroing = it.kgslSkipZeroing,
                        bypassCharging = it.bypassCharging,
                        allowDirtyPte = it.allowDirtyPte,
                        cpuConfigJson = it.cpuConfigJson,
                        gpuConfigJson = it.gpuConfigJson,
                        thermalProfile = it.thermalProfile,
                        isEnabled = it.isEnabled
                    )
                }
            } else null
            
            val games = if (includeGames) {
                gameRepository.getAllGames().first().map {
                    GameBackupItem(
                        packageName = it.packageName,
                        appName = it.appName,
                        isBenchmarkEnabled = it.isBenchmarkEnabled,
                        targetFps = it.targetFps,
                        preferredLayerPattern = it.preferredLayerPattern
                    )
                }
            } else null

            val backupData = BackupData(
                tuning = tuningSettings,
                networkStorage = networkSettings,
                battery = batterySettings,
                other = otherSettings,
                customTunables = customTunables,
                appProfiles = appProfiles,
                games = games
            )

            val jsonString = json.encodeToString(backupData)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: return Result.failure(Exception("Failed to open output stream"))

            Result.success("Backup saved successfully")
        } catch (e: Exception) {
            Log.e("BackupRepository", "Backup failed", e)
            Result.failure(e)
        }
    }

        suspend fun restoreBackup(
            uri: Uri,
            restoreTuning: Boolean,
            restoreNetwork: Boolean,
            restoreBattery: Boolean,
            restoreOther: Boolean,
            restoreCustomTunables: Boolean,
            restoreAppProfiles: Boolean,
            restoreGames: Boolean
        ): Result<Boolean> {
            return try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: return Result.failure(Exception("Failed to open input stream"))
    
                            val backupData = json.decodeFromString<BackupData>(jsonString)
                
                            if (!backupData.isValid) {
                                return Result.failure(Exception("Invalid or empty backup file"))
                            }
                
                            if (restoreTuning && backupData.tuning != null) {
                    // Restore Thermal Mode
                    backupData.tuning.thermalMode?.let { persistentSettingsManager.saveThermalMode(it) }
    
                    // Restore CPU Settings (Governor & Freq) with Validation
                    if (backupData.tuning.cpuGovernor != null || backupData.tuning.cpuMaxFreq != null) {
                        val targetCluster = "cpu7" // Primary/Prime cluster usually determines the main perf profile
                        
                        // Validate Governor
                        var validGovernor: String? = null
                        if (backupData.tuning.cpuGovernor != null) {
                            val availableGovs = tuningRepository.getAvailableCpuGovernors(targetCluster).first()
                            if (availableGovs.contains(backupData.tuning.cpuGovernor)) {
                                validGovernor = backupData.tuning.cpuGovernor
                            } else {
                                Log.w("BackupRepository", "Skipping unsupported governor: ${backupData.tuning.cpuGovernor}")
                            }
                        }
    
                        // Validate Frequency
                        var validFreq: Int? = null
                        if (backupData.tuning.cpuMaxFreq != null) {
                            val availableFreqs = tuningRepository.getAvailableCpuFrequencies(targetCluster).first()
                            // Allow exact match or closest lower match (safety)? For simplicity, exact match first.
                            // Or simply check if it's within range. Ideally, check availability.
                            if (availableFreqs.contains(backupData.tuning.cpuMaxFreq)) {
                                validFreq = backupData.tuning.cpuMaxFreq
                            } else {
                                Log.w("BackupRepository", "Skipping unsupported frequency: ${backupData.tuning.cpuMaxFreq}")
                            }
                        }
    
                        // Save only valid values. If one is null, preserve current (handled by PersistentSettingsManager logic ideally, 
                        // but here we are calling saveCpu7Settings which takes both. We need current values if we only have one valid.)
                        if (validGovernor != null || validFreq != null) {
                            val (currentGov, currentFreq) = persistentSettingsManager.getCpu7Settings()
                            persistentSettingsManager.saveCpu7Settings(
                                validGovernor ?: currentGov,
                                validFreq ?: currentFreq
                            )
                        }
                    }
                    
                    persistentSettingsManager.applyLastKnownSettings()
                }
    
                if (restoreNetwork && backupData.networkStorage != null) {
                    backupData.networkStorage.tcpCongestion?.let { 
                        // Only save preference if system successfully applied it (meaning it's valid/available)
                        if (systemRepository.setTcpCongestionAlgorithm(it)) {
                            preferenceManager.setTcpCongestionAlgorithm(it)
                        } else {
                            Log.w("BackupRepository", "Skipping unsupported TCP algorithm: $it")
                        }
                    }
                    backupData.networkStorage.ioScheduler?.let { 
                        if (systemRepository.setIoScheduler(it)) {
                            preferenceManager.setIoScheduler(it)
                        } else {
                            Log.w("BackupRepository", "Skipping unsupported I/O scheduler: $it")
                        }
                    }
                    backupData.networkStorage.applyOnBoot?.let { preferenceManager.setApplyNetworkStorageOnBoot(it) }
                }
    
                if (restoreBattery && backupData.battery != null) {
                    backupData.battery.bypassCharging?.let { 
                        if (systemRepository.setBypassCharging(it)) {
                            preferenceManager.setBypassCharging(it) 
                        }
                    }
                    backupData.battery.forceFastCharge?.let { 
                        if (systemRepository.setForceFastCharge(it)) {
                            preferenceManager.setForceFastCharge(it)
                        }
                    }
                    // Charging control logic is software-based (service), so we can restore preferences directly
                    // assuming the service handles capability checks or just doesn't run if unsupported.
                    backupData.battery.chargingControlEnabled?.let { preferenceManager.setChargingControlEnabled(it) }
                    backupData.battery.stopLevel?.let { preferenceManager.setChargingControlStopLevel(it) }
                    backupData.battery.resumeLevel?.let { preferenceManager.setChargingControlResumeLevel(it) }
                    
                    // History Auto-Reset
                    backupData.battery.autoResetOnReboot?.let { preferenceManager.setAutoResetOnReboot(it) }
                    backupData.battery.autoResetOnCharging?.let { preferenceManager.setAutoResetOnCharging(it) }
                    backupData.battery.autoResetAtLevel?.let { preferenceManager.setAutoResetAtLevel(it) }
                    backupData.battery.autoResetTargetLevel?.let { preferenceManager.setAutoResetTargetLevel(it) }
                    
                    // Monitor Auto-Reset
                    backupData.battery.monitorAutoResetOnReboot?.let { preferenceManager.setMonitorAutoResetOnReboot(it) }
                    backupData.battery.monitorAutoResetOnCharging?.let { preferenceManager.setMonitorAutoResetOnCharging(it) }
                    backupData.battery.monitorAutoResetAtLevel?.let { preferenceManager.setMonitorAutoResetAtLevel(it) }
                    backupData.battery.monitorAutoResetTargetLevel?.let { preferenceManager.setMonitorAutoResetTargetLevel(it) }
                    
                    backupData.battery.batteryMonitorEnabled?.let { 
                        preferenceManager.setBatteryMonitorEnabled(it)
                        if (it) {
                            BatteryMonitorService.start(context)
                        } else {
                            BatteryMonitorService.stop(context)
                        }
                    }
                }
    
                if (restoreOther && backupData.other != null) {
                    backupData.other.kgslSkipZeroing?.let { 
                        if (systemRepository.setKgslSkipZeroing(it)) {
                            preferenceManager.setKgslSkipZeroing(it)
                        }
                    }
                    backupData.other.avoidDirtyPte?.let { 
                        if (systemRepository.setAvoidDirtyPte(it)) {
                            preferenceManager.setAvoidDirtyPte(it)
                        }
                    }
                    // Notification icon is UI preference, always safe to restore
                    backupData.other.notificationIconStyle?.let { 
                        preferenceManager.setNotificationIconStyle(it)
                        BatteryMonitorService.updateIcon(context)
                    }
                }
                
                if (restoreCustomTunables && !backupData.customTunables.isNullOrEmpty()) {
                    backupData.customTunables.forEach { item ->
                        customTunableRepository.insertTunable(
                            id.nkz.nokontzzzmanager.data.database.CustomTunableEntity(
                                path = item.path,
                                value = item.value,
                                applyOnBoot = item.applyOnBoot
                            )
                        )
                    }
                }
                
                if (restoreAppProfiles && !backupData.appProfiles.isNullOrEmpty()) {
                    backupData.appProfiles.forEach { item ->
                        appProfileRepository.insertProfile(
                            id.nkz.nokontzzzmanager.data.database.AppProfileEntity(
                                packageName = item.packageName,
                                appName = item.appName,
                                performanceMode = item.performanceMode,
                                kgslSkipZeroing = item.kgslSkipZeroing,
                                bypassCharging = item.bypassCharging,
                                allowDirtyPte = item.allowDirtyPte,
                                cpuConfigJson = item.cpuConfigJson,
                                gpuConfigJson = item.gpuConfigJson,
                                thermalProfile = item.thermalProfile,
                                isEnabled = item.isEnabled
                            )
                        )
                    }
                }
                
                if (restoreGames && !backupData.games.isNullOrEmpty()) {
                    backupData.games.forEach { item ->
                        gameRepository.insertGame(
                            id.nkz.nokontzzzmanager.data.database.GameEntity(
                                packageName = item.packageName,
                                appName = item.appName,
                                isBenchmarkEnabled = item.isBenchmarkEnabled,
                                targetFps = item.targetFps,
                                preferredLayerPattern = item.preferredLayerPattern
                            )
                        )
                    }
                }
    
                Result.success(true)
            } catch (e: Exception) {
                Log.e("BackupRepository", "Restore failed", e)
                Result.failure(e)
            }
        }

    suspend fun getBackupPreview(uri: Uri): Result<BackupPreview> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return Result.failure(Exception("Failed to open input stream"))

            val data = json.decodeFromString<BackupData>(jsonString)
            
            if (!data.isValid) {
                return Result.failure(Exception("Invalid or empty backup file"))
            }

            Result.success(
                BackupPreview(
                    hasTuning = data.tuning != null,
                    hasNetwork = data.networkStorage != null,
                    hasBattery = data.battery != null,
                    hasOther = data.other != null,
                    hasCustomTunables = !data.customTunables.isNullOrEmpty(),
                    hasAppProfiles = !data.appProfiles.isNullOrEmpty(),
                    hasGames = !data.games.isNullOrEmpty(),
                    timestamp = data.timestamp
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
