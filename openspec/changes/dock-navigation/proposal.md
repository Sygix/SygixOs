# Change : dock-navigation

## Why
Le home doit s'ouvrir comme tvOS sur un écran focalisé : un dock d'apps épinglées en bas (Liquid Glass), et la grille complète se révèle quand on descend — première impression épurée, navigation directe vers les apps favorites.

## What Changes
- Home en deux zones : **dock** (écran initial, bas de l'écran, surface Liquid Glass) puis **grille** en dessous
- DPAD : au démarrage le focus est sur le dock ; descendre au-delà du dock fait défiler vers la grille plein écran ; remonter en haut de la grille ramène au dock
- Apps épinglées affichées dans le dock uniquement, **retirées de la grille** (pas de doublon)
- Animations de révélation fluides (translate/alpha, 250-400 ms, easing Apple)
- Up Next (P2) s'insérera au-dessus de la grille sans changer la mécanique dock

## Impact
- specs affectées : launcher-shell (delta ci-dessous)
- aucun changement data : `pinned` existe déjà (DataStore)
