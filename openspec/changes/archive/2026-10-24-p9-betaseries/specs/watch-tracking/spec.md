# Spec Delta

## Purpose

Suivi de visionnage indépendant du fournisseur et du lecteur : identité des contenus, compte actif, ouverture commune au héro et À suivre, actions et confirmation manuelle au retour.

## ADDED Requirements

### Requirement: Fournisseur de suivi actif
Le launcher SHALL utiliser un seul compte de suivi actif, distinct des applications de lecture. Les comportements communs SHALL dépendre d'un contrat de fournisseur extensible ; cette version SHALL exposer uniquement BetaSeries, sans option Trakt fictive.

#### Scenario: sans compte
- **WHEN** aucun compte n'est connecté
- **THEN** le launcher conserve les sources et parcours locaux ; aucune association n'est imposée au démarrage

#### Scenario: compte connecté
- **WHEN** la connexion est confirmée
- **THEN** le fournisseur alimente le suivi partagé du héro et d'À suivre, sans charger séparément le compte pour chaque zone

#### Scenario: identité et appartenance distinctes
- **WHEN** un contenu local a une identité résolue dans le catalogue du fournisseur
- **THEN** cela ne prouve pas son appartenance au compte actif ; « contenu suivi » désigne un contenu identifié appartenant au compte, seul cas activant le parcours commun de lecteur et sa préférence persistante ; un contenu identifié absent du compte peut proposer l'ajout en conservant son ouverture locale

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

#### Scenario: cache par contenu
- **WHEN** un contenu local a déjà été identifié
- **THEN** son identité est conservée par clé locale et réutilisée sans nouvelle recherche lors d'un rafraîchissement du compte ; son statut est un objet de cache avec validité de 15 minutes, relu individuellement après expiration, mutation ou changement de l'épisode local ; l'enrichissement ne change jamais la clé de focus locale

#### Scenario: émissions sans lecture locale
- **WHEN** l'état du suivi, un masquage ou une préférence de source change
- **THEN** les programmes locaux déjà lus sont combinés en mémoire ; aucune nouvelle requête au TV Provider n'est déclenchée, y compris sans compte

### Requirement: Choix et mémorisation du lecteur
Le premier lancement d'un contenu suivi SHALL demander son application de lecture, même avec une seule offre connue. Le choix SHALL être commun au héro et à À suivre, persistant par série ou film, et réutilisé tant que l'app est lançable et qu'aucune indisponibilité du contenu n'est établie.

#### Scenario: premier choix
- **WHEN** l'utilisateur ouvre un contenu sans préférence
- **THEN** les applications vidéo reconnues sont proposées dans cet ordre : apps qui publient ce contenu localement, apps installées correspondant aux services confirmés pour le pays, apps installées correspondant aux services mentionnés non confirmés, puis les autres apps vidéo par ordre alphabétique, enfin « Autre application » pour accéder aux autres apps lançables ; la première a le focus ; cet ordre ne prouve ni abonnement ni disponibilité et seules les offres régionales prouvées sont présentées comme confirmées

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
- **THEN** seule la saison courante est chargée d'abord ; sa liste à plat par numéro affiche « SxxEyy · titre » et une coche pour les vus ; l'épisode courant sinon le premier a le focus ; haut/bas parcourent sans boucle, gauche/droite restent dans la liste ; OK ouvre Ouvrir l'épisode, Marquer comme vu ou non vu, Retour aux épisodes, première action focusée ; consulter ne lance rien et ne modifie aucun statut ; Retour ferme d'abord les actions puis la liste et rend l'origine

#### Scenario: autres saisons à la demande
- **WHEN** Autres saisons est choisi
- **THEN** les numéros de saisons disponibles sont lus sans charger leurs épisodes ; la saison courante sinon la première a le focus, haut/bas parcourent sans boucle et Retour rend la liste d'épisodes ; OK charge uniquement la saison choisie, sans pagination fictive ; chargement, vide, erreur et réessai sont explicites

#### Scenario: état de la liste d'épisodes
- **WHEN** la liste charge, réussit sans épisode, échoue ou perd l'accès au compte
- **THEN** elle expose respectivement chargement, vide, erreur réessayable ou reconnexion, avec Retour toujours disponible ; au retour au premier plan elle se rafraîchit et conserve l'épisode focusé s'il existe, sinon son voisin, sinon l'action d'état

