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
