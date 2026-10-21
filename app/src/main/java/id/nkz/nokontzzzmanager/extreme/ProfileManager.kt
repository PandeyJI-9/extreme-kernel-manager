package id.nkz.nokontzzzmanager.extreme

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.extremeProfileStore: DataStore<Preferences> by preferencesDataStore(name = "extreme_profiles")

enum class ExtremeProfile(
    val id: String,
    val title: String,
    val description: String,
    val primeMaxFreqKHz: Int,
    val goldMaxFreqKHz: Int,
    val littleMaxFreqKHz: Int,
    val primeMinFreqKHz: Int,
    val goldMinFreqKHz: Int,
    val littleMinFreqKHz: Int,
    val gpuMaxPwrLevel: Int, // 0 = 670MHz, 1 = 587MHz, 2 = 490MHz
    val govUpRateUs: Int,
    val govDownRateUs: Int
) {
    BATTERY_SAVER(
        id = "battery_saver",
        title = "Battery Saver",
        description = "Clamped to 2.42 GHz Peak | GPU Capped 490MHz | Smooth Decay",
        primeMaxFreqKHz = 2419200,
        goldMaxFreqKHz = 2246400,
        littleMaxFreqKHz = 1804800,
        primeMinFreqKHz = 844800,
        goldMinFreqKHz = 710400,
        littleMinFreqKHz = 576000,
        gpuMaxPwrLevel = 2,
        govUpRateUs = 1000,
        govDownRateUs = 10000
    ),
    BALANCED_CRUISE(
        id = "balanced_cruise",
        title = "Balanced Cruise",
        description = "2.84 GHz Sweet Spot | 120 FPS Pacing | GPU 587MHz",
        primeMaxFreqKHz = 2841600,
        goldMaxFreqKHz = 2419200,
        littleMaxFreqKHz = 1804800,
        primeMinFreqKHz = 844800,
        goldMinFreqKHz = 710400,
        littleMinFreqKHz = 576000,
        gpuMaxPwrLevel = 1,
        govUpRateUs = 500,
        govDownRateUs = 20000
    ),
    APEX_TURBO(
        id = "apex_turbo",
        title = "Apex Turbo Gaming",
        description = "3.19 GHz Unlocked Monster | GPU 670MHz Uncapped | L3 Cache Priority",
        primeMaxFreqKHz = 3187200,
        goldMaxFreqKHz = 2419200,
        littleMaxFreqKHz = 1804800,
        primeMinFreqKHz = 844800,
        goldMinFreqKHz = 710400,
        littleMinFreqKHz = 576000,
        gpuMaxPwrLevel = 0,
        govUpRateUs = 250,
        govDownRateUs = 40000
    )
}

