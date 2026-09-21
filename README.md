# SygixOs

Launcher Apple TV-style pour Google TV — design tvOS (Liquid Glass), sans pubs, avec contenu des apps installées (Jellyfin, Netflix, Prime… via le TV Provider système). Remplace le launcher Google TV sur Android 14+.

## Statut

| Phase | Contenu | Statut |
|---|---|---|
| P1 | Squelette + design system + grille d'apps + dock | ✅ fait |
| P2a | Héro plein écran : programmes du TV Provider système, diaporama auto, fallback clips nature | ✅ fait |
| P2b | Réglages du launcher : choix des apps sources du héro / Top Shelf | à venir |
| P2c | Rangée Up Next dédiée + recherche | à venir |
| P3 | Screensaver système (clips nature en boucle, cache local) | à venir |
| P4 | BetaSeries (OAuth) + fusion Up Next | à venir |
| P5 | Remplacement du launcher système (ADB) | à venir |

## Fonctionnement du héro
Le héro est un fond plein écran, muet, qui enchaîne tout seul (fondu croisé, zoom lent façon Apple TV) les programmes que les apps installées publient dans le **TV Provider Android** (`content://android.media.tv`, watch next + preview programs : reprise de lecture, nouveautés, recommandations de Jellyfin, Netflix, Prime…). Vidéo d'aperçu si l'app en publie une, sinon poster. Gauche/droite : programme précédent/suivant ; le bouton « Ouvrir » (« Reprendre » si lecture en cours) sous le titre porte le focus et ouvre la fiche du contenu via l'intent fourni par l'app. Aucune connexion à un serveur, aucune liste d'apps codée en dur.

La lecture des programmes des autres apps exige la permission runtime `android.permission.READ_TV_LISTINGS`, demandée au premier lancement ; sans elle le provider ne renvoie que nos propres lignes. Le héro se recharge à chaque retour au launcher ; le catalogue d'apps est mis en cache pour un affichage immédiat au démarrage.

Les visuels sont validés avant affichage (vidéo d'aperçu privilégiée, image ou vidéo d'au moins 1080 px de large) ; un programme sans visuel correct n'entre pas dans le diaporama. Le TV Provider pouvant publier des centaines de programmes, seuls les premiers visuels du héro sont validés au démarrage, les affiches d'une app l'étant quand le focus s'y pose.

Fallback quand rien n'est publié : clips nature libres de droits (Pexels, 2560x1440, streaming, un seul lecteur), puis dégradé sombre animé si aucune vidéo ne peut être lue. Jamais d'écran noir.

## Navigation
DPAD uniquement, trois paliers : héro → dock (apps épinglées, overlay bas) → grille (masque le héro). Seule la zone active est focusable. Bas/haut changent de palier, gauche/droite restent dans la zone. Retour : revient au héro. Appui long sur OK sur une tuile : épingler/retirer du dock, ou « Déplacer » pour réorganiser la grille aux flèches (OK valide, Retour annule), ordre persisté. Dans la grille, l'aperçu ne s'ouvre qu'après environ 3 s de focus immobile sur une app qui publie des visuels : il s'insère au-dessus de la rangée et pousse la grille vers le bas. Tant qu'il est ouvert, passer sur une autre app avec du contenu bascule sans délai ni fermeture intermédiaire ; passer sur une app sans contenu le referme. Ouverture, déplacement et fermeture partagent un seul défilement calculé, synchronisé avec l'animation, pour qu'il n'y ait jamais deux mouvements à la suite. Les tuiles sont 16:9 et affichent la bannière Android TV de l'app (sinon son icône).

## Installation (test sur TV)
1. Sur la TV : Paramètres → À propos → 7× sur « Build » (mode développeur), puis activer le débogage ADB
2. `adb connect <IP-TV>:<port>` puis `adb install -r app/build/outputs/apk/debug/app-debug.apk` ; accorder la permission « programmes TV » au premier lancement
3. Remplacement définitif (P5, réversible) :
   ```
   adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
   adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
   ```
   Rétablir : `adb shell pm enable <package>`

## Performance
Le build `debug` interprète le bytecode (ART sans AOT) : la navigation y est saccadée sur la TV. Le build `release` (R8) tourne sans image sautée. Toujours juger la fluidité sur `assembleRelease`.

## Build
JDK 17+, SDK Android avec la plateforme et les build-tools 37 :
```
ANDROID_HOME=~/android-sdk ./gradlew assembleDebug testDebugUnitTest
```
Sans SDK local, dans un conteneur jetable (amd64 obligatoire : adb et aapt2 sont x86_64, adb y joint la TV) :
```
docker run -d --name sygixos-build --platform linux/amd64 -v "$PWD":/work -w /work \
  -v sygixos-gradle:/root/.gradle -v sygixos-m2:/root/.m2 ghcr.io/cirruslabs/android-sdk:34 sleep infinity
docker exec sygixos-build sdkmanager "platforms;android-37.0" "build-tools;37.0.0"
docker exec sygixos-build ./gradlew assembleDebug testDebugUnitTest
docker exec sygixos-build ./gradlew testDebugUnitTest -Proborazzi.test.record=true   # captures
docker exec sygixos-build ./gradlew assembleRelease                                  # build à tester sur la TV
```
Le conteneur doit disposer d'environ 4 Go : avant une session de build, `./gradlew --stop` évite que des démons Gradle résiduels fassent tuer le build par le noyau.
Les tests Robolectric tournent sur l'API 34 (`app/src/test/resources/robolectric.properties`) et ont besoin des ouvertures JDK déclarées dans `app/build.gradle.kts`.
La signature release vient de l'environnement (`SYGIXOS_STORE_FILE`, `SYGIXOS_STORE_PASSWORD`, `SYGIXOS_KEY_ALIAS`, `SYGIXOS_KEY_PASSWORD`) ; sans ces variables, la clé de debug est utilisée.

## Structure
```
openspec/           specs et workflow de spécification (project.md, specs/, changes/archive/)
app/                application Android (core/, data/, domain/, ui/)
docs/screenshots/   captures Roborazzi (canevas TV 960x540 dp)
```

## Stack
Kotlin 2.4 · Jetpack Compose (BOM 2026.09) · Haze 2 (Liquid Glass) · media3 (ExoPlayer) · Coil · DataStore · tests JUnit/Robolectric · screenshots Roborazzi
