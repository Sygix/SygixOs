# Design

## Context
Voir `proposal.md` (« Why »). État du code sur `main` (`423dbbd`), vérifié fichier par fichier :

- **Transition héro ↔ grille** (`HomeScreen.kt` (`ui.home`), `LauncherHome`) : trois couches empilées dans un `Box` plein écran (`HeroStage`, `AmbientGradient`, `HomeGrid`), plus le `Dock` aligné en bas et l'engrenage en haut à droite. Le passage de zone anime deux alphas (`heroAlpha`, `gridAlpha`, `tween(Motion.LAYER_FADE_MS, AppleEasing)`) ; le dock reçoit `heroAlpha`, le dégradé `gridAlpha`. Chaque couche est une `hazeSource` (verre du dock et du menu). `HeroStage` reçoit `active` (focus) et `visible` (lecture vidéo, Ken Burns, avance automatique) dérivés de `zone != GRID`. `Dock` dérive `active` du verre de `alpha > 0.01f`.
- **Grille** (`HomeGrid.kt` (`ui.home`)) : `Column` avec `verticalScroll(scrollState)` propre à la grille ; un `LaunchedEffect(openRow, focusedApp)` calcule la cible de défilement pour garder la rangée focusée et le panneau Top Shelf visibles (`animateScrollBy`, `Motion.SHELF_EXPAND_MS`) ; `LaunchedEffect(focusEnabled)` remet `scrollState` à 0 et ferme le panneau quand la grille perd le focus. `gridRow` (rangée focusée) remonte à `LauncherHome` pour décider que haut depuis la rangée 0 quitte la grille.
- **Réglages** (`SettingsScreen.kt` (`ui.settings`)) : `pane` (CATEGORIES / CONTENT) et `hiddenSubScreen` ; la catégorie « Applications cachées » n'affiche qu'un bouton d'entrée (`HiddenCategoryContent`, testTag `open-hidden`) qui ouvre `HiddenAppsScreen` (plein écran, zIndex 22, bouton « Tout réactiver » **sous** la liste, ligne retirée dès la réactivation, focus reporté sur la voisine). `Key.Back` ferme les réglages depuis n'importe quel volet ; droite sur une catégorie dont le contenu n'a rien de focalisable laisse le focus sur la catégorie (`contentFocus.tryRequestFocus()` échoue → `pane = CATEGORIES`).
- **État des réglages** (`SettingsViewModel.kt` (`ui.settings`)) : `state` combine `disabledSources`, `allApps`, `hidden` ; les deux listes sont triées par `label.lowercase()` ; `hiddenApps` est le filtre de cette liste. Les compteurs sont un `StateFlow` **séparé** (`counts`, `programCountsFlow()` avec `WhileSubscribed`), lu ligne par ligne via `derivedStateOf` pour ne recomposer que la ligne dont le compte change. `programCounts()` ne compte que les `PreviewPrograms` browsables (pas les `WatchNextPrograms`) : le tri utilise ce compteur tel qu'affiché (périmètre : question ouverte de `p2c-upnext`, hors de ce change).
- **Persistance** (`data/LauncherPrefs.kt`) : `hidden_apps` est un `stringSetPreferencesKey` ; `hideApps` retire aussi l'épinglage dans le même `edit` ; `updateGridOrder` lit l'ensemble caché pour conserver la position des apps cachées ; `unhideApps` / `setHidden(emptySet())` pour la réactivation. Aucune date nulle part.

## Goals / Non-Goals
**Goals :**
- Une seule animation de défilement pour héro ↔ grille, pilotée par un seul état, sans double défilement ni remise à zéro brutale.
- Le dock est géométriquement solidaire du héro (même translation), donc jamais sur la grille, sans logique d'alpha.
- Le volet « Applications cachées » réutilise la mécanique de contenu de volet existante (`SourcesContent`), avec une liste affichée découplée de l'état persisté.
- Tri en fonctions pures testables en JUnit ; dates de masquage persistées de façon additive et rétro-compatible.

