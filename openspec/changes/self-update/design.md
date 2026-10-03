# Design

## Context
Voir `proposal.md` pour la motivation. État de départ (`main`, plus `ui-tvos-polish` pour les réglages et la capsule) :
- **Versions** : `app/build.gradle.kts` dérive `versionName` (tag sans `v`) et `versionCode` du tag fourni par la CI (`SYGIXOS_VERSION`) : major·10⁸ + minor·10⁵ + patch·10² + suffixe (alpha.N → N, beta.N → 30 + N, rc.N → 60 + N, finale → 99), avec major ≤ 20, minor et patch ≤ 999, N ≤ 29 ; regex `(\d{1,2})\.(\d{1,3})\.(\d{1,3})(?:-(alpha|beta|rc)\.?(\d{1,2}))?`. Exemple : `v0.0.1-rc.4` → 164. Hors CI : `versionName` « 0.1.0 », `versionCode` 1.
- **Publication** : `.github/workflows/release.yml` construit sur un tag `v*`, signe avec la clé de release (secrets), vérifie la signature (`apksigner verify`) et publie `app/build/outputs/apk/release/app-release.apk` dans une release GitHub, marquée préversion si le tag contient `-`. Sans variables de signature, une build locale est signée avec la clé de debug.
- **Réseau** : permission `INTERNET` déjà déclarée ; `network_security_config.xml` autorise le HTTP en clair pour toute l'app (posters servis en HTTP sur le réseau local) : la règle « HTTPS uniquement » doit donc être appliquée par le code de mise à jour, pas par la configuration réseau. Aucun client HTTP n'est déclaré : Coil 2 embarque OkHttp de façon transitive, media3 utilise `HttpURLConnection`.
- **Réglages** : `SettingsViewModel` lit la version installée (`PackageManager.getPackageInfo(...).versionName`) ; `AboutContent` affiche « SygixOs », la version, une phrase de licence, puis la liste des licences (premier élément focusable : la première licence) ; les lignes ont la pilule de focus (`focusPill`, `animatedPillColors`) de `ui-tvos-polish`. `AppleSwitch` existe déjà (« Apps sources »).
- **Persistance** : un DataStore Preferences unique (`LauncherPrefs`), conteneur applicatif dans `SygixOsApp`.
- **Capsule** : `HeroCapsule` dans `HomeScreen.kt` (`hero-capsule`, `settings-gear`), introduite par `ui-tvos-polish`.

### Source : API REST des releases GitHub (vérifiée le 2026-10-03, structure seulement)
`GET https://api.github.com/repos/Sygix/SygixOs/releases?per_page=100`. Champs utilisés, présents sur les trois releases existantes (`v0.0.1-rc.1`, `rc.2`, `rc.4`, toutes préversions) :

