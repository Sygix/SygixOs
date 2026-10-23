# Spec Delta

## MODIFIED Requirements

### Requirement: Source multi-apps
La rangée SHALL agréger les `WatchNextPrograms` visibles du TV Provider sous `READ_TV_LISTINGS`, filtrés sur `COLUMN_BROWSABLE`, sans lecture de `PreviewPrograms`, et l'apport du fournisseur de suivi connecté. Les filtres d'apps SHALL suivre settings et launcher-shell ; chaque source SHALL fonctionner indépendamment des permissions et pannes des autres.

#### Scenario: agrégation
- **WHEN** plusieurs apps publient des programmes watch next
- **THEN** la rangée affiche les items de toutes les sources visibles, chaque carte locale portant l'app du programme retenu ; les cartes associées au suivi suivent le badge de « Carte Up Next »

#### Scenario: accès aux données
- **WHEN** la rangée est chargée
- **THEN** la requête `ContentResolver` s'exécute hors du thread principal avec un timeout borné, et chaque source renvoie un résultat distinguant erreur et vide

#### Scenario: filtres des réglages
- **WHEN** une app est désactivée dans « Apps sources » ou cachée de la grille
- **THEN** sa contribution suit les exigences « Apps sources » et « Cacher une application » de settings avant dédoublonnage ; cela ne déconnecte pas le fournisseur de suivi et ne retire pas ses contenus propres

#### Scenario: types non pris en charge
- **WHEN** un programme publie un `COLUMN_TYPE` qui n'est ni un épisode ni un film (clip, extrait, autre)
- **THEN** il est exclu ; un programme sans `COLUMN_TYPE` suit « Champs facultatifs »

#### Scenario: apport du compte
- **WHEN** le fournisseur de suivi renvoie des contenus à regarder absents des programmes locaux
- **THEN** ils rejoignent la même rangée, selon `watch-tracking` et le contrat du fournisseur, sans nécessiter `READ_TV_LISTINGS` ni une offre de lecture connue

### Requirement: Modèle canonique
La rangée SHALL manipuler un contenu canonique indépendant de l'app, avec identifiants externes facultatifs et sources conservées. Origine du suivi, identité de série ou film, épisode retenu et application de lecture SHALL être distincts ; les candidates affiche et miniature SHALL être conservées sans perdre l'alternative.

#### Scenario: constitution
- **WHEN** un programme watch next est converti en item
- **THEN** il conserve package, type publié ou inféré, série, saison, épisode, titre, images candidates, progression optionnelle, type watch next, activité, intent et nom d'app ; les identifiants externes ne sont ajoutés que s'ils sont vérifiés

#### Scenario: sources du contenu
- **WHEN** des items sont fusionnés
- **THEN** toutes les sources locales du contenu sont conservées avec package, nom, icône et intent ; la source de suivi n'est pas présentée comme une application capable de lire la vidéo

#### Scenario: extension ultérieure
- **WHEN** un fournisseur apporte des identifiants externes vérifiés
- **THEN** ils enrichissent l'identité et le dédoublonnage sans dépendre d'un package Android fictif ; leur absence laisse les replis locaux utilisables

### Requirement: Dédoublonnage en niveaux
La rangée SHALL conserver les niveaux exact dans une app, série/app, épisode inter-apps et film inter-apps, et activer l'identité externe vérifiée prioritaire. Pour une série associée au suivi, elle SHALL afficher une seule carte toutes sources confondues, avec la résolution d'épisode de « Gagnant d'un doublon ». Sans identité externe, les règles locales restent applicables.

#### Scenario: doublons dans une app
- **WHEN** une app publie le même contenu plusieurs fois
- **THEN** un seul item est retenu par package et identifiant fournisseur, sinon contenu, sinon intent

#### Scenario: série dans une app
- **WHEN** une app publie à la fois l'épisode en cours et le suivant
- **THEN** une seule carte est affichée pour la série et l'épisode en cours non déjà vu prime

