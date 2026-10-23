# Spec Delta

## MODIFIED Requirements

### Requirement: Configuration OAuth
L'authentification SHALL se faire par association OAuth BetaSeries pour appareil distant, depuis les réglages, avec code et QR vers la page officielle. La TV SHALL conserver le jeton utilisateur dans un stockage protégé ; aucun mot de passe ni identifiant d'application récupéré ne SHALL être publié ou embarqué comme valeur fixe.

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** l'association est volontaire, un seul compte est actif et les règles communes de `watch-tracking` s'appliquent

#### Scenario: association disponible
- **WHEN** l'utilisateur choisit de connecter BetaSeries
- **THEN** un code est demandé avec délai borné ; la TV affiche le code, l'adresse officielle lisible et son QR, puis attend l'autorisation selon l'intervalle et l'expiration renvoyés par le service

#### Scenario: connexion confirmée
- **WHEN** l'échange retourne un jeton utilisable et que l'identité du compte est confirmée
- **THEN** la TV passe à l'état connecté et charge le compte ; le simple affichage du code ou une réponse « en attente » n'est pas une connexion réussie

#### Scenario: attente et expiration
- **WHEN** l'autorisation reste en attente, le code expire ou l'utilisateur ferme l'association
- **THEN** aucun appel plus fréquent que l'intervalle serveur n'est émis ; à l'expiration les appels cessent et un nouveau code peut être demandé ; fermer arrête l'attente et restitue le focus au contrôle d'origine

#### Scenario: accès ou réponse en échec
- **WHEN** la configuration ou l'association est absente, refusée, en erreur réseau, limitée par le serveur ou renvoie une réponse non JSON
- **THEN** un état distinct et réessayable est affiché sans crash ni boucle rapide ; aucun secret n'apparaît dans l'erreur et le launcher reste utilisable

#### Scenario: reprise au premier plan
- **WHEN** la TV revient au premier plan sur l'association
- **THEN** son expiration est vérifiée avant reprise ; un code expiré n'est pas réutilisé et aucun second polling concurrent n'est créé

## ADDED Requirements

### Requirement: Configuration du client web récupérable
L'intégration SHALL récupérer automatiquement la configuration nécessaire depuis les ressources officielles du client web BetaSeries, avec cache protégé et renouvellement borné lorsqu'elle devient invalide. Un changement de ressource ou de format SHALL produire une erreur explicite, sans configuration de secours codée en dur ni évaluation de script distant.

#### Scenario: découverte
- **WHEN** une configuration est nécessaire et aucune configuration valide n'est disponible
- **THEN** les ressources officielles sont découvertes et analysées comme des données ; seules les valeurs correspondant au contrat testé sont utilisées

#### Scenario: rotation
- **WHEN** le service refuse explicitement la configuration d'application précédemment valide
- **THEN** une récupération mutualisée est tentée puis l'opération peut être rejouée une seule fois si elle est sans effet de bord ; un jeton utilisateur refusé déclenche la reconnexion, pas une boucle de récupération de clé

#### Scenario: ressource absente ou changée
- **WHEN** le site ne fournit plus les champs attendus, change de structure ou reste injoignable
- **THEN** la connexion expose son indisponibilité et son réessai ; les données locales et les préférences restent intactes

### Requirement: Chargement du compte BetaSeries
Le fournisseur SHALL charger les films à voir et le prochain épisode diffusé non vu des séries non archivées, sans limiter le compte aux 20 cartes affichées. Les identifiants externes, dates, images et liens réellement présents SHALL enrichir le modèle commun sans inventer les champs absents.

#### Scenario: chargement indépendant
- **WHEN** le compte charge pour la première fois
- **THEN** son état est distinct du chargement local ; les programmes des apps déjà prêts restent utilisables sans attendre BetaSeries

#### Scenario: pagination complète
- **WHEN** le compte contient plus de contenus que la première page API
- **THEN** toutes les pages nécessaires sont chargées avec concurrence et délais bornés ; une page manquante ne transforme pas le résultat en liste complète et ne permet pas de purger une mémoire

#### Scenario: compte vide ou série à jour
- **WHEN** une lecture complète réussit sans film à voir ni épisode diffusé non vu
- **THEN** la contribution BetaSeries est vide sans erreur ; une série à jour ne crée pas de carte d'attente avant diffusion

#### Scenario: source refusée ou indisponible
- **WHEN** le jeton est refusé ou qu'un appel indispensable échoue ou dépasse son délai
- **THEN** sa contribution n'est plus affichée dans À suivre ni comme candidat BetaSeries du héro ; l'état d'erreur ou de reconnexion reste distinct d'un compte vide, sans retirer les programmes locaux

#### Scenario: rafraîchissement
- **WHEN** le launcher revient au premier plan, qu'une mutation est confirmée ou qu'une relecture périodique est due
- **THEN** le compte est réconcilié par une lecture partagée ; aucune réponse d'une ancienne session ne remplace les données du compte courant

### Requirement: Contrats de mutation BetaSeries
Les opérations BetaSeries SHALL respecter « Actions sur le compte » et « Annulation non bloquante » de `watch-tracking`. Le marquage d'épisode SHALL explicitement désactiver le marquage en bloc des précédents ; l'intégration SHALL distinguer retirer, archiver et marquer non vu.

#### Scenario: épisode seul
- **WHEN** un épisode est marqué vu
- **THEN** l'appel cible cet identifiant avec le mode en bloc désactivé et le statut est relu ; les épisodes précédents restent inchangés

#### Scenario: film et annulation
- **WHEN** un film passe de « à voir » à « vu » puis que l'action est annulée
- **THEN** son état antérieur est rétabli sans retirer le film du compte ni créer implicitement un revisionnage

#### Scenario: retrait et archivage
- **WHEN** un contenu est retiré du compte ou une série archivée
- **THEN** les opérations distinctes sont utilisées ; aucun retrait distant n'est envoyé avant l'échéance d'annulation et aucune réaddition n'est présentée comme une restauration garantie de l'historique

### Requirement: Fin de session BetaSeries
La déconnexion SHALL supprimer immédiatement le jeton et les données de compte locales et arrêter leurs travaux. La révocation distante SHALL viser uniquement le jeton de cette session et rester au mieux possible si le réseau manque ; le changement de compte SHALL ne jamais conserver une mutation différée.

#### Scenario: déconnexion en ligne
- **WHEN** l'utilisateur se déconnecte
- **THEN** le jeton courant est révoqué si le service répond et le compte n'alimente plus l'accueil ; les préférences de lecteurs et masquages de la TV restent conservés

#### Scenario: déconnexion hors ligne
- **WHEN** la révocation distante ne peut pas aboutir
- **THEN** l'effacement local et l'arrêt du suivi sont effectifs, l'échec distant est distingué et aucune reconnexion automatique du compte n'a lieu

#### Scenario: stockage indisponible
- **WHEN** le stockage protégé ne peut pas être lu ou déchiffré
- **THEN** le launcher demande une nouvelle association sans exposer le jeton ni bloquer les sources locales
