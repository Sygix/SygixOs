# Spec Delta

L'« accueil d'origine » désigne l'écran d'accueil fourni avec la TV et le composant système qui le réactive ; son ensemble fermé est défini par `design.md` (D4). Le contrôle, la confirmation et la navigation sont décrits par `settings` ; ce delta ne décrit que les effets sur le système.

## ADDED Requirements

### Requirement: État de Shizuku
SygixOs SHALL exposer l'état du service et de l'autorisation Shizuku, le relire au retour au premier plan et aux changements Binder, et ne SHALL jamais déclarer le service prêt avant réponse positive de l'API. Son absence, arrêt ou perte SHALL laisser SygixOs utilisable sans crash.

#### Scenario: lecture au lancement
- **WHEN** SygixOs démarre et que la réponse de Shizuku n'est pas encore arrivée
- **THEN** l'état est « en cours de lecture », jamais « arrêté » par défaut, et aucune action privilégiée n'est possible avant la première lecture

#### Scenario: Shizuku absent
- **WHEN** ni Shizuku ni Sui n'est disponible
- **THEN** l'état est « absent » et SygixOs reste utilisable

#### Scenario: Shizuku installé mais arrêté
- **WHEN** Shizuku est installé mais son service n'est pas démarré (par exemple après un redémarrage de l'appareil sans root)
- **THEN** l'état est « arrêté »

#### Scenario: version non prise en charge
- **WHEN** le service Shizuku répond avec une version antérieure à la version minimale prise en charge (`design.md`, D1)
- **THEN** l'état est « version non prise en charge » et aucune autorisation n'est demandée

#### Scenario: Shizuku démarré hors de SygixOs
- **WHEN** l'utilisateur démarre Shizuku dans son application puis revient dans SygixOs
- **THEN** au retour au premier plan, l'état est relu : « autorisation non accordée », « autorisation refusée définitivement » ou « prêt » selon la réponse de Shizuku

#### Scenario: service perdu
- **WHEN** le service Shizuku s'arrête alors que SygixOs est au premier plan
- **THEN** l'état passe à « arrêté » sans crash ; une opération en cours suit « Garde-fous de l'accueil d'origine »

#### Scenario: autorisation retirée dans Shizuku
- **WHEN** l'utilisateur retire l'autorisation de SygixOs dans l'application Shizuku, qui arrête alors le processus de SygixOs
- **THEN** au lancement suivant, l'état est relu (« autorisation non accordée » ou « autorisation refusée définitivement ») et l'état de l'accueil d'origine reste celui du système

#### Scenario: erreur de lecture
- **WHEN** la lecture de l'état de Shizuku échoue (erreur inattendue du service)
- **THEN** l'état est traité comme « arrêté », l'erreur n'est pas avalée (journalisée), sans crash

#### Scenario: Sui sur un appareil rooté
- **WHEN** Sui est installé et actif
- **THEN** SygixOs le détecte comme Shizuku et l'état suit les mêmes règles

#### Scenario: gestionnaire compatible alternatif
- **WHEN** Shevery ou un autre gestionnaire implémentant l'API Shizuku est installé et actif
- **THEN** SygixOs détecte son Binder et applique les mêmes états et autorisations ; un gestionnaire non compatible est indiqué comme indisponible

### Requirement: Autorisation Shizuku
SygixOs SHALL ne demander l'autorisation Shizuku qu'après une action explicite de l'utilisateur sur une action du mode avancé (`settings`), par le dialogue du gestionnaire, jamais au démarrage, au retour au premier plan ni pendant une mise à jour. Un refus SHALL laisser SygixOs utilisable ; un refus définitif SHALL être exposé comme tel, sans nouvelle demande automatique.

#### Scenario: autorisation accordée
- **WHEN** l'état est « autorisation non accordée », que l'utilisateur valide le contrôle et accepte le dialogue de Shizuku
- **THEN** l'état devient « prêt » ; aucune opération privilégiée n'a lieu sans nouvelle action de l'utilisateur

#### Scenario: autorisation refusée
- **WHEN** l'utilisateur refuse le dialogue de Shizuku
- **THEN** l'état reste « autorisation non accordée », ou devient « autorisation refusée définitivement » si l'utilisateur a refusé sans retour possible, et SygixOs reste utilisable