#### Scenario: épisode entre apps
- **WHEN** deux apps publient le même épisode (titre de série normalisé + saison + épisode), type publié ou inféré
- **THEN** une seule carte est retenue

#### Scenario: film entre apps
- **WHEN** deux apps publient un film de même titre normalisé et même année, type publié ou inféré
- **THEN** une seule carte est retenue

#### Scenario: remake
- **WHEN** deux films de même titre ont des années connues différentes
- **THEN** ils ne sont pas fusionnés sur le titre ; des identifiants externes distincts restent distincts

#### Scenario: types différents
- **WHEN** un titre est publié comme film et comme épisode
- **THEN** aucune fusion entre ces types n'a lieu

#### Scenario: année inconnue
- **WHEN** l'année n'est connue que d'un côté et que le titre normalisé n'est associé qu'à une seule année connue
- **THEN** la fusion locale a lieu sur le titre seul, limite assumée et documentée

#### Scenario: année inconnue entre plusieurs remakes
- **WHEN** trois apps publient un même titre avec années 1982, inconnue et 2011
- **THEN** l'item sans année reste séparé et aucune fusion ne relie les années distinctes, quel que soit l'ordre

#### Scenario: normalisation stricte
- **WHEN** les titres sont comparés sans identité externe
- **THEN** l'égalité est stricte après minuscules, retrait des diacritiques, ponctuation et « (année) », puis compactage des espaces, sans rapprochement approximatif

#### Scenario: limite assumée
- **WHEN** des titres localisés différents ne peuvent pas être associés à une identité externe certaine
- **THEN** les cartes restent séparées plutôt que fusionnées à tort ; aucun statut n'est écrit sur une identité incertaine

#### Scenario: série identifiée entre fournisseurs
- **WHEN** plusieurs apps et le compte décrivent des épisodes différents de la même série identifiée
- **THEN** une seule carte de cette série est retenue et les possibilités d'ouverture sont conservées sans attribuer l'intent d'un épisode à un autre

### Requirement: Gagnant d'un doublon
Une reprise non vue SHALL primer sur le prochain épisode du compte ; une reprise déjà vue SHALL être écartée au profit du prochain non vu diffusé. Entre reprises admissibles, activité récente puis préférence locale existante SHALL trancher. Sans suivi identifié, l'ordre SHALL rester CONTINUE, NEXT/NEW ou absent, WATCHLIST, puis activité et préférence d'app.

#### Scenario: reprise en cours
- **WHEN** un contenu existe en CONTINUE et dans un groupe à suivre ou mis de côté
- **THEN** la reprise admissible est retenue avec sa progression et sa date d'activité

#### Scenario: à suivre contre mis de côté
- **WHEN** un même contenu existe comme à suivre et WATCHLIST sans reprise
- **THEN** l'item à suivre est retenu

#### Scenario: engagement le plus récent
- **WHEN** les candidats sont du même groupe admissible
- **THEN** l'activité la plus récente gagne

