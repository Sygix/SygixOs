# Tasks

## 1. Vérifications sur la TV de référence avant implémentation

Les tâches 1.1 à 1.7 précèdent toute implémentation. Chaque résultat est consigné dans `design.md`, section « Vérifications sur l'appareil » : versions, noms de paquets système, résultats, sans donnée personnelle, modèle exact, numéro de série ni adresse. Les autorisations, composants et réglages touchés par les essais sont rétablis à leur état initial par une action explicite du propriétaire. Ces cases restent ouvertes jusqu'à une preuve sur appareil ; les tests unitaires ne les remplacent pas. Une seconde Google TV d'une autre marque est vérifiée si le propriétaire en dispose.

- [ ] 1.1 ADB local depuis une application : avec un APK de vérification utilisant `dev.mobile:dadb`, vérifier que l'activation du débogage USB ouvre le port ADB réseau, que la connexion à l'adresse de bouclage aboutit, que le dialogue système d'autorisation s'affiche et que « Toujours autoriser » persiste après un redémarrage complet ; relever le comportement débogage désactivé, port fermé, refus, puis débogage désactivé et réactivé (clé toujours acceptée). Terminé quand les résultats et la bibliothèque retenue (ou son remplacement justifié) sont dans `design.md`, D1.
- [ ] 1.2 Composants de l'accueil d'origine : relever les activités `HOME` déclarées et leurs priorités (`cmd package query-activities --brief` sur l'intent `MAIN` + `HOME`, noms de paquets seulement) et, pour `com.google.android.tungsten.setupwraith`, `com.google.android.apps.tv.launcherx`, `com.google.android.tvlauncher` et ceux relevés, la présence pour l'utilisateur courant, l'état (`enabled=` dans `dumpsys package <paquet>`), le rôle et la protection. Terminé quand l'ensemble fermé de D4 est arrêté dans `design.md`.
- [ ] 1.3 Écran d'accueil : relever `cmd role get-role-holders android.app.role.HOME` et `cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME` avant et après `cmd package set-home-activity fr.sygix.sygixos/.ui.MainActivity` seul, puis avant et après la désactivation de l'ensemble (ordre nécessaire), et vérifier à chaque étape : touche Home, appui long, démarrage à froid, sortie de veille, boutons de la télécommande, panneau de réglages rapides, choix du profil, guide des programmes, configuration après une mise à jour système si l'occasion se présente, relance après une mise à jour intégrée (sujet 9 de `openspec/backlog.md`) ; puis rétablissement (`pm default-state` et `set-home-activity` vers le détenteur relevé). Terminé quand chaque cas a son résultat dans `design.md` et que D5 et D6 sont confirmés ou corrigés.
- [ ] 1.4 Autorisation de démarrage du constructeur : relever le nom de l'opération présente (`AUTO_START`, `APP_AUTO_START`), sa valeur initiale, l'accorder pour SygixOs, `cmd appops write-settings`, redémarrer complètement et relever la valeur, la liaison du service d'accessibilité (`dumpsys accessibility`), la réception de la fin du démarrage et l'ouverture à l'allumage ; noter si la valeur est réinitialisée au démarrage. Terminé quand les résultats sont dans `design.md` et le nom de l'opération fixé dans D2.
- [ ] 1.5 Solutions de repli par le shell : `appops set fr.sygix.sygixos SYSTEM_ALERT_WINDOW allow` vérifié par `Settings.canDrawOverlays` ; ajout du service d'accessibilité de SygixOs à `enabled_accessibility_services` sans retirer les autres services, `accessibility_enabled`, puis connexion effective du service (y compris après un redémarrage complet avec l'autorisation de 1.4). Terminé quand les résultats sont dans `design.md`.
- [ ] 1.6 Écrans d'auto-démarrage du constructeur : vérifier par `cmd package query-activities` puis par une ouverture depuis l'APK de vérification les actions candidates de D8, et relever leur effet. Terminé quand la liste retenue (éventuellement vide) est dans `design.md`, D8.
- [ ] 1.7 Délais : mesurer connexion, autorisation, durée de chaque commande et réattribution de l'écran d'accueil sur la TV de référence. Terminé quand les délais de D1 et D5 sont confirmés ou corrigés dans `design.md`.

## 2. Décisions et maquettes

- [x] 2.1 Vérifier que les quatre deltas MODIFIED conservent les scénarios des specs principales `launcher-shell`, `settings`, `ui-testing` et `up-next` qui restent valables ; `openspec validate p7-default-launcher --strict` passe.
- [ ] 2.2 Maquettes Penpot (rangée « 8. Launcher principal » de la page « TV », composants de la section « Panneaux et dialogues ») : page de choix (premier lancement et depuis les réglages), configuration automatique (avant, en cours, terminée, repli, échec, rétablissement), guide du débogage, page « Par autorisations » avec ses sous-étapes, confirmations, « Détails », conseil sur le débogage, ligne des réglages dans ses états ; validation du propriétaire puis valeurs finales reportées dans `design.md` (D7) et textes définitifs (décision 10, « Questions ouvertes », 5).