#### Scenario: aucune demande sans action
- **WHEN** SygixOs démarre, revient au premier plan ou installe une mise à jour, l'état de Shizuku étant « autorisation non accordée »
- **THEN** aucune autorisation n'est demandée

### Requirement: État de l'accueil d'origine
SygixOs SHALL lire sans Shizuku, à chaque retour au premier plan, l'état de chaque composant de l'accueil d'origine pour l'utilisateur courant : absent, actif, désactivé par l'utilisateur, ou hors de portée (désactivé par le système ou le constructeur, jamais modifié). L'accueil d'origine SHALL être désactivé quand tous les composants présents et à portée le sont, partiellement désactivé si une partie l'est, sinon actif ; indisponible sans composant présent et à portée, ou si la lecture échoue.

#### Scenario: lecture au lancement
- **WHEN** SygixOs affiche l'état avant la fin de la première lecture
- **THEN** l'état est « en cours de lecture » et aucune action n'est possible avant la fin de la lecture

#### Scenario: après un redémarrage
- **WHEN** l'accueil d'origine a été désactivé, que l'appareil redémarre et que Shizuku n'est pas démarré
- **THEN** SygixOs lit l'accueil d'origine désactivé

#### Scenario: réactivé hors de SygixOs
- **WHEN** l'accueil d'origine a été réactivé par le système ou par ADB pendant que SygixOs était en arrière-plan
- **THEN** au retour au premier plan, l'accueil d'origine est lu actif, sans valeur conservée d'avant la sortie

#### Scenario: désactivé par la procédure ADB
- **WHEN** l'utilisateur a suivi la procédure ADB du README et que tous les composants présents et à portée sont ainsi désactivés
- **THEN** SygixOs lit l'accueil d'origine désactivé, comme après une désactivation depuis SygixOs

#### Scenario: désactivation partielle
- **WHEN** une partie seulement des composants présents et à portée est désactivée (opération interrompue, procédure ADB incomplète)
- **THEN** l'accueil d'origine est lu « partiellement désactivé »

#### Scenario: composant hors de portée
- **WHEN** un composant de l'ensemble est désactivé par le système ou le constructeur (état autre que celui que pose la désactivation par l'utilisateur)
- **THEN** il est exclu du calcul de l'état et des opérations, et SygixOs ne le modifie jamais

#### Scenario: aucun composant ou lecture en échec
- **WHEN** aucun composant de l'ensemble n'est installé pour l'utilisateur courant, ou que la lecture de leur état échoue
- **THEN** l'accueil d'origine est « indisponible », avec une raison distincte pour l'absence et pour l'erreur, sans crash

### Requirement: Désactivation de l'accueil d'origine
Shizuku prêt et après confirmation de l'utilisateur (`settings`), SygixOs SHALL désactiver pour l'utilisateur courant, avec les droits de Shizuku, chaque composant actif de l'accueil d'origine, comme la procédure ADB du README, puis SHALL vérifier qu'il est devenu l'écran d'accueil effectif de l'appareil. Sans cette confirmation, SygixOs SHALL ne rien désactiver.

#### Scenario: désactivation réussie
- **WHEN** Shizuku est prêt, que l'accueil d'origine est actif et que l'utilisateur confirme sa désactivation
- **THEN** chaque composant actif est désactivé pour l'utilisateur courant, SygixOs est vérifié comme écran d'accueil effectif, et l'accueil d'origine est lu désactivé

#### Scenario: touche Home après la désactivation
- **WHEN** l'accueil d'origine est désactivé et que l'utilisateur presse Home depuis une autre application
- **THEN** le système ouvre SygixOs ; si SygixOs est déjà au premier plan, « Touche Home avec SygixOs au premier plan » s'applique

#### Scenario: aucune confirmation
- **WHEN** l'utilisateur n'a pas confirmé, ou a annulé la confirmation
- **THEN** aucun composant n'est modifié

### Requirement: Garde-fous de l'accueil d'origine
Avant de désactiver, SygixOs SHALL vérifier que l'appareil a terminé sa configuration initiale et que sa propre activité d'accueil est activée. Si un composant ne peut pas être désactivé, ou si SygixOs n'est pas l'écran d'accueil effectif après la désactivation, SygixOs SHALL remettre dans leur état par défaut les composants que l'opération a désactivés et exposer l'échec, sans crash. Chaque opération privilégiée SHALL être bornée dans le temps.

