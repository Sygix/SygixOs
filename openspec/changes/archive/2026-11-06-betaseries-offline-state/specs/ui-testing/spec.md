## MODIFIED Requirements

### Requirement: Couverture des parcours de suivi TV
Les parcours SHALL être testés en Compose TV avec testTags, key events physiques et données fictives : connexion, pays, sélecteur, épisodes, actions, erreur et état hors ligne. Rendu et navigation SHALL suivre les parcours fonctionnels et les composants existants, sans capture comparative. Les tests SHALL distinguer une action enregistrée localement d'une confirmation distante.

#### Scenario: association
- **WHEN** le test ouvre puis ferme une association, laisse expirer son code et demande un nouveau code
- **THEN** adresse/code/QR appartiennent à la même tentative, l'adresse visible conserve le chemin complet de `verification_url` et le QR encode la même URL précise plutôt que le domaine ; le QR n'est pas focusable, les actions ont un focus initial et Retour restaure Connecter sans poller une ancienne tentative

#### Scenario: pays en liste déroulante
- **WHEN** le test ouvre le pays, parcourt ses options, valide puis rouvre et annule
- **THEN** la liste reste ancrée dans les réglages sans modale, le focus et les bords suivent settings, seule la validation modifie la valeur et le libellé n'ajoute pas « · confirmé »

#### Scenario: composants du suivi
- **WHEN** le test présente synchronisation, indisponibilité, modales de contenu et retours transitoires
- **THEN** l'indicateur reste discret à droite sans suffixe « · Réglages », les modales réutilisent le composant existant dimensionné au contenu et les retours partagent le composant compact en bas à droite ; rendu glass, bordure et flou sont vérifiés sur TV, sans remplacer ces surfaces par des panneaux opaques

#### Scenario: échéance d'annulation
- **WHEN** une horloge contrôlée avance pendant une fenêtre Annuler, avec puis sans interaction
- **THEN** le retrait distant attend 10 secondes actives, le focus suspend le décompte sans le réinitialiser et la sortie reprend le temps restant ; annuler avant échéance n'envoie aucun retrait et l'expiration ne marque jamais vu

#### Scenario: sélecteur et épisodes
- **WHEN** le test parcourt les lecteurs et Autre application, puis les épisodes vus et non vus, annule et simule la disparition de l'entrée focusée
- **THEN** les bords ne bouclent pas, l'origine est restaurée, consulter ne modifie rien et le repli suivant/précédent reste utilisable ; chargement, vide, erreur et reconnexion sont distincts

#### Scenario: saisons à la demande
- **WHEN** la saison courante s'affiche puis qu'Autres saisons est ouvert et qu'une autre saison est validée
- **THEN** le sélecteur ne charge que les saisons demandées avec season explicite, sans interdire le préchargement indépendant de la réserve du compte ; six épisodes en réserve ne sont jamais présentés comme une saison entièrement chargée ; les tags tracking-seasons, tracking-season-picker, tracking-season-<number>, tracking-seasons-retry et tracking-season-back identifient le parcours sans texte ni donnée personnelle

#### Scenario: badge et masquage
- **WHEN** une carte de suivi n'a pas de lecteur choisi puis en reçoit un et est masquée/réaffichée
- **THEN** son badge passe du fournisseur à l'app ; le masquage persiste pour la série toutes sources et les candidats BetaSeries du héro, sans mutation du compte ; Réafficher restaure l'éligibilité

#### Scenario: ajout au compte depuis une carte locale
- **WHEN** le test ouvre le menu d'un contenu local identifié absent du compte connecté, puis répète sans compte, avec identité ambiguë et avec appartenance inconnue
- **THEN** Ajouter au suivi est présent seulement dans le premier cas, sans supprimer sources et retrait local ; avant ajout aucun choix de lecteur n'est persisté ; après ajout confirmé et appartenance relue le prochain lancement utilise le parcours commun, avec confirmation du premier lecteur si aucune préférence n'existe ; annulation ou échec d'ajout garde le parcours local sans appartenance fictive

## ADDED Requirements

### Requirement: Couverture de la persistance hors ligne du suivi
La persistance, la projection et le nettoyage du suivi SHALL être testés avec dépôt réel sur stockage de test et transport, session, ordonnanceur et horloges contrôlés. Les tests SHALL exercer recréation, révisions concurrentes et nettoyage référentiel pour épisodes et films sans utiliser le réseau réel.

#### Scenario: redémarrage sans réseau
- **WHEN** un cache et plusieurs vu/non vu sont enregistrés puis que dépôt et contrôleur sont recréés sans réseau
- **THEN** compte, candidats et intentions effectifs sont restaurés avant lecture distante, isolés par compte et sans faux succès de synchronisation

