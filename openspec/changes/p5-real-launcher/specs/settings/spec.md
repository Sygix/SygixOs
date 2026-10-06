# Spec Delta

## ADDED Requirements

### Requirement: Réglages P5 du launcher système
La page de réglages SHALL proposer une catégorie P5 permettant à l'utilisateur d'activer ou de désactiver le recours de remplacement du launcher système, de consulter l'état du rôle HOME et d'accéder à sa demande système, d'activer ou désactiver le recours d'accessibilité, et d'activer ou désactiver l'option de démarrage à l'allumage de l'appareil. Les états du rôle HOME et du service d'accessibilité SHALL refléter les états système réels, y compris après un retour de l'application Réglages Android. L'UI SHALL permettre de distinguer état inactif, actif, indisponible et action système en attente.

#### Scenario: états système actualisés
- **WHEN** l'utilisateur revient des réglages système Android vers la catégorie P5
- **THEN** les états du rôle HOME et de l'accessibilité sont relus et affichés tels qu'autorisés par le système

#### Scenario: options désactivées
- **WHEN** l'utilisateur désactive un recours P5 depuis les réglages SygixOs
- **THEN** SygixOs cesse de demander ce recours et conserve une navigation normale; elle ne révoque aucun accès système sans action autorisée de l'utilisateur

#### Scenario: capacité système indisponible
- **WHEN** le système ou l'appareil ne permet pas une action demandée dans P5
- **THEN** le contrôle indique qu'elle n'est pas disponible ou n'a pas abouti et les autres catégories de réglages restent utilisables

### Requirement: Présentation initiale de P5
Au premier lancement où le rôle HOME de SygixOs n'est pas attribué, SygixOs SHALL présenter une explication proposant à l'utilisateur de la définir comme launcher par défaut et d'activer les capacités système optionnelles nécessaires à P5. Cette présentation SHALL être distincte des dialogues système Android, ne SHALL pas affirmer que SygixOs est le launcher par défaut avant confirmation et SHALL laisser l'utilisateur poursuivre ou ignorer la proposition. Le style visuel et le parcours D-pad de la présentation ainsi que de la catégorie P5 restent en attente du choix de maquette.

#### Scenario: choix d'ignorer
- **WHEN** l'utilisateur ignore ou ferme la proposition initiale
- **THEN** SygixOs s'ouvre normalement sans demander le rôle HOME ni activer une capacité optionnelle

#### Scenario: choix de poursuivre
- **WHEN** l'utilisateur choisit de poursuivre la proposition initiale
- **THEN** SygixOs guide vers le mécanisme système approprié pour le rôle HOME et explique séparément toute activation d'accessibilité, laquelle reste à la décision de l'utilisateur

#### Scenario: UI en attente de maquette
- **WHEN** les exigences visuelles de la catégorie P5 ou de la proposition initiale sont rédigées avant la sélection d'une variante
- **THEN** elles demeurent explicitement en placeholder et aucune mise en page ni décision de focus propre à P5 n'est imposée