| Champ | Type | Usage |
| --- | --- | --- |
| `tag_name` | chaîne | version (`v0.0.1-rc.4`) |
| `draft` | booléen | brouillons ignorés (de toute façon invisibles sans authentification) |
| `prerelease` | booléen | préversion si vrai, en plus du suffixe du tag |
| `assets[].name` | chaîne | `app-release.apk` exactement |
| `assets[].state` | chaîne | `uploaded` exigé |
| `assets[].size` | entier | taille attendue (environ 3,5 Mo aujourd'hui) |
| `assets[].digest` | chaîne ou `null` | `sha256:<64 hex>` ; présent sur les trois assets existants ; le schéma de la doc GitHub le déclare « string or null » : `null` ou autre algorithme → release non éligible |
| `assets[].browser_download_url` | chaîne | URL HTTPS de téléchargement (`https://github.com/...`, redirigée vers un hôte de contenu GitHub en HTTPS) |

Ce qui manque et comment on s'en passe : aucune signature détachée ni attestation → l'authenticité repose sur le certificat de signature de l'APK (D6) ; l'empreinte vient du même canal que l'APK et ne protège que contre la corruption et la troncature. Les notes de version (`body`) ne sont pas utilisées. Une release pèse environ 4 Kio dans la réponse (12,8 Kio pour 3 releases) : 100 releases ≈ 400 Kio au pire, une fois par jour au plus.

Le dépôt est privé à la date de ce change : sans jeton, l'API répond 404. L'implémentation n'en dépend pas (tests contre un faux serveur) ; seule la validation sur la TV contre les vraies releases attend le passage en public (`proposal.md`, tâches 9.x).

### Limite de requêtes
Sans authentification : 60 requêtes par heure et par adresse IP publique (partagées par les appareils d'un même foyer). La doc GitHub précise qu'une réponse `304` à une requête conditionnelle ne décompte pas la limite **seulement pour une requête authentifiée** : `ETag` / `If-None-Match` n'économiserait que la bande passante. Les téléchargements d'assets (`browser_download_url`) ne passent pas par l'API REST et ne décomptent pas cette limite.

## Goals / Non-Goals

**Goals:**
- Une seule requête par vérification, un seul téléchargement par mise à jour, aucune requête sans besoin.
- Aucun octet exécuté ou installé sans vérification complète (taille, empreinte, paquet, version, certificat).
- Logique pure testable en JUnit ; Android (réseau, `PackageManager`, `PackageInstaller`) derrière des interfaces.
- Zéro nouvelle dépendance, ni d'exécution ni de test.

**Non-Goals:**
- Mises à jour en arrière-plan (WorkManager, tâches planifiées) : la vérification automatique n'a lieu qu'au démarrage.
- Prise en charge d'une rotation de la clé de signature (voir Risks).

## Decisions

### D1. Une requête sur la liste des releases
`GET /repos/Sygix/SygixOs/releases?per_page=100`, en-têtes `Accept: application/vnd.github+json`, `X-GitHub-Api-Version: 2022-11-28`, `User-Agent: SygixOs/<versionName>` (exigé par GitHub ; aucune donnée personnelle). Délais : connexion 10 s, lecture 15 s.
Alternatives écartées : `/releases/latest` (exclut les préversions et choisit par date, il faudrait une seconde requête pour le canal préversions) ; plusieurs pages (inutile : on cherche la plus haute version, toujours parmi les plus récentes) ; `ETag` (aucun gain sur la limite sans authentification, état de plus à persister).
Correspondance des erreurs (`domain/UpdateError`) : `UnknownHostException` / absence de réseau → `NoNetwork` ; `SocketTimeoutException` → `Timeout` ; 403 ou 429 avec `x-ratelimit-remaining: 0` → `RateLimited(resetAt)` (`x-ratelimit-reset`, secondes epoch) ou `retry-after` (secondes) ; 404 et autres codes ≥ 400 → `Unavailable(code)` ; JSON invalide → `Unreadable`. La source renvoie un `Result<List<ReleaseInfo>>`.

### D2. Client HTTP : `HttpsURLConnection` de la plateforme
Deux GET seulement (la liste, puis l'APK en flux) : la plateforme suffit. Déclarer OkHttp reviendrait à figer une dépendance aujourd'hui transitive de Coil 2 (et optionnelle dans Coil 3) ; aucune autre bibliothèque n'est justifiée. JSON : `org.json` de la plateforme (réel sous Robolectric).
- `data/HttpTransport` (interface) : `get(url, headers, maxBytes, sink)` → statut, en-têtes utiles, URL finale ; implémentation `HttpsUrlTransport` (`instanceFollowRedirects = true` : `HttpURLConnection` ne suit jamais une redirection qui change de protocole ; aucun `CookieHandler` installé ; lecture interrompue au-delà de `maxBytes`).
- `domain/UpdateUrlPolicy` : refuse toute URL non `https` **avant** l'appel, et l'URL finale renvoyée par le transport si elle n'est pas `https`. La règle est hors du transport pour pouvoir tester celui-ci contre un serveur HTTP local.
- Tests du transport : serveur `com.sun.net.httpserver.HttpServer` du JDK, sur la boucle locale, dans des tests JUnit simples (sans Robolectric) : aucune nouvelle dépendance de test. Alternative écartée : MockWebServer (nouvelle dépendance de test, inutile ici).

### D3. Versions : `domain/ReleaseVersion`
Même regex et même formule que `versionCodeOf` de `app/build.gradle.kts`, préfixe `v` exigé. Un script Gradle ne peut pas partager de code avec l'app : la formule est dupliquée, et `ReleaseVersionTest` fige la table d'exemples (`0.0.1-rc.4` → 164, `0.0.1` → 199, `1.2.3-beta.5` → 100200335, bornes major 20, N 29, rejets `v1.0`, `0.0.3` sans `v`, `rc.30`). Version installée : `PackageInfo.longVersionCode`. La comparaison se fait sur les `versionCode`, l'ordre même qu'Android applique (il refuse une mise à jour de `versionCode` inférieur).
`domain/UpdateSelector` (pur) : releases éligibles (`Versions comparées`), préversion = suffixe ou `prerelease`, meilleure version finale et meilleure version tous canaux ; version proposée selon la préférence, si `versionCode` > installé.
Build de développement (`versionCode` 1, clé de debug) : toute release est « plus récente », et la mise à jour échoue proprement au contrôle du certificat (scénario « certificat différent ») ; choix provisoire, voir QS11.

### D4. État et persistance
- `data/UpdateRepository` (portée application, dans `SygixOsApp`) : expose `StateFlow<UpdateStatus>` lu par `SettingsViewModel` (lignes) et `HomeViewModel` (`updateBadge: StateFlow<Boolean>`, flux séparé de l'état de l'accueil, comme `clock`).
- `UpdateStatus` : `NotChecked`, `Checking`, `UpToDate`, `Available(candidate)`, `NeedsPermission(candidate)`, `Downloading(candidate, percent)`, `Verifying(candidate)`, `Installing(candidate)`, `Failed(error, candidate?)`.
- DataStore (mêmes fichier et conteneur que `LauncherPrefs`) : `update_include_prereleases` (booléen, défaut faux), `update_last_check_at` (epoch ms), `update_known` (deux candidats sérialisés, « meilleure finale » et « meilleure tous canaux » : tag, `versionCode`, préversion, URL, taille, empreinte). Garder ces deux candidats suffit à afficher la pastille dès le démarrage à froid et à appliquer l'interrupteur des préversions sans requête.

### D5. Vérification automatique
`domain/DailyCheckPolicy` (pur, horloge injectée) : due si jamais vérifié, si `now - lastCheckAt ≥ 24 h`, ou si `lastCheckAt > now`. Déclenchée une fois par processus, après le premier affichage de l'accueil, dans la portée de `UpdateRepository` ; si `ConnectivityManager` n'a pas de réseau `NET_CAPABILITY_VALIDATED`, rien n'est envoyé ni compté. Sinon la tentative est enregistrée (`lastCheckAt`) avant l'appel, réussite ou non : « au plus une fois par jour ». Après un `RateLimited(resetAt)`, aucune vérification automatique avant `resetAt`.

### D6. Téléchargement et vérifications
Ordre dans `UpdateInstaller.prepare` (orchestrateur testé avec des fakes) :
1. `canRequestPackageInstalls()` sinon `NeedsPermission` ; `cacheDir.usableSpace ≥ 2 × size` sinon `Failed(NoSpace)` (fichier + copie dans la session d'installation).
2. Téléchargement en flux de `browser_download_url` (politique HTTPS de D2) vers `cacheDir/updates/<tag>.apk.part`, délais connexion 10 s et lecture 30 s, arrêt au-delà de `size`, SHA-256 calculé pendant l'écriture (`MessageDigest`), progression publiée au plus tous les 1 %.
3. Taille exacte, puis empreinte égale au `digest` (comparaison en temps constant inutile ici, simple égalité hexadécimale en minuscules) ; renommage en `.apk`.
4. `data/ApkInspector` (interface) : `PackageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)` → nom de paquet, `longVersionCode`, SHA-256 de chaque certificat de `signingInfo.apkContentsSigners` ; même lecture pour l'app installée (`getPackageInfo(packageName, GET_SIGNING_CERTIFICATES)`). Exigé : paquet `fr.sygix.sygixos`, `versionCode` égal à celui du tag et supérieur à l'installé, ensembles de certificats égaux.
5. Tout échec : suppression du fichier, `Failed(cause)`, l'installateur n'est jamais appelé.
Nettoyage : `cacheDir/updates/` vidé à chaque démarrage à froid, après tout échec, abandon ou statut d'installation. Le téléchargement tourne dans un `Job` annulé à la fermeture des réglages (choix provisoire QS6), ce qui supprime le fichier partiel.

### D7. Installation
`data/PackageInstallerGateway` (interface) : session `MODE_FULL_INSTALL`, `setAppPackageName(packageName)`, `setSize(size)`, `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` ; copie du fichier dans la session ; `commit` avec un `PendingIntent` (`FLAG_MUTABLE`, exigé pour que le système y ajoute le statut) vers un récepteur non exporté `InstallStatusReceiver`.
Conditions Android pour une mise à jour sans action de l'utilisateur (doc de `setRequireUserAction`) : l'installateur a `REQUEST_INSTALL_PACKAGES`, l'app installée cible un niveau d'API récent (SygixOs cible 37), l'installateur est l'installateur enregistré de l'app **ou se met à jour lui-même** (notre cas, même installée par `adb`), et il déclare `UPDATE_PACKAGES_WITHOUT_USER_ACTION`. Les deux permissions sont ajoutées au manifeste. Constat sur la TV en tâche 1.1.
Statuts : `STATUS_PENDING_USER_ACTION` → lancement de `EXTRA_INTENT` (l'app est au premier plan) ; `STATUS_FAILURE_ABORTED` → « installation annulée » ; autres `STATUS_FAILURE_*` → `Failed(InstallFailed(status))` avec un texte par famille (incompatible, stockage, bloquée, autre) ; `STATUS_SUCCESS` : le système arrête le processus pour le remplacer.
Autorisation absente : `Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:fr.sygix.sygixos"))` ; si aucune activité ne la gère (`resolveActivity` nul ou `ActivityNotFoundException`), la ligne affiche le chemin des paramètres relevé en tâche 1.1. `canRequestPackageInstalls()` est relu à chaque retour au premier plan (`onResume` existant).

### D8. Redémarrage
Récepteur déclaré dans le manifeste pour `Intent.ACTION_MY_PACKAGE_REPLACED` (diffusé à l'app mise à jour, dans son nouveau processus) : il démarre `MainActivity` (`FLAG_ACTIVITY_NEW_TASK`). **Hypothèse à vérifier** (tâche 1.1) : depuis Android 10, un démarrage d'activité depuis un récepteur en arrière-plan est bloqué sauf exemptions ; l'app d'accueil par défaut (rôle Home) en fait partie, mais SygixOs ne déclare pas `CATEGORY_HOME`. Si le démarrage est bloqué sur la TV, la solution de repli est un choix de Sygix (QS10) à trancher avant l'implémentation de cette partie.

### D9. Interface
- Lignes d'« À propos » (`AboutContent`), après la phrase de licence et avant « Licences open source » : `update-check` (« Vérifier les mises à jour » + texte d'état), `update-install` (« Mettre à jour » + étape ou erreur, seulement si une version est proposée ; placée provisoirement juste sous `update-check`, QS4), `update-prereleases` (« Inclure les préversions » + `AppleSwitch`, tag `update-prereleases-switch`). Pilules de focus existantes ; `contentFocus` passe de la première licence à `update-check`.
- Pastilles : `update-badge-gear` dans `HeroCapsule`, dessinée en surimpression de l'engrenage sans changer sa taille ; `update-badge-about` sur la catégorie « À propos ». Aspect provisoire : point plein de 6 dp, bleu système `#0A84FF` (nouveau jeton `SygixColors.UpdateBadge`), en haut à droite de la pastille de l'engrenage et en fin de ligne de la catégorie (QS2).
- Textes provisoires (QS1), en ressources FR :

| État | Texte provisoire |
| --- | --- |
| jamais vérifié | « Jamais vérifié » |
| en cours | « Vérification… » |
| à jour | « SygixOs est à jour » |
| disponible | « Version 0.0.2 disponible » / « Préversion 0.0.2-rc.1 disponible » |
| ligne Mettre à jour | « Installer la version 0.0.2 » ; « Téléchargement… 42 % » ; « Vérification du fichier… » ; « Installation… » |
| pas de réseau / délai | « Pas de connexion à Internet » / « GitHub ne répond pas, réessayez plus tard » |
| limite | « Trop de vérifications, réessayez après 14:05 » (format 12/24 h du système) |
| inaccessibles / illisible | « Releases inaccessibles » / « Réponse de GitHub illisible » |
| fichier | « Fichier corrompu, réessayez » ; « Signature différente de l'app installée » ; « Release incohérente » ; « Téléchargement interrompu » ; « Espace insuffisant » |
| autorisation | « Autorisez SygixOs à installer des applis » ; repli : chemin relevé sur la TV |
| installation | « Installation annulée » ; « Échec de l'installation : <famille> » |

## Risks / Trade-offs
- [Redémarrage bloqué par les restrictions de démarrage en arrière-plan] → constat en tâche 1.1 avant tout code ; repli à trancher (QS10).
- [La TV impose une confirmation malgré `USER_ACTION_NOT_REQUIRED`] → couvert par la spec (confirmation du système) ; constat en tâche 1.1.
- [Écran « applis inconnues » absent ou ailleurs sur Google TV] → repli textuel avec le chemin relevé en tâche 1.1.
- [Compromission du dépôt ou du compte GitHub] → l'empreinte vient du même canal et ne protège pas ; le certificat de signature, lui, n'est pas sur GitHub (secret de la CI) : un APK signé par une autre clé est refusé (et Android le refuserait aussi). Une fuite de la clé de release resterait non couverte.
- [Rotation future de la clé de signature] → l'égalité stricte des certificats refuserait la mise à jour ; à revoir dans un change dédié si Sygix fait tourner la clé.
- [Formule de `versionCode` dupliquée] → `ReleaseVersionTest` fige les exemples ; toute modification de `versionCodeOf` doit être répercutée (mentionné dans la PR qui la changerait).
- [Limite de 60 requêtes/h partagée par le foyer] → une requête par vérification, au plus une automatique par jour, message clair avec l'heure de réessai.
- [Processus tué pendant un téléchargement] → fichiers résiduels supprimés au démarrage à froid.
- [Dépôt encore privé à l'implémentation] → tests automatisés contre un faux serveur ; sur la TV, l'état « releases inaccessibles » est le comportement attendu jusqu'au passage en public, puis validation complète (tâches 9.x).

## Migration Plan
Nouvelles clés DataStore avec valeurs par défaut (préversions désactivées, jamais vérifié) : aucune migration. Les versions déjà installées sans cette fonctionnalité se mettent à jour une dernière fois par `adb install -r` ou depuis la page Releases. Retour arrière : revenir au commit précédent ; les clés ajoutées restent inertes.

## Questions ouvertes
Choix non tranchés par Sygix. Pour chacun, l'option la plus conservatrice est retenue provisoirement ; elle est **à confirmer**. QS3, QS5 et QS6 apparaissent dans des scénarios de la spec (« consultation », « téléchargement interrompu », « sortie des réglages pendant le téléchargement ») parce que les cas d'erreur doivent y être couverts : ces scénarios seront réécrits si Sygix retient une autre option.

- **QS1. Libellés secondaires** (tableau de D9). Options : textes du tableau ; textes plus courts sans conseil (« Hors ligne », « Erreur GitHub »…) ; textes du tableau plus la date de dernière vérification sous « Vérifier les mises à jour ». Provisoire : tableau de D9, à confirmer.
- **QS2. Aspect et position de la pastille.** Options : point bleu système de 6 dp en haut à droite de l'engrenage et en fin de ligne « À propos » ; point blanc ; point rouge façon badge iOS ; chiffre ou flèche. Provisoire : point bleu de 6 dp, à confirmer.
- **QS3. Effacement de la pastille.** Options : seulement quand plus aucune version n'est proposée (installée, préversions désactivées) ; aussi dès que « À propos » a été ouvert pour cette version ; aussi après un délai. Provisoire : seulement quand plus aucune version n'est proposée, à confirmer.
- **QS4. Place de « Mettre à jour ».** Options : ligne distincte juste sous « Vérifier les mises à jour » ; la ligne « Vérifier les mises à jour » devient « Mettre à jour » quand une version est connue ; ligne distincte après « Inclure les préversions ». Provisoire : ligne distincte sous « Vérifier », à confirmer.
- **QS5. Téléchargement interrompu.** Options : abandon, fichier supprimé, nouvel appui pour recommencer de zéro ; une relance automatique unique ; reprise du téléchargement (requête `Range`). Provisoire : abandon et relance manuelle, à confirmer.
- **QS6. Fermeture des réglages pendant le téléchargement.** Options : abandon du téléchargement ; téléchargement poursuivi puis installation (avec éventuelle confirmation système par-dessus l'accueil) ; téléchargement poursuivi puis attente d'un nouvel appui. Provisoire : abandon, à confirmer.
- **QS7. Déclenchement de la vérification automatique.** Le processus d'un launcher peut vivre des jours (veille de la TV) : au seul démarrage à froid, la vérification peut être rare. Options : démarrage à froid seulement (lecture littérale de la décision) ; aussi au retour au premier plan quand elle est due ; tâche périodique du système (nouvelle dépendance WorkManager). Provisoire : démarrage à froid seulement, à confirmer ; recommandation : ajouter le retour au premier plan.
- **QS8. Notes de version.** Options : non affichées ; premières lignes du champ `body` sous la ligne « Mettre à jour » ; code QR vers la page de la release. Provisoire : non affichées, à confirmer.
- **QS9. Message après redémarrage.** Options : aucun ; message bref « SygixOs a été mis à jour en 0.0.2 » au premier affichage de l'accueil ; mention seulement dans « À propos ». Provisoire : aucun, à confirmer.
- **QS10. Si Android bloque le redémarrage automatique** (selon la tâche 1.1). Options : déclarer SygixOs comme app d'accueil (`CATEGORY_HOME`, ce qui relève de P5) pour que le système le relance ; demander l'autorisation « afficher par-dessus d'autres apps » (exemption) ; accepter le retour au launcher système et documenter. Provisoire : aucune, la tâche 1.1 tranche d'abord ; recommandation : `CATEGORY_HOME` si le constat est négatif.
- **QS11. Build de développement** (`versionCode` 1, clé de debug). Options : comportement normal, échec au contrôle du certificat ; lignes de mise à jour masquées hors build de release signée par la CI ; état « build de développement » sans vérification. Provisoire : comportement normal, à confirmer.
