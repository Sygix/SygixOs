# AGENTS.md — instructions pour agents IA

## Vue d'ensemble
SygixOs : launcher Android TV natif (Kotlin, Jetpack Compose) au design tvOS pour remplacer le launcher Google TV. Propriétaire : Sygix. Langue UI : français.

## Feuille de route
- P3 — Logo et écran de démarrage animé : livré (v0.0.1) ; cadence de la mascotte en suivi
- P4 — Mises à jour intégrées depuis les releases GitHub : livré (v0.0.1) ; relance au premier plan bloquée par le constructeur, reprise manuelle, résolution différée à P5
- P5 — Remplacement du launcher système : livré (v0.1.0-rc.2), archivé (`archive/2026-10-11-p5-real-launcher`) ; ouverture à l'allumage non vérifiable sur la TV de référence (gestionnaire d'auto-démarrage du constructeur), relance après mise à jour toujours en suivi (backlog, sujet 9) ; procédure ADB manuelle conservée dans le README
- P6 — Rangée Up Next : livrée (v0.1.0-rc.2), archivée avec `jellyfin-tvprovider-only` (`archive/2026-10-11-up-next`, `archive/2026-10-11-jellyfin-tvprovider-only`)
- P7 — Écran d'accueil par défaut : configuration automatique par l'ADB local de la TV (démarrage du constructeur, écran d'accueil, désactivation de l'accueil d'origine si nécessaire) ou par autorisations ; livré, archivé (`archive/2026-10-17-p7-default-launcher`), validation sur la TV de référence encore à faire ; suite : parcours simplifié, troisième façon par Tsuyu installé depuis la TV, réparation automatique, livrée dans v0.3.0-rc.1 et archivée (`archive/2026-10-18-tsuyu-home-setup`), validation TV en attente
- P8 — Recherche : planifié
- P9 — BetaSeries (OAuth) : planifié
- P10 — Remappage des boutons de la télécommande : à spécifier
Les archives historiques (`openspec/changes/archive/`) conservent leur numérotation d'époque : ne pas les renuméroter.

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
- Commentaires : aucun commentaire dans le code ni dans les tests, sauf l'en-tête de licence de chaque fichier
- Tous les appels réseau : états loading/empty/error en UI, timeout borné, jamais de crash silencieux
- Tests JUnit sur la logique métier (fusions Up Next, tri, mapping) ; UI validée sur la TV réelle
- Pas de backend : tout en local (Coil, DataStore, media3)

## Design (référence : tvOS)
- Thème sombre uniquement en v1, noir pur, posters plein cadre
- Focus : zoom ~1.08x, ombre douce, animations 250-400ms courbes Apple, jamais de saut sec
- Liquid Glass : verre sombre réfractant (Haze 2), ombre portée douce autorisée, aucun halo coloré
- Grille d'apps auto-détectée (LEANBACK_LAUNCHER), tuiles 16:9 (bannière Android TV)

## Git & packaging
- Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`...) ; branches `feat/`, `fix/`, `chore/` — pas de commit direct sur main après l'init du projet
- Commits signés et author cohérent avec le compte GitHub
- Namespace Android : `fr.sygix.<appname>` — launcher : `fr.sygix.sygixos`
- Cible : Android 14+ (minSdk 34, compileSdk 37)
- Dépendances externes : uniquement des libs éprouvées/maintenues (Room, Retrofit, Coil...), sous licence compatible AGPL-3.0 (Apache-2.0, MIT, BSD, LGPL, GPL-3.0…)
- Licence : AGPL-3.0-or-later. Tout fichier `.kt` / `.kts` créé commence par cet en-tête, puis une ligne vide (y compris les tests) :
  ```
  /*
   * Copyright (C) <année> Sygix
   * SPDX-License-Identifier: AGPL-3.0-or-later
   */
  ```
  Avant de pousser, vérifier qu'aucun fichier n'en manque : `git ls-files '*.kt' '*.kts' | xargs grep -L 'SPDX-License-Identifier: AGPL-3.0-or-later'` doit être vide

## Repo public
Le repo est public : tout ce qui est poussé (code, specs, messages de commit, descriptions et commentaires de PR, issues, logs de CI) est lisible par tous et reste dans l'historique, même après suppression.
- Jamais de secret : clés, tokens, mots de passe, keystore, URL avec identifiants. Les secrets passent par les secrets GitHub ou des variables d'environnement, jamais par un fichier suivi
- Jamais de détail de l'environnement local : chemins absolus (`/home/…`), noms d'utilisateur, de machine ou de VM, adresses IP et ports, noms de réseau, numéro de série ou modèle exact d'un appareil, URL de serveurs personnels (Jellyfin…), comptes. Dans la doc, les specs et les PR, désigner l'appareil de test de façon générique (« TV Google TV sous Android 14 ») ; la marque TCL est déjà publique, rien de plus précis
- Données issues d'un appareil (dumps du TV Provider, `adb logcat`, captures) : ne publier que la structure (colonnes, packages, compteurs, types), jamais les titres, l'historique de visionnage ni des données personnelles
- Jamais de sortie de build ni de cache (`build/`, `.gradle/`, `.kotlin/`, APK) : vérifier `git status` et `git diff --cached` avant chaque commit ; ce qui est local va dans `.gitignore` ou `local.properties`
- Avant de pousser : `git diff origin/main... | grep -nE '/home/|/Users/|([0-9]{1,3}\.){3}[0-9]{1,3}'` doit être vide (hors versions de dépendances)

## Environnement
- Messages de commit en anglais, impératif court
- Repo : github.com/Sygix/SygixOs
- Cible : TCL Google TV, Android 14 ; juger la fluidité sur `assembleRelease` (le build debug est interprété et saccade)
- ADB : `adb connect <IP-TV>` pour installer et tester ; commandes P5 dans le README

## Outils
- OpenSpec CLI (`openspec`, v1.13+, installé via `pnpm add -g @fission-ai/openspec`) : valider avec `openspec validate --all --strict`, archiver un change avec `openspec archive <id>` ; pnpm (v12) obligatoire à la place de npm pour tout paquet Node
- Format strict des specs : `### Requirement:` (SHALL/MUST), `#### Scenario:` avec `- **WHEN** / **THEN**`
