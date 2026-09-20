# SygixOs

Launcher Apple TV-style pour Google TV — design tvOS (Liquid Glass), sans pubs, avec intégration Jellyfin (+ BetaSeries en v1.x). Remplace le launcher Google TV sur Android 14.

## Statut
En développement — voir [openspec/](openspec/) pour les specs.

| Phase | Contenu | Statut |
|---|---|---|
| P1 | Squelette + design system + grille d'apps | à venir |
| P2 | Intégration Jellyfin (Up Next, reprise, posters) | à venir |
| P3 | Screensaver aerial + polish animations | à venir |
| P4 | BetaSeries (OAuth) + fusion Up Next | à venir |
| P5 | Remplacement du launcher système (ADB) | à venir |

## Installation (test sur TV)
1. Sur la TCL : Paramètres → À propos → 7× sur "Build" (mode développeur), puis activer le débogage ADB
2. `adb connect <IP-TV>` puis `adb install app-debug.apk`
3. Remplacement définitif (P5, réversible) :
   ```
   adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
   adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
   ```
   Rétablir : `adb shell pm enable <package>`

## Structure
```
openspec/           specs et workflow de spécification (project.md, specs/, changes/)
app/                application Android (core/, data/, domain/, ui/)
```

## Stack
Kotlin · Jetpack Compose for TV · Retrofit · Coil · DataStore · tests JUnit sur la logique métier
