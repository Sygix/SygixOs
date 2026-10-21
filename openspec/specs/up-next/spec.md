# up-next Specification

## Purpose
Rangée Up Next de l'écran d'accueil : agrégation des programmes watch next publiés par toutes les apps dans le TV Provider Android, dédoublonnage, tri (contenus ouverts depuis SygixOs en tête), cartes, ouverture, menu « Ouvrir avec… » et contenus retirés.

## Requirements

### Requirement: Source multi-apps
La rangée SHALL agréger les `WatchNextPrograms` de toutes les apps visibles du TV Provider sous `READ_TV_LISTINGS`, filtrés sur `COLUMN_BROWSABLE`, sans app privilégiée ni lecture de `PreviewPrograms`. Les filtres SHALL suivre settings (« Apps sources », « Cacher une application ») et launcher-shell (« Sélection des apps sources »).

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
La rangée SHALL manipuler un modèle canonique `UpNextItem` indépendant de l'app d'origine, avec un champ `externalIds` facultatif et la liste des sources du contenu. Le modèle SHALL conserver les candidates affiche et miniature publiées, sans perdre l'alternative lors du choix d'une image.

#### Scenario: constitution
- **WHEN** un programme watch next est converti en item de la rangée
- **THEN** l'item expose le package source, le type (épisode ou film, publié ou inféré selon « Champs facultatifs »), le titre de série, la saison, l'épisode, le titre d'affichage, les candidates affiche et miniature, la progression optionnelle, le type watch next, le timestamp d'activité, l'intent publié et le nom de l'app source ; le champ `externalIds` est présent mais vide en P6

#### Scenario: sources du contenu
- **WHEN** des items sont fusionnés par le dédoublonnage
- **THEN** l'item retenu conserve la liste des sources qui possèdent le contenu (package, nom, icône et intent publié de chacune), après fusion comme avant ; c'est cette liste qu'affiche le menu « Ouvrir avec… »

#### Scenario: extension ultérieure
- **WHEN** un change dédié à BetaSeries apporte des identifiants externes (enrichissement) aux items de toutes les sources
- **THEN** le modèle canonique et la rangée absorbent ces identifiants sans refonte, et le niveau 5 de dédoublonnage s'active quand ils sont connus ; en P6, `externalIds` reste vide et le niveau 5 ne s'applique jamais

### Requirement: Champs facultatifs
Toute colonne watch next SHALL être facultative : absence/vide dégrade sans rejet, sauf tout titre absent ; `package_name` et `_ID` servent de repli. Un épisode sans `COLUMN_TITLE` mais avec `COLUMN_EPISODE_TITLE` SHALL être conservé. `watch_next_type` absent ou inconnu SHALL rejoindre « à suivre » ; `COLUMN_TYPE` absent SHALL être inféré (épisode si saison et épisode, sinon film). Les scénarios ci-dessous SHALL préciser les cas.

#### Scenario: titre absent
- **WHEN** un programme n'a ni titre (`COLUMN_TITLE`) ni, pour un épisode, titre d'épisode (`COLUMN_EPISODE_TITLE`), ou qu'un film n'a pas de titre
- **THEN** il est exclu de la rangée

#### Scenario: épisode sans titre
- **WHEN** un épisode (type publié ou inféré) n'a pas de `COLUMN_TITLE` mais publie un `COLUMN_EPISODE_TITLE`
- **THEN** il est conservé : le titre d'épisode est le seul texte de sa carte, en titre principal, sans ligne « SxxEyy · titre d'épisode », et sert de titre normalisé à chaque niveau de dédoublonnage comme n'importe quel titre

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

