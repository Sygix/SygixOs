# Delta up-next

## ADDED Requirements

### Requirement: Source multi-apps
La rangée Up Next SHALL agréger les programmes watch next (`WatchNextPrograms`, complétés de `PreviewPrograms`) de toutes les apps visibles dans le TV Provider Android, lues sous `READ_TV_LISTINGS`, sans limiter aux programmes d'une app en particulier.

#### Scenario: agrégation
- **WHEN** plusieurs apps publient des programmes watch next
- **THEN** la rangée affiche les items de toutes les sources visibles, chaque carte portant l'app qui a publié le programme retenu

#### Scenario: accès aux données
- **WHEN** la rangée est chargée
- **THEN** la requête `ContentResolver` s'exécute hors du thread principal avec un timeout borné, et la source renvoie un résultat qui distingue une erreur d'un résultat vide

#### Scenario: hypothèse de visibilité
- **WHEN** l'implémentation démarre
- **THEN** la visibilité réelle des lignes des autres apps sous `READ_TV_LISTINGS` a été vérifiée sur l'appareil (colonnes et packages visibles) avant toute utilisation d'une colonne ; toute colonne non confirmée est marquée comme hypothèse

### Requirement: Modèle canonique
La rangée SHALL manipuler un modèle canonique `UpNextItem` indépendant de l'app d'origine, avec un champ `externalIds` facultatif.

#### Scenario: constitution
- **WHEN** un programme watch next est converti en item de la rangée
- **THEN** l'item expose le package source, le type (épisode ou film), le titre de série, la saison, l'épisode, le titre d'affichage, le poster, la progression optionnelle, le type watch next, le timestamp d'activité, l'intent publié et le nom de l'app source ; le champ `externalIds` est présent mais vide en p2c

#### Scenario: extension P4
- **WHEN** l'enrichissement BetaSeries (P4) ajoute des IDs externes (IMDb, TVDB) aux items de toutes les sources
- **THEN** le modèle canonique et la rangée absorbent ces IDs sans refonte, et le niveau 5 de dédoublonnage s'active quand il est connu ; BetaSeries reste une étape d'enrichissement et non la source unique de la rangée

### Requirement: Dédoublonnage en niveaux
La rangée SHALL dédoublonner les items par une clé d'identité en 5 niveaux, du plus fiable au moins fiable : (1) doublons exacts dans une app ; (2) au plus une carte par série dans une app ; (3) même épisode entre apps ; (4) même film entre apps ; (5) en P4, ID IMDb/TVDB prenant le pas sur les niveaux 3 et 4 quand il est connu.

#### Scenario: doublons dans une app
- **WHEN** une app publie le même contenu plusieurs fois (y compris en `PreviewPrograms` et `WatchNextPrograms`)
- **THEN** un seul item est retenu, identifié par `package_name` + `internal_provider_id`, sinon `content_id`, sinon `intent_uri`

#### Scenario: série dans une app
- **WHEN** une app publie à la fois l'épisode en cours d'une série et son épisode suivant
- **THEN** une seule carte est affichée pour la série et l'épisode en cours prime

#### Scenario: épisode et film entre apps
- **WHEN** deux apps publient le même épisode (titre de série normalisé + saison + épisode) ou le même film (titre normalisé + année)
- **THEN** une seule carte est retenue ; pour un film, aucune fusion n'a lieu si les deux années sont connues et différentes (remakes)

#### Scenario: normalisation stricte
- **WHEN** les titres sont comparés
- **THEN** la comparaison est une égalité stricte après normalisation (minuscules, diacritiques retirés, ponctuation et « (année) » supprimés, espaces compactés), sans correspondance approximative

#### Scenario: limite assumée
- **WHEN** deux apps publient le même contenu sous des titres localisés différents
- **THEN** les items restent en double jusqu'à l'activation du niveau 5 en P4, et cette limite est documentée