**Non-goals :**
- Ne pas réécrire la logique du Top Shelf ni la sélection des apps sources ; ne pas toucher aux corrections de `fix/home-ui-bugs`.
- Ne pas décider la question ouverte de `proposal.md` (premier comptage pas encore reçu à l'entrée dans « Apps sources ») : D7 prend l'ordre avec la valeur des compteurs disponible à l'entrée ; la réponse ne change que cette valeur (attente bornée ou comptage démarré plus tôt), pas la mécanique de l'instantané.

## Decisions

### D1. Page d'un seul tenant : un seul `ScrollState` pour héro + grille
- `LauncherHome` compose une `Column` défilante (`verticalScroll(pageScroll)`) : premier enfant, le héro à hauteur exacte du viewport (`fillParentMaxHeight` ou hauteur mesurée par `BoxWithConstraints`) avec le dock **dans** ce premier bloc (aligné en bas de ce bloc, pas de l'écran) ; second enfant, le contenu de la grille sur le dégradé neutre. Le changement de zone anime `pageScroll` vers 0 (héro) ou vers la hauteur du héro (grille) avec `animateScrollTo(target, tween(Motion.LAYER_FADE_MS, AppleEasing))` : la durée est celle déjà utilisée pour cette transition (400 ms, dans la plage « Focus tvOS »), la constante peut être renommée en `Motion.PAGE_SCROLL_MS` sans changer la valeur.
- `HomeGrid` perd son `verticalScroll` propre et reçoit `pageScroll` (et l'offset du bloc grille) : son calcul « focus toujours visible » (rangée + panneau Top Shelf) cible désormais `pageScroll`, avec un plancher égal à la hauteur du héro tant que la zone grille est active. La remise à zéro `scrollState.scrollTo(0)` à la perte du focus disparaît : c'est l'animation de remontée qui ramène la page.
- Alternative rejetée : garder deux défilements (page + grille) — double défilement, remise à zéro visible (le « saut de la grille » de `fix/home-ui-bugs`), et l'état de la page dépend de deux sources.
- Alternative rejetée : `graphicsLayer { translationY }` sur les couches sans `ScrollState` — plus léger, mais la grille défilante devrait alors être clipée et son propre défilement coordonné à la main ; un `ScrollState` unique donne gratuitement `animateScrollTo`, l'interruption (« continuité de l'animation ») et la compatibilité avec le calcul du Top Shelf.

### D2. Interruption et cibles
- Toute pression haut/bas/Retour annule l'animation en cours et relance `animateScrollTo` depuis la position courante (`ScrollState` le fait nativement : un nouveau `animateScrollTo` remplace le précédent). `zone` reste la seule source de vérité du focus ; la position de la page en dérive (cible = 0 si `zone != GRID`, sinon `≥ heroHeight`).
- Retour depuis une rangée profonde : une seule `animateScrollTo(0)` à durée fixe (la vitesse varie avec la distance, comme le défilement Top Shelf existant).

### D3. Visibilité du héro et économie de ressources dérivées de la position
- `HeroStage.visible` = `zone != GRID` (inchangé : pause dès qu'on part vers la grille, reprise dès qu'on remonte). La libération du lecteur et l'arrêt du flou (« Préchargement et mémoire ») restent déclenchés par la zone, pas par le pixel près : le héro est hors écran à la fin de l'animation et en pause dès son début.
- `Dock.active` (flou du verre) = `zone != GRID` au lieu de `alpha > 0.01f` ; `alpha` du dock devient constant à 1 (paramètre supprimé). Pendant la descente, le verre du dock ne rafraîchit plus son flou (la source qu'il reflète, le héro, est en pause) : c'est une des sources probables des saccades observées, avec les trois `hazeSource` animées en alpha simultanément.
- Sources de saccades à mesurer sur la TV (`assembleRelease`) : flou Haze pendant le défilement, Ken Burns / vidéo du héro encore en cours au premier frame, recomposition de toutes les tuiles à chaque frame si `pageScroll.value` est lu en composition (le lire uniquement dans `graphicsLayer` / `Modifier.offset { }`).

### D4. Engrenage solidaire du héro
- `SettingsGear` est composé **dans** le bloc héro (aligné en haut à droite de ce bloc), comme le dock : il suit la translation de la page sans logique propre, sort par le haut à la descente et n'est jamais visible en vue grille (« Icône réglages flottante » de `settings`). Son focus reste celui d'aujourd'hui (haut depuis le héro), actif seulement quand `zone != GRID`.

### D5. Volet « Applications cachées » : liste affichée découplée de l'état persisté
- `SettingsViewModel` expose `hiddenRows: List<HiddenRow>` (`HiddenRow(app, hidden: Boolean, hiddenAt: Long?)`). La **composition** de la liste (quelles apps, dans quel ordre) est un instantané pris au moment du recalcul ; l'**état** `hidden` de chaque ligne est vivant (combiné au flux `hidden` de `LauncherPrefs`), ce qui donne le switch « visible » sans retirer la ligne.
- Un seul point d'entrée `refreshHiddenRows()` recalcule l'instantané (tri de D7). La spec recalcule la liste quand on quitte la catégorie (autre catégorie ou fermeture des réglages) : l'instantané est donc pris à chaque **entrée** dans la catégorie (catégorie devenue active, y compris à l'ouverture des réglages) et reste inchangé tant qu'elle est active. Les deux formulations sont indiscernables pour l'utilisateur, la liste n'étant visible que dans la catégorie, et l'entrée couvre aussi les apps cachées depuis le home entre deux ouvertures. `SettingsScreen` appelle le ViewModel quand `activeCategory` change ; les passages de focus entre volets (`pane`) ne déclenchent rien.
- OK sur une ligne : `hidden ? unhide(pkg) : hide(pkg)` ; `hide` passe par `AppCatalogRepository.hideApp` (retire l'épinglage, date le masquage). « Tout réactiver » : `unhideAll()` sur les apps de l'instantané.
- Alternative rejetée : garder une liste dérivée du flux `hidden` et « geler » les lignes en cours de retrait avec un délai — état dupliqué et non déterministe.

### D6. Volet « Applications cachées » : UI
- `HiddenAppsScreen` supprimé ; nouveau `HiddenContent` dans `SettingsContent.kt` : `LazyColumn` (`listState` de `SettingsScreen`, comme `SourcesContent`) avec en premier item le bouton « Tout réactiver » (`SettingsEntryButton` existant, testTag `unhide-all`), puis une ligne par `HiddenRow` (`tvFocus` + `tvClickable`, `AppleSwitch(checked = row.hidden)`, testTags `hidden-row-<pkg>` / `hidden-switch-<pkg>`), sémantique `Role.Switch` + `toggleableState` reflétant `hidden`. Conteneur testTag `hidden-pane`. État vide : texte centré testTag `hidden-empty`, rien de focalisable (le mécanisme existant garde alors le focus sur la catégorie).
- `contentFocus` est posé sur la **première ligne** (premier `HiddenRow`), pas sur le bouton : haut depuis la première ligne atteint « Tout réactiver » par le déplacement naturel de la `LazyColumn`. C'est l'exception de « Page de réglages » ; les autres catégories gardent `contentFocus` sur leur premier élément.
- `SettingsScreen` : suppression de `hiddenSubScreen` et de la branche `Key.Back` associée ; les bords haut/bas restent gérés par la `LazyColumn` (pas de boucle) ; gauche → `pane = CATEGORIES` (existant).
- Chaînes : libellés déjà en dur dans `SettingsContent.kt` (« Aucune application cachée », « Tout réactiver ») ; les passer en ressources FR (`strings.xml`) pour les chaînes touchées.

### D7. Tri : fonctions pures dans `domain/`
- `SettingsOrdering.sources(apps, counts: Map<String, Int>)` : partition `count > 0` / `count == 0` ; première partie par `count` décroissant puis `label.lowercase()` à égalité, seconde par `label.lowercase()`.
- `SettingsOrdering.hidden(apps, dates: Map<String, Long>)` : datées par `hiddenAt` décroissant, puis non datées par `label.lowercase()`.
- Pour « Apps sources », l'ordre est un **instantané** pris à l'entrée dans la catégorie (même déclencheur que D5 : `activeCategory` devient « Apps sources », y compris à l'ouverture des réglages) avec la valeur courante de `counts` ; `state` ne combine pas `counts`, donc une nouvelle émission ne réordonne rien. Le compteur affiché par ligne reste lu via `derivedStateOf` comme aujourd'hui et se met à jour sur place.

### D8. Persistance des dates de masquage : clé additive, l'ensemble reste la vérité
- `hidden_apps` (ensemble) reste la source de vérité de l'appartenance : `catalog`, `updateGridOrder`, `unhideApps`, `setHidden` continuent de le lire et de l'écrire sans changement.
- Nouvelle clé `hidden_apps_dates` (`stringPreferencesKey`, lignes `package\tepochMillis`, mêmes séparateurs `LINE` / `FIELD` que `cached_apps`). `hideApps(vararg)` écrit l'ensemble **et** la date (`clock()` injecté, défaut `System.currentTimeMillis`) dans le même `edit` ; `unhideApps` retire l'entrée de date ; `setHidden(emptySet())` vide aussi les dates.
- Lecture : `hiddenWithDates: Flow<Map<String, Long?>>` = chaque package de l'ensemble, associé à sa date si présente, `null` sinon. Une entrée de date sans package dans l'ensemble est ignorée (et nettoyée à la prochaine écriture). Aucune migration au démarrage : l'ancien format est lu tel quel, l'app sans date est « sans date » jusqu'à un nouveau masquage.
- Alternative rejetée : remplacer l'ensemble par une map sérialisée et migrer au premier démarrage — touche quatre lecteurs de l'ensemble, une migration à tester et à rendre idempotente, sans bénéfice.

## Risks / Trade-offs
- [Saccades persistantes malgré le défilement] → mesurer sur la TV en `assembleRelease` avec, un à un : flou Haze désactivé pendant l'animation, vidéo du héro mise en pause avant le premier frame, `pageScroll.value` lu hors composition ; conserver la combinaison la plus fluide, documentée dans la PR.
- [Calcul Top Shelf dépendant de la hauteur du héro] → la hauteur est celle du viewport (`BoxWithConstraints`) ; test Compose à la taille TV vérifiant que la rangée focusée reste visible avec panneau ouvert après le changement de base de défilement.
- [Interruption de l'animation laissant la page entre deux positions] → la cible ne dépend que de `zone` ; test « interruption de l'animation » de `ui-testing`.
- [Deux changes modifient « Navigation 3 paliers »] → règle de fusion documentée dans `proposal.md` ; l'implémenteur du second reprend le texte du premier archivé.
- [Liste cachée figée montrant un état obsolète] → l'état `hidden` de chaque ligne est vivant, seule la composition est figée ; elle est recalculée à chaque entrée dans la catégorie (D5).
- [Ordre d'« Apps sources » obsolète quand les compteurs changent] → assumé : l'ordre est figé tant que la catégorie est active, pour que les lignes ne bougent jamais sous le focus ; il est recalculé à la prochaine entrée. Le cas d'un premier comptage pas encore reçu à l'entrée est la question ouverte de `proposal.md`.

## Migration Plan
- Déploiement : aucune étape ; la clé `hidden_apps_dates` est créée au premier masquage après mise à jour. Les apps déjà cachées restent cachées, sans date, en fin de liste.
- Retour arrière : une version antérieure ignore `hidden_apps_dates` et continue de lire `hidden_apps` ; rien à défaire.
- Tests : `LauncherPrefsTest` avec un DataStore pré-rempli au format ancien (ensemble seul) → `hiddenWithDates` renvoie `null` pour ces apps, le catalogue les exclut toujours ; masquage puis réactivation puis masquage → une seule entrée de date, la plus récente.
