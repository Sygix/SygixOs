# Spec Delta

L'« accueil d'origine » désigne l'écran d'accueil fourni avec la TV et le composant système qui le réactive ; son ensemble fermé est défini par `design.md` (D4). La ligne des réglages est décrite par `settings`. Les numéros 8.1 à 8.21 renvoient aux écrans de la rangée « 8. Launcher principal » de la page « TV · Launcher principal » de la maquette Penpot (`design.md`, « Maquettes »), qui fait foi pour le rendu. Tous les panneaux sont des panneaux de verre centrés par-dessus un voile ; ceux du parcours ont la mascotte fixe de l'écran de démarrage à gauche de leur titre, les dialogues de confirmation et « Détails » n'en ont pas. Tous les textes entre guillemets sont les textes définitifs.

## ADDED Requirements

### Requirement: Parcours de l'écran d'accueil
SygixOs SHALL proposer le parcours guidé « Faire de SygixOs l'écran d'accueil », affiché automatiquement au premier lancement où il n'a encore jamais été fermé, y compris pour une installation existante mise à jour qui ne l'a jamais fermé (drapeau de fermeture conservé depuis v0.1.0-rc.2), après la fin de l'écran de démarrage (« Écran de démarrage ») et après la réponse à la demande de permission « Programmes TV » (`READ_TV_LISTINGS`) quand celle-ci est affichée, jamais par-dessus un dialogue système. Sa première page (8.1) SHALL afficher le titre « Faire de SygixOs l'écran d'accueil », la description « Choisissez comment SygixOs remplace l'accueil Google TV. Vous pourrez changer d'avis dans Réglages › Écran d'accueil. », puis deux lignes avec leur état à droite : « Configuration automatique (recommandée) », détail « Activez le débogage une fois : SygixOs fait les réglages lui-même. », état de la voie automatique (« Configuration automatique de l'écran d'accueil ») ; « Par autorisations (sans débogage) », détail « Quelques autorisations à donner. Limite : sur certaines TV, l'accueil Google TV peut reprendre la main après un redémarrage complet. », état de la voie par autorisations (« Page Par autorisations ») ; puis l'aide « Retour : plus tard » et le bouton « Plus tard ». Quand la voie automatique est « Indisponible sur cette TV » (8.2), sa ligne SHALL s'intituler « Configuration automatique », avoir le détail « SygixOs n'arrive pas à utiliser le débogage sur cette TV : utilisez la configuration par autorisations. », rester focalisable sans effet sur OK, et le focus initial SHALL être sur « Par autorisations (sans débogage) » ; sinon le focus initial est sur « Configuration automatique (recommandée) ». Haut et bas parcourent les deux lignes puis « Plus tard », sans boucle ; gauche et droite sont sans effet ; le focus SHALL rester dans le panneau tant qu'il est affiché. OK sur la ligne automatique SHALL ouvrir l'écran que demande l'état de l'accès (« Accès par le débogage ») : « Activer le débogage » (8.3), « Autoriser SygixOs » (8.4) ou directement « Configuration en cours » (8.5) ; OK sur la ligne par autorisations SHALL ouvrir la page « Par autorisations » (8.13). Retour sur un écran du parcours SHALL suivre l'aide affichée par cet écran (« Retour : précédent » revient à l'écran précédent avec le focus sur l'élément qui l'avait ouvert ; « Retour : plus tard », « Retour : fermer » ferment le parcours). « Plus tard », « Terminé », « Fermer », Retour quand il ferme, et la touche Home ferment le parcours ; affiché automatiquement, il SHALL ensuite ne plus jamais s'afficher automatiquement et l'accueil reprend avec le focus sur le héro ; tout reste accessible depuis Réglages › Écran d'accueil (« Panneau Launcher principal »).

#### Scenario: premier lancement
- **WHEN** SygixOs se lance et que le parcours n'a jamais été fermé, sur une TV où rien n'est encore fait
- **THEN** à la fin de l'écran de démarrage, le panneau 8.1 s'affiche sur l'accueil avec « Configuration automatique (recommandée) » à l'état « À configurer » et « Par autorisations (sans débogage) » à l'état « À faire », et « Configuration automatique (recommandée) » a le focus

#### Scenario: voie automatique indisponible
- **WHEN** la voie automatique est « Indisponible sur cette TV » à l'ouverture du parcours
- **THEN** le panneau 8.2 s'affiche, « Par autorisations (sans débogage) » a le focus, et OK sur « Configuration automatique » ne fait rien

#### Scenario: navigation
- **WHEN** l'utilisateur presse bas trois fois, haut trois fois, puis gauche et droite
- **THEN** le focus passe de la ligne automatique à la ligne par autorisations, à « Plus tard », y reste, remonte jusqu'à la ligne automatique et y reste ; gauche et droite ne le déplacent pas et aucun élément de l'accueil ne prend le focus

#### Scenario: plus tard
- **WHEN** l'utilisateur presse OK sur « Plus tard » ou Retour sur le panneau 8.1 affiché automatiquement
- **THEN** le parcours se ferme, il est enregistré comme fermé, l'accueil reprend avec le focus sur le héro, aucune connexion au débogage n'a eu lieu et aucun écran système n'est ouvert

#### Scenario: demande de permission au premier lancement
- **WHEN** la demande de permission « Programmes TV » est affichée à la fin de l'écran de démarrage
- **THEN** le parcours ne s'affiche pas tant que ce dialogue est à l'écran ; il s'affiche dès que l'utilisateur a accordé ou refusé la permission

#### Scenario: pas de nouvel affichage automatique
- **WHEN** SygixOs se lance de nouveau après la fermeture du parcours affiché automatiquement, y compris après un redémarrage de l'appareil
- **THEN** le parcours ne s'affiche pas ; Réglages › Écran d'accueil reste le point d'entrée

#### Scenario: processus arrêté pendant un écran système
- **WHEN** le système arrête le processus de SygixOs pendant qu'un écran système ouvert depuis le parcours affiché automatiquement est affiché, puis SygixOs se relance
- **THEN** le parcours, jamais fermé, s'affiche de nouveau sur le panneau 8.1 avec les états relus

### Requirement: Accès par le débogage
SygixOs SHALL pouvoir exécuter les opérations de « Opérations privilégiées limitées » en se connectant au débogage ADB de l'appareil sur lequel il tourne, sans ordinateur ni application tierce : connexion à l'adresse de bouclage sur le port ADB réseau, avec une paire de clés propre à SygixOs générée à la première connexion et conservée dans son stockage privé, que l'utilisateur autorise dans la fenêtre système « Autoriser le débogage USB ? ». SygixOs SHALL n'ouvrir cette connexion qu'après une action explicite de l'utilisateur dans le parcours, SHALL la fermer à la fin de chaque séquence, SHALL ne jamais se connecter à une autre adresse que l'appareil lui-même et SHALL ne jamais transmettre sa clé hors de l'appareil. L'état de l'accès SHALL être l'un de : débogage désactivé (réglage système lu sans connexion), connexion impossible, autorisation en attente, autorisation refusée, prêt (clé acceptée et commande de test exécutée avec l'identité du shell). La connexion, l'attente d'autorisation et chaque commande SHALL avoir un délai borné (`design.md`, D1) ; un délai dépassé SHALL ne jamais produire l'état prêt ni un succès. Quand le débogage est activé mais qu'aucun port ADB réseau n'est joignable, la voie automatique SHALL devenir « Indisponible sur cette TV » jusqu'à une tentative réussie. Aucune absence de débogage, refus ou perte de connexion SHALL ne rendre SygixOs inutilisable ni provoquer de crash.

