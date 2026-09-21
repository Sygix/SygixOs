# Tasks : jellyfin-hero

- [ ] 1. Change proposal + delta spec validés
- [ ] 2. `TvProviderHeroSource` : query channels/preview_programs, filtre package Jellyfin, tests avec provider mocké
- [ ] 3. Mapping programme → `HeroItem` (poster/thumbnail fallback, titre, progression, intent) + tests
- [ ] 4. `JellyfinHeroProvider` : fusion déterministe continue-watching + récents, ordre reprise→récent, test
- [ ] 5. Branchement injection : Jellyfin d'abord, aerial en fallback (chaîne de providers)
- [ ] 6. Coil : chargement posters dans `HeroCarousel`, placeholder dégradé si échec
- [ ] 7. Clic : intent du programme, fallback lancement app
- [ ] 8. Screenshot Roborazzi héro avec programmes mockés + build/test verts
