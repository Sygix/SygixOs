# Tasks

## 1. Vérifications sur la TV de référence avant implémentation

Chaque résultat est consigné dans `design.md`, section « Vérifications sur l'appareil ». Une première passe sur émulateur y est consignée (2026-10-17) ; elle ne remplace pas la TV. On y note les versions, les noms de paquets et les résultats, sans donnée personnelle, modèle exact, numéro de série ni adresse. Les réglages touchés par les essais sont rétablis à la fin.

- [ ] 1.1 Guide du débogage, à partir d'options pour les développeurs masquées et du débogage désactivé :
  - lire `DEVELOPMENT_SETTINGS_ENABLED` et `ADB_ENABLED` depuis un APK de vérification ;
  - ouvrir `ACTION_DEVICE_INFO_SETTINGS` puis `ACTION_APPLICATION_DEVELOPMENT_SETTINGS` ;
  - vérifier que Retour depuis chacun ramène dans l'APK ;
  - relever les libellés réels cités par 9.2 et 9.3 (« À propos », ligne du build, « Options pour les développeurs », « Débogage USB »).
  Terminé quand les résultats et les éventuelles corrections de texte sont reportés dans `design.md` (D7), dans la spec et dans la maquette.
- [ ] 1.2 Tsuyu, sur la dernière release de Tsuyu :
  - libellés et enchaînement de « Démarrer », de la fenêtre d'autorisation du débogage et de « Démarrage au redémarrage » ;
  - ouverture de sa `MainActivity` sans `FLAG_ACTIVITY_NEW_TASK` depuis un APK de vérification, et retour par Retour ;
  - réception du binder par un client qui déclare `API_V23` ;
  - fenêtre d'accord (focus de départ, retour à l'appelant) ;
  - comportement après un refus (`shouldShowRequestPermissionRationale`) ;
  - lancement d'un service utilisateur à l'identité du shell ;
  - état après la désactivation du débogage puis après un redémarrage complet.
  Terminé quand chaque point de `design.md` (D2, D4, D5) est confirmé ou corrigé.
- [ ] 1.3 Liaison du service d'accessibilité :
  - avec un autre service activé, écrire par le shell la liste sans le service de l'APK puis avec lui ;
  - vérifier que le service est relié (`dumpsys accessibility`) et que l'autre service reste relié ;
  - recommencer dans l'état « Bloqué » obtenu après un redémarrage avec `AUTO_START` à `ignore`.
  Terminé quand l'effet de la réparation (D9) est confirmé, ou que la spec est corrigée.
- [ ] 1.4 Installation depuis une application :
  - installation de `tsuyu.apk` par une session d'installation d'un APK de vérification, sans puis avec l'autorisation « applis inconnues » ;
  - relever les écrans d'Android et leurs libellés (étapes 2 et 3 de 9.4) ;
  - relever le statut rendu en cas de refus.
  Terminé quand les textes de 9.4 sont confirmés ou corrigés dans la spec et la maquette.

## 2. Maquettes et décisions

- [x] 2.1 Le propriétaire valide les décisions de `design.md` et la rangée 9 de la maquette (9.1 à 9.16) ; `openspec validate tsuyu-home-setup --strict` passe.

## 3. Disponibilité et installation de Tsuyu

- [x] 3.1 Paramétrer `GitHubReleaseSource` par dépôt, et la sélection de version de `UpdateRepository` par asset et réglage des préversions. Conserver le dernier résultat de Tsuyu dans DataStore. Vérifié par des tests JUnit de « Disponibilité de Tsuyu » (release trouvée, aucune release éligible, préversions, échec réseau qui garde le dernier résultat, Tsuyu installé sans réseau) et par les tests existants de `self-update`, inchangés et verts.
- [x] 3.2 Paramétrer `ApkInspector` et `UpdateInstaller` par le paquet attendu, avec un certificat de référence égal à celui de SygixOs installé. Vérifié par des tests JUnit qui refusent chaque fichier non conforme sans appeler l'installateur, nettoient les fichiers temporaires et distinguent installé, refusé et échec.

## 4. Accès par Tsuyu

- [x] 4.1 Ajouter la dépendance `dev.rikka.shizuku:api` et `:provider` (13.1.5) dans `gradle/libs.versions.toml`, ainsi que le manifeste de D11. Vérifié par `./gradlew assembleRelease` et la lecture du manifeste fusionné.
- [x] 4.2 Introduire `SwitchingAccess` avec deux moyens, l'ADB intégré et `TsuyuAccess` avec son service utilisateur AIDL (`PrivilegedOpsService`) qui n'exécute que les formes de commande de `PrivilegedCommands`, et partager la construction des commandes. Vérifié par :
  - des tests JUnit qui comparent les arguments exacts des deux implémentations pour chaque opération ;
  - le refus des cibles hors de l'ensemble fermé ;
  - le lien du service ouvert au début de la séquence et fermé à sa fin ;
  - la perte du binder en cours de séquence.
