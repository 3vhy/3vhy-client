use crate::api::Result;
use std::time::Duration;
use tauri::{AppHandle, Runtime};
use theseus::launcher_update::{self, LauncherUpdate};

/// Lets the window show that 3vhy Client is restarting before it closes for the installer.
const EXIT_DELAY: Duration = Duration::from_millis(400);

pub fn init<R: Runtime>() -> tauri::plugin::TauriPlugin<R> {
    tauri::plugin::Builder::<R>::new("launcher-update")
        .invoke_handler(tauri::generate_handler![
            check_for_3vhy_update,
            install_3vhy_update,
        ])
        .build()
}

/// The newest 3vhy Client release on GitHub, or `None` when this build is up to date.
#[tauri::command]
pub async fn check_for_3vhy_update<R: Runtime>(
    app: AppHandle<R>,
) -> Result<Option<LauncherUpdate>> {
    let current_version = app.package_info().version.to_string();
    Ok(launcher_update::check(&current_version).await?)
}

/// Downloads the newest release, starts its installer and closes 3vhy Client so the
/// installer can replace it. The installer opens 3vhy Client again once it's done.
#[tauri::command]
pub async fn install_3vhy_update<R: Runtime>(app: AppHandle<R>) -> Result<()> {
    let current_version = app.package_info().version.to_string();
    let update = launcher_update::download_and_launch(&current_version).await?;
    tracing::info!(
        "Closing 3vhy Client {current_version} to install {}",
        update.version
    );
    tauri::async_runtime::spawn(async move {
        tokio::time::sleep(EXIT_DELAY).await;
        app.exit(0);
    });
    Ok(())
}