@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kernelDetector: KernelDetector
) {
    private val TAG = "ExtremeProfileManager"

    companion object {
        private val KEY_ACTIVE_PROFILE = stringPreferencesKey("active_extreme_profile")

        const val CONF_DIR = "/data/adb/extreme"
        const val CONF_FILE = "$CONF_DIR/profile.conf"
        const val SERVICE_D_DIR = "/data/adb/service.d"
        const val BOOT_SCRIPT = "$SERVICE_D_DIR/extreme_profile.sh"

        // Sysfs Paths for SM8250 (POCO F4)
        const val POLICY0_DIR = "/sys/devices/system/cpu/cpufreq/policy0"
        const val POLICY4_DIR = "/sys/devices/system/cpu/cpufreq/policy4"
        const val POLICY7_DIR = "/sys/devices/system/cpu/cpufreq/policy7"

        const val GPU_DEV_DIR = "/sys/class/kgsl/kgsl-3d0"
        const val GPU_MAX_PWRLEVEL = "$GPU_DEV_DIR/max_pwrlevel"
        const val GPU_MIN_PWRLEVEL = "$GPU_DEV_DIR/min_pwrlevel"
        const val GPU_FORCE_BUS_ON = "$GPU_DEV_DIR/bus_split"
        const val LLCC_BW_MIN_FREQ = "/sys/class/devfreq/soc:qcom,cpu-cpu-llcc-bw/min_freq"
    }

    val activeProfileFlow: Flow<ExtremeProfile> = context.extremeProfileStore.data.map { preferences ->
        val profileId = preferences[KEY_ACTIVE_PROFILE] ?: ExtremeProfile.BALANCED_CRUISE.id
        ExtremeProfile.values().find { it.id == profileId } ?: ExtremeProfile.BALANCED_CRUISE
    }

    suspend fun applyProfile(profile: ExtremeProfile): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Applying Extreme Profile: ${profile.title}")

        val commands = mutableListOf<String>()

        // 1. Little Cluster (Policy 0: CPU 0-3)
        applyClusterLimits(POLICY0_DIR, profile.littleMinFreqKHz, profile.littleMaxFreqKHz, profile, commands)

        // 2. Gold Cluster (Policy 4: CPU 4-6)
        applyClusterLimits(POLICY4_DIR, profile.goldMinFreqKHz, profile.goldMaxFreqKHz, profile, commands)

        // 3. Prime Cluster (Policy 7: CPU 7)
        applyClusterLimits(POLICY7_DIR, profile.primeMinFreqKHz, profile.primeMaxFreqKHz, profile, commands)

        // 4. GPU Tuning
        applyGpuLimits(profile, commands)

        // 5. Memory Bus & L3 Cache Locks for Turbo
        if (profile == ExtremeProfile.APEX_TURBO) {
            writeNodeDualTier(LLCC_BW_MIN_FREQ, "10000000", commands)
        }

        // Execute any commands that couldn't be done via Tier 1 direct writes
        if (commands.isNotEmpty()) {
            val res = Shell.cmd(*commands.toTypedArray()).exec()
            if (!res.isSuccess) {
                Log.w(TAG, "Shell execution had warnings: ${res.err.joinToString("\n")}")
            }
        }

        // 6. Persist to DataStore
        context.extremeProfileStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE] = profile.id
        }

        // 7. Write to Root Boot Hook & Persistence files
        persistProfileConfig(profile)

        true
    }

    suspend fun applyAndReboot(profile: ExtremeProfile): Boolean = withContext(Dispatchers.IO) {
        val applied = applyProfile(profile)
        if (!applied) return@withContext false

        // Clean cache flush before reboot
        val rebootCommands = arrayOf(
            "sync",
            "echo 3 > /proc/sys/vm/drop_caches",
            "svc power reboot || reboot"
        )
        Shell.cmd(*rebootCommands).exec()
        true
    }

    private fun applyClusterLimits(
        policyDir: String,
        minFreq: Int,
        maxFreq: Int,
        profile: ExtremeProfile,
        shellCommands: MutableList<String>
    ) {
        writeNodeDualTier("$policyDir/scaling_min_freq", minFreq.toString(), shellCommands)
        writeNodeDualTier("$policyDir/scaling_max_freq", maxFreq.toString(), shellCommands)

        // Enforce EXTREME++ kernel custom governor 'extreme+' if supported
        shellCommands.add("""
            if grep -q "extreme+" "$policyDir/scaling_available_governors" 2>/dev/null; then
                echo "extreme+" > "$policyDir/scaling_governor" 2>/dev/null
            fi
        """.trimIndent())

        // Governor Rate Limits for extreme+ and fallback governors (schedutil, walt)
        for (gov in listOf("extreme+", "schedutil", "walt")) {
            writeNodeDualTier("$policyDir/$gov/up_rate_limit_us", profile.govUpRateUs.toString(), shellCommands)
            writeNodeDualTier("$policyDir/$gov/down_rate_limit_us", profile.govDownRateUs.toString(), shellCommands)
        }
    }

    private fun applyGpuLimits(profile: ExtremeProfile, shellCommands: MutableList<String>) {
        // max_pwrlevel: lower number means higher max clock (0 is 670MHz on Munch Adreno 650)
        writeNodeDualTier(GPU_MAX_PWRLEVEL, profile.gpuMaxPwrLevel.toString(), shellCommands)
    }

    /**
     * Tier 1: Direct Rootless Write (if node is chmod 666 / system owned)
     * Tier 2: Elevated Root Shell if node is protected.
     */
    private fun writeNodeDualTier(path: String, value: String, shellQueue: MutableList<String>): Boolean {
        val file = File(path)
        if (file.exists() && file.canWrite()) {
            return try {
                file.writeText(value)
                true
            } catch (e: Exception) {
                // If direct write threw security exception, enqueue for Tier 2 Shell
                shellQueue.add("echo '$value' > '$path' 2>/dev/null || true")
                false
            }
        } else {
            // Node exists or may exist under root; enqueue for Tier 2 Shell
            shellQueue.add("echo '$value' > '$path' 2>/dev/null || true")
            return false
        }
    }

    /**
     * Persists settings to /data/adb/extreme/profile.conf and generates
     * /data/adb/service.d/extreme_profile.sh for Magisk/KernelSU/APatch boot execution.
     */
    private fun persistProfileConfig(profile: ExtremeProfile) {
        val confContent = """
            # Extreme++ Kernel Profile Configuration
            PROFILE_ID=${profile.id}
            PRIME_MAX_KHZ=${profile.primeMaxFreqKHz}
            GOLD_MAX_KHZ=${profile.goldMaxFreqKHz}
            LITTLE_MAX_KHZ=${profile.littleMaxFreqKHz}
            PRIME_MIN_KHZ=${profile.primeMinFreqKHz}
            GOLD_MIN_KHZ=${profile.goldMinFreqKHz}
            LITTLE_MIN_KHZ=${profile.littleMinFreqKHz}
            GPU_MAX_PWRLEVEL=${profile.gpuMaxPwrLevel}
            GOV_UP_RATE_US=${profile.govUpRateUs}
            GOV_DOWN_RATE_US=${profile.govDownRateUs}
        """.trimIndent()

        val bootScriptContent = """
            #!/system/bin/sh
            # EXTREME++ Kernel Profile Boot Enforcer
            # Executed by KernelSU / Magisk / APatch service.d
            
            sleep 5
            
            # Policy 0 (Little 0-3)
            echo ${profile.littleMinFreqKHz} > /sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq 2>/dev/null
            echo ${profile.littleMaxFreqKHz} > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
            
            # Policy 4 (Gold 4-6)
            echo ${profile.goldMinFreqKHz} > /sys/devices/system/cpu/cpufreq/policy4/scaling_min_freq 2>/dev/null
            echo ${profile.goldMaxFreqKHz} > /sys/devices/system/cpu/cpufreq/policy4/scaling_max_freq 2>/dev/null
            
            # Policy 7 (Prime 7)
            echo ${profile.primeMinFreqKHz} > /sys/devices/system/cpu/cpufreq/policy7/scaling_min_freq 2>/dev/null
            echo ${profile.primeMaxFreqKHz} > /sys/devices/system/cpu/cpufreq/policy7/scaling_max_freq 2>/dev/null
            
            # Governor Rate limits & custom extreme+ governor
            for p in /sys/devices/system/cpu/cpufreq/policy*; do
                if [ -f "${'$'}p/scaling_available_governors" ] && grep -q "extreme+" "${'$'}p/scaling_available_governors"; then
                    echo "extreme+" > "${'$'}p/scaling_governor" 2>/dev/null
                fi
                for gov in extreme+ schedutil walt; do
                    [ -d "${'$'}p/${'$'}gov" ] && echo ${profile.govUpRateUs} > "${'$'}p/${'$'}gov/up_rate_limit_us" 2>/dev/null
                    [ -d "${'$'}p/${'$'}gov" ] && echo ${profile.govDownRateUs} > "${'$'}p/${'$'}gov/down_rate_limit_us" 2>/dev/null
                done
            done
            
            # GPU
            echo ${profile.gpuMaxPwrLevel} > /sys/class/kgsl/kgsl-3d0/max_pwrlevel 2>/dev/null
            
            exit 0
        """.trimIndent()

        try {
            Shell.cmd(
                "mkdir -p $CONF_DIR",
                "cat << 'EOF' > $CONF_FILE\n$confContent\nEOF",
                "chmod 644 $CONF_FILE",
                "mkdir -p $SERVICE_D_DIR",
                "cat << 'EOF' > $BOOT_SCRIPT\n$bootScriptContent\nEOF",
                "chmod 755 $BOOT_SCRIPT"
            ).exec()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist profile script: ${e.message}")
        }
    }
}
