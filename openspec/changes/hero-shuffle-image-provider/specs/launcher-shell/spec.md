# Spec Delta

## MODIFIED Requirements

### Requirement: Contenu héro TV Provider
Le héro SHALL afficher les programmes watch next et preview des apps installées, avec la seule permission système de lecture. Toute colonne absente ou vide SHALL retirer seulement l'élément concerné. Le héro et le Top Shelf SHALL ne montrer que des programmes encore publiés, relus au changement signalé et au plus tard toutes les 15 minutes au premier plan, jamais en arrière-plan ; une relecture sans changement ne republie rien ; un programme n'est jamais exclu pour sa seule absence d'image.

#### Scenario: permission
- **WHEN** le launcher démarre sans `android.permission.READ_TV_LISTINGS`
- **THEN** la demande système s'affiche ; dès l'octroi le héro se recharge sans redémarrage ; en cas de refus le héro reste sur le fallback sans erreur visible

#### Scenario: programmes disponibles
- **WHEN** une ou plusieurs apps installées publient des programmes dans le TV Provider
- **THEN** le héro les affiche : vidéo d'aperçu prioritaire, sinon candidates affiche et miniature selon « Chargement partagé des images » et « Qualité des visuels », puis repli du « Diaporama héro » ; titre, app source, type du programme (`watch_next_type` d'un watch next ; preview program sinon), numéros de saison et d'épisode affichés (`season_display_number`, `episode_display_number`), durée et progression quand disponibles ; ordre mélangé au hasard une fois par lancement de SygixOs (« Ordre aléatoire du héro »), toutes apps confondues, sans liste d'apps codée en dur ; les requêtes au provider ne portent aucune clause de sélection, le filtrage par app est fait côté launcher

#### Scenario: fallback
- **WHEN** aucune app ne publie de programme dans le TV Provider
- **THEN** le héro utilise le fond vidéo de secours, puis le fond neutre si les clips sont illisibles, sans crash ni écran vide

#### Scenario: colonnes facultatives
- **WHEN** un programme ne publie pas son type, sa saison, son épisode, sa durée ou sa position
- **THEN** il reste affiché ; seuls les éléments correspondants manquent, selon « Diaporama héro »

#### Scenario: rafraîchissement automatique
- **WHEN** l'accueil est au premier plan et que le TV Provider signale un changement, ou que 15 minutes se sont écoulées depuis la dernière relecture
- **THEN** le héro relit le provider ; aucune relecture n'a lieu tant que le launcher est en arrière-plan, et le retour au premier plan en déclenche une

#### Scenario: programme retiré
- **WHEN** une app retire un programme publié pendant que l'accueil est affiché
- **THEN** il disparaît du héro et du Top Shelf à la relecture suivante, sans redémarrage ; s'il était affiché dans le héro, le héro passe par fondu au programme qui prend sa place, ses textes ne changeant qu'avec le visuel (« Diaporama héro ») ; ses visuels sortent des ensembles de visuels vérifiés

#### Scenario: programme modifié
- **WHEN** une app modifie le titre ou le visuel d'un programme publié
- **THEN** le héro et le Top Shelf montrent le nouveau titre et le nouveau visuel à la relecture suivante ; un nouveau visuel est vérifié comme un visuel inconnu (« Qualité des visuels »)

#### Scenario: source en erreur ou absente
- **WHEN** une table du provider est absente, échoue ou ne répond pas dans son délai
- **THEN** l'erreur reste distincte d'une liste vide réussie ; seuls les anciens programmes encore valides de cette table sont conservés, sinon le héro montre son repli et le Top Shelf ne montre aucun ancien programme de cette table

**Détails.** — Données facultatives du héro : Hors identifiant et package, les colonnes du TV Provider SHALL être facultatives : absentes ou vides, elles retirent seulement l'élément alimenté. Le remplissage réel du type, de la saison, de l'épisode et de la durée sur la TV de référence reste une hypothèse à vérifier sur l'appareil. Cas « métadonnées partielles » : quand une colonne facultative n'est pas publiée, « colonnes facultatives » de « Contenu héro TV Provider » s'applique sans inventer la valeur manquante. — Relecture des programmes du héro : Le héro et le Top Shelf SHALL ne montrer que des programmes encore publiés, de « Validité des programmes publiés ». Le launcher SHALL relire le provider sur signal de changement et au plus tard toutes les 15 minutes au premier plan, jamais en arrière-plan ; un succès inchangé ne SHALL rien republier mais renouvelle sa confirmation, et un échec ne SHALL pas être assimilé à une liste vide. Cas « modification de publication » : quand une app retire ou modifie un programme, les scénarios « rafraîchissement automatique », « programme retiré », « programme modifié » et « source en erreur ou absente » de « Contenu héro TV Provider » s'appliquent avec les échéances de « Validité des programmes publiés ».

### Requirement: Panneau Top Shelf au focus
Le panneau SHALL s'ouvrir à la demande au-dessus de la rangée focusée, se fermer si son app n'a rien à montrer et ne réserver aucune place fermé. Le bloc focusé et son défilement SHALL suivre « Bloc focusé de la grille » et « Placement du bloc focusé », sans révéler le héro tant que la grille est active. Les images SHALL être prêtes et valides selon « Contenu du panneau ».

#### Scenario: arrivée dans la grille
- **WHEN** l'utilisateur descend du dock vers la grille
- **THEN** la grille occupe tout l'écran, sans emplacement réservé ni panneau ; à la première entrée, le haut du titre de la première section est sur la marge haute et sa première ligne est juste en dessous

#### Scenario: insertion
- **WHEN** le focus reste environ 3 s sur une tuile dont l'app a des visuels validés et chargés encore valides
- **THEN** le panneau s'insère au-dessus de la rangée focusée (la rangée précédente reste au-dessus du panneau), avec une animation d'expansion ; la rangée focusée reste immobile à l'écran et les rangées précédentes remontent pour laisser la place au panneau

#### Scenario: insertion près du haut
- **WHEN** le panneau s'ouvre au-dessus d'une rangée trop proche du haut pour qu'il tienne sous la marge haute sans déplacer cette rangée
- **THEN** le haut du panneau s'aligne sur la marge haute et la rangée focusée descend juste ce qu'il faut pour lui laisser la place

