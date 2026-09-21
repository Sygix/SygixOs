# Change : hero-home

Supersede `dock-navigation` (le dock reste, mais n'est plus l'écran initial).

## Why
Premier écran à la tvOS : un héro plein écran montrant du contenu (posters de recommandations) plutôt que des icônes d'apps. Fallback animé type Aerial Apple (paysages animés) quand aucun contenu n'est disponible. Le dock devient un rail overlay, la grille se découvre en descendant.

## What Changes
- **Écran 1 — Héro** : poster plein écran, carrousel horizontal avec le poster suivant/précédent visible en bord de cadre (peek). Contenu : Jellyfin (Continue watching + récemment ajoutés) — fallback : vidéos aériennes animées en boucle
- Clic sur un poster héro : **ouvre l'app source sans lecture auto** (Jellyfin → écran du contenu, Netflix → page de lancement de lecture, sinon lancement simple de l'app)
- **Palier 2 — Dock** : rail overlay bas semi-transparent, toujours visible sur l'écran héro
- **Palier 3 — Grille** : navigation DPAD héro → dock → grille (grille plein écran, dock masqué)
- Épinglées exclues de la grille (inchangé)
- Architecture : interface `HeroContentProvider` injectée — implémentée par aerial en v1, branchée sur Jellyfin en P2 sans toucher l'UI

## Impact
- specs affectées : launcher-shell (delta ci-dessous, modifie l'état initial défini dans dock-navigation)
