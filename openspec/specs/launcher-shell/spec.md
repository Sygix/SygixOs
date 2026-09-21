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

### Requirement: Focus tvOS
Le launcher SHALL animer le focus des tuiles à la tvOS (zoom, liseré, ombre, easing), de façon à ce que la tuile active soit identifiable d'un coup d'œil.

#### Scenario: focus
- **WHEN** une tuile prend le focus
- **THEN** zoom léger (~1.07x) et halo radial diffus, compact (ne déborde pas sur les tuiles voisines), de la couleur dominante du visuel de la tuile (style Google Play, lisible sur fond sombre), sans liseré, transition 250-400ms courbe Apple ; en zone grille le panneau Top Shelf reflète l'app focusée

### Requirement: Rangée Up Next
Le home SHALL afficher une rangée Up Next fusionnant Jellyfin puis BetaSeries.

#### Scenario: contenu
- **WHEN** des items Jellyfin en cours existent THEN rangée au-dessus de la grille : posters + barre de progression, fusion déterministe Jellyfin puis BetaSeries (v1.x), tri par date d'activité

### Requirement: Thème
L'UI SHALL être sombre (v1) avec des surfaces Liquid Glass : matériau verre réfractant (réfraction des bords, reflet spéculaire, teinte claire), sur fond noir pur.

- Sombre uniquement en v1, noir pur, posters plein cadre, police type Inter
- Aucune ombre noire : le relief vient des halos clairs ou colorés

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent

#### Scenario: surfaces verre
- **WHEN** le dock, le bouton d'ouverture du héro ou le menu contextuel sont affichés
- **THEN** ils utilisent le matériau verre (arrière-plan flouté et réfracté, reflet spéculaire sur les bords) ; si l'appareil ne supporte pas l'effet, la surface reste translucide sans flou, sans crash

### Requirement: Navigation
Le launcher SHALL être navigable au DPAD uniquement.

- DPAD natif uniquement ; Home de la télécommande retourne au launcher

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent

### Requirement: Écran initial du home
Le home SHALL s'ouvrir sur un héro plein écran sans cadre : visuel d'un programme publié par les apps installées ou, à défaut, vidéo nature en boucle, avec le dock en overlay bas.

#### Scenario: état initial
- **WHEN** le launcher démarre
- **THEN** le héro occupe tout l'écran (aucune carte, aucun aperçu du suivant), il détient le focus dès son affichage, et le dock est visible en overlay bas semi-transparent

#### Scenario: fallback sans contenu
- **WHEN** aucune app ne publie de programme, ou la permission est refusée
- **THEN** le héro joue les vidéos nature en boucle ; tant qu'aucune vidéo ne joue, un dégradé sombre animé est affiché, jamais d'écran noir ni de crash

### Requirement: Navigation 3 paliers
Le DPAD SHALL naviguer héro → dock → grille (descend) et grille → dock → héro (monte) de façon déterministe : seule la zone active est focusable.

#### Scenario: descente depuis le héro
- **WHEN** l'utilisateur presse bas depuis le héro
- **THEN** le premier élément du dock prend le focus, ou la grille si le dock est vide

#### Scenario: traversée du dock
- **WHEN** l'utilisateur descend depuis le dock
- **THEN** la grille plein écran prend le focus (dernière tuile visitée, sinon la première), le dock et le héro se masquent, la lecture du héro est mise en pause

#### Scenario: remontée
- **WHEN** l'utilisateur presse haut depuis la première rangée de la grille
- **THEN** le dock reprend le focus (ou le héro si le dock est vide) ; haut depuis le dock rend le focus au héro et relance sa lecture

#### Scenario: couches inactives
- **WHEN** une zone n'est pas active
- **THEN** aucun de ses éléments ne peut prendre le focus ; gauche et droite restent dans la zone active