#### Scenario: clé déjà autorisée
- **WHEN** la clé de SygixOs a déjà été autorisée avec « Toujours autoriser sur cet ordinateur » et que le débogage est activé
- **THEN** OK sur la configuration automatique ouvre directement « Configuration en cours » (8.5), sans fenêtre système

#### Scenario: port injoignable
- **WHEN** le débogage est activé mais que la connexion au port ADB réseau est refusée ou n'aboutit pas dans le délai
- **THEN** aucune opération n'est exécutée, la voie automatique devient « Indisponible sur cette TV » et le parcours revient au panneau 8.2

#### Scenario: débogage perdu pendant une séquence
- **WHEN** le débogage est désactivé ou la connexion perdue pendant une séquence
- **THEN** la séquence s'arrête, l'état du système est relu, l'écran « Configuration interrompue » (8.12) montre ce qui est fait et l'étape en échec, sans succès supposé

### Requirement: Écrans du débogage
Quand le débogage est désactivé, OK sur la configuration automatique SHALL ouvrir l'écran 8.3 : titre « Activer le débogage », description « SygixOs s'en sert pour se configurer ; vous pourrez le désactiver ensuite. », étapes numérotées non focalisables 1 « Activez les options pour les développeurs » (détail « Paramètres › Système › À propos : appuyez 7 fois sur la ligne du build (son nom varie selon la TV). ») et 2 « Activez le débogage » (détail « Options pour les développeurs › « Débogage USB », puis revenez dans SygixOs. »), une ligne « Débogage » d'état « Désactivé » ou « Activé », détail « Détecté automatiquement au retour dans SygixOs », l'aide « Retour : précédent » et le bouton « Ouvrir les options pour les développeurs », focalisé, qui ouvre l'écran système des options pour les développeurs, ou l'écran système « À propos » s'il ne peut pas s'ouvrir. Au retour dans SygixOs, l'état du débogage SHALL être relu ; activé, SygixOs SHALL afficher l'écran 8.4 : titre « Autoriser SygixOs », description « Le débogage est activé. Android va afficher la fenêtre « Autoriser le débogage USB ? » : SygixOs en a besoin pour se configurer. », étapes 1 « Cochez « Toujours autoriser sur cet ordinateur » » (détail « Sinon, Android vous le redemandera à chaque configuration. ») et 2 « Choisissez « Autoriser » » (détail « La configuration démarre aussitôt. »), la ligne « Débogage » à l'état « Activé » (détail « Détecté au retour dans SygixOs »), l'aide « Retour : précédent » et le bouton « Continuer », focalisé, qui déclenche la demande ; acceptée, la configuration démarre (8.5). Une autorisation refusée ou expirée SHALL afficher l'écran 8.11 : même titre et même description, un encadré d'erreur « Autorisation refusée », détail « Android n'a pas autorisé SygixOs et rien n'a été modifié. Réessayez, cochez « Toujours autoriser sur cet ordinateur » puis choisissez « Autoriser ». », la ligne « Débogage », et les boutons « Retour » et « Réessayer », ce dernier focalisé et relançant la demande. Un débogage désactivé entre-temps SHALL afficher l'écran 8.10 : titre et description de 8.3, encadré d'erreur « Le débogage est désactivé », détail « Rien n'a été modifié. Activez « Débogage USB » dans les options pour les développeurs, puis revenez dans SygixOs. », les deux étapes de 8.3 et les boutons « Retour » et « Ouvrir les options pour les développeurs », ce dernier focalisé. Gauche et droite passent d'un bouton à l'autre sans boucle ; haut et bas sont sans effet ; « Retour » et la touche Retour reviennent à l'écran précédent. Les chemins et libellés Android cités sont vérifiés sur la TV de référence par la tâche 1.1.

#### Scenario: activer le débogage
- **WHEN** l'utilisateur presse OK sur la configuration automatique alors que le débogage est désactivé
- **THEN** l'écran 8.3 s'affiche, la ligne « Débogage » est « Désactivé » et « Ouvrir les options pour les développeurs » a le focus

#### Scenario: retour avec le débogage activé
- **WHEN** l'utilisateur a ouvert les options pour les développeurs depuis 8.3, active le débogage puis revient dans SygixOs
- **THEN** l'écran 8.4 s'affiche avec la ligne « Débogage » « Activé » et « Continuer » focalisé ; aucune fenêtre système n'est déclenchée avant OK sur « Continuer »

#### Scenario: autorisation acceptée
- **WHEN** l'utilisateur presse « Continuer » puis accepte la fenêtre système
- **THEN** la configuration démarre et l'écran 8.5 s'affiche

#### Scenario: autorisation refusée
- **WHEN** l'utilisateur refuse ou ferme la fenêtre système, ou que le délai d'autorisation expire
- **THEN** l'écran 8.11 s'affiche, rien n'est modifié, « Réessayer » a le focus et relance la demande

#### Scenario: débogage désactivé entre-temps
- **WHEN** le débogage a été désactivé entre l'écran 8.4 et la demande
- **THEN** l'écran 8.10 s'affiche, rien n'est modifié et « Ouvrir les options pour les développeurs » a le focus

#### Scenario: écrans système absents
- **WHEN** ni l'écran des options pour les développeurs ni l'écran « À propos » ne peuvent s'ouvrir
- **THEN** le bouton ne change rien, sans crash, et les étapes restent affichées

