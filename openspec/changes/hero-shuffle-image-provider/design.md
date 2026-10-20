# Design

## Context

Voir `proposal.md` pour les symptômes et le périmètre convenu. L'audit de v0.2.3 et v0.3.0-rc.1 retrouvait un parcours d'images dispersé : une seule URI choisie par programme, des validations séquentielles du héro et du Top Shelf, puis des chargements Coil séparés dans `HeroPosterLoader`, le préchargement de `HomeGrid`, `ShelfPanel` et `UpNextArtwork`, avec des listes d'échec locales qui ne distinguaient pas une panne temporaire d'un rejet durable. Coil 2.7.0 conserve les réponses HTTP dans son cache disque, mais son `ContentUriFetcher` lit les flux des fournisseurs sans copier leurs octets.

### Régression de la première implémentation (v0.3.1)

La première implémentation a remplacé le transport de Coil par un transport maison et une invalidation globale. Sur la TV de référence, plus aucune image ne s'affichait et le repli nature redémarrait sans cesse :

| Cause | Effet observé |
| --- | --- |
| Ouverture des `content://` avec un filtre de type `image/*` : l'implémentation par défaut de `ContentProvider.openTypedAssetFile` exige alors que le fournisseur déclare un type compatible. Le fournisseur d'images de Jellyfin (`ImageProvider`) n'en déclare aucun ; vérifié dans ses sources et sur la TV. | Toutes les images Jellyfin échouent dans les trois zones. |
| Transport HTTP maison qui ne suit pas les redirections de http vers https ; `android.resource://` non géré. | Images HTTP redirigées perdues. |
| Budget de 8 s compté depuis la demande, attente en file comprise, et empilé dans le validateur, l'intercepteur, le service et l'ordonnanceur. | Avec une lecture à la fois par app, les images d'une app au-delà des premières expirent avant d'être lues, puis sont relancées. |
| Annulation qui ne ferme pas le descripteur ni le flux ouverts. | Un `read` bloqué sur le tube d'un fournisseur garde son worker ; après trois blocages, plus aucune lecture. |
| Seuil du Top Shelf égal à la largeur du panneau (environ 1 728 px en 1080p). | Presque toutes les images publiées sont refusées. |
| Programmes sans visuel paysage conservés dans le héro. | Les programmes à affiche portrait passent tous sur la vidéo nature. |
| Clip nature identifié par le programme, joué en boucle sur lui-même, fin de clip reliée à l'avance du programme, minuteur de 12 s actif en repli. | Le clip est rechargé et repart du début toutes les 12 s. |
| Aucun cache disque pour les vidéos. | Chaque clip 2K est retéléchargé à chaque lecture. |
| Compteur global de changements incrémenté au retour au premier plan, à chaque échec de décodage, à chaque rafraîchissement et à chaque échéance ; chaque incrément vide toutes les validations, le fond du héro et les cartes. | Clignotement au retour sur l'accueil ; un échec répété relance tout en boucle et les chargements sont annulés avant d'aboutir. |
| Empreinte de programme incluant l'identifiant de ligne et la progression. | Chaque mise à jour de progression ou republication fait revalider l'image. |
| Associations programme/image limitées à 64 et stockées dans les métadonnées du cache, avec une entrée « version précédente ». | Logique fragile, difficile à faire évoluer entre versions. |

Cette révision conserve les objectifs du change et corrige ces causes. Les décisions D1 à D7 ci-dessous remplacent celles de la première implémentation.

### Sources et données réellement connues

| Source | Données utilisables | Limites et vérification |
| --- | --- | --- |
| `PreviewPrograms` et `WatchNextPrograms` | `_ID`, `package_name`, titre, affiche et ratio déclaré, miniature, aperçu vidéo, intent, saison/épisode, durée/position, identifiants de contenu et de série quand l'app les publie ; engagement et type watch next pour ce dernier | Aucune échéance de programme ou d'URI n'est lue. Certaines apps suppriment puis réinsèrent leurs programmes avec de nouveaux `_ID`. Le shell adb ne peut pas lire ces tables : la vérification passe par SygixOs. |
| Fournisseur d'images tiers | URI `content://`, flux d'octets, type éventuel | Jellyfin publie `content://<app>.integration.provider.ImageProvider?src=<URL de l'image sur le serveur>&v=<version>`, unique par élément et par image ; il ne déclare aucun type, sert un tube de longueur inconnue et renvoie une icône à la place de l'image quand son serveur échoue. Les éléments sans image partagent une tuile générique. |
| Image HTTP(S) | URI exacte publiée, réponse et octets | Une URI peut être signée, rediriger ou expirer ; ne pas la reconstruire. |
| Vidéos nature | Playlist HTTPS de `NatureFallbackProvider`, clips 2K | Elles peuvent être injoignables ; le dégradé existant est le dernier recours local. |

