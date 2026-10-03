# Change : self-update

## Why
SygixOs n'est pas distribué par un store : chaque nouvelle version se télécharge à la main depuis la page Releases puis s'installe par `adb install -r`. Sur une TV, c'est une étape d'ordinateur pour chaque mise à jour, et l'utilisateur ne sait pas qu'une version existe. Sygix veut que le launcher vérifie lui-même les releases GitHub du dépôt et s'installe à jour depuis Réglages > À propos, sans serveur propre ni jeton dans l'APK, et sans jamais lancer une mise à jour sans action de l'utilisateur.

## What Changes
- **Vérification manuelle** : ligne « Vérifier les mises à jour » dans Réglages > À propos ; elle interroge l'API REST publique des releases du dépôt Sygix/SygixOs, sans jeton, et affiche l'état : en cours, à jour, nouvelle version disponible (avec son numéro), ou une erreur claire. L'état affiché est calculé à partir de ce qui est persisté (versions connues, dernier résultat, heure de réessai).
- **Préversions** : interrupteur « Inclure les préversions » dans À propos, toujours désactivé par défaut, même si la version installée est une préversion. Désactivé : seules les versions finales ; activé : les préversions (tags avec suffixe, par exemple `v0.0.1-rc.4`) aussi.
- **Vérification automatique** discrète au démarrage à froid et au retour au premier plan, si la dernière vérification date de plus d'un jour : elle ne fait que signaler (pastille sur l'engrenage de la capsule heure et réglages, et sur la catégorie « À propos »), elle n'installe jamais rien.
- **Pastille** : nouveau composant réutilisable du design system (point bleu de 6 dp), qui servira aussi plus tard aux apps ; elle disparaît seulement quand il n'y a plus rien à installer.
- **Comparaison de versions** robuste : le tag est converti en `versionCode` avec la formule du build (`app/build.gradle.kts`, par exemple `v0.0.1-rc.4` → 164, `v0.0.1` → 199), comparé au `versionCode` installé ; jamais de retour à une version inférieure.
- **« Mettre à jour vers X »** (ligne distincte sous « Vérifier les mises à jour », avec à sa droite un code QR vers les notes de version de la release) : relecture de la release (une requête) pour l'URL et l'empreinte à jour, téléchargement de `app-release.apk`, vérification de la taille, de l'empreinte SHA-256 publiée par l'API (champ `digest`), du paquet, de la version et du certificat de signature (identique à celui de l'app installée) ; installation par session `PackageInstaller`, empreinte recalculée pendant la copie dans la session et fichier supprimé avant la validation. Sans confirmation quand Android le permet ; sinon l'écran du système (confirmation ou autorisation « applis inconnues »), affiché tout de suite si SygixOs est au premier plan, gardé pour le prochain retour sinon.
- **Mise à jour en arrière-plan** : fermer les réglages ou presser Home ne l'interrompt pas ; l'installation suit le téléchargement. SygixOs se relance seul seulement s'il était au premier plan au moment de l'installation ; sinon rien ne surgit et la nouvelle version est là au prochain retour. Aucun message après le redémarrage.
- **Rôle d'écran d'accueil** : l'activité principale déclare `CATEGORY_HOME`, pour que le système relance SygixOs après sa mise à jour quand il est le launcher par défaut. SygixOs ne demande jamais ce rôle et ne fait rien pour devenir launcher par défaut ; effet attendu : Android peut proposer un choix de launcher au prochain appui sur Home, et c'est l'utilisateur qui décide. C'est aussi le premier pas de P5 (remplacement du launcher système) : quand le launcher Google est désactivé, SygixOs devient le seul écran d'accueil possible.
- **Sécurité** : HTTPS uniquement (trafic en clair interdit vers les domaines GitHub dans la configuration réseau), aucune installation si une vérification échoue, fichiers temporaires dans le cache de l'app et nettoyés, aucune donnée personnelle envoyée, limite de 60 requêtes par heure de l'API non authentifiée respectée (heure de réessai persistée, appliquée à la vérification automatique ; une vérification ou une mise à jour demandée à la main envoie toujours sa requête, décision de Sygix), asset limité à 200 Mo, délais totaux bornés.
- Permissions ajoutées : `REQUEST_INSTALL_PACKAGES`, `UPDATE_PACKAGES_WITHOUT_USER_ACTION`, et `ACCESS_NETWORK_STATE` déclarée explicitement (lecture du réseau validé ; déjà présente par media3, déclarée par l'app sur décision de Sygix).

## Capabilities

### New Capabilities
- `self-update` : source des versions, comparaison, préversions, vérifications manuelle et automatique, notes de version, pastille de mise à jour, lignes et navigation D-pad dans À propos, téléchargement vérifié, installation, redémarrage, sécurité et confidentialité, couverture de test de la logique et du transport.

### Modified Capabilities
- `launcher-shell` : ADDED « Pastille » (composant du design system, réutilisable).
- `settings` : MODIFIED « À propos » (renvoi vers les lignes de `self-update` ; le scénario existant « contenu » est repris tel quel, un scénario de renvoi est ajouté).
- `ui-testing` : ADDED « Couverture des mises à jour » (lignes d'À propos, code QR, pastille ; tests Compose à la taille TV). Les tests de logique et de transport sont dans `self-update`, le Purpose d'`ui-testing` ne couvrant que l'interface.

## Dépendances et chevauchements
- **Dépôt public (prérequis du test réel sur la TV seulement)** : le dépôt est encore privé ; l'API publique des releases y répond 404 sans jeton. L'implémentation commence dès le merge de cette spec, avec des tests automatisés contre un faux serveur (transport factice et serveur HTTP local du JDK). La vérification sur la TV contre les vraies releases attend le passage du dépôt en public (tâches 9.x de `tasks.md`) ; d'ici là, sur la TV, la vérification affiche l'état d'erreur « releases inaccessibles » prévu par la spec pour une source absente. Aucune exigence n'est propre au dépôt privé.
- **`ui-tvos-polish`** (PR #18, non mergée) : il renomme « Icône réglages flottante » en « Capsule heure et réglages » et modifie « Page de réglages » (pilules de focus, y compris pour les lignes d'« À propos »). Ce change n'écrit aucun delta sur ces exigences, il y renvoie. « À propos » n'est modifiée ni par `ui-tvos-polish` ni par `p2c-upnext`. **Ordre** : implémentation après le merge de #18 ; archivage de `ui-tvos-polish` avant celui de `self-update` (la pastille renvoie à « Capsule heure et réglages »).
- **`p2c-upnext`** (spec sur `main`) : il modifie « Page de réglages », pas « À propos » ; aucun recouvrement, aucune contrainte d'ordre.
- **`startup-splash`** (PR #20) : sans recouvrement d'exigences (les deux ajoutent des exigences différentes à `launcher-shell`). Après une mise à jour au premier plan, la relance est un démarrage à froid : l'écran de démarrage s'affiche si les deux changes sont implémentés.
- **Publication** : `release.yml` crée la release en brouillon, téléverse l'APK puis publie ; l'API non authentifiée ne voit pas les brouillons, donc une release n'apparaît qu'avec son asset complet.

## Impact
- `data/` : client des releases GitHub (`HttpsURLConnection` et `org.json` de la plateforme), téléchargeur, inspection de l'APK et installateur (`PackageManager`, `PackageInstaller`) derrière des interfaces ; persistance dans le DataStore existant (préférence des préversions, date et résultat de la dernière vérification, heure de réessai, versions connues).
- `domain/` : conversion tag → `versionCode`, choix de la version proposée, politique d'URL HTTPS, échéance de la vérification, état affiché.
- `core/designsystem` : composant pastille, jeton de couleur.
- `ui/settings/AboutContent.kt`, `SettingsViewModel` : lignes de mise à jour, code QR ; `ui/home` : pastille sur l'engrenage (`HomeViewModel`).
- Nouvelle dépendance : `com.google.zxing:core` (Apache-2.0) pour encoder le code QR (justification dans `design.md`).
- `AndroidManifest.xml` : deux permissions, filtre `MAIN` + `HOME` + `DEFAULT` sur `MainActivity`, récepteur non exporté du statut d'installation, récepteur `MY_PACKAGE_REPLACED` ; `network_security_config.xml` : domaines GitHub sans trafic en clair.
- README : mise à jour depuis les réglages, autorisation « applis inconnues », effet de `CATEGORY_HOME` (choix de launcher possible au prochain appui sur Home) et lien avec P5.

## Non-goals
- Mise à jour lancée sans action de l'utilisateur : exclue par décision de Sygix (la vérification automatique ne fait que signaler).
- Reprise d'un téléchargement interrompu : non prévue (relance manuelle depuis zéro, décision de Sygix).
- Retour à une version antérieure, choix d'une version précise : non prévus.
- Devenir launcher par défaut : SygixOs ne le demande jamais ; le remplacement du launcher système reste P5.
- Notes de version affichées en texte dans l'app : remplacées par le code QR vers la page de la release.
- Signature détachée ou attestation de provenance des releases : hors périmètre ; l'authenticité repose sur le certificat de signature de l'APK.

## Questions ouvertes
Aucune : Sygix a tranché les libellés, la pastille, sa disparition, la place de « Mettre à jour vers X », le téléchargement interrompu, la mise à jour en arrière-plan, le déclenchement de la vérification automatique, les notes de version en code QR, l'absence de message après redémarrage, `CATEGORY_HOME`, la build de développement et la valeur par défaut des préversions. Les comportements d'Android encore non constatés sur la TV (mise à jour sans confirmation, guidage vers l'autorisation, relance) sont vérifiés en tâche 1.1.
