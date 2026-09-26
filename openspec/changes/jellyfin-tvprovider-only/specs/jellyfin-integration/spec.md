# Delta : jellyfin-integration

## REMOVED Requirements

### Requirement: Configuration
**Reason :** Aucune connexion au serveur Jellyfin : l'intégration passe exclusivement par le TV Provider Android, il n'y a ni credentials ni authentification.

## MODIFIED Requirements

### Requirement: Continue watching / Up Next
Le home SHALL afficher les items en cours et les épisodes Next Up tels que publiés dans le TV Provider Android (`WatchNextPrograms`) par le client Jellyfin installé, avec poster, titre, progression et reprise via l'intent du programme.

#### Scenario: reprise
- **WHEN** un client Jellyfin installé publie des programmes watch next avec position de lecture THEN le home les affiche avec poster, titre et progression, et l'appui ouvre l'intent de reprise fourni par le programme

#### Scenario: aucun client Jellyfin
- **WHEN** aucun programme Jellyfin n'est présent dans le TV Provider (client absent, non authentifié ou sans contenu en cours) THEN la rangée concernée applique l'état vide standard (discret ou masquée selon le layout), sans erreur ni invitation à configurer quoi que ce soit

### Requirement: Résilience
L'app SHALL tolérer un TV Provider vide ou indisponible sans crash, avec état vide + retry au retour au launcher.

#### Scenario: comportement
- **WHEN** la requête vers le TV Provider échoue THEN état UI vide + retry au prochain retour au launcher, jamais de crash