#### Scenario: déplacement du focus
- **WHEN** un panneau est déjà ouvert et le focus passe à une autre tuile dont l'app a des visuels validés et chargés encore valides
- **THEN** le panneau montre immédiatement les visuels de la nouvelle app, sans délai ni fermeture intermédiaire, et se replace au-dessus de la nouvelle rangée focusée

#### Scenario: fermeture
- **WHEN** le focus passe sur une tuile dont l'app n'a aucun visuel validé et chargé encore valide
- **THEN** le panneau se referme et la grille reprend toute la place ; la rangée focusée reste immobile à l'écran, sauf près du début de la grille, quand il ne reste plus de quoi défiler : la grille revient alors à sa position de départ

#### Scenario: focus toujours visible
- **WHEN** le panneau s'ouvre, se ferme ou change de rangée, ou que le focus change de rangée
- **THEN** la tuile focusée reste entièrement visible entre les marges, selon les scénarios de cette exigence ; la grille ne défile pas davantage

#### Scenario: déplacement dans une rangée
- **WHEN** le focus passe à gauche ou à droite dans la même rangée, sans ouverture ni fermeture du panneau
- **THEN** la grille ne défile pas

#### Scenario: changement de rangée
- **WHEN** le focus passe sur une autre rangée
- **THEN** la grille ne défile pas si le bloc focusé est déjà entre les marges ; sinon elle défile juste ce qu'il faut pour l'y ramener : bas de la rangée sur la marge basse en descendant, haut du bloc sur la marge haute en montant

#### Scenario: titre de section dans le bloc
- **WHEN** le focus arrive sur la première ligne d'une section (rangée Up Next, ou première rangée d'apps), en montant ou en descendant
- **THEN** le titre de cette section fait partie du bloc focusé : il est entièrement visible au-dessus de la ligne, entre les marges, la grille ne défilant que si le bloc n'y est pas déjà

#### Scenario: panneau sous le titre de section
- **WHEN** le panneau s'ouvre au-dessus de la première rangée d'apps alors que le titre « Applications » fait partie du bloc focusé
- **THEN** le bloc est, de haut en bas, le titre « Applications », le panneau puis la rangée ; quand le bloc doit s'aligner sur la marge haute, c'est le haut du titre qui s'y place et la section précédente sort par le haut

