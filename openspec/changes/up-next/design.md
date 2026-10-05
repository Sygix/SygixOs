# Design : up-next

## Context
Motivation et périmètre : voir `proposal.md`. Ce document fixe le comment. État du repo qui conditionne l'approche :
- `TvProviderHeroSource` (`app/src/main/java/fr/sygix/sygixos/data/TvProviderHeroSource.kt`) **lit déjà les `WatchNextPrograms` de toutes les apps** pour le héro (`queryAll()`, `mapWatchNextRow()`) : package, `COLUMN_BROWSABLE`, titre, poster et ratio, miniature, vidéo d'aperçu, intent, position, durée, `last_engagement_time`. Elle expose aussi `programCountsFlow()`, un patron d'observation du provider (`ContentObserver` armé avant la valeur initiale, antirebond, désinscription à l'annulation, registration protégée par `runCatching`). Depuis `home-settings-polish`, ce flux est partagé en `WhileSubscribed()` et collecté par `LauncherHome` avec `collectAsStateWithLifecycle()` : l'observation démarre avec l'accueil, s'arrête quand le launcher passe en arrière-plan et relance un comptage au retour au premier plan.
- Le filtre des apps sources existe : `apps.disabledSources` combiné dans `HomeViewModel.state` (`HomeViewModel.kt`) et `filterBySources()` (`domain/HeroFeed.kt`).
- Le menu contextuel `AppContextMenu.kt` est lié à `TvApp`.
- L'accueil est une page d'un seul tenant depuis `home-settings-polish` (archivé) : `LauncherHome` (`HomeScreen.kt`) compose un bloc héro (héro, dock et capsule heure et réglages, qui sortent ensemble par le haut) puis le bloc grille, sous un seul `ScrollState`. La cible de la page est la fonction pure `domain/HomePage.kt` (`target`, `transition`, `heroOnScreen`) ; `domain/GridScroll.kt` prend un paramètre `origin` (le haut du bloc grille dans la page) et ne descend jamais sous lui. Le repli de focus du dock et de la grille est partagé (`domain/FocusFallback.kt`, `ui/home/TileFocus.kt`). Dans les réglages, « Applications cachées » s'affiche dans le volet de droite (plus de sous-écran) et les libellés des catégories sont des ressources (`@StringRes`).
- La lecture des `PreviewPrograms` publiés par les autres apps est acquise : le change archivé `2026-09-22-spec-sync` (`proposal.md`) documente environ 500 programmes lus sur la TV de test. Le README ne contient pas ce chiffre.
- Constats sur l'appareil : voir « Constats et hypothèses retenues ».
- `p2b-settings` est archivé sur main (`423dbbd`) : la capability `settings` existe, ce change y pose ses MODIFIED.

## Goals / Non-Goals
**Goals :** une seule source TV Provider pour la rangée, logique pure (mapping, dédoublonnage, tri) testable en JUnit, réutilisation du code existant du héro et des réglages, aucun état dupliqué.

**Non-Goals :** voir `proposal.md` ; au niveau design, aucun second cache d'images, aucun score, aucune persistance hors du réglage de position.

## Décisions

### Source : TV Provider, toutes les apps
La rangée lit uniquement `WatchNextPrograms` (les `PreviewPrograms` ne sont pas lus : pas de `watch_next_type` exploitable et des `_ID` qui peuvent entrer en collision) via `ContentResolver` sous `READ_TV_LISTINGS`, avec le filtre `COLUMN_BROWSABLE` déjà appliqué par le héro, sans filtrer sur le package : **toutes les apps visibles** alimentent la rangée. La requête s'exécute sur `Dispatchers.IO` avec un `withTimeout` court (~2 s).

**Réutilisation de l'existant** (alternative écartée : une seconde lecture indépendante du provider, qui dupliquerait le mapping du héro) :
- les extensions `Cursor.optString` / `optLong` / `optInt`, `landscapeImage()` et `HeroOrdering.progressRatio()` sont aujourd'hui privées à `TvProviderHeroSource` ou internes : les extraire dans un helper partagé du package `data`, utilisé par le mappeur `Cursor` → `UpNextItem` ;
- le `ContentObserver` de la rangée suit le patron de `programCountsFlow()` (observateur armé avant la valeur initiale, antirebond, `runCatching` sur la registration, désinscription dans `finally`), sur `WatchNextPrograms.CONTENT_URI` ;
- le filtre « Apps sources » réutilise `disabledSources` et `filterBySources` : la liste d'items est filtrée sur le package **avant** le dédoublonnage, pour qu'une source désactivée ne serve jamais de gagnant ni de source du menu « Ouvrir avec… ». Les apps cachées (`hiddenApps`) ne filtrent pas la rangée.

**Constats et hypothèses retenues** :
- constats sur la TV de test avec `v0.0.1-rc.1` : sous `READ_TV_LISTINGS`, le launcher lit bien des lignes `WatchNextPrograms` publiées par des apps tierces. Au moins Twitch : une reprise avec position et durée, affichée par le héro avec barre de progression et « Reprendre ». Très probablement Jellyfin : image en `content://` servie par son ImageProvider. Un dump par `adb shell` n'est pas possible (0 ligne dans `watch_next_program` et `preview_program` : le TvProvider filtre par appelant, la sélection est refusée, pas de `run-as` en release ni de root) ; aucun test sur l'appareil n'est donc prévu avant le code ;
- le remplissage exact des colonnes par chaque app n'est pas mesuré : **chaque colonne est facultative** et la spec définit le comportement quand elle manque (exigence « Champs facultatifs » de up-next). Colonnes lues : titre, titre de série, `episode_title`, numéros de saison/épisode, `internal_provider_id`, `content_id`, `intent_uri`, `poster_art_uri` et ratio, `thumbnail_uri`, `watch_next_type`, `last_engagement_time`, position de lecture, durée, `release_date` (année), `COLUMN_TYPE`, `COLUMN_BROWSABLE` ; `package_name` et `_ID` sont toujours remplis par le provider.

D'après `jellyfin-androidtv` (`LeanbackChannelWorker.getBaseItemAsWatchNextProgram`), les programmes Jellyfin portent `internal_provider_id`, le type, le titre de série, `episode_title`, les numéros de saison et d'épisode, `watch_next_type` et un intent qui ouvre la **fiche** de l'item (`StartupActivity` + `ItemId`), pas la lecture. Le verbe de la rangée est donc « ouvrir », pas « lire » : l'app affiche sa fiche ou reprend selon son propre comportement. `release_date` est absent des programmes Jellyfin : ni le tri ni le dédoublonnage ne s'y fient (voir niveau 4).

### Modèle canonique
`UpNextItem` : `id` (`_ID` du programme), package source, type (épisode / film : `COLUMN_TYPE` publié, sinon inféré, voir « Types absents »), titre de série, saison, épisode, titre d'affichage, poster, progression optionnelle (0–1, position / durée quand les deux colonnes existent ; la progression ne sert qu'à la barre de la carte), `watchNextType` (`CONTINUE`, `NEXT`, `NEW`, `WATCHLIST` ; absent : rattaché au groupe « à suivre », voir « Types absents »), timestamp d'activité (`last_engagement_time`), `intentUri` (`COLUMN_INTENT_URI`), nom de l'app source, la **liste des sources du contenu** (package, nom, icône, intent publié de chacune, conservée après fusion, c'est elle qu'affiche le menu « Ouvrir avec… »), et un champ **`externalIds` facultatif** (IMDb / TVDB), vide en P6, rempli en P8 par l'enrichissement BetaSeries.

