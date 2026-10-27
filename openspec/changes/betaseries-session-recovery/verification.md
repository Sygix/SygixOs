# Vérification — betaseries-session-recovery

## Synthèse

| Dimension | Résultat |
| --- | --- |
| Complétude | 10/11 tâches terminées ; validation finale sur TV réelle encore ouverte dans tasks.md. PR publiée. |
| Correction | Les cinq exigences ajoutées/modifiées sont reliées au code et aux tests ci-dessous. |
| Cohérence | D1 à D4 suivies : clé derrière une frontière injectable, état de session distinct de l'identité, refus explicite séparé du HTTP générique, composants et tags existants réutilisés. |

## Commandes et résultats

- Vérification initiale : `./gradlew :app:testDebugUnitTest` filtré sur les huit classes concernées, **53 tests réussis**.
- Réintroduction temporaire des anciens comportements d'effacement, de refus HTTP, de choix Connecter et de réessai : les quatre tests ciblés échouent pour les régressions attendues ; correctif ensuite rétabli avant les vérifications suivantes.
- Après correction P2, `./gradlew test assembleRelease` : **1 248 tests réussis, zéro échec, erreur ou test ignoré ; BUILD SUCCESSFUL**, dont minification R8 et lint vital release. Les options d'exécution limitent les workers et désactivent les daemons persistants.
- Vérification initiale : `./gradlew test assembleDebug assembleRelease` réussi. Un script de diagnostic externe au projet affichait le début et la fin des tests.
- Une première exécution complète a dépassé le délai de commande. La relance diagnostique et la commande finale réussissent sans changement du correctif ; la cause de ce blocage ponctuel n'est pas établie.
- `openspec validate --all --strict` : **11 passed, 0 failed**.

## Correspondance exigences / code / preuves

| Exigence | Implémentation | Preuves |
| --- | --- | --- |
| Fin de session BetaSeries | `KeystoreTrackingSession.kt:19`, `TrackingSessionKeys.kt:14`, `TrackingController.kt:58` | Les sept tests de `KeystoreTrackingSessionTest` couvrent conservation après panne de clé, remplacement en échec, clé absente, invalidation permanente puis nouveau jeton enregistré/relu, échec puis reprise du nettoyage de clé, GCM invalide et déconnexion explicite. `TrackingSessionRecoveryTest` couvre les erreurs d'identité/stockage, absence confirmée, reprise au premier plan, refus explicite et interruption pendant lecture. `TrackingSessionTest` garde la validation atomique et l'isolation des sessions. |
| Bornes réseau et délais | `BetaSeriesApi.kt:33`, transport inchangé | `BetaSeriesApiTest.genericHttpRefusalsNeverProveThatTheTokenExpired` couvre 401/403 avec corps JSON et non JSON ; `explicitTokenRefusalRequiresReconnectionRegardlessOfHttpStatus` conserve le refus explicite. `TrackingSessionRecoveryApiTest` garde les anciens contenus puis réussit avec le même jeton sans association ni révocation. Les tests existants de transport et de mutation restent inclus dans la suite complète. |
| Réglages du suivi de visionnage | `TrackingModels.kt:44`, `TrackingContent.kt:53`, `TrackingViewModel.kt:147` | `TrackingSettingsTest.restoringAndRetryingIdentityKeepsFocusAndNeverStartsAnAssociation` vérifie chargement, double appui sans double envoi, panne puis compte rétabli avec focus stable et sans QR. `retryAfterTemporaryStorageFailureReadsTheSavedSessionWithoutQr` vérifie la relecture du stockage. Les parcours existants de pays et d'association restent couverts. |
| Indicateur d'indisponibilité du suivi | `HomeScreen.kt:504`, `HomeScreen.kt:643`, `TrackingViewModel.kt:191` | `TrackingHomeNavigationTest.restorationFailureBeforeIdentityKeepsStatusAndRetryReachableFromTheGear` vérifie statut avant identité, détail depuis l'engrenage, réessai sans code et restitution du focus ; les parcours sans carte et les replis existants restent testés. |
| Couverture de la conservation de session | Tests précédents et ressources des cinq langues | Tests JVM avec transport/clé/horloge contrôlés, vrai fichier chiffré et D-pad Compose à la taille TV. Les assertions des quatre régressions ont été vérifiées avec les comportements défectueux réintroduits. |

## Correction de revue P2 — renouvellement après invalidation permanente

Statut : **corrigé et vérifié par les tests**, nouvelle revue indépendante du code changé encore à faire. La revue du head `35d36be` a identifié une clé invalidée conservée après suppression du fichier.

- `KeystoreTrackingSession.kt:29–35` supprime maintenant la clé définitivement invalidée avant de retirer le fichier et de retourner l'absence de session. Si cette suppression échoue, le fichier est conservé et STORAGE reste réessayable.
- `KeystoreTrackingSessionTest.confirmedPermanentKeyInvalidationAllowsSavingAndReadingANewSession` poursuit le scénario jusqu'à l'enregistrement et la relecture du jeton de remplacement avec une nouvelle instance du stockage.
- `KeystoreTrackingSessionTest.failedInvalidKeyCleanupKeepsSessionRetryableUntilANewKeyCanBeCreated` vérifie que l'échec de suppression garde le fichier intact, puis que réessai, nouvelle écriture et relecture aboutissent.
- Les deux tests échouent sur le code relu, respectivement en STORAGE lors de la nouvelle écriture et sur l'absence d'erreur attendue pendant le nettoyage. Après rétablissement du correctif, la suite complète de 1 248 tests et assembleRelease réussissent.

## Contrôle initial sur émulateur Android TV

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

### WARNING

- Le point P2 de la revue indépendante est corrigé avec les preuves ci-dessus ; cette vérification d'autrice ne vaut pas approbation indépendante du nouveau head. L'incident précis de la TV n'est pas diagnostiqué par les simulations ; une erreur de clé sans preuve structurée de perte définitive reste conservée et réessayable.

## Conclusion

Les contrôles de code, de tests automatisés et de navigation sur émulateur sont concluants. La validation sur TV réelle reste non vérifiée ; le change n'est pas prêt à être archivé tant que cette étape demeure ouverte.

## Publication

- PR : https://github.com/Sygix/SygixOs/pull/103.
- Commits signés de la branche de PR ; les corrections sont intégrées aux commits concernés par autosquash, sans changement supplémentaire du code testé.
- Scans des licences, commentaires de code ajoutés et informations privées : aucun écart détecté.
- CI test et CodeQL déclenchée à la création de la PR ; son état courant reste disponible dans la PR.
