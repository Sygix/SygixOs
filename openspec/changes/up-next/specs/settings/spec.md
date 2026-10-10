# Delta settings

La capability `settings` vient de l'archivage de `p2b-settings`. Ce delta y ajoute « Catégorie « Écran d'accueil » », « Afficher Up Next » et « Position d'Up Next » (rendu conforme aux maquettes Penpot validées le 2026-10-10, écrans 7.7 et 7.8) et modifie « Page de réglages », « Apps sources » et « Cacher une application » pour couvrir la rangée Up Next. Les blocs MODIFIED partent du texte courant de `openspec/specs/settings/spec.md`, après l'archivage de `home-settings-polish`, et n'y ajoutent que la catégorie « Écran d'accueil », ses deux contrôles, la rangée Up Next et le comptage des `WatchNextPrograms` ; « Page de réglages » reprend aussi le fond, les pilules de focus et la vignette des lignes d'apps du change `ui-tvos-polish`, archivé avant celui-ci.

## ADDED Requirements

### Requirement: Catégorie « Écran d'accueil »
Le volet droit de la catégorie « Écran d'accueil » (`SettingsCategory.HOME_SCREEN`, placée avant « À propos » dans le volet des catégories) SHALL reprendre l'en-tête des autres catégories, conformément à la maquette Penpot validée (« SygixOs Maquette », page « TV », écran 7.7) : le titre « Écran d'accueil » en `headlineSmall`, puis la description « Choisissez ce qu'affiche l'accueil de SygixOs. » en `bodyMedium` blanc à 60 %, avec les mêmes espacements que « Apps sources » et « Applications cachées », puis les lignes de réglage. Les lignes SHALL avoir la forme des lignes des réglages (`Dimens.SettingsRowCorner`), une hauteur minimale `Dimens.SettingsRowHeight` et les retraits des lignes d'« Apps sources », avec la pilule de focus de « Page de réglages » ; « Afficher Up Next » est la première, « Position d'Up Next » la suit ; les réglages ajoutés par d'autres changes viennent après, sans changer ce focus initial.

#### Scenario: en-tête du volet
- **WHEN** la catégorie « Écran d'accueil » est active
- **THEN** le volet droit affiche le titre « Écran d'accueil », la description « Choisissez ce qu'affiche l'accueil de SygixOs. », puis la ligne « Afficher Up Next » suivie de la ligne « Position d'Up Next », avec le même en-tête et les mêmes espacements que les autres catégories

#### Scenario: lignes de la catégorie
- **WHEN** une ligne de la catégorie est affichée, avec ou sans le focus
- **THEN** son rectangle a la même hauteur minimale et les mêmes retraits que les lignes d'« Apps sources », et son libellé est en `TextStyles.Row` au repos et `TextStyles.RowFocused` au focus

### Requirement: Afficher Up Next
La catégorie « Écran d'accueil » SHALL proposer, en première ligne, le réglage « Afficher Up Next » : une ligne avec un toggle switch (style Apple) identique à la ligne « Inclure les préversions » d'« À propos » (libellé à gauche, switch à droite ; maquette Penpot 7.7), activée par défaut et persistée dans DataStore. Ce réglage autorise l'affichage sans le forcer : la rangée n'est visible que si elle dispose de contenu affichable et que la permission requise est accordée. Lorsqu'il est désactivé, la rangée et son titre sont masqués et ignorés par la navigation (« Rangée Up Next » et « Navigation 3 paliers » de `launcher-shell`), sans modifier les programmes publiés ni les filtres des apps sources. La navigation D-pad de la ligne SHALL suivre « Page de réglages » : gauche rend le focus au volet des catégories, Retour ferme les réglages, haut et bas passent d'un contrôle de la catégorie à l'autre.

#### Scenario: réglage activé
- **WHEN** « Afficher Up Next » est activé
- **THEN** la rangée est présentée selon son réglage de position et les programmes publiés par les apps sources actives

#### Scenario: réglage désactivé
- **WHEN** « Afficher Up Next » est désactivé
- **THEN** la rangée et son titre « À suivre » sont masqués et la navigation de la zone grille les saute ; le réglage reste conservé après redémarrage

#### Scenario: rangée indisponible malgré le réglage activé
- **WHEN** « Afficher Up Next » est activé, mais qu'aucun programme affichable n'est publié ou que la permission est refusée
- **THEN** la rangée reste masquée conformément à la capability up-next et la navigation de la zone grille la saute jusqu'à ce qu'elle soit effectivement affichée

