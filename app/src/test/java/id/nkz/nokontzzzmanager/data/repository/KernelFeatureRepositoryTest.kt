package id.nkz.nokontzzzmanager.data.repository

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class KernelFeatureRepositoryTest {

    private lateinit var repo: KernelFeatureRepository

    @Before
    fun setUp() {
        repo = KernelFeatureRepository(
            mock(id.nkz.nokontzzzmanager.data.repository.SysfsHelper::class.java),
            mock(id.nkz.nokontzzzmanager.data.repository.RootRepository::class.java)
        )
    }

    // --- parseKgslSkipZeroingValue ---

    @Test
    fun `parseKgslSkipZeroingValue returns true for "1"`() {
        assertTrue(repo.parseKgslSkipZeroingValue("1"))
    }

    @Test
    fun `parseKgslSkipZeroingValue returns false for "0"`() {
        assertFalse(repo.parseKgslSkipZeroingValue("0"))
    }

    @Test
    fun `parseKgslSkipZeroingValue returns false for null`() {
        assertFalse(repo.parseKgslSkipZeroingValue(null))
    }

    @Test
    fun `parseKgslSkipZeroingValue returns false for non-numeric string`() {
        assertFalse(repo.parseKgslSkipZeroingValue("enabled"))
    }

    @Test
    fun `parseKgslSkipZeroingValue returns false for "2"`() {
        assertFalse(repo.parseKgslSkipZeroingValue("2"))
    }
}
