# Design

## Context

La motivation est dans `proposal.md`, « Why ». Point de départ : `p7-default-launcher`, implémenté sur `main` et en attente de validation TV.

Ce que P7 fournit :
- `AdbAccess` / `AdbAccessGateway` : accès par l'ADB intégré (dadb). `LocalAdbTransport.reachable()` sait déjà tester le port sans authentification.
- `PrivilegedShell` : opérations fixes, relecture après chaque opération.
- `HomeScreenStateReader` : lectures sans privilège.
- `DefaultLauncherController` : séquence, désactivation, rétablissement.
- `DefaultLauncherPanel` et `DefaultLauncherPresentation` : écrans 8.1 à 8.21.
- `DefaultLauncherPrefs` : drapeau du parcours, écran d'accueil précédent, liste des modifications.

`PrivilegedShell` a été conçu pour accueillir un second moyen d'accès (P7, D1 et décision 11).

Ce que `self-update` fournit :
- `GitHubReleaseSource` (base d'API fixée sur le dépôt de SygixOs) ;
- `UpdateRepository` : choix de version et préversions ;
- `ApkInspector` : paquet, `versionCode`, certificats ;
- `UpdateInstaller` et `PackageInstallerGateway` : session d'installation, action utilisateur différée au premier plan, statuts.

Tsuyu, état au 2026-10-17 :
- dépôt public Sygix/Tsuyu, préversions `v0.1.0-alpha.*` publiant l'asset `tsuyu.apk`, avec sa taille et son empreinte SHA-256 fournies par l'API ;
- paquet `fr.sygix.tsuyu`, signé avec la même clé que SygixOs ;
- démarrage par le port ADB réseau de la TV avec sa propre clé, après un « Toujours autoriser » unique ;
- serveur à l'identité du shell, qui survit à la fermeture de la connexion mais pas à la désactivation du débogage ;
- API Shizuku 13 (wire 13.1.5) fournie aux applications qui déclarent `moe.shizuku.manager.permission.API_V23` ;
- demande d'accord par application (« Autoriser » / « Refuser », « Refuser » sélectionné au départ, retour à l'application demandeuse) ;
- « Démarrage au redémarrage » activable dans Tsuyu ;
- `MainActivity` exportée en lancement standard (`LEANBACK_LAUNCHER`).
P2 et P3 de Tsuyu sont en validation : les libellés et comportements cités ici sont repris de son dépôt et vérifiés par la tâche 1.2.

## Goals / Non-Goals

**Goals:**
- Un utilisateur non technique termine la configuration en ne pressant que OK dans SygixOs, sans rien désactiver qui lui ferait perdre une touche de télécommande.
- Tsuyu s'obtient depuis la TV, avec la même garantie d'origine que la mise à jour de SygixOs.
- Un seul ensemble d'opérations fixes, quel que soit le moyen d'accès ; aucune nouvelle surface privilégiée.
- La réparation ne touche que ce que SygixOs a posé, sur symptôme visible, au plus une fois par démarrage.

**Non-Goals:** `proposal.md`, « Non-goals ».

## Decisions

### D1. Trois façons sur la première page, Tsuyu en tête
La première page présente trois lignes quand Tsuyu est disponible : avec Tsuyu (recommandée, focalisée), sans Tsuyu, par autorisations. Quand Tsuyu n'est pas disponible, elle garde les deux lignes de P7.
- Alternative écartée : deux niveaux de choix (automatique ou autorisations, puis Tsuyu ou non après le débogage). Elle pose deux questions au lieu d'une et rend le nombre d'étapes inconnu au départ, ce qui empêche le repère « Étape X sur N ».
- Alternative écartée : Tsuyu seul moyen de la voie automatique. Elle supprime la possibilité de désactiver le débogage après coup et ajoute une application à qui n'en veut pas.

### D2. Ce que SygixOs attend de Tsuyu
SygixOs ne dépend que de comportements publics de Tsuyu :
- l'asset `tsuyu.apk` dans les releases ;
- le paquet `fr.sygix.tsuyu` ;
- les mêmes certificats de signature que SygixOs ;
- l'API Shizuku 13 fournie par le mécanisme de « binder » au fournisseur `${applicationId}.shizuku` des applications qui déclarent `API_V23` ;
- la demande d'accord qui revient à l'application demandeuse ;
- une activité de lancement qui peut s'ouvrir dans la tâche de l'appelant.
Les libellés « Démarrer » et « Démarrage au redémarrage », cités en 9.7, sont ceux des ressources françaises de Tsuyu (vérification 1.2). Si Tsuyu change un de ces contrats, seul ce change est à reprendre.