`UpNextSource` est une interface aux frontières (SOLID) : retourne un `Result<List<UpNextItem>>` pour que l'UI distingue **erreur** et **vide** (jamais d'exception avalée, contrairement à `TvProviderHeroSource.load()` qui avale via `runCatching {...}.getOrDefault(emptyList())`).

### Dédoublonnage en 5 niveaux
Clé d'identité calculée à partir des colonnes du TV Provider, de la plus fiable à la moins fiable :

| Niveau | Clé | Rôle |
|---|---|---|
| 1. Dans une app | `package_name` + `internal_provider_id`, sinon `content_id`, sinon `intent_uri` | Retirer les doublons exacts |
| 2. Série dans une app | `package_name` + titre de série normalisé | Au plus une carte par série : l'épisode en cours prime sur l'épisode suivant |
| 3. Épisode entre apps | titre de série normalisé + saison + épisode | Même épisode sur deux apps (type publié ou inféré) |
| 4. Film entre apps | titre normalisé + année | Même titre et même année → fusion ; pas de fusion si les deux années sont connues et différentes (remakes) ; année inconnue d'un côté → fusion sur le titre seul (limite assumée) ; types différents (film contre épisode, type publié ou inféré) → jamais de fusion |
| 5. P8 | ID IMDb/TVDB ajouté aux items par l'enrichissement BetaSeries | Prend le pas sur les niveaux 3 et 4 quand il est connu |

