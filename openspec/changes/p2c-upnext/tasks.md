# Tasks : p2c-upnext

## Prérequis
- [ ] 1. Changes `jellyfin-tvprovider-only` et `ui-testing` fusionnés/archivés (jellyfin-integration en contrat de données TV Provider ; capability ui-testing dans les specs courantes) — réalisés dans cette PR

## Vérification sur l'appareil (avant tout code)
- [ ] 2. Test sur la TCL : lister les lignes `WatchNextPrograms` lisibles sous `READ_TV_LISTINGS` — packages visibles, colonnes réellement remplies par chaque app (titre de série, `episode_title`, saison/épisode, `internal_provider_id`, `content_id`, `intent_uri`, poster, `watch_next_type`, `last_engagement_time`, position, année, `COLUMN_TYPE`, durée, `COLUMN_BROWSABLE`). Ajuster les hypothèses de `design.md` avec les résultats avant d'écrire le moindre code. **Critère de sortie** : si seules nos propres lignes sont visibles, le change est suspendu et revu.

## Implémentation
- [ ] 3. Modèle canonique `UpNextItem` (champ `externalIds` facultatif + liste des sources du contenu conservée après fusion) + interface `UpNextSource` renvoyant un `Result` (erreur ≠ vide)
- [ ] 4. Source TV Provider : requête `ContentResolver` sur `Dispatchers.IO` avec `withTimeout` ~2 s, filtre `COLUMN_BROWSABLE`, exclusion des `PreviewPrograms` et des `COLUMN_TYPE` non épisode/film, mappeurs `Cursor` → `UpNextItem` pour toutes les apps visibles
- [ ] 5. Normalisation des titres + dédoublonnage en niveaux 1 à 4 (niveau 5 = P4, via `externalIds`), gagnant : `CONTINUE` > engagement le plus récent > ordre de préférence (constante)
- [ ] 6. Tri : `CONTINUE` d'abord par `last_engagement_time` décroissant, puis `NEXT`/`NEW` (même tri, à égalité par `_ID` croissant), puis `WATCHLIST`, tri stable, limite 20
- [ ] 7. Intégration `HomeViewModel`/`HomeState` : état de la rangée dans le uniflow existant, déclencheurs de refresh (retour au premier plan, permission accordée) + `ContentObserver` sur `WatchNextPrograms.CONTENT_URI`, implémenté et protégé contre `SecurityException` et provider absent
- [ ] 8. UI rangée : ligne en tête de la zone grille (position avant/après la grille pilotée par le réglage), carte 16:9 (image plein cadre sinon poster portrait centré), texte `SxxEyy` + titre d'épisode, barre de progression `CONTINUE`, placeholder image, badge icône app via `PackageManager`, squelette au premier chargement > 300 ms seulement
- [ ] 9. Ouverture : intent publié (`COLUMN_INTENT_URI`) puis lancement de l'app source en repli (+ toast en cas d'échec depuis le menu), sans crash
- [ ] 10. Généralisation du menu contextuel (`AppContextMenu` découplé de `TvApp`) : « Ouvrir avec… » sur une carte, aucune persistance
- [ ] 11. Réglage « Position d'Up Next » (delta ADDED `settings` de ce change) : contrôle avant la grille (défaut) / après la grille, persistance DataStore, navigation D-pad du cas « après la grille » — implémenté dans p2c, `p2b-settings` en dépendance

## Tests
- [ ] 12. Tests JUnit : normalisation, chaque niveau de dédoublonnage (y compris faux positifs : remake avec année différente, même titre mais types différents), gagnant, tri, limite, mapping `Cursor` → `UpNextItem`
- [ ] 13. Tests UI Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`, jamais la taille Robolectric par défaut) : navigation D-pad héro → dock → Up Next → apps (descente et montée), palier sauté, restauration du focus au retour d'une app, rangée devenue vide, menu « Ouvrir avec… », états de la rangée ; testTags `zone-upnext`, `upnext-card-<key>`, `upnext-menu`

## Finition
- [ ] 14. Chaînes FR en ressources (aucun texte en dur)
- [ ] 15. Mise à jour du README si le comportement visible change
- [ ] 16. Build et tests verts : `./gradlew test assembleRelease` (aucune configuration propre à une machine) + validation sur la TV réelle en `assembleRelease`
- [ ] 17. `openspec validate --all --strict` vert, puis `openspec archive p2c-upnext` après merge sur main, test sur la TCL (tâche 2) et validation sur la TV
