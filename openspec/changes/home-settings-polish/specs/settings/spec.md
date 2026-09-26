# Delta settings

Trois exigences modifiées : « Applications cachées » (liste dans le volet de droite, plus de sous-écran), « Apps sources » (tri) et « Cacher une application » (date de masquage). La PR #13 (`p2c-upnext`) modifie aussi « Apps sources » et « Cacher une application » (périmètre Up Next) : voir `proposal.md`, « Chevauchements ». Le moment où la liste des apps cachées est recalculée, le départage à égalité de compteur, le caractère figé ou vivant du tri des apps sources et le focus initial du volet sont des questions ouvertes de `proposal.md` : les scénarios ci-dessous ne les tranchent pas.

## MODIFIED Requirements

### Requirement: Apps sources
La catégorie « Apps sources » SHALL lister toutes les apps TV installées avec, pour chacune, un toggle switch (style Apple) activant sa contribution au héro et au Top Shelf ; par défaut toutes les apps sont activées. La liste SHALL être triée : d'abord les apps dont le nombre de programmes publiés affiché est supérieur à 0, par nombre décroissant, puis les apps à 0, par ordre alphabétique de leur nom (insensible à la casse).

#### Scenario: présentation
- **WHEN** la catégorie « Apps sources » est affichée
- **THEN** chaque ligne montre l'icône de l'app, son nom et sous le nom des informations sur l'app dont le nombre de programmes publiés dans le TV Provider, avec à droite de la ligne un toggle switch

#### Scenario: tri par contenu publié
- **WHEN** la catégorie est affichée avec des apps publiant respectivement 12, 0, 3 et 0 programmes
- **THEN** l'ordre est : l'app à 12, l'app à 3, puis les deux apps à 0 par ordre alphabétique

#### Scenario: aucun compteur disponible
- **WHEN** aucun compteur n'est disponible (TV Provider absent, permission refusée, ou comptage pas encore reçu)
- **THEN** toutes les apps sont considérées à 0 et listées par ordre alphabétique, sans erreur visible ni crash ; le switch de chaque ligne reste utilisable

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
La catégorie « Applications cachées » SHALL afficher directement dans le volet de droite la liste des apps cachées, chacune avec un toggle switch reflétant son état (cachée / visible), précédée d'un bouton « Tout réactiver » placé au-dessus de la liste ; aucun sous-écran n'est ouvert. La liste SHALL être triée par date de masquage décroissante (la plus récemment cachée en premier), les apps cachées sans date étant placées en fin de liste par ordre alphabétique de leur nom (insensible à la casse). Réactiver une app (par son switch ou par « Tout réactiver ») SHALL agir immédiatement sur la grille et le dock et être persisté, mais SHALL laisser sa ligne affichée dans le volet, switch en position « visible », jusqu'au prochain recalcul de la liste (moment non tranché : question ouverte de `proposal.md`).

#### Scenario: sous-écran
- **WHEN** l'utilisateur sélectionne ou valide « Applications cachées » dans le volet gauche
- **THEN** aucun sous-écran ne s'ouvre : le volet droit affiche le bouton « Tout réactiver » puis, en dessous, une ligne par app cachée (icône, nom, switch en position « cachée ») ; aucun sous-écran ni palier supplémentaire n'est ouvert

#### Scenario: tri des apps cachées
- **WHEN** des apps ont été cachées à des dates différentes et d'autres sans date (masquées avant ce change)
- **THEN** les apps datées viennent en premier, de la plus récente à la plus ancienne, puis les apps sans date par ordre alphabétique

#### Scenario: réactivation
- **WHEN** l'utilisateur presse OK sur la ligne d'une app cachée
- **THEN** le switch passe en position « visible » avec l'animation Apple, l'app réapparaît immédiatement dans la grille (ordre alphabétique par défaut si l'ordre précédent n'existe plus), l'état est persisté, la ligne reste affichée à sa place et garde le focus

#### Scenario: recacher avant le recalcul
- **WHEN** l'utilisateur presse à nouveau OK sur une ligne dont le switch est en position « visible » (app réactivée par erreur), avant le recalcul de la liste
- **THEN** l'app est cachée à nouveau (avec une nouvelle date de masquage), disparaît de la grille et du dock, le switch repasse en position « cachée », la ligne garde sa place et le focus

#### Scenario: tout réactiver
- **WHEN** l'utilisateur presse OK sur « Tout réactiver »
- **THEN** sans confirmation, toutes les apps de la liste réapparaissent dans la grille, l'état est persisté, toutes les lignes restent affichées avec leur switch en position « visible », le focus reste sur le bouton ; chaque ligne peut ensuite être recachée individuellement comme au scénario « recacher avant le recalcul »

#### Scenario: recalcul de la liste
- **WHEN** la liste est recalculée (moment défini par la question ouverte)
- **THEN** les lignes des apps devenues visibles disparaissent, les apps cachées entre-temps apparaissent, l'ordre suit « tri des apps cachées », et si la liste devient vide l'état vide s'affiche

#### Scenario: aucune app cachée
- **WHEN** aucune app n'est cachée au moment où la catégorie est affichée
- **THEN** le volet droit affiche uniquement un message d'état vide centré (aucune ligne, aucun bouton « Tout réactiver » rendu, aucun crash) ; droite depuis le volet gauche laisse le focus sur la catégorie, Retour ferme les réglages comme depuis toute la page

#### Scenario: navigation D-pad dans le volet
- **WHEN** le focus est dans le volet « Applications cachées »
- **THEN** haut et bas parcourent le bouton puis les lignes dans l'ordre affiché, sans boucle aux bords (haut depuis le bouton et bas depuis la dernière ligne ne font rien) ; la liste défile pour garder l'élément focusé entièrement visible ; gauche rend le focus au volet gauche sur la catégorie « Applications cachées » ; Retour ferme les réglages (« Page de réglages », scénario « retour ») ; OK bascule l'élément focusé ; le focus initial en entrant dans le volet est le premier élément focalisable (bouton ou première ligne : question ouverte de `proposal.md`)

#### Scenario: retour dans le volet
- **WHEN** l'utilisateur revient dans le volet depuis le volet gauche sans que la liste ait été recalculée
- **THEN** l'ordre et les états des switches sont ceux laissés à la sortie ; si l'élément précédemment focusé n'existe plus après un recalcul, le focus va au premier élément focalisable
