# Spec Delta

## MODIFIED Requirements

### Requirement: Fond de la zone grille
Dans la grille, le fond SHALL être un dégradé neutre uni type tvOS, le héro étant complètement masqué parce qu'il est sorti de l'écran par le haut.

#### Scenario: révélation de la grille
- **WHEN** l'utilisateur passe en zone grille
- **THEN** à la fin du défilement le héro est entièrement hors écran (aucun pixel du héro, du dock ni de la capsule heure et réglages n'est visible), le fond est le dégradé neutre (pas d'aerial, pas de posters en fond de grille), sans fondu de couches

#### Scenario: pendant le défilement
- **WHEN** la page est entre le héro et la grille
- **THEN** le héro et la grille sont chacun visibles pour la part de l'écran qu'ils occupent, sans zone noire ni image figée entre les deux ; le dégradé neutre est le fond de la partie grille dès le début du mouvement

#### Scenario: héro hors écran
- **WHEN** le héro est entièrement hors écran
- **THEN** sa lecture est interrompue et ses animations arrêtées conformément à « Préchargement et mémoire » (scénario « économie de ressources ») et « Fond vidéo de secours » ; son lecteur vidéo n'est pas libéré

### Requirement: Préchargement et mémoire
Le launcher SHALL valider les visuels avant de les afficher, en bornant ce travail : les premiers visuels du héro au chargement, les affiches d'une app quand le focus s'y pose. Il SHALL aussi limiter sa consommation mémoire et GPU : chaque image SHALL être décodée à sa taille d'affichage, jamais à la résolution native de la dalle, et le héro SHALL rester fluide au repos et pendant le focus du dock sur la TV de référence (au moins 55 images par seconde et moins de 10 % d'images en retard, mesurés sur l'APK de release). La navigation dans la grille (déplacements du focus, défilement, ouverture du panneau Top Shelf) SHALL avoir moins de 10 % d'images en retard sur la même TV : un déplacement du focus ne recompose que les tuiles et la rangée concernées, et ne lance ni décodage ni vérification d'image tant que le focus ne s'est pas posé.

#### Scenario: validation au chargement
- **WHEN** les programmes sont chargés
- **THEN** seuls les premiers visuels du héro sont vérifiés en arrière-plan (chargement, largeur ≥ 1080 px) ; ils sont gardés en mémoire, les suivants en cache disque ; le provider pouvant publier des centaines de programmes, aucun autre visuel n'est vérifié à ce moment

#### Scenario: validation à la demande
- **WHEN** le focus reste au moins 0,5 s sur une tuile de la grille
- **THEN** quelques affiches candidates de cette app sont vérifiées, chacune une seule fois tant qu'elle n'est pas injoignable (scénario « visuel injoignable ») ; seules les affiches validées alimentent son panneau ; un focus qui ne fait que passer sur une tuile ne lance aucune vérification

#### Scenario: validation interrompue ou nouvelles affiches
- **WHEN** la vérification des affiches d'une app a été interrompue (focus posé sur une autre app avant la fin), ou les programmes rechargés proposent d'autres affiches candidates pour cette app
- **THEN** au prochain focus prolongé sur l'app, les affiches candidates pas encore vérifiées le sont ; celles déjà vérifiées ne sont pas redemandées

#### Scenario: visuel injoignable
- **WHEN** la vérification d'un visuel du héro ou d'une affiche du panneau Top Shelf échoue pour une raison réseau (erreur d'entrée-sortie, erreur HTTP 5xx)
- **THEN** le visuel n'est pas affiché ; il est vérifié de nouveau au prochain rafraîchissement du héro (retour à l'accueil) s'il fait partie des premiers visuels du héro, ou au prochain focus prolongé sur son app pour une affiche du panneau Top Shelf

#### Scenario: visuel rejeté
- **WHEN** un visuel est trop étroit, refusé par le serveur (erreur HTTP 4xx) ou impossible à décoder
- **THEN** il n'est plus vérifié ni demandé pendant la session, même après un rafraîchissement du héro

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
