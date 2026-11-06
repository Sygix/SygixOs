# watch-tracking Specification

## Purpose
Suivi de visionnage indépendant du fournisseur et du lecteur : identité des contenus, compte actif, ouverture commune au héro et À suivre, actions et confirmation manuelle au retour.

## Requirements

### Requirement: Fournisseur de suivi actif
Le launcher SHALL utiliser un seul compte de suivi actif, distinct des applications de lecture, parmi BetaSeries et Trakt. Les comportements communs SHALL dépendre d'un contrat extensible ; les protocoles et les actions applicables SHALL suivre le fournisseur choisi. Changer de fournisseur SHALL nécessiter une déconnexion explicite de la session courante.

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

#### Scenario: mise à jour avec BetaSeries connecté
- **WHEN** une installation existante possède une session BetaSeries et des intentions durables
- **THEN** la mise à jour conserve le fournisseur, la session et les intentions liées sans exiger de nouvelle association ni les réattribuer à Trakt

### Requirement: Identité de suivi partagée
La correspondance d'un contenu local avec le suivi SHALL reposer sur une identité non ambiguë, typée et indépendante de l'app. Une série, un épisode et un film SHALL avoir des identités distinctes ; une recherche incertaine ne SHALL ni fusionner les contenus ni autoriser de mutation du compte.

#### Scenario: identifiants connus
- **WHEN** deux sources ont un identifiant externe compatible de même type
- **THEN** elles désignent la même série, le même épisode ou le même film ; l'identité de série ne suffit pas à identifier un épisode sans ses numéros ou son identifiant

#### Scenario: titre traduit ou alias
- **WHEN** une app publie un contenu sans identifiant externe sous un titre traduit
- **THEN** le titre normalisé est comparé strictement au titre, au titre original et aux alias que le fournisseur publie pour ses candidats, puis aux contenus déjà présents dans le compte ; un seul candidat correspondant est retenu, sinon aucun

#### Scenario: échec d'identification
- **WHEN** aucun candidat unique n'a été trouvé pour un contenu local
- **THEN** cet échec est mémorisé pour ce contenu seul et réessayé après la même validité de 15 minutes, jamais conservé jusqu'au redémarrage de l'application

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
Un contenu publié par une app SHALL s'ouvrir directement dans cette app, sans sélecteur. Le premier lancement d'un contenu apporté par le seul compte SHALL demander son application de lecture, même avec une seule offre connue. Le choix SHALL être commun au héro et à À suivre, persistant par série ou film, et réutilisé tant que l'app est lançable et qu'aucune indisponibilité du contenu n'est établie.

#### Scenario: contenu publié par une app
- **WHEN** l'utilisateur ouvre une carte ou un programme du héro publié par une app (par exemple Jellyfin), suivi ou non, sans préférence mémorisée
- **THEN** le contenu s'ouvre directement dans l'app de la source retenue, sinon dans l'unique app qui le publie, sans afficher le sélecteur ; « Ouvrir avec… » reste disponible par appui long

#### Scenario: premier choix
- **WHEN** l'utilisateur ouvre un contenu apporté par le seul compte, sans préférence
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
Un contenu identifié SHALL proposer les actions applicables au fournisseur : ajouter au suivi, vu/non vu, retirer et archiver une série. Vu/non vu SHALL être durable et immédiatement projeté localement, avec envoi après Annuler. Les autres actions SHALL garder leur confirmation distante. Un marquage SHALL ne modifier que le contenu ciblé ; masquage local et actions de compte SHALL rester distincts.

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
- **THEN** l'action expose son état et empêche un double envoi ; pour vu/non vu durablement enregistré, le changement local reste appliqué avec attente de synchronisation distincte du succès distant ; pour les autres mutations, le contenu garde son état précédent en échec ; réessai ou reconnexion reste accessible et fermer la notice ne supprime pas la demande persistante

#### Scenario: résultat réseau incertain
- **WHEN** une réponse est perdue après l'envoi d'une mutation
- **THEN** le statut distant est relu avant toute répétition ; seuls les vu/non vu persistants libérés après Annuler peuvent reprendre automatiquement selon « Synchronisation des intentions locales » ; aucune autre mutation n'entre dans une file hors ligne

