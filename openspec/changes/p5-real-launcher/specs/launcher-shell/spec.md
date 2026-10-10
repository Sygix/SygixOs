# Spec Delta

## ADDED Requirements

### Requirement: Rôle d'écran d'accueil
SygixOs SHALL rester candidat au rôle système d'écran d'accueil (HOME) et SHALL permettre à l'utilisateur d'en demander l'attribution par le mécanisme officiel d'Android ou, si le rôle est indisponible ou que sa demande ne peut pas s'ouvrir, par l'écran système des applications par défaut ou de l'écran d'accueil quand l'appareil en a un, uniquement après une action explicite de sa part sur le contrôle « Remplacer le launcher » (« Réglages du launcher système » de `settings`) ou sur l'élément correspondant de la présentation initiale (« Présentation initiale du launcher »). SygixOs SHALL ne jamais demander ni modifier ce rôle sans cette action, et SHALL afficher l'état réellement attribué, relu auprès du système à chaque retour au premier plan, avec les états définis par « Réglages du launcher système » de `settings`.

#### Scenario: demande après action explicite
- **WHEN** le rôle n'est pas détenu par SygixOs et que l'utilisateur valide « Remplacer le launcher » ou l'élément HOME de la présentation initiale
- **THEN** le dialogue système de demande du rôle s'ouvre ; aucune demande n'a lieu sans cette validation

#### Scenario: rôle déjà détenu
- **WHEN** le rôle HOME est déjà attribué à SygixOs et que l'utilisateur valide « Remplacer le launcher »
- **THEN** le contrôle reste « Actif » et aucun dialogue ni écran système n'est ouvert

#### Scenario: attribution acceptée
- **WHEN** l'utilisateur accepte l'attribution dans le dialogue système
- **THEN** au retour au premier plan, SygixOs relit le rôle et l'affiche « Actif »

#### Scenario: attribution refusée
- **WHEN** l'utilisateur refuse ou ferme le dialogue système
- **THEN** au retour au premier plan, le rôle est affiché « Inactif », le launcher par défaut n'est pas modifié et SygixOs reste utilisable

#### Scenario: rôle indisponible, écran des applications par défaut présent
- **WHEN** le système déclare le rôle indisponible, ou que le dialogue de demande ne peut pas s'ouvrir, et que l'appareil a un écran système des applications par défaut ou de l'écran d'accueil
- **THEN** cet écran système s'ouvre ; au retour au premier plan, l'état du rôle est relu et affiché, et SygixOs reste utilisable

#### Scenario: écran de demande refermé aussitôt
- **WHEN** l'écran système de demande du rôle se referme aussitôt sans interface (refus répété mémorisé par le système) et que le rôle n'est pas détenu
- **THEN** SygixOs le traite comme une demande impossible à ouvrir : il ouvre l'écran système des applications par défaut ou de l'écran d'accueil quand l'appareil en a un, sinon le contrôle est « Indisponible »

#### Scenario: rôle indisponible, sans écran de remplacement
- **WHEN** le système déclare le rôle indisponible, ou que le dialogue de demande ne peut pas s'ouvrir, et que l'appareil n'a aucun écran système des applications par défaut ou de l'écran d'accueil
- **THEN** le contrôle est affiché « Indisponible », aucun écran système n'est ouvert, sans crash, et SygixOs reste utilisable

#### Scenario: rôle changé hors de SygixOs
- **WHEN** SygixOs revient au premier plan après que l'utilisateur a attribué le rôle HOME à une autre app ou à SygixOs dans les réglages système
- **THEN** l'état affiché est le rôle courant relu auprès du système, sans valeur conservée d'avant la sortie

