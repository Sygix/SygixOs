# Change : rc5-tv-fixes

## Why
Le test de la pré-release v0.0.1-rc.5 sur la TV de référence (TV Google TV sous Android 14, interface rendue en 1920 × 1080 sur une dalle 4K) a relevé :
- un héro saccadé au repos (environ 31 images par seconde, 87 % d'images en retard, p50 81 ms) et un focus du dock presque entièrement en retard ; le processeur graphique est saturé : cinq passes plein écran par image (fond de fenêtre, fond noir de l'accueil, dégradé du repli sous le visuel, visuel, voiles) et le flou du dock et de la capsule recalculé à chaque image du Ken Burns ; une bannière d'app tierce décodée en 3840 × 2160 au démarrage ;
- un verre du dock gris moyen quel que soit le visuel, loin du verre sombre de la maquette validée ;
- un écran de démarrage dont la mascotte surgit sans fondu, reste immobile (aucun flottement ni clignement) et disparaît sans fondu : le réglage lu pour « animations désactivées » est l'échelle d'animation de l'app, que le constructeur force à 0 alors que l'échelle du système est normale ; la durée minimale est consommée avant que la mascotte soit visible ; la première composition de l'accueil bloque le fil principal pendant l'apparition ;
- la touche Retour ignorée de temps en temps pendant l'écran de démarrage : la fenêtre ne reçoit son focus qu'après la composition bloquante de l'accueil et le système abandonne une touche reçue avant ce focus ;
- pendant le fondu entre deux programmes, deux titres superposés avec un saut vertical ;
- après installation, l'app reste interprétée jusqu'à la compilation de fond : le profil de référence ne couvre que les bibliothèques.

## What Changes
- **Héro** : visuel décodé une seule fois à la taille d'affichage (au plus 1920 × 1080), voiles du bas et de la gauche intégrés à l'image décodée, voile du coin haut droit dessiné seul sur sa zone, dessiné en une passe ; Ken Burns par transformation de l'image déjà dimensionnée (gardé) ; plus de dégradé ni de fond noir dessinés sous un visuel opaque, plus de fond de fenêtre ; nouveau visuel en fondu par-dessus l'ancien, sans calque hors écran ; textes du programme en fondu enchaîné sans superposition, ancrés au-dessus du bouton.
- **Verre du dock et de la capsule** sur un visuel de programme : arrière-plan flouté calculé une fois par visuel (copie réduite, flou de 28 px, saturation 170 %, voiles compris, comme `backdrop-filter` de la maquette), puis déplacé avec le visuel ; teinte, bordure et reflets de la maquette. Sur une vidéo, le dégradé du repli, le menu contextuel et le bandeau du mode déplacement, le flou reste calculé en direct.
- **Bannières d'apps** décodées à la taille des tuiles (au plus 480 × 270), jamais à leur résolution native.
- **Écran de démarrage** : animation décodée dès le démarrage du processus principal, hors du fil principal, libérée sur manque de mémoire avant l'écran de démarrage ; durée minimale comptée depuis la première image qui montre la mascotte ; accueil composé seulement après le fondu d'entrée de la mascotte et le focus de la fenêtre (attente bornée à 400 ms) ; fondu de sortie sans calque hors écran ; « animations désactivées » lu dans l'échelle de durée des animations du système.
- **Profil de référence** : règles pour tout le code de l'app (et Coil) en plus des profils des bibliothèques.
- **Variante de test `perf`** (outillage, sans changement de comportement) : build minifiée signée avec la clé de debug, identifiant `fr.sygix.sygixos.perf`, sans catégorie HOME, installable à côté de la version publiée pour mesurer sur la TV ; jamais publiée par la CI.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `launcher-shell` : MODIFIED « Thème », « Diaporama héro », « Préchargement et mémoire » (versions de `ui-tvos-polish`), « Écran de démarrage », « Écran de démarrage sans animation » (versions de `startup-splash`).
- `ui-testing` : MODIFIED « Couverture de l'écran de démarrage » (version de `startup-splash`) ; ADDED « Couverture du rendu du héro et du verre ».
- `settings` : non touchée.

## Dépendances et chevauchements
- Ce change reprend des exigences portées par des changes non archivés : `ui-tvos-polish` (« Thème », « Diaporama héro », « Préchargement et mémoire ») et `startup-splash` (« Écran de démarrage », « Écran de démarrage sans animation », « Couverture de l'écran de démarrage »). Les deltas MODIFIED partent de leur texte et gardent tous leurs scénarios.
- `self-update` et `p2c-upnext` ne touchent aucune de ces exigences.
- **Ordre d'archivage** : `ui-tvos-polish`, puis `startup-splash`, puis `self-update`, puis `rc5-tv-fixes` ; `p2c-upnext` après `ui-tvos-polish`, sans contrainte vis-à-vis de ce change.

## Impact
- `ui/hero/HeroStage.kt`, nouveaux `ui/hero/HeroPosterPipeline.kt` et `ui/hero/SequentialFade.kt`, `ui/hero/AmbientGradient.kt`.
- `core/designsystem/GlassSurface.kt`, nouveau `core/designsystem/GlassBackdrop.kt`.
- `ui/home/HomeScreen.kt`, `ui/home/StartupSplash.kt`, `ui/home/StartupController.kt`, `ui/home/HomeViewModel.kt`, `ui/MainActivity.kt`, `SygixOsApp.kt`.
- `domain/StartupGate.kt`, nouveau `domain/ImageBounds.kt`, `data/AppArtworkSource.kt`, `data/MascotAnimationSource.kt`, `data/SystemMotionSource.kt`.
- `app/build.gradle.kts` (type de build `perf`), `app/src/perf/AndroidManifest.xml`, `app/src/main/baseline-prof.txt`.
- Aucune nouvelle dépendance. L'APK release ne change que par le code ci-dessus et le profil de référence.

## Non-goals
- Navigation dans la grille (10,9 % d'images en retard mesurés sur la TV avec ce change) : hors de l'objectif fixé par Sygix (héro et dock), à reprendre dans un prochain lot si besoin.
- Profil appliqué dès l'installation (fichier de métadonnées `.dm` livré avec l'APK et passé au `PackageInstaller` de la mise à jour intégrée) : demande de modifier la publication et `self-update` ; reporté (voir Questions ouvertes).
- Refaire l'asset de la mascotte : son flottement mesure 15 px d'amplitude sur 360 px, visible en moins d'une seconde.

## Questions ouvertes
- Ken Burns : gardé, mesuré à 60 images par seconde pendant le zoom et le fondu sur la TV avec la variante `perf` ; la décision finale (garder ou retirer) reste à prendre à la mesure TV de la rc.6 (objectif ≥ 55 i/s et < 10 % d'images en retard).
- Profil appliqué dès l'installation ou la mise à jour : livrer un `.dm` avec la release et l'ajouter à la session d'installation de la mise à jour intégrée, ou accepter la compilation de fond du système.
