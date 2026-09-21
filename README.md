# SygixOs

Launcher Apple TV-style pour Google TV — design tvOS (Liquid Glass), sans pubs, avec contenu des apps installées (Jellyfin, Netflix, Prime… via le TV Provider système). Remplace le launcher Google TV sur Android 14+.

## Statut

| Phase | Contenu | Statut |
|---|---|---|
| P1 | Squelette + design system + grille d'apps + dock | ✅ fait |
| P2a | Héro plein écran : programmes du TV Provider système + fallback aerial | ✅ fait |
| P2b | Rangée Up Next dédiée + recherche | à venir |
| P3 | Screensaver système aerial + fond contextuel par app (effet Top Shelf) | à venir |
| P4 | BetaSeries (OAuth) + fusion Up Next | à venir |
| P5 | Remplacement du launcher système (ADB) | à venir |

## Fonctionnement du héro
Le héro ne se connecte à aucun serveur : il lit le **TV Provider Android** (`content://android.media.tv`, watch next + preview programs). Dès qu'une app installée publie du contenu (reprise de lecture, récemment ajoutés — c'est ce que Jellyfin Android TV, Netflix, Prime… publient déjà pour le launcher Google), elle apparaît : poster, titre, barre de progression, clic → fiche du contenu via l'intent fourni par l'app. Fallback : vidéos aériennes Apple en boucle. La permission `com.android.providers.tv.permission.READ_EPG_DATA` est demandée au premier lancement.

## Installation (test sur TV)
1. Sur la TV : Paramètres → À propos → 7× sur « Build » (mode développeur), puis activer le débogage ADB
2. `adb connect <IP-TV>` puis `adb install app/build/outputs/apk/debug/app-debug.apk`
3. Remplacement définitif (P5, réversible) :
   ```
   adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
   adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
   ```
   Rétablir : `adb shell pm enable <package>`

## Build
```
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac)))) ANDROID_HOME=~/android-sdk ./gradlew assembleDebug test
```

## Structure
```
openspec/           specs et workflow de spécification (project.md, specs/, changes/archive/)
app/                application Android (core/, data/, domain/, ui/)
docs/screenshots/   captures Roborazzi
```

## Stack
Kotlin · Jetpack Compose for TV · media3 (ExoPlayer) · Coil · DataStore · tests JUnit/Robolectric · screenshots Roborazzi
