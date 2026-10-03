# Delta self-update

## Purpose
Mise à jour du launcher depuis les releases GitHub publiques du dépôt Sygix/SygixOs : vérification manuelle et quotidienne, choix des préversions, signalement discret, téléchargement vérifié et installation à la demande de l'utilisateur, sans serveur propre ni jeton.

## ADDED Requirements

### Requirement: Source des versions
Le launcher SHALL obtenir les versions publiées en une seule requête à l'API REST publique des releases du dépôt Sygix/SygixOs, en HTTPS, sans jeton ni authentification d'aucune sorte. Chaque requête SHALL avoir un délai de connexion et un délai de lecture bornés. La source SHALL distinguer pour l'appelant : liste obtenue (éventuellement vide), absence de réseau, délai dépassé, limite de requêtes atteinte (avec l'heure à partir de laquelle réessayer quand l'API la fournit), releases inaccessibles (dépôt introuvable ou privé, autre réponse HTTP d'erreur) et réponse illisible ; aucune erreur n'est avalée ni ne provoque de crash. Quand l'API signale la limite atteinte, l'heure de réessai SHALL être persistée et aucune requête à l'API, automatique ou manuelle, ne SHALL être envoyée avant cette heure, y compris après un redémarrage.

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

#### Scenario: vérification avant l'heure de réessai
- **WHEN** une vérification, automatique ou manuelle, ou la relecture de « Mettre à jour vers X » est demandée avant l'heure de réessai persistée, même après un redémarrage
- **THEN** aucune requête n'est envoyée ; l'état indique la limite atteinte et l'heure de réessai

#### Scenario: releases inaccessibles
- **WHEN** l'API répond « introuvable » (dépôt privé ou supprimé) ou par une autre erreur HTTP
- **THEN** la vérification aboutit à une erreur « releases inaccessibles », distincte d'une absence de réseau, sans crash

#### Scenario: réponse illisible
- **WHEN** la réponse n'est pas un JSON de releases valide
- **THEN** la vérification aboutit à une erreur « réponse illisible » ; les versions connues auparavant sont conservées

### Requirement: Versions comparées
Seules les releases publiées (non brouillons) dont le tag suit exactement la forme des tags du dépôt — `vX.Y.Z` ou `vX.Y.Z-alpha.N`, `-beta.N`, `-rc.N` (le point avant N facultatif), avec X ≤ 20, Y et Z ≤ 999, N ≤ 29 — et qui portent un asset `app-release.apk` complètement téléversé, avec une empreinte SHA-256 publiée et une URL de téléchargement HTTPS, SHALL être éligibles ; toute autre release SHALL être ignorée sans erreur. Une release SHALL être une préversion si son tag a un suffixe ou si l'API la marque comme préversion. Chaque tag éligible SHALL être converti en `versionCode` avec la formule du build : major × 10⁸ + minor × 10⁵ + patch × 10² + suffixe (alpha.N → N, beta.N → 30 + N, rc.N → 60 + N, version finale → 99). La version proposée SHALL être, parmi les releases éligibles autorisées par la préférence des préversions (« Préversions »), celle dont le `versionCode` est le plus grand, quelle que soit sa date de publication ; elle n'est proposée que si ce `versionCode` est strictement supérieur à celui de l'app installée. Une version inférieure ou égale à la version installée ne SHALL jamais être proposée.

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
- **WHEN** la release la plus récente n'a pas d'asset `app-release.apk`, ou son asset n'est pas complètement téléversé, n'a pas d'empreinte SHA-256 publiée ou pas d'URL HTTPS
- **THEN** elle est ignorée ; la version proposée est la meilleure des autres releases éligibles, s'il y en a une supérieure à la version installée

#### Scenario: préversion selon l'API
- **WHEN** une release au tag `vX.Y.Z` sans suffixe est marquée préversion par l'API
- **THEN** elle est traitée comme une préversion

