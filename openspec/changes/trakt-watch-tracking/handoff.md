# Handoff — Suivi Trakt et configuration d'application

## Mise à jour de l'implémentation

Le propriétaire a lancé l'implémentation dans une PR empilée sur la PR de spécification et a choisi l'émulateur Android TV pour tous les contrôles, à la place de la TV physique. `verification.md` contient les observations Android authentifiées et leurs limites. Il a également demandé de conserver la connexion de diagnostic afin de ne plus répéter les associations ; celle-ci est chiffrée et reste privée à l'émulateur, jamais dans le dépôt. L'état de cadrage ci-dessous est historique ; l'avancement courant fait foi dans `tasks.md`.

## État de cette session

Cadrage uniquement, rédigé sur `main` au commit `92240bd`, branche `docs/spec-trakt-watch-tracking`. Change : `trakt-watch-tracking`. Proposal, design, huit deltas et tâches d'implémentation sont présents. Aucun code applicatif, build, association Trakt ou essai TV n'a été réalisé ici ; toutes les tâches d'implémentation restent ouvertes.

Le propriétaire a demandé de valider les décisions par petits groupes interactifs plutôt que de relire les documents. Les décisions ci-dessous ont été soumises avec contexte et validées. Ne pas lui demander à nouveau le fonctionnement déjà spécifié de BetaSeries ; s'appuyer aussi sur ses évolutions fusionnées et non encore archivées.

## Registre des décisions validées

| Sujet | Décision finale et contexte |
| --- | --- |
| Fournisseurs | Un seul compte actif, BetaSeries ou Trakt ; Trakt reprend le parcours complet, pas une première version limitée à l'affichage. |
| Référence fonctionnelle | Réutiliser le fonctionnement BetaSeries, y compris cache durable, prochain épisode et cinq suivants, films, hors ligne, synchronisation en arrière-plan, nettoyage et fusion avec les programmes locaux. Les paramètres d'API restent propres au service. |
| Lecteur | Le propriétaire souhaite rendre sa préférence indépendante du compte et du fournisseur, mais réserve ce correctif à une autre session. Ne pas l'implémenter à l'occasion de Trakt. |
| Configuration d'application | Découverte dynamique depuis les ressources officielles, puis cache persistant sans expiration périodique pour les deux fournisseurs. La suppression du renouvellement BetaSeries après sept jours est explicitement incluse. Seul un rejet explicite du client peut déclencher sa redécouverte automatique. |
| Méthode et coût | Le propriétaire a proposé la capture de headers puis délégué le choix robuste et léger. Méthode retenue : analyse statique, pas de navigateur/WebView ni d'exécution JavaScript sur la TV ; une page et six scripts/imports maximum, 10 Mio par ressource, 30 secondes au total, hors UI. |
| Configuration et session | Deux objets séparés, aux finalités différentes. Garder le client d'origine des jetons ; ne pas les réattribuer à un nouveau client découvert. Si le service ne permet plus leur usage/renouvellement, proposer Reconnecter en conservant les demandes du même compte sans déconnexion explicite. |
| Renouvellement Trakt | À la demande pour jeton expiré ou explicitement refusé, mutualisé entre accueil et worker, sauvegarde atomique du couple avant utilisation, aucun réveil périodique. Panne distincte de refus définitif. |
| Architecture | Un socle partagé : même base, projection, file, ordonnanceur et politique de refus ; adaptateurs réseau/authentification/configuration par fournisseur. Préserver session BetaSeries et demandes à la mise à jour. |
| Refus | Mutualiser la politique commune ; les adaptateurs apportent les preuves de protocole. 401/403 générique, corps non JSON ou endpoint limité ne permettent pas d'effacer la session ni de relancer la découverte par supposition. |
| Réseau | Budgets BetaSeries réutilisés pour Trakt ; ajout de la régulation Trakt d'une mutation par seconde, respect de Retry-After et reprises espacées. |
| Lectures Trakt | Watchlist et progression, pagination complète, compléments seulement utiles à la réserve ; statuts et historique ciblés, pas d'historique global des films ni de toutes les métadonnées de saisons. Images/offres réellement fournies dans les caches existants, aucune nouvelle source externe. |
| Actions Trakt | Ajouter/Retirer sur watchlist ; Archiver masque la progression en conservant les vus. Pour une série commencée qui resterait suivie, proposer Archiver plutôt qu'un Retirer supprimant l'historique. Retirer d'À suivre reste purement local. |
| Vu et non vu | Vu ne crée pas de lecture supplémentaire si le contenu est déjà vu ; Non vu cible toutes les lectures du seul épisode ou film concerné, jamais les épisodes précédents. |
| Annuler : clarification | Le propriétaire croyait initialement que les marquages étaient déjà temporisés. Le code vérifié montre Annuler aussi pour les actions du menu, mais envoi vu/non vu planifié immédiatement. Après vérification de ce point et son accord conditionnel, la décision finale est de différer ces envois pour les deux fournisseurs, sans créer un nouveau bouton ou une nouvelle surface. |
| Annuler : règle finale | Effet local immédiat et durable, aucun envoi du marquage avant fin des dix secondes actives existantes ; mêmes pauses sur focus/arrière-plan/réglages. Annuler pendant la fenêtre restaure localement, sans mutation de compensation du marquage annulé. |
| Annuler : interruption | Sauvegarder le temps restant et reprendre notice/décompte au retour de l'accueil après arrêt du processus ou de la TV ; le worker ne contourne pas la retenue. Après arrêt brutal, reprendre le dernier temps durable : un peu plus est admis, jamais moins par supposition. Pas d'écriture par frame. |
| Annuler : migration | Les demandes antérieures gardent leur état de synchronisation, notamment un éventuel envoi déjà parti, sans nouvelle fenêtre rétroactive. La nouvelle retenue concerne les nouveaux marquages. |
| Choix dans les réglages | Le propriétaire a préféré un sélecteur utilisant notre composant existant, puis Connecter adapté au choix, plutôt que deux boutons de connexion. Choix mémorisé ; BetaSeries initial sans compte ni choix ; ordre BetaSeries puis Trakt. |
| Compte connecté | Sélecteur visible mais verrouillé avec « Déconnectez-vous pour changer de fournisseur ». Premier contrôle focusé : sélecteur sans session, sinon Synchroniser et ses états. La déconnexion explicite et sa purge précèdent le changement. |
| Replis non préapprouvés | Si extraction ou device flow avec le client web échoue, revenir au propriétaire avant de changer de méthode. Ni clé fixe, ni autre client emprunté, ni application à enregistrer, ni navigateur embarqué ne sont des replis autorisés. |
| Notes des changes liés | Ajout d'une note d'ordre d'archivage dans chacun des deux designs BetaSeries autorisé ; leurs tâches et leurs preuves sont conservées. |

