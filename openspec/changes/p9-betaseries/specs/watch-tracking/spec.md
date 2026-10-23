# Spec Delta

## Purpose

Suivi de visionnage indépendant du fournisseur et du lecteur : identité des contenus, compte actif, ouverture commune au héro et À suivre, actions sur le compte et observation optionnelle de la lecture.

## ADDED Requirements

### Requirement: Fournisseur de suivi actif
Le launcher SHALL utiliser un seul compte de suivi actif, distinct des applications de lecture. Les comportements communs SHALL dépendre d'un contrat de fournisseur extensible ; cette version SHALL exposer uniquement BetaSeries, sans option Trakt fictive.

#### Scenario: sans compte
- **WHEN** aucun compte n'est connecté
- **THEN** le launcher conserve les sources et parcours locaux ; aucune association n'est imposée au démarrage

#### Scenario: compte connecté
- **WHEN** la connexion est confirmée
- **THEN** le fournisseur alimente le suivi partagé du héro et d'À suivre, sans charger séparément le compte pour chaque zone

#### Scenario: changement de compte
- **WHEN** l'utilisateur se déconnecte puis associe un autre compte
- **THEN** les données, mutations et preuves du premier compte ne sont jamais réutilisées pour le second ; préférences de lecteur et masquages locaux restent conservés sur la TV

### Requirement: Identité de suivi partagée
La correspondance d'un contenu local avec le suivi SHALL reposer sur une identité non ambiguë, typée et indépendante de l'app. Une série, un épisode et un film SHALL avoir des identités distinctes ; une recherche incertaine ne SHALL ni fusionner les contenus ni autoriser de mutation du compte.

#### Scenario: identifiants connus
- **WHEN** deux sources ont un identifiant externe compatible de même type
- **THEN** elles désignent la même série, le même épisode ou le même film ; l'identité de série ne suffit pas à identifier un épisode sans ses numéros ou son identifiant

#### Scenario: rapprochement sans identifiant
- **WHEN** une recherche exacte par titre et informations disponibles donne plusieurs candidats plausibles
- **THEN** aucun candidat n'est retenu automatiquement ; la carte locale reste ouvrable et aucune progression ni statut n'est envoyé au mauvais contenu

#### Scenario: enrichissement indisponible
- **WHEN** le rapprochement est en attente, vide, en erreur ou non autorisé
- **THEN** les métadonnées et règles locales restent utilisables ; le retour au premier plan peut réessayer avec un délai borné sans bloquer l'accueil

### Requirement: Choix et mémorisation du lecteur
Le premier lancement d'un contenu suivi SHALL demander son application de lecture, même avec une seule offre connue. Le choix SHALL être commun au héro et à À suivre, persistant par série ou film, et réutilisé tant que l'app est lançable et qu'aucune indisponibilité du contenu n'est établie.

#### Scenario: premier choix
- **WHEN** l'utilisateur ouvre un contenu sans préférence
- **THEN** les applications vidéo reconnues sont proposées avec « Autre application » pour accéder aux autres apps lançables ; seules les offres régionales prouvées sont présentées comme confirmées

#### Scenario: préférence partagée
- **WHEN** une application a été choisie depuis une carte puis que la série ou le film est ouvert depuis le héro, ou inversement
- **THEN** le même choix est utilisé sans nouvelle confirmation ; un autre épisode de la même série utilise cette préférence

#### Scenario: application absente ou contenu indisponible
- **WHEN** l'application mémorisée est désinstallée, non lançable ou qu'une indisponibilité du contenu y est établie
- **THEN** le sélecteur est reproposé avec une raison ; aucune indisponibilité n'est déduite de la seule absence d'information de catalogue

#### Scenario: choix par appui long
- **WHEN** l'utilisateur choisit « Ouvrir avec… » dans le menu d'un contenu suivi
- **THEN** il peut changer d'application et ce choix remplace la préférence après un lancement réussi ; ouvrir puis annuler le sélecteur ne change rien

#### Scenario: sélecteur vide ou modifié
- **WHEN** aucune app n'est lançable ou que l'app focusée disparaît
- **THEN** un état vide explicite est affiché dans le premier cas ; sinon le focus va à l'entrée suivante, puis précédente, et jamais à une app disparue

### Requirement: Résolution de l'ouverture
L'ouverture SHALL viser l'application choisie et le contenu identifié. Elle SHALL privilégier un intent publié ou un lien testé vers l'épisode ou le film, puis une fiche, une recherche et enfin l'ouverture de l'app avec information explicite. La présence d'un lien web ne SHALL pas prouver une lecture ciblée.

#### Scenario: cible précise disponible
- **WHEN** un adaptateur sait ouvrir le contenu dans le paquet sélectionné
- **THEN** ce paquet est lancé avec la cible correspondante ; l'identité du contenu, l'origine héro ou À suivre et la précision effective sont conservées pour le retour