#### Scenario: sauvegarde locale impossible
- **WHEN** l'écriture durable d'un vu/non vu échoue
- **THEN** aucun changement n'est annoncé comme enregistré, l'état précédent est conservé et une erreur de stockage réessayable est exposée sans envoi non journalisé

#### Scenario: film hors ligne
- **WHEN** un film du compte connu localement est marqué vu ou non vu sans réseau
- **THEN** sa contribution à regarder est immédiatement recalculée après sauvegarde durable, commune au héro et à À suivre ; le film et son état nécessaire restent disponibles jusqu'à acquittement, sans calcul d'épisode suivant

#### Scenario: action manuelle ou confirmation de retour
- **WHEN** vu/non vu provient du menu d'un contenu, de la liste d'épisodes ou d'une confirmation explicite « Terminé ? »
- **THEN** la même fenêtre Annuler existante et la même libération durable avant envoi s'appliquent pour BetaSeries et Trakt ; aucun nouveau marquage automatique ni nouveau panneau n'est introduit

### Requirement: Annulation non bloquante
Vu/non vu SHALL proposer Annuler dès sa sauvegarde locale et rester non envoyable jusqu'à la fin de sa fenêtre. L'archivage SHALL garder l'annulation de l'action confirmée et le retrait son envoi différé existant. Annuler un marquage encore retenu SHALL restaurer l'état local précédent sans envoyer cette action. Le temps restant SHALL survivre à l'interruption du processus. Annuler SHALL s'afficher dans la bulle de la carte ou du bouton d'origine.

#### Scenario: Annuler à la place de la bulle
- **WHEN** l'utilisateur marque un contenu depuis la bulle « Terminé ? » ou depuis le menu d'une carte
- **THEN** « Annuler » apparaît sous la carte concernée, ou sous la carte qui la remplace à la même place, pendant la fenêtre ; Bas y mène en un appui ; si la carte quitte la rangée, la bulle reste à cette position de la rangée jusqu'à la fin de la fenêtre


#### Scenario: annuler un marquage
- **WHEN** l'utilisateur annule un marquage pendant sa fenêtre
- **THEN** l'état effectif antérieur de l'épisode ou du film est rétabli localement, sans envoyer le marquage annulé ni une opération inverse pour ce marquage ; une intention antérieure encore pertinente n'est pas perdue et l'annulation reste accessible même si la carte passe à l'épisode suivant ou disparaît

#### Scenario: retrait encore annulable
- **WHEN** l'utilisateur annule avant l'échéance d'un retrait du compte
- **THEN** la carte retrouve sa place de tri et aucun retrait distant n'a été envoyé

#### Scenario: durée de la fenêtre
- **WHEN** une annulation devient disponible
- **THEN** sa fenêtre dure 10 secondes actives ; focus, arrière-plan et réglages ouverts suspendent le décompte, repris avec le temps restant ; aucun retrait distant ni marquage vu/non vu ne part avant cette échéance ; une nouvelle action refusée conserve Annuler et explique le refus ; la fin du processus abandonne le retrait mais pas les vu/non vu persistants ni leur temps restant

#### Scenario: annuler un archivage
- **WHEN** l'utilisateur annule un archivage confirmé
- **THEN** l'état d'archive précédent est rétabli puis relu, sans retrait de la série ni modification de ses épisodes vus ou de son masquage local

#### Scenario: échéance puis échec
- **WHEN** la fenêtre d'annulation se termine puis que le retrait échoue
- **THEN** le contenu est réconcilié avec le compte et l'échec est visible avec réessai ; aucune restauration d'historique non garantie n'est promise après suppression distante

#### Scenario: changement de compte ou interruption
- **WHEN** une action différée n'est pas encore envoyée et que le compte est déconnecté ou le processus interrompu
- **THEN** la déconnexion abandonne et supprime toutes les demandes du compte ; l'interruption seule conserve les vu/non vu persistants et leur état d'annulation mais abandonne le retrait différé ; une requête déjà partie ne peut pas être déclarée annulée sans vérifier son résultat

#### Scenario: interaction avec le retour
- **WHEN** une proposition ou une annulation est présentée
- **THEN** elle n'interrompt pas la navigation, ne prend pas spontanément le focus et reste accessible au D-pad pendant sa fenêtre ; son échéance n'exécute jamais un marquage vu à la place de l'utilisateur

