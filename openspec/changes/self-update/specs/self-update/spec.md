# Delta self-update

## Purpose
Mise à jour du launcher depuis les releases GitHub publiques du dépôt Sygix/SygixOs : vérification manuelle et quotidienne, choix des préversions, signalement discret, téléchargement vérifié et installation à la demande de l'utilisateur, sans serveur propre ni jeton.

## ADDED Requirements

### Requirement: Source des versions
Le launcher SHALL obtenir les versions publiées en une seule requête à l'API REST publique des releases du dépôt Sygix/SygixOs, en HTTPS, sans jeton ni authentification d'aucune sorte. Chaque requête SHALL avoir un délai de connexion et un délai de lecture bornés. La source SHALL distinguer pour l'appelant : liste obtenue (éventuellement vide), absence de réseau, délai dépassé, limite de requêtes atteinte (avec l'heure à partir de laquelle réessayer quand l'API la fournit), releases inaccessibles (dépôt introuvable ou privé, autre réponse HTTP d'erreur) et réponse illisible ; aucune erreur n'est avalée ni ne provoque de crash.

#### Scenario: chargement
- **WHEN** une vérification est lancée
- **THEN** une seule requête HTTPS est envoyée à l'API des releases du dépôt ; l'état « vérification en cours » est exposé jusqu'à la réponse ou au délai dépassé

#### Scenario: aucune release
- **WHEN** l'API répond avec une liste vide, ou sans aucune release éligible (« Versions comparées »)
- **THEN** la vérification aboutit à « à jour », sans erreur

#### Scenario: pas de réseau ou délai dépassé
- **WHEN** la TV n'a pas de réseau, ou l'API ne répond pas dans le délai
- **THEN** la vérification aboutit à une erreur qui le dit (pas de connexion, ou délai dépassé), sans crash ; une nouvelle vérification reste possible

#### Scenario: limite de requêtes atteinte
- **WHEN** l'API refuse la requête parce que la limite de requêtes non authentifiées est atteinte
- **THEN** la vérification aboutit à une erreur qui l'indique avec l'heure locale à partir de laquelle réessayer quand l'API la fournit ; aucune nouvelle requête automatique n'est envoyée avant cette heure

#### Scenario: releases inaccessibles
- **WHEN** l'API répond « introuvable » (dépôt privé ou supprimé) ou par une autre erreur HTTP
- **THEN** la vérification aboutit à une erreur « releases inaccessibles », distincte d'une absence de réseau, sans crash

#### Scenario: réponse illisible
- **WHEN** la réponse n'est pas un JSON de releases valide
- **THEN** la vérification aboutit à une erreur « réponse illisible » ; les versions connues auparavant sont conservées

#### Scenario: retour au premier plan
- **WHEN** le launcher revient au premier plan
- **THEN** aucune requête n'est envoyée de ce seul fait ; l'état affiché est conservé et recalculé par rapport à la version installée

### Requirement: Versions comparées
Seules les releases publiées (non brouillons) dont le tag suit exactement la forme des tags du dépôt — `vX.Y.Z` ou `vX.Y.Z-alpha.N`, `-beta.N`, `-rc.N` (le point avant N facultatif), avec X ≤ 20, Y et Z ≤ 999, N ≤ 29 — et qui portent un asset `app-release.apk` complètement téléversé, avec une empreinte SHA-256 publiée, SHALL être éligibles ; toute autre release SHALL être ignorée sans erreur. Une release SHALL être une préversion si son tag a un suffixe ou si l'API la marque comme préversion. Chaque tag éligible SHALL être converti en `versionCode` avec la formule du build : major × 10⁸ + minor × 10⁵ + patch × 10² + suffixe (alpha.N → N, beta.N → 30 + N, rc.N → 60 + N, version finale → 99). La version proposée SHALL être, parmi les releases éligibles autorisées par la préférence des préversions (« Préversions »), celle dont le `versionCode` est le plus grand, quelle que soit sa date de publication ; elle n'est proposée que si ce `versionCode` est strictement supérieur à celui de l'app installée. Une version inférieure ou égale à la version installée ne SHALL jamais être proposée.