#### Scenario: retour d'une application
- **WHEN** une application ouverte par le parcours commun rend la main au launcher
- **THEN** le retour conserve son origine : restauration de la grille et du contenu pour À suivre, du héro pour le héro ; la carte d'origine retrouve le focus même si celui-ci s'était déplacé sur une voisine à la fermeture du sélecteur ; une proposition de suivi ne vole pas ce focus

### Requirement: Actions sur le compte
Un contenu identifié SHALL proposer les actions applicables : ajouter au suivi depuis un contenu existant, vu/non vu, retirer et, pour une série, archiver. Marquer un épisode SHALL ne modifier que cet épisode ; le masquage local SHALL rester distinct de toute action sur le compte.

#### Scenario: épisode isolé
- **WHEN** S02E05 est marqué vu et S02E03/S02E04 restent non vus
- **THEN** seul S02E05 change de statut ; aucun marquage implicite des précédents n'a lieu

#### Scenario: ajout depuis une source locale
- **WHEN** un compte est connecté et qu'une série ou un film existant est identifié sans ambiguïté et confirmé absent de ce compte
- **THEN** l'ajout est proposé dans son menu local sans exiger une appartenance préalable ; aucune recherche générale de catalogue n'est requise et une identité ou appartenance incertaine ne permet pas l'envoi ; seule une appartenance confirmée après succès active le parcours de lecteur commun

#### Scenario: archivage
- **WHEN** une série est archivée avec succès
- **THEN** elle quitte l'apport du fournisseur ; une reprise locale peut continuer à l'afficher et le masquage local n'est pas modifié

#### Scenario: opération en cours ou refusée
- **WHEN** une mutation charge, échoue, dépasse son délai ou perd son autorisation
- **THEN** l'action concernée expose son état et empêche un double envoi ; aucun succès n'est annoncé sans confirmation et le réessai ou la reconnexion est proposé ; un échec ne retire ni la carte ni le héro, le contenu garde son état précédent et l'erreur reste portée par la notice, y compris après sa fermeture

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

#### Scenario: durée de la fenêtre
- **WHEN** une annulation devient disponible
- **THEN** sa fenêtre dure 10 secondes actives ; focus, arrière-plan et réglages ouverts suspendent le décompte, repris avec le temps restant ; aucun retrait distant ne part avant cette échéance ; une nouvelle action refusée conserve Annuler et explique le refus ; la fin du processus abandonne le retrait

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

### Requirement: Confirmation manuelle au retour
Le suivi SHALL proposer « Terminé ? » après le retour effectif d'un lecteur ; seul un accord explicite SHALL marquer le contenu demandé. Aucun accès aux notifications ou aux sessions média SHALL être demandé dans P9.

#### Scenario: confirmation au retour
- **WHEN** l'utilisateur revient après un lancement effectif de contenu suivi
- **THEN** « Terminé ? » concerne le contenu demandé, pas l'épisode affiché après rafraîchissement ; sans réponse rien ne change ; la proposition reste sans échéance jusqu'à OK, Retour sur le composant, nouveau lancement suivi, déconnexion ou relecture montrant le contenu déjà vu

#### Scenario: départ vers le lecteur
- **WHEN** un lancement réussit
- **THEN** le départ est confirmé dès la pause ou l'arrêt du launcher dans les 15 secondes ; le lancement en attente n'est oublié que sur échec explicite du lancement ou après 15 secondes sans quitter le launcher

#### Scenario: notice Annuler encore affichée
- **WHEN** le retour effectif survient alors qu'une notice Annuler ou d'erreur est encore présente
- **THEN** « Terminé ? » n'est jamais perdu : une notice de marquage terminé est remplacée par la proposition ; un retrait encore annulable, une opération en cours ou une erreur se terminent d'abord, par échéance, Annuler, succès ou fermeture, puis la proposition s'affiche

#### Scenario: compte ou contenu changé
- **WHEN** un retour concerne une ancienne session ou un lancement qui n'a pas quitté le launcher
- **THEN** aucune proposition ni mutation ne vise ce lancement ; un cycle arrière-plan ultérieur ne le réutilise pas

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
- **THEN** seule la liste des plateformes du pays est relue et les offres sont recombinées en mémoire, sans lecture complète du compte ; une offre n'est confirmée que si son service figure dans la liste du pays confirmé ; cela ne prouve ni abonnement ni disponibilité de chaque épisode ; l'ancien pays ne confirme pas le nouveau
