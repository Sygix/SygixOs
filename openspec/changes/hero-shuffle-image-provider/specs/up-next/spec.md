# Spec Delta

## MODIFIED Requirements

### Requirement: Modèle canonique
La rangée SHALL manipuler un modèle canonique `UpNextItem` indépendant de l'app d'origine, avec un champ `externalIds` facultatif et la liste des sources du contenu. Le modèle SHALL conserver les candidates affiche et miniature publiées, sans perdre l'alternative lors du choix d'une image.

#### Scenario: constitution
- **WHEN** un programme watch next est converti en item de la rangée
- **THEN** l'item expose le package source, le type (épisode ou film, publié ou inféré selon « Champs facultatifs »), le titre de série, la saison, l'épisode, le titre d'affichage, les candidates affiche et miniature, la progression optionnelle, le type watch next, le timestamp d'activité, l'intent publié et le nom de l'app source ; le champ `externalIds` est présent mais vide en P6

#### Scenario: sources du contenu
- **WHEN** des items sont fusionnés par le dédoublonnage
- **THEN** l'item retenu conserve la liste des sources qui possèdent le contenu (package, nom, icône et intent publié de chacune), après fusion comme avant ; c'est cette liste qu'affiche le menu « Ouvrir avec… »

#### Scenario: extension par le change P8
- **WHEN** le change P9 dédié à BetaSeries apporte des identifiants externes (enrichissement) aux items de toutes les sources
- **THEN** le modèle canonique et la rangée absorbent ces identifiants sans refonte, et le niveau 5 de dédoublonnage s'active quand ils sont connus ; en P6, `externalIds` reste vide et le niveau 5 ne s'applique jamais

### Requirement: Carte Up Next
« À suivre » SHALL nommer la rangée de tous les groupes, pas une catégorie. Les cartes SHALL suivre les maquettes Penpot 7.1 à 7.5 et Carte Up Next (1920 × 1080 px = dp × 2), « Géométrie des cartes », « Médias des cartes », « Textes des cartes », « Progression et badge des cartes » et « Qualité des cartes ». Le focus SHALL suivre intégralement « Focus tvOS » ; le chargement partagé ne change ni la géométrie ni le comportement de focus.

#### Scenario: largeur et débordement
- **WHEN** la rangée est affichée avec au moins cinq cartes sur un écran de 960 dp de large
- **THEN** quatre cartes de 198 dp de large sont entièrement visibles à partir de la marge gauche, espacées de `Dimens.GridSpacing`, et la cinquième est coupée par le bord droit de l'écran ; aucune marge droite ne la masque

#### Scenario: défilement horizontal
- **WHEN** le focus passe à une carte partiellement visible ou hors écran
- **THEN** la rangée défile juste ce qu'il faut pour que la carte focalisée soit entière entre la marge gauche et le bord droit, avec la durée et la courbe de « Focus tvOS » ; au retour sur la rangée, la position de défilement laissée est conservée, sauf au retour d'une app ouverte depuis la rangée (« Contenus ouverts depuis SygixOs » : rangée ramenée à son début)

#### Scenario: fin de rangée
- **WHEN** la rangée compte plus de cartes que la largeur n'en montre et que le focus arrive sur la dernière carte
- **THEN** la rangée défile jusqu'à sa fin : le bord droit de l'emplacement de la dernière carte est à `Dimens.ScreenMarginH` du bord droit de l'écran, comme le bord gauche de la première carte, au début de la rangée, est à `Dimens.ScreenMarginH` du bord gauche ; seul le zoom de focus dépasse cette marge

#### Scenario: rangée qui tient dans la largeur
- **WHEN** toutes les cartes tiennent entre les deux marges de la grille
- **THEN** la rangée ne défile pas, quelle que soit la carte focalisée

