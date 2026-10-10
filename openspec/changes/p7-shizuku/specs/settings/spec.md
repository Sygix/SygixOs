# Spec Delta

Les catégories et lignes existantes viennent des changes `up-next` (catégorie « Écran d'accueil ») et `p5-real-launcher` (« Réglages du launcher système »), archivés avant celui-ci. L'« accueil d'origine », ses états et ceux de Shizuku sont définis par `launcher-shell` (delta de ce change).

## ADDED Requirements

### Requirement: Réglage de l'accueil d'origine
La catégorie « Écran d'accueil » SHALL contenir, après les lignes de « Réglages du launcher système », une ligne de réglage focalisable pour l'accueil d'origine, au rendu de ces lignes, qui affiche l'état de l'accueil d'origine et de Shizuku (`launcher-shell`) et déclenche par OK l'action de l'état courant. Placement exact (dans le groupe « Launcher système » ou sous un intertitre propre) et libellés : Question ouverte 3 de `design.md` ; rendu : maquettes à valider.

#### Scenario: lecture en cours
- **WHEN** l'état de Shizuku ou de l'accueil d'origine est « en cours de lecture » et que l'utilisateur presse OK sur la ligne
- **THEN** rien ne se passe ; l'action devient disponible à la fin de la lecture

#### Scenario: Shizuku absent
- **WHEN** l'état de Shizuku est « absent » et que l'utilisateur presse OK sur la ligne
- **THEN** SygixOs propose l'installation de Shizuku (moyen : Question ouverte 5 de `design.md`) et ne modifie rien ; le cas échéant, au retour au premier plan, l'état est relu et le focus est sur la ligne

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

### Requirement: Confirmation de l'accueil d'origine
Avant de désactiver ou de rétablir l'accueil d'origine, SygixOs SHALL afficher une confirmation distincte par-dessus les réglages : explication de l'effet, des conséquences et du moyen de revenir en arrière, un bouton de confirmation et un bouton d'annulation. Haut et bas passent d'un bouton à l'autre sans boucle, gauche et droite sont sans effet, Retour annule ; le focus reste dans la confirmation tant qu'elle est affichée. Textes et focus initial : Question ouverte 7 de `design.md`.

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

### Requirement: États affichés de l'accueil d'origine
La ligne SHALL réutiliser le rendu des états de « Réglages du launcher système » (pastilles, « En attente… », « Indisponible » avec raison et titre atténué, aucun succès affiché avant relecture). La correspondance entre l'état de l'accueil d'origine et le libellé d'état suit le libellé de la ligne (Question ouverte 3). Le détail SHALL indiquer l'état de Shizuku quand il empêche l'action, l'état partiel, et l'échec de la dernière opération jusqu'à la prochaine action ou au prochain lancement.

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

## MODIFIED Requirements

### Requirement: Réglages du launcher système
La catégorie « Écran d'accueil » SHALL contenir, après « Afficher Up Next » et « Position d'Up Next » et sans changer son focus initial (« Afficher Up Next » reste son premier contrôle), un intertitre « Launcher système » jamais focusable puis trois lignes de réglage, dans cet ordre : « Remplacer le launcher », détail « SygixOs devient l'écran d'accueil de la TV », qui affiche l'état du rôle HOME et en déclenche la demande (« Rôle d'écran d'accueil » de `launcher-shell`) ; « Démarrer à l'allumage », détail « Ouvrir SygixOs quand la TV s'allume », avec un switch Apple comme « Afficher Up Next » (« Démarrage à l'allumage » de `launcher-shell`) ; « Retour à l'accueil », détail « Revenir à SygixOs quand la touche Home ouvre l'accueil du constructeur », pour le service d'accessibilité (« Retour Home par le service d'accessibilité » de `launcher-shell`). Aucune autre catégorie ne SHALL porter ces contrôles. Les lignes SHALL avoir le rendu des lignes de « Page de réglages » (pilule de focus, sans zoom) ; libellés, détails et états SHALL être les mêmes chaînes que dans la présentation initiale de `launcher-shell`. « Remplacer le launcher » et « Retour à l'accueil » SHALL afficher à droite l'un des états suivants : « Actif » (rôle détenu ou service activé, selon le système), précédé d'une pastille ronde verte ; « En attente… » (action système en attente), précédé d'une pastille ronde bleue ; « Inactif », sans pastille ; « Indisponible » (capacité absente de l'appareil sans écran système de remplacement, ou aucun écran système ne peut s'ouvrir), sans pastille, avec le titre atténué hors focus et, à la place du détail, la raison : « Cette TV ne permet pas de changer l'écran d'accueil » pour le rôle, « Les réglages d'accessibilité ne peuvent pas s'ouvrir sur cette TV » pour l'accessibilité ; une ligne « Indisponible » reste focalisable, sa raison lisible au focus, et OK y est sans effet. L'état « En attente… » SHALL commencer quand SygixOs ouvre un dialogue ou un écran système pour ce contrôle et SHALL prendre fin au retour au premier plan suivant, qui relit l'état réel ; il n'est jamais persisté ni présenté comme un succès. « Démarrer à l'allumage » SHALL être un réglage activé ou désactivé, persisté dans DataStore, désactivé par défaut ; quand l'écran système de la permission d'affichage superposé est absent ou n'a pas pu s'ouvrir (« Permission d'affichage superposé pour le démarrage » de `launcher-shell`) et que l'option est désactivée, la ligne SHALL afficher « Indisponible » à la place du switch, sans pastille, avec le titre atténué hors focus et la raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage » à la place du détail, OK y étant sans effet ; quand l'état « démarrage automatique non observé » de `launcher-shell` est vrai, son détail SHALL devenir « SygixOs ne s'est pas ouvert automatiquement à ce démarrage », dans la couleur du détail normal, le switch restant activé, sans autre indicateur. Navigation D-pad : haut et bas parcourent toutes les lignes de la catégorie sans boucle (« Afficher Up Next », « Position d'Up Next », « Remplacer le launcher », « Démarrer à l'allumage », « Retour à l'accueil », puis la ligne de « Réglage de l'accueil d'origine »), l'intertitre étant sauté ; droite est sans effet ; gauche et Retour se comportent comme pour « Afficher Up Next ». Dimensions et jetons : maquettes Penpot 8.5 et 8.6 et `design.md` (D10).

