# Change : jellyfin-tvprovider-only

## Why
La spec `jellyfin-integration` actuelle mentionne des credentials saisis dans Settings et une authentification serveur Jellyfin. Décision prise en amont de P2c : aucune connexion directe au serveur — l'intégration Jellyfin passe exclusivement par le TV Provider Android (watch next + preview programs publiés par le client Jellyfin installé). La recherche de P2c est reportée : elle aurait exigé une API directe, hors scope pour le moment.

## What Changes
- **Suppression de l'exigence « Configuration »** (credentials / authentification) : plus rien à configurer, la capability ne parle jamais au serveur Jellyfin.
- **Réécriture de « Continue watching / Up Next »** : la source est le TV Provider (`WatchNextPrograms` publié par le client Jellyfin installé et authentifié sur l'appareil), avec titre, poster, position de reprise et deep link.
- **Prérequis explicite** : la capability dépend d'un client Jellyfin publié dans le TV Provider ; sans client (ou sans programmes), l'app affiche l'état vide standard, jamais d'erreur ni de config à remplir.
- **Résilience** conservée, reformulée : le TV Provider est local, le seul risque est un provider vide ou indisponible.

## Impact
- specs affectées : `jellyfin-integration` (delta ci-dessous, MODIFIED + REMOVED)
- dépendance de P2c levée : la rangée Up Next se nourrit du TV Provider, la permission `READ_TV_LISTINGS` est déjà demandée au premier lancement
- Aucun code de réseau, aucun stockage de credentials, aucune entrée réglages Jellyfin