#### Scenario: annulation d'un film
- **WHEN** l'utilisateur annule le marquage d'un film
- **THEN** l'état de film antérieur exact est rétabli sans retrait du compte ni revisionnage implicite ; ses métadonnées utiles ne sont pas purgées pendant la fenêtre d'annulation

#### Scenario: reprise après interruption
- **WHEN** le processus ou la TV redémarre avec un marquage retenu par Annuler
- **THEN** le marquage reste projeté, son temps restant est restauré et Annuler réapparaît dans sa bulle au retour de l'accueil sans prendre le focus ; le décompte ne reprend que dans les conditions actives existantes et aucun travail en arrière-plan ne peut contourner cette retenue

#### Scenario: arrêt brutal sans dernier état exact
- **WHEN** Android interrompt le processus avant la dernière sauvegarde du décompte
- **THEN** le dernier temps restant durable est repris ; une prolongation conservatrice est admise, jamais une réduction par supposition ; les écritures sont hors du thread UI et ne suivent pas les frames de rendu

#### Scenario: migration des demandes antérieures
- **WHEN** une demande créée par une version antérieure est migrée
- **THEN** son état de synchronisation et son éventuel envoi déjà parti sont conservés sans nouvelle fenêtre Annuler rétroactive ; la retenue avant envoi s'applique aux nouveaux marquages

#### Scenario: libération durable et course avec Annuler
- **WHEN** la fenêtre se termine ou qu'Annuler intervient en concurrence avec son échéance
- **THEN** la libération et l'annulation sont ordonnées par une seule décision durable ; seul le marquage libéré peut être synchronisé et aucun déclencheur manuel, au premier plan ou en arrière-plan ne peut envoyer une révision encore annulable

