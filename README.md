# SygixOs

Launcher Apple TV-style pour Google TV — design tvOS (Liquid Glass), sans pubs, avec contenu des apps installées (Jellyfin, Netflix, Prime… via le TV Provider système). Remplace le launcher Google TV sur Android 14+.

## Statut

| Phase | Contenu | Statut |
|---|---|---|
| P1 | Squelette + design system + grille d'apps + dock | ✅ fait |
| P2a | Héro plein écran : programmes du TV Provider système, diaporama auto, fallback clips nature | ✅ fait |
| P2b | Réglages du launcher : choix des apps sources du héro / Top Shelf | à venir |
| P2c | Rangée Up Next dédiée + recherche | à venir |
| P3 | Screensaver système aerial + fond contextuel par app (effet Top Shelf) | à venir |
| P4 | BetaSeries (OAuth) + fusion Up Next | à venir |
| P5 | Remplacement du launcher système (ADB) | à venir |

## Fonctionnement du héro
Le héro est un fond plein écran, muet, qui enchaîne tout seul (fondu croisé, zoom lent façon Apple TV) les programmes que les apps installées publient dans le **TV Provider Android** (`content://android.media.tv`, watch next + preview programs : reprise de lecture, nouveautés, recommandations de Jellyfin, Netflix, Prime…). Vidéo d'aperçu si l'app en publie une, sinon poster. Gauche/droite : programme précédent/suivant ; OK : fiche du contenu via l'intent fourni par l'app. Aucune connexion à un serveur, aucune liste d'apps codée en dur.

La lecture des programmes des autres apps exige la permission runtime `android.permission.READ_TV_LISTINGS`, demandée au premier lancement ; sans elle le provider ne renvoie que nos propres lignes. Le héro se recharge à chaque retour au launcher.

Seuls les visuels de bonne qualité sont retenus (vidéo d'aperçu privilégiée, image ou vidéo d'au moins 960 px de large) ; un programme sans visuel correct est écarté.

Fallback quand rien n'est publié : clips nature libres de droits (Pexels, 2560x1440, streaming, un seul lecteur), puis dégradé sombre animé si aucune vidéo ne peut être lue. Jamais d'écran noir.

## Navigation
DPAD uniquement, trois paliers : héro → dock (apps épinglées, overlay bas) → grille (masque le héro). Seule la zone active est focusable. Bas/haut changent de palier, gauche/droite restent dans la zone. Retour : revient au héro. Menu ou appui long sur une tuile : épingler/retirer du dock, ou « Déplacer » pour réorganiser la grille aux flèches (OK valide, Retour annule), ordre persisté. Les tuiles sont 16:9 et affichent la bannière Android TV de l'app (sinon son icône). Au focus d'une tuile de la grille, un panneau Top Shelf insère les posters publiés par cette app sous la rangée.

## Installation (test sur TV)
1. Sur la TV : Paramètres → À propos → 7× sur « Build » (mode développeur), puis activer le débogage ADB
2. `adb connect <IP-TV>:<port>` puis `adb install -r app/build/outputs/apk/debug/app-debug.apk` ; accorder la permission « programmes TV » au premier lancement
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
Sans SDK local, dans un conteneur jetable (amd64, adb inclus, connexion TV possible depuis le conteneur) :
```
docker run -d --name sygixos-build --platform linux/amd64 -v "$PWD":/work -w /work \
  -v sygixos-gradle:/root/.gradle -v sygixos-m2:/root/.m2 ghcr.io/cirruslabs/android-sdk:34 sleep infinity
docker exec sygixos-build ./gradlew assembleDebug test
docker exec sygixos-build ./gradlew testDebugUnitTest -Proborazzi.test.record=true   # captures
```

## Structure
```
openspec/           specs et workflow de spécification (project.md, specs/, changes/archive/)
app/                application Android (core/, data/, domain/, ui/)
docs/screenshots/   captures Roborazzi (canevas TV 960x540 dp)
```

## Stack
Kotlin · Jetpack Compose · media3 (ExoPlayer) · Coil · DataStore · tests JUnit/Robolectric · screenshots Roborazzi
