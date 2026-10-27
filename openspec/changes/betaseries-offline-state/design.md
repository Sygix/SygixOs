# Design

## Context

Voir `proposal.md` pour la motivation et le périmètre accepté. `TrackingController` conserve le compte dans un `StateFlow`, `TrackingActions` une seule notice volatile et `BetaSeriesProvider.snapshot` ne demande qu'un épisode par série. `verify` mélange actuellement confirmation du marquage et récupération du suivant : un échec du second peut masquer le succès du premier. Room/KSP sont déjà utilisés pour les images ; WorkManager n'est pas encore une dépendance.

### Sources vérifiées et limites

| Source | Données exploitées dans le code et les tests actuels | Limites et traitement |
| --- | --- | --- |
| `members/infos` | `member.id`, `member.login` textuels après décodage | Persister l'identité liée à la session ; ne pas exiger ce réseau pour restaurer le cache d'une session déjà associée. |
| `shows/member`, `movies/member?state=0` | identifiants, titres, `user.archived`, `user.status` entier, images, identifiants externes, offres facultatives | Pagination déjà spécifique à chaque endpoint ; conserver la notion de résultat incomplet. Ne pas télécharger les films vus pour reconstituer tout l'historique. |
| `episodes/list` | groupes `shows`, `unseen`, id, saison/épisode entiers, date textuelle, `user.seen` booléen ; `limit`, `showsLimit`, `released`, `showId` et pagination existants | La réponse réelle avec six épisodes et son franchissement de saison ne sont pas vérifiés ici. Première tâche : valider ce contrat sur appareil ; repli technique par saisons explicites si nécessaire, sans pagination inventée. |
| `episodes/display`, `movies/movie`, `shows/display` | statut individuel, état de film exact et appartenance selon les champs présents | Un champ absent n'est pas une confirmation. Pas de version distante ni horodatage de mutation fiable exploité : ne pas prétendre détecter un conflit chronologique. |
| mutations BetaSeries | épisode POST avec `bulk=false`, DELETE isolé ; film POST `state=1` ou `state=0`, restauration d'état exact pour Annuler | Aucun identifiant d'idempotence serveur établi. Lire avant répétition après incertitude ; comparer à l'état désiré, pas rejouer aveuglément une commande. |
| session protégée | fichier chiffré et jeton restaurable via `TrackingSessionStore` | Ajouter le lien durable session/compte sans copier le jeton dans Room ou les données WorkManager. |
| TV Provider et images | identités locales et cache partagé déjà présents | Aucune nouvelle colonne ni lecture de programmes déclenchée par la synchronisation du suivi. Les URL d'images sont des métadonnées, pas une garantie d'octets hors ligne. |

