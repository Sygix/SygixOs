# Delta : capability ui-testing

## ADDED Requirements

### Requirement: Tests UI sans capture d'image
La suite UI SHALL tester l'écran home par assertions sémantiques (Robolectric + Compose), sans dépendance de capture d'image ni de comparaison visuelle.

#### Scenario: indépendance des tests
- **WHEN** la suite UI s'exécute
- **THEN** tous les tests tournent sur JVM via Robolectric, sans émulateur ni génération de PNG, et font partie de la task `test` standard

### Requirement: Sélecteurs stables
Les zones et tuiles de l'écran home SHALL exposer des testTags stables utilisés par les tests à la place de sélecteurs fragiles (texte, position).

#### Scenario: sélection par tag
- **WHEN** un test cible une zone (héro, dock, grille, panneau shelf), le menu contextuel ou une tuile d'app
- **THEN** il le sélectionne par testTag documenté (« zone-hero », « zone-dock », « zone-grid », « shelf-panel », « app-menu », préfixe « app-tile-<package> »), sans dépendre du libellé affiché

### Requirement: Couverture dock
Le dock SHALL être couvert par des tests de rendu et de focus.

#### Scenario: dock avec apps épinglées
- **WHEN** le dock reçoit une liste d'apps épinglées
- **THEN** chaque app est rendue avec son libellé et la première tuile peut prendre le focus

#### Scenario: dock vide
- **WHEN** aucune app n'est épinglée
- **THEN** le message d'invitation à épingler est affiché

### Requirement: Couverture grille et shelf
La grille SHALL être couverte par des tests de rendu et d'insertion du panneau shelf.

#### Scenario: grille rendue
- **WHEN** le catalogue contient des apps non épinglées
- **THEN** chaque app apparaît une fois en grille, sans doublon avec le dock

#### Scenario: grille vide
- **WHEN** le catalogue ne contient aucune app
- **THEN** le message « Aucune app TV détectée » est affiché

#### Scenario: panneau shelf au focus
- **WHEN** une tuile d'app avec contenu publié prend le focus
- **THEN** le panneau shelf est inséré dans la grille (présence vérifiée par testTag) ; **WHEN** l'app ne publie rien THEN aucun panneau n'est inséré

### Requirement: Couverture héro
Le héro SHALL être couvert par des tests de rendu du carrousel et de l'état fallback.

#### Scenario: carrousel avec programmes
- **WHEN** des HeroItem sont fournis
- **THEN** le premier programme est rendu (titre affiché, progression visible quand exposée)

#### Scenario: fallback sans contenu
- **WHEN** aucun HeroItem n'est fourni
- **THEN** l'écran ne crash pas et le fallback visuel est rendu

### Requirement: Navigation D-pad des trois zones
La navigation D-pad entre héro, dock et grille SHALL être couverte par des tests simulant les key events physiques.

#### Scenario: descente
- **WHEN** l'utilisateur presse bas depuis le héro (dock non vide)
- **THEN** le focus passe au dock ; **WHEN** il presse bas depuis le dock THEN la grille prend le focus

#### Scenario: descente sans dock
- **WHEN** l'utilisateur presse bas depuis le héro alors que le dock est vide
- **THEN** la grille prend le focus directement

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la grille (première ligne)
- **THEN** le focus revient au dock puis au héro

#### Scenario: retour au héro
- **WHEN** l'utilisateur presse Retour depuis la grille
- **THEN** le héro reprend le focus

### Requirement: Épinglage depuis la grille
L'épinglage depuis la grille SHALL être couvert par le flux complet : appui long, menu contextuel, action d'épinglage.

#### Scenario: menu à l'appui long
- **WHEN** la touche OK est maintenue sur une tuile de la grille (durée LONG_PRESS_MS)
- **THEN** le menu contextuel s'ouvre (testTag « app-menu ») et affiche l'action d'épinglage

#### Scenario: épinglage confirmé
- **WHEN** l'action « Épingler au dock » est activée dans le menu
- **THEN** le callback d'épinglage est appelé avec le package de la tuile et le menu se ferme

#### Scenario: annulation
- **WHEN** la touche Retour est pressée pendant que le menu est ouvert
- **THEN** le menu se ferme sans appeler le callback d'épinglage

### Requirement: Composables testables sans environnement Android
Les composables de l'écran home SHALL pouvoir être composés dans un test avec un contenu déterministe, sans accès aux sources système (TV Provider, PackageManager).

#### Scenario: contenu déterministe en test
- **WHEN** LauncherHome est composé dans un test
- **THEN** le catalogue et le HeroState (programmes, visuels validés/contrôlés) sont fournis par paramètres sans requête au TV Provider, et le rendu des tuiles ne dépend pas du PackageManager (artwork déterministe ou absent)
