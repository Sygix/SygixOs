# Change : tv-provider-hero

Alimente le carrousel héro avec le contenu Jellyfin déjà publié par l'app Jellyfin Android TV dans le TV Provider système (aucune connexion directe au serveur, aucun réglage à saisir).

## Why
Le héro ne montre aujourd'hui que le fallback aérien : `HeroContentProvider` n'a qu'une implémentation (aerial). L'app Jellyfin Android TV publie elle-même son « Continue Watching » et ses channels dans le TV Provider (`TvContractCompat`, preview channels/programs + Watch Next) — c'est ce que lit aussi Projectivy. Lire cette source évite credentials, réglages, et duplication de logique.

## What Changes
- Nouveau `TvProviderHeroSource` (package `data`) : interroge `content://android.media.tv` (watch_next_programs + preview_programs + channels preview) et remonte **tous les programmes publiés par les apps installées** (Jellyfin, Netflix, Prime… dès qu'une app publie dans le TV Provider, son contenu apparaît — aucune liste codée en dur)
- Mapping programme → `HeroItem` : poster (posterArtUri, fallback thumbnailUri), titre, progression (playback position/durée), intent du programme (deep link direct fourni par Jellyfin — pas de scheme à deviner)
- Chargement des posters hors thread UI ; si un poster échoue, tuile dégradée sans crash
- Ordre déterministe : reprises en cours d'abord (progression > 0), puis le reste par engagement le plus récent, toutes apps confondues
- Fallback aerial inchangé quand aucun programme n'est publié ou que Jellyfin est absent
- Images : Coil (déjà compatible Compose) — nouvelle dépendance unique
- Clic sur poster : lancement de l'intent du programme (fiche du contenu dans Jellyfin), fallback lancement simple de l'app

## Impact
- specs affectées : launcher-shell (delta ci-dessous, étend le héro défini dans hero-home)
- nouvelle dépendance : coil-compose
- aucune permission sensible : lecture du TV Provider incluse dans les apps TV launcher (PLAYBACK/READ_TV_PROVIDER selon version — à confirmer au build)
