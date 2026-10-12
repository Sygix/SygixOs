# Proposal

## Why

P5 fait de SygixOs un launcher candidat, lui fait demander le rôle HOME et propose des rustines (service d'accessibilité pour la touche Home, ouverture à l'allumage avec la permission d'affichage superposé). Sur Google TV, ce n'est pas suffisant : l'accueil Google TV garde la priorité, le système le rouvre après l'attribution du rôle, et sur la TV de référence (TCL, Android 14) le gestionnaire d'auto-démarrage du constructeur bloque, après un redémarrage complet, la diffusion de fin de démarrage et la liaison du service d'accessibilité de toute application tierce qui n'a pas l'autorisation `AUTO_START` (constat de v0.1.0-rc.2). Cette autorisation n'est accordable que par le shell ADB ; l'écran du constructeur qui l'accordait est masqué sur les firmwares récents.

Les launchers et utilitaires qui tiennent sur ces TV (Launcher Manager, TvQuickActions) n'utilisent ni root ni Shizuku : ils se connectent eux-mêmes au débogage ADB de la TV, une fois le débogage activé par l'utilisateur et leur clé acceptée, puis exécutent quelques commandes du shell. Shizuku et Shevery ne sont pas utilisables simplement sur une TV : leur association par le débogage sans fil passe par une notification de code qui n'apparaît pas sur Google TV et s'interrompt dès qu'on change d'application, ce qui exige un ordinateur.

Le parcours P5 présente en outre la mécanique (un service, une permission par étape) plutôt que le but. P7 rend le but explicite : faire de SygixOs l'écran d'accueil, automatiquement quand le débogage est disponible, sinon par des autorisations guidées dont la limite est annoncée.

Terme : l'**accueil d'origine** désigne l'écran d'accueil fourni avec la TV (launcher Google TV ou Android TV) et le composant système qui le réactive.

## What Changes

- Le parcours guidé « Faire de SygixOs l'écran d'accueil » devient un choix entre deux voies, présenté au premier lancement (avec « Plus tard ») et rouvert depuis les réglages :
  - **Configuration automatique** (recommandée) : un guide en étapes numérotées fait activer le débogage, SygixOs se connecte à l'ADB de l'appareil et l'utilisateur accepte sa clé une fois ; SygixOs enchaîne ensuite, sur une seule action, l'autorisation de démarrage automatique du constructeur, sa définition comme écran d'accueil par le système puis la vérification de l'écran d'accueil effectif. Si l'accueil d'origine garde la main, SygixOs propose de le désactiver, avec une confirmation qui dit ce qui sera perdu ; si l'utilisateur refuse ou que c'est impossible, SygixOs pose automatiquement les solutions de repli (service d'accessibilité pour la touche Home, affichage superposé et ouverture à l'allumage).
  - **Par autorisations** : les étapes P5 de la touche Home et de l'ouverture à l'allumage, nommées par leur effet, plus l'écran d'auto-démarrage du constructeur quand la TV en a un, avec la limite annoncée : sur certaines TV, l'accueil Google TV peut reprendre la main après un redémarrage complet. L'étape facultative « Définir SygixOs par défaut » disparaît : l'écran d'accueil est défini par la configuration automatique.
- Les réglages gardent une seule ligne, « Configurer SygixOs comme launcher principal », qui résume l'état des deux voies et ouvre le panneau « Launcher principal » : état de chaque voie, « Relancer la configuration », « Rétablir l'accueil Google TV ».
- Un lien « Détails » liste, en clair, ce que SygixOs a modifié ou va modifier (opérations, composants, autorisations) ; l'interface principale ne parle que des effets pour l'utilisateur.
- Sur les TV TCL dont l'autorisation de démarrage se réinitialise, la configuration automatique ajoute aussi SygixOs aux listes blanches de la politique d'auto-démarrage du constructeur, après sauvegarde de la valeur d'origine et avec relecture ; une mise à jour du constructeur peut l'écraser, la configuration peut être relancée.
- Sur une TV dont le débogage n'expose pas de port ADB réseau, la voie automatique est « Indisponible sur cette TV » et renvoie à la voie « Par autorisations ».
- « Rétablir l'accueil Google TV » réactive l'accueil d'origine et rend l'écran d'accueil précédent ; les autorisations accordées restent, révocables dans Android.
- Les opérations privilégiées sont fixes et limitées au paquet SygixOs et à l'ensemble fermé des composants de l'accueil d'origine ; aucune commande libre. La connexion ADB est ouverte seulement pendant une séquence demandée par l'utilisateur et sa clé ne quitte pas l'appareil.
- Après une configuration réussie, SygixOs conseille de désactiver le débogage et explique qu'il faudra le réactiver pour rétablir l'accueil d'origine depuis SygixOs.
- L'état (écran d'accueil effectif, accueil d'origine, rustines) est relu auprès du système à chaque retour au premier plan, sans débogage.
- Garde-fous repris de la version précédente de ce change : rien n'est désactivé avant la fin de la configuration initiale de l'appareil ni si SygixOs n'est pas un écran d'accueil activé ; une désactivation partielle, ou qui ne fait pas de SygixOs l'écran d'accueil effectif, est annulée ; délais bornés.
- Vérifications sur la TV de référence avant implémentation : ADB local, autorisation de démarrage du constructeur et sa persistance, définition de l'écran d'accueil sans désactivation, désactivation et ses pertes, rustines par le shell, écrans d'auto-démarrage du constructeur.
- Feuille de route : P7 devient « Écran d'accueil par défaut » ; la recherche reste P8 et BetaSeries P9. Le change est renommé `p7-default-launcher` (anciennement `p7-shizuku`).

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : parcours de l'écran d'accueil, accès par le débogage, écrans du débogage, configuration automatique, page « Par autorisations », panneau « Launcher principal », état de l'écran d'accueil, désactivation et rétablissement de l'accueil d'origine, démarrage automatique du constructeur, opérations privilégiées limitées, détails des modifications, débogage après la configuration (ADDED) ; démarrage à l'allumage, permission d'affichage superposé et service d'accessibilité (MODIFIED) ; « Parcours guidé du launcher » et « Rôle d'écran d'accueil » (REMOVED, remplacés).
- `settings` : « Réglages du launcher système » (MODIFIED : ligne « Configurer SygixOs comme launcher principal », synthèse des deux voies, ouverture du panneau « Launcher principal »).
- `ui-testing` : couverture du parcours de l'écran d'accueil, couverture de la configuration automatique, sélecteurs de l'écran d'accueil (ADDED) ; « Couverture des parcours du launcher système » et « Sélecteurs du launcher système » (REMOVED, remplacées).
- `up-next` : MODIFIED « Modèle canonique » et « Dédoublonnage en niveaux » ; l'enrichissement BetaSeries devient P9 après l'insertion de P7 et de la recherche en P8.

### Dépendances

- `p5-real-launcher` (archivé sur `main`) : fournit le parcours guidé, les rustines et la ligne des réglages que P7 modifie.
- `up-next` et `jellyfin-tvprovider-only` (archivés sur `main`) : fournissent la catégorie « Écran d'accueil ».

## Impact

Nouvelle dépendance : un client ADB en Kotlin pour la connexion locale (`dev.mobile:dadb`, Apache-2.0, compatible AGPL-3.0), retenu après la tâche 1.1. Manifeste (visibilité des composants de l'accueil d'origine et des écrans d'auto-démarrage du constructeur), passerelle d'accès privilégié à opérations fixes, contrôleur et états dans le domaine, pages du parcours, confirmations et détails, chaînes françaises, tests JUnit et Compose, README (configuration automatique, débogage, rétablissement, procédure ADB manuelle conservée). Aucune dépendance réseau hors de l'appareil, aucun serveur. Documents de feuille de route mis à jour.

## Non-goals

- Shizuku et ses gestionnaires compatibles : la passerelle d'accès privilégié est conçue pour accueillir plus tard un second moyen d'accès (gestionnaire compatible Shizuku utilisable sur TV), hors P7.
- Association par le débogage sans fil (appairage par code) : hors P7 ; P7 utilise le port ADB réseau ouvert par le débogage quand l'appareil l'expose, sinon la voie automatique est indisponible.
- Révoquer une autorisation accordée par SygixOs, désactiver le service d'accessibilité ou retirer SygixOs de la politique du constructeur lors du rétablissement : hors P7 (l'utilisateur peut révoquer l'accessibilité et l'affichage superposé dans Android).
- Exécuter des commandes arbitraires, proposer un terminal, désactiver d'autres applications système (publicités, services Google) : sans phase prévue.
- Réappliquer automatiquement la configuration quand le système ou une mise à jour la défait : hors P7 ; l'état est relu et affiché, et la configuration peut être relancée.
- Modifier la politique d'auto-démarrage du constructeur au-delà de l'inscription de SygixOs dans ses deux listes blanches : sans phase prévue.
- Garantir le comportement sur tous les appareils : P7 décrit le comportement sur les appareils vérifiés et affiche « Indisponible » ailleurs.
- Recherche : P8. BetaSeries : P9.

## Questions ouvertes

Aucune. Les textes définitifs sont ceux de la maquette « 8. Launcher principal » (écrans 8.1 à 8.17), vérifiés le 2026-10-12. Les décisions du propriétaire du 2026-10-12 sont dans `design.md`, section « Décisions du propriétaire ».
