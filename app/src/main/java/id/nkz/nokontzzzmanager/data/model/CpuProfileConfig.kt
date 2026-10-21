package id.nkz.nokontzzzmanager.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CpuProfileConfig(
    val clusterConfigs: Map<String, ClusterConfig> = emptyMap(),
    val coreOnlineStatus: Map<Int, Boolean> = emptyMap()
)

@Serializable
data class ClusterConfig(
    val governor: String? = null,
    val minFreq: Int? = null,
    val maxFreq: Int? = null
)
