# Tasks : jellyfin-tvprovider-only

## Implémentation
- [ ] 1. Supprimer le code de configuration Jellyfin (écrans, credentials, stockage) et tout appel réseau au serveur — plus aucune trace dans le code ni les chaînes
- [ ] 2. Consommer les programmes Jellyfin uniquement via le TV Provider (`WatchNextPrograms`) sous `READ_TV_LISTINGS` : source renvoyant un `Result` (erreur ≠ vide), requête protégée contre `SecurityException` et provider absent, `withTimeout` borné sur `Dispatchers.IO`
- [ ] 3. Tests : mapping `Cursor` → modèle, états provider vide / indisponible / permission refusée

## Finition
- [ ] 4. Chaînes FR en ressources ; mise à jour du README si le comportement visible change
- [ ] 5. `./gradlew test assembleRelease` verts, `openspec validate --all --strict` vert, puis validation sur la TV réelle
