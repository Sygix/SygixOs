# Delta launcher-shell

## MODIFIED Requirements

### Requirement: Navigation 3 paliers
Le DPAD SHALL naviguer héro → dock → grille (descend) et grille → dock → héro (monte) de façon déterministe : seule la zone active est focusable. La zone grille comprend la rangée Up Next et les rangées d'apps sur le même fond : la rangée Up Next est sa première ligne (réglage « Position d'Up Next » à « avant la grille », défaut) ou sa dernière ligne (« après la grille ») ; elle n'est pas un palier distinct et elle est sautée si elle est masquée.

#### Scenario: descente depuis le héro
- **WHEN** l'utilisateur presse bas depuis le héro
- **THEN** le premier élément du dock prend le focus, ou la zone grille si le dock est vide

#### Scenario: traversée du dock
- **WHEN** l'utilisateur descend depuis le dock
- **THEN** la zone grille plein écran prend le focus sur sa première ligne (dernière carte ou tuile visitée, sinon la première), le dock et le héro se masquent, la lecture du héro est mise en pause

#### Scenario: rangée Up Next affichée en tête
- **WHEN** l'utilisateur descend depuis le dock, le réglage vaut « avant la grille » et la rangée Up Next est affichée
- **THEN** la première ligne de la zone grille est la rangée Up Next : elle prend le focus (dernière carte visitée, sinon la première) ; un bas de plus passe à la première rangée d'apps, sans changement de fond

#### Scenario: rangée masquée
- **WHEN** l'utilisateur descend depuis le dock et que la rangée Up Next est masquée
- **THEN** le focus va directement à la première rangée d'apps

#### Scenario: déplacements dans la rangée Up Next
- **WHEN** l'utilisateur presse gauche ou droite alors que le focus est sur la rangée Up Next
- **THEN** le focus reste dans la rangée, sans boucle aux bords

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la première ligne de la zone grille
- **THEN** le dock reprend le focus (ou le héro si le dock est vide) ; haut depuis le dock rend le focus au héro et relance sa lecture

#### Scenario: remontée depuis les apps
- **WHEN** l'utilisateur presse haut depuis la première rangée d'apps
- **THEN** la rangée Up Next prend le focus si elle est affichée en tête (dernière carte visitée, sinon la première), sinon le dock reprend le focus (ou le héro si le dock est vide)

#### Scenario: position après la grille
- **WHEN** le réglage « Position d'Up Next » vaut « après la grille » et que la rangée est affichée
- **THEN** la rangée Up Next est la dernière ligne de la zone grille : bas depuis la dernière rangée d'apps lui donne le focus, bas depuis la rangée ne fait rien, haut depuis la rangée revient à la dernière rangée d'apps, et la première ligne de la zone grille est la première rangée d'apps

#### Scenario: rangée devenue vide
- **WHEN** l'utilisateur revient au launcher alors que la rangée Up Next n'a plus de contenu
- **THEN** la rangée est masquée et le focus va à la rangée d'apps adjacente : la première si la rangée était avant la grille, la dernière si elle était après

#### Scenario: retour depuis une app
- **WHEN** l'utilisateur revient au launcher après avoir ouvert une app depuis la rangée Up Next
- **THEN** le focus est restauré sur la carte d'origine, ou sur la première carte si celle-ci a disparu

#### Scenario: retour au héro
- **WHEN** l'utilisateur presse Retour depuis la zone grille (rangée Up Next ou rangée d'apps)
- **THEN** le héro reprend le focus et sa lecture

#### Scenario: cohabitation avec la Top Shelf
- **WHEN** le focus est sur la rangée Up Next
- **THEN** aucun panneau Top Shelf n'est ouvert ; le panneau reste lié au focus des tuiles d'apps

#### Scenario: couches inactives
- **WHEN** une zone n'est pas active
- **THEN** aucun de ses éléments ne peut prendre le focus ; gauche et droite restent dans la zone active

### Requirement: Rangée Up Next
Le home SHALL afficher une rangée Up Next dans la zone grille (première ligne par défaut, dernière ligne si le réglage « Position d'Up Next » vaut « après la grille »), alimentée par le TV Provider Android ; son contenu, son dédoublonnage, ses cartes, ses états et l'ouverture des items sont spécifiés par la capability up-next.

#### Scenario: contenu
- **WHEN** des programmes watch next sont publiés dans le TV Provider
- **THEN** la rangée est affichée dans la zone grille, sur le fond de la zone grille, avec posters et barres de progression

#### Scenario: sans contenu
- **WHEN** aucun programme n'est visible dans le TV Provider, ou que la permission est refusée
- **THEN** la rangée est masquée et la navigation de la zone grille commence directement par les rangées d'apps

### Requirement: Sélection des apps sources
Le launcher SHALL permettre, dans ses réglages, de cocher les apps dont les programmes alimentent le héro, le Top Shelf et la rangée Up Next ; par défaut toutes les apps installées sont retenues. L'UI de ce réglage est spécifiée par « Apps sources » de la capability settings.

#### Scenario: app décochée
- **WHEN** l'utilisateur décoche une app dans les réglages du launcher
- **THEN** ses programmes n'apparaissent plus dans le héro, le Top Shelf ni la rangée Up Next, sans redémarrage