**Détails.** — Titres des items : Un item sans `COLUMN_TITLE` SHALL être rejeté sauf épisode publié/inféré ayant `COLUMN_EPISODE_TITLE` : son titre d'épisode SHALL servir à l'affichage et à la normalisation de dédoublonnage. Ni l'un ni l'autre : rejet. Cette règle conserve la décision du 2026-10-10. Cas « titre d'épisode en repli » : quand le titre principal manque, « titre absent » et « épisode sans titre » de « Champs facultatifs » s'appliquent. — Inférence des types Up Next : Un `watch_next_type` absent ou inconnu hors CONTINUE/NEXT/NEW/WATCHLIST SHALL rejoindre NEXT/NEW, après CONTINUE avant WATCHLIST pour tri et gagnant d'un doublon. Sans `COLUMN_TYPE`, le type SHALL être épisode si saison et épisode présents, film sinon, utilisé comme un type publié pour carte et dédoublonnage. Cas « types manquants » : quand les types publiés sont absents ou inconnus, les scénarios de type de « Champs facultatifs » vérifient ces replis sans écarter l'item.

### Requirement: Dédoublonnage en niveaux
La rangée SHALL dédoublonner par 5 niveaux : exact dans une app, une carte par série/app, même épisode inter-apps, même film inter-apps, identifiant externe BetaSeries (prioritaire sur 3/4 si connu, inactif en P6). Aux niveaux 3/4, le type SHALL être `COLUMN_TYPE`, sinon l'inférence de « Champs facultatifs ».

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
- **WHEN** l'année est connue d'un seul côté et le titre normalisé n'est pas associé à plusieurs années connues distinctes
- **THEN** la fusion a lieu sur le titre normalisé seul, limite assumée et documentée

#### Scenario: année inconnue entre plusieurs remakes
- **WHEN** trois apps publient un film de même titre normalisé, avec les années 1982, inconnue et 2011
- **THEN** l'item sans année reste séparé des deux remakes ; aucune fusion ne relie les années connues distinctes, quel que soit l'ordre des programmes

#### Scenario: normalisation stricte
- **WHEN** les titres sont comparés
- **THEN** la comparaison est une égalité stricte après normalisation (minuscules, diacritiques retirés, ponctuation et « (année) » supprimés, espaces compactés), sans correspondance approximative

#### Scenario: limite assumée
- **WHEN** deux apps publient le même contenu sous des titres localisés différents
- **THEN** les items restent en double jusqu'à l'activation du niveau 5 par un change dédié à BetaSeries, et cette limite est documentée

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
Après exclusion des contenus retirés, le tri stable SHALL placer les ouverts SygixOs par ouverture décroissante, puis CONTINUE, NEXT/NEW (type absent compris), WATCHLIST ; dans chaque groupe, activité décroissante puis `_ID` croissant. La limite SHALL être 20 items. « En cours » SHALL désigner CONTINUE, la progression servant seulement à la barre.

#### Scenario: ordre des groupes
- **WHEN** la rangée est affichée et qu'aucun de ses contenus n'a été ouvert depuis SygixOs
- **THEN** les groupes se suivent dans l'ordre `CONTINUE`, puis `NEXT`/`NEW`, puis `WATCHLIST`, chaque groupe trié par engagement décroissant et à égalité par `_ID` croissant, sans dépendre d'une date de sortie absente de la source

#### Scenario: inclusion WATCHLIST
- **WHEN** une app publie des programmes de type `WATCHLIST`
- **THEN** ils apparaissent en dernier groupe de la rangée, triés par engagement décroissant et à égalité par `_ID` croissant, dans la limite des 20 items

#### Scenario: limite
- **WHEN** plus de 20 items subsistent après dédoublonnage, exclusion des contenus retirés et tri
- **THEN** la rangée affiche les 20 premiers selon l'ordre ci-dessus

#### Scenario: contenus ouverts avant les groupes
- **WHEN** deux contenus de la rangée ont été ouverts depuis SygixOs, l'un `WATCHLIST` hier, l'autre `NEXT` il y a une heure, et que d'autres contenus `CONTINUE` n'ont jamais été ouverts depuis SygixOs
- **THEN** la rangée commence par le contenu `NEXT` puis le contenu `WATCHLIST`, puis les autres items dans l'ordre des groupes (`CONTINUE`, `NEXT`/`NEW`, `WATCHLIST`)