La documentation source de [WorkManager](https://github.com/androidx/androidx/blob/androidx-main/work/work-runtime/src/main/java/androidx/work/WorkManager.kt) confirme les travaux persistants contraints, les travaux uniques, l'annulation au mieux et les limites d'exécution. Le comportement réel sur TV sous Android 14 reste à valider ; aucun envoi instantané n'est garanti en veille ou après arrêt forcé Android.

## Goals / Non-Goals

**Goals:** une seule source de vérité durable pour le suivi ; même projection au premier plan et après exécution en arrière-plan ; transactions empêchant la perte d'intentions lors des courses réseau.

**Non-Goals:** voir `proposal.md`. Ne pas faire de la base d'images, évictable et recréable, le stockage de demandes utilisateur.

## Decisions

### D1 — Base dédiée et projection unique

Créer une base Room privée dédiée, exclue des sauvegardes transférables, avec des frontières de dépôt injectables. Réutiliser la dépendance Room existante plutôt qu'un fichier JSON de compte : la file, les confirmations et le nettoyage nécessitent des transactions. Ne pas utiliser le fallback destructif de `ArtworkDatabase` pour les intentions utilisateur.

Modèle logique minimal :
- session active : fournisseur, id du compte, identité affichable, identifiant opaque d'incarnation de session, dates et complétude des dernières lectures ; le lien avec le jeton protégé est validé avant exposition ;
- contenus : clé `(provider, accountId, type, id)`, parent, métadonnées communes facultatives, date de diffusion, statut distant connu, date de confirmation et état de réserve ;
- intentions : même clé de compte/contenu, état désiré explicite, révision locale monotone, état avant action utile à Annuler, état d'envoi et erreur réessayable ;
- contexte d'annulation limité à la fenêtre existante, conservant les données utiles au retour ; aucune archive historique illimitée.

Un `Flow` transactionnel expose l'état effectif : dernier état distant + intentions locales, puis sélection du candidat par série. Les contrôleurs et composables consomment cette projection, sans seconde liste mutable indépendante. Dates persistées en temps civil avec horloge injectable ; ne pas persister la valeur monotone actuelle de `snapshotAt` comme si elle survivait au redémarrage.

### D2 — Écriture locale avant effet visible

Pour un épisode ou film identifié appartenant au compte courant, enregistrer l'intention vu/non vu en transaction puis publier la projection immédiatement. Une erreur disque n'annonce pas de sauvegarde et ne déclenche pas un envoi non journalisé. Le cache précédemment lié au compte permet cet usage hors ligne ; une identité ambiguë ne le permet pas.

Conserver au plus l'intention la plus récente par contenu ; le worker capture sa révision. Si l'utilisateur fait vu puis non vu pendant un envoi, le retour de l'ancien envoi ne supprime jamais la nouvelle intention. Sur des épisodes différents, chaque demande reste isolée : aucun marquage implicite des précédents.

Annuler conserve la fenêtre et le parcours existants. Avant envoi, restaurer l'état précédent sans envoyer l'action annulée ; si un envoi est parti ou confirmé, enregistrer l'état désiré inverse à synchroniser. Pour un film, conserver `movieState` exact afin de rétablir son état précédent sans retrait ni revisionnage implicite. La fin du processus n'abandonne plus les intentions vu/non vu, mais continue d'abandonner le retrait différé non envoyé.

### D3 — Réserve glissante distincte des candidats

Après chargement du compte, conserver pour chaque série non archivée le prochain diffusé non vu et cinq suivants diffusés non vus, ordonnés par saison/épisode ; si la fin disponible est prouvée, conserver moins. La date de diffusion doit être connue et ne pas dépasser la date courante. Le complément traverse les saisons et ne dépend ni du nombre de cartes ni de la saison ouverte dans le sélecteur.

Demander six candidats via `episodes/list` si le contrat réel le permet. Sinon utiliser les endpoints de saisons et d'épisodes déjà présents, avec saisons explicites, jusqu'à constituer cette fenêtre ou prouver la fin ; conserver délais, pagination et concurrence bornés. Le protocole exact retenu sera consigné par la première tâche de vérification. Le sélecteur ne prétend pas disposer d'une saison entière du seul fait de six entrées préchargées.

Le marquage local choisit immédiatement le prochain candidat déjà enregistré. Réserve épuisée ne signifie pas « série à jour » : exposer l'attente du réseau sans inventer de suite. En ligne, compléter uniquement la réserve de la série touchée. Si des intentions locales concernent des épisodes encore renvoyés non vus par le serveur, les appliquer avant de compter les six candidats et continuer la récupération bornée si nécessaire. Un résultat partiel ne confirme jamais une fin de série.

Un film conserve son état et ses métadonnées ; vu le retire de l'apport à regarder, non vu le rétablit si ses données sont connues. Aucun calcul de suivant ni appel de réserve n'est effectué pour lui.

### D4 — Confirmation indépendante du complément

Séparer la mutation et la vérification du statut cible de la reconstitution de réserve. Après résultat incertain ou reprise après crash, relire la cible : si son état correspond déjà, confirmer sans renvoi ; sinon envoyer l'état désiré courant puis vérifier le résultat. Une panne de lecture ne permet pas un nouvel envoi aveugle. La dernière intention explicite de la TV gagne tant qu'elle est en attente, même si l'état distant a changé ailleurs.

Avant le départ réseau, marquer durablement la révision comme potentiellement envoyée. En transaction de confirmation, mettre à jour l'état distant et supprimer seulement l'intention de même révision et même incarnation. Une intention plus récente reste appliquée. Une réponse ancienne de rafraîchissement ne remplace pas une confirmation plus récente : sérialiser les engagements par compte et contrôler le point de départ des lectures.

Une fois le statut confirmé, l'action est acquittée même si le complément d'épisodes échoue. Cette panne devient une réserve incomplète à recompléter, pas une raison de renvoyer le marquage.

### D5 — Travail persistant à la demande

Ajouter WorkManager, avec un travail unique par incarnation de compte, contrainte réseau connecté, backoff exponentiel et respect des indications serveur. Planifier après écriture d'une intention, au réessai manuel et réparer une planification manquée à la restauration/au retour au premier plan. Le worker ne dépend ni d'une Activity ni de `TrackingRuntime` complet : utiliser le dépôt, le client API et l'accès protégé à la session uniquement, sans initialiser images, lecteur ou TV Provider.

Drainer les intentions et compléments requis en lots bornés, avec les budgets d'appels existants. Le même coordinateur évite tout envoi concurrent du worker et d'un essai au premier plan. Vérifier la session/compte/incarnation avant chaque envoi et avant engagement ; une simple annulation WorkManager ne suffit pas à l'isolation. La transaction DB et la planification Android ne sont pas atomiques : un contrôle durable de travail restant et une vérification en fin de lot évitent la perte d'une intention arrivée pendant la fermeture du worker.

Ne maintenir aucun travail réseau périodique quand il ne reste ni intention ni complément manqué. Un refus d'accès suspend les envois et demande la reconnexion ; une panne réseau conserve les données. La reconnexion au même compte peut reprendre les données si aucune déconnexion explicite ne les a purgées ; un compte différent déclenche la purge de l'ancien. Le défaut temporaire de stockage reste distinct de l'absence de session.

### D6 — Nettoyage référentiel après succès

La confirmation supprime l'intention acquittée, pas le cache entier. Dans la même séquence de dépôt, purger les épisodes consommés ou hors fenêtre lorsqu'ils ne sont plus référencés par une intention, une annulation active ou une preuve de statut encore nécessaire à une reprise locale déjà connue. Réévaluer ces références à la fermeture de l'annulation, lors d'une nouvelle projection locale et après complément. Ainsi la réserve glisse sans devenir un historique de tous les vus.

Un épisode confirmé vu peut encore devoir écarter une vieille reprise publiée par une app : garder sa preuve minimale tant que cette reprise reste pertinente, sinon sa suppression immédiate réafficherait l'épisode précédent. Les absences d'une lecture partielle ou en erreur ne sont pas une preuve permettant de purger ces références.

Pour un film confirmé vu, retirer l'entrée complète devenue inutile dès qu'aucune intention, annulation ni preuve locale ne la référence. Ne pas confondre « absent des films à voir » avec « film non suivi » pour une demande non vu encore en attente. Les images suivent leur propre cache partagé ; supprimer une association de compte n'efface pas les octets encore utilisés par un autre programme.

### D7 — Déconnexion et lien session/compte

La déconnexion rend d'abord la session non utilisable et invalide son incarnation, puis annule son travail, efface ses données de compte et le jeton et tente la révocation au mieux. Sérialiser cette frontière avec le départ des requêtes : après invalidation, aucun nouvel envoi de l'ancienne incarnation n'est autorisé. Une requête déjà partie peut avoir eu un effet distant, mais ne repeuple jamais la base après purge. Prévoir une purge locale reprise au prochain démarrage si l'effacement disque a échoué ; aucune ligne résiduelle n'est alors affichée ni synchronisée.

Le jeton et le lien durable de compte peuvent être dans des stockages distincts : identifier explicitement les états de remplacement incomplet et ne jamais associer par supposition un nouveau jeton au cache précédent. Une nouvelle identité confirmée différente purge l'ancien compte avant exposition ; un renouvellement du même compte conserve les demandes sauf déconnexion explicite.

### D8 — Présentation dans les composants existants

Réutiliser notice compacte, indicateur de suivi, détail et contrôle Synchroniser. Afficher « Enregistré sur la TV · En attente de synchronisation » après sauvegarde locale ; réserver « Synchronisé » à la confirmation distante. L'indicateur agrège les demandes restantes et les erreurs/réserves incomplètes, sans notification répétée, sans nouveau badge par carte ni vol de focus. Une erreur de stockage reste distincte d'une panne distante.

Réutiliser `tracking-notification`, `tracking-undo`, `tracking-status`, `tracking-status-detail`, `tracking-status-retry`, `tracking-sync` et les tags de cartes/héro existants. Rendu `GlassSurface`, tokens et D-pad restent ceux des exigences de navigation ; les textes sont traduits dans les cinq langues. Les contenus BetaSeries du cache restent admissibles sans réseau ; les octets d'image restent soumis aux échéances du cache existant, puis aux placeholders/replis.

## Risks / Trade-offs

- [Contrat six épisodes non encore vérifié] → première tâche bloquante sur appareil ; documenter la structure et les compteurs uniquement, puis fixtures anonymes. Ne pas déclarer ce contrat déjà garanti.
- [Exécution Android différée] → WorkManager persistant et reprise au retour ; validation TV pendant autre application, après redémarrage et en sortie de veille, sans promesse d'instantanéité.
- [Crash entre envoi et acquittement] → état d'envoi durable, lecture de réconciliation, suppression conditionnelle par révision.
- [Nettoyage trop agressif] → références explicites et tests de film/épisode encore publié localement ; la réussite d'une mutation n'implique pas une liste complète.
- [Autre change de récupération de session] → ordre et réconciliation décrits dans `proposal.md` ; ne pas écraser son traitement des pannes temporaires ni étendre la reprise aux autres mutations.

## Migration Plan

Créer le schéma de suivi sans toucher au format des préférences et identités de catalogue. À la première exécution, le jeton existant permet une lecture d'identité puis un premier remplissage ; sans cache déjà créé et sans réseau, ne pas inventer de compte ni de contenus. Les migrations futures conservent intentions, incarnation et confirmations ; une erreur de migration expose une erreur de stockage sans effacement destructif.

Un retour à une ancienne version ne sait pas synchroniser cette nouvelle file : documenter cette limite, conserver la base et ne pas présenter ce retour comme une synchronisation accomplie. Archiver après merge et validation sur TV réelle. Aucun code ni essai TV n'est exécuté dans cette phase de spécification.
