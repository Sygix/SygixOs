# Design

## Context

Voir `proposal.md` pour la motivation et le périmètre validé. Le code actuel sépare `HeroRepository`/`HeroItem` et `UpNextController`/`UpNextItem`, avec deux chemins d'ouverture dans `AppLauncher`. `UpNextFeed` sait fusionner des programmes Android et trier les ouvertures locales ; `externalIds` existe mais n'est pas alimenté. Les identités de mémoire sont aujourd'hui fondées sur les titres normalisés. `HttpTransport` ne propose que GET ; le manifeste n'a pas de service de lecture des notifications.

Les vérifications de `technical-verification.md` distinguent documentation, essais API et hypothèses sur la TV. Le flux appareil a effectivement délivré un jeton sans `client_secret` avec la configuration du client web ; cela ne rend ni l'extraction du site ni cette variante un contrat public stable.

## Goals / Non-Goals

**Goals:**

- Faire converger les deux surfaces vers une identité et un parcours de lecture communs, sans réécrire leur rendu.
- Séparer fournisseur de suivi, lecteur et preuve de visionnage ; conserver la possibilité d'un autre fournisseur sans exposer une option vide.
- Garantir la distinction succès/vide/erreur/incomplet et l'isolation des sessions, sans arrière-plan de synchronisation réseau permanent.
- Conserver les décisions de la passe Penpot et compléter sa navigation avant l'implémentation de l'UI.

**Non-Goals:**

- Déduire les catalogues privés, les abonnements, le contenu d'une bibliothèque personnelle ou une fin de lecture depuis la seule ouverture d'une app.
- Inventer un endpoint d'expiration/rotation de secret, un refresh token ou un accès de niveau système.
- Ajouter une seconde source de disponibilités régionales : le repli validé est « Disponibilité non confirmée ».

## Decisions

### D1 — Frontières métier et état unique

Introduire des contrats restreints : `WatchTrackingProvider` (session, lecture paginée, identification et mutations), `TrackingSessionStore`, `ContentIdentityResolver`, `PlaybackAppResolver`, `PlaybackLauncher` et `PlaybackEvidenceSource`. BetaSeries implémente le fournisseur ; une application vidéo est un lecteur et non un fournisseur de suivi. Un `TrackingController` expose le compte et ses opérations en StateFlow ; le `PlaybackCoordinator` sert les actions du héro, d'À suivre et de leurs menus.

```text
BetaSeries --> suivi canonique ----+
                                 +--> fusion --> projection Up Next (20)
TV Provider --> programmes locaux -+         +--> candidats hero (tous)
                                             |
hero / carte / menu --> PlaybackCoordinator --+--> lecteur choisi
                                             +--> preuve / confirmation
                                             +--> mutation du compte
```

Les ViewModels traduisent l'état et les intents ; les composables rendent les états. Pas de chargement API autonome par carte, pas de compte dupliqué par surface. Les notifications de session média alimentent seulement l'observation demandée ; réseau et rafraîchissements du compte s'effectuent au premier plan ou pour terminer une action explicitement engagée.

Alternative écartée : ajouter BetaSeries comme faux package dans le TV Provider ou comme app de lecture. Cela couplerait badge, filtre d'app, autorisation locale et identité du compte, et casserait la préparation à Trakt.

### D2 — Configuration web, association et stockage

Découvrir le bundle de configuration depuis le HTML officiel, plutôt que figer son nom haché. Analyser uniquement le bloc de données contenant `apiHost` et `apiKey`, sans exécuter du JavaScript. La variante essayée n'expose pas de `client_secret` dans ce bloc et n'en a pas demandé pour délivrer le jeton ; aucune valeur collectée n'est copiée dans le dépôt ou le build.

Utiliser HTTPS, hôtes officiels attendus, limites de corps et délais. La clé n'est envoyée qu'à l'API attendue, jamais à un lien de plateforme après redirection. Une récupération de configuration est mutualisée, mémorisée localement et renouvelée au plus une fois après refus explicite de configuration ; un jeton utilisateur refusé requiert une nouvelle association. Ne pas transformer une 502 en rotation de clé.

L'association utilise `device_code`, `user_code`, `verification_url`, `expires_in` et `interval`. Le texte et le QR utilisent la même `verification_url` complète de la même tentative, chemin compris ; la page officielle vérifiée pour la saisie est `https://www.betaseries.com/device`, au singulier. Cette adresse sert de référence, pas de valeur de secours codée en dur : ne pas remplacer l'URL renvoyée par le domaine ou inventer un paramètre prérempli. Le lien et le code doivent suffire à une saisie depuis PC. Une seule boucle par tentative, bornée par l'expiration, suspendue/arrêtée selon la visibilité du panneau et sans réutilisation après fermeture. Un code déjà consommé n'est pas réessayé indéfiniment.

