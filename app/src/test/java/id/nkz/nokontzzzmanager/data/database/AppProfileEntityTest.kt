package id.nkz.nokontzzzmanager.data.database

import id.nkz.nokontzzzmanager.data.model.ClusterConfig
import id.nkz.nokontzzzmanager.data.model.CpuProfileConfig
import id.nkz.nokontzzzmanager.data.model.GpuProfileConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AppProfileEntityTest {

    private fun entity(
        cpuConfigJson: String? = null,
        gpuConfigJson: String? = null
    ) = AppProfileEntity(
        packageName = "com.test.app",
        appName = "Test App",
        cpuConfigJson = cpuConfigJson,
        gpuConfigJson = gpuConfigJson
    )

    // --- getCpuConfig ---

    @Test
    fun `getCpuConfig returns default when cpuConfigJson is null`() {
        val config = entity().getCpuConfig()
        assertEquals(CpuProfileConfig(), config)
    }

    @Test
    fun `getCpuConfig returns default when cpuConfigJson is blank`() {
        val config = entity(cpuConfigJson = "  ").getCpuConfig()
        assertEquals(CpuProfileConfig(), config)
    }

    @Test
    fun `getCpuConfig returns default on malformed JSON`() {
        val config = entity(cpuConfigJson = "{not valid json}").getCpuConfig()
        assertEquals(CpuProfileConfig(), config)
    }

    @Test
    fun `getCpuConfig deserializes valid JSON correctly`() {
        val expected = CpuProfileConfig(
            clusterConfigs = mapOf(
                "cpu0" to ClusterConfig(governor = "schedutil", minFreq = 300000, maxFreq = 1800000)
            ),
            coreOnlineStatus = mapOf(0 to true, 1 to false)
        )
        val json = Json.encodeToString(expected)
        val actual = entity(cpuConfigJson = json).getCpuConfig()
        assertEquals(expected, actual)
    }

    @Test
    fun `getCpuConfig handles empty cluster maps`() {
        val expected = CpuProfileConfig()
        val json = Json.encodeToString(expected)
        val actual = entity(cpuConfigJson = json).getCpuConfig()
        assertEquals(expected, actual)
    }

    // --- getGpuConfig ---

    @Test
    fun `getGpuConfig returns default when gpuConfigJson is null`() {
        val config = entity().getGpuConfig()
        assertEquals(GpuProfileConfig(), config)
    }

    @Test
    fun `getGpuConfig returns default when gpuConfigJson is blank`() {
        val config = entity(gpuConfigJson = "").getGpuConfig()
        assertEquals(GpuProfileConfig(), config)
    }

    @Test
    fun `getGpuConfig returns default on malformed JSON`() {
        val config = entity(gpuConfigJson = "null_not_object").getGpuConfig()
        assertEquals(GpuProfileConfig(), config)
    }

    @Test
    fun `getGpuConfig deserializes valid JSON correctly`() {
        val expected = GpuProfileConfig(
            governor = "msm-adreno-tz",
            minFreq = 200000000,
            maxFreq = 587000000,
            powerLevel = 4,
            throttlingEnabled = true
        )
        val json = Json.encodeToString(expected)
        val actual = entity(gpuConfigJson = json).getGpuConfig()
        assertEquals(expected, actual)
    }

    @Test
    fun `getGpuConfig handles null fields in JSON`() {
        val expected = GpuProfileConfig(governor = "performance")
        val json = Json.encodeToString(expected)
        val actual = entity(gpuConfigJson = json).getGpuConfig()
        assertEquals(expected, actual)
        assertNull(actual.minFreq)
        assertNull(actual.maxFreq)
    }
}
