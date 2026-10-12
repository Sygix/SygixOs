# Spec Delta

## ADDED Requirements

### Requirement: Couverture de la configuration automatique
La configuration automatique, l'accès par le débogage, la désactivation et le rétablissement de l'accueil d'origine (`launcher-shell`) SHALL être couverts par des tests déterministes, sans connexion ADB réelle ni appareil : l'accès par le débogage, les opérations privilégiées et la lecture de l'état de l'écran d'accueil SHALL être fournis par des doubles contrôlés (`design.md`, D10). La logique (états de l'accès, séquence et ses étapes sans objet, repli, échecs indépendants, délais, perte de l'accès, garde-fous, ordre et annulation de la désactivation, rétablissement avec et sans écran d'accueil précédent relevé, refus des cibles hors de l'ensemble fermé, conservation des autres services d'accessibilité, fermeture de la connexion) SHALL être testée en JUnit ; les pages, le guide, les confirmations et « Détails », par des tests Compose à la taille d'une TV (`w960dp-h540dp-xhdpi`) avec les testTags de « Sélecteurs du launcher système ». Aucun test ne SHALL présenter un succès que le double n'a pas confirmé par relecture.

#### Scenario: séquence sans désactivation
- **WHEN** le double d'accès est prêt, que le double d'état rend SygixOs écran d'accueil effectif après la définition de l'écran d'accueil, et que le test presse OK sur « onboarding-auto-action »
- **THEN** l'autorisation de démarrage est demandée puis relue, l'écran d'accueil précédent est relevé, aucune confirmation n'apparaît, « onboarding-auto-status » affiche « SygixOs est l'écran d'accueil » et « onboarding-choice-auto-state » affiche « Configuré » au retour sur la page de choix

#### Scenario: désactivation proposée puis refusée
- **WHEN** le double d'état ne rend pas SygixOs écran d'accueil effectif après la définition, avec des composants actifs et modifiables, et que le test presse « launcher-confirm-disable-cancel »
- **THEN** aucune opération de désactivation n'est envoyée, le service d'accessibilité est ajouté sans retirer les autres services du double, l'affichage superposé est accordé, l'option de démarrage persistée est activée, et « onboarding-auto-status » affiche « La touche Home ouvre SygixOs »

#### Scenario: désactivation partielle annulée
- **WHEN** le double fait échouer la désactivation du deuxième composant après la confirmation
- **THEN** le premier composant est remis à son état par défaut, l'état est relu, aucun succès n'est affiché et les solutions de repli sont posées

#### Scenario: accès non prêt
- **WHEN** le double déclare le débogage désactivé, puis la connexion impossible, puis l'autorisation refusée, et que le test presse OK sur « onboarding-auto-action »
- **THEN** « onboarding-debug-guide » s'affiche avec ses quatre étapes et « onboarding-debug-state » explique chaque état ; aucune opération privilégiée n'est envoyée ; quand le double passe à prêt et que le test presse « onboarding-debug-continue », la séquence démarre

#### Scenario: échecs indépendants et délais
- **WHEN** le double fait échouer l'autorisation de démarrage, puis, dans un second test, fait dépasser son délai à la définition de l'écran d'accueil
- **THEN** dans le premier cas la définition de l'écran d'accueil est quand même faite ; dans le second, « onboarding-auto-status » affiche l'échec, « onboarding-auto-action » affiche « Réessayer », aucune nouvelle tentative n'a lieu sans action et la connexion est fermée

#### Scenario: politique d'auto-démarrage du constructeur
- **WHEN** le double expose une politique sans SygixOs dans ses listes blanches, puis une politique où il figure déjà, puis une politique de structure inattendue
- **THEN** dans le premier cas la valeur d'origine est sauvegardée et la valeur écrite ne diffère que par l'ajout de `fr.sygix.sygixos` aux deux listes ; dans le deuxième rien n'est écrit ; dans le troisième rien n'est écrit et l'inscription est en échec sans arrêter la séquence ; aucun rétablissement ne retire l'inscription

