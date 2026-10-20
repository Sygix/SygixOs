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

- **Startup screen**: on a cold start, the SygixOs ghost mascot floats and blinks alone on a black background (no icon or second screen from the system before it), for at least 0.6 s and until the home screen is ready (app catalog loaded and first hero picture ready, at most 5 s), then crossfades into the hero. It never shows when you come back to the launcher. With animations turned off in the system, the mascot stays still and the home screen appears without a fade.
- **Full-screen hero**: a muted slideshow (crossfade, one slow zoom per picture, then still) of the programs published by installed apps (continue watching, new releases, recommendations). It plays the preview video when the app provides one, otherwise the poster. Above the title, the source app's icon and a label that depends on what the app published: "Continuer dans X" (continue watching), "Épisode suivant dans X" (next episode), "Nouveau dans X" (new), "À regarder dans X" (watchlist), or just the app name for a highlighted program. Under the title, only the details the app provides (season, episode, duration, e.g. "Saison 2 · Épisode 5 · 42 min"), and a progress bar with the time left ("Reste 25 min") when the playback position and the duration are known. Soft dark veils keep the text and button readable on bright posters. The "Open" / "Resume" button opens the content page in its app.
- **Clock and settings capsule**: a small dark glass capsule at the top right of the hero shows the time (12 or 24-hour, as set on the TV) and the settings gear.
- **À suivre** (Up Next): the "À suivre" section combines Watch Next programs from all enabled source apps, deduplicates matching episodes and movies, and preserves each app in the "Ouvrir avec…" menu. The "Applications" section remains separate; "À suivre" can sit before or after it. Contents you open from this row are remembered on the TV: when you come back, the content you opened comes first with its card focused, and remembered contents stay first, most recent first, until no app publishes them anymore. The last card scrolls up to the right margin of the grid, like the first one sits on the left margin. Long-press a card for "Ouvrir avec…", whose last entry "Retirer d'À suivre" hides the whole series or the movie for every app. Program artwork accepts landscape or portrait images at least as wide as the displayed card, otherwise keeping its placeholder.
- **Dock and app grid**:
  - apps are detected automatically;
  - 16:9 tiles show the Android TV banner;
  - the focused tile lifts like on tvOS (zoom, soft shadow, light sheen), with no app name on the tiles;
  - up to 6 apps can be pinned to the dock, a dark glass bar whose tiles keep one fixed size;
  - the grid can be reordered with the arrow keys.
- **Dark glass**: the dock, the capsule, the context menu and the move-mode banner use a dark translucent glass with a live blur computed at reduced resolution, only under these surfaces.
- **Top Shelf-style preview**: after about 3 s on an app that publishes content, its artwork appears above the row and pans slowly once.
- **Shared program artwork**: the hero, Top Shelf, Up Next and removed-content thumbnails use one Coil image loader. HTTP(S) redirects are followed; third-party `content://` images do not need a declared MIME type. Published posters and thumbnails remain alternatives. Up to three source apps load in parallel, with one physical read per app; each request has eight seconds from its actual start, excluding queue time. Displayed artwork has priority over prevalidation. A private Room index tracks programs, image dimensions and associations, with one exported initial schema and tested reconstruction for an unsupported version or unreadable index; source bytes stay in Coil's bounded 256 MiB disk cache. Images are fresh for less than 24 hours, then remain available during their individual refresh while less than seven days old, subject to eviction. Reading or displaying a copy never extends its age. Provider events and foreground returns only update changed or expired images, without clearing the other artwork. Hero quality remains landscape and at least 640 px after reduction within 1920 × 1080; Top Shelf accepts landscape artwork from 640 px regardless of panel width. Images decode at their destination size.
- **Generic artwork**: images rejected by every destination policy, identical image bytes published for at least three different movie/series identities in one app, and `android.resource://` images belonging to another app count as missing artwork, with a one-hour retry interval. Episodes of one series and the same movie in Preview and Watch Next count as one identity. Up Next keeps its card and placeholder; the hero skips quality-rejected programs without another usable visual.
- **Program lifetime and hero order**: successful provider reads confirm each table independently and withdraw missing programs immediately; after a read failure, unconfirmed programs expire at 24 hours, including ones already on screen. Image caches cannot keep a withdrawn program alive. The hero is shuffled once per process and stays stable on refresh and republication under new provider row IDs; new programs are inserted after the displayed one. Programs with pending artwork are skipped until ready. If their image request fails, nature video then the neutral background keep their title and open target. Consecutive fallback programs share one uninterrupted 2K nature clip while their texts change every 12 seconds; its end advances to the next clip, and an intervening program image/video resets playback for the next fallback. Top Shelf stays closed without ready artwork from the focused app.
- **Hero video cache**: nature clips and program previews share a private LRU cache capped at 300 MiB or one quarter of usable disk space, whichever is smaller. Below a 64 MiB cache budget, or on creation/read/write errors, playback uses streaming without a visible cache error.
- **Settings** (gear in the capsule, same background as the grid, light focus pills):
  - choose which apps feed the hero, preview and "À suivre"; apps that publish content come first, by the combined number of Preview and Watch Next programs, then the others alphabetically. The order is set when you open the category, so rows never move under the focus;
  - "Écran d'accueil" contains "Afficher À suivre" (on by default) and "Position d'À suivre" (before applications by default). Changes apply immediately and persist; position remains editable when the row is off. Below them, under "Launcher système", "Configurer SygixOs comme launcher principal" summarizes the automatic and permission-based routes and opens the "Launcher principal" panel (see System launcher);
  - hide apps from the grid, and restore them: the hidden apps are listed right in the settings pane, most recently hidden first, with a "Tout réactiver" (restore all) button above the list that restores the listed apps. A restored app keeps its row (switched to visible) until you leave the category, so a mistake can be undone at once. Below, the "Retirés d'À suivre" group lists the contents removed from the row ("Série · apps" or "Film · apps"); "Réafficher" brings one back. Each group shows three lines, then "Afficher les N autres";
  - version and library licenses;
  - updates: "Vérifier les mises à jour" (check for updates) asks the public GitHub releases of this repository, with no account and no token; "Mettre à jour vers X" (update to X) downloads, verifies and installs the new version, with a QR code next to it that opens the release notes on a phone; "Inclure les préversions" (include pre-releases) is off by default.
