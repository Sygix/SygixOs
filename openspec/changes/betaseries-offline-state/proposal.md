# Proposal

## Why

Une panne BetaSeries ou réseau ne doit ni vider l'accueil ni perdre les marquages explicitement demandés sur la TV, même après redémarrage du processus. Le compte est aujourd'hui chargé en mémoire et les mutations ne disposent pas de file persistante.

## What Changes

- Persister l'identité et le dernier état connu du compte BetaSeries, ses films et sa réserve d'épisodes, isolés par identifiant stable de fournisseur et de compte.
- Appliquer immédiatement vu/non vu aux épisodes et aux films après écriture locale durable ; conserver les demandes jusqu'à confirmation distante, avec priorité à l'intention locale en attente.
- Conserver par série le prochain épisode diffusé non vu et au moins cinq suivants diffusés, toutes saisons confondues si nécessaire, ou tous ceux restant disponibles s'il y en a moins. Une seule carte et un seul candidat de série restent projetés.
- Synchroniser les demandes automatiquement, y compris en arrière-plan via l'ordonnanceur Android, avec réconciliation des résultats incertains et réessais espacés.
- Nettoyer les demandes confirmées et les épisodes sortis de la réserve dès qu'ils ne servent plus à une demande, une annulation ou un contenu local encore pertinent ; recompléter la réserve en ligne. Les films suivent le même nettoyage sans réserve de suivants.
- Effacer cache et demandes du compte à sa déconnexion ou à son remplacement par un autre compte ; préserver les préférences locales indépendantes du compte.

## Capabilities

### New Capabilities

Aucune : la persistance et la synchronisation prolongent les contrats de suivi existants.

### Modified Capabilities

- `watch-tracking` : état local durable, vu/non vu différé pour épisodes et films, annulation, isolation et projection prioritaire des demandes locales.
- `betaseries-integration` : restauration du cache, réserve, nettoyage, fin de session et exception encadrée à l'absence de reprise automatique des mutations.
- `up-next` : maintien de la contribution BetaSeries en panne transitoire et prise en compte des statuts locaux dans la fusion.
- `launcher-shell` : maintien du héro du suivi et présentation des demandes en attente dans l'indicateur existant.
- `settings` : distinction entre sauvegarde locale, attente d'envoi, synchronisation confirmée et reconnexion dans les contrôles existants.
- `ui-testing` : couverture de la persistance, des films, de la réserve, des courses de synchronisation et des parcours TV hors ligne.

## Impact

- Frontières concernées : `TrackingController`, `TrackingActions`, `TrackingRuntime`, `TrackingEnrichment`, `TrackingProjection`, `TrackingHeroSource`, `TrackingViewModel`, `BetaSeriesProvider`, stockage de session et présentations de suivi existantes.
- Base Room dédiée au suivi (Room et KSP sont déjà présents) ; ajout de WorkManager pour le travail persistant contraint au réseau. Les jetons restent dans le stockage protégé existant.
- API BetaSeries existante : identité `members/infos`, compte paginé, `episodes/list`, fiches individuelles et mutations vu/non vu. Le contrat réel de réserve multi-épisodes et inter-saisons doit être vérifié avant implémentation.
- Aucun nouveau palier ni nouvelle surface d'accueil ; les contrats de navigation existants restent la référence.

## Dependencies / Archive Order

- Base fonctionnelle : P9 livré et archivé. Aucune dépendance d'implémentation sur le change `tv-resource-budget` ; ses deltas de rafraîchissement du TV Provider ne sont pas modifiés ici et la synchronisation BetaSeries ne relance pas ces lectures.
- Le correctif `betaseries-session-recovery` est fusionné dans la base d'implémentation. Ses exigences de fin de session, de bornes réseau et de présentation sont conservées, notamment la distinction panne temporaire/refus explicite. Sa validation sur TV reste ouverte : son archivage attend cette preuve et précédera celui du présent change. Ce change remplace uniquement son interdiction de reprise automatique pour les vu/non vu persistants.
- Archiver ce change après son merge et sa validation sur TV réelle, avec réexamen des exigences communes si la récupération de session est encore en attente.

## Non-goals

- Ajout, retrait, archivage et désarchivage hors ligne : reportés à un éventuel change ultérieur ; leurs contrats actuels restent applicables.
- Téléchargement des vidéos, garantie de lecture hors ligne des applications tierces et préchargement intégral des images : hors périmètre ; le cache partagé et les replis existants s'appliquent.
- Conservation de plusieurs comptes déconnectés, résolution interactive des conflits et historique exhaustif du compte : hors périmètre de cette phase.

## Questions ouvertes

Aucun choix produit bloquant restant. Le contrat multi-épisodes et l'exécution réelle en arrière-plan constituent des vérifications techniques explicites, pas des garanties déjà observées sur la TV.
