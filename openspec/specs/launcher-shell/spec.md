# Capability : launcher-shell

## Purpose
Écran home du launcher : grille d'apps, rangée Up Next, fond contextuel.

## Requirements
### Requirement : Grille d'apps
Auto-détection de toutes les apps TV installées (category LEANBACK_LAUNCHER / LAUNCHER).

#### Scenario : affichage
- WHEN le home s'ouvre THEN toutes les apps TV installées apparaissent en grille (icônes arrondies superellipse, 6-7 par ligne selon densité), ordre et épinglage persistés (DataStore)

#### Scenario : menu contextuel
- WHEN long-press sur une app THEN menu : épingler, désépingler, ouvrir, infos

### Requirement : Focus tvOS
#### Scenario : focus
- WHEN une tuile prend le focus THEN zoom ~1.1x + ombre douce + transition 250-400ms, courbes d'easing Apple ; le fond de page passe aux recommandations de l'app focus (effet Top Shelf), sinon fond neutre

### Requirement : Rangée Up Next
#### Scenario : contenu
- WHEN des items Jellyfin en cours existent THEN rangée au-dessus de la grille : posters + barre de progression, fusion déterministe Jellyfin puis BetaSeries (v1.x), tri par date d'activité

### Requirement : Thème
- Sombre uniquement en v1, noir pur, posters plein cadre, surfaces Liquid Glass translucides floutées, police type Inter

### Requirement : Navigation
- DPAD natif uniquement ; Home de la télécommande retourne au launcher
