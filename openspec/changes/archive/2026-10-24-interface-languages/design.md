# Design

## Context
Les textes sont presque tous des ressources Android dans `values/` (français). Une dizaine de textes restaient écrits en dur dans les composables (dock vide, menu d'une app, héro, À propos…), la date des réparations utilisait `Locale.FRENCH`, comme les noms de pays du suivi et le tri des applications de lecture. Les tests Robolectric vérifient des textes français sans fixer de langue. minSdk 34 : l'API de langue par application d'Android 13 (`LocaleManager`) est toujours disponible.

## Goals / Non-Goals

**Goals:**
- Une seule source de vérité pour la langue : le système, via `LocaleManager`.
- Aucun texte visible hors ressources, aucune locale codée en dur pour un format visible.
- Ajouter une langue = un dossier `values-xx` et une entrée de `AppLanguage`, vérifiée par un test.

**Non-Goals:**
- Pas de bibliothèque d'i18n ni d'AppCompat : l'API système suffit.
- Pas de préférence stockée par SygixOs (DataStore) : elle doublerait celle du système.

## Decisions

### Langue de repli : l'anglais dans `values/`
`values/` devient l'anglais et le français passe dans `values-fr/`. Android choisit alors la première langue de la TV qui a des ressources, sinon `values/`. Garder le français en `values/` aurait affiché du français sur une TV japonaise. `resources.properties` déclare `unqualifiedResLocale=en`, `generateLocaleConfig` publie la liste des langues au système (écran « Langue de l'application » quand la TV le propose) et `localeFilters` n'embarque que les cinq langues, y compris pour les bibliothèques.

### Choix par application avec `LocaleManager`
`AppLanguageGateway` (interface, aux frontières) lit `applicationLocales` (vide = langue de la TV) et `systemLocales`, et écrit `applicationLocales`. Android conserve le choix (redémarrage, mise à jour) et recrée l'activité avec la nouvelle configuration. Alternative écartée : stocker la langue dans DataStore et envelopper le `Context` dans `attachBaseContext`. Il faudrait couvrir chaque contexte (application, services, toasts) et on aurait deux sources de vérité.

### Résolution dans le domaine
`AppLanguage` liste les langues (code, nom dans sa langue) dans l'ordre alphabétique de ces noms. `resolve` donne la première langue de la TV qui est traduite, et `LanguageState.effective` applique le repli. Ce code pur est testé en JUnit ; c'est la même règle que la résolution des ressources Android, pour que le texte secondaire de « Langue de la TV » dise vrai.

### État dans `SettingsViewModel`
`SettingsState.language` vient de la passerelle. `SettingsViewModel` le relit à chaque entrée dans une catégorie, car il survit à la recréation de l'activité, par exemple après un changement de langue de la TV. Le choix met d'abord l'état à jour, puis appelle `select` ; un choix identique est ignoré, ce qui évite une recréation inutile. Après recréation, les réglages restent ouverts (`rememberSaveable`) sur la catégorie et le volet ; le focus du volet va à la ligne retenue, donc à la langue choisie.

### Formats suivant la langue
- La date des réparations utilise `getBestDateTimePattern(locale, "dMMM")` avec la locale de la configuration courante.
- Les noms de pays sont recalculés quand la locale de l'écran change. `TrackingViewModel` survit à la recréation, donc le volet lui transmet la locale.
- Le tri des applications de lecture utilise la locale par défaut.
- Le pays proposé au suivi vient de la locale du système, car une langue choisie seule (« de ») n'a pas de région.

### Traductions
Elles ont été traduites depuis le français par langue, puis relues avec un glossaire commun :
- les termes d'Apple TV pour l'accueil (Up Next / Als Nächstes / A continuación / Prossimi) ;
- les libellés exacts d'Android pour les écrans système cités ;
- le tutoiement là où Google et Apple le pratiquent (de, es, it).

Tsuyu sera traduit dans les mêmes langues : ses boutons cités sont traduits directement (Start / Turn on Tsuyu / Allow / Deny ; Starten / Tsuyu aktivieren / Zulassen / Ablehnen ; Iniciar / Activar Tsuyu / Permitir / Rechazar ; Avvia / Attiva Tsuyu / Consenti / Rifiuta), libellés que Tsuyu reprendra.

## Risks / Trade-offs
- [Le libellé d'un écran Android varie selon le constructeur] → On reprend les libellés AOSP ; ces textes indiquent déjà que le nom peut varier.
- [Allemand et italien plus longs] → Textes courts dans les traductions ; à vérifier sur la TV, dans les lignes et pilules les plus étroites.
- [Recréation de l'activité au changement de langue] → C'est rare et voulu. L'état des réglages est sauvegardé ; le héro reprend comme après un retour au premier plan.
- [Un texte ajouté en anglais sans traduction] → Le test de complétude échoue.
