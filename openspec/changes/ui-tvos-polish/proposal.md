# Change : ui-tvos-polish

## Why
Le test de la pré-release v0.0.1-rc.4 sur la TV de test a relevé des défauts visuels et de fluidité qui éloignent le launcher de sa référence, tvOS 26 (Liquid Glass) : dock en verre clair à la couleur peu Apple, halo de focus coloré qui déborde (et, dans les réglages, coupé en rectangle), texte blanc illisible sur le verre clair du menu contextuel, texte du héro illisible sur un poster clair, espace vide sous le titre du héro sans progression, engrenage seul peu lisible. Surtout, le héro au repos tourne à environ 32 images/s avec la moitié des images en retard (95 % quand le dock a le focus), et le goulot est le GPU : le flou du verre recalculé sur le fond animé du héro. Sygix a validé une maquette 1920×1080 et une liste de décisions ; ce change les traduit en exigences.

## What Changes
- **Thème et verre** : thème toujours sombre ; matériau Liquid Glass façon tvOS 26 : teinte sombre transparente, fine bordure claire, fin reflet en haut, ombre légère. Le flou reste calculé en direct mais sur une image réduite, seulement sous les surfaces qui en ont besoin (dock, capsule, menu contextuel). Le bouton du héro n'est plus une surface floutée : au repos, fond sombre translucide et fine bordure façon verre ; au focus, fond blanc et texte noir, léger zoom et ombre. L'ancienne règle « aucune ombre noire, relief par des halos clairs ou colorés » est retirée.
- **Focus des tuiles (grille et dock)** : zoom d'environ 1,08 avec une légère montée, ombre portée douce, reflet blanc en diagonale, aucun halo ; rien ne déborde sur les tuiles voisines (l'espacement de la grille passe aux valeurs de la maquette pour absorber le zoom) ni n'est coupé par le conteneur. Le nom de l'app s'affiche sous la tuile focusée de la grille. Les marges de défilement de la grille suivent le nouvel espacement entre rangées.
- **Dock** : verre sombre ; tuiles de taille fixe, plus petites que celles de la grille (environ 80 %), quel que soit le nombre d'apps ; dock centré, ajusté au nombre d'apps.
- **Capsule heure et réglages** (remplace l'engrenage seul, exigence renommée) : capsule en verre en haut à droite du héro, avec l'heure (format 12 ou 24 h du système, non focusable, mise à jour à la minute sans recomposer l'écran) et l'engrenage, seul élément focusable (pastille blanche, icône noire au focus). Elle reste solidaire du héro.
- **Héro** : voiles en dégradé (bas, gauche, léger voile radial en haut à droite derrière la capsule) pour la lisibilité sur un poster clair ; plus d'espace vide entre les métadonnées et le bouton quand la progression est absente.
- **Menu contextuel** : panneau en verre plus foncé sur un voile sombre, actions en liste verticale, focus en pilule claire, lisible quelles que soient les tuiles derrière.
- **Réglages** : même fond que la grille ; focus des lignes (catégories, Apps sources, Applications cachées, « Tout réactiver », À propos) en pilule claire à texte sombre, sans zoom, sans halo, sans verre ; catégorie sélectionnée en pilule grise discrète quand le focus est ailleurs.
- **Fluidité** : aucune animation ne tourne pour un élément hors écran (Ken Burns du héro mis en pause quand le héro sort), plus aucune recomposition par image pour les animations continues (lecture des valeurs animées en phase de dessin), plus de second flou sur le héro.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `launcher-shell` : « Grille d'apps » (menu contextuel lisible), « Focus tvOS » (style tvOS sans halo, nom sous la tuile), « Thème » (verre sombre tvOS 26, flou limité et réduit, bouton du héro sans flou), « Navigation 3 paliers » (la capsule remplace l'engrenage dans les mouvements de page), « Dock d'apps épinglées » (verre sombre, taille fixe ; le scénario « tuiles adaptatives » disparaît), « Panneau Top Shelf au focus » (marge basse égale au nouvel espacement entre rangées), « Fond de la zone grille » (capsule au lieu de l'engrenage), « Diaporama héro » (voiles de lisibilité, bouton, pas d'espace vide), « Préchargement et mémoire » (aucune animation hors écran, rien d'autre que les animations spécifiées au repos).
- `settings` : « Icône réglages flottante » renommée « Capsule heure et réglages » et réécrite ; « Page de réglages » (fond de la grille, pilules de focus).
- `ui-testing` : « Couverture de la transition héro ↔ grille » (la capsule remplace l'engrenage dans le scénario de solidarité avec le héro) ; ADDED « Couverture du style tvOS » (nouvelle exigence de tests, sans équivalent existant).

## Dépendances et chevauchements
Deltas écrits sur `main` (`d5579cf`), après l'archivage de `home-settings-polish`, à partir du texte courant de `openspec/specs/`.

**`p2c-upnext` (spec sur `main`, implémentation reportée)**
- `launcher-shell` « Navigation 3 paliers » : `p2c-upnext` y ajoute la rangée Up Next, ce change y remplace « l'engrenage » par « la capsule ». Le delta de `p2c-upnext` est réconcilié dans le même commit que ce change (mêmes mots remplacés, rien d'autre). **Ordre d'archivage** : ce change est archivé avant `p2c-upnext` ; si `p2c-upnext` devait l'être d'abord, le delta « Navigation 3 paliers » de ce change devra être repris sur le texte archivé de `p2c-upnext` avant son propre archivage.
- `settings` « Page de réglages » : `p2c-upnext` y ajoute la catégorie « Écran d'accueil », ce change y ajoute le fond et les pilules de focus. Le delta de `p2c-upnext` est réconcilié de la même façon (il reprend le fond et les pilules) ; même règle d'ordre d'archivage.
- Les autres exigences touchées par `p2c-upnext` (« Rangée Up Next », « Sélection des apps sources », « Apps sources », « Cacher une application », « Sélecteurs stables », « Navigation D-pad des trois zones ») ne sont pas modifiées ici. La carte Up Next renvoie déjà à « Focus tvOS » : elle héritera du nouveau style de focus sans changement de son delta.

