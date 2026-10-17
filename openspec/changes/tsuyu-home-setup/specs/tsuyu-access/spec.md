## Purpose

Permettre à SygixOs d'obtenir, depuis la TV et sans ordinateur ni téléphone, un accès privilégié durable par Tsuyu (service compatible avec l'API Shizuku) : installation vérifiée, démarrage et autorisation guidés, exécution des opérations fixes de l'écran d'accueil et réparation automatique des réglages que la TV défait.

Les numéros 9.x renvoient à la rangée « 9. Parcours simplifié et Tsuyu » de la page « TV · Launcher principal » de la maquette Penpot (`design.md`, « Maquettes »), qui fait foi pour le rendu. Les panneaux suivent les règles de rendu de « Parcours de l'écran d'accueil » (`launcher-shell`) : verre centré sur un voile, mascotte à gauche du titre, repère « Étape X sur N » au-dessus du titre. Dans les écrans à deux boutons, gauche et droite passent de l'un à l'autre sans boucle et haut et bas sont sans effet. Tous les textes entre guillemets sont les textes définitifs.

## ADDED Requirements

### Requirement: Disponibilité de Tsuyu
Tsuyu SHALL être disponible quand le paquet `fr.sygix.tsuyu` est installé ou qu'une release éligible du dépôt public Sygix/Tsuyu est connue. Une release est éligible si elle est finale, ou préversion avec « Inclure les préversions » (`self-update`), et publie un asset `tsuyu.apk` avec sa taille et son empreinte SHA-256. Elle SHALL être vérifiée comme « Source des versions » (`self-update`) à l'ouverture du parcours ou du panneau ; la dernière réponse obtenue SHALL être conservée.

#### Scenario: release trouvée
- **WHEN** le parcours s'ouvre, que Tsuyu n'est pas installé et que l'API publie une release finale avec `tsuyu.apk`, sa taille et son empreinte
- **THEN** Tsuyu est disponible et ce résultat est conservé pour les ouvertures suivantes

#### Scenario: aucune release éligible
- **WHEN** l'API répond sans release éligible, ou seulement des préversions alors que « Inclure les préversions » est désactivé, et que Tsuyu n'est pas installé
- **THEN** Tsuyu n'est pas disponible et aucune ligne ni action Tsuyu n'est affichée

#### Scenario: vérification en échec
- **WHEN** la vérification échoue (pas de réseau, délai, limite de requêtes, réponse illisible)
- **THEN** la dernière disponibilité connue est conservée, l'échec n'est pas affiché dans le parcours et aucun crash n'a lieu

#### Scenario: Tsuyu déjà installé
- **WHEN** le paquet `fr.sygix.tsuyu` est installé, même sans réseau
- **THEN** Tsuyu est disponible, quelle que soit la réponse de l'API

### Requirement: Écran Installer Tsuyu
Quand Tsuyu n'est pas installé, ou qu'il est « À mettre à jour », l'étape « Installer Tsuyu » du parcours avec Tsuyu SHALL afficher l'écran 9.4. Cet écran présente trois étapes numérotées non focalisables, une ligne « Tsuyu » avec son état, l'aide « Retour : précédent » et les boutons « Sans Tsuyu » et « Installer Tsuyu ». « Installer Tsuyu » est focalisé et lance « Téléchargement vérifié de Tsuyu ». « Sans Tsuyu » poursuit la configuration automatique sans Tsuyu (`launcher-shell`).

