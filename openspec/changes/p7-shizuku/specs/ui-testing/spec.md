# Spec Delta

## ADDED Requirements

### Requirement: Couverture des parcours Shizuku
Les parcours P7 SHALL être couverts par des tests déterministes, sans Shizuku réel ni appareil : l'état de Shizuku, la réponse à la demande d'autorisation, le service privilégié (succès, composant refusé, délai dépassé, service perdu, arrêt), l'état des composants de l'accueil d'origine et l'écran d'accueil effectif SHALL être fournis par des doubles. Les tests Compose SHALL utiliser la taille TV des autres tests de navigation.

#### Scenario: action selon l'état de Shizuku
- **WHEN** le test affiche le groupe avancé avec un gestionnaire reconnu et le double Shizuku successivement « en cours de lecture », « arrêté », « autorisation non accordée », « autorisation refusée définitivement », « version non prise en charge » et « prêt », puis valide « setting-stock-home »
- **THEN** « en cours de lecture » ne lance rien ; « arrêté » ouvre le gestionnaire ; l'autorisation non accordée déclenche la demande Shizuku ; le refus définitif ouvre le gestionnaire ; la version non prise en charge n'opère pas ; « prêt » ouvre la confirmation P7 ; rien n'est modifié sans confirmation

#### Scenario: gestionnaire absent
- **WHEN** le test affiche « Écran d'accueil » sans gestionnaire reconnu et presse OK sur « setting-advanced-help »
- **THEN** le panneau d'aide s'ouvre, aucun contrôle `setting-stock-home` ni aucune action privilégiée n'existe, et aucune demande d'autorisation Shizuku n'est faite

#### Scenario: désactivation confirmée
- **WHEN** le double est « prêt », l'accueil d'origine actif, et que le test presse OK sur « setting-stock-home » puis sur « stock-home-confirm »
- **THEN** le double reçoit la désactivation des seuls composants actifs de l'ensemble, dans l'ordre de `design.md` (D5), « setting-stock-home-state » affiche « En attente… » pendant l'opération puis l'état relu, et le focus est sur « setting-stock-home »

#### Scenario: annulation
- **WHEN** le test ouvre « stock-home-confirmation » puis presse « stock-home-cancel », puis la rouvre et presse Retour
- **THEN** aucune opération n'est demandée au double et le focus revient sur « setting-stock-home » les deux fois

#### Scenario: garde-fous
- **WHEN** le double refuse un composant après en avoir désactivé un autre, puis, dans un second test, ne rend pas SygixOs écran d'accueil effectif, puis, dans un troisième, dépasse le délai d'une étape
- **THEN** dans les trois cas, le double reçoit la remise à l'état par défaut des composants désactivés par l'opération, après l'arrêt du service privilégié dans le troisième cas ; l'état relu est affiché avec un détail d'échec, sans crash

#### Scenario: service arrêté en fin d'opération
- **WHEN** une opération se termine par un succès, un échec, un délai dépassé ou la perte de Shizuku
- **THEN** le double constate l'arrêt du service privilégié dans chaque cas

#### Scenario: état partiel et rétablissement
- **WHEN** le double déclare l'accueil d'origine partiellement désactivé, puis désactivé, avec Shizuku « prêt », et que le test confirme le rétablissement
- **THEN** le double reçoit la remise à l'état par défaut des seuls composants désactivés par l'utilisateur et l'état relu est affiché

#### Scenario: opération en cours
- **WHEN** une opération est en cours et que le test presse OK sur « setting-stock-home », puis ferme et rouvre les réglages après la fin de l'opération
- **THEN** aucune nouvelle opération n'est demandée et la ligne affiche l'état relu

#### Scenario: état relu au retour
- **WHEN** le double change l'état des composants pendant que SygixOs est en arrière-plan, puis que le test simule le retour au premier plan
- **THEN** « setting-stock-home-state » affiche l'état relu, sans valeur conservée d'avant la sortie

#### Scenario: priorité et ouverture impossible
- **WHEN** le test affiche le groupe avancé reconnu avec l'accueil d'origine « Indisponible », puis avec Shizuku arrêté et un double qui fait échouer l'ouverture du gestionnaire
- **THEN** dans le premier cas OK ne demande aucune opération ; dans le second l'action garde le focus, indique l'échec sans crash et les autres réglages restent utilisables

