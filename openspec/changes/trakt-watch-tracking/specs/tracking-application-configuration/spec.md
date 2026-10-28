## Purpose

Fournir aux intégrations de suivi une configuration d'application découverte sur leurs ressources officielles, conservée durablement et renouvelée uniquement après invalidation explicite, sans pénaliser l'interface TV.

## ADDED Requirements

### Requirement: Découverte officielle bornée
Chaque fournisseur SHALL découvrir sa configuration d'application depuis ses seules ressources web officielles, analysées comme des données sans exécution JavaScript ni moteur web. La découverte SHALL être hors du thread principal, mutualisée entre demandeurs du même fournisseur et bornée en volume et durée. Une valeur absente ou ambiguë SHALL produire une indisponibilité réessayable, sans clé fixe de secours.

#### Scenario: premier besoin
- **WHEN** une opération nécessite une configuration et qu'aucune configuration exploitable n'est enregistrée
- **THEN** une seule découverte est lancée ; les contrôles concernés exposent le chargement sans retarder le démarrage de l'accueil ni les sources locales déjà prêtes

#### Scenario: ressources et imports
- **WHEN** la page officielle référence des scripts ou que ces modules référencent des imports utiles
- **THEN** leurs adresses sont résolues depuis les ressources officielles réellement reçues, sans nom de bundle codé en dur ; seuls les fichiers nécessaires au contrat d'extraction vérifié sont lus, avec contrôle des domaines, redirections et doublons

#### Scenario: plafonds
- **WHEN** une découverte s'exécute pour BetaSeries ou Trakt
- **THEN** elle lit au plus une page officielle et six fichiers JavaScript, imports compris, chacun limité à 10 Mio, dans une échéance totale de 30 secondes ; lecture, analyse et décodage restent hors du thread principal et les ressources réseau sont annulées au dépassement

#### Scenario: absence, ambiguïté et erreur
- **WHEN** le site est absent, reste inaccessible, change de format, ne fournit aucune configuration unique ou dépasse une borne
- **THEN** la découverte échoue explicitement avec réessai ; elle ne charge pas un navigateur, n'exécute pas de script, ne télécharge pas tout le site et ne déduit pas une valeur d'une chaîne ressemblant seulement à un identifiant

### Requirement: Cache de configuration sans échéance
Une configuration exploitable SHALL être conservée sur la TV, isolée par fournisseur et hors sauvegarde transférable, puis réutilisée après redémarrage sans expiration périodique. Le cache de configuration SHALL rester distinct de la session utilisateur et des données du compte. Seul un rejet explicite de cette configuration SHALL déclencher sa redécouverte automatique.

#### Scenario: cache ancien et retour au premier plan
- **WHEN** une configuration existe depuis plus de sept jours, que le processus redémarre ou que l'accueil revient au premier plan
- **THEN** elle est réutilisée sans lire le site ; ni son âge, ni le rafraîchissement du compte, ni une synchronisation hors ligne ne lancent de découverte

#### Scenario: mise à jour BetaSeries
- **WHEN** une configuration BetaSeries écrite par une version utilisant une date d'expiration est encore exploitable
- **THEN** elle est conservée indépendamment de sa date de récupération ; la mise à jour ne force ni découverte ni nouvelle association

#### Scenario: cache absent ou illisible
- **WHEN** le cache ne contient aucune configuration exploitable ou subit une erreur de lecture
- **THEN** absence et panne sont distinguées ; une panne de stockage ne supprime pas la session utilisateur, et les données locales du compte restent régies par leur contrat de persistance

#### Scenario: indépendance des fournisseurs
- **WHEN** l'utilisateur choisit un fournisseur ou se déconnecte d'un compte
- **THEN** aucune configuration d'un autre fournisseur ne sert de repli ; la configuration d'application n'est ni un compte connecté ni une autorisation d'envoyer les demandes de l'ancien compte

### Requirement: Récupération après rejet de configuration
Un rejet explicite de configuration SHALL autoriser une récupération mutualisée, bornée et distincte du renouvellement de session. Une opération sans effet de bord SHALL être rejouée au plus une fois avec une configuration renouvelée. Un second rejet ou une configuration toujours identique et refusée SHALL terminer en erreur, sans boucle ni réémission aveugle de mutation.

#### Scenario: refus explicite et appels concurrents
- **WHEN** plusieurs opérations reçoivent une preuve que leur configuration d'application est invalide
- **THEN** une seule récupération traite cette configuration ; une réponse tardive visant l'ancienne ne peut invalider une configuration plus récente et chaque opération garde son budget global

#### Scenario: configuration renouvelée ou toujours refusée
- **WHEN** une nouvelle découverte fournit une configuration ou échoue à en fournir une utilisable
- **THEN** une lecture peut être retentée une seule fois ; sinon l'erreur de configuration et le réessai sont exposés sans repartir immédiatement dans une nouvelle découverte

#### Scenario: mutation potentiellement envoyée
- **WHEN** un rejet ou incident touche une mutation dont l'effet distant n'est pas établi
- **THEN** le contrat de réconciliation du suivi s'applique avant toute répétition ; le renouvellement de configuration n'autorise pas un second envoi aveugle

### Requirement: Qualification commune des refus
Les intégrations SHALL partager une politique distinguant configuration invalide, refus de jeton utilisateur, endpoint non autorisé, limitation et panne transitoire. Chaque fournisseur SHALL apporter les preuves propres à son protocole ; un statut HTTP générique ou un corps non JSON ne SHALL pas suffire à effacer une session ou à redécouvrir son client d'application.

#### Scenario: configuration explicitement refusée
- **WHEN** une réponse contient une preuve reconnue d'invalidité de configuration, notamment le code BetaSeries 1001
- **THEN** seule la récupération de configuration correspondante est engagée ; les demandes durables et les jetons utilisateur ne sont pas supprimés par ce seul fait

#### Scenario: jeton expiré ou refusé
- **WHEN** le protocole confirme le refus du jeton utilisateur
- **THEN** le fournisseur applique son renouvellement ou sa reconnexion sans redécouverte du client par supposition ; les intentions restent conservées selon le contrat commun tant qu'aucune déconnexion explicite ne les purge

#### Scenario: refus ambigu, accès limité ou panne
- **WHEN** une réponse générique 401/403, une protection HTTP, un endpoint limité ou une panne ne permet pas de conclure
- **THEN** session et données liées sont conservées avec état réessayable ; aucune association forcée, invalidation de configuration ou boucle rapide n'est déclenchée et les reprises gardent les bornes de chaque fournisseur
