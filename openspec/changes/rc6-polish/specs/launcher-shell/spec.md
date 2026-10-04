# Delta launcher-shell

## MODIFIED Requirements

### Requirement: Thème
L'UI SHALL être toujours sombre, sur fond noir pur, avec des surfaces Liquid Glass façon tvOS 26 : verre sombre transparent (teinte sombre translucide sur l'arrière-plan flouté), fine bordure claire, fin reflet clair sur le bord haut et ombre légère sous la surface. Seules les surfaces verre (dock, capsule heure et réglages, menu contextuel, bandeau du mode déplacement) SHALL montrer un arrière-plan flouté ; aucun autre élément n'est flouté. Le verre SHALL reproduire la maquette validée : arrière-plan flouté de 28 px et saturé à 170 %, teinte sombre translucide (rgb 22, 22, 28 à 36 % pour le dock, 38 % pour la capsule), bordure de 1 px blanche à 16 %, reflet haut blanc à 32 % et reflet bas blanc à 6 %. Le flou SHALL être obtenu de deux façons selon ce qui est derrière :

- dock et capsule sur le visuel d'un programme : copie floutée et saturée du visuel voilé, calculée une seule fois par visuel à partir d'une image réduite, puis déplacée exactement comme le visuel (Ken Burns, fondu) sans être recalculée à chaque image ; si cette copie n'a pas pu être calculée, flou en direct ;
- dock et capsule sur une vidéo ou sur le dégradé du repli, menu contextuel et bandeau du mode déplacement : flou calculé en direct, à partir d'une image réduite de l'arrière-plan et seulement sur la zone couverte par la surface.

- Toujours sombre, noir pur, posters plein cadre, police Figtree (SIL Open Font License 1.1, listée dans « À propos »)
- Relief par une ombre portée douce et un reflet clair, jamais par un halo coloré

Les coins arrondis SHALL suivre les rayons du design system, plus proches de tvOS (valeurs en dp pour l'interface de 960 × 540 dp) : tuiles d'apps de la grille et du dock 14 ; dock 24 ; menu contextuel 26 et ses pilules 12 ; lignes des réglages 14 ; capsule heure et réglages et engrenage en pilule (rayon égal à la demi-hauteur). Toute surface arrondie qui en contient une autre SHALL avoir des coins concentriques : rayon extérieur = rayon intérieur + marge entre les deux bords. Les couples imbriqués sont : dock et tuiles (marge 10, 24 = 14 + 10) ; capsule et engrenage (marge 4, capsule de 36 de haut : 18 = 14 + 4) ; menu et pilules (marge 14, 26 = 12 + 14) ; menu et vignette de l'app en tête du menu (marge 16, vignette 10) ; ligne d'app des réglages et vignette 16:9 (marge 10,5 au-dessus et au-dessous, vignette 3,5) ; code QR d'« À propos » (fond blanc arrondi d'un rayon égal à sa marge blanche, modules carrés jamais rognés). Quand les marges horizontale et verticale d'un couple diffèrent, la marge retenue est la plus petite. Les effets qui épousent une surface (ombre et reflet de la tuile focalisée, pilule de focus, verre précalculé du dock, bordure et reflets du verre) SHALL suivre le rayon de cette surface.

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent

#### Scenario: surfaces verre
- **WHEN** le dock, la capsule heure et réglages (« Capsule heure et réglages » de `settings`), le menu contextuel ou le bandeau du mode déplacement sont affichés
- **THEN** ils utilisent le matériau verre sombre (arrière-plan flouté selon les deux façons ci-dessus, teinte sombre translucide, fine bordure claire, reflet sur le bord haut, ombre légère) ; le panneau du menu contextuel est nettement plus foncé que le dock et la capsule ; si l'appareil ne supporte pas l'effet, la surface reste sombre et translucide, sans flou et sans crash

#### Scenario: bouton sans flou
- **WHEN** le bouton d'ouverture du héro est affiché au repos
- **THEN** il a un fond sombre translucide, un texte blanc et une fine bordure façon verre, sans flou d'arrière-plan ; au focus il a un fond blanc, un texte noir, un léger zoom et une ombre

#### Scenario: coût du flou
- **WHEN** une surface verre est affichée
- **THEN** son flou est soit la copie floutée du visuel calculée une fois par visuel, soit un flou en direct calculé seulement sur la zone qu'elle couvre à partir d'une image réduite de l'arrière-plan ; une surface verre hors écran n'est plus calculée (« Préchargement et mémoire »)

#### Scenario: copie floutée indisponible
- **WHEN** le visuel d'un programme est affiché mais que sa copie floutée n'a pas pu être calculée
- **THEN** le dock et la capsule utilisent le flou en direct ; le visuel net n'apparaît jamais sous leur teinte

