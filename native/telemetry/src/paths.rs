pub const CPU_BASE: &str = "/sys/devices/system/cpu";
pub const PROC_MEMINFO: &str = "/proc/meminfo";
pub const PROC_SWAPS: &str = "/proc/swaps";

pub const GPU_CURRENT_FREQ_PATHS: &[&str] = &[
    "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
    "/sys/class/kgsl/kgsl-3d0/gpuclk",
];

pub const GPU_MAX_FREQ_PATHS: &[&str] = &[
    "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq",
    "/sys/class/devfreq/1c00000.gpu/max_freq",
];

pub const GPU_USAGE_PATHS: &[&str] = &[
    "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
    "/sys/class/kgsl/kgsl-3d0/usage",
    "/sys/class/devfreq/1c00000.gpu/device/gpu_busy_percentage",
    "/sys/kernel/gpu/gpu_busy",
];

pub const ZRAM_DISKSIZE: &str = "/sys/block/zram0/disksize";
pub const ZRAM_MM_STAT: &str = "/sys/block/zram0/mm_stat";

pub const BATTERY_BASE: &str = "/sys/class/power_supply/battery";
