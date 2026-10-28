## MODIFIED Requirements

### Requirement: Réglages du suivi de visionnage
La catégorie Suivi de visionnage SHALL exposer BetaSeries ou Trakt selon le fournisseur choisi, connexion ou déconnexion, état du compte, synchronisation, réessai et pays confirmé. Elle SHALL distinguer état local durable, demandes en attente, confirmation distante et reconnexion. Une session en restauration ou conservée après panne SHALL proposer un réessai sans nouvelle association implicite.

#### Scenario: sans compte
- **WHEN** la catégorie est ouverte avec une absence de session confirmée
- **THEN** le sélecteur de fournisseur précède Connecter, adapté au fournisseur choisi ; OK sur Connecter ouvre son association et Retour restaure ce contrôle sans connexion implicite

#### Scenario: compte connecté
- **WHEN** la catégorie est ouverte avec un compte
- **THEN** son fournisseur, son identité et son état sont affichés avec Synchroniser, pays et Déconnecter ; l'identité persistée de la session liée peut être présentée hors ligne, mais aucune donnée d'une autre session n'est présentée comme courante

#### Scenario: état du chargement
- **WHEN** une synchronisation charge, est vide, échoue ou n'est plus autorisée
- **THEN** le contrôle expose respectivement En cours, compte sans contenu, cause avec Réessayer ou Reconnecter, sans empêcher les autres réglages ni présenter un échec comme succès ; une panne n'efface pas les données locales du compte et une file en attente reste distincte d'une synchronisation terminée

#### Scenario: restauration sans identité chargée
- **WHEN** la session est en cours de restauration ou que son identité n'a pas pu être chargée
- **THEN** son fournisseur reste celui de la session, le sélecteur reste verrouillé et le premier contrôle utilisable conserve une place et un focus stables avec chargement ou Réessayer ; Connecter n'est pas proposé par supposition ; Réessayer relit le stockage ou le compte avec la session conservée, sans code ni QR ; seul le traitement confirmé du refus de session peut proposer Reconnecter

#### Scenario: stockage temporairement indisponible
- **WHEN** la lecture sécurisée échoue sans perte définitive confirmée
- **THEN** le message explique que la connexion est conservée et propose de réessayer, sans demander de se reconnecter ni autoriser un changement de fournisseur

#### Scenario: navigation
- **WHEN** droite entre dans le volet
- **THEN** le premier contrôle utilisable a le focus : le sélecteur sans session, Synchroniser ou son état de chargement/réessai/reconnexion avec session ; haut/bas parcourent les contrôles sans boucle, gauche retourne à la catégorie et Retour ferme d'abord un panneau actif sinon les réglages ; un contrôle disparu transmet le focus à son voisin sinon à la catégorie

#### Scenario: pays
- **WHEN** le pays est ouvert puis validé ou annulé
- **THEN** une liste déroulante ancrée au contrôle reprend le composant de position d'À suivre, sans modale ; le pays proposé depuis la TV reste distingué d'une valeur choisie ; OK valide, persiste et applique la valeur selon watch-tracking puis referme la liste ; Retour referme sans modification ; le contrôle reprend le focus et n'ajoute pas « · confirmé » après le pays

#### Scenario: navigation dans les pays
- **WHEN** la liste déroulante est ouverte
- **THEN** le pays choisi sinon proposé, sinon la première option prend le focus ; haut/bas parcourent les pays sans boucle, gauche/droite ne quittent pas la liste et Retour l'annule ; une option disparue transmet le focus à sa suivante sinon précédente, sinon referme la liste sur son contrôle

#### Scenario: attribution
- **WHEN** le fournisseur est présenté dans les réglages
- **THEN** son nom, son adresse officielle et son attribution requise sont présentés pour le fournisseur concerné ; aucun identifiant d'application ni jeton n'est affiché et l'attribution ne présente pas SygixOs comme une application officielle du service