Stocker le jeton chiffré par une clé AES-GCM générée dans Android Keystore, hors sauvegarde transférable. Les préférences non secrètes de lecteur restent séparées du compte. Déconnexion : invalider la génération locale, arrêter les travaux, effacer jeton/données, puis révoquer au mieux le jeton courant par `POST /members/destroy`. Une réponse tardive d'une ancienne génération est rejetée.

Alternative écartée : secret embarqué ou relais backend. Le flux essayé permet de garder la contrainte locale, à condition de tester sa compatibilité et de traiter explicitement sa disparition.

### D3 — Transport, lectures et reprises bornées

Réutiliser les conventions du transport existant, mais introduire une frontière HTTP adaptée aux verbes et aux erreurs BetaSeries : ne pas contourner le GET existant par des URLs de mutation. Pas de nouvelle bibliothèque réseau nécessaire au cadrage ; le transport reste remplaçable et testable.

Valeurs techniques initiales : connexion 5 s, lecture 10 s, échéance totale d'un appel 15 s ; réponses API plafonnées à 2 Mio, ressources de découverte à 10 Mio. Fermer connexions et flux sur terminaison, annulation ou dépassement. Les lots paginés ont une échéance globale et un statut incomplet distinct ; aucune pagination infinie ni parallélisme non borné.

Pour les lectures idempotentes, autoriser au plus deux reprises sur panne de transport, 502/503/504 et réponse non JSON issue d'une erreur serveur, avec temporisation croissante ; respecter Retry-After sur limitation, dans le budget restant. Le polling OAuth garde en plus l'intervalle et l'expiration du serveur. Les erreurs d'accès ou de validation ne déclenchent pas ces reprises.

Ne pas rejouer automatiquement un POST/DELETE de suivi sur réponse perdue : relire d'abord le statut et demander un réessai si nécessaire. L'absence de JSON est une erreur de transport/protocole, pas un compte vide. Les erreurs exposées ne contiennent ni corps brut, ni URL avec jeton, ni titre personnel.

### D4 — Données réellement accessibles et replis

| Source | Champs observés ou documentés | Absences / conséquence |
| --- | --- | --- |
| Configuration web | `apiHost`, `apiKey` dans des données statiques du bundle | Pas de contrat stable de découverte ; pas de secret ou refresh token présumé |
| OAuth appareil | code appareil/utilisateur, adresse, expiration et intervalle ; jeton et type après autorisation | Pas de renouvellement silencieux du jeton établi ; reconfiguration et reconnexion sont distinctes |
| `members/infos` authentifié | identifiant, nom, options dont `country` et `language` | Le pays du compte est une métadonnée, pas une preuve de catalogue ; le choix confirmé reste dans les réglages |
| `episodes/list` authentifié | séries et `unseen[]`, identifiants, saison/épisode, date de diffusion, durée, `user.seen`, `user.seen_date`, pagination/compteurs | `seen_date` du non-vu peut être nul ; la date de diffusion n'est pas l'activité utilisateur |
| `shows/member` / `shows/display` | identifiants BetaSeries/TVDB/IMDb/TMDB, titres, images, archive et prochain épisode, plateformes/liens/plages de saisons | `user.last` est un code d'épisode, pas un timestamp ; les pages peuvent ne pas avoir un prochain épisode exploitable |
| `movies/movie` | identifiants, année, poster/backdrop, durée, statut, `user.date`, liens et plateformes | Ne garantit ni présence de ces champs dans la liste du compte ni date d'ajout ; compléter par détail seulement si nécessaire |
| `movies/member` authentifié | Liste de films, total/totalMissingMovies, id/IMDb/TMDB, poster/backdrop, année, `user.status`, `user.date`, `auth_user`, offres et liens, tous observés après un nouvel essai réussi | Première tentative en 502 non JSON puis lecture réussie lors d'une nouvelle association ; sémantique précise de la date et pagination complète restent à contrôler |
| `platforms/list?country=…` | groupes SVOD/VOD, id, nom, logo ; résultats différents selon pays | La liste des services du pays ne prouve pas l'offre d'un contenu précis |
| TV Provider | Métadonnées, intents et progression déjà lus par le repo, colonnes facultatives | `internal_provider_id` est opaque tant qu'un adaptateur n'a pas prouvé sa signification ; aucune fin de lecture universelle |
| Android sessions média | Accès via permission système ou listener de notifications autorisé, métadonnées et état publiés par l'app | Identité et cause d'arrêt non garanties ; autorisation et comportement constructeur restent à valider sur TV |

