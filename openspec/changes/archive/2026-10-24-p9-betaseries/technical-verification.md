# Vérifications techniques

Contrats et structures observés les 23–24 octobre 2026, sans identifiant, clé, jeton ni historique personnel. Les essais publics, retours de TV et fixtures sont distingués.

## 0. Corrections confirmées sur TV

Retour de validation externe sur le build initial : l'échange avec `device_code` reste en erreur 2001 après autorisation. Avec le seul remplacement par **`code`**, l'association aboutit en quelques secondes ; ce paramètre reçoit la valeur du device_code, conformément à « Identification by code ».

Le premier instantané échoue aussi lorsque `episodes/list` demande 100 épisodes par série : réponse supérieure à 2 Mio. La correction demande `limit=1`, `showsLimit=100`, `released=1`, conserve les limites explicites dans la pagination, décode hors thread principal et ne charge que les films state=0. Une erreur de page ou de fiche reste partielle et ne retire pas les autres entrées valides. L'association corrigée est confirmée par le validateur sur TV ; le chargement corrigé reste à y contrôler.

La validation externe établit que `page` est ignoré par `shows/member` et `movies/member` : utiliser respectivement **offset** et **start**, à partir de zéro. Les compteurs `totalMissingShows/Movies` peuvent être zéro dès le premier lot ; la fin exige le total atteint ou une page vide cohérente, avec détection des pages répétées.

`shows/episodes` ne pagine pas non plus : les décalages renvoient la même liste complète. La correction utilise **season**, saison courante d'abord ; `shows/seasons` fournit les numéros, les autres épisodes ne sont chargés que sur choix explicite. Le SDK officiel et son test de saisons décrivent number/episodes/seen. Les POST vu et archive, avec relecture de fiche, ont été validés sur TV par le validateur ; DELETE et Annuler restent à y vérifier sur la version corrigée. Annuler dure 10 secondes actives, survit aux réémissions/relectures et suspend son reliquat au focus ou en arrière-plan.

La correction finale confirme un épisode muté par `episodes/display` puis, seulement pour vu, demande le prochain candidat avec `episodes/list` filtré par showId (paramètre du SDK officiel). Cette seconde lecture ciblée évite de charger une saison entière ou le compte et couvre la frontière de saison ; son fonctionnement authentifié dans l'application corrigée reste dans 1.4. Tests : BetaSeriesApiTest.seenCrossesSeasonBoundaryAndUndoRestoresOneCandidateThroughRealReadbacks et repeatedEmptyAndMalformedPagesPreservePreviouslyDecodedItems ; le changement de pays suspendu est couvert par BetaSeriesOffersTest.changingCountryRechecksOnlyPlatformsAndNeverReloadsAccount.

Pour `episodes/list`, le SDK documente `page` et `showsLimit`, mais aucun décalage offset ; le compte testé tient dans un lot. Sans nouvel essai authentifié autorisé ici, le cas de plus de 100 séries reste ouvert en 1.3 : le code utilise le paramètre documenté et refuse une page répétée plutôt que déclarer la liste complète. Le SDK expose `shows/member.status` et `summary`, sans preuve qu'un filtre unique conserve toutes les séries non archivées avec les images et états indispensables ; ces paramètres ne sont pas activés par hypothèse. La recherche locale utilise le titre de série nettoyé et cinq candidats maximum, avec cache par clé locale.

## 1. Résultats des essais API

| Vérification | Résultat observé | Conclusion |
| --- | --- | --- |
| Découverte du client web | HTML officiel puis bundle de configuration découvert dynamiquement ; champs `apiHost` et `apiKey` trouvés dans des données statiques | Extraction automatique réalisable aujourd'hui ; pas de valeur à embarquer ni de JS à exécuter |
| Association appareil | `POST /oauth/device` : HTTP 200, `device_code:string`, `user_code:string`, `verification_url:string`, `expires_in:int`, `interval:int` | Code + adresse + QR réalisables ; le code doit rester saisissable depuis un PC |
| Attente OAuth | `POST /oauth/access_token`, client_id et **code** (valeur de device_code), sans client_secret : HTTP 400 erreur 2001 avant autorisation | Respecter l'intervalle ; l'ancien essai avec device_code ne prouvait pas le contrat |
| Autorisation complète | Échange avec code confirmé sur TV par le validateur | Jeton utilisable ; aucune association réelle effectuée pendant les corrections |
| Compte | `GET /members/infos` avec ce jeton : HTTP 200, objet `member`, identifiant, nom et `options.country/language` | Compte identifié et pays de compte disponibles ; aucune valeur personnelle conservée dans les documents |
| Épisodes à regarder | `GET /episodes/list`, limit=1, showsLimit=100, released=1 : lot unique confirmé sur TV | `shows`, `total`, `totalEpisodes`, compteurs et `unseen` ; pagination de plus de 100 séries encore à établir |
| Séries du compte | `GET /shows/member`, offset=0 puis décalages successifs : confirmé sur TV | Archive, images, identifiants et total ; ne pas utiliser page ni totalMissingShows comme fin |
| Films du compte, premier essai | `GET /movies/member`, state=0 : HTTP 502, corps texte non JSON | Panne serveur réelle, à traiter distinctement du vide |
| Films du compte, nouvel essai | `GET /movies/member`, state=0 et start successifs : confirmé sur TV | Identifiants, images, année, état et total ; offset/page ignorés, aucun besoin de charger state=1/2 pour l'appartenance |
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

