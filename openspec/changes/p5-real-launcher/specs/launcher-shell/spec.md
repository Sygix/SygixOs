# Spec Delta

## ADDED Requirements

### Requirement: Rôle d'écran d'accueil
SygixOs SHALL rester candidat au rôle système d'écran d'accueil (HOME) et SHALL permettre à l'utilisateur d'en demander l'attribution par le mécanisme officiel d'Android, uniquement après une action explicite de sa part sur le contrôle « Remplacer le launcher » (« Réglages du launcher système » de `settings`) ou sur l'élément correspondant de la présentation initiale (« Présentation initiale du launcher »). SygixOs SHALL ne jamais demander ni modifier ce rôle sans cette action, et SHALL afficher l'état réellement attribué, relu auprès du système à chaque retour au premier plan, avec les états définis par « Réglages du launcher système » de `settings`.

#### Scenario: demande après action explicite
- **WHEN** le rôle n'est pas détenu par SygixOs et que l'utilisateur valide « Remplacer le launcher » ou l'élément HOME de la présentation initiale
- **THEN** le dialogue système de demande du rôle s'ouvre ; aucune demande n'a lieu sans cette validation

#### Scenario: attribution acceptée
- **WHEN** l'utilisateur accepte l'attribution dans le dialogue système
- **THEN** au retour au premier plan, SygixOs relit le rôle et l'affiche « actif »

#### Scenario: attribution refusée
- **WHEN** l'utilisateur refuse ou ferme le dialogue système
- **THEN** au retour au premier plan, le rôle est affiché « inactif », le launcher par défaut n'est pas modifié et SygixOs reste utilisable

#### Scenario: rôle indisponible
- **WHEN** le système déclare le rôle indisponible, ou que le dialogue de demande ne peut pas s'ouvrir
- **THEN** le contrôle est affiché « indisponible », aucun écran système n'est ouvert et SygixOs reste utilisable

#### Scenario: rôle changé hors de SygixOs
- **WHEN** SygixOs revient au premier plan après que l'utilisateur a attribué le rôle HOME à une autre app ou à SygixOs dans les réglages système
- **THEN** l'état affiché est le rôle courant relu auprès du système, sans valeur conservée d'avant la sortie

### Requirement: Présentation initiale du launcher
Au premier lancement où elle n'a encore jamais été fermée, SygixOs SHALL afficher sur l'accueil une présentation initiale, après la fin de l'écran de démarrage (« Écran de démarrage ») et jamais par-dessus un dialogue système. Elle SHALL proposer, chacun avec son état (états de « Réglages du launcher système » de `settings`) : l'attribution du rôle HOME (« Rôle d'écran d'accueil »), le choix d'activer ou non le démarrage à l'allumage (« Démarrage à l'allumage ») et l'activation du service d'accessibilité (« Retour Home par le service d'accessibilité »). Elle SHALL être distincte des écrans système, ne SHALL rien activer sans action explicite et SHALL pouvoir être ignorée. Une fois fermée, quel qu'en soit le moyen, elle SHALL ne plus jamais s'afficher automatiquement ; les mêmes contrôles restent dans la catégorie « Écran d'accueil » des réglages. Le rendu visuel, les libellés définitifs et la navigation D-pad détaillée suivent les maquettes Penpot à venir.

#### Scenario: premier lancement
- **WHEN** SygixOs se lance et que la présentation initiale n'a jamais été fermée
- **THEN** à la fin de l'écran de démarrage, la présentation s'affiche sur l'accueil avec les éléments rôle HOME, démarrage à l'allumage et service d'accessibilité, chacun avec son état courant

#### Scenario: présentation ignorée
- **WHEN** l'utilisateur ignore la présentation (action d'ignorer ou Retour) sans rien valider
- **THEN** la présentation se ferme, l'accueil reprend avec le focus sur le héro, le rôle HOME n'est pas demandé, l'option de démarrage reste désactivée et aucun écran d'accessibilité n'est ouvert

#### Scenario: pas de nouvel affichage
- **WHEN** SygixOs se lance de nouveau après la fermeture de la présentation, y compris après un redémarrage de l'appareil
- **THEN** la présentation ne s'affiche pas

#### Scenario: retour d'un écran système ouvert depuis la présentation
- **WHEN** l'utilisateur a ouvert le dialogue du rôle HOME ou les réglages d'accessibilité depuis la présentation, puis revient dans SygixOs par Retour
- **THEN** la présentation est toujours affichée, l'état de l'élément est relu auprès du système et le focus revient sur l'élément qui a ouvert l'écran système

#### Scenario: choix du démarrage à l'allumage
- **WHEN** l'utilisateur choisit d'activer ou de laisser désactivé le démarrage à l'allumage dans la présentation
- **THEN** le choix est persisté aussitôt et la même valeur apparaît dans la catégorie « Écran d'accueil » des réglages

#### Scenario: processus arrêté pendant un écran système
- **WHEN** le système arrête le processus de SygixOs pendant qu'un écran système ouvert depuis la présentation est affiché, puis SygixOs se relance
- **THEN** la présentation, jamais fermée, s'affiche de nouveau avec les états relus

