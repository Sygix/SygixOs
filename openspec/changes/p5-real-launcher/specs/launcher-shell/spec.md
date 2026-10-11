# Spec Delta

## ADDED Requirements

### Requirement: Rôle d'écran d'accueil
SygixOs SHALL rester candidat au rôle système d'écran d'accueil (HOME) et SHALL permettre à l'utilisateur d'en demander l'attribution par le mécanisme officiel d'Android ou, si le rôle est indisponible ou que sa demande ne peut pas s'ouvrir, par l'écran système des applications par défaut ou de l'écran d'accueil quand l'appareil en a un, uniquement après une action explicite de sa part sur l'étape 3 « Définir SygixOs par défaut » du parcours guidé (« Parcours guidé du launcher »). SygixOs SHALL ne jamais demander ni modifier ce rôle sans cette action, et SHALL afficher l'état réellement attribué, relu auprès du système à chaque retour au premier plan. L'étape 3 est facultative : elle ne change pas la touche Home sur certaines TV, et elle SHALL être masquée quand le rôle est indisponible et qu'aucun écran système de remplacement n'existe.

#### Scenario: demande après action explicite
- **WHEN** le rôle n'est pas détenu par SygixOs et que l'utilisateur valide l'étape 3 du parcours guidé
- **THEN** le dialogue système de demande du rôle s'ouvre et l'étape passe à « En attente… » ; aucune demande n'a lieu sans cette validation

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

### Requirement: Parcours guidé du launcher
SygixOs SHALL proposer un parcours guidé « Faire de SygixOs l'écran d'accueil », panneau de verre centré par-dessus un voile, affiché automatiquement au premier lancement où il n'a encore jamais été fermé, y compris pour une installation existante mise à jour qui n'avait jamais fermé la présentation initiale, après la fin de l'écran de démarrage (« Écran de démarrage ») et après la réponse à la demande de permission « Programmes TV » (`READ_TV_LISTINGS`) quand celle-ci est affichée, jamais par-dessus un dialogue système ; il SHALL aussi s'ouvrir depuis la ligne « SygixOs comme écran d'accueil » des réglages (« Réglages du launcher système » de `settings`), par-dessus la page de réglages. Il SHALL se composer d'un en-tête (mascotte fixe de l'écran de démarrage à gauche du titre « Faire de SygixOs l'écran d'accueil », texte d'introduction « SygixOs peut remplacer l'accueil Google TV. Activez les étapes qui vous conviennent : chacune s'ouvre dans les réglages Android, puis revient ici. », progression « X étape sur Y terminée » ou « X étapes sur Y terminées »), des étapes, puis d'un pied avec l'aide « Retour : fermer » et le bouton « Terminé ». Les étapes, dans cet ordre, SHALL être : 1 « Revenir à SygixOs avec la touche Home » (service d'accessibilité, « Retour Home par le service d'accessibilité ») ; 2 « Ouvrir SygixOs à l'allumage », avec un switch (« Démarrage à l'allumage ») ; 2b « Autoriser l'ouverture au démarrage », sous-étape en retrait affichée sous l'étape 2 seulement quand l'option est activée et que l'écran système de la permission d'affichage superposé existe (« Permission d'affichage superposé pour le démarrage ») ; 3 « Définir SygixOs par défaut », facultative, affichée selon « Rôle d'écran d'accueil ». Chaque étape SHALL afficher à droite son état : « À faire », « En attente… » (écran système ouvert, ou vérification en cours), « Fait », « Bloqué » (étape 1 seulement), « Indisponible » (raison à la place du détail) ; l'étape 3 affiche « Facultatif » tant qu'elle n'est pas faite, et l'étape 2 son switch. Une étape faite SHALL remplacer son numéro par une coche ; l'étape 2 est faite quand l'option est activée et que la sous-étape 2b est « Fait » ou n'est pas affichée. La progression SHALL compter les étapes obligatoires affichées (1, 2 et 2b) qui ne sont pas « Indisponible », et parmi elles celles qui sont « Fait » ; l'étape 3 n'est jamais comptée, et la progression n'est pas affichée quand aucune étape obligatoire n'est comptée. Le parcours SHALL ne rien activer sans action explicite et SHALL pouvoir être ignoré. Navigation D-pad : focus initial sur la première étape obligatoire qui n'est pas « Fait », sinon sur « Terminé » ; haut et bas parcourent les étapes affichées puis « Terminé », sans boucle ; gauche et droite sont sans effet ; le focus SHALL rester dans le panneau tant qu'il est affiché. OK sur une étape déclenche son action ; au retour d'un écran système, le panneau est toujours affiché, les états sont relus et le focus SHALL être sur l'étape qui a ouvert cet écran. OK sur « Terminé », Retour et la touche Home ferment le parcours. Affiché automatiquement, une fois fermé quel qu'en soit le moyen, il SHALL ne plus jamais s'afficher automatiquement et l'accueil reprend avec le focus sur le héro ; ouvert depuis les réglages, sa fermeture rend le focus à la ligne « SygixOs comme écran d'accueil ». Le rendu (dimensions, jetons, états) est fixé par les maquettes Penpot 8.1 à 8.4, 8.6 et 8.7 et `design.md` (D9).

