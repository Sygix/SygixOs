# Design

## Context
Voir `proposal.md` pour la motivation. État de départ (`main`, plus `ui-tvos-polish` pour `HomeScreen.kt`) :
- `HomeViewModel.state` est un `StateFlow<HomeState>` partagé en `SharingStarted.Eagerly` avec la valeur initiale `HomeState.Loading` ; il passe à `HomeState.Ready` dès que le catalogue d'apps (mis en cache dans DataStore) et les autres flux combinés ont émis. `HomeScreen` affiche `AmbientGradient(animated = true)` pendant `Loading`.
- `HomeViewModel` est créé par `ViewModelProvider(this)` dans `MainActivity` : il survit aux recréations de l'activité, pas à la mort du processus. `MainActivity` est en `singleTask` : la touche Home sur un launcher vivant passe par `onNewIntent` / `onResume`, sans recréer l'accueil.
- `MainActivity.onCreate` demande `READ_TV_LISTINGS` si besoin, après `setContent`.
- Thème `Theme.SygixOs` (parent `Theme.Material.NoActionBar`) : `android:windowBackground` noir, aucun attribut `windowSplashScreen*`. Sur Android 12+, l'écran de lancement du système prend alors par défaut un fond dérivé du thème et l'icône du launcher (`ic_launcher` : icône adaptative, fond noir et hexagone bleu) : c'est le « double écran » à supprimer. Ce comportement par défaut sur la TV de test n'est pas encore observé : tâche 1.1.
- Compose : `MaterialTheme` a `background = Color.Black` ; le fond de l'app est donc le noir pur (`#000000`), seule couleur unie qui peut être partagée avec l'écran de lancement du système (qui n'accepte qu'une couleur unie). Le dégradé de la zone grille (`AmbientGradient` fixe) n'est pas reproductible par le système et n'est pas retenu comme fond.
- Aucune dépendance `androidx.core:core-splashscreen` : minSdk 34, l'API SplashScreen de la plateforme suffit (la bibliothèque n'est qu'un rétroportage).
- Les tests Compose tournent sous Robolectric ; le décodage natif d'un WebP animé (`ImageDecoder` / `AnimatedImageDrawable`) n'y est pas fiable.

## Goals / Non-Goals

**Goals:**
- Un seul fond noir continu de l'appui jusqu'à l'accueil, la mascotte n'apparaissant qu'une fois.
- Toute la logique de démarrage (durée minimale, accueil prêt, animations désactivées, une fois par processus) en domaine pur, testée en JUnit ; les composables ne font que rendre une phase.
- Aucun coût après le démarrage : l'animation est arrêtée et libérée à la fin du fondu.

**Non-Goals:**
- Accélérer le chargement de l'accueil : l'écran de démarrage masque l'attente, il ne la réduit pas.
- Produire l'asset : seul son contrat est fixé ici (D4).

## Decisions