**Normalisation** : minuscules, diacritiques retirés (NFKD), ponctuation et `(année)` supprimés, espaces compactés, puis **égalité stricte**. Aucune correspondance approximative : il ne faut jamais fusionner à tort.

**Limite assumée, documentée dans la spec** : les titres localisés différemment selon l'app (« La Casa de Papel » contre « Money Heist ») restent en double jusqu'à P8.

**Types exclus** : les programmes dont le `COLUMN_TYPE` n'est ni un épisode ni un film (clip, extrait, autre) ne sont pas convertis en items.

**Types absents** (décisions Sygix, exigence « Champs facultatifs » de up-next) :
- `COLUMN_TYPE` absent : le type est inféré, épisode si le numéro de saison et le numéro d'épisode sont présents, film sinon ; le type inféré sert aux niveaux 2 à 4 et à la carte comme un type publié ;
- `watch_next_type` absent ou inconnu (valeur hors de `CONTINUE`, `NEXT`, `NEW`, `WATCHLIST`, traitée comme absente) : le programme est rattaché au groupe « à suivre » (`NEXT`/`NEW`), après les `CONTINUE` et avant les `WATCHLIST`, pour le tri comme pour le gagnant d'un doublon.

**Gagnant d'un doublon** (décision produit) :
1. `CONTINUE` bat le groupe « à suivre » (`NEXT`/`NEW`, programmes sans `watch_next_type` inclus), qui bat `WATCHLIST` ;
2. ensuite, l'engagement le plus récent (`last_engagement_time` décroissant) ;
3. à égalité, l'ordre de préférence des apps (constante en P6 : Jellyfin d'abord) tranche.

### Tri
`release_date` n'est pas fiable (absent côté Jellyfin) et trier globalement par `last_engagement_time` mettrait tous les `NEXT` avant les `CONTINUE` (Jellyfin publie pour `NEXT` un `last_engagement_time` égal à l'heure de synchro, qui tourne toutes les heures). Tri final :
1. items `CONTINUE` d'abord ;
2. puis le groupe « à suivre » : `NEXT`/`NEW` et programmes sans `watch_next_type` ;
3. puis `WATCHLIST` (décision Sygix : incluse en dernier groupe) ;
4. dans chaque groupe : `last_engagement_time` décroissant, à égalité `_ID` croissant ;
5. tri **stable** (l'ordre d'entrée des niveaux de dédoublonnage est préservé à égalité).

Limite : 20 items.

### Position (décision produit)
La rangée Up Next est la **première ligne de la zone grille** : on y arrive par bas depuis le dock, et la page d'un seul tenant défile alors jusqu'à la zone grille, le héro, le dock et la capsule heure et réglages sortant par le haut, exactement comme pour les apps ; un bas de plus descend sur la première rangée d'apps dans la même zone, sans changement de fond. C'est un arrêt D-pad entre le dock et les apps, mais **pas un palier distinct** au sens de launcher-shell (une zone avec son propre fond) : « Navigation 3 paliers » reste héro → dock → grille. La rangée est sautée si elle est masquée. Le réglage « Position d'Up Next » (catégorie « Écran d'accueil », delta `settings`) permet *avant la grille* (défaut) ou *après la grille* : la rangée est alors la **dernière ligne de la zone grille**. Toute la navigation de la zone grille, rangée incluse, est dans le delta launcher-shell ; up-next et settings y renvoient.

