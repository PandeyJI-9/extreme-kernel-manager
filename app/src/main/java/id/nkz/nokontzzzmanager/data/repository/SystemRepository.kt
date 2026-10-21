package id.nkz.nokontzzzmanager.data.repository

import android.app.ActivityManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import id.nkz.nokontzzzmanager.data.model.MemoryInfo
import id.nkz.nokontzzzmanager.data.model.RealtimeAggregatedInfo
import id.nkz.nokontzzzmanager.data.model.RealtimeCpuInfo
import id.nkz.nokontzzzmanager.data.model.RealtimeGpuInfo
import id.nkz.nokontzzzmanager.data.model.SystemInfo
import id.nkz.nokontzzzmanager.utils.KernelPaths

import id.nkz.nokontzzzmanager.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose // Diperlukan untuk callbackFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Singleton
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.opengles.GL10


// ponytail: Feature toggles extracted to KernelFeatureRepository (KGSL, Dirty PTE, Bypass,
// Fast Charge, BG Blocker, TCP, GPU Throttling, I/O Scheduler). CPU/Battery/GPU/Memory/Kernel
// monitoring still live here — tightly coupled via getCachedSystemInfo() and realtimeAggregatedInfoFlow.
// Extract monitoring sections when a clean boundary emerges.

