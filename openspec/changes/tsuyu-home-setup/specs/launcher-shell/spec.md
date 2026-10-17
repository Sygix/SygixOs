# Spec Delta

Ce delta modifie des exigences ajoutées par `p7-default-launcher`, qui doit être archivé d'abord (`design.md`, « Ordre d'archivage »). Les numéros 8.x renvoient à la rangée « 8. Launcher principal », les numéros 9.x à la rangée « 9. Parcours simplifié et Tsuyu » de la page « TV · Launcher principal » de la maquette Penpot, qui fait foi pour le rendu. Les écrans propres à Tsuyu (9.4 à 9.9), la disponibilité de Tsuyu, son état et la réparation automatique sont décrits par `tsuyu-access`. Tous les textes entre guillemets sont les textes définitifs.

## MODIFIED Requirements

### Requirement: Parcours de l'écran d'accueil
SygixOs SHALL proposer le parcours guidé « Faire de SygixOs l'écran d'accueil », affiché automatiquement au premier lancement où il n'a encore jamais été fermé, y compris pour une installation existante mise à jour qui ne l'a jamais fermé (drapeau de fermeture conservé depuis v0.1.0-rc.2), après la fin de l'écran de démarrage (« Écran de démarrage ») et après la réponse à la demande de permission « Programmes TV » (`READ_TV_LISTINGS`) quand celle-ci est affichée, jamais par-dessus un dialogue système. Sa première page SHALL afficher le titre « Faire de SygixOs l'écran d'accueil », la description « Choisissez comment SygixOs remplace l'accueil Google TV. Vous pourrez changer d'avis dans Réglages › Écran d'accueil. », puis des lignes de choix avec leur état à droite, l'aide « Retour : plus tard » et le bouton « Plus tard ». Quand Tsuyu est disponible (« Disponibilité de Tsuyu », `tsuyu-access`) et que la voie automatique n'est pas « Indisponible sur cette TV », la page (9.1) SHALL présenter trois lignes : « Automatique avec Tsuyu (recommandée) », détail « Un guide pas à pas installe Tsuyu : SygixOs se règle seul et se répare si la TV défait ses réglages. » ; « Automatique sans Tsuyu », détail « Activez le débogage une fois : SygixOs fait les réglages, sans réparation ensuite. » ; « Par autorisations (sans débogage) », détail « Quelques autorisations à donner. Limite : sur certaines TV, l'accueil Google TV peut reprendre la main après un redémarrage complet. ». Sinon (8.1), elle SHALL présenter deux lignes : « Configuration automatique (recommandée) », détail « Activez le débogage une fois : SygixOs fait les réglages lui-même. », et la ligne par autorisations. Chaque ligne automatique affiche l'état de la voie automatique (« Configuration automatique de l'écran d'accueil »), sauf que « Active » ne s'affiche que sur la ligne du moyen d'accès par lequel la voie est active (avec Tsuyu quand Tsuyu est « Actif », sinon sans Tsuyu), l'autre ligne affichant alors « Non utilisée » ; la ligne par autorisations affiche l'état de sa voie (« Page Par autorisations »). Quand la voie automatique est « Indisponible sur cette TV » (8.2), la page SHALL être celle de 8.2 quelle que soit la disponibilité de Tsuyu : ligne « Configuration automatique », détail « SygixOs n'arrive pas à utiliser le débogage sur cette TV : utilisez la configuration par autorisations. », focalisable sans effet sur OK. Le focus initial SHALL être sur la première ligne qui n'est pas « Indisponible sur cette TV ». Haut et bas parcourent les lignes puis « Plus tard », sans boucle ; gauche et droite sont sans effet ; le focus SHALL rester dans le panneau tant qu'il est affiché. OK sur « Automatique avec Tsuyu (recommandée) » SHALL ouvrir le premier écran que demandent l'accès et l'état de Tsuyu, dans l'ordre : « Activer le débogage » (9.2 ou 9.3), « Installer Tsuyu » (9.4), « Démarrer Tsuyu » (9.7), « Autoriser SygixOs » (9.8), « Configuration en cours » (9.10). OK sur « Automatique sans Tsuyu » ou « Configuration automatique (recommandée) » SHALL ouvrir, dans l'ordre, « Activer le débogage » (9.2 ou 9.3), « Autoriser SygixOs » (8.4) ou « Configuration en cours » (9.10), les étapes déjà faites étant sautées. OK sur la ligne par autorisations SHALL ouvrir la page « Par autorisations » (8.13). Les écrans d'étape de la configuration automatique SHALL afficher au-dessus du titre le repère « Étape X sur N » : N compte les étapes de la façon choisie qui restaient à faire quand elle a été ouverte, parmi « Débogage », « Installer Tsuyu », « Démarrer Tsuyu », « Autoriser SygixOs » et « Réglages » (les deux étapes de Tsuyu seulement avec Tsuyu, « Réglages » toujours), et X est le rang de l'étape affichée ; un écran d'erreur garde le repère de son étape ; N est recalculé seulement quand l'utilisateur choisit « Sans Tsuyu » en 9.4 ou 9.6. Le repère n'est affiché ni sur la première page, ni sur la page « Par autorisations », ni sur « Détails », les dialogues et le panneau « Launcher principal ». Sur chaque écran du parcours, l'élément focalisé au départ SHALL être l'étape conseillée suivante, de sorte que des appuis successifs sur OK dans SygixOs, en suivant les écrans d'Android et de Tsuyu, mènent à une configuration terminée qui ne désactive pas l'accueil d'origine ; Retour SHALL ne jamais modifier la TV. Retour sur un écran du parcours SHALL suivre l'aide affichée par cet écran (« Retour : précédent » revient à l'écran précédent avec le focus sur l'élément qui l'avait ouvert ; « Retour : plus tard », « Retour : fermer » ferment le parcours). « Plus tard », « Terminé », « Fermer », Retour quand il ferme, et la touche Home ferment le parcours ; affiché automatiquement, il SHALL ensuite ne plus jamais s'afficher automatiquement et l'accueil reprend avec le focus sur le héro ; tout reste accessible depuis Réglages › Écran d'accueil (« Panneau Launcher principal »).

#### Scenario: premier lancement
- **WHEN** SygixOs se lance, que le parcours n'a jamais été fermé, que Tsuyu n'est pas disponible et que rien n'est encore fait
- **THEN** à la fin de l'écran de démarrage, le panneau 8.1 s'affiche sur l'accueil avec « Configuration automatique (recommandée) » à l'état « À configurer » et « Par autorisations (sans débogage) » à l'état « À faire », et « Configuration automatique (recommandée) » a le focus

#### Scenario: premier lancement avec Tsuyu disponible
- **WHEN** SygixOs se lance, que le parcours n'a jamais été fermé, que Tsuyu est disponible et que rien n'est encore fait
- **THEN** le panneau 9.1 s'affiche avec, de haut en bas, « Automatique avec Tsuyu (recommandée) » et « Automatique sans Tsuyu » à « À configurer », « Par autorisations (sans débogage) » à « À faire », puis « Plus tard » ; « Automatique avec Tsuyu (recommandée) » a le focus

#### Scenario: voie automatique indisponible
- **WHEN** la voie automatique est « Indisponible sur cette TV » à l'ouverture du parcours, que Tsuyu soit disponible ou non
- **THEN** le panneau 8.2 s'affiche, sans ligne Tsuyu, « Par autorisations (sans débogage) » a le focus, et OK sur « Configuration automatique » ne fait rien

#### Scenario: navigation
- **WHEN** l'utilisateur presse bas quatre fois, haut quatre fois, puis gauche et droite sur 9.1
- **THEN** le focus passe d'une ligne à la suivante jusqu'à « Plus tard », y reste, remonte jusqu'à la première ligne et y reste ; gauche et droite ne le déplacent pas et aucun élément de l'accueil ne prend le focus

#### Scenario: plus tard
- **WHEN** l'utilisateur presse OK sur « Plus tard » ou Retour sur la première page affichée automatiquement
- **THEN** le parcours se ferme, il est enregistré comme fermé, l'accueil reprend avec le focus sur le héro, aucune connexion privilégiée n'a eu lieu, rien n'est téléchargé et aucun écran système n'est ouvert

#### Scenario: repère d'avancement
- **WHEN** l'utilisateur choisit « Automatique avec Tsuyu (recommandée) » alors que le débogage est désactivé et que Tsuyu n'est pas installé
- **THEN** l'écran du débogage affiche « Étape 1 sur 5 », puis « Installer Tsuyu » « Étape 2 sur 5 », « Démarrer Tsuyu » « Étape 3 sur 5 », « Autoriser SygixOs » « Étape 4 sur 5 » et « Configuration en cours » « Étape 5 sur 5 »

#### Scenario: étapes déjà faites
- **WHEN** l'utilisateur choisit « Automatique sans Tsuyu » alors que le débogage est déjà activé
- **THEN** l'écran « Autoriser SygixOs » s'affiche directement avec « Étape 1 sur 2 »

#### Scenario: passage sans Tsuyu
- **WHEN** l'utilisateur presse « Sans Tsuyu » sur « Installer Tsuyu » affiché « Étape 2 sur 5 »
- **THEN** l'écran « Autoriser SygixOs » (8.4) s'affiche avec « Étape 2 sur 3 »

#### Scenario: tout par OK
- **WHEN** sur la TV de référence, rien n'étant fait et Tsuyu disponible, l'utilisateur presse uniquement OK sur chaque écran de SygixOs et suit les écrans d'Android et de Tsuyu qu'il annonce
- **THEN** la configuration se termine par « SygixOs garde la main » avec Tsuyu, l'accueil Google TV n'est pas désactivé et aucune confirmation de désactivation ne s'est affichée

#### Scenario: demande de permission au premier lancement
- **WHEN** la demande de permission « Programmes TV » est affichée à la fin de l'écran de démarrage
- **THEN** le parcours ne s'affiche pas tant que ce dialogue est à l'écran ; il s'affiche dès que l'utilisateur a accordé ou refusé la permission

#### Scenario: pas de nouvel affichage automatique
- **WHEN** SygixOs se lance de nouveau après la fermeture du parcours affiché automatiquement, y compris après un redémarrage de l'appareil
- **THEN** le parcours ne s'affiche pas ; Réglages › Écran d'accueil reste le point d'entrée

#### Scenario: processus arrêté pendant un écran système
- **WHEN** le système arrête le processus de SygixOs pendant qu'un écran système ou Tsuyu, ouvert depuis le parcours affiché automatiquement, est affiché, puis SygixOs se relance
- **THEN** le parcours, jamais fermé, s'affiche de nouveau sur sa première page avec les états relus

### Requirement: Accès par le débogage
SygixOs SHALL exécuter les opérations de « Opérations privilégiées limitées » par Tsuyu quand Tsuyu est « Actif » (« Accès privilégié par Tsuyu », `tsuyu-access`), et sinon en se connectant au débogage ADB de l'appareil sur lequel il tourne, sans ordinateur ni application tierce : connexion à l'adresse de bouclage sur le port ADB réseau, avec une paire de clés propre à SygixOs générée à la première connexion et conservée dans son stockage privé, que l'utilisateur autorise dans la fenêtre système « Autoriser le débogage USB ? ». SygixOs SHALL n'ouvrir cette connexion qu'après une action explicite de l'utilisateur dans le parcours, SHALL la fermer à la fin de chaque séquence, SHALL ne jamais se connecter à une autre adresse que l'appareil lui-même et SHALL ne jamais transmettre sa clé hors de l'appareil. L'état de l'accès SHALL être l'un de : débogage désactivé (réglage système lu sans connexion), connexion impossible, autorisation en attente, autorisation refusée, prêt (clé acceptée et commande de test exécutée avec l'identité du shell, ou Tsuyu « Actif »). La connexion, l'attente d'autorisation et chaque commande SHALL avoir un délai borné (`design.md` de `p7-default-launcher`, D1) ; un délai dépassé SHALL ne jamais produire l'état prêt ni un succès. Sur le parcours avec Tsuyu, dès que le débogage est activé et avant tout téléchargement, SygixOs SHALL vérifier que le port ADB réseau est joignable par une simple ouverture de connexion à l'adresse de bouclage, sans échange d'authentification ni fenêtre système, avec le délai de connexion. Quand le débogage est activé mais qu'aucun port ADB réseau n'est joignable, la voie automatique SHALL devenir « Indisponible sur cette TV » jusqu'à une tentative réussie. Aucune absence de débogage, refus ou perte de connexion SHALL ne rendre SygixOs inutilisable ni provoquer de crash.

