package id.nkz.nokontzzzmanager.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GpuProfileConfig(
    val governor: String? = null,
    val minFreq: Int? = null,
    val maxFreq: Int? = null,
    val powerLevel: Int? = null,
    val throttlingEnabled: Boolean? = null
)
