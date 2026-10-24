# Spec Delta

## MODIFIED Requirements

### Requirement: Sélecteurs stables
Les zones, tuiles et contrôles SHALL exposer des testTags stables, sans dépendre de leur texte, position ou d'une donnée personnelle. Les contrôles du suivi SHALL ajouter leurs sélecteurs aux sélecteurs existants, sans renommer les éléments locaux.

#### Scenario: sélection par tag
- **WHEN** un test cible héro, dock, grille, shelf, À suivre, titres, menus, cartes, badges, progression, placeholder, squelette, erreur ou réglages d'accueil
- **THEN** il conserve les tags zone-hero, zone-dock, zone-grid, shelf-panel, zone-upnext, section-title-upnext, section-title-apps, app-menu, upnext-menu, upnext-menu-remove, upnext-skeleton, upnext-error, settings-home-screen, setting-upnext-visible, setting-upnext-position, setting-upnext-position-list, settings-category-HOME_SCREEN et les formes `app-tile-<package>`, `upnext-card-<key>`, `upnext-card-badge-<key>`, `upnext-card-progress-<key>`, `upnext-card-placeholder-<key>`, `upnext-menu-entry-<package>`, `setting-upnext-position-<VALUE>`, sans dépendre du libellé affiché

#### Scenario: contrôles du suivi
- **WHEN** un test cible le suivi
- **THEN** il utilise tracking-settings, tracking-connect, tracking-disconnect, tracking-sync, tracking-country, tracking-country-list, tracking-device, tracking-device-code, tracking-device-qr, tracking-device-retry, tracking-device-close, tracking-status, tracking-status-detail, tracking-status-retry, tracking-status-close, tracking-player-picker, tracking-player-close, tracking-other-app, tracking-episodes, tracking-episodes-close, tracking-episodes-retry, tracking-episode-actions, tracking-episode-open, tracking-episode-seen, tracking-episode-back, tracking-notification, tracking-return-prompt, tracking-undo et les préfixes tracking-player-, tracking-episode-, tracking-country-, tracking-card-, tracking-hero-, tracking-action-<add|seen|archive|remove|episodes|player>, avec identités fictives

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
- **WHEN** le test quitte la grille depuis une carte À suivre non initiale, redescend, puis recommence depuis une tuile d'une rangée d'apps profonde
- **THEN** à chaque redescente la carte puis la tuile reprend le focus à la même position à l'écran qu'avant la sortie

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
- **WHEN** le test compose les cartes locales à taille TV avec au moins cinq items, dont un épisode CONTINUE avec image, un film, un item sans image et un item sans icône d'app, puis parcourt une rangée de huit cartes
- **THEN** chaque `upnext-card-<key>` a une largeur (contenu − 3 × `Dimens.GridSpacing`) / 4 et un ratio 16:9, quatre cartes entières depuis la marge gauche et cinquième dépassant le bord droit ; `upnext-card-badge-<key>` est présent sauf sans icône, `upnext-card-progress-<key>` seulement pour l'item en cours, `upnext-card-placeholder-<key>` seulement sans image ; l'épisode porte deux nœuds texte (série et « SxxEyy · titre d'épisode »), le film un seul, sans texte d'action ni nom d'app ; droite jusqu'à la cinquième la rend entière, puis jusqu'à la huitième aligne son bord droit sans zoom à `Dimens.ScreenMarginH`, symétriquement au bord gauche initial ; aucune assertion de pixel

#### Scenario: squelette et carte d'erreur
- **WHEN** la rangée locale passe du chargement à l'erreur sans autre source prête
- **THEN** `upnext-skeleton` contient cinq nœuds de la taille d'une carte, chacun avec deux barres et aucun focusable ; en erreur `upnext-error` est un nœud unique de la taille d'une carte, focusable, portant « Impossible de charger » et « OK pour réessayer » sous `section-title-upnext`

#### Scenario: menu Ouvrir avec
- **WHEN** l'appui long est simulé sans compte connecté sur une carte locale, avec une seule source puis plusieurs
- **THEN** `upnext-menu` montre vignette, titre et « Ouvrir avec… », une entrée `upnext-menu-entry-<package>` par source dans l'ordre, première focusée puis `upnext-menu-remove` juste après la dernière ; Bas depuis la dernière app atteint Retirer et Bas depuis Retirer ne fait rien ; OK ouvre l'app et ferme le menu, Retour ferme et rend la carte ; valider Retirer ferme, persiste le masquage, retire aussi la seconde carte de même série d'une autre app et donne le focus à la suivante sinon précédente

#### Scenario: réglage de position
- **WHEN** le test ouvre Écran d'accueil, ouvre Position d'À suivre, change de valeur et valide, recommence avec Retour puis laisse la liste ouverte pendant un passage en arrière-plan/premier plan
- **THEN** `settings-home-screen` montre titre « Écran d'accueil », description, `setting-upnext-visible` avant `setting-upnext-position` avec valeur courante et chevron ; la liste contient exactement les deux `setting-upnext-position-<VALUE>` dans l'ordre, valeur courante focusée et seule cochée, rectangle sous le contrôle et dans le volet droit ; Haut/Bas sans boucle, Gauche/Droite sans effet ; OK ferme, restaure le contrôle, persiste et replace titre/rangée au home ; Retour ferme sans modifier ni quitter les réglages, le cycle arrière-plan ferme aussi et restaure le contrôle sans changer la valeur ; liste fermée Haut atteint la visibilité, Gauche `settings-category-HOME_SCREEN`, Retour ferme ; rectangle de chaque ligne identique avec/sans focus, pilule sans zoom et aucune assertion de pixel

#### Scenario: réglage de visibilité
- **WHEN** Afficher À suivre est désactivé puis réactivé
- **THEN** titre et rangée disparaissent et sont sautés, désactivation conservée après redémarrage ; Bas atteint Position toujours focusable/modifiable, sans état grisé, avec même rectangle et même liste qu'activé ; une position modifiée pendant la désactivation s'applique au titre et à la rangée dès réactivation ; Haut/Droite sur la visibilité ne font rien, Gauche rend `settings-category-HOME_SCREEN` et Retour ferme les réglages

#### Scenario: aucune app TV détectée
- **WHEN** le catalogue d'apps est vide
- **THEN** Applications est absent et le message existant est centré ; À suivre dépend de toutes ses sources

#### Scenario: tests existants de la transition héro ↔ grille
- **WHEN** les tests de transition sans À suivre s'exécutent
- **THEN** ils gardent Afficher À suivre désactivé, de sorte que leurs premières tuiles désignent toujours les apps

#### Scenario: sources indépendantes et propositions
- **WHEN** le test refuse READ_TV_LISTINGS avec compte accessible, puis fait échouer le compte avec apps accessibles
- **THEN** les contenus de la source encore accessible sont navigables ; une proposition ou Annuler ne vole pas leur focus ; Haut puis Droite depuis le héro, ou Retour, Haut puis Droite depuis la grille l'atteignent ; Gauche, Haut ou Retour rendent l'engrenage

#### Scenario: ouverture depuis le héro
- **WHEN** le test choisit un lecteur depuis le héro puis ouvre la même série depuis À suivre et inversement
- **THEN** la préférence est commune ; le retour restaure le bon contexte et le signal de lecture vise le contenu demandé, pas un épisode suivant affiché entre-temps

## ADDED Requirements

### Requirement: Couverture du fournisseur de suivi
Les contrats de suivi SHALL être testés en JUnit avec transport, stockage, compte, lecteurs et horloges contrôlés, sans réseau, temps ou threads réels. Les tests SHALL vérifier reprises, pagination, isolation et absence de mutation sans confirmation.

#### Scenario: association et configuration
- **WHEN** le transport simule ressource web changée, attente OAuth, 502 non JSON, expiration, succès puis jeton refusé
- **THEN** l'extraction échoue proprement ou renouvelle une seule fois, le polling respecte intervalle et expiration, un succès exige un compte confirmé et une ancienne réponse ne reconnecte pas une session fermée

#### Scenario: pagination et fusion
- **WHEN** plusieurs pages contiennent doublons, reprises, épisodes vus et non vus, films remakes et dates manquantes
- **THEN** la fusion conserve identités et sources exactes, une seule carte par série identifiée et 20 cartes après tri ; le héro peut recevoir les candidats au-delà de 20 et aucun résultat partiel ne purge la mémoire

#### Scenario: mutations et annulation
- **WHEN** le test marque S02E05, annule puis retire un contenu avec annulation avant échéance, échec après échéance et réponse perdue
- **THEN** bulk reste désactivé, les précédents ne changent pas, aucune suppression n'est envoyée avant échéance et une réponse incertaine déclenche une lecture avant réessai ; aucune mutation différée ne traverse une déconnexion

#### Scenario: retour manuel
- **WHEN** le test revient d'un lecteur, ignore puis confirme « Terminé ? », et répète après changement de compte
- **THEN** seule la confirmation du compte courant marque le contenu demandé ; aucune autre action ni temps écoulé ne marque vu

#### Scenario: relectures ciblées
- **WHEN** le test revient avant TTL, effectue une mutation, revient après TTL puis émet un état de suivi
- **THEN** il compte respectivement zéro lecture de liste, une relecture ciblée, une lecture complète et zéro requête au TV Provider

### Requirement: Couverture des parcours de suivi TV
Les nouveaux parcours SHALL être testés en Compose TV avec testTags, key events physiques et données fictives : connexion, pays, sélecteur, épisodes, actions et erreur. Rendu et navigation SHALL suivre les parcours fonctionnels et la passe Penpot validée, sans capture comparative.

#### Scenario: association
- **WHEN** le test ouvre puis ferme une association, laisse expirer son code et demande un nouveau code
- **THEN** adresse/code/QR appartiennent à la même tentative, l'adresse visible conserve le chemin complet de `verification_url` et le QR encode la même URL précise plutôt que le domaine ; le QR n'est pas focusable, les actions ont un focus initial et Retour restaure Connecter sans poller une ancienne tentative

#### Scenario: pays en liste déroulante
- **WHEN** le test ouvre le pays, parcourt ses options, valide puis rouvre et annule
- **THEN** la liste reste ancrée dans les réglages sans modale, le focus et les bords suivent settings, seule la validation modifie la valeur et le libellé n'ajoute pas « · confirmé »

#### Scenario: composants du suivi
- **WHEN** le test présente synchronisation, indisponibilité, modales de contenu et retours transitoires
- **THEN** l'indicateur reste discret à droite sans suffixe « · Réglages », les modales réutilisent le composant existant dimensionné au contenu et les retours partagent le composant compact en bas à droite ; rendu glass, bordure et flou sont vérifiés sur TV, sans remplacer ces surfaces par des panneaux opaques

#### Scenario: échéance d'annulation
- **WHEN** une horloge contrôlée avance pendant une fenêtre Annuler, avec puis sans interaction
- **THEN** le retrait distant attend 10 secondes actives, le focus suspend le décompte sans le réinitialiser et la sortie reprend le temps restant ; annuler avant échéance n'envoie aucun retrait et l'expiration ne marque jamais vu

#### Scenario: sélecteur et épisodes
- **WHEN** le test parcourt les lecteurs et Autre application, puis les épisodes vus et non vus, annule et simule la disparition de l'entrée focusée
- **THEN** les bords ne bouclent pas, l'origine est restaurée, consulter ne modifie rien et le repli suivant/précédent reste utilisable ; chargement, vide, erreur et reconnexion sont distincts

#### Scenario: saisons à la demande
- **WHEN** la saison courante s'affiche puis qu'Autres saisons est ouvert et qu'une autre saison est validée
- **THEN** aucun épisode des autres saisons n'est chargé avant cette validation ; seuls les appels avec season explicite sont utilisés ; les tags tracking-seasons, tracking-season-picker, tracking-season-<number>, tracking-seasons-retry et tracking-season-back identifient le parcours sans texte ni donnée personnelle

#### Scenario: badge et masquage
- **WHEN** une carte de suivi n'a pas de lecteur choisi puis en reçoit un et est masquée/réaffichée
- **THEN** son badge passe du fournisseur à l'app ; le masquage persiste pour la série toutes sources et les candidats BetaSeries du héro, sans mutation du compte ; Réafficher restaure l'éligibilité

#### Scenario: ajout au compte depuis une carte locale
- **WHEN** le test ouvre le menu d'un contenu local identifié absent du compte connecté, puis répète sans compte, avec identité ambiguë et avec appartenance inconnue
- **THEN** Ajouter au suivi est présent seulement dans le premier cas, sans supprimer sources et retrait local ; avant ajout aucun choix de lecteur n'est persisté ; après ajout confirmé et appartenance relue le prochain lancement utilise le parcours commun, avec confirmation du premier lecteur si aucune préférence n'existe ; annulation ou échec d'ajout garde le parcours local sans appartenance fictive
