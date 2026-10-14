@AGENTS.md

# 3vhy Client

3vhy Client is a Windows Minecraft launcher forked from the Modrinth App, with its own built-in Fabric mod. The repo is github.com/3vhy/3vhy-client. The Modrinth instructions above still apply (TABs, no heading comments, prepr commands only when asked).

## Where things are

- `apps/app-frontend`, `apps/app`: the launcher (Vue 3 + Tauri) with 3vhy branding and the October look (`use-october.ts`, `3vhy-october.scss`, `PixelGhosts.vue`, `GhostSprite.vue`).
- `apps/setup`: the installer UI (`ui/style.css`, `ui/app.js`).
- `apps/mod`: the 3vhy Fabric mod (Java). `common/` holds the shared code; `mc1_21_11/` and `mc26_1/` hold each Minecraft version's `Compat` and `DrawImpl`.
- `website/index.html`: the download page, one self-contained HTML file. It's published as a claude.ai artifact, so republish it after changing it. Its mod list mirrors `Modules.ALL` and needs updating when a mod is added.

## Branches, CI and releases

- Work on `mod-dev`. Pushing it runs `.github/workflows/3vhy-mod.yml`, which builds the mod and runs the in-game tests on 1.21.11 and 26.1.
- `main` only moves by fast-forward to a commit that passed CI: `git merge-base --is-ancestor origin/main <sha> && git push origin <sha>:refs/heads/main`.
- Releases: the owner drafts a GitHub release, tags `vX.Y.Z` on `main` and publishes it. `.github/workflows/3vhy-client-windows.yml` then builds and attaches `3vhy.Client.Setup.exe` and `3vhy.Client.Classic.Installer.exe`. Give release notes as `## 3vhy Client vX.Y.Z` with New / Changed / Fixed lists, written for players.
- Commit as `xsaaacc-collab <xsaaaccg@gmail.com>`.

## Checking mod changes

- There's no need to run Minecraft locally: push to `mod-dev` and read the CI report. Each run overwrites the branches `ci-report/mc1_21_11` and `ci-report/mc26_1`. Unpack one with `git fetch origin ci-report/mc1_21_11 && git archive origin/ci-report/mc1_21_11 | tar -x -C <dir>`, check that every `3vhy-check` line in the log ends `-> OK`, and look at `screenshots/`. Its commit message names the commit it tested.
- If the build fails, the compiler errors are in the job's annotations: `gh api repos/3vhy/3vhy-client/check-runs/<job id>/annotations`.
- 26.1 skips in-world screenshots (Sodium), so look at the 1.21.11 ones.
- Tests live in `apps/mod/common/src/gametest/java/dev/threevhy/client/test/ThreevhyGameTest.java`. Add a `check(...)` for new behaviour and a `shot(...)` for new UI. HUD close-ups use `MusicShowcase` (1.5x, on grey).
- The CI window is 854x480 at GUI scale 2, so screenshots look smaller than on a real screen.

## Mod code

- `Draw` is the version-neutral drawing interface. `gui/Smooth` draws anti-aliased round boxes, circles, rings, line icons and glowing text in real pixels.
- `gui/UiFont` is the clean font: Titillium Web bold and semibold (SIL OFL, in `assets/threevhy/font`). Theme > Menu font switches back to Minecraft's.
- `gui/Accent.get()` is the theme colour, set from the Themes panel or Theme > Panels colour, with Rainbow as an option.
- Mods and their settings are in `module/Modules.java`; the order of `ALL` is the menu order. Settings the player never changed use the default, so a new default reaches them.
- A new mod needs an icon in `gui/PixelIcons.java` and a card on the website.

## The owner's style rules

- The Panels menu, Music HUD, Keystrokes, Player List and Module List copy the look of 67 Client's, from the owner's reference pictures. Copy only the look, never 67's modules or features.
- When the owner sends a reference picture, match it closely. Measure proportions off the picture (sizes as shares of the text's capital height) and compare a CI screenshot side by side with it before calling it done.
- Send the owner screenshots of visual changes.