### Requirement: Gagnant d'un doublon
Quand plusieurs items partagent une même clé, la carte retenue SHALL être déterminée dans cet ordre : une reprise en cours (progression > 0) bat un « à suivre » ; ensuite l'engagement le plus récent ; à égalité, l'ordre de préférence des apps (constante, Jellyfin d'abord).

#### Scenario: reprise en cours
- **WHEN** un même contenu existe à la fois comme en cours et comme à suivre
- **THEN** l'item en cours est retenu, avec sa progression et sa date d'activité

#### Scenario: engagement le plus récent
- **WHEN** les items candidats sont tous du même type de progression
- **THEN** celui dont le timestamp d'activité est le plus récent est retenu

#### Scenario: égalité
- **WHEN** deux candidats ont le même timestamp d'activité
- **THEN** l'ordre de préférence des apps (constante en p2c, Jellyfin d'abord) tranche

### Requirement: Tri et limite
La rangée SHALL être triée de façon déterministe : les items en cours (`CONTINUE`) d'abord par timestamp d'activité décroissant, puis les items à suivre (`NEXT`/`NEW`) par timestamp d'activité décroissant et à égalité par identifiant croissant, avec un tri stable ; les items `WATCHLIST` sont exclus ; la rangée est limitée à 20 items.

#### Scenario: ordre des groupes
- **WHEN** la rangée est affichée
- **THEN** les items en cours précèdent les items à suivre, chaque groupe trié par engagement décroissant, sans dépendre d'une date de sortie absente de la source

#### Scenario: exclusion WATCHLIST
- **WHEN** une app publie des programmes de type `WATCHLIST`
- **THEN** ils n'apparaissent pas dans la rangée

#### Scenario: limite
- **WHEN** plus de 20 items subsistent après dédoublonnage et tri
- **THEN** la rangée affiche les 20 premiers selon l'ordre ci-dessus

