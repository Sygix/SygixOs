# Delta launcher-shell

## ADDED Requirements
### Requirement: Écran initial du home
Le home SHALL s'ouvrir sur un héro plein écran : poster de recommandation (Jellyfin) ou, à défaut, vidéo aérienne animée en boucle.

#### Scenario: état initial
- **WHEN** le launcher démarre
- **THEN** le héro plein écran est affiché avec le focus, le dock est visible en overlay bas semi-transparent

#### Scenario: fallback sans contenu
- **WHEN** aucune recommandation n'est disponible (Jellyfin vide/injoignable)
- **THEN** le héro joue une vidéo aérienne en boucle, sans crash

### Requirement: Carrousel héro
Le héro SHALL défiler horizontalement avec le poster suivant/précédent visible en bord de cadre.

#### Scenario: navigation
- **WHEN** l'utilisateur presse gauche/droite sur le héro
- **THEN** le carrousel passe au poster adjacent avec animation, les bords du voisin restent visibles

#### Scenario: clic poster
- **WHEN** l'utilisateur clique sur un poster
- **THEN** l'app source s'ouvre sur la page du contenu sans lecture automatique (fallback : ouverture simple de l'app)

### Requirement: Navigation 3 paliers
Le DPAD SHALL naviguer héro → dock → grille (descend) et grille → dock → héro (monte).

#### Scenario: traversée du dock
- **WHEN** l'utilisateur descend depuis le dock
- **THEN** la grille plein écran prend le focus et le dock se masque
