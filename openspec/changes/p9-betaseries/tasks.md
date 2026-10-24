# Tasks

Les tâches cochées sont vérifiées par les tests nommés ci-dessous ; 1.x, 3.5b et 8.2 restent ouvertes jusqu'à validation TV. Une couverture partielle ne clôt pas un critère complet.

## 1. Contrats et appareil

- [ ] 1.1 Valider association/reconnexion et chargement corrigés en release sur TV ; critère : code lisible du canapé, échange `code`, absence d'accès aux notifications et chargement sans blocage du thread principal.
- [ ] 1.2 Vérifier les cibles et replis réellement obtenus pour chaque lecteur depuis héro et À suivre ; critère : matrice de cibles anonymisée, aucun lien non testé annoncé ciblé.
- [x] 1.3 Vérifier la pagination authentifiée et les volumes avec limit=1/showsLimit=100, seuls films state=0 ; critère : deux pages et fin établies, aucune réponse volumineuse ni purge sur liste partielle.
- [x] 1.4 Vérifier mutations, DELETE en query, bulk=false et voisins sur contenus autorisés ; critère : états avant/après et annulations consignés sans historique personnel.

## 2. Design accepté et navigation

- [ ] 2.1 Vérifier les surfaces réutilisées et matières selon le design accepté ; critère : ContextMenuPanel et Dimens utilisés, contrôle TV du verre et des états compacts (inclus dans 8.2).
- [x] 2.2 Vérifier les trajets explicites et le retour à l'engrenage ; preuve : TrackingHomeNavigationTest, incluant carte disparue et rangée masquée.

## 3. Transport et session

- [x] 3.1 Isoler les comptes et réponses tardives ; preuve : TrackingSessionTest.disconnectedAccountCannotPublishLateData et secondAccountCannotBeReplacedByNonCancellableFirstAccountResponse.
- [x] 3.2 Borner appels/reprises/Retry-After et fermer les ressources ; preuve : TrackingTransportTest (dont serverDelayIsCappedByTheRemainingCallBudget, horloge virtuelle injectée) et TrackingHttpsTransportTest, sans mutation rejouée ; JSON invalide en erreur de protocole : TrackingReviewV2Test.rejectedCandidateDoesNotReportAUserSessionRevocationFailure.
- [x] 3.3 Découvrir les données statiques sans exécution de script ; preuve : BetaSeriesConfigTest.discoveryOnlyReadsSixOfficialScriptsAndLargeUnrelatedBlocksDoNotChangeConfiguration et concurrentDiscoverySharesOneHtmlAndRenamedBundleRequest ; analyse hors main vérifiée dans le code.
- [x] 3.4 Associer avec code, intervalle, expiration et fermeture ; preuve : BetaSeriesDeviceTest.confirmedAccountFinishesAssociationAndStopsPolling, transientHtmlAndNetworkPollsKeepAssociationUntilSuccessOrExpiry et autres cas d'attente/expiration.
- [x] 3.5a Stockage et fin de session au niveau contrôleur ; preuve : TrackingSessionTest.offlineRevocationIsNotAnAccountFailure, cancelledAssociationValidatesWithoutReplacingExistingSessionAndRevokesUnusedToken et TrackingRefreshScheduleTest.transientStorageFailureRetriesOnNextForegroundWithoutDeletingSession ; jeton candidat refusé sans « Révocation distante non confirmée » : TrackingReviewV2Test.rejectedCandidateDoesNotReportAUserSessionRevocationFailure ; Keystore réel, dont clé absente, en 3.5b.
- [ ] 3.5b Contrôler sur TV le chiffrement de tracking-session hors sauvegarde, compte conservé après redémarrage et effacement à la déconnexion.
- [x] 3.6 Persister la configuration 7 jours et l'invalider sur refus explicite ; preuve : BetaSeriesConfigTest.persistedConfigurationSurvivesRestartExpiresAfterSevenDaysAndIsRemovedOnRefusal et BetaSeriesApiTest.rejectedPostConfigurationIsInvalidatedButMutationIsNotReplayed.

## 4. Compte et fusion

- [ ] 4.1 Mapper listes et détails, compte vide, pages/éléments incomplets ; critère : BetaSeriesApiTest avec paramètres limit/showsLimit et conservation des autres entrées sur dépassement de corps.
- [x] 4.2 Identifier exactement sans ambiguïté et distinguer l'appartenance ; preuves : ContentIdentityResolverTest.verifiedIdsResolveLocalizedTitlesButConflictingIdsNeverMerge et TrackingEnrichmentTest.partialAccountKeepsEnrichmentAndRefreshOnlyReadsExpiredCachedIdentity.
- [x] 4.3 Fusionner reprises/prochains épisodes sans mélanger leurs intents ; preuve : TrackingProjectionTest.unseenResumeWinsWithoutReusingAnIntentForAnotherEpisode et samePlayerKeepsTheExactNextEpisodeIntentAlongsideSeenResumeIntent.
- [x] 4.4 Préserver ouvertures, masquages et identités migrées ; preuves : TrackingIdentityMigrationTest.persistedOpeningAndHideRetainIdentityAcrossPreferenceRecreation et UpNextTrackingMemoryTest.permissionAndReadErrorsKeepRemoteOpeningAndReturnTarget.
- [ ] 4.5 Projeter rangée et héro sans ajout au Top Shelf ; preuves attendues : TrackingHeroExclusionsTest et TrackingProjectionTest, identité visuelle native conservée.
- [ ] 4.6 Vérifier indépendance des sources et rétablissement après panne ; critère : tests nommés couvrant refus TV, erreur partielle et absence de résurrection par cache d'image.
- [x] 4.7 Compter les lectures : retour avant TTL (0 liste), mutation (1 relecture ciblée), retour après TTL (1 lecture complète), émission d'état (0 requête TV Provider) ; preuves : TrackingRefreshScheduleTest.foregroundUsesLazyTtlAndMutationOnlyPatchesItsContent, TrackingReviewV2Test.partialSnapshotAndRepeatedRestoreRespectTtlWithRealHttpCounts (requêtes HTTP réelles du transport factice, instantané partiel et recréation de l'accueil), artworkIsConfirmedOnlyWhenPairsChangeAndNeverWithoutAccount et TrackingHeroExclusionsTest.trackingAndMemoryEmissionsNeverTriggerTvProviderReadsWithOrWithoutAccount.
- [x] 4.8 Projeter hors du thread principal ; preuve : UpNextControllerTest.`row projection runs on the injected projection dispatcher` ; fluidité réelle dans 8.2.