#### Scenario: premier lancement
- **WHEN** SygixOs se lance et que le parcours guidé n'a jamais été fermé, sur une TV où rien n'est encore fait
- **THEN** à la fin de l'écran de démarrage, le parcours s'affiche sur l'accueil (maquette 8.1) : en-tête avec la mascotte à gauche du titre, introduction et « 0 étape sur 2 terminée », puis l'étape 1 « À faire », l'étape 2 avec son switch désactivé, l'étape 3 « Facultatif », sans sous-étape 2b, puis l'aide « Retour : fermer » et « Terminé » ; l'étape 1 a le focus

#### Scenario: navigation dans le parcours
- **WHEN** le parcours affiche les étapes 1, 2 et 3 et que l'utilisateur presse bas quatre fois, puis haut quatre fois, puis gauche et droite
- **THEN** le focus passe de l'étape 1 à l'étape 2, à l'étape 3, à « Terminé », puis y reste (sans boucle) ; il remonte jusqu'à l'étape 1 et y reste ; gauche et droite ne le déplacent pas et aucun élément de l'accueil ou des réglages ne prend le focus

#### Scenario: actions des étapes
- **WHEN** l'utilisateur presse OK sur l'étape 1, sur l'étape 2, sur la sous-étape 2b ou sur l'étape 3
- **THEN** l'étape 1 ouvre les réglages d'accessibilité du système et passe à « En attente… » ; l'étape 2 bascule son switch et persiste l'option aussitôt, le focus restant sur elle ; la sous-étape 2b ouvre l'écran système de la permission d'affichage superposé et passe à « En attente… » ; l'étape 3 demande le rôle HOME (« Rôle d'écran d'accueil ») ; le parcours reste affiché dans tous les cas

#### Scenario: sous-étape 2b
- **WHEN** l'utilisateur active l'étape 2 alors que la permission d'affichage superposé manque et que son écran système existe
- **THEN** la sous-étape 2b « Autoriser l'ouverture au démarrage » apparaît sous l'étape 2, en retrait, avec l'état « À faire », sans autre panneau ni confirmation ; le focus reste sur l'étape 2 et la progression compte désormais 2b (maquette 8.3 : « 1 étape sur 3 terminée ») ; désactiver l'étape 2 fait disparaître 2b

#### Scenario: retour d'un écran système ouvert depuis le parcours
- **WHEN** l'utilisateur a ouvert les réglages d'accessibilité, l'écran de la permission ou le dialogue du rôle HOME depuis le parcours, puis revient dans SygixOs
- **THEN** le parcours est toujours affiché, l'état de l'étape est relu auprès du système (plus jamais « En attente… » d'un écran fermé), la progression est recalculée et le focus est sur l'étape qui a ouvert l'écran système

#### Scenario: parcours terminé
- **WHEN** les étapes obligatoires affichées sont toutes « Fait »
- **THEN** chacune porte une coche à la place de son numéro et l'état « Fait », la progression indique que toutes sont terminées (maquette 8.4 : « 3 étapes sur 3 terminées »), l'étape 3 reste « Facultatif » si le rôle n'est pas détenu, et le focus initial d'une nouvelle ouverture est sur « Terminé »

#### Scenario: fermeture par Terminé
- **WHEN** l'utilisateur presse OK sur « Terminé », quels que soient les états des étapes
- **THEN** le parcours disparaît ; affiché automatiquement, il est enregistré comme fermé et l'accueil reprend avec le focus sur le héro ; ouvert depuis les réglages, le focus revient sur « SygixOs comme écran d'accueil » ; les choix déjà faits restent tels quels

#### Scenario: demande de permission au premier lancement
- **WHEN** au premier lancement, la demande de permission « Programmes TV » est affichée à la fin de l'écran de démarrage
- **THEN** le parcours ne s'affiche pas tant que ce dialogue est à l'écran ; il s'affiche dès que l'utilisateur a accordé ou refusé la permission

#### Scenario: installation existante mise à jour
- **WHEN** SygixOs se lance pour la première fois après la mise à jour d'une installation existante qui n'avait jamais fermé la présentation initiale
- **THEN** le parcours s'affiche une fois, avec les états courants (par exemple l'étape 3 « Fait » si SygixOs détient déjà le rôle HOME) ; une installation qui avait déjà fermé la présentation initiale ne le voit pas automatiquement

