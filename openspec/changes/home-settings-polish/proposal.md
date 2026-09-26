# Change : home-settings-polish

## Why
Trois finitions demandées par Sygix après l'usage quotidien du launcher : la transition héro ↔ grille est aujourd'hui un fondu croisé de couches (alpha animé sur le héro, le dégradé et la grille, le dock suivant l'alpha du héro) qui saccade sur la TV de test ; le sous-écran « Applications cachées » ajoute un palier de navigation inutile dans les réglages ; et les deux listes des réglages sont triées par ordre alphabétique uniquement, ce qui noie les apps utiles (celles qui publient du contenu, celles qu'on vient de cacher).

## What Changes
- **Accueil, transition héro ↔ grille en défilement vertical continu** : l'accueil devient une page d'un seul tenant. Bas depuis le dock fait défiler la page d'environ un écran (le héro sort par le haut pendant que la grille remonte) ; Haut depuis la première rangée fait le mouvement inverse. Le dock et l'engrenage des réglages partent avec le héro : ils ne sont jamais visibles en vue grille. Les trois paliers (héro → dock → grille, Retour → héro) et les règles de focus sont inchangés, seul le rendu de la transition change (plus de fondu de couches).
- **Réglages, « Applications cachées » sans sous-écran** : la liste des apps cachées s'affiche directement dans le volet de droite quand la catégorie est sélectionnée, avec le bouton « Tout réactiver » au-dessus de la liste. Chaque ligne garde un switch ; réactiver une app ne retire pas sa ligne tant qu'on reste dans la catégorie (on peut la recacher si on s'est trompé), la liste est recalculée quand on quitte la catégorie (autre catégorie ou fermeture des réglages) ; « Tout réactiver » agit sans confirmation et suit la même règle. Le focus initial du volet est la première ligne, « Tout réactiver » est atteint par Haut. État vide et navigation D-pad complète conservés.
- **Tri intelligent des deux listes** : « Apps sources » place d'abord les apps qui publient du contenu, par nombre de programmes décroissant (ordre alphabétique à égalité), puis celles à 0 par ordre alphabétique ; l'ordre est figé à l'entrée dans la catégorie (les compteurs se mettent à jour sur place, les lignes ne bougent pas sous le focus). « Applications cachées » place la plus récemment masquée en premier ; les apps cachées avant ce change (sans date) vont en fin de liste par ordre alphabétique.
- **Persistance** : toute app cachée à partir de ce change est datée ; l'ancien format (`hidden_apps`, simple ensemble) reste lu tel quel, sans perte ni migration destructive.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `launcher-shell` : « Navigation 3 paliers » (rendu de la transition en défilement, dock et engrenage jamais visibles en vue grille, Retour depuis la grille), « Fond de la zone grille » (le héro est masqué parce qu'il est sorti de l'écran, plus par alpha), « Dock d'apps épinglées » (le dock part avec le héro).
- `settings` : « Applications cachées » (liste dans le volet de droite, bouton au-dessus, recalcul à la sortie de la catégorie, focus initial sur la première ligne, tri, état vide, D-pad), « Apps sources » (tri par nombre de programmes, figé à l'entrée dans la catégorie), « Cacher une application » (date de masquage enregistrée), « Page de réglages » (exception de focus initial pour « Applications cachées »), « Icône réglages flottante » (engrenage solidaire du héro).
- `ui-testing` : ADDED « Couverture de la transition héro ↔ grille » et « Couverture du volet Applications cachées ». `ui-testing` est archivé sur `main` ; ces deux exigences sont nouvelles et ne modifient aucune exigence existante (« Navigation D-pad des trois zones » et « Sélecteurs stables » restent valables telles quelles), d'où des ADDED et non des MODIFIED.

## Dépendances et chevauchements
Deltas écrits sur `main` après le merge de la PR #13 (`p2c-upnext`, spec seule, non archivée) et l'archivage de `p2b-settings` et `ui-testing`.

**Dépendances**
- `p2b-settings` (archivé, `openspec/specs/settings/spec.md`) et `ui-testing` (archivé, `openspec/specs/ui-testing/spec.md`).
- **Ordre retenu** (décision Sygix) : `fix/home-ui-bugs` (PR de bugs, en cours) → ce change → implémentation d'Up Next (`p2c-upnext`) dans une itération ultérieure. L'implémentation de ce change repart donc de la version de `fix/home-ui-bugs` ; ses corrections sur `HomeGrid` (défilement interne et saut de la grille) sont reprises par la structure de page à `ScrollState` unique (`design.md`, D1), qui remplace ce code.

**`p2c-upnext` (sur `main`, implémenté et archivé après ce change)**
- `launcher-shell` « Navigation 3 paliers » : `p2c-upnext` y intègre la rangée Up Next comme première (ou dernière) ligne de la zone grille ; ce change y remplace le fondu par un défilement. Même en-tête : `p2c-upnext`, archivé en second, reprend le texte de ce change (« la zone grille » désigne alors la rangée Up Next et les rangées d'apps ; le défilement amène la première ligne de la zone grille en haut de l'écran, quelle qu'elle soit).
- `launcher-shell` « Rangée Up Next », « Sélection des apps sources » : touchées par `p2c-upnext` seulement.
- `settings` « Page de réglages » : `p2c-upnext` y ajoute la catégorie « Écran d'accueil » ; ce change y ajoute l'exception de focus initial d'« Applications cachées ». Même en-tête : `p2c-upnext` reprend l'exception au rebase.
- `settings` « Apps sources » et « Cacher une application » : `p2c-upnext` y ajoute « et la rangée Up Next » dans le périmètre des apps sources ; ce change y ajoute le tri figé (Apps sources) et la date de masquage (Cacher une application). Même en-tête : `p2c-upnext` fusionne les deux textes au rebase.
- `ui-testing` : `p2c-upnext` modifie « Sélecteurs stables » et « Navigation D-pad des trois zones » ; ce change n'ajoute que deux exigences nouvelles, sans recouvrement d'en-tête.

**Branche `fix/home-ui-bugs` (en cours, mergée avant ce change)**
- Corrige la visibilité de l'engrenage, le rafraîchissement de la grille et du dock après une action, le Top Shelf et un saut de la grille. **Ce change ne re-spécifie aucun de ces points** : les scénarios ci-dessous supposent ces corrections acquises (le dock et la grille reflètent l'état persisté ; l'engrenage est visible sur le héro conformément à « Icône réglages flottante » de `settings`, et en sort avec lui au défilement).
- Zone de contact : le « saut de la grille » et la remise à zéro du défilement interne de la grille (`HomeGrid` remet son `scrollState` à 0 quand la zone grille perd le focus) sont le code que la page d'un seul tenant remplace ; ces corrections sont reprises par la structure à `ScrollState` unique.

## Impact
- **Code accueil** : `HomeScreen.kt` (`ui.home`) (couches alpha `heroAlpha` / `gridAlpha`, `LAYER_FADE_MS`, position du dock et de l'engrenage), `HomeGrid.kt` (`ui.home`) (défilement interne, remise à zéro à la perte du focus, calcul « focus toujours visible » du Top Shelf), `Dock.kt` (`ui.home`) (alpha et `active` du verre), `HeroStage.kt` (`ui.hero`) (`visible` / `active` dérivés de la position de la page).
- **Code réglages** : `SettingsScreen.kt` (`ui.settings`) (suppression de `hiddenSubScreen`), `HiddenAppsScreen.kt` (`ui.settings`) (supprimé, remplacé par un contenu de volet), `SettingsContent.kt` (`ui.settings`), `SettingsViewModel.kt` (`ui.settings`) (tri, liste affichée distincte de l'état persisté, comptages intégrés au tri).
- **Persistance** : `data/LauncherPrefs.kt` (nouvelle clé de dates de masquage à côté de `hidden_apps`), `data/AppCatalogRepository.kt` (horodatage au masquage), nouvelles fonctions de tri pures dans `domain/`.
- **Tests** : `HomeNavigationTest`, `HiddenAppsScreenTest`, `SettingsNavigationTest`, `SettingsViewModelTest`, `LauncherPrefsTest` à reprendre ; nouveaux tests JUnit de tri et de compatibilité de lecture.
- **README** : section « Navigation » (grille qui « couvre le héro » → défilement) et « Settings ».
- Aucune dépendance ajoutée.

## Non-goals
- Aucun changement des paliers ni des règles de focus (seule la zone active est focusable, Retour → héro) : reportés à aucune phase, ils restent tels quels.
- Le contenu de la rangée Up Next, sa position et ses réglages : `p2c-upnext`, implémenté dans une itération ultérieure, après ce change.
- Les corrections de `fix/home-ui-bugs` (engrenage, rafraîchissement grille/dock, Top Shelf, saut de la grille) : non re-spécifiées ici.
- Le périmètre du compteur « programmes publiés » (aujourd'hui `PreviewPrograms` seulement) : question ouverte de `p2c-upnext`, non tranchée ici ; le tri utilise le compteur tel qu'il est affiché.
- Défilement libre de la page à la molette ou au pointeur, geste tactile : hors sujet (D-pad uniquement).
- Thème clair, tests de régression visuelle : phases ultérieures (voir `ui-testing`).

## Questions ouvertes
Un seul point reste non tranché ; il n'apparaît ni en SHALL ni comme décision dans `design.md` ou `tasks.md` :

1. **Premier comptage pas encore reçu à l'entrée dans « Apps sources ».** Les compteurs arrivent de façon asynchrone (première valeur vide). « Apps sources » étant la première catégorie, l'entrée coïncide avec l'ouverture des réglages : appliqué tel quel, l'ordre « calculé avec les compteurs connus à l'entrée » serait alors alphabétique, et figé jusqu'à la prochaine entrée. Options : (a) accepter ce cas ; (b) attendre la première émission du comptage, avec un délai borné, avant de figer l'ordre ; (c) démarrer le comptage plus tôt (dès le home) pour qu'il soit connu à l'ouverture.