- **System launcher**: after the startup screen and the TV-programs permission answer, "Faire de SygixOs l'écran d'accueil" offers automatic setup or guided permissions. "Plus tard", finishing, Back when it closes the panel, or Home dismisses this introduction permanently; the existing dismissal flag is preserved across updates. Settings → "Écran d'accueil" → "Configurer SygixOs comme launcher principal" remains the entry point. Pressing Home while SygixOs is already in front closes settings and menus, scrolls back to the top and focuses the hero; an automatic setup already running continues to its result or deadline.
- **Self-update**: at startup and when you come back to the home screen, at most once a day, SygixOs checks for a new version in the background. It only signals it with a small blue dot on the settings gear and on "À propos": nothing is ever downloaded or installed without you pressing "Mettre à jour vers X".
- **Never a black screen**: falls back to nature clips (Pexels), then to a gradient that drifts once and settles.
- **Fully local**: no backend, no telemetry, no hard-coded app list.

## Roadmap

| Phase | Scope | Status |
|---|---|---|
| P1 | Scaffold, Liquid Glass design system, app grid, dock | ✅ done |
| P2a | Full-screen hero fed by the TV Provider, Top Shelf preview, nature fallback | ✅ done |
| P2b | Settings: source apps, hidden apps, about | ✅ done |
| Polish | Home fixes: settings gear, grid and dock refresh, Top Shelf placement | ✅ done (v0.0.1-rc.2) |
| Polish | Continuous hero/grid scroll, hidden apps in the settings panel, source apps sorting | ✅ done (v0.0.1-rc.4) |
| Polish | tvOS 26 dark glass, focus, readability, concentric corners and smoother grid | ✅ delivered in v0.0.1; historical TV checks still pending documentation |
| P3 | Logo and animated startup splash | ✅ delivered in v0.0.1; splash cadence follow-up remains |
| P4 | Built-in updates from GitHub releases | ✅ delivered in v0.0.1; manufacturer-blocked relaunch to be checked with the HOME role in P5 |
| P5 | Replacing the system launcher (guided setup, Home key, boot opening) | ✅ delivered in v0.1.0-rc.2 and archived; boot opening not verifiable on the reference TV (manufacturer auto-start manager) |
| P6 | "À suivre" row (all apps, deduplication, opened contents first, removal) | ✅ delivered in v0.1.0-rc.2 and archived |
| P7 | Default home screen: automatic setup through the TV's local ADB, or guided permissions | ✅ implemented and archived; real-TV validation pending |
| P7 follow-up | Simpler setup for non-technical users, Tsuyu installed from the TV, automatic repair | ✅ delivered in v0.3.0-rc.1 and archived ([change](openspec/changes/archive/2026-10-18-tsuyu-home-setup/proposal.md)); real-TV validation pending |
| P8 | Search | planned |
| P9 | BetaSeries (OAuth): Up Next enrichment and reliability | planned |

