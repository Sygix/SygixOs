# Spec Delta

## MODIFIED Requirements

### Requirement: Couverture grille et shelf
La grille SHALL être couverte par des tests de rendu et d'insertion du panneau shelf, y compris avec une grille plus haute que le seuil de composition de « Préchargement et mémoire » (`launcher-shell`), à la taille d'une TV (`w960dp-h540dp-xhdpi`). Les assertions SHALL porter sur les nœuds réels (tuiles, panneau, zone grille), sans marqueur ni sélecteur ajouté pour les tests.

#### Scenario: grille rendue
- **WHEN** le catalogue contient des apps non épinglées
- **THEN** chaque app apparaît une fois en grille, sans doublon avec le dock

#### Scenario: grille vide
- **WHEN** le catalogue ne contient aucune app
- **THEN** le message « Aucune app TV détectée » est affiché

#### Scenario: panneau shelf au focus
- **WHEN** une tuile d'app avec contenu publié prend le focus
- **THEN** le panneau shelf est inséré dans la grille (présence vérifiée par testTag)

#### Scenario: shelf sans contenu
- **WHEN** l'app ne publie rien
- **THEN** aucun panneau n'est inséré

#### Scenario: rangées composées
- **WHEN** le catalogue compte au plus le seuil de rangées, puis une rangée de plus, 200 et 2 000 apps
- **THEN** toutes les tuiles sont composées sous le seuil ; au-dessus, la première tuile l'est, la dernière ne l'est pas et le nombre de tuiles composées reste sous la borne de la fenêtre et de sa prélecture

#### Scenario: rafale et restauration profonde
- **WHEN** le test enchaîne des touches Bas sans attendre la fin du défilement, inverse avec Haut, puis quitte la grille (Retour, menu contextuel) et y revient
- **THEN** la tuile visée a le focus à chaque étape, se retrouve entre les marges une fois le défilement terminé, la même tuile reprend le focus à la même place au retour, et la première rangée n'est plus composée

#### Scenario: déplacement et Up Next après les apps
- **WHEN** le test déplace une app sur dix rangées puis valide, la déplace encore puis annule, ou descend jusqu'à la rangée Up Next placée après une grande grille et revient
- **THEN** l'app déplacée garde le focus et reste entre les marges, l'ordre validé est conservé et l'ordre annulé restauré ; la carte Up Next prend le focus, est entre les marges et est restaurée au retour

#### Scenario: panneau et verre sur une grande grille
- **WHEN** le panneau s'ouvre sur une rangée profonde d'une grille au-dessus du seuil
- **THEN** le panneau est sous la marge haute, immédiatement au-dessus de la rangée focusée, qui reste entre les marges ; la première affiche a été demandée avant l'ouverture et une validation tardive n'entraîne aucune nouvelle demande de l'affiche courante ; la zone grille porte toujours la source du verre et couvre toutes les rangées
