package id.nkz.nokontzzzmanager.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import id.nkz.nokontzzzmanager.R
import dagger.hilt.android.AndroidEntryPoint
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.manager.TileUpdateManager
import id.nkz.nokontzzzmanager.service.RootActionService
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BypassChargingTileService : TileService() {

    @Inject
    lateinit var preferenceManager: PreferenceManager
    @Inject
    lateinit var tileUpdateManager: TileUpdateManager

    @Inject
    lateinit var systemRepository: SystemRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var collectorJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
        collectorJob?.cancel()
        collectorJob = serviceScope.launch {
            tileUpdateManager.updateFlow.collectLatest {
                updateTile()
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        collectorJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        collectorJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, RootActionService::class.java).apply {
            action = RootActionService.ACTION_TOGGLE_BYPASS_CHARGING
        }
        startForegroundService(intent)
    }

    fun updateTile() {
        serviceScope.launch {
            val isEnabled = systemRepository.getBypassCharging()
            qsTile.apply {
                state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = getString(R.string.tile_bypass_charging)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    subtitle = if (isEnabled) getString(R.string.status_active) else getString(R.string.status_inactive)
                }
                updateTile()
            }
        }
    }
}