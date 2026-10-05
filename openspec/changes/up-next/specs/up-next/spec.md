# Delta up-next

## Purpose
Rangée Up Next de l'écran d'accueil : agrégation des programmes watch next publiés par toutes les apps dans le TV Provider Android, dédoublonnage, tri, cartes, ouverture et menu « Ouvrir avec… ».

## ADDED Requirements

### Requirement: Source multi-apps
La rangée Up Next SHALL agréger les programmes `WatchNextPrograms` de toutes les apps visibles dans le TV Provider Android, lues sous `READ_TV_LISTINGS` et filtrées sur `COLUMN_BROWSABLE`, sans limiter aux programmes d'une app en particulier. Seuls les `WatchNextPrograms` alimentent la rangée : les `PreviewPrograms` ne sont pas lus. Les filtres des réglages (apps sources désactivées, apps cachées) s'appliquent tels que définis par la capability settings (« Apps sources », « Cacher une application ») et par launcher-shell (« Sélection des apps sources »).

#### Scenario: agrégation
- **WHEN** plusieurs apps publient des programmes watch next
- **THEN** la rangée affiche les items de toutes les sources visibles, chaque carte portant l'app qui a publié le programme retenu

#### Scenario: accès aux données
- **WHEN** la rangée est chargée
- **THEN** la requête `ContentResolver` s'exécute hors du thread principal avec un timeout borné, et la source renvoie un résultat qui distingue une erreur d'un résultat vide

#### Scenario: filtres des réglages
- **WHEN** une app est désactivée dans « Apps sources » ou cachée de la grille
- **THEN** la rangée applique les exigences « Apps sources » et « Cacher une application » de la capability settings, avant le dédoublonnage, sans redémarrage

#### Scenario: types non pris en charge
- **WHEN** un programme publie un `COLUMN_TYPE` qui n'est ni un épisode ni un film (clip, extrait, autre)
- **THEN** il est exclu de la rangée ; un programme sans `COLUMN_TYPE` n'est pas concerné et suit l'exigence « Champs facultatifs »

### Requirement: Modèle canonique
La rangée SHALL manipuler un modèle canonique `UpNextItem` indépendant de l'app d'origine, avec un champ `externalIds` facultatif et la liste des sources du contenu.

#### Scenario: constitution
- **WHEN** un programme watch next est converti en item de la rangée
- **THEN** l'item expose le package source, le type (épisode ou film, publié ou inféré selon « Champs facultatifs »), le titre de série, la saison, l'épisode, le titre d'affichage, le poster, la progression optionnelle, le type watch next, le timestamp d'activité, l'intent publié et le nom de l'app source ; le champ `externalIds` est présent mais vide en P6

#### Scenario: sources du contenu
- **WHEN** des items sont fusionnés par le dédoublonnage
- **THEN** l'item retenu conserve la liste des sources qui possèdent le contenu (package, nom, icône et intent publié de chacune), après fusion comme avant ; c'est cette liste qu'affiche le menu « Ouvrir avec… »

#### Scenario: extension P8
- **WHEN** l'enrichissement BetaSeries (P8) ajoute des IDs externes (IMDb, TVDB) aux items de toutes les sources
- **THEN** le modèle canonique et la rangée absorbent ces IDs sans refonte, et le niveau 5 de dédoublonnage s'active quand il est connu ; BetaSeries reste une étape d'enrichissement et non la source unique de la rangée

### Requirement: Champs facultatifs
Toute colonne d'un programme watch next SHALL être traitée comme facultative : un champ absent ou vide dégrade l'item sans le rejeter, sauf l'absence de tout titre ; `package_name` et `_ID` sont fournis par le provider et servent de repli. Un programme sans `watch_next_type` SHALL être rattaché au groupe « à suivre » (`NEXT`/`NEW`), après les reprises (`CONTINUE`) et avant `WATCHLIST`, pour le tri comme pour le gagnant d'un doublon. Une valeur de `watch_next_type` inconnue (hors des quatre valeurs définies par Android : `CONTINUE`, `NEXT`, `NEW`, `WATCHLIST`) SHALL être traitée comme une valeur absente, donc rattachée au groupe « à suivre ». Un programme sans `COLUMN_TYPE` SHALL recevoir un type inféré : épisode si le numéro de saison et le numéro d'épisode sont présents, film sinon ; ce type inféré sert au dédoublonnage et à la carte comme un type publié.