#### Scenario: égalité
- **WHEN** deux candidats locaux ont le même timestamp d'activité
- **THEN** la préférence locale existante (Jellyfin d'abord) tranche ; cette règle ne constitue pas une préférence de lecture imposée au premier choix du suivi

#### Scenario: épisode local en avance
- **WHEN** BetaSeries propose S02E03 et qu'une app propose une reprise non vue de S02E05
- **THEN** S02E05 est affiché sans marquer S02E03 ou S02E04 comme vus

#### Scenario: reprise ancienne déjà vue
- **WHEN** l'épisode repris par une app est confirmé vu dans le compte
- **THEN** cette reprise ne masque pas le prochain non vu ; si aucun épisode diffusé ne reste à regarder, le fournisseur n'apporte pas de carte de série

#### Scenario: statut de reprise inconnu
- **WHEN** l'identité ou le statut de l'épisode local n'est pas confirmé
- **THEN** l'absence dans la liste des épisodes à voir ne vaut pas preuve de visionnage ; la reprise locale reste admissible sans mutation automatique du compte

### Requirement: Contenus ouverts depuis SygixOs
La dernière ouverture locale SHALL rester mémorisée par contenu, depuis carte ou menu, pas depuis héro, Top Shelf ou grille. L'identité SHALL survivre à l'enrichissement externe et au changement d'épisode. L'ordre SHALL rester stable jusqu'au retour ; la mémoire SHALL être purgée seulement après absence confirmée dans toutes les sources concernées, jamais sur erreur ou résultat partiel.

#### Scenario: contenu ouvert passé en tête
- **WHEN** un contenu est ouvert depuis la quatrième carte puis que l'utilisateur revient
- **THEN** il est premier, focusé, rangée au début sur la marge gauche et position verticale de grille restaurée

#### Scenario: épisode suivant publié entre-temps
- **WHEN** l'épisode 3 est ouvert puis que l'épisode 4 est retenu au retour
- **THEN** la série garde sa mémoire d'ouverture et sa première place avec le focus

#### Scenario: ouverture depuis le menu
- **WHEN** une entrée d'app ouvre un intent ou l'app en repli
- **THEN** l'ouverture est enregistrée comme depuis la carte

#### Scenario: rien ne s'ouvre
- **WHEN** ni l'intent ni l'app ne s'ouvrent
- **THEN** aucune ouverture n'est enregistrée et le tri reste inchangé

#### Scenario: ordre entre contenus ouverts
- **WHEN** plusieurs contenus ont été ouverts depuis À suivre
- **THEN** ils restent en tête par ouverture décroissante, avant les contenus jamais ouverts

#### Scenario: chargement et premier lancement
- **WHEN** le launcher redémarre avec une mémoire persistée
- **THEN** les contenus déjà chargés et encore présents sont ordonnés avec leur mémoire dès le premier affichage, sans réordonnancement ultérieur dû au chargement tardif de cette mémoire ; une mémoire vide ou illisible donne le tri des groupes sans erreur, sans empêcher l'insertion ultérieure de nouveaux contenus

#### Scenario: purge des contenus disparus
- **WHEN** les sources concernées ont été lues entièrement avec succès et qu'aucune ne contient le contenu
- **THEN** son ouverture est supprimée ; une réapparition suit les groupes jusqu'à sa prochaine ouverture depuis À suivre

#### Scenario: pas de purge sans lecture fiable
- **WHEN** une source échoue, dépasse son délai, manque de permission, est désactivée ou n'a pas terminé sa pagination
- **THEN** aucune absence déduite de ce résultat ne purge la mémoire

#### Scenario: rafraîchissement
- **WHEN** la rangée se recharge
- **THEN** le tri est recalculé sans déplacer le focus d'une carte encore présente, sauf retour suivant son ouverture depuis À suivre

**Détails.** — Les anciennes clés locales restent reconnues : titre normalisé de série, ou titre/année de film avec les règles existantes d'année inconnue. Un identifiant externe confirmé enrichit cette clé sans perdre ouvertures ni retraits. L'enregistrement exige toujours un lancement réussi depuis carte ou menu ; l'ouverture depuis le héro conserve son origine et ne change pas cette règle. La purge exige une lecture fiable de toutes les sources concernées, TV Provider et compte compris ; les erreurs, permissions manquantes, filtres et pannes ne valent pas absence.

### Requirement: Contenus retirés d'À suivre
« Retirer d'À suivre » SHALL masquer localement la série entière ou le film pour toutes les sources de la rangée, sans modifier le compte. Identité, titre, type, sources, image et date SHALL être persistés. Le retrait SHALL aussi exclure les candidats BetaSeries du héro ; seul Réafficher dans settings l'annule, sans purge automatique.

#### Scenario: retrait d'une série
- **WHEN** une série présente via deux apps et le suivi est retirée
- **THEN** ses épisodes présents et futurs disparaissent de la rangée sans confirmation, même après redémarrage, et son apport BetaSeries quitte le héro

#### Scenario: retrait d'un film
- **WHEN** un film est retiré
- **THEN** toutes ses cartes disparaissent ; un autre film de même titre et d'année ou identité distincte n'est pas retiré

#### Scenario: place libérée
- **WHEN** la rangée contient 20 cartes avant retrait
- **THEN** le contenu suivant du tri commun entre dans la limite au prochain calcul

#### Scenario: focus après retrait
- **WHEN** la carte focusée est retirée
- **THEN** le menu se ferme et le focus suit les scénarios de carte disparue ou rangée vide de launcher-shell

#### Scenario: contenu retiré qui n'est plus publié
- **WHEN** le contenu n'est plus fourni par aucune source
- **THEN** le retrait persiste et reste réaffichable dans les réglages

#### Scenario: réaffichage
- **WHEN** l'utilisateur choisit Réafficher
- **THEN** le retrait est supprimé et le contenu revient selon les sources encore valides, le tri et la politique du héro ; aucun statut du compte n'est modifié

### Requirement: Carte Up Next
« À suivre » SHALL désigner la rangée de tous les groupes, pas une catégorie. Les cartes SHALL suivre les maquettes 7.1 à 7.5 et Carte Up Next, avec rendu et qualité décrits ci-dessous et focus intégral selon « Focus tvOS ». Un contenu du compte SHALL montrer le badge du lecteur choisi, sinon du fournisseur ; une carte locale conserve l'icône réelle d'app. La progression SHALL provenir uniquement d'une lecture connue.

#### Scenario: largeur et débordement
- **WHEN** au moins cinq cartes sont affichées à 960 dp de large
- **THEN** quatre cartes de 198 dp sont entières depuis la marge gauche avec `Dimens.GridSpacing`, la cinquième déborde du bord droit sans marge qui la masque

#### Scenario: défilement horizontal
- **WHEN** une carte partiellement visible ou hors écran prend le focus
- **THEN** la rangée défile juste pour la rendre entière entre la marge gauche et le bord droit, avec la durée et la courbe de « Focus tvOS » ; sa position est conservée hors retour suivant une ouverture depuis la rangée

#### Scenario: fin de rangée
- **WHEN** la rangée dépasse la largeur disponible et que sa dernière carte prend le focus
- **THEN** son bord droit hors zoom finit à `Dimens.ScreenMarginH` du bord, symétriquement au début ; seul le zoom peut dépasser

#### Scenario: rangée qui tient dans la largeur
- **WHEN** toutes les cartes tiennent entre les marges
- **THEN** aucun défilement n'a lieu

#### Scenario: carte épisode
- **WHEN** un épisode est affiché
- **THEN** il porte image plein cadre ou portrait centré, dégradé, titre de série et « SxxEyy · titre d'épisode » selon les champs disponibles et « Champs facultatifs » ; une reprise connue porte sa progression

#### Scenario: carte film
- **WHEN** un film est affiché
- **THEN** il conserve image et titre seul, sans texte d'action ni SxxEyy ; la progression apparaît seulement pour une reprise connue

#### Scenario: badge et image
- **WHEN** une carte est affichée
- **THEN** une carte locale montre en haut à droite l'icône réelle de son app via PackageManager, sans logo embarqué ; une carte de suivi montre au même emplacement celle du lecteur choisi, sinon le badge BetaSeries ; si le badge applicable est indisponible, il est omis sans libellé texte de substitution

#### Scenario: poster portrait
- **WHEN** seul un poster portrait exploitable existe
- **THEN** il est centré à pleine hauteur sur le fond existant, sans changer le dégradé, les textes, la progression ou le badge

#### Scenario: placeholder
- **WHEN** toutes les candidates et copies utilisables sont absentes ou refusées
- **THEN** la taille et les coins restent identiques avec le fond et pictogramme existants ; textes, progression connue et badge restent affichés

#### Scenario: image trop petite
- **WHEN** toutes les candidates sont plus étroites que la carte affichée en pixels hors zoom
- **THEN** la carte reste avec son placeholder

#### Scenario: focus
- **WHEN** une carte prend le focus
- **THEN** « Focus tvOS » s'applique intégralement et aucun Top Shelf ne s'ouvre

#### Scenario: image à la largeur de la carte
- **WHEN** une candidate de 396 px est disponible pour une carte de 396 px mais refusée par une autre zone
- **THEN** elle est affichée dans cette carte, indépendamment de ce refus

#### Scenario: chargement indépendant des images
- **WHEN** certaines images attendent et d'autres sont prêtes
- **THEN** seules les premières restent en placeholder ; aucun chargement collectif ne bloque la rangée ou ne change focus et géométrie

#### Scenario: image locale pendant une panne
- **WHEN** une copie reste utilisable selon le contrat d'images et le contenu est encore valide
- **THEN** elle est affichée sans attendre le fournisseur ; à expiration le placeholder la remplace ; une image en cache ne réintroduit pas un contenu BetaSeries retiré pour panne

**Détails.** — Barre : CONTINUE avec progression SHALL montrer une barre de hauteur `Dimens.ProgressHeight`, piste `SygixColors.ProgressTrack`, remplissage `SygixColors.OnDark`, placée à `Dimens.UpNextTextBottom` du bas ; ses textes sont à `Dimens.UpNextTextAboveProgress` au-dessus. Sans barre, les textes sont à `Dimens.UpNextTextBottom` sans réserve de progression. Badge : en haut à droite à `Dimens.UpNextBadgeInset`, taille `Dimens.HeroSourceIcon`, coins `Dimens.UpNextBadgeCorner` concentriques avec la carte selon « Thème » ; l'exception fournisseur ci-dessus remplace seulement l'obligation d'un package pour un contenu du compte, sans autoriser de logo embarqué à la place de l'icône PackageManager d'une carte locale. Géométrie : largeur (contenu des rangées d'apps − 3 × `Dimens.GridSpacing`) / 4, départ `Dimens.ScreenMarginH`, pas de marge droite, fin symétrique hors zoom ; seul le demi-agrandissement du zoom peut déborder. Médias : 16:9, coins `Dimens.TileCorner`, fond `SygixColors.TileBackground` continu avec la grille, paysage sinon portrait centré à pleine hauteur sinon `Dimens.UpNextPlaceholderIcon` / `SygixColors.PlaceholderGlyph` centré dans la moitié haute ; dégradé transparent à `Dimens.CardScrimStart` jusqu'à `SygixColors.CardScrimEnd` en bas. Textes : en bas à gauche à `Dimens.UpNextTextInset`, `TextStyles.Row` / `SygixColors.OnDark`, détails `TextStyles.RowSecondary` / `SygixColors.OnDarkDetails`, sans texte supplémentaire, action ni nom d'app. Qualité : largeur réelle au moins égale à la carte en pixels hors zoom, portrait accepté, dimensions issues du parcours partagé de launcher-shell sans hériter du verdict d'une autre zone ; essayer alternatives et copies utilisables avant placeholder.

