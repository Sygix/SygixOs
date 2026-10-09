# Delta ui-testing

## MODIFIED Requirements

### Requirement: Sélecteurs stables
Les zones et tuiles de l'écran home SHALL exposer des testTags stables utilisés par les tests à la place de sélecteurs fragiles (texte, position).

#### Scenario: sélection par tag
- **WHEN** un test cible une zone (héro, dock, grille, panneau shelf, rangée Up Next), un titre de section de la zone grille, un menu contextuel, une tuile d'app, une carte Up Next ou un contrôle de la catégorie « Écran d'accueil » des réglages
- **THEN** il le sélectionne par testTag documenté (« zone-hero », « zone-dock », « zone-grid », « shelf-panel », « zone-upnext », « section-title-upnext », « section-title-apps », « app-menu », « upnext-menu », « setting-upnext-visible », « setting-upnext-position », « setting-upnext-position-list », préfixes « app-tile-<package> », « upnext-card-<key> » et « setting-upnext-position-<VALUE> », la catégorie elle-même étant « settings-category-HOME_SCREEN »), sans dépendre du libellé affiché

### Requirement: Navigation D-pad des trois zones
La navigation D-pad entre héro, dock et grille, rangée Up Next et titres de section inclus dans la zone grille, SHALL être couverte par des tests simulant les key events physiques, exécutés à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`).

#### Scenario: descente
- **WHEN** l'utilisateur presse bas depuis le héro (dock non vide)
- **THEN** le focus passe au dock, puis, à la première entrée dans la zone grille, à la première carte de la rangée Up Next (première ligne de la zone grille, « section-title-upnext » au-dessus d'elle), puis à la première rangée d'apps (« section-title-apps » au-dessus d'elle)

#### Scenario: descente sans dock
- **WHEN** l'utilisateur presse bas depuis le héro alors que le dock est vide
- **THEN** à la première entrée dans la zone grille, la rangée Up Next prend le focus directement, puis la première rangée d'apps

#### Scenario: descente depuis le dock
- **WHEN** l'utilisateur presse bas depuis le dock
- **THEN** à la première entrée dans la zone grille, la rangée Up Next prend le focus, puis la première rangée d'apps

#### Scenario: descente sans rangée Up Next
- **WHEN** l'utilisateur descend depuis le dock alors que la rangée Up Next est masquée (réglage désactivé, vide ou permission refusée)
- **THEN** à la première entrée dans la zone grille, la première rangée d'apps prend le focus directement, « section-title-upnext » est absent et le haut de « section-title-apps » est le haut de la zone grille

#### Scenario: titres de section
- **WHEN** le test compose la zone grille avec la rangée Up Next, pour chaque position réglée, puis parcourt toutes les lignes au D-pad
- **THEN** « section-title-upnext » est au-dessus de « zone-upnext » et « section-title-apps » au-dessus de la première rangée d'apps, dans l'ordre de la position réglée ; aucun titre ne prend jamais le focus ; à chaque arrivée sur la première ligne d'une section, le titre de cette section est entièrement entre les marges de la grille ; à la première entrée, le haut du titre de la première section est sur la marge haute

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la première rangée d'apps
- **THEN** la rangée Up Next reprend le focus si elle est affichée en tête, puis le dock puis le héro ; si elle est masquée, le focus revient au dock puis au héro

#### Scenario: position après les applications
- **WHEN** le réglage « Position d'Up Next » vaut « Après les applications »
- **THEN** bas depuis le dock donne le focus à la première rangée d'apps, bas depuis la dernière rangée d'apps donne le focus à la rangée Up Next, bas depuis la rangée ne fait rien, haut depuis la rangée revient à la dernière rangée d'apps

#### Scenario: retour au héro
- **WHEN** l'utilisateur presse Retour depuis la zone grille
- **THEN** le héro reprend le focus

#### Scenario: retour dans la grille à la position laissée
- **WHEN** le test quitte la zone grille depuis une carte Up Next qui n'est pas la première, redescend, puis recommence depuis une tuile d'une rangée d'apps profonde
- **THEN** à chaque redescente, l'élément laissé (la carte Up Next, puis la tuile) a le focus, à la même position à l'écran qu'avant la sortie

#### Scenario: carte focusée disparue
- **WHEN** le test retire du flux de la rangée la carte qui a le focus, d'abord au milieu de la rangée, puis en dernière position
- **THEN** la carte qui prend sa position a le focus dans le premier cas, la précédente dans le second ; le focus reste dans « zone-upnext »

#### Scenario: restauration au retour d'une app
- **WHEN** le test simule l'ouverture d'une app depuis une carte Up Next puis le retour au launcher, avec la carte d'origine toujours présente, puis disparue
- **THEN** la position précédente dans la zone grille est restaurée ; le focus est sur la carte d'origine dans le premier cas, sur la carte qui a pris sa position (ou la précédente si elle était la dernière) dans le second

#### Scenario: rangée devenue vide
- **WHEN** le test simule le retour au launcher, ou un rechargement pendant que la rangée a le focus, avec une rangée Up Next devenue vide, pour chaque position réglée
- **THEN** la rangée et « section-title-upnext » sont absents et le focus est sur la première tuile de la première rangée d'apps (position « Avant les applications ») ou sur la dernière tuile de la dernière rangée d'apps (position « Après les applications ») ; sans aucune app, le héro a le focus

#### Scenario: états de la rangée
- **WHEN** la rangée est composée avec chaque état (chargement initial, rechargement, erreur, vide, permission refusée)
- **THEN** le rendu correspond : squelette au premier chargement seulement, état précédent conservé au rechargement, carte « Réessayer » focusable en erreur, rangée masquée si vide ou permission refusée

#### Scenario: menu Ouvrir avec
- **WHEN** l'appui long est simulé sur une carte Up Next
- **THEN** le menu « Ouvrir avec… » s'ouvre (testTag « upnext-menu »), y compris avec une seule source, OK ouvre l'app choisie, Retour ferme le menu et rend le focus à la carte

#### Scenario: réglage de position
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages, presse OK sur « setting-upnext-position », se déplace dans « setting-upnext-position-list » puis valide, et recommence en fermant la liste par Retour
- **THEN** OK ouvre la liste avec la valeur courante focusée et cochée ; haut et bas changent de valeur sans boucle, gauche et droite ne font rien ; OK sur l'autre valeur ferme la liste, rend le focus à « setting-upnext-position », persiste la valeur et le home recomposé place la rangée et son titre à la position choisie ; Retour liste ouverte ferme la liste sans changer la valeur ni quitter les réglages ; liste fermée, haut donne le focus à « setting-upnext-visible », gauche au volet des catégories sur « settings-category-HOME_SCREEN », Retour ferme les réglages ; le rectangle de chaque ligne est identique avec et sans le focus

#### Scenario: réglage de visibilité
- **WHEN** le test désactive « Afficher Up Next » (« setting-upnext-visible ») dans « Écran d'accueil » puis revient au home
- **THEN** la rangée et « section-title-upnext » sont masqués, la navigation les saute, le réglage est conservé après redémarrage, et la rangée réapparaît si le réglage est réactivé ; sur la ligne, bas donne le focus à « setting-upnext-position », haut et droite ne font rien, gauche rend le focus à « settings-category-HOME_SCREEN » et Retour ferme les réglages

#### Scenario: tests existants de la transition héro ↔ grille
- **WHEN** les tests de « Couverture de la transition héro ↔ grille » s'exécutent
- **THEN** ils composent l'accueil avec la rangée Up Next masquée (« Afficher Up Next » désactivé), de sorte que « première tuile de la grille » et « première rangée » y désignent la première rangée d'apps ; les cas où la rangée Up Next est affichée sont couverts par les scénarios de cette exigence
