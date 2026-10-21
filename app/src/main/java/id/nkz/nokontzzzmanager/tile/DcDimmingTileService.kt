package id.nkz.nokontzzzmanager.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.extreme.DcDimmingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DcDimmingTileService : TileService() {

    @Inject
    lateinit var dcDimmingController: DcDimmingController

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var collectorJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
        collectorJob?.cancel()
        collectorJob = serviceScope.launch {
            dcDimmingController.isDcDimmingEnabled.collectLatest {
                updateTileState()
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
        serviceScope.launch {
            val currentState = dcDimmingController.isDcDimmingEnabled.value
            dcDimmingController.setDcDimmingEnabled(!currentState)
            updateTileState()
        }
    }

    private fun updateTileState() {
        serviceScope.launch {
            val supported = dcDimmingController.isSupported()
            val enabled = if (supported) dcDimmingController.refreshState() else false

            qsTile?.apply {
                if (!supported) {
                    state = Tile.STATE_UNAVAILABLE
                    label = getString(R.string.tile_dc_dimming)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = "Unsupported"
                    }
                } else {
                    state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    label = getString(R.string.tile_dc_dimming)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = if (enabled) getString(R.string.status_active) else getString(R.string.status_inactive)
                    }
                }
                updateTile()
            }
        }
    }
}
