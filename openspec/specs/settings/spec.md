# settings Specification

## Purpose
Page de réglages du launcher : choix des apps sources du héro et du Top Shelf, applications cachées de la grille, version et licences.

## Requirements

### Requirement: Focus des réglages
Toute ligne focusable SHALL être une pilule claire à texte/icônes sombres, sans zoom, halo ni verre : catégories, apps, contenus, dépliage, Tout réactiver, visibilité/position et options, À propos. La catégorie active SHALL garder une pilule grise discrète au focus à droite.

#### Scenario: focus de ligne et catégorie
- **WHEN** le focus change de volet ou de ligne
- **THEN** « focus d'une ligne » et « catégorie active, focus à droite » de « Page de réglages » s'appliquent à tous les contrôles listés

### Requirement: Compteurs des sources
Les compteurs SHALL compter tous les PreviewPrograms et WatchNextPrograms par app dès l'accueil au lancement, non à l'ouverture des réglages, par l'unique observation existante sans seconde lecture. Elle SHALL réagir aux deux tables, s'arrêter en arrière-plan sans relecture, conserver ses valeurs puis recompter aussitôt au retour au premier plan.

#### Scenario: comptage partagé et cycle de vie
- **WHEN** le launcher démarre, passe en arrière-plan ou revient
- **THEN** les scénarios de compteurs de « Apps sources », y compris Watch Next seul et aucun compteur disponible, s'appliquent

### Requirement: Tri stable des sources
À chaque entrée de catégorie, les apps SHALL être triées : compteur > 0 décroissant, égalité par nom sans casse, puis zéros par nom. Sans premier comptage, alphabet puis un seul retri à son arrivée, focus par app, pas position. Ensuite les nombres seuls SHALL changer, ordre figé jusqu'à nouvelle entrée ; un aller-retour entre volets ne recalcule pas.

#### Scenario: compteurs en retard ou actualisés
- **WHEN** un compteur arrive dans la catégorie
- **THEN** « compteurs en retard », « compteurs mis à jour dans la catégorie » et « nouvel ordre à la prochaine entrée » de « Apps sources » s'appliquent sans saut sous le focus

### Requirement: Lignes des contenus retirés
Les contenus SHALL être triés par retrait décroissant, avec vignette 16:9/image persistée sinon placeholder, titre et « Série · <apps> » ou « Film · <apps> » (sources persistées encore installées séparées par virgules, type seul sinon). Seul le focus SHALL montrer « Réafficher » à droite. OK SHALL supprimer le retrait persisté et sa ligne ; retour en rangée au prochain chargement si encore publié.

#### Scenario: retrait réaffiché
- **WHEN** un contenu est réaffiché
- **THEN** « contenus retirés » et « réafficher un contenu » de « Applications cachées » s'appliquent sans action sur les switches d'apps

### Requirement: Groupes repliés
Chaque groupe SHALL montrer au plus 3 lignes, puis « Afficher les N autres » pour N masquées ou « Afficher l'autre ». OK SHALL afficher tout dans l'ordre, supprimer cette ligne et focaliser la 4e. Le dépliage ne SHALL pas être persisté et revient replié à chaque entrée de catégorie.

#### Scenario: dépliage et retour
- **WHEN** un groupe dépasse trois éléments
- **THEN** « groupe limité à 3 lignes », « dépliage d'un groupe » et « retour dans le volet » de « Applications cachées » s'appliquent

### Requirement: Liste des apps cachées
Les apps SHALL être triées par masquage décroissant, sans date en fin par nom sans casse. La liste SHALL être reconstruite depuis l'état persisté à chaque entrée de catégorie, avant la première image, jamais pour un aller-retour entre volets sans changer de catégorie.

#### Scenario: entrée et changement de volet
- **WHEN** les apps ont été réactivées ou recachées
- **THEN** « recalcul au retour dans la catégorie » et « pas de recalcul sans changer de catégorie » de « Applications cachées » préservent l'état attendu sans image d'une ancienne liste

### Requirement: Réactivation des apps cachées
Réactiver par switch ou Tout réactiver SHALL agir et persister immédiatement sur grille/dock, en gardant la ligne visible et son switch visible tant que catégorie active. Tout réactiver SHALL viser seulement les apps de la liste, repliées comprises, sans réafficher les contenus ; une cachée désinstallée non listée reste cachée, même réinstallée.

#### Scenario: réactivation et annulation locale
- **WHEN** une app ou la liste est réactivée
- **THEN** « réactivation », « recacher avant le recalcul », « tout réactiver » et « tout réactiver avec une app cachée désinstallée » de « Applications cachées » s'appliquent sans recalcul prématuré

