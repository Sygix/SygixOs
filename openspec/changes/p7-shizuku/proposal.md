# Proposal

## Why

P5 fait de SygixOs un launcher candidat et lui fait demander le rôle HOME, mais ce rôle peut ne pas suffire sur Google TV : sur l'émulateur Android TV (Android 14) utilisé pour valider P5, le rôle attribué à SygixOs ne redirige pas la touche Home, le launcher fourni avec l'image garde la priorité et le système le rouvre juste après l'acceptation du rôle ; seule sa désactivation a fait de SygixOs l'écran d'accueil effectif. La TV de référence reste à vérifier (tâches 1.x). Cette désactivation n'est aujourd'hui possible que par la procédure ADB manuelle du README. [Shizuku](https://shizuku.rikka.app/) permet à une application d'utiliser les droits du shell ADB (ou de root avec Sui) une fois démarré par l'utilisateur : P7 l'utilise pour proposer cette procédure depuis SygixOs, de façon réversible, après une action explicite de l'utilisateur.

Terme : l'**accueil d'origine** désigne l'écran d'accueil fourni avec la TV (launcher Google TV ou Android TV) et le composant système qui le réactive.

## What Changes

- SygixOs détecte Shizuku (et Sui sur un appareil rooté) et en expose l'état : en cours de lecture, absent, version non prise en charge, arrêté, autorisation non accordée, autorisation refusée définitivement, prêt.
- Le mode avancé est présenté dans l'onboarding et les réglages : Shizuku détecté, il expose les actions avancées ; absent, une quatrième ligne explique son installation et les fonctions accessibles, avec un panneau d'aide navigable au D-pad.
- Le mode avancé permet, après une confirmation propre à chaque action, de désactiver/rétablir l'accueil d'origine, d'accorder à SygixOs la permission d'affichage superposé et d'activer son service d'accessibilité. Il n'effectue aucune révocation.
- L'action de rétablissement du mode avancé remet les composants de l'accueil d'origine dans leur état par défaut, y compris après une désactivation partielle ou faite par ADB, avec Shizuku prêt et après confirmation ; le rôle HOME n'est pas modifié.
- L'état de l'accueil d'origine (actif, partiellement désactivé, désactivé, indisponible) est relu auprès du système à chaque retour au premier plan, sans Shizuku : il reste exact après un redémarrage, quand Shizuku est arrêté ou si le système réactive l'accueil d'origine.
- Garde-fous : rien n'est désactivé avant la fin de la configuration initiale de l'appareil ni si SygixOs n'est pas un écran d'accueil activé ; une désactivation partielle, ou qui ne fait pas de SygixOs l'écran d'accueil effectif, est annulée ; opérations bornées dans le temps ; aucune commande arbitraire ; aucun processus privilégié qui survit à l'opération ou à SygixOs.
- Démarrage automatique du constructeur : sur les TV dont le gestionnaire d'auto-démarrage du constructeur bloque les applications (TCL), SygixOs se fait accorder par Shizuku, après une action explicite, l'autorisation `AUTO_START` du constructeur (opération d'application `appops`, accordable seulement par le shell). Sans elle, après un redémarrage complet, la diffusion de fin de démarrage n'arrive pas à SygixOs et le système ne relie pas son service d'accessibilité : ni l'ouverture à l'allumage ni la touche Home ne fonctionnent jusqu'à une réactivation manuelle (constat de v0.1.0-rc.2, D10).
- L'accueil d'origine est constitué d'un ensemble fermé de composants, arrêté après les vérifications sur la TV et affiché dans la confirmation. Les composants HOME qui ne font pas partie de cet ensemble ne sont jamais modifiés.
- Le mode avancé prolonge l'onboarding et la navigation des réglages P5 ; la présentation visuelle reste à valider par maquette après cette mise à jour de la spec.
- Vérifications sur la TV de référence avant implémentation : Shizuku et gestionnaires compatibles, composants de l'accueil d'origine, effets de leur désactivation (touche Home, démarrage, sortie de veille, boutons de la télécommande, rôle HOME, relance après mise à jour, sujet 9 du suivi), permissions de superposition et d'accessibilité, `AUTO_START` et rétablissement.
- Feuille de route : Shizuku devient P7 ; la recherche passe en P8 et BetaSeries en P9 (`AGENTS.md`, README, contexte `openspec/config.yaml` et références des changes actifs ; les archives et l'historique du suivi gardent leur numérotation).

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : état de Shizuku, accueil d'origine, garde-fous et opérations privilégiées limitées (ADDED) ; présentation initiale et permissions d'affichage superposé et d'accessibilité de P5 (MODIFIED).
- `settings` : mode avancé, aide Shizuku/Shevery, confirmations et réglages avancés (ADDED) ; « Réglages du launcher système » (MODIFIED : mode avancé conditionnel).
- `ui-testing` : couverture Shizuku et sélecteurs du mode avancé (ADDED) ; « Sélecteurs du launcher système » (MODIFIED : éléments conditionnels de l'onboarding et des réglages).
- `up-next` : MODIFIED « Modèle canonique » et « Dédoublonnage en niveaux » pour déplacer l'enrichissement BetaSeries de P8 à P9.
- `up-next` : MODIFIED « Modèle canonique » et « Dédoublonnage en niveaux » ; l'enrichissement BetaSeries devient P9 après insertion de Shizuku en P7 et Recherche en P8.

### Dépendances

- `p5-real-launcher` (archivé sur `main`) : fournit les exigences P5 de parcours guidé, de permission et de réglages que P7 modifie.
- `up-next` et `jellyfin-tvprovider-only` (archivés sur `main`) : fournissent la catégorie « Écran d'accueil ».

## Impact

Nouvelle dépendance : Shizuku API (`dev.rikka.shizuku:api` et `dev.rikka.shizuku:provider`, licence MIT, compatible AGPL-3.0), utilisée avec les gestionnaires compatibles Shizuku tels que Shizuku, Sui et Shevery. Shevery est recommandé dans l'aide et le README ; sa compatibilité effective et son démarrage sur la TV sont vérifiés en tâche 1.1. Manifeste (fournisseur `ShizukuProvider`, déclarations de visibilité des composants et gestionnaires), service privilégié à opérations fixes, contrôleur et états dans le domaine, mode avancé, confirmations, chaînes françaises, tests JUnit et Compose, README (procédure, risques, rétablissement, procédure ADB conservée). Aucune dépendance réseau ni serveur. Documents de feuille de route renumérotés.

## Non-goals

- Modifier le rôle HOME ou révoquer une permission système : hors P7. Les permissions d'affichage superposé et d'accessibilité sont accordées au seul paquet/service de SygixOs après action et confirmation distinctes ; `AUTO_START` l'est après une action distincte, sans confirmation additionnelle.
- Exécuter des commandes arbitraires, proposer un terminal ou d'autres opérations privilégiées : sans phase prévue.
- Désactiver d'autres applications système (publicités, services Google) : sans phase prévue.
- Réappliquer automatiquement la désactivation quand le système réactive l'accueil d'origine : hors P7 ; l'état est simplement relu et affiché.
- Garantir le comportement sur tous les appareils : P7 décrit le comportement sur les appareils vérifiés et affiche « Indisponible » ailleurs.
- Recherche : P8. BetaSeries : P9.

## Décisions du propriétaire

Les décisions des questions initialement ouvertes sont intégrées dans `design.md` : périmètre des opérations (accueil, affichage superposé, accessibilité et `AUTO_START`), ensemble fermé des composants, présentation du mode avancé dans l'onboarding et les réglages, panneau d'aide navigable, vérifications appareil avant implémentation, textes des confirmations, absence de réapplication automatique, rappel de débogage sans fil et compatibilité Shizuku API avec recommandation de Shevery. Les maquettes Penpot restent à réaliser après validation de cette spec.
