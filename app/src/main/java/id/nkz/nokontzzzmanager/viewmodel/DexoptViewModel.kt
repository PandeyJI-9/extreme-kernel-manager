package id.nkz.nokontzzzmanager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.nkz.nokontzzzmanager.data.repository.DexoptRepository
import id.nkz.nokontzzzmanager.service.DexoptService
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DexoptViewModel @Inject constructor(
    private val dexoptRepository: DexoptRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isRunning: StateFlow<Boolean> = dexoptRepository.isRunning
    val isFinished: StateFlow<Boolean> = dexoptRepository.isFinished
    
    // Expose recent logs (last 10 lines)
    val recentLogs: StateFlow<List<String>> = dexoptRepository.recentLogs

    fun runDexopt() {
        DexoptService.start(context)
    }

    fun stopDexopt() {
        DexoptService.stop(context)
    }
    
    fun clearLogs() {
        dexoptRepository.clearLogs()
    }
}