#### Scenario: clé déjà autorisée
- **WHEN** la clé de SygixOs a déjà été autorisée avec « Toujours autoriser sur cet ordinateur », que le débogage est activé et que Tsuyu n'est pas « Actif »
- **THEN** OK sur la configuration automatique sans Tsuyu ouvre directement « Configuration en cours », sans fenêtre système

#### Scenario: Tsuyu actif
- **WHEN** Tsuyu est « Actif » et qu'une séquence démarre, depuis le parcours ou le panneau
- **THEN** aucune connexion ADB de SygixOs n'est ouverte et les opérations passent par Tsuyu

#### Scenario: port testé avant Tsuyu
- **WHEN** l'utilisateur revient du guide du débogage avec le débogage activé sur le parcours avec Tsuyu
- **THEN** SygixOs teste le port ADB réseau sans fenêtre système ; joignable, le parcours passe à l'étape Tsuyu suivante ; injoignable, rien n'est téléchargé

#### Scenario: port injoignable
- **WHEN** le débogage est activé mais que la connexion au port ADB réseau est refusée ou n'aboutit pas dans le délai
- **THEN** aucune opération n'est exécutée, la voie automatique devient « Indisponible sur cette TV » et le parcours revient au panneau 8.2

#### Scenario: débogage perdu pendant une séquence
- **WHEN** le débogage est désactivé ou la connexion perdue pendant une séquence
- **THEN** la séquence s'arrête, l'état du système est relu, l'écran « Configuration interrompue » (8.12) montre ce qui est fait et l'étape en échec, sans succès supposé