### Requirement: Configuration automatique de l'écran d'accueil
La configuration automatique SHALL enchaîner, sans autre confirmation et selon `design.md` (D5) : l'autorisation de démarrage du constructeur (« Démarrage automatique du constructeur »), la définition de SygixOs comme écran d'accueil après avoir relevé l'écran d'accueil précédent, puis la vérification de l'écran d'accueil effectif (« État de l'écran d'accueil »). Pendant la séquence, SygixOs SHALL afficher l'écran 8.5 : titre « Configuration en cours », description « SygixOs règle la TV. Ne l'éteignez pas pendant la configuration. », une ligne par étape relue une à une — « Démarrage automatique » (détail « La TV laisse SygixOs démarrer avec elle », affichée seulement si l'appareil a cette autorisation), « Écran d'accueil » (détail « SygixOs devient l'écran d'accueil d'Android »), « Vérification » (détail « SygixOs vérifie que la TV l'ouvre comme écran d'accueil ») — avec l'état « Fait » (pastille verte), « En cours… » (pastille bleue) ou « À venir », aucun bouton et l'aide « Patientez… ». Fermer le panneau (Retour, touche Home) SHALL ne pas arrêter la séquence, qui continue jusqu'à sa fin ou son délai ; à la réouverture, l'écran correspondant à l'état relu est affiché. Si SygixOs est l'écran d'accueil effectif, SygixOs SHALL afficher l'écran 8.6 : titre « SygixOs est votre écran d'accueil », description « SygixOs a vérifié que la TV l'ouvre maintenant comme écran d'accueil. », les lignes « Démarrage automatique » (détail « La TV laisse SygixOs démarrer avec elle ») et « Écran d'accueil » (détail « La TV ouvre SygixOs comme écran d'accueil ») à l'état « Fait », la ligne « Conseil : désactivez le débogage », détail « SygixOs n'en a plus besoin. Réactivez-le pour relancer la configuration ou rétablir l'accueil Google TV. », tant que le débogage est activé (« Débogage après la configuration »), l'aide « Retour : fermer » et les boutons « Détails » (« Détails des modifications ») et « Terminé », focalisé. Sinon, si l'accueil d'origine a un composant actif et modifiable et que les garde-fous le permettent, SygixOs SHALL afficher l'écran 8.7 : titre « Encore une étape », description « Sur cette TV, l'accueil Google TV reste prioritaire : la touche Home l'ouvre encore. Désactivez-le pour que SygixOs devienne l'écran d'accueil ; vous pourrez le rétablir dans Réglages › Écran d'accueil. », la ligne « Démarrage automatique », la ligne « Touche Home » à l'état « À faire » (détail « Ouvre encore l'accueil Google TV »), l'aide « Retour : plus tard » et les boutons « Plus tard » et « Désactiver l'accueil Google TV », ce dernier focalisé et ouvrant la confirmation (« Désactivation de l'accueil d'origine »). « Plus tard » ou Retour sur 8.7, une désactivation impossible ou une désactivation échouée et annulée SHALL poser automatiquement les réglages de secours : activation du service d'accessibilité de SygixOs (autres services conservés), accord de la permission de superposition aux autres applications, activation de l'option « Démarrage à l'allumage », chacun relu ; puis SygixOs SHALL afficher l'écran 8.9 : titre « SygixOs garde la main », description « L'accueil Google TV reste actif : SygixOs a réglé la touche Home et l'ouverture au démarrage. », les lignes « Touche Home » (détail « Le service de SygixOs ramène à SygixOs »), « Superposer aux autres applications » (détail « Permet à SygixOs de s'ouvrir au démarrage de la TV »), « Ouverture à l'allumage » (détail « SygixOs demande à s'ouvrir au démarrage de la TV »), chacune avec son état relu, la ligne « Bon à savoir », détail « Sur certaines TV, après un redémarrage complet, l'accueil Google TV peut reprendre la main ; SygixOs vous le signale. », l'aide « Retour : fermer » et les boutons « Détails » et « Terminé », focalisé. Une étape en échec SHALL arrêter les étapes qui en dépendent et afficher l'écran 8.12 : titre « Configuration interrompue », description « Une étape n'a pas abouti. Ce qui est fait reste en place ; vous pouvez réessayer. », un encadré d'erreur propre à l'étape (pour l'écran d'accueil : « L'écran d'accueil n'a pas pu être réglé », détail « La touche Home ouvre encore l'accueil Google TV. Vérifiez que le débogage est toujours activé, puis réessayez. »), les lignes des étapes avec « Fait », « Échec » (rouge) ou « Annulée » (détail « Non effectuée »), l'aide « Retour : fermer » et les boutons « Fermer » et « Réessayer », focalisé. Aucune étape SHALL n'être présentée comme faite sans relecture, ni réessayée sans action. Dans les écrans à deux boutons, gauche et droite passent de l'un à l'autre sans boucle et haut et bas sont sans effet. L'état de la voie automatique, affiché en 8.1 et dans le panneau « Launcher principal », SHALL être « Active » quand SygixOs est l'écran d'accueil effectif ou que les réglages de secours sont en place, « Indisponible sur cette TV » (« Accès par le débogage »), sinon « À configurer ».

#### Scenario: configuration sans désactivation
- **WHEN** l'accès est prêt et que SygixOs devient l'écran d'accueil effectif après la définition de l'écran d'accueil
- **THEN** 8.5 montre les étapes passer à « Fait » une à une, puis 8.6 s'affiche avec « Terminé » focalisé, sans confirmation ni désactivation, et la voie automatique est « Active »

#### Scenario: désactivation proposée
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif après la vérification et que l'accueil d'origine peut être désactivé
- **THEN** l'écran 8.7 s'affiche avec « Désactiver l'accueil Google TV » focalisé

#### Scenario: plus tard sur la désactivation
- **WHEN** l'utilisateur presse « Plus tard » ou Retour sur 8.7
- **THEN** aucun composant n'est désactivé, le service d'accessibilité est ajouté sans retirer les autres, la superposition est accordée, l'option de démarrage est activée, chaque réglage est relu, et 8.9 s'affiche avec « Terminé » focalisé

#### Scenario: désactivation impossible
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif et qu'aucun composant de l'accueil d'origine n'est actif et modifiable, ou que les garde-fous l'interdisent
- **THEN** 8.7 n'est pas affiché et les réglages de secours sont posés, puis 8.9 s'affiche

#### Scenario: échec d'une étape
- **WHEN** la définition de l'écran d'accueil échoue (erreur, délai ou relecture qui ne la confirme pas)
- **THEN** 8.12 s'affiche avec « Démarrage automatique » « Fait », « Écran d'accueil » « Échec », « Vérification » « Annulée », l'encadré « L'écran d'accueil n'a pas pu être réglé » et « Réessayer » focalisé ; les étapes déjà faites restent en place

#### Scenario: panneau fermé pendant la configuration
- **WHEN** l'utilisateur presse Retour ou la touche Home pendant 8.5
- **THEN** le panneau se ferme, la séquence continue jusqu'à sa fin ou son délai, et à la réouverture l'écran correspondant à l'état relu s'affiche

### Requirement: Page Par autorisations
La page « Par autorisations » (8.13) SHALL afficher le titre « Par autorisations », la description « Pour chaque ligne, OK ouvre le réglage Android à changer. Activez SygixOs, puis revenez ici : l'état se met à jour tout seul. », puis, dans cet ordre : « Ouvrir SygixOs avec la touche Home » (« Retour Home par le service d'accessibilité »), « Ouvrir SygixOs à l'allumage » (« Démarrage à l'allumage »), « Autoriser l'ouverture au démarrage » seulement quand l'option de démarrage est activée et que la permission de superposition manque (« Permission d'affichage superposé pour le démarrage »), « Autoriser le démarrage automatique » seulement quand l'appareil déclare un écran d'auto-démarrage du constructeur retenu par `design.md` (D8), détail « OK : ouvre l'écran de démarrage automatique de la TV (présent sur certains modèles) », dont l'état reste « À faire » tant qu'aucune ouverture au démarrage n'a été observée depuis son ouverture et devient « Fait » ensuite, puis la ligne non focalisable « Bon à savoir », détail « Sur certaines TV, après un redémarrage complet, l'accueil Google TV peut reprendre la main ; SygixOs vous le signale. », l'aide « Retour : fermer » et le bouton « Terminé ». Chaque ligne SHALL afficher à droite son état relu : « À faire », « En attente… » (écran système ouvert ou vérification en cours), « Fait », « Bloqué » (touche Home seulement) ou « Indisponible » (raison à la place du détail, ligne focalisable, OK sans effet). OK sur une ligne SHALL ouvrir le réglage Android correspondant ; au retour, le panneau est toujours affiché, les états sont relus et le focus SHALL être sur la ligne qui a ouvert l'écran. Focus initial sur la première ligne qui n'est pas « Fait », sinon sur « Terminé » ; haut et bas parcourent les lignes focalisables puis « Terminé », sans boucle ; gauche et droite sont sans effet. La page ne SHALL rien activer sans action explicite. L'état de la voie par autorisations, affiché en 8.1 et dans le panneau « Launcher principal », SHALL être « Active » quand toutes les lignes affichées et disponibles sont « Fait », « Bloquée » quand la touche Home est « Bloqué », « En partie » quand au moins une est « Fait », « Non utilisée » quand aucune ne l'est et que la voie automatique est « Active », sinon « À faire ».

#### Scenario: ouverture de la page
- **WHEN** l'utilisateur presse OK sur « Par autorisations (sans débogage) » sur une TV sans écran d'auto-démarrage du constructeur, rien n'étant fait
- **THEN** la page 8.13 s'affiche avec « Ouvrir SygixOs avec la touche Home » et « Ouvrir SygixOs à l'allumage » à « À faire », sans « Autoriser le démarrage automatique », puis « Bon à savoir » et « Terminé » ; la première ligne a le focus

#### Scenario: retour d'un réglage Android
- **WHEN** l'utilisateur a ouvert un réglage Android depuis une ligne puis revient dans SygixOs
- **THEN** le panneau 8.13 est toujours affiché, l'état de la ligne est relu (plus jamais « En attente… » d'un écran fermé) et la ligne a le focus

