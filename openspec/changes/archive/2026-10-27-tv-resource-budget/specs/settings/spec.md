## MODIFIED Requirements

### Requirement: Apps sources
« Apps sources » SHALL lister uniquement les apps TV installées, avec un switch Apple activé par défaut pour leur contribution au héro, Top Shelf et Up Next. Les compteurs SHALL couvrir tous leurs programmes du TV Provider, avec une seule observation active réglages ouverts, en pause hors premier plan et recomptage au retour ; tri, focus et comptage SHALL suivre les scénarios ci-dessous.

#### Scenario: présentation
- **WHEN** la catégorie « Apps sources » est affichée
- **THEN** la description du volet est « Choisissez les applications qui alimentent le héro, le Top Shelf et À suivre. », aucune ligne de contenu retiré n'y figure, et chaque ligne montre l'icône de l'app, son nom et sous le nom des informations sur l'app dont le nombre de programmes publiés dans le TV Provider, avec à droite de la ligne un toggle switch

#### Scenario: tri par contenu publié
- **WHEN** la catégorie est affichée avec des apps publiant respectivement 12, 0, 3 et 0 programmes
- **THEN** l'ordre est : l'app à 12, l'app à 3, puis les deux apps à 0 par ordre alphabétique

#### Scenario: compteurs prêts à l'entrée
- **WHEN** l'utilisateur entre dans la catégorie « Apps sources » alors qu'un comptage, lancé à une ouverture précédente des réglages, est déjà connu
- **THEN** la liste s'affiche directement triée par les derniers compteurs connus, sans passage par l'ordre alphabétique ni retri ultérieur tant que la catégorie reste active ; le comptage lancé à l'ouverture met seulement à jour les nombres affichés

#### Scenario: compteurs en retard
- **WHEN** l'utilisateur entre dans la catégorie « Apps sources » avant l'arrivée du tout premier comptage, lancé à la première ouverture des réglages, puis ce comptage arrive alors que le focus est sur une ligne
- **THEN** la liste s'affiche d'abord par ordre alphabétique, puis elle est retriée une seule fois par compteur à l'arrivée du comptage ; le focus reste sur la même app, à sa nouvelle position ; les émissions suivantes mettent seulement à jour les nombres affichés, sans retri

#### Scenario: app qui ne publie que du Watch Next
- **WHEN** une app ne publie aucun `PreviewPrograms` mais publie des `WatchNextPrograms`
- **THEN** son compteur affiché est le nombre de ses `WatchNextPrograms`, supérieur à 0, et elle est triée parmi les apps à compteur non nul selon ce nombre, jamais parmi les apps à 0

#### Scenario: égalité de compteur
- **WHEN** deux apps publient le même nombre de programmes, supérieur à 0
- **THEN** elles sont départagées par ordre alphabétique de leur nom (insensible à la casse), à leur place dans la partie des apps à compteur non nul

#### Scenario: compteurs mis à jour dans la catégorie
- **WHEN** un compteur change (programme `PreviewPrograms` ou `WatchNextPrograms` publié ou retiré dans le TV Provider) alors que la catégorie « Apps sources » est active
- **THEN** le nombre affiché sur la ligne se met à jour sur place, aucune ligne ne change de position et le focus reste sur la même ligne (hors le retri unique du scénario « compteurs en retard »)

#### Scenario: nouvel ordre à la prochaine entrée
- **WHEN** l'utilisateur quitte la catégorie (autre catégorie ou fermeture des réglages) puis y revient après un changement de compteur
- **THEN** l'ordre est recalculé avec les compteurs connus à cette nouvelle entrée

#### Scenario: comptage en pause en arrière-plan
- **WHEN** les réglages sont fermés, ou le launcher passe en arrière-plan (une autre app au premier plan), et qu'une app publie ou retire des programmes pendant ce temps
- **THEN** l'observation du TV Provider pour les compteurs est arrêtée : aucune lecture des compteurs n'a lieu tant que les réglages restent fermés ou que le launcher reste en arrière-plan ; les derniers compteurs connus sont conservés

#### Scenario: nouveau comptage au retour au premier plan
- **WHEN** le launcher revient au premier plan réglages ouverts, ou que les réglages sont ouverts
- **THEN** un nouveau comptage est lancé aussitôt et l'observation reprend ; les nombres affichés se mettent à jour sur place, la prochaine entrée dans « Apps sources » ordonne la liste avec ce comptage, et une catégorie « Apps sources » déjà active garde son ordre (aucun retri : le retri unique du scénario « compteurs en retard » ne concerne que le tout premier comptage)

#### Scenario: aucun compteur disponible
- **WHEN** le comptage ne peut rien lire (TV Provider absent ou permission refusée)
- **THEN** toutes les apps sont considérées à 0 et listées par ordre alphabétique, y compris après l'arrivée du comptage vide, sans erreur visible ni crash ; le switch de chaque ligne reste utilisable

#### Scenario: bascule
- **WHEN** l'utilisateur presse OK sur une ligne
- **THEN** le switch bascule avec l'animation Apple, l'effet est immédiat : les programmes de l'app disparaissent ou réapparaissent dans le héro, le Top Shelf et la rangée Up Next sans redémarrage, et l'état est persisté (DataStore) ; la bascule ne change pas la position de la ligne (le tri ne dépend pas de l'état du switch) et le focus reste sur la ligne

**Détails.** — Compteurs des sources : Les compteurs SHALL compter tous les PreviewPrograms et WatchNextPrograms par app à l'ouverture des réglages, non à l'affichage de l'accueil, par l'unique observation existante sans seconde lecture. Elle SHALL réagir aux deux tables tant que les réglages sont ouverts, s'arrêter réglages fermés ou en arrière-plan sans relecture, conserver ses valeurs puis recompter aussitôt au retour au premier plan réglages ouverts. Cas « comptage partagé et cycle de vie » : quand les réglages s'ouvrent ou se ferment, ou que le launcher passe en arrière-plan ou revient, les scénarios de compteurs de « Apps sources », y compris Watch Next seul et aucun compteur disponible, s'appliquent. — Tri stable des sources : À chaque entrée de catégorie, les apps SHALL être triées : compteur > 0 décroissant, égalité par nom sans casse, puis zéros par nom. Sans premier comptage, alphabet puis un seul retri à son arrivée, focus par app, pas position. Ensuite les nombres seuls SHALL changer, ordre figé jusqu'à nouvelle entrée ; un aller-retour entre volets ne recalcule pas. Cas « compteurs en retard ou actualisés » : quand un compteur arrive dans la catégorie, « compteurs en retard », « compteurs mis à jour dans la catégorie » et « nouvel ordre à la prochaine entrée » de « Apps sources » s'appliquent sans saut sous le focus.

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
