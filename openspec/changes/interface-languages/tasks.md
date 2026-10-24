# Tasks

## 1. Textes en ressources

- [x] 1.1 Passer en ressources les textes écrits en dur (dock vide, menu d'une app, aide au déplacement, boutons du héro, À propos, compteur de programmes en pluriel, toast d'ouverture) ; vérifié par `grep` sans texte visible en dur dans `ui/` et par les tests existants en français
- [x] 1.2 Faire suivre la langue affichée par la date des réparations, les noms et l'ordre des pays et le tri des applications de lecture ; le pays proposé vient de la région du système ; vérifié par `./gradlew test`

## 2. Catalogues de langues

- [x] 2.1 Déplacer le français dans `values-fr/`, écrire l'anglais dans `values/`, déclarer `unqualifiedResLocale=en`, `generateLocaleConfig` et `localeFilters` ; vérifié par `assembleRelease` et le `localeConfig` présent dans le manifeste fusionné
- [x] 2.2 Traduire en allemand, espagnol et italien (glossaire commun, libellés Android exacts, boutons de Tsuyu en français) et relire les quatre langues ; vérifié par `TranslationCatalogTest`
- [x] 2.3 Fixer la langue française des tests Robolectric (`fr-w960dp-h540dp-xhdpi`, `qualifiers=fr`) et mettre à jour la règle de `openspec/config.yaml` ; vérifié par `./gradlew test`

## 3. Réglage de langue

- [x] 3.1 `AppLanguage` et `LanguageState` (résolution, repli sur l'anglais) ; vérifié par `AppLanguageTest`
- [x] 3.2 `AppLanguageGateway` sur `LocaleManager`, état et action de langue dans `SettingsViewModel` (relecture à l'entrée d'une catégorie, choix identique ignoré) ; vérifié par `LanguageSettingsTest`
- [x] 3.3 Catégorie « Langue » avant À propos, lignes avec coche, focus sur la ligne retenue ; vérifié par `LanguageSettingsTest` et `LauncherSettingsTest`
- [x] 3.4 Mettre à jour le README (langues, réglage) et AGENTS.md (langue UI) ; vérifié à la relecture du diff

## 4. Validation

- [x] 4.1 `openspec validate --all --strict` et `./gradlew test` passent
- [ ] 4.2 Sur la TV réelle en `assembleRelease` : langue de la TV suivie par défaut, choix de chaque langue avec retour du focus sur la ligne choisie, retour à « Langue de la TV », choix conservé après redémarrage de l'application, textes sans débordement dans les cinq langues, langue d'Android inchangée

## Workflow follow-up

- Archiver le change après le merge et la validation sur la TV (`openspec archive interface-languages`).
