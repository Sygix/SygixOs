# Tasks : p2c-upnext

## Prérequis
- [ ] 1. Change `jellyfin-tvprovider-only` fusionné (jellyfin-integration réécrite en TV Provider uniquement)

## Vérification sur l'appareil (avant tout code)
- [ ] 2. Test sur la TCL : lister les lignes `WatchNextPrograms` et `PreviewPrograms` lisibles sous `READ_TV_LISTINGS` — packages visibles, colonnes réellement remplies par chaque app (titre de série, `episode_title`, saison/épisode, `internal_provider_id`, `content_id`, `intent_uri`, poster, `watch_next_type`, `last_engagement_time`, position). Ajuster les hypothèses de `design.md` avec les résultats avant d'écrire le moindre code.

## Implémentation
- [ ] 3. Modèle canonique `UpNextItem` (avec champ `externalIds` facultatif) + interface `UpNextSource` renvoyant un `Result` (erreur ≠ vide)
- [ ] 4. Source TV Provider : requête `ContentResolver` sur `Dispatchers.IO` avec `withTimeout` ~2 s, mappeurs `Cursor` → `UpNextItem` pour toutes les apps visibles
- [ ] 5. Normalisation des titres + dédoublonnage en niveaux 1 à 4 (niveau 5 = P4, via `externalIds`), gagnant : reprise en cours > engagement le plus récent > ordre de préférence (constante)
- [ ] 6. Tri : `CONTINUE` d'abord par `last_engagement_time` décroissant, puis `NEXT`/`NEW` (même tri, à égalité par `_ID` croissant), tri stable, `WATCHLIST` exclu, limite 20
- [ ] 7. Intégration `HomeViewModel`/`HomeState` : état de la rangée dans le uniflow existant, déclencheurs de refresh (retour au premier plan, permission accordée), étude d'un `ContentObserver` protégé contre `SecurityException` et provider absent
- [ ] 8. UI rangée : ligne en tête de la zone grille (position avant/après la grille pilotée par le réglage p2b), carte 16:9 avec poster portrait centré, texte `SxxEyy` + titre d'épisode, barre de progression `CONTINUE`, placeholder image, badge icône app via `PackageManager`, squelette au premier chargement > 300 ms seulement
- [ ] 9. Ouverture : intent publié (`COLUMN_INTENT_URI`) puis lancement de l'app source en repli, sans crash
- [ ] 10. Généralisation du menu contextuel (`AppContextMenu` découplé de `TvApp`) : « Ouvrir avec… » sur une carte, aucune persistance
- [ ] 11. Réglage « Position d'Up Next » côté change `p2b-settings` (avant la grille, défaut / après la grille) — coordonner avec la PR settings

## Tests
- [ ] 12. Tests JUnit : normalisation, chaque niveau de dédoublonnage (y compris faux positifs : remake avec année différente, même titre mais types différents), gagnant, tri, limite, mapping `Cursor` → `UpNextItem`
- [ ] 13. Tests UI Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`, jamais la taille Robolectric par défaut) : navigation D-pad héro → dock → Up Next → apps, palier sauté, restauration du focus au retour d'une app, menu « Ouvrir avec… », états de la rangée ; testTags `zone-upnext`, `upnext-card-<key>`, `upnext-menu`

## Finition
- [ ] 14. Chaînes FR en ressources (aucun texte en dur)
- [ ] 15. Mise à jour du README si le comportement visible change
- [ ] 16. Build et tests verts : `./gradlew test assembleRelease` (aucune configuration propre à une machine) + validation sur la TV réelle en `assembleRelease`
- [ ] 17. `openspec validate --all --strict` vert, puis `openspec archive p2c-upnext` après merge sur main et validation sur la TV