#### Scenario: titre absent
- **WHEN** un programme n'a ni titre ni titre de série
- **THEN** il est exclu de la rangée

#### Scenario: titre de série absent
- **WHEN** un épisode n'a pas de titre de série mais un titre
- **THEN** le titre sert de titre d'affichage et de clé de titre normalisé, sans « SxxEyy » si les numéros manquent

#### Scenario: type watch next absent
- **WHEN** un programme n'a pas de `watch_next_type`
- **THEN** il est rattaché au groupe « à suivre » (`NEXT`/`NEW`) : la rangée le place après les `CONTINUE` et avant les `WATCHLIST`, et dans un doublon il perd contre un `CONTINUE` et gagne contre un `WATCHLIST`

#### Scenario: type watch next inconnu
- **WHEN** un programme publie une valeur de `watch_next_type` hors de `CONTINUE`, `NEXT`, `NEW` et `WATCHLIST`
- **THEN** il est traité comme un programme sans `watch_next_type` : rattaché au groupe « à suivre » (`NEXT`/`NEW`) pour le tri comme pour le gagnant d'un doublon, sans être exclu de la rangée

#### Scenario: type absent avec saison et épisode
- **WHEN** un programme n'a pas de `COLUMN_TYPE` mais publie un numéro de saison et un numéro d'épisode
- **THEN** il est traité comme un épisode : niveaux 2 et 3 de dédoublonnage, carte épisode avec « SxxEyy »

#### Scenario: type absent sans saison ou sans épisode
- **WHEN** un programme n'a pas de `COLUMN_TYPE` et qu'il lui manque le numéro de saison, le numéro d'épisode ou les deux
- **THEN** il est traité comme un film : niveau 4 de dédoublonnage, carte film (titre seul)

#### Scenario: numéros de saison ou d'épisode absents
- **WHEN** un programme publié comme épisode (`COLUMN_TYPE`) n'a pas de numéro de saison ou d'épisode
- **THEN** la carte omet « SxxEyy » ; le niveau 3 de dédoublonnage ne s'applique pas à cet item (pas de fusion entre apps sans numéros), le niveau 2 reste appliqué

#### Scenario: identifiants absents
- **WHEN** un programme n'a ni `internal_provider_id`, ni `content_id`, ni `intent_uri`
- **THEN** la clé du niveau 1 est `package_name` + `_ID` : seule la ligne elle-même est dédoublonnée

#### Scenario: année absente
- **WHEN** un film n'a pas de `release_date`
- **THEN** le niveau 4 de dédoublonnage suit le scénario « année inconnue »

#### Scenario: position ou durée absentes
- **WHEN** la position de lecture ou la durée manque
- **THEN** l'item n'a pas de progression : aucune barre n'est affichée, même pour un `CONTINUE`, et son groupe de tri ne change pas

#### Scenario: engagement absent
- **WHEN** `last_engagement_time` manque
- **THEN** l'item est traité comme le plus ancien de son groupe (engagement nul) et départagé par `_ID` croissant

#### Scenario: image absente
- **WHEN** ni poster ni miniature ne sont publiés, ou que l'image ne se charge pas
- **THEN** la carte affiche le placeholder (scénario « badge et image »)

#### Scenario: intent absent
- **WHEN** `intent_uri` manque
- **THEN** l'ouverture lance l'app source (scénario « intent absent ou en échec ») et l'entrée du menu « Ouvrir avec… » fait de même

#### Scenario: browsable absent
- **WHEN** `COLUMN_BROWSABLE` manque
- **THEN** le programme est considéré comme browsable, comme dans le héro

### Requirement: Dédoublonnage en niveaux
La rangée SHALL dédoublonner les items par une clé d'identité en 5 niveaux, du plus fiable au moins fiable : (1) doublons exacts dans une app ; (2) au plus une carte par série dans une app ; (3) même épisode entre apps ; (4) même film entre apps ; (5) en P8, ID IMDb/TVDB prenant le pas sur les niveaux 3 et 4 quand il est connu. Aux niveaux 3 et 4, le type (épisode ou film) est le `COLUMN_TYPE` publié, sinon le type inféré défini par « Champs facultatifs ».

