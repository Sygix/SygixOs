# Delta ui-testing

## MODIFIED Requirements

### Requirement: Sélecteurs stables
Les zones et tuiles de l'écran home SHALL exposer des testTags stables utilisés par les tests à la place de sélecteurs fragiles (texte, position).

#### Scenario: sélection par tag
- **WHEN** un test cible une zone (héro, dock, grille, panneau shelf, rangée Up Next), un menu contextuel, une tuile d'app ou une carte Up Next
- **THEN** il le sélectionne par testTag documenté (« zone-hero », « zone-dock », « zone-grid », « shelf-panel », « zone-upnext », « app-menu », « upnext-menu », préfixes « app-tile-<package> » et « upnext-card-<key> »), sans dépendre du libellé affiché

### Requirement: Navigation D-pad des trois zones
La navigation D-pad entre héro, dock et grille, rangée Up Next incluse dans la zone grille, SHALL être couverte par des tests simulant les key events physiques, exécutés à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`).

#### Scenario: descente
- **WHEN** l'utilisateur presse bas depuis le héro (dock non vide)
- **THEN** le focus passe au dock, puis, à la première entrée dans la zone grille, à la rangée Up Next (première ligne de la zone grille), puis à la première rangée d'apps

#### Scenario: descente sans dock
- **WHEN** l'utilisateur presse bas depuis le héro alors que le dock est vide
- **THEN** à la première entrée dans la zone grille, la rangée Up Next prend le focus directement, puis la première rangée d'apps

#### Scenario: descente depuis le dock
- **WHEN** l'utilisateur presse bas depuis le dock
- **THEN** à la première entrée dans la zone grille, la rangée Up Next prend le focus, puis la première rangée d'apps

#### Scenario: descente sans rangée Up Next
- **WHEN** l'utilisateur descend depuis le dock alors que la rangée Up Next est masquée
- **THEN** à la première entrée dans la zone grille, la première rangée d'apps prend le focus directement

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la première rangée d'apps
- **THEN** la rangée Up Next reprend le focus si elle est affichée en tête, puis le dock puis le héro ; si elle est masquée, le focus revient au dock puis au héro

#### Scenario: position après la grille
- **WHEN** le réglage « Position d'Up Next » vaut « après la grille »
- **THEN** bas depuis le dock donne le focus à la première rangée d'apps, bas depuis la dernière rangée d'apps donne le focus à la rangée Up Next, bas depuis la rangée ne fait rien, haut depuis la rangée revient à la dernière rangée d'apps

#### Scenario: retour au héro
- **WHEN** l'utilisateur presse Retour depuis la zone grille
- **THEN** le héro reprend le focus

#### Scenario: restauration au retour d'une app
- **WHEN** le test simule l'ouverture d'une app depuis une carte Up Next puis le retour au launcher
- **THEN** la position précédente dans la grille est restaurée et le focus revient sur la carte d'origine, ou au début de la section Up Next si cette carte a disparu

#### Scenario: rangée devenue vide
- **WHEN** le test simule le retour au launcher avec une rangée Up Next devenue vide, pour chaque position réglée
- **THEN** la rangée est absente et le focus est sur la première rangée d'apps (position « avant la grille ») ou sur la dernière (position « après la grille »)

#### Scenario: états de la rangée
- **WHEN** la rangée est composée avec chaque état (chargement initial, rechargement, erreur, vide, permission refusée)
- **THEN** le rendu correspond : squelette au premier chargement seulement, état précédent conservé au rechargement, carte « Réessayer » focusable en erreur, rangée masquée si vide ou permission refusée

#### Scenario: menu Ouvrir avec
- **WHEN** l'appui long est simulé sur une carte Up Next
- **THEN** le menu « Ouvrir avec… » s'ouvre (testTag « upnext-menu »), y compris avec une seule source, OK ouvre l'app choisie, Retour ferme le menu et rend le focus à la carte

#### Scenario: réglage de position
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages et bascule « Position d'Up Next »
- **THEN** la valeur change au D-pad (OK, gauche, droite) sans boucle, Retour rend le focus au volet des catégories, et le home recomposé place la rangée à la position choisie

#### Scenario: réglage de visibilité
- **WHEN** le test désactive « Afficher Up Next » dans « Écran d'accueil » puis revient au home
- **THEN** la rangée est masquée, la navigation la saute, le réglage est conservé après redémarrage, et la rangée réapparaît si le réglage est réactivé
