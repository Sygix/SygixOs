# Spec Delta

## ADDED Requirements

### Requirement: Couverture des parcours du launcher système
Les parcours P5 SHALL être couverts par des tests déterministes, sans environnement Android réel : le rôle HOME, l'état du service d'accessibilité, la fin du démarrage, le numéro de démarrage et l'ouverture des écrans système SHALL être fournis par des doubles contrôlés. La logique (états, présentation initiale, démarrage observé) SHALL être testée en JUnit ; la navigation et l'affichage, par des tests Compose à la taille d'une TV (`w960dp-h540dp-xhdpi`) avec des testTags stables. Les tests du service d'accessibilité SHALL distinguer le service activé par l'utilisateur (double déclaré activé par le système) du service non activé, désactivé ou arrêté. Les assertions de rendu et de navigation de la présentation initiale et des contrôles P5 utilisent les testTags de « Sélecteurs du launcher système ».

#### Scenario: refus du rôle HOME
- **WHEN** le test simule l'ouverture du dialogue du rôle HOME puis un retour au premier plan avec le rôle non détenu
- **THEN** le contrôle passe par « En attente… » puis affiche « Inactif », jamais « Actif », et l'accueil reste utilisable

#### Scenario: rôle HOME accepté ou indisponible
- **WHEN** le test simule un retour au premier plan avec le rôle détenu, puis un rôle déclaré indisponible par le système avec, puis sans, écran système des applications par défaut ou de l'écran d'accueil
- **THEN** le contrôle affiche « Actif » dans le premier cas ; avec l'écran de remplacement, sa validation ouvre cet écran ; sans lui, le contrôle affiche « Indisponible » et aucun écran système n'est ouvert

#### Scenario: états relus après retour système
- **WHEN** le test change l'état du rôle HOME ou du service d'accessibilité pendant que SygixOs est en arrière-plan, puis simule le retour au premier plan
- **THEN** les contrôles affichent le nouvel état, sans valeur périmée, et le focus est sur le contrôle qui avait ouvert l'écran système

#### Scenario: présentation initiale ignorée
- **WHEN** le test lance SygixOs sans présentation enregistrée comme fermée, simule la réponse à la demande de permission « Programmes TV » (la présentation n'apparaît qu'après), puis presse Retour sur la présentation
- **THEN** la présentation est enregistrée comme fermée, le héro a le focus, aucune demande de rôle ni ouverture d'écran système n'a eu lieu, l'option de démarrage est désactivée, et un second lancement simulé n'affiche pas la présentation

#### Scenario: service d'accessibilité non activé par l'utilisateur
- **WHEN** le double déclare le service non activé, désactivé ou arrêté
- **THEN** le contrôle d'accessibilité affiche « Inactif », un signal Home simulé ne ramène pas SygixOs au premier plan, les autres contrôles restent utilisables et aucune nouvelle proposition n'est affichée automatiquement

#### Scenario: service d'accessibilité activé par l'utilisateur
- **WHEN** le double déclare le service activé par l'utilisateur et que le test simule le signal Home détourné, SygixOs étant en arrière-plan puis au premier plan
- **THEN** l'ouverture de l'accueil de SygixOs est demandée dans les deux cas ; au premier plan, le comportement de « Touche Home avec SygixOs au premier plan » s'applique

#### Scenario: démarrage automatique non observé
- **WHEN** le test simule un démarrage (numéro de démarrage incrémenté) avec l'option activée avant ce démarrage, sans ouverture observée, puis affiche « Écran d'accueil »
- **THEN** « setting-boot-start-detail » indique que le démarrage automatique n'a pas eu lieu (« détail du démarrage non observé ») ; avec une ouverture au démarrage observée, ou l'option désactivée, ou l'option activée après ce démarrage, le détail normal est affiché

