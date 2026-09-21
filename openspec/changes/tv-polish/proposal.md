# Change : tv-polish

Retours de test TV (Sygix) après hero-fullscreen : fluidité de la grille, panneau Top Shelf stable, focus visible, menu sur appui long, dock ajusté, héro plus posé avec bouton d'ouverture.

## Why
Sur la TCL : navigation dans la grille saccadée, panneau Top Shelf qui apparaît sans animation et fait sauter la rangée, flashs quand un aperçu se charge puis s'annule, tuile focusée peu visible, touche Menu introuvable sur la télécommande, dock trop large quand peu d'apps, héro qui enchaîne trop vite et sans repère de focus.

## What Changes
- **Fluidité** : visuels du héro et du Top Shelf validés et préchargés au chargement (taille vérifiée une fois, cache disque pour le Top Shelf, mémoire pour les premiers héros) ; Coil en RGB565 avec décodeurs limités ; effets verre et animations de fond inactifs quand la surface est invisible ; lecteur vidéo libéré quand le héro est masqué ; fond de grille statique
- **Top Shelf** : emplacement toujours réservé au-dessus de la rangée active en zone grille ; affiches validées de l'app affichées en fondu une fois chargées, rien sinon (pas de logo de repli) ; changement d'app temporisé ; panneau fixe par-dessus la liste, la place du panneau étant une marge animée de la rangée active (aucun item réordonné, plus de saut)
- **Focus** : zoom 1.07x, liseré verre fin, halo radial diffus de la couleur dominante de la bannière (idée Sygix, style Google Play) ; les surfaces verre portent un halo clair léger, plus aucune ombre noire ; seuil qualité des visuels relevé à 1080 px de large (demande Sygix)
- **Menu contextuel** : appui long sur OK uniquement (touche Menu retirée), clic géré au clavier par le launcher
- **Dock** : largeur ajustée au nombre de tuiles, centré ; tuiles taille grille jusqu'à 5, réduites au-delà
- **Héro** : 12 s par programme, fondu 1,4 s ; bouton « Ouvrir » (« Reprendre » si progression) sous le titre, seul élément focusable du héro quand le programme est ouvrable, état focus net

## Impact
- specs affectées : launcher-shell (MODIFIED « Focus tvOS », « Grille d'apps », « Panneau Top Shelf au focus », « Contenu du panneau », « Diaporama héro », « Dock d'apps épinglées » ; ADDED « Préchargement et mémoire »)
- nouvelle classe Application (configuration Coil)
