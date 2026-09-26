# Delta launcher-shell

## MODIFIED Requirements

### Requirement: Navigation 3 paliers
Le DPAD SHALL naviguer héro → dock → rangée Up Next → apps (descend) et apps → rangée Up Next → dock → héro (monte) de façon déterministe : seule la zone active est focusable, et le palier Up Next est sauté si la rangée est masquée. La navigation détaillée de la rangée Up Next est spécifiée par la capability up-next.

#### Scenario: descente depuis le héro
- **WHEN** l'utilisateur presse bas depuis le héro
- **THEN** le premier élément du dock prend le focus, ou la rangée Up Next si le dock est vide

#### Scenario: traversée du dock
- **WHEN** l'utilisateur descend depuis le dock et que la rangée Up Next est affichée
- **THEN** la zone grille prend le focus avec le focus sur la rangée Up Next (dernière carte visitée, sinon la première), le dock et le héro se masquent, la lecture du héro est mise en pause

#### Scenario: rangée masquée
- **WHEN** l'utilisateur descend depuis le dock et que la rangée Up Next est masquée
- **THEN** le focus va directement à la première rangée d'apps, sans palier intermédiaire

#### Scenario: déplacements dans la rangée Up Next
- **WHEN** l'utilisateur presse gauche ou droite alors que le focus est sur la rangée Up Next
- **THEN** le focus reste dans la rangée, sans boucle aux bords ; haut depuis la rangée rend le focus au dock (ou au héro si le dock est vide) ; bas passe à la première rangée d'apps

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la première rangée d'apps
- **THEN** la rangée Up Next prend le focus si elle est affichée (dernière carte visitée, sinon la première), sinon le dock reprend le focus (ou le héro si le dock est vide) ; haut depuis la rangée Up Next rend le focus au dock (ou au héro si le dock est vide) ; haut depuis le dock rend le focus au héro et relance sa lecture

#### Scenario: position après la grille
- **WHEN** le réglage « Position d'Up Next » vaut « après la grille »
- **THEN** bas depuis la dernière rangée d'apps donne le focus à la rangée Up Next, haut depuis la rangée Up Next revient à la dernière rangée d'apps, et la remontée standard (haut depuis la première rangée d'apps) mène au dock puis au héro

#### Scenario: rangée devenue vide
- **WHEN** l'utilisateur revient au launcher alors que la rangée Up Next n'a plus de contenu
- **THEN** la rangée est masquée et le focus va à la première rangée d'apps

#### Scenario: retour depuis une app
- **WHEN** l'utilisateur revient au launcher après avoir ouvert une app depuis la rangée Up Next
- **THEN** le focus est restauré sur la carte d'origine, ou sur la première carte si celle-ci a disparu ; Retour depuis la zone grille ramène au héro

#### Scenario: cohabitation avec la Top Shelf
- **WHEN** le focus est sur la rangée Up Next
- **THEN** aucun panneau Top Shelf n'est ouvert ; le panneau reste lié au focus des tuiles d'apps

#### Scenario: couches inactives
- **WHEN** une zone n'est pas active
- **THEN** aucun de ses éléments ne peut prendre le focus ; gauche et droite restent dans la zone active

### Requirement: Rangée Up Next
Le home SHALL afficher une rangée Up Next en tête de la zone grille, alimentée par le TV Provider Android, dont le contenu, le dédoublonnage, les états et l'ouverture sont spécifiés par la capability up-next.

#### Scenario: contenu
- **WHEN** des programmes watch next sont publiés dans le TV Provider
- **THEN** la rangée est affichée en tête de la zone grille au-dessus des apps (position réglable, voir up-next), avec posters et barres de progression

#### Scenario: sans contenu
- **WHEN** aucun programme n'est visible dans le TV Provider, ou que la permission est refusée
- **THEN** la rangée est masquée et la navigation ne présente pas son palier