## Goals / Non-Goals

**Goals:**
- Coil reste le moteur d'images ; un seul chargeur, un seul cache disque et des clés communes aux trois zones.
- Aucun appel vers une app ne bloque le rendu, les images des autres apps ou la navigation.
- Chaque image est un objet suivi individuellement : elle n'est récupérée, remplacée ou expirée que pour elle-même, sur événement ou échéance.
- Les images locales aident pendant une panne sans prolonger la publication d'un programme ni la vie d'une image au-delà des délais acceptés.

**Non-Goals:**
- Aucun nouveau réglage ni nouvelle source distante.
- Pas de moteur générique de travaux ni de partage de films basé sur leur seul titre. Les bannières d'apps via `PackageManager` restent hors des files des programmes.

## Decisions

### D1. Coil comme moteur, un intercepteur et un fetcher

Les trois zones passent un `ProgramArtwork` (candidates et identité du programme) au chargeur Coil de l'application, avec la politique de leur usage.

- **Intercepteur** : résout la candidate à partir de l'index (D3) et de la politique, puis réécrit la requête vers l'URI choisie avec une clé mémoire explicite (URI + version des octets + taille + transformations) et la clé disque. Il ne lit aucun octet.
- **Fetcher unique des images de programmes** : lit les octets et les écrit dans le cache disque de Coil, comme le fetcher HTTP de Coil (instantané existant, sinon éditeur, écriture, `commitAndGet`).
  - `http(s)` : OkHttp (déjà fourni par Coil), redirections suivies y compris de http vers https, délai d'appel total.
  - `content://` : `ContentProviderClient` instable, `openAssetFile(uri, "r", signal)`, sans filtre de type ; lecture jusqu'à la fin du flux sans se fier à sa longueur, plafond de 16 Mo.
  - `android.resource://` d'une autre app : jamais lue, image générique (D5). `file://` et ressources propres passent par les fetchers de Coil.
- Avant `commit`, le fetcher vérifie l'en-tête et un décodage réduit ; un flux tronqué, une page d'erreur ou des octets indécodables ne sont jamais engagés. L'ancienne copie reste donc intacte sans entrée « version précédente ».
- La clé disque est l'URI exacte publiée, comme pour Coil HTTP : une même URL publiée par deux apps n'est conservée qu'une fois.
- `respectCacheHeaders(false)` reste : l'index porte le TTL. Un rafraîchissement d'échéance passe `diskCachePolicy(WRITE_ONLY)` sur sa requête.
- Les travaux en cours sont partagés par clé : une seconde zone rejoint la lecture déjà en cours au lieu d'échouer sur un éditeur occupé.
- Les dimensions viennent de l'en-tête du fichier engagé et sont écrites dans l'index. `VisualValidator` lit l'index et applique les politiques pures, sans requête Coil dédiée ni décodeur de bornes.
- `SettingsContent` (vignettes des contenus retirés) utilise le même chargeur et les mêmes clés.

Alternative écartée : un transport maison complet (v0.3.1), qui a perdu les comportements éprouvés de Coil.

### D2. Files par app et délai de requête

Toutes les images de programmes, quel que soit le schéma, passent par l'ordonnanceur : une lecture physique à la fois par app, `MAX_PARALLEL_APPS = 3` au total, file bornée. Un hit du cache disque ne prend aucune place.

- Le délai `IMAGE_REQUEST_TIMEOUT_MS = 8_000` court depuis le début effectif de la requête (connexion ou ouverture du fournisseur), jamais pendant l'attente en file. Il n'est pas empilé ailleurs : ni validateur, ni intercepteur, ni zone n'ajoute son propre délai.
- Dans une file d'app, les demandes d'une zone affichée passent avant les validations anticipées ; une demande sans consommateur est retirée avant son début.
- À l'échéance, l'échéancier fait échouer la requête et une voie d'annulation distincte du worker et du fil d'échéances annule le signal, déconnecte l'appel HTTP et **ferme le descripteur et le flux déjà ouverts** (référence atomique), ce qui débloque un `read` sur un tube. Si l'appel Binder ne revient pas, le worker reste en quarantaine jusqu'à sa sortie réelle et aucune autre lecture de cette app ne démarre ; les autres apps utilisent les places restantes, la saturation totale échoue rapidement.
- Un résultat tardif est fermé exactement une fois, sans publication ni engagement.
- Le délai, `DeadObjectException`, `RemoteException`, `SecurityException` et `FileNotFoundException` restent distingués.
- Le héro ne porte aucun délai d'image propre : un programme dont l'image n'est pas prête est passé ; il entre dans le diaporama quand elle l'est, ou avec son repli si la requête échoue.

