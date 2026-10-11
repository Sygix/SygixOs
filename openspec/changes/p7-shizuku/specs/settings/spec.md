# Spec Delta

Les catégories et lignes existantes viennent des changes `up-next` (catégorie « Écran d'accueil ») et `p5-real-launcher` (« Réglages du launcher système »), archivés avant celui-ci. L'« accueil d'origine », ses états et ceux de Shizuku sont définis par `launcher-shell` (delta de ce change).

## ADDED Requirements

### Requirement: Réglage de l'accueil d'origine
La catégorie « Écran d'accueil » SHALL exposer l'action de désactivation/rétablissement de l'accueil d'origine dans le mode avancé, avec son état et celui de Shizuku (`launcher-shell`). L'ensemble des composants visés est celui présenté dans la confirmation.

#### Scenario: lecture en cours
- **WHEN** l'état de Shizuku ou de l'accueil d'origine est « en cours de lecture » et que l'utilisateur presse OK sur la ligne
- **THEN** rien ne se passe ; l'action devient disponible à la fin de la lecture

#### Scenario: Shizuku absent
- **WHEN** l'état de Shizuku est « absent » et que l'utilisateur presse OK sur l'entrée d'aide
- **THEN** le panneau d'aide explique l'installation d'un gestionnaire compatible et aucune opération privilégiée n'a lieu

#### Scenario: Shizuku arrêté
- **WHEN** l'état de Shizuku est « arrêté » et que l'utilisateur presse OK sur la ligne
- **THEN** l'application Shizuku s'ouvre pour que l'utilisateur la démarre et la ligne passe à « En attente… » ; au retour au premier plan, l'état est relu et le focus est sur la ligne

#### Scenario: autorisation non accordée
- **WHEN** l'état de Shizuku est « autorisation non accordée » et que l'utilisateur presse OK sur la ligne
- **THEN** le dialogue d'autorisation de Shizuku s'ouvre (« Autorisation Shizuku » de `launcher-shell`) et la ligne passe à « En attente… » ; à sa fermeture, l'état est relu et le focus est sur la ligne

#### Scenario: autorisation refusée définitivement
- **WHEN** l'état de Shizuku est « autorisation refusée définitivement » et que l'utilisateur presse OK sur la ligne
- **THEN** l'application Shizuku s'ouvre, où l'utilisateur peut accorder l'autorisation à SygixOs, et la ligne passe à « En attente… » ; au retour au premier plan, l'état est relu et le focus est sur la ligne

#### Scenario: Shizuku prêt
- **WHEN** l'état de Shizuku est « prêt » et que l'utilisateur presse OK sur la ligne
- **THEN** la confirmation de désactivation s'ouvre si l'accueil d'origine est actif ; la confirmation de rétablissement s'ouvre s'il est désactivé ou partiellement désactivé (« Confirmation de l'accueil d'origine »)

#### Scenario: ligne indisponible
- **WHEN** l'état de Shizuku est « version non prise en charge », ou que l'accueil d'origine est « indisponible »
- **THEN** la ligne affiche « Indisponible » avec sa raison, reste focalisable, et OK n'a aucun effet

#### Scenario: priorité des états
- **WHEN** plusieurs états s'appliquent à la fois, par exemple l'accueil d'origine « indisponible » et Shizuku « absent »
- **THEN** l'action et l'état affichés suivent cet ordre : lecture en cours, puis indisponibilité (accueil d'origine ou version de Shizuku), puis état de Shizuku, puis état de l'accueil d'origine ; dans l'exemple, la ligne est « Indisponible » et OK n'a aucun effet

#### Scenario: ouverture impossible
- **WHEN** l'application Shizuku ou la proposition d'installation ne peut pas s'ouvrir (aucune activité, erreur)
- **THEN** la ligne garde le focus, son détail indique l'échec, aucun crash n'a lieu et les autres réglages restent utilisables

#### Scenario: touche droite
- **WHEN** la ligne a le focus et que l'utilisateur presse droite
- **THEN** rien ne se passe, comme sur les lignes de « Réglages du launcher système »

### Requirement: Mode avancé Shizuku
Après la ligne P5 « SygixOs comme écran d'accueil », les réglages SHALL afficher directement le groupe d'actions avancées quand un gestionnaire compatible est reconnu ; sans gestionnaire reconnu, ils SHALL afficher une quatrième ligne d'aide expliquant l'installation et les capacités. L'entrée et l'aide de l'onboarding sont définies par « Parcours guidé du launcher » de `launcher-shell`. Le détail visuel est fixé par la maquette Penpot.

#### Scenario: gestionnaire reconnu dans les réglages
- **WHEN** un gestionnaire compatible est installé, que son service soit démarré ou arrêté, et que son état est lu
- **THEN** après la ligne P5 « SygixOs comme écran d'accueil », le groupe avancé expose directement les actions d'accueil d'origine, d'affichage superposé et d'accessibilité ; `AUTO_START` est affiché seulement si l'opération existe

#### Scenario: ordre et navigation des actions avancées
- **WHEN** le groupe avancé est affiché avec un gestionnaire reconnu et prêt
- **THEN** les actions focalisables suivent l'ordre accueil d'origine, affichage superposé, accessibilité, puis `AUTO_START` si disponible ; l'intertitre du groupe n'est pas focalisable, le focus initial de la catégorie reste celui de P5, haut/bas parcourent les actions sans boucle, droite est sans effet, gauche revient aux catégories et Retour ferme les réglages

#### Scenario: gestionnaire absent
- **WHEN** aucun gestionnaire compatible n'est installé ou reconnu
- **THEN** une quatrième ligne focalisable explique l'installation et les fonctions du mode avancé ; OK ouvre le panneau d'aide et ne lance aucune opération privilégiée

#### Scenario: gestionnaire arrêté
- **WHEN** un gestionnaire compatible est installé mais son service est arrêté
- **THEN** le groupe avancé reste affiché avec l'état « Shizuku arrêté » ; OK sur une action ouvre l'application du gestionnaire afin que l'utilisateur le démarre, puis au retour le focus revient à l'action qui a demandé son ouverture

#### Scenario: autorisation AUTO_START nécessaire
- **WHEN** `AUTO_START` existe pour SygixOs, vaut `ignore` et Shizuku est prêt
- **THEN** une action `AUTO_START` distincte est affichée ; OK l'exécute sans confirmation supplémentaire, puis l'état passe à « En attente… » jusqu'à relecture

#### Scenario: autorisation AUTO_START déjà accordée
- **WHEN** `AUTO_START` existe et vaut déjà `allow` pour SygixOs
- **THEN** l'état accordé est affiché après relecture et aucune opération n'est relancée automatiquement

#### Scenario: échec AUTO_START
- **WHEN** l'action `AUTO_START` est refusée, dépasse son délai ou sa relecture échoue
- **THEN** aucun succès n'est affiché, l'erreur ou l'état inconnu est présenté et une nouvelle tentative exige une action explicite

#### Scenario: aide Shizuku
- **WHEN** l'utilisateur ouvre le panneau d'aide depuis les réglages
- **THEN** le panneau décrit l'installation et le démarrage d'un gestionnaire compatible, recommande Shevery avec son lien officiel, offre une action pour ouvrir ce lien si possible et un bouton de fermeture ; le focus initial est sur le bouton de fermeture, haut/bas parcourent les contrôles sans boucle, gauche/droite sont sans effet, Retour ferme le panneau et le focus revient à son entrée

#### Scenario: retour au premier plan
- **WHEN** l'utilisateur revient d'un gestionnaire Shizuku ouvert depuis le mode avancé
- **THEN** SygixOs relit l'état et actualise les actions ; si le gestionnaire est encore reconnu, le focus revient à l'action qui l'a ouvert, et si cette action a disparu, il revient à l'entrée d'aide conditionnelle

### Requirement: Confirmation de l'accueil d'origine
Avant de désactiver ou de rétablir l'accueil d'origine, SygixOs SHALL afficher une confirmation qui explique l'effet, les conséquences et le moyen de revenir en arrière. Chaque confirmation possède un bouton de confirmation et un bouton d'annulation, avec le focus initial défini dans cette exigence.

#### Scenario: textes et focus de désactivation
- **WHEN** l'utilisateur demande la désactivation
- **THEN** le titre est « Désactiver l'accueil Google TV ? », le texte est « SygixOs deviendra le seul écran d'accueil. Les recommandations et menus de l'accueil Google TV ne seront plus disponibles. Avant de désinstaller SygixOs, rétablissez l'accueil d'origine depuis ce réglage. », les boutons sont « Désactiver » et « Annuler », et « Annuler » a le focus initial

#### Scenario: textes et focus de rétablissement
- **WHEN** l'utilisateur demande le rétablissement
- **THEN** le titre est « Rétablir l'accueil Google TV ? », le texte est « L'accueil Google TV sera réactivé. La touche Home peut continuer d'ouvrir SygixOs tant qu'il reste l'écran d'accueil par défaut. », les boutons sont « Rétablir » et « Annuler », et « Rétablir » a le focus initial

#### Scenario: navigation de la confirmation
- **WHEN** une confirmation est affichée
- **THEN** haut et bas passent d'un bouton à l'autre sans boucle, gauche et droite sont sans effet, Retour annule et le focus reste dans le panneau

#### Scenario: confirmation de la désactivation
- **WHEN** l'utilisateur presse le bouton de confirmation de la désactivation
- **THEN** la confirmation se ferme, la ligne passe à « En attente… » pendant l'opération (« Désactivation de l'accueil d'origine » de `launcher-shell`), puis affiche l'état relu ; le focus est sur la ligne

#### Scenario: confirmation du rétablissement
- **WHEN** l'utilisateur presse le bouton de confirmation du rétablissement
- **THEN** la confirmation se ferme, la ligne passe à « En attente… » pendant l'opération (« Rétablissement de l'accueil d'origine » de `launcher-shell`), puis affiche l'état relu ; le focus est sur la ligne

#### Scenario: annulation
- **WHEN** l'utilisateur presse le bouton d'annulation ou Retour
- **THEN** la confirmation se ferme sans aucune modification et le focus revient sur la ligne

#### Scenario: Shizuku perdu pendant la confirmation
- **WHEN** le service Shizuku s'arrête pendant que la confirmation est affichée, puis que l'utilisateur confirme
- **THEN** aucune opération n'est lancée, la confirmation se ferme, l'état relu (« arrêté ») est affiché et le focus revient sur la ligne

#### Scenario: touche Home pendant la confirmation
- **WHEN** la touche Home atteint SygixOs pendant que la confirmation est affichée
- **THEN** la confirmation se ferme sans modification, puis « Touche Home avec SygixOs au premier plan » de `launcher-shell` s'applique

### Requirement: Ligne de l'accueil d'origine pendant une opération
Pendant une désactivation ou un rétablissement, la ligne SHALL afficher « En attente… » et OK y SHALL être sans effet. L'opération SHALL aller à son terme, ou jusqu'à son délai, même si les réglages sont fermés (Retour ou touche Home) ; à la réouverture des réglages, la ligne SHALL afficher l'état relu et l'éventuel échec. Le focus SHALL rester libre de se déplacer dans les réglages pendant l'opération.

#### Scenario: OK pendant l'opération
- **WHEN** la ligne affiche « En attente… » pour une opération en cours et que l'utilisateur presse OK
- **THEN** aucune nouvelle opération ni confirmation n'est lancée

#### Scenario: réglages fermés pendant l'opération
- **WHEN** l'utilisateur ferme les réglages par Retour ou par la touche Home pendant l'opération, puis les rouvre après sa fin
- **THEN** l'opération s'est terminée ou a atteint son délai, et la ligne affiche l'état relu et, le cas échéant, l'échec

### Requirement: Confirmation des autorisations P5
SygixOs SHALL afficher une confirmation distincte avant d'accorder par Shizuku la permission d'affichage superposé ou d'activer son service d'accessibilité. Chaque confirmation explique l'action et propose une validation et une annulation ; « Annuler » reçoit le focus initial, haut/bas passent entre les deux boutons sans boucle, gauche/droite sont sans effet, Retour annule, et le focus est restauré sur l'action d'origine après fermeture.

#### Scenario: annulation d'une autorisation
- **WHEN** l'utilisateur annule ou presse Retour dans la confirmation
- **THEN** aucune opération privilégiée n'est lancée et le focus revient à l'action

#### Scenario: autorisation confirmée
- **WHEN** l'utilisateur valide la confirmation d'une permission
- **THEN** seule l'opération confirmée est lancée, la ligne indique « En attente… » jusqu'à relecture système, puis affiche l'état réel et le focus revient à l'action

#### Scenario: focus de l'action conditionnelle disparue
- **WHEN** le gestionnaire compatible n'est plus reconnu à la fermeture de la confirmation
- **THEN** le focus revient à la quatrième ligne d'aide qui remplace les actions avancées

### Requirement: États affichés de l'accueil d'origine
La ligne SHALL réutiliser le rendu des états de « Réglages du launcher système ». Elle SHALL ne présenter aucun succès avant relecture système et SHALL indiquer dans son détail l'état de Shizuku bloquant l'action, l'état partiel ou l'échec de la dernière opération.

#### Scenario: accueil d'origine désactivé, Shizuku arrêté
- **WHEN** l'accueil d'origine est désactivé et que Shizuku est arrêté
- **THEN** la ligne montre l'accueil d'origine désactivé et son détail indique que Shizuku doit être démarré pour le rétablir ; OK suit l'état de Shizuku

#### Scenario: état partiel affiché
- **WHEN** l'accueil d'origine est « partiellement désactivé »
- **THEN** le détail l'indique et OK, Shizuku prêt, propose le rétablissement

#### Scenario: échec affiché
- **WHEN** la dernière désactivation ou le dernier rétablissement a échoué (« Garde-fous de l'accueil d'origine » de `launcher-shell`)
- **THEN** la ligne affiche l'état relu et un détail d'échec avec sa raison, jusqu'à la prochaine action ou au prochain lancement

#### Scenario: retour d'un écran de Shizuku
- **WHEN** l'utilisateur revient dans SygixOs après un écran de Shizuku ouvert depuis la ligne
- **THEN** « En attente… » est remplacé par l'état relu et le focus est sur la ligne

#### Scenario: rappel de débogage sans fil
- **WHEN** une opération privilégiée réussit après utilisation du débogage sans fil
- **THEN** le détail rappelle que l'auto-démarrage de Shizuku peut réactiver le débogage sans fil après chaque redémarrage et conseille de le désactiver après usage

## MODIFIED Requirements

### Requirement: Réglages du launcher système
La catégorie « Écran d'accueil » SHALL conserver, après « Afficher À suivre » et « Position d'À suivre », l'intertitre non focalisable « Launcher système » et la ligne P5 « SygixOs comme écran d'accueil », avec sa synthèse de parcours et son focus initial P5. P7 ajoute après cette ligne les actions avancées si un gestionnaire est reconnu, ou une quatrième ligne d'aide sinon. Aucune autre catégorie ne SHALL porter les contrôles P5/P7. Le focus initial reste « Afficher À suivre » ; le rendu des lignes et états P5 est conservé. P7 n'altère pas la synthèse P5 et ajoute l'ordre D-pad et le retour focus des actions avancées définis ci-dessous.

#### Scenario: emplacement
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil »
- **THEN** le volet droit présente « Afficher À suivre », « Position d'À suivre », l'intertitre « Launcher système » puis « SygixOs comme écran d'accueil » ; après cette ligne P5, les actions avancées sont directement affichées si un gestionnaire est reconnu, sinon une quatrième ligne explique l'installation et ouvre l'aide. Le focus initial reste sur « Afficher À suivre » et aucune autre catégorie ne porte ces contrôles

#### Scenario: navigation D-pad de la ligne P5
- **WHEN** le focus est sur « Position d'À suivre » (liste fermée) et l'utilisateur presse bas, puis bas encore, puis haut deux fois, puis bas, droite, puis gauche
- **THEN** le focus passe à « SygixOs comme écran d'accueil », puis, si le gestionnaire est reconnu, au groupe et à ses actions ; sinon il passe à l'aide, quatrième contrôle focalisable. Haut remonte l'ordre affiché sans boucle ; droite ne fait rien ; gauche rend le focus au volet des catégories sur « Écran d'accueil » ; Retour ferme les réglages

#### Scenario: ouverture du parcours
- **WHEN** l'utilisateur presse OK sur « SygixOs comme écran d'accueil »
- **THEN** le parcours guidé P5 s'affiche par-dessus les réglages ; Retour ou « Terminé » le ferme, conserve les réglages ouverts, rend le focus à la ligne et recalcule sa synthèse

#### Scenario: synthèse partielle
- **WHEN** l'étape 1 du parcours est « Fait » et que l'option de démarrage est désactivée
- **THEN** la ligne P5 affiche « Partiel », sans pastille, et le détail « Démarrage à l'allumage à terminer »

#### Scenario: synthèse terminée
- **WHEN** l'étape 1 est « Fait », le démarrage est activé et la sous-étape 2b est « Fait » ou absente, quel que soit le rôle HOME
- **THEN** la ligne P5 affiche « Terminé » avec pastille verte et le détail « Toutes les étapes sont faites »

#### Scenario: touche Home bloquée
- **WHEN** l'étape 1 du parcours est « Bloqué »
- **THEN** la ligne P5 affiche « Bloqué », sans pastille, et le détail « Touche Home bloquée par la TV », quels que soient les autres états

#### Scenario: rien de fait
- **WHEN** aucune étape obligatoire du parcours n'est « Fait »
- **THEN** la ligne P5 affiche « À faire » et le détail « Touche Home et démarrage à l'allumage à terminer », ou la seule étape disponible qui reste

#### Scenario: état relu au retour
- **WHEN** SygixOs revient au premier plan après un écran système ouvert depuis le parcours ou après un passage en arrière-plan
- **THEN** l'état du service d'accessibilité, de la permission et du rôle HOME est relu auprès du système, la synthèse de la ligne P5 est recalculée sans valeur périmée et le focus revient à la ligne appelante

#### Scenario: navigation D-pad des actions avancées
- **WHEN** un gestionnaire est reconnu et le focus parcourt le groupe avancé après la ligne P5
- **THEN** il passe à l'accueil d'origine, à la superposition, à l'accessibilité puis à `AUTO_START` si disponible ; l'intertitre est sauté, les bords ne bouclent pas, gauche rend le focus aux catégories et Retour ferme les réglages

#### Scenario: gestionnaire absent
- **WHEN** aucun gestionnaire compatible n'est reconnu
- **THEN** la quatrième ligne d'aide suit « SygixOs comme écran d'accueil » ; OK ouvre l'aide et le focus revient à cette ligne à sa fermeture

#### Scenario: gestionnaire arrêté et retour focus
- **WHEN** un gestionnaire reconnu est arrêté, l'utilisateur valide une action P7 puis revient après l'avoir démarré
- **THEN** l'application du gestionnaire s'ouvre ; si elle reste reconnue, le focus revient à l'action P7, sinon à la quatrième ligne d'aide

#### Scenario: Shizuku prêt, écran de superposition indisponible
- **WHEN** l'écran Android de superposition est absent ou a échoué à s'ouvrir, mais Shizuku est prêt
- **THEN** l'action P7 d'octroi reste disponible ; après succès et relecture, le parcours P5 permet de réactiver le démarrage explicitement s'il avait été rendu indisponible
