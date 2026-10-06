# Spec Delta

## ADDED Requirements

### Requirement: Couverture des parcours fonctionnels P5
Les parcours fonctionnels P5 SHALL être couverts par des tests déterministes des états, des décisions utilisateur et des retours depuis les réglages système; les interactions avec Android SHALL être remplacées par des doubles contrôlés dans les tests JVM. Les tests SHALL vérifier qu'un refus ou une capacité indisponible ne bloque pas le launcher et qu'aucun état n'est annoncé comme accordé avant confirmation. Les exigences de rendu, de disposition et de navigation D-pad de l'UI P5 restent en placeholder jusqu'au choix d'une variante de maquette.

#### Scenario: refus du rôle HOME
- **WHEN** le test simule le refus de l'attribution du rôle HOME
- **THEN** l'état reste non attribué, la proposition demeure non bloquante et l'accueil reste utilisable

#### Scenario: accessibilité inactive ou interrompue
- **WHEN** le test simule l'accessibilité inactive, refusée ou arrêtée
- **THEN** le recours de compatibilité est indiqué inactif et les autres contrôles P5 restent utilisables

#### Scenario: états relus après retour système
- **WHEN** le test simule un changement d'état HOME ou accessibilité pendant l'ouverture des réglages Android
- **THEN** le retour dans P5 affiche le nouvel état et ne conserve pas une valeur périmée

#### Scenario: option de démarrage bloquée
- **WHEN** le test simule un refus ou une indisponibilité système du démarrage automatique
- **THEN** l'échec est distingué d'un démarrage réussi et SygixOs reste utilisable
