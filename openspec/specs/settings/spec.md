# settings Specification

## Purpose
Page de réglages du launcher : choix des apps sources du héro et du Top Shelf, applications cachées de la grille, version et licences.

## Requirements

### Requirement: Icône réglages flottante
Le home SHALL afficher sur le héro une icône engrenage flottante en haut à droite, qui ouvre la page de réglages. L'icône SHALL être un engrenage plein façon tvOS, blanc opaque, dessiné sans fond, sans bordure ni matériau verre. L'engrenage SHALL être solidaire du héro, comme le dock (« Dock d'apps épinglées » de `launcher-shell`) : il n'a aucun fondu propre, sort par le haut avec le héro pendant le défilement vers la grille, revient avec lui, et il n'est jamais visible en vue grille.

#### Scenario: affichage
- **WHEN** le héro est affiché
- **THEN** l'engrenage est visible en haut à droite, blanc opaque, sans fond, sans bordure ni verre, sans masquer le héro et sans détourner le focus du héro à l'ouverture du home

#### Scenario: accès DPAD
- **WHEN** l'utilisateur presse haut depuis le héro
- **THEN** l'icône réglages prend le focus avec l'état focus net (zoom + halo, cohérent avec le focus tvOS du launcher) ; bas depuis l'icône rend le focus au héro

#### Scenario: ouverture
- **WHEN** l'utilisateur presse OK sur l'icône réglages
- **THEN** la page de réglages plein écran s'ouvre, la lecture du héro est mise en pause

#### Scenario: zone grille
- **WHEN** l'utilisateur passe en zone grille et la page de l'accueil défile du héro vers la grille (« Navigation 3 paliers » de `launcher-shell`)
- **THEN** l'engrenage suit exactement le mouvement du héro, sans fondu propre ni décalage, et sort par le haut avec lui ; à la fin du défilement il est entièrement hors écran ; il n'est ni visible ni focusable tant que la grille est affichée

#### Scenario: retour sur le héro
- **WHEN** l'utilisateur revient sur le héro depuis la grille (Retour, ou remontée par le dock)
- **THEN** l'engrenage revient par le haut avec le héro, dans le même mouvement et sans fondu propre, jusqu'à sa position initiale, sans prendre le focus

### Requirement: Page de réglages
Le launcher SHALL offrir une page de réglages plein écran à la tvOS : volet catégories à gauche, contenu de la catégorie à droite, fond sombre neutre, navigable au DPAD uniquement.

#### Scenario: structure
- **WHEN** la page de réglages s'ouvre
- **THEN** le volet gauche liste les catégories « Apps sources », « Applications cachées », « À propos », le volet droit affiche le contenu de la catégorie active, la première catégorie porte le focus à l'ouverture

#### Scenario: changement de catégorie
- **WHEN** l'utilisateur presse haut/bas dans le volet gauche
- **THEN** la catégorie active change et le volet droit affiche son contenu ; droite depuis le volet gauche porte le focus sur le premier élément du volet droit, sauf pour « Applications cachées » dont le focus initial est la première ligne de la liste (« Applications cachées », scénario « focus initial ») ; gauche depuis le volet droit le rend au volet gauche

#### Scenario: retour
- **WHEN** l'utilisateur presse Retour depuis la page de réglages
- **THEN** le home reprend avec le héro affiché et focusé (comportement standard), la lecture du héro reprend

### Requirement: Apps sources
La catégorie « Apps sources » SHALL lister toutes les apps TV installées avec, pour chacune, un toggle switch (style Apple) activant sa contribution au héro et au Top Shelf ; par défaut toutes les apps sont activées. La liste SHALL être triée : d'abord les apps dont le nombre de programmes publiés affiché est supérieur à 0, par nombre décroissant et, à nombre égal, par ordre alphabétique de leur nom (insensible à la casse), puis les apps à 0, par ordre alphabétique de leur nom (insensible à la casse). Les compteurs de programmes par app SHALL être calculés dès l'affichage de l'accueil au lancement du launcher, et non à l'ouverture des réglages, par l'unique observation existante du TV Provider (sans seconde lecture). Cette observation SHALL être mise en pause quand le launcher passe en arrière-plan (aucune relecture du TV Provider pour les compteurs tant qu'une autre app est au premier plan) et un nouveau comptage SHALL être lancé à chaque retour du launcher au premier plan (décision de Sygix). Cet ordre SHALL être calculé à l'entrée dans la catégorie (ouverture des réglages ou passage depuis une autre catégorie) avec les compteurs connus à ce moment. Si aucun comptage n'est encore arrivé à l'entrée, la liste SHALL s'afficher d'abord par ordre alphabétique, puis être retriée une seule fois, à l'arrivée du premier comptage, le focus restant sur l'app qui l'avait (et non sur la position). L'ordre SHALL ensuite rester figé tant que la catégorie reste active : les compteurs affichés se mettent à jour sur place, mais les lignes ne changent plus de position sous le focus. Un aller-retour entre le volet gauche et le volet droit sans changer de catégorie ne recalcule pas l'ordre.

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
- **THEN** le switch bascule avec l'animation Apple, l'effet est immédiat : les programmes de l'app disparaissent ou réapparaissent dans le héro et le Top Shelf sans redémarrage, et l'état est persisté (DataStore) ; la bascule ne change pas la position de la ligne (le tri ne dépend pas de l'état du switch) et le focus reste sur la ligne

### Requirement: Cacher une application
Le menu contextuel d'une tuile SHALL offrir une option « Cacher » en plus des actions existantes (épingler/retirer du dock, déplacer) ; une app cachée disparaît de la grille et du dock. Chaque masquage SHALL être daté (instant du masquage) et cette date SHALL être persistée avec l'app cachée ; les apps cachées avant l'introduction de la date restent cachées, sans date.

#### Scenario: action cacher
- **WHEN** l'utilisateur choisit « Cacher » dans le menu contextuel (appui long sur OK)
- **THEN** l'app disparaît de la grille et du dock (épinglage éventuel retiré), sans confirmation supplémentaire, et l'état est persisté (DataStore) avec la date du masquage

#### Scenario: portée du masquage
- **WHEN** une app est cachée
- **THEN** elle n'apparaît plus ni dans la grille ni dans le dock ; sa contribution au héro et au Top Shelf reste régie uniquement par les apps sources

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
La catégorie « Applications cachées » SHALL afficher directement dans le volet de droite la liste des apps cachées, chacune avec un toggle switch reflétant son état (cachée / visible), précédée d'un bouton « Tout réactiver » placé au-dessus de la liste ; aucun sous-écran n'est ouvert. La liste SHALL être triée par date de masquage décroissante (la plus récemment cachée en premier), les apps cachées sans date étant placées en fin de liste par ordre alphabétique de leur nom (insensible à la casse). Réactiver une app (par son switch ou par « Tout réactiver ») SHALL agir immédiatement sur la grille et le dock et être persisté, mais SHALL laisser sa ligne affichée dans le volet, switch en position « visible », tant que la catégorie reste active. « Tout réactiver » SHALL ne réactiver que les apps de la liste affichée (décision de Sygix) : une app cachée puis désinstallée n'est pas listée et reste cachée, y compris à sa réinstallation. La liste SHALL être recalculée à partir de l'état persisté à chaque entrée dans la catégorie (passage depuis une autre catégorie ou ouverture des réglages), avant son premier affichage : aucune image ne montre l'ancienne liste ; passer du volet droit au volet gauche sans changer de catégorie ne la recalcule pas. Si l'app d'une ligne disparaît pendant l'affichage (désinstallée), sa ligne SHALL disparaître ; si cette ligne avait le focus, le focus SHALL passer à la ligne qui occupe désormais sa position (la nouvelle dernière ligne si c'était la dernière), et si la liste devient vide, le focus SHALL revenir sur la catégorie « Applications cachées » du volet gauche (décision de Sygix). Le focus initial du volet SHALL être la première ligne ; « Tout réactiver » est atteint par Haut depuis la première ligne (exception assumée à la règle de « Page de réglages » qui porte le focus sur le premier élément du volet droit).

#### Scenario: sous-écran
- **WHEN** l'utilisateur sélectionne ou valide « Applications cachées » dans le volet gauche
- **THEN** aucun sous-écran ne s'ouvre : le volet droit affiche le bouton « Tout réactiver » puis, en dessous, une ligne par app cachée (icône, nom, switch en position « cachée ») ; aucun sous-écran ni palier supplémentaire n'est ouvert

#### Scenario: focus initial
- **WHEN** l'utilisateur presse droite depuis la catégorie « Applications cachées » alors que la liste contient au moins une app
- **THEN** la première ligne de la liste prend le focus, pas le bouton « Tout réactiver » ; haut depuis cette ligne porte le focus sur le bouton

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
- **WHEN** aucune app n'est cachée au moment où la catégorie est affichée
- **THEN** le volet droit affiche uniquement un message d'état vide centré (aucune ligne, aucun bouton « Tout réactiver » rendu, aucun crash) ; droite depuis le volet gauche laisse le focus sur la catégorie, Retour ferme les réglages comme depuis toute la page

#### Scenario: navigation D-pad dans le volet
- **WHEN** le focus est dans le volet « Applications cachées »
- **THEN** haut et bas parcourent le bouton puis les lignes dans l'ordre affiché, sans boucle aux bords (haut depuis le bouton et bas depuis la dernière ligne ne font rien) ; la liste défile pour garder l'élément focusé entièrement visible ; gauche rend le focus au volet gauche sur la catégorie « Applications cachées » ; Retour ferme les réglages (« Page de réglages », scénario « retour ») ; OK bascule l'élément focusé ; le focus initial en entrant dans le volet est la première ligne (scénario « focus initial »)

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
- **THEN** l'ordre et les états des switches sont ceux laissés à la sortie, et le focus va à la première ligne (scénario « focus initial »)

### Requirement: À propos
La catégorie « À propos » SHALL afficher la version du launcher et les licences des bibliothèques open source utilisées.

#### Scenario: contenu
- **WHEN** la catégorie « À propos » est affichée
- **THEN** la version de l'application et la liste des licences OSS sont visibles, navigables au DPAD
