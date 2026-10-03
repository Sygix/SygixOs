# Tasks

## 1. Vérifications préalables (avant tout code)
- [ ] 1.1 Sur la TV de test, avec la dernière pré-release (`assembleRelease`) : arrêter l'app (`adb shell am force-stop fr.sygix.sygixos`), la relancer à froid depuis le système et filmer le lancement ; consigner dans `design.md` (section Context) si un écran de lancement système s'affiche, sa couleur de fond, la présence de l'icône et sa durée approximative (structure seulement, aucune donnée personnelle)
- [ ] 1.2 Réponses de Sygix aux questions ouvertes Q1 à Q5 de `design.md` reportées dans les documents du change (specs, design, tasks) ; plus aucune mention « à confirmer » restante
- [ ] 1.3 Asset `splash_mascot.webp` reçu du chantier logo et validé visuellement par Sygix (mascotte seule, sans TV ni nom, première image en pose de repos)

## 2. Asset et contrat
- [ ] 2.1 `app/src/main/res/raw/splash_mascot.webp` ajouté (D4)
- [ ] 2.2 Test JUnit `MascotAssetContractTest` : format WebP animé, canevas carré égal à 2 × `Dimens.SplashMascot` et ≤ 512 px, répétitions = 0, chaque durée ∈ {16, 17} ms, boucle entre 1 et 4 s, poids ≤ 1 048 576 octets ; message d'échec nommant la contrainte ; vérifié en le faisant échouer avec un fichier hors contrat local non commité

## 3. Écran de lancement du système
- [ ] 3.1 `Theme.SygixOs` : `windowSplashScreenBackground` noir, `windowSplashScreenAnimatedIcon` vide (`splash_icon_empty`), aucun fond d'icône ; `MainActivity` retire l'écran système sans animation (D1)
- [ ] 3.2 Test Robolectric `SystemSplashThemeTest` : la couleur de `windowSplashScreenBackground` résolue dans le thème est égale au fond de l'écran de démarrage (`MaterialTheme` `background`) et à `windowBackground` ; l'icône résolue ne dessine aucun pixel opaque

## 4. Logique de démarrage
- [ ] 4.1 `domain/StartupGate` et `StartupPhase` (D2) ; test JUnit `StartupGateTest` : prêt à 100 ms → `Splash` jusqu'à 600 ms puis `FadingOut` puis `Done` ; prêt à 2 s → `FadingOut` dès 2 s ; animations désactivées → `Splash` puis `Done` sans `FadingOut` ; session déjà jouée → `Done` immédiat
- [ ] 4.2 `StartupSession` dans `SygixOsApp`, `HomeViewModel.startup` (flux séparé de `state`, dispatcher injecté) ; test `HomeViewModelStartupTest` sous `runTest` : une seconde instance du ViewModel dans le même processus démarre en `Done`
- [ ] 4.3 `data/MascotAnimationSource` (`Result<Drawable>`, décodage sur `Dispatchers.IO`) et `data/SystemMotionSource` (D3, D5) ; test Robolectric : `ANIMATOR_DURATION_SCALE` à 0 → animations désactivées, à 1 ou absent → activées

## 5. Interface
- [ ] 5.1 Composable `StartupSplash` (`startup-splash`, `startup-splash-mascot`) : fond noir, mascotte centrée de taille `Dimens.SplashMascot`, fondu d'entrée, consommation des touches en `Splash`, arrêt et libération de l'animation à `Done` ; jetons `Motion.SPLASH_MIN_MS`, `SPLASH_APPEAR_MS`, `SPLASH_FADE_MS` et `Dimens.SplashMascot` ; plus aucun `AmbientGradient(animated = true)` pour l'état de chargement dans `HomeScreen.kt` (grep vide)
- [ ] 5.2 `HomeScreen` : accueil composé sous l'écran de démarrage dès qu'il est prêt, focus du héro au début de `FadingOut` ; cas hors démarrage à froid selon la réponse à Q4
- [ ] 5.3 Chaîne FR en ressources : description d'accessibilité de la mascotte ; aucune chaîne en dur dans le code
- [ ] 5.4 Tests Compose `StartupSplashTest` à la taille TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), horloge de test, source d'animation factice : scénarios « durée minimale », « attente de l'accueil », « focus au début du fondu », « touches ignorées », « retour au premier plan », « animations désactivées », « animation illisible » de « Couverture de l'écran de démarrage » ; chaque test échoue si l'on retire le comportement testé (vérifié en local)
- [ ] 5.5 Les tests existants de l'accueil restent verts : leurs fixtures démarrent en phase `Done` sans attendre 600 ms

## 6. Documentation et vérification
- [ ] 6.1 README : écran de démarrage avec la mascotte dans les fonctionnalités
- [ ] 6.2 `./gradlew test` vert, `./gradlew assembleRelease` vert, `openspec validate --all --strict` vert
- [ ] 6.3 Aucun commentaire ajouté dans le code ni dans les tests (diff vérifié), en-tête de licence sur chaque nouveau fichier `.kt`

## 7. Validation sur la TV de test (pré-release, `assembleRelease`)
- [ ] 7.1 Lancement à froid filmé : un seul fond noir continu, aucune icône système, la mascotte apparaît une seule fois en fondu, sans cadre ni écart de teinte autour, boucle sans saut, fondu vers le héro qui a le focus ; validé par Sygix
- [ ] 7.2 `adb shell dumpsys gfxinfo fr.sygix.sygixos` pendant un écran de démarrage prolongé (accueil retardé par une build de test locale non commitée) : environ 60 images/s et moins de 5 % d'images en retard ; consigné dans la PR
- [ ] 7.3 Touche Home depuis une autre app et retour : aucun écran de démarrage ; arrêt du processus (`am force-stop`) puis relance : écran de démarrage
- [ ] 7.4 « Supprimer les animations » (ou échelle des animations à 0) : mascotte fixe, aucun fondu ; réglage rétabli ensuite

## 8. Clôture
- [ ] 8.1 `openspec archive startup-splash` après merge et validation sur la TV ; `openspec validate --all --strict` vert après fusion des deltas
