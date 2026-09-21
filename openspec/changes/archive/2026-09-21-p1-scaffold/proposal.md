# Change : p1-scaffold

## Why
Phase 1 de la roadmap : poser le squelette Gradle et le design system tvOS, livrer un premier APK installable pour valider le look sur la TCL avant toute intégration.

## What Changes
- Squelette Gradle Kotlin (Compose for TV, minSdk 34, structure core/data/domain/ui)
- Design system Compose : thème sombre, tuiles superellipse, gestion du focus (zoom 1.1x, ombre, easing Apple, 250-400ms), surfaces Liquid Glass
- Home : grille d'apps auto-détectée (LEANBACK_LAUNCHER), navigation DPAD, menu contextuel long-press
- Persistance de l'ordre/épinglage des apps (DataStore)
- CI GitHub Actions : build Gradle à chaque push
- Pas de contenu réseau, pas de rangée Up Next (P2/P4), pas de screensaver (P3)

## Impact
- Specs affectées : launcher-shell (ajouts focus + grille + persistance déjà couverts en partie ; deltas : épinglage persisté, fond contextuel neutre en v1)
- Aucune dépendance externe au-delà des libs Android standard