### Requirement: Confirmation manuelle au retour
Le suivi SHALL proposer « Terminé ? » après le retour effectif d'un lecteur, dans une bulle attachée à l'origine du lancement (sous la carte d'À suivre ou à droite du bouton du héro) ; seul un accord explicite SHALL marquer le contenu demandé. Aucun accès aux notifications ou aux sessions média SHALL être demandé dans P9.

#### Scenario: bulle sous l'origine
- **WHEN** l'utilisateur revient d'un lecteur lancé depuis une carte d'À suivre
- **THEN** le focus revient sur la carte d'origine et une bulle « Terminé ? » avec « Vu » et « Pas fini » apparaît juste sous elle ; Bas y mène en un appui, « Vu » marque le contenu demandé, « Pas fini », Haut ou Retour la quittent sans rien changer ; la bulle ne prend jamais le focus d'elle-même et aucune notice n'est affichée en bas à droite

#### Scenario: bulle à côté du bouton du héro
- **WHEN** le lecteur avait été lancé depuis le héro
- **THEN** la même bulle apparaît à droite du bouton « Ouvrir », centrée sur lui pour ne pas chevaucher le dock ; Droite depuis le bouton l'atteint, Gauche y revient, Bas mène toujours au dock ; mêmes actions


#### Scenario: confirmation au retour
- **WHEN** l'utilisateur revient après un lancement effectif de contenu suivi
- **THEN** « Terminé ? » concerne le contenu demandé, pas l'épisode affiché après rafraîchissement ; sans réponse rien ne change ; la proposition reste sans échéance jusqu'à OK, Retour sur le composant, nouveau lancement suivi, déconnexion ou relecture montrant le contenu déjà vu

#### Scenario: départ vers le lecteur
- **WHEN** un lancement réussit
- **THEN** le départ est confirmé dès la pause ou l'arrêt du launcher dans les 15 secondes ; le lancement en attente n'est oublié que sur échec explicite du lancement ou après 15 secondes sans quitter le launcher

#### Scenario: Annuler encore affiché
- **WHEN** le retour effectif survient alors qu'une bulle Annuler ou une erreur est encore présente
- **THEN** « Terminé ? » n'est jamais perdu : une bulle Annuler de marquage est remplacée par la proposition ; un retrait encore annulable, une opération en cours ou une erreur se terminent d'abord, par échéance, Annuler, succès ou fermeture, puis la proposition s'affiche

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

### Requirement: État de suivi local durable
Le suivi SHALL conserver le dernier état connu du compte et ses intentions locales par fournisseur et identifiant stable de compte. L'état effectif SHALL appliquer les intentions en attente au dernier état distant connu et alimenter conjointement héro et À suivre. Un redémarrage sans réseau SHALL restaurer cet état d'une session déjà liée, sans attendre la lecture distante de l'identité.

#### Scenario: restauration et rafraîchissement
- **WHEN** une session précédemment liée et son cache sont restaurés, notamment au retour au premier plan
- **THEN** les contenus locaux du compte et les intentions apparaissent sans attendre BetaSeries ; chargement et ancienneté restent distincts de la confirmation distante et les rafraîchissements respectent le contrat du fournisseur

#### Scenario: premier chargement sans cache
- **WHEN** aucune donnée de compte n'a encore été enregistrée et que le réseau manque
- **THEN** une indisponibilité réessayable est exposée, sans compte inventé ni blocage des programmes locaux

#### Scenario: cache vide et source absente
- **WHEN** le cache du compte est vide ou qu'aucune session liée n'est disponible
- **THEN** un compte connu vide reste distinct d'une absence de session ; aucune donnée d'un autre compte ne sert de repli

#### Scenario: erreur de stockage ou accès refusé
- **WHEN** le cache est illisible ou que l'autorisation du compte est refusée
- **THEN** l'erreur de stockage ou la reconnexion est exposée distinctement ; aucune intention n'est envoyée sous une session non vérifiée et les programmes locaux gardent leurs règles

#### Scenario: rafraîchissement concurrent
- **WHEN** une lecture distante arrive après un marquage local ou une confirmation plus récente
- **THEN** elle ne rétablit pas l'ancien statut effectif, ne supprime pas l'intention en attente et ne remplace pas une confirmation plus récente

### Requirement: Synchronisation des intentions locales
Les vu/non vu durables d'épisodes et films libérés après Annuler SHALL être synchronisés automatiquement, y compris en arrière-plan selon Android. La dernière intention locale en attente SHALL primer sur l'état distant. Seule une confirmation de sa cible SHALL l'acquitter ; un ancien résultat ne SHALL pas acquitter la demande courante. Une intention encore annulable SHALL rester retenue pour tous les déclencheurs.

#### Scenario: reprise après panne ou redémarrage
- **WHEN** une demande libérée reste en attente et que réseau et session autorisent le travail
- **THEN** une tentative bornée est planifiée même sans accueil visible, y compris après redémarrage du processus ou de la TV ; les restrictions Android peuvent différer l'exécution, sans perte de demande ni promesse d'envoi instantané

#### Scenario: résultat déjà appliqué
- **WHEN** une demande potentiellement envoyée est reprise et que la lecture distante confirme déjà sa cible
- **THEN** la demande correspondante est acquittée sans nouvelle mutation ; une lecture en échec maintient l'incertitude sans renvoi aveugle

#### Scenario: intention prioritaire et nouvelle révision
- **WHEN** le statut distant diffère ou qu'un nouveau vu/non vu remplace une demande pendant son envoi
- **THEN** l'état désiré local courant reste projeté ; l'ancien résultat ne supprime pas la nouvelle demande et la convergence vise la dernière intention explicite de la TV après sa propre fenêtre Annuler

#### Scenario: refus et erreurs temporaires
- **WHEN** le réseau échoue, le serveur limite les appels ou l'accès au compte est refusé
- **THEN** les erreurs temporaires gardent les intentions avec réessais espacés respectant le serveur ; le refus d'accès suspend les envois et expose la reconnexion selon le fournisseur ; aucun autre compte n'est utilisé

#### Scenario: absence de travail et concurrence
- **WHEN** la file est vide ou que plusieurs déclencheurs demandent sa synchronisation
- **THEN** aucun polling réseau ne reste actif sans travail utile et une même demande n'est pas envoyée concurremment ; le réessai manuel et le retour au premier plan réutilisent le même suivi des confirmations

#### Scenario: confirmation sans réserve complète
- **WHEN** le marquage est confirmé mais que le complément d'épisodes échoue
- **THEN** le marquage est acquitté sans être renvoyé ; la réserve reste explicitement incomplète et son complément peut être réessayé séparément

#### Scenario: file uniquement retenue par Annuler
- **WHEN** toutes les intentions restantes ont encore une fenêtre d'annulation ouverte
- **THEN** elles restent persistées sans envoi ni boucle de travail inutile ; leur libération durable planifie la synchronisation et une planification interrompue est réparée à la reprise
