package id.nkz.nokontzzzmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.data.model.WakelockInfo
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WakelockViewModel @Inject constructor(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _wakelocks = MutableStateFlow<List<WakelockInfo>>(emptyList())
    val wakelocks: StateFlow<List<WakelockInfo>> = _wakelocks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var isMonitoring = false

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        viewModelScope.launch {
            _isLoading.value = true
            while (isMonitoring) {
                try {
                    val data = systemRepository.getWakelocks()
                    _wakelocks.value = data
                } catch (e: Exception) {
                    // Log error if needed
                }
                _isLoading.value = false
                delay(2000) // Update every 2 seconds
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
