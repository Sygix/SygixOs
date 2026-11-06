# Vérification d'implémentation

## Cible acceptée

Le propriétaire a explicitement remplacé les vérifications sur TV physique par l'émulateur Android TV sous Android 14 pour l'ensemble de cette implémentation. Les contrôles de protocole ci-dessous ont été exécutés depuis le runtime Android émulé, pas déduits des tests JVM. Les mesures de fluidité, FPS et jank sur TV physique ne sont pas réalisées et ne sont pas prouvées par cet émulateur logiciel.

## Préflight des sources

| Contrat | Observation | Statut |
| --- | --- | --- |
| Découverte Trakt | Une page officielle, point d'entrée puis module racine découvert dynamiquement : deux fichiers JavaScript, chacun sous le plafond ; configuration unique extraite du contexte `trakt-api-key`, aucune exécution de script | Vérifié |
| Acceptation du client | Lecture publique API et demande de code appareil réussies | Vérifié |
| Association | Échange du code autorisé puis lecture authentifiée du compte réussis ; présence d'identifiants stables et du nom d'affichage vérifiée sans publier leurs valeurs | Vérifié |
| Renouvellement | Renouvellement accepté, nouveau jeton de renouvellement et durée de 604800 secondes ; nouvelle lecture du compte acceptée | Vérifié |
| Configuration BetaSeries | Une page officielle puis cinq scripts nécessaires ; extraction compatible avec le parseur existant et appel API public accepté | Vérifié |
| Sources Trakt vides | Watchlists films/séries, progression et séries masquées renvoient des tableaux JSON vides distincts d'une erreur | Vérifié |
| Watchlist | Ajout des deux cibles de test explicitement autorisées et relecture de leur appartenance | Vérifié |
| Progression non commencée | Prochain épisode numéroté disponible dès l'ajout de la série en watchlist | Vérifié |
| Réserve inter-saisons | Après cinq marquages de test explicitement ciblés, prochain S01E06 puis cinq épisodes de saison 2 ; six candidats diffusés obtenus avec progression et deux lectures de saisons explicites | Vérifié |
| Statut et historique | Booléens `completed`, identifiants d'événement numériques et historique ciblé par épisode disponibles | Vérifié |
| Non vu ciblé | Le retrait des lectures de l'épisode ciblé conserve les quatre autres épisodes marqués pendant le test | Vérifié |
| Séries masquées | Ajout et retrait de `progress_watched` acceptés ; la progression générale continue à renvoyer la série masquée : le client doit appliquer explicitement la liste des masquages | Vérifié |
| Film vu/non vu | Vu retire le film de watchlist ; retirer son historique ne le réajoute pas. Le rétablissement de son état à voir nécessite un ajout ciblé à la watchlist | Vérifié |
| Événements multiples réels | Avec `watch_only_once`, les répétitions mettent à jour une lecture. Après désactivation temporaire explicitement autorisée par le propriétaire, deux événements distincts ont été créés puis supprimés par le Non vu ciblé ; les quatre autres épisodes restent vus | Vérifié |
| Nettoyage du jeu de test | Absence initiale contrôlée ; suppression des seules données créées, puis absence finale confirmée dans watchlists, masquages et historique des deux cibles | Vérifié |
| Restauration du réglage | Après le test de lectures multiples, la valeur initiale de `watch_only_once` est rétablie puis confirmée par relecture | Vérifié |

Les cibles de mutation ont été choisies et autorisées par le propriétaire dans la conversation. Le présent rapport ne publie ni titres, ni identifiants de contenus ou de compte, ni configuration cliente, ni code d'association, ni jeton.

## Connexion de diagnostic

À la demande du propriétaire, la dernière connexion de diagnostic est conservée et réutilisée, sans nouvelle association à chaque contrôle. Son enveloppe est chiffrée par AES-GCM avec une clé Android Keystore, dans un fichier privé de l'émulateur, avec remplacement atomique et écriture durable. Une recréation du runtime a vérifié sa restauration. Cette session n'est pas ajoutée au dépôt et n'est pas une preuve de l'implémentation du stockage applicatif à venir.

Deux erreurs de l'outil temporaire ont été corrigées avant les vérifications finales : persistance non synchronisée des fichiers au redémarrage et initialisation manquante du fournisseur Keystore dans un processus de diagnostic indépendant. Elles ne sont pas présentées comme des défauts de SygixOs.

