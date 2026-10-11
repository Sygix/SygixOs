# Spec Delta

## MODIFIED Requirements

### Requirement: Modèle canonique
La rangée SHALL manipuler un modèle canonique `UpNextItem` indépendant de l'app d'origine, avec un champ `externalIds` facultatif et la liste des sources du contenu.

#### Scenario: constitution
- **WHEN** un programme watch next est converti en item de la rangée
- **THEN** l'item expose le package source, le type (épisode ou film, publié ou inféré selon « Champs facultatifs »), le titre de série, la saison, l'épisode, le titre d'affichage, le poster, la progression optionnelle, le type watch next, le timestamp d'activité, l'intent publié et le nom de l'app source ; le champ `externalIds` est présent mais vide en P6

#### Scenario: sources du contenu
- **WHEN** des items sont fusionnés par le dédoublonnage
- **THEN** l'item retenu conserve la liste des sources qui possèdent le contenu (package, nom, icône et intent publié de chacune), après fusion comme avant ; c'est cette liste qu'affiche le menu « Ouvrir avec… »

#### Scenario: extension par le change P8
- **WHEN** le change P9 dédié à BetaSeries apporte des identifiants externes (enrichissement) aux items de toutes les sources
- **THEN** le modèle canonique et la rangée absorbent ces identifiants sans refonte, et le niveau 5 de dédoublonnage s'active quand ils sont connus ; en P6, `externalIds` reste vide et le niveau 5 ne s'applique jamais

### Requirement: Dédoublonnage en niveaux
La rangée SHALL dédoublonner les items par une clé d'identité en 5 niveaux, du plus fiable au moins fiable : (1) doublons exacts dans une app ; (2) au plus une carte par série dans une app ; (3) même épisode entre apps ; (4) même film entre apps ; (5) identifiant externe apporté par l'enrichissement BetaSeries du change P9 dédié, prenant le pas sur les niveaux 3 et 4 quand il est connu (inactif en P6). Aux niveaux 3 et 4, le type (épisode ou film) est le `COLUMN_TYPE` publié, sinon le type inféré défini par « Champs facultatifs ».

#### Scenario: doublons dans une app
- **WHEN** une app publie le même contenu plusieurs fois
- **THEN** un seul item est retenu, identifié par `package_name` + `internal_provider_id`, sinon `content_id`, sinon `intent_uri`

#### Scenario: série dans une app
- **WHEN** une app publie à la fois l'épisode en cours d'une série et son épisode suivant
- **THEN** une seule carte est affichée pour la série et l'épisode en cours prime

#### Scenario: épisode entre apps
- **WHEN** deux apps publient le même épisode (titre de série normalisé + saison + épisode), que le type soit publié ou inféré
- **THEN** une seule carte est retenue

#### Scenario: film entre apps
- **WHEN** deux apps publient un film de même titre normalisé et de même année, que le type soit publié ou inféré
- **THEN** une seule carte est retenue

#### Scenario: remake
- **WHEN** deux apps publient un film de même titre normalisé et que les deux années sont connues et différentes
- **THEN** aucune fusion n'a lieu

#### Scenario: types différents
- **WHEN** deux apps publient un même titre normalisé, l'un comme film et l'autre comme épisode (type publié ou inféré)
- **THEN** aucune fusion n'a lieu

#### Scenario: année inconnue
- **WHEN** l'année est connue d'un seul côté et le titre normalisé n'est pas associé à plusieurs années connues distinctes
- **THEN** la fusion a lieu sur le titre normalisé seul, limite assumée et documentée

#### Scenario: année inconnue entre plusieurs remakes
- **WHEN** trois apps publient un film de même titre normalisé, avec les années 1982, inconnue et 2011
- **THEN** l'item sans année reste séparé des deux remakes ; aucune fusion ne relie les années connues distinctes, quel que soit l'ordre des programmes

#### Scenario: normalisation stricte
- **WHEN** les titres sont comparés
- **THEN** la comparaison est une égalité stricte après normalisation (minuscules, diacritiques retirés, ponctuation et « (année) » supprimés, espaces compactés), sans correspondance approximative

#### Scenario: limite assumée
- **WHEN** deux apps publient le même contenu sous des titres localisés différents
- **THEN** les items restent en double jusqu'à l'activation du niveau 5 par le change P9 dédié à BetaSeries, et cette limite est documentée