### Requirement: Préversions
La catégorie « À propos » SHALL contenir une ligne « Inclure les préversions » avec un interrupteur (style Apple, comme « Apps sources »), désactivé par défaut dans tous les cas, y compris quand la version installée est elle-même une préversion. OK sur la ligne SHALL basculer l'interrupteur et persister l'état. Le changement SHALL s'appliquer immédiatement à la version proposée, à l'état affiché et à la pastille, à partir des dernières versions connues et sans nouvelle requête ; si aucune vérification n'a encore réussi, l'état affiché ne change pas.

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
La catégorie « À propos » SHALL contenir une ligne « Vérifier les mises à jour » dont un texte secondaire indique l'état : vérification en cours, ou bien, à partir de ce qui est persisté (versions connues, résultat de la dernière vérification, heure de réessai), dans cet ordre de priorité : nouvelle version disponible (avec son numéro et, pour une préversion, la mention de préversion) si une version est proposée ; sinon le résultat de la dernière vérification, manuelle ou automatique (à jour, ou l'erreur avec sa cause, « Source des versions ») ; sinon « jamais vérifié » si aucune vérification n'a jamais abouti ni échoué. OK sur la ligne SHALL lancer une vérification ; un nouvel appui pendant une vérification en cours SHALL être sans effet. Toute vérification terminée SHALL persister son résultat et sa date. Quand une version est proposée, une ligne « Mettre à jour vers X » (X : numéro de la version proposée) SHALL être affichée juste sous « Vérifier les mises à jour » ; elle SHALL être absente sinon.

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
- **THEN** le texte secondaire donne la cause en français clair (pas de connexion, délai dépassé, limite atteinte avec l'heure, releases inaccessibles, réponse illisible), sauf si une version connue reste proposée : le texte nomme alors cette version et la ligne « Mettre à jour vers X » reste affichée ; OK relance une vérification

#### Scenario: état après un redémarrage
- **WHEN** le launcher redémarre à froid et que l'utilisateur ouvre « À propos » sans nouvelle vérification
- **THEN** le texte est calculé à partir de ce qui est persisté, selon l'ordre de priorité de l'exigence : version proposée, sinon dernier résultat (à jour ou erreur), sinon « jamais vérifié »

#### Scenario: après un échec automatique
- **WHEN** la dernière vérification, automatique, a échoué et qu'aucune version n'est proposée
- **THEN** « À propos » affiche cette erreur et sa cause, et non « jamais vérifié »

#### Scenario: version installée par un autre moyen
- **WHEN** une version égale ou supérieure à la version proposée a été installée autrement (par exemple `adb install`) et que le launcher redémarre
- **THEN** cette version n'est plus proposée : plus de ligne « Mettre à jour vers X » ni de pastille, et le texte suit l'ordre de priorité

### Requirement: Notes de version
Quand une version est proposée, « À propos » SHALL afficher, à droite de la ligne « Mettre à jour vers X », un code QR non focusable qui pointe vers la page de la release sur GitHub, avec la légende « Notes de version ». Le code QR SHALL être lisible par un téléphone depuis le canapé (modules sombres sur fond clair, marge de silence) et ne SHALL être affiché que si l'URL de la page de la release est une URL HTTPS de github.com.

#### Scenario: version proposée
- **WHEN** une version est proposée
- **THEN** le code QR de la page de sa release et sa légende sont affichés à droite de la ligne « Mettre à jour vers X » ; le focus et la navigation D-pad sont les mêmes que sans code QR

#### Scenario: aucune version proposée
- **WHEN** aucune version n'est proposée
- **THEN** aucun code QR n'est affiché

#### Scenario: URL de release invalide
- **WHEN** l'URL de la page de la release publiée par l'API n'est pas une URL HTTPS de github.com
- **THEN** aucun code QR n'est affiché, sans erreur ; la ligne « Mettre à jour vers X » reste utilisable

### Requirement: Vérification automatique quotidienne
Au démarrage à froid du launcher, après l'affichage de l'accueil, et à chaque retour du launcher au premier plan, le launcher SHALL lancer en arrière-plan une vérification si la dernière vérification (manuelle ou automatique, réussie ou non) date de plus de 24 h, n'a jamais eu lieu, ou a une date dans le futur ; seulement si un réseau validé est disponible et si l'heure de réessai persistée est passée. La vérification automatique SHALL être silencieuse : elle ne montre ni message, ni dialogue, ne télécharge et n'installe rien ; ses seuls effets visibles sont la pastille quand une version est proposée et l'état d'« À propos ».

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
Tant qu'une version est proposée (« Versions comparées », avec la préférence des préversions courante), la pastille du design system (« Pastille » de `launcher-shell`) SHALL être affichée sur l'engrenage de la capsule de l'accueil (« Capsule heure et réglages » de `settings`) et sur la catégorie « À propos » du volet gauche des réglages. Elle SHALL disparaître seulement quand il n'y a plus rien à installer : version proposée installée, ou plus aucune version proposée (préversions désactivées, vérification qui ne trouve plus rien de supérieur). La consulter ne la fait pas disparaître.

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
Les lignes « Vérifier les mises à jour », « Mettre à jour vers X » (si présente, juste dessous) et « Inclure les préversions » SHALL être des lignes focusables d'« À propos », dans cet ordre, placées avant la liste des licences, avec le focus et les pilules de toute ligne des réglages (« Page de réglages » de `settings`). Le premier élément focusable du volet droit d'« À propos » SHALL être « Vérifier les mises à jour » (focus initial de « Page de réglages »). Haut et bas SHALL parcourir les lignes dans l'ordre affiché puis la liste des licences, sans boucle aux bords ; gauche rend le focus à la catégorie « À propos » ; Retour ferme les réglages (« Page de réglages », scénario « retour »).

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
OK sur « Mettre à jour vers X » SHALL d'abord relire la release de cette version (une requête à l'API) pour obtenir son URL de téléchargement et son empreinte à jour, vérifier l'espace libre, puis télécharger l'asset `app-release.apk` en HTTPS dans le cache de l'application, en affichant la progression sur la ligne. Le téléchargement SHALL continuer si l'utilisateur ferme les réglages ou quitte SygixOs (touche Home, autre app), et l'installation SHALL suivre dès sa fin (« Installation de la mise à jour »). Le fichier téléchargé SHALL n'être installé que si toutes les vérifications réussissent, dans cet ordre : taille égale à celle publiée par l'API, empreinte SHA-256 égale à celle publiée par l'API, nom de paquet `fr.sygix.sygixos`, `versionCode` de l'APK égal à celui calculé depuis le tag et supérieur à celui de l'app installée, ensemble des certificats de signature de l'APK identique à celui de l'app installée. Un échec SHALL supprimer le fichier, n'exécuter ni installer rien, et afficher sa cause dans « À propos ». Un appui sur « Mettre à jour vers X » pendant une opération en cours SHALL être sans effet. Tout fichier temporaire de mise à jour SHALL être supprimé dès que son contenu a été transmis à l'installation, après un échec, et à chaque démarrage à froid.

