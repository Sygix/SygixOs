# Spec Delta

## ADDED Requirements

### Requirement: Réglages P5 du launcher système
Si, après résolution des questions ouvertes du design, une catégorie de réglages P5 et ses contrôles sont retenus, la page de réglages SHALL présenter uniquement les contrôles et actions système correspondant au périmètre validé. Pour tout contrôle retenu qui expose un état système, l'UI SHALL refléter l'état réel, y compris après un retour de l'application Réglages Android, et distinguer les états pertinents parmi inactif, actif, indisponible et action système en attente. Aucun contrôle relatif au remplacement HOME, à l'accessibilité ou au démarrage n'est imposé avant validation de son périmètre et de sa sémantique.

#### Scenario: état système actualisé pour un contrôle retenu
- **WHEN** un contrôle P5 retenu selon le périmètre validé expose un état système et que l'utilisateur revient des réglages système Android
- **THEN** l'état correspondant est relu et affiché conformément à l'état rapporté par le système

#### Scenario: contrôle retenu désactivé
- **WHEN** l'utilisateur désactive un contrôle P5 inclus dans le périmètre validé
- **THEN** SygixOs applique le comportement convenu pour ce contrôle, conserve une navigation normale et ne révoque aucun accès système sans action autorisée de l'utilisateur

#### Scenario: capacité retenue indisponible
- **WHEN** une action système incluse dans le périmètre P5 validé n'est pas disponible ou n'aboutit pas
- **THEN** le contrôle correspondant indique cette indisponibilité ou cet échec et les autres catégories de réglages restent utilisables

### Requirement: Présentation initiale de P5
Si une présentation initiale P5 et son déclenchement sont retenus après résolution des questions ouvertes du design, son contenu et ses actions SHALL se limiter au parcours validé. Toute explication précédant une demande de rôle HOME ou une capacité système SHALL être distincte des dialogues système Android, ne SHALL pas annoncer un état non confirmé et SHALL laisser l'utilisateur poursuivre ou ignorer cette proposition. Aucun moment de présentation ni aucune capacité à proposer n'est imposé avant validation des décisions correspondantes. Le style visuel et le parcours D-pad restent en attente du choix de maquette.

#### Scenario: proposition retenue ignorée
- **WHEN** une proposition initiale a été retenue et que l'utilisateur l'ignore ou la ferme
- **THEN** SygixOs s'ouvre normalement sans demander le rôle HOME ni activer une capacité optionnelle sans action explicite

#### Scenario: parcours retenu poursuivi
- **WHEN** l'utilisateur choisit de poursuivre un parcours initial validé
- **THEN** SygixOs guide uniquement vers le mécanisme système prévu par ce parcours et toute activation de capacité sensible reste soumise à l'action explicite de l'utilisateur

#### Scenario: UI en attente de maquette
- **WHEN** les exigences visuelles de la catégorie P5 ou de la proposition initiale sont rédigées avant la sélection d'une variante
- **THEN** elles demeurent explicitement en placeholder et aucune mise en page ni décision de focus propre à P5 n'est imposée