## 3. Socle d'accès

- [ ] 3.1 Dépendance `dev.mobile:dadb` (version vérifiée en 1.1) par `gradle/libs.versions.toml`, `<queries>` pour les composants de l'ensemble fermé et les écrans du constructeur retenus ; vérifié par `./gradlew assembleRelease` et le manifeste fusionné.
- [ ] 3.2 `AdbAccessGateway` (D1, D3) : clé générée puis conservée dans le stockage privé, états de l'accès, connexion de bouclage seulement, délais, fermeture ; vérifié par des tests JUnit de chaque état et transition (débogage désactivé, port fermé, attente, refus, délai, prêt, perte de connexion) avec un double du transport.
- [ ] 3.3 `PrivilegedShell` (D2) : opérations typées à arguments fixes, refus des cibles hors du paquet SygixOs et de l'ensemble fermé, relecture après chaque opération, liste d'accessibilité préservée ; vérifié par des tests JUnit qui contrôlent les commandes exactes envoyées au double, les refus et l'absence de succès sans relecture.
- [ ] 3.4 `HomeScreenStateReader` (D3) : écran d'accueil effectif, composants (actif, désactivé par l'utilisateur, hors de portée, absent), configuration de l'appareil, service d'accessibilité activé et connecté, affichage superposé, avec `Result` ; vérifié par des tests JUnit, dont erreur de lecture distincte de l'absence et résolution ambiguë.

## 4. Domaine et interface

- [ ] 4.1 `DefaultLauncherController` (D5, D6) : séquence de configuration, étapes sans objet, proposition de désactivation, repli, échecs indépendants, délais, perte de l'accès, garde-fous, désactivation et annulation, rétablissement avec et sans détenteur précédent, persistance du détenteur précédent et des modifications, relecture au retour au premier plan, séquence qui continue à la fermeture du parcours ; vérifié par un test JUnit pour chaque scénario de `launcher-shell` concerné.
- [ ] 4.2 Parcours : page de choix, pages dans le même panneau, retour au choix, bouton « Plus tard » ou « Fermer », page « Par autorisations » avec la sous-étape 2c, drapeau de fermeture conservé ; vérifié par des tests Compose à taille TV de chaque scénario de « Parcours guidé du launcher » et de « Sélecteurs du launcher système ».
- [ ] 4.3 Page « Configuration automatique », guide du débogage, confirmations, « Détails » et conseil sur le débogage (maquettes 2.2) ; vérifié par des tests Compose à taille TV (focus initial, navigation, Retour, états, textes) et la comparaison visuelle aux maquettes.
- [ ] 4.4 Ligne « Configurer SygixOs comme launcher principal » et synthèse des deux voies ; vérifié par des tests JUnit de la synthèse et Compose de la catégorie.
- [ ] 4.5 Chaînes françaises en ressources pour tous les textes de la spec, testTags de « Sélecteurs du launcher système » ; vérifié par `./gradlew test` et l'absence de texte en dur dans les composables.

## 5. Documentation

- [ ] 5.1 README : les deux voies, activation du débogage et autorisation de SygixOs, ce que fait la configuration automatique, fonctions perdues et rétablissement, conseil de désactiver le débogage et réactivation pour rétablir, rétablir avant désinstallation, profils multiples, procédure ADB manuelle conservée comme repli. Vérifié par relecture du diff contre le comportement spécifié.

## 6. Intégration et livraison

- [ ] 6.1 `./gradlew test` et `./gradlew assembleRelease` verts ; terminé quand les tests passent et que l'APK release est construit.
- [ ] 6.2 Validation sur la TV réelle avec l'APK release : débogage désactivé, port fermé, refus puis autorisation ; configuration sans désactivation, avec désactivation, désactivation refusée (repli), échec et nouvelle tentative ; redémarrage complet après chaque cas (touche Home, démarrage à froid, sortie de veille) ; rétablissement ; débogage désactivé après coup puis réactivé pour rétablir ; page « Par autorisations » et sous-étape 2c ; rendu comparé aux maquettes 2.2 ; résultats consignés dans `design.md`, sans donnée personnelle.

## Workflow follow-up

- `p5-real-launcher` et `up-next` sont archivés sur `main`. Avant d'archiver P7, vérifier ses quatre deltas MODIFIED contre les specs principales `launcher-shell`, `settings`, `ui-testing` et `up-next`.
- Après merge et validation sur la TV : `openspec archive p7-default-launcher`, puis `openspec validate --all --strict`.
- Si la tâche 1.3 montre que l'écran d'accueil effectif résout la relance après mise à jour (sujet 9), mettre à jour le sujet 9 de `openspec/backlog.md` et, par un change dédié, l'exigence « Redémarrage après la mise à jour » de `self-update`.
