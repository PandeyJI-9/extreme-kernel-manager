package id.nkz.nokontzzzmanager.extreme

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class DcDimmingMode {
    DSI_PRIMARY_EA,    // /sys/devices/platform/soc/soc:qcom,dsi-display-primary/msm_fb_ea_enable (1 / 0)
    DRM_DISP_PARAM,    // /sys/class/drm/card0-DSI-1/disp_param (0x10000 / 0x20000 or 1 / 0)
    FB0_EA,            // /sys/devices/virtual/graphics/fb0/msm_fb_ea_enable (1 / 0)
    UNSUPPORTED
}

@Singleton
class DcDimmingController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "DcDimmingController"

    companion object {
        const val NODE_DSI_PRIMARY = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/msm_fb_ea_enable"
        const val NODE_DRM_DISP_PARAM = "/sys/class/drm/card0-DSI-1/disp_param"
        const val NODE_FB0_EA = "/sys/devices/virtual/graphics/fb0/msm_fb_ea_enable"

        const val DISP_PARAM_ENABLE_HEX = "0x10000"
        const val DISP_PARAM_DISABLE_HEX = "0x20000"
    }

    private var activeMode: DcDimmingMode = DcDimmingMode.UNSUPPORTED
    private var activePath: String? = null

    private val _isDcDimmingEnabled = MutableStateFlow(false)
    val isDcDimmingEnabled: StateFlow<Boolean> = _isDcDimmingEnabled.asStateFlow()

    init {
        detectHardwareNode()
    }

    fun isSupported(): Boolean = activeMode != DcDimmingMode.UNSUPPORTED

    fun getActiveNodePath(): String? = activePath

    private fun detectHardwareNode() {
        // Probe in priority order for Munch (POCO F4)
        when {
            checkNodeExists(NODE_DSI_PRIMARY) -> {
                activeMode = DcDimmingMode.DSI_PRIMARY_EA
                activePath = NODE_DSI_PRIMARY
            }
            checkNodeExists(NODE_DRM_DISP_PARAM) -> {
                activeMode = DcDimmingMode.DRM_DISP_PARAM
                activePath = NODE_DRM_DISP_PARAM
            }
            checkNodeExists(NODE_FB0_EA) -> {
                activeMode = DcDimmingMode.FB0_EA
                activePath = NODE_FB0_EA
            }
            else -> {
                activeMode = DcDimmingMode.UNSUPPORTED
                activePath = null
            }
        }
        Log.i(TAG, "Munch DC Dimming Hardware Node: $activeMode at $activePath")
    }

    private fun checkNodeExists(path: String): Boolean {
        val file = File(path)
        if (file.exists()) return true

        return try {
            val res = Shell.cmd("[ -e $path ] && echo 1 || echo 0").exec()
            res.isSuccess && res.out.firstOrNull() == "1"
        } catch (_: Exception) {
            false
        }
    }

    suspend fun refreshState(): Boolean = withContext(Dispatchers.IO) {
        if (!isSupported()) {
            _isDcDimmingEnabled.value = false
            return@withContext false
        }

        val path = activePath ?: return@withContext false
        val content = readNodeValue(path)

        val isEnabled = when (activeMode) {
            DcDimmingMode.DRM_DISP_PARAM -> {
                content.contains("0x10000", ignoreCase = true) || content == "1"
            }
            DcDimmingMode.DSI_PRIMARY_EA, DcDimmingMode.FB0_EA -> {
                content.trim() == "1"
            }
            DcDimmingMode.UNSUPPORTED -> false
        }

        _isDcDimmingEnabled.value = isEnabled
        isEnabled
    }

    suspend fun setDcDimmingEnabled(enable: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!isSupported()) {
            Log.w(TAG, "DC Dimming not supported on this kernel/ROM combination")
            return@withContext false
        }

        val path = activePath ?: return@withContext false

        val valueToWrite = when (activeMode) {
            DcDimmingMode.DRM_DISP_PARAM -> if (enable) DISP_PARAM_ENABLE_HEX else DISP_PARAM_DISABLE_HEX
            DcDimmingMode.DSI_PRIMARY_EA, DcDimmingMode.FB0_EA -> if (enable) "1" else "0"
            DcDimmingMode.UNSUPPORTED -> return@withContext false
        }

        val success = writeNodeDualTier(path, valueToWrite)
        if (success) {
            _isDcDimmingEnabled.value = enable
            Log.i(TAG, "DC Dimming set to $enable on $path")
        } else {
            Log.e(TAG, "Failed to write DC Dimming value $valueToWrite to $path")
        }

        success
    }

    private fun readNodeValue(path: String): String {
        val file = File(path)
        if (file.exists() && file.canRead()) {
            try {
                return file.readText().trim()
            } catch (_: Exception) {}
        }

        return try {
            val res = Shell.cmd("cat $path 2>/dev/null").exec()
            if (res.isSuccess && res.out.isNotEmpty()) {
                res.out.first().trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading node $path: ${e.message}")
            ""
        }
    }

    private fun writeNodeDualTier(path: String, value: String): Boolean {
        val file = File(path)
        if (file.exists() && file.canWrite()) {
            try {
                file.writeText(value)
                return true
            } catch (_: Exception) {}
        }

        return try {
            val res = Shell.cmd("echo '$value' > '$path'").exec()
            res.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Shell write error for $path: ${e.message}")
            false
        }
    }
}
