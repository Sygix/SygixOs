# Spec Delta

La catégorie « Écran d'accueil » (`SettingsCategory.HOME_SCREEN`) est introduite par le change `up-next`, archivé avant celui-ci. Ce delta y ajoute les contrôles P5 sans créer de catégorie ni modifier les exigences d'`up-next`.

## ADDED Requirements

### Requirement: Réglages du launcher système
La catégorie « Écran d'accueil » SHALL contenir, sans changer son focus initial (« Afficher Up Next » reste son premier contrôle), trois contrôles distincts : « Remplacer le launcher », qui affiche l'état du rôle HOME et en déclenche la demande (« Rôle d'écran d'accueil » de `launcher-shell`) ; l'option de démarrage à l'allumage (« Démarrage à l'allumage » de `launcher-shell`) ; le contrôle du service d'accessibilité (« Retour Home par le service d'accessibilité » de `launcher-shell`). Aucune autre catégorie ne SHALL porter ces contrôles. « Remplacer le launcher » et le contrôle d'accessibilité SHALL afficher l'un des états suivants, partagés avec la présentation initiale : « actif » (rôle détenu ou service activé, selon le système), « inactif », « indisponible » (capacité absente de l'appareil, ou écran système impossible à ouvrir) et « action système en attente ». L'état « action système en attente » SHALL commencer quand SygixOs ouvre un dialogue ou un écran système pour ce contrôle et SHALL prendre fin au retour au premier plan suivant, qui relit l'état réel ; il n'est jamais persisté ni présenté comme un succès. L'option de démarrage à l'allumage SHALL être un réglage activé ou désactivé, persisté dans DataStore, désactivé par défaut ; quand l'état « démarrage automatique non observé » de `launcher-shell` est vrai, le contrôle SHALL l'indiquer. Le rendu visuel, les libellés définitifs autres que « Remplacer le launcher », la place des trois contrôles dans la catégorie et leur navigation D-pad détaillée suivent les maquettes Penpot à venir ; gauche et Retour se comportent comme pour les autres contrôles de la catégorie (« Afficher Up Next »).

#### Scenario: emplacement
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil »
- **THEN** les trois contrôles P5 y figurent, distincts, avec les réglages Up Next ; le focus initial reste sur « Afficher Up Next » ; aucune autre catégorie (dont « Apps sources ») ne les contient et aucune nouvelle catégorie n'apparaît dans le volet gauche

#### Scenario: action système en attente
- **WHEN** l'utilisateur valide « Remplacer le launcher » ou le contrôle d'accessibilité et que l'écran système correspondant s'ouvre
- **THEN** le contrôle passe à l'état « action système en attente » jusqu'au retour au premier plan, sans jamais afficher « actif » avant la relecture

#### Scenario: état relu au retour
- **WHEN** SygixOs revient au premier plan après un dialogue ou un écran système, ou après tout passage en arrière-plan
- **THEN** l'état du rôle HOME et celui du service d'accessibilité sont relus auprès du système et affichés, l'état « action système en attente » disparaît et le focus est sur le contrôle qui a ouvert l'écran système

#### Scenario: écran système impossible à ouvrir
- **WHEN** le dialogue du rôle HOME ou les réglages d'accessibilité du système ne peuvent pas s'ouvrir
- **THEN** le contrôle affiche « indisponible », sans crash, le focus reste sur lui et les autres contrôles et catégories restent utilisables

#### Scenario: service d'accessibilité depuis les réglages
- **WHEN** l'utilisateur valide le contrôle d'accessibilité, que le service soit actif ou non
- **THEN** les réglages d'accessibilité du système s'ouvrent ; SygixOs n'active ni ne désactive le service lui-même

#### Scenario: bascule du démarrage à l'allumage
- **WHEN** l'utilisateur presse OK sur l'option de démarrage à l'allumage
- **THEN** l'option bascule, l'état est persisté (DataStore) et restauré au lancement suivant, et le focus reste sur l'option

#### Scenario: démarrage automatique non observé
- **WHEN** la catégorie est affichée alors que l'option est activée et que l'état « démarrage automatique non observé » de `launcher-shell` est vrai pour le démarrage courant
- **THEN** le contrôle de démarrage indique que le démarrage automatique n'a pas eu lieu, sans masquer l'option ni la désactiver

#### Scenario: démarrage automatique observé ou option désactivée
- **WHEN** l'ouverture au démarrage a été observée pour le démarrage courant, ou que l'option est désactivée
- **THEN** le contrôle de démarrage n'affiche aucune indication d'échec
