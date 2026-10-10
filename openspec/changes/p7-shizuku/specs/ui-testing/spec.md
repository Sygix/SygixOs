# Spec Delta

## ADDED Requirements

### Requirement: Couverture des parcours Shizuku
Les parcours P7 SHALL être couverts par des tests déterministes, sans Shizuku réel ni appareil : l'état de Shizuku, la réponse à la demande d'autorisation, le service privilégié (succès, composant refusé, délai dépassé, service perdu, arrêt), l'état des composants de l'accueil d'origine et l'écran d'accueil effectif SHALL être fournis par des doubles. Les tests Compose SHALL utiliser la taille TV des autres tests de navigation.

#### Scenario: action selon l'état de Shizuku
- **WHEN** le test affiche « Écran d'accueil » avec le double de Shizuku successivement dans chaque état, puis presse OK sur « setting-stock-home »
- **THEN** « en cours de lecture » : aucune action ; « absent » : proposition d'installation ; « arrêté » : ouverture de Shizuku ; « autorisation non accordée » : demande d'autorisation ; « autorisation refusée définitivement » : ouverture de Shizuku ; « version non prise en charge » : aucune action ; « prêt » : « stock-home-confirmation » affichée ; rien n'est modifié tant qu'aucune confirmation n'est validée

#### Scenario: désactivation confirmée
- **WHEN** le double est « prêt », l'accueil d'origine actif, et que le test presse OK sur « setting-stock-home » puis sur « stock-home-confirm »
- **THEN** le double reçoit la désactivation des seuls composants actifs de l'ensemble, dans l'ordre de `design.md` (D5), « setting-stock-home-state » affiche « En attente… » pendant l'opération puis l'état relu, et le focus est sur « setting-stock-home »

#### Scenario: annulation
- **WHEN** le test ouvre « stock-home-confirmation » puis presse « stock-home-cancel », puis la rouvre et presse Retour
- **THEN** aucune opération n'est demandée au double et le focus revient sur « setting-stock-home » les deux fois

#### Scenario: garde-fous
- **WHEN** le double refuse un composant après en avoir désactivé un autre, puis, dans un second test, ne rend pas SygixOs écran d'accueil effectif, puis, dans un troisième, dépasse le délai d'une étape
- **THEN** dans les trois cas, le double reçoit la remise à l'état par défaut des composants désactivés par l'opération, après l'arrêt du service privilégié dans le troisième cas ; l'état relu est affiché avec un détail d'échec, sans crash

#### Scenario: service arrêté en fin d'opération
- **WHEN** une opération se termine par un succès, un échec, un délai dépassé ou la perte de Shizuku
- **THEN** le double constate l'arrêt du service privilégié dans chaque cas

#### Scenario: état partiel et rétablissement
- **WHEN** le double déclare l'accueil d'origine partiellement désactivé, puis désactivé, avec Shizuku « prêt », et que le test confirme le rétablissement
- **THEN** le double reçoit la remise à l'état par défaut des seuls composants désactivés par l'utilisateur et l'état relu est affiché

#### Scenario: opération en cours
- **WHEN** une opération est en cours et que le test presse OK sur « setting-stock-home », puis ferme et rouvre les réglages après la fin de l'opération
- **THEN** aucune nouvelle opération n'est demandée et la ligne affiche l'état relu

#### Scenario: état relu au retour
- **WHEN** le double change l'état des composants pendant que SygixOs est en arrière-plan, puis que le test simule le retour au premier plan
- **THEN** « setting-stock-home-state » affiche l'état relu, sans valeur conservée d'avant la sortie

#### Scenario: priorité et ouverture impossible
- **WHEN** le test affiche la ligne avec l'accueil d'origine « indisponible » et Shizuku « absent », puis avec Shizuku « arrêté » et un double qui fait échouer l'ouverture de Shizuku
- **THEN** la ligne est d'abord « Indisponible » et OK ne demande rien au double ; ensuite, après OK, la ligne garde le focus et son détail indique l'échec, sans crash

#### Scenario: confirmation interrompue et retour de Shizuku
- **WHEN** la touche Home atteint l'activité, puis, dans un second test, le double perd Shizuku, pendant que « stock-home-confirmation » est affiché ; dans un troisième test, le test revient au premier plan après l'ouverture de Shizuku depuis la ligne
- **THEN** la confirmation se ferme sans opération demandée au double dans les deux premiers cas ; dans le troisième, « setting-stock-home-state » affiche l'état relu et « setting-stock-home » a le focus

