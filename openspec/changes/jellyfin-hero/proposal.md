# Change : jellyfin-hero

Alimente le carrousel héro avec le contenu Jellyfin déjà publié par l'app Jellyfin Android TV dans le TV Provider système (aucune connexion directe au serveur, aucun réglage à saisir).

## Why
Le héro ne montre aujourd'hui que le fallback aérien : `HeroContentProvider` n'a qu'une implémentation (aerial). L'app Jellyfin Android TV publie elle-même son « Continue Watching » et ses channels dans le TV Provider (`TvContractCompat`, preview channels/programs + Watch Next) — c'est ce que lit aussi Projectivy. Lire cette source évite credentials, réglages, et duplication de logique.

## What Changes
- Nouveau `TvProviderHeroSource` (package `data`) : interroge `content://android.media.tv/channels` + `preview_programs`, filtre les programmes du package Jellyfin (`org.jellyfin.mobiletv` — confirmé à l'exécution via l'app, pas codé en dur dans le domaine)
- Mapping programme → `HeroItem` : poster (posterArtUri, fallback thumbnailUri), titre, progression (playback position/durée), intent du programme (deep link direct fourni par Jellyfin — pas de scheme à deviner)
- Chargement des posters hors thread UI ; si un poster échoue, tuile dégradée sans crash
- `JellyfinHeroProvider` (implémentation de `HeroContentProvider`) : fusionne Continue Watching + programmes récents du channel Jellyfin dans un seul carrousel, ordre déterministe (reprise d'abord, puis plus récent)
- Fallback aerial inchangé quand aucun programme n'est publié ou que Jellyfin est absent
- Images : Coil (déjà compatible Compose) — nouvelle dépendance unique
- Clic sur poster : lancement de l'intent du programme (fiche du contenu dans Jellyfin), fallback lancement simple de l'app

## Impact
- specs affectées : launcher-shell (delta ci-dessous, étend le héro défini dans hero-home)
- nouvelle dépendance : coil-compose
- aucune permission sensible : lecture du TV Provider incluse dans les apps TV launcher (PLAYBACK/READ_TV_PROVIDER selon version — à confirmer au build)
