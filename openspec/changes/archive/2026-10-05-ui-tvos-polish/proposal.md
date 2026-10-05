# Change : ui-tvos-polish

## Why
Le test de la pré-release v0.0.1-rc.4 sur la TV de test a relevé des défauts visuels et de fluidité qui éloignent le launcher de sa référence, tvOS 26 (Liquid Glass) : dock en verre clair à la couleur peu Apple, halo de focus coloré qui déborde (et, dans les réglages, coupé en rectangle), texte blanc illisible sur le verre clair du menu contextuel, texte du héro illisible sur un poster clair, espace vide sous le titre du héro sans progression, engrenage seul peu lisible. Surtout, le héro au repos tourne à environ 32 images/s avec la moitié des images en retard (95 % quand le dock a le focus), et le goulot est le GPU : le flou du verre recalculé sur le fond animé du héro. Sygix a validé une maquette 1920×1080 et une liste de décisions ; ce change les traduit en exigences.

## What Changes
- **Thème et verre** : thème toujours sombre, police Figtree (OFL 1.1, listée dans « À propos ») ; matériau Liquid Glass façon tvOS 26 : teinte sombre transparente, fine bordure claire, fin reflet en haut, ombre légère. Le flou reste calculé en direct mais sur une image réduite, seulement sous les surfaces qui en ont besoin (dock, capsule, menu contextuel). Le bouton du héro n'est plus une surface floutée : au repos, fond sombre translucide et fine bordure façon verre ; au focus, fond blanc et texte noir, léger zoom et ombre. L'ancienne règle « aucune ombre noire, relief par des halos clairs ou colorés » est retirée.
- **Focus des tuiles (grille et dock)** : zoom d'environ 1,08 avec une légère montée, ombre portée douce, reflet blanc en diagonale, aucun halo ; rien ne déborde sur les tuiles voisines (l'espacement de la grille passe aux valeurs de la maquette pour absorber le zoom) ni n'est coupé par le conteneur. Aucun nom d'app sur ou sous les tuiles (décision de Sygix). Les marges de défilement de la grille suivent le nouvel espacement entre rangées.
- **Dock** : verre sombre ; 6 apps au plus, comme tvOS (décision de Sygix) ; tuiles de taille fixe, plus petites que celles de la grille (environ 80 %) ; dock centré, ajusté au nombre d'apps. Dock plein : « Épingler au dock » est indisponible, avec un message court ; aucun épinglage enregistré n'est effacé.
- **Capsule heure et réglages** (remplace l'engrenage seul, exigence renommée) : capsule en verre en haut à droite du héro, avec l'heure (format 12 ou 24 h du système, non focusable, mise à jour à la minute sans recomposer l'écran) et l'engrenage dessiné au trait, seul élément focusable (pastille blanche, icône noire au focus) ; Retour depuis l'engrenage rend le focus au bouton du héro. Elle reste solidaire du héro.
- **Héro** : voiles en dégradé (bas, gauche, léger voile radial en haut à droite derrière la capsule) pour la lisibilité sur un poster clair ; plus d'espace vide entre les métadonnées et le bouton quand la progression est absente.
- **Menu contextuel** : panneau en verre plus foncé sur un voile sombre, actions en liste verticale (Épingler/Retirer, Déplacer, Cacher ; plus de « Fermer », Retour ferme), focus en pilule claire, lisible quelles que soient les tuiles derrière.
- **Réglages** : même fond que la grille ; vignette 16:9 des lignes d'apps avec la bannière TV de l'app ; focus des lignes (catégories, Apps sources, Applications cachées, « Tout réactiver », À propos) en pilule claire à texte sombre, sans zoom, sans halo, sans verre ; catégorie sélectionnée en pilule grise discrète quand le focus est ailleurs.
- **Fluidité** : aucune animation ne tourne pour un élément hors écran (Ken Burns du héro mis en pause quand le héro sort) ; Ken Burns du héro (10 s, avant le changement de visuel à 12 s) et du panneau Top Shelf en un seul passage, puis image immobile, reprise au changement de visuel ; dégradé du repli en un seul passage, puis figé (décisions de Sygix) ; plus aucune recomposition par image pour les animations continues (lecture des valeurs animées en phase de dessin) ; plus de second flou sur le héro ; le bandeau du mode déplacement garde son flou.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `launcher-shell` : « Grille d'apps » (menu contextuel lisible), « Focus tvOS » (style tvOS sans halo, aucun nom sur les tuiles), « Thème » (verre sombre tvOS 26, flou limité et réduit, bouton du héro sans flou), « Navigation 3 paliers » (la capsule remplace l'engrenage dans les mouvements de page), « Dock d'apps épinglées » (verre sombre, taille fixe, 6 apps au plus, dock plein ; le scénario « tuiles adaptatives » est réécrit pour le maximum de 6), « Panneau Top Shelf au focus » (marge basse égale au nouvel espacement entre rangées), « Fond de la zone grille » (capsule au lieu de l'engrenage), « Contenu du panneau » (Ken Burns en un seul passage), « Diaporama héro » (voiles de lisibilité, bouton, pas d'espace vide), « Préchargement et mémoire » (aucune animation hors écran, rien d'autre que les animations spécifiées au repos), « Écran initial du home » (dégradé du repli en un seul passage).
- `settings` : « Icône réglages flottante » renommée « Capsule heure et réglages » et réécrite ; « Page de réglages » (fond de la grille, pilules de focus, vignette 16:9 des lignes d'apps).
- `ui-testing` : « Couverture de la transition héro ↔ grille » (la capsule remplace l'engrenage dans le scénario de solidarité avec le héro) ; ADDED « Couverture du style tvOS » (nouvelle exigence de tests, sans équivalent existant).

## Dépendances et chevauchements
Deltas écrits sur `main` (`d5579cf`), après l'archivage de `home-settings-polish`, à partir du texte courant de `openspec/specs/`.

**`up-next` (spec sur `main`, implémentation reportée)**
- `launcher-shell` « Navigation 3 paliers » : `up-next` y ajoute la rangée Up Next, ce change y remplace « l'engrenage » par « la capsule ». Le delta de `up-next` est réconcilié dans le même commit que ce change (mêmes mots remplacés, rien d'autre). **Ordre d'archivage** : ce change est archivé avant `up-next` ; si `up-next` devait l'être d'abord, le delta « Navigation 3 paliers » de ce change devra être repris sur le texte archivé de `up-next` avant son propre archivage.
- `settings` « Page de réglages » : `up-next` y ajoute la catégorie « Écran d'accueil », ce change y ajoute le fond et les pilules de focus. Le delta de `up-next` est réconcilié de la même façon (il reprend le fond et les pilules) ; même règle d'ordre d'archivage.
- Les autres exigences touchées par `up-next` (« Rangée Up Next », « Sélection des apps sources », « Apps sources », « Cacher une application », « Sélecteurs stables », « Navigation D-pad des trois zones ») ne sont pas modifiées ici. La carte Up Next renvoie déjà à « Focus tvOS » : elle héritera du nouveau style de focus sans changement de son delta.

**`jellyfin-tvprovider-only`** : sans recouvrement.

## Impact
- `core/designsystem` : jetons de couleurs et de dimensions de la maquette (convertis en dp), `GlassSurface` (verre sombre, mode de performance réduit, bordure, reflet, ombre), `tvFocus` (style tvOS), nouveaux éléments partagés : pilule de focus, contour de verre sans flou.
- `ui/home` : `Dock`, `AppTile`, `HomeGrid`, `HomeScreen` (capsule), `AppContextMenu`, `ShelfPanel` (lecture en phase de dessin) ; `domain/DockLayout.kt` (taille fixe).
- `ui/hero` : `HeroStage` (voiles, bouton, espacement, Ken Burns en pause hors écran), `AmbientGradient` (lecture en phase de dessin).
- `ui/settings` : fond, pilules, `AppleSwitch` (dimensions et couleurs de la maquette).
- Nouvelle source de l'heure (`data/`), exposée par `HomeViewModel` dans un flux séparé de l'état de l'accueil.
- Tests : `DockLayoutTest`, `HideFlowTest`, `CatalogUpdateFocusTest` et `HomeGridScrollTest` adaptés (taille fixe du dock, menu vertical, nouvel espacement), nouveaux tests Compose à la taille TV et tests JUnit.
- `domain/AccentColor.kt` et la couleur dominante calculée pour chaque tuile sont supprimés (ils ne servaient qu'au halo).
- Aucune nouvelle dépendance de code ; une ressource de police (Figtree, 62 712 octets, OFL 1.1) et sa déclaration AboutLibraries (`app/config/`).

## Non-goals
- Mesure de la fluidité sur la TV : faite ensuite sur une pré-release (tâches TV de `tasks.md`).
- Lignes de la maquette qui demandent des données absentes du modèle (« Continuer dans … », saison et épisode, « Reste 25 min », date de masquage sous le nom d'une app cachée) : hors périmètre ; « Continuer dans … », saison et épisode et « Reste 25 min » sont repris par `rc6-polish`, la date de masquage reste à reprendre avec Up Next (p2c) ou un change dédié.

## Décisions de Sygix après la première version de la PR
Dock limité à 6 apps (plus de défilement) ; aucun nom sur les tuiles ; menu : maquette confirmée (repos transparent, pilule claire sans zoom), « Fermer » retiré ; Ken Burns du héro et du panneau en un seul passage puis immobile, reprise au changement d'image ; Retour depuis l'engrenage vers le bouton du héro ; engrenage au trait ; bandeau du mode déplacement flouté. `AGENTS.md` et `openspec/config.yaml` sont alignés (zoom ~1,08, ombre portée douce autorisée, aucun halo coloré).

## Réponses de Sygix aux questions de la deuxième version
Dock plein : action grisée et focusable avec « Dock plein (6 apps maximum) », confirmé ; plus de 6 épinglages existants : rien n'est effacé, les 6 premiers sont affichés, confirmé ; Ken Burns du héro à 10 s, pour au moins 2 s d'image immobile par cycle de 12 s ; dégradé du repli en un seul passage, puis figé ; vignettes des réglages avec la bannière TV de l'app, icône centrée en repli ; police Figtree ajoutée dans cette PR.

## Questions ouvertes
1. **Engrenage au trait** : dessiné par le projet (union du disque et des dents, moyeu), sans reprendre le tracé de la bibliothèque d'icônes de la maquette (licence). À valider à l'œil sur la TV.
