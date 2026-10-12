# Spec Delta

Le parcours guidé, ses pages, ses dialogues et leurs effets sont décrits par `launcher-shell` (« Parcours guidé du launcher », « Configuration automatique de l'écran d'accueil ») ; ce delta ne décrit que la ligne de la catégorie « Écran d'accueil ».

## MODIFIED Requirements

### Requirement: Réglages du launcher système
La catégorie « Écran d'accueil » SHALL contenir, après « Afficher À suivre » et « Position d'À suivre » et sans changer son focus initial (« Afficher À suivre » reste son premier contrôle), un intertitre « Launcher système » jamais focusable puis une seule ligne de réglage, « Configurer SygixOs comme launcher principal », qui ouvre le parcours guidé sur sa page de choix par-dessus la page de réglages (« Parcours guidé du launcher » de `launcher-shell`) ; la configuration automatique peut y être lancée ou relancée et l'accueil d'origine rétabli. À la fermeture du parcours, le focus SHALL revenir sur cette ligne. Aucune autre ligne (rôle HOME, démarrage à l'allumage, service d'accessibilité, débogage, accueil d'origine) et aucune autre catégorie ne SHALL porter ces réglages : ils ne se règlent que dans le parcours guidé. La ligne SHALL avoir le rendu des lignes de « Page de réglages » (pilule de focus, sans zoom). Son détail SHALL donner l'état des deux voies, « Automatique : <état> · Par autorisations : <état> », où l'état de la voie automatique est celui de « Configuration automatique de l'écran d'accueil » (`launcher-shell`) et celui de la voie « Par autorisations » la synthèse de ses étapes obligatoires affichées et non « Indisponible » (étapes 1, 2, 2b et 2c ; l'étape 3, facultative, n'y entre jamais) : « Terminé » quand elles sont toutes « Fait », « Bloqué » quand l'étape 1 est « Bloqué », sinon « Partiel » quand au moins une est « Fait », « À faire » quand aucune ne l'est, « Indisponible » quand aucune n'est disponible. Son état à droite SHALL être : « Configuré », précédé d'une pastille ronde verte, quand la voie automatique est « Configuré » ou la voie « Par autorisations » « Terminé » ; sinon « Partiel » quand l'une des voies est « Partiel » ; sinon « Bloqué » quand la voie « Par autorisations » est « Bloqué » ; sinon « Indisponible », sans pastille, quand les deux voies sont « Indisponible » (OK ouvre quand même le parcours) ; sinon « À faire ». L'état et le détail SHALL être recalculés à chaque retour au premier plan, à chaque fermeture du parcours et à la fin de chaque séquence de la configuration automatique. Navigation D-pad : haut et bas parcourent toutes les lignes de la catégorie sans boucle (« Afficher À suivre », « Position d'À suivre », « Configurer SygixOs comme launcher principal »), l'intertitre étant sauté ; droite est sans effet ; gauche et Retour se comportent comme pour « Afficher À suivre ». Dimensions et jetons : rangée « 8. Launcher principal » de la maquette Penpot et `design.md` (D7).

#### Scenario: emplacement
- **WHEN** l'utilisateur ouvre la catégorie « Écran d'accueil »
- **THEN** le volet droit présente, dans cet ordre, « Afficher À suivre », « Position d'À suivre », l'intertitre « Launcher système » et « Configurer SygixOs comme launcher principal » avec son détail et son état ; le focus initial reste sur « Afficher À suivre » ; aucune ligne « SygixOs comme écran d'accueil », « Remplacer le launcher », « Démarrer à l'allumage », « Retour à l'accueil » ou « Accueil d'origine » n'existe, et aucune nouvelle catégorie n'apparaît dans le volet gauche

#### Scenario: navigation D-pad de la ligne P5
- **WHEN** le focus est sur « Position d'À suivre » (liste fermée) et l'utilisateur presse bas, puis bas encore, puis haut deux fois, puis bas, droite, puis gauche
- **THEN** le focus passe à « Configurer SygixOs comme launcher principal » (l'intertitre n'est jamais focalisé), puis y reste (dernière ligne, sans boucle) ; il remonte jusqu'à « Afficher À suivre », redescend ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » ; Retour depuis la ligne ferme les réglages (« Page de réglages », scénario « retour »)

#### Scenario: ouverture du parcours
- **WHEN** l'utilisateur presse OK sur « Configurer SygixOs comme launcher principal »
- **THEN** le parcours guidé s'affiche par-dessus la page de réglages sur sa page de choix ; à sa fermeture (« Fermer », « Terminé » ou Retour depuis la page de choix), il disparaît, la page de réglages est toujours ouverte sur « Écran d'accueil » et la ligne a le focus avec son état et son détail recalculés

#### Scenario: rien de fait
- **WHEN** aucune étape de « Par autorisations » n'est « Fait » et que la voie automatique est « À faire »
- **THEN** la ligne affiche l'état « À faire », sans pastille, et le détail « Automatique : À faire · Par autorisations : À faire »

#### Scenario: synthèse terminée
- **WHEN** la voie automatique est « Configuré », quel que soit l'état de « Par autorisations »
- **THEN** la ligne affiche l'état « Configuré » précédé d'une pastille verte et un détail qui commence par « Automatique : Configuré »

#### Scenario: synthèse partielle
- **WHEN** la voie automatique est « Partiel » (solutions de repli posées) et que « Par autorisations » est « Partiel »
- **THEN** la ligne affiche l'état « Partiel », sans pastille, et le détail « Automatique : Partiel · Par autorisations : Partiel »

#### Scenario: touche Home bloquée
- **WHEN** l'étape 1 de « Par autorisations » est « Bloqué » et que la voie automatique est « À faire »
- **THEN** la ligne affiche l'état « Bloqué », sans pastille, et le détail « Automatique : À faire · Par autorisations : Bloqué »

#### Scenario: état relu au retour
- **WHEN** SygixOs revient au premier plan après un écran système ouvert depuis le parcours, après tout passage en arrière-plan, ou à la fin d'une séquence de la configuration automatique
- **THEN** l'écran d'accueil effectif, l'accueil d'origine, le service d'accessibilité, la permission et le rôle HOME sont relus auprès du système et l'état et le détail de la ligne sont recalculés, sans valeur périmée
