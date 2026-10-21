package id.nkz.nokontzzzmanager.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.data.database.BenchmarkEntity
import id.nkz.nokontzzzmanager.data.database.GameEntity
import id.nkz.nokontzzzmanager.data.repository.BenchmarkRepository
import id.nkz.nokontzzzmanager.data.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FpsMonitorViewModel @Inject constructor(
    private val application: Application,
    private val gameRepository: GameRepository,
    private val benchmarkRepository: BenchmarkRepository,
    private val preferenceManager: id.nkz.nokontzzzmanager.utils.PreferenceManager
) : AndroidViewModel(application) {

    val games: StateFlow<List<GameEntity>> = gameRepository.getAllGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val benchmarks: StateFlow<List<BenchmarkEntity>> = benchmarkRepository.getAllBenchmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var allApps: List<AppInfo> = emptyList()

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _isLayerSearchInfoDismissed = MutableStateFlow(preferenceManager.isFpsLayerSearchInfoDismissed())
    val isLayerSearchInfoDismissed: StateFlow<Boolean> = _isLayerSearchInfoDismissed.asStateFlow()

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = application.packageManager
                val packages = pm.getInstalledPackages(0)
                packages.mapNotNull { packageInfo ->
                    val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null

                    // Exclude system apps that are not updated
                    if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                        return@mapNotNull null
                    }

                    val label = appInfo.loadLabel(pm).toString()

                    AppInfo(
                        packageName = packageInfo.packageName,
                        appName = label
                    )
                }.sortedBy { it.appName.lowercase() }
            }
            allApps = apps
            filterApps(_searchQuery.value)
            _isLoadingApps.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterApps(query)
    }

    private fun filterApps(query: String) {
        if (query.isBlank()) {
            _filteredApps.value = allApps
        } else {
            _filteredApps.value = allApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    fun addGame(appInfo: AppInfo) {
        viewModelScope.launch {
            gameRepository.insertGame(
                GameEntity(
                    packageName = appInfo.packageName,
                    appName = appInfo.appName,
                    isBenchmarkEnabled = true
                )
            )
            // Ensure monitor is enabled when adding a game
            preferenceManager.setAppMonitorEnabled(true)
        }
    }

    fun removeGame(game: GameEntity) {
        viewModelScope.launch {
            gameRepository.deleteGame(game)
        }
    }

    fun toggleBenchmark(game: GameEntity, enabled: Boolean) {
        viewModelScope.launch {
            gameRepository.insertGame(game.copy(isBenchmarkEnabled = enabled))
            
            // Check if any games still have benchmarking enabled
            val allGames = games.value
            val anyEnabled = allGames.any { if (it.packageName == game.packageName) enabled else it.isBenchmarkEnabled }
            
            if (anyEnabled) {
                preferenceManager.setAppMonitorEnabled(true)
            }
        }
    }

    fun deleteBenchmark(benchmark: BenchmarkEntity) {
        viewModelScope.launch {
            benchmarkRepository.deleteBenchmark(benchmark)
        }
    }

    fun dismissLayerSearchInfo() {
        preferenceManager.setFpsLayerSearchInfoDismissed(true)
        _isLayerSearchInfoDismissed.value = true
    }
}