### D3. Disponibilité et installation de Tsuyu
`GitHubReleaseSource` est paramétré par le dépôt (SygixOs ou Tsuyu). La règle de choix de version de `UpdateRepository` est réutilisée : dernière finale, préversions selon « Inclure les préversions ». L'asset attendu est `tsuyu.apk`. Une seule requête est faite à l'ouverture du parcours ou du panneau. Le dernier résultat connu (version, taille, empreinte, URL) est conservé dans DataStore pour que la ligne Tsuyu ne clignote pas.

Vérifications de `ApkInspector`, dans l'ordre :
1. taille ;
2. empreinte SHA-256 publiée ;
3. paquet `fr.sygix.tsuyu` ;
4. ensemble des certificats égal à celui de SygixOs installé.
Le certificat de SygixOs sert d'ancre : un asset modifié sur GitHub sans la clé ne passe pas.

Installation par `UpdateInstaller` / `PackageInstallerGateway`, paramétrés par le paquet attendu. Une première installation exige toujours une confirmation d'Android. Si SygixOs n'a pas l'autorisation « applis inconnues », Android l'intercale, et elle a pu être donnée pour la mise à jour de SygixOs.

Alternative écartée : installer par l'ADB intégré (`pm install`), sans écran d'Android. Cela exige d'autoriser la clé de SygixOs en plus de celle de Tsuyu (deux fenêtres « Autoriser le débogage USB ? » au lieu d'une) et ne dispense pas Tsuyu de sa propre autorisation pour redémarrer après un redémarrage de la TV.

### D4. Démarrage de Tsuyu
SygixOs ouvre la `MainActivity` de Tsuyu par un intent explicite sans `FLAG_ACTIVITY_NEW_TASK`. Tsuyu entre ainsi dans la tâche de SygixOs et Retour y ramène.

Au retour, SygixOs attend la réception du binder (`Shizuku.addBinderReceivedListenerSticky`) au plus 3 s, puis relit `Shizuku.pingBinder()` et `Shizuku.getVersion()`. SygixOs ne démarre jamais Tsuyu lui-même : le serveur de Tsuyu doit être lancé avec la clé de Tsuyu, pour que Tsuyu puisse redémarrer seul avec la TV.

### D5. Accès par Tsuyu : service utilisateur à opérations typées
Un `PrivilegedAccess` commun remplace l'usage direct de `AdbAccess` par `PrivilegedShell`, avec deux implémentations :
- l'ADB intégré, existant ;
- `ShizukuPrivilegedAccess`.
Celle-ci lie, par `Shizuku.bindUserService`, un service de SygixOs exécuté avec l'identité du shell. Son interface AIDL n'expose que les opérations typées de P7, D2 (pas de chaîne de commande). Le service traduit chaque opération en tableau d'arguments fixe et l'exécute par `ProcessBuilder`, avec le même délai par commande que l'ADB intégré.

La construction des arguments est partagée entre les deux implémentations, pour que la liste des commandes reste unique. Le service est lié au début d'une séquence et délié à la fin (`unbindUserService` avec arrêt du processus).

Choix du moyen d'accès au début de chaque séquence :
- Tsuyu « Actif » : `pingBinder`, version 13 ou plus, `checkSelfPermission` accordé ;
- sinon, l'ADB intégré.

Alternatives écartées :
- `Shizuku.newProcess` : privé depuis l'API 13, accessible seulement par réflexion ;
- un service qui accepte une chaîne de commande : cela rouvre la surface que P7 a fermée.

### D6. Repère d'avancement
Le contrôleur du parcours calcule la liste des étapes de la façon choisie à son ouverture. Il retire les étapes déjà faites :
- débogage activé ;
- Tsuyu installé dans une version suffisante ;
- Tsuyu « Actif » ;
- SygixOs autorisé (clé déjà acceptée sans Tsuyu, accord Shizuku avec Tsuyu).
« Réglages » est toujours comptée. Le repère est dérivé de cette liste et de l'écran courant, sans état dupliqué. Il n'est recalculé que lorsque l'utilisateur passe « Sans Tsuyu » : une étape réussie ne fait pas changer N, seul X avance.