### Requirement: Focus des éléments cachés
L'ordre SHALL être Tout réactiver, apps, dépliage apps, contenus, dépliage contenus, sans intertitre focusable. Initialement première ligne du premier groupe non vide, Haut atteignant Tout réactiver. Une ligne désinstallée/réaffichée SHALL disparaître ; si focusée, focus au successeur de sa position sinon dernier, puis catégorie si vide.

#### Scenario: disparition ou volet vide
- **WHEN** une ligne disparaît pendant l'affichage
- **THEN** les scénarios « app de la ligne focalisée désinstallée », « liste vidée pendant l'affichage » et « autre app désinstallée » de « Applications cachées » gardent la navigation sans boucler

### Requirement: Ligne de position fermée
Selon 7.7/7.8 et les composants « Ligne de réglage à valeur »/« Liste déroulante », la ligne SHALL montrer libellé à gauche, valeur `TextStyles.RowState` à droite, couleur tertiaire `SygixColors.OnDarkSecondary` au repos / `OnPillTertiary` au focus, puis chevron bas `Dimens.SettingsChevron` à `Dimens.SettingsChevronGap`, même couleur.

#### Scenario: ligne ouverte
- **WHEN** la liste est ouverte
- **THEN** la ligne prend `SygixColors.PillSelected` et son chevron pointe vers le haut, selon les scénarios de « Position d'À suivre »

### Requirement: Liste de position ouverte
Le panneau SHALL être `GlassSurface` / `GlassLook.Menu`, largeur `Dimens.DropdownWidth`, coins `Dimens.DropdownCorner`, padding `Dimens.DropdownPadding`, ancré `Dimens.DropdownOffset` sous la ligne et `Dimens.DropdownInsetEnd` en retrait du bord droit. Il SHALL couvrir le volet sans voile et présenter les deux valeurs dans leur ordre, selon « Options de position ».

#### Scenario: ancrage et matériau
- **WHEN** le menu de position est affiché
- **THEN** les scénarios de liste ouverte de « Position d'À suivre » gardent la géométrie et le matériau du menu contextuel

### Requirement: Options de position
Chaque option SHALL avoir hauteur `Dimens.DropdownOptionHeight`, coins `Dimens.DropdownOptionCorner` concentriques au panneau, écart `Dimens.DropdownOptionGap`, colonne gauche `Dimens.DropdownCheck` avec coche pour la seule valeur courante, texte `TextStyles.Row` / `RowFocused` au focus, pilule de « Page de réglages ».

#### Scenario: option courante et focus
- **WHEN** la liste de position est parcourue
- **THEN** « déplacements dans la liste » et les scénarios de validation/fermeture de « Position d'À suivre » gardent la coche et le focus dans la liste

### Requirement: État de la ligne launcher
État/détail SHALL être recalculés au retour au premier plan, à la fermeture du panneau et en fin de séquence. Le détail SHALL être « Automatique : <état> · Par autorisations : <état> », états en minuscules. À droite : « Actif » avec point vert si une voie active, sinon « Bloqué » si voie bloquée, sinon « En partie » si partielle, sinon « À configurer ».

#### Scenario: libellés des états des voies
- **WHEN** la ligne est relue
- **THEN** la voie automatique donne « actif », « à configurer » ou « indisponible sur cette TV », la voie par autorisations « active », « en partie », « bloquée », « non utilisée » ou « à faire », selon launcher-shell ; le premier contrôle reste « Afficher À suivre », et aucune autre ligne rôle HOME, allumage, accessibilité, débogage ou accueil d'origine n'est ajoutée

### Requirement: Capsule heure et réglages
Sur le héro en haut à droite, la capsule verre de « Thème » SHALL montrer heure puis engrenage au trait ouvrant les réglages. Seul l'engrenage SHALL être focusable. L'heure système 12/24 h SHALL changer par minute sans recomposer le reste. Solidaire du héro comme le dock, la capsule SHALL sortir/revenir avec lui sans fondu propre, invisible en grille, remplaçant l'ancien engrenage seul.

#### Scenario: affichage
- **WHEN** le héro est affiché
- **THEN** la capsule est visible en haut à droite avec l'heure puis l'engrenage, sans masquer le titre du héro et sans détourner le focus du héro à l'ouverture du home ; au repos l'engrenage est blanc sur une pastille claire discrète