#### Scenario: doublons dans une app
- **WHEN** une app publie le même contenu plusieurs fois
- **THEN** un seul item est retenu, identifié par `package_name` + `internal_provider_id`, sinon `content_id`, sinon `intent_uri`

#### Scenario: série dans une app
- **WHEN** une app publie à la fois l'épisode en cours d'une série et son épisode suivant
- **THEN** une seule carte est affichée pour la série et l'épisode en cours prime

#### Scenario: épisode entre apps
- **WHEN** deux apps publient le même épisode (titre de série normalisé + saison + épisode), que le type soit publié ou inféré
- **THEN** une seule carte est retenue

#### Scenario: film entre apps
- **WHEN** deux apps publient un film de même titre normalisé et de même année, que le type soit publié ou inféré
- **THEN** une seule carte est retenue

#### Scenario: remake
- **WHEN** deux apps publient un film de même titre normalisé et que les deux années sont connues et différentes
- **THEN** aucune fusion n'a lieu

#### Scenario: types différents
- **WHEN** deux apps publient un même titre normalisé, l'un comme film et l'autre comme épisode (type publié ou inféré)
- **THEN** aucune fusion n'a lieu

#### Scenario: année inconnue
- **WHEN** l'année est connue d'un seul côté
- **THEN** la fusion a lieu sur le titre normalisé seul, limite assumée et documentée

#### Scenario: normalisation stricte
- **WHEN** les titres sont comparés
- **THEN** la comparaison est une égalité stricte après normalisation (minuscules, diacritiques retirés, ponctuation et « (année) » supprimés, espaces compactés), sans correspondance approximative

#### Scenario: limite assumée
- **WHEN** deux apps publient le même contenu sous des titres localisés différents
- **THEN** les items restent en double jusqu'à l'activation du niveau 5 en P8, et cette limite est documentée

### Requirement: Gagnant d'un doublon
Quand plusieurs items partagent une même clé, la carte retenue SHALL être déterminée dans cet ordre : `CONTINUE` bat le groupe « à suivre » (`NEXT`/`NEW`, et programmes sans `watch_next_type` selon « Champs facultatifs »), qui bat `WATCHLIST` ; ensuite l'engagement le plus récent ; à égalité, l'ordre de préférence des apps (constante, Jellyfin d'abord).

#### Scenario: reprise en cours
- **WHEN** un même contenu existe à la fois comme en cours (`CONTINUE`) et comme à suivre (`NEXT`/`NEW`) ou mis de côté (`WATCHLIST`)
- **THEN** l'item en cours est retenu, avec sa progression et sa date d'activité

#### Scenario: à suivre contre mis de côté
- **WHEN** un même contenu existe à la fois comme à suivre (`NEXT`/`NEW`) et comme mis de côté (`WATCHLIST`)
- **THEN** l'item à suivre est retenu

#### Scenario: engagement le plus récent
- **WHEN** les items candidats sont tous du même groupe de type watch next
- **THEN** celui dont le timestamp d'activité est le plus récent est retenu

#### Scenario: égalité
- **WHEN** deux candidats ont le même timestamp d'activité
- **THEN** l'ordre de préférence des apps (constante en P6, Jellyfin d'abord) tranche

### Requirement: Tri et limite
La rangée SHALL être triée de façon déterministe en trois groupes : les items en cours (`CONTINUE`) d'abord, puis les items à suivre (`NEXT`/`NEW`, et programmes sans `watch_next_type` selon « Champs facultatifs »), puis les items `WATCHLIST` ; dans chaque groupe, par timestamp d'activité décroissant et à égalité par `_ID` croissant ; le tri est stable et la rangée est limitée à 20 items. « En cours » désigne le type `CONTINUE` ; la progression ne sert qu'à la barre de la carte.

#### Scenario: ordre des groupes
- **WHEN** la rangée est affichée
- **THEN** les groupes se suivent dans l'ordre `CONTINUE`, puis `NEXT`/`NEW`, puis `WATCHLIST`, chaque groupe trié par engagement décroissant et à égalité par `_ID` croissant, sans dépendre d'une date de sortie absente de la source

