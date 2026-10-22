# Suivi des anomalies et travaux reportés

Dernière revue : **22 octobre 2026**, après v0.3.2 et le correctif des images Jellyfin. Les sujets terminés, validés sur la TV par le propriétaire ou abandonnés sont retirés ; leur historique reste dans git et dans les changes archivés. Les numéros des sujets restants sont conservés pour les références historiques.

## 4. Tests Robolectric instables en CI

**Constat** : la CI échoue de façon intermittente sur des tests différents d'un run à l'autre, alors que la suite complète passe en local :
- `main`, [run 39695972888](https://github.com/Sygix/SygixOs/actions/runs/39695972888) : `HomeViewModelSourceToggleTest` / `re-enabling a source re-prepares the shelf of the focused app on the recomputed hero` (`TimeoutCancellationException`, puis `UncaughtExceptionsBeforeTest`) ;
- PR #89, [run 39793889214](https://github.com/Sygix/SygixOs/actions/runs/39793889214) : `HomeRefreshTest` / `lateImagesCannotRepopulateWithdrawnExpiredReplacedOrReusedProgramGenerations` (`NoSuchElementException`).

Sous forte charge de la machine de build, des tests des réglages (`HiddenPaneTest`, `SettingsNavigationTest`, `SettingsViewModelTest`) échouaient aussi par dépassement de délai.

**Piste** : rendre ces tests déterministes, sans dépendance au temps réel ni à la vitesse de la machine (horloge de test, attente d'un état plutôt que d'un délai).