## 5. Lecteurs et actions

- [ ] 5.1 Vérifier catalogue vidéo/Autre application, absence et désinstallation ; critère : tests nommés sur toute la matrice et préférences persistantes.
- [x] 5.2 Mutualiser le premier choix et la préférence ; preuve : PlaybackCoordinatorTest.firstChoiceIsRequiredForSingleAppAndPreferenceIsSharedAcrossSurfaces ; cibles réelles dans 1.2.
- [x] 5.3 Appliquer le pays aux offres, sans relecture du compte ; preuve : BetaSeriesOffersTest.changingCountryRechecksOnlyPlatformsAndNeverReloadsAccount et seasonOfferDoesNotBecomeAnEpisodeOrRegionConfirmation.
- [x] 5.4 Mutations ciblées et Annuler ; preuves : TrackingActionsTest.movieUndoRestoresStateTwoAndConcurrentDuplicateActionIsNotSent, blockedActionPreservesUndoAndItsRemainingRemovalDeadline, BetaSeriesApiTest.archiveMappingAndUndoRestoreBothOriginalArchiveStates, TrackingReviewV2Test.failedMutationKeepsCardAndHeroBeforeAndAfterDismissingError et settingsPauseUndoDeadlineUntilVisibleAgain ; API réelle dans 1.4.
- [ ] 5.5 Liste d'épisodes à plat et actions isolées ; preuve attendue : TrackingSettingsTest.selectedUnseenEpisodeDoesNotMarkPreviousEpisodesAndReturnRefreshRestoresNeighbor, complétée pour vide/erreur/reconnexion.
- [ ] 5.6 Réconcilier README avec lecteurs, masquages, archive et retrait ; critère : documentation cohérente et aucune promesse de restauration de l'historique.
- [x] 5.7 Ordonner le sélecteur : apps qui publient le contenu, services confirmés, services mentionnés, autres apps vidéo, Autre application ; preuve : PlaybackAppOrderTest et TrackingHomeNavigationTest.publisherLeadsPickerAndFirstTrackedCardRegainsFocusAfterRealControllerReturn (focus initial sur la première).

## 6. Confirmation manuelle

- [x] 6.3 Terminé au retour effectif et Annuler accessible sans vol de focus ; preuves : TrackingHomeNavigationTest.returnPromptDoesNotStealFocusAndRemainsReachableAfterItsCardDisappears, publisherLeadsPickerAndFirstTrackedCardRegainsFocusAfterRealControllerReturn, TrackingActionsTest.watchedAndArchiveUndoSurviveAccountRereadsForTenActiveSeconds, TrackingReviewV2Test.returnPromptReplacesCompletedUndoAndQueuesBehindPendingRemoval et pauseOrStopAtTwelveSecondsConfirmsDepartureAndNoDepartureExpiresAtFifteen.
- [ ] 6.4 Retirer toute demande d'accès média et preuve automatique ; critère : manifeste, commandes et UI sans accès aux notifications, README limité au mode manuel.

## 7. Interface

- [x] 7.1 Réglages, pays et association ; preuve : TrackingSettingsTest.associationShowsOneCompleteAddressAndBackRestoresConnectWithoutPolling et countryDropdownCancelsOrCommitsAndReturnsFocusToItsControl à taille TV ; lisibilité réelle dans 1.1.
- [ ] 7.2 Vérifier menus, lecteurs, épisodes, badges et ajout local ; critère : matrice Compose TV des quatre états d'appartenance, ajout sans remplacer les actions locales.
- [x] 7.3 Réutiliser modales et notification compacte, focus piloté par ViewModel ; preuves : TrackingModalTest.removedFocusedPlayerUsesNextThenPreviousWithoutEscapingModal et TrackingHomeNavigationTest ; matières et compacité dans 8.2.
- [ ] 7.4 Vérifier Réafficher et mémoire des contenus du compte ; critère : test Compose TV conservant tri, dépliage et réaffichage sans mutation distante.

## 8. Validation intégrée

- [x] 8.1 Exécuter tests, build release et `openspec validate --all --strict`, puis openspec-verify-change ; preuves : 1 193 tests sans échec ni skip, build release réussi et 9 validations ; commandes Gradle séparées avec `--no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx2g`. Vérification du change partielle : tâches non cochées et validations TV non couvertes restent ouvertes ; aucun archivage déclaré possible.
- [ ] 8.2 Valider la release sur TV : association, compte, lecteurs, pays, panne/reprise, stockage, retours, matières et fluidité ; critère : matrice anonymisée validée par le propriétaire.
- [ ] 8.3 Vérifier régressions sans compte/déconnexion et hygiène publique ; critère : tests nommés, attribution/licences, aucune donnée personnelle ni sortie de build dans le diff.
