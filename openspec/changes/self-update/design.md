# Design

## Context
Voir `proposal.md` pour la motivation. État de départ (`main`, plus `ui-tvos-polish` pour les réglages et la capsule) :
- **Versions** : `app/build.gradle.kts` dérive `versionName` (tag sans `v`) et `versionCode` du tag fourni par la CI (`SYGIXOS_VERSION`) : major·10⁸ + minor·10⁵ + patch·10² + suffixe (alpha.N → N, beta.N → 30 + N, rc.N → 60 + N, finale → 99), avec major ≤ 20, minor et patch ≤ 999, N ≤ 29 ; regex `(\d{1,2})\.(\d{1,3})\.(\d{1,3})(?:-(alpha|beta|rc)\.?(\d{1,2}))?`. Exemple : `v0.0.1-rc.4` → 164. Hors CI : `versionName` « 0.1.0 », `versionCode` 1.
- **Publication** : `.github/workflows/release.yml` a deux jobs. `build` construit et signe l'APK avec la clé de release (secrets), vérifie la signature (`apksigner verify`) et le passe en artefact. `publish` crée la release **en brouillon** (`gh release create --draft --verify-tag --generate-notes`, préversion si le tag contient `-`), téléverse `app-release.apk` (`gh release upload --clobber`), puis la **publie** (`gh release edit --draft=false`) ; une release déjà publiée n'est jamais retouchée. Sans authentification, l'API ne voit pas les brouillons : une release n'y apparaît qu'une fois son APK complètement téléversé. Une build locale sans variables de signature est signée avec la clé de debug.
- **Réseau** : permission `INTERNET` déclarée ; `network_security_config.xml` autorise le HTTP en clair pour toute l'app (posters servis en HTTP sur le réseau local). Aucun client HTTP n'est déclaré : Coil 2 embarque OkHttp de façon transitive, media3 utilise `HttpURLConnection`.
- **Manifeste** : `MainActivity` (`singleTask`) a un seul filtre `MAIN` + `LEANBACK_LAUNCHER` + `LAUNCHER` ; pas de `CATEGORY_HOME`.
- **Réglages** : `SettingsViewModel` lit la version installée ; `AboutContent` affiche « SygixOs », la version, une phrase de licence, puis la liste des licences (premier élément focusable : la première licence), avec la pilule de focus de `ui-tvos-polish`. `AppleSwitch` existe déjà.
- **Persistance** : un DataStore Preferences unique (`LauncherPrefs`), conteneur applicatif dans `SygixOsApp`.
- **Capsule** : `HeroCapsule` dans `HomeScreen.kt` (`hero-capsule`, `settings-gear`), introduite par `ui-tvos-polish`.

### Source : API REST des releases GitHub (vérifiée le 2026-10-03, structure seulement)
`GET https://api.github.com/repos/Sygix/SygixOs/releases?per_page=100` pour une vérification, `GET https://api.github.com/repos/Sygix/SygixOs/releases/tags/<tag>` pour la relecture avant téléchargement. Champs utilisés, présents sur les trois releases existantes (`v0.0.1-rc.1`, `rc.2`, `rc.4`, toutes préversions) :

