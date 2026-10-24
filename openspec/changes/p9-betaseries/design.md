# Design

## Context

Voir `proposal.md` pour le périmètre et `technical-verification.md` pour les contrats observés. Le suivi complète les programmes Android sans ajouter de rangée ni de faux paquet au catalogue. La confirmation de fin est uniquement manuelle.

## Goals / Non-Goals

**Goals:** identité et lecteur communs au héro et à À suivre, états explicites, isolation des sessions, mémoire locale préservée et lectures pilotées par événements.

**Non-Goals:** preuve automatique de fin via sessions média, accès aux notifications, abonnements ou bibliothèques privées, recherche globale P8, fournisseur supplémentaire et backend.

## Decisions

### D1 — Frontières et état

`TrackingController` possède la session et les objets de cache par contenu (données, fetchedAt, validité, erreur individuelle). `TrackingViewModel` traduit les intents en StateFlow, y compris décisions d'action et de focus ; les composables rendent cet état. Fournisseur, stockage, identification, catalogue de lecteurs et lancement restent des frontières testables. Aucune invalidation par compteur global.

Une lecture complète intervient à la connexion, puis paresseusement au retour au premier plan si le dernier instantané réussi a plus de 15 minutes. Pas de boucle de synchronisation au repos. Une mutation relit uniquement sa fiche et patche son entrée. Une incohérence d'élément ne retire pas les autres ; une pagination partielle ne devient jamais une preuve d'absence.

Après un épisode vu, la fiche confirme son statut ; une lecture supplémentaire `episodes/list?showId=…&limit=1&showsLimit=1&released=1` découvre le prochain candidat de cette seule série, même dans une autre saison. Aucun endpoint vérifié ne fournit à la fois ces deux informations dans la fiche d'épisode. Les statuts restent en cache par épisode ; la projection choisit un seul non-vu par série, recalculé après Annuler.

### D2 — Configuration, association et stockage

Découverte limitée à la page officielle et six scripts de www.betaseries.com, 10 Mio par ressource, 30 s au total. Rechercher la sous-chaîne `"apiHost":"https://api.betaseries.com"`, analyser le bloc local, sans exécution JavaScript ni regex sur le bundle entier. Découverte et décodage JSON tournent sur Dispatchers.Default. Configuration conservée 7 jours dans noBackupFilesDir ; refus explicite 1001 : invalidation, renouvellement unique pour GET, jamais replay d'une mutation.

L'association reçoit device_code, user_code, verification_url, expires_in et interval. L'échange POST oauth/access_token envoie **code**, dont la valeur est device_code. Code en grand avec le style de titre existant, adresse complète en dessous et même URL dans le QR ; aucun repli inventé vers le domaine. Une seule tentative, intervalle et expiration serveur respectés.

Le nouveau jeton est validé avant échange atomique de session, dans le scope du contrôleur. Stockage et révocation utilisent NonCancellable. AES-GCM et Android Keystore protègent le jeton hors sauvegarde. Seules AEADBadTagException et UnrecoverableKeyException autorisent la suppression lors d'une lecture ; les autres erreurs conservent le fichier et réessaient au prochain premier plan. Une révocation distante échouée est signalée séparément du compte.

### D3 — Réseau et données

Bornes normatives : « Bornes réseau et délais » de betaseries-integration. Connexion 5 s, lecture 10 s, tentative 15 s ; GET avec au plus deux reprises dans 60 s, compte et mutation avec relecture 90 s, identification locale 30 s. Pas de replay automatique POST/DELETE ; id passe en query pour DELETE. Retry-After et date courante utilisent des horloges injectées. Une 401/403 non JSON signifie accès refusé.

| Source | Champs et emploi | Limites |
| --- | --- | --- |
| Configuration web | apiHost, apiKey statiques | Client web non contractuel ; aucune valeur embarquée |
| members/infos | Identité et nom | Un seul compte, aucune donnée publiée |
| shows/member | Identifiants, images, user.archived, total ; pagination offset | Une archive absente rend uniquement cet élément incomplet ; totalMissingShows=0 ne prouve pas la fin |
| episodes/list | shows, unseen, dates, saison/épisode, compteurs distincts | released=1, limit=1, showsLimit=100 ; page documenté, décalage de plus de 100 séries à valider en 1.3 |
| movies/member | Films state=0, images, état, total ; pagination start | Aucun téléchargement state=1/2 ; totalMissingMovies=0 ne prouve pas la fin |
| shows/display, movies/movie, episodes/display | Fiche authentifiée, appartenance/statut | Champs absents : inconnu, jamais absence déduite |
| platforms/list?country= | Groupes de services et identifiants par pays | Ne prouve ni abonnement ni disponibilité exacte de chaque épisode |
| TV Provider | Identifiants externes, intents, reprise, images | Pas de preuve universelle de fin ; identifiant opaque sans contrat |

