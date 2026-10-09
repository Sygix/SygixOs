# Spec Delta

## ADDED Requirements

### Requirement: Couverture des parcours du launcher système
Les parcours P5 SHALL être couverts par des tests déterministes, sans environnement Android réel : le rôle HOME, l'état du service d'accessibilité, la fin du démarrage, le numéro de démarrage et l'ouverture des écrans système SHALL être fournis par des doubles contrôlés. La logique (états, présentation initiale, démarrage observé) SHALL être testée en JUnit ; la navigation et l'affichage, par des tests Compose à la taille d'une TV (`w960dp-h540dp-xhdpi`) avec des testTags stables. Les tests du service d'accessibilité SHALL distinguer le service activé par l'utilisateur (double déclaré activé par le système) du service non activé, désactivé ou arrêté. Les assertions de rendu et de navigation D-pad détaillée de la présentation initiale et des contrôles P5 suivent les maquettes Penpot à venir.

#### Scenario: refus du rôle HOME
- **WHEN** le test simule l'ouverture du dialogue du rôle HOME puis un retour au premier plan avec le rôle non détenu
- **THEN** le contrôle passe par « action système en attente » puis affiche « inactif », jamais « actif », et l'accueil reste utilisable

#### Scenario: rôle HOME accepté ou indisponible
- **WHEN** le test simule un retour au premier plan avec le rôle détenu, puis un rôle déclaré indisponible par le système avec, puis sans, écran système des applications par défaut ou de l'écran d'accueil
- **THEN** le contrôle affiche « actif » dans le premier cas ; avec l'écran de remplacement, sa validation ouvre cet écran ; sans lui, le contrôle affiche « indisponible » et aucun écran système n'est ouvert

#### Scenario: états relus après retour système
- **WHEN** le test change l'état du rôle HOME ou du service d'accessibilité pendant que SygixOs est en arrière-plan, puis simule le retour au premier plan
- **THEN** les contrôles affichent le nouvel état, sans valeur périmée, et le focus est sur le contrôle qui avait ouvert l'écran système

#### Scenario: présentation initiale ignorée
- **WHEN** le test lance SygixOs sans présentation enregistrée comme fermée, simule la réponse à la demande de permission « Programmes TV » (la présentation n'apparaît qu'après), puis presse Retour sur la présentation
- **THEN** la présentation est enregistrée comme fermée, le héro a le focus, aucune demande de rôle ni ouverture d'écran système n'a eu lieu, l'option de démarrage est désactivée, et un second lancement simulé n'affiche pas la présentation

#### Scenario: service d'accessibilité non activé par l'utilisateur
- **WHEN** le double déclare le service non activé, désactivé ou arrêté
- **THEN** le contrôle d'accessibilité affiche « inactif », un signal Home simulé ne ramène pas SygixOs au premier plan, les autres contrôles restent utilisables et aucune nouvelle proposition n'est affichée automatiquement

#### Scenario: service d'accessibilité activé par l'utilisateur
- **WHEN** le double déclare le service activé par l'utilisateur et que le test simule le signal Home détourné, SygixOs étant en arrière-plan puis au premier plan
- **THEN** l'ouverture de l'accueil de SygixOs est demandée dans les deux cas ; au premier plan, le comportement de « Touche Home avec SygixOs au premier plan » s'applique

#### Scenario: démarrage automatique non observé
- **WHEN** le test simule un démarrage (numéro de démarrage incrémenté) avec l'option activée avant ce démarrage, sans ouverture observée, puis affiche « Écran d'accueil »
- **THEN** le contrôle de démarrage indique que le démarrage automatique n'a pas eu lieu ; avec une ouverture au démarrage observée, ou l'option désactivée, ou l'option activée après ce démarrage, aucune indication d'échec n'est affichée

#### Scenario: touche Home au premier plan
- **WHEN** le test ouvre les réglages (y compris avec la liste déroulante « Position d'Up Next » ouverte), ou descend dans la grille, ou ouvre le menu contextuel d'une tuile, ou active le mode « Déplacer », ou affiche la présentation initiale, puis transmet un intent Home à l'activité au premier plan
- **THEN** réglages et surcouche sont fermés en une fois (déplacement annulé, valeur de « Position d'Up Next » inchangée, présentation enregistrée comme fermée), la page de l'accueil est en haut et le héro a le focus

#### Scenario: mise à jour sans demande de rôle
- **WHEN** le test exécute une mise à jour intégrée factice jusqu'à la relance, avec un double du rôle HOME non détenu
- **THEN** aucune demande du rôle HOME n'est faite par le parcours de mise à jour
