# Proposal

## Why

Permettre de choisir Trakt comme fournisseur de suivi, avec le même parcours et le même fonctionnement hors ligne que BetaSeries. Éviter les découvertes périodiques inutiles de configuration d'application et les mutations envoyées pendant une fenêtre Annuler encore ouverte.

## What Changes

- Ajouter Trakt avec association TV par code et QR, identité du compte, films à voir, prochains épisodes, réserve, actions applicables, cache durable et synchronisation des intentions.
- Conserver un seul compte actif et proposer le fournisseur via la liste déroulante existante, suivie de Connecter ; verrouiller ce choix tant qu'une session existe.
- Partager stockage, projection, synchronisation et politique d'erreurs ; adapter les protocoles de chaque fournisseur derrière les frontières communes. Conserver la session BetaSeries et ses données à la mise à jour.
- Découvrir la configuration depuis les ressources web officielles, sans navigateur ni exécution JavaScript. Pour les deux fournisseurs, réutiliser le cache sans expiration périodique et redécouvrir seulement après rejet explicite de la configuration.
- Garder les jetons utilisateur distincts de la configuration d'application ; renouveler ceux de Trakt à la demande en conservant leur lien au client qui les a émis.
- Pour BetaSeries et Trakt, sauvegarder et projeter vu/non vu immédiatement mais différer l'envoi jusqu'à la fin de la fenêtre Annuler existante. Conserver le temps restant et reprendre l'annulation après interruption.
- Adapter les actions Trakt à ses objets natifs : ajout/retrait de watchlist, archivage dans la progression, marquage d'un épisode ou film isolé ; ne pas effacer globalement l'historique pour retirer une série du suivi.

## Capabilities

### New Capabilities

- `trakt-integration` : association et session Trakt, lectures et correspondances d'identités, réserve et mutations propres à ce service.
- `tracking-application-configuration` : découverte bornée des configurations d'application, cache persistant sans échéance et récupération mutualisée après rejet explicite.

### Modified Capabilities

- `watch-tracking` : choix entre les fournisseurs, actions applicables Trakt et libération durable des marquages après Annuler.
- `betaseries-integration` : rattachement au contrat commun de configuration et suppression du renouvellement après sept jours.
- `settings` : choix du fournisseur, adaptation des contrôles et attribution, continuité du compte existant.
- `launcher-shell` : présentation et masquage du fournisseur actif dans le héro, avec conservation du comportement hors ligne déjà livré.
- `up-next` : badges et retraits indépendants du nom BetaSeries ; contrats de fusion, tri et persistance conservés.
- `ui-testing` : couverture des deux fournisseurs, configuration, renouvellement, migration, sélection et annulation durable avant envoi.

## Impact

Frontières concernées : `TrackingRuntime`, `TrackingSyncServices`, `WatchTrackingProvider`, `TrackingSessionStore`, `TrackingController`, `TrackingAccountLifecycle`, `TrackingRepository`, schéma Room, `TrackingSyncCoordinator`, `TrackingActions`, enrichissement, projection, modèles et présentations de suivi. Les identifiants de fournisseur existent dans les identités et la base, mais plusieurs dépendances restent concrètement liées à `BetaSeriesProvider` ; le jeton protégé actuel est une chaîne sans enveloppe de fournisseur.

Les API et ressources web officielles Trakt s'ajoutent aux sources existantes. Aucun nouveau backend, navigateur, ordonnanceur ou service externe de métadonnées n'est prévu. Les bibliothèques réseau, Room, WorkManager, QR et le cache partagé d'images sont réutilisés.

La possibilité d'extraire le client du site déployé dans les bornes retenues et de l'utiliser avec le parcours TV n'est pas encore vérifiée. La première tâche vérifie ces contrats sur appareil avant le code d'intégration ; tout échec nécessitant un changement de méthode est rapporté au propriétaire.

## Dependencies / Archive Order

Base : `main` contenant les implémentations de `betaseries-session-recovery` et `betaseries-offline-state`. Leurs validations TV et archivages restent distincts de ce cadrage.

Ordre d'archivage : `betaseries-session-recovery`, puis `betaseries-offline-state`, puis `trakt-watch-tracking`, chaque fois après merge et validation TV. Les deltas communs du présent change sont écrits sur le contrat résultant de ces deux changes, pas sur les seules anciennes specs courantes. Préserver tous leurs scénarios lors de la synchronisation. Le présent change ne les archive pas et ne modifie pas leurs preuves de validation.

## Non-goals

- Comptes simultanés, fusion BetaSeries/Trakt et transfert d'historique entre services : reportés à un éventuel change dédié.
- Correctif rendant la préférence de lecteur indépendante du fournisseur et du compte : autre session demandée par le propriétaire ; conserver les parcours et préférences existants sans redéfinir ce chantier.
- Scrobbling automatique, recherche générale, revisionnage et téléchargement de vidéos : hors périmètre de cette phase. « Terminé ? » reste une confirmation explicite.
- Ajout, retrait et archivage hors ligne : restent hors du périmètre de la file persistante, comme dans `betaseries-offline-state`.
- Application Trakt à enregistrer, clé fixe de secours, navigateur/WebView ou exécution de scripts distants : aucun de ces replis n'est autorisé par les décisions de ce change ; une incompatibilité exige un nouveau cadrage.

## Questions ouvertes

Aucun choix produit ou technique supplémentaire n'est considéré comme acquis. Les décisions ont été validées en petits groupes interactifs ; leur registre et les limites des vérifications figurent dans `handoff.md`. Les contrats réseau non observés constituent les vérifications bloquantes de la première tâche, pas des capacités déjà démontrées.