#### Scenario: opérations limitées
- **WHEN** le test JUnit demande au service privilégié une opération sur un paquet absent de l'ensemble de l'accueil d'origine
- **THEN** l'opération est refusée et aucune commande n'est exécutée

#### Scenario: autorisation jamais demandée sans action
- **WHEN** le test simule le démarrage, le retour au premier plan et une mise à jour avec le double « autorisation non accordée »
- **THEN** aucune demande d'autorisation n'est faite au double

### Requirement: Sélecteurs de l'accueil d'origine
Les éléments P7 SHALL porter ces testTags : « setting-stock-home » (ligne), « setting-stock-home-title », « setting-stock-home-detail » (détail ou raison), « setting-stock-home-state » (libellé d'état), « setting-stock-home-dot » (pastille, absente quand l'état n'en a pas), « stock-home-confirmation » (confirmation), « stock-home-confirm » et « stock-home-cancel » (boutons).

#### Scenario: ordre dans la catégorie
- **WHEN** le test affiche « Écran d'accueil » avec les lignes P5 et P7
- **THEN** « setting-stock-home » est affiché après « setting-accessibility-home », et « setting-upnext-visible » garde le focus initial

#### Scenario: confirmation
- **WHEN** le test ouvre la confirmation depuis « setting-stock-home »
- **THEN** « stock-home-confirmation » est affiché par-dessus les réglages avec « stock-home-confirm » et « stock-home-cancel », haut et bas passent de l'un à l'autre sans boucle, gauche et droite ne déplacent pas le focus, Retour la ferme

## MODIFIED Requirements

### Requirement: Sélecteurs du launcher système
La présentation initiale et les contrôles P5 de « Écran d'accueil » SHALL exposer des testTags stables, utilisés par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) à la place du texte ou de la position : « onboarding-launcher » (voile et panneau de la présentation), « onboarding-mascot », « onboarding-row-home-role », « onboarding-row-boot », « onboarding-row-accessibility », « onboarding-continue », « onboarding-hint » ; « setting-launcher-group » (intertitre « Launcher système »), « setting-home-role », « setting-boot-start », « setting-accessibility-home » ; « launcher-overlay-confirmation » (voile et panneau de la confirmation d'affichage superposé), « launcher-overlay-open », « launcher-overlay-decline » ; pour toute ligne « <ligne> » ci-dessus, « <ligne>-title » (titre), « <ligne>-detail » (détail), « <ligne>-state » (libellé d'état), « <ligne>-dot » (pastille d'état, absente quand l'état n'en a pas) et, pour les lignes de démarrage, « <ligne>-switch ». Les tests SHALL vérifier l'ordre, le focus initial, la navigation D-pad, les états affichés et la fermeture définitive de la présentation définis par « Présentation initiale du launcher » de `launcher-shell` et « Réglages du launcher système » de `settings`.

#### Scenario: ordre et focus initial de la présentation
- **WHEN** le test compose l'accueil avec la présentation à afficher, après la fin de l'écran de démarrage et la réponse à la demande de permission
- **THEN** « onboarding-launcher » est affiché par-dessus « zone-hero » ; « onboarding-mascot », « onboarding-row-home-role », « onboarding-row-boot », « onboarding-row-accessibility » puis le pied sont affichés de haut en bas dans cet ordre, à l'intérieur de « onboarding-launcher », le pied plaçant « onboarding-hint » à gauche de « onboarding-continue » sur la même ligne (`design.md`, D9) ; « onboarding-row-home-role » a le focus sans aucune touche pressée ; « onboarding-row-boot-switch » est désactivé ; le rectangle de chaque ligne est identique avec et sans le focus

#### Scenario: navigation dans la présentation
- **WHEN** le test presse bas quatre fois, haut quatre fois, puis gauche et droite depuis chaque élément focalisable
- **THEN** le focus suit « onboarding-row-home-role », « onboarding-row-boot », « onboarding-row-accessibility », « onboarding-continue » et y reste ; il remonte jusqu'à « onboarding-row-home-role » et y reste ; gauche et droite ne changent pas le focus ; « zone-hero », « zone-dock », « hero-capsule » et « settings-gear » ne prennent jamais le focus tant que « onboarding-launcher » est affiché

