# Design

## Context

Motivation : `proposal.md`, « Why ». État de départ : `up-next`, `jellyfin-tvprovider-only` et `p5-real-launcher` sont archivés sur `main` ; P5 fournit le parcours guidé (étapes touche Home, ouverture à l'allumage avec la sous-étape d'affichage superposé, écran d'accueil par défaut facultatif) et une seule ligne de synthèse dans la catégorie « Écran d'accueil ».

Constats de SygixOs :
- **Écran d'accueil** : SygixOs déclare `MAIN` + `HOME` + `DEFAULT` sur `.ui.MainActivity`. Sur l'émulateur Android TV (Android 14, launcher `com.google.android.tvlauncher`) utilisé pour valider P5, le rôle HOME passe à SygixOs mais le contrôleur de permissions rouvre aussitôt le launcher fourni ; la touche Home et le démarrage à froid ouvrent toujours ce launcher, de priorité `HOME` plus élevée ; SygixOs ne devient l'écran d'accueil effectif qu'une fois ce launcher désactivé.
- **Auto-démarrage du constructeur** (TV de référence TCL, Android 14, v0.1.0-rc.1 et rc.2) : le gestionnaire d'auto-démarrage (`TclAppBoot`, politique « gestion manuelle ») bloque, après un redémarrage complet, la diffusion `BOOT_COMPLETED` (« Skipping delivery … for auto run ») et la liaison du service d'accessibilité (« forbid bind service … callee_does't_have_OP_AUTO_START ») pour toute application tierce sans l'opération d'application `AUTO_START` ; la mise en veille n'est pas concernée. Le menu du constructeur qui l'accorde (« Dispositif de protection ») est masqué sur ce firmware ; `appops set fr.sygix.sygixos AUTO_START allow` par le shell l'accorde.
- **README** : procédure ADB manuelle, conservée par P5 comme alternative : `pm disable-user --user 0` sur `com.google.android.apps.tv.launcherx` et `com.google.android.tungsten.setupwraith`.

Sources publiques relevées (2026-10-12, à confirmer sur la TV de référence par les tâches 1.x) :
- Projectivy Launcher n'est pas l'écran d'accueil du système sans ADB : son remplacement repose sur un service d'accessibilité qui filtre la touche Home et repère l'activité du launcher d'origine ; sur TCL, ce service n'est pas relié après un redémarrage complet, pour la même raison `AUTO_START`. Les utilisateurs qui le font tenir passent par ADB (`appops set <paquet> AUTO_START allow`, ou `APP_AUTO_START` selon le firmware, et `cmd package set-home-activity`), par l'écran d'auto-démarrage du constructeur sur les anciens firmwares, ou par Launcher Manager.
- Launcher Manager (Android TV) n'a que la permission `INTERNET` : il se connecte à l'ADB de l'appareil sur l'adresse locale, port 5555, désactive `launcherx`, `setupwraith` et `tvlauncher` puis appelle `cmd package set-home-activity`. Utilisé sur des TCL sans perte de la configuration au redémarrage ; perte signalée du panneau de réglages rapides du constructeur.
- TvQuickActions intègre un client ADB : l'utilisateur active le débogage et accepte « Toujours autoriser », puis l'application s'accorde ses autorisations par le shell. Sa FAQ TCL renvoie aux écrans du constructeur (lancement automatique, service sans écran) pour le démarrage.
- Sur certains firmwares TCL, `cmd package set-home-activity` et l'autorisation `AUTO_START` suffisent sans rien désactiver ; sur d'autres, l'autorisation revient à `ignore` à chaque démarrage et seule une modification de la politique du constructeur (`AppBootPolicy`, listes blanches d'auto-démarrage) la rend durable. `cmd appops write-settings` est cité comme nécessaire pour que l'opération persiste.
- Désactiver `launcherx` exige de désactiver aussi `setupwraith`, qui le réactive. Pertes signalées par des utilisateurs isolés (forums, une TV chacun), non vérifiées : choix du profil Google TV, panneau de réglages rapides ouvert par le bouton engrenage, guide des programmes du bouton CH sur TCL ; un écran noir nécessitant une réinitialisation a été signalé sur un appareil. L'interface les présente donc comme possibles « selon la TV », et la tâche 1.3 les relève sur la TV de référence.
- Activer le débogage USB ouvre sur TCL le port ADB réseau 5555, même quand le débogage sans fil n'est pas proposé (retour d'utilisateur, à vérifier par la tâche 1.1).
- Libellés Android (traductions françaises d'AOSP `android14-release`) : fenêtre « Autoriser le débogage USB ? » avec la case « Toujours autoriser sur cet ordinateur » et les boutons « Autoriser » et « Annuler » (SystemUI) ; « Options pour les développeurs », « Débogage USB » (SettingsLib) ; « À propos », « Build Android TV », « Superposer aux autres applications » (TvSettings) ; l'application Réglages s'appelle « Paramètres ». Google TV et les constructeurs peuvent les modifier : la maquette reste générique sur le chemin (« la ligne du build, son nom varie selon la TV ») et la tâche 1.1 relève les libellés réels. Le débogage sans fil (Android 11+) exige un appairage par code affiché dans une notification, absente sur Google TV.
- Shizuku officiel n'est pas utilisable à la télécommande et ne s'associe pas sur Google TV sans ordinateur ; Shevery est dans le même cas (constat du propriétaire, 2026-10-12).

Plateforme (sources AOSP `android14-release`) :
- Le shell (uid 2000) détient `CHANGE_COMPONENT_ENABLED_STATE`, `MANAGE_ROLE_HOLDERS`, `WRITE_SECURE_SETTINGS`, `MANAGE_APP_OPS_MODES` et `QUERY_ALL_PACKAGES`. Il ne peut poser que `COMPONENT_ENABLED_STATE_DEFAULT`, `ENABLED` et `DISABLED_USER` ; un paquet `DISABLED` ou `DISABLED_UNTIL_USED` posé par le système n'est pas modifiable, un paquet protégé refuse d'être désactivé.
- Après le changement d'état d'un paquet, le contrôleur des rôles réévalue le rôle HOME de façon asynchrone. `cmd package set-home-activity` attribue le rôle HOME (`RoleManager.addRoleHolderAsUser`) ; il diffère du dialogue de P5 en ce qu'il ne passe pas par le contrôleur de permissions (effet sur Google TV à vérifier, tâche 1.3).
- `getApplicationEnabledSetting` et `resolveActivity`/`queryIntentActivities` sur l'intent `HOME` se lisent sans privilège avec la visibilité (`<queries>`) ; `resolveActivity` renvoie `ResolverActivity` quand la résolution est ambiguë. `Settings.Global.ADB_ENABLED`, `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` et `Settings.canDrawOverlays` se lisent sans privilège. L'état d'une opération d'application propre au constructeur ne se lit pas sans privilège.

## Goals / Non-Goals

**Goals:**
- Faire de SygixOs l'écran d'accueil effectif en une action quand le débogage est disponible, avec la configuration la moins intrusive qui marche sur l'appareil.
- Ne jamais laisser l'appareil sans écran d'accueil par une opération de SygixOs : préconditions, vérification après coup, annulation en cas d'échec, état partiel toujours rétablissable.
- Un état toujours relu auprès du système, exact sans débogage (après un redémarrage, débogage désactivé).
- Surface privilégiée minimale : opérations fixes, limitées au paquet SygixOs et à l'ensemble fermé des composants de l'accueil d'origine ; connexion ouverte seulement pendant une séquence demandée.
- Une interface qui parle des effets pour l'utilisateur ; la mécanique seulement dans « Détails ».

**Non-Goals:** `proposal.md`, « Non-goals ».

## Decisions

### D1. Accès privilégié par l'ADB local
SygixOs se connecte au démon ADB de l'appareil sur lequel il tourne (adresse de bouclage, port ADB réseau 5555), avec une paire de clés RSA propre générée à la première connexion et conservée dans son stockage privé ; l'utilisateur l'autorise une fois dans le dialogue système (« Toujours autoriser »). Bibliothèque : `dev.mobile:dadb` (Kotlin, Apache-2.0, protocole ADB sur TCP et authentification par clé, sans binaire `adb`), à confirmer par la tâche 1.1 sur la TV de référence ; elle ne gère pas l'appairage TLS du débogage sans fil, hors P7 (décision 12) : sur une TV dont le débogage n'expose pas de port ADB réseau, la voie automatique est « Indisponible sur cette TV » et renvoie à la voie « Par autorisations ». Alternatives écartées : Shizuku et gestionnaires compatibles (association impossible sans ordinateur sur TV, `proposal.md`), binaire `adb` embarqué (taille, licences, maintenance), `libadb-android` (appairage TLS inutile en P7, intégration plus lourde). La passerelle (`PrivilegedShell`, D2) isole ce moyen d'accès pour qu'un second moyen (gestionnaire compatible Shizuku) puisse s'ajouter plus tard sans toucher au domaine ; ce second moyen est hors P7 (décision 11).
États de l'accès, lus à l'ouverture de la page et au retour au premier plan : débogage désactivé (`ADB_ENABLED` = 0, sans connexion), connexion impossible (port fermé, refus ou délai), autorisation en attente (connexion ouverte, clé non encore acceptée), autorisation refusée, prêt (commande de test `id` réussie avec l'uid du shell). Délais : connexion et autorisation bornées (60 s pour l'autorisation, le temps de lire le dialogue), chaque commande bornée (10 s), valeurs à confirmer par la tâche 1.7.

### D2. Opérations fixes
`PrivilegedShell` (`data/`) n'expose que des opérations typées, chacune traduite en une commande du shell aux arguments fixes ; le paquet visé est toujours `fr.sygix.sygixos` ou un membre de l'ensemble fermé (D4), toute autre cible est refusée avant envoi :
- lecture et accord de l'autorisation de démarrage du constructeur (`appops get|set fr.sygix.sygixos <op> allow` pour l'opération présente parmi `AUTO_START` et `APP_AUTO_START`, puis `cmd appops write-settings`) ;
- lecture de l'écran d'accueil par défaut (`cmd role get-role-holders android.app.role.HOME`) et définition (`cmd package set-home-activity fr.sygix.sygixos/.ui.MainActivity`, ou le composant précédemment relevé pour le rétablissement) ;
- état, désactivation (`pm disable-user --user <utilisateur courant>`) et remise à l'état par défaut (`pm default-state --user <utilisateur courant>`) des composants de l'ensemble fermé ;
- accord de l'affichage superposé (`appops set fr.sygix.sygixos SYSTEM_ALERT_WINDOW allow`) ;
- activation du service d'accessibilité de SygixOs en conservant les autres (`settings put secure enabled_accessibility_services` avec la liste relue augmentée de `fr.sygix.sygixos/.data.HomeAccessibilityService`, puis `settings put secure accessibility_enabled 1`) ;
- lecture et mise à jour de la politique d'auto-démarrage du constructeur, limitée à l'ajout de `fr.sygix.sygixos` à ses deux listes blanches (D12).
Chaque opération est suivie d'une relecture qui fait foi ; la sortie de la commande ne suffit jamais à déclarer un succès. Aucune autorisation n'est révoquée par SygixOs. La connexion est fermée à la fin de chaque séquence.

