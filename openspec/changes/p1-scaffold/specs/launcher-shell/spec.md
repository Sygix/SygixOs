# Delta launcher-shell

## ADDED Requirements
### Requirement: Persistance de la grille
L'ordre des apps et les épinglages SHALL être persistés localement (DataStore) et restaurés au démarrage.

#### Scenario: réorganisation
- **WHEN** l'utilisateur épingle ou réordonne une app
- **THEN** l'état est persisté et conservé après redémarrage de l'app