#### Scenario: écran du constructeur
- **WHEN** l'appareil déclare un écran d'auto-démarrage du constructeur retenu
- **THEN** « Autoriser le démarrage automatique » est affichée après « Ouvrir SygixOs à l'allumage » et OK ouvre cet écran

### Requirement: Panneau Launcher principal
La ligne « Configurer SygixOs comme launcher principal » des réglages (`settings`) SHALL ouvrir, par-dessus la page de réglages, le panneau « Launcher principal » (8.16) : description « SygixOs est l'écran d'accueil de la TV. Voici l'état de chaque façon de le configurer. » quand une voie est « Active », sinon « SygixOs n'est pas encore l'écran d'accueil. Voici l'état de chaque façon de le configurer. » ; les lignes « Configuration automatique » et « Par autorisations » avec l'état de leur voie et un détail qui le résume (par exemple « Accueil Google TV désactivé · SygixOs est l'écran d'accueil », « Pas encore configurée », « Pas utilisée sur cette TV », « Aucune autorisation donnée ») ; puis les actions « Relancer la configuration », détail « Refait les réglages automatiques (débogage nécessaire) », et « Rétablir l'accueil Google TV », détail « Le réactive et lui rend l'écran d'accueil », affichée seulement quand au moins un composant de l'accueil d'origine est désactivé par l'utilisateur ; l'aide « Retour : fermer ». Focus initial sur « Relancer la configuration » ; haut et bas parcourent les deux lignes de voie puis les actions, sans boucle ; gauche et droite sont sans effet. OK sur « Configuration automatique » ou « Relancer la configuration » SHALL suivre la configuration automatique comme depuis 8.1 ; OK sur « Par autorisations » SHALL ouvrir la page 8.13 ; OK sur « Rétablir l'accueil Google TV » SHALL ouvrir sa confirmation (« Rétablissement de l'accueil d'origine »). Retour et la fermeture d'un écran ouvert depuis ce panneau SHALL rendre le focus à la ligne des réglages, sans modifier le drapeau du parcours automatique.

#### Scenario: ouverture depuis les réglages
- **WHEN** l'accueil d'origine a été désactivé par la configuration automatique et que l'utilisateur presse OK sur la ligne des réglages
- **THEN** le panneau 8.16 s'affiche avec « Configuration automatique » « Active », « Par autorisations » « Non utilisée », « Relancer la configuration » focalisé et « Rétablir l'accueil Google TV » affiché

#### Scenario: rien de configuré
- **WHEN** aucune voie n'est « Active » et que l'accueil d'origine est actif
- **THEN** le panneau 8.21 affiche « SygixOs n'est pas encore l'écran d'accueil. Voici l'état de chaque façon de le configurer. », « Configuration automatique » « À configurer » (« Pas encore configurée »), « Par autorisations » « À faire » (« Aucune autorisation donnée »), « Relancer la configuration » focalisé, sans « Rétablir l'accueil Google TV »

#### Scenario: fermeture
- **WHEN** l'utilisateur presse Retour sur le panneau
- **THEN** il se ferme et la ligne des réglages a le focus avec son état recalculé

### Requirement: État de l'écran d'accueil
SygixOs SHALL rester candidat au rôle HOME et SHALL relire, sans débogage, à chaque retour au premier plan, à l'ouverture du parcours ou du panneau et après chaque opération : l'écran d'accueil effectif (SygixOs quand l'intent `HOME` se résout vers lui et qu'aucune autre activité `HOME` activée n'a une priorité supérieure à la sienne), le rôle HOME, la présence et l'état de chaque composant de l'accueil d'origine (actif, désactivé par l'utilisateur, hors de portée), la fin de la configuration initiale de l'appareil, l'état du service d'accessibilité (activé, connecté) et de la superposition. Ces lectures SHALL déterminer les états affichés, sans valeur conservée d'avant la sortie ; une erreur de lecture SHALL être distincte de l'absence et ne SHALL jamais produire l'état « Active ». L'autorisation de démarrage du constructeur et l'inscription dans sa politique, illisibles sans débogage, SHALL être affichées telles que relues lors de la dernière séquence.

#### Scenario: configuration défaite hors de SygixOs
- **WHEN** l'utilisateur ou le système réactive l'accueil d'origine ou change l'écran d'accueil pendant que SygixOs est en arrière-plan
- **THEN** au retour au premier plan, les états sont recalculés, sans réapplication ni rappel automatique

#### Scenario: accueil désactivé par la procédure ADB
- **WHEN** SygixOs se lance alors que l'accueil d'origine a été désactivé par la procédure ADB du README et que SygixOs est l'écran d'accueil effectif
- **THEN** la voie automatique est « Active » et le panneau « Launcher principal » propose « Rétablir l'accueil Google TV »

#### Scenario: erreur de lecture
- **WHEN** la lecture de l'état d'un composant ou de l'écran d'accueil échoue
- **THEN** aucune voie n'est déclarée « Active » sur cette lecture, aucun crash n'a lieu et la lecture est refaite au prochain retour au premier plan

