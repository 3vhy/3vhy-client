//! Updates for 3vhy Client itself, published as GitHub releases. The in-game 3vhy mod is
//! bundled in the launcher (see `threevhy.rs`) and copied into instances before every
//! launch, so installing a launcher update also updates the mod.

use crate::ErrorKind;
use crate::event::LoadingBarType;
use crate::event::emit::{emit_loading, init_loading};
use crate::util::fetch::REQWEST_CLIENT;
use crate::util::io::{self, IOError};
use chrono::{DateTime, Utc};
use futures::StreamExt;
use reqwest::StatusCode;
use reqwest::header::ACCEPT;
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use tokio::io::AsyncWriteExt;
use tokio::sync::Mutex;
use tracing::{info, warn};

const LATEST_RELEASE_URL: &str =
    "https://api.github.com/repos/3vhy/3vhy-client/releases/latest";

/// The plain Tauri NSIS installer. `3vhy.Client.Setup.exe` wraps it in an interactive
/// setup and can't run unattended, so updates never use it.
const INSTALLER_ASSET: &str = "3vhy.Client.Classic.Installer.exe";

/// Flags understood by Tauri's NSIS installer template, as `tauri-plugin-updater` passes
/// them: `/P` installs passively (progress only, no questions, closes a running copy of
/// the app), `/UPDATE` installs over the current copy without uninstalling it first and
/// leaves shortcuts alone, and `/R` with `/ARGS` starts the app again once it's done.
const NSIS_UPDATE_ARGS: [&str; 4] = ["/P", "/UPDATE", "/R", "/ARGS"];

const DOWNLOAD_DIR_NAME: &str = "3vhy-client-update";

static INSTALLING: Mutex<()> = Mutex::const_new(());

/// A 3vhy Client release that's newer than the running app.
#[derive(Serialize, Clone, Debug)]
pub struct LauncherUpdate {
    pub version: String,
    pub name: Option<String>,
    pub notes: String,
    pub published_at: Option<DateTime<Utc>>,
    /// Download URL of the installer
    pub url: String,
    /// Size of the installer in bytes
    pub size: u64,
    /// The release page on GitHub
    pub page_url: Option<String>,
    #[serde(skip)]
    sha256: Option<String>,
}

#[derive(Deserialize)]
struct Release {
    tag_name: String,
    name: Option<String>,
    body: Option<String>,
    html_url: Option<String>,
    published_at: Option<DateTime<Utc>>,
    #[serde(default)]
    assets: Vec<ReleaseAsset>,
}

#[derive(Deserialize)]
struct ReleaseAsset {
    name: String,
    browser_download_url: String,
    size: u64,
    /// `sha256:<hex>`, for assets uploaded since GitHub started recording digests
    digest: Option<String>,
}

