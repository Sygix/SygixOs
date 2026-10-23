# Vérifications techniques

Vérifications effectuées le 23 octobre 2026. Ce document conserve les contrats, statuts et limites, sans clé, jeton, code d'association, identité de compte, titre personnel ou réponse brute. Les essais réseau ont été faits par une sonde locale temporaire, pas par une implémentation de l'application.

## 1. Résultats des essais API

| Vérification | Résultat observé | Conclusion |
| --- | --- | --- |
| Découverte du client web | HTML officiel puis bundle de configuration découvert dynamiquement ; champs `apiHost` et `apiKey` trouvés dans des données statiques | Extraction automatique réalisable aujourd'hui ; pas de valeur à embarquer ni de JS à exécuter |
| Association appareil | `POST /oauth/device` : HTTP 200, `device_code:string`, `user_code:string`, `verification_url:string`, `expires_in:int`, `interval:int` | Code + adresse + QR réalisables ; le code doit rester saisissable depuis un PC |
| Attente OAuth | `POST /oauth/access_token`, client_id et device_code, sans client_secret : HTTP 400 avec erreur 2001 « en attente » | L'attente n'est pas un échec de secret et doit respecter l'intervalle |
| Autorisation complète | Après autorisation de l'utilisateur, même échange sans client_secret : jeton `access_token` et `token_type` reçus | Variante du client web testée de bout en bout, alors que la documentation générale demande un secret |
| Compte | `GET /members/infos` avec ce jeton : HTTP 200, objet `member`, identifiant, nom et `options.country/language` | Compte identifié et pays de compte disponibles ; aucune valeur personnelle conservée dans les documents |
| Épisodes à regarder | `GET /episodes/list`, limit=1, showsLimit=1, page=1, released=1 : HTTP 200 | Enveloppe `shows`, `total`, `totalEpisodes`, `totalMissingShows`, `totalMissingEpisodes`, liste `unseen` et champs de suivi observés |
| Séries du compte | `GET /shows/member` : HTTP 200 avec `shows`, `total`, `totalMissingShows` | Archive, prochain épisode, images et identifiants externes disponibles ; cela ne valide pas une lecture exhaustive de toutes les pages |
| Films du compte, premier essai | `GET /movies/member`, state=0 : HTTP 502, corps texte non JSON | Panne serveur réelle, à traiter distinctement du vide |
| Films du compte, nouvel essai | Après nouvelle association, `GET /movies/member`, state=0 : HTTP 200 avec `movies`, `total`, `totalMissingMovies` | Mapping de liste observé : identifiants, images, année, `user.status/date`, `auth_user`, liens et plateformes |
| Fin des deux sessions authentifiées | `POST /members/destroy` : HTTP 200 sans erreur pour chaque jeton | Jetons de test révoqués ; aucune écriture de suivi, retrait ou archivage effectuée |

Une attente OAuth a aussi reçu une réponse non JSON : la première sonde s'est arrêtée, puis la version suivante a traité cet état comme une erreur transitoire bornée. Un code déjà consommé a été signalé comme absent par la page de saisie ; le test avait entre-temps obtenu le jeton. Le produit doit rendre ces états compréhensibles, sans diagnostiquer chaque code invalide comme une panne de clé.

Le second essai films a été préparé avec reprises de lecture bornées, mais sa première lecture a réussi : la récupération réelle après une séquence 502 puis retry dans la même session n'a pas été démontrée. Elle sera couverte par un transport contrôlé et par les validations d'intégration.

## 2. Forme des données observées

- Séries : identifiants BetaSeries et `thetvdb_id`, `imdb_id`, `themoviedb_id` ; titres et alias ; images `show/banner/box/poster`, clearlogo facultatif ; `user.archived`, `user.remaining`, `user.last`, `user.next`.
- Épisodes : id, `thetvdb_id`, série, saison, épisode, code, date de diffusion, durée, `user.seen`, `user.seen_date` éventuellement nul ; `platform_links` peut être vide.
- Films : id, IMDb/TMDB, année, poster et backdrop, durée, état du compte, date utilisateur, offres et liens. La présence d'une date chaîne ne prouve pas sa signification exacte pour le tri.
- Pays : `member.options.country` est une chaîne. Cette option n'est pas une preuve de la région d'une offre ; les réglages conservent le pays confirmé par l'utilisateur.
- Activité : `show.user.last` est une chaîne de code d'épisode, pas une date ; aucune date globale de dernière action par série n'a été établie. Ne pas utiliser l'heure de synchronisation ou la date de diffusion comme activité utilisateur.
- Aucune réponse testée ne fournit une position de lecture vidéo via le simple statut vu/non vu.

## 3. Disponibilités régionales et ouverture

### Essais publics

`GET /platforms/list?country=…` a été testé pour trois pays. Les groupes SVOD/VOD et leur cardinalité varient selon le pays : le paramètre est donc opérant pour cette liste de services.

Sur une série et un film publics de référence, `shows/display` et `movies/movie` ont ensuite été appelés avec chacun de ces pays. Les offres reçues sont restées identiques. Cela ne démontre pas que country fonctionne pour l'offre d'un contenu ; une intersection avec les services du pays serait une approximation et n'est pas retenue comme preuve.

### Champs accessibles

