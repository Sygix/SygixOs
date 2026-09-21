# Change : focus-shelf

Panneau poster « Top Shelf » inséré entre les rangées de la grille au focus d'une tuile d'app, à la Apple TV.

## Why
La spec Focus tvOS prévoit un fond contextuel « effet Top Shelf » au focus d'une app. Plutôt qu'un simple fond flouté, le poster de l'app focus s'insère dans la grille : immersion forte sans casser la navigation DPAD.

## What Changes
- Dans la grille, au focus d'une tuile : un **panneau plein largeur (40-50% de la hauteur d'écran), coins arrondis, ombre douce** s'insère dans le layout **sous la rangée focusée** ; les rangées du dessous glissent vers le bas (animation), navigation DPAD inchangée (rangée par rangée, le panneau n'est pas focusable)
- Contenu du panneau : **preview programs de l'app focus** (TV Provider système, déjà disponibles) — poster dominant, fondu croisé 300-400ms easing Apple, défilement lent (Ken Burns) entre les posters de l'app
- Quand on est dans la grille : le héro disparaît complètement, le fond est un **dégradé neutre uni type tvOS** (pas d'aerial)
- Si l'app focus ne publie rien : pas de panneau, rangées normales
- Nouveau : `TvProviderHeroSource.posterUrisFor(packageName)` (posters preview/watch next par app, cache mémoire)

## Impact
- specs affectées : launcher-shell (delta ci-dessous, concrétise le scenario « focus » de Focus tvOS)
- UI grille : passage de LazyVerticalGrid à LazyColumn de rangées pour permettre l'insertion
