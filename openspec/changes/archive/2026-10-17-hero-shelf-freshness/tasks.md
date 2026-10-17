# Tasks

## 1. Programmes publiés

- [x] 1.1 Observation du TV Provider partagée entre le héro, le Top Shelf et la rangée Up Next ; le héro relit le provider à chaque signal et toutes les 15 minutes tant que l'accueil est au premier plan ; vérifié par des tests JUnit à temps virtuel (relecture au signal, au délai, aucune hors premier plan, aucune republication sans changement).
- [x] 1.2 Programme retiré ou modifié : disparition du héro et du Top Shelf, textes et visuel mis à jour, ensembles de visuels vérifiés purgés ; vérifié par des tests JUnit et un test Compose du héro dont le programme affiché est retiré.

## 2. Fraîcheur des images

- [x] 2.1 Intercepteur Coil : une entrée du cache disque de plus d'un jour est relue depuis le réseau, la version en cache servant de repli ; vérifié par des tests JUnit (entrée récente servie, entrée ancienne relue et mémoire invalidée, repli sur échec réseau).

## 3. Vérification

- [ ] 3.1 `./gradlew test` vert ; sur la TV réelle avec l'APK release : publier, retirer puis modifier un programme pendant que l'accueil est affiché et pendant qu'une autre app est au premier plan.

## Workflow follow-up

- Après merge et validation sur la TV, une fois archivés les changes dont il dépend (proposal) : `openspec archive hero-shelf-freshness`.
