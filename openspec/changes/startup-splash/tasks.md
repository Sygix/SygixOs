# Tasks

## 1. Vérifications préalables (avant tout code)
- [ ] 1.1 Sur la TV de test, avec la dernière pré-release (`assembleRelease`) : arrêter l'app (`adb shell am force-stop fr.sygix.sygixos`), la relancer à froid depuis le système et filmer le lancement ; consigner dans `design.md` (section Context) si un écran de lancement système s'affiche, sa couleur de fond, la présence de l'icône et sa durée approximative (structure seulement, aucune donnée personnelle)
- [ ] 1.2 Asset `splash_mascot.webp` reçu du chantier logo et validé visuellement par Sygix (mascotte seule, sans TV ni nom, première image en pose de repos) : l'implémentation ne commence qu'après cette tâche

## 2. Asset et contrat
- [ ] 2.1 `app/src/main/res/raw/splash_mascot.webp` ajouté (D4)
- [ ] 2.2 Test `MascotAssetContractTest` : WebP animé, canevas 360 × 360 px, drapeau alpha, répétitions = 0, 120 images de 16 ou 17 ms, somme dans [1 997, 2 003] ms, poids ≤ 1 048 576 octets ; message d'échec nommant la contrainte ; vérifié en le faisant échouer avec un fichier hors contrat local non commité

## 3. Écran de lancement du système
- [ ] 3.1 `Theme.SygixOs` : `windowSplashScreenBackground` noir, `windowSplashScreenAnimatedIcon` vide (`splash_icon_empty`), aucun fond d'icône ; `MainActivity` retire l'écran système sans animation (D1)
- [ ] 3.2 Test Robolectric `SystemSplashThemeTest` sous `@GraphicsMode(GraphicsMode.Mode.NATIVE)` (rendu réel du drawable) : la couleur de `windowSplashScreenBackground` résolue dans le thème est égale au fond de l'écran de démarrage (`MaterialTheme` `background`) et à `windowBackground` ; l'icône résolue, dessinée dans un bitmap, n'a aucun pixel d'alpha non nul

## 4. Logique de démarrage
- [ ] 4.1 `domain/StartupGate` et `StartupPhase` (D2) ; test JUnit `StartupGateTest` : tout prêt à 100 ms → `Splash` jusqu'à 600 ms puis `FadingOut` puis `Done` ; catalogue à 200 ms et visuel à 1,5 s → `FadingOut` à 1,5 s ; visuel jamais prêt → `FadingOut` à 2 s ; catalogue jamais prêt → `FadingOut` à 5 s ; animations désactivées → `Splash` puis `Done` sans `FadingOut`
- [ ] 4.2 `StartupSession` dans `SygixOsApp`, `HomeViewModel.startup` (flux séparé de `state`, dispatcher injecté) ; test `HomeViewModelStartupTest` sous `runTest` : le premier ViewModel du processus démarre en `Splash`, une seconde instance dans le même processus démarre en `Done` ; une session non marquée (processus démarré en arrière-plan) donne `Splash`
- [ ] 4.3 `HeroStage` signale le premier visuel prêt (succès de l'image du programme, première image rendue d'une vidéo de programme ou nature ; jamais le dégradé) ; test Compose à la taille TV avec visuel factice
- [ ] 4.4 `data/MascotAnimationSource` (`Result<Drawable>`, décodage sur `Dispatchers.IO`) et `data/SystemMotionSource` (`ValueAnimator.areAnimatorsEnabled()`, D5), derrière des interfaces injectables

## 5. Interface
- [ ] 5.1 Composable `StartupSplash` (`startup-splash`, `startup-splash-mascot`) : fond noir, mascotte centrée de `Dimens.SplashMascot` (180 dp), fondu d'entrée, arrêt et libération de l'animation à `Done` ; jetons `Motion.SPLASH_MIN_MS`, `SPLASH_VISUAL_CAP_MS`, `SPLASH_CAP_MS`, `SPLASH_APPEAR_MS`, `SPLASH_FADE_MS` ; plus aucun `AmbientGradient(animated = true)` pour l'état de chargement dans `HomeScreen.kt` (grep vide)
- [ ] 5.2 `HomeScreen` : accueil composé sous `graphicsLayer { alpha = 0f }` et `focusProperties { canFocus = false }` jusqu'à `FadingOut`, focus du héro au début de `FadingOut` ; aucune interception de Retour ; fond noir uni pour `HomeState.Loading` en phase `Done`
- [ ] 5.3 Chaîne FR en ressources : description d'accessibilité de la mascotte ; aucune chaîne en dur dans le code
- [ ] 5.4 Tests Compose `StartupSplashTest` à la taille TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), horloge de test, sources factices : tous les scénarios de « Couverture de l'écran de démarrage » ; chaque test échoue si l'on retire le comportement testé (vérifié en local)
- [ ] 5.5 Les tests existants de l'accueil restent verts : leurs fixtures démarrent en phase `Done`

## 6. Documentation et vérification
- [ ] 6.1 README : écran de démarrage avec la mascotte dans les fonctionnalités
- [ ] 6.2 `./gradlew test` vert, `./gradlew assembleRelease` vert, `openspec validate --all --strict` vert
- [ ] 6.3 Aucun commentaire ajouté dans le code ni dans les tests (diff vérifié), en-tête de licence sur chaque nouveau fichier `.kt`

## 7. Validation sur la TV de test (pré-release signée par la CI, `assembleRelease`)
- [ ] 7.1 Lancement à froid filmé : un seul fond noir continu, aucune icône système, la mascotte apparaît une seule fois en fondu, sans cadre ni écart de teinte autour, boucle sans saut, fondu vers le héro qui a le focus ; validé par Sygix
- [ ] 7.2 Cadence de l'animation mesurée sur une pré-release signée par la CI, pendant au moins 2 s d'écran de démarrage (premier lancement après `am force-stop`, héro sans visuel immédiat), avec une trace Perfetto (`FrameTimeline`) ou `adb shell dumpsys SurfaceFlinger --latency` sur la couche de SygixOs : environ 60 images/s, moins de 5 % d'images manquées ; consigné dans la PR. Si une build locale signée avec la clé de debug est nécessaire (écran de démarrage prolongé artificiellement), elle impose de désinstaller la version de la CI, ce qui efface les réglages du launcher : les noter avant
- [ ] 7.3 Touche Home depuis une autre app et retour : aucun écran de démarrage ; arrêt du processus (`am force-stop`) puis relance : écran de démarrage ; Retour pendant l'écran de démarrage : SygixOs quitte le premier plan sans crash
- [ ] 7.4 « Supprimer les animations » (ou échelle des animations à 0) : mascotte fixe, aucun fondu ; réglage rétabli ensuite

## 8. Clôture
- [ ] 8.1 `openspec archive startup-splash` après merge et validation sur la TV ; `openspec validate --all --strict` vert après fusion des deltas
