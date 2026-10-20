# Vérification : hero-shuffle-image-provider

## Révision v0.3.2 — vérification avant essai sur émulateur

La source de vérité est la spec révisée de la PR #84. Les résultats historiques plus bas ne sont pas utilisés comme preuve de cette révision.

| Dimension | Résultat |
| --- | --- |
| Complétude | Tâches 8.1 à 8.10 implémentées et vérifiées ; 8.11 attend encore l'émulateur ; 8.12 reste réservée à la TV réelle. Les anciennes tâches 7.2 et 7.3 renvoient à 8.12. |
| Conformité | Les contrats modifiés sont reliés aux preuves ci-dessous ; le fonctionnement des fournisseurs tiers et la fluidité sur la TV ne sont pas vérifiés ici. |
| Cohérence | D1 à D7 : Coil et index Room, délai de requête sans attente en file, événements ciblés, qualité par destination, clips continus et cache vidéo adaptatif. |

| Contrat | Code | Preuves automatisées |
| --- | --- | --- |
| Index durable, associations non bornées, migrations et reconstruction | `ArtworkDatabase`, `ArtworkRepository` | `ArtworkDatabaseTest` : 100 associations, purge, conservation des dimensions et dates, migration absente et migration en échec ; `ArtworkRepositoryTest` : réouverture hors ligne et éviction Coil. |
| Fournisseur sans type, flux de longueur inconnue, fermeture et redirections | `ProgramImageReader`, `ProgramImageFetcher`, `ImageCancellation` | `ContentArtworkTransportTest` : fournisseur sans type, pipe, cache commun, appel tardif, fermeture qui débloque la lecture ; `AndroidArtworkTransportTest` : serveur HTTP redirigeant vers HTTPS avec certificat de test éphémère. |
| Vérification avant engagement et clés communes | `ProgramArtworkInterceptor`, `ProgramImageFetcher`, `ArtworkRepository` | `ArtworkRepositoryTest` : trois destinations, abonné annulé, contenu différent sur URI connue, page d'erreur et PNG tronqué non engagés, copie valide préservée devant une icône d'erreur. |
| Délai unique et priorité | `ArtworkScheduler` | `ArtworkSchedulerTest` : trente images sans expiration en file, quatre apps, requête abandonnée, priorité aux zones visibles, quarantaine, saturation et annulation bloquée. |
| Événements ciblés, fraîcheur et identité indépendante de la ligne | `ArtworkRepository`, `ArtworkEvents`, `HomeViewModel`, `HeroSessionOrder` | `ArtworkFingerprintTest`, `HomeRefreshTest`, `ArtworkRepositoryTest`, `HeroVisualSyncTest`, `UpNextArtworkRenderTest` : nouvelle URI isolée, voisin frais inchangé, absence de revalidation après republication/progression, maintien du visuel pendant son remplacement et contrôle de TTL. |
| Génériques et qualité | `ArtworkRepository`, `ArtworkPolicy` | Trois identités contre une seule série ; ressource étrangère jamais lue ; icône n'écrasant pas la copie valide ; seuils 639/640, cartes à leur largeur et portrait. |
| Continuité des clips et cache vidéo | `HeroStage`, `HeroPlayer`, `HeroVideoCache`, `StreamingFallbackDataSource` | `HeroVideoTest` : préparation unique pour trois replis, fin de clip indépendante, reprise au début après image ; `HeroVideoCacheTest` : budget, LRU, manque de place, création impossible et erreur d'écriture avec reprise à l'octet non lu. |
| Vignettes des réglages et diagnostics | `SettingsContent`, intercepteur partagé et journaux image/vidéo | Régressions de vignettes des réglages ; `ContentArtworkTransportTest` vérifie l'absence d'URL serveur et de paramètres dans les diagnostics. Les autorités réseau sont masquées. |

Commandes exécutées : `openspec validate --all --strict` (**9 éléments validés, aucun échec**) et `./gradlew test assembleRelease --no-daemon` (**réussi**). La vérification JVM ne démontre ni les performances sur la TV ni le comportement Binder d'une application tierce réelle. L'essai émulateur et la revue indépendante restent à consigner ; aucun archivage n'est autorisé à ce stade.

### Corrections de la revue indépendante

