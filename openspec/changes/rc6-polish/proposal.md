# Change : rc6-polish

## Why
Retours de Sygix et du test de la pré-release v0.0.1-rc.6 sur la TV de référence (TV Google TV sous Android 14) :
- les rayons des coins ne suivent plus de règle : les tuiles d'apps ont changé sans que le dock suive, et les rayons sont plus petits que ceux de tvOS. Sygix veut des rayons plus proches d'Apple et des coins imbriqués toujours concentriques ;
- après une installation ou une mise à jour, l'app reste interprétée (état ART `verify`) jusqu'à la compilation de fond du système, alors que le profil de référence est déjà dans l'APK ;
- la navigation dans la grille a 10,9 % d'images en retard (objectif : moins de 10 %), alors que le héro tient 60 images par seconde ;
- le héro n'affiche pas les infos de la maquette validée : en-tête « Continuer dans X » avec l'icône de l'app, ligne « Saison 2 · Épisode 5 · 42 min », « Reste 25 min » à côté de la barre de progression ;
- l'écran de démarrage joue la mascotte à 25-30 images par seconde (animation environ 2,5 fois trop lente, sans clignement visible) et son fondu de sortie à 16-32 images par seconde : la première composition et le premier dessin de l'accueil, ses chargements et ses animations ont lieu pendant la mascotte, puis l'accueil est dessiné pour la première fois pendant le fondu ;
- le titre du héro change avant le visuel : le texte suit le programme demandé alors que le visuel attend son image, de 0,4 à 0,9 s d'habitude et jusqu'à 10 s ; mesuré sur la TV, seules les images servies par le fournisseur de contenu d'une app (`content://`, Jellyfin) sont lentes (5 à 11 s), les images HTTPS des autres apps arrivent en 0,1 à 0,5 s.