#### Scenario: accès DPAD
- **WHEN** l'utilisateur presse haut depuis le héro
- **THEN** l'engrenage prend le focus : pastille blanche, icône noire, léger zoom et ombre ; l'heure ne prend jamais le focus ; bas depuis l'engrenage rend le focus au héro ; Retour depuis l'engrenage rend le focus au bouton d'ouverture du héro (au héro s'il n'en a pas) ; gauche et droite laissent le focus sur l'engrenage

#### Scenario: ouverture
- **WHEN** l'utilisateur presse OK sur l'engrenage
- **THEN** la page de réglages plein écran s'ouvre et remplace l'accueil selon « Page de réglages » ; la lecture du héro est mise en pause, une vidéo d'aperçu à sa position courante

#### Scenario: zone grille
- **WHEN** l'utilisateur passe en zone grille et la page de l'accueil défile du héro vers la grille (« Navigation 3 paliers » de `launcher-shell`)
- **THEN** la capsule suit exactement le mouvement du héro, sans fondu propre ni décalage, et sort par le haut avec lui ; à la fin du défilement elle est entièrement hors écran ; ni elle ni l'engrenage ne sont visibles ou focusables tant que la grille est affichée

#### Scenario: retour sur le héro
- **WHEN** l'utilisateur revient sur le héro depuis la grille (Retour, ou remontée par le dock)
- **THEN** la capsule revient par le haut avec le héro, dans le même mouvement et sans fondu propre, jusqu'à sa position initiale, sans que l'engrenage prenne le focus

#### Scenario: heure
- **WHEN** la minute change, ou que l'heure ou le fuseau du système change
- **THEN** l'heure affichée est mise à jour dans le format 12 ou 24 h du système, seule l'heure est recomposée ; un changement du format 12/24 h est pris en compte au plus tard à la minute suivante

#### Scenario: retour au premier plan
- **WHEN** le launcher revient au premier plan après un passage en arrière-plan
- **THEN** l'heure affichée redevient l'heure courante dès le retour, sans attendre le changement de minute ; aucune mise à jour n'a lieu tant que le launcher est en arrière-plan

### Requirement: Page de réglages
Les réglages SHALL être plein écran tvOS au DPAD : catégories à gauche, contenu à droite, fond de « Fond de la zone grille » de launcher-shell. L'accueil ne SHALL être ni dessiné ni focusable dessous, sans réinitialiser programme/visuel/vidéo du héro ni position grille. Les lignes SHALL suivre « Focus des réglages » ; le retour garde les scénarios ci-dessous.

#### Scenario: structure
- **WHEN** la page de réglages s'ouvre
- **THEN** le volet gauche liste les catégories « Apps sources », « Applications cachées », « Écran d'accueil », « À propos », le volet droit affiche le contenu de la catégorie active, la première catégorie porte le focus à l'ouverture

