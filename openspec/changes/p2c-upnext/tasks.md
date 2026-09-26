# Tasks : p2c-upnext

## 1. Prérequis de spec

- [x] 1.1 Après archivage de `p2b-settings` (main `423dbbd`) : MODIFIED « Page de réglages », « Apps sources » et « Cacher une application » posés dans `specs/settings/spec.md` de ce change ; vérifié par `openspec validate --all --strict` vert et `openspec/specs/settings/spec.md` présent
- [ ] 1.2 `jellyfin-tvprovider-only` embarqué dans cette PR et `ui-testing` archivé (commit dédié) : vérifié par `openspec validate --all --strict` vert et `openspec/specs/ui-testing/spec.md` contenant « Navigation D-pad des trois zones »

## 2. Modèle et source

- [ ] 2.1 Modèle canonique `UpNextItem` (champ `externalIds` facultatif + liste des sources du contenu conservée après fusion) et interface `UpNextSource` renvoyant un `Result` ; vérifié par un test JUnit qui distingue erreur et vide
- [ ] 2.2 Extraire de `TvProviderHeroSource` les helpers `Cursor.optString` / `optLong` / `optInt`, `landscapeImage()` et `HeroOrdering.progressRatio()` dans un helper partagé ; vérifié par `./gradlew test` vert sans duplication des helpers (grep unique)
- [ ] 2.3 Source TV Provider : requête `ContentResolver` sur `Dispatchers.IO` avec `withTimeout` ~2 s, `WatchNextPrograms` uniquement, filtre `COLUMN_BROWSABLE`, exclusion des `COLUMN_TYPE` non épisode/film, mappeur `Cursor` → `UpNextItem` pour toutes les apps visibles, chaque colonne facultative selon « Champs facultatifs » ; vérifié par des tests JUnit du mapping (chaque colonne absente une à une, `SecurityException`, provider absent, timeout)

## 3. Logique métier

- [ ] 3.1 Normalisation des titres + dédoublonnage niveaux 1 à 4 (niveau 5 = P4, via `externalIds`), gagnant `CONTINUE` > `NEXT`/`NEW` > `WATCHLIST`, puis engagement le plus récent, puis ordre de préférence (constante) ; vérifié par des tests JUnit par niveau, y compris faux positifs (remake année différente, même titre types différents) et cas de fusion (même titre même année, année inconnue d'un côté)
- [ ] 3.2 Tri : `CONTINUE` puis `NEXT`/`NEW` puis `WATCHLIST`, chaque groupe par `last_engagement_time` décroissant et à égalité `_ID` croissant, tri stable, limite 20 ; vérifié par des tests JUnit (ordre des groupes, égalités, limite)
- [ ] 3.3 Filtre des apps sources : filtrer les items Up Next via `disabledSources` / `filterBySources` existants (`HomeViewModel.kt`, `domain/HeroFeed.kt`) **avant** le dédoublonnage ; les apps cachées ne filtrent pas ; vérifié par un test JUnit (source désactivée ni gagnante ni dans les sources du menu ; app cachée toujours présente) et un test Compose (bascule du switch → carte disparue sans redémarrage)

## 4. Intégration et UI

- [ ] 4.1 Intégration `HomeViewModel`/`HomeState` : état de la rangée dans le uniflow existant, déclencheurs de refresh (retour au premier plan, permission accordée : ajouter la rangée aux rappels de `MainActivity`) + `ContentObserver` sur `WatchNextPrograms.CONTENT_URI` sur le patron de `programCountsFlow()` (protégé contre `SecurityException` et provider absent) ; vérifié par des tests JUnit du ViewModel (rechargement sur signal, contenu précédent conservé, observer refusé sans crash)
- [ ] 4.2 UI rangée : ligne de la zone grille (première ou dernière selon le réglage), carte 16:9 (image plein cadre sinon poster portrait centré), seuil de qualité propre (largeur ≥ 2 × la largeur affichée, sinon placeholder), texte `SxxEyy` + titre d'épisode, barre de progression `CONTINUE`, badge icône app via `PackageManager`, squelette au premier chargement > 300 ms seulement, carte « Réessayer » ; testTags `zone-upnext`, `upnext-card-<key>` ; vérifié par des tests Compose à la taille TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) sur chaque état
- [ ] 4.3 Navigation D-pad de la zone grille avec la rangée : descente dock → rangée → apps, remontée, rangée sautée si masquée, position « après la grille », rangée devenue vide (focus sur la rangée d'apps adjacente), restauration du focus au retour d'une app, Retour → héro, aucun panneau Top Shelf sur la rangée ; vérifié par des tests Compose à la taille TV pour chaque scénario du delta ui-testing
- [ ] 4.4 Ouverture : intent publié (`COLUMN_INTENT_URI`) puis lancement de l'app source en repli (+ toast en cas d'échec depuis le menu), sans crash ; vérifié par un test JUnit du routage (intent absent, `ActivityNotFoundException`, `SecurityException`)
- [ ] 4.5 Généralisation du menu contextuel (`AppContextMenu` découplé de `TvApp`) : « Ouvrir avec… » sur une carte, ouvert même avec une seule entrée, aucune persistance, testTag `upnext-menu` ; vérifié par un test Compose (ouverture à une et plusieurs entrées, OK, Retour, focus rendu à la carte)
- [ ] 4.6 Réglage « Position d'Up Next » : catégorie « Écran d'accueil » dans `SettingsCategory`, contrôle avant la grille (défaut) / après la grille, navigation D-pad du contrôle, persistance DataStore (`LauncherPrefs`), application immédiate sur le home ; vérifié par un test JUnit de persistance et un test Compose de navigation du contrôle et du repositionnement de la rangée

## 5. Finition

- [ ] 5.1 Chaînes FR en ressources (aucun texte en dur) ; vérifié par un grep des chaînes des composables Up Next et du réglage
- [ ] 5.2 README : rangée Up Next (toutes apps, dédoublonnage) et catégorie « Écran d'accueil » documentées, ligne P2c du roadmap mise à jour ; vérifié par relecture du diff
- [ ] 5.3 `./gradlew test assembleRelease` verts (aucune configuration propre à une machine) et validation sur la TV réelle en `assembleRelease` : rangée, navigation, réglage de position, menu
- [ ] 5.4 `openspec validate --all --strict` vert, puis `openspec archive p2c-upnext` après merge sur main, après la validation sur la TV ; vérifié par `openspec/specs/up-next/spec.md` créée avec son Purpose (le delta up-next en porte un)
