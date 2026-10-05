# Change : up-next

## Why
P6 du roadmap : rangée Up Next dédiée (ancien change `p2c-upnext`). La spec launcher-shell prévoit une rangée Up Next au-dessus de la grille, mais rien n'existe encore. Décision produit validée : la rangée agrège les programmes watch next de **toutes les apps visibles dans le TV Provider** (pas seulement Jellyfin), avec un dédoublonnage à plusieurs niveaux, et chaque carte ouvre l'**intent publié par son programme** (`COLUMN_INTENT_URI`), sinon l'app est lancée. La recherche (P7 du README) fait l'objet d'un change séparé. Cette PR embarque aussi le change `jellyfin-tvprovider-only`, qui transforme jellyfin-integration en contrat de données TV Provider.

## What Changes
- **Source unique : le TV Provider Android** (`WatchNextPrograms` uniquement, les `PreviewPrograms` ne sont pas lus, lu sous `READ_TV_LISTINGS` et filtré sur `COLUMN_BROWSABLE`) : items en cours et à suivre publiés par toutes les apps, sans appels réseau ni credentials
- **Filtres des réglages** : une app désactivée dans « Apps sources » ne contribue plus à Up Next (comme au héro et au Top Shelf) ; une app cachée de la grille garde ses contenus dans Up Next
- **Dédoublonnage en 5 niveaux** (clé d'identité, de la plus fiable à la moins fiable) : doublons exacts dans une app, série dans une app, épisode entre apps, film entre apps (fusion sur le titre seul quand l'année n'est connue que d'un côté), puis en P8 les IDs IMDb/TVDB ajoutés par l'enrichissement BetaSeries
- **Gagnant d'un doublon** (décision produit) : `CONTINUE` bat le groupe « à suivre » (`NEXT`/`NEW`, et programmes sans `watch_next_type`), qui bat `WATCHLIST` ; ensuite l'engagement le plus récent ; à égalité, l'ordre de préférence des apps (constante, Jellyfin d'abord)
- **Tri** : trois groupes, `CONTINUE` puis « à suivre » (`NEXT`/`NEW`, et programmes sans `watch_next_type`) puis `WATCHLIST`, chacun par `last_engagement_time` décroissant et à égalité par `_ID` croissant ; tri stable ; limite 20
- **Position** (décision produit) : la rangée est la **première ligne de la zone grille** (au-dessus des apps, même fond, pas de palier distinct : on y arrive par bas depuis le dock, la page d'un seul tenant défile et le héro sort par le haut comme pour les apps, un bas de plus descend sur la première rangée d'apps) ; réglage « Position d'Up Next » dans une nouvelle catégorie **« Écran d'accueil »** des réglages : *avant la grille* (défaut) ou *après la grille* (dernière ligne de la zone grille)
- **Aucun dédoublonnage entre le héro et Up Next** (décision produit) : le héro peut montrer les mêmes contenus, comme sur tvOS
- **Menu « Ouvrir avec… »** (décision produit) : appui long sur une carte, liste des apps qui ont ce contenu, ouverture via leur intent, rien n'est persisté ; le menu s'ouvre même avec une seule entrée
- **Badge app** : petite icône de l'app source prise dans le `PackageManager` (aucun logo embarqué)
- **Cartes 16:9 uniformes** : image 16:9 plein cadre quand elle est exploitable, sinon poster portrait centré sur fond sombre ; seuil de qualité propre aux cartes (la règle ≥ 1080 px du héro ne s'applique pas), carte gardée avec placeholder en dessous ; barre de progression pour les items en cours ; états chargement / erreur / vide / permission refusée
- **Champs facultatifs** (décisions produit) : chaque colonne est facultative ; aucun titre → programme exclu ; date d'engagement absente → plus ancien de son groupe, puis `_ID` croissant ; `watch_next_type` absent ou inconnu (hors des 4 valeurs Android, traité comme absent) → groupe « à suivre » ; `COLUMN_TYPE` absent → type inféré (épisode si saison et épisode sont présents, film sinon), utilisé pour le dédoublonnage et la carte
- **Modèle canonique `UpNextItem`** avec champ `externalIds` facultatif (prêt pour l'enrichissement BetaSeries de P8) et liste des sources du contenu conservée après fusion (pour le menu « Ouvrir avec… »)

## Capabilities

### New Capabilities
- `up-next` : agrégation, dédoublonnage, tri, cartes, ouverture, menu « Ouvrir avec… », états et rafraîchissement de la rangée Up Next

### Modified Capabilities
- `launcher-shell` : « Navigation 3 paliers » (texte archivé de `home-settings-polish` repris : page d'un seul tenant, retour dans la grille à la position laissée, dock jamais visible en vue grille, Retour depuis la grille, continuité de l'animation ; la zone grille y comprend la rangée Up Next, première ou dernière ligne, sautée si masquée), « Rangée Up Next » (renvoi à up-next), « Sélection des apps sources » (le filtre couvre aussi Up Next)
- `ui-testing` : « Sélecteurs stables » (testTags `zone-upnext`, `upnext-card-<key>`, `upnext-menu`), « Navigation D-pad des trois zones » (rangée Up Next dans la zone grille, position réglable, états, menu)
- `settings` : ADDED « Position d'Up Next » (catégorie « Écran d'accueil ») ; MODIFIED « Page de réglages » (nouvelle catégorie « Écran d'accueil » dans la liste des catégories ; l'exception de focus initial d'« Applications cachées » est conservée), « Apps sources » (la rangée Up Next entre dans la contribution d'une app ; tri, comptage et pause en arrière-plan conservés) et « Cacher une application » (Up Next dans la portée du masquage ; date de masquage conservée). Les trois blocs partent du texte archivé de `home-settings-polish` (`p2b-settings` archivé, tâche 1.1 faite)
- `betaseries-integration` : « Agrégation Up Next » (BetaSeries enrichit les items de toutes les sources au lieu de s'y concaténer)
- `jellyfin-integration` : par le change embarqué `jellyfin-tvprovider-only` (contrat de données TV Provider)

## Impact
- persistance : le réglage « Position d'Up Next » est persisté dans DataStore ; le menu « Ouvrir avec… » ne persiste rien ; l'ordre de préférence des apps est une constante P6
- code existant réutilisé : lecture des `WatchNextPrograms` de toutes les apps et observation du provider déjà en place dans `TvProviderHeroSource`, filtre des apps sources déjà en place dans `HeroFeed` / `HomeViewModel` (détail dans `design.md`)
- **Dépendances** : `jellyfin-tvprovider-only` (embarqué dans cette PR), `ui-testing` (archivé dans cette PR avant l'écriture du delta MODIFIED), `p2b-settings` (archivé sur main, capability `settings`), `betaseries-integration` (delta MODIFIED aligné). `home-settings-polish` (archivé sur main, `2026-10-03-home-settings-polish`) : les blocs MODIFIED de ce change sur launcher-shell « Navigation 3 paliers » et settings « Page de réglages », « Apps sources », « Cacher une application » partent de son texte archivé et n'y ajoutent que la rangée Up Next et la catégorie « Écran d'accueil » ; les points où les deux changes divergent sont en « Questions ouvertes ». Implémentation reportée à une itération ultérieure, sur la page d'un seul tenant de `home-settings-polish` (voir `design.md`).
- **Données de l'appareil** : le launcher lit des lignes `WatchNextPrograms` d'apps tierces sous `READ_TV_LISTINGS` (constaté avec `v0.0.1-rc.1`, voir `design.md`) ; le remplissage des colonnes n'est pas mesuré, chaque champ est donc facultatif. Toute donnée issue de l'appareil n'est publiée que sous forme de structure (colonnes, packages, compteurs, types), jamais de titres

## Non-goals
- **Routage de lecture par score et deep links** (Stremio `autoPlay`, `source=30`) : retiré de P6, pourra revenir en P8 avec les IDs externes
- **Override par série persisté** : remplacé par le menu « Ouvrir avec… » (rien n'est persisté)
- **BetaSeries comme étape d'enrichissement** : P8 (IDs externes IMDb/TVDB attachés aux items de toutes les sources, niveau 5 de dédoublonnage) ; la rangée reste multi-sources
- **Correspondance approximative des titres** : égalité stricte après normalisation, jamais de fusion à tort ; les titres localisés différemment restent en double jusqu'à P8
- **Autres réglages de la catégorie « Écran d'accueil »** : la catégorie est créée pour accueillir les futurs réglages de l'accueil ; seul « Position d'Up Next » est spécifié ici
- Recherche (change séparé, P7), réglage d'ordre de préférence des apps (constante P6), multi-comptes

## Questions ouvertes
- **BetaSeries comme source à part entière en P8** : en plus de l'enrichissement, les épisodes « à voir » BetaSeries pourraient devenir une source de plus de la rangée. Non tranché ; ne figure dans aucun SHALL
- **Compteur « Apps sources »** : il ne compte aujourd'hui que les `PreviewPrograms` ; une app qui ne publie que du watch next affiche 0. Depuis `home-settings-polish`, ce compteur trie aussi la liste : une telle app est rangée parmi les apps à 0, alors qu'elle alimente Up Next. Inclure les `WatchNextPrograms` dans le compteur (et donc dans le tri) est un choix produit à trancher (voir `design.md`, « Compteur des apps sources »)
- **Focus à la redescente dans la zone grille** : la version précédente de ce change donnait toujours le focus à la première ligne de la zone grille (la rangée Up Next quand elle est en tête) en descendant du dock ; `home-settings-polish` (archivé) fait redescendre la page à la position laissée à la sortie, focus sur la dernière tuile visitée. Les deux règles divergent quand l'utilisateur a quitté la zone grille depuis une rangée d'apps. Le texte MODIFIED garde la règle archivée et n'applique l'arrivée sur la rangée Up Next qu'à la première entrée ; à trancher, hors SHALL
- **Carte disparue au retour d'une app** : ce change place le focus sur la première carte de la rangée ; le repli partagé du dock et de la grille (`domain/FocusFallback.kt`, « Focus d'une app disparue », réutilisé par `home-settings-polish` pour le volet « Applications cachées ») donne la voisine à la même position. Aligner ou non la rangée sur ce repli n'est pas tranché ; le texte MODIFIED garde « première carte »
