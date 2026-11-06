## MODIFIED Requirements

### Requirement: Gagnant d'un doublon
Une reprise non vue SHALL primer sur le prochain épisode du compte ; une reprise déjà vue SHALL être écartée au profit du prochain non vu diffusé. Le statut effectif SHALL intégrer les intentions locales persistantes de `watch-tracking`, pour épisodes et films. Entre reprises admissibles, activité récente puis préférence locale existante SHALL trancher. Sans suivi identifié, l'ordre SHALL rester CONTINUE, NEXT/NEW ou absent, WATCHLIST, puis activité et préférence d'app.

#### Scenario: reprise en cours
- **WHEN** un contenu existe en CONTINUE et dans un groupe à suivre ou mis de côté
- **THEN** la reprise admissible est retenue avec sa progression et sa date d'activité

#### Scenario: à suivre contre mis de côté
- **WHEN** un même contenu existe comme à suivre et WATCHLIST sans reprise
- **THEN** l'item à suivre est retenu

#### Scenario: engagement le plus récent
- **WHEN** les candidats sont du même groupe admissible
- **THEN** l'activité la plus récente gagne

#### Scenario: égalité
- **WHEN** deux candidats locaux ont le même timestamp d'activité
- **THEN** la préférence locale existante (Jellyfin d'abord) tranche ; cette règle ne constitue pas une préférence de lecture imposée au premier choix du suivi

#### Scenario: épisode local en avance
- **WHEN** BetaSeries propose S02E03 et qu'une app propose une reprise non vue de S02E05
- **THEN** S02E05 est affiché sans marquer S02E03 ou S02E04 comme vus

#### Scenario: reprise ancienne déjà vue
- **WHEN** l'épisode repris par une app est vu selon l'état effectif du compte, confirmé à distance ou enregistré localement en attente
- **THEN** cette reprise ne masque pas le prochain non vu disponible ; sans prochain épisode en cache, aucune suite n'est inventée et la réserve épuisée reste distincte d'une série confirmée à jour

#### Scenario: statut de reprise inconnu
- **WHEN** l'identité ou le statut de l'épisode local n'est pas confirmé et qu'aucune intention locale ne l'établit
- **THEN** l'absence dans la liste des épisodes à voir ne vaut pas preuve de visionnage ; la reprise locale reste admissible sans mutation automatique du compte

#### Scenario: film marqué localement
- **WHEN** une app publie encore la reprise d'un film marqué vu localement en attente de synchronisation
- **THEN** le même statut effectif s'applique à la contribution identifiée, sans réapparition due à un ancien résultat distant ; annuler ou marquer non vu recalcule la contribution avec les données disponibles

### Requirement: États de la rangée
La rangée SHALL distinguer chargement, vide, erreur et accès refusé par source, sans bloquer le home. Les programmes locaux SHALL garder leur contrat de validité. Une panne temporaire du suivi SHALL conserver son état local durable avec indicateur ; refus d'accès et déconnexion SHALL retirer son apport. Focus et disparition SHALL suivre « Navigation 3 paliers ».

#### Scenario: permission refusée
- **WHEN** READ_TV_LISTINGS n'est pas accordée
- **THEN** seules les données locales deviennent indisponibles ; les contenus de suivi accessibles, y compris leur cache lié, restent affichés ; sans eux la rangée est masquée sans invitation à configurer la permission

#### Scenario: erreur
- **WHEN** la lecture locale échoue au premier chargement et qu'aucune autre source n'apporte de contenu
- **THEN** une seule carte d'état focusable remplace le contenu en première position sous le titre À suivre à sa position réglée (maquette 7.4), distincte du vide : même taille et coins qu'une carte, fond `SygixColors.TileBackground`, pictogramme recharger centré de `Dimens.UpNextErrorIcon`, « Impossible de charger » en `TextStyles.Row` / `SygixColors.OnDark`, « OK pour réessayer » en `TextStyles.RowSecondary` / `SygixColors.OnDarkSecondary` ; une erreur du suivi reste aussi consultable via son indicateur

#### Scenario: carte d'erreur
- **WHEN** la carte d'erreur locale prend le focus
- **THEN** le focus d'une tuile selon « Focus tvOS » s'applique ; elle occupe la place de la rangée sous À suivre dans la navigation de grille, avant ou après les applications selon le réglage, et OK relance la requête

#### Scenario: chargement initial
- **WHEN** le premier chargement sans contenu prêt dépasse environ 300 ms
- **THEN** le squelette sous À suivre (maquette 7.3) montre cinq cartes de la taille et des coins d'une carte, fond `SygixColors.SkeletonBase`, chacune avec deux barres `SygixColors.SkeletonBar` en bas à gauche, `Dimens.UpNextSkeletonTitle` puis `Dimens.UpNextSkeletonSubtitle`, rayons à mi-hauteur, sans animation imposée ni élément focusable ; sous environ 300 ms aucune étape intermédiaire n'apparaît, et un résultat local prêt n'attend pas le compte

#### Scenario: rechargement
- **WHEN** la rangée se recharge avec du contenu encore valide
- **THEN** elle conserve ce contenu pendant l'attente sans squelette ni prolongation de validité ; une panne temporaire du suivi maintient son cache selon `watch-tracking`, sans le déclarer fraîchement confirmé

#### Scenario: erreur au rechargement
- **WHEN** une relecture locale échoue
- **THEN** ses programmes précédents restent affichés jusqu'à expiration sans carte d'erreur ni saut, sans renouveler leur confirmation ; le cache BetaSeries suit sa propre persistance et ne dépend pas du plafond des programmes du TV Provider

#### Scenario: vide
- **WHEN** toutes les sources accessibles réussissent sans item après application des intentions locales et qu'aucune autre n'apporte de contenu
- **THEN** la rangée est masquée et les images en cache ne la repeuplent pas ; une réserve épuisée sans réseau n'est pas annoncée comme un résultat distant complet

#### Scenario: non-blocage
- **WHEN** une source est dans n'importe quel état
- **THEN** le D-pad du home reste utilisable ; vu/non vu peut être enregistré hors ligne selon `watch-tracking` et les autres actions gardent leur résultat propre

#### Scenario: expiration du dernier résultat après une erreur
- **WHEN** les programmes locaux atteignent leur plafond sans confirmation
- **THEN** ils sont retirés ; s'il reste des contenus de suivi affichables selon leur propre contrat, ils restent affichés, sinon l'erreur locale réessayable apparaît ; le focus est restauré selon la navigation existante ; aucune mémoire de retrait ou d'ouverture n'est effacée sur cette erreur

#### Scenario: redémarrage hors ligne et reconnexion
- **WHEN** la rangée est recréée sans réseau avec un cache du compte lié puis que le réseau revient
- **THEN** les films et candidats d'épisodes effectifs restent navigables ; la synchronisation partagée met à jour leurs états sans dupliquer de carte par série ni relire le TV Provider pour ce seul événement