### Requirement: Contenus ouverts depuis SygixOs
DataStore SHALL mémoriser la dernière ouverture locale par clé de contenu (titre de série normalisé, ou titre de film et année), enregistrée depuis une carte ou « Ouvrir avec… », jamais depuis le héro, le Top Shelf ou la grille. L'ordre SHALL rester inchangé jusqu'à l'arrière-plan ; au retour, le contenu ouvert revient premier et focusé, rangée au début. Une entrée n'est supprimée qu'après un chargement réussi où plus aucune app ne publie le contenu.

#### Scenario: contenu ouvert passé en tête
- **WHEN** l'utilisateur ouvre depuis la rangée le contenu de la quatrième carte, puis revient dans SygixOs
- **THEN** ce contenu est la première carte de la rangée, sa carte a le focus, la première carte est alignée sur la marge gauche et la position verticale de la zone grille est celle laissée

#### Scenario: épisode suivant publié entre-temps
- **WHEN** l'utilisateur ouvre l'épisode 3 d'une série depuis la rangée et que l'app publie à son retour l'épisode 4 sous un autre programme
- **THEN** la carte de la série (épisode 4) est en première place et reçoit le focus : la mémoire porte sur la série, pas sur le programme

#### Scenario: ouverture depuis le menu
- **WHEN** l'utilisateur ouvre un contenu par une entrée d'app du menu « Ouvrir avec… », que l'intent publié s'ouvre ou que l'app soit lancée en repli
- **THEN** l'ouverture est enregistrée comme depuis la carte, et le contenu passe en tête au retour

#### Scenario: rien ne s'ouvre
- **WHEN** ni l'intent publié ni l'app source ne peuvent s'ouvrir
- **THEN** aucune ouverture n'est enregistrée et l'ordre de la rangée est inchangé

#### Scenario: ordre entre contenus ouverts
- **WHEN** plusieurs contenus de la rangée ont été ouverts depuis SygixOs à des instants différents
- **THEN** ils sont en tête, du plus récemment ouvert au plus ancien, avant tous les contenus jamais ouverts depuis SygixOs

#### Scenario: chargement et premier lancement
- **WHEN** la rangée se charge pour la première fois après un redémarrage du launcher, avec une mémoire déjà persistée
- **THEN** les contenus mémorisés encore publiés sont en tête dès le premier affichage, sans réordonnancement ultérieur ; une mémoire vide ou illisible donne l'ordre des groupes seul, sans erreur

#### Scenario: purge des contenus disparus
- **WHEN** un chargement réussit et qu'un contenu mémorisé n'est plus publié par aucune app
- **THEN** son entrée est supprimée de la mémoire ; s'il est publié de nouveau plus tard, il suit l'ordre des groupes jusqu'à sa prochaine ouverture depuis SygixOs

#### Scenario: pas de purge sans lecture fiable
- **WHEN** le chargement échoue, dépasse le timeout ou que `READ_TV_LISTINGS` est refusée, ou que l'app qui publie un contenu mémorisé est désactivée dans « Apps sources »
- **THEN** la mémoire est conservée intacte

#### Scenario: rafraîchissement
- **WHEN** la rangée se recharge (retour au premier plan, signal du `ContentObserver`)
- **THEN** l'ordre est recalculé avec la mémoire courante, sans changer le focus d'une carte encore présente, hormis le retour qui suit une ouverture depuis la rangée

