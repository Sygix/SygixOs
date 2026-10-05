# Delta launcher-shell

## ADDED Requirements

### Requirement: Écran de démarrage
Au démarrage à froid du launcher, c'est-à-dire à la première création de l'accueil dans son processus (y compris quand Android avait déjà démarré le processus en arrière-plan, et après une mise à jour), le launcher SHALL afficher un écran de démarrage plein écran à la place de l'accueil : la mascotte seule (le fantôme, sans TV ni nom, aucun texte), centrée, sur le fond noir de l'application (« Thème »), animée (« Animation de la mascotte »). La mascotte SHALL apparaître par un fondu court depuis le fond, jamais par un saut sec.

L'accueil SHALL être prêt quand le catalogue d'apps est chargé et que le premier visuel du héro est prêt à l'affichage (image d'un programme chargée, ou première image d'une vidéo, de programme ou nature, rendue ; le dégradé animé du repli ne compte pas comme visuel) ; l'attente de ce visuel SHALL être plafonnée à 2 s, comptées depuis la première image de l'écran de démarrage. L'écran de démarrage SHALL rester affiché au moins 600 ms depuis sa première image, et tant que l'accueil n'est pas prêt, sans dépasser 5 s au total. Dès que la durée minimale est atteinte et que l'accueil est prêt, ou au plus tard à 5 s, il SHALL laisser place à l'accueil par un fondu enchaîné, avec la courbe et une durée du design system (« Focus tvOS »), sans image noire, sans saut et sans flash ; à 5 s, l'accueil est révélé dans l'état où il se trouve et ses propres états de chargement ou d'erreur prennent le relais.