#### Scenario: changement de catégorie
- **WHEN** l'utilisateur presse haut/bas dans le volet gauche
- **THEN** la catégorie active change et le volet droit affiche son contenu ; droite depuis le volet gauche porte le focus sur le premier élément du volet droit (« Afficher À suivre » pour « Écran d'accueil »), sauf pour « Applications cachées » dont le focus initial est la première ligne de la liste (« Applications cachées », scénario « focus initial ») ; gauche depuis le volet droit le rend au volet gauche, sur la catégorie active, sauf quand la liste déroulante de « Position d'À suivre » est ouverte (« Position d'À suivre », scénario « déplacements dans la liste »)

#### Scenario: retour
- **WHEN** l'utilisateur presse Retour depuis la page de réglages
- **THEN** le home reprend avec le héro affiché et focusé (comportement standard), la lecture du héro reprend là où elle s'était arrêtée ; seule exception : quand la liste déroulante de « Position d'À suivre » est ouverte, Retour la ferme d'abord, sans quitter les réglages (« Position d'À suivre », scénario « fermeture sans changement »)

#### Scenario: accueil masqué
- **WHEN** la page de réglages est affichée
- **THEN** aucun élément de l'accueil (héro, capsule, dock, grille) n'est dessiné sous la page ni ne peut prendre le focus au DPAD ; le panneau du launcher système ouvert depuis les réglages reste affiché au-dessus d'eux ; son verre, comme celui de la liste déroulante de « Position d'À suivre », floute la page de réglages

#### Scenario: retour immédiat
- **WHEN** l'utilisateur quitte les réglages par Retour, ou par la touche Home au premier plan (« Touche Home avec SygixOs au premier plan » de `launcher-shell`)
- **THEN** le visuel, le titre et le bouton du héro affichés avant l'ouverture sont affichés immédiatement, sans mascotte ni écran de démarrage, sans fond noir et sans fondu d'entrée ; une vidéo d'aperçu reprend sans être rechargée ; si le launcher est passé en arrière-plan pendant les réglages, la vidéo, libérée entre-temps, est rechargée selon « Préchargement et mémoire » de `launcher-shell`

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
« Apps sources » SHALL lister toutes les apps TV installées avec switch Apple activé par défaut pour leur contribution au héro, Top Shelf et Up Next. La description SHALL être « Choisissez les applications qui alimentent le héro, le Top Shelf et À suivre. ». Seules les apps y figurent, les contenus retirés allant dans Applications cachées. Comptage et ordre SHALL suivre « Compteurs des sources » et « Tri stable des sources ».

#### Scenario: présentation
- **WHEN** la catégorie « Apps sources » est affichée
- **THEN** la description du volet est « Choisissez les applications qui alimentent le héro, le Top Shelf et À suivre. », aucune ligne de contenu retiré n'y figure, et chaque ligne montre l'icône de l'app, son nom et sous le nom des informations sur l'app dont le nombre de programmes publiés dans le TV Provider, avec à droite de la ligne un toggle switch

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

### Requirement: Applications cachées
Le volet SHALL afficher sans sous-écran une liste défilante : « Tout réactiver » puis apps cachées avec switch cachée/visible, puis « Retirés d'À suivre » non focusable et contenus (7.9). Un groupe vide et son en-tête/bouton SHALL être absents. Les règles SHALL suivre « Lignes des contenus retirés », « Groupes repliés », « Réactivation des apps cachées », « Liste des apps cachées » et « Focus des éléments cachés ».

#### Scenario: sous-écran
- **WHEN** l'utilisateur sélectionne ou valide « Applications cachées » dans le volet gauche
- **THEN** aucun sous-écran ne s'ouvre : le volet droit affiche le bouton « Tout réactiver » puis, en dessous, une ligne par app cachée (icône, nom, switch en position « cachée »), puis, s'il y a des contenus retirés, l'intertitre « Retirés d'À suivre » et leurs lignes ; aucun sous-écran ni palier supplémentaire n'est ouvert

#### Scenario: focus initial
- **WHEN** l'utilisateur presse droite depuis la catégorie « Applications cachées » alors que la liste contient au moins une app
- **THEN** la première ligne d'app prend le focus, pas le bouton « Tout réactiver » ; haut depuis cette ligne porte le focus sur le bouton ; sans app cachée mais avec des contenus retirés, la première ligne de contenu prend le focus

#### Scenario: tri des apps cachées
- **WHEN** des apps ont été cachées à des dates différentes et d'autres sans date (masquées avant ce change)
- **THEN** les apps datées viennent en premier, de la plus récente à la plus ancienne, puis les apps sans date par ordre alphabétique

#### Scenario: réactivation
- **WHEN** l'utilisateur presse OK sur la ligne d'une app cachée
- **THEN** le switch passe en position « visible » avec l'animation Apple, l'app réapparaît immédiatement dans la grille (ordre alphabétique par défaut si l'ordre précédent n'existe plus), l'état est persisté, la ligne reste affichée à sa place et garde le focus

#### Scenario: recacher avant le recalcul
- **WHEN** l'utilisateur presse à nouveau OK sur une ligne dont le switch est en position « visible » (app réactivée par erreur), sans avoir quitté la catégorie
- **THEN** l'app est cachée à nouveau (avec une nouvelle date de masquage), disparaît de la grille et du dock, le switch repasse en position « cachée », la ligne garde sa place et le focus

#### Scenario: tout réactiver
- **WHEN** l'utilisateur presse OK sur « Tout réactiver »
- **THEN** sans confirmation, toutes les apps de la liste réapparaissent dans la grille, l'état est persisté, toutes les lignes restent affichées avec leur switch en position « visible », le focus reste sur le bouton ; chaque ligne peut ensuite être recachée individuellement comme au scénario « recacher avant le recalcul »

#### Scenario: tout réactiver avec une app cachée désinstallée
- **WHEN** une app a été cachée puis désinstallée (elle n'apparaît donc pas dans la liste), et l'utilisateur presse OK sur « Tout réactiver »
- **THEN** seules les apps de la liste affichée sont réactivées ; l'app désinstallée reste cachée dans l'état persisté et, si elle est réinstallée, elle n'apparaît ni dans la grille ni dans le dock et figure de nouveau dans « Applications cachées » (scénario « réinstallation » de « Cacher une application »)

#### Scenario: recalcul au retour dans la catégorie
- **WHEN** l'utilisateur a réactivé une ou plusieurs apps, quitte la catégorie (haut ou bas vers une autre catégorie dans le volet gauche, ou Retour qui ferme les réglages), puis revient sur « Applications cachées »
- **THEN** dès la première image du volet, les apps réactivées n'y figurent plus, les apps cachées entre-temps y figurent, l'ordre suit « tri des apps cachées », et si la liste est vide l'état vide s'affiche

#### Scenario: pas de recalcul sans changer de catégorie
- **WHEN** l'utilisateur a réactivé une app puis presse gauche (focus sur la catégorie « Applications cachées » dans le volet gauche) et revient par droite
- **THEN** la liste n'est pas recalculée : la ligne de l'app réactivée est toujours affichée, switch en position « visible »

#### Scenario: aucune app cachée
- **WHEN** aucune app n'est cachée et aucun contenu n'est retiré d'À suivre au moment où la catégorie est affichée
- **THEN** le volet droit affiche uniquement un message d'état vide centré (aucune ligne, aucun bouton « Tout réactiver » rendu, aucun crash) ; droite depuis le volet gauche laisse le focus sur la catégorie, Retour ferme les réglages comme depuis toute la page

#### Scenario: navigation D-pad dans le volet
- **WHEN** le focus est dans le volet « Applications cachées »
- **THEN** haut et bas parcourent le bouton puis les lignes et les lignes « Afficher les N autres » dans l'ordre affiché, en sautant l'intertitre « Retirés d'À suivre », sans boucle aux bords (haut depuis le premier élément et bas depuis le dernier ne font rien) ; la liste défile pour garder l'élément focusé entièrement visible ; gauche rend le focus au volet gauche sur la catégorie « Applications cachées » ; Retour ferme les réglages (« Page de réglages », scénario « retour ») ; OK bascule l'élément focusé (switch d'app), réaffiche le contenu de la ligne ou déplie le groupe ; le focus initial en entrant dans le volet est la première ligne (scénario « focus initial »)

#### Scenario: app de la ligne focalisée désinstallée
- **WHEN** l'app de la ligne qui a le focus est désinstallée pendant que le volet est affiché, et qu'il reste au moins une ligne
- **THEN** sa ligne disparaît et le focus passe à la ligne qui occupe désormais sa position (la nouvelle dernière ligne si la ligne disparue était la dernière) ; haut, bas, gauche et Retour fonctionnent comme au scénario « navigation D-pad dans le volet »

#### Scenario: liste vidée pendant l'affichage
- **WHEN** la dernière app de la liste est désinstallée pendant que le volet a le focus
- **THEN** l'état vide s'affiche et le focus revient sur la catégorie « Applications cachées » du volet gauche ; gauche y laisse le focus, haut et bas changent de catégorie et Retour ferme les réglages

#### Scenario: autre app désinstallée
- **WHEN** l'app d'une ligne qui n'a pas le focus est désinstallée pendant l'affichage (focus sur une autre ligne ou sur « Tout réactiver »)
- **THEN** sa ligne disparaît et le focus ne bouge pas

#### Scenario: retour dans le volet
- **WHEN** l'utilisateur revient dans le volet depuis le volet gauche sans avoir changé de catégorie
- **THEN** l'ordre, les états des switches et les groupes dépliés sont ceux laissés à la sortie, et le focus va à la première ligne (scénario « focus initial »)

#### Scenario: contenus retirés
- **WHEN** la catégorie est affichée alors que des contenus ont été retirés d'À suivre
- **THEN** sous les apps cachées, l'intertitre « Retirés d'À suivre » précède une ligne par contenu, du plus récemment retiré au plus ancien, chacune avec sa vignette, son titre et « Série · <apps> » ou « Film · <apps> » ; l'action « Réafficher » n'apparaît que sur la ligne focusée

#### Scenario: réafficher un contenu
- **WHEN** l'utilisateur presse OK sur la ligne d'un contenu retiré
- **THEN** le contenu quitte l'ensemble persisté des contenus retirés, sa ligne disparaît, le focus passe à l'élément qui occupe sa position (sinon au dernier élément, sinon à la catégorie si le volet est vide), et le contenu revient dans la rangée Up Next au chargement suivant s'il est encore publié

#### Scenario: groupe limité à 3 lignes
- **WHEN** 5 apps sont cachées et 2 contenus retirés
- **THEN** le groupe des apps montre ses 3 premières lignes puis « Afficher les 2 autres », le groupe des contenus montre ses 2 lignes sans ligne de dépliage ; « Tout réactiver » réactive les 5 apps

#### Scenario: dépliage d'un groupe
- **WHEN** l'utilisateur presse OK sur « Afficher les N autres »
- **THEN** toutes les lignes du groupe s'affichent à leur place dans l'ordre du tri, la ligne « Afficher les N autres » disparaît et le focus est sur la 4e ligne du groupe ; à la prochaine entrée dans la catégorie, le groupe est de nouveau replié

#### Scenario: un seul élément masqué
- **WHEN** un groupe compte exactement 4 lignes
- **THEN** ses 3 premières lignes sont suivies de « Afficher l'autre »

#### Scenario: groupe des apps vide
- **WHEN** aucune app n'est cachée mais des contenus sont retirés
- **THEN** ni « Tout réactiver » ni ligne d'app n'est affiché ; l'intertitre « Retirés d'À suivre » et ses lignes forment tout le volet

### Requirement: À propos
La catégorie « À propos » SHALL afficher la version du launcher et les licences des bibliothèques open source utilisées. Elle SHALL aussi contenir les lignes de mise à jour du launcher, décrites par `self-update` (« Lignes de mise à jour dans À propos »), qui fixe leur contenu, leur place et leur navigation D-pad.

#### Scenario: contenu
- **WHEN** la catégorie « À propos » est affichée
- **THEN** la version de l'application et la liste des licences OSS sont visibles, navigables au DPAD

#### Scenario: lignes de mise à jour
- **WHEN** la catégorie « À propos » est affichée
- **THEN** les lignes de mise à jour de `self-update` sont visibles avant la liste des licences, avec le comportement que `self-update` spécifie

### Requirement: Catégorie « Écran d'accueil »
Avant À propos, `SettingsCategory.HOME_SCREEN` SHALL suivre 7.7 : titre « Écran d'accueil » en `headlineSmall`, description « Choisissez ce qu'affiche l'accueil de SygixOs. » en `bodyMedium` blanc 60 %, espacements des autres catégories. Les lignes SHALL garder `Dimens.SettingsRowCorner`, minimum `Dimens.SettingsRowHeight`, retraits Apps sources et focus standard ; afficher À suivre puis sa position, autres réglages après, sans changer le focus initial.

#### Scenario: en-tête du volet
- **WHEN** la catégorie « Écran d'accueil » est active
- **THEN** le volet droit affiche le titre « Écran d'accueil », la description « Choisissez ce qu'affiche l'accueil de SygixOs. », puis la ligne « Afficher À suivre » suivie de la ligne « Position d'À suivre », avec le même en-tête et les mêmes espacements que les autres catégories

#### Scenario: lignes de la catégorie
- **WHEN** une ligne de la catégorie est affichée, avec ou sans le focus
- **THEN** son rectangle a la même hauteur minimale et les mêmes retraits que les lignes d'« Apps sources », et son libellé est en `TextStyles.Row` au repos et `TextStyles.RowFocused` au focus

### Requirement: Afficher À suivre
« Afficher À suivre » SHALL être le premier contrôle : switch Apple identique aux préversions, libellé gauche/switch droite (7.7), activé par défaut, persisté DataStore. Il autorise sans forcer : contenu affichable et permission restent requis. Désactivé, rangée/titre SHALL disparaître de la navigation sans modifier programmes ni filtres. Gauche SHALL rendre les catégories, Retour fermer, haut/bas parcourir les contrôles selon « Page de réglages ».

#### Scenario: réglage activé
- **WHEN** « Afficher À suivre » est activé
- **THEN** la rangée est présentée selon son réglage de position et les programmes publiés par les apps sources actives

#### Scenario: réglage désactivé
- **WHEN** « Afficher À suivre » est désactivé
- **THEN** la rangée et son titre « À suivre » sont masqués et la navigation de la zone grille les saute ; le réglage reste conservé après redémarrage

#### Scenario: rangée indisponible malgré le réglage activé
- **WHEN** « Afficher À suivre » est activé, mais qu'aucun programme affichable n'est publié ou que la permission est refusée
- **THEN** la rangée reste masquée conformément à la capability up-next et la navigation de la zone grille la saute jusqu'à ce qu'elle soit effectivement affichée

#### Scenario: bascule
- **WHEN** l'utilisateur presse OK sur la ligne « Afficher À suivre »
- **THEN** le switch bascule avec l'animation Apple, l'état est persisté (DataStore), l'effet sur l'accueil est immédiat, sans redémarrage ; le focus reste sur la ligne

#### Scenario: navigation D-pad de la ligne
- **WHEN** le focus est sur « Afficher À suivre »
- **THEN** bas porte le focus sur « Position d'À suivre » ; haut ne fait rien (première ligne de la catégorie, sans boucle) ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » (« Page de réglages », scénario « changement de catégorie ») ; Retour ferme les réglages et le home reprend avec le héro focusé (« Page de réglages », scénario « retour »)

#### Scenario: focus initial de la catégorie
- **WHEN** l'utilisateur presse droite depuis la catégorie « Écran d'accueil » du volet des catégories
- **THEN** le focus est placé sur le premier contrôle de la catégorie, « Afficher À suivre », qui prend la pilule de focus des lignes (« Page de réglages », scénario « focus d'une ligne ») ; après gauche vers le volet puis droite, le focus revient sur « Afficher À suivre » et les valeurs des deux réglages sont inchangées

### Requirement: Position d'À suivre
Sous « Afficher À suivre », « Position d'À suivre » SHALL proposer « Avant les applications » par défaut et « Après les applications », persistés DataStore/restaurés au démarrage. Effet : grille/navigation de launcher-shell. Rendu SHALL suivre 7.7/7.8 et « Ligne de position fermée »/« Liste de position ouverte ». Le focus SHALL rester dans la liste ouverte ; fermée, navigation de « Page de réglages », sans animation supplémentaire au design system.

#### Scenario: contrôle fermé
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** la ligne « Position d'À suivre » affiche à droite la valeur courante, « Avant les applications » par défaut, en `TextStyles.RowState`, suivie d'un chevron vers le bas ; la valeur affichée est celle persistée dans DataStore, y compris après un redémarrage du launcher

#### Scenario: ouverture de la liste
- **WHEN** le focus est sur « Position d'À suivre » fermée et l'utilisateur presse OK
- **THEN** la liste s'ouvre dans son panneau verre sous la ligne, par-dessus le volet droit, avec « Avant les applications » puis « Après les applications », la valeur courante est cochée et a le focus ; la ligne du contrôle prend le fond `SygixColors.PillSelected` et son chevron pointe vers le haut tant que la liste est ouverte, et la catégorie active reste marquée par la pilule grise discrète (« Page de réglages »)

#### Scenario: déplacements dans la liste
- **WHEN** la liste est ouverte et l'utilisateur presse haut, bas, gauche ou droite
- **THEN** haut et bas déplacent le focus d'une valeur à l'autre, sans boucle aux bords (haut depuis la première valeur et bas depuis la dernière ne font rien) ; gauche et droite ne font rien ; le focus ne quitte jamais la liste tant qu'elle est ouverte

#### Scenario: choix d'une valeur
- **WHEN** la liste est ouverte et l'utilisateur presse OK sur une valeur
- **THEN** cette valeur devient la valeur du réglage, la liste se ferme, le focus revient sur la ligne « Position d'À suivre » qui affiche la nouvelle valeur ; la valeur est persistée dans DataStore et appliquée à l'accueil sans redémarrage ; OK sur la valeur déjà cochée ferme la liste sans rien changer

#### Scenario: fermeture sans changement
- **WHEN** la liste est ouverte et l'utilisateur presse Retour
- **THEN** la liste se ferme sans changer la valeur, le focus revient sur la ligne « Position d'À suivre » et les réglages restent ouverts

#### Scenario: états de focus
- **WHEN** le focus passe sur la ligne fermée, puis sur une valeur de la liste ouverte
- **THEN** la ligne fermée focusée est une pilule claire à texte, valeur et chevron sombres (« Page de réglages », scénario « focus d'une ligne ») ; dans la liste, la valeur focusée est une pilule claire à texte sombre, sans zoom ni halo, l'autre valeur n'a pas de fond, et la coche reste dans la colonne de gauche de la valeur courante, qu'elle ait le focus ou non ; seul le panneau de la liste est en verre, pas les pilules

#### Scenario: navigation D-pad du contrôle fermé
- **WHEN** le focus est sur « Position d'À suivre » fermée
- **THEN** haut porte le focus sur « Afficher À suivre » ; bas ne fait rien tant que « Position d'À suivre » est la dernière ligne de la catégorie (sans boucle) ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » ; Retour ferme les réglages et le home reprend avec le héro focusé (« Page de réglages », scénario « retour »)

#### Scenario: launcher en arrière-plan liste ouverte
- **WHEN** la liste est ouverte et le launcher passe en arrière-plan (touche Home de la télécommande ou autre app au premier plan) alors que les réglages restent ouverts, puis revient au premier plan
- **THEN** la liste est fermée sans changement de valeur, les réglages sont toujours ouverts, le focus est sur la ligne « Position d'À suivre » et celle-ci affiche la valeur persistée, inchangée

#### Scenario: réglages rouverts
- **WHEN** les réglages sont fermés puis rouverts après que la liste a été ouverte
- **THEN** la liste est fermée et la ligne affiche la valeur persistée, inchangée

#### Scenario: application immédiate
- **WHEN** la valeur du réglage change
- **THEN** la rangée Up Next et son titre sont repositionnés sans redémarrage du launcher, et la navigation suit le scénario « position après les applications » de « Navigation 3 paliers » (`launcher-shell`)

#### Scenario: « Afficher À suivre » désactivé
- **WHEN** « Afficher À suivre » est désactivé et le focus arrive sur « Position d'À suivre »
- **THEN** la ligne a le même rendu, le même focus et la même liste qu'avec la rangée affichée (jamais grisée ni sautée), la valeur choisie est persistée et s'applique dès que « Afficher À suivre » est réactivé

### Requirement: Réglages du launcher système
Après visibilité/position d'À suivre, « Launcher système » non focusable SHALL précéder la seule ligne « Configurer SygixOs comme launcher principal », ouvrant le panneau de launcher-shell (9.14/9.15/8.21) puis retrouvant le focus. Ces réglages ne SHALL figurer sur aucune autre ligne/catégorie. Focus sans zoom et D-pad standard SHALL sauter l'intertitre, sans boucle ni effet droite ; synthèse selon « État de la ligne launcher ».

#### Scenario: emplacement
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil »
- **THEN** le volet droit présente, dans cet ordre, « Afficher À suivre », « Position d'À suivre », l'intertitre « Launcher système » et « Configurer SygixOs comme launcher principal » avec son détail et son état ; le focus initial reste sur « Afficher À suivre » ; aucune ligne « SygixOs comme écran d'accueil », « Remplacer le launcher », « Démarrer à l'allumage » ou « Retour à l'accueil » n'existe, et aucune nouvelle catégorie n'apparaît dans le volet gauche

#### Scenario: navigation D-pad de la ligne P5
- **WHEN** le focus est sur « Position d'À suivre » (liste fermée) et l'utilisateur presse bas, puis bas encore, puis haut deux fois, puis bas, droite, puis gauche
- **THEN** le focus passe à « Configurer SygixOs comme launcher principal » (l'intertitre n'est jamais focalisé), puis y reste (dernière ligne, sans boucle) ; il remonte jusqu'à « Afficher À suivre », redescend ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » ; Retour depuis la ligne ferme les réglages (« Page de réglages », scénario « retour »)

#### Scenario: ouverture du parcours
- **WHEN** l'utilisateur presse OK sur « Configurer SygixOs comme launcher principal »
- **THEN** le panneau « Launcher principal » s'affiche par-dessus la page de réglages ; à sa fermeture, la page de réglages est toujours ouverte sur « Écran d'accueil » et la ligne a le focus avec son état et son détail recalculés

#### Scenario: synthèse partielle
- **WHEN** la voie automatique est à configurer et qu'une partie seulement des lignes de la voie par autorisations est faite
- **THEN** la ligne affiche l'état « En partie », sans pastille, et le détail « Automatique : à configurer · Par autorisations : en partie » (maquette 8.18)

#### Scenario: synthèse terminée
- **WHEN** la voie automatique est active et que la voie par autorisations n'est pas utilisée
- **THEN** la ligne affiche l'état « Actif » précédé d'une pastille verte et le détail « Automatique : actif · Par autorisations : non utilisée » (maquette 8.15)

#### Scenario: touche Home bloquée
- **WHEN** la voie par autorisations est bloquée et que la voie automatique est à configurer
- **THEN** la ligne affiche l'état « Bloqué », sans pastille, et le détail « Automatique : à configurer · Par autorisations : bloquée » (maquette 8.19)

#### Scenario: rien de fait
- **WHEN** aucune voie n'est utilisée
- **THEN** la ligne affiche l'état « À configurer », sans pastille, et le détail « Automatique : à configurer · Par autorisations : à faire » (maquette 8.20)

#### Scenario: état relu au retour
- **WHEN** SygixOs revient au premier plan après un écran système ouvert depuis le parcours, après tout passage en arrière-plan, ou à la fin d'une séquence de la configuration automatique
- **THEN** l'écran d'accueil effectif, l'accueil d'origine, le service d'accessibilité, la permission et le rôle HOME sont relus auprès du système et l'état et le détail de la ligne sont recalculés, sans valeur périmée