### Requirement: Ouverture d'un item
L'appui sur une carte associée au suivi SHALL utiliser le parcours commun de `watch-tracking`. Une carte locale non associée SHALL conserver l'ouverture de son intent publié, sinon de son app source. L'origine de l'ouverture SHALL déterminer la restauration et la mémoire existantes.

#### Scenario: intent disponible
- **WHEN** une carte locale non associée publie un intent
- **THEN** cet intent ouvre le contenu selon le comportement de l'app et l'ouverture réussie est mémorisée ; « ouvrir » ne signifie pas qu'une lecture est prouvée

#### Scenario: intent absent ou en échec
- **WHEN** une carte locale non associée ne publie pas d'intent ou que son ouverture échoue
- **THEN** l'app source est lancée sans crash

#### Scenario: contenu suivi
- **WHEN** une carte identifiée dans le compte est validée
- **THEN** le choix initial ou mémorisé, les replis, la preuve de lecture et le retour suivent le parcours commun, partagé avec le héro

### Requirement: Menu « Ouvrir avec… »
L'appui long SHALL toujours ouvrir le menu. Le menu local conserve sources et retrait local sans préférence de lecteur, mais SHALL proposer Ajouter au suivi si un compte est connecté et le contenu identifié absent de ce compte. Un contenu appartenant au compte SHALL offrir les actions et le lecteur partagé de `watch-tracking`. Identité résolue et appartenance au compte SHALL être distinctes ; retrait local et retrait du compte aussi.