**Détails.** — Clé des contenus : La clé de contenu SHALL être indépendante de l'app et de l'épisode : titre de série normalisé pour épisode publié/inféré (clé partagée par toute la série), titre normalisé et année connue pour film ; année inconnue d'un côté correspond à toute année selon le niveau 4. Cas « épisode suivant ou autre app » : quand le même contenu change d'épisode ou de source, « épisode suivant publié entre-temps » de « Contenus ouverts depuis SygixOs » et les scénarios de retrait conservent la même identité. — Enregistrement des ouvertures : L'ouverture SHALL être enregistrée quand une carte ou une entrée d'app de son menu lance l'intent publié ou l'app en repli. Aucune ouverture ne SHALL être enregistrée si rien ne s'ouvre, ni depuis héro, Top Shelf ou grille. Cas « lancement ou échec » : quand une action tente d'ouvrir le contenu, « ouverture depuis le menu » et « rien ne s'ouvre » de « Contenus ouverts depuis SygixOs » déterminent l'enregistrement. — Purge des ouvertures : Après lecture réussie, la mémoire SHALL retirer seulement les contenus plus publiés par aucune app du TV Provider. Erreur, timeout, permission manquante ou désactivation d'une app source ne SHALL supprimer aucune entrée. Cas « lecture fiable ou indisponible » : quand la source est rechargée, « purge des contenus disparus » et « pas de purge sans lecture fiable » de « Contenus ouverts depuis SygixOs » s'appliquent.

### Requirement: Contenus retirés d'À suivre
« Retirer d'À suivre » du menu SHALL retirer la clé de contenu : série entière et épisodes présents/futurs, ou film, toutes apps. DataStore SHALL persister titre, type, sources, image et date ; exclusion après dédoublonnage, avant tri/limite. Aucun retrait ne SHALL être purgé automatiquement ; seul « Réafficher » dans Applications cachées de settings permet le retour.

#### Scenario: retrait d'une série
- **WHEN** l'utilisateur choisit « Retirer d'À suivre » sur la carte d'un épisode publié par deux apps
- **THEN** la carte disparaît sans confirmation, aucune carte de cette série n'apparaît plus dans la rangée, quelle que soit l'app ou l'épisode publié ensuite, et le retrait est conservé après redémarrage

#### Scenario: retrait d'un film
- **WHEN** l'utilisateur retire un film de la rangée
- **THEN** ce film n'apparaît plus pour aucune app ; un film de même titre dont l'année connue diffère n'est pas retiré

#### Scenario: place libérée
- **WHEN** la rangée comptait 20 cartes et qu'un contenu est retiré
- **THEN** l'item suivant dans l'ordre de la rangée entre dans la limite au prochain calcul

#### Scenario: focus après retrait
- **WHEN** le contenu de la carte focusée est retiré
- **THEN** le menu se ferme et le focus suit « carte focusée disparue » de launcher-shell (la suivante, sinon la précédente) ; si la rangée devient vide, « rangée devenue vide » s'applique

#### Scenario: contenu retiré qui n'est plus publié
- **WHEN** un contenu retiré n'est plus publié par aucune app
- **THEN** il reste dans l'ensemble des contenus retirés et dans la liste « Retirés d'À suivre » des réglages

#### Scenario: réaffichage
- **WHEN** l'utilisateur choisit « Réafficher » pour ce contenu dans « Applications cachées »
- **THEN** il revient dans la rangée au chargement suivant s'il est encore publié, à la place que lui donne l'ordre de la rangée

### Requirement: Position de la rangée
Up Next SHALL être une ligne tvOS de la grille sous « À suivre », avant les apps par défaut ou après selon settings. Elle ne SHALL pas créer de palier ni de changement de fond : la grille masque le héro. Sections et D-pad SHALL suivre « Grille d'apps » et « Navigation 3 paliers » de launcher-shell.

#### Scenario: position par défaut
- **WHEN** la zone grille prend le focus et que le réglage vaut « Avant les applications »
- **THEN** la rangée Up Next, sous son titre « À suivre », est la première ligne de la zone grille, au-dessus du titre « Applications » et de la première rangée d'apps, sur le fond de la zone grille