Le statut du lot de suivi est atomique pour les listes indispensables : si une page de séries ou films manque après reprises, ne pas publier une fausse liste complète. Retirer la contribution BetaSeries en erreur comme décidé, garder les programmes Android et l'indicateur. Les caches d'images ne rendent pas un ancien compte valide ; les métadonnées transitoires restent distinctes des préférences persistantes.

### D5 — Identité, fusion et mémoire

Porter `TrackingIdentity(provider, type, id)` et des identifiants externes typés (série, épisode, film), sans collision d'identifiants entre namespaces. Résoudre d'abord les identifiants que les sources fournissent réellement ; sinon recherche exacte bornée sur titre, année et numéros, avec cache et absence de correspondance explicite. Aucune sélection du premier résultat ambigu. Les recherches de correspondance sont internes, pas une nouvelle UI P8.

Séparer l'identité catalogue de l'appartenance au compte actif (`Present`, `Absent`, `Unknown`). Une carte locale identifiée et confirmée absente du compte connecté conserve son ouverture locale sans préférence persistante et ajoute seulement l'action Ajouter au suivi à son menu. Après ajout confirmé et appartenance relue, ses ouvertures utilisent le parcours commun des contenus du compte. Une recherche réussie ou une erreur de chargement du compte ne vaut jamais preuve de présence ou d'absence ; sans compte ou avec identité ambiguë, le menu local ne propose pas cet ajout.

La fusion choisit une seule série identifiée avant tri/limite. Une reprise non confirmée comme vue reste admissible ; une reprise déjà vue est écartée. La liste unseen est insuffisante pour prouver que les autres épisodes sont vus : résoudre puis lire le statut authentifié des épisodes locaux concernés, avec cache et budget bornés. En cas d'identité d'épisode incomplète, aucune mutation et aucune substitution d'intent d'un autre épisode. Conserver toutes les sources, mais marquer leur cible exacte : un intent local de S02E03 n'ouvre pas S02E05 sous prétexte de même série.

Préserver le tri actuel : mémoire d'ouverture depuis À suivre d'abord, puis groupes locaux. Les prochains épisodes du suivi rejoignent NEXT, les films à voir WATCHLIST, les vraies reprises CONTINUE. L'activité vient d'un timestamp de l'action réellement disponible, ou de la mémoire locale d'actions ; sans timestamp exploitable, appliquer le repli existant d'activité inconnue (zéro) avec identifiant stable, jamais l'heure de téléchargement, la date de diffusion ou le code `user.last` converti artificiellement en activité.

L'ouverture depuis le héro mutualise le choix de lecteur et la session, mais conserve la règle existante qui ne crée pas de priorité d'ouverture dans À suivre. Migrer les clés de titre vers des liens d'identité enrichis sans perdre ouvertures/masquages. Une panne, permission refusée, page incomplète ou déconnexion n'autorise pas une purge par absence du compte. Sans compte associé, les règles de dédoublonnage locales restent inchangées.

### D6 — Lecteurs et offres

Le sélecteur utilise les activités lançables déjà détectables, la catégorie Android vidéo quand déclarée, les sources locales vidéo connues et les adaptateurs vérifiés. « Autre application » donne accès aux autres apps lançables. Ne pas figer une liste exhaustive de fournisseurs ; les correspondances service/package sont une petite frontière de compatibilité testée, pas une priorité produit.

Les offres reçues sont `Unknown`, `Unconfirmed` ou `Confirmed(country, scope)` ; une offre de série ou de plage de saisons n'est pas promue en offre d'épisode. Les appels testés à shows/display et movies/movie n'ont pas varié selon country ; ne pas filtrer la simple liste des plateformes du pays pour fabriquer une confirmation. Les cartes restent présentes sans offre.

Ordre de résolution : intent de la bonne source/cible ; lien officiellement fourni et adapté au paquet choisi après test ; fiche ; recherche réellement prise en charge ; ouverture simple annoncée. Les redirections sont résolues avec borne et validation des schémas/paquets, sans transmettre les en-têtes d'authentification à un autre hôte. Aucun schéma Netflix/Jellyfin ou identifiant de bibliothèque n'est inventé à partir d'un id BetaSeries. Le lancement système confirme uniquement que la demande a été acceptée, pas que la bonne vidéo est jouée.

