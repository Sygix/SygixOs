# Delta settings

La capability `settings` vient de l'archivage de `p2b-settings`. Ce delta y ajoute « Position d'Up Next » (catégorie « Écran d'accueil ») et modifie « Page de réglages », « Apps sources » et « Cacher une application » pour couvrir la rangée Up Next. Les trois blocs MODIFIED partent du texte courant de `openspec/specs/settings/spec.md`, après l'archivage de `home-settings-polish`, et n'y ajoutent que la catégorie « Écran d'accueil » et la rangée Up Next ; « Page de réglages » reprend aussi le fond, les pilules de focus et la vignette des lignes d'apps du change `ui-tvos-polish`, archivé avant celui-ci.

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
Le launcher SHALL offrir une page de réglages plein écran à la tvOS : volet catégories à gauche, contenu de la catégorie à droite, sur le même fond que la zone grille de l'accueil (« Fond de la zone grille » de `launcher-shell`), navigable au DPAD uniquement. Le focus de toute ligne focusable de la page (catégories, lignes d'« Apps sources » et d'« Applications cachées », bouton « Tout réactiver », contrôle « Position d'Up Next », lignes d'« À propos ») SHALL être une pilule claire à texte et icônes sombres, sans zoom, sans halo et sans matériau verre. La catégorie active SHALL rester marquée par une pilule grise discrète quand le focus est dans le volet droit.

#### Scenario: structure
- **WHEN** la page de réglages s'ouvre
- **THEN** le volet gauche liste les catégories « Apps sources », « Applications cachées », « Écran d'accueil », « À propos », le volet droit affiche le contenu de la catégorie active, la première catégorie porte le focus à l'ouverture

#### Scenario: changement de catégorie
- **WHEN** l'utilisateur presse haut/bas dans le volet gauche
- **THEN** la catégorie active change et le volet droit affiche son contenu ; droite depuis le volet gauche porte le focus sur le premier élément du volet droit, sauf pour « Applications cachées » dont le focus initial est la première ligne de la liste (« Applications cachées », scénario « focus initial ») ; gauche depuis le volet droit le rend au volet gauche

#### Scenario: retour
- **WHEN** l'utilisateur presse Retour depuis la page de réglages
- **THEN** le home reprend avec le héro affiché et focusé (comportement standard), la lecture du héro reprend

#### Scenario: fond
- **WHEN** la page de réglages s'ouvre
- **THEN** son fond est identique à celui de la zone grille de l'accueil

#### Scenario: focus d'une ligne
- **WHEN** une ligne de la page prend le focus
- **THEN** elle devient une pilule claire à texte et icônes sombres, à la même taille et à la même place qu'au repos (aucun zoom), sans halo ni verre ; au repos les lignes n'ont pas de fond, sauf « Tout réactiver » qui garde un fond discret

#### Scenario: catégorie active, focus à droite
- **WHEN** le focus est dans le volet droit
- **THEN** la catégorie active est une pilule grise discrète à texte blanc et les autres catégories n'ont pas de fond

#### Scenario: vignette des lignes d'apps
- **WHEN** une ligne d'app est affichée (« Apps sources », « Applications cachées »)
- **THEN** l'icône de l'app est présentée dans une vignette 16:9 : la bannière TV de l'app quand elle en a une, sinon son icône entière centrée, comme le repli des tuiles de l'accueil

### Requirement: Apps sources
La catégorie « Apps sources » SHALL lister toutes les apps TV installées avec, pour chacune, un toggle switch (style Apple) activant sa contribution au héro, au Top Shelf et à la rangée Up Next ; par défaut toutes les apps sont activées. La liste SHALL être triée : d'abord les apps dont le nombre de programmes publiés affiché est supérieur à 0, par nombre décroissant et, à nombre égal, par ordre alphabétique de leur nom (insensible à la casse), puis les apps à 0, par ordre alphabétique de leur nom (insensible à la casse). Les compteurs de programmes par app SHALL être calculés dès l'affichage de l'accueil au lancement du launcher, et non à l'ouverture des réglages, par l'unique observation existante du TV Provider (sans seconde lecture). Cette observation SHALL être mise en pause quand le launcher passe en arrière-plan (aucune relecture du TV Provider pour les compteurs tant qu'une autre app est au premier plan) et un nouveau comptage SHALL être lancé à chaque retour du launcher au premier plan (décision de Sygix). Cet ordre SHALL être calculé à l'entrée dans la catégorie (ouverture des réglages ou passage depuis une autre catégorie) avec les compteurs connus à ce moment. Si aucun comptage n'est encore arrivé à l'entrée, la liste SHALL s'afficher d'abord par ordre alphabétique, puis être retriée une seule fois, à l'arrivée du premier comptage, le focus restant sur l'app qui l'avait (et non sur la position). L'ordre SHALL ensuite rester figé tant que la catégorie reste active : les compteurs affichés se mettent à jour sur place, mais les lignes ne changent plus de position sous le focus. Un aller-retour entre le volet gauche et le volet droit sans changer de catégorie ne recalcule pas l'ordre.

#### Scenario: présentation
- **WHEN** la catégorie « Apps sources » est affichée
- **THEN** chaque ligne montre l'icône de l'app, son nom et sous le nom des informations sur l'app dont le nombre de programmes publiés dans le TV Provider, avec à droite de la ligne un toggle switch

