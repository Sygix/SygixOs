# Spec Delta

## MODIFIED Requirements

### Requirement: Qualité des visuels
Le héro et le panneau Top Shelf SHALL n'afficher que des visuels de bonne qualité : vidéo d'aperçu privilégiée quand l'app en fournit une, image paysage d'au moins 640 px de large ou vidéo d'au moins 1080 px de large, sinon le programme est écarté. La largeur d'une image SHALL être mesurée une fois l'image ramenée dans 1920 × 1080 sans agrandissement, et une image plus haute que large SHALL être écartée.

#### Scenario: image trop petite
- **WHEN** l'image d'un programme, ramenée dans 1920 × 1080 sans agrandissement, fait moins de 640 px de large, est plus haute que large, ou ne peut pas être chargée
- **THEN** le héro passe au programme suivant sans afficher l'image, et le panneau Top Shelf ne l'inclut pas

#### Scenario: variante YouTube
- **WHEN** l'image d'un programme est une miniature YouTube (`i.ytimg.com`, `iN.ytimg.com` ou `img.youtube.com`, chemin `/vi/<id>/` ou `/vi_webp/<id>/`)
- **THEN** la variante `https://i.ytimg.com/vi/<id>/maxresdefault.jpg` est vérifiée d'abord ; acceptée, c'est elle qu'affichent le héro et le panneau Top Shelf ; absente ou trop petite, l'image publiée est vérifiée selon la règle générale ; un échec réseau sur l'une ou l'autre laisse le programme à revérifier

#### Scenario: vidéo trop petite
- **WHEN** la vidéo d'aperçu d'un programme fait moins de 1080 px de large
- **THEN** le héro replie sur le poster du programme, puis l'écarte si le poster est aussi insuffisant

### Requirement: Préchargement et mémoire
Le launcher SHALL valider les visuels avant de les afficher, en bornant ce travail : les premiers visuels du héro au chargement, les affiches d'une app quand le focus s'y pose. Il SHALL aussi limiter sa consommation mémoire et GPU : chaque image SHALL être décodée à sa taille d'affichage, jamais à la résolution native de la dalle, et le héro SHALL rester fluide au repos et pendant le focus du dock sur la TV de référence (au moins 55 images par seconde et moins de 10 % d'images en retard, mesurés sur l'APK de release). La navigation dans la grille (déplacements du focus, défilement, ouverture du panneau Top Shelf) SHALL avoir moins de 10 % d'images en retard sur la même TV : un déplacement du focus ne recompose que les tuiles et la rangée concernées, et ne lance ni décodage ni vérification d'image tant que le focus ne s'est pas posé. La grille SHALL borner sa composition : jusqu'à 12 rangées (rangée Up Next comprise), toutes les rangées sont composées ; au-delà, seules les rangées de la fenêtre le sont — celles qui couvrent l'écran à la position courante du défilement et à sa position cible (rangée focusée), plus 2 rangées de prélecture de chaque côté — et les autres sont remplacées par un espace de leur hauteur. La page garde une seule autorité de défilement vertical, la zone grille reste la source du verre et du dégradé d'ambiance, et les règles de « Panneau Top Shelf au focus », « Navigation 3 paliers », « Persistance de la grille » et « Focus d'une app disparue » s'appliquent telles quelles.

#### Scenario: validation au chargement
- **WHEN** les programmes sont chargés
- **THEN** seuls les premiers visuels du héro sont vérifiés en arrière-plan (chargement, largeur selon « Qualité des visuels ») d'après les dimensions lues dans l'en-tête de l'image, sans décoder ses pixels ; les visuels vérifiés restent en cache disque, aucun n'est préchargé en mémoire ; le provider pouvant publier des centaines de programmes, aucun autre visuel n'est vérifié à ce moment

#### Scenario: validation à la demande
- **WHEN** le focus reste au moins 0,5 s sur une tuile de la grille
- **THEN** quelques affiches candidates de cette app sont vérifiées, chacune une seule fois tant qu'elle n'est pas injoignable (scénario « visuel injoignable ») ; seules les affiches validées alimentent son panneau ; un focus qui ne fait que passer sur une tuile ne lance aucune vérification

#### Scenario: validation interrompue ou nouvelles affiches
- **WHEN** la vérification des affiches d'une app a été interrompue (focus posé sur une autre app avant la fin), ou les programmes rechargés proposent d'autres affiches candidates pour cette app
- **THEN** au prochain focus prolongé sur l'app, les affiches candidates pas encore vérifiées le sont ; celles déjà vérifiées ne sont pas redemandées

#### Scenario: visuel injoignable
- **WHEN** la vérification d'un visuel du héro ou d'une affiche du panneau Top Shelf échoue pour une raison réseau (erreur d'entrée-sortie, erreur HTTP 5xx)
- **THEN** le visuel n'est pas affiché ; il est vérifié de nouveau à la prochaine relecture du héro (retour au premier plan, changement signalé par le TV Provider ou relecture périodique, selon « Contenu héro TV Provider ») s'il fait partie des premiers visuels du héro, ou au prochain focus prolongé sur son app pour une affiche du panneau Top Shelf

#### Scenario: visuel rejeté
- **WHEN** un visuel est trop étroit, refusé par le serveur (erreur HTTP 4xx) ou impossible à décoder
- **THEN** il n'est plus vérifié ni demandé pendant la session tant que son programme reste publié, même après une relecture du héro

#### Scenario: économie de ressources
- **WHEN** le héro est masqué à l'intérieur du launcher (zone grille, page de réglages) ou une surface verre est invisible
- **THEN** la lecture de la vidéo est interrompue (arrêtée, ou mise en pause à sa position quand une exigence le prévoit) sans que le lecteur vidéo soit libéré, le launcher étant au premier plan ; les animations de fond, le Ken Burns du visuel du héro et le flou d'arrière-plan sont arrêtés ; les images sont décodées avec au plus deux décodeurs simultanés

#### Scenario: launcher en arrière-plan
- **WHEN** une autre app passe au premier plan et le launcher n'est plus visible
- **THEN** le lecteur vidéo du héro est libéré, ce qui rend le décodeur vidéo à l'app au premier plan ; le défilement automatique du diaporama et les délais du héro (visuel lent, démarrage d'une vidéo) sont suspendus

