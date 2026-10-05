# Capability : launcher-shell

## Purpose
Écran home du launcher : grille d'apps, rangée Up Next, fond contextuel.

## Requirements

### Requirement: Grille d'apps
Le launcher SHALL auto-détecter toutes les apps TV installées et les afficher en grille.

Auto-détection de toutes les apps TV installées (category LEANBACK_LAUNCHER / LAUNCHER).

#### Scenario: affichage
- **WHEN** le home s'ouvre
- **THEN** toutes les apps TV installées apparaissent en grille 5 colonnes, tuiles 16:9 remplies par la bannière Android TV de l'app (`android:banner`), repli sur l'icône entière centrée sur fond sombre ; ordre et épinglage persistés (DataStore)

#### Scenario: menu contextuel
- **WHEN** appui long sur OK sur une tuile
- **THEN** menu en overlay : épingler / retirer du dock, déplacer (grille) ; OK valide l'action focusée, Retour ferme ; un appui court ouvre l'app

#### Scenario: lisibilité du menu contextuel
- **WHEN** le menu contextuel est ouvert au-dessus de tuiles claires ou sombres
- **THEN** la grille derrière est assombrie par un voile sombre et le menu est un panneau en verre sombre (« Thème », scénario « surfaces verre ») sur lequel le texte blanc reste lisible quelles que soient les tuiles derrière ; le nom et le package de l'app sont en tête, les actions sont listées verticalement en dessous ; l'action focusée est une pilule claire à texte sombre, sans zoom ni halo, les autres actions n'ont pas de fond

#### Scenario: navigation dans le menu
- **WHEN** le menu contextuel s'ouvre
- **THEN** la première action (épingler ou retirer du dock) a le focus ; haut et bas parcourent les actions dans l'ordre affiché, sans boucle aux bords ; gauche et droite ne déplacent pas le focus hors du menu ; Retour ferme le menu et rend le focus à la tuile d'origine

### Requirement: Focus tvOS
Le launcher SHALL animer le focus des tuiles d'apps de la grille et du dock, et de toute carte dont la spec renvoie à cette exigence, à la tvOS : la tuile focusée se soulève (zoom, légère montée, ombre portée douce, reflet blanc en diagonale), sans halo ni liseré, de façon à ce que la tuile active soit identifiable d'un coup d'œil. La tuile agrandie SHALL ne recouvrir aucune tuile voisine et SHALL n'être coupée par aucun conteneur.

#### Scenario: focus
- **WHEN** une tuile prend le focus
- **THEN** elle zoome d'environ 1,08x en montant légèrement, une ombre portée douce apparaît sous elle et un reflet blanc en diagonale éclaire son visuel ; aucun halo, coloré ou non, et aucun liseré ; transition 250-400ms courbe Apple ; en zone grille le panneau Top Shelf reflète l'app focusée

#### Scenario: rien ne déborde
- **WHEN** une tuile de la grille ou du dock a le focus
- **THEN** la tuile agrandie ne recouvre aucune tuile voisine, l'espacement entre tuiles et entre rangées absorbant le zoom, et elle reste entière : ni la rangée, ni la grille, ni le dock ne la coupent

#### Scenario: aucun nom sur les tuiles
- **WHEN** une tuile de la grille ou du dock est affichée, avec ou sans le focus
- **THEN** aucun nom d'app n'est écrit sur la tuile ni sous elle ; une app sans bannière garde le repli de « Grille d'apps » (icône entière centrée sur fond sombre)

### Requirement: Rangée Up Next
Le home SHALL afficher une rangée Up Next fusionnant Jellyfin puis BetaSeries.

#### Scenario: contenu
- **WHEN** des items Jellyfin en cours existent THEN rangée au-dessus de la grille : posters + barre de progression, fusion déterministe Jellyfin puis BetaSeries (v1.x), tri par date d'activité

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

### Requirement: Navigation
Le launcher SHALL être navigable au DPAD uniquement.

- DPAD natif uniquement ; Home de la télécommande retourne au launcher

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent

### Requirement: Écran initial du home
Le home SHALL s'ouvrir sur un héro plein écran sans cadre : visuel d'un programme publié par les apps installées ou, à défaut, vidéo nature en boucle, avec le dock en overlay bas. Au démarrage à froid, le héro SHALL apparaître à la fin de l'écran de démarrage, par le fondu qui le termine (« Écran de démarrage »).

#### Scenario: état initial
- **WHEN** le launcher démarre
- **THEN** le héro occupe tout l'écran (aucune carte, aucun aperçu du suivant), il détient le focus dès son affichage, et le dock est visible en overlay bas semi-transparent ; au démarrage à froid, cet affichage suit l'écran de démarrage et le focus est donné au héro dès le début du fondu

#### Scenario: fallback sans contenu
- **WHEN** aucune app ne publie de programme, ou la permission est refusée
- **THEN** le héro joue les vidéos nature en boucle ; tant qu'aucune vidéo ne joue, un dégradé sombre animé est affiché (un seul passage lent, puis figé sur sa dernière position, comme le Ken Burns), jamais d'écran noir ni de crash

### Requirement: Navigation 3 paliers
Le DPAD SHALL naviguer héro → dock → grille (descend) et grille → dock → héro (monte) de façon déterministe : seule la zone active est focusable. L'accueil SHALL être une page d'un seul tenant qui défile verticalement : le héro (avec le dock en overlay) occupe le premier écran, la grille suit en dessous ; passer du dock à la grille et de la grille au dock est un défilement continu de la page, sans fondu ni saut, avec la courbe et la durée du design system (« Focus tvOS »). Au retour dans la zone grille, la page SHALL redescendre jusqu'à la position de la grille laissée à la sortie, et non d'un écran exactement (décision de Sygix) ; à la toute première entrée, elle descend d'un écran, au début de la grille.