#### Scenario: ouverture du menu
- **WHEN** une carte locale non associée reçoit un appui long
- **THEN** les sources conservées sont listées dans leur ordre, avec icône et nom, suivies de « Retirer d'À suivre », sous vignette, titre et « Ouvrir avec… » ; si le compte est connecté et le contenu identifié absent de ce compte, Ajouter au suivi est également proposé sans remplacer ces actions locales

#### Scenario: contenu identifié absent du compte
- **WHEN** une carte locale est identifiée sans ambiguïté mais absente du compte connecté
- **THEN** son menu propose Ajouter au suivi ; sa simple identification ne déclenche ni préférence persistante ni parcours de lecteur du compte ; après ajout confirmé et appartenance relue, ses prochaines ouvertures suivent le parcours commun

#### Scenario: compte absent ou identité incertaine
- **WHEN** aucun compte n'est connecté, que la correspondance est ambiguë ou que l'appartenance au compte n'est pas établie
- **THEN** le menu conserve les ouvertures locales ; aucune action Ajouter n'est proposée sur la base d'une absence supposée et aucun statut du compte n'est modifié

#### Scenario: rendu des entrées
- **WHEN** le menu est ouvert
- **THEN** la pilule focusée est claire avec texte sombre sans zoom ni halo, les autres entrées sans fond ; verre et voile suivent le menu existant

