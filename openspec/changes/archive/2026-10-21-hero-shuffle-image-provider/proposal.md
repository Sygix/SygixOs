# Proposal

## Why

Les images manquaient dans le héro, le Top Shelf et À suivre, y compris après avoir déjà été affichées : le chargement ne conservait pas les octets des images `content://`, perdait l'alternative affiche/miniature et pouvait attendre un fournisseur bloqué. Le blocage du fournisseur Jellyfin avait déjà entraîné l'arrêt de SygixOs par dépendance forte au fournisseur.

La première implémentation de ce change (v0.3.1) a aggravé le symptôme sur la TV de référence : plus aucune image ne s'affiche et la vidéo nature redémarre sans cesse. Elle exigeait un type d'image que le fournisseur de Jellyfin ne déclare pas, comptait l'attente en file dans un délai de 8 s, imposait au Top Shelf la largeur du panneau, invalidait toutes les images à chaque événement et ne mettait pas les vidéos en cache (détail dans `design.md`). Cette révision corrige ces causes en gardant les objectifs du change.

Le change conserve aussi la décision de mélanger le héro à chaque lancement.

## What Changes

- Coil reste le moteur d'images. Un intercepteur choisit la candidate et les clés ; un fetcher unique lit les octets HTTP(S) (redirections suivies) et `content://` (sans exiger de type d'image) et les écrit dans le cache disque de Coil après vérification.
- Les récupérations de toutes les sources sont séquentielles par app, parallèles entre apps (3 au total). Seule la requête a un délai de 8 s, depuis son début effectif ; l'attente en file n'en consomme aucun. L'annulation ferme le flux ouvert.
- Un index local (Room) conserve programmes, candidates, images et associations, sans limite de nombre, avec migrations entre versions et repli par recréation. Les octets restent dans le cache de Coil.
- Les mises à jour sont pilotées par les événements du TV Provider et par l'échéance de chaque image : seule l'image concernée est récupérée ou remplacée, sans invalidation globale ni effacement au retour au premier plan. Un programme est reconnu par une empreinte indépendante de son identifiant de ligne et de sa progression.
- Fraîcheur des images inchangée : 24 h, secours jusqu'à 7 jours ; programmes confirmés par les lectures réussies et expirés à 24 h sans confirmation.
- Qualité : Top Shelf à partir de 640 px de large, paysage ; héro inchangé et limité aux programmes dont un visuel paysage acceptable ou une vidéo est prêt ; À suivre à la largeur de la carte. Les images génériques (tuile ou icône d'erreur d'une app, mêmes octets pour au moins 3 contenus distincts, ressource d'une autre app) sont traitées comme absentes.
- Héro : un programme dont l'image n'est pas prête est passé sans délai propre ; une requête en échec donne le repli nature puis neutre. Un même clip nature continue pendant les programmes en repli consécutifs et passe au suivant à sa fin.
- Les vidéos du héro sont mises en cache disque, jusqu'à 300 Mo selon l'espace libre, avec lecture en flux si le cache est impossible.
- L'ordre du héro est tiré au hasard une fois par lancement de SygixOs, stable pendant la session ; un programme apparu pendant la session est inséré au hasard après le programme affiché.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : source et confirmation des programmes, admission et repli du diaporama, vidéo de secours et son cache, contenu et seuil du Top Shelf, qualité, préchargement ; exigences communes de chargement, cache, images génériques, validité et ordre aléatoire.
- `up-next` : conservation des candidates d'image, seuil des cartes, mises à jour ciblées, états après expiration et rafraîchissement ; référence au parcours partagé de `launcher-shell`.
- `ui-testing` : vérification des trois zones, du partage, du parallélisme, des délais, des mises à jour ciblées, des images génériques, de la conservation entre versions et des vidéos.

La capability `settings` est examinée mais ses exigences ne changent pas : aucune préférence de cache, de délai ou de parallélisme n'est ajoutée ; ses vignettes de contenus retirés utilisent le même chargeur sans devenir un catalogue de programmes.

## Impact

Sources TV Provider, modèles de programmes et candidates, intercepteur et fetcher Coil, ordonnanceur, index Room, état exposé par les ViewModels, héro (admission, repli, lecteur vidéo), Top Shelf, cartes À suivre, vignettes des réglages, tri du héro, tests JUnit et Compose, README.

Nouvelles dépendances : Room et KSP (index local). Le cache vidéo utilise les modules media3 déjà présents. Les images HTTP(S) continuent d'être obtenues depuis les URI publiées.

Le change s'appuie sur les archives `hero-shelf-freshness`, `hero-visual-retry` et `visual-header-validation`, dont il remplace les règles de cache et de reprise concernées. Sa première implémentation a été livrée en v0.3.1 ; cette révision la remplace.

## Non-goals

- Corriger le fournisseur d'images de l'application source (hors SygixOs).
- Garantir la première récupération d'une image dont le fournisseur ne répond jamais, ou une conservation hors ligne illimitée.
- Inventer des URL, des dates d'expiration ou des identifiants de série non publiés ; les URI signées ne sont pas réécrites. L'enrichissement externe et BetaSeries restent hors périmètre ; BetaSeries sera traitée dans un change ultérieur.
- Choisir l'ordre du héro dans les réglages, mélanger À suivre ou le Top Shelf.
