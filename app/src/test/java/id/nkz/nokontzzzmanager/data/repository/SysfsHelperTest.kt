package id.nkz.nokontzzzmanager.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.*

class SysfsHelperTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var rootRepository: RootRepository
    private lateinit var sysfsHelper: SysfsHelper

    @Before
    fun setUp() {
        rootRepository = mock(RootRepository::class.java)
        sysfsHelper = SysfsHelper(rootRepository)
    }

    // --- readFileToString ---

    @Test
    fun `readFileToString returns content from readable file`() = runBlocking {
        val file = tmpFolder.newFile("gov").apply { writeText("schedutil") }
        val result = sysfsHelper.readFileToString(file.absolutePath, "governor", attemptSu = false)
        assertEquals("schedutil", result)
    }

    @Test
    fun `readFileToString falls back to su when file not directly readable`() = runBlocking {
        val path = "/sys/nonexistent/file"
        `when`(rootRepository.run("cat \"$path\"", useRetry = true)).thenReturn("performance")
        val result = sysfsHelper.readFileToString(path, "governor", attemptSu = true)
        assertEquals("performance", result)
    }

    @Test
    fun `readFileToString returns null when su also fails`() = runBlocking {
        val path = "/sys/nonexistent/file"
        `when`(rootRepository.run("cat \"$path\"", useRetry = true)).thenThrow(RuntimeException("no root"))
        val result = sysfsHelper.readFileToString(path, "governor", attemptSu = true)
        assertNull(result)
    }

    @Test
    fun `readFileToString skips su when attemptSu false and file missing`() = runBlocking {
        val path = "/sys/nonexistent/file"
        val result = sysfsHelper.readFileToString(path, "governor", attemptSu = false)
        assertNull(result)
        verifyNoInteractions(rootRepository)
    }

    @Test
    fun `readFileToString returns null for blank file content, falls through to su`() = runBlocking {
        val file = tmpFolder.newFile("blank").apply { writeText("   ") }
        `when`(rootRepository.run("cat \"${file.absolutePath}\"", useRetry = true)).thenReturn("powersave")
        val result = sysfsHelper.readFileToString(file.absolutePath, "governor", attemptSu = true)
        assertEquals("powersave", result)
    }

    // --- writeStringToFile ---

    @Test
    fun `writeStringToFile writes to writable file`() = runBlocking {
        val file = tmpFolder.newFile("swappiness")
        val success = sysfsHelper.writeStringToFile(file.absolutePath, "60", "swappiness", attemptSu = false)
        assertTrue(success)
        assertEquals("60", file.readText())
    }

    @Test
    fun `writeStringToFile falls back to su when file not writable`() = runBlocking {
        val path = "/sys/nonexistent/swappiness"
        `when`(rootRepository.run("echo -n \"60\" > \"$path\"")).thenReturn("")
        val success = sysfsHelper.writeStringToFile(path, "60", "swappiness", attemptSu = true)
        assertTrue(success)
    }

    @Test
    fun `writeStringToFile returns false when su also throws`() = runBlocking {
        val path = "/sys/nonexistent/swappiness"
        `when`(rootRepository.run("echo -n \"60\" > \"$path\"")).thenThrow(RuntimeException("no root"))
        val success = sysfsHelper.writeStringToFile(path, "60", "swappiness", attemptSu = true)
        assertFalse(success)
    }

    @Test
    fun `writeStringToFile skips su when attemptSu false and file missing`() = runBlocking {
        val path = "/sys/nonexistent/swappiness"
        val success = sysfsHelper.writeStringToFile(path, "60", "swappiness", attemptSu = false)
        assertFalse(success)
        verifyNoInteractions(rootRepository)
    }
}