#### Scenario: emplacement
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil »
- **THEN** le volet droit présente, dans cet ordre, « Afficher Up Next », « Position d'Up Next », l'intertitre « Launcher système », « Remplacer le launcher », « Démarrer à l'allumage », « Retour à l'accueil » puis la ligne de « Réglage de l'accueil d'origine », chacune avec son détail et son état ; le focus initial reste sur « Afficher Up Next » ; aucune autre catégorie (dont « Apps sources ») ne les contient et aucune nouvelle catégorie n'apparaît dans le volet gauche

#### Scenario: navigation D-pad des lignes P5
- **WHEN** le focus est sur « Position d'Up Next » (liste fermée) et l'utilisateur presse bas quatre fois, puis bas encore, puis haut cinq fois, puis droite, puis gauche
- **THEN** le focus passe à « Remplacer le launcher » (l'intertitre n'est jamais focalisé), « Démarrer à l'allumage », « Retour à l'accueil », la ligne de « Réglage de l'accueil d'origine », puis y reste (dernière ligne, sans boucle) ; il remonte jusqu'à « Afficher Up Next » ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » ; Retour depuis l'une de ces lignes ferme les réglages (« Page de réglages », scénario « retour »)

#### Scenario: rendu des états
- **WHEN** « Remplacer le launcher » ou « Retour à l'accueil » est affiché dans chacun de ses états
- **THEN** « Actif » et « En attente… » sont précédés d'une pastille ronde, verte puis bleue ; « Inactif » et « Indisponible » n'en ont pas ; en « Indisponible », le titre est atténué hors focus et le détail est remplacé par la raison propre au contrôle ; le libellé d'état et sa couleur suivent la pilule de focus de la ligne

#### Scenario: action système en attente
- **WHEN** l'utilisateur valide « Remplacer le launcher » ou le contrôle d'accessibilité et que l'écran système correspondant s'ouvre
- **THEN** le contrôle passe à l'état « En attente… » jusqu'au retour au premier plan, sans jamais afficher « Actif » avant la relecture

#### Scenario: état relu au retour
- **WHEN** SygixOs revient au premier plan après un dialogue ou un écran système, ou après tout passage en arrière-plan
- **THEN** l'état du rôle HOME et celui du service d'accessibilité sont relus auprès du système et affichés, l'état « En attente… » disparaît et le focus est sur le contrôle qui a ouvert l'écran système

#### Scenario: écran système impossible à ouvrir
- **WHEN** les réglages d'accessibilité du système ne peuvent pas s'ouvrir, ou que ni le dialogue du rôle HOME ni l'écran système des applications par défaut ou de l'écran d'accueil ne peuvent s'ouvrir (« Rôle d'écran d'accueil » de `launcher-shell`)
- **THEN** le contrôle affiche « Indisponible » avec sa raison à la place du détail, sans crash, le focus reste sur lui, OK n'y a plus d'effet, et les autres contrôles et catégories restent utilisables

#### Scenario: service d'accessibilité depuis les réglages
- **WHEN** l'utilisateur valide le contrôle d'accessibilité, que le service soit actif ou non
- **THEN** les réglages d'accessibilité du système s'ouvrent ; SygixOs n'active ni ne désactive le service lui-même ; un service déjà activé reste affiché « Actif » au retour tant que le système le déclare activé

#### Scenario: bascule du démarrage à l'allumage
- **WHEN** l'utilisateur presse OK sur « Démarrer à l'allumage »
- **THEN** le switch bascule avec l'animation Apple, l'état est persisté (DataStore) et restauré au lancement suivant, et le focus reste sur la ligne ; à l'activation, la confirmation de « Permission d'affichage superposé pour le démarrage » de `launcher-shell` peut s'afficher, et le focus revient sur la ligne à sa fermeture

#### Scenario: démarrage automatique non observé
- **WHEN** la catégorie est affichée alors que l'option est activée et que l'état « démarrage automatique non observé » de `launcher-shell` est vrai pour le démarrage courant
- **THEN** le détail de « Démarrer à l'allumage » est « SygixOs ne s'est pas ouvert automatiquement à ce démarrage », son switch reste activé et aucun autre indicateur n'est ajouté (maquette 8.6)

#### Scenario: démarrage automatique observé ou option désactivée
- **WHEN** l'ouverture au démarrage a été observée pour le démarrage courant, ou que l'option est désactivée
- **THEN** le détail de « Démarrer à l'allumage » est « Ouvrir SygixOs quand la TV s'allume »