#### Scenario: focus initial dans le menu
- **WHEN** un menu local s'ouvre
- **THEN** la première app est focalisée et haut/bas parcourent les entrées puis le retrait, sans boucle ; pour un menu de suivi, « Navigation des parcours de suivi » s'applique

#### Scenario: choix d'une app
- **WHEN** une entrée d'app est validée
- **THEN** l'app est ouverte et le menu fermé ; pour une carte suivie, la préférence commune est mise à jour après succès

#### Scenario: fermeture et focus
- **WHEN** Retour est pressé dans le menu
- **THEN** il se ferme sans ouverture et la carte d'origine reprend le focus, sinon son repli de launcher-shell

#### Scenario: source unique
- **WHEN** une seule source locale existe
- **THEN** le menu reste disponible avec cette entrée et le retrait ; une carte suivie garde aussi ses actions de compte applicables

#### Scenario: retirer d'À suivre
- **WHEN** « Retirer d'À suivre » est validé
- **THEN** le retrait local est appliqué sans confirmation ni toast, le menu ferme sans ouvrir d'app ; aucune mutation BetaSeries n'est envoyée

#### Scenario: intent en échec depuis le menu
- **WHEN** l'intent d'une source locale choisie échoue
- **THEN** l'app est lancée en repli avec un toast « Contenu indisponible, ouverture de <nom de l'app> », sans crash ; un contenu suivi suit les replis du parcours commun

**Détails.** — Le menu local SHALL être centré sous voile `SygixColors.Scrim`, verre `GlassLook.Menu`, largeur `Dimens.MenuWidth`, coins `Dimens.MenuCorner`, padding `Dimens.MenuPadding`. Son en-tête garde une vignette 16:9 `Dimens.MenuThumbnailWidth`, image sinon placeholder, titre `TextStyles.MenuTitle`, sous-titre « Ouvrir avec… » en `TextStyles.MenuSubtitle` / `SygixColors.MenuSubtitle`. Les entrées gardent `Dimens.PillCorner`, hauteur minimale `Dimens.MenuActionHeight`, retrait `Dimens.MenuEntryPadding`, vraie icône `Dimens.MenuEntryIcon` aux coins `Dimens.MenuEntryIconCorner`, écart `Dimens.MenuEntryGap`, nom `TextStyles.Row` au repos et `TextStyles.RowFocused` au focus. Le retrait local suit la dernière app sans séparateur, même pilule et œil barré de `Dimens.MenuEntryIcon` dans la couleur du texte, conformément à 7.6. La passe Penpot complète ce composant pour Ajouter et les actions du compte sans modifier ces règles locales.