/// Looks up the latest 3vhy Client release on GitHub and returns it when it's newer than
/// `current_version`. Local builds are `1.0.0-local`, which is newer than any 0.x release,
/// so they're never offered one.
#[tracing::instrument]
pub async fn check(
    current_version: &str,
) -> crate::Result<Option<LauncherUpdate>> {
    let current = parse_version(current_version)?;
    if let Ok(_idle) = INSTALLING.try_lock() {
        remove_old_installers().await;
    }

    let response = REQWEST_CLIENT
        .get(LATEST_RELEASE_URL)
        .header(ACCEPT, "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .send()
        .await?;
    match response.status() {
        StatusCode::NOT_FOUND => return Ok(None),
        status if !status.is_success() => {
            return Err(ErrorKind::OtherError(format!(
                "GitHub couldn't be asked for 3vhy Client updates right now ({status}). Try again later."
            ))
            .into());
        }
        _ => {}
    }
    let release: Release = response.json().await?;

    let latest = parse_version(&release.tag_name)?;
    if latest <= current {
        return Ok(None);
    }

    let Some(installer) = release
        .assets
        .into_iter()
        .find(|asset| asset.name == INSTALLER_ASSET)
    else {
        warn!(
            "3vhy Client {latest} is released but has no {INSTALLER_ASSET} yet"
        );
        return Ok(None);
    };

    Ok(Some(LauncherUpdate {
        version: latest.to_string(),
        name: release.name.filter(|name| !name.trim().is_empty()),
        notes: release.body.unwrap_or_default(),
        published_at: release.published_at,
        url: installer.browser_download_url,
        size: installer.size,
        page_url: release.html_url,
        sha256: installer.digest.and_then(|digest| {
            digest
                .strip_prefix("sha256:")
                .map(|hex| hex.to_ascii_lowercase())
        }),
    }))
}

/// Downloads the newest release's installer and starts it. On success the caller has to
/// exit the app straight away: the installer replaces the app's files and then opens 3vhy
/// Client again. Returns the update that's being installed.
pub async fn download_and_launch(
    current_version: &str,
) -> crate::Result<LauncherUpdate> {
    if !cfg!(windows) {
        return Err(ErrorKind::OtherError(
            "3vhy Client can only update itself on Windows. Download the new version from GitHub instead.".to_string(),
        )
        .into());
    }

    let Ok(_installing) = INSTALLING.try_lock() else {
        return Err(ErrorKind::OtherError(
            "The update is already being downloaded.".to_string(),
        )
        .into());
    };

    let Some(update) = check(current_version).await? else {
        return Err(ErrorKind::OtherError(
            "You're already on the latest version of 3vhy Client.".to_string(),
        )
        .into());
    };

    let installer = download(&update, current_version).await?;
    info!(
        "Starting the 3vhy Client {} installer at {}",
        update.version,
        installer.display()
    );
    Command::new(&installer)
        .args(NSIS_UPDATE_ARGS)
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()
        .map_err(|error| IOError::with_path(error, &installer))?;

    Ok(update)
}

/// Streams the installer into a temporary folder (replacing installers from earlier
/// updates) while reporting progress as a launcher update loading bar. The file only gets
/// its final name once its size, and its SHA-256 when GitHub lists one, match the release.
async fn download(
    update: &LauncherUpdate,
    current_version: &str,
) -> crate::Result<PathBuf> {
    check_download_url(&update.url)?;

    remove_old_installers().await;
    let dir = download_dir();
    io::create_dir_all(&dir).await?;
    let installer =
        dir.join(format!("3vhy-client-{}-installer.exe", update.version));
    let partial = installer.with_extension("part");

    let loading_bar = init_loading(
        LoadingBarType::LauncherUpdate {
            version: update.version.clone(),
            current_version: current_version.to_string(),
        },
        update.size.max(1) as f64,
        "Downloading update",
    )
    .await?;

    let response = REQWEST_CLIENT
        .get(&update.url)
        .header(ACCEPT, "application/octet-stream")
        .send()
        .await?
        .error_for_status()?;

    let mut file = tokio::fs::File::create(&partial)
        .await
        .map_err(|error| IOError::with_path(error, &partial))?;
    let mut hasher = Sha256::new();
    let mut downloaded = 0u64;
    let mut stream = response.bytes_stream();
    while let Some(chunk) = stream.next().await {
        let chunk = chunk?;
        downloaded += chunk.len() as u64;
        if downloaded > update.size {
            break;
        }
        file.write_all(&chunk)
            .await
            .map_err(|error| IOError::with_path(error, &partial))?;
        hasher.update(&chunk);
        if let Err(error) = emit_loading(&loading_bar, chunk.len() as f64, None)
        {
            warn!("Couldn't report update download progress: {error}");
        }
    }
    file.flush()
        .await
        .map_err(|error| IOError::with_path(error, &partial))?;
    drop(file);

    if downloaded != update.size {
        discard(&partial).await;
        return Err(ErrorKind::OtherError(format!(
            "The 3vhy Client {} download has the wrong size ({downloaded} bytes, expected {}). Try again.",
            update.version, update.size
        ))
        .into());
    }

    if let Some(expected) = &update.sha256 {
        let actual = format!("{:x}", hasher.finalize());
        if actual != *expected {
            discard(&partial).await;
            return Err(ErrorKind::OtherError(format!(
                "The 3vhy Client {} download is damaged (SHA-256 {actual}, expected {expected}). Try again.",
                update.version
            ))
            .into());
        }
    }

    tokio::fs::rename(&partial, &installer)
        .await
        .map_err(|error| IOError::with_path(error, &installer))?;
    Ok(installer)
}

fn download_dir() -> PathBuf {
    std::env::temp_dir().join(DOWNLOAD_DIR_NAME)
}

/// Installers from earlier updates aren't needed once 3vhy Client is running again.
async fn remove_old_installers() {
    let dir = download_dir();
    if dir.exists()
        && let Err(error) = io::remove_dir_all(&dir).await
    {
        warn!("Couldn't clear out old 3vhy Client installers: {error}");
    }
}

async fn discard(path: &Path) {
    if let Err(error) = io::remove_file(path).await {
        warn!("Couldn't remove a broken update download: {error}");
    }
}

/// Only installers attached to a GitHub release get downloaded and run.
fn check_download_url(url: &str) -> crate::Result<()> {
    let parsed = url::Url::parse(url)?;
    let from_release = parsed.scheme() == "https"
        && parsed.host_str() == Some("github.com")
        && parsed.path().contains("/releases/download/");
    if from_release {
        Ok(())
    } else {
        Err(ErrorKind::OtherError(format!(
            "Refusing to download a 3vhy Client update from {url}"
        ))
        .into())
    }
}

fn parse_version(version: &str) -> crate::Result<semver::Version> {
    let trimmed = version.trim();
    let trimmed = trimmed.strip_prefix(['v', 'V']).unwrap_or(trimmed);
    semver::Version::parse(trimmed).map_err(|error| {
        ErrorKind::OtherError(format!(
            "'{version}' isn't a version 3vhy Client understands: {error}"
        ))
        .into()
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn release_tags_compare_as_versions() {
        let tag = parse_version("v0.3.1").unwrap();
        assert!(tag > parse_version("0.3.0").unwrap());
        assert!(tag > parse_version("0.1.57").unwrap());
        assert!(tag < parse_version("1.0.0-local").unwrap());
        assert!(parse_version("0.10.0").unwrap() > tag);
        assert!(parse_version("not a version").is_err());
    }

    #[test]
    fn only_release_downloads_are_allowed() {
        assert!(
            check_download_url(
                "https://github.com/3vhy/3vhy-client/releases/download/v0.3.1/3vhy.Client.Classic.Installer.exe"
            )
            .is_ok()
        );
        assert!(
            check_download_url("https://example.com/releases/download/a.exe")
                .is_err()
        );
        assert!(
            check_download_url(
                "http://github.com/3vhy/3vhy-client/releases/download/v0.3.1/a.exe"
            )
            .is_err()
        );
    }
}
