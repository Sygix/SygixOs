# Spec Delta

## ADDED Requirements

### Requirement: Fenêtre de rendu de la grille
La grille SHALL limiter ses rangées composées à la fenêtre visible et à une prélecture bornée indépendante du nombre total d'apps. Toutes les apps du catalogue restent accessibles. La virtualisation SHALL conserver « Navigation 3 paliers », « Panneau Top Shelf au focus », « Persistance de la grille » et « Focus d'une app disparue », sans nouveau palier ni seconde autorité de défilement vertical.

#### Scenario: grand catalogue
- **WHEN** le catalogue contient beaucoup plus de rangées que l'écran n'en affiche
- **THEN** les rangées éloignées ne sont pas composées simultanément ; leur nombre peut augmenter sans augmenter proportionnellement le nombre de tuiles actives pour un viewport identique

#### Scenario: cible non composée
- **WHEN** une touche D-pad ou une restauration désigne une tuile hors de la fenêtre composée
- **THEN** le défilement révèle sa rangée avant la demande de focus ; la bonne app reçoit le focus par son identité, sans perte de focus ni sélection d'une autre app réutilisant une position

#### Scenario: navigation et bords
- **WHEN** l'utilisateur parcourt les rangées, atteint un bord, presse Retour ou revient depuis une app
- **THEN** le focus initial, l'absence de boucle, les positions d'À suivre, le retour au héro et les replis suivent « Navigation 3 paliers » ; les titres restent non focusables

#### Scenario: shelf et déplacement
- **WHEN** un shelf s'ouvre sur une rangée profonde, ou que l'utilisateur déplace une app à travers une limite de fenêtre puis valide ou annule
- **THEN** le bloc focusé reste placé selon « Panneau Top Shelf au focus » et la réorganisation suit « Persistance de la grille », même si des rangées sont créées ou retirées de la composition

#### Scenario: catalogue modifié
- **WHEN** une app disparaît, devient cachée ou change de position pendant que sa tuile est active ou mémorisée pour le retour
- **THEN** le repli suit « Focus d'une app disparue » et la cible de repli est révélée avant de recevoir le focus ; une fenêtre recyclée ne garde ni artwork ni état de focus d'une autre app

#### Scenario: chargement et absence de catalogue
- **WHEN** le catalogue charge, devient vide ou ne peut pas fournir d'apps
- **THEN** les états de « Écran de démarrage » et « Grille d'apps » restent ceux de ces exigences, sans faux éléments focusables ni chargement exhaustif destiné seulement à créer la fenêtre

#### Scenario: sources de programmes indisponibles
- **WHEN** le TV Provider est absent, la permission est refusée, les programmes chargent ou leur rechargement échoue
- **THEN** les états du héro, de la rangée Up Next et de ses titres suivent leurs exigences existantes ; la virtualisation ne supprime pas la carte d'erreur ni ne transforme l'absence de source en absence d'apps

#### Scenario: retour au premier plan
- **WHEN** l'utilisateur revient sur le launcher après une mise à jour du catalogue ou de la rangée Up Next
- **THEN** les données sont rafraîchies selon leurs contrats et la cible encore valable est restaurée à sa position ; une cible disparue utilise son repli, sans retour arbitraire à la première rangée

### Requirement: Préparation bornée des artworks d'apps
La préparation des artworks SHALL cibler le dock et la fenêtre utile de la grille avec une prélecture bornée, plutôt que tout le catalogue. Elle SHALL respecter « Préchargement et mémoire », y compris le travail hors Main et l'absence de nouvelle préparation pendant un déplacement de focus. Les délais de validation des affiches du Top Shelf restent inchangés.

#### Scenario: ouverture et repos
- **WHEN** l'accueil est prêt et que la navigation est au repos
- **THEN** seuls le dock et la fenêtre utile avec sa prélecture sont préparés, en priorité par proximité ; aucune boucle ne décode toutes les bannières d'un grand catalogue

#### Scenario: défilement rapide
- **WHEN** l'utilisateur traverse rapidement plusieurs rangées
- **THEN** aucune nouvelle préparation ne démarre pendant le mouvement ; au repos, la fenêtre courante remplace le travail non commencé de l'ancienne, sans file de travail croissant avec toutes les rangées traversées

#### Scenario: artwork absent ou en erreur
- **WHEN** une bannière n'existe pas ou ne peut pas être décodée
- **THEN** le repli de « Grille d'apps » est conservé, la tuile reste navigable et l'échec ne bloque pas la révélation d'une cible ; un résultat tardif ne remplit pas une tuile d'une autre app