### D3. Index Room des programmes et des images

Une base Room privée est l'index des programmes et des images ; les octets restent dans le cache disque de Coil. Room (et KSP) est la seule nouvelle dépendance, acceptée pour sa robustesse entre versions.

| Table | Contenu |
| --- | --- |
| `programs` | clé `(table, _id)` du dernier instantané, package, type, empreinte d'image, empreinte complète, identité partagée (série ou film), date de dernière confirmation, expiration |
| `candidates` | programme, URI, rang (affiche, miniature, variante YouTube) |
| `images` | id, package, URI, clé disque, largeur, hauteur, hash des octets, date de récupération, état (`valid`, `generic`, `failed`), date de nouvelle tentative |
| `associations` | image, empreinte d'identité, identité partagée, date de confirmation |

- **Empreinte d'image** : package, table, intent, titre, saison, épisode, identifiants de contenu, interne et de série, candidates. Elle exclut `_ID`, position, engagement et durée. Seule sa variation déclenche une récupération ; l'empreinte complète ne fait que rafraîchir les textes.
- **Garde-fou** : une identité nouvelle sur une image connue impose une relecture, sauf si son identité partagée est déjà associée. Des octets identiques ajoutent l'association sans nouvelle analyse. Aucune limite de nombre d'associations.
- **Room est un index, pas la vérité** : un instantané disque absent (éviction LRU de Coil) remet l'image à récupérer ; une purge de l'index supprime l'entrée disque.
- **Purge** au démarrage sur IO et après chaque comparaison : programmes non confirmés depuis 24 h, images sans association vivante ou de 7 jours et plus.
- **Migrations** écrites et testées à chaque changement de schéma, avec schémas exportés ; si une migration manque ou échoue, repli destructif : l'index est recréé vide et se reconstruit depuis le TV Provider.
- **Threading** : DAO `suspend`, écritures en transaction, aucune requête sur le fil principal ; l'ouverture de la base ne retarde pas l'écran de démarrage.
- **Outillage** : vérifier la version KSP correspondant à Kotlin 2.4.x et la compatibilité de Room avec l'AGP du projet avant l'implémentation ; tests Room en base mémoire sous Robolectric.

### D4. Mises à jour par événements

Le compteur global de changements et les paramètres de révision des requêtes sont supprimés.

- Le `ContentObserver` existant sur les deux tables (debounce 500 ms à 1 s) relit les programmes. La comparaison avec l'index se fait sur l'empreinte d'image, jamais sur `_ID` :
  - programme absent : retiré des zones et marqué non confirmé ;
  - nouveau programme : candidates résolues dans l'index, image prête immédiatement ou récupérée ;
  - empreinte d'image changée : seule son image est récupérée ;
  - inchangé : rien, pas même un nouveau painter.
- Un seul minuteur suit la prochaine échéance d'image et émet un événement pour cette image seule.
- Les composants s'abonnent par clé à un flux d'événements typés (`ImageReady`, `ImageFailed`, `ProgramsChanged`) ; les clés Compose sont l'empreinte d'image et la version des octets. Une nouvelle version remplace l'ancienne à l'écran seulement quand elle est prête.
- Retour au premier plan : relecture et comparaison en arrière-plan, contrôle des échéances, sans rien effacer. La relecture périodique de 15 minutes reste pour un launcher toujours au premier plan.

### D5. Qualité et images génériques

Les politiques restent des fonctions pures appliquées aux dimensions de l'index :
- héro : règle courante de « Qualité des visuels » (paysage, au moins 640 px après réduction dans 1920 × 1080) ;
- Top Shelf : paysage, au moins 640 px de large réels ;
- À suivre : largeur au moins égale à celle de la carte affichée en pixels hors zoom, portrait accepté.

Le héro n'admet un programme que si une vidéo ou un visuel accepté est prêt, ou si sa récupération a échoué (repli). Un programme dont toutes les candidates sont refusées par la qualité ou génériques en est écarté.

