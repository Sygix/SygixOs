# Proposal

## Why

SygixOs se déclare déjà comme écran d'accueil Android (filtre `CATEGORY_HOME`), mais ne propose aucun parcours pour le devenir : le README renvoie à une procédure ADB manuelle qui désactive des applications système. Certains constructeurs détournent la touche Home vers leur propre launcher ; sur la TV de référence, le gestionnaire d'auto-démarrage du constructeur ignore aussi les diffusions vers les récepteurs statiques (suivi post-release, sujet 9). P5 fait de SygixOs un vrai launcher : rôle HOME demandé par l'utilisateur, démarrage à l'allumage et retour Home par le service d'accessibilité quand le constructeur détourne la touche, sans promettre un comportement identique sur tous les appareils.

## What Changes

- Une présentation initiale ignorable s'affiche au premier lancement (panneau « Faire de SygixOs l'écran d'accueil », maquettes Penpot 8.1 à 8.4) et propose, chacun après une action explicite de l'utilisateur : l'attribution du rôle HOME (« Remplacer le launcher »), le choix de l'option de démarrage à l'allumage (« Démarrer à l'allumage ») et l'activation du service d'accessibilité (« Retour à l'accueil », dans les réglages système Android).
- La catégorie « Écran d'accueil » des réglages, créée par `up-next`, reçoit, sous un intertitre « Launcher système », les trois mêmes lignes (maquettes 8.5 et 8.6) : « Remplacer le launcher » (état et demande du rôle HOME), « Démarrer à l'allumage » et « Retour à l'accueil » (service d'accessibilité).
- Démarrage à l'allumage : à la réception de la fin du démarrage du système, SygixOs demande son ouverture si l'option est activée ; si cette ouverture n'a pas été observée, les réglages l'indiquent au lancement suivant.
- Service d'accessibilité livré : quand le constructeur détourne la touche Home et que l'utilisateur a activé le service, SygixOs revient au premier plan.
- Touche Home pressée alors que SygixOs est déjà au premier plan : réglages et surcouches fermés, retour en haut de l'accueil, focus sur le héro.
- `self-update` : le parcours de mise à jour ne demande toujours jamais le rôle HOME ; seule une action explicite de l'utilisateur, dans la présentation initiale ou les réglages, peut le demander.
- Vérifications sur la TV de référence à la réception de l'APK RC, après implémentation : rôle HOME, réception de la fin du démarrage et ouverture qui suit, signal Home transmis au service d'accessibilité, relance après une mise à jour intégrée quand il détient le rôle HOME.
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

## Décisions du propriétaire (2026-10-10, maquettes)

Maquettes Penpot validées le 2026-10-10 (fichier « SygixOs Maquette », page « TV », écrans 8.1 à 8.6 ; détail dans `design.md`, « Maquettes » et décisions 19 à 25). Elles ferment la question ouverte 5 :

