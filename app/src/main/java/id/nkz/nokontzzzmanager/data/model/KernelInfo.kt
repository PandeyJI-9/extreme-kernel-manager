package id.nkz.nokontzzzmanager.data.model
import kotlinx.serialization.Serializable

@Serializable
data class KernelInfo(
    val version: String,
    val gkiType: String,
    val scheduler: String,
    val selinuxStatus: String = "Unknown",
    val abi: String = "Unknown",
    val architecture: String = "Unknown",
    val kernelSuStatus: String = "Not Detected",
    val fingerprint: String = "Unknown",
    val wireguardVersion: String? = null
)