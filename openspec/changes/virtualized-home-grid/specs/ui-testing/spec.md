# Spec Delta

## MODIFIED Requirements

### Requirement: Couverture grille et shelf
La grille SHALL être couverte par des tests de rendu et d'insertion du panneau shelf. Les assertions SHALL distinguer accessibilité par navigation et présence simultanée dans la composition : une app hors fenêtre est testée après révélation, sans exiger la composition de tout le catalogue.

#### Scenario: grille rendue
- **WHEN** le catalogue contient des apps non épinglées
- **THEN** chaque app est accessible une fois dans la grille, sans doublon avec le dock ; une cible hors fenêtre est révélée avant les assertions de rendu et de focus

#### Scenario: grille vide
- **WHEN** le catalogue ne contient aucune app
- **THEN** le message « Aucune app TV détectée » est affiché

#### Scenario: panneau shelf au focus
- **WHEN** une tuile d'app avec contenu publié prend le focus
- **THEN** le panneau shelf est inséré dans la grille (présence vérifiée par testTag)

#### Scenario: shelf sans contenu
- **WHEN** l'app ne publie rien
- **THEN** aucun panneau n'est inséré

## ADDED Requirements

### Requirement: Couverture de la grille virtualisée
Les tests SHALL couvrir les cibles hors fenêtre, leur recyclage et la préparation bornée à taille TV (`w960dp-h540dp-xhdpi`), avec catalogue et artworks déterministes. Les tests SHALL observer les nœuds attachés et les appels de préparation sans désactiver la virtualisation ni utiliser de délai réel comme preuve de performance.

#### Scenario: coût borné
- **WHEN** le test fournit des catalogues synthétiques croissants dans un même viewport
- **THEN** une tuile éloignée n'est pas attachée initialement, les tuiles de la fenêtre le sont, et les comptes de composition et de préparation restent liés à la fenêtre documentée plutôt qu'à la taille du catalogue

#### Scenario: restauration profonde
- **WHEN** le test atteint une rangée initialement non composée, quitte la grille par Retour puis y redescend
- **THEN** l'app mémorisée est révélée et focusée à sa position précédente, sans composer toutes les rangées intermédiaires pour la conserver

#### Scenario: focus supprimé et déplacement
- **WHEN** le test supprime la cible mémorisée puis traverse une limite de fenêtre en mode déplacement, avec validation et annulation dans des cas distincts
- **THEN** le repli par identité, l'ordre et les positions restent ceux des exigences du domaine ; aucun test ne remplace le D-pad par un clic sémantique pour masquer une erreur de focus

#### Scenario: transitions et matériaux
- **WHEN** les tests existants de transition, de shelf et de verre sont exécutés avec une grille profonde
- **THEN** leurs assertions de géométrie, de continuité et de non-recréation du contenu du dock pendant les bascules de verre restent satisfaites ; seul le harnais d'accès aux tuiles hors fenêtre est adapté

#### Scenario: préchargement annulé
- **WHEN** une préparation contrôlée est suspendue puis la fenêtre ou la destination change
- **THEN** les demandes non commencées de l'ancienne fenêtre ne sont pas lancées et un résultat tardif ne modifie pas une autre tuile ; les erreurs et absences conservent leurs replis
