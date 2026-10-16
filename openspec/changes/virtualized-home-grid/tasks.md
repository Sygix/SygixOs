# Tasks

## 1. Reference Measurements

- [ ] 1.1 Ajouter des fixtures synthétiques de 20, 200 et 2 000 apps avec sources et artworks contrôlés ; vérifier que les identités et la navigation sont déterministes, sans accès au TV Provider ni ouverture réelle d'app.
- [ ] 1.2 Ajouter l'hôte de profilage limité au source set `perf` et documenter son lancement ; vérifier son absence du manifest release et enregistrer deux échauffements puis cinq passages de référence avec CPU, mémoire et appels de préparation.

## 2. Scroll And Layout

- [ ] 2.1 Adapter le modèle pur sections/rangées/ancres et sa conversion index-offset ; vérifier par JUnit les deux positions Up Next, les titres, les rangées vides et le shelf ouvert/fermé.
- [ ] 2.2 Remplacer le scroll exhaustif par l'autorité lazy unique et l'en-tête résident de D1 ; vérifier par tests Compose TV la présence des tuiles visibles et l'absence initiale de cibles éloignées, en gardant les testTags et les chaînes FR existants.
- [ ] 2.3 Conserver transitions interrompues, verre et géométrie du shelf ; vérifier les assertions de `HomeNavigationTest`, `HomeGridScrollTest`, de coins/focus et de continuité, y compris depuis une rangée profonde.

## 3. Focus And Identity

- [ ] 3.1 Implémenter révélation/attachement/focus par clé avec annulation des demandes dépassées ; vérifier le D-pad jusqu'à une cible non composée, les bords et le retour héro puis grille à l'ancre laissée, à `w960dp-h540dp-xhdpi`.
- [ ] 3.2 Préserver retour d'app, disparition de cible et déplacement entre fenêtres ; vérifier validation/annulation du déplacement, repli vers le voisin puis héro si vide et absence de fuite d'artwork dans une tuile recyclée.

## 4. Artwork Preparation

- [ ] 4.1 Remplacer le préchargement exhaustif et les chargements de tuiles pendant le mouvement par le préparateur borné de D4 ; vérifier avec un double à portes contrôlées les apps demandées, la fenêtre de prélecture, l'absence de travail nouveau pendant le mouvement et l'annulation du non commencé.
- [ ] 4.2 Vérifier les résultats tardifs, l'absence de bannière, l'erreur et la conservation des replis par tests déterministes ; conserver les délais de validation Top Shelf et les tests de démarrage qui interdisent la composition anticipée de grille.

## 5. Integration And Evidence

- [ ] 5.1 Rejouer les fixtures et traces avant/après sur émulateur, avec les mêmes paramètres et au moins cinq passages ; livrer les échantillons, médianes et dispersion, en distinguant composition, décodage, CPU et mémoire, sans publier de données de provider.
- [ ] 5.2 Documenter le coordinateur et le protocole dans la doc de performance ; conserver le README utilisateur si le comportement visible est identique, sinon mettre à jour ses seules instructions devenues inexactes et vérifier leur parcours.
- [ ] 5.3 Exécuter les tests ciblés puis `./gradlew test assemblePerf --no-daemon`, `openspec-verify-change` et la validation ciblée stricte ; faire vérifier navigation et focus par le tester sur émulateur et obtenir une review indépendante du head exact avant livraison de la PR.

## Workflow Follow-up

- Le propriétaire valide sur TV réelle en `assembleRelease` après release ; cette passe reste distincte des comparaisons locales et aucun accès TV autonome n'est autorisé.
- Après merge et validation TV, archiver avec `openspec archive virtualized-home-grid` et vérifier les deltas synchronisés ; les six échecs stricts historiques identifiés avant ce change restent à distinguer de toute nouvelle régression documentaire.
