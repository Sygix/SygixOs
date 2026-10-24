# Spec Delta

## MODIFIED Requirements

### Requirement: Configuration OAuth
L'authentification SHALL se faire par association OAuth BetaSeries pour appareil distant, depuis les réglages, avec code et QR vers la page officielle. La TV SHALL conserver le jeton utilisateur dans un stockage protégé ; aucun mot de passe ni identifiant d'application récupéré ne SHALL être publié ou embarqué comme valeur fixe.

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** l'association est volontaire, un seul compte est actif et les règles communes de `watch-tracking` s'appliquent

#### Scenario: association disponible
- **WHEN** l'utilisateur choisit de connecter BetaSeries
- **THEN** un code est demandé selon « Bornes réseau et délais » ; la TV affiche le code en grand avec le style de titre existant, puis la `verification_url` complète en dessous ; le QR encode cette même URL ; l'attente respecte l'intervalle et l'expiration serveur ; l'échange envoie le code appareil dans le paramètre `code`

#### Scenario: adresse de saisie du code
- **WHEN** l'association fournit une adresse comme `https://www.betaseries.com/device`
- **THEN** cette adresse précise est affichée et encodée sans remplacer son chemin ni inventer de paramètre prérempli ; si l'adresse manque ou est invalide, le parcours présente l'erreur réessayable plutôt qu'un lien de secours vers le domaine

#### Scenario: connexion confirmée
- **WHEN** l'échange retourne un jeton utilisable et que l'identité du compte est confirmée
- **THEN** la TV passe à l'état connecté et charge le compte ; le simple affichage du code ou une réponse « en attente » n'est pas une connexion réussie

#### Scenario: attente et expiration
- **WHEN** l'autorisation reste en attente, le code expire ou l'utilisateur ferme l'association
- **THEN** aucun appel plus fréquent que l'intervalle serveur n'est émis ; à l'expiration les appels cessent et un nouveau code peut être demandé ; fermer arrête l'attente et restitue le focus au contrôle d'origine

#### Scenario: accès ou réponse en échec
- **WHEN** la configuration ou l'association est absente, refusée, en erreur réseau, limitée par le serveur ou renvoie une réponse non JSON
- **THEN** un état distinct et réessayable est affiché sans crash ni boucle rapide ; aucun secret n'apparaît dans l'erreur et le launcher reste utilisable

#### Scenario: réponse transitoire pendant l'attente
- **WHEN** une interrogation de l'attente reçoit une réponse non JSON ou subit une coupure réseau ponctuelle
- **THEN** l'état reste en attente et l'interrogation continue à l'intervalle serveur jusqu'à l'expiration ; seul un refus définitif termine l'association en erreur

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

#### Scenario: conservation locale
- **WHEN** une configuration valide a été obtenue
- **THEN** elle est conservée hors sauvegarde transférable et réutilisée au démarrage pendant 7 jours ; l'appel suivant renouvelle une configuration expirée ; un refus explicite l'invalide immédiatement

#### Scenario: bornes de la découverte
- **WHEN** la découverte s'exécute
- **THEN** elle lit au plus la page officielle et six scripts de www.betaseries.com, chacun limité à 10 Mio, dans un délai total de 30 secondes ; découverte et décodage s'effectuent hors du thread principal ; tout dépassement expose une indisponibilité réessayable

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
- **THEN** un refus d'accès retire l'apport du compte ; les autres échecs conservent les entrées valides indépendantes avec état incomplet et réessai ; erreur et reconnexion restent distinctes d'un compte vide, sans retirer les programmes locaux

#### Scenario: rafraîchissement
- **WHEN** le launcher revient au premier plan
- **THEN** la lecture complète n'est relancée qu'à la connexion, si la dernière lecture réussie date de plus de 15 minutes, ou si la dernière lecture partielle ou en échec, hors refus d'accès, date de plus d'une minute ; une session déjà chargée n'est pas relue par la recréation de l'accueil avant cette échéance ; aucune boucle de synchronisation ne tourne au repos ; aucune réponse d'une ancienne session ne remplace le compte courant

#### Scenario: mutation confirmée
- **WHEN** une mutation est confirmée sur un épisode, un film ou une série
- **THEN** seul le statut de ce contenu est relu et son entrée remplacée dans la liste du compte ; après un épisode vu, le prochain candidat de sa seule série peut être demandé séparément, y compris dans une autre saison ; aucune lecture complète, invalidation globale ou réidentification des autres contenus n'est déclenchée

#### Scenario: appartenance à la demande
- **WHEN** l'appartenance d'un contenu local identifié doit être établie
- **THEN** elle est lue depuis sa fiche authentifiée et mise en cache ; les films vus ou refusés ne sont pas téléchargés pour cela

#### Scenario: élément ou page incomplets
- **WHEN** une fiche est incohérente ou qu'une page échoue, notamment par dépassement de corps
- **THEN** les autres contenus valides restent utilisables ; les entrées incohérentes restent individuellement incomplètes, une liste partielle n'est jamais déclarée complète et aucune purge ne se fonde sur ses absences ; l'indicateur reste visible mais l'enrichissement des contenus locaux continue avec le cache d'identités

#### Scenario: volume des épisodes
- **WHEN** les prochains épisodes sont chargés
- **THEN** un seul épisode diffusé non vu par série est demandé (`limit=1`, `showsLimit=100`, `released=1`) ; la pagination conserve toute limite explicite

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
- **THEN** le fichier est conservé et relu au prochain retour au premier plan, sauf erreur définitive de déchiffrement ou clé Keystore absente ; seuls ces cas permettent sa suppression et une nouvelle association ; aucune donnée secrète n'est exposée

#### Scenario: remplacement de session
- **WHEN** un nouveau jeton est reçu pendant une association
- **THEN** il est validé avant remplacement atomique de la session ; un jeton refusé à la validation est révoqué au mieux sans afficher « Révocation distante non confirmée » ; fermeture du panneau et annulation de l'appelant ne laissent ni session bloquée ni jeton orphelin ; stockage et révocation terminent leur séquence malgré cette annulation

### Requirement: Bornes réseau et délais
Les appels BetaSeries SHALL avoir des bornes fixes et annuler leurs ressources au dépassement. Les lectures idempotentes SHALL autoriser au plus deux reprises ; aucune mutation ne SHALL être rejouée automatiquement.

#### Scenario: budgets
- **WHEN** un appel réseau s'exécute
- **THEN** les bornes sont connexion 5 s, lecture 10 s, tentative 15 s, lecture avec reprises 60 s, corps API 2 Mio, compte et mutation avec relecture 90 s, identification locale 30 s ; 502/503/504, panne et non-JSON transitoire autorisent des reprises croissantes et Retry-After dans le budget restant, un délai serveur supérieur à ce reste échouant aussitôt ; 401/403 même non JSON expose l'accès refusé ; un JSON invalide est une erreur de protocole

#### Scenario: révocation au mieux
- **WHEN** la déconnexion révoque le jeton
- **THEN** une seule tentative est faite dans l'échéance d'appel ; son échec affiche « Révocation distante non confirmée », sans échec du compte ni réessai ni blocage de l'effacement local
