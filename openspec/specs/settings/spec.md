# settings Specification

## Purpose
Page de réglages du launcher : choix des apps sources du héro et du Top Shelf, applications cachées de la grille, version et licences.

## Requirements

### Requirement: Capsule heure et réglages
Le home SHALL afficher sur le héro, en haut à droite, une capsule en verre sombre (« Thème » de `launcher-shell`) qui contient l'heure courante puis un engrenage dessiné au trait ouvrant la page de réglages ; elle remplace l'engrenage seul. L'heure SHALL suivre le format 12 ou 24 h choisi dans le système, n'est jamais focusable et SHALL se mettre à jour au changement de minute sans recomposer le reste de l'écran. Seul l'engrenage SHALL prendre le focus. La capsule SHALL être solidaire du héro, comme le dock (« Dock d'apps épinglées » de `launcher-shell`) : elle n'a aucun fondu propre, sort par le haut avec le héro pendant le défilement vers la grille, revient avec lui, et n'est jamais visible en vue grille.

#### Scenario: affichage
- **WHEN** le héro est affiché
- **THEN** la capsule est visible en haut à droite avec l'heure puis l'engrenage, sans masquer le titre du héro et sans détourner le focus du héro à l'ouverture du home ; au repos l'engrenage est blanc sur une pastille claire discrète

#### Scenario: accès DPAD
- **WHEN** l'utilisateur presse haut depuis le héro
- **THEN** l'engrenage prend le focus : pastille blanche, icône noire, léger zoom et ombre ; l'heure ne prend jamais le focus ; bas depuis l'engrenage rend le focus au héro ; Retour depuis l'engrenage rend le focus au bouton d'ouverture du héro (au héro s'il n'en a pas) ; gauche et droite laissent le focus sur l'engrenage

#### Scenario: ouverture
- **WHEN** l'utilisateur presse OK sur l'engrenage
- **THEN** la page de réglages plein écran s'ouvre, la lecture du héro est mise en pause

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
Le launcher SHALL offrir une page de réglages plein écran à la tvOS : volet catégories à gauche, contenu de la catégorie à droite, sur le même fond que la zone grille de l'accueil (« Fond de la zone grille » de `launcher-shell`), navigable au DPAD uniquement. Le focus de toute ligne focusable de la page (catégories, lignes d'« Apps sources » et d'« Applications cachées », lignes de contenus retirés et lignes « Afficher les N autres », bouton « Tout réactiver », lignes « Afficher À suivre » et « Position d'À suivre » et valeurs de la liste déroulante de « Position d'À suivre », lignes d'« À propos ») SHALL être une pilule claire à texte et icônes sombres, sans zoom, sans halo et sans matériau verre. La catégorie active SHALL rester marquée par une pilule grise discrète quand le focus est dans le volet droit.

#### Scenario: structure
- **WHEN** la page de réglages s'ouvre
- **THEN** le volet gauche liste les catégories « Apps sources », « Applications cachées », « Écran d'accueil », « À propos », le volet droit affiche le contenu de la catégorie active, la première catégorie porte le focus à l'ouverture

