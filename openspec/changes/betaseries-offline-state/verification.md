# Vérification d'implémentation

## Contrat API — 27 octobre 2026

À la demande de Sygix, le prérequis API a été exécuté dans l'émulateur Android TV sous Android 14, avec association volontaire de son compte. Il remplace la vérification initiale sur appareil physique ; la validation finale sur TV reste distincte. Les appels étaient exclusivement des lectures. Aucun jeton, identifiant, titre ou historique n'a été exporté.

- `episodes/list?released=1&showsLimit=100&limit=6` : objet contenant `shows` (tableau), `total`, `totalEpisodes`, `totalMissingEpisodes`, `totalMissingShows` (compteurs) et `errors`.
- Chaque groupe expose `unseen` ; seize groupes contenaient six épisodes, un groupe traversait une frontière de saison. Le maximum observé était six. Aucun ordre saison/épisode décroissant ou doublonné dans un groupe.
- Les 103 épisodes observés exposaient `date` textuelle et `user.seen` booléen. Les champs d'identité et numéros étaient exploitables par les décodeurs existants.
- `showId` avec `showsLimit=1` sélectionne un seul groupe de six épisodes. `page=2` renvoie zéro groupe pour cette série : ce n'est pas un curseur sur les épisodes.
- La lecture du compte renvoyait vingt groupes pour `total=20`, sans manquants ; la deuxième page était vide et sans recouvrement.
- `shows/seasons?id=…` expose un tableau `seasons` et `number` entier. `shows/episodes?id=…&season=…` fournit les épisodes de cette saison avec `user.seen` booléen.

Protocole retenu : chargement initial multi-épisodes par `episodes/list`, pagination des groupes selon le contrat existant ; complément ciblé par série. Si les intentions locales absorbent la fenêtre distante, utiliser les saisons explicites pour continuer après application des intentions, dans les bornes réseau, sans inventer de pagination d'épisodes. Ne jamais déduire une fin disponible d'un résultat partiel ou d'une date inconnue.

Preuve : instrumentation de diagnostic locale dans le processus Android, `INSTRUMENTATION_CODE: 0`, `sessionPresent=true`. L'instrumentation ne fait pas partie de l'application livrée.

## Synthèse de vérification

| Dimension | Résultat |
| --- | --- |
| Complétude | 20/22 tâches vérifiées ; validations finales sur TV physique encore ouvertes, sans archivage. |
| Correction | Les exigences ajoutées et modifiées sont reliées aux frontières et tests ci-dessous ; essais authentifiés sur émulateur avec restauration exacte des deux statuts. |
| Cohérence | Base Room dédiée sans effacement destructif, projection transactionnelle, jeton protégé séparé, intentions révisionnées et worker autonome. Les corrections de récupération de session fusionnées sont conservées. |

### Commandes et résultats

- `./gradlew test` : **1 283 tests, zéro échec** après corrections de revue ; dernière exécution complète réussie en 48 secondes, avec limite externe de 150 secondes.
- `./gradlew assembleRelease` : réussi sur les sources corrigées en 1 minute 18 secondes, avec limite externe de 120 secondes.
- `openspec validate --all --strict` : **11 éléments valides, aucun échec**.
- Un blocage de suite a été observé pendant le développement des tests Compose ; la projection Room a été déplacée hors du thread principal et les tests ont reçu un ordonnanceur explicite. Une exécution complète ultérieure n'a pas terminé ; le blocage ne s'est pas reproduit lors du diagnostic isolé puis complet, et sa cause n'est pas établie. Aucun succès n'est déduit d'une commande interrompue.

### Correspondance exigences / preuves

