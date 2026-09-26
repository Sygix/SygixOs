# Design : p2c-upnext

## Décisions

### Source : TV Provider, toutes les apps
La rangée lit `WatchNextPrograms` (et `PreviewPrograms` pour retirer les doublons publiés deux fois) via `ContentResolver` sous `READ_TV_LISTINGS`, sans filtrer sur le package : **toutes les apps visibles** alimentent la rangée. La requête s'exécute sur `Dispatchers.IO` avec un `withTimeout` court (~2 s) — aucun appel réseau, aucun timeout de 10 s.

**Hypothèses à vérifier sur l'appareil (tâche 1, avant tout code)** :
- quelles lignes `WatchNextPrograms` des autres apps le launcher peut lire sous `READ_TV_LISTINGS` (le TvProvider peut restreindre la lecture aux lignes de l'appelant) ;
- quels packages publient réellement, et quelles colonnes ils remplissent (titre de série, `episode_title`, numéros de saison/épisode, `internal_provider_id`, `content_id`, `intent_uri`, `poster_art_uri`, `watch_next_type`, `last_engagement_time`, position de lecture).

D'après `jellyfin-androidtv` (`LeanbackChannelWorker.getBaseItemAsWatchNextProgram`), les programmes Jellyfin portent `internal_provider_id`, le type, le titre de série, `episode_title`, les numéros de saison et d'épisode, `watch_next_type` et un intent qui ouvre la **fiche** de l'item (`StartupActivity` + `ItemId`) — pas la lecture. Le verbe de la rangée est donc « ouvrir », pas « lire » : l'app affiche sa fiche ou reprend selon son propre comportement. `release_date` est absent des programmes Jellyfin : le tri ne s'en sert pas.

### Modèle canonique
`UpNextItem` : `id` (`_ID` du programme), package source, type (épisode / film), titre de série, saison, épisode, titre d'affichage, poster, progression optionnelle (0–1, position / durée quand les deux colonnes existent), `watchNextType` (`CONTINUE`, `NEXT`, `NEW`, `WATCHLIST`), timestamp d'activité (`last_engagement_time`), `intentUri` (`COLUMN_INTENT_URI`), nom de l'app source, et un champ **`externalIds` facultatif** (IMDb / TVDB) — vide en p2c, rempli en P4 par l'enrichissement BetaSeries.

`UpNextSource` est une interface aux frontières (SOLID) : retourne un `Result<List<UpNextItem>>` pour que l'UI distingue **erreur** et **vide** (jamais d'exception avalée, contrairement à `TvProviderHeroSource.load()` qui avale via `runCatching {...}.getOrDefault(emptyList())`).

### Dédoublonnage en 5 niveaux
Clé d'identité calculée à partir des colonnes du TV Provider, de la plus fiable à la moins fiable :

| Niveau | Clé | Rôle |
|---|---|---|
| 1. Dans une app | `package_name` + `internal_provider_id`, sinon `content_id`, sinon `intent_uri` | Retirer les doublons exacts (y compris ceux publiés aussi en `PreviewPrograms`) |
| 2. Série dans une app | `package_name` + titre de série normalisé | Au plus une carte par série : l'épisode en cours prime sur l'épisode suivant |
| 3. Épisode entre apps | titre de série normalisé + saison + épisode | Même épisode sur deux apps |
| 4. Film entre apps | titre normalisé + année | Pas de fusion si les deux années sont connues et différentes (remakes) |
| 5. P4 | ID IMDb/TVDB ajouté aux items par l'enrichissement BetaSeries | Prend le pas sur les niveaux 3 et 4 quand il est connu |

**Normalisation** : minuscules, diacritiques retirés (NFKD), ponctuation et `(année)` supprimés, espaces compactés, puis **égalité stricte**. Aucune correspondance approximative : il ne faut jamais fusionner à tort.

**Limite assumée, documentée dans la spec** : les titres localisés différemment selon l'app (« La Casa de Papel » contre « Money Heist ») restent en double jusqu'à P4.

**Gagnant d'un doublon** (décision produit) :
1. une reprise en cours (`CONTINUE` avec progression > 0) bat un « à suivre » (`NEXT`/`NEW`) ;
2. ensuite, l'engagement le plus récent (`last_engagement_time` décroissant) ;
3. à égalité, l'ordre de préférence des apps (constante en p2c : Jellyfin d'abord) tranche.

