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
import javax.inject.Inject
import javax.inject.Singleton

private val Context.extremeProfileStore: DataStore<Preferences> by preferencesDataStore(name = "extreme_profiles")

enum class ExtremeProfile(
    val code: Int,
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val primeMaxFreqKHz: Int,
    val goldMaxFreqKHz: Int,
    val littleMaxFreqKHz: Int,
    val primeMinFreqKHz: Int,
    val goldMinFreqKHz: Int,
    val littleMinFreqKHz: Int,
    val gpuClockHz: Long,
    val gpuMaxPwrLevel: Int, // 0 = 670MHz, 1 = 587MHz, 2 = 510MHz
    val schedtuneBoost: Int,
    val schedUpmigrate: String,
    val govUpRateUs: Int,
    val govDownRateUs: Int
) {
    GAMING(
        code = 0,
        id = "gaming",
        title = "Gaming (BMW Monster Peak)",
        subtitle = "3.19 GHz Prime Core • 670MHz GPU • 120 FPS Unlocked",
        description = "BMW Monster Peak: 3.19 GHz Prime Core, 670MHz GPU, 120 FPS Unlocked (Default Boot State)",
        primeMaxFreqKHz = 3187200,
        goldMaxFreqKHz = 2419200,
        littleMaxFreqKHz = 1804800,
        primeMinFreqKHz = 844800,
        goldMinFreqKHz = 710400,
        littleMinFreqKHz = 576000,
        gpuClockHz = 670000000L,
        gpuMaxPwrLevel = 0,
        schedtuneBoost = 25,
        schedUpmigrate = "60 75",
        govUpRateUs = 250,
        govDownRateUs = 40000
    ),
    BALANCED(
        code = 1,
        id = "balanced",
        title = "Balanced (Sustained)",
        subtitle = "2.84 GHz Prime Core • 587MHz GPU • 90 FPS Zero Throttling",
        description = "2.84 GHz Prime Core, 587MHz GPU, 90 FPS Sustained Zero Throttling",
        primeMaxFreqKHz = 2841600,
        goldMaxFreqKHz = 2419200,
        littleMaxFreqKHz = 1804800,
        primeMinFreqKHz = 844800,
        goldMinFreqKHz = 710400,
        littleMinFreqKHz = 576000,
        gpuClockHz = 587000000L,
        gpuMaxPwrLevel = 1,
        schedtuneBoost = 15,
        schedUpmigrate = "65 80",
        govUpRateUs = 500,
        govDownRateUs = 20000
    ),
    BATTERY(
        code = 2,
        id = "battery",
        title = "Battery Saver",
        subtitle = "2.42 GHz Prime Core • 510MHz GPU • Maximum Battery Backup",
        description = "2.42 GHz Prime Core, 510MHz GPU, Maximum Battery Backup",
        primeMaxFreqKHz = 2419200,
        goldMaxFreqKHz = 2246400,
        littleMaxFreqKHz = 1804800,
        primeMinFreqKHz = 844800,
        goldMinFreqKHz = 710400,
        littleMinFreqKHz = 576000,
        gpuClockHz = 510000000L,
        gpuMaxPwrLevel = 2,
        schedtuneBoost = 0,
        schedUpmigrate = "75 90",
        govUpRateUs = 1000,
        govDownRateUs = 10000
    );

    companion object {
        fun fromCode(code: Int): ExtremeProfile = when (code) {
            0 -> GAMING
            1 -> BALANCED
            2 -> BATTERY
            else -> GAMING
        }

        fun fromId(id: String?): ExtremeProfile = when (id?.lowercase()) {
            "gaming", "apex_turbo", "0" -> GAMING
            "balanced", "balanced_cruise", "1" -> BALANCED
            "battery", "battery_saver", "2" -> BATTERY
            else -> GAMING
        }
    }
}