19. Présentation initiale : panneau de verre centré sur l'accueil (voile, mascotte fixe, titre « Faire de SygixOs l'écran d'accueil », description, trois lignes de réglage, aide « Retour : fermer » et bouton « Continuer »).
20. Trois lignes, dans cet ordre et avec les mêmes libellés dans la présentation et les réglages : « Remplacer le launcher », « Démarrer à l'allumage » (switch, désactivé par défaut), « Retour à l'accueil » (service d'accessibilité).
21. États « Actif » (pastille verte), « En attente… » (pastille bleue), « Inactif » et « Indisponible » (sans pastille, titre atténué, raison à la place du détail).
22. Navigation de la présentation : focus initial sur « Remplacer le launcher », haut/bas sur les trois lignes puis « Continuer » sans boucle, gauche/droite sans effet ; « Continuer », Retour et Home ferment définitivement ; au retour d'un écran système, focus sur la ligne qui l'a ouvert.
23. Réglages « Écran d'accueil » : après les deux réglages Up Next (`up-next`), un intertitre « Launcher système » non focusable puis les trois lignes ; focus initial inchangé (« Afficher Up Next »), haut/bas sans boucle, intertitre sauté.
24. Démarrage automatique non observé : seul le détail de « Démarrer à l'allumage » change (« SygixOs ne s'est pas ouvert automatiquement à ce démarrage »), le switch reste activé.
25. testTags fixés dans le delta `ui-testing`.
26. Confirmations du même jour, après relecture de la spec mise à jour : pastille « En attente… » en `SygixColors.Badge` (aucune nouvelle couleur) ; mascotte de la présentation = pose de repos fixe (première image de l'animation), 64 dp ; ligne « Indisponible » focalisable, OK sans effet, raison lisible au focus, dans la présentation et dans les réglages ; bouton « Continuer » de hauteur `Dimens.MenuActionHeight` (38 dp), sans jeton propre ; détail « non observé » sans autre indicateur, dans la couleur du détail normal.

## Décisions du propriétaire (2026-10-10, réception RC)

- Implémenter avant les essais TV, qui restent non vérifiés jusqu'à la réception RC par le propriétaire.
- Rôle HOME déjà détenu : aucune action, afficher « Actif ».
- Proposer aussi la permission « Afficher par-dessus d'autres applis » pour le démarrage hors exemption, via les réglages système après consentement explicite et explication ; refus et indisponibilité non bloquants, sans garantie constructeur. Placement : décision 32 ci-dessous.
- Livrer le service d'accessibilité même sans signal exploitable sur la TV de référence, sans action dans ce cas ; première implémentation par filtrage de la touche Home, sans lecture de contenu des fenêtres.
- Si la relance après mise à jour reste bloquée avec le rôle HOME, conserver le sujet 9 ouvert sans nouveau mécanisme P5.

## Décisions du propriétaire (2026-10-10, finalisation)

32. Permission d'affichage superposé : quand l'utilisateur fait passer « Démarrer à l'allumage » de désactivé à activé et que la permission manque, une confirmation explicative s'affiche aussitôt (« Ouvrir les réglages », « Pas maintenant ») ; trois lignes seulement ; l'option reste activée en cas de refus ; l'écran système est un choix délibéré de l'utilisateur, sans autorisation automatique ni garantie ; nouvelle proposition à la prochaine activation ; styles existants, sans maquette dédiée, rendu vérifié par capture lors de la validation 4.2. 33. Écran système de la permission d'affichage superposé absent ou impossible à ouvrir : « Démarrer à l'allumage » devient « Indisponible » avec sa raison et l'option ne s'active pas (aucune confirmation, activation refusée). Un refus de l'utilisateur sur un écran existant garde la décision 32 (option conservée, « non observé » si besoin, nouvelle proposition à la prochaine activation).

## Décisions du propriétaire (2026-10-10, relecture)

34. Raison affichée par « Démarrer à l'allumage » « Indisponible » (décision 33) : « Cette TV ne permet pas d'autoriser l'ouverture au démarrage ».
35. Un appui long sur Home garde son comportement système (tableau de bord Google TV) : le service d'accessibilité n'agit que sur un appui court ; un appui long (événements répétés, ou touche tenue au-delà du délai d'appui long) n'est pas consommé et reste au système.
36. Le démarrage à l'allumage reste limité au démarrage à froid dans P5. La tâche 1.1 teste séparément la sortie de veille (veille puis rallumage) et le démarrage à froid, et consigne les deux résultats ; si la sortie de veille n'ouvre pas SygixOs, ce cas va à un change ultérieur, sans nouveau mécanisme dans P5.

Les autres confirmations du jour (rôle déjà détenu, service livré sans signal, relance en échec gardée au backlog, ordre des lignes dans « Écran d'accueil ») étaient déjà écrites (décisions 28, 30, 31 et 23 de `design.md`).

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : rôle HOME, présentation initiale, démarrage à l'allumage, retour Home par le service d'accessibilité, touche Home avec SygixOs au premier plan (ADDED).
- `settings` : contrôles P5 dans la catégorie « Écran d'accueil », intertitre « Launcher système », rendu des états (ADDED).
- `ui-testing` : couverture des parcours P5 et sélecteurs du launcher système (ADDED).
- `self-update` : « Redémarrage après la mise à jour » (MODIFIED) ; la mise à jour ne demande jamais le rôle HOME, P5 seulement après une action explicite de l'utilisateur.

### Dépendances

- `up-next` (change ouvert, P6) : crée la catégorie « Écran d'accueil » (`SettingsCategory.HOME_SCREEN`) et ses réglages Up Next ; à archiver avant ce change.
- `jellyfin-tvprovider-only` : indépendant, aucun ordre relatif.

## Impact

Manifeste (récepteur de fin de démarrage, service d'accessibilité et sa configuration), `MainActivity` (`onNewIntent`), accès au rôle HOME derrière une interface, persistance DataStore (présentation initiale vue, option de démarrage, ouverture au démarrage observée), catégorie « Écran d'accueil » des réglages, chaînes françaises, tests JUnit et Compose, README. Aucune dépendance réseau ni serveur.

## Non-goals

- Désactiver automatiquement le launcher Google TV, SetupWraith ou toute autre application système, notamment par ADB : hors périmètre, sans phase prévue.
- Garantir le retour Home ou le démarrage automatique sur tous les appareils : P5 décrit le comportement sur les appareils qui le permettent et l'indique quand il n'a pas eu lieu.
- Ouverture de SygixOs à la sortie de veille : P5 ne couvre que le démarrage à froid (décision 36) ; un change ultérieur traitera la sortie de veille si la tâche 1.1 montre qu'elle n'ouvre pas SygixOs.
- Nouveau composant de design system ou nouveau style de ligne : la présentation initiale et les contrôles P5 réutilisent les lignes de réglages, la pilule de focus, le switch Apple et le verre existants (`design.md`, D9 et D10).

## Questions ouvertes

Liste unique dans `design.md`, section « Questions ouvertes », reprise telle quelle dans la description de la PR.
