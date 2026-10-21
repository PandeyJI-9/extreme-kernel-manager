package id.nkz.nokontzzzmanager.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DexoptRepository @Inject constructor() {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    // Store last 50 logs
    private val _recentLogs = MutableStateFlow<List<String>>(listOf("Waiting for logs..."))
    val recentLogs: StateFlow<List<String>> = _recentLogs.asStateFlow()

    private val _isCanceled = MutableStateFlow(false)
    val isCanceled: StateFlow<Boolean> = _isCanceled.asStateFlow()

    // Keep compatibility if needed, but unused
    val logs: StateFlow<List<String>> = _recentLogs 

    fun setRunning(running: Boolean) {
        _isRunning.value = running
        if (running) {
            _isFinished.value = false
            _isCanceled.value = false
            _recentLogs.value = listOf("Starting...")
        }
    }

    fun setCanceled(canceled: Boolean) {
        _isCanceled.value = canceled
        if (canceled) {
            _isRunning.value = false
        }
    }

    fun setFinished(finished: Boolean) {
        _isFinished.value = finished
        if (finished) {
            _isRunning.value = false
        }
    }

    fun updateLastLog(log: String) {
        if (log.isNotBlank()) {
            val currentList = _recentLogs.value.toMutableList()
            
            // Optimization: If latest log is exactly the same as previous, don't duplicate
            if (currentList.isNotEmpty() && currentList.last() == log) return

            currentList.add(log)
            // Keep only last 50 lines for better visibility
            if (currentList.size > 50) {
                _recentLogs.value = currentList.takeLast(50)
            } else {
                _recentLogs.value = currentList
            }
        }
    }

    fun clearLogs() {
        if (!_isRunning.value) {
            _recentLogs.value = listOf("Ready")
            _isFinished.value = false
        }
    }
}