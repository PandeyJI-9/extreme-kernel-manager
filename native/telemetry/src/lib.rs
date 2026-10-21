use jni::objects::JClass;
use jni::sys::jstring;
use jni::JNIEnv;

mod paths;
mod readers;
mod snapshot;

const FIXED_SNAPSHOT_JSON: &str =
    r#"{"schemaVersion":1,"nativeAvailable":true,"cpu":[],"gpu":null,"thermal":[],"zram":null,"battery":null,"errors":[]}"#;

fn fixed_snapshot_json() -> &'static str {
    FIXED_SNAPSHOT_JSON
}

fn snapshot_json() -> String {
    serde_json::to_string(&snapshot::read_snapshot()).unwrap_or_else(|_| fixed_snapshot_json().to_owned())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_id_nkz_nokontzzzmanager_data_native_NativeTelemetry_readSnapshotJsonNative(
    env: JNIEnv,
    _: JClass,
) -> jstring {
    std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        env.new_string(snapshot_json())
            .map(|snapshot| snapshot.into_raw())
            .unwrap_or(std::ptr::null_mut())
    }))
    .unwrap_or(std::ptr::null_mut())
}

#[cfg(test)]
mod tests {
    #[test]
    fn fixed_snapshot_uses_the_initial_schema() {
        let snapshot = super::fixed_snapshot_json();

        assert_eq!(
            snapshot,
            r#"{"schemaVersion":1,"nativeAvailable":true,"cpu":[],"gpu":null,"thermal":[],"zram":null,"battery":null,"errors":[]}"#,
        );
    }
}
