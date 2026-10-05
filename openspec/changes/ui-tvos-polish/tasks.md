# Tasks

## 1. Design system
- [x] 1.1 Jetons de la maquette (design, D1) dans `Theme.kt` (`SygixColors`) et `Motion.kt` (`Dimens`) ; vérifier par grep qu'aucune des couleurs de D1 n'est écrite en dur dans `ui/` (`grep -rn "0xFFF2F2F5\|Color(22, 22, 28\|Color(18, 18, 24" app/src/main/java/fr/sygix/sygixos/ui` vide)
- [x] 1.2 `GlassSurface` sombre (D2) : teinte paramétrable, `HazePerformanceMode.Performance`, sans aberration chromatique ni `alpha`, bordure, reflets intérieurs et ombre ; repli sans `clip` ; `GlassSurfaceTest` reste vert et un test vérifie qu'un enfant plus grand que la surface n'est pas découpé par le repli
- [x] 1.3 `Modifier.tvFocusable` (câblage du focus) et `Modifier.tvFocus` au style tvOS (D4) sans halo ni paramètre `glow` ; valeurs animées lues en phase de dessin ; vérifier par grep que `radialGradient` n'apparaît plus dans `TvFocus.kt`
- [x] 1.4 `FocusPill.kt` (D8) : `pillColors` et `Modifier.focusPill` ; test JUnit `FocusPillTest` (focus, sélectionnée, repos)
- [x] 1.5 `Modifier.glassRim` (contour de verre sans flou, D3) ; test JUnit `GlassContrastTest` : contraste du texte blanc sur le panneau du menu et du texte sombre sur la pilule, composés sur blanc, ≥ 4,5:1

## 2. Grille et dock
- [x] 2.1 Espacement de la grille 24 / 32 dp et rayon des tuiles 9 dp (D4) ; `GridScrollTest` et `HomeGridScrollTest` restent verts
- [x] 2.2 ~~Nom de l'app sous la tuile focusée~~ retiré sur décision de Sygix (11.2)
- [x] 2.3 `DockLayout` à taille fixe (D5) et `Dock` centré, verre sombre ; `DockLayoutTest` adapté ; test Compose `TvFocusStyleTest` : 1, 3 et 6 apps à la même taille, plus petite que la grille, dock centré
- [x] 2.4 Test Compose `TvFocusStyleTest` : tuile focusée de la grille et du dock sans recouvrement des voisines et entière dans l'écran et dans `dock-glass`

