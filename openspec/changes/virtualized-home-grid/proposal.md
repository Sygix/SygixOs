# Proposal

## Why

La grille compose actuellement toutes ses rangées et prépare les artworks de tout le catalogue. Son coût augmente donc avec le nombre d'apps même quand seules quelques tuiles sont visibles. Le propriétaire demande de poursuivre l'optimisation par la virtualisation, en conservant le rendu et la navigation TV existants.

## What Changes

- Borner la composition des rangées et la préparation des artworks à la fenêtre utile, avec une prélecture bornée.
- Conserver une seule autorité de défilement vertical, les paliers héro/dock/grille et les règles du Top Shelf ; rendre une cible hors fenêtre avant de lui demander le focus.
- Préserver déplacement, masquage, retour depuis une app, positions d'À suivre et restauration par identité.
- Adapter les tests qui supposent toutes les tuiles simultanément présentes ; mesurer séparément composition, décodage, mémoire et réactivité.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `launcher-shell` : composition bornée de la grille et préparation bornée des bannières ; les affiches du Top Shelf restent soumises à leur délai de focus existant.
- `ui-testing` : couverture d'un catalogue virtualisé et des cibles hors fenêtre.

## Impact

`settings` est consommée pour la position/visibilité d'À suivre sans modification de contrat ni delta.

`HomeScreen.kt`, `HomeGrid.kt`, `TileFocus.kt`, `HomeViewModel.kt`, `AppArtworkSource.kt`, calculs `HomePage`/`GridScroll`/`GridSections` et tests associés. Aucune dépendance, permission, chaîne métier ou clé DataStore nouvelle.

Base d'implémentation : corrections #49 (tests), #50 (visibilité), #52 (artworks), #53 (projections) et #55 (validation/refresh) intégrées. Les autres PR de l'audit sont compatibles et indépendantes. `hero-shuffle-image-provider` reste propriétaire de l'ordre du héro et de l'ouverture des images tierces ; `p7-default-launcher` reste propriétaire des effets de Home. `settings-destination` est compatible mais non requis.

## Non-goals

- Nouvelle présentation, nouveau palier ou modification des durées d'animation : hors de cette phase.
- Virtualisation des réglages ou séparation des destinations : familles distinctes.
- Réécriture du pipeline d'affiches, du mélange du héro ou de la fusion Up Next : autres changes.

## Open Questions

Aucune décision produit ouverte. Le budget technique de prélecture sera fixé et documenté par les mesures, sans option utilisateur ni seuil temporel arbitraire en CI.