### D7. Guide du débogage en deux temps
États relus sans privilège, à l'ouverture et à chaque retour au premier plan :
- `Settings.Global.DEVELOPMENT_SETTINGS_ENABLED`, pour l'étape 1 ;
- `Settings.Global.ADB_ENABLED`, pour l'étape 2. Un débogage activé rend aussi l'étape 1 « Fait ».
Le bouton ouvre `Settings.ACTION_DEVICE_INFO_SETTINGS` (repli : `Settings.ACTION_SETTINGS`) tant que l'étape 1 est à faire, puis `Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS` (repli : `ACTION_DEVICE_INFO_SETTINGS`). Le retour par Retour depuis ces écrans est vérifié par la tâche 1.1.

Sur le parcours avec Tsuyu, `LocalAdbTransport.reachable()` (connexion TCP sans échange ADB, délai de connexion de P7) est appelé dès que le débogage est activé, avant tout téléchargement.

### D8. « Un dernier choix » et remplacement depuis le panneau
L'écran 8.7 devient 9.11 : deux lignes de choix expliquées, dont « Garder » (le choix conservateur), focalisée. Retour équivaut à « Garder ». La confirmation 8.8 est inchangée.

L'action « Remplacer complètement l'accueil Google TV » du panneau réutilise la désactivation de P7 (D6), sans le repli automatique : la voie est déjà active par les réglages de secours quand l'action est proposée. En cas d'échec, l'état est annulé puis l'encadré est affiché.

Alternative écartée : retirer la désactivation du parcours. La décision 4 de P7 (« désactivation seulement si encore nécessaire, avec ses conséquences ») est maintenue ; seule la sélection par défaut change.

### D9. Réparation automatique sur symptôme
Le contrôleur de réparation s'exécute au retour au premier plan, après la relecture de l'état. Il n'agit que si toutes ces conditions sont réunies :
- Tsuyu est « Actif » ;
- aucune séquence n'est en cours ;
- un symptôme est présent ;
- le réglage visé figure dans la liste des modifications faites par SygixOs (`DefaultLauncherPrefs`).

Symptômes, tous lisibles sans privilège :
1. Le service de la touche Home est activé dans les réglages mais non connecté au terme de l'attente bornée de P7 (« Bloqué »).
2. L'ouverture à l'allumage, activée par SygixOs, n'a pas été observée au démarrage courant (enregistrement par démarrage de P5/P7).

Actions :
- Symptôme 1 ou 2 : relire l'opération `AUTO_START` (ou `APP_AUTO_START`) ; si elle existe et n'est pas `allow`, l'accorder, `write-settings`, relire.
- Symptôme 1 : écrire la liste des services sans celui de SygixOs, puis avec lui, autres services conservés. Une écriture identique ne déclenche pas de nouvelle liaison, d'où le retrait puis la remise. Puis relire la connexion pendant l'attente bornée.
Effet attendu de la remise à vérifier sur la TV (tâche 1.3).

Une tentative par réglage et par numéro de démarrage (`Settings.Global.BOOT_COUNT`, déjà lu par P5). Le résultat est horodaté dans `DefaultLauncherPrefs` pour « Détails ». Aucune lecture privilégiée n'est faite sans symptôme : le service lancé par Tsuyu ne démarre pas à chaque retour au premier plan.

Alternative écartée : vérifier `AUTO_START` à chaque retour au premier plan. Il faudrait lancer un processus privilégié à chaque fois, alors que le symptôme suffit à détecter le problème.

### D10. États et présentation
`TsuyuState` est calculé à chaque relecture, selon la priorité de « États de Tsuyu » :
- disponibilité (paquet installé, release connue) ;
- `pingBinder` ;
- version ;
- `checkSelfPermission` ;
- port de la voie automatique.

Les écrans 9.x réutilisent les composants de P7 : panneau, en-tête, étape numérotée, ligne de réglage avec statut, encadré d'erreur, pied de panneau. Le repère est un texte au-dessus du titre, dans l'en-tête, et la ligne d'étape gagne un état à droite.

