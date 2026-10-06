# Change : up-next

## Why
P6 du roadmap : rangée Up Next dédiée (ancien change `p2c-upnext`). La spec launcher-shell prévoit une rangée Up Next au-dessus de la grille, mais rien n'existe encore. Décision produit validée : la rangée agrège les programmes watch next de **toutes les apps visibles dans le TV Provider** (pas seulement Jellyfin), avec un dédoublonnage à plusieurs niveaux, et chaque carte ouvre l'**intent publié par son programme** (`COLUMN_INTENT_URI`), sinon l'app est lancée. La recherche (P7 du README) fait l'objet d'un change séparé. Cette PR embarque aussi le change `jellyfin-tvprovider-only`, qui transforme jellyfin-integration en contrat de données TV Provider.

## What Changes
- **Source unique : le TV Provider Android** (`WatchNextPrograms` uniquement, les `PreviewPrograms` ne sont pas lus, lu sous `READ_TV_LISTINGS` et filtré sur `COLUMN_BROWSABLE`) : items en cours et à suivre publiés par toutes les apps, sans appels réseau ni credentials
- **Filtres des réglages** : une app désactivée dans « Apps sources » ne contribue plus à Up Next (comme au héro et au Top Shelf) ; une app cachée de la grille garde ses contenus dans Up Next
- **Dédoublonnage en 5 niveaux** (clé d'identité, de la plus fiable à la moins fiable) : doublons exacts dans une app, série dans une app, épisode entre apps, film entre apps (fusion sur le titre seul quand l'année n'est connue que d'un côté), puis identifiant externe apporté par un change distinct, prenant le pas sur les niveaux 3 et 4
- **Gagnant d'un doublon** (décision produit) : `CONTINUE` bat le groupe « à suivre » (`NEXT`/`NEW`, et programmes sans `watch_next_type`), qui bat `WATCHLIST` ; ensuite l'engagement le plus récent ; à égalité, l'ordre de préférence des apps (constante, Jellyfin d'abord)
- **Tri** : trois groupes, `CONTINUE` puis « à suivre » (`NEXT`/`NEW`, et programmes sans `watch_next_type`) puis `WATCHLIST`, chacun par `last_engagement_time` décroissant et à égalité par `_ID` croissant ; tri stable ; limite 20
- **Position** (décision produit) : la rangée est la **première ligne de la zone grille** (au-dessus des apps, même fond, pas de palier distinct : on y arrive par bas depuis le dock, la page d'un seul tenant défile et le héro sort par le haut comme pour les apps, un bas de plus descend sur la première rangée d'apps) ; réglage « Position d'Up Next » dans une nouvelle catégorie **« Écran d'accueil »** des réglages : *avant la grille* (défaut) ou *après la grille* (dernière ligne de la zone grille)
- **Aucun dédoublonnage entre le héro et Up Next** (décision produit) : le héro peut montrer les mêmes contenus, comme sur tvOS
- **Menu « Ouvrir avec… »** (décision produit) : appui long sur une carte, liste des apps qui ont ce contenu, ouverture via leur intent, rien n'est persisté ; le menu s'ouvre même avec une seule entrée
- **Badge app** : petite icône de l'app source prise dans le `PackageManager` (aucun logo embarqué)
- **Présentation visuelle de référence** : `assets/p2-grid.png` montre la rangée « À suivre » distincte au-dessus du libellé et des tuiles « Applications », sans chevauchement ; cartes média 16:9 uniformes, image plein cadre, titre lisible sur dégradé sombre, badge discret de l'app source et focus par agrandissement/ombre sans contour blanc ; poster portrait centré sur fond sombre ou placeholder explicite si aucune image exploitable. `assets/p3-settings.png` montre les réglages distincts « Afficher Up Next » et « Position d'Up Next » dans « Écran d'accueil », « Avant la grille » par défaut. Les deux références font 1920×1080. Aucun changement de présentation ou comportement du hero n'est inclus dans P6.
- **Cartes et états** : seuil de qualité propre aux cartes (la règle ≥ 1080 px du héro ne s'applique pas), carte gardée avec placeholder en dessous ; barre de progression pour les items en cours ; états chargement / erreur / vide / permission refusée
- **Compteur « Apps sources »** : compte tous les programmes publiés par chaque app dans le TV Provider, qu'ils soient `PreviewPrograms` ou `WatchNextPrograms` ; ces derniers s'ajoutent au comptage existant et ne le remplacent pas
- **Réglage de visibilité** : « Afficher Up Next » est proposé dans « Écran d'accueil », à côté du réglage de position ; désactivée, la rangée est masquée et la navigation la saute
- **Retour d'une app** : si la carte Up Next d'origine existe encore, elle reprend le focus ; si elle a disparu, le focus revient au début de la section Up Next
- **Champs facultatifs** (décisions produit) : chaque colonne est facultative ; aucun titre → programme exclu ; date d'engagement absente → plus ancien de son groupe, puis `_ID` croissant ; `watch_next_type` absent ou inconnu (hors des 4 valeurs Android, traité comme absent) → groupe « à suivre » ; `COLUMN_TYPE` absent → type inféré (épisode si saison et épisode sont présents, film sinon), utilisé pour le dédoublonnage et la carte
- **Modèle canonique `UpNextItem`** avec champ `externalIds` facultatif pour une extension ultérieure, et liste des sources du contenu conservée après fusion (pour le menu « Ouvrir avec… »)

## Capabilities

### New Capabilities
- `up-next` : agrégation, dédoublonnage, tri, cartes, ouverture, menu « Ouvrir avec… », états et rafraîchissement de la rangée Up Next

### Modified Capabilities
- `launcher-shell` : « Navigation 3 paliers » (texte archivé de `home-settings-polish` repris : page d'un seul tenant, retour dans la grille à la position laissée, dock jamais visible en vue grille, Retour depuis la grille, continuité de l'animation ; la zone grille y comprend la rangée Up Next, première ou dernière ligne, sautée si masquée ; retour sur la position précédente de la grille, avec repli Up Next au début de sa section si la carte d'origine a disparu), « Rangée Up Next » (renvoi à up-next), « Sélection des apps sources » (le filtre couvre aussi Up Next)
- `ui-testing` : « Sélecteurs stables » (testTags `zone-upnext`, `upnext-card-<key>`, `upnext-menu`), « Navigation D-pad des trois zones » (rangée Up Next dans la zone grille, position réglable, états, menu)
- `settings` : ADDED « Position d'Up Next » et « Afficher Up Next » (catégorie « Écran d'accueil ») ; MODIFIED « Page de réglages » (nouvelle catégorie « Écran d'accueil » dans la liste des catégories ; l'exception de focus initial d'« Applications cachées » est conservée), « Apps sources » (Up Next entre dans la contribution d'une app ; le compteur comprend tout programme publié, y compris WatchNext, et la règle de tri/comptage/pause en arrière-plan reste cohérente) et « Cacher une application » (Up Next dans la portée du masquage ; date conservée)
- `jellyfin-integration` : par le change embarqué `jellyfin-tvprovider-only` (contrat de données TV Provider)

## Impact
- persistance : les réglages « Position d'Up Next » et « Afficher Up Next » sont persistés dans DataStore ; le menu « Ouvrir avec… » ne persiste rien ; l'ordre de préférence des apps est une constante P6
- code existant réutilisé : lecture des `WatchNextPrograms` de toutes les apps et observation du provider déjà en place dans `TvProviderHeroSource`, filtre des apps sources déjà en place dans `HeroFeed` / `HomeViewModel` (détail dans `design.md`)
- **Dépendances** : `jellyfin-tvprovider-only` (embarqué dans ce change), `ui-testing` (à archiver avant l'écriture du delta MODIFIED), `p2b-settings` (archivé sur main, capability `settings`). `home-settings-polish` (archivé sur main, `2026-10-03-home-settings-polish`) : les blocs MODIFIED de ce change sur launcher-shell « Navigation 3 paliers » et settings « Page de réglages », « Apps sources », « Cacher une application » partent de son texte archivé et n'y ajoutent que la rangée Up Next et la catégorie « Écran d'accueil ». L'implémentation est planifiée dans une itération ultérieure sur la page d'un seul tenant de `home-settings-polish` (voir `design.md`).
- **Données de l'appareil** : le launcher lit des lignes `WatchNextPrograms` d'apps tierces sous `READ_TV_LISTINGS` (constaté avec `v0.0.1-rc.1`, voir `design.md`) ; le remplissage des colonnes n'est pas mesuré, chaque champ est donc facultatif. Toute donnée issue de l'appareil n'est publiée que sous forme de structure (colonnes, packages, compteurs, types), jamais de titres

## Non-goals
- **Routage de lecture par score et deep links** (Stremio `autoPlay`, `source=30`) : retiré de ce change
- **Override par série persisté** : remplacé par le menu « Ouvrir avec… » (rien n'est persisté)
- **BetaSeries** : absente de P6 ; son éventuel rôle comme source de programmes Up Next sera traité dans son propre change futur, sans préjuger de son placement dans la roadmap
- **Correspondance approximative des titres** : égalité stricte après normalisation, jamais de fusion à tort ; les titres localisés différemment restent en double jusqu'à l'activation du niveau 5 par un change distinct
- **Autres réglages de la catégorie « Écran d'accueil »** : la catégorie est créée pour accueillir les futurs réglages de l'accueil ; seuls « Position d'Up Next » et « Afficher Up Next » sont spécifiés ici
- Recherche (change séparé, P7), réglage d'ordre de préférence des apps (constante P6), multi-comptes

## Questions ouvertes
- **Retour dans la grille** : la position précédente est restaurée comme spécifié dans `home-settings-polish`; le comportement reste indépendant de l'état Up Next.
