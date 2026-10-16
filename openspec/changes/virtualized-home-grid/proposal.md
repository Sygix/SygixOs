# Proposal

## Why

La grille compose toutes ses rangées quel que soit le nombre d'apps : chaque tuile porte son focus, son fondu et son chargement de bannière, et la mémoire des bannières suit la taille du catalogue. Pour un utilisateur à 20-60 apps ce coût est acceptable ; au-delà, composition et mémoire croissent sans borne alors que seules quelques rangées sont visibles. Le propriétaire demande de borner ce coût sans toucher au rendu ni à la navigation.

## What Changes

- La grille reste une `Column` défilée par la page ; au-dessus d'un seuil de rangées, les rangées hors de la fenêtre utile (écran à la position courante et à la position cible du défilement, plus une prélecture) sont remplacées par un espace de leur hauteur. Sous le seuil, rien ne change.
- La fenêtre inclut toujours la rangée visée par le focus (ancre), de sorte qu'une rangée profonde est composée avant qu'on lui demande le focus : rafale de touches, retour dans la grille, déplacement d'une app, rangée Up Next après les apps.
- Les cibles de focus de la grille, du dock et de la rangée Up Next passent par le registre partagé de cibles composées du design system ; le registre propre à la grille disparaît.
- Top Shelf : la première affiche est préchauffée en mémoire pendant le délai d'ouverture avec la requête de l'affichage ; le panneau s'ouvre avec les affiches validées puis ne met sa liste à jour qu'une fois la vérification terminée, en gardant l'affiche courante ; une rangée ne porte le panneau animé que lorsqu'il est ouvert ou en train de se fermer.
- Une fixture de profilage (source set `perf`, processus séparé, absente du manifest release) compose l'accueil avec 20, 60, 200 ou 2 000 apps synthétiques pour mesurer montage, CPU et mémoire.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `launcher-shell` : « Préchargement et mémoire » (fenêtre de composition de la grille, prélecture, Top Shelf).
- `ui-testing` : « Couverture grille et shelf » (grande grille, cibles hors fenêtre, restauration profonde, déplacement, panneau sur rangée profonde, source du verre).

Dépend de `artwork-tile-decode`, `hero-visual-retry` et `hero-background-release`, qui modifient aussi « Préchargement et mémoire » (repris tels quels) : les archiver avant celui-ci. Dépend aussi de `settings-lazy-lists` (registre de cibles de focus).

## Impact

`HomeGrid`, `TileFocus`, `Dock`, `UpNextRow`, `ShelfPanel`, `GridScroll` (inverse de `rowTop`, calcul de la fenêtre), `HomeScreen` (lecture du défilement de page), fixture `perf`, tests. Aucune dépendance, permission, chaîne ou clé DataStore nouvelle ; aucun changement de géométrie, d'animation ni de sélecteurs.

## Non-goals

- Liste paresseuse, seconde autorité de défilement ou coordinateur de page : écartés.
- Préparateur d'artworks dédié : la source d'artworks partagée (cache borné, préchargement du dock et des premières apps au démarrage) suffit ; une tuile composée charge via le cache.
- Changer les durées, les délais du Top Shelf ou la validation des visuels.

## Open Questions

Aucune décision produit ouverte. Le seuil (12 rangées) et la prélecture (2 rangées) sont des budgets techniques fixés par la spec, à confirmer par les mesures de la fixture ; ils n'ont pas d'option utilisateur.
