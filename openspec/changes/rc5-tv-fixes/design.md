# Design

## Context
Mesures de la rc.5 et de la variante `perf` sur la TV de référence (interface en 1920 × 1080, GPU faible) :
- héro au repos avec Ken Burns : 22 tâches de rendu hors écran par image (flou du dock et de la capsule), 75 remplissages, 24 ms de dessin ; sans verre : encore 17 ms et 40 images par seconde, le GPU étant saturé par cinq passes plein écran ;
- fondu entre programmes : deux calques plein écran hors écran (`Crossfade`) et deux pour les textes ;
- le décodage 3840 × 2160 vient de `PackageItemInfo.loadBanner` (bannière d'une app tierce), pas de Coil ;
- `ValueAnimator.areAnimatorsEnabled()` vaut `false` dans l'app (échelle d'animation de l'app forcée à 0 par le constructeur) alors que `Settings.Global.ANIMATOR_DURATION_SCALE` n'est pas réglé : l'écran de démarrage se croyait sans animation ;
- démarrage à froid : la première composition de l'accueil bloque le fil principal 260 à 730 ms ; `MSG_WINDOW_FOCUS_CHANGED` n'est traité qu'après.

## Decisions

### D1. Visuel du héro décodé une fois, voiles compris
Une transformation Coil produit, à partir du visuel, une image opaque à la taille de la fenêtre plafonnée à 1920 × 1080 (`ImageBounds.screen`), recadrée comme `ContentScale.Crop`, avec les voiles bas, gauche et haut droit dessinés une fois, puis copiée en bitmap matériel hors du fil principal. Le héro la dessine en une passe, sous une transformation d'échelle (Ken Burns). Les voiles restent dessinés en direct seulement sous une vidéo ou le dégradé du repli, sous le visuel : un visuel en fondu au-dessus reste correctement voilé. Le dégradé et les voiles ne sont plus dessinés quand un visuel opaque couvre le héro ; le fond noir de l'accueil et le fond de fenêtre sont retirés (chaque zone a déjà un fond opaque).

Alternative écartée : voiles en dégradés dessinés à chaque image (1,25 passe plein écran de plus).

### D2. Fondus sans calque hors écran
Le nouveau visuel apparaît par-dessus l'ancien (alpha appliqué au dessin) ; l'ancien est retiré à la fin, sans assombrissement à mi-fondu. Les textes du programme passent par `SequentialFade` : l'ancien s'efface sur la première moitié, le nouveau apparaît sur la seconde, chacun dans un calque mis en cache (ombres du texte rendues une fois), alignés en bas : le bouton et le bas des textes ne bougent pas.

### D3. Verre du dock et de la capsule sur un arrière-plan précalculé
Une seconde transformation produit une copie au huitième (240 × 135) du visuel voilé, floutée par trois passes de flou boîte (rayon équivalent à 28 px à pleine taille) puis saturée à 170 %, comme `backdrop-filter: blur(28px) saturate(170%)` de la maquette. `GlassBackdrop` partage les couches du héro (image, zoom, fondu) avec le dock et la capsule ; ceux-ci dessinent la copie floutée avec la même transformation que le visuel, découpée à leur forme, puis la teinte, la bordure et les reflets de la maquette. Aucun flou n'est recalculé pendant le Ken Burns ou le focus. Sans visuel de programme (vidéo, dégradé du repli), et pour le menu et le bandeau du mode déplacement, `GlassSurface` garde le flou Haze en direct.

### D4. Bannières bornées
`AppArtworkSource` décode la ressource de bannière avec `ImageDecoder.setTargetSize` à 480 × 270 au plus (`ImageBounds.artwork`), en mémoire logicielle ; une bannière vectorielle est rendue à la même taille.

### D5. Écran de démarrage
- `RawMascotAnimationSource.prefetch` lance le décodage dans `SygixOsApp.onCreate`, hors du fil principal ; la source est libérée quand le démarrage est terminé.
- `StartupGate` reçoit `mascotShown` (première image qui montre la mascotte) et `mascotUnavailable` ; la durée minimale part de la première, ou de la première image de l'écran de démarrage si l'animation est illisible ; les plafonds de 2 s et 5 s restent comptés depuis la première image de l'écran de démarrage.
- `StartupHost` ne compose l'accueil qu'après le fondu d'entrée de la mascotte et le focus de la fenêtre (`LocalWindowInfo.isWindowFocused`), ou dès le début du fondu de sortie ; l'animation de la mascotte, jouée par le fil de rendu, ne dépend pas du fil principal.
- Fondu de sortie avec `CompositingStrategy.ModulateAlpha` (pas de calque plein écran).
- `SettingsMotionSource` lit `Settings.Global.ANIMATOR_DURATION_SCALE` (1 par défaut), source déjà utilisée par Compose pour ses animations.

### D6. Profil de référence de l'app
`app/src/main/baseline-prof.txt` : règles génériques pour `fr.sygix.sygixos` et `coil`, développées par AGP au build ; profileinstaller (déjà présent) les installe au premier lancement.

### D7. Variante `perf`
Type de build `perf` (`initWith(release)`, R8, suffixe `.perf`, clé de debug), manifeste de variante sans catégorie HOME et `profileable` pour le shell, tests unitaires désactivés pour cette variante. La CI ne construit que `test` et `assembleRelease`.

## Risks / Trade-offs
- Mémoire : un visuel 1920 × 1080 en ARGB (8 Mo, bitmap matériel) et sa copie floutée par programme en cache ; les couches sont retirées à la fin de chaque fondu.
- Sous le verre, le flou d'un visuel ne contient pas les textes du héro : ils ne passent jamais sous le dock ni sous la capsule.
- L'accueil composé plus tard allonge le démarrage d'environ 300 ms quand le catalogue est prêt tôt ; la durée reste dans les plafonds.
