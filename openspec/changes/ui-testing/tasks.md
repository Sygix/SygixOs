# Tasks : ui-testing

- [ ] 1. Refactor HomeGrid : injection du fournisseur de posters du shelf (paramètre avec valeur par défaut, aucun appelant modifié)
- [ ] 2. testTags stables sur les zones : héro, dock, grille, panneau shelf, tuiles d'app
- [ ] 3. DockTest : rendu des tuiles épinglées, focus initial, message d'état vide
- [ ] 4. HomeGridTest : rendu de la grille (exclusion du dock, ordre alphabétique), insertion du panneau shelf au focus d'une tuile avec contenu, absence de panneau sans contenu
- [ ] 5. HeroTest : rendu du carrousel (poster, titre, progression), état fallback sans contenu
- [ ] 6. NavigationTest : D-pad héro → dock → grille (bas) et retour (haut), le dock/grille prennent le focus à chaque palier
- [ ] 7. Épinglage : appui long sur une tuile de la grille → onTogglePin appelé avec le bon package
- [ ] 8. Build + suite de tests verts (assembleDebug test), commits signés, PR vers main
