# Delta launcher-shell

## MODIFIED Requirements

### Requirement: Grille d'apps
Le launcher SHALL auto-détecter toutes les apps TV installées et les afficher en grille.

Auto-détection de toutes les apps TV installées (category LEANBACK_LAUNCHER / LAUNCHER).

La zone grille SHALL être découpée en sections titrées, comme les en-têtes de section de tvOS : le titre « À suivre » au-dessus de la rangée Up Next (« Rangée Up Next ») et le titre « Applications » au-dessus des rangées d'apps. Les sections se suivent dans l'ordre fixé par le réglage « Position d'Up Next » de `settings` : « À suivre » puis « Applications » quand il vaut « Avant les applications » (défaut), « Applications » puis « À suivre » quand il vaut « Après les applications ». Chaque titre SHALL être aligné à gauche sur la première carte ou tuile de sa section (`Dimens.ScreenMarginH`) et ne jamais prendre le focus. Les deux titres SHALL utiliser le style `SygixTypography.titleLarge` et la couleur `SygixColors.OnDarkSecondary` du design system (`core/designsystem`), conformément aux maquettes Penpot validées (« SygixOs Maquette », page « TV », écrans 4.1, 7.1, 7.2 et 7.5 ; composant « Titre de section ») ; le haut du premier titre est sur la marge haute de la grille (`Dimens.GridTopMargin`), un titre est séparé de sa première ligne par `Dimens.SectionTitleGap` et de la dernière ligne de la section précédente par `Dimens.GridRowSpacing`. Un titre appartient à sa section : il défile avec elle et, quand la rangée Up Next est masquée, le titre « À suivre » est masqué avec elle, sans place réservée. En erreur, la rangée n'est pas masquée : la carte d'erreur remplace son contenu (« États de la rangée » de up-next) et le titre « À suivre » reste au-dessus d'elle. Le haut de la zone grille, d'où part son défilement (« Panneau Top Shelf au focus »), est le haut du titre de sa première section.

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

#### Scenario: titres de section
- **WHEN** la zone grille est affichée avec la rangée Up Next et le réglage « Position d'Up Next » vaut « Avant les applications »
- **THEN** le titre « À suivre » est au-dessus de la rangée Up Next, puis le titre « Applications » est au-dessus de la première rangée d'apps ; avec « Après les applications », le titre « Applications » vient en premier et le titre « À suivre » est sous la dernière rangée d'apps, au-dessus de la rangée Up Next

#### Scenario: titres jamais focusables
- **WHEN** l'utilisateur passe au D-pad d'une section à l'autre (bas depuis la rangée Up Next, haut depuis la première rangée d'apps, ou l'inverse avec « Après les applications »)
- **THEN** le focus passe directement d'une ligne à l'autre sans jamais se poser sur un titre ; le titre de la section d'arrivée est entièrement visible au-dessus de sa première ligne

#### Scenario: section Up Next masquée
- **WHEN** la rangée Up Next est masquée (réglage « Afficher Up Next » désactivé, aucun contenu ou permission refusée)
- **THEN** le titre « À suivre » n'est pas affiché et aucune place ne lui est réservée ; « Applications » est le seul titre de la zone grille et son haut est le haut de la zone grille

#### Scenario: aucune app TV détectée
- **WHEN** aucune app TV n'est installée (maquette Penpot 4.4)
- **THEN** la section « Applications » n'a pas de titre : seul le message « Aucune app TV détectée » est affiché, centré dans l'espace des rangées d'apps ; la section « À suivre » suit ses propres règles d'affichage

#### Scenario: titre au-dessus de la carte d'erreur
- **WHEN** la rangée Up Next est en erreur au premier chargement et que la carte d'erreur remplace son contenu
- **THEN** le titre « À suivre » reste affiché au-dessus de la carte, à la position réglée, et fait partie du bloc focusé quand la carte a le focus

#### Scenario: style des titres
- **WHEN** les deux titres de section sont affichés
- **THEN** ils sont en `SygixTypography.titleLarge` et `SygixColors.OnDarkSecondary`, chacun aligné à gauche sur la première carte ou tuile de sa section, à `Dimens.SectionTitleGap` au-dessus de sa première ligne ; le second titre est à `Dimens.GridRowSpacing` sous la dernière ligne de la section précédente

