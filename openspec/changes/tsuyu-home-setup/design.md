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
- « Démarrage au redémarrage » proposé par « Démarrer » (bouton « Activer Tsuyu ») ;
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
Les libellés « Démarrer » et « Activer Tsuyu », cités en 9.7, sont ceux de Tsuyu v0.1.0-alpha.4 : « Démarrer » ouvre l'écran « Démarrage au redémarrage », dont « Activer Tsuyu » prépare le redémarrage puis déclenche la fenêtre d'autorisation du débogage (vérifié sur émulateur). Si Tsuyu change un de ces contrats, seul ce change est à reprendre.

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

### D5. Accès par Tsuyu : service utilisateur à liste blanche
`SwitchingAccess` implémente `AdbAccess` et choisit, à chaque connexion, entre deux moyens :
- l'ADB intégré, existant (`AdbAccessGateway`) ;
- `TsuyuAccess`.
`TsuyuAccess` lie, par `Shizuku.bindUserService`, le service `PrivilegedOpsService` de SygixOs, exécuté avec l'identité du shell. Son interface AIDL (`IPrivilegedOps`) n'a qu'une méthode `run(command)`.

`PrivilegedCommands` construit chaque commande des opérations fixes de P7 (D2) et décrit leur forme exacte par des expressions régulières ancrées. `allowed()` refuse tout le reste : autres paquets, autres composants, enchaînements de commandes. Ce contrôle est fait trois fois : dans `PrivilegedShell` avant l'envoi, dans `TsuyuAccess`, puis dans le service lui-même avant l'exécution. Le service exécute la commande avec un délai de 10 s et renvoie code de sortie et sortie.

La liste des commandes reste unique pour les deux moyens. Le service est lié au début d'une séquence et délié à la fin (`unbindUserService` avec arrêt du processus).

Choix du moyen d'accès au début de chaque séquence :
- Tsuyu « Actif » : `pingBinder`, version 13 ou plus, `checkSelfPermission` accordé ;
- sinon, l'ADB intégré.

Alternatives écartées :
- `Shizuku.newProcess` : privé depuis l'API 13, accessible seulement par réflexion ;
- une méthode AIDL par opération : la liste des opérations serait dupliquée entre l'interface, le service et `PrivilegedShell`, alors que la liste blanche vérifiée dans le service donne la même garantie avec une seule liste.

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
- Permettre de revenir sur un refus depuis Tsuyu (liste des applications refusées). Tant que ce n'est pas possible, l'écran « Tsuyu ne demande plus » (9.9) propose de continuer sans Tsuyu. Retenue en priorité par Métis dans la PR Tsuyu #14 (« Autoriser une nouvelle demande ») ; une fois livrée, 9.9 pourra de nouveau proposer « Ouvrir Tsuyu ».
- Une mise à jour intégrée de Tsuyu, comme `self-update`, pour que les utilisateurs non techniques ne restent pas sur une ancienne version.

## Vérifications sur l'appareil

Émulateur Android TV (Android 14), 2026-10-17, APK release signé comme en production, verrou de l'émulateur pris puis rendu. L'état de l'émulateur a été rétabli à la fin : applications d'origine réinstallées, écran d'accueil rendu au launcher d'origine.

- **Parcours avec Tsuyu déjà démarré** (build de développement de Tsuyu) :
  - 9.1 affiche les trois lignes, Tsuyu sélectionné ;
  - OK mène directement à « Autoriser SygixOs » (« Étape 1 sur 2 »). La fenêtre de Tsuyu s'ouvre par-dessus SygixOs, « Refuser » sélectionné ; « Autoriser » lance la séquence par Tsuyu, sans fenêtre d'autorisation du débogage pour SygixOs ;
  - le rôle HOME, le service d'accessibilité et la superposition sont posés par le service à l'identité du shell. L'accueil d'origine de l'émulateur n'est pas dans l'ensemble fermé : la séquence finit sur « SygixOs garde la main » avec « Réparation automatique » « Active ». « Détails » liste les modifications et « Réparations automatiques » ;
  - Tsuyu désinstallé : « Réparations automatiques » passe à « Arrêtée » au retour.
- **Installation depuis le panneau** (Tsuyu v0.1.0-alpha.4, préversions activées) :
  - la ligne « Tsuyu » est « À installer » ; 9.4 sans repère (ouvert depuis le panneau) ;
  - téléchargement et vérification acceptés : certificat identique à celui de SygixOs.
  - **Constat 1** : quand SygixOs n'a pas l'autorisation « applis inconnues », Android TV affiche son écran, puis annule la session une fois l'autorisation donnée, au lieu de reprendre l'installation. D'où la relance automatique unique au retour (« Installation de Tsuyu par le système », scénario « autorisation donnée pendant l'installation »), vérifiée : la confirmation d'Android s'affiche sans autre action.
  - **Constat 2** : la confirmation d'Android sélectionne « Annuler » au départ ; le détail de l'étape 3 de 9.4 le dit.
  - Installé, l'écran passe à « Démarrer Tsuyu » ; « Ouvrir Tsuyu » ouvre Tsuyu dans la tâche de SygixOs (pile vérifiée).
  - **Constat 3** : dans Tsuyu alpha.4, « Démarrer » ouvre « Démarrage au redémarrage » ; « Activer Tsuyu » déclenche la fenêtre d'autorisation du débogage. Les étapes de 9.7 sont reprises dans cet ordre.
  - **Constat 4** : sur l'émulateur, après l'autorisation, le service de Tsuyu alpha.4 reste « arrêté », même après une seconde tentative. Signalé à Tsuyu ; SygixOs affiche correctement « Arrêté » et garde « Ouvrir Tsuyu ».
- **Audit du contrat par Tsuyu** (2026-10-17, PR Tsuyu #14, en brouillon) :
  - conformes : « Autoriser » et « Refuser », focus initial sur « Refuser », retour à l'appelant, lancement standard de la `MainActivity`, livraison du binder aux clients `API_V23` (API 13, patch 6), UserService à l'identité du shell ;
  - différents : le premier parcours passe par « Activer Tsuyu », intégré à « Démarrage au redémarrage » (9.7 corrigé) ; un refus persistant n'est pas réversible depuis Tsuyu (9.9 propose donc de continuer sans Tsuyu) ;
  - l'attente du binder (3 s) est une règle de SygixOs : le retour suivant relit l'état et « Ouvrir Tsuyu » reste proposé.
- Non vérifiable sur l'émulateur, à faire sur la TV (tâche 9.2) :
  - « Un dernier choix » et la désactivation, faute de composants de l'ensemble fermé ;
  - la réparation automatique sur un service « Bloqué » ;
  - le guide du débogage en deux temps sur des options pour les développeurs masquées.

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

Penpot, fichier « SygixOs Maquette », page « TV · Launcher principal », rangée unique « 8. Launcher principal ». Les écrans 9.1 à 9.16 remplacent 8.1, 8.3, 8.5, 8.6, 8.7, 8.9 et 8.16, supprimés le 2026-10-17. Les autres écrans de P7 restent valables : 8.2, 8.4, 8.8, 8.10 à 8.15 et 8.17 à 8.21. Chaque écran porte une note de comportement. Un écran « sans Tsuyu » a le rendu de son écran 9.x sans les éléments propres à Tsuyu.

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

La maquette fait foi pour le rendu, la spec pour le comportement. Versions Penpot : « Parcours simplifié et Tsuyu (Claude, 2026-10-17) », puis « Rangée unique 8 · parcours Tsuyu (Claude, 2026-10-17) ». Dimensions : 1920 × 1080 px, soit 2 px par dp.
