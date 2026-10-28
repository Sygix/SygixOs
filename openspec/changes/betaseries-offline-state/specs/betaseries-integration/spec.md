## MODIFIED Requirements

### Requirement: Chargement du compte BetaSeries
Le fournisseur SHALL charger et persister les films à voir et la réserve d'épisodes diffusés non vus des séries non archivées, sans limiter le compte aux 20 cartes affichées. Les identifiants externes, dates, images et liens réellement présents SHALL enrichir le modèle commun sans inventer les champs absents. L'accueil SHALL projeter un seul épisode candidat par série.

#### Scenario: chargement indépendant
- **WHEN** le compte charge pour la première fois
- **THEN** son état est distinct du chargement local ; les programmes des apps déjà prêts restent utilisables sans attendre BetaSeries ; un cache déjà lié au compte est restauré selon « État de suivi local durable »

#### Scenario: pagination complète
- **WHEN** le compte contient plus de contenus que la première page API
- **THEN** toutes les pages nécessaires sont chargées avec concurrence et délais bornés ; une page manquante ne transforme pas le résultat en liste complète et ne permet pas de purger une mémoire

#### Scenario: compte vide ou série à jour
- **WHEN** une lecture complète réussit sans film à voir ni épisode diffusé non vu
- **THEN** la contribution BetaSeries est vide sans erreur sous réserve des intentions locales encore en attente ; une série à jour ne crée pas de carte d'attente avant diffusion et l'épuisement du seul cache ne prouve pas cet état

#### Scenario: source refusée ou indisponible
- **WHEN** le jeton est refusé ou qu'un appel indispensable échoue ou dépasse son délai
- **THEN** un refus d'accès retire l'apport du compte et suspend ses envois ; une panne temporaire conserve le cache et les intentions avec état incomplet et réessai ; erreur et reconnexion restent distinctes d'un compte vide, sans retirer les programmes locaux

#### Scenario: rafraîchissement
- **WHEN** le launcher revient au premier plan
- **THEN** la lecture complète n'est relancée qu'à la connexion, si la dernière lecture réussie date de plus de 15 minutes, ou si la dernière lecture partielle ou en échec, hors refus d'accès, date de plus d'une minute ; une session déjà chargée n'est pas relue par la recréation de l'accueil avant cette échéance ; aucune lecture complète périodique ne tourne au repos ; les intentions et compléments en attente suivent « Synchronisation des intentions locales », y compris en arrière-plan ; aucune réponse d'une ancienne session ne remplace le compte courant

#### Scenario: mutation confirmée
- **WHEN** une mutation est confirmée sur un épisode, un film ou une série
- **THEN** seul le statut de ce contenu est relu et son état distant remplacé sans écraser une intention plus récente ; après un épisode vu, seule la réserve de sa série est recomplétée, y compris dans une autre saison ; aucune lecture complète, invalidation globale ou réidentification des autres contenus n'est déclenchée

#### Scenario: appartenance à la demande
- **WHEN** l'appartenance d'un contenu local identifié doit être établie
- **THEN** elle est lue depuis sa fiche authentifiée et mise en cache ; les films vus ou refusés ne sont pas téléchargés pour cela ; une appartenance déjà connue du compte peut servir hors ligne, sans assimiler une identité de catalogue à une appartenance

#### Scenario: élément ou page incomplets
- **WHEN** une fiche est incohérente ou qu'une page échoue, notamment par dépassement de corps
- **THEN** les autres contenus valides restent utilisables ; les entrées incohérentes restent individuellement incomplètes, une liste partielle n'est jamais déclarée complète et aucune purge ne se fonde sur ses absences ; l'indicateur reste visible mais l'enrichissement des contenus locaux continue avec le cache d'identités

#### Scenario: volume des épisodes
- **WHEN** les prochains épisodes sont chargés
- **THEN** la réserve suit « Réserve glissante d'épisodes » avec pagination complète et bornée ; la limite de cartes ne réduit ni le nombre de séries chargées ni leur réserve, et aucune liste illimitée de saisons n'est téléchargée par défaut

### Requirement: Fin de session BetaSeries
La déconnexion SHALL supprimer immédiatement le jeton et les données de compte locales et arrêter leurs travaux, y compris cache, réserve et intentions persistantes. La révocation distante SHALL viser uniquement le jeton de cette session et rester au mieux possible si le réseau manque ; le changement de compte SHALL ne jamais conserver une mutation différée de l'ancien compte. Une panne temporaire du stockage ou du service SHALL conserver la session et permettre sa restauration sans nouvelle association.

#### Scenario: déconnexion en ligne
- **WHEN** l'utilisateur se déconnecte
- **THEN** le jeton courant est révoqué si le service répond et le compte n'alimente plus l'accueil ; cache, réserve et intentions du compte sont supprimés ; les préférences de lecteurs et masquages de la TV restent conservés

