# ui-testing Specification

## Purpose
Couverture de test de l'écran home : assertions sémantiques sur JVM (Robolectric + Compose), navigation D-pad et états de chaque zone vérifiés par testTags, sans capture d'image ni environnement Android réel.

## Requirements

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
- **THEN** le panneau shelf est inséré dans la grille (présence vérifiée par testTag)

#### Scenario: shelf sans contenu
- **WHEN** l'app ne publie rien
- **THEN** aucun panneau n'est inséré

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
- **THEN** le focus passe au dock

#### Scenario: descente depuis le dock
- **WHEN** l'utilisateur presse bas depuis le dock
- **THEN** la grille prend le focus

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
- **THEN** après la descente l'engrenage (« settings-gear ») est entièrement hors écran, au-dessus du viewport, avec le même décalage vertical que « zone-hero » ; après la remontée il est de nouveau à sa position initiale (« Icône réglages flottante » de `settings`)

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

### Requirement: Couverture du volet Applications cachées
Le volet « Applications cachées » des réglages SHALL être couvert par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) avec des données déterministes, par testTags stables : « hidden-pane », « unhide-all », préfixes « hidden-row-<package> » et « hidden-switch-<package> », « hidden-empty ».

#### Scenario: liste dans le volet
- **WHEN** la catégorie « Applications cachées » est sélectionnée avec des apps cachées
- **THEN** sans aucune validation préalable (aucune touche OK), « unhide-all » et chaque ligne « hidden-row-<package> » sont affichés comme descendants de « hidden-pane », « unhide-all » précède les lignes à l'écran, et l'ordre des lignes suit le tri spécifié (datées de la plus récente à la plus ancienne, puis sans date par ordre alphabétique)

#### Scenario: focus initial sur la première ligne
- **WHEN** le test presse droite depuis la catégorie « Applications cachées » avec au moins une app cachée
- **THEN** la première ligne « hidden-row-<package> » a le focus, pas « unhide-all » ; haut depuis cette ligne donne le focus à « unhide-all »

#### Scenario: réactiver puis recacher
- **WHEN** le test presse OK sur une ligne, puis OK à nouveau
- **THEN** après le premier OK le callback de bascule est appelé avec le package de la ligne, l'app n'est plus cachée dans l'état persisté, la ligne est toujours présente, son switch est en position « visible » et elle garde le focus ; après le second OK le callback de bascule est appelé de nouveau, l'app est de nouveau cachée et le switch est en position « cachée »

#### Scenario: tout réactiver
- **WHEN** le test presse OK sur « unhide-all »
- **THEN** le callback global est appelé une fois, toutes les lignes restent rendues avec leur switch en position « visible » et le focus reste sur le bouton

#### Scenario: recalcul à la sortie de la catégorie
- **WHEN** le test réactive une ligne, presse gauche puis droite, puis presse gauche, bas vers une autre catégorie et haut pour revenir sur « Applications cachées »
- **THEN** après l'aller-retour gauche/droite la ligne réactivée est toujours rendue et le focus est sur la première ligne ; après le changement de catégorie et le retour elle n'est plus rendue, y compris à la première image du volet, et si c'était la seule, « hidden-empty » est rendu

#### Scenario: état vide
- **WHEN** la catégorie est sélectionnée sans app cachée
- **THEN** seul « hidden-empty » est affiché (ni « unhide-all » ni ligne), droite laisse le focus sur la catégorie et Retour appelle la fermeture des réglages

#### Scenario: ligne focalisée disparue
- **WHEN** le test désinstalle l'app de la ligne focalisée, puis les suivantes jusqu'à vider la liste
- **THEN** le focus passe à chaque fois à la ligne qui occupe la position de la ligne disparue, puis, liste vide, à « settings-category-HIDDEN » ; gauche l'y laisse et Retour appelle la fermeture des réglages ; une désinstallation pendant que « unhide-all » a le focus ne le déplace pas

#### Scenario: longue liste
- **WHEN** la liste compte plus de lignes que l'écran n'en montre et le test descend jusqu'à la dernière, puis remonte jusqu'à « unhide-all »
- **THEN** l'élément focalisé est à chaque fois affiché à l'écran (assertion d'affichage, pas seulement d'existence)

#### Scenario: bords et retour
- **WHEN** le test presse haut depuis « unhide-all », bas depuis la dernière ligne, gauche depuis une ligne, puis Retour
- **THEN** haut et bas aux bords ne déplacent pas le focus, gauche rend le focus à la catégorie « Applications cachées », Retour appelle la fermeture des réglages