### Requirement: Écrans du débogage
Quand le débogage est désactivé, la configuration automatique SHALL commencer par l'écran « Activer le débogage » : titre « Activer le débogage », description « Une option de la TV que SygixOs utilise pour se régler lui-même. Deux réglages à changer, puis revenez ici. », étapes numérotées non focalisables avec leur état à droite (« À faire » ou « Fait », pastille verte), relu à l'ouverture et à chaque retour dans SygixOs : 1 « Affichez les options pour les développeurs » (détail « Dans À propos, appuyez 7 fois sur la ligne du build (son nom varie selon la TV), puis revenez avec Retour. »), « Fait » quand le système déclare les options pour les développeurs activées ; 2 « Activez le débogage USB » (détail « Dans Options pour les développeurs, activez « Débogage USB », puis revenez avec Retour. »), « Fait » quand le débogage est activé ; l'aide « Retour : précédent » et un seul bouton, focalisé : « Ouvrir À propos » tant que l'étape 1 est « À faire » (9.2), qui ouvre l'écran système « À propos », ou les Paramètres s'il ne peut pas s'ouvrir ; « Ouvrir les options pour les développeurs » quand l'étape 1 est « Fait » (9.3), qui ouvre l'écran système des options pour les développeurs, ou « À propos » s'il ne peut pas s'ouvrir. Au retour dans SygixOs avec le débogage activé, le parcours SHALL passer à l'étape suivante de la façon choisie, sans action supplémentaire (« Parcours de l'écran d'accueil »). Sans Tsuyu, l'étape suivante est l'écran 8.4 : titre « Autoriser SygixOs », description « Le débogage est activé. Android va afficher la fenêtre « Autoriser le débogage USB ? » : SygixOs en a besoin pour se configurer. », étapes 1 « Cochez « Toujours autoriser sur cet ordinateur » » (détail « Sinon, Android vous le redemandera à chaque configuration. ») et 2 « Choisissez « Autoriser » » (détail « La configuration démarre aussitôt. »), la ligne « Débogage » à l'état « Activé » (détail « Détecté au retour dans SygixOs »), l'aide « Retour : précédent » et le bouton « Continuer », focalisé, qui déclenche la demande ; acceptée, la configuration démarre. Une autorisation refusée ou expirée SHALL afficher l'écran 8.11 : même titre et même description, un encadré d'erreur « Autorisation refusée », détail « Android n'a pas autorisé SygixOs et rien n'a été modifié. Réessayez, cochez « Toujours autoriser sur cet ordinateur » puis choisissez « Autoriser ». », la ligne « Débogage », et les boutons « Retour » et « Réessayer », ce dernier focalisé et relançant la demande. Un débogage désactivé entre-temps SHALL afficher l'écran 8.10 : titre et description de « Activer le débogage », encadré d'erreur « Le débogage est désactivé », détail « Rien n'a été modifié. Activez « Débogage USB » dans les options pour les développeurs, puis revenez dans SygixOs. », les deux étapes avec leur état et les boutons « Retour » et « Ouvrir les options pour les développeurs », ce dernier focalisé. Gauche et droite passent d'un bouton à l'autre sans boucle ; haut et bas sont sans effet ; « Retour » et la touche Retour reviennent à l'écran précédent. Les chemins et libellés Android cités sont vérifiés sur la TV de référence (`tasks.md`, 1.1).

#### Scenario: activer le débogage
- **WHEN** l'utilisateur ouvre la configuration automatique alors que les options pour les développeurs sont masquées et le débogage désactivé
- **THEN** l'écran 9.2 s'affiche avec les deux étapes à « À faire » et « Ouvrir À propos » focalisé ; OK ouvre l'écran système « À propos »

