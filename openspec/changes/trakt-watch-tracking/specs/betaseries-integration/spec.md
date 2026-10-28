## MODIFIED Requirements

### Requirement: Configuration du client web récupérable
L'intégration SHALL récupérer automatiquement la configuration nécessaire depuis les ressources officielles du client web BetaSeries selon `tracking-application-configuration`, avec cache hors sauvegarde transférable, sans expiration périodique et récupération bornée après rejet explicite. Un changement de format SHALL produire une erreur explicite, sans configuration fixe de secours ni évaluation de script distant.

#### Scenario: découverte
- **WHEN** une configuration est nécessaire et aucune configuration valide n'est disponible
- **THEN** les ressources officielles sont découvertes et analysées comme des données selon « Découverte officielle bornée » ; seules les valeurs correspondant au contrat testé sont utilisées

#### Scenario: rotation
- **WHEN** le service refuse explicitement la configuration d'application précédemment valide
- **THEN** une récupération mutualisée est tentée selon « Récupération après rejet de configuration » puis l'opération peut être rejouée une seule fois si elle est sans effet de bord ; un jeton utilisateur refusé suit son traitement de session distinct, pas une boucle de récupération de clé

#### Scenario: ressource absente ou changée
- **WHEN** le site ne fournit plus les champs attendus, change de structure ou reste injoignable
- **THEN** la connexion expose son indisponibilité et son réessai ; les données locales et les préférences restent intactes

#### Scenario: conservation locale
- **WHEN** une configuration valide a été obtenue
- **THEN** elle est conservée hors sauvegarde transférable et réutilisée au démarrage sans expiration périodique ; une configuration existante âgée de plus de sept jours reste utilisable et seul un refus explicite déclenche sa redécouverte automatique

#### Scenario: bornes de la découverte
- **WHEN** la découverte s'exécute
- **THEN** elle applique les plafonds et l'exécution hors thread principal de « Découverte officielle bornée » aux ressources officielles de www.betaseries.com ; tout dépassement expose une indisponibilité réessayable