#### Scenario: version finale après une préversion
- **WHEN** l'app installée est `0.0.1-rc.4` (`versionCode` 164) et la release `v0.0.1` (199) est publiée, préversions désactivées
- **THEN** la version `0.0.1` est proposée

#### Scenario: préversion masquée par défaut
- **WHEN** l'app installée est `0.0.1` (199), la seule release plus récente est `v0.0.2-rc.1` (261) et les préversions sont désactivées
- **THEN** le launcher est « à jour »

#### Scenario: préversion proposée si activée
- **WHEN** l'app installée est `0.0.1` (199), les releases `v0.0.2-rc.1` (261) et `v0.0.2-rc.2` (262) sont publiées et les préversions sont activées
- **THEN** la version `0.0.2-rc.2` est proposée

#### Scenario: préversion installée, canal final
- **WHEN** l'app installée est `0.0.2-rc.1` (261), les préversions sont désactivées et la dernière version finale publiée est `v0.0.1` (199)
- **THEN** le launcher est « à jour » : aucune version inférieure n'est proposée

#### Scenario: ordre par version et non par date
- **WHEN** la release `v0.0.1-rc.5` est publiée après la release `v0.1.0`, préversions activées
- **THEN** la version proposée est `0.1.0`, si elle est supérieure à la version installée

#### Scenario: même version
- **WHEN** la meilleure release éligible a le même `versionCode` que l'app installée
- **THEN** le launcher est « à jour »

#### Scenario: tags non conformes
- **WHEN** des releases ont pour tags `v1.0`, `nightly`, `0.0.3` ou `v0.0.3-rc.30`
- **THEN** elles sont ignorées sans erreur, et le choix se fait parmi les autres releases

#### Scenario: release sans APK vérifiable
- **WHEN** la release la plus récente n'a pas d'asset `app-release.apk`, ou son asset n'est pas complètement téléversé, ou n'a pas d'empreinte SHA-256 publiée
- **THEN** elle est ignorée ; la version proposée est la meilleure des autres releases éligibles, s'il y en a une supérieure à la version installée

#### Scenario: préversion selon l'API
- **WHEN** une release au tag `vX.Y.Z` sans suffixe est marquée préversion par l'API
- **THEN** elle est traitée comme une préversion

### Requirement: Préversions
La catégorie « À propos » SHALL contenir une ligne « Inclure les préversions » avec un interrupteur (style Apple, comme « Apps sources »), désactivé par défaut. OK sur la ligne SHALL basculer l'interrupteur et persister l'état. Le changement SHALL s'appliquer immédiatement à la version proposée, à l'état affiché et à la pastille, à partir des dernières versions connues et sans nouvelle requête ; si aucune vérification n'a encore réussi, l'état affiché ne change pas.

#### Scenario: valeur par défaut
- **WHEN** le launcher est installé pour la première fois ou mis à jour depuis une version sans ce réglage
- **THEN** l'interrupteur est désactivé : seules les versions finales sont proposées

#### Scenario: activation
- **WHEN** l'utilisateur active l'interrupteur alors que la dernière vérification connaît une préversion plus récente que la version installée
- **THEN** cette préversion devient la version proposée, l'état et la pastille sont mis à jour aussitôt, sans requête réseau ; l'état est persisté

#### Scenario: désactivation
- **WHEN** l'utilisateur désactive l'interrupteur alors que la version proposée était une préversion
- **THEN** la version proposée devient la meilleure version finale connue, ou « à jour » s'il n'y en a pas de plus récente ; la pastille disparaît dans ce dernier cas ; aucune requête n'est envoyée

#### Scenario: bascule pendant un téléchargement
- **WHEN** l'utilisateur bascule l'interrupteur pendant le téléchargement ou l'installation d'une version
- **THEN** l'opération en cours n'est pas modifiée ; la nouvelle préférence s'applique à la version proposée une fois l'opération terminée ou abandonnée

