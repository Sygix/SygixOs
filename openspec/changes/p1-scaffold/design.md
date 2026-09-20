# Design : p1-scaffold

## Décisions
- **Un seul module app** avec packages core/data/domain/ui (KISS ; split en modules seulement si ça grossit)
- **Focus** : un unique Modificateur Compose `tvFocus()` centralisé dans core/ — scale + shadow animés avec `animateFloatAsState`, courbes `FastOutSlowInEasing`/custom Apple ; toute la grille réutilise le même modificateur (DRY)
- **Fond contextuel** : interface `HomeBackdropProvider` injectée — en P1 retourne un fond neutre ; P2 branchera les recommandations sans toucher l'UI (DIP)
- **Persistance** : DataStore Preferences (ordre = liste d'IDs, épinglés en tête) ; mapping isolé dans domain/
- **Grille** : LazyVerticalGrid avec `focusProperties` soignés pour un déplacement DPAD sans saut ; pas de pagination en v1
- Pas de Hilt en P1 (KISS) : factories manuelles, DI ajoutée en P2 quand les dépendances réseau arrivent
