# Change : jellyfin-tvprovider-only

## Why
La spec `jellyfin-integration` actuelle mentionne des credentials saisis dans Settings et une authentification serveur Jellyfin. Décision prise en amont de P6 : aucune connexion directe au serveur ; l'intégration Jellyfin passe exclusivement par le TV Provider Android (watch next publiés par le client Jellyfin installé). La recherche P7 est reportée dans un change dédié : elle pourra passer par l'intent `ACTION_SEARCH` accepté par le client Jellyfin (`StartupActivity`), sans API directe.

## What Changes
- **Suppression de l'exigence « Configuration »** (credentials / authentification) : plus rien à configurer, la capability ne parle jamais au serveur Jellyfin.
- **Réécriture de « Continue watching / Up Next » en pur contrat de données** : la source est le TV Provider (`WatchNextPrograms` publié par le client Jellyfin installé et authentifié sur l'appareil) ; l'affichage, les états et l'ouverture des contenus sont spécifiés par la capability up-next, qui se nourrit de toutes les apps publiantes.
- **Prérequis explicite** : la capability dépend d'un client Jellyfin publié dans le TV Provider ; sans client (ou sans programmes), les états affichés suivent up-next.
- **Résilience** reformulée : tolérer un provider vide ou indisponible sans crash, les états UI suivant up-next.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `jellyfin-integration` : REMOVED « Configuration » ; MODIFIED « Continue watching / Up Next » (contrat de données TV Provider) et « Résilience » (renvoi à up-next)

## Non-goals
- Écran de configuration, credentials, appels réseau vers un serveur Jellyfin : abandonnés définitivement, aucune phase ne les prévoit.
- Règles d'affichage, d'état UI ou d'ouverture : elles appartiennent à up-next (P6, change `up-next`).
- Recherche dans Jellyfin : change séparé (P7, recherche).
- Enrichissement des programmes Jellyfin par des IDs externes : P8 (`betaseries-integration`).

## Impact
- specs affectées : `jellyfin-integration` (delta ci-dessous, MODIFIED + REMOVED) ; le `Purpose` de la capability est corrigé dans la même PR (la description « serveur homelab » ne correspond plus)
- dépendance de P6 levée : la rangée Up Next se nourrit du TV Provider, la permission `READ_TV_LISTINGS` est déjà demandée au premier lancement
- Aucun code : le repo ne contient ni code réseau Jellyfin, ni stockage de credentials, ni entrée de réglages Jellyfin ; le change est spec-only