#### Scenario: inclusion WATCHLIST
- **WHEN** une app publie des programmes de type `WATCHLIST`
- **THEN** ils apparaissent en dernier groupe de la rangée, triés par engagement décroissant et à égalité par `_ID` croissant, dans la limite des 20 items

#### Scenario: limite
- **WHEN** plus de 20 items subsistent après dédoublonnage et tri
- **THEN** la rangée affiche les 20 premiers selon l'ordre ci-dessus

### Requirement: Position de la rangée
La rangée Up Next SHALL être une ligne style tvOS de la zone grille, première ligne de la zone par défaut (au-dessus de la première rangée d'apps) ou dernière ligne (sous la dernière rangée d'apps) selon le réglage « Position d'Up Next » de la capability settings ; elle n'est pas un palier distinct de launcher-shell : la zone grille masque le héro comme pour les apps, sans changement de fond entre la rangée et les apps. La navigation D-pad de la zone grille, rangée Up Next incluse, est spécifiée par « Navigation 3 paliers » de launcher-shell.

#### Scenario: position par défaut
- **WHEN** la zone grille prend le focus et que le réglage vaut « avant la grille »
- **THEN** la rangée Up Next est la première ligne de la zone grille, au-dessus de la première rangée d'apps, sur le fond de la zone grille

#### Scenario: position réglée après la grille
- **WHEN** le réglage « Position d'Up Next » vaut « après la grille »
- **THEN** la rangée est la dernière ligne de la zone grille, sous la dernière rangée d'apps, sur le même fond

### Requirement: Indépendance avec le héro
La rangée Up Next SHALL être affichée sans dédoublonnage par rapport au héro : le héro peut montrer les mêmes contenus.

#### Scenario: contenu commun
- **WHEN** un contenu apparaît à la fois dans le héro et dans la rangée Up Next
- **THEN** les deux zones l'affichent, sans filtrage entre elles

### Requirement: Carte Up Next
Chaque carte Up Next SHALL être au format 16:9 uniforme affichant l'image du programme en 16:9 plein cadre quand elle est exploitable, sinon le poster portrait centré sur fond sombre, avec le texte de série (« SxxEyy » + titre d'épisode ; films : titre seul), une barre de progression pour les items en cours seulement, un placeholder si l'image manque ou est trop petite, et un petit badge avec l'icône de l'app source prise dans le `PackageManager`. Le seuil de qualité des visuels est propre aux cartes : la règle « ≥ 1080 px » de « Qualité des visuels » (launcher-shell) ne s'applique pas.

#### Scenario: carte épisode
- **WHEN** l'item est un épisode (type publié ou inféré selon « Champs facultatifs »)
- **THEN** la carte affiche l'image 16:9 plein cadre (sinon le poster portrait centré sur fond sombre), le titre de série, « SxxEyy », le titre d'épisode et, pour un item en cours, la barre de progression

#### Scenario: carte film
- **WHEN** l'item est un film (type publié ou inféré selon « Champs facultatifs »)
- **THEN** la carte affiche l'image et le titre du film

#### Scenario: badge et image
- **WHEN** une carte est affichée
- **THEN** le badge est l'icône réelle de l'app source obtenue du `PackageManager` (aucun logo embarqué), et un placeholder remplace l'image si celle-ci manque ou échoue

#### Scenario: image trop petite
- **WHEN** l'image décodée d'une carte fait moins de deux fois la largeur affichée de la carte
- **THEN** la carte reste dans la rangée avec le placeholder à la place de l'image ; l'item n'est pas écarté

#### Scenario: focus
- **WHEN** une carte prend le focus
- **THEN** le comportement visuel suit l'exigence « Focus tvOS » de launcher-shell

### Requirement: Ouverture d'un item
L'appui sur une carte SHALL ouvrir l'intent publié par le programme de la rangée (`COLUMN_INTENT_URI`) ; à défaut, ou en cas d'échec d'ouverture, l'app source SHALL être lancée.

