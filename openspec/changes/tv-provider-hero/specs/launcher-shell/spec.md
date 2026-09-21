# Delta launcher-shell

## ADDED Requirements

### Requirement: Contenu héro TV Provider
Le héro SHALL afficher les programmes publiés dans le TV Provider système par les apps installées (watch next + preview programs), sans configuration utilisateur.

#### Scenario: programmes disponibles
- **WHEN** une ou plusieurs apps installées publient des programmes dans le TV Provider
- **THEN** le carrousel héro les affiche : poster (posterArtUri, fallback thumbnailUri), titre et progression visible quand disponible, ordre déterministe (reprises d'abord, puis plus récents), toutes apps confondues, sans liste d'apps codée en dur

#### Scenario: fallback
- **WHEN** aucune app ne publie de programme dans le TV Provider
- **THEN** le héro utilise la vidéo aérienne en boucle (comportement hero-home inchangé), sans crash ni écran vide

### Requirement: Clic programme publié
Le clic sur un poster du TV Provider SHALL ouvrir le contenu via l'intent publié par l'app source.

#### Scenario: ouverture
- **WHEN** l'utilisateur clique sur un poster héro issu du TV Provider
- **THEN** l'app source s'ouvre sur la fiche du contenu (intent du programme), fallback : lancement simple de l'app si l'intent échoue

#### Scenario: progression
- **WHEN** le programme expose une position de lecture (watch next)
- **THEN** une barre de progression est visible sur le poster héro