### Requirement: Démarrage à l'allumage
SygixOs SHALL proposer une option de démarrage à l'allumage, désactivée tant que l'utilisateur ne l'a pas activée dans la présentation initiale ou dans les réglages. Option activée, à la réception de la fin du démarrage du système, SygixOs SHALL demander l'ouverture de son accueil par les seuls mécanismes autorisés par Android, sans contourner ses restrictions. SygixOs SHALL enregistrer, pour chaque démarrage de l'appareil, si cette ouverture a été observée ; l'état « démarrage automatique non observé » qui en découle est affiché par « Réglages du launcher système » de `settings`. Option désactivée, SygixOs SHALL ne demander aucune ouverture au démarrage.

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

### Requirement: Retour Home par le service d'accessibilité
SygixOs SHALL fournir un service d'accessibilité qui, une fois activé par l'utilisateur dans les réglages d'accessibilité du système, ramène SygixOs au premier plan sur son accueil quand l'appui sur la touche Home est détourné par le système ou le constructeur vers un autre launcher. SygixOs SHALL ne jamais activer ni désactiver ce service lui-même : la présentation initiale et le contrôle des réglages ouvrent seulement les réglages d'accessibilité du système. Après un refus, le service SHALL n'être proposé de nouveau que depuis la catégorie « Écran d'accueil » des réglages, sans relance automatique. Le service SHALL ne lire que les événements nécessaires à ce signal et ne rien conserver. Service non activé, désactivé ou arrêté, SygixOs SHALL rester pleinement utilisable. Le signal exploité est celui confirmé sur la TV de référence (tâche 1.3 de ce change).

#### Scenario: activation par l'utilisateur
- **WHEN** l'utilisateur valide l'élément accessibilité de la présentation initiale ou le contrôle correspondant des réglages
- **THEN** les réglages d'accessibilité du système s'ouvrent ; au retour au premier plan, l'état du service est relu et affiché « actif » seulement si le système le déclare activé

#### Scenario: service refusé
- **WHEN** l'utilisateur revient des réglages d'accessibilité sans avoir activé le service, ou ferme la présentation sans l'ouvrir
- **THEN** le service est affiché « inactif », SygixOs reste utilisable et aucune nouvelle proposition ne s'affiche automatiquement, à ce lancement comme aux suivants

#### Scenario: touche Home détournée, service activé
- **WHEN** le service a été activé par l'utilisateur, que SygixOs est en arrière-plan et que l'appui sur Home affiche le launcher du constructeur
- **THEN** SygixOs revient au premier plan sur son accueil, selon les règles de retour existantes (« Écran de démarrage », scénario « retour sur le launcher »)

#### Scenario: touche Home détournée depuis SygixOs, service activé
- **WHEN** le service a été activé par l'utilisateur, que SygixOs est au premier plan et que l'appui sur Home affiche le launcher du constructeur
- **THEN** SygixOs revient au premier plan et applique « Touche Home avec SygixOs au premier plan »

#### Scenario: service désactivé ou arrêté
- **WHEN** l'utilisateur désactive le service dans les réglages système, ou que le système l'arrête
- **THEN** l'appui sur Home suit le comportement du système, aucune erreur n'est affichée, et au retour au premier plan le service est affiché « inactif »

### Requirement: Touche Home avec SygixOs au premier plan
Quand la touche Home atteint SygixOs alors qu'il est déjà au premier plan, ou quand le service d'accessibilité signale un appui sur Home détourné alors que SygixOs était au premier plan (« Retour Home par le service d'accessibilité »), SygixOs SHALL fermer la page de réglages (comme au scénario « retour » de « Page de réglages » de `settings`) et toute surcouche de l'accueil comme le ferait Retour sur cette surcouche, ramener la page de l'accueil en haut et donner le focus au héro. Hors premier plan, les règles de retour existantes s'appliquent sans changement (« Écran de démarrage », scénario « retour sur le launcher »).

#### Scenario: depuis la grille
- **WHEN** le focus est dans la zone grille et que la touche Home atteint SygixOs
- **THEN** la page défile jusqu'au héro comme au scénario « retour depuis la grille » de « Navigation 3 paliers », le héro prend le focus et sa lecture reprend

#### Scenario: depuis les réglages
- **WHEN** la page de réglages est ouverte, quelle que soit la catégorie ou la ligne focalisée, et que la touche Home atteint SygixOs
- **THEN** les réglages se ferment comme au scénario « retour » de « Page de réglages » (`settings`), la page de l'accueil est en haut et le héro a le focus

#### Scenario: depuis une surcouche
- **WHEN** le menu contextuel d'une tuile, le mode « Déplacer » ou la présentation initiale est affiché et que la touche Home atteint SygixOs
- **THEN** la surcouche se ferme comme par Retour (déplacement annulé, présentation enregistrée comme fermée), la page de l'accueil est en haut et le héro a le focus

#### Scenario: déjà sur le héro
- **WHEN** le héro a déjà le focus, sans surcouche, et que la touche Home atteint SygixOs
- **THEN** le héro garde le focus et rien d'autre ne change

#### Scenario: pendant l'écran de démarrage
- **WHEN** la touche Home atteint SygixOs pendant l'écran de démarrage
- **THEN** l'écran de démarrage se poursuit sans changement et le héro prend le focus à son fondu de sortie (« Écran de démarrage »)
