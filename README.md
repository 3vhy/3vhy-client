<p align="center"><img src="assets/brand/eventide-banner.png" alt="Eventide" width="760"></p>

<p align="center">
<a href="https://github.com/3vhy/3vhy-client/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/3vhy/3vhy-client?label=release&color=A855F7"></a>
<a href="https://github.com/3vhy/3vhy-client/releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/3vhy/3vhy-client/total?color=A855F7"></a>
<img alt="Windows" src="https://img.shields.io/badge/platform-Windows-120D1C">
<img alt="Minecraft 1.21.11 and 26.1" src="https://img.shields.io/badge/Minecraft-1.21.11%20%C2%B7%2026.1-120D1C">
<a href="LICENSE"><img alt="GPL-3.0" src="https://img.shields.io/badge/license-GPL--3.0-120D1C"></a>
</p>

# Eventide

Formerly 3vhy client.

A Minecraft launcher for Windows with its own in-game Fabric mod, built on the open-source
[Modrinth App](https://github.com/modrinth/code). It keeps everything the Modrinth App does
(downloading mods and modpacks from Modrinth, instances, Microsoft sign-in, "Jump in" to recent
servers) and adds a new home screen and the Eventide mod: a HUD, PvP tools, knives, living skies,
Name Protect and the Orbit menu.

**Eventide is not affiliated with or endorsed by Modrinth.** Mods and modpacks come from
[modrinth.com](https://modrinth.com) through Modrinth's public API.

## What's different from the Modrinth App

- **Labeled sidebar**: Home, Modpacks, Mods, Library, Skins, Settings, plus Logs and Folder for the
  instance picked on Home.
- **The Eventide mod** in every supported Fabric instance (`apps/mod`), kept up to date by the
  launcher, and also released on its own for other launchers.
- **Removed**: ads and ad-consent prompts, Modrinth+ upsell, Modrinth news, in-app surveys,
  Modrinth's analytics (PostHog) and crash reporting (Sentry), Modrinth's auto-updater, and the
  Modrinth Hosting tab.
- **Interactive installer** (`apps/setup`): a small app that runs the real installer silently and
  shows progress, with options for install folder, shortcuts and launching.
- **Separate install**: its own app ID, data folder and `eventide://` links, so it can sit next to
  the real Modrinth App without conflicts. (The app ID is still `com.threevhy.client` and the old
  `threevhy://` links still work, so installs from before the rename keep their instances.)

## Brand

The logo, icons, wordmark and banners are in [`assets/brand`](assets/brand): `eventide-icon.svg` is
the master, and every PNG, `eventide.ico` and `eventide.icns` is rendered from it. The wordmark is
Unbounded Bold (SIL Open Font License, in `assets/brand/fonts`). Colours: tile `#120D1C`, trident
`#F3ECFF`, riptide rings `#A855F7`.

## Building the Windows installer

The easiest way is GitHub Actions (no local setup needed):

1. Push this repository to your own GitHub account.
2. Open the **Actions** tab, pick **Build Eventide (Windows)** and click **Run workflow**.
3. When it finishes (about 20–40 minutes), download the `eventide-windows` artifact. It contains:
   - **`Eventide Setup.exe`**: the interactive installer (welcome screen, install options,
     live progress, launch when done). Give this one to players.
   - **`Eventide Classic Installer.exe`**: the standard Windows installer. Use it if the Setup
     doesn't open (it needs Microsoft Edge WebView2, which Windows 10 and 11 normally have).
   - **`3vhy Client Classic Installer.exe`**: the same classic installer under its old name, which
     copies of 3vhy Client look for when they check for updates.

To publish a release instead, push a tag such as `v0.8.0`; the installers and the mod jars
(`eventide-<minecraft>-<version>.jar`) are attached to the GitHub Release automatically.

The installer is not code-signed, so Windows SmartScreen will warn the first time it runs
("More info" → "Run anyway"). Signing needs a paid code-signing certificate.

### Building locally

You need Node.js 24.15+, pnpm (`corepack enable`), Rust (installed from `rust-toolchain.toml`),
Java 17, and the [Tauri prerequisites for Windows](https://tauri.app/start/prerequisites/).

```sh
cp packages/app-lib/.env.prod packages/app-lib/.env
pnpm install
pnpm --filter=@modrinth/app run tauri build --bundles nsis
```

The installer ends up in `target/release/bundle/nsis/`. For live development use
`pnpm --filter=@modrinth/app run dev`.

## Discord Rich Presence

The launcher and the in-game mod show Eventide on Discord through the Discord application
`1558249416144457829`. Its name is what Discord shows after "Playing", and its logo is the art asset
with the key `eventide`; both are set at <https://discord.com/developers/applications>.

To use a different application, set a repository variable `EVENTIDE_DISCORD_APP_ID` (Settings →
Secrets and variables → Actions → Variables).

## Keeping up with Modrinth

Modrinth ships fixes, including security fixes, regularly. To pull them in:

```sh
git remote add upstream https://github.com/modrinth/code.git
git fetch upstream
git merge upstream/main
```

Most Eventide changes are in new files (`pages/Index.vue`, `pages/Library.vue`,
`components/ui/home/`, `components/ui/brand/`, `assets/stylesheets/eventide-theme.scss`), which
keeps merges small. Lines changed in existing files are marked with `Eventide:` comments.

## License

The launcher (`apps/app`, `apps/app-frontend`, `packages/app-lib`) is licensed under
**GPL-3.0**, like the Modrinth App it is based on; other packages keep their own licenses (see each
package's `LICENSE`). If you share builds of Eventide, you must also share its source code under the
same license.

Modrinth's logos and branding are not covered by these licenses and have been removed, as required
by Modrinth's [copying guidelines](COPYING.md). "Minecraft" is a trademark of Mojang/Microsoft.