#### Scenario: verre sur un visuel en mouvement
- **WHEN** le visuel d'un programme zoome (Ken Burns) ou change par fondu sous le dock et la capsule, ou qu'une tuile du dock prend le focus
- **THEN** le verre montre le visuel flouté qui suit exactement le mouvement et le fondu du visuel, sans qu'aucun flou soit recalculé pendant le mouvement

#### Scenario: verre sur un visuel sombre
- **WHEN** le visuel du héro est sombre sous le dock
- **THEN** le dock est sombre comme sur la maquette : sa couleur est celle du visuel voilé, flouté et saturé, recouvert de la teinte sombre translucide, jamais un gris moyen uniforme

#### Scenario: coins concentriques
- **WHEN** une surface arrondie en contient une autre (tuile dans le dock, engrenage dans la capsule, pilule ou vignette dans le menu contextuel, vignette 16:9 dans une ligne des réglages, code QR dans son fond blanc)
- **THEN** le rayon de la surface extérieure est égal au rayon de la surface intérieure plus la marge réelle entre leurs bords, avec les valeurs ci-dessus

#### Scenario: effets qui suivent le rayon
- **WHEN** une tuile prend le focus, qu'une pilule de focus s'affiche ou que le dock montre son verre précalculé
- **THEN** l'ombre et le reflet de la tuile, la pilule et le verre découpé du dock ont les mêmes coins que la surface qu'ils épousent ; aucun angle ne dépasse ni ne laisse apparaître un autre rayon

### Requirement: Diaporama héro
Le héro SHALL enchaîner automatiquement les programmes publiés, un seul à la fois, plein écran et muet, avec des transitions à la tvOS et un bouton d'ouverture focusable.

