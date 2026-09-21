# Delta launcher-shell

## MODIFIED Requirements

### Requirement: Qualité des visuels
Le héro et le panneau Top Shelf SHALL n'afficher que des visuels de bonne qualité : vidéo d'aperçu privilégiée quand l'app en fournit une, image ou vidéo d'au moins 1080 px de large, sinon le programme est écarté.

#### Scenario: image trop petite
- **WHEN** l'image décodée d'un programme fait moins de 1080 px de large, ou ne peut pas être chargée
- **THEN** le héro passe au programme suivant sans afficher l'image, et le panneau Top Shelf ne l'inclut pas

#### Scenario: vidéo trop petite
- **WHEN** la vidéo d'aperçu d'un programme fait moins de 1080 px de large
- **THEN** le héro replie sur le poster du programme, puis l'écarte si le poster est aussi insuffisant

### Requirement: Préchargement et mémoire
Le launcher SHALL valider les visuels avant de les afficher, en bornant ce travail : les premiers visuels du héro au chargement, les affiches d'une app quand le focus s'y pose. Il SHALL aussi limiter sa consommation mémoire et GPU.

#### Scenario: validation au chargement
- **WHEN** les programmes sont chargés
- **THEN** seuls les premiers visuels du héro sont vérifiés en arrière-plan (chargement, largeur ≥ 1080 px) ; ils sont gardés en mémoire, les suivants en cache disque ; le provider pouvant publier des centaines de programmes, aucun autre visuel n'est vérifié à ce moment

#### Scenario: validation à la demande
- **WHEN** le focus se pose sur une tuile de la grille
- **THEN** quelques affiches de cette app sont vérifiées une seule fois ; seules les affiches validées alimentent son panneau

#### Scenario: économie de ressources
- **WHEN** le héro est masqué (zone grille) ou une surface verre est invisible
- **THEN** le lecteur vidéo est libéré, les animations de fond et le flou d'arrière-plan sont arrêtés ; les images sont décodées en RGB565 avec au plus deux décodeurs simultanés