#### Scenario: carte épisode
- **WHEN** l'item est un épisode (type publié ou inféré selon « Champs facultatifs »)
- **THEN** la carte affiche l'image 16:9 plein cadre (sinon le poster portrait centré sur le fond des tuiles) sous le dégradé, le titre de série en `TextStyles.Row`, une seconde ligne « SxxEyy · titre d'épisode » en `TextStyles.RowSecondary` (sans « SxxEyy » si les numéros manquent ; un épisode sans titre ne montre que son titre d'épisode, scénario « épisode sans titre ») et, pour un item en cours, la barre de progression sous les textes

#### Scenario: carte film
- **WHEN** l'item est un film (type publié ou inféré selon « Champs facultatifs »)
- **THEN** la carte affiche l'image et le titre du film seul, sans « SxxEyy » ni libellé d'action, et la barre de progression pour un item en cours

#### Scenario: badge et image
- **WHEN** une carte est affichée
- **THEN** le badge est l'icône réelle de l'app source obtenue du `PackageManager` (aucun logo embarqué, aucun libellé texte à la place de l'icône), en haut à droite ; sans icône disponible, la carte n'a pas de badge

#### Scenario: poster portrait
- **WHEN** le programme n'a pas d'image 16:9 exploitable mais un poster portrait exploitable
- **THEN** la carte montre ce poster centré horizontalement à pleine hauteur sur le fond des tuiles (variante « Poster portrait » du composant « Carte Up Next »), avec le dégradé, les textes, la barre de progression éventuelle et le badge des autres cartes

#### Scenario: placeholder
- **WHEN** toutes les candidates et copies locales utilisables manquent, échouent, expirent ou sont trop petites
- **THEN** la carte garde sa taille et ses coins, montre le fond des tuiles avec le pictogramme « image » centré horizontalement dans sa moitié haute, et conserve le dégradé, ses textes, sa barre de progression éventuelle et son badge

#### Scenario: image trop petite
- **WHEN** la largeur réelle de chaque image candidate est inférieure à la largeur affichée de la carte en pixels, hors zoom
- **THEN** la carte reste dans la rangée avec le placeholder à la place de l'image ; l'item n'est pas écarté

#### Scenario: focus
- **WHEN** une carte prend le focus
- **THEN** le comportement visuel est celui d'une tuile d'app selon l'exigence « Focus tvOS » de launcher-shell, sans exception, et aucun panneau Top Shelf ne s'ouvre

#### Scenario: image à la largeur de la carte
- **WHEN** une candidate décodable de 396 px de large est disponible pour une carte de 396 px, alors qu'elle ne satisfait pas la politique du héro ou du Top Shelf
- **THEN** elle remplace le placeholder ; le rejet dans une autre zone n'empêche pas son affichage dans À suivre

#### Scenario: chargement indépendant des images
- **WHEN** l'image d'une carte est en attente ou en cours de récupération et que d'autres cartes ont une image prête
- **THEN** la première montre son placeholder et les autres leur image ; aucune attente collective ne bloque la rangée, et les changements d'image ne modifient ni le focus ni la géométrie

#### Scenario: image locale pendant une panne
- **WHEN** le fournisseur est indisponible et qu'une copie locale respecte « Cache borné et fraîcheur des images » de launcher-shell pour un programme toujours valide
- **THEN** cette copie est affichée sans attendre le fournisseur ; à son expiration, le placeholder la remplace si aucune nouvelle image n'est utilisable

**Détails.** — Barre de progression des cartes : CONTINUE avec progression SHALL montrer une barre de `Dimens.ProgressHeight`, piste `SygixColors.ProgressTrack`, remplissage `SygixColors.OnDark`, à `Dimens.UpNextTextBottom` du bas ; textes à `Dimens.UpNextTextAboveProgress` au-dessus. Sans barre, textes à `Dimens.UpNextTextBottom`. Cas « barre présente ou absente » : quand une carte avec ou sans progression est affichée, les scénarios « carte épisode » et « carte film » de « Carte Up Next » gardent les retraits de texte correspondants. — Badge des cartes : L'icône réelle PackageManager SHALL être en haut à droite à `Dimens.UpNextBadgeInset`, taille `Dimens.HeroSourceIcon`, coins `Dimens.UpNextBadgeCorner` concentriques avec la carte (« Thème », couple card-badge). Sans icône disponible, aucun badge SHALL être affiché. Cas « app sans icône » : quand PackageManager ne fournit pas d'icône, « badge et image » de « Carte Up Next » omet le badge sans logo embarqué ni texte substitué. — Géométrie des cartes : La rangée horizontale SHALL commencer à `Dimens.ScreenMarginH`, sans marge droite, cartes de largeur (contenu des rangées d'apps − 3 × `Dimens.GridSpacing`) / 4 et même espacement : quatre entières, cinquième coupée. Elle SHALL défiler juste pour garder la carte focusée entière ; en fin, le rectangle sans zoom finit à la marge symétrique `ScreenMarginH`, le zoom pouvant déborder de son demi-agrandissement (7.10). Cas « largeur et défilement » : quand la rangée est parcourue, « largeur et débordement », « défilement horizontal », « fin de rangée » et « rangée qui tient dans la largeur » de « Carte Up Next » s'appliquent. — Médias des cartes : La carte SHALL être 16:9, coins `Dimens.TileCorner`, fond `SygixColors.TileBackground`, image plein cadre sinon portrait centré sinon pictogramme image `Dimens.UpNextPlaceholderIcon` / `SygixColors.PlaceholderGlyph` dans la moitié haute centrée. Dégradé SHALL aller du transparent à `Dimens.CardScrimStart` jusqu'à `SygixColors.CardScrimEnd` en bas ; fond continu avec la grille. Cas « portrait ou absence d'image » : quand le média paysage n'est pas exploitable, « poster portrait » et « placeholder » de « Carte Up Next » gardent le portrait à pleine hauteur centré ou le pictogramme, sans changer taille ni coins. — Textes des cartes : En bas à gauche à `Dimens.UpNextTextInset`, le titre SHALL utiliser `TextStyles.Row` / `SygixColors.OnDark` (série pour épisode, film sinon). L'épisode SHALL ajouter « SxxEyy · titre d'épisode » en `TextStyles.RowSecondary` / `SygixColors.OnDarkDetails`, selon les champs disponibles. Aucun autre texte, action ni nom d'app ne SHALL apparaître. Cas « épisode ou film » : quand une carte est affichée, « carte épisode » et « carte film » de « Carte Up Next » gardent les textes et leurs replis. — Progression et badge des cartes : La progression SHALL suivre « Barre de progression des cartes » et le badge « Badge des cartes », avec les mêmes métadonnées et replis. Cas « données ou icône absentes » : quand progression ou icône manque, « carte épisode », « carte film » et « badge et image » de « Carte Up Next » omettent seulement l'élément absent. — Qualité des cartes : La qualité SHALL être propre aux cartes, sans appliquer le seuil du héro. La largeur réelle de l'image SHALL atteindre au moins la largeur affichée de la carte en pixels hors zoom, portrait accepté. Les dimensions SHALL venir du parcours partagé de launcher-shell sans hériter du verdict d'une autre destination. Sinon les candidates sont essayées puis l'item reste avec placeholder. Cas « qualité insuffisante » : quand l'image est trop petite pour la carte, le scénario « image trop petite » de « Carte Up Next » garde l'item et son placeholder seulement après épuisement des candidates et copies locales utilisables.

### Requirement: États de la rangée
La rangée SHALL couvrir les états chargement, erreur, vide et permission refusée, sans jamais avaler une erreur ni bloquer le reste du home. Le comportement du focus quand une carte disparaît ou que la rangée se masque est spécifié par « Navigation 3 paliers » de launcher-shell (scénarios « carte focusée disparue » et « rangée devenue vide »). La conservation d'un ancien résultat SHALL respecter « Validité des programmes publiés » de launcher-shell.

#### Scenario: permission refusée
- **WHEN** `READ_TV_LISTINGS` n'est pas accordée
- **THEN** la rangée est masquée, sans carte d'erreur et sans invitation à configurer quoi que ce soit

#### Scenario: erreur
- **WHEN** la requête vers le TV Provider échoue ou dépasse le timeout au premier chargement
- **THEN** une seule carte d'état focusable remplace le contenu de la rangée, en première position (maquette Penpot 7.4) : même taille et mêmes coins qu'une carte, fond `SygixColors.TileBackground`, et, centrés, un pictogramme « recharger » de `Dimens.UpNextErrorIcon`, « Impossible de charger » en `TextStyles.Row` et `SygixColors.OnDark`, « OK pour réessayer » en `TextStyles.RowSecondary` et `SygixColors.OnDarkSecondary` ; elle est distincte de l'état vide, sans crash ; la rangée n'est pas masquée : le titre « À suivre » reste affiché au-dessus de la carte, à la position réglée

#### Scenario: carte d'erreur
- **WHEN** la carte d'erreur est focusée
- **THEN** elle a le focus d'une tuile (« Focus tvOS »), occupe la place de la rangée dans la navigation de la zone grille, sous le titre « À suivre », quelle que soit la position réglée (« Avant les applications » ou « Après les applications »), et OK relance la requête

#### Scenario: chargement initial
- **WHEN** le premier chargement dépasse ~300 ms
- **THEN** un squelette est affiché sous le titre « À suivre » (maquette Penpot 7.3) : cinq cartes de la taille et des coins d'une carte, fond `SygixColors.SkeletonBase`, chacune avec deux barres `SygixColors.SkeletonBar` en bas à gauche (`Dimens.UpNextSkeletonTitle` puis `Dimens.UpNextSkeletonSubtitle`, rayons à mi-hauteur), sans animation imposée et sans élément focusable ; s'il est plus court, aucune étape intermédiaire n'apparaît

#### Scenario: rechargement
- **WHEN** la rangée se recharge alors qu'un contenu encore valide est déjà affiché
- **THEN** l'état précédent reste affiché jusqu'au résultat ou à son expiration, sans squelette ; l'attente ne prolonge pas sa validité

#### Scenario: erreur au rechargement
- **WHEN** un rechargement échoue alors qu'un contenu encore valide est déjà affiché
- **THEN** le contenu précédent reste affiché jusqu'à son expiration, sans carte d'erreur ni saut de mise en page ; un échec ne renouvelle pas sa confirmation

#### Scenario: vide
- **WHEN** aucun item n'est retourné par une lecture réussie
- **THEN** la rangée est masquée ; les images en cache ne la repeuplent pas

#### Scenario: non-blocage
- **WHEN** la rangée est dans n'importe quel état
- **THEN** la navigation DPAD du home (héro, dock, grille) reste fonctionnelle

#### Scenario: expiration du dernier résultat après une erreur
- **WHEN** les programmes affichés atteignent le plafond sans confirmation pendant que la source reste en erreur
- **THEN** les anciennes cartes sont retirées et l'état d'erreur réessayable remplace le résultat expiré ; le focus est restauré selon la navigation existante, sans effacer les contenus retirés ni la mémoire d'ouverture

### Requirement: Rafraîchissement
La rangée SHALL se recharger quand le launcher revient au premier plan, que `READ_TV_LISTINGS` vient d'être accordée ou que le TV Provider signale un changement, ainsi qu'à la périodicité de relecture de « Contenu héro TV Provider » de launcher-shell tant que le launcher est au premier plan. Les résultats SHALL suivre « Validité des programmes publiés ».

#### Scenario: retour au premier plan
- **WHEN** le launcher revient au premier plan (ou que la permission vient d'être accordée)
- **THEN** la rangée se recharge, le contenu précédent reste affiché pendant ce temps seulement s'il est encore valide

#### Scenario: observer du provider
- **WHEN** les programmes du TV Provider changent pendant que le launcher est au premier plan
- **THEN** un `ContentObserver` sur `WatchNextPrograms.CONTENT_URI` déclenche le rechargement, protégé contre `SecurityException` et l'absence du provider

#### Scenario: relecture périodique et arrière-plan
- **WHEN** la périodicité de relecture est atteinte sans notification du provider
- **THEN** la rangée relit sa source au premier plan seulement ; le retour au premier plan vérifie les échéances avant d'afficher un ancien résultat

## ADDED Requirements

### Requirement: Images d'À suivre mutualisées
À suivre SHALL utiliser le contrat « Chargement partagé des images », la protection des fournisseurs, le cache et la validité des programmes de launcher-shell. La politique « Carte Up Next » SHALL s'appliquer aux dimensions communes sans hériter du verdict de qualité d'une autre zone.

#### Scenario: même image dans plusieurs zones
- **WHEN** le héro ou le Top Shelf et une carte demandent simultanément la même version d'image publiée
- **THEN** la récupération et l'analyse des dimensions sont partagées ; chaque zone applique sa politique et décode à sa taille sans nouvelle ouverture du fournisseur

#### Scenario: candidates alternatives
- **WHEN** l'image préférée échoue ou ne satisfait pas la carte, mais l'autre candidate publiée est exploitable
- **THEN** la carte utilise cette candidate selon le budget partagé, avant de rester sur le placeholder ; aucune image d'un autre film n'est substituée
