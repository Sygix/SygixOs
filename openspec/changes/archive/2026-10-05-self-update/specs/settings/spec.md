# Delta settings

## MODIFIED Requirements

### Requirement: À propos
La catégorie « À propos » SHALL afficher la version du launcher et les licences des bibliothèques open source utilisées. Elle SHALL aussi contenir les lignes de mise à jour du launcher, décrites par `self-update` (« Lignes de mise à jour dans À propos »), qui fixe leur contenu, leur place et leur navigation D-pad.

#### Scenario: contenu
- **WHEN** la catégorie « À propos » est affichée
- **THEN** la version de l'application et la liste des licences OSS sont visibles, navigables au DPAD

#### Scenario: lignes de mise à jour
- **WHEN** la catégorie « À propos » est affichée
- **THEN** les lignes de mise à jour de `self-update` sont visibles avant la liste des licences, avec le comportement que `self-update` spécifie
