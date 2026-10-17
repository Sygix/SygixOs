# Spec Delta

## MODIFIED Requirements

### Requirement: Couverture du rendu du héro et du verre
Le rendu du héro et du verre (« Thème », « Diaporama héro » et « Préchargement et mémoire » de `launcher-shell`) SHALL être couvert par des tests JVM : calcul des tailles de décodage, transformations du visuel et de l'arrière-plan du verre et rendu du verre sous Robolectric en rendu natif (`@GraphicsMode(NATIVE)`), changement de programme par test Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) avec un chargeur d'images de test. La fluidité SHALL être mesurée sur la TV réelle (`dumpsys gfxinfo` sur l'APK de release ou sur une build minifiée équivalente).

#### Scenario: tailles de décodage
- **WHEN** la fenêtre ou une bannière mesure 3840 × 2160
- **THEN** le visuel du héro est demandé en 1920 × 1080 et la bannière à la taille de la tuile au zoom de focus (332 × 187 sur une TV 1080p), plafonnée à 480 × 270 sur une dalle plus dense, sans agrandir une image plus petite

#### Scenario: visuel voilé
- **WHEN** un visuel blanc de 3840 × 2160 passe par la transformation du héro
- **THEN** l'image produite mesure 1920 × 1080, est opaque, reste blanche hors des voiles (coin haut droit compris, son voile étant dessiné à part) et est assombrie en bas et à gauche

#### Scenario: arrière-plan du verre
- **WHEN** un visuel moitié noir, moitié blanc passe par la transformation du verre
- **THEN** l'image produite mesure le huitième du visuel, est opaque, a une transition adoucie entre les deux moitiés et est assombrie en bas

#### Scenario: alignement du verre
- **WHEN** la copie floutée sous le dock est à motif, zoomée à 1,08 et à mi-fondu sur une autre copie
- **THEN** chaque point du verre montre la couleur du point du visuel situé dessous, avec ce zoom et ce fondu

#### Scenario: verre sans copie floutée
- **WHEN** un visuel est chargé sans copie floutée
- **THEN** la surface verre utilise le flou en direct, puis la copie dès qu'elle existe

#### Scenario: voile du coin pendant le Ken Burns
- **WHEN** un visuel blanc zoome jusqu'à 1,08
- **THEN** l'écran en haut à droite est voilé et ne s'éclaircit pas entre le début et la fin du zoom

#### Scenario: retour pendant le fondu
- **WHEN** l'utilisateur revient au programme précédent à mi-fondu
- **THEN** le visuel suivant s'efface à partir de son opacité courante et le précédent reste entièrement visible dessous

#### Scenario: couleur du verre
- **WHEN** le dock est rendu sur un arrière-plan gris moyen puis sur un arrière-plan sombre
- **THEN** sa couleur est celle de l'arrière-plan recouvert de la teinte de la maquette, à 3 niveaux près, et reste sombre sur un arrière-plan sombre

#### Scenario: changement de programme
- **WHEN** le héro passe d'un programme au titre court à un programme au titre long
- **THEN** pendant tout le fondu, le bas de chaque bloc « hero-metadata » et la position de « hero-open » ne changent pas ; à la fin, seul le nouveau bloc reste
