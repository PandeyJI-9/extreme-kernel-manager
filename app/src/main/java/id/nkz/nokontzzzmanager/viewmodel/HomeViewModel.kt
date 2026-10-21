package id.nkz.nokontzzzmanager.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.nkz.nokontzzzmanager.data.model.MemoryInfo
import id.nkz.nokontzzzmanager.data.model.RealtimeCpuInfo
import id.nkz.nokontzzzmanager.data.model.RealtimeGpuInfo
import id.nkz.nokontzzzmanager.data.model.SystemInfo
import id.nkz.nokontzzzmanager.data.model.*
import id.nkz.nokontzzzmanager.data.repository.RootRepository
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

import id.nkz.nokontzzzmanager.extreme.KernelDetector
import id.nkz.nokontzzzmanager.extreme.ProfileManager
import id.nkz.nokontzzzmanager.extreme.DcDimmingController
import id.nkz.nokontzzzmanager.extreme.ExtremeKernelInfo

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    @field:ApplicationContext private val context: Context,
    private val systemRepo: SystemRepository,
    private val rootRepo: RootRepository,
    val profileManager: ProfileManager,
    val kernelDetector: KernelDetector,
    val dcDimmingController: DcDimmingController
) : ViewModel() {

    val kernelState: StateFlow<ExtremeKernelInfo> = kernelDetector.kernelState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            kernelDetector.detect()
            dcDimmingController.refreshState()
        }
    }

    private val _isScrolling = MutableStateFlow(false)
    private val _cpuGraphMode = MutableStateFlow(GraphMode.LOAD)

    fun setCPUGraphMode(mode: GraphMode) { _cpuGraphMode.value = mode }

    // ponytail: single upstream subscription — WhileSubscribed stops polling when all UI collectors
    // unsubscribe (i.e. Activity backgrounded). Previously init{} launch was a permanent subscriber
    // that kept the 1s polling loop alive forever.
    private val realtimeFlow: StateFlow<RealtimeAggregatedInfo?> = _isScrolling
        .flatMapLatest { isScrolling ->
            if (isScrolling) emptyFlow()
            else systemRepo.realtimeAggregatedInfoFlow
                .catch { e -> Log.e("HomeViewModel", "Error in realtimeAggregatedInfoFlow: ${e.message}", e) }
                .sample(500L)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0),
            initialValue = null
        )

    val cpuInfo: StateFlow<RealtimeCpuInfo> = realtimeFlow
        .map { it?.cpuInfo ?: RealtimeCpuInfo(cores = 0, governor = "N/A", freqs = emptyList(), temp = 0f, soc = "N/A", cpuLoadPercentage = null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            RealtimeCpuInfo(cores = 0, governor = "N/A", freqs = emptyList(), temp = 0f, soc = "N/A", cpuLoadPercentage = null))

    val gpuInfo: StateFlow<RealtimeGpuInfo> = realtimeFlow
        .map { it?.gpuInfo ?: RealtimeGpuInfo(usagePercentage = null, currentFreq = 0, maxFreq = 0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            RealtimeGpuInfo(usagePercentage = null, currentFreq = 0, maxFreq = 0))

    val batteryInfo: StateFlow<BatteryInfo?> = realtimeFlow
        .map { it?.batteryInfo }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val memoryInfo: StateFlow<MemoryInfo?> = realtimeFlow
        .map { it?.memoryInfo }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val deepSleep: StateFlow<DeepSleepInfo?> = realtimeFlow
        .map { it?.let { info -> DeepSleepInfo(uptime = info.uptimeMillis, deepSleep = info.deepSleepMillis) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ponytail: persist accumulator outside scan so history survives flow restarts (WhileSubscribed(0))
    private val _graphData = MutableStateFlow(GraphData())
    val graphData: StateFlow<GraphData> = _graphData.asStateFlow()

    init {
        viewModelScope.launch {
            realtimeFlow.collect { info ->
                if (info == null) return@collect
                val cpuInfo = info.cpuInfo
                val avgSpeed = if (cpuInfo.freqs.isNotEmpty())
                    cpuInfo.freqs.filter { it > 0 }.map { it.toFloat() }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
                else 0f
                val cpuLoad = (cpuInfo.cpuLoadPercentage ?: 0f).coerceIn(0f, 100f)
                val gpuUsage = (info.gpuInfo.usagePercentage ?: 0f).coerceIn(0f, 100f)
                _graphData.update { current ->
                    current.copy(
                        cpuLoadHistory = (current.cpuLoadHistory + cpuLoad).takeLast(50).toImmutableList(),
                        cpuSpeedHistory = (current.cpuSpeedHistory + avgSpeed).takeLast(50).toImmutableList(),
                        gpuHistory = (current.gpuHistory + gpuUsage).takeLast(50).toImmutableList()
                    )
                }
            }
        }
    }

    private val _kernelInfo = MutableStateFlow<KernelInfo?>(null)
    val kernelInfo: StateFlow<KernelInfo?> = _kernelInfo.asStateFlow()

    private val _rootStatus = MutableStateFlow<Boolean?>(null)
    val rootStatus: StateFlow<Boolean?> = _rootStatus.asStateFlow()

    private val _appVersion = MutableStateFlow<String?>("N/A")
    val appVersion: StateFlow<String?> = _appVersion.asStateFlow()

    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo.asStateFlow()

    private val _isTitleAnimationDone = MutableStateFlow(false)
    val isTitleAnimationDone: StateFlow<Boolean> = _isTitleAnimationDone.asStateFlow()

    fun onTitleAnimationFinished() {
        _isTitleAnimationDone.value = true
    }

    private val _cpuClusters = MutableStateFlow<ImmutableList<CpuCluster>?>(null)
    val cpuClusters: StateFlow<ImmutableList<CpuCluster>?> = _cpuClusters.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setScrolling(isScrolling: Boolean) {
        _isScrolling.value = isScrolling
    }

    private val isInitialDataLoaded = java.util.concurrent.atomic.AtomicBoolean(false)

    fun loadInitialData() {
        if (!isInitialDataLoaded.compareAndSet(false, true)) return
        viewModelScope.launch(Dispatchers.IO) {
            val systemInfoDeferred = async { systemRepo.getSystemInfo() }
            val kernelInfoDeferred = async { systemRepo.getKernelInfo() }
            val rootStatusDeferred = async { rootRepo.isRootGrantedQuietly() }
            val cpuClustersDeferred = async { systemRepo.getCpuClusters() }
            val appVersionDeferred = async {
                try {
                    @SuppressLint("PackageManagerGetSignatures")
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error getting app version", e)
                    "N/A"
                }
            }

            _systemInfo.value = systemInfoDeferred.await()
            _kernelInfo.value = kernelInfoDeferred.await()
            _rootStatus.value = rootStatusDeferred.await()
            _cpuClusters.value = cpuClustersDeferred.await().toImmutableList()
            _appVersion.value = appVersionDeferred.await()
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("HomeViewModel", "onCleared called.")
    }
}
