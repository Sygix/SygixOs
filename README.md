<h1 align="center"><img src=".github/assets/readme-logo.png" alt="SygixOs" width="640"></h1>

[![test](https://github.com/Sygix/SygixOs/actions/workflows/test.yml/badge.svg)](https://github.com/Sygix/SygixOs/actions/workflows/test.yml)
[![release](https://img.shields.io/github/v/release/Sygix/SygixOs?include_prereleases&sort=semver)](https://github.com/Sygix/SygixOs/releases)
[![license](https://img.shields.io/badge/license-AGPL--3.0--or--later-blue)](LICENSE)

A free and open source launcher for Android TV / Google TV, inspired by tvOS 26: dark Liquid Glass, polished focus and animations, the Figtree typeface, **no ads**. It showcases the content published by your installed apps (Jellyfin, Netflix, Prime Video…) through the Android TV Provider, with no server and no account.

> Independent project, not affiliated with Apple, Google or TCL. Apple TV and tvOS are trademarks of Apple Inc.; Android TV and Google TV are trademarks of Google LLC.

## Your TV, without the clutter

An immersive home screen, fluid navigation, and your apps where you want them. See SygixOs in this **41-second presentation** (French · Full HD · 60 fps).

https://github.com/user-attachments/assets/5f70ce5f-76bd-4901-baad-af92669b23b2

Music: **“Electric Dreams” by Scott Buckley**, released under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) — [www.scottbuckley.com.au](https://www.scottbuckley.com.au/library/electric-dreams/). Edited excerpt with original sound effects. Nature footage and demonstration artwork: [Pexels](https://www.pexels.com/license/).

## Features

- **Startup splash**: on a cold start, the SygixOs ghost mascot floats and blinks alone on a black background, then crossfades into the hero. Never shows on return. Respects the system's animation setting.
- **Full-screen hero**: a muted slideshow of the programs published by your installed apps (continue watching, new releases, recommendations), with preview video when available. Source app and label above the title, details and progress bar below, readable on any poster.
- **À suivre**: a single row combining Watch Next from all your apps, deduplicated, with opened content first and removal per series or movie.
- **Dock and app grid**: apps detected automatically, 16:9 banners, tvOS-style focus, up to 6 pinned apps, reorderable grid.
- **Dark glass**: translucent glass with live blur on the dock, capsule, context menu and move-mode banner.
- **Top Shelf preview**: artwork appears above an app tile after a short hover and pans once.
- **Clock and settings capsule**: time and settings gear in a small glass capsule at the top right of the hero.
- **Settings**: choose source apps, hide and restore apps, manage removed content, version and licenses, check for updates.
- **System launcher**: guided setup to make SygixOs your home screen, automatic or by permissions, with restore before uninstall.
- **Self-update**: daily background check, signaled by a blue dot; nothing downloads without your consent.
- **Never a black screen**: falls back to nature clips, then a settling gradient.
- **Fully local**: no backend, no telemetry, no hard-coded app list.

## Roadmap

### Available now

- **Launcher core** — app grid, dock, dark glass design system, tvOS-inspired focus and animations
- **Home hero** — full-screen slideshow of the content published by your apps, with preview video, source attribution and progress
- **À suivre** — unified Up Next row across all apps, with opened-first ordering and removal
- **Settings** — source apps, hidden apps, about, updates
- **Startup splash** — animated mascot on cold start
- **Built-in updates** — check, download and install new versions from GitHub
- **System launcher setup** — guided setup to make SygixOs your home screen, automatic or by permissions
- **Simplified home setup** — [Tsuyu](https://github.com/Sygix/Tsuyu)-based automatic setup and repair for non-technical users

### Coming next

- **Search**
- **BetaSeries (OAuth)** — richer Up Next data and reliability

Detailed requirements for each feature live in [`openspec/specs/`](openspec/specs), and ongoing changes in [`openspec/changes/`](openspec/changes). Follow-ups deferred after v0.0.1, not specified yet, are tracked in [`openspec/backlog.md`](openspec/backlog.md). Specs and the user interface are written in French.

## Requirements

- Android TV or Google TV on **Android 14 or later** (minSdk 34)
- Tested on a TCL Google TV (Android 14)

## Installation

1. Download `app-release.apk` from the [Releases](https://github.com/Sygix/SygixOs/releases) page.
2. Send it to your TV and install it:
   - with [LocalSend](https://localsend.org) or "Send Files to TV" from your phone, or
   - by opening the release page in the TV's browser.
   Accept the install from an unknown source when Android asks.
3. On first launch, grant the "TV programs" permission (`READ_TV_LISTINGS`). Without it, the hero cannot see other apps' content.

**Startup profile (optional)**: `app-release.dm` compiles the app for a fast start. Install it with the APK if your installer supports multiple files; otherwise the APK works alone.

**Via ADB (advanced)**:
```
adb connect <TV-IP>:<port>
adb install-multiple app-release.apk app-release.dm
```
Check: `adb shell dumpsys package fr.sygix.sygixos | grep status=` → `[status=speed-profile]`.

**Later updates** install from the app: Settings → About → "Vérifier les mises à jour" → "Mettre à jour vers X". The download continues in the background; reopen SygixOs if it does not relaunch. Android may ask once to allow SygixOs to install unknown apps.

**Home screen setup**: at first launch, a panel offers up to three ways to make SygixOs your home screen. The preselected button is the recommended next step; pressing OK all the way through completes a setup that keeps the Google TV home and every remote key. Automatic steps show "Étape X sur N".

- **With Tsuyu** (recommended, when a release is available): [Tsuyu](https://github.com/Sygix/Tsuyu) is a privileged service by the same author. SygixOs downloads and verifies `tsuyu.apk` from Tsuyu's releases, then walks you through starting it and authorizing SygixOs. USB debugging stays on; relaunching or restoring no longer needs re-enabling it. Automatic repair runs silently when the Home-key service is blocked or boot opening was not observed.
- **Without Tsuyu**: enable developer options and USB debugging, then follow the prompts. SygixOs connects to the TV's own ADB, grants the manufacturer's auto-start operation when present, and defines the HOME role.
- **By permissions**: "Ouvrir SygixOs avec la touche Home" and "Ouvrir SygixOs à l'allumage" toggle the accessibility and boot-opening routes. The manufacturer's auto-start row appears only when its manager is installed.

When needed, setup asks "Un dernier choix": keep the Google TV home (preselected) or disable it, with a confirmation of what is lost. A different Android user keeps the original home. Failed partial operations roll back. "Détails" lists every change and its state.

Some TVs expose no network ADB port: automatic setup then says "Indisponible sur cette TV" and the permission route stays usable. Without Tsuyu, disable debugging after success; re-enable it to rerun setup.

**Restore before uninstalling**: use "Rétablir l'accueil Google TV". Without it, uninstalling can leave only an empty system fallback home. If SygixOs is unusable, the manual ADB recovery is:
```
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb shell cmd package set-home-activity --user 0 fr.sygix.sygixos/.ui.MainActivity
```
Reverse order restores the stock home; a factory reset is the last resort.

## Navigation

Remote control (D-pad) only, three tiers: hero → dock (pinned apps, at the bottom of the hero) → grid. The home screen is a single page: the hero fills the first screen, with the dock and the clock and settings capsule on it, and the grid sits below. Going from the dock to the grid scrolls the page in one continuous move: the hero, the dock and the capsule slide out at the top while the grid comes up, back to where you left the grid. Up from the first row, or Back, scrolls back.

- **Down / Up**: switch tiers; only the active zone takes focus.
- **Left / Right**: stay within the zone (previous or next program on the hero).
- **Up from the hero**: settings gear (the clock never takes the focus); Back from the gear returns to the hero button.
- **Back**: returns to the hero.
- **Long press OK** on a tile opens a dark menu (Up / Down to choose, Back to close):
  - pin to or unpin from the dock;
  - "Move" to reorder the grid with the arrows (OK confirms, Back cancels);
  - "Hide".

## How the hero works

The hero reads the programs apps publish to the TV Provider (`content://android.media.tv`, watch next and preview programs), and opens each item through the intent provided by the app. It reloads every time you return to the launcher.

- **Artwork validated before display**: a preview video, or an image or video at least 1080 px wide. A program without suitable artwork is left out of the slideshow.
- **Progressive validation**: the TV Provider can hold hundreds of programs, so only the first hero visuals are validated at startup, and an app's posters once the focus rests on its tile for half a second.
- **Text follows the picture**: the title, details and button change together with the picture, never before it. A program whose picture is not ready within 1 s (some apps serve their pictures slowly through their own content provider) is skipped; its picture keeps loading in the background and the program comes back once it is ready.
- **Optional fields**: the program type (`watch_next_type`), season and episode numbers, duration and playback position are all optional; whatever an app leaves out is simply not shown.
- **Instant startup**: the app catalog is cached.

## Building from source

Requirements: JDK 21, Android SDK with platform and build-tools 37.
```
./gradlew testDebugUnitTest      # JUnit / Robolectric tests
./gradlew assembleRelease        # APK to test on the TV
./gradlew dexMetadataRelease     # its startup profile, app/build/outputs/dexmetadata/release/app-release.dm
```
Without a local SDK, use a throwaway container (Docker or Podman; amd64 is required because adb and aapt2 are x86_64):
```
docker run -d --name sygixos-build --platform linux/amd64 -v "$PWD":/work -w /work \
  -v sygixos-gradle:/root/.gradle ghcr.io/cirruslabs/android-sdk:34 sleep infinity
docker exec sygixos-build sdkmanager "platforms;android-37.0" "build-tools;37.0.0"
docker exec sygixos-build ./gradlew testDebugUnitTest assembleRelease
```
On Fedora or any other SELinux system, add `:z` to the mount (`-v "$PWD":/work:z`). Give the container about 4 GB of memory. Run `./gradlew --stop` before a build session, so leftover Gradle daemons don't get the build killed.

**Performance:** the `debug` build interprets bytecode and stutters on the TV. Always judge smoothness on `assembleRelease` (R8). To measure next to an installed release, `./gradlew assemblePerf` builds `fr.sygix.sygixos.perf` ("SygixOs perf"): same R8 build signed with the debug key, not declared as a home app, profileable from the shell; `./gradlew dexMetadataPerf` builds its startup profile (`app/build/outputs/dexmetadata/perf/app-perf.dm`, install both with `adb install-multiple`); launch it with `adb shell am start -n fr.sygix.sygixos.perf/fr.sygix.sygixos.ui.MainActivity` and measure with `adb shell dumpsys gfxinfo fr.sygix.sygixos.perf`. CI never builds or publishes it. The `perf` variant is for measurements only: its built-in update always ends in an error (its debug signature does not match the release), which is expected.

**Signing:** signing keys come from the environment (`SYGIXOS_STORE_FILE`, `SYGIXOS_STORE_PASSWORD`, `SYGIXOS_KEY_ALIAS`, `SYGIXOS_KEY_PASSWORD`). Without these variables, the debug key is used.

**Releases:** a `vX.Y.Z` tag (or `vX.Y.Z-alpha.N`, `-beta.N`, `-rc.N`) triggers the `release` workflow. It builds, tests, signs and publishes the APK and its startup profile (`app-release.dm`) to a GitHub release; the `versionCode` is derived from the tag.

## Contributing

Contributions are welcome, through issues or pull requests.

- **Spec first**: every feature or behavior change starts with an [OpenSpec](https://github.com/Fission-AI/OpenSpec) proposal in `openspec/changes/<id>/`, approved before implementation. Project context and authoring rules: [`openspec/config.yaml`](openspec/config.yaml).
- **Conventions**: [`AGENTS.md`](AGENTS.md) (in French) applies to humans and AI agents alike. In short:
  - uniflow architecture (ViewModel → StateFlow → Compose), no logic in composables;
  - no code comments;
  - AGPL license header on every file;
  - D-pad navigation tests at TV screen size.
- **Git**:
  - `feat/`, `fix/`, `chore/` branches;
  - [Conventional Commits](https://www.conventionalcommits.org/) in English;
  - signed commits;
  - green CI (`./gradlew test`).
- **Validation**: run `openspec validate --all --strict` before opening a PR.

## Project layout

```
app/          Android app: core/ (design system), data/, domain/, model/, ui/
openspec/     current specs (specs/), changes (changes/), deferred follow-ups (backlog.md), rules (config.yaml)
.github/      CI (test) and release publishing (release)
```

## Stack

Kotlin 2.4 · Jetpack Compose (BOM 2026.09) · Haze 2 (Liquid Glass) · media3 (ExoPlayer) · Coil · Room · DataStore · JUnit / Robolectric

## License

SygixOs is licensed under the [GNU AGPL-3.0-or-later](LICENSE). Third-party library licenses are listed in the app (Settings → About). The fallback nature clips are streamed from [Pexels](https://www.pexels.com/license/).
