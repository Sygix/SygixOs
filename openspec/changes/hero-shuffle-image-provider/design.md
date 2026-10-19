# Design

## Context

Voir `proposal.md` pour les symptômes et le périmètre convenu. L'audit de v0.2.3 et v0.3.0-rc.1 retrouve le même parcours d'images : `TvProgramFields.landscapeImage` choisit une seule URI ; `HomeViewModel` valide les candidates du héro et du Top Shelf séquentiellement ; `HeroPosterLoader`, le préchargement de `HomeGrid`, `ShelfPanel` et `UpNextArtwork` chargent ensuite via Coil. `ShelfPanel.failed` et `HeroStage.failedImages` ne distinguent pas une panne temporaire d'un rejet durable.

Coil 2.7.0 conserve les réponses HTTP dans son cache disque mais son `ContentUriFetcher` lit les flux des fournisseurs sans y copier les octets. La validation des en-têtes utilise un drawable sans pixels : elle ne constitue donc pas un cache mémoire de l'image et une ouverture ultérieure du fournisseur peut être nécessaire. `VisualFreshness` ne traite actuellement que les URI HTTP et attend le réseau avant de servir une ancienne entrée.

### Sources et données réellement connues

| Source | Données utilisables | Limites et vérification |
| --- | --- | --- |
| `PreviewPrograms` et `WatchNextPrograms` | `_ID`, `package_name`, titre, affiche et ratio déclaré, miniature, aperçu vidéo, intent, saison/épisode, durée/position ; engagement et type watch next pour ce dernier | Aucune échéance de programme ou d'URI n'est actuellement lue. Les colonnes facultatives et les schémas d'URI varient selon l'app. La tâche 1.1 vérifie leur remplissage, sans présumer une colonne d'expiration. |
| Fournisseur d'images tiers | URI `content://`, octets et éventuellement type MIME ; Jellyfin utilise un `ImageProvider` qui peut contacter son serveur | Les URI de Disney+ et Crunchyroll sont une hypothèse à vérifier, pas une condition de fonctionnement. Le fournisseur peut bloquer à l'ouverture, à la lecture du flux ou à une demande de type MIME. |
| Image HTTP(S) | URI exacte publiée, réponse et octets | Une URI peut être signée ou expirer ; ne pas la reconstruire et ne pas déduire la validité du programme de son erreur HTTP. |
| Cache privé de SygixOs | Octets, dimensions, empreinte de version, instant de récupération réussie et associations aux programmes | La lecture ou l'affichage ne renouvelle aucune échéance. Aucune date manquante n'est remplacée par l'heure courante. |
| Vidéos nature existantes | Playlist HTTPS de `NatureFallbackProvider`, sans titre propre | Elles peuvent être injoignables ; le dégradé existant est le dernier recours local. |

## Goals / Non-Goals

**Goals:**
- Une seule autorité de récupération, de métadonnées, de validité et de reprise, consommée par les trois zones.
- Aucun appel vers une app ne bloque le rendu, les images des autres apps ou la navigation ; les flux et leurs résultats tardifs restent bornés et maîtrisés.
- Les images locales aident pendant une panne sans prolonger la publication d'un programme ni la vie d'une image au-delà des délais acceptés.

**Non-Goals:**
- Aucun nouveau réglage ni nouvelle source distante ; le cache ne devient pas un catalogue de programmes.
- Pas de moteur générique de travaux, de score de priorité ou de partage de films basé sur leur seul titre. Les décodages de bannières d'apps via `PackageManager` restent hors des files des programmes.

## Decisions

### D1. Parcours d'images partagé, politiques par usage

Un service d'images à portée application reçoit la source, les candidates publiées, l'identité/version du programme et l'usage demandé. Il expose des états typés de chargement, succès, absence, rejet de qualité, refus d'accès, délai et panne temporaire. Les ViewModels les projettent vers l'UI ; les composables ne pilotent ni files, ni expiration, ni blacklist locale.

La récupération des octets, l'analyse des dimensions réelles et leur version sont mutualisées, y compris entre validation, préchargement et affichage. Un travail en cours par clé est partagé entre abonnés ; quitter une zone n'annule pas celui encore utilisé ailleurs. Une erreur réseau n'est pas transformée en état vide. Les images de contenus des menus et des réglages qui utilisent le même chargeur ne doivent pas ouvrir un chemin `content://` tiers non protégé ; leurs listes historiques ne deviennent pas un catalogue actif.

Le résultat commun contient les dimensions, pas un verdict universel. Les politiques sont des fonctions pures :
- héro : règle courante de « Qualité des visuels » ;
- Top Shelf : paysage, largeur réelle au moins égale à celle du panneau affiché en pixels, hors zoom ;
- À suivre : largeur réelle au moins égale à celle de la carte affichée en pixels, hors zoom, portrait accepté.

