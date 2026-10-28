## Purpose

Associer un compte Trakt sur la TV et l'utiliser dans les parcours communs de suivi, avec état durable, progression hors ligne et opérations adaptées aux objets du service.

## ADDED Requirements

### Requirement: Association TV Trakt
L'intégration SHALL associer volontairement le compte Trakt depuis les réglages par code et QR officiels, avec la configuration de `tracking-application-configuration`. Elle SHALL respecter les règles de présentation, navigation et fermeture de l'association BetaSeries sans réutiliser ses paramètres de protocole. Aucun mot de passe ni secret d'application ne SHALL être demandé ou embarqué.

#### Scenario: démarrage et connexion confirmée
- **WHEN** l'utilisateur lance Connecter avec Trakt sélectionné
- **THEN** la demande de code expose le chargement puis le code, l'adresse officielle complète et son QR cohérents avec une seule tentative ; la connexion n'est confirmée qu'après réception des jetons et validation de l'identité du compte

#### Scenario: attente et limitation
- **WHEN** l'autorisation est en attente ou que le serveur impose de ralentir les interrogations
- **THEN** l'intervalle serveur et l'expiration sont respectés ; une réponse en attente n'est pas une erreur de compte, aucune boucle rapide ni attente concurrente n'est lancée

#### Scenario: fermeture, expiration et retour
- **WHEN** l'utilisateur ferme l'association, refuse l'accès, laisse expirer le code ou revient au premier plan
- **THEN** fermeture et refus arrêtent l'attente et restaurent le contrôle d'origine ; l'expiration permet un nouveau code ; au retour l'échéance est vérifiée avant toute reprise et une ancienne tentative ne remplace jamais la session courante

#### Scenario: configuration absente ou erreur transitoire
- **WHEN** la découverte ou l'association échoue, reste vide, renvoie une réponse invalide ou est inaccessible
- **THEN** une erreur distincte et réessayable est affichée sans bloquer les sources locales ; les incidents transitoires d'interrogation suivent l'attente bornée existante, sans code inventé ni succès implicite

### Requirement: Session et renouvellement Trakt
La session Trakt SHALL conserver de façon protégée les jetons d'accès et de renouvellement, leur échéance et leur client d'origine, séparément du cache de configuration. Le renouvellement SHALL être mutualisé et déclenché à la demande, sans réveil périodique. Il SHALL conserver le lien au même compte et les intentions locales, sans création implicite d'un autre compte actif.

#### Scenario: jeton expiré au prochain appel
- **WHEN** un appel nécessite un jeton arrivé à échéance ou explicitement refusé et qu'un renouvellement est possible
- **THEN** un seul renouvellement est effectué pour les consommateurs concurrents ; les nouveaux jetons sont sauvegardés atomiquement avant réutilisation et un ancien résultat ne remplace pas une nouvelle session

#### Scenario: panne et résultat incertain du renouvellement
- **WHEN** le renouvellement subit une panne, une réponse perdue ou une erreur de stockage
- **THEN** session, compte lié et intentions restent conservés avec erreur réessayable ; un jeton de renouvellement à usage unique n'est pas réémis aveuglément et aucune réussite n'est annoncée sans sauvegarde ; un refus définitif confirmé propose Reconnecter

#### Scenario: client d'origine remplacé
- **WHEN** une nouvelle configuration d'application a été découverte
- **THEN** les anciens jetons ne lui sont pas réattribués ; la session conserve son client d'origine ; si elle ne peut plus être utilisée ou renouvelée avec ce client, Reconnecter est proposé sans perdre les intentions du même compte en l'absence de déconnexion explicite

#### Scenario: restauration, absence et stockage
- **WHEN** la TV démarre ou revient au premier plan avec une session restaurable, absente ou temporairement illisible
- **THEN** restauration du compte lié, distinction absence/panne, conservation sur erreur temporaire et réessai suivent « État de suivi local durable » et les scénarios de stockage et d'identité de « Fin de session BetaSeries » ; l'enveloppe Trakt ne transforme pas une panne en absence de compte

