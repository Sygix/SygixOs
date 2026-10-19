# Tasks

## 1. Vérification des données sur la TV avant implémentation

- [ ] 1.1 Sur la TV Google TV sous Android 14, relever pour Jellyfin, Disney+ et Crunchyroll les schémas d'URI, colonnes réellement présentes, compteurs de candidates, ratios et dimensions, causes et durées d'échec dans les trois zones. Vérifier si une échéance ou une identité de contenu fiable est réellement publiée, sans en présumer l'existence ; livrer dans `design.md` un relevé structurel sans URI brute, titre, intent personnel, serveur ni donnée de visionnage. Si un constat invalide le modèle retenu, réconcilier les documents avant le code.

## 2. Programmes, candidates et confirmations

- [ ] 2.1 Conserver affiche, miniature et identité/version vérifiées dans les modèles du héro et d'À suivre, sans éliminer un programme du seul fait d'une image absente ; tests de mapping avec colonnes absentes, ratios trompeurs, film et épisode, et alternative encore disponible après échec de la première candidate.
- [ ] 2.2 Remplacer la conversion d'erreur en liste vide du parcours héro par des résultats explicites et des lectures bornées par table ; tests de provider absent, permission refusée, timeout, succès vide et succès partiel entre tables, prouvant que seule une lecture réussie retire les absents et renouvelle la confirmation correspondante.
- [ ] 2.3 Porter les échéances de programmes dans l'état commun, appliquer le plafond de 24 h à la prochaine échéance utile et au retour au premier plan, étendre la relecture périodique à À suivre ; tests à 24 h moins un instant et exactement 24 h, sur contenu déjà affiché, sans réinitialisation par un échec ou un succès de l'autre table, et sans purge de mémoire utilisateur à l'expiration.
- [ ] 2.4 Protéger les publications asynchrones par identité et génération ; tests d'image reçue après retrait, expiration, remplacement ou réutilisation d'identifiant de ligne, et de deux films de même titre sans échange d'image. Vérifier aussi que des épisodes utilisant une même image de série publiée partagent ses octets sans rapprochement non vérifié.

## 3. Cache de sources et dimensions partagés

- [ ] 3.1 Introduire le service commun et ses états typés, une seule analyse d'en-tête par version et des politiques pures propres au héro, au Top Shelf et aux cartes ; tests comptant une récupération et une analyse pour les trois consommateurs, avec verdicts différents selon l'usage, portrait et seuils juste sous, égaux et au-dessus de 640 px ou de 1× la largeur de destination selon la politique.
- [ ] 3.2 Conserver les sources HTTP(S) et `content://` dans un cache disque privé au budget global existant avec dimensions, empreinte et date de succès ; tests recréant le chargeur avec le même disque et un fournisseur indisponible, prouvant que les octets locaux sont disponibles avant tout accès à celui-ci, sans second cache ni prolongation de date.
- [ ] 3.3 Mettre en œuvre engagements atomiques, fraîcheur 24 h, secours jusqu'à moins de 7 jours et invalidation des copies mémoire/verdicts d'une ancienne version ; tests aux frontières exactes, rafraîchissement en échec ou nouveaux octets indécodables préservant l'ancienne copie décodable, nouvelle version de même URI, entrée héritée sans date, date incohérente et éviction normale.
- [ ] 3.4 Mutualiser les demandes en cours et les alternatives dans un budget commun, conserver les décodages et transformations par taille ; tests où validation, préchargement et affichage rejoignent la même demande, où un abonné disparaît mais un autre reste, et où l'affiche échoue avant la réussite de la miniature sans nouvelle analyse ni ouverture de la même version.

## 4. Ordonnancement et fournisseurs isolés