#### Scenario: déconnexion hors ligne
- **WHEN** la révocation distante ne peut pas aboutir
- **THEN** l'effacement local et l'arrêt du suivi sont effectifs, l'échec distant est distingué et aucune reconnexion automatique du compte n'a lieu ; reconnecter ultérieurement ce compte ne rejoue pas les demandes abandonnées

#### Scenario: stockage indisponible
- **WHEN** le stockage protégé ne peut pas être lu ou déchiffré
- **THEN** le fichier est conservé et relu au prochain retour au premier plan ou sur Réessayer, sauf erreur définitive de déchiffrement ou clé Keystore confirmée absente ou définitivement invalidée ; une exception de récupération de clé dont la cause reste temporaire ou inconnue ne permet pas sa suppression ; aucune donnée secrète n'est exposée ni donnée de compte associée à un jeton par supposition

#### Scenario: identité indisponible au démarrage
- **WHEN** un jeton a été restauré mais que le chargement de l'identité échoue sans refus explicite du jeton
- **THEN** la session reste conservée ; Réessayer et la reprise au premier plan selon « Chargement du compte BetaSeries » réutilisent ce jeton, sans association OAuth ni révocation ; le cache déjà lié suit « État de suivi local durable » et une réussite charge ensuite les contenus du compte

#### Scenario: absence de session confirmée
- **WHEN** aucune session n'est enregistrée ou que son déchiffrement est définitivement impossible
- **THEN** la restauration se termine sans rester en chargement et une nouvelle association est proposée ; une panne temporaire ne suffit jamais à conclure à cette absence

#### Scenario: remplacement de session
- **WHEN** un nouveau jeton est reçu pendant une association
- **THEN** il est validé avant remplacement atomique de la session ; un jeton refusé à la validation est révoqué au mieux sans afficher « Révocation distante non confirmée » ; fermeture du panneau et annulation de l'appelant ne laissent ni session bloquée ni jeton orphelin ; stockage et révocation terminent leur séquence malgré cette annulation ; si l'identité confirmée change, les données de l'ancien compte sont purgées avant exposition du nouveau

#### Scenario: purge concurrente avec un envoi
- **WHEN** une déconnexion ou un changement de compte intervient pendant un travail de synchronisation
- **THEN** aucun nouvel envoi de l'ancienne session ne part après son invalidation et aucune réponse tardive ne recrée ses données ni ne modifie le cache du nouveau compte ; un envoi déjà parti peut avoir un effet distant sans être déclaré annulé

#### Scenario: purge locale interrompue
- **WHEN** l'effacement local échoue ou est interrompu
- **THEN** la session invalidée ne peut plus afficher ni envoyer les lignes résiduelles ; la purge est reprise après rétablissement du stockage sans restauration du compte déconnecté

### Requirement: Bornes réseau et délais
Les appels BetaSeries SHALL avoir des bornes fixes et annuler leurs ressources au dépassement. Les lectures idempotentes SHALL autoriser au plus deux reprises. Seuls les vu/non vu persistants SHALL reprendre automatiquement selon « Synchronisation des intentions locales » ; aucune autre mutation ne SHALL être rejouée automatiquement. Une expiration du compte SHALL exiger un refus explicite du jeton par le service, pas seulement un statut HTTP générique.

#### Scenario: budgets
- **WHEN** un appel réseau s'exécute
- **THEN** les bornes sont connexion 5 s, lecture 10 s, tentative 15 s, lecture avec reprises 60 s, corps API 2 Mio, compte et mutation avec relecture 90 s, identification locale 30 s ; 502/503/504, panne et non-JSON transitoire autorisent des reprises croissantes et Retry-After dans le budget restant, un délai serveur supérieur à ce reste échouant aussitôt ; un JSON invalide est une erreur de protocole

#### Scenario: refus HTTP sans preuve d'expiration
- **WHEN** une lecture reçoit 401/403 sans code BetaSeries établissant le refus du jeton, notamment avec un corps non JSON
- **THEN** une indisponibilité réessayable est exposée sans effacer le jeton, sans bloquer les prochains rafraîchissements et sans retirer les contenus précédemment valides ; un refus explicite de configuration conserve son renouvellement distinct

#### Scenario: jeton explicitement refusé
- **WHEN** le service retourne un code BetaSeries établissant le refus du jeton
- **THEN** la reconnexion est proposée, l'apport du compte retiré selon « Chargement du compte BetaSeries » et les envois persistants suspendus sans perte des intentions ; aucune boucle de renouvellement de configuration ni reprise d'une autre mutation n'est lancée

#### Scenario: révocation au mieux
- **WHEN** la déconnexion révoque le jeton
- **THEN** une seule tentative est faite dans l'échéance d'appel ; son échec affiche « Révocation distante non confirmée », sans échec du compte ni réessai ni blocage de l'effacement local

