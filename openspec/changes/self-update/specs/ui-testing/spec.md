# Delta ui-testing

## ADDED Requirements

### Requirement: Couverture des mises à jour
Les lignes de mise à jour d'« À propos », le code QR des notes de version et la pastille (`self-update`, « Pastille » de `launcher-shell`) SHALL être couverts par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), avec un état de mise à jour déterministe fourni par le test, les key events physiques du D-pad et des testTags stables : « update-check », « update-install », « update-release-notes-qr », « update-prereleases », « update-prereleases-switch », « update-badge-gear », « update-badge-about ». La logique et le transport sont couverts par « Couverture de test des mises à jour » de `self-update`.

#### Scenario: navigation dans À propos
- **WHEN** le test ouvre « À propos » et presse droite, puis bas et haut
- **THEN** « update-check » prend le focus en premier ; bas parcourt « update-install » (si présente), « update-prereleases » puis les licences dans l'ordre affiché ; haut depuis « update-check » et bas depuis la dernière licence ne bougent pas le focus ; gauche rend le focus à la catégorie « À propos »

#### Scenario: ligne Mettre à jour
- **WHEN** l'état passe de « à jour » à « version disponible » puis revient à « à jour » alors que « update-install » a le focus
- **THEN** « update-install » apparaît juste sous « update-check » sans voler le focus, avec le libellé « Mettre à jour vers X » ; à sa disparition le focus passe à « update-check »

#### Scenario: code QR des notes de version
- **WHEN** une version est proposée avec une URL de release HTTPS de github.com, puis avec une URL invalide
- **THEN** « update-release-notes-qr » est affiché à droite de « update-install » et n'est jamais focusé dans le premier cas, absent dans le second

#### Scenario: états affichés
- **WHEN** le test fournit successivement les états jamais vérifié, en cours, à jour, version disponible, erreur sans version connue, erreur avec version connue, téléchargement à 42 %
- **THEN** les textes de « update-check » et « update-install » sont ceux de « Vérification des mises à jour » et « Téléchargement vérifié » (`self-update`)

#### Scenario: préversions
- **WHEN** le test bascule « update-prereleases » avec des versions connues contenant une préversion plus récente
- **THEN** l'état de « update-prereleases-switch », la version annoncée et la présence de « update-badge-gear » et « update-badge-about » changent aussitôt, sans appel au transport factice

#### Scenario: pastille sans effet sur le focus
- **WHEN** une version est proposée et le test parcourt héro, engrenage et retour au héro
- **THEN** « update-badge-gear » est affichée, n'est jamais focusée, et la position et la taille de la capsule et de l'engrenage sont identiques à celles d'un test sans version proposée
