# Change : p2c-upnext

## Why
P2c du roadmap : rangée Up Next dédiée. La spec launcher-shell prévoit une rangée Up Next au-dessus de la grille, mais rien n'existe encore. Décision produit validée : la rangée agrège les programmes watch next de **toutes les apps visibles dans le TV Provider** (pas seulement Jellyfin), avec un dédoublonnage à plusieurs niveaux, et chaque carte ouvre l'**intent publié par son programme** (`COLUMN_INTENT_URI`), sinon l'app est lancée. La recherche (autre moitié du P2c du README) fait l'objet d'un change séparé.

## What Changes
- **Source unique : le TV Provider Android** (`WatchNextPrograms` / `PreviewPrograms`, lu sous `READ_TV_LISTINGS`) : items en cours et à suivre publiés par toutes les apps, sans appels réseau ni credentials
- **Dédoublonnage en 5 niveaux** (clé d'identité, de la plus fiable à la moins fiable) : doublons exacts dans une app, série dans une app, épisode entre apps, film entre apps, puis en P4 les IDs IMDb/TVDB ajoutés par l'enrichissement BetaSeries
- **Gagnant d'un doublon** (décision produit) : une reprise en cours (position > 0) bat un « à suivre » ; ensuite l'engagement le plus récent ; à égalité, l'ordre de préférence des apps (constante, Jellyfin d'abord)
- **Tri** : items `CONTINUE` d'abord (par `last_engagement_time` décroissant), puis `NEXT`/`NEW` (même tri, à égalité par `_ID` croissant) ; tri stable ; `WATCHLIST` exclu de la rangée
- **Position réglable** (décision produit) : une ligne style tvOS en tête de la zone grille, au-dessus des apps ; réglage p2b « Position d'Up Next » : *avant la grille* (défaut) ou *après la grille*
- **Aucun dédoublonnage entre le héro et Up Next** (décision produit) : le héro peut montrer les mêmes contenus, comme sur tvOS
- **Menu « Ouvrir avec… »** (décision produit) : appui long sur une carte, liste des apps qui ont ce contenu, ouverture via leur intent, rien n'est persisté — remplace l'override par série
- **Badge app** : petite icône de l'app source prise dans le `PackageManager` (aucun logo embarqué)
- **Cartes 16:9 uniformes**, poster portrait centré sur fond sombre, barre de progression pour les items en cours, états chargement / erreur / vide / permission refusée
- **Modèle canonique `UpNextItem`** avec champ `externalIds` facultatif, prêt pour l'enrichissement BetaSeries de P4

## Impact
- specs affectées : nouvelle capability `up-next` (delta ci-dessous)
- launcher-shell : deltas MODIFIED « Navigation 3 paliers » (séquence héro → dock → Up Next → apps) et « Rangée Up Next » (délégation à up-next)
- ui-testing : delta MODIFIED « Navigation D-pad des trois zones » (quatre zones avec Up Next) et couverture des états de la rangée
- jellyfin-integration : reste le contrat de données (TV Provider), l'UI et l'ouverture sont décrites par up-next — les deux capabilities se renvoient l'une à l'autre, aucune contradiction
- settings (change `p2b-settings`) : nouveau réglage « Position d'Up Next » à ajouter de son côté (non édité ici)
- persistance : aucune nouvelle persistance (le menu « Ouvrir avec… » ne persiste rien) ; l'ordre de préférence des apps est une constante en p2c
- **Dépendance** : le change `jellyfin-tvprovider-only` doit être fusionné avant l'implémentation de p2c
- **Vérification sur l'appareil** : la visibilité réelle des lignes `WatchNextPrograms` des autres apps sous `READ_TV_LISTINGS` doit être confirmée sur la TCL avant tout code (tâche 1)

## Non-goals
- **Routage de lecture par score et deep links** (Stremio `autoPlay`, `source=30`) : retiré de p2c, pourra revenir en P4 avec les IDs externes
- **Override par série persisté** : remplacé par le menu « Ouvrir avec… » (rien n'est persisté)
- **BetaSeries comme source** : en P4, BetaSeries sert d'abord à **enrichir** les items de toutes les sources avec des IDs externes IMDb/TVDB (niveau 5 de dédoublonnage) ; la rangée reste multi-sources
- **Correspondance approximative des titres** : égalité stricte après normalisation, jamais de fusion à tort ; les titres localisés différemment restent en double jusqu'à P4
- Recherche (change séparé), réglage d'ordre de préférence des apps (constante en p2c), multi-comptes
