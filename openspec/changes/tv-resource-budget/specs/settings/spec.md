## MODIFIED Requirements

### Requirement: Catégorie « Écran d'accueil »
Avant À propos, `SettingsCategory.HOME_SCREEN` SHALL suivre 7.7 : titre « Écran d'accueil » en `headlineSmall`, description « Choisissez ce qu'affiche l'accueil de SygixOs. » en `bodyMedium` blanc 60 %, espacements des autres catégories. Les lignes SHALL garder `Dimens.SettingsRowCorner`, minimum `Dimens.SettingsRowHeight`, retraits Apps sources et focus standard ; afficher À suivre puis sa position, puis « Mouvement des visuels », autres réglages après, sans changer le focus initial.

#### Scenario: en-tête du volet
- **WHEN** la catégorie « Écran d'accueil » est active
- **THEN** le volet droit affiche le titre « Écran d'accueil », la description « Choisissez ce qu'affiche l'accueil de SygixOs. », puis la ligne « Afficher À suivre », la ligne « Position d'À suivre » et la ligne « Mouvement des visuels », avec le même en-tête et les mêmes espacements que les autres catégories

#### Scenario: lignes de la catégorie
- **WHEN** une ligne de la catégorie est affichée, avec ou sans le focus
- **THEN** son rectangle a la même hauteur minimale et les mêmes retraits que les lignes d'« Apps sources », et son libellé est en `TextStyles.Row` au repos et `TextStyles.RowFocused` au focus

## ADDED Requirements

### Requirement: Mouvement des visuels
« Mouvement des visuels » SHALL être un switch Apple comme « Afficher À suivre », sous « Position d'À suivre », avec le texte secondaire « Zoom lent des images du héro et du Top Shelf », activé par défaut et persisté dans DataStore. Désactivé, il SHALL rendre immobiles les visuels du héro et du Top Shelf selon launcher-shell, sans toucher aux fondus, au défilement ni aux vidéos.

#### Scenario: valeur par défaut
- **WHEN** l'utilisateur ouvre la catégorie pour la première fois, y compris après une mise à jour depuis une version sans ce réglage
- **THEN** le switch est activé et l'accueil garde le zoom lent des visuels

#### Scenario: bascule
- **WHEN** l'utilisateur presse OK sur la ligne « Mouvement des visuels »
- **THEN** le switch bascule avec l'animation Apple, l'état est persisté dans DataStore et appliqué à l'accueil sans redémarrage : un visuel en cours de zoom s'arrête à sa taille d'origine à la fermeture des réglages ; le focus reste sur la ligne

#### Scenario: réglage conservé
- **WHEN** le launcher redémarre ou la TV est redémarrée
- **THEN** le switch affiche la valeur persistée et l'accueil l'applique dès le premier visuel

#### Scenario: animations du système coupées
- **WHEN** l'échelle d'animation du système est à 0 et le réglage est activé
- **THEN** les visuels restent immobiles comme aujourd'hui ; le switch reste modifiable et sa valeur s'appliquera quand les animations du système seront rétablies

#### Scenario: navigation D-pad de la ligne
- **WHEN** le focus est sur « Mouvement des visuels »
- **THEN** haut porte le focus sur « Position d'À suivre » ; bas ne fait rien tant que la ligne est la dernière de la catégorie (sans boucle) ; droite ne fait rien ; gauche rend le focus au volet des catégories, sur « Écran d'accueil » ; Retour ferme les réglages et le home reprend avec le héro focusé (« Page de réglages », scénario « retour »)

#### Scenario: sélecteur stable
- **WHEN** un test cherche la ligne
- **THEN** elle porte le testTag `setting-visual-motion` et son switch reflète l'état persisté