#### Scenario: options affichées au retour
- **WHEN** l'utilisateur a affiché les options pour les développeurs depuis « À propos » puis revient dans SygixOs sans avoir activé le débogage
- **THEN** l'écran 9.3 s'affiche avec l'étape 1 à « Fait », l'étape 2 à « À faire » et « Ouvrir les options pour les développeurs » focalisé

#### Scenario: retour avec le débogage activé
- **WHEN** l'utilisateur active le débogage puis revient dans SygixOs, sans Tsuyu
- **THEN** l'écran 8.4 s'affiche avec la ligne « Débogage » « Activé » et « Continuer » focalisé ; aucune fenêtre système n'est déclenchée avant OK sur « Continuer »

#### Scenario: autorisation acceptée
- **WHEN** l'utilisateur presse « Continuer » puis accepte la fenêtre système
- **THEN** la configuration démarre et l'écran « Configuration en cours » s'affiche

#### Scenario: autorisation refusée
- **WHEN** l'utilisateur refuse ou ferme la fenêtre système, ou que le délai d'autorisation expire
- **THEN** l'écran 8.11 s'affiche, rien n'est modifié, « Réessayer » a le focus et relance la demande

#### Scenario: débogage désactivé entre-temps
- **WHEN** le débogage a été désactivé entre l'écran 8.4 et la demande
- **THEN** l'écran 8.10 s'affiche, rien n'est modifié et « Ouvrir les options pour les développeurs » a le focus

#### Scenario: écrans système absents
- **WHEN** ni l'écran demandé ni son écran de repli ne peuvent s'ouvrir
- **THEN** le bouton ne change rien, sans crash, et les étapes restent affichées

