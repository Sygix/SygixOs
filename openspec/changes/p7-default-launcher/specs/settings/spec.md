# Spec Delta

Le parcours, le panneau « Launcher principal » et leurs effets sont décrits par `launcher-shell` (« Parcours de l'écran d'accueil », « Panneau Launcher principal ») ; ce delta ne décrit que la ligne de la catégorie « Écran d'accueil » (maquette 8.15).

## MODIFIED Requirements

### Requirement: Réglages du launcher système
La catégorie « Écran d'accueil » SHALL contenir, après « Afficher À suivre » et « Position d'À suivre » et sans changer son focus initial (« Afficher À suivre » reste son premier contrôle), un intertitre « Launcher système » jamais focusable puis une seule ligne de réglage, « Configurer SygixOs comme launcher principal », qui ouvre le panneau « Launcher principal » par-dessus la page de réglages (`launcher-shell`, maquette 8.16) ; à sa fermeture, le focus SHALL revenir sur cette ligne. Aucune autre ligne (rôle HOME, démarrage à l'allumage, service d'accessibilité, débogage, accueil d'origine) et aucune autre catégorie ne SHALL porter ces réglages. La ligne SHALL avoir le rendu des lignes de « Page de réglages » (pilule de focus, sans zoom). Son détail SHALL être « Automatique : <état> · Par autorisations : <état> », avec l'état de chaque voie en minuscules : pour la voie automatique « actif », « à configurer » ou « indisponible sur cette TV » ; pour la voie par autorisations « active », « en partie », « bloquée », « non utilisée » ou « à faire » (états définis par « Configuration automatique de l'écran d'accueil » et « Page Par autorisations » de `launcher-shell`). Son état à droite SHALL être : « Actif », précédé d'une pastille ronde verte, quand l'une des voies est active ; sinon « Bloqué » quand la voie par autorisations est bloquée ; sinon « En partie » quand elle est en partie faite ; sinon « À configurer ». L'état et le détail SHALL être recalculés à chaque retour au premier plan, à chaque fermeture du panneau et à la fin de chaque séquence de la configuration automatique. Navigation D-pad : haut et bas parcourent toutes les lignes de la catégorie sans boucle (« Afficher À suivre », « Position d'À suivre », « Configurer SygixOs comme launcher principal »), l'intertitre étant sauté ; droite est sans effet ; gauche et Retour se comportent comme pour « Afficher À suivre ».

#### Scenario: emplacement
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil »
- **THEN** le volet droit présente, dans cet ordre, « Afficher À suivre », « Position d'À suivre », l'intertitre « Launcher système » et « Configurer SygixOs comme launcher principal » avec son détail et son état ; le focus initial reste sur « Afficher À suivre » ; aucune ligne « SygixOs comme écran d'accueil », « Remplacer le launcher », « Démarrer à l'allumage » ou « Retour à l'accueil » n'existe, et aucune nouvelle catégorie n'apparaît dans le volet gauche

#### Scenario: navigation D-pad de la ligne P5
- **WHEN** le focus est sur « Position d'À suivre » (liste fermée) et l'utilisateur presse bas, puis bas encore, puis haut deux fois, puis bas, droite, puis gauche
- **THEN** le focus passe à « Configurer SygixOs comme launcher principal » (l'intertitre n'est jamais focalisé), puis y reste (dernière ligne, sans boucle) ; il remonte jusqu'à « Afficher À suivre », redescend ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » ; Retour depuis la ligne ferme les réglages (« Page de réglages », scénario « retour »)

#### Scenario: ouverture du parcours
- **WHEN** l'utilisateur presse OK sur « Configurer SygixOs comme launcher principal »
- **THEN** le panneau « Launcher principal » s'affiche par-dessus la page de réglages ; à sa fermeture, la page de réglages est toujours ouverte sur « Écran d'accueil » et la ligne a le focus avec son état et son détail recalculés

#### Scenario: synthèse partielle
- **WHEN** la voie automatique est à configurer et qu'une partie seulement des lignes de la voie par autorisations est faite
- **THEN** la ligne affiche l'état « En partie », sans pastille, et le détail « Automatique : à configurer · Par autorisations : en partie »

#### Scenario: synthèse terminée
- **WHEN** la voie automatique est active et que la voie par autorisations n'est pas utilisée
- **THEN** la ligne affiche l'état « Actif » précédé d'une pastille verte et le détail « Automatique : actif · Par autorisations : non utilisée » (maquette 8.15)

#### Scenario: touche Home bloquée
- **WHEN** la voie par autorisations est bloquée et que la voie automatique est à configurer
- **THEN** la ligne affiche l'état « Bloqué », sans pastille, et le détail « Automatique : à configurer · Par autorisations : bloquée »

#### Scenario: rien de fait
- **WHEN** aucune voie n'est utilisée
- **THEN** la ligne affiche l'état « À configurer », sans pastille, et le détail « Automatique : à configurer · Par autorisations : à faire »

#### Scenario: état relu au retour
- **WHEN** SygixOs revient au premier plan après un écran système ouvert depuis le parcours, après tout passage en arrière-plan, ou à la fin d'une séquence de la configuration automatique
- **THEN** l'écran d'accueil effectif, l'accueil d'origine, le service d'accessibilité, la permission et le rôle HOME sont relus auprès du système et l'état et le détail de la ligne sont recalculés, sans valeur périmée
