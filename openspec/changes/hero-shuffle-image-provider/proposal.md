# Proposal

## Why

- **Plantage** : sur la TV de référence (Google TV sous Android 14), quand le serveur d'une application source (Jellyfin) ne répond pas, le fournisseur d'images de cette application se bloque ; le système arrête l'application pour ANR et arrête aussi SygixOs. Journal système (structure) : `Killing <pid>:fr.sygix.sygixos (adj 100): depends on provider org.jellyfin.androidtv/.integration.provider.ImageProvider in dying proc org.jellyfin.androidtv`. Cause : les images `content://` d'autres applications sont ouvertes par `ContentResolver` (récupérateur par défaut de Coil, `openAssetFileDescriptor`), qui garde une référence stable au fournisseur tant que le descripteur est ouvert ; Android arrête tout client qui garde une référence stable vers un fournisseur dont le processus meurt.
- **Héro** : l'ordre déterministe (reprises d'abord, puis plus récents) montre toujours les mêmes programmes en premier. Décision du propriétaire (2026-10-11) : ordre aléatoire « à chaque lancement ».

## What Changes

- Les images `content://` d'autres applications sont ouvertes par une référence instable (`ContentResolver.acquireUnstableContentProviderClient`, puis `openTypedAssetFile(uri, "image/*", null, CancellationSignal)`), sur un fil d'entrée-sortie, avec un délai borné qui annule l'ouverture ; référence et descripteur toujours libérés ; `DeadObjectException`, délai ou erreur : image en échec, sans plantage. Les images qui ne se chargent pas quand le serveur de l'application est injoignable restent un comportement attendu (espace réservé).
- L'ordre du héro est tiré au hasard une fois par lancement de SygixOs, stable pendant la session ; un programme apparu pendant la session est inséré au hasard après le programme affiché.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : « Contenu héro TV Provider » (MODIFIED : ordre du héro) ; « Ordre aléatoire du héro » et « Images d'autres applications sans dépendance forte » (ADDED).

## Impact

Chargement d'images (récupérateur Coil propre aux URI `content://` d'autres applications, enregistré dans l'`ImageLoader`), tri du héro dans le domaine, tests JUnit. Aucune dépendance ajoutée, aucune préférence persistée.

## Non-goals

- Corriger le blocage du fournisseur d'images de l'application source (hors SygixOs).
- Choisir l'ordre du héro dans les réglages, mélanger À suivre ou le Top Shelf.