## What Changes
- **Rayons concentriques** (décision de Sygix) : tuiles de la grille et du dock 14 dp, dock 24 dp (marge 10), menu contextuel 26 dp avec des pilules de 12 dp (marge 14), lignes des réglages 14 dp, capsule heure et engrenage en pilule. Les autres imbrications suivent la même règle avec leurs marges réelles : vignette en tête du menu 10 dp (marge 16), vignette 16:9 des lignes des réglages 3,5 dp (marge 10,5), fond du code QR d'« À propos » arrondi à sa marge blanche. L'ombre et le reflet de la tuile focalisée, la pilule de focus et le verre précalculé du dock suivent le rayon de leur surface. Jetons centralisés dans `Dimens`, avec la liste des couples imbriqués vérifiée par un test.
- **Profil de démarrage livré** : le build produit `app-release.dm` (fichier de métadonnées généré par AGP à partir du profil de l'APK) ; la release le publie à côté de l'APK, avec son empreinte ; la mise à jour intégrée le télécharge, le vérifie par l'empreinte SHA-256 publiée par GitHub et l'écrit dans la même session d'installation que l'APK (`base.dm`), sans jamais bloquer la mise à jour s'il manque, échoue ou dépasse 10 s, et en relançant une seule fois l'installation sans profil si Android refuse la session qui le portait ; le README documente `adb install-multiple app-release.apk app-release.dm`.
- **Navigation dans la grille** : focus des tuiles sans recomposition (animation dans un nœud de modificateur), rangées de la grille isolées (un déplacement ne recompose que les rangées touchées), vérification des affiches du panneau Top Shelf lancée seulement quand le focus reste 0,5 s sur une tuile, vérifications d'un visuel sans recomposition du héro masqué ni des rangées, affiches du panneau décodées à sa taille (avec la marge du zoom).
- **Infos du programme dans le héro** (décision de Sygix, maquette validée) : en-tête au-dessus du titre (icône de l'app source, libellé selon le type du programme : « Continuer dans X », « Épisode suivant dans X », « Nouveau dans X », « À regarder dans X », « X » seul pour un programme mis en avant), ligne d'infos (saison, épisode, durée, seulement ce que l'app publie), « Reste X min » à côté de la barre de progression quand la position et la durée sont connues ; lecture des colonnes `watch_next_type`, `season_display_number`, `episode_display_number`, `duration_millis` et `last_playback_position_millis`.

- **Écran de démarrage** : l'accueil, composé après le fondu d'entrée de la mascotte comme avant, est dessiné sous l'écran de démarrage opaque au lieu d'être transparent, sans animation (visuel du héro sans fondu) ; la grille hors écran n'est composée qu'après le fondu ; pendant la mascotte, la vérification des visuels suivants du héro attend, et les bannières sont décodées sur un fil de basse priorité.
- **Synchronisation du héro** : titre, infos et bouton changent avec le visuel ; un visuel (image ou première image d'une vidéo d'aperçu) qui n'est pas prêt 1 s après le changement fait passer au programme suivant sans changer les textes, continue de se préparer en arrière-plan sans nouvelle requête, et revient dans la rotation une fois prêt. Sous l'écran de démarrage, une vidéo d'aperçu est préparée sans être lue.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `launcher-shell` : MODIFIED « Thème », « Diaporama héro », « Préchargement et mémoire », « Écran de démarrage » (versions de `rc5-tv-fixes`), « Contenu héro TV Provider » (version de `openspec/specs/`) ; ADDED « Profil de démarrage livré avec l'application ».
- `self-update` : MODIFIED « Téléchargement vérifié », « Installation de la mise à jour », « Couverture de test des mises à jour » (versions de `self-update`).
- `ui-testing` : MODIFIED « Couverture du style tvOS » (version de `ui-tvos-polish`), « Couverture de l'écran de démarrage » (version de `rc5-tv-fixes`) ; ADDED « Couverture des infos du héro ».
- `settings` : non touchée (la règle des rayons des lignes et du code QR est portée par « Thème »).

## Dépendances et chevauchements
- Ce change reprend des exigences portées par des changes non archivés ; chaque delta MODIFIED part de leur dernier texte et garde tous leurs scénarios : `rc5-tv-fixes` (« Thème », « Diaporama héro », « Préchargement et mémoire », elles-mêmes reprises de `ui-tvos-polish`), `ui-tvos-polish` (« Couverture du style tvOS »), `self-update` (capability `self-update`, créée par ce change non archivé).
- `startup-splash` : « Écran de démarrage » vient de ce change, reprise par `rc5-tv-fixes` ; ce change part du texte de `rc5-tv-fixes`.
- `up-next` : aucune exigence commune. Son exigence « Champs facultatifs » (`up-next`) rattache un watch next sans type au groupe « à suivre » pour le tri et le dédoublonnage de la rangée Up Next ; ici, le même programme montre seulement le nom de l'app dans l'en-tête du héro. Les deux règles portent sur des choses différentes (tri de la rangée, libellé du héro) et ne se contredisent pas ; la carte Up Next garde son propre texte (« SxxEyy »).
- `jellyfin-tvprovider-only` : sans recouvrement.
- Les questions ouvertes « profil appliqué dès l'installation » et le non-goal « navigation dans la grille » de `rc5-tv-fixes`, et le non-goal « lignes de la maquette qui demandent des données absentes du modèle » de `ui-tvos-polish`, sont traités ici ; leurs documents renvoient à ce change.
- **Ordre d'archivage** : `ui-tvos-polish`, `startup-splash`, `self-update`, `rc5-tv-fixes`, puis ce change ; `up-next` après `ui-tvos-polish`, sans contrainte vis-à-vis de ce change. Archiver ce change avant `ui-tvos-polish`, `self-update` ou `rc5-tv-fixes` est refusé par `openspec archive` (exigences cibles absentes).

## Impact
- `core/designsystem` : `Dimens` (rayons, couples imbriqués `Nested`, capsule, vignettes, icône du héro), `TvFocus` (focus des tuiles en nœuds de modificateur, `TileShape`), `Type` (styles des infos du héro).
- `ui/home` : `AppTile`, `HomeGrid` (rangées isolées, préparation différée du panneau), `ShelfPanel` (taille de décodage, rayon), `Dock`, `AppContextMenu` (rayons, vignette), `HomeScreen` (capsule de hauteur fixe, bandeau en pilule, visuels vérifiés transmis sans recomposer le héro masqué ni la grille).
- `ui/hero/HeroStage` (en-tête, ligne d'infos, temps restant), nouveau `domain/HeroCaption`, `model/HeroItem` (type, saison, épisode, durée, position), `data/TvProviderHeroSource` (colonnes lues), `data/AppIconCache` (icône carrée sans masque).
- `ui/settings` : `SettingsContent` (jetons des lignes et de la vignette), `AboutContent` (fond du code QR arrondi).
- `data/AppArtworkSource` (image préparée pour le dessin, copie floue inutilisée supprimée).
- Mise à jour : `domain/UpdateModels`, `domain/UpdateSelector`, nouveau `domain/DexMetadata`, `data/UpdateInstaller`, `data/PackageInstallerGateway`.
- Build et publication : `app/build.gradle.kts` (tâches `dexMetadataRelease` et `dexMetadataPerf`), `.github/workflows/release.yml`, `.gitignore`, `README.md`.
- Aucune nouvelle dépendance.

## Non-goals
- Refaire les mesures de la rc.6 au-delà de la grille, du héro et du démarrage après installation : faites avec la variante `perf` sur la TV (voir la PR).
- Rayons des surfaces sans imbrication (panneau Top Shelf 16 dp, bouton du héro et barre de progression en pilule, interrupteurs) : inchangés.
- Titre d'épisode, date de sortie ou classification dans la ligne d'infos : absents de la maquette.
- Signature fs-verity du fichier `.dm` : non vérifiée sur la TV pour la mise à jour intégrée (tâche 3.5) ; si une TV l'exige, la session portant le profil échoue et la relance sans profil installe la mise à jour.

## Décisions de Sygix
Validés après la relecture de la PR : libellé seul (type) quand le nom de l'app est inconnu ; arrondis (durée à la minute la plus proche, temps restant à la minute supérieure) ; rayon intérieur calculé sur la plus petite marge quand les marges diffèrent. Délai d'un visuel du héro avant de passer au programme suivant : environ 1 s (consigne de Sygix), fixé à 1 s.

## Application des règles de Sygix (à confirmer à l'œil sur la TV)

1. **Code QR d'« À propos »** : son fond blanc, jusqu'ici à angles droits, est arrondi à sa marge blanche (environ 11 dp) pour suivre la règle de concentricité. À valider.
2. **Icône de l'app source dans le héro** : 18 dp, coins de 4,5 dp repris de la maquette (pas d'imbrication) ; image carrée de l'icône sans le masque du système.

## Questions ouvertes
Aucune.
