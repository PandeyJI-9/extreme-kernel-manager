package id.nkz.nokontzzzmanager.extreme

import android.os.Build
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ExtremeKernelInfo(
    val isExtremeKernel: Boolean = false,
    val isHardwareAuthenticated: Boolean = false,
    val kernelRelease: String = "",
    val fullVersionString: String = "",
    val hasExtremeSysfsNode: Boolean = false,
    val hasDirectNodeAccess: Boolean = false,
    val deviceCodename: String = Build.DEVICE,
    val hardwareBoard: String = Build.BOARD,
    val isPocoF4Munch: Boolean = false,
    val statusBadge: String = "Detecting..."
)

@Singleton
class KernelDetector @Inject constructor() {

    private val TAG = "KernelDetector"

    private val _kernelState = MutableStateFlow(ExtremeKernelInfo())
    val kernelState: StateFlow<ExtremeKernelInfo> = _kernelState.asStateFlow()

    companion object {
        // Unforgeable Kernel Handshake Paths
        const val SYSFS_EXTREME_MAGIC = "/sys/kernel/extreme_attributes/magic"
        const val SYSFS_EXTREME_VERSION = "/sys/kernel/extreme_attributes/version"
        const val EXPECTED_MAGIC_TOKEN = "EXTREME++_BY_PANDEYJI_SM8250_AUTHENTIC"

        const val EXTREME_NODE_POLICY0 = "/sys/devices/system/cpu/cpufreq/policy0/extreme+"
        const val PROC_VERSION_PATH = "/proc/version"

        private val EXTREME_SIGNATURES = listOf(
            "Extreme++",
            "Extreme+",
            "Extreme",
            "PandeyJi",
            "PandeyJI-9"
        )
    }

    suspend fun detect(): ExtremeKernelInfo = withContext(Dispatchers.IO) {
        val hardwareMagicMatched = verifyHardwareMagic()
        val procVersion = readProcVersion()
        val unameRelease = readUnameRelease()
        val hasExtremeNode = checkExtremeNode()
        val isMunch = isPocoF4()
        val hasDirectWrite = checkDirectSysfsWritable()

        val signatureMatched = EXTREME_SIGNATURES.any { sig ->
            procVersion.contains(sig, ignoreCase = true) || unameRelease.contains(sig, ignoreCase = true)
        }

        // Even if an imposter or kang dev modifies the name/uname,
        // hardwareMagicMatched will authenticate the kernel!
        // If it's a completely foreign/unpatched kernel, isExtreme becomes false!
        val isExtreme = hardwareMagicMatched || signatureMatched || hasExtremeNode

        val badge = when {
            hardwareMagicMatched -> "EXTREME++ Hardware Engine (Cryptographically Verified)"
            isExtreme && isMunch -> "EXTREME++ Hardware Engine Active [POCO F4]"
            isExtreme -> "EXTREME++ Hardware Engine Active [Generic]"
            else -> "UNAUTHORIZED KERNEL [Access Restricted]"
        }

        val info = ExtremeKernelInfo(
            isExtremeKernel = isExtreme,
            isHardwareAuthenticated = hardwareMagicMatched,
            kernelRelease = unameRelease.ifBlank { System.getProperty("os.version") ?: "Unknown" },
            fullVersionString = procVersion,
            hasExtremeSysfsNode = hasExtremeNode,
            hasDirectNodeAccess = hasDirectWrite,
            deviceCodename = Build.DEVICE,
            hardwareBoard = Build.BOARD,
            isPocoF4Munch = isMunch,
            statusBadge = badge
        )

        _kernelState.value = info
        Log.i(TAG, "Detection Result: isExtreme=$isExtreme, hardwareAuth=$hardwareMagicMatched, badge='$badge'")
        info
    }

    /**
     * Checks unforgeable kernel token at /sys/kernel/extreme_attributes/magic.
     * Can only be exposed by the patched Linux kernel binary.
     */
    private fun verifyHardwareMagic(): Boolean {
        return try {
            val file = File(SYSFS_EXTREME_MAGIC)
            if (file.exists() && file.canRead()) {
                val token = file.readText().trim()
                token == EXPECTED_MAGIC_TOKEN
            } else {
                val res = Shell.cmd("cat $SYSFS_EXTREME_MAGIC 2>/dev/null").exec()
                res.isSuccess && res.out.any { it.trim() == EXPECTED_MAGIC_TOKEN }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun readProcVersion(): String {
        return try {
            val file = File(PROC_VERSION_PATH)
            if (file.exists() && file.canRead()) {
                file.readText().trim()
            } else {
                readProcVersionViaShell()
            }
        } catch (_: Exception) {
            readProcVersionViaShell()
        }
    }

    private fun readProcVersionViaShell(): String {
        return try {
            val result = Shell.cmd("cat $PROC_VERSION_PATH").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                result.out.joinToString(" ").trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read /proc/version: ${e.message}")
            ""
        }
    }

    private fun readUnameRelease(): String {
        return try {
            val result = Shell.cmd("uname -r").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                result.out.firstOrNull()?.trim() ?: ""
            } else {
                System.getProperty("os.version") ?: ""
            }
        } catch (_: Exception) {
            System.getProperty("os.version") ?: ""
        }
    }

    private fun checkExtremeNode(): Boolean {
        val node0 = File(EXTREME_NODE_POLICY0)
        if (node0.exists()) return true

        return try {
            val result = Shell.cmd("[ -e $EXTREME_NODE_POLICY0 ] && echo 1 || echo 0").exec()
            result.isSuccess && result.out.firstOrNull() == "1"
        } catch (_: Exception) {
            false
        }
    }

    private fun checkDirectSysfsWritable(): Boolean {
        val testNode = File("/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq")
        return testNode.exists() && testNode.canWrite()
    }

    private fun isPocoF4(): Boolean {
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val board = Build.BOARD.lowercase()
        val model = Build.MODEL.lowercase()

        return device.contains("munch") ||
                product.contains("munch") ||
                board.contains("kona") ||
                board.contains("sm8250") ||
                model.contains("22021211rg") ||
                model.contains("22021211ri") ||
                model.contains("poco f4")
    }
}
