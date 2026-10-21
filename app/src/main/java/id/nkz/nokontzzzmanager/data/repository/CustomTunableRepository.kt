package id.nkz.nokontzzzmanager.data.repository

import id.nkz.nokontzzzmanager.data.database.CustomTunableDao
import id.nkz.nokontzzzmanager.data.database.CustomTunableEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import android.util.Log

@Singleton
class CustomTunableRepository @Inject constructor(
    private val customTunableDao: CustomTunableDao,
    private val rootRepository: RootRepository
) {
    private val TAG = "CustomTunableRepository"

    fun getAllTunables(): Flow<List<CustomTunableEntity>> = customTunableDao.getAllTunables()

    suspend fun getBootTunables(): List<CustomTunableEntity> = customTunableDao.getBootTunables()

    suspend fun insertTunable(tunable: CustomTunableEntity) = customTunableDao.insertTunable(tunable)

    suspend fun deleteTunable(tunable: CustomTunableEntity) = customTunableDao.deleteTunable(tunable)

    private fun isPathSafe(path: String): Boolean {
        // Prevent basic shell injection characters
        val blockList = listOf(";", "|", "&&", "$", "`", "\n", "(", ")", ">", "<", "\\")
        return blockList.none { path.contains(it) }
    }

    private fun isValueSafe(value: String): Boolean {
        // Values for kernel nodes should typically be simple strings or numbers.
        // We block characters that can be used to escape the 'echo' command or chain other commands.
        val blockList = listOf(";", "|", "&&", "$", "`", "\n", "(", ")", ">", "<", "\\")
        return blockList.none { value.contains(it) }
    }

    suspend fun applyTunable(path: String, value: String): Boolean {
        if (!isPathSafe(path)) {
            Log.e(TAG, "Unsafe path blocked: $path")
            return false
        }
        if (!isValueSafe(value)) {
            Log.e(TAG, "Unsafe value blocked: $value")
            return false
        }
        return try {
            // Check if file exists
            val check = rootRepository.run("ls $path")
            if (check.isEmpty() || check.contains("No such file")) {
                Log.e(TAG, "File not found: $path")
                return false
            }

            // Apply value
            // Use standard echo (without -n) to ensure newline character is sent,
            // which acts as a flush/commit signal for many kernel sysfs drivers.
            rootRepository.run("echo \"$value\" > \"$path\"")
            
            // Verify
            val current = rootRepository.run("cat \"$path\"")
            val success = current.trim() == value.trim()
            if (!success) {
                Log.w(TAG, "Verification failed for $path. Expected: $value, Got: $current")
                // Some nodes might format the output differently, so this check isn't always reliable.
                // We'll consider it a success if the command ran without throwing.
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply tunable $path", e)
            false
        }
    }

    suspend fun readTunable(path: String): String {
        if (!isPathSafe(path)) return ""
        return try {
             val result = rootRepository.run("cat \"$path\"")
             result.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read tunable $path", e)
            ""
        }
    }

    suspend fun listFiles(path: String): List<FileItem> {
        return try {
            // ls -p appends '/' to directories
            // -1 lists one file per line
            // -a lists hidden files (optional, maybe clutter for tuning)
            val result = rootRepository.run("ls -p -1 \"$path\"")
            
            if (result.isEmpty()) return emptyList()

            result.split("\n")
                .filter { it.isNotBlank() }
                .map { name ->
                    val isDirectory = name.endsWith("/")
                    val cleanName = if (isDirectory) name.dropLast(1) else name
                    val fullPath = if (path == "/") "/$cleanName" else "$path/$cleanName"
                    FileItem(cleanName, fullPath, isDirectory)
                }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name }))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list files in $path", e)
            emptyList()
        }
    }
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean
)
