# Delta betaseries-integration

Alignement sur la décision P8 : BetaSeries est une étape d'**enrichissement** des items de toutes les sources, pas une concaténation après Jellyfin. L'éventuel rôle de BetaSeries comme source à part entière n'est pas tranché : voir « Questions ouvertes » de `proposal.md`.

## MODIFIED Requirements

### Requirement: Agrégation Up Next
Les données BetaSeries SHALL enrichir les items de toutes les sources de la rangée Up Next avec des IDs externes (IMDb/TVDB) activant le niveau 5 de dédoublonnage de la capability up-next, plutôt que s'y concaténer ; la rangée reste multi-sources.

#### Scenario: comportement
- **WHEN** la capability est utilisée (P8)
- **THEN** les IDs externes BetaSeries sont attachés aux items existants de toutes les sources (Jellyfin inclus), sans ordre « Jellyfin puis BetaSeries »

#### Scenario: fusion par ID externe
- **WHEN** un ID externe BetaSeries est connu pour deux items de sources différentes
- **THEN** ils sont fusionnés au niveau 5 de dédoublonnage, qui prend le pas sur les niveaux 3 et 4