#### Scenario: préconditions non remplies
- **WHEN** l'appareil n'a pas terminé sa configuration initiale, ou que l'activité d'accueil de SygixOs n'est pas déclarée ou est désactivée
- **THEN** aucune désactivation n'a lieu et l'échec est exposé avec sa raison

#### Scenario: composant refusé
- **WHEN** un composant est désactivé mais qu'un autre est refusé par le système (par exemple un paquet protégé)
- **THEN** SygixOs remet dans son état par défaut celui que l'opération a désactivé, relit l'état, l'accueil d'origine reste actif et l'échec est exposé

#### Scenario: écran d'accueil effectif inattendu
- **WHEN** tous les composants ont été désactivés mais que SygixOs n'est pas l'écran d'accueil effectif dans le délai de vérification (autre launcher prioritaire, écran de choix)
- **THEN** SygixOs remet dans leur état par défaut les composants que l'opération a désactivés, relit l'état et expose l'échec

#### Scenario: délai dépassé
- **WHEN** une étape privilégiée ne répond pas dans son délai
- **THEN** SygixOs arrête d'abord le service privilégié, relit l'état des composants, puis remet dans leur état par défaut ceux que l'opération visait et qui sont désactivés, relit de nouveau l'état et expose l'échec, sans crash

#### Scenario: Shizuku perdu pendant l'opération
- **WHEN** le service Shizuku est perdu pendant l'opération, avant que l'annulation soit possible
- **THEN** l'état réel est relu sans Shizuku et exposé (éventuellement « partiellement désactivé »), avec l'échec ; le rétablissement reste proposé dès que Shizuku est de nouveau prêt, et le README décrit le rétablissement manuel

### Requirement: Rétablissement de l'accueil d'origine
Shizuku prêt et après confirmation de l'utilisateur (`settings`), SygixOs SHALL remettre dans leur état par défaut les composants de l'accueil d'origine désactivés par l'utilisateur, y compris par la procédure ADB du README, dès qu'au moins un l'est. Le rôle HOME n'est pas modifié au rétablissement ; son état affiché SHALL toujours être celui relu auprès du système.

#### Scenario: rétablissement réussi
- **WHEN** l'accueil d'origine est désactivé ou partiellement désactivé, que Shizuku est prêt et que l'utilisateur confirme le rétablissement
- **THEN** chaque composant désactivé par l'utilisateur est remis dans son état par défaut et l'état relu est « actif »

#### Scenario: rôle HOME après le rétablissement
- **WHEN** le rôle HOME a été attribué à SygixOs (par P5 ou par le système après la désactivation) et que l'accueil d'origine est rétabli
- **THEN** l'état du rôle affiché par « Réglages du launcher système » est celui relu auprès du système, et la touche Home ouvre l'écran d'accueil qui détient alors le rôle

#### Scenario: rétablissement en échec
- **WHEN** un composant ne peut pas être remis dans son état par défaut, ou qu'une étape dépasse son délai
- **THEN** le service privilégié est arrêté, l'état réel est relu et exposé avec l'échec, sans crash, et la procédure manuelle reste décrite dans le README

### Requirement: Autorisation de démarrage automatique du constructeur
Sur un appareil dont le constructeur fournit l'opération d'application `AUTO_START`, SygixOs SHALL, Shizuku prêt et après une action explicite de l'utilisateur, s'accorder cette opération pour son seul paquet par le service privilégié, puis la relire. Il SHALL ne rien proposer quand l'opération n'existe pas sur l'appareil, et ne SHALL jamais modifier cette opération pour un autre paquet.

#### Scenario: autorisation accordée
- **WHEN** Shizuku est prêt, que l'opération `AUTO_START` existe et vaut `ignore` pour SygixOs, et que l'utilisateur valide l'action
- **THEN** l'opération vaut `allow` pour SygixOs à la relecture, et au redémarrage complet suivant le système relie le service d'accessibilité de SygixOs et lui délivre la diffusion de fin de démarrage

#### Scenario: opération absente
- **WHEN** l'appareil ne connaît pas l'opération `AUTO_START`
- **THEN** aucune action d'autorisation de démarrage automatique n'est proposée

#### Scenario: Shizuku indisponible
- **WHEN** Shizuku n'est pas prêt
- **THEN** l'état de l'autorisation reste inconnu, rien n'est modifié et le parcours guidé de P5 garde son état « Bloqué » quand le service n'est pas relié