#### Scenario: déconnexion et changement de compte
- **WHEN** le compte Trakt est déconnecté, remplacé ou invalidé pendant un envoi ou un renouvellement
- **THEN** invalidation, purge, révocation au mieux et protection contre les retours tardifs suivent « Fin de session BetaSeries » pour les données du compte Trakt ; aucun renouvellement tardif ne recrée sa session et les préférences locales restent conservées

### Requirement: Contribution et réserve Trakt
Trakt SHALL fournir les films à voir et un seul prochain épisode diffusé non vu par série non archivée, avec la même projection que BetaSeries. Les règles de pagination, rafraîchissement ciblé, réserve glissante et nettoyage de `betaseries-integration`, complétées par l'état durable et la synchronisation de `watch-tracking`, SHALL s'appliquer sans reprendre les paramètres réseau propres à BetaSeries.

#### Scenario: chargement et pagination
- **WHEN** le compte charge depuis la watchlist et la progression Trakt
- **THEN** les sources locales et le cache déjà lié restent immédiatement utilisables ; toutes les pages nécessaires sont traitées indépendamment de la limite de cartes et l'état partiel ne prouve aucune absence ni fin de série

#### Scenario: films et séries admissibles
- **WHEN** des films non vus de la watchlist, des séries de la watchlist ou des séries commencées ont des contenus diffusés non vus
- **THEN** ces contenus alimentent les mêmes parcours de suivi que BetaSeries ; les séries masquées de la progression Trakt sont exclues de son apport, même si une autre liste les contient ; le masquage local conserve sa portée distincte

#### Scenario: compte vide et série à jour
- **WHEN** les lectures nécessaires réussissent complètement sans contenu à voir ou sans épisode diffusé non vu
- **THEN** l'apport concerné est vide sans erreur sous réserve des intentions locales ; une réserve épuisée ou une lecture incomplète ne permet pas d'annoncer une série à jour

#### Scenario: panne, source absente et refus
- **WHEN** un endpoint est indisponible, une page manque, le réseau échoue ou l'accès est refusé
- **THEN** les règles communes de qualification des refus distinguent panne et reconnexion ; les données durables du compte restent utilisables sur panne temporaire, aucun autre compte ne sert de repli et l'indicateur existant expose l'erreur sans bloquer les programmes locaux

#### Scenario: réserve et saisons à la demande
- **WHEN** une réserve doit être constituée ou complétée après progression
- **THEN** le contrat « Réserve glissante d'épisodes » est appliqué avec les statuts effectifs locaux ; seules les séries concernées sont complétées, sans télécharger les métadonnées de toutes leurs saisons ; le sélecteur conserve son chargement par saison explicite et une réserve ne prétend pas représenter une saison complète

#### Scenario: retour au premier plan et synchronisation
- **WHEN** l'accueil revient ou qu'une intention est confirmée en arrière-plan
- **THEN** les échéances de rafraîchissement et la relecture ciblée suivent le contrat BetaSeries ; la même projection durable met à jour héro et À suivre sans lecture du TV Provider ni redécouverte périodique du client d'application

### Requirement: Identités et métadonnées Trakt
Les identités Trakt SHALL être typées et distinctes des identités BetaSeries, avec correspondances externes seulement vérifiées. Les champs réellement fournis SHALL enrichir les modèles et caches existants. Une identité de catalogue ne SHALL pas prouver l'appartenance au compte ; les règles d'identité et d'offres de `watch-tracking` SHALL rester applicables.

#### Scenario: rapprochement et statut ciblés
- **WHEN** un contenu local doit être identifié ou son statut établi
- **THEN** la résolution non ambiguë et les caches existants sont utilisés avec le fournisseur actif ; le statut ou l'historique utile est lu pour ce contenu, sans télécharger l'historique de tous les films ni réidentifier les autres contenus

#### Scenario: images et champs manquants
- **WHEN** des identifiants, images, titres, dates ou durées sont disponibles, absents ou incohérents
- **THEN** seuls les champs vérifiés enrichissent le contenu ; les images passent par le cache partagé et les règles de qualité existantes, les absences conservent leurs replis et aucune date inconnue ne rend un épisode admissible par supposition

