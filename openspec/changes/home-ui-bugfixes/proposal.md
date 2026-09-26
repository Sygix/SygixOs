# Change : home-ui-bugfixes

## Why
La PR des correctifs de l'accueil (`fix/home-ui-bugs`, après le test de `v0.0.1-rc.1` sur la TV de test) change des comportements visibles que les specs décrivent autrement : l'engrenage des réglages n'est plus en verre translucide, la grille ne suit plus la règle « défiler juste ce qu'il faut » à l'ouverture du Top Shelf, et le focus d'une app qui disparaît n'était spécifié nulle part. Ce change aligne les specs sur le comportement déjà implémenté sur la branche, validé par Sygix, sans rien changer au code du Top Shelf.

## What Changes
- **Engrenage des réglages** (décision de Sygix) : engrenage plein façon tvOS, blanc opaque, sans fond, sans bordure ni matériau verre ; il n'est affiché qu'avec le héro et disparaît en fondu en zone grille. Le focus (zoom et halo) est inchangé
- **Placement de la grille et du Top Shelf** : une rangée ne bouge que si elle sort des marges (marge haute de la grille 40 dp, marge basse 20 dp) ; quand le panneau s'ouvre ou se ferme au-dessus de la rangée focusée, cette rangée reste immobile à l'écran ; un bloc trop grand s'aligne sur la marge haute ; la position de défilement n'est plus remise à zéro en quittant la zone grille
- **App focusée qui disparaît** (app cachée, retirée du dock, désinstallée) : le focus passe à la tuile voisine à la même position ; si la zone devient vide, le focus revient au héro, pour la grille comme pour le dock

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `settings` : MODIFIED « Icône réglages flottante » (engrenage opaque sans verre, affiché seulement avec le héro)
- `launcher-shell` : MODIFIED « Panneau Top Shelf au focus » (règle de placement de la grille) ; ADDED « Focus d'une app disparue »

« Focus d'une app disparue » est ajoutée plutôt que glissée dans une exigence existante : aucune exigence ne décrit ce cas, et les deux candidates, « Navigation 3 paliers » et « Cacher une application », sont déjà modifiées par `p2c-upnext` et par `home-settings-polish`. Un troisième MODIFIED sur la même exigence ferait perdre des scénarios à l'archivage. La capability `ui-testing` n'est pas touchée : ce change n'ajoute ni zone ni palier, et les tests sont listés dans `tasks.md`.

## Impact
- Code : `HomeScreen.kt` (engrenage, garde-fou grille vide), `HomeGrid.kt` et `Dock.kt` (focus d'entrée partagé), `domain/GridScroll.kt` (placement), `domain/FocusFallback.kt` (choix de la tuile cible)
- Tests : `SettingsEntryTest`, `HomeGridScrollTest`, `GridScrollTest`, `CatalogUpdateFocusTest`, `FocusFallbackTest`, `AppTileArtworkTest`
- **Dépendances** : aucune. `settings` et `ui-testing` sont archivés sur main
- **Chevauchements** :
  - `home-settings-polish` (PR #16, non mergée) modifie aussi « Icône réglages flottante » en gardant « verre translucide, faible opacité ». Elle devra se rebaser sur ce change et reprendre ce texte (engrenage opaque sans verre) dans son propre delta, pour que la spec ne contredise pas le code après l'archivage des deux changes. Son scénario « défilement vers la grille » (engrenage solidaire du héro, sans fondu propre) remplacera alors le fondu décrit ici
  - `p2c-upnext` ne modifie aucune des exigences de ce change

## Non-goals
- **Saut de quelques pixels sur Gauche/Droite** : mesuré sur la TV avant le correctif, aucun test JVM ne le reproduit. Il reste validé par la mesure sur la TV
- **Grille vide à l'entrée** : quand toutes les apps sont cachées, le dock et la grille sont vides ; bas depuis le héro mène alors à la zone grille, où rien ne peut prendre le focus, et Retour ne répond plus. Constaté par une sonde Robolectric à la taille TV, c'est antérieur à ce change et non traité ici (voir Questions ouvertes)
- **Descente depuis le héro vers le dock** : la spec dit « le premier élément du dock », le code restaure la dernière tuile visitée du dock. Écart antérieur, à aligner dans un prochain delta de « Navigation 3 paliers »
- Défilement continu héro → grille : `home-settings-polish`

## Questions ouvertes
- **Entrer dans une grille vide** : faut-il bloquer la descente depuis le héro quand la grille est vide, ou y entrer avec un élément focusable (par exemple le message « Aucune app TV détectée ») ? À trancher par Sygix, aucun SHALL ne le décrit ici
