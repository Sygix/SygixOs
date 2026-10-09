# Proposal

## Why

SygixOs se déclare déjà comme écran d'accueil Android (filtre `CATEGORY_HOME`), mais ne propose aucun parcours pour le devenir : le README renvoie à une procédure ADB manuelle qui désactive des applications système. Certains constructeurs détournent la touche Home vers leur propre launcher ; sur la TV de référence, le gestionnaire d'auto-démarrage du constructeur ignore aussi les diffusions vers les récepteurs statiques (suivi post-release, sujet 9). P5 fait de SygixOs un vrai launcher : rôle HOME demandé par l'utilisateur, démarrage à l'allumage et retour Home par le service d'accessibilité quand le constructeur détourne la touche, sans promettre un comportement identique sur tous les appareils.

## What Changes

- Une présentation initiale ignorable s'affiche au premier lancement et propose, chacun après une action explicite de l'utilisateur : l'attribution du rôle HOME, le choix de l'option de démarrage à l'allumage et l'activation du service d'accessibilité (dans les réglages système Android).
- La catégorie « Écran d'accueil » des réglages, créée par `up-next`, reçoit les contrôles P5 : « Remplacer le launcher » (état et demande du rôle HOME), l'option de démarrage à l'allumage et le contrôle du service d'accessibilité.
- Démarrage à l'allumage : à la réception de la fin du démarrage du système, SygixOs demande son ouverture si l'option est activée ; si cette ouverture n'a pas été observée, les réglages l'indiquent au lancement suivant.
- Service d'accessibilité livré : quand le constructeur détourne la touche Home et que l'utilisateur a activé le service, SygixOs revient au premier plan.
- Touche Home pressée alors que SygixOs est déjà au premier plan : réglages et surcouches fermés, retour en haut de l'accueil, focus sur le héro.
- `self-update` : le parcours de mise à jour ne demande toujours jamais le rôle HOME ; seule une action explicite de l'utilisateur, dans la présentation initiale ou les réglages, peut le demander.
- Vérifications sur la TV de référence avant le code qui en dépend : rôle HOME, réception de la fin du démarrage et ouverture qui suit, signal Home transmis au service d'accessibilité, relance après une mise à jour intégrée quand SygixOs détient le rôle HOME.
- Aucune désactivation automatique (ADB ou autre) du launcher Google TV ni d'aucune application système.

## Décisions du propriétaire (2026-10-10)

