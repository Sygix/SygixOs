# Delta launcher-shell

## MODIFIED Requirements

### Requirement: Écran de démarrage
Au démarrage à froid du launcher, c'est-à-dire à la première création de l'accueil dans son processus (y compris quand Android avait déjà démarré le processus en arrière-plan, et après une mise à jour), le launcher SHALL afficher un écran de démarrage plein écran à la place de l'accueil : la mascotte seule (le fantôme, sans TV ni nom, aucun texte), centrée, sur le fond noir de l'application (« Thème »), animée (« Animation de la mascotte »). La mascotte SHALL apparaître par un fondu court depuis le fond, jamais par un saut sec.

L'accueil SHALL être prêt quand le catalogue d'apps est chargé et que le premier visuel du héro est prêt à l'affichage (image d'un programme chargée, ou première image d'une vidéo, de programme ou nature, rendue ; le dégradé animé du repli ne compte pas comme visuel) ; l'attente de ce visuel SHALL être plafonnée à 2 s, comptées depuis la première image de l'écran de démarrage. L'écran de démarrage SHALL rester affiché au moins 600 ms depuis la première image qui montre la mascotte (depuis sa propre première image si l'animation est illisible), et tant que l'accueil n'est pas prêt, sans dépasser 5 s au total depuis sa première image. L'animation de la mascotte SHALL être décodée dès le démarrage du processus, hors du fil principal. Dès que la durée minimale est atteinte et que l'accueil est prêt, ou au plus tard à 5 s, il SHALL laisser place à l'accueil par un fondu enchaîné, avec la courbe et une durée du design system (« Focus tvOS »), sans image noire, sans saut et sans flash ; à 5 s, l'accueil est révélé dans l'état où il se trouve et ses propres états de chargement ou d'erreur prennent le relais.

L'accueil SHALL être composé seulement après le fondu d'entrée de la mascotte, puis dès que la fenêtre est active ou au plus tard 400 ms après ce fondu si elle ne l'est pas (demande de permission, surcouche système), ou au début du fondu de sortie, pour que sa composition ne retarde ni l'apparition de la mascotte ni la réception des touches ; jusqu'au début du fondu, il SHALL être composé et dessiné sous l'écran de démarrage opaque, donc invisible, sans aucune animation (le visuel du héro y apparaît sans fondu, une vidéo d'aperçu y est préparée sans être lue, le Ken Burns et la lecture ne commencent qu'au fondu de sortie, la grille hors écran n'est composée qu'à la fin du fondu), pour que le fondu de sortie n'ait plus à le dessiner pour la première fois, et aucun de ses éléments ne SHALL pouvoir prendre le focus : les touches du D-pad et OK n'ont aucun effet sur lui. Le fondu de sortie SHALL être fluide, sans calque plein écran hors écran. Le fondu de sortie appartient à l'écran de démarrage (décision de Sygix) : la touche Retour pressée à tout moment de l'écran de démarrage, fondu de sortie compris, SHALL suivre le comportement normal du système (SygixOs quitte le premier plan), sans être interceptée par l'accueil ni déclencher d'action sur lui. L'écran de démarrage SHALL ne jamais être affiché au retour sur un accueil déjà créé dans le processus (touche Home, fin d'une autre app, retour au premier plan). Hors démarrage à froid, si l'accueil est recréé alors que son catalogue n'est pas encore chargé, le launcher SHALL afficher un fond noir uni, sans mascotte ni animation, jusqu'à ce que l'accueil s'affiche.

#### Scenario: démarrage à froid
- **WHEN** l'accueil est créé pour la première fois dans le processus du launcher
- **THEN** l'écran de démarrage s'affiche : la mascotte seule, centrée, animée, sur fond noir ; aucun texte, aucun dégradé animé, aucune autre zone de l'accueil n'est visible

#### Scenario: processus démarré en arrière-plan
- **WHEN** Android a démarré le processus du launcher en arrière-plan (diffusion système, mise à jour) sans créer l'accueil, puis l'utilisateur ouvre le launcher
- **THEN** c'est un démarrage à froid : l'écran de démarrage s'affiche selon les mêmes règles