#### Scenario: autorisation déjà accordée
- **WHEN** l'opération `AUTO_START` existe et vaut déjà `allow` pour SygixOs
- **THEN** cet état est relu, aucune écriture n'est effectuée et le redémarrage ne dépend pas d'une nouvelle action

#### Scenario: autorisation refusée
- **WHEN** le service privilégié ou le constructeur refuse l'accord de `AUTO_START`
- **THEN** l'état n'est pas annoncé comme accordé, l'erreur est relue et exposée sans crash, et aucune nouvelle tentative n'a lieu sans action explicite

#### Scenario: délai ou erreur de relecture
- **WHEN** l'appel ou la relecture de `AUTO_START` dépasse le délai ou échoue
- **THEN** le service privilégié est arrêté, l'état reste inconnu ou est relu, l'échec est exposé sans crash et une nouvelle tentative requiert une action explicite

### Requirement: Opérations privilégiées limitées
SygixOs SHALL limiter Shizuku aux opérations décrites dans ce change et aux composants/permissions de son propre paquet. Il SHALL refuser les cibles non autorisées et SHALL arrêter son service privilégié à la fin de chaque opération et à l'arrêt de son processus.

#### Scenario: paquet hors de l'ensemble
- **WHEN** une opération privilégiée reçoit un paquet absent de l'ensemble des composants de l'accueil d'origine
- **THEN** elle est refusée sans rien modifier

#### Scenario: fin d'opération
- **WHEN** une désactivation ou un rétablissement se termine, réussi, en échec, interrompu ou après un délai dépassé
- **THEN** le service privilégié de SygixOs est arrêté et aucun processus privilégié de SygixOs ne reste actif

#### Scenario: arrêt de SygixOs pendant l'opération
- **WHEN** le processus de SygixOs s'arrête pendant une opération privilégiée
- **THEN** le service privilégié s'arrête aussi ; au lancement suivant, l'état de l'accueil d'origine est relu (éventuellement « partiellement désactivé »)

## MODIFIED Requirements

### Requirement: Parcours guidé du launcher
SygixOs SHALL préserver le parcours guidé P5 « Faire de SygixOs l'écran d'accueil », affiché selon les conditions P5 et accessible depuis les réglages. P7 SHALL ajouter après les étapes P5 une entrée focalisable vers le mode avancé si un gestionnaire compatible est reconnu, ou vers l'aide Shizuku sinon. L'aide décrit l'installation et le démarrage, recommande Shevery, ouvre son lien si possible, et place initialement le focus sur « Fermer ». L'entrée et l'aide ne demandent aucune permission Shizuku sans action explicite.

#### Scenario: premier lancement
- **WHEN** SygixOs se lance et que le parcours guidé n'a jamais été fermé, sur une TV où rien n'est encore fait
- **THEN** après l'écran de démarrage, il affiche l'en-tête, l'étape 1 « À faire », l'étape 2 avec switch désactivé, l'étape 3 « Facultatif » si disponible, puis l'entrée du mode avancé, « Retour : fermer » et « Terminé » ; l'étape 1 a le focus

#### Scenario: navigation dans le parcours
- **WHEN** le parcours est affiché et l'utilisateur parcourt les contrôles avec haut et bas
- **THEN** le focus parcourt les étapes P5 focalisables, l'entrée avancée puis « Terminé », sans boucle ; les intertitres ne sont pas focalisables, gauche/droite sont sans effet et aucun élément de l'accueil ne prend le focus

#### Scenario: actions des étapes
- **WHEN** l'utilisateur presse OK sur l'étape 1, l'étape 2, la sous-étape 2b, l'étape 3 ou l'entrée P7
- **THEN** les étapes P5 conservent leurs actions respectives ; l'entrée P7 ouvre le mode avancé si un gestionnaire compatible est reconnu, sinon le panneau d'aide ; le parcours reste affiché et le focus revient au contrôle appelant au retour

#### Scenario: sous-étape 2b
- **WHEN** l'utilisateur active l'étape 2 alors que la permission d'affichage superposé manque et que son écran Android existe
- **THEN** la sous-étape 2b apparaît sous l'étape 2 avec l'état « À faire », la progression est recalculée et le focus reste sur l'étape 2 ; la désactivation de l'étape 2 retire la sous-étape

