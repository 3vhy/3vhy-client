@AGENTS.md

# 3vhy Client

3vhy Client is a Windows Minecraft launcher forked from the Modrinth App, with its own built-in Fabric mod. The repo is github.com/3vhy/3vhy-client. The Modrinth instructions above still apply (TABs, no heading comments, prepr commands only when asked).

## Where things are

- `apps/app-frontend`, `apps/app`: the launcher (Vue 3 + Tauri) with 3vhy branding and the October look (`use-october.ts`, `3vhy-october.scss`, `PixelGhosts.vue`, `GhostSprite.vue`).
- `apps/setup`: the installer UI (`ui/style.css`, `ui/app.js`).
- `apps/mod`: the 3vhy Fabric mod (Java). `common/` holds the shared code; `mc1_21_11/` and `mc26_1/` hold each Minecraft version's `Compat` and `DrawImpl`.
- `website/index.html`: the download page, one self-contained HTML file. It's published as a claude.ai artifact, so republish it after changing it. It uses the mod's look (the `ui/Tokens` colours, Inter, Lucide icons in an SVG sprite at the top, the orbit logo), and its mod list is a copy of the in-game mod menu: the eight tabs, then every mod in `Modules.ALL` order with its name, description, icon and default. A new mod needs a card there (and its icon in the sprite), plus the "N mods" heading, the hero button and the facts strip updated. Screenshots come from the CI report. The published artifact is the newest version: if it differs from the repo, start from the artifact.

## Branches, CI and releases

- Work on `mod-dev`. `.github/workflows/3vhy-mod.yml` builds the mod and runs the in-game tests on 1.21.11 and 26.1. It doesn't run on push: start it by hand (workflow_dispatch) on the branch you pushed. Release builds run it without the tests.
- `main` only moves by fast-forward to a commit that passed CI: `git merge-base --is-ancestor origin/main <sha> && git push origin <sha>:refs/heads/main`.
- Releases: the owner drafts a GitHub release, tags `vX.Y.Z` on `main` and publishes it. `.github/workflows/3vhy-client-windows.yml` then builds and attaches `3vhy.Client.Setup.exe`, `3vhy.Client.Classic.Installer.exe` and the mod on its own (`3vhy-mod-fabric-1.21.11.jar`, `3vhy-mod-fabric-26.1.jar`). Give release notes as `## 3vhy Client vX.Y.Z` with New / Changed / Fixed lists, written for players. Notes saved as `.github/release-notes/vX.Y.Z.md` before the tag is pushed become the release's text (pushing the tag alone also publishes the release, named "3vhy Client vX.Y.Z").
- Commit as `xsaaacc-collab <xsaaaccg@gmail.com>`.

## Checking mod changes

- There's no need to run Minecraft locally: push, start the workflow and read the CI report. Each run overwrites the branches `ci-report/mc1_21_11` and `ci-report/mc26_1`. Unpack one with `git fetch origin ci-report/mc1_21_11 && git archive origin/ci-report/mc1_21_11 | tar -x -C <dir>`, check that every `3vhy-check` line in the log ends `-> OK`, and look at `screenshots/`. Its commit message names the commit it tested.
- If the build fails, the compiler errors are in the job's annotations: `gh api repos/3vhy/3vhy-client/check-runs/<job id>/annotations`.
- 26.1 skips in-world screenshots (Sodium), so look at the 1.21.11 ones. CI also adds Mod Menu, so its buttons are tested.
- Locally (JDK 25, `CI=1`): `./gradlew :mc1_21_11:runProductionClientGameTest -Pgametest`, or one part with `-PtestFocus=` `knives`, `glass`, `skies`, `protect`, `perf`, `lyrics`, `bench` (frame times and allocation) or `calibrate` (the knife lab: every knife at rest and frozen through its animations on a sky platform with the HUD off; `calibrate:karambit` for one). Add `--rerun` when nothing changed. Delete `run/config/3vhy-client.json` before a full run.
- Tests live in `apps/mod/common/src/gametest/java/dev/threevhy/client/test/ThreevhyGameTest.java`. Add a `check(...)` for new behaviour and a `shot(...)` for new UI. HUD close-ups use `MusicShowcase` (1.5x, on grey; the Music Player on the 3vhy sky). Test-only entries: `Lyrics.simulate`, `StaffList.simulate`/`leave`/`stopSimulating`, `KnifeRenderer.preview`.
- The CI window is 854x480 at GUI scale 2, so screenshots look smaller than on a real screen.

