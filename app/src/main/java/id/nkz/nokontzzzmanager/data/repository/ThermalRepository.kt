package id.nkz.nokontzzzmanager.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalRepository @Inject constructor(
    private val context: Context,
    private val rootRepository: RootRepository,
    private val thermalDataStore: DataStore<Preferences>
) {
    private val TAG = "ThermalRepository"
    private val thermalSysfsNode = "/sys/class/thermal/thermal_message/sconfig"
    private val serviceDir = "/data/adb/service.d"
    private val thermalScriptPath = "$serviceDir/thermal.sh"
    private val persistentScriptPath = "/data/adb/post-fs-data.d/thermal_persist.sh"

    private val LAST_THERMAL_MODE = intPreferencesKey("last_thermal_mode")
    private val USER_MAX_FREQ = intPreferencesKey("user_max_freq")
    private val USER_GOVERNOR = stringPreferencesKey("user_governor")

    data class ThermalProfile(val displayName: String, val index: Int)
    val availableThermalProfiles = listOf(
        ThermalProfile("Dynamic", 10),
        ThermalProfile("Class 0", 11),
        ThermalProfile("AR VR", 15),
        ThermalProfile("In-calls", 8),
        ThermalProfile("Game", 9),
        ThermalProfile("Game 2", 16),
        ThermalProfile("Extreme", 2),
        ThermalProfile("Pubg", 12),
        ThermalProfile("Camera", 13),
        ThermalProfile("Youtube", 14),
        ThermalProfile("Disabled", 0),
        ThermalProfile("Not Set", -1)
    )

    private var userSetMaxFreq: Int = 0
    private var userSetGovernor: String? = null

    private suspend fun executeRootCommand(cmd: String, logTag: String = TAG): Boolean {
        if (!rootRepository.checkRootFresh()) {
            Log.e(logTag, "Root access not available for command: $cmd")
            return false
        }
        return try {
            rootRepository.run(cmd)
            Log.i(logTag, "Root Command Success: '$cmd'")
            true
        } catch (e: Exception) {
            Log.e(logTag, "Exception during root command: '$cmd'", e)
            false
        }
    }

    private suspend fun readRootCommand(cmd: String, logTag: String = TAG): String? {
        if (!rootRepository.checkRootFresh()) {
            Log.e(logTag, "Root access not available for command: $cmd")
            return null
        }
        return try {
            val result = rootRepository.run(cmd)
            Log.i(logTag, "Read Root Command Success: '$cmd'. Output: $result")
            result.trim()
        } catch (e: Exception) {
            Log.e(logTag, "Exception during read root command: '$cmd'", e)
            null
        }
    }

    private suspend fun createPersistentScript(modeIndex: Int): Boolean = withContext(Dispatchers.IO) {
        val scriptContent = """
            #!/system/bin/sh

            # Wait for system to fully boot
            until [ -d /sys/class/thermal ]; do
                sleep 1
            done

            # Set SELinux context for thermal access
            chcon u:object_r:sysfs_thermal:s0 $thermalSysfsNode
            chmod 0644 $thermalSysfsNode

            # Apply thermal mode
            echo "$modeIndex" > $thermalSysfsNode

            # Lock permissions
            chmod 0444 $thermalSysfsNode

            exit 0
        """.trimIndent()

        executeRootCommand("mkdir -p /data/adb/post-fs-data.d")
        executeRootCommand("chmod 755 /data/adb/post-fs-data.d")

        val writeResult = executeRootCommand("cat > '$persistentScriptPath' << 'EOF'\n$scriptContent\nEOF")
        if (!writeResult) {
            Log.e(TAG, "Failed to write persistent script")
            return@withContext false
        }

        executeRootCommand("chmod 755 '$persistentScriptPath'")
        executeRootCommand("chown root:root '$persistentScriptPath'")

        return@withContext true
    }

    private suspend fun updateThermalScript(modeIndex: Int): Boolean = withContext(Dispatchers.IO) {
        if (modeIndex <= 0) {
            Log.w(TAG, "updateThermalScript: Invalid modeIndex $modeIndex for creating a script. Aborting.")
            return@withContext false
        }
        val serviceResult = createServiceScript(modeIndex)
        val persistentResult = createPersistentScript(modeIndex)
        return@withContext serviceResult && persistentResult
    }

    private suspend fun createServiceScript(modeIndex: Int): Boolean = withContext(Dispatchers.IO) {
        val scriptContent = """
            #!/system/bin/sh

            # Wait for thermal system
            until [ -d /sys/class/thermal ]; do
                sleep 1
            done

            # Set SELinux context for thermal access
            chcon u:object_r:sysfs_thermal:s0 $thermalSysfsNode
            chmod 0644 $thermalSysfsNode

            # Apply thermal mode
            echo "$modeIndex" > $thermalSysfsNode

            # Lock permissions
            chmod 0444 $thermalSysfsNode

            exit 0
        """.trimIndent()

        executeRootCommand("mkdir -p '$serviceDir'")
        executeRootCommand("chmod 755 '$serviceDir'")

        val writeResult = executeRootCommand("cat > '$thermalScriptPath' << 'EOF'\n$scriptContent\nEOF")
        if (!writeResult) {
            return@withContext false
        }

        executeRootCommand("chmod 755 '$thermalScriptPath'")
        executeRootCommand("chown root:root '$thermalScriptPath'")

        return@withContext true
    }

    private suspend fun removeThermalScript(): Boolean = withContext(Dispatchers.IO) {
        if (!rootRepository.checkRootFresh()) {
            Log.e(TAG, "removeThermalScript: Root access is not available.")
            return@withContext false
        }
        val rmSuccess = executeRootCommand("rm -f '$thermalScriptPath'")
        if (rmSuccess) {
            Log.i(TAG, "removeThermalScript: Successfully removed $thermalScriptPath")
            return@withContext true
        } else {
            Log.e(TAG, "removeThermalScript: Failed to remove $thermalScriptPath")
            return@withContext false
        }
    }

    private suspend fun restoreLastThermalMode() {
        try {
            val lastMode = thermalDataStore.data.first()[LAST_THERMAL_MODE] ?: 0
            if (lastMode != 0) {
                setThermalModeIndex(lastMode).collect { success ->
                    if (success) {
                        Log.i(TAG, "Successfully restored thermal mode: $lastMode")
                    } else {
                        Log.e(TAG, "Failed to restore thermal mode: $lastMode")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore last thermal mode", e)
        }
    }

    private suspend fun saveLastThermalMode(modeIndex: Int) {
        try {
            thermalDataStore.edit { preferences ->
                preferences[LAST_THERMAL_MODE] = modeIndex
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save last thermal mode", e)
        }
    }

    fun getCurrentThermalModeIndex(): Flow<Int> = flow {
        val output = readRootCommand("cat '$thermalSysfsNode'")
        val value = output?.toIntOrNull() ?: -1
        emit(value)
    }.flowOn(Dispatchers.IO)

    fun setThermalModeIndex(modeIndex: Int): Flow<Boolean> = flow {
        if (availableThermalProfiles.none { it.index == modeIndex }) {
            Log.w(TAG, "setThermalModeIndex: Mode index $modeIndex is not supported.")
            emit(false)
            return@flow
        }

        if (!rootRepository.checkRootFresh()) {
            Log.e(TAG, "setThermalModeIndex: Root access is not available.")
            emit(false)
            return@flow
        }

        // Apply thermal mode with necessary permission handling for sysfs
        // Some kernels/engines lock this node with 0444
        executeRootCommand("chcon u:object_r:sysfs_thermal:s0 $thermalSysfsNode")
        executeRootCommand("chmod 0644 '$thermalSysfsNode'")
        
        val writeOk = executeRootCommand("echo $modeIndex > '$thermalSysfsNode'")
        
        // Lock with read-only permissions to prevent system engine from overriding our setting
        executeRootCommand("chmod 0444 '$thermalSysfsNode'")

        if (!writeOk) {
            Log.e(TAG, "setThermalModeIndex: Failed to write $modeIndex to $thermalSysfsNode")
            emit(false)
            return@flow
        }

        thermalDataStore.edit { preferences ->
            preferences[LAST_THERMAL_MODE] = modeIndex
        }

        val scriptOperationSuccess = if (modeIndex == 0) {
            removeThermalScript()
        } else {
            updateThermalScript(modeIndex)
        }

        if (!scriptOperationSuccess) {
            Log.w(TAG, "setThermalModeIndex: Failed to update/remove the thermal boot script.")
        }

        delay(300)

        val verifyValue = readRootCommand("cat '$thermalSysfsNode'")?.toIntOrNull() ?: -1
        val success = verifyValue == modeIndex

        if (success) {
            Log.i(TAG, "setThermalModeIndex: Successfully set and verified thermal mode $modeIndex")
        } else {
            Log.e(TAG, "setThermalModeIndex: Failed to verify thermal mode. Expected $modeIndex but got $verifyValue")
        }

        emit(success)
    }.flowOn(Dispatchers.IO)

    suspend fun setUserCPUSettings(maxFreq: Int, governor: String) {
        userSetMaxFreq = maxFreq
        userSetGovernor = governor
        withContext(Dispatchers.IO) {
            thermalDataStore.edit { preferences ->
                preferences[USER_MAX_FREQ] = maxFreq
                preferences[USER_GOVERNOR] = governor
            }
        }
    }

    private suspend fun restoreUserSettings() {
        val prefs = thermalDataStore.data.first()
        userSetMaxFreq = prefs[USER_MAX_FREQ] ?: 0
        userSetGovernor = prefs[USER_GOVERNOR]

        if (userSetMaxFreq > 0 || !userSetGovernor.isNullOrEmpty()) {
            val currentMode = readRootCommand("cat '$thermalSysfsNode'")?.toIntOrNull() ?: 0
            if (currentMode == 10) {
                setThermalModeIndex(10).collect()
            }
        }
    }

    init {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            restoreUserSettings()
        }
    }

    fun getSupportedThermalProfiles(): Flow<List<ThermalProfile>> = flow {
        emit(availableThermalProfiles)
    }.flowOn(Dispatchers.IO)

    fun getCurrentThermalProfileName(currentIndex: Int): String {
        return availableThermalProfiles.find { it.index == currentIndex }?.displayName
            ?: if (currentIndex == -1) "Not Set"
            else "Unknown ($currentIndex)"
    }

    suspend fun getSavedThermalMode(): Int {
        return thermalDataStore.data.first()[LAST_THERMAL_MODE] ?: 0
    }
}