# Change : self-update

## Why
SygixOs n'est pas distribué par un store : chaque nouvelle version se télécharge à la main depuis la page Releases puis s'installe par `adb install -r`. Sur une TV, c'est une étape d'ordinateur pour chaque mise à jour, et l'utilisateur ne sait pas qu'une version existe. Sygix veut que le launcher vérifie lui-même les releases GitHub du dépôt et s'installe à jour depuis Réglages > À propos, sans serveur propre ni jeton dans l'APK, et sans jamais installer quoi que ce soit sans action de l'utilisateur.

## What Changes
- **Vérification manuelle** : nouvelle ligne « Vérifier les mises à jour » dans Réglages > À propos ; elle interroge l'API REST publique des releases du dépôt Sygix/SygixOs, sans jeton, et affiche l'état : en cours, à jour, nouvelle version disponible (avec son numéro), ou une erreur claire.
- **Préversions** : interrupteur « Inclure les préversions » dans À propos, désactivé par défaut. Désactivé : seules les versions finales sont proposées ; activé : les préversions (tags avec suffixe, par exemple `v0.0.1-rc.4`) aussi.
- **Vérification automatique** discrète, au plus une fois par jour, au démarrage du launcher : elle ne fait que signaler (pastille sur l'engrenage de la capsule heure et réglages, et sur la catégorie « À propos »), elle n'installe jamais rien.
- **Comparaison de versions** robuste : le tag est converti en `versionCode` avec la formule du build (`app/build.gradle.kts`, par exemple `v0.0.1-rc.4` → 164, `v0.0.1` → 199), comparé au `versionCode` installé ; jamais de retour à une version inférieure.
- **« Mettre à jour »** : téléchargement de l'APK `app-release.apk` de la release, vérification de la taille et de l'empreinte SHA-256 publiée par l'API (champ `digest` des assets), puis de l'APK lui-même (paquet, version, certificat de signature identique à celui de l'app installée) ; installation par `PackageInstaller` (sans confirmation quand Android le permet, sinon avec la confirmation du système) ; guidage vers l'écran système « Installer des applis inconnues » si l'autorisation manque ; le launcher redémarre seul après la mise à jour.
- **Sécurité** : HTTPS uniquement, aucune installation si une vérification échoue, fichiers temporaires dans le cache de l'app et nettoyés, aucune donnée personnelle envoyée, respect de la limite de 60 requêtes par heure de l'API GitHub non authentifiée.
- Permissions ajoutées : `REQUEST_INSTALL_PACKAGES`, `UPDATE_PACKAGES_WITHOUT_USER_ACTION`.

## Capabilities

### New Capabilities
- `self-update` : source des versions, comparaison, vérifications manuelle et automatique, préversions, pastille, téléchargement vérifié, installation, autorisation, redémarrage, sécurité et confidentialité, lignes et navigation D-pad dans À propos.

### Modified Capabilities
- `ui-testing` : ADDED « Couverture des mises à jour ».
- `settings` : aucune exigence modifiée. « À propos » (version et licences) reste vraie ; les nouvelles lignes sont décrites dans `self-update`, qui renvoie à « Page de réglages » pour la structure, le focus initial et les pilules de focus, et à « Capsule heure et réglages » pour la pastille de l'engrenage.
- `launcher-shell` : non touchée (la pastille n'est pas une zone ni un palier et ne prend jamais le focus).

## Dépendances et chevauchements
- **Dépôt public (prérequis du test réel sur la TV seulement)** : le dépôt est encore privé ; l'API publique des releases y répond 404 sans jeton. L'implémentation commence dès le merge de cette spec, avec des tests automatisés contre un faux serveur (transport factice et serveur HTTP local du JDK). La vérification sur la TV contre les vraies releases attend le passage du dépôt en public (tâches 9.x de `tasks.md`) ; d'ici là, sur la TV, la vérification affiche l'état d'erreur « releases inaccessibles » prévu par la spec pour une source absente. Aucune exigence n'est propre au dépôt privé.
- **`ui-tvos-polish`** (PR #18, non mergée) : il renomme « Icône réglages flottante » en « Capsule heure et réglages » (l'engrenage dans une capsule) et modifie « Page de réglages » (pilules de focus, y compris pour les lignes d'« À propos »). Ce change n'écrit aucun delta sur ces exigences, il y renvoie. **Ordre** : implémentation après le merge de #18 ; archivage de `ui-tvos-polish` avant celui de `self-update` (sinon la référence à « Capsule heure et réglages » ne correspond à aucune exigence archivée).
- **`p2c-upnext`** (spec sur `main`) : il modifie « Page de réglages » (catégorie « Écran d'accueil ») ; ce change ne la modifie pas : aucun recouvrement, aucune contrainte d'ordre.
- **`startup-splash`** (PR #20) : sans recouvrement d'exigences. Après une mise à jour, le redémarrage est un démarrage à froid : si les deux changes sont implémentés, l'écran de démarrage s'affiche alors normalement.

## Impact
- `data/` : client des releases GitHub (`HttpsURLConnection` et `org.json` de la plateforme, aucune nouvelle dépendance), téléchargeur, inspection de l'APK et installateur (`PackageManager`, `PackageInstaller`) derrière des interfaces ; persistance dans le DataStore existant (préférence des préversions, date de la dernière vérification, dernières versions connues).
- `domain/` : conversion tag → `versionCode`, choix de la version proposée, politique d'URL HTTPS, échéance de la vérification quotidienne.
- `ui/settings/AboutContent.kt` et `SettingsViewModel` : lignes de mise à jour ; `ui/home` : pastille sur l'engrenage de la capsule (`HomeViewModel`).
- `AndroidManifest.xml` : deux permissions, un récepteur non exporté pour le statut d'installation, un récepteur `MY_PACKAGE_REPLACED` pour le redémarrage.
- Tests : JUnit et Robolectric avec transport factice ; transport réel testé contre le serveur HTTP du JDK (`com.sun.net.httpserver`), sans nouvelle dépendance de test.
- README : section Installation (mise à jour depuis les réglages) et fonctionnalités.

## Non-goals
- Notes de version dans l'app : non affichées (voir Questions ouvertes).
- Mise à jour entièrement automatique (téléchargement ou installation sans action) : exclue par décision de Sygix.
- Reprise d'un téléchargement interrompu : non prévue en v1 (voir Questions ouvertes).
- Retour à une version antérieure, choix d'une version précise : non prévus.
- Signature détachée ou attestation de provenance des releases : hors périmètre ; l'authenticité repose sur le certificat de signature de l'APK.
- Mise à jour d'autres apps : hors périmètre.

## Questions ouvertes
Les choix non tranchés par Sygix (libellés secondaires, aspect et effacement de la pastille, place de la ligne « Mettre à jour », téléchargement interrompu, sortie des réglages pendant le téléchargement, déclenchement de la vérification automatique, notes de version, message après redémarrage, solution de repli si Android bloque le redémarrage automatique) sont détaillés avec leurs options et le choix provisoire « à confirmer » dans `design.md`, section « Questions ouvertes ».
