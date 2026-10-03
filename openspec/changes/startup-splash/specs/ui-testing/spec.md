# Delta ui-testing

## ADDED Requirements

### Requirement: Couverture de l'écran de démarrage
L'écran de démarrage (« Écran de démarrage » de `launcher-shell`) SHALL être couvert par des tests Compose exécutés à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), avec une horloge de test, un état de l'accueil contrôlé par le test et une animation de mascotte remplacée par une source déterministe, par assertions sémantiques et testTags stables : « startup-splash » pour l'écran, « startup-splash-mascot » pour la mascotte. Le contrat du fichier de l'animation et l'enchaînement avec l'écran de lancement du système SHALL être couverts par des tests JUnit sans capture d'image.

#### Scenario: durée minimale
- **WHEN** l'accueil devient prêt 100 ms après la première image de l'écran de démarrage
- **THEN** « startup-splash » est encore affiché à 590 ms et l'accueil n'a pas le focus ; après 600 ms et la durée du fondu, « startup-splash » n'existe plus, le héro est affiché et détient le focus

#### Scenario: attente de l'accueil
- **WHEN** l'accueil ne devient prêt qu'après 2 s
- **THEN** « startup-splash » reste affiché jusque-là, puis disparaît après la durée du fondu

#### Scenario: focus au début du fondu
- **WHEN** le fondu vient de commencer
- **THEN** le héro détient déjà le focus et un appui sur haut porte le focus sur l'élément situé au-dessus du héro, comme sans écran de démarrage

#### Scenario: touches ignorées
- **WHEN** le test envoie bas, droite, OK et Retour avant le début du fondu
- **THEN** aucune action n'est déclenchée sur l'accueil et, après le fondu, le héro détient le focus comme si aucune touche n'avait été pressée

#### Scenario: retour au premier plan
- **WHEN** l'activité repasse en arrière-plan puis au premier plan dans le même processus
- **THEN** « startup-splash » n'est jamais affiché

#### Scenario: animations désactivées
- **WHEN** le réglage système d'échelle de durée des animations vaut 0
- **THEN** la mascotte affichée est l'image fixe et n'est pas animée ; l'accueil remplace « startup-splash » dès la première image après 600 ms, sans fondu

#### Scenario: animation illisible
- **WHEN** la source de l'animation renvoie une erreur
- **THEN** « startup-splash » est affiché sans « startup-splash-mascot », puis l'accueil s'affiche selon les mêmes règles de durée, sans crash

#### Scenario: contrat du fichier
- **WHEN** la suite de tests s'exécute
- **THEN** un test JUnit lit le fichier de l'animation livré dans l'application et vérifie chaque contrainte de « Fichier de l'animation de la mascotte » (`launcher-shell`)

#### Scenario: écran de lancement du système
- **WHEN** la suite de tests s'exécute
- **THEN** un test vérifie dans le thème de l'application que le fond de l'écran de lancement du système est la couleur de fond de l'écran de démarrage et que son icône ne dessine rien
