# Spec Delta

## ADDED Requirements

### Requirement: Couverture des parcours du launcher système
Les parcours P5 SHALL être couverts par des tests déterministes, sans environnement Android réel : le rôle HOME, l'état du service d'accessibilité (activé dans les réglages du système, connecté ou non), la fin du démarrage, le numéro de démarrage, le temps écoulé depuis le démarrage, la permission d'affichage superposé et l'ouverture des écrans système SHALL être fournis par des doubles contrôlés. La logique (états des étapes, étapes affichées, progression, synthèse de la ligne des réglages, parcours affiché automatiquement, démarrage observé, filtre de la touche Home, ouverture unique au démarrage, attente de connexion du service) SHALL être testée en JUnit ; la navigation et l'affichage, par des tests Compose à la taille d'une TV (`w960dp-h540dp-xhdpi`) avec des testTags stables. Les tests du service d'accessibilité SHALL distinguer le service activé et connecté, activé mais non connecté, et non activé. Les assertions de rendu et de navigation du parcours guidé et de la ligne P5 utilisent les testTags de « Sélecteurs du launcher système ».

#### Scenario: refus du rôle HOME
- **WHEN** le test simule l'ouverture du dialogue du rôle HOME depuis l'étape 3 puis un retour au premier plan avec le rôle non détenu
- **THEN** l'étape passe par « En attente… » puis affiche « Facultatif », jamais « Fait », et l'accueil reste utilisable

#### Scenario: rôle HOME accepté ou indisponible
- **WHEN** le test simule un retour au premier plan avec le rôle détenu, puis un rôle déclaré indisponible par le système avec, puis sans, écran système des applications par défaut ou de l'écran d'accueil
- **THEN** l'étape 3 affiche « Fait » dans le premier cas ; avec l'écran de remplacement, elle est affichée et sa validation ouvre cet écran ; sans lui, l'étape 3 n'est pas affichée et aucun écran système n'est ouvert

#### Scenario: écran du rôle refermé aussitôt
- **WHEN** le test simule un résultat `RESULT_CANCELED` de l'écran du rôle HOME aussitôt après son lancement, rôle non détenu
- **THEN** l'écran système de l'écran d'accueil par défaut est ouvert quand il existe ; un refus après un délai humain n'ouvre rien d'autre

#### Scenario: états relus après retour système
- **WHEN** le test change l'état du rôle HOME, de la permission ou du service d'accessibilité pendant que SygixOs est en arrière-plan, puis simule le retour au premier plan
- **THEN** les étapes et la ligne des réglages affichent le nouvel état, sans valeur périmée, et le focus est sur l'étape qui avait ouvert l'écran système

#### Scenario: parcours ignoré
- **WHEN** le test lance SygixOs sans parcours enregistré comme fermé, simule la réponse à la demande de permission « Programmes TV » (le parcours n'apparaît qu'après), puis presse Retour sur le parcours
- **THEN** le parcours est enregistré comme fermé, le héro a le focus, aucune demande de rôle ni ouverture d'écran système n'a eu lieu, l'option de démarrage est désactivée, et un second lancement simulé n'affiche pas le parcours

