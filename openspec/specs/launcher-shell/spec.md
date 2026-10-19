# Capability : launcher-shell

## Purpose
Écran home du launcher : grille d'apps, rangée Up Next, fond contextuel.

## Requirements

### Requirement: Grille d'apps
Le launcher SHALL auto-détecter toutes les apps TV installées et les afficher en grille, avec les sections « À suivre » et « Applications » dans l'ordre de « Position d'À suivre » de settings. Chaque section SHALL avoir un titre non focusable aligné sur sa première carte, masqué avec la section vide ; le titre « À suivre » reste en erreur. Le haut de la zone grille SHALL être le haut du titre de sa première section. Style et espacements SHALL suivre les scénarios ci-dessous.

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
- **WHEN** la zone grille est affichée avec la rangée Up Next et le réglage « Position d'À suivre » vaut « Avant les applications »
- **THEN** le titre « À suivre » est au-dessus de la rangée Up Next, puis le titre « Applications » est au-dessus de la première rangée d'apps ; avec « Après les applications », le titre « Applications » vient en premier et le titre « À suivre » est sous la dernière rangée d'apps, au-dessus de la rangée Up Next

#### Scenario: titres jamais focusables
- **WHEN** l'utilisateur passe au D-pad d'une section à l'autre (bas depuis la rangée Up Next, haut depuis la première rangée d'apps, ou l'inverse avec « Après les applications »)
- **THEN** le focus passe directement d'une ligne à l'autre sans jamais se poser sur un titre ; le titre de la section d'arrivée est entièrement visible au-dessus de sa première ligne

#### Scenario: section Up Next masquée
- **WHEN** la rangée Up Next est masquée (réglage « Afficher À suivre » désactivé, aucun contenu ou permission refusée)
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

