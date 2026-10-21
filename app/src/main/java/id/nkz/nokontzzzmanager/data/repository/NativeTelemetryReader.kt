package id.nkz.nokontzzzmanager.data.repository

import android.util.Log
import id.nkz.nokontzzzmanager.data.native.NativeTelemetry
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeTelemetryReader(
    private val isNativeTelemetryEnabled: () -> Boolean = { nativeTelemetryEnabled },
    private val nativeJsonProvider: () -> String? = NativeTelemetry::readSnapshotJson,
) {
    @Inject constructor() : this({ nativeTelemetryEnabled }, NativeTelemetry::readSnapshotJson)

    private val json = Json { ignoreUnknownKeys = true }

    // ponytail: simple time-based cache — deduplicates JNI+JSON parse across providers
    // within one polling tick. Upgrade to SharedFlow if providers ever need push-based updates.
    @Volatile private var cachedSnapshot: TelemetrySnapshot? = null
    @Volatile private var cacheTimestampMs: Long = 0L

    fun readSnapshot(): TelemetrySnapshot? {
        if (!isNativeTelemetryEnabled()) {
            disabledCount.incrementAndGet()
            return null
        }
        val now = System.currentTimeMillis()
        cachedSnapshot?.takeIf { now - cacheTimestampMs < CACHE_TTL_MS }?.let { return it }

        val rawSnapshot = nativeJsonProvider()
        if (rawSnapshot == null) {
            nativeUnavailableCount.incrementAndGet()
            Log.d(TAG, "native unavailable (so not loaded or returned null)")
            return null
        }
        return try {
            val snapshot = json.decodeFromString<TelemetrySnapshot>(rawSnapshot)
            cachedSnapshot = snapshot
            cacheTimestampMs = now
            successCount.incrementAndGet()
            if (snapshot.hasPartialData()) partialDataCount.incrementAndGet()
            Log.d(TAG, "native ok — cpu=${snapshot.cpu.size} thermal=${snapshot.thermal.size} gpu=${snapshot.gpu != null} zram=${snapshot.zram != null} battery=${snapshot.battery != null}")
            snapshot
        } catch (_: IllegalArgumentException) {
            parseFailureCount.incrementAndGet()
            Log.d(TAG, "native parse failed (IllegalArgumentException)")
            null
        } catch (_: SerializationException) {
            parseFailureCount.incrementAndGet()
            Log.d(TAG, "native parse failed (SerializationException)")
            null
        }
    }

    data class Counters(
        val success: Long,
        val partialData: Long,
        val parseFailure: Long,
        val nativeUnavailable: Long,
        val disabled: Long,
    )

    companion object {
        private const val TAG = "NativeTelemetry"
        private const val CACHE_TTL_MS = 750L

        @Volatile
        var nativeTelemetryEnabled: Boolean = true

        private val successCount = AtomicLong(0)
        private val partialDataCount = AtomicLong(0)
        private val parseFailureCount = AtomicLong(0)
        private val nativeUnavailableCount = AtomicLong(0)
        private val disabledCount = AtomicLong(0)

        fun counters(): Counters = Counters(
            success = successCount.get(),
            partialData = partialDataCount.get(),
            parseFailure = parseFailureCount.get(),
            nativeUnavailable = nativeUnavailableCount.get(),
            disabled = disabledCount.get(),
        )

        fun resetCounters() {
            successCount.set(0)
            partialDataCount.set(0)
            parseFailureCount.set(0)
            nativeUnavailableCount.set(0)
            disabledCount.set(0)
        }
    }
}

private fun TelemetrySnapshot.hasPartialData(): Boolean = errors.isNotEmpty() ||
    cpu.any { entry ->
        entry.currentFreqKhz == null ||
            entry.minFreqKhz == null ||
            entry.maxFreqKhz == null ||
            entry.governor.isNullOrBlank()
    } ||
    gpu?.let { it.currentFreqHz == null || it.maxFreqHz == null || it.usagePercent == null } == true ||
    thermal.any { it.type.isNullOrBlank() || it.tempMilliCelsius == null } ||
    zram?.let {
        it.disksizeBytes == null ||
            it.usedBytes == null ||
            it.swapTotalBytes == null ||
            it.swapUsedBytes == null
    } == true ||
    battery?.let {
        it.levelPercent == null ||
            it.tempDeciCelsius == null ||
            it.voltageMicrovolts == null ||
            it.currentMicroamps == null ||
            it.status.isNullOrBlank()
    } == true