Jusqu'au début du fondu, l'accueil SHALL être composé sans être dessiné (entièrement transparent) et aucun de ses éléments ne SHALL pouvoir prendre le focus : les touches du D-pad et OK n'ont aucun effet sur lui. La touche Retour SHALL suivre le comportement normal du système (SygixOs quitte le premier plan). L'écran de démarrage SHALL ne jamais être affiché au retour sur un accueil déjà créé dans le processus (touche Home, fin d'une autre app, retour au premier plan). Hors démarrage à froid, si l'accueil est recréé alors que son catalogue n'est pas encore chargé, le launcher SHALL afficher un fond noir uni, sans mascotte ni animation, jusqu'à ce que l'accueil s'affiche.

#### Scenario: démarrage à froid
- **WHEN** l'accueil est créé pour la première fois dans le processus du launcher
- **THEN** l'écran de démarrage s'affiche : la mascotte seule, centrée, animée, sur fond noir ; aucun texte, aucun dégradé animé, aucune autre zone de l'accueil n'est visible

#### Scenario: processus démarré en arrière-plan
- **WHEN** Android a démarré le processus du launcher en arrière-plan (diffusion système, mise à jour) sans créer l'accueil, puis l'utilisateur ouvre le launcher
- **THEN** c'est un démarrage à froid : l'écran de démarrage s'affiche selon les mêmes règles

#### Scenario: accueil prêt avant la durée minimale
- **WHEN** le catalogue est chargé et le premier visuel du héro est prêt 100 ms après la première image de l'écran de démarrage
- **THEN** l'écran de démarrage reste affiché jusqu'à 600 ms après sa première image, puis le fondu vers l'accueil commence

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
- **THEN** l'accueil, jusque-là composé mais transparent, devient visible pendant que l'écran de démarrage s'efface ; le héro détient le focus dès le début du fondu (« Écran initial du home ») ; à la fin du fondu l'écran de démarrage n'est plus affiché et son animation est arrêtée

#### Scenario: touches pendant l'écran de démarrage
- **WHEN** l'utilisateur presse une touche du D-pad ou OK avant le début du fondu
- **THEN** aucun élément de l'accueil ne prend le focus ni ne réagit ; au début du fondu, le héro prend le focus comme si aucune touche n'avait été pressée

#### Scenario: Retour pendant l'écran de démarrage
- **WHEN** l'utilisateur presse Retour pendant l'écran de démarrage
- **THEN** le comportement normal du système s'applique : SygixOs quitte le premier plan, sans crash ; aucune action n'est déclenchée sur l'accueil

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
- **THEN** l'écran de démarrage affiche le fond seul, sans mascotte, avec les mêmes règles de durée et de fondu ; aucune erreur visible, aucun crash

### Requirement: Animation de la mascotte
La mascotte de l'écran de démarrage SHALL être une animation pré-rendue (flottement et clignement des yeux) de 2 s (120 images), jouée à 60 images par seconde en boucle infinie, décodée par le système sans lecteur vidéo. Le raccord entre la dernière image et la première SHALL être invisible. La mascotte SHALL être affichée à 180 dp de côté, centrée à l'écran, soit à sa résolution native (360 px) sur un écran 1080p, jamais agrandie au-delà. Autour de la mascotte, les pixels SHALL être transparents, de sorte qu'aucun cadre ni écart de teinte avec le fond ne soit visible. La première image de l'animation SHALL être une pose de repos (yeux ouverts, position centrale du flottement), utilisée telle quelle comme image fixe (« Écran de démarrage sans animation »).

#### Scenario: lecture en boucle
- **WHEN** l'écran de démarrage reste affiché plus de 2 s
- **THEN** l'animation reprend au début sans arrêt, sans image figée ni saut de position ou d'expression au raccord

#### Scenario: bords de la mascotte
- **WHEN** l'écran de démarrage est affiché
- **THEN** aucun rectangle ni halo de couleur différente du fond n'est visible autour de la mascotte

#### Scenario: taille à l'écran
- **WHEN** l'écran de démarrage est affiché sur une TV 1080p
- **THEN** la mascotte mesure 180 dp de côté, centrée horizontalement et verticalement, sans agrandissement de l'animation au-delà de sa résolution native

#### Scenario: fin de l'écran de démarrage
- **WHEN** le fondu vers l'accueil se termine
- **THEN** l'animation est arrêtée et ses ressources sont libérées ; aucune image de la mascotte n'est plus décodée tant que le processus vit

### Requirement: Fichier de l'animation de la mascotte
L'animation de la mascotte SHALL être livrée dans l'application sous la forme d'un unique fichier WebP animé qui respecte ce contrat : canevas carré de 360 × 360 px ; canal alpha présent ; nombre de répétitions 0 (boucle infinie) ; 120 images ; chaque image dure 16 ou 17 ms ; durée totale de la boucle de 2 s à 3 ms près ; poids du fichier au plus 1 Mio (1 048 576 octets). Un fichier qui ne respecte pas ce contrat SHALL faire échouer la suite de tests.

#### Scenario: fichier conforme
- **WHEN** la suite de tests s'exécute avec le fichier livré
- **THEN** le contrôle du fichier vérifie le format WebP animé, le canevas, le canal alpha, le nombre de répétitions, le nombre et la durée des images, la durée de la boucle et le poids, et réussit

#### Scenario: fichier hors contrat
- **WHEN** le fichier livré pèse plus de 1 Mio, n'a pas de canal alpha, a un canevas autre que 360 × 360 px, une boucle finie, un autre nombre d'images ou des images de durée autre que 16 ou 17 ms
- **THEN** la suite de tests échoue en nommant la contrainte non respectée

### Requirement: Enchaînement avec l'écran de lancement du système
L'écran de lancement que le système affiche au lancement d'une app (Android 12 et suivants) SHALL avoir exactement le fond noir de l'écran de démarrage et ne montrer aucune icône ni image. Entre l'appui qui lance le launcher et l'affichage de l'accueil, l'utilisateur SHALL voir au plus ce fond noir puis l'écran de démarrage : jamais deux écrans de démarrage distincts, jamais un changement de couleur de fond, jamais d'icône système, jamais de double apparition de la mascotte. La disparition de l'écran de lancement du système SHALL être invisible (aucun fondu ni zoom d'une autre couleur ou d'une icône).

#### Scenario: lancement à froid
- **WHEN** le système affiche son écran de lancement pour SygixOs
- **THEN** son fond est le noir exact du fond de l'écran de démarrage et il ne montre ni icône ni image

#### Scenario: passage à l'écran de démarrage
- **WHEN** la première image de l'écran de démarrage est dessinée
- **THEN** l'écran de lancement du système disparaît sans animation visible : aucune image intermédiaire d'une autre couleur, aucune icône ; la mascotte n'apparaît qu'une fois, par son fondu d'entrée

#### Scenario: écran de lancement absent
- **WHEN** le système n'affiche pas d'écran de lancement pour SygixOs
- **THEN** la fenêtre de l'app montre le même fond noir jusqu'à la première image de l'écran de démarrage, sans flash d'une autre couleur

#### Scenario: relance sans démarrage à froid
- **WHEN** le système affiche son écran de lancement alors que l'accueil a déjà été créé dans le processus (activité recréée)
- **THEN** cet écran a le même fond noir, sans icône, et aucun écran de démarrage n'est affiché ensuite (« Écran de démarrage », scénario « accueil recréé hors démarrage à froid »)

### Requirement: Écran de démarrage sans animation
Quand les animations sont désactivées dans le système (accessibilité « Supprimer les animations », ou échelle de durée des animations à 0 dans les options pour les développeurs), l'écran de démarrage SHALL afficher la première image de la mascotte, fixe, sans flottement ni clignement ; la mascotte SHALL apparaître sans fondu et l'accueil SHALL remplacer l'écran de démarrage sans fondu. Les règles de durée de « Écran de démarrage » (600 ms au moins, accueil prêt, plafonds de 2 s et de 5 s) restent les mêmes. Le réglage SHALL être lu à chaque démarrage à froid.

#### Scenario: animations désactivées
- **WHEN** le launcher démarre à froid alors que les animations sont désactivées dans le système
- **THEN** l'écran de démarrage montre la première image de la mascotte, immobile, dès sa première image ; dès que les règles de durée le permettent, l'accueil s'affiche d'un coup, sans fondu, le héro ayant le focus

#### Scenario: réglage changé entre deux démarrages
- **WHEN** l'utilisateur réactive les animations puis le launcher redémarre à froid
- **THEN** la mascotte est de nouveau animée et les fondus sont joués

## MODIFIED Requirements

### Requirement: Écran initial du home
Le home SHALL s'ouvrir sur un héro plein écran sans cadre : visuel d'un programme publié par les apps installées ou, à défaut, vidéo nature en boucle, avec le dock en overlay bas. Au démarrage à froid, le héro SHALL apparaître à la fin de l'écran de démarrage, par le fondu qui le termine (« Écran de démarrage »).

#### Scenario: état initial
- **WHEN** le launcher démarre
- **THEN** le héro occupe tout l'écran (aucune carte, aucun aperçu du suivant), il détient le focus dès son affichage, et le dock est visible en overlay bas semi-transparent ; au démarrage à froid, cet affichage suit l'écran de démarrage et le focus est donné au héro dès le début du fondu

#### Scenario: fallback sans contenu
- **WHEN** aucune app ne publie de programme, ou la permission est refusée
- **THEN** le héro joue les vidéos nature en boucle ; tant qu'aucune vidéo ne joue, un dégradé sombre animé est affiché (un seul passage lent, puis figé sur sa dernière position, comme le Ken Burns), jamais d'écran noir ni de crash