### D3. Frontières et état
- `AdbAccessGateway` (`data/`) : états de D1, génération et stockage de la clé, connexion, autorisation, délais ; `Result` pour chaque appel.
- `HomeScreenStateReader` (`data/`) : écran d'accueil effectif, présence et état de chaque composant de l'ensemble (actif, désactivé par l'utilisateur, hors de portée), configuration de l'appareil (`DEVICE_PROVISIONED`, `user_setup_complete`), service d'accessibilité activé et connecté, affichage superposé ; lectures sans privilège, erreur de lecture distincte de l'absence.
- `DefaultLauncherController` (`domain/`, sur le modèle de `LauncherSystemController`) : machine d'états de la configuration automatique (D5), de la désactivation et du rétablissement (D6), relecture au retour au premier plan, opération qui continue si le parcours se ferme (portée du contrôleur, pas de la composition) ; exposé par un ViewModel (StateFlow), aucune logique dans les composables.
- Persistance minimale (DataStore) : le composant d'écran d'accueil par défaut relevé avant la première définition par SygixOs (pour le rétablissement), la liste des modifications faites par SygixOs (pour « Détails ») et les sauvegardes de la politique d'auto-démarrage du constructeur (D12). L'état affiché vient toujours du système ; ces valeurs ne servent qu'au rétablissement et aux détails.

### D4. Ensemble fermé des composants de l'accueil d'origine
Arrêté après la tâche 1.2 et utilisé à l'identique pour la lecture, la validation des opérations et « Détails ». Candidats : `com.google.android.tungsten.setupwraith`, `com.google.android.apps.tv.launcherx`, `com.google.android.tvlauncher`. Un état désactivé exige que tous les composants présents et modifiables le soient ; sinon il est partiel. SygixOs, l'écran de secours des Réglages et les autres activités HOME ne sont jamais modifiés.

### D5. Séquence de configuration automatique
Déclenchée par l'action explicite « Lancer la configuration » (ou « Relancer », « Réessayer »), accès prêt :
1. Autorisation de démarrage du constructeur : si l'opération existe et n'est pas déjà `allow`, accord puis `write-settings` et relecture ; si elle n'existe pas, étape sans objet. Puis, si l'appareil a la politique d'auto-démarrage du constructeur (D12) et que SygixOs n'est pas dans ses deux listes blanches, ajout avec sauvegarde préalable, puis relecture ; un échec de cette partie est rapporté sans arrêter la séquence.
2. Écran d'accueil : relevé du détenteur courant (mémorisé s'il n'est pas SygixOs et qu'aucun relevé n'existe), puis `set-home-activity` vers SygixOs et relecture.
3. Vérification, pendant 3 s au plus (réattribution asynchrone) : SygixOs est l'écran d'accueil effectif quand `resolveActivity(HOME)` renvoie SygixOs et qu'aucune autre activité `HOME` activée n'a une priorité supérieure à la sienne.
4. Effectif : configuration terminée (8.6). Sinon, si l'accueil d'origine a au moins un composant actif et modifiable et que les garde-fous (D6) le permettent, l'écran 8.7 propose de le désactiver (confirmation 8.8, D6) ; acceptée et réussie, la configuration est terminée.
5. Sinon (« Plus tard » en 8.7, garde-fous, échec annulé) : réglages de secours posés sans autre confirmation, chacun relu : service d'accessibilité activé, superposition accordée, option « Démarrage à l'allumage » activée ; résultat 8.9. La liaison effective du service est relue au retour au premier plan.
Une étape en échec est rapportée avec son effet pour l'utilisateur (8.12) ; les étapes qui en dépendent sont annulées, les étapes indépendantes continuent (l'échec de l'étape 1 n'empêche pas l'étape 2) ; aucune étape n'est réessayée sans action. Débogage perdu en cours de séquence : arrêt, relecture, échec rapporté, état partiel rétablissable.

### D6. Désactivation, rétablissement et garde-fous
Désactivation : préconditions relues juste avant (accès prêt, appareil configuré, activité d'accueil de SygixOs déclarée et activée, au moins un composant actif) ; pour chaque composant actif, dans l'ordre de l'ensemble (le composant qui réactive le launcher en premier, ordre confirmé par la tâche 1.3) : désactivation puis relecture ; puis vérification de l'écran d'accueil effectif (D5, étape 3). Échec ou délai : remise à l'état par défaut, dans l'ordre inverse, des composants désactivés par cette opération, relecture et détail d'échec. Rétablissement : proposé quand au moins un composant est `DISABLED_USER` ; préconditions : accès prêt ; remise à l'état par défaut des composants `DISABLED_USER` dans l'ordre inverse, puis, si un détenteur précédent a été relevé et est présent et activé, `set-home-activity` vers lui ; relecture. Les autorisations accordées, le service d'accessibilité et l'inscription de SygixOs dans la politique d'auto-démarrage du constructeur ne sont pas modifiés (décisions 13 et 14) ; l'utilisateur peut révoquer l'accessibilité et l'affichage superposé dans Android.

### D7. Parcours et présentation
Le rendu et les textes sont fixés par la rangée « 8. Launcher principal » de la maquette (page « TV · Launcher principal », écrans 8.1 à 8.21, « Maquettes »). Enchaînement :
- 8.1 (premier lancement, ou 8.2 quand la voie automatique est indisponible) : les deux voies avec leur état, « Plus tard ».
- Configuration automatique : 8.3 (activer le débogage) → retour avec le débogage activé → 8.4 (autoriser SygixOs) → « Continuer » et fenêtre Android acceptée → 8.5 (en cours) → 8.6 (terminée) ; ou, si l'accueil d'origine garde la main, 8.7 (encore une étape) → « Désactiver l'accueil Google TV » → 8.8 (confirmation, « Annuler » focalisé) → 8.6 ; « Plus tard » en 8.7 → réglages de secours → 8.9. Erreurs dans l'écran concerné : 8.10 (débogage désactivé), 8.11 (autorisation refusée), 8.12 (étape en échec, suite annulée, « Réessayer »).
- Par autorisations : 8.13.
- « Détails » (bouton secondaire de 8.6 et 8.9) : 8.14.
- Réglages : ligne 8.15 (états 8.18 à 8.20) → panneau « Launcher principal » 8.16 (8.21 quand rien n'est configuré) (états des deux voies, « Relancer la configuration », « Rétablir l'accueil Google TV » quand l'accueil d'origine est désactivé) → 8.17 (confirmation, « Rétablir » focalisé).
La mascotte figure sur les panneaux du parcours, pas sur les dialogues de confirmation ni sur « Détails ». Les écrans Android (options pour les développeurs, fenêtre d'autorisation du débogage, réglages d'accessibilité) ne sont pas dessinés. La catégorie « Écran d'accueil » doit tenir dans 540 dp sans défilement.

### D8. Écrans d'auto-démarrage du constructeur
Pour la voie « Par autorisations », l'étape « Ouvrir SygixOs à l'allumage » ouvre, quand l'appareil en déclare un, l'écran du constructeur qui autorise le démarrage automatique des applications (candidats : actions `com.tcl.guard.action.PermissionShield`, `com.tcl.guard.action.PermissionManager` et `com.tcl.guard.action.SAFETY_GUARD_HOME`, déclarées par `<queries>` et résolues par `queryIntentActivities`, retenues après la tâche 1.6) ; sinon le détail de l'étape explique de chercher un réglage de démarrage automatique des applications dans les réglages de la TV. SygixOs ne peut pas lire cet état sans privilège : l'étape reste fondée sur l'ouverture observée au démarrage (« Démarrage à l'allumage »).

### D9. Débogage laissé actif
Le débogage ouvre un port ADB réseau sur certaines TV : toute machine du réseau local peut demander une autorisation, que l'utilisateur doit accepter à l'écran. Après une configuration réussie, la ligne « Conseil : désactivez le débogage » de 8.6 le conseille et précise qu'il faudra le réactiver pour relancer la configuration ou rétablir l'accueil Google TV (la clé de SygixOs reste autorisée tant que l'utilisateur ne révoque pas les autorisations de débogage). SygixOs ne désactive jamais le débogage lui-même.

### D10. Tests
Doubles de `AdbAccessGateway`, `PrivilegedShell` et `HomeScreenStateReader` ; JUnit pour la machine d'états, la séquence de D5 (ordre, étapes sans objet, repli, échecs indépendants, délais, perte de l'accès, aucune nouvelle tentative sans action), la désactivation et le rétablissement (ordre, annulation, relevé du détenteur précédent), et la validation des cibles dans `PrivilegedShell` ; tests Compose à la taille TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) pour les pages, le guide, les confirmations, « Détails », la navigation D-pad et la ligne des réglages ; aucune connexion ADB réelle en CI.