La catégorie « Écran d'accueil » et le panneau doivent tenir dans 540 dp. Le panneau 9.14 a au plus trois lignes de voie et deux actions affichées en même temps : « Remplacer » et « Rétablir » s'excluent.

### D11. Manifeste et dépendances
- `dev.rikka.shizuku:api` et `dev.rikka.shizuku:provider` (13.1.5, MIT) ;
- `<uses-permission android:name="moe.shizuku.manager.permission.API_V23" />` ;
- `rikka.shizuku.ShizukuProvider` (autorité `${applicationId}.shizuku`, `exported`, permission `INTERACT_ACROSS_USERS_FULL`) ;
- `<queries><package android:name="fr.sygix.tsuyu" /></queries>`.
Aucune nouvelle permission dangereuse ; `REQUEST_INSTALL_PACKAGES` est déjà déclarée.

### D12. Tests
Des doubles remplacent `GitHubReleaseSource`, le téléchargement, `PackageInstallerGateway`, `ApkInspector`, la présence et la version de Tsuyu, l'API Shizuku (binder, version, accord, refus définitif), `PrivilegedAccess` et le test du port. Ils servent :
- en JUnit : la machine d'états du parcours (ordre des écrans, repère, passage « Sans Tsuyu »), les vérifications d'installation, le choix du moyen d'accès, la réparation (symptômes, ordre, une fois par démarrage, liste des modifications), les états de Tsuyu ;
- en tests Compose à la taille d'une TV : les écrans 9.1 à 9.16, le focus, la navigation et les textes.
Aucun réseau, aucun Tsuyu réel en CI.

## Suggestions pour Tsuyu

Ces suggestions ne sont requises par aucune exigence de ce change. Elles simplifieraient le parcours ; elles sont à porter, si le propriétaire le souhaite, dans le backlog de Tsuyu.
- Revenir à l'application appelante dès que le service est actif, quand Tsuyu a été ouvert par une autre application. Cela retire l'étape « revenez avec Retour ».
- Proposer « Démarrage au redémarrage » juste après le premier démarrage, dans le même enchaînement. Cela retire l'étape 3 de 9.7.
- Permettre de revenir sur un refus depuis Tsuyu (liste des applications refusées), pour l'écran « Tsuyu ne demande plus » (9.9).
- Une mise à jour intégrée de Tsuyu, comme `self-update`, pour que les utilisateurs non techniques ne restent pas sur une ancienne version.

## Risks / Trade-offs

