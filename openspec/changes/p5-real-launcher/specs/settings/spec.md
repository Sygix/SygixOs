# Spec Delta

La catégorie « Écran d'accueil » (`SettingsCategory.HOME_SCREEN`) est introduite par le change `up-next`, archivé avant celui-ci. Ce delta y ajoute la ligne P5 sans créer de catégorie ni modifier les exigences d'`up-next`.

## ADDED Requirements

### Requirement: Réglages du launcher système
La catégorie « Écran d'accueil » SHALL contenir, après « Afficher À suivre » et « Position d'À suivre » et sans changer son focus initial (« Afficher À suivre » reste son premier contrôle), un intertitre « Launcher système » jamais focusable puis une seule ligne de réglage, « SygixOs comme écran d'accueil », qui ouvre le parcours guidé par-dessus la page de réglages (« Parcours guidé du launcher » de `launcher-shell`) ; à la fermeture du parcours, le focus SHALL revenir sur cette ligne. Aucune autre ligne P5 (rôle HOME, démarrage à l'allumage, service d'accessibilité) et aucune autre catégorie ne SHALL porter ces réglages : ils ne se règlent que dans le parcours guidé. La ligne SHALL avoir le rendu des lignes de « Page de réglages » (pilule de focus, sans zoom) et SHALL résumer l'état du parcours, recalculé à chaque retour au premier plan et à chaque fermeture du parcours, à partir des étapes obligatoires affichées et non « Indisponible » (étapes 1, 2 et 2b ; l'étape 3, facultative, n'entre jamais dans la synthèse) : état « Terminé », précédé d'une pastille ronde verte, et détail « Toutes les étapes sont faites » quand elles sont toutes « Fait » ; état « Bloqué » et détail « Touche Home bloquée par la TV » quand l'étape 1 est « Bloqué », quel que soit le reste ; sinon état « Partiel » quand au moins une étape est « Fait », « À faire » quand aucune ne l'est, avec le détail de ce qui reste : « Touche Home à terminer » (étape 1), « Démarrage à l'allumage à terminer » (étape 2 désactivée ou 2b non faite), ou « Touche Home et démarrage à l'allumage à terminer » ; état « Indisponible », sans pastille, avec le détail « Cette TV ne permet pas ces réglages », quand aucune étape obligatoire n'est disponible (OK ouvre quand même le parcours). Navigation D-pad : haut et bas parcourent toutes les lignes de la catégorie sans boucle (« Afficher À suivre », « Position d'À suivre », « SygixOs comme écran d'accueil »), l'intertitre étant sauté ; droite est sans effet ; gauche et Retour se comportent comme pour « Afficher À suivre ». Dimensions et jetons : maquette Penpot 8.5 et `design.md` (D10).

#### Scenario: emplacement
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil »
- **THEN** le volet droit présente, dans cet ordre, « Afficher À suivre », « Position d'À suivre », l'intertitre « Launcher système » et « SygixOs comme écran d'accueil » avec son détail et son état ; le focus initial reste sur « Afficher À suivre » ; aucune ligne « Remplacer le launcher », « Démarrer à l'allumage » ou « Retour à l'accueil » n'existe, et aucune nouvelle catégorie n'apparaît dans le volet gauche

#### Scenario: navigation D-pad de la ligne P5
- **WHEN** le focus est sur « Position d'À suivre » (liste fermée) et l'utilisateur presse bas, puis bas encore, puis haut deux fois, puis bas, droite, puis gauche
- **THEN** le focus passe à « SygixOs comme écran d'accueil » (l'intertitre n'est jamais focalisé), puis y reste (dernière ligne, sans boucle) ; il remonte jusqu'à « Afficher À suivre », redescend ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » ; Retour depuis la ligne ferme les réglages (« Page de réglages », scénario « retour »)

#### Scenario: ouverture du parcours
- **WHEN** l'utilisateur presse OK sur « SygixOs comme écran d'accueil »
- **THEN** le parcours guidé s'affiche par-dessus la page de réglages (maquette 8.6) ; à sa fermeture (« Terminé » ou Retour), il disparaît, la page de réglages est toujours ouverte sur « Écran d'accueil » et la ligne a le focus avec sa synthèse recalculée

#### Scenario: synthèse partielle
- **WHEN** l'étape 1 est « Fait » et que l'option de démarrage est désactivée
- **THEN** la ligne affiche l'état « Partiel », sans pastille, et le détail « Démarrage à l'allumage à terminer » (maquette 8.5)

#### Scenario: synthèse terminée
- **WHEN** l'étape 1 est « Fait », l'option de démarrage activée et la sous-étape 2b « Fait » ou absente, quel que soit l'état du rôle HOME
- **THEN** la ligne affiche l'état « Terminé » précédé d'une pastille verte et le détail « Toutes les étapes sont faites »

#### Scenario: touche Home bloquée
- **WHEN** l'étape 1 est « Bloqué »
- **THEN** la ligne affiche l'état « Bloqué », sans pastille, et le détail « Touche Home bloquée par la TV », quels que soient les autres états

#### Scenario: rien de fait
- **WHEN** aucune étape obligatoire n'est « Fait »
- **THEN** la ligne affiche l'état « À faire » et le détail « Touche Home et démarrage à l'allumage à terminer » (ou la seule étape disponible qui reste)

#### Scenario: état relu au retour
- **WHEN** SygixOs revient au premier plan après un écran système ouvert depuis le parcours, ou après tout passage en arrière-plan
- **THEN** l'état du service d'accessibilité, de la permission et du rôle HOME est relu auprès du système et la synthèse de la ligne est recalculée, sans valeur périmée
