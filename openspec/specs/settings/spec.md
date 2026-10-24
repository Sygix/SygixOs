# settings Specification

## Purpose
Page de réglages du launcher : choix des apps sources du héro et du Top Shelf, applications cachées de la grille, version et licences.

## Requirements

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
Les réglages SHALL conserver deux volets plein écran, leur fond et leurs pilules sans zoom, accueil masqué sans perdre son état. Une catégorie Suivi de visionnage SHALL accueillir le compte et ses réglages. Les panneaux ouverts SHALL recevoir Retour avant la page ; navigation et restauration SHALL conserver les comportements existants.

#### Scenario: structure
- **WHEN** les réglages s'ouvrent
- **THEN** les catégories sont Apps sources, Applications cachées, Écran d'accueil, Suivi de visionnage et À propos ; la première a le focus et son contenu est à droite

#### Scenario: changement de catégorie
- **WHEN** haut/bas est pressé à gauche puis droite
- **THEN** la catégorie active et son contenu changent ; droite atteint le premier contrôle du volet, sauf la règle existante de première ligne d'Applications cachées ; gauche revient à la catégorie sauf panneau ou liste actif

#### Scenario: retour
- **WHEN** Retour est pressé
- **THEN** une liste ou un panneau actif se ferme d'abord avec restauration de son contrôle ; sinon les réglages ferment sur le héro focusé et sa lecture reprend à la position conservée

#### Scenario: accueil masqué
- **WHEN** les réglages sont affichés
- **THEN** aucun élément du home n'est dessiné ni focusable dessous ; panneaux et listes utilisent les réglages comme arrière-plan, selon leur matériau

#### Scenario: retour immédiat
- **WHEN** les réglages sont quittés par Retour ou Home au premier plan
- **THEN** visuel, titre et bouton du héro reviennent sans mascotte, écran noir ou fondu d'entrée ; sa vidéo reprend sans rechargement sauf libération pendant un passage en arrière-plan

#### Scenario: fond
- **WHEN** les réglages s'ouvrent
- **THEN** leur fond reste celui de la zone grille

#### Scenario: focus d'une ligne
- **WHEN** une ligne prend le focus
- **THEN** elle devient une pilule claire à texte et icônes sombres sans zoom, halo ou verre, sans changer son rectangle ; les lignes au repos n'ont pas de fond, sauf Tout réactiver qui garde son fond discret

#### Scenario: catégorie active, focus à droite
- **WHEN** le focus est à droite
- **THEN** la catégorie active garde sa pilule grise et son texte blanc, les autres catégories sans fond

#### Scenario: vignette des lignes d'apps
- **WHEN** une ligne d'app est affichée
- **THEN** elle garde la vignette 16:9, bannière sinon icône centrée ; le fournisseur de suivi n'est pas ajouté à la liste des applications

**Détails.** — Le focus des catégories, lignes, contenus, dépliages, contrôles et actions reprend la pilule claire sans zoom, halo ou verre, et l'état marqué de la catégorie active. Les parcours de connexion et pays respectent cette base et leur navigation propre ; les autres catégories ne changent pas de règles.

### Requirement: Apps sources
« Apps sources » SHALL lister toutes les apps TV installées avec un switch Apple activé par défaut pour leur contribution au héro, Top Shelf et Up Next, description « Choisissez les applications qui alimentent le héro, le Top Shelf et À suivre. ». Seules les apps y figurent. Les compteurs SHALL couvrir tous leurs programmes du TV Provider, avec une seule observation dès l'accueil, en pause hors premier plan et recomptage au retour ; tri, focus et comptage SHALL suivre les scénarios ci-dessous.

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

