# Change : jellyfin-tvprovider-only

## Why
La spec `jellyfin-integration` actuelle mentionne des credentials saisis dans Settings et une authentification serveur Jellyfin. Décision prise en amont de P2c : aucune connexion directe au serveur — l'intégration Jellyfin passe exclusivement par le TV Provider Android (watch next publiés par le client Jellyfin installé). La recherche de P2c est reportée dans un change dédié : elle pourra passer par l'intent `ACTION_SEARCH` accepté par le client Jellyfin (`StartupActivity`), sans API directe.

## What Changes
- **Suppression de l'exigence « Configuration »** (credentials / authentification) : plus rien à configurer, la capability ne parle jamais au serveur Jellyfin.
- **Réécriture de « Continue watching / Up Next » en pur contrat de données** : la source est le TV Provider (`WatchNextPrograms` publié par le client Jellyfin installé et authentifié sur l'appareil) ; l'affichage, les états et l'ouverture des contenus sont spécifiés par la capability up-next, qui se nourrit de toutes les apps publiantes.
- **Prérequis explicite** : la capability dépend d'un client Jellyfin publié dans le TV Provider ; sans client (ou sans programmes), les états affichés suivent up-next.
- **Résilience** reformulée : tolérer un provider vide ou indisponible sans crash, les états UI suivant up-next.

## Non-goals
- Aucun écran de configuration, aucun credential, aucun appel réseau vers un serveur Jellyfin.
- Aucune règle d'affichage, d'état UI ou d'ouverture ici : elles appartiennent à up-next.

## Impact
- specs affectées : `jellyfin-integration` (delta ci-dessous, MODIFIED + REMOVED) ; le `Purpose` de la capability est corrigé dans la même PR (la description « serveur homelab » ne correspond plus)
- dépendance de P2c levée : la rangée Up Next se nourrit du TV Provider, la permission `READ_TV_LISTINGS` est déjà demandée au premier lancement
- Aucun code de réseau, aucun stockage de credentials, aucune entrée réglages Jellyfin