### Requirement: Présentation initiale du launcher
Au premier lancement où elle n'a encore jamais été fermée, y compris pour une installation existante mise à jour vers une version qui l'introduit, SygixOs SHALL afficher sur l'accueil une présentation initiale, après la fin de l'écran de démarrage (« Écran de démarrage ») et après la réponse à la demande de permission « Programmes TV » (`READ_TV_LISTINGS`) quand celle-ci est affichée ; elle SHALL ne jamais s'afficher par-dessus un dialogue système. Elle SHALL être un panneau de verre centré par-dessus un voile sur l'accueil (le héro reste rendu dessous), composé d'un en-tête (mascotte fixe de l'écran de démarrage, titre « Faire de SygixOs l'écran d'accueil », description « Trois réglages pour que la TV s'ouvre toujours sur SygixOs. Rien n'est activé sans votre accord, et vous les retrouverez dans Réglages › Écran d'accueil. »), puis de trois lignes de réglage, dans cet ordre, identiques en libellé, détail, état et rendu à celles de « Réglages du launcher système » de `settings` : « Remplacer le launcher » (« Rôle d'écran d'accueil »), « Démarrer à l'allumage » (switch, « Démarrage à l'allumage ») et « Retour à l'accueil » (« Retour Home par le service d'accessibilité »), puis d'un pied avec l'aide « Retour : fermer » et le bouton « Continuer ». Elle SHALL être distincte des écrans système, ne SHALL rien activer sans action explicite et SHALL pouvoir être ignorée. Navigation D-pad : focus initial sur « Remplacer le launcher » ; haut et bas parcourent les trois lignes puis « Continuer », sans boucle ; gauche et droite sont sans effet ; le focus SHALL rester dans le panneau tant qu'il est affiché. OK sur une ligne déclenche l'action de cette ligne définie par `settings` ; OK sur « Continuer », Retour et la touche Home ferment la présentation. Une fois fermée, quel qu'en soit le moyen, elle SHALL ne plus jamais s'afficher automatiquement, et l'accueil reprend avec le focus sur le héro ; les mêmes contrôles restent dans la catégorie « Écran d'accueil » des réglages. Le rendu (dimensions, jetons, états) est fixé par les maquettes Penpot 8.1 à 8.4 et `design.md` (D9).

#### Scenario: premier lancement
- **WHEN** SygixOs se lance et que la présentation initiale n'a jamais été fermée
- **THEN** à la fin de l'écran de démarrage, la présentation s'affiche sur l'accueil : en-tête, puis « Remplacer le launcher », « Démarrer à l'allumage » et « Retour à l'accueil » dans cet ordre, chacune avec son état courant (« Inactif », switch désactivé et « Inactif » sur une TV où rien n'est encore fait, maquette 8.1), puis l'aide « Retour : fermer » et « Continuer » ; « Remplacer le launcher » a le focus

#### Scenario: navigation dans la présentation
- **WHEN** la présentation est affichée et l'utilisateur presse bas, bas, bas, bas, puis haut quatre fois, puis gauche et droite
- **THEN** le focus passe de « Remplacer le launcher » à « Démarrer à l'allumage », « Retour à l'accueil », « Continuer », puis reste sur « Continuer » (sans boucle) ; il remonte jusqu'à « Remplacer le launcher » et y reste ; gauche et droite ne le déplacent pas et aucun élément de l'accueil ne prend le focus

#### Scenario: actions des lignes
- **WHEN** l'utilisateur presse OK sur « Remplacer le launcher », sur « Démarrer à l'allumage » ou sur « Retour à l'accueil »
- **THEN** la première ligne demande le rôle HOME ou ouvre l'écran système de remplacement et passe à « En attente… » (« Rôle d'écran d'accueil ») ; la deuxième bascule son switch et persiste l'option aussitôt, le focus restant sur elle ; la troisième ouvre les réglages d'accessibilité du système et passe à « En attente… » ; la présentation reste affichée dans les trois cas

#### Scenario: fermeture par Continuer
- **WHEN** l'utilisateur presse OK sur « Continuer », quels que soient les états des trois lignes
- **THEN** la présentation est enregistrée comme fermée, disparaît, et l'accueil reprend avec le focus sur le héro ; les choix déjà faits (option de démarrage, rôle, service) restent tels quels

#### Scenario: demande de permission au premier lancement
- **WHEN** au premier lancement, la demande de permission « Programmes TV » est affichée à la fin de l'écran de démarrage
- **THEN** la présentation ne s'affiche pas tant que ce dialogue est à l'écran ; elle s'affiche dès que l'utilisateur a accordé ou refusé la permission

#### Scenario: installation existante mise à jour
- **WHEN** SygixOs se lance pour la première fois après la mise à jour d'une installation existante vers une version qui introduit la présentation
- **THEN** la présentation s'affiche une fois, avec les états courants (par exemple « Actif » si SygixOs détient déjà le rôle HOME)

#### Scenario: présentation ignorée
- **WHEN** l'utilisateur ignore la présentation (« Continuer » ou Retour) sans rien valider
- **THEN** la présentation se ferme, l'accueil reprend avec le focus sur le héro, le rôle HOME n'est pas demandé, l'option de démarrage reste désactivée et aucun écran d'accessibilité n'est ouvert

#### Scenario: pas de nouvel affichage
- **WHEN** SygixOs se lance de nouveau après la fermeture de la présentation, y compris après un redémarrage de l'appareil
- **THEN** la présentation ne s'affiche pas

#### Scenario: retour d'un écran système ouvert depuis la présentation
- **WHEN** l'utilisateur a ouvert le dialogue du rôle HOME ou les réglages d'accessibilité depuis la présentation, puis revient dans SygixOs par Retour
- **THEN** la présentation est toujours affichée, l'état de l'élément est relu auprès du système (« Actif » ou « Inactif », plus jamais « En attente… ») et le focus revient sur la ligne qui a ouvert l'écran système (maquette 8.3 : rôle « Actif », démarrage activé, focus sur « Retour à l'accueil » « Inactif »)

