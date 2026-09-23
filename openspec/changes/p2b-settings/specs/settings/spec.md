# Delta settings

## ADDED Requirements

### Requirement: Icône réglages flottante
Le home SHALL afficher sur le héro une icône engrenage flottante en haut à droite, discrète (verre translucide, faible opacité), qui ouvre la page de réglages.

#### Scenario: affichage
- **WHEN** le héro est affiché
- **THEN** l'icône engrenage est visible en haut à droite, en matériau verre translucide discret (lisible sur le héro vidéo sans le masquer), sans détourner le focus du héro à l'ouverture du home

#### Scenario: accès DPAD
- **WHEN** l'utilisateur presse haut depuis le héro
- **THEN** l'icône réglages prend le focus avec l'état focus net (zoom + halo, cohérent avec le focus tvOS du launcher) ; bas depuis l'icône rend le focus au héro

#### Scenario: ouverture
- **WHEN** l'utilisateur presse OK sur l'icône réglages
- **THEN** la page de réglages plein écran s'ouvre, la lecture du héro est mise en pause

### Requirement: Page de réglages
Le launcher SHALL offrir une page de réglages plein écran à la tvOS : volet catégories à gauche, contenu de la catégorie à droite, fond sombre neutre, navigable au DPAD uniquement.

#### Scenario: structure
- **WHEN** la page de réglages s'ouvre
- **THEN** le volet gauche liste les catégories « Apps sources », « Applications cachées », « À propos », le volet droit affiche le contenu de la catégorie active, la première catégorie porte le focus à l'ouverture

#### Scenario: changement de catégorie
- **WHEN** l'utilisateur presse haut/bas dans le volet gauche
- **THEN** la catégorie active change et le volet droit affiche son contenu ; droite depuis le volet gauche porte le focus sur le premier élément du volet droit, gauche depuis le volet droit le rend au volet gauche

#### Scenario: retour
- **WHEN** l'utilisateur presse Retour depuis la page de réglages
- **THEN** le home reprend avec le héro affiché et focusé (comportement standard), la lecture du héro reprend

### Requirement: Apps sources
La catégorie « Apps sources » SHALL lister toutes les apps TV installées avec, pour chacune, un toggle switch (style Apple) activant sa contribution au héro et au Top Shelf ; par défaut toutes les apps sont activées.

#### Scenario: présentation
- **WHEN** la catégorie « Apps sources » est affichée
- **THEN** chaque ligne montre l'icône de l'app, son nom et sous le nom des informations sur l'app dont le nombre de programmes publiés dans le TV Provider, avec à droite de la ligne un toggle switch

#### Scenario: bascule
- **WHEN** l'utilisateur presse OK sur une ligne
- **THEN** le switch bascule avec l'animation Apple, l'effet est immédiat : les programmes de l'app disparaissent ou réapparaissent dans le héro et le Top Shelf sans redémarrage, et l'état est persisté (DataStore)

### Requirement: Cacher une application
Le menu contextuel d'une tuile SHALL offrir une option « Cacher » en plus des actions existantes (épingler/retirer du dock, déplacer) ; une app cachée disparaît de la grille et du dock.

#### Scenario: action cacher
- **WHEN** l'utilisateur choisit « Cacher » dans le menu contextuel (appui long sur OK)
- **THEN** l'app disparaît de la grille et du dock (épinglage éventuel retiré), sans confirmation supplémentaire, et l'état est persisté (DataStore)

#### Scenario: portée du masquage
- **WHEN** une app est cachée
- **THEN** elle n'apparaît plus ni dans la grille ni dans le dock ; sa contribution au héro et au Top Shelf reste régie uniquement par les apps sources

#### Scenario: réinstallation
- **WHEN** une app cachée est réinstallée ou mise à jour
- **THEN** elle reste cachée jusqu'à réactivation explicite dans les réglages

### Requirement: Applications cachées
La catégorie « Applications cachées » SHALL offrir un sous-écran plein écran listant les apps cachées, chacune réactivable par un toggle switch, avec un bouton « Tout réactiver ».

#### Scenario: sous-écran
- **WHEN** l'utilisateur valide « Applications cachées »
- **THEN** un sous-écran plein écran liste les apps cachées (icône + nom) avec un switch par ligne et un bouton « Tout réactiver » ; Retour revient au volet de réglages

#### Scenario: réactivation
- **WHEN** l'utilisateur bascule le switch d'une app ou active « Tout réactiver »
- **THEN** l'app ou toutes les apps réapparaissent dans la grille (ordre alphabétique par défaut si l'ordre précédent n'existe plus), effet immédiat, état persisté

#### Scenario: aucune app cachée
- **WHEN** aucune app n'est cachée
- **THEN** le sous-écran affiche uniquement un message d'état vide centré dans son panneau (aucune ligne, aucun bouton « Tout réactiver » rendu, aucun crash)

### Requirement: À propos
La catégorie « À propos » SHALL afficher la version du launcher et les licences des bibliothèques open source utilisées.

#### Scenario: contenu
- **WHEN** la catégorie « À propos » est affichée
- **THEN** la version de l'application et la liste des licences OSS sont visibles, navigables au DPAD
