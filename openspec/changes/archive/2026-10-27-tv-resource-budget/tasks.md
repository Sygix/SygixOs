# Tasks

Les tâches cochées sont vérifiées par un test nommé ; les tâches 1.x et 5.x exigent la TV réelle en `assembleRelease`.

Les mesures TV des tâches 1.1 et 5.1 ont été dispensées pour cet archivage ; leurs résultats et la comparaison de fluidité et de mémoire restent non vérifiés.

## 1. Référence sur la TV

- [ ] 1.1 Remesurer la référence de `design.md` sur `main` (accueil au repos, grille avec panneau, arrière-plan, navigation grille, mémoire, threads) avec le protocole de mesure ; critère : valeurs consignées dans la PR d'implémentation, dans les mêmes unités.

## 2. Mouvement des visuels

- [x] 2.1 Préférence « Mouvement des visuels » dans DataStore, activée par défaut, combinée à l'échelle d'animation du système en un seul drapeau de mouvement ; critère : JUnit sur la valeur par défaut, la persistance et la combinaison.
- [x] 2.2 Ligne switch dans « Écran d'accueil » sous « Position d'À suivre », chaînes FR « Mouvement des visuels » et « Zoom lent des images du héro et du Top Shelf », testTag `setting-visual-motion` ; critère : test Compose à la taille TV (w960dp-h540dp-xhdpi) sur la bascule, le focus conservé et la navigation haut, bas, gauche et Retour.
- [x] 2.3 Héro et panneau Top Shelf sans Ken Burns ni panoramique quand le drapeau est faux, fondus et défilement inchangés ; critère : test qui vérifie l'échelle 1 du visuel et l'absence d'image redessinée entre deux changements de visuel, sous horloge de test.
- [x] 2.4 README : mention du réglage dans la description des réglages.

## 3. Optimisations sans changement visible

- [x] 3.1 Verre du dock et de la capsule sans zoom (D2) ; critère : test `GlassBackdrop` qui vérifie que le zoom n'invalide pas le dessin du verre et que le fondu est suivi.
- [x] 3.2 Affiches du héro en `memoryCachePolicy(DISABLED)` (D3) ; critère : test sur la requête construite et sur la relecture d'un programme déjà vu depuis le cache disque.
- [x] 3.3 Compteurs des réglages collectés seulement réglages ouverts (D4) ; critère : test qui compte zéro requête de compteurs tant que les réglages sont fermés.
- [x] 3.4 Une lecture d'À suivre par événement ; observateur conservé pendant l'arrêt avec drapeau de changement ; relecture au retour seulement si le drapeau est levé ou si le TTL du héro est échu (D5) ; critère : tests qui comptent les requêtes au TV Provider pour un retour sans changement (0), un retour avec changement (1 par zone) et un événement (1 lecture d'À suivre).
- [x] 3.5 ExoPlayer sans piste audio (D6) ; critère : test sur les paramètres du sélecteur de pistes du lecteur créé.
- [x] 3.6 Clips nature en 1920 × 1080 (D7) ; critère : test sur la liste des URLs (aucune en 2560 × 1440).
- [x] 3.7 Top Shelf : validation par en-tête sans décodage à la taille du panneau, décodage de la seule affiche affichée, décodage anticipé après 0,5 s de focus posé (D8) ; critère : tests qui comptent les décodages pour une traversée rapide (0) et pour un focus posé (1).
- [x] 3.8 Un seul ordonnanceur d'images pour l'index et les requêtes au TV Provider ; `schedule()` regroupé ; `purge()` en une requête de sélection (D9) ; critère : tests de l'ordonnanceur et de l'index (orphelins et périmés purgés, nombre de lectures de table par rafale de `resolve`).
- [x] 3.9 Badges À suivre à 36 dp (D9) ; critère : test sur la taille du bitmap produit.
- [x] 3.10 Sous une vidéo rendue et son fondu terminé : pas de dégradé de repli et `TextureView` opaque (D10) ; critère : test d'état du héro sur ces deux conditions.

## 4. Vérification

- [x] 4.1 `./gradlew test assembleRelease` et `openspec validate --all --strict` verts.

## 5. Validation TV et archive

- [ ] 5.1 Remesurer sur la TV avec le réglage activé puis désactivé ; critère : réglage désactivé, l'accueil au repos et la grille avec panneau ne redessinent que les fondus ; avec le réglage activé, ni régression de fluidité ni hausse de mémoire par rapport à 1.1 ; résultats consignés dans la PR.
- [ ] 5.2 Archiver `tv-resource-budget` après le merge et la validation sur TV.