#### Scenario: repli nécessaire
- **WHEN** l'épisode précis n'est pas ouvrable mais qu'une fiche, une recherche ou le lancement de l'app est possible
- **THEN** le meilleur repli vérifié est utilisé et annoncé ; aucune simple ouverture de l'app n'est présentée comme une lecture effectivement commencée

#### Scenario: échec complet
- **WHEN** ni la cible ni l'application ne peuvent être ouvertes
- **THEN** une erreur non bloquante est exposée avec réessai ; aucune préférence de lecteur ni ouverture réussie ni preuve de lecture n'est enregistrée

### Requirement: Navigation des parcours de suivi
Sélecteur, menu et liste d'épisodes SHALL être navigables au D-pad, avec focus initial déterministe, sans boucle aux bords, et Retour fermant d'abord le niveau courant. Ils SHALL restaurer le contexte d'origine sans détourner le focus d'une zone inactive.

#### Scenario: sélecteur et menu
- **WHEN** un sélecteur ou menu s'ouvre
- **THEN** la préférence encore disponible, sinon la première action utilisable a le focus ; haut/bas parcourent les entrées, gauche/droite ne quittent pas le panneau, et Retour restitue le focus à l'élément d'origine ou à son repli de launcher-shell

#### Scenario: épisodes diffusés
- **WHEN** « Choisir un épisode » s'ouvre
- **THEN** l'épisode courant, sinon le premier épisode diffusé est focalisé ; les saisons et épisodes diffusés vus et non vus sont parcourables, sans boucle, avec leur statut ; consulter la liste ne lance rien et ne change aucun statut

#### Scenario: état de la liste d'épisodes
- **WHEN** la liste charge, réussit sans épisode, échoue ou perd l'accès au compte
- **THEN** elle expose respectivement chargement, vide, erreur réessayable ou reconnexion, avec Retour toujours disponible ; au retour au premier plan elle se rafraîchit et conserve l'épisode focusé s'il existe, sinon son voisin, sinon l'action d'état

#### Scenario: retour d'une application
- **WHEN** une application ouverte par le parcours commun rend la main au launcher
- **THEN** le retour conserve son origine : restauration de la grille et du contenu pour À suivre, du héro pour le héro ; une proposition de suivi ne vole pas ce focus

### Requirement: Actions sur le compte
Un contenu identifié SHALL proposer les actions applicables : ajouter au suivi depuis un contenu existant, vu/non vu, retirer et, pour une série, archiver. Marquer un épisode SHALL ne modifier que cet épisode ; le masquage local SHALL rester distinct de toute action sur le compte.

#### Scenario: épisode isolé
- **WHEN** S02E05 est marqué vu et S02E03/S02E04 restent non vus
- **THEN** seul S02E05 change de statut ; aucun marquage implicite des précédents n'a lieu

#### Scenario: ajout depuis une source locale
- **WHEN** une série ou un film existant est identifié sans ambiguïté et n'est pas dans le compte
- **THEN** l'ajout est proposé depuis son menu ; aucune recherche générale de catalogue n'est requise et une correspondance incertaine ne permet pas l'envoi

#### Scenario: archivage
- **WHEN** une série est archivée avec succès
- **THEN** elle quitte l'apport du fournisseur ; une reprise locale peut continuer à l'afficher et le masquage local n'est pas modifié

#### Scenario: opération en cours ou refusée
- **WHEN** une mutation charge, échoue, dépasse son délai ou perd son autorisation
- **THEN** l'action concernée expose son état et empêche un double envoi ; aucun succès n'est annoncé sans confirmation et le réessai ou la reconnexion est proposé

#### Scenario: résultat réseau incertain
- **WHEN** une réponse est perdue après l'envoi d'une mutation
- **THEN** le statut distant est relu avant toute répétition ; aucune file hors ligne ni réémission automatique aveugle ne modifie le compte plus tard

### Requirement: Annulation non bloquante
Un marquage vu/non vu ou archivage SHALL proposer une annulation non bloquante de l'action confirmée. Un retrait du compte SHALL retirer visuellement le contenu mais différer l'envoi pendant la fenêtre Annuler. Une erreur d'annulation SHALL être distinguée d'une annulation réussie.

#### Scenario: annuler un marquage
- **WHEN** l'utilisateur annule un marquage confirmé
- **THEN** l'état précédent de l'épisode ou du film est rétabli par l'opération adaptée puis relu ; l'annulation reste accessible même si la carte passe à l'épisode suivant ou disparaît

#### Scenario: retrait encore annulable
- **WHEN** l'utilisateur annule avant l'échéance d'un retrait du compte
- **THEN** la carte retrouve sa place de tri et aucun retrait distant n'a été envoyé