#### Scenario: synchronisation différée
- **WHEN** un vu/non vu attend une nouvelle tentative après échec
- **THEN** chaque tentative garde les budgets existants, les tentatives sont espacées et respectent Retry-After ; l'attente ne garde pas une requête ouverte, ne multiplie pas les envois concurrents et ne relance pas une lecture complète du compte

## ADDED Requirements

### Requirement: Réserve glissante d'épisodes
Chaque série non archivée SHALL conserver durablement le prochain épisode diffusé non vu et au moins cinq suivants diffusés non vus, ou tous ceux disponibles s'il en reste moins. Les intentions locales SHALL être appliquées avant sélection. La réserve SHALL traverser les saisons si nécessaire et être complétée en ligne après progression sans charger l'historique intégral.

#### Scenario: réserve constituée
- **WHEN** le chargement réussit avec au moins six épisodes diffusés non vus pour une série
- **THEN** le prochain et au moins cinq suivants sont enregistrés avec leur identité exacte, numéros et métadonnées disponibles ; seul le prochain contribue au héro et à À suivre

#### Scenario: passage de saison et fin disponible
- **WHEN** la fenêtre franchit une fin de saison ou que moins de six épisodes diffusés restent disponibles
- **THEN** les saisons suivantes contribuent dans l'ordre ; une fin disponible confirmée autorise une fenêtre plus courte et aucun épisode non diffusé ou de date inconnue n'est inventé comme suivant admissible

#### Scenario: progression hors ligne
- **WHEN** l'épisode courant est marqué vu sans réseau et qu'un suivant enregistré reste disponible
- **THEN** le prochain candidat effectif est choisi dans la réserve après application de toutes les intentions locales, même après redémarrage et sans appel distant

#### Scenario: réserve épuisée ou incomplète
- **WHEN** la réserve est épuisée hors ligne, ou que son complément charge ou échoue
- **THEN** les marquages restent conservés, le manque de suite est distingué d'une série confirmée à jour et aucun épisode n'est inventé ; les autres contenus restent utilisables et la reprise réseau peut recompléter la réserve

#### Scenario: compte absent ou accès refusé
- **WHEN** aucune session n'est disponible ou que son accès est refusé
- **THEN** aucune réserve d'un autre compte n'est utilisée et aucun complément non autorisé n'est lancé ; les états de session et de reconnexion existants s'appliquent

#### Scenario: retour au premier plan
- **WHEN** l'accueil revient avec une réserve existante
- **THEN** elle est utilisée immédiatement ; les besoins de complément rejoignent le travail partagé sans dupliquer le chargement complet ni les envois en arrière-plan

### Requirement: Nettoyage du suivi persistant
Les demandes confirmées et les données sorties de la réserve SHALL être nettoyées sans effacer les intentions plus récentes ni les données encore utiles à une annulation ou à une reprise locale connue. Le nettoyage SHALL conserver le cache nécessaire à l'accueil et ne SHALL pas constituer un historique illimité des contenus vus. Les films SHALL suivre ces règles sans réserve de suivants.

#### Scenario: épisode consommé confirmé
- **WHEN** un épisode vu est confirmé par BetaSeries et ne sert plus à une intention, une annulation ou une preuve de statut locale encore pertinente
- **THEN** sa demande est acquittée et son entrée consommée est supprimée ; la réserve glisse vers le prochain et ses cinq suivants disponibles, sans conserver les anciens épisodes inutilement

#### Scenario: protection avant confirmation
- **WHEN** un marquage reste en attente, qu'une réponse est incertaine ou qu'une annulation est encore disponible
- **THEN** les données nécessaires ne sont pas purgées ; une ancienne confirmation ne supprime jamais une nouvelle intention portant sur le même contenu

#### Scenario: film confirmé vu
- **WHEN** le statut vu d'un film est confirmé et qu'aucune intention, annulation ou preuve locale ne nécessite plus son entrée
- **THEN** la demande acquittée et l'entrée devenue inutile sont supprimées sans chargement de suivant ni retrait du film du compte distant

#### Scenario: preuve encore nécessaire et libération
- **WHEN** un ancien épisode ou film vu reste publié comme reprise locale, puis que cette référence et les autres protections disparaissent de façon fiable
- **THEN** le statut minimal empêche d'abord sa réapparition comme non vu ; l'entrée devenue inutile est ensuite purgée, sans dépendre d'une absence déduite d'une erreur ou d'une liste partielle

#### Scenario: liste incomplète et images partagées
- **WHEN** une lecture échoue, reste partielle ou que le nettoyage retire une entrée utilisant une image partagée
- **THEN** les absences non confirmées ne déclenchent pas de purge de compte ; le nettoyage ne supprime pas les octets encore utiles à un autre programme et le cache d'images ne recrée pas les entrées retirées