#### Scenario: descente depuis le héro
- **WHEN** l'utilisateur presse bas depuis le héro
- **THEN** le premier élément du dock prend le focus, ou la grille si le dock est vide (dans ce cas la page défile comme au scénario « traversée du dock »)

#### Scenario: traversée du dock
- **WHEN** l'utilisateur descend depuis le dock
- **THEN** la page défile en une seule animation continue : le héro, le dock et la capsule heure et réglages (« Capsule heure et réglages » de `settings`) sortent par le haut pendant que la grille remonte jusqu'à occuper tout l'écran, d'environ un écran à la première entrée, jusqu'à la position de la grille laissée à la sortie sinon (« Panneau Top Shelf au focus », scénario « sortie et retour dans la grille ») ; la grille prend le focus (dernière tuile visitée, sinon la première) ; la lecture du héro est mise en pause

#### Scenario: retour dans la grille à la position laissée
- **WHEN** l'utilisateur a parcouru la grille jusqu'à une position qui n'est pas son début, la quitte (Retour, ou remontée vers le dock ou le héro), puis y redescend
- **THEN** la page redescend en une seule animation continue jusqu'à la position de la grille laissée à la sortie, et non d'un écran exactement : la dernière tuile visitée reprend le focus à la même place à l'écran qu'au moment de la sortie

#### Scenario: dock jamais visible en vue grille
- **WHEN** la zone grille est active, pendant ou après le défilement
- **THEN** le dock n'est visible à aucun moment au-dessus ou en travers de la grille : il quitte l'écran avec le héro et ne réapparaît qu'avec lui ; aucune position intermédiaire ne montre le dock sur la grille

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la première rangée de la grille
- **THEN** la page défile en sens inverse avec la même animation continue : la grille redescend pendant que le héro, le dock et la capsule reviennent par le haut ; le dock reprend le focus (ou le héro si le dock est vide) ; haut depuis le dock rend le focus au héro et relance sa lecture

#### Scenario: retour depuis la grille
- **WHEN** l'utilisateur presse Retour depuis la grille, quelle que soit la rangée focusée
- **THEN** la page défile jusqu'au héro avec la même animation continue (une seule animation, même si la grille avait été parcourue en profondeur), le héro reprend le focus et sa lecture

#### Scenario: continuité de l'animation
- **WHEN** un défilement héro ↔ grille est en cours et l'utilisateur presse à nouveau haut, bas ou Retour
- **THEN** la page repart de sa position courante vers la nouvelle cible sans saut ni retour à une position de départ

#### Scenario: couches inactives
- **WHEN** une zone n'est pas active
- **THEN** aucun de ses éléments ne peut prendre le focus ; gauche et droite restent dans la zone active

### Requirement: Dock d'apps épinglées
Le home SHALL afficher un dock en verre sombre (« Thème », rail overlay bas) contenant au plus 6 apps épinglées (comme tvOS), qui restent également présentes dans la grille. Le dock overlay le héro et SHALL se déplacer avec lui : il n'est visible que lorsque le héro l'est. Son matériau verre SHALL rester actif tant qu'une partie du héro est à l'écran, y compris pendant le défilement, et n'être coupé qu'une fois le héro entièrement sorti de l'écran (décision de Sygix) : le dock ne change pas d'aspect au début de la descente, et la bascule du verre ne recrée pas son contenu.

#### Scenario: état initial
- **WHEN** le home s'ouvre
- **THEN** le dock est visible en overlay bas sur le héro, le focus est sur le héro

#### Scenario: taille tvOS
- **WHEN** le dock contient une ou plusieurs apps
- **THEN** toutes ses tuiles ont la même taille fixe, plus petite que celle des tuiles de la grille (environ 80 %), quel que soit le nombre d'apps, et le dock, centré, s'ajuste à leur nombre

#### Scenario: tuiles adaptatives
- **WHEN** le dock contient 6 apps, son maximum
- **THEN** ses tuiles gardent la taille fixe du scénario « taille tvOS », sans réduction, et le dock reste centré et entièrement dans la largeur de l'écran

#### Scenario: épinglage
- **WHEN** l'utilisateur épingle ou retire une app via le menu contextuel (appui long, OK valide directement)
- **THEN** le dock est mis à jour et l'app reste dans la grille

#### Scenario: solidaire du héro
- **WHEN** la page défile vers la grille ou revient vers le héro (« Navigation 3 paliers »)
- **THEN** le dock suit exactement le mouvement du héro, sans fondu propre ni décalage ; en vue grille il est entièrement hors écran et son matériau verre n'est plus calculé (« Préchargement et mémoire »)

#### Scenario: verre du dock pendant le défilement
- **WHEN** la page défile du dock vers la grille, puis de la grille vers le dock
- **THEN** à la descente, le verre du dock reste actif et identique tant que le héro est au moins en partie à l'écran, et n'est coupé qu'une fois le héro entièrement sorti ; à la remontée, il redevient actif dès que le héro réapparaît ; à aucune de ces bascules les tuiles du dock ne sont recréées (le focus et l'état des tuiles sont conservés)

#### Scenario: dock plein
- **WHEN** le dock contient déjà 6 apps et l'utilisateur ouvre le menu contextuel d'une app qui n'y est pas
- **THEN** l'action « Épingler au dock » est indisponible : grisée, accompagnée d'un message court indiquant que le dock est plein, et OK n'y fait rien ; elle redevient disponible dès qu'une app est retirée du dock

