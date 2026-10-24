# Design

## Context

### Méthode de mesure

Build `assembleRelease` signé, TV de référence, adb. Chaque mesure est faite au moins trois fois, l'app au premier plan, sans interaction et après réveil de l'écran de veille système (qui suspend l'app). Les commandes sont dans « Protocole de mesure ». Les pourcentages CPU sont ceux de `top` : 100 % = un cœur.

### Mesures de référence (v0.3.3, sans compte de suivi)

| Situation | Images rendues | CPU process | Détail |
| --- | --- | --- | --- |
| Accueil au repos, affiche fixe | 535, 541, 561, 604, 259, 661, 605, 587, 272 par 10 s | 37 à 58 %, ≈ 40 % en moyenne | `RenderThread` 26,6 %, thread principal 16,6 %, pilote GPU 3,3 % |
| Même build sans Ken Burns (A/B) | 8, 65, 51, 40 par 10 s | 0 à 35 %, ≈ 10 % en moyenne | ne restent que les fondus de 1,4 s toutes les 12 s |
| Grille au repos, panneau Top Shelf ouvert | 523 par 10 s | 22 à 52 % | panoramique de 16 s relancé toutes les 6 s |
| Réglages ouverts au repos | 0 | — | l'accueil n'est pas placé |
| Launcher en arrière-plan | — | 0,0 % (12 mesures) | pas d'alarme, pas de job actif |

| Interaction | Images | En retard | p50 | p90 | p95 | p99 |
| --- | --- | --- | --- | --- | --- | --- |
| Navigation grille (6 bas, 3 droite, 6 haut, 3 gauche) | 702 | 7,7 % | 25 ms | 46 ms | 85 ms | 133 ms |
| Réglages, catégories | 116 | 12,9 % | 25 ms | 77 ms | 97 ms | 133 ms |
| Réglages, contenu | 304 | 5,3 % | 25 ms | 25 ms | 25 ms | 28 ms |
| Retour des réglages | 289 | 0,35 % | 25 ms | 25 ms | 25 ms | 25 ms |

Mémoire (PSS) :
- repos 75,2 Mo (tas Java 21,0, natif 29,3, code 9,3) ;
- après la grille 87,7 Mo (Java 27,8) ;
- en arrière-plan 98,5 Mo (Java 34,2).

Le tas Java n'est pas en fuite : `Heap Size` 26,9 Mo pour `Heap Alloc` 13,5 Mo, les pages libres ne sont pas rendues entre deux GC. Le tas natif passe de 33 à 49,5 Mo à un changement d'affiche du héro (décodage, transformation, copie). Le démarrage après mise à jour (processus déjà relancé par le service d'accessibilité) prend 453 à 528 ms. Le processus compte 62 threads, dont 8 `artwork-worker`, 7 `DefaultDispatcher` et 2 `CodecLooper` alors que le héro affiche une image.

### Cause du rendu permanent

Avec une affiche fixe, le seul moteur d'invalidation par image est le Ken Burns du héro :
- `HeroStage.kt` lance `layer.zoom.animateTo(KenBurnsScale, tween(Motion.HERO_KEN_BURNS_MS))` pour chaque calque prêt ;
- le défilement automatique (`delay(Motion.HERO_DWELL_MS); step(1)`) crée un calque toutes les 12 s ;
- le zoom de 10 s occupe donc ≈ 83 % du temps.

Le `Spacer.drawBehind` du poster lit `layer.zoom.value` : il n'y a pas de recomposition, mais HWUI rend une image complète. Celle-ci comprend :
- l'affiche 1920 × 1080 mise à l'échelle ;
- le verre du dock et de la capsule, car `GlassBackdrop.drawLayers` lit aussi `zoom` et `fade` ;
- le calque hors écran du texte (`SequentialFade`).

Dans la grille, `ShelfPanel` relance un panoramique de 16 s toutes les 6 s. Le code ne contient aucune `rememberInfiniteTransition`, ni `infiniteRepeatable`, ni boucle `withFrameNanos` non bornée. Le dégradé ambiant ne joue qu'un passage, sans visuel ; l'horloge redessine une fois par minute.

## Goals / Non-Goals

**Goals:**
- Donner aux utilisateurs de TV modestes un réglage qui rend l'accueil immobile au repos, sans changer le rendu par défaut.
- Supprimer les travaux en double ou inutiles qui n'ont aucun effet visible.

**Non-Goals:** voir `proposal.md`.

## Decisions

### D1 — Réglage « Mouvement des visuels »

Un switch dans « Écran d'accueil », après « Position d'À suivre », activé par défaut et persisté dans DataStore avec les autres préférences d'accueil. Il entre dans le même drapeau que l'échelle d'animation système déjà lue (`SystemMotionSource`) : `motion = système && réglage`.

Désactivé :
- ni Ken Burns du héro ni panoramique du Top Shelf ; le visuel est dessiné à l'échelle 1 ;
- le fondu croisé, le défilement automatique et le changement d'affiche du panneau restent ;
- les vidéos d'aperçu ne sont pas concernées.

Gain mesuré par A/B : ≈ 30 points de CPU au repos sur l'accueil, et de 523 images/10 s à quelques fondus dans la grille. Alternative écartée : couper le Ken Burns par défaut, ce qui change le rendu tvOS validé par Sygix.

### D2 — Verre indépendant du zoom

`GlassBackdrop.drawLayers` n'applique plus le zoom au flou du dock et de la capsule. Sur une copie 240 × 135 floutée à 28 px, un zoom de 1,00 à 1,08 est invisible. Le fondu continue d'être suivi. Le dock et la capsule ne sont plus invalidés à chaque image du zoom : seule l'affiche l'est.

### D3 — Affiches du héro hors cache mémoire d'images

Les requêtes poster et backdrop de `HeroPosterPipeline` passent en `memoryCachePolicy(DISABLED)`. Le carrousel est séquentiel sur jusqu'à 24 programmes : une entrée de 8,3 Mo est évincée avant d'être relue. L'affichage dispose déjà de ses calques et la spec prévoit la relecture depuis le cache disque. L'écriture disque reste inchangée.

### D4 — Compteurs des réglages à la demande

`SettingsViewModel.counts` n'est plus collecté par l'accueil en permanence : il ne l'est que réglages ouverts. Aujourd'hui, il ajoute un troisième observateur et deux requêtes complètes au TV Provider à chaque changement et à chaque retour au premier plan. Au démarrage, ces requêtes concurrencent celles du héro.

### D5 — Une relecture par événement, aucune relecture aveugle au retour

- `heroUpdates` n'appelle plus `refreshUpNext()` : `UpNextController.updates` observe déjà le même URI, et la seconde requête annulait la première.
- `ContentResolver.changes()` reste enregistré pendant l'arrêt de l'activité et lève un drapeau à `onChange`. Au retour au premier plan, la relecture n'a lieu que si le drapeau est levé ou si le TTL du héro est échu.

Les échéances des images et programmes restent contrôlées comme aujourd'hui, selon « Retour au premier plan après libération ».

### D6 — Vidéo du héro sans audio

`HeroPlayer` construit ExoPlayer avec un `DefaultTrackSelector` qui désactive le type de piste audio. Le volume à 0 laissait le décodeur audio et l'`AudioTrack` actifs quand la piste existe. La vidéo reste muette comme spécifié.

### D7 — Vidéos nature en 1920 × 1080

Les 11 URLs de `NatureFallbackProvider` passent de 2560 × 1440 aux versions 1920 × 1080 du même clip, même fps. Les 11 adresses ont été vérifiées (HTTP 200, `video/mp4`, même dossier de vidéo). L'écran est en 1080p : le décodeur traite 44 % de pixels en moins et le cache disque, comme le réseau, ≈ 1,8 fois moins de volume.

### D8 — Top Shelf : un seul décodage pleine taille

`VisualValidator.prepare` ne décode plus chaque candidate à la taille du panneau. La validation reste celle de l'en-tête (`validate`) et seule l'affiche affichée est décodée, comme le prévoit déjà « Préchargement et mémoire ». Le décodage anticipé de la première affiche dans `HomeGrid` attend le même délai de focus posé que la validation à la demande (0,5 s), toujours dans le délai d'ouverture du panneau. Une traversée rapide de la grille ne décode plus une affiche de 5 Mo par tuile.

### D9 — Ressources partagées

- **Pool d'images :** `ProviderQueries.Shared` réutilise l'ordonnanceur de `ArtworkRepository` au lieu d'un second `ArtworkScheduler`, soit 4 à 6 threads en moins.
- **Index d'images :**
  - `ArtworkRepository.schedule()` est regroupé, un seul calcul différé par rafale, au lieu d'une lecture complète de la table à chaque `resolve` ;
  - `purge()` passe à une requête SQL de sélection des orphelins et périmés au lieu d'une requête d'associations par image.
- **Badges :** les badges d'app des cartes et du menu À suivre sont convertis à leur taille d'affichage (36 dp), et non à la taille intrinsèque de l'icône.

### D10 — Sous une vidéo

- Le dégradé ambiant n'est plus dessiné une fois la première image vidéo rendue et le fondu terminé.
- La `TextureView` devient opaque à la fin du fondu.

Le rendu reste identique : le dégradé était entièrement recouvert. On économise une passe plein écran et le mélange alpha de la vidéo.

## Diagnostic détaillé (pour les agents)

Pistes classées par gain attendu. **[M]** = mesuré sur la TV, **[C]** = vérifié dans le code, coût à mesurer.

| Piste | Mécanisme (fichier) | Gain attendu | Retenue |
| --- | --- | --- | --- |
| Ken Burns héro + panoramique Top Shelf | `HeroStage` zoom par calque, défilement 12 s ; `ShelfPanel` 16 s toutes les 6 s | -30 points CPU au repos, GPU au repos **[M]** | D1 (option) |
| Verre suivant le zoom | `GlassBackdrop.drawLayers` lit `zoom` | moins de travail par image du zoom **[C]** | D2 |
| Cache mémoire des affiches du héro | `HeroPosterPipeline`, cache Coil ≈ 15 % du tas | 16 à 33 Mo de mémoire graphique **[C]** | D3 |
| Compteurs des réglages | `HomeScreen` collecte `counts` | -2 requêtes TV Provider par événement et par retour **[C]** | D4 |
| Double relecture d'À suivre, relecture aveugle au retour | `HomeViewModel.heroUpdates`, `UpNextController.updates`, `ProviderQuery.changes` | -3 à -6 requêtes par retour d'app, moins de transactions Room **[C]** | D5 |
| Audio d'une vidéo muette | `HeroPlayer.create` (`volume = 0f`) | décodeur audio et `AudioTrack` en moins **[C]** | D6 |
| Clips nature 1440p | `NatureFallbackProvider.CLIPS` | -44 % de pixels décodés, -45 % de débit **[C]** | D7 |
| 8 décodages 1832 × 687 par app focalisée | `VisualValidator.prepare`, `HomeGrid` | -35 Mo de churn graphique par pause, moins de saccades p95 en navigation **[C]** | D8 |
| Deux pools `artwork-worker` | `ProviderQuery.Shared`, `ArtworkRepository` | -4 à -6 threads **[M]** 8 threads observés | D9 |
| Scan complet / N+1 de l'index | `ArtworkRepository.schedule`, `purge` | -100 à -300 ms CPU de fond par relecture **[C]** | D9 |
| Badges à taille intrinsèque | `UpNextRow`, `UpNextContextMenu` (`icon.toBitmap()`) | ≈ 150 Ko par carte **[C]** | D9 |
| Dégradé sous la vidéo, vidéo non opaque | `AmbientGradient`, `HeroStage` `TextureView(isOpaque = false)` | ≈ 1 passe plein écran par image vidéo **[C]** | D10 |
| Libération du lecteur à chaque arrêt | `HeroStage` `LifecycleStartEffect` | — | Écartée : voulue par la spec |
| Bannières en bitmap logiciel | `AppArtworkSource` `ALLOCATOR_SOFTWARE` | — | Écartée : pixels nécessaires au flou des tuiles |
| Mascotte décodée à tout démarrage de processus | `SygixOsApp` | — | Écartée : imposée par « Préparation du démarrage » |
| Flou Haze direct sous vidéo, `SurfaceView` | `GlassSurface`, `HeroStage` | — | Reportée : compromis visuel non mesuré |

Autres points relevés, sans changement dans ce change :
- `HomeGrid` reconstruit la table des sources du panneau à chaque recomposition de rangée ;
- `FocusPill` utilise 4 `animateColorAsState` par ligne ;
- `LauncherSystemViewModel` relance `onResume()` à chaque changement d'état du launcher par défaut ;
- l'expansion du panneau et le défilement animé sont simultanés.

Ils sont candidats à un change de qualité ultérieur, avec une mesure dédiée.

## Protocole de mesure

```sh
PKG=fr.sygix.sygixos
adb shell dumpsys gfxinfo $PKG reset; sleep 10; adb shell dumpsys gfxinfo $PKG | grep -E "Total frames|Janky|50th|90th|95th|99th"
adb shell top -b -d 5 -n 12 -p $(adb shell pidof $PKG)
adb shell top -H -b -n 1 -p $(adb shell pidof $PKG)
adb shell dumpsys meminfo -a $PKG | grep -E "Java Heap|Native Heap|Heap Alloc|Heap Size|TOTAL PSS"
adb shell dumpsys alarm | grep -A2 $PKG; adb shell dumpsys jobscheduler | grep $PKG
```

À répéter dans les situations du tableau de référence. L'écran de veille système interrompt l'app : vérifier `topResumedActivity` avant chaque série.

## Risks / Trade-offs

- [Réglage oublié] → activé par défaut, effet immédiat et réversible, comme « Afficher À suivre ».
- [Relecture manquée au retour] → l'observateur reste enregistré et le TTL du héro force une relecture ; test de non-régression sur un changement survenu pendant l'arrêt.
- [Version vidéo retirée par l'hébergeur] → le repli existant (clip suivant, puis dégradé) s'applique comme aujourd'hui.

## Migration Plan

Aucune migration de données : le réglage est absent des préférences existantes et vaut « activé ». Archiver le change après le merge et la mesure sur la TV réelle.
