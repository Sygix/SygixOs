# Tasks

## 1. Accueil non placé

- [x] 1.1 Mesurer sans placer la colonne de l'accueil pendant les réglages (état lu au placement) ; test Compose TV (`w960dp-h540dp-xhdpi`) : `zone-hero`, `zone-dock` et `home-grid` existent sans être affichés, aucun nœud hors de `settings-screen` ne prend le focus au DPAD, Retour rend `zone-hero` affiché et focusé.
- [x] 1.2 Mettre en pause la vidéo d'aperçu pendant les réglages au lieu de l'arrêter ; test : même élément chargé et en pause pendant les réglages, lecture reprise au retour sans nouvelle préparation, première image conservée, titre affiché.
- [x] 1.3 Faire de la page de réglages la source du verre affiché par-dessus elle ; tests : la liste déroulante de « Position d'À suivre » et le panneau du launcher système ouvert depuis les réglages sont en verre et floutent la page de réglages, pas le héro.

## 2. À propos

- [x] 2.1 Lire le modèle de licences une seule fois par processus ; test : à une seconde visite d'« À propos », les licences sont listées dès la première image, sans ligne de chargement.

## 3. Vérification

- [x] 3.1 Suites existantes des réglages, de l'accueil, de la touche Home et du panneau du launcher système vertes avec `./gradlew test`.
- [ ] 3.2 Sur émulateur en `assembleRelease` : images en retard pendant la navigation dans les réglages avant/après ; retour des réglages (Retour, Home) avec visuel, titre et bouton affichés immédiatement, image et vidéo d'aperçu ; panneau du launcher système ouvert depuis les réglages.
- [ ] 3.3 Validation du propriétaire sur la TV réelle en `assembleRelease`, puis `openspec archive settings-destination`.
