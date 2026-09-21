# Capability : launcher-shell

## Purpose
Écran home du launcher : grille d'apps, rangée Up Next, fond contextuel.

## Requirements

### Requirement: Grille d'apps
Le launcher SHALL auto-détecter toutes les apps TV installées et les afficher en grille.

Auto-détection de toutes les apps TV installées (category LEANBACK_LAUNCHER / LAUNCHER).

#### Scenario: affichage
- **WHEN** le home s'ouvre THEN toutes les apps TV installées apparaissent en grille (tuiles wide 1.2:1 façon tvOS, 5 colonnes, icônes/remplissage pleine tuile), ordre et épinglage persistés (DataStore)

#### Scenario: menu contextuel
- **WHEN** long-press sur une app THEN menu : épingler / désépingler (ouvrir + infos : v1.x)

### Requirement: Focus tvOS
Le launcher SHALL animer le focus des tuiles à la tvOS (zoom, ombre, easing).

#### Scenario: focus
- **WHEN** une tuile prend le focus THEN zoom ~1.1x + ombre douce + transition 250-400ms, courbes d'easing Apple ; le fond de page passe aux recommandations de l'app focus (effet Top Shelf), sinon fond neutre

### Requirement: Rangée Up Next
Le home SHALL afficher une rangée Up Next fusionnant Jellyfin puis BetaSeries.

#### Scenario: contenu
- **WHEN** des items Jellyfin en cours existent THEN rangée au-dessus de la grille : posters + barre de progression, fusion déterministe Jellyfin puis BetaSeries (v1.x), tri par date d'activité

### Requirement: Thème
L'UI SHALL être sombre (v1) avec surfaces Liquid Glass translucides.

- Sombre uniquement en v1, noir pur, posters plein cadre, surfaces Liquid Glass translucides floutées, police type Inter

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent

### Requirement: Navigation
Le launcher SHALL être navigable au DPAD uniquement.

- DPAD natif uniquement ; Home de la télécommande retourne au launcher

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent

### Requirement: Écran initial du home
Le home SHALL s'ouvrir sur un héro plein écran : poster de recommandation (Jellyfin) ou, à défaut, vidéo aérienne animée en boucle.

#### Scenario: état initial
- **WHEN** le launcher démarre
- **THEN** le héro plein écran est affiché avec le focus, le dock est visible en overlay bas semi-transparent

#### Scenario: fallback sans contenu
- **WHEN** aucune recommandation n'est disponible (Jellyfin vide/injoignable)
- **THEN** le héro joue une vidéo aérienne en boucle, sans crash

### Requirement: Carrousel héro
Le héro SHALL défiler horizontalement avec le poster suivant/précédent visible en bord de cadre.

#### Scenario: navigation
- **WHEN** l'utilisateur presse gauche/droite sur le héro
- **THEN** le carrousel passe au poster adjacent avec animation, les bords du voisin restent visibles

#### Scenario: clic poster
- **WHEN** l'utilisateur clique sur un poster
- **THEN** l'app source s'ouvre sur la page du contenu sans lecture automatique (fallback : ouverture simple de l'app)

### Requirement: Navigation 3 paliers
Le DPAD SHALL naviguer héro → dock → grille (descend) et grille → dock → héro (monte).

#### Scenario: traversée du dock
- **WHEN** l'utilisateur descend depuis le dock
- **THEN** la grille plein écran prend le focus et le dock se masque

### Requirement: Dock d'apps épinglées
Le home SHALL afficher un dock Liquid Glass (rail overlay bas semi-transparent) contenant les apps épinglées, exclues de la grille. Le dock n'est plus l'écran initial : il overlay le héro (superseded par hero-home).

#### Scenario: état initial
- **WHEN** le home s'ouvre
- **THEN** le dock est visible en overlay bas sur le héro, le focus est sur le héro

#### Scenario: tuiles adaptatives
- **WHEN** le nombre d'apps épinglées varie (4 à 8+)
- **THEN** les tuiles du dock se répartissent uniformément et réduisent leur taille automatiquement

### Requirement: Persistance de la grille
L'ordre des apps et les épinglages SHALL être persistés localement (DataStore) et restaurés au démarrage.

#### Scenario: réorganisation
- **WHEN** l'utilisateur épingle ou réordonne une app
- **THEN** l'état est persisté et conservé après redémarrage de l'app

### Requirement: Contenu héro TV Provider
Le héro SHALL afficher les programmes publiés dans le TV Provider système par les apps installées (watch next + preview programs), sans configuration utilisateur.

#### Scenario: programmes disponibles
- **WHEN** une ou plusieurs apps installées publient des programmes dans le TV Provider
- **THEN** le carrousel héro les affiche : poster (posterArtUri, fallback thumbnailUri), titre et progression visible quand disponible, ordre déterministe (reprises d'abord, puis plus récents), toutes apps confondues, sans liste d'apps codée en dur

#### Scenario: fallback
- **WHEN** aucune app ne publie de programme dans le TV Provider
- **THEN** le héro utilise la vidéo aérienne en boucle (comportement hero-home inchangé), sans crash ni écran vide

### Requirement: Clic programme publié
Le clic sur un poster du TV Provider SHALL ouvrir le contenu via l'intent publié par l'app source.

#### Scenario: ouverture
- **WHEN** l'utilisateur clique sur un poster héro issu du TV Provider
- **THEN** l'app source s'ouvre sur la fiche du contenu (intent du programme), fallback : lancement simple de l'app si l'intent échoue

#### Scenario: progression
- **WHEN** le programme expose une position de lecture (watch next)
- **THEN** une barre de progression est visible sur le poster héro
