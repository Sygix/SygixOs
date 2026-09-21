# SygixOs — Contexte projet

Launcher Apple TV-style pour Google TV (TCL, Android 14 / Google TV). Remplace le launcher Google (avec pubs).

## Priorités
1. Design fidèle à tvOS (Liquid Glass, focus, animations)
2. Zéro bug : états loading/empty/error partout, pas de crash silencieux

## Stack
- Kotlin 2.4, Jetpack Compose (BOM 2026.09), Haze 2 pour le Liquid Glass
- Coil, DataStore, media3 (ExoPlayer)
- Aucun backend ; tout en local

## Conventions de code
- SOLID, KISS, DRY ; interfaces aux frontières (repos, sources de données), logique hors UI
- Uniflow : ViewModel -> StateFlow -> Compose
- Commentaires : uniquement quand le code ne s'auto-décrit pas, jamais de bloat
- Tests sur la logique métier (fusions, tri, mapping)
- Langue UI : français uniquement (architecture i18n prête)

## Git
- **Conventional Commits** : `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`, etc. (impératif, court)
- **Branches** : jamais de commit direct sur main après l'init du projet — branches `feat/<nom>`, `fix/<nom>`, `chore/<nom>` puis PR (ou merge direct validé par Sygix en attendant les CI checks)
- Commits signés, author cohérent avec le compte GitHub

## Packaging & cible
- Namespace Android : `fr.sygix.<appname>` (ce launcher : `fr.sygix.sygixos`)
- Cible : **Android 14 et plus** (TCL Google TV, minSdk 34, compileSdk 37)
- Dépendances externes autorisées si éprouvées et maintenues (stores, DB locale : Room, etc.) — éviter les libs exotiques

## Workflow des specs
- `openspec/project.md` : contexte, stack, conventions (ce fichier)
- `openspec/specs/<capability>/spec.md` : exigences courantes, format Requirement/Scenario
- `openspec/changes/<change-id>/` : propositions de changement (proposal.md, tasks.md, specs/) avant toute feature non triviale ; archivées une fois implémentées
- Toute évolution de spec passe par une change proposal, jamais d'édition directe de specs/