Les textes du programme SHALL reprendre la maquette validée (tailles en dp pour l'interface de 960 × 540 dp) :
- **en-tête** au-dessus du titre : l'icône de l'app source (18 dp, coins de 4,5 dp, image carrée de l'icône sans masque du système) puis un libellé qui dépend du type du programme publié dans le TV Provider, X étant le nom de l'app source : programme en cours (`WATCH_NEXT_TYPE_CONTINUE`) « Continuer dans X » ; épisode suivant (`NEXT`) « Épisode suivant dans X » ; nouveauté (`NEW`) « Nouveau dans X » ; liste de lecture (`WATCHLIST`) « À regarder dans X » ; programme mis en avant par l'app (preview program) ou programme watch next sans type ou de type inconnu : « X » seul. Seul ce qui est disponible est affiché : sans nom d'app, le libellé se réduit au type (« Continuer », « Épisode suivant », « Nouveau », « À regarder ») ou disparaît (« X » seul) ; sans icône, le libellé est seul ; sans icône ni libellé, il n'y a pas d'en-tête ;
- **ligne d'infos** sous le titre, par exemple « Saison 2 · Épisode 5 · 42 min » : seulement les éléments publiés par l'app (numéro de saison, numéro d'épisode, durée), séparés par « · » ; la durée est arrondie à la minute (au moins 1 min) et s'écrit « 1 h 35 min » ou « 2 h » au-delà d'une heure ; sans aucun de ces éléments, il n'y a pas de ligne ;
- **progression** : la barre, suivie de « Reste X min » (même écriture des heures), seulement si la position de lecture et la durée sont connues ; le temps restant est arrondi à la minute supérieure.

Aucune ligne absente ne laisse d'espace vide. Ces textes sont fixes pendant l'affichage d'un programme : ils ne sont ni recomposés ni redessinés pendant le Ken Burns, et restent lisibles sur un visuel clair grâce aux voiles (scénario « lisibilité sur un poster clair »).

#### Scenario: défilement automatique
- **WHEN** plusieurs programmes sont disponibles
- **THEN** le héro passe au programme suivant toutes les 12 s environ par fondu croisé (1,4 s, easing Apple : le nouveau visuel apparaît par-dessus l'ancien, sans assombrissement intermédiaire), le visuel courant zoome lentement une seule fois (Ken Burns, un seul passage d'environ 10 s, achevé avant le changement de visuel : au moins 2 s d'image immobile par cycle) puis reste immobile sur sa dernière position, le zoom repartant du début à chaque nouveau visuel, sans son, sans aperçu de l'élément suivant

#### Scenario: navigation manuelle
- **WHEN** l'utilisateur presse gauche/droite sur le héro
- **THEN** le héro passe au programme précédent/suivant avec le même fondu et le minuteur d'avance automatique repart

#### Scenario: vidéo d'aperçu
- **WHEN** le programme expose une vidéo d'aperçu (previewVideoUri)
- **THEN** elle est lue plein écran en muet à la place du poster, repli sur le poster si la lecture échoue

#### Scenario: métadonnées
- **WHEN** un programme ouvrable est affiché
- **THEN** en bas à gauche apparaissent, de haut en bas : l'en-tête (icône et libellé de l'app source), le titre, la ligne d'infos, la barre de progression suivie du temps restant, chacun seulement s'il a quelque chose à montrer, puis un bouton « Ouvrir » (« Reprendre » si progression) qui porte le focus du héro, au style de « Thème » (scénario « bouton sans flou ») ; OK sur ce bouton ouvre le contenu

#### Scenario: retour au programme précédent pendant le fondu
- **WHEN** l'utilisateur revient au programme précédent avant la fin du fondu croisé
- **THEN** le fondu repart de l'opacité courante vers le programme précédent, sans coupure ni saut d'image

#### Scenario: voiles pendant le Ken Burns
- **WHEN** le visuel zoome (Ken Burns)
- **THEN** le voile du coin haut droit derrière la capsule reste fixe et garde son opacité ; les voiles du bas et de la gauche, intégrés au visuel, ne s'affaiblissent pas de façon visible

#### Scenario: changement de programme
- **WHEN** le héro passe d'un programme à un autre
- **THEN** les textes de l'ancien programme s'effacent pendant la première moitié du fondu, puis ceux du nouveau apparaissent pendant la seconde : jamais deux titres superposés ; le bas des textes et le bouton ne bougent pas, quel que soit le nombre de lignes des deux titres

#### Scenario: lisibilité sur un poster clair
- **WHEN** le visuel affiché est clair, jusqu'au blanc (pire cas)
- **THEN** un voile sombre en dégradé depuis le bas et depuis la gauche passe derrière le titre, les métadonnées et le bouton, et un léger voile radial assombrit le coin haut droit derrière la capsule heure et réglages ; le texte blanc et le bouton restent lisibles, le reste du visuel n'est pas voilé

#### Scenario: sans progression
- **WHEN** le programme affiché n'expose pas de progression
- **THEN** aucun espace vide ne sépare les métadonnées du bouton : l'écart entre la dernière ligne de métadonnées et le bouton est le même que l'écart entre la barre de progression et le bouton quand elle est présente, et un titre d'une ligne ne réserve pas de place pour une seconde ligne

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

#### Scenario: textes immobiles pendant le Ken Burns
- **WHEN** le visuel du programme zoome (Ken Burns)
- **THEN** l'en-tête, le titre, la ligne d'infos et la progression ne sont pas recomposés

### Requirement: Préchargement et mémoire
Le launcher SHALL valider les visuels avant de les afficher, en bornant ce travail : les premiers visuels du héro au chargement, les affiches d'une app quand le focus s'y pose. Il SHALL aussi limiter sa consommation mémoire et GPU : chaque image SHALL être décodée à sa taille d'affichage, jamais à la résolution native de la dalle, et le héro SHALL rester fluide au repos et pendant le focus du dock sur la TV de référence (au moins 55 images par seconde et moins de 10 % d'images en retard, mesurés sur l'APK de release). La navigation dans la grille (déplacements du focus, défilement, ouverture du panneau Top Shelf) SHALL avoir moins de 10 % d'images en retard sur la même TV : un déplacement du focus ne recompose que les tuiles et la rangée concernées, et ne lance ni décodage ni vérification d'image tant que le focus ne s'est pas posé.

#### Scenario: validation au chargement
- **WHEN** les programmes sont chargés
- **THEN** seuls les premiers visuels du héro sont vérifiés en arrière-plan (chargement, largeur ≥ 1080 px) ; ils sont gardés en mémoire, les suivants en cache disque ; le provider pouvant publier des centaines de programmes, aucun autre visuel n'est vérifié à ce moment

#### Scenario: validation à la demande
- **WHEN** le focus reste au moins 0,5 s sur une tuile de la grille
- **THEN** quelques affiches de cette app sont vérifiées une seule fois ; seules les affiches validées alimentent son panneau ; un focus qui ne fait que passer sur une tuile ne lance aucune vérification

#### Scenario: économie de ressources
- **WHEN** le héro est masqué (zone grille) ou une surface verre est invisible
- **THEN** le lecteur vidéo est libéré, les animations de fond, le Ken Burns du visuel du héro et le flou d'arrière-plan sont arrêtés ; les images sont décodées avec au plus deux décodeurs simultanés

#### Scenario: écran au repos
- **WHEN** aucune touche n'est pressée et aucune transition n'est en cours
- **THEN** seules les animations spécifiées encore visibles redessinent l'écran (vidéo d'aperçu, passage unique du dégradé animé du repli, passage unique du Ken Burns d'un nouveau visuel du héro ou du panneau Top Shelf, changement de visuel du défilement automatique) ; une fois ces passages terminés, l'image est immobile ; aucune animation d'un élément hors écran ne tourne, et une animation continue ne provoque aucune recomposition à chaque image

#### Scenario: taille de décodage
- **WHEN** un visuel du héro, une affiche du panneau Top Shelf ou une bannière d'app est décodé, même publié en 3840 × 2160
- **THEN** le visuel du héro est décodé à la taille de la fenêtre, au plus 1920 × 1080, l'affiche à la taille du panneau (en le couvrant), et la bannière à la taille des tuiles, au plus 480 × 270

#### Scenario: une passe par image
- **WHEN** le héro affiche le visuel d'un programme, au repos, pendant le Ken Burns ou un fondu
- **THEN** les voiles du bas et de la gauche sont intégrés au visuel décodé et le voile du coin haut droit est dessiné seul sur sa petite zone ; ni fond de fenêtre, ni fond noir, ni dégradé du repli, ni voile n'est dessiné sous un visuel opaque, et aucun fondu ne passe par un calque plein écran hors écran

#### Scenario: navigation dans la grille
- **WHEN** l'utilisateur déplace le focus de tuile en tuile et de rangée en rangée dans la grille
- **THEN** seules les tuiles qui prennent ou perdent le focus et les rangées qui les contiennent sont recomposées, l'animation du focus (zoom, ombre, reflet) ne recompose rien, et la vérification d'un visuel d'une autre app ne recompose aucune rangée

### Requirement: Contenu héro TV Provider
Le héro SHALL afficher les programmes publiés dans le TV Provider système par les apps installées (watch next + preview programs), sans autre configuration que l'octroi de la permission système de lecture des programmes TV. Toute colonne autre que l'identifiant et le package SHALL être traitée comme facultative : une colonne absente ou vide retire seulement l'élément qu'elle alimente. Le remplissage réel du type, de la saison, de l'épisode et de la durée par les apps de la TV de référence est une hypothèse à vérifier sur l'appareil.

#### Scenario: permission
- **WHEN** le launcher démarre sans `android.permission.READ_TV_LISTINGS`
- **THEN** la demande système s'affiche ; dès l'octroi le héro se recharge sans redémarrage ; en cas de refus le héro reste sur le fallback sans erreur visible

#### Scenario: programmes disponibles
- **WHEN** une ou plusieurs apps installées publient des programmes dans le TV Provider
- **THEN** le héro les affiche : visuel (previewVideoUri, sinon posterArtUri, sinon thumbnailUri), titre, app source, type du programme (`watch_next_type` d'un watch next ; preview program sinon), numéros de saison et d'épisode affichés (`season_display_number`, `episode_display_number`), durée et progression quand disponibles (« Diaporama héro »), ordre déterministe (reprises d'abord, puis plus récents), toutes apps confondues, sans liste d'apps codée en dur ; les requêtes au provider ne portent aucune clause de sélection, le filtrage par app est fait côté launcher

#### Scenario: fallback
- **WHEN** aucune app ne publie de programme dans le TV Provider
- **THEN** le héro utilise le fond vidéo de secours, sans crash ni écran vide

#### Scenario: colonnes facultatives
- **WHEN** un programme ne publie pas son type, sa saison, son épisode, sa durée ou sa position
- **THEN** il reste affiché ; seuls les éléments correspondants manquent, selon « Diaporama héro »

## ADDED Requirements

### Requirement: Profil de démarrage livré avec l'application
L'APK de release SHALL embarquer le profil de référence du code de l'app et de ses bibliothèques, et chaque release publiée SHALL fournir à côté de `app-release.apk` un fichier de métadonnées `app-release.dm` (archive qui contient seulement ce profil : `primary.prof` et `primary.profm`), produit par le build à partir de l'APK publié, avec son empreinte SHA-256 publiée par GitHub. Installé avec l'APK, ce fichier SHALL permettre à Android de compiler le code de démarrage dès l'installation, sans attendre la compilation de fond. L'APK SHALL rester installable seul.

#### Scenario: release publiée
- **WHEN** une release est publiée par la CI
- **THEN** elle contient `app-release.apk` et `app-release.dm` produits par le même build, chacun avec son empreinte SHA-256, et le profil de `app-release.dm` est celui embarqué dans `app-release.apk`

#### Scenario: installation manuelle avec le profil
- **WHEN** l'utilisateur installe l'APK et le fichier `.dm` ensemble (`adb install-multiple app-release.apk app-release.dm`, documenté dans le README)
- **THEN** l'app est compilée avec son profil dès la fin de l'installation, sans attendre la compilation de fond du système

#### Scenario: installation sans le profil
- **WHEN** l'APK est installé seul
- **THEN** l'installation réussit ; le profil embarqué est appliqué plus tard par le système, comme avant

#### Scenario: mise à jour intégrée
- **WHEN** la mise à jour intégrée installe une nouvelle version
- **THEN** le profil est installé avec l'APK quand il est disponible et vérifié (« Installation de la mise à jour » de `self-update`)
