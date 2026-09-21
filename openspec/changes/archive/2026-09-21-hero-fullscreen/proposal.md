# Change : hero-fullscreen

Héro plein écran sans cadre, diaporama automatique du contenu des apps, fallback vidéos nature libres de droits, navigation DPAD déterministe, tuiles bannière 16:9.

## Why
Le test sur la TV (Google TV, Android 14) de la branche `fix/tv-debug` montre une app inutilisable : héro toujours sur le fallback, navigation DPAD erratique, rendu éloigné de la cible tvOS. Causes racines identifiées :
- Le TV Provider ne renvoie que les lignes de notre propre package sans `android.permission.READ_TV_LISTINGS` (permission runtime absente du manifest) ; toute requête avec clause `selection` lève une SecurityException (panneau Top Shelf jamais affiché). Le héro n'est chargé qu'une fois, avant tout octroi de permission.
- Les trois couches (héro, dock, grille) restent focusables à alpha 0 : le focus saute sur des éléments invisibles ; haut depuis le dock ne rend pas le focus au héro ; focus initial non déterministe.
- Un ExoPlayer par page de carrousel, jamais libéré ; vidéos Apple de 200 à 700 Mo, URLs fragiles, non redistribuables.
- Posters http (Jellyfin LAN) bloqués par la politique cleartext ; posters portrait rognés plein cadre ; icônes carrées rognées en 1.2:1 au lieu des bannières TV.
- Chargement du héro et du TV Provider dans les composables, hors ViewModel.

Décision produit (Sygix) : le héro n'est pas un carrousel à cartes mais un fond plein écran, muet, qui défile tout seul façon Apple TV, le dock par-dessus ; la grille masque totalement le héro.

## What Changes
- **Héro plein écran** : plus de carte ni d'aperçu du voisin. Un seul visuel à la fois (vidéo d'aperçu du programme si publiée, sinon poster), plein écran, muet, zoom lent (Ken Burns), fondu croisé 700-1000 ms easing Apple, avance automatique ~8 s, gauche/droite = précédent/suivant, OK = ouverture du contenu. Titre, app source et progression en bas à gauche sur un dégradé de lisibilité.
- **Contenu des apps** : permission `android.permission.READ_TV_LISTINGS` déclarée et demandée au premier lancement ; rechargement du héro dès l'octroi. Requêtes TV Provider sans clause `selection`, filtrage par app côté client ; package lu directement sur les programmes ; `previewVideoUri` remonté.
- **Fallback vidéo** : `AerialHeroProvider` remplacé par une source de clips nature libres de droits (Pexels, 2K sinon 1080p, mp4 H.264 https), lus en boucle par un unique lecteur libéré au démontage, clip fautif ignoré ; fallback ultime : dégradé sombre animé. Plus aucune URL Apple.
- **Navigation** : machine d'états `zone` (héro / dock / grille) unique source de vérité ; seule la zone active est focusable (`focusProperties.canFocus`) ; transitions bas/haut explicites ; focus initial déterministe ; restauration de la dernière tuile de grille ; lecture héro en pause quand la grille est active.
- **Top Shelf** (retour de test TV) : panneau au-dessus de la rangée active au format Apple Top Shelf 2,67:1 pleine largeur, rangée active à hauteur fixe (la précédente dépasse en haut, les suivantes en dessous), glissement animé au changement de rangée ; masqué si aucun poster n'est lisible ; vidéos d'aperçu de type TV input (VLC) ignorées
- **Dock** (retour de test TV) : tuiles de la taille de la grille, alignées sur ses 5 colonnes (centrées si moins, réduites si plus) ; les apps épinglées restent dans la grille ; OK valide directement l'épinglage dans le menu
- **Tuiles** : 16:9 avec la bannière Android TV de l'app (`android:banner`), repli icône entière centrée sur fond sombre.
- **Qualité des visuels** (retour de test TV) : image ou vidéo d'au moins 960 px de large sinon programme écarté, vidéo d'aperçu privilégiée ; menu contextuel en overlay dans la fenêtre (plus de Dialog) pour que le focus revienne sur la tuile
- **Suite (v1.x, spécifiée, non implémentée)** : écran de réglages du launcher pour cocher les apps sources du héro et du Top Shelf
- **Liquid Glass réel** (retour de test TV) : flou d'arrière-plan RenderEffect (bibliothèque Haze) sous le dock et le menu, teinte claire, grain, liseré spéculaire et reflet lent animé ; repli translucide sans flou si indisponible
- **Réorganisation de la grille** (retour de test TV) : menu « Déplacer », flèches (case / rangée par insertion), OK valide, Retour annule, ordre persisté (DataStore), nouvelles apps en fin par ordre alphabétique
- **Réseau** : cleartext http autorisé (posters de serveurs LAN type Jellyfin), états loading/empty/error du héro.
- **Architecture** : `HomeViewModel` porte l'état héro (loading / ready / empty) ; plus aucune source de données instanciée dans un composable.
- **Ménage** : permission `READ_EPG_DATA` inutile retirée ; fuite ExoPlayer corrigée.

## Impact
- specs affectées : launcher-shell (REMOVED « Carrousel héro », ADDED « Diaporama héro » et « Fond vidéo de secours », MODIFIED « Écran initial du home », « Navigation 3 paliers », « Contenu héro TV Provider », « Grille d'apps », « Panneau Top Shelf au focus », « Contenu du panneau », « Dock d'apps épinglées », « Persistance de la grille » ; ADDED « Qualité des visuels », « Sélection des apps sources » (v1.x))
- manifest : `READ_TV_LISTINGS`, `usesCleartextTraffic` via network security config
- prérequis : archiver `focus-shelf` (implémenté, non archivé) pour que ses exigences entrent dans la spec courante
- APK : aucun média embarqué (clips en streaming)
