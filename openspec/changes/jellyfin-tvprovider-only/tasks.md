# Tasks : jellyfin-tvprovider-only

Change spec-only : aucun code de configuration Jellyfin ni d'appel réseau n'existe dans le repo, il n'y a rien à supprimer ; la lecture du TV Provider et ses tests appartiennent à `p2c-upnext`.

## 1. Spec

- [ ] 1.1 `openspec validate --all --strict` vert avec ce change (delta jellyfin-integration : REMOVED « Configuration », MODIFIED « Continue watching / Up Next » et « Résilience »)

## 2. Archivage

- [ ] 2.1 Après merge sur main : `openspec archive jellyfin-tvprovider-only`, puis `openspec validate --all --strict` vert et `openspec/specs/jellyfin-integration/spec.md` sans exigence « Configuration »