**`jellyfin-tvprovider-only`** : sans recouvrement.

## Impact
- `core/designsystem` : jetons de couleurs et de dimensions de la maquette (convertis en dp), `GlassSurface` (verre sombre, mode de performance réduit, bordure, reflet, ombre), `tvFocus` (style tvOS), nouveaux éléments partagés : pilule de focus, contour de verre sans flou.
- `ui/home` : `Dock`, `AppTile`, `HomeGrid`, `HomeScreen` (capsule), `AppContextMenu`, `ShelfPanel` (lecture en phase de dessin) ; `domain/DockLayout.kt` (taille fixe).
- `ui/hero` : `HeroStage` (voiles, bouton, espacement, Ken Burns en pause hors écran), `AmbientGradient` (lecture en phase de dessin).
- `ui/settings` : fond, pilules, `AppleSwitch` (dimensions et couleurs de la maquette).
- Nouvelle source de l'heure (`data/`), exposée par `HomeViewModel` dans un flux séparé de l'état de l'accueil.
- Tests : `DockLayoutTest`, `HideFlowTest` et `CatalogUpdateFocusTest` adaptés (taille fixe du dock, menu vertical), nouveaux tests Compose à la taille TV et tests JUnit.
- Aucune nouvelle dépendance.

## Non-goals
- Mesure de la fluidité sur la TV : faite ensuite sur une pré-release (tâches TV de `tasks.md`).
- Police de la maquette (Figtree) : la police système reste ; à reconsidérer dans un change de typographie.
- Lignes de la maquette qui demandent des données absentes du modèle (« Continuer dans … », saison et épisode, « Reste 25 min », date de masquage sous le nom d'une app cachée) : hors périmètre, à reprendre avec Up Next (p2c) ou un change dédié.
- Révision d'`AGENTS.md` et du contexte de `openspec/config.yaml` (« aucune ombre noire », « halos clairs ou colorés », « zoom ~1.1x ») : à faire par Sygix, voir Questions ouvertes.

## Questions ouvertes
1. **Dock au-delà de sa capacité** : à la taille fixe de la maquette, le dock tient 6 apps sur la largeur de l'écran. Au-delà, faut-il faire défiler le dock, limiter l'épinglage, ou autre ? Le code fait provisoirement défiler la rangée pour garder la tuile focusée entière, sans que ce soit une exigence.
2. **Nom de l'app sous la tuile focusée du dock** : la maquette ne le montre que pour la grille ; sous le dock, il sortirait du verre. Il n'est affiché que dans la grille pour l'instant.
3. **Boutons du menu contextuel** : la décision 6 demande au repos un fond sombre translucide avec bordure et, au focus, un léger zoom ; la maquette du menu montre des actions transparentes au repos et une pilule claire sans zoom au focus (décision 10). La maquette est suivie ; à confirmer.
4. **Action « Fermer » du menu contextuel** : absente de la maquette, conservée en fin de liste (Retour ferme aussi le menu). La retirer ?
5. **Ken Burns du héro et du panneau Top Shelf** : spécifiés (« Diaporama héro », « Contenu du panneau »), ce sont, avec la vidéo d'aperçu et le dégradé animé du repli, les seuls redessins continus restants au repos. Le Ken Burns du héro oblige à recalculer le flou du dock et de la capsule à chaque image. Les garder tels quels, les arrêter au repos après un passage, ou les retirer ?
6. **Retour depuis l'engrenage** : aujourd'hui sans effet (comportement hérité). Le rendre au héro ?
7. **Icône de l'engrenage** : la maquette dessine un engrenage au trait ; l'engrenage plein actuel est conservé (même rôle, aucune ressource tierce). À confirmer.
8. **`AGENTS.md` et `openspec/config.yaml`** contiennent encore « aucune ombre noire, relief par des halos clairs ou colorés » et « zoom ~1.1x », contraires aux décisions 2 et 4. À mettre à jour par Sygix.