#### Scenario: parcours ignoré
- **WHEN** l'utilisateur ignore le parcours affiché automatiquement (« Terminé » ou Retour) sans rien valider
- **THEN** le parcours se ferme, l'accueil reprend avec le focus sur le héro, le rôle HOME n'est pas demandé, l'option de démarrage reste désactivée et aucun écran système n'est ouvert

#### Scenario: pas de nouvel affichage automatique
- **WHEN** SygixOs se lance de nouveau après la fermeture du parcours affiché automatiquement, y compris après un redémarrage de l'appareil
- **THEN** le parcours ne s'affiche pas ; il reste accessible depuis la ligne « SygixOs comme écran d'accueil » des réglages

#### Scenario: ouverture depuis les réglages
- **WHEN** l'utilisateur presse OK sur « SygixOs comme écran d'accueil » dans la catégorie « Écran d'accueil »
- **THEN** le parcours s'affiche par-dessus la page de réglages (maquette 8.6), avec les états courants et le focus initial de cette exigence ; Retour ou « Terminé » le ferment et rendent le focus à la ligne, sans modifier l'état « fermé » du parcours automatique

#### Scenario: étape indisponible
- **WHEN** l'étape 1 ou l'étape 2 est « Indisponible » (réglages d'accessibilité impossibles à ouvrir, ou écran de la permission d'affichage superposé absent ou impossible à ouvrir avec l'option désactivée)
- **THEN** l'étape reste affichée et focalisable avec l'état « Indisponible », son titre atténué hors focus et la raison à la place du détail ; OK n'ouvre rien et ne change rien ; elle n'est pas comptée dans la progression ; les autres étapes et « Terminé » restent utilisables

#### Scenario: processus arrêté pendant un écran système
- **WHEN** le système arrête le processus de SygixOs pendant qu'un écran système ouvert depuis le parcours affiché automatiquement est affiché, puis SygixOs se relance
- **THEN** le parcours, jamais fermé, s'affiche de nouveau avec les états relus

### Requirement: Démarrage à l'allumage
SygixOs SHALL proposer une option de démarrage à l'allumage, l'étape 2 « Ouvrir SygixOs à l'allumage » du parcours guidé, désactivée tant que l'utilisateur ne l'a pas activée. Option activée, SygixOs SHALL demander l'ouverture de son accueil par les seuls mécanismes autorisés par Android, sans contourner ses restrictions, par deux chemins : la réception de la fin du démarrage du système, et la connexion de son service d'accessibilité peu après le démarrage (moins de 5 minutes après lui, valeur à valider sur la TV), quand le service est activé par l'utilisateur. Le premier chemin qui aboutit SHALL ouvrir SygixOs ; SygixOs SHALL s'ouvrir une seule fois par démarrage, l'autre chemin ne faisant rien si l'ouverture a déjà été observée. Une connexion du service plus tardive (activation par l'utilisateur, réactivation au lancement) SHALL ne rien ouvrir. SygixOs SHALL enregistrer, pour chaque démarrage de l'appareil, si cette ouverture a été observée ; l'état « démarrage automatique non observé » qui en découle remplace le détail de l'étape 2 par « SygixOs ne s'est pas ouvert automatiquement à ce démarrage », le switch restant activé. Option désactivée, SygixOs SHALL ne demander aucune ouverture au démarrage. L'option ne vise que le démarrage à froid de l'appareil ; la sortie de veille n'est pas couverte par cette exigence.

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
- **WHEN** le service d'accessibilité se connecte plus de 5 minutes après le démarrage (activation par l'utilisateur, réactivation au lancement de SygixOs), option activée
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
- **WHEN** l'utilisateur active l'option alors que l'appareil a déjà démarré
- **THEN** le démarrage courant n'est pas compté comme un échec ; seul le prochain démarrage est évalué