| Constat | Statut et correction | Preuve de régression |
| --- | --- | --- |
| Clip redémarré entre programmes ayant des candidates en échec | Traité : l'état de présentation porte l'échec connu ; aucun détour par un poster en attente entre deux replis. | `HeroVideoTest` utilise trois URI distinctes non vides en échec, exige une seule préparation et aucun `clear()` avant la fin du clip. |
| Ancien résultat validé conservé après rejet définitif | Traité : la revalidation retire son ancien résultat si aucune image n'est acceptée ; les événements de qualité retirent immédiatement les associations de présentation concernées. | `HomeRefreshTest.imagesBecomingGenericWithdrawPreviouslyAcceptedHeroAndShelfResults` et `aRefreshedPortraitRemainsCardCompatibleButLeavesHeroAndShelf`. |
| Top Shelf vide pendant le décodage de remplacement | Traité : le dernier painter réussi reste affiché jusqu'au prochain succès ; les painters sont explicitement identifiés par empreinte et version, y compris À suivre et les réglages. | `ShelfArtworkRefreshTest.previousShelfImageStaysVisibleWhileTheNewVersionIsBeingDecoded` bloque le second décodage ; `UpNextArtworkRenderTest` exige un nouveau succès de rendu avec une nouvelle clé versionnée. |
| Comptage des identités génériques non atomique | Traité : comptage, associations et états sont publiés dans une seule transaction Room ; les événements suivent son engagement. | `ArtworkRepositoryTest.concurrentPublicationsOfTheSameHashBecomeGenericRegardlessOfTheirCompletionOrder` résout trois URI distinctes en concurrence dans six ordres déterministes. |
| Lecture locale non bornée et non fermée à l'annulation | Traité : propriétaire d'annulation et copie bornée communs aux lecteurs locaux et distants ; le décodage final local reste délégué à Coil. | `ArtworkRepositoryTest.localReadIsClosedOnTimeoutAndTheOtherAppStillProgresses` et `oversizedLocalSourcesAreBoundedAndClosed`. |

Après ces corrections, `./gradlew test assembleRelease --no-daemon` réussit. Le premier essai émulateur a vérifié lancement, métadonnées synthétiques, navigation, replis, lecture/pause vidéo et retour au premier plan sans nouveau chargement ; aucun visuel réussi ni continuité exacte de clip n'y a été déclaré vérifié. Un second passage sur le correctif revu reste à consigner.

### Derniers cas limites et noms de tables

- **Collision de clés des cartes traitée** : les identités de présentation identiques sont désambiguïsées par la ligne, sans changer le dédoublonnage métier. `UpNextCardsTest` compose et navigue entre deux films de même titre sans identifiant publié et avec deux affiches distinctes.
- **Alternative inutilisée traitée** : le painter du panneau n'écoute que l'URI affichée ; le ViewModel ne retire que les résultats utilisant l'image rejetée. `ShelfArtworkRefreshTest.rejectionOfAnUnusedCandidateDoesNotBlacklistTheDisplayedPoster` conserve A quand B est rejetée ; `HomeRefreshTest.rejectedDisplayedCandidateCanSwitchToItsAlreadyValidAlternative` prouve le passage de A à B après rejet de A.
- **Héro vide au retour traité dans le code et les tests** : le second essai émulateur a révélé un retour sans titre après fermeture du guide initial. Les résultats de requêtes en échec sont désormais observables par programme ; un résultat jamais valide n'émet plus d'expiration d'image. `HomeRefreshTest.cachedFailureRestoresFallbackAfterRevalidationWithoutAnotherCheckedImageEmission` et `ArtworkRepositoryTest.aNeverValidImageDoesNotEmitAnExpirationOnForegroundChecks` couvrent cette cause. La confirmation sur émulateur du correctif reste à faire.
- **Noms de tables précisés à la demande de Sygix** : `tv_programs`, `program_image_candidates`, `program_images`, `program_image_links`. Le schéma 3 migre depuis le schéma 2 en conservant programmes, candidates, liens, dates et contraintes ; `ArtworkDatabaseTest.tableRenamePreservesProgramsCandidatesAndImageLinks` vérifie aussi `foreign_key_check`.
- **Isolation des tests d'activité** : le dispatcher applicatif et le lecteur vidéo sont injectables sans changer les valeurs de production. Le double d'application évite les lectures vidéo distantes et les transactions de préférences liées à un ancien looper Robolectric ; le test d'activité réinitialise son dispatcher au nettoyage.

Dernière suite complète : **1 091 tests, aucun échec** ; `assembleRelease` réussi. Le journal de début des tests a été activé pour diagnostiquer leur cycle de vie, sans filtrage ni exclusion de scénario.

## Historique v0.3.1

