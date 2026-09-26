# Change : home-settings-polish

## Why
Trois finitions demandées par Sygix après l'usage quotidien du launcher : la transition héro ↔ grille est aujourd'hui un fondu croisé de couches (alpha animé sur le héro, le dégradé et la grille, le dock suivant l'alpha du héro) qui saccade sur la TV de test ; le sous-écran « Applications cachées » ajoute un palier de navigation inutile dans les réglages ; et les deux listes des réglages sont triées par ordre alphabétique uniquement, ce qui noie les apps utiles (celles qui publient du contenu, celles qu'on vient de cacher).

## What Changes
- **Accueil, transition héro ↔ grille en défilement vertical continu** : l'accueil devient une page d'un seul tenant. Bas depuis le dock fait défiler la page d'environ un écran à la première entrée, jusqu'à la position de la grille laissée à la sortie ensuite (le héro sort par le haut pendant que la grille remonte) ; Haut depuis la première rangée fait le mouvement inverse. Le dock et l'engrenage des réglages partent avec le héro : ils ne sont jamais visibles en vue grille. Les trois paliers (héro → dock → grille, Retour → héro) et les règles de focus sont inchangés, seul le rendu de la transition change (plus de fondu de couches).
- **Réglages, « Applications cachées » sans sous-écran** : la liste des apps cachées s'affiche directement dans le volet de droite quand la catégorie est sélectionnée, avec le bouton « Tout réactiver » au-dessus de la liste. Chaque ligne garde un switch ; réactiver une app ne retire pas sa ligne tant qu'on reste dans la catégorie (on peut la recacher si on s'est trompé), la liste est recalculée quand on quitte la catégorie (autre catégorie ou fermeture des réglages) ; « Tout réactiver » agit sans confirmation et suit la même règle. Le focus initial du volet est la première ligne, « Tout réactiver » est atteint par Haut. État vide et navigation D-pad complète conservés.
- **Tri intelligent des deux listes** : « Apps sources » place d'abord les apps qui publient du contenu, par nombre de programmes décroissant (ordre alphabétique à égalité), puis celles à 0 par ordre alphabétique ; les compteurs sont calculés dès le lancement du launcher ; l'ordre est figé à l'entrée dans la catégorie (les compteurs se mettent à jour sur place, les lignes ne bougent pas sous le focus), avec un seul retri, focus conservé sur l'app, si le premier comptage arrive après l'entrée. « Applications cachées » place la plus récemment masquée en premier ; les apps cachées avant ce change (sans date) vont en fin de liste par ordre alphabétique.
- **Persistance** : toute app cachée à partir de ce change est datée ; l'ancien format (`hidden_apps`, simple ensemble) reste lu tel quel, sans perte ni migration destructive.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `launcher-shell` : « Navigation 3 paliers » (rendu de la transition en défilement, dock et engrenage jamais visibles en vue grille, Retour depuis la grille), « Fond de la zone grille » (le héro est masqué parce qu'il est sorti de l'écran, plus par alpha), « Dock d'apps épinglées » (le dock part avec le héro), « Panneau Top Shelf au focus » (règle de placement inchangée ; « sortie et retour dans la grille » reformulé pour la page d'un seul tenant : la page remonte au héro, la position de la grille est retrouvée à la redescente). « Focus d'une app disparue » n'est pas modifiée : elle reste valable avec le défilement (zone vidée → héro, la page remonte avec l'animation de « Navigation 3 paliers »).
- `settings` : « Applications cachées » (liste dans le volet de droite, bouton au-dessus, recalcul à la sortie de la catégorie, focus initial sur la première ligne, tri, état vide, D-pad), « Apps sources » (comptage dès le lancement, tri par nombre de programmes figé à l'entrée dans la catégorie, retri unique si le comptage est en retard), « Cacher une application » (date de masquage enregistrée), « Page de réglages » (exception de focus initial pour « Applications cachées »), « Icône réglages flottante » (texte courant conservé : engrenage opaque sans fond ni verre ; son fondu est remplacé par le défilement : l'engrenage est solidaire du héro).
- `ui-testing` : ADDED « Couverture de la transition héro ↔ grille » et « Couverture du volet Applications cachées ». `ui-testing` est archivé sur `main` ; ces deux exigences sont nouvelles et ne modifient aucune exigence existante (« Navigation D-pad des trois zones » et « Sélecteurs stables » restent valables telles quelles), d'où des ADDED et non des MODIFIED.

## Dépendances et chevauchements
Deltas écrits sur `main` après le merge de la PR #13 (`p2c-upnext`, spec seule, non archivée), l'archivage de `p2b-settings` et `ui-testing`, puis le merge de la PR #17 (`fix/home-ui-bugs`) et l'archivage de son change `home-ui-bugfixes`.

**Dépendances**
- `p2b-settings` (archivé, `openspec/specs/settings/spec.md`) et `ui-testing` (archivé, `openspec/specs/ui-testing/spec.md`).
- `home-ui-bugfixes` (PR #17, archivé sur `main`) : ses exigences sont dans les specs courantes. « Icône réglages flottante » (`settings`) et « Panneau Top Shelf au focus » (`launcher-shell`) sont reprises ici à partir de leur texte courant ; « Focus d'une app disparue » (`launcher-shell`) n'est pas modifiée.
- **Ordre retenu** (décision Sygix) : `fix/home-ui-bugs` (mergé) → ce change → implémentation d'Up Next (`p2c-upnext`) dans une itération ultérieure.

**`p2c-upnext` (sur `main`, implémenté et archivé après ce change)**
- `launcher-shell` « Navigation 3 paliers » : `p2c-upnext` y intègre la rangée Up Next comme première (ou dernière) ligne de la zone grille ; ce change y remplace le fondu par un défilement. Même en-tête : `p2c-upnext`, archivé en second, reprend le texte de ce change (« la zone grille » désigne alors la rangée Up Next et les rangées d'apps ; le défilement amène la première ligne de la zone grille en haut de l'écran, quelle qu'elle soit).
- `launcher-shell` « Rangée Up Next », « Sélection des apps sources » : touchées par `p2c-upnext` seulement.
- `settings` « Page de réglages » : `p2c-upnext` y ajoute la catégorie « Écran d'accueil » ; ce change y ajoute l'exception de focus initial d'« Applications cachées ». Même en-tête : `p2c-upnext` reprend l'exception au rebase.
- `settings` « Apps sources » et « Cacher une application » : `p2c-upnext` y ajoute « et la rangée Up Next » dans le périmètre des apps sources ; ce change y ajoute le tri figé (Apps sources) et la date de masquage (Cacher une application). Même en-tête : `p2c-upnext` fusionne les deux textes au rebase.
- `ui-testing` : `p2c-upnext` modifie « Sélecteurs stables » et « Navigation D-pad des trois zones » ; ce change n'ajoute que deux exigences nouvelles, sans recouvrement d'en-tête.

**Corrections de la PR #17 reprises par la page d'un seul tenant**
- La PR #17 a donné à la grille une seule autorité de défilement (`domain/GridScroll.kt`, `LocalBringIntoViewSpec` neutralisé dans la grille, aucune remise à zéro en quittant la zone) et partagé le repli de focus du dock et de la grille (`domain/FocusFallback.kt`, `ui/home/TileFocus.kt`). Ce change les réutilise sans les dupliquer : `GridScroll` reçoit un plancher de défilement paramétrable (le haut du bloc grille dans la page), la neutralisation de `bringIntoView` s'étend à toute la page, et le seul `ScrollState` de la page remplace celui de la grille (`design.md`, D1).
- L'engrenage de la PR #17 (opaque, sans fond, `AnimatedVisibility` en fondu sur le héro) garde son dessin ; seul son fondu est remplacé par le défilement avec le héro.

## Impact
- **Code accueil** : `HomeScreen.kt` (`ui.home`) (couches alpha `heroAlpha` / `gridAlpha`, `LAYER_FADE_MS`, position du dock, `AnimatedVisibility` de l'engrenage), `HomeGrid.kt` (`ui.home`) (`verticalScroll` propre et `LocalBringIntoViewSpec` local remplacés par la page, calcul « focus toujours visible » du Top Shelf reciblé sur la page), `Dock.kt` (`ui.home`) (alpha et `active` du verre), `domain/GridScroll.kt` (plancher paramétrable), nouveau `domain/HomePage.kt` (cible de défilement de la page).
- **Code réglages** : `SettingsScreen.kt` (`ui.settings`) (suppression de `hiddenSubScreen`), `HiddenAppsScreen.kt` (`ui.settings`) (supprimé, remplacé par un contenu de volet), `SettingsContent.kt` (`ui.settings`), `SettingsViewModel.kt` (`ui.settings`) (tri, liste affichée distincte de l'état persisté, comptages intégrés au tri).
- **Persistance** : `data/LauncherPrefs.kt` (nouvelle clé de dates de masquage à côté de `hidden_apps`), `data/AppCatalogRepository.kt` (horodatage au masquage), nouvelles fonctions de tri pures dans `domain/`.
- **Tests** : `HomeNavigationTest`, `HiddenAppsScreenTest`, `SettingsNavigationTest`, `SettingsViewModelTest`, `LauncherPrefsTest`, `SettingsEntryTest` (l'engrenage reste composé hors écran en vue grille au lieu de disparaître en fondu) à reprendre ; `HomeGridScrollTest`, `CatalogUpdateFocusTest`, `GridScrollTest`, `FocusFallbackTest` de la PR #17 doivent passer sans changement ; nouveaux tests JUnit de tri, de compatibilité de lecture et de cible de défilement.
- **README** : section « Navigation » (grille qui « couvre le héro » → défilement) et « Settings ».
- Aucune dépendance ajoutée.

## Non-goals
- Aucun changement des paliers ni des règles de focus (seule la zone active est focusable, Retour → héro) : reportés à aucune phase, ils restent tels quels.
- Le contenu de la rangée Up Next, sa position et ses réglages : `p2c-upnext`, implémenté dans une itération ultérieure, après ce change.
- Les corrections de la PR #17 (dessin de l'engrenage, rafraîchissement grille/dock, règle de placement du Top Shelf, focus d'une app disparue) : non re-spécifiées ici, hormis le fondu de l'engrenage et le scénario « sortie et retour dans la grille », reformulés pour le défilement.
- Le périmètre du compteur « programmes publiés » (aujourd'hui `PreviewPrograms` seulement) : question ouverte de `p2c-upnext`, non tranchée ici ; le tri utilise le compteur tel qu'il est affiché.
- Défilement libre de la page à la molette ou au pointeur, geste tactile : hors sujet (D-pad uniquement).
- Thème clair, tests de régression visuelle : phases ultérieures (voir `ui-testing`).

## Questions ouvertes
Aucune : les choix produit de ce change sont tranchés par Sygix et reportés dans les specs. Le périmètre du compteur reste une question de `p2c-upnext` (voir « Non-goals »).
