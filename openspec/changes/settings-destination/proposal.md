# Proposal

## Why

La page de réglages saccade : elle est dessinée au-dessus de l'accueil, qui reste dessiné en entier dessous (visuel plein écran du héro, voiles, dégradé, verre du dock, tuiles, vidéo). Chaque image animée des réglages rejoue cet accueil invisible. Le propriétaire veut une page réellement distincte, sans perdre le retour immédiat vers le héro.

## What Changes

- Pendant les réglages, l'accueil reste composé avec tout son état (programme et visuel du héro, lecteur, verre, position de la grille) mais il n'est plus placé : ni dessiné ni focusable. Le retour le replace tel quel, sans recomposition ni nouvel écran de démarrage.
- La vidéo d'aperçu du héro est mise en pause à sa position pendant les réglages au lieu d'être arrêtée, et reprend au retour sans rechargement.
- Le verre des réglages (liste déroulante de « Position d'À suivre ») ne floute plus les sources de l'accueil masqué.
- Les licences d'« À propos » sont lues une seule fois par processus au lieu de l'être à chaque entrée dans la catégorie.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `settings` : « Capsule heure et réglages » (scénario « ouverture ») et « Page de réglages » (accueil non dessiné pendant les réglages, retour immédiat).

`launcher-shell` et `ui-testing` ne changent pas : la touche Home au premier plan, les panneaux du launcher système et l'économie de ressources en zone grille gardent leurs exigences.

Dépend de `hero-background-release` : le lecteur n'est plus libéré quand le héro est masqué et le scénario « retour immédiat » renvoie à son rechargement après un passage en arrière-plan (« Préchargement et mémoire ») ; l'archiver avant celui-ci. Compatible avec `settings-lazy-lists` et `p7-default-launcher` (le panneau du launcher système reste dessiné au-dessus des réglages).

## Impact

`LauncherHome` (placement de l'accueil, verre des réglages), `HeroStage` (pause de la vidéo), `AboutContent` (licences). Aucune nouvelle Activity, bibliothèque, préférence ni chaîne.

## Non-goals

- Démonter l'accueil ou créer une destination de navigation : l'état conservé et le retour immédiat priment ; aucune phase prévue.
- Réduire la mémoire de l'accueil pendant les réglages : hors périmètre.
- Retoucher l'animation des pilules de focus : hors périmètre, aucune phase prévue.

## Open Questions

Aucune.