### D11. Repli sans SygixOs
La procédure ADB du README reste documentée comme alternative et comme moyen de rétablissement si SygixOs n'est plus utilisable ou a été désinstallé (exige le débogage et un ordinateur, ou une réinitialisation).

### D12. Politique d'auto-démarrage du constructeur
Sur les firmwares TCL où l'autorisation de démarrage revient à `ignore` à chaque démarrage, le gestionnaire d'auto-démarrage lit une politique JSON exposée par le fournisseur de contenu du constructeur (`content://com.tcl.providers.config/AppBootPolicy`), dont les listes `autoStartWhiteList` et `assocStartWhiteList` exemptent des paquets (source publique, à confirmer par la tâche 1.8). Quand ce fournisseur existe, la configuration automatique, par le shell : lit la valeur ; si SygixOs manque dans l'une des deux listes, sauvegarde la valeur d'origine telle quelle dans le stockage privé de SygixOs (une sauvegarde par écriture, horodatée, la plus ancienne conservée), écrit la même valeur avec `fr.sygix.sygixos` ajouté à ces seules listes, sans toucher au reste ; puis relit et vérifie que le paquet figure dans les deux listes et que le reste est inchangé. Une valeur illisible, un JSON inattendu (listes absentes ou d'un autre type) ou une écriture refusée : aucune écriture, échec rapporté. Méthode d'écriture retenue par la tâche 1.8 : `content update` sur ce fournisseur quand il accepte la valeur complète, sinon un outil interne de l'APK de SygixOs lancé par `app_process` avec l'identité du shell (classe gardée par R8), qui appelle le fournisseur. Pas de retrait au rétablissement : le rétablissement ne concerne que l'accueil (décision 13), l'inscription permet seulement à SygixOs de démarrer comme l'autorisation de démarrage, qui reste aussi ; elle n'a plus d'effet une fois SygixOs désinstallé, et réécrire une seconde fois la politique du constructeur ajouterait un risque sans bénéfice. La sauvegarde reste consultable dans « Détails » et sert à la restauration manuelle documentée dans le README. Une mise à jour ou une synchronisation du constructeur peut écraser la politique : l'état est relu à chaque séquence et « Relancer la configuration » réinscrit SygixOs.

## Vérifications sur l'appareil

Résultats à consigner ici par les tâches 1.x (structure et comportements seulement : versions, noms de paquets système, résultats ; jamais de modèle exact, de numéro de série ni d'adresse).

| Vérification | Tâche | Résultat |
| --- | --- | --- |
| ADB local depuis une application : port réseau ouvert par le débogage USB, connexion de bouclage avec la bibliothèque retenue, dialogue d'autorisation et « Toujours autoriser », persistance après redémarrage, débogage désactivé puis réactivé | 1.1 | à consigner |
| Activités `HOME`, priorités, composants de l'accueil d'origine, état, rôle, paquets protégés | 1.2 | à consigner |
| `set-home-activity` seul puis désactivation : touche Home, appui long, démarrage à froid, sortie de veille, boutons de la télécommande, réglages rapides, choix du profil, guide, mise à jour système, relance après mise à jour intégrée (sujet 9), rétablissement | 1.3 | à consigner |
| Autorisation de démarrage du constructeur : nom de l'opération, accord, `write-settings`, persistance après redémarrage complet, liaison du service d'accessibilité, fin du démarrage reçue | 1.4 | à consigner |
| Rustines par le shell : affichage superposé, service d'accessibilité ajouté sans retirer les autres, connexion effective | 1.5 | à consigner |
| Écrans d'auto-démarrage du constructeur résolus et ouvrables depuis une application | 1.6 | à consigner |
| Délais mesurés : connexion, autorisation, commandes, réattribution du rôle | 1.7 | à consigner |
| Politique d'auto-démarrage du constructeur : présence du fournisseur, structure de la valeur, lecture et écriture par le shell (`content update` ou outil `app_process`), persistance après redémarrage complet, effet sur l'autorisation de démarrage et la liaison du service | 1.8 | à consigner |

## Risks / Trade-offs

- [Port ADB réseau ouvert tant que le débogage est actif] → conseil de désactivation après succès (D9) ; toute nouvelle clé exige une acceptation à l'écran ; README.
- [Clé de SygixOs autorisée de façon permanente : une version compromise de SygixOs aurait l'accès du shell] → opérations fixes validées côté client (D2), connexion seulement pendant une séquence demandée ; l'utilisateur peut révoquer les autorisations de débogage.
- [Autorisation de démarrage du constructeur réinitialisée à chaque démarrage sur certains firmwares] → inscription dans la politique d'auto-démarrage du constructeur (D12), vérifiée par la tâche 1.8.
- [Politique du constructeur écrasée par une mise à jour du firmware ou une synchronisation du constructeur] → état relu à chaque séquence, « Relancer la configuration » réinscrit SygixOs ; risque dit dans « Détails » et le README.
- [Écriture d'une politique système du constructeur] → seules les deux listes changent, valeur d'origine sauvegardée avant chaque écriture, relecture complète, aucune écriture sur une structure inattendue.
- [SygixOs désinstallé ou ses données effacées alors que l'accueil d'origine est désactivé : écran d'accueil de secours, vide] → la confirmation avertit de rétablir avant désinstallation ; README (ADB ou réinitialisation).
- [Fonctions perdues avec l'accueil d'origine : choix du profil, réglages rapides, guide] → dites dans la confirmation et « Détails » ; désactivation seulement si nécessaire (D5) ; rétablissement proposé.
- [Système ou mise à jour qui réactive l'accueil d'origine ou réattribue l'écran d'accueil] → état relu au retour au premier plan ; aucune réapplication automatique ; « Relancer la configuration ».
- [Débogage indisponible ou port non exposé sur un appareil] → voie automatique « Indisponible », voie « Par autorisations » toujours proposée.
- [Bibliothèque ADB peu maintenue] → surface réduite (connexion, authentification, `shell`), tâche 1.1, voie manuelle et procédure README valables.
- [Profils multiples] → opérations limitées à l'utilisateur courant de SygixOs ; README.

## Migration Plan

Aucune donnée existante migrée. Le drapeau « parcours fermé » de P5 est conservé : une installation qui l'a fermé ne le revoit pas automatiquement. Une installation où l'accueil d'origine a été désactivé par la procédure ADB affiche cet état dès le premier lancement de P7 ; le rétablissement depuis SygixOs y est possible (aucun détenteur précédent relevé : seuls les composants sont rétablis). Archivage après merge et validation sur la TV réelle.

## Ordre d'archivage

`up-next`, `jellyfin-tvprovider-only` et `p5-real-launcher` sont archivés sur `main`. P7 modifie `launcher-shell`, `settings`, `ui-testing` et `up-next` (références P9 à BetaSeries). `hero-shuffle-image-provider`, ouvert sur la même branche, touche d'autres exigences de `launcher-shell` ; il s'archive indépendamment, avant ou après P7.

## Décisions du propriétaire

Décisions du 2026-10-12, qui remplacent celles du 2026-10-11 de ce change :
1. But affiché : faire de SygixOs l'écran d'accueil. Plus de liste de services ni d'actions séparées par autorisation, plus de « mode avancé ».
2. Premier lancement : une modale explique les deux façons de faire et propose « Plus tard » ; les réglages permettent de relancer le parcours et montrent l'état des deux voies.
3. Voie automatique par ADB intégré à SygixOs (débogage activé, clé acceptée une fois), avec un guide en étapes simples ; Shizuku et Shevery ne sont pas la voie principale (association impossible sur TV sans ordinateur).
4. Opérations progressives : démarrage automatique du constructeur, écran d'accueil, vérification ; désactivation de l'accueil d'origine seulement si c'est encore nécessaire, avec ses conséquences et un rétablissement ; solutions de repli posées automatiquement quand la méthode principale n'est pas possible.
5. Voie sans débogage conservée, avec sa limite annoncée.
6. Confirmation en langage utilisateur et lien discret « Détails » qui liste ce qui est modifié.
7. Confirmations : désactivation avec « Annuler » focalisé, rétablissement avec « Rétablir » focalisé ; textes : décision 10.
8. Ensemble fermé des composants, arrêté après la tâche 1.2 (décision du 2026-10-11 maintenue).
9. Si le système réactive l'accueil d'origine, SygixOs affiche l'état relu sans réapplication ni rappel (maintenue).
10. Textes : ceux de la maquette « 8. Launcher principal » (écrans 8.1 à 8.21), repris mot pour mot dans les deltas, après vérification de chaque affirmation (2026-10-12) : les pertes à la désactivation sont formulées « selon la TV » (retours d'utilisateurs isolés), les libellés Android sont ceux des traductions françaises d'AOSP (« Autoriser le débogage USB ? », « Toujours autoriser sur cet ordinateur », « Autoriser », « Débogage USB », « Options pour les développeurs », « Superposer aux autres applications ») et restent à confirmer sur la TV (tâches 1.1 et 1.5).
11. Second moyen d'accès (gestionnaire compatible Shizuku) : hors P7 ; la passerelle interne reste prête pour un second moyen.
12. TV sans port ADB réseau : la voie automatique est « Indisponible sur cette TV » et renvoie à la voie « Par autorisations » ; pas d'appairage par code en P7.
13. « Rétablir » ne concerne que l'accueil : il réactive l'accueil Google TV et rend l'écran d'accueil précédent ; l'accessibilité et l'affichage superposé restent, révocables dans Android.
14. Politique d'auto-démarrage du constructeur réinitialisée : SygixOs s'ajoute aux listes `autoStartWhiteList` et `assocStartWhiteList`, après sauvegarde de la valeur d'origine, avec relecture ; risque d'écrasement par une mise à jour du constructeur documenté ; vérification sur appareil (tâche 1.8). Pas de retrait au rétablissement (D12).

## Questions ouvertes

Aucune.

## Maquettes

Penpot, fichier « SygixOs Maquette », page « TV · Launcher principal », rangée « 8. Launcher principal », écrans 8.1 à 8.21, chacun avec une note de comportement (8.18 à 8.20 : états « En partie », « Bloqué » et « À configurer » de la ligne des réglages ; 8.21 : panneau « Launcher principal » quand rien n'est configuré) ; composants de la page « Composants », section « Panneaux et dialogues ». La maquette fait foi pour le rendu, la spec pour le comportement. Les textes de la maquette ont été vérifiés et alignés sur la spec le 2026-10-12 (version Penpot « Textes vérifiés (Claude, 2026-10-12) »). Dimensions : 1920 × 1080 px, soit 2 px par dp.
