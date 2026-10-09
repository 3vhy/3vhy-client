# 3vhy Client

A Minecraft launcher for Windows, built on the open-source [Modrinth App](https://github.com/modrinth/code).
It keeps everything the Modrinth App does — downloading mods and modpacks from Modrinth, instances,
Microsoft sign-in, "Jump in" to recent servers — with a new look and home screen.

**3vhy Client is not affiliated with or endorsed by Modrinth.** Mods and modpacks come from
[modrinth.com](https://modrinth.com) through Modrinth's public API.

## What's different from the Modrinth App

- **Labeled sidebar**: Home, Modpacks, Mods, Library, Skins, Settings, plus Logs and Folder for the
  instance picked on Home.
- **3vhy theme**: colors taken from the 3vhy banner (navy to sky blue, ice-white pixel lettering),
  new app icon and logo.
- **Removed**: ads and ad-consent prompts, Modrinth+ upsell, Modrinth news, in-app surveys,
  Modrinth's analytics (PostHog) and crash reporting (Sentry), Modrinth's auto-updater, and the
  Modrinth Hosting tab.
- **Interactive installer** (`apps/setup`): a small app in the 3vhy style that runs the real
  installer silently and shows progress, with options for install folder, shortcuts and launching.
- **Separate install**: own app ID (`com.threevhy.client`), own data folder and `threevhy://`
  links, so it can sit next to the real Modrinth App without conflicts.

## Building the Windows installer

The easiest way is GitHub Actions (no local setup needed):

1. Push this repository to your own GitHub account.
2. Open the **Actions** tab, pick **Build 3vhy Client (Windows)** and click **Run workflow**.
3. When it finishes (about 20–40 minutes), download the `3vhy-client-windows` artifact. It contains:
   - **`3vhy Client Setup.exe`**: the interactive installer (welcome screen, install options,
     live progress, launch when done). Give this one to players.
   - **`3vhy Client Classic Installer.exe`**: the standard Windows installer, themed in 3vhy blue.
     Use it if the Setup doesn't open (it needs Microsoft Edge WebView2, which Windows 10 and 11
     normally have).

To publish a release instead, push a tag such as `v0.1.0`; the installer is attached to the
GitHub Release automatically.

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

The launcher and the in-game mod show "Playing 3vhy Client" using the 3vhy Client Discord
application (`1558249416144457829`). Its logo is the art asset with the key `3vhy`, uploaded under
**Rich Presence → Art Assets** at <https://discord.com/developers/applications>.

To use a different application, set a repository variable `THREEVHY_DISCORD_APP_ID` (Settings →
Secrets and variables → Actions → Variables).

## Keeping up with Modrinth

Modrinth ships fixes, including security fixes, regularly. To pull them in:

```sh
git remote add upstream https://github.com/modrinth/code.git
git fetch upstream
git merge upstream/main
```

Most 3vhy changes are in new files (`pages/Index.vue`, `pages/Library.vue`,
`components/ui/home/`, `components/ui/brand/`, `assets/stylesheets/3vhy-theme.scss`), which keeps
merges small. Lines changed in existing files are marked with `3vhy Client:` comments.

## License

The launcher (`apps/app`, `apps/app-frontend`, `packages/app-lib`) is licensed under
**GPL-3.0**, like the Modrinth App it is based on; other packages keep their own licenses (see each
package's `LICENSE`). If you share builds of 3vhy Client, you must also share its source code under
the same license.

Modrinth's logos and branding are not covered by these licenses and have been removed, as required
by Modrinth's [copying guidelines](COPYING.md). "Minecraft" is a trademark of Mojang/Microsoft.
