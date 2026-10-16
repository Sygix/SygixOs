# Spec Delta

## ADDED Requirements

### Requirement: Activité de l'accueil entre destinations
Quand les réglages remplacent l'accueil, le launcher SHALL suspendre les consommateurs propres au rendu d'accueil et libérer leurs ressources visuelles. Il SHALL conserver les données et identités utiles à la reprise, sans annuler les opérations applicatives indépendantes ni l'observation partagée des compteurs exigée par `settings`. « Écran de démarrage » et les retours Home gardent leurs contrats.

#### Scenario: accueil démonté
- **WHEN** les réglages deviennent la destination active
- **THEN** lecteur, animations, flou et effets de composition d'accueil sont arrêtés ; aucune nouvelle validation ou préparation visuelle propre à l'accueil n'est lancée pendant son absence

#### Scenario: opérations applicatives
- **WHEN** une configuration P7 ou une mise à jour est en cours et que la destination d'accueil est démontée
- **THEN** les opérations suivent leurs durées de vie et leurs règles existantes ; quitter un panneau n'annule pas une configuration qui doit continuer

#### Scenario: état de session conservé
- **WHEN** l'utilisateur revient des réglages dans le même processus
- **THEN** les données du catalogue, l'identité du programme héro, les échecs visuels de session, l'ancre de grille et l'identité de sa dernière cible restent disponibles ; le retour affiche d'abord le héro selon `settings`, et non la dernière zone active

#### Scenario: élément devenu invalide
- **WHEN** le programme ou l'app mémorisée disparaît, est masqué ou exclu par les sources pendant les réglages
- **THEN** le retour utilise les replis existants du héro et de « Focus d'une app disparue », sans ouvrir ni afficher un ancien contenu par une clé devenue invalide

#### Scenario: résultat visuel tardif
- **WHEN** un chargement visuel se termine après le démontage de sa destination
- **THEN** il ne réactive aucun lecteur ni animation et ne publie pas dans une ancienne cible d'interface ; une ressource libérée est recréée seulement pour une destination visible qui en a encore besoin

#### Scenario: retour sans contenu prêt
- **WHEN** l'accueil revient avec catalogue ou visuel encore en chargement, source absente, erreur ou permission refusée
- **THEN** ses états et replis existants s'affichent sans nouveau splash dans le processus ; les rafraîchissements suivent les contrats des sources et sont coalescés avec le retour de destination, sans doublonner ceux de la reprise d'Activity
