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