#### Scenario: tri par contenu publié
- **WHEN** la catégorie est affichée avec des apps publiant respectivement 12, 0, 3 et 0 programmes
- **THEN** l'ordre est : l'app à 12, l'app à 3, puis les deux apps à 0 par ordre alphabétique

#### Scenario: compteurs prêts à l'entrée
- **WHEN** l'utilisateur entre dans la catégorie « Apps sources » alors que le premier comptage, lancé à l'affichage de l'accueil, est déjà arrivé
- **THEN** la liste s'affiche directement triée par compteur, sans passage par l'ordre alphabétique ni retri ultérieur tant que la catégorie reste active

#### Scenario: compteurs en retard
- **WHEN** l'utilisateur entre dans la catégorie « Apps sources » avant l'arrivée du premier comptage, puis ce comptage arrive alors que le focus est sur une ligne
- **THEN** la liste s'affiche d'abord par ordre alphabétique, puis elle est retriée une seule fois par compteur à l'arrivée du comptage ; le focus reste sur la même app, à sa nouvelle position ; les émissions suivantes mettent seulement à jour les nombres affichés, sans retri

#### Scenario: égalité de compteur
- **WHEN** deux apps publient le même nombre de programmes, supérieur à 0
- **THEN** elles sont départagées par ordre alphabétique de leur nom (insensible à la casse), à leur place dans la partie des apps à compteur non nul

#### Scenario: compteurs mis à jour dans la catégorie
- **WHEN** un compteur change (programme publié ou retiré dans le TV Provider) alors que la catégorie « Apps sources » est active
- **THEN** le nombre affiché sur la ligne se met à jour sur place, aucune ligne ne change de position et le focus reste sur la même ligne (hors le retri unique du scénario « compteurs en retard »)

#### Scenario: nouvel ordre à la prochaine entrée
- **WHEN** l'utilisateur quitte la catégorie (autre catégorie ou fermeture des réglages) puis y revient après un changement de compteur
- **THEN** l'ordre est recalculé avec les compteurs connus à cette nouvelle entrée

#### Scenario: comptage en pause en arrière-plan
- **WHEN** le launcher passe en arrière-plan (une autre app au premier plan) et qu'une app publie ou retire des programmes pendant ce temps
- **THEN** l'observation du TV Provider pour les compteurs est arrêtée : aucune relecture n'a lieu tant que le launcher reste en arrière-plan ; les derniers compteurs connus sont conservés

#### Scenario: nouveau comptage au retour au premier plan
- **WHEN** le launcher revient au premier plan
- **THEN** un nouveau comptage est lancé aussitôt et l'observation reprend ; les nombres affichés se mettent à jour sur place, la prochaine entrée dans « Apps sources » ordonne la liste avec ce comptage, et une catégorie « Apps sources » déjà active garde son ordre (aucun retri : le retri unique du scénario « compteurs en retard » ne concerne que le tout premier comptage)

#### Scenario: aucun compteur disponible
- **WHEN** le comptage ne peut rien lire (TV Provider absent ou permission refusée)
- **THEN** toutes les apps sont considérées à 0 et listées par ordre alphabétique, y compris après l'arrivée du comptage vide, sans erreur visible ni crash ; le switch de chaque ligne reste utilisable

#### Scenario: bascule
- **WHEN** l'utilisateur presse OK sur une ligne
- **THEN** le switch bascule avec l'animation Apple, l'effet est immédiat : les programmes de l'app disparaissent ou réapparaissent dans le héro, le Top Shelf et la rangée Up Next sans redémarrage, et l'état est persisté (DataStore) ; la bascule ne change pas la position de la ligne (le tri ne dépend pas de l'état du switch) et le focus reste sur la ligne

### Requirement: Cacher une application
Le menu contextuel d'une tuile SHALL offrir une option « Cacher » en plus des actions existantes (épingler/retirer du dock, déplacer) ; une app cachée disparaît de la grille et du dock. Chaque masquage SHALL être daté (instant du masquage) et cette date SHALL être persistée avec l'app cachée ; les apps cachées avant l'introduction de la date restent cachées, sans date.

#### Scenario: action cacher
- **WHEN** l'utilisateur choisit « Cacher » dans le menu contextuel (appui long sur OK)
- **THEN** l'app disparaît de la grille et du dock (épinglage éventuel retiré), sans confirmation supplémentaire, et l'état est persisté (DataStore) avec la date du masquage

#### Scenario: portée du masquage
- **WHEN** une app est cachée
- **THEN** elle n'apparaît plus ni dans la grille ni dans le dock ; sa contribution au héro, au Top Shelf et à Up Next reste régie uniquement par les apps sources

#### Scenario: réinstallation
- **WHEN** une app cachée est réinstallée ou mise à jour
- **THEN** elle reste cachée jusqu'à réactivation explicite dans les réglages, avec sa date de masquage inchangée

#### Scenario: nouveau masquage d'une app réactivée
- **WHEN** une app réactivée est cachée à nouveau
- **THEN** la date persistée est celle du nouveau masquage

#### Scenario: compatibilité de lecture
- **WHEN** le launcher démarre avec des apps cachées enregistrées par une version antérieure (ensemble sans date)
- **THEN** ces apps restent cachées (grille et dock), sont listées dans « Applications cachées » sans date, et aucune donnée n'est perdue ni réécrite au démarrage
