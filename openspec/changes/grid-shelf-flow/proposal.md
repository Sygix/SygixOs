# Change : grid-shelf-flow

L'aperçu de la grille s'ouvre à la demande, après une pause du focus, en poussant la grille vers le bas ; plus aucune place réservée.

## Why
Test TV de `tv-polish` : l'emplacement d'aperçu toujours réservé produit des sauts et mange la vue ; en descendant dans la grille, des rangées sortent de l'écran et le focus n'est plus visible. Sygix décrit le comportement attendu : on arrive dans la grille et on la voit entièrement ; l'aperçu n'apparaît que si on s'attarde sur une tuile qui a du contenu, en poussant la grille ; il se ferme dès qu'on passe sur une app sans contenu.

## What Changes
- **Aucune place réservée** : à l'arrivée dans la grille (depuis le dock), la grille occupe tout l'écran
- **Ouverture temporisée** : après ~3 s de focus immobile sur une tuile dont l'app a des visuels validés, le panneau s'insère au-dessus de la rangée focusée et pousse cette rangée et les suivantes vers le bas ; la rangée précédente reste au-dessus du panneau
- **Enchaînement immédiat** : tant qu'un panneau est ouvert, passer sur une autre tuile avec du contenu remplace le visuel sans attendre
- **Fermeture** : passer sur une tuile sans visuel validé referme le panneau, la grille reprend toute la place
- **Focus toujours visible** : la grille défile juste ce qu'il faut pour garder la tuile focusée à l'écran, à l'ouverture comme à la fermeture du panneau (plus de rangée hors champ)
- Grille en colonne défilante simple (plus de liste paresseuse avec débordement) : les ~45 tuiles d'un launcher tiennent en composition, ce qui supprime le ré-ancrage du défilement à l'origine des sauts

## Impact
- specs affectées : launcher-shell (MODIFIED « Panneau Top Shelf au focus », « Contenu du panneau »)
- remplace le comportement d'emplacement fixe introduit par tv-polish