#### Scenario: voie indisponible sur cette TV
- **WHEN** le double déclare le débogage activé et la connexion impossible après « onboarding-debug-continue »
- **THEN** « onboarding-debug-state » affiche que la configuration automatique n'est pas disponible sur cette TV, « onboarding-debug-manual » remplace « onboarding-debug-continue » et ouvre « onboarding-manual », et « onboarding-choice-auto-state » affiche « Indisponible »

#### Scenario: cible refusée
- **WHEN** le test demande à la passerelle une opération sur un paquet hors du paquet SygixOs et de l'ensemble fermé
- **THEN** la passerelle la refuse sans rien envoyer au double

#### Scenario: rétablissement
- **WHEN** le double déclare deux composants désactivés par l'utilisateur et un écran d'accueil précédent relevé, et que le test presse « onboarding-auto-restore » puis « launcher-confirm-restore-accept »
- **THEN** les composants sont remis à leur état par défaut dans l'ordre inverse de l'ensemble, l'écran d'accueil précédent est redéfini, l'état est relu, « onboarding-auto-restore » n'existe plus et « onboarding-auto-action » a le focus ; sans écran d'accueil précédent relevé, seuls les composants sont rétablis

#### Scenario: perte de l'accès pendant la séquence
- **WHEN** le double perd la connexion pendant la désactivation
- **THEN** la séquence s'arrête, l'état est relu, l'échec est affiché, sans succès supposé, et « onboarding-auto-restore » est proposé si un composant reste désactivé

## MODIFIED Requirements

### Requirement: Couverture des parcours du launcher système
Les parcours du launcher système SHALL être couverts par des tests déterministes, sans environnement Android réel : le rôle HOME, l'état du service d'accessibilité (activé dans les réglages du système, connecté ou non), la fin du démarrage, le numéro de démarrage, le temps écoulé depuis le démarrage, la permission d'affichage superposé, l'écran d'auto-démarrage du constructeur, l'ouverture des écrans système, l'accès par le débogage et l'état de l'écran d'accueil SHALL être fournis par des doubles contrôlés. La logique (pages et voies du parcours, états des étapes, étapes affichées, progression, états des deux voies, synthèse de la ligne des réglages, parcours affiché automatiquement, démarrage observé, filtre de la touche Home, ouverture unique au démarrage, attente de connexion du service) SHALL être testée en JUnit ; la navigation et l'affichage, par des tests Compose à la taille d'une TV (`w960dp-h540dp-xhdpi`) avec des testTags stables. Les tests du service d'accessibilité SHALL distinguer le service activé et connecté, activé mais non connecté, et non activé. Les assertions de rendu et de navigation du parcours guidé et de la ligne des réglages utilisent les testTags de « Sélecteurs du launcher système ».

#### Scenario: refus du rôle HOME
- **WHEN** le test simule l'ouverture du dialogue du rôle HOME depuis l'étape 3 de la page « Par autorisations » puis un retour au premier plan avec le rôle non détenu
- **THEN** l'étape passe par « En attente… » puis affiche « Facultatif », jamais « Fait », et l'accueil reste utilisable

#### Scenario: rôle HOME accepté ou indisponible
- **WHEN** le test simule un retour au premier plan avec le rôle détenu, puis un rôle déclaré indisponible par le système avec, puis sans, écran système des applications par défaut ou de l'écran d'accueil
- **THEN** l'étape 3 affiche « Fait » dans le premier cas ; avec l'écran de remplacement, elle est affichée et sa validation ouvre cet écran ; sans lui, l'étape 3 n'est pas affichée et aucun écran système n'est ouvert

#### Scenario: écran du rôle refermé aussitôt
- **WHEN** le test simule un résultat `RESULT_CANCELED` de l'écran du rôle HOME aussitôt après son lancement, rôle non détenu
- **THEN** l'écran système de l'écran d'accueil par défaut est ouvert quand il existe ; un refus après un délai humain n'ouvre rien d'autre

#### Scenario: états relus après retour système
- **WHEN** le test change l'état du rôle HOME, de la permission, du service d'accessibilité ou de l'écran d'accueil effectif pendant que SygixOs est en arrière-plan, puis simule le retour au premier plan
- **THEN** les étapes, les états des deux voies et la ligne des réglages affichent le nouvel état, sans valeur périmée, et le focus est sur l'étape qui avait ouvert l'écran système

