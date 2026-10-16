# Tasks

## 1. Shared Lazy Shell

- [ ] 1.1 Extraire la coquille `SettingsLazyList` et son contrat de contenu lazy sans heuristique de longueur ; vérifier par un test Compose TV que les lignes visibles sont attachées et qu'une ligne éloignée ne l'est pas, tout en restant accessible après défilement.
- [ ] 1.2 Extraire la révélation par clé avec annulation et registre limité aux cibles composées ; vérifier par JUnit les clés/indices, et par test Compose l'attachement avant focus, une demande remplacée et le nettoyage après sortie de fenêtre.
- [ ] 1.3 Documenter la responsabilité de la coquille et la distinction cible logique/item lazy ; vérifier qu'elle ne charge aucune source et ne porte aucune politique de tri, d'action ou de repli métier.

## 2. Migrate Consumers

- [ ] 2.1 Migrer `SourcesContent` en conservant tri figé, compteurs et identité de focus ; vérifier les comptes tardifs, le toggle sur place et la révélation d'une app hors fenêtre à taille TV.
- [ ] 2.2 Migrer `HiddenContent` avec ses groupes et replis existants ; vérifier disparition de la cible, repli initialement hors fenêtre, dépliage vers la quatrième ligne, « Tout réactiver » et retours de catégorie.
- [ ] 2.3 Migrer `AboutContent` sans découper artificiellement son bloc de mise à jour/QR ; vérifier longues licences, accès aux deux bouts de liste, disparition de la ligne d'installation, retour système et géométrie du QR par les tests existants.
- [ ] 2.4 Conserver les tags, styles, en-têtes, états loading/vide/erreur déjà fournis et leurs sources ; vérifier l'équivalence des assertions existantes et placer toute chaîne extraite dans les ressources FR sans changer son texte.

## 3. Integration And Evidence

- [ ] 3.1 Mesurer sur les mêmes longues fixtures le nombre d'items attachés, la première composition et les allocations avant/après ; livrer les résultats et vérifier que la borne ne dépend pas du nombre total de lignes, sans attribuer à cette refonte une virtualisation déjà présente.
- [ ] 3.2 Vérifier la réutilisation effective de la coquille et de la révélation dans les trois consommateurs, sans mécanisme parallèle ni registre de requesters qui grandit avec toutes les lignes parcourues ; documenter les appels et leurs états conservés.
- [ ] 3.3 Exécuter les tests ciblés puis `./gradlew test assemblePerf --no-daemon` et `openspec-verify-change` avec `skip_specs: true` ; faire vérifier les parcours longs sur émulateur et obtenir une review indépendante du head exact, sans ajouter de tests qui ne font que refléter l'implémentation.

## Workflow Follow-up

- Le propriétaire vérifie sur TV réelle en `assembleRelease` après release la navigation et la fluidité des longues listes ; le README est conservé si ses instructions restent exactes, sinon seules les instructions affectées sont mises à jour.
- Après merge et cette validation, `openspec archive settings-lazy-lists` sans synchronisation de specs ; garder distincts les échecs stricts historiques du dépôt et les résultats de ce refactoring.
