# Delta launcher-shell

## ADDED Requirements
### Requirement: Dock d'apps épinglées
Le home SHALL s'ouvrir sur un dock Liquid Glass en bas de l'écran contenant les apps épinglées, celles-ci étant exclues de la grille.

#### Scenario: état initial
- **WHEN** le home s'ouvre
- **THEN** le focus est sur le dock et seules les apps épinglées y sont visibles

#### Scenario: révélation de la grille
- **WHEN** l'utilisateur descend au-delà du dock
- **THEN** la grille complète (sans les épinglées) se révèle plein écran avec une animation fluide, et remonter en haut ramène au dock
