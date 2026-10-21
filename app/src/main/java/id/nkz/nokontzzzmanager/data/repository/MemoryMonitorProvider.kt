package id.nkz.nokontzzzmanager.data.repository

import android.content.Context
import id.nkz.nokontzzzmanager.data.model.MemoryInfo
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryMonitorProvider @Inject constructor(
    private val context: Context,
    private val tuningRepository: TuningRepository,
    private val nativeTelemetryReader: NativeTelemetryReader,
) {
    // ponytail: ZRAM layout values (disksize, swapTotal) are static after boot — cache them.
    // Dynamic values (used, swapUsed) come from native first and only fall back when native misses.
    @Volatile private var cachedZramDisksize: Long = -1L
    @Volatile private var cachedSwapTotal: Long = -1L

    suspend fun getMemoryInfo(): MemoryInfo {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val nativeZram = nativeTelemetryReader.readSnapshot()?.zram

            val zramTotal = nativeZram?.disksizeBytes?.takeIf { it > 0L }
                ?: run {
                    if (cachedZramDisksize < 0L) cachedZramDisksize = tuningRepository.getZramDisksize().firstOrNull() ?: 0L
                    cachedZramDisksize
                }
            val zramUsed = nativeZram?.usedBytes?.takeIf { it > 0L }
                ?: tuningRepository.getZramUsed().firstOrNull() ?: 0L
            val swapTotal = nativeZram?.swapTotalBytes?.takeIf { it > 0L }
                ?: run {
                    if (cachedSwapTotal < 0L) cachedSwapTotal = tuningRepository.getSwapTotal().firstOrNull() ?: 0L
                    cachedSwapTotal
                }
            val swapUsed = nativeZram?.swapUsedBytes?.takeIf { it > 0L }
                ?: tuningRepository.getSwapUsed().firstOrNull() ?: 0L

            MemoryInfo(
                used = memoryInfo.totalMem - memoryInfo.availMem,
                total = memoryInfo.totalMem,
                free = memoryInfo.availMem,
                zramTotal = zramTotal,
                zramUsed = zramUsed,
                swapTotal = swapTotal,
                swapUsed = swapUsed
            )
        } catch (e: Exception) {
            MemoryInfo(0, 0, 0)
        }
    }
}
