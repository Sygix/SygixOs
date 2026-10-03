# Design

## Context
Voir `proposal.md` pour la motivation. État de départ (`main`, plus `ui-tvos-polish` pour `HomeScreen.kt` et `HeroStage.kt`) :
- `HomeViewModel.state` est un `StateFlow<HomeState>` partagé en `SharingStarted.Eagerly` avec la valeur initiale `HomeState.Loading` ; il passe à `HomeState.Ready` dès que le catalogue d'apps (mis en cache dans DataStore) et les autres flux combinés ont émis. `HomeScreen` affiche `AmbientGradient(animated = true)` pendant `Loading`.
- `HeroStage` dessine le visuel du programme par `AsyncImage` (Coil), la vidéo dans un `TextureView` (donc soumis à l'opacité de Compose), et `AmbientGradient(animated = visible && !hasVisual)` tant qu'aucun visuel n'est prêt.
- `HomeViewModel` est créé par `ViewModelProvider(this)` dans `MainActivity` : il survit aux recréations de l'activité, pas à la mort du processus. `MainActivity` est en `singleTask` : la touche Home sur un launcher vivant passe par `onNewIntent` / `onResume`, sans recréer l'accueil.
- `MainActivity.onCreate` demande `READ_TV_LISTINGS` si besoin, après `setContent`.
- Thème `Theme.SygixOs` (parent `Theme.Material.NoActionBar`) : `android:windowBackground` noir, aucun attribut `windowSplashScreen*`. Sur Android 12+, l'écran de lancement du système prend alors par défaut un fond dérivé du thème et l'icône du launcher (`ic_launcher` : icône adaptative, fond noir et hexagone bleu) : c'est le « double écran » à supprimer. Comportement par défaut sur la TV de test à constater en tâche 1.1.
- Compose : `MaterialTheme` a `background = Color.Black` ; le fond de l'app est le noir pur (`#000000`), seule couleur unie partageable avec l'écran de lancement du système (qui n'accepte qu'une couleur unie).
- Aucune dépendance `androidx.core:core-splashscreen` : minSdk 34, l'API SplashScreen de la plateforme suffit.
- Les tests Compose tournent sous Robolectric ; le décodage natif d'un WebP animé (`ImageDecoder` / `AnimatedImageDrawable`) n'y est pas fiable.

## Goals / Non-Goals

**Goals:**
- Un seul fond noir continu de l'appui jusqu'à l'accueil, la mascotte n'apparaissant qu'une fois.
- Toute la logique de démarrage (durée minimale, accueil prêt, plafonds, animations désactivées, une fois par processus) en domaine pur, testée en JUnit ; les composables ne font que rendre une phase.
- Pas de travail GPU de l'accueil pendant l'animation, aucun coût après le démarrage.

**Non-Goals:**
- Accélérer le chargement de l'accueil : l'écran de démarrage masque l'attente, il ne la réduit pas.
- Produire l'asset : seul son contrat est fixé ici (D4).

## Decisions

### D1. Écran de lancement du système : fond noir, icône vide (décision de Sygix)
Dans `Theme.SygixOs` :
- `android:windowSplashScreenBackground` = `@android:color/black` (même valeur que `windowBackground` et que le fond Compose) ;
- `android:windowSplashScreenAnimatedIcon` = un drawable vide (`res/drawable/splash_icon_empty.xml`, forme transparente) ;
- pas de `windowSplashScreenIconBackgroundColor` (aucun disque derrière l'icône), pas de `windowSplashScreenBrandingImage`.

`MainActivity` installe `splashScreen.setOnExitAnimationListener { it.remove() }` : l'écran du système est retiré sans animation dès la première image de l'app ; son fond étant identique, le retrait est invisible et déterministe.

Alternatives écartées :
- **Mascotte comme icône de l'écran système**, fixe ou animée : l'icône accepte une image fixe, un `AnimatedVectorDrawable` ou un `AnimationDrawable` (suite d'images), mais elle est découpée dans un disque (environ 2/3 de la zone d'icône), sa taille et sa position dépendent du système, l'animation de l'icône est bornée en durée par le système, et un `AnimationDrawable` de 120 images de 360 px serait chargé en mémoire en entier ; tout écart avec l'écran Compose donnerait un saut de la mascotte.
- **Désactiver l'écran de lancement du système** : impossible depuis Android 12 pour une app ordinaire.
- **`androidx.core:core-splashscreen`** (`setKeepOnScreenCondition`) : garder l'écran du système jusqu'à l'accueil prêt empêcherait l'animation WebP ; et la plateforme suffit à minSdk 34.

### D2. Logique de démarrage dans le domaine
- `domain/StartupGate` (pur, sans Android, horloge injectée) : entrées `catalogReady`, `heroVisualReady`, instant de la première image de l'écran de démarrage, `animationsEnabled` ; sortie `StartupPhase` : `Splash`, `FadingOut`, `Done`. Règle : `t` = temps écoulé depuis la première image ; « prêt » = `catalogReady && (heroVisualReady || t ≥ 2 s)` ; `Splash` tant que `t < 600 ms` ou (non prêt et `t < 5 s`) ; puis `FadingOut` jusqu'à ce que l'interface signale la fin réelle du fondu (`fadeFinished`), puis `Done` (`Done` directement si les animations sont désactivées). La fin du fondu n'est pas un minuteur fixe : une échelle d'animation du système supérieure à 1 allonge le fondu sans le couper.
- `heroVisualReady` : remonté par `HeroStage` au premier visuel prêt (succès de l'`AsyncImage` du programme, ou `onRenderedFirstFrame` du lecteur pour une vidéo de programme ou nature) ; le dégradé de repli ne compte pas.
- **Démarrage à froid** : un `StartupSession` porté par `SygixOsApp` (portée application) retient si l'accueil a déjà été créé dans le processus. Le premier `HomeViewModel` créé dans le processus démarre en `Splash` et marque la session ; tout autre démarre en `Done`. Un processus démarré en arrière-plan sans accueil (diffusion système, `MY_PACKAGE_REPLACED` après une mise à jour) n'a pas marqué la session : la première ouverture est bien un démarrage à froid.
- Accueil recréé hors démarrage à froid (phase `Done`, `HomeState.Loading`) : `HomeScreen` affiche un fond noir uni (décision de Sygix) ; c'est aussi ce que révèle le fondu si le plafond de 5 s est atteint avant le chargement du catalogue.
- `HomeViewModel` expose `startup: StateFlow<StartupPhase>`, séparé de `state` (même patron que `clock` dans `ui-tvos-polish`) ; les minuteurs tournent dans `viewModelScope` avec un dispatcher injecté (testable par `runTest`). Le composable de l'écran de démarrage signale sa première image (`withFrameNanos`) par `onSplashShown()` et la fin de son fondu de sortie (fin de l'`Animatable`) par `onSplashFadeFinished()` ; il ne décide de rien.
- Focus et touches : rien n'est consommé. Tant que la phase est `Splash`, `LauncherHome` reçoit `interactive = false`, qui coupe le focus de chacune de ses cibles (héro et son bouton, engrenage, dock, grille) : aucun élément focusable, donc D-pad et OK sans effet. Un `focusProperties { canFocus = false }` sur une enveloppe ne suffirait pas : il n'atteint que les cibles de premier niveau, pas celles qui sont imbriquées. Au passage à `FadingOut`, le héro demande le focus (`tryRequestFocus`). Retour n'est pas intercepté : comportement système (l'activité racine du launcher passe en arrière-plan).

### D3. Rendu
- `data/MascotAnimationSource` (interface) : `suspend fun load(): Result<Drawable>`. Implémentation : `ImageDecoder.createSource(resources, R.raw.splash_mascot)` puis `ImageDecoder.decodeDrawable` sur `Dispatchers.IO` ; résultat `AnimatedImageDrawable` (`repeatCount = REPEAT_INFINITE`). Erreur → `Result.failure`, fond seul. Les tests Compose injectent une source qui renvoie un drawable fixe ou une erreur.
- Affichage par `AndroidView` (un `ImageView` de `Dimens.SplashMascot` = 180 dp, centré) : `AnimatedImageDrawable` s'invalide lui-même, sans dépendance. `start()` seulement si les animations sont activées ; `stop()` et abandon de la référence à `Done`.
- Fondus : apparition de la mascotte `Motion.SPLASH_APPEAR_MS` (300 ms, `AppleEasing`), qui couvre aussi le décodage de la première image ; sortie `Motion.SPLASH_FADE_MS` (400 ms, `AppleEasing`) ; durées et plafonds `Motion.SPLASH_MIN_MS` (600), `SPLASH_VISUAL_CAP_MS` (2 000), `SPLASH_CAP_MS` (5 000).
- **Accueil composé, non dessiné** : `LauncherHome` est composé dès `HomeState.Ready` (ses images et sa vidéo se chargent), mais sous un calque d'opacité nulle jusqu'au début du fondu : un calque d'opacité nulle n'est pas dessiné, l'accueil ne coûte donc rien au GPU pendant l'animation (y compris son flou Haze et la vidéo dans un `TextureView`). Un seul modificateur pose cette opacité et la propriété sémantique `StartupHomeAlpha` lue par les tests.
- **Fondu de sortie** : au passage à `FadingOut`, l'accueil passe d'un coup à l'opacité 1 et seul l'écran de démarrage (fond noir et mascotte) passe de 1 à 0 par-dessus. Le mélange est donc linéaire (accueil × (1 − a) + noir × a), avec un seul calque intermédiaire, celui de l'écran de démarrage ; l'accueil n'est jamais dessiné dans un tampon d'opacité inférieure à 1.
- **Mouvement du héro différé** : le Ken Burns du poster et le passage unique du dégradé de repli ne démarrent qu'à `FadingOut` (paramètre `motion` de `HeroStage`), pour que l'utilisateur voie le mouvement depuis le début ; le dégradé attend sur sa position de départ. La vidéo, elle, peut décoder et tourner muette pendant l'écran de démarrage (voir Risks).
- testTags : `startup-splash`, `startup-splash-mascot`. Chaîne FR : description d'accessibilité de la mascotte (« SygixOs démarre »).

Alternatives écartées : Coil + `coil-gif` (nouvel artefact pour un fichier local) ; Lottie (source vectorielle, alors que Sygix a choisi un rendu Blender) ; vidéo ExoPlayer (coût au démarrage, pas d'alpha) ; suite de PNG ou `AnimationDrawable` (poids et mémoire : toutes les images décodées).

### D4. Contrat de l'asset et budget (décisions de Sygix : 180 dp, 360 px, 2 s)
Livrable du chantier logo : `app/src/main/res/raw/splash_mascot.webp` (dans `raw` pour qu'aucun outil de build ne le retouche).

| Contrainte | Valeur | Raison |
| --- | --- | --- |
| Format | WebP animé (`VP8X` avec drapeaux animation et alpha, `ANIM`, `ANMF`) | décodé nativement par `ImageDecoder` |
| Canevas | 360 × 360 px | affiché à 180 dp ; la TV rend l'interface en 1920 × 1080 à densité 2 : 1 px d'asset = 1 px d'écran |
| Fond | canal alpha obligatoire (Blender « Film > Transparent ») ; images en perte pour la couleur, alpha sans perte (`-alpha_q 100`, valeur par défaut) | le WebP avec perte ne garantit pas un noir exact : seul l'alpha garantit l'absence de cadre |
| Cadence | 120 images de 16 ou 17 ms (alterner 17, 17, 16) : 2 000 ms | 60 images/s ; le format stocke des millisecondes entières |
| Boucle | répétitions = 0 ; scène Blender cyclique, l'image 120 (identique à l'image 0) n'est pas exportée | raccord sans saut |
| Première image | pose de repos, yeux ouverts, au centre du flottement | image fixe quand les animations sont désactivées |
| Poids | au plus 1 Mio ; cible environ 600 Kio | voir ci-dessous |

Budget de poids : l'APK publié pèse environ 3,5 Mo (asset `app-release.apk` de `v0.0.1-rc.4` : 3 501 746 octets) ; 1 Mio représente au plus 30 % de plus, et l'APK est aussi téléchargé par la TV lors d'une mise à jour. Une image WebP avec perte et alpha de 360 × 360 px d'un personnage en aplats pèse de l'ordre de 4 à 8 Kio ; 120 images donnent 0,5 à 1 Mio. Si le budget est dépassé : baisser la qualité couleur (`-q`), activer `-mixed` / `-min_size` (images partielles : le clignement ne change qu'une petite zone), jamais baisser la cadence ni retirer l'alpha. Exemple d'assemblage avec `img2webp` (libwebp) depuis la suite PNG RGBA de Blender : `img2webp -loop 0 -lossy -q 80 -m 6 -d 17 f000.png f001.png -d 16 f002.png …`.

Coût à l'exécution : 360 × 360 px à 60 images/s, environ 7,8 Mpx/s décodés par libwebp sur le thread de décodage d'`AnimatedImageDrawable` ; mémoire de l'ordre de deux images ARGB (environ 1 Mo). Mesure sur la TV en tâche 7.2.

Contrôle automatique (`MascotAssetContractTest`, Robolectric pour lire la ressource `raw`) : parcours des chunks RIFF sans décodage — `RIFF`/`WEBP`, `VP8X` (drapeaux animation et alpha, canevas sur 24 bits + 1 = 360), `ANIM` (répétitions sur 16 bits = 0), 120 `ANMF` (durée sur 24 bits ∈ {16, 17}), somme dans [1 997, 2 003] ms, taille ≤ 1 048 576 octets.

### D5. Animations désactivées
`data/SystemMotionSource` (interface) : `animationsEnabled(): Boolean` = `ValueAnimator.areAnimatorsEnabled()`, lu une fois au démarrage à froid (faux quand l'échelle de durée des animations vaut 0, ce que règlent « Supprimer les animations » et les options pour les développeurs). `AnimatedImageDrawable` ignore cette échelle : on n'appelle donc pas `start()`, et la phase passe directement de `Splash` à `Done`, de façon déterministe. Les tests injectent une source factice.

## Risks / Trade-offs
- [La TV n'affiche pas d'écran de lancement système, ou un écran propre au fabricant] → D1 reste valable (fond de fenêtre noir) ; constat en tâche 1.1.
- [Fondu système par défaut visible si une autre couleur s'y glissait] → retrait immédiat et test du thème (tâche 3.2).
- [Décodage de la première image après la première image de l'écran] → fondu d'entrée de 300 ms depuis le fond.
- [Décodage à 60 images/s trop coûteux sur la TV] → mesure (tâche 7.2) ; levier côté asset (qualité, images partielles), jamais côté cadence.
- [Asset livré hors contrat] → l'implémentation attend l'asset final (`proposal.md`) ; le test de contrat le refuse avant tout merge.
- [Accueil composé sous l'écran de démarrage] → composition et chargements anticipés, mais aucun dessin (opacité nulle) ; la vidéo d'un héro peut commencer muette avant d'être visible.
- [Visuel du héro jamais prêt] → plafond de 2 s, puis plafond global de 5 s même sans catalogue.
- [Robolectric ne décode pas le WebP animé] → source injectée dans les tests Compose ; décodage réel validé sur la TV.
- [Activité recréée pendant l'écran de démarrage] → le WebP est décodé de nouveau (la source n'est pas gardée par le ViewModel, pour ne pas retenir un drawable lié à une vue) ; accepté : cas rare, coût d'un décodage de la première image.
- [Couleur de fond du chunk `ANIM`] → l'asset livré déclare un fond blanc opaque (`0xFFFFFFFF`) ; le format en fait une simple indication, et la relecture a vérifié dans Skia et libwebp, utilisés par `ImageDecoder`, que cette couleur n'est jamais utilisée pour dessiner : aucune action.

## Migration Plan
Aucune donnée persistée. Retour arrière : revenir au commit précédent (l'état de chargement redevient le dégradé animé et le thème perd ses attributs `windowSplashScreen*`).
