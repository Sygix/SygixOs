# Proposal

## Why

SygixOs se déclare déjà comme écran d'accueil Android (filtre `CATEGORY_HOME`), mais ne propose aucun parcours pour le devenir : le README renvoie à une procédure ADB manuelle qui désactive des applications système. Certains constructeurs détournent la touche Home vers leur propre launcher ; sur la TV de référence, le gestionnaire d'auto-démarrage du constructeur ignore aussi les diffusions vers les récepteurs statiques (suivi post-release, sujet 9). P5 fait de SygixOs un vrai launcher : rôle HOME demandé par l'utilisateur, démarrage à l'allumage et retour Home par le service d'accessibilité quand le constructeur détourne la touche, sans promettre un comportement identique sur tous les appareils.

## What Changes

- Un parcours guidé ignorable, « Faire de SygixOs l'écran d'accueil » (maquettes Penpot 8.1 à 8.7, décisions 43 et 44), s'affiche au premier lancement et depuis les réglages ; chaque étape attend une action explicite de l'utilisateur : 1 « Revenir à SygixOs avec la touche Home » (service d'accessibilité, réglages système Android), 2 « Ouvrir SygixOs à l'allumage » (switch, avec la sous-étape 2b « Autoriser l'ouverture au démarrage » quand la permission d'affichage superposé manque), 3 « Définir SygixOs par défaut » (rôle HOME, facultatif).
- La catégorie « Écran d'accueil » des réglages, créée par `up-next`, reçoit sous un intertitre « Launcher système » une seule ligne, « SygixOs comme écran d'accueil », qui résume l'état du parcours et l'ouvre (maquette 8.5, décision 45).
- Démarrage à l'allumage : à la fin du démarrage du système, ou à la connexion du service d'accessibilité peu après un démarrage à froid, SygixOs demande une fois son ouverture si l'option est activée ; si cette ouverture n'a pas été observée, le parcours l'indique au lancement suivant.
- Service d'accessibilité livré : quand l'utilisateur l'a activé, il consomme la touche Home et ouvre SygixOs aussitôt, sans laisser l'accueil du constructeur s'afficher (décision 37) ; si un gestionnaire d'auto-démarrage du constructeur l'empêche de démarrer après un redémarrage, SygixOs tente de le relier à son lancement et affiche « Bloqué » sinon (décisions 39 à 42).
- Touche Home pressée alors que SygixOs est déjà au premier plan : réglages et surcouches fermés, retour en haut de l'accueil, focus sur le héro.
- `self-update` : le parcours de mise à jour ne demande toujours jamais le rôle HOME ; seule une action explicite de l'utilisateur, sur l'étape 3 du parcours guidé, peut le demander.
- Vérifications sur la TV de référence à la réception de l'APK RC, après implémentation : rôle HOME, réception de la fin du démarrage et ouverture qui suit, signal Home transmis au service d'accessibilité, relance après une mise à jour intégrée quand il détient le rôle HOME.
- Aucune désactivation automatique (ADB ou autre) du launcher Google TV ni d'aucune application système.

## Décisions du propriétaire (2026-10-10)

1. Le contrôle « Remplacer le launcher » porte l'état du rôle HOME et sa demande ; le démarrage à l'allumage et l'accessibilité sont des options distinctes.
2. L'état initial de l'option de démarrage à l'allumage est demandé dans la présentation initiale ; l'option n'est jamais activée sans ce choix. *(2026-10-11 : précisée par la décision 43 : le choix se fait à l'étape 2 du parcours guidé.)*
3. Parcours : une présentation initiale ignorable au premier lancement, puis les réglages. La présentation propose le rôle HOME, le choix du démarrage à l'allumage et l'accessibilité. *(2026-10-11 : précisée par la décision 43 : le parcours guidé remplace la présentation initiale.)*
4. Le service d'accessibilité (retour Home quand le constructeur détourne la touche) est livré et proposé par défaut dans la présentation initiale, qui renvoie vers les réglages d'accessibilité du système (SygixOs ne peut jamais l'activer lui-même). S'il est refusé, il n'est proposé de nouveau que depuis les réglages de SygixOs, sans relance automatique. Le risque de conformité et de distribution reste suivi dans les risques. *(2026-10-11 : précisée par la décision 43 : étape 1 du parcours guidé.)*
5. `self-update` : le parcours de mise à jour ne demande jamais le rôle HOME ; P5 ne le demande qu'après une action explicite de l'utilisateur (présentation initiale ou réglages). Le README est mis à jour en conséquence (phrase « never asks » et procédure ADB manuelle).
6. Suivi post-release, sujet 9 : vérifier sur l'appareil si SygixOs est relancé après sa propre mise à jour quand il détient le rôle HOME, et consigner le résultat dans `design.md`.
7. Démarrage à l'allumage : la première tâche vérifie sur la TV la réception de la fin du démarrage et l'ouverture de l'activité qui suit. Au lancement suivant, si l'option était activée et que l'ouverture au démarrage n'a pas été observée, les réglages indiquent que le démarrage automatique n'a pas eu lieu.
8. Les réglages P5 vont dans la catégorie « Écran d'accueil » créée par `up-next` (`SettingsCategory.HOME_SCREEN`) : ni nouvelle catégorie, ni « Apps sources ».
9. Touche Home avec SygixOs déjà au premier plan : fermer réglages et surcouches, remonter en haut de l'accueil et donner le focus au héro.
10. Ordre d'archivage : `up-next` avant `p5-real-launcher`, parce que P5 ajoute ses réglages à la catégorie « Écran d'accueil » introduite par `up-next`.
11. Maquettes : v1 et v2 sont remplacées. De nouvelles maquettes (présentation initiale et réglages P5 dans « Écran d'accueil ») seront faites dans Penpot et fournies aux agents d'implémentation ; les exigences visuelles restent en attente de ces maquettes.

Décisions complémentaires du même jour, après la relecture de la PR :

12. Rôle HOME indisponible, ou dialogue de demande impossible à ouvrir : le contrôle ouvre l'écran système des applications par défaut ou de l'écran d'accueil quand l'appareil en a un ; sinon il affiche l'état « indisponible ». *(2026-10-11 : précisée par la décision 44 : l'étape 3 est masquée au lieu d'être « Indisponible ».)*
13. Une installation existante qui passe à P5 voit la présentation initiale une fois. *(2026-10-11 : s'applique au parcours guidé (décision 43).)*
14. La touche Home pendant la présentation initiale la ferme définitivement. *(2026-10-11 : s'applique au parcours guidé (décision 43).)*
15. La procédure ADB du README est conservée, reformulée comme alternative manuelle.
16. Contrôle d'accessibilité avec le service déjà activé : il affiche « actif » et OK ouvre les réglages d'accessibilité du système.
17. La présentation initiale ne s'affiche jamais par-dessus un dialogue système : la demande de permission « Programmes TV » (`READ_TV_LISTINGS`) passe d'abord, puis la présentation. *(2026-10-11 : s'applique au parcours guidé (décision 43).)*
18. La détection du démarrage automatique par numéro de démarrage et l'état « action système en attente » restent tels qu'écrits.

## Décisions du propriétaire (2026-10-10, maquettes)

Maquettes Penpot validées le 2026-10-10 (fichier « SygixOs Maquette », page « TV », écrans 8.1 à 8.6 ; détail dans `design.md`, « Maquettes » et décisions 19 à 25). Elles ferment la question ouverte 5 :

19. Présentation initiale : panneau de verre centré sur l'accueil (voile, mascotte fixe, titre « Faire de SygixOs l'écran d'accueil », description, trois lignes de réglage, aide « Retour : fermer » et bouton « Continuer »). *(2026-10-11 : remplacée par la décision 43.)*
20. Trois lignes, dans cet ordre et avec les mêmes libellés dans la présentation et les réglages : « Remplacer le launcher », « Démarrer à l'allumage » (switch, désactivé par défaut), « Retour à l'accueil » (service d'accessibilité). *(2026-10-11 : remplacée par les décisions 43 et 45.)*
21. États « Actif » (pastille verte), « En attente… » (pastille bleue), « Inactif » et « Indisponible » (sans pastille, titre atténué, raison à la place du détail). *(2026-10-11 : remplacée en partie par la décision 44 (libellés d'état des étapes), pastilles conservées.)*
22. Navigation de la présentation : focus initial sur « Remplacer le launcher », haut/bas sur les trois lignes puis « Continuer » sans boucle, gauche/droite sans effet ; « Continuer », Retour et Home ferment définitivement ; au retour d'un écran système, focus sur la ligne qui l'a ouvert. *(2026-10-11 : remplacée par la décision 43.)*
23. Réglages « Écran d'accueil » : après les deux réglages Up Next (`up-next`), un intertitre « Launcher système » non focusable puis les trois lignes ; focus initial inchangé (« Afficher À suivre »), haut/bas sans boucle, intertitre sauté. *(2026-10-11 : remplacée par la décision 45.)*
24. Démarrage automatique non observé : seul le détail de « Démarrer à l'allumage » change (« SygixOs ne s'est pas ouvert automatiquement à ce démarrage »), le switch reste activé. *(2026-10-11 : conservée, appliquée au détail de l'étape 2 (décision 43).)*
25. testTags fixés dans le delta `ui-testing`. *(2026-10-11 : testTags revus par la décision 43 (delta `ui-testing`).)*
26. Confirmations du même jour, après relecture de la spec mise à jour : pastille « En attente… » en `SygixColors.Badge` (aucune nouvelle couleur) ; mascotte de la présentation = pose de repos fixe (première image de l'animation), 64 dp ; ligne « Indisponible » focalisable, OK sans effet, raison lisible au focus, dans la présentation et dans les réglages ; bouton « Continuer » de hauteur `Dimens.MenuActionHeight` (38 dp), sans jeton propre ; détail « non observé » sans autre indicateur, dans la couleur du détail normal. *(2026-10-11 : « Continuer » remplacé par « Terminé » (décision 43), le reste conservé.)*

## Décisions du propriétaire (2026-10-10, réception RC)

- Implémenter avant les essais TV, qui restent non vérifiés jusqu'à la réception RC par le propriétaire.
- Rôle HOME déjà détenu : aucune action, afficher « Actif ».
- Proposer aussi la permission « Afficher par-dessus d'autres applis » pour le démarrage hors exemption, via les réglages système après consentement explicite et explication ; refus et indisponibilité non bloquants, sans garantie constructeur. Placement : décision 32 ci-dessous.
- Livrer le service d'accessibilité même sans signal exploitable sur la TV de référence, sans action dans ce cas ; première implémentation par filtrage de la touche Home, sans lecture de contenu des fenêtres.
- Si la relance après mise à jour reste bloquée avec le rôle HOME, conserver le sujet 9 ouvert sans nouveau mécanisme P5.

## Décisions du propriétaire (2026-10-10, finalisation)

32. Permission d'affichage superposé : quand l'utilisateur fait passer « Démarrer à l'allumage » de désactivé à activé et que la permission manque, une confirmation explicative s'affiche aussitôt (« Ouvrir les réglages », « Pas maintenant ») ; trois lignes seulement ; l'option reste activée en cas de refus ; l'écran système est un choix délibéré de l'utilisateur, sans autorisation automatique ni garantie ; nouvelle proposition à la prochaine activation ; styles existants, sans maquette dédiée, rendu vérifié par capture lors de la validation 4.2. *(2026-10-11 : remplacée par la décision 43 : sous-étape 2b, sans confirmation.)* 33. Écran système de la permission d'affichage superposé absent ou impossible à ouvrir : « Démarrer à l'allumage » devient « Indisponible » avec sa raison et l'option ne s'active pas (aucune confirmation, activation refusée). Un refus de l'utilisateur sur un écran existant garde la décision 32 (option conservée, « non observé » si besoin, nouvelle proposition à la prochaine activation). *(2026-10-11 : conservée pour l'étape 2 du parcours guidé (décision 43).)*

## Décisions du propriétaire (2026-10-10, relecture)

34. Raison affichée par « Démarrer à l'allumage » « Indisponible » (décision 33) : « Cette TV ne permet pas d'autoriser l'ouverture au démarrage ».
35. Un appui long sur Home garde son comportement système (tableau de bord Google TV) : le service d'accessibilité n'agit que sur un appui court ; un appui long (événements répétés, ou touche tenue au-delà du délai d'appui long) n'est pas consommé et reste au système. *(2026-10-11 : remplacée par la décision 37.)*
36. Le démarrage à l'allumage reste limité au démarrage à froid dans P5. La tâche 1.1 teste séparément la sortie de veille (veille puis rallumage) et le démarrage à froid, et consigne les deux résultats ; si la sortie de veille n'ouvre pas SygixOs, ce cas va à un change ultérieur, sans nouveau mécanisme dans P5.

Les autres confirmations du jour (rôle déjà détenu, service livré sans signal, relance en échec gardée au backlog, ordre des lignes dans « Écran d'accueil ») étaient déjà écrites (décisions 28, 30, 31 et 23 de `design.md`).

## Décisions du propriétaire (2026-10-11, retours de v0.1.0-rc.1)

Après l'essai de v0.1.0-rc.1 sur la TV de référence (détail et justifications dans `design.md`, même section) ; les décisions remplacées ci-dessus sont marquées.

37. Touche Home consommée par le service (appui, répétitions, relâchement) tant que la redirection est active, SygixOs ouvert aussitôt au relâchement, sans délai ; appui long = appui court. Remplace la décision 35.
38. Seuil de 700 ms de D1 conservé (écran de demande du rôle refermé sans interface, étape 3) ; le délai d'appui long utilisé pour la touche Home disparaît.
39. Gestionnaires d'auto-démarrage du constructeur (par exemple TCL) : après un redémarrage complet, `BOOT_COMPLETED` et la liaison du service sont bloqués tant que le processus ne tourne pas ; autorisation impossible sans adb (menu du constructeur sur certains firmwares) ; octroi par le shell en P7 (Shizuku).
40. Réactivation du service au lancement de SygixOs par bascule d'un de ses composants, gardée seulement si la TV la valide.
41. Ouverture à l'allumage aussi par la connexion du service peu après un démarrage à froid ; une seule ouverture.
42. Étape 1 « Bloqué », expliquée, quand le service est activé mais ne tourne pas (maquette 8.7).
43. Parcours guidé « Faire de SygixOs l'écran d'accueil » à la place de la présentation initiale, des trois lignes des réglages et de la confirmation d'affichage superposé (sous-étape 2b en ligne).
44. Étapes affichées selon l'appareil et les options ; états « À faire », « En attente… », « Fait », « Bloqué », « Indisponible », « Facultatif » (étape 3) ; progression sur les étapes obligatoires.
45. Une seule ligne « SygixOs comme écran d'accueil » dans « Écran d'accueil », qui résume le parcours (« Terminé », « Partiel » ou « À faire » avec ce qui reste, « Touche Home bloquée par la TV ») et l'ouvre.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : rôle HOME, parcours guidé, démarrage à l'allumage (récepteur et service), retour Home par le service d'accessibilité (touche consommée, réactivation, état « Bloqué »), touche Home avec SygixOs au premier plan (ADDED).
- `settings` : ligne « SygixOs comme écran d'accueil » dans la catégorie « Écran d'accueil », sous l'intertitre « Launcher système », rendu des états (ADDED).
- `ui-testing` : couverture des parcours P5 et sélecteurs du launcher système (ADDED).
- `self-update` : « Redémarrage après la mise à jour » (MODIFIED) ; la mise à jour ne demande jamais le rôle HOME, P5 seulement après une action explicite de l'utilisateur.

### Dépendances

- `up-next` (change ouvert, P6) : crée la catégorie « Écran d'accueil » (`SettingsCategory.HOME_SCREEN`) et ses réglages Up Next ; à archiver avant ce change.
- `jellyfin-tvprovider-only` : indépendant, aucun ordre relatif.

## Impact

Manifeste (récepteur de fin de démarrage, service d'accessibilité et sa configuration, composant inerte de réactivation), `MainActivity` (`onNewIntent`), accès au rôle HOME derrière une interface, persistance DataStore (parcours guidé fermé, option de démarrage, ouverture au démarrage observée), catégorie « Écran d'accueil » des réglages, chaînes françaises, tests JUnit et Compose, README. Aucune dépendance réseau ni serveur.

## Non-goals

- Désactiver automatiquement le launcher Google TV, SetupWraith ou toute autre application système, notamment par ADB : hors périmètre, sans phase prévue.
- Garantir le retour Home ou le démarrage automatique sur tous les appareils : P5 décrit le comportement sur les appareils qui le permettent et l'indique quand il n'a pas eu lieu.
- Ouverture de SygixOs à la sortie de veille : P5 ne couvre que le démarrage à froid (décision 36) ; un change ultérieur traitera la sortie de veille si la tâche 1.1 montre qu'elle n'ouvre pas SygixOs.
- Obtenir l'autorisation de démarrage automatique d'un gestionnaire du constructeur : impossible sans adb pour une app ordinaire ; l'octroi par le shell relève du change P7 (Shizuku).
- Nouveau composant de design system ou nouveau style de ligne : le parcours guidé et la ligne P5 réutilisent les lignes de réglages, la pilule de focus, le switch Apple et le verre existants (`design.md`, D9 et D10).

## Questions ouvertes

Liste unique dans `design.md`, section « Questions ouvertes », reprise telle quelle dans la description de la PR.
