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

Décisions complémentaires du même jour, après la relecture de la PR :

12. Rôle HOME indisponible, ou dialogue de demande impossible à ouvrir : le contrôle ouvre l'écran système des applications par défaut ou de l'écran d'accueil quand l'appareil en a un ; sinon il affiche l'état « indisponible ».
13. Une installation existante qui passe à P5 voit la présentation initiale une fois.
14. La touche Home pendant la présentation initiale la ferme définitivement.
15. La procédure ADB du README est conservée, reformulée comme alternative manuelle.
16. Contrôle d'accessibilité avec le service déjà activé : il affiche « actif » et OK ouvre les réglages d'accessibilité du système.
17. La présentation initiale ne s'affiche jamais par-dessus un dialogue système : la demande de permission « Programmes TV » (`READ_TV_LISTINGS`) passe d'abord, puis la présentation.
18. La détection du démarrage automatique par numéro de démarrage et l'état « action système en attente » restent tels qu'écrits.

## Décisions du propriétaire (2026-10-10, maquettes)

Maquettes Penpot validées par le propriétaire le 2026-10-10 (voir « Maquettes ») ; elles ferment la question ouverte 5. Les valeurs ci-dessous sont reprises par D7, D9 et D10 et par les deltas `launcher-shell`, `settings` et `ui-testing`.

