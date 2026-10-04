# Delta self-update

## MODIFIED Requirements

### Requirement: Téléchargement vérifié
OK sur « Mettre à jour vers X » SHALL d'abord relire la release de cette version (une requête à l'API) pour obtenir son URL de téléchargement et son empreinte à jour, vérifier l'espace libre, puis télécharger l'asset `app-release.apk` en HTTPS dans le cache de l'application, en affichant la progression sur la ligne. Le téléchargement SHALL continuer si l'utilisateur ferme les réglages ou quitte SygixOs (touche Home, autre app), et l'installation SHALL suivre dès sa fin (« Installation de la mise à jour »). Le fichier téléchargé SHALL n'être installé que si toutes les vérifications réussissent, dans cet ordre : taille égale à celle publiée par l'API, empreinte SHA-256 égale à celle publiée par l'API, nom de paquet `fr.sygix.sygixos`, `versionCode` de l'APK égal à celui calculé depuis le tag et supérieur à celui de l'app installée, ensemble des certificats de signature de l'APK identique à celui de l'app installée. Un échec SHALL supprimer le fichier, n'exécuter ni installer rien, et afficher sa cause dans « À propos ». Un appui sur « Mettre à jour vers X » pendant une opération en cours SHALL être sans effet. Tout fichier temporaire de mise à jour SHALL être supprimé dès que son contenu a été transmis à l'installation, après un échec, et à chaque démarrage à froid. Chaque requête de téléchargement SHALL avoir un délai total borné. Si la release relue publie aussi un asset `app-release.dm` (profil de démarrage, « Profil de démarrage livré avec l'application » de `launcher-shell`) complètement téléversé, d'une taille publiée d'au plus 16 Mo, avec une empreinte SHA-256 publiée et une URL de téléchargement HTTPS, le launcher SHALL le télécharger en HTTPS après les vérifications de l'APK, dans un fichier temporaire du cache, avec un délai total d'au plus 10 s, et ne le garder que si sa taille et son empreinte SHA-256 sont celles publiées par l'API et si c'est une archive qui contient exactement `primary.prof` et `primary.profm` (ce que produit le build), non vides ; le fichier temporaire SHALL être supprimé dans tous les cas. Un profil sans empreinte SHA-256 publiée valide SHALL n'être ni téléchargé ni installé. L'absence du profil, son refus ou l'échec de son téléchargement SHALL n'empêcher ni retarder l'installation de l'APK, qui se fait alors sans profil, sans message.

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

### Requirement: Installation de la mise à jour
Une fois le fichier vérifié, le launcher SHALL l'installer comme mise à jour de lui-même par une session d'installation du système, en demandant qu'aucune action de l'utilisateur ne soit requise quand Android le permet. Le contenu transmis à la session SHALL être celui qui a été vérifié : l'empreinte SHA-256 SHALL être recalculée pendant la copie dans la session et comparée à l'empreinte publiée, et le fichier SHALL être supprimé avant la validation de la session. Si Android exige une action de l'utilisateur (confirmation, ou autorisation d'installer des applis inconnues), le launcher SHALL afficher l'écran du système quand SygixOs est au premier plan ; si SygixOs est en arrière-plan, il SHALL garder cette demande et l'afficher au prochain retour sur SygixOs, sans jamais surgir au-dessus d'une autre app. Le launcher SHALL distinguer : installation réussie, action refusée par l'utilisateur, échec (avec sa cause), et SHALL ignorer les statuts d'une autre session que celle en cours. Si l'utilisateur revient sur SygixOs depuis l'écran du système sans qu'Android ait rendu de statut et sans installation en cours, le launcher SHALL abandonner la session et traiter ce retour comme une action refusée. Quand un profil de démarrage vérifié est disponible (« Téléchargement vérifié »), il SHALL être écrit dans la même session que l'APK, sous le nom attendu par Android pour cet APK (`base.dm` pour `base.apk`), avec exactement le contenu vérifié, avant la validation de la session ; si la session refuse le profil, elle SHALL être abandonnée sans être validée et l'APK SHALL être installé seul dans une nouvelle session, avec les mêmes règles. Si une session qui portait un profil se termine en échec (statut d'échec rendu par Android après sa validation, par exemple un profil refusé ou une exigence de signature du profil), le launcher SHALL relancer automatiquement, une seule fois, la mise à jour sans profil : relecture de la release, nouveau téléchargement de l'APK (supprimé avant la validation de la première session) et mêmes vérifications ; seule l'étape affichée sur la ligne change, et la relance au premier plan et la demande de relance suivent les mêmes règles que la première tentative ; un échec de la session sans profil SHALL être traité comme un échec normal, sans nouvel essai, et un échec de la relance avant la validation de sa session (relecture, téléchargement, vérifications) SHALL effacer la demande de relance, comme tout échec. Il SHALL n'installer qu'à la suite d'un appui de l'utilisateur sur « Mettre à jour vers X ».

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

### Requirement: Couverture de test des mises à jour
La logique de mise à jour SHALL être couverte sans accès au réseau réel ni au vrai installateur : conversion des tags, choix de la version proposée, échéance de la vérification automatique, heure de réessai, politique HTTPS, état affiché calculé depuis ce qui est persisté, enchaînement des vérifications du fichier et de la copie dans la session, profil de démarrage (téléchargé, vérifié et écrit dans la session de l'APK, ou abandonné sans bloquer l'APK), conservation d'une action requise en arrière-plan, abandon au retour sans statut, décision de relance, correspondance des statuts d'installation, par des tests JUnit avec des sources factices ; le transport HTTP réel SHALL être testé contre un serveur local lancé par le test.

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
