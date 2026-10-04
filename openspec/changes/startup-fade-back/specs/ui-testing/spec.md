# Delta ui-testing

## MODIFIED Requirements

### Requirement: Couverture de l'écran de démarrage
L'écran de démarrage (« Écran de démarrage » de `launcher-shell`) SHALL être couvert par des tests Compose exécutés à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), avec une horloge de test, un état de l'accueil (catalogue, premier visuel du héro) contrôlé par le test et une animation de mascotte remplacée par une source déterministe, par assertions sémantiques et testTags stables : « startup-splash » pour l'écran, « startup-splash-mascot » pour la mascotte. Le contrat du fichier de l'animation et l'enchaînement avec l'écran de lancement du système SHALL être couverts par des tests sans capture d'image. La touche Retour pressée pendant le fondu de sortie (« Écran de démarrage », scénario « Retour pendant le fondu de sortie ») SHALL être couverte par un test qui échoue si l'accueil l'intercepte.

#### Scenario: durée minimale
- **WHEN** le catalogue et le premier visuel du héro sont prêts 100 ms après la première image qui montre la mascotte
- **THEN** « startup-splash » est encore affiché 590 ms après cette image et aucun élément de l'accueil n'a le focus ; après 600 ms et la durée du fondu, « startup-splash » n'existe plus, le héro est affiché et détient le focus

#### Scenario: attente du premier visuel
- **WHEN** le catalogue est prêt à 200 ms et le premier visuel du héro à 1,5 s
- **THEN** « startup-splash » reste affiché jusqu'à 1,5 s, puis disparaît après la durée du fondu

#### Scenario: plafond du visuel
- **WHEN** le catalogue est prêt à 200 ms et le premier visuel du héro ne l'est jamais
- **THEN** le fondu commence à 2 s et le héro affiche son état de repli

#### Scenario: plafond global
- **WHEN** le catalogue n'est jamais chargé
- **THEN** le fondu commence à 5 s et révèle le fond noir uni de l'accueil en chargement, sans crash

#### Scenario: accueil caché sous l'écran de démarrage
- **WHEN** l'accueil est prêt mais que la durée minimale n'est pas atteinte
- **THEN** l'accueil est composé et aucun de ses éléments n'est focusable ; un test de pixels en rendu natif (`@GraphicsMode(NATIVE)`), avec un accueil d'une couleur unie, montre que l'écran reste entièrement noir (écran de démarrage opaque au-dessus de l'accueil) jusqu'au début du fondu, puis que la couleur de l'accueil apparaît progressivement pendant le fondu et occupe tout l'écran à sa fin

#### Scenario: accueil sans animation sous l'écran de démarrage
- **WHEN** l'accueil est composé sous l'écran de démarrage avec un visuel de programme, une vidéo d'aperçu, puis une grille
- **THEN** le visuel du héro est entièrement opaque dès qu'il est chargé, sans fondu ; la vidéo d'aperçu est préparée mais pas lue, et sa lecture commence quand l'accueil devient interactif ; la grille (« home-grid ») n'existe qu'après la fin du fondu de sortie ; la vérification des visuels du héro s'arrête après le premier visuel utilisable et ne reprend qu'à la fin de l'écran de démarrage (test JUnit du ViewModel)

#### Scenario: focus au début du fondu
- **WHEN** le fondu vient de commencer
- **THEN** le héro détient déjà le focus et un appui sur haut porte le focus sur l'élément situé au-dessus du héro, comme sans écran de démarrage

#### Scenario: touches avant le fondu
- **WHEN** le test envoie bas, droite et OK avant le début du fondu
- **THEN** aucun élément de l'accueil ne prend le focus ni ne réagit et, après le fondu, le héro détient le focus

#### Scenario: retour pendant le fondu de sortie
- **WHEN** le test envoie Retour pendant le fondu de sortie, alors que le héro détient le focus et que l'accueil traite les touches du D-pad
- **THEN** l'app quitte le premier plan (l'activité n'est plus au premier plan) sans crash, et aucune action de l'accueil n'a été déclenchée : le héro n'a pas ouvert de programme, la page n'a pas défilé ; le même test sans Retour laisse l'accueil au premier plan, ce qui prouve que la sortie vient de la touche

#### Scenario: retour sans fondu (animations désactivées)
- **WHEN** les animations sont désactivées et le test envoie Retour alors que l'écran de démarrage est encore affiché, puis l'envoie après son remplacement par l'accueil
- **THEN** pendant l'écran de démarrage, l'app quitte le premier plan sans action déclenchée sur l'accueil ; après le remplacement, Retour suit « Navigation 3 paliers » (le héro reprend le focus, l'app reste au premier plan)

#### Scenario: retour au premier plan
- **WHEN** l'activité repasse en arrière-plan puis au premier plan dans le même processus
- **THEN** « startup-splash » n'est jamais affiché

#### Scenario: accueil recréé
- **WHEN** l'accueil est recréé dans un processus où il avait déjà été créé, avant le chargement du catalogue
- **THEN** « startup-splash » n'est pas affiché ; un fond noir uni est affiché sans « startup-splash-mascot »

#### Scenario: mascotte tardive
- **WHEN** la source de l'animation ne répond qu'après 800 ms, l'accueil étant prêt
- **THEN** la durée minimale de 600 ms compte depuis la première image qui montre la mascotte, dont l'animation est démarrée

#### Scenario: composition différée de l'accueil
- **WHEN** le catalogue est prêt avant la fin du fondu d'entrée de la mascotte, ou alors que la fenêtre n'est pas active
- **THEN** « zone-hero » n'existe pas avant la fin de ce fondu et l'activation de la fenêtre, puis existe aussitôt après ; si la fenêtre reste sans focus (demande de permission), « zone-hero » existe 400 ms après la fin du fondu et le héro a le focus après le fondu de sortie

#### Scenario: échelle d'animation de l'app forcée à 0
- **WHEN** l'échelle d'animation de l'app est forcée à 0 alors que l'échelle de durée des animations du système n'est pas réglée
- **THEN** la source du réglage système indique que les animations sont actives ; une échelle du système à 0 les indique désactivées

#### Scenario: animations désactivées
- **WHEN** la source du réglage système indique que les animations sont désactivées
- **THEN** la mascotte affichée est l'image fixe et n'est pas animée ; l'accueil remplace « startup-splash » sans fondu dès que les règles de durée le permettent

#### Scenario: animation illisible
- **WHEN** la source de l'animation renvoie une erreur
- **THEN** « startup-splash » est affiché sans « startup-splash-mascot », puis l'accueil s'affiche selon les mêmes règles de durée, sans crash

#### Scenario: contrat du fichier
- **WHEN** la suite de tests s'exécute
- **THEN** un test lit le fichier de l'animation livré dans l'application et vérifie chaque contrainte de « Fichier de l'animation de la mascotte » (`launcher-shell`)

#### Scenario: écran de lancement du système
- **WHEN** la suite de tests s'exécute
- **THEN** un test vérifie dans le thème de l'application que le fond de l'écran de lancement du système est le noir du fond de l'écran de démarrage et que son icône ne dessine aucun pixel opaque