#### Scenario: position réglée après les applications
- **WHEN** le réglage « Position d'À suivre » vaut « Après les applications »
- **THEN** la rangée, sous son titre « À suivre », est la dernière ligne de la zone grille, sous la dernière rangée d'apps, sur le même fond

#### Scenario: mode déplacement
- **WHEN** le mode « Déplacer » d'une tuile d'app est actif (« Persistance de la grille » de launcher-shell)
- **THEN** le déplacement ne concerne que les tuiles d'apps : la rangée Up Next et ses cartes ne sont ni déplaçables ni une cible du déplacement, et le menu « Ouvrir avec… » d'une carte n'offre pas « Déplacer »

### Requirement: Indépendance avec le héro
La rangée Up Next SHALL être affichée sans dédoublonnage par rapport au héro : le héro peut montrer les mêmes contenus.

#### Scenario: contenu commun
- **WHEN** un contenu apparaît à la fois dans le héro et dans la rangée Up Next
- **THEN** les deux zones l'affichent, sans filtrage entre elles

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
- **WHEN** toutes les candidates et copies locales utilisables manquent, échouent, expirent, sont trop petites ou sont des images génériques (« Images génériques des applications » de launcher-shell)
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

### Requirement: Ouverture d'un item
L'appui sur une carte SHALL ouvrir l'intent publié par le programme de la rangée (`COLUMN_INTENT_URI`) ; à défaut, ou en cas d'échec d'ouverture, l'app source SHALL être lancée.