#### Scenario: parcours ignoré
- **WHEN** le test lance SygixOs sans parcours enregistré comme fermé, simule la réponse à la demande de permission « Programmes TV » (le parcours n'apparaît qu'après), puis presse Retour sur la page de choix
- **THEN** le parcours est enregistré comme fermé, le héro a le focus, aucune demande de rôle, connexion au débogage ni ouverture d'écran système n'a eu lieu, l'option de démarrage est désactivée, et un second lancement simulé n'affiche pas le parcours

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
- **THEN** pendant l'attente l'étape 1 affiche « En attente… » ; elle affiche « Fait » dans le premier cas, « Bloqué » avec son explication dans le second, sans nouvelle attente au retour au premier plan suivant, et la voie « Par autorisations » est alors « Bloqué » ; service connecté dès le lancement, ou non activé, aucune attente n'a lieu et « Bloqué » n'est jamais affiché

#### Scenario: ouverture unique au démarrage
- **WHEN** le test simule, option activée, la fin du démarrage puis la connexion du service dans le même démarrage, puis la connexion du service seule, puis une connexion plus de 5 minutes après le démarrage, puis la fin du démarrage avec l'option désactivée
- **THEN** une seule ouverture est demandée dans le premier cas (aucune après l'ouverture observée), une dans le deuxième, aucune dans les deux derniers

#### Scenario: démarrage automatique non observé
- **WHEN** le test simule un démarrage (numéro de démarrage incrémenté) avec l'option activée avant ce démarrage, sans ouverture observée, puis affiche la page « Par autorisations »
- **THEN** « onboarding-step-boot-detail » indique que le démarrage automatique n'a pas eu lieu (« SygixOs ne s'est pas ouvert automatiquement à ce démarrage ») et « onboarding-step-boot-switch » reste activé ; avec une ouverture au démarrage observée, ou l'option désactivée, ou l'option activée après ce démarrage, le détail normal est affiché

