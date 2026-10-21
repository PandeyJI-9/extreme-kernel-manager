package id.nkz.nokontzzzmanager.data.repository

import id.nkz.nokontzzzmanager.utils.KernelPaths
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Self-contained kernel feature toggles extracted from SystemRepository.
 * Handles: KGSL, Dirty PTE, Bypass Charging, USB Fast Charge,
 * BG Blocker, TCP Congestion, GPU Throttling, I/O Scheduler.
 */
@Singleton
class KernelFeatureRepository @Inject constructor(
    private val sysfsHelper: SysfsHelper,
    private val rootRepository: RootRepository,
) {
    // ── KGSL Skip Pool Zeroing ──────────────────────────────────────────

    private suspend fun getAvailableKgslPath(): String? {
        for (path in KernelPaths.KGSL_SKIP_ZEROING) {
            try {
                if (File(path).exists()) return path
            } catch (_: Exception) { }
        }
        for (path in KernelPaths.KGSL_SKIP_ZEROING) {
            try {
                if (sysfsHelper.readFileToString(path, "KGSL Skip Pool Zeroing Check") != null) return path
            } catch (_: Exception) { }
        }
        return null
    }

    suspend fun getKgslSkipZeroing(): Boolean {
        val path = getAvailableKgslPath() ?: return false
        val value = sysfsHelper.readFileToString(path, "KGSL Skip Pool Zeroing")
        return parseKgslSkipZeroingValue(value)
    }

    suspend fun setKgslSkipZeroing(enabled: Boolean): Boolean {
        val path = getAvailableKgslPath() ?: return false
        val value = if (enabled) "1" else "0"
        if (!sysfsHelper.writeStringToFile(path, value, "KGSL Skip Pool Zeroing")) return false
        val actualValue = sysfsHelper.readFileToString(path, "KGSL Verification")
        return parseKgslSkipZeroingValue(actualValue) == enabled
    }

    suspend fun isKgslFeatureAvailable(): Boolean = getAvailableKgslPath() != null

    fun parseKgslSkipZeroingValue(value: String?): Boolean = value?.toIntOrNull() == 1

    // ── Avoid Dirty PTE ─────────────────────────────────────────────────

    private suspend fun getAvailableAvoidDirtyPtePath(): String? {
        for (path in KernelPaths.AVOID_DIRTY_PTE) {
            if (File(path).exists()) return path
        }
        for (path in KernelPaths.AVOID_DIRTY_PTE) {
            if (sysfsHelper.readFileToString(path, "Avoid Dirty PTE Check", false) != null) return path
        }
        return null
    }

    suspend fun isAvoidDirtyPteAvailable(): Boolean = getAvailableAvoidDirtyPtePath() != null

    suspend fun getAvoidDirtyPte(): Boolean {
        val path = getAvailableAvoidDirtyPtePath() ?: return false
        val value = sysfsHelper.readFileToString(path, "Avoid Dirty PTE Status")
        return value?.trim() == "1"
    }

    suspend fun setAvoidDirtyPte(enabled: Boolean): Boolean {
        val path = getAvailableAvoidDirtyPtePath() ?: return false
        val value = if (enabled) "1" else "0"
        try {
            rootRepository.run("chmod 666 $path")
            rootRepository.run("echo -n \"$value\" > \"$path\"")
            rootRepository.run("chmod 444 $path")
            return true
        } catch (_: Exception) {
            return sysfsHelper.writeStringToFile(path, value, "Avoid Dirty PTE")
        }
    }

    // ── Bypass Charging ─────────────────────────────────────────────────

    suspend fun isBypassChargingAvailable(): Boolean {
        if (File(KernelPaths.BYPASS_CHARGING).exists()) return true
        return sysfsHelper.readFileToString(KernelPaths.BYPASS_CHARGING, "Bypass Charging Check") != null
    }

    suspend fun getBypassCharging(): Boolean {
        val value = sysfsHelper.readFileToString(KernelPaths.BYPASS_CHARGING, "Bypass Charging Status")
        return value?.trim() == "1"
    }

    suspend fun setBypassCharging(enabled: Boolean): Boolean {
        val value = if (enabled) "1" else "0"
        if (!sysfsHelper.writeStringToFile(KernelPaths.BYPASS_CHARGING, value, "Bypass Charging")) return false
        val actualValue = sysfsHelper.readFileToString(KernelPaths.BYPASS_CHARGING, "Bypass Charging Verification")
        return actualValue?.trim() == value
    }

    // ── USB Fast Charge ─────────────────────────────────────────────────

    suspend fun isForceFastChargeAvailable(): Boolean {
        if (File(KernelPaths.FORCE_FAST_CHARGE).exists()) return true
        return sysfsHelper.readFileToString(KernelPaths.FORCE_FAST_CHARGE, "USB Fast Charge Check") != null
    }

    suspend fun getForceFastCharge(): Boolean {
        val value = sysfsHelper.readFileToString(KernelPaths.FORCE_FAST_CHARGE, "USB Fast Charge Status")
        return value?.trim() == "1"
    }

    suspend fun setForceFastCharge(enabled: Boolean): Boolean {
        val value = if (enabled) "1" else "0"
        return sysfsHelper.writeStringToFile(KernelPaths.FORCE_FAST_CHARGE, value, "USB Fast Charge")
    }

    // ── Background App Blocker ──────────────────────────────────────────

    private suspend fun getAvailableBgBlocklistPath(): String? {
        for (path in KernelPaths.BG_BLOCKLIST) {
            if (File(path).exists()) return path
        }
        for (path in KernelPaths.BG_BLOCKLIST) {
            if (sysfsHelper.readFileToString(path, "BG Blocker Check", true) != null) return path
        }
        return null
    }

    suspend fun isBgBlockerAvailable(): Boolean = getAvailableBgBlocklistPath() != null

    suspend fun getBgBlocklist(): String {
        val path = getAvailableBgBlocklistPath() ?: return ""
        return sysfsHelper.readFileToString(path, "Background Blocker List") ?: ""
    }

    suspend fun setBgBlocklist(blocklist: String): Boolean {
        val path = getAvailableBgBlocklistPath() ?: return false
        return sysfsHelper.writeStringToFile(path, blocklist, "Background Blocker List")
    }

    // ── TCP Congestion Control ──────────────────────────────────────────

    private suspend fun getCurrentTcpCongestionAlgorithm(): String? {
        return sysfsHelper.readFileToString("/proc/sys/net/ipv4/tcp_congestion_control", "TCP CC Algorithm")
    }

    private suspend fun getAvailableTcpCongestionAlgorithms(): List<String> {
        val available = sysfsHelper.readFileToString("/proc/sys/net/ipv4/tcp_available_congestion_control", "Available TCP CC")
        return available?.split("\\s+".toRegex())?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun getTcpCongestionAlgorithm(): String {
        return getCurrentTcpCongestionAlgorithm() ?: "Unknown"
    }

    suspend fun setTcpCongestionAlgorithm(algorithm: String): Boolean {
        if (!getAvailableTcpCongestionAlgorithms().contains(algorithm)) return false
        return sysfsHelper.writeStringToFile("/proc/sys/net/ipv4/tcp_congestion_control", algorithm, "TCP CC Algorithm")
    }

    suspend fun getAvailableTcpCongestionAlgorithmsList(): List<String> {
        return getAvailableTcpCongestionAlgorithms()
    }

    // ── GPU Throttling ──────────────────────────────────────────────────

    private fun getGpuThrottlingPath(): String? {
        return KernelPaths.GPU_THROTTLING.find { File(it).exists() }
    }

    private suspend fun getGpuThrottlingStatus(): Boolean? {
        val path = getGpuThrottlingPath() ?: return null
        val result = sysfsHelper.readFileToString(path, "GPU Throttling Status")
        return when (result?.trim()) {
            "1", "Y", "yes", "on", "enabled" -> true
            "0", "N", "no", "off", "disabled" -> false
            else -> null
        }
    }

    suspend fun isGpuThrottlingEnabled(): Boolean = getGpuThrottlingStatus() ?: false

    suspend fun setGpuThrottling(enabled: Boolean): Boolean {
        val path = getGpuThrottlingPath() ?: return false
        val value = if (enabled) "1" else "0"
        return sysfsHelper.writeStringToFile(path, value, "GPU Throttling")
    }

    // ── I/O Scheduler ───────────────────────────────────────────────────

    private suspend fun getCurrentIoScheduler(): String {
        val paths = listOf(
            "/sys/block/sda/queue/scheduler",
            "/sys/block/mmcblk0/queue/scheduler",
            "/sys/block/sdb/queue/scheduler",
            "/sys/block/nvme0n1/queue/scheduler",
        )
        for (path in paths) {
            val result = sysfsHelper.readFileToString(path, "I/O Scheduler from $path")
            if (result != null) {
                val activeMatch = Regex("""\[(\w+)]""").find(result)
                return activeMatch?.groupValues?.get(1) ?: "N/A"
            }
        }
        return "N/A"
    }

    private suspend fun getAvailableIoSchedulers(): List<String> {
        val paths = listOf(
            "/sys/block/sda/queue/scheduler",
            "/sys/block/mmcblk0/queue/scheduler",
            "/sys/block/sdb/queue/scheduler",
            "/sys/block/nvme0n1/queue/scheduler",
        )
        for (path in paths) {
            val result = sysfsHelper.readFileToString(path, "Available I/O Schedulers from $path")
            if (result != null) {
                return Regex("""\[(\w+)]|(\w+)""")
                    .findAll(result)
                    .map { it.groupValues[1].ifEmpty { it.groupValues[2] } }
                    .filter { it.isNotEmpty() }
                    .toList()
            }
        }
        return emptyList()
    }

    suspend fun getIoScheduler(): String = getCurrentIoScheduler()

    suspend fun setIoScheduler(scheduler: String): Boolean {
        if (!getAvailableIoSchedulers().contains(scheduler)) return false
        val paths = listOf(
            "/sys/block/sda/queue/scheduler",
            "/sys/block/mmcblk0/queue/scheduler",
            "/sys/block/sdb/queue/scheduler",
            "/sys/block/nvme0n1/queue/scheduler",
        )
        for (path in paths) {
            if (sysfsHelper.readFileToString(path, "Testing I/O Scheduler Path $path") != null) {
                return sysfsHelper.writeStringToFile(path, scheduler, "I/O Scheduler Setting")
            }
        }
        return false
    }

    suspend fun getAvailableIoSchedulersList(): List<String> = getAvailableIoSchedulers()
}
