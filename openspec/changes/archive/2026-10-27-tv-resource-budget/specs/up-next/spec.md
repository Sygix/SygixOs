## MODIFIED Requirements

### Requirement: Rafraîchissement
La rangée SHALL recharger le TV Provider sur notification, octroi de `READ_TV_LISTINGS` ou échéance de « Contenu héro TV Provider » de launcher-shell au premier plan. Au retour, les scénarios ci-dessous et « Contenus ouverts depuis SygixOs » SHALL déterminer la relecture. Les résultats SHALL suivre « Validité des programmes publiés ». L'apport du compte SHALL suivre sa source et son rafraîchissement propres (watch-tracking), et ses changements SHALL être projetés sans relire le TV Provider.

#### Scenario: retour au premier plan
- **WHEN** le launcher revient au premier plan après un changement du TV Provider pendant l'arrêt, après l'échéance de la périodicité de relecture ou après l'ouverture d'un contenu depuis la rangée, ou que la permission vient d'être accordée
- **THEN** la rangée se recharge une seule fois, le contenu précédent reste affiché pendant ce temps seulement s'il est encore valide

#### Scenario: retour sans changement
- **WHEN** le launcher revient au premier plan sans changement du TV Provider pendant l'arrêt, avant l'échéance de la périodicité et sans ouverture depuis la rangée
- **THEN** la rangée garde son contenu sans relire sa source ; ses échéances restent contrôlées selon « Validité des programmes publiés »

#### Scenario: observer du provider
- **WHEN** les programmes du TV Provider changent pendant que le launcher est au premier plan
- **THEN** un `ContentObserver` sur `WatchNextPrograms.CONTENT_URI` déclenche le rechargement, protégé contre `SecurityException` et l'absence du provider ; il reste enregistré pendant l'arrêt et un changement reçu alors est relu au retour

#### Scenario: relecture périodique et arrière-plan
- **WHEN** la périodicité de relecture est atteinte sans notification du provider
- **THEN** la rangée relit sa source au premier plan seulement ; le retour au premier plan vérifie les échéances avant d'afficher un ancien résultat

#### Scenario: apport du compte
- **WHEN** l'état du compte de suivi change, au premier plan ou pendant l'arrêt
- **THEN** la rangée projette ce changement sur les programmes déjà lus sans relire le TV Provider ; le retour au premier plan n'entraîne pas de relecture du provider pour ce seul changement, et le rafraîchissement du compte reste celui de sa source