@Singleton
class SystemRepository @Inject constructor(
    private val context: Context,
    private val tuningRepository: TuningRepository,
    private val rootRepository: RootRepository,
    private val sysfsHelper: SysfsHelper,
    private val kernelFeatures: KernelFeatureRepository,
    private val cpuMonitor: CpuMonitorProvider,
    private val batteryMonitor: BatteryMonitorProvider,
    private val memoryMonitor: MemoryMonitorProvider,
    private val kernelInfoProvider: KernelInfoProvider,
    private val nativeTelemetryReader: NativeTelemetryReader,
) {

    companion object {
        private const val VALUE_NOT_AVAILABLE = "N/A"
        private const val VALUE_UNKNOWN = "Unknown"
        private const val REALTIME_UPDATE_INTERVAL_MS = 1000L
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val systemInfoMutex = Mutex()
    private var cachedSystemInfo: SystemInfo? = null

    // Wakelock detection memory
    private enum class WakelockSource { MODERN, LEGACY, CLASS_SYS }
    private var lastSuccessfulSource: WakelockSource? = null

    private suspend fun getCachedSystemInfo(): SystemInfo {
        // Menggunakan double-checked locking untuk thread-safety sederhana jika diakses dari coroutine berbeda
        // Meskipun dalam kasus ini, kemungkinan besar akan dipanggil dari scope callbackFlow yang sama.
        return cachedSystemInfo ?: systemInfoMutex.withLock {
            cachedSystemInfo ?: getSystemInfoInternal().also { cachedSystemInfo = it }
        }
    }

    private suspend fun readFileToString(filePath: String, fileDescription: String, attemptSu: Boolean = true, useRetry: Boolean = true): String? {
        return sysfsHelper.readFileToString(filePath, fileDescription, attemptSu, useRetry)
    }

    private suspend fun writeStringToFile(filePath: String, content: String, fileDescription: String, attemptSu: Boolean = true): Boolean {
        return sysfsHelper.writeStringToFile(filePath, content, fileDescription, attemptSu)
    }

    // CPU realtime — delegated to CpuMonitorProvider

    fun getCpuRealtime(): RealtimeCpuInfo = cpuMonitor.getCpuRealtime()

    suspend fun getCpuClusters(): List<CpuCluster> = cpuMonitor.getCpuClusters()

    // Battery + Deep Sleep — delegated to BatteryMonitorProvider
    fun getBatteryInfo(): BatteryInfo = batteryMonitor.getBatteryInfo()
    fun getDeepSleepInfo(): DeepSleepInfo = batteryMonitor.getDeepSleepInfo()
    fun getAwakeTime(): Long = batteryMonitor.getAwakeTime()

    // Memory — delegated to MemoryMonitorProvider
    suspend fun getMemoryInfo(): MemoryInfo = memoryMonitor.getMemoryInfo()

    private suspend fun getGpuModel(): String {
        return try {
            val result = tuningRepository.getOpenGlesDriver().firstOrNull()
            
            if (result != null && result != "N/A" && result.isNotBlank()) {
                // Clean up the result first
                val cleanResult = result.trim()
                
                // The format is: "Qualcomm, Adreno (TM) 650, OpenGL ES 3.2..."
                // So we want the second part after splitting by comma
                val parts = cleanResult.split(",")
                if (parts.size >= 2) {
                    val gpuModel = parts[1].trim()
                    
                    // Clean up the GPU model
                    val cleanGpuModel = gpuModel
                        .replace("(TM)", "")
                        .replace("  ", " ")
                        .trim()
                    
                    return cleanGpuModel
                }
                
                // Fallback: if we can't parse properly, try the old method
                var gpuModel = cleanResult
                val commaIndex = cleanResult.indexOf(',')
                if (commaIndex != -1) {
                    gpuModel = cleanResult.take(commaIndex).trim()
                }
                
                // Remove common prefixes and clean up
                gpuModel = gpuModel
                    .replace("GLES:", "")
                    .replace("OpenGL ES", "")
                    .replace("(TM)", "")
                    .replace("  ", " ")
                    .trim()
                
                
                // If still too generic, fall back to default
                if (gpuModel.equals("Qualcomm", ignoreCase = true) || gpuModel.length < 5) {
                    return "Graphics Processing Unit (GPU)"
                }
                
                return gpuModel
            }
            "Graphics Processing Unit (GPU)"
        } catch (e: Exception) {
            "Graphics Processing Unit (GPU)"
        }
    }

    private var cachedGlVersion: String? = null

    private fun getDetailedGlVersion(): String {
        val cached = cachedGlVersion
        if (cached != null && cached.isNotEmpty()) return cached

        // 1. Try basic ActivityManager first as fallback
        var version = try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.deviceConfigurationInfo.glEsVersion
        } catch (e: Exception) {
            ""
        }

        // 2. Try to get detailed string from EGL (e.g. "OpenGL ES 3.2 V@0530...")
        // We run this in a try-catch block to ensure safety as EGL calls can fail on some devices
        try {
            val egl = EGLContext.getEGL() as EGL10
            val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

            if (display === EGL10.EGL_NO_DISPLAY) return version

            val versionArray = IntArray(2)
            if (!egl.eglInitialize(display, versionArray)) return version

            val configAttribs = intArrayOf(
                EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                EGL10.EGL_NONE
            )

            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfig = IntArray(1)
            egl.eglChooseConfig(display, configAttribs, configs, 1, numConfig)

            if (numConfig[0] == 0) return version
            val config = configs[0]

            val contextAttribs = intArrayOf(
                0x3098, 2, // EGL_CONTEXT_CLIENT_VERSION, 2
                EGL10.EGL_NONE
            )

            val ctx = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, contextAttribs)

            if (ctx === EGL10.EGL_NO_CONTEXT) return version

            val surfAttribs = intArrayOf(
                EGL10.EGL_WIDTH, 1,
                EGL10.EGL_HEIGHT, 1,
                EGL10.EGL_NONE
            )
            val surface = egl.eglCreatePbufferSurface(display, config, surfAttribs)

            if (surface === EGL10.EGL_NO_SURFACE) {
                egl.eglDestroyContext(display, ctx)
                return version
            }

            if (!egl.eglMakeCurrent(display, surface, surface, ctx)) {
                egl.eglDestroySurface(display, surface)
                egl.eglDestroyContext(display, ctx)
                return version
            }

            val gl = ctx.gl as GL10
            val fullVersion = gl.glGetString(GL10.GL_VERSION)

            // Cleanup
            egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
            egl.eglDestroySurface(display, surface)
            egl.eglDestroyContext(display, ctx)
            egl.eglTerminate(display)

            if (!fullVersion.isNullOrBlank()) {
                version = fullVersion
            }
        } catch (e: Exception) {
            // EGL failed, keep using the basic version
        }

        cachedGlVersion = version
        return version
    }

    private suspend fun getGpuRealtimeInternal(): RealtimeGpuInfo {
        val nativeGpu = nativeTelemetryReader.readSnapshot()?.gpu
        var currentFreq = nativeGpu?.currentFreqHz?.takeIf { it > 0L }?.div(1_000_000L)?.toInt()
        var maxFreq = nativeGpu?.maxFreqHz?.takeIf { it > 0L }?.div(1_000_000L)?.toInt()
        var usage = nativeGpu?.usagePercent?.takeIf { it in 0..100 }
        val gpuModel = getGpuModel()
        val glVersion = getDetailedGlVersion() // Use the detailed fetcher
        
        try {
            // Get current GPU frequency from TuningRepository
            if (currentFreq == null) currentFreq = tuningRepository.getCurrentGpuFreq().firstOrNull() ?: 0
            
            // Get max GPU frequency from TuningRepository
            if (maxFreq == null) {
                val (_, max) = tuningRepository.getGpuFreq().firstOrNull() ?: (0 to 0)
                maxFreq = max
            }
            
            // Get GPU usage from TuningRepository
            if (usage == null) usage = tuningRepository.getGpuUsage().firstOrNull() ?: 0
        } catch (e: Exception) {
        }
        
        return RealtimeGpuInfo(
            usagePercentage = (usage ?: 0).toFloat(),
            currentFreq = currentFreq ?: 0,
            maxFreq = maxFreq ?: 0,
            model = gpuModel,
            glVersion = glVersion
        )
    }
    
    fun getGpuRealtime(): RealtimeGpuInfo {
        return runBlocking { getGpuRealtimeInternal() }
    }

    private fun getSystemInfoInternal(): SystemInfo {

        // Improved SoC detection with multiple property sources
        var socName = VALUE_UNKNOWN
        var manufacturer: String? = null
        var model: String? = null
        
        try {
            // Try multiple property sources for SoC detection
            val socProperties = listOf(
                "ro.soc.manufacturer" to "ro.soc.model",
                "ro.hardware" to null,
                "ro.product.board" to null,
                "ro.chipname" to null,
                "ro.board.platform" to null,
                "vendor.product.cpu" to null
            )

            // Try each property pair
            for ((manufacturerProp, modelProp) in socProperties) {
                if (manufacturer.isNullOrBlank()) {
                    manufacturer = getSystemProperty(manufacturerProp)
                }

                if (modelProp != null && model.isNullOrBlank()) {
                    model = getSystemProperty(modelProp)
                }

                // If we have both, break early
                if (!manufacturer.isNullOrBlank() && !model.isNullOrBlank()) {
                    break
                }
            }

            // Additional fallback checks
            if (manufacturer.isNullOrBlank()) {
                manufacturer = getSystemProperty("ro.product.cpu.abi")?.let { abi ->
                    when {
                        abi.contains("arm64") || abi.contains("aarch64") -> "ARM"
                        abi.contains("x86") -> "Intel"
                        else -> null
                    }
                }
            }

            // Parse hardware string for additional info
            val hardware = getSystemProperty("ro.hardware")
            if (!hardware.isNullOrBlank()) {
                when {
                    hardware.startsWith("qcom", ignoreCase = true) -> {
                        if (manufacturer.isNullOrBlank()) manufacturer = "QTI"
                        if (model.isNullOrBlank()) model = hardware.uppercase()
                    }
                    hardware.contains("mtk", ignoreCase = true) || hardware.contains("mediatek", ignoreCase = true) -> {
                        if (manufacturer.isNullOrBlank()) manufacturer = "Mediatek"
                        if (model.isNullOrBlank()) model = hardware
                    }
                    hardware.contains("exynos", ignoreCase = true) -> {
                        if (manufacturer.isNullOrBlank()) manufacturer = "Samsung"
                        if (model.isNullOrBlank()) model = hardware
                    }
                }
            }

            if (!manufacturer.isNullOrBlank() && !model.isNullOrBlank()) {
                socName = when {
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM7475", ignoreCase = true) -> "Qualcomm® Snapdragon™ 7+ Gen 2"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8650", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8 Gen 3"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8635", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8s Gen 3"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM845", ignoreCase = true) || model.equals("sdm845", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 845"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8250", ignoreCase = true) -> "Qualcomm® Snapdragon™ 870"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8150", ignoreCase = true) -> "Qualcomm® Snapdragon™ 860"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SM7435-AB", ignoreCase = true) || model.equals("SM7435", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 7s Gen 2"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SM8735", ignoreCase = true) || model.equals("sm8735", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 8s Gen 4"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM665", ignoreCase = true) || model.equals("sdm665", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 665"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM660", ignoreCase = true) || model.equals("sdm660", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 660"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8750", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8 Elite"
                    manufacturer.equals("Mediatek", ignoreCase = true) && (model.equals("MT6785V/CD", ignoreCase = true) || model.equals("MT6785", ignoreCase = true)) -> "MediaTek Helio G95"
                    manufacturer.equals("Mediatek", ignoreCase = true) && (model.equals("MT6877V/TTZA", ignoreCase = true) || model.equals("MT6877V", ignoreCase = true)) -> "MediaTek Dimensity 1080"
                    manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6833GP", ignoreCase = true) -> "MediaTek Dimensity 6080"
                    manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6769Z", ignoreCase = true) -> "MediaTek Helio G85"
                    manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6989W", ignoreCase = true) -> "MediaTek Dimensity 9300+"
                    else -> "$manufacturer $model"
                }
            } else if (!manufacturer.isNullOrBlank()) {
                socName = manufacturer
            } else if (!model.isNullOrBlank()) {
                socName = model
            }

        } catch (e: Exception) {
        }

        // Get actual display information
        val displayInfo = getDisplayInfo()

        return SystemInfo(
            model = Build.MODEL ?: VALUE_UNKNOWN,
            codename = Build.DEVICE ?: VALUE_UNKNOWN,
            productBoard = getSystemProperty("ro.product.board") ?: VALUE_UNKNOWN,
            androidVersion = Build.VERSION.RELEASE ?: VALUE_UNKNOWN,
            sdk = Build.VERSION.SDK_INT,
            fingerprint = Build.FINGERPRINT ?: VALUE_UNKNOWN,
            soc = socName,
            board = model ?: VALUE_UNKNOWN,
            screenResolution = displayInfo.resolution,
            displayTechnology = displayInfo.technology,
            refreshRate = displayInfo.refreshRate,
            screenDpi = displayInfo.dpi
        )
    }

    private fun getSystemProperty(property: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $property")
            val result = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()
            process.waitFor()
            process.destroy()
            if (result.isNullOrBlank()) null else result
        } catch (e: Exception) {
            null
        }
    }

    private data class DisplayInfo(
        val resolution: String,
        val technology: String,
        val refreshRate: String,
        val dpi: String
    )

    private fun getDisplayInfo(): DisplayInfo {
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            val resolution = "${bounds.width()}x${bounds.height()}"
            val dpi = "${context.resources.configuration.densityDpi}"

            // Get refresh rate - hardcoded to 60-120Hz
            val refreshRate = "60-120Hz"

            // Detect display technology - hardcoded to AMOLED
            val technology = "AMOLED"

            return DisplayInfo(resolution, technology, refreshRate, dpi)

        } catch (e: Exception) {
            return DisplayInfo(VALUE_UNKNOWN, "LCD", "60Hz", VALUE_UNKNOWN)
        }
    }



    fun getSystemInfo(): SystemInfo {
        return runBlocking { getCachedSystemInfo() }
    }

    suspend fun getBootId(): String? {
        return readFileToString("/proc/sys/kernel/random/boot_id", "Boot ID")
    }

    suspend fun getKernelInfo(): KernelInfo = kernelInfoProvider.getKernelInfo()

    // KGSL — delegated to KernelFeatureRepository
    suspend fun getKgslSkipZeroing(): Boolean = kernelFeatures.getKgslSkipZeroing()
    suspend fun setKgslSkipZeroing(enabled: Boolean): Boolean = kernelFeatures.setKgslSkipZeroing(enabled)
    suspend fun isKgslFeatureAvailable(): Boolean = kernelFeatures.isKgslFeatureAvailable()
    fun parseKgslSkipZeroingValue(value: String?): Boolean = kernelFeatures.parseKgslSkipZeroingValue(value)

    // Avoid Dirty PTE — delegated to KernelFeatureRepository
    suspend fun isAvoidDirtyPteAvailable(): Boolean = kernelFeatures.isAvoidDirtyPteAvailable()
    suspend fun getAvoidDirtyPte(): Boolean = kernelFeatures.getAvoidDirtyPte()
    suspend fun setAvoidDirtyPte(enabled: Boolean): Boolean = kernelFeatures.setAvoidDirtyPte(enabled)

    // Bypass Charging — delegated to KernelFeatureRepository
    suspend fun isBypassChargingAvailable(): Boolean = kernelFeatures.isBypassChargingAvailable()
    suspend fun getBypassCharging(): Boolean = kernelFeatures.getBypassCharging()
    suspend fun setBypassCharging(enabled: Boolean): Boolean = kernelFeatures.setBypassCharging(enabled)

    // USB Fast Charge — delegated to KernelFeatureRepository
    suspend fun isForceFastChargeAvailable(): Boolean = kernelFeatures.isForceFastChargeAvailable()
    suspend fun getForceFastCharge(): Boolean = kernelFeatures.getForceFastCharge()
    suspend fun setForceFastCharge(enabled: Boolean): Boolean = kernelFeatures.setForceFastCharge(enabled)

    // Background App Blocker — delegated to KernelFeatureRepository
    suspend fun isBgBlockerAvailable(): Boolean = kernelFeatures.isBgBlockerAvailable()
    suspend fun getBgBlocklist(): String = kernelFeatures.getBgBlocklist()
    suspend fun setBgBlocklist(blocklist: String): Boolean = kernelFeatures.setBgBlocklist(blocklist)

    // TCP Congestion — delegated to KernelFeatureRepository
    suspend fun getTcpCongestionAlgorithm(): String = kernelFeatures.getTcpCongestionAlgorithm()
    suspend fun setTcpCongestionAlgorithm(algorithm: String): Boolean = kernelFeatures.setTcpCongestionAlgorithm(algorithm)
    suspend fun getAvailableTcpCongestionAlgorithmsList(): List<String> = kernelFeatures.getAvailableTcpCongestionAlgorithmsList()

    // GPU Throttling — delegated to KernelFeatureRepository
    suspend fun isGpuThrottlingEnabled(): Boolean = kernelFeatures.isGpuThrottlingEnabled()
    suspend fun setGpuThrottling(enabled: Boolean): Boolean = kernelFeatures.setGpuThrottling(enabled)

    // I/O Scheduler — delegated to KernelFeatureRepository
    suspend fun getIoScheduler(): String = kernelFeatures.getIoScheduler()
    suspend fun setIoScheduler(scheduler: String): Boolean = kernelFeatures.setIoScheduler(scheduler)
    suspend fun getAvailableIoSchedulersList(): List<String> = kernelFeatures.getAvailableIoSchedulersList()

    val realtimeAggregatedInfoFlow: Flow<RealtimeAggregatedInfo> = callbackFlow {

        val lastState = java.util.concurrent.atomic.AtomicReference<RealtimeAggregatedInfo>(null)

        // Initial full data fetch
        launch(Dispatchers.IO) {
            val initialData = RealtimeAggregatedInfo(
                cpuInfo = cpuMonitor.getCpuRealtimeSuspend(),
                gpuInfo = getGpuRealtimeInternal(),
                batteryInfo = batteryMonitor.getBatteryInfoSuspend(),
                memoryInfo = memoryMonitor.getMemoryInfo(),
                uptimeMillis = batteryMonitor.getUptimeMillis(),
                deepSleepMillis = batteryMonitor.getDeepSleepMillis()
            )
            lastState.set(initialData)
            trySend(initialData)
        }

        // Decoupled battery update via BroadcastReceiver
        val batteryReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                    val currentState = lastState.get()
                    if (currentState != null) {
                        launch(Dispatchers.IO) {
                            val newBatteryInfo = batteryMonitor.getBatteryInfoSuspend(status)
                            val newState = currentState.copy(batteryInfo = newBatteryInfo)
                            lastState.set(newState)
                            trySend(newState)
                        }
                    }
                }
            }
        }
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Decoupled polling for other system stats
        val pollingJob = launch(Dispatchers.IO) {
            while (isActive) {
                delay(REALTIME_UPDATE_INTERVAL_MS)
                val currentState = lastState.get()
                if (currentState != null) {
                    // Fetch non-battery stats
                    val newCpuInfo = cpuMonitor.getCpuRealtimeSuspend()
                    val newGpuInfo = getGpuRealtimeInternal()
                    val newMemoryInfo = memoryMonitor.getMemoryInfo()
                    val newUptime = batteryMonitor.getUptimeMillis()
                    val newDeepSleep = batteryMonitor.getDeepSleepMillis()

                    // Create new state by copying the last one and updating polled values
                    val newState = currentState.copy(
                        cpuInfo = newCpuInfo,
                        gpuInfo = newGpuInfo,
                        memoryInfo = newMemoryInfo,
                        uptimeMillis = newUptime,
                        deepSleepMillis = newDeepSleep
                    )
                    lastState.set(newState)
                    trySend(newState)
                }
            }
        }

        awaitClose {
            context.unregisterReceiver(batteryReceiver)
            pollingJob.cancel()
        }
    }.shareIn(
        scope = repositoryScope,
        started = SharingStarted.WhileSubscribed(0),
        replay = 1
    )

    suspend fun getWakelocks(): List<WakelockInfo> {
        // 0. Use cached source if available for instant results
        val rawList = lastSuccessfulSource?.let { source ->
            val data = when (source) {
                WakelockSource.MODERN -> getWakelocksModern()
                WakelockSource.LEGACY -> getWakelocksLegacy()
                WakelockSource.CLASS_SYS -> getWakelocksClassSys()
            }
            if (data.isNotEmpty()) data else {
                lastSuccessfulSource = null
                null
            }
        } ?: run {
            // Discovery phase
            val modern = getWakelocksModern()
            if (modern.isNotEmpty()) {
                lastSuccessfulSource = WakelockSource.MODERN
                modern
            } else {
                val legacy = getWakelocksLegacy()
                if (legacy.isNotEmpty()) {
                    lastSuccessfulSource = WakelockSource.LEGACY
                    legacy
                } else {
                    val classSys = getWakelocksClassSys()
                    if (classSys.isNotEmpty()) {
                        lastSuccessfulSource = WakelockSource.CLASS_SYS
                        classSys
                    } else emptyList()
                }
            }
        }

        if (rawList.isEmpty()) return emptyList()

        // Aggregate duplicates by name
        return rawList.groupBy { it.name }.map { (name, group) ->
            WakelockInfo(
                name = name,
                activeCount = group.sumOf { it.activeCount },
                wakeupCount = group.sumOf { it.wakeupCount },
                totalTimeMs = group.sumOf { it.totalTimeMs },
                maxTimeMs = group.maxOf { it.maxTimeMs },
                preventSuspendTimeMs = group.sumOf { it.preventSuspendTimeMs }
            )
        }.sortedByDescending { it.preventSuspendTimeMs.coerceAtLeast(it.totalTimeMs) }
    }

    private suspend fun getWakelocksModern(): List<WakelockInfo> {
        val wakelocks = mutableListOf<WakelockInfo>()
        try {
            val rawData = readFileToString("/sys/kernel/debug/wakeup_sources", "Modern Wakeup Sources", attemptSu = true, useRetry = false)
            if (!rawData.isNullOrBlank()) {
                rawData.lines().filter { it.isNotBlank() }.drop(1).forEach { line ->
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 10) {
                        val name = parts[0]
                        val activeCount = parts[1].toLongOrNull() ?: 0
                        val wakeupCount = parts[3].toLongOrNull() ?: 0
                        val totalTime = parts[6].toLongOrNull() ?: 0
                        val maxTime = parts[7].toLongOrNull() ?: 0
                        val preventSuspendTime = parts[9].toLongOrNull() ?: 0
                        if (totalTime > 0 || preventSuspendTime > 0 || activeCount > 0) {
                            wakelocks.add(WakelockInfo(name, activeCount, wakeupCount, totalTime, 0, maxTime, 0, preventSuspendTime))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return wakelocks.sortedByDescending { it.preventSuspendTimeMs.coerceAtLeast(it.totalTimeMs) }
    }

    private suspend fun getWakelocksLegacy(): List<WakelockInfo> {
        val wakelocks = mutableListOf<WakelockInfo>()
        try {
            val rawData = readFileToString("/proc/wakelocks", "Legacy Proc Wakelocks", attemptSu = true, useRetry = false)
            if (!rawData.isNullOrBlank()) {
                rawData.lines().filter { it.isNotBlank() }.drop(1).forEach { line ->
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 9) {
                        val name = parts[0]
                        val count = parts[1].toLongOrNull() ?: 0
                        val wakeCount = parts[3].toLongOrNull() ?: 0
                        val totalTime = parts[5].toLongOrNull() ?: 0
                        val maxTime = parts[6].toLongOrNull() ?: 0
                        val preventSuspendTime = parts[8].toLongOrNull() ?: 0
                        if (totalTime > 0 || preventSuspendTime > 0 || count > 0) {
                            wakelocks.add(WakelockInfo(name, count, wakeCount, totalTime / 1000000L, 0, maxTime / 1000000L, 0, preventSuspendTime / 1000000L))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return wakelocks.sortedByDescending { it.preventSuspendTimeMs.coerceAtLeast(it.totalTimeMs) }
    }

    private suspend fun getWakelocksClassSys(): List<WakelockInfo> {
        val wakelockMap = mutableMapOf<String, MutableMap<String, String>>()
        try {
            // High-speed grep: find all properties in one go
            val script = "grep -r . /sys/class/wakeup/*/{name,active_count,wakeup_count,total_time_ms,max_time_ms,prevent_suspend_time_ms} 2>/dev/null"
            val output = rootRepository.run(script, useRetry = false)
            
            output.lines().filter { it.contains(":") }.forEach { line ->
                val pathPart = line.substringBeforeLast(":")
                val value = line.substringAfterLast(":").trim()
                val dirName = pathPart.substringBeforeLast("/").substringAfterLast("/")
                val property = pathPart.substringAfterLast("/")
                
                wakelockMap.getOrPut(dirName) { mutableMapOf() }[property] = value
            }
            
            val wakelocks = wakelockMap.values.mapNotNull { props ->
                val name = props["name"] ?: return@mapNotNull null
                val activeCount = props["active_count"]?.toLongOrNull() ?: 0
                val wakeupCount = props["wakeup_count"]?.toLongOrNull() ?: 0
                val totalTime = props["total_time_ms"]?.toLongOrNull() ?: 0
                val maxTime = props["max_time_ms"]?.toLongOrNull() ?: 0
                val preventSuspendTime = props["prevent_suspend_time_ms"]?.toLongOrNull() ?: 0
                
                if (totalTime > 0 || preventSuspendTime > 0 || activeCount > 0) {
                    WakelockInfo(name, activeCount, wakeupCount, totalTime, 0, maxTime, 0, preventSuspendTime)
                } else null
            }
            return wakelocks.sortedByDescending { it.preventSuspendTimeMs.coerceAtLeast(it.totalTimeMs) }
        } catch (_: Exception) {}
        return emptyList()
    }

    fun onDestroy() {
        repositoryScope.cancel()
    }
}

