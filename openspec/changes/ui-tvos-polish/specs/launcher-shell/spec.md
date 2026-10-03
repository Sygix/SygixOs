# Delta launcher-shell

## MODIFIED Requirements

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
Le launcher SHALL animer le focus des tuiles d'apps de la grille et du dock à la tvOS : la tuile focusée se soulève (zoom, légère montée, ombre portée douce, reflet blanc en diagonale), sans halo ni liseré, de façon à ce que la tuile active soit identifiable d'un coup d'œil. La tuile agrandie SHALL ne recouvrir aucune tuile voisine et SHALL n'être coupée par aucun conteneur.

#### Scenario: focus
- **WHEN** une tuile prend le focus
- **THEN** elle zoome d'environ 1,08x en montant légèrement, une ombre portée douce apparaît sous elle et un reflet blanc en diagonale éclaire son visuel ; aucun halo, coloré ou non, et aucun liseré ; transition 250-400ms courbe Apple ; en zone grille le panneau Top Shelf reflète l'app focusée

#### Scenario: rien ne déborde
- **WHEN** une tuile de la grille ou du dock a le focus
- **THEN** la tuile agrandie ne recouvre aucune tuile voisine, l'espacement entre tuiles et entre rangées absorbant le zoom, et elle reste entière : ni la rangée, ni la grille, ni le dock ne la coupent

#### Scenario: nom de l'app sous la tuile
- **WHEN** une tuile de la grille a le focus
- **THEN** le nom de l'app s'affiche centré sous la tuile, dans l'espace entre les rangées, sans déplacer aucune tuile ; il disparaît quand la tuile perd le focus

### Requirement: Thème
L'UI SHALL être toujours sombre, sur fond noir pur, avec des surfaces Liquid Glass façon tvOS 26 : verre sombre transparent (teinte sombre translucide sur l'arrière-plan flouté), fine bordure claire, fin reflet clair sur le bord haut et ombre légère sous la surface. Le flou SHALL être calculé en direct, à partir d'une image réduite de l'arrière-plan, et seulement sous les surfaces verre (dock, capsule heure et réglages, menu contextuel) ; aucun autre élément n'est flouté.

- Toujours sombre, noir pur, posters plein cadre, police type Inter
- Relief par une ombre portée douce et un reflet clair, jamais par un halo coloré

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent

#### Scenario: surfaces verre
- **WHEN** le dock, la capsule heure et réglages (« Capsule heure et réglages » de `settings`) ou le menu contextuel sont affichés
- **THEN** ils utilisent le matériau verre sombre (arrière-plan flouté en direct, teinte sombre translucide, fine bordure claire, reflet sur le bord haut, ombre légère) ; le panneau du menu contextuel est nettement plus foncé que le dock et la capsule ; si l'appareil ne supporte pas l'effet, la surface reste sombre et translucide, sans flou et sans crash

#### Scenario: bouton sans flou
- **WHEN** le bouton d'ouverture du héro est affiché au repos
- **THEN** il a un fond sombre translucide, un texte blanc et une fine bordure façon verre, sans flou d'arrière-plan ; au focus il a un fond blanc, un texte noir, un léger zoom et une ombre

#### Scenario: coût du flou
- **WHEN** une surface verre est affichée
- **THEN** son flou n'est calculé que sur la zone qu'elle couvre, à partir d'une image réduite de l'arrière-plan ; une surface verre hors écran n'est plus calculée (« Préchargement et mémoire »)

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
Le home SHALL afficher un dock en verre sombre (« Thème », rail overlay bas) contenant les apps épinglées, qui restent également présentes dans la grille. Le dock overlay le héro et SHALL se déplacer avec lui : il n'est visible que lorsque le héro l'est. Son matériau verre SHALL rester actif tant qu'une partie du héro est à l'écran, y compris pendant le défilement, et n'être coupé qu'une fois le héro entièrement sorti de l'écran (décision de Sygix) : le dock ne change pas d'aspect au début de la descente, et la bascule du verre ne recrée pas son contenu.

#### Scenario: état initial
- **WHEN** le home s'ouvre
- **THEN** le dock est visible en overlay bas sur le héro, le focus est sur le héro

#### Scenario: taille tvOS
- **WHEN** le dock contient une ou plusieurs apps
- **THEN** toutes ses tuiles ont la même taille fixe, plus petite que celle des tuiles de la grille (environ 80 %), quel que soit le nombre d'apps, et le dock, centré, s'ajuste à leur nombre

#### Scenario: tuiles adaptatives
- **WHEN** le dock contient plus de 5 apps
- **THEN** ses tuiles ne réduisent pas leur taille : elles gardent la taille fixe du scénario « taille tvOS », et le dock reste centré tant qu'il tient dans la largeur de l'écran

#### Scenario: épinglage
- **WHEN** l'utilisateur épingle ou retire une app via le menu contextuel (appui long, OK valide directement)
- **THEN** le dock est mis à jour et l'app reste dans la grille

