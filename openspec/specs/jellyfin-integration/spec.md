# Capability : jellyfin-integration

## Purpose
Intégration Jellyfin par le TV Provider Android (client installé) : consommation des contenus qu'il publie pour la rangée Up Next ; aucune connexion directe au serveur.

## Requirements
### Requirement: Configuration
Les credentials SHALL être saisis dans Settings et persistés localement.

#### Scenario: réglage
- **WHEN** l'utilisateur ouvre Settings > Jellyfin THEN il saisit URL du serveur et s'authentifie via le mécanisme applicatif Jellyfin (pas d'API developer) ; credentials persistés localement

### Requirement: Continue watching / Up Next
Le home SHALL afficher les items en cours Jellyfin avec progression et reprise.

#### Scenario: reprise
- **WHEN** des items sont en cours THEN le home affiche posters, progression et reprise de lecture via deep link vers le client Jellyfin

### Requirement: Résilience
L'app SHALL tolérer un serveur injoignable sans crash, avec état erreur + retry.

- **WHEN** le serveur est injoignable THEN état UI erreur + retry, jamais de crash ; délai de réponse borné

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent