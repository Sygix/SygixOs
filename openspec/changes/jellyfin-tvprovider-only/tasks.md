# Tasks : jellyfin-tvprovider-only

Change spec-only : aucun code de configuration Jellyfin ni d'appel réseau n'existe dans le repo, il n'y a rien à supprimer ; la lecture du TV Provider et ses tests appartiennent à `up-next`.

## 1. Spec

- [x] 1.1 `openspec validate --all --strict` vert avec ce change (delta jellyfin-integration : REMOVED « Configuration », MODIFIED « Continue watching / Up Next » et « Résilience ») — 2026-10-11 : `openspec validate jellyfin-tvprovider-only --strict` vert ; pour `--all --strict`, openspec 1.14.1 signale en `--strict` les avertissements « Requirement text is very long », déjà présents sur main ; `openspec validate --all` vert.

## 2. Archivage

- [x] 2.1 Après merge sur main : `openspec archive jellyfin-tvprovider-only`, puis `openspec validate --all --strict` vert et `openspec/specs/jellyfin-integration/spec.md` sans exigence « Configuration » — Archivé le 2026-10-11 avec `up-next` (P6, v0.1.0-rc.2) ; `openspec/specs/jellyfin-integration/spec.md` sans exigence « Configuration » ; openspec 1.14.1 signale en `--strict` les avertissements « Requirement text is very long », déjà présents sur main ; `openspec validate --all` vert.