### Requirement: Vérification des mises à jour
La catégorie « À propos » SHALL contenir une ligne « Vérifier les mises à jour » dont un texte secondaire indique l'état de la mise à jour : jamais vérifié, vérification en cours, à jour, nouvelle version disponible (avec son numéro et, pour une préversion, la mention de préversion), ou l'erreur de la dernière vérification avec sa cause (« Source des versions »). OK sur la ligne SHALL lancer une vérification ; un nouvel appui pendant une vérification en cours SHALL être sans effet. Une vérification manuelle réussie SHALL mettre à jour les versions connues et la date de dernière vérification utilisée par « Vérification automatique quotidienne ». Quand une version est proposée, une ligne « Mettre à jour » SHALL être affichée dans « À propos » ; elle SHALL être absente sinon.

#### Scenario: jamais vérifié
- **WHEN** l'utilisateur ouvre « À propos » et qu'aucune vérification n'a jamais réussi
- **THEN** la ligne « Vérifier les mises à jour » indique qu'aucune vérification n'a eu lieu ; aucune ligne « Mettre à jour » n'est affichée ; aucune requête n'est envoyée par la seule ouverture de la catégorie

#### Scenario: vérification en cours
- **WHEN** l'utilisateur presse OK sur « Vérifier les mises à jour »
- **THEN** le texte secondaire indique la vérification en cours ; un nouvel appui sur OK n'envoie pas de seconde requête

#### Scenario: à jour
- **WHEN** la vérification ne trouve aucune version proposée
- **THEN** le texte secondaire indique que SygixOs est à jour ; aucune ligne « Mettre à jour » ; aucune pastille

#### Scenario: nouvelle version disponible
- **WHEN** la vérification trouve une version proposée
- **THEN** le texte secondaire nomme cette version (et précise s'il s'agit d'une préversion), la ligne « Mettre à jour » apparaît et la pastille s'affiche (« Pastille de mise à jour ») ; le focus reste sur « Vérifier les mises à jour »

#### Scenario: erreur
- **WHEN** la vérification échoue
- **THEN** le texte secondaire donne la cause en français clair (pas de connexion, délai dépassé, limite atteinte avec l'heure, releases inaccessibles, réponse illisible) ; une version proposée connue auparavant reste proposée avec sa ligne « Mettre à jour » ; OK relance une vérification

#### Scenario: version installée par un autre moyen
- **WHEN** une version égale ou supérieure à la version proposée a été installée autrement (par exemple `adb install`) et que le launcher redémarre
- **THEN** cette version n'est plus proposée : état « à jour » si aucune version connue n'est supérieure, plus de ligne « Mettre à jour » ni de pastille

### Requirement: Vérification automatique quotidienne
Au démarrage à froid du launcher, après l'affichage de l'accueil, le launcher SHALL lancer en arrière-plan une vérification si aucune vérification n'a abouti ou n'a été tentée automatiquement depuis 24 h (ou si la date enregistrée est dans le futur), et seulement si un réseau validé est disponible. Il SHALL y avoir au plus une vérification automatique par période de 24 h. La vérification automatique SHALL être silencieuse : elle ne montre ni message, ni erreur, ni dialogue, ne télécharge et n'installe rien ; son seul effet visible est la pastille quand une version est proposée, et l'état affiché dans « À propos ».

#### Scenario: première vérification
- **WHEN** le launcher démarre à froid, aucun contrôle n'a jamais eu lieu et le réseau est disponible
- **THEN** une vérification est lancée en arrière-plan après l'affichage de l'accueil, sans ralentir ni bloquer l'accueil

#### Scenario: moins de 24 h
- **WHEN** le launcher redémarre à froid 3 h après une vérification réussie ou tentée automatiquement
- **THEN** aucune requête n'est envoyée ; la pastille reflète les versions connues et la version installée

#### Scenario: pas de réseau au démarrage
- **WHEN** le launcher démarre à froid sans réseau validé alors qu'une vérification est due
- **THEN** aucune requête n'est envoyée et aucune tentative n'est comptée ; la vérification sera de nouveau due au prochain démarrage à froid

#### Scenario: échec silencieux
- **WHEN** la vérification automatique échoue (délai, limite, releases inaccessibles, réponse illisible)
- **THEN** aucun message ni pastille n'apparaît de ce fait ; la tentative est comptée ; les versions connues sont conservées ; l'erreur est visible seulement dans « À propos »

#### Scenario: jamais d'installation
- **WHEN** la vérification automatique trouve une version proposée
- **THEN** rien n'est téléchargé ni installé ; seule la pastille signale la version

#### Scenario: horloge déréglée
- **WHEN** la date de la dernière vérification enregistrée est postérieure à l'heure courante
- **THEN** la vérification est considérée comme due

### Requirement: Pastille de mise à jour
Tant qu'une version est proposée (« Versions comparées », avec la préférence des préversions courante), une pastille SHALL être affichée sur l'engrenage de la capsule de l'accueil (« Capsule heure et réglages » de `settings`) et sur la catégorie « À propos » du volet gauche des réglages. La pastille n'est jamais focusable, ne change ni la taille ni la position de la capsule, de l'engrenage ou de la catégorie, ni la navigation D-pad, et suit la capsule dans ses mouvements. Elle SHALL disparaître dès qu'aucune version n'est proposée (version installée à jour, préversions désactivées, vérification qui ne trouve plus rien). La consulter ne la fait pas disparaître.

#### Scenario: version proposée
- **WHEN** une version est proposée
- **THEN** la pastille est visible sur l'engrenage de la capsule et sur la catégorie « À propos » ; le focus, les déplacements D-pad et la taille de la capsule sont identiques à ceux de l'accueil sans pastille

#### Scenario: aucune version proposée
- **WHEN** aucune version n'est proposée
- **THEN** aucune pastille n'est affichée

#### Scenario: consultation
- **WHEN** l'utilisateur ouvre « À propos » puis revient à l'accueil sans mettre à jour
- **THEN** la pastille reste affichée

#### Scenario: au démarrage
- **WHEN** le launcher démarre à froid alors qu'une version proposée est connue d'une vérification antérieure
- **THEN** la pastille est affichée dès l'accueil, sans attendre de nouvelle vérification

#### Scenario: après la mise à jour
- **WHEN** le launcher redémarre après avoir installé la version proposée
- **THEN** la pastille n'est plus affichée

### Requirement: Lignes de mise à jour dans À propos
Les lignes « Vérifier les mises à jour », « Mettre à jour » (si présente) et « Inclure les préversions » SHALL être des lignes focusables d'« À propos », placées avant la liste des licences, avec le focus et les pilules de toute ligne des réglages (« Page de réglages » de `settings`). Le premier élément focusable du volet droit d'« À propos » SHALL être « Vérifier les mises à jour » (focus initial de « Page de réglages »). Haut et bas SHALL parcourir les lignes dans l'ordre affiché puis la liste des licences, sans boucle aux bords ; gauche rend le focus à la catégorie « À propos » ; Retour ferme les réglages (« Page de réglages », scénario « retour »).

#### Scenario: focus initial
- **WHEN** l'utilisateur presse droite depuis la catégorie « À propos »
- **THEN** « Vérifier les mises à jour » prend le focus

#### Scenario: déplacements
- **WHEN** le focus est dans le volet « À propos »
- **THEN** bas descend de « Vérifier les mises à jour » à la ligne suivante dans l'ordre affiché jusqu'à la dernière licence ; haut depuis « Vérifier les mises à jour » et bas depuis la dernière licence ne font rien ; la liste défile pour garder l'élément focusé entièrement visible

#### Scenario: apparition de « Mettre à jour »
- **WHEN** une vérification fait apparaître la ligne « Mettre à jour » alors que le focus est sur une autre ligne
- **THEN** le focus reste sur la même ligne

#### Scenario: disparition de la ligne focusée
- **WHEN** la ligne « Mettre à jour » a le focus et disparaît (préférence des préversions changée, vérification qui ne trouve plus de version)
- **THEN** le focus passe à « Vérifier les mises à jour »

#### Scenario: retour d'un écran système
- **WHEN** l'utilisateur revient d'un écran du système ouvert depuis « Mettre à jour » (autorisation, confirmation d'installation) sans que la mise à jour ait abouti
- **THEN** les réglages sont toujours ouverts sur « À propos » et le focus est sur « Mettre à jour » si elle est encore affichée, sinon sur « Vérifier les mises à jour »

### Requirement: Téléchargement vérifié
OK sur « Mettre à jour » SHALL télécharger l'asset `app-release.apk` de la version proposée en HTTPS dans le cache de l'application, en affichant la progression sur la ligne, après avoir vérifié l'autorisation d'installer (« Autorisation d'installer des applis inconnues ») et l'espace libre. Le fichier téléchargé SHALL n'être installé que si toutes les vérifications réussissent, dans cet ordre : taille égale à celle publiée par l'API, empreinte SHA-256 égale à celle publiée par l'API, nom de paquet `fr.sygix.sygixos`, `versionCode` de l'APK égal à celui calculé depuis le tag et supérieur à celui de l'app installée, ensemble des certificats de signature de l'APK identique à celui de l'app installée. Un échec SHALL supprimer le fichier, n'exécuter ni installer rien, et afficher sa cause. Un appui sur « Mettre à jour » pendant une opération en cours SHALL être sans effet. Tout fichier temporaire de mise à jour SHALL être supprimé après l'installation (réussie ou non), après un abandon, et à chaque démarrage à froid.

#### Scenario: téléchargement et progression
- **WHEN** l'utilisateur presse OK sur « Mettre à jour » avec l'autorisation accordée et assez d'espace
- **THEN** le téléchargement démarre, la ligne affiche la progression en pourcentage, puis l'étape de vérification, puis l'installation

#### Scenario: empreinte différente
- **WHEN** l'empreinte SHA-256 du fichier téléchargé diffère de celle publiée par l'API
- **THEN** le fichier est supprimé, rien n'est installé, la ligne indique un fichier corrompu ; « Mettre à jour » peut être relancé

#### Scenario: taille différente
- **WHEN** le serveur envoie plus ou moins d'octets que la taille publiée
- **THEN** le téléchargement s'arrête au plus tard à la taille publiée dépassée, le fichier est supprimé, rien n'est installé, la ligne indique un fichier corrompu

#### Scenario: certificat différent
- **WHEN** l'APK téléchargé est signé par un autre certificat que l'app installée (par exemple une build de développement signée avec la clé de debug)
- **THEN** le fichier est supprimé, rien n'est installé, la ligne indique que la signature ne correspond pas à l'app installée

#### Scenario: APK incohérent
- **WHEN** l'APK téléchargé n'est pas lisible comme APK, porte un autre nom de paquet, ou un `versionCode` différent de celui du tag ou inférieur ou égal à celui installé
- **THEN** le fichier est supprimé, rien n'est installé, la ligne indique une release incohérente

#### Scenario: téléchargement interrompu
- **WHEN** la connexion est perdue ou aucune donnée n'arrive dans le délai pendant le téléchargement
- **THEN** le fichier partiel est supprimé, rien n'est installé, la ligne indique un téléchargement interrompu ; « Mettre à jour » recommence le téléchargement depuis le début

#### Scenario: sortie des réglages pendant le téléchargement
- **WHEN** l'utilisateur ferme les réglages (Retour) pendant le téléchargement ou la vérification du fichier
- **THEN** le téléchargement est abandonné, le fichier partiel est supprimé, rien n'est installé ; à la réouverture, la version reste proposée

#### Scenario: espace insuffisant
- **WHEN** l'espace libre du cache est inférieur au double de la taille publiée de l'APK
- **THEN** rien n'est téléchargé, la ligne indique un espace insuffisant

#### Scenario: fichiers résiduels
- **WHEN** le launcher démarre à froid et un fichier de mise à jour subsiste dans le cache (processus arrêté pendant un téléchargement)
- **THEN** ce fichier est supprimé

### Requirement: Installation de la mise à jour
Une fois le fichier vérifié, le launcher SHALL l'installer comme mise à jour de lui-même par une session d'installation du système, en demandant qu'aucune action de l'utilisateur ne soit requise quand Android le permet ; sinon le système SHALL afficher sa propre confirmation. Le launcher SHALL distinguer : installation réussie, confirmation refusée par l'utilisateur, échec (avec sa cause). Il SHALL n'installer qu'à la suite d'un appui de l'utilisateur sur « Mettre à jour ».

#### Scenario: installation sans confirmation
- **WHEN** Android permet la mise à jour sans action de l'utilisateur
- **THEN** l'installation se fait sans dialogue et le launcher redémarre seul (« Redémarrage après la mise à jour »)

#### Scenario: confirmation du système
- **WHEN** Android exige une confirmation
- **THEN** l'écran de confirmation du système s'affiche ; s'il est accepté, l'installation se poursuit et le launcher redémarre seul

#### Scenario: confirmation refusée
- **WHEN** l'utilisateur refuse ou quitte la confirmation du système
- **THEN** le fichier est supprimé, la ligne indique que l'installation a été annulée, la version reste proposée et « Mettre à jour » peut être relancé

#### Scenario: échec de l'installation
- **WHEN** le système refuse l'installation (incompatibilité, stockage, autre cause)
- **THEN** le fichier est supprimé, la ligne indique l'échec et sa cause en français clair, sans crash ; la version reste proposée

### Requirement: Autorisation d'installer des applis inconnues
Avant tout téléchargement, si le système n'autorise pas SygixOs à installer des applications, la ligne « Mettre à jour » SHALL l'indiquer et OK SHALL ouvrir l'écran système de cette autorisation pour SygixOs. Si cet écran n'existe pas sur l'appareil, la ligne SHALL indiquer le chemin à suivre dans les paramètres. Au retour de cet écran, l'état de l'autorisation SHALL être relu ; la mise à jour ne démarre que sur un nouvel appui de l'utilisateur.

#### Scenario: autorisation manquante
- **WHEN** l'utilisateur presse OK sur « Mettre à jour » sans l'autorisation
- **THEN** rien n'est téléchargé ; la ligne indique que l'autorisation est requise et un nouvel appui ouvre l'écran système de l'autorisation pour SygixOs

#### Scenario: autorisation accordée
- **WHEN** l'utilisateur accorde l'autorisation puis revient avec Retour
- **THEN** la ligne propose de nouveau « Mettre à jour » sans mention d'autorisation, le focus est sur la ligne ; rien ne démarre sans nouvel appui

#### Scenario: autorisation refusée
- **WHEN** l'utilisateur revient sans accorder l'autorisation
- **THEN** la ligne indique toujours que l'autorisation est requise, sans crash

#### Scenario: écran système absent
- **WHEN** l'écran système de l'autorisation ne peut pas être ouvert sur l'appareil
- **THEN** la ligne indique le chemin dans les paramètres de la TV pour accorder l'autorisation, sans crash

### Requirement: Redémarrage après la mise à jour
Après une installation réussie, le launcher SHALL se rouvrir sur son accueil sans action de l'utilisateur, dans la nouvelle version, comme lors d'un démarrage à froid.

#### Scenario: redémarrage
- **WHEN** l'installation de la mise à jour réussit
- **THEN** l'accueil de SygixOs s'affiche de nouveau sans appui de l'utilisateur, dans la nouvelle version ; « À propos » affiche la nouvelle version et la pastille a disparu

### Requirement: Sécurité et confidentialité des mises à jour
Toute requête de mise à jour SHALL utiliser HTTPS ; toute URL, y compris une redirection, qui n'est pas en HTTPS SHALL être refusée avant tout envoi ou abandonnée. Les requêtes SHALL ne contenir aucune donnée personnelle ni identifiant de l'appareil : ni jeton, ni cookie, ni liste d'apps, ni compte ; seuls des en-têtes techniques (type de réponse attendu, version de l'API, nom et version de SygixOs comme agent) sont envoyés. Aucun jeton ni secret SHALL figurer dans l'APK. Le launcher SHALL respecter la limite de requêtes de l'API non authentifiée : une seule requête par vérification, au plus une vérification automatique par 24 h, aucune nouvelle vérification automatique avant l'heure de réessai donnée par l'API.

#### Scenario: redirection non HTTPS
- **WHEN** le téléchargement est redirigé vers une URL en HTTP
- **THEN** la redirection n'est pas suivie, rien n'est installé, la ligne indique une erreur de téléchargement

#### Scenario: URL d'asset non HTTPS
- **WHEN** l'API publie une URL de téléchargement qui n'est pas en HTTPS
- **THEN** aucune requête n'est envoyée vers cette URL et la release est ignorée

#### Scenario: contenu des requêtes
- **WHEN** une vérification ou un téléchargement est envoyé
- **THEN** la requête ne contient ni en-tête d'autorisation, ni cookie, ni identifiant de l'appareil ou de l'utilisateur