Intégration à la page d'un seul tenant : la rangée fait partie du bloc grille, donc de la zone que `GridScroll` place entre les marges et dont `origin` marque le début ; à la première entrée, la page s'arrête sur `origin`, première ligne de la zone grille en haut de l'écran. `GridScroll` suppose aujourd'hui des rangées de même hauteur (`rowHeight`) : la hauteur propre de la rangée Up Next reste à y intégrer à l'implémentation. La redescente à la position laissée et le focus qui l'accompagne suivent le texte archivé de `home-settings-polish` (voir « Questions ouvertes » du `proposal.md`).

**Pas de dédoublonnage entre le héro et Up Next** (décision produit) : le héro peut montrer les mêmes contenus, comme sur tvOS. Écrit explicitement dans la spec pour que personne ne « l'optimise » plus tard.

Cohabitation avec la Top Shelf : le panneau Top Shelf appartient au focus des tuiles de la grille ; tant que le focus est sur la rangée Up Next, aucun panneau n'est ouvert.

### Réglage « Position d'Up Next » et catégorie « Écran d'accueil »
Nouvelle entrée `HOME` dans `SettingsCategory` (`ui/settings/SettingsScreen.kt`), libellé « Écran d'accueil » en ressource (`@StringRes`, comme les autres catégories), placée avant « À propos », qui accueillera les futurs réglages de l'accueil. Le contrôle est un sélecteur à deux valeurs : OK bascule, gauche/droite se déplacent sans boucle, gauche depuis la première valeur et Retour rendent le focus au volet des catégories (règle « changement de catégorie » de « Page de réglages »). Persistance : nouvelle clé DataStore dans `LauncherPrefs`, exposée en `Flow` et combinée dans `HomeViewModel.state` comme `disabledSources`.

### Carte
- format **16:9 uniforme** : image du programme en 16:9 plein cadre quand elle est exploitable, sinon poster portrait centré sur fond sombre ;
- texte : titre de série, `SxxEyy`, titre d'épisode (films : titre seul) ;
- barre de progression pour les `CONTINUE` seulement (progression 0–1) ;
- focus tvOS : suit l'exigence « Focus tvOS » de launcher-shell (renvoi, aucune valeur recopiée ici) ;
- placeholder si le poster échoue, manque ou est trop petit ;
- **seuil de qualité propre aux cartes** (décision Sygix) : la règle « ≥ 1080 px » de « Qualité des visuels » ne s'applique pas ; une image est exploitable si sa largeur décodée est ≥ 2 × la largeur affichée de la carte ; en dessous, la carte reste avec le placeholder, l'item n'est jamais écarté ;
- badge : petite icône de l'app source prise dans le `PackageManager` (aucun logo de marque embarqué) ;
- budget mémoire : posters Up Next intégrés au mécanisme existant « Préchargement et mémoire » de launcher-shell (même cache Coil), pas de second cache.

### Ouverture et menu « Ouvrir avec… » (décision produit)
Appui OK sur une carte : ouverture de `COLUMN_INTENT_URI` du programme ; si absent ou si l'ouverture échoue (`ActivityNotFoundException`, `SecurityException`), l'app source est lancée (`LeanbackLauncher` du package). Aucun routage par score en P6.

Appui long : menu « Ouvrir avec… » listant les sources du contenu conservées par l'item, chacune ouverte via **son** intent publié ; OK valide, Retour ferme, le focus revient sur la carte ; focus initial sur la première entrée, bords sans boucle ; intent en échec → repli lancement de l'app + toast. Rien n'est persisté. **Le menu s'ouvre même avec une seule entrée** (décision Sygix). Le menu contextuel existant (`AppContextMenu.kt`, lié à `TvApp`) est généralisé pour accepter les entrées Up Next.