#### Scenario: touche Home au premier plan
- **WHEN** le test ouvre les réglages (y compris avec la liste déroulante « Position d'À suivre » ouverte, et avec le parcours guidé ouvert par-dessus sur chacune de ses pages ou avec une confirmation affichée), ou descend dans la grille, ou ouvre le menu contextuel d'une tuile, ou active le mode « Déplacer », ou affiche le parcours au premier lancement, puis transmet un intent Home à l'activité au premier plan
- **THEN** réglages et surcouche sont fermés en une fois (déplacement annulé, valeur de « Position d'À suivre » inchangée, parcours affiché automatiquement enregistré comme fermé, confirmation de désactivation traitée comme « Annuler »), la page de l'accueil est en haut et le héro a le focus ; un intent d'ouverture au démarrage reçu au premier plan ne produit pas cet effet

#### Scenario: mise à jour sans demande de rôle
- **WHEN** le test exécute une mise à jour intégrée factice jusqu'à la relance, avec un double du rôle HOME non détenu
- **THEN** aucune demande du rôle HOME ni connexion au débogage n'est faite par le parcours de mise à jour

### Requirement: Sélecteurs du launcher système
Le parcours guidé et la ligne de la catégorie « Écran d'accueil » SHALL exposer des testTags stables, utilisés par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) à la place du texte ou de la position : « onboarding-launcher » (voile et panneau du parcours), « onboarding-mascot », « onboarding-title », « onboarding-hint » ; page de choix : « onboarding-choice-auto », « onboarding-choice-manual », « onboarding-close » (bouton « Plus tard » ou « Fermer ») ; page « Par autorisations » : « onboarding-manual », « onboarding-progress », « onboarding-step-accessibility » (étape 1), « onboarding-step-boot » (étape 2), « onboarding-step-overlay » (sous-étape 2b), « onboarding-step-vendor-boot » (sous-étape 2c), « onboarding-step-home-role » (étape 3), « onboarding-done » (bouton « Terminé »), « onboarding-step-boot-switch » ; page « Configuration automatique » : « onboarding-auto », « onboarding-auto-action » (action principale), « onboarding-auto-status » (opération en cours ou résultat), « onboarding-auto-restore », « onboarding-auto-details », « onboarding-auto-debug-advice », « onboarding-auto-open-developer » ; guide du débogage : « onboarding-debug-guide », « onboarding-debug-step-1 » à « onboarding-debug-step-4 », « onboarding-debug-state », « onboarding-debug-open-settings », « onboarding-debug-continue », « onboarding-debug-manual » (« Configurer par autorisations », à la place de « onboarding-debug-continue » quand la voie est indisponible) ; « Détails » : « onboarding-details » et « onboarding-details-item-<opération> » ; confirmations : « launcher-confirm-disable » et « launcher-confirm-restore », chacune avec « <dialogue>-title », « <dialogue>-text », « <dialogue>-accept », « <dialogue>-cancel » et « <dialogue>-details » ; réglages : « setting-launcher-group » (intertitre « Launcher système »), « setting-launcher-guide » (ligne « Configurer SygixOs comme launcher principal ») ; pour toute option, étape ou ligne « <ligne> » ci-dessus, « <ligne>-title » (titre), « <ligne>-detail » (détail), « <ligne>-state » (libellé d'état, absent pour l'étape 2 tant qu'elle porte son switch), « <ligne>-dot » (pastille d'état, absente quand l'état n'en a pas), et, pour les étapes, « <ligne>-marker » (numéro ou coche). Les anciens testTags « onboarding-row-* », « onboarding-continue », « setting-home-role », « setting-boot-start », « setting-accessibility-home » et « launcher-overlay-* » n'existent plus. Les tests SHALL vérifier l'ordre, les pages, les étapes affichées, le focus initial, la navigation D-pad, les états affichés, la progression et la fermeture définitive du parcours affiché automatiquement définis par « Parcours guidé du launcher » et « Configuration automatique de l'écran d'accueil » de `launcher-shell`, et la synthèse définie par « Réglages du launcher système » de `settings`.

#### Scenario: ordre et focus initial du parcours
- **WHEN** le test compose l'accueil avec le parcours à afficher, après la fin de l'écran de démarrage et la réponse à la demande de permission, rien n'étant fait
- **THEN** « onboarding-launcher » est affiché par-dessus « zone-hero » ; « onboarding-mascot » est à gauche de « onboarding-title » sur la même ligne ; « onboarding-choice-auto » puis « onboarding-choice-manual » sont affichés de haut en bas, avec les états « À faire » ; « onboarding-hint » est à gauche de « onboarding-close », qui affiche « Plus tard » ; « onboarding-choice-auto » a le focus sans aucune touche pressée ; le rectangle de chaque option est identique avec et sans le focus

#### Scenario: navigation dans le parcours
- **WHEN** le test presse bas trois fois, haut trois fois, puis gauche et droite depuis chaque élément focalisable
- **THEN** le focus suit « onboarding-choice-auto », « onboarding-choice-manual », « onboarding-close » et y reste ; il remonte jusqu'à « onboarding-choice-auto » et y reste ; gauche et droite ne changent pas le focus ; « zone-hero », « zone-dock », « hero-capsule » et « settings-gear » ne prennent jamais le focus tant que « onboarding-launcher » est affiché

#### Scenario: pages et retour
- **WHEN** le test presse OK sur « onboarding-choice-manual », puis Retour, puis OK sur « onboarding-choice-auto », puis Retour
- **THEN** « onboarding-manual » s'affiche dans « onboarding-launcher », puis la page de choix revient avec le focus sur « onboarding-choice-manual » ; « onboarding-auto » s'affiche avec le focus sur « onboarding-auto-action », puis la page de choix revient avec le focus sur « onboarding-choice-auto »

#### Scenario: ordre et focus initial de la page Par autorisations
- **WHEN** le test ouvre « onboarding-manual », rien n'étant fait et le rôle disponible, sur un appareil sans écran d'auto-démarrage du constructeur
- **THEN** « onboarding-progress » affiche « 0 étape sur 2 terminée » ; « onboarding-step-accessibility », « onboarding-step-boot » puis « onboarding-step-home-role » sont affichés de haut en bas, sans « onboarding-step-overlay » ni « onboarding-step-vendor-boot », puis le pied, « onboarding-hint » à gauche de « onboarding-done » ; « onboarding-step-accessibility » a le focus ; « onboarding-step-boot-switch » est désactivé ; « onboarding-step-home-role-state » affiche « Facultatif »

#### Scenario: étape 3 masquée
- **WHEN** le double déclare le rôle indisponible sans écran système de remplacement et que le test ouvre « onboarding-manual »
- **THEN** « onboarding-step-home-role » n'existe pas et bas depuis la dernière étape obligatoire donne le focus à « onboarding-done »

#### Scenario: navigation dans la page Par autorisations
- **WHEN** le test presse bas quatre fois, haut quatre fois, puis gauche et droite depuis chaque élément focalisable
- **THEN** le focus suit « onboarding-step-accessibility », « onboarding-step-boot », « onboarding-step-home-role », « onboarding-done » et y reste ; il remonte jusqu'à « onboarding-step-accessibility » et y reste ; gauche et droite ne changent pas le focus

#### Scenario: sous-étape 2b et progression
- **WHEN** le double déclare le service connecté, la permission d'affichage superposé absente avec son écran disponible et un écran d'auto-démarrage du constructeur, puis le test presse OK sur « onboarding-step-boot », puis OK sur « onboarding-step-overlay », simule le passage en arrière-plan puis le retour avec la permission accordée
- **THEN** après le premier OK, « onboarding-step-overlay » puis « onboarding-step-vendor-boot » apparaissent entre « onboarding-step-boot » et « onboarding-step-home-role », avec l'état « À faire », le focus reste sur « onboarding-step-boot », dont « onboarding-step-boot-marker » porte toujours le numéro 2, et « onboarding-progress » affiche « 1 étape sur 4 terminée » ; pendant l'absence, « onboarding-step-overlay-state » affiche « En attente… » ; au retour il affiche « Fait » et « onboarding-step-overlay » a le focus ; OK sur « onboarding-step-boot » (désactivation) retire les deux sous-étapes

#### Scenario: états affichés
- **WHEN** le test compose une option, une étape ou la ligne des réglages dans chacun des états qui la concernent (« Fait », « En attente… », « À faire », « Bloqué », « Indisponible », « Facultatif », « Terminé », « Partiel », « Configuré », « En cours », « Échec »), avec et sans le focus
- **THEN** « <ligne>-state » affiche le libellé de l'état ; « <ligne>-dot » existe, à gauche de « <ligne>-state », pour « Fait », « Terminé » et « Configuré » (couleur `SwitchOn`) et « En attente… » et « En cours » (couleur `Badge`), et n'existe pas pour les autres ; « <ligne>-marker » porte une coche pour une étape « Fait » et son numéro sinon (« 2b », « 2c » pour les sous-étapes) ; en « Indisponible », « <ligne>-detail » affiche la raison propre à l'étape, le titre a la couleur secondaire hors focus, l'élément prend le focus, OK n'ouvre aucun écran et la progression ne la compte pas ; le libellé d'état n'est jamais « Fait » ni « Configuré » tant que le double n'a pas confirmé l'état

#### Scenario: confirmations
- **WHEN** le test affiche « launcher-confirm-disable », puis « launcher-confirm-restore », et presse gauche, droite, haut, bas puis Retour dans chacun
- **THEN** « launcher-confirm-disable-cancel » a le focus initial dans le premier, « launcher-confirm-restore-accept » dans le second ; gauche et droite passent d'un bouton à l'autre sans boucle ; haut et bas ne changent rien ; Retour ferme le dialogue sans opération et rend le focus à l'élément qui l'avait ouvert, ou poursuit la séquence avec les solutions de repli pour la désactivation

#### Scenario: Détails
- **WHEN** le test presse OK sur « onboarding-auto-details », puis Retour, puis ouvre « launcher-confirm-disable-details »
- **THEN** « onboarding-details » liste un « onboarding-details-item-<opération> » par modification faite ou à venir, non focalisable ; Retour rend le focus à « onboarding-auto-details » ; depuis la confirmation, Retour rend le focus à « launcher-confirm-disable-details » sans fermer la confirmation

#### Scenario: guide du débogage
- **WHEN** le double déclare le débogage désactivé et que le test presse OK sur « onboarding-auto-action », puis droite, gauche, puis OK sur « onboarding-debug-open-settings »
- **THEN** « onboarding-debug-guide » s'affiche avec « onboarding-debug-step-1 » à « onboarding-debug-step-4 » de haut en bas, « onboarding-debug-open-settings » a le focus, droite le donne à « onboarding-debug-continue », gauche le rend ; l'écran des options pour les développeurs est ouvert ; au retour, « onboarding-debug-continue » a le focus

#### Scenario: fermeture définitive du parcours
- **WHEN** le test, dans trois compositions distinctes, presse OK sur « onboarding-close », presse Retour sur la page de choix, puis transmet un intent Home à l'activité au premier plan, chaque fois avec « onboarding-launcher » affiché automatiquement
- **THEN** dans les trois cas « onboarding-launcher » n'existe plus, le drapeau de fermeture est persisté, « zone-hero » a le focus et un second lancement simulé n'affiche pas « onboarding-launcher »

#### Scenario: retour système dans le parcours
- **WHEN** le test presse OK sur « onboarding-step-accessibility », simule le passage en arrière-plan puis le retour au premier plan avec le double déclarant le service activé et connecté
- **THEN** pendant l'absence « onboarding-step-accessibility-state » affiche « En attente… » avec « onboarding-step-accessibility-dot » ; au retour il affiche « Fait », « onboarding-step-accessibility-marker » porte une coche, « onboarding-manual » est toujours affiché et « onboarding-step-accessibility » a le focus

#### Scenario: ordre des contrôles de la catégorie
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** « setting-upnext-visible », « setting-upnext-position », « setting-launcher-group » et « setting-launcher-guide » sont affichés de haut en bas dans cet ordre ; « setting-upnext-visible » a le focus ; « setting-launcher-group » n'est pas focalisable ; aucun des anciens testTags n'existe

#### Scenario: navigation dans la catégorie
- **WHEN** le test, depuis « setting-upnext-position » liste fermée, presse bas deux fois, haut deux fois, puis bas, droite, gauche, et Retour depuis « setting-launcher-guide »
- **THEN** le focus passe à « setting-launcher-guide » et y reste, sans jamais passer par « setting-launcher-group » ; il remonte jusqu'à « setting-upnext-visible » puis redescend ; droite ne fait rien ; gauche rend le focus à « settings-category-HOME_SCREEN » ; Retour ferme les réglages

#### Scenario: parcours ouvert depuis les réglages
- **WHEN** le test presse OK sur « setting-launcher-guide », puis Retour, puis de nouveau OK et OK sur « onboarding-close »
- **THEN** « onboarding-launcher » s'affiche par-dessus « settings-home-screen » sur la page de choix, « onboarding-close » affiche « Fermer » ; après Retour comme après « onboarding-close », il n'existe plus, les réglages sont toujours affichés, « setting-launcher-guide » a le focus et le drapeau de fermeture du parcours automatique n'est pas modifié

#### Scenario: synthèse de la ligne des réglages
- **WHEN** le test affiche « Écran d'accueil » avec rien de configuré, puis la voie automatique « Configuré », puis les deux voies « Partiel », puis l'étape 1 « Bloqué » et la voie automatique « À faire »
- **THEN** « setting-launcher-guide-state » et « setting-launcher-guide-detail » affichent respectivement « À faire » et « Automatique : À faire · Par autorisations : À faire », « Configuré » (avec « setting-launcher-guide-dot ») et un détail commençant par « Automatique : Configuré », « Partiel » et « Automatique : Partiel · Par autorisations : Partiel », « Bloqué » et « Automatique : À faire · Par autorisations : Bloqué »

#### Scenario: démarrage indisponible sans écran de permission
- **WHEN** le double déclare l'écran système de la permission absent, ou fait échouer son ouverture depuis « onboarding-step-overlay »
- **THEN** « onboarding-step-boot-state » affiche « Indisponible », « onboarding-step-boot-detail » affiche la raison, « onboarding-step-boot-switch », « onboarding-step-boot-dot » et « onboarding-step-overlay » n'existent pas, l'étape garde le focus, OK ne l'active pas et l'option persistée est désactivée
