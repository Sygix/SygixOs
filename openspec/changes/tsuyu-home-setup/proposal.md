# Proposal

## Why

P7 (`p7-default-launcher`) fait de SygixOs l'écran d'accueil par deux voies : la configuration automatique par l'ADB local de la TV, et la voie par autorisations. Le parcours fonctionne, mais il reste pensé pour quelqu'un qui connaît Android :
- le guide du débogage envoie vers « Options pour les développeurs » avant qu'elles existent ;
- l'utilisateur ne sait pas où il en est ni combien d'étapes restent ;
- sur la TV de référence, l'écran « Encore une étape » propose toujours la désactivation de l'accueil Google TV, avec le bouton « Désactiver » sélectionné. Un utilisateur qui valide tout par OK perd ainsi la touche Réglages de la télécommande, la touche Guide/CH et l'appui long sur Home ;
- une fois le débogage désactivé, comme SygixOs le conseille, plus rien n'est réparable sans refaire tout le guide : si la TV défait un réglage, le service de la touche Home reste « Bloqué » jusqu'à une manipulation dans Accessibilité.

Tsuyu, l'autre projet du propriétaire, arrive en version utilisable. C'est un service d'accès privilégié pour Android TV, compatible avec l'API Shizuku, qui démarre depuis la TV elle-même, sans ordinateur ni téléphone, et redémarre avec elle. Il apporte à SygixOs ce que l'ADB intégré ne peut pas donner : un accès qui reste disponible après la configuration. SygixOs peut alors réparer ses propres réglages et relancer ou rétablir la configuration sans repasser par le guide du débogage. Encore faut-il que Tsuyu soit facile à obtenir : il n'est pas sur le Play Store, et un utilisateur non technique ne sait pas installer un APK.

Ce change simplifie le parcours pour un public non technique et ajoute une troisième façon de faire, Tsuyu, installable en une action depuis la TV.

## What Changes

- **Principe « OK, OK, OK »** : sur chaque écran du parcours, l'élément sélectionné au départ est l'étape conseillée suivante. Valider chaque écran par OK mène à une configuration complète et réversible, sans désactiver l'accueil Google TV. Retour n'est jamais destructif.
- **Repère d'avancement** : les écrans de la configuration automatique affichent « Étape X sur N » au-dessus du titre. N compte les étapes de la façon choisie qui restaient à faire quand le parcours a été ouvert.
- **Guide du débogage en deux temps** : chaque étape affiche son état, relu au retour dans SygixOs. Le bouton ouvre l'écran utile au moment présent : « À propos » tant que les options pour les développeurs sont masquées, puis les options pour les développeurs. Le texte ne promet plus qu'on pourra désactiver le débogage, ce qui serait faux avec Tsuyu.
- **Trois façons, deux niveaux de choix** : le premier écran garde deux choix, « Configuration automatique (recommandée) » et « Par autorisations (sans débogage) ». Dans la configuration automatique, une fois le débogage activé et le port ADB réseau joignable, SygixOs propose Tsuyu :
  - « Avec Tsuyu (recommandé) » : SygixOs installe Tsuyu, le fait démarrer, s'y fait autoriser et l'utilise pour la configuration, puis pour l'entretien. Le débogage reste activé.
  - « Sans Tsuyu » : la configuration automatique actuelle par l'ADB intégré ; le débogage peut être désactivé ensuite.
  La question n'est pas posée quand aucune version de Tsuyu n'est disponible, ni quand Tsuyu est déjà actif.
