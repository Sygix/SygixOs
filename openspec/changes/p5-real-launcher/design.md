# Design

## Context

État de départ (`main`) :
- **Manifeste** : `MainActivity` (`singleTask`) porte un filtre `MAIN` + `LEANBACK_LAUNCHER` + `LAUNCHER` et un filtre `MAIN` + `HOME` + `DEFAULT` : SygixOs est déjà candidat au rôle d'écran d'accueil. Aucun récepteur de fin de démarrage, aucun service d'accessibilité.
- **`MainActivity`** ne redéfinit pas `onNewIntent` : une touche Home qui atteint SygixOs déjà au premier plan ne change rien à l'écran.
- **Réglages** : `SettingsCategory` (`ui/settings/SettingsScreen.kt`) compte aujourd'hui « Apps sources », « Applications cachées » et « À propos ». Le change ouvert `up-next` (P6) y ajoute la catégorie « Écran d'accueil » (`SettingsCategory.HOME_SCREEN`), dont le premier contrôle est « Afficher Up Next ».
- **`self-update`** : « Redémarrage après la mise à jour » interdit au launcher de demander le rôle HOME. Sur la TV de référence, le gestionnaire d'auto-démarrage du constructeur ignore les diffusions vers les récepteurs statiques (journal « Skipping delivery of static … for auto run ») : SygixOs n'est pas relancé après sa mise à jour (suivi post-release, sujet 9, reporté à P5 par le propriétaire). Le même gestionnaire peut bloquer la diffusion de fin de démarrage ; Android 14 restreint aussi l'ouverture d'une activité depuis l'arrière-plan.
- **README** : affirme que SygixOs ne demande jamais le rôle HOME et décrit une procédure ADB manuelle qui désactive le launcher Google TV, présentée comme « première étape de P5 ».

