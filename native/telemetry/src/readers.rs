use std::fs;
use std::path::Path;

#[derive(Debug, Default, PartialEq, Eq)]
pub struct MeminfoBytes {
    pub mem_total_bytes: Option<i64>,
    pub mem_available_bytes: Option<i64>,
    pub swap_total_bytes: Option<i64>,
    pub swap_used_bytes: Option<i64>,
}

pub fn read_trimmed(path: &str) -> Option<String> {
    fs::read_to_string(Path::new(path))
        .ok()
        .map(|value| value.trim().to_owned())
        .filter(|value| !value.is_empty())
}

pub fn read_i64(path: &str) -> Option<i64> {
    read_trimmed(path).and_then(|value| parse_first_i64(&value))
}

pub fn parse_first_i64(value: &str) -> Option<i64> {
    let mut digits = String::new();
    for ch in value.chars() {
        if ch.is_ascii_digit() || (ch == '-' && digits.is_empty()) {
            digits.push(ch);
        } else if !digits.is_empty() && digits != "-" {
            break;
        }
    }
    digits.parse::<i64>().ok()
}

pub fn parse_meminfo_bytes(meminfo: &str) -> MeminfoBytes {
    let mut parsed = MeminfoBytes::default();
    let mut swap_free_bytes = None;

    for line in meminfo.lines() {
        let Some((key, rest)) = line.split_once(':') else {
            continue;
        };
        let bytes = parse_first_i64(rest).map(|kb| kb * 1024);
        match key {
            "MemTotal" => parsed.mem_total_bytes = bytes,
            "MemAvailable" => parsed.mem_available_bytes = bytes,
            "SwapTotal" => parsed.swap_total_bytes = bytes,
            "SwapFree" => swap_free_bytes = bytes,
            _ => {}
        }
    }

    parsed.swap_used_bytes = match (parsed.swap_total_bytes, swap_free_bytes) {
        (Some(total), Some(free)) => Some((total - free).max(0)),
        _ => None,
    };
    parsed
}

#[cfg(test)]
mod tests {
    #[test]
    fn parse_meminfo_bytes_extracts_ram_and_swap() {
        let meminfo = "MemTotal:        8192000 kB\nMemAvailable:    4096000 kB\nSwapTotal:       2097152 kB\nSwapFree:        1572864 kB\n";

        let parsed = super::parse_meminfo_bytes(meminfo);

        assert_eq!(parsed.mem_total_bytes, Some(8_388_608_000));
        assert_eq!(parsed.mem_available_bytes, Some(4_194_304_000));
        assert_eq!(parsed.swap_total_bytes, Some(2_147_483_648));
        assert_eq!(parsed.swap_used_bytes, Some(536_870_912));
    }

    #[test]
    fn parse_first_i64_accepts_units_and_percent_signs() {
        assert_eq!(super::parse_first_i64("315000000\n"), Some(315000000));
        assert_eq!(super::parse_first_i64("42 %"), Some(42));
        assert_eq!(super::parse_first_i64("-500000 uA"), Some(-500000));
        assert_eq!(super::parse_first_i64("not available"), None);
    }
}
