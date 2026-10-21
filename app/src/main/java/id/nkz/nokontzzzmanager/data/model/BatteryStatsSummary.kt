package id.nkz.nokontzzzmanager.data.model

data class BatteryStatsSummary(
    val avgChargeCurrent: Double = 0.0,
    val maxChargeCurrent: Float = 0f,
    val avgChargeTemp: Double = 0.0,
    val maxChargeTemp: Float = 0f,

    val avgDischargeCurrent: Double = 0.0,
    val maxDischargeCurrent: Float = 0f,
    val avgDischargeTemp: Double = 0.0,
    val maxDischargeTemp: Float = 0f,

    val activeDrainRate: Double = 0.0,
    val idleDrainRate: Double = 0.0,

    val totalDischargeTimeMs: Long = 0L,
    val screenOnTimeMs: Long = 0L,
    val screenOffTimeMs: Long = 0L,
    val totalAwakeMs: Long = 0L,
    val totalDeepSleepMs: Long = 0L,

    val totalDischargeMah: Double = 0.0,
    val screenOnMah: Double = 0.0,
    val screenOffMah: Double = 0.0,

    val chargeStartLevel: Int = 0,
    val chargeEndLevel: Int = 0,
    val chargeDurationMs: Long = 0L,

    val isSyncedWithMonitor: Boolean = false
)
