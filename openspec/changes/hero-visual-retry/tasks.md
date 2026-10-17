# Tasks

## 1. Visuels en échec

- [x] 1.1 Vérification d'un visuel distinguant un échec réseau (erreur d'entrée-sortie, HTTP 5xx) d'un rejet définitif (trop étroit, HTTP 4xx, décodage) ; vérifié par des tests JUnit (seuls les échecs réseau restent à retenter, l'annulation se propage sans résultat).
- [x] 1.2 Rafraîchissement du héro qui annule le chargement précédent et revérifie les visuels injoignables, jamais les visuels rejetés ; vérifié par des tests JUnit.

## 2. Top Shelf

- [x] 2.1 Vérification des affiches portant sur les affiches candidates de l'app : reprise après interruption, nouvelles affiches candidates vérifiées sans attendre une vérification obsolète ; vérifié par des tests JUnit.

## 3. Vérification

- [ ] 3.1 `./gradlew test` vert ; sur la TV réelle avec l'APK release : un visuel servi par un serveur coupé puis rétabli réapparaît après un retour à l'accueil.

## Workflow follow-up

- Après merge et validation sur la TV : `openspec archive hero-visual-retry`.
