## MODIFIED Requirements

### Requirement: Réglages du suivi de visionnage
La catégorie Suivi de visionnage SHALL exposer BetaSeries, connexion ou déconnexion, état du compte, synchronisation, réessai et pays confirmé. Aucun fournisseur non implémenté SHALL être proposé ; erreurs et révocation distante non confirmée SHALL être distinguées d'un compte vide. Une session en restauration ou conservée après une panne SHALL proposer un réessai, pas une nouvelle association.

#### Scenario: sans compte
- **WHEN** la catégorie est ouverte avec une absence de session confirmée
- **THEN** Connecter BetaSeries est son premier contrôle ; OK ouvre l'association, dont Retour restaure ce contrôle sans connexion implicite

#### Scenario: compte connecté
- **WHEN** la catégorie est ouverte avec un compte
- **THEN** son identité et son état sont affichés avec Synchroniser, pays et Déconnecter ; aucune donnée d'une session précédente n'est présentée comme courante

#### Scenario: état du chargement
- **WHEN** une synchronisation charge, est vide, échoue ou n'est plus autorisée
- **THEN** le contrôle expose respectivement En cours, compte sans contenu, cause avec Réessayer ou Reconnecter, sans empêcher les autres réglages ni présenter un échec comme succès

#### Scenario: restauration sans identité chargée
- **WHEN** la session est en cours de restauration ou que son identité n'a pas pu être chargée
- **THEN** le premier contrôle conserve une place et un focus stables, affiche le chargement ou Réessayer avec la cause réessayable ; Connecter BetaSeries n'est pas proposé ; OK sur Réessayer relit le stockage ou le compte avec le jeton conservé, sans code ni QR ; seul un refus explicite du jeton propose Reconnecter

#### Scenario: stockage temporairement indisponible
- **WHEN** la lecture sécurisée échoue sans perte définitive confirmée
- **THEN** le message explique que la connexion est conservée et propose de réessayer, sans demander de se reconnecter

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