### Requirement: Configuration automatique de l'écran d'accueil
La configuration automatique SHALL enchaîner, sans autre confirmation et selon `design.md` de `p7-default-launcher` (D5), par le moyen d'accès de « Accès par le débogage » : l'autorisation de démarrage du constructeur (« Démarrage automatique du constructeur »), la définition de SygixOs comme écran d'accueil après avoir relevé l'écran d'accueil précédent, puis la vérification de l'écran d'accueil effectif (« État de l'écran d'accueil »). Pendant la séquence, SygixOs SHALL afficher l'écran « Configuration en cours » (9.10, rendu de 8.5 avec le repère d'avancement) : titre « Configuration en cours », description « SygixOs règle la TV. Ne l'éteignez pas pendant la configuration. », une ligne par étape relue une à une — « Démarrage automatique » (détail « La TV laisse SygixOs démarrer avec elle », affichée seulement si l'appareil a cette autorisation), « Écran d'accueil » (détail « SygixOs devient l'écran d'accueil d'Android »), « Vérification » (détail « SygixOs vérifie que la TV l'ouvre comme écran d'accueil ») — avec l'état « Fait » (pastille verte), « En cours… » (pastille bleue) ou « À venir », aucun bouton et l'aide « Patientez… ». Fermer le panneau (Retour, touche Home) SHALL ne pas arrêter la séquence, qui continue jusqu'à sa fin ou son délai ; à la réouverture, l'écran correspondant à l'état relu est affiché. Si SygixOs est l'écran d'accueil effectif, SygixOs SHALL afficher l'écran « SygixOs est votre écran d'accueil » : titre « SygixOs est votre écran d'accueil », description « SygixOs a vérifié que la TV l'ouvre maintenant comme écran d'accueil. », les lignes « Démarrage automatique » (détail « La TV laisse SygixOs démarrer avec elle ») et « Écran d'accueil » (détail « La TV ouvre SygixOs comme écran d'accueil ») à l'état « Fait », puis, sans Tsuyu (8.6), la ligne « Conseil : désactivez le débogage », détail « SygixOs n'en a plus besoin. Réactivez-le pour relancer la configuration ou rétablir l'accueil Google TV. », tant que le débogage est activé (« Débogage après la configuration »), ou, avec Tsuyu « Actif » (9.13), la ligne « Réparation automatique » à l'état « Active », détail « Tsuyu permet à SygixOs de refaire ses réglages si la TV les défait. Gardez le débogage activé. » ; l'aide « Retour : fermer » et les boutons « Détails » (« Détails des modifications ») et « Terminé », focalisé. Sinon, si l'accueil d'origine a un composant actif et modifiable et que les garde-fous le permettent, SygixOs SHALL afficher l'écran « Un dernier choix » (9.11) : titre « Un dernier choix », description « Sur cette TV, l'accueil Google TV reste l'écran d'accueil du système. SygixOs peut le garder ou le remplacer complètement. Vous pourrez changer d'avis dans Réglages › Écran d'accueil. », puis deux lignes de choix, sans état : « Garder l'accueil Google TV (conseillé) », détail « La touche Home et l'allumage ouvrent SygixOs. Toutes les touches de la télécommande continuent de marcher. », focalisée, et « Désactiver l'accueil Google TV », détail « SygixOs le remplace complètement. Selon la TV, des touches de la télécommande ne marchent plus. », qui ouvre la confirmation (« Désactivation de l'accueil d'origine ») ; l'aide « Retour : garder ». Haut et bas passent d'une ligne à l'autre sans boucle ; gauche et droite sont sans effet. OK sur « Garder l'accueil Google TV (conseillé) » ou Retour sur 9.11, une désactivation impossible ou une désactivation échouée et annulée SHALL poser automatiquement les réglages de secours : activation du service d'accessibilité de SygixOs (autres services conservés), accord de la permission de superposition aux autres applications, activation de l'option « Démarrage à l'allumage », chacun relu ; puis SygixOs SHALL afficher l'écran « SygixOs garde la main » : titre « SygixOs garde la main », description « L'accueil Google TV reste actif : SygixOs a réglé la touche Home et l'ouverture au démarrage. », les lignes « Touche Home » (détail « Le service de SygixOs ramène à SygixOs »), « Superposer aux autres applications » (détail « Permet à SygixOs de s'ouvrir au démarrage de la TV »), « Ouverture à l'allumage » (détail « SygixOs demande à s'ouvrir au démarrage de la TV »), chacune avec son état relu, puis, avec Tsuyu « Actif » (9.12), la ligne « Réparation automatique » à l'état « Active », détail « Tsuyu permet à SygixOs de refaire ces réglages si la TV les défait. Gardez le débogage activé. », et la ligne « Bon à savoir », détail « Si la touche Home ne ramène plus à SygixOs après un redémarrage, ouvrez SygixOs une fois : il refait ses réglages. » ; sans Tsuyu (8.9), la ligne « Bon à savoir », détail « Sur certaines TV, après un redémarrage complet, l'accueil Google TV peut reprendre la main ; SygixOs vous le signale. » ; l'aide « Retour : fermer » et les boutons « Détails » et « Terminé », focalisé. Une étape en échec SHALL arrêter les étapes qui en dépendent et afficher l'écran 8.12 : titre « Configuration interrompue », description « Une étape n'a pas abouti. Ce qui est fait reste en place ; vous pouvez réessayer. », un encadré d'erreur propre à l'étape (pour l'écran d'accueil : « L'écran d'accueil n'a pas pu être réglé », détail « La touche Home ouvre encore l'accueil Google TV. Vérifiez que le débogage est toujours activé, puis réessayez. »), les lignes des étapes avec « Fait », « Échec » (rouge) ou « Annulée » (détail « Non effectuée »), l'aide « Retour : fermer » et les boutons « Fermer » et « Réessayer », focalisé. Les écrans de résultat et d'échec de la séquence affichent le repère de l'étape « Réglages ». Aucune étape SHALL n'être présentée comme faite sans relecture, ni réessayée sans action. Dans les écrans à deux boutons, gauche et droite passent de l'un à l'autre sans boucle et haut et bas sont sans effet. L'état de la voie automatique, affiché sur la première page du parcours et dans le panneau « Launcher principal », SHALL être « Active » quand SygixOs est l'écran d'accueil effectif ou que les réglages de secours sont en place, « Indisponible sur cette TV » (« Accès par le débogage »), sinon « À configurer ».

#### Scenario: configuration sans désactivation
- **WHEN** l'accès est prêt et que SygixOs devient l'écran d'accueil effectif après la définition de l'écran d'accueil
- **THEN** « Configuration en cours » montre les étapes passer à « Fait » une à une, puis « SygixOs est votre écran d'accueil » s'affiche avec « Terminé » focalisé, sans confirmation ni désactivation, et la voie automatique est « Active »

#### Scenario: désactivation proposée
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif après la vérification et que l'accueil d'origine peut être désactivé
- **THEN** l'écran 9.11 s'affiche avec « Garder l'accueil Google TV (conseillé) » focalisé ; aucun composant n'est modifié

#### Scenario: ne pas désactiver
- **WHEN** l'utilisateur presse OK sur « Garder l'accueil Google TV (conseillé) » ou Retour sur 9.11
- **THEN** aucun composant n'est désactivé, le service d'accessibilité est ajouté sans retirer les autres, la superposition est accordée, l'option de démarrage est activée, chaque réglage est relu, et « SygixOs garde la main » s'affiche avec « Terminé » focalisé

#### Scenario: désactivation demandée
- **WHEN** l'utilisateur presse bas puis OK sur 9.11
- **THEN** la confirmation 8.8 s'affiche avec « Annuler » focalisé

#### Scenario: désactivation impossible
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif et qu'aucun composant de l'accueil d'origine n'est actif et modifiable, ou que les garde-fous l'interdisent
- **THEN** 9.11 n'est pas affiché et les réglages de secours sont posés, puis « SygixOs garde la main » s'affiche

#### Scenario: résultat avec Tsuyu
- **WHEN** la séquence s'est exécutée par Tsuyu et que les réglages de secours sont posés
- **THEN** l'écran 9.12 affiche la ligne « Réparation automatique » à « Active », le « Bon à savoir » propre à Tsuyu et aucun conseil de désactiver le débogage

#### Scenario: échec d'une étape
- **WHEN** la définition de l'écran d'accueil échoue (erreur, délai ou relecture qui ne la confirme pas)
- **THEN** 8.12 s'affiche avec « Démarrage automatique » « Fait », « Écran d'accueil » « Échec », « Vérification » « Annulée », l'encadré « L'écran d'accueil n'a pas pu être réglé » et « Réessayer » focalisé ; les étapes déjà faites restent en place

#### Scenario: panneau fermé pendant la configuration
- **WHEN** l'utilisateur presse Retour ou la touche Home pendant « Configuration en cours »
- **THEN** le panneau se ferme, la séquence continue jusqu'à sa fin ou son délai, et à la réouverture l'écran correspondant à l'état relu s'affiche

### Requirement: Panneau Launcher principal
La ligne « Configurer SygixOs comme launcher principal » des réglages (`settings`) SHALL ouvrir, par-dessus la page de réglages, le panneau « Launcher principal » (8.16, 9.14, 9.15) : description « SygixOs est l'écran d'accueil de la TV. Voici l'état de chaque façon de le configurer. » quand une voie est « Active », sinon « SygixOs n'est pas encore l'écran d'accueil. Voici l'état de chaque façon de le configurer. » ; les lignes « Configuration automatique », « Tsuyu » (affichée seulement quand Tsuyu est disponible, `tsuyu-access`) et « Par autorisations », chacune avec son état et un détail qui le résume (par exemple « Accueil Google TV désactivé · SygixOs est l'écran d'accueil », « Accueil Google TV gardé · touche Home et allumage réglés », « Pas encore configurée », « Pas utilisée sur cette TV », « Aucune autorisation donnée ») ; la ligne « Tsuyu » affiche l'état de « États de Tsuyu » (`tsuyu-access`) avec le détail « Répare les réglages si la TV les défait » (« Actif »), « Pour que SygixOs se répare tout seul » (« À installer »), « OK : ouvre Tsuyu pour le démarrer » (« Arrêté »), « OK : autorise SygixOs dans Tsuyu » (« À autoriser »), « Version trop ancienne pour SygixOs » (« À mettre à jour ») ou la raison « Le débogage de cette TV ne permet pas à Tsuyu de démarrer » (« Indisponible sur cette TV ») ; puis les actions « Relancer la configuration », détail « Refait les réglages automatiques » quand Tsuyu est « Actif », sinon « Refait les réglages automatiques (débogage nécessaire) » ; « Remplacer complètement l'accueil Google TV », détail « Le désactive : selon la TV, des touches de la télécommande ne marchent plus », affichée seulement quand la voie automatique est « Active » avec l'accueil d'origine actif et modifiable et que les garde-fous le permettent ; « Rétablir l'accueil Google TV », détail « Le réactive et lui rend l'écran d'accueil », affichée seulement quand au moins un composant de l'accueil d'origine est désactivé par l'utilisateur ; l'aide « Retour : fermer ». Focus initial sur « Relancer la configuration » ; haut et bas parcourent les lignes de voie puis les actions, sans boucle ; gauche et droite sont sans effet. OK sur « Configuration automatique » ou « Relancer la configuration » SHALL suivre la configuration automatique comme depuis la première page, par Tsuyu quand il est « Actif », sinon sans Tsuyu ; OK sur « Tsuyu » SHALL ouvrir l'écran que demande son état (« À installer » et « À mettre à jour » : écrans du débogage si besoin puis 9.4 ; « Arrêté » : 9.7 ; « À autoriser » : 9.8 ; « Actif » : Tsuyu ; « Indisponible sur cette TV » : sans effet) ; OK sur « Par autorisations » SHALL ouvrir la page 8.13 ; OK sur « Remplacer complètement l'accueil Google TV » SHALL ouvrir la confirmation 8.8 (« Désactivation de l'accueil d'origine ») ; OK sur « Rétablir l'accueil Google TV » SHALL ouvrir sa confirmation (« Rétablissement de l'accueil d'origine »). Les écrans ouverts depuis ce panneau n'affichent pas le repère d'avancement. Retour et la fermeture d'un écran ouvert depuis ce panneau SHALL rendre le focus à la ligne des réglages, sans modifier le drapeau du parcours automatique.

#### Scenario: ouverture depuis les réglages
- **WHEN** l'accueil d'origine a été désactivé par la configuration automatique sans Tsuyu, que Tsuyu n'est pas disponible et que l'utilisateur presse OK sur la ligne des réglages
- **THEN** le panneau 8.16 s'affiche avec « Configuration automatique » « Active », « Par autorisations » « Non utilisée », « Relancer la configuration » focalisé et « Rétablir l'accueil Google TV » affiché

#### Scenario: configuré avec Tsuyu
- **WHEN** la configuration automatique a été faite par Tsuyu en gardant l'accueil Google TV et que Tsuyu est « Actif »
- **THEN** le panneau 9.14 affiche « Configuration automatique » « Active » (« Accueil Google TV gardé · touche Home et allumage réglés »), « Tsuyu » « Actif », « Par autorisations » « Non utilisée », « Relancer la configuration » (« Refait les réglages automatiques ») focalisé et « Remplacer complètement l'accueil Google TV », sans « Rétablir l'accueil Google TV »

#### Scenario: Tsuyu à installer
- **WHEN** la configuration automatique a été faite sans Tsuyu en gardant l'accueil Google TV et qu'une release de Tsuyu est connue
- **THEN** le panneau 9.15 affiche « Tsuyu » « À installer » (« Pour que SygixOs se répare tout seul ») ; OK sur cette ligne ouvre les écrans du débogage si le débogage est désactivé, sinon 9.4

#### Scenario: rien de configuré
- **WHEN** aucune voie n'est « Active », que l'accueil d'origine est actif et que Tsuyu n'est pas disponible
- **THEN** le panneau 8.21 affiche « SygixOs n'est pas encore l'écran d'accueil. Voici l'état de chaque façon de le configurer. », « Configuration automatique » « À configurer » (« Pas encore configurée »), « Par autorisations » « À faire » (« Aucune autorisation donnée »), « Relancer la configuration » focalisé, sans « Remplacer complètement l'accueil Google TV » ni « Rétablir l'accueil Google TV »

#### Scenario: fermeture
- **WHEN** l'utilisateur presse Retour sur le panneau
- **THEN** il se ferme et la ligne des réglages a le focus avec son état recalculé

### Requirement: Désactivation de l'accueil d'origine
SygixOs SHALL désactiver l'accueil d'origine uniquement depuis la ligne « Désactiver l'accueil Google TV » de 9.11 ou l'action « Remplacer complètement l'accueil Google TV » du panneau « Launcher principal », après le dialogue de confirmation 8.8 : titre « Désactiver l'accueil Google TV ? », texte « SygixOs deviendra l'écran d'accueil. Selon la TV, des fonctions de l'accueil Google TV peuvent disparaître : réglages rapides, guide des programmes, appui long sur Home, choix du profil. Rétablissez-le dans Réglages › Écran d'accueil, et avant de désinstaller SygixOs. », boutons « Annuler », focalisé, et « Désactiver », aide « Retour : annuler » ; gauche et droite passent d'un bouton à l'autre sans boucle, haut et bas sont sans effet, Retour équivaut à « Annuler », qui revient à l'écran d'origine avec le focus sur l'élément qui a ouvert le dialogue. Depuis le panneau, l'accès doit être prêt : Tsuyu « Actif », sinon les écrans du débogage sans Tsuyu, comme pour la configuration automatique. SygixOs SHALL relire juste avant les garde-fous : accès prêt, configuration initiale de l'appareil terminée, activité d'accueil de SygixOs déclarée et activée, au moins un composant actif et modifiable ; un garde-fou non tenu SHALL empêcher toute désactivation. La désactivation SHALL suivre `design.md` de `p7-default-launcher` (D6), sa progression étant affichée comme « Configuration en cours » avec la ligne « Accueil Google TV » : composants actifs désactivés un par un dans l'ordre de l'ensemble, chacun relu, puis vérification de l'écran d'accueil effectif ; réussie, l'écran « SygixOs est votre écran d'accueil » s'affiche depuis 9.11, ou le panneau revient avec ses états recalculés et le focus sur « Rétablir l'accueil Google TV » depuis le panneau. Un échec, un délai ou une vérification négative SHALL remettre à l'état par défaut, dans l'ordre inverse, les composants désactivés par cette opération, puis relire ; depuis 9.11, SygixOs pose ensuite les réglages de secours (« SygixOs garde la main ») ; depuis le panneau, il affiche l'écran 8.12 avec l'encadré « L'accueil Google TV n'a pas pu être désactivé », détail « Il est resté actif et rien d'autre n'a changé. Vérifiez l'accès, puis réessayez. ». SygixOs SHALL ne jamais laisser l'appareil sans écran d'accueil activé.

#### Scenario: confirmation
- **WHEN** l'utilisateur presse OK sur « Désactiver l'accueil Google TV » en 9.11
- **THEN** le dialogue 8.8 s'affiche, sans mascotte, avec « Annuler » focalisé ; aucun composant n'est modifié avant OK sur « Désactiver »

#### Scenario: annulation
- **WHEN** l'utilisateur presse « Annuler » ou Retour dans 8.8 ouvert depuis 9.11
- **THEN** le dialogue se ferme, rien n'est modifié et 9.11 a le focus sur « Désactiver l'accueil Google TV »

#### Scenario: désactivation réussie
- **WHEN** l'utilisateur presse « Désactiver », que chaque composant actif est désactivé et que SygixOs est ensuite l'écran d'accueil effectif
- **THEN** l'écran « SygixOs est votre écran d'accueil » s'affiche et l'état relu de chaque composant est « désactivé par l'utilisateur »

#### Scenario: remplacement depuis le panneau
- **WHEN** Tsuyu est « Actif », que l'utilisateur presse « Remplacer complètement l'accueil Google TV » puis « Désactiver » et que la désactivation réussit
- **THEN** aucun écran du débogage n'est affiché, le panneau revient avec « Configuration automatique » « Active », « Rétablir l'accueil Google TV » affiché et focalisé, et sans « Remplacer complètement l'accueil Google TV »

#### Scenario: désactivation partielle annulée
- **WHEN** la désactivation d'un composant échoue ou dépasse son délai, ou que SygixOs n'est pas l'écran d'accueil effectif après la désactivation
- **THEN** les composants désactivés par cette opération sont remis à leur état par défaut dans l'ordre inverse, l'état est relu, aucun succès n'est affiché, et les réglages de secours sont posés depuis 9.11, ou l'encadré « L'accueil Google TV n'a pas pu être désactivé » s'affiche depuis le panneau

#### Scenario: appareil pas encore configuré
- **WHEN** la configuration initiale de l'appareil n'est pas terminée
- **THEN** 9.11 n'est pas affiché, « Remplacer complètement l'accueil Google TV » n'est pas proposé et aucune désactivation n'est exécutée

### Requirement: Rétablissement de l'accueil d'origine
OK sur « Rétablir l'accueil Google TV » (panneau « Launcher principal »), accès prêt (Tsuyu « Actif », sinon les écrans du débogage sans Tsuyu, comme pour la configuration automatique), SHALL afficher le dialogue 8.17 : titre « Rétablir l'accueil Google TV ? », texte « L'accueil Google TV sera réactivé et redeviendra l'écran d'accueil. Les autres réglages faits par SygixOs restent en place : si son service reste activé, la touche Home ouvrira encore SygixOs. », boutons « Annuler » et « Rétablir », ce dernier focalisé, aide « Retour : annuler », mêmes règles de navigation que 8.8. « Rétablir » SHALL remettre à l'état par défaut les composants désactivés par l'utilisateur, dans l'ordre inverse de l'ensemble, puis, quand un écran d'accueil précédent a été relevé et qu'il est présent et activé, le redéfinir comme écran d'accueil ; tout est relu. Le rétablissement ne concerne que l'accueil : les autorisations accordées, le service d'accessibilité et Tsuyu ne sont pas modifiés. Réussi, le panneau revient avec ses états recalculés, sans « Rétablir l'accueil Google TV », et le focus sur « Relancer la configuration ». Un échec SHALL être affiché comme en 8.12 avec l'encadré « L'accueil Google TV n'a pas pu être rétabli », sans succès supposé.

#### Scenario: rétablissement réussi
- **WHEN** l'utilisateur presse « Rétablir » et que les composants sont remis à leur état par défaut et l'écran d'accueil précédent redéfini
- **THEN** le panneau revient sans « Rétablir l'accueil Google TV », « Relancer la configuration » a le focus et la voie automatique n'est plus « Active » si aucun réglage de secours n'est en place

#### Scenario: rétablissement par Tsuyu
- **WHEN** Tsuyu est « Actif », le débogage n'ayant pas été réactivé par l'utilisateur depuis la configuration
- **THEN** OK sur « Rétablir l'accueil Google TV » affiche directement 8.17, sans écran du débogage ni fenêtre système

#### Scenario: rétablissement annulé
- **WHEN** l'utilisateur presse « Annuler » ou Retour dans 8.17
- **THEN** rien n'est modifié et « Rétablir l'accueil Google TV » a le focus

#### Scenario: sans écran d'accueil précédent relevé
- **WHEN** l'accueil d'origine a été désactivé par la procédure ADB du README et qu'aucun écran d'accueil précédent n'a été relevé
- **THEN** seuls les composants sont remis à leur état par défaut, et le système réattribue l'écran d'accueil

#### Scenario: échec du rétablissement
- **WHEN** la remise à l'état par défaut d'un composant échoue ou dépasse son délai
- **THEN** l'encadré « L'accueil Google TV n'a pas pu être rétabli » s'affiche avec « Réessayer » focalisé et l'état relu, sans succès supposé

### Requirement: Opérations privilégiées limitées
SygixOs SHALL n'exécuter, par l'ADB intégré comme par Tsuyu, que les opérations fixes de `design.md` de `p7-default-launcher` (D2), chacune avec des arguments fixes visant uniquement le paquet SygixOs, son service d'accessibilité ou un composant de l'ensemble fermé de l'accueil d'origine ; toute autre cible SHALL être refusée avant envoi. SygixOs SHALL ne jamais exécuter de commande saisie ou reçue de l'extérieur, ne SHALL révoquer aucune autorisation, et SHALL conserver les autres services d'accessibilité activés. Le retrait suivi de la remise du service de SygixOs dans la liste des services activés est la seule écriture de cette liste qui le retire, et seulement pour la réparation automatique (`tsuyu-access`). Aucune connexion privilégiée, ni aucun service de SygixOs lancé par Tsuyu, SHALL ne rester ouvert hors d'une séquence demandée par l'utilisateur ou d'une réparation automatique.

#### Scenario: cible refusée
- **WHEN** une opération est demandée avec un paquet ou un composant hors du paquet SygixOs et de l'ensemble fermé
- **THEN** elle est refusée sans être envoyée, par l'ADB intégré comme par Tsuyu, et l'échec est rapporté

#### Scenario: service d'accessibilité ajouté
- **WHEN** la configuration active le service d'accessibilité de SygixOs alors que d'autres services sont activés
- **THEN** la liste relue contient les autres services inchangés et celui de SygixOs

#### Scenario: service relié par la réparation
- **WHEN** la réparation automatique retire puis remet le service de SygixOs
- **THEN** la liste relue après l'opération contient les autres services inchangés et celui de SygixOs

#### Scenario: fin de séquence
- **WHEN** une séquence ou une réparation se termine, réussie, en échec ou par délai
- **THEN** la connexion au débogage est fermée, ou le service de SygixOs lancé par Tsuyu est arrêté

### Requirement: Détails des modifications
Le bouton secondaire « Détails » (« SygixOs est votre écran d'accueil », « SygixOs garde la main ») SHALL ouvrir le panneau « Ce que SygixOs a modifié » (8.14, 9.16), sans mascotte : titre « Ce que SygixOs a modifié », description « Rien d'autre n'a été touché. « Rétablir l'accueil Google TV » ne concerne que l'accueil ; les autorisations se retirent dans les paramètres Android. », puis une ligne non focalisable par réglage, avec son état relu : « Démarrage automatique » (« La TV laisse SygixOs démarrer avec elle »), « Écran d'accueil » (« SygixOs défini comme écran d'accueil d'Android »), « Superposer aux autres applications » (« Permet à SygixOs de s'ouvrir au démarrage de la TV »), « Touche Home » (« Le service de SygixOs ne lit que la touche Home »), « Accueil Google TV » (« Toujours actif : désactivé seulement si vous le choisissez » à l'état « Inchangé », ou « Désactivé à votre demande » à l'état « Fait »), « Tsuyu » (« Installé par SygixOs : il garde l'accès au débogage » à l'état « Fait »), « Réparations automatiques » (détail « Dernière : <date> · <réglage> », ou « Aucune pour l'instant », à l'état « Active » quand Tsuyu est « Actif », « Arrêtée » sinon) ; seules les lignes des réglages réellement modifiés par SygixOs sont affichées, l'accueil Google TV toujours, « Tsuyu » quand SygixOs l'a installé et « Réparations automatiques » quand une configuration a été faite par Tsuyu. L'aide « Retour : fermer » et le bouton « Fermer », focalisé, rendent le focus à « Détails ».

#### Scenario: détails après les réglages de secours
- **WHEN** l'utilisateur presse OK sur « Détails » sur « SygixOs garde la main » sans Tsuyu
- **THEN** 8.14 liste « Démarrage automatique », « Écran d'accueil », « Superposer aux autres applications » et « Touche Home » à l'état « Fait », et « Accueil Google TV » à l'état « Inchangé » ; « Fermer » a le focus et rend le focus à « Détails »

#### Scenario: détails avec Tsuyu
- **WHEN** l'utilisateur presse OK sur « Détails » après une configuration par Tsuyu installé par SygixOs, une réparation ayant eu lieu
- **THEN** 9.16 liste aussi « Tsuyu » « Fait » et « Réparations automatiques » « Active » avec la date et le réglage de la dernière réparation

#### Scenario: détails après désactivation
- **WHEN** l'utilisateur presse OK sur « Détails » sur « SygixOs est votre écran d'accueil » après une désactivation sans Tsuyu
- **THEN** 8.14 liste « Démarrage automatique », « Écran d'accueil » et « Accueil Google TV » « Désactivé à votre demande », sans les réglages de secours

### Requirement: Débogage après la configuration
Après une configuration réussie sans Tsuyu, la ligne « Conseil : désactivez le débogage » de 8.6 SHALL être affichée tant que le débogage est activé, relu au retour au premier plan ; relancer la configuration ou rétablir l'accueil Google TV exigera alors de le réactiver (écrans du débogage et 8.4). Après une configuration par Tsuyu, aucun écran SHALL ne conseiller de désactiver le débogage : la ligne « Réparation automatique » demande de le garder activé, car Tsuyu en a besoin. SygixOs SHALL ne jamais désactiver ni activer le débogage lui-même.

#### Scenario: conseil masqué
- **WHEN** l'utilisateur désactive le débogage puis revient dans SygixOs sur 8.6
- **THEN** la ligne « Conseil : désactivez le débogage » n'est plus affichée

#### Scenario: pas de conseil avec Tsuyu
- **WHEN** la configuration s'est faite par Tsuyu
- **THEN** ni 9.12 ni 9.13 n'affichent « Conseil : désactivez le débogage » et la ligne « Réparation automatique » est affichée
