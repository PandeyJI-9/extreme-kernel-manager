package id.nkz.nokontzzzmanager.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class DeepSleepInfo(
    val uptime: Long,       // ms
    val deepSleep: Long     // ms
)