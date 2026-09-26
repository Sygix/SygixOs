# Delta settings

Le change `p2b-settings` (non encore archivé) introduit la capability `settings` : l'exigence ci-dessous est déclarée ADDED et complète ce change ; à fusionner avec la capability lors de l'archivage. `p2b-settings` est une dépendance de ce change.

## ADDED Requirements

### Requirement: Position d'Up Next
Les réglages SHALL proposer une catégorie dédiée à Up Next avec un contrôle « Position d'Up Next » valant « avant la grille » (défaut) ou « après la grille », persisté dans DataStore.

#### Scenario: contrôle
- **WHEN** l'utilisateur ouvre la catégorie Up Next des réglages
- **THEN** le contrôle « Position d'Up Next » propose « avant la grille » (valeur par défaut) et « après la grille », et le choix est persisté dans DataStore puis restauré au démarrage

#### Scenario: application immédiate
- **WHEN** la valeur du réglage change
- **THEN** la rangée est repositionnée sans redémarrage du launcher

#### Scenario: navigation D-pad dans le cas « après la grille »
- **WHEN** la position vaut « après la grille »
- **THEN** bas depuis la dernière rangée d'apps donne le focus à la rangée Up Next, haut depuis la rangée revient à la dernière rangée d'apps, et la remontée standard (haut depuis la première rangée d'apps) mène au dock puis au héro