#### Scenario: épinglages au-delà du maximum
- **WHEN** le launcher démarre avec plus de 6 apps épinglées enregistrées (version antérieure)
- **THEN** le dock n'en affiche que 6 et aucun épinglage enregistré n'est effacé

### Requirement: Persistance de la grille
L'ordre des apps et les épinglages SHALL être persistés localement (DataStore) et restaurés au démarrage ; les apps nouvellement installées s'ajoutent à la fin par ordre alphabétique.

#### Scenario: réorganisation
- **WHEN** l'utilisateur choisit « Déplacer » sur une tuile de la grille puis presse les flèches (gauche/droite : une case, haut/bas : une rangée, par insertion), OK pour valider
- **THEN** la tuile suit le focus avec un liseré de déplacement, un rappel des touches s'affiche, l'ordre est persisté et conservé après redémarrage

#### Scenario: annulation
- **WHEN** l'utilisateur presse Retour pendant un déplacement
- **THEN** l'ordre d'avant le déplacement est restauré

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

### Requirement: Clic programme publié
Le clic sur un poster du TV Provider SHALL ouvrir le contenu via l'intent publié par l'app source.

#### Scenario: ouverture
- **WHEN** l'utilisateur clique sur un poster héro issu du TV Provider
- **THEN** l'app source s'ouvre sur la fiche du contenu (intent du programme), fallback : lancement simple de l'app si l'intent échoue

#### Scenario: progression
- **WHEN** le programme expose une position de lecture (watch next)
- **THEN** une barre de progression est visible sur le poster héro

