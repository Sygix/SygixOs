# Delta launcher-shell

Les trois premières exigences ci-dessous changent le rendu de la transition héro ↔ grille (défilement vertical continu à la place d'un fondu de couches). La quatrième, « Panneau Top Shelf au focus », garde sa règle de placement telle qu'elle est sur `main` ; seul son scénario « sortie et retour dans la grille » est reformulé pour la page d'un seul tenant : la page remonte jusqu'au héro, mais la position de la grille est conservée et retrouvée à la redescente. Les paliers, les règles de focus, la mise en pause et la libération du héro restent spécifiés par leurs exigences existantes (« Fond vidéo de secours », « Préchargement et mémoire »), auxquelles ces scénarios renvoient. « Focus d'une app disparue » reste valable telle quelle : quand la zone active se vide, le héro reprend le focus et la page revient au héro avec l'animation de « Navigation 3 paliers ». La courbe et les durées sont celles du design system, spécifiées par « Focus tvOS » (250-400 ms, courbe Apple) : elles ne sont pas recopiées ici. L'engrenage suit le héro comme le dock : son comportement est spécifié par « Icône réglages flottante » de `settings`, auquel ces scénarios renvoient. `p2c-upnext` (sur `main`, non archivé, implémenté après ce change) modifie aussi « Navigation 3 paliers » : voir `proposal.md`, « Dépendances et chevauchements ».

## MODIFIED Requirements

### Requirement: Navigation 3 paliers
Le DPAD SHALL naviguer héro → dock → grille (descend) et grille → dock → héro (monte) de façon déterministe : seule la zone active est focusable. L'accueil SHALL être une page d'un seul tenant qui défile verticalement : le héro (avec le dock en overlay) occupe le premier écran, la grille suit en dessous ; passer du dock à la grille et de la grille au dock est un défilement continu de la page, sans fondu ni saut, avec la courbe et la durée du design system (« Focus tvOS »). Au retour dans la zone grille, la page SHALL redescendre jusqu'à la position de la grille laissée à la sortie, et non d'un écran exactement (décision de Sygix) ; à la toute première entrée, elle descend d'un écran, au début de la grille.

#### Scenario: descente depuis le héro
- **WHEN** l'utilisateur presse bas depuis le héro
- **THEN** le premier élément du dock prend le focus, ou la grille si le dock est vide (dans ce cas la page défile comme au scénario « traversée du dock »)

#### Scenario: traversée du dock
- **WHEN** l'utilisateur descend depuis le dock
- **THEN** la page défile en une seule animation continue : le héro, le dock et l'engrenage (« Icône réglages flottante » de `settings`) sortent par le haut pendant que la grille remonte jusqu'à occuper tout l'écran, d'environ un écran à la première entrée, jusqu'à la position de la grille laissée à la sortie sinon (« Panneau Top Shelf au focus », scénario « sortie et retour dans la grille ») ; la grille prend le focus (dernière tuile visitée, sinon la première) ; la lecture du héro est mise en pause

#### Scenario: retour dans la grille à la position laissée
- **WHEN** l'utilisateur a parcouru la grille jusqu'à une position qui n'est pas son début, la quitte (Retour, ou remontée vers le dock ou le héro), puis y redescend
- **THEN** la page redescend en une seule animation continue jusqu'à la position de la grille laissée à la sortie, et non d'un écran exactement : la dernière tuile visitée reprend le focus à la même place à l'écran qu'au moment de la sortie

#### Scenario: dock jamais visible en vue grille
- **WHEN** la zone grille est active, pendant ou après le défilement
- **THEN** le dock n'est visible à aucun moment au-dessus ou en travers de la grille : il quitte l'écran avec le héro et ne réapparaît qu'avec lui ; aucune position intermédiaire ne montre le dock sur la grille

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la première rangée de la grille
- **THEN** la page défile en sens inverse avec la même animation continue : la grille redescend pendant que le héro, le dock et l'engrenage reviennent par le haut ; le dock reprend le focus (ou le héro si le dock est vide) ; haut depuis le dock rend le focus au héro et relance sa lecture

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
Le home SHALL afficher un dock Liquid Glass (rail overlay bas semi-transparent) contenant les apps épinglées, qui restent également présentes dans la grille. Le dock overlay le héro et SHALL se déplacer avec lui : il n'est visible que lorsque le héro l'est. Son matériau verre SHALL rester actif tant qu'une partie du héro est à l'écran, y compris pendant le défilement, et n'être coupé qu'une fois le héro entièrement sorti de l'écran (décision de Sygix) : le dock ne change pas d'aspect au début de la descente, et la bascule du verre ne recrée pas son contenu.

#### Scenario: état initial
- **WHEN** le home s'ouvre
- **THEN** le dock est visible en overlay bas sur le héro, le focus est sur le héro

#### Scenario: taille tvOS
- **WHEN** le dock contient jusqu'à 5 apps
- **THEN** ses tuiles ont la taille des tuiles de la grille et le dock, centré, s'ajuste à leur nombre

#### Scenario: tuiles adaptatives
- **WHEN** le dock contient plus de 5 apps
- **THEN** les tuiles se répartissent uniformément dans la largeur de la grille et réduisent leur taille automatiquement

#### Scenario: épinglage
- **WHEN** l'utilisateur épingle ou retire une app via le menu contextuel (appui long, OK valide directement)
- **THEN** le dock est mis à jour et l'app reste dans la grille

#### Scenario: solidaire du héro
- **WHEN** la page défile vers la grille ou revient vers le héro (« Navigation 3 paliers »)
- **THEN** le dock suit exactement le mouvement du héro, sans fondu propre ni décalage ; en vue grille il est entièrement hors écran et son matériau verre n'est plus calculé (« Préchargement et mémoire »)

#### Scenario: verre du dock pendant le défilement
- **WHEN** la page défile du dock vers la grille, puis de la grille vers le dock
- **THEN** à la descente, le verre du dock reste actif et identique tant que le héro est au moins en partie à l'écran, et n'est coupé qu'une fois le héro entièrement sorti ; à la remontée, il redevient actif dès que le héro réapparaît ; à aucune de ces bascules les tuiles du dock ne sont recréées (le focus et l'état des tuiles sont conservés)

