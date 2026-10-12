# Spec Delta

L'« accueil d'origine » désigne l'écran d'accueil fourni avec la TV et le composant système qui le réactive ; son ensemble fermé est défini par `design.md` (D4). La ligne des réglages est décrite par `settings` ; ce delta décrit le parcours, ses pages, ses dialogues et leurs effets sur le système.

## ADDED Requirements

### Requirement: Accès par le débogage
SygixOs SHALL pouvoir exécuter les opérations de « Opérations privilégiées limitées » en se connectant au débogage ADB de l'appareil sur lequel il tourne, sans ordinateur ni application tierce : connexion à l'adresse de bouclage sur le port ADB réseau, avec une paire de clés propre à SygixOs générée à la première connexion et conservée dans son stockage privé, que l'utilisateur autorise dans le dialogue système du débogage. SygixOs SHALL n'ouvrir cette connexion qu'après une action explicite de l'utilisateur dans la page « Configuration automatique » (« Configuration automatique de l'écran d'accueil »), SHALL la fermer à la fin de chaque séquence, SHALL ne jamais se connecter à une autre adresse que l'appareil lui-même et SHALL ne jamais transmettre sa clé hors de l'appareil. L'état de l'accès SHALL être l'un de : « Débogage désactivé » (réglage système du débogage désactivé, lu sans connexion), « Connexion impossible » (débogage activé mais port fermé, connexion refusée ou délai dépassé), « Autorisation en attente » (connexion ouverte, clé pas encore acceptée), « Autorisation refusée », « Prêt » (clé acceptée et commande de test exécutée avec l'identité du shell). La connexion, l'attente d'autorisation et chaque commande SHALL avoir un délai borné (`design.md`, D1) ; un délai dépassé SHALL ne jamais produire un état « Prêt » ni un succès. Aucune absence de débogage, refus ou perte de connexion SHALL ne rendre SygixOs inutilisable ni provoquer de crash.