#### Scenario: bloc trop grand
- **WHEN** le bloc focusé ne tient pas entre les deux marges
- **THEN** le haut du bloc (le titre de section quand il en fait partie, sinon le haut du panneau s'il est ouvert) s'aligne sur la marge haute

#### Scenario: sortie et retour dans la grille
- **WHEN** l'utilisateur quitte la zone grille (Retour, ou remontée vers le dock ou le héro) puis y revient
- **THEN** la page défile jusqu'au héro (« Navigation 3 paliers »), mais la position de la grille n'est pas remise à zéro : au retour, la page redescend jusqu'à la position de la grille laissée à la sortie, la ligne focusée à la même place à l'écran (le panneau éventuellement ouvert s'est refermé à la sortie), le focus va au dernier élément visité, carte Up Next ou tuile (« Navigation 3 paliers »), et la grille ne défile davantage que si cet élément est hors des marges

**Détails.** — Bloc focusé de la grille : Le bloc focusé SHALL comprendre la ligne focusée (apps ou Up Next), son titre si c'est la première ligne de sa section, et le panneau ouvert au-dessus. L'ordre SHALL rester titre, panneau, ligne selon la maquette 4.2 ; le panneau reste immédiatement au-dessus de la rangée, sous le titre. Cas « première ligne de section » : quand le panneau s'ouvre sur la première rangée d'apps, « titre de section dans le bloc » et « panneau sous le titre de section » de « Panneau Top Shelf au focus » déterminent le placement. — Placement du bloc focusé : Le bloc SHALL rester entre les marges haute de 40 dp et basse de 32 dp (espacement des rangées). La grille SHALL défiler seulement pour le ramener entre elles, avec durée/courbe de l'expansion du panneau. En grille active, elle ne SHALL jamais remonter avant le haut de son premier titre : le héro reste hors écran. Cas « bloc déplacé ou trop grand » : quand le focus ou le panneau change de rangée, ou que le bloc dépasse la hauteur disponible, les scénarios de placement de « Panneau Top Shelf au focus » restent applicables, sans défilement supplémentaire.

### Requirement: Contenu du panneau
Le panneau SHALL n'afficher que des visuels validés, chargés et non expirés de programmes valides de l'app focusée, et ne pas s'ouvrir sinon. Validation, préchargement et affichage SHALL utiliser « Chargement partagé des images » et la politique Top Shelf de « Qualité des visuels ».

#### Scenario: contenu disponible
- **WHEN** l'app focusée a des visuels validés (preview programs / watch next)
- **THEN** chaque affiche apparaît en fondu (300-400ms easing Apple) une fois chargée, puis défile lentement une seule fois (Ken Burns, un seul passage) et reste ensuite immobile sur sa dernière position ; le défilement repart du début à chaque nouvelle affiche

#### Scenario: pas de contenu
- **WHEN** l'app focusée n'a aucun visuel validé
- **THEN** aucun panneau n'est ouvert (ni cadre, ni logo de repli) et la grille occupe tout l'écran

#### Scenario: affiche retirée
- **WHEN** le panneau est ouvert et que son app retire le programme d'une affiche affichée ou en rotation
- **THEN** l'affiche quitte le panneau à la relecture suivante ; le panneau se referme s'il ne lui reste aucune affiche validée et prête

#### Scenario: image en cours ou en échec
- **WHEN** une affiche est en cours de chargement, refusée, absente ou dépasse le délai de récupération
- **THEN** le panneau utilise une autre affiche prête de cette app, sinon reste fermé ou se ferme ; le chargement n'empêche pas de focaliser une autre app et ne produit pas de cadre vide

#### Scenario: changement d'app pendant une panne
- **WHEN** le focus quitte une app avec un panneau ouvert pour une app sans image prête
- **THEN** les images de la première ne servent pas de repli à la seconde ; le panneau se ferme et pourra s'ouvrir quand une image de la nouvelle app sera prête selon « Panneau Top Shelf au focus »

#### Scenario: expiration et validation inachevée
- **WHEN** une image ou son programme expire pendant que le panneau est ouvert et que d'autres validations de l'app sont encore en cours
- **THEN** l'affiche expirée quitte le panneau sans attendre le lot ; une autre affiche prête est montrée, sinon le panneau se ferme ; le focus reste sur la tuile et les règles de placement existantes s'appliquent

#### Scenario: permission perdue ou source absente
- **WHEN** la permission de lecture est perdue ou qu'aucun programme valide de l'app ne peut être obtenu
- **THEN** aucun panneau ne montre ses anciens programmes ; la grille reste navigable sans nouvelle demande de permission déclenchée par le panneau

#### Scenario: reprise au prochain focus
- **WHEN** une image a échoué temporairement et que l'app reçoit de nouveau un focus prolongé après la temporisation de reprise
- **THEN** sa récupération est réessayée par le parcours partagé, sans blacklist locale ni doublon avec une demande du héro ou d'À suivre

### Requirement: Diaporama héro
Le héro SHALL enchaîner automatiquement les programmes publiés, un seul à la fois, plein écran et muet, avec des transitions à la tvOS et un bouton d'ouverture focusable. En-tête, ligne d'infos, progression et bouton SHALL toujours correspondre au programme affiché et changer avec son média ; un programme n'est jamais exclu pour sa seule absence d'image. L'attente et le repli SHALL suivre les règles ci-dessous, qui précisent les textes, médias et échecs.



#### Scenario: défilement automatique
- **WHEN** plusieurs programmes sont disponibles
- **THEN** le héro passe au programme suivant toutes les 12 s environ par fondu croisé (1,4 s, easing Apple : le nouveau visuel apparaît par-dessus l'ancien, sans assombrissement intermédiaire), le visuel courant zoome lentement une seule fois (Ken Burns, un seul passage d'environ 10 s, achevé avant le changement de visuel : au moins 2 s d'image immobile par cycle) puis reste immobile sur sa dernière position, le zoom repartant du début à chaque nouveau visuel, sans son, sans aperçu de l'élément suivant

#### Scenario: navigation manuelle
- **WHEN** l'utilisateur presse gauche/droite sur le héro
- **THEN** le héro passe au programme précédent/suivant avec le même fondu et le minuteur d'avance automatique repart

#### Scenario: vidéo d'aperçu
- **WHEN** le programme expose une vidéo d'aperçu (previewVideoUri)
- **THEN** elle est lue plein écran en muet à la place du poster, repli sur le poster si la lecture échoue, puis sur le média de secours si aucune image n'est utilisable

#### Scenario: métadonnées
- **WHEN** un programme ouvrable est affiché
- **THEN** en bas à gauche apparaissent, de haut en bas : l'en-tête (icône et libellé de l'app source), le titre, la ligne d'infos, la barre de progression suivie du temps restant, chacun seulement s'il a quelque chose à montrer, puis un bouton « Ouvrir » (« Reprendre » si progression) qui porte le focus du héro, au style de « Thème » (scénario « bouton sans flou ») ; OK sur ce bouton ouvre le contenu

#### Scenario: lisibilité sur un poster clair
- **WHEN** le visuel affiché est clair, jusqu'au blanc (pire cas)
- **THEN** un voile sombre en dégradé depuis le bas et depuis la gauche passe derrière le titre, les métadonnées et le bouton, et un léger voile radial assombrit le coin haut droit derrière la capsule heure et réglages ; le texte blanc et le bouton restent lisibles, le reste du visuel n'est pas voilé

#### Scenario: sans progression
- **WHEN** le programme affiché n'expose pas de progression
- **THEN** aucun espace vide ne sépare les métadonnées du bouton : l'écart entre la dernière ligne de métadonnées et le bouton est le même que l'écart entre la barre de progression et le bouton quand elle est présente, et un titre d'une ligne ne réserve pas de place pour une seconde ligne

#### Scenario: retour au programme précédent pendant le fondu
- **WHEN** l'utilisateur revient au programme précédent avant la fin du fondu croisé
- **THEN** le fondu repart de l'opacité courante vers le programme précédent, sans coupure ni saut d'image

#### Scenario: voiles pendant le Ken Burns
- **WHEN** le visuel zoome (Ken Burns)
- **THEN** le voile du coin haut droit derrière la capsule reste fixe et garde son opacité ; les voiles du bas et de la gauche, intégrés au visuel, ne s'affaiblissent pas de façon visible

#### Scenario: changement de programme
- **WHEN** le héro passe d'un programme à un autre
- **THEN** le fondu des médias et celui des textes commencent ensemble, quand l'image, la première image vidéo ou le fond de secours du nouveau programme est prêt ; jusque-là, le titre, les infos et le bouton restent ceux du programme affiché ; les textes de l'ancien programme s'effacent pendant la première moitié du fondu, puis ceux du nouveau apparaissent pendant la seconde : jamais deux titres superposés ; le bas des textes et le bouton ne bougent pas, quel que soit le nombre de lignes des deux titres

#### Scenario: programme en cours
- **WHEN** le programme affiché est un watch next de type `CONTINUE` publié par l'app « Appli » avec son icône, saison 2, épisode 5, durée 42 min, position 17 min
- **THEN** l'en-tête montre l'icône et « Continuer dans Appli », la ligne d'infos « Saison 2 · Épisode 5 · 42 min », la barre de progression est suivie de « Reste 25 min » et le bouton est « Reprendre »

#### Scenario: épisode suivant, nouveauté, liste de lecture
- **WHEN** le programme affiché est un watch next de type `NEXT`, `NEW` ou `WATCHLIST` de l'app « Appli »
- **THEN** l'en-tête montre respectivement « Épisode suivant dans Appli », « Nouveau dans Appli » ou « À regarder dans Appli »

#### Scenario: programme mis en avant par l'app
- **WHEN** le programme affiché est un preview program de l'app « Appli »
- **THEN** l'en-tête montre l'icône et « Appli » seul

#### Scenario: type absent ou inconnu
- **WHEN** le programme affiché est un watch next sans `watch_next_type` ou avec une valeur hors des quatre types d'Android
- **THEN** l'en-tête montre l'icône et le nom de l'app seuls, comme un programme mis en avant ; le rattachement de ces programmes à un groupe de tri dans Up Next (« Champs facultatifs » de `up-next`) ne change pas ce libellé

#### Scenario: nom ou icône de l'app manquants
- **WHEN** le nom de l'app source est inconnu, ou son icône ne peut pas être chargée
- **THEN** seul ce qui est disponible est affiché : l'icône avec le type seul (« Continuer », « Épisode suivant », « Nouveau », « À regarder ») ou sans libellé pour un programme mis en avant, ou le libellé sans icône ; sans icône ni libellé, l'en-tête disparaît, sans espace vide

#### Scenario: infos partielles
- **WHEN** l'app ne publie qu'une partie de la saison, de l'épisode et de la durée
- **THEN** la ligne d'infos ne montre que ces éléments (par exemple « Épisode 3 » ou « 1 h 35 min ») ; sans aucun d'eux, il n'y a pas de ligne d'infos et le titre est directement suivi de la progression ou du bouton

#### Scenario: progression sans position ou sans durée
- **WHEN** la position de lecture ou la durée n'est pas publiée
- **THEN** ni barre de progression ni temps restant ne sont affichés, sans espace vide (scénario « sans progression »)

#### Scenario: textes avec le visuel
- **WHEN** l'image du programme suivant met 3 s à charger et réussit avant son échéance
- **THEN** pendant ces 3 s, le titre, les infos et le bouton restent ceux du programme affiché et OK ouvre ce programme ; ils ne changent qu'avec le fondu vers le nouveau visuel

#### Scenario: visuel lent
- **WHEN** l'image du programme demandé n'est pas prête mais que son budget de récupération n'est pas encore écoulé
- **THEN** une seule récupération commune continue, le média précédent et ses textes restent cohérents, et une navigation vers un autre programme prêt ne l'attend pas ; à l'échec ou à l'échéance, le programme demandé utilise son repli s'il est toujours sélectionné et valide

#### Scenario: textes immobiles pendant le Ken Burns
- **WHEN** le visuel du programme zoome (Ken Burns)
- **THEN** l'en-tête, le titre, la ligne d'infos et la progression ne sont pas recomposés

#### Scenario: image indisponible et repli nature
- **WHEN** toutes les candidates du programme échouent ou dépassent le délai et qu'aucune copie locale n'est utilisable
- **THEN** le programme est affiché sur la vidéo nature de secours avec ses propres textes et sa propre cible d'ouverture ; le défilement continue vers les programmes des autres apps et les flèches restent actives

#### Scenario: repli sans réseau
- **WHEN** l'image du programme et les clips nature sont indisponibles
- **THEN** le fond neutre local porte les textes et le bouton du programme, sans écran noir ni attente indéfinie ; le chargement de la vidéo de secours ne retarde pas la navigation

**Détails.** — En-tête du héro : L'en-tête SHALL précéder le titre selon la maquette 960 × 540 dp : icône carrée sans masque de 18 dp, coins 4,5 dp, puis libellé par type publié. Pour l'app X : CONTINUE (`WATCH_NEXT_TYPE_CONTINUE`) « Continuer dans X », NEXT « Épisode suivant dans X », NEW « Nouveau dans X », WATCHLIST « À regarder dans X », preview ou type absent/inconnu « X ». Il SHALL ne montrer que les éléments disponibles. Cas « données manquantes de l'en-tête » : quand le nom ou l'icône manque, le type seul ou le libellé sans icône est montré, et sans les deux l'en-tête disparaît ; les scénarios de types et de nom/icône manquants de « Diaporama héro » s'appliquent. — Infos et progression du héro : La ligne sous le titre SHALL réunir seulement saison, épisode et durée publiés, séparés par « · ». La durée SHALL être arrondie à la minute (minimum 1 min), en heures au-delà d'une heure (« 1 h 35 min », « 2 h »). Barre et « Reste X min » SHALL exister seulement avec position et durée connues ; le restant est arrondi à la minute supérieure avec la même écriture des heures. Cas « infos partielles » : quand les données du programme sont partielles, « infos partielles », « progression sans position ou sans durée » et « programme en cours » de « Diaporama héro » s'appliquent sans réserver d'espace aux lignes absentes. — Synchronisation du héro : Titre, en-tête, infos et bouton SHALL correspondre au programme affiché, même sur son média de secours ; « Ouvrir »/« Reprendre » SHALL ouvrir ce programme et changer avec son média, jamais avant. Les textes SHALL rester fixes sans recomposition ni dessin pendant le Ken Burns, sans espace pour les lignes absentes et lisibles sur visuel clair grâce aux voiles. Cas « changement et animation » : quand le média change ou zoome, « changement de programme », « textes avec le visuel », « textes immobiles pendant le Ken Burns » et « lisibilité sur un poster clair » de « Diaporama héro » s'appliquent aussi au média de secours, sans image ou cible d'un autre programme. — Visuel du héro en attente : L'attente d'image SHALL suivre le délai partagé, sans bloquer navigation ni médias prêts ; à l'échec/échéance, « Repli du programme du héro » SHALL s'appliquer. Une vidéo suivante sans première image après 1 s SHALL rester préparée en pause sans doublon tant qu'une autre n'est pas demandée, et laisser passer le prochain média utilisable ; prête, elle redevient éligible. La première vidéo sans média à remplacer reste exemptée. Cas « préparation lente » : quand le visuel suivant n'est pas prêt, une image continue dans son budget commun et utilise le repli à l'échéance si son programme est toujours demandé et valide ; une vidéo suit son délai de 1 s ; une image préparée n'est pas retenue en bitmap par le héro et est relue depuis le cache à son retour.

### Requirement: Fond vidéo de secours
Le fallback du héro SHALL être une liste de clips nature libres de droits (Pexels, 2K sinon 1080p, mp4 H.264 en https) lus en boucle par un unique lecteur, muet. Il SHALL pouvoir servir de média de secours d'un programme sans image utilisable sans remplacer l'identité de ce programme.

#### Scenario: lecture
- **WHEN** le fallback est actif
- **THEN** un seul lecteur lit la playlist en boucle et muet, un clip en erreur est ignoré au profit du suivant, le lecteur est mis en pause quand la grille est active et libéré quand le héro est démonté

#### Scenario: aucun clip lisible
- **WHEN** aucun clip ne peut être lu (réseau absent, URL morte)
- **THEN** le dégradé sombre animé reste affiché, sans écran noir ni crash ; les tentatives et la préparation des clips ont un délai borné qui ne bloque pas le défilement

#### Scenario: vidéo de secours d'un programme
- **WHEN** un programme valide utilise une vidéo nature comme média de secours
- **THEN** ses textes et sa cible d'ouverture restent ceux du programme ; sans programme valide, le fallback global n'affiche aucun ancien titre ni bouton vers un contenu expiré

### Requirement: Qualité des visuels
Le héro SHALL privilégier la vidéo d'aperçu, large d'au moins 1080 px ; ses images SHALL rester paysage, au moins 640 px après réduction dans 1920 × 1080 sans agrandissement. Le panneau SHALL suivre « Qualité des affiches Top Shelf ». Les dimensions communes ne SHALL pas imposer un verdict global ; un rejet mène aux candidates puis au repli du héro ou à l'exclusion de l'affiche, sans abaisser les seuils du héro.

#### Scenario: image trop petite
- **WHEN** une image du programme ne satisfait pas la politique de dimensions de sa destination, est portrait ou ne peut pas être chargée
- **THEN** cette image n'est pas affichée dans le héro ou le panneau concernés ; une autre candidate est essayée, puis le héro utilise son repli et le panneau écarte l'affiche si aucune candidate ne convient

#### Scenario: variante YouTube
- **WHEN** l'image d'un programme est une miniature YouTube (`i.ytimg.com`, `iN.ytimg.com` ou `img.youtube.com`, chemin `/vi/<id>/` ou `/vi_webp/<id>/`)
- **THEN** la variante `https://i.ytimg.com/vi/<id>/maxresdefault.jpg` est vérifiée d'abord pour le héro et le panneau ; acceptée selon la politique de destination, c'est elle qui s'affiche ; absente ou insuffisante, les candidates publiées sont vérifiées ; un échec réseau laisse la récupération à revérifier selon les règles communes

#### Scenario: vidéo trop petite
- **WHEN** la vidéo d'aperçu d'un programme fait moins de 1080 px de large
- **THEN** le héro replie sur les candidates image du programme, puis sur son média de secours si aucune ne convient

#### Scenario: image à la largeur du panneau
- **WHEN** une image paysage a une largeur réelle égale à la largeur affichée du Top Shelf en pixels, hors zoom
- **THEN** elle est acceptée pour le panneau ; une image plus étroite ne l'est pas même si elle satisfait la politique du héro ou d'une carte À suivre

### Requirement: Préchargement et mémoire
Le launcher SHALL borner la validation : premiers visuels du héro au chargement, affiches d'une app au focus posé. Il SHALL décoder à la destination, jamais à la dalle, et garder « Fluidité de release »/« Fenêtre de composition de la grille ». Récupérations, dimensions et échéances SHALL être partagées entre les trois zones selon « Chargement partagé des images » et « Cache borné et fraîcheur des images ».

#### Scenario: validation au chargement
- **WHEN** les programmes sont chargés
- **THEN** seuls les premiers visuels du héro sont vérifiés en arrière-plan, par files d'apps selon le contrat partagé, d'après les dimensions de l'en-tête sans décoder les pixels ; les octets vérifiés restent en cache disque, aucun visuel n'est préchargé en mémoire ; le provider pouvant publier des centaines de programmes, aucun autre lot n'est validé à ce moment

#### Scenario: validation à la demande
- **WHEN** le focus reste au moins 0,5 s sur une tuile de la grille
- **THEN** quelques affiches candidates de cette app sont vérifiées selon la politique Top Shelf ; les octets et dimensions connus sont réutilisés tant qu'ils restent valides, seules les affiches acceptées alimentent son panneau et un focus qui ne fait que passer ne lance aucune vérification

#### Scenario: validation interrompue ou nouvelles affiches
- **WHEN** la vérification des affiches d'une app a été interrompue (focus posé sur une autre app avant la fin), ou les programmes rechargés proposent d'autres affiches candidates pour cette app
- **THEN** au prochain focus prolongé sur l'app, les candidates encore inconnues ou à réessayer sont traitées ; les résultats de version encore valides et les récupérations encore demandées par une autre zone sont partagés, sans nouvelle ouverture identique

#### Scenario: visuel injoignable
- **WHEN** la validation ou le chargement d'une image d'une des trois zones échoue pour une raison temporaire, notamment erreur d'entrée-sortie, HTTP 5xx ou délai de fournisseur
- **THEN** une copie locale encore utilisable sert de secours, sinon le repli de la zone s'affiche ; la demande peut être réessayée à une relecture pertinente, au retour au premier plan ou au prochain focus prolongé, avec une temporisation commune et sans requête concurrente dupliquée

#### Scenario: visuel rejeté
- **WHEN** une version d'image est trop étroite pour un usage, refusée par le serveur ou impossible à décoder
- **THEN** le rejet est conservé pour cette version et cette politique sans boucle de requêtes ; les autres usages et candidates restent possibles, une nouvelle publication ou une revalidation à l'échéance permet une nouvelle tentative, et une URI expirée n'est jamais reconstruite

#### Scenario: économie de ressources
- **WHEN** le héro est masqué à l'intérieur du launcher (zone grille, page de réglages) ou une surface verre est invisible
- **THEN** la lecture de la vidéo est interrompue (arrêtée, ou mise en pause à sa position quand une exigence le prévoit) sans que le lecteur vidéo soit libéré, le launcher étant au premier plan ; les animations de fond, le Ken Burns du visuel du héro et le flou d'arrière-plan sont arrêtés ; les images sont décodées avec au plus deux décodeurs simultanés

#### Scenario: launcher en arrière-plan
- **WHEN** une autre app passe au premier plan et le launcher n'est plus visible
- **THEN** le lecteur vidéo du héro est libéré, ce qui rend le décodeur vidéo à l'app au premier plan ; le défilement automatique du diaporama et les délais du héro (visuel lent, démarrage d'une vidéo) sont suspendus ; aucun nouveau lot de prélecture n'est démarré, les récupérations déjà ouvertes gardent leur échéance physique et les TTL ne sont pas suspendus

#### Scenario: retour au premier plan après libération
- **WHEN** le launcher revient au premier plan après avoir libéré le lecteur vidéo
- **THEN** la vidéo d'aperçu du programme affiché encore valide est rechargée et reprend, sans compter comme un échec ni faire passer le héro au programme suivant ; les images et programmes sont contrôlés selon leurs échéances avant réaffichage, le défilement automatique et les délais de présentation repartent de zéro ; une vidéo lente dont la préparation a été interrompue redevient éligible au diaporama

#### Scenario: écran au repos
- **WHEN** aucune touche n'est pressée et aucune transition n'est en cours
- **THEN** seules les animations spécifiées encore visibles redessinent l'écran (vidéo d'aperçu, passage unique du dégradé animé du repli, passage unique du Ken Burns d'un nouveau visuel du héro ou du panneau Top Shelf, changement de visuel du défilement automatique) ; une fois ces passages terminés, l'image est immobile ; aucune animation d'un élément hors écran ne tourne, et une animation continue ne provoque aucune recomposition à chaque image ; les contrôles d'expiration se déclenchent aux échéances utiles, pas par frame

#### Scenario: taille de décodage
- **WHEN** un visuel du héro, une affiche du panneau Top Shelf ou une bannière d'app est décodé, même publié en 3840 × 2160
- **THEN** le visuel du héro est décodé à la taille de la fenêtre, au plus 1920 × 1080, l'affiche à la taille du panneau (en le couvrant), et la bannière à la taille de la tuile de la grille au zoom de focus, en pixels de l'appareil, au plus 480 × 270

#### Scenario: une passe par image
- **WHEN** le héro affiche le visuel d'un programme, au repos, pendant le Ken Burns ou un fondu
- **THEN** les voiles du bas et de la gauche sont intégrés au visuel décodé et le voile du coin haut droit est dessiné seul sur sa petite zone ; ni fond de fenêtre, ni fond noir, ni dégradé du repli, ni voile n'est dessiné sous un visuel opaque, et aucun fondu ne passe par un calque plein écran hors écran

#### Scenario: écran de démarrage
- **WHEN** l'écran de démarrage est affiché
- **THEN** seuls le catalogue et les premiers candidats bornés du héro sont préparés en priorité ; après le premier visuel utilisable, aucune nouvelle validation de prélecture n'est démarrée avant la fin de l'écran de démarrage ; les travaux déjà ouverts restent bornés par le parallélisme partagé, les bannières sont décodées sur un fil de basse priorité et la grille n'est composée qu'après le fondu vers l'accueil

#### Scenario: navigation dans la grille
- **WHEN** l'utilisateur déplace le focus de tuile en tuile et de rangée en rangée dans la grille
- **THEN** seules les tuiles qui prennent ou perdent le focus et la rangée concernée sont recomposées, l'animation du focus ne recompose rien, et la validation des images reste commune et sans effet sur les autres rangées

#### Scenario: grande grille
- **WHEN** le catalogue compte plus de 12 rangées
- **THEN** le nombre de tuiles composées dépend de la hauteur de l'écran et de la prélecture, pas du nombre d'apps ; les bannières des rangées composées sont lues dans le cache borné des visuels d'apps ou décodées sur le fil de basse priorité, celles des rangées hors fenêtre ne sont ni composées ni décodées

#### Scenario: rangée visée hors fenêtre
- **WHEN** le focus vise une rangée non composée : rafale de touches plus rapide que le défilement, retour dans la grille sur la rangée laissée (héro, menu contextuel, réglages, retour d'une app), app déplacée d'une rangée hors fenêtre, rangée Up Next placée après les apps
- **THEN** la rangée visée est composée dès que l'ancre la désigne, avant la demande de focus, et la tuile ou la carte reçoit le focus par son identité à la place prévue par « Panneau Top Shelf au focus » ; un retour rapide en arrière retrouve la rangée précédente encore composée

#### Scenario: panneau sur une rangée profonde
- **WHEN** le panneau Top Shelf s'ouvre sur une rangée d'une grande grille
- **THEN** le bloc focusé est placé comme sur une petite grille ; pendant le délai d'ouverture, la première affiche validée est décodée à la taille du panneau avec la demande partagée de l'affichage, pour que l'expansion montre une image prête ; les ajouts attendent la fin de la validation sans changer l'affiche courante, mais les retraits et expirations s'appliquent immédiatement ; seules la rangée dont le panneau est ouvert et celle dont il se ferme portent son animation

#### Scenario: fraîcheur du cache disque
- **WHEN** une image est demandée par le héro, le Top Shelf ou À suivre
- **THEN** ses octets, dimensions et dates suivent « Cache borné et fraîcheur des images », y compris pour une URI `content://`, une entrée mémoire ou une image déjà affichée ; une validation ne provoque pas une seconde ouverture du fournisseur lors de l'affichage

**Détails.** — Reprise des visuels du héro : Une vidéo d'aperçu en échec ne SHALL pas être redemandée pendant la session. Les images SHALL suivre les reprises et expirations communes, sans blacklist locale permanente après une panne temporaire. Un rejet de dimensions SHALL porter sur l'usage et la version ; les candidates alternatives restent possibles. Cas « classement de l'échec » : quand un visuel échoue, les scénarios « visuel injoignable » et « visuel rejeté » de « Préchargement et mémoire » déterminent la reprise selon version, cause et échéance, sans empêcher les autres usages. — Fluidité de release : Sur la TV de référence et l'APK release, le héro au repos/au focus du dock SHALL atteindre au moins 55 images/s et moins de 10 % d'images en retard. La navigation grille/panneau SHALL avoir moins de 10 % de retard : seules les tuiles et la rangée concernées sont recomposées, sans décodage ni validation avant le focus posé. Cas « navigation et repos » : quand les scénarios « navigation dans la grille » et « écran au repos » de « Préchargement et mémoire » sont mesurés, ces seuils s'appliquent sans animations hors écran ni recomposition par frame. — Fenêtre de composition de la grille : Jusqu'à 12 rangées, Up Next comprise, toutes SHALL être composées ; au-delà, seulement celles couvrant l'écran aux positions courante et cible, plus 2 rangées de prélecture de chaque côté, les autres remplacées par leur hauteur. La page SHALL garder une seule autorité verticale ; grille, verre, ambiance, placement, navigation, persistance et repli du focus conservent leurs règles. Cas « grande grille et rangée visée » : quand la grille dépasse 12 rangées, « grande grille », « rangée visée hors fenêtre » et « panneau sur une rangée profonde » de « Préchargement et mémoire » s'appliquent, la grille restant source du verre et du dégradé d'ambiance.

## ADDED Requirements

### Requirement: Qualité des affiches Top Shelf
Le Top Shelf SHALL accepter une image paysage dont la largeur réelle atteint celle du panneau affiché en pixels, hors zoom, à partir des dimensions de « Chargement partagé des images ». Le refus pour le panneau ne SHALL pas interdire l'usage de la même image dans le héro ou une carte selon leurs propres politiques.

#### Scenario: seuil exact du panneau
- **WHEN** la largeur réelle de l'image atteint exactement la largeur du panneau
- **THEN** elle est acceptée ; juste sous cette largeur elle est refusée pour le panneau, même si une autre destination l'accepte

### Requirement: Repli du programme du héro
Sans candidate ni copie locale utilisable à l'échec/échéance, le héro SHALL afficher le programme demandé avec la vidéo nature de « Fond vidéo de secours », puis le fond neutre si aucun clip n'est lisible dans le délai borné. Le fond neutre SHALL couvrir la préparation du clip sans noir ; titre et ouverture restent ceux du programme, et navigation/défilement continuent.

#### Scenario: fournisseur et réseau indisponibles
- **WHEN** image et vidéos nature sont indisponibles pour un programme toujours valide
- **THEN** le fond neutre local porte son titre et son bouton ; le programme suivant d'une autre app reste accessible sans attendre un lot d'images

### Requirement: Ordre aléatoire du héro
L'ordre des programmes du héro SHALL être tiré au hasard au démarrage du processus et rester stable pendant la session. Un retour au premier plan ou une relecture ne SHALL pas remélanger les programmes présents. Un nouveau programme SHALL être inséré au hasard après le programme affiché, sans réordonner les autres ; un programme disparu ou expiré SHALL être retiré en préservant leur ordre relatif.

#### Scenario: nouveau lancement
- **WHEN** SygixOs est relancé (nouveau processus) avec les mêmes programmes publiés
- **THEN** l'ordre du héro est tiré de nouveau au hasard ; le premier programme n'est pas toujours le même

#### Scenario: retour au premier plan
- **WHEN** l'utilisateur revient sur SygixOs pendant la même session, avec ou sans rechargement du TV Provider
- **THEN** les programmes déjà présents gardent leur ordre

#### Scenario: programme ajouté pendant la session
- **WHEN** une app publie un nouveau programme pendant la session
- **THEN** il est inséré à une position tirée au hasard après le programme affiché, sans réordonner les autres

#### Scenario: retrait, expiration et liste vide
- **WHEN** un programme est retiré ou expire, puis que la liste devient vide
- **THEN** l'ordre relatif des programmes restants est conservé, puis le fallback global s'affiche sans ancien titre ; aucun ordre n'est reconstruit à partir du cache d'images

### Requirement: Images d'autres applications sans dépendance forte
SygixOs SHALL accéder aux images `content://` tierces sans dépendance forte au fournisseur. L'ouverture et la lecture complète SHALL avoir un délai de résultat borné, même si le fournisseur ignore l'annulation. Une panne ne SHALL ni arrêter SygixOs ni bloquer le rendu ; les copies locales utilisables, candidates alternatives et replis SHALL suivre le contrat partagé.

#### Scenario: fournisseur bloqué puis arrêté par le système
- **WHEN** le fournisseur d'images d'une application ne répond pas (serveur de l'application injoignable) et que le système arrête cette application pour ANR pendant qu'une de ses images est en cours d'ouverture
- **THEN** SygixOs reste lancé ; la récupération concernée échoue et la zone utilise sa copie locale utilisable, une candidate alternative ou son repli sans bloquer les autres apps

#### Scenario: délai dépassé
- **WHEN** l'ouverture ou la lecture du flux d'une image `content://` tierce dépasse le délai
- **THEN** le résultat d'image échoue au plus tard à l'échéance et l'annulation est déclenchée indépendamment de l'appel bloqué ; la zone passe à sa solution de repli sans bloquer le rendu

#### Scenario: appel Binder bloqué et résultat tardif
- **WHEN** `openTypedAssetFile` reste bloqué après l'échéance malgré la demande d'annulation, puis retourne un descripteur
- **THEN** le résultat du chargement a déjà échoué ; le repli reste actif, les ressources tardives sont fermées exactement une fois, aucune entrée de cache n'est engagée et ce résultat n'est jamais publié à un consommateur

#### Scenario: pool de lecture saturé
- **WHEN** tous les workers bornés sont encore bloqués dans des appels de fournisseurs
- **THEN** une nouvelle récupération échoue rapidement vers le repli de sa zone, sans créer de thread ou de file non bornés et sans bloquer le rendu ; les copies locales encore utilisables restent accessibles

#### Scenario: image disponible
- **WHEN** le fournisseur répond dans le délai
- **THEN** ses octets et dimensions sont conservés selon le cache partagé, puis l'image s'affiche si elle satisfait la politique de sa destination

#### Scenario: permission refusée ou fournisseur absent
- **WHEN** l'accès à l'image est refusé ou que le fournisseur n'existe plus
- **THEN** l'échec est distingué d'une image vide ; les alternatives et replis sont utilisés sans crash ni demande d'autorisation non prévue

### Requirement: Chargement partagé des images
Le héro, le Top Shelf et À suivre SHALL partager récupération, cache, dimensions et reprises d'images. Une même version demandée simultanément SHALL être récupérée et analysée une seule fois. Chaque zone SHALL appliquer sa politique de qualité aux dimensions communes et décoder à sa taille. Les récupérations SHALL être séquentielles par app, parallèles entre apps sous une constante globale, et bornées en délai et en file.

#### Scenario: validation, préchargement et affichage communs
- **WHEN** plusieurs zones demandent une même image, y compris pendant la validation et le préchargement du Top Shelf
- **THEN** elles partagent la récupération et les dimensions de cette version sans rouvrir le fournisseur ; chaque destination conserve ses propres taille et transformations de décodage

#### Scenario: résultat de qualité propre à l'usage
- **WHEN** une image ne convient pas au héro mais convient à une carte, ou qu'elle convient au héro sans atteindre la largeur du panneau
- **THEN** son refus dans une destination n'empêche pas son acceptation dans une autre ; aucune validation d'usage n'est utilisée comme rejet global

#### Scenario: apps parallèles et app lente
- **WHEN** plusieurs apps ont des images à charger et qu'une app ne répond pas
- **THEN** les apps saines progressent sans attendre son lot ; une seule récupération physique existe par app, la concurrence totale respecte la constante commune et aucun composable ne crée une limite indépendante

#### Scenario: délai et alternatives
- **WHEN** une demande attend en file, tente une première candidate puis une alternative
- **THEN** son résultat reste borné par un budget unique, jamais remis à zéro entre candidates ou abonnés ; une récupération expirée n'empêche pas la navigation ni les images déjà prêtes

#### Scenario: travail encore demandé ailleurs
- **WHEN** l'utilisateur quitte une zone pendant qu'une autre attend la même récupération
- **THEN** le travail commun reste disponible pour l'autre zone sans seconde requête ; les travaux en attente sans consommateur sont retirés

#### Scenario: affiche et miniature publiées
- **WHEN** l'app fournit affiche et miniature et que la candidate préférée manque, échoue ou est refusée pour la destination
- **THEN** l'autre candidate est essayée dans le budget restant avant le repli ; le ratio déclaré ne fait pas perdre l'alternative et les dimensions réelles font autorité

#### Scenario: reprise après panne temporaire
- **WHEN** une demande échoue temporairement et qu'un événement de rafraîchissement ou de focus prolongé autorise une reprise après temporisation
- **THEN** une nouvelle tentative commune est possible sans blacklist locale permanente ; elle n'est pas lancée si une ancienne récupération physique de la même app reste bloquée

### Requirement: Cache borné et fraîcheur des images
Les images HTTP(S) et `content://` obtenues SHALL être conservées dans un cache privé borné avec dimensions et date de récupération réussie. Une copie utilisable SHALL s'afficher immédiatement, rester fraîche moins de 24 h et pouvoir servir en secours jusqu'à moins de 7 jours. À 7 jours elle ne SHALL plus être affichée sans récupération réussie. Lecture, validation depuis le cache et affichage ne SHALL jamais renouveler cette date.

#### Scenario: entrée fraîche et reprise après redémarrage
- **WHEN** une image fraîche est disponible sur disque, y compris après recréation du chargeur ou redémarrage du processus
- **THEN** elle peut être affichée sans demande préalable au fournisseur et sans nouvelle analyse des dimensions de cette version ; l'éviction normale du cache peut toutefois avoir supprimé l'entrée

#### Scenario: entrée ancienne encore utilisable
- **WHEN** une copie a au moins 24 h et moins de 7 jours et reste utilisable pour un programme valide
- **THEN** elle est affichée immédiatement pendant un rafraîchissement commun borné ; le succès remplace les octets et la date, l'échec conserve la dernière copie valide sans prolonger son âge

#### Scenario: image expirée même déjà affichée
- **WHEN** une copie atteint exactement 7 jours sans récupération réussie
- **THEN** elle cesse d'être affichable, même déjà en mémoire ou à l'écran ; les candidates et replis de la zone sont utilisés sans attendre le prochain changement de programme

#### Scenario: nouvelle version et écriture interrompue
- **WHEN** de nouveaux octets sont récupérés sous la même URI, ou que leur copie ou décodage échoue
- **THEN** une nouvelle version utilisable remplace l'ancienne et invalide ses dimensions, verdicts et bitmaps ; une copie incomplète ou indécodable n'efface pas la dernière version décodable encore autorisée par son âge

#### Scenario: association au contenu
- **WHEN** deux films distincts ont des titres proches ou identiques, ou que plusieurs épisodes publient la même image de série
- **THEN** le cache n'échange pas les images des films ; les épisodes peuvent partager les octets d'une image de série explicitement associée par la source, sans inventer d'identifiant ni réécrire une URI signée

#### Scenario: métadonnées manquantes ou dates incohérentes
- **WHEN** une entrée héritée n'a pas de date de récupération fiable ou porte une date future incohérente
- **THEN** elle n'est pas déclarée fraîche par défaut et doit être revalidée avant de bénéficier d'une nouvelle période de validité

#### Scenario: arrière-plan puis retour
- **WHEN** le launcher revient au premier plan après le franchissement d'une échéance d'image
- **THEN** l'âge est contrôlé avant de réafficher une copie et les tentatives reprennent selon le contrat commun ; le temps d'arrière-plan ne suspend pas le TTL

### Requirement: Validité des programmes publiés
Le héro, le Top Shelf et À suivre SHALL suivre la dernière confirmation réussie de chaque table du TV Provider. Une lecture réussie SHALL retirer les absents et confirmer les présents, même inchangés. Une erreur SHALL ne pas confirmer de résultat ; un ancien programme SHALL expirer après 24 h sans confirmation. Le cache d'images ne SHALL jamais réintroduire un programme retiré, expiré ou inaccessible.

#### Scenario: lecture réussie et liste vide
- **WHEN** la lecture d'une table réussit, avec une liste complète ou vide
- **THEN** seuls les programmes affichables encore présents de cette table sont confirmés ; les absents sont retirés des trois zones sans délai de grâce, même si leur image est locale

#### Scenario: panne avant le plafond
- **WHEN** une table ne peut pas être relue et que son dernier résultat confirmé a moins de 24 h
- **THEN** les anciens programmes encore accessibles peuvent être conservés sans être confondus avec un résultat vide ; cette panne ne renouvelle pas leur confirmation

#### Scenario: expiration du programme affiché
- **WHEN** un programme atteint exactement 24 h sans confirmation alors qu'il est sélectionné dans le héro, affiché dans un panneau ou présent dans À suivre
- **THEN** il est retiré de la surface active ; le héro passe à un autre programme valide ou au fallback global sans ancien texte, le panneau utilise une autre affiche valide sinon se ferme, et À suivre suit ses états et règles de focus existants

#### Scenario: confirmations indépendantes et âge du visionnage
- **WHEN** seule une table est relue avec succès, ou qu'un programme ancien dans l'historique de lecture reste publié
- **THEN** la confirmation ne prolonge que le résultat de cette table ; un programme confirmé n'est pas retiré en raison de sa seule date d'engagement

#### Scenario: permission perdue ou app désinstallée
- **WHEN** la permission de lecture est retirée ou qu'une app source est désinstallée
- **THEN** les programmes concernés deviennent inaccessibles selon les règles existantes sans bénéficier du délai de grâce ; les préférences, retraits utilisateur et mémoire d'ouverture ne sont pas purgés comme après une lecture vide réussie

#### Scenario: génération retirée et réponse tardive
- **WHEN** une image finit de charger après le retrait, l'expiration ou le remplacement de son programme
- **THEN** son résultat ne repeuple aucune liste et ne remplace pas le média d'un autre programme ; l'affichage contrôle encore l'identité et la génération courantes

#### Scenario: reprise au premier plan sans nouveau signal
- **WHEN** le launcher revient au premier plan après l'expiration d'un ancien résultat, sans notification du provider
- **THEN** l'échéance est contrôlée avant réaffichage puis une relecture bornée est lancée ; aucun ancien résultat n'est rendu frais par son simple retour à l'écran
