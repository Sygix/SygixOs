# Tasks

## 1. Vérification par en-tête

- [x] 1.1 Vérification d'un visuel d'après les dimensions de son en-tête, sans bitmap, en conservant l'échec transitoire (réseau, lecture) ou définitif ; plus de visuel gardé en mémoire ; vérifié par des tests JUnit (aucun résultat bitmap, erreur de lecture rejouable, annulation propagée).
- [x] 1.2 Largeur mesurée sans agrandissement ; vérifié par un test JUnit (480 × 270 et 900 × 1200 écartés, 640 × 360, 1920 × 1080 et 2400 × 1350 acceptés).
- [x] 1.3 Variante `maxresdefault` des miniatures YouTube vérifiée avant l'image publiée et affichée par le héro et le Top Shelf ; vérifié par des tests JUnit (formes d'URL publiées, variante acceptée affichée, variante absente puis repli, échec réseau à revérifier, autre domaine inchangé).

## 2. Vérification

- [ ] 2.1 `./gradlew test` vert ; sur la TV réelle avec l'APK release : le héro et le Top Shelf n'affichent que des images paysage d'au moins 640 px de large, les programmes YouTube gardent un visuel (variante pleine résolution), et un visuel en échec réseau réapparaît après le retour du réseau.

## Workflow follow-up

- Après merge et validation sur la TV, une fois archivés les changes dont il dépend (proposal) : `openspec archive visual-header-validation`.