### Requirement: Navigation 3 paliers
Le DPAD SHALL naviguer héro → dock → grille (descend) et grille → dock → héro (monte) de façon déterministe : seule la zone active est focusable. La zone grille comprend, sur le même fond et chacune sous son titre (« Grille d'apps »), la section « À suivre » (rangée Up Next) et la section « Applications » (rangées d'apps) : la rangée Up Next est la première ligne de la zone grille quand le réglage « Position d'Up Next » vaut « Avant les applications » (défaut), sa dernière ligne quand il vaut « Après les applications » ; elle n'est pas un palier distinct et elle est sautée, avec son titre, quand elle est masquée (réglage « Afficher Up Next » désactivé, aucun contenu, permission refusée). L'accueil SHALL être une page d'un seul tenant qui défile verticalement : le héro (avec le dock en overlay) occupe le premier écran, la zone grille suit en dessous ; passer du dock à la zone grille et de la zone grille au dock est un défilement continu de la page, sans fondu ni saut, avec la courbe et la durée du design system (« Focus tvOS »). Au retour dans la zone grille, la page SHALL redescendre jusqu'à la position de la zone grille laissée à la sortie, et non d'un écran exactement, et l'élément laissé (carte Up Next ou tuile d'app) SHALL reprendre le focus (décision de Sygix) ; à la toute première entrée, elle descend d'un écran, au début de la zone grille : le titre de la première section en haut, sa première ligne juste en dessous, quelle qu'elle soit (rangée Up Next ou première rangée d'apps). Quand la carte Up Next focusée, ou la dernière visitée, disparaît alors que d'autres cartes restent, le focus SHALL passer à la carte voisine : celle qui prend sa position dans la rangée (la suivante), ou la précédente si c'était la dernière (décision de Sygix). Si la rangée devient vide ou masquée, le repli standard de « Focus d'une app disparue » SHALL s'appliquer à la zone grille, rangée Up Next comprise.

#### Scenario: descente depuis le héro
- **WHEN** l'utilisateur presse bas depuis le héro
- **THEN** le premier élément du dock prend le focus, ou la zone grille si le dock est vide (dans ce cas la page défile comme au scénario « traversée du dock »)

#### Scenario: traversée du dock
- **WHEN** l'utilisateur descend depuis le dock
- **THEN** la page défile en une seule animation continue : le héro, le dock et la capsule heure et réglages (« Capsule heure et réglages » de `settings`) sortent par le haut pendant que la zone grille remonte jusqu'à occuper tout l'écran, d'environ un écran à la première entrée, jusqu'à la position de la zone grille laissée à la sortie sinon (« Panneau Top Shelf au focus », scénario « sortie et retour dans la grille ») ; la zone grille prend le focus (dernière carte Up Next ou tuile visitée, sinon le premier élément de sa première ligne) ; la lecture du héro est mise en pause

#### Scenario: rangée Up Next affichée en tête
- **WHEN** l'utilisateur descend depuis le dock pour la première fois (aucune carte ni tuile de la zone grille encore visitée), le réglage vaut « Avant les applications » et la rangée Up Next est affichée
- **THEN** le titre « À suivre » arrive en haut de la zone grille, la rangée Up Next juste en dessous, et sa première carte prend le focus ; un bas de plus passe à la première rangée d'apps, sous le titre « Applications », sans changement de fond

#### Scenario: rangée masquée
- **WHEN** l'utilisateur descend depuis le dock et que la rangée Up Next est masquée
- **THEN** la rangée et son titre sont sautés, sans place réservée : à la première entrée, le titre « Applications » est en haut de la zone grille et la première tuile de la première rangée d'apps prend le focus ; sinon le scénario « traversée du dock » s'applique aux rangées d'apps

#### Scenario: déplacements dans la rangée Up Next
- **WHEN** l'utilisateur presse gauche ou droite alors que le focus est sur la rangée Up Next
- **THEN** le focus reste dans la rangée, sans boucle aux bords

#### Scenario: retour dans la grille à la position laissée
- **WHEN** l'utilisateur a parcouru la zone grille jusqu'à un élément qui n'est pas son premier élément, carte de la rangée Up Next ou tuile d'une rangée d'apps, la quitte (Retour, ou remontée vers le dock ou le héro), puis y redescend
- **THEN** la page redescend en une seule animation continue jusqu'à la position de la zone grille laissée à la sortie, et non d'un écran exactement : cet élément reprend le focus à la même place à l'écran qu'au moment de la sortie, qu'il soit dans la rangée Up Next ou dans les rangées d'apps

#### Scenario: carte laissée disparue avant le retour
- **WHEN** la dernière carte Up Next visitée a disparu de la rangée pendant que l'utilisateur était hors de la zone grille, puis il y redescend
- **THEN** la carte qui occupe désormais sa position dans la rangée prend le focus, ou la précédente si la carte disparue était la dernière ; si la rangée est devenue vide ou masquée, le scénario « rangée devenue vide » s'applique

#### Scenario: dock jamais visible en vue grille
- **WHEN** la zone grille est active, pendant ou après le défilement
- **THEN** le dock n'est visible à aucun moment au-dessus ou en travers de la grille : il quitte l'écran avec le héro et ne réapparaît qu'avec lui ; aucune position intermédiaire ne montre le dock sur la grille

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la première ligne de la zone grille (la rangée Up Next si elle est affichée en tête, sinon la première rangée d'apps)
- **THEN** la page défile en sens inverse avec la même animation continue : la zone grille redescend pendant que le héro, le dock et la capsule reviennent par le haut ; le dock reprend le focus (ou le héro si le dock est vide) ; haut depuis le dock rend le focus au héro et relance sa lecture

#### Scenario: remontée depuis les apps
- **WHEN** l'utilisateur presse haut depuis la première rangée d'apps
- **THEN** la rangée Up Next prend le focus si elle est affichée en tête (dernière carte visitée, sinon la première), sans défilement vers le héro ; sinon le scénario « remontée » s'applique

#### Scenario: position après les applications
- **WHEN** le réglage « Position d'Up Next » vaut « Après les applications » et que la rangée est affichée
- **THEN** la rangée Up Next est la dernière ligne de la zone grille, sous son titre « À suivre » : bas depuis la dernière rangée d'apps lui donne le focus, bas depuis la rangée ne fait rien, haut depuis la rangée revient à la dernière rangée d'apps, et la première ligne de la zone grille est la première rangée d'apps, sous le titre « Applications »

#### Scenario: carte focusée disparue
- **WHEN** la carte Up Next qui a le focus disparaît de la rangée (rechargement du TV Provider, app source désactivée) et qu'il reste au moins une carte
- **THEN** la carte qui prend sa position dans la rangée (la suivante) reçoit le focus, ou la précédente si la carte disparue était la dernière ; le focus ne quitte pas la rangée et la position verticale de la page ne change pas

#### Scenario: rangée devenue vide
- **WHEN** la rangée Up Next devient vide ou masquée (dernière carte disparue, réglage « Afficher Up Next » désactivé, permission retirée) alors qu'elle a le focus, ou qu'elle porte la dernière carte visitée et que l'utilisateur revient dans la zone grille ou au launcher
- **THEN** la rangée et son titre sont masqués et le repli de « Focus d'une app disparue » s'applique à la zone grille : la première tuile de la première rangée d'apps prend le focus si la rangée était avant les applications, la dernière tuile de la dernière rangée d'apps si elle était après ; si la zone grille ne contient aucune app, le héro reprend le focus

#### Scenario: retour depuis une app
- **WHEN** l'utilisateur revient au launcher après avoir ouvert une app depuis la rangée Up Next
- **THEN** la position précédente dans la zone grille est restaurée et le focus revient sur la carte d'origine, même si le rechargement l'a déplacée dans la rangée ; si cette carte a disparu, les scénarios « carte focusée disparue » et « rangée devenue vide » s'appliquent ; le repli des apps disparues du dock et des rangées d'apps reste celui de « Focus d'une app disparue »

#### Scenario: retour depuis la grille
- **WHEN** l'utilisateur presse Retour depuis la zone grille, quelle que soit la ligne focusée (rangée Up Next ou rangée d'apps)
- **THEN** la page défile jusqu'au héro avec la même animation continue (une seule animation, même si la grille avait été parcourue en profondeur), le héro reprend le focus et sa lecture

#### Scenario: continuité de l'animation
- **WHEN** un défilement héro ↔ grille est en cours et l'utilisateur presse à nouveau haut, bas ou Retour
- **THEN** la page repart de sa position courante vers la nouvelle cible sans saut ni retour à une position de départ

#### Scenario: cohabitation avec la Top Shelf
- **WHEN** le focus est sur la rangée Up Next
- **THEN** aucun panneau Top Shelf n'est ouvert ; le panneau reste lié au focus des tuiles d'apps

#### Scenario: couches inactives
- **WHEN** une zone n'est pas active
- **THEN** aucun de ses éléments ne peut prendre le focus ; gauche et droite restent dans la zone active

### Requirement: Panneau Top Shelf au focus
Le panneau d'aperçu SHALL s'ouvrir à la demande au-dessus de la rangée focusée, et se refermer quand l'app focusée n'a rien à montrer ; aucune place n'est réservée quand il est fermé. Le défilement de la grille SHALL suivre une règle de placement unique, appliquée au bloc focusé : la ligne focusée de la zone grille (rangée d'apps ou rangée Up Next), plus le titre de sa section quand elle est la première ligne de sa section (« Grille d'apps »), plus le panneau quand il est ouvert au-dessus d'elle, dans l'ordre titre de section, panneau, ligne focusée (maquette Penpot 4.2 : le panneau reste immédiatement au-dessus de la rangée, sous le titre) ; le bloc reste entre la marge haute de la grille (40 dp sous le haut de l'écran) et la marge basse (32 dp au-dessus du bas de l'écran, l'espacement entre rangées). La grille ne défile que pour y ramener le bloc, et ce défilement est animé avec la durée et la courbe de l'expansion du panneau. Tant que la zone grille est active, la page ne remonte jamais au-dessus du début de la zone grille, le haut du titre de sa première section : le héro reste hors écran.