#### Scenario: confirmation interrompue et retour de Shizuku
- **WHEN** la touche Home atteint l'activité, puis, dans un second test, le double perd Shizuku, pendant que « stock-home-confirmation » est affiché ; dans un troisième test, le test revient au premier plan après l'ouverture de Shizuku depuis la ligne
- **THEN** la confirmation se ferme sans opération demandée au double dans les deux premiers cas ; dans le troisième, « setting-stock-home-state » affiche l'état relu et « setting-stock-home » a le focus

#### Scenario: opérations limitées
- **WHEN** le test JUnit demande au service privilégié une opération sur un paquet absent de l'ensemble de l'accueil d'origine
- **THEN** l'opération est refusée et aucune commande n'est exécutée

#### Scenario: autorisation jamais demandée sans action
- **WHEN** le test simule le démarrage, le retour au premier plan et une mise à jour avec le double « autorisation non accordée »
- **THEN** aucune demande d'autorisation n'est faite au double

#### Scenario: confirmation des permissions P5
- **WHEN** le test ouvre la confirmation d'affichage superposé puis celle d'accessibilité, presse OK sans déplacer le focus, puis annule chacune
- **THEN** « Annuler » a le focus initial, OK n'accorde aucune permission, haut/bas parcourent les boutons sans boucle, gauche/droite ne changent pas le focus, Retour annule et le focus revient à l'action d'origine

#### Scenario: octroi overlay limité et relu
- **WHEN** le test confirme l'autorisation d'affichage superposé avec un double Shizuku prêt, puis simule un refus et une erreur de relecture
- **THEN** les appels ciblent uniquement le paquet SygixOs ; chaque résultat est relu, aucun succès n'est affiché au refus/à l'erreur, le parcours Android reste disponible et la permission n'est pas révoquée automatiquement

#### Scenario: activation accessibilité préserve les autres services
- **WHEN** le test confirme l'activation de SygixOs alors que le double déclare d'autres services accessibles et un service SygixOs activé mais non connecté
- **THEN** les autres services restent activés, l'état configuré est distinct de la connexion, et l'application ne déclare pas le retour Home fonctionnel avant confirmation de la connexion

#### Scenario: AUTO_START refuse les résultats non confirmés
- **WHEN** le test simule AUTO_START déjà autorisé, puis refusé, expiré et en erreur de relecture
- **THEN** un état déjà autorisé n'entraîne aucune écriture ; seuls les résultats relus sont affichés, les échecs restent visibles et une nouvelle écriture exige une nouvelle action, sans confirmation

### Requirement: Sélecteurs du mode avancé
Les éléments du mode avancé SHALL porter des testTags stables : « setting-advanced-group », « onboarding-advanced-mode », « setting-advanced-help », « onboarding-advanced-help », « setting-advanced-overlay », « setting-advanced-accessibility », « setting-advanced-auto-start », « advanced-help-panel », « advanced-help-open » et « advanced-help-close ». Les actions d'accueil d'origine gardent les sélecteurs `setting-stock-home*` et `stock-home-*`.

