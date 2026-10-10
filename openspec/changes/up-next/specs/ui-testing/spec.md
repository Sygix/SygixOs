# Delta ui-testing

## MODIFIED Requirements

### Requirement: Sélecteurs stables
Les zones et tuiles de l'écran home SHALL exposer des testTags stables utilisés par les tests à la place de sélecteurs fragiles (texte, position).

#### Scenario: sélection par tag
- **WHEN** un test cible une zone (héro, dock, grille, panneau shelf, rangée Up Next), un titre de section de la zone grille, un menu contextuel ou une de ses entrées, une tuile d'app, une carte Up Next ou un de ses éléments (badge, barre de progression, placeholder), le squelette ou la carte d'erreur de la rangée, le volet ou un contrôle de la catégorie « Écran d'accueil » des réglages
- **THEN** il le sélectionne par testTag documenté (« zone-hero », « zone-dock », « zone-grid », « shelf-panel », « zone-upnext », « section-title-upnext », « section-title-apps », « app-menu », « upnext-menu », « upnext-skeleton », « upnext-error », « settings-home-screen », « setting-upnext-visible », « setting-upnext-position », « setting-upnext-position-list », préfixes « app-tile-<package> », « upnext-card-<key> », « upnext-card-badge-<key> », « upnext-card-progress-<key> », « upnext-card-placeholder-<key> », « upnext-menu-entry-<package> » et « setting-upnext-position-<VALUE> », la catégorie elle-même étant « settings-category-HOME_SCREEN »), sans dépendre du libellé affiché

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
- **THEN** le rendu correspond : squelette au premier chargement seulement, état précédent conservé au rechargement, carte d'erreur « upnext-error » focusable en erreur, sous « section-title-upnext » toujours affiché, rangée et « section-title-upnext » masqués si vide ou permission refusée

#### Scenario: structure des cartes
- **WHEN** le test compose la rangée Up Next à la taille TV avec au moins cinq items (un épisode en cours avec image, un film, un item sans image, un item sans icône d'app)
- **THEN** chaque « upnext-card-<key> » a une largeur égale à (largeur de contenu − 3 × `Dimens.GridSpacing`) / 4 et un ratio 16:9, les quatre premières sont entières à partir de la marge gauche et le rectangle de la cinquième dépasse le bord droit de l'écran ; « upnext-card-badge-<key> » est présent sauf sans icône, « upnext-card-progress-<key> » n'existe que pour l'item en cours, « upnext-card-placeholder-<key> » n'existe que pour l'item sans image ; l'épisode porte deux nœuds de texte (titre de série, « SxxEyy · titre d'épisode »), le film un seul, aucune carte ne contient de texte d'action ni de nom d'app ; après droite jusqu'à la cinquième carte, son rectangle est entier à l'écran ; aucune assertion de pixel

#### Scenario: squelette et carte d'erreur
- **WHEN** le test compose la rangée en chargement initial puis en erreur
- **THEN** « upnext-skeleton » contient cinq nœuds de la taille d'une carte, aucun n'est focusable ; en erreur, « upnext-error » est un nœud unique de la taille d'une carte, focusable, portant les textes « Impossible de charger » et « OK pour réessayer », sous « section-title-upnext »

#### Scenario: menu Ouvrir avec
- **WHEN** l'appui long est simulé sur une carte Up Next
- **THEN** le menu « Ouvrir avec… » s'ouvre (testTag « upnext-menu »), y compris avec une seule source, avec son en-tête (vignette, titre du contenu, sous-titre « Ouvrir avec… ») et une entrée « upnext-menu-entry-<package> » par source dans l'ordre des sources de l'item, la première ayant le focus ; OK ouvre l'app choisie, Retour ferme le menu et rend le focus à la carte

#### Scenario: réglage de position
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages, presse OK sur « setting-upnext-position », se déplace dans « setting-upnext-position-list » puis valide, et recommence en fermant la liste par Retour
- **THEN** « settings-home-screen » affiche le titre « Écran d'accueil », sa description, puis « setting-upnext-visible » au-dessus de « setting-upnext-position », cette ligne portant la valeur courante et un chevron ; OK ouvre la liste avec la valeur courante focusée et cochée, « setting-upnext-position-list » contient exactement « setting-upnext-position-<VALUE> » pour les deux valeurs dans l'ordre, la coche n'existe que sur la valeur courante, le rectangle de la liste est sous celui de la ligne et dans le volet droit ; haut et bas changent de valeur sans boucle, gauche et droite ne font rien ; OK sur l'autre valeur ferme la liste, rend le focus à « setting-upnext-position », persiste la valeur et le home recomposé place la rangée et son titre à la position choisie ; Retour liste ouverte ferme la liste sans changer la valeur ni quitter les réglages ; liste ouverte, un passage simulé du launcher en arrière-plan puis au premier plan (cycle de vie arrêté puis repris) la ferme sans changer la valeur et rend le focus à « setting-upnext-position », réglages toujours ouverts ; liste fermée, haut donne le focus à « setting-upnext-visible », gauche au volet des catégories sur « settings-category-HOME_SCREEN », Retour ferme les réglages ; le rectangle de chaque ligne est identique avec et sans le focus ; aucune assertion de pixel

#### Scenario: réglage de visibilité
- **WHEN** le test désactive « Afficher Up Next » (« setting-upnext-visible ») dans « Écran d'accueil » puis revient au home
- **THEN** la rangée et « section-title-upnext » sont masqués, la navigation les saute, le réglage est conservé après redémarrage, et la rangée réapparaît si le réglage est réactivé ; sur la ligne, bas donne le focus à « setting-upnext-position », qui reste focusable et modifiable (même rectangle et même liste que réglage activé, aucun état grisé), une valeur choisie à ce moment étant appliquée au home dès la réactivation ; haut et droite ne font rien, gauche rend le focus à « settings-category-HOME_SCREEN » et Retour ferme les réglages

#### Scenario: aucune app TV détectée
- **WHEN** le test compose l'accueil sans aucune app TV
- **THEN** « section-title-apps » est absent et le message « Aucune app TV détectée » est affiché, centré dans l'espace des rangées d'apps ; « section-title-upnext » et « zone-upnext » suivent leurs propres règles

#### Scenario: tests existants de la transition héro ↔ grille
- **WHEN** les tests de « Couverture de la transition héro ↔ grille » s'exécutent
- **THEN** ils composent l'accueil avec la rangée Up Next masquée (« Afficher Up Next » désactivé), de sorte que « première tuile de la grille » et « première rangée » y désignent la première rangée d'apps ; les cas où la rangée Up Next est affichée sont couverts par les scénarios de cette exigence