| Champ | Type | Usage |
| --- | --- | --- |
| `tag_name` | chaîne | version (`v0.0.1-rc.4`) |
| `draft` | booléen | brouillons ignorés (de toute façon invisibles sans authentification) |
| `prerelease` | booléen | préversion si vrai, en plus du suffixe du tag |
| `html_url` | chaîne | page de la release (`https://github.com/Sygix/SygixOs/releases/tag/<tag>`), cible du code QR des notes de version |
| `assets[].name` | chaîne | `app-release.apk` exactement |
| `assets[].state` | chaîne | `uploaded` exigé |
| `assets[].size` | entier | taille attendue (environ 3,5 Mo aujourd'hui) |
| `assets[].digest` | chaîne ou `null` | `sha256:<64 hex>` ; présent sur les trois assets ; la doc GitHub le déclare « string or null » : `null` ou autre algorithme → release non éligible |
| `assets[].browser_download_url` | chaîne | `https://github.com/Sygix/SygixOs/releases/download/<tag>/app-release.apk`, redirigée en HTTPS vers `release-assets.githubusercontent.com` |

Ce qui manque et comment on s'en passe : aucune signature détachée ni attestation → l'authenticité repose sur le certificat de signature de l'APK (D6) ; l'empreinte vient du même canal que l'APK et ne protège que contre la corruption, la troncature et la substitution du fichier local. Le texte des notes (`body`) n'est pas utilisé : le code QR mène à la page de la release. Une release pèse environ 4 Kio dans la réponse : 100 releases ≈ 400 Kio au pire.

Le dépôt est privé à la date de ce change : sans jeton, l'API répond 404. L'implémentation n'en dépend pas (tests contre un faux serveur) ; seule la validation sur la TV contre les vraies releases attend le passage en public (`proposal.md`, tâches 9.x).

### Limite de requêtes
Sans authentification : 60 requêtes par heure et par adresse IP publique (partagées par les appareils d'un même foyer). D'après la doc GitHub, une réponse `304` à une requête conditionnelle n'est exemptée de la limite que pour une requête authentifiée : `ETag` / `If-None-Match` n'économiserait que la bande passante. Les téléchargements d'assets (`browser_download_url`) ne passent pas par l'API REST et ne décomptent pas cette limite.

## Goals / Non-Goals

**Goals:**
- Une requête par vérification, une relecture et un téléchargement par mise à jour, aucune requête sans besoin ni de vérification automatique avant l'heure de réessai (décision de Sygix : une vérification ou une mise à jour demandée à la main envoie toujours sa requête).
- Aucun octet installé sans vérification complète (taille, empreinte, paquet, version, certificat, empreinte de la copie).
- Logique pure testable en JUnit ; Android (réseau, `PackageManager`, `PackageInstaller`, premier plan) derrière des interfaces.
- Jamais d'écran qui surgit au-dessus d'une autre app.

**Non-Goals:**
- Tâches planifiées (WorkManager) : la vérification automatique n'a lieu qu'au démarrage et au retour au premier plan.
- Service de premier plan pour le téléchargement en arrière-plan (voir Risks).
- Prise en charge d'une rotation de la clé de signature (voir Risks).

## Decisions

### D1. Requêtes à l'API
Vérification : `GET /repos/Sygix/SygixOs/releases?per_page=100`. Relecture avant téléchargement : `GET /repos/Sygix/SygixOs/releases/tags/<tag>` (404 → version retirée). En-têtes : `Accept: application/vnd.github+json`, `X-GitHub-Api-Version: 2022-11-28`, `User-Agent: SygixOs/<versionName>` (exigé par GitHub ; aucune donnée personnelle). Délais : connexion 10 s, lecture 15 s, total 30 s. Le `User-Agent` est lu paresseusement, hors du thread principal.
Alternatives écartées : `/releases/latest` (exclut les préversions, choisit par date) ; plusieurs pages (on cherche la plus haute version, toujours parmi les plus récentes) ; `ETag` (aucun gain sur la limite sans authentification).
Correspondance des erreurs (`domain/UpdateError`) : absence de réseau / `UnknownHostException` / autre `IOException` → `NoNetwork` ; `SSLHandshakeException` ou `SSLPeerUnverifiedException` (négociation TLS ou certificat refusé, souvent une date de TV fausse) → `SecureConnection`, toute autre `SSLException` (lecture coupée quand le Wi-Fi tombe) suit le chemin `IOException` ; `SocketTimeoutException` ou délai total dépassé → `Timeout` ; 403 ou 429 avec `x-ratelimit-remaining: 0` → `RateLimited(retryAt)` (`retry-after` en secondes, sinon `x-ratelimit-reset` en secondes epoch, sinon une heure plus tard, décision de Sygix) ; 404 et autres codes ≥ 400 → `Unavailable(code)` ; JSON invalide → `Unreadable`. La source renvoie un `Result`.

### D2. Client HTTP : `HttpsURLConnection` de la plateforme
Trois GET seulement (liste, relecture, APK en flux) : la plateforme suffit. Déclarer OkHttp reviendrait à figer une dépendance aujourd'hui transitive de Coil 2. JSON : `org.json` de la plateforme (réel sous Robolectric).
- `data/HttpTransport` (interface) : `get(url, headers, maxBytes, sink)` → statut, en-têtes utiles, URL finale ; implémentation `HttpsUrlTransport` (`instanceFollowRedirects = true` : `HttpURLConnection` ne suit jamais une redirection qui change de protocole ; aucun `CookieHandler` ; lecture interrompue au-delà de `maxBytes`).
- `domain/UpdateUrlPolicy` : refuse toute URL non `https` avant l'appel, et l'URL finale renvoyée par le transport si elle n'est pas `https` ; valide aussi `html_url` (HTTPS, hôte `github.com`) pour le code QR.
- `network_security_config.xml` : `base-config` inchangé (posters du réseau local), plus un `domain-config` `cleartextTrafficPermitted="false"` pour `github.com`, `githubusercontent.com` et `api.github.com` (`includeSubdomains="true"`) : la plateforme refuse le clair vers ces domaines même si le code se trompait.
- Tests du transport : serveur `com.sun.net.httpserver.HttpServer` du JDK, en boucle locale, dans des tests JUnit simples : aucune nouvelle dépendance de test. La règle HTTPS étant hors du transport, celui-ci se teste en HTTP local. Alternative écartée : MockWebServer (dépendance de test inutile ici).

### D3. Versions : `domain/ReleaseVersion` et `domain/UpdateSelector`
Même regex et même formule que `versionCodeOf` de `app/build.gradle.kts`, préfixe `v` exigé. Un script Gradle ne peut pas partager de code avec l'app : la formule est dupliquée, et `ReleaseVersionTest` fige la table d'exemples (`0.0.1-rc.4` → 164, `0.0.1` → 199, `1.2.3-beta.5` → 100200335, bornes, rejets). Version installée : `PackageInfo.longVersionCode` ; la comparaison porte sur les `versionCode`, l'ordre qu'Android applique.
`UpdateSelector` (pur) : releases éligibles, préversion = suffixe ou `prerelease`, meilleure version finale et meilleure version tous canaux, version proposée selon la préférence (désactivée par défaut, quelle que soit la version installée).
Build de développement (`versionCode` 1, clé de debug) : comportement normal ; toute release est « plus récente » et la mise à jour échoue au contrôle du certificat avec le message de signature différente (décision de Sygix).

### D4. État et persistance
- `data/UpdateRepository` (portée application, dans `SygixOsApp`) : expose `StateFlow<UpdateStatus>` lu par `SettingsViewModel` et `HomeViewModel` (`updateBadge: StateFlow<Boolean>`, flux séparé de l'état de l'accueil, comme `clock`). Le téléchargement et l'installation tournent dans sa portée : fermer les réglages ou quitter SygixOs ne les annule pas.
- `UpdateStatus` : `Checking`, `Idle(proposed?, lastResult?)`, `Downloading(candidate, percent)`, `Verifying(candidate)`, `Installing(candidate)`, `Failed(error, candidate?)`. Le texte de « Vérifier les mises à jour » est calculé par `domain/UpdateStatusText` selon l'ordre de priorité de la spec (version proposée, sinon dernier résultat, sinon jamais vérifié).
- DataStore (même conteneur que `LauncherPrefs`) : `update_include_prereleases` (booléen, défaut faux) ; `update_last_check_at` (epoch ms) ; `update_last_result` (`ok` ou code d'erreur) ; `update_retry_at` (epoch ms, heure de réessai) ; `update_known` (deux candidats sérialisés, « meilleure finale » et « meilleure tous canaux » : tag, `versionCode`, préversion, URL de l'asset, taille, empreinte, `html_url`) ; `update_relaunch` (booléen) et `update_relaunch_version` (`versionCode` visé), D8. La bascule des préversions est une seule écriture atomique (`togglePrereleases`, lecture et écriture dans le même `edit`) : deux appuis rapides ne perdent rien.
- `UpdateStatus` est un data class (`checking`, `includePrereleases`, `proposed`, `lastResult`, `step`) plutôt qu'un type scellé : l'étape de l'opération (`UpdateStep` : `Downloading`, `Verifying`, `Installing`, `Failed`) coexiste avec une vérification. La version installée est lue hors du thread principal (`flowOn(IO)`), le code QR est encodé hors du thread principal (`flowOn(Default)` dans `SettingsViewModel`).
- Une vérification manuelle efface l'étape `Failed` affichée (l'utilisateur est devant l'écran) ; une vérification automatique la laisse, pour qu'une erreur non vue ne disparaisse pas.

### D5. Vérification automatique
`domain/DailyCheckPolicy` (pur, horloge injectée) : due si jamais vérifié, si `now - lastCheckAt ≥ 24 h` ou si `lastCheckAt > now`, et seulement si `now ≥ retryAt` ; seule la vérification automatique applique `retryAt`, une vérification manuelle envoie toujours sa requête (décision de Sygix). Évaluée après le premier affichage de l'accueil au démarrage à froid, et à chaque `onResume` de `MainActivity` (qui appelle déjà `viewModel.refresh()`). Sans réseau `NET_CAPABILITY_VALIDATED`, rien n'est envoyé ni enregistré. Sinon la date est enregistrée avant l'appel et le résultat après, réussite ou erreur.

### D6. Téléchargement et vérifications
Ordre dans `UpdateInstaller` (orchestrateur testé avec des fakes) :
1. Relecture de la release (D1), envoyée même avant `retryAt` puisqu'elle suit un appui ; release introuvable ou inéligible → candidat retiré de `update_known`, étape `Failed(Withdrawn)` gardée pour la ligne de version retirée (D9) jusqu'à la vérification manuelle suivante.
2. `cacheDir.usableSpace / 2 ≥ size` sinon `NoSpace` (sans dépassement ; une release dont l'asset dépasse 200 Mo n'est de toute façon pas éligible). L'autorisation « applis inconnues » n'est **pas** vérifiée avant : c'est le système qui la demande au moment de l'installation (D7).
3. Téléchargement en flux (politique HTTPS de D2) vers `cacheDir/updates/<tag>.apk.part`, délais connexion 10 s, lecture 30 s et total 10 min, arrêt au-delà de `size`, SHA-256 calculé pendant l'écriture (`MessageDigest`), progression publiée au plus tous les 1 % ; 404 ou 410 → `AssetNotFound` ; `SSLHandshakeException` ou `SSLPeerUnverifiedException` → `SecureConnection`, autre `SSLException` → `Interrupted` ; redirection vers HTTP, autre code HTTP, coupure ou délai → `Interrupted` (« Téléchargement interrompu », regroupement validé par Sygix).
4. Taille exacte, puis empreinte égale au `digest` ; renommage en `.apk`.
5. `data/ApkInspector` (interface) : `PackageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)` → nom de paquet, `longVersionCode`, SHA-256 de chaque certificat de `signingInfo.apkContentsSigners` ; même lecture pour l'app installée. Exigé : paquet `fr.sygix.sygixos`, `versionCode` égal à celui du tag et supérieur à l'installé, ensembles de certificats égaux.
6. Tout échec : suppression du fichier, `Failed(cause)`, l'installateur n'est jamais appelé.
Nettoyage : `cacheDir/updates/` vidé à chaque démarrage à froid, après tout échec, et dès la copie dans la session (D7).

### D7. Installation
`data/PackageInstallerGateway` (interface) : session `MODE_FULL_INSTALL`, `setAppPackageName`, `setSize`, `setRequireUserAction(USER_ACTION_NOT_REQUIRED)`.
- **Copie sans TOCTOU** : le fichier vérifié est recopié dans `session.openWrite` en recalculant le SHA-256 au fil de la copie ; après `session.fsync`, l'empreinte est comparée au `digest` ; si elle diffère, `session.abandon()` ; sinon le fichier est supprimé **avant** `session.commit`. Les octets validés par le système sont donc ceux dont l'empreinte vient d'être recalculée, et plus aucun fichier modifiable ne subsiste au moment du commit.
- `commit` avec un `PendingIntent` (`FLAG_MUTABLE`, exigé pour que le système y ajoute le statut) vers un récepteur non exporté `InstallStatusReceiver`.
- Conditions Android d'une mise à jour sans action de l'utilisateur (doc de `setRequireUserAction`) : l'installateur détient `REQUEST_INSTALL_PACKAGES` (et l'autorisation « applis inconnues » accordée) ; **l'APK installé, donc la nouvelle version**, cible un niveau d'API récent (SygixOs cible 37) ; l'installateur est l'installateur enregistré de l'app ou **se met à jour lui-même** (notre cas, même installée par `adb`) ; il déclare `UPDATE_PACKAGES_WITHOUT_USER_ACTION`. Les deux permissions sont ajoutées au manifeste. À constater sur la TV en tâche 1.1, y compris depuis l'arrière-plan.
- `STATUS_PENDING_USER_ACTION` : `EXTRA_INTENT` est l'écran du système (confirmation, ou guidage vers l'autorisation « applis inconnues » quand elle manque). Si SygixOs est au premier plan (`data/ForegroundTracker`, compteur d'activités démarrées par `ActivityLifecycleCallbacks` dans `SygixOsApp`, sans dépendance), il est lancé aussitôt. Sinon il est gardé en mémoire dans `UpdateRepository` et lancé au prochain `onResume` de `MainActivity` : mécanisme le plus simple, sans notification ni service. Si le processus meurt entre-temps, la demande est perdue ; au démarrage à froid, les sessions de SygixOs restées ouvertes (`packageInstaller.mySessions`) sont abandonnées et la version reste proposée. Si l'écran ne peut pas être lancé (`ActivityNotFoundException`), la session est abandonnée et la ligne indique où accorder l'autorisation (texte provisoire validé, ajusté après la tâche 1.1).
- Seuls les statuts de la session en cours sont pris en compte (identifiant de session gardé au `commit`) ; un statut d'une autre session est ignoré.
- **Retour sans statut** : quand SygixOs revient au premier plan après avoir affiché un écran du système et qu'aucun statut n'est arrivé, il attend 1,5 s puis, si la session n'est pas active (`SessionInfo.isActive`, vrai pendant une installation acceptée), l'abandonne et passe à « Installation annulée », « Mettre à jour vers X » restant disponible. Hypothèse à constater en tâche 1.1 : `isActive` est faux tant que la session attend l'utilisateur et vrai dès qu'il a accepté.
- `STATUS_FAILURE_ABORTED` → « installation annulée » ; autres `STATUS_FAILURE_*` → `InstallFailed(famille)` (textes du tableau de D9) ; `STATUS_SUCCESS` : le système arrête le processus pour le remplacer.

### D8. Redémarrage et rôle d'écran d'accueil
- La demande de relance (`update_relaunch` vrai et `update_relaunch_version` = `versionCode` visé) est écrite quand l'installation est validée au premier plan : juste avant le `commit` d'une installation lancée alors que SygixOs est au premier plan, ou juste avant d'afficher l'écran de confirmation du système (qui n'est affiché qu'au premier plan). Un `commit` en arrière-plan (Home pressé pendant le téléchargement) l'efface. Elle n'est écrite pour l'écran de confirmation que si SygixOs est encore au premier plan juste avant de le lancer (sinon la demande d'action est gardée pour le retour). Elle est aussi effacée sur `STATUS_FAILURE_ABORTED`, tout autre échec (y compris un statut reçu par un processus neuf, sans opération en cours), écran système indisponible, abandon de la session au retour sans statut, et au démarrage à froid quand une session restée ouverte est abandonnée.
- Récepteur `Intent.ACTION_MY_PACKAGE_REPLACED` (diffusé à l'app mise à jour, dans son nouveau processus) : lit puis efface la demande ; il démarre `MainActivity` (`FLAG_ACTIVITY_NEW_TASK`) seulement si elle valait vrai et que la version installée est celle visée ; sinon ne fait rien (aucun écran ne surgit). Le processus ainsi démarré n'a pas créé l'accueil : la première ouverture est un démarrage à froid (`startup-splash`).
- `MainActivity` reçoit un second filtre `MAIN` + `HOME` + `DEFAULT` (décision de Sygix). Effets attendus : quand SygixOs est le launcher par défaut, le système relance de lui-même l'écran d'accueil après le remplacement du processus, et Android exempte l'app d'accueil des restrictions de démarrage d'activité en arrière-plan ; quand il ne l'est pas, Android peut proposer un choix de launcher au prochain appui sur Home. L'app n'appelle jamais `RoleManager.createRequestRoleIntent(ROLE_HOME)` et n'ouvre aucun réglage de launcher. Lien avec P5 : si le launcher Google est désactivé (commandes `pm disable-user` du README), SygixOs devient le seul écran d'accueil possible.
- **Hypothèse à vérifier** (tâche 1.1) : quand SygixOs n'est pas le launcher par défaut, le démarrage de `MainActivity` depuis le récepteur peut être bloqué par les restrictions de démarrage en arrière-plan d'Android 14. Robolectric ne simule pas ces restrictions : seule la TV tranche.

### D9. Interface
- Lignes d'« À propos » (`AboutContent`), après la phrase de licence et avant « Licences open source » : `update-check` (« Vérifier les mises à jour » + texte d'état), `update-install` (« Mettre à jour vers X » + étape ou erreur, juste dessous, seulement si une version est proposée), `update-withdrawn` (version retirée : titre « Mettre à jour vers X » de la version retirée et « Cette version n'est plus disponible », sans code QR ni pastille, OK sans effet, jusqu'à la vérification manuelle suivante ; affichée sous `update-install` quand une autre version est proposée, décision de Sygix), `update-prereleases` (« Inclure les préversions » + `AppleSwitch`, tag `update-prereleases-switch`). `contentFocus` passe de la première licence à `update-check`.
- **Notes de version** : code QR `update-release-notes-qr` avec la légende « Notes de version », dans une colonne à droite du bloc des lignes de mise à jour, aligné en haut sur `update-install` et superposé à la hauteur des lignes suivantes (décision de Sygix). Un `Layout` dédié mesure les lignes à la largeur restante et les empile ; la colonne n'augmente ni leur hauteur ni leur position, ne recouvre aucun texte, et le bloc grandit seulement si le code dépasse la dernière ligne ; choix : à côté de la ligne plutôt que dans un détail, parce qu'« À propos » n'a pas d'écran de détail et que le code reste visible sans changer la navigation. Taille 120 dp, soit 240 px : une URL de release (environ 60 caractères) donne un code de version 4 (33 modules, 41 avec la marge), environ 6 px par module, modules noirs sur plaque blanche, marge de silence de 4 modules, correction d'erreur M ; dessiné dans un `Canvas` Compose depuis la matrice, sans bitmap.
- **Génération du QR** : `com.google.zxing:core` (Apache-2.0), seulement `QRCodeWriter` / `Encoder` ; R8 retire le reste de la bibliothèque. Bibliothèque de référence, sans dépendance transitive, éprouvée depuis des années ; elle est en maintenance (corrections seulement), ce qui suffit pour un encodeur au format figé. Alternatives écartées : encodeur maison (Reed-Solomon, masques, choix de version : du code délicat à écrire et à tester pour un seul usage) ; `io.nayuki:qrcodegen` (MIT, plus petit, mais bien moins répandu).
- **Pastille** : composant `Badge` de `core/designsystem` (« Pastille » de `launcher-shell`), jeton `SygixColors.Badge` = `#0A84FF`, 6 dp ; `update-badge-gear` sur l'engrenage de `HeroCapsule`, `update-badge-about` sur la catégorie « À propos ».
- Textes (validés par Sygix, en ressources FR) :