#### Scenario: retour d'un écran système ouvert depuis le parcours
- **WHEN** l'utilisateur revient des réglages d'accessibilité, de permission, du rôle HOME ou d'un gestionnaire Shizuku ouvert depuis le parcours
- **THEN** le parcours est toujours affiché, les états sont relus, la progression est recalculée et le focus revient au contrôle qui a ouvert l'écran

#### Scenario: parcours terminé
- **WHEN** toutes les étapes obligatoires affichées sont « Fait »
- **THEN** les étapes obligatoires portent une coche, la progression indique qu'elles sont terminées, l'étape 3 reste facultative si le rôle HOME n'est pas détenu, et une nouvelle ouverture place le focus sur « Terminé »

#### Scenario: fermeture par Terminé
- **WHEN** l'utilisateur presse OK sur « Terminé », quels que soient les états des étapes
- **THEN** le parcours disparaît ; s'il était affiché automatiquement, sa fermeture est persistée et le focus revient au héro ; s'il venait des réglages, ceux-ci restent ouverts et le focus revient à la ligne P5 ; les choix restent inchangés

#### Scenario: demande de permission au premier lancement
- **WHEN** la demande « Programmes TV » est affichée à la fin de l'écran de démarrage
- **THEN** le parcours ne s'affiche pas tant que le dialogue système est ouvert, puis apparaît après la réponse

#### Scenario: installation existante mise à jour
- **WHEN** une installation existante lance pour la première fois la version qui introduit le parcours, qu'elle ait fermé ou non l'ancienne présentation
- **THEN** le parcours s'affiche une fois avec les états courants, puis ne réapparaît pas automatiquement après fermeture

#### Scenario: parcours ignoré
- **WHEN** l'utilisateur ferme un parcours affiché automatiquement par « Terminé » ou Retour sans valider d'étape
- **THEN** le parcours se ferme, le focus revient au héro, le rôle HOME n'est pas demandé, le démarrage reste désactivé et aucun écran système ne s'ouvre

#### Scenario: pas de nouvel affichage automatique
- **WHEN** SygixOs se lance de nouveau après la fermeture du parcours automatique, y compris après un redémarrage
- **THEN** il ne s'affiche pas automatiquement et reste accessible par les réglages

#### Scenario: ouverture depuis les réglages
- **WHEN** l'utilisateur ouvre le parcours depuis « SygixOs comme écran d'accueil »
- **THEN** il s'affiche par-dessus les réglages avec les états courants ; Retour ou « Terminé » le ferme, conserve les réglages ouverts, et rend le focus à la ligne sans changer le drapeau de fermeture automatique

#### Scenario: étape indisponible
- **WHEN** une étape P5 est « Indisponible » sur l'appareil
- **THEN** elle reste focalisable avec sa raison, OK n'ouvre rien, elle n'est pas comptée dans la progression et les autres étapes restent utilisables

#### Scenario: étape 3 masquée
- **WHEN** le rôle HOME est indisponible sans écran système de remplacement
- **THEN** l'étape 3 n'existe pas et bas depuis la dernière étape obligatoire donne le focus à l'entrée P7

#### Scenario: entrée P7 avec gestionnaire reconnu
- **WHEN** un gestionnaire compatible est reconnu et que l'utilisateur valide l'entrée P7
- **THEN** le mode avancé s'ouvre ; au retour, le parcours reste affiché avec le focus sur l'entrée

#### Scenario: entrée P7 sans gestionnaire reconnu
- **WHEN** aucun gestionnaire compatible n'est reconnu et que l'utilisateur valide l'entrée P7
- **THEN** l'aide Shizuku s'ouvre sans demander de permission ; Retour ferme l'aide et restaure le focus à l'entrée P7

#### Scenario: aide Shizuku navigable
- **WHEN** l'aide est ouverte et l'utilisateur parcourt ses contrôles au D-pad
- **THEN** le focus reste dans le panneau, commence sur « Fermer », haut/bas parcourent les commandes sans boucle, gauche/droite sont sans effet et Retour ferme l'aide

#### Scenario: processus arrêté pendant un écran système
- **WHEN** le système arrête le processus pendant qu'un écran système ou un gestionnaire ouvert depuis le parcours automatique est affiché, puis SygixOs se relance
- **THEN** le parcours jamais fermé s'affiche de nouveau avec les états relus

