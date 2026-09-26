# Change : home-settings-polish

## Why
Trois finitions demandées par Sygix après l'usage quotidien du launcher : la transition héro ↔ grille est aujourd'hui un fondu croisé de couches (alpha animé sur le héro, le dégradé et la grille, le dock suivant l'alpha du héro) qui saccade sur la TV de test ; le sous-écran « Applications cachées » ajoute un palier de navigation inutile dans les réglages ; et les deux listes des réglages sont triées par ordre alphabétique uniquement, ce qui noie les apps utiles (celles qui publient du contenu, celles qu'on vient de cacher).

## What Changes
- **Accueil, transition héro ↔ grille en défilement vertical continu** : l'accueil devient une page d'un seul tenant. Bas depuis le dock fait défiler la page d'environ un écran (le héro sort par le haut pendant que la grille remonte) ; Haut depuis la première rangée fait le mouvement inverse. Le dock part avec le héro : il n'est jamais visible en vue grille. Les trois paliers (héro → dock → grille, Retour → héro) et les règles de focus sont inchangés, seul le rendu de la transition change (plus de fondu de couches).
- **Réglages, « Applications cachées » sans sous-écran** : la liste des apps cachées s'affiche directement dans le volet de droite quand la catégorie est sélectionnée, avec le bouton « Tout réactiver » au-dessus de la liste. Chaque ligne garde un switch ; réactiver une app ne retire pas sa ligne immédiatement (on peut la recacher si on s'est trompé), « Tout réactiver » agit sans confirmation et suit la même règle. État vide et navigation D-pad complète conservés.
- **Tri intelligent des deux listes** : « Apps sources » place d'abord les apps qui publient du contenu, par nombre de programmes décroissant, puis celles à 0 par ordre alphabétique. « Applications cachées » place la plus récemment masquée en premier ; les apps cachées avant ce change (sans date) vont en fin de liste par ordre alphabétique.
- **Persistance** : toute app cachée à partir de ce change est datée ; l'ancien format (`hidden_apps`, simple ensemble) reste lu tel quel, sans perte ni migration destructive.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `launcher-shell` : « Navigation 3 paliers » (rendu de la transition en défilement, dock jamais visible en vue grille, Retour depuis la grille), « Fond de la zone grille » (le héro est masqué parce qu'il est sorti de l'écran, plus par alpha), « Dock d'apps épinglées » (le dock part avec le héro).
- `settings` : « Applications cachées » (liste dans le volet de droite, bouton au-dessus, non-retrait immédiat, tri, état vide, D-pad), « Apps sources » (tri par nombre de programmes), « Cacher une application » (date de masquage enregistrée).
- `ui-testing` : ADDED « Couverture de la transition héro ↔ grille » et « Couverture du volet Applications cachées ». Sur `main`, `ui-testing` est encore un change ouvert (`openspec/changes/ui-testing`), pas une spec courante : un MODIFIED sur « Navigation D-pad des trois zones » ne peut pas y être posé ; les deux exigences ajoutées complètent la capability sans la contredire, quel que soit l'ordre d'archivage.

