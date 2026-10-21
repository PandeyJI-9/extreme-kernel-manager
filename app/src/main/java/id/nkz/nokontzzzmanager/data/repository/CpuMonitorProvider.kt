package id.nkz.nokontzzzmanager.data.repository

import id.nkz.nokontzzzmanager.data.model.CpuCluster
import id.nkz.nokontzzzmanager.data.model.RealtimeCpuInfo
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CpuMonitorProvider @Inject constructor(
    private val sysfsHelper: SysfsHelper,
    private val nativeTelemetryReader: NativeTelemetryReader,
) {
    companion object {
        private const val VALUE_NOT_AVAILABLE = "N/A"
    }

    private var previousCpuData: List<LongArray>? = null

    /** SoC model name, updated by SystemRepository when SystemInfo is loaded. */
    @Volatile
    var socModel: String = "Unknown"

    private suspend fun getCpuRealtimeInternal(): RealtimeCpuInfo {
        val nativeSnapshot = nativeTelemetryReader.readSnapshot()
        val nativeCpu = nativeSnapshot?.cpu.orEmpty()
        val nativeThermal = nativeSnapshot?.thermal.orEmpty()
        if (nativeCpu.isNotEmpty()) {
            val cores = Runtime.getRuntime().availableProcessors()
            val frequencies = List(cores) { coreIndex ->
                nativeCpu.firstOrNull { it.core == coreIndex }
                    ?.currentFreqKhz
                    ?.takeIf { it > 0L }
                    ?.div(1000L)
                    ?.toInt()
                    ?: readCpuFrequency(coreIndex)
            }
            val governor = nativeCpu.firstNotNullOfOrNull { it.governor?.takeIf(String::isNotBlank) }
                ?: readCpuGovernor()
            val temperature = nativeThermal.firstOrNull { zone ->
                zone.type?.contains("cpu", ignoreCase = true) == true
            }?.tempMilliCelsius?.div(1000f)
                ?: nativeThermal.firstOrNull()?.tempMilliCelsius?.div(1000f)
                ?: readCpuTemperature()

            return RealtimeCpuInfo(
                cores = cores,
                governor = governor,
                freqs = frequencies,
                temp = temperature,
                soc = socModel,
                cpuLoadPercentage = calculateCpuLoadPercentage()
            )
        }

        val cores = Runtime.getRuntime().availableProcessors()
        val governor = readCpuGovernor()

        val frequencies = List(cores) { coreIndex ->
            val onlineStr = sysfsHelper.readFileToString(
                "/sys/devices/system/cpu/cpu$coreIndex/online", "CPU$coreIndex Online", attemptSu = false
            )
            val isOnline = onlineStr == null || onlineStr.trim() != "0"
            if (isOnline) {
                readCpuFrequency(coreIndex)
            } else 0
        }

        val temperature = readCpuTemperature()

        val cpuLoadPercentage = calculateCpuLoadPercentage()

        return RealtimeCpuInfo(
            cores = cores,
            governor = governor,
            freqs = frequencies,
            temp = temperature,
            soc = socModel,
            cpuLoadPercentage = cpuLoadPercentage
        )
    }

    private suspend fun readCpuGovernor(): String = sysfsHelper.readFileToString(
        "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "CPU0 Governor"
    ) ?: VALUE_NOT_AVAILABLE

    private suspend fun readCpuFrequency(coreIndex: Int): Int {
        val freqStr = sysfsHelper.readFileToString(
            "/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq", "CPU$coreIndex Freq"
        )
        return (freqStr?.toLongOrNull()?.div(1000))?.toInt() ?: 0
    }

    private suspend fun readCpuTemperature(): Float {
        val tempStr = sysfsHelper.readFileToString("/sys/class/thermal/thermal_zone0/temp", "Thermal Zone0 Temp")
        return (tempStr?.toFloatOrNull()?.div(1000)) ?: 0f
    }

    private suspend fun calculateCpuLoadPercentage(): Float? {
        try {
            val cpuStat = sysfsHelper.readFileToString("/proc/stat", "CPU Stat")
                ?.lines()?.firstOrNull { it.startsWith("cpu ") } ?: return null

            val cpuData = cpuStat.trim().split("\\s+".toRegex()).drop(1).map { it.toLong() }.toLongArray()
            if (cpuData.size < 4) return null

            val previousData = previousCpuData
            if (previousData == null) {
                previousCpuData = listOf(cpuData)
                return null
            }

            val prevData = previousData.first()
            val diffData = LongArray(cpuData.size) { i -> cpuData[i] - prevData[i] }
            val total = diffData.sum()
            val idle = diffData[3] + (if (diffData.size > 4) diffData[4] else 0)
            val usage = if (total > 0) ((total - idle).toDouble() / total.toDouble() * 100.0) else 0.0

            previousCpuData = listOf(cpuData)
            return usage.toFloat().coerceIn(0f, 100f)
        } catch (_: Exception) {
            return null
        }
    }

    fun getCpuRealtime(): RealtimeCpuInfo = runBlocking { getCpuRealtimeInternal() }

    suspend fun getCpuRealtimeSuspend(): RealtimeCpuInfo = getCpuRealtimeInternal()

    // ── CPU Clusters ──────────────────────────────────────────────────────

    suspend fun getCpuClusters(): List<CpuCluster> {
        val clusters = mutableListOf<CpuCluster>()
        val cores = Runtime.getRuntime().availableProcessors()
        val cpuBase = "/sys/devices/system/cpu"

        val clusterOffsets = when {
            cores >= 8 -> listOf(0 to "Little Cluster", 4 to "Big Cluster", 7 to "Prime Cluster")
            cores >= 4 -> listOf(0 to "Little Cluster", 4 to "Big Cluster")
            else -> listOf(0 to "Little Cluster")
        }

        for ((offset, name) in clusterOffsets) {
            if (offset >= cores) continue
            try {
                val availGovs = sysfsHelper.readFileToString(
                    "$cpuBase/cpu$offset/cpufreq/scaling_available_governors", "CPU$offset Govs"
                )?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()

                val availFreqs = sysfsHelper.readFileToString(
                    "$cpuBase/cpu$offset/cpufreq/scaling_available_frequencies", "CPU$offset Freqs"
                )?.split(" ")?.filter { it.isNotBlank() }?.mapNotNull { it.toIntOrNull() } ?: emptyList()

                val currentGov = sysfsHelper.readFileToString(
                    "$cpuBase/cpu$offset/cpufreq/scaling_governor", "CPU$offset Current Gov"
                ) ?: "unknown"

                val minFreq = (sysfsHelper.readFileToString(
                    "$cpuBase/cpu$offset/cpufreq/scaling_min_freq", "CPU$offset Min Freq"
                )?.toLongOrNull()?.div(1000))?.toInt() ?: 0

                val maxFreq = (sysfsHelper.readFileToString(
                    "$cpuBase/cpu$offset/cpufreq/scaling_max_freq", "CPU$offset Max Freq"
                )?.toLongOrNull()?.div(1000))?.toInt() ?: 0

                clusters.add(
                    CpuCluster(
                        name = name,
                        availableGovernors = availGovs,
                        governor = currentGov,
                        minFreq = minFreq,
                        maxFreq = maxFreq
                    )
                )
            } catch (_: Exception) { }
        }
        return clusters
    }
}
