# Tasks

## 1. Shared Focus Primitive And Lazy Shell

- [x] 1.1 Ajouter `FocusTargets` (`bind`/`focus`/`reveal` avec lambda de révélation fournie par l'appelant) dans `core/designsystem`, sans dépendance à `LazyListState`, sans état « actif », sans attente ni API de test ; vérifier par un test Compose TV qu'un item non composé est révélé puis focalisé, et qu'une cible non composée renvoie `false`.
- [x] 1.2 Extraire la coquille `SettingsLazyList` et `revealRow` (décalage d'en-tête + révélation + focus) ; la résolution de position reste chez chaque consommateur et est couverte par les tests de 2.x (dernière entrée de Cachés, source déplacée en fin de liste, bloc de mise à jour d'À propos).

## 2. Migrate Consumers

- [x] 2.1 Migrer `SourcesContent` en remplaçant `ListReveal` ; vérifier qu'une source focalisée déplacée hors fenêtre par un retri garde le focus.
- [x] 2.2 Migrer `HiddenContent` en remplaçant son registre local ; vérifier le repli vers une ligne initialement hors fenêtre et conserver les suites existantes (groupes, « Tout réactiver », retours de catégorie).
- [x] 2.3 Migrer `AboutContent` sans découper son bloc de mise à jour/QR ni lier les lignes de licences ; vérifier le retour d'écran système depuis les licences et conserver les tests existants de D-pad et de géométrie du QR.
- [x] 2.4 Conserver tags, styles, en-têtes et états loading/vide ; placer les chaînes extraites dans les ressources FR sans changer leur texte.

## 3. Integration

- [x] 3.1 Vérifier qu'aucun mécanisme parallèle de registre ou de révélation ne subsiste dans les réglages et que le bilan de lignes de production baisse.
- [ ] 3.2 Vérifier sur émulateur les parcours longs : Sources au D-pad après retri, Cachés avec repli hors fenêtre, licences vers le bas puis retour d'écran système.

## Workflow Follow-up

- Le propriétaire vérifie sur TV réelle en `assembleRelease` la navigation des longues listes.
- Après merge et cette validation, `openspec archive settings-lazy-lists` sans synchronisation de specs.