### Requirement: Permission d'affichage superposé pour le démarrage
P5 SHALL conserver la sous-étape 2b du parcours guidé comme parcours Android sans confirmation intermédiaire. P7 SHALL ajouter une action Shizuku distincte avec confirmation propre, limitée au paquet SygixOs et jamais révoquée. Chaque parcours relit l'état réel ; l'absence de l'écran Android ne bloque pas l'action P7 si Shizuku est prêt.

#### Scenario: sous-étape affichée à l'activation
- **WHEN** l'utilisateur active l'étape 2 alors que la permission d'affichage superposé manque et que son écran Android existe
- **THEN** la sous-étape 2b apparaît avec « À faire », la progression se recalcule et le focus reste sur l'étape 2 ; désactiver l'étape 2 retire 2b

#### Scenario: ouverture de l'écran de la permission
- **WHEN** l'utilisateur presse OK sur la sous-étape 2b
- **THEN** l'écran système de la permission s'ouvre et 2b passe à « En attente… » ; au retour son état est relu et le focus est sur 2b

#### Scenario: permission refusée
- **WHEN** l'utilisateur revient de l'écran système sans accorder la permission
- **THEN** SygixOs reste utilisable, conserve l'option de démarrage activée et 2b affiche « À faire »

#### Scenario: permission déjà accordée
- **WHEN** l'option de démarrage est activée alors que la permission est déjà accordée
- **THEN** la sous-étape 2b affiche « Fait » après relecture système

#### Scenario: écran de la permission absent
- **WHEN** l'appareil n'a pas d'écran système pour la permission et que l'option de démarrage est désactivée
- **THEN** l'étape 2 est « Indisponible », sans switch ni 2b, avec sa raison ; OK ne l'active pas ; l'action P7 reste disponible séparément si Shizuku est prêt

#### Scenario: écran de la permission impossible à ouvrir
- **WHEN** l'utilisateur presse OK sur 2b et que l'écran système ne peut pas s'ouvrir
- **THEN** 2b disparaît, une option activée par cette interaction revient à désactivé et l'étape 2 est « Indisponible » jusqu'au prochain lancement, sauf si la permission est relue accordée ; l'action P7 reste séparée

#### Scenario: autorisation Shizuku confirmée
- **WHEN** l'utilisateur confirme l'action d'affichage superposé dans le mode avancé P7
- **THEN** Shizuku accorde `SYSTEM_ALERT_WINDOW` au seul paquet SygixOs, l'état est relu et l'opération ne modifie pas le choix de démarrage à l'allumage

#### Scenario: refus de l'action Shizuku
- **WHEN** l'utilisateur annule la confirmation P7, refuse Shizuku ou le système refuse l'opération
- **THEN** aucune permission n'est annoncée comme accordée, l'état réel est relu, l'erreur est visible sans crash et le parcours Android reste disponible

#### Scenario: erreur d'autorisation Shizuku
- **WHEN** Shizuku est perdu, l'appel expire ou la relecture échoue pendant le parcours P7
- **THEN** le service privilégié est arrêté, aucun succès n'est annoncé sans relecture, l'erreur est visible sans crash et le parcours Android reste disponible

#### Scenario: révocation
- **WHEN** l'utilisateur révoque la permission dans Android
- **THEN** SygixOs relit l'état au retour au premier plan et ne la réaccorde pas automatiquement

### Requirement: Retour Home par le service d'accessibilité
SygixOs SHALL conserver l'activation Android P5 et ajouter l'activation Shizuku P7 après une action explicite et une confirmation distincte ; il SHALL ne jamais activer automatiquement, désactiver ou révoquer le service. Le service SHALL observer uniquement Home via `AccessibilityService.onKeyEvent`, ne lire aucun contenu de fenêtre et ne rien conserver. Quand il est connecté, il SHALL consommer l'appui, les répétitions et le relâchement de Home, puis ouvrir SygixOs au relâchement non annulé ; un appui long agit comme un appui court. Inactif, arrêté ou non connecté, aucune touche n'est consommée. L'état enabled et la connexion effective SHALL être suivis séparément ; aucune fonction Home n'est annoncée comme opérationnelle avant connexion. Une absence de signal Home ne SHALL pas bloquer SygixOs.