#### Scenario: téléchargement et progression
- **WHEN** l'utilisateur presse OK sur « Mettre à jour vers X » avec assez d'espace
- **THEN** la release est relue, le téléchargement démarre, la ligne affiche la progression en pourcentage, puis l'étape de vérification, puis l'installation

#### Scenario: release relue modifiée
- **WHEN** la relecture de la release renvoie une autre URL de téléchargement ou une autre empreinte que celles connues
- **THEN** le téléchargement utilise l'URL et l'empreinte relues

#### Scenario: release retirée
- **WHEN** la relecture de la release répond « introuvable », ou la release n'est plus éligible (« Versions comparées »)
- **THEN** rien n'est téléchargé ; la ligne indique que la version n'est plus disponible ; cette version est retirée des versions connues, la version proposée et la pastille sont recalculées

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

### Requirement: Installation de la mise à jour
Une fois le fichier vérifié, le launcher SHALL l'installer comme mise à jour de lui-même par une session d'installation du système, en demandant qu'aucune action de l'utilisateur ne soit requise quand Android le permet. Le contenu transmis à la session SHALL être celui qui a été vérifié : l'empreinte SHA-256 SHALL être recalculée pendant la copie dans la session et comparée à l'empreinte publiée, et le fichier SHALL être supprimé avant la validation de la session. Si Android exige une action de l'utilisateur (confirmation, ou autorisation d'installer des applis inconnues), le launcher SHALL afficher l'écran du système quand SygixOs est au premier plan ; si SygixOs est en arrière-plan, il SHALL garder cette demande et l'afficher au prochain retour sur SygixOs, sans jamais surgir au-dessus d'une autre app. Le launcher SHALL distinguer : installation réussie, action refusée par l'utilisateur, échec (avec sa cause). Il SHALL n'installer qu'à la suite d'un appui de l'utilisateur sur « Mettre à jour vers X ».

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
- **WHEN** l'utilisateur refuse ou quitte l'écran du système
- **THEN** la ligne indique que l'installation a été annulée, la version reste proposée et « Mettre à jour vers X » peut être relancé

