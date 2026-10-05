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
- **THEN** le visuel du héro est demandé en 1920 × 1080 et la bannière en 480 × 270, sans agrandir une image plus petite

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