#### Scenario: débogage désactivé
- **WHEN** l'utilisateur lance la configuration automatique alors que le débogage est désactivé dans les réglages du système
- **THEN** aucune connexion n'est tentée, l'accès est « Débogage désactivé » et le guide d'activation du débogage s'affiche (« Guide d'activation du débogage »)

#### Scenario: première autorisation
- **WHEN** le débogage est activé et que SygixOs se connecte pour la première fois
- **THEN** l'accès passe à « Autorisation en attente » pendant que le dialogue système d'autorisation est à l'écran ; quand l'utilisateur accepte, la commande de test est exécutée et l'accès passe à « Prêt »

#### Scenario: autorisation refusée
- **WHEN** l'utilisateur refuse ou ferme le dialogue système d'autorisation, ou que le délai d'autorisation expire
- **THEN** l'accès est « Autorisation refusée » (« Connexion impossible » pour un délai expiré), aucune opération n'est exécutée et le guide explique d'accepter le dialogue en cochant « Toujours autoriser » ; une nouvelle tentative n'a lieu qu'après une action de l'utilisateur

#### Scenario: port fermé
- **WHEN** le débogage est activé mais que la connexion au port ADB réseau est refusée ou n'aboutit pas dans le délai
- **THEN** l'accès est « Connexion impossible », aucune opération n'est exécutée, SygixOs reste utilisable et le guide affiche l'explication de cet état

#### Scenario: clé déjà autorisée
- **WHEN** la clé de SygixOs a déjà été autorisée avec « Toujours autoriser » et que le débogage est activé
- **THEN** la connexion aboutit sans dialogue système et l'accès passe directement à « Prêt »

#### Scenario: débogage perdu pendant une séquence
- **WHEN** le débogage est désactivé ou la connexion perdue pendant une séquence d'opérations
- **THEN** la séquence s'arrête, l'état du système est relu, l'opération en cours est rapportée en échec, sans succès supposé, et l'état partiel reste rétablissable

### Requirement: Guide d'activation du débogage
Quand l'accès n'est pas « Prêt » au lancement de la configuration automatique, SygixOs SHALL afficher dans le panneau du parcours une sous-page « Activer le débogage » en étapes numérotées, non focalisables : 1 « Ouvrez les options pour les développeurs. Si elles sont masquées : Réglages › Système › À propos, puis appuyez 7 fois sur « Build ». » ; 2 « Activez « Débogage USB ». » ; 3 « Revenez dans SygixOs et choisissez « Continuer ». » ; 4 « À l'écran « Autoriser le débogage ? », cochez « Toujours autoriser » puis choisissez « Autoriser ». ». Une ligne d'état SHALL expliquer l'état courant de l'accès en langage utilisateur (« Le débogage est désactivé. », « SygixOs n'arrive pas à se connecter au débogage de cette TV. », « Autorisez SygixOs à l'écran « Autoriser le débogage ? ». », « L'autorisation a été refusée. »). Le pied SHALL proposer « Ouvrir les réglages » (écran système des options pour les développeurs quand il s'ouvre, sinon l'écran système « À propos ») et « Continuer » (nouvelle tentative de connexion), avec l'aide « Retour : revenir ». Navigation D-pad : focus initial sur « Ouvrir les réglages » quand le débogage est désactivé, sinon sur « Continuer » ; gauche et droite passent d'un bouton à l'autre sans boucle ; haut et bas sont sans effet ; Retour revient à la page « Configuration automatique » avec le focus sur son action principale. Au retour d'un écran système, la sous-page est toujours affichée, l'état est relu et le focus est sur « Continuer ». Quand l'accès devient « Prêt » après « Continuer », SygixOs SHALL revenir à la page « Configuration automatique » et lancer la séquence demandée, sans autre action.

#### Scenario: guide affiché
- **WHEN** l'utilisateur presse OK sur « Lancer la configuration » avec le débogage désactivé
- **THEN** la sous-page « Activer le débogage » s'affiche avec ses quatre étapes numérotées, la ligne « Le débogage est désactivé. » et le focus sur « Ouvrir les réglages »

#### Scenario: ouverture des réglages
- **WHEN** l'utilisateur presse OK sur « Ouvrir les réglages », puis revient dans SygixOs après avoir activé le débogage
- **THEN** l'écran système des options pour les développeurs s'ouvre (l'écran « À propos » s'il ne peut pas s'ouvrir) ; au retour, la sous-page est affichée, l'état est relu et « Continuer » a le focus

#### Scenario: écrans système absents
- **WHEN** ni l'écran des options pour les développeurs ni l'écran « À propos » ne peuvent s'ouvrir
- **THEN** « Ouvrir les réglages » ne change rien, sans crash, et les étapes restent affichées

#### Scenario: continuer jusqu'à l'accès
- **WHEN** l'utilisateur presse OK sur « Continuer », débogage activé, et accepte le dialogue système
- **THEN** la page « Configuration automatique » revient et la séquence demandée démarre

#### Scenario: retour depuis le guide
- **WHEN** l'utilisateur presse Retour sur la sous-page
- **THEN** la page « Configuration automatique » s'affiche avec le focus sur son action principale, et aucune opération n'est lancée

### Requirement: Configuration automatique de l'écran d'accueil
Le parcours guidé SHALL proposer une page « Configuration automatique » qui explique en une phrase ce que fera SygixOs (« SygixOs autorise son démarrage, se définit comme écran d'accueil et vérifie que la TV l'ouvre. Si l'accueil Google TV garde la main, SygixOs vous proposera de le désactiver ; sinon, la touche Home ouvrira SygixOs. »), affiche le résultat courant et propose, dans cet ordre, l'action principale, « Rétablir l'accueil Google TV » quand un rétablissement est possible (« Rétablissement de l'accueil d'origine »), et le lien « Détails » (« Détails des modifications »), avec l'aide « Retour : revenir au choix ». L'action principale SHALL être « Lancer la configuration » tant que rien n'est configuré, « Réessayer » après un échec, « Relancer la configuration » sinon. Sur OK, accès prêt (sinon « Guide d'activation du débogage »), SygixOs SHALL enchaîner sans autre confirmation, selon `design.md` (D5) : l'autorisation de démarrage du constructeur (« Démarrage automatique du constructeur »), la définition de SygixOs comme écran d'accueil après avoir relevé l'écran d'accueil précédent, puis la vérification de l'écran d'accueil effectif (« État de l'écran d'accueil »). Si SygixOs n'est pas alors l'écran d'accueil effectif et que l'accueil d'origine peut être désactivé, SygixOs SHALL proposer sa désactivation avec sa confirmation (« Désactivation de l'accueil d'origine ») ; si l'utilisateur l'annule, si elle n'est pas possible ou si elle échoue et est annulée, SygixOs SHALL poser automatiquement les solutions de repli dans la même séquence : activation de son service d'accessibilité, accord de l'affichage superposé et activation de l'option « Démarrage à l'allumage ». Pendant la séquence, la page SHALL afficher l'opération en cours en langage utilisateur (« Autorisation du démarrage… », « Définition de l'écran d'accueil… », « Vérification… », « Désactivation de l'accueil Google TV… », « Configuration de la touche Home… ») et aucune action ne SHALL pouvoir en lancer une autre. À la fin, la page SHALL afficher un résultat : « SygixOs est l'écran d'accueil » (configuration terminée), « La touche Home ouvre SygixOs » (repli posé), ou un échec qui dit l'effet manquant pour l'utilisateur (par exemple « SygixOs n'a pas pu se définir comme écran d'accueil. »). Chaque opération SHALL être relue auprès du système ; une opération qui échoue SHALL ne pas empêcher les opérations indépendantes, ne SHALL jamais être présentée comme réussie et ne SHALL être réessayée qu'après une action. L'état de la voie, affiché sur la page de choix et dans les réglages, SHALL être : « À faire » (rien de configuré par cette voie), « En cours » (séquence en cours), « Configuré » (SygixOs écran d'accueil effectif), « Partiel » (repli posé ou configuration incomplète), « Échec » (dernière séquence en échec, jusqu'à la prochaine action ou au prochain lancement), « Indisponible » (débogage impossible à activer ou à joindre sur cet appareil, constaté par le guide). Après une configuration terminée ou un repli posé, la page SHALL afficher le conseil de « Débogage après la configuration ». Une séquence lancée SHALL continuer si le parcours est fermé, par Retour ou par la touche Home ; l'état est relu à la réouverture. Navigation D-pad : focus initial sur l'action principale ; haut et bas parcourent l'action principale, « Rétablir l'accueil Google TV » quand elle est affichée, puis « Détails », sans boucle ; gauche et droite sont sans effet ; Retour revient à la page de choix avec le focus sur « Configuration automatique ».

#### Scenario: configuration sans désactivation
- **WHEN** l'accès est prêt, que l'utilisateur presse OK sur « Lancer la configuration » et que SygixOs devient l'écran d'accueil effectif après la définition de l'écran d'accueil
- **THEN** l'autorisation de démarrage est accordée si l'appareil la prend en charge, l'écran d'accueil précédent est relevé, aucune confirmation ni désactivation n'a lieu, la page affiche « SygixOs est l'écran d'accueil » et l'état de la voie est « Configuré »

#### Scenario: désactivation nécessaire et acceptée
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif après la vérification et que l'utilisateur confirme la désactivation de l'accueil d'origine, qui réussit
- **THEN** la page affiche « SygixOs est l'écran d'accueil », l'état est « Configuré » et « Rétablir l'accueil Google TV » est affiché

#### Scenario: désactivation refusée
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif et que l'utilisateur choisit « Annuler » dans la confirmation de désactivation
- **THEN** aucun composant n'est désactivé, le service d'accessibilité de SygixOs est activé sans retirer les autres services, l'affichage superposé est accordé, l'option « Démarrage à l'allumage » est activée, chaque opération est relue, la page affiche « La touche Home ouvre SygixOs » et l'état est « Partiel »

#### Scenario: désactivation impossible
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif et que l'accueil d'origine n'a aucun composant actif et modifiable, ou que les garde-fous l'interdisent
- **THEN** aucune confirmation de désactivation n'est affichée et les solutions de repli sont posées comme au scénario « désactivation refusée »

#### Scenario: autorisation de démarrage absente
- **WHEN** l'appareil n'a pas d'autorisation de démarrage du constructeur
- **THEN** cette opération est sans objet, n'est pas un échec, et la séquence continue avec l'écran d'accueil

#### Scenario: échec d'une opération
- **WHEN** la définition de l'écran d'accueil échoue (erreur, délai ou relecture qui ne la confirme pas)
- **THEN** la page affiche « SygixOs n'a pas pu se définir comme écran d'accueil. », l'action principale devient « Réessayer », aucune opération n'est réessayée sans action, les opérations déjà réussies restent en place et l'état est « Échec »

#### Scenario: relance
- **WHEN** l'utilisateur presse OK sur « Relancer la configuration » alors que la voie est « Configuré » ou « Partiel »
- **THEN** la séquence reprend depuis le début ; une opération déjà en place n'est pas réécrite (relecture seulement) et le résultat est recalculé

#### Scenario: parcours fermé pendant la séquence
- **WHEN** l'utilisateur presse Retour ou la touche Home pendant une séquence
- **THEN** la séquence continue jusqu'à sa fin ou son délai ; à la réouverture du parcours, la page affiche l'état relu, ou l'opération en cours si elle n'est pas finie ; une confirmation de désactivation encore attendue à la fermeture compte comme « Annuler »

#### Scenario: navigation de la page
- **WHEN** la page affiche l'action principale, « Rétablir l'accueil Google TV » et « Détails », et que l'utilisateur presse bas trois fois, haut trois fois, puis gauche et droite
- **THEN** le focus passe de l'action principale à « Rétablir l'accueil Google TV », à « Détails », y reste, remonte jusqu'à l'action principale et y reste ; gauche et droite ne le déplacent pas

### Requirement: État de l'écran d'accueil
SygixOs SHALL relire, sans débogage, à chaque retour au premier plan, à l'ouverture du parcours et après chaque opération : l'écran d'accueil effectif (SygixOs quand l'intent `HOME` se résout vers lui et qu'aucune autre activité `HOME` activée n'a une priorité supérieure à la sienne), la présence et l'état de chaque composant de l'accueil d'origine (actif, désactivé par l'utilisateur, hors de portée), la fin de la configuration initiale de l'appareil, l'état du service d'accessibilité (activé, connecté) et de l'affichage superposé. Ces lectures SHALL déterminer les états affichés, sans valeur conservée d'avant la sortie ; une erreur de lecture SHALL être distincte de l'absence et ne SHALL jamais produire l'état « Configuré ». L'état de l'autorisation de démarrage du constructeur, illisible sans débogage, SHALL n'être affiché que dans « Détails », tel que relu lors de la dernière séquence.

#### Scenario: configuration défaite hors de SygixOs
- **WHEN** l'utilisateur ou le système réactive l'accueil d'origine ou change l'écran d'accueil pendant que SygixOs est en arrière-plan
- **THEN** au retour au premier plan, l'état de la voie automatique est recalculé (par exemple « Partiel » ou « À faire »), sans réapplication ni rappel automatique

#### Scenario: accueil désactivé par la procédure ADB
- **WHEN** SygixOs se lance alors que l'accueil d'origine a été désactivé par la procédure ADB du README et que SygixOs est l'écran d'accueil effectif
- **THEN** la voie automatique affiche « Configuré » et « Rétablir l'accueil Google TV » est proposé

#### Scenario: erreur de lecture
- **WHEN** la lecture de l'état d'un composant ou de l'écran d'accueil échoue
- **THEN** l'état de la voie n'est pas « Configuré », aucun crash n'a lieu et la lecture est refaite au prochain retour au premier plan

### Requirement: Désactivation de l'accueil d'origine
SygixOs SHALL désactiver l'accueil d'origine uniquement depuis la configuration automatique, après la confirmation « Désactiver l'accueil Google TV ? » (texte, boutons et focus : `design.md`, décision 10), dialogue centré par-dessus le panneau, avec « Annuler » focalisé au départ, gauche et droite passant d'un bouton à l'autre sans boucle, haut et bas sans effet, et Retour équivalent à « Annuler ». La confirmation SHALL proposer le lien « Détails ». SygixOs SHALL relire juste avant les garde-fous : accès prêt, configuration initiale de l'appareil terminée, activité d'accueil de SygixOs déclarée et activée, au moins un composant actif et modifiable ; un garde-fou non tenu SHALL empêcher toute désactivation. La désactivation SHALL suivre la séquence de `design.md` (D6) : composants actifs désactivés un par un dans l'ordre de l'ensemble, chacun relu, puis vérification de l'écran d'accueil effectif ; un échec, un délai ou une vérification négative SHALL remettre à l'état par défaut, dans l'ordre inverse, les composants désactivés par cette opération, puis relire. SygixOs SHALL ne jamais laisser l'appareil sans écran d'accueil activé.

#### Scenario: confirmation
- **WHEN** la configuration automatique propose la désactivation
- **THEN** le dialogue « Désactiver l'accueil Google TV ? » s'affiche avec son texte, « Désactiver » et « Annuler », « Annuler » ayant le focus ; aucun composant n'est modifié avant OK sur « Désactiver »

#### Scenario: désactivation réussie
- **WHEN** l'utilisateur presse OK sur « Désactiver » et que chaque composant actif est désactivé puis que SygixOs est l'écran d'accueil effectif
- **THEN** le dialogue se ferme, l'état relu de chaque composant est « désactivé par l'utilisateur » et la configuration est terminée

#### Scenario: désactivation partielle annulée
- **WHEN** la désactivation d'un composant échoue, dépasse son délai, ou que SygixOs n'est pas l'écran d'accueil effectif après la désactivation
- **THEN** les composants désactivés par cette opération sont remis à leur état par défaut dans l'ordre inverse, l'état est relu, aucun succès n'est affiché et les solutions de repli sont posées

#### Scenario: appareil pas encore configuré
- **WHEN** la configuration initiale de l'appareil n'est pas terminée
- **THEN** aucune désactivation n'est proposée ni exécutée

#### Scenario: composant hors de portée
- **WHEN** un composant de l'ensemble est désactivé par le système ou jusqu'à utilisation, ou protégé
- **THEN** il n'est jamais modifié ; s'il ne reste aucun composant actif et modifiable, la désactivation n'est pas proposée

### Requirement: Rétablissement de l'accueil d'origine
La page « Configuration automatique » SHALL proposer « Rétablir l'accueil Google TV » quand au moins un composant de l'accueil d'origine est désactivé par l'utilisateur ou que SygixOs a défini l'écran d'accueil et relevé un écran d'accueil précédent. Sur OK, accès prêt (sinon « Guide d'activation du débogage »), SygixOs SHALL afficher la confirmation « Rétablir l'accueil Google TV ? » (`design.md`, décision 10), dialogue centré avec « Rétablir » focalisé au départ, mêmes règles de navigation que la confirmation de désactivation, puis SHALL remettre à l'état par défaut les composants désactivés par l'utilisateur dans l'ordre inverse de l'ensemble et, quand un écran d'accueil précédent a été relevé et qu'il est présent et activé, le redéfinir comme écran d'accueil ; tout est relu. Les autorisations accordées et le service d'accessibilité ne sont pas modifiés par le rétablissement. Un échec SHALL être affiché avec l'effet manquant, sans succès supposé, et laisser l'état relu rétablissable.

#### Scenario: rétablissement réussi
- **WHEN** l'utilisateur confirme « Rétablir » et que les composants sont remis à leur état par défaut et l'écran d'accueil précédent redéfini
- **THEN** la page affiche « L'accueil Google TV est rétabli. », l'état de la voie est recalculé, « Rétablir l'accueil Google TV » disparaît et le focus va à l'action principale

#### Scenario: rétablissement annulé
- **WHEN** l'utilisateur presse « Annuler » ou Retour dans la confirmation
- **THEN** rien n'est modifié et le focus revient sur « Rétablir l'accueil Google TV »

#### Scenario: sans écran d'accueil précédent relevé
- **WHEN** l'accueil d'origine a été désactivé par la procédure ADB du README et qu'aucun écran d'accueil précédent n'a été relevé
- **THEN** seuls les composants sont remis à leur état par défaut, et le système réattribue l'écran d'accueil

#### Scenario: échec du rétablissement
- **WHEN** la remise à l'état par défaut d'un composant échoue ou dépasse son délai
- **THEN** la page affiche « L'accueil Google TV n'a pas pu être rétabli. », l'état relu est affiché et « Rétablir l'accueil Google TV » reste proposé

### Requirement: Démarrage automatique du constructeur
Dans la configuration automatique, SygixOs SHALL accorder à son seul paquet l'autorisation de démarrage automatique propre au constructeur quand l'appareil en déclare une (opération d'application dont le nom est arrêté par la tâche 1.4, parmi `AUTO_START` et `APP_AUTO_START`), puis enregistrer les réglages d'opérations pour qu'elle persiste, et la relire. Déjà accordée, SygixOs SHALL ne rien écrire. Absente, l'étape est sans objet. Un refus, un délai ou une relecture qui ne la confirme pas SHALL ne jamais produire de succès et ne SHALL entraîner aucune nouvelle tentative sans action. L'autorisation SHALL ne jamais être présentée comme une garantie d'ouverture au démarrage.

#### Scenario: autorisation accordée
- **WHEN** l'opération existe avec une valeur autre que l'autorisation et que la séquence l'accorde
- **THEN** la relecture la donne autorisée et l'opération est notée comme faite dans « Détails »

#### Scenario: déjà accordée
- **WHEN** l'opération est déjà autorisée pour SygixOs
- **THEN** aucune écriture n'a lieu

#### Scenario: relecture négative
- **WHEN** l'écriture semble réussir mais que la relecture ne donne pas l'opération autorisée
- **THEN** l'étape est en échec, la séquence continue avec l'écran d'accueil et l'échec figure dans le résultat et dans « Détails »

### Requirement: Opérations privilégiées limitées
SygixOs SHALL n'exécuter par le débogage que les opérations fixes de `design.md` (D2), chacune avec des arguments fixes visant uniquement le paquet SygixOs, son service d'accessibilité ou un composant de l'ensemble fermé de l'accueil d'origine ; toute autre cible SHALL être refusée avant envoi. SygixOs SHALL ne jamais exécuter de commande saisie ou reçue de l'extérieur, ne SHALL révoquer aucune autorisation, et SHALL conserver les autres services d'accessibilité activés. Aucune connexion privilégiée SHALL ne rester ouverte hors d'une séquence demandée par l'utilisateur.

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
La page « Configuration automatique » et les confirmations SHALL proposer un lien discret « Détails » qui ouvre, dans le panneau, une liste non focalisable de ce que SygixOs a modifié ou va modifier, en clair puis avec le nom technique : autorisation de démarrage du constructeur (nom de l'opération et valeur relue), écran d'accueil défini (composant de SygixOs, écran d'accueil précédent relevé), composants de l'accueil d'origine (nom de paquet et état relu), affichage superposé, service d'accessibilité, option de démarrage à l'allumage, et les fonctions perdues quand l'accueil d'origine est désactivé (choix du profil, panneau de réglages rapides, guide des programmes). Une modification pas encore faite SHALL être présentée comme à venir, une modification en échec comme telle. Retour SHALL fermer la liste et rendre le focus à « Détails ».

#### Scenario: détails après configuration
- **WHEN** l'utilisateur ouvre « Détails » après une configuration terminée avec désactivation
- **THEN** la liste montre l'autorisation de démarrage, l'écran d'accueil défini et le précédent, chaque composant désactivé avec son paquet, et les fonctions perdues ; Retour rend le focus à « Détails »

#### Scenario: détails depuis la confirmation
- **WHEN** l'utilisateur ouvre « Détails » depuis la confirmation de désactivation
- **THEN** les composants à désactiver sont présentés comme à venir, avec leurs paquets et les fonctions qui seront perdues ; Retour rend le focus au lien dans la confirmation, sans la fermer

### Requirement: Débogage après la configuration
Après une configuration terminée ou un repli posé, la page « Configuration automatique » SHALL afficher « Vous pouvez désactiver le débogage : SygixOs n'en a plus besoin. Pour rétablir l'accueil Google TV depuis SygixOs, il faudra le réactiver. » et proposer « Ouvrir les options pour les développeurs », placé après « Détails » dans l'ordre de focus. SygixOs SHALL ne jamais désactiver le débogage lui-même.

#### Scenario: conseil après succès
- **WHEN** la configuration se termine et que le débogage est toujours activé
- **THEN** le conseil est affiché et OK sur « Ouvrir les options pour les développeurs » ouvre cet écran système ; au retour, l'état est relu et le conseil disparaît si le débogage est désactivé

## MODIFIED Requirements

### Requirement: Rôle d'écran d'accueil
SygixOs SHALL rester candidat au rôle système d'écran d'accueil (HOME). SygixOs SHALL permettre à l'utilisateur d'en demander l'attribution par le mécanisme officiel d'Android ou, si le rôle est indisponible ou que sa demande ne peut pas s'ouvrir, par l'écran système des applications par défaut ou de l'écran d'accueil quand l'appareil en a un, après une action explicite de sa part sur l'étape 3 « Définir SygixOs par défaut » de la page « Par autorisations » du parcours guidé (« Parcours guidé du launcher ») ; la configuration automatique SHALL aussi l'attribuer par le débogage après l'action de l'utilisateur (« Configuration automatique de l'écran d'accueil »). SygixOs SHALL ne jamais demander ni modifier ce rôle sans l'une de ces actions, et SHALL afficher l'état réellement attribué, relu auprès du système à chaque retour au premier plan. L'étape 3 est facultative : elle ne change pas la touche Home sur certaines TV, et elle SHALL être masquée quand le rôle est indisponible et qu'aucun écran système de remplacement n'existe.

#### Scenario: demande après action explicite
- **WHEN** le rôle n'est pas détenu par SygixOs et que l'utilisateur valide l'étape 3 de la page « Par autorisations »
- **THEN** le dialogue système de demande du rôle s'ouvre et l'étape passe à « En attente… » ; aucune demande n'a lieu sans cette validation ou sans la configuration automatique

#### Scenario: rôle déjà détenu
- **WHEN** le rôle HOME est déjà attribué à SygixOs et que l'utilisateur valide l'étape 3
- **THEN** l'étape reste « Fait » et aucun dialogue ni écran système n'est ouvert

#### Scenario: attribution acceptée
- **WHEN** l'utilisateur accepte l'attribution dans le dialogue système
- **THEN** au retour au premier plan, SygixOs relit le rôle et l'étape 3 affiche « Fait »

#### Scenario: attribution refusée
- **WHEN** l'utilisateur refuse ou ferme le dialogue système
- **THEN** au retour au premier plan, l'étape 3 affiche « Facultatif », le launcher par défaut n'est pas modifié et SygixOs reste utilisable

#### Scenario: rôle indisponible, écran des applications par défaut présent
- **WHEN** le système déclare le rôle indisponible, ou que le dialogue de demande ne peut pas s'ouvrir, et que l'appareil a un écran système des applications par défaut ou de l'écran d'accueil
- **THEN** l'étape 3 est affichée et sa validation ouvre cet écran système ; au retour au premier plan, l'état du rôle est relu et affiché, et SygixOs reste utilisable

#### Scenario: écran de demande refermé aussitôt
- **WHEN** l'écran système de demande du rôle se referme aussitôt sans interface (refus répété mémorisé par le système, résultat d'annulation moins de 700 ms après son lancement) et que le rôle n'est pas détenu
- **THEN** SygixOs le traite comme une demande impossible à ouvrir : il ouvre l'écran système des applications par défaut ou de l'écran d'accueil quand l'appareil en a un, sinon l'étape 3 est masquée

#### Scenario: rôle indisponible, sans écran de remplacement
- **WHEN** le système déclare le rôle indisponible, ou que le dialogue de demande ne peut pas s'ouvrir, et que l'appareil n'a aucun écran système des applications par défaut ou de l'écran d'accueil
- **THEN** l'étape 3 n'est pas affichée, aucun écran système n'est ouvert, sans crash, et les autres étapes restent utilisables

#### Scenario: rôle changé hors de SygixOs
- **WHEN** SygixOs revient au premier plan après que l'utilisateur a attribué le rôle HOME à une autre app ou à SygixOs dans les réglages système
- **THEN** l'état affiché est le rôle courant relu auprès du système, sans valeur conservée d'avant la sortie

#### Scenario: rôle attribué par la configuration automatique
- **WHEN** la configuration automatique définit SygixOs comme écran d'accueil
- **THEN** au retour au premier plan, l'étape 3 de la page « Par autorisations » affiche « Fait » si le rôle relu est détenu par SygixOs

### Requirement: Parcours guidé du launcher
SygixOs SHALL proposer un parcours guidé « Faire de SygixOs l'écran d'accueil », panneau de verre centré par-dessus un voile, affiché automatiquement au premier lancement où il n'a encore jamais été fermé, y compris pour une installation existante mise à jour qui ne l'a jamais fermé (le parcours garde le drapeau de fermeture de v0.1.0-rc.2), après la fin de l'écran de démarrage (« Écran de démarrage ») et après la réponse à la demande de permission « Programmes TV » (`READ_TV_LISTINGS`) quand celle-ci est affichée, jamais par-dessus un dialogue système ; il SHALL aussi s'ouvrir depuis la ligne « Configurer SygixOs comme launcher principal » des réglages (« Réglages du launcher système » de `settings`), par-dessus la page de réglages. Le panneau SHALL afficher une page à la fois, dans le même panneau : la page de choix, la page « Configuration automatique » (« Configuration automatique de l'écran d'accueil ») avec sa sous-page « Activer le débogage » (« Guide d'activation du débogage ») et sa liste « Détails », et la page « Par autorisations ». Toutes les pages SHALL commencer par l'en-tête du parcours : mascotte fixe de l'écran de démarrage à gauche du titre « Faire de SygixOs l'écran d'accueil ».
La page de choix SHALL afficher, sous l'en-tête, l'introduction « SygixOs peut remplacer l'accueil Google TV. Choisissez comment : », puis deux options, chacune avec un titre, un détail et son état à droite : « Configuration automatique », détail « Recommandé : activez une fois le débogage, SygixOs s'occupe du reste. », état de la voie automatique ; « Par autorisations », détail « Sans débogage : autorisez SygixOs dans les réglages Android. Sur certaines TV, après un redémarrage complet, il faut appuyer une fois sur Home. », état de synthèse de cette voie (« Terminé », « Partiel », « À faire », « Bloqué » ou « Indisponible », calculé comme dans « Réglages du launcher système » de `settings`) ; puis un pied avec l'aide « Retour : fermer » et le bouton « Plus tard » quand le parcours est affiché automatiquement, « Fermer » quand il est ouvert depuis les réglages. OK sur une option SHALL afficher sa page.
La page « Par autorisations » SHALL afficher, sous l'en-tête, le texte « Activez les étapes qui vous conviennent : chacune s'ouvre dans les réglages Android, puis revient ici. Sur certaines TV, après un redémarrage complet, il faut appuyer une fois sur Home. », la progression « X étape sur Y terminée » ou « X étapes sur Y terminées », les étapes, puis un pied avec l'aide « Retour : revenir au choix » et le bouton « Terminé ». Les étapes, dans cet ordre, SHALL être : 1 « Revenir à SygixOs avec la touche Home » (service d'accessibilité, « Retour Home par le service d'accessibilité ») ; 2 « Ouvrir SygixOs à l'allumage », avec un switch (« Démarrage à l'allumage ») ; 2b « Autoriser l'ouverture au démarrage », sous-étape en retrait affichée sous l'étape 2 seulement quand l'option est activée et que l'écran système de la permission d'affichage superposé existe (« Permission d'affichage superposé pour le démarrage ») ; 2c « Autoriser le démarrage dans les réglages de la TV », sous-étape en retrait affichée sous l'étape 2 seulement quand l'option est activée et que l'appareil déclare un écran d'auto-démarrage du constructeur retenu par `design.md` (D8), dont OK ouvre cet écran et dont l'état reste « À faire » tant qu'aucune ouverture au démarrage n'a été observée depuis son ouverture, « Fait » ensuite ; 3 « Définir SygixOs par défaut », facultative, affichée selon « Rôle d'écran d'accueil ». Chaque étape SHALL afficher à droite son état : « À faire », « En attente… » (écran système ouvert, ou vérification en cours), « Fait », « Bloqué » (étape 1 seulement), « Indisponible » (raison à la place du détail) ; l'étape 3 affiche « Facultatif » tant qu'elle n'est pas faite, et l'étape 2 son switch. Une étape faite SHALL remplacer son numéro par une coche ; l'étape 2 est faite quand l'option est activée et que les sous-étapes affichées sont « Fait ». La progression SHALL compter les étapes obligatoires affichées (1, 2, 2b et 2c) qui ne sont pas « Indisponible », et parmi elles celles qui sont « Fait » ; l'étape 3 n'est jamais comptée, et la progression n'est pas affichée quand aucune étape obligatoire n'est comptée. Cette page SHALL ne rien activer sans action explicite.
Navigation D-pad, sur toutes les pages : le focus SHALL rester dans le panneau tant qu'il est affiché ; gauche et droite sont sans effet sauf dans les dialogues et le guide du débogage, qui ont leurs propres règles ; haut et bas parcourent les éléments focalisables de la page dans l'ordre, sans boucle. Page de choix : focus initial sur « Configuration automatique », ou sur l'option dont la page vient d'être quittée par Retour ; ordre « Configuration automatique », « Par autorisations », bouton du pied. Page « Par autorisations » : focus initial sur la première étape obligatoire qui n'est pas « Fait », sinon sur « Terminé » ; ordre des étapes affichées puis « Terminé » ; OK sur une étape déclenche son action ; au retour d'un écran système, le panneau est toujours affiché sur cette page, les états sont relus et le focus SHALL être sur l'étape qui a ouvert cet écran. Retour sur une page autre que la page de choix revient à la page de choix ; Retour sur la page de choix, OK sur « Plus tard », « Fermer » ou « Terminé », et la touche Home ferment le parcours. Affiché automatiquement, une fois fermé quel qu'en soit le moyen, il SHALL ne plus jamais s'afficher automatiquement et l'accueil reprend avec le focus sur le héro ; ouvert depuis les réglages, sa fermeture rend le focus à la ligne « Configurer SygixOs comme launcher principal ». Le rendu (dimensions, jetons, états) est fixé par la rangée « 8. Launcher principal » de la maquette Penpot et `design.md` (D7).

#### Scenario: premier lancement
- **WHEN** SygixOs se lance et que le parcours guidé n'a jamais été fermé, sur une TV où rien n'est encore fait
- **THEN** à la fin de l'écran de démarrage, le parcours s'affiche sur l'accueil sur la page de choix : en-tête avec la mascotte à gauche du titre, introduction, « Configuration automatique » avec l'état « À faire » et « Par autorisations » avec l'état « À faire », puis l'aide « Retour : fermer » et « Plus tard » ; « Configuration automatique » a le focus

#### Scenario: navigation dans la page de choix
- **WHEN** l'utilisateur presse bas trois fois, puis haut trois fois, puis gauche et droite
- **THEN** le focus passe de « Configuration automatique » à « Par autorisations », au bouton du pied, y reste (sans boucle), remonte jusqu'à « Configuration automatique » et y reste ; gauche et droite ne le déplacent pas et aucun élément de l'accueil ou des réglages ne prend le focus

#### Scenario: choix d'une voie et retour
- **WHEN** l'utilisateur presse OK sur « Par autorisations », puis Retour
- **THEN** la page « Par autorisations » s'affiche dans le même panneau avec son focus initial ; Retour revient à la page de choix avec le focus sur « Par autorisations » et les états recalculés

#### Scenario: parcours ignoré
- **WHEN** l'utilisateur presse OK sur « Plus tard » ou Retour sur la page de choix d'un parcours affiché automatiquement, sans rien avoir validé
- **THEN** le parcours se ferme, il est enregistré comme fermé, l'accueil reprend avec le focus sur le héro, aucune connexion au débogage n'a eu lieu, le rôle HOME n'est pas demandé, l'option de démarrage reste désactivée et aucun écran système n'est ouvert

#### Scenario: navigation dans le parcours
- **WHEN** la page affiche les étapes 1, 2 et 3 et que l'utilisateur presse bas quatre fois, puis haut quatre fois, puis gauche et droite
- **THEN** le focus passe de l'étape 1 à l'étape 2, à l'étape 3, à « Terminé », puis y reste (sans boucle) ; il remonte jusqu'à l'étape 1 et y reste ; gauche et droite ne le déplacent pas

#### Scenario: actions des étapes
- **WHEN** l'utilisateur presse OK sur l'étape 1, sur l'étape 2, sur la sous-étape 2b, sur la sous-étape 2c ou sur l'étape 3
- **THEN** l'étape 1 ouvre les réglages d'accessibilité du système et passe à « En attente… » ; l'étape 2 bascule son switch et persiste l'option aussitôt, le focus restant sur elle ; la sous-étape 2b ouvre l'écran système de la permission d'affichage superposé et passe à « En attente… » ; la sous-étape 2c ouvre l'écran d'auto-démarrage du constructeur ; l'étape 3 demande le rôle HOME (« Rôle d'écran d'accueil ») ; le parcours reste affiché sur la page dans tous les cas

#### Scenario: sous-étape 2b
- **WHEN** l'utilisateur active l'étape 2 alors que la permission d'affichage superposé manque et que son écran système existe
- **THEN** la sous-étape 2b « Autoriser l'ouverture au démarrage » apparaît sous l'étape 2, en retrait, avec l'état « À faire », sans autre panneau ni confirmation ; le focus reste sur l'étape 2 et la progression compte désormais 2b ; désactiver l'étape 2 fait disparaître 2b

#### Scenario: sous-étape 2c
- **WHEN** l'utilisateur active l'étape 2 sur un appareil qui déclare un écran d'auto-démarrage du constructeur retenu
- **THEN** la sous-étape 2c « Autoriser le démarrage dans les réglages de la TV » apparaît sous l'étape 2 (après 2b si elle est affichée), en retrait, avec l'état « À faire » ; sur un appareil sans écran retenu, elle n'apparaît pas et le détail de l'étape 2 invite à chercher un réglage de démarrage automatique des applications dans les réglages de la TV

#### Scenario: retour d'un écran système ouvert depuis le parcours
- **WHEN** l'utilisateur a ouvert les réglages d'accessibilité, l'écran de la permission, l'écran du constructeur ou le dialogue du rôle HOME depuis la page « Par autorisations », puis revient dans SygixOs
- **THEN** le parcours est toujours affiché sur cette page, l'état de l'étape est relu auprès du système (plus jamais « En attente… » d'un écran fermé), la progression est recalculée et le focus est sur l'étape qui a ouvert l'écran système

#### Scenario: parcours terminé
- **WHEN** les étapes obligatoires affichées sont toutes « Fait »
- **THEN** chacune porte une coche à la place de son numéro et l'état « Fait », la progression indique que toutes sont terminées, l'étape 3 reste « Facultatif » si le rôle n'est pas détenu, le focus initial d'une nouvelle ouverture de la page est sur « Terminé » et l'option « Par autorisations » de la page de choix affiche « Terminé »

#### Scenario: fermeture par Terminé
- **WHEN** l'utilisateur presse OK sur « Terminé », quels que soient les états des étapes
- **THEN** le parcours disparaît ; affiché automatiquement, il est enregistré comme fermé et l'accueil reprend avec le focus sur le héro ; ouvert depuis les réglages, le focus revient sur « Configurer SygixOs comme launcher principal » ; les choix déjà faits restent tels quels

#### Scenario: demande de permission au premier lancement
- **WHEN** au premier lancement, la demande de permission « Programmes TV » est affichée à la fin de l'écran de démarrage
- **THEN** le parcours ne s'affiche pas tant que ce dialogue est à l'écran ; il s'affiche dès que l'utilisateur a accordé ou refusé la permission

#### Scenario: installation existante mise à jour
- **WHEN** SygixOs se lance pour la première fois après la mise à jour d'une installation existante
- **THEN** le parcours s'affiche une fois sur la page de choix, avec les états courants (par exemple « Par autorisations » en « Partiel »), seulement s'il n'avait jamais été fermé ; une installation qui l'avait fermé ne le revoit pas automatiquement

#### Scenario: pas de nouvel affichage automatique
- **WHEN** SygixOs se lance de nouveau après la fermeture du parcours affiché automatiquement, y compris après un redémarrage de l'appareil
- **THEN** le parcours ne s'affiche pas ; il reste accessible depuis la ligne « Configurer SygixOs comme launcher principal » des réglages

#### Scenario: ouverture depuis les réglages
- **WHEN** l'utilisateur presse OK sur « Configurer SygixOs comme launcher principal » dans la catégorie « Écran d'accueil »
- **THEN** le parcours s'affiche par-dessus la page de réglages sur la page de choix, avec les états courants des deux voies et « Fermer » dans le pied ; Retour ou « Fermer » le ferment et rendent le focus à la ligne, sans modifier l'état « fermé » du parcours automatique

#### Scenario: étape indisponible
- **WHEN** l'étape 1 ou l'étape 2 est « Indisponible » (réglages d'accessibilité impossibles à ouvrir, ou écran de la permission d'affichage superposé absent ou impossible à ouvrir avec l'option désactivée)
- **THEN** l'étape reste affichée et focalisable avec l'état « Indisponible », son titre atténué hors focus et la raison à la place du détail ; OK n'ouvre rien et ne change rien ; elle n'est pas comptée dans la progression ; les autres étapes et « Terminé » restent utilisables

#### Scenario: processus arrêté pendant un écran système
- **WHEN** le système arrête le processus de SygixOs pendant qu'un écran système ouvert depuis le parcours affiché automatiquement est affiché, puis SygixOs se relance
- **THEN** le parcours, jamais fermé, s'affiche de nouveau sur la page de choix avec les états relus

### Requirement: Démarrage à l'allumage
SygixOs SHALL proposer une option de démarrage à l'allumage, l'étape 2 « Ouvrir SygixOs à l'allumage » de la page « Par autorisations » du parcours guidé, désactivée tant que l'utilisateur ne l'a pas activée ou que la configuration automatique ne l'a pas activée comme solution de repli (« Configuration automatique de l'écran d'accueil »). Option activée, SygixOs SHALL demander l'ouverture de son accueil par les seuls mécanismes autorisés par Android, sans contourner ses restrictions, par deux chemins : la réception de la fin du démarrage du système, et la connexion de son service d'accessibilité peu après le démarrage (moins de 5 minutes après lui, valeur à valider sur la TV), quand le service est activé. Le premier chemin qui aboutit SHALL ouvrir SygixOs ; SygixOs SHALL s'ouvrir une seule fois par démarrage, l'autre chemin ne faisant rien si l'ouverture a déjà été observée. Une connexion du service plus tardive (activation ou réactivation) SHALL ne rien ouvrir. SygixOs SHALL enregistrer, pour chaque démarrage de l'appareil, si cette ouverture a été observée ; l'état « démarrage automatique non observé » qui en découle remplace le détail de l'étape 2 par « SygixOs ne s'est pas ouvert automatiquement à ce démarrage », le switch restant activé. Option désactivée, SygixOs SHALL ne demander aucune ouverture au démarrage. L'option ne vise que le démarrage à froid de l'appareil ; la sortie de veille n'est pas couverte par cette exigence.

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
- **WHEN** le service d'accessibilité se connecte plus de 5 minutes après le démarrage (activation ou réactivation), option activée
- **THEN** aucune ouverture au démarrage n'est demandée

#### Scenario: option désactivée
- **WHEN** l'appareil démarre et que l'option est désactivée
- **THEN** SygixOs ne demande pas son ouverture, ni par le récepteur ni par le service, et n'enregistre aucun échec

#### Scenario: fin du démarrage non reçue
- **WHEN** l'option était activée avant le démarrage, que le système ou le constructeur ne transmet pas la fin du démarrage à SygixOs et que le service d'accessibilité ne se connecte pas non plus
- **THEN** aucune ouverture n'est observée pour ce démarrage ; au lancement suivant de SygixOs, l'état « démarrage automatique non observé » est affiché par l'étape 2

#### Scenario: ouverture refusée par le système
- **WHEN** l'option était activée avant le démarrage, qu'un chemin d'ouverture est déclenché mais qu'Android refuse l'ouverture de l'activité
- **THEN** aucune ouverture n'est observée par ce chemin, aucun crash n'a lieu, l'autre chemin reste possible, et au lancement suivant l'état « démarrage automatique non observé » est affiché si aucune ouverture n'a été observée

#### Scenario: option activée après le démarrage
- **WHEN** l'option est activée, par l'utilisateur ou par la configuration automatique, alors que l'appareil a déjà démarré
- **THEN** le démarrage courant n'est pas compté comme un échec ; seul le prochain démarrage est évalué

#### Scenario: option activée par la configuration automatique
- **WHEN** la configuration automatique pose les solutions de repli
- **THEN** l'option est persistée activée et l'étape 2 de la page « Par autorisations » affiche son switch activé ; l'utilisateur peut la désactiver comme une option qu'il a activée

### Requirement: Permission d'affichage superposé pour le démarrage
SygixOs SHALL proposer la permission système « Afficher par-dessus d'autres applis » pour permettre le démarrage hors des autres exemptions Android, par la sous-étape 2b « Autoriser l'ouverture au démarrage » de la page « Par autorisations », détail « Android l'exige : dans la liste, choisissez SygixOs puis activez « Afficher par-dessus d'autres applis ». », affichée sous l'étape 2 quand l'option de démarrage est activée et que l'écran système de la permission existe, sans panneau de confirmation intermédiaire ; la configuration automatique SHALL aussi l'accorder par le débogage comme solution de repli (« Configuration automatique de l'écran d'accueil »). La sous-étape SHALL afficher « À faire » tant que la permission manque, « En attente… » pendant l'écran système, « Fait » quand le système la déclare accordée. OK SHALL ouvrir l'écran système de la permission pour SygixOs ; hors de la configuration automatique, SygixOs SHALL ne jamais accorder la permission lui-même ; il SHALL ne jamais la supposer accordée, et au retour au premier plan son état SHALL être relu auprès du système. Un refus de l'utilisateur SHALL ne pas bloquer l'utilisation de SygixOs ni effacer son choix de démarrage. Quand l'écran système de la permission n'existe pas sur l'appareil, ou ne peut pas s'ouvrir, l'étape 2 SHALL devenir « Indisponible », avec la raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage » à la place du détail, la sous-étape 2b SHALL disparaître et l'option SHALL ne pas s'activer : le passage de désactivé à activé est refusé, et une option activée juste avant l'échec revient à désactivé ; SygixOs reste utilisable sans crash. Cet état dure jusqu'au prochain lancement du processus, sauf si le système déclare ensuite la permission accordée ; une option déjà activée auparavant reste désactivable. Une permission accordée SHALL ne pas être présentée comme une garantie d'ouverture au démarrage sur tous les appareils.

#### Scenario: sous-étape affichée à l'activation
- **WHEN** l'utilisateur fait passer l'étape 2 de désactivé à activé alors que la permission manque et que son écran existe
- **THEN** l'option est persistée activée, la sous-étape 2b apparaît avec « À faire », aucun écran système ne s'ouvre avant OK sur 2b et le focus reste sur l'étape 2

#### Scenario: ouverture de l'écran de la permission
- **WHEN** l'utilisateur presse OK sur la sous-étape 2b
- **THEN** l'écran système de la permission s'ouvre et 2b passe à « En attente… » ; au retour, 2b affiche « Fait » seulement si le système déclare la permission accordée, et le focus est sur 2b

#### Scenario: permission refusée
- **WHEN** l'utilisateur revient de l'écran système sans accorder la permission
- **THEN** SygixOs reste utilisable sans crash, ne suppose aucune permission accordée, conserve l'option activée et 2b affiche « À faire »

#### Scenario: permission déjà accordée
- **WHEN** l'option est activée alors que la permission est déjà accordée
- **THEN** la sous-étape 2b est affichée avec l'état « Fait »

#### Scenario: écran de la permission absent
- **WHEN** l'appareil n'a pas d'écran système pour la permission et que l'option de démarrage est désactivée
- **THEN** l'étape 2 est affichée « Indisponible », sans switch, avec la raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage » ; OK ne l'active pas, aucune sous-étape ni écran système n'apparaît et l'étape garde le focus

#### Scenario: écran de la permission impossible à ouvrir
- **WHEN** l'utilisateur valide la sous-étape 2b et que l'écran système ne peut pas s'ouvrir
- **THEN** la sous-étape disparaît, l'option revient à désactivé et l'étape 2 devient « Indisponible » avec sa raison, focalisée ; une nouvelle activation est refusée jusqu'au prochain lancement, sauf si la permission est ensuite déclarée accordée

#### Scenario: permission accordée par la configuration automatique
- **WHEN** la configuration automatique accorde la permission comme solution de repli
- **THEN** la permission est relue auprès du système, et la sous-étape 2b affiche « Fait » seulement si le système la déclare accordée

### Requirement: Retour Home par le service d'accessibilité
SygixOs SHALL fournir un service d'accessibilité qui, une fois activé dans les réglages d'accessibilité du système, ramène SygixOs au premier plan sur son accueil à chaque appui sur la touche Home. Le service SHALL être activé soit par l'utilisateur, l'étape 1 de la page « Par autorisations » ouvrant seulement les réglages d'accessibilité du système, soit par la configuration automatique comme solution de repli, par le débogage et en conservant les autres services activés (« Configuration automatique de l'écran d'accueil ») ; hors de ces cas, SygixOs SHALL ne jamais activer ni désactiver ce service lui-même. Après un refus, le service SHALL n'être proposé de nouveau que par le parcours guidé ouvert depuis les réglages, sans relance automatique. Le service SHALL observer uniquement la touche Home via `AccessibilityService.onKeyEvent`, sans lecture du contenu des fenêtres, et ne rien conserver. Tant que la redirection Home est active, c'est-à-dire tant que le service est connecté, il SHALL consommer tous les événements de la touche Home (appui, répétitions et relâchement), pour que le système et le launcher du constructeur ne la reçoivent jamais, et SHALL ouvrir SygixOs aussitôt au relâchement non annulé, sans délai ; un appui long SHALL être traité comme un appui court. Service non activé, désactivé ou arrêté, aucune touche n'est consommée et SygixOs SHALL rester pleinement utilisable. Quand le service est activé dans les réglages du système mais n'est pas connecté, au lancement du processus SygixOs ou au retour au premier plan, SygixOs SHALL attendre sa connexion pendant un délai borné, sans jamais tenter de le faire relier lui-même ni toucher au service ou aux réglages du système. L'étape 1 SHALL afficher « Fait » quand le service est activé et connecté, « À faire » quand il n'est pas activé, « En attente… » pendant les réglages d'accessibilité ouverts et tant que cette attente n'a pas conclu, « Bloqué » quand il est activé mais toujours non connecté au terme de l'attente, jusqu'à sa connexion ou sa désactivation, avec le détail « La TV a bloqué ce service au démarrage. Désactivez-le puis réactivez-le dans Accessibilité ; la configuration automatique peut aussi autoriser le démarrage de SygixOs. », et « Indisponible » quand les réglages d'accessibilité ne peuvent pas s'ouvrir, avec la raison « Les réglages d'accessibilité ne peuvent pas s'ouvrir sur cette TV ». Le service SHALL être livré même si un appareil ne lui transmet pas la touche Home, et ne rien faire dans ce cas.

#### Scenario: activation par l'utilisateur
- **WHEN** l'utilisateur valide l'étape 1 de la page « Par autorisations »
- **THEN** les réglages d'accessibilité du système s'ouvrent ; au retour au premier plan, l'étape affiche « Fait » seulement si le système déclare le service activé et qu'il est connecté

#### Scenario: activation par la configuration automatique
- **WHEN** la configuration automatique active le service comme solution de repli
- **THEN** le service est ajouté à la liste des services activés sans retirer les autres, l'état est relu, et l'étape 1 affiche « Fait » quand le service est connecté

#### Scenario: service refusé
- **WHEN** l'utilisateur revient des réglages d'accessibilité sans avoir activé le service, ou ferme le parcours sans l'ouvrir
- **THEN** l'étape 1 affiche « À faire », SygixOs reste utilisable et aucune nouvelle proposition ne s'affiche automatiquement, à ce lancement comme aux suivants

#### Scenario: touche Home, SygixOs en arrière-plan
- **WHEN** le service est connecté, que SygixOs est en arrière-plan et que l'utilisateur presse Home
- **THEN** le service consomme l'appui et le relâchement, l'accueil du constructeur ne s'affiche à aucun moment, et SygixOs revient au premier plan sur son accueil dès le relâchement, selon les règles de retour existantes (« Écran de démarrage », scénario « retour sur le launcher »)

#### Scenario: touche Home, SygixOs au premier plan
- **WHEN** le service est connecté, que SygixOs est au premier plan et que l'utilisateur presse Home
- **THEN** la touche est consommée et SygixOs applique « Touche Home avec SygixOs au premier plan »

#### Scenario: appui long sur Home
- **WHEN** le service est connecté et que l'utilisateur maintient la touche Home (répétitions ou maintien au-delà du délai d'appui long)
- **THEN** tous les événements sont consommés, aucun comportement système d'appui long (tableau de bord Google TV) ne s'applique, et SygixOs s'ouvre au relâchement comme pour un appui court

#### Scenario: relâchement annulé
- **WHEN** le système annule l'appui sur Home en cours (relâchement marqué annulé)
- **THEN** l'événement est consommé et SygixOs n'est pas ouvert

#### Scenario: signal Home absent
- **WHEN** l'appareil ne transmet aucune touche Home au service, même connecté
- **THEN** le service reste livré, ne lit aucun contenu de fenêtre, ne demande aucune ouverture et laisse le comportement système inchangé

#### Scenario: service désactivé ou arrêté
- **WHEN** l'utilisateur désactive le service dans les réglages système, ou que le système l'arrête
- **THEN** aucune touche n'est consommée, l'appui sur Home suit le comportement du système, aucune erreur n'est affichée, et au retour au premier plan l'étape 1 affiche « À faire » (désactivé) ou suit le scénario « service bloqué » (activé mais arrêté)

#### Scenario: service non lié au lancement
- **WHEN** après un redémarrage complet, le gestionnaire d'auto-démarrage du constructeur a empêché la liaison du service activé, et que l'utilisateur lance SygixOs
- **THEN** l'étape 1 affiche « En attente… » pendant l'attente bornée, puis « Bloqué » si le service ne s'est pas connecté ; SygixOs ne demande aucune nouvelle liaison et ne modifie ni ses composants, ni le service, ni les réglages du système

#### Scenario: service bloqué
- **WHEN** le service est activé dans les réglages du système mais n'est toujours pas connecté au terme de l'attente bornée
- **THEN** l'étape 1 affiche « Bloqué » avec son explication, reste focalisable, et OK ouvre les réglages d'accessibilité du système ; la synthèse de « Par autorisations » est « Bloqué » ; quand l'utilisateur a désactivé puis réactivé le service et que le système le relie, l'étape 1 affiche « Fait » au retour au premier plan
