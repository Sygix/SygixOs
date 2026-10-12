# Spec Delta

## ADDED Requirements

### Requirement: Couverture du parcours de l'écran d'accueil
Le parcours de l'écran d'accueil, la page « Par autorisations », le panneau « Launcher principal » et les réglages de secours (`launcher-shell`) SHALL être couverts par des tests déterministes, sans environnement Android réel : le rôle HOME, l'écran d'accueil effectif, l'état du service d'accessibilité (activé dans les réglages du système, connecté ou non), la fin du démarrage, le numéro de démarrage, le temps écoulé depuis le démarrage, la permission de superposition, l'écran d'auto-démarrage du constructeur, l'ouverture des écrans système et l'accès par le débogage SHALL être fournis par des doubles contrôlés. La logique (écrans affichés et enchaînements 8.1 à 8.17, états des deux voies, synthèse de la ligne des réglages, parcours affiché automatiquement, démarrage observé, filtre de la touche Home, ouverture unique au démarrage, attente de connexion du service) SHALL être testée en JUnit ; la navigation et l'affichage, par des tests Compose à la taille d'une TV (`w960dp-h540dp-xhdpi`) avec les testTags de « Sélecteurs de l'écran d'accueil ». Les tests du service d'accessibilité SHALL distinguer le service activé et connecté, activé mais non connecté, et non activé. Aucun test ne SHALL présenter un état « Fait » ou « Active » que le double n'a pas confirmé.

#### Scenario: premier lancement et plus tard
- **WHEN** le test lance SygixOs sans parcours enregistré comme fermé, simule la réponse à la demande de permission « Programmes TV » (le parcours n'apparaît qu'après), puis presse Retour sur le panneau 8.1
- **THEN** le parcours est enregistré comme fermé, le héro a le focus, aucune connexion au débogage ni ouverture d'écran système n'a eu lieu, et un second lancement simulé n'affiche pas le parcours

#### Scenario: états relus après retour système
- **WHEN** le test change l'état de la permission, du service d'accessibilité ou de l'écran d'accueil effectif pendant que SygixOs est en arrière-plan, puis simule le retour au premier plan
- **THEN** les lignes de 8.13, les états des deux voies et la ligne des réglages affichent le nouvel état, sans valeur périmée, et le focus est sur la ligne qui avait ouvert l'écran système

