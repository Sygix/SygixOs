# Change : ui-testing

Suite de tests UI Compose (Robolectric) pour l'écran home : assertions sémantiques et navigation D-pad simulée, sans capture d'image.

## Why
La suite actuelle couvre uniquement data/domain (9 tests : TvProviderHeroSource, AppCatalog). Aucun test UI — or le cœur d'un launcher TV est le focus et la navigation D-pad entre zones (les régressions de focus corrigées dans fix/tv-debug l'ont montré). Roborazzi a été retiré (génératrices de captures sans assertions) ; ce change rétablit la couverture UI par de vraies assertions, sans dépendance de capture.

## What Changes
- Nouvelle capability « ui-testing » : exigences de couverture UI de l'écran home
- Tests UI Robolectric + compose-ui-test-junit4 (déjà dans le build) couvrant : dock, grille + épinglage, panneau shelf, héro, navigation D-pad des 3 zones
- Aucun refactor d'injection requis : LauncherHome/HomeGrid reçoivent déjà catalogue et HeroState en paramètres — les tests injectent des données déterministes
- testTags stables en production sur les zones (héro, dock, grille, panneau shelf), le menu contextuel et les tuiles d'app pour des sélecteurs robustes
- Pas de Roborazzi : assertions sémantiques uniquement ; visual regression et tests d'animation/alpha reportés à un change ultérieur

## Impact
- specs affectées : aucune capability existante ; nouvelle capability « ui-testing » (delta ci-dessous)
- code : testTags additifs sur HomeScreen/HomeGrid/Dock/HeroStage/ShelfPanel/AppContextMenu, app/src/test/…/ui/ (nouvelles classes de test)
- CI : rien à changer — ./gradlew test exécute déjà les tests unitaires Robolectric
- dépendances : aucune ajoutée (robolectric, compose-ui-test-junit4, androidx-test déjà présents)