### D1. Écran de lancement du système : fond noir, icône vide
Dans `Theme.SygixOs` :
- `android:windowSplashScreenBackground` = `@android:color/black` (même valeur que `windowBackground` et que le fond Compose) ;
- `android:windowSplashScreenAnimatedIcon` = un drawable vide (`res/drawable/splash_icon_empty.xml`, forme transparente) ;
- pas de `windowSplashScreenIconBackgroundColor` (le système ne dessine alors aucun disque derrière l'icône) ;
- pas de `windowSplashScreenBrandingImage`.

`MainActivity` installe `splashScreen.setOnExitAnimationListener { it.remove() }` : l'écran du système est retiré sans animation dès la première image de l'app. Comme son fond est identique, le retrait est invisible, avec ou sans fondu système ; le retrait immédiat rend le comportement déterministe.

Alternatives écartées :
- **Mascotte statique comme icône de l'écran système** (choix provisoire écarté, voir Questions ouvertes, Q2) : l'icône du système est découpée dans un disque (environ 2/3 de la zone d'icône, 240 dp avec fond d'icône, 288 dp sans), sa taille et sa position dépendent du système et non de notre mise en page, et seule une image fixe ou un `AnimatedVectorDrawable` y est accepté (pas le WebP) ; le moindre écart de taille ou de position donne un saut de la mascotte au passage à l'écran Compose.
- **Désactiver l'écran de lancement du système** : impossible depuis Android 12 pour une app ordinaire.
- **`androidx.core:core-splashscreen`** (`installSplashScreen`, `setKeepOnScreenCondition`) : garder l'écran du système jusqu'à l'accueil prêt rendrait l'animation impossible (le système n'affiche pas un WebP animé) ; et la plateforme suffit à minSdk 34.

### D2. Logique de démarrage dans le domaine
- `domain/StartupGate` (pur, sans Android) : entrées `homeReady: Boolean`, instant de la première image de l'écran de démarrage, horloge injectée, `animationsEnabled` ; sortie `StartupPhase` : `Splash`, `FadingOut`, `Done`. `Splash` tant que moins de 600 ms se sont écoulées depuis la première image ou que l'accueil n'est pas prêt ; `FadingOut` pendant la durée du fondu (`Done` directement si les animations sont désactivées) ; `Done` ensuite.
- « Accueil prêt » = `HomeState.Ready` tel qu'il existe aujourd'hui (catalogue chargé), choix provisoire à confirmer (Q3).
- **Une fois par processus** : un `StartupSession` porté par `SygixOsApp` (portée application, comme les autres singletons du conteneur) retient que l'écran de démarrage a été joué. Une activité recréée dans le même processus démarre directement en `Done`. Ce qui s'affiche dans ce cas si l'accueil est encore en chargement est la question Q4 (provisoire : fond uni noir, sans mascotte ni dégradé).
- `HomeViewModel` expose `startup: StateFlow<StartupPhase>`, séparé de `state` (même patron que `clock` dans `ui-tvos-polish`) ; le minuteur des 600 ms tourne dans `viewModelScope` avec un dispatcher injecté (testable par `runTest`). Le composable de l'écran de démarrage signale sa première image (`withFrameNanos`) par un événement `onSplashShown()` ; il ne décide de rien.
- Clés : tant que la phase est `Splash`, l'écran de démarrage consomme tous les événements clavier (`onPreviewKeyEvent` renvoie `true`) ; au passage à `FadingOut`, le héro demande le focus (`tryRequestFocus`) puis l'écran de démarrage ne consomme plus rien.

### D3. Rendu de l'animation
- `data/MascotAnimationSource` (interface) : `suspend fun load(): Result<Drawable>`. Implémentation : `ImageDecoder.createSource(resources, R.raw.splash_mascot)` puis `ImageDecoder.decodeDrawable` sur `Dispatchers.IO` ; le résultat est un `AnimatedImageDrawable` (`repeatCount = REPEAT_INFINITE`). Erreur de décodage → `Result.failure`, l'écran affiche le fond seul (scénario « animation illisible »). Les tests Compose injectent une source qui renvoie un drawable fixe ou une erreur.
- Affichage par `AndroidView` (un `ImageView` de taille `Dimens.SplashMascot`, centré) : c'est le moyen le plus direct de laisser `AnimatedImageDrawable` s'invalider lui-même, sans dépendance. `start()` seulement si les animations sont activées ; `stop()` et abandon de la référence à `Done`.
- Apparition de la mascotte : fondu d'entrée de `Motion.SPLASH_APPEAR_MS` (300 ms, `AppleEasing`) depuis le fond, qui couvre aussi le temps de décodage de la première image. Sortie : fondu de `Motion.SPLASH_FADE_MS` (400 ms, `AppleEasing`) de l'écran entier au-dessus de l'accueil. Durée minimale : `Motion.SPLASH_MIN_MS` (600). Les trois jetons rejoignent `Motion`, la taille rejoint `Dimens`.
- L'accueil (`LauncherHome`) est composé sous l'écran de démarrage dès `HomeState.Ready`, pour que sa première composition (coûteuse) ait lieu pendant que l'écran de démarrage est opaque et non pendant le fondu. La lecture du héro peut commencer quelques centaines de millisecondes avant d'être visible ; elle est muette, c'est sans effet visible.
- testTags : `startup-splash` (écran), `startup-splash-mascot` (mascotte, absente si l'animation est illisible). Chaîne FR : description d'accessibilité de la mascotte (« SygixOs démarre »).

Alternatives écartées :
- **Coil + `coil-gif`** (`ImageDecoderDecoder`) : nouvel artefact pour un seul fichier local.
- **Lottie** : demande une source vectorielle ; Sygix a choisi un rendu Blender pré-calculé.
- **Vidéo (ExoPlayer)** : coût de démarrage d'un lecteur au moment le plus chargé, pas de canal alpha.
- **Suite de PNG** : poids et mémoire bien supérieurs au WebP animé.

### D4. Contrat de l'asset et budget
Livrable du chantier logo : `app/src/main/res/raw/splash_mascot.webp` (dans `raw` pour qu'aucun outil de build ne le retouche).

| Contrainte | Valeur | Raison |
| --- | --- | --- |
| Format | WebP animé (chunks `VP8X` avec drapeau animation, `ANIM`, `ANMF`) | décodé nativement par `ImageDecoder` depuis Android 9 |
| Canevas | carré, côté = 2 × `Dimens.SplashMascot` en px, au plus 512 px ; provisoirement 360 × 360 px pour 180 dp (Q1) | la TV rend l'interface en 1920 × 1080 à densité 2 (960 × 540 dp) : 1 px d'asset = 1 px d'écran, ni flou ni travail de mise à l'échelle |
| Cadence | images de 16 ou 17 ms (alterner 17, 17, 16 pour une moyenne de 16,67 ms) | 60 images/s, décision de Sygix ; le format stocke des millisecondes entières |
| Boucle | répétitions = 0 ; durée totale entre 1 et 4 s, provisoirement 2 s (120 images, Q5) ; la scène Blender est cyclique et la dernière image exportée précède la première (l'image N, identique à l'image 0, n'est pas exportée) | raccord sans saut |
| Première image | pose de repos, yeux ouverts, au centre du flottement | image fixe quand les animations sont désactivées |
| Fond | canal alpha (rendu Blender « Film > Transparent »), sinon noir `#000000` exact | aucun cadre visible sur le fond noir |
| Poids | au plus 1 Mio ; cible environ 600 Kio | voir ci-dessous |

Budget de poids : l'APK publié pèse environ 3,5 Mo (asset `app-release.apk` de `v0.0.1-rc.4` : 3 501 746 octets) ; 1 Mio représente au plus 30 % de plus, pour un écran vu quelques secondes, et l'APK est aussi téléchargé par la TV lors d'une mise à jour. Une image WebP avec perte et alpha de 360 × 360 px d'un personnage en aplats pèse de l'ordre de 4 à 8 Kio ; 120 images donnent 0,5 à 1 Mio. Si le budget est dépassé : baisser la qualité (`-q`), activer `-mixed` / `-min_size` (images partielles : le clignement ne change qu'une petite zone), ou raccourcir la boucle dans la plage de 1 à 4 s, jamais baisser la cadence. Exemple d'assemblage avec l'outil `img2webp` de libwebp à partir de la suite PNG RGBA de Blender : `img2webp -loop 0 -lossy -q 80 -m 6 -d 17 f000.png f001.png -d 16 f002.png …`.

Coût à l'exécution : 360 × 360 px à 60 images/s, soit environ 7,8 Mpx/s décodés par libwebp sur le thread de décodage d'`AnimatedImageDrawable`, à la portée d'un SoC de TV ; mémoire de l'ordre de deux images ARGB (environ 1 Mo). Mesure sur la TV en tâche 7.2.

Contrôle automatique (`MascotAssetContractTest`, JUnit sous Robolectric pour lire la ressource `raw`) : parcours des chunks RIFF sans décodage d'image — `RIFF`/`WEBP`, `VP8X` (drapeau animation, canevas sur 24 bits + 1), `ANIM` (nombre de répétitions sur 16 bits = 0), chaque `ANMF` (durée sur 24 bits ∈ {16, 17}), somme des durées dans [1000, 4000] ms, taille du fichier ≤ 1 048 576 octets. Le fond (alpha ou noir exact) est contrôlé à l'œil sur la TV (tâche 7.1) : le drapeau alpha seul ne prouve rien sur les pixels.

### D5. Animations désactivées
`data/SystemMotionSource` (interface) : `animationsEnabled(): Boolean`, lu une fois au démarrage à froid dans `Settings.Global.ANIMATOR_DURATION_SCALE` (valeur 0 ⇒ désactivées ; c'est ce que règlent « Supprimer les animations » de l'accessibilité et l'échelle des options pour les développeurs). Compose applique déjà cette échelle à ses propres animations, mais `AnimatedImageDrawable` l'ignore : il faut donc ne pas appeler `start()`, et la phase passe directement de `Splash` à `Done` (aucun fondu, de façon déterministe et testable, sans dépendre de l'échelle globale). Robolectric permet de régler la valeur dans les tests (`Settings.Global.putFloat`).

## Risks / Trade-offs
- [La TV de test n'affiche pas d'écran de lancement système, ou un écran propre au fabricant] → D1 reste valable (fond de fenêtre noir) ; constat consigné en tâche 1.1 avant le code.
- [Le fondu système par défaut serait visible si une autre couleur s'y glissait] → retrait immédiat (`it.remove()`) et test du thème (scénario « écran de lancement du système » de `ui-testing`).
- [Décodage de la première image après la première image de l'écran] → fondu d'entrée de 300 ms qui part du fond ; la mascotte n'apparaît jamais d'un coup.
- [Décodage à 60 images/s trop coûteux sur la TV] → mesure `gfxinfo` (tâche 7.2) ; levier côté asset (canevas plus petit dans le contrat), jamais côté cadence.
- [Asset non livré au moment de l'implémentation] → tout le reste (logique, thème, tests avec source factice) est réalisable ; la tâche d'intégration de l'asset et son test de contrat bloquent la PR d'implémentation tant que le fichier final n'est pas là.
- [L'accueil composé sous l'écran de démarrage démarre la lecture du héro un peu tôt] → muette et invisible ; accepté pour éviter une saccade au début du fondu.
- [Robolectric ne décode pas le WebP animé] → source injectée dans les tests Compose ; décodage réel validé sur la TV.

## Migration Plan
Aucune donnée persistée. Retour arrière : revenir au commit précédent (l'état de chargement redevient le dégradé animé et le thème perd ses attributs `windowSplashScreen*`).

## Questions ouvertes
Choix non tranchés par Sygix. Pour chacun, l'option retenue provisoirement est la plus conservatrice ; elle est **à confirmer** et n'est pas écrite comme décision dans les specs.

- **Q1. Taille affichée de la mascotte.** Options : 120 dp (240 px, discret, proche du logo de démarrage d'Apple TV) ; 180 dp (360 px, un tiers de la hauteur) ; 240 dp (480 px, la moitié de la hauteur, asset plus lourd). Provisoire : 180 dp, à confirmer. Le contrat de l'asset (D4) suit cette valeur.
- **Q2. Contenu de l'écran de lancement du système.** Options : fond noir seul, icône vide (D1) ; mascotte fixe dans l'écran système, à faire coïncider avec l'écran de démarrage ; logo ou nom en image de marque en bas de l'écran système. Provisoire : fond noir seul, à confirmer.
- **Q3. Moment où l'accueil est « prêt ».** Options : catalogue chargé (`HomeState.Ready`, comme aujourd'hui) ; catalogue chargé et premier visuel du héro validé, avec un plafond (par exemple 2 s) ; catalogue chargé et première image de la vidéo nature si le héro n'a pas de contenu. Provisoire : catalogue chargé, à confirmer.
- **Q4. Chargement hors démarrage à froid** (activité recréée dans un processus vivant alors que l'accueil n'est pas encore prêt, cas rare). Options : fond noir uni, sans mascotte ; ancien dégradé animé ; mascotte fixe, sans animation ni durée minimale. Provisoire : fond noir uni, à confirmer.
- **Q5. Durée de la boucle de l'animation.** Options : 1,5 s ; 2 s ; 3 s (dans la plage 1–4 s du contrat). Provisoire : 2 s, à confirmer avec le chantier logo.