### Requirement: Dock d'apps épinglées
Le home SHALL afficher un dock Liquid Glass (rail overlay bas semi-transparent) contenant les apps épinglées, qui restent également présentes dans la grille. Le dock overlay le héro.

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

### Requirement: Persistance de la grille
L'ordre des apps et les épinglages SHALL être persistés localement (DataStore) et restaurés au démarrage ; les apps nouvellement installées s'ajoutent à la fin par ordre alphabétique.

#### Scenario: réorganisation
- **WHEN** l'utilisateur choisit « Déplacer » sur une tuile de la grille puis presse les flèches (gauche/droite : une case, haut/bas : une rangée, par insertion), OK pour valider
- **THEN** la tuile suit le focus avec un liseré de déplacement, un rappel des touches s'affiche, l'ordre est persisté et conservé après redémarrage

#### Scenario: annulation
- **WHEN** l'utilisateur presse Retour pendant un déplacement
- **THEN** l'ordre d'avant le déplacement est restauré

### Requirement: Contenu héro TV Provider
Le héro SHALL afficher les programmes publiés dans le TV Provider système par les apps installées (watch next + preview programs), sans autre configuration que l'octroi de la permission système de lecture des programmes TV.

#### Scenario: permission
- **WHEN** le launcher démarre sans `android.permission.READ_TV_LISTINGS`
- **THEN** la demande système s'affiche ; dès l'octroi le héro se recharge sans redémarrage ; en cas de refus le héro reste sur le fallback sans erreur visible