#### Scenario: accueil prêt avant la durée minimale
- **WHEN** le catalogue est chargé et le premier visuel du héro est prêt 100 ms après la première image qui montre la mascotte
- **THEN** l'écran de démarrage reste affiché jusqu'à 600 ms après cette image, puis le fondu vers l'accueil commence

#### Scenario: mascotte décodée tard
- **WHEN** la mascotte n'apparaît que 800 ms après la première image de l'écran de démarrage, l'accueil étant déjà prêt
- **THEN** la mascotte apparaît par son fondu d'entrée et l'écran de démarrage reste affiché 600 ms à partir de son apparition, dans la limite du plafond global de 5 s

#### Scenario: composition de l'accueil
- **WHEN** le catalogue est chargé avant la fin du fondu d'entrée de la mascotte ou avant que la fenêtre soit active
- **THEN** l'accueil n'est composé qu'après ce fondu et l'activation de la fenêtre ; l'apparition de la mascotte et son animation ne sont pas interrompues

#### Scenario: fenêtre sans focus
- **WHEN** la demande de permission du premier lancement ou une surcouche système garde le focus de la fenêtre pendant l'écran de démarrage
- **THEN** l'accueil est composé 400 ms après le fondu d'entrée de la mascotte et l'écran de démarrage se termine selon les mêmes règles de durée

#### Scenario: attente du premier visuel
- **WHEN** le catalogue est chargé à 200 ms et le premier visuel du héro n'est prêt qu'à 1,5 s
- **THEN** l'écran de démarrage reste affiché et animé jusqu'à 1,5 s, puis le fondu commence aussitôt

#### Scenario: visuel trop lent
- **WHEN** le catalogue est chargé à 200 ms et le premier visuel du héro n'est toujours pas prêt 2 s après la première image de l'écran de démarrage
- **THEN** le fondu commence à 2 s ; le héro poursuit son propre chargement et affiche son état de repli tant qu'aucun visuel n'est prêt (« Écran initial du home », scénario « fallback sans contenu »)

#### Scenario: plafond global
- **WHEN** le catalogue n'est toujours pas chargé 5 s après la première image de l'écran de démarrage
- **THEN** le fondu commence à 5 s et révèle l'accueil dans son état courant : fond noir uni tant que le catalogue n'est pas chargé, puis l'accueil dès qu'il l'est, avec ses propres états de chargement ou d'erreur

#### Scenario: fondu vers l'accueil
- **WHEN** le fondu vers l'accueil commence
- **THEN** l'accueil, jusque-là composé et caché sous l'écran de démarrage, apparaît pendant que l'écran de démarrage s'efface ; le héro détient le focus dès le début du fondu (« Écran initial du home ») ; à la fin du fondu l'écran de démarrage n'est plus affiché et son animation est arrêtée

#### Scenario: touches pendant l'écran de démarrage
- **WHEN** l'utilisateur presse une touche du D-pad ou OK avant le début du fondu
- **THEN** aucun élément de l'accueil ne prend le focus ni ne réagit ; au début du fondu, le héro prend le focus comme si aucune touche n'avait été pressée

#### Scenario: Retour pendant l'écran de démarrage
- **WHEN** l'utilisateur presse Retour pendant l'écran de démarrage, y compris dans la première seconde après le lancement
- **THEN** le comportement normal du système s'applique : SygixOs quitte le premier plan, sans crash ; aucune action n'est déclenchée sur l'accueil

#### Scenario: Retour pendant le fondu de sortie
- **WHEN** l'utilisateur presse Retour pendant le fondu de sortie, entre son début et sa fin
- **THEN** le comportement normal du système s'applique comme pendant le reste de l'écran de démarrage : SygixOs quitte le premier plan, sans crash ; le fondu n'est pas intercepté par l'accueil et aucune action n'est déclenchée sur lui