#### Scenario: filtre de la touche Home
- **WHEN** le test transmet au filtre du service, service connecté, une séquence pressé puis relâché courte, une séquence avec répétitions maintenue au-delà du délai d'appui long, une séquence dont le relâchement est annulé, et une autre touche
- **THEN** tous les événements Home sont consommés (le filtre renvoie vrai pour l'appui, chaque répétition et le relâchement ; un relâchement sans appui consommé reste au système) ; les deux premières séquences demandent l'ouverture de l'accueil une seule fois chacune, au relâchement, sans délai ; le relâchement annulé ne demande rien ; l'autre touche n'est pas consommée ; service non connecté, aucun événement n'est consommé

#### Scenario: service d'accessibilité non activé ou activé
- **WHEN** le double déclare le service non activé, puis activé et connecté, et que le test simule l'appui sur Home, SygixOs étant en arrière-plan puis au premier plan
- **THEN** non activé, la ligne « Ouvrir SygixOs avec la touche Home » affiche « À faire » et aucune touche n'est consommée ; activé, l'ouverture de l'accueil est demandée dans les deux cas, « Touche Home avec SygixOs au premier plan » s'applique au premier plan et la ligne affiche « Fait »

#### Scenario: attente de connexion et service bloqué
- **WHEN** le double déclare le service activé mais non connecté au lancement du processus, puis connecté pendant l'attente bornée dans un premier test et toujours non connecté à son terme dans un second
- **THEN** pendant l'attente la ligne affiche « En attente… » ; elle affiche « Fait » dans le premier cas, « Bloqué » avec son explication dans le second, et la voie par autorisations est alors « Bloquée »

#### Scenario: ouverture unique au démarrage
- **WHEN** le test simule, option activée, la fin du démarrage puis la connexion du service dans le même démarrage, puis la connexion du service seule, puis une connexion plus de 5 minutes après le démarrage, puis la fin du démarrage avec l'option désactivée
- **THEN** une seule ouverture est demandée dans le premier cas, une dans le deuxième, aucune dans les deux derniers

#### Scenario: démarrage automatique non observé
- **WHEN** le test simule un démarrage avec l'option activée avant ce démarrage, sans ouverture observée, puis affiche 8.13
- **THEN** « setup-manual-boot-detail » affiche « SygixOs ne s'est pas ouvert automatiquement à ce démarrage » ; avec une ouverture observée, l'option désactivée ou l'option activée après ce démarrage, le détail normal est affiché

#### Scenario: touche Home au premier plan
- **WHEN** le test affiche les réglages (avec la liste « Position d'À suivre » ouverte, et avec le panneau « Launcher principal » ou un dialogue de confirmation par-dessus), le parcours au premier lancement, la grille, le menu contextuel ou le mode « Déplacer », puis transmet un intent Home à l'activité au premier plan
- **THEN** réglages et surcouches sont fermés en une fois (parcours affiché automatiquement enregistré comme fermé, confirmation de désactivation traitée comme « Annuler »), la page de l'accueil est en haut et le héro a le focus ; une configuration en cours continue

#### Scenario: mise à jour sans opération privilégiée
- **WHEN** le test exécute une mise à jour intégrée factice jusqu'à la relance
- **THEN** aucune demande du rôle HOME ni connexion au débogage n'a lieu

### Requirement: Couverture de la configuration automatique
La configuration automatique, l'accès par le débogage, la désactivation et le rétablissement de l'accueil d'origine et la politique d'auto-démarrage du constructeur (`launcher-shell`) SHALL être couverts en JUnit et en tests Compose à la taille d'une TV avec des doubles de l'accès, des opérations privilégiées et de la lecture de l'état (`design.md`, D10), sans connexion ADB réelle : écrans affichés, séquence et étapes sans objet, réglages de secours, étapes dépendantes annulées, délais, perte de l'accès, garde-fous, ordre et annulation de la désactivation, rétablissement avec et sans écran d'accueil précédent relevé, refus des cibles hors de l'ensemble fermé, conservation des autres services d'accessibilité, fermeture de la connexion.

#### Scenario: séquence sans désactivation
- **WHEN** le double d'accès est prêt et rend SygixOs écran d'accueil effectif après la définition de l'écran d'accueil, et que le test presse OK sur « setup-choice-auto »
- **THEN** « setup-progress » (8.5) montre « setup-step-boot », « setup-step-home » et « setup-step-verify » passer à « Fait », puis « setup-done » (8.6) s'affiche avec « setup-done-finish » focalisé et l'état automatique « Active »

#### Scenario: écrans du débogage
- **WHEN** le double déclare le débogage désactivé, puis activé au retour, puis l'autorisation refusée, puis le débogage désactivé entre-temps
- **THEN** « setup-debug » (8.3) s'affiche avec « setup-debug-open » focalisé, puis « setup-authorize » (8.4) avec « setup-authorize-continue » focalisé, puis « setup-authorize » avec « setup-error » et « setup-authorize-retry » focalisé (8.11), puis « setup-debug » avec « setup-error » (8.10) ; aucune opération privilégiée n'est envoyée

#### Scenario: désactivation proposée puis plus tard
- **WHEN** le double ne rend pas SygixOs écran d'accueil effectif après la définition, avec des composants actifs, et que le test presse « setup-more-later » sur « setup-more » (8.7)
- **THEN** aucune désactivation n'est envoyée, le service d'accessibilité est ajouté sans retirer les autres, la superposition est accordée, l'option de démarrage est activée, et « setup-fallback » (8.9) s'affiche

#### Scenario: désactivation confirmée puis partiellement annulée
- **WHEN** le test presse « setup-more-disable », puis « confirm-disable-accept », et que le double fait échouer la désactivation du deuxième composant
- **THEN** « confirm-disable » (8.8) s'affiche d'abord avec « confirm-disable-cancel » focalisé ; après l'échec, le premier composant est remis à son état par défaut, l'état est relu, aucun succès n'est affiché et « setup-fallback » s'affiche

#### Scenario: échec d'une étape
- **WHEN** le double fait dépasser son délai à la définition de l'écran d'accueil
- **THEN** « setup-failed » (8.12) s'affiche avec « setup-step-home-state » « Échec », « setup-step-verify-state » « Annulée » et « setup-failed-retry » focalisé ; aucune nouvelle tentative n'a lieu sans action et la connexion est fermée

#### Scenario: politique d'auto-démarrage du constructeur
- **WHEN** le double expose une politique sans SygixOs dans ses listes blanches, puis une politique où il figure déjà, puis une politique de structure inattendue
- **THEN** dans le premier cas la valeur d'origine est sauvegardée et la valeur écrite ne diffère que par l'ajout de `fr.sygix.sygixos` aux deux listes ; dans le deuxième rien n'est écrit ; dans le troisième rien n'est écrit et l'étape est en échec sans arrêter l'étape de l'écran d'accueil ; aucun rétablissement ne retire l'inscription

#### Scenario: voie indisponible sur cette TV
- **WHEN** le double déclare le débogage activé et le port ADB réseau injoignable
- **THEN** le parcours revient à 8.2, « setup-choice-auto-state » affiche « Indisponible sur cette TV » et « setup-choice-manual » a le focus

#### Scenario: cible refusée
- **WHEN** le test demande à la passerelle une opération sur un paquet hors du paquet SygixOs et de l'ensemble fermé
- **THEN** la passerelle la refuse sans rien envoyer au double

#### Scenario: rétablissement
- **WHEN** le double déclare deux composants désactivés par l'utilisateur et un écran d'accueil précédent relevé, et que le test presse « launcher-panel-restore » puis « confirm-restore-accept »
- **THEN** « confirm-restore » (8.17) s'est affiché avec « confirm-restore-accept » focalisé ; les composants sont remis à leur état par défaut dans l'ordre inverse, l'écran d'accueil précédent est redéfini, « launcher-panel-restore » n'existe plus et « launcher-panel-relaunch » a le focus

#### Scenario: panneau fermé pendant la configuration
- **WHEN** le test presse Retour pendant « setup-progress »
- **THEN** le panneau se ferme, la séquence du double continue jusqu'à sa fin, et la réouverture affiche l'écran de l'état relu

### Requirement: Sélecteurs de l'écran d'accueil
Les écrans 8.1 à 8.17 et la ligne des réglages SHALL exposer des testTags stables, utilisés par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) à la place du texte ou de la position : « setup-panel » (voile et panneau), « setup-mascot », « setup-title », « setup-description », « setup-hint » ; 8.1/8.2 : « setup-choice-auto », « setup-choice-manual », « setup-later » ; 8.3/8.10 : « setup-debug », « setup-debug-step-1 », « setup-debug-step-2 », « setup-debug-status », « setup-debug-open », « setup-debug-back » ; 8.4/8.11 : « setup-authorize », « setup-authorize-step-1 », « setup-authorize-step-2 », « setup-authorize-continue », « setup-authorize-retry », « setup-authorize-back » ; erreurs : « setup-error » (encadré, avec « setup-error-title » et « setup-error-detail ») ; 8.5 : « setup-progress » ; 8.6 : « setup-done », « setup-done-tip » (conseil), « setup-done-details », « setup-done-finish » ; 8.7 : « setup-more », « setup-more-home », « setup-more-later », « setup-more-disable » ; 8.9 : « setup-fallback », « setup-fallback-home », « setup-fallback-overlay », « setup-fallback-boot », « setup-fallback-tip », « setup-fallback-details », « setup-fallback-finish » ; 8.12 : « setup-failed », « setup-failed-close », « setup-failed-retry » ; lignes d'étapes de 8.5 à 8.12 : « setup-step-boot », « setup-step-home », « setup-step-verify », « setup-step-stock » ; 8.13 : « setup-manual », « setup-manual-home », « setup-manual-boot », « setup-manual-overlay », « setup-manual-vendor-boot », « setup-manual-tip », « setup-manual-finish » ; 8.14 : « setup-details », « setup-details-item-<réglage> », « setup-details-close » ; 8.16 : « launcher-panel », « launcher-panel-auto », « launcher-panel-manual », « launcher-panel-relaunch », « launcher-panel-restore » ; dialogues 8.8 et 8.17 : « confirm-disable » et « confirm-restore », chacun avec « <dialogue>-title », « <dialogue>-text », « <dialogue>-accept », « <dialogue>-cancel » ; réglages : « setting-launcher-group », « setting-launcher-guide » ; pour toute ligne « <ligne> » ci-dessus, « <ligne>-title », « <ligne>-detail », « <ligne>-state » et « <ligne>-dot » (pastille, absente quand l'état n'en a pas). Les anciens testTags « onboarding-* », « setting-home-role », « setting-boot-start », « setting-accessibility-home » et « launcher-overlay-* » n'existent plus. Les tests SHALL vérifier les écrans, l'ordre, le focus initial, la navigation D-pad, les états, la présence de « setup-mascot » (absente des dialogues et de « setup-details ») et la fermeture définitive du parcours définis par `launcher-shell`, et la synthèse définie par « Réglages du launcher système » de `settings`.

#### Scenario: ordre et focus initial
- **WHEN** le test compose l'accueil avec le parcours à afficher, rien n'étant fait
- **THEN** « setup-panel » est affiché par-dessus « zone-hero » ; « setup-mascot » est à gauche de « setup-title » ; « setup-choice-auto » puis « setup-choice-manual » sont affichés de haut en bas avec les états « À configurer » et « À faire » ; « setup-hint » est à gauche de « setup-later » ; « setup-choice-auto » a le focus sans aucune touche pressée

#### Scenario: navigation
- **WHEN** le test presse bas trois fois, haut trois fois, puis gauche et droite sur 8.1, puis gauche et droite sur 8.3, 8.4, 8.7, 8.8 et 8.17
- **THEN** sur 8.1 le focus suit « setup-choice-auto », « setup-choice-manual », « setup-later » sans boucle et gauche et droite ne changent rien ; sur les écrans à deux boutons, gauche et droite passent d'un bouton à l'autre sans boucle ; « zone-hero », « zone-dock », « hero-capsule » et « settings-gear » ne prennent jamais le focus tant que « setup-panel » est affiché

#### Scenario: mascotte
- **WHEN** le test affiche chacun des écrans 8.1 à 8.17
- **THEN** « setup-mascot » existe sur les panneaux du parcours et n'existe pas dans « confirm-disable », « confirm-restore » ni « setup-details »

#### Scenario: page Par autorisations
- **WHEN** le test ouvre « setup-manual » sur un appareil avec écran d'auto-démarrage du constructeur, l'option de démarrage activée et la superposition manquante
- **THEN** « setup-manual-home », « setup-manual-boot », « setup-manual-overlay », « setup-manual-vendor-boot » puis « setup-manual-tip » et « setup-manual-finish » sont affichés de haut en bas ; « setup-manual-tip » n'est pas focalisable ; la première ligne qui n'est pas « Fait » a le focus

#### Scenario: états affichés
- **WHEN** le test compose une ligne dans chacun des états qui la concernent, avec et sans le focus
- **THEN** « <ligne>-state » affiche le libellé ; « <ligne>-dot » existe pour « Fait », « Active », « Actif » (couleur `SwitchOn`) et « En cours… », « En attente… » (couleur `Badge`), et n'existe pas pour les autres ; « Échec » est rendu en couleur d'erreur ; en « Indisponible » et « Indisponible sur cette TV », la ligne prend le focus et OK n'ouvre rien

#### Scenario: ordre des contrôles de la catégorie
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** « setting-upnext-visible », « setting-upnext-position », « setting-launcher-group » et « setting-launcher-guide » sont affichés de haut en bas ; « setting-upnext-visible » a le focus ; « setting-launcher-group » n'est pas focalisable ; aucun des anciens testTags n'existe

#### Scenario: panneau depuis les réglages
- **WHEN** le test presse OK sur « setting-launcher-guide », puis Retour
- **THEN** « launcher-panel » s'affiche par-dessus « settings-home-screen » avec « launcher-panel-relaunch » focalisé ; après Retour, il n'existe plus, les réglages sont toujours affichés, « setting-launcher-guide » a le focus et le drapeau du parcours automatique n'est pas modifié

#### Scenario: synthèse de la ligne des réglages
- **WHEN** le test affiche « Écran d'accueil » avec rien de fait, puis la voie automatique active, puis la voie par autorisations en partie, puis bloquée
- **THEN** « setting-launcher-guide-state » et « setting-launcher-guide-detail » affichent « À configurer » et « Automatique : à configurer · Par autorisations : à faire », « Actif » (avec « setting-launcher-guide-dot ») et « Automatique : actif · Par autorisations : non utilisée », « En partie » et « Automatique : à configurer · Par autorisations : en partie », « Bloqué » et « Automatique : à configurer · Par autorisations : bloquée »

## REMOVED Requirements

### Requirement: Couverture des parcours du launcher système
**Reason**: remplacée par « Couverture du parcours de l'écran d'accueil » et « Couverture de la configuration automatique », qui suivent les écrans 8.1 à 8.17.
**Migration**: les tests du service d'accessibilité, du démarrage et de la touche Home sont repris dans « Couverture du parcours de l'écran d'accueil ».

### Requirement: Sélecteurs du launcher système
**Reason**: remplacée par « Sélecteurs de l'écran d'accueil » (nouveaux écrans, nouveaux testTags).
**Migration**: les testTags « onboarding-* » sont remplacés par « setup-* », « launcher-panel-* » et « confirm-* » ; « setting-launcher-group » et « setting-launcher-guide » sont conservés.