#### Scenario: solidaire du héro
- **WHEN** la page défile vers la grille ou revient vers le héro (« Navigation 3 paliers »)
- **THEN** le dock suit exactement le mouvement du héro, sans fondu propre ni décalage ; en vue grille il est entièrement hors écran et son matériau verre n'est plus calculé (« Préchargement et mémoire »)

#### Scenario: verre du dock pendant le défilement
- **WHEN** la page défile du dock vers la grille, puis de la grille vers le dock
- **THEN** à la descente, le verre du dock reste actif et identique tant que le héro est au moins en partie à l'écran, et n'est coupé qu'une fois le héro entièrement sorti ; à la remontée, il redevient actif dès que le héro réapparaît ; à aucune de ces bascules les tuiles du dock ne sont recréées (le focus et l'état des tuiles sont conservés)

### Requirement: Panneau Top Shelf au focus
Le panneau d'aperçu SHALL s'ouvrir à la demande au-dessus de la rangée focusée, et se refermer quand l'app focusée n'a rien à montrer ; aucune place n'est réservée quand il est fermé. Le défilement de la grille SHALL suivre une règle de placement unique, appliquée au bloc focusé (la rangée focusée, plus le panneau quand il est ouvert au-dessus d'elle) : le bloc reste entre la marge haute de la grille (40 dp sous le haut de l'écran) et la marge basse (32 dp au-dessus du bas de l'écran, l'espacement entre rangées, qui laisse la place au nom de l'app sous la tuile focusée). La grille ne défile que pour y ramener le bloc, et ce défilement est animé avec la durée et la courbe de l'expansion du panneau. Tant que la zone grille est active, la page ne remonte jamais au-dessus du début de la grille : le héro reste hors écran.

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

#### Scenario: défilement automatique
- **WHEN** plusieurs programmes sont disponibles
- **THEN** le héro passe au programme suivant toutes les 12 s environ par fondu croisé (1,4 s, easing Apple), le visuel courant zoome lentement (Ken Burns), sans son, sans aperçu de l'élément suivant

#### Scenario: navigation manuelle
- **WHEN** l'utilisateur presse gauche/droite sur le héro
- **THEN** le héro passe au programme précédent/suivant avec le même fondu et le minuteur d'avance automatique repart

#### Scenario: vidéo d'aperçu
- **WHEN** le programme expose une vidéo d'aperçu (previewVideoUri)
- **THEN** elle est lue plein écran en muet à la place du poster, repli sur le poster si la lecture échoue

#### Scenario: métadonnées
- **WHEN** un programme ouvrable est affiché
- **THEN** titre, app source et barre de progression (si connue) apparaissent en bas à gauche, suivis d'un bouton « Ouvrir » (« Reprendre » si progression) qui porte le focus du héro, au style de « Thème » (scénario « bouton sans flou ») ; OK sur ce bouton ouvre le contenu

#### Scenario: lisibilité sur un poster clair
- **WHEN** le visuel affiché est clair, jusqu'au blanc (pire cas)
- **THEN** un voile sombre en dégradé depuis le bas et depuis la gauche passe derrière le titre, les métadonnées et le bouton, et un léger voile radial assombrit le coin haut droit derrière la capsule heure et réglages ; le texte blanc et le bouton restent lisibles, le reste du visuel n'est pas voilé

#### Scenario: sans progression
- **WHEN** le programme affiché n'expose pas de progression
- **THEN** aucun espace vide ne sépare les métadonnées du bouton : l'écart entre la dernière ligne de métadonnées et le bouton est le même que l'écart entre la barre de progression et le bouton quand elle est présente, et un titre d'une ligne ne réserve pas de place pour une seconde ligne

### Requirement: Préchargement et mémoire
Le launcher SHALL valider les visuels avant de les afficher, en bornant ce travail : les premiers visuels du héro au chargement, les affiches d'une app quand le focus s'y pose. Il SHALL aussi limiter sa consommation mémoire et GPU.

#### Scenario: validation au chargement
- **WHEN** les programmes sont chargés
- **THEN** seuls les premiers visuels du héro sont vérifiés en arrière-plan (chargement, largeur ≥ 1080 px) ; ils sont gardés en mémoire, les suivants en cache disque ; le provider pouvant publier des centaines de programmes, aucun autre visuel n'est vérifié à ce moment

#### Scenario: validation à la demande
- **WHEN** le focus se pose sur une tuile de la grille
- **THEN** quelques affiches de cette app sont vérifiées une seule fois ; seules les affiches validées alimentent son panneau

#### Scenario: économie de ressources
- **WHEN** le héro est masqué (zone grille) ou une surface verre est invisible
- **THEN** le lecteur vidéo est libéré, les animations de fond, le Ken Burns du visuel du héro et le flou d'arrière-plan sont arrêtés ; les images sont décodées en RGB565 avec au plus deux décodeurs simultanés

#### Scenario: écran au repos
- **WHEN** aucune touche n'est pressée et aucune transition n'est en cours
- **THEN** seules les animations spécifiées encore visibles redessinent l'écran (vidéo d'aperçu, Ken Burns du héro et du panneau Top Shelf, dégradé animé du repli, défilement automatique du héro) ; aucune animation d'un élément hors écran ne tourne, et une animation continue ne provoque aucune recomposition à chaque image

