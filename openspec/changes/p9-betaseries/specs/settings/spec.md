# Spec Delta

## MODIFIED Requirements

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

**Détails.** — Le focus des catégories, lignes, contenus, dépliages, contrôles et actions reprend la pilule claire sans zoom, halo ou verre, et l'état marqué de la catégorie active. Les parcours de connexion, pays et accès média respectent cette base et leur navigation propre ; les autres catégories ne changent pas de règles.

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

## ADDED Requirements

### Requirement: Réglages du suivi de visionnage
La catégorie Suivi de visionnage SHALL exposer le fournisseur BetaSeries, connexion ou déconnexion, état du compte, synchronisation et réessai, pays confirmé et accès média optionnel. Aucun fournisseur non implémenté SHALL être proposé ; les erreurs et accès manquants SHALL être distingués d'un compte vide.

#### Scenario: sans compte
- **WHEN** la catégorie est ouverte sans compte
- **THEN** Connecter BetaSeries est son premier contrôle ; OK ouvre l'association, dont Retour restaure ce contrôle sans connexion implicite

#### Scenario: compte connecté
- **WHEN** la catégorie est ouverte avec un compte
- **THEN** son identité et son état sont affichés avec Synchroniser, pays, accès média et Déconnecter ; aucune donnée d'une session précédente n'est présentée comme courante

#### Scenario: état du chargement
- **WHEN** une synchronisation charge, est vide, échoue ou n'est plus autorisée
- **THEN** le contrôle expose respectivement En cours, compte sans contenu, cause avec Réessayer ou Reconnecter, sans empêcher les autres réglages ni présenter un échec comme succès

#### Scenario: navigation
- **WHEN** droite entre dans le volet
- **THEN** le premier contrôle utilisable a le focus ; haut/bas parcourent les contrôles sans boucle, gauche retourne à la catégorie et Retour ferme d'abord un panneau actif sinon les réglages ; un contrôle disparu transmet le focus à son voisin sinon à la catégorie

#### Scenario: pays
- **WHEN** le pays est ouvert puis validé ou annulé
- **THEN** le pays proposé depuis la TV, sinon aucun pays confirmé, est affiché ; la valeur confirmée est persistée et appliquée selon watch-tracking ; annuler ne change rien et le contrôle reprend le focus

#### Scenario: retour système
- **WHEN** l'utilisateur revient des réglages Android d'accès média
- **THEN** l'autorisation réelle est relue, le contrôle d'origine retrouve le focus et un refus ou une indisponibilité ne bloque pas la catégorie

#### Scenario: attribution
- **WHEN** le fournisseur est présenté dans les réglages
- **THEN** BetaSeries est identifié et son adresse officielle est consultable, conformément à l'attribution de la source ; aucun identifiant d'application ni jeton n'est affiché
