# Capability : jellyfin-integration

## Purpose
Intégration complète Jellyfin (serveur homelab) : reprise de lecture, Up Next, posters, recherche.

## Requirements
### Requirement : Configuration
#### Scenario : réglage
- WHEN l'utilisateur ouvre Settings > Jellyfin THEN il saisit URL du serveur et s'authentifie via le mécanisme applicatif Jellyfin (pas d'API developer) ; credentials persistés localement

### Requirement : Continue watching / Up Next
#### Scenario : reprise
- WHEN des items sont en cours THEN le home affiche posters, progression et reprise de lecture via deep link vers le client Jellyfin

### Requirement : Résilience
- WHEN le serveur est injoignable THEN état UI erreur + retry, jamais de crash ; délai de réponse borné
