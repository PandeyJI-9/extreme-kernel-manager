package id.nkz.nokontzzzmanager.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NativeTelemetryReaderTest {
    @org.junit.Before
    fun resetNativeTelemetryState() {
        NativeTelemetryReader.nativeTelemetryEnabled = true
        NativeTelemetryReader.resetCounters()
    }

    @Test
    fun readSnapshotParsesValidNativeJson() {
        val reader = NativeTelemetryReader(
            nativeJsonProvider = {
                """
                {
                  "schemaVersion": 1,
                  "nativeAvailable": true,
                  "cpu": [{"core":0,"online":true,"currentFreqKhz":300000,"minFreqKhz":300000,"maxFreqKhz":2803200,"governor":"schedutil"}],
                  "gpu": {"currentFreqHz":315000000,"maxFreqHz":587000000,"usagePercent":42},
                  "thermal": [{"zone":"thermal_zone0","type":"cpu-therm","tempMilliCelsius":42000}],
                  "zram": {"disksizeBytes":2147483648,"usedBytes":268435456,"swapTotalBytes":2147483648,"swapUsedBytes":134217728,"memTotalBytes":8589934592,"memAvailableBytes":4294967296},
                  "battery": {"levelPercent":80,"tempDeciCelsius":310,"voltageMicrovolts":3900000,"currentMicroamps":-500000,"status":"Discharging"},
                  "errors": []
                }
                """.trimIndent()
            },
        )

        val snapshot = reader.readSnapshot()

        assertNotNull(snapshot)
        requireNotNull(snapshot)
        assertEquals(1, snapshot.schemaVersion)
        assertEquals(true, snapshot.nativeAvailable)
        assertEquals(1, snapshot.cpu.size)
        assertEquals(0, snapshot.cpu.first().core)
        assertEquals(315000000L, snapshot.gpu?.currentFreqHz)
        assertEquals("cpu-therm", snapshot.thermal.first().type)
        assertEquals(2147483648L, snapshot.zram?.disksizeBytes)
        assertEquals(80, snapshot.battery?.levelPercent)
    }

    @Test
    fun readSnapshotReturnsNullForMalformedJson() {
        val reader = NativeTelemetryReader(nativeJsonProvider = { "{" })

        assertNull(reader.readSnapshot())
    }

    @Test
    fun readSnapshotReturnsNullWhenNativeProviderIsUnavailable() {
        val reader = NativeTelemetryReader(nativeJsonProvider = { null })

        assertNull(reader.readSnapshot())
    }

    @Test
    fun readSnapshotParsesThermalTypeFromNativeSchema() {
        val reader = NativeTelemetryReader(
            nativeJsonProvider = {
                """
                {
                  "schemaVersion": 1,
                  "nativeAvailable": true,
                  "thermal": [{"zone":"thermal_zone0","type":"cpu-therm","tempMilliCelsius":42000}]
                }
                """.trimIndent()
            },
        )

        val snapshot = reader.readSnapshot()

        assertNotNull(snapshot)
        requireNotNull(snapshot)
        assertEquals("cpu-therm", snapshot.thermal.first().type)
    }

    @Test
    fun readSnapshotReturnsNullWhenNativeTelemetryIsDisabled() {
        val reader = NativeTelemetryReader(
            isNativeTelemetryEnabled = { false },
            nativeJsonProvider = { error("disabled reader should not call native provider") },
        )

        assertNull(reader.readSnapshot())
        assertEquals(1, NativeTelemetryReader.counters().disabled)
    }

    @Test
    fun readSnapshotTracksSuccessAndPartialCounters() {
        val reader = NativeTelemetryReader(
            nativeJsonProvider = {
                """
                {
                  "schemaVersion": 1,
                  "nativeAvailable": true,
                  "cpu": [{"core":0,"currentFreqKhz":300000}],
                  "errors": ["missing:gpu"]
                }
                """.trimIndent()
            },
        )

        assertNotNull(reader.readSnapshot())

        val counters = NativeTelemetryReader.counters()
        assertEquals(1, counters.success)
        assertEquals(1, counters.partialData)
    }

    @Test
    fun readSnapshotTracksUnavailableAndParseFailureCounters() {
        assertNull(NativeTelemetryReader(nativeJsonProvider = { null }).readSnapshot())
        assertNull(NativeTelemetryReader(nativeJsonProvider = { "{" }).readSnapshot())

        val counters = NativeTelemetryReader.counters()
        assertEquals(1, counters.nativeUnavailable)
        assertEquals(1, counters.parseFailure)
    }
}
