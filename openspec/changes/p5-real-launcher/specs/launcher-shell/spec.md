# Spec Delta

## ADDED Requirements

### Requirement: Rôle d'écran d'accueil
Le rôle système HOME fait partie du périmètre P5 confirmé. SygixOs SHALL être éligible à ce rôle et SHALL permettre à l'utilisateur d'en demander l'attribution via le mécanisme officiel Android, uniquement après une action explicite. SygixOs SHALL distinguer toute explication qu'elle présente des écrans système. L'application SHALL refléter l'état réellement accordé; elle ne SHALL pas modifier le rôle sans action utilisateur. Le déclenchement et la forme d'une éventuelle présentation initiale restent ouverts et ne sont pas imposés ici.

#### Scenario: proposition initiale
- **WHEN** l'utilisateur choisit le parcours d'attribution HOME et que le rôle n'est pas attribué
- **THEN** SygixOs peut présenter une explication distincte du dialogue Android, puis ne demande l'attribution qu'après l'action explicite de l'utilisateur

#### Scenario: attribution acceptée ou refusée
- **WHEN** l'utilisateur accepte ou refuse le mécanisme système d'attribution HOME
- **THEN** SygixOs reprend au premier plan et affiche l'état effectivement attribué; un refus ne modifie pas le launcher par défaut et n'empêche pas l'utilisation de SygixOs

#### Scenario: rôle déjà attribué ou remplacé
- **WHEN** SygixOs revient au premier plan après que l'utilisateur a changé le rôle HOME dans les réglages système
- **THEN** l'état affiché reflète le rôle courant, sans afficher à tort une activation réussie

### Requirement: Retour Home sur les appareils compatibles
Uniquement si la conformité et la politique de distribution sont validées et si le recours est retenu dans le périmètre produit, SygixOs SHALL proposer le service d'accessibilité pour retourner au launcher quand la touche Home est détournée par le système ou l'OEM. Dans ce cas, la fonction SHALL rester désactivée tant que l'utilisateur ne l'a pas activée explicitement dans les réglages système; SygixOs SHALL expliquer séparément son utilité et l'étape système requise. Elle SHALL pouvoir être désactivée selon le parcours retenu et SHALL tolérer un service indisponible, désactivé ou interrompu sans bloquer le launcher. Avant ces validations, aucun service, recours ou contrôle d'accessibilité n'est exigé par cette spécification.

#### Scenario: activation explicite après validation
- **WHEN** conformité, distribution et inclusion du recours sont validées, puis l'utilisateur choisit de poursuivre son activation
- **THEN** SygixOs explique que l'activation s'effectue dans les réglages système, puis ouvre le parcours système approprié sans prétendre accorder elle-même l'accès

#### Scenario: accès refusé ou service arrêté après validation
- **WHEN** le recours est validé et retenu, et que l'utilisateur n'active pas le service, le désactive, ou que le système l'arrête
- **THEN** le recours de compatibilité reste inactif et SygixOs demeure utilisable; l'état est présenté comme inactif

#### Scenario: touche Home détournée
- **WHEN** le recours est validé et retenu, le service est activé par l'utilisateur et reçoit un événement Home pris en charge par l'appareil
- **THEN** une activité de SygixOs devient visible au premier plan (état de reprise de l'activité observable), sans annoncer une interception garantie sur les appareils qui ne transmettent pas cet événement

### Requirement: Option de démarrage à l'allumage
Une option permettant de demander le démarrage ou la reprise de SygixOs à l'allumage TV fait partie du périmètre P5 confirmé. Lorsque l'utilisateur l'active, SygixOs SHALL demander ce démarrage uniquement par les mécanismes système disponibles et autorisés. Le comportement SHALL être réversible et ne SHALL pas être présenté comme garanti si le système ou l'OEM l'empêche. Le mécanisme exact et l'état initial de l'option restent à décider; aucune activation automatique par défaut n'est exigée.

#### Scenario: démarrage activé
- **WHEN** l'appareil démarre et que l'option est activée
- **THEN** SygixOs tente de s'ouvrir ou de reprendre conformément aux possibilités système, sans contourner les restrictions du système

#### Scenario: démarrage désactivé
- **WHEN** l'appareil démarre et que l'option est désactivée
- **THEN** SygixOs ne demande pas son propre démarrage automatique

#### Scenario: démarrage bloqué
- **WHEN** le système ou l'OEM empêche l'ouverture automatique
- **THEN** l'application reste utilisable lors d'une ouverture manuelle et ne signale pas le démarrage comme garanti

### Requirement: Comportement d'UI P5
<!-- Placeholder UI: compléter les exigences visuelles et de navigation après sélection par Simon d'une variante de la maquette « SygixOs UI tvOS 26-png.zip ». Les exigences fonctionnelles P5 ci-dessus ne dépendent pas de ce choix. -->
SygixOs SHALL présenter les contrôles fonctionnels P5 dans ses réglages et SHALL distinguer les états activé, désactivé, indisponible et en attente de l'action de l'utilisateur dans le système.

#### Scenario: état de réglage P5
- **WHEN** l'utilisateur consulte les contrôles fonctionnels P5
- **THEN** chaque contrôle reflète l'état connu et n'indique pas une permission ou une capacité système comme accordée avant confirmation du système