## Conséquences confirmées pour l'implémentation

- Filtrer les séries masquées même si l'endpoint de progression les renvoie encore.
- Réconcilier l'appartenance watchlist lors d'un film non vu, sans acquitter une intention après la seule suppression de ses lectures.
- Ne pas répéter un marquage déjà vu : le service peut modifier sa lecture existante plutôt que créer un nouvel événement.
- Conserver les règles acceptées d'erreur, de budget, de cache, d'identité et d'isolation de session ; aucun autre client, secret fixe ou moteur web n'a été nécessaire.

Les validations du code applicatif, des migrations, de l'interface et des tests sont complétées au fur et à mesure ; aucun résultat de cette section ne vaut tâche d'implémentation déjà réalisée.

## Configuration et frontières communes

- Régression reproduite avant correction : le test de cache ancien BetaSeries attendait une seule découverte et en observait deux après l'échéance historique.
- Après correction : 44 tests ciblés réussis dans `BetaSeriesConfigTest`, `TraktConfigurationTest`, `TrackingHttpsTransportTest`, `BetaSeriesApiTest`, `BetaSeriesDeviceTest` et `TrackingSessionRecoveryApiTest` ; zéro échec, erreur ou test ignoré.
- Les tests contrôlent notamment cache persistant sans échéance, ressources officielles renommées, ambiguïté, rejet de domaine/redirection, annulation à l'échéance, protection contre un refus tardif de l'ancienne configuration et récupération partagée pour huit lectures refusées concurrentes.
- Le transport commun accepte les tableaux JSON Trakt sans trois lectures inutiles, préserve les corps JSON des mutations et expose les en-têtes de pagination ; le parcours d'association BetaSeries continue de passer après extraction de son protocole derrière le flux commun.
- Contrôle des prérequis : dix exigences MODIFIED ont une cible dans l'ordre d'archivage et conservent les noms de tous les scénarios cumulés. L'ordre reste récupération de session, état hors ligne, puis Trakt.

## Implémentation intégrée et preuves métier

- Suite complète exécutée séparément : `./gradlew test` avec instrumentation locale de progression, sans filtre de tests : **1348 tests réussis, zéro échec et zéro test ignoré**, en 55 secondes. Une commande groupant tests et construction release avait auparavant dépassé son délai ; elle n'est pas comptée comme réussite des tests.
- Deux tests supplémentaires de présentation par fournisseur ont ensuite été ajoutés et exécutés avec leurs classes : huit tests ciblés réussis. Ils vérifient noms, identité du héro, priorité du lecteur, badge disponible, retrait local et réaffichage pour BetaSeries et Trakt, sans changement de l'état du compte.
- Les régressions d'intégration ont été observées avec les anciens comportements réintroduits : une nouvelle association échouait avec ACCESS avant la première sauvegarde ; le renouvellement rendait la file introuvable si la liaison utilisait le texte complet des jetons. Elles passent avec validation explicite du candidat et empreinte du lien logique stable.
- Un test de stockage réel provoque une erreur SQLite au moment de libérer Annuler : Réessayer conserve maintenant la même révision et ne redémarre pas une fenêtre complète. Un autre vérifie que « Terminé ? » attend la fin d'une retenue existante au lieu de la masquer et de bloquer sa demande.
- La migration v2→v3 conserve les intentions héritées envoyables et crée le contexte durable d'Annuler seulement pour les nouveaux marquages. Les tests couvrent recréation de base, pauses, reprise après interruption, concurrence annulation/expiration, anciennes confirmations et protection du nettoyage.
- Le worker ignore intentions et réserves retenues ; une file uniquement annulable s'arrête sans requête API. Une autre demande libérée peut continuer. La convergence après réponse perdue relit la cible et reste portée par la file Room, sans seconde file de contenus dans les jetons d'authentification.
- Le lecteur mémorisé n'attend pas les offres facultatives. Le sélecteur manuel reste utilisable pendant leur chargement et après leur erreur, avec réessai dédié et confirmations invalidées au changement de pays.

### Contrats de métadonnées complémentaires vérifiés sur l'émulateur