### Requirement: États de la rangée
La rangée SHALL distinguer chargement, vide, erreur et accès refusé par source, sans bloquer le home. Les programmes locaux SHALL garder leur contrat de validité ; l'apport du suivi en échec SHALL être retiré immédiatement avec son indicateur. Focus et disparition SHALL suivre « Navigation 3 paliers ».

#### Scenario: permission refusée
- **WHEN** READ_TV_LISTINGS n'est pas accordée
- **THEN** seules les données locales deviennent indisponibles ; les contenus de suivi accessibles restent affichés ; sans eux la rangée est masquée sans invitation à configurer la permission

#### Scenario: erreur
- **WHEN** la lecture locale échoue au premier chargement et qu'aucune autre source n'apporte de contenu
- **THEN** une seule carte d'état focusable remplace le contenu en première position sous le titre À suivre à sa position réglée (maquette 7.4), distincte du vide : même taille et coins qu'une carte, fond `SygixColors.TileBackground`, pictogramme recharger centré de `Dimens.UpNextErrorIcon`, « Impossible de charger » en `TextStyles.Row` / `SygixColors.OnDark`, « OK pour réessayer » en `TextStyles.RowSecondary` / `SygixColors.OnDarkSecondary` ; une erreur du suivi reste aussi consultable via son indicateur

#### Scenario: carte d'erreur
- **WHEN** la carte d'erreur locale prend le focus
- **THEN** le focus d'une tuile selon « Focus tvOS » s'applique ; elle occupe la place de la rangée sous À suivre dans la navigation de grille, avant ou après les applications selon le réglage, et OK relance la requête

#### Scenario: chargement initial
- **WHEN** le premier chargement sans contenu prêt dépasse environ 300 ms
- **THEN** le squelette sous À suivre (maquette 7.3) montre cinq cartes de la taille et des coins d'une carte, fond `SygixColors.SkeletonBase`, chacune avec deux barres `SygixColors.SkeletonBar` en bas à gauche, `Dimens.UpNextSkeletonTitle` puis `Dimens.UpNextSkeletonSubtitle`, rayons à mi-hauteur, sans animation imposée ni élément focusable ; sous environ 300 ms aucune étape intermédiaire n'apparaît, et un résultat local prêt n'attend pas le compte

#### Scenario: rechargement
- **WHEN** la rangée se recharge avec du contenu encore valide
- **THEN** elle conserve ce contenu pendant l'attente sans squelette ni prolongation de validité ; une réponse du suivi en échec retire son apport

#### Scenario: erreur au rechargement
- **WHEN** une relecture locale échoue
- **THEN** ses programmes précédents restent affichés jusqu'à expiration sans carte d'erreur ni saut, sans renouveler leur confirmation ; cette rétention ne s'applique pas à la contribution BetaSeries en échec

#### Scenario: vide
- **WHEN** toutes les sources accessibles réussissent sans item et qu'aucune autre n'apporte de contenu
- **THEN** la rangée est masquée et les images en cache ne la repeuplent pas

#### Scenario: non-blocage
- **WHEN** une source est dans n'importe quel état
- **THEN** le D-pad du home reste utilisable ; les actions de compte restent tentables pendant une panne et exposent leur propre résultat

#### Scenario: expiration du dernier résultat après une erreur
- **WHEN** les programmes locaux atteignent leur plafond sans confirmation
- **THEN** ils sont retirés ; s'il reste des contenus de suivi, ils restent affichés, sinon l'erreur locale réessayable apparaît ; aucune mémoire de retrait ou d'ouverture n'est effacée sur cette erreur
