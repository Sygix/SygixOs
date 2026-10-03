# Delta launcher-shell

## ADDED Requirements

### Requirement: Écran de démarrage
Au démarrage à froid du launcher (son processus n'existait pas), tant que l'accueil n'est pas prêt à s'afficher, le launcher SHALL afficher un écran de démarrage plein écran à la place de l'accueil : la mascotte seule (le fantôme, sans TV ni nom, aucun texte), centrée, sur le fond de l'application (« Thème »), animée (« Animation de la mascotte »). La mascotte SHALL apparaître par un fondu court depuis le fond, jamais par un saut sec. L'écran de démarrage SHALL rester affiché au moins 600 ms, comptées depuis sa première image, et tant que l'accueil n'est pas prêt ; dès que les deux conditions sont remplies, il SHALL laisser place à l'accueil par un fondu enchaîné, avec la courbe et une durée du design system (« Focus tvOS »), sans image noire, sans saut et sans flash. L'écran de démarrage SHALL ne jamais être affiché au retour sur un launcher dont le processus est vivant (touche Home, fin d'une autre app, retour au premier plan). Il n'est jamais focusable et n'est pas un palier de l'accueil : les touches pressées avant le début du fondu SHALL n'avoir aucun effet et ne pas être rejouées sur l'accueil.

#### Scenario: démarrage à froid
- **WHEN** le launcher démarre alors que son processus n'existait pas
- **THEN** l'écran de démarrage s'affiche : la mascotte seule, centrée, animée, sur le fond de l'application ; aucun texte, aucun dégradé animé, aucune autre zone de l'accueil n'est visible

#### Scenario: accueil prêt avant la durée minimale
- **WHEN** l'accueil est prêt 100 ms après la première image de l'écran de démarrage
- **THEN** l'écran de démarrage reste affiché jusqu'à 600 ms après sa première image, puis le fondu vers l'accueil commence

#### Scenario: accueil prêt après la durée minimale
- **WHEN** l'accueil n'est prêt que 2 s après la première image de l'écran de démarrage
- **THEN** l'écran de démarrage reste affiché et animé jusqu'à ce que l'accueil soit prêt, puis le fondu commence aussitôt, sans attente supplémentaire

#### Scenario: fondu vers l'accueil
- **WHEN** le fondu vers l'accueil commence
- **THEN** l'accueil est déjà rendu sous l'écran de démarrage, l'écran de démarrage s'efface progressivement jusqu'à laisser l'accueil seul ; le héro détient le focus dès le début du fondu (« Écran initial du home ») ; à la fin du fondu l'écran de démarrage n'est plus affiché et son animation est arrêtée

#### Scenario: touches pendant l'écran de démarrage
- **WHEN** l'utilisateur presse une touche du D-pad, OK ou Retour avant le début du fondu
- **THEN** rien ne se passe : aucune action sur l'accueil, le launcher ne se ferme pas, et la touche n'est pas rejouée sur l'accueil ; l'écran de démarrage se termine selon les règles de durée ci-dessus

