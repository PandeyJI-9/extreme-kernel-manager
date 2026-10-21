package id.nkz.nokontzzzmanager.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class CustomTunableRepositoryTest {

    // We test the parts of CustomTunableRepository that don't need a real DAO:
    // - listFiles output parsing (via mocked rootRepository)
    // FileItem data class

    private lateinit var rootRepository: RootRepository
    private lateinit var customTunableDao: id.nkz.nokontzzzmanager.data.database.CustomTunableDao
    private lateinit var repo: CustomTunableRepository

    @Before
    fun setUp() {
        rootRepository = mock(RootRepository::class.java)
        customTunableDao = mock(id.nkz.nokontzzzmanager.data.database.CustomTunableDao::class.java)
        repo = CustomTunableRepository(customTunableDao, rootRepository)
    }

    // --- listFiles ---

    @Test
    fun `listFiles returns empty list when root returns empty string`() = runBlocking {
        `when`(rootRepository.run("ls -p -1 \"/sys/block\"")).thenReturn("")
        val result = repo.listFiles("/sys/block")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `listFiles parses files and directories`() = runBlocking {
        val lsOutput = "zram0/\nloop0\nmmcblk0\n"
        `when`(rootRepository.run("ls -p -1 \"/sys/block\"")).thenReturn(lsOutput)
        val result = repo.listFiles("/sys/block")
        assertEquals(3, result.size)
        val dir = result.find { it.name == "zram0" }
        assertNotNull(dir)
        assertTrue(dir!!.isDirectory)
        assertEquals("/sys/block/zram0", dir.path)
        val file = result.find { it.name == "loop0" }
        assertNotNull(file)
        assertFalse(file!!.isDirectory)
        assertEquals("/sys/block/loop0", file.path)
    }

    @Test
    fun `listFiles sorts directories before files`() = runBlocking {
        val lsOutput = "loop0\nzram0/\nmmcblk0\n"
        `when`(rootRepository.run("ls -p -1 \"/sys/block\"")).thenReturn(lsOutput)
        val result = repo.listFiles("/sys/block")
        assertTrue(result.first().isDirectory)
        assertFalse(result.last().isDirectory)
    }

    @Test
    fun `listFiles handles root path correctly`() = runBlocking {
        val lsOutput = "sys/\nproc/\n"
        `when`(rootRepository.run("ls -p -1 \"/\"")).thenReturn(lsOutput)
        val result = repo.listFiles("/")
        val sys = result.find { it.name == "sys" }
        assertNotNull(sys)
        assertEquals("/sys", sys!!.path) // root path: no double slash
    }

    @Test
    fun `listFiles skips blank lines in output`() = runBlocking {
        val lsOutput = "zram0\n\n\nloop0\n"
        `when`(rootRepository.run("ls -p -1 \"/sys/block\"")).thenReturn(lsOutput)
        val result = repo.listFiles("/sys/block")
        assertEquals(2, result.size)
    }

    // --- FileItem ---

    @Test
    fun `FileItem stores name path and isDirectory correctly`() {
        val item = FileItem("zram0", "/sys/block/zram0", true)
        assertEquals("zram0", item.name)
        assertEquals("/sys/block/zram0", item.path)
        assertTrue(item.isDirectory)
    }
}
