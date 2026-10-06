# Spec Delta

## ADDED Requirements

### Requirement: Rôle d'écran d'accueil
SygixOs SHALL être éligible au rôle système HOME et SHALL permettre à l'utilisateur de demander son attribution par le mécanisme officiel Android, uniquement après un consentement explicite. SygixOs SHALL distinguer son explication initiale des écrans système de choix ou de confirmation. L'application SHALL refléter l'état réellement accordé; elle ne SHALL pas modifier le rôle sans action utilisateur.

#### Scenario: proposition initiale
- **WHEN** l'utilisateur ouvre SygixOs pour la première fois et que le rôle HOME n'est pas attribué
- **THEN** SygixOs lui présente une explication distincte du dialogue Android et ne demande l'attribution du rôle qu'après son action explicite

#### Scenario: attribution acceptée ou refusée
- **WHEN** l'utilisateur accepte ou refuse le mécanisme système d'attribution HOME
- **THEN** SygixOs reprend au premier plan et affiche l'état effectivement attribué; un refus ne modifie pas le launcher par défaut et n'empêche pas l'utilisation de SygixOs

#### Scenario: rôle déjà attribué ou remplacé
- **WHEN** SygixOs revient au premier plan après que l'utilisateur a changé le rôle HOME dans les réglages système
- **THEN** l'état affiché reflète le rôle courant, sans afficher à tort une activation réussie

### Requirement: Retour Home sur les appareils compatibles
Lorsque l'utilisateur active le recours de compatibilité dans les réglages P5, SygixOs SHALL proposer son service d'accessibilité pour retourner au launcher quand la touche Home est détournée par le système ou l'OEM. Cette fonction SHALL rester désactivée tant que l'utilisateur ne l'a pas activée explicitement dans les réglages système; SygixOs SHALL expliquer séparément son utilité et l'étape système requise. Elle SHALL pouvoir être désactivée dans les réglages de SygixOs et SHALL tolérer un service indisponible, désactivé ou interrompu sans bloquer le launcher.

#### Scenario: activation explicite
- **WHEN** l'utilisateur active le recours de compatibilité et choisit de poursuivre
- **THEN** SygixOs explique que l'activation s'effectue dans les réglages système, puis ouvre le parcours système approprié sans prétendre accorder elle-même l'accès

#### Scenario: accès refusé ou service arrêté
- **WHEN** l'utilisateur n'active pas le service, le désactive, ou que le système l'arrête
- **THEN** le recours de compatibilité reste inactif et SygixOs demeure utilisable; l'état est présenté comme inactif

#### Scenario: touche Home détournée
- **WHEN** le service est activé par l'utilisateur et reçoit un événement Home pris en charge par l'appareil
- **THEN** SygixOs retourne au premier plan sans annoncer une interception garantie sur les appareils qui ne transmettent pas cet événement

### Requirement: Démarrage à l'allumage
Lorsque l'utilisateur active l'option de démarrage dans les réglages P5, SygixOs SHALL demander le démarrage ou la reprise de l'accueil au démarrage de l'appareil uniquement par les mécanismes système disponibles et autorisés. Le comportement SHALL être réversible et ne SHALL pas être présenté comme garanti si le système ou l'OEM l'empêche.

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