#### Scenario: filtre de la touche Home
- **WHEN** le test transmet au filtre du service, service connecté, une séquence pressé puis relâché courte, une séquence avec répétitions maintenue au-delà du délai d'appui long, une séquence dont le relâchement est annulé, et une autre touche
- **THEN** tous les événements Home sont consommés (le filtre renvoie vrai pour l'appui, chaque répétition et le relâchement ; un relâchement sans appui consommé reste au système) ; les deux premières séquences demandent l'ouverture de l'accueil une seule fois chacune, au relâchement, sans délai, avec l'état « au premier plan » capturé à l'appui ; le relâchement annulé ne demande rien ; l'autre touche n'est pas consommée ; service non connecté, aucun événement n'est consommé

#### Scenario: service d'accessibilité non activé par l'utilisateur
- **WHEN** le double déclare le service non activé, ou désactivé
- **THEN** l'étape 1 affiche « À faire », aucune touche n'est consommée, un signal Home simulé ne ramène pas SygixOs au premier plan, les autres étapes restent utilisables et aucune nouvelle proposition n'est affichée automatiquement

#### Scenario: service d'accessibilité activé par l'utilisateur
- **WHEN** le double déclare le service activé et connecté et que le test simule l'appui sur Home, SygixOs étant en arrière-plan puis au premier plan
- **THEN** l'ouverture de l'accueil de SygixOs est demandée dans les deux cas ; au premier plan, le comportement de « Touche Home avec SygixOs au premier plan » s'applique ; l'étape 1 affiche « Fait »

#### Scenario: attente de connexion et service bloqué
- **WHEN** le double déclare le service activé mais non connecté au lancement du processus, puis connecté pendant l'attente bornée dans un premier test et toujours non connecté à son terme dans un second
- **THEN** pendant l'attente l'étape 1 affiche « En attente… » ; elle affiche « Fait » dans le premier cas, « Bloqué » avec son explication dans le second, sans nouvelle attente au retour au premier plan suivant, et la ligne des réglages affiche alors « Bloqué » et « Touche Home bloquée par la TV » ; service connecté dès le lancement, ou non activé, aucune attente n'a lieu et « Bloqué » n'est jamais affiché

#### Scenario: ouverture unique au démarrage
- **WHEN** le test simule, option activée, la fin du démarrage puis la connexion du service dans le même démarrage, puis la connexion du service seule, puis une connexion plus de 5 minutes après le démarrage, puis la fin du démarrage avec l'option désactivée
- **THEN** une seule ouverture est demandée dans le premier cas (aucune après l'ouverture observée), une dans le deuxième, aucune dans les deux derniers

#### Scenario: démarrage automatique non observé
- **WHEN** le test simule un démarrage (numéro de démarrage incrémenté) avec l'option activée avant ce démarrage, sans ouverture observée, puis affiche le parcours
- **THEN** « onboarding-step-boot-detail » indique que le démarrage automatique n'a pas eu lieu (« SygixOs ne s'est pas ouvert automatiquement à ce démarrage ») et « onboarding-step-boot-switch » reste activé ; avec une ouverture au démarrage observée, ou l'option désactivée, ou l'option activée après ce démarrage, le détail normal est affiché

#### Scenario: touche Home au premier plan
- **WHEN** le test ouvre les réglages (y compris avec la liste déroulante « Position d'À suivre » ouverte, et avec le parcours guidé ouvert par-dessus), ou descend dans la grille, ou ouvre le menu contextuel d'une tuile, ou active le mode « Déplacer », ou affiche le parcours au premier lancement, puis transmet un intent Home à l'activité au premier plan
- **THEN** réglages et surcouche sont fermés en une fois (déplacement annulé, valeur de « Position d'À suivre » inchangée, parcours affiché automatiquement enregistré comme fermé), la page de l'accueil est en haut et le héro a le focus ; un intent d'ouverture au démarrage reçu au premier plan ne produit pas cet effet

#### Scenario: mise à jour sans demande de rôle
- **WHEN** le test exécute une mise à jour intégrée factice jusqu'à la relance, avec un double du rôle HOME non détenu
- **THEN** aucune demande du rôle HOME n'est faite par le parcours de mise à jour

### Requirement: Sélecteurs du launcher système
Le parcours guidé et la ligne P5 de « Écran d'accueil » SHALL exposer des testTags stables, utilisés par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) à la place du texte ou de la position : « onboarding-launcher » (voile et panneau du parcours guidé), « onboarding-mascot », « onboarding-title », « onboarding-progress », « onboarding-step-accessibility » (étape 1), « onboarding-step-boot » (étape 2), « onboarding-step-overlay » (sous-étape 2b), « onboarding-step-home-role » (étape 3), « onboarding-done » (bouton « Terminé »), « onboarding-hint » ; « setting-launcher-group » (intertitre « Launcher système »), « setting-launcher-guide » (ligne « SygixOs comme écran d'accueil ») ; pour toute étape ou ligne « <ligne> » ci-dessus, « <ligne>-title » (titre), « <ligne>-detail » (détail), « <ligne>-state » (libellé d'état, absent pour l'étape 2 tant qu'elle porte son switch), « <ligne>-dot » (pastille d'état, absente quand l'état n'en a pas), et, pour les étapes, « <ligne>-marker » (numéro ou coche) ; pour l'étape 2, « onboarding-step-boot-switch ». Les anciens testTags « onboarding-row-* », « onboarding-continue », « setting-home-role », « setting-boot-start », « setting-accessibility-home » et « launcher-overlay-* » n'existent plus. Les tests SHALL vérifier l'ordre, les étapes affichées, le focus initial, la navigation D-pad, les états affichés, la progression et la fermeture définitive du parcours affiché automatiquement définis par « Parcours guidé du launcher » de `launcher-shell`, et la synthèse définie par « Réglages du launcher système » de `settings`.

#### Scenario: ordre et focus initial du parcours
- **WHEN** le test compose l'accueil avec le parcours à afficher, après la fin de l'écran de démarrage et la réponse à la demande de permission, rien n'étant fait et le rôle disponible
- **THEN** « onboarding-launcher » est affiché par-dessus « zone-hero » ; « onboarding-mascot » est à gauche de « onboarding-title » sur la même ligne ; « onboarding-progress » affiche « 0 étape sur 2 terminée » ; « onboarding-step-accessibility », « onboarding-step-boot » puis « onboarding-step-home-role » sont affichés de haut en bas, sans « onboarding-step-overlay », puis le pied, « onboarding-hint » à gauche de « onboarding-done » sur la même ligne ; « onboarding-step-accessibility » a le focus sans aucune touche pressée ; « onboarding-step-boot-switch » est désactivé ; « onboarding-step-home-role-state » affiche « Facultatif » ; le rectangle de chaque étape est identique avec et sans le focus

#### Scenario: navigation dans le parcours
- **WHEN** le test presse bas quatre fois, haut quatre fois, puis gauche et droite depuis chaque élément focalisable
- **THEN** le focus suit « onboarding-step-accessibility », « onboarding-step-boot », « onboarding-step-home-role », « onboarding-done » et y reste ; il remonte jusqu'à « onboarding-step-accessibility » et y reste ; gauche et droite ne changent pas le focus ; « zone-hero », « zone-dock », « hero-capsule » et « settings-gear » ne prennent jamais le focus tant que « onboarding-launcher » est affiché

#### Scenario: sous-étape 2b et progression
- **WHEN** le double déclare le service connecté et la permission d'affichage superposé absente avec son écran disponible, puis le test presse OK sur « onboarding-step-boot », puis OK sur « onboarding-step-overlay », simule le passage en arrière-plan puis le retour avec la permission accordée
- **THEN** après le premier OK, « onboarding-step-overlay » apparaît entre « onboarding-step-boot » et « onboarding-step-home-role », son état est « À faire », le focus reste sur « onboarding-step-boot », dont « onboarding-step-boot-marker » porte toujours le numéro 2, aucun autre panneau n'apparaît et « onboarding-progress » affiche « 1 étape sur 3 terminée » ; pendant l'absence, « onboarding-step-overlay-state » affiche « En attente… » ; au retour il affiche « Fait », « onboarding-step-overlay » a le focus et « onboarding-progress » affiche « 3 étapes sur 3 terminées » ; OK sur « onboarding-step-boot » (désactivation) retire « onboarding-step-overlay »

#### Scenario: états affichés
- **WHEN** le test compose une étape ou la ligne des réglages dans chacun des états « Fait », « En attente… », « À faire », « Bloqué », « Indisponible », « Facultatif », « Terminé » et « Partiel » qui la concernent, avec et sans le focus
- **THEN** « <ligne>-state » affiche le libellé de l'état ; « <ligne>-dot » existe, à gauche de « <ligne>-state », pour « Fait » et « Terminé » (couleur `SwitchOn`) et « En attente… » (couleur `Badge`), et n'existe pas pour les autres ; « <ligne>-marker » porte une coche pour une étape « Fait » et son numéro sinon (« 2b » pour la sous-étape) ; en « Indisponible », « <ligne>-detail » affiche la raison propre à l'étape, le titre a la couleur secondaire hors focus, l'étape prend le focus, OK n'ouvre aucun écran et la progression ne la compte pas ; en « Bloqué », « onboarding-step-accessibility-detail » affiche l'explication et OK ouvre les réglages d'accessibilité ; le libellé d'état n'est jamais « Fait » tant que le double n'a pas confirmé l'état

#### Scenario: étape 3 masquée
- **WHEN** le double déclare le rôle indisponible sans écran système de remplacement
- **THEN** « onboarding-step-home-role » n'existe pas et bas depuis la dernière étape obligatoire donne le focus à « onboarding-done »

#### Scenario: fermeture définitive du parcours
- **WHEN** le test, dans trois compositions distinctes, presse OK sur « onboarding-done », presse Retour, puis transmet un intent Home à l'activité au premier plan, chaque fois avec « onboarding-launcher » affiché automatiquement
- **THEN** dans les trois cas « onboarding-launcher » n'existe plus, le drapeau de fermeture est persisté, « zone-hero » a le focus et un second lancement simulé n'affiche pas « onboarding-launcher » ; après OK sur « onboarding-step-boot » avant la fermeture, l'option de démarrage persistée est activée

#### Scenario: retour système dans le parcours
- **WHEN** le test presse OK sur « onboarding-step-accessibility », simule le passage en arrière-plan puis le retour au premier plan avec le double déclarant le service activé et connecté
- **THEN** pendant l'absence « onboarding-step-accessibility-state » affiche « En attente… » avec « onboarding-step-accessibility-dot » ; au retour il affiche « Fait », « onboarding-step-accessibility-marker » porte une coche, « onboarding-launcher » est toujours affiché et « onboarding-step-accessibility » a le focus

#### Scenario: ordre des contrôles de la catégorie
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** « setting-upnext-visible », « setting-upnext-position », « setting-launcher-group » et « setting-launcher-guide » sont affichés de haut en bas dans cet ordre ; « setting-upnext-visible » a le focus ; « setting-launcher-group » n'est pas focalisable ; aucun des anciens testTags P5 n'existe

#### Scenario: navigation dans la catégorie
- **WHEN** le test, depuis « setting-upnext-position » liste fermée, presse bas deux fois, haut deux fois, puis bas, droite, gauche, et Retour depuis « setting-launcher-guide »
- **THEN** le focus passe à « setting-launcher-guide » et y reste, sans jamais passer par « setting-launcher-group » ; il remonte jusqu'à « setting-upnext-visible » puis redescend ; droite ne fait rien ; gauche rend le focus à « settings-category-HOME_SCREEN » ; Retour ferme les réglages

#### Scenario: parcours ouvert depuis les réglages
- **WHEN** le test presse OK sur « setting-launcher-guide », puis Retour, puis de nouveau OK et OK sur « onboarding-done »
- **THEN** « onboarding-launcher » s'affiche par-dessus « settings-home-screen » ; après Retour comme après « onboarding-done », il n'existe plus, les réglages sont toujours affichés, « setting-launcher-guide » a le focus et le drapeau de fermeture du parcours automatique n'est pas modifié

#### Scenario: synthèse de la ligne des réglages
- **WHEN** le test affiche « Écran d'accueil » avec l'étape 1 « Fait » et l'option désactivée, puis tout fait, puis l'étape 1 « Bloqué », puis rien de fait
- **THEN** « setting-launcher-guide-state » et « setting-launcher-guide-detail » affichent respectivement « Partiel » et « Démarrage à l'allumage à terminer », « Terminé » (avec « setting-launcher-guide-dot ») et « Toutes les étapes sont faites », « Bloqué » et « Touche Home bloquée par la TV », « À faire » et « Touche Home et démarrage à l'allumage à terminer »

#### Scenario: démarrage indisponible sans écran de permission
- **WHEN** le double déclare l'écran système de la permission absent, ou fait échouer son ouverture depuis « onboarding-step-overlay »
- **THEN** « onboarding-step-boot-state » affiche « Indisponible », « onboarding-step-boot-detail » affiche la raison, « onboarding-step-boot-switch », « onboarding-step-boot-dot » et « onboarding-step-overlay » n'existent pas, l'étape garde le focus, OK ne l'active pas et l'option persistée est désactivée