Les en-têtes numériques de pagination sont présents. Les offres Trakt renvoient un objet par pays avec tableaux de catégories et champs `source`, `link`, `link_android`, `prices`, devise et UHD. Les métadonnées `full,images` exposent les images et la durée ; les épisodes d'une saison doivent être demandés avec `extended=full` pour obtenir `first_aired` et `runtime`. Le paramètre `extended=episodes` ne fournit pas ces champs sur l'endpoint testé ; l'implémentation utilise le contrat effectivement observé.

### Correspondance avec les exigences

| Capability | Implémentation | Preuves principales |
| --- | --- | --- |
| `tracking-application-configuration` | `TrackingApplicationConfiguration`, `TrackingOfficialResources`, sources BetaSeries/Trakt | `BetaSeriesConfigTest`, `TraktConfigurationTest`, récupération concurrente dans `BetaSeriesApiTest` et refus dans `TraktProtocolTest` |
| `trakt-integration` | `TraktApi`, `TraktDeviceProtocol`, `TraktSessionEnvelope`, `TraktProvider` | préflight authentifié ; `TraktProtocolTest`, `TrackingRuntimeProviderTest`, `TraktWorkerIntegrationTest` |
| `watch-tracking` | interfaces de fournisseur, registre, dépôt/contrôleur communs, intentions et contexte Annuler | `TrackingRepositoryTest`, `TrackingOfflineControllerTest`, `TrackingUndoDispatchTest`, `TrackingSyncTest`, tests de fin de session |
| `betaseries-integration` | adaptateur conservé, cache partagé sans échéance et protocole d'association derrière le flux commun | suites BetaSeries existantes, cache ancien et récupération de session |
| `settings` | choix persisté, liste partagée avec Position, verrouillage et contrôles adaptés | `TrackingSettingsTest`, `TrackingRuntimeProviderTest`, parcours de réglages existants |
| `launcher-shell` | projection commune, identités de héro séparées par fournisseur, notice et indicateur existants | `TrackingProjectionTest`, `TrackingHomeNavigationTest`, `TrackingOfflineHomeTest`, suites du héro |
| `up-next` | fusion, priorité de lecteur et masquages locaux inchangés | `UpNextTrackingMemoryTest`, suites Up Next et contenus retirés |
| `ui-testing` | sélecteurs nouveaux et conservés, tests D-pad à taille TV et cinq langues | suites Compose et `TranslationCatalogTest` |

Les contrôles d'APK installé, de navigation réelle et de cycle de vie sur l'émulateur restent distincts et ouverts dans le groupe 8 de `tasks.md`. La présence des tests JVM ne vaut pas observation de ces parcours sur appareil.

## Revue indépendante et correction d'appartenance

La revue du code `0724414` a relevé un point P2 : un historique de film ne prouve pas sa présence dans la watchlist. Le marquage vu est désormais conservé séparément et l'appartenance d'un film identifié découle seulement de sa watchlist. Le test `aWatchedMovieOutsideTheWatchlistCanBeAddedWithoutChangingItsHistory` a échoué avant correction puis réussi : Ajouter est autorisé, Retirer est refusé pour ce contenu absent, l'ajout ne modifie pas son historique. La classe `TraktProtocolTest` complète passe après correction. La relecture du diff correctif reste distincte du verdict initial.

## Vérification du code avant review complémentaire

- Code relu : `6e1e9d6`, verdict indépendant **MERGEABLE** après correction du point P2. La relecture incrémentale ne relève aucun nouveau défaut concret ; elle ne vaut ni CI ni autorisation de merge.
- Suite complète du code corrigé : **1351 tests réussis, zéro échec, zéro ignoré**, en 56 secondes, avec le même script local de journalisation et sans filtrage.
- `assembleRelease`, `assembleDebug` et `assemblePerf` ont réussi séparément de la suite. La release standard est construite ; l'installation runtime utilise un identifiant d'application isolé et un fixture débogable. Ce fixture ne fournit pas de mesure de performance de la release ni du matériel.

## Parcours observés sur l'émulateur