**Détails.** — Détection des apps : Le launcher SHALL lister les activités des catégories `LEANBACK_LAUNCHER` et `LAUNCHER` installées sur la TV. — Sections de la grille : La grille SHALL afficher « À suivre » au-dessus d'Up Next et « Applications » au-dessus des apps, sur le même fond. L'ordre SHALL suivre « Position d'À suivre » de settings : avant les applications par défaut, après quand choisi. Les titres SHALL être alignés sur la première carte ou tuile (`Dimens.ScreenMarginH`) et ne jamais prendre le focus. Cas « ordre des sections » : quand la position d'À suivre change, ses cartes et son titre changent de place ensemble, selon les scénarios « titres de section » de « Grille d'apps ». — Style des titres de section : Les titres SHALL suivre « SygixOs Maquette », page TV, écrans 4.1, 7.1, 7.2 et 7.5 et composant « Titre de section » : `SygixTypography.titleLarge`, `SygixColors.OnDarkSecondary`, premier titre à `Dimens.GridTopMargin`, écart à la première ligne `Dimens.SectionTitleGap` et à la section précédente `Dimens.GridRowSpacing`. Cas « géométrie du titre » : quand une section est affichée, son titre respecte le scénario « style des titres » de « Grille d'apps » et défile avec sa section. — Visibilité des sections : Le titre SHALL défiler avec sa section. Une rangée Up Next masquée SHALL masquer « À suivre » sans place réservée. En erreur, la carte d'état remplace le contenu et le titre SHALL rester au-dessus (« États de la rangée » de up-next). Cas « erreur ou masquage » : quand Up Next devient vide, masqué ou en erreur, le titre suit respectivement les scénarios « section Up Next masquée » et « titre au-dessus de la carte d'erreur » de « Grille d'apps ».

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
Le home SHALL afficher une rangée Up Next dans la zone grille, sous le titre de section « À suivre » (« Grille d'apps »), première ligne par défaut, dernière ligne si le réglage « Position d'À suivre » vaut « Après les applications », alimentée par le TV Provider Android, tant que le réglage « Afficher À suivre » de `settings` est activé ; son contenu, son dédoublonnage, ses cartes, ses états et l'ouverture des items sont spécifiés par la capability up-next.

#### Scenario: contenu
- **WHEN** des programmes watch next sont publiés dans le TV Provider et que « Afficher À suivre » est activé
- **THEN** la rangée est affichée dans la zone grille, sous son titre « À suivre », sur le fond de la zone grille, avec ses cartes et barres de progression (« Carte Up Next » de up-next)

#### Scenario: sans contenu
- **WHEN** aucun programme n'est visible dans le TV Provider, ou que la permission est refusée
- **THEN** la rangée et son titre sont masqués et la navigation de la zone grille commence directement par les rangées d'apps

#### Scenario: réglage désactivé
- **WHEN** « Afficher À suivre » est désactivé alors que des programmes watch next sont publiés
- **THEN** ni la rangée ni son titre ne sont affichés, aucune place ne leur est réservée, la navigation de la zone grille ne passe que par les rangées d'apps ; réactiver le réglage réaffiche la rangée à la position réglée, sans redémarrage

### Requirement: Thème
L'UI SHALL rester sombre sur noir pur, avec posters plein cadre et police Figtree (SIL OFL, listée dans À propos). Le relief SHALL venir d'ombres douces et de reflets clairs, jamais d'un halo coloré. Les surfaces verre (dock, capsule, menu, bandeau) SHALL seules être floutées, avec une teinte sombre translucide, une bordure claire et des reflets ; les rayons et coins concentriques SHALL suivre le design system. Les scénarios ci-dessous SHALL préciser les valeurs et couples imbriqués.

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

**Détails.** — Matériau Liquid Glass : Les seules surfaces floutées SHALL être dock, capsule heure/réglages, menu contextuel et bandeau de déplacement. Le verre tvOS 26 SHALL avoir un flou de 28 px saturé à 170 %, teinte rgb 22, 22, 28 à 36 % pour le dock et 38 % pour la capsule, bordure blanche de 1 px à 16 %, reflet haut à 32 %, bas à 6 %, transparence sombre et ombre légère. Cas « surface verre » : quand une surface verre est visible, elle respecte « surfaces verre » et « verre sur un visuel sombre » de « Thème » ; les autres éléments ne floutent pas leur arrière-plan. — Calcul du flou : Sur une image de programme, dock et capsule SHALL utiliser une copie réduite du visuel voilé, floutée et saturée une fois, animée avec son Ken Burns/fondu sans recalcul par frame, sinon le flou direct. Sur vidéo ou dégradé, et pour menu/bandeau, le flou SHALL être direct, à partir d'une image réduite et limité à la zone de la surface. Cas « copie ou calcul direct » : quand le média ou la disponibilité de la copie change, les scénarios « coût du flou », « copie floutée indisponible » et « verre sur un visuel en mouvement » de « Thème » restent applicables. — Rayons des surfaces : Les rayons SHALL être ceux du design system, en dp pour 960 × 540 dp : tuiles grille/dock 14, dock 24, menu 26 et pilules 12, lignes de réglages 14 ; capsule et engrenage en pilule à demi-hauteur. Ombre/reflet de tuile, focus, verre précalculé, bordure et reflets SHALL suivre le rayon de leur surface. Cas « effets concentriques » : quand une surface ou un effet de focus est dessiné, le scénario « effets qui suivent le rayon » de « Thème » s'applique sans halo coloré. — Coins concentriques : Une surface arrondie imbriquée SHALL respecter rayon extérieur = rayon intérieur + plus petite marge : dock/tuile 24 = 14 + 10 ; capsule/engrenage de 36 de haut 18 = 14 + 4 ; menu/pilule 26 = 12 + 14 ; menu/vignette marge 16 et rayon 10 ; ligne/vignette 16:9 marge verticale 10,5 et rayon 3,5. Le QR SHALL avoir un fond blanc arrondi au rayon de sa marge blanche, sans rogner ses modules carrés. Cas « imbrication » : quand une surface arrondie en contient une autre, le scénario « coins concentriques » de « Thème » vérifie ces couples, en retenant la plus petite marge si les axes diffèrent.

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
Le DPAD SHALL naviguer héro → dock → grille et inversement ; seule la zone active est focusable. À suivre SHALL rester une ligne de la grille, pas un palier, sautée avec son titre quand elle est masquée. La page SHALL défiler d'un seul tenant, sans fondu ni saut, et restaurer à la grille sa position et son focus ; une carte Up Next disparue SHALL passer le focus à sa voisine. Les scénarios ci-dessous SHALL préciser les cas.

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
- **WHEN** le réglage « Position d'À suivre » vaut « Après les applications » et que la rangée est affichée
- **THEN** la rangée Up Next est la dernière ligne de la zone grille, sous son titre « À suivre » : bas depuis la dernière rangée d'apps lui donne le focus, bas depuis la rangée ne fait rien, haut depuis la rangée revient à la dernière rangée d'apps, et la première ligne de la zone grille est la première rangée d'apps, sous le titre « Applications »

#### Scenario: carte focusée disparue
- **WHEN** la carte Up Next qui a le focus disparaît de la rangée (rechargement du TV Provider, app source désactivée) et qu'il reste au moins une carte
- **THEN** la carte qui prend sa position dans la rangée (la suivante) reçoit le focus, ou la précédente si la carte disparue était la dernière ; le focus ne quitte pas la rangée et la position verticale de la page ne change pas

#### Scenario: rangée devenue vide
- **WHEN** la rangée Up Next devient vide ou masquée (dernière carte disparue, réglage « Afficher À suivre » désactivé, permission retirée) alors qu'elle a le focus, ou qu'elle porte la dernière carte visitée et que l'utilisateur revient dans la zone grille ou au launcher
- **THEN** la rangée et son titre sont masqués et le repli de « Focus d'une app disparue » s'applique à la zone grille : la première tuile de la première rangée d'apps prend le focus si la rangée était avant les applications, la dernière tuile de la dernière rangée d'apps si elle était après ; si la zone grille ne contient aucune app, le héro reprend le focus

#### Scenario: retour depuis une app
- **WHEN** l'utilisateur revient au launcher après avoir ouvert une app depuis la rangée Up Next (carte ou menu « Ouvrir avec… »)
- **THEN** la position verticale précédente dans la zone grille est restaurée ; le contenu ouvert est en première place de la rangée (« Contenus ouverts depuis SygixOs » de up-next), sa carte, retrouvée par la clé de contenu même si l'app publie désormais un autre épisode de la série, reçoit le focus et la rangée est ramenée à son début, sa première carte sur la marge gauche ; si ce contenu n'est plus publié, les scénarios « carte focusée disparue » (à partir de la position de la carte à l'ouverture) et « rangée devenue vide » s'appliquent ; le repli des apps disparues du dock et des rangées d'apps reste celui de « Focus d'une app disparue »

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

**Détails.** — Défilement de l'accueil : L'accueil SHALL être une page verticale continue : héro avec dock en overlay sur le premier écran, grille dessous. Les passages dock/grille SHALL défiler sans fondu ni saut avec durée/courbe de « Focus tvOS ». À la première entrée, la page SHALL descendre d'un écran au début de la grille, titre de sa première section en haut et première ligne dessous. Cas « première entrée ou traversée » : quand le focus passe du dock à la grille ou revient, « continuité de l'animation » et les scénarios de première descente de « Navigation 3 paliers » s'appliquent. — Restauration de la grille : Au retour dans la grille, la page SHALL retrouver sa position laissée, pas descendre d'un écran exactement, et la carte ou tuile laissée SHALL reprendre le focus. La disparition de cette carte SHALL suivre « Repli du focus des cartes ». Cas « retour dans la grille » : quand la grille redevient active après une sortie, « retour dans la grille à la position laissée » et « carte laissée disparue avant le retour » de « Navigation 3 paliers » restent applicables.

### Requirement: Dock d'apps épinglées
Le home SHALL afficher un dock en verre sombre (« Thème », rail overlay bas) contenant au plus 6 apps épinglées qui restent dans la grille. Il SHALL se déplacer avec le héro et n'être visible qu'avec lui. Le verre SHALL rester actif pendant le défilement jusqu'à la sortie complète du héro, sans changement d'aspect au début de la descente ni recréation du contenu lors de sa bascule.

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
Le héro SHALL afficher les programmes watch next et preview des apps installées, avec la seule permission système de lecture. Toute colonne absente ou vide SHALL retirer seulement l'élément concerné. Le héro et le Top Shelf SHALL ne montrer que des programmes encore publiés, relus au changement signalé et au plus tard toutes les 15 minutes au premier plan, jamais en arrière-plan ; une relecture sans changement ne republie rien.

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

#### Scenario: rafraîchissement automatique
- **WHEN** l'accueil est au premier plan et que le TV Provider signale un changement, ou que 15 minutes se sont écoulées depuis la dernière relecture
- **THEN** le héro relit le provider ; aucune relecture n'a lieu tant que le launcher est en arrière-plan, et le retour au premier plan en déclenche une

#### Scenario: programme retiré
- **WHEN** une app retire un programme publié pendant que l'accueil est affiché
- **THEN** il disparaît du héro et du Top Shelf à la relecture suivante, sans redémarrage ; s'il était affiché dans le héro, le héro passe par fondu au programme qui prend sa place, ses textes ne changeant qu'avec le visuel (« Diaporama héro ») ; ses visuels sortent des ensembles de visuels vérifiés

#### Scenario: programme modifié
- **WHEN** une app modifie le titre ou le visuel d'un programme publié
- **THEN** le héro et le Top Shelf montrent le nouveau titre et le nouveau visuel à la relecture suivante ; un nouveau visuel est vérifié comme un visuel inconnu (« Qualité des visuels »)

**Détails.** — Données facultatives du héro : Hors identifiant et package, les colonnes du TV Provider SHALL être facultatives : absentes ou vides, elles retirent seulement l'élément alimenté. Le remplissage réel du type, de la saison, de l'épisode et de la durée sur la TV de référence reste une hypothèse à vérifier sur l'appareil. Cas « métadonnées partielles » : quand une colonne facultative n'est pas publiée, « colonnes facultatives » de « Contenu héro TV Provider » s'applique sans inventer la valeur manquante. — Relecture des programmes du héro : Le héro et le Top Shelf SHALL ne montrer que des programmes encore publiés. Le launcher SHALL relire le provider sur signal de changement et au plus tard toutes les 15 minutes au premier plan, jamais en arrière-plan ; un résultat inchangé ne SHALL rien republier. Cas « modification de publication » : quand une app retire ou modifie un programme, les scénarios « rafraîchissement automatique », « programme retiré » et « programme modifié » de « Contenu héro TV Provider » s'appliquent.

### Requirement: Clic programme publié
Le clic sur un poster du TV Provider SHALL ouvrir le contenu via l'intent publié par l'app source.

#### Scenario: ouverture
- **WHEN** l'utilisateur clique sur un poster héro issu du TV Provider
- **THEN** l'app source s'ouvre sur la fiche du contenu (intent du programme), fallback : lancement simple de l'app si l'intent échoue

#### Scenario: progression
- **WHEN** le programme expose une position de lecture (watch next)
- **THEN** une barre de progression est visible sur le poster héro

### Requirement: Panneau Top Shelf au focus
Le panneau SHALL s'ouvrir à la demande au-dessus de la rangée focusée, se fermer si son app n'a rien à montrer et ne réserver aucune place fermé. Le bloc focusé (titre de section si première ligne, panneau, ligne focusée) SHALL rester entre les marges haute et basse de la grille ; la grille ne défile que pour le ramener, et jamais au-dessus du début de la zone grille. Les scénarios ci-dessous SHALL préciser le placement.

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

**Détails.** — Bloc focusé de la grille : Le bloc focusé SHALL comprendre la ligne focusée (apps ou Up Next), son titre si c'est la première ligne de sa section, et le panneau ouvert au-dessus. L'ordre SHALL rester titre, panneau, ligne selon la maquette 4.2 ; le panneau reste immédiatement au-dessus de la rangée, sous le titre. Cas « première ligne de section » : quand le panneau s'ouvre sur la première rangée d'apps, « titre de section dans le bloc » et « panneau sous le titre de section » de « Panneau Top Shelf au focus » déterminent le placement. — Placement du bloc focusé : Le bloc SHALL rester entre les marges haute de 40 dp et basse de 32 dp (espacement des rangées). La grille SHALL défiler seulement pour le ramener entre elles, avec durée/courbe de l'expansion du panneau. En grille active, elle ne SHALL jamais remonter avant le haut de son premier titre : le héro reste hors écran. Cas « bloc déplacé ou trop grand » : quand le focus ou le panneau change de rangée, ou que le bloc dépasse la hauteur disponible, les scénarios de placement de « Panneau Top Shelf au focus » restent applicables, sans défilement supplémentaire.

### Requirement: Contenu du panneau
Le panneau SHALL n'afficher que des visuels validés et chargés de l'app focus, et ne pas s'ouvrir sinon.

#### Scenario: contenu disponible
- **WHEN** l'app focusée a des visuels validés (preview programs / watch next)
- **THEN** chaque affiche apparaît en fondu (300-400ms easing Apple) une fois chargée, puis défile lentement une seule fois (Ken Burns, un seul passage) et reste ensuite immobile sur sa dernière position ; le défilement repart du début à chaque nouvelle affiche

#### Scenario: pas de contenu
- **WHEN** l'app focusée n'a aucun visuel validé
- **THEN** aucun panneau n'est ouvert (ni cadre, ni logo de repli) et la grille occupe tout l'écran

#### Scenario: affiche retirée
- **WHEN** le panneau est ouvert et que son app retire le programme d'une affiche affichée ou en rotation
- **THEN** l'affiche quitte le panneau à la relecture suivante ; le panneau se referme s'il ne lui reste aucune affiche validée

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
- **THEN** sa lecture est interrompue et ses animations arrêtées conformément à « Préchargement et mémoire » (scénario « économie de ressources ») et « Fond vidéo de secours » ; son lecteur vidéo n'est pas libéré

### Requirement: Diaporama héro
Le héro SHALL enchaîner automatiquement les programmes publiés, un seul à la fois, plein écran et muet, avec des transitions à la tvOS et un bouton d'ouverture focusable. En-tête, ligne d'infos, progression et bouton SHALL toujours correspondre au visuel affiché et changer avec lui. Si le visuel suivant n'est pas prêt après 1 s, le héro SHALL passer au suivant utilisable sans changer les textes ; les scénarios ci-dessous SHALL préciser les textes, médias et échecs.

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

**Détails.** — En-tête du héro : L'en-tête SHALL précéder le titre selon la maquette 960 × 540 dp : icône carrée sans masque de 18 dp, coins 4,5 dp, puis libellé par type publié. Pour l'app X : CONTINUE (`WATCH_NEXT_TYPE_CONTINUE`) « Continuer dans X », NEXT « Épisode suivant dans X », NEW « Nouveau dans X », WATCHLIST « À regarder dans X », preview ou type absent/inconnu « X ». Il SHALL ne montrer que les éléments disponibles. Cas « données manquantes de l'en-tête » : quand le nom ou l'icône manque, le type seul ou le libellé sans icône est montré, et sans les deux l'en-tête disparaît ; les scénarios de types et de nom/icône manquants de « Diaporama héro » s'appliquent. — Infos et progression du héro : La ligne sous le titre SHALL réunir seulement saison, épisode et durée publiés, séparés par « · ». La durée SHALL être arrondie à la minute (minimum 1 min), en heures au-delà d'une heure (« 1 h 35 min », « 2 h »). Barre et « Reste X min » SHALL exister seulement avec position et durée connues ; le restant est arrondi à la minute supérieure avec la même écriture des heures. Cas « infos partielles » : quand les données du programme sont partielles, « infos partielles », « progression sans position ou sans durée » et « programme en cours » de « Diaporama héro » s'appliquent sans réserver d'espace aux lignes absentes. — Synchronisation du héro : Titre, en-tête, infos et bouton SHALL correspondre au visuel affiché ; « Ouvrir »/« Reprendre » SHALL ouvrir ce programme et changer avec son visuel, jamais avant. Les textes SHALL rester fixes sans recomposition ni dessin pendant le Ken Burns, sans espace pour les lignes absentes, et lisibles sur visuel clair grâce aux voiles. Cas « changement et animation » : quand le visuel change ou zoome, « changement de programme », « textes avec le visuel », « textes immobiles pendant le Ken Burns » et « lisibilité sur un poster clair » de « Diaporama héro » s'appliquent. — Visuel du héro en attente : Si l'image ou la première image vidéo suivante n'est pas prête 1 s après une avance automatique ou gauche/droite, le héro SHALL passer au prochain visuel utilisable sans changer ses textes avant. Le visuel lent SHALL continuer sans nouvelle requête ; la vidéo reste préparée en pause jusqu'à la demande d'une autre vidéo. Le programme redevient éligible prêt. La toute première vidéo sans visuel à remplacer est exemptée. Cas « préparation lente » : quand le visuel suivant n'est pas prêt dans la seconde, le scénario « visuel lent » de « Diaporama héro » s'applique ; une image préparée n'est pas retenue en bitmap par le héro et est relue depuis le cache à son retour.

### Requirement: Fond vidéo de secours
Le fallback du héro SHALL être une liste de clips nature libres de droits (Pexels, 2K sinon 1080p, mp4 H.264 en https) lus en boucle par un unique lecteur, muet.

#### Scenario: lecture
- **WHEN** le fallback est actif
- **THEN** un seul lecteur lit la playlist en boucle et muet, un clip en erreur est ignoré au profit du suivant, le lecteur est mis en pause quand la grille est active et libéré quand le héro est démonté

#### Scenario: aucun clip lisible
- **WHEN** aucun clip ne peut être lu (réseau absent, URL morte)
- **THEN** le dégradé sombre animé reste affiché, sans écran noir ni crash

### Requirement: Qualité des visuels
Le héro et le panneau Top Shelf SHALL n'afficher que des visuels de bonne qualité : vidéo d'aperçu privilégiée quand l'app en fournit une, image paysage d'au moins 640 px de large ou vidéo d'au moins 1080 px de large, sinon le programme est écarté. La largeur d'une image SHALL être mesurée une fois l'image ramenée dans 1920 × 1080 sans agrandissement, et une image plus haute que large SHALL être écartée.

#### Scenario: image trop petite
- **WHEN** l'image d'un programme, ramenée dans 1920 × 1080 sans agrandissement, fait moins de 640 px de large, est plus haute que large, ou ne peut pas être chargée
- **THEN** le héro passe au programme suivant sans afficher l'image, et le panneau Top Shelf ne l'inclut pas

#### Scenario: variante YouTube
- **WHEN** l'image d'un programme est une miniature YouTube (`i.ytimg.com`, `iN.ytimg.com` ou `img.youtube.com`, chemin `/vi/<id>/` ou `/vi_webp/<id>/`)
- **THEN** la variante `https://i.ytimg.com/vi/<id>/maxresdefault.jpg` est vérifiée d'abord ; acceptée, c'est elle qu'affichent le héro et le panneau Top Shelf ; absente ou trop petite, l'image publiée est vérifiée selon la règle générale ; un échec réseau sur l'une ou l'autre laisse le programme à revérifier

#### Scenario: vidéo trop petite
- **WHEN** la vidéo d'aperçu d'un programme fait moins de 1080 px de large
- **THEN** le héro replie sur le poster du programme, puis l'écarte si le poster est aussi insuffisant

### Requirement: Sélection des apps sources
Le launcher SHALL permettre, dans ses réglages, de cocher les apps dont les programmes alimentent le héro, le Top Shelf et la rangée Up Next ; par défaut toutes les apps installées sont retenues. L'UI de ce réglage est spécifiée par « Apps sources » de la capability settings.

#### Scenario: app décochée
- **WHEN** l'utilisateur décoche une app dans les réglages du launcher
- **THEN** ses programmes n'apparaissent plus dans le héro, le Top Shelf ni la rangée Up Next, sans redémarrage

### Requirement: Préchargement et mémoire
Le launcher SHALL borner la validation des visuels (premiers du héro au chargement, affiches d'une app au focus posé), décoder à la taille d'affichage et garder le héro fluide au repos comme au focus du dock (au moins 55 images/s, moins de 10 % en retard, APK release). La grille SHALL borner sa composition à 12 rangées, puis à la fenêtre visible/cible plus 2 rangées de prélecture, avec une seule autorité de défilement vertical. Les scénarios ci-dessous SHALL préciser les seuils.

#### Scenario: validation au chargement
- **WHEN** les programmes sont chargés
- **THEN** seuls les premiers visuels du héro sont vérifiés en arrière-plan (chargement, largeur selon « Qualité des visuels ») d'après les dimensions lues dans l'en-tête de l'image, sans décoder ses pixels ; les visuels vérifiés restent en cache disque, aucun n'est préchargé en mémoire ; le provider pouvant publier des centaines de programmes, aucun autre visuel n'est vérifié à ce moment

#### Scenario: validation à la demande
- **WHEN** le focus reste au moins 0,5 s sur une tuile de la grille
- **THEN** quelques affiches candidates de cette app sont vérifiées, chacune une seule fois tant qu'elle n'est pas injoignable (scénario « visuel injoignable ») ; seules les affiches validées alimentent son panneau ; un focus qui ne fait que passer sur une tuile ne lance aucune vérification

#### Scenario: validation interrompue ou nouvelles affiches
- **WHEN** la vérification des affiches d'une app a été interrompue (focus posé sur une autre app avant la fin), ou les programmes rechargés proposent d'autres affiches candidates pour cette app
- **THEN** au prochain focus prolongé sur l'app, les affiches candidates pas encore vérifiées le sont ; celles déjà vérifiées ne sont pas redemandées

#### Scenario: visuel injoignable
- **WHEN** la vérification d'un visuel du héro ou d'une affiche du panneau Top Shelf échoue pour une raison réseau (erreur d'entrée-sortie, erreur HTTP 5xx)
- **THEN** le visuel n'est pas affiché ; il est vérifié de nouveau à la prochaine relecture du héro (retour au premier plan, changement signalé par le TV Provider ou relecture périodique, selon « Contenu héro TV Provider ») s'il fait partie des premiers visuels du héro, ou au prochain focus prolongé sur son app pour une affiche du panneau Top Shelf

#### Scenario: visuel rejeté
- **WHEN** un visuel est trop étroit, refusé par le serveur (erreur HTTP 4xx) ou impossible à décoder
- **THEN** il n'est plus vérifié ni demandé pendant la session tant que son programme reste publié, même après une relecture du héro

#### Scenario: économie de ressources
- **WHEN** le héro est masqué à l'intérieur du launcher (zone grille, page de réglages) ou une surface verre est invisible
- **THEN** la lecture de la vidéo est interrompue (arrêtée, ou mise en pause à sa position quand une exigence le prévoit) sans que le lecteur vidéo soit libéré, le launcher étant au premier plan ; les animations de fond, le Ken Burns du visuel du héro et le flou d'arrière-plan sont arrêtés ; les images sont décodées avec au plus deux décodeurs simultanés

#### Scenario: launcher en arrière-plan
- **WHEN** une autre app passe au premier plan et le launcher n'est plus visible
- **THEN** le lecteur vidéo du héro est libéré, ce qui rend le décodeur vidéo à l'app au premier plan ; le défilement automatique du diaporama et les délais du héro (visuel lent, démarrage d'une vidéo) sont suspendus

#### Scenario: retour au premier plan après libération
- **WHEN** le launcher revient au premier plan après avoir libéré le lecteur vidéo
- **THEN** la vidéo d'aperçu du programme affiché est rechargée et reprend, sans compter comme un échec ni faire passer le héro au programme suivant ; le défilement automatique et les délais du héro repartent de zéro ; une vidéo lente dont la préparation a été interrompue (héro masqué ou launcher en arrière-plan) redevient éligible au diaporama

#### Scenario: écran au repos
- **WHEN** aucune touche n'est pressée et aucune transition n'est en cours
- **THEN** seules les animations spécifiées encore visibles redessinent l'écran (vidéo d'aperçu, passage unique du dégradé animé du repli, passage unique du Ken Burns d'un nouveau visuel du héro ou du panneau Top Shelf, changement de visuel du défilement automatique) ; une fois ces passages terminés, l'image est immobile ; aucune animation d'un élément hors écran ne tourne, et une animation continue ne provoque aucune recomposition à chaque image

#### Scenario: taille de décodage
- **WHEN** un visuel du héro, une affiche du panneau Top Shelf ou une bannière d'app est décodé, même publié en 3840 × 2160
- **THEN** le visuel du héro est décodé à la taille de la fenêtre, au plus 1920 × 1080, l'affiche à la taille du panneau (en le couvrant), et la bannière à la taille de la tuile de la grille au zoom de focus, en pixels de l'appareil, au plus 480 × 270

#### Scenario: une passe par image
- **WHEN** le héro affiche le visuel d'un programme, au repos, pendant le Ken Burns ou un fondu
- **THEN** les voiles du bas et de la gauche sont intégrés au visuel décodé et le voile du coin haut droit est dessiné seul sur sa petite zone ; ni fond de fenêtre, ni fond noir, ni dégradé du repli, ni voile n'est dessiné sous un visuel opaque, et aucun fondu ne passe par un calque plein écran hors écran

#### Scenario: écran de démarrage
- **WHEN** l'écran de démarrage est affiché
- **THEN** seuls le catalogue et le premier visuel utilisable du héro sont préparés en priorité ; la vérification des visuels suivants du héro attend la fin de l'écran de démarrage, les bannières sont décodées sur un fil de basse priorité, et la grille, hors écran, n'est composée qu'après le fondu vers l'accueil

#### Scenario: navigation dans la grille
- **WHEN** l'utilisateur déplace le focus de tuile en tuile et de rangée en rangée dans la grille
- **THEN** seules les tuiles qui prennent ou perdent le focus et les rangées qui les contiennent sont recomposées, l'animation du focus (zoom, ombre, reflet) ne recompose rien, et la vérification d'un visuel d'une autre app ne recompose aucune rangée

#### Scenario: grande grille
- **WHEN** le catalogue compte plus de 12 rangées
- **THEN** le nombre de tuiles composées dépend de la hauteur de l'écran et de la prélecture, pas du nombre d'apps ; les bannières des rangées composées sont lues dans le cache borné des visuels d'apps ou décodées sur le fil de basse priorité, celles des rangées hors fenêtre ne sont ni composées ni décodées

#### Scenario: rangée visée hors fenêtre
- **WHEN** le focus vise une rangée non composée : rafale de touches plus rapide que le défilement, retour dans la grille sur la rangée laissée (héro, menu contextuel, réglages, retour d'une app), app déplacée d'une rangée hors fenêtre, rangée Up Next placée après les apps
- **THEN** la rangée visée est composée dès que l'ancre la désigne, avant la demande de focus, et la tuile ou la carte reçoit le focus par son identité à la place prévue par « Panneau Top Shelf au focus » ; un retour rapide en arrière retrouve la rangée précédente encore composée

#### Scenario: panneau sur une rangée profonde
- **WHEN** le panneau Top Shelf s'ouvre sur une rangée d'une grande grille
- **THEN** le bloc focusé est placé comme sur une petite grille ; pendant le délai d'ouverture, la première affiche validée est décodée à la taille du panneau avec la requête de l'affichage, pour que l'expansion montre une image prête ; le panneau s'ouvre avec les affiches déjà validées et ne met sa liste à jour qu'une fois la vérification de l'app terminée, sans changer l'affiche en cours ; seules la rangée dont le panneau est ouvert et celle dont il se ferme portent son animation

#### Scenario: fraîcheur du cache disque
- **WHEN** un visuel du héro ou une affiche du panneau est demandé alors que son entrée du cache disque date de plus d'un jour
- **THEN** l'image est relue depuis le réseau et remplace l'entrée du cache disque et les images en mémoire issues de cette URL ; si le réseau échoue, la version en cache est affichée ; une entrée plus récente est servie sans requête réseau

**Détails.** — Reprise des visuels du héro : Une vidéo en échec ou une image définitivement rejetée (trop étroite, HTTP 4xx, indécodable) ne SHALL pas être redemandée pendant la session. Une image injoignable par erreur d'entrée-sortie ou HTTP 5xx SHALL être revérifiée selon « Préchargement et mémoire », scénario « visuel injoignable ». Cas « classement de l'échec » : quand un visuel échoue, les scénarios « visuel injoignable » et « visuel rejeté » de « Préchargement et mémoire » déterminent la possibilité de reprise. — Fluidité de release : Sur la TV de référence et l'APK release, le héro au repos/au focus du dock SHALL atteindre au moins 55 images/s et moins de 10 % d'images en retard. La navigation grille/panneau SHALL avoir moins de 10 % de retard : seules les tuiles et la rangée concernées sont recomposées, sans décodage ni validation avant le focus posé. Cas « navigation et repos » : quand les scénarios « navigation dans la grille » et « écran au repos » de « Préchargement et mémoire » sont mesurés, ces seuils s'appliquent sans animations hors écran ni recomposition par frame. — Fenêtre de composition de la grille : Jusqu'à 12 rangées, Up Next comprise, toutes SHALL être composées ; au-delà, seulement celles couvrant l'écran aux positions courante et cible, plus 2 rangées de prélecture de chaque côté, les autres remplacées par leur hauteur. La page SHALL garder une seule autorité verticale ; grille, verre, ambiance, placement, navigation, persistance et repli du focus conservent leurs règles. Cas « grande grille et rangée visée » : quand la grille dépasse 12 rangées, « grande grille », « rangée visée hors fenêtre » et « panneau sur une rangée profonde » de « Préchargement et mémoire » s'appliquent, la grille restant source du verre et du dégradé d'ambiance.

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

**Détails.** — Repli du focus des cartes : Si la carte focusée ou dernière visitée disparaît et que d'autres restent, le focus SHALL aller à celle qui prend sa position, ou à la précédente si c'était la dernière. Une rangée vide ou masquée SHALL appliquer « Focus d'une app disparue » à la grille entière, Up Next comprise. Cas « disparition d'une carte ou rangée » : quand le contenu change pendant l'affichage ou avant le retour, « carte focusée disparue », « carte laissée disparue avant le retour » et « rangée devenue vide » de « Navigation 3 paliers » déterminent le focus.

### Requirement: Écran de démarrage
À la première création de l'accueil du processus, y compris en arrière-plan ou après mise à jour, le launcher SHALL afficher plein écran la mascotte seule, centrée sur le noir, avec un court fondu. L'accueil SHALL être prêt quand catalogue et premier visuel le sont (attente plafonnée à 2 s) ; l'écran SHALL durer au moins 600 ms et au plus 5 s, puis laisser place par fondu sans saut ni flash. Composition, touches et sortie SHALL suivre les scénarios ci-dessous ; Retour suit le système.

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

**Détails.** — Préparation du démarrage : Le catalogue chargé et le premier visuel du héro prêt SHALL rendre l'accueil prêt : image chargée ou première image vidéo programme/nature rendue, pas le dégradé animé. L'attente du visuel SHALL être plafonnée à 2 s depuis la première image de démarrage. La mascotte SHALL être décodée dès le début du processus, hors fil principal. Cas « attente du premier média » : quand le premier visuel tarde ou que l'animation est illisible, « attente du premier visuel », « visuel trop lent », « animation illisible » et « mascotte décodée tard » de « Écran de démarrage » s'appliquent. — Durées du démarrage : Le démarrage SHALL durer au moins 600 ms depuis la première image montrant la mascotte, ou sa propre première image si elle est illisible ; il attend l'accueil prêt, au plus 5 s depuis sa première image. Dès minimum atteint et accueil prêt, ou à 5 s, le fondu SHALL révéler l'accueil ; au plafond ses états de chargement/erreur prennent le relais. Cas « minimum et plafonds » : quand l'accueil est prêt tôt ou reste incomplet, « accueil prêt avant la durée minimale » et « plafond global » de « Écran de démarrage » déterminent la sortie, même sans animation. — Composition avant révélation : L'accueil SHALL être composé après le fondu d'entrée de la mascotte, dès fenêtre active ou au plus tard 400 ms après ce fondu sinon, ou au début du fondu de sortie. Avant révélation, il SHALL être dessiné sous le démarrage opaque, invisible et sans animation, sans retarder mascotte ni touches : image sans fondu, vidéo préparée en pause, Ken Burns/lecture au fondu de sortie, grille hors écran composée seulement à sa fin. Cas « fenêtre active ou surcouche » : quand une permission ou autre surcouche prive la fenêtre de focus, « composition de l'accueil » et « fenêtre sans focus » de « Écran de démarrage » gardent ces bornes et un accueil déjà dessiné pour le fondu. — Sortie du démarrage : Le fondu de sortie SHALL utiliser durée/courbe de « Focus tvOS », sans noir, saut, flash ni calque plein écran hors écran. Pendant tout le démarrage, fondu compris, D-pad/OK ne SHALL pas atteindre l'accueil ; Retour SHALL suivre Android, quitter le premier plan et ne déclencher aucune action de l'accueil. Cas « touches et transition » : quand l'utilisateur agit pendant le démarrage ou son fondu, « touches pendant l'écran de démarrage », « Retour pendant le fondu de sortie » et « fluidité de l'écran de démarrage » restent applicables. — Retour sans écran de démarrage : Un accueil déjà créé dans le processus ne SHALL jamais remontrer le démarrage au retour Home, d'une autre app ou au premier plan. Recréé hors démarrage à froid avec catalogue non chargé, il SHALL montrer seulement un fond noir uni, sans mascotte ni animation, jusqu'à l'accueil. Cas « retour ou recréation » : quand le launcher revient sans nouveau processus, « retour sur le launcher » et « accueil recréé hors démarrage à froid » de « Écran de démarrage » s'appliquent.

### Requirement: Animation de la mascotte
La mascotte SHALL flotter et cligner dans une animation pré-rendue de 2 s / 120 images, à 60 images/s en boucle sans raccord visible, décodée par le système sans lecteur vidéo. Centrée à 180 dp, native 360 px en 1080p, elle SHALL ne jamais être agrandie au-delà, avec pixels extérieurs transparents sans cadre ni écart de fond. La première image SHALL être la pose fixe de repos, yeux ouverts au centre du flottement, utilisée sans animation.

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
Sur Android 12+, l'écran de lancement système SHALL avoir le même noir que le démarrage, sans icône ni image. Du lancement à l'accueil, seuls ce noir puis le démarrage SHALL apparaître, sans double écran, changement de fond, icône système ou double mascotte. La disparition de l'écran système SHALL être invisible, sans fondu ni zoom d'une couleur ou icône différente.

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
À chaque démarrage à froid, le launcher SHALL lire l'échelle de durée système, commune à « Supprimer les animations » et aux options développeur. À 0, il SHALL montrer la première image fixe de la mascotte, sans flottement, clignement ni fondu d'entrée/sortie, en conservant les durées du démarrage (600 ms au moins, plafonds de 2 s et de 5 s). Une échelle propre aux apps forcée par le constructeur ne SHALL pas désactiver l'animation.

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
Le design system SHALL fournir une pastille unique : point plein de 6 dp bleu `#0A84FF`, au coin haut droit de l'élément ou en fin de ligne de liste. Non focusable et sans animation continue, elle SHALL suivre défilement/zoom sans changer taille, position, focus ni navigation de l'élément. Toute fonctionnalité, dont self-update, SHALL utiliser ce composant sans en redéfinir l'aspect.

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
L'APK release SHALL embarquer le profil de référence de l'app et de ses bibliothèques. Le build SHALL produire depuis cet APK `app-release.dm`, contenant seulement `primary.prof` et `primary.profm`, publié à côté de `app-release.apk` avec SHA-256 GitHub. Installé avec l'APK, il SHALL permettre la compilation du démarrage dès l'installation sans attendre le fond ; l'APK reste installable seul.

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

### Requirement: Démarrage à l'allumage
La ligne « Ouvrir SygixOs à l'allumage » de « Par autorisations » SHALL être désactivée par défaut, activable par l'utilisateur ou la configuration automatique de secours. OK SHALL basculer et persister aussitôt sans perdre le focus. Option activée, l'ouverture SHALL être demandée une seule fois par démarrage à froid, par la fin du démarrage ou le service d'accessibilité connecté moins de 5 minutes après, sans sortie de veille. Les scénarios ci-dessous SHALL préciser les états.

#### Scenario: ouverture au démarrage observée
- **WHEN** l'appareil démarre, que l'option est activée et que la demande d'ouverture aboutit
- **THEN** l'accueil de SygixOs s'affiche au premier plan et l'ouverture est enregistrée comme observée pour ce démarrage

#### Scenario: diffusion bloquée, service connecté
- **WHEN** l'appareil démarre, que l'option est activée, que la fin du démarrage n'est pas transmise à SygixOs mais que le service d'accessibilité activé se connecte moins de 5 minutes après le démarrage
- **THEN** le service ouvre l'accueil de SygixOs et l'ouverture est enregistrée comme observée pour ce démarrage

#### Scenario: deux chemins dans le même démarrage
- **WHEN** la fin du démarrage est reçue et que le service d'accessibilité se connecte peu après, option activée
- **THEN** SygixOs s'ouvre une seule fois : le chemin qui arrive après l'ouverture observée ne demande rien, et une seconde demande arrivée avant elle n'a aucun effet visible

#### Scenario: connexion tardive du service
- **WHEN** le service d'accessibilité se connecte plus de 5 minutes après le démarrage, option activée
- **THEN** aucune ouverture au démarrage n'est demandée

#### Scenario: option désactivée
- **WHEN** l'appareil démarre et que l'option est désactivée
- **THEN** SygixOs ne demande pas son ouverture, ni par le récepteur ni par le service, et n'enregistre aucun échec

#### Scenario: fin du démarrage non reçue
- **WHEN** l'option était activée avant le démarrage, que le système ou le constructeur ne transmet pas la fin du démarrage à SygixOs et que le service d'accessibilité ne se connecte pas non plus
- **THEN** aucune ouverture n'est observée pour ce démarrage ; au lancement suivant de SygixOs, l'état « démarrage automatique non observé » est affiché par la ligne

#### Scenario: ouverture refusée par le système
- **WHEN** l'option était activée avant le démarrage, qu'un chemin d'ouverture est déclenché mais qu'Android refuse l'ouverture de l'activité
- **THEN** aucune ouverture n'est observée par ce chemin, aucun crash n'a lieu, l'autre chemin reste possible, et au lancement suivant l'état « démarrage automatique non observé » est affiché si aucune ouverture n'a été observée

#### Scenario: option activée après le démarrage
- **WHEN** l'option est activée, par l'utilisateur ou par la configuration automatique, alors que l'appareil a déjà démarré
- **THEN** le démarrage courant n'est pas compté comme un échec ; seul le prochain démarrage est évalué

#### Scenario: option activée par la configuration automatique
- **WHEN** la configuration automatique pose les réglages de secours
- **THEN** l'option est persistée activée et la ligne « Ouvrir SygixOs à l'allumage » la montre activée ; l'utilisateur peut la désactiver comme une option qu'il a activée

**Détails.** — Ligne de démarrage à l'allumage : La ligne SHALL afficher « Fait » si l'option est activée et « Autoriser l'ouverture au démarrage » absente, « À faire » sinon ; détail « SygixOs demande à s'ouvrir au démarrage de la TV ». L'ouverture observée SHALL être mémorisée par démarrage ; non observée, le détail devient « SygixOs ne s'est pas ouvert automatiquement à ce démarrage ». Cas « ouverture observée ou non » : quand l'utilisateur consulte la page après le démarrage, les scénarios de « Démarrage à l'allumage » déterminent l'état, sans assimiler une activation tardive à une tentative déjà due. — Ouverture unique au démarrage : Option activée, SygixOs SHALL demander l'accueil par les mécanismes Android autorisés seulement : fin du démarrage système et connexion de son service activé moins de 5 minutes après le démarrage (valeur à valider sur TV). Le premier chemin observé SHALL ouvrir une seule fois ; l'autre et une connexion tardive ne font rien. Option désactivée, aucune demande. Cas « deux chemins et refus du système » : quand les deux chemins arrivent, tardent ou sont bloqués, les scénarios « deux chemins dans le même démarrage », « connexion tardive du service » et « ouverture refusée par le système » de « Démarrage à l'allumage » s'appliquent sans contournement Android.

### Requirement: Permission d'affichage superposé pour le démarrage
La permission de superposition SHALL être proposée par la ligne « Autoriser l'ouverture au démarrage », sans confirmation, quand l'option est activée et l'écran système présent. Seule la configuration automatique peut l'accorder par le débogage ; sinon OK ouvre Android. Son état SHALL être relu au retour, jamais supposé accordé ; un refus ne bloque pas SygixOs. Sans écran système, la ligne devient « Indisponible » et l'option ne s'active pas. L'accord ne garantit pas le démarrage.

#### Scenario: sous-étape affichée à l'activation
- **WHEN** l'utilisateur active l'option alors que la permission manque et que son écran existe
- **THEN** l'option est persistée activée, la ligne « Autoriser l'ouverture au démarrage » apparaît avec « À faire », aucun écran système ne s'ouvre avant OK sur elle et le focus reste sur « Ouvrir SygixOs à l'allumage »

#### Scenario: ouverture de l'écran de la permission
- **WHEN** l'utilisateur presse OK sur « Autoriser l'ouverture au démarrage »
- **THEN** l'écran système de la permission s'ouvre et la ligne passe à « En attente… » ; au retour, elle disparaît seulement si le système déclare la permission accordée, et le focus va à « Ouvrir SygixOs à l'allumage » dans ce cas, sinon reste sur elle

#### Scenario: permission refusée
- **WHEN** l'utilisateur revient de l'écran système sans accorder la permission
- **THEN** SygixOs reste utilisable sans crash, ne suppose aucune permission accordée, conserve l'option activée et la ligne affiche « À faire »

#### Scenario: permission déjà accordée
- **WHEN** l'option est activée alors que la permission est déjà accordée
- **THEN** la ligne de la permission n'est pas affichée et « Ouvrir SygixOs à l'allumage » est « Fait »

#### Scenario: écran de la permission absent
- **WHEN** l'appareil n'a pas d'écran système pour la permission et que l'option de démarrage est désactivée
- **THEN** « Ouvrir SygixOs à l'allumage » est affichée « Indisponible », avec la raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage » ; OK ne l'active pas, aucune ligne de permission ni écran système n'apparaît et la ligne garde le focus

#### Scenario: écran de la permission impossible à ouvrir
- **WHEN** l'utilisateur valide « Autoriser l'ouverture au démarrage » et que l'écran système ne peut pas s'ouvrir
- **THEN** la ligne disparaît, l'option revient à désactivé et « Ouvrir SygixOs à l'allumage » devient « Indisponible » avec sa raison, focalisée ; une nouvelle activation est refusée jusqu'au prochain lancement, sauf si la permission est ensuite déclarée accordée

#### Scenario: permission accordée par la configuration automatique
- **WHEN** la configuration automatique accorde la permission comme réglage de secours
- **THEN** la permission est relue auprès du système, et la ligne « Superposer aux autres applications » de « SygixOs garde la main » (9.12) n'est « Fait » que si le système la déclare accordée

**Détails.** — Ligne d'autorisation de démarrage : « Autoriser l'ouverture au démarrage » SHALL apparaître sous l'option activée si la permission manque et l'écran existe, détail « Android l'exige : choisissez SygixOs dans la liste, puis autorisez-le. ». État « À faire » sans accord, « En attente… » dans Android, masquée après accord système. OK SHALL ouvrir la permission de SygixOs ; son libellé exact reste celui relevé par la tâche 1.5 de P7. Cas « permission et retour » : quand l'utilisateur ouvre ou quitte l'écran système, les scénarios « ouverture de l'écran de la permission », « permission refusée » et « permission déjà accordée » de « Permission d'affichage superposé pour le démarrage » s'appliquent. — Superposition indisponible : Si l'écran de permission est absent ou impossible à ouvrir, l'option SHALL devenir « Indisponible », raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage », sans ligne de permission ni crash. L'activation SHALL être refusée ou annulée si juste faite. L'état dure jusqu'au prochain processus sauf accord système ultérieur ; une option déjà activée reste désactivable. Cas « écran absent ou inaccessible » : quand l'ouverture de la permission échoue, « écran de la permission absent » et « écran de la permission impossible à ouvrir » de « Permission d'affichage superposé pour le démarrage » vérifient l'option sans bloquer le launcher.

### Requirement: Retour Home par le service d'accessibilité
Le service SHALL ramener l'accueil sur Home après activation par l'utilisateur (ligne « Ouvrir SygixOs avec la touche Home » ouvrant Accessibilité) ou par la configuration automatique de secours, autres services conservés. Hors ces cas SygixOs ne SHALL pas l'activer ; après refus, reproposition seulement dans Réglages. Connecté, il SHALL consommer tous les événements Home et ouvrir au relâchement non annulé ; non connecté, rien n'est consommé. Les états SHALL suivre les scénarios ci-dessous.

#### Scenario: activation par l'utilisateur
- **WHEN** l'utilisateur valide « Ouvrir SygixOs avec la touche Home »
- **THEN** les réglages d'accessibilité du système s'ouvrent ; au retour au premier plan, la ligne affiche « Fait » seulement si le système déclare le service activé et qu'il est connecté

#### Scenario: activation par la configuration automatique
- **WHEN** la configuration automatique active le service comme réglage de secours
- **THEN** le service est ajouté à la liste des services activés sans retirer les autres, l'état est relu, et la ligne « Touche Home » de « SygixOs garde la main » (9.12) n'est « Fait » que si le service est connecté

#### Scenario: service refusé
- **WHEN** l'utilisateur revient des réglages d'accessibilité sans avoir activé le service, ou ferme le parcours sans l'ouvrir
- **THEN** la ligne affiche « À faire », SygixOs reste utilisable et aucune nouvelle proposition ne s'affiche automatiquement, à ce lancement comme aux suivants

#### Scenario: touche Home, SygixOs en arrière-plan
- **WHEN** le service est connecté, que SygixOs est en arrière-plan et que l'utilisateur presse Home
- **THEN** le service consomme l'appui et le relâchement, l'accueil du constructeur ne s'affiche à aucun moment, et SygixOs revient au premier plan sur son accueil dès le relâchement, selon les règles de retour existantes (« Écran de démarrage », scénario « retour sur le launcher »)

#### Scenario: touche Home, SygixOs au premier plan
- **WHEN** le service est connecté, que SygixOs est au premier plan et que l'utilisateur presse Home
- **THEN** la touche est consommée et SygixOs applique « Touche Home avec SygixOs au premier plan »

#### Scenario: appui long sur Home
- **WHEN** le service est connecté et que l'utilisateur maintient la touche Home (répétitions ou maintien au-delà du délai d'appui long)
- **THEN** tous les événements sont consommés, aucun comportement système d'appui long ne s'applique, et SygixOs s'ouvre au relâchement comme pour un appui court

#### Scenario: relâchement annulé
- **WHEN** le système annule l'appui sur Home en cours (relâchement marqué annulé)
- **THEN** l'événement est consommé et SygixOs n'est pas ouvert

#### Scenario: signal Home absent
- **WHEN** l'appareil ne transmet aucune touche Home au service, même connecté
- **THEN** le service reste livré, ne lit aucun contenu de fenêtre, ne demande aucune ouverture et laisse le comportement système inchangé

#### Scenario: service désactivé ou arrêté
- **WHEN** l'utilisateur désactive le service dans les réglages système, ou que le système l'arrête
- **THEN** aucune touche n'est consommée, l'appui sur Home suit le comportement du système, aucune erreur n'est affichée, et au retour au premier plan la ligne affiche « À faire » (désactivé) ou suit le scénario « service bloqué » (activé mais arrêté)

#### Scenario: service non lié au lancement
- **WHEN** après un redémarrage complet, le gestionnaire d'auto-démarrage du constructeur a empêché la liaison du service activé, et que l'utilisateur lance SygixOs
- **THEN** la ligne affiche « En attente… » pendant l'attente bornée, puis « Bloqué » si le service ne s'est pas connecté ; SygixOs ne demande aucune nouvelle liaison et ne modifie ni ses composants, ni le service, ni les réglages du système

#### Scenario: service bloqué
- **WHEN** le service est activé dans les réglages du système mais n'est toujours pas connecté au terme de l'attente bornée
- **THEN** la ligne affiche « Bloqué » avec son explication, reste focalisable, et OK ouvre les réglages d'accessibilité du système ; la voie par autorisations est « Bloquée » ; quand le système relie de nouveau le service, la ligne affiche « Fait » au retour au premier plan

**Détails.** — Filtrage de Home : Le service SHALL observer seulement Home par `AccessibilityService.onKeyEvent`, sans lire les fenêtres ni rien conserver. Connecté, il SHALL consommer appui, répétitions et relâchement pour les soustraire au système/launcher constructeur, et ouvrir aussitôt au relâchement non annulé, appui long comme court. Inactif/arrêté, il ne consomme rien. Il SHALL être livré même sans signal Home sur l'appareil, sans action alors. Cas « événements de touche » : quand Home est reçu ou absent, les scénarios de touches et de signal absent de « Retour Home par le service d'accessibilité » s'appliquent ; SygixOs reste utilisable sans le service. — État du service Home : Activé mais non connecté au lancement ou au retour au premier plan, le service SHALL être attendu pendant un délai borné sans tentative de liaison par SygixOs. La ligne SHALL refléter activation, connexion, attente et disponibilité des réglages, avec les libellés ci-dessous ; « Bloqué » dure jusqu'à connexion ou désactivation. Cas « présentation de la ligne du service » : quand l'état est relu, activé/connecté affiche « Fait », non activé « À faire », détail « OK : ouvre le réglage Android correspondant » ; réglages ouverts ou attente non conclue affiche « En attente… » ; délai écoulé activé/non connecté affiche « Bloqué », détail « La TV a bloqué ce service au démarrage. Désactivez-le puis réactivez-le dans Accessibilité, ou utilisez la configuration automatique. » ; réglages impossibles à ouvrir affiche « Indisponible », raison « Les réglages d'accessibilité ne peuvent pas s'ouvrir sur cette TV ».

### Requirement: Touche Home avec SygixOs au premier plan
Home reçu au premier plan, directement ou par le service (premier plan à l'appui), SHALL fermer tous les réglages d'un coup, liste « Position d'À suivre » comprise, puis les surcouches comme Retour, parcours compris ; page en haut, héro focusé et lecture reprise. L'allumage ne SHALL pas compter comme Home. Hors premier plan, « retour sur le launcher » de « Écran de démarrage » reste inchangé.

#### Scenario: depuis la grille
- **WHEN** le focus est dans la zone grille et que la touche Home atteint SygixOs
- **THEN** la page défile jusqu'au héro comme au scénario « retour depuis la grille » de « Navigation 3 paliers », le héro prend le focus et sa lecture reprend

#### Scenario: depuis les réglages
- **WHEN** la page de réglages est ouverte, quelle que soit la catégorie ou la ligne focalisée, et que la touche Home atteint SygixOs
- **THEN** les réglages se ferment en une fois, la page de l'accueil est en haut, le héro a le focus et sa lecture reprend

#### Scenario: depuis la liste déroulante « Position d'À suivre »
- **WHEN** la liste déroulante « Position d'À suivre » est ouverte dans les réglages et que la touche Home atteint SygixOs
- **THEN** la liste se ferme sans changer la valeur et les réglages se ferment dans le même temps, sans étape intermédiaire sur la ligne « Position d'À suivre » ; la page de l'accueil est en haut, le héro a le focus et sa lecture reprend ; à la réouverture des réglages, la liste est fermée et la valeur persistée inchangée

#### Scenario: depuis une surcouche
- **WHEN** le menu contextuel d'une tuile, le mode « Déplacer » ou le parcours guidé est affiché et que la touche Home atteint SygixOs
- **THEN** la surcouche se ferme comme par Retour (déplacement annulé ; parcours enregistré comme fermé s'il avait été affiché automatiquement), les réglages éventuellement ouverts dessous se ferment aussi, la page de l'accueil est en haut et le héro a le focus

#### Scenario: déjà sur le héro
- **WHEN** le héro a déjà le focus, sans surcouche, et que la touche Home atteint SygixOs
- **THEN** le héro garde le focus et rien d'autre ne change

#### Scenario: pendant l'écran de démarrage
- **WHEN** la touche Home atteint SygixOs pendant l'écran de démarrage
- **THEN** l'écran de démarrage se poursuit sans changement et le héro prend le focus à son fondu de sortie (« Écran de démarrage »)

### Requirement: Parcours de l'écran d'accueil
Le parcours « Faire de SygixOs l'écran d'accueil » SHALL s'afficher une fois tant que jamais fermé, installations mises à jour comprises (drapeau conservé depuis v0.1.0-rc.2), après démarrage et réponse à « Programmes TV », jamais sur un dialogue système. Sa première page SHALL proposer les choix disponibles (Tsuyu, automatique, autorisations) avec leur état, l'aide « Retour : plus tard » et le bouton « Plus tard ». Choix, navigation, étapes et avancement SHALL suivre les scénarios ci-dessous.

#### Scenario: premier lancement
- **WHEN** SygixOs se lance, que le parcours n'a jamais été fermé, que Tsuyu n'est pas disponible et que rien n'est encore fait
- **THEN** à la fin de l'écran de démarrage, la première page s'affiche sur l'accueil, sans ligne Tsuyu, avec « Configuration automatique (recommandée) » à l'état « À configurer » et « Par autorisations (sans débogage) » à l'état « À faire », et « Configuration automatique (recommandée) » a le focus

#### Scenario: premier lancement avec Tsuyu disponible
- **WHEN** SygixOs se lance, que le parcours n'a jamais été fermé, que Tsuyu est disponible et que rien n'est encore fait
- **THEN** le panneau 9.1 s'affiche avec, de haut en bas, « Automatique avec Tsuyu (recommandée) » et « Automatique sans Tsuyu » à « À configurer », « Par autorisations (sans débogage) » à « À faire », puis « Plus tard » ; « Automatique avec Tsuyu (recommandée) » a le focus

#### Scenario: voie automatique indisponible
- **WHEN** la voie automatique est « Indisponible sur cette TV » à l'ouverture du parcours, que Tsuyu soit disponible ou non
- **THEN** le panneau 8.2 s'affiche, sans ligne Tsuyu, « Par autorisations (sans débogage) » a le focus, et OK sur « Configuration automatique » ne fait rien

#### Scenario: navigation
- **WHEN** l'utilisateur presse bas quatre fois, haut quatre fois, puis gauche et droite sur 9.1
- **THEN** le focus passe d'une ligne à la suivante jusqu'à « Plus tard », y reste, remonte jusqu'à la première ligne et y reste ; gauche et droite ne le déplacent pas et aucun élément de l'accueil ne prend le focus

#### Scenario: plus tard
- **WHEN** l'utilisateur presse OK sur « Plus tard » ou Retour sur la première page affichée automatiquement
- **THEN** le parcours se ferme, il est enregistré comme fermé, l'accueil reprend avec le focus sur le héro, aucune connexion privilégiée n'a eu lieu, rien n'est téléchargé et aucun écran système n'est ouvert

#### Scenario: repère d'avancement
- **WHEN** l'utilisateur choisit « Automatique avec Tsuyu (recommandée) » alors que le débogage est désactivé et que Tsuyu n'est pas installé
- **THEN** l'écran du débogage affiche « Étape 1 sur 5 », puis « Installer Tsuyu » « Étape 2 sur 5 », « Démarrer Tsuyu » « Étape 3 sur 5 », « Autoriser SygixOs » « Étape 4 sur 5 » et « Configuration en cours » « Étape 5 sur 5 »

#### Scenario: étapes déjà faites
- **WHEN** l'utilisateur choisit « Automatique sans Tsuyu » alors que le débogage est déjà activé
- **THEN** l'écran « Autoriser SygixOs » s'affiche directement avec « Étape 1 sur 2 »

#### Scenario: passage sans Tsuyu
- **WHEN** l'utilisateur presse « Sans Tsuyu » sur « Installer Tsuyu » affiché « Étape 2 sur 5 »
- **THEN** l'écran « Autoriser SygixOs » (8.4) s'affiche avec « Étape 2 sur 3 »

#### Scenario: tout par OK
- **WHEN** sur la TV de référence, rien n'étant fait et Tsuyu disponible, l'utilisateur presse uniquement OK sur chaque écran de SygixOs et suit les écrans d'Android et de Tsuyu qu'il annonce
- **THEN** la configuration se termine par « SygixOs garde la main » avec Tsuyu, l'accueil Google TV n'est pas désactivé et aucune confirmation de désactivation ne s'est affichée

#### Scenario: demande de permission au premier lancement
- **WHEN** la demande de permission « Programmes TV » est affichée à la fin de l'écran de démarrage
- **THEN** le parcours ne s'affiche pas tant que ce dialogue est à l'écran ; il s'affiche dès que l'utilisateur a accordé ou refusé la permission

#### Scenario: pas de nouvel affichage automatique
- **WHEN** SygixOs se lance de nouveau après la fermeture du parcours affiché automatiquement, y compris après un redémarrage de l'appareil
- **THEN** le parcours ne s'affiche pas ; Réglages › Écran d'accueil reste le point d'entrée

#### Scenario: processus arrêté pendant un écran système
- **WHEN** le système arrête le processus de SygixOs pendant qu'un écran système ou Tsuyu, ouvert depuis le parcours affiché automatiquement, est affiché, puis SygixOs se relance
- **THEN** le parcours, jamais fermé, s'affiche de nouveau sur sa première page avec les états relus

**Détails.** — Choix du parcours : La première page SHALL présenter les choix et leurs états à droite, avec Tsuyu seulement s'il est disponible et la voie automatique non indisponible. « Active » SHALL identifier seulement la façon active (avec Tsuyu s'il est actif, sinon sans), l'autre « Non utilisée ». La voie par autorisations SHALL montrer son état propre ; la voie indisponible suit 8.2 quel que soit Tsuyu. Cas « première page et aide » : quand la première page s'affiche, le titre est « Faire de SygixOs l'écran d'accueil », la description « Choisissez comment SygixOs remplace l'accueil Google TV. Vous pourrez changer d'avis dans Réglages › Écran d'accueil. », avec aide « Retour : plus tard » et bouton « Plus tard ». Cas « trois choix avec Tsuyu » : quand Tsuyu est disponible et la voie utilisable (9.1), les lignes sont « Automatique avec Tsuyu (recommandée) » / « Un guide pas à pas installe Tsuyu : SygixOs se règle seul et se répare si la TV défait ses réglages. », « Automatique sans Tsuyu » / « Activez le débogage une fois : SygixOs fait les réglages, sans réparation ensuite. », puis « Par autorisations (sans débogage) » / « Quelques autorisations à donner. Limite : sur certaines TV, l'accueil Google TV peut reprendre la main après un redémarrage complet. ». Cas « deux choix sans Tsuyu » : quand la page 9.1 ne propose pas Tsuyu, « Configuration automatique (recommandée) » / « Activez le débogage une fois : SygixOs fait les réglages lui-même. » précède la même ligne par autorisations. Cas « façon automatique indisponible » : quand la voie est « Indisponible sur cette TV » (8.2), la ligne « Configuration automatique » affiche « SygixOs n'arrive pas à utiliser le débogage sur cette TV : utilisez la configuration par autorisations. », focusable sans effet sur OK, même si Tsuyu est disponible. — Navigation du parcours : Le focus SHALL rester dans le panneau, initialement première ligne non indisponible, puis étape conseillée pour chaque écran. Haut/bas SHALL parcourir choix puis « Plus tard », sans boucle ; gauche/droite sans effet. Des OK successifs suivant Android/Tsuyu SHALL terminer sans désactiver l'accueil d'origine. Retour ne SHALL jamais modifier la TV et suit l'aide affichée. Cas « précédent ou fermeture » : quand Retour est pressé, « Retour : précédent » rend l'écran précédent et le focus à l'élément ouvrant ; « Retour : plus tard » et « Retour : fermer » ferment ; « Plus tard », « Terminé », « Fermer » et Home ferment aussi ; affiché automatiquement, le parcours ne revient plus automatiquement et le héro reprend le focus ; tout reste accessible dans Réglages › Écran d'accueil. — Étapes du parcours : OK sur le choix SHALL ouvrir le premier écran encore nécessaire dans l'ordre de sa façon, les étapes faites sautées. Avec Tsuyu : débogage 9.2/9.3, installation 9.4, démarrage 9.7, autorisation 9.8, réglages 9.10 ; sans : débogage 9.2/9.3, autorisation 8.4, réglages 9.10. La voie par autorisations SHALL ouvrir 8.13. Cas « premier écran nécessaire » : quand un choix est validé, « étapes déjà faites » et « tout par OK » de « Parcours de l'écran d'accueil » s'appliquent dans l'ordre « Activer le débogage », « Installer Tsuyu », « Démarrer Tsuyu », « Autoriser SygixOs », « Configuration en cours » selon la façon. — Avancement du parcours : Les écrans automatiques SHALL montrer au-dessus du titre « Étape X sur N » : étapes restantes à l'ouverture parmi débogage, installation/démarrage Tsuyu seulement avec lui, autorisation, réglages toujours ; X est le rang courant. L'erreur SHALL garder son repère. N ne SHALL être recalculé que pour « Sans Tsuyu » en 9.4/9.6. Première page, autorisations, détails, dialogues et panneau n'ont pas ce repère. Cas « repère et changement de façon » : quand une étape est affichée ou « Sans Tsuyu » choisi, « repère d'avancement » et « passage sans Tsuyu » de « Parcours de l'écran d'accueil » s'appliquent.

### Requirement: Accès par le débogage
Les opérations privilégiées SHALL passer par Tsuyu « Actif » (tsuyu-access), sinon par l'ADB local, après action explicite, délais bornés. La connexion SHALL viser la boucle locale, utiliser une clé propre à SygixOs et ne jamais la transmettre ; elle SHALL être fermée à la fin de chaque séquence. Les états distinguent débogage désactivé, connexion impossible, autorisation en attente/refusée et prêt ; le port ADB SHALL être testé avant Tsuyu. Aucun échec ne SHALL rendre SygixOs inutilisable.

#### Scenario: clé déjà autorisée
- **WHEN** la clé de SygixOs a déjà été autorisée avec « Toujours autoriser sur cet ordinateur », que le débogage est activé et que Tsuyu n'est pas « Actif »
- **THEN** OK sur la configuration automatique sans Tsuyu ouvre directement « Configuration en cours », sans fenêtre système

#### Scenario: Tsuyu actif
- **WHEN** Tsuyu est « Actif » et qu'une séquence démarre, depuis le parcours ou le panneau
- **THEN** aucune connexion ADB de SygixOs n'est ouverte et les opérations passent par Tsuyu

#### Scenario: port testé avant Tsuyu
- **WHEN** l'utilisateur revient du guide du débogage avec le débogage activé sur le parcours avec Tsuyu
- **THEN** SygixOs teste le port ADB réseau sans fenêtre système ; joignable, le parcours passe à l'étape Tsuyu suivante ; injoignable, rien n'est téléchargé

#### Scenario: port injoignable
- **WHEN** le débogage est activé mais que la connexion au port ADB réseau est refusée ou n'aboutit pas dans le délai
- **THEN** aucune opération n'est exécutée, la voie automatique devient « Indisponible sur cette TV » et le parcours revient au panneau 8.2

#### Scenario: débogage perdu pendant une séquence
- **WHEN** le débogage est désactivé ou la connexion perdue pendant une séquence
- **THEN** la séquence s'arrête, l'état du système est relu, l'écran « Configuration interrompue » (8.12) montre ce qui est fait et l'étape en échec, sans succès supposé

**Détails.** — États de l'accès privilégié : L'accès SHALL distinguer débogage désactivé (lu sans connexion), connexion impossible, autorisation en attente/refusée et prêt (clé acceptée et commande test shell réussie, ou Tsuyu actif). Connexion, attente et chaque commande SHALL être bornées selon p7-default-launcher D1 ; un délai dépassé ne SHALL donner ni prêt ni succès. Cas « perte d'accès » : quand le débogage disparaît ou un délai expire, « débogage perdu pendant une séquence » de « Accès par le débogage » s'applique sans succès ni crash supposé. — Port du débogage avant Tsuyu : Avec Tsuyu, débogage activé et avant téléchargement, une simple ouverture au bouclage SHALL tester le port ADB réseau, sans authentification ni fenêtre système, avec le délai de connexion. Aucun port joignable SHALL rendre la voie « Indisponible sur cette TV » jusqu'à une tentative réussie. Cas « port testé avant téléchargement » : quand la façon avec Tsuyu est choisie, « port testé avant Tsuyu » et « port injoignable » de « Accès par le débogage » s'appliquent.

### Requirement: Écrans du débogage
Débogage désactivé, la configuration SHALL commencer par « Activer le débogage » (deux étapes relues, bouton vers À propos puis options développeur). Activé sans Tsuyu, elle SHALL montrer « Autoriser SygixOs » avec « Toujours autoriser », puis « Continuer ». Refus, expiration et désactivation SHALL afficher l'erreur et revenir au parcours. Gauche/droite naviguent sans boucle, haut/bas sans effet ; Retour revient à l'écran précédent. Les scénarios ci-dessous SHALL préciser libellés et chemins.

#### Scenario: activer le débogage
- **WHEN** l'utilisateur ouvre la configuration automatique alors que les options pour les développeurs sont masquées et le débogage désactivé
- **THEN** l'écran 9.2 s'affiche avec les deux étapes à « À faire » et « Ouvrir À propos » focalisé ; OK ouvre l'écran système « À propos »

#### Scenario: options affichées au retour
- **WHEN** l'utilisateur a affiché les options pour les développeurs depuis « À propos » puis revient dans SygixOs sans avoir activé le débogage
- **THEN** l'écran 9.3 s'affiche avec l'étape 1 à « Fait », l'étape 2 à « À faire » et « Ouvrir les options pour les développeurs » focalisé

#### Scenario: retour avec le débogage activé
- **WHEN** l'utilisateur active le débogage puis revient dans SygixOs, sans Tsuyu
- **THEN** l'écran 8.4 s'affiche avec la ligne « Débogage » « Activé » et « Continuer » focalisé ; aucune fenêtre système n'est déclenchée avant OK sur « Continuer »

#### Scenario: autorisation acceptée
- **WHEN** l'utilisateur presse « Continuer » puis accepte la fenêtre système
- **THEN** la configuration démarre et l'écran « Configuration en cours » s'affiche

#### Scenario: autorisation refusée
- **WHEN** l'utilisateur refuse ou ferme la fenêtre système, ou que le délai d'autorisation expire
- **THEN** l'écran 8.11 s'affiche, rien n'est modifié, « Réessayer » a le focus et relance la demande

#### Scenario: débogage désactivé entre-temps
- **WHEN** le débogage a été désactivé entre l'écran 8.4 et la demande
- **THEN** l'écran 8.10 s'affiche, rien n'est modifié et « Ouvrir les options pour les développeurs » a le focus

#### Scenario: écrans système absents
- **WHEN** ni l'écran demandé ni son écran de repli ne peuvent s'ouvrir
- **THEN** le bouton ne change rien, sans crash, et les étapes restent affichées

**Détails.** — Guide du débogage : Le guide SHALL relire les états à l'ouverture et au retour, présenter deux étapes numérotées non focusables, « À faire »/« Fait » avec pastille verte, aide « Retour : précédent » et un seul bouton focusé. Avant options développeur activées : 9.2 « Ouvrir À propos » (sinon Paramètres) ; après : 9.3 « Ouvrir les options pour les développeurs » (sinon À propos). Au retour débogage activé, il SHALL avancer sans action. Cas « contenu des deux étapes » : quand « Activer le débogage » est affiché, la description est « Une option de la TV que SygixOs utilise pour se régler lui-même. Deux réglages à changer, puis revenez ici. » ; étape 1 « Affichez les options pour les développeurs », détail « Dans À propos, appuyez 7 fois sur la ligne du build (son nom varie selon la TV), puis revenez avec Retour. », faite quand le système confirme les options ; étape 2 « Activez le débogage USB », détail « Dans Options pour les développeurs, activez « Débogage USB », puis revenez avec Retour. », faite quand le débogage est activé. — Autorisation ADB guidée : Sans Tsuyu, l'étape 8.4 SHALL demander « Autoriser SygixOs », avec aide « Retour : précédent » et « Continuer » focusé déclenchant la demande. La ligne « Débogage » SHALL être « Activé », détail « Détecté au retour dans SygixOs » ; l'acceptation démarre la configuration. Cas « explications de l'autorisation » : quand 8.4 s'affiche, la description est « Le débogage est activé. Android va afficher la fenêtre « Autoriser le débogage USB ? » : SygixOs en a besoin pour se configurer. » ; étapes 1 « Cochez « Toujours autoriser sur cet ordinateur » » / « Sinon, Android vous le redemandera à chaque configuration. », 2 « Choisissez « Autoriser » » / « La configuration démarre aussitôt. ». — Erreurs du débogage : Refus ou expiration d'autorisation SHALL afficher 8.11, même titre/description que 8.4, ligne Débogage, boutons « Retour » et « Réessayer » focusé relançant la demande. Débogage désactivé entre-temps SHALL afficher 8.10, titre/description du guide, étapes relues, boutons « Retour » et « Ouvrir les options pour les développeurs » focusé. Cas « erreur d'autorisation » : quand l'autorisation est refusée ou expire, l'encadré « Autorisation refusée » dit « Android n'a pas autorisé SygixOs et rien n'a été modifié. Réessayez, cochez « Toujours autoriser sur cet ordinateur » puis choisissez « Autoriser ». ». Cas « débogage désactivé » : quand le débogage a été désactivé, l'encadré « Le débogage est désactivé » dit « Rien n'a été modifié. Activez « Débogage USB » dans les options pour les développeurs, puis revenez dans SygixOs. ».

### Requirement: Configuration automatique de l'écran d'accueil
La configuration automatique SHALL enchaîner selon p7-default-launcher D5 : autorisation constructeur, relevé de l'écran d'accueil précédent, définition de SygixOs comme HOME, puis vérification. « Configuration en cours » SHALL relire chaque étape (fait/en cours/à venir) et SHALL survivre à la fermeture du panneau jusqu'à la fin ou au délai ; rouvert, il affiche l'état relu. Succès, secours, échec et annulation SHALL suivre les scénarios ci-dessous.

#### Scenario: configuration sans désactivation
- **WHEN** l'accès est prêt et que SygixOs devient l'écran d'accueil effectif après la définition de l'écran d'accueil
- **THEN** « Configuration en cours » montre les étapes passer à « Fait » une à une, puis « SygixOs est votre écran d'accueil » s'affiche avec « Terminé » focalisé, sans confirmation ni désactivation, et la voie automatique est « Active »

#### Scenario: désactivation proposée
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif après la vérification et que l'accueil d'origine peut être désactivé
- **THEN** l'écran 9.11 s'affiche avec « Garder l'accueil Google TV (conseillé) » focalisé ; aucun composant n'est modifié

#### Scenario: ne pas désactiver
- **WHEN** l'utilisateur presse OK sur « Garder l'accueil Google TV (conseillé) » ou Retour sur 9.11
- **THEN** aucun composant n'est désactivé, le service d'accessibilité est ajouté sans retirer les autres, la superposition est accordée, l'option de démarrage est activée, chaque réglage est relu, et « SygixOs garde la main » s'affiche avec « Terminé » focalisé

#### Scenario: désactivation demandée
- **WHEN** l'utilisateur presse bas puis OK sur 9.11
- **THEN** la confirmation 8.8 s'affiche avec « Annuler » focalisé

#### Scenario: désactivation impossible
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif et qu'aucun composant de l'accueil d'origine n'est actif et modifiable, ou que les garde-fous l'interdisent
- **THEN** 9.11 n'est pas affiché et les réglages de secours sont posés, puis « SygixOs garde la main » s'affiche

#### Scenario: résultat avec Tsuyu
- **WHEN** la séquence s'est exécutée par Tsuyu et que les réglages de secours sont posés
- **THEN** l'écran 9.12 affiche la ligne « Réparation automatique » à « Active », le « Bon à savoir » propre à Tsuyu et aucun conseil de désactiver le débogage

#### Scenario: échec d'une étape
- **WHEN** la définition de l'écran d'accueil échoue (erreur, délai ou relecture qui ne la confirme pas)
- **THEN** 8.12 s'affiche avec « Démarrage automatique » « Fait », « Écran d'accueil » « Échec », « Vérification » « Annulée », l'encadré « L'écran d'accueil n'a pas pu être réglé » et « Réessayer » focalisé ; les étapes déjà faites restent en place

#### Scenario: panneau fermé pendant la configuration
- **WHEN** l'utilisateur presse Retour ou la touche Home pendant « Configuration en cours »
- **THEN** le panneau se ferme, la séquence continue jusqu'à sa fin ou son délai, et à la réouverture l'écran correspondant à l'état relu s'affiche

**Détails.** — Progression de la configuration : 9.10 SHALL afficher « Configuration en cours », étapes relues une à une, sans bouton, aide « Patientez… ». Les états SHALL être « Fait » vert, « En cours… » bleu ou « À venir ». Les résultats/échecs SHALL porter le repère « Réglages » ; leurs écrans à deux boutons naviguent gauche/droite sans boucle, haut/bas sans effet. Cas « contenu de la progression » : quand la séquence de configuration s'exécute, la description est « SygixOs règle la TV. Ne l'éteignez pas pendant la configuration. » ; les lignes sont « Démarrage automatique » / « La TV laisse SygixOs démarrer avec elle » seulement si cette autorisation existe, « Écran d'accueil » / « SygixOs devient l'écran d'accueil d'Android », « Vérification » / « SygixOs vérifie que la TV l'ouvre comme écran d'accueil ». — Résultat de configuration effectif : Accueil effectif vérifié, le résultat SHALL afficher « SygixOs est votre écran d'accueil », les étapes faites et le conseil sans Tsuyu ou la réparation active avec lui. Aide « Retour : fermer », boutons « Détails » et « Terminé » focusé SHALL suivre « Progression de la configuration ». Cas « succès et lignes » : quand SygixOs est devenu l'accueil effectif, la description est « SygixOs a vérifié que la TV l'ouvre maintenant comme écran d'accueil. » ; « Démarrage automatique » / « La TV laisse SygixOs démarrer avec elle » et « Écran d'accueil » / « La TV ouvre SygixOs comme écran d'accueil » sont « Fait » ; sans Tsuyu, « Conseil : désactivez le débogage » dit « SygixOs n'en a plus besoin. Réactivez-le pour relancer la configuration ou rétablir l'accueil Google TV. » tant qu'activé ; avec Tsuyu actif (9.13), « Réparation automatique » est « Active », détail « Tsuyu permet à SygixOs de refaire ses réglages si la TV les défait. Gardez le débogage activé. ». — Dernier choix de configuration : Si l'accueil d'origine reste actif/modifiable et les garde-fous le permettent, 9.11 SHALL proposer « Un dernier choix », deux lignes sans état, garder conseillé focusé puis désactiver ouvrant 8.8, aide « Retour : garder ». Haut/bas SHALL passer entre elles sans boucle, gauche/droite sans effet. Cas « contenu du choix » : quand 9.11 est affiché, la description est « Sur cette TV, l'accueil Google TV reste l'écran d'accueil du système. SygixOs peut le garder ou le remplacer complètement. Vous pourrez changer d'avis dans Réglages › Écran d'accueil. » ; « Garder l'accueil Google TV (conseillé) » dit « La touche Home et l'allumage ouvrent SygixOs. Toutes les touches de la télécommande continuent de marcher. » ; « Désactiver l'accueil Google TV » dit « SygixOs le remplace complètement. Selon la TV, des touches de la télécommande ne marchent plus. ». — Réglages de secours automatiques : Garder ou Retour en 9.11, désactivation impossible ou échouée/annulée SHALL activer le service d'accessibilité en conservant les autres, accorder la superposition et activer « Démarrage à l'allumage », chacun relu, puis montrer « SygixOs garde la main ». Aucun succès SHALL être supposé. Cas « résultat de secours » : quand les réglages de secours ont été relus, « SygixOs garde la main » décrit « L'accueil Google TV reste actif : SygixOs a réglé la touche Home et l'ouverture au démarrage. » ; « Touche Home » / « Le service de SygixOs ramène à SygixOs », « Superposer aux autres applications » / « Permet à SygixOs de s'ouvrir au démarrage de la TV », « Ouverture à l'allumage » / « SygixOs demande à s'ouvrir au démarrage de la TV » montrent leurs états ; aide « Retour : fermer », boutons « Détails » et « Terminé » focusé. Cas « indications de réparation du secours » : quand ce résultat s'affiche avec ou sans Tsuyu actif, avec Tsuyu (9.12), « Réparation automatique » est « Active », détail « Tsuyu permet à SygixOs de refaire ces réglages si la TV les défait. Gardez le débogage activé. », puis « Bon à savoir » / « Si la touche Home ne ramène plus à SygixOs après un redémarrage, ouvrez SygixOs une fois : il refait ses réglages. » ; sans Tsuyu, « Bon à savoir » dit « Sur certaines TV, après un redémarrage complet, l'accueil Google TV peut reprendre la main ; SygixOs vous le signale. ». — Échec de configuration et état automatique : Une étape en échec SHALL arrêter les dépendantes et afficher 8.12, sans réessai sans action. La voie automatique SHALL être « Active » si accueil effectif ou secours en place, « Indisponible sur cette TV » selon l'accès, sinon « À configurer », sur première page et panneau Launcher. Cas « écran interrompu » : quand une étape de configuration échoue, « Configuration interrompue » dit « Une étape n'a pas abouti. Ce qui est fait reste en place ; vous pouvez réessayer. » ; erreur propre à l'étape, pour HOME « L'écran d'accueil n'a pas pu être réglé » / « La touche Home ouvre encore l'accueil Google TV. Vérifiez que le débogage est toujours activé, puis réessayez. » ; étapes « Fait », « Échec » rouge ou « Annulée » / « Non effectuée » ; aide « Retour : fermer », boutons « Fermer » et « Réessayer » focusé.

### Requirement: Page Par autorisations
La page 8.13 SHALL afficher les lignes dans l'ordre suivant : touche Home, démarrage à l'allumage, superposition si requise, démarrage automatique si le constructeur en déclare un, puis « Bon à savoir ». OK SHALL ouvrir le réglage Android correspondant sans activation implicite ; au retour, états relus et focus sur la ligne ouvrante. Chaque ligne SHALL montrer à droite son état réel, jamais une réussite supposée. Navigation et bouton « Terminé » SHALL suivre les scénarios ci-dessous.

#### Scenario: ouverture de la page
- **WHEN** l'utilisateur presse OK sur « Par autorisations (sans débogage) » sur une TV sans gestionnaire d'auto-démarrage du constructeur, rien n'étant fait
- **THEN** la page 8.13 s'affiche avec « Ouvrir SygixOs avec la touche Home » et « Ouvrir SygixOs à l'allumage » à « À faire », sans « Autoriser le démarrage automatique », puis « Bon à savoir » et « Terminé » ; la première ligne a le focus

#### Scenario: retour d'un réglage Android
- **WHEN** l'utilisateur a ouvert un réglage Android depuis une ligne puis revient dans SygixOs
- **THEN** le panneau 8.13 est toujours affiché, l'état de la ligne est relu (plus jamais « En attente… » d'un écran fermé) et la ligne a le focus

#### Scenario: écran du constructeur
- **WHEN** l'appareil a un gestionnaire d'auto-démarrage du constructeur et déclare un écran retenu
- **THEN** « Autoriser le démarrage automatique » est affichée après « Ouvrir SygixOs à l'allumage » et OK ouvre cet écran

#### Scenario: réglage du constructeur indisponible
- **WHEN** l'appareil a un gestionnaire d'auto-démarrage du constructeur sans écran retenu, comme la TV de référence
- **THEN** « Autoriser le démarrage automatique » est affichée « Indisponible » avec la raison « Pas de réglage sur cette TV : seule la configuration automatique peut l'autoriser », reste focalisable, et OK n'ouvre rien

**Détails.** — Lignes de la page Par autorisations : 8.13 SHALL afficher Home, allumage, permission de démarrage conditionnelle puis autorisation constructeur si détectée (D8). Sans action, la page ne SHALL rien activer. Après les lignes, « Bon à savoir » non focusable, aide « Retour : fermer » et « Terminé » SHALL être présents. Cas « texte de la page » : quand « Par autorisations » s'affiche, sa description est « Pour chaque ligne, OK ouvre le réglage Android à changer. Activez SygixOs, puis revenez ici : l'état se met à jour tout seul. » ; les lignes sont « Ouvrir SygixOs avec la touche Home », « Ouvrir SygixOs à l'allumage », « Autoriser l'ouverture au démarrage » si option activée et superposition manquante, et « Autoriser le démarrage automatique » si gestionnaire détecté ; « Bon à savoir » dit « Sur certaines TV, après un redémarrage complet, l'accueil Google TV peut reprendre la main ; SygixOs vous le signale. ». Cas « ligne du constructeur » : quand le gestionnaire de démarrage constructeur est détecté, avec écran retenu, le détail est « OK : ouvre l'écran de démarrage automatique de la TV », « À faire » jusqu'à une ouverture observée après sa consultation puis « Fait » ; sans écran, « Indisponible » explique « Pas de réglage sur cette TV : seule la configuration automatique peut l'autoriser ». — Navigation des autorisations : Focus initial SHALL être première ligne non « Fait », sinon « Terminé ». Haut/bas SHALL parcourir les lignes focusables puis Terminé sans boucle ; gauche/droite sans effet. États à droite : « À faire », « En attente… » dans Android/vérification, « Fait », « Bloqué » pour Home seulement, « Indisponible » avec raison à la place du détail, focusable et OK sans effet. Cas « déplacement et retour système » : quand la page est parcourue ou Android est fermé, « ouverture de la page » et « retour d'un réglage Android » de « Page Par autorisations » gardent le panneau et restaurent la ligne ouvrante. — État de la voie par autorisations : Sur la première page et le panneau Launcher, la voie SHALL être « Active » si toutes les lignes affichées/disponibles sont « Fait », « Bloquée » si Home est « Bloqué », « En partie » si au moins une est faite, « Non utilisée » si aucune faite et voie automatique active, sinon « À faire ». Cas « synthèse de la voie » : quand les états des autorisations sont relus, la première page et le panneau affichent cette synthèse sans modifier les réglages ni contourner un refus.

### Requirement: Panneau Launcher principal
« Configurer SygixOs comme launcher principal » SHALL ouvrir « Launcher principal » (9.14/9.15/8.21) au-dessus des réglages, avec les lignes Automatique, Tsuyu et Par autorisations (état et détail), puis Relancer, Remplacer et Rétablir selon les conditions. Focus initial : relancer ; haut/bas voies puis actions sans boucle, gauche/droite sans effet. Retour/fermeture SHALL rendre le focus à la ligne des réglages sans changer le drapeau du parcours automatique.

#### Scenario: ouverture depuis les réglages
- **WHEN** l'accueil d'origine a été désactivé par la configuration automatique sans Tsuyu, que Tsuyu n'est pas disponible et que l'utilisateur presse OK sur la ligne des réglages
- **THEN** le panneau s'affiche, sans ligne Tsuyu, avec « Configuration automatique » « Active », « Par autorisations » « Non utilisée », « Relancer la configuration » focalisé et « Rétablir l'accueil Google TV » affiché

#### Scenario: configuré avec Tsuyu
- **WHEN** la configuration automatique a été faite par Tsuyu en gardant l'accueil Google TV et que Tsuyu est « Actif »
- **THEN** le panneau 9.14 affiche « Configuration automatique » « Active » (« Accueil Google TV gardé · touche Home et allumage réglés »), « Tsuyu » « Actif », « Par autorisations » « Non utilisée », « Relancer la configuration » (« Refait les réglages automatiques ») focalisé et « Remplacer complètement l'accueil Google TV », sans « Rétablir l'accueil Google TV »

#### Scenario: Tsuyu à installer
- **WHEN** la configuration automatique a été faite sans Tsuyu en gardant l'accueil Google TV et qu'une release de Tsuyu est connue
- **THEN** le panneau 9.15 affiche « Tsuyu » « À installer » (« Pour que SygixOs se répare tout seul ») ; OK sur cette ligne ouvre les écrans du débogage si le débogage est désactivé, sinon 9.4

#### Scenario: rien de configuré
- **WHEN** aucune voie n'est « Active », que l'accueil d'origine est actif et que Tsuyu n'est pas disponible
- **THEN** le panneau 8.21 affiche « SygixOs n'est pas encore l'écran d'accueil. Voici l'état de chaque façon de le configurer. », « Configuration automatique » « À configurer » (« Pas encore configurée »), « Par autorisations » « À faire » (« Aucune autorisation donnée »), « Relancer la configuration » focalisé, sans « Remplacer complètement l'accueil Google TV » ni « Rétablir l'accueil Google TV »

#### Scenario: fermeture
- **WHEN** l'utilisateur presse Retour sur le panneau
- **THEN** il se ferme et la ligne des réglages a le focus avec son état recalculé

**Détails.** — Synthèse du panneau Launcher : Le panneau SHALL afficher « Configuration automatique », « Tsuyu » seulement si disponible, puis « Par autorisations », états et détails de synthèse. Une voie active SHALL faire décrire SygixOs comme accueil ; sinon comme pas encore accueil. La ligne Tsuyu SHALL refléter « États de Tsuyu » de tsuyu-access, avec les détails ci-dessous. Cas « description et synthèses » : quand le panneau « Launcher principal » s'ouvre, une voie active donne « SygixOs est l'écran d'accueil de la TV. Voici l'état de chaque façon de le configurer. », sinon « SygixOs n'est pas encore l'écran d'accueil. Voici l'état de chaque façon de le configurer. » ; exemples de détails : « Accueil Google TV désactivé · SygixOs est l'écran d'accueil », « Accueil Google TV gardé · touche Home et allumage réglés », « Pas encore configurée », « Pas utilisée sur cette TV », « Aucune autorisation donnée ». Cas « détails de Tsuyu » : quand la ligne Tsuyu est présente, Actif dit « Répare les réglages si la TV les défait », À installer « Pour que SygixOs se répare tout seul », Arrêté « OK : ouvre Tsuyu pour le démarrer », À autoriser « OK : autorise SygixOs dans Tsuyu », À mettre à jour « Version trop ancienne pour SygixOs », Indisponible sur cette TV « Le débogage de cette TV ne permet pas à Tsuyu de démarrer ». — Actions du panneau Launcher : Le panneau SHALL proposer relancer, remplacer si voie active/accueil actif modifiable/garde-fous tenus, rétablir si un composant est désactivé par l'utilisateur. OK sur automatique/relancer SHALL suivre le parcours via Tsuyu actif sinon sans ; autorisations ouvre 8.13, remplacer 8.8, rétablir sa confirmation. L'aide SHALL être « Retour : fermer ». Cas « libellés des actions » : quand les actions sont affichées, « Relancer la configuration » dit « Refait les réglages automatiques » avec Tsuyu actif, sinon « Refait les réglages automatiques (débogage nécessaire) » ; « Remplacer complètement l'accueil Google TV » dit « Le désactive : selon la TV, des touches de la télécommande ne marchent plus » ; « Rétablir l'accueil Google TV » dit « Le réactive et lui rend l'écran d'accueil ». Cas « action de la ligne Tsuyu » : quand OK est pressé sur Tsuyu, À installer/À mettre à jour ouvre débogage si besoin puis 9.4, Arrêté 9.7, À autoriser 9.8, Actif Tsuyu, Indisponible sur cette TV reste sans effet.

### Requirement: État de l'écran d'accueil
SygixOs SHALL rester candidat HOME et relire sans débogage les états système au retour au premier plan, à l'ouverture du parcours/panneau et après chaque opération : écran d'accueil effectif, rôle HOME, composants de l'accueil d'origine, fin de configuration, service et superposition. Les valeurs relues SHALL remplacer les anciennes ; une erreur ne SHALL jamais produire « Active ». L'autorisation constructeur illisible garde la dernière relecture.

#### Scenario: configuration défaite hors de SygixOs
- **WHEN** l'utilisateur ou le système réactive l'accueil d'origine ou change l'écran d'accueil pendant que SygixOs est en arrière-plan
- **THEN** au retour au premier plan, les états sont recalculés, sans réapplication ni rappel automatique

#### Scenario: accueil désactivé par la procédure ADB
- **WHEN** SygixOs se lance alors que l'accueil d'origine a été désactivé par la procédure ADB du README et que SygixOs est l'écran d'accueil effectif
- **THEN** la voie automatique est « Active » et le panneau « Launcher principal » propose « Rétablir l'accueil Google TV »

#### Scenario: erreur de lecture
- **WHEN** la lecture de l'état d'un composant ou de l'écran d'accueil échoue
- **THEN** aucune voie n'est déclarée « Active » sur cette lecture, aucun crash n'a lieu et la lecture est refaite au prochain retour au premier plan

**Détails.** — Lectures de l'état du launcher : Les lectures SHALL couvrir écran d'accueil effectif, rôle HOME, composants d'origine (actif, désactivé par l'utilisateur, hors de portée), configuration initiale terminée, accessibilité (activée, connectée) et superposition. SygixOs SHALL être effectif seulement si HOME se résout vers lui et qu'aucune autre activité HOME activée n'a de priorité supérieure. Cas « résolution et état relus » : quand une lecture est déclenchée selon « État de l'écran d'accueil », ces champs sont relus ; « configuration défaite hors de SygixOs », « accueil désactivé par la procédure ADB » et « erreur de lecture » restent applicables.

### Requirement: Désactivation de l'accueil d'origine
La désactivation SHALL venir seulement de 9.11 ou du panneau Launcher, après confirmation et garde-fous relus (accès prêt, configuration terminée, activité HOME activée, un composant actif et modifiable). Elle SHALL désactiver les composants un par un puis vérifier l'écran d'accueil effectif ; un échec ou une vérification négative SHALL les remettre à leur état par défaut dans l'ordre inverse, sans laisser la TV sans accueil actif.

#### Scenario: confirmation
- **WHEN** l'utilisateur presse OK sur « Désactiver l'accueil Google TV » en 9.11
- **THEN** le dialogue 8.8 s'affiche, sans mascotte, avec « Annuler » focalisé ; aucun composant n'est modifié avant OK sur « Désactiver »

#### Scenario: annulation
- **WHEN** l'utilisateur presse « Annuler » ou Retour dans 8.8 ouvert depuis 9.11
- **THEN** le dialogue se ferme, rien n'est modifié et 9.11 a le focus sur « Désactiver l'accueil Google TV »

#### Scenario: désactivation réussie
- **WHEN** l'utilisateur presse « Désactiver », que chaque composant actif est désactivé et que SygixOs est ensuite l'écran d'accueil effectif
- **THEN** l'écran « SygixOs est votre écran d'accueil » s'affiche et l'état relu de chaque composant est « désactivé par l'utilisateur »

#### Scenario: remplacement depuis le panneau
- **WHEN** Tsuyu est « Actif », que l'utilisateur presse « Remplacer complètement l'accueil Google TV » puis « Désactiver » et que la désactivation réussit
- **THEN** aucun écran du débogage n'est affiché, le panneau revient avec « Configuration automatique » « Active », « Rétablir l'accueil Google TV » affiché et focalisé, et sans « Remplacer complètement l'accueil Google TV »

#### Scenario: désactivation partielle annulée
- **WHEN** la désactivation d'un composant échoue ou dépasse son délai, ou que SygixOs n'est pas l'écran d'accueil effectif après la désactivation
- **THEN** les composants désactivés par cette opération sont remis à leur état par défaut dans l'ordre inverse, l'état est relu, aucun succès n'est affiché, et les réglages de secours sont posés depuis 9.11, ou l'encadré « L'accueil Google TV n'a pas pu être désactivé » s'affiche depuis le panneau

#### Scenario: appareil pas encore configuré
- **WHEN** la configuration initiale de l'appareil n'est pas terminée
- **THEN** 9.11 n'est pas affiché, « Remplacer complètement l'accueil Google TV » n'est pas proposé et aucune désactivation n'est exécutée

**Détails.** — Confirmation de désactivation : Seules la ligne « Désactiver l'accueil Google TV » en 9.11 et l'action « Remplacer complètement l'accueil Google TV » SHALL ouvrir 8.8. « Annuler » SHALL être focusé, puis « Désactiver », aide « Retour : annuler » ; gauche/droite sans boucle, haut/bas sans effet. Retour/Annuler SHALL rendre l'écran et le focus ouvrants sans modification. Cas « avertissement explicite » : quand « Désactiver l'accueil Google TV ? » s'affiche, le texte est « SygixOs deviendra l'écran d'accueil. Selon la TV, des fonctions de l'accueil Google TV peuvent disparaître : réglages rapides, guide des programmes, appui long sur Home, choix du profil. Rétablissez-le dans Réglages › Écran d'accueil, et avant de désinstaller SygixOs. ». — Garde-fous de désactivation : Juste avant l'opération, SygixOs SHALL relire : accès prêt, configuration initiale de la TV terminée, activité HOME SygixOs déclarée et activée, au moins un composant d'origine actif/modifiable. Toute condition non tenue SHALL empêcher toute désactivation. Cas « appareil pas encore prêt » : quand une condition manque, « appareil pas encore configuré » de « Désactivation de l'accueil d'origine » et les garde-fous de p7-default-launcher D6 empêchent l'opération. — Séquence de désactivation : Selon p7-default-launcher D6, les composants actifs SHALL être désactivés un par un dans l'ordre de l'ensemble, chacun relu, puis HOME effectif vérifié. La progression SHALL être « Configuration en cours » avec ligne « Accueil Google TV ». Succès depuis 9.11 : résultat effectif ; depuis panneau : états recalculés, focus « Rétablir l'accueil Google TV ». Cas « opération réussie » : quand tous les composants et l'accueil effectif sont confirmés, « désactivation réussie » et « remplacement depuis le panneau » de « Désactivation de l'accueil d'origine » s'appliquent. — Annulation d'une désactivation échouée : Échec, délai ou HOME non confirmé SHALL remettre par défaut les composants touchés par cette opération, dans l'ordre inverse, puis relire. Depuis 9.11, les secours SHALL suivre ; depuis panneau, 8.12 SHALL afficher l'échec, sans laisser la TV sans accueil activé. Cas « retour arrière de l'opération » : quand la désactivation échoue depuis le panneau, l'encadré dit « L'accueil Google TV n'a pas pu être désactivé », détail « Il est resté actif et rien d'autre n'a changé. Vérifiez l'accès, puis réessayez. » ; le scénario « désactivation partielle annulée » de « Désactivation de l'accueil d'origine » reste applicable.

### Requirement: Rétablissement de l'accueil d'origine
OK sur « Rétablir l'accueil Google TV », accès prêt, SHALL ouvrir le dialogue de confirmation ; confirmé, le rétablissement SHALL remettre les composants désactivés à leur état par défaut dans l'ordre inverse, puis redéfinir l'écran d'accueil précédent s'il a été relevé, présent et activé, le tout relu. Autorisations, service d'accessibilité et Tsuyu SHALL rester inchangés. Succès : panneau relu, focus « Relancer la configuration » ; échec : « L'accueil Google TV n'a pas pu être rétabli ».

#### Scenario: rétablissement réussi
- **WHEN** l'utilisateur presse « Rétablir » et que les composants sont remis à leur état par défaut et l'écran d'accueil précédent redéfini
- **THEN** le panneau revient sans « Rétablir l'accueil Google TV », « Relancer la configuration » a le focus et la voie automatique n'est plus « Active » si aucun réglage de secours n'est en place

#### Scenario: rétablissement par Tsuyu
- **WHEN** Tsuyu est « Actif », le débogage n'ayant pas été réactivé par l'utilisateur depuis la configuration
- **THEN** OK sur « Rétablir l'accueil Google TV » affiche directement 8.17, sans écran du débogage ni fenêtre système

#### Scenario: rétablissement annulé
- **WHEN** l'utilisateur presse « Annuler » ou Retour dans 8.17
- **THEN** rien n'est modifié et « Rétablir l'accueil Google TV » a le focus

#### Scenario: sans écran d'accueil précédent relevé
- **WHEN** l'accueil d'origine a été désactivé par la procédure ADB du README et qu'aucun écran d'accueil précédent n'a été relevé
- **THEN** seuls les composants sont remis à leur état par défaut, et le système réattribue l'écran d'accueil

#### Scenario: échec du rétablissement
- **WHEN** la remise à l'état par défaut d'un composant échoue ou dépasse son délai
- **THEN** l'encadré « L'accueil Google TV n'a pas pu être rétabli » s'affiche avec « Réessayer » focalisé et l'état relu, sans succès supposé

**Détails.** — Confirmation du rétablissement : Le dialogue 8.17 SHALL demander « Rétablir l'accueil Google TV ? », proposer « Annuler » et « Rétablir » focalisé, aide « Retour : annuler », avec navigation de 8.8. Son texte SHALL expliquer la portée exacte du rétablissement. Cas « texte du dialogue » : quand le dialogue de rétablissement est affiché, il dit « L'accueil Google TV sera réactivé et redeviendra l'écran d'accueil. Les autres réglages faits par SygixOs restent en place : si son service reste activé, la touche Home ouvrira encore SygixOs. » ; « rétablissement annulé » de « Rétablissement de l'accueil d'origine » préserve la TV. — Opération de rétablissement : Confirmé, le rétablissement SHALL remettre par défaut les composants désactivés par l'utilisateur dans l'ordre inverse de l'ensemble ; si l'accueil précédent relevé est présent et activé, le redéfinir. Tout SHALL être relu avant le résultat, sans toucher les autres réglages. Cas « avec ou sans accueil précédent » : quand l'utilisateur confirme le rétablissement, « rétablissement réussi », « rétablissement par Tsuyu », « sans écran d'accueil précédent relevé » et « échec du rétablissement » de « Rétablissement de l'accueil d'origine » s'appliquent.

### Requirement: Démarrage automatique du constructeur
La configuration automatique SHALL accorder au seul paquet SygixOs `AUTO_START`, ou `APP_AUTO_START` si seul disponible, enregistrer les opérations pour persistance puis relire. Déjà accordée : aucune écriture ; absente : étape sans objet et ligne masquée. Refus, délai ou relecture négative ne SHALL produire ni succès ni réessai sans action. Cette autorisation ne SHALL garantir aucun démarrage.

#### Scenario: autorisation accordée
- **WHEN** l'opération existe avec une valeur autre que l'autorisation et que la séquence l'accorde
- **THEN** la relecture la donne autorisée et la ligne « Démarrage automatique » passe à « Fait »

#### Scenario: déjà accordée
- **WHEN** l'opération est déjà autorisée pour SygixOs
- **THEN** aucune écriture n'a lieu et la ligne est « Fait »

#### Scenario: relecture négative
- **WHEN** l'écriture semble réussir mais que la relecture ne donne pas l'opération autorisée
- **THEN** l'étape est en échec et la séquence continue avec l'écran d'accueil

### Requirement: Opérations privilégiées limitées
ADB intégré et Tsuyu SHALL n'exécuter que les opérations/arguments fixes de p7-default-launcher D2 vers SygixOs, son service ou l'ensemble fermé de l'accueil d'origine ; toute autre cible est refusée avant envoi. Aucune commande externe ni révocation d'autorisation ne SHALL être exécutée. Les autres services d'accessibilité SHALL être conservés ; le retrait de celui de SygixOs n'est permis que pour la réparation Tsuyu. Aucune connexion privilégiée ne SHALL rester ouverte hors séquence demandée.

#### Scenario: cible refusée
- **WHEN** une opération est demandée avec un paquet ou un composant hors du paquet SygixOs et de l'ensemble fermé
- **THEN** elle est refusée sans être envoyée, par l'ADB intégré comme par Tsuyu, et l'échec est rapporté

#### Scenario: service d'accessibilité ajouté
- **WHEN** la configuration active le service d'accessibilité de SygixOs alors que d'autres services sont activés
- **THEN** la liste relue contient les autres services inchangés et celui de SygixOs

#### Scenario: service relié par la réparation
- **WHEN** la réparation automatique retire puis remet le service de SygixOs
- **THEN** la liste relue après l'opération contient les autres services inchangés et celui de SygixOs

#### Scenario: fin de séquence
- **WHEN** une séquence ou une réparation se termine, réussie, en échec ou par délai
- **THEN** la connexion au débogage est fermée, ou le service de SygixOs lancé par Tsuyu est arrêté

**Détails.** — Clé et connexion ADB locale : L'ADB SHALL se connecter au bouclage sur le port réseau de l'appareil seulement, après action explicite dans le parcours, puis fermer à la fin de la séquence. La paire de clés propre à SygixOs SHALL être générée à la première connexion, conservée en stockage privé, autorisée dans « Autoriser le débogage USB ? » et ne jamais sortir de l'appareil. Cas « autorisation et fermeture » : quand le parcours utilise l'ADB intégré, « clé déjà autorisée » de « Accès par le débogage » et « fin de séquence » de « Opérations privilégiées limitées » s'appliquent sans connexion à un autre appareil. — Conservation des services et accès privilégiés : Les autres services d'accessibilité activés SHALL être conservés. Retirer puis remettre celui de SygixOs ne SHALL être permis que pour la réparation automatique de tsuyu-access. Aucune connexion privilégiée ni service SygixOs lancé par Tsuyu ne SHALL rester ouvert hors séquence utilisateur ou réparation automatique. Cas « service et fin de séquence » : quand une opération privilégiée touche le service ou se termine, « service d'accessibilité ajouté », « service relié par la réparation » et « fin de séquence » de « Opérations privilégiées limitées » s'appliquent.

### Requirement: Détails des modifications
« Détails » des écrans de succès/secours SHALL ouvrir « Ce que SygixOs a modifié » (8.14/9.16), sans mascotte. Le panneau SHALL suivre les scénarios ci-dessous : lignes non focusables des réglages réellement modifiés avec états relus, accueil Google TV toujours, Tsuyu et réparations selon les cas, aide « Retour : fermer » et bouton « Fermer » focalisé rendant le focus à « Détails ».

#### Scenario: détails après les réglages de secours
- **WHEN** l'utilisateur presse OK sur « Détails » sur « SygixOs garde la main » sans Tsuyu
- **THEN** 8.14 liste « Démarrage automatique », « Écran d'accueil », « Superposer aux autres applications » et « Touche Home » à l'état « Fait », et « Accueil Google TV » à l'état « Inchangé » ; « Fermer » a le focus et rend le focus à « Détails »

#### Scenario: détails avec Tsuyu
- **WHEN** l'utilisateur presse OK sur « Détails » après une configuration par Tsuyu installé par SygixOs, une réparation ayant eu lieu
- **THEN** 9.16 liste aussi « Tsuyu » « Fait » et « Réparations automatiques » « Active » avec la date et le réglage de la dernière réparation

#### Scenario: détails après désactivation
- **WHEN** l'utilisateur presse OK sur « Détails » sur « SygixOs est votre écran d'accueil » après une désactivation sans Tsuyu
- **THEN** 8.14 liste « Démarrage automatique », « Écran d'accueil » et « Accueil Google TV » « Désactivé à votre demande », sans les réglages de secours

**Détails.** — Lignes des modifications : Le panneau SHALL montrer les réglages réellement modifiés, l'accueil Google TV toujours, Tsuyu seulement installé par SygixOs et les réparations si configuration via Tsuyu. Les lignes SHALL être non focusables avec états relus, selon les détails ci-dessous, sans prétendre que d'autres réglages ont changé. Cas « contenu du panneau de détails » : quand « Ce que SygixOs a modifié » est affiché, la description est « Rien d'autre n'a été touché. « Rétablir l'accueil Google TV » ne concerne que l'accueil ; les autorisations se retirent dans les paramètres Android. » ; les lignes sont « Démarrage automatique » / « La TV laisse SygixOs démarrer avec elle », « Écran d'accueil » / « SygixOs défini comme écran d'accueil d'Android », « Superposer aux autres applications » / « Permet à SygixOs de s'ouvrir au démarrage de la TV », « Touche Home » / « Le service de SygixOs ne lit que la touche Home ». Cas « accueil et réparations dans les détails » : quand les lignes d'accueil ou Tsuyu sont présentes, « Accueil Google TV » dit « Toujours actif : désactivé seulement si vous le choisissez » avec « Inchangé », sinon « Désactivé à votre demande » avec « Fait » ; « Tsuyu » dit « Installé par SygixOs : il garde l'accès au débogage » avec « Fait » ; « Réparations automatiques » dit « Dernière : <date> · <réglage> » ou « Aucune pour l'instant », « Active » si Tsuyu « Actif », sinon « Arrêtée ».

### Requirement: Débogage après la configuration
Après succès sans Tsuyu, « Conseil : désactivez le débogage » SHALL rester tant qu'il est activé, relu au retour ; reconfiguration/rétablissement exigent sa réactivation (écrans du débogage et 8.4). Avec Tsuyu, aucun conseil de désactivation : « Réparation automatique » SHALL demander de le garder activé car Tsuyu en dépend. SygixOs ne SHALL jamais activer ni désactiver le débogage lui-même.

#### Scenario: conseil masqué
- **WHEN** l'utilisateur désactive le débogage puis revient dans SygixOs sur « SygixOs est votre écran d'accueil »
- **THEN** la ligne « Conseil : désactivez le débogage » n'est plus affichée

#### Scenario: pas de conseil avec Tsuyu
- **WHEN** la configuration s'est faite par Tsuyu
- **THEN** ni 9.12 ni 9.13 n'affichent « Conseil : désactivez le débogage » et la ligne « Réparation automatique » est affichée
