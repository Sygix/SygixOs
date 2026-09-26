# Delta ui-testing

Le change `ui-testing` est archivé dans cette PR (commit dédié) : « Navigation D-pad des trois zones » existe désormais dans les specs courantes, le delta ci-dessous est donc un MODIFIED réel.

## MODIFIED Requirements

### Requirement: Navigation D-pad des trois zones
La navigation D-pad entre héro, dock, rangée Up Next et grille SHALL être couverte par des tests simulant les key events physiques, exécutés à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`).

#### Scenario: descente
- **WHEN** l'utilisateur presse bas depuis le héro (dock non vide)
- **THEN** le focus passe au dock, puis à la rangée Up Next, puis à la première rangée d'apps

#### Scenario: descente sans dock
- **WHEN** l'utilisateur presse bas depuis le héro alors que le dock est vide
- **THEN** la rangée Up Next prend le focus directement, puis la première rangée d'apps

#### Scenario: descente depuis le dock
- **WHEN** l'utilisateur presse bas depuis le dock
- **THEN** la rangée Up Next prend le focus, puis la première rangée d'apps

#### Scenario: descente sans rangée Up Next
- **WHEN** l'utilisateur descend depuis le dock alors que la rangée Up Next est masquée
- **THEN** la première rangée d'apps prend le focus sans palier intermédiaire

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la première rangée d'apps
- **THEN** la rangée Up Next reprend le focus si elle est affichée, puis le dock puis le héro ; si elle est masquée, le focus revient au dock puis au héro

#### Scenario: retour au héro
- **WHEN** l'utilisateur presse Retour depuis la zone grille
- **THEN** le héro reprend le focus

#### Scenario: restauration au retour d'une app
- **WHEN** le test simule l'ouverture d'une app depuis une carte Up Next puis le retour au launcher
- **THEN** le focus est restauré sur la carte d'origine, ou sur la première carte si celle-ci a disparu

#### Scenario: états de la rangée
- **WHEN** la rangée est composée avec chaque état (chargement initial, rechargement, erreur, vide, permission refusée)
- **THEN** le rendu correspond : squelette au premier chargement seulement, état précédent conservé au rechargement, carte « Réessayer » focusable en erreur, rangée masquée si vide ou permission refusée

#### Scenario: menu Ouvrir avec
- **WHEN** l'appui long est simulé sur une carte Up Next
- **THEN** le menu « Ouvrir avec… » s'ouvre (testTag « upnext-menu »), OK ouvre l'app choisie, Retour ferme le menu et rend le focus à la carte
