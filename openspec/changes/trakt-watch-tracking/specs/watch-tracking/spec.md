## MODIFIED Requirements

### Requirement: Fournisseur de suivi actif
Le launcher SHALL utiliser un seul compte de suivi actif, distinct des applications de lecture, parmi BetaSeries et Trakt. Les comportements communs SHALL dépendre d'un contrat extensible ; les protocoles et les actions applicables SHALL suivre le fournisseur choisi. Changer de fournisseur SHALL nécessiter une déconnexion explicite de la session courante.

#### Scenario: sans compte
- **WHEN** aucun compte n'est connecté
- **THEN** le launcher conserve les sources et parcours locaux ; aucune association n'est imposée au démarrage

#### Scenario: compte connecté
- **WHEN** la connexion est confirmée
- **THEN** le fournisseur alimente le suivi partagé du héro et d'À suivre, sans charger séparément le compte pour chaque zone

#### Scenario: identité et appartenance distinctes
- **WHEN** un contenu local a une identité résolue dans le catalogue du fournisseur
- **THEN** cela ne prouve pas son appartenance au compte actif ; « contenu suivi » désigne un contenu identifié appartenant au compte, seul cas activant le parcours commun de lecteur et sa préférence persistante ; un contenu identifié absent du compte peut proposer l'ajout en conservant son ouverture locale

#### Scenario: changement de compte
- **WHEN** l'utilisateur se déconnecte puis associe un autre compte
- **THEN** les données, mutations et preuves du premier compte ne sont jamais réutilisées pour le second ; préférences de lecteur et masquages locaux restent conservés sur la TV

#### Scenario: mise à jour avec BetaSeries connecté
- **WHEN** une installation existante possède une session BetaSeries et des intentions durables
- **THEN** la mise à jour conserve le fournisseur, la session et les intentions liées sans exiger de nouvelle association ni les réattribuer à Trakt

### Requirement: Actions sur le compte
Un contenu identifié SHALL proposer les actions applicables au fournisseur : ajouter au suivi, vu/non vu, retirer et archiver une série. Vu/non vu SHALL être durable et immédiatement projeté localement, avec envoi après Annuler. Les autres actions SHALL garder leur confirmation distante. Un marquage SHALL ne modifier que le contenu ciblé ; masquage local et actions de compte SHALL rester distincts.

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
- **THEN** le statut distant est relu avant toute répétition ; seuls les vu/non vu persistants libérés après Annuler peuvent reprendre automatiquement selon « Synchronisation des intentions locales » ; aucune autre mutation n'entre dans une file hors ligne

#### Scenario: sauvegarde locale impossible
- **WHEN** l'écriture durable d'un vu/non vu échoue
- **THEN** aucun changement n'est annoncé comme enregistré, l'état précédent est conservé et une erreur de stockage réessayable est exposée sans envoi non journalisé

#### Scenario: film hors ligne
- **WHEN** un film du compte connu localement est marqué vu ou non vu sans réseau
- **THEN** sa contribution à regarder est immédiatement recalculée après sauvegarde durable, commune au héro et à À suivre ; le film et son état nécessaire restent disponibles jusqu'à acquittement, sans calcul d'épisode suivant

#### Scenario: action manuelle ou confirmation de retour
- **WHEN** vu/non vu provient du menu d'un contenu, de la liste d'épisodes ou d'une confirmation explicite « Terminé ? »
- **THEN** la même fenêtre Annuler existante et la même libération durable avant envoi s'appliquent pour BetaSeries et Trakt ; aucun nouveau marquage automatique ni nouveau panneau n'est introduit

### Requirement: Annulation non bloquante
Vu/non vu SHALL proposer Annuler dès sa sauvegarde locale et rester non envoyable jusqu'à la fin de sa fenêtre. L'archivage SHALL garder l'annulation de l'action confirmée et le retrait son envoi différé existant. Annuler un marquage encore retenu SHALL restaurer l'état local précédent sans envoyer cette action. Le temps restant du marquage SHALL survivre à l'interruption du processus.

#### Scenario: annuler un marquage
- **WHEN** l'utilisateur annule un marquage pendant sa fenêtre
- **THEN** l'état effectif antérieur de l'épisode ou du film est rétabli localement, sans envoyer le marquage annulé ni une opération inverse pour ce marquage ; une intention antérieure encore pertinente n'est pas perdue et l'annulation reste accessible même si la carte passe à l'épisode suivant ou disparaît

#### Scenario: retrait encore annulable
- **WHEN** l'utilisateur annule avant l'échéance d'un retrait du compte
- **THEN** la carte retrouve sa place de tri et aucun retrait distant n'a été envoyé

#### Scenario: durée de la fenêtre
- **WHEN** une annulation devient disponible
- **THEN** sa fenêtre dure 10 secondes actives ; focus, arrière-plan et réglages ouverts suspendent le décompte, repris avec le temps restant ; aucun retrait distant ni marquage vu/non vu ne part avant cette échéance ; une nouvelle action refusée conserve Annuler et explique le refus ; la fin du processus abandonne le retrait mais pas les vu/non vu persistants ni leur temps restant

#### Scenario: annuler un archivage
- **WHEN** l'utilisateur annule un archivage confirmé
- **THEN** l'état d'archive précédent est rétabli puis relu, sans retrait de la série ni modification de ses épisodes vus ou de son masquage local

