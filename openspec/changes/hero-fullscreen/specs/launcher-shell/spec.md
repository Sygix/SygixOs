# Delta launcher-shell

## REMOVED Requirements

### Requirement: Carrousel héro
**Reason**: Le héro n'est plus un carrousel à cartes avec aperçu du voisin ; il devient un fond plein écran à défilement automatique (voir « Diaporama héro »).
**Migration**: Le scénario « clic poster » est couvert par « Clic programme publié » ; la navigation gauche/droite est reprise dans « Diaporama héro ».

## ADDED Requirements

### Requirement: Diaporama héro
Le héro SHALL enchaîner automatiquement les programmes publiés, un seul à la fois, plein écran et muet, avec des transitions à la tvOS.

#### Scenario: défilement automatique
- **WHEN** plusieurs programmes sont disponibles
- **THEN** le héro passe au programme suivant toutes les 8 s environ par fondu croisé (700-1000 ms, easing Apple), le visuel courant zoome lentement (Ken Burns), sans son, sans aperçu de l'élément suivant

#### Scenario: navigation manuelle
- **WHEN** l'utilisateur presse gauche/droite sur le héro
- **THEN** le héro passe au programme précédent/suivant avec le même fondu et le minuteur d'avance automatique repart

#### Scenario: vidéo d'aperçu
- **WHEN** le programme expose une vidéo d'aperçu (previewVideoUri)
- **THEN** elle est lue plein écran en muet à la place du poster, repli sur le poster si la lecture échoue

#### Scenario: métadonnées
- **WHEN** un programme est affiché
- **THEN** titre, app source et barre de progression (si connue) apparaissent en bas à gauche sur un dégradé de lisibilité, en fondu synchronisé avec le visuel

### Requirement: Fond vidéo de secours
Le fallback du héro SHALL être une liste de clips nature libres de droits (Pexels, 2K sinon 1080p, mp4 H.264 en https) lus en boucle par un unique lecteur, muet.

#### Scenario: lecture
- **WHEN** le fallback est actif
- **THEN** un seul lecteur lit la playlist en boucle et muet, un clip en erreur est ignoré au profit du suivant, le lecteur est mis en pause quand la grille est active et libéré quand le héro est démonté

#### Scenario: aucun clip lisible
- **WHEN** aucun clip ne peut être lu (réseau absent, URL morte)
- **THEN** le dégradé sombre animé reste affiché, sans écran noir ni crash

## MODIFIED Requirements

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

### Requirement: Grille d'apps
Le launcher SHALL auto-détecter toutes les apps TV installées et les afficher en grille.

Auto-détection de toutes les apps TV installées (category LEANBACK_LAUNCHER / LAUNCHER).

#### Scenario: affichage
- **WHEN** le home s'ouvre
- **THEN** toutes les apps TV installées apparaissent en grille 5 colonnes, tuiles 16:9 remplies par la bannière Android TV de l'app (`android:banner`), repli sur l'icône entière centrée sur fond sombre ; ordre et épinglage persistés (DataStore)

#### Scenario: menu contextuel
- **WHEN** long-press ou touche Menu sur une app
- **THEN** menu : épingler / désépingler (ouvrir + infos : v1.x)
