# Delta settings

La capability `settings` vient de l'archivage de `p2b-settings`. Ce delta y ajoute « Position d'Up Next » (catégorie « Écran d'accueil ») et modifie « Page de réglages », « Apps sources » et « Cacher une application » pour couvrir la rangée Up Next.

## ADDED Requirements

### Requirement: Position d'Up Next
La catégorie « Écran d'accueil » des réglages SHALL proposer un contrôle « Position d'Up Next » valant « avant la grille » (défaut) ou « après la grille », persisté dans DataStore, l'effet sur la navigation de la zone grille étant spécifié par « Navigation 3 paliers » de launcher-shell.

#### Scenario: contrôle
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** le contrôle « Position d'Up Next » propose « avant la grille » (valeur par défaut) et « après la grille », et le choix est persisté dans DataStore puis restauré au démarrage

#### Scenario: navigation D-pad du contrôle
- **WHEN** le focus est sur le contrôle « Position d'Up Next »
- **THEN** OK bascule la valeur ; droite passe à la valeur suivante et gauche à la précédente, sans boucle aux bords ; gauche depuis la première valeur et Retour rendent le focus au volet des catégories sur « Écran d'accueil », la valeur choisie étant conservée ; droite depuis le volet des catégories ramène le focus sur le contrôle

#### Scenario: application immédiate
- **WHEN** la valeur du réglage change
- **THEN** la rangée Up Next est repositionnée sans redémarrage du launcher, et la navigation suit le scénario « position après la grille » de launcher-shell

## MODIFIED Requirements

### Requirement: Page de réglages
Le launcher SHALL offrir une page de réglages plein écran à la tvOS : volet catégories à gauche, contenu de la catégorie à droite, fond sombre neutre, navigable au DPAD uniquement.

#### Scenario: structure
- **WHEN** la page de réglages s'ouvre
- **THEN** le volet gauche liste les catégories « Apps sources », « Applications cachées », « Écran d'accueil », « À propos », le volet droit affiche le contenu de la catégorie active, la première catégorie porte le focus à l'ouverture

#### Scenario: changement de catégorie
- **WHEN** l'utilisateur presse haut/bas dans le volet gauche
- **THEN** la catégorie active change et le volet droit affiche son contenu ; droite depuis le volet gauche porte le focus sur le premier élément du volet droit, gauche depuis le volet droit le rend au volet gauche

#### Scenario: retour
- **WHEN** l'utilisateur presse Retour depuis la page de réglages
- **THEN** le home reprend avec le héro affiché et focusé (comportement standard), la lecture du héro reprend

### Requirement: Apps sources
La catégorie « Apps sources » SHALL lister toutes les apps TV installées avec, pour chacune, un toggle switch (style Apple) activant sa contribution au héro, au Top Shelf et à la rangée Up Next ; par défaut toutes les apps sont activées.

#### Scenario: présentation
- **WHEN** la catégorie « Apps sources » est affichée
- **THEN** chaque ligne montre l'icône de l'app, son nom et sous le nom des informations sur l'app dont le nombre de programmes publiés dans le TV Provider, avec à droite de la ligne un toggle switch

#### Scenario: bascule
- **WHEN** l'utilisateur presse OK sur une ligne
- **THEN** le switch bascule avec l'animation Apple, l'effet est immédiat : les programmes de l'app disparaissent ou réapparaissent dans le héro, le Top Shelf et la rangée Up Next sans redémarrage, et l'état est persisté (DataStore)

### Requirement: Cacher une application
Le menu contextuel d'une tuile SHALL offrir une option « Cacher » en plus des actions existantes (épingler/retirer du dock, déplacer) ; une app cachée disparaît de la grille et du dock.

#### Scenario: action cacher
- **WHEN** l'utilisateur choisit « Cacher » dans le menu contextuel (appui long sur OK)
- **THEN** l'app disparaît de la grille et du dock (épinglage éventuel retiré), sans confirmation supplémentaire, et l'état est persisté (DataStore)

#### Scenario: portée du masquage
- **WHEN** une app est cachée
- **THEN** elle n'apparaît plus ni dans la grille ni dans le dock ; sa contribution au héro, au Top Shelf et à Up Next reste régie uniquement par les apps sources

#### Scenario: réinstallation
- **WHEN** une app cachée est réinstallée ou mise à jour
- **THEN** elle reste cachée jusqu'à réactivation explicite dans les réglages
