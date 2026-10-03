# Tasks

## 1. Design system
- [ ] 1.1 Jetons de la maquette (design, D1) dans `Theme.kt` (`SygixColors`) et `Motion.kt` (`Dimens`) ; vérifier par grep qu'aucune des couleurs de D1 n'est écrite en dur dans `ui/` (`grep -rn "0xFFF2F2F5\|0x5C16161C" app/src/main/java/fr/sygix/sygixos/ui` vide)
- [ ] 1.2 `GlassSurface` sombre (D2) : teinte paramétrable, `HazePerformanceMode.Performance`, sans aberration chromatique ni `alpha`, bordure, reflets intérieurs et ombre ; repli sans `clip` ; `GlassSurfaceTest` reste vert et un test vérifie qu'un enfant plus grand que la surface n'est pas découpé par le repli
- [ ] 1.3 `Modifier.tvFocusable` (câblage du focus) et `Modifier.tvFocus` au style tvOS (D4) sans halo ni paramètre `glow` ; valeurs animées lues en phase de dessin ; vérifier par grep que `radialGradient` n'apparaît plus dans `TvFocus.kt`
- [ ] 1.4 `FocusPill.kt` (D8) : `pillColors` et `Modifier.focusPill` ; test JUnit `FocusPillTest` (focus, sélectionnée, repos)
- [ ] 1.5 `Modifier.glassRim` (contour de verre sans flou, D3) ; test JUnit `GlassContrastTest` : contraste du texte blanc sur le panneau du menu et du texte sombre sur la pilule, composés sur blanc, ≥ 4,5:1

## 2. Grille et dock
- [ ] 2.1 Espacement de la grille 24 / 32 dp et rayon des tuiles 9 dp (D4) ; `GridScrollTest` et `HomeGridScrollTest` restent verts
- [ ] 2.2 Nom de l'app sous la tuile focusée de la grille (`app-tile-name-<package>`), sans décaler la grille ; test Compose `TvFocusStyleTest`
- [ ] 2.3 `DockLayout` à taille fixe (D5) et `Dock` centré, verre sombre, rangée défilante au-delà de la capacité ; `DockLayoutTest` adapté ; test Compose `TvFocusStyleTest` : 1, 3 et 6 apps à la même taille, plus petite que la grille, dock centré
- [ ] 2.4 Test Compose `TvFocusStyleTest` : tuile focusée de la grille et du dock sans recouvrement des voisines et entière dans l'écran et dans `dock-glass`

## 3. Capsule heure et réglages
- [ ] 3.1 `data/SystemClockSource` et `HomeViewModel.clock` (D6) ; test JUnit `SystemClockSourceTest` : format 24 h (« 21:47 ») et 12 h selon le réglage système, émission initiale puis à chaque diffusion d'heure
- [ ] 3.2 Capsule (`hero-capsule`, `hero-clock`, `settings-gear`) solidaire du héro, verre actif comme le dock ; gauche/droite consommées sur l'engrenage ; test Compose `HeroCapsuleTest` (contenu, focus de l'engrenage seul, gauche/droite, bas, heure mise à jour) ; `SettingsEntryTest` reste vert
- [ ] 3.3 Test de la transition : `SettingsEntryTest` vérifie aussi que `hero-capsule` est hors écran en vue grille avec le décalage de `zone-hero`

## 4. Héro
- [ ] 4.1 Voiles bas, gauche et haut droit (D7) dans la source du flou
- [ ] 4.2 Bouton « Ouvrir » / « Reprendre » sans flou (D3), colonne espacée sans espace vide (D7) ; test Compose `HeroLayoutTest` : même écart avant le bouton avec et sans progression ; `HeroStageTest` reste vert
- [ ] 4.3 Ken Burns arrêté quand le héro n'est pas visible (D9) ; `AmbientGradient` et Ken Burns du panneau lus en phase de dessin

## 5. Menu contextuel
- [ ] 5.1 Panneau sombre sur voile, liste verticale, pilule de focus (D8), tags `menu-action-<action>` ; `HideFlowTest` et `CatalogUpdateFocusTest` adaptés (bas au lieu de droite) ; test Compose `ContextMenuTest` (première action focusée, bas descend à l'action suivante placée dessous, Retour ferme et rend le focus à la tuile)

## 6. Réglages
- [ ] 6.1 Fond de la grille (`AmbientGradient` fixe), volet gauche aux dimensions de D1, pilules sur les catégories, Apps sources, Applications cachées, « Tout réactiver » et À propos ; plus aucun `tvFocus` dans `ui/settings` (grep vide)
- [ ] 6.2 `AppleSwitch` aux dimensions et couleurs de D1, état « Cachée » / « Visible » affiché sur les lignes d'apps cachées
- [ ] 6.3 Test Compose `SettingsPillTest` : catégorie, ligne d'Apps sources, ligne d'Applications cachées et « Tout réactiver » gardent le même rectangle avec et sans focus ; `SettingsNavigationTest` et `HiddenPaneTest` restent verts

## 7. Fluidité
- [ ] 7.1 Test `IdleFrameTest` (écran au repos, D9) : aucune écriture d'état pendant plusieurs secondes sans touche, héro sans programme, dock focusé, grille focusée, grille atteinte depuis un héro avec poster ; le dernier cas échoue sur `main`

## 8. Documentation et vérification
- [ ] 8.1 README : capsule, verre sombre, focus tvOS, menu et réglages ; feuille de route
- [ ] 8.2 `./gradlew test` vert, `./gradlew assembleRelease` (R8) vert, `openspec validate --all --strict` vert
- [ ] 8.3 Aucun commentaire ajouté dans le code ni dans les tests (diff vérifié), en-tête de licence sur chaque nouveau fichier

## 9. Validation sur la TV de test (pré-release, `assembleRelease`)
- [ ] 9.1 Mesures de fluidité de design, « Ce qui reste à mesurer sur la TV », points 1 à 4, consignées dans la PR
- [ ] 9.2 Rendu visuel (point 5) validé par Sygix
- [ ] 9.3 Réponses de Sygix aux questions ouvertes reportées dans les documents du change

## 10. Clôture
- [ ] 10.1 `openspec archive ui-tvos-polish` après merge et validation sur la TV, avant l'archivage de `p2c-upnext` ; `openspec validate --all --strict` vert après fusion
