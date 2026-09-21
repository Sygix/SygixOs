# Delta launcher-shell

## ADDED Requirements

### Requirement: Contenu héro Jellyfin
Le héro SHALL afficher les programmes Jellyfin publiés dans le TV Provider système (continue watching + récents), sans configuration utilisateur.

#### Scenario: programmes disponibles
- **WHEN** l'app Jellyfin Android TV a publié des programmes (package Jellyfin détecté) dans le TV Provider
- **THEN** le carrousel héro les affiche : poster (posterArtUri, fallback thumbnailUri), titre et progression visibles, ordre déterministe (reprises d'abord, puis plus récents)

#### Scenario: fallback
- **WHEN** Jellyfin est absent, non connecté, ou ne publie aucun programme
- **THEN** le héro utilise la vidéo aérienne en boucle (comportement hero-home inchangé), sans crash ni écran vide

### Requirement: Clic programme Jellyfin
Le clic sur un poster Jellyfin SHALL ouvrir la fiche du contenu via l'intent publié par Jellyfin.

#### Scenario: ouverture
- **WHEN** l'utilisateur clique sur un poster héro Jellyfin
- **THEN** l'app Jellyfin s'ouvre sur la fiche du contenu (intent du programme), fallback : lancement simple de l'app si l'intent échoue

#### Scenario: progression
- **WHEN** le programme expose une position de lecture
- **THEN** une barre de progression est visible sur le poster héro