Une image est **générique** et traitée comme absente si elle est refusée par toutes les politiques, si ses octets (hash) sont publiés par une même app pour au moins 3 identités partagées distinctes, ou si c'est une ressource `android.resource://` d'une autre app. Elle n'est jamais conservée comme valide et sa nouvelle tentative a lieu au plus tôt une heure après. Les épisodes d'une même série et un même film publié dans les deux tables comptent pour une identité.

### D6. Fraîcheur de l'image distincte de la validité du programme

| Objet | Horodatage de référence | Politique |
| --- | --- | --- |
| Image | Dernière récupération complète réussie | Fraîche moins de 24 h ; de 24 h à moins de 7 jours, affichage immédiat puis rafraîchissement de cette image seule ; conservée si celui-ci échoue ; à 7 jours, plus affichée sans récupération réussie. |
| Programme | Dernière lecture réussie de sa table le confirmant | Retiré dès qu'une lecture réussie confirme son absence ; conservable moins de 24 h si la lecture échoue ; à 24 h sans confirmation, retiré des surfaces actives. |

Lire, afficher ou valider depuis le cache ne renouvelle aucune date. Une date absente ou future n'est pas fraîche par défaut. Une erreur de table n'est pas une liste vide. Une publication tardive porte la génération de son programme et ne réintroduit rien.

### D7. Héro, vidéos et ordre

- **Clip nature** : identifiant indépendant du programme (`nature-<index>`), lecture sans boucle sur lui-même. La fin d'un clip passe au clip suivant ; la fin d'une vidéo d'aperçu garde son comportement d'avance du programme. Plusieurs programmes en repli consécutifs gardent le même clip ; un programme avec image ou vidéo arrête le lecteur et le clip repart du début au repli suivant.
- **Cache vidéo** : un singleton applicatif `SimpleCache(dir, LeastRecentlyUsedCacheEvictor(max), StandaloneDatabaseProvider)`, créé sur IO, jamais dans `HeroPlayer` (une seule instance par dossier, alors que le lecteur est recréé à chaque retour au premier plan). `max = min(300 Mo, espace utilisable / 4)` ; sous 64 Mo, pas de cache. Lecture par `CacheDataSource` avec `FLAG_IGNORE_CACHE_ON_ERROR` : toute erreur de cache retombe en flux. Les tests injectent une source sans cache.
- **Ordre aléatoire** : graine par processus, ordre stable par empreinte de programme ; un nouveau programme est inséré au hasard après le programme affiché, un programme republié garde sa place.
- **Journalisation** : chaque échec d'image ou de vidéo est journalisé avec l'app, le schéma, l'autorité et la cause, jamais l'URI complète ni ses paramètres (l'URI Jellyfin contient l'adresse du serveur).

## Risks / Trade-offs

- [Fournisseur qui ignore l'annulation et la fermeture] → Quarantaine par app jusqu'à la sortie réelle, échec rapide à saturation, jamais de thread supplémentaire.
- [Files longues] → Sans délai d'attente, une app qui publie beaucoup d'images est servie progressivement ; priorité aux zones affichées et retrait des demandes sans consommateur.
- [Heuristique d'image générique] → Faux positif possible pour une app publiant la même image pour des contenus distincts ; seuil de 3 identités, nouvelle tentative après une heure, vérification sur la TV.
- [Index et cache désynchronisés] → L'index ne fait jamais foi pour les octets : instantané absent, image à récupérer.
- [Espace disque] → Cache vidéo dimensionné sur l'espace libre et désactivable sans erreur.

## Migration Plan

La révision remplace le transport, le service et le stockage d'associations de v0.3.1. Les entrées du cache disque créées par v0.3.1 (format `artwork-v2` et entrées « previous ») ne sont pas lues par l'index : elles sont supprimées au premier démarrage, puis les images sont récupérées de nouveau. Les préférences, épinglages, contenus retirés et la mémoire d'ouverture ne changent pas. Le README est mis à jour pour le seuil de 640 px du Top Shelf, le cache vidéo et les images génériques.

### Ordre d'archivage

Après merge de l'implémentation et validation sur la TV : `openspec archive hero-shuffle-image-provider`. Les réserves TV de l'archive Tsuyu restent indépendantes.

## Vérification de la première implémentation (v0.3.1, historique)

Ces résultats concernent l'implémentation remplacée par cette révision ; ils ne valent pas preuve pour elle.

- `./gradlew test` : 1 061 tests réussis ; `assembleRelease` réussi.
- Émulateur Android 14 TV : lancement, navigation, épisode synthétique dans À suivre avec placeholder.
- Sur la TV de référence : aucune image affichée dans les trois zones, repli nature redémarrant, causes listées dans « Régression de la première implémentation ».
