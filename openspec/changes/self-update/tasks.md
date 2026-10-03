# Tasks

## 1. Vérifications préalables (avant tout code de la fonctionnalité)
- [ ] 1.1 Sur la TV de test, avec deux pré-releases successives signées par la CI installées depuis un fichier local (sans passer par l'API, réalisable dépôt privé) et une build d'essai locale non mergée (récepteur `MY_PACKAGE_REPLACED` qui démarre `MainActivity`, session `PackageInstaller` avec `USER_ACTION_NOT_REQUIRED`) : consigner dans `design.md` (structure seulement) si la mise à jour passe sans confirmation, si l'app se relance seule après la mise à jour, si `ACTION_MANAGE_UNKNOWN_APP_SOURCES` ouvre un écran et, sinon, le chemin de l'autorisation dans les paramètres de la TV
- [ ] 1.2 Réponses de Sygix aux questions ouvertes QS1 à QS11 de `design.md` reportées dans les documents du change (specs, design, tasks) ; plus aucune mention « à confirmer » restante

## 2. Domaine (JUnit)
- [ ] 2.1 `domain/ReleaseVersion` (D3) ; `ReleaseVersionTest` : `v0.0.1-rc.4` → 164, `v0.0.1` → 199, `v1.2.3-beta.5` → 100200335, `v0.0.2-rc1` accepté, rejets `v1.0`, `0.0.3`, `v0.0.3-rc.30`, `v21.0.0`
- [ ] 2.2 `domain/UpdateSelector` ; `UpdateSelectorTest` : chaque scénario de « Versions comparées » (finale après préversion, préversion masquée, préversion activée, canal final avec préversion installée, ordre par version et non par date, même version, tags non conformes, release sans APK, sans empreinte, asset non téléversé, URL non HTTPS, préversion selon l'API)
- [ ] 2.3 `domain/DailyCheckPolicy` ; test : jamais vérifié, 3 h, 24 h, date future, heure de réessai après limite atteinte
- [ ] 2.4 `domain/UpdateUrlPolicy` ; test : `https` accepté, `http`, sans schéma et autres schémas refusés, URL finale `http` refusée

## 3. Données
- [ ] 3.1 `data/HttpTransport` et `HttpsUrlTransport` (D2) ; tests JUnit contre `com.sun.net.httpserver.HttpServer` sur la boucle locale : corps et en-têtes `x-ratelimit-*` / `retry-after` rendus, délai dépassé sur un serveur muet, corps tronqué au-delà de `maxBytes`, absence d'en-tête `Authorization` et `Cookie`, `User-Agent` = `SygixOs/<version>`
- [ ] 3.2 `data/GitHubReleaseSource` (`Result`, D1) ; tests Robolectric avec transport factice : liste vide, JSON illisible, 404, 403 avec `x-ratelimit-remaining: 0`, 429 avec `retry-after`, absence de réseau, délai, liste valide
- [ ] 3.3 Persistance DataStore (D4) : préversions (défaut faux), date de dernière vérification, deux candidats connus ; test de lecture des valeurs par défaut et d'aller-retour
- [ ] 3.4 `data/ApkInspector` et `data/PackageInstallerGateway` (D6, D7) derrière des interfaces ; `InstallStatusReceiver` non exporté ; correspondance des statuts testée en JUnit (`PENDING_USER_ACTION`, `FAILURE_ABORTED`, familles d'échec, `SUCCESS`)
- [ ] 3.5 `UpdateRepository` et orchestration (D4 à D6) ; tests avec fakes : empreinte invalide → installateur jamais appelé et fichier absent ; certificat différent → idem ; paquet ou `versionCode` incohérent → idem ; taille trop grande ou trop petite → idem ; espace insuffisant → aucun téléchargement ; autorisation absente → `NeedsPermission` sans téléchargement ; annulation du `Job` → fichier partiel supprimé ; nettoyage de `cacheDir/updates` au démarrage
- [ ] 3.6 Vérification automatique (D5) : test avec horloge et connectivité factices : due + réseau validé → une requête et date enregistrée ; sans réseau → aucune requête ni date ; non due → aucune requête ; échec → aucun état d'erreur visible hors « À propos »

## 4. Manifeste et redémarrage
- [ ] 4.1 Permissions `REQUEST_INSTALL_PACKAGES` et `UPDATE_PACKAGES_WITHOUT_USER_ACTION`, récepteur `MY_PACKAGE_REPLACED` (ou repli retenu en QS10) ; test Robolectric : la diffusion `MY_PACKAGE_REPLACED` démarre `MainActivity`

## 5. Interface
- [ ] 5.1 Lignes `update-check`, `update-install`, `update-prereleases` / `update-prereleases-switch` dans `AboutContent` (D9), focus initial sur `update-check`, pilules de focus existantes ; textes en ressources FR (`strings.xml`), aucune chaîne en dur
- [ ] 5.2 Pastilles `update-badge-gear` (capsule) et `update-badge-about` (catégorie), `HomeViewModel.updateBadge` séparé de l'état de l'accueil ; jeton `SygixColors.UpdateBadge`
- [ ] 5.3 Ouverture de l'écran d'autorisation et de la confirmation système, focus restauré au retour (« retour d'un écran système »)
- [ ] 5.4 Tests Compose à la taille TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) `UpdateAboutTest` : navigation dans À propos, apparition et disparition de `update-install` avec le focus, bascule des préversions sans appel au transport, états et erreurs affichés ; `UpdateBadgeTest` : pastille visible, jamais focusée, capsule et engrenage aux mêmes position et taille qu'un test sans version ; chaque test échoue si l'on retire le comportement testé (vérifié en local)
- [ ] 5.5 Les tests existants des réglages et de la capsule restent verts (`SettingsNavigationTest`, `SettingsPillTest`, `HeroCapsuleTest`)

## 6. Documentation et vérification
- [ ] 6.1 README : fonctionnalité de mise à jour depuis Réglages > À propos, autorisation « applis inconnues », section Installation (première installation par `adb`, puis mises à jour depuis l'app)
- [ ] 6.2 `./gradlew test` vert, `./gradlew assembleRelease` vert, `openspec validate --all --strict` vert
- [ ] 6.3 Aucun commentaire ajouté dans le code ni dans les tests (diff vérifié), en-tête de licence sur chaque nouveau fichier `.kt` ; aucun jeton ni secret dans le diff (`git diff origin/main... | grep -niE 'token|authorization|ghp_|github_pat'` vide hors noms d'en-têtes testés)

## 7. Validation sur la TV tant que le dépôt est privé (pré-release signée par la CI, `assembleRelease`)
- [ ] 7.1 « Vérifier les mises à jour » affiche l'erreur « releases inaccessibles », sans crash ; aucune pastille ; vérification automatique au démarrage silencieuse
- [ ] 7.2 Sans réseau : « Pas de connexion à Internet » ; aucun fichier dans le cache de mise à jour

## 8. Passage du dépôt en public (Sygix)
- [ ] 8.1 Une requête sans jeton à l'API des releases du dépôt répond 200 avec les champs du tableau de `design.md` (`tag_name`, `draft`, `prerelease`, `assets[].name`, `state`, `size`, `digest`, `browser_download_url`)

## 9. Validation sur la TV après le passage en public (pré-releases signées par la CI, `assembleRelease`)
- [ ] 9.1 Préversions désactivées puis activées, avec deux pré-releases publiées : états « à jour » puis « préversion disponible », pastille sur l'engrenage et sur « À propos »
- [ ] 9.2 Mise à jour complète depuis « Mettre à jour » : téléchargement, vérification, installation, redémarrage seul sur l'accueil, nouvelle version dans « À propos », pastille disparue
- [ ] 9.3 Autorisation « applis inconnues » retirée puis rétablie : guidage, retour, focus sur la ligne
- [ ] 9.4 Build locale signée avec la clé de debug : la mise à jour est refusée avec « Signature différente de l'app installée », aucun fichier dans le cache de l'app ensuite
- [ ] 9.5 Réseau coupé pendant un téléchargement : « Téléchargement interrompu », aucun fichier résiduel

## 10. Clôture
- [ ] 10.1 `openspec archive self-update` après merge, validation sur la TV (9.x) et archivage préalable de `ui-tvos-polish` ; `openspec validate --all --strict` vert après fusion des deltas