| Exigences | Implémentation | Preuves |
| --- | --- | --- |
| État de suivi local durable | `TrackingDatabase`, `TrackingContentCodec`, `TrackingRepository`, restauration de `TrackingController` | `TrackingRepositoryTest` : recréations du fichier entre six intentions, compte différent, protections de nettoyage, migration réelle 1 vers 2 conservant compte, intentions potentiellement envoyées, révisions, statuts, dates et références, puis refus de version inconnue sans effacer les intentions. `TrackingOfflineControllerTest` : restauration sans réseau et sauvegarde impossible sans publication ni planification. Le schéma 2 ajoute la révision de demande du complément de réserve. |
| Actions sur le compte et Annuler | `TrackingActions`, transactions `record`, `undo`, `confirm` | Sauvegarde avant publication ; tests d'intentions concurrentes et d'état exact du film. Tests existants d'actions, de retrait différé et de fenêtre active conservés ; la confirmation de notice ne prolonge pas sa fenêtre. |
| Réserve glissante | `BetaSeriesProvider.snapshot`, `reserve`, table `tracking_reserve` | `BetaSeriesReserveTest` : six candidats après application des intentions, traversée de saison, fin plus courte, date future et date absente. `TrackingRepositoryTest` : six progressions avec recréation sans réseau. Les sélecteurs de saisons restent à la demande. |
| Confirmation indépendante et synchronisation | `TrackingSyncCoordinator`, `TrackingSyncWorker`, `AndroidTrackingScheduler` | `TrackingSyncTest` : réponse perdue, vérification en panne sans renvoi aveugle, intention plus récente, worker sans Activity, refus d'accès, Retry-After durable, complément seul réessayé. Réseau contraint et absence d'envoi concurrent également observés sur Android. |
| Nettoyage du suivi persistant | Transactions et références `undo` / `local` de `TrackingRepository` | Suppression réelle des lignes d'épisode et film après libération des références ; une intention plus récente reste protégée. Les références locales ne sont remplacées qu'après un snapshot local confirmé, jamais sur une erreur du TV Provider. Aucun accès au cache d'images dans ce nettoyage. |
| Fin de session | `TrackingAccountLifecycle`, marqueur de purge atomique, liaison par empreinte du jeton | `TrackingAccountLifecycleTest` : interruption de purge, recréation du marqueur, session bloquée jusqu'à nettoyage puis reconnexion sans ancienne file. `TrackingSyncTest` : réponse tardive de A après association de B. Les préférences indépendantes du compte ne font pas partie de la base purgée. |
| Héro, À suivre, réglages et indicateur | Projection commune de `TrackingRuntime`, `TrackingEnrichment`, présentations et tags existants | `TrackingOfflineSettingsTest` à la taille TV française : projection commune, Annuler, attente sans carte, détail et Retour avec focus conservé, refus d'accès sans purge implicite. `TrackingOfflineHomeTest` utilise le véritable accueil et le dépôt Room : Menu et D-pad marquent le film, sa carte disparaît avec repli sur la grille, Annuler la restaure depuis l'engrenage ; un marquage d'épisode affiche le suivant et conserve le focus de la série. Ressources présentes dans les cinq langues et contrôle de complétude inclus. |

### Corrections de revue indépendante

La revue initiale a demandé quatre corrections. Les quatre tests de régression échouaient sur le head relu avant modification, puis réussissent avec les correctifs ; la suite complète et la construction release ont ensuite été exécutées.

| Point | Statut | Correction et preuve |
| --- | --- | --- |
| P1, départ après invalidation pendant la découverte de configuration | Traité et vérifié | Autorisation de requête transportée par le contexte de coroutine, revalidation après découverte, puis garde atomique au véritable `Call.enqueue`, partagée avec invalidation et liaison de compte. `configurationDiscoveryCannotDispatchAfterDisconnectOrAccountReplacement` vérifie zéro mutation de A ; `departureAuthorizationIsCheckedAtTheActualEnqueueBoundary` couvre l'invalidation juste avant envoi effectif. |
| P2, archivage/retrait appliqué au seul épisode courant | Traité et vérifié | `patch` propage appartenance et archive transactionnellement à tous les enfants, sans effacer leurs intentions. `parentArchiveRemovalAndUndoApplyToTheEntireSixEpisodeReserve` couvre archivage, annulation et retrait avec une demande d'épisode conservée. |
| P2, rafraîchissement effaçant un complément concurrent | Traité et vérifié | Révision durable de la demande de complément, contrôle du point de départ du snapshot et conservation des besoins liés aux intentions/confirmations plus récentes. `oldSnapshotCannotClearARefillRequestedByAnOverlappingLocalIntent` conserve le complément puis vérifie six candidats après remplissage ciblé. |
| P2, réserve orpheline réessayée sans contenu | Traité et vérifié | Une absence complète supprime la réserve devenue inutile, sans se fonder sur une absence partielle ni effacer une demande protégée. `completeAbsenceRemovesObsoleteReserveWithoutUsingPartialAbsenceAsProof` vérifie conservation sur résultat partiel puis suppression après résultat complet. |