- Série : `platforms.svods[]` et parfois `platforms.svod`, identifiant, nom, logo, `link_url`, plage `available.first/last` facultative. Les nombres reçus ne sont pas interprétés comme une garantie par épisode sans validation supplémentaire.
- Film : `platforms_svod[]`, id, type, date/jour facultatifs, `link_url`, et `platform_links[]`.
- Les liens observés passent par le domaine BetaSeries. Leurs destinations et leur résolution Android n'ont pas été validées sur TV.

### Comportement validé avec Sygix

Afficher « Disponibilité non confirmée » quand la région n'est pas prouvée ; conserver les contenus et le choix manuel. Une app installée ne prouve ni abonnement ni disponibilité. Premier choix confirmé même avec une seule offre, puis préférence commune héro/À suivre. Fiche, recherche et ouverture de l'app restent des replis explicites.

## 4. Mutations : documentation, pas d'écriture réelle

Le [SDK officiel](https://github.com/betaseries/betaseries-api-redux-sdk/tree/f1bdf45103a53246fa7e3000d02126912c7ac8dd) a été lu pour compléter les pages d'API qui n'exposent pas leurs schémas.

| Contrat | Preuve source | Conséquence |
| --- | --- | --- |
| `POST episodes/watched` | `lib/modules/shows/actions/doMarkEpisodeAsWatched.js` : paramètre bulk, valeur par défaut true | L'intégration doit imposer bulk=false pour l'épisode seul |
| `DELETE episodes/watched` | `doUnmarkEpisodeAsWatched.js` | Opération inverse de statut disponible ; ses effets exacts seront vérifiés sur un contenu de test autorisé |
| `POST movies/movie` | `lib/modules/movies/actions/doUpdateMovieState.js` : 0 à voir, 1 vu, 2 ne veut pas voir | Non vu n'est pas supprimer ; restaurer l'état antérieur explicitement |
| Retirer une série | `doRemoveShow.js` : DELETE shows/show | Aucune garantie que réajouter restitue tout l'historique ; envoi différé pendant Annuler |
| Archiver/désarchiver | Actions distinctes `doAddShowArchive.js` / `doRemoveShowArchive.js` | Distinguer archive, retrait du compte et masquage local |

Aucune de ces mutations n'a été exécutée sur le compte utilisé pour le test. La validation avant code doit utiliser un contenu de test explicitement autorisé et vérifier aussi les épisodes voisins et l'état avant/après.

## 5. Contrats Android vérifiés par source

- [MediaSessionManager, source AOSP](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/media/java/android/media/session/MediaSessionManager.java) : `getActiveSessions` exige MEDIA_CONTENT_CONTROL ou un NotificationListenerService activé. Une app launcher ordinaire n'a pas implicitement ce droit.
- [ApplicationInfo, source AOSP](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/content/pm/ApplicationInfo.java) : CATEGORY_VIDEO existe, mais sa déclaration n'est pas exhaustive. D'où lecteurs reconnus + Autre application.
- [TvContract, source AOSP](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/media/java/android/media/tv/TvContract.java) : Watch Next expose position/activité/types, pas une preuve universelle de fin du bon contenu.
- Code existant : UpNextContentKey masque la série entière, UpNextFeed priorise déjà les ouvertures locales, puis CONTINUE/NEXT/WATCHLIST, et UpNextController n'a actuellement qu'une source ; AppLauncher possède des chemins séparés héro et cartes, à mutualiser pour le suivi.

Ce sont des vérifications de contrat et de code, pas un test de disponibilité des écrans système, d'intents ou de métadonnées sur une TV.

## 6. Validations restantes avant implémentation

1. Sur TV Google TV sous Android 14 : vérifier l'écran d'accès notifications/sessions, refus, accord et révocation, sans présumer de sa présence constructeur.
2. Relever structure et complétude des identités et événements de session par app testée ; établir quelles observations prouvent réellement la fin du bon épisode/film. Sans preuve, repli manuel obligatoire.
3. Tester les liens/intents par app et leur repli : épisode, fiche, recherche, lancement simple ; noter la précision réelle sans données personnelles.
4. Vérifier la pagination complète séries/épisodes/films et la sémantique des dates sur fixtures contrôlées ou données autorisées ; résultat partiel interdit pour une purge.
5. Vérifier vu/non vu, bulk=false, effets des suppressions et archive sur des contenus de test autorisés, sans utiliser l'historique personnel comme fixture publique.
6. Vérifier sur Android le stockage protégé, le changement de compte et le rejet des réponses tardives ; le test Python ne valide pas une implémentation Keystore.

Ces points sont les premières tâches du change. Ils ne bloquent pas sa PR de spécification ni la future passe Penpot, mais empêchent de déclarer les parcours concernés implémentés ou universellement compatibles.

## 7. Références officielles

- [Authentification et association par code](https://developers.betaseries.com/docs/getting-started/authentication)
- [oauth/device](https://developers.betaseries.com/docs/api/post-oauth-device) et [oauth/access_token](https://developers.betaseries.com/docs/api/post-oauth-access-token)
- [Plateformes par pays](https://developers.betaseries.com/docs/api/get-platforms-list), [série](https://developers.betaseries.com/docs/api/get-shows-display), [film](https://developers.betaseries.com/docs/api/get-movies-movie)
- [Requêtes et erreurs](https://developers.betaseries.com/docs/getting-started/making-requests) et [conditions d'utilisation/attribution](https://developers.betaseries.com/docs/getting-started/terms)

Le lien OpenAPI indiqué dans la documentation renvoyait une 404 au moment du cadrage. Le schéma manquant n'a pas été reconstruit comme une certitude ; les types ci-dessus viennent des essais ou du SDK cité.