| Scénario | Preuve observée | Statut |
| --- | --- | --- |
| Restauration Trakt et contribution | Session chiffrée restaurée, contrôles connectés et attribution Trakt, cartes série/film, métadonnées et images visibles | Vérifié |
| Menu et saisons | Série commencée avec Archiver plutôt que Retirer, épisodes vus distingués, retour de focus sans ouverture involontaire | Vérifié |
| Choix du fournisseur et association BetaSeries | Liste existante au D-pad, choix sans association implicite, code/adresse complète/QR puis fermeture sans autorisation | Vérifié pour l'UI ; compte BetaSeries authentifié non exercé |
| Marquage local retenu | Changement de prochain épisode et avis sans prise de focus ; base avec `undoHeld=1`, `possiblySent=0` | Vérifié |
| Pause et restauration d'Annuler | Focus sur Annuler suspendant le compteur ; arrêt forcé et lancement à froid conservant intention, contexte et temps restant | Vérifié ; aucune précision de durée GPU déclarée |
| Annuler au D-pad | Une intention retenue supprimée après validation d'Annuler ; les intentions antérieures restent présentes et aucune émission du marquage annulé n'était engagée | Vérifié |
| Lecteur et retour | Application vidéo contrôlée ouverte avec repli applicatif explicite ; retour proposant « Terminé ? » ; absence de nouveau marquage en ignorant la proposition | Vérifié pour la délégation et le cycle de vie ; pas de décodage vidéo réel |
| Cache après simulation de panne | Cartes et données durables conservées après changement temporaire de proxy et redémarrage | Observé ; blocage effectif de tous les appels non établi |
| Synchronisation automatique en arrière-plan | Demandes libérées conservées ; le JobScheduler de l'émulateur attendait la contrainte CONNECTIVITY/VALIDATED malgré des appels API directs possibles | Non vérifié automatiquement dans cet état du réseau ; convergence métier et worker couverts en JVM |

Deux passages indépendants ont été complétés par des contrôles du processus principal sur Annuler. Les captures utiles ont été transmises au propriétaire dans son canal privé, sans upload public et sans chemin local dans le présent rapport. Les écrans runtime ont suivi la langue du fixture ; les textes français à taille TV et les cinq catalogues sont couverts par les tests Compose et de complétude, pas par une prétendue visite runtime de chaque traduction.

### Remise en état

Les deux cibles distantes de test ont retrouvé leur absence initiale : watchlists, historique ciblé et masque de progression contrôlés vides. Les intentions et contextes locaux de l'installation isolée ont été nettoyés après arrêt du processus pour empêcher une réémission ultérieure. L'autorisation Trakt reste conservée dans son stockage protégé, sans nouveau code. Le proxy est revenu à son état initial et l'émulateur a été arrêté. Les anciens incidents des outils CLI temporaires ne sont pas comptés comme des crashes produit.

## Rapport OpenSpec

| Dimension | Résultat |
| --- | --- |
| Completeness | 32/34 tâches ; les 23 exigences ADDED/MODIFIED ont une implémentation identifiée. Les tâches runtime 8.2 et 8.3 restent ouvertes avec les limites ci-dessus. |
| Correctness | Correspondance code/tests pour les 23 exigences et examen des 140 scénarios ; couverture runtime partielle, explicitement non assimilée aux preuves JVM. |
| Coherence | Socle partagé, une base et une file Room, frontières par fournisseur, configuration et jetons séparés, composants et navigation existants réutilisés ; préférence de lecteur hors compte/fournisseur toujours réservée à l'autre session demandée. |

### CRITICAL — avant archivage

- Terminer 8.2 : validation complète des deux associations et des cas de migration/cache ancien sur APK installé, au-delà des preuves contrôlées et de l'UI déjà observée.
- Terminer 8.3 : convergence automatique réelle après coupure/rétablissement du réseau pendant une autre application et autres combinaisons runtime non exercées ; ne pas assimiler l'attente de validation réseau Android à une confirmation distante.

### WARNING — limites de preuve

- La TV physique et les mesures FPS/jank sont écartées explicitement par le propriétaire ; elles ne sont pas une preuve manquante cachée.
- Le fixture runtime est débogable ; la release standard est construite mais ses performances sur matériel ne sont pas évaluées.

Les contrôles de code réalisés ne présentent plus de constat de revue bloquant. La PR d'implémentation peut exposer ces preuves et limites ; **le change n'est pas déclaré prêt pour archivage**, et aucun archivage ni merge n'est effectué ici.

