# Tasks

## 1. State Ownership

- [ ] 1.1 Extraire l'état léger de destination et les identités/ancres de reprise hors des compositions démontées ; vérifier par JUnit les transitions ouverture/fermeture, le retour héro, la conservation de l'ancre grille et les cibles disparues.
- [ ] 1.2 Conserver sélection et erreurs visuelles de session hors de l'UI détruite, sans sauvegarder de ressources graphiques ; vérifier qu'un programme encore valide reprend et qu'une source désactivée ou un échec déjà connu n'est pas réutilisé incorrectement.

## 2. Exclusive Host

- [ ] 2.1 Introduire l'hôte à deux branches exclusives dans l'Activity existante et obtenir les ViewModels au niveau commun ; vérifier l'absence de `zone-hero`, `zone-dock` et `home-grid` pendant `launcher-destination-settings`, puis le remontage sous `launcher-destination-home`.
- [ ] 2.2 Remonter les handlers Home et panneaux P7 hors de `LauncherHome` ; conserver les tags et textes FR, et vérifier les parcours `ForegroundHomeTest`, réglages/liste de position, confirmation et retour d'écran système à taille TV.
- [ ] 2.3 Vérifier que la fermeture explicite revient au héro, qu'une réouverture commence sur la première catégorie et qu'un simple retour au premier plan conserve la catégorie prévue ; couvrir la fermeture de la liste de position sans changer sa valeur.

## 3. Producer Lifetimes

- [ ] 3.1 Piloter explicitement l'activité du rendu d'accueil, des observations Up Next et des préparations visuelles ; vérifier par portes contrôlées leur arrêt immédiat, l'absence de publication tardive et leur reprise sans deuxième splash, sans dépendre du délai de `WhileSubscribed`.
- [ ] 3.2 Conserver au niveau commun l'unique observation des compteurs et les données nécessaires aux réglages ; vérifier ses abonnements/lectures entre destinations et sa pause/reprise avec le cycle de vie d'Activity.
- [ ] 3.3 Préserver scopes des mises à jour et configurations P7 ; vérifier qu'une séquence continue après fermeture de panneau et que Home au premier plan ferme l'UI sans annuler cette opération.

## 4. Return Paths

- [ ] 4.1 Coalescer activation de destination et refresh d'Activity ; vérifier les comptes d'appels sans doublon et les états chargement/vide/erreur/permission avec doubles de sources.
- [ ] 4.2 Vérifier les changements de préférences pendant les réglages, la sélection héro invalide, une app de grille disparue et le retour à l'ancre après fermeture ; garder la navigation D-pad et les tests sémantiques à `w960dp-h540dp-xhdpi`.
- [ ] 4.3 Vérifier recréation de l'Activity et du processus selon les règles de démarrage existantes, sans transformer un démontage de destination dans le même processus en démarrage à froid.

## 5. Integration And Evidence

- [ ] 5.1 Comparer sur émulateur l'activité au repos des deux destinations, les montages/dispositions et la mémoire après plusieurs allers-retours avec la même fixture ; livrer les traces et mesures répétées sans seuil de temps réel en CI.
- [ ] 5.2 Documenter les propriétaires de navigation, d'ancres et de producteurs ; vérifier le parcours du README et le conserver si ses instructions utilisateur restent exactes, en plaçant toute chaîne ajoutée dans les ressources FR.
- [ ] 5.3 Exécuter tests ciblés puis `./gradlew test assemblePerf --no-daemon`, `openspec-verify-change` et la validation ciblée stricte ; obtenir smoke émulateur du tester et review indépendante au head exact de la PR.

## Workflow Follow-up

- Validation du propriétaire sur TV réelle en `assembleRelease` après release, notamment la reprise vidéo et les panneaux système ; pas d'accès autonome à la TV.
- Après merge et validation TV, `openspec archive settings-destination` ; les deltas P7 restent propriétaires de leurs exigences et les échecs stricts historiques sont identifiés séparément.
