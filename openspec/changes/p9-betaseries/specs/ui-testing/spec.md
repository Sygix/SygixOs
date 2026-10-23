# Spec Delta

## MODIFIED Requirements

### Requirement: Sélecteurs stables
Les zones, tuiles et contrôles SHALL exposer des testTags stables, sans dépendre de leur texte, position ou d'une donnée personnelle. Les contrôles du suivi SHALL ajouter leurs sélecteurs aux sélecteurs existants, sans renommer les éléments locaux.

#### Scenario: sélection par tag
- **WHEN** un test cible héro, dock, grille, shelf, À suivre, titres, menus, cartes, badges, progression, placeholder, squelette, erreur ou réglages d'accueil
- **THEN** il conserve les tags existants : zone-hero, zone-dock, zone-grid, shelf-panel, zone-upnext, section-title-upnext, section-title-apps, app-menu, upnext-menu, upnext-menu-remove, upnext-skeleton, upnext-error, settings-home-screen, setting-upnext-visible, setting-upnext-position, setting-upnext-position-list, settings-category-HOME_SCREEN et préfixes app-tile-, upnext-card-, upnext-card-badge-, upnext-card-progress-, upnext-card-placeholder-, upnext-menu-entry-, setting-upnext-position-

#### Scenario: contrôles du suivi
- **WHEN** un test cible le suivi
- **THEN** il utilise tracking-settings, tracking-connect, tracking-disconnect, tracking-sync, tracking-country, tracking-media-access, tracking-device-code, tracking-device-qr, tracking-device-retry, tracking-status, tracking-status-retry, tracking-player-picker, tracking-other-app, tracking-episodes, tracking-return-prompt, tracking-undo et les préfixes tracking-player-, tracking-episode-, tracking-action-, avec identités fictives en test

### Requirement: Navigation D-pad des trois zones
La navigation héro, dock et grille, À suivre et titres compris, SHALL rester testée par key events physiques en Compose à taille TV. Les sources du compte SHALL être simulées indépendamment du TV Provider ; menus, propositions et retours de suivi SHALL ne pas changer les trois paliers.

#### Scenario: descente
- **WHEN** bas est pressé depuis le héro avec dock et À suivre en tête
- **THEN** le focus passe au dock, à la première carte sous section-title-upnext puis aux apps sous section-title-apps

#### Scenario: descente sans dock
- **WHEN** le dock est vide
- **THEN** bas atteint directement la première carte puis les apps

#### Scenario: descente depuis le dock
- **WHEN** bas est pressé depuis le dock
- **THEN** la première carte de la première entrée en grille reçoit le focus

#### Scenario: descente sans rangée Up Next
- **WHEN** la rangée est réellement masquée (désactivée ou sans contenu affichable de toutes les sources)
- **THEN** la première tuile d'app prend le focus, le titre À suivre est absent et Applications est en haut

#### Scenario: titres de section
- **WHEN** chaque position de rangée est parcourue au D-pad
- **THEN** les titres sont dans l'ordre réglé, jamais focusables, entièrement visibles au-dessus de leur première ligne ; le premier titre est aligné en haut de grille

#### Scenario: remontée
- **WHEN** haut est pressé depuis la première rangée d'apps
- **THEN** À suivre reprend le focus s'il est avant les apps, puis dock et héro ; masqué, il est sauté

#### Scenario: position après les applications
- **WHEN** À suivre est après les apps
- **THEN** dock mène aux apps, leur dernière rangée mène à À suivre, son bord bas reste sans boucle et haut revient à la dernière rangée d'apps

#### Scenario: retour au héro
- **WHEN** Retour est pressé depuis la grille
- **THEN** le héro reprend le focus

#### Scenario: retour dans la grille à la position laissée
- **WHEN** le test quitte puis retrouve une carte non initiale ou une rangée d'apps profonde
- **THEN** l'élément reprend le focus à la même position à l'écran

#### Scenario: carte focusée disparue
- **WHEN** le test retire une carte au milieu puis à la fin
- **THEN** la suivante puis la précédente reçoit le focus dans zone-upnext

#### Scenario: restauration au retour d'une app
- **WHEN** le test ouvre une quatrième carte puis revient avec le même contenu, un épisode suivant ou son absence
- **THEN** position verticale conservée ; contenu retrouvé premier, focusé et aligné sur la marge gauche, sinon repli à la position de la carte ouverte ou précédente

#### Scenario: rangée devenue vide
- **WHEN** un rechargement ou retour vide la rangée pour chaque position réglée
- **THEN** rangée et titre disparaissent, focus à la première app avant, dernière app après, sinon héro

#### Scenario: états de la rangée
- **WHEN** les états locaux et du compte sont variés indépendamment
- **THEN** squelette initial, maintien local valide, erreur locale et vide suivent leurs contrats ; refus TV n'efface pas le compte et panne du compte ne masque pas les apps

#### Scenario: structure des cartes
- **WHEN** les cartes locales existantes sont rendues à taille TV avec épisode, film, image et icône absentes
- **THEN** largeur (contenu − 3 × GridSpacing) / 4, ratio 16:9, quatre cartes entières puis cinquième coupée, textes épisode/film, badges/progression/placeholders conditionnels, défilement de la cinquième et alignement de la dernière à ScreenMarginH sont conservés, sans texte d'action ou nom d'app et sans assertion de pixel

#### Scenario: squelette et carte d'erreur
- **WHEN** la rangée locale passe du chargement à l'erreur sans autre source prête
- **THEN** cinq cartes non focusables à deux barres sont remplacées par la seule carte upnext-error focusable avec ses textes existants sous le titre

