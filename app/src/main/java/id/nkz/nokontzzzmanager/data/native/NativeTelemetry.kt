package id.nkz.nokontzzzmanager.data.native

object NativeTelemetry {
    private val loaded: Boolean = try {
        System.loadLibrary("nkm_telemetry")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    } catch (_: SecurityException) {
        false
    }

    fun readSnapshotJson(): String? {
        if (!loaded) return null
        return try {
            readSnapshotJsonNative()
        } catch (_: UnsatisfiedLinkError) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private external fun readSnapshotJsonNative(): String?
}
