# AGENTS.md — instructions pour agents IA

## Vue d'ensemble
SygixOs : launcher Android TV natif (Kotlin, Jetpack Compose) au design tvOS pour remplacer le launcher Google TV. Propriétaire : Sygix. Langue UI : français.

## Workflow de spécification (OpenSpec)
- Contexte, conventions et règles de rédaction : `openspec/config.yaml` (`context`, `rules` par document, `operations`), injectés par `openspec instructions` — à lire AVANT tout travail
- Passer par les skills OpenSpec (`openspec-propose`, `openspec-apply-change`, `openspec-verify-change`, `openspec-archive-change`…) : ils appellent `openspec instructions`, qui applique les règles du repo ; ne pas rédiger les documents d'un change à la main sans eux
- Exigences courantes par capability : `openspec/specs/<capability>/spec.md`
- Toute feature ou changement de comportement : créer d'abord `openspec/changes/<id>/proposal.md` (+ `tasks.md` et deltas dans `specs/`), le faire valider, puis implémenter, puis archiver dans `changes/archive/`
- Ne jamais éditer `openspec/specs/` directement ; les specs courantes évoluent uniquement via des changes implémentés
- Avant d'ouvrir ou de mettre à jour une PR de feature : `openspec-verify-change` sur le change et `./gradlew test` ; ne cocher une tâche ou déclarer un point de review traité qu'après vérification
- Après merge et validation sur la TV : `openspec archive <id>`

## Conventions de code (non négociables)
- SOLID, KISS, DRY : petites classes à responsabilité unique, interfaces aux frontières (repos, sources de données), zéro logique dans les composables
- State uniflow : ViewModel → StateFlow → Compose ; pas d'état dupliqué
- Commentaires : uniquement quand le code ne s'auto-décrit pas. Pas de commentaires narratifs ni de docstrings décoratives
- Tous les appels réseau : états loading/empty/error en UI, timeout borné, jamais de crash silencieux
- Tests JUnit sur la logique métier (fusions Up Next, tri, mapping) ; UI validée sur la TV réelle
- Pas de backend : tout en local (Coil, DataStore, media3)

## Design (référence : tvOS)
- Thème sombre uniquement en v1, noir pur, posters plein cadre
- Focus : zoom ~1.1x, ombre douce, animations 250-400ms courbes Apple, jamais de saut sec
- Liquid Glass : matériau verre réfractant (Haze 2), aucune ombre noire — le relief vient de halos clairs ou colorés
- Grille d'apps auto-détectée (LEANBACK_LAUNCHER), tuiles 16:9 (bannière Android TV)

## Git & packaging
- Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`...) ; branches `feat/`, `fix/`, `chore/` — pas de commit direct sur main après l'init du projet
- Commits signés et author cohérent avec le compte GitHub
- Namespace Android : `fr.sygix.<appname>` — launcher : `fr.sygix.sygixos`
- Cible : Android 14+ (minSdk 34, compileSdk 37)
- Dépendances externes : uniquement des libs éprouvées/maintenues (Room, Retrofit, Coil...)

## Environnement
- Messages de commit en anglais, impératif court
- Jamais de contenu spécifique à l'environnement dans le repo : secrets, clés, chemins locaux, config machine — tout va dans .gitignore / local.properties
- Repo : github.com/Sygix/SygixOs
- Cible : TCL Google TV, Android 14 ; juger la fluidité sur `assembleRelease` (le build debug est interprété et saccade)
- ADB : `adb connect <IP-TV>` pour installer et tester ; commandes P5 dans le README

## Outils
- OpenSpec CLI (`openspec`, v1.13+, installé via `pnpm add -g @fission-ai/openspec`) : valider avec `openspec validate --all --strict`, archiver un change avec `openspec archive <id>` ; pnpm (v12) obligatoire à la place de npm pour tout paquet Node
- Format strict des specs : `### Requirement:` (SHALL/MUST), `#### Scenario:` avec `- **WHEN** / **THEN**`