### Requirement: Désactivation de l'accueil d'origine
SygixOs SHALL désactiver l'accueil d'origine uniquement depuis l'écran 8.7, après le dialogue de confirmation 8.8 : titre « Désactiver l'accueil Google TV ? », texte « SygixOs deviendra l'écran d'accueil. Selon la TV, des fonctions de l'accueil Google TV, comme le choix du profil ou les réglages rapides, peuvent disparaître. Rétablissez-le dans Réglages › Écran d'accueil, et avant de désinstaller SygixOs. », boutons « Annuler », focalisé, et « Désactiver », aide « Retour : annuler » ; gauche et droite passent d'un bouton à l'autre sans boucle, haut et bas sont sans effet, Retour équivaut à « Annuler », qui revient à 8.7 avec le focus sur « Désactiver l'accueil Google TV ». SygixOs SHALL relire juste avant les garde-fous : accès prêt, configuration initiale de l'appareil terminée, activité d'accueil de SygixOs déclarée et activée, au moins un composant actif et modifiable ; un garde-fou non tenu SHALL empêcher toute désactivation. La désactivation SHALL suivre `design.md` (D6), sa progression étant affichée comme en 8.5 avec la ligne « Accueil Google TV » : composants actifs désactivés un par un dans l'ordre de l'ensemble, chacun relu, puis vérification de l'écran d'accueil effectif ; réussie, l'écran 8.6 s'affiche ; un échec, un délai ou une vérification négative SHALL remettre à l'état par défaut, dans l'ordre inverse, les composants désactivés par cette opération, puis relire et poser les réglages de secours (8.9). SygixOs SHALL ne jamais laisser l'appareil sans écran d'accueil activé.

#### Scenario: confirmation
- **WHEN** l'utilisateur presse OK sur « Désactiver l'accueil Google TV » en 8.7
- **THEN** le dialogue 8.8 s'affiche, sans mascotte, avec « Annuler » focalisé ; aucun composant n'est modifié avant OK sur « Désactiver »

#### Scenario: annulation
- **WHEN** l'utilisateur presse « Annuler » ou Retour dans 8.8
- **THEN** le dialogue se ferme, rien n'est modifié et 8.7 a le focus sur « Désactiver l'accueil Google TV »

#### Scenario: désactivation réussie
- **WHEN** l'utilisateur presse « Désactiver », que chaque composant actif est désactivé et que SygixOs est ensuite l'écran d'accueil effectif
- **THEN** l'écran 8.6 s'affiche et l'état relu de chaque composant est « désactivé par l'utilisateur »

#### Scenario: désactivation partielle annulée
- **WHEN** la désactivation d'un composant échoue ou dépasse son délai, ou que SygixOs n'est pas l'écran d'accueil effectif après la désactivation
- **THEN** les composants désactivés par cette opération sont remis à leur état par défaut dans l'ordre inverse, l'état est relu, aucun succès n'est affiché et les réglages de secours sont posés (8.9)

#### Scenario: appareil pas encore configuré
- **WHEN** la configuration initiale de l'appareil n'est pas terminée
- **THEN** 8.7 n'est pas affiché et aucune désactivation n'est exécutée

### Requirement: Rétablissement de l'accueil d'origine
OK sur « Rétablir l'accueil Google TV » (8.16), accès prêt (sinon les écrans du débogage, comme pour la configuration automatique), SHALL afficher le dialogue 8.17 : titre « Rétablir l'accueil Google TV ? », texte « L'accueil Google TV sera réactivé et redeviendra l'écran d'accueil. Les autres réglages faits par SygixOs restent en place : si son service reste activé, la touche Home ouvrira encore SygixOs. », boutons « Annuler » et « Rétablir », ce dernier focalisé, aide « Retour : annuler », mêmes règles de navigation que 8.8. « Rétablir » SHALL remettre à l'état par défaut les composants désactivés par l'utilisateur, dans l'ordre inverse de l'ensemble, puis, quand un écran d'accueil précédent a été relevé et qu'il est présent et activé, le redéfinir comme écran d'accueil ; tout est relu. Le rétablissement ne concerne que l'accueil : les autorisations accordées, le service d'accessibilité et l'inscription dans la politique d'auto-démarrage du constructeur ne sont pas modifiés. Réussi, le panneau 8.16 revient avec ses états recalculés, sans « Rétablir l'accueil Google TV », et le focus sur « Relancer la configuration ». Un échec SHALL être affiché comme en 8.12 avec l'encadré « L'accueil Google TV n'a pas pu être rétabli », sans succès supposé.

#### Scenario: rétablissement réussi
- **WHEN** l'utilisateur presse « Rétablir » et que les composants sont remis à leur état par défaut et l'écran d'accueil précédent redéfini
- **THEN** 8.16 revient sans « Rétablir l'accueil Google TV », « Relancer la configuration » a le focus et la voie automatique n'est plus « Active »

#### Scenario: rétablissement annulé
- **WHEN** l'utilisateur presse « Annuler » ou Retour dans 8.17
- **THEN** rien n'est modifié et « Rétablir l'accueil Google TV » a le focus

#### Scenario: sans écran d'accueil précédent relevé
- **WHEN** l'accueil d'origine a été désactivé par la procédure ADB du README et qu'aucun écran d'accueil précédent n'a été relevé
- **THEN** seuls les composants sont remis à leur état par défaut, et le système réattribue l'écran d'accueil

#### Scenario: échec du rétablissement
- **WHEN** la remise à l'état par défaut d'un composant échoue ou dépasse son délai
- **THEN** l'encadré « L'accueil Google TV n'a pas pu être rétabli » s'affiche avec « Réessayer » focalisé et l'état relu, sans succès supposé

