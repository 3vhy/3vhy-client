use std::{env, fs, path::PathBuf};

// The real Eventide installer (built by `tauri build --bundles nsis`) is
// embedded into the setup app. CI copies it to `payload/eventide-installer.exe`
// before building this crate. Without it (e.g. a local `cargo check`), an empty
// payload is embedded and the setup app reports that it was built without one.
fn main() {
    let payload = PathBuf::from("payload/eventide-installer.exe");
    let out =
        PathBuf::from(env::var_os("OUT_DIR").unwrap()).join("payload.exe");
    println!("cargo::rerun-if-changed=payload");
    if payload.is_file() {
        fs::copy(&payload, &out).expect("failed to copy installer payload");
    } else {
        println!(
            "cargo::warning=No installer payload at apps/setup/payload/eventide-installer.exe; the setup app will not be able to install"
        );
        fs::write(&out, b"").unwrap();
    }

    tauri_build::build();
}
