# Delta launcher-shell

## MODIFIED Requirements

### Requirement: Panneau Top Shelf au focus
Le panneau d'aperçu SHALL s'ouvrir à la demande au-dessus de la rangée focusée, et se refermer quand l'app focusée n'a rien à montrer ; aucune place n'est réservée quand il est fermé. Le défilement de la grille SHALL suivre une règle de placement unique, appliquée au bloc focusé (la rangée focusée, plus le panneau quand il est ouvert au-dessus d'elle) : le bloc reste entre la marge haute de la grille (40 dp sous le haut de l'écran) et la marge basse (20 dp au-dessus du bas de l'écran, l'espacement entre rangées). La grille ne défile que pour y ramener le bloc, et ce défilement est animé avec la durée et la courbe de l'expansion du panneau.

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
- **THEN** la position de défilement n'est pas remise à zéro : la grille réapparaît à la même position, le focus va à la dernière tuile visitée (« Navigation 3 paliers »), et la grille ne défile que si cette tuile est hors des marges

## ADDED Requirements

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
