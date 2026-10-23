# Tasks

Cette liste suit l'implémentation future, pas la rédaction de cette PR. Aucune tâche n'est déclarée faite à partir d'un simple essai de cadrage. La passe Penpot peut reprendre le handoff indépendamment des validations TV ; le code des parcours concernés attend leurs prérequis.

## 1. Contrats et appareil avant code produit

- [ ] 1.1 Sur une TV Google TV sous Android 14, vérifier la commande et l'accès média effectif via ADB/Tsuyu déjà autorisé, son échec/retrait, puis le repli Android manuel (écran présent ou absent, refus, accord, révocation) ; relever uniquement les colonnes/types d'identité, progression et événements des apps testées ; critère : matrice anonymisée prouvant l'activation automatique sans écran d'autorisation supplémentaire ou son repli, et indiquant preuve de fin disponible ou confirmation manuelle obligatoire, sans code produit engagé avant cette vérification.
- [ ] 1.2 Tester les intents et liens de plateforme pour les lecteurs disponibles, avec cible épisode/film, fiche, recherche et ouverture simple ; critère : chaque adaptateur prévu a une cible et un repli réellement observés, les liens non testés restent non confirmés.
- [ ] 1.3 Confirmer la pagination complète des séries, épisodes et films et la sémantique des dates d'activité avec un accès de test autorisé ; critère : exemple structurel de deux pages et de fin de pagination, dates absentes couvertes et aucune confusion entre diffusion, ajout, visionnage et synchronisation.
- [ ] 1.4 Vérifier vu/non vu, bulk=false, retrait, archive et annulation sur des contenus de test explicitement autorisés ; critère : état avant/après et épisodes voisins contrôlés, différences de restauration consignées sans historique personnel publié.

## 2. Passe Penpot sur instruction de Sygix

La maquette a été produite, montrée puis validée par Sygix ; les décisions sont reportées dans design et handoff. Cette validation visuelle ne vaut pas validation du comportement D-pad ni exécution des tests de l'implémentation future.

- [ ] 2.1 À la demande explicite de Sygix, produire les états du handoff dans Penpot et lui montrer les aperçus ; critère : écrans de connexion, compte, pays, accès média, lecteur, épisodes, menu, erreur et propositions annotés avec leurs états.
- [ ] 2.2 Valider le trajet D-pad des propositions non bloquantes, leur accès après disparition d'une carte et l'indicateur sans rangée ; critère : parcours complétés et fenêtre Annuler de 10 secondes actives suspendue pendant l'interaction vérifiée, aucun point de navigation laissé à deviner avant code UI.

## 3. Frontières, transport et session

- [ ] 3.1 Ajouter les contrats de fournisseur de suivi, identité, lecteur et preuve, avec états succès/vide/erreur/incomplet et génération de session ; critère : tests JUnit démontrant isolation de deux comptes et rejet des réponses tardives sans dépendance BetaSeries dans le contrat commun.
- [ ] 3.2 Ajouter le transport de requêtes et les erreurs structurées, avec délais, limites de taille et reprises de lectures bornées de D3 ; critère : tests de 502/503/non JSON/timeout/Retry-After, fermeture des ressources et aucun replay de mutation sur réponse perdue.
- [ ] 3.3 Implémenter découverte et lecture des seules données de configuration web, sans exécution de script ni valeur de clé fixe ; critère : fixtures synthétiques couvrant nom de bundle changé, champ absent, hôte refusé et récupération concurrente mutualisée.
- [ ] 3.4 Implémenter l'association appareil, son polling borné, expiration, fermeture et reconnexion ; critère : tests d'attente 2001, succès, code consommé/expiré, panne, suspension/reprise, absence de polling dupliqué et même URL complète avec chemin dans texte/QR de la même tentative, sans repli vers le domaine.
- [ ] 3.5 Ajouter le stockage Android Keystore et la fin de session avec révocation au mieux ; critère : tests de stockage indisponible, déconnexion hors ligne et nettoyage immédiat, puis contrôle Android de chiffrement et exclusion des sauvegardes, sans jeton dans logs ou fixtures.

## 4. Lecture du compte et fusion

- [ ] 4.1 Mapper les listes paginées et détails réellement nécessaires, y compris films state=0, séries archivées et épisodes diffusés ; critère : tests de champs nuls, pages manquantes, compte vide, erreurs distinctes et absence de limite de compte à 20.
- [ ] 4.2 Ajouter la résolution exacte des identifiants et des correspondances sans ambiguïté, avec cache borné et appartenance au compte distincte (présent/absent/inconnu) ; critère : tests de titres localisés, remakes, série/film homonymes, épisodes mal numérotés, correspondance multiple et erreur de compte sans fusion, appartenance fictive ni mutation.
- [ ] 4.3 Étendre le dédoublonnage et le choix d'épisode selon up-next ; critère : tests reprise S02E05 contre prochain S02E03, reprise déjà vue, plusieurs sources conservées et absence de réutilisation d'un intent d'un autre épisode.
- [ ] 4.4 Migrer par enrichissement la mémoire d'ouvertures et de masquages ; critère : tests conservant les clés existantes, tri actuel, activité inconnue, retrait de série entière et absence de purge sur erreur, permission manquante ou pagination incomplète.
- [ ] 4.5 Brancher la source de suivi partagée dans les projections À suivre et héro ; critère : tests 20 cartes après tri, candidats du héro au-delà de 20, ordre de session stable, masquage BetaSeries du héro et aucun ajout au Top Shelf.
- [ ] 4.6 Brancher les erreurs indépendantes des sources et l'indicateur ; critère : tests compte accessible sans permission TV, apps conservées en panne BetaSeries, retour du compte après succès et cache d'images incapable de ressusciter une contribution invalide.