## 3. Capsule heure et réglages
- [x] 3.1 `data/SystemClockSource` et `HomeViewModel.clock` (D6) ; test JUnit `SystemClockSourceTest` : format 24 h (« 21:47 ») et 12 h selon le réglage système, émission initiale puis à chaque diffusion d'heure
- [x] 3.2 Capsule (`hero-capsule`, `hero-clock`, `settings-gear`) solidaire du héro, verre actif comme le dock ; test Compose `HeroCapsuleTest` (contenu, focus de l'engrenage seul, gauche/droite, bas, heure mise à jour) ; `SettingsEntryTest` reste vert
- [x] 3.3 Test de la transition : `SettingsEntryTest` vérifie aussi que `hero-capsule` est hors écran en vue grille avec le décalage de `zone-hero`

## 4. Héro
- [ ] 4.1 Voiles bas, gauche et haut droit (D7) dans la source du flou ; implémentés, sans test automatisé possible sans capture d'image : à valider sur la TV (9.2)
- [x] 4.2 Bouton « Ouvrir » / « Reprendre » sans flou (D3), colonne espacée sans espace vide (D7) ; test Compose `HeroLayoutTest` : même écart avant le bouton avec et sans progression ; `HeroStageTest` reste vert
- [x] 4.3 Ken Burns arrêté quand le héro n'est pas visible (D9) ; `AmbientGradient` et Ken Burns du panneau lus en phase de dessin

## 5. Menu contextuel
- [x] 5.1 Panneau sombre sur voile, liste verticale, pilule de focus (D8), tags `menu-action-<action>` ; `HideFlowTest` et `CatalogUpdateFocusTest` adaptés (bas au lieu de droite) ; test Compose `ContextMenuTest` (première action focusée, bas descend à l'action suivante placée dessous, sans boucle, gauche/droite sans effet, Retour ferme et rend le focus à la tuile)

## 6. Réglages
- [x] 6.1 Fond de la grille (`AmbientGradient` fixe), volet gauche aux dimensions de D1, pilules sur les catégories, Apps sources, Applications cachées, « Tout réactiver » et À propos ; plus aucun `tvFocus` dans `ui/settings` (grep vide)
- [ ] 6.2 `AppleSwitch` aux dimensions et couleurs de D1, état « Cachée » / « Visible » affiché sur les lignes d'apps cachées ; implémentés, sans test qui échouerait sans eux : à valider sur la TV (9.2)
- [x] 6.3 Test Compose `SettingsPillTest` : catégorie, ligne d'Apps sources, ligne d'Applications cachées et « Tout réactiver » gardent le même rectangle avec et sans focus ; `SettingsNavigationTest` et `HiddenPaneTest` restent verts

## 7. Fluidité
- [x] 7.1 Test `IdleFrameTest` (écran au repos, D9) : aucune recomposition sur le héro au dégradé animé, avec le dock focusé et sur un poster en Ken Burns ; aucune écriture d'état en vue grille, y compris après un poster ; les cas du dégradé, du dock et du poster quitté échouent sur `main`

## 8. Documentation et vérification
- [x] 8.1 README : capsule, verre sombre, focus tvOS, menu et réglages ; feuille de route
- [x] 8.2 `./gradlew test` vert, `./gradlew assembleRelease` (R8) vert, `openspec validate --all --strict` vert
- [x] 8.3 Aucun commentaire ajouté dans le code ni dans les tests (diff vérifié), en-tête de licence sur chaque nouveau fichier

## 9. Validation sur la TV de test (pré-release, `assembleRelease`)
- [ ] 9.1 Mesures de fluidité de design, « Ce qui reste à mesurer sur la TV », points 1 à 4, consignées dans la PR
- [ ] 9.2 Rendu visuel (point 5) validé par Sygix
- [ ] 9.3 Réponses de Sygix aux questions ouvertes reportées dans les documents du change

## 10. Clôture
- [ ] 10.1 `openspec archive ui-tvos-polish` après merge et validation sur la TV, avant l'archivage de `up-next` ; `openspec validate --all --strict` vert après fusion

## 11. Décisions de Sygix et corrections après relecture
- [x] 11.1 Dock limité à 6 apps (`AppCatalog.MAX_DOCK`, `pinState`, `togglePinned` refusant un 7e épinglage, aucun épinglage enregistré effacé), défilement provisoire du dock supprimé, « Épingler au dock » grisé avec « Dock plein (6 apps maximum) » ; tests `AppCatalogTest` (maximum, conservation, refus, état) et `ContextMenuTest` (dock plein : action désactivée, message, OK sans effet ; dock à 5 : épinglage)
- [x] 11.2 Aucun nom d'app sur ou sous les tuiles ; test `TvFocusStyleTest` « aucun nom » (aucun nœud n'affiche le nom d'une app de la grille)
- [x] 11.3 « Fermer » retiré du menu ; `ContextMenuTest` vérifie trois actions et l'absence de `menu-action-close`
- [x] 11.4 Ken Burns du héro et du panneau en un seul passage, reprise au changement de visuel ; tests `IdleFrameTest` (héro et panneau immobiles après le passage, reprise au changement) ; le cas du panneau échoue sur `main`
- [x] 11.5 Retour depuis l'engrenage vers le bouton du héro ; test `HeroCapsuleTest`
- [ ] 11.6 Engrenage au trait ; implémenté (rendu vérifié localement), sans test automatisé possible sans capture d'image : à valider sur la TV (9.2)
- [x] 11.7 Bandeau du mode déplacement dans la liste des surfaces floutées de « Thème » et du README (code inchangé : déjà une `GlassSurface`)
- [x] 11.8 « Tuiles adaptatives » réécrit pour le maximum de 6 ; scénarios « dock plein » et « épinglages au-delà du maximum »
- [x] 11.9 `TvFocusStyleTest` : tuiles des quatre coins de la grille et des deux bouts d'un dock de 6 apps entières (rectangle visible égal au rectangle complet)
- [x] 11.10 `HeroCapsuleTest` « seule l'heure est recomposée » (observateur de composition : 1 état lu, au plus 5 scopes ; 15 scopes si l'accueil lit l'heure, vérifié en modifiant le code)
- [x] 11.11 `HomeGridScrollTest` en dp littéraux
- [x] 11.12 `@OptIn(ExperimentalHazeApi::class)` remis dans `GlassSurface.kt` (plus d'avertissement Haze à la compilation)
- [ ] 11.13 Alignements maquette : vignette 16:9 de 48 × 27 dp dans les réglages, 13 dp avant le bouton du héro (couvert par `HeroLayoutTest`), icône lecture 13 dp, engrenage focusé blanc, reflet du menu .28 sans reflet bas, catégories au repos en blanc .9 ; implémentés, rendu à valider sur la TV (9.2)
- [x] 11.14 `AGENTS.md` et `openspec/config.yaml` alignés sur les décisions (zoom ~1,08, ombre portée douce autorisée, aucun halo coloré)
- [x] 11.15 Réponses de Sygix aux questions de la deuxième version reportées dans `proposal.md` et `design.md` (section 12)

## 12. Réponses de Sygix (deuxième version)
- [x] 12.1 Dock plein et épinglages au-delà de 6 : comportements confirmés, mentions « à confirmer » retirées de `proposal.md` et `design.md`
- [x] 12.2 Ken Burns du héro à 10 s (`Motion.HERO_KEN_BURNS_MS`) ; test `IdleFrameTest` : aucune écriture d'état entre la fin du passage et le changement de visuel à 12 s, reprise sur le visuel suivant
- [x] 12.3 Dégradé du repli en un seul passage (`Motion.AMBIENT_PASS_MS`) puis figé ; spec « Écran initial du home » ; test `IdleFrameTest` : écritures pendant le passage, aucune après (échoue avec l'ancienne transition infinie)
- [x] 12.4 Vignette 16:9 des lignes d'apps avec la bannière TV, icône centrée en repli ; test `SettingsThumbnailTest`
- [x] 12.5 Police Figtree (google/fonts `a60a77e`, blob `579e2ab`, version 2.002, police variable 62 712 octets) dans `res/font`, typographie du design system sur Figtree, licence OFL 1.1 dans « À propos » via la configuration AboutLibraries (`app/config/`) ; vérifié dans le JSON généré ; suite de tests verte
- [x] 12.6 `IdleFrameTest` : après une touche, les écritures d'état sont appliquées avant d'avancer l'horloge (sans animation en cours, l'horloge de test arrêtée ne les applique pas)

