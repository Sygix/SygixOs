# Delta betaseries-integration

Alignement sur la décision P4 : BetaSeries est d'abord une étape d'**enrichissement** des items de toutes les sources, pas une concaténation après Jellyfin.

## MODIFIED Requirements

### Requirement: Agrégation Up Next
Les épisodes à voir BetaSeries SHALL enrichir les items de toutes les sources de la rangée Up Next avec des IDs externes (IMDb/TVDB) activant le niveau 5 de dédoublonnage, plutôt que s'y concaténer ; en P4 ils pourront aussi y être ajoutés comme une source parmi d'autres, la rangée restant multi-sources.

#### Scenario: comportement
- **WHEN** la capability est utilisée (P4)
- **THEN** les IDs externes BetaSeries sont attachés aux items existants de toutes les sources (Jellyfin inclus), activant le niveau 5 de dédoublonnage ; la rangée reste multi-sources et ne décrit plus un ordre « Jellyfin puis BetaSeries »