La proposition initiale de conserver des lectures pour Annuler un marquage déjà envoyé a été dépassée par la décision finale : le marquage courant reste retenu tant qu'Annuler est disponible. La réconciliation des anciennes révisions potentiellement envoyées reste nécessaire ; ne pas réintroduire une copie globale d'historique pour cette annulation.

## Départ de la prochaine session

1. Reprendre cette branche et lire `proposal.md`, `design.md`, les deltas et `tasks.md`, ainsi que les deux changes BetaSeries dépendants. Vérifier la base courante avant tout diagnostic ou modification.
2. Commencer par 1.1 et 1.2 : la documentation publique confirme les champs, pas l'acceptation effective du client web par le device flow ni l'accès aux endpoints requis. L'extraction du site déployé dans six fichiers et les réponses réelles de réserve restent également à prouver.
3. Si ces vérifications réussissent, appliquer les tâches dans l'ordre. Conserver les migrations et non-régressions BetaSeries, le verrou durable Annuler et le renouvellement sans changement d'incarnation parmi les critères essentiels.
4. Tout choix nouveau imposé par un résultat réel doit être présenté au propriétaire avec contexte et options, en petits groupes ; une réponse de cadrage ne vaut pas autorisation de changer silencieusement la méthode.

## Dépendances et archivage

Les implémentations `betaseries-session-recovery` et `betaseries-offline-state` sont dans la base, mais leurs validations TV et archivages restent ouverts. Ordre : récupération de session, état hors ligne, puis Trakt. Les deltas du présent change préservent leurs scénarios cumulés, notamment indicateur sans identité, restauration temporairement en panne et projection durable. Ne pas considérer leur présence dans `main` comme preuve d'une validation TV accomplie.

## Vérifications de la spécification

- `openspec status --change trakt-watch-tracking` : quatre artefacts de planification sur quatre terminés ; les tâches d'implémentation restent toutes ouvertes.
- `openspec validate --all --strict` : `Totals: 12 passed, 0 failed (12 items)`.
- L'information d'archivage « Synchronisation des intentions locales — not found » est attendue contre les seules anciennes specs courantes : cette exigence est ajoutée par `betaseries-offline-state`, qui doit être archivé avant le présent change. Ce n'est pas une autorisation d'archiver dans le désordre.
- Contrôle en mémoire, depuis les représentations JSON OpenSpec des specs et des trois changes ordonnés : les dix exigences MODIFIED ont une cible existante après leurs prérequis et conservent tous les noms de scénarios cumulés. Résultats : `Archive-order target check: PASS` et `Cumulative prerequisite scenarios preserved: PASS`.
- Scan des nouveaux documents : aucun chemin local, adresse IP ou affectation de secret détecté. Les notes dans les deux designs dépendants ont été relues séparément et ne modifient ni tâches ni preuves.

Les vérifications réseau authentifiées, les migrations exécutées, les tests applicatifs et le rendu sur TV relèvent de l'implémentation ; aucun n'est déclaré réalisé par ce handoff. La validation syntaxique et la conservation des scénarios ne prouvent pas la compatibilité du client web Trakt avec le device flow.