#### Scenario: touche Home au premier plan
- **WHEN** le test ouvre les réglages (y compris avec la liste déroulante « Position d'Up Next » ouverte), ou descend dans la grille, ou ouvre le menu contextuel d'une tuile, ou active le mode « Déplacer », ou affiche la présentation initiale, puis transmet un intent Home à l'activité au premier plan
- **THEN** réglages et surcouche sont fermés en une fois (déplacement annulé, valeur de « Position d'Up Next » inchangée, présentation enregistrée comme fermée), la page de l'accueil est en haut et le héro a le focus

#### Scenario: mise à jour sans demande de rôle
- **WHEN** le test exécute une mise à jour intégrée factice jusqu'à la relance, avec un double du rôle HOME non détenu
- **THEN** aucune demande du rôle HOME n'est faite par le parcours de mise à jour

### Requirement: Sélecteurs du launcher système
La présentation initiale et les contrôles P5 de « Écran d'accueil » SHALL exposer des testTags stables, utilisés par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) à la place du texte ou de la position : « onboarding-launcher » (voile et panneau de la présentation), « onboarding-mascot », « onboarding-row-home-role », « onboarding-row-boot », « onboarding-row-accessibility », « onboarding-continue », « onboarding-hint » ; « setting-launcher-group » (intertitre « Launcher système »), « setting-home-role », « setting-boot-start », « setting-accessibility-home » ; pour toute ligne « <ligne> » ci-dessus, « <ligne>-detail » (détail), « <ligne>-state » (libellé d'état), « <ligne>-dot » (pastille d'état, absente quand l'état n'en a pas) et, pour les lignes de démarrage, « <ligne>-switch ». Les tests SHALL vérifier l'ordre, le focus initial, la navigation D-pad, les états affichés et la fermeture définitive de la présentation définis par « Présentation initiale du launcher » de `launcher-shell` et « Réglages du launcher système » de `settings`.

#### Scenario: ordre et focus initial de la présentation
- **WHEN** le test compose l'accueil avec la présentation à afficher, après la fin de l'écran de démarrage et la réponse à la demande de permission
- **THEN** « onboarding-launcher » est affiché par-dessus « zone-hero » ; « onboarding-mascot », « onboarding-row-home-role », « onboarding-row-boot », « onboarding-row-accessibility », « onboarding-hint » et « onboarding-continue » sont affichés de haut en bas dans cet ordre, à l'intérieur de « onboarding-launcher » ; « onboarding-row-home-role » a le focus sans aucune touche pressée ; « onboarding-row-boot-switch » est désactivé ; le rectangle de chaque ligne est identique avec et sans le focus

#### Scenario: navigation dans la présentation
- **WHEN** le test presse bas quatre fois, haut quatre fois, puis gauche et droite depuis chaque élément focalisable
- **THEN** le focus suit « onboarding-row-home-role », « onboarding-row-boot », « onboarding-row-accessibility », « onboarding-continue » et y reste ; il remonte jusqu'à « onboarding-row-home-role » et y reste ; gauche et droite ne changent pas le focus ; « zone-hero », « zone-dock », « hero-capsule » et « settings-gear » ne prennent jamais le focus tant que « onboarding-launcher » est affiché

#### Scenario: états affichés
- **WHEN** le test compose une ligne d'état (présentation ou réglages) dans chacun des états « Actif », « En attente… », « Inactif » et « Indisponible », avec et sans le focus
- **THEN** « <ligne>-state » affiche le libellé de l'état ; « <ligne>-dot » existe, à gauche de « <ligne>-state », pour « Actif » (couleur `SwitchOn`) et « En attente… » (couleur `Badge`) et n'existe pas pour « Inactif » et « Indisponible » ; en « Indisponible », « <ligne>-detail » affiche la raison propre au contrôle et le titre a la couleur secondaire hors focus ; le libellé d'état n'est jamais « Actif » tant que le double n'a pas confirmé l'état

#### Scenario: fermeture définitive de la présentation
- **WHEN** le test, dans trois compositions distinctes, presse OK sur « onboarding-continue », presse Retour, puis transmet un intent Home à l'activité au premier plan, chaque fois avec « onboarding-launcher » affiché
- **THEN** dans les trois cas « onboarding-launcher » n'existe plus, le drapeau de fermeture est persisté, « zone-hero » a le focus et un second lancement simulé n'affiche pas « onboarding-launcher » ; après OK sur « onboarding-row-boot » avant la fermeture, le switch « setting-boot-start-switch » est activé dans les réglages

#### Scenario: retour système dans la présentation
- **WHEN** le test presse OK sur « onboarding-row-accessibility », simule le passage en arrière-plan puis le retour au premier plan avec le double déclarant le service activé
- **THEN** pendant l'absence « onboarding-row-accessibility-state » affiche « En attente… » avec « onboarding-row-accessibility-dot » ; au retour il affiche « Actif », « onboarding-launcher » est toujours affiché et « onboarding-row-accessibility » a le focus

#### Scenario: ordre des contrôles de la catégorie
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** « setting-upnext-visible », « setting-upnext-position », « setting-launcher-group », « setting-home-role », « setting-boot-start » et « setting-accessibility-home » sont affichés de haut en bas dans cet ordre ; « setting-upnext-visible » a le focus ; « setting-launcher-group » n'est pas focalisable ; « setting-boot-start-switch » est désactivé par défaut

#### Scenario: navigation dans la catégorie
- **WHEN** le test, depuis « setting-upnext-position » liste fermée, presse bas quatre fois, haut quatre fois, puis droite, gauche et Retour depuis « setting-home-role »
- **THEN** le focus suit « setting-home-role », « setting-boot-start », « setting-accessibility-home » et y reste, sans jamais passer par « setting-launcher-group » ; il remonte jusqu'à « setting-upnext-visible » ; droite ne fait rien ; gauche rend le focus à « settings-category-HOME_SCREEN » ; Retour ferme les réglages

#### Scenario: détail du démarrage non observé
- **WHEN** le test affiche « Écran d'accueil » avec l'état « démarrage automatique non observé » vrai, puis avec une ouverture observée, puis avec l'option désactivée
- **THEN** « setting-boot-start-detail » affiche « SygixOs ne s'est pas ouvert automatiquement à ce démarrage » et « setting-boot-start-switch » reste activé dans le premier cas ; il affiche « Ouvrir SygixOs quand la TV s'allume » dans les deux autres