### Requirement: Permission d'affichage superposé pour le démarrage
SygixOs SHALL proposer la permission système « Afficher par-dessus d'autres applis » pour permettre le démarrage hors des autres exemptions Android, par la sous-étape 2b « Autoriser l'ouverture au démarrage » du parcours guidé, détail « Android l'exige : dans la liste, choisissez SygixOs puis activez « Afficher par-dessus d'autres applis ». », affichée sous l'étape 2 quand l'option de démarrage est activée et que l'écran système de la permission existe, sans panneau de confirmation intermédiaire. La sous-étape SHALL afficher « À faire » tant que la permission manque, « En attente… » pendant l'écran système, « Fait » quand le système la déclare accordée. OK SHALL ouvrir l'écran système de la permission pour SygixOs ; SygixOs SHALL ne jamais accorder la permission lui-même ni la supposer accordée ; au retour au premier plan, son état SHALL être relu auprès du système. Un refus de l'utilisateur SHALL ne pas bloquer l'utilisation de SygixOs ni effacer son choix de démarrage. Quand l'écran système de la permission n'existe pas sur l'appareil, ou ne peut pas s'ouvrir, l'étape 2 SHALL devenir « Indisponible », avec la raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage » à la place du détail, la sous-étape 2b SHALL disparaître et l'option SHALL ne pas s'activer : le passage de désactivé à activé est refusé, et une option activée juste avant l'échec revient à désactivé ; SygixOs reste utilisable sans crash. Cet état dure jusqu'au prochain lancement du processus, sauf si le système déclare ensuite la permission accordée ; une option déjà activée auparavant reste désactivable. Une permission accordée SHALL ne pas être présentée comme une garantie d'ouverture au démarrage sur tous les appareils.

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

### Requirement: Retour Home par le service d'accessibilité
SygixOs SHALL fournir un service d'accessibilité qui, une fois activé par l'utilisateur dans les réglages d'accessibilité du système, ramène SygixOs au premier plan sur son accueil à chaque appui sur la touche Home. SygixOs SHALL ne jamais activer ni désactiver ce service lui-même : l'étape 1 du parcours guidé ouvre seulement les réglages d'accessibilité du système, quel que soit l'état du service. Après un refus, le service SHALL n'être proposé de nouveau que par le parcours guidé ouvert depuis les réglages, sans relance automatique. Le service SHALL observer uniquement la touche Home via `AccessibilityService.onKeyEvent`, sans lecture du contenu des fenêtres, et ne rien conserver. Tant que la redirection Home est active, c'est-à-dire tant que le service est connecté, il SHALL consommer tous les événements de la touche Home (appui, répétitions et relâchement), pour que le système et le launcher du constructeur ne la reçoivent jamais, et SHALL ouvrir SygixOs aussitôt au relâchement non annulé, sans délai ; un appui long SHALL être traité comme un appui court. Service non activé, désactivé ou arrêté, aucune touche n'est consommée et SygixOs SHALL rester pleinement utilisable. Au lancement du processus SygixOs, si le service est activé dans les réglages du système mais n'est pas connecté après un délai borné, SygixOs SHALL tenter une fois de le faire relier par le système, en basculant l'état d'un de ses propres composants inertes, sans jamais toucher au service ni aux réglages du système ; cette réactivation n'est livrée que si la tâche 5.8 de ce change la valide sur la TV réelle. L'étape 1 SHALL afficher « Fait » quand le service est activé et connecté, « À faire » quand il n'est pas activé, « En attente… » pendant les réglages d'accessibilité ouverts et tant que la vérification de lancement n'a pas conclu, « Bloqué » quand il est activé mais toujours non connecté après cette vérification, avec le détail « La TV a bloqué ce service au démarrage. Désactivez-le puis réactivez-le dans Accessibilité ; si la TV le propose, autorisez aussi le démarrage automatique de SygixOs. », et « Indisponible » quand les réglages d'accessibilité ne peuvent pas s'ouvrir, avec la raison « Les réglages d'accessibilité ne peuvent pas s'ouvrir sur cette TV ». Le service SHALL être livré même si un appareil ne lui transmet pas la touche Home, et ne rien faire dans ce cas.

