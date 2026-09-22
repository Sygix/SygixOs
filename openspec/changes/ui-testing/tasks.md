# Tasks : ui-testing

- [ ] 1. testTags stables en code de production : zone-hero, zone-dock, zone-grid, shelf-panel, app-menu, app-tile-<package>
- [ ] 2. Fixtures : factories déterministes (TvApp, HeroItem, Catalog, HeroState) + AppArtworkSource de test fourni via LocalAppArtwork
- [ ] 3. DockTest : rendu des tuiles épinglées, focus initial, message d'état vide
- [ ] 4. HomeGridTest : rendu de la grille (exclusion du dock, une fois par app), grille vide, insertion du panneau shelf au focus d'une tuile avec contenu validé, absence de panneau sans contenu
- [ ] 5. HeroTest : rendu du carrousel (titre, progression), état fallback sans contenu
- [ ] 6. NavigationTest : D-pad bas héro → dock → grille, haut grille → dock → héro, dock vide → héro ↔ grille direct, Retour → héro
- [ ] 7. PinFlowTest : appui long (OK maintenu + LONG_PRESS_MS) → menu ouvert (app-menu) ; « Épingler au dock » → onTogglePin(package) + fermeture ; Retour → fermeture sans épinglage
- [ ] 8. Build + suite de tests verts (assembleDebug test), commits signés, push feat/ui-testing ; PR vers main à la revue
