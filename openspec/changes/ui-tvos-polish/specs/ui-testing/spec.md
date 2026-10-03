# Delta ui-testing

## MODIFIED Requirements

### Requirement: Couverture de la transition héro ↔ grille
La transition en défilement entre le héro et la grille SHALL être couverte par des tests Compose exécutés à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), par assertions sémantiques et de position, avec les key events physiques du D-pad.

#### Scenario: descente vers la grille
- **WHEN** le test presse bas depuis le dock (ou depuis le héro avec un dock vide) et laisse l'animation se terminer
- **THEN** la première tuile de la grille a le focus, la zone grille (« zone-grid ») est entièrement dans l'écran et les zones héro (« zone-hero ») et dock (« zone-dock ») sont entièrement hors écran (limites vérifiées par position, pas par alpha)

#### Scenario: dock jamais visible en vue grille
- **WHEN** le test avance l'horloge d'animation par étapes pendant la descente, puis pendant la remontée
- **THEN** à chaque étape le rectangle du dock ne recouvre jamais le rectangle de la grille visible : ils sont soit disjoints, soit le dock est hors écran

#### Scenario: engrenage solidaire du héro
- **WHEN** le test presse bas depuis le dock et laisse l'animation se terminer, puis presse haut depuis la première rangée
- **THEN** après la descente la capsule (« hero-capsule ») et l'engrenage (« settings-gear ») sont entièrement hors écran, au-dessus du viewport, avec le même décalage vertical que « zone-hero » ; après la remontée ils sont de nouveau à leur position initiale (« Capsule heure et réglages » de `settings`)

#### Scenario: remontée vers le dock
- **WHEN** le test presse haut depuis la première rangée de la grille
- **THEN** la page revient à sa position initiale, le dock a le focus (ou le héro sans dock) et la zone grille est de nouveau hors écran

#### Scenario: retour depuis une rangée profonde
- **WHEN** le test descend jusqu'à une rangée non visible initialement puis presse Retour
- **THEN** le héro reprend le focus et la page est à sa position initiale

#### Scenario: interruption de l'animation
- **WHEN** le test presse haut alors que la descente est encore en cours
- **THEN** le test se termine avec la page à la position du dock, sans exception ni focus perdu

#### Scenario: verre du dock pendant le défilement
- **WHEN** le test, avec le verre activé, descend du dock vers la grille puis remonte en avançant l'horloge d'animation par étapes
- **THEN** à chaque étape où une partie de « zone-hero » est à l'écran, le verre de « dock-glass » est actif (propriété sémantique `GlassActive`) ; une fois le héro entièrement hors écran il est inactif ; et la bascule du verre d'une `GlassSurface` ne recrée pas son contenu

#### Scenario: retour dans la grille à la position laissée
- **WHEN** le test parcourt la grille jusqu'à une position où la première rangée est hors écran, presse Retour, puis redescend vers la grille
- **THEN** la dernière tuile visitée a le focus, à la même position à l'écran qu'avant Retour, et la page y est arrivée par un mouvement monotone

#### Scenario: plancher de la grille
- **WHEN** le panneau Top Shelf se referme sur la première rangée de la grille
- **THEN** à chaque étape de l'animation « zone-hero » reste entièrement hors écran et la grille revient à sa position de départ

#### Scenario: pas de dock en vue grille sur les tests existants
- **WHEN** les tests existants de navigation (« Navigation D-pad des trois zones ») s'exécutent
- **THEN** ils passent inchangés dans leurs assertions de focus ; toute assertion fondée sur l'alpha des couches est remplacée par une assertion de position

## ADDED Requirements

### Requirement: Couverture du style tvOS
Le style tvOS de l'accueil, du menu contextuel et des réglages SHALL être couvert par des tests exécutés à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), par assertions sémantiques, de position et de couleur calculée, sans capture d'image, avec des testTags stables : « hero-capsule », « hero-clock », « settings-gear », « dock-glass », « hero-open », « hero-metadata », « hero-progress », « app-menu », préfixes « app-tile-<package> », « app-tile-name-<package> », « menu-action-<action> », « settings-category-<CATEGORY> », « source-row-<package> », « hidden-row-<package> », et « unhide-all ».

#### Scenario: tuile focusée sans débordement
- **WHEN** une tuile de la grille, puis une tuile du dock prend le focus et l'animation se termine
- **THEN** le rectangle agrandi de la tuile focusée ne recoupe celui d'aucune tuile voisine et reste entièrement dans l'écran, et dans « dock-glass » pour une tuile du dock

#### Scenario: nom sous la tuile focusée
- **WHEN** une tuile de la grille prend le focus, puis le perd
- **THEN** tant qu'elle a le focus, « app-tile-name-<package> » est affiché entre le bas de la tuile et le haut de la rangée suivante ; ensuite il n'existe plus ; la position des tuiles de la grille ne change à aucun moment

#### Scenario: taille fixe du dock
- **WHEN** le dock contient 1, 3 puis 6 apps
- **THEN** toutes les tuiles du dock ont la même largeur et la même hauteur dans les trois cas, plus petites que celles d'une tuile de la grille, et « dock-glass » est centré horizontalement

#### Scenario: capsule
- **WHEN** l'accueil affiche le héro
- **THEN** « hero-clock » et « settings-gear » sont dans « hero-capsule », à droite de l'écran ; haut depuis le héro donne le focus à « settings-gear », jamais à « hero-clock » ; gauche et droite depuis l'engrenage le laissent focusé ; bas le rend au héro

#### Scenario: heure mise à jour
- **WHEN** la source de l'heure émet une nouvelle valeur
- **THEN** « hero-clock » affiche la nouvelle valeur ; le format 12 ou 24 h suit le réglage du système (test JUnit de la source)

#### Scenario: pilules des réglages sans zoom
- **WHEN** une catégorie, une ligne d'« Apps sources », une ligne d'« Applications cachées » ou « Tout réactiver » prend le focus
- **THEN** son rectangle à l'écran est identique à celui qu'elle avait sans le focus

#### Scenario: héro sans progression
- **WHEN** le héro affiche un programme ouvrable avec progression, puis un programme ouvrable sans progression
- **THEN** l'écart vertical entre le bas de « hero-progress » et le haut de « hero-open » dans le premier cas est égal à l'écart entre le bas de « hero-metadata » et le haut de « hero-open » dans le second, et « hero-metadata » ne contient pas de ligne vide sous un titre d'une ligne

#### Scenario: menu contextuel lisible
- **WHEN** les couleurs du panneau du menu contextuel et de sa pilule de focus sont composées sur un arrière-plan blanc (pire cas, sans compter le voile)
- **THEN** le contraste du texte blanc sur le panneau et celui du texte sombre sur la pilule atteignent chacun au moins 4,5:1 ; et dans l'accueil, à l'ouverture du menu, la première action « menu-action-<action> » a le focus, bas passe à l'action suivante, placée sous la première

#### Scenario: écran au repos
- **WHEN** l'accueil est laissé sans touche, l'horloge de test avancée de plusieurs secondes, après chacune de ces situations : héro sans programme, dock focusé, grille focusée après la descente, grille atteinte depuis un héro dont le poster était affiché
- **THEN** aucun état Compose n'est modifié pendant cet intervalle (aucune animation ni recomposition en cours) ; dans le dernier cas, le Ken Burns du poster, actif sur le héro, est arrêté une fois le héro sorti de l'écran
