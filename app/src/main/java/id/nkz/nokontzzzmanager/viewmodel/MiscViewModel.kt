package id.nkz.nokontzzzmanager.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import id.nkz.nokontzzzmanager.service.BatteryMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MiscViewModel @Inject constructor(
    private val application: Application,
    private val preferenceManager: PreferenceManager,
    private val systemRepository: SystemRepository
) : AndroidViewModel(application) {

    companion object {
        const val MAX_BG_BLOCKLIST_APPS = 16
        const val MAX_BG_BLOCKLIST_CHARS = 255
    }

    private val _kgslSkipZeroingEnabled = MutableStateFlow(preferenceManager.getKgslSkipZeroing())
    val kgslSkipZeroingEnabled: StateFlow<Boolean> = _kgslSkipZeroingEnabled.asStateFlow()

    private val _isKgslFeatureAvailable = MutableStateFlow<Boolean?>(null)
    val isKgslFeatureAvailable: StateFlow<Boolean?> = _isKgslFeatureAvailable.asStateFlow()

    private val _avoidDirtyPteEnabled = MutableStateFlow(preferenceManager.getAvoidDirtyPte())
    val avoidDirtyPteEnabled: StateFlow<Boolean> = _avoidDirtyPteEnabled.asStateFlow()

    private val _isAvoidDirtyPteAvailable = MutableStateFlow<Boolean?>(null)
    val isAvoidDirtyPteAvailable: StateFlow<Boolean?> = _isAvoidDirtyPteAvailable.asStateFlow()

    private val _bypassChargingEnabled = MutableStateFlow(preferenceManager.getBypassCharging())
    val bypassChargingEnabled: StateFlow<Boolean> = _bypassChargingEnabled.asStateFlow()

    private val _isBypassChargingAvailable = MutableStateFlow<Boolean?>(null)
    val isBypassChargingAvailable: StateFlow<Boolean?> = _isBypassChargingAvailable.asStateFlow()

    private val _forceFastChargeEnabled = MutableStateFlow(preferenceManager.getForceFastCharge())
    val forceFastChargeEnabled: StateFlow<Boolean> = _forceFastChargeEnabled.asStateFlow()

    private val _isForceFastChargeAvailable = MutableStateFlow<Boolean?>(null)
    val isForceFastChargeAvailable: StateFlow<Boolean?> = _isForceFastChargeAvailable.asStateFlow()

    private val _tcpCongestionAlgorithm = MutableStateFlow<String?>(null)
    val tcpCongestionAlgorithm: StateFlow<String?> = _tcpCongestionAlgorithm.asStateFlow()

    private val _availableTcpCongestionAlgorithms = MutableStateFlow<List<String>>(emptyList())
    val availableTcpCongestionAlgorithms: StateFlow<List<String>> = _availableTcpCongestionAlgorithms.asStateFlow()

    private val _ioScheduler = MutableStateFlow<String?>(null)
    val ioScheduler: StateFlow<String?> = _ioScheduler.asStateFlow()

    private val _availableIoSchedulers = MutableStateFlow<List<String>>(emptyList())
    val availableIoSchedulers: StateFlow<List<String>> = _availableIoSchedulers.asStateFlow()

    private val _applyNetworkStorageOnBoot = MutableStateFlow(preferenceManager.isApplyNetworkStorageOnBoot())
    val applyNetworkStorageOnBoot: StateFlow<Boolean> = _applyNetworkStorageOnBoot.asStateFlow()

    private val _bgBlocklist = MutableStateFlow<String>("")
    val bgBlocklist: StateFlow<String> = _bgBlocklist.asStateFlow()

    private val _isBgBlockerAvailable = MutableStateFlow<Boolean?>(null)
    val isBgBlockerAvailable: StateFlow<Boolean?> = _isBgBlockerAvailable.asStateFlow()

    private val _applyBgBlockerOnBoot = MutableStateFlow(preferenceManager.isApplyBgBlockerOnBoot())
    val applyBgBlockerOnBoot: StateFlow<Boolean> = _applyBgBlockerOnBoot.asStateFlow()

    private val _batteryMonitorEnabled = MutableStateFlow(preferenceManager.isBatteryMonitorEnabled())
    val batteryMonitorEnabled: StateFlow<Boolean> = _batteryMonitorEnabled.asStateFlow()

    private val _autoResetOnReboot = MutableStateFlow(preferenceManager.isAutoResetOnReboot())
    val autoResetOnReboot: StateFlow<Boolean> = _autoResetOnReboot.asStateFlow()

    private val _autoResetOnCharging = MutableStateFlow(preferenceManager.isAutoResetOnCharging())
    val autoResetOnCharging: StateFlow<Boolean> = _autoResetOnCharging.asStateFlow()

    private val _autoResetAtLevel = MutableStateFlow(preferenceManager.isAutoResetAtLevel())
    val autoResetAtLevel: StateFlow<Boolean> = _autoResetAtLevel.asStateFlow()

    private val _autoResetTargetLevel = MutableStateFlow(preferenceManager.getAutoResetTargetLevel())
    val autoResetTargetLevel: StateFlow<Int> = _autoResetTargetLevel.asStateFlow()

    private val _monitorAutoResetOnReboot = MutableStateFlow(preferenceManager.isMonitorAutoResetOnReboot())
    val monitorAutoResetOnReboot: StateFlow<Boolean> = _monitorAutoResetOnReboot.asStateFlow()

    private val _monitorAutoResetOnCharging = MutableStateFlow(preferenceManager.isMonitorAutoResetOnCharging())
    val monitorAutoResetOnCharging: StateFlow<Boolean> = _monitorAutoResetOnCharging.asStateFlow()

    private val _monitorAutoResetAtLevel = MutableStateFlow(preferenceManager.isMonitorAutoResetAtLevel())
    val monitorAutoResetAtLevel: StateFlow<Boolean> = _monitorAutoResetAtLevel.asStateFlow()

    private val _monitorAutoResetTargetLevel = MutableStateFlow(preferenceManager.getMonitorAutoResetTargetLevel())
    val monitorAutoResetTargetLevel: StateFlow<Int> = _monitorAutoResetTargetLevel.asStateFlow()

    private val _chargingControlEnabled = MutableStateFlow(preferenceManager.isChargingControlEnabled())
    val chargingControlEnabled: StateFlow<Boolean> = _chargingControlEnabled.asStateFlow()

    private val _chargingControlStopLevel = MutableStateFlow(preferenceManager.getChargingControlStopLevel())
    val chargingControlStopLevel: StateFlow<Int> = _chargingControlStopLevel.asStateFlow()

    private val _chargingControlResumeLevel = MutableStateFlow(preferenceManager.getChargingControlResumeLevel())
    val chargingControlResumeLevel: StateFlow<Int> = _chargingControlResumeLevel.asStateFlow()

    val batteryInfo = systemRepository.realtimeAggregatedInfoFlow
        .map { it.batteryInfo }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val isDataLoaded = java.util.concurrent.atomic.AtomicBoolean(false)

    init {
        // Data is now loaded lazily by the UI by calling loadInitialData()
    }

    fun loadInitialData() {
        // Always reload data to ensure we have the latest state, especially after restore operations
        // if (isDataLoaded.getAndSet(true)) return

        viewModelScope.launch(Dispatchers.IO) {
            // Check KGSL feature
            val kgslAvailable = systemRepository.isKgslFeatureAvailable()
            _isKgslFeatureAvailable.value = kgslAvailable
            if (kgslAvailable) {
                _kgslSkipZeroingEnabled.value = systemRepository.getKgslSkipZeroing()
            } else {
                _kgslSkipZeroingEnabled.value = false
            }

            // Check Avoid Dirty PTE
            val avoidDirtyPteAvailable = systemRepository.isAvoidDirtyPteAvailable()
            _isAvoidDirtyPteAvailable.value = avoidDirtyPteAvailable
            if (avoidDirtyPteAvailable) {
                val kernelState = systemRepository.getAvoidDirtyPte()
                val userPref = preferenceManager.getAvoidDirtyPte()
                
                if (userPref && !kernelState) {
                    // Self-heal: User wants enabled, but kernel is disabled (e.g. boot restore pending/failed)
                    // Apply immediately
                    val success = systemRepository.setAvoidDirtyPte(true)
                    _avoidDirtyPteEnabled.value = success
                } else {
                    _avoidDirtyPteEnabled.value = kernelState
                }
            } else {
                _avoidDirtyPteEnabled.value = false
            }

            // Check bypass charging
            val bypassAvailable = systemRepository.isBypassChargingAvailable()
            _isBypassChargingAvailable.value = bypassAvailable
            if (bypassAvailable) {
                _bypassChargingEnabled.value = systemRepository.getBypassCharging()
            } else {
                _bypassChargingEnabled.value = false
            }

            // Check USB Fast Charge
            val fastChargeAvailable = systemRepository.isForceFastChargeAvailable()
            _isForceFastChargeAvailable.value = fastChargeAvailable
            if (fastChargeAvailable) {
                _forceFastChargeEnabled.value = systemRepository.getForceFastCharge()
            } else {
                _forceFastChargeEnabled.value = false
            }

            // Load TCP congestion algorithm
            loadTcpCongestionAlgorithm()

            // Load I/O scheduler
            loadIoScheduler()

            // Check Background Blocker
            val bgBlockerAvailable = systemRepository.isBgBlockerAvailable()
            if (bgBlockerAvailable) {
                val kernelBlocklist = systemRepository.getBgBlocklist()
                val prefBlocklist = preferenceManager.getBgBlocklist()
                
                // Priority:
                // 1. If kernel has data, use it.
                // 2. If kernel is empty but prefs has data (even empty string ""), use prefs and sync to kernel.
                // 3. If both are empty/null, use defaults.
                
                val finalBlocklist = when {
                    kernelBlocklist.isNotBlank() -> kernelBlocklist
                    prefBlocklist != null -> {
                        // Pref exists (even if empty), sync it to kernel
                        systemRepository.setBgBlocklist(prefBlocklist)
                        prefBlocklist
                    }
                    else -> {
                        // First time or reset: use defaults
                        val defaultList = "com.shopee.id,com.lazada.android,com.tokopedia.tkpd"
                        systemRepository.setBgBlocklist(defaultList)
                        defaultList
                    }
                }
                
                _bgBlocklist.value = finalBlocklist
                preferenceManager.setBgBlocklist(finalBlocklist)
                _isBgBlockerAvailable.value = true
            } else {
                _isBgBlockerAvailable.value = false
            }

            // Fix for restored backup state where permission is missing
            if (_batteryMonitorEnabled.value && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    application,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (!granted) {
                    // Silently disable it so user sees "Off" and must click to trigger permission prompt
                    toggleBatteryMonitor(false)
                }
            }

            // Sync bypass charging state with real-time kernel status (e.g. if changed by Charging Control service)
            launch {
                batteryInfo.collect { info ->
                    if (info != null) {
                        _bypassChargingEnabled.value = info.isBypassActive
                    }
                }
            }
        }
    }

    fun toggleKgslSkipZeroing(enabled: Boolean) {
        viewModelScope.launch {
            // Try to set the value in the kernel
            val success = systemRepository.setKgslSkipZeroing(enabled)
            
            if (success) {
                // Update state and save preference
                _kgslSkipZeroingEnabled.value = enabled
                preferenceManager.setKgslSkipZeroing(enabled)
            } else {
                // If failed, revert the state to the actual value
                _kgslSkipZeroingEnabled.value = systemRepository.getKgslSkipZeroing()
                preferenceManager.setKgslSkipZeroing(_kgslSkipZeroingEnabled.value)
            }
        }
    }

    fun toggleAvoidDirtyPte(enabled: Boolean) {
        viewModelScope.launch {
            val success = systemRepository.setAvoidDirtyPte(enabled)
            if (success) {
                _avoidDirtyPteEnabled.value = enabled
                preferenceManager.setAvoidDirtyPte(enabled)
            } else {
                _avoidDirtyPteEnabled.value = systemRepository.getAvoidDirtyPte()
                preferenceManager.setAvoidDirtyPte(_avoidDirtyPteEnabled.value)
            }
        }
    }

    fun toggleBypassCharging(enabled: Boolean) {
        viewModelScope.launch {
            // Disable automation if manual bypass is being enabled
            if (enabled && _chargingControlEnabled.value) {
                setChargingControlEnabled(false)
            }

            val success = systemRepository.setBypassCharging(enabled)
            if (success) {
                _bypassChargingEnabled.value = enabled
                preferenceManager.setBypassCharging(enabled)
            } else {
                _bypassChargingEnabled.value = systemRepository.getBypassCharging()
                preferenceManager.setBypassCharging(_bypassChargingEnabled.value)
            }
        }
    }

    fun toggleForceFastCharge(enabled: Boolean) {
        viewModelScope.launch {
            val success = systemRepository.setForceFastCharge(enabled)
            if (success) {
                _forceFastChargeEnabled.value = enabled
                preferenceManager.setForceFastCharge(enabled)
            } else {
                _forceFastChargeEnabled.value = systemRepository.getForceFastCharge()
                preferenceManager.setForceFastCharge(_forceFastChargeEnabled.value)
            }
        }
    }

    private suspend fun loadTcpCongestionAlgorithm() {
        _tcpCongestionAlgorithm.value = systemRepository.getTcpCongestionAlgorithm()
        _availableTcpCongestionAlgorithms.value = systemRepository.getAvailableTcpCongestionAlgorithmsList()
    }

    private suspend fun loadIoScheduler() {
        _ioScheduler.value = systemRepository.getIoScheduler()
        _availableIoSchedulers.value = systemRepository.getAvailableIoSchedulersList()
    }

    fun updateTcpCongestionAlgorithm(algorithm: String) {
        viewModelScope.launch {
            val success = systemRepository.setTcpCongestionAlgorithm(algorithm)
            if (success) {
                // Update the current algorithm in state
                _tcpCongestionAlgorithm.value = algorithm
                preferenceManager.setTcpCongestionAlgorithm(algorithm)
            } else {
                // If failed, reload the actual current value
                _tcpCongestionAlgorithm.value = systemRepository.getTcpCongestionAlgorithm()
            }
        }
    }

    fun updateIoScheduler(scheduler: String) {
        viewModelScope.launch {
            val success = systemRepository.setIoScheduler(scheduler)
            if (success) {
                // Update the current scheduler in state
                _ioScheduler.value = scheduler
                preferenceManager.setIoScheduler(scheduler)
            } else {
                // If failed, reload the actual current value
                _ioScheduler.value = systemRepository.getIoScheduler()
            }
        }
    }

    fun toggleBatteryMonitor(enabled: Boolean) {
        viewModelScope.launch {
            _batteryMonitorEnabled.value = enabled
            preferenceManager.setBatteryMonitorEnabled(enabled)
            val ctx = getApplication<Application>()
            if (enabled) {
                BatteryMonitorService.start(ctx)
            } else {
                BatteryMonitorService.stop(ctx)
            }
        }
    }

    fun resetBatteryMonitor() {
        val ctx = getApplication<Application>()
        BatteryMonitorService.reset(ctx)
    }

    fun setAutoResetOnReboot(enabled: Boolean) {
        _autoResetOnReboot.value = enabled
        preferenceManager.setAutoResetOnReboot(enabled)
    }

    fun setAutoResetOnCharging(enabled: Boolean) {
        _autoResetOnCharging.value = enabled
        preferenceManager.setAutoResetOnCharging(enabled)
    }

    fun setAutoResetAtLevel(enabled: Boolean) {
        _autoResetAtLevel.value = enabled
        preferenceManager.setAutoResetAtLevel(enabled)
    }

    fun setAutoResetTargetLevel(level: Int) {
        _autoResetTargetLevel.value = level
        preferenceManager.setAutoResetTargetLevel(level)
    }

    fun setMonitorAutoResetOnReboot(enabled: Boolean) {
        _monitorAutoResetOnReboot.value = enabled
        preferenceManager.setMonitorAutoResetOnReboot(enabled)
    }

    fun setMonitorAutoResetOnCharging(enabled: Boolean) {
        _monitorAutoResetOnCharging.value = enabled
        preferenceManager.setMonitorAutoResetOnCharging(enabled)
    }

    fun setMonitorAutoResetAtLevel(enabled: Boolean) {
        _monitorAutoResetAtLevel.value = enabled
        preferenceManager.setMonitorAutoResetAtLevel(enabled)
    }

    fun setMonitorAutoResetTargetLevel(level: Int) {
        _monitorAutoResetTargetLevel.value = level
        preferenceManager.setMonitorAutoResetTargetLevel(level)
    }

    fun setChargingControlEnabled(enabled: Boolean) {
        _chargingControlEnabled.value = enabled
        preferenceManager.setChargingControlEnabled(enabled)
        
        if (enabled) {
            // If enabling automation, turn off manual bypass (preference) to give control to the service
            if (_bypassChargingEnabled.value) {
                toggleBypassCharging(false)
            }
        } else {
            // If disabling automation, ensure we don't leave the device in bypass mode (not charging)
            // Use current kernel state check or just force disable if flag is set
            if (_bypassChargingEnabled.value) {
                toggleBypassCharging(false)
            }
        }
    }

    fun setChargingControlStopLevel(level: Int) {
        _chargingControlStopLevel.value = level
        preferenceManager.setChargingControlStopLevel(level)
    }

    fun setChargingControlResumeLevel(level: Int) {
        _chargingControlResumeLevel.value = level
        preferenceManager.setChargingControlResumeLevel(level)
    }

    fun setApplyNetworkStorageOnBoot(enabled: Boolean) {
        _applyNetworkStorageOnBoot.value = enabled
        preferenceManager.setApplyNetworkStorageOnBoot(enabled)
    }

    fun updateBgBlocklist(blocklist: String) {
        val apps = if (blocklist.isBlank()) emptyList() else blocklist.split(",")
        
        // Kernel limits: max 16 apps and 255 characters
        if (apps.size > MAX_BG_BLOCKLIST_APPS) {
            Toast.makeText(application, application.getString(R.string.bg_blocker_max_apps), Toast.LENGTH_SHORT).show()
            return
        }
        if (blocklist.length > MAX_BG_BLOCKLIST_CHARS) {
            Toast.makeText(application, application.getString(R.string.bg_blocker_max_chars), Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val success = systemRepository.setBgBlocklist(blocklist)
            if (success) {
                _bgBlocklist.value = blocklist
                preferenceManager.setBgBlocklist(blocklist)
            } else {
                // If failed, reload actual value from kernel
                _bgBlocklist.value = systemRepository.getBgBlocklist()
            }
        }
    }

    fun setApplyBgBlockerOnBoot(enabled: Boolean) {
        _applyBgBlockerOnBoot.value = enabled
        preferenceManager.setApplyBgBlockerOnBoot(enabled)
    }
}