1. Le contrôle « Remplacer le launcher » porte l'état du rôle HOME et sa demande ; le démarrage à l'allumage et l'accessibilité sont des options distinctes.
2. L'état initial de l'option de démarrage à l'allumage est demandé dans la présentation initiale ; l'option n'est jamais activée sans ce choix.
3. Parcours : une présentation initiale ignorable au premier lancement, puis les réglages. La présentation propose le rôle HOME, le choix du démarrage à l'allumage et l'accessibilité.
4. Le service d'accessibilité (retour Home quand le constructeur détourne la touche) est livré et proposé par défaut dans la présentation initiale, qui renvoie vers les réglages d'accessibilité du système (SygixOs ne peut jamais l'activer lui-même). S'il est refusé, il n'est proposé de nouveau que depuis les réglages de SygixOs, sans relance automatique. Le risque de conformité et de distribution reste suivi dans les risques.
5. `self-update` : le parcours de mise à jour ne demande jamais le rôle HOME ; P5 ne le demande qu'après une action explicite de l'utilisateur (présentation initiale ou réglages). Le README est mis à jour en conséquence (phrase « never asks » et procédure ADB manuelle).
6. Suivi post-release, sujet 9 : vérifier sur l'appareil si SygixOs est relancé après sa propre mise à jour quand il détient le rôle HOME, et consigner le résultat dans `design.md`.
7. Démarrage à l'allumage : la première tâche vérifie sur la TV la réception de la fin du démarrage et l'ouverture de l'activité qui suit. Au lancement suivant, si l'option était activée et que l'ouverture au démarrage n'a pas été observée, les réglages indiquent que le démarrage automatique n'a pas eu lieu.
8. Les réglages P5 vont dans la catégorie « Écran d'accueil » créée par `up-next` (`SettingsCategory.HOME_SCREEN`) : ni nouvelle catégorie, ni « Apps sources ».
9. Touche Home avec SygixOs déjà au premier plan : fermer réglages et surcouches, remonter en haut de l'accueil et donner le focus au héro.
10. Ordre d'archivage : `up-next` avant `p5-real-launcher`, parce que P5 ajoute ses réglages à la catégorie « Écran d'accueil » introduite par `up-next`.
11. Maquettes : v1 et v2 sont remplacées. De nouvelles maquettes (présentation initiale et réglages P5 dans « Écran d'accueil ») seront faites dans Penpot et fournies aux agents d'implémentation ; les exigences visuelles restent en attente de ces maquettes.

Décisions complémentaires du même jour, après la relecture de la PR :

12. Rôle HOME indisponible, ou dialogue de demande impossible à ouvrir : le contrôle ouvre l'écran système des applications par défaut ou de l'écran d'accueil quand l'appareil en a un ; sinon il affiche l'état « indisponible ».
13. Une installation existante qui passe à P5 voit la présentation initiale une fois.
14. La touche Home pendant la présentation initiale la ferme définitivement.
15. La procédure ADB du README est conservée, reformulée comme alternative manuelle.
16. Contrôle d'accessibilité avec le service déjà activé : il affiche « actif » et OK ouvre les réglages d'accessibilité du système.
17. La présentation initiale ne s'affiche jamais par-dessus un dialogue système : la demande de permission « Programmes TV » (`READ_TV_LISTINGS`) passe d'abord, puis la présentation.
18. La détection du démarrage automatique par numéro de démarrage et l'état « action système en attente » restent tels qu'écrits.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : rôle HOME, présentation initiale, démarrage à l'allumage, retour Home par le service d'accessibilité, touche Home avec SygixOs au premier plan (ADDED).
- `settings` : contrôles P5 dans la catégorie « Écran d'accueil » (ADDED).
- `ui-testing` : couverture des parcours P5 (ADDED).
- `self-update` : « Redémarrage après la mise à jour » (MODIFIED) ; la mise à jour ne demande jamais le rôle HOME, P5 seulement après une action explicite de l'utilisateur.

### Dépendances

- `up-next` (change ouvert, P6) : crée la catégorie « Écran d'accueil » (`SettingsCategory.HOME_SCREEN`) et ses réglages Up Next ; à archiver avant ce change.
- `jellyfin-tvprovider-only` : indépendant, aucun ordre relatif.

## Impact

Manifeste (récepteur de fin de démarrage, service d'accessibilité et sa configuration), `MainActivity` (`onNewIntent`), accès au rôle HOME derrière une interface, persistance DataStore (présentation initiale vue, option de démarrage, ouverture au démarrage observée), catégorie « Écran d'accueil » des réglages, chaînes françaises, tests JUnit et Compose, README. Aucune dépendance réseau ni serveur.

## Non-goals

- Désactiver automatiquement le launcher Google TV, SetupWraith ou toute autre application système, notamment par ADB : hors périmètre, sans phase prévue.
- Garantir le retour Home ou le démarrage automatique sur tous les appareils : P5 décrit le comportement sur les appareils qui le permettent et l'indique quand il n'a pas eu lieu.
- Rendu visuel, libellés définitifs et navigation D-pad détaillée de la présentation initiale et des contrôles P5 : en attente des maquettes Penpot (voir `design.md`, « Maquettes »).

## Questions ouvertes

Liste unique dans `design.md`, section « Questions ouvertes », reprise telle quelle dans la description de la PR.