**Détails.** — Compteurs des sources : Les compteurs SHALL compter tous les PreviewPrograms et WatchNextPrograms par app dès l'accueil au lancement, non à l'ouverture des réglages, par l'unique observation existante sans seconde lecture. Elle SHALL réagir aux deux tables, s'arrêter en arrière-plan sans relecture, conserver ses valeurs puis recompter aussitôt au retour au premier plan. Cas « comptage partagé et cycle de vie » : quand le launcher démarre, passe en arrière-plan ou revient, les scénarios de compteurs de « Apps sources », y compris Watch Next seul et aucun compteur disponible, s'appliquent. — Tri stable des sources : À chaque entrée de catégorie, les apps SHALL être triées : compteur > 0 décroissant, égalité par nom sans casse, puis zéros par nom. Sans premier comptage, alphabet puis un seul retri à son arrivée, focus par app, pas position. Ensuite les nombres seuls SHALL changer, ordre figé jusqu'à nouvelle entrée ; un aller-retour entre volets ne recalcule pas. Cas « compteurs en retard ou actualisés » : quand un compteur arrive dans la catégorie, « compteurs en retard », « compteurs mis à jour dans la catégorie » et « nouvel ordre à la prochaine entrée » de « Apps sources » s'appliquent sans saut sous le focus.

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
Le volet SHALL conserver sa liste directe d'apps cachées puis Retirés d'À suivre, son tri, son dépliage à trois lignes et sa navigation. Les retraits de contenus SHALL accepter aussi les identités et sources du suivi ; Réafficher SHALL supprimer le masquage local pour les sources de la rangée et les candidats BetaSeries du héro, sans modifier le compte.

#### Scenario: sous-écran
- **WHEN** Applications cachées est sélectionné
- **THEN** le volet affiche directement Tout réactiver, apps cachées, puis intertitre et contenus retirés, en omettant les groupes vides, sans sous-écran supplémentaire

#### Scenario: focus initial
- **WHEN** droite entre dans le volet avec des apps cachées
- **THEN** la première app a le focus et haut atteint Tout réactiver ; sans app la première ligne de contenu est initiale

#### Scenario: tri des apps cachées
- **WHEN** des apps ont des dates de masquage et d'autres non
- **THEN** les datées sont triées par date décroissante puis les sans date par nom en ordre alphabétique sans casse

#### Scenario: réactivation
- **WHEN** OK est pressé sur une app cachée
- **THEN** son switch devient visible avec animation Apple, elle revient immédiatement dans la grille à son ancienne place si elle existe encore, sinon selon l'ordre alphabétique ; l'état est persisté et sa ligne reste focusée à sa place dans les réglages

#### Scenario: recacher avant le recalcul
- **WHEN** OK est pressé à nouveau sur cette ligne sans quitter la catégorie
- **THEN** l'app est recachée avec nouvelle date et sa ligne conserve place et focus

#### Scenario: tout réactiver
- **WHEN** Tout réactiver est validé
- **THEN** toutes les apps de la liste redeviennent visibles sans confirmation ; leurs lignes restent présentes avec switch visible, focus sur le bouton, et peuvent être recachées

#### Scenario: tout réactiver avec une app cachée désinstallée
- **WHEN** une app cachée désinstallée n'est pas dans la liste et Tout réactiver est validé
- **THEN** seules les apps listées sont réactivées ; la désinstallée reste cachée en cas de réinstallation

#### Scenario: recalcul au retour dans la catégorie
- **WHEN** la catégorie est quittée puis rouverte
- **THEN** la liste est recalculée avant sa première image, sans apps réactivées, avec nouveaux masquages, tri et état vide éventuel

#### Scenario: pas de recalcul sans changer de catégorie
- **WHEN** le focus passe à gauche puis à droite sans changer de catégorie
- **THEN** lignes, switches et dépliage sont conservés

#### Scenario: aucune app cachée
- **WHEN** aucun groupe ne contient d'élément
- **THEN** seul l'état vide centré est affiché, sans Tout réactiver ; droite reste sur la catégorie et Retour ferme les réglages

