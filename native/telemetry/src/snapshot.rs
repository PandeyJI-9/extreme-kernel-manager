use crate::paths;
use crate::readers::{parse_first_i64, parse_meminfo_bytes, read_i64, read_trimmed};
use serde::Serialize;
use std::fs;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TelemetrySnapshot {
    schema_version: i32,
    native_available: bool,
    cpu: Vec<CpuTelemetrySnapshot>,
    gpu: Option<GpuTelemetrySnapshot>,
    thermal: Vec<ThermalTelemetrySnapshot>,
    zram: Option<ZramTelemetrySnapshot>,
    battery: Option<BatteryTelemetrySnapshot>,
    errors: Vec<String>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct CpuTelemetrySnapshot {
    core: usize,
    online: Option<bool>,
    current_freq_khz: Option<i64>,
    min_freq_khz: Option<i64>,
    max_freq_khz: Option<i64>,
    governor: Option<String>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct GpuTelemetrySnapshot {
    current_freq_hz: Option<i64>,
    max_freq_hz: Option<i64>,
    usage_percent: Option<i64>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct ThermalTelemetrySnapshot {
    zone: String,
    #[serde(rename = "type")]
    thermal_type: Option<String>,
    temp_milli_celsius: Option<i64>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct ZramTelemetrySnapshot {
    disksize_bytes: Option<i64>,
    used_bytes: Option<i64>,
    swap_total_bytes: Option<i64>,
    swap_used_bytes: Option<i64>,
    mem_total_bytes: Option<i64>,
    mem_available_bytes: Option<i64>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct BatteryTelemetrySnapshot {
    level_percent: Option<i64>,
    temp_deci_celsius: Option<i64>,
    voltage_microvolts: Option<i64>,
    current_microamps: Option<i64>,
    status: Option<String>,
}

pub fn read_snapshot() -> TelemetrySnapshot {
    let mut errors = Vec::new();
    let cpu = read_cpu(&mut errors);
    let gpu = read_gpu(&mut errors);
    let thermal = read_thermal(&mut errors);
    let zram = read_zram(&mut errors);
    let battery = read_battery(&mut errors);

    TelemetrySnapshot {
        schema_version: 1,
        native_available: true,
        cpu,
        gpu,
        thermal,
        zram,
        battery,
        errors,
    }
}

fn read_cpu(errors: &mut Vec<String>) -> Vec<CpuTelemetrySnapshot> {
    let mut cpu = Vec::new();
    let Ok(entries) = fs::read_dir(paths::CPU_BASE) else {
        errors.push(format!("missing:{}", paths::CPU_BASE));
        return cpu;
    };

    for entry in entries.flatten() {
        let name = entry.file_name();
        let name = name.to_string_lossy();
        let Some(core) = name.strip_prefix("cpu").and_then(|value| value.parse::<usize>().ok()) else {
            continue;
        };
        let base = format!("{}/{name}", paths::CPU_BASE);
        let online = read_trimmed(&format!("{base}/online")).map(|value| value != "0");
        let freq_base = format!("{base}/cpufreq");
        cpu.push(CpuTelemetrySnapshot {
            core,
            online,
            current_freq_khz: read_i64(&format!("{freq_base}/scaling_cur_freq")),
            min_freq_khz: read_i64(&format!("{freq_base}/scaling_min_freq")),
            max_freq_khz: read_i64(&format!("{freq_base}/scaling_max_freq")),
            governor: read_trimmed(&format!("{freq_base}/scaling_governor")),
        });
    }

    cpu.sort_by_key(|entry| entry.core);
    cpu
}

fn read_gpu(errors: &mut Vec<String>) -> Option<GpuTelemetrySnapshot> {
    let current_freq_hz = first_i64(paths::GPU_CURRENT_FREQ_PATHS);
    let max_freq_hz = first_i64(paths::GPU_MAX_FREQ_PATHS);
    let usage_percent = first_i64(paths::GPU_USAGE_PATHS).map(|value| value.clamp(0, 100));
    if current_freq_hz.is_none() && max_freq_hz.is_none() && usage_percent.is_none() {
        errors.push("missing:gpu".to_owned());
        return None;
    }
    Some(GpuTelemetrySnapshot {
        current_freq_hz,
        max_freq_hz,
        usage_percent,
    })
}

fn read_thermal(errors: &mut Vec<String>) -> Vec<ThermalTelemetrySnapshot> {
    let mut thermal = Vec::new();
    let thermal_base = "/sys/class/thermal";
    let Ok(entries) = fs::read_dir(thermal_base) else {
        errors.push(format!("missing:{thermal_base}"));
        return thermal;
    };

    for entry in entries.flatten() {
        let name = entry.file_name().to_string_lossy().into_owned();
        if !name.starts_with("thermal_zone") {
            continue;
        }
        let zone_path = format!("{thermal_base}/{name}");
        thermal.push(ThermalTelemetrySnapshot {
            zone: name,
            thermal_type: read_trimmed(&format!("{zone_path}/type")),
            temp_milli_celsius: read_i64(&format!("{zone_path}/temp")),
        });
    }
    thermal.sort_by(|a, b| a.zone.cmp(&b.zone));
    thermal
}

fn read_zram(errors: &mut Vec<String>) -> Option<ZramTelemetrySnapshot> {
    let meminfo = fs::read_to_string(paths::PROC_MEMINFO)
        .ok()
        .map(|content| parse_meminfo_bytes(&content));
    let swap_from_proc = fs::read_to_string(paths::PROC_SWAPS).ok().and_then(|content| parse_proc_swaps(&content));
    let used_bytes = read_trimmed(paths::ZRAM_MM_STAT)
        .and_then(|value| value.split_whitespace().nth(2).and_then(parse_first_i64));

    let disksize_bytes = read_i64(paths::ZRAM_DISKSIZE);
    if meminfo.is_none() && swap_from_proc.is_none() && used_bytes.is_none() && disksize_bytes.is_none() {
        errors.push("missing:zram".to_owned());
        return None;
    }
    let parsed = meminfo.unwrap_or_default();
    let (swap_total_bytes, swap_used_bytes) = swap_from_proc
        .map_or((parsed.swap_total_bytes, parsed.swap_used_bytes), |swap| (Some(swap.0), Some(swap.1)));
    Some(ZramTelemetrySnapshot {
        disksize_bytes,
        used_bytes,
        swap_total_bytes,
        swap_used_bytes,
        mem_total_bytes: parsed.mem_total_bytes,
        mem_available_bytes: parsed.mem_available_bytes,
    })
}

fn read_battery(errors: &mut Vec<String>) -> Option<BatteryTelemetrySnapshot> {
    let level_percent = read_i64(&format!("{}/capacity", paths::BATTERY_BASE));
    let temp_deci_celsius = read_i64(&format!("{}/temp", paths::BATTERY_BASE));
    let voltage_microvolts = first_i64(&[
        &format!("{}/voltage_now", paths::BATTERY_BASE),
        &format!("{}/batt_voltage_now", paths::BATTERY_BASE),
    ]);
    let current_microamps = first_i64(&[
        &format!("{}/current_now", paths::BATTERY_BASE),
        &format!("{}/current_avg", paths::BATTERY_BASE),
    ]);
    let status = read_trimmed(&format!("{}/status", paths::BATTERY_BASE));

    if level_percent.is_none()
        && temp_deci_celsius.is_none()
        && voltage_microvolts.is_none()
        && current_microamps.is_none()
        && status.is_none()
    {
        errors.push("missing:battery".to_owned());
        return None;
    }

    Some(BatteryTelemetrySnapshot {
        level_percent,
        temp_deci_celsius,
        voltage_microvolts,
        current_microamps,
        status,
    })
}

fn first_i64(paths: &[&str]) -> Option<i64> {
    paths.iter().find_map(|path| read_i64(path))
}

fn parse_proc_swaps(content: &str) -> Option<(i64, i64)> {
    let mut total = 0_i64;
    let mut used = 0_i64;
    for fields in content.lines().skip(1).map(|line| line.split_whitespace().collect::<Vec<_>>()) {
        if fields.len() < 4 {
            continue;
        }
        total += fields[2].parse::<i64>().unwrap_or(0) * 1024;
        used += fields[3].parse::<i64>().unwrap_or(0) * 1024;
    }
    (total > 0 || used > 0).then_some((total, used))
}
