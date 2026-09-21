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
- **WHEN** un test cible une zone (héro, dock, grille, panneau shelf) ou une tuile d'app
- **THEN** il la sélectionne par testTag documenté (ex. « zone-dock », « app-tile-&lt;package&gt; »), sans dépendre du libellé affiché

### Requirement: Couverture dock
Le dock SHALL être couvert par des tests de rendu et de focus.

#### Scenario: dock avec apps épinglées
- **WHEN** le dock reçoit une liste d'apps épinglées
- **THEN** chaque app est rendue avec son libellé et la première tuile peut prendre le focus

#### Scenario: dock vide
- **WHEN** aucune app n'est épinglée
- **THEN** le message d'invitation à épingler est affiché

### Requirement: Couverture grille et shelf
La grille SHALL être couverte par des tests de rendu, d'insertion du panneau shelf et d'épinglage.

#### Scenario: grille rendue
- **WHEN** le catalogue contient des apps non épinglées
- **THEN** chaque app apparaît une fois en grille, sans doublon avec le dock

#### Scenario: panneau shelf au focus
- **WHEN** une tuile d'app avec contenu publié prend le focus
- **THEN** le panneau shelf est inséré dans la grille (présence vérifiée par testTag) ; **WHEN** l'app ne publie rien THEN aucun panneau n'est inséré

#### Scenario: épinglage depuis la grille
- **WHEN** un appui long est déclenché sur une tuile de la grille
- **THEN** le callback d'épinglage est appelé avec le package de cette tuile

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

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la grille
- **THEN** le focus revient au dock puis au héro

### Requirement: Injection des dépendances UI
Les composables consommant des sources de données système (TV Provider) SHALL recevoir leur fournisseur par paramètre avec valeur par défaut, pour rester testables sans environnement Android réel.

#### Scenario: fournisseur de posters injecté
- **WHEN** HomeGrid est composé dans un test
- **THEN** le fournisseur de posters du shelf est injectable (contenu déterministe), sans modifier les appelants de production
