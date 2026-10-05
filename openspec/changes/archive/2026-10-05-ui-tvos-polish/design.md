# Design

## Context
Voir `proposal.md` pour la motivation. État de départ (`main`, `d5579cf`) :
- `core/designsystem/GlassSurface.kt` applique `hazeGlass` (Haze 2) avec `GlassStyle.regular`, une teinte blanche à 8 %, une aberration chromatique, un `alpha(0.96f)` et le mode de performance par défaut (adaptatif, image d'entrée à environ 0,71 de la taille réelle). Le repli sans flou découpe (`clip`) son contenu.
- `Modifier.tvFocus` zoome à 1,07 et dessine un halo radial de la couleur dominante de la tuile ; il est aussi utilisé tel quel par l'engrenage, les catégories et les lignes des réglages (halo coupé en rectangle par le `clip` des lignes).
- Le bouton « Ouvrir » du héro est une `GlassSurface` au repos : c'est un second flou, dans la source même du héro, actif dès que le focus quitte le bouton (donc dès que le dock a le focus).
- `DockLayout` donne aux tuiles du dock la largeur des tuiles de la grille, puis les réduit au-delà de 5 apps.
- Le Ken Burns du poster du héro (`animateFloatAsState` sur 16 s, alors qu'un programme reste 12 s) continue quand le héro sort de l'écran ; `AmbientGradient`, le panneau Top Shelf et `tvFocus` lisent leurs valeurs animées pendant la composition, ce qui recompose à chaque image.

## Goals / Non-Goals

**Goals:**
- Reproduire les valeurs de la maquette validée dans le design system, une seule fois, et les réutiliser partout.
- Réduire le coût GPU du verre sans renoncer au flou en direct.
- Supprimer tout travail par image qui n'est pas une animation spécifiée et visible.
- Garder l'architecture uniflow : les composables n'ont que de l'état visuel local (focus, animation), aucune décision.

**Non-Goals:**
- Changer les données du héro (voir `proposal.md`, Non-goals).
- Mesurer la fluidité autrement que sur la TV de test (Robolectric ne rend pas le GPU).

## Decisions

### D1. Jetons de la maquette (1920 px = 960 dp, densité 2)
Les valeurs partagées par plusieurs écrans sont dans `core/designsystem` (`Theme.kt` pour les couleurs `SygixColors`, `Motion.kt` pour `Dimens`, `Type.kt` pour `TextStyles`) ; une valeur propre à un seul écran (barre de progression du héro, volet des réglages, interrupteur, couleur du package dans le menu) est une constante privée de cet écran, définie une seule fois.

| Élément | Maquette (px) | Jeton (dp / sp) |
| --- | --- | --- |
| Grille : marge latérale, marge haute | 96, 80 | 48 dp, 40 dp (inchangés) |
| Grille : écart entre colonnes, entre rangées | 48, 64 | 24 dp, 32 dp |
| Tuile : rayon | 18 | 9 dp |
| Focus tuile : zoom, montée | 1,08, 4 | 1,08, 2 dp |
| Focus tuile : ombre | 0 22 40 rgba(0,0,0,.55) | élévation 11 dp, ombre portée noire |
| Focus tuile : reflet | 135°, blanc .30 → .08 à 32 % → 0 à 52 % | même dégradé |
| Dock : tuile, écart, marge interne, rayon, marge basse | 240×135, 28, 20, 40, 48 | 120×67,5 dp, 14 dp, 10 dp, 20 dp, 24 dp ; 6 apps au plus (décision de Sygix) |
| Verre dock / capsule : teinte | rgba(22,22,28,.36 / .38) | `GlassTint`, `CapsuleTint` |
| Verre : bordure, reflet haut, reflet bas | 1 px blanc .16, inset blanc .32, inset blanc .06 | 0,5 dp |
| Verre : ombre dock / capsule | 0 12 32 .32 / 0 8 24 .28 | 6 dp / 16 dp, 4 dp / 12 dp |
| Menu : panneau | 600 de large, rayon 36, rgba(18,18,24,.72), reflet haut .28 sans reflet bas, ombre 0 30 60 .5 | 300 dp, 18 dp, `MenuTint`, `MenuHighlight` |
| Menu : voile | rgba(0,0,0,.55) | `Scrim` |
| Menu : action | 76 de haut, rayon 18, 28 | 38 dp, 9 dp, 14 sp |
| Pilule de focus | #F2F2F5, texte #0B0B0F, secondaire #4A4A52, ombre 0 10 24 .35 | `PillFocus`, `OnPill`, `OnPillSecondary` |
| Catégorie sélectionnée, focus ailleurs | blanc .14 | `PillSelected` |
| Texte des catégories au repos | blanc .9 | `OnDarkRest` |
| « Tout réactiver » au repos | blanc .08 | `PillRest` |
| Bouton du héro au repos | rgba(22,22,28,.45), bordure blanc .22, inset blanc .30 | `ButtonRest`, `ButtonRim` |
| Bouton du héro au focus | blanc, texte #0B0B0F, zoom 1,05, ombre 0 14 32 .38 | `OnPill`, 1,05 |
| Bouton du héro : hauteur, marges, police | 72, 32 / 40, 28 gras | 36 dp, 16 / 20 dp, 14 sp |
| Capsule : position, marges internes, écart | haut 48, droite 64, 8 8 8 26, 18 | 24 dp, 32 dp, 4/4/4/13 dp, 9 dp |
| Capsule : heure, pastille de l'engrenage | 28 semi-gras, 56 (blanc .10) | 14 sp, 28 dp |
| Engrenage | au trait, épaisseur 1,8/24, 30 | 15 dp |
| Engrenage focusé | blanc #FFFFFF, icône #0B0B0F, zoom 1,08, ombre 0 8 18 .35 | idem |
| Héro : bloc de texte | gauche 96, bas 296, largeur 900, écart 20 (26 avant le bouton) | 48 dp, 148 dp, 450 dp, 10 dp (13 dp avant le bouton) |
| Héro : icône lecture | 26 | 13 dp |
| Héro : titre, source, progression | 76 extra-gras ; 24 ; 320×6, piste blanc .28 | 38 sp ; 12 sp ; 160×3 dp |
| Voile bas | vers le haut : .82 à 0 %, .55 à 26 %, 0 à 55 % | même dégradé |
| Voile gauche | vers la droite : .62 à 0 %, .28 à 38 %, 0 à 62 % | même dégradé |
| Voile haut droit | ellipse 640×300 centrée sur le coin, .5 → 0 à 70 % | ellipse 320×150 dp |
| Réglages : volet gauche | 680 de large, haut 96, gauche 96 | 340 dp, 48 dp, 48 dp |
| Réglages : ligne, rayon, catégorie | 96 / 84 / 76 de haut, 16 | 48 / 42 / 38 dp, 8 dp |
| Réglages : vignette de ligne | 96×54, rayon 10 | 48×27 dp, 5 dp : bannière TV de l'app, sinon icône centrée (décision de Sygix) |
| Police | Figtree 400, 500, 600, 700, 800 | Figtree variable (`res/font/figtree.ttf`), `FontVariation.weight` par graisse |
| Ken Burns du héro | — | 10 s, pour 12 s d'affichage (au moins 2 s immobiles par cycle, décision de Sygix) |
| Interrupteur | 76×44, pouce 36, éteint rgba(120,120,128,.55), allumé #34C759 | 38×22 dp, 18 dp |

### D2. Verre : flou réduit, limité, sans couche de groupe
`GlassSurface` garde `hazeGlass` et `HazeInput.Sources` (le flou suit le fond en direct, décision de Sygix) mais :
- `performanceMode = HazePerformanceMode.Performance` : Haze calcule le verre sur une image d'entrée à 0,5 de la taille réelle (un quart des pixels), au lieu d'environ 0,71 en mode adaptatif ;
- plus d'aberration chromatique ni d'`alpha` global (un alpha < 1 force une couche de groupe hors écran) ;
- teinte sombre (D1) au lieu de la teinte blanche ;
- bordure, reflets intérieurs et ombre dessinés par des modificateurs Compose (`border`, `innerShadow`, `dropShadow`), mis en cache par Compose et indépendants du fond ;
- le flou n'existe que sous le dock, la capsule, le menu et le bandeau du mode déplacement ; Haze ne traite que la zone de la surface.
Le repli (pas de `HazeState`, verre coupé) dessine un fond sombre plus opaque (`GlassFallback`, et `MenuFallback` plus foncé pour le menu) avec la même bordure, sans découper le contenu (`background(shape)` au lieu de `clip`) : une tuile focusée du dock n'est jamais coupée.
Alternative écartée : un `RenderEffect` maison sur une copie réduite du héro ; Haze le fait déjà et gère le repli des appareils sans `RuntimeShader`.

### D3. Bouton du héro sans flou
Au repos, `Modifier.glassRim` (fond `ButtonRest`, bordure et reflet de D1), sans `hazeGlass` ; au focus, fond blanc, texte `OnPill`, zoom 1,05 et ombre. Cela supprime le second flou, qui doublait le travail GPU dès que le focus quittait le bouton (95 % d'images en retard avec le focus sur le dock).

### D4. Focus des tuiles
`Modifier.tvFocus` devient le focus tvOS des tuiles : une seule `graphicsLayer` porte le zoom, la montée et l'ombre (élévation de plateforme, ombre noire douce), lues en phase de dessin depuis la progression animée ; le reflet est un dégradé dessiné par-dessus le contenu, découpé à la forme de la tuile. Plus de halo ni de couleur dominante (le paramètre `glow` disparaît). Le câblage du focus (requester, `canFocus`, `onFocusChanged`) est extrait dans `Modifier.tvFocusable`, réutilisé par les pilules et les boutons.
L'espacement de la grille passe à 24 dp entre colonnes et 32 dp entre rangées : avec une tuile de 153,6 dp de large, le zoom 1,08 ajoute 6,1 dp de chaque côté (12,3 dp au total, sous 24 dp), et 3,5 dp en hauteur plus 2 dp de montée. `GridScroll` utilise déjà l'espacement entre rangées comme marge basse, qui passe donc à 32 dp (« Panneau Top Shelf au focus »).
Aucun nom d'app n'est affiché sur ou sous les tuiles (décision de Sygix) : `TileBox` garde le repli de `main` (icône centrée, initiale si aucune image n'est disponible).

### D5. Dock à taille fixe
`DockLayout` devient une fonction pure de la taille fixe : largeur du dock = tuiles + écarts + marges internes, bornée à la largeur disponible ; 6 tuiles tiennent dans 864 dp, sans défilement.
Limite à 6 apps (décision de Sygix), dans `domain/AppCatalog.kt` : `MAX_DOCK = 6`, `dock()` ne garde que les 6 premières apps épinglées visibles, `pinState()` (épinglée, disponible, dock plein) alimente le menu, et `togglePinned()` refuse un nouvel épinglage quand le dock est plein. Les épinglages enregistrés ne sont jamais tronqués : une base d'une version antérieure avec plus de 6 apps garde toutes ses entrées, le dock affiche les 6 premières (ordre d'enregistrement, confirmé par Sygix), et une app retirée laisse la place à la suivante.
Dock plein : « Épingler au dock » reste focusable mais grisé, avec la ligne « Dock plein (6 apps maximum) » (ressource `menu_dock_full`), sémantique désactivée, OK sans effet (confirmé par Sygix).

### D6. Capsule et heure
- La capsule est une `GlassSurface` (teinte `CapsuleTint`, forme pilule), active comme le verre du dock tant que le héro est à l'écran. Elle contient l'heure puis l'engrenage (pastille de 28 dp, blanc .10 au repos, blanc au focus avec icône `OnPill`, zoom 1,08, ombre). L'engrenage est dessiné au trait (décision de Sygix) : contour obtenu par union du disque et de 8 dents (`Path.combine`) plus le moyeu, tracés avec un trait arrondi de 1,8/24 de la taille ; dessin propre au projet, sans reprendre le tracé d'une bibliothèque d'icônes (à valider à l'œil sur la TV, question 1).
- Retour depuis l'engrenage rend le focus au bouton d'ouverture du héro (au héro sans bouton), décision de Sygix.
- Source : `data/SystemClockSource` émet l'heure formatée avec `android.text.format.DateFormat.getTimeFormat(context)` (format 12/24 h et langue du système, relus à chaque émission) au démarrage de la collecte puis à chaque diffusion `ACTION_TIME_TICK`, `ACTION_TIME_CHANGED` et `ACTION_TIMEZONE_CHANGED` (récepteur enregistré pendant la collecte seulement). Aucune donnée externe, aucune erreur possible autre qu'un enregistrement refusé, qui est journalisé et laisse la dernière heure connue.
- `HomeViewModel.clock` expose un `StateFlow<String>` séparé de `HomeState` (`WhileSubscribed`, valeur initiale lue tout de suite pour que la capsule ne change pas de largeur au démarrage) ; seul le composable de l'heure le collecte (`collectAsStateWithLifecycle`), donc une nouvelle minute ne recompose que ce texte, et rien n'est collecté en arrière-plan. Au retour au premier plan, la collecte reprend et émet l'heure courante aussitôt.
- Gauche et droite depuis l'engrenage ne trouvent aucune cible avec la recherche de focus 2D de Compose (vérifié par `HeroCapsuleTest` avec un bouton « Ouvrir » présent en bas à gauche) : aucun code dédié.

### D7. Héro : voiles et espacement
Les trois voiles sont dessinés par un seul `drawBehind`, chacun limité à la zone qu'il couvre (55 % du bas, 62 % de la gauche, rectangle de l'ellipse en haut à droite) pour limiter le remplissage GPU ; l'ellipse est un dégradé radial dont la matrice locale étire le cercle (rayons 320 × 150 dp). Ils font partie de la source du flou : le dock et la capsule floutent un fond déjà assombri.
Les métadonnées et le bouton sont une seule colonne espacée de 10 dp, plus 3 dp au-dessus du bouton (13 dp, maquette) ; la barre de progression n'est composée que si elle existe (plus de boîte transparente de 15 dp) et le titre n'a plus `minLines = 2`. Le bouton reste ancré en bas : seul le haut du bloc bouge d'un programme à l'autre.

### D8. Pilules (réglages et menu)
`core/designsystem/FocusPill.kt` : `pillColors(focused, selected, rest, restContent)` (fonction pure : `PillFocus`/`OnPill` au focus, `PillSelected`/blanc si sélectionnée, sinon `rest`/`restContent`, blanc .9 pour les catégories) et `Modifier.focusPill(colors, focused, shape)` qui peint le fond et l'ombre au focus, sans zoom. Les lignes des réglages, « Tout réactiver », les licences et les actions du menu l'utilisent ; leurs textes prennent la couleur de contenu de la pilule. Plus aucun `tvFocus` dans les réglages.
Le menu contextuel passe en liste verticale (maquette), panneau `GlassSurface` teinté `MenuTint` sur le voile `Scrim`, en-tête avec la vignette de l'app. Les actions sont celles de la maquette (Épingler/Retirer, Déplacer en grille, Cacher) ; « Fermer » est retiré, Retour ferme le menu (décision de Sygix). Repos transparent et pilule claire sans zoom au focus, confirmés par Sygix. Panneau : reflet haut .28, sans reflet bas.

### D9. Fluidité : ce qui tournait au repos et ce qui change
Constat sur la TV de test (rc.4) : héro au repos ≈ 32 i/s et 50 % d'images en retard, focus sur le dock 95 %, grille au repos 0 % (une fois 626 images en 10 s à l'écran identique) ; « Slow issue draw commands » égal au nombre d'images en retard, donc goulot GPU.
Sources trouvées dans le code :
1. Ken Burns du poster du héro : 16 s d'animation pour 12 s d'affichage, donc toujours actif, et il continuait hors écran après la descente vers la grille (jusqu'à 16 s d'images rendues pour rien, cohérent avec les 626 images en 10 s). → Le Ken Burns s'arrête quand le héro n'est plus visible et reprend à son retour (`Animatable` piloté par `visible`).
2. Second flou du bouton du héro (D3). → Supprimé.
3. Verre du dock en pleine qualité adaptative avec aberration chromatique et alpha global (D2). → Mode `Performance`, sans aberration ni alpha.
4. Lectures de valeurs animées en composition (`AmbientGradient`, Ken Burns du panneau Top Shelf, `tvFocus`, zoom du bouton du héro et des catégories) : recomposition à chaque image. → Lectures dans `graphicsLayer` / `drawBehind`. Restent en composition, seulement pendant les 300 ms d'une transition de focus et jamais au repos : les couleurs animées des pilules (`animatedPillColors`) et la couleur du texte du bouton du héro.
5. Ken Burns (décision de Sygix) : un seul passage par visuel, puis arrêt sur la dernière position ; il repart à chaque nouveau visuel. Héro : `Animatable` par URL, passage de 10 s (`Motion.HERO_KEN_BURNS_MS`, décision de Sygix) achevé avant le changement de visuel à 12 s, mis en pause hors écran. Panneau Top Shelf : la transition infinie aller-retour est remplacée par un `Animatable` par affiche (`Motion.SHELF_KEN_BURNS_MS`, 16 s). Une affiche seule devient immobile après son passage.
6. Dégradé animé du repli (décision de Sygix) : un seul passage de 24 s (`Motion.AMBIENT_PASS_MS`, `Animatable`), puis figé ; il reprend là où il s'était arrêté si le héro revient sans visuel. Restent animés au repos, parce que spécifiés : la vidéo d'aperçu et le changement de visuel du défilement automatique (12 s sur le héro, 6 s dans le panneau).
Mesures reproductibles ajoutées (JVM, `IdleFrameTest`), pendant plusieurs secondes d'horloge de test sans touche :
- compteur de recompositions (`Recomposer.runningRecomposers`, somme des `changeCount`) : zéro sur le héro au dégradé animé, avec le dock focusé sur ce héro, et sur un poster en Ken Burns ; les deux premiers cas échouent sur `main` (le dégradé lisait sa phase en composition) ;
- écritures d'état Compose appliquées (`Snapshot.registerApplyObserver`) : zéro en vue grille, y compris juste après avoir quitté un poster (échoue sur `main`, Ken Burns hors écran) ; zéro sur le héro et dans le panneau à une affiche après la fin du passage du Ken Burns (le cas du panneau échoue sur `main`, transition infinie) ; des écritures de nouveau quand le visuel change après la fin du passage précédent (reprise).
- observateur de composition (`Composition.setObserver`) : une nouvelle minute ne lit que l'état de l'heure et ne recompose que quelques scopes (3 mesurés, contre 15 si l'accueil lisait l'heure).
Le poster est chargé par un `ImageLoader` Coil de test qui répond tout de suite, sans réseau.

### D11. Police Figtree (décision de Sygix)
- Source : dépôt officiel `google/fonts`, `ofl/figtree/Figtree[wght].ttf` au commit `a60a77e14f28abd4ef243a1b5dfc48df0cec5205` (blob Git `579e2ab5f30beb21cd0b618dd0796b0fce0eff8e`, SHA-256 `26ad3db9b31ff7dde67a91ff515d022d2f495cd506590699cf264f0bfe6fb714`, version de police 2.002, issue du dépôt amont `erikdkennedy/figtree` au commit `032dfa7`) ; licence `OFL.txt` du même commit (blob `80b12bb3d0c0942676657ef6d125daf4a1e1346d`).
- Fichier : la police variable (axe `wght` 300-900, 62 712 octets) plutôt que cinq fichiers statiques : un seul fichier couvre les graisses 400, 500, 600, 700 et 800 de la maquette, chacune déclarée dans `Figtree` (`Type.kt`) avec `FontVariation.weight`. Toute la typographie (`SygixTypography`, styles Material 3 compris, et `TextStyles`) utilise `Figtree`.
- Licence dans « À propos » : mécanisme existant d'AboutLibraries ; `app/config/libraries/figtree.json` et `app/config/licenses/OFL-1.1.json` (texte complet de l'OFL) sont déclarés par `aboutLibraries { collect { configPath } }` et apparaissent dans la liste générée.

### D10. Tests
- JUnit : `DockLayoutTest` (taille fixe, bornage), `AppCatalogTest` (6 apps au plus, épinglages enregistrés conservés, épinglage refusé dock plein, état d'épinglage), `FocusPillTest` (couleurs), `GlassContrastTest` (contraste ≥ 4,5:1 du texte du menu composé sur blanc avec les jetons, échoue avec l'ancienne teinte blanche), `SystemClockSourceTest` (format 12/24 h selon le réglage système, émission initiale et à chaque diffusion), `GlassSurfaceTest` (le repli ne découpe plus un enfant plus grand que la surface).
- Compose à la taille TV : `TvFocusStyleTest` (tuile sans débordement en grille et dans le dock, tuiles des quatre coins et des deux bouts du dock entières, aucun nom sur les tuiles, dock à taille fixe et centré), `HeroCapsuleTest` (capsule, focus de l'engrenage seul, gauche/droite, Retour vers le bouton du héro, heure mise à jour, seule l'heure recomposée), `HeroLayoutTest` (pas d'espace vide sans progression), `SettingsPillTest` (pas de zoom au focus), `SettingsThumbnailTest` (bannière TV, repli sur l'icône), `ContextMenuTest` (liste verticale de trois actions, dock plein, dock à 5 apps), `IdleFrameTest` (écran au repos).
- Tests adaptés : `HideFlowTest` et `CatalogUpdateFocusTest` (menu vertical : bas au lieu de droite, l'assertion de focus sur l'action « Cacher » est conservée sous son nouveau tag « menu-action-hide ») ; `DockLayoutTest` (la réduction au-delà de 5 apps n'est plus spécifiée) ; `AppCatalogTest` (`togglePinned` reçoit le dock affiché) ; `HomeGridScrollTest` (positions attendues réécrites en dp littéraux pour le nouvel espacement) ; `HomeViewModelSourceToggleTest` (nouvelle dépendance `clockSource`) ; `AccentColorTest` supprimé avec `AccentColor`, devenu inutile sans halo coloré ; `SettingsEntryTest` est étendu (la capsule part et revient avec le héro, l'engrenage garde son tag).

### Ce qui reste à mesurer sur la TV
Sur une pré-release (`assembleRelease`, R8), avec `adb shell dumpsys gfxinfo fr.sygix.sygixos reset` puis `framestats` après 10 s sans toucher :
1. Héro au repos sur un poster : pendant le passage du Ken Burns, images par seconde, part d'images en retard et « Slow issue draw commands » ; après le passage (un seul programme), nombre d'images rendues en 10 s (attendu : quasi nul).
2. Focus sur le dock pendant 10 s : mêmes compteurs ; l'écart avec le cas 1 doit avoir disparu (plus de second flou).
3. Grille au repos, juste après la descente depuis un poster : nombre d'images rendues en 10 s (attendu : quasi nul).
4. Coût du verre : comparer le cas 1 avec l'option de débogage `noglass` (intent extra existant) pour isoler la part du flou.
5. Rendu visuel : verre sombre lisible sur poster clair et sombre, image d'entrée réduite acceptable (pas de pixelisation visible), ombre et reflet des tuiles, absence de halo, menu lisible sur tuiles claires, pilules des réglages.

## Risks / Trade-offs
- [Image d'entrée réduite visiblement pixelisée] → Haze lisse le résultat ; à juger sur la TV (point 5). Repli possible : `HazePerformanceMode.Balanced`.
- [Ombre d'élévation de plateforme différente de l'ombre CSS de la maquette] → ombre douce cohérente avec Android, sans flou logiciel ; à juger sur la TV.
- [Pendant le passage du Ken Burns, le flou est recalculé à chaque image] → coût réduit (D2) ; l'image est immobile au moins 2 s par cycle de 12 s, et en continu après le passage avec un seul programme.
- [Ordre d'archivage avec `up-next`] → voir `proposal.md`, Dépendances.
- [Tests qui ne voient pas le GPU] → les tests vérifient la géométrie, l'absence de travail au repos et les couleurs calculées ; le rendu est validé sur la TV.

## Migration Plan
Aucune donnée persistée ne change. Retour arrière : revert de la PR.
