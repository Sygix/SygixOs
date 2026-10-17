# Spec Delta

« Sélecteurs de l'écran d'accueil » a été ajoutée par `p7-default-launcher`, qui doit être archivé avant ce change.

## ADDED Requirements

### Requirement: Couverture du parcours avec Tsuyu
Le parcours avec Tsuyu et la réparation automatique (`tsuyu-access`, `launcher-shell`) SHALL être couverts en JUnit et en tests Compose à la taille d'une TV (`w960dp-h540dp-xhdpi`), sans réseau, sans Tsuyu réel ni connexion ADB réelle. Des doubles contrôlés SHALL fournir la source des releases de Tsuyu, le téléchargement, l'installateur du système, la présence et la version de Tsuyu, l'API Shizuku, le service lancé par Tsuyu, le numéro de démarrage et le test du port.

#### Scenario: installation vérifiée
- **WHEN** le double de téléchargement rend successivement un fichier de mauvaise taille, de mauvaise empreinte, d'un autre paquet, signé par un autre certificat, puis un fichier conforme
- **THEN** les quatre premiers sont supprimés sans appel à l'installateur et affichent « tsuyu-install-error » avec la cause attendue ; seul le dernier est transmis à l'installateur

#### Scenario: parcours complet avec Tsuyu
- **WHEN** le test choisit « setup-choice-tsuyu », débogage désactivé et Tsuyu absent, puis simule le retour avec le débogage activé, l'installation réussie, Tsuyu démarré et l'autorisation accordée
- **THEN** « setup-debug », « tsuyu-install », « tsuyu-start », « tsuyu-authorize » puis « setup-progress » s'affichent dans cet ordre avec « setup-step-counter » de « Étape 1 sur 5 » à « Étape 5 sur 5 », et aucune connexion ADB de SygixOs n'est ouverte

#### Scenario: port injoignable avant le téléchargement
- **WHEN** le double du port déclare le port injoignable au retour du guide du débogage, sur le parcours avec Tsuyu
- **THEN** aucun téléchargement n'est demandé et la première page 8.2 s'affiche

#### Scenario: tout par OK
- **WHEN** le test, avec des doubles qui reproduisent la TV de référence (vérification négative de l'écran d'accueil), ne presse que OK sur chaque écran de SygixOs et simule l'acceptation des écrans d'Android et de Tsuyu
- **THEN** le dernier écran est « setup-fallback » avec « setup-fallback-repair », aucune désactivation n'a été envoyée et « confirm-disable » ne s'est jamais affiché

#### Scenario: autorisation refusée puis plus demandée
- **WHEN** le double de l'API Shizuku refuse l'autorisation, puis indique que Tsuyu ne présentera plus de demande
- **THEN** « tsuyu-authorize-retry » est focalisé après le premier refus, puis « tsuyu-authorize-open » remplace « tsuyu-authorize-retry » ; aucune opération n'est envoyée

#### Scenario: réparation automatique
- **WHEN** le double déclare Tsuyu « Actif », le service de la touche Home posé par SygixOs activé mais non connecté au terme de l'attente, puis le simule de nouveau au retour au premier plan suivant du même démarrage, puis après un nouveau démarrage
- **THEN** une seule réparation est envoyée par démarrage, dans l'ordre autorisation du constructeur puis retrait et remise du service, les autres services conservés, et elle est enregistrée pour « setup-details »

#### Scenario: réparation interdite
- **WHEN** le double déclare le service désactivé par l'utilisateur, ou l'accueil d'origine réactivé, ou Tsuyu « Arrêté »
- **THEN** aucune opération privilégiée n'est envoyée

#### Scenario: états de Tsuyu dans le panneau
- **WHEN** le test affiche « launcher-panel » avec Tsuyu absent et une release connue, puis absent sans release, puis installé arrêté, non autorisé, en version d'API 12, actif, puis le port injoignable
- **THEN** « launcher-panel-tsuyu-state » affiche « À installer », la ligne n'existe pas, puis « Arrêté », « À autoriser », « À mettre à jour », « Actif » avec « launcher-panel-tsuyu-dot », puis « Indisponible sur cette TV »

### Requirement: Sélecteurs de Tsuyu
Les écrans 9.4 à 9.9 SHALL exposer ces testTags : 9.4 à 9.6 « tsuyu-install » avec « -step-1 » à « -step-3 », « -status », « -skip », « -accept », « -retry » et « -error » ; 9.7 « tsuyu-start » avec « -step-1 » à « -step-3 », « -status », « -open » ; 9.8 et 9.9 « tsuyu-authorize » avec « tsuyu-authorize-step-1 », « -step-2 », « -status », « -continue », « -retry », « -open », « -back ».

