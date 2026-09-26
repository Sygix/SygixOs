# Tasks : p2c-upnext

## Prérequis
- [ ] 1. Change `jellyfin-config` spécifié, implémenté et archivé (URL serveur + auth applicative, compte unique, userId exposé)

## Spécification
- [ ] 2. Relire le delta contre le code réel au moment de l'implémentation (nav home, menu contextuel, patterns DataStore) et corriger la spec avant de coder

## Implémentation
- [ ] 3. Modèle canonique `UpNextItem` + interface `UpNextProvider` (IDs externes dès l'extraction)
- [ ] 4. Provider Jellyfin : `Resume` (épisodes + films) + `Shows/NextUp`, mappeurs vers items canoniques, timeout 10 s
- [ ] 5. Fusion locale : dédoublonnage (en cours gagne), tri par date d'activité décroissante, limite 20
- [ ] 6. Persistance DataStore : ordre de préférence des apps, overrides par série (map ID externe → app)
- [ ] 7. `PlaybackTargetResolver` : score rang × dispo, override prioritaire, écart des apps non installées
- [ ] 8. Adapters : Jellyfin (VIEW intent + extra `source=30`, fallback app), Stremio (deep link `stremio:///detail/…?autoPlay=true`, fallback app), générique (lancement app)
- [ ] 9. Cascade d'échec : fallback app puis app suivante de l'ordre, toast discret si aucune, jamais de crash
- [ ] 10. UI rangée : position sous le héro au-dessus de la grille, squelette loading, carte d'état erreur + « Réessayer », masquée si vide ou non configurée
- [ ] 11. Badge logo app cible sur chaque carte, sans texte, mise à jour sans rechargement
- [ ] 12. Menu contextuel d'override (appui long) : « Toujours ouvrir avec » + « Retirer l'override » conditionnel

## Tests
- [ ] 13. Tests unitaires : fusion, dédup, tri, résolution de cible (score, override, sans IMDb), persistance par exceptions
- [ ] 14. Tests UI Robolectric + Compose : états de rangée, DPAD, menu contextuel, badge, non-blocage de la nav home

## Validation
- [ ] 15. `openspec validate p2c-upnext --type change --strict` vert
- [ ] 16. Build + tests verts : `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=~/android-sdk ./gradlew assembleDebug test`