#### Scenario: activation par l'utilisateur
- **WHEN** l'utilisateur valide l'étape 1 du parcours guidé
- **THEN** les réglages d'accessibilité du système s'ouvrent ; au retour au premier plan, l'étape affiche « Fait » seulement si le système déclare le service activé et qu'il est connecté

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

#### Scenario: réactivation au lancement
- **WHEN** après un redémarrage complet, le gestionnaire d'auto-démarrage du constructeur a empêché la liaison du service activé, et que l'utilisateur lance SygixOs
- **THEN** après un délai borné sans connexion, SygixOs bascule une fois l'état de son composant inerte ; si le service se connecte ensuite, l'étape 1 affiche « Fait » et la touche Home est de nouveau redirigée, sans action de l'utilisateur

#### Scenario: service bloqué
- **WHEN** le service est activé dans les réglages du système mais n'est toujours pas connecté après la vérification de lancement et sa réactivation éventuelle
- **THEN** l'étape 1 affiche « Bloqué » avec son explication (maquette 8.7), reste focalisable, et OK ouvre les réglages d'accessibilité du système ; la ligne des réglages indique « Touche Home bloquée par la TV »

### Requirement: Touche Home avec SygixOs au premier plan
Quand la touche Home atteint SygixOs alors qu'il est déjà au premier plan, ou quand le service d'accessibilité signale un appui sur Home alors que SygixOs était au premier plan à l'appui (« Retour Home par le service d'accessibilité »), SygixOs SHALL fermer entièrement la page de réglages en une seule fois, quel que soit l'élément ouvert ou focalisé dans les réglages (y compris la liste déroulante « Position d'À suivre » de `settings`, que Retour ne ferait que refermer), fermer toute surcouche de l'accueil comme le ferait Retour sur cette surcouche, y compris le parcours guidé, ramener la page de l'accueil en haut, donner le focus au héro et reprendre sa lecture. L'ouverture au démarrage (« Démarrage à l'allumage ») n'est jamais traitée comme une touche Home. Hors premier plan, les règles de retour existantes s'appliquent sans changement (« Écran de démarrage », scénario « retour sur le launcher »).

#### Scenario: depuis la grille
- **WHEN** le focus est dans la zone grille et que la touche Home atteint SygixOs
- **THEN** la page défile jusqu'au héro comme au scénario « retour depuis la grille » de « Navigation 3 paliers », le héro prend le focus et sa lecture reprend

#### Scenario: depuis les réglages
- **WHEN** la page de réglages est ouverte, quelle que soit la catégorie ou la ligne focalisée, et que la touche Home atteint SygixOs
- **THEN** les réglages se ferment en une fois, la page de l'accueil est en haut, le héro a le focus et sa lecture reprend

#### Scenario: depuis la liste déroulante « Position d'À suivre »
- **WHEN** la liste déroulante « Position d'À suivre » est ouverte dans les réglages et que la touche Home atteint SygixOs
- **THEN** la liste se ferme sans changer la valeur et les réglages se ferment dans le même temps, sans étape intermédiaire sur la ligne « Position d'À suivre » ; la page de l'accueil est en haut, le héro a le focus et sa lecture reprend ; à la réouverture des réglages, la liste est fermée et la valeur persistée inchangée

#### Scenario: depuis une surcouche
- **WHEN** le menu contextuel d'une tuile, le mode « Déplacer » ou le parcours guidé est affiché et que la touche Home atteint SygixOs
- **THEN** la surcouche se ferme comme par Retour (déplacement annulé ; parcours enregistré comme fermé s'il avait été affiché automatiquement), les réglages éventuellement ouverts dessous se ferment aussi, la page de l'accueil est en haut et le héro a le focus

#### Scenario: déjà sur le héro
- **WHEN** le héro a déjà le focus, sans surcouche, et que la touche Home atteint SygixOs
- **THEN** le héro garde le focus et rien d'autre ne change

#### Scenario: pendant l'écran de démarrage
- **WHEN** la touche Home atteint SygixOs pendant l'écran de démarrage
- **THEN** l'écran de démarrage se poursuit sans changement et le héro prend le focus à son fondu de sortie (« Écran de démarrage »)