- **Installation de Tsuyu depuis la TV** : téléchargement de la dernière version publiée de Tsuyu depuis ses releases GitHub publiques, vérification complète, puis installation par le système. La vérification porte sur la taille, l'empreinte SHA-256, le nom de paquet et un certificat de signature identique à celui de SygixOs. Rien n'est installé sans un appui sur OK. Les préversions suivent le réglage « Inclure les préversions » existant.
- **Démarrage et autorisation guidés** : SygixOs ouvre Tsuyu avec les étapes à suivre et détecte son démarrage au retour. Il demande ensuite son autorisation dans Tsuyu, en expliquant quel bouton choisir.
- **Accès par Tsuyu** : les mêmes opérations fixes qu'en P7, exécutées par Tsuyu quand il est actif et que SygixOs y est autorisé. Sinon, l'ADB intégré reste utilisé. Aucune commande libre n'est ajoutée.
- **Réparation automatique (avec Tsuyu seulement)** : au retour au premier plan, SygixOs remet en place deux réglages techniques qu'il avait posés et que la TV a défaits : l'autorisation de démarrage automatique du constructeur, et la liaison du service de la touche Home. Rien d'autre n'est réparé : SygixOs ne refait pas un choix que l'utilisateur a pu défaire lui-même. La réparation est silencieuse, limitée à une tentative par réglage et par démarrage de la TV, et listée dans « Détails ».
- **Accueil Google TV gardé par défaut** : l'écran « Encore une étape » (8.7) devient « Un dernier choix ». Il présente deux lignes expliquées, « Garder l'accueil Google TV (conseillé) », sélectionnée au départ, et « Désactiver l'accueil Google TV ». La désactivation reste proposée, avec sa confirmation inchangée. Le panneau « Launcher principal » permet aussi de la faire plus tard (« Remplacer complètement l'accueil Google TV »).
- **Panneau « Launcher principal »** : une ligne « Tsuyu » s'ajoute, avec son état (actif, à installer, arrêté, à autoriser, à mettre à jour, indisponible) et l'action qui le corrige. Avec Tsuyu actif, « Relancer la configuration » et « Rétablir l'accueil Google TV » ne demandent plus le débogage.
- **Après la configuration** : avec Tsuyu, le conseil « désactivez le débogage » est remplacé par la ligne « Réparation automatique » et par la consigne de garder le débogage activé. Sans Tsuyu, P7 est inchangé.
- **Maquettes** : nouvelle rangée « 9. Parcours simplifié et Tsuyu » (écrans 9.1 à 9.16) sur la page « TV · Launcher principal ». La rangée 8 reste la référence de l'implémentation P7.

## Capabilities

### New Capabilities

- `tsuyu-access` : proposition de Tsuyu dans le parcours, installation vérifiée depuis la TV, démarrage guidé, autorisation de SygixOs, accès privilégié par Tsuyu, réparation automatique et états de Tsuyu.

### Modified Capabilities

- `launcher-shell` (exigences ajoutées par `p7-default-launcher`), toutes en MODIFIED :
  - « Parcours de l'écran d'accueil » : principe OK, repère d'avancement, branche Tsuyu.
  - « Accès par le débogage » : choix du moyen d'accès, test du port avant la proposition de Tsuyu.
  - « Écrans du débogage » : guide en deux temps.
  - « Configuration automatique de l'écran d'accueil » : « Un dernier choix », variantes avec Tsuyu de 8.6 et 8.9.
  - « Panneau Launcher principal » : ligne Tsuyu, action de remplacement.
  - « Désactivation de l'accueil d'origine » : déclenchée depuis « Un dernier choix » ou depuis le panneau.
  - « Rétablissement de l'accueil d'origine » : sans débogage avec Tsuyu.
  - « Opérations privilégiées limitées » : deux moyens d'accès, séquences de réparation.
  - « Détails des modifications » : Tsuyu et réparations.
  - « Débogage après la configuration » : cas Tsuyu.
- `ui-testing` :
  - ADDED : « Couverture du parcours avec Tsuyu », « Sélecteurs de Tsuyu ».
  - MODIFIED : « Sélecteurs de l'écran d'accueil », pour la navigation de « Un dernier choix » et le guide du débogage. Cette exigence a été ajoutée par `p7-default-launcher`.
