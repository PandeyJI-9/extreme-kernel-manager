package id.nkz.nokontzzzmanager.utils

object KernelPaths {
    // KGSL Skip Zeroing paths
    val KGSL_SKIP_ZEROING = listOf(
        "/sys/kernel/extreme_attributes/kgsl_skip_zeroing",
        "/sys/kernel/e404/kgsl_skip_zeroing",
        "/sys/kernel/lunar_attributes/kgsl_skip_zeroing",
        "/sys/kernel/lunar_attributes/lunar_kgsl_skip_zeroing",
        "/sys/kernel/n0kz_attributes/kgsl_skip_zeroing",
        "/sys/kernel/n0kz_attributes/n0kz_kgsl_skip_zeroing",
        "/sys/kernel/fusionx_attributes/fusionx_kgsl_skip_zeroing",
        "/sys/kernel/fusionx_attributes/kgsl_skip_zeroing"
    )

    // Avoid Dirty PTE paths
    val AVOID_DIRTY_PTE = listOf(
        "/sys/kernel/extreme_attributes/avoid_dirty_pte",
        "/sys/kernel/n0kz_attributes/avoid_dirty_pte",
        "/sys/kernel/e404/avoid_dirty_pte"
    )

    // Background App Blocker paths
    val BG_BLOCKLIST = listOf(
        "/sys/kernel/n0kz_attributes/bg_blocklist",
        "/sys/kernel/e404/bg_blocklist"
    )

    // Bypass Charging path
    const val BYPASS_CHARGING = "/sys/class/power_supply/battery/input_suspend"

    // Force Fast Charge path
    const val FORCE_FAST_CHARGE = "/sys/kernel/fast_charge/force_fast_charge"

    // GPU Throttling paths
    val GPU_THROTTLING = listOf(
        "/sys/class/kgsl/kgsl-3d0/throttling",
        "/sys/class/kgsl/kgsl-3d0/devfreq/throttling",
        "/sys/kernel/gpu/gpu_throttling"
    )
}