### D4 — Identité, projections et mémoire

Identité typée provider/type/id. Correspondance exacte par identifiants puis recherche sur le titre de série nettoyé, année et numéros, cinq candidats maximum, sans choisir un résultat ambigu. Cache par clé locale ; un rafraîchissement du compte ne réidentifie pas les contenus connus. Appartenance lue à la demande depuis la fiche ; catalogue et compte restent distincts.

Clé locale stable `<pkg>:<id>` ; l'identité de suivi est séparée. État de suivi, masquages et sources sont combinés avec les programmes déjà chargés en mémoire ; `TrackingHeroSource.changes()` relaie seulement les événements de sa source locale. Aucune requête TV Provider sur émission de suivi, même sans compte. Un héro enrichi conserve id et artwork.identity du programme remplacé. Aucun contenu BetaSeries n'entre au Top Shelf.

Fusion avant tri/limite de 20 : reprise non vue prioritaire, sinon prochain épisode diffusé ; aucune substitution de l'intent d'un autre épisode. Mémoire d'ouverture et masquages migrés sans purge sur absence non prouvée. Héro et À suivre partagent le lecteur sans créer depuis le héro une priorité d'ouverture dans À suivre.

### D5 — Lecteurs et offres

Premier choix obligatoire, même avec une seule app ; préférence persistée par série/film après lancement réussi. Apps vidéo reconnues puis Autre application. Intent publié de la cible exacte, sinon meilleur repli réellement testé, enfin ouverture de l'app annoncée ; aucun lien ou identifiant privé inventé.

Le contrôle public confirme que platforms/list varie par pays. Croiser les identifiants des services d'une offre avec cette liste pour le pays confirmé ; les autres restent non confirmés. Changer de pays relit uniquement cette liste et recombine les offres, sans recharger le compte. Ne pas transformer cette intersection en garantie d'épisode ou d'abonnement. Les champs d'offre sans consommateur sont supprimés.

### D6 — Actions et confirmation manuelle

Épisode isolé avec bulk=false ; films : 0 à voir, 1 vu, 2 refusé. Vu/non vu et archive relisent le résultat avant succès et conservent l'état antérieur pour Annuler. Retrait différé de 10 secondes actives ; focus et arrière-plan suspendent le reliquat, fin du processus ou déconnexion l'abandonnent. Une nouvelle action refusée explique la raison sans supprimer Annuler. Aucune file hors ligne persistée.

« Terminé ? » concerne le contenu demandé après un aller-retour effectif du lecteur. Aucun marquage sans accord. Proposition sans échéance jusqu'à validation, Retour, nouveau lancement suivi, déconnexion ou statut déjà vu. Pas d'écouteur de notifications, commande privilégiée, permission média ni adaptateur de preuve dans P9.

### D7 — Navigation et composants

Les parcours précis sont dans launcher-shell et watch-tracking : héro → Haut (engrenage) → Droite ; grille → Retour → Haut → Droite. Gauche/Haut/Retour rendent l'engrenage. Le statut dans le titre d'À suivre n'est jamais focusable. Épisodes de la saison courante chargés d'abord avec season explicite, sans pagination ; liste à plat et coche pour les vus. Autres saisons lit seulement les numéros, puis les épisodes de la saison validée. OK sur un épisode ouvre ses actions isolées.

Réutiliser ContextMenuPanel, Dimens, GlassSurface, tvFocus/tvClickable et Motion. Modales adaptées au contenu, sans grande réserve vide ; notification compacte bas droite dans la zone sûre. Pays en dropdown natif ancré au contrôle. Les décisions visuelles de la maquette restent acquises ; matières et fluidité se valident sur TV release.

## Risks / Trade-offs

- [Conditions BetaSeries] → La clé du client web n'est pas attribuée à SygixOs ; un blocage expose une configuration indisponible. Demander une clé officielle reste à trancher par Sygix.
- [Pages ou fiches incomplètes] → État explicite, conservation des autres entrées, aucune purge sur liste partielle.
- [Retrait destructif] → Envoi différé ; réaddition non présentée comme restauration de l'historique.
- [Évolution des specs] → Réconcilier les mêmes exigences avant chaque rebase.

## Migration Plan

1. Corriger le contrat OAuth et réduire les lectures avant nouvelle validation authentifiée.
2. Garder ouverts les contrôles de contrats, stockage et compatibilité TV jusqu'à preuve nominative.
3. Vérifier JUnit, Compose à taille TV, build release et OpenSpec ; conserver le chemin local sans compte.
4. Après merge de l'implémentation et validation TV, archiver p9-betaseries.

## Validations appareil restantes

Association/reconnexion, pagination authentifiée, mutations et voisins, cibles des lecteurs, chiffrement/redémarrage, pays, navigation, verre et fluidité : tâches 1.x, 3.5b et 8.2. Les fixtures et l'émulateur ne remplacent pas ces preuves.
