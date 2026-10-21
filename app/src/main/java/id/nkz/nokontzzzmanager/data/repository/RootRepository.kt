package id.nkz.nokontzzzmanager.data.repository

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootRepository @Inject constructor() {

    private var lastRootCheckTime = 0L
    private var cachedRootStatus = false

    init {
        Shell.enableVerboseLogging = false
    }

    fun isRooted(): Boolean = Shell.getShell().isRoot

    /**
     * Checks root status with cache invalidation to avoid excessive system calls
     */
    suspend fun checkRootFresh(): Boolean {
        val currentTime = System.currentTimeMillis()
        // Cache result for 500ms to avoid excessive system calls
        if (currentTime - lastRootCheckTime < 500) {
            return cachedRootStatus
        }
        
        cachedRootStatus = checkRootStatus()
        lastRootCheckTime = currentTime
        return cachedRootStatus
    }
    
    /**
     * Performs actual root check by executing system command
     */
    private suspend fun checkRootStatus(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = Shell.cmd("id").exec()
                result.isSuccess && (result.out.any { it.contains("uid=0") || it.contains("root") })
            } catch (e: Exception) {
                Log.e("RootRepository", "Error checking root access: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Executes a command with optional retry logic
     */
    suspend fun run(cmd: String, useRetry: Boolean = true): String {
        return executeCommand(cmd, if (useRetry) 2 else 0)
    }

    /**
     * Executes a command with retry mechanism
     */
    private suspend fun executeCommand(cmd: String, maxRetries: Int): String {
        var attempts = 0
        var lastError: Exception? = null

        return withContext(Dispatchers.IO) {
            while (attempts <= maxRetries) {
                try {
                    val result = Shell.cmd(cmd).exec()
                    if (result.isSuccess) {
                        return@withContext result.out.joinToString("\n")
                    } else {
                        Log.w("RootRepository", "Command failed (attempt ${attempts + 1}/$maxRetries): $cmd, Error: ${result.err.joinToString("\n")}")
                    }
                } catch (e: Exception) {
                    Log.w("RootRepository", "Exception during command execution (attempt ${attempts + 1}/$maxRetries): $cmd", e)
                    lastError = e
                }
                
                attempts++
                if (attempts <= maxRetries) {
                    // Wait a bit before retrying
                    delay(100 * attempts.toLong())
                }
            }
            
            Log.e("RootRepository", "Command failed after $maxRetries retries: $cmd")
            throw lastError ?: RuntimeException("Command execution failed: $cmd")
        }
    }
    
    /**
     * Checks if root access has been revoked since last successful operation
     */
    suspend fun isRootStillAvailable(): Boolean {
        // Use the cached check if within cache window, otherwise refresh
        return checkRootFresh()
    }
}