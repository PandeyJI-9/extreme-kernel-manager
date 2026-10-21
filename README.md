<div align="center">

# ⚡ Extreme+ Kernel Manager (EKM) ⚡

### *The High-Performance Hardware Companion for POCO F4 (`munch` / SM8250)*
**Engineered Exclusively for EXTREME++ Kernel by PandeyJi**

---

[![Target Device](https://img.shields.io/badge/Device-POCO_F4_(munch)-FF6900?style=for-the-badge&logo=xiaomi&logoColor=white)](https://t.me/Extremeplus_Support)
[![Kernel Target](https://img.shields.io/badge/Kernel-EXTREME++_4.19-E50914?style=for-the-badge&logo=linux&logoColor=white)](https://github.com/PandeyJI-9/extreme-kernel-manager)
[![Platform](https://img.shields.io/badge/Platform-Android_13--15-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/PandeyJI-9/extreme-kernel-manager)
[![Language](https://img.shields.io/badge/Language-Kotlin_2.4-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![UI Engine](https://img.shields.io/badge/UI-Jetpack_Compose_MD3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

[![Support Group](https://img.shields.io/badge/Telegram-Extreme+_Support-2CA5E0?style=flat-square&logo=telegram&logoColor=white)](https://t.me/Extremeplus_Support)
[![Lead Developer](https://img.shields.io/badge/Dev-@PandeyJi__8-0088cc?style=flat-square&logo=telegram&logoColor=white)](https://t.me/PandeyJi_8)
[![GitHub](https://img.shields.io/badge/GitHub-PandeyJI--9-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/PandeyJI-9)

</div>

---

## 📖 About EKM

**Extreme+ Kernel Manager (EKM)** is an advanced, ultra-responsive kernel control and monitoring suite built natively for the **POCO F4 (Snapdragon 870 / SM8250)**. 

Designed specifically to tap into the low-level silicon potential of **EXTREME++ Kernel**, EKM bridges kernel-space optimizations (`kgsl_skip_zeroing`, `avoid_dirty_pte`, custom governor rate clamping) with a buttery-smooth Material 3 Expressive UI.

---

## 📱 Hardware Architecture (POCO F4 / Munch)

| Subsystem | Hardware Specification | EKM Integration |
| :--- | :--- | :--- |
| **SoC** | Qualcomm Snapdragon 870 (SM8250-AC) | Direct per-cluster clock scaling |
| **CPU Cluster** | 1x Prime (3.2GHz) + 3x Gold (2.42GHz) + 4x Little (1.8GHz) | Tri-Profile Dynamic Engine |
| **GPU** | Adreno 650 (670 MHz Peak) | Power level throttling & zero-copy bypass |
| **Display** | 6.67" E4 AMOLED (120Hz, HDR10+) | Hardware DSI / DRM DC Dimming |
| **Memory** | LPDDR5 + UFS 3.1 | L3 / LLCC bus lock & dirty PTE bypass |

---

## 🚀 Key Features

### ⚡ 1. Tri-Profile Dynamic Engine
Switch between tailored thermal & frequency profiles instantly without reboots:

| Setting | 🔋 Battery Saver | ⚖️ Balanced Cruise | 🔥 Apex Turbo Gaming |
| :--- | :--- | :--- | :--- |
| **Prime Core 7** | Clamped at **2.42 GHz** | Locked at **2.84 GHz** sweet-spot | Full **3.19 GHz** uncapped |
| **Gold Cores 4-6** | Max **2.25 GHz** | Max **2.42 GHz** | Max **2.42 GHz** |
| **Little Cores 0-3** | 576 MHz - 1.80 GHz | 576 MHz - 1.80 GHz | 576 MHz - 1.80 GHz |
| **Adreno 650 GPU** | Level 2 (490 MHz) | Level 1 (587 MHz) | Level 0 (**670 MHz Peak**) |
| **Governor Pacing** | Smooth decay (10,000us) | 120 FPS Cruise (500us/20,000us) | Zero-latency instant burst |
| **L3 Cache Bus** | Dynamic downclock | Stock scaling | **High-priority bus lock** |

> **Persistence**: Active profiles automatically sync to `/data/adb/extreme/profile.conf` and re-apply cleanly via KernelSU / Magisk / APatch `service.d` boot hooks.

---

### 🌙 2. Hardware DC Dimming & QS Tile
* Directly writes to native Munch display registers (`/sys/devices/platform/soc/soc:qcom,dsi-display-primary/msm_fb_ea_enable` or `card0-DSI-1/disp_param`).
* Completely stops OLED pulse-width modulation (PWM) flicker at low brightness.
* Includes a native Android Quick Settings (QS) Tile for instant notification shade toggling.

---

### 📊 3. Live Telemetry & Floating Overlay
* **Real-Time Frequency Gauges**: Per-core Little, Gold, and Prime clock load bars.
* **Adreno 650 Monitor**: GPU frequency, current power level, and thermal load.
* **Battery Diagnostics**: Live current draw (mA), battery voltage (mV), wattage, and temperature.
* **In-Game FPS Meter**: Floating draggable overlay tracking real-time FPS and frame times.

---

### 🔐 4. Kang-Proof Hardware Handshake
EKM verifies authentic **EXTREME++** builds via an unforgeable kernel-space token:
* Checks `/sys/kernel/extreme_attributes/magic`.
* Even if another developer renames the kernel string in `uname -r`, EKM detects the internal compiled kernel signature and unlocks full hardware features.
* Foreign / stock kernels are strictly prevented from running EKM.

---

## 🛠️ Kernel Patching Guide (Quick & Concise)

To enable the unforgeable handshake and GPU optimizations in your EXTREME++ kernel tree:

### Step 1: Copy Patch to Kernel Root
The patch file is located in this repository at:
[`kernel-patches/extreme_kernel_handshake.patch`](kernel-patches/extreme_kernel_handshake.patch)

```bash
cp /path/to/extreme-kernel-manager/kernel-patches/extreme_kernel_handshake.patch /path/to/your/kernel/source/
cd /path/to/your/kernel/source/
```

### Step 2: Apply the Patch
```bash
# Verify no conflicts:
git apply --check extreme_kernel_handshake.patch

# Apply cleanly:
git apply extreme_kernel_handshake.patch
```

### Step 3: Compile & Verify
Compile your kernel and flash to POCO F4. After boot, verify in terminal:
```bash
cat /sys/kernel/extreme_attributes/magic
# Expected output: EXTREME++_BY_PANDEYJI_SM8250_AUTHENTIC
```
*That's it! EKM will instantly authenticate and unlock all hardware controls.*

---

## 🏗️ Automated CI/CD Compilation

This repository uses **GitHub Actions** (`.github/workflows/build_apk.yml`):
* **Automatic Builds**: On every push to `main` or release tag.
* **Auto-Sign Engine**: Releases are automatically signed with production keys.
* **Telegram Notification**: Pushes signed APKs directly to Telegram channels.

---

## 👑 Credits & Community

* **Lead Architect & Developer**: **PandeyJi**
  * Telegram: [@PandeyJi_8](https://t.me/PandeyJi_8)
  * GitHub: [@PandeyJI-9](https://github.com/PandeyJI-9)
* **Official Support Group**: [https://t.me/Extremeplus_Support](https://t.me/Extremeplus_Support)
  * Join for feature suggestions, bug reports, and kernel update announcements.
* **Open Source Acknowledgments**:
  * [N0Kontzzz Kernel Manager](https://github.com/bimoalfarrabi/N0Kontzzz-Kernel-Manager) by Bimo Al-Farrabi
  * [libsu](https://github.com/topjohnwu/libsu) by John Wu
  * [XKM](https://github.com/Gustyx-Power/Xtra-Kernel-Manager) by Gustyx-Power
