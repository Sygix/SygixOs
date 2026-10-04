# Delta ui-testing

## MODIFIED Requirements

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
- **WHEN** le focus passe d'une tuile à sa voisine de droite, puis à la rangée suivante, dans une grille de 20 apps, ou qu'un visuel d'une autre app devient vérifié (observateur de composition)
- **THEN** un déplacement recompose au plus 30 scopes et la vérification d'un visuel au plus 40, plafonds que dépassait l'implémentation précédente (43 et 95)

## ADDED Requirements

### Requirement: Couverture des infos du héro
Les infos du programme dans le héro (« Diaporama héro » de `launcher-shell`) SHALL être couvertes par des tests JUnit (choix du libellé de l'en-tête, éléments de la ligne d'infos, arrondis de la durée et du temps restant, lecture des colonnes du TV Provider) et par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) avec une source d'icônes de test, sans capture d'image et avec des titres fictifs uniquement.

#### Scenario: types de programme
- **WHEN** le héro affiche un programme `CONTINUE`, `NEXT`, `NEW`, `WATCHLIST`, un preview program et un watch next sans type
- **THEN** « hero-header-label » vaut respectivement « Continuer dans X », « Épisode suivant dans X », « Nouveau dans X », « À regarder dans X », « X » et « X »

#### Scenario: données manquantes
- **WHEN** le nom de l'app, son icône, la saison, l'épisode, la durée ou la position manquent
- **THEN** seuls les éléments disponibles existent (« hero-source-icon », « hero-header-label », « hero-details », « hero-progress-bar », « hero-remaining »), « hero-header » n'existe pas sans icône ni libellé, et sans ligne d'infos le bas du titre est le bas de « hero-metadata »

#### Scenario: héro immobile
- **WHEN** un programme avec en-tête, ligne d'infos et temps restant est affiché pendant le Ken Burns
- **THEN** des états Compose sont modifiés (zoom) mais aucune recomposition n'a lieu (`IdleFrameTest`)