## 5. Lecture commune et actions de suivi

- [ ] 5.1 Détecter les lecteurs reconnus et proposer Autre application depuis le catalogue lançable ; critère : tests catégorie absente, aucune app, app désinstallée et préférence commune par série/film conservée après déconnexion.
- [ ] 5.2 Mutualiser l'ouverture du héro, des cartes et des menus avec leurs origines et replis ; critère : tests du premier choix obligatoire, choix mémorisé, lancement refusé, fiche/recherche/app simple annoncés et focus de retour sans changer la règle de tri depuis le héro.
- [ ] 5.3 Mapper les offres et le pays sans confirmation régionale inventée ; critère : tests pays non confirmé/changé, offres identiques entre pays, offre de saison non promue en épisode et app installée sans abonnement présumé.
- [ ] 5.4 Implémenter ajout depuis contenu identifié, vu/non vu, archive et retrait différé ; critère : tests d'action applicable, bulk=false, état film et archive restaurés par Annuler, double envoi empêché, annulation avant échéance sans suppression et réconciliation après échec.
- [ ] 5.5 Ajouter la liste des saisons/épisodes diffusés, vus et non vus, sans mutation à la consultation ; critère : tests chargement/vide/erreur/reconnexion, épisode disparu et sélection du bon épisode sans changer les précédents.
- [ ] 5.6 Mettre à jour la documentation utilisateur des lecteurs, replis, masquages et retraits ; critère : README distinguant clairement masquage local, archive et suppression du compte, sans promesse de restauration distante d'historique.

## 6. Observation média optionnelle

- [ ] 6.1 Ajouter le service et la passerelle de sessions média, séparés du service Home, avec activation automatique via ADB/Tsuyu déjà utilisable, vérification effective et repli Android volontaire ; critère : tests activation en cours/succès/échec, accès retiré, écran absent, désactivation respectée et arrêt à la déconnexion, sans faux succès ni collecte du corps général des notifications.
- [ ] 6.2 Implémenter les adaptateurs de preuve seulement pour les cas démontrés par 1.1, avec session exacte et dédoublonnage ; critère : tests pause/STOPPED/titre ambigu/changement d'épisode/ancien compte n'envoyant aucune mutation et preuve complète n'en envoyant qu'une.
- [ ] 6.3 Ajouter la proposition Terminé au retour et Annuler après succès manuel ou automatique ; critère : tests sans réponse inchangée, contenu demandé conservé malgré nouveau prochain épisode, annulation après disparition de carte, échéance de 10 secondes actives et suspension/reprise du temps restant pendant l'interaction, aucun retrait avant échéance.
- [ ] 6.4 Documenter les limites de compatibilité et le fonctionnement sans accès média ; critère : README ne présentant aucune app non validée comme compatible avec le suivi automatique.

## 7. Interface et navigation après validation Penpot

- [ ] 7.1 Ajouter la catégorie Suivi de visionnage, le compte, le pays en dropdown natif sans suffixe « · confirmé », le QR/code/adresse complète et les états de connexion ; critère : chaînes FR en ressources, testTags de ui-testing et tests Compose `@Config(qualifiers = "w960dp-h540dp-xhdpi")` pour focus, bords, validation/annulation de la liste, Retour et restauration.
- [ ] 7.2 Ajouter les menus de suivi, sélecteurs de lecteur/épisode et badges, selon les composants et maquettes validés ; critère : tests Compose TV locaux inchangés et nouveaux cas vus/non vus, app disparue, image absente, préférence partagée et Ajouter au suivi depuis une carte locale identifiée absente du compte couverts, y compris maintien local avant ajout et bascule après succès confirmé.
- [ ] 7.3 Ajouter petit indicateur à droite sans « · Réglages », détail/réessai et composant transitoire compact bas droite sans vol de focus ; réutiliser les modales glass transparentes floutées dimensionnées au contenu ; critère : tests Compose TV rangée vide, carte disparue, panne, arrivée/expiration et restauration, puis contrôle TV des matières et de la compacité, notamment état indisponible.
- [ ] 7.4 Adapter Réafficher dans Applications cachées pour les contenus du compte et du héro ; critère : tests Compose TV conservant tri, dépliage, première ligne et réaffichage sans mutation distante.

## 8. Validation intégrée

- [ ] 8.1 Exécuter `./gradlew test` et `openspec validate --all --strict`, puis vérifier la conformité du change avec openspec-verify-change ; critère : commandes réussies et aucun scénario accepté déclaré couvert sans test ou preuve de runtime correspondante.
- [ ] 8.2 Produire `./gradlew assembleRelease` puis valider sur TV réelle connexion/reconnexion, compte, lecteur, pays, panne/reprise, autorisation média et retour des deux surfaces ; critère : matrice anonymisée des scénarios réussis/partiels et fluidité vérifiée selon launcher-shell, sans confondre émulateur et TV réelle.
- [ ] 8.3 Vérifier la migration et les régressions sans compte puis après déconnexion, et mettre à jour README/attribution/licences si nécessaire ; critère : sources locales, tri, masquages et préférences conservés, aucune clé, donnée personnelle ni sortie de build dans le diff public.

## Workflow follow-up

- Obtenir la validation explicite de Sygix avant merge de l'implémentation ; le merge de cette PR de spécification ne vaut ni validation graphique ni validation TV.
- Après merge de l'implémentation et validation sur TV, exécuter `openspec archive p9-betaseries`, puis `openspec validate --all --strict` sur les specs fusionnées.
- Aucun ordre d'archive concurrent n'est requis au cadrage ; réexaminer les changes ouverts avant l'implémentation future et réconcilier les mêmes exigences si nécessaire.
