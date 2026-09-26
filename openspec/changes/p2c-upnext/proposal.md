# Change : p2c-upnext

## Why
P2c du roadmap : rangée Up Next dédiée. La spec launcher-shell prévoit une rangée Up Next au-dessus de la grille (« fusion Jellyfin puis BetaSeries, tri par date d'activité ») mais rien n'existe encore. Ce change matérialise cette exigence avec un **modèle provider-agnostic** : la rangée est alimentée par des providers (Jellyfin en p2c, BetaSeries en P4) et chaque carte route la lecture vers la **bonne app installée** (Jellyfin, Stremio, …) via un système de score de priorité, au lieu de lier la rangée à une seule app. La recherche (autre moitié du P2c du README) fait l'objet d'un change séparé.

## What Changes
- **Modèle Up Next provider-agnostic** : item canonique (série, saison, épisode, titre, poster, progression, IDs externes IMDb/TVDB, date d'activité, app source) alimenté par une interface provider ; Jellyfin est le premier provider, BetaSeries (P4) n'ajoutera qu'un adapter
- **Fusion Continue Watching + Next Up** : épisodes en cours + films en cours (barre de progression) + épisodes à suivre de l'API `/Shows/NextUp`, dédoublonnés (l'épisode en cours gagne) et triés par date d'activité décroissante
- **Badge app cible** : petit logo de l'app de lecture résolue sur chaque carte, sans texte
- **Routage de lecture par adapter** avec chaîne de fallback :
  - Jellyfin : `ACTION_VIEW` (item ID, catégorie `LEANBACK_LAUNCHER`, extra `source=30`) sur `StartupActivity` — intent non documenté, fallback lancement de l'app
  - Stremio : deep link `stremio:///detail/series/{imdb}/{imdb}:{s}:{e}` (`autoPlay=true`), fallback lancement de l'app
  - Apps sans deep link (Netflix, etc.) : lancement de l'app seul, sobre
- **Score de priorité** : app cible = ordre de préférence persisté (défaut Jellyfin > Stremio) × disponibilité catalogue ; l'override par série est prioritaire sur le score
- **Override par série** : menu contextuel sur une carte (« Toujours ouvrir avec … », « Retirer l'override »), persistance de l'ensemble des exceptions (jamais un snapshot)
- **États de la rangée** : loading / vide / erreur + retry, timeouts bornés ; rangée masquée si aucun serveur Jellyfin n'est configuré

## Impact
- specs affectées : nouvelle capability `up-next` (delta ci-dessous)
- launcher-shell : l'exigence « Rangée Up Next » devient implémentée par cette capability (pas de modification de son texte)
- jellyfin-integration : l'exigence « Continue watching / Up Next » (reprise via deep link vers le client) devient implémentée par cette capability ; l'écran Settings > Jellyfin est couvert par le change `jellyfin-config` séparé
- **Dépendance** : le change `jellyfin-config` (URL serveur + auth applicative Jellyfin, compte unique) doit être spécifié et implémenté avant l'implémentation de p2c ; p2c consomme la config qu'il fournit
- persistance : nouveaux états DataStore (ordre de lecture, overrides par série)
- hors scope : recherche (change séparé), BetaSeries (P4, adapter seulement), UI de réordre de l'ordre de lecture global (l'override par série couvre le besoin immédiat), multi-comptes Jellyfin
