## MODIFIED Requirements

### Requirement: Indicateur d'indisponibilité du suivi
Une panne du fournisseur connecté ou de la restauration de sa session SHALL avoir un indicateur sans vol de focus ni notification répétée, avec cause et Réessayer ou Reconnecter. Le détail SHALL rester accessible même sans identité chargée ni carte À suivre et disparaître après rétablissement confirmé, absence de session confirmée ou déconnexion.

#### Scenario: panne partielle
- **WHEN** le compte échoue et que les programmes locaux restent disponibles
- **THEN** ils restent utilisables ; l'indicateur ne prend ni carte ni focus, utilise TextStyles.RowSecondary sans dépasser la hauteur du titre, aligné à droite sans suffixe « · Réglages » ; seul le trajet depuis l'engrenage donne accès au détail

#### Scenario: accès sans carte depuis le héro
- **WHEN** aucune notification de suivi n'est affichée, le compte est en panne et le focus est sur l'engrenage
- **THEN** Droite ouvre le détail (cause, Réessayer sinon Reconnecter) ; fermer rend l'engrenage ; ce trajet reste identique quand À suivre est masqué

#### Scenario: synchronisation en cours
- **WHEN** une synchronisation affiche un statut dans l'en-tête d'À suivre
- **THEN** ce statut reprend le placement discret à droite et n'agrandit pas l'en-tête ni ne prend la place des cartes

#### Scenario: aucune carte restante
- **WHEN** la panne retire les dernières cartes du suivi
- **THEN** le repli de focus de rangée vide s'applique et la cause/réessai restent accessibles depuis les réglages de suivi, sans dépendre d'une carte disparue

#### Scenario: détail et fermeture
- **WHEN** le détail est ouvert
- **THEN** Réessayer, sinon Reconnecter a le focus ; haut/bas parcourent ses actions sans boucle, gauche/droite restent dans le panneau, Retour restitue l'origine ou son repli ; une action disparue au rétablissement restitue le contexte sans focus perdu

#### Scenario: restauration en échec au démarrage
- **WHEN** la restauration échoue avant de connaître l'identité du compte et que la session n'est pas confirmée absente
- **THEN** l'indicateur et le trajet vers son détail restent disponibles ; Réessayer utilise la session conservée selon betaseries-integration et ne lance pas l'association ; les programmes locaux et les apps restent utilisables
