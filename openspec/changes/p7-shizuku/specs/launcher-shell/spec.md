# Spec Delta

L'« accueil d'origine » désigne l'écran d'accueil fourni avec la TV et le composant système qui le réactive ; l'ensemble de ses composants est défini par `design.md` (D4, Question ouverte 2). Le contrôle, la confirmation et la navigation sont décrits par `settings` (« Réglage de l'accueil d'origine », « Confirmation de l'accueil d'origine ») ; ce delta ne décrit que les effets sur le système.

## ADDED Requirements

### Requirement: État de Shizuku
SygixOs SHALL détecter Shizuku, ou Sui sur un appareil rooté, et en exposer l'état : en cours de lecture, absent, version non prise en charge, arrêté, autorisation non accordée, autorisation refusée définitivement, prêt. L'état SHALL être relu à chaque retour au premier plan et à chaque apparition ou perte du service Shizuku, et ne SHALL jamais être « prêt » avant confirmation par Shizuku. Shizuku absent, arrêté ou perdu, SygixOs SHALL rester pleinement utilisable, sans crash.

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

### Requirement: Autorisation Shizuku
SygixOs SHALL ne demander l'autorisation Shizuku qu'après une action explicite de l'utilisateur sur le contrôle de l'accueil d'origine (`settings`), par le dialogue de Shizuku, jamais au démarrage, au retour au premier plan ni pendant une mise à jour. Un refus SHALL laisser SygixOs utilisable ; un refus définitif SHALL être exposé comme tel, sans nouvelle demande automatique.

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
Shizuku prêt et après confirmation de l'utilisateur (`settings`), SygixOs SHALL remettre dans leur état par défaut les composants de l'accueil d'origine désactivés par l'utilisateur, y compris par la procédure ADB du README, dès qu'au moins un l'est. Le traitement du rôle HOME au rétablissement suit la Question ouverte 1 de `design.md` ; l'état du rôle affiché SHALL toujours être celui relu auprès du système.

#### Scenario: rétablissement réussi
- **WHEN** l'accueil d'origine est désactivé ou partiellement désactivé, que Shizuku est prêt et que l'utilisateur confirme le rétablissement
- **THEN** chaque composant désactivé par l'utilisateur est remis dans son état par défaut et l'état relu est « actif »

#### Scenario: rôle HOME après le rétablissement
- **WHEN** le rôle HOME a été attribué à SygixOs (par P5 ou par le système après la désactivation) et que l'accueil d'origine est rétabli
- **THEN** l'état du rôle affiché par « Réglages du launcher système » est celui relu auprès du système, et la touche Home ouvre l'écran d'accueil qui détient alors le rôle

#### Scenario: rétablissement en échec
- **WHEN** un composant ne peut pas être remis dans son état par défaut, ou qu'une étape dépasse son délai
- **THEN** le service privilégié est arrêté, l'état réel est relu et exposé avec l'échec, sans crash, et la procédure manuelle reste décrite dans le README

### Requirement: Opérations privilégiées limitées
SygixOs SHALL n'utiliser les droits fournis par Shizuku que pour les opérations sur l'accueil d'origine décrites par ce change, pour l'utilisateur courant. Il SHALL n'exécuter aucune commande arbitraire, SHALL refuser tout paquet hors de ceux que prévoit l'opération demandée (les composants de l'accueil d'origine, sauf extension décidée par la Question ouverte 1), et SHALL arrêter son service privilégié à la fin de chaque opération et à l'arrêt de son processus.

#### Scenario: paquet hors de l'ensemble
- **WHEN** une opération privilégiée reçoit un paquet absent de l'ensemble des composants de l'accueil d'origine
- **THEN** elle est refusée sans rien modifier

#### Scenario: fin d'opération
- **WHEN** une désactivation ou un rétablissement se termine, réussi, en échec, interrompu ou après un délai dépassé
- **THEN** le service privilégié de SygixOs est arrêté et aucun processus privilégié de SygixOs ne reste actif

#### Scenario: arrêt de SygixOs pendant l'opération
- **WHEN** le processus de SygixOs s'arrête pendant une opération privilégiée
- **THEN** le service privilégié s'arrête aussi ; au lancement suivant, l'état de l'accueil d'origine est relu (éventuellement « partiellement désactivé »)