#### Scenario: focus initial des écrans de Tsuyu
- **WHEN** le test affiche 9.4, 9.6, 9.7, 9.8 puis 9.9
- **THEN** le focus est sur « tsuyu-install-accept », « tsuyu-install-retry », « tsuyu-start-open », « tsuyu-authorize-continue » puis « tsuyu-authorize-retry », sans touche pressée ; chaque écran a « setup-mascot » et « setup-step-counter »

#### Scenario: navigation des écrans de Tsuyu
- **WHEN** le test presse gauche, droite, haut et bas sur 9.4 et 9.9
- **THEN** gauche et droite passent d'un bouton à l'autre sans boucle, haut et bas ne déplacent pas le focus, et aucun élément de l'accueil ne prend le focus

## MODIFIED Requirements

### Requirement: Sélecteurs de l'écran d'accueil
Les écrans 8.1 à 8.21 et 9.1 à 9.16 et la ligne des réglages SHALL exposer des testTags stables, utilisés par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) à la place du texte ou de la position : « setup-panel » (voile et panneau), « setup-mascot », « setup-step-counter » (repère « Étape X sur N »), « setup-title », « setup-description », « setup-hint » ; 8.1/8.2/9.1 : « setup-choice-tsuyu », « setup-choice-auto », « setup-choice-manual », « setup-later » ; 9.2/9.3/8.10 : « setup-debug », « setup-debug-step-1 », « setup-debug-step-2 », « setup-debug-open », « setup-debug-back » ; 8.4/8.11 : « setup-authorize », « setup-authorize-step-1 », « setup-authorize-step-2 », « setup-authorize-status », « setup-authorize-continue », « setup-authorize-retry », « setup-authorize-back » ; erreurs : « setup-error » (encadré, avec « setup-error-title » et « setup-error-detail ») ; 9.10 : « setup-progress » ; 8.6/9.13 : « setup-done », « setup-done-tip » (conseil), « setup-done-repair », « setup-done-details », « setup-done-finish » ; 9.11 : « setup-more », « setup-more-keep », « setup-more-disable » ; 8.9/9.12 : « setup-fallback », « setup-fallback-home », « setup-fallback-overlay », « setup-fallback-boot », « setup-fallback-repair », « setup-fallback-tip », « setup-fallback-details », « setup-fallback-finish » ; 8.12 : « setup-failed », « setup-failed-close », « setup-failed-retry » ; lignes d'étapes de la séquence : « setup-step-boot », « setup-step-home », « setup-step-verify », « setup-step-stock » ; 8.13 : « setup-manual », « setup-manual-home », « setup-manual-boot », « setup-manual-overlay », « setup-manual-vendor-boot », « setup-manual-tip », « setup-manual-finish » ; 8.14/9.16 : « setup-details », « setup-details-item-<réglage> », « setup-details-close » ; 8.16/9.14/9.15 : « launcher-panel », « launcher-panel-auto », « launcher-panel-tsuyu », « launcher-panel-manual », « launcher-panel-relaunch », « launcher-panel-replace », « launcher-panel-restore » ; dialogues 8.8 et 8.17 : « confirm-disable » et « confirm-restore », chacun avec « <dialogue>-title », « <dialogue>-text », « <dialogue>-accept », « <dialogue>-cancel » ; réglages : « setting-launcher-group », « setting-launcher-guide » ; pour toute ligne « <ligne> » ci-dessus, « <ligne>-title », « <ligne>-detail », « <ligne>-state » et « <ligne>-dot » (pastille, absente quand l'état n'en a pas). Les anciens testTags « onboarding-* », « setting-home-role », « setting-boot-start », « setting-accessibility-home », « launcher-overlay-* », « setup-debug-status » et « setup-more-home » n'existent plus. Les écrans propres à Tsuyu suivent « Sélecteurs de Tsuyu ». Les tests SHALL vérifier les écrans, l'ordre, le focus initial, la navigation D-pad, les états, la présence de « setup-mascot » (absente des dialogues et de « setup-details »), la présence de « setup-step-counter » (absente de la première page, de « setup-manual », de « setup-details », des dialogues, du panneau et des écrans ouverts depuis le panneau) et la fermeture définitive du parcours définis par `launcher-shell`, et la synthèse définie par « Réglages du launcher système » de `settings`.

#### Scenario: ordre et focus initial
- **WHEN** le test compose l'accueil avec le parcours à afficher, rien n'étant fait, sans Tsuyu disponible, puis avec Tsuyu disponible
- **THEN** « setup-panel » est affiché par-dessus « zone-hero » ; « setup-mascot » est à gauche de « setup-title » ; sans Tsuyu, « setup-choice-auto » puis « setup-choice-manual » sont affichés de haut en bas avec les états « À configurer » et « À faire » et « setup-choice-auto » a le focus ; avec Tsuyu, « setup-choice-tsuyu », « setup-choice-auto » puis « setup-choice-manual » sont affichés et « setup-choice-tsuyu » a le focus ; « setup-hint » est à gauche de « setup-later »

