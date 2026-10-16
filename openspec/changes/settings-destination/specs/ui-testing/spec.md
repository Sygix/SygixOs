# Spec Delta

## ADDED Requirements

### Requirement: Couverture des destinations exclusives
Les destinations accueil/réglages SHALL être testées à taille TV (`w960dp-h540dp-xhdpi`) avec leurs sources contrôlées, les touches physiques et des tags stables de racine. Les tests SHALL prouver le démontage effectif de l'accueil, pas seulement son opacité ou sa perte de focus, et conserver les assertions existantes de réglages et des parcours P7.

#### Scenario: exclusivité
- **WHEN** le test ouvre les réglages depuis l'engrenage
- **THEN** `launcher-destination-settings` et `settings-screen` existent ; `launcher-destination-home`, `zone-hero`, `zone-dock` et `home-grid` sont absents ; la catégorie initiale a le focus

#### Scenario: arrêt et reprise
- **WHEN** une vidéo, une animation et une préparation d'image contrôlées sont actives puis que le test ouvre et ferme les réglages
- **THEN** leurs consommateurs d'accueil sont arrêtés pendant les réglages ; le retour rend le focus au héro et conserve la sélection encore valide, sans deuxième splash ni publication visuelle tardive dans l'ancienne composition

#### Scenario: ancre de grille
- **WHEN** le test quitte une rangée profonde, ouvre les réglages depuis le héro puis les ferme et redescend dans la grille
- **THEN** le héro a d'abord repris le focus à la fermeture, puis la cible de grille et sa position antérieure sont restaurées ; si elle a disparu, le repli existant est révélé et focusé

#### Scenario: sources et compteurs
- **WHEN** les réglages sont actifs et les doubles émettent un nouveau comptage, une nouvelle préférence et un résultat héro tardif
- **THEN** le comptage partagé et le contrôle concerné se mettent à jour sans remonter l'accueil ; le résultat visuel ne réactive rien, et le retour reflète les préférences courantes avec les états de chargement, vide, erreur et permission prévus

#### Scenario: Home et panneaux
- **WHEN** le test transmet un intent Home valide au premier plan avec la liste de position, le panneau P7 ou sa confirmation ouvert
- **THEN** les fermetures et les effets suivent leurs exigences ; l'accueil revient avec le héro focusé, et une configuration applicative en cours continue

#### Scenario: retour système
- **WHEN** le test ouvre un écran système depuis les réglages puis simule le retour avec un état modifié
- **THEN** les réglages et leur panneau restent la destination attendue, les états sont relus et le contrôle qui avait ouvert l'écran retrouve le focus ; aucune UI d'accueil n'est recomposée derrière
