## ADDED Requirements

### Requirement: Couverture du suivi multi-fournisseurs
Les contrats communs de suivi SHALL être exercés avec BetaSeries et Trakt, à frontières et horloges contrôlées, sans réseau réel dans la suite JVM. Les tests SHALL vérifier que l'ajout de Trakt conserve session, intentions et comportement BetaSeries, sans duplication des projections ni confusion entre configuration d'application et jetons utilisateur.

#### Scenario: configuration persistante et refus
- **WHEN** des fixtures officielles anonymisées simulent découverte, imports, cache âgé, refus explicite, appels concurrents, refus générique et second rejet
- **THEN** l'extraction reste bornée et non exécutante, le cache survit sans échéance et au redémarrage, seule l'invalidité prouvée lance une récupération mutualisée, et aucun refus ambigu n'efface la session ni ne boucle

#### Scenario: jetons Trakt et migration BetaSeries
- **WHEN** le test migre une ancienne session BetaSeries puis simule expiration Trakt, renouvellements concurrents, réponse perdue, erreur disque et déconnexion pendant le renouvellement
- **THEN** le compte BetaSeries et sa file restent liés ; Trakt ne renouvelle qu'à la demande, ne remplace pas une session plus récente et ne réattribue pas un jeton à un autre client ; la session n'est déclarée renouvelée qu'après sauvegarde atomique

#### Scenario: compte, réserve et mutations natives
- **WHEN** le test simule compte paginé, séries masquées, réserve inter-saisons, film vu, relectures multiples, mutation incertaine et endpoint d'offres refusé
- **THEN** fusion, cache, réserve et nettoyage respectent leurs contrats communs ; seules les actions natives applicables sont exposées, le statut déjà vu ne crée pas de lecture, Non vu ne touche que sa cible et aucune suppression globale d'historique ni disponibilité régionale inventée n'est envoyée

#### Scenario: Annuler avant envoi
- **WHEN** un vu/non vu manuel ou confirmé au retour est enregistré pour chacun des deux fournisseurs, puis que l'horloge avance dans la fenêtre avec focus, réglages, arrière-plan, redémarrage et synchronisation concurrente
- **THEN** la projection locale est immédiate mais le nombre de mutations réseau reste nul tant que l'intention est retenue ; Annuler restaure sans envoi, l'échéance libère une seule fois et un travail déjà planifié ne contourne pas la retenue

#### Scenario: révisions et nettoyage
- **WHEN** une ancienne révision potentiellement envoyée, une nouvelle intention annulable et un résultat tardif se croisent
- **THEN** la confirmation n'acquitte pas la nouvelle intention, son annulation ne perd pas l'état antérieur encore utile et le nettoyage conserve les données référencées par une fenêtre restaurable

### Requirement: Couverture TV du choix de fournisseur
Le sélecteur de fournisseur et les parcours adaptés SHALL être testés en Compose à taille TV selon « Tests UI sans capture d'image », avec données fictives et événements D-pad physiques. Les composants existants SHALL conserver leur rendu et navigation ; les badges, attributions et contrôles SHALL correspondre au fournisseur actif sans nouvelle surface d'accueil.

#### Scenario: choix initial, persistance et fermeture
- **WHEN** le test ouvre Suivi de visionnage sans compte, choisit Trakt, rouvre la liste puis annule ou passe en arrière-plan
- **THEN** tracking-provider précède tracking-connect et reçoit le focus initial ; tracking-provider-list contient BetaSeries puis Trakt avec les tags tracking-provider-betaseries et tracking-provider-trakt ; coche, validation, fermeture, persistance, bords et repli suivent settings sans connexion automatique

#### Scenario: compte existant et fournisseur verrouillé
- **WHEN** une session BetaSeries ou Trakt est active, en restauration ou conservée après panne
- **THEN** le fournisseur exact est affiché et verrouillé avec son explication, Synchroniser ou son état adapté est le premier contrôle utilisable, et aucun changement de sélection n'efface une demande ; après déconnexion effective le choix redevient disponible

#### Scenario: association et retour de focus
- **WHEN** l'association du fournisseur sélectionné charge, attend, expire, échoue, est refusée ou ferme par Retour
- **THEN** les sélecteurs tracking-device existants sont réutilisés, le code, l'adresse et le QR concernent le bon fournisseur et la même tentative ; le panneau garde sa navigation, sans vol de focus ni réponse tardive restaurant une session fermée

#### Scenario: restauration d'Annuler
- **WHEN** un marquage encore annulable est restauré après interruption puis annulé ou libéré
- **THEN** tracking-notification et tracking-undo réapparaissent dans le composant existant sans prendre le focus ; le temps actif restant et ses pauses sont respectés, le focus d'origine reste utilisable et aucun marquage automatique n'est introduit

#### Scenario: badges, masquage et langues
- **WHEN** les deux fournisseurs alimentent séparément les mêmes scénarios de cartes, héro, erreurs, retrait local et réaffichage
- **THEN** les noms et badges correspondent au fournisseur actif sinon au lecteur choisi ; le retrait reste local et couvre aussi les candidats du héro ; tous les nouveaux textes existent dans les cinq langues selon « Couverture des langues »