### D7 — Mutations et annulation

Sérialiser par compte/contenu et conserver l'état antérieur nécessaire. Envoyer `bulk=false` pour un épisode : le SDK officiel fait l'inverse par défaut. Films : état 0 à voir, 1 vu, 2 ne veut pas voir selon le contrat documenté ; retirer est un autre endpoint, pas l'état non vu. Archiver une série ne modifie pas le masquage local ni n'empêche une reprise locale.

Pour vu/non vu ou archive, relire le résultat confirmé puis proposer l'opération inverse avec son propre résultat. Pour un retrait destructif, masquer temporairement et conserver une action en mémoire pendant la fenêtre d'annulation ; ne pas envoyer avant son échéance. Si l'utilisateur annule ou se déconnecte avant envoi, abandonner. À l'échéance, une erreur restaure la projection cohérente du compte et expose le réessai. Une réaddition n'est pas une restauration d'historique garantie.

La fenêtre Annuler est fixée à 10 secondes actives, suspendues pendant l'interaction, notamment tant que le composant porte le focus, puis reprises avec le temps restant. Aucun message ne prend automatiquement le focus. Le trajet D-pad doit rester accessible après disparition de la carte et être validé avant code UI. Les opérations ne sont pas mises dans une file de synchronisation hors ligne persistante. Une interruption avant l'envoi d'un retrait abandonne ce retrait, conformément à la spec.

### D8 — Observation média optionnelle

Déclarer le `NotificationListenerService` séparément du service Home et utiliser `MediaSessionManager`. Quand un compte est connecté et qu'une voie ADB/Tsuyu déjà autorisée est utilisable, activer automatiquement l'accès média puis relire l'accès effectif avant de l'annoncer disponible, sauf désactivation volontaire de l'observation. Réutiliser la frontière de commandes privilégiées existante ; la commande exacte et son effet sont à prouver dans la tâche appareil 1.1. La présence d'ADB/Tsuyu n'est pas une permission de l'app et `MEDIA_CONTENT_CONTROL` n'est pas une simple permission runtime accordable par hypothèse.

Sans accès effectif par cette voie, proposer le parcours Android documenté d'autorisation du listener ; ouvrir l'écran système seulement sur action et relire l'accès au retour. Activation bornée, erreur explicite et relecture au premier plan évitent toute boucle ou faux succès. Si la TV ne l'expose pas ou refuse, le mode manuel reste pleinement utilisable. Une désactivation volontaire ou une déconnexion arrête l'observation sans réactivation automatique de cette session.

Ne conserver que les observations nécessaires à la session de lecture demandée (paquet, identité corrélable, événements et position/durée utiles), sans collecter le contenu général des notifications. Pas de journal de visionnage en logs. Un adaptateur de preuve doit fournir identité exacte, fin établie et provenance ; `STOPPED`, temps écoulé, changement de carte ou de titre ne sont pas une preuve seuls. Aucun seuil arbitraire de 90 % n'est décidé dans ce cadrage.

La tâche de validation sur TV établit une matrice par app testée. En l'absence de preuve démontrée, l'implémentation livre la proposition Terminé au retour et n'annonce pas l'app comme compatible avec le marquage automatique. Les signaux tardifs d'une session ou d'un compte précédent sont rejetés. Le marquage confirmé ou sa proposition est partagé entre les deux surfaces, sans double envoi.

### D9 — Héro, images et interface

Projeter les mêmes contenus identifiés vers les candidats du héro avant la limite d'À suivre. Écarter les épisodes à venir, les séries à jour et les candidats BetaSeries localement masqués. Réutiliser l'ordre de session et le pipeline partagé ; BetaSeries devient une origine d'image explicite, pas une app fictive du Top Shelf. Un contenu déjà connu ne réinitialise pas le shuffle lors d'une connexion ou d'un retour.

Garder les contrats de dimensions, cache et affichage existants. Préférer le paysage réellement reçu (backdrop/show/image d'épisode s'il existe), sinon portrait accepté seulement dans les cartes ; pas de promesse de bande-annonce native depuis un lien YouTube. Enrichir les images ne crée pas de position de lecture.

Réutiliser `tvFocus`/`tvClickable`, `Motion`, `Dimens` et `GlassSurface` aux endroits autorisés par le design system. Les nouveaux contrôles exposent les tags de ui-testing. `handoff.md` contient les états et la référence Penpot validée ; la PR reste un cadrage, sans implémentation UI ni validation sur TV.

