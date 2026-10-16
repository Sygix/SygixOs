# Proposal

## Why

Les bannières d'apps sont décodées jusqu'à 480 × 270 quelle que soit la taille des tuiles. Sur une TV 1080p, une tuile de la grille au zoom de focus mesure 332 × 187 px : le plafond actuel alloue 518 400 octets par bannière là où 248 336 suffisent, soit deux fois plus de mémoire pour le cache des visuels et des bitmaps réduits par le GPU à l'affichage.

## What Changes

- La bannière d'une app est décodée à la taille de la tuile de la grille au zoom de focus, en pixels de l'appareil (largeur de tuile dérivée des jetons de la grille et de la densité, hauteur 16:9), plafonnée à 480 × 270 sur une dalle plus dense.
- Le cache des visuels d'apps réserve sa marge de préchargement sur cette taille réelle.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : « Préchargement et mémoire » (scénario « taille de décodage »).
- `ui-testing` : « Couverture du rendu du héro et du verre » (scénario « tailles de décodage »).

## Impact

Calcul des tailles de décodage dans le domaine, taille de tuile dans le design system, source des visuels d'apps, tests JUnit. Aucune dépendance ajoutée, aucun changement de rendu.

## Non-goals

- Changer la taille de décodage du héro ou des affiches du Top Shelf.
- Décoder les vignettes du menu contextuel et des réglages à leur propre taille : elles réutilisent le visuel de la tuile.