## Mod code

- `Draw` is the version-neutral drawing interface. On top of it, `ui/Paint` draws round boxes, circles, strokes, soft shadows and glows, lines, rounded pictures and Lucide icons as meshes, sharp at any GUI scale. `Paint.opacity(...)` fades everything drawn after it (it returns the old value to put back). Shapes keep their vertices in `ui/VertexArena` until the frame's GUI is drawn, so don't allocate per shape.
- `ui/Type` draws text by capital height in Inter 500/600/700 (`gui/UiFont`, SIL OFL, in `assets/threevhy/font`). Theme > Menu font switches back to Minecraft's.
- `ui/Tokens` holds the shared colours and sizes; `ui/Anim`, `Ease` and `Spring` do the motion; `ui/Widgets`, `Logo`, `Sky` and `Hits` are the shared controls, the 3vhy logo, the night sky and hit boxes.
- Menus: `gui/menu/MenuScreen` hosts `MenuView` (the tabbed mod grid) and `SettingsSheet` (a mod's settings, with `SheetExtras` for the special rows). `OrbitScreen` is the Right Shift ring. `gui/HudEditorScreen` is Edit HUD. `gui/TitleScreenStyle` is the main menu and its Scene panel. `gui/ButtonStyle` and `gui/Glass` draw Minecraft's (and other mods') widgets as glass by hooking the widget art (button background, text fields, checkboxes, tabs, scrollbars, tooltips, lists), not by class; `ui/Widgets.control` is the same glass for 3vhy's own panels.
- HUD: widgets extend `hud/HudWidget` and are listed in `HudWidgets.ALL` (widgets at the same anchor stack in that order). `hud/Hud` has the shared glass and text. `hud/Hotbar` replaces Minecraft's hotbar when the Hotbar mod is on.
- Knives (formerly Blades; old configs migrate in `Config.migrateBlades`): `module/Knives` (slots, auto mode), `knife/` (models built from code, clips and animator, renderer hooked in `ItemInHandRendererMixin`). Each knife's grip (`KnifeModel.grip`) sits in the middle of the fist and its angle is set relative to the hand (`KnifeRenderer`'s `HOLD_*`), so the handle crosses the palm. Clips are keyframes on monotone cubic curves (no per-key easing), and `KnifeAnimator` hands over between clips with inertial blending. The hand's pose stack arrives turned to the world, so measure depth in a fresh `PoseStack`, as `nearPush` does.
- Skies: `sky/LivingSky` draws `shaders/core/sky_<id>.fsh` (one per sky, shared `include/sky_lib.glsl`) as a full-screen pass in the sky pass, its settings packed in the transform block's texture matrix; thumbnails render into `SkyCanvas` textures.
- Name Protect: `NameProtect` swaps names, `TextRules` applies rules (yours, then the ones `NameDetect` makes from the scoreboard and tab list) wherever text is drawn (`ProtectedText`).
- `module/Performance` applies the Performance preset (shadows via `Paint.shadowQuality`, animations via `Anim.speed`, blur and particles via Minecraft's options).
- `gui/Accent.get()` is the theme colour, set from Theme > Accent colour, with Rainbow as an option.
- Mods and their settings are in `module/Modules.java`; the order of `ALL` is the menu order. Settings the player never changed use the default, so a new default reaches them.
- A new mod needs an icon (`Module.icon("name")`, a Lucide name from `assets/threevhy/icons/lucide.txt`; add the path there if it's missing) and a card on the website.

## The owner's style rules

- The menus, the Orbit, the HUD (Music Player, Staff List, Keystrokes, Target HUD and the rest) and the main menu follow purxiy.com's look, as the owner asked; before that, parts copied 67 Client's. Copy only the look: never another client's logo, art, text, code, modules or features.
- When the owner sends a reference picture, match it closely. Measure proportions off the picture (sizes as shares of the text's capital height) and compare a CI screenshot side by side with it before calling it done.
- Send the owner screenshots of visual changes.
