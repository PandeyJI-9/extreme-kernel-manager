package id.nkz.nokontzzzmanager.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import id.nkz.nokontzzzmanager.data.model.BatteryInfo
import id.nkz.nokontzzzmanager.data.model.DeepSleepInfo
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryMonitorProvider @Inject constructor(
    private val context: Context,
    private val sysfsHelper: SysfsHelper,
    private val kernelFeatures: KernelFeatureRepository,
    private val nativeTelemetryReader: NativeTelemetryReader,
) {
    private fun getBatteryLevelFromApi(): Int {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatusIntent = context.applicationContext.registerReceiver(null, intentFilter) ?: return -1
        val level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level != -1 && scale != -1 && scale != 0) (level / scale.toFloat() * 100).toInt() else -1
    }

    private suspend fun getBatteryInfoInternal(statusFromIntent: Int = -1): BatteryInfo {
        val nativeBattery = nativeTelemetryReader.readSnapshot()?.battery
        val batteryDir = "/sys/class/power_supply/battery"
        val batteryLevelStr = nativeBattery?.levelPercent?.takeIf { it in 0..100 }?.toString()
            ?: sysfsHelper.readFileToString("$batteryDir/capacity", "Battery Level Percent from File")
        val finalLevel = batteryLevelStr?.toIntOrNull() ?: getBatteryLevelFromApi().let { if (it == -1) 0 else it }

        var tempStr = nativeBattery?.tempDeciCelsius?.toString()
            ?: sysfsHelper.readFileToString("$batteryDir/temp", "Battery Temperature")
        var tempSource = "$batteryDir/temp"
        if (tempStr == null) {
            val thermalZoneDirs = File("/sys/class/thermal/").listFiles { dir, name ->
                dir.isDirectory && name.startsWith("thermal_zone")
            }
            thermalZoneDirs?.sortedBy { it.name }?.forEach thermalLoop@{ zoneDir ->
                val type = sysfsHelper.readFileToString("${zoneDir.path}/type", "Thermal Zone Type (${zoneDir.name})", attemptSu = false)
                if (type != null && (type.contains("battery", ignoreCase = true) || type.contains("แบตเตอรี่") || type.contains("case_therm", ignoreCase = true) || type.contains("ibat_therm", ignoreCase = true))) {
                    tempStr = sysfsHelper.readFileToString("${zoneDir.path}/temp", "Battery Temperature from ${zoneDir.name} ($type)")
                    if (tempStr != null) {
                        tempSource = "${zoneDir.path}/temp (type: $type)"
                        return@thermalLoop
                    }
                }
            }
        }
        val finalTemperature = tempStr?.toFloatOrNull()?.let { rawTemp ->
            if (rawTemp > 1000 && (tempSource.contains("thermal_zone") || tempSource.contains("temp_input"))) rawTemp / 1000 else rawTemp / 10
        } ?: 0f

        val cycleCountStr = sysfsHelper.readFileToString("$batteryDir/cycle_count", "Battery Cycle Count")
        val finalCycleCount = cycleCountStr?.toIntOrNull() ?: run {
            val altCyclePaths = listOf(
                "/sys/class/power_supply/bms/cycle_count",
                "/sys/class/power_supply/battery/cycle_count_summary",
                "/proc/driver/battery_cycle",
                "/proc/battinfo"
            )
            for (altPath in altCyclePaths) {
                val altCycleStr = sysfsHelper.readFileToString(altPath, "Alternative Battery Cycle Count ($altPath)")
                val cycles = altCycleStr?.toIntOrNull()
                if (cycles != null && cycles > 0) return@run cycles
            }
            0
        }

        val designCapacityUahStr = sysfsHelper.readFileToString("$batteryDir/charge_full_design", "Battery Design Capacity (uAh)")
        val designCapacityUah = designCapacityUahStr?.toLongOrNull() ?: run {
            val altCapacityPaths = listOf(
                "/sys/class/power_supply/bms/charge_full_design",
                "/sys/class/power_supply/battery/energy_full_design",
                "/proc/driver/battery_capacity"
            )
            for (altPath in altCapacityPaths) {
                val altCapStr = sysfsHelper.readFileToString(altPath, "Alternative Battery Design Capacity ($altPath)")
                val cap = altCapStr?.toLongOrNull()
                if (cap != null && cap > 0) return@run cap
            }
            null
        }

        val finalDesignCapacityMah = if (designCapacityUah != null && designCapacityUah > 0) {
            when {
                designCapacityUah > 10000000 -> (designCapacityUah / 1000).toInt()
                designCapacityUah > 10000 -> (designCapacityUah / 1000).toInt()
                else -> designCapacityUah.toInt()
            }
        } else 0

        var calculatedSohPercentage = 0
        var currentCapacityMah = 0

        if (finalDesignCapacityMah > 0 && designCapacityUah != null) {
            val currentFullUahStr = sysfsHelper.readFileToString("$batteryDir/charge_full", "Battery Current Full Capacity (uAh)")
            val currentFullUah = currentFullUahStr?.toLongOrNull() ?: run {
                val altCurrentCapPaths = listOf(
                    "/sys/class/power_supply/bms/charge_full",
                    "/sys/class/power_supply/battery/energy_full",
                    "/proc/driver/battery_current_capacity"
                )
                for (altPath in altCurrentCapPaths) {
                    val altCapStr = sysfsHelper.readFileToString(altPath, "Alternative Battery Current Capacity ($altPath)")
                    val cap = altCapStr?.toLongOrNull()
                    if (cap != null && cap > 0) return@run cap
                }
                null
            }

            if (currentFullUah != null && currentFullUah > 0) {
                currentCapacityMah = when {
                    currentFullUah > 10000000 -> (currentFullUah / 1000).toInt()
                    currentFullUah > 10000 -> (currentFullUah / 1000).toInt()
                    else -> currentFullUah.toInt()
                }
                val sohDouble = (currentCapacityMah.toDouble() / finalDesignCapacityMah.toDouble()) * 100.0
                calculatedSohPercentage = sohDouble.toInt().coerceIn(0, 100)
            } else {
                val healthPercentageStr = sysfsHelper.readFileToString("$batteryDir/health", "Direct Battery Health")
                calculatedSohPercentage = healthPercentageStr?.toIntOrNull()?.coerceIn(0, 100) ?: run {
                    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
                    when (health) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> 100
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> 85
                        BatteryManager.BATTERY_HEALTH_COLD -> 90
                        BatteryManager.BATTERY_HEALTH_DEAD -> 0
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> 75
                        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> 50
                        else -> 100
                    }
                }
                currentCapacityMah = (finalDesignCapacityMah * calculatedSohPercentage / 100.0).toInt()
            }
        } else {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val energyCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            if (energyCounter != Int.MIN_VALUE && energyCounter > 0) {
                val estimatedCapacityMah = (energyCounter / (3.7 * 1000000)).toInt()
                if (estimatedCapacityMah > 0) {
                    currentCapacityMah = estimatedCapacityMah
                    calculatedSohPercentage = 100
                }
            }
        }

        val healthStatus = when {
            calculatedSohPercentage >= 90 -> "Excellent"
            calculatedSohPercentage >= 80 -> "Good"
            calculatedSohPercentage >= 70 -> "Fair"
            calculatedSohPercentage >= 60 -> "Poor"
            calculatedSohPercentage > 0 -> "Critical"
            else -> "Unknown"
        }

        val voltagePaths = listOf(
            "$batteryDir/voltage_now", "$batteryDir/batt_vol", "$batteryDir/batt_voltage",
            "$batteryDir/battery_voltage", "/sys/class/power_supply/battery/voltage_now",
            "/sys/class/power_supply/bms/voltage_now", "/sys/class/power_supply/main/voltage_now",
            "/sys/class/power_supply/pm8921-bms/voltage_now"
        )
        var finalVoltage: Float? = nativeBattery?.voltageMicrovolts?.takeIf { it > 0L }?.toFloat()
        for (path in voltagePaths) {
            if (finalVoltage != null) break
            val voltageStr = sysfsHelper.readFileToString(path, "Battery Voltage from $path")
            if (voltageStr.isNullOrBlank()) continue
            val cleanedVoltage = buildString {
                for (ch in voltageStr) if (ch.isDigit() || ch == '.' || ch == '-') append(ch)
            }
            val voltageValue = cleanedVoltage.toFloatOrNull()
            if (voltageValue != null && voltageValue > 0f) { finalVoltage = voltageValue; break }
        }

        var finalCurrent: Float? = nativeBattery?.currentMicroamps?.toFloat()
        val currentPaths = listOf(
            "$batteryDir/current_now", "$batteryDir/current_avg",
            "/sys/class/power_supply/bms/current_now", "/sys/class/power_supply/usb/current_now"
        )
        for (path in currentPaths) {
            if (finalCurrent != null) break
            val currentStr = sysfsHelper.readFileToString(path, "Battery Current from $path")
            if (currentStr != null) { finalCurrent = currentStr.toFloatOrNull(); break }
        }

        val finalWattage = if (finalVoltage != null && finalCurrent != null) {
            val v = if (finalVoltage > 10_000) finalVoltage / 1_000_000f else finalVoltage / 1_000_000f
            val i = finalCurrent / 1_000_000f
            kotlin.math.abs(v * i)
        } else {
            sysfsHelper.readFileToString("$batteryDir/power_now", "Battery Power Now", attemptSu = false)?.toFloatOrNull()
        }

        val finalTechnology = sysfsHelper.readFileToString("$batteryDir/technology", "Battery Technology")
        val statusString = nativeBattery?.status?.takeIf { it.isNotBlank() }
            ?: sysfsHelper.readFileToString("$batteryDir/status", "Battery Status")

        val isCharging = when {
            statusFromIntent != -1 && statusFromIntent != BatteryManager.BATTERY_STATUS_UNKNOWN ->
                statusFromIntent == BatteryManager.BATTERY_STATUS_CHARGING || statusFromIntent == BatteryManager.BATTERY_STATUS_FULL
            finalCurrent != null -> finalCurrent < -1000f
            else -> statusString?.contains("Charging", ignoreCase = true) == true || statusString?.contains("Full", ignoreCase = true) == true
        }

        val finalStatus = when {
            statusString.isNullOrBlank() -> ""
            statusString.contains("Charging", ignoreCase = true) -> "Charging"
            statusString.contains("Discharging", ignoreCase = true) -> "Discharging"
            statusString.contains("Full", ignoreCase = true) -> "Full"
            else -> statusString
        }

        val displayCurrent = finalCurrent?.let {
            val absCurrent = kotlin.math.abs(it)
            if (isCharging) absCurrent else -absCurrent
        } ?: 0f

        return BatteryInfo(
            level = finalLevel,
            temp = finalTemperature,
            voltage = finalVoltage ?: 0f,
            isCharging = isCharging,
            current = displayCurrent,
            chargingWattage = finalWattage ?: 0f,
            technology = finalTechnology ?: "Unknown",
            health = healthStatus,
            status = finalStatus,
            chargingType = getChargingTypeFromStatus(statusString),
            powerSource = getChargingTypeFromStatus(statusString),
            healthPercentage = calculatedSohPercentage,
            cycleCount = finalCycleCount,
            capacity = finalDesignCapacityMah,
            currentCapacity = currentCapacityMah,
            plugged = 0,
            isBypassActive = kernelFeatures.getBypassCharging()
        )
    }

    private fun getChargingTypeFromStatus(statusString: String?): String {
        return when {
            statusString.isNullOrBlank() -> "Unknown"
            statusString.contains("Charging", ignoreCase = true) -> "Charging"
            statusString.contains("Full", ignoreCase = true) -> "Not Charging"
            statusString.contains("Discharging", ignoreCase = true) -> "Not Charging"
            else -> "Unknown"
        }
    }

    fun getBatteryInfo(): BatteryInfo = runBlocking { getBatteryInfoInternal() }

    suspend fun getBatteryInfoSuspend(statusFromIntent: Int = -1): BatteryInfo = getBatteryInfoInternal(statusFromIntent)

    private fun getUptimeMillisInternal(): Long = android.os.SystemClock.elapsedRealtime()

    private fun getDeepSleepMillisInternal(): Long {
        val uptime = android.os.SystemClock.elapsedRealtime()
        val awakeTime = android.os.SystemClock.uptimeMillis()
        return uptime - awakeTime
    }

    @SuppressLint("DefaultLocale")
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    fun getDeepSleepInfo(): DeepSleepInfo = DeepSleepInfo(getUptimeMillisInternal(), getDeepSleepMillisInternal())

    fun getAwakeTime(): Long = android.os.SystemClock.uptimeMillis()

    fun getUptimeMillis(): Long = getUptimeMillisInternal()

    fun getDeepSleepMillis(): Long = getDeepSleepMillisInternal()
}