#### Scenario: navigation D-pad dans le volet
- **WHEN** le focus parcourt le volet
- **THEN** haut/bas suivent bouton, lignes et dépliages sans boucle, en sautant l'intertitre ; le défilement garde la ligne focusée entièrement visible, gauche rend la catégorie, Retour ferme la page, OK bascule, réaffiche ou déplie selon l'élément

#### Scenario: app de la ligne focalisée désinstallée
- **WHEN** l'app focusée est désinstallée et des lignes restent
- **THEN** la suivante à sa position, sinon la dernière reprend le focus

#### Scenario: liste vidée pendant l'affichage
- **WHEN** la dernière app est désinstallée sans contenu restant
- **THEN** l'état vide apparaît et le focus revient à la catégorie ; gauche y reste, haut/bas changent de catégorie et Retour ferme

#### Scenario: autre app désinstallée
- **WHEN** une ligne autre que celle focusée disparaît
- **THEN** le focus courant reste inchangé, y compris sur Tout réactiver

#### Scenario: retour dans le volet
- **WHEN** droite revient sans changement de catégorie
- **THEN** l'ordre, les états et le dépliage restent conservés et le focus initial est de nouveau appliqué

#### Scenario: contenus retirés
- **WHEN** des contenus locaux ou de suivi sont retirés
- **THEN** Retirés d'À suivre précède leurs lignes par retrait décroissant, vignette 16:9 avec image persistée sinon placeholder, titre puis « Série · … » ou « Film · … » avec sources affichables, ou le type seul sans source ; Réafficher apparaît à droite seulement sur la ligne focusée, sans package fictif pour BetaSeries

#### Scenario: réafficher un contenu
- **WHEN** OK est pressé sur un contenu retiré
- **THEN** le retrait et sa ligne sont supprimés ; focus à la suivante, sinon précédente, sinon catégorie ; le contenu revient selon ses sources valides et l'éligibilité du héro, sans exiger une publication TV si le compte le fournit

#### Scenario: groupe limité à 3 lignes
- **WHEN** cinq apps et deux contenus sont masqués
- **THEN** trois apps puis Afficher les 2 autres sont rendus, les deux contenus sont entiers et Tout réactiver vise les cinq apps

#### Scenario: dépliage d'un groupe
- **WHEN** Afficher les N autres est validé
- **THEN** toutes les lignes du groupe apparaissent, le contrôle disparaît, la quatrième ligne reçoit le focus et le groupe redevient replié à la prochaine entrée dans la catégorie

#### Scenario: un seul élément masqué
- **WHEN** un groupe contient quatre éléments
- **THEN** ses trois premières lignes sont suivies d'Afficher l'autre

#### Scenario: groupe des apps vide
- **WHEN** seuls des contenus sont retirés
- **THEN** aucun bouton Tout réactiver ni app n'est affiché ; l'intertitre et les contenus forment le volet

**Détails.** — Les contenus retirés gardent une vignette 16:9, image persistée sinon placeholder, tri par retrait décroissant, titre et libellés « Série · … » ou « Film · … » avec sources persistées ; les apps sources encore installées sont séparées par virgules, le fournisseur peut être nommé sans paquet d'app fictif, et le type seul est affiché sans source disponible. Réafficher est à droite uniquement au focus et ne modifie pas le compte. Dépliage non persisté, trois lignes par groupe puis Afficher les N autres ou Afficher l'autre, quatrième focusée au dépliage. Le recalcul a lieu uniquement à l'entrée de catégorie, pas entre volets. Les switches et Tout réactiver agissent immédiatement sur grille/dock avec animation Apple en gardant les lignes pendant la visite ; ils n'annulent jamais les retraits de contenus. Une app réactivée retrouve son ancien ordre de grille s'il existe encore, sinon son ordre alphabétique.

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
Afficher À suivre SHALL rester le premier contrôle, avec libellé à gauche et switch Apple à droite, activé par défaut et persisté. Il autorise les contenus affichables de toutes les sources sans imposer READ_TV_LISTINGS au compte. Désactiver SHALL retirer rangée et titre sans modifier programmes, compte, filtres ni apport du compte au héro.

