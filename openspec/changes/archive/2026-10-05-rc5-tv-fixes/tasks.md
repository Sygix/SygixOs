# Tasks

## Acceptation historique rapportée par le propriétaire
Le 6 octobre 2026, Sygix confirme : « J’ai aussi validé ces anciens changes sur la TV ». Cette acceptation couvre le rendu et le comportement livrés, sans constituer un relevé chiffré ni un test exécuté par l'agente. Les cases historiques non cochées restent ouvertes lorsqu'elles demandent une mesure, une consignation ou un scénario individuel non détaillé dans cette confirmation générale ; l'archivage demandé conserve ces réserves documentaires. La cadence de mascotte reste l'écart accepté du backlog, sujet 2 ; la relance automatique bloquée par le constructeur reste reportée à P5 (sujet 9), avec réouverture manuelle.

## 1. Outillage de mesure
- [x] 1.1 Type de build `perf` (`initWith(release)`, suffixe `.perf`, clé de debug, nom « SygixOs perf »), manifeste de variante sans catégorie HOME et `profileable`, tests unitaires désactivés pour la variante ; `assembleRelease` inchangé en dehors du code de ce change (manifeste release identique)
- [x] 1.2 Mesures de référence de la rc.5 sur la TV avec la variante `perf` (héro au repos, focus du dock, grille) et traces Perfetto (origine du décodage 3840 × 2160, passes de rendu par image)

## 2. Héro
- [x] 2.1 `ImageBounds` (D1, D4) ; test JUnit `ImageBoundsTest`
- [x] 2.2 Transformations du visuel voilé et de l'arrière-plan du verre (D1, D3) ; test Robolectric natif `HeroPosterPipelineTest`
- [x] 2.3 Couches du héro (`GlassBackdrop`) : visuel dessiné en une passe, Ken Burns par transformation, fondu par-dessus l'ancien, dégradé et voiles masqués sous un visuel opaque ; `IdleFrameTest` et `HeroMotionDeferTest` restent verts
- [x] 2.4 Textes du programme en `SequentialFade` (D2) ; test Compose `HeroMetadataFadeTest`
- [x] 2.5 Fond noir de l'accueil et fond de fenêtre retirés

## 3. Verre
- [x] 3.1 `GlassSurface` sur l'arrière-plan précalculé quand un visuel de programme est affiché, Haze sinon (D3) ; test Robolectric natif `GlassBackdropTest` (couleur de la maquette, verre sombre sur visuel sombre) ; `GlassSurfaceTest` reste vert
- [ ] 3.2 Capture du dock sur la TV comparée à la maquette sur un visuel sombre et un visuel clair

## 4. Bannières
- [x] 4.1 `AppArtworkSource` borné à 480 × 270 (D4) ; `AppTileArtworkTest` reste vert

## 5. Écran de démarrage
- [x] 5.1 Décodage de l'animation au démarrage du processus, libération à la fin (D5)
- [x] 5.2 `StartupGate` : durée minimale depuis la mascotte ; tests JUnit `StartupGateTest`
- [x] 5.3 Accueil composé après le fondu d'entrée de la mascotte et le focus de la fenêtre, fondu de sortie sans calque ; test Compose `StartupMascotTest` ; `StartupSplashTest`, `HomeScreenStartupTest` et `HomeViewModelStartupTest` adaptés
- [x] 5.4 `SettingsMotionSource` (D5) ; test Robolectric `SettingsMotionSourceTest`

## 5bis. Corrections de relecture
- [x] 5bis.1 Voile du coin haut droit dessiné à part, fixe, au-dessus du visuel et dans le verre de la capsule ; test `HeroPosterLayersTest` (coin non éclairci en fin de zoom)
- [x] 5bis.2 Test d'alignement du verre (copie à motif, zoom 1,08, mi-fondu) dans `GlassBackdropTest`
- [x] 5bis.3 Attente du focus de la fenêtre bornée à 400 ms ; test `StartupMascotTest` (demande de permission)
- [x] 5bis.4 Copie floutée absente : flou en direct ; test `GlassBackdropTest`
- [x] 5bis.5 Retour au programme précédent pendant un fondu sans coupure ; test `HeroPosterLayersTest`
- [x] 5bis.6 Préchargement de l'animation dans le seul processus principal, libéré sur `onTrimMemory` avant l'écran de démarrage, désactivé dans l'application de test ; test `MascotAnimationSourceTest`

## 6. Profil de référence
- [x] 6.1 `app/src/main/baseline-prof.txt` (D6) ; le profil compilé de l'APK contient les règles de l'app

## 7. Validation
- [x] 7.1 `./gradlew test` et `./gradlew assembleRelease` verts ; `openspec validate --all --strict` vert
- [x] 7.2 Mesures TV de la variante `perf` avant/après pour le héro, le dock et la grille (voir la PR)
- [ ] 7.3 Sur la rc.6 installée sur la TV : héro au repos et focus du dock ≥ 55 images par seconde et < 10 % d'images en retard (`dumpsys gfxinfo` sur 10 s) ; décision de Sygix sur le Ken Burns
- [ ] 7.4 Sur la rc.6 : écran de démarrage (noir initial, fondu d'entrée, flottement et clignement visibles, durée de visibilité de la mascotte, fondu de sortie, Retour pendant l'écran de démarrage)
- [x] 7.5 Synchronisation des deltas vérifiée et change archivé dans le lot documentaire, après ses prédécesseurs ; acceptation TV rapportée par le propriétaire, avec les réserves ci-dessus. Le sujet 3 est retiré du backlog après le dernier archivage.
