## MODIFIED Requirements

### Requirement: Actions sur le compte
Un contenu identifié SHALL proposer les actions applicables : ajouter au suivi, vu/non vu, retirer et, pour une série, archiver. Vu/non vu SHALL être enregistré durablement et appliqué localement avant confirmation distante pour épisodes et films. Les autres actions SHALL conserver leur confirmation distante. Marquer un épisode SHALL ne modifier que celui-ci ; le masquage local SHALL rester distinct.

#### Scenario: épisode isolé
- **WHEN** S02E05 est marqué vu et S02E03/S02E04 restent non vus
- **THEN** seul S02E05 change de statut ; aucun marquage implicite des précédents n'a lieu

#### Scenario: ajout depuis une source locale
- **WHEN** un compte est connecté et qu'une série ou un film existant est identifié sans ambiguïté et confirmé absent de ce compte
- **THEN** l'ajout est proposé dans son menu local sans exiger une appartenance préalable ; aucune recherche générale de catalogue n'est requise et une identité ou appartenance incertaine ne permet pas l'envoi ; seule une appartenance confirmée après succès active le parcours de lecteur commun

#### Scenario: archivage
- **WHEN** une série est archivée avec succès
- **THEN** elle quitte l'apport du fournisseur ; une reprise locale peut continuer à l'afficher et le masquage local n'est pas modifié

#### Scenario: opération en cours ou refusée
- **WHEN** une mutation charge, échoue, dépasse son délai ou perd son autorisation
- **THEN** l'action expose son état et empêche un double envoi ; pour vu/non vu durablement enregistré, le changement local reste appliqué avec attente de synchronisation distincte du succès distant ; pour les autres mutations, le contenu garde son état précédent en échec ; réessai ou reconnexion reste accessible et fermer la notice ne supprime pas la demande persistante

#### Scenario: résultat réseau incertain
- **WHEN** une réponse est perdue après l'envoi d'une mutation
- **THEN** le statut distant est relu avant toute répétition ; seuls les vu/non vu persistants peuvent reprendre automatiquement selon « Synchronisation des intentions locales » ; aucune autre mutation n'entre dans une file hors ligne

#### Scenario: sauvegarde locale impossible
- **WHEN** l'écriture durable d'un vu/non vu échoue
- **THEN** aucun changement n'est annoncé comme enregistré, l'état précédent est conservé et une erreur de stockage réessayable est exposée sans envoi non journalisé

#### Scenario: film hors ligne
- **WHEN** un film du compte connu localement est marqué vu ou non vu sans réseau
- **THEN** sa contribution à regarder est immédiatement recalculée après sauvegarde durable, commune au héro et à À suivre ; le film et son état nécessaire restent disponibles jusqu'à acquittement, sans calcul d'épisode suivant

### Requirement: Annulation non bloquante
Vu/non vu SHALL proposer une annulation dès sa sauvegarde locale ; l'archivage SHALL proposer l'annulation de l'action confirmée. Le retrait SHALL rester visuellement immédiat avec envoi différé pendant Annuler. L'annulation d'un vu/non vu SHALL restaurer localement l'état précédent et synchroniser cet état si nécessaire, sans annoncer un succès distant non confirmé.

#### Scenario: annuler un marquage
- **WHEN** l'utilisateur annule un marquage enregistré localement ou confirmé
- **THEN** l'état précédent de l'épisode ou du film est rétabli localement ; sans envoi parti la demande annulée n'est pas envoyée, sinon l'état antérieur devient l'intention à synchroniser ; l'annulation reste accessible même si la carte passe à l'épisode suivant ou disparaît

#### Scenario: retrait encore annulable
- **WHEN** l'utilisateur annule avant l'échéance d'un retrait du compte
- **THEN** la carte retrouve sa place de tri et aucun retrait distant n'a été envoyé

#### Scenario: durée de la fenêtre
- **WHEN** une annulation devient disponible
- **THEN** sa fenêtre dure 10 secondes actives ; focus, arrière-plan et réglages ouverts suspendent le décompte, repris avec le temps restant ; aucun retrait distant ne part avant cette échéance ; une nouvelle action refusée conserve Annuler et explique le refus ; la fin du processus abandonne le retrait mais pas les vu/non vu persistants

#### Scenario: annuler un archivage
- **WHEN** l'utilisateur annule un archivage confirmé
- **THEN** l'état d'archive précédent est rétabli puis relu, sans retrait de la série ni modification de ses épisodes vus ou de son masquage local

#### Scenario: échéance puis échec
- **WHEN** la fenêtre d'annulation se termine puis que le retrait échoue
- **THEN** le contenu est réconcilié avec le compte et l'échec est visible avec réessai ; aucune restauration d'historique non garantie n'est promise après suppression distante

#### Scenario: changement de compte ou interruption
- **WHEN** une action différée n'est pas encore envoyée et que le compte est déconnecté ou le processus interrompu
- **THEN** la déconnexion abandonne et supprime toutes les demandes du compte ; l'interruption seule conserve les vu/non vu persistants mais abandonne le retrait différé ; une requête déjà partie ne peut pas être déclarée annulée sans vérifier son résultat