#### Scenario: états affichés
- **WHEN** le test compose une ligne d'état (présentation ou réglages) dans chacun des états « Actif », « En attente… », « Inactif » et « Indisponible », avec et sans le focus
- **THEN** « <ligne>-state » affiche le libellé de l'état ; « <ligne>-dot » existe, à gauche de « <ligne>-state », pour « Actif » (couleur `SwitchOn`) et « En attente… » (couleur `Badge`) et n'existe pas pour « Inactif » et « Indisponible » ; en « Indisponible », « <ligne>-detail » affiche la raison propre au contrôle, le titre a la couleur secondaire hors focus, la ligne prend le focus et OK n'ouvre aucun écran ; le libellé d'état n'est jamais « Actif » tant que le double n'a pas confirmé l'état

#### Scenario: fermeture définitive de la présentation
- **WHEN** le test, dans trois compositions distinctes, presse OK sur « onboarding-continue », presse Retour, puis transmet un intent Home à l'activité au premier plan, chaque fois avec « onboarding-launcher » affiché
- **THEN** dans les trois cas « onboarding-launcher » n'existe plus, le drapeau de fermeture est persisté, « zone-hero » a le focus et un second lancement simulé n'affiche pas « onboarding-launcher » ; après OK sur « onboarding-row-boot » avant la fermeture, le switch « setting-boot-start-switch » est activé dans les réglages

#### Scenario: retour système dans la présentation
- **WHEN** le test presse OK sur « onboarding-row-accessibility », simule le passage en arrière-plan puis le retour au premier plan avec le double déclarant le service activé
- **THEN** pendant l'absence « onboarding-row-accessibility-state » affiche « En attente… » avec « onboarding-row-accessibility-dot » ; au retour il affiche « Actif », « onboarding-launcher » est toujours affiché et « onboarding-row-accessibility » a le focus

#### Scenario: ordre des contrôles de la catégorie
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** « setting-upnext-visible », « setting-upnext-position », « setting-launcher-group », « setting-home-role », « setting-boot-start », « setting-accessibility-home » et « setting-stock-home » (« Sélecteurs de l'accueil d'origine ») sont affichés de haut en bas dans cet ordre ; « setting-upnext-visible » a le focus ; « setting-launcher-group » n'est pas focalisable ; « setting-boot-start-switch » est désactivé par défaut

#### Scenario: navigation dans la catégorie
- **WHEN** le test, depuis « setting-upnext-position » liste fermée, presse bas cinq fois, haut cinq fois, puis droite, gauche et Retour depuis « setting-home-role »
- **THEN** le focus suit « setting-home-role », « setting-boot-start », « setting-accessibility-home », « setting-stock-home » et y reste, sans jamais passer par « setting-launcher-group » ; il remonte jusqu'à « setting-upnext-visible » ; droite ne fait rien ; gauche rend le focus à « settings-category-HOME_SCREEN » ; Retour ferme les réglages

#### Scenario: confirmation d'affichage superposé
- **WHEN** le test fait passer « onboarding-row-boot » ou « setting-boot-start » de désactivé à activé avec le double déclarant la permission absente mais son écran disponible, puis presse « launcher-overlay-decline », Retour, ou « launcher-overlay-open »
- **THEN** « launcher-overlay-confirmation » s'affiche avec « launcher-overlay-open » focalisé ; après chaque réponse il n'existe plus, le switch reste activé et la ligne de démarrage a le focus ; seul « launcher-overlay-open » ouvre l'écran système ; avec la permission accordée, « launcher-overlay-confirmation » n'apparaît pas

#### Scenario: démarrage indisponible sans écran de permission
- **WHEN** le double déclare l'écran système de la permission absent, ou fait échouer son ouverture depuis « launcher-overlay-open »
- **THEN** « <ligne de démarrage>-state » affiche « Indisponible », « <ligne de démarrage>-detail » affiche la raison, « <ligne de démarrage>-switch » et « <ligne de démarrage>-dot » n'existent pas, la ligne garde le focus, OK n'affiche pas « launcher-overlay-confirmation » et l'option persistée est désactivée

#### Scenario: détail du démarrage non observé
- **WHEN** le test affiche « Écran d'accueil » avec l'état « démarrage automatique non observé » vrai, puis avec une ouverture observée, puis avec l'option désactivée
- **THEN** « setting-boot-start-detail » affiche « SygixOs ne s'est pas ouvert automatiquement à ce démarrage » et « setting-boot-start-switch » reste activé dans le premier cas ; il affiche « Ouvrir SygixOs quand la TV s'allume » dans les deux autres
