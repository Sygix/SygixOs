# Design : p2c-upnext

## Décisions

### Source : TV Provider, toutes les apps
La rangée lit uniquement `WatchNextPrograms` (les `PreviewPrograms` ne sont pas lus : pas de `watch_next_type` exploitable et des `_ID` qui peuvent entrer en collision) via `ContentResolver` sous `READ_TV_LISTINGS`, avec le filtre `COLUMN_BROWSABLE` déjà appliqué par le héro (`TvProviderHeroSource`), sans filtrer sur le package : **toutes les apps visibles** alimentent la rangée. La requête s'exécute sur `Dispatchers.IO` avec un `withTimeout` court (~2 s).

**État des connaissances et hypothèses à vérifier sur l'appareil (tâche 2, avant tout code)** :
- la lecture des programmes publiés par les autres apps est déjà acquise : le README documente ~500 programmes lus sur la TCL (previews du TV Provider). La vraie inconnue se limite aux lignes `WatchNextPrograms` des autres apps — le TvProvider peut restreindre la lecture aux lignes de l'appelant — et aux colonnes qu'elles remplissent ;
- colonnes supposées disponibles et à confirmer : titre de série, `episode_title`, numéros de saison/épisode, `internal_provider_id`, `content_id`, `intent_uri`, `poster_art_uri`, `watch_next_type`, `last_engagement_time`, position de lecture, **année (`release_date`)**, `COLUMN_TYPE`, durée, `COLUMN_BROWSABLE`.

D'après `jellyfin-androidtv` (`LeanbackChannelWorker.getBaseItemAsWatchNextProgram`), les programmes Jellyfin portent `internal_provider_id`, le type, le titre de série, `episode_title`, les numéros de saison et d'épisode, `watch_next_type` et un intent qui ouvre la **fiche** de l'item (`StartupActivity` + `ItemId`) — pas la lecture. Le verbe de la rangée est donc « ouvrir », pas « lire » : l'app affiche sa fiche ou reprend selon son propre comportement. `release_date` est absent des programmes Jellyfin : ni le tri ni le dédoublonnage ne s'y fient (voir niveau 4).

### Modèle canonique
`UpNextItem` : `id` (`_ID` du programme), package source, type (épisode / film), titre de série, saison, épisode, titre d'affichage, poster, progression optionnelle (0–1, position / durée quand les deux colonnes existent — la progression ne sert qu'à la barre de la carte), `watchNextType` (`CONTINUE`, `NEXT`, `NEW`, `WATCHLIST`), timestamp d'activité (`last_engagement_time`), `intentUri` (`COLUMN_INTENT_URI`), nom de l'app source, la **liste des sources du contenu** (package, nom, icône, intent publié de chacune — conservée après fusion, c'est elle qu'affiche le menu « Ouvrir avec… »), et un champ **`externalIds` facultatif** (IMDb / TVDB) — vide en p2c, rempli en P4 par l'enrichissement BetaSeries.

`UpNextSource` est une interface aux frontières (SOLID) : retourne un `Result<List<UpNextItem>>` pour que l'UI distingue **erreur** et **vide** (jamais d'exception avalée, contrairement à `TvProviderHeroSource.load()` qui avale via `runCatching {...}.getOrDefault(emptyList())`).

### Dédoublonnage en 5 niveaux
Clé d'identité calculée à partir des colonnes du TV Provider, de la plus fiable à la moins fiable :

| Niveau | Clé | Rôle |
|---|---|---|
| 1. Dans une app | `package_name` + `internal_provider_id`, sinon `content_id`, sinon `intent_uri` | Retirer les doublons exacts |
| 2. Série dans une app | `package_name` + titre de série normalisé | Au plus une carte par série : l'épisode en cours prime sur l'épisode suivant |
| 3. Épisode entre apps | titre de série normalisé + saison + épisode | Même épisode sur deux apps |
| 4. Film entre apps | titre normalisé + année | Pas de fusion si les deux années sont connues et différentes (remakes) ; année inconnue d'un côté → fusion sur le titre seul (limite assumée) |
| 5. P4 | ID IMDb/TVDB ajouté aux items par l'enrichissement BetaSeries | Prend le pas sur les niveaux 3 et 4 quand il est connu |

**Normalisation** : minuscules, diacritiques retirés (NFKD), ponctuation et `(année)` supprimés, espaces compactés, puis **égalité stricte**. Aucune correspondance approximative : il ne faut jamais fusionner à tort.

**Limite assumée, documentée dans la spec** : les titres localisés différemment selon l'app (« La Casa de Papel » contre « Money Heist ») restent en double jusqu'à P4.

**Types exclus** : les programmes dont le `COLUMN_TYPE` n'est ni un épisode ni un film (clip, extrait, autre) ne sont pas convertis en items.

**Gagnant d'un doublon** (décision produit) :
1. un item en cours (`CONTINUE`) bat un item à suivre (`NEXT`/`NEW`) ;
2. ensuite, l'engagement le plus récent (`last_engagement_time` décroissant) ;
3. à égalité, l'ordre de préférence des apps (constante en p2c : Jellyfin d'abord) tranche.