| État | Texte |
| --- | --- |
| jamais vérifié | « Jamais vérifié » |
| en cours | « Vérification… » |
| à jour | « SygixOs est à jour » |
| disponible | « Version 0.0.2 disponible » / « Préversion 0.0.2-rc.1 disponible » |
| ligne de mise à jour | titre « Mettre à jour vers 0.0.2 » ; étapes « Téléchargement… 42 % », « Vérification du fichier… », « Installation… » |
| pas de réseau / délai | « Pas de connexion à Internet » / « GitHub ne répond pas, réessayez plus tard » |
| connexion sécurisée | « Connexion sécurisée impossible (vérifiez la date et l'heure de la TV) » (décision de Sygix) |
| limite | « Trop de vérifications, réessayez après 14:05 » (format 12/24 h du système) |
| inaccessibles / illisible | « Releases inaccessibles » / « Réponse de GitHub illisible » |
| fichier | « Fichier corrompu, réessayez » ; « Signature différente de l'app installée » ; « Release incohérente » ; « Téléchargement interrompu » ; « Espace insuffisant » |
| téléchargement | « Téléchargement interrompu » regroupe la coupure, le délai, une redirection vers HTTP et tout autre code HTTP que 404/410 (validé par Sygix) |
| installation | « Installation annulée » ; « Échec de l'installation : <famille> », familles « bloquée par le système », « conflit avec la version installée », « version incompatible avec la TV », « fichier refusé par le système », « stockage insuffisant », « délai dépassé », « erreur inconnue » (validées par Sygix) ; repli sans écran système : « Autorisez SygixOs à installer des applis inconnues dans les paramètres de la TV » (validé, ajusté après la tâche 1.1) |

Le tableau validé ne couvrait pas quatre textes ajoutés par les décisions et la relecture : le titre « Mettre à jour vers X » (décision de Sygix), la légende « Notes de version », « Cette version n'est plus disponible » (release retirée) et « Fichier de la version introuvable » (asset 404/410).

## Risks / Trade-offs
- [Relance bloquée quand SygixOs n'est pas le launcher par défaut] → constat en tâche 1.1 ; quand il l'est, le système relance l'écran d'accueil.
- [`CATEGORY_HOME` fait apparaître un choix de launcher] → effet attendu, documenté dans le README ; l'app ne fait rien pour devenir launcher par défaut.
- [Installation sans confirmation refusée depuis l'arrière-plan, ou confirmation imposée] → couvert par la spec (action requise gardée pour le retour) ; constat en tâche 1.1.
- [Processus tué pendant un téléchargement en arrière-plan] → fichiers et sessions nettoyés au démarrage à froid, version toujours proposée ; pas de service de premier plan, l'utilisateur relance.
- [Compromission du dépôt ou du compte GitHub] → l'empreinte vient du même canal ; le certificat de signature n'est pas sur GitHub (secret de la CI) : un APK signé par une autre clé est refusé (Android le refuserait aussi). Une fuite de la clé de release resterait non couverte.
- [Rotation future de la clé de signature] → l'égalité stricte des certificats refuserait la mise à jour ; à revoir dans un change dédié.
- [Formule de `versionCode` dupliquée] → `ReleaseVersionTest` fige les exemples ; toute modification de `versionCodeOf` doit être répercutée.
- [Limite de 60 requêtes/h partagée par le foyer] → une requête par vérification, au plus une automatique par jour, heure de réessai persistée et respectée par la vérification automatique ; les requêtes demandées à la main passent toujours (décision de Sygix).
- [Dépôt encore privé à l'implémentation] → tests automatisés contre un faux serveur ; sur la TV, l'état « releases inaccessibles » est le comportement attendu jusqu'au passage en public, puis validation complète (tâches 9.x).

## Migration Plan
Nouvelles clés DataStore avec valeurs par défaut (préversions désactivées, jamais vérifié) : aucune migration. Les versions déjà installées sans cette fonctionnalité se mettent à jour une dernière fois par `adb install -r` ou depuis la page Releases. Retour arrière : revenir au commit précédent ; les clés ajoutées restent inertes, le filtre `HOME` disparaît avec la version.
