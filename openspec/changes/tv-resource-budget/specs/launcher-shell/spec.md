## MODIFIED Requirements

### Requirement: Diaporama héro
Le héro SHALL enchaîner automatiquement les programmes publiés, un seul à la fois, plein écran et muet, avec un bouton d'ouverture focusable ; en-tête, infos, progression et bouton SHALL correspondre au programme affiché et changer avec son média. Un programme SHALL y entrer quand sa vidéo d'aperçu ou un visuel accepté par la politique du héro est prêt, ou sur son média de secours quand ses images ont échoué ; un programme sans visuel acceptable SHALL en être écarté.

#### Scenario: défilement automatique
- **WHEN** plusieurs programmes sont disponibles
- **THEN** le héro passe au programme suivant toutes les 12 s environ par fondu croisé (1,4 s, easing Apple : le nouveau visuel apparaît par-dessus l'ancien, sans assombrissement intermédiaire), le visuel courant zoome lentement une seule fois (Ken Burns, un seul passage d'environ 10 s, achevé avant le changement de visuel : au moins 2 s d'image immobile par cycle) puis reste immobile sur sa dernière position, le zoom repartant du début à chaque nouveau visuel, sans son, sans aperçu de l'élément suivant ; le zoom n'a lieu que si « Mouvement des visuels » est activé et que les animations du système ne sont pas coupées

#### Scenario: mouvement des visuels désactivé
- **WHEN** « Mouvement des visuels » est désactivé ou que l'échelle d'animation du système est à 0
- **THEN** chaque visuel du héro est affiché immobile à sa taille d'origine, sans Ken Burns ; le fondu croisé de 1,4 s, le défilement toutes les 12 s environ, les textes et les vidéos d'aperçu sont inchangés, et entre deux changements de programme aucune image n'est redessinée tant qu'aucune vidéo ne joue

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
- **WHEN** l'image du programme suivant met 3 s à charger puis réussit
- **THEN** pendant ces 3 s, le titre, les infos et le bouton restent ceux du programme affiché et OK ouvre ce programme ; ils ne changent qu'avec le fondu vers le nouveau visuel

#### Scenario: visuel lent
- **WHEN** l'image d'un programme n'est pas encore prête, en attente de son tour ou en cours de récupération
- **THEN** le défilement et la navigation passent ce programme sans l'attendre ; une seule récupération commune continue et le programme entre dans le diaporama dès que son image est prête, ou avec son repli si la requête échoue

#### Scenario: programme sans visuel acceptable
- **WHEN** un programme sans vidéo ne publie que des candidates refusées par la politique du héro (portrait seul, trop petites) ou reconnues comme images génériques
- **THEN** il n'apparaît pas dans le héro, sans repli nature ; il reste dans le Top Shelf et À suivre si leurs politiques l'acceptent ; sans autre programme, le fallback global s'affiche

#### Scenario: textes immobiles pendant le Ken Burns
- **WHEN** le visuel du programme zoome (Ken Burns)
- **THEN** l'en-tête, le titre, la ligne d'infos et la progression ne sont pas recomposés

#### Scenario: image indisponible et repli nature
- **WHEN** la récupération de toutes les candidates du programme échoue (erreur ou délai de requête dépassé) et qu'aucune copie locale n'est utilisable
- **THEN** le programme est affiché sur la vidéo nature de secours avec ses propres textes et sa propre cible d'ouverture ; le défilement continue vers les programmes des autres apps et les flèches restent actives

#### Scenario: programmes en repli consécutifs
- **WHEN** plusieurs programmes affichés à la suite utilisent la vidéo nature
- **THEN** le même clip continue sans redémarrer ni se recharger pendant que leurs textes changent toutes les 12 s environ ; à la fin du clip, le clip suivant de la playlist prend le relais sans changer de programme ; quand un programme avec image ou vidéo s'affiche, le clip est arrêté et repart du début au repli suivant

#### Scenario: repli sans réseau
- **WHEN** l'image du programme et les clips nature sont indisponibles
- **THEN** le fond neutre local porte les textes et le bouton du programme, sans écran noir ni attente indéfinie ; le chargement de la vidéo de secours ne retarde pas la navigation

**Détails.** — En-tête du héro : L'en-tête SHALL précéder le titre selon la maquette 960 × 540 dp : icône carrée sans masque de 18 dp, coins 4,5 dp, puis libellé par type publié. Pour l'app X : CONTINUE (`WATCH_NEXT_TYPE_CONTINUE`) « Continuer dans X », NEXT « Épisode suivant dans X », NEW « Nouveau dans X », WATCHLIST « À regarder dans X », preview ou type absent/inconnu « X ». Il SHALL ne montrer que les éléments disponibles. Cas « données manquantes de l'en-tête » : quand le nom ou l'icône manque, le type seul ou le libellé sans icône est montré, et sans les deux l'en-tête disparaît ; les scénarios de types et de nom/icône manquants de « Diaporama héro » s'appliquent. — Infos et progression du héro : La ligne sous le titre SHALL réunir seulement saison, épisode et durée publiés, séparés par « · ». La durée SHALL être arrondie à la minute (minimum 1 min), en heures au-delà d'une heure (« 1 h 35 min », « 2 h »). Barre et « Reste X min » SHALL exister seulement avec position et durée connues ; le restant est arrondi à la minute supérieure avec la même écriture des heures. Cas « infos partielles » : quand les données du programme sont partielles, « infos partielles », « progression sans position ou sans durée » et « programme en cours » de « Diaporama héro » s'appliquent sans réserver d'espace aux lignes absentes. — Synchronisation du héro : Titre, en-tête, infos et bouton SHALL correspondre au programme affiché, même sur son média de secours ; « Ouvrir »/« Reprendre » SHALL ouvrir ce programme et changer avec son média, jamais avant. Les textes SHALL rester fixes sans recomposition ni dessin pendant le Ken Burns, sans espace pour les lignes absentes et lisibles sur visuel clair grâce aux voiles. Cas « changement et animation » : quand le média change ou zoome, « changement de programme », « textes avec le visuel », « textes immobiles pendant le Ken Burns » et « lisibilité sur un poster clair » de « Diaporama héro » s'appliquent aussi au média de secours, sans image ou cible d'un autre programme. — Visuel du héro en attente : Un programme dont l'image n'est pas prête SHALL être passé sans attente ni délai propre au héro, sans bloquer navigation ni médias prêts ; à l'échec de la requête, « Repli du programme du héro » SHALL s'appliquer. Une vidéo suivante sans première image après 1 s SHALL rester préparée en pause sans doublon tant qu'une autre n'est pas demandée, et laisser passer le prochain média utilisable ; prête, elle redevient éligible. La première vidéo sans média à remplacer reste exemptée. Cas « préparation lente » : quand le visuel suivant n'est pas prêt, sa récupération commune continue et le programme est passé jusqu'à ce que l'image soit prête ou que la requête échoue ; une vidéo suit son délai de 1 s ; une image préparée n'est pas retenue en bitmap par le héro et est relue depuis le cache à son retour.

### Requirement: Contenu du panneau
Le panneau SHALL n'afficher que des visuels validés, chargés et non expirés de programmes valides de l'app focusée, et ne pas s'ouvrir sinon. Validation, préchargement et affichage SHALL utiliser « Chargement partagé des images » et la politique Top Shelf de « Qualité des visuels ».

#### Scenario: contenu disponible
- **WHEN** l'app focusée a des visuels validés (preview programs / watch next)
- **THEN** chaque affiche apparaît en fondu (300-400ms easing Apple) une fois chargée, puis défile lentement une seule fois (Ken Burns, un seul passage) et reste ensuite immobile sur sa dernière position ; le défilement repart du début à chaque nouvelle affiche ; il n'a lieu que si « Mouvement des visuels » est activé et que les animations du système ne sont pas coupées

#### Scenario: mouvement des visuels désactivé
- **WHEN** « Mouvement des visuels » est désactivé ou que l'échelle d'animation du système est à 0
- **THEN** chaque affiche apparaît en fondu puis reste immobile, cadrée comme au début du défilement ; le changement d'affiche et son fondu sont inchangés, et entre deux affiches le panneau ne redessine aucune image

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

### Requirement: Préchargement et mémoire
Le launcher SHALL borner la validation : premiers visuels du héro au chargement, affiches d'une app au focus posé. Il SHALL décoder à la destination, jamais à la dalle, et garder « Fluidité de release »/« Fenêtre de composition de la grille ». Récupérations, dimensions et échéances SHALL être partagées entre les trois zones selon « Chargement partagé des images » et « Cache borné et fraîcheur des images ».

#### Scenario: validation au chargement
- **WHEN** les programmes sont chargés
- **THEN** seuls les premiers visuels du héro sont vérifiés en arrière-plan, par files d'apps selon le contrat partagé, d'après les dimensions conservées avec les octets vérifiés, sans décoder à la taille d'affichage ; les octets vérifiés restent en cache disque, aucun visuel n'est préchargé en mémoire ; le provider pouvant publier des centaines de programmes, aucun autre lot n'est validé à ce moment

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
- **THEN** la vidéo d'aperçu du programme affiché encore valide est rechargée et reprend, sans compter comme un échec ni faire passer le héro au programme suivant ; les images et programmes sont contrôlés selon leurs échéances avant réaffichage, sans effacer ni recharger les images encore valides, le défilement automatique et les délais de présentation repartent de zéro ; une vidéo lente dont la préparation a été interrompue redevient éligible au diaporama

#### Scenario: écran au repos
- **WHEN** aucune touche n'est pressée et aucune transition n'est en cours
- **THEN** seules les animations spécifiées encore visibles redessinent l'écran (vidéo d'aperçu, passage unique du dégradé animé du repli, passage unique du Ken Burns d'un nouveau visuel du héro ou du panneau Top Shelf, changement de visuel du défilement automatique) ; une fois ces passages terminés, l'image est immobile ; aucune animation d'un élément hors écran ne tourne, et une animation continue ne provoque aucune recomposition à chaque image ; les contrôles d'expiration se déclenchent aux échéances utiles, pas par frame ; avec « Mouvement des visuels » désactivé, un visuel fixe du héro ou du panneau ne redessine rien entre deux changements de visuel ; le verre du dock et de la capsule suit le fondu de son visuel mais n'est pas redessiné par le zoom

#### Scenario: taille de décodage
- **WHEN** un visuel du héro, une affiche du panneau Top Shelf ou une bannière d'app est décodé, même publié en 3840 × 2160
- **THEN** le visuel du héro est décodé à la taille de la fenêtre, au plus 1920 × 1080, l'affiche à la taille du panneau (en le couvrant), et la bannière à la taille de la tuile de la grille au zoom de focus, en pixels de l'appareil, au plus 480 × 270

#### Scenario: une passe par image
- **WHEN** le héro affiche le visuel d'un programme, au repos, pendant le Ken Burns ou un fondu
- **THEN** les voiles du bas et de la gauche sont intégrés au visuel décodé et le voile du coin haut droit est dessiné seul sur sa petite zone ; ni fond de fenêtre, ni fond noir, ni dégradé du repli, ni voile n'est dessiné sous un visuel opaque, et aucun fondu ne passe par un calque plein écran hors écran

#### Scenario: affiches du héro hors cache mémoire
- **WHEN** une affiche du héro est préparée puis affichée
- **THEN** elle est lue depuis le cache disque partagé et n'est pas conservée dans le cache mémoire des images ; seuls les calques affichés par le héro la retiennent, et un retour à un programme déjà vu la relit depuis le disque

#### Scenario: relecture au retour au premier plan
- **WHEN** le launcher revient au premier plan
- **THEN** les programmes du héro et d'À suivre ne sont relus que si le TV Provider a signalé un changement pendant l'arrêt ou si le délai de relecture du héro est échu ; un même changement ne provoque qu'une lecture d'À suivre ; les compteurs de programmes des réglages ne sont lus que réglages ouverts ; les échéances des images et programmes restent contrôlées comme en « retour au premier plan après libération »

#### Scenario: aperçu sans audio
- **WHEN** une vidéo d'aperçu ou une vidéo nature joue dans le héro
- **THEN** aucune piste audio n'est décodée et aucune sortie audio n'est ouverte par le launcher

#### Scenario: vidéo nature à la taille de l'écran
- **WHEN** le héro joue une vidéo nature
- **THEN** la version lue est en 1920 × 1080 au plus ; sous une vidéo dont la première image est rendue et le fondu terminé, le dégradé de repli n'est plus dessiné et la vidéo est composée sans transparence

#### Scenario: affiches du panneau décodées une à une
- **WHEN** les affiches candidates d'une app sont validées au focus posé
- **THEN** elles sont vérifiées par leur en-tête sans être décodées à la taille du panneau ; seule l'affiche affichée, puis la suivante au changement, est décodée à la taille du panneau ; un focus qui traverse une tuile en moins de 0,5 s ne décode aucune affiche

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