#### Scenario: programmes disponibles
- **WHEN** une ou plusieurs apps installées publient des programmes dans le TV Provider
- **THEN** le héro les affiche : visuel (previewVideoUri, sinon posterArtUri, sinon thumbnailUri), titre, app source et progression quand disponible, ordre déterministe (reprises d'abord, puis plus récents), toutes apps confondues, sans liste d'apps codée en dur ; les requêtes au provider ne portent aucune clause de sélection, le filtrage par app est fait côté launcher

#### Scenario: fallback
- **WHEN** aucune app ne publie de programme dans le TV Provider
- **THEN** le héro utilise le fond vidéo de secours, sans crash ni écran vide

### Requirement: Clic programme publié
Le clic sur un poster du TV Provider SHALL ouvrir le contenu via l'intent publié par l'app source.

#### Scenario: ouverture
- **WHEN** l'utilisateur clique sur un poster héro issu du TV Provider
- **THEN** l'app source s'ouvre sur la fiche du contenu (intent du programme), fallback : lancement simple de l'app si l'intent échoue

#### Scenario: progression
- **WHEN** le programme expose une position de lecture (watch next)
- **THEN** une barre de progression est visible sur le poster héro

### Requirement: Panneau Top Shelf au focus
En zone grille, un emplacement de panneau SHALL être réservé au-dessus de la rangée active, la rangée active restant à hauteur fixe, sans bloquer la navigation DPAD.

#### Scenario: insertion
- **WHEN** une tuile d'une rangée prend le focus
- **THEN** le panneau pleine largeur au format Apple Top Shelf (2,67:1, coins arrondis, ombre douce) occupe l'emplacement au-dessus de cette rangée ; la rangée précédente dépasse en haut, la rangée active est juste sous le panneau, les rangées suivantes occupent l'espace restant ; le panneau n'est pas focusable

#### Scenario: déplacement du focus
- **WHEN** le focus passe à une tuile d'une autre rangée
- **THEN** le panneau reste en place (surface fixe par-dessus la liste) et les rangées glissent derrière lui en 400 ms (easing Apple), sans saut ni apparition brusque ; à l'entrée dans la grille la position est recalée avant l'apparition

### Requirement: Contenu du panneau
Le panneau SHALL n'afficher que des visuels validés et chargés de l'app focus, et rien sinon.

#### Scenario: contenu disponible
- **WHEN** l'app focusée a des visuels validés (preview programs / watch next)
- **THEN** chaque affiche apparaît en fondu (300-400ms easing Apple) une fois chargée, puis défile lentement (Ken Burns) ; l'app du panneau ne change qu'après une courte pause du focus (~200 ms) pour ne pas clignoter en traversant la grille

#### Scenario: pas de contenu
- **WHEN** l'app focusée n'a aucun visuel validé, ou l'affiche n'est pas encore chargée
- **THEN** l'emplacement reste vide (ni cadre, ni logo de repli) ; la position des rangées ne change pas

### Requirement: Fond de la zone grille
Dans la grille, le fond SHALL être un dégradé neutre uni type tvOS, le héro étant complètement masqué.

#### Scenario: révélation de la grille
- **WHEN** l'utilisateur passe en zone grille
- **THEN** le héro disparaît (alpha 0), le fond est le dégradé neutre (pas d'aerial, pas de posters en fond de grille)

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
- **THEN** titre, app source et barre de progression (si connue) apparaissent en bas à gauche, suivis d'un bouton « Ouvrir » (« Reprendre » si progression) qui porte le focus du héro avec un état focus net ; OK sur ce bouton ouvre le contenu

### Requirement: Fond vidéo de secours
Le fallback du héro SHALL être une liste de clips nature libres de droits (Pexels, 2K sinon 1080p, mp4 H.264 en https) lus en boucle par un unique lecteur, muet.

#### Scenario: lecture
- **WHEN** le fallback est actif
- **THEN** un seul lecteur lit la playlist en boucle et muet, un clip en erreur est ignoré au profit du suivant, le lecteur est mis en pause quand la grille est active et libéré quand le héro est démonté

#### Scenario: aucun clip lisible
- **WHEN** aucun clip ne peut être lu (réseau absent, URL morte)
- **THEN** le dégradé sombre animé reste affiché, sans écran noir ni crash

### Requirement: Qualité des visuels
Le héro et le panneau Top Shelf SHALL n'afficher que des visuels de bonne qualité : vidéo d'aperçu privilégiée quand l'app en fournit une, image ou vidéo d'au moins 960 px de large, sinon le programme est écarté.

#### Scenario: image trop petite
- **WHEN** l'image décodée d'un programme fait moins de 960 px de large, ou ne peut pas être chargée
- **THEN** le héro passe au programme suivant sans afficher l'image, et le panneau Top Shelf ne l'inclut pas

#### Scenario: vidéo trop petite
- **WHEN** la vidéo d'aperçu d'un programme fait moins de 960 px de large
- **THEN** le héro replie sur le poster du programme, puis l'écarte si le poster est aussi insuffisant

### Requirement: Sélection des apps sources
Le launcher SHALL permettre, dans ses réglages (v1.x), de cocher les apps dont les programmes alimentent le héro et le Top Shelf ; par défaut toutes les apps installées sont retenues.

#### Scenario: app décochée
- **WHEN** l'utilisateur décoche une app dans les réglages du launcher (v1.x)
- **THEN** ses programmes n'apparaissent plus dans le héro ni dans le Top Shelf, sans redémarrage

### Requirement: Préchargement et mémoire
Le launcher SHALL valider et précharger les visuels du héro et du Top Shelf au chargement, et limiter sa consommation mémoire et GPU.

#### Scenario: validation au chargement
- **WHEN** les programmes sont chargés
- **THEN** chaque visuel est vérifié une fois en arrière-plan (chargement, largeur ≥ 1080 px) ; seuls les visuels validés sont proposés au héro et au Top Shelf ; les premiers visuels du héro sont gardés en mémoire, les autres en cache disque

#### Scenario: économie de ressources
- **WHEN** le héro est masqué (zone grille) ou une surface verre est invisible
- **THEN** le lecteur vidéo est libéré, les animations de fond et le flou d'arrière-plan sont arrêtés ; les images sont décodées en RGB565 avec au plus deux décodeurs simultanés