### Requirement: Fond de la zone grille
Dans la grille, le fond SHALL être un dégradé neutre uni type tvOS, le héro étant complètement masqué parce qu'il est sorti de l'écran par le haut.

#### Scenario: révélation de la grille
- **WHEN** l'utilisateur passe en zone grille
- **THEN** à la fin du défilement le héro est entièrement hors écran (aucun pixel du héro, du dock ni de l'engrenage n'est visible), le fond est le dégradé neutre (pas d'aerial, pas de posters en fond de grille), sans fondu de couches

#### Scenario: pendant le défilement
- **WHEN** la page est entre le héro et la grille
- **THEN** le héro et la grille sont chacun visibles pour la part de l'écran qu'ils occupent, sans zone noire ni image figée entre les deux ; le dégradé neutre est le fond de la partie grille dès le début du mouvement

#### Scenario: héro hors écran
- **WHEN** le héro est entièrement hors écran
- **THEN** sa lecture est en pause et ses ressources sont libérées conformément à « Préchargement et mémoire » et « Fond vidéo de secours »

### Requirement: Panneau Top Shelf au focus
Le panneau d'aperçu SHALL s'ouvrir à la demande au-dessus de la rangée focusée, et se refermer quand l'app focusée n'a rien à montrer ; aucune place n'est réservée quand il est fermé. Le défilement de la grille SHALL suivre une règle de placement unique, appliquée au bloc focusé (la rangée focusée, plus le panneau quand il est ouvert au-dessus d'elle) : le bloc reste entre la marge haute de la grille (40 dp sous le haut de l'écran) et la marge basse (20 dp au-dessus du bas de l'écran, l'espacement entre rangées). La grille ne défile que pour y ramener le bloc, et ce défilement est animé avec la durée et la courbe de l'expansion du panneau. Tant que la zone grille est active, la page ne remonte jamais au-dessus du début de la grille : le héro reste hors écran.

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