#### Scenario: textes de l'écran
- **WHEN** l'écran 9.4 s'affiche, Tsuyu n'étant pas installé
- **THEN** il montre le titre « Installer Tsuyu », la description « Tsuyu est une application gratuite de l'auteur de SygixOs. Elle garde l'accès qui permet à SygixOs de se régler et de se réparer tout seul. », l'étape 1 « Choisissez « Installer Tsuyu » » (détail « SygixOs le télécharge et vérifie qu'il vient bien de son auteur. »), l'étape 2 « Si la TV le demande, autorisez SygixOs » (détail « Activez SygixOs dans la liste des sources, puis revenez avec Retour. »), l'étape 3 « Choisissez « Installer » » (détail « Android confirme l'installation de Tsuyu. »), la ligne « Tsuyu » à l'état « Non installé » (détail « Vérifié avant l'installation ») et « Installer Tsuyu » focalisé

#### Scenario: sans Tsuyu
- **WHEN** l'utilisateur presse OK sur « Sans Tsuyu »
- **THEN** rien n'est téléchargé, le parcours passe à l'étape suivante de la configuration automatique sans Tsuyu et le repère d'avancement est recalculé pour cette façon

#### Scenario: Tsuyu à mettre à jour
- **WHEN** l'écran s'affiche alors que Tsuyu est installé dans une version trop ancienne (« Version minimale de Tsuyu »)
- **THEN** le titre est « Mettre à jour Tsuyu », la ligne « Tsuyu » est « À mettre à jour » et le bouton focalisé est « Mettre à jour Tsuyu »

### Requirement: Téléchargement vérifié de Tsuyu
OK sur « Installer Tsuyu » SHALL relire la release éligible puis télécharger `tsuyu.apk` comme « Téléchargement vérifié » (`self-update`) : mêmes délais, progression et nettoyage. Le fichier SHALL n'être installé que si sa taille et son empreinte SHA-256 égalent celles publiées, si son paquet est `fr.sygix.tsuyu` et si ses certificats de signature sont identiques à ceux de SygixOs installé. Un fichier refusé SHALL être supprimé sans rien installer.

#### Scenario: progression
- **WHEN** le téléchargement est en cours
- **THEN** la ligne « Tsuyu » affiche « Téléchargement… » suivi du pourcentage, puis « Vérification… », chacun avec une pastille bleue ; les boutons sont masqués et l'aide devient « Retour : annuler »

#### Scenario: signature différente
- **WHEN** le fichier téléchargé a la bonne empreinte mais des certificats de signature différents de ceux de SygixOs
- **THEN** le fichier est supprimé, rien n'est installé et l'écran 9.6 affiche l'encadré « Le fichier de Tsuyu n'est pas valide »

#### Scenario: annulation
- **WHEN** l'utilisateur presse Retour pendant le téléchargement
- **THEN** le téléchargement s'arrête, le fichier temporaire est supprimé, rien n'est installé et l'écran 9.4 revient avec « Installer Tsuyu » focalisé

### Requirement: Installation de Tsuyu par le système
Un fichier vérifié SHALL être installé par une session d'installation du système, selon « Installation de la mise à jour » (`self-update`) : contenu vérifié, empreinte recalculée à la copie, écran système seulement au premier plan, autres sessions ignorées. La ligne « Tsuyu » SHALL afficher « Installation… ». Installé, le parcours SHALL passer à « Démarrer Tsuyu ». SygixOs SHALL n'installer Tsuyu qu'après un appui sur « Installer Tsuyu » ou « Mettre à jour Tsuyu ».

#### Scenario: sources inconnues puis installation
- **WHEN** Android demande d'autoriser SygixOs à installer des applications, que l'utilisateur l'autorise puis choisit « Installer »
- **THEN** Tsuyu est installé, la ligne « Tsuyu » passe à « Installé » avec une pastille verte, puis l'écran « Démarrer Tsuyu » (9.7) s'affiche

#### Scenario: installation refusée
- **WHEN** l'utilisateur refuse l'installation, ou revient sans qu'Android ait rendu de statut
- **THEN** la session est abandonnée et l'écran 9.6 affiche l'encadré « Tsuyu n'a pas été installé »

#### Scenario: autorisation d'installation déjà donnée
- **WHEN** SygixOs a déjà été autorisé à installer des applications, par exemple pour sa propre mise à jour
- **THEN** seule la confirmation d'installation d'Android s'affiche

### Requirement: Échec de l'installation de Tsuyu
Un échec du téléchargement, de la vérification ou de l'installation SHALL afficher l'écran 9.6 : le titre et les étapes de 9.4, un encadré d'erreur propre à la cause, la ligne « Tsuyu » à l'état relu et les boutons « Sans Tsuyu » et « Réessayer ». « Réessayer » est focalisé et relance « Téléchargement vérifié de Tsuyu ». Aucun échec SHALL ne laisser de fichier temporaire, ne relancer de tentative sans action ni empêcher de continuer sans Tsuyu.

#### Scenario: pas de connexion
- **WHEN** le téléchargement échoue faute de réseau, par délai dépassé ou par limite de requêtes
- **THEN** l'encadré « Tsuyu n'a pas pu être téléchargé » s'affiche avec le détail « Vérifiez la connexion Internet de la TV, puis réessayez, ou continuez sans Tsuyu. »

#### Scenario: fichier refusé
- **WHEN** une vérification du fichier échoue
- **THEN** l'encadré « Le fichier de Tsuyu n'est pas valide » s'affiche avec le détail « Il a été supprimé sans être installé. Réessayez plus tard, ou continuez sans Tsuyu. »

#### Scenario: installation refusée par l'utilisateur
- **WHEN** l'installation est refusée ou échoue dans la session du système
- **THEN** l'encadré « Tsuyu n'a pas été installé » s'affiche avec le détail « Android n'a pas installé Tsuyu et rien n'a été modifié. Réessayez et choisissez « Installer », ou continuez sans Tsuyu. »

### Requirement: Démarrer Tsuyu
Quand Tsuyu est installé mais ne fournit pas l'API Shizuku à SygixOs, l'étape « Démarrer Tsuyu » SHALL afficher l'écran 9.7. Il présente trois étapes numérotées, la ligne « Tsuyu » (état « Arrêté » ou « Actif », détail « Détecté au retour dans SygixOs »), l'aide « Retour : précédent » et le bouton « Ouvrir Tsuyu », focalisé. Ce bouton ouvre Tsuyu dans la tâche de SygixOs, pour que Retour y ramène. Au retour, SygixOs SHALL attendre l'API un temps borné puis relire l'état.

#### Scenario: textes de l'écran
- **WHEN** l'écran 9.7 s'affiche
- **THEN** il montre le titre « Démarrer Tsuyu », la description « Tsuyu s'ouvre. Suivez ces étapes, puis revenez dans SygixOs avec Retour. », l'étape 1 « Choisissez « Démarrer » » (détail « Sur l'écran d'accueil de Tsuyu. »), l'étape 2 « Cochez « Toujours autoriser sur cet ordinateur », puis « Autoriser » » (détail « Android le demande une seule fois, pour Tsuyu. »), l'étape 3 « Activez « Démarrage au redémarrage » » (détail « Tsuyu redémarrera avec la TV. ») et « Ouvrir Tsuyu » focalisé

#### Scenario: Tsuyu démarré
- **WHEN** l'utilisateur revient dans SygixOs et que Tsuyu fournit l'API Shizuku dans le délai d'attente
- **THEN** la ligne « Tsuyu » passe à « Actif » et le parcours passe à « Autoriser SygixOs dans Tsuyu » (9.8), ou directement à la configuration si SygixOs est déjà autorisé

#### Scenario: Tsuyu non démarré
- **WHEN** l'utilisateur revient sans que Tsuyu fournisse l'API dans le délai
- **THEN** l'écran 9.7 reste affiché avec « Tsuyu » à « Arrêté » et « Ouvrir Tsuyu » focalisé, sans erreur ni nouvelle ouverture automatique

#### Scenario: Tsuyu impossible à ouvrir
- **WHEN** l'activité de Tsuyu ne peut pas s'ouvrir
- **THEN** rien ne change, sans crash, et la ligne « Tsuyu » garde son état relu

### Requirement: Autoriser SygixOs dans Tsuyu
Quand Tsuyu est actif et que SygixOs n'y est pas autorisé, l'étape « Autoriser SygixOs » SHALL afficher l'écran 9.8 avec le bouton « Continuer », focalisé. Aucune demande SHALL n'être faite avant OK sur « Continuer ». OK SHALL demander l'autorisation par l'API Shizuku, ce qui ouvre la fenêtre de Tsuyu par-dessus SygixOs. Accordée, la configuration automatique démarre par Tsuyu. Refusée, SygixOs SHALL afficher l'écran 9.9, sans rien modifier.

#### Scenario: textes de l'écran
- **WHEN** l'écran 9.8 s'affiche
- **THEN** il montre le titre « Autoriser SygixOs », la description « Tsuyu est actif. Il va vous demander si SygixOs peut l'utiliser. », l'étape 1 « Sélectionnez « Autoriser » » (détail « Par prudence, Tsuyu sélectionne d'abord « Refuser » : déplacez-vous sur « Autoriser ». »), l'étape 2 « Validez avec OK » (détail « La configuration démarre aussitôt. »), la ligne « Tsuyu » à l'état « Actif » et « Continuer » focalisé

#### Scenario: autorisation accordée
- **WHEN** l'utilisateur presse « Continuer » puis choisit « Autoriser » dans Tsuyu
- **THEN** l'écran « Configuration en cours » s'affiche et la séquence s'exécute par Tsuyu, sans fenêtre « Autoriser le débogage USB ? » pour SygixOs

#### Scenario: autorisation refusée
- **WHEN** l'utilisateur choisit « Refuser » ou presse Retour dans Tsuyu
- **THEN** l'écran 9.9 s'affiche avec le titre et la description de 9.8, l'encadré « SygixOs n'est pas autorisé » (détail « Tsuyu a refusé l'accès et rien n'a été modifié. Réessayez et choisissez « Autoriser ». ») et les boutons « Retour » et « Réessayer », ce dernier focalisé

#### Scenario: Tsuyu ne demande plus
- **WHEN** l'API Shizuku indique que Tsuyu ne présentera plus de demande pour SygixOs
- **THEN** l'encadré a le détail « Tsuyu a gardé votre refus. Ouvrez Tsuyu pour changer ce choix, puis revenez. » et le bouton focalisé est « Ouvrir Tsuyu » à la place de « Réessayer »

### Requirement: Accès privilégié par Tsuyu
Quand Tsuyu fournit l'API Shizuku et y autorise SygixOs, SygixOs SHALL exécuter les opérations fixes de « Opérations privilégiées limitées » (`launcher-shell`) par Tsuyu, dans un service propre à SygixOs, lancé avec l'identité du shell. Ce service SHALL n'accepter que ces opérations typées. Il SHALL être lancé au début de chaque séquence et arrêté à sa fin. Un accès perdu pendant une séquence SHALL être traité comme un débogage perdu (`launcher-shell`).

#### Scenario: séquence par Tsuyu
- **WHEN** une séquence de configuration démarre alors que Tsuyu est actif et que SygixOs y est autorisé
- **THEN** les opérations sont exécutées par le service de SygixOs avec l'identité du shell, chacune relue, et aucune connexion ADB de SygixOs n'est ouverte

#### Scenario: fin de séquence
- **WHEN** la séquence se termine, réussie, en échec ou par délai
- **THEN** le service de SygixOs lancé par Tsuyu est arrêté

#### Scenario: Tsuyu arrêté pendant une séquence
- **WHEN** Tsuyu s'arrête ou retire l'autorisation de SygixOs pendant une séquence
- **THEN** la séquence s'arrête, l'état est relu et l'écran « Configuration interrompue » montre ce qui est fait, sans succès supposé

### Requirement: Version minimale de Tsuyu
SygixOs SHALL n'utiliser Tsuyu que si l'API Shizuku qu'il fournit est en version 13 ou plus. Une version inférieure SHALL rendre Tsuyu « À mettre à jour » et faire proposer « Mettre à jour Tsuyu » (9.4). Ce bouton n'est actif que si une release éligible plus récente que la version installée est connue ; sinon l'état est « Indisponible pour le moment ». Aucune mise à jour de Tsuyu SHALL n'être proposée hors de ce cas.

#### Scenario: version trop ancienne
- **WHEN** Tsuyu fournit l'API Shizuku dans une version inférieure à 13
- **THEN** aucune opération n'est envoyée par Tsuyu et la ligne « Tsuyu » est « À mettre à jour »

#### Scenario: version suffisante
- **WHEN** Tsuyu est à jour pour l'API mais qu'une release plus récente existe
- **THEN** aucune mise à jour n'est proposée

### Requirement: États de Tsuyu
L'état de Tsuyu SHALL être relu à chaque retour au premier plan et à l'ouverture du parcours ou du panneau, sans valeur conservée d'avant la sortie. Les états sont, dans cet ordre de priorité :
- « Indisponible sur cette TV » : pas de port ADB réseau (`launcher-shell`) ;
- « À installer » : Tsuyu absent ;
- « À mettre à jour » ;
- « Arrêté » : l'API ne répond pas ;
- « À autoriser » ;
- « Actif », avec une pastille verte.

#### Scenario: Tsuyu désactivé par la TV
- **WHEN** le débogage a été désactivé ou la TV a redémarré sans que Tsuyu redémarre
- **THEN** au retour au premier plan, la ligne « Tsuyu » est « Arrêté », la configuration automatique utilise l'ADB intégré si elle est relancée, et aucune erreur n'est affichée hors du panneau

#### Scenario: autorisation retirée dans Tsuyu
- **WHEN** l'utilisateur retire l'accès de SygixOs dans Tsuyu
- **THEN** au retour au premier plan, la ligne « Tsuyu » est « À autoriser » et la réparation automatique ne s'exécute plus

#### Scenario: Tsuyu désinstallé
- **WHEN** Tsuyu est désinstallé
- **THEN** la ligne « Tsuyu » est « À installer » si une release éligible est connue, absente sinon, et SygixOs reste utilisable sans crash

### Requirement: Réparation automatique
Avec Tsuyu « Actif », SygixOs SHALL, au retour au premier plan et sans rien afficher, réparer deux réglages qu'il a posés et que la TV a défaits : le service de la touche Home « Bloqué » (activé mais non connecté), qu'il retire puis remet dans la liste des services activés en gardant les autres ; l'autorisation de démarrage du constructeur, relue et réaccordée quand ce service est « Bloqué » ou que l'ouverture à l'allumage n'a pas été observée à ce démarrage.

#### Scenario: service bloqué après un redémarrage
- **WHEN** après un redémarrage, le service de la touche Home posé par SygixOs est activé mais non connecté au terme de l'attente bornée, et que Tsuyu est actif
- **THEN** SygixOs relit et réaccorde au besoin l'autorisation de démarrage du constructeur, retire puis remet son service sans retirer les autres, relit la connexion, et la ligne « Touche Home » affiche « Fait » si le service est connecté

#### Scenario: ouverture à l'allumage non observée
- **WHEN** l'option d'ouverture à l'allumage posée par SygixOs est activée, que l'ouverture n'a pas été observée à ce démarrage et que l'autorisation du constructeur relue n'est plus accordée
- **THEN** SygixOs la réaccorde, la relit et enregistre la réparation ; le démarrage suivant est évalué normalement

#### Scenario: rien à réparer
- **WHEN** aucun des deux symptômes n'est présent au retour au premier plan
- **THEN** aucune opération privilégiée n'est envoyée et le service de SygixOs n'est pas lancé par Tsuyu

### Requirement: Limites de la réparation automatique
La réparation SHALL ne viser que les réglages listés comme faits par SygixOs dans « Détails des modifications » (`launcher-shell`). Elle SHALL ne jamais réactiver un service désactivé, ne jamais reposer la superposition ou l'option d'allumage, et ne jamais désactiver l'accueil d'origine. Chaque réglage SHALL être tenté au plus une fois par démarrage de la TV. Chaque réparation, réussie ou non, SHALL être enregistrée avec sa date pour « Détails ».

#### Scenario: service désactivé par l'utilisateur
- **WHEN** l'utilisateur a désactivé le service de la touche Home dans Accessibilité
- **THEN** aucune réparation n'a lieu et la ligne affiche « À faire »

#### Scenario: une tentative par démarrage
- **WHEN** une réparation du service a eu lieu et que le service reste « Bloqué » au retour au premier plan suivant, dans le même démarrage
- **THEN** aucune nouvelle tentative n'a lieu avant le prochain démarrage de la TV

#### Scenario: accueil Google TV réactivé
- **WHEN** l'accueil d'origine désactivé à la demande de l'utilisateur est de nouveau actif
- **THEN** aucune réparation n'a lieu ; l'état relu est affiché par le panneau « Launcher principal »