#### Scenario: réglage activé
- **WHEN** le réglage est activé
- **THEN** la rangée suit sa position et les sources affichables locales ou de suivi

#### Scenario: réglage désactivé
- **WHEN** le réglage est désactivé
- **THEN** rangée et titre sont masqués, sautés dans la navigation et le réglage reste persisté

#### Scenario: rangée indisponible malgré le réglage activé
- **WHEN** aucune source n'a d'état affichable selon up-next
- **THEN** la rangée reste masquée ; un refus de la permission TV ne suffit pas si le compte est accessible

#### Scenario: bascule
- **WHEN** OK est pressé sur la ligne
- **THEN** le switch et la préférence changent avec animation Apple et effet immédiat sans redémarrage, et le focus reste sur la ligne

#### Scenario: navigation D-pad de la ligne
- **WHEN** le focus est sur Afficher À suivre
- **THEN** bas atteint Position d'À suivre, haut et droite ne font rien, gauche rend la catégorie Écran d'accueil et Retour ferme les réglages sauf panneau actif

#### Scenario: focus initial de la catégorie
- **WHEN** droite entre dans Écran d'accueil depuis les catégories, y compris après gauche puis droite
- **THEN** Afficher À suivre reçoit le focus avec les valeurs conservées

### Requirement: Position d'À suivre
Sous « Afficher À suivre », « Position d'À suivre » SHALL proposer « Avant les applications » par défaut et « Après les applications », persistés DataStore/restaurés au démarrage. L'effet sur la grille SHALL suivre launcher-shell. Rendu, panneau verre, coche, focus et navigation SHALL suivre les scénarios ci-dessous et le design system, sans animation supplémentaire.

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

**Détails.** — Ligne de position fermée : Selon 7.7/7.8 et les composants « Ligne de réglage à valeur »/« Liste déroulante », la ligne SHALL montrer libellé à gauche, valeur `TextStyles.RowState` à droite, couleur tertiaire `SygixColors.OnDarkSecondary` au repos / `OnPillTertiary` au focus, puis chevron bas `Dimens.SettingsChevron` à `Dimens.SettingsChevronGap`, même couleur. Cas « ligne ouverte » : quand la liste est ouverte, la ligne prend `SygixColors.PillSelected` et son chevron pointe vers le haut, selon les scénarios de « Position d'À suivre ». — Liste de position ouverte : Le panneau SHALL être `GlassSurface` / `GlassLook.Menu`, largeur `Dimens.DropdownWidth`, coins `Dimens.DropdownCorner`, padding `Dimens.DropdownPadding`, ancré `Dimens.DropdownOffset` sous la ligne et `Dimens.DropdownInsetEnd` en retrait du bord droit. Il SHALL couvrir le volet sans voile et présenter les deux valeurs dans leur ordre, selon « Options de position ». Cas « ancrage et matériau » : quand le menu de position est affiché, les scénarios de liste ouverte de « Position d'À suivre » gardent la géométrie et le matériau du menu contextuel. — Options de position : Chaque option SHALL avoir hauteur `Dimens.DropdownOptionHeight`, coins `Dimens.DropdownOptionCorner` concentriques au panneau, écart `Dimens.DropdownOptionGap`, colonne gauche `Dimens.DropdownCheck` avec coche pour la seule valeur courante, texte `TextStyles.Row` / `RowFocused` au focus, pilule de « Page de réglages ». Cas « option courante et focus » : quand la liste de position est parcourue, « déplacements dans la liste » et les scénarios de validation/fermeture de « Position d'À suivre » gardent la coche et le focus dans la liste.