## Corrections de la review complémentaire

La review complémentaire de `8ea1250` a reproduit six défauts supplémentaires par tests ciblés et identifié deux autres défauts par lecture croisée du code et de ses tests. Le verdict historique ci-dessus ne couvre pas ces constats. Les corrections ci-dessous ont été réalisées sur demande du propriétaire, avec validation explicite du parcours Reconnecter après renouvellement incertain et de l'intégration par fixups signés/autosquash.

| Constat | Statut et correction | Preuve |
| --- | --- | --- |
| Identités Trakt relues comme BetaSeries | Traité : cache versionné et clés isolées par fournisseur ; format historique ambigu écarté | `TrackingIdentityCacheTest` : aller-retour disque, mêmes IDs numériques, bascule entre fournisseurs et cache historique |
| Renouvellement incertain bloqué sur Réessayer | Traité : état RECONNECT sans suppression des contenus ni intentions ; entrée dans l'association depuis réglages, détail, épisodes, saisons et offres ; panne de sauvegarde récupérable conservant Réessayer | `TraktProtocolTest.lostRefreshResponseLeavesDurableUncertaintyAndNeverReissuesToken`, `receivedRotatedPairCanRepairStorageWithoutExchangingAgain`, `TrackingOfflineControllerTest.uncertainSessionReassociationRetainsSameAccountIntentAndPurgesOnlyAfterDifferentAccountConfirmation`, `TrackingSettingsTest.uncertainSessionOffersReconnectionWithoutDisconnectingOrDiscardingAccount` |
| Épisode Trakt sans date annulant une série valide | Traité : filtrage de l'admissibilité avant lecture du statut dans réserve et liste de saison | `TrackingRecoveryRegressionTest.undatedFutureEpisodeDoesNotDiscardAiredTraktEpisode` |
| Entrée BetaSeries malformée annulant les films valides | Traité : conversion isolée par groupe et épisode, résultat partiel conservant les contenus valides et empêchant une purge fondée sur l'absence | `TrackingRecoveryRegressionTest.malformedBetaSeriesGroupKeepsValidMovie` |
| Ancien prochain épisode conservé après complément complet | Traité : réconciliation de la fenêtre ciblée, sans purge sur résultat partiel, avec protection des intentions, références et révisions postérieures | `TrackingRecoveryRegressionTest.completeReserveReplacesStaleUnseenCandidates`, `TrackingRepositoryTest.reserveReconciliationProtectsPendingReferencedAndNewerEntriesAndNeverPurgesPartialResults` |
| Succès du worker effaçant une erreur de compte | Traité : erreurs snapshot et synchronisation séparées, résultat global dérivé ; migration Room v3→v4 conservatrice | `TrackingRecoveryRegressionTest.successfulReserveDoesNotClearUnrecoveredSnapshotFailure`, `TrackingRepositoryTest.successfulSnapshotDoesNotHidePendingSynchronizationFailure`, `migrationFromVersionThreeRetainsSnapshotErrorAndHeldUndo` |
| Découvertes répétées après échec/rejet de configuration | Traité : résultat d'échec partagé, reprise explicite ou après une minute au prochain besoin ; refus tardif de la même génération sans remise à zéro de cet échec | `TrackingRecoveryRegressionTest.rejectedConfigurationDiscoveryIsSharedAcrossConsumers`, `TraktConfigurationTest.failedDiscoveryIsSharedUntilExplicitRetryOrBoundedRecoveryDelay`, consommateurs concurrents et échéance de découverte |
| Statut d'indisponibilité affiché deux fois | Traité : détail affiché seulement s'il diffère du statut principal, dans les réglages et le panneau de détail | `TrackingSettingsTest.unavailableStatusAppearsOnceAndKeepsSyncFocus`, tests Compose à taille TV |

Deux lectures redondantes sont également corrigées : une identité connue réutilise le snapshot frais avant de relire son statut (`TrackingEnrichmentTest.restoredIdentityReusesFreshSnapshotButReadsAgainAfterItsFreshnessExpires`) ; une panne des films ne force pas une seconde lecture des réserves déjà complètes (`TrackingRecoveryRegressionTest.failedMovieCollectionDoesNotForceReloadOfACompleteEpisodeReserve`). La coalescence supplémentaire des sollicitations WorkManager et l'optimisation des gros comptes Trakt restent des pistes non implémentées : elles nécessitent respectivement une preuve de réveil sans perte et une mesure des lectures avant modification des budgets ou de la concurrence.

