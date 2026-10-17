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
- **WHEN** un test cible une zone (héro, dock, grille, panneau shelf, rangée Up Next), un titre de section de la zone grille, un menu contextuel ou une de ses entrées (dont « Retirer d'À suivre »), une tuile d'app, une carte Up Next ou un de ses éléments (badge, barre de progression, placeholder), le squelette ou la carte d'erreur de la rangée, le volet ou un contrôle de la catégorie « Écran d'accueil » des réglages
- **THEN** il le sélectionne par testTag documenté (« zone-hero », « zone-dock », « zone-grid », « shelf-panel », « zone-upnext », « section-title-upnext », « section-title-apps », « app-menu », « upnext-menu », « upnext-menu-remove », « upnext-skeleton », « upnext-error », « settings-home-screen », « setting-upnext-visible », « setting-upnext-position », « setting-upnext-position-list », préfixes « app-tile-<package> », « upnext-card-<key> », « upnext-card-badge-<key> », « upnext-card-progress-<key> », « upnext-card-placeholder-<key> », « upnext-menu-entry-<package> » et « setting-upnext-position-<VALUE> », la catégorie elle-même étant « settings-category-HOME_SCREEN »), sans dépendre du libellé affiché

### Requirement: Couverture dock
Le dock SHALL être couvert par des tests de rendu et de focus.

#### Scenario: dock avec apps épinglées
- **WHEN** le dock reçoit une liste d'apps épinglées
- **THEN** chaque app est rendue avec son libellé et la première tuile peut prendre le focus

#### Scenario: dock vide
- **WHEN** aucune app n'est épinglée
- **THEN** le message d'invitation à épingler est affiché

### Requirement: Couverture grille et shelf
La grille SHALL être couverte par des tests de rendu et d'insertion du panneau shelf, y compris avec une grille plus haute que le seuil de composition de « Préchargement et mémoire » (`launcher-shell`), à la taille d'une TV (`w960dp-h540dp-xhdpi`). Les assertions SHALL porter sur les nœuds réels (tuiles, panneau, zone grille), sans marqueur ni sélecteur ajouté pour les tests.

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

#### Scenario: rangées composées
- **WHEN** le catalogue compte au plus le seuil de rangées, puis une rangée de plus, 200 et 2 000 apps
- **THEN** toutes les tuiles sont composées sous le seuil ; au-dessus, la première tuile l'est, la dernière ne l'est pas et le nombre de tuiles composées reste sous la borne de la fenêtre et de sa prélecture

#### Scenario: rafale et restauration profonde
- **WHEN** le test enchaîne des touches Bas sans attendre la fin du défilement, inverse avec Haut, puis quitte la grille (Retour, menu contextuel) et y revient
- **THEN** la tuile visée a le focus à chaque étape, se retrouve entre les marges une fois le défilement terminé, la même tuile reprend le focus à la même place au retour, et la première rangée n'est plus composée

#### Scenario: déplacement et Up Next après les apps
- **WHEN** le test déplace une app sur dix rangées puis valide, la déplace encore puis annule, ou descend jusqu'à la rangée Up Next placée après une grande grille et revient
- **THEN** l'app déplacée garde le focus et reste entre les marges, l'ordre validé est conservé et l'ordre annulé restauré ; la carte Up Next prend le focus, est entre les marges et est restaurée au retour

#### Scenario: panneau et verre sur une grande grille
- **WHEN** le panneau s'ouvre sur une rangée profonde d'une grille au-dessus du seuil
- **THEN** le panneau est sous la marge haute, immédiatement au-dessus de la rangée focusée, qui reste entre les marges ; la première affiche a été demandée avant l'ouverture et une validation tardive n'entraîne aucune nouvelle demande de l'affiche courante ; la zone grille porte toujours la source du verre et couvre toutes les rangées

### Requirement: Couverture héro
Le héro SHALL être couvert par des tests de rendu du carrousel et de l'état fallback.

#### Scenario: carrousel avec programmes
- **WHEN** des HeroItem sont fournis
- **THEN** le premier programme est rendu (titre affiché, progression visible quand exposée)

#### Scenario: fallback sans contenu
- **WHEN** aucun HeroItem n'est fourni
- **THEN** l'écran ne crash pas et le fallback visuel est rendu

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
- **WHEN** le réglage « Position d'À suivre » vaut « Après les applications »
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
- **WHEN** le test simule l'ouverture d'une app depuis la quatrième carte Up Next puis le retour au launcher, avec le contenu ouvert toujours publié (même programme, puis un autre épisode de la même série sous une autre clé de carte), puis plus publié
- **THEN** la position verticale précédente dans la zone grille est restaurée ; dans les deux premiers cas, la carte du contenu ouvert est la première de « zone-upnext », elle a le focus et son rectangle commence à la marge gauche ; dans le dernier, le focus est sur la carte qui a pris la position de la carte ouverte (ou la précédente si elle était la dernière)

#### Scenario: rangée devenue vide
- **WHEN** le test simule le retour au launcher, ou un rechargement pendant que la rangée a le focus, avec une rangée Up Next devenue vide, pour chaque position réglée
- **THEN** la rangée et « section-title-upnext » sont absents et le focus est sur la première tuile de la première rangée d'apps (position « Avant les applications ») ou sur la dernière tuile de la dernière rangée d'apps (position « Après les applications ») ; sans aucune app, le héro a le focus

#### Scenario: états de la rangée
- **WHEN** la rangée est composée avec chaque état (chargement initial, rechargement, erreur, vide, permission refusée)
- **THEN** le rendu correspond : squelette au premier chargement seulement, état précédent conservé au rechargement, carte d'erreur « upnext-error » focusable en erreur, sous « section-title-upnext » toujours affiché, rangée et « section-title-upnext » masqués si vide ou permission refusée

#### Scenario: structure des cartes
- **WHEN** le test compose la rangée Up Next à la taille TV avec au moins cinq items (un épisode en cours avec image, un film, un item sans image, un item sans icône d'app)
- **THEN** chaque « upnext-card-<key> » a une largeur égale à (largeur de contenu − 3 × `Dimens.GridSpacing`) / 4 et un ratio 16:9, les quatre premières sont entières à partir de la marge gauche et le rectangle de la cinquième dépasse le bord droit de l'écran ; « upnext-card-badge-<key> » est présent sauf sans icône, « upnext-card-progress-<key> » n'existe que pour l'item en cours, « upnext-card-placeholder-<key> » n'existe que pour l'item sans image ; l'épisode porte deux nœuds de texte (titre de série, « SxxEyy · titre d'épisode »), le film un seul, aucune carte ne contient de texte d'action ni de nom d'app ; après droite jusqu'à la cinquième carte, son rectangle est entier à l'écran ; après droite jusqu'à la dernière carte d'une rangée de huit cartes, le bord droit du rectangle sans zoom de la dernière carte est à `Dimens.ScreenMarginH` du bord droit de l'écran, comme le bord gauche de la première carte au début de la rangée ; aucune assertion de pixel

#### Scenario: squelette et carte d'erreur
- **WHEN** le test compose la rangée en chargement initial puis en erreur
- **THEN** « upnext-skeleton » contient cinq nœuds de la taille d'une carte, aucun n'est focusable ; en erreur, « upnext-error » est un nœud unique de la taille d'une carte, focusable, portant les textes « Impossible de charger » et « OK pour réessayer », sous « section-title-upnext »

#### Scenario: menu Ouvrir avec
- **WHEN** l'appui long est simulé sur une carte Up Next
- **THEN** le menu « Ouvrir avec… » s'ouvre (testTag « upnext-menu »), y compris avec une seule source, avec son en-tête (vignette, titre du contenu, sous-titre « Ouvrir avec… »), une entrée « upnext-menu-entry-<package> » par source dans l'ordre des sources de l'item, la première ayant le focus, puis « upnext-menu-remove » juste en dessous de la dernière entrée ; bas depuis la dernière entrée d'app donne le focus à « upnext-menu-remove », bas depuis lui ne fait rien ; OK ouvre l'app choisie, Retour ferme le menu et rend le focus à la carte ; OK sur « upnext-menu-remove » ferme le menu, retire la carte de « zone-upnext » (et toute carte de la même série publiée par une autre app), persiste le retrait et donne le focus à la carte suivante, ou à la précédente si c'était la dernière

#### Scenario: réglage de position
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages, presse OK sur « setting-upnext-position », se déplace dans « setting-upnext-position-list » puis valide, et recommence en fermant la liste par Retour
- **THEN** « settings-home-screen » affiche le titre « Écran d'accueil », sa description, puis « setting-upnext-visible » au-dessus de « setting-upnext-position », cette ligne portant la valeur courante et un chevron ; OK ouvre la liste avec la valeur courante focusée et cochée, « setting-upnext-position-list » contient exactement « setting-upnext-position-<VALUE> » pour les deux valeurs dans l'ordre, la coche n'existe que sur la valeur courante, le rectangle de la liste est sous celui de la ligne et dans le volet droit ; haut et bas changent de valeur sans boucle, gauche et droite ne font rien ; OK sur l'autre valeur ferme la liste, rend le focus à « setting-upnext-position », persiste la valeur et le home recomposé place la rangée et son titre à la position choisie ; Retour liste ouverte ferme la liste sans changer la valeur ni quitter les réglages ; liste ouverte, un passage simulé du launcher en arrière-plan puis au premier plan (cycle de vie arrêté puis repris) la ferme sans changer la valeur et rend le focus à « setting-upnext-position », réglages toujours ouverts ; liste fermée, haut donne le focus à « setting-upnext-visible », gauche au volet des catégories sur « settings-category-HOME_SCREEN », Retour ferme les réglages ; le rectangle de chaque ligne est identique avec et sans le focus ; aucune assertion de pixel

#### Scenario: réglage de visibilité
- **WHEN** le test désactive « Afficher À suivre » (« setting-upnext-visible ») dans « Écran d'accueil » puis revient au home
- **THEN** la rangée et « section-title-upnext » sont masqués, la navigation les saute, le réglage est conservé après redémarrage, et la rangée réapparaît si le réglage est réactivé ; sur la ligne, bas donne le focus à « setting-upnext-position », qui reste focusable et modifiable (même rectangle et même liste que réglage activé, aucun état grisé), une valeur choisie à ce moment étant appliquée au home dès la réactivation ; haut et droite ne font rien, gauche rend le focus à « settings-category-HOME_SCREEN » et Retour ferme les réglages

#### Scenario: aucune app TV détectée
- **WHEN** le test compose l'accueil sans aucune app TV
- **THEN** « section-title-apps » est absent et le message « Aucune app TV détectée » est affiché, centré dans l'espace des rangées d'apps ; « section-title-upnext » et « zone-upnext » suivent leurs propres règles

#### Scenario: tests existants de la transition héro ↔ grille
- **WHEN** les tests de « Couverture de la transition héro ↔ grille » s'exécutent
- **THEN** ils composent l'accueil avec la rangée Up Next masquée (« Afficher À suivre » désactivé), de sorte que « première tuile de la grille » et « première rangée » y désignent la première rangée d'apps ; les cas où la rangée Up Next est affichée sont couverts par les scénarios de cette exigence

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

### Requirement: Couverture du volet Applications cachées
Le volet « Applications cachées » des réglages SHALL être couvert par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) avec des données déterministes, par testTags stables : « hidden-pane », « unhide-all », préfixes « hidden-row-<package> » et « hidden-switch-<package> », « hidden-empty », « hidden-group-upnext » (intertitre « Retirés d'À suivre »), préfixes « hidden-content-row-<clé> » et « hidden-content-action-<clé> » (clé de contenu sérialisée), « hidden-show-more-apps » et « hidden-show-more-upnext ».

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
- **WHEN** la catégorie est sélectionnée sans app cachée ni contenu retiré d'À suivre
- **THEN** seul « hidden-empty » est affiché (ni « unhide-all », ni ligne, ni « hidden-group-upnext »), droite laisse le focus sur la catégorie et Retour appelle la fermeture des réglages

#### Scenario: ligne focalisée disparue
- **WHEN** le test désinstalle l'app de la ligne focalisée, puis les suivantes jusqu'à vider la liste
- **THEN** le focus passe à chaque fois à la ligne qui occupe la position de la ligne disparue, puis, liste vide, à « settings-category-HIDDEN » ; gauche l'y laisse et Retour appelle la fermeture des réglages ; une désinstallation pendant que « unhide-all » a le focus ne le déplace pas

#### Scenario: longue liste
- **WHEN** la liste compte plus de lignes que l'écran n'en montre et le test descend jusqu'à la dernière, puis remonte jusqu'à « unhide-all »
- **THEN** l'élément focalisé est à chaque fois affiché à l'écran (assertion d'affichage, pas seulement d'existence)

#### Scenario: bords et retour
- **WHEN** le test presse haut depuis « unhide-all », bas depuis la dernière ligne, gauche depuis une ligne, puis Retour
- **THEN** haut et bas aux bords ne déplacent pas le focus, gauche rend le focus à la catégorie « Applications cachées », Retour appelle la fermeture des réglages

#### Scenario: groupe des contenus retirés
- **WHEN** la catégorie est sélectionnée avec deux apps cachées et deux contenus retirés à des instants différents
- **THEN** « hidden-group-upnext » est sous la dernière ligne « hidden-row-<package> » et au-dessus des deux « hidden-content-row-<clé> », dans l'ordre du retrait le plus récent au plus ancien ; « hidden-group-upnext » ne prend jamais le focus ; « hidden-content-action-<clé> » n'existe que pour la ligne focusée ; bas depuis la dernière app donne le focus à la première ligne de contenu

#### Scenario: réafficher un contenu
- **WHEN** le test presse OK sur une ligne « hidden-content-row-<clé> »
- **THEN** le callback de réaffichage est appelé avec la clé, le contenu n'est plus dans l'ensemble persisté, la ligne n'est plus rendue et le focus est sur l'élément qui occupe sa position ; quand c'était la dernière ligne du volet, le focus va à l'élément précédent, et quand le volet devient vide, « hidden-empty » est rendu et « settings-category-HIDDEN » a le focus

#### Scenario: limite de 3 lignes et dépliage
- **WHEN** la catégorie est sélectionnée avec cinq apps cachées et quatre contenus retirés, puis le test presse OK sur « hidden-show-more-apps », change de catégorie et revient
- **THEN** trois « hidden-row-<package> » sont rendues, suivies de « hidden-show-more-apps » portant « Afficher les 2 autres », puis trois « hidden-content-row-<clé> » suivies de « hidden-show-more-upnext » portant « Afficher l'autre » ; après OK, les cinq lignes d'apps sont rendues, « hidden-show-more-apps » n'existe plus et la quatrième ligne d'app a le focus ; au retour dans la catégorie, le groupe des apps est de nouveau replié ; OK sur « unhide-all » appelle le callback global pour les cinq apps

#### Scenario: contenus retirés seuls
- **WHEN** la catégorie est sélectionnée sans app cachée mais avec un contenu retiré
- **THEN** ni « unhide-all » ni « hidden-row-<package> » ne sont rendus, « hidden-group-upnext » et la ligne de contenu le sont, et droite depuis la catégorie donne le focus à cette ligne

### Requirement: Couverture du style tvOS
Le style tvOS de l'accueil, du menu contextuel et des réglages SHALL être couvert par des tests exécutés à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), par assertions sémantiques, de position et de couleur calculée, sans capture d'image, avec des testTags stables : « hero-capsule », « hero-clock », « settings-gear », « dock-glass », « hero-open », « hero-metadata », « hero-progress », « hero-poster », « app-menu », « menu-glass », « menu-thumbnail », « menu-dock-full », « move-banner », « hero-header », « hero-header-label », « hero-source-icon », « hero-details », « hero-progress-bar », « hero-remaining », préfixes « app-banner-<package> », « app-icon-<package> », « app-tile-<package> », « app-tile-art-<package> » (visuel de la tuile, transformations du focus comprises), « app-thumbnail-<package> », « menu-action-<action> », « settings-category-<CATEGORY> », « source-row-<package> », « hidden-row-<package> », et « unhide-all ».

#### Scenario: tuile focusée sans débordement
- **WHEN** une tuile de la grille, puis une tuile du dock prend le focus et l'animation se termine
- **THEN** le rectangle agrandi de la tuile focusée ne recoupe celui d'aucune tuile voisine et reste entièrement dans l'écran, et dans « dock-glass » pour une tuile du dock

#### Scenario: aucun nom sur les tuiles
- **WHEN** une tuile de la grille a le focus
- **THEN** aucun nœud de l'accueil n'affiche le nom d'une app de la grille

#### Scenario: tuiles des bords entières
- **WHEN** le focus passe sur les tuiles des quatre coins de la grille, puis sur la première et la dernière tuile d'un dock de 6 apps
- **THEN** à chaque fois le rectangle visible de la tuile agrandie (découpé par ses conteneurs) est égal à son rectangle complet, entièrement dans l'écran, et dans « dock-glass » pour une tuile du dock

#### Scenario: taille fixe du dock
- **WHEN** le dock contient 1, 3 puis 6 apps
- **THEN** toutes les tuiles du dock ont la même largeur et la même hauteur dans les trois cas, plus petites que celles d'une tuile de la grille, et « dock-glass » est centré horizontalement

#### Scenario: dock plein
- **WHEN** le menu contextuel d'une app de la grille est ouvert alors que le dock contient 6 apps, puis 5
- **THEN** avec 6 apps, « menu-action-pin » est focalisé mais désactivé, « menu-dock-full » est affiché et OK n'appelle pas l'épinglage ; avec 5, l'action est active, sans message, et OK épingle ; la limite et la conservation des épinglages enregistrés sont vérifiées par des tests JUnit du domaine

#### Scenario: capsule
- **WHEN** l'accueil affiche le héro
- **THEN** « hero-clock » et « settings-gear » sont dans « hero-capsule », à droite de l'écran ; haut depuis le héro donne le focus à « settings-gear », jamais à « hero-clock » ; gauche et droite depuis l'engrenage le laissent focusé ; bas le rend au héro ; Retour, avec un programme ouvrable, donne le focus à « hero-open »

#### Scenario: heure mise à jour
- **WHEN** la source de l'heure émet une nouvelle valeur
- **THEN** « hero-clock » affiche la nouvelle valeur, et la recomposition qui suit ne lit que cet état et ne touche que quelques scopes (observateur de composition), le reste de l'accueil n'étant pas recomposé ; le format 12 ou 24 h suit le réglage du système (test JUnit de la source)

#### Scenario: pilules des réglages sans zoom
- **WHEN** une catégorie, une ligne d'« Apps sources », une ligne d'« Applications cachées » ou « Tout réactiver » prend le focus
- **THEN** son rectangle à l'écran est identique à celui qu'elle avait sans le focus

#### Scenario: héro sans progression
- **WHEN** le héro affiche un programme ouvrable avec progression, puis un programme ouvrable sans progression
- **THEN** l'écart vertical entre le bas de « hero-progress » et le haut de « hero-open » dans le premier cas est égal à l'écart entre le bas de « hero-metadata » et le haut de « hero-open » dans le second, et « hero-metadata » ne contient pas de ligne vide sous un titre d'une ligne

#### Scenario: menu contextuel lisible
- **WHEN** les couleurs du panneau du menu contextuel et de sa pilule de focus sont composées sur un arrière-plan blanc (pire cas, sans compter le voile)
- **THEN** le contraste du texte blanc sur le panneau et celui du texte sombre sur la pilule atteignent chacun au moins 4,5:1 ; et dans l'accueil, à l'ouverture du menu, la première action « menu-action-<action> » a le focus, bas passe à l'action suivante, placée sous la première ; le menu n'a que les actions épingler, déplacer (grille) et cacher, sans « Fermer »

#### Scenario: écran au repos
- **WHEN** l'accueil est laissé sans touche, l'horloge de test avancée de plusieurs secondes, après chacune de ces situations : héro sur le dégradé animé du repli, dock focusé sur ce héro, grille focusée après la descente, grille atteinte depuis un héro dont le poster était affiché, poster du héro après la fin de son Ken Burns, panneau Top Shelf à une affiche après la fin de son Ken Burns, dégradé du repli après son passage, héro à deux programmes entre la fin du Ken Burns (10 s) et le changement de visuel (12 s)
- **THEN** dans les deux premières, aucune recomposition n'a lieu (le dégradé ne modifie que le dessin) ; dans les autres, aucun état Compose n'est modifié : le Ken Burns et le dégradé s'arrêtent à la sortie du héro et après leur unique passage

#### Scenario: reprise du Ken Burns
- **WHEN** le visuel du héro, puis l'affiche du panneau Top Shelf, change après la fin du passage du visuel précédent
- **THEN** le Ken Burns repart sur le nouveau visuel (des états Compose sont de nouveau modifiés)

#### Scenario: vignette des réglages
- **WHEN** la vignette d'une ligne d'app des réglages est composée pour une app avec bannière TV, puis pour une app sans bannière
- **THEN** la première montre « app-banner-<package> » sans icône, la seconde « app-icon-<package> » sans bannière

#### Scenario: coins concentriques
- **WHEN** les tests JUnit des jetons du design system s'exécutent
- **THEN** pour chaque couple imbriqué déclaré (dock et tuile, capsule et engrenage, menu et pilule, menu et vignette, ligne des réglages et vignette), rayon extérieur = rayon intérieur + marge, et les rayons valent ceux de « Thème » ; le coin du fond du code QR est égal à sa marge blanche et aucun module ne tombe dans un coin arrondi

#### Scenario: marges réelles
- **WHEN** le dock, la capsule, le menu contextuel et une ligne d'app des réglages sont composés à la taille d'une TV
- **THEN** l'écart mesuré entre le bord de la surface extérieure et celui de la surface intérieure (« dock-glass » et « app-tile-<package> », « hero-capsule » et « settings-gear », « menu-glass » et « menu-action-<action> » ou « menu-thumbnail », « hidden-row-<package> » et « app-thumbnail-<package> ») est la marge déclarée du couple, et la capsule mesure 36 dp de haut

#### Scenario: navigation dans la grille
- **WHEN** dans une grille de 25 apps, le focus passe d'une tuile à sa voisine de droite, puis à la rangée suivante, ou qu'un visuel d'une autre app devient vérifié (chaque recomposition d'une tuile est comptée par une source de bannières de test)
- **THEN** seules des tuiles des rangées qui contiennent la tuile qui perd ou prend le focus sont recomposées, et aucune tuile ne l'est quand un visuel devient vérifié

#### Scenario: focus qui passe
- **WHEN** le focus traverse trois tuiles en moins de 0,5 s chacune, puis reste sur la dernière
- **THEN** la préparation du panneau Top Shelf n'est demandée que pour la dernière tuile, 0,5 s après que le focus s'y est posé

### Requirement: Couverture de l'écran de démarrage
L'écran de démarrage (« Écran de démarrage » de `launcher-shell`) SHALL être couvert par des tests Compose exécutés à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), avec une horloge de test, un état de l'accueil (catalogue, premier visuel du héro) contrôlé par le test et une animation de mascotte remplacée par une source déterministe, par assertions sémantiques et testTags stables : « startup-splash » pour l'écran, « startup-splash-mascot » pour la mascotte. Le contrat du fichier de l'animation et l'enchaînement avec l'écran de lancement du système SHALL être couverts par des tests sans capture d'image. La touche Retour pressée pendant le fondu de sortie (« Écran de démarrage », scénario « Retour pendant le fondu de sortie ») SHALL être couverte par un test qui échoue si l'accueil l'intercepte.

#### Scenario: durée minimale
- **WHEN** le catalogue et le premier visuel du héro sont prêts 100 ms après la première image qui montre la mascotte
- **THEN** « startup-splash » est encore affiché 590 ms après cette image et aucun élément de l'accueil n'a le focus ; après 600 ms et la durée du fondu, « startup-splash » n'existe plus, le héro est affiché et détient le focus

#### Scenario: attente du premier visuel
- **WHEN** le catalogue est prêt à 200 ms et le premier visuel du héro à 1,5 s
- **THEN** « startup-splash » reste affiché jusqu'à 1,5 s, puis disparaît après la durée du fondu

#### Scenario: plafond du visuel
- **WHEN** le catalogue est prêt à 200 ms et le premier visuel du héro ne l'est jamais
- **THEN** le fondu commence à 2 s et le héro affiche son état de repli

#### Scenario: plafond global
- **WHEN** le catalogue n'est jamais chargé
- **THEN** le fondu commence à 5 s et révèle le fond noir uni de l'accueil en chargement, sans crash

#### Scenario: accueil transparent avant le fondu
- **WHEN** l'accueil est prêt mais que la durée minimale n'est pas atteinte
- **THEN** l'accueil est composé avec une opacité nulle et aucun de ses éléments n'est focusable

#### Scenario: focus au début du fondu
- **WHEN** le fondu vient de commencer
- **THEN** le héro détient déjà le focus et un appui sur haut porte le focus sur l'élément situé au-dessus du héro, comme sans écran de démarrage

#### Scenario: touches avant le fondu
- **WHEN** le test envoie bas, droite et OK avant le début du fondu
- **THEN** aucun élément de l'accueil ne prend le focus ni ne réagit et, après le fondu, le héro détient le focus

#### Scenario: retour au premier plan
- **WHEN** l'activité repasse en arrière-plan puis au premier plan dans le même processus
- **THEN** « startup-splash » n'est jamais affiché

#### Scenario: accueil recréé
- **WHEN** l'accueil est recréé dans un processus où il avait déjà été créé, avant le chargement du catalogue
- **THEN** « startup-splash » n'est pas affiché ; un fond noir uni est affiché sans « startup-splash-mascot »

#### Scenario: animations désactivées
- **WHEN** la source du réglage système indique que les animations sont désactivées
- **THEN** la mascotte affichée est l'image fixe et n'est pas animée ; l'accueil remplace « startup-splash » sans fondu dès que les règles de durée le permettent

#### Scenario: animation illisible
- **WHEN** la source de l'animation renvoie une erreur
- **THEN** « startup-splash » est affiché sans « startup-splash-mascot », puis l'accueil s'affiche selon les mêmes règles de durée, sans crash

#### Scenario: contrat du fichier
- **WHEN** la suite de tests s'exécute
- **THEN** un test lit le fichier de l'animation livré dans l'application et vérifie chaque contrainte de « Fichier de l'animation de la mascotte » (`launcher-shell`)

#### Scenario: écran de lancement du système
- **WHEN** la suite de tests s'exécute
- **THEN** un test vérifie dans le thème de l'application que le fond de l'écran de lancement du système est le noir du fond de l'écran de démarrage et que son icône ne dessine aucun pixel opaque

#### Scenario: mascotte tardive
- **WHEN** la source de l'animation ne répond qu'après 800 ms, l'accueil étant prêt
- **THEN** la durée minimale de 600 ms compte depuis la première image qui montre la mascotte, dont l'animation est démarrée

#### Scenario: composition différée de l'accueil
- **WHEN** le catalogue est prêt avant la fin du fondu d'entrée de la mascotte, ou alors que la fenêtre n'est pas active
- **THEN** « zone-hero » n'existe pas avant la fin de ce fondu et l'activation de la fenêtre, puis existe aussitôt après ; si la fenêtre reste sans focus (demande de permission), « zone-hero » existe 400 ms après la fin du fondu et le héro a le focus après le fondu de sortie

#### Scenario: échelle d'animation de l'app forcée à 0
- **WHEN** l'échelle d'animation de l'app est forcée à 0 alors que l'échelle de durée des animations du système n'est pas réglée
- **THEN** la source du réglage système indique que les animations sont actives ; une échelle du système à 0 les indique désactivées

#### Scenario: accueil caché sous l'écran de démarrage
- **WHEN** l'accueil est prêt mais que la durée minimale n'est pas atteinte
- **THEN** l'accueil est composé et aucun de ses éléments n'est focusable ; un test de pixels en rendu natif (`@GraphicsMode(NATIVE)`), avec un accueil d'une couleur unie, montre que l'écran reste entièrement noir (écran de démarrage opaque au-dessus de l'accueil) jusqu'au début du fondu, puis que la couleur de l'accueil apparaît progressivement pendant le fondu et occupe tout l'écran à sa fin

#### Scenario: accueil sans animation sous l'écran de démarrage
- **WHEN** l'accueil est composé sous l'écran de démarrage avec un visuel de programme, une vidéo d'aperçu, puis une grille
- **THEN** le visuel du héro est entièrement opaque dès qu'il est chargé, sans fondu ; la vidéo d'aperçu est préparée mais pas lue, et sa lecture commence quand l'accueil devient interactif ; la grille (« home-grid ») n'existe qu'après la fin du fondu de sortie ; la vérification des visuels du héro s'arrête après le premier visuel utilisable et ne reprend qu'à la fin de l'écran de démarrage (test JUnit du ViewModel)

#### Scenario: retour pendant le fondu de sortie
- **WHEN** le test envoie Retour pendant le fondu de sortie, alors que le héro détient le focus et que l'accueil traite les touches du D-pad
- **THEN** l'app quitte le premier plan (l'activité n'est plus au premier plan) sans crash, et aucune action de l'accueil n'a été déclenchée : le héro n'a pas ouvert de programme, la page n'a pas défilé ; le même test sans Retour laisse l'accueil au premier plan, ce qui prouve que la sortie vient de la touche

#### Scenario: retour sans fondu (animations désactivées)
- **WHEN** les animations sont désactivées et le test envoie Retour alors que l'écran de démarrage est encore affiché, puis l'envoie après son remplacement par l'accueil
- **THEN** pendant l'écran de démarrage, l'app quitte le premier plan sans action déclenchée sur l'accueil ; après le remplacement, Retour suit « Navigation 3 paliers » (le héro reprend le focus, l'app reste au premier plan)

### Requirement: Couverture des mises à jour
Les lignes de mise à jour d'« À propos », le code QR des notes de version et la pastille (`self-update`, « Pastille » de `launcher-shell`) SHALL être couverts par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), avec un état de mise à jour déterministe fourni par le test, les key events physiques du D-pad et des testTags stables : « update-check », « update-install », « update-withdrawn », « update-release-notes-qr », « update-prereleases », « update-prereleases-switch », « update-badge-gear », « update-badge-about ». La logique et le transport sont couverts par « Couverture de test des mises à jour » de `self-update`.

#### Scenario: navigation dans À propos
- **WHEN** le test ouvre « À propos » et presse droite, puis bas et haut
- **THEN** « update-check » prend le focus en premier ; bas parcourt « update-install » (si présente), « update-prereleases » puis les licences dans l'ordre affiché ; haut depuis « update-check » et bas depuis la dernière licence ne bougent pas le focus ; gauche rend le focus à la catégorie « À propos »

#### Scenario: ligne Mettre à jour
- **WHEN** l'état passe de « à jour » à « version disponible » puis revient à « à jour » alors que « update-install » a le focus
- **THEN** « update-install » apparaît juste sous « update-check » sans voler le focus, avec le libellé « Mettre à jour vers X » ; à sa disparition le focus passe à « update-check »

#### Scenario: code QR des notes de version
- **WHEN** une version est proposée avec une URL de release HTTPS de github.com, puis avec une URL invalide
- **THEN** « update-release-notes-qr » est affiché à droite, aligné en haut sur « update-install », sans chevaucher aucune ligne ni changer la hauteur ou la position de « update-prereleases », même avec une erreur affichée, et n'est jamais focusé dans le premier cas ; il est absent dans le second

#### Scenario: version retirée
- **WHEN** le test fournit une version retirée, seule puis avec une autre version proposée
- **THEN** « update-withdrawn » affiche « Cette version n'est plus disponible », sans code QR ni pastille, OK y est sans effet ; avec une autre version, « update-install » est aussi affichée, au-dessus

#### Scenario: états affichés
- **WHEN** le test fournit successivement les états jamais vérifié, en cours, à jour, version disponible, erreur sans version connue, erreur avec version connue, téléchargement à 42 %
- **THEN** les textes de « update-check » et « update-install » sont ceux de « Vérification des mises à jour » et « Téléchargement vérifié » (`self-update`)

#### Scenario: préversions
- **WHEN** le test bascule « update-prereleases » avec des versions connues contenant une préversion plus récente
- **THEN** l'état de « update-prereleases-switch », la version annoncée et la présence de « update-badge-gear » et « update-badge-about » changent aussitôt, sans appel au transport factice

#### Scenario: pastille sans effet sur le focus
- **WHEN** une version est proposée et le test parcourt héro, engrenage et retour au héro
- **THEN** « update-badge-gear » est affichée, n'est jamais focusée, et la position et la taille de la capsule et de l'engrenage sont identiques à celles d'un test sans version proposée

### Requirement: Couverture du rendu du héro et du verre
Le rendu du héro et du verre (« Thème », « Diaporama héro » et « Préchargement et mémoire » de `launcher-shell`) SHALL être couvert par des tests JVM : calcul des tailles de décodage, transformations du visuel et de l'arrière-plan du verre et rendu du verre sous Robolectric en rendu natif (`@GraphicsMode(NATIVE)`), changement de programme par test Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) avec un chargeur d'images de test. La fluidité SHALL être mesurée sur la TV réelle (`dumpsys gfxinfo` sur l'APK de release ou sur une build minifiée équivalente).

#### Scenario: tailles de décodage
- **WHEN** la fenêtre ou une bannière mesure 3840 × 2160
- **THEN** le visuel du héro est demandé en 1920 × 1080 et la bannière à la taille de la tuile au zoom de focus (332 × 187 sur une TV 1080p), plafonnée à 480 × 270 sur une dalle plus dense, sans agrandir une image plus petite

#### Scenario: visuel voilé
- **WHEN** un visuel blanc de 3840 × 2160 passe par la transformation du héro
- **THEN** l'image produite mesure 1920 × 1080, est opaque, reste blanche hors des voiles (coin haut droit compris, son voile étant dessiné à part) et est assombrie en bas et à gauche

#### Scenario: arrière-plan du verre
- **WHEN** un visuel moitié noir, moitié blanc passe par la transformation du verre
- **THEN** l'image produite mesure le huitième du visuel, est opaque, a une transition adoucie entre les deux moitiés et est assombrie en bas

#### Scenario: alignement du verre
- **WHEN** la copie floutée sous le dock est à motif, zoomée à 1,08 et à mi-fondu sur une autre copie
- **THEN** chaque point du verre montre la couleur du point du visuel situé dessous, avec ce zoom et ce fondu

#### Scenario: verre sans copie floutée
- **WHEN** un visuel est chargé sans copie floutée
- **THEN** la surface verre utilise le flou en direct, puis la copie dès qu'elle existe

#### Scenario: voile du coin pendant le Ken Burns
- **WHEN** un visuel blanc zoome jusqu'à 1,08
- **THEN** l'écran en haut à droite est voilé et ne s'éclaircit pas entre le début et la fin du zoom

#### Scenario: retour pendant le fondu
- **WHEN** l'utilisateur revient au programme précédent à mi-fondu
- **THEN** le visuel suivant s'efface à partir de son opacité courante et le précédent reste entièrement visible dessous

#### Scenario: couleur du verre
- **WHEN** le dock est rendu sur un arrière-plan gris moyen puis sur un arrière-plan sombre
- **THEN** sa couleur est celle de l'arrière-plan recouvert de la teinte de la maquette, à 3 niveaux près, et reste sombre sur un arrière-plan sombre

#### Scenario: changement de programme
- **WHEN** le héro passe d'un programme au titre court à un programme au titre long
- **THEN** pendant tout le fondu, le bas de chaque bloc « hero-metadata » et la position de « hero-open » ne changent pas ; à la fin, seul le nouveau bloc reste

### Requirement: Couverture des infos du héro
Les infos du programme dans le héro (« Diaporama héro » de `launcher-shell`) SHALL être couvertes par des tests JUnit (choix du libellé de l'en-tête, éléments de la ligne d'infos, arrondis de la durée et du temps restant, lecture des colonnes du TV Provider) et par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) avec une source d'icônes de test, sans capture d'image et avec des titres fictifs uniquement.

#### Scenario: types de programme
- **WHEN** le héro affiche un programme `CONTINUE`, `NEXT`, `NEW`, `WATCHLIST`, un preview program et un watch next sans type
- **THEN** « hero-header-label » vaut respectivement « Continuer dans X », « Épisode suivant dans X », « Nouveau dans X », « À regarder dans X », « X » et « X »

#### Scenario: données manquantes
- **WHEN** le nom de l'app, son icône, la saison, l'épisode, la durée ou la position manquent
- **THEN** seuls les éléments disponibles existent (« hero-source-icon », « hero-header-label », « hero-details », « hero-progress-bar », « hero-remaining »), « hero-header » n'existe pas sans icône ni libellé, et sans ligne d'infos le bas du titre est le bas de « hero-metadata »

#### Scenario: textes avec le visuel
- **WHEN** le visuel du programme suivant est retenu par un chargeur d'images de test, puis libéré
- **THEN** tant qu'il est retenu, le titre et la cible de « hero-open » restent ceux du programme affiché ; après 1 s, le héro passe au programme suivant dont le visuel est prêt, sans nouvelle requête du visuel lent tant qu'il est en cours ; une fois libéré, son programme revient dans la rotation ; un programme sans visuel montre ses textes aussitôt

#### Scenario: vidéo d'aperçu lente
- **WHEN** une vidéo d'aperçu d'un lecteur de test ne rend pas sa première image dans la seconde qui suit son changement
- **THEN** le héro passe au programme suivant sans afficher les textes de la vidéo ; la vidéo reste préparée, en pause, sans être préparée une seconde fois ; quand elle est prête et qu'on y revient, elle est lue avec ses textes

#### Scenario: héro immobile
- **WHEN** un programme avec en-tête, ligne d'infos et temps restant est affiché pendant le Ken Burns
- **THEN** des états Compose sont modifiés (zoom) mais aucune recomposition n'a lieu (`IdleFrameTest`)

### Requirement: Couverture du parcours de l'écran d'accueil
Le parcours de l'écran d'accueil, la page « Par autorisations », le panneau « Launcher principal » et les réglages de secours (`launcher-shell`) SHALL être couverts par des tests déterministes, sans environnement Android réel : le rôle HOME, l'écran d'accueil effectif, l'état du service d'accessibilité (activé dans les réglages du système, connecté ou non), la fin du démarrage, le numéro de démarrage, le temps écoulé depuis le démarrage, la permission de superposition, l'écran d'auto-démarrage du constructeur, l'ouverture des écrans système et l'accès par le débogage SHALL être fournis par des doubles contrôlés. La logique (écrans affichés et enchaînements 8.1 à 8.17, états des deux voies, synthèse de la ligne des réglages, parcours affiché automatiquement, démarrage observé, filtre de la touche Home, ouverture unique au démarrage, attente de connexion du service) SHALL être testée en JUnit ; la navigation et l'affichage, par des tests Compose à la taille d'une TV (`w960dp-h540dp-xhdpi`) avec les testTags de « Sélecteurs de l'écran d'accueil ». Les tests du service d'accessibilité SHALL distinguer le service activé et connecté, activé mais non connecté, et non activé. Aucun test ne SHALL présenter un état « Fait » ou « Active » que le double n'a pas confirmé.

#### Scenario: premier lancement et plus tard
- **WHEN** le test lance SygixOs sans parcours enregistré comme fermé, simule la réponse à la demande de permission « Programmes TV » (le parcours n'apparaît qu'après), puis presse Retour sur le panneau 8.1
- **THEN** le parcours est enregistré comme fermé, le héro a le focus, aucune connexion au débogage ni ouverture d'écran système n'a eu lieu, et un second lancement simulé n'affiche pas le parcours

#### Scenario: états relus après retour système
- **WHEN** le test change l'état de la permission, du service d'accessibilité ou de l'écran d'accueil effectif pendant que SygixOs est en arrière-plan, puis simule le retour au premier plan
- **THEN** les lignes de 8.13, les états des deux voies et la ligne des réglages affichent le nouvel état, sans valeur périmée, et le focus est sur la ligne qui avait ouvert l'écran système

#### Scenario: filtre de la touche Home
- **WHEN** le test transmet au filtre du service, service connecté, une séquence pressé puis relâché courte, une séquence avec répétitions maintenue au-delà du délai d'appui long, une séquence dont le relâchement est annulé, et une autre touche
- **THEN** tous les événements Home sont consommés (le filtre renvoie vrai pour l'appui, chaque répétition et le relâchement ; un relâchement sans appui consommé reste au système) ; les deux premières séquences demandent l'ouverture de l'accueil une seule fois chacune, au relâchement, sans délai ; le relâchement annulé ne demande rien ; l'autre touche n'est pas consommée ; service non connecté, aucun événement n'est consommé

#### Scenario: service d'accessibilité non activé ou activé
- **WHEN** le double déclare le service non activé, puis activé et connecté, et que le test simule l'appui sur Home, SygixOs étant en arrière-plan puis au premier plan
- **THEN** non activé, la ligne « Ouvrir SygixOs avec la touche Home » affiche « À faire » et aucune touche n'est consommée ; activé, l'ouverture de l'accueil est demandée dans les deux cas, « Touche Home avec SygixOs au premier plan » s'applique au premier plan et la ligne affiche « Fait »

#### Scenario: attente de connexion et service bloqué
- **WHEN** le double déclare le service activé mais non connecté au lancement du processus, puis connecté pendant l'attente bornée dans un premier test et toujours non connecté à son terme dans un second
- **THEN** pendant l'attente la ligne affiche « En attente… » ; elle affiche « Fait » dans le premier cas, « Bloqué » avec son explication dans le second, et la voie par autorisations est alors « Bloquée »

#### Scenario: ouverture unique au démarrage
- **WHEN** le test simule, option activée, la fin du démarrage puis la connexion du service dans le même démarrage, puis la connexion du service seule, puis une connexion plus de 5 minutes après le démarrage, puis la fin du démarrage avec l'option désactivée
- **THEN** une seule ouverture est demandée dans le premier cas, une dans le deuxième, aucune dans les deux derniers

#### Scenario: démarrage automatique non observé
- **WHEN** le test simule un démarrage avec l'option activée avant ce démarrage, sans ouverture observée, puis affiche 8.13
- **THEN** « setup-manual-boot-detail » affiche « SygixOs ne s'est pas ouvert automatiquement à ce démarrage » ; avec une ouverture observée, l'option désactivée ou l'option activée après ce démarrage, le détail normal est affiché

#### Scenario: touche Home au premier plan
- **WHEN** le test affiche les réglages (avec la liste « Position d'À suivre » ouverte, et avec le panneau « Launcher principal » ou un dialogue de confirmation par-dessus), le parcours au premier lancement, la grille, le menu contextuel ou le mode « Déplacer », puis transmet un intent Home à l'activité au premier plan
- **THEN** réglages et surcouches sont fermés en une fois (parcours affiché automatiquement enregistré comme fermé, confirmation de désactivation traitée comme « Annuler »), la page de l'accueil est en haut et le héro a le focus ; une configuration en cours continue

#### Scenario: mise à jour sans opération privilégiée
- **WHEN** le test exécute une mise à jour intégrée factice jusqu'à la relance
- **THEN** aucune demande du rôle HOME ni connexion au débogage n'a lieu

### Requirement: Couverture de la configuration automatique
La configuration automatique, l'accès par le débogage, la désactivation et le rétablissement de l'accueil d'origine (`launcher-shell`) SHALL être couverts en JUnit et en tests Compose à la taille d'une TV avec des doubles de l'accès, des opérations privilégiées et de la lecture de l'état (`design.md`, D10), sans connexion ADB réelle : écrans affichés, séquence et étapes sans objet, réglages de secours, étapes dépendantes annulées, délais, perte de l'accès, garde-fous, ordre et annulation de la désactivation, rétablissement avec et sans écran d'accueil précédent relevé, refus des cibles hors de l'ensemble fermé, conservation des autres services d'accessibilité, fermeture de la connexion.

#### Scenario: séquence sans désactivation
- **WHEN** le double d'accès est prêt et rend SygixOs écran d'accueil effectif après la définition de l'écran d'accueil, et que le test presse OK sur « setup-choice-auto »
- **THEN** « setup-progress » (8.5) montre « setup-step-boot », « setup-step-home » et « setup-step-verify » passer à « Fait », puis « setup-done » (8.6) s'affiche avec « setup-done-finish » focalisé et l'état automatique « Active »

#### Scenario: écrans du débogage
- **WHEN** le double déclare le débogage désactivé, puis activé au retour, puis l'autorisation refusée, puis le débogage désactivé entre-temps
- **THEN** « setup-debug » (8.3) s'affiche avec « setup-debug-open » focalisé, puis « setup-authorize » (8.4) avec « setup-authorize-continue » focalisé, puis « setup-authorize » avec « setup-error » et « setup-authorize-retry » focalisé (8.11), puis « setup-debug » avec « setup-error » (8.10) ; aucune opération privilégiée n'est envoyée

#### Scenario: désactivation proposée puis refusée
- **WHEN** le double ne rend pas SygixOs écran d'accueil effectif après la définition, avec des composants actifs, et que le test presse « setup-more-keep » sur « setup-more » (8.7)
- **THEN** aucune désactivation n'est envoyée, le service d'accessibilité est ajouté sans retirer les autres, la superposition est accordée, l'option de démarrage est activée, et « setup-fallback » (8.9) s'affiche

#### Scenario: désactivation confirmée puis partiellement annulée
- **WHEN** le test presse « setup-more-disable », puis « confirm-disable-accept », et que le double fait échouer la désactivation du deuxième composant
- **THEN** « confirm-disable » (8.8) s'affiche d'abord avec « confirm-disable-cancel » focalisé ; après l'échec, le premier composant est remis à son état par défaut, l'état est relu, aucun succès n'est affiché et « setup-fallback » s'affiche

#### Scenario: échec d'une étape
- **WHEN** le double fait dépasser son délai à la définition de l'écran d'accueil
- **THEN** « setup-failed » (8.12) s'affiche avec « setup-step-home-state » « Échec », « setup-step-verify-state » « Annulée » et « setup-failed-retry » focalisé ; aucune nouvelle tentative n'a lieu sans action et la connexion est fermée

#### Scenario: voie indisponible sur cette TV
- **WHEN** le double déclare le débogage activé et le port ADB réseau injoignable
- **THEN** le parcours revient à 8.2, « setup-choice-auto-state » affiche « Indisponible sur cette TV » et « setup-choice-manual » a le focus

#### Scenario: cible refusée
- **WHEN** le test demande à la passerelle une opération sur un paquet hors du paquet SygixOs et de l'ensemble fermé
- **THEN** la passerelle la refuse sans rien envoyer au double

#### Scenario: rétablissement
- **WHEN** le double déclare deux composants désactivés par l'utilisateur et un écran d'accueil précédent relevé, et que le test presse « launcher-panel-restore » puis « confirm-restore-accept »
- **THEN** « confirm-restore » (8.17) s'est affiché avec « confirm-restore-accept » focalisé ; les composants sont remis à leur état par défaut dans l'ordre inverse, l'écran d'accueil précédent est redéfini, « launcher-panel-restore » n'existe plus et « launcher-panel-relaunch » a le focus

#### Scenario: panneau fermé pendant la configuration
- **WHEN** le test presse Retour pendant « setup-progress »
- **THEN** le panneau se ferme, la séquence du double continue jusqu'à sa fin, et la réouverture affiche l'écran de l'état relu

### Requirement: Sélecteurs de l'écran d'accueil
Les écrans 8.1 à 8.21 et la ligne des réglages SHALL exposer des testTags stables, utilisés par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) à la place du texte ou de la position : « setup-panel » (voile et panneau), « setup-mascot », « setup-title », « setup-description », « setup-hint » ; 8.1/8.2 : « setup-choice-auto », « setup-choice-manual », « setup-later » ; 8.3/8.10 : « setup-debug », « setup-debug-step-1 », « setup-debug-step-2 », « setup-debug-status », « setup-debug-open », « setup-debug-back » ; 8.4/8.11 : « setup-authorize », « setup-authorize-step-1 », « setup-authorize-step-2 », « setup-authorize-continue », « setup-authorize-retry », « setup-authorize-back » ; erreurs : « setup-error » (encadré, avec « setup-error-title » et « setup-error-detail ») ; 8.5 : « setup-progress » ; 8.6 : « setup-done », « setup-done-tip » (conseil), « setup-done-details », « setup-done-finish » ; 8.7 : « setup-more », « setup-more-home », « setup-more-keep », « setup-more-disable » ; 8.9 : « setup-fallback », « setup-fallback-home », « setup-fallback-overlay », « setup-fallback-boot », « setup-fallback-tip », « setup-fallback-details », « setup-fallback-finish » ; 8.12 : « setup-failed », « setup-failed-close », « setup-failed-retry » ; lignes d'étapes de 8.5 à 8.12 : « setup-step-boot », « setup-step-home », « setup-step-verify », « setup-step-stock » ; 8.13 : « setup-manual », « setup-manual-home », « setup-manual-boot », « setup-manual-overlay », « setup-manual-vendor-boot », « setup-manual-tip », « setup-manual-finish » ; 8.14 : « setup-details », « setup-details-item-<réglage> », « setup-details-close » ; 8.16 : « launcher-panel », « launcher-panel-auto », « launcher-panel-manual », « launcher-panel-relaunch », « launcher-panel-restore » ; dialogues 8.8 et 8.17 : « confirm-disable » et « confirm-restore », chacun avec « <dialogue>-title », « <dialogue>-text », « <dialogue>-accept », « <dialogue>-cancel » ; réglages : « setting-launcher-group », « setting-launcher-guide » ; pour toute ligne « <ligne> » ci-dessus, « <ligne>-title », « <ligne>-detail », « <ligne>-state » et « <ligne>-dot » (pastille, absente quand l'état n'en a pas). Les anciens testTags « onboarding-* », « setting-home-role », « setting-boot-start », « setting-accessibility-home » et « launcher-overlay-* » n'existent plus. Les tests SHALL vérifier les écrans, l'ordre, le focus initial, la navigation D-pad, les états, la présence de « setup-mascot » (absente des dialogues et de « setup-details ») et la fermeture définitive du parcours définis par `launcher-shell`, et la synthèse définie par « Réglages du launcher système » de `settings`.

#### Scenario: ordre et focus initial
- **WHEN** le test compose l'accueil avec le parcours à afficher, rien n'étant fait
- **THEN** « setup-panel » est affiché par-dessus « zone-hero » ; « setup-mascot » est à gauche de « setup-title » ; « setup-choice-auto » puis « setup-choice-manual » sont affichés de haut en bas avec les états « À configurer » et « À faire » ; « setup-hint » est à gauche de « setup-later » ; « setup-choice-auto » a le focus sans aucune touche pressée

#### Scenario: navigation
- **WHEN** le test presse bas trois fois, haut trois fois, puis gauche et droite sur 8.1, puis gauche et droite sur 8.3, 8.4, 8.7, 8.8 et 8.17
- **THEN** sur 8.1 le focus suit « setup-choice-auto », « setup-choice-manual », « setup-later » sans boucle et gauche et droite ne changent rien ; sur les écrans à deux boutons, gauche et droite passent d'un bouton à l'autre sans boucle ; « zone-hero », « zone-dock », « hero-capsule » et « settings-gear » ne prennent jamais le focus tant que « setup-panel » est affiché

#### Scenario: mascotte
- **WHEN** le test affiche chacun des écrans 8.1 à 8.17
- **THEN** « setup-mascot » existe sur les panneaux du parcours et n'existe pas dans « confirm-disable », « confirm-restore » ni « setup-details »

#### Scenario: page Par autorisations
- **WHEN** le test ouvre « setup-manual » sur un appareil avec gestionnaire d'auto-démarrage du constructeur sans écran retenu, l'option de démarrage activée et la superposition manquante
- **THEN** « setup-manual-home », « setup-manual-boot », « setup-manual-overlay », « setup-manual-vendor-boot » puis « setup-manual-tip » et « setup-manual-finish » sont affichés de haut en bas ; « setup-manual-vendor-boot-state » affiche « Indisponible » ; « setup-manual-tip » n'est pas focalisable ; la première ligne qui n'est pas « Fait » a le focus

#### Scenario: états affichés
- **WHEN** le test compose une ligne dans chacun des états qui la concernent, avec et sans le focus
- **THEN** « <ligne>-state » affiche le libellé ; « <ligne>-dot » existe pour « Fait », « Active », « Actif » (couleur `SwitchOn`) et « En cours… », « En attente… » (couleur `Badge`), et n'existe pas pour les autres ; « Échec » est rendu en couleur d'erreur ; en « Indisponible » et « Indisponible sur cette TV », la ligne prend le focus et OK n'ouvre rien

#### Scenario: ordre des contrôles de la catégorie
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** « setting-upnext-visible », « setting-upnext-position », « setting-launcher-group » et « setting-launcher-guide » sont affichés de haut en bas ; « setting-upnext-visible » a le focus ; « setting-launcher-group » n'est pas focalisable ; aucun des anciens testTags n'existe

#### Scenario: panneau depuis les réglages
- **WHEN** le test presse OK sur « setting-launcher-guide », puis Retour
- **THEN** « launcher-panel » s'affiche par-dessus « settings-home-screen » avec « launcher-panel-relaunch » focalisé ; après Retour, il n'existe plus, les réglages sont toujours affichés, « setting-launcher-guide » a le focus et le drapeau du parcours automatique n'est pas modifié

#### Scenario: synthèse de la ligne des réglages
- **WHEN** le test affiche « Écran d'accueil » avec rien de fait, puis la voie automatique active, puis la voie par autorisations en partie, puis bloquée
- **THEN** « setting-launcher-guide-state » et « setting-launcher-guide-detail » affichent « À configurer » et « Automatique : à configurer · Par autorisations : à faire », « Actif » (avec « setting-launcher-guide-dot ») et « Automatique : actif · Par autorisations : non utilisée », « En partie » et « Automatique : à configurer · Par autorisations : en partie », « Bloqué » et « Automatique : à configurer · Par autorisations : bloquée »