#### Scenario: retour sur le launcher
- **WHEN** le launcher revient au premier plan (touche Home, fin d'une autre app) alors que l'accueil a déjà été créé dans le processus
- **THEN** aucun écran de démarrage n'est affiché ; l'accueil s'affiche directement, selon les règles de retour existantes (« Navigation 3 paliers »)

#### Scenario: accueil recréé hors démarrage à froid
- **WHEN** l'accueil est recréé dans un processus où il avait déjà été créé, avant que son catalogue soit chargé
- **THEN** un fond noir uni s'affiche, sans mascotte ni animation, jusqu'à ce que l'accueil s'affiche

#### Scenario: processus arrêté par le système
- **WHEN** le système a arrêté le processus du launcher (manque de mémoire) et l'utilisateur revient sur le launcher
- **THEN** c'est un démarrage à froid : l'écran de démarrage s'affiche selon les mêmes règles

#### Scenario: demande de permission au premier lancement
- **WHEN** la demande système de la permission `READ_TV_LISTINGS` s'affiche pendant l'écran de démarrage
- **THEN** la demande apparaît par-dessus ; l'écran de démarrage et le fondu se poursuivent sans attendre la réponse, qui est traitée comme avant (« Contenu héro TV Provider », scénario « permission »)

#### Scenario: animation illisible
- **WHEN** l'animation de la mascotte ne peut pas être décodée (fichier absent ou corrompu)
- **THEN** l'écran de démarrage affiche le fond seul, sans mascotte, avec les mêmes règles de durée et de fondu, la durée minimale comptant depuis sa première image ; aucune erreur visible, aucun crash

#### Scenario: fluidité de l'écran de démarrage
- **WHEN** l'écran de démarrage est affiché puis s'efface vers l'accueil sur la TV de référence (APK de release)
- **THEN** la mascotte et le fondu de sortie visent 60 images par seconde et moins de 10 % d'images en retard ; aucun travail de l'accueil autre que sa première composition et son premier dessin, cachés sous l'écran de démarrage, n'a lieu pendant la mascotte

### Requirement: Écran de démarrage sans animation
Quand les animations sont désactivées dans le système (accessibilité « Supprimer les animations », ou échelle de durée des animations à 0 dans les options pour les développeurs), l'écran de démarrage SHALL afficher la première image de la mascotte, fixe, sans flottement ni clignement ; la mascotte SHALL apparaître sans fondu et l'accueil SHALL remplacer l'écran de démarrage sans fondu. Les règles de durée de « Écran de démarrage » (600 ms au moins, accueil prêt, plafonds de 2 s et de 5 s) restent les mêmes. Le réglage SHALL être lu à chaque démarrage à froid, dans l'échelle de durée des animations du système (réglage que modifient ces deux options) ; une échelle d'animation propre aux apps forcée par le constructeur ne SHALL pas désactiver l'animation.

#### Scenario: animations désactivées
- **WHEN** le launcher démarre à froid alors que les animations sont désactivées dans le système
- **THEN** l'écran de démarrage montre la première image de la mascotte, immobile, dès sa première image ; dès que les règles de durée le permettent, l'accueil s'affiche d'un coup, sans fondu, le héro ayant le focus

#### Scenario: réglage changé entre deux démarrages
- **WHEN** l'utilisateur réactive les animations puis le launcher redémarre à froid
- **THEN** la mascotte est de nouveau animée et les fondus sont joués

#### Scenario: échelle forcée par le constructeur
- **WHEN** le système laisse l'échelle de durée des animations à sa valeur normale mais que le constructeur force à 0 l'échelle d'animation des apps
- **THEN** la mascotte est animée (flottement et clignement), apparaît et disparaît par ses fondus

#### Scenario: Retour sans fondu
- **WHEN** les animations sont désactivées et l'utilisateur presse Retour, avant ou après le remplacement instantané de l'écran de démarrage par l'accueil
- **THEN** avant le remplacement, le comportement normal du système s'applique : SygixOs quitte le premier plan, sans action sur l'accueil ; après le remplacement, l'accueil est interactif et Retour suit « Navigation 3 paliers », l'app restant au premier plan