#### Scenario: demandes hors ligne et réessai
- **WHEN** des vu/non vu d'épisodes ou films sont sauvegardés mais non confirmés
- **THEN** leur attente est visible dans le statut existant ; Synchroniser ou Réessayer sollicite le même travail partagé sans double envoi ni contournement d'une fenêtre Annuler ; fermer les réglages ne supprime pas la file et son exécution peut continuer en arrière-plan pour les demandes libérées

#### Scenario: retour au premier plan
- **WHEN** l'utilisateur revient dans les réglages après une synchronisation en arrière-plan
- **THEN** le statut reflète les confirmations et demandes restantes du dépôt courant, sans relancer une lecture complète pour le seul affichage du statut ni déplacer le focus d'un contrôle toujours présent

#### Scenario: déconnexion avec attente
- **WHEN** Déconnecter est validé avec des demandes non synchronisées
- **THEN** les données et demandes de ce compte sont supprimées selon le contrat de fin de session du fournisseur ; aucune conservation du compte déconnecté n'est promise et les contrôles disponibles reprennent le focus selon le repli existant

## ADDED Requirements

### Requirement: Sélecteur du fournisseur de suivi
Le fournisseur SHALL se choisir par la liste déroulante existante, avant Connecter, avec BetaSeries puis Trakt. Le dernier choix SHALL être persisté sur la TV ; sans compte ni choix, BetaSeries SHALL être initial. Une session existante SHALL imposer son fournisseur et verrouiller le sélecteur avec « Déconnectez-vous pour changer de fournisseur ». Aucun changement implicite de compte ne SHALL résulter d'un choix.

#### Scenario: première ouverture et choix mémorisé
- **WHEN** les réglages s'ouvrent sans session ni choix, ou après un choix précédent
- **THEN** la valeur initiale est respectivement BetaSeries ou le dernier fournisseur choisi ; le sélecteur est le premier contrôle utilisable et les textes, l'attribution et l'association de Connecter correspondent à cette valeur

#### Scenario: ouverture et choix dans la liste
- **WHEN** OK ouvre le sélecteur puis l'utilisateur parcourt ou valide ses valeurs
- **THEN** le composant ancré de « Position d'À suivre » est réutilisé avec les mêmes matériau, coche, focus, bords sans boucle et règles Retour ; OK persiste le choix et referme sur le sélecteur ; Retour referme sans modifier le choix ; choisir une valeur ne lance ni découverte ni association

#### Scenario: session active ou restauration
- **WHEN** une session est active, en restauration ou conservée après une panne
- **THEN** le sélecteur reste visible sur son fournisseur, non modifiable et avec son explication ; le premier contrôle utilisable est Synchroniser ou son état adapté ; aucune tentative de changement ne purge les demandes ou ne remplace la session

#### Scenario: connexion et déconnexion
- **WHEN** l'association réussit ou qu'une déconnexion est effectuée
- **THEN** le sélecteur est verrouillé sur la session confirmée puis redevient modifiable après déconnexion effective ; le focus suit le contrôle utilisable de l'état obtenu, sans ouvrir automatiquement une nouvelle liste ni association

#### Scenario: liste ouverte et changement de contexte
- **WHEN** les réglages passent en arrière-plan, sont fermés ou changent de session pendant que la liste est ouverte
- **THEN** la liste ferme sans choix implicite ; au retour, la valeur persistée ou le fournisseur imposé par la session est affiché et le contrôle encore utilisable sinon son repli reçoit le focus

#### Scenario: option disparue et sélecteurs
- **WHEN** l'option focusée disparaît ou qu'un test cible le choix de fournisseur
- **THEN** le repli suivant puis précédent du composant existant s'applique, sinon la liste ferme sur son contrôle ; les tags tracking-provider, tracking-provider-list, tracking-provider-betaseries et tracking-provider-trakt identifient le parcours sans donnée de compte
