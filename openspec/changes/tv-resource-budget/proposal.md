# Proposal

## Why

Mesuré sur une TV de référence en build release (v0.3.3), sans interaction :

| Mesure | Valeur |
| --- | --- |
| CPU du launcher sur l'accueil au repos, image fixe dans le héro | ≈ 40 % d'un cœur en continu (pics à 58 %) |
| Images redessinées au repos | 535 à 661 par tranche de 10 s, soit ≈ 55 img/s |
| Même accueil sans le zoom lent (Ken Burns) | ≈ 10 % d'un cœur, 8 à 65 images par 10 s (les seuls fondus de changement de programme) |
| Grille au repos avec un panneau Top Shelf ouvert | 523 images par 10 s, 43 à 52 % d'un cœur |
| Réglages ouverts au repos | 0 image, 0 % |
| Launcher en arrière-plan | 0 % |
| Mémoire (PSS) | 75 Mo au repos, 88 Mo après la grille, 98 Mo en arrière-plan |
| Navigation dans la grille | 7,7 % d'images en retard, p95 85 ms, p99 133 ms |
| Threads | 62, dont 8 `artwork-worker` issus de deux pools identiques |

Le launcher est visible des heures par jour. Le premier poste de consommation est le zoom lent des visuels : un passage de 10 s, relancé toutes les 12 s par le défilement du héro et toutes les 6 s par le panneau Top Shelf. L'écran n'est donc presque jamais immobile. Les autres pertes viennent de travaux en double ou plus lourds que nécessaire : requêtes au TV Provider, décodages d'affiches, cache mémoire sans effet, threads, audio décodé pour une vidéo muette. Le diagnostic complet est dans `design.md`.

## What Changes

- Nouveau réglage « Mouvement des visuels » dans « Écran d'accueil », activé par défaut. Désactivé, les visuels du héro et du Top Shelf restent immobiles et seuls les fondus subsistent. Le comportement actuel est inchangé par défaut ; la coupure des animations par le système produit déjà le même effet.
- Optimisations sans changement visible :
  - le verre du dock n'est plus redessiné à chaque image du zoom ;
  - les affiches du héro sortent du cache mémoire d'images, où elles n'étaient jamais relues ;
  - les compteurs de programmes des réglages ne sont lus que réglages ouverts ;
  - une seule relecture d'À suivre par événement, et aucune relecture au retour d'une app si rien n'a changé ;
  - pas de décodeur ni de sortie audio pour la vidéo muette du héro ;
  - le Top Shelf ne décode à la taille du panneau que l'affiche affichée, comme la spec le prévoit déjà, et pas avant que le focus soit posé ;
  - les badges d'app des cartes À suivre sont convertis à leur taille d'affichage ;
  - un seul pool de threads d'images ;
  - les requêtes de l'index d'images sont regroupées ;
  - les vidéos nature sont en 1920 × 1080 au lieu de 2560 × 1440 ;
  - sous une vidéo, le dégradé de fond n'est plus dessiné et la surface vidéo devient opaque une fois le fondu terminé.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `settings` : catégorie « Écran d'accueil » et navigation de « Position d'À suivre » ; nouveau réglage « Mouvement des visuels ».
- `launcher-shell` : « Diaporama héro » et « Contenu du panneau » suivent le réglage ; « Préchargement et mémoire » précise l'écran au repos, le cache des affiches, les relectures au retour, l'audio de l'aperçu et le décodage des affiches du Top Shelf.

`ui-testing` et `up-next` sont examinées : aucune exigence ne change, les tests ajoutés suivent les règles existantes.

## Impact

`HeroStage`, `ShelfPanel`, `GlassBackdrop`, `HeroPosterPipeline`, `HeroPlayer`, `NatureFallbackProvider`, `AmbientGradient`, `HomeScreen`, `HomeViewModel`, `UpNextController`, `ProviderQuery`, `ArtworkRepository`, `VisualValidator`, `HomeGrid`, `AppArtworkSource`, `UpNextRow`, `SygixOsApp`, préférences et réglages, tests JUnit et Compose, README.

## Dependencies

Part de `main` après le merge de P9 (`p9-betaseries`). Aucune exigence modifiée ici n'est modifiée par P9.

## Non-goals

- Changer la libération du lecteur vidéo en arrière-plan : elle est voulue, elle rend le décodeur à l'app au premier plan (« Préchargement et mémoire »).
- Remplacer la surface vidéo par une `SurfaceView` ou figer le verre sous une vidéo : compromis visuel non mesuré, à évaluer dans un change ultérieur avec des mesures vidéo.
- Bannières d'apps en mémoire graphique, réglage de profondeur de préchargement des bannières et trim mémoire agressif en arrière-plan : gain non mesuré sur la TV, et la copie logicielle des bannières sert au calcul de leur flou ; reporté.
- Coût propre au suivi BetaSeries : traité dans `p9-betaseries`.

## Questions ouvertes

Aucune.