### Tri
`release_date` n'est pas fiable (absent côté Jellyfin) et trier globalement par `last_engagement_time` mettrait tous les `NEXT` avant les `CONTINUE` (Jellyfin publie pour `NEXT` un `last_engagement_time` égal à l'heure de synchro, qui tourne toutes les heures). Tri final :
1. items `CONTINUE` d'abord, par `last_engagement_time` décroissant ;
2. puis `NEXT`/`NEW` par `last_engagement_time` décroissant, et à égalité par `_ID` croissant ;
3. puis items `WATCHLIST` (même tri que `NEXT`/`NEW`) — décision Sygix : la `WATCHLIST` est **incluse** en dernier groupe ;
4. tri **stable** (l'ordre d'entrée des niveaux de dédoublonnage est préservé à égalité).

Limite : 20 items.

### Position (décision produit)
Une ligne style tvOS en tête de la zone grille, au-dessus des apps — c'est le **palier Up Next** de la navigation. Séquence D-pad : héro → dock → Up Next → apps à la descente, et apps → Up Next → dock → héro à la montée (détaillée dans le delta launcher-shell). Le réglage « Position d'Up Next » (delta ADDED sur `settings` dans ce change, `p2b-settings` en dépendance) permet *avant la grille* (défaut) ou *après la grille* ; le palier est sauté si la rangée est masquée.

**Pas de dédoublonnage entre le héro et Up Next** (décision produit) : le héro peut montrer les mêmes contenus, comme sur tvOS. Écrit explicitement dans la spec pour que personne ne « l'optimise » plus tard.

Cohabitation avec la Top Shelf : le panneau Top Shelf appartient au focus des tuiles de la grille ; tant que le focus est sur la rangée Up Next, aucun panneau n'est ouvert.

### Carte
- format **16:9 uniforme** : image du programme en 16:9 plein cadre quand elle est exploitable, sinon poster portrait centré sur fond sombre ;
- texte : titre de série, `SxxEyy`, titre d'épisode (films : titre seul) ;
- barre de progression pour les `CONTINUE` seulement (progression 0–1) ;
- focus tvOS : suit l'exigence « Focus tvOS » de launcher-shell (renvoi, aucune valeur recopiée ici) ;
- placeholder si le poster échoue ou manque ;
- règle « ≥ 1080 px » de « Qualité des visuels » **non applicable** aux posters portrait Up Next (artwork fourni par les apps, tailles hétérogènes) — le poster est affiché tel quel, centré ;
- badge : petite icône de l'app source prise dans le `PackageManager` (aucun logo de marque embarqué) ;
- budget mémoire : posters Up Next intégrés au mécanisme existant « Préchargement et mémoire » de launcher-shell (même cache Coil), pas de second cache.

### Ouverture et menu « Ouvrir avec… » (décision produit)
Appui OK sur une carte : ouverture de `COLUMN_INTENT_URI` du programme ; si absent ou si l'ouverture échoue (`ActivityNotFoundException`, `SecurityException`), l'app source est lancée (`LeanbackLauncher` du package). Aucun routage par score en p2c.

Appui long : menu « Ouvrir avec… » listant les sources du contenu conservées par l'item, chacune ouverte via **son** intent publié ; OK valide, Retour ferme, le focus revient sur la carte ; focus initial sur la première entrée, bords sans boucle ; intent en échec → repli lancement de l'app + toast. Rien n'est persisté. Si une seule app possède le contenu, le menu réduit (« Ouvrir avec » à une entrée) est affiché ; le menu contextuel existant (`AppContextMenu.kt`, lié à `TvApp`) est généralisé pour accepter les entrées Up Next.

### États de la rangée
- **Permission refusée** : rangée **masquée**, sans carte d'erreur (scénario dédié) ;
- **Erreur** (requête en échec, timeout au premier chargement) : carte d'état focusable (message + « Réessayer ») — l'erreur se distingue du vide grâce au `Result` de la source ; en erreur au rechargement, le contenu précédent reste affiché ;
- **Chargement** : squelette au **premier** chargement uniquement, et seulement si la requête dépasse ~300 ms ; aux rechargements, l'état précédent reste affiché jusqu'au résultat (jamais de saut de mise en page) ;
- **Vide** : rangée masquée ; au retour d'une app, le focus va à la première rangée d'apps ;
- **Rafraîchissement** : retour au premier plan ou permission `READ_TV_LISTINGS` venant d'être accordée → la rangée se recharge, le contenu précédent reste affiché pendant le rechargement. Un `ContentObserver` sur `WatchNextPrograms.CONTENT_URI` (décision : implémenté, protégé contre `SecurityException` et provider absent) déclenche aussi le rechargement quand le provider change. Hypothèse documentée (comportement des apps, non testable dans le launcher) : Jellyfin ne resynchronise le TV Provider qu'une fois par heure — le launcher reflète simplement ce que le provider expose, sans traiter ce délai comme une erreur.
- Non-blocage : la navigation du home reste fonctionnelle dans tous les états.

## Notes de test
- Dédoublonnage (chaque niveau), normalisation, gagnant, tri : tests **JUnit** purs sur le mapping `Cursor` → `UpNextItem` → fusion, y compris les faux positifs : remake avec une année différente, même titre mais types différents.
- UI : Robolectric + Compose **à la taille d'une TV** (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), jamais la taille Robolectric par défaut : séquence D-pad héro → dock → Up Next → apps (et la montée), menu « Ouvrir avec… », états (squelette, erreur, masquée), testTags `zone-upnext`, `upnext-card-<key>`, `upnext-menu`.
- Validation finale sur la TV réelle en `assembleRelease` : colonnes réellement visibles (tâche 2) et rendu.
