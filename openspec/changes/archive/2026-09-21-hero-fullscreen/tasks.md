# Tasks : hero-fullscreen

- [x] 1. Change proposal + delta spec validés par Sygix
- [x] 2. Archiver `focus-shelf` (`openspec archive`), commit chore
- [x] 3. TV Provider : permission `READ_TV_LISTINGS` (manifest + demande runtime + rechargement à l'octroi), requêtes sans `selection`, filtrage client dans `posterUrisFor`, `previewVideoUri` + package lus sur les programmes ; tests JUnit (mapping, filtrage)
- [x] 4. `HomeViewModel` : état unique (catalog + héro loading/ready/empty), `refreshHero()`, plus de chargement dans les composables
- [x] 5. Héro plein écran : `HeroStage` (diaporama, fondu croisé, Ken Burns, avance auto 8 s, gauche/droite, OK, métadonnées, vidéo d'aperçu → poster → dégradé), un seul lecteur, pause en zone grille, libération au démontage
- [x] 6. Fallback : `NatureVideoProvider` (URLs Pexels vérifiées, 2K sinon 1080p), playlist ExoPlayer en boucle, clip fautif ignoré, dégradé animé si rien ne joue ; suppression des URLs Apple
- [x] 7. Navigation : machine d'états zone, `focusProperties { canFocus }` par couche, transitions bas/haut explicites, focus initial déterministe, restauration dernière tuile, haut depuis la première rangée → dock/héro
- [x] 8. Tuiles 16:9 : bannière `android:banner` via PackageManager, repli icône entière centrée ; grille 5 colonnes
- [x] 9. Network security config (cleartext autorisé), retrait de `READ_EPG_DATA`
- [x] 10. `assembleDebug test` verts, test sur TV via adb (logcat sans erreur, navigation complète), README + spec synchronisés
