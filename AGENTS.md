# AGENTS.md — instructions pour agents IA

## Vue d'ensemble
SygixOs : launcher Android TV natif (Kotlin, Jetpack Compose for TV) au design tvOS pour remplacer le launcher Google TV. Propriétaire : Sygix. Langue UI : français.

## Workflow de spécification (OpenSpec)
- Contexte et conventions : `openspec/project.md` — à lire AVANT tout travail
- Exigences courantes par capability : `openspec/specs/<capability>/spec.md`
- Toute feature ou changement de comportement : créer d'abord `openspec/changes/<id>/proposal.md` (+ `tasks.md` et deltas dans `specs/`), le faire valider, puis implémenter, puis archiver dans `changes/archive/`
- Ne jamais éditer `openspec/specs/` directement ; les specs courantes évoluent uniquement via des changes implémentés
- Après implémentation d'un change : mettre à jour `tasks.md`, puis déplacer le dossier dans `openspec/changes/archive/`

## Conventions de code (non négociables)
- SOLID, KISS, DRY : petites classes à responsabilité unique, interfaces aux frontières (repos, sources de données), zéro logique dans les composables
- State uniflow : ViewModel → StateFlow → Compose ; pas d'état dupliqué
- Commentaires : uniquement quand le code ne s'auto-décrit pas. Pas de commentaires narratifs ni de docstrings décoratives
- Tous les appels réseau : états loading/empty/error en UI, timeout borné, jamais de crash silencieux
- Tests JUnit sur la logique métier (fusions Up Next, tri, mapping) ; UI validée sur la TV réelle
- Pas de backend : tout en local (Retrofit/OkHttp, Coil, DataStore)

## Design (référence : tvOS)
- Thème sombre uniquement en v1, noir pur, posters plein cadre
- Focus : zoom ~1.1x, ombre douce, animations 250-400ms courbes Apple, jamais de saut sec
- Liquid Glass : surfaces translucides floutées
- Grille d'apps auto-détectée (LEANBACK_LAUNCHER), rangée Up Next en haut

## Git & packaging
- Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`...) ; branches `feat/`, `fix/`, `chore/` — pas de commit direct sur main après l'init du projet
- Commits signés GPG (config global en place) ; author Sygix + noreply GitHub
- Namespace Android : `fr.sygix.<appname>` — launcher : `fr.sygix.sygixos`
- Cible : Android 14+ (minSdk 34)
- Dépendances externes : uniquement des libs éprouvées/maintenues (Room, Retrofit, Coil...)

## Environnement
- Repo : github.com/Sygix/SygixOs (SSH). Commits en anglais, messages impératifs courts
- Cible : TCL Google TV, Android 14 (min SDK 30+)
- ADB : `adb connect <IP-TV>` pour installer et tester ; commandes P5 dans le README

## Outils
- OpenSpec CLI (`openspec`, v1.13+, installé via `pnpm add -g @fission-ai/openspec`) : valider avec `openspec validate --all --strict`, archiver un change avec `openspec archive <id>` ; pnpm (v12) obligatoire à la place de npm pour tout paquet Node
- Format strict des specs : `### Requirement:` (SHALL/MUST), `#### Scenario:` avec `- **WHEN** / **THEN**`
