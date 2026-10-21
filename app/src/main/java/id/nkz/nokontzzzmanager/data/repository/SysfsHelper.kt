package id.nkz.nokontzzzmanager.data.repository

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared kernel sysfs file read/write utilities.
 * Extracted from SystemRepository to avoid duplication across split repositories.
 */
@Singleton
class SysfsHelper @Inject constructor(
    private val rootRepository: RootRepository,
) {
    suspend fun readFileToString(
        filePath: String,
        fileDescription: String,
        attemptSu: Boolean = true,
        useRetry: Boolean = true,
    ): String? {
        val file = File(filePath)
        try {
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                if (content.isNotBlank()) return content
                else if (!attemptSu) return null
            }
        } catch (_: SecurityException) {
        } catch (_: FileNotFoundException) {
        } catch (_: IOException) {
            return null
        } catch (_: Exception) {
            return null
        }

        if (attemptSu) {
            try {
                val result = rootRepository.run("cat \"$filePath\"", useRetry = useRetry)
                if (result.isNotBlank()) return result.trim()
            } catch (_: Exception) {
                return null
            }
        }
        return null
    }

    suspend fun writeStringToFile(
        filePath: String,
        content: String,
        fileDescription: String,
        attemptSu: Boolean = true,
    ): Boolean {
        val file = File(filePath)
        try {
            if (file.exists() && file.canWrite()) {
                file.writeText(content)
                return true
            } else if (!attemptSu) return false
        } catch (_: SecurityException) {
        } catch (_: FileNotFoundException) {
        } catch (_: IOException) {
            return false
        } catch (_: Exception) {
            return false
        }

        if (attemptSu) {
            try {
                rootRepository.run("echo -n \"$content\" > \"$filePath\"")
                return true
            } catch (_: Exception) {
                return false
            }
        }
        return false
    }
}
