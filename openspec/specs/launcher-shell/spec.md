# Capability : launcher-shell

## Purpose
Écran home du launcher : grille d'apps, rangée Up Next, fond contextuel.

## Requirements
### Requirement: Grille d'apps
Le launcher SHALL auto-détecter toutes les apps TV installées et les afficher en grille.

Auto-détection de toutes les apps TV installées (category LEANBACK_LAUNCHER / LAUNCHER).

#### Scenario: affichage
- **WHEN** le home s'ouvre THEN toutes les apps TV installées apparaissent en grille (icônes arrondies superellipse, 6-7 par ligne selon densité), ordre et épinglage persistés (DataStore)

#### Scenario: menu contextuel
- **WHEN** long-press sur une app THEN menu : épingler, désépingler, ouvrir, infos

### Requirement: Focus tvOS
Le launcher SHALL animer le focus des tuiles à la tvOS (zoom, ombre, easing).

#### Scenario: focus
- **WHEN** une tuile prend le focus THEN zoom ~1.1x + ombre douce + transition 250-400ms, courbes d'easing Apple ; le fond de page passe aux recommandations de l'app focus (effet Top Shelf), sinon fond neutre

### Requirement: Rangée Up Next
Le home SHALL afficher une rangée Up Next fusionnant Jellyfin puis BetaSeries.

#### Scenario: contenu
- **WHEN** des items Jellyfin en cours existent THEN rangée au-dessus de la grille : posters + barre de progression, fusion déterministe Jellyfin puis BetaSeries (v1.x), tri par date d'activité

### Requirement: Thème
L'UI SHALL être sombre (v1) avec surfaces Liquid Glass translucides.

- Sombre uniquement en v1, noir pur, posters plein cadre, surfaces Liquid Glass translucides floutées, police type Inter

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent
### Requirement: Navigation
Le launcher SHALL être navigable au DPAD uniquement.

- DPAD natif uniquement ; Home de la télécommande retourne au launcher

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent