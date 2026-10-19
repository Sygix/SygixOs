# self-update Specification

## Purpose
Mise à jour du launcher depuis les releases GitHub publiques du dépôt Sygix/SygixOs : vérification manuelle et quotidienne, choix des préversions, signalement discret, téléchargement vérifié et installation à la demande de l'utilisateur, sans serveur propre ni jeton.

## Requirements

### Requirement: Source des versions
Une seule requête REST HTTPS aux releases publiques Sygix/SygixOs, sans authentification, SHALL obtenir les versions, avec délais de connexion, lecture et total bornés. La source SHALL distinguer les cas listés dans les scénarios ci-dessous, sans erreur avalée ni crash. Une limite SHALL persister l'heure de réessai fournie, sinon une heure plus tard ; aucune vérification automatique avant, même après redémarrage ; manuelle et relecture envoient toujours leur requête.

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
- **WHEN** l'API refuse la requête parce que la limite de requêtes non authentifiées est atteinte, avec une heure de réessai
- **THEN** la vérification aboutit à une erreur qui l'indique avec l'heure locale de réessai ; cette heure est persistée

#### Scenario: vérification automatique avant l'heure de réessai
- **WHEN** une vérification automatique est due avant l'heure de réessai persistée, même après un redémarrage
- **THEN** aucune requête n'est envoyée ; l'état reste celui de la dernière vérification

#### Scenario: vérification manuelle avant l'heure de réessai
- **WHEN** l'utilisateur presse OK sur « Vérifier les mises à jour », ou sur « Mettre à jour vers X », avant l'heure de réessai persistée
- **THEN** la requête est envoyée ; si l'API refuse encore, l'erreur de limite et la nouvelle heure de réessai sont affichées et persistées

#### Scenario: connexion sécurisée impossible
- **WHEN** la connexion TLS échoue ou le certificat est refusé (par exemple parce que la date de la TV est fausse)
- **THEN** la vérification aboutit à une erreur « connexion sécurisée impossible » qui invite à vérifier la date et l'heure de la TV, distincte d'une absence de réseau

#### Scenario: releases inaccessibles
- **WHEN** l'API répond « introuvable » (dépôt privé ou supprimé) ou par une autre erreur HTTP
- **THEN** la vérification aboutit à une erreur « releases inaccessibles », distincte d'une absence de réseau, sans crash

#### Scenario: réponse illisible
- **WHEN** la réponse n'est pas un JSON de releases valide
- **THEN** la vérification aboutit à une erreur « réponse illisible » ; les versions connues auparavant sont conservées

**Détails.** — Heure de réessai des versions : Une limite API SHALL persister l'heure de réessai fournie, sinon une heure plus tard. Aucune vérification automatique ne SHALL partir avant, même après redémarrage. Vérification manuelle et relecture de « Mettre à jour vers X » SHALL toujours envoyer leur requête. Cas « limite sans heure publiée » : quand l'API limite la requête sans fournir d'heure, le prochain réessai automatique est bloqué pendant une heure ; les scénarios de vérification avant l'heure de « Source des versions » restent applicables.

### Requirement: Versions comparées
Parmi les releases non brouillons éligibles (asset `app-release.apk` complet ≤ 200 Mo, SHA-256 publiée et URL HTTPS ; tags et bornes conformes selon les scénarios ci-dessous), la plus grande valeur de `versionCode` SHALL être proposée si strictement supérieure à l'installée, sans tenir compte de la date. Le `versionCode` SHALL suivre la formule du build (major × 10⁸ + minor × 10⁵ + patch × 10² + suffixe). Suffixe ou marque API SHALL classer une préversion.

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
- **WHEN** la release la plus récente n'a pas d'asset `app-release.apk`, ou son asset n'est pas complètement téléversé, dépasse 200 Mo, n'a pas d'empreinte SHA-256 publiée ou pas d'URL HTTPS
- **THEN** elle est ignorée ; la version proposée est la meilleure des autres releases éligibles, s'il y en a une supérieure à la version installée

#### Scenario: préversion selon l'API
- **WHEN** une release au tag `vX.Y.Z` sans suffixe est marquée préversion par l'API
- **THEN** elle est traitée comme une préversion

**Détails.** — Éligibilité des releases : Seules les releases non brouillons SHALL être éligibles : tag exact `vX.Y.Z` ou `vX.Y.Z-alpha.N`, `-beta.N`, `-rc.N` (point avant N facultatif), X ≤ 20, Y/Z ≤ 999, N ≤ 29 ; asset `app-release.apk` totalement téléversé, taille publiée ≤ 200 Mo, SHA-256 publié, téléchargement HTTPS. Les autres SHALL être ignorées sans erreur. Cas « tag ou asset non conforme » : quand une release est examinée, « tags non conformes » et « release sans APK vérifiable » de « Versions comparées » déterminent son admissibilité. — Code des versions : Un tag éligible SHALL devenir `versionCode` selon la formule du build : major × 10⁸ + minor × 10⁵ + patch × 10² + suffixe ; alpha.N → N, beta.N → 30 + N, rc.N → 60 + N, finale → 99. Cas « priorité des versions » : quand des tags éligibles sont comparés, les scénarios « version finale après une préversion », « ordre par version et non par date » et « même version » de « Versions comparées » suivent ces codes.

### Requirement: Préversions
« À propos » SHALL proposer « Inclure les préversions », switch Apple comme Apps sources, désactivé par défaut même avec une préversion installée. Une release SHALL être une préversion si son tag a un suffixe ou si l'API la marque comme telle. OK SHALL basculer et persister ; version proposée, état et pastille SHALL être recalculés aussitôt depuis les versions connues, sans requête. Sans vérification réussie, l'état affiché ne change pas.

#### Scenario: valeur par défaut
- **WHEN** le launcher est installé pour la première fois ou mis à jour depuis une version sans ce réglage
- **THEN** l'interrupteur est désactivé : seules les versions finales sont proposées

#### Scenario: préversion installée
- **WHEN** la version installée est une préversion (par exemple `0.0.1-rc.4`) et que l'utilisateur n'a jamais touché l'interrupteur
- **THEN** l'interrupteur est désactivé : seules les versions finales supérieures à la version installée sont proposées

#### Scenario: activation
- **WHEN** l'utilisateur active l'interrupteur alors que la dernière vérification connaît une préversion plus récente que la version installée
- **THEN** cette préversion devient la version proposée, l'état et la pastille sont mis à jour aussitôt, sans requête réseau ; l'état est persisté

#### Scenario: désactivation
- **WHEN** l'utilisateur désactive l'interrupteur alors que la version proposée était une préversion
- **THEN** la version proposée devient la meilleure version finale connue, ou « à jour » s'il n'y en a pas de plus récente ; la pastille disparaît dans ce dernier cas ; aucune requête n'est envoyée

#### Scenario: bascule pendant une mise à jour
- **WHEN** l'utilisateur bascule l'interrupteur pendant le téléchargement ou l'installation d'une version
- **THEN** l'opération en cours n'est pas modifiée ; la nouvelle préférence s'applique à la version proposée une fois l'opération terminée ou abandonnée

### Requirement: Vérification des mises à jour
Dans À propos, OK sur « Vérifier les mises à jour » SHALL vérifier, sans effet si déjà en cours. Le secondaire SHALL afficher en cours, sinon la version proposée (numéro et préversion), sinon le dernier résultat manuel ou automatique, sinon « jamais vérifié » ; résultat et date terminés sont persistés. « Mettre à jour vers X » SHALL apparaître juste dessous seulement si une version est proposée. Une vérification manuelle efface les messages d'échec et de retrait ; l'automatique les conserve.

#### Scenario: jamais vérifié
- **WHEN** l'utilisateur ouvre « À propos » et qu'aucune vérification n'a jamais abouti ni échoué
- **THEN** la ligne « Vérifier les mises à jour » indique qu'aucune vérification n'a eu lieu ; aucune ligne « Mettre à jour vers X » ; aucune requête n'est envoyée par la seule ouverture de la catégorie

#### Scenario: vérification en cours
- **WHEN** l'utilisateur presse OK sur « Vérifier les mises à jour »
- **THEN** le texte secondaire indique la vérification en cours ; un nouvel appui sur OK n'envoie pas de seconde requête

#### Scenario: à jour
- **WHEN** la vérification ne trouve aucune version proposée
- **THEN** le texte secondaire indique que SygixOs est à jour ; aucune ligne « Mettre à jour vers X » ; aucune pastille

