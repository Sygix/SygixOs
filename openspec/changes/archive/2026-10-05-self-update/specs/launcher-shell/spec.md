# Delta launcher-shell

## ADDED Requirements

### Requirement: Pastille
Le design system SHALL fournir un composant pastille unique, réutilisable par tout élément de l'interface qui doit signaler une nouveauté : un point plein de 6 dp de diamètre, bleu système (`#0A84FF`), ancré au coin supérieur droit de l'élément qu'il signale (ou en fin de ligne pour une ligne de liste). La pastille SHALL n'être jamais focusable, ne SHALL changer ni la taille, ni la position, ni le focus, ni la navigation D-pad de l'élément qui la porte, SHALL suivre cet élément dans tous ses mouvements (défilement, zoom de focus), et n'a aucune animation continue. Les fonctionnalités qui affichent une pastille (par exemple « Pastille de mise à jour » de `self-update`) SHALL utiliser ce composant, sans en redéfinir l'aspect.

#### Scenario: affichage
- **WHEN** un élément porte une pastille
- **THEN** un point bleu plein de 6 dp est dessiné à son coin supérieur droit (ou en fin de ligne pour une ligne de liste), par-dessus l'élément, sans décaler son contenu

#### Scenario: focus inchangé
- **WHEN** l'utilisateur déplace le focus sur ou à côté d'un élément qui porte une pastille
- **THEN** la pastille ne prend jamais le focus, et le focus, la taille et la position de l'élément sont identiques à ceux du même élément sans pastille

#### Scenario: élément en mouvement
- **WHEN** l'élément qui porte la pastille défile ou change d'échelle (focus)
- **THEN** la pastille reste à la même place relative sur l'élément

#### Scenario: écran au repos
- **WHEN** une pastille est affichée et que l'utilisateur ne touche à rien
- **THEN** la pastille ne provoque aucun redessin continu