#### Scenario: annuler un archivage
- **WHEN** l'utilisateur annule un archivage confirmé
- **THEN** l'état d'archive précédent est rétabli puis relu, sans retrait de la série ni modification de ses épisodes vus ou de son masquage local

#### Scenario: échéance puis échec
- **WHEN** la fenêtre d'annulation se termine puis que le retrait échoue
- **THEN** le contenu est réconcilié avec le compte et l'échec est visible avec réessai ; aucune restauration d'historique non garantie n'est promise après suppression distante

#### Scenario: changement de compte ou interruption
- **WHEN** une action différée n'est pas encore envoyée et que le compte est déconnecté ou le processus interrompu
- **THEN** elle est abandonnée et ne sera pas envoyée sous une future session ; une requête déjà partie ne peut pas être déclarée annulée sans vérifier son résultat

#### Scenario: interaction avec le retour
- **WHEN** une proposition ou une annulation est présentée
- **THEN** elle n'interrompt pas la navigation, ne prend pas spontanément le focus et reste accessible au D-pad pendant sa fenêtre ; son échéance n'exécute jamais un marquage vu à la place de l'utilisateur

### Requirement: Accès optionnel aux sessions média
Le suivi automatique SHALL proposer un accès Android optionnel, séparé de la connexion au compte et de READ_TV_LISTINGS. Refus, retrait ou écran système absent SHALL laisser le launcher et la confirmation manuelle utilisables, sans nouvelle sollicitation automatique.

#### Scenario: autorisation demandée
- **WHEN** l'utilisateur ouvre volontairement le contrôle d'accès aux sessions
- **THEN** l'écran système disponible s'ouvre ; l'accord est relu au retour et le focus revient au contrôle, sans présumer du succès

#### Scenario: accès absent ou retiré
- **WHEN** l'accès est refusé, révoqué, non pris en charge ou que les données d'une app manquent
- **THEN** aucun marquage automatique n'est réalisé sur cette base ; la raison est consultable dans les réglages et le parcours manuel reste disponible

#### Scenario: arrêt du suivi
- **WHEN** l'utilisateur désactive l'observation ou se déconnecte
- **THEN** l'observation associée à ce compte s'arrête et aucune nouvelle preuve ne peut déclencher une mutation pour cette session

### Requirement: Preuve de fin de lecture
Le marquage automatique SHALL exiger une preuve vérifiée de fin pour le bon contenu et le compte courant. Ni temps passé dans l'app, ni simple arrêt média, ni disparition d'une carte, ni passage à l'épisode suivant ne SHALL suffire seuls. Un succès automatique SHALL proposer Annuler au retour.

#### Scenario: preuve complète
- **WHEN** un adaptateur validé observe la fin du film ou de l'épisode identifié avec certitude
- **THEN** ce contenu seul est marqué vu, au plus une fois pour cette session de lecture, et son résultat est partagé entre héro et À suivre

#### Scenario: preuve incomplète
- **WHEN** seuls le paquet, un titre ambigu, une pause ou un arrêt sont connus
- **THEN** aucune mutation automatique n'est envoyée et la confirmation discrète au retour est utilisée

#### Scenario: confirmation au retour
- **WHEN** l'utilisateur revient après un lancement de contenu suivi sans preuve complète
- **THEN** une proposition non bloquante « Terminé ? » concerne le contenu réellement demandé, pas l'épisode éventuellement affiché après rafraîchissement ; sans réponse le suivi ne change pas

#### Scenario: compte ou contenu changé
- **WHEN** un signal tardif concerne une ancienne session, un autre épisode ou un compte déconnecté
- **THEN** il est ignoré pour les mutations ; une autorisation Android ne constitue pas une preuve d'identité ni de fin

### Requirement: Offres et région
Le pays SHALL être proposé depuis la configuration de la TV s'il existe puis confirmé et modifiable dans les réglages. Les offres non vérifiées pour ce pays SHALL être signalées « Disponibilité non confirmée ». L'installation d'une app ne SHALL prouver ni abonnement ni présence du contenu.

#### Scenario: pays non confirmé
- **WHEN** aucun pays n'est confirmé
- **THEN** le choix manuel des apps reste disponible et aucune offre n'est annoncée comme régionale

#### Scenario: catalogue ou offre absents
- **WHEN** l'appel charge, est vide, échoue, n'est pas autorisé ou ne fournit pas la région de l'offre
- **THEN** les cartes et le choix du lecteur restent utilisables ; l'offre reste inconnue ou non confirmée, sans invention d'une disponibilité ou exclusion du contenu

#### Scenario: pays modifié
- **WHEN** le pays confirmé change
- **THEN** les informations d'offre sont réévaluées pour ce pays et au prochain retour au premier plan ; les préférences de lecteur ne sont pas effacées et une donnée d'un ancien pays ne devient pas une preuve pour le nouveau
