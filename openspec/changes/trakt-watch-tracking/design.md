# Design

## Context

Voir `proposal.md` pour le périmètre. La base examinée est `main` au commit `92240bd`. `betaseries-session-recovery` et `betaseries-offline-state` sont implémentés mais non archivés ; leurs deltas constituent le contrat fonctionnel de référence.

Points d'intégration vérifiés :
- `WatchTrackingProvider` ne couvre aujourd'hui que compte, snapshot et révocation ; `TrackingRuntime`, `TrackingEnrichment` et `TrackingSyncCoordinator` utilisent encore directement `BetaSeriesProvider` pour les autres opérations.
- `TrackingSessionStore` persiste une chaîne chiffrée. `TrackingAccountLifecycle` et `TrackingRepository` lient ce jeton au cache via une empreinte et une incarnation. Un renouvellement Trakt ne doit pas être pris pour un remplacement de compte.
- `TrackingDatabase` est en version 2. Les clés de contenu comprennent déjà le fournisseur ; le compte est un emplacement unique, les intentions ont révision et état potentiellement envoyé, mais aucun verrou d'annulation durable.
- `TrackingController.record` planifie immédiatement le worker. `TrackingActions` propose déjà Annuler pour les marquages du menu, de la liste d'épisodes et de « Terminé ? ». Seul REMOVE diffère actuellement son envoi.
- `BetaSeriesConfigurationSource` conserve configuration et date pendant sept jours, sous mutex ; `BetaSeriesApi` invalide sur code 1001 et rejoue seulement les lectures, une fois.
- `TrackingSettingsContent`, son menu pays et les contrôles de position fournissent les composants réutilisables ; `TrackingProjection` et certains textes/badges nomment BetaSeries directement.

### Sources documentaires vérifiées et limites

Les liens ci-dessous décrivent les protocoles et structures, pas une preuve d'accès avec le client du site déployé. Aucune association Trakt, mutation ou vérification TV n'a été réalisée pendant cette spécification.

