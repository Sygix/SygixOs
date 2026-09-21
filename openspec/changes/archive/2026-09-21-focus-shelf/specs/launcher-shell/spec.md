# Delta launcher-shell

## ADDED Requirements

### Requirement: Panneau Top Shelf au focus
Quand une tuile de la grille prend le focus, un panneau de posters de l'app SHALL s'insérer dans le layout sous la rangée focusée, sans bloquer la navigation DPAD.

#### Scenario: insertion
- **WHEN** une tuile d'une rangée prend le focus
- **THEN** un panneau plein largeur (40-50% de la hauteur d'écran, coins arrondis, ombre douce) s'insère sous cette rangée, les rangées du dessous glissent vers le bas, et la navigation DPAD reste rangée par rangée (le panneau n'est pas focusable)

#### Scenario: déplacement du focus
- **WHEN** le focus passe à une tuile d'une autre rangée
- **THEN** le panneau se déplace sous la nouvelle rangée focusée avec une animation fluide

### Requirement: Contenu du panneau
Le panneau SHALL afficher les preview programs publiés par l'app focus (TV Provider système).

#### Scenario: contenu disponible
- **WHEN** l'app focusée publie des posters (preview programs / watch next)
- **THEN** le panneau affiche un poster dominant avec fondu croisé 300-400ms easing Apple et défilement lent (Ken Burns) entre les posters de l'app

#### Scenario: pas de contenu
- **WHEN** l'app focusée ne publie rien
- **THEN** aucun panneau n'est inséré, la grille reste classique

### Requirement: Fond de la zone grille
Dans la grille, le fond SHALL être un dégradé neutre uni type tvOS, le héro étant complètement masqué.

#### Scenario: révélation de la grille
- **WHEN** l'utilisateur passe en zone grille
- **THEN** le héro disparaît (alpha 0), le fond est le dégradé neutre (pas d'aerial, pas de posters en fond de grille)
