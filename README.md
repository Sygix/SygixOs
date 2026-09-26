# SygixOs

[![test](https://github.com/Sygix/SygixOs/actions/workflows/test.yml/badge.svg)](https://github.com/Sygix/SygixOs/actions/workflows/test.yml)
[![release](https://img.shields.io/github/v/release/Sygix/SygixOs?include_prereleases&sort=semver)](https://github.com/Sygix/SygixOs/releases)
[![license](https://img.shields.io/badge/license-AGPL--3.0--or--later-blue)](LICENSE)

A free and open source launcher for Android TV / Google TV, inspired by tvOS: Liquid Glass, polished focus and animations, **no ads**. It showcases the content published by your installed apps (Jellyfin, Netflix, Prime Video…) through the Android TV Provider, with no server and no account.

> Independent project, not affiliated with Apple, Google or TCL. Apple TV and tvOS are trademarks of Apple Inc.; Android TV and Google TV are trademarks of Google LLC.

## Features

- **Full-screen hero**: a muted slideshow (crossfade, slow zoom) of the programs published by installed apps (continue watching, new releases, recommendations). It plays the preview video when the app provides one, otherwise the poster. The "Open" / "Resume" button opens the content page in its app.
- **Dock and app grid**:
  - apps are detected automatically;
  - 16:9 tiles show the Android TV banner;
  - apps can be pinned to the dock;
  - the grid can be reordered with the arrow keys.
- **Top Shelf-style preview**: after about 3 s on an app that publishes content, its artwork appears above the row.
- **Settings** (gear at the top of the hero):
  - choose which apps feed the hero and the preview;
  - hide apps from the grid, and restore them;
  - version and library licenses.
- **Never a black screen**: falls back to nature clips (Pexels), then to an animated gradient.
- **Fully local**: no backend, no telemetry, no hard-coded app list.

## Roadmap

| Phase | Scope | Status |
|---|---|---|
| P1 | Scaffold, Liquid Glass design system, app grid, dock | ✅ done |
| P2a | Full-screen hero fed by the TV Provider, Top Shelf preview, nature fallback | ✅ done |
| P2b | Settings: source apps, hidden apps, about | ✅ done |
| Polish | Home fixes: settings gear, grid and dock refresh, Top Shelf placement | ✅ done (v0.0.1-rc.2) |
| Polish | Continuous hero/grid scroll, hidden apps in the settings panel, source apps sorting | 🚧 in progress ([#16](https://github.com/Sygix/SygixOs/pull/16)) |
| P2c | Up Next row (all apps, deduplication) | 📝 spec done, implementation planned |
| P2c | Search | planned |
| P4 | BetaSeries (OAuth): Up Next enrichment and reliability | planned |
| P5 | Replacing the system launcher | ADB commands available (see Installation) |

Detailed requirements for each feature live in [`openspec/specs/`](openspec/specs), and ongoing changes in [`openspec/changes/`](openspec/changes). Specs and the user interface are written in French.

## Requirements

- Android TV or Google TV on **Android 14 or later** (minSdk 34)
- Tested on a TCL Google TV (Android 14)

## Installation

1. On the TV: Settings → About → press "Build" 7 times (developer mode), then enable ADB debugging.
2. Download `app-release.apk` from the [Releases](https://github.com/Sygix/SygixOs/releases) page.
3. Install it:
   ```
   adb connect <TV-IP>:<port>
   adb install -r app-release.apk
   ```
4. On first launch, grant the "TV programs" permission (`READ_TV_LISTINGS`). Without it, the hero cannot see other apps' content.

To make SygixOs the default launcher by disabling the Google TV one (reversible):
```
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
```
Restore with `adb shell pm enable <package>`. Use at your own risk: disabling the stock launcher may behave differently depending on the TV model.

## Navigation

Remote control (D-pad) only, three tiers: hero → dock (pinned apps, at the bottom) → grid (covers the hero).

- **Down / Up**: switch tiers; only the active zone takes focus.
- **Left / Right**: stay within the zone (previous or next program on the hero).
- **Up from the hero**: settings gear.
- **Back**: returns to the hero.
- **Long press OK** on a tile:
  - pin to or unpin from the dock;
  - "Move" to reorder the grid with the arrows (OK confirms, Back cancels);
  - "Hide".

## How the hero works

The hero reads the programs apps publish to the TV Provider (`content://android.media.tv`, watch next and preview programs), and opens each item through the intent provided by the app. It reloads every time you return to the launcher.

- **Artwork validated before display**: a preview video, or an image or video at least 1080 px wide. A program without suitable artwork is left out of the slideshow.
- **Progressive validation**: the TV Provider can hold hundreds of programs, so only the first hero visuals are validated at startup, and an app's posters when it gets focus.
- **Instant startup**: the app catalog is cached.

## Building from source

Requirements: JDK 21, Android SDK with platform and build-tools 37.
```
./gradlew testDebugUnitTest      # JUnit / Robolectric tests
./gradlew assembleRelease        # APK to test on the TV
```
Without a local SDK, use a throwaway container (Docker or Podman; amd64 is required because adb and aapt2 are x86_64):
```
docker run -d --name sygixos-build --platform linux/amd64 -v "$PWD":/work -w /work \
  -v sygixos-gradle:/root/.gradle ghcr.io/cirruslabs/android-sdk:34 sleep infinity
docker exec sygixos-build sdkmanager "platforms;android-37.0" "build-tools;37.0.0"
docker exec sygixos-build ./gradlew testDebugUnitTest assembleRelease
```
On Fedora or any other SELinux system, add `:z` to the mount (`-v "$PWD":/work:z`). Give the container about 4 GB of memory. Run `./gradlew --stop` before a build session, so leftover Gradle daemons don't get the build killed.

**Performance:** the `debug` build interprets bytecode and stutters on the TV. Always judge smoothness on `assembleRelease` (R8).

**Signing:** signing keys come from the environment (`SYGIXOS_STORE_FILE`, `SYGIXOS_STORE_PASSWORD`, `SYGIXOS_KEY_ALIAS`, `SYGIXOS_KEY_PASSWORD`). Without these variables, the debug key is used.

**Releases:** a `vX.Y.Z` tag (or `vX.Y.Z-alpha.N`, `-beta.N`, `-rc.N`) triggers the `release` workflow. It builds, tests, signs and publishes the APK to a GitHub release; the `versionCode` is derived from the tag.

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
openspec/     current specs (specs/), changes (changes/), rules (config.yaml)
.github/      CI (test) and release publishing (release)
```

## Stack

Kotlin 2.4 · Jetpack Compose (BOM 2026.09) · Haze 2 (Liquid Glass) · media3 (ExoPlayer) · Coil · DataStore · JUnit / Robolectric

## License

SygixOs is licensed under the [GNU AGPL-3.0-or-later](LICENSE). Third-party library licenses are listed in the app (Settings → About). The fallback nature clips are streamed from [Pexels](https://www.pexels.com/license/).