### Requirement: Position de la rangée
La rangée Up Next SHALL être une ligne style tvOS en tête de la zone grille, au-dessus des apps, sans nouveau palier de navigation, avec une position réglable dans les réglages (« Position d'Up Next » : avant la grille, par défaut, ou après la grille).

#### Scenario: position par défaut
- **WHEN** la zone grille prend le focus
- **THEN** la rangée Up Next est affichée en tête, au-dessus de la première rangée d'apps

#### Scenario: position réglée après la grille
- **WHEN** le réglage « Position d'Up Next » vaut « après la grille »
- **THEN** la rangée est affichée sous la dernière rangée d'apps, dans la même zone

### Requirement: Indépendance avec le héro
La rangée Up Next SHALL être affichée sans dédoublonnage par rapport au héro : le héro peut montrer les mêmes contenus.

#### Scenario: contenu commun
- **WHEN** un contenu apparaît à la fois dans le héro et dans la rangée Up Next
- **THEN** les deux zones l'affichent, sans filtrage entre elles

### Requirement: Carte Up Next
Chaque carte Up Next SHALL être au format 16:9 uniforme avec le poster portrait centré sur fond sombre, le texte de série (« SxxEyy » + titre d'épisode ; films : titre seul), une barre de progression pour les items en cours seulement, un placeholder si l'image manque, et un petit badge avec l'icône de l'app source prise dans le `PackageManager`.

#### Scenario: carte épisode
- **WHEN** l'item est un épisode
- **THEN** la carte affiche le poster en 16:9, le titre de série, « SxxEyy », le titre d'épisode et, pour un item en cours, la barre de progression

#### Scenario: carte film
- **WHEN** l'item est un film
- **THEN** la carte affiche le poster en 16:9 et le titre du film

#### Scenario: badge et image
- **WHEN** une carte est affichée
- **THEN** le badge est l'icône réelle de l'app source obtenue du `PackageManager` (aucun logo embarqué), et un placeholder remplace le poster si celui-ci manque ou échoue

#### Scenario: focus
- **WHEN** une carte prend le focus
- **THEN** le comportement visuel suit l'exigence « Focus tvOS » de launcher-shell (zoom ~1.1x, ombre douce, easing Apple)

### Requirement: Ouverture d'un item
L'appui sur une carte SHALL ouvrir l'intent publié par le programme de la rangée (`COLUMN_INTENT_URI`) ; à défaut, ou en cas d'échec d'ouverture, l'app source SHALL être lancée.

#### Scenario: intent disponible
- **WHEN** l'utilisateur valide une carte dont le programme publie un intent
- **THEN** cet intent est ouvert (l'app affiche sa fiche ou reprend selon son propre comportement — le verbe est « ouvrir », pas « lire »)

#### Scenario: intent absent ou en échec
- **WHEN** le programme ne publie pas d'intent, ou que son ouverture échoue (activity absente, exception de sécurité)
- **THEN** l'app source est lancée, sans crash

### Requirement: Menu « Ouvrir avec… »
L'appui long sur une carte SHALL ouvrir un menu « Ouvrir avec… » listant les apps qui possèdent ce contenu, chacune ouverte via l'intent qu'elle publie ; rien n'est persisté.

#### Scenario: ouverture du menu
- **WHEN** l'utilisateur fait un appui long sur une carte
- **THEN** le menu « Ouvrir avec… » liste les apps de la clé de dédoublonnage retenue, avec leur nom et leur icône

#### Scenario: choix d'une app
- **WHEN** l'utilisateur valide une entrée du menu
- **THEN** l'app choisie est ouverte via l'intent qu'elle publie, le menu se ferme et le focus revient sur la carte

#### Scenario: fermeture et focus
- **WHEN** l'utilisateur presse Retour pendant que le menu est ouvert
- **THEN** le menu se ferme sans ouvrir d'app et le focus revient sur la carte

#### Scenario: source unique
- **WHEN** une seule app possède le contenu de la carte
- **THEN** le menu réduit à une entrée est affiché

### Requirement: États de la rangée
La rangée SHALL couvrir les états chargement, erreur, vide et permission refusée, sans jamais avaler une erreur ni bloquer le reste du home.

#### Scenario: permission refusée
- **WHEN** `READ_TV_LISTINGS` n'est pas accordée
- **THEN** la rangée est masquée, sans carte d'erreur et sans invitation à configurer quoi que ce soit

#### Scenario: erreur
- **WHEN** la requête vers le TV Provider échoue ou dépasse le timeout
- **THEN** une carte d'état focusable (message + « Réessayer ») remplace la rangée, distincte de l'état vide, sans crash

#### Scenario: chargement initial
- **WHEN** le premier chargement dépasse ~300 ms
- **THEN** un squelette de cartes est affiché ; s'il est plus court, aucune étape intermédiaire n'apparaît

#### Scenario: rechargement
- **WHEN** la rangée se recharge alors qu'un contenu est déjà affiché
- **THEN** l'état précédent reste affiché jusqu'au résultat, sans saut de mise en page ni squelette

#### Scenario: vide
- **WHEN** aucun item n'est retourné
- **THEN** la rangée est masquée

#### Scenario: non-blocage
- **WHEN** la rangée est dans n'importe quel état
- **THEN** la navigation DPAD du home (héro, dock, grille) reste fonctionnelle

### Requirement: Rafraîchissement
La rangée SHALL se recharger quand le launcher revient au premier plan ou que `READ_TV_LISTINGS` vient d'être accordée.

#### Scenario: retour au premier plan
- **WHEN** le launcher revient au premier plan (ou que la permission vient d'être accordée)
- **THEN** la rangée se recharge, le contenu précédent reste affiché pendant ce temps

#### Scenario: limite de fraîcheur
- **WHEN** la rangée est rechargée
- **THEN** elle reflète les données publiées par les apps, qui ne resynchronisent pas en continu (Jellyfin, par exemple, ne resynchronise qu'une fois par heure) — ce délai est documenté et non traité comme une erreur