### Tri (B3)
`release_date` n'est pas fiable (absent côté Jellyfin) et trier globalement par `last_engagement_time` mettrait tous les `NEXT` avant les `CONTINUE` (Jellyfin publie pour `NEXT` un `last_engagement_time` égal à l'heure de synchro, qui tourne toutes les heures). Tri final :
1. items `CONTINUE` d'abord, par `last_engagement_time` décroissant ;
2. puis `NEXT`/`NEW` par `last_engagement_time` décroissant, et à égalité par `_ID` croissant ;
3. tri **stable** (l'ordre d'entrée des niveaux de dédoublonnage est préservé à égalité) ;
4. `WATCHLIST` : exclu de la rangée (une mise de côté volontaire n'est pas un « continuer à regarder ») — à confirmer par Sygix.

Limite : 20 items.

### Position (décision produit)
Une ligne style tvOS en tête de la zone grille, au-dessus des apps ; **aucun nouveau palier**, le héro reste plein écran. Séquence D-pad : héro → dock → Up Next → apps (détaillée dans le delta launcher-shell). Un réglage p2b « Position d'Up Next » permet *avant la grille* (défaut) ou *après la grille* ; le palier est sauté si la rangée est masquée.

**Pas de dédoublonnage entre le héro et Up Next** (décision produit) : le héro peut montrer les mêmes contenus, comme sur tvOS. Écrit explicitement dans la spec pour que personne ne « l'optimise » plus tard.

Cohabitation avec la Top Shelf : le panneau Top Shelf appartient au focus des tuiles de la grille ; tant que le focus est sur la rangée Up Next, aucun panneau n'est ouvert.

### Carte (I4)
- format **16:9 uniforme**, poster portrait centré sur fond sombre ;
- texte : titre de série, `SxxEyy`, titre d'épisode (films : titre seul) ;
- barre de progression pour les `CONTINUE` seulement (progréssement 0–1) ;
- focus tvOS : renvoi à l'exigence « Focus tvOS » de launcher-shell (zoom ~1.1x, ombre douce, easing Apple) ;
- placeholder si le poster échoue ou manque ;
- règle « ≥ 1080 px » de « Qualité des visuels » **non applicable** aux posters portrait Up Next (artwork fourni par les apps, tailles hétérogènes) — le poster est affiché tel quel, centré ;
- badge : petite icône de l'app source prise dans le `PackageManager` (aucun logo de marque embarqué, contrairement à l'ancienne décision d'icônes vectorielles) ;
- budget mémoire : posters Up Next intégrés au mécanisme existant « Préchargement et mémoire » de launcher-shell (même cache Coil), pas de second cache.

### Ouverture et menu « Ouvrir avec… » (décision produit)
Appui OK sur une carte : ouverture de `COLUMN_INTENT_URI` du programme ; si absent ou si l'ouverture échoue (`ActivityNotFoundException`, `SecurityException`), l'app source est lancée (`LeanbackLauncher` du package). Aucun routage par score en p2c.

Appui long : menu « Ouvrir avec… » listant les apps qui possèdent ce contenu (les packages de la clé de doublon retenue), chacune ouverte via **son** intent publié ; OK valide, Retour ferme, le focus revient sur la carte. Rien n'est persisté. Si une seule app possède le contenu, le menu réduit (« Ouvrir avec » à une entrée) est affiché ; le menu contextuel existant (`AppContextMenu.kt`, lié à `TvApp`) est généralisé pour accepter les entrées Up Next.

### États de la rangée (I2, I3)
- **Permission refusée** : rangée **masquée**, sans carte d'erreur (scénario dédié) ;
- **Erreur** (requête en échec, timeout) : carte d'état focusable (message + « Réessayer ») — l'erreur se distingue du vide grâce au `Result` de la source ;
- **Chargement** : squelette au **premier** chargement uniquement, et seulement si la requête dépasse ~300 ms ; aux rechargements, l'état précédent reste affiché jusqu'au résultat (jamais de saut de mise en page) ;
- **Vide** : rangée masquée ;
- **Rafraîchissement (I1)** : retour au premier plan ou permission `READ_TV_LISTINGS` venant d'être accordée → la rangée se recharge, le contenu précédent reste affiché pendant le rechargement. Un `ContentObserver` sur `WatchNextPrograms.CONTENT_URI` est envisagé (tâche dédiée, protégé contre `SecurityException` et provider absent). Hypothèse documentée : Jellyfin ne resynchronise le TV Provider qu'une fois par heure.
- Non-blocage : la navigation du home reste fonctionnelle dans tous les états.

## Notes de test
- Dédoublonnage (chaque niveau), normalisation, gagnant, tri : tests **JUnit** purs sur le mapping `Cursor` → `UpNextItem` → fusion, y compris les faux positifs : remake avec une année différente, même titre mais types différents.
- UI : Robolectric + Compose **à la taille d'une TV** (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), jamais la taille Robolectric par défaut : séquence D-pad héro → dock → Up Next → apps, menu « Ouvrir avec… », états (squelette, erreur, masquée), testTags `zone-upnext`, `upnext-card-<key>`, `upnext-menu`.
- Validation finale sur la TV réelle en `assembleRelease` : colonnes réellement visibles (tâche 1) et rendu.
