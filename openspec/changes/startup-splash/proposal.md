# Change : startup-splash

## Why
Au démarrage à froid, tant que l'accueil n'est pas prêt, le launcher affiche aujourd'hui le dégradé sombre animé du repli du héro (`HomeState.Loading` dans `HomeScreen.kt`), précédé de l'écran de lancement que le système dessine pour toute app depuis Android 12. Le résultat n'a pas d'identité et peut montrer deux écrans successifs différents. Sygix veut un écran de démarrage propre à SygixOs : la mascotte seule, animée, centrée sur le fond sombre de l'app, puis un fondu vers l'accueil, sans double écran ni flash.

## What Changes
- **Écran de démarrage** à la place de l'état de chargement de l'accueil : la mascotte seule (le fantôme, sans TV ni nom, aucun texte), centrée sur le fond de l'app, animée en boucle ; affiché seulement au démarrage à froid, tant que l'accueil n'est pas prêt, au moins 600 ms pour éviter le flash, puis fondu enchaîné vers l'accueil. Jamais au retour sur le launcher (touche Home, retour d'une autre app).
- **Animation pré-rendue** : WebP animé produit depuis Blender par le chantier logo (flottement et clignement, boucle courte, 60 images/s), décodé nativement par le système. Ce change fixe le contrat du fichier (format, résolution pour un rendu TV 1080p, budget de poids, boucle sans saut, fond transparent ou de la couleur exacte du fond), vérifié par un test automatique.
- **Enchaînement avec l'écran de lancement du système** (API SplashScreen d'Android 12+) : fond identique, aucune icône système, de sorte que l'utilisateur ne voie ni double écran ni changement de couleur ni saut de la mascotte.
- **Accessibilité** : quand les animations sont désactivées dans le système, la mascotte est une image fixe et aucun fondu n'est joué.
- L'écran de démarrage n'est pas une zone de l'accueil : il n'est jamais focusable et n'ajoute aucun palier ; « Navigation 3 paliers » n'est pas modifiée.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `launcher-shell` : ADDED « Écran de démarrage », « Animation de la mascotte », « Fichier de l'animation de la mascotte », « Enchaînement avec l'écran de lancement du système », « Écran de démarrage sans animation » ; MODIFIED « Écran initial du home » (au démarrage à froid, le héro apparaît après l'écran de démarrage ; scénarios existants conservés).
- `ui-testing` : ADDED « Couverture de l'écran de démarrage ».
- `settings` : non touchée.

## Dépendances et chevauchements
- **Asset de la mascotte (prérequis d'implémentation)** : le WebP animé final est produit par le chantier logo et n'est pas encore livré. La spécification peut être validée sans lui ; l'implémentation ne commencera qu'à la livraison de l'asset WebP final, qui doit respecter « Fichier de l'animation de la mascotte » (tâche 1.3 de `tasks.md`).
- **`ui-tvos-polish`** (PR #18, non mergée) : aucune exigence en commun. « Écran initial du home » n'est modifiée ni par `ui-tvos-polish` ni par `p2c-upnext` ; ce change ne touche ni « Navigation 3 paliers » ni « Page de réglages ». L'implémentation modifie `HomeScreen.kt`, aussi modifié par `ui-tvos-polish` : elle part de `main` après le merge de `ui-tvos-polish`.
- **`p2c-upnext`** (spec sur `main`) : aucun recouvrement.
- **Ordre d'archivage** : aucune contrainte vis-à-vis de `ui-tvos-polish` ni de `p2c-upnext`.

## Impact
- `ui/home/HomeScreen.kt` : l'état de chargement affiche l'écran de démarrage au lieu de `AmbientGradient` ; l'accueil est composé sous l'écran de démarrage dès qu'il est prêt, puis révélé par le fondu.
- Nouvelle logique de démarrage (domaine pur, testable en JUnit) et petite source de données pour l'animation et le réglage d'animation du système (voir `design.md`).
- Ressources : `res/raw/splash_mascot.webp` (asset livré par le chantier logo), un drawable transparent pour l'icône de l'écran de lancement du système, attributs du thème `Theme.SygixOs`.
- `core/designsystem` : jetons de durée et de taille de l'écran de démarrage (`Motion`, `Dimens`).
- Aucune nouvelle dépendance : `ImageDecoder` / `AnimatedImageDrawable` et l'API SplashScreen sont dans la plateforme (minSdk 34).
- Poids de l'APK : au plus 1 Mio de plus (budget de l'asset, voir `design.md`).

## Non-goals
- Animation de sortie de la mascotte (vers le héro ou le dock) : hors périmètre, à reprendre dans un change de polish si Sygix le souhaite.
- Écran de démarrage au retour sur le launcher ou à la sortie de veille de la TV : exclu par décision de Sygix.
- Son au démarrage : non prévu.
- Production de l'asset (modélisation, rendu Blender) : chantier logo, hors de ce dépôt de specs.

## Questions ouvertes
Les choix non tranchés par Sygix sont détaillés, avec options et choix provisoire « à confirmer », dans `design.md`, section « Questions ouvertes » : taille affichée de la mascotte, contenu de l'écran de lancement du système, moment où l'accueil est « prêt », chargement hors démarrage à froid, durée de la boucle.
