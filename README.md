<h1 align="center">Extreme+ Kernel Manager (EKM)</h1>

<p align="center">
  <b>The Ultimate Gaming Companion for POCO F4 (munch / SM8250) running EXTREME++ Kernel</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-green?style=for-the-badge&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/target-POCO_F4_(munch)-orange?style=for-the-badge" alt="POCO F4">
  <img src="https://img.shields.io/badge/kernel-EXTREME++-red?style=for-the-badge" alt="EXTREME++">
  <img src="https://img.shields.io/badge/language-Kotlin-purple?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-blue?style=for-the-badge&logo=jetpackcompose" alt="Compose">
</p>

<p align="center">
  <a href="https://t.me/Extremeplus_Support"><img src="https://img.shields.io/badge/Telegram-Support_Group-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white" alt="Support Group"></a>
  <a href="https://t.me/PandeyJi_8"><img src="https://img.shields.io/badge/Developer-@PandeyJi__8-0088cc?style=for-the-badge&logo=telegram&logoColor=white" alt="Developer Telegram"></a>
  <a href="https://github.com/pandeyJi_9"><img src="https://img.shields.io/badge/GitHub-pandeyJi__9-181717?style=for-the-badge&logo=github&logoColor=white" alt="Developer GitHub"></a>
</p>

---

## ⚡ Overview
**Extreme+ Kernel Manager (EKM)** is an elite, high-performance Android root companion designed from the ground up for the **EXTREME++ Kernel** by **PandeyJi** on the **POCO F4 (`munch` / Snapdragon 870)**.

EKM replaces clunky kernel interfaces with dynamic profiles, hardware-level unforgeable handshakes, native E4 AMOLED DC Dimming, and real-time gaming telemetry.

---

## 🚀 Key Features

### 1. ⚡ Dynamic Tri-Profile Switcher
Seamlessly switch hardware states on the fly without rebooting:
* **🔋 Battery Saver (2.42 GHz Peak)**: Little up to 1.80GHz, Gold up to 2.25GHz, Prime clamped to 2.42GHz. GPU capped at 490MHz (pwrlevel 2). Smooth 10,000us decay.
* **⚖️ Balanced Cruise (2.84 GHz Sweet Spot)**: Locked to 2.84GHz for locked 120 FPS frame pacing. GPU up to 587MHz (pwrlevel 1).
* **🔥 Apex Turbo Gaming (3.19 GHz Unlocked Monster)**: Full 3.19GHz Prime Core 7, GPU uncapped 670MHz (pwrlevel 0), zero-latency governor slope, L3 cache/bus priority lock.
* **Persistent Boot Execution**: Automatically restored on boot via `/data/adb/service.d/extreme_profile.sh` (KernelSU / Magisk / APatch).

### 2. 🔐 Kang-Proof Hardware Handshake
* Directly verifies `/sys/kernel/extreme_attributes/magic` compiled into the EXTREME++ kernel binary.
* Even if another developer forks or strips names from `uname -r`, EKM validates the internal cryptographic token and unlocks.
* Unauthorized, generic, or stock kernels are blocked from running EKM.

### 3. 🌙 Native E4 AMOLED DC Dimming
* Directly controls Munch hardware registers (`msm_fb_ea_enable` / `disp_param`).
* Eliminates PWM flicker at low brightness.
* Features a Quick Settings (QS) Tile for instant notification shade toggling.

### 4. 📊 Live Gaming Telemetry & Floating Overlay
* Per-core CPU load and cluster clock tracking (Little 0-3, Gold 4-6, Prime 7).
* Adreno 650 GPU clock, load, and temperature stats.
* Floating FPS counter with 1% Lows, frame time tracking, and battery drain rate.

---

## 🛠️ Developer & Community Credits

### 👑 Lead Developer & Kernel Architect
* **PandeyJi**
  * Telegram: [@PandeyJi_8](https://t.me/PandeyJi_8)
  * GitHub: [@pandeyJi_9](https://github.com/pandeyJi_9)

### 💬 Community & Bug Reporting
* **Extreme+ Official Support Group**: [https://t.me/Extremeplus_Support](https://t.me/Extremeplus_Support)
  * Join for bug reports, discussions, and new feature requests.

### 🌟 Open-Source Acknowledgments
* **[N0Kontzzz Kernel Manager](https://github.com/bimoalfarrabi/N0Kontzzz-Kernel-Manager)** by Bimo Al-Farrabi (Viasco)
* **[Xtra Kernel Manager](https://github.com/Gustyx-Power/Xtra-Kernel-Manager)** by Gustyx-Power
* **[RvKernel Manager](https://github.com/Rve27/RvKernel-Manager)** by Radika (Rve27)
* **[libsu](https://github.com/topjohnwu/libsu)** by John Wu

---

## 📦 Building EKM
Compilation is fully automated via **GitHub Actions CI/CD** (`.github/workflows/build_apk.yml`). 
Simply push to GitHub or trigger manual dispatch to produce signed release APKs with automated Telegram and GitHub Release broadcasts.