Detailed requirements for each feature live in [`openspec/specs/`](openspec/specs), and ongoing changes in [`openspec/changes/`](openspec/changes). Follow-ups deferred after v0.0.1, not specified yet, are tracked in [`openspec/backlog.md`](openspec/backlog.md). Specs and the user interface are written in French.

## Requirements

- Android TV or Google TV on **Android 14 or later** (minSdk 34)
- Tested on a TCL Google TV (Android 14)

## Installation

1. On the TV: Settings → About → press "Build" 7 times (developer mode), then enable ADB debugging.
2. Download `app-release.apk` and `app-release.dm` from the [Releases](https://github.com/Sygix/SygixOs/releases) page.
3. Install both files together:
   ```
   adb connect <TV-IP>:<port>
   adb install-multiple app-release.apk app-release.dm
   ```
   `app-release.dm` is the startup profile of this APK (Android "dex metadata"): installed with it, the app is compiled for a fast start right away instead of running interpreted until the system compiles it in the background. `adb install -r app-release.apk` alone also works, without that head start. Check with `adb shell dumpsys package fr.sygix.sygixos | grep status=`: `[status=speed-profile] [reason=install-dm]` means the profile was applied (`verify` without it).
4. On first launch, grant the "TV programs" permission (`READ_TV_LISTINGS`). Without it, the hero cannot see other apps' content.

ADB is only needed for this first installation. Later versions install from the app: Settings → About → "Vérifier les mises à jour", then "Mettre à jour vers X". The download goes on if you leave the settings or open another app, and the new version is available when you return to SygixOs. Automatic relaunch is not guaranteed: on the reference TV, the manufacturer's auto-start policy blocks it even after a foreground update. Whether holding the HOME role (P5) lets Android bring SygixOs back is still to be checked on the reference TV; P5 adds no other relaunch mechanism, so if it stays blocked, reopen SygixOs manually. The first time, Android may ask you to allow SygixOs to install unknown apps: accept, and the update carries on. The startup profile (`app-release.dm`) is installed with the update when the release provides it with a SHA-256 digest that matches; otherwise, or if Android refuses it, the update is installed without it. An update is installed only if its size, its SHA-256 digest published by GitHub, its package name, its version and its signing certificate all match; a build signed with another key (for example a local debug build) is refused with "Signature différente de l'app installée".

SygixOs remains a possible home screen (`CATEGORY_HOME`). The first-launch panel offers up to three ways; on every screen the preselected button is the recommended next step, so pressing OK all the way through ends in a complete setup that keeps the Google TV home and every remote key. Automatic steps show "Étape X sur N". The "Launcher principal" panel shows each way using fresh system readings, "Relancer la configuration", "Remplacer complètement l'accueil Google TV" when the stock home is kept, and "Rétablir l'accueil Google TV" when stock components are disabled.

**Automatic with Tsuyu (recommended when a Tsuyu release is available)**: [Tsuyu](https://github.com/Sygix/Tsuyu) is a TV-friendly privileged service compatible with the Shizuku API, by the same author. SygixOs downloads `tsuyu.apk` from Tsuyu's GitHub releases (prereleases follow "Inclure les préversions") and installs it only if its size, its SHA-256 digest published by GitHub, its package name and its signing certificate (identical to SygixOs's) all match. If Android first asks you to allow SygixOs to install apps, allow it and come back: SygixOs restarts the installation once by itself. Then SygixOs opens Tsuyu: select "Démarrer", then "Activer Tsuyu", accept Android's debugging dialog with "Toujours autoriser sur cet ordinateur", and come back with Back. Finally allow SygixOs in Tsuyu's dialog (Tsuyu preselects "Refuser"). SygixOs then runs the same fixed operations through a service started by Tsuyu with the shell identity; the service refuses any command whose exact form is not one of these operations, and it only runs during a requested sequence or a repair. Keep USB debugging on: Tsuyu needs it. With Tsuyu active, relaunching the setup or restoring the Google TV home no longer requires re-enabling debugging.

**Automatic repair (Tsuyu only)**: when SygixOs comes back to the foreground and the Home-key service it enabled is "Bloqué", or opening at boot was not observed, it silently grants the manufacturer's auto-start operation again and removes then re-adds its own accessibility service (other services are kept). Each setting is tried at most once per TV boot, only for settings SygixOs itself made; it never re-enables something you turned off and never disables the Google TV home. "Détails" shows the last repair.

**Automatic without Tsuyu**: enable developer options (Settings → System → About, press the build line seven times; SygixOs's button opens "À propos" first, then the developer options once they are visible), then "Débogage USB" and return to SygixOs. Select "Continuer", check "Toujours autoriser sur cet ordinateur" in Android's debugging dialog, and select "Autoriser". SygixOs stores its own key privately and connects only to the TV's local ADB. It grants the manufacturer's auto-start app operation when present, saves it, defines the HOME role, and checks which home screen Android actually opens. Merely holding the role is insufficient on some Google TV firmware.

Only when necessary, setup asks "Un dernier choix": keeping the Google TV home (preselected) or disabling it. The confirmation explains possible losses: quick settings, the programme guide, long-press Home and profile selection. It only touches `com.google.android.tungsten.setupwraith` and `com.google.android.apps.tv.launcherx`, for the current Android user, with setup-complete and working-home guards. A different Android user keeps the original home. Unverified launchers are excluded. A failed partial operation is rolled back in reverse order. If disabling is declined or unavailable, setup enables SygixOs's accessibility service (preserving other services), grants overlay permission, saves app operations, and enables opening at boot. "Détails" lists changes with their current state.

Some TVs do not expose a network ADB port when debugging is enabled: automatic setup then says "Indisponible sur cette TV" and the permission route remains usable. Connections, authorization and commands have bounded deadlines; no shell terminal or arbitrary commands are exposed. Without Tsuyu, after success, disable debugging as advised: an open network ADB port lets other machines request authorization. Re-enable debugging to rerun setup or restore the stock home; the remembered app key remains accepted unless you revoke debugging authorizations in Android.

**By permissions (without debugging)**: "Ouvrir SygixOs avec la touche Home" opens Accessibility; "Ouvrir SygixOs à l'allumage" toggles boot opening. When needed, "Autoriser l'ouverture au démarrage" opens Android's overlay permission page. The state updates when you return. The manufacturer's auto-start row appears only when its manager is installed; on the reference firmware its menu no longer exposes this permission, so the row says "Indisponible" and explains that only automatic setup can grant it. This route may lose control after a full restart on some TVs.

Once connected, the accessibility service consumes Home press, repeats and release and opens SygixOs on release; a long press acts like a short one. It reads no screen content. If enabled but not connected, it waits briefly, then shows "Bloqué"; SygixOs never tries to rebind it. Turning the service off gives Home back to Android. Boot opening is off by default except when automatic fallback enables it, covers cold boot only, and opens at most once per boot via the boot broadcast or an accessibility connection within five minutes. An unobserved boot shows "SygixOs ne s'est pas ouvert automatiquement à ce démarrage". Overlay permission alone cannot guarantee startup on every firmware.

**Restore before uninstalling SygixOs**: use "Rétablir l'accueil Google TV" and confirm. Stock home components return to their default state and the previously recorded enabled home is restored if available. Accessibility and overlay permissions remain; remove them in Android if desired. Without this restoration, uninstalling SygixOs can leave only an empty system fallback home. If SygixOs is unusable, the following computer-based ADB procedure remains the manual alternative (replace the user with the current Android user):
```
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb shell cmd package set-home-activity --user 0 fr.sygix.sygixos/.ui.MainActivity
```
Manual restoration (reverse order, requires debugging):
```
adb shell pm default-state --user 0 com.google.android.apps.tv.launcherx
adb shell pm default-state --user 0 com.google.android.tungsten.setupwraith
```
Then set the previously recorded home component with `adb shell cmd package set-home-activity --user 0 <previous-home-component>`, when available. Without debugging or a working app, a factory reset is the final recovery route. The release implementation still needs the final real-TV validation recorded in the P7 change.

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

Kotlin 2.4 · Jetpack Compose (BOM 2026.09) · Haze 2 (Liquid Glass) · media3 (ExoPlayer) · Coil · DataStore · JUnit / Robolectric

## License

SygixOs is licensed under the [GNU AGPL-3.0-or-later](LICENSE). Third-party library licenses are listed in the app (Settings → About). The fallback nature clips are streamed from [Pexels](https://www.pexels.com/license/).