#### Scenario: intent disponible
- **WHEN** l'utilisateur valide une carte dont le programme publie un intent
- **THEN** cet intent est ouvert (l'app affiche sa fiche ou reprend selon son propre comportement ; le verbe est « ouvrir », pas « lire ») et l'ouverture est inscrite selon « Contenus ouverts depuis SygixOs »

#### Scenario: intent absent ou en échec
- **WHEN** le programme ne publie pas d'intent, ou que son ouverture échoue (activity absente, exception de sécurité)
- **THEN** l'app source est lancée, sans crash

### Requirement: Menu « Ouvrir avec… »
L'appui long SHALL ouvrir « Ouvrir avec… », même pour une seule source, listant les apps via leur intent puis « Retirer d'À suivre ». Aucun choix d'app ne SHALL être persisté ; seule l'ouverture suit sa mémoire. Rendu et entrées SHALL suivre la maquette 7.6, le composant « Menu Ouvrir avec » et la lisibilité du menu de « Grille d'apps ».

#### Scenario: ouverture du menu
- **WHEN** l'utilisateur fait un appui long sur une carte
- **THEN** le menu « Ouvrir avec… » liste les sources du contenu conservées par l'item, dans l'ordre de cette liste, chacune avec son icône et son nom, puis l'entrée « Retirer d'À suivre », sous un en-tête qui montre la vignette du programme, le titre du contenu et le sous-titre « Ouvrir avec… »

#### Scenario: rendu des entrées
- **WHEN** le menu est ouvert
- **THEN** l'entrée focusée est une pilule claire à texte sombre, sans zoom ni halo, les autres entrées n'ont pas de fond, et le panneau est le même verre sombre que le menu contextuel des tuiles, au-dessus d'un voile sur l'accueil

#### Scenario: focus initial dans le menu
- **WHEN** le menu s'ouvre
- **THEN** le focus est sur la première entrée d'app, haut et bas parcourent les entrées d'apps puis « Retirer d'À suivre », et les déplacements restent dans le menu sans boucle aux bords

#### Scenario: choix d'une app
- **WHEN** l'utilisateur valide une entrée du menu
- **THEN** l'app choisie est ouverte via l'intent qu'elle publie, le menu se ferme et le focus revient sur la carte

#### Scenario: fermeture et focus
- **WHEN** l'utilisateur presse Retour pendant que le menu est ouvert
- **THEN** le menu se ferme sans ouvrir d'app et le focus revient sur la carte

#### Scenario: source unique
- **WHEN** une seule app possède le contenu de la carte
- **THEN** le menu s'ouvre quand même, avec cette seule entrée d'app suivie de « Retirer d'À suivre »

#### Scenario: retirer d'À suivre
- **WHEN** l'utilisateur valide « Retirer d'À suivre »
- **THEN** le contenu est retiré selon « Contenus retirés d'À suivre », sans confirmation ni toast, le menu se ferme et aucune app n'est ouverte

#### Scenario: intent en échec depuis le menu
- **WHEN** l'ouverture de l'intent d'une source choisie dans le menu échoue
- **THEN** l'app source est lancée en repli et un toast « Contenu indisponible, ouverture de <nom de l'app> » en informe l'utilisateur, sans crash

**Détails.** — Rendu du menu des sources : Le menu SHALL être centré sous voile `SygixColors.Scrim`, verre `GlassLook.Menu`, largeur `Dimens.MenuWidth`, coins `Dimens.MenuCorner`, padding `Dimens.MenuPadding`. Son en-tête SHALL montrer vignette 16:9 `Dimens.MenuThumbnailWidth` avec image sinon placeholder, titre `TextStyles.MenuTitle`, sous-titre « Ouvrir avec… » en `TextStyles.MenuSubtitle` / `SygixColors.MenuSubtitle`. Cas « contenu du panneau » : quand le menu est ouvert, « ouverture du menu » et « rendu des entrées » de « Menu « Ouvrir avec… » » s'appliquent avec le verre du menu des tuiles. — Entrées du menu des sources : Les sources SHALL avoir pilule `Dimens.PillCorner`, minimum `Dimens.MenuActionHeight`, retrait `Dimens.MenuEntryPadding`, vraie icône `Dimens.MenuEntryIcon` aux coins `Dimens.MenuEntryIconCorner`, écart `Dimens.MenuEntryGap`, nom `TextStyles.Row` / `RowFocused` au focus. Retirer SHALL suivre la dernière app sans séparateur, même pilule, œil barré de `Dimens.MenuEntryIcon` dans la couleur du texte. Cas « entrée de retrait et sources » : quand le menu liste ses actions, « rendu des entrées » et « retirer d'À suivre » de « Menu « Ouvrir avec… » » gardent la même présentation que la maquette 7.6.

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

### Requirement: Images d'À suivre mutualisées
À suivre SHALL utiliser le contrat « Chargement partagé des images », la protection des fournisseurs, le cache et la validité des programmes de launcher-shell. La politique « Carte Up Next » SHALL s'appliquer aux dimensions communes sans hériter du verdict de qualité d'une autre zone.

#### Scenario: même image dans plusieurs zones
- **WHEN** le héro ou le Top Shelf et une carte demandent simultanément la même version d'image publiée
- **THEN** la récupération et l'analyse des dimensions sont partagées ; chaque zone applique sa politique et décode à sa taille sans nouvelle ouverture du fournisseur

#### Scenario: candidates alternatives
- **WHEN** l'image préférée échoue ou ne satisfait pas la carte, mais l'autre candidate publiée est exploitable
- **THEN** la carte utilise cette candidate selon le contrat partagé, avant de rester sur le placeholder ; aucune image d'un autre film n'est substituée

#### Scenario: progression mise à jour
- **WHEN** l'app met à jour la position de lecture d'un item, ou le republie identique sous un nouvel identifiant de ligne
- **THEN** la carte garde son image affichée sans la recharger ni repasser par le placeholder ; seule sa barre de progression change

#### Scenario: image d'une seule carte mise à jour
- **WHEN** l'image d'une carte change de version ou expire
- **THEN** seule cette carte change d'image, quand la nouvelle est prête ou vers son placeholder ; les autres cartes ne sont ni rechargées ni redessinées