- `settings` : aucune modification. La ligne des réglages garde sa synthèse des deux voies : Tsuyu est un moyen de la voie automatique, pas une voie à part.
- `self-update` : aucune modification. L'installation de Tsuyu reprend sa source, son téléchargement vérifié et sa session d'installation sans en changer le comportement ; elle est décrite par `tsuyu-access`.

### Dépendances

- `p7-default-launcher` : implémenté sur `main`, pas encore archivé (validation TV 6.2). Il doit être archivé avant ce change, dont les deltas MODIFIED portent sur ses exigences (`design.md`, « Ordre d'archivage »).
- Tsuyu (dépôt public Sygix/Tsuyu), à partir d'une version qui :
  - publie un asset `tsuyu.apk` dans ses releases ;
  - fournit l'API Shizuku (version 13) aux applications qui la déclarent ;
  - demande à l'écran l'accord pour chaque application ;
  - redémarre avec la TV.
  Ce change ne modifie pas Tsuyu. Les attentes de SygixOs envers Tsuyu sont listées dans `design.md` (D2).

## Impact

- **Dépendances** : l'API cliente Shizuku (`dev.rikka.shizuku:api` et `dev.rikka.shizuku:provider`, MIT, compatibles AGPL-3.0).
- **Manifeste** : déclaration de l'API Shizuku et de son fournisseur, visibilité du paquet de Tsuyu.
- **Code** :
  - un second moyen d'accès derrière la passerelle d'opérations privilégiées de P7, qui avait été prévue pour ce cas ;
  - un service utilisateur exécuté par Tsuyu, limité aux mêmes opérations fixes ;
  - la généralisation de la source de releases, du téléchargement vérifié et de l'installation de `self-update` à un second dépôt et un second paquet ;
  - un contrôleur de Tsuyu et de la réparation.
- **Interface** : nouveaux écrans et textes du parcours, chaînes françaises, tests JUnit et Compose.
- **Documentation** : le README décrit Tsuyu, le débogage qui reste activé et la réparation automatique. Les documents de feuille de route sont mis à jour.
- **Réseau** : aucun serveur propre, uniquement l'API publique des releases GitHub.

## Non-goals

- **Mise à jour régulière de Tsuyu par SygixOs** : hors de ce change. SygixOs ne propose de remplacer Tsuyu que lorsque la version installée est trop ancienne pour l'API attendue (« À mettre à jour »). Les mises à jour courantes relèvent de Tsuyu (backlog de Tsuyu).
- **Modifier Tsuyu** : aucun changement de Tsuyu n'est requis. Les améliorations souhaitables côté Tsuyu sont des suggestions (`design.md`, « Suggestions pour Tsuyu »).
- **Utiliser l'accès de Tsuyu pour autre chose que l'écran d'accueil** (applications cachées, arrêt d'applications, recherche) : sans phase prévue ; chaque usage fera l'objet de son propre change.
- **Réparer ce que l'utilisateur a pu défaire lui-même** : l'accueil Google TV réactivé, le service d'accessibilité désactivé, la superposition retirée ou l'option d'allumage désactivée ne sont pas réparés. La décision 9 de P7 est maintenue : l'état relu est affiché, sans réapplication.
- **TV sans port ADB réseau** : Tsuyu y est indisponible, comme la voie automatique, et la voie par autorisations reste proposée.
- **Autres gestionnaires compatibles Shizuku** : SygixOs utilise tout gestionnaire qui lui fournit l'API Shizuku et l'autorise, mais il ne propose d'installer que Tsuyu.
- Recherche : P8. BetaSeries : P9.

## Questions ouvertes

Les choix produit de ce change ont été faits en autonomie, à la demande du propriétaire (2026-10-17). Ils sont listés pour validation dans `design.md`, section « Décisions à valider ». Ils deviennent définitifs à la validation de la PR, ou sont corrigés partout s'ils sont refusés. Il ne reste aucune question ouverte en dehors de cette liste.
