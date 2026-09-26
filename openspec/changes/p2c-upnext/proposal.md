# Change : p2c-upnext

## Why
P2c du roadmap : rangée Up Next dédiée. La spec launcher-shell prévoit une rangée Up Next au-dessus de la grille, mais rien n'existe encore. Décision produit validée : la rangée agrège les programmes watch next de **toutes les apps visibles dans le TV Provider** (pas seulement Jellyfin), avec un dédoublonnage à plusieurs niveaux, et chaque carte ouvre l'**intent publié par son programme** (`COLUMN_INTENT_URI`), sinon l'app est lancée. La recherche (autre moitié du P2c du README) fait l'objet d'un change séparé. Cette PR embarque aussi le change `jellyfin-tvprovider-only`, qui transforme jellyfin-integration en contrat de données TV Provider.

## What Changes
- **Source unique : le TV Provider Android** (`WatchNextPrograms` uniquement — les `PreviewPrograms` ne sont pas lus —, lu sous `READ_TV_LISTINGS` et filtré sur `COLUMN_BROWSABLE`) : items en cours et à suivre publiés par toutes les apps, sans appels réseau ni credentials
- **Dédoublonnage en 5 niveaux** (clé d'identité, de la plus fiable à la moins fiable) : doublons exacts dans une app, série dans une app, épisode entre apps, film entre apps (fusion sur le titre seul quand l'année n'est connue que d'un côté), puis en P4 les IDs IMDb/TVDB ajoutés par l'enrichissement BetaSeries
- **Gagnant d'un doublon** (décision produit) : un item en cours (`CONTINUE`) bat un item à suivre (`NEXT`/`NEW`) ; ensuite l'engagement le plus récent ; à égalité, l'ordre de préférence des apps (constante, Jellyfin d'abord)
- **Tri** : trois groupes — items `CONTINUE` d'abord (par `last_engagement_time` décroissant), puis `NEXT`/`NEW` (même tri, à égalité par `_ID` croissant), puis `WATCHLIST` ; tri stable ; limite 20
- **Position réglable** (décision produit) : une ligne style tvOS en tête de la zone grille, au-dessus des apps ; réglage « Position d'Up Next » : *avant la grille* (défaut) ou *après la grille* — spécifié dans ce change (delta ADDED sur la capability `settings`), `p2b-settings` en dépendance
- **Aucun dédoublonnage entre le héro et Up Next** (décision produit) : le héro peut montrer les mêmes contenus, comme sur tvOS
- **Menu « Ouvrir avec… »** (décision produit) : appui long sur une carte, liste des apps qui ont ce contenu, ouverture via leur intent, rien n'est persisté — remplace l'override par série
- **Badge app** : petite icône de l'app source prise dans le `PackageManager` (aucun logo embarqué)
- **Cartes 16:9 uniformes** : image 16:9 plein cadre quand elle est exploitable, sinon poster portrait centré sur fond sombre ; barre de progression pour les items en cours ; états chargement / erreur / vide / permission refusée
- **Modèle canonique `UpNextItem`** avec champ `externalIds` facultatif (prêt pour l'enrichissement BetaSeries de P4) et liste des sources du contenu conservée après fusion (pour le menu « Ouvrir avec… »)

## Impact
- specs affectées : nouvelle capability `up-next` (delta ci-dessous)
- launcher-shell : deltas MODIFIED « Navigation 3 paliers » (séquence héro → dock → Up Next → apps, et le chemin inverse à la montée) et « Rangée Up Next » (délégation à up-next)
- ui-testing : le change `ui-testing` est **archivé dans cette PR** (toutes ses tâches cochées), puis delta MODIFIED sur « Navigation D-pad des trois zones » (quatre zones avec Up Next) et couverture des états de la rangée
- settings : delta ADDED « Position d'Up Next » sur la capability `settings` introduite par `p2b-settings`
- betaseries-integration : delta MODIFIED « Agrégation Up Next » (BetaSeries enrichit les items de toutes les sources au lieu de s'y concaténer)
- jellyfin-integration : devient le contrat de données (TV Provider), l'UI et l'ouverture sont décrites par up-next — les deux capabilities se renvoient l'une à l'autre
- persistance : le réglage « Position d'Up Next » est persisté dans DataStore ; le menu « Ouvrir avec… » ne persiste rien ; l'ordre de préférence des apps est une constante en p2c
- **Dépendances** : `jellyfin-tvprovider-only` (fusionné dans cette PR avant p2c), `ui-testing` (archivé dans cette PR avant l'écriture du delta MODIFIED), `p2b-settings` (capability `settings`), `betaseries-integration` (delta MODIFIED aligné)
- **Vérification sur l'appareil** : la visibilité réelle des lignes `WatchNextPrograms` des autres apps sous `READ_TV_LISTINGS` doit être confirmée sur la TCL avant tout code (tâche 2) ; si seules nos propres lignes sont visibles, le change est suspendu et revu

## Non-goals
- **Routage de lecture par score et deep links** (Stremio `autoPlay`, `source=30`) : retiré de p2c, pourra revenir en P4 avec les IDs externes
- **Override par série persisté** : remplacé par le menu « Ouvrir avec… » (rien n'est persisté)
- **BetaSeries comme source** : en P4, BetaSeries sert d'abord à **enrichir** les items de toutes les sources avec des IDs externes IMDb/TVDB (niveau 5 de dédoublonnage) ; la rangée reste multi-sources
- **Correspondance approximative des titres** : égalité stricte après normalisation, jamais de fusion à tort ; les titres localisés différemment restent en double jusqu'à P4
- Recherche (change séparé), réglage d'ordre de préférence des apps (constante en p2c), multi-comptes
