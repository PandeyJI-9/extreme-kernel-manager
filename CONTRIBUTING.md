# Contributing to N0Kontzzz Kernel Manager

Thanks for taking the time to contribute. This document covers everything you need to get started.

---

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (`JAVA_HOME=/opt/android-studio/jbr` or equivalent)
- Android NDK 30.x installed via SDK Manager
- Rust toolchain via `rustup` with the `aarch64-linux-android` target:
  ```bash
  rustup target add aarch64-linux-android
  ```
- A rooted Poco F4 (munch) for on-device testing, or a rooted emulator for non-root-dependent features

---

## Project Structure

```
app/
  src/main/
    java/id/nkz/nokontzzzmanager/
      data/           # Repositories, data sources, DTOs
      di/             # Hilt modules
      domain/         # Use cases and domain models
      service/        # Foreground services (Battery, Thermal, etc.)
      ui/             # Compose screens and ViewModels
    jniLibs/arm64-v8a/
      libnkm_telemetry.so   # Prebuilt Rust telemetry library
native/
  telemetry/          # Rust crate — JNI entrypoint + sysfs readers
```

---

## Building

### Kotlin / Android

```bash
./gradlew assembleDebug
```

### Rust native library (only needed if you change `native/telemetry/`)

```bash
cd native/telemetry
cargo build --release --target aarch64-linux-android
cp target/aarch64-linux-android/release/libnkm_telemetry.so \
   ../../app/src/main/jniLibs/arm64-v8a/
```

Commit the updated `.so` alongside your Rust changes.

---

## Running Tests

```bash
# Kotlin unit tests
./gradlew test

# Rust unit tests (host target, no device needed)
cd native/telemetry && cargo test
```

---

## Code Style

**Kotlin**
- Follow the [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide)
- MVVM: ViewModels observe `StateFlow`/`SharedFlow`, screens are stateless Composables
- Use `Dispatchers.IO` for all I/O; never block `Dispatchers.Main`
- Hilt for all DI — no manual service locators
- Room for persistence — no raw SQLite

**Rust**
- `rustfmt` before committing (`cargo fmt`)
- `cargo clippy -- -D warnings` must pass clean
- Keep JNI functions in `src/lib.rs`; business logic in separate modules
- Use `env_logger` / `android_logger` for logging, not `eprintln!`

---

## Architecture Notes

- **Native-first telemetry**: `NativeTelemetryReader` calls the Rust JNI layer for world-readable sysfs paths. The Kotlin fallback (`*Provider` classes) handles root-only paths via `libsu`.
- **Rollout guard**: `NativeTelemetryReader` tracks a success counter; if native reads consistently return nulls the caller falls back automatically. Don't bypass this guard.
- **Root operations**: All `libsu` calls go through the `SystemRepository`. Don't shell out from ViewModels.
- **Services**: `BatteryMonitorService`, `ThermalMonitorService` are foreground services. Keep their work on `Dispatchers.IO` — no Binder IPC on the main thread.

---

## Submitting a Pull Request

1. Fork the repo and create a branch from `main`:
   ```bash
   git checkout -b feat/your-feature
   ```
2. Make your changes, keeping commits focused. One logical change per commit.
3. Run tests and confirm the build is green before opening a PR.
4. Open a PR against `main` with:
   - A clear title (under 70 chars)
   - What changed and why
   - How you tested it (logcat output, device model, kernel version if relevant)
5. If your change touches a sysfs path or root operation, note which kernel(s) you tested on.

---

## Reporting Bugs

Open an [issue](https://github.com/bimoalfarrabi/N0Kontzzz-Kernel-Manager/issues) with:

- Device model and kernel version
- App version
- Steps to reproduce
- Relevant `adb logcat` output (filter: `NKM`, `NativeTelemetry`, or the relevant tag)

---

## What's in Scope

Good candidates for contributions:

- New sysfs path candidates in `native/telemetry/src/paths.rs`
- Support for additional Kona kernels
- UI improvements (Material 3, accessibility, localization)
- Additional languages beyond English and Indonesian
- Performance and battery analytics improvements
- Bug fixes backed by a reproduction case

Out of scope for now:

- Support for non-Kona (SM8250) devices
- Breaking changes to existing backup/restore format without a migration path

---

## Questions

Reach out on the [Telegram group](https://t.me/n0kontzzz) before starting large changes — saves everyone time.
