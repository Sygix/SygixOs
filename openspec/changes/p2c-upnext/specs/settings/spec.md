# Delta settings

Le change `p2b-settings` (mergé, non encore archivé) introduit la capability `settings` : l'exigence ci-dessous est ADDED et vient compléter cette capability. Les MODIFIED sur « Page de réglages » (nouvelle catégorie « Écran d'accueil »), « Apps sources » et « Cacher une application » ne peuvent pas être posés tant que `openspec/specs/settings` n'existe pas ; leur texte exact est dans `design.md` (section « MODIFIED à poser sur settings après l'archivage de p2b-settings ») et leur pose est la première tâche de `tasks.md`.

## ADDED Requirements

### Requirement: Position d'Up Next
La catégorie « Écran d'accueil » des réglages SHALL proposer un contrôle « Position d'Up Next » valant « avant la grille » (défaut) ou « après la grille », persisté dans DataStore, l'effet sur la navigation de la zone grille étant spécifié par « Navigation 3 paliers » de launcher-shell.

#### Scenario: contrôle
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** le contrôle « Position d'Up Next » propose « avant la grille » (valeur par défaut) et « après la grille », et le choix est persisté dans DataStore puis restauré au démarrage

#### Scenario: navigation D-pad du contrôle
- **WHEN** le focus est sur le contrôle « Position d'Up Next »
- **THEN** OK bascule la valeur ; droite passe à la valeur suivante et gauche à la précédente, sans boucle aux bords ; gauche depuis la première valeur et Retour rendent le focus au volet des catégories sur « Écran d'accueil », la valeur choisie étant conservée ; droite depuis le volet des catégories ramène le focus sur le contrôle

#### Scenario: application immédiate
- **WHEN** la valeur du réglage change
- **THEN** la rangée Up Next est repositionnée sans redémarrage du launcher, et la navigation suit le scénario « position après la grille » de launcher-shell