### Requirement: Démarrage automatique du constructeur
Dans la configuration automatique, SygixOs SHALL accorder à son seul paquet l'autorisation de démarrage automatique propre au constructeur quand l'appareil en déclare une (opération d'application dont le nom est arrêté par la tâche 1.4, parmi `AUTO_START` et `APP_AUTO_START`), puis enregistrer les réglages d'opérations pour qu'elle persiste, et la relire. Déjà accordée, SygixOs SHALL ne rien écrire. Absente, l'étape est sans objet et sa ligne n'est pas affichée. Quand l'appareil expose la politique d'auto-démarrage du constructeur (`design.md`, D12) et que `fr.sygix.sygixos` manque dans l'une de ses listes `autoStartWhiteList` et `assocStartWhiteList`, SygixOs SHALL sauvegarder la valeur d'origine dans son stockage privé, puis écrire la même valeur avec son seul paquet ajouté à ces deux listes, sans rien changer d'autre, et la relire ; une valeur illisible ou d'une structure inattendue SHALL n'entraîner aucune écriture. Cette inscription SHALL n'être jamais retirée par SygixOs, y compris au rétablissement. Un refus, un délai ou une relecture qui ne confirme pas l'opération SHALL ne jamais produire de succès et ne SHALL entraîner aucune nouvelle tentative sans action. L'autorisation SHALL ne jamais être présentée comme une garantie d'ouverture au démarrage.

#### Scenario: autorisation accordée
- **WHEN** l'opération existe avec une valeur autre que l'autorisation et que la séquence l'accorde
- **THEN** la relecture la donne autorisée et la ligne « Démarrage automatique » passe à « Fait »

#### Scenario: déjà accordée
- **WHEN** l'opération est déjà autorisée pour SygixOs
- **THEN** aucune écriture n'a lieu et la ligne est « Fait »

#### Scenario: inscription dans la politique du constructeur
- **WHEN** l'appareil expose la politique d'auto-démarrage du constructeur et que SygixOs n'est dans aucune de ses deux listes blanches
- **THEN** la valeur d'origine est sauvegardée, `fr.sygix.sygixos` est ajouté aux deux listes et le reste de la valeur est inchangé à la relecture

#### Scenario: déjà inscrit
- **WHEN** SygixOs figure déjà dans les deux listes blanches
- **THEN** aucune sauvegarde ni écriture n'a lieu

#### Scenario: politique de structure inattendue
- **WHEN** la valeur de la politique est illisible, ou que ses listes manquent ou ne sont pas des listes
- **THEN** rien n'est écrit, l'étape « Démarrage automatique » est en échec (8.12) et les étapes indépendantes continuent

#### Scenario: politique écrasée par le constructeur
- **WHEN** une mise à jour ou une synchronisation du constructeur a retiré SygixOs des listes et que l'utilisateur relance la configuration
- **THEN** la nouvelle valeur est sauvegardée puis SygixOs y est réinscrit, avec relecture

#### Scenario: relecture négative
- **WHEN** l'écriture semble réussir mais que la relecture ne donne pas l'opération autorisée
- **THEN** l'étape est en échec et la séquence continue avec l'écran d'accueil

### Requirement: Opérations privilégiées limitées
SygixOs SHALL n'exécuter par le débogage que les opérations fixes de `design.md` (D2), chacune avec des arguments fixes visant uniquement le paquet SygixOs, son service d'accessibilité, un composant de l'ensemble fermé de l'accueil d'origine ou l'ajout de SygixOs aux deux listes blanches de la politique d'auto-démarrage du constructeur ; toute autre cible SHALL être refusée avant envoi. SygixOs SHALL ne jamais exécuter de commande saisie ou reçue de l'extérieur, ne SHALL révoquer aucune autorisation, et SHALL conserver les autres services d'accessibilité activés. Aucune connexion privilégiée SHALL ne rester ouverte hors d'une séquence demandée par l'utilisateur.

#### Scenario: cible refusée
- **WHEN** une opération est demandée avec un paquet ou un composant hors du paquet SygixOs et de l'ensemble fermé
- **THEN** elle est refusée sans être envoyée et l'échec est rapporté

#### Scenario: service d'accessibilité ajouté
- **WHEN** la configuration active le service d'accessibilité de SygixOs alors que d'autres services sont activés
- **THEN** la liste relue contient les autres services inchangés et celui de SygixOs

#### Scenario: fin de séquence
- **WHEN** une séquence se termine, réussie, en échec ou par délai
- **THEN** la connexion au débogage est fermée

### Requirement: Détails des modifications
Le bouton secondaire « Détails » (8.6, 8.9) SHALL ouvrir le panneau 8.14, sans mascotte : titre « Ce que SygixOs a modifié », description « Rien d'autre n'a été touché. « Rétablir l'accueil Google TV » ne concerne que l'accueil ; les autorisations se retirent dans les paramètres Android. », puis une ligne non focalisable par réglage, avec son état relu : « Démarrage automatique » (« La TV laisse SygixOs démarrer avec elle » ; inscription dans la politique du constructeur comprise quand elle a eu lieu), « Écran d'accueil » (« SygixOs défini comme écran d'accueil d'Android »), « Superposer aux autres applications » (« Permet à SygixOs de s'ouvrir au démarrage de la TV »), « Touche Home » (« Le service de SygixOs ne lit que la touche Home »), « Accueil Google TV » (« Toujours actif : désactivé seulement si vous le choisissez » à l'état « Inchangé », ou « Désactivé à votre demande » à l'état « Fait ») ; seules les lignes des réglages réellement modifiés par SygixOs sont affichées, l'accueil Google TV toujours. L'aide « Retour : fermer » et le bouton « Fermer », focalisé, rendent le focus à « Détails ».

#### Scenario: détails après les réglages de secours
- **WHEN** l'utilisateur presse OK sur « Détails » en 8.9
- **THEN** 8.14 liste « Démarrage automatique », « Écran d'accueil », « Superposer aux autres applications » et « Touche Home » à l'état « Fait », et « Accueil Google TV » à l'état « Inchangé » ; « Fermer » a le focus et rend le focus à « Détails »

#### Scenario: détails après désactivation
- **WHEN** l'utilisateur presse OK sur « Détails » en 8.6 après une désactivation
- **THEN** 8.14 liste « Démarrage automatique », « Écran d'accueil » et « Accueil Google TV » « Désactivé à votre demande », sans les réglages de secours

### Requirement: Débogage après la configuration
Après une configuration réussie, la ligne « Conseil : désactivez le débogage » de 8.6 SHALL être affichée tant que le débogage est activé, relu au retour au premier plan. SygixOs SHALL ne jamais désactiver le débogage lui-même ; relancer la configuration ou rétablir l'accueil Google TV exigera de le réactiver (écrans 8.3 et 8.4).

#### Scenario: conseil masqué
- **WHEN** l'utilisateur désactive le débogage puis revient dans SygixOs sur 8.6
- **THEN** la ligne « Conseil : désactivez le débogage » n'est plus affichée

## MODIFIED Requirements

### Requirement: Démarrage à l'allumage
SygixOs SHALL proposer une option de démarrage à l'allumage, la ligne « Ouvrir SygixOs à l'allumage » de la page « Par autorisations » (« Page Par autorisations »), désactivée tant que l'utilisateur ne l'a pas activée ou que la configuration automatique ne l'a pas activée comme réglage de secours (« Configuration automatique de l'écran d'accueil »). OK sur la ligne SHALL basculer l'option et la persister aussitôt, le focus restant sur elle ; la ligne affiche « Fait » quand l'option est activée et que la ligne « Autoriser l'ouverture au démarrage » n'est pas affichée, « À faire » sinon, avec le détail « SygixOs demande à s'ouvrir au démarrage de la TV ». Option activée, SygixOs SHALL demander l'ouverture de son accueil par les seuls mécanismes autorisés par Android, sans contourner ses restrictions, par deux chemins : la réception de la fin du démarrage du système, et la connexion de son service d'accessibilité peu après le démarrage (moins de 5 minutes après lui, valeur à valider sur la TV), quand le service est activé. Le premier chemin qui aboutit SHALL ouvrir SygixOs ; SygixOs SHALL s'ouvrir une seule fois par démarrage, l'autre chemin ne faisant rien si l'ouverture a déjà été observée. Une connexion du service plus tardive SHALL ne rien ouvrir. SygixOs SHALL enregistrer, pour chaque démarrage de l'appareil, si cette ouverture a été observée ; l'état « démarrage automatique non observé » qui en découle remplace le détail de la ligne par « SygixOs ne s'est pas ouvert automatiquement à ce démarrage ». Option désactivée, SygixOs SHALL ne demander aucune ouverture au démarrage. L'option ne vise que le démarrage à froid de l'appareil ; la sortie de veille n'est pas couverte par cette exigence.