La mesure commune vient de l'en-tête sans décodage de pixels ; le calcul de qualité du héro peut ramener ces dimensions dans sa borne courante. Le seuil du Top Shelf peut être plus exigeant que les 640 px historiques sur un panneau pleine largeur. Un refus pour un usage ne bloque pas les autres. Les bitmaps et transformations restent en cache mémoire par version, taille et transformation ; ils sont décodés à la taille de destination, avec la limite de décodeurs de « Préchargement et mémoire ».

Alternative écartée : trois chargeurs ou un booléen global « image valide », qui dupliqueraient les ouvertures ou feraient rejeter une bonne vignette après son rejet dans le héro.

### D2. Files par app et concurrence bornée

Une file séquentielle par package source couvre toutes les demandes de programmes de cette app, quel que soit le schéma d'URI ou la zone. Un ordonnanceur équitable entre apps applique `MAX_PARALLEL_APPS = 3` au total, pas par composable ni par chargeur ; une app n'a jamais deux récupérations physiques simultanées. Les hits de cache utilisables ne prennent pas de place de récupération.

Les candidates sont bornées par les limites actuelles du héro, du Top Shelf et d'À suivre, avec une file d'attente globale bornée. Une demande qui n'a plus d'abonné est retirée avant son démarrage. Le budget de résultat d'une demande, attente en file comprise, est `IMAGE_LOAD_TIMEOUT_MS = 8_000` ; partager un travail en cours ou passer à une autre candidate ne remet pas ce budget à zéro. Ces constantes techniques initiales sont injectables dans les tests et vérifiées sur la TV.

Le démarrage garde sa priorité existante : dès qu'un premier visuel est utilisable, aucune nouvelle validation de prélecture n'est démarrée avant la fin de l'écran de démarrage ; les quelques travaux déjà en cours restent bornés par la limite globale. Un démarrage lent d'une app n'empêche pas un premier visuel d'une autre app d'être prêt.

Alternative écartée : un `awaitAll` sur toutes les images ou un sémaphore distinct dans chaque zone, qui maintiendrait une attente collective ou multiplierait la concurrence.

### D3. Fournisseurs isolés et délai sur le flux complet

Le récupérateur tiers acquiert un `ContentProviderClient` instable et appelle `openTypedAssetFile(uri, "image/*", null, signal)` sur un pool de workers borné. Il copie ensuite le flux vers une entrée temporaire du cache. L'ouverture, la lecture et les éventuels appels MIME sont couverts par la même échéance indépendante. Le transport HTTP(S) respecte aussi un délai total, au-delà des délais de connexion et de lecture.

L'échéancier complète le résultat en échec à l'échéance et délègue l'annulation à une voie bornée distincte, hors du worker bloqué et du fil d'échéances : une annulation Binder bloquante ne doit pas empêcher les délais suivants. `withTimeout` autour d'un appel Binder ou d'un `read` synchrone ne suffit pas : le worker et ses ressources restent en quarantaine jusqu'à la sortie réelle. Aucune nouvelle récupération vers la même app n'est démarrée pendant ce blocage, même après un rafraîchissement. Les autres apps utilisent les places physiques restantes ; la saturation totale échoue rapidement sans créer de threads supplémentaires.

Après un résultat tardif, le worker ferme le flux, le descripteur et le client exactement une fois, abandonne l'entrée temporaire et ne publie rien. Le délai, `DeadObjectException`, `RemoteException`, `SecurityException` et `FileNotFoundException` sont distingués dans les résultats. Une permission refusée ou un fournisseur absent aboutit au repli sans crash ni contournement des autorisations. Les lectures du TV Provider système restent des opérations distinctes, avec résultat explicite et délai borné.

Alternative écartée : `ContentResolver.openInputStream` pour un fournisseur tiers, qui conserve une référence forte, ou libérer fictivement un worker dont l'appel n'est pas revenu.

### D4. Cache de sources versionné et candidates conservées

Un cache privé borné, partagé avec le chargeur, conserve les octets sources HTTP(S) et `content://` ainsi que les dimensions et `fetchedAt`. Le budget disque global reste celui de l'application, sans ajouter un second cache de même taille. Une récupération réussie n'est publiée qu'après copie complète, vérification d'un en-tête d'image et engagement atomique de l'entrée ; un échec ne remplace pas la dernière copie valide. Une nouvelle version validée seulement par son en-tête ne détruit pas la dernière copie connue comme décodable avant son premier décodage réussi ; si ce décodage échoue, elle est invalidée et l'ancienne copie encore dans son TTL reste disponible.