#### Scenario: interaction avec le retour
- **WHEN** une proposition ou une annulation est présentée
- **THEN** elle n'interrompt pas la navigation, ne prend pas spontanément le focus et reste accessible au D-pad pendant sa fenêtre ; son échéance n'exécute jamais un marquage vu à la place de l'utilisateur

#### Scenario: annulation d'un film
- **WHEN** l'utilisateur annule le marquage d'un film
- **THEN** l'état de film antérieur exact est rétabli sans retrait du compte ni revisionnage implicite ; ses métadonnées utiles ne sont pas purgées pendant la fenêtre d'annulation

## ADDED Requirements

### Requirement: État de suivi local durable
Le suivi SHALL conserver le dernier état connu du compte et ses intentions locales par fournisseur et identifiant stable de compte. L'état effectif SHALL appliquer les intentions en attente au dernier état distant connu et alimenter conjointement héro et À suivre. Un redémarrage sans réseau SHALL restaurer cet état d'une session déjà liée, sans attendre la lecture distante de l'identité.

#### Scenario: restauration et rafraîchissement
- **WHEN** une session précédemment liée et son cache sont restaurés, notamment au retour au premier plan
- **THEN** les contenus locaux du compte et les intentions apparaissent sans attendre BetaSeries ; chargement et ancienneté restent distincts de la confirmation distante et les rafraîchissements respectent le contrat du fournisseur

#### Scenario: premier chargement sans cache
- **WHEN** aucune donnée de compte n'a encore été enregistrée et que le réseau manque
- **THEN** une indisponibilité réessayable est exposée, sans compte inventé ni blocage des programmes locaux

#### Scenario: cache vide et source absente
- **WHEN** le cache du compte est vide ou qu'aucune session liée n'est disponible
- **THEN** un compte connu vide reste distinct d'une absence de session ; aucune donnée d'un autre compte ne sert de repli

#### Scenario: erreur de stockage ou accès refusé
- **WHEN** le cache est illisible ou que l'autorisation du compte est refusée
- **THEN** l'erreur de stockage ou la reconnexion est exposée distinctement ; aucune intention n'est envoyée sous une session non vérifiée et les programmes locaux gardent leurs règles

#### Scenario: rafraîchissement concurrent
- **WHEN** une lecture distante arrive après un marquage local ou une confirmation plus récente
- **THEN** elle ne rétablit pas l'ancien statut effectif, ne supprime pas l'intention en attente et ne remplace pas une confirmation plus récente

### Requirement: Synchronisation des intentions locales
Les vu/non vu durables d'épisodes et de films SHALL être synchronisés automatiquement, y compris en arrière-plan selon les possibilités d'Android. La dernière intention locale en attente SHALL primer sur un état distant différent. Seule une confirmation de son état cible SHALL acquitter une demande ; une réponse d'une ancienne demande ou session ne SHALL pas acquitter la demande courante.

#### Scenario: reprise après panne ou redémarrage
- **WHEN** une demande reste en attente et que réseau et session autorisent le travail
- **THEN** une tentative bornée est planifiée même sans accueil visible, y compris après redémarrage du processus ou de la TV ; les restrictions Android peuvent différer l'exécution, sans perte de demande ni promesse d'envoi instantané

#### Scenario: résultat déjà appliqué
- **WHEN** une demande potentiellement envoyée est reprise et que la lecture distante confirme déjà sa cible
- **THEN** la demande correspondante est acquittée sans nouvelle mutation ; une lecture en échec maintient l'incertitude sans renvoi aveugle

#### Scenario: intention prioritaire et nouvelle révision
- **WHEN** le statut distant diffère ou qu'un nouveau vu/non vu remplace une demande pendant son envoi
- **THEN** l'état désiré local courant reste projeté ; l'ancien résultat ne supprime pas la nouvelle demande et la convergence vise la dernière intention explicite de la TV

#### Scenario: refus et erreurs temporaires
- **WHEN** le réseau échoue, le serveur limite les appels ou l'accès au compte est refusé
- **THEN** les erreurs temporaires gardent les intentions avec réessais espacés respectant le serveur ; le refus d'accès suspend les envois et expose la reconnexion ; aucun autre compte n'est utilisé

#### Scenario: absence de travail et concurrence
- **WHEN** la file est vide ou que plusieurs déclencheurs demandent sa synchronisation
- **THEN** aucun polling réseau ne reste actif sans travail utile et une même demande n'est pas envoyée concurremment ; le réessai manuel et le retour au premier plan réutilisent le même suivi des confirmations

#### Scenario: confirmation sans réserve complète
- **WHEN** le marquage est confirmé mais que le complément d'épisodes échoue
- **THEN** le marquage est acquitté sans être renvoyé ; la réserve reste explicitement incomplète et son complément peut être réessayé séparément
