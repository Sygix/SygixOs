## MODIFIED Requirements

### Requirement: Réglages du suivi de visionnage
La catégorie Suivi de visionnage SHALL exposer BetaSeries, connexion ou déconnexion, état du compte, synchronisation, réessai et pays confirmé. Elle SHALL distinguer l'état local durable des demandes en attente et des confirmations distantes. Aucun fournisseur non implémenté SHALL être proposé ; erreurs et révocation distante non confirmée SHALL être distinguées d'un compte vide.

#### Scenario: sans compte
- **WHEN** la catégorie est ouverte sans compte
- **THEN** Connecter BetaSeries est son premier contrôle ; OK ouvre l'association, dont Retour restaure ce contrôle sans connexion implicite

#### Scenario: compte connecté
- **WHEN** la catégorie est ouverte avec un compte
- **THEN** son identité et son état sont affichés avec Synchroniser, pays et Déconnecter ; l'identité persistée de la session liée peut être présentée hors ligne, mais aucune donnée d'une autre session n'est présentée comme courante

#### Scenario: état du chargement
- **WHEN** une synchronisation charge, est vide, échoue ou n'est plus autorisée
- **THEN** le contrôle expose respectivement En cours, compte sans contenu, cause avec Réessayer ou Reconnecter, sans empêcher les autres réglages ni présenter un échec comme succès ; une panne n'efface pas les données locales du compte et une file en attente reste distincte d'une synchronisation terminée

#### Scenario: navigation
- **WHEN** droite entre dans le volet
- **THEN** le premier contrôle utilisable a le focus ; haut/bas parcourent les contrôles sans boucle, gauche retourne à la catégorie et Retour ferme d'abord un panneau actif sinon les réglages ; un contrôle disparu transmet le focus à son voisin sinon à la catégorie

#### Scenario: pays
- **WHEN** le pays est ouvert puis validé ou annulé
- **THEN** une liste déroulante ancrée au contrôle reprend le composant de position d'À suivre, sans modale ; le pays proposé depuis la TV reste distingué d'une valeur choisie ; OK valide, persiste et applique la valeur selon watch-tracking puis referme la liste ; Retour referme sans modification ; le contrôle reprend le focus et n'ajoute pas « · confirmé » après le pays

#### Scenario: navigation dans les pays
- **WHEN** la liste déroulante est ouverte
- **THEN** le pays choisi sinon proposé, sinon la première option prend le focus ; haut/bas parcourent les pays sans boucle, gauche/droite ne quittent pas la liste et Retour l'annule ; une option disparue transmet le focus à sa suivante sinon précédente, sinon referme la liste sur son contrôle

#### Scenario: attribution
- **WHEN** le fournisseur est présenté dans les réglages
- **THEN** BetaSeries est identifié et son adresse officielle est consultable, conformément à l'attribution de la source ; aucun identifiant d'application ni jeton n'est affiché

#### Scenario: demandes hors ligne et réessai
- **WHEN** des vu/non vu d'épisodes ou films sont sauvegardés mais non confirmés
- **THEN** leur attente est visible dans le statut existant ; Synchroniser ou Réessayer sollicite le même travail partagé sans double envoi ; fermer les réglages ne supprime pas la file et son exécution peut continuer en arrière-plan

#### Scenario: retour au premier plan
- **WHEN** l'utilisateur revient dans les réglages après une synchronisation en arrière-plan
- **THEN** le statut reflète les confirmations et demandes restantes du dépôt courant, sans relancer une lecture complète pour le seul affichage du statut ni déplacer le focus d'un contrôle toujours présent

#### Scenario: déconnexion avec attente
- **WHEN** Déconnecter est validé avec des demandes non synchronisées
- **THEN** les données et demandes de ce compte sont supprimées selon « Fin de session BetaSeries » ; aucune conservation du compte déconnecté n'est promise et le contrôle Connecter remplace les contrôles disparus avec le repli de focus existant