19. Présentation initiale (écrans 8.1 à 8.4) : un panneau `GlassSurface` (`GlassLook.Menu`) centré sur l'accueil, par-dessus le voile `SygixColors.Scrim`, avec un en-tête (mascotte fixe de l'écran de démarrage, titre « Faire de SygixOs l'écran d'accueil », description), trois lignes de réglage et un pied (aide « Retour : fermer » et bouton « Continuer »). Les écrans système Android ne sont jamais dessinés dans les maquettes.
20. Les trois lignes, dans cet ordre, avec les mêmes libellés, détails et états dans la présentation initiale et dans les réglages : « Remplacer le launcher » (détail « SygixOs devient l'écran d'accueil de la TV », état à droite) ; « Démarrer à l'allumage » (détail « Ouvrir SygixOs quand la TV s'allume », `AppleSwitch` désactivé par défaut) ; « Retour à l'accueil » (détail « Revenir à SygixOs quand la touche Home ouvre l'accueil du constructeur », état à droite).
21. États affichés à droite d'une ligne : libellé en `TextStyles.RowState`, précédé d'une pastille ronde de 6 dp pour « Actif » (pastille `SygixColors.SwitchOn`) et « En attente… » (pastille `SygixColors.Badge`) ; « Inactif » et « Indisponible » sans pastille. « Indisponible » met le titre en `SygixColors.OnDarkSecondary` hors focus et remplace le détail par la raison : « Cette TV ne permet pas de changer l'écran d'accueil » (rôle) ou « Les réglages d'accessibilité ne peuvent pas s'ouvrir sur cette TV » (accessibilité). « En attente… » n'est jamais présenté comme un succès.
22. Navigation de la présentation : focus initial sur « Remplacer le launcher » ; haut et bas parcourent les trois lignes puis « Continuer », sans boucle ; gauche et droite sans effet. OK sur « Remplacer le launcher » demande le rôle HOME (ou ouvre l'écran système de remplacement, décision 12) ; OK sur « Démarrer à l'allumage » bascule l'option, persistée aussitôt ; OK sur « Retour à l'accueil » ouvre les réglages d'accessibilité du système. « Continuer », Retour et la touche Home ferment définitivement la présentation (drapeau persisté) et rendent le focus au héro. Au retour d'un écran système, les états sont relus et le focus est sur la ligne qui l'a ouvert.
23. Réglages, catégorie « Écran d'accueil » (écrans 8.5 et 8.6) : après « Afficher Up Next » et « Position d'Up Next » (`up-next`, inchangés), un intertitre « Launcher système » non focusable, puis les trois lignes de la décision 20 dans le même ordre. Le focus initial de la catégorie reste « Afficher Up Next » ; haut et bas parcourent toutes les lignes sans boucle, l'intertitre étant sauté ; gauche rend le focus au volet des catégories ; Retour ferme les réglages.
24. « Démarrage automatique non observé » : l'interrupteur reste activé et le détail de « Démarrer à l'allumage » devient « SygixOs ne s'est pas ouvert automatiquement à ce démarrage », sans autre indicateur ; le détail normal revient quand l'ouverture est observée ou que l'option est désactivée.
25. testTags (préfixes `onboarding-` pour la présentation, `setting-` pour les réglages) : fixés dans le delta `ui-testing`, « Sélecteurs du launcher système ».
26. Confirmations du même jour, après relecture de la spec mise à jour : pastille « En attente… » en `SygixColors.Badge` (aucune nouvelle couleur) ; mascotte de la présentation = pose de repos fixe (première image de l'animation), 64 dp ; ligne « Indisponible » focalisable, OK sans effet, raison lisible au focus, dans la présentation et dans les réglages ; bouton « Continuer » de hauteur `Dimens.MenuActionHeight` (38 dp), sans jeton propre ; détail « non observé » sans autre indicateur, dans la couleur du détail normal.

## Goals / Non-Goals

**Goals:**
- Rôle HOME, démarrage à l'allumage et service d'accessibilité proposés dans la présentation initiale puis dans « Écran d'accueil », chacun après une action explicite de l'utilisateur.
- États toujours relus auprès du système au retour au premier plan ; aucune capacité annoncée comme accordée avant confirmation du système.
- Échecs non bloquants et visibles : rôle indisponible, service non activé, ouverture au démarrage non observée.
- Hypothèses plateforme vérifiées sur la TV de référence avant le code qui en dépend, résultats consignés ci-dessous.

**Non-Goals:**
- Désactivation automatique (ADB ou autre) de composants système.
- Garantie universelle du retour Home ou du démarrage automatique.
- Nouveau composant de design system : la présentation et les réglages P5 réutilisent les lignes de réglages, la pilule de focus, `AppleSwitch` et `GlassSurface` existants.

## Decisions

### D1. Rôle HOME derrière une interface
Une interface `HomeRoleGateway` (`data/`) expose l'état du rôle (`isRoleAvailable` puis `isRoleHeld`) et l'intent de demande (`createRequestRoleIntent(ROLE_HOME)`), remplacée par un double dans les tests. Le résultat de l'activité de demande n'est jamais pris pour vérité : l'état est relu à chaque retour au premier plan (`ON_RESUME`). Si le rôle est déclaré indisponible ou que la demande ne peut pas s'ouvrir (aucune activité, exception), le contrôle ouvre l'écran système des applications par défaut ou de l'écran d'accueil (`Settings.ACTION_HOME_SETTINGS`, sinon `Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS`) quand l'appareil en résout un ; sinon il affiche « Indisponible » (décision 12). Dans ce cas, l'état « Actif » ou « Inactif » relu au retour vient de l'activité d'écran d'accueil par défaut résolue par le système (`PackageManager.resolveActivity` sur `CATEGORY_HOME`). Les écrans disponibles sur la TV de référence sont relevés par la tâche 1.2. Le parcours de mise à jour (`self-update`) n'a pas accès à cette interface.

### D2. États des contrôles système
Un même modèle d'état sert la présentation initiale et les réglages : actif, inactif, indisponible, action système en attente, affichés « Actif », « Inactif », « Indisponible » et « En attente… » (décision 21, D10). L'état « action système en attente » commence quand SygixOs ouvre un écran ou un dialogue système pour un contrôle et se termine au retour au premier plan suivant, qui relit l'état réel ; il n'est jamais persisté (un processus recréé relit directement l'état). Il est exposé par le ViewModel des réglages et de l'accueil (StateFlow), aucune logique dans les composables.

### D3. Présentation initiale
Persistance d'un drapeau « présentation initiale fermée » dans `LauncherPrefs`, écrit à toute fermeture (« Continuer », Retour ou touche Home ; décision 22). Tant qu'il est absent, la présentation s'affiche une fois l'accueil interactif, après la fin de l'écran de démarrage et après la réponse à la demande de permission « Programmes TV » quand celle-ci est affichée, jamais par-dessus un dialogue système (décision 17) ; une installation existante qui passe à P5 la voit une fois (décision 13). Si le processus est arrêté pendant qu'un écran système ouvert depuis la présentation est affiché, la présentation réapparaît au lancement suivant avec les états relus. Contenu : rôle HOME (état et demande, D1), choix du démarrage à l'allumage (D4), service d'accessibilité (état et ouverture des réglages d'accessibilité du système, D5). Rendu et navigation : D9.

### D4. Démarrage à l'allumage
- Option persistée dans `LauncherPrefs`, désactivée tant que l'utilisateur ne l'a pas choisie (présentation initiale ou réglages). À l'activation, SygixOs mémorise le numéro de démarrage courant (`Settings.Global.BOOT_COUNT`).
- Récepteur `BOOT_COMPLETED` déclaré (permission `RECEIVE_BOOT_COMPLETED`) : option désactivée, il ne fait rien ; activée, il demande l'ouverture de `MainActivity` avec un extra qui marque l'ouverture au démarrage. Mécanisme confirmé ou ajusté par la tâche 1.1 avant toute implémentation.
- `MainActivity` qui reçoit cet extra (création ou `onNewIntent`) mémorise « ouverture au démarrage observée » pour le numéro de démarrage courant.
- État affiché : « démarrage automatique non observé » quand l'option est activée, qu'elle l'était avant le démarrage courant et qu'aucune ouverture au démarrage n'a été observée pour ce démarrage. Couvre la diffusion ignorée par le constructeur comme l'ouverture refusée par Android. L'état est réactif : une ouverture observée plus tard dans le même démarrage l'efface ; désactiver l'option l'efface.

### D5. Service d'accessibilité
Service déclaré avec la permission `BIND_ACCESSIBILITY_SERVICE` et une description française qui en dit l'usage (retour à SygixOs quand la touche Home est détournée). Le signal utilisé (touche Home filtrée par le service, ou passage au premier plan du launcher du constructeur) est choisi d'après la tâche 1.3 ; le service ne lit que les événements nécessaires à ce signal et ne conserve rien. Sur ce signal, il ouvre `MainActivity` comme une touche Home (D6 s'applique si SygixOs était déjà au premier plan). L'état activé est lu par `AccessibilityManager` au retour au premier plan. Le contrôle ouvre `Settings.ACTION_ACCESSIBILITY_SETTINGS`, que le service soit actif ou non ; SygixOs ne l'active ni ne le désactive jamais lui-même.

### D6. Touche Home avec SygixOs au premier plan
`MainActivity.onNewIntent` transmet au ViewModel de l'accueil un événement « Home » quand l'intent porte `CATEGORY_HOME` et que l'activité était au premier plan à sa réception, ou quand l'intent vient du service (D5) et que SygixOs était au premier plan avant l'appui détourné. L'événement ferme entièrement la page de réglages en une fois, y compris quand la liste déroulante « Position d'Up Next » (`up-next`) est ouverte, sans passer par la fermeture de la liste seule que fait Retour ; il ferme aussi toute surcouche de l'accueil comme le ferait Retour sur elle (menu contextuel, mode « Déplacer » annulé, présentation initiale fermée définitivement), ramène la page en haut et donne le focus au héro. Hors premier plan (retour depuis une autre app), les règles de retour existantes s'appliquent sans changement. Pendant l'écran de démarrage, l'événement est sans effet.

### D7. Réglages dans « Écran d'accueil »
Les trois contrôles P5 sont des lignes de `SettingsCategory.HOME_SCREEN`, placées après « Afficher Up Next » et « Position d'Up Next » (`up-next`), sous un intertitre « Launcher système » non focusable ; « Afficher Up Next » reste le premier contrôle et le focus initial de la catégorie (`settings`). Aucune nouvelle entrée de `SettingsCategory`. Ordre, libellés, détails et états : décisions 20, 21 et 24 ; les libellés sont des ressources françaises partagées avec la présentation initiale (une seule chaîne par libellé, détail et état). Rendu : D10. testTags : delta `ui-testing`.

### D8. `self-update`
Delta MODIFIED sur « Redémarrage après la mise à jour » : la mise à jour ne demande jamais le rôle HOME ; seule une action explicite de l'utilisateur dans la présentation initiale ou les réglages le peut. Le reste de l'exigence est inchangé ; le résultat de la tâche 1.4 y est reporté avant l'archivage (tâche 4.3).

### D9. Rendu de la présentation initiale (maquettes 8.1 à 8.4)
Composition, de l'extérieur vers l'intérieur, avec les jetons existants ; les valeurs nouvelles sont nommées comme futurs jetons de `Dimens` :
- Voile plein écran `SygixColors.Scrim` sur l'accueil (le héro reste rendu dessous, sa lecture continue), comme le menu contextuel (`AppContextMenu`).
- Panneau centré `GlassSurface` avec `GlassLook.Menu` : largeur `OnboardingWidth` = 564 dp, coins `OnboardingCorner` = 28 dp, padding `OnboardingPadding` = 14 dp, `OnboardingSpacing` = 4 dp entre l'en-tête, chaque ligne et le pied. Hauteur donnée par le contenu.
- En-tête : retraits `OnboardingHeaderInsetH` = 14 dp à gauche et à droite, `OnboardingHeaderInsetTop` = 14 dp, `OnboardingHeaderInsetBottom` = 12 dp, `OnboardingHeaderSpacing` = 6 dp entre les éléments ; mascotte `OnboardingMascot` = 64 dp de côté, image fixe de la mascotte de l'écran de démarrage (première image de l'animation, pose de repos, « Animation de la mascotte » de `launcher-shell`), sans lecture de l'animation (décision 26) ; titre « Faire de SygixOs l'écran d'accueil » en `TextStyles.SettingsTitle`, `SygixColors.OnDark` ; description en `SygixTypography.bodyMedium`, `SygixColors.OnDarkSecondary` : « Trois réglages pour que la TV s'ouvre toujours sur SygixOs. Rien n'est activé sans votre accord, et vous les retrouverez dans Réglages › Écran d'accueil. »
- Lignes : largeur du panneau moins le padding, même rendu que les lignes de réglages (« Page de réglages » de `settings` : `RowShape`, coins `Dimens.SettingsRowCorner`, hauteur minimale `Dimens.SettingsRowHeight`, `RowPadding` et `RowVerticalPadding`, `focusPill` avec `animatedPillColors`, sans zoom) ; titre `TextStyles.Row` au repos et `TextStyles.RowFocused` au focus, couleur `pillColors.content` ; détail `TextStyles.RowSecondary`, couleur `pillColors.secondary` ; à droite, l'état (D10, « Rendu d'un état ») ou l'`AppleSwitch` de « Démarrer à l'allumage ».
- Pied : retrait gauche 14 dp (`OnboardingHeaderInsetH`), `OnboardingFooterTop` = 10 dp au-dessus ; aide « Retour : fermer » en `TextStyles.RowSecondary`, `SygixColors.OnDarkSecondary`, à gauche ; bouton pilule « Continuer » à droite : hauteur `Dimens.MenuActionHeight` (38 dp, décision 26), coins `Dimens.PillCorner`, retrait horizontal `OnboardingButtonPaddingH` = 20 dp, fond `animatedPillColors(focused, rest = SygixColors.PillRest)` (donc `PillRest` au repos, `PillFocus` au focus), texte `TextStyles.Row` / `TextStyles.RowFocused`, couleur `pillColors.content`.
- Navigation : décision 22 ; la présentation capture le D-pad (aucun déplacement vers le héro, le dock ou la capsule tant qu'elle est affichée). Les écrans de référence : 8.1 état initial (focus sur « Remplacer le launcher », trois états « Inactif », switch désactivé) ; 8.2 « Remplacer le launcher » « En attente… » pendant que le dialogue système est ouvert ; 8.3 retour : rôle « Actif », démarrage activé, focus sur « Retour à l'accueil » « Inactif » ; 8.4 rôle « Indisponible » focalisé.

### D10. Rendu des contrôles dans « Écran d'accueil » (maquettes 8.5 et 8.6)
- Intertitre « Launcher système » : `SygixTypography.labelMedium` (12 sp Medium, interlettrage 1,2 sp), `SygixColors.OnDarkSecondary`, retrait gauche `RowPadding` (14 dp), `SettingsGroupTitleTop` = 15 dp au-dessus et `SettingsGroupTitleBottom` = 3 dp en dessous, jamais focusable (`focusProperties { canFocus = false }`), sauté par haut et bas.
- Les trois lignes reprennent le composant de ligne de la présentation (D9), avec la pilule de focus de « Page de réglages » ; même ordre, libellés, détails, états et switch.
- Rendu d'un état (partagé avec la présentation) : à droite de la ligne, libellé en `TextStyles.RowState`, couleur `pillColors.tertiary` ; pour « Actif » et « En attente… », une pastille ronde de `Dimens.Badge` (6 dp) précède le libellé avec `StateDotGap` = 5 dp d'écart, `SygixColors.SwitchOn` pour « Actif », `SygixColors.Badge` pour « En attente… » (décision 26, aucune nouvelle couleur) ; « Inactif » et « Indisponible » sans pastille. « Indisponible » : titre en `SygixColors.OnDarkSecondary` hors focus (couleur de focus inchangée) et détail remplacé par la raison (décision 21).
- « Démarrage automatique non observé » : décision 24, seul le détail change, dans la couleur du détail normal (`pillColors.secondary`, décision 26) ; le switch reste à l'état activé.
- Ligne « Indisponible » (présentation et réglages) : reste focalisable, OK est sans effet, la raison reste lisible au focus (décision 26).
- Écran 8.6 : rôle « Actif », « Démarrer à l'allumage » activé et non observé, focalisé, « Retour à l'accueil » « Indisponible ».

## Vérifications sur l'appareil

Résultats à consigner ici par les tâches 1.x (structure et comportements seulement : versions d'API, résultats, extraits de journal sans donnée personnelle ; jamais de modèle exact, de numéro de série ni d'adresse).

| Vérification | Tâche | Résultat |
| --- | --- | --- |
| Fin du démarrage reçue par le récepteur statique, ouverture de l'activité acceptée (sans rôle HOME, avec rôle HOME, avec le service d'accessibilité activé) | 1.1 | à consigner |
| Rôle HOME : disponibilité, dialogue de demande, acceptation, refus, refus répété ; écran système des applications par défaut ou de l'écran d'accueil disponible ou non ; touche Home qui atteint `onNewIntent` quand SygixOs détient le rôle | 1.2 | à consigner |
| Signal transmis au service d'accessibilité quand la touche Home est pressée et que le constructeur affiche son launcher | 1.3 | à consigner |
| Relance de SygixOs après sa propre mise à jour intégrée quand il détient le rôle HOME (sujet 9) | 1.4 | à consigner |

## Risks / Trade-offs

- [Le service d'accessibilité est destiné à l'assistance ; son usage pour le retour Home peut être refusé par une politique de distribution] → Service jamais activé par SygixOs, description explicite de son usage, aucune donnée lue au-delà du signal Home ni conservée ; distribution actuelle par les releases GitHub. Une distribution sur un autre canal exigera une revue de conformité.
- [Rôle HOME, touche Home et démarrage varient selon le constructeur et la version] → Vérifications 1.x avant le code ; états « Indisponible » et « démarrage automatique non observé » au lieu d'une promesse.
- [Android 14 peut refuser l'ouverture depuis le récepteur de fin de démarrage] → Tâche 1.1 identifie les cas acceptés ; l'échec est détecté et affiché (D4).
- [« Démarrage automatique non observé » peut s'afficher brièvement si SygixOs est ouvert avant la diffusion de fin de démarrage] → État réactif, effacé dès que l'ouverture est observée dans le même démarrage.
- [Une touche Home pendant un écran système ouvert depuis la présentation initiale ferme cette présentation] → Conforme aux décisions 9 et 14 ; les mêmes contrôles restent dans « Écran d'accueil ».

## Migration Plan

Aucune donnée existante migrée. Nouvelles clés DataStore (présentation initiale fermée, option de démarrage, numéros de démarrage) absentes = valeurs par défaut (présentation à afficher, option désactivée). SygixOs ne modifie jamais lui-même le rôle HOME ni les réglages d'accessibilité. Archivage après merge, validation sur la TV réelle et archivage de `up-next`.

## Ordre d'archivage

`up-next` puis `p5-real-launcher` : P5 ajoute ses contrôles à la catégorie « Écran d'accueil » que `up-next` introduit dans `settings`. Les deux changes touchent `launcher-shell` et `ui-testing` sans modifier les mêmes exigences (P5 n'y ajoute que des exigences). `jellyfin-tvprovider-only` est indépendant.

## Questions ouvertes

1. « Remplacer le launcher » quand le rôle est déjà détenu : A) aucune action, l'état « actif » est affiché — proposé, à confirmer ; B) ouvrir l'écran système des applications par défaut ou de l'écran d'accueil pour le rendre.
2. Ouverture au démarrage refusée par Android hors exemption (résultat de 1.1) : A) s'en tenir aux cas déjà permis (rôle HOME, service d'accessibilité activé) et afficher l'échec — proposé, à confirmer ; B) demander en plus la permission « Afficher par-dessus d'autres applis ».
3. Aucun signal Home exploitable transmis au service d'accessibilité sur la TV de référence (résultat de 1.3) : A) suspendre l'implémentation du service et revenir vers Simon — proposé, à confirmer ; B) le livrer quand même pour d'autres appareils.
4. Sujet 9, si SygixOs n'est pas relancé après sa mise à jour même avec le rôle HOME (résultat de 1.4) : A) consigner le résultat et laisser le sujet 9 ouvert au backlog — proposé, à confirmer ; B) traiter la relance dans P5 (piste du backlog : statut de session transmis à une activité).

La question 5 (rendu, libellés, ordre, navigation D-pad et testTags) est fermée par les décisions 19 à 25 (maquettes validées le 2026-10-10).

## Maquettes

Référence : Penpot, fichier « SygixOs Maquette » (équipe Sygix), page « TV », écrans 8.1 à 8.6 (présentation initiale 8.1 à 8.4, catégorie « Écran d'accueil » 8.5 et 8.6 ; les écrans 6.x et 7.7 montrent la catégorie avant P5) ; composants de la page « Composants », section « Launcher système (P5) ». Les maquettes sont dessinées à 1920 × 1080 px : une mesure en px vaut 2 × sa valeur en dp. Les captures ne sont pas versionnées pour l'instant (`mockups/README.md` renvoie vers Penpot). La maquette fait foi pour le rendu, la spec pour le comportement ; en cas d'écart, la spec est corrigée après décision du propriétaire, jamais extrapolée. Les anciennes maquettes v1 et v2 sont supprimées (décision 11).