### Requirement: Panneau Top Shelf au focus
Le panneau d'aperçu SHALL s'ouvrir à la demande au-dessus de la rangée focusée, et se refermer quand l'app focusée n'a rien à montrer ; aucune place n'est réservée quand il est fermé. Le défilement de la grille SHALL suivre une règle de placement unique, appliquée au bloc focusé (la rangée focusée, plus le panneau quand il est ouvert au-dessus d'elle) : le bloc reste entre la marge haute de la grille (40 dp sous le haut de l'écran) et la marge basse (32 dp au-dessus du bas de l'écran, l'espacement entre rangées). La grille ne défile que pour y ramener le bloc, et ce défilement est animé avec la durée et la courbe de l'expansion du panneau. Tant que la zone grille est active, la page ne remonte jamais au-dessus du début de la grille : le héro reste hors écran.

#### Scenario: arrivée dans la grille
- **WHEN** l'utilisateur descend du dock vers la grille
- **THEN** la grille occupe tout l'écran, sans emplacement réservé ni panneau

#### Scenario: insertion
- **WHEN** le focus reste environ 3 s sur une tuile dont l'app a des visuels validés
- **THEN** le panneau s'insère au-dessus de la rangée focusée (la rangée précédente reste au-dessus du panneau), avec une animation d'expansion ; la rangée focusée reste immobile à l'écran et les rangées précédentes remontent pour laisser la place au panneau

#### Scenario: insertion près du haut
- **WHEN** le panneau s'ouvre au-dessus d'une rangée trop proche du haut pour qu'il tienne sous la marge haute sans déplacer cette rangée
- **THEN** le haut du panneau s'aligne sur la marge haute et la rangée focusée descend juste ce qu'il faut pour lui laisser la place

#### Scenario: déplacement du focus
- **WHEN** un panneau est déjà ouvert et le focus passe à une autre tuile dont l'app a des visuels validés
- **THEN** le panneau montre immédiatement les visuels de la nouvelle app, sans délai ni fermeture intermédiaire, et se replace au-dessus de la nouvelle rangée focusée

#### Scenario: fermeture
- **WHEN** le focus passe sur une tuile dont l'app n'a aucun visuel validé
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

#### Scenario: bloc trop grand
- **WHEN** le bloc focusé ne tient pas entre les deux marges
- **THEN** le haut du bloc (le haut du panneau s'il est ouvert) s'aligne sur la marge haute

#### Scenario: sortie et retour dans la grille
- **WHEN** l'utilisateur quitte la zone grille (Retour, ou remontée vers le dock ou le héro) puis y revient
- **THEN** la page défile jusqu'au héro (« Navigation 3 paliers »), mais la position de la grille n'est pas remise à zéro : au retour, la page redescend jusqu'à la position de la grille laissée à la sortie, la rangée focusée à la même place à l'écran (le panneau éventuellement ouvert s'est refermé à la sortie), le focus va à la dernière tuile visitée (« Navigation 3 paliers »), et la grille ne défile davantage que si cette tuile est hors des marges

### Requirement: Contenu du panneau
Le panneau SHALL n'afficher que des visuels validés et chargés de l'app focus, et ne pas s'ouvrir sinon.

#### Scenario: contenu disponible
- **WHEN** l'app focusée a des visuels validés (preview programs / watch next)
- **THEN** chaque affiche apparaît en fondu (300-400ms easing Apple) une fois chargée, puis défile lentement une seule fois (Ken Burns, un seul passage) et reste ensuite immobile sur sa dernière position ; le défilement repart du début à chaque nouvelle affiche

#### Scenario: pas de contenu
- **WHEN** l'app focusée n'a aucun visuel validé
- **THEN** aucun panneau n'est ouvert (ni cadre, ni logo de repli) et la grille occupe tout l'écran

### Requirement: Fond de la zone grille
Dans la grille, le fond SHALL être un dégradé neutre uni type tvOS, le héro étant complètement masqué parce qu'il est sorti de l'écran par le haut.

#### Scenario: révélation de la grille
- **WHEN** l'utilisateur passe en zone grille
- **THEN** à la fin du défilement le héro est entièrement hors écran (aucun pixel du héro, du dock ni de la capsule heure et réglages n'est visible), le fond est le dégradé neutre (pas d'aerial, pas de posters en fond de grille), sans fondu de couches

#### Scenario: pendant le défilement
- **WHEN** la page est entre le héro et la grille
- **THEN** le héro et la grille sont chacun visibles pour la part de l'écran qu'ils occupent, sans zone noire ni image figée entre les deux ; le dégradé neutre est le fond de la partie grille dès le début du mouvement

#### Scenario: héro hors écran
- **WHEN** le héro est entièrement hors écran
- **THEN** sa lecture est en pause et ses ressources sont libérées conformément à « Préchargement et mémoire » et « Fond vidéo de secours »

### Requirement: Diaporama héro
Le héro SHALL enchaîner automatiquement les programmes publiés, un seul à la fois, plein écran et muet, avec des transitions à la tvOS et un bouton d'ouverture focusable.

Les textes du programme SHALL reprendre la maquette validée (tailles en dp pour l'interface de 960 × 540 dp) :
- **en-tête** au-dessus du titre : l'icône de l'app source (18 dp, coins de 4,5 dp, image carrée de l'icône sans masque du système) puis un libellé qui dépend du type du programme publié dans le TV Provider, X étant le nom de l'app source : programme en cours (`WATCH_NEXT_TYPE_CONTINUE`) « Continuer dans X » ; épisode suivant (`NEXT`) « Épisode suivant dans X » ; nouveauté (`NEW`) « Nouveau dans X » ; liste de lecture (`WATCHLIST`) « À regarder dans X » ; programme mis en avant par l'app (preview program) ou programme watch next sans type ou de type inconnu : « X » seul. Seul ce qui est disponible est affiché : sans nom d'app, le libellé se réduit au type (« Continuer », « Épisode suivant », « Nouveau », « À regarder ») ou disparaît (« X » seul) ; sans icône, le libellé est seul ; sans icône ni libellé, il n'y a pas d'en-tête ;
- **ligne d'infos** sous le titre, par exemple « Saison 2 · Épisode 5 · 42 min » : seulement les éléments publiés par l'app (numéro de saison, numéro d'épisode, durée), séparés par « · » ; la durée est arrondie à la minute (au moins 1 min) et s'écrit « 1 h 35 min » ou « 2 h » au-delà d'une heure ; sans aucun de ces éléments, il n'y a pas de ligne ;
- **progression** : la barre, suivie de « Reste X min » (même écriture des heures), seulement si la position de lecture et la durée sont connues ; le temps restant est arrondi à la minute supérieure.

Le titre, l'en-tête, la ligne d'infos et le bouton SHALL toujours être ceux du programme dont le visuel est affiché, et « Ouvrir » ou « Reprendre » SHALL ouvrir ce programme : ils changent avec le visuel, jamais avant. Si le visuel du programme suivant (image chargée, ou première image de sa vidéo d'aperçu) n'est pas prêt 1 s après le changement demandé (défilement automatique ou gauche/droite), le héro SHALL passer au programme suivant dont le visuel est utilisable, sans changer les textes tant qu'aucun nouveau visuel n'est affiché. Le visuel lent SHALL continuer de se préparer en arrière-plan sans nouvelle requête (une vidéo lente reste préparée, en pause, tant qu'une autre vidéo n'est pas demandée) et son programme redevient éligible dès qu'il est prêt ; une image chargée en arrière-plan n'est pas gardée en mémoire par le héro et est relue depuis le cache d'images quand son programme revient. La règle ne s'applique pas à la toute première vidéo du héro, qui n'a aucun visuel à remplacer. Une image ou une vidéo en échec n'est pas redemandée pendant la session.

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
- **THEN** le fondu des visuels et celui des textes commencent ensemble, quand le visuel du nouveau programme est prêt (image chargée, ou première image de sa vidéo rendue) ; jusque-là, le titre, les infos et le bouton restent ceux du programme dont le visuel est affiché ; les textes de l'ancien programme s'effacent pendant la première moitié du fondu, puis ceux du nouveau apparaissent pendant la seconde : jamais deux titres superposés ; le bas des textes et le bouton ne bougent pas, quel que soit le nombre de lignes des deux titres

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
- **WHEN** le visuel du programme suivant met 3 s à charger
- **THEN** pendant ces 3 s, le titre, les infos et le bouton restent ceux du programme affiché et OK ouvre ce programme ; ils ne changent qu'avec le fondu vers le nouveau visuel

#### Scenario: visuel lent
- **WHEN** le visuel du programme suivant n'est pas prêt 1 s après le changement demandé (par exemple une image servie lentement par le fournisseur de contenu d'une app, ou une vidéo d'aperçu dont la première image tarde)
- **THEN** le héro passe au programme suivant dont le visuel est utilisable, sans afficher les textes du programme lent ; le visuel lent continue de se préparer sans nouvelle requête, et le programme revient dans la rotation une fois son visuel prêt

#### Scenario: textes immobiles pendant le Ken Burns
- **WHEN** le visuel du programme zoome (Ken Burns)
- **THEN** l'en-tête, le titre, la ligne d'infos et la progression ne sont pas recomposés

### Requirement: Fond vidéo de secours
Le fallback du héro SHALL être une liste de clips nature libres de droits (Pexels, 2K sinon 1080p, mp4 H.264 en https) lus en boucle par un unique lecteur, muet.

#### Scenario: lecture
- **WHEN** le fallback est actif
- **THEN** un seul lecteur lit la playlist en boucle et muet, un clip en erreur est ignoré au profit du suivant, le lecteur est mis en pause quand la grille est active et libéré quand le héro est démonté

#### Scenario: aucun clip lisible
- **WHEN** aucun clip ne peut être lu (réseau absent, URL morte)
- **THEN** le dégradé sombre animé reste affiché, sans écran noir ni crash

### Requirement: Qualité des visuels
Le héro et le panneau Top Shelf SHALL n'afficher que des visuels de bonne qualité : vidéo d'aperçu privilégiée quand l'app en fournit une, image ou vidéo d'au moins 1080 px de large, sinon le programme est écarté.

#### Scenario: image trop petite
- **WHEN** l'image décodée d'un programme fait moins de 1080 px de large, ou ne peut pas être chargée
- **THEN** le héro passe au programme suivant sans afficher l'image, et le panneau Top Shelf ne l'inclut pas

#### Scenario: vidéo trop petite
- **WHEN** la vidéo d'aperçu d'un programme fait moins de 1080 px de large
- **THEN** le héro replie sur le poster du programme, puis l'écarte si le poster est aussi insuffisant

### Requirement: Sélection des apps sources
Le launcher SHALL permettre, dans ses réglages (v1.x), de cocher les apps dont les programmes alimentent le héro et le Top Shelf ; par défaut toutes les apps installées sont retenues.

#### Scenario: app décochée
- **WHEN** l'utilisateur décoche une app dans les réglages du launcher (v1.x)
- **THEN** ses programmes n'apparaissent plus dans le héro ni dans le Top Shelf, sans redémarrage

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

#### Scenario: écran de démarrage
- **WHEN** l'écran de démarrage est affiché
- **THEN** seuls le catalogue et le premier visuel utilisable du héro sont préparés en priorité ; la vérification des visuels suivants du héro attend la fin de l'écran de démarrage, les bannières sont décodées sur un fil de basse priorité, et la grille, hors écran, n'est composée qu'après le fondu vers l'accueil

#### Scenario: navigation dans la grille
- **WHEN** l'utilisateur déplace le focus de tuile en tuile et de rangée en rangée dans la grille
- **THEN** seules les tuiles qui prennent ou perdent le focus et les rangées qui les contiennent sont recomposées, l'animation du focus (zoom, ombre, reflet) ne recompose rien, et la vérification d'un visuel d'une autre app ne recompose aucune rangée

### Requirement: Focus d'une app disparue
Quand l'app focusée disparaît du dock ou de la grille (app cachée, retirée du dock, désinstallée), le focus SHALL passer à la tuile voisine à la même position dans la zone. Si la zone devient vide, le focus SHALL revenir au héro. Les autres règles de focus restent celles de « Navigation 3 paliers ».

#### Scenario: tuile au milieu
- **WHEN** l'app focusée disparaît alors qu'une tuile la suivait dans la zone
- **THEN** cette tuile, qui prend sa position, reçoit le focus ; le focus ne retombe ni sur la première tuile ni hors de la zone

#### Scenario: dernière tuile
- **WHEN** l'app focusée était la dernière tuile de la zone et qu'il en reste d'autres
- **THEN** la tuile précédente prend le focus

#### Scenario: zone vidée
- **WHEN** l'app focusée était la seule de sa zone (dernière app du dock retirée, dernière app de la grille cachée)
- **THEN** le héro reprend le focus et Retour répond à nouveau, de la même façon pour la grille et pour le dock

#### Scenario: zone inactive
- **WHEN** la dernière tuile visitée d'une zone disparaît pendant que l'utilisateur est dans une autre zone
- **THEN** le focus ne change pas de zone ; au retour dans la zone, la tuile voisine à la même position prend le focus

### Requirement: Écran de démarrage
Au démarrage à froid du launcher, c'est-à-dire à la première création de l'accueil dans son processus (y compris quand Android avait déjà démarré le processus en arrière-plan, et après une mise à jour), le launcher SHALL afficher un écran de démarrage plein écran à la place de l'accueil : la mascotte seule (le fantôme, sans TV ni nom, aucun texte), centrée, sur le fond noir de l'application (« Thème »), animée (« Animation de la mascotte »). La mascotte SHALL apparaître par un fondu court depuis le fond, jamais par un saut sec.

L'accueil SHALL être prêt quand le catalogue d'apps est chargé et que le premier visuel du héro est prêt à l'affichage (image d'un programme chargée, ou première image d'une vidéo, de programme ou nature, rendue ; le dégradé animé du repli ne compte pas comme visuel) ; l'attente de ce visuel SHALL être plafonnée à 2 s, comptées depuis la première image de l'écran de démarrage. L'écran de démarrage SHALL rester affiché au moins 600 ms depuis la première image qui montre la mascotte (depuis sa propre première image si l'animation est illisible), et tant que l'accueil n'est pas prêt, sans dépasser 5 s au total depuis sa première image. L'animation de la mascotte SHALL être décodée dès le démarrage du processus, hors du fil principal. Dès que la durée minimale est atteinte et que l'accueil est prêt, ou au plus tard à 5 s, il SHALL laisser place à l'accueil par un fondu enchaîné, avec la courbe et une durée du design system (« Focus tvOS »), sans image noire, sans saut et sans flash ; à 5 s, l'accueil est révélé dans l'état où il se trouve et ses propres états de chargement ou d'erreur prennent le relais.

L'accueil SHALL être composé seulement après le fondu d'entrée de la mascotte, puis dès que la fenêtre est active ou au plus tard 400 ms après ce fondu si elle ne l'est pas (demande de permission, surcouche système), ou au début du fondu de sortie, pour que sa composition ne retarde ni l'apparition de la mascotte ni la réception des touches ; jusqu'au début du fondu, il SHALL être composé et dessiné sous l'écran de démarrage opaque, donc invisible, sans aucune animation (le visuel du héro y apparaît sans fondu, une vidéo d'aperçu y est préparée sans être lue, le Ken Burns et la lecture ne commencent qu'au fondu de sortie, la grille hors écran n'est composée qu'à la fin du fondu), pour que le fondu de sortie n'ait plus à le dessiner pour la première fois, et aucun de ses éléments ne SHALL pouvoir prendre le focus : les touches du D-pad et OK n'ont aucun effet sur lui. Le fondu de sortie SHALL être fluide, sans calque plein écran hors écran. Le fondu de sortie appartient à l'écran de démarrage (décision de Sygix) : la touche Retour pressée à tout moment de l'écran de démarrage, fondu de sortie compris, SHALL suivre le comportement normal du système (SygixOs quitte le premier plan), sans être interceptée par l'accueil ni déclencher d'action sur lui. L'écran de démarrage SHALL ne jamais être affiché au retour sur un accueil déjà créé dans le processus (touche Home, fin d'une autre app, retour au premier plan). Hors démarrage à froid, si l'accueil est recréé alors que son catalogue n'est pas encore chargé, le launcher SHALL afficher un fond noir uni, sans mascotte ni animation, jusqu'à ce que l'accueil s'affiche.

#### Scenario: démarrage à froid
- **WHEN** l'accueil est créé pour la première fois dans le processus du launcher
- **THEN** l'écran de démarrage s'affiche : la mascotte seule, centrée, animée, sur fond noir ; aucun texte, aucun dégradé animé, aucune autre zone de l'accueil n'est visible

#### Scenario: processus démarré en arrière-plan
- **WHEN** Android a démarré le processus du launcher en arrière-plan (diffusion système, mise à jour) sans créer l'accueil, puis l'utilisateur ouvre le launcher
- **THEN** c'est un démarrage à froid : l'écran de démarrage s'affiche selon les mêmes règles

#### Scenario: accueil prêt avant la durée minimale
- **WHEN** le catalogue est chargé et le premier visuel du héro est prêt 100 ms après la première image qui montre la mascotte
- **THEN** l'écran de démarrage reste affiché jusqu'à 600 ms après cette image, puis le fondu vers l'accueil commence

#### Scenario: attente du premier visuel
- **WHEN** le catalogue est chargé à 200 ms et le premier visuel du héro n'est prêt qu'à 1,5 s
- **THEN** l'écran de démarrage reste affiché et animé jusqu'à 1,5 s, puis le fondu commence aussitôt

#### Scenario: visuel trop lent
- **WHEN** le catalogue est chargé à 200 ms et le premier visuel du héro n'est toujours pas prêt 2 s après la première image de l'écran de démarrage
- **THEN** le fondu commence à 2 s ; le héro poursuit son propre chargement et affiche son état de repli tant qu'aucun visuel n'est prêt (« Écran initial du home », scénario « fallback sans contenu »)

#### Scenario: plafond global
- **WHEN** le catalogue n'est toujours pas chargé 5 s après la première image de l'écran de démarrage
- **THEN** le fondu commence à 5 s et révèle l'accueil dans son état courant : fond noir uni tant que le catalogue n'est pas chargé, puis l'accueil dès qu'il l'est, avec ses propres états de chargement ou d'erreur

#### Scenario: fondu vers l'accueil
- **WHEN** le fondu vers l'accueil commence
- **THEN** l'accueil, jusque-là composé et caché sous l'écran de démarrage, apparaît pendant que l'écran de démarrage s'efface ; le héro détient le focus dès le début du fondu (« Écran initial du home ») ; à la fin du fondu l'écran de démarrage n'est plus affiché et son animation est arrêtée

#### Scenario: touches pendant l'écran de démarrage
- **WHEN** l'utilisateur presse une touche du D-pad ou OK avant le début du fondu
- **THEN** aucun élément de l'accueil ne prend le focus ni ne réagit ; au début du fondu, le héro prend le focus comme si aucune touche n'avait été pressée

#### Scenario: Retour pendant l'écran de démarrage
- **WHEN** l'utilisateur presse Retour pendant l'écran de démarrage, y compris dans la première seconde après le lancement
- **THEN** le comportement normal du système s'applique : SygixOs quitte le premier plan, sans crash ; aucune action n'est déclenchée sur l'accueil

#### Scenario: retour sur le launcher
- **WHEN** le launcher revient au premier plan (touche Home, fin d'une autre app) alors que l'accueil a déjà été créé dans le processus
- **THEN** aucun écran de démarrage n'est affiché ; l'accueil s'affiche directement, selon les règles de retour existantes (« Navigation 3 paliers »)

#### Scenario: accueil recréé hors démarrage à froid
- **WHEN** l'accueil est recréé dans un processus où il avait déjà été créé, avant que son catalogue soit chargé
- **THEN** un fond noir uni s'affiche, sans mascotte ni animation, jusqu'à ce que l'accueil s'affiche

#### Scenario: processus arrêté par le système
- **WHEN** le système a arrêté le processus du launcher (manque de mémoire) et l'utilisateur revient sur le launcher
- **THEN** c'est un démarrage à froid : l'écran de démarrage s'affiche selon les mêmes règles

#### Scenario: demande de permission au premier lancement
- **WHEN** la demande système de la permission `READ_TV_LISTINGS` s'affiche pendant l'écran de démarrage
- **THEN** la demande apparaît par-dessus ; l'écran de démarrage et le fondu se poursuivent sans attendre la réponse, qui est traitée comme avant (« Contenu héro TV Provider », scénario « permission »)

#### Scenario: animation illisible
- **WHEN** l'animation de la mascotte ne peut pas être décodée (fichier absent ou corrompu)
- **THEN** l'écran de démarrage affiche le fond seul, sans mascotte, avec les mêmes règles de durée et de fondu, la durée minimale comptant depuis sa première image ; aucune erreur visible, aucun crash

#### Scenario: mascotte décodée tard
- **WHEN** la mascotte n'apparaît que 800 ms après la première image de l'écran de démarrage, l'accueil étant déjà prêt
- **THEN** la mascotte apparaît par son fondu d'entrée et l'écran de démarrage reste affiché 600 ms à partir de son apparition, dans la limite du plafond global de 5 s

#### Scenario: composition de l'accueil
- **WHEN** le catalogue est chargé avant la fin du fondu d'entrée de la mascotte ou avant que la fenêtre soit active
- **THEN** l'accueil n'est composé qu'après ce fondu et l'activation de la fenêtre ; l'apparition de la mascotte et son animation ne sont pas interrompues

#### Scenario: fenêtre sans focus
- **WHEN** la demande de permission du premier lancement ou une surcouche système garde le focus de la fenêtre pendant l'écran de démarrage
- **THEN** l'accueil est composé 400 ms après le fondu d'entrée de la mascotte et l'écran de démarrage se termine selon les mêmes règles de durée

#### Scenario: fluidité de l'écran de démarrage
- **WHEN** l'écran de démarrage est affiché puis s'efface vers l'accueil sur la TV de référence (APK de release)
- **THEN** la mascotte et le fondu de sortie visent 60 images par seconde et moins de 10 % d'images en retard ; aucun travail de l'accueil autre que sa première composition et son premier dessin, cachés sous l'écran de démarrage, n'a lieu pendant la mascotte

#### Scenario: Retour pendant le fondu de sortie
- **WHEN** l'utilisateur presse Retour pendant le fondu de sortie, entre son début et sa fin
- **THEN** le comportement normal du système s'applique comme pendant le reste de l'écran de démarrage : SygixOs quitte le premier plan, sans crash ; la touche n'est pas interceptée par l'accueil et aucune action n'est déclenchée sur lui

### Requirement: Animation de la mascotte
La mascotte de l'écran de démarrage SHALL être une animation pré-rendue (flottement et clignement des yeux) de 2 s (120 images), jouée à 60 images par seconde en boucle infinie, décodée par le système sans lecteur vidéo. Le raccord entre la dernière image et la première SHALL être invisible. La mascotte SHALL être affichée à 180 dp de côté, centrée à l'écran, soit à sa résolution native (360 px) sur un écran 1080p, jamais agrandie au-delà. Autour de la mascotte, les pixels SHALL être transparents, de sorte qu'aucun cadre ni écart de teinte avec le fond ne soit visible. La première image de l'animation SHALL être une pose de repos (yeux ouverts, position centrale du flottement), utilisée telle quelle comme image fixe (« Écran de démarrage sans animation »).

#### Scenario: lecture en boucle
- **WHEN** l'écran de démarrage reste affiché plus de 2 s
- **THEN** l'animation reprend au début sans arrêt, sans image figée ni saut de position ou d'expression au raccord

#### Scenario: bords de la mascotte
- **WHEN** l'écran de démarrage est affiché
- **THEN** aucun rectangle ni halo de couleur différente du fond n'est visible autour de la mascotte

#### Scenario: taille à l'écran
- **WHEN** l'écran de démarrage est affiché sur une TV 1080p
- **THEN** la mascotte mesure 180 dp de côté, centrée horizontalement et verticalement, sans agrandissement de l'animation au-delà de sa résolution native

#### Scenario: fin de l'écran de démarrage
- **WHEN** le fondu vers l'accueil se termine
- **THEN** l'animation est arrêtée et ses ressources sont libérées ; aucune image de la mascotte n'est plus décodée tant que le processus vit

### Requirement: Fichier de l'animation de la mascotte
L'animation de la mascotte SHALL être livrée dans l'application sous la forme d'un unique fichier WebP animé qui respecte ce contrat : canevas carré de 360 × 360 px ; canal alpha présent ; nombre de répétitions 0 (boucle infinie) ; 120 images ; chaque image dure 16 ou 17 ms ; durée totale de la boucle de 2 s à 3 ms près ; poids du fichier au plus 1 Mio (1 048 576 octets). Un fichier qui ne respecte pas ce contrat SHALL faire échouer la suite de tests.

#### Scenario: fichier conforme
- **WHEN** la suite de tests s'exécute avec le fichier livré
- **THEN** le contrôle du fichier vérifie le format WebP animé, le canevas, le canal alpha, le nombre de répétitions, le nombre et la durée des images, la durée de la boucle et le poids, et réussit

#### Scenario: fichier hors contrat
- **WHEN** le fichier livré pèse plus de 1 Mio, n'a pas de canal alpha, a un canevas autre que 360 × 360 px, une boucle finie, un autre nombre d'images ou des images de durée autre que 16 ou 17 ms
- **THEN** la suite de tests échoue en nommant la contrainte non respectée

### Requirement: Enchaînement avec l'écran de lancement du système
L'écran de lancement que le système affiche au lancement d'une app (Android 12 et suivants) SHALL avoir exactement le fond noir de l'écran de démarrage et ne montrer aucune icône ni image. Entre l'appui qui lance le launcher et l'affichage de l'accueil, l'utilisateur SHALL voir au plus ce fond noir puis l'écran de démarrage : jamais deux écrans de démarrage distincts, jamais un changement de couleur de fond, jamais d'icône système, jamais de double apparition de la mascotte. La disparition de l'écran de lancement du système SHALL être invisible (aucun fondu ni zoom d'une autre couleur ou d'une icône).

#### Scenario: lancement à froid
- **WHEN** le système affiche son écran de lancement pour SygixOs
- **THEN** son fond est le noir exact du fond de l'écran de démarrage et il ne montre ni icône ni image

#### Scenario: passage à l'écran de démarrage
- **WHEN** la première image de l'écran de démarrage est dessinée
- **THEN** l'écran de lancement du système disparaît sans animation visible : aucune image intermédiaire d'une autre couleur, aucune icône ; la mascotte n'apparaît qu'une fois, par son fondu d'entrée

#### Scenario: écran de lancement absent
- **WHEN** le système n'affiche pas d'écran de lancement pour SygixOs
- **THEN** la fenêtre de l'app montre le même fond noir jusqu'à la première image de l'écran de démarrage, sans flash d'une autre couleur

#### Scenario: relance sans démarrage à froid
- **WHEN** le système affiche son écran de lancement alors que l'accueil a déjà été créé dans le processus (activité recréée)
- **THEN** cet écran a le même fond noir, sans icône, et aucun écran de démarrage n'est affiché ensuite (« Écran de démarrage », scénario « accueil recréé hors démarrage à froid »)

### Requirement: Écran de démarrage sans animation
Quand les animations sont désactivées dans le système (accessibilité « Supprimer les animations », ou échelle de durée des animations à 0 dans les options pour les développeurs), l'écran de démarrage SHALL afficher la première image de la mascotte, fixe, sans flottement ni clignement ; la mascotte SHALL apparaître sans fondu et l'accueil SHALL remplacer l'écran de démarrage sans fondu. Les règles de durée de « Écran de démarrage » (600 ms au moins, accueil prêt, plafonds de 2 s et de 5 s) restent les mêmes. Le réglage SHALL être lu à chaque démarrage à froid, dans l'échelle de durée des animations du système (réglage que modifient ces deux options) ; une échelle d'animation propre aux apps forcée par le constructeur ne SHALL pas désactiver l'animation.

#### Scenario: animations désactivées
- **WHEN** le launcher démarre à froid alors que les animations sont désactivées dans le système
- **THEN** l'écran de démarrage montre la première image de la mascotte, immobile, dès sa première image ; dès que les règles de durée le permettent, l'accueil s'affiche d'un coup, sans fondu, le héro ayant le focus

#### Scenario: réglage changé entre deux démarrages
- **WHEN** l'utilisateur réactive les animations puis le launcher redémarre à froid
- **THEN** la mascotte est de nouveau animée et les fondus sont joués

#### Scenario: échelle forcée par le constructeur
- **WHEN** le système laisse l'échelle de durée des animations à sa valeur normale mais que le constructeur force à 0 l'échelle d'animation des apps
- **THEN** la mascotte est animée (flottement et clignement), apparaît et disparaît par ses fondus

#### Scenario: Retour sans fondu
- **WHEN** les animations sont désactivées et l'utilisateur presse Retour, avant ou après le remplacement instantané de l'écran de démarrage par l'accueil
- **THEN** avant le remplacement, le comportement normal du système s'applique : SygixOs quitte le premier plan, sans action sur l'accueil ; après le remplacement, l'accueil est interactif et Retour suit « Navigation 3 paliers », l'app restant au premier plan

### Requirement: Pastille
Le design system SHALL fournir un composant pastille unique, réutilisable par tout élément de l'interface qui doit signaler une nouveauté : un point plein de 6 dp de diamètre, bleu système (`#0A84FF`), ancré au coin supérieur droit de l'élément qu'il signale (ou en fin de ligne pour une ligne de liste). La pastille SHALL n'être jamais focusable, ne SHALL changer ni la taille, ni la position, ni le focus, ni la navigation D-pad de l'élément qui la porte, SHALL suivre cet élément dans tous ses mouvements (défilement, zoom de focus), et n'a aucune animation continue. Les fonctionnalités qui affichent une pastille (par exemple « Pastille de mise à jour » de `self-update`) SHALL utiliser ce composant, sans en redéfinir l'aspect.

#### Scenario: affichage
- **WHEN** un élément porte une pastille
- **THEN** un point bleu plein de 6 dp est dessiné à son coin supérieur droit (ou en fin de ligne pour une ligne de liste), par-dessus l'élément, sans décaler son contenu

#### Scenario: focus inchangé
- **WHEN** l'utilisateur déplace le focus sur ou à côté d'un élément qui porte une pastille
- **THEN** la pastille ne prend jamais le focus, et le focus, la taille et la position de l'élément sont identiques à ceux du même élément sans pastille

#### Scenario: élément en mouvement
- **WHEN** l'élément qui porte la pastille défile ou change d'échelle (focus)
- **THEN** la pastille reste à la même place relative sur l'élément

#### Scenario: écran au repos
- **WHEN** une pastille est affichée et que l'utilisateur ne touche à rien
- **THEN** la pastille ne provoque aucun redessin continu

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