#### Scenario: ouverture au démarrage observée
- **WHEN** l'appareil démarre, que l'option est activée et que la demande d'ouverture aboutit
- **THEN** l'accueil de SygixOs s'affiche au premier plan et l'ouverture est enregistrée comme observée pour ce démarrage

#### Scenario: diffusion bloquée, service connecté
- **WHEN** l'appareil démarre, que l'option est activée, que la fin du démarrage n'est pas transmise à SygixOs mais que le service d'accessibilité activé se connecte moins de 5 minutes après le démarrage
- **THEN** le service ouvre l'accueil de SygixOs et l'ouverture est enregistrée comme observée pour ce démarrage

#### Scenario: deux chemins dans le même démarrage
- **WHEN** la fin du démarrage est reçue et que le service d'accessibilité se connecte peu après, option activée
- **THEN** SygixOs s'ouvre une seule fois : le chemin qui arrive après l'ouverture observée ne demande rien, et une seconde demande arrivée avant elle n'a aucun effet visible

#### Scenario: connexion tardive du service
- **WHEN** le service d'accessibilité se connecte plus de 5 minutes après le démarrage, option activée
- **THEN** aucune ouverture au démarrage n'est demandée

#### Scenario: option désactivée
- **WHEN** l'appareil démarre et que l'option est désactivée
- **THEN** SygixOs ne demande pas son ouverture, ni par le récepteur ni par le service, et n'enregistre aucun échec

#### Scenario: fin du démarrage non reçue
- **WHEN** l'option était activée avant le démarrage, que le système ou le constructeur ne transmet pas la fin du démarrage à SygixOs et que le service d'accessibilité ne se connecte pas non plus
- **THEN** aucune ouverture n'est observée pour ce démarrage ; au lancement suivant de SygixOs, l'état « démarrage automatique non observé » est affiché par la ligne

#### Scenario: ouverture refusée par le système
- **WHEN** l'option était activée avant le démarrage, qu'un chemin d'ouverture est déclenché mais qu'Android refuse l'ouverture de l'activité
- **THEN** aucune ouverture n'est observée par ce chemin, aucun crash n'a lieu, l'autre chemin reste possible, et au lancement suivant l'état « démarrage automatique non observé » est affiché si aucune ouverture n'a été observée

#### Scenario: option activée après le démarrage
- **WHEN** l'option est activée, par l'utilisateur ou par la configuration automatique, alors que l'appareil a déjà démarré
- **THEN** le démarrage courant n'est pas compté comme un échec ; seul le prochain démarrage est évalué

#### Scenario: option activée par la configuration automatique
- **WHEN** la configuration automatique pose les réglages de secours
- **THEN** l'option est persistée activée et la ligne « Ouvrir SygixOs à l'allumage » la montre activée ; l'utilisateur peut la désactiver comme une option qu'il a activée

### Requirement: Permission d'affichage superposé pour le démarrage
SygixOs SHALL proposer la permission système de superposition aux autres applications pour permettre le démarrage hors des autres exemptions Android, par la ligne « Autoriser l'ouverture au démarrage » de la page « Par autorisations », détail « Android l'exige : choisissez SygixOs dans la liste, puis autorisez-le. », affichée sous « Ouvrir SygixOs à l'allumage » quand l'option de démarrage est activée, que la permission manque et que son écran système existe, sans panneau de confirmation intermédiaire ; la configuration automatique SHALL aussi l'accorder par le débogage comme réglage de secours (« Configuration automatique de l'écran d'accueil »). La ligne SHALL afficher « À faire » tant que la permission manque et « En attente… » pendant l'écran système ; elle disparaît quand le système déclare la permission accordée. OK SHALL ouvrir l'écran système de la permission pour SygixOs ; hors de la configuration automatique, SygixOs SHALL ne jamais accorder la permission lui-même ; il SHALL ne jamais la supposer accordée, et au retour au premier plan son état SHALL être relu auprès du système. Un refus de l'utilisateur SHALL ne pas bloquer l'utilisation de SygixOs ni effacer son choix de démarrage. Quand l'écran système de la permission n'existe pas sur l'appareil, ou ne peut pas s'ouvrir, la ligne « Ouvrir SygixOs à l'allumage » SHALL devenir « Indisponible », avec la raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage » à la place du détail, la ligne de la permission SHALL disparaître et l'option SHALL ne pas s'activer : le passage de désactivé à activé est refusé, et une option activée juste avant l'échec revient à désactivé ; SygixOs reste utilisable sans crash. Cet état dure jusqu'au prochain lancement du processus, sauf si le système déclare ensuite la permission accordée ; une option déjà activée auparavant reste désactivable. Une permission accordée SHALL ne pas être présentée comme une garantie d'ouverture au démarrage sur tous les appareils. Le libellé exact de l'écran système est relevé par la tâche 1.5.

#### Scenario: sous-étape affichée à l'activation
- **WHEN** l'utilisateur active l'option alors que la permission manque et que son écran existe
- **THEN** l'option est persistée activée, la ligne « Autoriser l'ouverture au démarrage » apparaît avec « À faire », aucun écran système ne s'ouvre avant OK sur elle et le focus reste sur « Ouvrir SygixOs à l'allumage »

#### Scenario: ouverture de l'écran de la permission
- **WHEN** l'utilisateur presse OK sur « Autoriser l'ouverture au démarrage »
- **THEN** l'écran système de la permission s'ouvre et la ligne passe à « En attente… » ; au retour, elle disparaît seulement si le système déclare la permission accordée, et le focus va à « Ouvrir SygixOs à l'allumage » dans ce cas, sinon reste sur elle

#### Scenario: permission refusée
- **WHEN** l'utilisateur revient de l'écran système sans accorder la permission
- **THEN** SygixOs reste utilisable sans crash, ne suppose aucune permission accordée, conserve l'option activée et la ligne affiche « À faire »

#### Scenario: permission déjà accordée
- **WHEN** l'option est activée alors que la permission est déjà accordée
- **THEN** la ligne de la permission n'est pas affichée et « Ouvrir SygixOs à l'allumage » est « Fait »

#### Scenario: écran de la permission absent
- **WHEN** l'appareil n'a pas d'écran système pour la permission et que l'option de démarrage est désactivée
- **THEN** « Ouvrir SygixOs à l'allumage » est affichée « Indisponible », avec la raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage » ; OK ne l'active pas, aucune ligne de permission ni écran système n'apparaît et la ligne garde le focus

#### Scenario: écran de la permission impossible à ouvrir
- **WHEN** l'utilisateur valide « Autoriser l'ouverture au démarrage » et que l'écran système ne peut pas s'ouvrir
- **THEN** la ligne disparaît, l'option revient à désactivé et « Ouvrir SygixOs à l'allumage » devient « Indisponible » avec sa raison, focalisée ; une nouvelle activation est refusée jusqu'au prochain lancement, sauf si la permission est ensuite déclarée accordée

#### Scenario: permission accordée par la configuration automatique
- **WHEN** la configuration automatique accorde la permission comme réglage de secours
- **THEN** la permission est relue auprès du système, et la ligne « Superposer aux autres applications » de 8.9 n'est « Fait » que si le système la déclare accordée

