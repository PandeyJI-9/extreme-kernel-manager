package id.nkz.nokontzzzmanager.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class RealtimeCpuInfo(
    val cores: Int,
    val governor: String,
    val freqs: List<Int>,   // MHz
    val temp: Float,        // °C
    val cpuLoadPercentage: Float? = null,
    val soc: String  = "Unknown SOC"
)