#### Scenario: nouvelle version disponible
- **WHEN** la vérification trouve une version proposée
- **THEN** le texte secondaire nomme cette version (et précise s'il s'agit d'une préversion), la ligne « Mettre à jour vers X » apparaît juste dessous et la pastille s'affiche (« Pastille de mise à jour ») ; le focus reste sur « Vérifier les mises à jour »

#### Scenario: erreur
- **WHEN** la vérification échoue
- **THEN** le texte secondaire donne la cause en français clair (pas de connexion, connexion sécurisée impossible, délai dépassé, limite atteinte avec l'heure, releases inaccessibles, réponse illisible), sauf si une version connue reste proposée : le texte nomme alors cette version et la ligne « Mettre à jour vers X » reste affichée ; OK relance une vérification

#### Scenario: état après un redémarrage
- **WHEN** le launcher redémarre à froid et que l'utilisateur ouvre « À propos » sans nouvelle vérification
- **THEN** le texte est calculé à partir de ce qui est persisté, selon l'ordre de priorité de l'exigence : version proposée, sinon dernier résultat (à jour ou erreur), sinon « jamais vérifié »

#### Scenario: après un échec automatique
- **WHEN** la dernière vérification, automatique, a échoué et qu'aucune version n'est proposée
- **THEN** « À propos » affiche cette erreur et sa cause, et non « jamais vérifié »

#### Scenario: version installée par un autre moyen
- **WHEN** une version égale ou supérieure à la version proposée a été installée autrement (par exemple `adb install`) et que le launcher redémarre
- **THEN** cette version n'est plus proposée : plus de ligne « Mettre à jour vers X » ni de pastille, et le texte suit l'ordre de priorité

**Détails.** — État affiché de la vérification : Le secondaire SHALL afficher vérification en cours, sinon calculer depuis versions/résultat/réessai persistés : version proposée avec numéro et mention préversion ; sinon dernier résultat manuel ou automatique, à jour ou erreur avec cause ; sinon « jamais vérifié » si aucune vérification terminée. Une erreur SHALL ne pas masquer une version encore proposée. Cas « état reconstruit » : quand À propos s'ouvre sans requête nouvelle, « état après un redémarrage », « après un échec automatique » et « version installée par un autre moyen » de « Vérification des mises à jour » s'appliquent. — Nettoyage des messages de mise à jour : Une vérification manuelle SHALL effacer la cause d'échec sur « Mettre à jour vers X » et le message de version retirée ; une vérification automatique SHALL les conserver. Cas « origine de la vérification » : quand une nouvelle vérification est lancée après un échec ou un retrait, les messages sont effacés seulement par l'action manuelle, sans les confondre avec le résultat persisté de la vérification.

### Requirement: Notes de version
Version proposée, À propos SHALL afficher à droite un QR non focusable vers la page HTTPS de github.com, légende « Notes de version », aligné en haut sur « Mettre à jour vers X ». Lisible depuis le canapé (modules sombres, fond clair, marge de silence), il SHALL ne changer ni hauteur ni position des lignes et ne recouvrir aucun texte, erreur comprise. URL non conforme ou aucune version : pas de QR.

#### Scenario: version proposée
- **WHEN** une version est proposée
- **THEN** le code QR de la page de sa release et sa légende sont affichés à droite, alignés en haut sur la ligne « Mettre à jour vers X » ; « Inclure les préversions » reste juste sous les lignes qui le précèdent ; le focus et la navigation D-pad sont les mêmes que sans code QR

#### Scenario: aucune version proposée
- **WHEN** aucune version n'est proposée
- **THEN** aucun code QR n'est affiché

#### Scenario: URL de release invalide
- **WHEN** l'URL de la page de la release publiée par l'API n'est pas une URL HTTPS de github.com
- **THEN** aucun code QR n'est affiché, sans erreur ; la ligne « Mettre à jour vers X » reste utilisable

### Requirement: Vérification automatique quotidienne
Après l'accueil au démarrage à froid et au retour au premier plan, une vérification de fond SHALL partir si la dernière, réussie ou non/manuelle ou automatique, date de plus de 24 h, manque ou est future, seulement avec réseau validé et heure de réessai passée. Silencieuse, elle ne SHALL montrer ni message/dialogue, ni télécharger/installer : seuls pastille et état d'À propos changent.

#### Scenario: première vérification
- **WHEN** le launcher démarre à froid, aucune vérification n'a jamais eu lieu et le réseau est disponible
- **THEN** une vérification est lancée en arrière-plan après l'affichage de l'accueil, sans ralentir ni bloquer l'accueil

#### Scenario: retour au premier plan
- **WHEN** le launcher revient au premier plan 26 h après la dernière vérification, le processus n'ayant pas été redémarré
- **THEN** une vérification est lancée en arrière-plan

#### Scenario: moins de 24 h
- **WHEN** le launcher démarre à froid ou revient au premier plan 3 h après la dernière vérification
- **THEN** aucune requête n'est envoyée ; la pastille reflète les versions connues et la version installée

#### Scenario: pas de réseau
- **WHEN** une vérification est due mais qu'aucun réseau validé n'est disponible
- **THEN** aucune requête n'est envoyée et aucune date n'est enregistrée ; la vérification reste due au prochain démarrage à froid ou retour au premier plan

#### Scenario: échec silencieux
- **WHEN** la vérification automatique échoue (délai, limite, releases inaccessibles, réponse illisible)
- **THEN** aucun message ni dialogue n'apparaît ; la date et le résultat (l'erreur) sont persistés ; les versions connues sont conservées ; l'erreur est visible dans « À propos » selon « Vérification des mises à jour »

#### Scenario: jamais d'installation
- **WHEN** la vérification automatique trouve une version proposée
- **THEN** rien n'est téléchargé ni installé ; seule la pastille signale la version

#### Scenario: horloge déréglée
- **WHEN** la date de la dernière vérification enregistrée est postérieure à l'heure courante
- **THEN** la vérification est considérée comme due

### Requirement: Pastille de mise à jour
Version proposée selon « Versions comparées » et la préférence courante, « Pastille » de launcher-shell SHALL apparaître sur l'engrenage de la capsule et la catégorie « À propos ». Elle SHALL disparaître seulement si la version est installée ou plus rien n'est proposé (préversions désactivées ou vérification sans version supérieure), jamais à la consultation.

#### Scenario: version proposée
- **WHEN** une version est proposée
- **THEN** la pastille est visible sur l'engrenage de la capsule et sur la catégorie « À propos », avec le comportement de « Pastille » (`launcher-shell`)

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
Les lignes focusables SHALL précéder les licences : vérifier, mettre à jour si proposée, version retirée si présente, préversions, avec pilules de « Page de réglages ». Focus initial SHALL être « Vérifier les mises à jour » ; haut/bas parcourent lignes puis licences sans boucle, gauche rend la catégorie À propos, Retour ferme les réglages. Les scénarios ci-dessous SHALL régir les changements de focus.

#### Scenario: focus initial
- **WHEN** l'utilisateur presse droite depuis la catégorie « À propos »
- **THEN** « Vérifier les mises à jour » prend le focus

#### Scenario: déplacements
- **WHEN** le focus est dans le volet « À propos »
- **THEN** bas descend de « Vérifier les mises à jour » à la ligne suivante dans l'ordre affiché jusqu'à la dernière licence ; haut depuis « Vérifier les mises à jour » et bas depuis la dernière licence ne font rien ; la liste défile pour garder l'élément focusé entièrement visible

#### Scenario: apparition de « Mettre à jour vers X »
- **WHEN** une vérification fait apparaître la ligne « Mettre à jour vers X » alors que le focus est sur une autre ligne
- **THEN** le focus reste sur la même ligne

#### Scenario: disparition de la ligne focusée
- **WHEN** la ligne « Mettre à jour vers X » a le focus et disparaît (préférence des préversions changée, vérification qui ne trouve plus de version)
- **THEN** le focus passe à « Vérifier les mises à jour »

#### Scenario: retour d'un écran système
- **WHEN** l'utilisateur revient d'un écran du système ouvert pour la mise à jour (autorisation, confirmation d'installation) sans que la mise à jour ait abouti
- **THEN** les réglages sont toujours ouverts sur « À propos » et le focus est sur « Mettre à jour vers X » si elle est encore affichée, sinon sur « Vérifier les mises à jour »

### Requirement: Téléchargement vérifié
OK sur « Mettre à jour vers X » SHALL relire la release (une requête) puis télécharger `app-release.apk` en HTTPS au cache avec progression et délai total borné, même réglages fermés ou SygixOs quitté. L'installation SHALL n'être lancée qu'après les contrôles ordonnés des scénarios ci-dessous (taille, SHA-256, paquet, `versionCode` égal au tag et supérieur à l'installé, certificats) ; tout échec supprime le fichier. Profil `app-release.dm` optionnel et temporaires : scénarios ci-dessous.

#### Scenario: téléchargement et progression
- **WHEN** l'utilisateur presse OK sur « Mettre à jour vers X » avec assez d'espace
- **THEN** la release est relue, le téléchargement démarre, la ligne affiche la progression en pourcentage, puis l'étape de vérification, puis l'installation

#### Scenario: release relue modifiée
- **WHEN** la relecture de la release renvoie une autre URL de téléchargement ou une autre empreinte que celles connues
- **THEN** le téléchargement utilise l'URL et l'empreinte relues

#### Scenario: release retirée
- **WHEN** la relecture de la release répond « introuvable », ou la release n'est plus éligible (« Versions comparées »)
- **THEN** rien n'est téléchargé ; cette version est retirée des versions connues, la version proposée et la pastille sont recalculées ; une ligne « Mettre à jour vers X » propre à la version retirée affiche « Cette version n'est plus disponible » jusqu'à la vérification manuelle suivante, sans code QR ni pastille, et OK y est sans effet ; si une autre version Y est proposée, « Mettre à jour vers Y » est affichée aussi, juste sous « Vérifier les mises à jour »

#### Scenario: relecture impossible
- **WHEN** la relecture échoue (pas de réseau, délai, limite atteinte, réponse illisible)
- **THEN** rien n'est téléchargé ; la ligne indique la cause comme pour une vérification ; la version reste proposée

#### Scenario: asset introuvable au téléchargement
- **WHEN** le serveur répond 404 ou 410 à la requête de téléchargement de l'APK
- **THEN** rien n'est installé, aucun fichier ne subsiste, la ligne indique que le fichier de la version est introuvable ; la version reste proposée jusqu'à la prochaine vérification

#### Scenario: empreinte différente
- **WHEN** l'empreinte SHA-256 du fichier téléchargé diffère de celle publiée par l'API
- **THEN** le fichier est supprimé, rien n'est installé, la ligne indique un fichier corrompu ; « Mettre à jour vers X » peut être relancé

#### Scenario: taille différente
- **WHEN** le serveur envoie plus ou moins d'octets que la taille publiée
- **THEN** le téléchargement s'arrête au plus tard dès la taille publiée dépassée, le fichier est supprimé, rien n'est installé, la ligne indique un fichier corrompu

#### Scenario: certificat différent
- **WHEN** l'APK téléchargé est signé par un autre certificat que l'app installée, par exemple parce que l'app installée est une build de développement signée avec la clé de debug
- **THEN** le fichier est supprimé, rien n'est installé, la ligne indique clairement que la signature de la version publiée ne correspond pas à celle de l'app installée

#### Scenario: APK incohérent
- **WHEN** l'APK téléchargé n'est pas lisible comme APK, porte un autre nom de paquet, ou un `versionCode` différent de celui du tag ou inférieur ou égal à celui installé
- **THEN** le fichier est supprimé, rien n'est installé, la ligne indique une release incohérente

#### Scenario: téléchargement interrompu
- **WHEN** la connexion est perdue ou aucune donnée n'arrive dans le délai pendant le téléchargement
- **THEN** le fichier partiel est supprimé, rien n'est installé, la ligne indique un téléchargement interrompu ; un nouvel appui sur « Mettre à jour vers X » recommence depuis le début

#### Scenario: réglages fermés pendant le téléchargement
- **WHEN** l'utilisateur ferme les réglages, presse Home ou ouvre une autre app pendant le téléchargement
- **THEN** le téléchargement continue, puis les vérifications et l'installation s'enchaînent dès sa fin ; à la réouverture d'« À propos » pendant l'opération, la ligne montre l'étape en cours

#### Scenario: espace insuffisant
- **WHEN** l'espace libre du cache est inférieur au double de la taille publiée de l'APK
- **THEN** rien n'est téléchargé, la ligne indique un espace insuffisant

#### Scenario: fichiers résiduels
- **WHEN** le launcher démarre à froid et un fichier de mise à jour subsiste dans le cache (processus arrêté pendant un téléchargement)
- **THEN** ce fichier est supprimé ; la version reste proposée

#### Scenario: profil de démarrage vérifié
- **WHEN** la release relue publie `app-release.dm` avec une empreinte SHA-256 et le fichier reçu a la taille et l'empreinte publiées et contient exactement `primary.prof` et `primary.profm`
- **THEN** le profil est gardé pour l'installation (« Installation de la mise à jour ») ; la ligne passe par les mêmes étapes qu'une mise à jour sans profil

#### Scenario: profil absent ou sans empreinte
- **WHEN** la release relue ne publie pas `app-release.dm`, ou le publie sans empreinte SHA-256 valide, au-delà de 16 Mo, incomplet ou avec une URL non HTTPS
- **THEN** le profil n'est pas téléchargé ; l'APK vérifié est installé seul

#### Scenario: profil refusé
- **WHEN** le téléchargement du profil échoue (introuvable, connexion coupée, plus de 10 s, redirection non HTTPS) ou que le fichier reçu n'a pas la taille ou l'empreinte publiées ou n'est pas une archive de `primary.prof` et `primary.profm`
- **THEN** le profil est abandonné sans message, son fichier temporaire est supprimé, et l'APK vérifié est installé seul, au plus 10 s après la fin des vérifications de l'APK

**Détails.** — Vérifications de l'APK : Avant installation, les contrôles SHALL réussir dans cet ordre : taille API, SHA-256 API, paquet `fr.sygix.sygixos`, `versionCode` égal au tag et supérieur à l'installé, ensemble des certificats identique à l'installé. Tout échec SHALL supprimer le fichier, ne rien exécuter/installer et afficher sa cause dans À propos. Cas « fichier non conforme » : quand une vérification du fichier échoue, « empreinte différente », « taille différente », « certificat différent » et « APK incohérent » de « Téléchargement vérifié » empêchent l'installation dans l'ordre prescrit. — Continuité et fichiers de mise à jour : Le téléchargement SHALL continuer réglages fermés ou SygixOs quitté par Home/autre app, puis installer dès sa fin. Tout temporaire SHALL être supprimé après transmission à l'installation, après échec et au démarrage à froid. Cas « sortie ou fichier résiduel » : quand l'utilisateur quitte SygixOs ou le processus redémarre, « réglages fermés pendant le téléchargement » et « fichiers résiduels » de « Téléchargement vérifié » préservent la continuité ou nettoient le cache. — Profil de mise à jour téléchargé : Après APK vérifié, `app-release.dm` complet, ≤ 16 Mo, SHA-256 publié valide et URL HTTPS SHALL être téléchargé au cache temporaire avec total ≤ 10 s. Seuls taille/SHA-256 conformes et archive contenant exactement `primary.prof` et `primary.profm` non vides SHALL être gardés. Sans empreinte valide, aucun téléchargement/installation du profil ; son temporaire est toujours supprimé. Cas « profil absent ou refusé » : quand le profil manque, est invalide ou son téléchargement échoue, les scénarios de profil de « Téléchargement vérifié » installent l'APK seul sans message ni blocage ; aucun retard au-delà du délai de 10 s.

### Requirement: Installation de la mise à jour
Après appui utilisateur sur « Mettre à jour vers X » seulement, l'APK vérifié SHALL être installé comme mise à jour de soi par une session Android, sans action supplémentaire si permis. Copie, action Android, statuts et profil SHALL suivre les scénarios ci-dessous : SHA-256 recalculé, fichier supprimé avant validation, action gardée jusqu'au retour au premier plan, succès/refus/échec distincts, autres sessions ignorées, échec avec profil relancé une fois sans profil.

#### Scenario: installation sans confirmation
- **WHEN** Android permet la mise à jour sans action de l'utilisateur
- **THEN** l'installation se fait sans dialogue (« Redémarrage après la mise à jour » pour la suite)

#### Scenario: copie altérée
- **WHEN** l'empreinte recalculée pendant la copie dans la session diffère de l'empreinte publiée
- **THEN** la session est abandonnée sans être validée, le fichier est supprimé, rien n'est installé, la ligne indique un fichier corrompu

#### Scenario: action requise, SygixOs au premier plan
- **WHEN** Android exige une confirmation ou l'autorisation d'installer des applis inconnues alors que SygixOs est au premier plan
- **THEN** l'écran du système s'affiche et guide l'utilisateur ; s'il accepte, l'installation se poursuit

#### Scenario: action requise, SygixOs en arrière-plan
- **WHEN** Android exige une action de l'utilisateur alors qu'une autre app est au premier plan
- **THEN** rien ne s'affiche par-dessus cette app ; au prochain retour sur SygixOs, l'écran du système s'affiche ; si le processus de SygixOs a été arrêté entre-temps, la demande est perdue, la session est abandonnée et la version reste proposée

#### Scenario: action refusée
- **WHEN** l'utilisateur refuse ou quitte l'écran du système, que le système rende un statut d'abandon ou qu'aucun statut n'arrive avant son retour sur SygixOs
- **THEN** la ligne indique que l'installation a été annulée, la version reste proposée et « Mettre à jour vers X » peut être relancé

#### Scenario: écran du système indisponible
- **WHEN** l'écran du système demandé par Android ne peut pas être ouvert sur l'appareil
- **THEN** la session est abandonnée, la ligne indique où accorder l'autorisation dans les paramètres de la TV, sans crash

#### Scenario: échec de l'installation
- **WHEN** le système refuse l'installation (incompatibilité, stockage, autre cause)
- **THEN** aucun fichier ne subsiste, la ligne indique l'échec et sa cause en français clair, sans crash ; la version reste proposée

#### Scenario: profil installé avec l'APK
- **WHEN** un profil vérifié est disponible et la session l'accepte
- **THEN** la session contient l'APK et le profil, est validée une seule fois, et Android compile l'app avec son profil dès l'installation

#### Scenario: échec de la session avec le profil
- **WHEN** Android rend un statut d'échec pour la session qui portait le profil
- **THEN** la mise à jour est relancée une seule fois sans profil (relecture, téléchargement et vérifications de l'APK), sans message ; si cette seconde session réussit, la mise à jour est installée ; si elle échoue aussi, la ligne indique l'échec et sa cause, la version reste proposée et aucune autre tentative n'est lancée

#### Scenario: profil refusé par la session
- **WHEN** l'écriture du profil dans la session échoue
- **THEN** cette session est abandonnée sans être validée, une nouvelle session reçoit l'APK seul (empreinte recalculée pendant la copie) et est validée ; aucun fichier ne subsiste

**Détails.** — Copie dans la session : Le contenu de session SHALL être celui vérifié : SHA-256 recalculé pendant copie et comparé au publié, fichier supprimé avant validation. Divergence SHALL abandonner la session non validée selon « copie altérée » de « Installation de la mise à jour ». Cas « copie corrompue » : quand les octets changent entre vérification et copie, la session n'est pas validée, le fichier est supprimé et l'état signale le fichier corrompu. — Action d'installation requise : Une action Android requise SHALL s'afficher seulement SygixOs au premier plan ; sinon elle est gardée jusqu'au retour, jamais au-dessus d'une autre app. Retour sans statut et sans installation en cours SHALL abandonner la session comme refus utilisateur. Cas « demande différée ou quittée » : quand confirmation ou autorisation d'applis inconnues est requise, les scénarios d'action au premier/arrière-plan et « action refusée » de « Installation de la mise à jour » s'appliquent, y compris abandon après arrêt du processus. — Profil dans la session : Un profil vérifié SHALL être copié exactement avec l'APK dans la même session avant validation, `base.dm` pour `base.apk`. Refus à l'écriture SHALL abandonner la session non validée puis installer l'APK seul dans une nouvelle session, mêmes règles. Un échec Android après validation avec profil SHALL suivre « Reprise d'installation sans profil ». Cas « profil accepté ou refusé avant validation » : quand une session reçoit le profil, « profil installé avec l'APK » ou « profil refusé par la session » de « Installation de la mise à jour » s'applique sans valider une session ayant refusé son profil. — Reprise d'installation sans profil : Après échec d'une session validée avec profil, une seule relance automatique SHALL relire la release, retélécharger l'APK supprimé et refaire les contrôles sans profil. Seule l'étape affichée change ; premier plan/relance gardent leurs règles. Nouvel échec SHALL être normal sans autre essai ; échec avant validation (relecture/téléchargement/contrôles) SHALL effacer la demande de relance. Cas « seconde tentative échouée » : quand la relance sans profil échoue à son tour, « échec de la session avec le profil » de « Installation de la mise à jour » expose l'erreur et conserve la version proposée sans troisième tentative.

### Requirement: Redémarrage après la mise à jour
Après succès validé au premier plan, le launcher SHALL demander son accueil dans la nouvelle version ; hors premier plan, il ne SHALL ni rouvrir ni afficher. La demande SHALL viser la version installée et être effacée après refus, échec ou abandon. La politique constructeur peut bloquer la relance : limitation acceptée, la version restant présente au retour manuel. L'activité SHALL rester candidate HOME, sans que la mise à jour demande ce rôle ; seul l'étape 3 du parcours guidé le fait.

#### Scenario: installation au premier plan
- **WHEN** l'installation réussit alors que SygixOs était au premier plan
- **THEN** l'accueil de SygixOs s'affiche de nouveau dans la nouvelle version si le système autorise la relance ; si la politique constructeur la bloque, l'utilisateur rouvre SygixOs manuellement ; « À propos » affiche la nouvelle version et la pastille a disparu ; aucun message n'annonce la mise à jour

#### Scenario: installation en arrière-plan
- **WHEN** l'installation réussit alors qu'une autre app est au premier plan
- **THEN** SygixOs ne s'ouvre pas et rien ne s'affiche par-dessus l'app ; au prochain retour sur SygixOs, la nouvelle version est installée et c'est un démarrage à froid

#### Scenario: confirmation acceptée au retour
- **WHEN** l'utilisateur a pressé Home pendant le téléchargement, que la confirmation du système a été gardée puis affichée à son retour sur SygixOs, et qu'il l'accepte
- **THEN** l'accueil de SygixOs s'affiche de nouveau après l'installation, dans la nouvelle version, si le système autorise la relance ; sinon la nouvelle version est disponible au retour manuel, conformément à la limitation constructeur reportée à P5

#### Scenario: remplacement venu d'ailleurs
- **WHEN** une demande de relance a été effacée par un échec, ou vise une autre version, et que SygixOs est remplacé par un autre moyen
- **THEN** SygixOs ne se rouvre pas

#### Scenario: pas de prise de rôle
- **WHEN** SygixOs est installé ou mis à jour sur une TV dont le launcher par défaut est un autre launcher
- **THEN** ni l'installation ni la mise à jour ne demandent le rôle d'écran d'accueil ni ne modifient un réglage ; Android peut proposer un choix de launcher au prochain appui sur Home, et la réponse appartient à l'utilisateur ; la demande du rôle n'a lieu qu'après une action explicite de l'utilisateur sur l'étape 3 du parcours guidé (« Rôle d'écran d'accueil » de `launcher-shell`)

**Détails.** — Demande de relance après installation : La demande SHALL viser la version attendue, être effacée après refus/échec/abandon et rester sans effet sur une autre version installée. Si le constructeur bloque la relance sur la TV de référence, la nouvelle version SHALL être présente au retour manuel ; cette limite acceptée reste au suivi P5 post-release, sujet 9. Cas « relance bloquée ou remplacement externe » : quand Android refuse la relance ou une autre version remplace l'app, « installation au premier plan » et « remplacement venu d'ailleurs » de « Redémarrage après la mise à jour » s'appliquent sans message annonçant la mise à jour. — Rôle HOME et mise à jour : L'activité principale SHALL rester candidate HOME pour une relance système si launcher par défaut. Vérification, téléchargement, installation et relance ne SHALL jamais demander ce rôle ni devenir launcher par défaut ; ce choix reste explicite dans l'étape 3 du parcours guidé « Rôle d'écran d'accueil » de launcher-shell. Cas « mise à jour sans prise de rôle » : quand le launcher est mis à jour sans être l'accueil par défaut, « pas de prise de rôle » de « Redémarrage après la mise à jour » laisse la décision à l'utilisateur, y compris si Android propose un choix au prochain Home.

### Requirement: Sécurité et confidentialité des mises à jour
Toute URL de mise à jour ou redirection non HTTPS SHALL être refusée avant envoi ; la configuration réseau SHALL interdire le clair vers les domaines GitHub utilisés. Les requêtes SHALL ne contenir aucune donnée personnelle, identifiant, jeton, cookie ni compte ; seuls des en-têtes techniques SHALL être envoyés, et aucun secret dans l'APK. Une requête par vérification, au plus une automatique par 24 h et aucune avant l'heure de réessai persistée SHALL respecter l'API non authentifiée.

#### Scenario: redirection non HTTPS
- **WHEN** le téléchargement est redirigé vers une URL en HTTP
- **THEN** la redirection n'est pas suivie, rien n'est installé, la ligne indique une erreur de téléchargement

#### Scenario: trafic en clair vers GitHub
- **WHEN** une requête en HTTP vers un domaine de GitHub est tentée par l'application
- **THEN** la plateforme la refuse avant tout envoi

#### Scenario: contenu des requêtes
- **WHEN** une vérification, une relecture ou un téléchargement est envoyé
- **THEN** la requête ne contient ni en-tête d'autorisation, ni cookie, ni identifiant de l'appareil ou de l'utilisateur

**Détails.** — Confidentialité du transport de mise à jour : Les requêtes ne SHALL contenir ni donnée personnelle, identifiant appareil, jeton, cookie, liste d'apps ni compte. Seuls type de réponse attendu, version API, nom/version SygixOs comme agent SHALL être envoyés. Aucun jeton ni secret ne SHALL figurer dans l'APK. Cas « contenu technique seulement » : quand une vérification, relecture ou téléchargement part, « contenu des requêtes » de « Sécurité et confidentialité des mises à jour » s'applique sans autorisation ni cookie.

### Requirement: Couverture de test des mises à jour
Les contrats de cette capability SHALL être couverts en JUnit par sources factices, sans réseau réel ni vrai installateur : tags/choix, échéances/réessai, HTTPS, état persisté, vérifications ordonnées du fichier/session, profil avec repli, action différée, abandon sans statut, relance et statuts. Le transport HTTP réel SHALL être testé par un serveur local lancé par le test.

#### Scenario: préversion contre version finale
- **WHEN** les tests de conversion et de choix s'exécutent
- **THEN** `v0.0.1-rc.4` donne 164, `v0.0.1` donne 199, `v1.2.3-beta.5` donne 100200335 ; une version finale bat les préversions du même numéro ; une préversion n'est proposée que si la préférence l'autorise ; une version inférieure ou égale à la version installée n'est jamais proposée ; les tags non conformes sont ignorés

#### Scenario: réponses de l'API
- **WHEN** le transport factice renvoie une liste vide, une release sans asset, un asset sans empreinte, un JSON illisible, une erreur 404, une erreur 403 avec la limite atteinte et son heure de réessai, une absence de réseau, un délai dépassé
- **THEN** chaque cas donne l'état attendu par « Source des versions » et « Versions comparées », sans exception non gérée, et l'heure de réessai est persistée

#### Scenario: empreinte invalide
- **WHEN** le contenu téléchargé, ou le contenu copié dans la session, ne correspond pas à l'empreinte publiée
- **THEN** la session factice n'est jamais validée, le fichier temporaire n'existe plus et l'état indique un fichier corrompu

#### Scenario: certificat différent
- **WHEN** l'inspection factice de l'APK renvoie un certificat différent de celui de l'app installée
- **THEN** l'installateur factice n'est jamais appelé, le fichier temporaire n'existe plus et l'état indique une signature différente

#### Scenario: asset introuvable
- **WHEN** le transport factice répond 404 ou 410 au téléchargement
- **THEN** l'installateur factice n'est jamais appelé, aucun fichier ne subsiste, l'état indique un fichier introuvable

#### Scenario: action requise en arrière-plan
- **WHEN** le statut factice demande une action de l'utilisateur alors que l'état de premier plan factice indique l'arrière-plan
- **THEN** aucun écran n'est lancé ; la demande est lancée au retour au premier plan simulé

#### Scenario: transport réel
- **WHEN** le transport HTTP réel interroge un serveur local du test
- **THEN** il renvoie le corps et les en-têtes utiles, respecte les délais (un serveur qui ne répond pas provoque une erreur de délai), interrompt un corps plus long que la taille attendue, et n'envoie ni en-tête d'autorisation ni cookie

#### Scenario: politique HTTPS
- **WHEN** une URL d'asset ou une URL finale après redirection est en HTTP
- **THEN** elle est refusée et aucun téléchargement n'est transmis à l'installation

#### Scenario: profil de démarrage
- **WHEN** le transport factice sert un profil valide, un profil sans empreinte, de plus de 16 Mo, incomplet, d'empreinte différente, introuvable, coupé, trop long, en HTTP ou qui n'est pas une archive de `primary.prof` et `primary.profm`, que la session factice accepte ou refuse le profil, et que le statut factice d'une session avec profil est un échec
- **THEN** seul le profil valide est écrit, dans la session de l'APK ; un profil sans empreinte valide, trop gros ou incomplet n'est jamais demandé au transport ; dans tous les autres cas, l'APK vérifié est installé seul, dans une nouvelle session si la première a refusé le profil ; un échec de la session avec profil relance une seule fois l'installation sans profil, et un second échec donne l'erreur habituelle sans autre essai ; aucun fichier ne subsiste