| Source | Champs et comportements documentés | Limites et usage retenu |
| --- | --- | --- |
| [Build du client web](https://github.com/trakt/trakt-web/blob/main/projects/client/vite.config.ts), [configuration OIDC](https://github.com/trakt/trakt-web/blob/main/projects/client/src/lib/features/auth/getOidcConfig.ts), [client API](https://github.com/trakt/trakt-web/blob/main/projects/client/src/lib/requests/api.ts) | `TRAKT_CLIENT_ID` remplacé par une chaîne au build ; utilisé comme `client_id` et `trakt-api-key` | Le nom de variable source ne subsiste pas nécessairement après minification. Extraire un contexte structurel vérifié dans les modules publiés, pas une chaîne hexadécimale arbitraire. Le chemin exact et l'extraction en six fichiers restent à prouver. |
| [OAuth](https://github.com/trakt/trakt-api/blob/master/projects/api/src/contracts/oauth/index.ts), [guide d'authentification](https://github.com/trakt/trakt-api/blob/master/projects/developer/src/lib/guides/authentication-oauth.md) | code appareil, code utilisateur, URL de vérification, intervalle et durée ; `access_token`, `refresh_token`, échéance ; renouvellement à usage unique | Requêtes OAuth sur `auth.trakt.tv`. Code appareil dans `code`, pas `user_code`. Secret client non requis par le contrat actuel. Compatibilité du client web avec le device flow et les méthodes de compte non vérifiée. |
| [Identité du compte](https://github.com/trakt/trakt-api/blob/master/projects/api/src/contracts/users/schema/response/settingsResponseSchema.ts) | `user.username`, `user.name` facultatif, `user.ids.uuid`, `user.ids.trakt` facultatif, slug | Utiliser une identité stable confirmée, jamais le seul nom mutable ; ne conserver que les champs nécessaires au suivi, pas les autres données personnelles de cette réponse. |
| [Watchlist](https://github.com/trakt/trakt-api/blob/master/projects/api/src/contracts/users/subroutes/watchlist.ts), [progression et mutations](https://github.com/trakt/trakt-api/blob/master/projects/api/src/contracts/sync/index.ts) | listes typées paginées ; progression avec série et prochain épisode ; ajout/retrait de watchlist ; historique ciblé | Vérifier que progression et watchlist couvrent les mêmes objectifs que le compte BetaSeries. Aucun téléchargement global de l'historique des films. Un endpoint documenté peut rester non autorisé pour ce client. |
| [Progression d'une série](https://github.com/trakt/trakt-api/blob/master/projects/api/src/contracts/shows/schema/response/showProgressResponseSchema.ts), [saisons et épisodes](https://github.com/trakt/trakt-api/blob/master/projects/api/src/contracts/shows/index.ts) | saisons/épisodes numérotés, `completed` booléen, dates de visionnage facultatives, épisodes avec `first_aired` ; détail d'une saison explicite | Les métadonnées de toutes les saisons ne sont pas chargées par défaut. Constituer les suivants avec progression puis détails nécessaires ; date absente/future exclue. Les réponses réelles de réserve inter-saisons sont à vérifier avant code. |
| [Séries masquées](https://github.com/trakt/trakt-api/blob/master/projects/api/src/contracts/users/subroutes/hidden.ts) | liste paginée et ajout/retrait de `progress_watched` | Sert à l'archivage distant, distinct de Retirer d'À suivre local. Ne pas utiliser une suppression d'historique de série comme raccourci. |
| [Historique ciblé](https://github.com/trakt/trakt-api/blob/master/projects/api/src/contracts/users/subroutes/history.ts), mutations sync | lectures d'un seul épisode/film, identifiants d'événement 64 bits et dates ; suppression par cible ou identifiant d'événement | Conserver les identifiants sans perte numérique. Le marquage vu est un état cible, pas un ordre d'ajouter systématiquement une lecture. Le passage vu peut retirer un film de watchlist : sa réconciliation ciblée doit être vérifiée. |
| [Images](https://github.com/trakt/trakt-api/blob/master/projects/developer/src/lib/guides/images.md), [offres](https://github.com/trakt/trakt-api/blob/master/projects/api/src/contracts/_internal/response/watchNowResponseSchema.ts) | images facultatives fanart/poster/thumb, parfois sans préfixe HTTPS ; offres par pays, source et liens Android facultatifs | Normaliser seulement les URLs vérifiées et utiliser le cache partagé existant. Les endpoints watchnow sont à accès limité ; une absence n'empêche pas le choix manuel du lecteur et n'est pas comblée par un autre service. |
| [Statuts](https://github.com/trakt/trakt-api/blob/master/projects/developer/src/lib/guides/status-codes.md), [limites](https://github.com/trakt/trakt-api/blob/master/projects/developer/src/lib/guides/rate-limiting.md) | 401 OAuth, 403 clé invalide ou application non autorisée ; 429 et Retry-After ; mutation au plus une par seconde en accès standard | Les statuts seuls sont ambigus, notamment pour les endpoints limités et protections HTTP. Le classificateur exige une preuve de protocole avant invalidation ou renouvellement. |

Les délais, la réserve et la persistance BetaSeries ont déjà leur source de vérité dans les specs et les deux changes dépendants ; les deltas Trakt y renvoient plutôt que créer une nouvelle politique de cache du compte.

## Goals / Non-Goals

**Goals :** une seule projection et une seule file durables, des protocoles fournisseurs isolés, aucune découverte de client au repos et aucun envoi contournant Annuler.

**Non-Goals :** voir `proposal.md`. Ne pas profiter de l'ajout de fournisseur pour modifier les clés de préférence de lecteur ; leur indépendance est le chantier séparé demandé par le propriétaire. Ne pas ajouter de moteur web ou de source d'images externe.

## Decisions

Toutes les décisions de cette section ont été validées en conversation ; le registre de `handoff.md` relie les groupes et leurs ajustements. Les mécanismes exacts d'un protocole non encore observé sont soumis à la vérification initiale, sans repli implicite.

### D1 — Socle partagé et adaptateurs

Étendre les interfaces de frontière réellement nécessaires aux usages existants : association, identité, lecture du compte et des statuts, résolution, saisons/épisodes, offres et mutations applicables. Le contrôleur, la projection, le dépôt, l'ordonnanceur et les présentations consomment ce contrat au lieu de vérifier le nom du fournisseur. Les décodeurs et les paramètres de protocole restent propres à chaque adaptateur. Une seule base Room et le worker existant suffisent.

Les actions applicables sont fournies selon l'état natif du contenu ; ne pas supposer que les états numériques de film BetaSeries sont des états Trakt. Les métadonnées de protocole utiles à la réconciliation restent associées au fournisseur derrière le dépôt, sans logique métier dans les composables.

Alternative écartée : une copie complète du runtime et de la base par service doublerait les courses, le cache et les tests au lieu de prolonger les frontières existantes.

### D2 — Configuration d'application découverte sans échéance

Mutualiser politique de cache, exclusions de sauvegarde, budgets et récupération ; garder un extracteur officiel par fournisseur. Réutiliser l'extraction BetaSeries connue et découvrir, pour Trakt, les points d'entrée puis les imports nécessaires du site. Contrôler domaine, HTTPS et redirections ; dédupliquer les ressources. Les six fichiers comprennent tous les imports lus. Lire et analyser séquentiellement les ressources nécessaires hors UI, puis libérer leur corps au lieu de conserver le site en mémoire.

Une configuration persistée et syntaxiquement exploitable ne déclenche aucune visite du site en raison de son âge, du démarrage, d'un retour au premier plan ou d'un travail de compte. Conserver le cache BetaSeries existant même si son ancien `fetchedAt` dépasse sept jours. Une configuration manquante suit la découverte initiale ; une erreur disque reste une erreur de stockage, pas une déconnexion.

Après rejet explicite, un seul résultat de récupération est partagé pour la génération de configuration rejetée. Une ancienne réponse n'invalide pas le remplacement. Une valeur identique encore rejetée ou un échec reste exposé en erreur au lieu d'entamer immédiatement une nouvelle extraction dans la même opération. Les requêtes gardent leur budget global et une mutation incertaine exige toujours réconciliation.

Alternative écartée : capturer les headers d'un navigateur obligerait à exécuter le site sur la TV. C'est un outil d'inspection possible lors de la vérification, pas le mécanisme de découverte embarqué accepté.

### D3 — Refus communs, preuves spécifiques

Partager la politique de reprise et de conservation de session, avec des résultats structurés : configuration, accès compte, endpoint limité, limitation, panne/protocole, stockage. Les adaptateurs reconnaissent seulement les preuves propres à leur service. Préserver le traitement BetaSeries 1001 et les refus explicites de jeton déjà testés.

Ne pas assimiler tous les 403 Trakt à un client expiré : un endpoint watchnow non autorisé ne doit pas lancer une nouvelle extraction. Si aucune preuve fiable n'est disponible, conserver la session et exposer une indisponibilité. Les limites Trakt de mutations sont appliquées au point de départ partagé entre premier plan et worker ; Retry-After et les budgets existants encadrent l'attente.

Alternative écartée : deux politiques de refus indépendantes reproduiraient la régression déjà corrigée par `betaseries-session-recovery`.

### D4 — Session Trakt et renouvellement coordonné

Faire évoluer le stockage protégé vers une enveloppe versionnée identifiant fournisseur, jetons, échéance et client d'origine. Lire l'ancien jeton brut comme une session BetaSeries ; ne pas écraser sa session ou son lien de base à la première lecture. Les secrets ne vont ni dans Room, ni dans les paramètres WorkManager, ni dans les logs.

Renouveler seulement avant un appel nécessitant un jeton expiré ou après preuve de refus renouvelable. Mutualiser le renouvellement entre les consommateurs et sauvegarder le nouveau couple avant publication. Maintenir l'incarnation et le compte lors du renouvellement ; adapter le lien d'empreinte actuel sans purger les intentions. Une panne entre fichier et lien de base reste explicitement récupérable, jamais interprétée comme un autre compte. Une réponse tardive après déconnexion ne recrée rien.

Les refresh tokens étant à usage unique, le transport ne rejoue pas aveuglément l'échange après réponse perdue. Conserver l'état d'incertitude et les données locales ; une preuve de refus définitif impose une reconnexion. Garder le client d'origine de chaque session : une nouvelle découverte ne transplante pas les jetons sur un autre client.

Alternative écartée : renouveler par timer indépendant réveillerait la TV sans besoin et multiplierait les risques de rotation concurrente.

### D5 — Chargements Trakt ciblés et même cache fonctionnel

S'appuyer sur watchlist et progression pour le compte, avec la liste de séries masquées. Les films à voir et les séries à commencer ou commencées rejoignent la projection commune ; l'état masqué de progression prime pour leur apport fournisseur. Compléter uniquement les séries nécessaires pour la réserve déjà spécifiée, avec progression et saisons explicites utiles plutôt qu'une récupération de toutes les métadonnées de toutes les saisons.

Les endpoints documentés et leurs filtres exacts sont vérifiés au départ : `/sync/watchlist`, `/sync/progress/up_next` ou progression documentée équivalente, `/users/hidden/progress_watched`, progression d'une série, saisons et historique ciblé. Une variante documentée ne peut remplacer une source que si elle couvre le même contrat et les mêmes budgets ; une impossibilité fonctionnelle nécessite un nouveau cadrage, pas une liste partielle présentée complète.

Conserver les échéances de compte et de statut, l'absence de relecture TV Provider, les protections de cache partiel, les identités de focus et le nettoyage existants. Utiliser les identifiants Trakt et externes typés, avec résolution locale non ambiguë. Les images et offres réellement disponibles passent par les frontières communes ; une offre inaccessible garde le choix manuel et son état non confirmé.

Alternative écartée : charger tout l'historique ou étendre les délais de cache au motif qu'un fournisseur change ne répond pas à la parité demandée.

### D6 — Actions natives sans suppression globale d'historique

Ajouter et retirer ciblent la watchlist. Archiver/désarchiver cible le masquage de progression `progress_watched`, en conservant son état précédent pour Annuler ; cet état prime sur une éventuelle présence en watchlist. Une série commencée qui continuerait à contribuer via sa progression expose Archiver au lieu d'un Retirer trompeur. Le masquage local reste inchangé.

Pour Vu, lire l'état ciblé avant un ajout à l'historique et confirmer sans ajout s'il est déjà vu. Pour Non vu, ne supprimer que les lectures de l'épisode ou film exact ; aucun objet série ou saison n'est envoyé à la suppression d'historique. La réconciliation du film doit aussi tenir compte de la watchlist automatiquement modifiée par Trakt, sans créer de revisionnage ni acquitter une opération dont l'état cible complet n'est pas établi.

La décision finale de différer l'envoi pendant Annuler rend inutile une sauvegarde de tout l'historique pour annuler le marquage courant : tant que son bouton est disponible, celui-ci n'est pas parti. Les révisions antérieures potentiellement envoyées conservent le contrat de réconciliation déjà existant ; annuler la révision courante ne supprime pas leur état utile.

Alternative écartée après validation : retirer une série en effaçant tout son historique Trakt.

### D7 — Retenue durable avant envoi des marquages

Ajouter à l'intention et à son contexte d'annulation le temps actif restant et l'état de libération. L'écriture initiale est durable avant projection ; une intention encore annulable reste projetée mais non sélectionnable pour un départ réseau. Tous les chemins, y compris Synchroniser, reprise et worker déjà planifié, contrôlent ce verrou au départ et lors de l'engagement par révision.

Réutiliser les pauses et le composant Annuler existants. La fin du décompte engage atomiquement la libération ; Annuler engage à la place la restauration du précédent état effectif et clôt la retenue. Restaurer après interruption la notice et le temps restant, sans vol de focus ; une demande retenue seule ne maintient pas un worker en boucle. La libération répare ou planifie le travail durable existant. Les écritures de temps ne dépendent pas des frames Compose et une panne disque ne peut être interprétée comme une autorisation d'envoi.

Après arrêt brutal, reprendre le dernier temps restant sauvegardé, même si cela prolonge légèrement la fenêtre ; ne jamais reconstruire par supposition une durée active plus courte depuis le temps mural écoulé. Cette garantie conservatrice a été validée explicitement. L'état de synchronisation des demandes héritées est conservé sans ajouter de fenêtre rétroactive ; seuls les nouveaux marquages entrent initialement en retenue.

Préserver les références nécessaires à Annuler jusque-là, puis les libérer par le nettoyage commun. Une déconnexion purge demandes et contexte ; une interruption seule conserve vu/non vu, tandis que REMOVE garde son abandon existant à la mort du processus. Les marquages du menu et de la confirmation de retour utilisent exactement cette même retenue.

Alternative écartée : différer seulement le lancement initial du worker ne protège pas contre un worker déjà actif ou un réessai manuel. La retenue doit être dans le dépôt durable.

### D8 — Sélecteur et composants existants

Réutiliser la ligne à valeur et la liste de « Position d'À suivre » pour Fournisseur, puis Connecter adapté au choix. Le fournisseur choisi est persisté ; BetaSeries est initial sans choix et imposé par une session BetaSeries existante. Une session, même en restauration ou en panne temporaire, verrouille le sélecteur visible. Le premier contrôle utilisable est le sélecteur sans session, sinon Synchroniser et ses états.

Réutiliser `PreferenceRow`, `GlassSurface`, tokens `Dimens`, typographies et focus sans zoom des réglages. Le panneau d'association et les notices restent ceux existants, leur contenu et attribution dépendant du fournisseur. Les noms et badges dans le héro et À suivre ne sont plus des constantes BetaSeries. Aucun nouvel écran, nouveau palier ni nouveau rendu de contrôle n'est nécessaire pour ce sélecteur demandé avec le composant existant.

Tags ajoutés : `tracking-provider`, `tracking-provider-list`, `tracking-provider-betaseries`, `tracking-provider-trakt`. Les tags d'association, de statut, `tracking-connect`, `tracking-sync`, `tracking-notification` et `tracking-undo` sont conservés. Les textes nouveaux et adaptés sont présents dans les cinq langues ; l'attribution correspond à celle requise par la source.

Alternative écartée par le propriétaire : deux lignes Connecter distinctes. Le remplacement direct de compte par changement de liste a également été écarté.

## Risks / Trade-offs

- [Client web incompatible avec le device flow ou l'API nécessaire] → tâche initiale bloquante sur appareil ; rapporter l'échec au propriétaire avant tout changement de méthode, sans utiliser une autre application cliente ou un navigateur embarqué.
- [Minification ou imports dépassant les plafonds] → contrat d'extraction testé sur ressources déployées, échec explicite et borné ; ne pas promettre que toute future version du site sera récupérable sans mise à jour.
- [Client d'application révoqué et session liée à ce client] → conservation du lien et reconnexion explicite si le service interdit l'utilisation ; aucune promesse de transfert de jetons.
- [Rotation OAuth réussie côté serveur mais réponse ou sauvegarde perdue] → échange non rejoué aveuglément, état incertain visible, conservation des intentions et tests de panne ; reconnexion si refus définitif confirmé.
- [Worker contournant Annuler] → verrou transactionnel de dépôt, contrôlé pour toutes les voies, tests avec worker déjà en cours et interruption.
- [Fenêtre suspendue en arrière-plan] → le marquage reste durable et affiché localement mais son départ attend réellement la fin des secondes actives ; c'est le comportement validé, pas un envoi garanti après dix secondes murales.
- [Deltas dépendants encore ouverts] → appliquer l'ordre de `proposal.md`, réunir les scénarios de récupération de session et de cache durable avant archivage ; ne pas remplacer une exigence par une version antérieure.

## Migration Plan

1. Vérifier les contrats Trakt sur appareil, puis fixer dans le rapport seulement structure, types, compteurs, domaines officiels et chemins de protocole, sans identifiants extraits ni données personnelles.
2. Étendre le schéma Room avec migrations non destructives et la session protégée versionnée ; tester une vraie base version 2, un jeton BetaSeries brut, sa file et son cache de configuration âgé. Les intentions héritées conservent leur état de synchronisation, sans disparition ni fenêtre Annuler rétroactive.
3. Conserver fournisseur et incarnation existants, brancher les adaptateurs puis le sélecteur. Une erreur de migration ou de stockage ne lance jamais de remise à zéro automatique.
4. Valider la retenue durable et les tests contractuels communs avant les parcours TV des deux fournisseurs. Documenter qu'un retour à une ancienne version ne connaît pas les nouveaux jetons et verrous ; il n'est pas un chemin garantissant la même protection d'annulation.
5. Après merge et validation TV, archiver dans l'ordre de `proposal.md`, puis valider toutes les specs consolidées. Aucun archivage n'est effectué pendant cette session de cadrage.

## Open Questions

Aucune décision de repli n'est préapprouvée. Les vérifications initiales de protocole et de plate-forme restent ouvertes et explicitement bloquantes ; si elles imposent un autre comportement, une autre source ou d'autres budgets, revenir au propriétaire avec le contexte avant de modifier le change.
