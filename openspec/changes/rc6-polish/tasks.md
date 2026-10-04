# Tasks

## 1. Rayons concentriques
- [x] 1.1 Jetons de `Dimens` (rayons validés, capsule de 36 dp, vignettes, couples `Nested`) et test JUnit `ConcentricCornersTest` (extérieur = intérieur + marge pour chaque couple, valeurs validées)
- [x] 1.2 Tuiles, dock, capsule (hauteur fixe), menu (panneau, pilules, vignette alignée en haut), lignes et vignettes des réglages, bandeau du mode déplacement en pilule, fond du code QR arrondi à sa marge blanche ; tests Compose `ConcentricLayoutTest` et `SettingsRowCornerTest` (marges réelles mesurées, aucun module du code QR dans un coin)
- [x] 1.3 Ombre, reflet de la tuile focalisée et verre précalculé du dock sur le rayon de leur surface (`TileShape`, forme passée au verre) ; `TvFocusStyleTest`, `DockTest` et `GlassBackdropTest` restent verts
- [ ] 1.4 Contrôle à l'œil sur la TV (dock, capsule, menu, réglages, code QR)

## 2. Navigation dans la grille
- [x] 2.1 `Modifier.tvFocus` en nœuds de modificateur (aucune recomposition au focus) ; `TvFocusStyleTest` reste vert
- [x] 2.2 Rangées de la grille isolées, affiches du panneau en état dérivé, visuels vérifiés transmis sans recomposer le héro masqué ni les rangées ; test `GridRecompositionTest` (plafonds de scopes recomposés, dépassés par l'ancienne implémentation)
- [x] 2.3 Préparation du panneau après 0,5 s de focus, affiches décodées à la taille du panneau, bannières préparées pour le dessin ; `IdleFrameTest` et `HomeGridScrollTest` restent verts
- [ ] 2.4 Mesure `dumpsys gfxinfo` de la navigation dans la grille avec la variante `perf`, avant et après (objectif : moins de 10 % d'images en retard)

## 3. Profil de démarrage
- [x] 3.1 Tâches `dexMetadataRelease` et `dexMetadataPerf` (copie du `.dm` produit par AGP pour `minSdk`) ; `./gradlew assembleRelease dexMetadataRelease` produit `app-release.dm` (`primary.prof`, `primary.profm`)
- [x] 3.2 `release.yml` : vérification du `.dm`, publication avec l'APK dans le brouillon avant la publication ; `actionlint` et `zizmor` verts
- [x] 3.3 Mise à jour intégrée : sélection de l'asset (empreinte obligatoire), téléchargement en mémoire vérifié, écriture `base.dm` dans la session de l'APK, repli sur l'APK seul ; tests `UpdateProfileTest` (faux serveur) et `DexMetadataTest`
- [x] 3.4 README : installation `adb install-multiple app-release.apk app-release.dm`, profil de la variante `perf`, release
- [ ] 3.5 Sur la TV : `adb install-multiple` de la variante `perf` avec son `.dm`, état de compilation lu dans `dumpsys package` juste après l'installation

## 4. Infos du programme dans le héro
- [x] 4.1 `HeroItem` (type, saison, épisode, durée, position) et lecture des colonnes dans `TvProviderHeroSource` ; test Robolectric `TvProviderCaptionColumnsTest`
- [x] 4.2 `HeroCaption` (en-tête, ligne d'infos, arrondis) ; test JUnit `HeroCaptionTest`
- [x] 4.3 En-tête avec icône carrée, ligne d'infos et temps restant dans `HeroStage`, chaînes en ressources ; tests Compose `HeroInfoTest` (chaque type, données manquantes, sans espace vide), `IdleFrameTest` (aucune recomposition pendant le Ken Burns), `HeroLayoutTest` reste vert
- [x] 4.4 README : infos du héro
- [ ] 4.5 Sur la TV : remplissage réel du type, de la saison, de l'épisode et de la durée par les apps installées (affichage du héro de la variante `perf`, structure seulement)

## 5. Réconciliation et validation
- [x] 5.1 Renvois depuis `rc5-tv-fixes` et `ui-tvos-polish` vers ce change ; `openspec validate --all --strict` vert
- [x] 5.2 `./gradlew test` et `./gradlew assembleRelease dexMetadataRelease` verts
- [ ] 5.3 `openspec archive rc6-polish` après le merge, la validation sur la TV et l'archivage de `ui-tvos-polish`, `startup-splash`, `self-update` et `rc5-tv-fixes`