- [Débogage laissé activé avec Tsuyu : port ADB réseau ouvert en permanence] → toute nouvelle clé exige une acceptation à l'écran ; texte « Gardez le débogage activé » seulement avec Tsuyu ; README ; la façon sans Tsuyu reste proposée.
- [Tsuyu compromis ou release GitHub altérée] → vérification de taille, d'empreinte et de certificat identique à SygixOs avant installation ; Tsuyu demande l'accord pour chaque application ; SygixOs n'envoie que des opérations typées.
- [Tsuyu change ses libellés ou son contrat] → contrat limité à D2 ; tâche 1.2 avant implémentation ; les textes de 9.7 et 9.8 se corrigent sans changer le comportement.
- [Une écriture de la liste des services d'accessibilité coupe brièvement d'autres services] → la liste relue doit garder tous les autres services ; vérification 1.3 sur la TV ; au plus une fois par démarrage.
- [Réparation qui contredit l'utilisateur] → seulement sur symptôme technique, seulement pour un réglage posé par SygixOs et encore activé ; rien n'est réactivé ni désactivé.
- [Deux applications à comprendre pour l'utilisateur] → Tsuyu n'apparaît que sur la première page, dans ses trois écrans d'étape et dans le panneau ; tout le reste parle de SygixOs.
- [Disponibilité lue sur une réponse ancienne] → l'installation relit toujours la release avant de télécharger (comme `self-update`) ; un échec affiche 9.6 et laisse continuer sans Tsuyu.

## Migration Plan

Aucune migration de données. Après la mise à jour :
- une installation configurée par P7 sans Tsuyu garde son état ;
- le panneau propose « Tsuyu » « À installer » quand une release est connue ;
- la réparation ne s'active que pour les configurations faites ou relancées par Tsuyu, car la liste des modifications de P7 est reprise telle quelle.
Le drapeau de fermeture du parcours est conservé : un parcours déjà fermé ne réapparaît pas automatiquement pour présenter Tsuyu. Retour arrière : désinstaller Tsuyu ou retirer l'accès de SygixOs dans Tsuyu ramène au fonctionnement de P7.

## Ordre d'archivage

1. `p7-default-launcher`, après sa validation sur la TV (tâche 6.2) : ses exigences ajoutées à `launcher-shell` et `ui-testing` deviennent celles de la spec principale.
2. `tsuyu-home-setup`, dont les deltas MODIFIED portent sur ces exigences. Un archivage dans l'autre ordre est refusé par OpenSpec (« MODIFIED failed … not found »).
Les autres changes ouverts (`hero-*`, `settings-*`, `virtualized-home-grid`, `visual-header-validation`, `artwork-tile-decode`) ne touchent pas ces exigences.

## Décisions du propriétaire

Choix faits en autonomie le 2026-10-17, à la demande du propriétaire (« complètement en autonomie »), puis validés tels quels par le propriétaire le même jour.

1. Trois lignes sur la première page quand Tsuyu est disponible, « Automatique avec Tsuyu (recommandée) » focalisée (D1) ; deux lignes de P7 sinon.
2. Principe « OK, OK, OK » : la sélection de départ mène toujours à une configuration sans désactivation ; dans « Un dernier choix » (ex-8.7), « Garder l'accueil Google TV (conseillé) » est focalisé au lieu de « Désactiver » (D8).
3. Désactivation proposée aussi plus tard, depuis le panneau (« Remplacer complètement l'accueil Google TV »).
4. Repère « Étape X sur N » sur les écrans d'étape de la configuration automatique (D6).
5. Guide du débogage en deux temps, sans la ligne « Débogage » de 8.3 ; texte qui ne promet plus de pouvoir désactiver le débogage (D7).
6. Installation de Tsuyu par l'installateur du système depuis ses releases GitHub, vérifiée par le certificat de SygixOs, préversions selon le réglage existant (D3).
7. Réparation automatique limitée à `AUTO_START` et à la liaison du service de la touche Home, sur symptôme, une fois par démarrage, avec Tsuyu seulement (D9) ; décision 9 de P7 maintenue pour l'accueil Google TV.
8. Avec Tsuyu, aucun conseil de désactiver le débogage ; consigne de le garder activé.
9. Aucune mise à jour courante de Tsuyu par SygixOs ; seulement « À mettre à jour » sous l'API 13.
10. Pas de nouvelle phase numérotée : ce change prolonge P7 dans la feuille de route (P8 et P9 inchangées).

## Questions ouvertes

Aucune.

## Maquettes

Penpot, fichier « SygixOs Maquette », page « TV · Launcher principal » :
- rangée « 8. Launcher principal » (8.1 à 8.21) : inchangée, référence de P7 ;
- rangée « 9. Parcours simplifié et Tsuyu » (9.1 à 9.16) : chaque écran porte une note de comportement.

| Écran | Contenu |
| --- | --- |
| 9.1 | Premier lancement, trois façons |
| 9.2, 9.3 | Débogage : options masquées, puis affichées |
| 9.4 | Installer Tsuyu |
| 9.5 | Installation en cours |
| 9.6 | Installation en échec |
| 9.7 | Démarrer Tsuyu |
| 9.8 | Autoriser SygixOs dans Tsuyu |
| 9.9 | Autorisation refusée |
| 9.10 | Configuration en cours |
| 9.11 | Un dernier choix |
| 9.12 | SygixOs garde la main, avec Tsuyu |
| 9.13 | SygixOs est votre écran d'accueil, avec Tsuyu |
| 9.14 | Panneau, Tsuyu actif |
| 9.15 | Panneau, Tsuyu à installer |
| 9.16 | Détails avec Tsuyu |

La maquette fait foi pour le rendu, la spec pour le comportement. Une version Penpot « Parcours simplifié et Tsuyu (Claude, 2026-10-17) » marque cet état. Dimensions : 1920 × 1080 px, soit 2 px par dp.
