# Design

## Context

Voir `proposal.md`. `LauncherHome` défile une `Column` (`pageScroll`) qui contient le héro puis la zone grille ; cette zone est la source du verre (`hazeSource`) et porte le dégradé d'ambiance. `HomeGrid` connaît déjà la géométrie exacte de chaque rangée (`GridScroll`, hauteurs mesurées des titres) pour placer le bloc focusé. `FocusTargets` (design system) lie une cible composée à sa clé et révèle puis focalise.

## Decisions

### D1 — Column fenêtrée, pas de liste paresseuse

La `Column` et `verticalScroll(pageScroll)` restent l'unique autorité de défilement. `GridScroll.rowAt(y)` (inverse de `rowTop`) donne la rangée à une position de page ; `GridScroll.window(scroll, anchor, openRow, viewport)` renvoie l'intervalle de rangées à composer : toutes si la grille a au plus 12 rangées (sections comprises), sinon les rangées couvrant l'écran à la position courante et à la position de l'ancre, plus 2 rangées de chaque côté. Une rangée hors fenêtre devient un `Spacer` de la hauteur de rangée (plus le titre mesuré pour la première). La fenêtre est un `derivedStateOf` : la grille ne recompose que lorsque l'intervalle change, pas à chaque image du défilement.

Alternative écartée : `LazyColumn` imbriquée ou en autorité de page (deux autorités de scroll, en-tête résident, coordinateur et conversion index/offset : +500 lignes pour le même rendu).

### D2 — l'ancre révèle

L'ancre (`GridScroll.Anchor`) désigne déjà la rangée focusée ; l'inclure dans la fenêtre compose la rangée visée dès que l'ancre change, avant la fin du défilement animé. Pour les demandes de focus programmées (entrée dans la grille, app déplacée), `HomeGrid` passe par `FocusTargets.reveal` : pose l'ancre sur la rangée de la cible, attend une image, focalise. La navigation D-pad ordinaire ne demande rien : la rangée suivante est toujours dans la prélecture.

### D3 — un seul registre de cibles

`TileFocus` garde la mémoire de l'app focusée et des replis, et délègue `bind`/`focus`/`reveal` à `FocusTargets`. Les tuiles (grille, dock), les cartes Up Next et la carte d'erreur se lient par leur clé ; le requester d'entrée de zone est passé à `bind` au lieu d'un second modificateur. Les requesters ne vivent que le temps de la composition de leur cible.

### D4 — Top Shelf

- Ouverture : pendant le délai d'ouverture, la première affiche validée est demandée à Coil avec exactement la requête de `ShelfPanel` (taille du panneau, `Scale.FILL`), de sorte que l'expansion affiche une image déjà décodée.
- Liste : le panneau reçoit la liste des affiches validées au moment de l'ouverture, puis une seule mise à jour quand la vérification de l'app est terminée ; `ShelfPanel` garde l'affiche courante par son URI si elle est encore présente et écarte lui-même les affiches illisibles.
- Animation : `AnimatedVisibility` n'est composée dans une rangée que si le panneau y est ouvert ou en cours de fermeture (`MutableTransitionState`) ; les autres rangées n'ont pas de transition.

### D5 — preuves

Tests JUnit sur `rowAt` et `window` ; tests Compose à taille TV : nombre de tuiles composées (total sous le seuil, borné au-dessus), rafale Bas/Haut avec inversion sans attendre la fin du défilement, restauration profonde après le héro et après le menu contextuel, Up Next après une grande grille, déplacement d'une app sur dix rangées avec validation et annulation, panneau sur une rangée profonde, préchauffage et conservation de l'affiche courante, zone grille toujours source du verre. Mesures CPU, mémoire et images sur émulateur avec la fixture `perf`.

## Risks / Trade-offs

- Rafale sans aucune image entre deux touches (impossible sur une télécommande, possible dans un test) : la fenêtre suit l'ancre avec une image de retard, la prélecture de 2 rangées absorbe une touche d'avance.
- Hauteur d'un espace vs rangée composée : toutes deux dérivent de la même largeur de tuile ; un écart d'arrondi d'un pixel ne concerne que les rangées hors écran.
- Panneau en cours de fermeture sur une rangée qui quitte la fenêtre : la fermeture est coupée ; cela demande plus de deux rangées traversées en 420 ms au-dessus du seuil.
