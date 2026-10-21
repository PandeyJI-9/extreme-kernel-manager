package id.nkz.nokontzzzmanager.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.data.database.BenchmarkEntity
import id.nkz.nokontzzzmanager.data.repository.BenchmarkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BenchmarkDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val benchmarkRepository: BenchmarkRepository
) : ViewModel() {

    private val benchmarkId: Long? = savedStateHandle["benchmarkId"]

    val benchmark: StateFlow<BenchmarkEntity?> = if (benchmarkId != null) {
        benchmarkRepository.getBenchmarkById(benchmarkId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(null)
    }


    private val _shareTrigger = MutableSharedFlow<Unit>()
    val shareTrigger: SharedFlow<Unit> = _shareTrigger.asSharedFlow()

    private val _downloadTrigger = MutableSharedFlow<Unit>()
    val downloadTrigger: SharedFlow<Unit> = _downloadTrigger.asSharedFlow()

    fun triggerShare() {
        viewModelScope.launch {
            _shareTrigger.emit(Unit)
        }
    }

    fun triggerDownload() {
        viewModelScope.launch {
            _downloadTrigger.emit(Unit)
        }
    }
}