#### Scenario: réserve inter-saisons et épuisement
- **WHEN** une série compte plus de six épisodes diffusés dont une frontière de saison, puis que six sont marqués vus hors ligne
- **THEN** courant et cinq suivants ont été enregistrés et un seul candidat par série est projeté ; chaque demande reste isolée et durable ; après épuisement, aucun suivant n'est inventé ni fin distante déclarée, puis le complément en ligne rétablit la réserve

#### Scenario: nettoyage après confirmation
- **WHEN** des demandes sont confirmées avec épisodes consommés hors fenêtre, puis que leurs références d'annulation et de reprise locale sont libérées
- **THEN** les demandes acquittées et entrées inutiles disparaissent de la base ; les six candidats disponibles restent, ainsi que toute entrée encore protégée ; un échec ou résultat partiel ne purge pas les absents supposés

#### Scenario: film et annulation
- **WHEN** un film passe vu puis non vu ou est annulé sans réseau, puis est confirmé vu sans référence restante
- **THEN** l'état exact et les métadonnées nécessaires survivent à la recréation ; aucune requête de suivant ni suppression distante du film n'a lieu ; la confirmation finale supprime demande et entrée inutiles, sans réapparition depuis une vieille reprise locale

#### Scenario: réponse perdue et nouvelle intention
- **WHEN** une réponse est perdue après envoi puis que la même cible reçoit une intention plus récente pendant la reprise
- **THEN** une lecture précède la répétition ; un état déjà conforme évite le renvoi et la confirmation ancienne ne supprime pas la nouvelle révision ; le rafraîchissement concurrent n'écrase pas l'état effectif récent

#### Scenario: complément en échec après mutation réussie
- **WHEN** la lecture individuelle confirme le marquage mais que la récupération des suivants échoue
- **THEN** la demande est acquittée et n'est jamais renvoyée pour ce seul échec ; seul le complément reste à reprendre et le statut incomplet est observable

#### Scenario: arrière-plan et travail unique
- **WHEN** le double Android exécute le travail sans Activity puis simule interruption, redémarrage et déclenchement simultané d'un essai manuel
- **THEN** la file est reprise avec contraintes et backoff sans envoi concurrent ; l'envoi respecte les budgets, la file vide arrête le travail et aucune lecture TV Provider ni initialisation de lecteur n'est déclenchée

#### Scenario: déconnexion pendant envoi et purge interrompue
- **WHEN** A est déconnecté pendant un envoi, que B est associé puis que l'ancienne réponse arrive, y compris après interruption de la purge locale
- **THEN** aucun nouvel envoi de A ne part, aucune réponse ne recrée A ni ne modifie B ; cache et file de A sont purgés et ne sont pas rejoués à sa reconnexion ; les préférences indépendantes du compte restent présentes

#### Scenario: erreurs de stockage et migrations
- **WHEN** une écriture ou migration échoue, puis que le stockage redevient disponible
- **THEN** aucun succès local non durable n'est publié, aucune file n'est effacée par migration destructive et le réessai peut retrouver les intentions conservées

### Requirement: Couverture des parcours TV hors ligne
Les états hors ligne du suivi SHALL être testés en Compose TV à la taille prescrite, avec les sélecteurs existants et événements D-pad physiques. La projection commune SHALL couvrir films et épisodes, sans nouveau palier ni vol de focus par les messages de synchronisation.

#### Scenario: épisode suivant et film disparu
- **WHEN** le test marque un épisode puis un film vu sans réseau depuis À suivre et depuis le héro
- **THEN** le suivant connu et le retrait du candidat film sont cohérents entre zones ; l'annulation demeure accessible, le focus reste sur la série ou suit le repli de contenu disparu et aucun succès distant n'est annoncé

#### Scenario: attente sans carte et réglages
- **WHEN** la dernière carte disparaît alors qu'une demande reste en attente puis que les réglages sont ouverts et refermés
- **THEN** l'état reste accessible depuis l'engrenage et Synchroniser ; focus initial, bords sans boucle, Retour et restauration suivent leurs contrats ; fermer une notice ou les réglages ne supprime pas la demande

#### Scenario: reprise et état du stockage
- **WHEN** le compte est restauré hors ligne, puis que le transport confirme certaines demandes et refuse l'accès ou que le stockage échoue dans d'autres cas
- **THEN** les tests distinguent cache affichable, travail en attente, confirmation partielle, reconnexion et erreur de stockage ; le contrôle toujours présent garde son focus et une disparition applique le repli existant