#### Décisions visuelles confirmées après la passe Penpot

La maquette est validée par Sygix après intégration des retours ci-dessous. Cette validation porte sur le design ; les vérifications de navigation et de compatibilité sur TV restent à exécuter.

- Réutiliser le composant de modale de la maquette et sa matière transparente floutée, fond et bordure compris ; dimensionner ses instances au contenu et aux actions, notamment les états vide/indisponible, sans surface opaque de substitution ni grande réserve vide.
- Mutualiser Terminé, Annuler et les résultats transitoires dans le composant Notification de suivi, compact en bas à droite dans la zone sûre, largeur adaptée au contenu, même matière que les modales. Les variantes repos/focus/information partagent cette base.
- Présenter le pays dans la liste déroulante native des réglages, comme la position d'À suivre, sans modale ni suffixe « · confirmé ». La valeur proposée n'est pas confirmée implicitement.
- Garder le statut de synchronisation petit et aligné à droite du titre À suivre, sans suffixe « · Réglages » ; les réglages conservent le détail/réessai même sans rangée.
- Fenêtre Annuler : 10 secondes actives selon D7. Le délai ne déclenche jamais une confirmation Terminé à la place de l'utilisateur.

Les surfaces Penpot de référence sont les pages P9 du fichier partagé « SygixOs Maquette », avec le Panneau de verre et le dropdown existants et le composant Notification de suivi. Les valeurs de matière, focus et animation continuent de provenir du design system ; les exports ne justifient pas de les remplacer par des fonds opaques.

## Risks / Trade-offs

- [Client web non contractuel] → Découverte bornée, parseur de données pur, fixtures synthétiques, renouvellement unique et indisponibilité explicite. Ne pas assimiler réussite d'un essai à compatibilité garantie dans le temps.
- [Pannes serveur intermittentes] → Reprises de lectures bornées, respect du budget et erreur non JSON distincte ; aucune répétition aveugle des mutations.
- [Offres régionales insuffisantes] → Disponibilité non confirmée et choix manuel, sans filtrage prétendument exhaustif.
- [Identité ou fin média absente] → Pas de marquage automatique, confirmation au retour ; validation par app et TV avant déclaration de compatibilité.
- [Retraits pouvant perdre l'historique] → Retarder l'envoi pendant Annuler, ne pas promettre de rollback distant complet.
- [Compte volumineux] → Pagination, une seule source partagée, préchargement borné, aucune récupération d'image collective avant affichage.
- [Évolution des specs entre cadrage et implémentation] → Rebase et réconciliation des mêmes exigences avant code ; ne pas archiver cette PR de planification.

## Migration Plan

1. Partir du cadrage et des retours Penpot reportés sur cette branche ; compléter et faire valider les trajets D-pad encore ouverts avant code UI.
2. Avant code, exécuter les tâches 1 de validation de contrat/appareil et reporter une matrice sans données personnelles.
3. Ajouter les frontières de suivi et le stockage protégé ; conserver le chemin local comme repli lorsque le compte est absent.
4. Migrer les identités par enrichissement compatible et vérifier que masquages, ouvertures et préférences ne disparaissent pas.
5. Brancher les projections et parcours UI après validation Penpot ; tester JUnit, Compose TV et TV réelle en release.
6. En cas de régression, déconnecter le fournisseur restaure le parcours local sans suppression des préférences existantes.
7. Après merge de l'implémentation et validation TV, archiver p9-betaseries. Aucun change concurrent à ordonner au moment du cadrage ; ne pas archiver lors du merge de la seule spec.

## Questions ouvertes

### Questions ouvertes — passe Penpot

La passe Penpot et les retours ont fixé les décisions visuelles de D9 et la fenêtre de 10 secondes de D7. Restent à valider avant code UI : le trajet D-pad exact vers Terminé/Annuler depuis chaque origine, y compris après disparition d'une carte, et l'accès à l'indicateur quand À suivre est masqué. Les réglages fournissent déjà le détail/réessai sans carte ; les propositions ne volent pas le focus et n'ajoutent aucun quatrième palier. Ne pas déclarer une navigation validée sur la seule base des planches statiques.

### Hypothèses vérifiables avant code

Les essais d'intents, d'autorisation média et de preuves de fin sur TV, ainsi que les effets exacts des mutations distantes, sont des validations de compatibilité et non des choix produit restants. Leurs résultats doivent respecter les replis déjà spécifiés ; tout besoin d'une autre architecture ou d'un changement de comportement doit revenir à Sygix.
