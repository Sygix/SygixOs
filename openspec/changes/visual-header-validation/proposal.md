# Proposal

## Why

La vérification des visuels du héro et du Top Shelf décode chaque image en 1920 × 1080 (jusqu'à 24 visuels du héro par chargement, 8 affiches par app focalisée) alors qu'elle n'a besoin que de ses dimensions : ce travail occupe les décodeurs partagés avec l'affichage pendant la navigation. Les quatre premiers visuels du héro sont en plus gardés en mémoire sans que le héro ne les relise. Enfin, la vérification agrandit les petites images avant de mesurer leur largeur : toute image paysage est acceptée, même minuscule. Le seuil de 1080 px de « Qualité des visuels » écarterait pourtant la plupart des visuels réellement publiés (840 à 1024 px pour plusieurs services, 640 px pour YouTube).

## What Changes

- Les visuels sont vérifiés d'après les dimensions lues dans l'en-tête de l'image, sans décoder ses pixels ; le chargement et le cache disque restent ceux de l'affichage.
- Aucun visuel vérifié n'est gardé en mémoire : tous restent en cache disque.
- La largeur est mesurée sur l'image ramenée dans 1920 × 1080 sans agrandissement : une image de moins de 640 px de large ou plus haute que large est écartée (le seuil des vidéos reste 1080 px).
- Pour une miniature YouTube, souvent publiée en 640 × 480, la variante pleine résolution (`maxresdefault`) est essayée d'abord et remplace l'image publiée à l'affichage ; l'image publiée sert de repli.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : « Qualité des visuels » (règle de largeur explicite, variante YouTube) et « Préchargement et mémoire » (scénario « validation au chargement »).

Dépend de `artwork-tile-decode`, `hero-visual-retry`, `hero-background-release` et `virtualized-home-grid`, qui modifient aussi « Préchargement et mémoire » (repris tels quels) : les archiver avant celui-ci.

## Impact

`VisualValidator` (variantes d'un visuel) et l'orchestration de la vérification dans `HomeViewModel`, tests JUnit. Aucune dépendance ajoutée. Les images de moins de 640 px de large ou en portrait ne sont plus affichées dans le héro ni le Top Shelf.

## Non-goals

- Lire l'orientation EXIF des images : les visuels publiés par le TV Provider n'en dépendent pas.
- Préchauffer en mémoire la première affiche du Top Shelf avant son ouverture : porté par `virtualized-home-grid` (requête de l'affichage, pas de la vérification).
