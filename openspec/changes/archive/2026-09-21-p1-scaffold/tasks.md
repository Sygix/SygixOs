# Tasks : p1-scaffold

## 1. Squelette projet
- [ ] 1.1 Gradle Kotlin DSL, versions catalog, modules app (packages core/data/domain/ui)
- [ ] 1.2 Manifest : LEANBACK_LAUNCHER + LAUNCHER intents, thème sombre, banner
- [ ] 1.3 CI GitHub Actions : assembleDebug à chaque push

## 2. Design system (core/)
- [ ] 2.1 Thème : couleurs sombres, typographie Inter, formes superellipse
- [ ] 2.2 Modificateur focus tvOS : scale 1.1x, ombre douce, animations 250-400ms easing Apple
- [ ] 2.3 Surface Liquid Glass : flou translucide + reflet subtil

## 3. Home (ui/)
- [ ] 3.1 Écran Home : grille d'icônes 6-7 par ligne selon densité, DPAD complet
- [ ] 3.2 Catalogue apps : auto-détection via PackageManager (LEANBACK_LAUNCHER), use case + tri/épinglage persisté DataStore
- [ ] 3.3 Lancement d'app au clic, deep link si disponible
- [ ] 3.4 Menu contextuel long-press : épingler, désépingler, ouvrir, infos
- [ ] 3.5 Fond contextuel : neutre en P1, hook prêt pour le fond par app (P2)

## 4. Qualité
- [ ] 4.1 Tests JUnit : catalogue apps (tri, épinglage, ordre persisté)
- [ ] 4.2 Build debug APK et test visuel sur la TCL (focus, animations, DPAD)