Références plateforme : rôle HOME par [`RoleManager`](https://developer.android.com/reference/android/app/role/RoleManager) (`isRoleAvailable`, `isRoleHeld`, `createRequestRoleIntent`) ; service d'accessibilité activé uniquement par l'utilisateur dans les réglages système ([`AccessibilityService`](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)) ; [restrictions d'ouverture d'activité depuis l'arrière-plan](https://developer.android.com/guide/components/activities/background-starts).

## Décisions du propriétaire (2026-10-10)

1. Le contrôle « Remplacer le launcher » porte l'état du rôle HOME et sa demande ; le démarrage à l'allumage et l'accessibilité sont des options distinctes.
2. L'état initial de l'option de démarrage à l'allumage est demandé dans la présentation initiale ; l'option n'est jamais activée sans ce choix.
3. Parcours : une présentation initiale ignorable au premier lancement, puis les réglages. La présentation propose le rôle HOME, le choix du démarrage à l'allumage et l'accessibilité.
4. Le service d'accessibilité (retour Home quand le constructeur détourne la touche) est livré et proposé par défaut dans la présentation initiale, qui renvoie vers les réglages d'accessibilité du système (SygixOs ne peut jamais l'activer lui-même). S'il est refusé, il n'est proposé de nouveau que depuis les réglages de SygixOs, sans relance automatique. Le risque de conformité et de distribution reste suivi dans les risques.
5. `self-update` : le parcours de mise à jour ne demande jamais le rôle HOME ; P5 ne le demande qu'après une action explicite de l'utilisateur (présentation initiale ou réglages). Le README est mis à jour en conséquence (phrase « never asks » et procédure ADB manuelle).
6. Suivi post-release, sujet 9 : vérifier sur l'appareil si SygixOs est relancé après sa propre mise à jour quand il détient le rôle HOME, et consigner le résultat dans ce document.
7. Démarrage à l'allumage : la première tâche vérifie sur la TV la réception de la fin du démarrage et l'ouverture de l'activité qui suit. Au lancement suivant, si l'option était activée et que l'ouverture au démarrage n'a pas été observée, les réglages indiquent que le démarrage automatique n'a pas eu lieu.
8. Les réglages P5 vont dans la catégorie « Écran d'accueil » créée par `up-next` (`SettingsCategory.HOME_SCREEN`) : ni nouvelle catégorie, ni « Apps sources ».
9. Touche Home avec SygixOs déjà au premier plan : fermer réglages et surcouches, remonter en haut de l'accueil et donner le focus au héro.
10. Ordre d'archivage : `up-next` avant `p5-real-launcher`, parce que P5 ajoute ses réglages à la catégorie « Écran d'accueil » introduite par `up-next`.
11. Maquettes : v1 et v2 sont remplacées. De nouvelles maquettes (présentation initiale et réglages P5 dans « Écran d'accueil ») seront faites dans Penpot et fournies aux agents d'implémentation ; les exigences visuelles restent en attente de ces maquettes.

## Goals / Non-Goals

**Goals:**
- Rôle HOME, démarrage à l'allumage et service d'accessibilité proposés dans la présentation initiale puis dans « Écran d'accueil », chacun après une action explicite de l'utilisateur.
- États toujours relus auprès du système au retour au premier plan ; aucune capacité annoncée comme accordée avant confirmation du système.
- Échecs non bloquants et visibles : rôle indisponible, service non activé, ouverture au démarrage non observée.
- Hypothèses plateforme vérifiées sur la TV de référence avant le code qui en dépend, résultats consignés ci-dessous.

**Non-Goals:**
- Désactivation automatique (ADB ou autre) de composants système.
- Garantie universelle du retour Home ou du démarrage automatique.
- Rendu visuel et navigation D-pad détaillée avant les maquettes Penpot.

## Decisions

### D1. Rôle HOME derrière une interface
Une interface `HomeRoleGateway` (`data/`) expose l'état du rôle (`isRoleAvailable` puis `isRoleHeld`) et l'intent de demande (`createRequestRoleIntent(ROLE_HOME)`), remplacée par un double dans les tests. Le résultat de l'activité de demande n'est jamais pris pour vérité : l'état est relu à chaque retour au premier plan (`ON_RESUME`). Une demande qui ne peut pas s'ouvrir (aucune activité, exception) donne l'état « indisponible ». Le parcours de mise à jour (`self-update`) n'a pas accès à cette interface.

### D2. États des contrôles système
Un même modèle d'état sert la présentation initiale et les réglages : actif, inactif, indisponible, action système en attente. L'état « action système en attente » commence quand SygixOs ouvre un écran ou un dialogue système pour un contrôle et se termine au retour au premier plan suivant, qui relit l'état réel ; il n'est jamais persisté (un processus recréé relit directement l'état). Il est exposé par le ViewModel des réglages et de l'accueil (StateFlow), aucune logique dans les composables.

### D3. Présentation initiale
Persistance d'un drapeau « présentation initiale fermée » dans `LauncherPrefs`, écrit à toute fermeture (Retour, action d'ignorer, touche Home, ou fin du parcours). Tant qu'il est absent, la présentation s'affiche une fois l'accueil interactif, après la fin de l'écran de démarrage et jamais par-dessus un dialogue système ; une installation existante qui passe à P5 la voit donc une fois. Si le processus est arrêté pendant qu'un écran système ouvert depuis la présentation est affiché, la présentation réapparaît au lancement suivant avec les états relus. Contenu : rôle HOME (état et demande, D1), choix du démarrage à l'allumage (D4), service d'accessibilité (état et ouverture des réglages d'accessibilité du système, D5).

### D4. Démarrage à l'allumage
- Option persistée dans `LauncherPrefs`, désactivée tant que l'utilisateur ne l'a pas choisie (présentation initiale ou réglages). À l'activation, SygixOs mémorise le numéro de démarrage courant (`Settings.Global.BOOT_COUNT`).
- Récepteur `BOOT_COMPLETED` déclaré (permission `RECEIVE_BOOT_COMPLETED`) : option désactivée, il ne fait rien ; activée, il demande l'ouverture de `MainActivity` avec un extra qui marque l'ouverture au démarrage. Mécanisme confirmé ou ajusté par la tâche 1.1 avant toute implémentation.
- `MainActivity` qui reçoit cet extra (création ou `onNewIntent`) mémorise « ouverture au démarrage observée » pour le numéro de démarrage courant.
- État affiché : « démarrage automatique non observé » quand l'option est activée, qu'elle l'était avant le démarrage courant et qu'aucune ouverture au démarrage n'a été observée pour ce démarrage. Couvre la diffusion ignorée par le constructeur comme l'ouverture refusée par Android. L'état est réactif : une ouverture observée plus tard dans le même démarrage l'efface ; désactiver l'option l'efface.

### D5. Service d'accessibilité
Service déclaré avec la permission `BIND_ACCESSIBILITY_SERVICE` et une description française qui en dit l'usage (retour à SygixOs quand la touche Home est détournée). Le signal utilisé (touche Home filtrée par le service, ou passage au premier plan du launcher du constructeur) est choisi d'après la tâche 1.3 ; le service ne lit que les événements nécessaires à ce signal et ne conserve rien. Sur ce signal, il ouvre `MainActivity` comme une touche Home (D6 s'applique si SygixOs était déjà au premier plan). L'état activé est lu par `AccessibilityManager` au retour au premier plan. Le contrôle ouvre `Settings.ACTION_ACCESSIBILITY_SETTINGS`, que le service soit actif ou non ; SygixOs ne l'active ni ne le désactive jamais lui-même.

### D6. Touche Home avec SygixOs au premier plan
`MainActivity.onNewIntent` transmet au ViewModel de l'accueil un événement « Home » quand l'intent porte `CATEGORY_HOME` et que l'activité était au premier plan à sa réception, ou quand l'intent vient du service (D5) et que SygixOs était au premier plan avant l'appui détourné. L'événement ferme la page de réglages et toute surcouche de l'accueil comme le ferait Retour (menu contextuel, mode « Déplacer » annulé, présentation initiale fermée), ramène la page en haut et donne le focus au héro. Hors premier plan (retour depuis une autre app), les règles de retour existantes s'appliquent sans changement. Pendant l'écran de démarrage, l'événement est sans effet.

### D7. Réglages dans « Écran d'accueil »
Les trois contrôles P5 sont des lignes de `SettingsCategory.HOME_SCREEN` ; « Afficher Up Next » reste le premier contrôle et le focus initial de la catégorie (`settings`). Aucune nouvelle entrée de `SettingsCategory`. Les libellés sont des ressources françaises ; leur texte définitif, leur ordre et leurs testTags suivent les maquettes Penpot.

### D8. `self-update`
Delta MODIFIED sur « Redémarrage après la mise à jour » : la mise à jour ne demande jamais le rôle HOME ; seule une action explicite de l'utilisateur dans la présentation initiale ou les réglages le peut. Le reste de l'exigence est inchangé ; le résultat de la tâche 1.4 y est reporté avant l'archivage (tâche 4.3).

## Vérifications sur l'appareil

Résultats à consigner ici par les tâches 1.x (structure et comportements seulement : versions d'API, résultats, extraits de journal sans donnée personnelle ; jamais de modèle exact, de numéro de série ni d'adresse).

| Vérification | Tâche | Résultat |
| --- | --- | --- |
| Fin du démarrage reçue par le récepteur statique, ouverture de l'activité acceptée (sans rôle HOME, avec rôle HOME, avec le service d'accessibilité activé) | 1.1 | à consigner |
| Rôle HOME : disponibilité, dialogue de demande, acceptation, refus, refus répété ; touche Home qui atteint `onNewIntent` quand SygixOs détient le rôle | 1.2 | à consigner |
| Signal transmis au service d'accessibilité quand la touche Home est pressée et que le constructeur affiche son launcher | 1.3 | à consigner |
| Relance de SygixOs après sa propre mise à jour intégrée quand il détient le rôle HOME (sujet 9) | 1.4 | à consigner |

## Risks / Trade-offs

- [Le service d'accessibilité est destiné à l'assistance ; son usage pour le retour Home peut être refusé par une politique de distribution] → Service jamais activé par SygixOs, description explicite de son usage, aucune donnée lue au-delà du signal Home ni conservée ; distribution actuelle par les releases GitHub. Une distribution sur un autre canal exigera une revue de conformité.
- [Rôle HOME, touche Home et démarrage varient selon le constructeur et la version] → Vérifications 1.x avant le code ; états « indisponible » et « démarrage automatique non observé » au lieu d'une promesse.
- [Android 14 peut refuser l'ouverture depuis le récepteur de fin de démarrage] → Tâche 1.1 identifie les cas acceptés ; l'échec est détecté et affiché (D4).
- [« Démarrage automatique non observé » peut s'afficher brièvement si SygixOs est ouvert avant la diffusion de fin de démarrage] → État réactif, effacé dès que l'ouverture est observée dans le même démarrage.
- [Une touche Home pendant un écran système ouvert depuis la présentation initiale ferme cette présentation] → Conforme aux décisions 3 et 9 ; les mêmes contrôles restent dans « Écran d'accueil ».

## Migration Plan

Aucune donnée existante migrée. Nouvelles clés DataStore (présentation initiale fermée, option de démarrage, numéros de démarrage) absentes = valeurs par défaut (présentation à afficher, option désactivée). SygixOs ne modifie jamais lui-même le rôle HOME ni les réglages d'accessibilité. Archivage après merge, validation sur la TV réelle et archivage de `up-next`.

## Ordre d'archivage

`up-next` puis `p5-real-launcher` : P5 ajoute ses contrôles à la catégorie « Écran d'accueil » que `up-next` introduit dans `settings`. Les deux changes touchent `launcher-shell` et `ui-testing` sans modifier les mêmes exigences (P5 n'y ajoute que des exigences). `jellyfin-tvprovider-only` est indépendant.

## Questions ouvertes

1. Rôle HOME indisponible, ou demande sans dialogue sur la TV (résultat de 1.2) : A) afficher seulement l'état « indisponible » — proposé, à confirmer ; B) ouvrir l'écran système de choix du launcher s'il existe.
2. « Remplacer le launcher » quand le rôle est déjà détenu : A) aucune action, l'état « actif » est affiché — proposé, à confirmer ; B) ouvrir l'écran système de choix du launcher pour le rendre.
3. Ouverture au démarrage refusée par Android hors exemption (résultat de 1.1) : A) s'en tenir aux cas déjà permis (rôle HOME, service d'accessibilité activé) et afficher l'échec — proposé, à confirmer ; B) demander en plus la permission « Afficher par-dessus d'autres applis ».
4. Aucun signal Home exploitable transmis au service d'accessibilité sur la TV de référence (résultat de 1.3) : A) suspendre l'implémentation du service et revenir vers Simon — proposé, à confirmer ; B) le livrer quand même pour d'autres appareils.
5. Sujet 9, si SygixOs n'est pas relancé après sa mise à jour même avec le rôle HOME (résultat de 1.4) : A) consigner le résultat et laisser le sujet 9 ouvert au backlog — proposé, à confirmer ; B) traiter la relance dans P5 (piste du backlog : statut de session transmis à une activité).
6. Moment de la présentation initiale par rapport à la demande de permission « Programmes TV » (`READ_TV_LISTINGS`) au premier lancement : A) après la réponse à cette demande — proposé, à confirmer ; B) avant.
7. Rendu visuel, libellés définitifs, ordre des contrôles P5 dans « Écran d'accueil », navigation D-pad détaillée et testTags de la présentation initiale et des contrôles P5 : à confirmer à la réception des maquettes Penpot.

## Maquettes

Les variantes v1 et v2 de `mockups/` sont remplacées (voir `mockups/README.md`). Les nouvelles maquettes, présentation initiale et contrôles P5 dans « Écran d'accueil », seront faites dans Penpot et fournies aux agents d'implémentation. Jusqu'à leur réception, les exigences de rendu, de disposition et de navigation D-pad détaillée restent en attente (tâches 2.3 et 3.6) ; ne pas extrapoler v1/v2.