#### Scenario: retour au premier plan après libération
- **WHEN** le launcher revient au premier plan après avoir libéré le lecteur vidéo
- **THEN** la vidéo d'aperçu du programme affiché est rechargée et reprend, sans compter comme un échec ni faire passer le héro au programme suivant ; le défilement automatique et les délais du héro repartent de zéro ; une vidéo lente dont la préparation a été interrompue (héro masqué ou launcher en arrière-plan) redevient éligible au diaporama

#### Scenario: écran au repos
- **WHEN** aucune touche n'est pressée et aucune transition n'est en cours
- **THEN** seules les animations spécifiées encore visibles redessinent l'écran (vidéo d'aperçu, passage unique du dégradé animé du repli, passage unique du Ken Burns d'un nouveau visuel du héro ou du panneau Top Shelf, changement de visuel du défilement automatique) ; une fois ces passages terminés, l'image est immobile ; aucune animation d'un élément hors écran ne tourne, et une animation continue ne provoque aucune recomposition à chaque image

#### Scenario: taille de décodage
- **WHEN** un visuel du héro, une affiche du panneau Top Shelf ou une bannière d'app est décodé, même publié en 3840 × 2160
- **THEN** le visuel du héro est décodé à la taille de la fenêtre, au plus 1920 × 1080, l'affiche à la taille du panneau (en le couvrant), et la bannière à la taille de la tuile de la grille au zoom de focus, en pixels de l'appareil, au plus 480 × 270

#### Scenario: une passe par image
- **WHEN** le héro affiche le visuel d'un programme, au repos, pendant le Ken Burns ou un fondu
- **THEN** les voiles du bas et de la gauche sont intégrés au visuel décodé et le voile du coin haut droit est dessiné seul sur sa petite zone ; ni fond de fenêtre, ni fond noir, ni dégradé du repli, ni voile n'est dessiné sous un visuel opaque, et aucun fondu ne passe par un calque plein écran hors écran

#### Scenario: écran de démarrage
- **WHEN** l'écran de démarrage est affiché
- **THEN** seuls le catalogue et le premier visuel utilisable du héro sont préparés en priorité ; la vérification des visuels suivants du héro attend la fin de l'écran de démarrage, les bannières sont décodées sur un fil de basse priorité, et la grille, hors écran, n'est composée qu'après le fondu vers l'accueil

#### Scenario: navigation dans la grille
- **WHEN** l'utilisateur déplace le focus de tuile en tuile et de rangée en rangée dans la grille
- **THEN** seules les tuiles qui prennent ou perdent le focus et les rangées qui les contiennent sont recomposées, l'animation du focus (zoom, ombre, reflet) ne recompose rien, et la vérification d'un visuel d'une autre app ne recompose aucune rangée

#### Scenario: grande grille
- **WHEN** le catalogue compte plus de 12 rangées
- **THEN** le nombre de tuiles composées dépend de la hauteur de l'écran et de la prélecture, pas du nombre d'apps ; les bannières des rangées composées sont lues dans le cache borné des visuels d'apps ou décodées sur le fil de basse priorité, celles des rangées hors fenêtre ne sont ni composées ni décodées

#### Scenario: rangée visée hors fenêtre
- **WHEN** le focus vise une rangée non composée : rafale de touches plus rapide que le défilement, retour dans la grille sur la rangée laissée (héro, menu contextuel, réglages, retour d'une app), app déplacée d'une rangée hors fenêtre, rangée Up Next placée après les apps
- **THEN** la rangée visée est composée dès que l'ancre la désigne, avant la demande de focus, et la tuile ou la carte reçoit le focus par son identité à la place prévue par « Panneau Top Shelf au focus » ; un retour rapide en arrière retrouve la rangée précédente encore composée

#### Scenario: panneau sur une rangée profonde
- **WHEN** le panneau Top Shelf s'ouvre sur une rangée d'une grande grille
- **THEN** le bloc focusé est placé comme sur une petite grille ; pendant le délai d'ouverture, la première affiche validée est décodée à la taille du panneau avec la requête de l'affichage, pour que l'expansion montre une image prête ; le panneau s'ouvre avec les affiches déjà validées et ne met sa liste à jour qu'une fois la vérification de l'app terminée, sans changer l'affiche en cours ; seules la rangée dont le panneau est ouvert et celle dont il se ferme portent son animation

#### Scenario: fraîcheur du cache disque
- **WHEN** un visuel du héro ou une affiche du panneau est demandé alors que son entrée du cache disque date de plus d'un jour
- **THEN** l'image est relue depuis le réseau et remplace l'entrée du cache disque et les images en mémoire issues de cette URL ; si le réseau échoue, la version en cache est affichée ; une entrée plus récente est servie sans requête réseau
