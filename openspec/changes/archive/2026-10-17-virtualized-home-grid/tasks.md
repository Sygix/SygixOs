# Tasks

## 1. Fenêtre de composition

- [x] 1.1 `GridScroll.rowAt` (inverse de `rowTop`, panneau ouvert compris) et `GridScroll.window` (seuil de 12 rangées, écran courant ∪ ancre, prélecture de 2) ; vérifiés par `GridScrollTest`.
- [x] 1.2 `HomeGrid` remplace les rangées hors fenêtre par un espace de hauteur connue, la `Column`, `verticalScroll`, `hazeSource`, dégradé et ancres restant inchangés ; vérifié par `WindowedGridTest` (total composé sous le seuil, borne au-dessus, zone grille source du verre).

## 2. Focus par cibles composées

- [x] 2.1 `TileFocus` délègue à `FocusTargets` ; grille, dock, cartes Up Next et carte d'erreur liées par clé, requester d'entrée passé à `bind` ; `rememberTileFocus`/`refocus` supprimés ; suites `DockTest`, `UpNextNavigationTest`, `HomeNavigationTest` vertes.
- [x] 2.2 Révélation par l'ancre pour l'entrée dans la grille et l'app déplacée ; vérifié par `WindowedGridTest` (rafale Bas/Haut et inversion, restauration après le héro et le menu contextuel, Up Next après une grande grille, déplacement sur dix rangées avec validation et annulation).

## 3. Top Shelf

- [x] 3.1 Préchauffage de la première affiche avec la requête du panneau pendant le délai d'ouverture ; liste figée jusqu'à la fin de la vérification ; `ShelfPanel` garde l'affiche courante ; vérifié par `WindowedGridTest` (requête avant l'ouverture, aucune nouvelle requête après une validation tardive).
- [x] 3.2 `AnimatedVisibility` seulement sur la rangée dont le panneau est ouvert ou se ferme ; panneau sur une rangée profonde d'une grande grille entre les marges (`WindowedGridTest`), suites `HomeGridTest` et `HomeGridScrollTest` vertes.

## 4. Mesures

- [x] 4.1 Fixture `GridProfileActivity` (source set `perf`, processus `:gridprofile`, 20/60/200/2 000 apps, artworks locaux, aucune ouverture d'app), absente du manifest release.
- [ ] 4.2 Sur émulateur, `assemblePerf` puis cinq passages par taille : premier affichage, CPU, allocations, PSS, tuiles affichées ; ouverture du Top Shelf et navigation rapide sans image en retard ; résultats dans la description de la PR.
- [ ] 4.3 Vérification sur la TV réelle en `assembleRelease` (verre, dégradé, Top Shelf, zoom de focus, transitions, déplacement, retour dans la grille) puis `openspec archive virtualized-home-grid`.
