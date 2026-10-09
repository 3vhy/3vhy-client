//! 3vhy Client Setup: a small, good-looking front end for the real installer.
//!
//! The actual install is done by the NSIS installer that `tauri build` produces
//! for the launcher (it handles files, the uninstaller, Add/Remove Programs and
//! updates). This app embeds that installer, runs it silently with the options
//! picked in the UI, and shows live progress while it works.

#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;
use std::sync::atomic::{AtomicBool, Ordering};
use std::time::Duration;

use serde::Serialize;
use tauri::{AppHandle, Emitter, Manager, WebviewWindow};
use tauri_plugin_dialog::DialogExt;

const PRODUCT_NAME: &str = "3vhy Client";
const MAIN_BINARY: &str = "3vhy Client.exe";

#[allow(clippy::large_include_file)]
static PAYLOAD: &[u8] =
    include_bytes!(concat!(env!("OUT_DIR"), "/payload.exe"));

static INSTALLING: AtomicBool = AtomicBool::new(false);

#[derive(Serialize)]
struct SetupInfo {
    version: String,
    default_dir: String,
    existing: bool,
    size_mb: u64,
    has_payload: bool,
    supported: bool,
}

#[derive(Clone, Serialize)]
struct Progress {
    stage: &'static str,
    percent: f64,
}

fn default_install_dir() -> PathBuf {
    dirs::data_local_dir()
        .unwrap_or_else(|| PathBuf::from("C:\\"))
        .join(PRODUCT_NAME)
}

/// Roughly how much disk space the installed launcher takes. The installer is
/// LZMA-compressed, so the installed files are a few times larger.
fn estimated_size_bytes() -> u64 {
    (PAYLOAD.len() as u64).saturating_mul(5) / 2
}

#[tauri::command]
fn setup_info(app: AppHandle) -> SetupInfo {
    let dir = default_install_dir();
    SetupInfo {
        version: app.package_info().version.to_string(),
        existing: dir.join(MAIN_BINARY).is_file(),
        default_dir: dir.to_string_lossy().into_owned(),
        size_mb: estimated_size_bytes().div_ceil(1024 * 1024).max(1),
        has_payload: !PAYLOAD.is_empty(),
        supported: cfg!(windows),
    }
}

/// Opens a folder picker. A generic folder (like "D:\Games") gets a
/// "3vhy Client" subfolder so the install doesn't spill into it.
#[tauri::command]
async fn pick_folder(app: AppHandle, current: String) -> Option<String> {
    let start = PathBuf::from(&current);
    let start = start.parent().map(Path::to_path_buf).unwrap_or(start);
    let picked = tauri::async_runtime::spawn_blocking(move || {
        app.dialog()
            .file()
            .set_title("Choose where to install 3vhy Client")
            .set_directory(start)
            .blocking_pick_folder()
    })
    .await
    .ok()
    .flatten()?;

    let mut dir = picked.into_path().ok()?;
    if dir.file_name().and_then(|name| name.to_str()) != Some(PRODUCT_NAME) {
        dir.push(PRODUCT_NAME);
    }
    Some(dir.to_string_lossy().into_owned())
}

fn dir_size(path: &Path) -> u64 {
    let Ok(entries) = fs::read_dir(path) else {
        return 0;
    };
    entries
        .flatten()
        .map(|entry| match entry.metadata() {
            Ok(meta) if meta.is_dir() => dir_size(&entry.path()),
            Ok(meta) => meta.len(),
            Err(_) => 0,
        })
        .sum()
}

fn emit(app: &AppHandle, stage: &'static str, percent: f64) {
    let _ = app.emit("setup-progress", Progress { stage, percent });
}

fn start_menu_dir() -> Option<PathBuf> {
    Some(dirs::data_dir()?.join("Microsoft\\Windows\\Start Menu\\Programs"))
}

fn remove_shortcut(dir: Option<PathBuf>) {
    if let Some(dir) = dir {
        let _ = fs::remove_file(dir.join(format!("{PRODUCT_NAME}.lnk")));
    }
}

#[cfg(windows)]
fn installer_command(installer: &Path, dir: &Path) -> Command {
    use std::os::windows::process::CommandExt;
    const CREATE_NO_WINDOW: u32 = 0x0800_0000;

    let mut command = Command::new(installer);
    // NSIS wants `/D=` last and unquoted, even when the path has spaces
    command
        .arg("/S")
        .raw_arg(format!("/D={}", dir.display()))
        .creation_flags(CREATE_NO_WINDOW);
    command
}

#[cfg(not(windows))]
fn installer_command(installer: &Path, dir: &Path) -> Command {
    let mut command = Command::new(installer);
    command.arg("/S").arg(format!("/D={}", dir.display()));
    command
}