- [ ] 4.1 Installer les files séquentielles par app, le parallélisme global borné et l'attente bornée de D2 ; tests à quatre apps avec récupérations contrôlées vérifiant la constante globale, l'absence de deux récupérations physiques d'une même app, l'équité, le service immédiat des hits de cache et l'absence d'attente collective d'un lot.
- [ ] 4.2 Raccorder le récupérateur `content://` tiers par client instable et échéancier indépendant couvrant ouverture, appels MIME et copie du flux ; tests avec chaque appel synchrone bloqué prouvant l'échec avant libération du double, annulation hors worker et hors fil d'échéances, délais suivants encore rendus même si l'annulation bloque, et fermeture unique des ressources tardives sans publication ni engagement de cache.
- [ ] 4.3 Quarantiner un worker bloqué et interdire une nouvelle récupération de la même app jusqu'à sa sortie, sans créer de threads supplémentaires ; tests où une app bloquée laisse progresser les autres sur les places restantes, puis où tous les workers sont bloqués : résultat en délai borné, taille du pool et de la file bornées, cache encore lisible.
- [ ] 4.4 Appliquer le délai total HTTP(S) et les reprises temporisées communes ; tests avec transport local contrôlé pour lecture lente, HTTP 4xx/5xx, réseau absent, annulation, fournisseur disparu et refus de sécurité, prouvant qu'un événement autorisé peut reprendre sans blacklist locale ni rafale par abonné.
- [ ] 4.5 Auditer les ouvertures d'images tierces dans le héro, le préchargement de `HomeGrid`, `ShelfPanel`, les cartes, menus et vignettes de réglages ; vérifier par recherche et tests d'intégration qu'aucun chemin de programme `content://` ne contourne le récupérateur protégé. Les bannières locales `PackageManager` ne passent pas par ces files.

## 5. Intégration des trois zones et documentation

- [ ] 5.1 Raccorder le héro au service commun, remplacer la blacklist d'images et séparer programme affiché de média de secours ; tests Compose à la taille TV avec image lente/réussie/expirée, vidéo nature prête puis illisible, titre et cible d'ouverture exacts, absence de nouveau focus sur les replis, navigation manuelle et avance automatique vers une autre app saine. Préserver le comportement de la vidéo d'aperçu lente et les plafonds de l'écran de démarrage.
- [ ] 5.2 Raccorder validation, préchargement et affichage du Top Shelf au même service et appliquer le seuil paysage de 1× sa largeur en pixels hors zoom ; tests Compose à la taille TV avec passage app saine → app en panne → app saine, absence d'affiche d'une mauvaise app ou de cadre vide, expiration pendant une validation inachevée, fermeture sans perdre la tuile focusée et compteur sans double ouverture.
- [ ] 5.3 Raccorder À suivre et ses candidates au service commun, décoder à sa taille et appliquer 1× la largeur réelle de carte hors zoom ; tests avec image de 396 px pour carte de 396 px, juste sous le seuil, portrait, absence totale et panne avec cache, puis expiration d'image sans changement de géométrie et expiration de carte avec restauration du focus aux bords et rangée vide.
- [ ] 5.4 Réutiliser les sélecteurs existants et exposer les états réels `hero-fallback-nature` et `hero-fallback-neutral`, placer les nouvelles chaînes éventuelles en ressources FR ; vérifier par tests sémantiques et recherche l'absence de texte UI ajouté en dur ou de sélecteur dépendant d'un titre personnel.
- [ ] 5.5 Mettre à jour le README pour le parcours partagé, les seuils par destination, la rétention bornée, les expirations et les replis ; vérifier la correspondance des valeurs avec les specs et l'absence d'affirmation de cache illimité ou de schéma d'URI non vérifié pour une app.

## 6. Ordre aléatoire du héro

- [ ] 6.1 Implémenter le tri de session de D7 : graine par processus, ordre stable, insertion aléatoire après le programme affiché, retrait/expiration sans réordonner ; tests JUnit avec `Random` injecté, deux graines déterministes, rechargement inchangé, ajout, retrait et liste vide, sans test probabiliste exigeant deux ordres différents à chaque lancement réel.

## 7. Vérification intégrée

- [ ] 7.1 Exécuter `openspec validate --all --strict`, la vérification OpenSpec de l'implémentation et `./gradlew test` ; consigner les résultats, sans cocher de scénario couvert seulement par un double comme validation réelle du comportement Android.
- [ ] 7.2 Construire avec `./gradlew assembleRelease` et vérifier sur la TV les trois zones : premier chargement, cache après relance, serveur indisponible, fournisseur lent puis arrêté, permission refusée, retour au premier plan, alternative d'image, diaporama et shuffle. Vérifier que les autres apps progressent pendant le blocage et confirmer les constantes techniques de concurrence/délai ; consigner des résultats structurels dans `design.md` sans donnée personnelle.
- [ ] 7.3 Vérifier les expirations par horloge contrôlée en intégration, puis les états stables correspondants sur la TV, ainsi que la navigation D-pad et la fluidité de release selon « Préchargement et mémoire » ; prouver qu'une image déjà affichée expire, qu'un programme retiré ne revient pas après une réponse tardive et que la page reste navigable sans fuite de workers ou décodeurs.

## Workflow follow-up

- Après merge et validation sur la TV : `openspec archive hero-shuffle-image-provider`.
