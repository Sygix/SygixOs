# Spec Delta

La catégorie « Écran d'accueil » (`SettingsCategory.HOME_SCREEN`) est introduite par le change `up-next`, archivé avant celui-ci. Ce delta y ajoute les contrôles P5 sans créer de catégorie ni modifier les exigences d'`up-next`.

## ADDED Requirements

### Requirement: Réglages du launcher système
La catégorie « Écran d'accueil » SHALL contenir, après « Afficher Up Next » et « Position d'Up Next » et sans changer son focus initial (« Afficher Up Next » reste son premier contrôle), un intertitre « Launcher système » jamais focusable puis trois lignes de réglage, dans cet ordre : « Remplacer le launcher », détail « SygixOs devient l'écran d'accueil de la TV », qui affiche l'état du rôle HOME et en déclenche la demande (« Rôle d'écran d'accueil » de `launcher-shell`) ; « Démarrer à l'allumage », détail « Ouvrir SygixOs quand la TV s'allume », avec un switch Apple comme « Afficher Up Next » (« Démarrage à l'allumage » de `launcher-shell`) ; « Retour à l'accueil », détail « Revenir à SygixOs quand la touche Home ouvre l'accueil du constructeur », pour le service d'accessibilité (« Retour Home par le service d'accessibilité » de `launcher-shell`). Aucune autre catégorie ne SHALL porter ces contrôles. Les lignes SHALL avoir le rendu des lignes de « Page de réglages » (pilule de focus, sans zoom) ; libellés, détails et états SHALL être les mêmes chaînes que dans la présentation initiale de `launcher-shell`. « Remplacer le launcher » et « Retour à l'accueil » SHALL afficher à droite l'un des états suivants : « Actif » (rôle détenu ou service activé, selon le système), précédé d'une pastille ronde verte ; « En attente… » (action système en attente), précédé d'une pastille ronde bleue ; « Inactif », sans pastille ; « Indisponible » (capacité absente de l'appareil sans écran système de remplacement, ou aucun écran système ne peut s'ouvrir), sans pastille, avec le titre atténué hors focus et, à la place du détail, la raison : « Cette TV ne permet pas de changer l'écran d'accueil » pour le rôle, « Les réglages d'accessibilité ne peuvent pas s'ouvrir sur cette TV » pour l'accessibilité ; une ligne « Indisponible » reste focalisable, sa raison lisible au focus, et OK y est sans effet. L'état « En attente… » SHALL commencer quand SygixOs ouvre un dialogue ou un écran système pour ce contrôle et SHALL prendre fin au retour au premier plan suivant, qui relit l'état réel ; il n'est jamais persisté ni présenté comme un succès. « Démarrer à l'allumage » SHALL être un réglage activé ou désactivé, persisté dans DataStore, désactivé par défaut ; quand l'écran système de la permission d'affichage superposé est absent ou n'a pas pu s'ouvrir (« Permission d'affichage superposé pour le démarrage » de `launcher-shell`) et que l'option est désactivée, la ligne SHALL afficher « Indisponible » à la place du switch, sans pastille, avec le titre atténué hors focus et la raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage » à la place du détail, OK y étant sans effet ; quand l'état « démarrage automatique non observé » de `launcher-shell` est vrai, son détail SHALL devenir « SygixOs ne s'est pas ouvert automatiquement à ce démarrage », dans la couleur du détail normal, le switch restant activé, sans autre indicateur. Navigation D-pad : haut et bas parcourent toutes les lignes de la catégorie sans boucle (« Afficher Up Next », « Position d'Up Next », « Remplacer le launcher », « Démarrer à l'allumage », « Retour à l'accueil »), l'intertitre étant sauté ; droite est sans effet ; gauche et Retour se comportent comme pour « Afficher Up Next ». Dimensions et jetons : maquettes Penpot 8.5 et 8.6 et `design.md` (D10).

#### Scenario: emplacement
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil »
- **THEN** le volet droit présente, dans cet ordre, « Afficher Up Next », « Position d'Up Next », l'intertitre « Launcher système », « Remplacer le launcher », « Démarrer à l'allumage » et « Retour à l'accueil », chacune avec son détail et son état ; le focus initial reste sur « Afficher Up Next » ; aucune autre catégorie (dont « Apps sources ») ne les contient et aucune nouvelle catégorie n'apparaît dans le volet gauche

#### Scenario: navigation D-pad des lignes P5
- **WHEN** le focus est sur « Position d'Up Next » (liste fermée) et l'utilisateur presse bas trois fois, puis bas encore, puis haut quatre fois, puis droite, puis gauche
- **THEN** le focus passe à « Remplacer le launcher » (l'intertitre n'est jamais focalisé), « Démarrer à l'allumage », « Retour à l'accueil », puis y reste (dernière ligne, sans boucle) ; il remonte jusqu'à « Afficher Up Next » ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » ; Retour depuis l'une de ces lignes ferme les réglages (« Page de réglages », scénario « retour »)

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