> Historique : ce rapport concerne la première version de ce change (spec #82, implémentation livrée en v0.3.1), remplacée par la révision v0.3.2. Voir `design.md` et la section 8 de `tasks.md`.

## Résumé

| Dimension | Résultat |
| --- | --- |
| Complétude | Implémentation et vérifications automatisées réalisées ; relevé préalable dispensé ; deux validations finales sur la TV restent ouvertes. |
| Conformité | Les 23 exigences ajoutées ou modifiées de la base finale de la PR #82 sont reliées au code et aux tests ci-dessous ; les scénarios matériels ne sont pas déclarés vérifiés. |
| Cohérence | Cache d'octets commun, associations de contenu distinctes, politiques par destination, limites globales et ordre par processus conformes aux décisions D1 à D7. |

## Correspondance des contrats

| Contrats | Implémentation principale | Preuves |
| --- | --- | --- |
| Chargement partagé, alternatives, qualité par usage | `ProgramArtworkService`, `ProgramArtworkInterceptor`, `ArtworkPolicy`, modèles `HeroItem` et `UpNextItem` | `ProgramArtworkServiceTest`, `ArtworkPolicyTest`, tests de mapping TV Provider |
| Fournisseurs sans dépendance forte, échéances et quarantaine | `AndroidArtworkTransport`, `ArtworkScheduler` | `ContentArtworkTransportTest`, `AndroidArtworkTransportTest`, `ArtworkSchedulerTest` |
| Fraîcheur, secours borné, versions et restauration | `ArtworkDiskStore`, `ProgramArtworkService`, clés mémoire versionnées | `ProgramArtworkServiceTest`, `UpNextArtworkRenderTest` |
| Confirmations indépendantes, retrait, expiration et générations | `TvProviderHeroSource`, `ProviderQuery`, `UpNextController`, `HomeViewModel` | `ProgramConfirmationTest`, `UpNextControllerTest`, `HomeRefreshTest` |
| Relecture périodique et retour au premier plan | `HomeViewModel.heroUpdates`, `MainActivity`, collecte liée au cycle de vie | `HomeRefreshTest`, régressions de cycle de vie de l'accueil |
| Héro : attente, synchronisation, reprises et replis | `HeroStage`, `HeroPosterPipeline`, `NatureFallbackProvider` | `HeroVisualSyncTest`, `HeroVideoTest`, `HeroVisualReadyTest`, tests existants du démarrage et des métadonnées |
| Top Shelf : validation et préchargement communs, images prêtes et retraits immédiats | `VisualValidator.prepare`, `HomeViewModel`, `HomeGrid`, `ShelfPanel` | Tests de grille, de rotation et de rafraîchissement ; vérifications de taille de décodage |
| À suivre : qualité 1×, portrait, placeholder, reprise et focus | `UpNextArtwork`, `UpNextController`, restauration existante de la rangée | `UpNextArtworkRenderTest`, `UpNextArtworkQualityTest`, tests de navigation de la rangée |
| Shuffle stable de session | `HeroSessionOrder`, instance application, retour du programme affiché | `HeroSessionOrderTest` |

## Résultats

- Suite Gradle complète : **1 061 tests réussis**, sans échec ni test ignoré.
- Build release : **réussi**.
- Validation OpenSpec stricte : **9 éléments validés, aucun échec**.
- Le README décrit les nouvelles valeurs et les replis ; aucun réglage supplémentaire n'est introduit.
- Les listes utilisateur de contenus retirés et la mémoire d'ouverture ne sont pas purgées lors d'une expiration.
- Revue indépendante : **MERGEABLE** après correction des cinq constats initiaux. Le réalignement sur la base documentaire finale de la PR #82 ne change pas l'arbre applicatif vérifié.
- Émulateur Android 14 TV, APK release : lancement sans crash, navigation accueil/grille, épisode synthétique `TYPE_TV_EPISODE` dans « À suivre » avec placeholder et métadonnées propres conservées dans le repli du héro. Aucun résultat de fluidité ni de fournisseur réel n'en est déduit.

## Limites et suite

- **Tâche 1.1 dispensée** par instruction de Sygix : aucune mesure réelle de source n'est présentée comme effectuée.
- **Tâche 7.2 ouverte** : scénarios de panne, permissions et constantes techniques à confirmer sur la TV Google TV sous Android 14.
- **Tâche 7.3 ouverte** : états stables et fluidité release à confirmer sur la TV, en complément des expirations automatisées à horloge contrôlée.
- Les simulations de fournisseur, tests JVM et vérifications sur émulateur ne remplacent pas ces validations matérielles.

L'implémentation est disponible pour revue ; l'archivage reste bloqué par les deux tâches de validation TV et l'absence de merge.
