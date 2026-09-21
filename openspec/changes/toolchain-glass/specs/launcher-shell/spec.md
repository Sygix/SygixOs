# Delta launcher-shell

## MODIFIED Requirements

### Requirement: Thème
L'UI SHALL être sombre (v1) avec des surfaces Liquid Glass : matériau verre réfractant (réfraction des bords, reflet spéculaire, teinte claire), sur fond noir pur.

- Sombre uniquement en v1, noir pur, posters plein cadre, police type Inter
- Aucune ombre noire : le relief vient des halos clairs ou colorés

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent

#### Scenario: surfaces verre
- **WHEN** le dock, le bouton d'ouverture du héro ou le menu contextuel sont affichés
- **THEN** ils utilisent le matériau verre (arrière-plan flouté et réfracté, reflet spéculaire sur les bords) ; si l'appareil ne supporte pas l'effet, la surface reste translucide sans flou, sans crash