fn run_install(
    app: &AppHandle,
    dir: &Path,
    desktop: bool,
    start_menu: bool,
) -> Result<String, String> {
    if !cfg!(windows) {
        return Err("3vhy Client Setup only runs on Windows.".into());
    }
    if PAYLOAD.is_empty() {
        return Err("This copy of the setup was built without the 3vhy Client installer inside it.".into());
    }

    emit(app, "Unpacking", 3.0);
    let temp =
        std::env::temp_dir().join(format!("3vhy-setup-{}", std::process::id()));
    fs::create_dir_all(&temp)
        .map_err(|e| format!("Couldn't create a temporary folder: {e}"))?;
    let installer = temp.join("3vhy-client-installer.exe");
    fs::write(&installer, PAYLOAD)
        .map_err(|e| format!("Couldn't unpack the installer: {e}"))?;
    emit(app, "Installing files", 8.0);

    let mut child = installer_command(&installer, dir)
        .spawn()
        .map_err(|e| format!("Couldn't start the installer: {e}"))?;

    // Progress follows the files landing in the install folder and stops short
    // of 100% until the installer says it is finished. When updating, the folder
    // is already full, so progress eases along with elapsed time instead.
    let expected = estimated_size_bytes().max(1) as f64;
    let updating = dir_size(dir) as f64 > expected * 0.5;
    let started = std::time::Instant::now();
    let status = loop {
        match child.try_wait() {
            Ok(Some(status)) => break status,
            Ok(None) => {
                let fraction = if updating {
                    let secs = started.elapsed().as_secs_f64();
                    secs / (secs + 5.0)
                } else {
                    (dir_size(dir) as f64 / expected).min(1.0)
                };
                let percent = 8.0 + fraction * 82.0;
                emit(
                    app,
                    if updating {
                        "Updating files"
                    } else {
                        "Installing files"
                    },
                    percent,
                );
                std::thread::sleep(Duration::from_millis(150));
            }
            Err(e) => return Err(format!("Lost track of the installer: {e}")),
        }
    };
    let _ = fs::remove_dir_all(&temp);

    if !status.success() {
        return Err(match status.code() {
            Some(2) => "The installation was cancelled.".into(),
            Some(code) => format!(
                "The installer stopped with error code {code}. If 3vhy Client is open, close it and try again."
            ),
            None => "The installer stopped unexpectedly.".into(),
        });
    }

    emit(app, "Creating shortcuts", 94.0);
    // The installer always adds both shortcuts in silent mode; drop the ones
    // that were switched off.
    if !desktop {
        remove_shortcut(dirs::desktop_dir());
    }
    if !start_menu {
        remove_shortcut(start_menu_dir());
    }

    let exe = dir.join(MAIN_BINARY);
    if !exe.is_file() {
        return Err("The installer finished, but 3vhy Client wasn't found in the chosen folder.".into());
    }
    emit(app, "Done", 100.0);
    Ok(exe.to_string_lossy().into_owned())
}

#[tauri::command]
async fn install(
    app: AppHandle,
    dir: String,
    desktop: bool,
    start_menu: bool,
) -> Result<String, String> {
    if INSTALLING.swap(true, Ordering::SeqCst) {
        return Err("Setup is already installing.".into());
    }
    let dir = PathBuf::from(dir.trim());
    let result = tauri::async_runtime::spawn_blocking(move || {
        run_install(&app, &dir, desktop, start_menu)
    })
    .await
    .map_err(|e| format!("Setup crashed while installing: {e}"))
    .and_then(|result| result);
    INSTALLING.store(false, Ordering::SeqCst);
    result
}

#[tauri::command]
fn launch(exe: String) -> Result<(), String> {
    let exe = PathBuf::from(exe);
    let mut command = Command::new(&exe);
    if let Some(dir) = exe.parent() {
        command.current_dir(dir);
    }
    command
        .spawn()
        .map(|_| ())
        .map_err(|e| format!("Couldn't start 3vhy Client: {e}"))
}

#[tauri::command]
fn open_link(url: String) -> Result<(), String> {
    if !url.starts_with("https://") {
        return Err("Only https links can be opened.".into());
    }
    tauri_plugin_opener::open_url(url, None::<&str>).map_err(|e| e.to_string())
}

#[tauri::command]
fn minimize(window: WebviewWindow) {
    let _ = window.minimize();
}

#[tauri::command]
fn close(window: WebviewWindow) -> bool {
    // Closing mid-install would leave a half-written folder behind
    if INSTALLING.load(Ordering::SeqCst) {
        return false;
    }
    window.app_handle().exit(0);
    true
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .on_window_event(|_, event| {
            if let tauri::WindowEvent::CloseRequested { api, .. } = event
                && INSTALLING.load(Ordering::SeqCst)
            {
                api.prevent_close();
            }
        })
        .invoke_handler(tauri::generate_handler![
            setup_info,
            pick_folder,
            install,
            launch,
            open_link,
            minimize,
            close
        ])
        .run(tauri::generate_context!())
        .expect("error while running 3vhy Client Setup");
}