#### Scenario: bascule
- **WHEN** l'utilisateur presse OK sur la ligne « Afficher Up Next »
- **THEN** le switch bascule avec l'animation Apple, l'état est persisté (DataStore), l'effet sur l'accueil est immédiat, sans redémarrage ; le focus reste sur la ligne

#### Scenario: navigation D-pad de la ligne
- **WHEN** le focus est sur « Afficher Up Next »
- **THEN** bas porte le focus sur « Position d'Up Next » ; haut ne fait rien (première ligne de la catégorie, sans boucle) ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » (« Page de réglages », scénario « changement de catégorie ») ; Retour ferme les réglages et le home reprend avec le héro focusé (« Page de réglages », scénario « retour »)

#### Scenario: focus initial de la catégorie
- **WHEN** l'utilisateur presse droite depuis la catégorie « Écran d'accueil » du volet des catégories
- **THEN** le focus est placé sur le premier contrôle de la catégorie, « Afficher Up Next », qui prend la pilule de focus des lignes (« Page de réglages », scénario « focus d'une ligne ») ; après gauche vers le volet puis droite, le focus revient sur « Afficher Up Next » et les valeurs des deux réglages sont inchangées

### Requirement: Position d'Up Next
La catégorie « Écran d'accueil » SHALL proposer, sous « Afficher Up Next », le contrôle « Position d'Up Next » : une liste déroulante à deux valeurs, « Avant les applications » (défaut) et « Après les applications », persistée dans DataStore et restaurée au démarrage ; l'effet sur la zone grille est spécifié par « Grille d'apps » et « Navigation 3 paliers » de `launcher-shell`. Son rendu SHALL suivre les maquettes Penpot validées (« SygixOs Maquette », page « TV », écrans 7.7 et 7.8 ; composants « Ligne de réglage à valeur » et « Liste déroulante » de la page « Composants »). Fermée, la ligne affiche le libellé « Position d'Up Next » à gauche et, à droite, la valeur courante en `TextStyles.RowState` dans la couleur tertiaire de la pilule (`SygixColors.OnDarkSecondary` au repos, `OnPillTertiary` au focus) suivie, à `Dimens.SettingsChevronGap`, d'un chevron vers le bas de `Dimens.SettingsChevron` de la même couleur. Ouverte, la ligne SHALL prendre le fond `SygixColors.PillSelected` et son chevron SHALL pointer vers le haut ; la liste SHALL présenter les deux valeurs dans cet ordre dans un panneau verre `GlassSurface` au look `GlassLook.Menu` (le matériau du menu contextuel), de largeur `Dimens.DropdownWidth`, coins `Dimens.DropdownCorner`, padding `Dimens.DropdownPadding`, ancré `Dimens.DropdownOffset` sous la ligne avec son bord droit à `Dimens.DropdownInsetEnd` à l'intérieur du bord droit de la ligne, dessiné par-dessus le volet droit, sans voile ; chaque option est une ligne de `Dimens.DropdownOptionHeight` aux coins `Dimens.DropdownOptionCorner` (concentriques avec le panneau), séparée de la suivante par `Dimens.DropdownOptionGap`, avec à gauche une colonne de `Dimens.DropdownCheck` qui porte une coche sur la valeur courante seulement, puis le libellé en `TextStyles.Row` (`RowFocused` au focus) ; la pilule de focus est celle de « Page de réglages ». Aucune animation n'est imposée au-delà de celles du design system. Tant que la liste est ouverte, le focus SHALL y rester. La navigation D-pad de la ligne fermée SHALL suivre « Page de réglages », comme « Afficher Up Next ».

#### Scenario: contrôle fermé
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** la ligne « Position d'Up Next » affiche à droite la valeur courante, « Avant les applications » par défaut, en `TextStyles.RowState`, suivie d'un chevron vers le bas ; la valeur affichée est celle persistée dans DataStore, y compris après un redémarrage du launcher

#### Scenario: ouverture de la liste
- **WHEN** le focus est sur « Position d'Up Next » fermée et l'utilisateur presse OK
- **THEN** la liste s'ouvre dans son panneau verre sous la ligne, par-dessus le volet droit, avec « Avant les applications » puis « Après les applications », la valeur courante est cochée et a le focus ; la ligne du contrôle prend le fond `SygixColors.PillSelected` et son chevron pointe vers le haut tant que la liste est ouverte, et la catégorie active reste marquée par la pilule grise discrète (« Page de réglages »)

#### Scenario: déplacements dans la liste
- **WHEN** la liste est ouverte et l'utilisateur presse haut, bas, gauche ou droite
- **THEN** haut et bas déplacent le focus d'une valeur à l'autre, sans boucle aux bords (haut depuis la première valeur et bas depuis la dernière ne font rien) ; gauche et droite ne font rien ; le focus ne quitte jamais la liste tant qu'elle est ouverte

#### Scenario: choix d'une valeur
- **WHEN** la liste est ouverte et l'utilisateur presse OK sur une valeur
- **THEN** cette valeur devient la valeur du réglage, la liste se ferme, le focus revient sur la ligne « Position d'Up Next » qui affiche la nouvelle valeur ; la valeur est persistée dans DataStore et appliquée à l'accueil sans redémarrage ; OK sur la valeur déjà cochée ferme la liste sans rien changer

#### Scenario: fermeture sans changement
- **WHEN** la liste est ouverte et l'utilisateur presse Retour
- **THEN** la liste se ferme sans changer la valeur, le focus revient sur la ligne « Position d'Up Next » et les réglages restent ouverts

#### Scenario: états de focus
- **WHEN** le focus passe sur la ligne fermée, puis sur une valeur de la liste ouverte
- **THEN** la ligne fermée focusée est une pilule claire à texte, valeur et chevron sombres (« Page de réglages », scénario « focus d'une ligne ») ; dans la liste, la valeur focusée est une pilule claire à texte sombre, sans zoom ni halo, l'autre valeur n'a pas de fond, et la coche reste dans la colonne de gauche de la valeur courante, qu'elle ait le focus ou non ; seul le panneau de la liste est en verre, pas les pilules

#### Scenario: navigation D-pad du contrôle fermé
- **WHEN** le focus est sur « Position d'Up Next » fermée
- **THEN** haut porte le focus sur « Afficher Up Next » ; bas ne fait rien tant que « Position d'Up Next » est la dernière ligne de la catégorie (sans boucle) ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » ; Retour ferme les réglages et le home reprend avec le héro focusé (« Page de réglages », scénario « retour »)

#### Scenario: launcher en arrière-plan liste ouverte
- **WHEN** la liste est ouverte et le launcher passe en arrière-plan (touche Home de la télécommande ou autre app au premier plan) alors que les réglages restent ouverts, puis revient au premier plan
- **THEN** la liste est fermée sans changement de valeur, les réglages sont toujours ouverts, le focus est sur la ligne « Position d'Up Next » et celle-ci affiche la valeur persistée, inchangée

#### Scenario: réglages rouverts
- **WHEN** les réglages sont fermés puis rouverts après que la liste a été ouverte
- **THEN** la liste est fermée et la ligne affiche la valeur persistée, inchangée

#### Scenario: application immédiate
- **WHEN** la valeur du réglage change
- **THEN** la rangée Up Next et son titre sont repositionnés sans redémarrage du launcher, et la navigation suit le scénario « position après les applications » de « Navigation 3 paliers » (`launcher-shell`)

#### Scenario: « Afficher Up Next » désactivé
- **WHEN** « Afficher Up Next » est désactivé et le focus arrive sur « Position d'Up Next »
- **THEN** la ligne a le même rendu, le même focus et la même liste qu'avec la rangée affichée (jamais grisée ni sautée), la valeur choisie est persistée et s'applique dès que « Afficher Up Next » est réactivé

## MODIFIED Requirements

### Requirement: Page de réglages
Le launcher SHALL offrir une page de réglages plein écran à la tvOS : volet catégories à gauche, contenu de la catégorie à droite, sur le même fond que la zone grille de l'accueil (« Fond de la zone grille » de `launcher-shell`), navigable au DPAD uniquement. Le focus de toute ligne focusable de la page (catégories, lignes d'« Apps sources » et d'« Applications cachées », bouton « Tout réactiver », lignes « Afficher Up Next » et « Position d'Up Next » et valeurs de la liste déroulante de « Position d'Up Next », lignes d'« À propos ») SHALL être une pilule claire à texte et icônes sombres, sans zoom, sans halo et sans matériau verre. La catégorie active SHALL rester marquée par une pilule grise discrète quand le focus est dans le volet droit.

#### Scenario: structure
- **WHEN** la page de réglages s'ouvre
- **THEN** le volet gauche liste les catégories « Apps sources », « Applications cachées », « Écran d'accueil », « À propos », le volet droit affiche le contenu de la catégorie active, la première catégorie porte le focus à l'ouverture

#### Scenario: changement de catégorie
- **WHEN** l'utilisateur presse haut/bas dans le volet gauche
- **THEN** la catégorie active change et le volet droit affiche son contenu ; droite depuis le volet gauche porte le focus sur le premier élément du volet droit (« Afficher Up Next » pour « Écran d'accueil »), sauf pour « Applications cachées » dont le focus initial est la première ligne de la liste (« Applications cachées », scénario « focus initial ») ; gauche depuis le volet droit le rend au volet gauche, sur la catégorie active, sauf quand la liste déroulante de « Position d'Up Next » est ouverte (« Position d'Up Next », scénario « déplacements dans la liste »)

#### Scenario: retour
- **WHEN** l'utilisateur presse Retour depuis la page de réglages
- **THEN** le home reprend avec le héro affiché et focusé (comportement standard), la lecture du héro reprend ; seule exception : quand la liste déroulante de « Position d'Up Next » est ouverte, Retour la ferme d'abord, sans quitter les réglages (« Position d'Up Next », scénario « fermeture sans changement »)

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
La catégorie « Apps sources » SHALL lister toutes les apps TV installées avec, pour chacune, un toggle switch (style Apple) activant sa contribution au héro, au Top Shelf et à la rangée Up Next ; par défaut toutes les apps sont activées. Le nombre affiché SHALL compter tous les programmes publiés par chaque app dans le TV Provider, `PreviewPrograms` et `WatchNextPrograms` compris. La liste SHALL être triée : d'abord les apps dont le nombre de programmes publiés affiché est supérieur à 0, par nombre décroissant et, à nombre égal, par ordre alphabétique de leur nom (insensible à la casse), puis les apps à 0, par ordre alphabétique de leur nom (insensible à la casse). Les compteurs de programmes par app SHALL être calculés dès l'affichage de l'accueil au lancement du launcher, et non à l'ouverture des réglages, par l'unique observation existante du TV Provider (sans seconde lecture), qui SHALL réagir aux changements des `PreviewPrograms` comme des `WatchNextPrograms`. Cette observation SHALL être mise en pause quand le launcher passe en arrière-plan (aucune relecture du TV Provider pour les compteurs tant qu'une autre app est au premier plan) et un nouveau comptage SHALL être lancé à chaque retour du launcher au premier plan (décision de Sygix). Cet ordre SHALL être calculé à l'entrée dans la catégorie (ouverture des réglages ou passage depuis une autre catégorie) avec les compteurs connus à ce moment. Si aucun comptage n'est encore arrivé à l'entrée, la liste SHALL s'afficher d'abord par ordre alphabétique, puis être retriée une seule fois, à l'arrivée du premier comptage, le focus restant sur l'app qui l'avait (et non sur la position). L'ordre SHALL ensuite rester figé tant que la catégorie reste active : les compteurs affichés se mettent à jour sur place, mais les lignes ne changent plus de position sous le focus. Un aller-retour entre le volet gauche et le volet droit sans changer de catégorie ne recalcule pas l'ordre.

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

#### Scenario: app qui ne publie que du Watch Next
- **WHEN** une app ne publie aucun `PreviewPrograms` mais publie des `WatchNextPrograms`
- **THEN** son compteur affiché est le nombre de ses `WatchNextPrograms`, supérieur à 0, et elle est triée parmi les apps à compteur non nul selon ce nombre, jamais parmi les apps à 0

#### Scenario: égalité de compteur
- **WHEN** deux apps publient le même nombre de programmes, supérieur à 0
- **THEN** elles sont départagées par ordre alphabétique de leur nom (insensible à la casse), à leur place dans la partie des apps à compteur non nul

#### Scenario: compteurs mis à jour dans la catégorie
- **WHEN** un compteur change (programme `PreviewPrograms` ou `WatchNextPrograms` publié ou retiré dans le TV Provider) alors que la catégorie « Apps sources » est active
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
