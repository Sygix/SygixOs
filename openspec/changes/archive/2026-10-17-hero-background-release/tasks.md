# Tasks

## 1. Lecteur du héro

- [x] 1.1 Lecteur libéré seulement à l'arrêt de l'activité ou au démontage du héro, lecture interrompue sans libération quand le héro est masqué ; vérifié par un test Compose (aucune libération en zone grille, une libération en arrière-plan, vidéo rechargée et lue au retour).
- [x] 1.2 Défilement automatique et délais du héro suspendus hors premier plan ; vérifié par un test Compose (le diaporama n'avance pas en arrière-plan, puis avance au retour).
- [x] 1.3 Vidéo lente dont la préparation est interrompue rendue éligible ; vérifié par un test Compose.

## 2. Vérification

- [ ] 2.1 `./gradlew test` vert ; sur la TV réelle avec l'APK release : descente vers la grille sans saccade, lecture d'une autre app sans conflit de décodeur pendant que le héro avait une vidéo, vidéo du héro relancée au retour.

## Workflow follow-up

- Après merge et validation sur la TV : `openspec archive hero-background-release`.