#### Scenario: échéance puis échec
- **WHEN** la fenêtre d'annulation se termine puis que le retrait échoue
- **THEN** le contenu est réconcilié avec le compte et l'échec est visible avec réessai ; aucune restauration d'historique non garantie n'est promise après suppression distante

#### Scenario: changement de compte ou interruption
- **WHEN** une action différée n'est pas encore envoyée et que le compte est déconnecté ou le processus interrompu
- **THEN** la déconnexion abandonne et supprime toutes les demandes du compte ; l'interruption seule conserve les vu/non vu persistants et leur état d'annulation mais abandonne le retrait différé ; une requête déjà partie ne peut pas être déclarée annulée sans vérifier son résultat

#### Scenario: interaction avec le retour
- **WHEN** une proposition ou une annulation est présentée
- **THEN** elle n'interrompt pas la navigation, ne prend pas spontanément le focus et reste accessible au D-pad pendant sa fenêtre ; son échéance n'exécute jamais un marquage vu à la place de l'utilisateur

#### Scenario: annulation d'un film
- **WHEN** l'utilisateur annule le marquage d'un film
- **THEN** l'état de film antérieur exact est rétabli sans retrait du compte ni revisionnage implicite ; ses métadonnées utiles ne sont pas purgées pendant la fenêtre d'annulation

#### Scenario: reprise après interruption
- **WHEN** le processus ou la TV redémarre avec un marquage retenu par Annuler
- **THEN** le marquage reste projeté, son temps restant est restauré et la notice Annuler réapparaît au retour de l'accueil sans prendre le focus ; le décompte ne reprend que dans les conditions actives existantes et aucun travail en arrière-plan ne peut contourner cette retenue

#### Scenario: arrêt brutal sans dernier état exact
- **WHEN** Android interrompt le processus avant la dernière sauvegarde du décompte
- **THEN** le dernier temps restant durable est repris ; une prolongation conservatrice est admise, jamais une réduction par supposition ; les écritures sont hors du thread UI et ne suivent pas les frames de rendu

#### Scenario: migration des demandes antérieures
- **WHEN** une demande créée par une version antérieure est migrée
- **THEN** son état de synchronisation et son éventuel envoi déjà parti sont conservés sans nouvelle fenêtre Annuler rétroactive ; la retenue avant envoi s'applique aux nouveaux marquages

#### Scenario: libération durable et course avec Annuler
- **WHEN** la fenêtre se termine ou qu'Annuler intervient en concurrence avec son échéance
- **THEN** la libération et l'annulation sont ordonnées par une seule décision durable ; seul le marquage libéré peut être synchronisé et aucun déclencheur manuel, au premier plan ou en arrière-plan ne peut envoyer une révision encore annulable

### Requirement: Synchronisation des intentions locales
Les vu/non vu durables d'épisodes et films libérés après Annuler SHALL être synchronisés automatiquement, y compris en arrière-plan selon Android. La dernière intention locale en attente SHALL primer sur l'état distant. Seule une confirmation de sa cible SHALL l'acquitter ; un ancien résultat ne SHALL pas acquitter la demande courante. Une intention encore annulable SHALL rester retenue pour tous les déclencheurs.

#### Scenario: reprise après panne ou redémarrage
- **WHEN** une demande libérée reste en attente et que réseau et session autorisent le travail
- **THEN** une tentative bornée est planifiée même sans accueil visible, y compris après redémarrage du processus ou de la TV ; les restrictions Android peuvent différer l'exécution, sans perte de demande ni promesse d'envoi instantané

#### Scenario: résultat déjà appliqué
- **WHEN** une demande potentiellement envoyée est reprise et que la lecture distante confirme déjà sa cible
- **THEN** la demande correspondante est acquittée sans nouvelle mutation ; une lecture en échec maintient l'incertitude sans renvoi aveugle

#### Scenario: intention prioritaire et nouvelle révision
- **WHEN** le statut distant diffère ou qu'un nouveau vu/non vu remplace une demande pendant son envoi
- **THEN** l'état désiré local courant reste projeté ; l'ancien résultat ne supprime pas la nouvelle demande et la convergence vise la dernière intention explicite de la TV après sa propre fenêtre Annuler

#### Scenario: refus et erreurs temporaires
- **WHEN** le réseau échoue, le serveur limite les appels ou l'accès au compte est refusé
- **THEN** les erreurs temporaires gardent les intentions avec réessais espacés respectant le serveur ; le refus d'accès suspend les envois et expose la reconnexion selon le fournisseur ; aucun autre compte n'est utilisé

#### Scenario: absence de travail et concurrence
- **WHEN** la file est vide ou que plusieurs déclencheurs demandent sa synchronisation
- **THEN** aucun polling réseau ne reste actif sans travail utile et une même demande n'est pas envoyée concurremment ; le réessai manuel et le retour au premier plan réutilisent le même suivi des confirmations

#### Scenario: confirmation sans réserve complète
- **WHEN** le marquage est confirmé mais que le complément d'épisodes échoue
- **THEN** le marquage est acquitté sans être renvoyé ; la réserve reste explicitement incomplète et son complément peut être réessayé séparément

#### Scenario: file uniquement retenue par Annuler
- **WHEN** toutes les intentions restantes ont encore une fenêtre d'annulation ouverte
- **THEN** elles restent persistées sans envoi ni boucle de travail inutile ; leur libération durable planifie la synchronisation et une planification interrompue est réparée à la reprise
