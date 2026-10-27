# Vérification — betaseries-session-recovery

## Synthèse

| Dimension | Résultat |
| --- | --- |
| Complétude | Les corrections et leurs régressions sont terminées ; validation finale sur TV réelle encore ouverte dans tasks.md. Publication suivie séparément. |
| Correction | Les cinq exigences ajoutées/modifiées sont reliées au code et aux tests ci-dessous. |
| Cohérence | D1 à D4 suivies : clé derrière une frontière injectable, état de session distinct de l'identité, refus explicite séparé du HTTP générique, composants et tags existants réutilisés. |

## Commandes et résultats

- `./gradlew :app:testDebugUnitTest` filtré sur les huit classes concernées : **53 tests réussis**.
- Réintroduction temporaire des anciens comportements d'effacement, de refus HTTP, de choix Connecter et de réessai : les quatre tests ciblés échouent pour les régressions attendues ; correctif ensuite rétabli avant les vérifications suivantes.
- `./gradlew :app:testDebugUnitTest` sans filtre : **1 247 tests réussis, zéro échec, erreur ou test ignoré**.
- `./gradlew test assembleDebug assembleRelease` : **BUILD SUCCESSFUL**, dont minification R8 et lint vital release. Les options d'exécution limitent les workers et désactivent les daemons persistants ; un script de diagnostic externe au projet affiche le début et la fin des tests.
- Une première exécution complète a dépassé le délai de commande. La relance diagnostique et la commande finale réussissent sans changement du correctif ; la cause de ce blocage ponctuel n'est pas établie.
- `openspec validate --all --strict` : **11 passed, 0 failed**.

## Correspondance exigences / code / preuves

| Exigence | Implémentation | Preuves |
| --- | --- | --- |
| Fin de session BetaSeries | `KeystoreTrackingSession.kt:19`, `TrackingSessionKeys.kt:14`, `TrackingController.kt:58` | Les six tests de `KeystoreTrackingSessionTest` couvrent conservation après panne de clé, remplacement en échec, clé absente, invalidation permanente, GCM invalide et déconnexion explicite. `TrackingSessionRecoveryTest` couvre les erreurs d'identité/stockage, absence confirmée, reprise au premier plan, refus explicite et interruption pendant lecture. `TrackingSessionTest` garde la validation atomique et l'isolation des sessions. |
| Bornes réseau et délais | `BetaSeriesApi.kt:33`, transport inchangé | `BetaSeriesApiTest.genericHttpRefusalsNeverProveThatTheTokenExpired` couvre 401/403 avec corps JSON et non JSON ; `explicitTokenRefusalRequiresReconnectionRegardlessOfHttpStatus` conserve le refus explicite. `TrackingSessionRecoveryApiTest` garde les anciens contenus puis réussit avec le même jeton sans association ni révocation. Les tests existants de transport et de mutation restent inclus dans la suite complète. |
| Réglages du suivi de visionnage | `TrackingModels.kt:44`, `TrackingContent.kt:53`, `TrackingViewModel.kt:147` | `TrackingSettingsTest.restoringAndRetryingIdentityKeepsFocusAndNeverStartsAnAssociation` vérifie chargement, double appui sans double envoi, panne puis compte rétabli avec focus stable et sans QR. `retryAfterTemporaryStorageFailureReadsTheSavedSessionWithoutQr` vérifie la relecture du stockage. Les parcours existants de pays et d'association restent couverts. |
| Indicateur d'indisponibilité du suivi | `HomeScreen.kt:504`, `HomeScreen.kt:643`, `TrackingViewModel.kt:191` | `TrackingHomeNavigationTest.restorationFailureBeforeIdentityKeepsStatusAndRetryReachableFromTheGear` vérifie statut avant identité, détail depuis l'engrenage, réessai sans code et restitution du focus ; les parcours sans carte et les replis existants restent testés. |
| Couverture de la conservation de session | Tests précédents et ressources des cinq langues | Tests JVM avec transport/clé/horloge contrôlés, vrai fichier chiffré et D-pad Compose à la taille TV. Les assertions des quatre régressions ont été vérifiées avec les comportements défectueux réintroduits. |

## Contrôle sur émulateur Android TV

APK debug issu du correctif, Android TV sous Android 14. Aucun compte réel ni association distante n'est utilisé. Un fichier de session fictif vide provoque une erreur de lecture ; ce contrôle ne prétend pas reproduire une panne matérielle du Keystore.

| Scénario | Attendu | Observé | Statut |
| --- | --- | --- | --- |
| Erreur de restauration avant identité | Réessayer et erreur explicite, sans Connecter ni code/QR | Contrôle Réessayer focusé, message de stockage temporaire et de conservation | Vérifié |
| OK sur Réessayer pendant l'erreur | Fichier conservé, pas d'association | Même fichier fictif conservé, erreur réessayable, aucun code/QR | Vérifié |
| Détail depuis le héro sans compte identifié | Haut vers l'engrenage puis Droite ouvre la cause et Réessayer | Détail ouvert, réessai utilisable | Vérifié |
| Absence de session confirmée après retrait du seul fichier fictif créé pour ce contrôle | Réessai termine la restauration et propose Connecter | Connecter focusé, sans erreur résiduelle ni chargement bloqué | Vérifié |
| Crash | Aucun | Buffer de crash vide | Vérifié |

Le fichier fictif est retiré, l'application arrêtée et l'émulateur éteint. Les captures sont transmises au propriétaire hors du dépôt.

## Points ouverts

### CRITICAL avant archivage

- **Tâche 4.3, validation sur TV réelle** : `assembleRelease` est réussi, mais la conservation de la session après redémarrage et panne/reprise n'a pas été observée sur la TV de référence. Réaliser ce contrôle avant de cocher la tâche et d'archiver ; les erreurs Keystore et API restent simulées par les tests pour cette PR.
- **Tâche 4.4, publication** : à cocher après création effective de la PR et vérification des signatures/scans.

### WARNING

- Aucune divergence fonctionnelle trouvée entre les deltas, le design et les vérifications effectuées. L'incident précis de la TV n'est pas diagnostiqué par les simulations ; une erreur de clé sans preuve structurée de perte définitive reste conservée et réessayable.

## Conclusion

Les contrôles de code, de tests automatisés et de navigation sur émulateur sont concluants. La validation sur TV réelle reste non vérifiée ; le change n'est pas prêt à être archivé tant que cette étape demeure ouverte.
