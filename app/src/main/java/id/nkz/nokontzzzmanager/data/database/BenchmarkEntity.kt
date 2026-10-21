package id.nkz.nokontzzzmanager.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmarks")
data class BenchmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val durationMs: Long,
    val avgFps: Float,
    val fps1Low: Float,
    val fps01Low: Float,
    val jankCount: Int,
    val bigJankCount: Int,
    val avgCpuUsage: Float = 0f,
    val avgGpuUsage: Float = 0f,
    val avgTemp: Float = 0f,
    val maxTemp: Float = 0f,
    val avgPower: Float = 0f,
    val maxFps: Float = 0f,
    val minFps: Float = 0f,
    val fpsVariance: Float = 0f,
    val frameTimeDataJson: String? = null,
    val cpuUsageDataJson: String? = null,
    val gpuUsageDataJson: String? = null,
    val tempDataJson: String? = null,
    val cpuTempDataJson: String? = null,
    val gpuFreqDataJson: String? = null,
    val cpuFreqLittleDataJson: String? = null,
    val cpuFreqBigDataJson: String? = null,
    val cpuFreqPrimeDataJson: String? = null,
    val batteryPowerDataJson: String? = null,
    val batteryLevelDataJson: String? = null
)