#### Scenario: activation par l'utilisateur
- **WHEN** l'utilisateur valide l'étape 1 du parcours P5 ou confirme l'action d'accessibilité du mode avancé P7
- **THEN** P5 ouvre les réglages d'accessibilité Android ; P7 active uniquement le service de SygixOs par Shizuku ; dans les deux cas l'étape 1 affiche « Fait » seulement si le service est activé et connecté

#### Scenario: activation Shizuku confirmée
- **WHEN** l'utilisateur confirme l'activation du service d'accessibilité de SygixOs dans le mode avancé
- **THEN** le service déclaré par SygixOs est ajouté aux services activés sans retirer les autres ; le retour affiche l'état système relu et restaure le focus sur le contrôle

#### Scenario: autres services d'accessibilité
- **WHEN** d'autres services d'accessibilité sont activés avant l'activation Shizuku de SygixOs
- **THEN** la liste des autres services et leurs états restent inchangés après l'opération

#### Scenario: annulation ou activation refusée
- **WHEN** l'utilisateur annule la confirmation ou que Shizuku/le système refuse l'activation
- **THEN** le service n'est pas présenté comme actif, l'erreur est visible sans crash et le parcours Android reste accessible

#### Scenario: service refusé
- **WHEN** l'utilisateur revient des réglages d'accessibilité sans avoir activé le service, ou ferme la présentation sans l'ouvrir
- **THEN** l'étape 1 affiche « À faire », SygixOs reste utilisable et aucune nouvelle proposition ne s'affiche automatiquement, à ce lancement comme aux suivants

#### Scenario: aucune nouvelle demande automatique
- **WHEN** l'utilisateur refuse une confirmation Shizuku, ou revient d'Android sans activer le service
- **THEN** aucune demande d'autorisation ni confirmation d'activation n'est réaffichée sans une nouvelle action explicite sur l'action d'accessibilité

#### Scenario: touche Home, SygixOs en arrière-plan
- **WHEN** le service est connecté, SygixOs est en arrière-plan et l'utilisateur presse Home
- **THEN** le service consomme l'appui et le relâchement, l'accueil du constructeur ne s'affiche pas et SygixOs revient au premier plan au relâchement

#### Scenario: touche Home, SygixOs au premier plan
- **WHEN** le service est connecté, SygixOs est au premier plan et l'utilisateur presse Home
- **THEN** la touche est consommée et SygixOs applique « Touche Home avec SygixOs au premier plan »

#### Scenario: appui long sur Home
- **WHEN** le service est connecté et que l'utilisateur maintient Home avec répétitions ou au-delà du délai d'appui long
- **THEN** tous les événements restent consommés et SygixOs s'ouvre au relâchement comme pour un appui court

#### Scenario: relâchement annulé
- **WHEN** le système marque annulé le relâchement de Home
- **THEN** l'événement est consommé et SygixOs ne s'ouvre pas

#### Scenario: signal Home absent
- **WHEN** l'appareil ne transmet aucune touche Home au service, même connecté
- **THEN** le service ne lit aucun contenu de fenêtre, ne demande aucune ouverture et laisse le comportement système inchangé

#### Scenario: service désactivé ou arrêté
- **WHEN** l'utilisateur désactive le service dans Android, ou que le système l'arrête
- **THEN** aucune touche n'est consommée, Home suit le comportement système, aucune erreur n'est affichée et l'étape 1 affiche « À faire » si le service est désactivé

#### Scenario: service non lié au lancement
- **WHEN** après un redémarrage complet, le constructeur empêche la liaison du service activé et l'utilisateur lance SygixOs
- **THEN** l'étape 1 affiche « En attente… » pendant l'attente bornée, puis « Bloqué » si le service ne s'est pas connecté ; SygixOs ne tente aucune nouvelle liaison et ne modifie pas les composants ni les réglages système

#### Scenario: service activé mais non connecté
- **WHEN** Android déclare le service activé mais ne l'a pas lié au processus de SygixOs
- **THEN** l'état d'autorisation activée reste distinct de l'état de connexion et SygixOs n'annonce pas que le retour Home est opérationnel tant que la connexion n'est pas observée

#### Scenario: service bloqué
- **WHEN** le service est activé dans Android mais reste non connecté au terme de l'attente bornée
- **THEN** l'étape 1 affiche « Bloqué » avec son explication ; OK ouvre les réglages d'accessibilité ; après désactivation/réactivation manuelle et connexion, l'étape 1 affiche « Fait» au retour
