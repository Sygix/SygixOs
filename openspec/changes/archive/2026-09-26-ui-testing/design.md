# Design : ui-testing

## Contexte
Les tests UI Robolectric + Compose tournent sur la JVM (rapides, pas d'émulateur) via compose-ui-test-junit4 déjà en testImplementation. Roborazzi est exclu : la couverture visée est comportementale (sémantique, focus, navigation), pas visuelle.

## Décisions
- **Outil** : createComposeRule() + assertions sémantiques (onNodeWithTag/onNodeWithText, assertIsFocused, assertCountEquals, assertDoesNotExist). Pas de capture d'image.
- **Périmètre v1** : écran home complet — héro, dock, grille, panneau shelf, menu contextuel, navigation D-pad 3 zones. Settings et screensaver plus tard.
- **Aucun refactor d'injection requis** : LauncherHome et HomeGrid reçoivent déjà catalogue et HeroState (programmes + visuels validés/contrôlés) en paramètres — les tests passent des données déterministes directement. Seule modification de production : les testTags.
- **Artwork des tuiles** : AppTile retombe sur AppArtworkSource(packageManager) quand LocalAppArtwork est absent ; les tests fournissent un AppArtworkSource de test (cache vide, load → null) via LocalAppArtwork pour un rendu déterministe (initiale du libellé).
- **testTags** : chaînes stables (« zone-hero », « zone-dock », « zone-grid », « shelf-panel », « app-menu », préfixe « app-tile-<package> ») ajoutées via Modifier.testTag en code de production. Justification : les sélecteurs par texte/position cassent au premier changement de copy ; le tag est le contrat test↔UI.
- **Horloge de test** : mainClock.autoAdvance = false. Le panneau shelf s'ouvre après delay(Motion.SHELF_OPEN_DELAY_MS) et se déploie en SHELF_EXPAND_MS, les zones changent d'alpha en LAYER_FADE_MS, le focus de zone passe par withFrameNanos — avancer l'horloge explicitement (advanceTimeBy + advanceTimeByFrame) avant d'assertir présence/focus. Les assertions ne dépendent pas des valeurs d'alpha intermédiaires.
- **Appui long** : tvClickable écoute les key events, pas le touch — un appui long se simule par keyDown(OK) suivi de advanceTimeBy(Motion.LONG_PRESS_MS) (job de delay) ou par l'événement natif isLongPress ; keyDown + keyUp seuls déclenchent le clic simple.
- **Navigation D-pad simulée** : key events via performKeyInput sur onRoot(), à travers LauncherHome qui intercepte via onPreviewKeyEvent — teste la machine à états des zones, pas seulement le rendu.
- **Animations** : non testées au-delà de la présence (alpha héro/grille, glissement du panneau, ken-burns). Les tests d'animation Robolectric (captureValues) sont un change ultérieur.
- **HomeViewModel/HomeState** : non testés en UI dans ce change. Test du ViewModel avec fake repository : change ultérieur si besoin.

## Risques / Trade-offs
- Robolectric + Compose for TV : les composables TvMaterial n'ont pas de sémantique particulière en JVM ; cas OK. GraphicsMode NATIVE requis pour certains rendus — à garder uniquement si un test en dépend, sinon l'omettre.
- Key events : performKeyInput simule le DPAD physique ; si l'interception onPreviewKeyEvent ne les voit pas en Robolectric, fallback approuvé = extraire la machine à états des zones (Zone + transitions) en classe testable, sans changer le comportement.
- Pas de visual regression : une régression visuelle (espacement, couleur) ne sera pas attrapée par ces tests. Accepté — le visual regression (Roborazzi compare ou Paparazzi) est un change séparé si demandé.

## Migration
- Aucune migration de données ; les seules modifications de production sont additives (testTags).
- Les tests Robolectric UI s'exécutent dans ./gradlew test existant — pas de nouveau task CI.