Les remarques de preuve concernant les tâches 2.1 et 6.2 sont également traitées par la migration réussie et les tests du véritable accueil cités ci-dessus. La seconde revue du code corrigé reste distincte de ces vérifications d'autrice.

La seconde revue a confirmé les quatre corrections et identifié un cas supplémentaire de renouvellement du jeton pour le même compte. Une autorisation locale devenue obsolète est désormais distincte d'un refus du fournisseur ; les erreurs et échéances du worker ne sont engagées qu'en transaction sous la même empreinte de session. `sameAccountTokenRenewalDoesNotTurnAnObsoleteReadIntoAnAccessRefusal` échouait avant correction ; il couvre désormais lecture de réconciliation et lecture de réserve, conservation des intentions et reprise réussie avec le nouveau jeton. `oldBindingCannotRecordAccessFailureOrBackoffAfterSameAccountRenewal` vérifie qu'une ancienne session ne peut ni refuser ni retarder la nouvelle. Les lectures obsolètes ne sont pas reprises comme des pannes réseau.

### Essais Android authentifiés

Substitution de l'émulateur à la TV physique demandée par Sygix ; mutations temporaires d'un épisode et d'un film explicitement autorisées, puis restauration exacte obligatoire. Seuls structure, compteurs et statuts sont consignés.

| Scénario | Observation | Résultat |
| --- | --- | --- |
| Premier remplissage | Session présente, snapshot complet, épisode et film disponibles, 229 entrées mises en cache | Vérifié |
| Coupure réelle du réseau | Aucun réseau par défaut ; contrainte `CONNECTIVITY` non satisfaite | Vérifié |
| Marquages locaux | Deux intentions persistées, un complément en attente ; accueil conservé après relance hors ligne et attente distincte du succès distant dans les réglages | Vérifié |
| Release en arrière-plan au retour du réseau | Launcher quitté ; exécution naturelle de `TrackingSyncWorker`, sans commande d'exécution forcée, environ 2,5 secondes après restauration du réseau | Vérifié |
| Confirmation distante et complément | Zéro intention et zéro complément restant ; les deux statuts distants modifiés | Vérifié |
| Restauration du compte | Deux statuts initiaux exactement rétablis et relus ; zéro intention et zéro complément restant | Vérifié |
| Déconnexion de test | Session de l'émulateur révoquée, compte et file purgés, résultat `purged=true` | Vérifié |

L'installation d'un APK sur l'émulateur a placé le paquet dans l'état Android arrêté : une ouverture utilisateur a été nécessaire avant reprise du travail de restauration. Cette observation est distincte du succès naturel du worker release précédent. Le réseau de l'émulateur a été rétabli et l'émulateur arrêté. Les captures ont été transmises directement au propriétaire, sans publication dans le dépôt. Le diagnostic local et son instrumentation ont été retirés des sources avant publication.

Un contrôle release supplémentaire après les premières corrections de revue a installé l'APK sans effacer les données existantes : accueil puis réglages de suivi déconnectés utilisables, contrôle Connecter BetaSeries présent, aucune erreur de stockage/migration affichée et buffer de crash vide. Aucun nouveau compte n'a été associé et aucune mutation distante effectuée. Ce contrôle ne constitue pas une inspection SQL du binaire release ni un nouvel essai authentifié ; le dernier correctif de renouvellement de session est vérifié par les tests automatisés. Émulateur arrêté à nouveau après ce contrôle.

### Points encore ouverts

- **Avant archivage :** tâches 7.2 et 7.3 sur TV physique, notamment plusieurs épisodes, sortie de veille, redémarrage complet et arrêt forcé ; l'émulateur ne prouve ni ces restrictions constructeur ni la fluidité.
- L'essai Android authentifié précède l'extraction finale de la frontière de purge, l'ajustement de la notice et les corrections de revue. Ces changements sont couverts par la suite automatisée ; ils ne constituent pas un second essai authentifié du dernier head.
- Le change `betaseries-session-recovery`, fusionné dans la base, attend lui aussi sa preuve TV avant archivage. Il doit être archivé avant le présent change.
- Revue indépendante et CI du head courant : consulter la PR #106 ; leurs résultats restent distincts des vérifications d'autrice consignées ici.
