# Delta settings

## MODIFIED Requirements

### Requirement: Icône réglages flottante
Le home SHALL afficher sur le héro une icône engrenage flottante en haut à droite, qui ouvre la page de réglages. L'icône SHALL être un engrenage plein façon tvOS, blanc opaque, dessiné sans fond, sans bordure ni matériau verre. Elle SHALL n'être affichée qu'avec le héro : elle disparaît en fondu quand le héro se masque pour la zone grille (« Fond de la zone grille » de `launcher-shell`) et réapparaît avec lui.

#### Scenario: affichage
- **WHEN** le héro est affiché
- **THEN** l'engrenage est visible en haut à droite, blanc opaque, sans fond, sans bordure ni verre, sans masquer le héro et sans détourner le focus du héro à l'ouverture du home

#### Scenario: accès DPAD
- **WHEN** l'utilisateur presse haut depuis le héro
- **THEN** l'icône réglages prend le focus avec l'état focus net (zoom + halo, cohérent avec le focus tvOS du launcher) ; bas depuis l'icône rend le focus au héro

#### Scenario: ouverture
- **WHEN** l'utilisateur presse OK sur l'icône réglages
- **THEN** la page de réglages plein écran s'ouvre, la lecture du héro est mise en pause

#### Scenario: zone grille
- **WHEN** l'utilisateur passe en zone grille (« Navigation 3 paliers » de `launcher-shell`)
- **THEN** l'engrenage disparaît en fondu, avec la même durée et la même courbe que le héro ; il n'est ni visible ni focusable tant que la grille est affichée

#### Scenario: retour sur le héro
- **WHEN** l'utilisateur revient sur le héro depuis la grille (Retour, ou remontée par le dock)
- **THEN** l'engrenage réapparaît en fondu avec le héro, sans prendre le focus
