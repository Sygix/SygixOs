# Proposal

## Why

Les images manquent dans le héro, le Top Shelf et À suivre, y compris après avoir déjà été affichées : le chargement actuel ne conserve pas les octets des images `content://` dans le cache disque de SygixOs, perd l'alternative affiche/miniature et peut attendre un fournisseur bloqué. Le blocage du fournisseur Jellyfin a déjà entraîné l'arrêt de SygixOs par dépendance forte au fournisseur ; les URI réellement publiées par Disney+ et Crunchyroll restent à vérifier sur la TV.

Le change conserve aussi la décision de mélanger le héro à chaque lancement. Il regroupe désormais la fiabilité, la mutualisation et la péremption des images des trois zones, pour qu'une panne d'une app n'empêche pas les autres de fonctionner et qu'un cache ne maintienne pas de vieux programmes indéfiniment.

## What Changes

- Un parcours commun au héro, au Top Shelf et à À suivre mutualise récupération, analyse des dimensions, résultats en cours, cache, délais et nouvelles tentatives. Les règles de qualité restent propres à l'usage ; les décodages restent adaptés à chaque taille d'affichage.
- Les récupérations sont séquentielles par app, parallèles entre apps, avec une limite globale constante. L'accès `content://` utilise une référence instable ; l'ouverture et la lecture du flux sont bornées, même si un appel Android ignore l'annulation. Les accès HTTP suivent le même contrat de délai et de cache.
- Les images obtenues sont conservées sur disque, y compris celles des fournisseurs tiers. Une copie locale utilisable s'affiche immédiatement ; elle est fraîche pendant 24 h et peut servir en secours jusqu'à 7 jours après la dernière récupération réussie si son programme reste valide. Les affiche et miniature publiées sont conservées comme candidates.
- Les programmes sont confirmés par les lectures réussies du TV Provider : les absents sont retirés, les anciens résultats ne restent utilisables que 24 h sans confirmation. Ces échéances s'appliquent aussi aux contenus et images déjà à l'écran ou en mémoire ; une réponse tardive ne les réintroduit pas.
- Dans le héro, si aucun visuel du programme n'est utilisable à l'échéance, une vidéo nature puis, si elle est illisible, le fond neutre assurent le repli en conservant le titre et l'ouverture du bon programme. Le défilement se poursuit. Le Top Shelf ne montre que les images prêtes de l'app focusée ; À suivre conserve son placeholder en dernier recours.
- À suivre accepte une image à partir de la largeur affichée de la carte, au lieu du double ; le Top Shelf accepte une image paysage à partir de la largeur affichée du panneau. Ces largeurs sont mesurées en pixels hors zoom ; les seuils du héro restent inchangés.
- L'ordre du héro est tiré au hasard une fois par lancement de SygixOs, stable pendant la session ; un programme apparu pendant la session est inséré au hasard après le programme affiché.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : source et confirmation des programmes, repli du diaporama, contenu du Top Shelf, qualité, préchargement et cache ; exigences communes de chargement résilient, validité et ordre aléatoire.
- `up-next` : conservation des candidates d'image, seuil des cartes, états après expiration et rafraîchissement ; référence au parcours partagé de `launcher-shell`.
- `ui-testing` : vérification des trois zones, du partage des récupérations et des dimensions, du parallélisme, des délais, des reprises et de l'expiration.

La capability `settings` est examinée mais ses exigences ne changent pas : aucune préférence de cache, de délai ou de parallélisme n'est ajoutée ; les listes de contenus retirés restent des archives utilisateur, pas des programmes remis en circulation par le cache.

## Impact

Sources TV Provider, modèles de programmes et candidates, service d'images partagé et intégration Coil, état de validité exposé par les ViewModels, chargement du héro et du Top Shelf, cartes À suivre, tri du héro, tests JUnit et Compose, README. Aucune dépendance externe nouvelle prévue ; aucune nouvelle intégration d'API métier ni compte ajouté. Les images HTTP(S) continuent d'être obtenues depuis les URI publiées.

Le change s'appuie sur les archives `hero-shelf-freshness`, `hero-visual-retry` et `visual-header-validation`, dont il remplace les règles de cache et de reprise concernées. Sa base est la correction des specs existantes de la PR #81, au-dessus de l'archivage `tsuyu-home-setup` (PR #80). Ordre d'intégration : #80, #81, cette spec, puis implémentation images ; les réserves de validation TV de Tsuyu restent indépendantes.

## Non-goals

- Corriger le blocage du fournisseur d'images de l'application source (hors SygixOs).
- Garantir la première récupération d'une image dont le fournisseur ne répond jamais, ou une conservation hors ligne illimitée malgré les expirations et l'éviction du cache borné.
- Inventer des URL, des dates d'expiration ou des identifiants de série non publiés ; les URI signées ne sont pas réécrites. L'enrichissement externe et BetaSeries restent hors périmètre, en P9 pour ce dernier.
- Choisir l'ordre du héro dans les réglages, mélanger À suivre ou le Top Shelf.
