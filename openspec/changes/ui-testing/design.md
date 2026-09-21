# Design : ui-testing

## Contexte
Les tests UI Robolectric + Compose tournent sur la JVM (rapides, pas d'émulateur) via compose-ui-test-junit4 déjà en testImplementation. Roborazzi est exclu : la couverture visée est comportementale (sémantique, focus, navigation), pas visuelle.

## Décisions
- **Outil** : createComposeRule() + assertions sémantiques (onNodeWithTag/onNodeWithText, assertIsFocused, assertCountEquals, assertDoesNotExist). Pas de capture d'image.
- **Périmètre v1** : écran home complet — héro, dock, grille, panneau shelf, navigation D-pad 3 zones. Settings et screensaver plus tard.
- **Navigation D-pad simulée** : key events via performKeyInput (ou pressKey sur onRoot) à travers LauncherHome, qui intercepte via onPreviewKeyEvent — teste la machine à états des zones, pas seulement le rendu.
- **Injection** : HomeGrid gagne un paramètre `shelfPosters: (String) -> List<String>` (ou interface équivalente) avec valeur par défaut branchée sur TvProviderHeroSource — les tests passent une lambda déterministe. Aucun appelant modifié (Kotlin default args).
- **testTags** : chaînes stables ("zone-hero", "zone-dock", "zone-grid", "shelf-panel", préfixe "app-tile-<package>") ajoutées via Modifier.testTag en code de production. Justification : les sélecteurs par texte/position cassent au premier changement de copy ; le tag est le contrat test↔UI.
- **Animations** : non testées en v1 (alpha héro/grille, glissement du panneau). Les tests d'animation Robolectric (mainClock autoAdvance, captureValues) sont un change ultérieur. Les assertions de zone ne dépendent pas de l'alpha.
- **HomeViewModel/HomeState** : non testés en UI dans ce change (les composables sous test — LauncherHome, HomeGrid, Dock, HeroCarousel — reçoivent catalog/heroItems en paramètres). Test du ViewModel avec fake repository : change ultérieur si besoin.

## Risques / Trade-offs
- Robolectric + Compose for TV : les composables TvMaterial n'ont pas de sémantique particulière en JVM ; cas OK. GraphicsMode NATIVE requis pour certains rendus — à garder uniquement si un test en dépend, sinon l'omettre.
- Key events : performKeyInput simule le DPAD physique ; si l'interception onPreviewKeyEvent ne les voit pas en Robolectric, fallback = tester la machine à états des zones par refactor (extraire Zone en classe testable). Décision au moment du test, pas dans la spec.
- Pas de visual regression : une régression visuelle (espacement, couleur) ne sera pas attrapée par ces tests. Accepté — le visual regression (Roborazzi compare ou Paparazzi) est un change séparé si demandé.

## Migration
- Aucune migration de données ; refactor HomeGrid purement additif (paramètre avec défaut).
- Les tests Robolectric UI s'exécutent dans ./gradlew test existant — pas de nouveau task CI.