La clé d'octets contient le package source et l'URI publiée exacte, sous forme d'empreinte pour le stockage. Les associations aux programmes incluent une empreinte des champs d'identité réellement disponibles, pas seulement le titre ni le numéro de ligne réutilisable du provider. Une association incompatible avec l'identité confirmée impose une revalidation de l'URI, même inchangée, plutôt qu'un hit aveugle sur les octets d'un ancien film. Un film ne reçoit pas l'image d'un autre film. Les épisodes peuvent partager les octets de la même image de série explicitement publiée ; aucun rapprochement de série à partir d'un champ non vérifié n'est requis. Les URL signées ne sont ni normalisées ni tronquées pour forcer un hit.

Les modèles conservent affiche, miniature et ratio déclaré. Les candidates sont essayées dans l'ordre pertinent pour l'usage, sans doublon : paysage d'abord, portrait autorisé seulement pour la carte ; une préférence de ratio déclaré ne supprime pas l'alternative. Les dimensions réelles font autorité. La variante YouTube existante reste une candidate prioritaire pour les usages qui la demandent, sans imposer une récupération haute définition inutile à une carte déjà satisfaite.

Alternative écartée : ne conserver que l'URI choisie par `landscapeImage`, ou utiliser la clé de série/film d'À suivre comme clé universelle d'image.

### D5. Fraîcheur de l'image distincte de la validité du programme

| Objet | Horodatage de référence | Politique |
| --- | --- | --- |
| Image | Dernière récupération complète réussie | Fraîche moins de 24 h ; de 24 h à moins de 7 jours, affichage immédiat de la copie utilisable puis rafraîchissement borné ; conservée si celui-ci échoue ; à 7 jours, ne plus l'afficher sans récupération réussie. |
| Programme | Dernière lecture réussie de sa table du TV Provider le confirmant | Retiré dès qu'une lecture réussie confirme son absence ou son caractère non affichable ; conservable moins de 24 h si la lecture échoue ; à 24 h sans confirmation, retiré des surfaces actives. |

Lire une entrée disque, la montrer, la vérifier depuis le cache ou lancer un rechargement ne renouvelle aucun de ces horodatages. Les dimensions sont attachées à la version des octets ; les nouveaux octets invalident les verdicts et bitmaps de cette version. Une entrée héritée sans horodatage fiable est inconnue, pas fraîche par défaut. Le contrôle d'âge n'est pas court-circuité par un hit mémoire ou un bitmap déjà dessiné ; un échéancier unique agit à la prochaine échéance utile, sans boucle par frame, et le retour au premier plan recontrôle immédiatement les dates. Une date future incohérente ne prolonge pas la validité.

La copie périmée à 7 jours n'est plus un repli autorisé ; une nouvelle récupération du même URI peut produire une nouvelle version. Les métadonnées et fichiers de cache restent évictables avant ces plafonds : il ne s'agit pas d'une garantie de rétention minimale.

Les programmes restent des instantanés en mémoire de leurs tables, non un nouveau catalogue persisté. Chaque table confirme uniquement son propre résultat : une erreur sur `PreviewPrograms` n'efface pas un snapshot watch next valide et n'est pas convertie en liste vide. Le succès d'une lecture watch next peut être partagé entre les consommateurs du même instantané. La panne du serveur Jellyfin ne signifie pas que le TV Provider système est illisible ; s'il confirme encore le programme, seules les règles de l'image entrent en jeu.

La relecture suit les signaux, le retour au premier plan et la périodicité courante du héro, étendue à À suivre. Une lecture vide réussie retire les programmes concernés immédiatement. La perte de permission et la désinstallation ne sont pas un délai de grâce : les contenus deviennent inaccessibles selon les règles existantes ; les préférences et l'historique utilisateur ne sont pas effacés. Toute publication tardive porte une génération de programme : un résultat d'une génération retirée ou remplacée ne réintroduit ni programme ni image à l'écran.

Alternative écartée : prolonger le TTL à chaque affichage, confondre une erreur avec un résultat vide, ou retirer un programme à partir de sa dernière activité de visionnage alors que la source continue de le confirmer.

### D6. Reprise et rendu des trois zones

Les pannes d'ouverture, de transport, de lecture et les délais ne créent plus de blacklist à vie dans le héro ou le Top Shelf. Les nouvelles tentatives sont mutualisées, déclenchées par une relecture pertinente, un retour au premier plan ou un focus prolongé, avec une temporisation technique commune ; aucune boucle de réessai par frame ou par consommateur. Une erreur 4xx d'URI expirée peut conduire à relire les candidates publiées ; aucune ancienne URI n'est reconstruite. Une image trop petite reste refusée pour cet usage et cette version, pas pour tous les usages ni toutes les versions.

