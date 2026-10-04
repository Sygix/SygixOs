# Delta ui-testing

## ADDED Requirements

### Requirement: Couverture du rendu du héro et du verre
Le rendu du héro et du verre (« Thème », « Diaporama héro » et « Préchargement et mémoire » de `launcher-shell`) SHALL être couvert par des tests JVM : calcul des tailles de décodage, transformations du visuel et de l'arrière-plan du verre et rendu du verre sous Robolectric en rendu natif (`@GraphicsMode(NATIVE)`), changement de programme par test Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) avec un chargeur d'images de test. La fluidité SHALL être mesurée sur la TV réelle (`dumpsys gfxinfo` sur l'APK de release ou sur une build minifiée équivalente).

#### Scenario: tailles de décodage
- **WHEN** la fenêtre ou une bannière mesure 3840 × 2160
- **THEN** le visuel du héro est demandé en 1920 × 1080 et la bannière en 480 × 270, sans agrandir une image plus petite

#### Scenario: visuel voilé
- **WHEN** un visuel blanc de 3840 × 2160 passe par la transformation du héro
- **THEN** l'image produite mesure 1920 × 1080, est opaque, reste blanche hors des voiles et est assombrie en bas, à gauche et en haut à droite

#### Scenario: arrière-plan du verre
- **WHEN** un visuel moitié noir, moitié blanc passe par la transformation du verre
- **THEN** l'image produite mesure le huitième du visuel, est opaque, a une transition adoucie entre les deux moitiés et est assombrie en bas

#### Scenario: couleur du verre
- **WHEN** le dock est rendu sur un arrière-plan gris moyen puis sur un arrière-plan sombre
- **THEN** sa couleur est celle de l'arrière-plan recouvert de la teinte de la maquette, à 3 niveaux près, et reste sombre sur un arrière-plan sombre

#### Scenario: changement de programme
- **WHEN** le héro passe d'un programme au titre court à un programme au titre long
- **THEN** pendant tout le fondu, le bas de chaque bloc « hero-metadata » et la position de « hero-open » ne changent pas ; à la fin, seul le nouveau bloc reste

## MODIFIED Requirements

### Requirement: Couverture de l'écran de démarrage
L'écran de démarrage (« Écran de démarrage » de `launcher-shell`) SHALL être couvert par des tests Compose exécutés à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), avec une horloge de test, un état de l'accueil (catalogue, premier visuel du héro) contrôlé par le test et une animation de mascotte remplacée par une source déterministe, par assertions sémantiques et testTags stables : « startup-splash » pour l'écran, « startup-splash-mascot » pour la mascotte. Le contrat du fichier de l'animation et l'enchaînement avec l'écran de lancement du système SHALL être couverts par des tests sans capture d'image.

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

#### Scenario: accueil transparent avant le fondu
- **WHEN** l'accueil est prêt mais que la durée minimale n'est pas atteinte
- **THEN** l'accueil est composé avec une opacité nulle et aucun de ses éléments n'est focusable

#### Scenario: focus au début du fondu
- **WHEN** le fondu vient de commencer
- **THEN** le héro détient déjà le focus et un appui sur haut porte le focus sur l'élément situé au-dessus du héro, comme sans écran de démarrage

#### Scenario: touches avant le fondu
- **WHEN** le test envoie bas, droite et OK avant le début du fondu
- **THEN** aucun élément de l'accueil ne prend le focus ni ne réagit et, après le fondu, le héro détient le focus

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
- **THEN** « zone-hero » n'existe pas avant la fin de ce fondu et l'activation de la fenêtre, puis existe aussitôt après

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
