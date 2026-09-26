# Delta : jellyfin-integration

## REMOVED Requirements

### Requirement: Configuration
**Reason :** Aucune connexion au serveur Jellyfin : l'intégration passe exclusivement par le TV Provider Android, il n'y a ni credentials ni authentification.
**Migration :** Aucune : aucun écran, stockage ni code de configuration Jellyfin n'existe dans le launcher.

## MODIFIED Requirements

### Requirement: Continue watching / Up Next
Le client Jellyfin SHALL être consommé exclusivement via les programmes qu'il publie dans le TV Provider Android (`WatchNextPrograms`) ; l'affichage, les états et l'ouverture des contenus sont spécifiés par la capability up-next.

#### Scenario: reprise
- **WHEN** un client Jellyfin installé publie des programmes watch next avec position de lecture
- **THEN** le launcher lit ces programmes dans le TV Provider avec les champs publiés (titre, poster, progression, intent), sans aucun appel direct au serveur Jellyfin ; l'affichage et l'ouverture suivent up-next

#### Scenario: aucun client Jellyfin
- **WHEN** aucun programme Jellyfin n'est présent dans le TV Provider (client absent, non authentifié ou sans contenu en cours)
- **THEN** l'état affiché suit « États de la rangée » de up-next, sans erreur ni invitation à configurer quoi que ce soit

### Requirement: Résilience
L'app SHALL tolérer un TV Provider vide ou indisponible sans crash ; les états affichés dans ces cas sont ceux spécifiés par la capability up-next.

#### Scenario: comportement
- **WHEN** la requête vers le TV Provider échoue ou que le provider est absent
- **THEN** aucun crash et l'état affiché suit l'exigence « États de la rangée » de up-next