### Vérifications des corrections

- `./gradlew test` : **1367 tests réussis, zéro échec, zéro ignoré**, dont 16 tests supplémentaires. Les six reproductions initiales échouaient sur les comportements attendus avant correction. La suite vérifie aussi les régressions BetaSeries, le nettoyage, la migration depuis les anciennes bases et les cinq catalogues de traduction.
- Le test de changement de jeton pendant lecture utilise un dispatcher réel pour ses opérations bloquées par la base Room, afin que l'avance automatique de l'horloge virtuelle ne transforme pas l'attente de cette base en expiration fictive de découverte. Les échéances réseau restent testées séparément avec horloges contrôlées.
- `assembleDebug` et `assembleRelease` ont produit les APK corrigés ; debug : 49318161 octets, release : 6176753 octets. Une commande regroupant tests et builds a dépassé son délai après génération des APK ; elle n'est pas la preuve de réussite de la suite. La commande `./gradlew test` séparée ci-dessus a terminé avec succès.
- `openspec validate --all --strict` : 12 éléments valides, zéro échec. Le diagnostic informatif d'archivage rappelle la dépendance à l'archivage préalable du suivi hors ligne ; aucun archivage n'est tenté.
- Vérification directe de l'APK debug standard sur émulateur Android TV : installation sans effacement des données, lancement, navigation vers Suivi de visionnage, sélection de Trakt, retour vers les réglages, choix conservé et focus sur le contrôle attendu. Le choix initial BetaSeries a été rétabli ; aucun crash dans le journal dédié. Captures envoyées en privé et émulateur arrêté.
- Aucun compte n'était connecté dans cette installation standard. Les états d'erreur connectée, de renouvellement incertain et la réassociation authentifiée ne sont donc pas déclarés observés sur l'émulateur lors de ce passage ; leur preuve reste les tests métier/Room et Compose, distincte des parcours historiques du fixture isolé.

### Bilan OpenSpec actualisé

| Dimension | Résultat |
| --- | --- |
| Completeness | 32/34 tâches ; les tâches 8.2 et 8.3 restent ouvertes. Aucun scénario historique non observé n'a été coché par la réussite des nouveaux tests. |
| Correctness | Correspondance des exigences relue sur les fichiers affectés : configuration, session/identités Trakt, résultat partiel BetaSeries, réserve, erreurs et présentation. Preuves JVM actualisées ; les preuves runtime historiques des chemins non rejoués ne sont pas une validation complète du nouveau code. |
| Coherence | Une base Room avec migration non destructive, erreurs persistantes distinctes, cache d'identités isolé et interfaces partagées. Spec, design et README précisent le parcours de reconnexion accepté. |

Les deux tâches runtime incomplètes restent des points critiques avant archivage. La réassociation authentifiée après perte réelle de réponse, les états connectés dans l'APK corrigé et les performances matérielles ne sont pas vérifiés lors de ce passage. Les huit défauts ont une correction et des preuves automatisées ; le change n'est toujours pas déclaré prêt pour archivage.

## Validation sur la TV de référence (6 novembre 2026)

APK publié v0.6.0-rc.1, TV Google TV sous Android 14, comptes réels ; toute mutation a été annulée sur le compte.
- Déconnexion BetaSeries, choix de Trakt dans le sélecteur, Connecter : QR et code Trakt affichés en quelques secondes ; autorisation sur la page officielle ; compte associé puis synchronisé.
- Programmes locaux identifiés par Trakt et action « Ajouter au suivi » effective (vérifiée puis retirée sur le compte).
- Retour à BetaSeries : déconnexion Trakt, nouveau choix et nouvelle association, compte synchronisé.
- Le marquage avec retenue d'Annuler reste couvert par les tests et l'émulateur ; le compte Trakt de test ne contenait aucun contenu suivi.
- Défaut relevé et traité dans v0.6.0 : après un ajout au suivi, la rangée annonçait une liste de compte incomplète jusqu'à une synchronisation manuelle.
