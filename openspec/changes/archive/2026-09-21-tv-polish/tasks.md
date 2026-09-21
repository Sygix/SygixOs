# Tasks : tv-polish

- [x] 1. Change proposal + delta spec (issus des retours de test)
- [x] 2. `VisualValidator` : validation + préchargement des visuels (taille ≥ 960 px), état `validated` dans le ViewModel ; Coil configuré (Application) : RGB565, cache mémoire, décodeurs limités
- [x] 3. Grille : panneau à emplacement fixe (posters validés, sinon bannière floutée), `animateScrollBy` + `animateItem` synchronisés, fond statique
- [x] 4. Focus tuile : zoom 1.12x, liseré blanc, ombre ; appui long OK → menu (touche Menu retirée), clic clavier géré
- [x] 5. Dock : largeur ajustée au nombre de tuiles
- [x] 6. Héro : rythme 12 s / fondu 1,4 s, bouton Ouvrir/Reprendre focusable, lecteur libéré hors héro, verre et fond animé inactifs si invisibles
- [x] 7. Tests unitaires (validation, dock width), build vert, test TV (fluidité, appui long, persistance), README + spec synchronisés
