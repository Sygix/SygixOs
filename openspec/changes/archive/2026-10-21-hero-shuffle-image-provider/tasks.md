# Tasks

Les sections 1 à 7 décrivent la première implémentation livrée en v0.3.1 ; ses cases cochées restent un historique. La section 8 est la révision à implémenter ; elle remplace le transport, le service, le stockage des associations et l'invalidation globale de v0.3.1.

## 1. Vérification des données sur la TV avant implémentation

- [x] 1.1 Dispense explicite de Sygix pour la première implémentation. Constat ultérieur sur la TV de référence : le fournisseur d'images de Jellyfin ne déclare aucun type ; le shell adb ne peut pas lire le TV Provider, les schémas d'URI des autres apps restent à relever depuis SygixOs (8.12).

## 2. Programmes, candidates et confirmations

- [x] 2.1 Conserver affiche, miniature et identité/version dans les modèles du héro et d'À suivre.
- [x] 2.2 Résultats explicites et lectures bornées par table du TV Provider.
- [x] 2.3 Échéances de programmes, plafond de 24 h, relecture périodique étendue à À suivre.
- [x] 2.4 Protection des publications asynchrones par identité et génération.

## 3. Cache de sources et dimensions partagés

- [x] 3.1 Service commun, politiques pures par destination.
- [x] 3.2 Conservation des sources HTTP(S) et `content://` sur disque.
- [x] 3.3 Fraîcheur 24 h, secours jusqu'à 7 jours.
- [x] 3.4 Mutualisation des demandes en cours et des alternatives.

## 4. Ordonnancement et fournisseurs isolés

- [x] 4.1 Files séquentielles par app, parallélisme global borné.
- [x] 4.2 Récupérateur `content://` par client instable et échéancier indépendant.
- [x] 4.3 Quarantaine d'un worker bloqué.
- [x] 4.4 Délai HTTP(S) et reprises temporisées communes.
- [x] 4.5 Audit des ouvertures d'images tierces.

## 5. Intégration des trois zones et documentation

- [x] 5.1 Héro raccordé au service commun.
- [x] 5.2 Top Shelf raccordé au service commun.
- [x] 5.3 À suivre raccordé au service commun.
- [x] 5.4 États `hero-fallback-nature` et `hero-fallback-neutral`.
- [x] 5.5 README.

## 6. Ordre aléatoire du héro

- [x] 6.1 Tri de session : graine par processus, ordre stable, insertion aléatoire après le programme affiché.

## 7. Vérification intégrée de la première implémentation

- [x] 7.1 `openspec validate --all --strict`, `./gradlew test`.

## 8. Révision v0.3.2

- [x] 8.1 Ajouter Room et KSP (versions compatibles avec Kotlin 2.4.x et l'AGP du projet) ; créer l'index `tv_programs`, `program_image_candidates`, `program_images`, `program_image_links` de D3 avec un seul schéma initial 1 exporté et repli destructif pour une version non prise en charge ; supprimer au premier démarrage les entrées de cache au format de v0.3.1. Tests Room : écriture, purge, réouverture conservant les liens et les dates, migration indisponible et index illisible. Aucune migration de développement intermédiaire n'est conservée avant cette première livraison de Room.
- [x] 8.2 Calculer l'empreinte d'image (sans `_ID`, position, engagement ni durée) et l'empreinte complète ; comparer les relectures du TV Provider sur l'empreinte ; ordre du héro par empreinte. Tests : republication sous un nouvel `_ID`, progression mise à jour, URI changée, programme retiré ou ajouté.
- [x] 8.3 Remplacer `AndroidArtworkTransport`, `ArtworkDiskStore` et `ProgramArtworkService` par l'intercepteur et le fetcher Coil de D1 : OkHttp avec redirections, `openAssetFile(uri, "r")` sans filtre de type, lecture jusqu'à la fin du flux, plafond 16 Mo, vérification d'en-tête et décodage réduit avant `commit`, clé disque = URI, clé mémoire explicite, travaux partagés par clé, `WRITE_ONLY` sur rafraîchissement. Tests : fournisseur sans type et tube de longueur inconnue, redirection http→https, flux tronqué ou page d'erreur non engagés, deux zones sur la même clé.
- [x] 8.4 Adapter l'ordonnanceur : délai de 8 s depuis le début effectif de la requête, aucun délai d'attente ni délai empilé (validateur, intercepteur, zones), priorité aux zones affichées, retrait des demandes sans consommateur, annulation qui ferme descripteur et flux, quarantaine jusqu'à la sortie réelle. Tests : trente images d'une app sans expiration en file, quatre apps sous la limite globale, `read` bloqué débloqué par la fermeture, Binder bloqué en quarantaine, saturation.
- [x] 8.5 Images génériques de D5 : refus par toutes les politiques, hash publié pour au moins 3 identités partagées d'une même app, `android.resource://` d'une autre app ; état `generic`, nouvelle tentative après une heure, jamais conservé comme valide. Tests : tuile commune à trois films, épisodes d'une série, même film dans les deux tables, icône d'erreur sans écraser la copie valide.
- [x] 8.6 Supprimer le compteur global (`changes`, `retryChanges`, `ArtworkRevision.kt`, paramètres de révision des requêtes, collecteur de `HomeViewModel`) ; flux d'événements typés par image et par programme ; minuteur unique vers la prochaine échéance émettant un événement ciblé ; clés Compose par empreinte et version ; retour au premier plan sans effacement. Tests : une URI changée ne récupère qu'une image et ne recompose pas les autres, échéance d'une image affichée remplacée sans écran vide, retour au premier plan sans rechargement.
- [x] 8.7 Qualité : Top Shelf à 640 px paysage ; admission du héro limitée aux programmes dont une vidéo ou un visuel accepté est prêt, ou dont la requête a échoué ; programme passé tant que son image n'est pas prête ; `VisualValidator` lit l'index. Tests aux seuils 639/640 px, programme à affiche portrait seule écarté du héro mais présent dans À suivre.
- [x] 8.8 Héro : clip nature identifié indépendamment du programme, sans boucle sur lui-même, fin de clip vers le clip suivant distincte de la fin d'une vidéo d'aperçu, minuteur de 12 s sans rechargement du clip, arrêt et reprise au début après un programme avec image. Tests avec lecteur de test : une seule préparation pour trois programmes en repli, clip suivant à la fin, reprise au début.
- [x] 8.9 Cache vidéo de D7 : singleton applicatif `SimpleCache` créé sur IO, taille `min(300 Mo, espace utilisable / 4)`, pas de cache sous 64 Mo, `CacheDataSource` avec `FLAG_IGNORE_CACHE_ON_ERROR`, source sans cache injectée en test. Tests : cache plein évincé, erreur d'écriture et espace insuffisant lus en flux.
- [x] 8.10 Raccorder `SettingsContent` au même chargeur et aux mêmes clés ; rétablir une journalisation concise des échecs d'image et de vidéo (app, schéma, autorité, cause, jamais l'URI complète).
- [x] 8.11 Mettre à jour le README (seuil Top Shelf, cache vidéo, images génériques) ; `openspec validate --all --strict`, `./gradlew test`, `assembleRelease`, vérification sur l'émulateur.

## Workflow follow-up

- Après merge et validation sur la TV : `openspec archive hero-shuffle-image-provider`.