- [x] 4.3 Ajouter `TsuyuState` et le choix du moyen d'accès au début de chaque séquence. Vérifié par des tests JUnit de « États de Tsuyu » et du scénario « Tsuyu actif » de `launcher-shell`.

## 5. Parcours

- [x] 5.1 Contrôleur du parcours : liste des étapes par façon, étapes déjà faites sautées, repère, passage « Sans Tsuyu », test du port avant le téléchargement. Vérifié par un test JUnit par scénario de « Parcours de l'écran d'accueil » et de « Accès par le débogage ».
- [x] 5.2 Écrans 9.1 à 9.3 (première page à trois lignes, guide du débogage en deux temps) avec chaînes françaises et testTags. Vérifié par des tests Compose à la taille d'une TV pour l'ordre, le focus, la navigation, les états et le repère, selon « Sélecteurs de l'écran d'accueil ».
- [x] 5.3 Écrans 9.4 à 9.9 (installer, démarrer, autoriser Tsuyu, et leurs erreurs). Vérifié par des tests Compose de « Sélecteurs de Tsuyu » et des tests JUnit de « Couverture du parcours avec Tsuyu » (installation vérifiée, parcours complet, port injoignable, refus).
- [x] 5.4 Écrans 9.10 à 9.13 : repère sur la séquence, « Un dernier choix » en lignes avec « Garder » focalisé, variantes avec Tsuyu de « SygixOs garde la main » et « SygixOs est votre écran d'accueil ». Vérifié par des tests Compose et par le scénario « tout par OK », qui échoue avec la sélection de départ de P7.

## 6. Panneau, désactivation, rétablissement et détails

- [x] 6.1 Panneau « Launcher principal » : ligne « Tsuyu » et ses actions, « Remplacer complètement l'accueil Google TV », texte de « Relancer la configuration » selon Tsuyu. Vérifié par des tests Compose de 9.14 et 9.15 et le scénario « états de Tsuyu dans le panneau ».
- [x] 6.2 Désactivation depuis le panneau (succès, échec annulé avec encadré) et rétablissement par Tsuyu sans écrans du débogage. Vérifié par des tests JUnit et Compose des scénarios « remplacement depuis le panneau », « désactivation partielle annulée » et « rétablissement par Tsuyu ».
- [x] 6.3 « Détails » avec les lignes « Tsuyu » et « Réparations automatiques ». Vérifié par un test Compose de 9.16.

## 7. Réparation automatique

- [x] 7.1 Contrôleur de réparation (D9) : symptômes, conditions, ordre des actions, une tentative par réglage et par démarrage, enregistrement horodaté, aucune action sans symptôme. Vérifié par des tests JUnit de chaque scénario de « Réparation automatique » et « Limites de la réparation automatique », dont un test qui échoue si la réparation se déclenche sur un service désactivé par l'utilisateur.

## 8. Documentation

- [x] 8.1 README : les trois façons, l'installation de Tsuyu depuis la TV et sa vérification, le débogage qui reste activé avec Tsuyu, la réparation automatique et ses limites, le passage sans Tsuyu, le rétablissement. AGENTS.md et `openspec/config.yaml` : P7 et sa suite marquées livrées. Vérifié par relecture du diff contre les specs, et par l'absence de détail d'environnement (contrôle d'AGENTS.md, « Repo public »).

## 9. Intégration et livraison

- [x] 9.1 `./gradlew test` et `./gradlew assembleRelease` verts ; `openspec validate --all --strict` sans erreur pour ce change.
- [ ] 9.2 Validation sur la TV réelle avec l'APK release et une release de Tsuyu :
  - parcours avec Tsuyu de bout en bout, en ne pressant que OK dans SygixOs ;
  - parcours sans Tsuyu ;
  - erreurs : pas de réseau, installation refusée, Tsuyu non démarré, accord refusé ;
  - redémarrage complet après chaque cas (touche Home, démarrage à froid) ;
  - réparation après un service « Bloqué » provoqué ;
  - remplacement et rétablissement depuis le panneau sans débogage réactivé ;
  - Tsuyu arrêté, puis désinstallé ;
  - rendu comparé à la rangée 9.
  Terminé quand les résultats sont consignés dans `design.md`, sans donnée personnelle.

## Workflow follow-up

- Archiver `p7-default-launcher` d'abord (après sa tâche 6.2), puis ce change après son merge et sa validation sur la TV : `openspec archive tsuyu-home-setup`, puis `openspec validate --all --strict`.
- Porter dans le backlog de Tsuyu, si le propriétaire le souhaite, les « Suggestions pour Tsuyu » de `design.md`.
