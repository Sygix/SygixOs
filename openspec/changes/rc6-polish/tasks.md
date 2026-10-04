# Tasks

## 0. Vérification sur l'appareil
- [x] 0.1 Colonnes du TV Provider réellement remplies par les apps de la TV de référence (journal de diagnostic temporaire de la variante `perf`, structure seulement) : `watch_next_type` présent (programmes `NEW` et `CONTINUE` observés, les autres sont des preview programs), numéros de saison et d'épisode publiés par deux apps, durée par plusieurs, position seulement avec un `CONTINUE` ; visuels `content://` du fournisseur d'images de Jellyfin lents (5 à 11 s), visuels HTTPS des autres apps en 0,06 à 0,47 s

## 1. Rayons concentriques
- [x] 1.1 Jetons de `Dimens` (rayons validés, capsule de 36 dp, vignettes, couples `Nested`) et test JUnit `ConcentricCornersTest` (extérieur = intérieur + marge pour chaque couple, valeurs validées)
- [x] 1.2 Tuiles, dock, capsule (hauteur fixe), menu (panneau, pilules, vignette alignée en haut), lignes et vignettes des réglages, bandeau du mode déplacement en pilule, fond du code QR arrondi à sa marge blanche ; tests Compose `ConcentricLayoutTest` et `SettingsRowCornerTest` (marges réelles mesurées, aucun module du code QR dans un coin)
- [x] 1.3 Ombre, reflet de la tuile focalisée et verre précalculé du dock sur le rayon de leur surface (`TileShape`, forme passée au verre) ; `TvFocusStyleTest`, `DockTest` et `GlassBackdropTest` restent verts
- [ ] 1.4 Contrôle à l'œil sur la TV (dock, capsule, menu, réglages, code QR)

## 2. Navigation dans la grille
- [x] 2.1 `Modifier.tvFocus` en nœuds de modificateur (aucune recomposition au focus) ; `TvFocusStyleTest` reste vert
- [x] 2.2 Rangées de la grille isolées, affiches du panneau en état dérivé, visuels vérifiés transmis sans recomposer le héro masqué ni les rangées (état mis à jour dans un `SideEffect`) ; test `GridRecompositionTest` (même nombre de scopes recomposés pour 10 et 25 apps)
- [x] 2.3 Préparation du panneau après 0,5 s de focus, affiches décodées à la taille du panneau × 1,06 ; test `GridRecompositionTest` (focus qui passe) ; `IdleFrameTest` et `HomeGridScrollTest` restent verts
- [x] 2.4 Mesure `dumpsys gfxinfo` de la navigation dans la grille avec la variante `perf`, avant et après (voir la PR)

## 3. Profil de démarrage
- [x] 3.1 Tâches `dexMetadataRelease` et `dexMetadataPerf` (copie du `.dm` produit par AGP pour `minSdk`) ; `./gradlew assembleRelease dexMetadataRelease` produit `app-release.dm` (`primary.prof`, `primary.profm`)
- [x] 3.2 `release.yml` : vérification du `.dm`, publication avec l'APK dans le brouillon avant la publication ; `actionlint` et `zizmor` verts
- [x] 3.3 Mise à jour intégrée (demande de relance effacée si la relance sans profil échoue avant sa validation) : sélection de l'asset (empreinte obligatoire, 16 Mo, état téléversé), téléchargement vérifié dans un fichier temporaire (10 s au plus, archive ouverte avec `ZipFile`), écriture `base.dm` dans la session de l'APK, repli sur l'APK seul, relance unique sans profil après un échec de la session ; tests `UpdateProfileTest` (faux serveur) et `DexMetadataTest`
- [x] 3.4 README : installation `adb install-multiple app-release.apk app-release.dm`, profil de la variante `perf`, release
- [x] 3.5 Sur la TV : `adb install-multiple` de la variante `perf` avec son `.dm` (compilée `speed-profile`, raison `install-dm`, dès l'installation) et sans (`verify`)
- [x] 3.6 Sur la TV : mise à jour intégrée d'une release qui publie le `.dm` (après le passage public), état de compilation lu juste après

## 4. Infos du programme dans le héro
- [x] 4.1 `HeroItem` (type, saison, épisode, durée, position ; progression dérivée de la position et de la durée) et lecture des colonnes dans `TvProviderHeroSource` ; test Robolectric `TvProviderCaptionColumnsTest`
- [x] 4.2 `HeroCaption` (en-tête, ligne d'infos, arrondis) ; test JUnit `HeroCaptionTest`
- [x] 4.3 En-tête avec icône carrée (hauteur réservée), ligne d'infos et temps restant dans `HeroStage`, chaînes en ressources ; tests Compose `HeroInfoTest` (chaque type, données manquantes, sans espace vide), `IdleFrameTest` (aucune recomposition pendant le Ken Burns), `HeroLayoutTest` reste vert
- [x] 4.4 README : infos du héro
- [x] 4.5 Textes et bouton avec le visuel affiché, visuel lent (1 s) remplacé par le programme suivant et chargé en arrière-plan sans nouvelle requête ; test Compose `HeroVisualSyncTest`
- [x] 4.6 Même règle pour la première image d'une vidéo d'aperçu (vidéo lente gardée préparée en pause), vidéo préparée sans être lue sous l'écran de démarrage ; lecteur derrière `HeroVideo` ; test Compose `HeroVideoTest`
- [x] 4.7 Chargement d'image terminé après l'annulation de son effet : résultat non gardé ; `HeroVisualSyncTest`

## 5. Écran de démarrage
- [x] 5.1 Accueil dessiné sous l'écran de démarrage opaque, visuel du héro sans fondu pendant l'écran de démarrage, grille composée à la fin du fondu ; tests `StartupSplashTest` (pixels en rendu natif : accueil invisible jusqu'au fondu), `HeroPosterLayersTest` (visuel sans fondu), `HomeScreenStartupTest` (grille après le fondu)
- [x] 5.2 Vérification des visuels suivants du héro retenue jusqu'à la fin de l'écran de démarrage, bannières décodées sur un fil de basse priorité ; test `StartupValidationTest`
- [x] 5.3 Mesures Perfetto et vidéo de démarrages à froid avant/après avec la variante `perf` (voir la PR)

## 6. Réconciliation et validation
- [x] 6.1 Renvois depuis `rc5-tv-fixes` et `ui-tvos-polish` vers ce change ; `openspec validate --all --strict` vert
- [x] 6.2 `./gradlew test` et `./gradlew assembleRelease dexMetadataRelease` verts
- [ ] 6.3 `openspec archive rc6-polish` après le merge, la validation sur la TV et l'archivage de `ui-tvos-polish`, `startup-splash`, `self-update` et `rc5-tv-fixes`