### Requirement: Retour Home par le service d'accessibilité
SygixOs SHALL fournir un service d'accessibilité qui, une fois activé dans les réglages d'accessibilité du système, ramène SygixOs au premier plan sur son accueil à chaque appui sur la touche Home. Le service SHALL être activé soit par l'utilisateur, la ligne « Ouvrir SygixOs avec la touche Home » de la page « Par autorisations » ouvrant seulement les réglages d'accessibilité du système, soit par la configuration automatique comme réglage de secours, par le débogage et en conservant les autres services activés (« Configuration automatique de l'écran d'accueil ») ; hors de ces cas, SygixOs SHALL ne jamais activer ni désactiver ce service lui-même. Après un refus, le service SHALL n'être proposé de nouveau que depuis Réglages › Écran d'accueil, sans relance automatique. Le service SHALL observer uniquement la touche Home via `AccessibilityService.onKeyEvent`, sans lecture du contenu des fenêtres, et ne rien conserver. Tant que la redirection Home est active, c'est-à-dire tant que le service est connecté, il SHALL consommer tous les événements de la touche Home (appui, répétitions et relâchement), pour que le système et le launcher du constructeur ne la reçoivent jamais, et SHALL ouvrir SygixOs aussitôt au relâchement non annulé, sans délai ; un appui long SHALL être traité comme un appui court. Service non activé, désactivé ou arrêté, aucune touche n'est consommée et SygixOs SHALL rester pleinement utilisable. Quand le service est activé dans les réglages du système mais n'est pas connecté, au lancement du processus SygixOs ou au retour au premier plan, SygixOs SHALL attendre sa connexion pendant un délai borné, sans jamais tenter de le faire relier lui-même. La ligne SHALL afficher « Fait » quand le service est activé et connecté, « À faire » quand il n'est pas activé, avec le détail « OK : ouvre le réglage Android correspondant », « En attente… » pendant les réglages d'accessibilité ouverts et tant que cette attente n'a pas conclu, « Bloqué » quand il est activé mais toujours non connecté au terme de l'attente, jusqu'à sa connexion ou sa désactivation, avec le détail « La TV a bloqué ce service au démarrage. Désactivez-le puis réactivez-le dans Accessibilité, ou utilisez la configuration automatique. », et « Indisponible » quand les réglages d'accessibilité ne peuvent pas s'ouvrir, avec la raison « Les réglages d'accessibilité ne peuvent pas s'ouvrir sur cette TV ». Le service SHALL être livré même si un appareil ne lui transmet pas la touche Home, et ne rien faire dans ce cas.

#### Scenario: activation par l'utilisateur
- **WHEN** l'utilisateur valide « Ouvrir SygixOs avec la touche Home »
- **THEN** les réglages d'accessibilité du système s'ouvrent ; au retour au premier plan, la ligne affiche « Fait » seulement si le système déclare le service activé et qu'il est connecté

#### Scenario: activation par la configuration automatique
- **WHEN** la configuration automatique active le service comme réglage de secours
- **THEN** le service est ajouté à la liste des services activés sans retirer les autres, l'état est relu, et la ligne « Touche Home » de 8.9 n'est « Fait » que si le service est connecté

#### Scenario: service refusé
- **WHEN** l'utilisateur revient des réglages d'accessibilité sans avoir activé le service, ou ferme le parcours sans l'ouvrir
- **THEN** la ligne affiche « À faire », SygixOs reste utilisable et aucune nouvelle proposition ne s'affiche automatiquement, à ce lancement comme aux suivants

#### Scenario: touche Home, SygixOs en arrière-plan
- **WHEN** le service est connecté, que SygixOs est en arrière-plan et que l'utilisateur presse Home
- **THEN** le service consomme l'appui et le relâchement, l'accueil du constructeur ne s'affiche à aucun moment, et SygixOs revient au premier plan sur son accueil dès le relâchement, selon les règles de retour existantes (« Écran de démarrage », scénario « retour sur le launcher »)

#### Scenario: touche Home, SygixOs au premier plan
- **WHEN** le service est connecté, que SygixOs est au premier plan et que l'utilisateur presse Home
- **THEN** la touche est consommée et SygixOs applique « Touche Home avec SygixOs au premier plan »

#### Scenario: appui long sur Home
- **WHEN** le service est connecté et que l'utilisateur maintient la touche Home (répétitions ou maintien au-delà du délai d'appui long)
- **THEN** tous les événements sont consommés, aucun comportement système d'appui long ne s'applique, et SygixOs s'ouvre au relâchement comme pour un appui court

#### Scenario: relâchement annulé
- **WHEN** le système annule l'appui sur Home en cours (relâchement marqué annulé)
- **THEN** l'événement est consommé et SygixOs n'est pas ouvert

#### Scenario: signal Home absent
- **WHEN** l'appareil ne transmet aucune touche Home au service, même connecté
- **THEN** le service reste livré, ne lit aucun contenu de fenêtre, ne demande aucune ouverture et laisse le comportement système inchangé

#### Scenario: service désactivé ou arrêté
- **WHEN** l'utilisateur désactive le service dans les réglages système, ou que le système l'arrête
- **THEN** aucune touche n'est consommée, l'appui sur Home suit le comportement du système, aucune erreur n'est affichée, et au retour au premier plan la ligne affiche « À faire » (désactivé) ou suit le scénario « service bloqué » (activé mais arrêté)

#### Scenario: service non lié au lancement
- **WHEN** après un redémarrage complet, le gestionnaire d'auto-démarrage du constructeur a empêché la liaison du service activé, et que l'utilisateur lance SygixOs
- **THEN** la ligne affiche « En attente… » pendant l'attente bornée, puis « Bloqué » si le service ne s'est pas connecté ; SygixOs ne demande aucune nouvelle liaison et ne modifie ni ses composants, ni le service, ni les réglages du système

#### Scenario: service bloqué
- **WHEN** le service est activé dans les réglages du système mais n'est toujours pas connecté au terme de l'attente bornée
- **THEN** la ligne affiche « Bloqué » avec son explication, reste focalisable, et OK ouvre les réglages d'accessibilité du système ; la voie par autorisations est « Bloquée » ; quand le système relie de nouveau le service, la ligne affiche « Fait » au retour au premier plan

## REMOVED Requirements

### Requirement: Rôle d'écran d'accueil
**Reason**: le rôle HOME est attribué par la configuration automatique (« Configuration automatique de l'écran d'accueil ») ; l'étape facultative « Définir SygixOs par défaut », sans effet sur la touche Home de Google TV, n'apparaît plus dans le parcours (maquette 8.13).
**Migration**: le rôle relu reste une donnée de « État de l'écran d'accueil » ; un rôle déjà attribué par P5 est conservé.

### Requirement: Parcours guidé du launcher
**Reason**: remplacé par « Parcours de l'écran d'accueil », « Page Par autorisations » et « Panneau Launcher principal » (maquette 8.1 à 8.21).
**Migration**: le drapeau de fermeture du parcours de P5 est conservé ; les étapes P5 deviennent les lignes de la page « Par autorisations ».
