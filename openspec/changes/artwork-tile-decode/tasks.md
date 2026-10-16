# Tasks

## 1. Taille de décodage

- [x] 1.1 Taille de tuile au zoom de focus en pixels (jetons de la grille × densité) et bornes de décodage plafonnées à 480 × 270 dans le domaine ; vérifié par des tests JUnit (332 × 187 à 960 dp et densité 2, 480 × 270 à densité 4, image plus petite jamais agrandie, ratio conservé).
- [x] 1.2 Source des visuels d'apps décodant dans ces bornes et réservant sa marge de préchargement sur leur taille ; vérifié par un test JUnit avec des bornes réduites.

## 2. Vérification

- [x] 2.1 `./gradlew test` vert ; sur la TV réelle avec l'APK release : bannières nettes au repos et au focus, mémoire des visuels d'apps divisée par deux (`dumpsys meminfo`).

## Workflow follow-up

- Après merge et validation sur la TV : `openspec archive artwork-tile-decode`.