#### Scenario: menu Ouvrir avec
- **WHEN** l'appui long est simulé sur une carte locale non associée
- **THEN** le menu, l'en-tête et les sources existants sont présents, première app focusée, Retirer après la dernière, sans boucle ; Retour rend la carte, choisir ouvre l'app et retirer persiste le masquage avec focus suivant/précédent ; une seconde carte de la même série disparaît aussi

#### Scenario: réglage de position
- **WHEN** Position d'À suivre est ouvert, modifié, annulé et laissé ouvert pendant un passage en arrière-plan
- **THEN** liste et contrôle gardent ordre, ancrage, coche courante, pilule sans zoom, focus et D-pad existants ; validation persiste et replace la rangée, Retour ferme sans modifier, arrière-plan ferme la liste sans quitter la page et restaure le contrôle

#### Scenario: réglage de visibilité
- **WHEN** Afficher À suivre est désactivé puis réactivé
- **THEN** titre et rangée disparaissent puis reviennent à la position persistée ; Position reste modifiable ; D-pad et focus du contrôle sont inchangés

#### Scenario: aucune app TV détectée
- **WHEN** le catalogue d'apps est vide
- **THEN** Applications est absent et le message existant est centré ; À suivre dépend de toutes ses sources

#### Scenario: tests existants de la transition héro ↔ grille
- **WHEN** les tests de transition sans À suivre s'exécutent
- **THEN** ils gardent Afficher À suivre désactivé, de sorte que leurs premières tuiles désignent toujours les apps

#### Scenario: sources indépendantes et propositions
- **WHEN** le test refuse READ_TV_LISTINGS avec compte accessible, puis fait échouer le compte avec apps accessibles
- **THEN** les contenus de la source encore accessible sont navigables ; une proposition de retour ou Annuler ne vole pas leur focus et son action est atteignable selon le parcours validé en Penpot

#### Scenario: ouverture depuis le héro
- **WHEN** le test choisit un lecteur depuis le héro puis ouvre la même série depuis À suivre et inversement
- **THEN** la préférence est commune ; le retour restaure le bon contexte et le signal de lecture vise le contenu demandé, pas un épisode suivant affiché entre-temps

## ADDED Requirements

### Requirement: Couverture du fournisseur de suivi
Les contrats de suivi SHALL être testés en JUnit avec transport, stockage, compte, lecteurs, horloges et sessions média contrôlés, sans réseau réel. Les tests SHALL vérifier les limites de reprise, pagination, isolation des comptes et absence de mutation sur preuve ambiguë.

#### Scenario: association et configuration
- **WHEN** le transport simule ressource web changée, attente OAuth, 502 non JSON, expiration, succès puis jeton refusé
- **THEN** l'extraction échoue proprement ou renouvelle une seule fois, le polling respecte intervalle et expiration, un succès exige un compte confirmé et une ancienne réponse ne reconnecte pas une session fermée

#### Scenario: pagination et fusion
- **WHEN** plusieurs pages contiennent doublons, reprises, épisodes vus et non vus, films remakes et dates manquantes
- **THEN** la fusion conserve identités et sources exactes, une seule carte par série identifiée et 20 cartes après tri ; le héro peut recevoir les candidats au-delà de 20 et aucun résultat partiel ne purge la mémoire

#### Scenario: mutations et annulation
- **WHEN** le test marque S02E05, annule puis retire un contenu avec annulation avant échéance, échec après échéance et réponse perdue
- **THEN** bulk reste désactivé, les précédents ne changent pas, aucune suppression n'est envoyée avant échéance et une réponse incertaine déclenche une lecture avant réessai ; aucune mutation différée ne traverse une déconnexion

#### Scenario: fin de lecture
- **WHEN** le test fournit identité fiable et fin prouvée, puis seulement pause, arrêt, titre ambigu, carte retirée et compte changé
- **THEN** seul le premier cas autorise au plus un marquage du bon contenu ; les autres restent sans mutation et la confirmation au retour ne marque rien sans action

### Requirement: Couverture des parcours de suivi TV
Les nouveaux parcours SHALL être testés en Compose TV avec testTags, key events physiques et données fictives : connexion, pays, accès média, sélecteur, épisodes, actions et erreur. Rendu et navigation SHALL suivre les parcours fonctionnels et la passe Penpot validée, sans capture comparative.

#### Scenario: association
- **WHEN** le test ouvre puis ferme une association, laisse expirer son code et demande un nouveau code
- **THEN** adresse/code/QR appartiennent à la même tentative, le QR n'est pas focusable, les actions ont un focus initial et Retour restaure Connecter sans poller une ancienne tentative

#### Scenario: sélecteur et épisodes
- **WHEN** le test parcourt les lecteurs et Autre application, puis les épisodes vus et non vus, annule et simule la disparition de l'entrée focusée
- **THEN** les bords ne bouclent pas, l'origine est restaurée, consulter ne modifie rien et le repli suivant/précédent reste utilisable ; chargement, vide, erreur et reconnexion sont distincts

#### Scenario: badge et masquage
- **WHEN** une carte de suivi n'a pas de lecteur choisi puis en reçoit un et est masquée/réaffichée
- **THEN** son badge passe du fournisseur à l'app ; le masquage persiste pour la série toutes sources et les candidats BetaSeries du héro, sans mutation du compte ; Réafficher restaure l'éligibilité

#### Scenario: autorisation média
- **WHEN** le double refuse, accorde, retire l'accès ou ne fournit aucun écran système
- **THEN** le contrôle affiche l'état réel, retrouve le focus et laisse fonctionner le suivi manuel sans nouvelle sollicitation automatique