#### Scenario: offres et région
- **WHEN** les offres régionales chargent, réussissent sans résultat, échouent ou ne sont pas autorisées
- **THEN** le sélecteur de lecteur reste utilisable ; seules les offres réellement confirmées pour le pays sont présentées comme telles ; aucun nouveau service externe ni abonnement présumé ne comble l'absence

#### Scenario: session changée pendant un rapprochement
- **WHEN** une réponse concerne un ancien fournisseur ou une ancienne session
- **THEN** elle ne modifie ni appartenance, ni statut, ni clé de focus du contenu courant ; les états de chargement, vide, erreur et retour au premier plan restent ceux des contrats communs

### Requirement: Mutations natives Trakt
Les mutations Trakt SHALL respecter « Actions sur le compte » et « Annulation non bloquante » de `watch-tracking`. Ajout et retrait SHALL agir sur la watchlist ; l'archivage d'une série SHALL la masquer de la progression en conservant son historique. Les marquages SHALL viser seulement l'épisode ou le film identifié et ne SHALL pas provoquer de revisionnage implicite.

#### Scenario: ajout et retrait de watchlist
- **WHEN** un contenu est ajouté ou retiré du suivi Trakt
- **THEN** la watchlist est modifiée puis l'état applicable est vérifié ; pour une série commencée qui resterait dans la progression, Archiver remplace l'action Retirer ; aucune suppression de l'historique de série n'est envoyée

#### Scenario: archivage et annulation
- **WHEN** une série est archivée puis que l'action est annulée
- **THEN** seul son état de masquage de progression est modifié puis rétabli selon l'état antérieur ; ni ses épisodes vus, ni sa watchlist, ni son masquage local ne sont changés par supposition

#### Scenario: vu sans revisionnage
- **WHEN** un épisode ou film doit être marqué vu
- **THEN** après libération de sa fenêtre Annuler, l'état distant utile est vérifié ; un contenu déjà vu est confirmé sans ajouter de lecture et seul le contenu ciblé peut recevoir un nouveau marquage

#### Scenario: non vu et annulation avant envoi
- **WHEN** un épisode ou film est marqué non vu puis annulé pendant sa fenêtre
- **THEN** l'état effectif local précédent est restauré sans suppression de lecture distante ; après expiration sans annulation, seules les lectures du contenu ciblé peuvent être retirées pour le rendre non vu, jamais celles des épisodes précédents

#### Scenario: watchlist du film et résultat incertain
- **WHEN** un marquage de film modifie automatiquement sa présence dans la watchlist ou qu'une réponse est perdue
- **THEN** l'état souhaité du film et son appartenance utile sont réconciliés par lectures ciblées, sans revisionnage ni acquittement prématuré ; les compléments nécessaires restent liés à la même intention durable et une action incertaine n'est jamais répétée aveuglément

### Requirement: Réseau et refus Trakt
Les appels Trakt SHALL réutiliser les budgets de « Bornes réseau et délais » de `betaseries-integration` et la qualification commune des refus, avec au plus une mutation par seconde dans le client et respect des indications serveur. Les protocoles d'association et de renouvellement SHALL distinguer leur attente normale des erreurs de compte ou de configuration.

#### Scenario: limitation et concurrence
- **WHEN** des mutations concurrentes ou une réponse de limitation surviennent
- **THEN** leur départ est régulé et Retry-After est respecté ; les reprises restent espacées et bornées, sans requête gardée ouverte pendant l'attente ni multiplication des appels à l'accueil et au worker

#### Scenario: refus générique ou endpoint limité
- **WHEN** une réponse 401/403 ou non JSON ne prouve ni le refus du jeton ni l'invalidité du client
- **THEN** session et intentions sont conservées avec indisponibilité réessayable ; un endpoint non autorisé ne provoque ni déconnexion ni boucle de découverte ; seule une preuve propre au protocole active le traitement correspondant
