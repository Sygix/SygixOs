# Design

## Context
- Rayons avant ce change : tuiles 9 dp, dock 20 dp (marge 10), menu 18 dp (marge 14) et pilules 9 dp, lignes des réglages 8 dp, vignette des réglages 5 dp ; seuls la capsule et l'engrenage étaient déjà concentriques (pilules, marge 4).
- Les traces Perfetto de la rc.6 n'ont pas été conservées ; l'analyse de la grille part du code et d'un comptage des scopes recomposés sous Robolectric (observateur de composition), puis de mesures `dumpsys gfxinfo` sur la TV avec la variante `perf`.
- AGP 9.4 produit déjà, à côté de l'APK de release, des fichiers `.dm` par plage d'API (`outputs/apk/<variante>/baselineProfiles/<n>/app-<variante>.dm`, listés dans `output-metadata.json`), à partir du même profil compilé que `assets/dexopt/` de l'APK.

## Decisions

### D1. Jetons des rayons et couples imbriqués
`Dimens` porte les rayons et marges, et `Dimens.Nested` la liste des couples (extérieur, intérieur, marge). Un test JUnit vérifie extérieur = intérieur + marge pour chaque couple et les valeurs validées ; des tests Compose à la taille TV mesurent les marges réelles (dock, capsule, menu, ligne des réglages). La capsule reçoit une hauteur fixe (engrenage + 2 × marge) pour que la marge ne dépende pas de la hauteur du texte de l'heure. La vignette du menu est alignée en haut de l'en-tête pour que sa marge haute ne dépende pas des métriques de la police. Le fond du code QR est arrondi à `QrSize × zone de silence / modules`, ce qui laisse les modules intacts.

Quand les marges horizontale et verticale diffèrent, la plus petite est retenue (le coin intérieur reste dans le coin extérieur). Alternative écartée : déplacer les vignettes pour égaliser les marges, ce qui décalerait le texte des lignes.

### D2. Focus des tuiles sans recomposition
`Modifier.tvFocus` devient deux nœuds de modificateur : un nœud de mise en page avec calque (zoom, montée, ombre) qui écoute le focus et anime sa progression dans sa propre coroutine, puis un nœud de dessin (reflet) qui lit cette progression. La progression n'est lue que dans le calque et le dessin : le focus d'une tuile ne recompose plus la tuile. Alternative écartée : garder `composed` avec un `animateFloatAsState`, qui recompose chaque tuile qui prend ou perd le focus et recrée ses modificateurs.

### D3. Grille découpée en rangées
Chaque rangée est un composable dont les paramètres ne changent que si la rangée contient la tuile d'entrée ou l'app déplacée, ou porte le panneau. Les affiches du panneau sont un état dérivé à égalité structurelle. Les visuels vérifiés et le contenu du héro sont passés à la grille par des lambdas qui lisent un état mis à jour, et au héro masqué (zone grille) sous leur dernière valeur visible : une vérification d'image ne recompose ni le héro caché ni les rangées. Mesure Robolectric (grille de 20 apps, observateur de composition) : un déplacement recomposait 43 scopes, il en recompose 17 vers la droite et 27 vers le bas ; une vérification d'image en recomposait 95, elle en recompose 28. Les plafonds de `GridRecompositionTest` (30 et 40) échouent sur l'ancienne implémentation.

### D4. Vérification différée et décodage des affiches
La préparation du panneau (vérification d'au plus 8 affiches de l'app, décodées en 1920 × 1080 hors du fil principal) attend que le focus reste 0,5 s sur la tuile : un parcours rapide de la grille ne lance plus de décodage à chaque tuile. Le panneau s'ouvre toujours 3 s après le focus. Les affiches du panneau sont demandées à la taille du panneau (en le couvrant) au lieu de 1920 × 1080. Les bannières sont préparées pour le dessin (`prepareToDraw`) dès leur décodage et gardent une seule `ImageBitmap`.

### D5. Profil de démarrage
La tâche `dexMetadata<Variante>` lit `output-metadata.json` de l'APK, choisit le `.dm` produit par AGP pour la plage d'API qui contient `minSdk` (34, donc la plage 31+) et le copie en `outputs/dexmetadata/<variante>/app-<variante>.dm`. La release le vérifie (seulement `primary.prof` et `primary.profm`, métadonnées identiques à celles de l'APK) puis le publie avec l'APK, dans le même brouillon, avant la publication : le flux brouillon → téléversement → publication reste compatible avec les releases immuables.

Mise à jour intégrée : `UpdateSelector` retient l'asset `app-release.dm` seulement avec une empreinte SHA-256, une taille de 16 Mo au plus et une URL HTTPS. Après les vérifications de l'APK, `UpdateInstaller` le télécharge en mémoire (pas de fichier), vérifie taille, empreinte et contenu (`DexMetadata`), puis l'écrit dans la session de l'APK sous `base.dm` (l'APK y est écrit sous `base.apk`). Tout échec du profil le fait abandonner ; si la session refuse le profil, elle est abandonnée et l'APK est installé seul dans une nouvelle session. Alternative écartée : construire le `.dm` à partir de `assets/dexopt/baseline.prof` de l'APK (procédure manuelle de la documentation Android) : ce fichier est au format attendu par `profileinstaller`, pas forcément par l'ART de la version cible, alors que le `.dm` d'AGP est produit pour la plage d'API.

### D6. Infos du programme dans le héro
`HeroItem` reçoit le type du programme (`ProgramKind`), la saison, l'épisode, la durée et la position, lus par `TvProviderHeroSource` (colonnes facultatives). `HeroCaption` (domaine, JVM) choisit l'en-tête, les éléments de la ligne d'infos et les arrondis ; `HeroStage` les écrit avec les chaînes en ressources. L'icône de l'app vient d'un cache d'icônes carrées sans masque (couches d'une icône adaptative dessinées à plein cadre), chargé hors du fil principal une fois par app. Les textes ne lisent aucun état animé : le Ken Burns ne les recompose pas (`IdleFrameTest`).

## Risks / Trade-offs
- [Profil refusé par une TV qui exige une signature fs-verity du `.dm`] → la session est abandonnée et l'APK est réinstallé seul ; à surveiller dans les journaux de la mise à jour.
- [AGP change l'emplacement des `.dm`] → la tâche échoue au build (pas de release sans profil silencieusement).
- [Colonnes du TV Provider peu remplies par les apps] → les lignes manquent simplement ; vérification sur la TV avec la variante `perf`.
- [Vérification différée de 0,5 s] → le panneau peut s'ouvrir un peu plus tard qu'avant si les affiches sont lentes à vérifier ; il reste à 3 s quand elles le sont en moins de 2,5 s.