#### Scenario: changement de catégorie
- **WHEN** l'utilisateur presse haut/bas dans le volet gauche
- **THEN** la catégorie active change et le volet droit affiche son contenu ; droite depuis le volet gauche porte le focus sur le premier élément du volet droit (« Afficher À suivre » pour « Écran d'accueil »), sauf pour « Applications cachées » dont le focus initial est la première ligne de la liste (« Applications cachées », scénario « focus initial ») ; gauche depuis le volet droit le rend au volet gauche, sur la catégorie active, sauf quand la liste déroulante de « Position d'À suivre » est ouverte (« Position d'À suivre », scénario « déplacements dans la liste »)

#### Scenario: retour
- **WHEN** l'utilisateur presse Retour depuis la page de réglages
- **THEN** le home reprend avec le héro affiché et focusé (comportement standard), la lecture du héro reprend ; seule exception : quand la liste déroulante de « Position d'À suivre » est ouverte, Retour la ferme d'abord, sans quitter les réglages (« Position d'À suivre », scénario « fermeture sans changement »)

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
La catégorie « Apps sources » SHALL lister toutes les apps TV installées avec, pour chacune, un toggle switch (style Apple) activant sa contribution au héro, au Top Shelf et à la rangée Up Next ; par défaut toutes les apps sont activées. La description du volet SHALL être « Choisissez les applications qui alimentent le héro, le Top Shelf et À suivre. » et la catégorie SHALL ne lister que des apps : les contenus retirés d'À suivre sont listés dans « Applications cachées ». Le nombre affiché SHALL compter tous les programmes publiés par chaque app dans le TV Provider, `PreviewPrograms` et `WatchNextPrograms` compris. La liste SHALL être triée : d'abord les apps dont le nombre de programmes publiés affiché est supérieur à 0, par nombre décroissant et, à nombre égal, par ordre alphabétique de leur nom (insensible à la casse), puis les apps à 0, par ordre alphabétique de leur nom (insensible à la casse). Les compteurs de programmes par app SHALL être calculés dès l'affichage de l'accueil au lancement du launcher, et non à l'ouverture des réglages, par l'unique observation existante du TV Provider (sans seconde lecture), qui SHALL réagir aux changements des `PreviewPrograms` comme des `WatchNextPrograms`. Cette observation SHALL être mise en pause quand le launcher passe en arrière-plan (aucune relecture du TV Provider pour les compteurs tant qu'une autre app est au premier plan) et un nouveau comptage SHALL être lancé à chaque retour du launcher au premier plan (décision de Sygix). Cet ordre SHALL être calculé à l'entrée dans la catégorie (ouverture des réglages ou passage depuis une autre catégorie) avec les compteurs connus à ce moment. Si aucun comptage n'est encore arrivé à l'entrée, la liste SHALL s'afficher d'abord par ordre alphabétique, puis être retriée une seule fois, à l'arrivée du premier comptage, le focus restant sur l'app qui l'avait (et non sur la position). L'ordre SHALL ensuite rester figé tant que la catégorie reste active : les compteurs affichés se mettent à jour sur place, mais les lignes ne changent plus de position sous le focus. Un aller-retour entre le volet gauche et le volet droit sans changer de catégorie ne recalcule pas l'ordre.

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
La catégorie « Applications cachées » SHALL afficher directement dans le volet de droite, dans une même liste défilante, deux groupes : d'abord la liste des apps cachées, chacune avec un toggle switch reflétant son état (cachée / visible), précédée d'un bouton « Tout réactiver » placé au-dessus de la liste ; puis le groupe « Retirés d'À suivre », sous un intertitre non focusable, qui liste les contenus retirés de la rangée Up Next (« Contenus retirés d'À suivre » de up-next ; maquette Penpot 7.9). Un groupe vide n'est pas affiché (ni son intertitre, ni « Tout réactiver » pour les apps) ; aucun sous-écran n'est ouvert. Chaque ligne de contenu retiré SHALL montrer la vignette 16:9 des lignes d'apps avec l'image persistée du contenu (placeholder à défaut), son titre et le détail « Série · <apps> » ou « Film · <apps> » (noms des apps sources persistées encore installées, séparés par des virgules ; « Série » ou « Film » seul si aucune ne l'est), et, sur la ligne focusée seulement, l'action « Réafficher » à droite ; les contenus SHALL être triés par instant du retrait décroissant. OK sur une ligne de contenu SHALL retirer ce contenu de l'ensemble des contenus retirés, de façon persistée, la ligne disparaissant aussitôt ; le contenu revient dans la rangée Up Next au chargement suivant s'il est encore publié. Chaque groupe SHALL montrer au plus 3 lignes : au-delà, ses 3 premières lignes sont suivies d'une ligne « Afficher les N autres » (N lignes masquées ; « Afficher l'autre » pour une seule) dont OK déplie le groupe, toutes ses lignes s'affichant, la ligne « Afficher les N autres » disparaissant et le focus passant à la 4e ligne du groupe ; le dépliage n'est jamais persisté et revient replié à chaque entrée dans la catégorie. La liste des apps SHALL être triée par date de masquage décroissante (la plus récemment cachée en premier), les apps cachées sans date étant placées en fin de liste par ordre alphabétique de leur nom (insensible à la casse). Réactiver une app (par son switch ou par « Tout réactiver ») SHALL agir immédiatement sur la grille et le dock et être persisté, mais SHALL laisser sa ligne affichée dans le volet, switch en position « visible », tant que la catégorie reste active. « Tout réactiver » SHALL ne réactiver que les apps de la liste affichée, lignes repliées comprises (décision de Sygix), et ne SHALL pas réafficher de contenu retiré : une app cachée puis désinstallée n'est pas listée et reste cachée, y compris à sa réinstallation. La liste SHALL être recalculée à partir de l'état persisté à chaque entrée dans la catégorie (passage depuis une autre catégorie ou ouverture des réglages), avant son premier affichage : aucune image ne montre l'ancienne liste ; passer du volet droit au volet gauche sans changer de catégorie ne la recalcule pas. Si l'app d'une ligne disparaît pendant l'affichage (désinstallée), ou qu'une ligne de contenu est réaffichée, sa ligne SHALL disparaître ; si cette ligne avait le focus, le focus SHALL passer à l'élément qui occupe désormais sa position dans l'ordre de focus du volet (le nouveau dernier élément si c'était le dernier), et si le volet devient vide, le focus SHALL revenir sur la catégorie « Applications cachées » du volet gauche (décision de Sygix). L'ordre de focus du volet est : « Tout réactiver », lignes d'apps affichées, « Afficher les N autres » des apps, lignes de contenus affichées, « Afficher les N autres » des contenus ; l'intertitre n'est jamais focalisé. Le focus initial du volet SHALL être la première ligne du premier groupe non vide ; « Tout réactiver » est atteint par Haut depuis la première ligne d'app (exception assumée à la règle de « Page de réglages » qui porte le focus sur le premier élément du volet droit).

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
Le volet droit de la catégorie « Écran d'accueil » (`SettingsCategory.HOME_SCREEN`, placée avant « À propos » dans le volet des catégories) SHALL reprendre l'en-tête des autres catégories, conformément à la maquette Penpot validée (« SygixOs Maquette », page « TV », écran 7.7) : le titre « Écran d'accueil » en `headlineSmall`, puis la description « Choisissez ce qu'affiche l'accueil de SygixOs. » en `bodyMedium` blanc à 60 %, avec les mêmes espacements que « Apps sources » et « Applications cachées », puis les lignes de réglage. Les lignes SHALL avoir la forme des lignes des réglages (`Dimens.SettingsRowCorner`), une hauteur minimale `Dimens.SettingsRowHeight` et les retraits des lignes d'« Apps sources », avec la pilule de focus de « Page de réglages » ; « Afficher À suivre » est la première, « Position d'À suivre » la suit ; les réglages ajoutés par d'autres changes viennent après, sans changer ce focus initial.

#### Scenario: en-tête du volet
- **WHEN** la catégorie « Écran d'accueil » est active
- **THEN** le volet droit affiche le titre « Écran d'accueil », la description « Choisissez ce qu'affiche l'accueil de SygixOs. », puis la ligne « Afficher À suivre » suivie de la ligne « Position d'À suivre », avec le même en-tête et les mêmes espacements que les autres catégories

#### Scenario: lignes de la catégorie
- **WHEN** une ligne de la catégorie est affichée, avec ou sans le focus
- **THEN** son rectangle a la même hauteur minimale et les mêmes retraits que les lignes d'« Apps sources », et son libellé est en `TextStyles.Row` au repos et `TextStyles.RowFocused` au focus

### Requirement: Afficher À suivre
La catégorie « Écran d'accueil » SHALL proposer, en première ligne, le réglage « Afficher À suivre » : une ligne avec un toggle switch (style Apple) identique à la ligne « Inclure les préversions » d'« À propos » (libellé à gauche, switch à droite ; maquette Penpot 7.7), activée par défaut et persistée dans DataStore. Ce réglage autorise l'affichage sans le forcer : la rangée n'est visible que si elle dispose de contenu affichable et que la permission requise est accordée. Lorsqu'il est désactivé, la rangée et son titre sont masqués et ignorés par la navigation (« Rangée Up Next » et « Navigation 3 paliers » de `launcher-shell`), sans modifier les programmes publiés ni les filtres des apps sources. La navigation D-pad de la ligne SHALL suivre « Page de réglages » : gauche rend le focus au volet des catégories, Retour ferme les réglages, haut et bas passent d'un contrôle de la catégorie à l'autre.

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
La catégorie « Écran d'accueil » SHALL proposer, sous « Afficher À suivre », le contrôle « Position d'À suivre » : une liste déroulante à deux valeurs, « Avant les applications » (défaut) et « Après les applications », persistée dans DataStore et restaurée au démarrage ; l'effet sur la zone grille est spécifié par « Grille d'apps » et « Navigation 3 paliers » de `launcher-shell`. Son rendu SHALL suivre les maquettes Penpot validées (« SygixOs Maquette », page « TV », écrans 7.7 et 7.8 ; composants « Ligne de réglage à valeur » et « Liste déroulante » de la page « Composants »). Fermée, la ligne affiche le libellé « Position d'À suivre » à gauche et, à droite, la valeur courante en `TextStyles.RowState` dans la couleur tertiaire de la pilule (`SygixColors.OnDarkSecondary` au repos, `OnPillTertiary` au focus) suivie, à `Dimens.SettingsChevronGap`, d'un chevron vers le bas de `Dimens.SettingsChevron` de la même couleur. Ouverte, la ligne SHALL prendre le fond `SygixColors.PillSelected` et son chevron SHALL pointer vers le haut ; la liste SHALL présenter les deux valeurs dans cet ordre dans un panneau verre `GlassSurface` au look `GlassLook.Menu` (le matériau du menu contextuel), de largeur `Dimens.DropdownWidth`, coins `Dimens.DropdownCorner`, padding `Dimens.DropdownPadding`, ancré `Dimens.DropdownOffset` sous la ligne avec son bord droit à `Dimens.DropdownInsetEnd` à l'intérieur du bord droit de la ligne, dessiné par-dessus le volet droit, sans voile ; chaque option est une ligne de `Dimens.DropdownOptionHeight` aux coins `Dimens.DropdownOptionCorner` (concentriques avec le panneau), séparée de la suivante par `Dimens.DropdownOptionGap`, avec à gauche une colonne de `Dimens.DropdownCheck` qui porte une coche sur la valeur courante seulement, puis le libellé en `TextStyles.Row` (`RowFocused` au focus) ; la pilule de focus est celle de « Page de réglages ». Aucune animation n'est imposée au-delà de celles du design system. Tant que la liste est ouverte, le focus SHALL y rester. La navigation D-pad de la ligne fermée SHALL suivre « Page de réglages », comme « Afficher À suivre ».

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
La catégorie « Écran d'accueil » SHALL contenir, après « Afficher À suivre » et « Position d'À suivre » et sans changer son focus initial (« Afficher À suivre » reste son premier contrôle), un intertitre « Launcher système » jamais focusable puis une seule ligne de réglage, « SygixOs comme écran d'accueil », qui ouvre le parcours guidé par-dessus la page de réglages (« Parcours guidé du launcher » de `launcher-shell`) ; à la fermeture du parcours, le focus SHALL revenir sur cette ligne. Aucune autre ligne P5 (rôle HOME, démarrage à l'allumage, service d'accessibilité) et aucune autre catégorie ne SHALL porter ces réglages : ils ne se règlent que dans le parcours guidé. La ligne SHALL avoir le rendu des lignes de « Page de réglages » (pilule de focus, sans zoom) et SHALL résumer l'état du parcours, recalculé à chaque retour au premier plan et à chaque fermeture du parcours, à partir des étapes obligatoires affichées et non « Indisponible » (étapes 1, 2 et 2b ; l'étape 3, facultative, n'entre jamais dans la synthèse) : état « Terminé », précédé d'une pastille ronde verte, et détail « Toutes les étapes sont faites » quand elles sont toutes « Fait » ; état « Bloqué » et détail « Touche Home bloquée par la TV » quand l'étape 1 est « Bloqué », quel que soit le reste ; sinon état « Partiel » quand au moins une étape est « Fait », « À faire » quand aucune ne l'est, avec le détail de ce qui reste : « Touche Home à terminer » (étape 1), « Démarrage à l'allumage à terminer » (étape 2 désactivée ou 2b non faite), ou « Touche Home et démarrage à l'allumage à terminer » ; état « Indisponible », sans pastille, avec le détail « Cette TV ne permet pas ces réglages », quand aucune étape obligatoire n'est disponible (OK ouvre quand même le parcours). Navigation D-pad : haut et bas parcourent toutes les lignes de la catégorie sans boucle (« Afficher À suivre », « Position d'À suivre », « SygixOs comme écran d'accueil »), l'intertitre étant sauté ; droite est sans effet ; gauche et Retour se comportent comme pour « Afficher À suivre ». Dimensions et jetons : maquette Penpot 8.5 et `design.md` (D10).

#### Scenario: emplacement
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil »
- **THEN** le volet droit présente, dans cet ordre, « Afficher À suivre », « Position d'À suivre », l'intertitre « Launcher système » et « SygixOs comme écran d'accueil » avec son détail et son état ; le focus initial reste sur « Afficher À suivre » ; aucune ligne « Remplacer le launcher », « Démarrer à l'allumage » ou « Retour à l'accueil » n'existe, et aucune nouvelle catégorie n'apparaît dans le volet gauche

#### Scenario: navigation D-pad de la ligne P5
- **WHEN** le focus est sur « Position d'À suivre » (liste fermée) et l'utilisateur presse bas, puis bas encore, puis haut deux fois, puis bas, droite, puis gauche
- **THEN** le focus passe à « SygixOs comme écran d'accueil » (l'intertitre n'est jamais focalisé), puis y reste (dernière ligne, sans boucle) ; il remonte jusqu'à « Afficher À suivre », redescend ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » ; Retour depuis la ligne ferme les réglages (« Page de réglages », scénario « retour »)

#### Scenario: ouverture du parcours
- **WHEN** l'utilisateur presse OK sur « SygixOs comme écran d'accueil »
- **THEN** le parcours guidé s'affiche par-dessus la page de réglages (maquette 8.6) ; à sa fermeture (« Terminé » ou Retour), il disparaît, la page de réglages est toujours ouverte sur « Écran d'accueil » et la ligne a le focus avec sa synthèse recalculée

#### Scenario: synthèse partielle
- **WHEN** l'étape 1 est « Fait » et que l'option de démarrage est désactivée
- **THEN** la ligne affiche l'état « Partiel », sans pastille, et le détail « Démarrage à l'allumage à terminer » (maquette 8.5)

#### Scenario: synthèse terminée
- **WHEN** l'étape 1 est « Fait », l'option de démarrage activée et la sous-étape 2b « Fait » ou absente, quel que soit l'état du rôle HOME
- **THEN** la ligne affiche l'état « Terminé » précédé d'une pastille verte et le détail « Toutes les étapes sont faites »

#### Scenario: touche Home bloquée
- **WHEN** l'étape 1 est « Bloqué »
- **THEN** la ligne affiche l'état « Bloqué », sans pastille, et le détail « Touche Home bloquée par la TV », quels que soient les autres états

#### Scenario: rien de fait
- **WHEN** aucune étape obligatoire n'est « Fait »
- **THEN** la ligne affiche l'état « À faire » et le détail « Touche Home et démarrage à l'allumage à terminer » (ou la seule étape disponible qui reste)

#### Scenario: état relu au retour
- **WHEN** SygixOs revient au premier plan après un écran système ouvert depuis le parcours, ou après tout passage en arrière-plan
- **THEN** l'état du service d'accessibilité, de la permission et du rôle HOME est relu auprès du système et la synthèse de la ligne est recalculée, sans valeur périmée
