# Vérification : hero-shuffle-image-provider

## Résumé

| Dimension | Résultat |
| --- | --- |
| Complétude | Implémentation et vérifications automatisées réalisées ; relevé préalable dispensé ; deux validations finales sur la TV restent ouvertes. |
| Conformité | Les 28 exigences ajoutées ou modifiées sont reliées au code et aux tests ci-dessous ; les scénarios matériels ne sont pas déclarés vérifiés. |
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

## Limites et suite

- **Tâche 1.1 dispensée** par instruction de Sygix : aucune mesure réelle de source n'est présentée comme effectuée.
- **Tâche 7.2 ouverte** : scénarios de panne, permissions et constantes techniques à confirmer sur la TV Google TV sous Android 14.
- **Tâche 7.3 ouverte** : états stables et fluidité release à confirmer sur la TV, en complément des expirations automatisées à horloge contrôlée.
- Les simulations de fournisseur, tests JVM et vérifications sur émulateur ne remplacent pas ces validations matérielles.

L'implémentation est disponible pour revue ; l'archivage reste bloqué par les deux tâches de validation TV et l'absence de merge.