#### Scenario: écran du système indisponible
- **WHEN** l'écran du système demandé par Android ne peut pas être ouvert sur l'appareil
- **THEN** la session est abandonnée, la ligne indique où accorder l'autorisation dans les paramètres de la TV, sans crash

#### Scenario: échec de l'installation
- **WHEN** le système refuse l'installation (incompatibilité, stockage, autre cause)
- **THEN** aucun fichier ne subsiste, la ligne indique l'échec et sa cause en français clair, sans crash ; la version reste proposée

### Requirement: Redémarrage après la mise à jour
Après une installation réussie, si SygixOs était au premier plan au moment de l'installation, le launcher SHALL se rouvrir sur son accueil sans action de l'utilisateur, dans la nouvelle version, comme lors d'un démarrage à froid. Si SygixOs n'était pas au premier plan, il SHALL ne pas se rouvrir et ne rien afficher : la nouvelle version est simplement présente au prochain retour sur SygixOs. Le launcher SHALL déclarer son activité principale comme candidate au rôle d'écran d'accueil d'Android, pour que le système la relance quand SygixOs est le launcher par défaut, mais SHALL ne jamais demander ce rôle ni rien faire pour devenir launcher par défaut : ce choix reste celui de l'utilisateur.

#### Scenario: installation au premier plan
- **WHEN** l'installation réussit alors que SygixOs était au premier plan
- **THEN** l'accueil de SygixOs s'affiche de nouveau sans appui de l'utilisateur, dans la nouvelle version ; « À propos » affiche la nouvelle version et la pastille a disparu ; aucun message n'annonce la mise à jour

#### Scenario: installation en arrière-plan
- **WHEN** l'installation réussit alors qu'une autre app est au premier plan
- **THEN** SygixOs ne s'ouvre pas et rien ne s'affiche par-dessus l'app ; au prochain retour sur SygixOs, la nouvelle version est installée et c'est un démarrage à froid

#### Scenario: pas de prise de rôle
- **WHEN** SygixOs est installé ou mis à jour sur une TV dont le launcher par défaut est un autre launcher
- **THEN** SygixOs ne demande pas le rôle d'écran d'accueil et ne modifie aucun réglage ; Android peut proposer un choix de launcher au prochain appui sur Home, et la réponse appartient à l'utilisateur

### Requirement: Sécurité et confidentialité des mises à jour
Toute requête de mise à jour SHALL utiliser HTTPS ; toute URL, y compris une redirection, qui n'est pas en HTTPS SHALL être refusée avant tout envoi ou abandonnée, et la configuration réseau de l'application SHALL interdire le trafic en clair vers les domaines de GitHub utilisés (API, pages et hébergement des assets). Les requêtes SHALL ne contenir aucune donnée personnelle ni identifiant de l'appareil : ni jeton, ni cookie, ni liste d'apps, ni compte ; seuls des en-têtes techniques (type de réponse attendu, version de l'API, nom et version de SygixOs comme agent) sont envoyés. Aucun jeton ni secret SHALL figurer dans l'APK. Le launcher SHALL respecter la limite de requêtes de l'API non authentifiée : une seule requête par vérification et par relecture, au plus une vérification automatique par 24 h, aucune requête avant l'heure de réessai persistée.

#### Scenario: redirection non HTTPS
- **WHEN** le téléchargement est redirigé vers une URL en HTTP
- **THEN** la redirection n'est pas suivie, rien n'est installé, la ligne indique une erreur de téléchargement

#### Scenario: trafic en clair vers GitHub
- **WHEN** une requête en HTTP vers un domaine de GitHub est tentée par l'application
- **THEN** la plateforme la refuse avant tout envoi

#### Scenario: contenu des requêtes
- **WHEN** une vérification, une relecture ou un téléchargement est envoyé
- **THEN** la requête ne contient ni en-tête d'autorisation, ni cookie, ni identifiant de l'appareil ou de l'utilisateur

### Requirement: Couverture de test des mises à jour
La logique de mise à jour SHALL être couverte sans accès au réseau réel ni au vrai installateur : conversion des tags, choix de la version proposée, échéance de la vérification automatique, heure de réessai, politique HTTPS, état affiché calculé depuis ce qui est persisté, enchaînement des vérifications du fichier et de la copie dans la session, conservation d'une action requise en arrière-plan, décision de relance, correspondance des statuts d'installation, par des tests JUnit avec des sources factices ; le transport HTTP réel SHALL être testé contre un serveur local lancé par le test.

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
