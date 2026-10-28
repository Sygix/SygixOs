## MODIFIED Requirements

### Requirement: Contenus retirés d'À suivre
« Retirer d'À suivre » SHALL masquer localement la série entière ou le film pour toutes les sources de la rangée, sans modifier le compte. Identité, titre, type, sources, image et date SHALL être persistés. Le retrait SHALL aussi exclure les candidats du fournisseur actif du héro ; seul Réafficher dans settings l'annule, sans purge automatique.

#### Scenario: retrait d'une série
- **WHEN** une série présente via deux apps et le suivi est retirée
- **THEN** ses épisodes présents et futurs disparaissent de la rangée sans confirmation, même après redémarrage, et son apport du fournisseur actif quitte le héro

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
« À suivre » SHALL désigner la rangée de tous les groupes, pas une catégorie. Les cartes SHALL suivre les maquettes 7.1 à 7.5 et Carte Up Next, avec rendu et qualité décrits ci-dessous et focus intégral selon « Focus tvOS ». Un contenu du compte SHALL montrer le badge du lecteur choisi, sinon du fournisseur actif ; une carte locale conserve l'icône réelle d'app. La progression SHALL provenir uniquement d'une lecture connue.

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
- **THEN** une carte locale montre en haut à droite l'icône réelle de son app via PackageManager, sans logo embarqué ; une carte de suivi montre au même emplacement celle du lecteur choisi, sinon le badge du fournisseur actif BetaSeries ou Trakt ; si le badge applicable est indisponible, il est omis sans libellé texte de substitution

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
- **THEN** elle est affichée sans attendre le fournisseur ; à expiration le placeholder la remplace ; une image en cache ne réintroduit pas un contenu dont l'apport du compte n'est plus autorisé ou a été retiré, tandis qu'une panne temporaire conserve le contenu durable selon son contrat

**Détails.** — Barre : CONTINUE avec progression SHALL montrer une barre de hauteur `Dimens.ProgressHeight`, piste `SygixColors.ProgressTrack`, remplissage `SygixColors.OnDark`, placée à `Dimens.UpNextTextBottom` du bas ; ses textes sont à `Dimens.UpNextTextAboveProgress` au-dessus. Sans barre, les textes sont à `Dimens.UpNextTextBottom` sans réserve de progression. Badge : en haut à droite à `Dimens.UpNextBadgeInset`, taille `Dimens.HeroSourceIcon`, coins `Dimens.UpNextBadgeCorner` concentriques avec la carte selon « Thème » ; l'exception fournisseur ci-dessus remplace seulement l'obligation d'un package pour un contenu du compte, sans autoriser de logo embarqué à la place de l'icône PackageManager d'une carte locale. Géométrie : largeur (contenu des rangées d'apps − 3 × `Dimens.GridSpacing`) / 4, départ `Dimens.ScreenMarginH`, pas de marge droite, fin symétrique hors zoom ; seul le demi-agrandissement du zoom peut déborder. Médias : 16:9, coins `Dimens.TileCorner`, fond `SygixColors.TileBackground` continu avec la grille, paysage sinon portrait centré à pleine hauteur sinon `Dimens.UpNextPlaceholderIcon` / `SygixColors.PlaceholderGlyph` centré dans la moitié haute ; dégradé transparent à `Dimens.CardScrimStart` jusqu'à `SygixColors.CardScrimEnd` en bas. Textes : en bas à gauche à `Dimens.UpNextTextInset`, `TextStyles.Row` / `SygixColors.OnDark`, détails `TextStyles.RowSecondary` / `SygixColors.OnDarkDetails`, sans texte supplémentaire, action ni nom d'app. Qualité : largeur réelle au moins égale à la carte en pixels hors zoom, portrait accepté, dimensions issues du parcours partagé de launcher-shell sans hériter du verdict d'une autre zone ; essayer alternatives et copies utilisables avant placeholder.
