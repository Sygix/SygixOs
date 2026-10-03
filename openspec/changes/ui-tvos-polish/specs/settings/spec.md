# Delta settings

## RENAMED Requirements

- FROM: `### Requirement: Icône réglages flottante`
- TO: `### Requirement: Capsule heure et réglages`

## MODIFIED Requirements

### Requirement: Capsule heure et réglages
Le home SHALL afficher sur le héro, en haut à droite, une capsule en verre sombre (« Thème » de `launcher-shell`) qui contient l'heure courante puis un engrenage dessiné au trait ouvrant la page de réglages ; elle remplace l'engrenage seul. L'heure SHALL suivre le format 12 ou 24 h choisi dans le système, n'est jamais focusable et SHALL se mettre à jour au changement de minute sans recomposer le reste de l'écran. Seul l'engrenage SHALL prendre le focus. La capsule SHALL être solidaire du héro, comme le dock (« Dock d'apps épinglées » de `launcher-shell`) : elle n'a aucun fondu propre, sort par le haut avec le héro pendant le défilement vers la grille, revient avec lui, et n'est jamais visible en vue grille.

#### Scenario: affichage
- **WHEN** le héro est affiché
- **THEN** la capsule est visible en haut à droite avec l'heure puis l'engrenage, sans masquer le titre du héro et sans détourner le focus du héro à l'ouverture du home ; au repos l'engrenage est blanc sur une pastille claire discrète

#### Scenario: heure
- **WHEN** la minute change, ou que l'heure ou le fuseau du système change
- **THEN** l'heure affichée est mise à jour dans le format 12 ou 24 h du système, seule l'heure est recomposée ; un changement du format 12/24 h est pris en compte au plus tard à la minute suivante

#### Scenario: retour au premier plan
- **WHEN** le launcher revient au premier plan après un passage en arrière-plan
- **THEN** l'heure affichée redevient l'heure courante dès le retour, sans attendre le changement de minute ; aucune mise à jour n'a lieu tant que le launcher est en arrière-plan

#### Scenario: accès DPAD
- **WHEN** l'utilisateur presse haut depuis le héro
- **THEN** l'engrenage prend le focus : pastille blanche, icône noire, léger zoom et ombre ; l'heure ne prend jamais le focus ; bas depuis l'engrenage rend le focus au héro ; Retour depuis l'engrenage rend le focus au bouton d'ouverture du héro (au héro s'il n'en a pas) ; gauche et droite laissent le focus sur l'engrenage

#### Scenario: ouverture
- **WHEN** l'utilisateur presse OK sur l'engrenage
- **THEN** la page de réglages plein écran s'ouvre, la lecture du héro est mise en pause

#### Scenario: zone grille
- **WHEN** l'utilisateur passe en zone grille et la page de l'accueil défile du héro vers la grille (« Navigation 3 paliers » de `launcher-shell`)
- **THEN** la capsule suit exactement le mouvement du héro, sans fondu propre ni décalage, et sort par le haut avec lui ; à la fin du défilement elle est entièrement hors écran ; ni elle ni l'engrenage ne sont visibles ou focusables tant que la grille est affichée

#### Scenario: retour sur le héro
- **WHEN** l'utilisateur revient sur le héro depuis la grille (Retour, ou remontée par le dock)
- **THEN** la capsule revient par le haut avec le héro, dans le même mouvement et sans fondu propre, jusqu'à sa position initiale, sans que l'engrenage prenne le focus

### Requirement: Page de réglages
Le launcher SHALL offrir une page de réglages plein écran à la tvOS : volet catégories à gauche, contenu de la catégorie à droite, sur le même fond que la zone grille de l'accueil (« Fond de la zone grille » de `launcher-shell`), navigable au DPAD uniquement. Le focus de toute ligne focusable de la page (catégories, lignes d'« Apps sources » et d'« Applications cachées », bouton « Tout réactiver », lignes d'« À propos ») SHALL être une pilule claire à texte et icônes sombres, sans zoom, sans halo et sans matériau verre. La catégorie active SHALL rester marquée par une pilule grise discrète quand le focus est dans le volet droit.

#### Scenario: structure
- **WHEN** la page de réglages s'ouvre
- **THEN** le volet gauche liste les catégories « Apps sources », « Applications cachées », « À propos », le volet droit affiche le contenu de la catégorie active, la première catégorie porte le focus à l'ouverture

#### Scenario: changement de catégorie
- **WHEN** l'utilisateur presse haut/bas dans le volet gauche
- **THEN** la catégorie active change et le volet droit affiche son contenu ; droite depuis le volet gauche porte le focus sur le premier élément du volet droit, sauf pour « Applications cachées » dont le focus initial est la première ligne de la liste (« Applications cachées », scénario « focus initial ») ; gauche depuis le volet droit le rend au volet gauche

#### Scenario: retour
- **WHEN** l'utilisateur presse Retour depuis la page de réglages
- **THEN** le home reprend avec le héro affiché et focusé (comportement standard), la lecture du héro reprend

#### Scenario: fond
- **WHEN** la page de réglages s'ouvre
- **THEN** son fond est identique à celui de la zone grille de l'accueil

#### Scenario: focus d'une ligne
- **WHEN** une ligne de la page prend le focus
- **THEN** elle devient une pilule claire à texte et icônes sombres, à la même taille et à la même place qu'au repos (aucun zoom), sans halo ni verre ; au repos les lignes n'ont pas de fond, sauf « Tout réactiver » qui garde un fond discret

#### Scenario: catégorie active, focus à droite
- **WHEN** le focus est dans le volet droit
- **THEN** la catégorie active est une pilule grise discrète à texte blanc et les autres catégories n'ont pas de fond