#### Scenario: intent disponible
- **WHEN** l'utilisateur valide une carte dont le programme publie un intent
- **THEN** cet intent est ouvert (l'app affiche sa fiche ou reprend selon son propre comportement ; le verbe est « ouvrir », pas « lire »)

#### Scenario: intent absent ou en échec
- **WHEN** le programme ne publie pas d'intent, ou que son ouverture échoue (activity absente, exception de sécurité)
- **THEN** l'app source est lancée, sans crash

### Requirement: Menu « Ouvrir avec… »
L'appui long sur une carte SHALL ouvrir un menu « Ouvrir avec… » listant les apps qui possèdent ce contenu, chacune ouverte via l'intent qu'elle publie ; le menu s'ouvre même quand une seule app possède le contenu ; rien n'est persisté.

#### Scenario: ouverture du menu
- **WHEN** l'utilisateur fait un appui long sur une carte
- **THEN** le menu « Ouvrir avec… » liste les sources du contenu conservées par l'item, avec leur nom et leur icône

#### Scenario: focus initial dans le menu
- **WHEN** le menu s'ouvre
- **THEN** le focus est sur la première entrée, et les déplacements restent dans le menu sans boucle aux bords

#### Scenario: choix d'une app
- **WHEN** l'utilisateur valide une entrée du menu
- **THEN** l'app choisie est ouverte via l'intent qu'elle publie, le menu se ferme et le focus revient sur la carte

#### Scenario: fermeture et focus
- **WHEN** l'utilisateur presse Retour pendant que le menu est ouvert
- **THEN** le menu se ferme sans ouvrir d'app et le focus revient sur la carte

#### Scenario: source unique
- **WHEN** une seule app possède le contenu de la carte
- **THEN** le menu s'ouvre quand même, avec cette seule entrée

#### Scenario: intent en échec depuis le menu
- **WHEN** l'ouverture de l'intent d'une source choisie dans le menu échoue
- **THEN** l'app source est lancée en repli et un toast en informe l'utilisateur, sans crash

### Requirement: États de la rangée
La rangée SHALL couvrir les états chargement, erreur, vide et permission refusée, sans jamais avaler une erreur ni bloquer le reste du home. Le comportement du focus quand la rangée se masque est spécifié par « Navigation 3 paliers » de launcher-shell (scénario « rangée devenue vide »).

#### Scenario: permission refusée
- **WHEN** `READ_TV_LISTINGS` n'est pas accordée
- **THEN** la rangée est masquée, sans carte d'erreur et sans invitation à configurer quoi que ce soit

#### Scenario: erreur
- **WHEN** la requête vers le TV Provider échoue ou dépasse le timeout au premier chargement
- **THEN** une carte d'état focusable (message + « Réessayer ») remplace la rangée, distincte de l'état vide, sans crash

#### Scenario: carte d'erreur
- **WHEN** la carte « Réessayer » est focusée
- **THEN** elle occupe la place de la rangée dans la navigation de la zone grille, quelle que soit la position réglée (avant ou après la grille), et OK relance la requête

#### Scenario: chargement initial
- **WHEN** le premier chargement dépasse ~300 ms
- **THEN** un squelette de cartes est affiché ; s'il est plus court, aucune étape intermédiaire n'apparaît

#### Scenario: rechargement
- **WHEN** la rangée se recharge alors qu'un contenu est déjà affiché
- **THEN** l'état précédent reste affiché jusqu'au résultat, sans saut de mise en page ni squelette

#### Scenario: erreur au rechargement
- **WHEN** un rechargement échoue alors qu'un contenu est déjà affiché
- **THEN** le contenu précédent reste affiché, sans carte d'erreur ni saut de mise en page

#### Scenario: vide
- **WHEN** aucun item n'est retourné
- **THEN** la rangée est masquée

#### Scenario: non-blocage
- **WHEN** la rangée est dans n'importe quel état
- **THEN** la navigation DPAD du home (héro, dock, grille) reste fonctionnelle

### Requirement: Rafraîchissement
La rangée SHALL se recharger quand le launcher revient au premier plan, que `READ_TV_LISTINGS` vient d'être accordée ou que le TV Provider signale un changement.

#### Scenario: retour au premier plan
- **WHEN** le launcher revient au premier plan (ou que la permission vient d'être accordée)
- **THEN** la rangée se recharge, le contenu précédent reste affiché pendant ce temps

#### Scenario: observer du provider
- **WHEN** les programmes du TV Provider changent pendant que le launcher est au premier plan
- **THEN** un `ContentObserver` sur `WatchNextPrograms.CONTENT_URI` déclenche le rechargement, protégé contre `SecurityException` et l'absence du provider