#### Scenario: navigation
- **WHEN** le test presse bas quatre fois, haut quatre fois, puis gauche et droite sur 9.1, puis gauche et droite sur 8.4, 8.8, 8.11 et 8.17, puis haut et bas sur 9.11
- **THEN** sur 9.1 le focus suit « setup-choice-tsuyu », « setup-choice-auto », « setup-choice-manual », « setup-later » sans boucle et gauche et droite ne changent rien ; sur les écrans à deux boutons, gauche et droite passent d'un bouton à l'autre sans boucle ; sur 9.11, haut et bas passent de « setup-more-keep » à « setup-more-disable » sans boucle ; « zone-hero », « zone-dock », « hero-capsule » et « settings-gear » ne prennent jamais le focus tant que « setup-panel » est affiché

#### Scenario: guide du débogage en deux temps
- **WHEN** le double déclare les options pour les développeurs masquées, puis affichées, puis le débogage activé
- **THEN** « setup-debug-step-1-state » et « setup-debug-step-2-state » affichent « À faire » et « setup-debug-open » ouvre « À propos » ; puis « setup-debug-step-1-state » affiche « Fait » et « setup-debug-open » ouvre les options pour les développeurs ; puis l'écran suivant de la façon choisie s'affiche sans touche pressée

#### Scenario: mascotte et repère
- **WHEN** le test affiche chacun des écrans 8.1 à 8.17 et 9.1 à 9.16
- **THEN** « setup-mascot » existe sur les panneaux du parcours et n'existe pas dans « confirm-disable », « confirm-restore » ni « setup-details » ; « setup-step-counter » existe sur les écrans d'étape de la configuration automatique ouverts depuis la première page et n'existe pas ailleurs

#### Scenario: page Par autorisations
- **WHEN** le test ouvre « setup-manual » sur un appareil avec gestionnaire d'auto-démarrage du constructeur sans écran retenu, l'option de démarrage activée et la superposition manquante
- **THEN** « setup-manual-home », « setup-manual-boot », « setup-manual-overlay », « setup-manual-vendor-boot » puis « setup-manual-tip » et « setup-manual-finish » sont affichés de haut en bas ; « setup-manual-vendor-boot-state » affiche « Indisponible » ; « setup-manual-tip » n'est pas focalisable ; la première ligne qui n'est pas « Fait » a le focus

#### Scenario: états affichés
- **WHEN** le test compose une ligne dans chacun des états qui la concernent, avec et sans le focus
- **THEN** « <ligne>-state » affiche le libellé ; « <ligne>-dot » existe pour « Fait », « Active », « Actif », « Installé » (couleur `SwitchOn`) et « En cours… », « En attente… », « Téléchargement… », « Vérification… », « Installation… » (couleur `Badge`), et n'existe pas pour les autres ; « Échec » est rendu en couleur d'erreur ; en « Indisponible » et « Indisponible sur cette TV », la ligne prend le focus et OK n'ouvre rien

#### Scenario: ordre des contrôles de la catégorie
- **WHEN** le test ouvre la catégorie « Écran d'accueil » des réglages
- **THEN** « setting-upnext-visible », « setting-upnext-position », « setting-launcher-group » et « setting-launcher-guide » sont affichés de haut en bas ; « setting-upnext-visible » a le focus ; « setting-launcher-group » n'est pas focalisable ; aucun des anciens testTags n'existe

#### Scenario: panneau depuis les réglages
- **WHEN** le test presse OK sur « setting-launcher-guide », puis Retour
- **THEN** « launcher-panel » s'affiche par-dessus « settings-home-screen » avec « launcher-panel-relaunch » focalisé ; après Retour, il n'existe plus, les réglages sont toujours affichés, « setting-launcher-guide » a le focus et le drapeau du parcours automatique n'est pas modifié

#### Scenario: synthèse de la ligne des réglages
- **WHEN** le test affiche « Écran d'accueil » avec rien de fait, puis la voie automatique active, puis la voie par autorisations en partie, puis bloquée
- **THEN** « setting-launcher-guide-state » et « setting-launcher-guide-detail » affichent « À configurer » et « Automatique : à configurer · Par autorisations : à faire », « Actif » (avec « setting-launcher-guide-dot ») et « Automatique : actif · Par autorisations : non utilisée », « En partie » et « Automatique : à configurer · Par autorisations : en partie », « Bloqué » et « Automatique : à configurer · Par autorisations : bloquée »
