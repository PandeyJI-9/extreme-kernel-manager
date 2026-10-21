package id.nkz.nokontzzzmanager.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import id.nkz.nokontzzzmanager.data.model.KernelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KernelInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sysfsHelper: SysfsHelper
) {
    private fun getSystemProperty(key: String): String? = try {
        val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
        val result = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()
        process.waitFor()
        process.destroy()
        result?.ifBlank { null }
    } catch (_: Exception) { null }

    suspend fun getKernelInfo(): KernelInfo {

        // Get kernel version
        val version = sysfsHelper.readFileToString("/proc/version", "Kernel Version")
            ?: Build.VERSION.RELEASE

        // Improved GKI type detection with version-based detection
        val gkiType = when {
            // Check for specific GKI patterns first (more specific)
            version.contains("gki", ignoreCase = true) ||
            version.contains("generic kernel image", ignoreCase = true) ||
            Build.VERSION.RELEASE.contains("gki", ignoreCase = true) -> "Generic Kernel Image (GKI)"

            // Check for Android Common Kernel patterns
            version.contains("android-mainline", ignoreCase = true) ||
            version.contains("android-common", ignoreCase = true) -> "Android Common Kernel (ACK)"

            // GKI version detection based on Linux kernel version
            version.contains("Linux version", ignoreCase = true) -> {
                // Extract kernel version number
                val versionRegex = """Linux version (\d+\.\d+)""".toRegex()
                val kernelVersionMatch = versionRegex.find(version)
                val kernelVersion = kernelVersionMatch?.groupValues?.get(1)?.toFloatOrNull()

                when {
                    kernelVersion != null && kernelVersion >= 6.6f -> "Generic Kernel Image (GKI 2.0)"
                    kernelVersion != null && kernelVersion >= 5.15f -> "Generic Kernel Image (GKI 2.0)"
                    kernelVersion != null && kernelVersion >= 5.10f -> "Generic Kernel Image (GKI 2.0)"
                    kernelVersion != null && kernelVersion >= 5.4f -> "Generic Kernel Image (GKI 1.0)"
                    kernelVersion != null && kernelVersion == 4.19f -> "Non GKI"
                    version.contains("android", ignoreCase = true) -> "Android Kernel"
                    else -> "Linux Kernel $kernelVersion"
                }
            }

            // Check build fingerprint for additional clues
            Build.FINGERPRINT.contains("gki", ignoreCase = true) -> "Generic Kernel Image (GKI)"

            // Fallback check for android (less specific) - moved to lower priority
            version.contains("android", ignoreCase = true) -> "Android Kernel"

            else -> "Custom/OEM Kernel"
        }

        // Get scheduler information with better fallback paths
        var scheduler = sysfsHelper.readFileToString("/sys/block/sda/queue/scheduler", "I/O Scheduler")
            ?.let { schedulerLine ->
                // Extract currently active scheduler (marked with brackets)
                val activeSchedulerRegex = """\[([^]]+)]""".toRegex()
                activeSchedulerRegex.find(schedulerLine)?.groupValues?.get(1) ?: schedulerLine.trim()
            }

        if (scheduler == null) {
            // Try alternative block devices
            val alternativeDevices = listOf("mmcblk0", "nvme0n1", "sdb", "sdc")
            for (device in alternativeDevices) {
                val altScheduler = sysfsHelper.readFileToString("/sys/block/$device/queue/scheduler", "I/O Scheduler ($device)")
                if (altScheduler != null) {
                    val activeSchedulerRegex = """\[([^]]+)]""".toRegex()
                    scheduler = activeSchedulerRegex.find(altScheduler)?.groupValues?.get(1) ?: altScheduler.trim()
                    if (scheduler.isNotBlank()) break
                }
            }
        }
        if (scheduler.isNullOrBlank()) scheduler = "Unknown"

        // Get SELinux status
        var selinuxStatus = sysfsHelper.readFileToString("/sys/fs/selinux/enforce", "SELinux Status")
            ?.let { enforceValue ->
                when (enforceValue.trim()) {
                    "1" -> "Enforcing"
                    "0" -> "Permissive"
                    else -> "Unknown"
                }
            }

        if (selinuxStatus == null) {
            // Fallback: try getenforce command
            selinuxStatus = try {
                withContext(Dispatchers.IO) {
                    val process = Runtime.getRuntime().exec("getenforce")
                    val result = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()
                    process.waitFor()
                    process.destroy()
                    result ?: "Unknown"
                }
            } catch (e: Exception) {
                "Unknown"
            }
        }

        // Get ABI
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"

        // Get architecture
        val architecture = when {
            abi.contains("arm64") || abi.contains("aarch64") -> "ARM64"
            abi.contains("arm") -> "ARM"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "x86"
            else -> abi
        }

        // Get WireGuard Version
        val wireguardVersion = sysfsHelper.readFileToString("/sys/module/wireguard/version", "WireGuard Version")

        // Enhanced KernelSU detection - improved with better logging and error handling
        val kernelSuStatus = when {
            // Method 1: Check kernel version for KernelSU signature (primary method)
            version.contains("KernelSU", ignoreCase = true) -> {
                // Extract KernelSU version if available
                val ksuVersionRegex = """KernelSU[ -]?v?(\d+\.\d+\.\d+)""".toRegex()
                val ksuMatch = ksuVersionRegex.find(version)
                if (ksuMatch != null) {
                    "Active (${ksuMatch.groupValues[1]})"
                } else {
                    "Active"
                }
            }

            // Method 2: Check KernelSU directory
            File("/data/adb/ksu").exists() -> "Active"

            // Method 3: Check for KernelSU binary
            File("/system/bin/ksu").exists() -> "Active"

            // Method 4: Try various detection methods
            else -> {
                // Helper function for additional KernelSU checks
                suspend fun checkOtherKsuMethods(): String {
                    // Check kernel cmdline
                    val cmdline = sysfsHelper.readFileToString("/proc/cmdline", "Kernel Command Line")
                    if (cmdline?.contains("ksu", ignoreCase = true) == true) {
                        return "Active"
                    }

                    // Check for KernelSU manager app
                    try {
                        context.packageManager.getPackageInfo("me.weishu.kernelsu", 0)
                        return "Detected (Manager Installed)"
                    } catch (e: Exception) {
                        // Ignore and continue
                    }

                    // Check system properties
                    if (getSystemProperty("ro.kernel.su")?.isNotEmpty() == true) {
                        return "Active"
                    }

                    // Default case - not detected
                    return "Not Detected"
                }

                // Helper: cek keberadaan binary di PATH atau path absolut
                fun binaryExists(cmd: String): Boolean {
                    return try {
                        if (cmd.contains("/")) {
                            File(cmd).exists()
                        } else {
                            val common = listOf("/system/bin/$cmd", "/system/xbin/$cmd", "/vendor/bin/$cmd")
                            if (common.any { File(it).exists() }) return true
                            
                            // For simplicity in this context, we'll stick to a more direct check or assume false if not in common paths
                            false
                        }
                    } catch (_: Exception) {
                        false
                    }
                }

                // Enhanced function to execute KernelSU commands dengan error handling lebih aman
                suspend fun executeKsuCommand(command: Array<String>, description: String): String? {
                    return withContext(Dispatchers.IO) {
                        var process: Process? = null
                        try {
                            // Hindari IOException: No such file or directory saat binary tidak ada
                            if (command.isNotEmpty()) {
                                val bin = command[0]
                                val notFound = when {
                                    bin == "ksu" -> !binaryExists("ksu")
                                    bin.startsWith("/") -> !File(bin).exists()
                                    else -> false
                                }
                                if (notFound) {
                                    return@withContext null
                                }
                            }

                            process = Runtime.getRuntime().exec(command)
                            val reader = BufferedReader(InputStreamReader(process.inputStream))
                            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                            val output = StringBuilder()
                            val errorOutput = StringBuilder()
                            var line: String?

                            // Read output
                            while (reader.readLine().also { line = it } != null) {
                                output.append(line).append("\n")
                            }

                            // Read error stream
                            while (errorReader.readLine().also { line = it } != null) {
                                errorOutput.append(line).append("\n")
                            }

                            val exitCode = process.waitFor()

                            if (errorOutput.isNotEmpty()) {
                                Log.w("SystemRepository", "KSU Command Error: $errorOutput")
                            }

                            reader.close()
                            errorReader.close()

                            if (exitCode == 0) {
                                val result = output.toString().trim()
                                return@withContext result.ifBlank { null }
                            }

                        } catch (e: Exception) {
                            // Jangan spam stacktrace untuk ENOENT; cukup log ringkas
                        } finally {
                            process?.destroy()
                        }
                        null
                    }
                }

                // Try ksu -V command first
                val ksuVOutput = executeKsuCommand(arrayOf("ksu", "-V"), "ksu -V")
                if (ksuVOutput != null) {
                    "Active ($ksuVOutput)"
                } else {
                    // Try su -c "ksu -V" command
                    val suKsuVOutput = executeKsuCommand(arrayOf("su", "-c", "ksu -V"), "su -c ksu -V")
                    if (suKsuVOutput != null) {
                        "Active ($suKsuVOutput)"
                    } else {
                        // Try /data/adb/ksud --version command
                        val ksudOutput = executeKsuCommand(arrayOf("su", "-c", "/data/adb/ksud --version"), "su -c /data/adb/ksud --version")
                        if (ksudOutput != null) {
                            "Active ($ksudOutput)"
                        } else {
                            // Try alternative ksud paths
                            val alternativeKsudPaths = listOf(
                                "/data/adb/ksud version",
                                "/data/adb/ksu/bin/ksud --version",
                                "/data/adb/modules/kernelsu/bin/ksud --version"
                            )

                            var foundOutput: String? = null
                            for (ksudPath in alternativeKsudPaths) {
                                val altOutput = executeKsuCommand(arrayOf("su", "-c", ksudPath), "su -c $ksudPath")
                                if (altOutput != null) {
                                    foundOutput = altOutput
                                    break
                                }
                            }

                            if (foundOutput != null) {
                                "Active ($foundOutput)"
                            } else {
                                // Check if we can find ksud binary directly
                                val ksudPaths = listOf(
                                    "/data/adb/ksud",
                                    "/data/adb/ksu/bin/ksud",
                                    "/data/adb/modules/kernelsu/bin/ksud"
                                )

                                var binaryFound = false
                                for (ksudPath in ksudPaths) {
                                    if (File(ksudPath).exists()) {
                                        binaryFound = true
                                        break
                                    }
                                }

                                if (binaryFound) {
                                    "Active (Binary Found)"
                                } else {
                                    // Final fallback checks
                                    checkOtherKsuMethods()
                                }
                            }
                        }
                    }
                }
            }
        }

        return KernelInfo(
            version = version,
            gkiType = gkiType,
            scheduler = scheduler,
            selinuxStatus = selinuxStatus,
            abi = abi,
            architecture = architecture,
            kernelSuStatus = kernelSuStatus,
            fingerprint = Build.FINGERPRINT,
            wireguardVersion = wireguardVersion
        )
    }
}