### Requirement: Réglages du launcher système
Après visibilité/position d'À suivre, « Launcher système » non focusable SHALL précéder la seule ligne « Configurer SygixOs comme launcher principal », ouvrant le panneau de launcher-shell (9.14/9.15/8.21) puis retrouvant le focus. Ces réglages ne SHALL figurer sur aucune autre ligne/catégorie. Focus sans zoom et D-pad standard SHALL sauter l'intertitre, sans boucle ni effet droite ; état et détail SHALL suivre les scénarios ci-dessous, relus à chaque retour.

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

**Détails.** — État de la ligne launcher : État/détail SHALL être recalculés au retour au premier plan, à la fermeture du panneau et en fin de séquence. Le détail SHALL être « Automatique : <état> · Par autorisations : <état> », états en minuscules. À droite : « Actif » avec point vert si une voie active, sinon « Bloqué » si voie bloquée, sinon « En partie » si partielle, sinon « À configurer ». Cas « libellés des états des voies » : quand la ligne est relue, la voie automatique donne « actif », « à configurer » ou « indisponible sur cette TV », la voie par autorisations « active », « en partie », « bloquée », « non utilisée » ou « à faire », selon launcher-shell ; le premier contrôle reste « Afficher À suivre », et aucune autre ligne rôle HOME, allumage, accessibilité, débogage ou accueil d'origine n'est ajoutée.

### Requirement: Réglages du suivi de visionnage
La catégorie Suivi de visionnage SHALL exposer BetaSeries, connexion ou déconnexion, état du compte, synchronisation, réessai et pays confirmé. Aucun fournisseur non implémenté SHALL être proposé ; erreurs et révocation distante non confirmée SHALL être distinguées d'un compte vide.

#### Scenario: sans compte
- **WHEN** la catégorie est ouverte sans compte
- **THEN** Connecter BetaSeries est son premier contrôle ; OK ouvre l'association, dont Retour restaure ce contrôle sans connexion implicite

#### Scenario: compte connecté
- **WHEN** la catégorie est ouverte avec un compte
- **THEN** son identité et son état sont affichés avec Synchroniser, pays et Déconnecter ; aucune donnée d'une session précédente n'est présentée comme courante

#### Scenario: état du chargement
- **WHEN** une synchronisation charge, est vide, échoue ou n'est plus autorisée
- **THEN** le contrôle expose respectivement En cours, compte sans contenu, cause avec Réessayer ou Reconnecter, sans empêcher les autres réglages ni présenter un échec comme succès

#### Scenario: navigation
- **WHEN** droite entre dans le volet
- **THEN** le premier contrôle utilisable a le focus ; haut/bas parcourent les contrôles sans boucle, gauche retourne à la catégorie et Retour ferme d'abord un panneau actif sinon les réglages ; un contrôle disparu transmet le focus à son voisin sinon à la catégorie

#### Scenario: pays
- **WHEN** le pays est ouvert puis validé ou annulé
- **THEN** une liste déroulante ancrée au contrôle reprend le composant de position d'À suivre, sans modale ; le pays proposé depuis la TV reste distingué d'une valeur choisie ; OK valide, persiste et applique la valeur selon watch-tracking puis referme la liste ; Retour referme sans modification ; le contrôle reprend le focus et n'ajoute pas « · confirmé » après le pays

#### Scenario: navigation dans les pays
- **WHEN** la liste déroulante est ouverte
- **THEN** le pays choisi sinon proposé, sinon la première option prend le focus ; haut/bas parcourent les pays sans boucle, gauche/droite ne quittent pas la liste et Retour l'annule ; une option disparue transmet le focus à sa suivante sinon précédente, sinon referme la liste sur son contrôle

#### Scenario: attribution
- **WHEN** le fournisseur est présenté dans les réglages
- **THEN** BetaSeries est identifié et son adresse officielle est consultable, conformément à l'attribution de la source ; aucun identifiant d'application ni jeton n'est affiché
