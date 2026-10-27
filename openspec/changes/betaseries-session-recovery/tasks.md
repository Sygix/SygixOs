# Tasks

Les tests sont écrits avec chaque correction puis exécutés seulement en fin d'implémentation, à la demande du propriétaire.

## 1. Conservation du stockage sécurisé

- [x] 1.1 Extraire le fournisseur de clés et conserver le fichier sur erreur de récupération temporaire ou inconnue ; preuve : KeystoreTrackingSessionTest.temporaryOrUnknownKeyFailuresKeepEncryptedSessionUntilRecovery et failedReplacementDoesNotOverwriteTheSavedSession ; régression d'effacement vérifiée en réintroduisant l'ancien comportement.
- [x] 1.2 Terminer la restauration sur clé confirmée absente, invalidation définitive ou authentification GCM invalide ; preuve : KeystoreTrackingSessionTest.confirmedMissingKeyEndsRestorationAndDeletesOnlyUnreadableSession, confirmedPermanentKeyInvalidationAllowsSavingAndReadingANewSession, failedInvalidKeyCleanupKeepsSessionRetryableUntilANewKeyCanBeCreated et failedGcmAuthenticationEndsRestorationWithoutAnEndlessStorageError. Le point P2 de revue est couvert jusqu'à la relecture du nouveau jeton ; les deux régressions d'invalidation échouent sur le code relu puis passent avec la correction.

## 2. Restauration et qualification des erreurs

- [x] 2.1 Distinguer session absente, restauration et jeton présent, avec réessai commun ; preuve : les six scénarios de TrackingSessionRecoveryTest, dont reprise au premier plan et déconnexion pendant une lecture suspendue.
- [x] 2.2 Classer 401/403 génériques comme erreurs réessayables et conserver le refus explicite du jeton ; preuve : BetaSeriesApiTest.genericHttpRefusalsNeverProveThatTheTokenExpired, explicitTokenRefusalRequiresReconnectionRegardlessOfHttpStatus et TrackingSessionRecoveryApiTest ; régression vérifiée en réintroduisant l'ancien refus HTTP générique.

## 3. Présentation et documentation

- [x] 3.1 Réutiliser `tracking-sync` avant identité, garder le focus et réserver Connecter à l'absence confirmée ; preuve : TrackingSettingsTest.restoringAndRetryingIdentityKeepsFocusAndNeverStartsAnAssociation et retryAfterTemporaryStorageFailureReadsTheSavedSessionWithoutQr ; régression vérifiée avec l'ancien choix par identité. Contrôle supplémentaire au D-pad sur émulateur Android TV.
- [x] 3.2 Garder le détail d'erreur accessible sans identité ni carte ; preuve : TrackingHomeNavigationTest.restorationFailureBeforeIdentityKeepsStatusAndRetryReachableFromTheGear, en échec avec l'ancien réessai ; détail également ouvert et réessayé sur émulateur sans compte identifié.
- [x] 3.3 Adapter le message de stockage en anglais, français, allemand, espagnol et italien et documenter le réessai dans le README ; preuve : suite complète incluant les contrôles de ressources, lecture du README et message anglais vérifié sur émulateur.

## 4. Vérification finale et publication

- [x] 4.1 Exécuter les régressions et `./gradlew test` après toutes les corrections ; preuve après correction P2 : 1 248 tests, zéro échec/erreur/test ignoré, et assembleRelease réussi. Vérification initiale : 53 tests ciblés et quatre régressions vérifiées avec les comportements défectueux réintroduits ; les deux régressions P2 ont aussi échoué sur le code relu avant la réussite de la suite corrigée. Une première exécution complète initiale avait dépassé son délai avant les relances réussies.
- [x] 4.2 Vérifier le change avec openspec-verify-change et `openspec validate --all --strict` ; preuve : verification.md relie les cinq exigences au code et aux tests, sans divergence fonctionnelle trouvée ; validation stricte 11/11. La validation sur TV de 4.3 reste explicitement ouverte avant archivage.
- [ ] 4.3 Construire `assembleRelease` et vérifier sur TV la session conservée après redémarrage et panne/reprise ; preuve : résultat distinct des tests JVM, sans donnée personnelle.
- [x] 4.4 Relire le diff et les scans licence/confidentialité, créer les commits signés puis la PR ; preuve : scans vides et signatures G vérifiées, PR https://github.com/Sygix/SygixOs/pull/103 ; état de CI du head courant à consulter dans la PR. Les corrections de revue sont intégrées aux commits concernés par autosquash signé.

## Workflow follow-up

- Partie build de 4.3 vérifiée : assembleDebug et assembleRelease réussis, minification R8 et lint vital inclus. Vérification sur TV réelle non effectuée ; les erreurs de clé et de réseau sont simulées en tests, la navigation et le réessai de stockage sont contrôlés sur émulateur avec un fichier fictif, sans compte réel.

- Fusionner seulement après l'accord explicite du propriétaire.
- Après fusion et validation sur TV, exécuter `openspec archive betaseries-session-recovery` puis valider les specs synchronisées.
