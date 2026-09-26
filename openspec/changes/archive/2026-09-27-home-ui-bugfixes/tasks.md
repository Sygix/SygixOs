# Tasks : home-ui-bugfixes

Le code existe déjà sur la branche `fix/home-ui-bugs` : ces tâches décrivent comment chaque exigence est vérifiée. Une tâche n'est cochée qu'avec un test qui échoue sans le comportement.

## 1. Engrenage des réglages (settings)

- [x] 1.1 Engrenage affiché seulement avec le héro, retiré en fondu en zone grille, de retour sur le héro ; vérifié par `SettingsEntryTest.gear disappears in the grid zone and comes back on the hero` à la taille TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), qui échoue sur `main` (engrenage présent en grille)
- [x] 1.2 Focus et ouverture inchangés : vérifié par `SettingsEntryTest` (`up focuses the gear, down returns to hero`, `ok on gear opens settings and back resumes hero`) à la taille TV
- [x] 1.3 Engrenage plein blanc opaque, sans fond, sans bordure ni verre : vérifié dans le code (aucune `GlassSurface` ni fond autour de l'engrenage dans `HomeScreen.kt`) ; le rendu n'est pas testable sans capture d'image (« Tests UI sans capture d'image » de `ui-testing`) et relève de la tâche 4.2

## 2. Placement de la grille et du Top Shelf (launcher-shell)

- [x] 2.1 Règle de placement pure, sans état Compose : vérifié par `GridScrollTest` (rangée visible immobile, rangée sous la pliure sur la marge basse, panneau sur une rangée haute aligné sur la marge haute, rangée profonde immobile à l'ouverture et à la fermeture, bloc trop grand sur la marge haute, défilement jamais négatif)
- [x] 2.2 Comportement dans la grille à la taille TV : vérifié par `HomeGridScrollTest` ; `shelf opening on a top row keeps the panel below the top margin`, `shelf opening and closing on a deep row keeps the focused tile in place` et `re-entering the grid brings the restored tile back within the margins` échouent sur `main`
- [x] 2.3 Aucun défilement sur Gauche/Droite : `HomeGridScrollTest.moving within a row leaves the grid where it is` est un test de non-régression, il passe aussi sur `main`. Le saut mesuré sur la TV n'a pas été reproduit en Robolectric ; vérifié par la mesure image par image sur la TV de test, puis validé par le propriétaire (tâche 4.2)

## 3. Focus d'une app disparue (launcher-shell)

- [x] 3.1 Choix de la tuile cible hors des composables : vérifié par `FocusFallbackTest` (app présente, disparue au milieu, disparue en fin, aucune app focusée, liste vide, zone inactive)
- [x] 3.2 Voisine à la même position dans la grille et le dock : vérifié par `CatalogUpdateFocusTest` à la taille TV (`hiding the focused grid app removes its tile and focuses the next one`, `hiding the last grid app focuses the previous one`, `unpinning the focused dock app removes its tile and focuses the next one`), qui échouent sur `main`
- [x] 3.3 Grille vidée : le héro reprend le focus et les touches répondent à nouveau (bas vers le dock, Retour) ; vérifié par `CatalogUpdateFocusTest.hiding the only grid app returns the focus to the hero and back still works` à la taille TV, qui échoue sans le garde-fou de la grille vide

## 4. Finition

- [x] 4.1 `./gradlew test` vert et `openspec validate --all --strict` vert
- [x] 4.2 Validation sur la TV réelle en `assembleRelease` : engrenage, placement du Top Shelf, aucun saut sur Gauche/Droite, focus après avoir caché une app ; validé par le propriétaire sur la TV de test. Les correctifs post-review (grille vidée, repli de focus partagé) sont couverts par les tests et seront revus sur `v0.0.1-rc.2`
- [x] 4.3 README : aucune mise à jour nécessaire, il ne décrit ni le matériau de l'engrenage ni le placement de la grille (vérifié par relecture) ; aucune nouvelle chaîne ni nouveau testTag
- [x] 4.4 `openspec archive home-ui-bugfixes` après le merge sur main et la validation sur la TV