#### Scenario: affichage conditionnel dans les réglages
- **WHEN** le test affiche « Écran d'accueil » avec un gestionnaire compatible installé (service prêt puis arrêté), puis sans gestionnaire reconnu
- **THEN** après « setting-launcher-guide », « setting-advanced-group » expose directement les actions (`setting-stock-home`, « setting-advanced-overlay », « setting-advanced-accessibility », et « setting-advanced-auto-start » si l'opération existe) quand le gestionnaire est reconnu ; sinon « setting-advanced-help » est le quatrième contrôle focalisable et explique l'installation ; « setting-upnext-visible » garde le focus initial

#### Scenario: gestionnaire arrêté et focus rétabli
- **WHEN** Shizuku est installé mais arrêté, le test valide une action, puis simule le retour du gestionnaire avec son Binder prêt, puis, dans un second cas, sans gestionnaire reconnu
- **THEN** l'application du gestionnaire s'ouvre ; au retour le focus revient à l'action initiale quand elle existe, ou à « setting-advanced-help » si elle a été remplacée

#### Scenario: aide navigable
- **WHEN** le test ouvre l'aide, parcourt les contrôles au D-pad puis presse Retour
- **THEN** « advanced-help-panel » contient « advanced-help-open » et « advanced-help-close », le focus ne sort pas du panneau et revient à l'entrée d'aide après fermeture

#### Scenario: actions sensibles confirmées
- **WHEN** le test presse OK sur une action d'affichage superposé ou d'accessibilité, puis ouvre et annule la confirmation
- **THEN** aucune opération privilégiée n'est demandée au double, le focus initial est « Annuler » et le focus revient à l'action ou à la quatrième ligne d'aide si elle a remplacé le groupe

#### Scenario: AUTO_START sans confirmation
- **WHEN** le test presse OK sur « setting-advanced-auto-start » avec l'opération disponible et Shizuku prêt
- **THEN** l'action est envoyée sans panneau de confirmation, la ligne passe à « En attente… » puis affiche l'état relu et le focus reste sur l'action

#### Scenario: confirmation
- **WHEN** le test ouvre la confirmation depuis « setting-stock-home »
- **THEN** « stock-home-confirmation » est affiché par-dessus les réglages avec « stock-home-confirm » et « stock-home-cancel », haut et bas passent de l'un à l'autre sans boucle, gauche et droite ne déplacent pas le focus, Retour la ferme

## MODIFIED Requirements

### Requirement: Sélecteurs du launcher système
Le parcours et la ligne P5 SHALL exposer les testTags `onboarding-launcher`, `onboarding-mascot`, `onboarding-title`, `onboarding-progress`, `onboarding-step-accessibility`, `onboarding-step-boot`, `onboarding-step-overlay`, `onboarding-step-home-role`, `onboarding-done`, `onboarding-hint`, `setting-launcher-group` et `setting-launcher-guide`, ainsi que leurs sélecteurs d'état/détail/marker définis par `ui-testing`. L'étape 2 conserve `onboarding-step-boot-switch`. Aucun ancien sélecteur `onboarding-row-*`, `onboarding-continue`, `setting-home-role`, `setting-boot-start`, `setting-accessibility-home` ou `launcher-overlay-*` ne SHALL être réintroduit. Les tests Compose TV SHALL vérifier l'ordre, le focus, les états, la navigation D-pad et la fermeture du parcours selon les scénarios P5 et P7 ci-dessous.

#### Scenario: ordre et focus initial du parcours
- **WHEN** le test compose l'accueil avec le parcours à afficher, après la fin de l'écran de démarrage et la réponse à la demande de permission, rien n'étant fait et le rôle disponible
- **THEN** « onboarding-launcher » est affiché par-dessus « zone-hero » ; « onboarding-mascot » et « onboarding-title » précèdent l'introduction, la progression, les étapes P5, « onboarding-advanced-mode » puis le pied ; le pied place « onboarding-hint » à gauche de « onboarding-done » ; « onboarding-step-accessibility » a le focus, le switch boot est désactivé et le rôle facultatif est indiqué

#### Scenario: navigation dans le parcours
- **WHEN** le test parcourt les étapes P5, l'entrée P7 puis le pied avec haut/bas, puis presse gauche et droite
- **THEN** le focus suit les étapes disponibles, « onboarding-advanced-mode » et « onboarding-done » sans boucle ; si le rôle facultatif est absent, le parcours va directement à l'entrée P7 ; gauche/droite ne déplacent pas le focus et l'accueil ne le reçoit jamais

#### Scenario: sous-étape 2b et progression
- **WHEN** le double déclare le service connecté et l'écran superposé disponible mais la permission absente, puis le test active le boot, ouvre 2b et revient avec la permission accordée
- **THEN** 2b apparaît entre boot et rôle, « À faire », sans panneau intermédiaire ; la progression est recalculée sans compter l'entrée P7, le focus initial reste sur boot, puis 2b affiche « Fait » au retour et le focus est rétabli

#### Scenario: états affichés
- **WHEN** le test compose une ligne d'état (présentation ou réglages) dans chacun des états « Actif », « En attente… », « Inactif » et « Indisponible », avec et sans le focus
- **THEN** « <ligne>-state » affiche le libellé de l'état ; « <ligne>-dot » existe, à gauche de « <ligne>-state », pour « Actif » (couleur `SwitchOn`) et « En attente… » (couleur `Badge`) et n'existe pas pour « Inactif » et « Indisponible » ; en « Indisponible », « <ligne>-detail » affiche la raison propre au contrôle, le titre a la couleur secondaire hors focus, la ligne prend le focus et OK n'ouvre aucun écran ; le libellé d'état n'est jamais « Actif » tant que le double n'a pas confirmé l'état

#### Scenario: étape 3 masquée
- **WHEN** le double déclare le rôle HOME indisponible sans écran de remplacement
- **THEN** « onboarding-step-home-role » n'existe pas et bas depuis la dernière étape P5 donne le focus à l'entrée P7

#### Scenario: fermeture définitive du parcours
- **WHEN** le test, dans trois compositions distinctes, presse OK sur « onboarding-done », Retour puis Home avec le parcours automatique affiché
- **THEN** le parcours disparaît et son drapeau fermé est persisté ; le héro récupère le focus et un second lancement ne le réaffiche pas ; les choix P5 déjà faits persistent

#### Scenario: retour système dans le parcours
- **WHEN** le test ouvre une étape P5 depuis le parcours, simule l'absence et revient avec le double signalant le résultat
- **THEN** l'état « En attente… » disparaît, les états et la progression sont relus, le parcours reste affiché et le focus revient à l'étape ayant ouvert l'écran

#### Scenario: ordre des contrôles de la catégorie
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** « setting-upnext-visible », « setting-upnext-position », « setting-launcher-group » et « setting-launcher-guide » précèdent les actions P7 si un gestionnaire est reconnu, ou « setting-advanced-help » comme quatrième contrôle focalisable sinon ; « setting-upnext-visible » a le focus, les intertitres ne le prennent pas et les anciens tags P5 n'existent pas

#### Scenario: navigation dans la catégorie
- **WHEN** le test parcourt « setting-launcher-guide » puis les actions P7 avec un gestionnaire reconnu, puis sans gestionnaire
- **THEN** le focus parcourt les actions P7 dans l'ordre défini, ou la quatrième ligne d'aide ; les intertitres sont sautés, les bords ne bouclent pas, gauche rend le focus aux catégories et Retour ferme les réglages

#### Scenario: parcours ouvert depuis les réglages
- **WHEN** le test presse OK sur « setting-launcher-guide », puis ferme le parcours par Retour et « onboarding-done » dans deux cas distincts
- **THEN** le parcours s'affiche par-dessus les réglages et se ferme sans fermer la page ; « setting-launcher-guide » retrouve le focus et le drapeau du parcours automatique reste inchangé

#### Scenario: synthèse de la ligne des réglages
- **WHEN** le test simule l'étape 1 « Fait » et le boot désactivé, toutes les étapes faites, l'étape 1 « Bloqué », puis rien de fait
- **THEN** « setting-launcher-guide-state/detail » affiche successivement « Partiel » / « Démarrage à l'allumage à terminer », « Terminé » / « Toutes les étapes sont faites », « Bloqué » / « Touche Home bloquée par la TV », puis « À faire » / « Touche Home et démarrage à l'allumage à terminer »

#### Scenario: confirmation d'octroi superposé par Shizuku
- **WHEN** le test presse OK sur « setting-advanced-overlay » avec Shizuku prêt, puis confirme ou annule
- **THEN** la confirmation dédiée a « Annuler » focalisé ; aucun appel n'est fait avant validation ; seule la permission SygixOs est ciblée et, au retour, le focus revient à l'action ou à la ligne d'aide conditionnelle

#### Scenario: démarrage indisponible sans écran de permission
- **WHEN** le double déclare l'écran system de permission absent ou échoue à l'ouvrir depuis « onboarding-step-overlay »
- **THEN** la sous-étape 2b et le switch boot deviennent indisponibles, un choix activé par cette interaction revient à désactivé ; l'action P7 reste accessible si Shizuku est prêt, et après octroi/relecture l'étape P5 redevient disponible sans réactiver le boot