Le contrôle public du 24 octobre confirme des listes de plateformes différentes pour deux pays, sans jeton utilisateur. Les fiches publiques série/film exposent leurs plateformes ; leur filtre country n'a pas été démontré. La décision P9 croise leurs identifiants avec les services du pays confirmé : confirmation au niveau service, jamais garantie d'épisode ni d'abonnement.

### Champs accessibles

- Série : `platforms.svods[]` et parfois `platforms.svod`, identifiant, nom, logo, `link_url`, plage `available.first/last` facultative. Les nombres reçus ne sont pas interprétés comme une garantie par épisode sans validation supplémentaire.
- Film : `platforms_svod[]`, id, type, date/jour facultatifs, `link_url`, et `platform_links[]`.
- Les liens observés passent par le domaine BetaSeries. Leurs destinations et leur résolution Android n'ont pas été validées sur TV.

### Comportement validé avec Sygix

Sans pays confirmé ou sans service correspondant, afficher « Disponibilité non confirmée » ; conserver contenus et choix manuel. Changer de pays relit uniquement les plateformes. Premier choix obligatoire même avec une seule offre, puis préférence commune héro/À suivre ; replis ciblés seulement s'ils ont été vérifiés.

## 4. Mutations : documentation, pas d'écriture réelle

Le [SDK officiel](https://github.com/betaseries/betaseries-api-redux-sdk/tree/f1bdf45103a53246fa7e3000d02126912c7ac8dd) a été lu pour compléter les pages d'API qui n'exposent pas leurs schémas.

| Contrat | Preuve source | Conséquence |
| --- | --- | --- |
| `POST episodes/watched` | `lib/modules/shows/actions/doMarkEpisodeAsWatched.js` : paramètre bulk, valeur par défaut true | L'intégration doit imposer bulk=false pour l'épisode seul |
| `DELETE episodes/watched` | `doUnmarkEpisodeAsWatched.js` : id en query | Même encodage pour les DELETE de retrait et désarchivage ; effets réels à vérifier sur un contenu autorisé |
| `POST movies/movie` | `lib/modules/movies/actions/doUpdateMovieState.js` : 0 à voir, 1 vu, 2 ne veut pas voir | Non vu n'est pas supprimer ; restaurer l'état antérieur explicitement |
| Retirer une série | `doRemoveShow.js` : DELETE shows/show | Aucune garantie que réajouter restitue tout l'historique ; envoi différé pendant Annuler |
| Archiver/désarchiver | Actions distinctes `doAddShowArchive.js` / `doRemoveShowArchive.js` | Distinguer archive, retrait du compte et masquage local |

Aucune de ces mutations n'a été exécutée sur le compte utilisé pour le test. La validation avant code doit utiliser un contenu de test explicitement autorisé et vérifier aussi les épisodes voisins et l'état avant/après.

## 5. Contrats Android vérifiés par source

- [MediaSessionManager, source AOSP](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/media/java/android/media/session/MediaSessionManager.java) : `getActiveSessions` exige MEDIA_CONTENT_CONTROL ou un NotificationListenerService activé. Une app launcher ordinaire n'a pas implicitement ce droit.
- [ApplicationInfo, source AOSP](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/content/pm/ApplicationInfo.java) : CATEGORY_VIDEO existe, mais sa déclaration n'est pas exhaustive. D'où lecteurs reconnus + Autre application.
- [TvContract, source AOSP](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/media/java/android/media/tv/TvContract.java) : Watch Next expose position/activité/types, pas une preuve universelle de fin du bon contenu.
- Code existant : UpNextContentKey masque la série entière, UpNextFeed priorise déjà les ouvertures locales, puis CONTINUE/NEXT/WATCHLIST, et UpNextController n'a actuellement qu'une source ; AppLauncher possède des chemins séparés héro et cartes, à mutualiser pour le suivi.

## 7. Références officielles

- [Authentification et association par code](https://developers.betaseries.com/docs/getting-started/authentication)
- [oauth/device](https://developers.betaseries.com/docs/api/post-oauth-device) et [oauth/access_token](https://developers.betaseries.com/docs/api/post-oauth-access-token)
- [Plateformes par pays](https://developers.betaseries.com/docs/api/get-platforms-list), [série](https://developers.betaseries.com/docs/api/get-shows-display), [film](https://developers.betaseries.com/docs/api/get-movies-movie)
- [Requêtes et erreurs](https://developers.betaseries.com/docs/getting-started/making-requests) et [conditions d'utilisation/attribution](https://developers.betaseries.com/docs/getting-started/terms)

Le lien OpenAPI indiqué dans la documentation renvoyait une 404 au moment du cadrage. Le schéma manquant n'a pas été reconstruit comme une certitude ; les types ci-dessus viennent des essais ou du SDK cité.
