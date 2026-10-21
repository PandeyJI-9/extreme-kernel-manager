package id.nkz.nokontzzzmanager.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThermalWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    private val TAG = "ThermalWorker"
    private val thermalSysfsNode = "/sys/class/thermal/thermal_message/sconfig"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modeIndex = inputData.getInt("thermal_mode", 0)
        Log.d(TAG, "ThermalWorker: Applying thermal mode $modeIndex")

        val currentValue = Shell.cmd("cat $thermalSysfsNode").exec().out.joinToString("").trim().toIntOrNull() ?: -1
        if (currentValue == modeIndex) {
            Log.d(TAG, "ThermalWorker: Mode already $modeIndex, no need to apply")
            return@withContext Result.success()
        }

        // Apply with robust permissions and SELinux context handling
        // Some kernels/engines lock this node with 0444
        Shell.cmd("chcon u:object_r:sysfs_thermal:s0 $thermalSysfsNode").exec()
        Shell.cmd("chmod 0644 $thermalSysfsNode").exec()
        val result = Shell.cmd("echo $modeIndex > $thermalSysfsNode").exec()
        // Lock with read-only permissions to prevent system override
        Shell.cmd("chmod 0444 $thermalSysfsNode").exec()

        if (result.isSuccess) {
            val verifyValue = Shell.cmd("cat $thermalSysfsNode").exec().out.joinToString("").trim().toIntOrNull() ?: -1
            if (verifyValue == modeIndex) {
                Log.i(TAG, "ThermalWorker: Successfully applied mode $modeIndex")
                return@withContext Result.success()
            }
        }
        
        Log.e(TAG, "ThermalWorker: Failed to apply mode $modeIndex")
        Result.retry()
    }
}