#### Scenario: ligne indisponible dans la présentation
- **WHEN** le rôle HOME ou l'ouverture des réglages d'accessibilité est « Indisponible » (« Rôle d'écran d'accueil », « Retour Home par le service d'accessibilité »)
- **THEN** la ligne reste affichée et focalisable avec l'état « Indisponible », son titre atténué hors focus et la raison à la place du détail (maquette 8.4) ; OK n'ouvre rien et ne change rien ; les autres lignes et « Continuer » restent utilisables

#### Scenario: choix du démarrage à l'allumage
- **WHEN** l'utilisateur choisit d'activer ou de laisser désactivé le démarrage à l'allumage dans la présentation
- **THEN** le choix est persisté aussitôt et la même valeur apparaît dans la catégorie « Écran d'accueil » des réglages

#### Scenario: processus arrêté pendant un écran système
- **WHEN** le système arrête le processus de SygixOs pendant qu'un écran système ouvert depuis la présentation est affiché, puis SygixOs se relance
- **THEN** la présentation, jamais fermée, s'affiche de nouveau avec les états relus

### Requirement: Démarrage à l'allumage
SygixOs SHALL proposer une option de démarrage à l'allumage, désactivée tant que l'utilisateur ne l'a pas activée dans la présentation initiale ou dans les réglages. Option activée, à la réception de la fin du démarrage du système, SygixOs SHALL demander l'ouverture de son accueil par les seuls mécanismes autorisés par Android, sans contourner ses restrictions. SygixOs SHALL enregistrer, pour chaque démarrage de l'appareil, si cette ouverture a été observée ; l'état « démarrage automatique non observé » qui en découle est affiché par « Réglages du launcher système » de `settings`. Option désactivée, SygixOs SHALL ne demander aucune ouverture au démarrage. L'option ne vise que le démarrage à froid de l'appareil ; la sortie de veille n'est pas couverte par cette exigence.

#### Scenario: ouverture au démarrage observée
- **WHEN** l'appareil démarre, que l'option est activée et que la demande d'ouverture aboutit
- **THEN** l'accueil de SygixOs s'affiche au premier plan et l'ouverture est enregistrée comme observée pour ce démarrage

#### Scenario: option désactivée
- **WHEN** l'appareil démarre et que l'option est désactivée
- **THEN** SygixOs ne demande pas son ouverture et n'enregistre aucun échec

#### Scenario: fin du démarrage non reçue
- **WHEN** l'option était activée avant le démarrage et que le système ou le constructeur ne transmet pas la fin du démarrage à SygixOs
- **THEN** aucune ouverture n'est observée pour ce démarrage ; au lancement suivant de SygixOs, l'état « démarrage automatique non observé » est disponible pour les réglages

#### Scenario: ouverture refusée par le système
- **WHEN** l'option était activée avant le démarrage, que la fin du démarrage est reçue mais qu'Android refuse l'ouverture de l'activité
- **THEN** aucune ouverture n'est observée pour ce démarrage, aucun crash n'a lieu, et au lancement suivant l'état « démarrage automatique non observé » est disponible pour les réglages

#### Scenario: option activée après le démarrage
- **WHEN** l'utilisateur active l'option alors que l'appareil a déjà démarré
- **THEN** le démarrage courant n'est pas compté comme un échec ; seul le prochain démarrage est évalué

### Requirement: Permission d'affichage superposé pour le démarrage
SygixOs SHALL proposer aussi la permission système « Afficher par-dessus d'autres applis » pour permettre le démarrage hors des autres exemptions Android, avec une explication et un consentement explicite avant d'ouvrir les réglages système. SygixOs SHALL ne jamais accorder la permission lui-même ni la supposer accordée ; au retour au premier plan, son état SHALL être relu auprès du système. Un refus de l'utilisateur SHALL ne pas bloquer l'utilisation de SygixOs ni effacer son choix de démarrage. Quand l'écran système de la permission n'existe pas sur l'appareil, ou ne peut pas s'ouvrir, « Démarrer à l'allumage » SHALL devenir « Indisponible », avec sa raison à la place du détail, et l'option SHALL ne pas s'activer : aucune confirmation n'est affichée, le passage de désactivé à activé est refusé, et une option activée juste avant l'échec revient à désactivé ; SygixOs reste utilisable sans crash. Cet état dure jusqu'au prochain lancement du processus, sauf si le système déclare ensuite la permission accordée ; une option déjà activée auparavant reste désactivable. Une permission accordée SHALL ne pas être présentée comme une garantie d'ouverture au démarrage sur tous les appareils. Le parcours SHALL être une confirmation distincte, affichée aussitôt quand l'utilisateur fait passer « Démarrer à l'allumage » de désactivé à activé (présentation initiale ou réglages) alors que la permission manque et que son écran système existe : un panneau de verre par-dessus l'écran courant, avec une explication, « Ouvrir les réglages » (focus initial) et « Pas maintenant », sans quatrième ligne de réglage. « Ouvrir les réglages » ouvre l'écran système de la permission ; « Pas maintenant » et Retour referment la confirmation ; sauf échec d'ouverture (ci-dessus), l'option de démarrage reste activée ; dans tous les cas le focus revient sur « Démarrer à l'allumage ». La confirmation SHALL être proposée de nouveau à la prochaine activation de l'option, et ne pas s'afficher quand la permission est déjà accordée. Haut et bas passent d'un bouton à l'autre sans boucle ; gauche et droite sont sans effet ; le focus reste dans le panneau tant qu'il est affiché.

#### Scenario: consentement explicite
- **WHEN** l'utilisateur accepte l'explication du parcours de permission
- **THEN** l'écran système de la permission s'ouvre ; aucune ouverture n'a lieu sans ce consentement et l'état n'est pas affiché comme accordé avant une relecture système

#### Scenario: permission refusée
- **WHEN** l'utilisateur répond « Pas maintenant » ou Retour, ou revient de l'écran système sans accorder la permission
- **THEN** SygixOs reste utilisable sans crash, ne suppose aucune permission accordée et conserve le choix explicite de démarrage (« démarrage automatique non observé » s'affiche si besoin) ; la confirmation est refermée et le focus est sur « Démarrer à l'allumage »

#### Scenario: écran de la permission absent
- **WHEN** l'appareil n'a pas d'écran système pour la permission et que l'option de démarrage est désactivée
- **THEN** « Démarrer à l'allumage » est affiché « Indisponible », sans switch, avec la raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage » ; OK ne l'active pas, aucune confirmation ni écran système ne s'ouvre et la ligne garde le focus

#### Scenario: écran de la permission impossible à ouvrir
- **WHEN** l'utilisateur valide « Ouvrir les réglages » et que l'écran système ne peut pas s'ouvrir
- **THEN** la confirmation se referme, l'option revient à désactivé et « Démarrer à l'allumage » devient « Indisponible » avec sa raison, focalisé ; une nouvelle activation est refusée jusqu'au prochain lancement, sauf si la permission est ensuite déclarée accordée

#### Scenario: confirmation à l'activation
- **WHEN** l'utilisateur fait passer « Démarrer à l'allumage » de désactivé à activé alors que la permission manque
- **THEN** l'option est persistée activée et la confirmation s'affiche aussitôt, focus sur « Ouvrir les réglages » ; aucun écran système n'est ouvert avant la validation de ce bouton

#### Scenario: nouvelle proposition à la prochaine activation
- **WHEN** l'utilisateur a répondu « Pas maintenant », puis désactive et réactive l'option, la permission manquant toujours
- **THEN** la confirmation s'affiche de nouveau ; elle ne s'affiche jamais à la désactivation ni quand la permission est déjà accordée

### Requirement: Retour Home par le service d'accessibilité
SygixOs SHALL fournir un service d'accessibilité qui, une fois activé par l'utilisateur dans les réglages d'accessibilité du système, ramène SygixOs au premier plan sur son accueil quand l'appui sur la touche Home est détourné par le système ou le constructeur vers un autre launcher. SygixOs SHALL ne jamais activer ni désactiver ce service lui-même : la présentation initiale et le contrôle des réglages ouvrent seulement les réglages d'accessibilité du système. Après un refus, le service SHALL n'être proposé de nouveau que depuis la catégorie « Écran d'accueil » des réglages, sans relance automatique. Le service SHALL ne lire que les événements nécessaires à ce signal et ne rien conserver. Service non activé, désactivé ou arrêté, SygixOs SHALL rester pleinement utilisable. Le service SHALL observer uniquement la touche Home via `AccessibilityService.onKeyEvent`, sans lecture du contenu des fenêtres, et SHALL ne consommer aucun événement de touche ; il SHALL n'agir que sur un appui court (relâché avant le délai d'appui long du système, sans répétition) et SHALL laisser un appui long (répétitions ou touche tenue au-delà de ce délai) au comportement du système, par exemple le tableau de bord Google TV ; il SHALL être livré même si la TV de référence ne transmet pas ce signal et SHALL ne rien faire dans ce cas. La tâche 1.3 de ce change confirme la compatibilité à la réception RC, pas avant l'implémentation.

#### Scenario: activation par l'utilisateur
- **WHEN** l'utilisateur valide l'élément accessibilité de la présentation initiale ou le contrôle correspondant des réglages
- **THEN** les réglages d'accessibilité du système s'ouvrent ; au retour au premier plan, l'état du service est relu et affiché « Actif » seulement si le système le déclare activé

#### Scenario: service refusé
- **WHEN** l'utilisateur revient des réglages d'accessibilité sans avoir activé le service, ou ferme la présentation sans l'ouvrir
- **THEN** le service est affiché « Inactif », SygixOs reste utilisable et aucune nouvelle proposition ne s'affiche automatiquement, à ce lancement comme aux suivants

#### Scenario: touche Home détournée, service activé
- **WHEN** le service a été activé par l'utilisateur, que SygixOs est en arrière-plan et que l'appui sur Home affiche le launcher du constructeur
- **THEN** SygixOs revient au premier plan sur son accueil, selon les règles de retour existantes (« Écran de démarrage », scénario « retour sur le launcher »)

#### Scenario: touche Home détournée depuis SygixOs, service activé
- **WHEN** le service a été activé par l'utilisateur, que SygixOs est au premier plan et que l'appui sur Home affiche le launcher du constructeur
- **THEN** SygixOs revient au premier plan et applique « Touche Home avec SygixOs au premier plan »

#### Scenario: appui long sur Home, service activé
- **WHEN** le service a été activé par l'utilisateur et que l'utilisateur maintient la touche Home (répétitions ou maintien au-delà du délai d'appui long)
- **THEN** le service ne consomme aucun événement et ne demande aucune ouverture : le comportement système de l'appui long (tableau de bord Google TV, par exemple) s'applique sans changement

#### Scenario: signal Home absent
- **WHEN** l'appareil ne transmet aucune touche Home au service, même activé par l'utilisateur
- **THEN** le service reste livré, ne lit aucun contenu de fenêtre, ne demande aucune ouverture et laisse le comportement système inchangé

#### Scenario: service désactivé ou arrêté
- **WHEN** l'utilisateur désactive le service dans les réglages système, ou que le système l'arrête
- **THEN** l'appui sur Home suit le comportement du système, aucune erreur n'est affichée, et au retour au premier plan le service est affiché « Inactif »

### Requirement: Touche Home avec SygixOs au premier plan
Quand la touche Home atteint SygixOs alors qu'il est déjà au premier plan, ou quand le service d'accessibilité signale un appui sur Home détourné alors que SygixOs était au premier plan (« Retour Home par le service d'accessibilité »), SygixOs SHALL fermer entièrement la page de réglages en une seule fois, quel que soit l'élément ouvert ou focalisé dans les réglages (y compris la liste déroulante « Position d'Up Next » de `settings`, que Retour ne ferait que refermer), fermer toute surcouche de l'accueil comme le ferait Retour sur cette surcouche, ramener la page de l'accueil en haut, donner le focus au héro et reprendre sa lecture. Hors premier plan, les règles de retour existantes s'appliquent sans changement (« Écran de démarrage », scénario « retour sur le launcher »).

#### Scenario: depuis la grille
- **WHEN** le focus est dans la zone grille et que la touche Home atteint SygixOs
- **THEN** la page défile jusqu'au héro comme au scénario « retour depuis la grille » de « Navigation 3 paliers », le héro prend le focus et sa lecture reprend

#### Scenario: depuis les réglages
- **WHEN** la page de réglages est ouverte, quelle que soit la catégorie ou la ligne focalisée, et que la touche Home atteint SygixOs
- **THEN** les réglages se ferment en une fois, la page de l'accueil est en haut, le héro a le focus et sa lecture reprend

#### Scenario: depuis la liste déroulante « Position d'Up Next »
- **WHEN** la liste déroulante « Position d'Up Next » est ouverte dans les réglages et que la touche Home atteint SygixOs
- **THEN** la liste se ferme sans changer la valeur et les réglages se ferment dans le même temps, sans étape intermédiaire sur la ligne « Position d'Up Next » ; la page de l'accueil est en haut, le héro a le focus et sa lecture reprend ; à la réouverture des réglages, la liste est fermée et la valeur persistée inchangée

#### Scenario: depuis une surcouche
- **WHEN** le menu contextuel d'une tuile, le mode « Déplacer » ou la présentation initiale est affiché et que la touche Home atteint SygixOs
- **THEN** la surcouche se ferme comme par Retour (déplacement annulé, présentation enregistrée comme fermée définitivement), la page de l'accueil est en haut et le héro a le focus

#### Scenario: déjà sur le héro
- **WHEN** le héro a déjà le focus, sans surcouche, et que la touche Home atteint SygixOs
- **THEN** le héro garde le focus et rien d'autre ne change

#### Scenario: pendant l'écran de démarrage
- **WHEN** la touche Home atteint SygixOs pendant l'écran de démarrage
- **THEN** l'écran de démarrage se poursuit sans changement et le héro prend le focus à son fondu de sortie (« Écran de démarrage »)