@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kernelDetector: KernelDetector
) {
    private val TAG = "ExtremeProfileManager"

    companion object {
        private val KEY_ACTIVE_PROFILE = stringPreferencesKey("active_extreme_profile")

        const val SYSFS_EXTREME_PROFILE = "/sys/kernel/extreme_profile"
        const val GPU_MAX_CLOCK = "/sys/class/kgsl/kgsl-3d0/gpu_max_clock"
        const val SCHEDTUNE_BOOST = "/dev/stune/top-app/schedtune.boost"
        const val SCHED_UPMIGRATE = "/proc/sys/kernel/sched_upmigrate"

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
        val profileId = preferences[KEY_ACTIVE_PROFILE] ?: ExtremeProfile.GAMING.id
        ExtremeProfile.fromId(profileId)
    }

    /**
     * Synchronizes active profile state with the kernel dynamic node /sys/kernel/extreme_profile.
     * By default on boot / first run, highlights Gaming Profile (0).
     */
    suspend fun syncWithKernel(): ExtremeProfile = withContext(Dispatchers.IO) {
        val kernelCode = readKernelProfileNode()
        val profile = if (kernelCode != null) {
            ExtremeProfile.fromCode(kernelCode)
        } else {
            // Default boot state: Gaming (0)
            ExtremeProfile.GAMING
        }

        context.extremeProfileStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE] = profile.id
        }
        Log.i(TAG, "Synchronized active profile: ${profile.title} (code=${profile.code})")
        profile
    }

    /**
     * Reads active mode from /sys/kernel/extreme_profile.
     * Non-intrusive: rootlessly first, and uses su only if root is already granted.
     */
    fun readKernelProfileNode(): Int? {
        val file = File(SYSFS_EXTREME_PROFILE)
        if (file.exists() && file.canRead()) {
            try {
                return file.readText().trim().toIntOrNull()
            } catch (_: Exception) {}
        }

        if (Shell.isAppGrantedRoot() == true) {
            try {
                val res = Shell.cmd("cat $SYSFS_EXTREME_PROFILE 2>/dev/null").exec()
                if (res.isSuccess && res.out.isNotEmpty()) {
                    return res.out.first().trim().toIntOrNull()
                }
            } catch (_: Exception) {}
        }
        return null
    }

    suspend fun applyProfile(profile: ExtremeProfile): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Applying Extreme Profile (BMW Edition): ${profile.title} [code=${profile.code}]")

        val commands = mutableListOf<String>()

        // 1. Core BMW Edition Profile Engine Switch Command:
        // su -c "echo <code > /sys/kernel/extreme_profile && echo <gpu_clock> > /sys/class/kgsl/kgsl-3d0/gpu_max_clock && echo <schedtune> > /dev/stune/top-app/schedtune.boost && echo '<upmigrate>' > /proc/sys/kernel/sched_upmigrate"
        val bmwCommand = "echo ${profile.code} > $SYSFS_EXTREME_PROFILE && " +
                "echo ${profile.gpuClockHz} > $GPU_MAX_CLOCK && " +
                "echo ${profile.schedtuneBoost} > $SCHEDTUNE_BOOST && " +
                "echo '${profile.schedUpmigrate}' > $SCHED_UPMIGRATE"

        commands.add(bmwCommand)

        // 2. Little Cluster (Policy 0: CPU 0-3)
        applyClusterLimits(POLICY0_DIR, profile.littleMinFreqKHz, profile.littleMaxFreqKHz, profile, commands)

        // 3. Gold Cluster (Policy 4: CPU 4-6)
        applyClusterLimits(POLICY4_DIR, profile.goldMinFreqKHz, profile.goldMaxFreqKHz, profile, commands)

        // 4. Prime Cluster (Policy 7: CPU 7)
        applyClusterLimits(POLICY7_DIR, profile.primeMinFreqKHz, profile.primeMaxFreqKHz, profile, commands)

        // 5. GPU Tuning
        applyGpuLimits(profile, commands)

        // 6. Memory Bus & L3 Cache Locks for Gaming Peak
        if (profile == ExtremeProfile.GAMING) {
            writeNodeDualTier(LLCC_BW_MIN_FREQ, "10000000", commands)
        }

        // Execute via Root Shell (libsu)
        if (commands.isNotEmpty()) {
            val res = Shell.cmd(*commands.toTypedArray()).exec()
            if (!res.isSuccess) {
                Log.w(TAG, "Shell execution had warnings: ${res.err.joinToString("\n")}")
            }
        }

        // 7. Persist to DataStore
        context.extremeProfileStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE] = profile.id
        }

        // 8. Write to Root Boot Hook & Persistence files
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
                shellQueue.add("echo '$value' > '$path' 2>/dev/null || true")
                false
            }
        } else {
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
            # Extreme++ Kernel Profile Configuration (BMW Edition)
            PROFILE_CODE=${profile.code}
            PROFILE_ID=${profile.id}
            GPU_CLOCK_HZ=${profile.gpuClockHz}
            SCHEDTUNE_BOOST=${profile.schedtuneBoost}
            SCHED_UPMIGRATE="${profile.schedUpmigrate}"
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
            # EXTREME++ Gaming (BMW Edition) Profile Boot Enforcer
            # Executed by KernelSU / Magisk / APatch service.d
            
            sleep 5
            
            # BMW Edition Dynamic Node
            echo ${profile.code} > /sys/kernel/extreme_profile 2>/dev/null
            echo ${profile.gpuClockHz} > /sys/class/kgsl/kgsl-3d0/gpu_max_clock 2>/dev/null
            echo ${profile.schedtuneBoost} > /dev/stune/top-app/schedtune.boost 2>/dev/null
            echo '${profile.schedUpmigrate}' > /proc/sys/kernel/sched_upmigrate 2>/dev/null
            
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