Changes dont celui-ci dépend : `p2b-settings` (archivé, `openspec/specs/settings/spec.md` existe) ; `ui-testing` (change ouvert, voir ci-dessus). Chevauchements avec `p2c-upnext` (PR #13) et `fix/home-ui-bugs` : section suivante.

## Chevauchements avec les changes en cours
Les deltas de ce change sont écrits sur `main` (`423dbbd`). Deux travaux touchent les mêmes exigences ; l'ordre de merge et d'archivage n'est pas tranché (voir « Questions ouvertes »).

**PR #13, `feat/p2c-upnext` (spec uniquement)**
- `launcher-shell` « Navigation 3 paliers » : #13 y intègre la rangée Up Next comme première (ou dernière) ligne de la zone grille ; ce change y remplace le fondu par un défilement. Les deux MODIFIED portent le même en-tête : le second archivé doit reprendre le texte du premier (ici : « la zone grille » désigne la rangée Up Next et les rangées d'apps ; le défilement amène la première ligne de la zone grille en haut de l'écran, quelle qu'elle soit).
- `launcher-shell` « Rangée Up Next », « Sélection des apps sources » : touchées par #13 seulement.
- `settings` « Page de réglages » : #13 y ajoute la catégorie « Écran d'accueil » (texte dans son `design.md`, pose après archivage de p2b-settings, qui est fait) ; ce change ne la modifie pas.
- `settings` « Apps sources » et « Cacher une application » : #13 y ajoute « et la rangée Up Next » dans le périmètre des apps sources ; ce change y ajoute le tri (Apps sources) et la date de masquage (Cacher une application). Même en-tête : le second archivé fusionne les deux textes.
- `ui-testing` : #13 archive `ui-testing` dans sa PR puis pose des MODIFIED ; ce change n'ajoute que des exigences ADDED, compatibles avant ou après.

**Branche `fix/home-ui-bugs` (en cours, non publiée sur le dépôt distant à la date de rédaction)**
- Corrige la visibilité de l'engrenage, le rafraîchissement de la grille et du dock après une action, le Top Shelf et un saut de la grille. **Ce change ne re-spécifie aucun de ces points** : les scénarios ci-dessous supposent ces corrections acquises (le dock et la grille reflètent l'état persisté ; l'engrenage est visible sur le héro conformément à « Icône réglages flottante » de `settings`).
- Zone de contact : le « saut de la grille » et la remise à zéro du défilement interne de la grille (`HomeGrid` remet son `scrollState` à 0 quand la zone grille perd le focus) sont exactement le code que la page d'un seul tenant remplace. Si `fix/home-ui-bugs` est mergé après ce change, sa correction du saut est à reprendre sur la nouvelle structure ; si avant, l'implémentation de ce change repart de sa version.

## Impact
- **Code accueil** : `HomeScreen.kt` (`ui.home`) (couches alpha `heroAlpha` / `gridAlpha`, `LAYER_FADE_MS`, position du dock et de l'engrenage), `HomeGrid.kt` (`ui.home`) (défilement interne, remise à zéro à la perte du focus, calcul « focus toujours visible » du Top Shelf), `Dock.kt` (`ui.home`) (alpha et `active` du verre), `HeroStage.kt` (`ui.hero`) (`visible` / `active` dérivés de la position de la page).
- **Code réglages** : `SettingsScreen.kt` (`ui.settings`) (suppression de `hiddenSubScreen`), `HiddenAppsScreen.kt` (`ui.settings`) (supprimé, remplacé par un contenu de volet), `SettingsContent.kt` (`ui.settings`), `SettingsViewModel.kt` (`ui.settings`) (tri, liste affichée distincte de l'état persisté, comptages intégrés au tri).
- **Persistance** : `data/LauncherPrefs.kt` (nouvelle clé de dates de masquage à côté de `hidden_apps`), `data/AppCatalogRepository.kt` (horodatage au masquage), nouvelles fonctions de tri pures dans `domain/`.
- **Tests** : `HomeNavigationTest`, `HiddenAppsScreenTest`, `SettingsNavigationTest`, `SettingsViewModelTest`, `LauncherPrefsTest` à reprendre ; nouveaux tests JUnit de tri et de compatibilité de lecture.
- **README** : section « Navigation » (grille qui « couvre le héro » → défilement) et « Settings ».
- Aucune dépendance ajoutée.

## Non-goals
- Aucun changement des paliers ni des règles de focus (seule la zone active est focusable, Retour → héro) : reportés à aucune phase, ils restent tels quels.
- Le contenu de la rangée Up Next, sa position et ses réglages : PR #13 (p2c).
- Les corrections de `fix/home-ui-bugs` (engrenage, rafraîchissement grille/dock, Top Shelf, saut de la grille) : non re-spécifiées ici.
- Le périmètre du compteur « programmes publiés » (aujourd'hui `PreviewPrograms` seulement) : question déjà ouverte dans #13, non tranchée ici ; le tri utilise le compteur tel qu'il est affiché.
- Défilement libre de la page à la molette ou au pointeur, geste tactile : hors sujet (D-pad uniquement).
- Thème clair, tests de régression visuelle : phases ultérieures (voir `ui-testing`).

## Questions ouvertes
Aucun des points suivants n'est tranché : ils n'apparaissent ni en SHALL dans les specs, ni comme décision dans `design.md` ou `tasks.md`.

1. **Rafraîchissement de la liste des apps cachées.** Après « réactiver » (ligne ou « Tout réactiver »), la ligne reste affichée. Quand la liste est-elle recalculée à partir de l'état persisté ? Options : (a) en quittant la catégorie (haut/bas vers une autre catégorie, gauche vers le volet gauche ne suffisant pas) ; (b) en fermant les réglages (Retour) seulement ; (c) au prochain affichage de la catégorie, ce qui revient à (a) plus la réouverture des réglages.
2. **Départage à égalité de compteur dans « Apps sources ».** Deux apps ayant le même nombre de programmes (> 0) : ordre alphabétique (probable), ou autre critère (ordre de la grille, date d'installation).
3. **Tri d'« Apps sources » en direct ou figé.** Les compteurs arrivent de façon asynchrone (première valeur vide, puis mise à jour à chaque changement du TV Provider avec antirebond) : si le tri est recalculé à chaque émission, les lignes peuvent se réordonner sous le focus ; sinon il faut fixer le moment du calcul. Options : (a) recalcul à chaque émission, focus conservé sur la même app ; (b) ordre figé à l'entrée dans la catégorie, recalculé à la prochaine entrée ; (c) ordre figé à l'ouverture des réglages.
4. **Focus initial dans le volet « Applications cachées ».** L'exigence « Page de réglages » place le focus sur « le premier élément du volet droit » : avec le bouton au-dessus de la liste, ce serait « Tout réactiver ». Confirmer, ou préférer la première ligne (le bouton étant alors atteint par haut).
5. **Engrenage pendant la transition.** « Icône réglages flottante » de `settings` le place « sur le héro » ; avec le défilement, il sort de l'écran avec le héro (comme le dock) ou reste fixe au-dessus de la grille. Le premier est cohérent avec l'exigence existante ; à confirmer, d'autant que `fix/home-ui-bugs` touche sa visibilité.
6. **Ordre de merge et d'archivage** entre PR #13 (`p2c-upnext`), `fix/home-ui-bugs` et ce change. Options : (a) `fix/home-ui-bugs` → ce change → #13 ; (b) `fix/home-ui-bugs` → #13 → ce change ; (c) ce change d'abord. Dans tous les cas, le second change archivé sur « Navigation 3 paliers », « Apps sources » et « Cacher une application » reprend le texte du premier.