#### Scenario: arrivée dans la grille
- **WHEN** l'utilisateur descend du dock vers la grille
- **THEN** la grille occupe tout l'écran, sans emplacement réservé ni panneau ; à la première entrée, le haut du titre de la première section est sur la marge haute et sa première ligne est juste en dessous

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

### Requirement: Rangée Up Next
Le home SHALL afficher une rangée Up Next dans la zone grille, sous le titre de section « À suivre » (« Grille d'apps »), première ligne par défaut, dernière ligne si le réglage « Position d'Up Next » vaut « Après les applications », alimentée par le TV Provider Android, tant que le réglage « Afficher Up Next » de `settings` est activé ; son contenu, son dédoublonnage, ses cartes, ses états et l'ouverture des items sont spécifiés par la capability up-next.

#### Scenario: contenu
- **WHEN** des programmes watch next sont publiés dans le TV Provider et que « Afficher Up Next » est activé
- **THEN** la rangée est affichée dans la zone grille, sous son titre « À suivre », sur le fond de la zone grille, avec ses cartes et barres de progression (« Carte Up Next » de up-next)

#### Scenario: sans contenu
- **WHEN** aucun programme n'est visible dans le TV Provider, ou que la permission est refusée
- **THEN** la rangée et son titre sont masqués et la navigation de la zone grille commence directement par les rangées d'apps

#### Scenario: réglage désactivé
- **WHEN** « Afficher Up Next » est désactivé alors que des programmes watch next sont publiés
- **THEN** ni la rangée ni son titre ne sont affichés, aucune place ne leur est réservée, la navigation de la zone grille ne passe que par les rangées d'apps ; réactiver le réglage réaffiche la rangée à la position réglée, sans redémarrage

### Requirement: Sélection des apps sources
Le launcher SHALL permettre, dans ses réglages, de cocher les apps dont les programmes alimentent le héro, le Top Shelf et la rangée Up Next ; par défaut toutes les apps installées sont retenues. L'UI de ce réglage est spécifiée par « Apps sources » de la capability settings.

#### Scenario: app décochée
- **WHEN** l'utilisateur décoche une app dans les réglages du launcher
- **THEN** ses programmes n'apparaissent plus dans le héro, le Top Shelf ni la rangée Up Next, sans redémarrage