#### Scenario: retour sur le launcher
- **WHEN** le launcher revient au premier plan (touche Home, fin d'une autre app) alors que son processus est vivant
- **THEN** aucun écran de démarrage n'est affiché ; l'accueil s'affiche directement, selon les règles de retour existantes (« Navigation 3 paliers »)

#### Scenario: processus arrêté par le système
- **WHEN** le système a arrêté le processus du launcher (manque de mémoire) et l'utilisateur revient sur le launcher
- **THEN** c'est un démarrage à froid : l'écran de démarrage s'affiche selon les mêmes règles

#### Scenario: demande de permission au premier lancement
- **WHEN** la demande système de la permission `READ_TV_LISTINGS` s'affiche pendant l'écran de démarrage
- **THEN** la demande apparaît par-dessus ; l'écran de démarrage et le fondu vers l'accueil se poursuivent sans attendre la réponse, qui est traitée comme avant (« Contenu héro TV Provider », scénario « permission »)

#### Scenario: animation illisible
- **WHEN** l'animation de la mascotte ne peut pas être décodée (fichier absent ou corrompu)
- **THEN** l'écran de démarrage affiche le fond seul, sans mascotte, avec les mêmes règles de durée et de fondu ; aucune erreur visible, aucun crash

### Requirement: Animation de la mascotte
La mascotte de l'écran de démarrage SHALL être une animation pré-rendue (flottement et clignement des yeux), jouée à 60 images par seconde en boucle infinie, décodée par le système sans lecteur vidéo. Le raccord entre la dernière image et la première SHALL être invisible. La mascotte SHALL être affichée à une taille fixe, centrée à l'écran, à sa résolution native sur un écran 1080p (jamais agrandie au-delà). Autour de la mascotte, aucun cadre ni écart de teinte avec le fond de l'écran de démarrage ne SHALL être visible. La première image de l'animation SHALL être une pose de repos (yeux ouverts, position centrale du flottement), utilisée telle quelle comme image fixe (« Écran de démarrage sans animation »).

#### Scenario: lecture en boucle
- **WHEN** l'écran de démarrage reste affiché plus longtemps qu'une boucle de l'animation
- **THEN** l'animation reprend au début sans arrêt, sans image figée ni saut de position ou d'expression au raccord

#### Scenario: bords de la mascotte
- **WHEN** l'écran de démarrage est affiché
- **THEN** aucun rectangle ni halo de couleur différente du fond n'est visible autour de la mascotte

#### Scenario: taille à l'écran
- **WHEN** l'écran de démarrage est affiché sur une TV 1080p
- **THEN** la mascotte est centrée horizontalement et verticalement, à une taille fixe qui n'agrandit pas l'animation au-delà de sa résolution native

#### Scenario: fin de l'écran de démarrage
- **WHEN** le fondu vers l'accueil se termine
- **THEN** l'animation est arrêtée et ses ressources sont libérées ; aucune image de la mascotte n'est plus décodée tant que le processus vit

### Requirement: Fichier de l'animation de la mascotte
L'animation de la mascotte SHALL être livrée dans l'application sous la forme d'un unique fichier WebP animé qui respecte ce contrat : canevas carré dont le côté vaut deux fois la taille d'affichage en dp (résolution native d'un écran 1080p, densité 2) et au plus 512 px ; nombre de répétitions 0 (boucle infinie) ; chaque image dure 16 ou 17 ms (60 images par seconde) ; durée totale de la boucle comprise entre 1 et 4 s ; poids du fichier au plus 1 Mio (1 048 576 octets) ; pixels hors de la mascotte transparents (canal alpha) ou exactement de la couleur du fond de l'écran de démarrage. Un fichier qui ne respecte pas ce contrat SHALL faire échouer la suite de tests.

#### Scenario: fichier conforme
- **WHEN** la suite de tests s'exécute avec le fichier livré
- **THEN** le contrôle du fichier vérifie le format WebP animé, le canevas, le nombre de répétitions, la durée de chaque image, la durée de la boucle et le poids, et réussit

#### Scenario: fichier hors budget
- **WHEN** le fichier livré pèse plus de 1 Mio, a un canevas non carré ou plus grand que 512 px, une boucle finie, ou des images de durée autre que 16 ou 17 ms
- **THEN** la suite de tests échoue en nommant la contrainte non respectée

### Requirement: Enchaînement avec l'écran de lancement du système
L'écran de lancement que le système affiche au lancement d'une app (Android 12 et suivants) SHALL avoir exactement la couleur de fond de l'écran de démarrage et ne montrer ni l'icône du launcher ni aucune image qui diffère, en taille ou en position, de la mascotte telle qu'elle apparaît dans l'écran de démarrage. Entre l'appui qui lance le launcher et l'affichage de l'accueil, l'utilisateur SHALL voir au plus ce fond puis l'écran de démarrage : jamais deux écrans de démarrage distincts, jamais un changement de couleur de fond, jamais d'icône système, jamais de saut ni de double apparition de la mascotte. La disparition de l'écran de lancement du système SHALL être invisible (aucun fondu ni zoom d'une autre couleur ou d'une icône).

#### Scenario: lancement à froid
- **WHEN** le système affiche son écran de lancement pour SygixOs
- **THEN** son fond a la couleur exacte du fond de l'écran de démarrage et il ne montre aucune icône ni image différente de la mascotte de l'écran de démarrage

#### Scenario: passage à l'écran de démarrage
- **WHEN** la première image de l'écran de démarrage est dessinée
- **THEN** l'écran de lancement du système disparaît sans animation visible : aucune image intermédiaire d'une autre couleur, aucune icône, aucun saut de la mascotte

#### Scenario: écran de lancement absent
- **WHEN** le système n'affiche pas d'écran de lancement pour SygixOs
- **THEN** la fenêtre de l'app montre le même fond jusqu'à la première image de l'écran de démarrage, sans flash d'une autre couleur

#### Scenario: relance sans démarrage à froid
- **WHEN** le système affiche son écran de lancement alors que le processus du launcher est vivant (activité recréée)
- **THEN** cet écran a le même fond, sans icône, et aucun écran de démarrage n'est affiché ensuite (« Écran de démarrage », scénario « retour sur le launcher »)

### Requirement: Écran de démarrage sans animation
Quand les animations sont désactivées dans le système (accessibilité « Supprimer les animations », ou échelle de durée des animations à 0 dans les options pour les développeurs), l'écran de démarrage SHALL afficher la première image de la mascotte, fixe, sans flottement ni clignement ; la mascotte SHALL apparaître sans fondu et l'accueil SHALL remplacer l'écran de démarrage sans fondu. Les règles de durée de « Écran de démarrage » (600 ms au moins, attente de l'accueil) restent les mêmes. Le réglage SHALL être lu à chaque démarrage à froid.

#### Scenario: animations désactivées
- **WHEN** le launcher démarre à froid alors que les animations sont désactivées dans le système
- **THEN** l'écran de démarrage montre la première image de la mascotte, immobile, dès sa première image ; après 600 ms et quand l'accueil est prêt, l'accueil s'affiche d'un coup, sans fondu, le héro ayant le focus

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
- **THEN** le héro joue les vidéos nature en boucle ; tant qu'aucune vidéo ne joue, un dégradé sombre animé est affiché, jamais d'écran noir ni de crash