Le héro distingue l'identité du programme de celle du média affiché. Pendant l'attente, il conserve le visuel précédent avec ses textes ; la navigation reste disponible. À l'échec ou à l'échéance image, il passe au programme demandé avec son média de secours : vidéo nature, puis dégradé neutre si aucun clip ne peut être lu dans le délai borné. Le dégradé local couvre aussi l'attente de préparation de la vidéo, sans écran noir. Le titre, les métadonnées, le bouton et la cible restent ceux du programme, même sur une vidéo de secours sans titre propre. Le minuteur de diaporama continue ; les programmes des autres apps restent accessibles. Le comportement existant de saut d'une vidéo d'aperçu lente reste distinct du timeout d'image.

Le Top Shelf utilise le service commun dès la validation et le préchargement de la première affiche, pas seulement dans `LoadedPoster`. Il ne s'ouvre qu'avec une image prête, ne montre jamais l'ancienne app comme repli de la nouvelle, passe à une autre affiche prête lors d'une panne/expiration et se ferme s'il n'en reste aucune. Les retraits et expirations s'appliquent immédiatement même pendant une validation inachevée ; les autres ajouts respectent la stabilité de rotation existante. Le focus reste sur la tuile.

À suivre rend sa carte et son placeholder pendant le chargement sans attendre les autres images. Une copie locale utilisable remplace immédiatement le placeholder ; sinon les candidates sont essayées et l'échec final le conserve. Le changement d'image ne change ni la clé, ni la géométrie, ni le focus de la carte. Un programme retiré/expiré suit les règles existantes de focus d'une carte disparue. Les listes utilisateur de contenus retirés ne sont pas purgées par le TTL des programmes.

Les chaînes nouvelles éventuelles restent en ressources françaises. Réutiliser les testTags existants ; ajouter `hero-fallback-nature` et `hero-fallback-neutral` pour les états rendus réels, sans en faire de nouveaux éléments focusables.

### D7. Ordre aléatoire du héro
Le tri du héro (domaine) reçoit une graine tirée au démarrage du processus (`Random` injectable pour les tests). L'ordre de session est mémorisé dans le domaine par identifiant de programme : les programmes déjà présents gardent leur rang ; un nouveau programme est inséré à une position aléatoire après le programme affiché ; un programme disparu est retiré. Rien n'est persisté : un nouveau processus tire un nouvel ordre.

## Risks / Trade-offs

- [Fournisseur qui ignore l'annulation] → Une place physique peut rester occupée jusqu'à son retour ; quarantaine par app, nombre de workers borné et échec rapide à saturation, jamais promesse d'interrompre de force Binder.
- [Délai trop court ou concurrence trop forte sur la TV] → Valeurs techniques initiales de D2 vérifiées avec un fournisseur lent et des apps saines, sans modifier les plafonds de conservation décidés.
- [Cache disque dupliqué ou validation répétée] → Budget unique, clé de source/version commune et tests comptant ouvertures, téléchargements et analyses d'en-tête entre les trois consommateurs.
- [Image ancienne sur un programme valide] → Revalidation en arrière-plan après 24 h, arrêt impératif à 7 jours ; changement d'URI ou de version invalidant les associations concernées.
- [Vidéos nature indisponibles en même temps que le fournisseur] → Dernier recours local déjà existant ; le programme reste ouvrable et le défilement continue.

## Migration Plan

Déployer le service partagé et raccorder tous les chemins avant de retirer les états d'échec locaux. Les caches d'images hérités dépourvus de métadonnées fiables sont revalidés ; les préférences, épinglages, contenus retirés et mémoire d'ouverture ne changent pas. La tâche de documentation corrige le README pour les seuils, le cache borné et les replis. Un retour à la version précédente ne nécessite aucune migration des préférences.

La base documentaire est la correction des specs existantes de la PR #81, au-dessus de l'archivage Tsuyu (PR #80). La PR #81 a replié les exigences extraites (relecture et colonnes facultatives, synchronisation et attente, reprise, qualité des cartes) dans leurs exigences parentes ; ce change modifie ces parents et rétablit leurs paragraphes **Détails.** avec les règles mises à jour, sans réintroduire les longs blocs ni laisser les anciennes règles 1 s et 2× actives.

### Ordre d'archivage

Les changements de fraîcheur et de validation cités dans la proposition et Tsuyu sont archivés dans la base de la pile. La PR #80 puis la PR #81 précèdent cette spec ; après son implémentation et sa validation TV, l'archivage images applique ses deltas sur cette référence compacte. Les réserves TV de l'archive Tsuyu restent indépendantes.