### États de la rangée
- **Permission refusée** : rangée **masquée**, sans carte d'erreur (scénario dédié) ;
- **Erreur** (requête en échec, timeout au premier chargement) : carte d'état focusable (message + « Réessayer »), à la place de la rangée dans la navigation quelle que soit la position réglée ; l'erreur se distingue du vide grâce au `Result` de la source ; en erreur au rechargement, le contenu précédent reste affiché ;
- **Chargement** : squelette au **premier** chargement uniquement, et seulement si la requête dépasse ~300 ms ; aux rechargements, l'état précédent reste affiché jusqu'au résultat (jamais de saut de mise en page) ;
- **Vide** : rangée masquée ; le focus au retour d'une app suit « rangée devenue vide » de launcher-shell ;
- **Rafraîchissement** : retour au premier plan ou permission `READ_TV_LISTINGS` venant d'être accordée → la rangée se recharge, le contenu précédent reste affiché pendant le rechargement. `MainActivity` rappelle aujourd'hui `HomeViewModel.refresh()` (apps et héro) dans `onResume` et `refreshHero()` à l'octroi de la permission : la rangée s'ajoute à ces deux rappels. Le `ContentObserver` sur `WatchNextPrograms.CONTENT_URI` déclenche aussi le rechargement quand le provider change. Hypothèse documentée (comportement des apps, non testable dans le launcher) : Jellyfin ne resynchronise le TV Provider qu'une fois par heure ; le launcher reflète simplement ce que le provider expose, sans traiter ce délai comme une erreur.
- Non-blocage : la navigation du home reste fonctionnelle dans tous les états.

### Compteur des apps sources
`TvProviderHeroSource.programCounts()` ne compte que les `PreviewPrograms` : une app qui ne publie que des `WatchNextPrograms` affiche « 0 programme » dans « Apps sources » alors qu'elle alimente Up Next. Depuis `home-settings-polish`, ce compteur trie aussi la liste « Apps sources » (apps à 0 en fin de liste). Ce change n'y touche pas ; l'élargissement du compteur est en Questions ouvertes du `proposal.md`.

## MODIFIED settings
Les MODIFIED « Page de réglages » (catégorie « Écran d'accueil »), « Apps sources » et « Cacher une application » sont posés dans `specs/settings/spec.md` de ce change (tâche 1.1, faite après l'archivage de `p2b-settings`). Ils partent du texte archivé de `home-settings-polish` (exception de focus d'« Applications cachées », tri et comptage d'« Apps sources », date de masquage) et n'y ajoutent que la catégorie « Écran d'accueil » et la rangée Up Next.

## Risks / Trade-offs
- [Une app remplit moins de colonnes que prévu] → chaque champ est facultatif avec un comportement défini (« Champs facultatifs ») ; le seul rejet est l'absence de tout titre ou un `COLUMN_TYPE` hors épisode/film.
- [Colonnes non mesurées sur l'appareil] → lecture en `opt*`, dégradation champ par champ (pas d'année, pas de progression, pas de `SxxEyy`) sans exclure l'item ; type watch next absent → groupe « à suivre », `COLUMN_TYPE` absent → type inféré (épisode si saison et épisode, sinon film).
- [Faux positifs de fusion] → égalité stricte après normalisation, tests JUnit sur les remakes et les types différents ; les doublons de titres localisés sont acceptés jusqu'à P8.
- [Repo public] → toute donnée issue de l'appareil n'est publiée que sous forme de structure (colonnes, packages, compteurs, types), jamais de titres.

## Notes de test
- Dédoublonnage (chaque niveau), normalisation, gagnant, tri : tests **JUnit** purs sur le mapping `Cursor` → `UpNextItem` → fusion, y compris les faux positifs : remake avec une année différente, même titre mais types différents, même titre et même année (fusion) ; types absents : inférence épisode/film, programme sans `watch_next_type` ou avec une valeur inconnue dans le groupe « à suivre » (tri et gagnant).
- Filtre des apps sources : test JUnit (une source désactivée n'est ni gagnante ni dans les sources du menu) et test Compose (bascule du switch → la carte disparaît sans redémarrage).
- UI : Robolectric + Compose **à la taille d'une TV** (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), jamais la taille Robolectric par défaut : séquence D-pad héro → dock → zone grille (rangée Up Next puis apps, et la montée), position « après la grille », rangée sautée, menu « Ouvrir avec… » (y compris à une entrée), états (squelette, erreur, masquée), réglage de position, testTags `zone-upnext`, `upnext-card-<key>`, `upnext-menu`.
- Validation finale sur la TV réelle en `assembleRelease` : rendu de la rangée avec les apps installées, navigation, réglage de position.
