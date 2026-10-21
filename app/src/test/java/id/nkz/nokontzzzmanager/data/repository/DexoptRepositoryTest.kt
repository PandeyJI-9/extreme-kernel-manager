package id.nkz.nokontzzzmanager.data.repository

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DexoptRepositoryTest {

    private lateinit var repo: DexoptRepository

    @Before
    fun setUp() {
        repo = DexoptRepository()
    }

    // --- setRunning ---

    @Test
    fun `setRunning true sets isRunning and resets state`() {
        repo.setFinished(true)
        repo.setCanceled(true)
        repo.setRunning(true)
        assertTrue(repo.isRunning.value)
        assertFalse(repo.isFinished.value)
        assertFalse(repo.isCanceled.value)
        assertEquals(listOf("Starting..."), repo.recentLogs.value)
    }

    @Test
    fun `setRunning false only sets isRunning false`() {
        repo.setRunning(true)
        repo.setRunning(false)
        assertFalse(repo.isRunning.value)
        // state flags unchanged by setRunning(false)
        assertFalse(repo.isFinished.value)
        assertFalse(repo.isCanceled.value)
    }

    // --- setFinished ---

    @Test
    fun `setFinished true sets isFinished and clears isRunning`() {
        repo.setRunning(true)
        repo.setFinished(true)
        assertTrue(repo.isFinished.value)
        assertFalse(repo.isRunning.value)
    }

    @Test
    fun `setFinished false only sets isFinished false`() {
        repo.setFinished(true)
        repo.setFinished(false)
        assertFalse(repo.isFinished.value)
    }

    // --- setCanceled ---

    @Test
    fun `setCanceled true sets isCanceled and clears isRunning`() {
        repo.setRunning(true)
        repo.setCanceled(true)
        assertTrue(repo.isCanceled.value)
        assertFalse(repo.isRunning.value)
    }

    @Test
    fun `setCanceled false only sets isCanceled false`() {
        repo.setCanceled(true)
        repo.setCanceled(false)
        assertFalse(repo.isCanceled.value)
    }

    // --- updateLastLog ---

    @Test
    fun `updateLastLog appends new log entry`() {
        repo.setRunning(true) // resets to ["Starting..."]
        repo.updateLastLog("Step 1")
        repo.updateLastLog("Step 2")
        val logs = repo.recentLogs.value
        assertTrue(logs.contains("Step 1"))
        assertTrue(logs.contains("Step 2"))
    }

    @Test
    fun `updateLastLog deduplicates consecutive identical lines`() {
        repo.setRunning(true)
        repo.updateLastLog("Optimizing...")
        repo.updateLastLog("Optimizing...") // duplicate, should be ignored
        assertEquals(1, repo.recentLogs.value.count { it == "Optimizing..." })
    }

    @Test
    fun `updateLastLog ignores blank lines`() {
        repo.setRunning(true)
        val before = repo.recentLogs.value.size
        repo.updateLastLog("   ")
        assertEquals(before, repo.recentLogs.value.size)
    }

    @Test
    fun `updateLastLog caps at 50 entries`() {
        repo.setRunning(true)
        repeat(60) { repo.updateLastLog("line $it") }
        assertEquals(50, repo.recentLogs.value.size)
    }

    @Test
    fun `updateLastLog keeps last 50 when capped`() {
        repo.setRunning(true)
        repeat(60) { repo.updateLastLog("line $it") }
        // last entry should be "line 59"
        assertEquals("line 59", repo.recentLogs.value.last())
    }

    // --- clearLogs ---

    @Test
    fun `clearLogs resets logs to Ready when not running`() {
        repo.setRunning(true)
        repo.setFinished(true) // also sets isRunning=false
        repo.updateLastLog("some log")
        repo.clearLogs()
        assertEquals(listOf("Ready"), repo.recentLogs.value)
        assertFalse(repo.isFinished.value)
    }

    @Test
    fun `clearLogs does nothing when isRunning`() {
        repo.setRunning(true)
        repo.updateLastLog("active log")
        val before = repo.recentLogs.value.toList()
        repo.clearLogs()
        assertEquals(before, repo.recentLogs.value)
    }

    // --- logs alias ---

    @Test
    fun `logs StateFlow is same reference as recentLogs`() {
        repo.setRunning(true)
        repo.updateLastLog("test")
        assertEquals(repo.recentLogs.value, repo.logs.value)
    }
}
