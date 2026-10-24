# Proposal

## Why
SygixOs n'existe qu'en français : sur une TV réglée dans une autre langue, tout l'accueil, les réglages et le parcours de configuration restent en français. L'architecture i18n est prête (presque tous les textes sont des ressources), il manque les traductions, le choix de la langue et quelques textes encore écrits en dur.

## What Changes
- L'interface est traduite en anglais, allemand, espagnol et italien, en plus du français. Les traductions sont relues pour sonner naturelles sur une TV et reprennent les libellés exacts des écrans Android cités (options pour les développeurs, débogage USB, accessibilité…).
- Par défaut, SygixOs suit la langue de la TV : la première langue du système qui est traduite est utilisée, sinon l'anglais.
- Nouvelle catégorie de réglages « Langue », avant « À propos » : « Langue de la TV » (choix par défaut, avec la langue utilisée) puis chaque langue sous son propre nom. Le choix ne s'applique qu'à SygixOs : la langue de la TV et des autres applications ne change pas.
- Le choix est conservé par Android (langue propre à l'application, Android 13+) et survit au redémarrage et aux mises à jour.
- Les derniers textes écrits en dur (dock vide, menu d'une app, déplacement, héro, À propos, compteur de programmes, toast d'ouverture) passent en ressources ; la date des réparations, les noms de pays et le tri des applications de lecture suivent la langue affichée.
- Les textes cités par les specs restent ceux du catalogue français ; les autres langues en portent la traduction.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `settings` : « Page de réglages » gagne la catégorie « Langue » ; nouvelle exigence « Langue de l'interface » (détection, choix, persistance, textes traduits).
- `ui-testing` : les tests Compose à taille TV fixent aussi la langue française (`fr-w960dp-h540dp-xhdpi`) ; nouvelle exigence « Couverture des langues ».

`launcher-shell`, `self-update`, `tsuyu-access`, `watch-tracking`, `up-next` : comportement inchangé, leurs textes suivent la langue de l'interface par la règle générale de `settings`.

Dépendances : aucune ; `tv-resource-budget` (ouvert) ne touche pas les mêmes exigences.

## Non-goals
- Pas d'autre langue pour l'instant (portugais, néerlandais, langues non latines, écriture de droite à gauche) : une langue s'ajoute plus tard avec ses ressources et une entrée dans la liste.
- Pas de changement de la langue du système ni des autres applications (il faudrait un accès privilégié).
- Tsuyu reste en français : ses boutons cités dans le parcours gardent leur libellé français, suivi de leur sens.
- Les données externes (titres, résumés, services BetaSeries) restent dans la langue de leur source.

## Questions ouvertes
Aucune.

## Impact
- Ressources : `values/` devient l'anglais (langue de repli), le français passe dans `values-fr/`, ajout de `values-de/`, `values-es/`, `values-it/` ; `localeConfig` généré par AGP et filtre des langues embarquées.
- Code : `AppLanguage` (domaine), `AppLanguageGateway` (`LocaleManager`), état et action de langue dans `SettingsViewModel`, `LanguageContent` dans les réglages.
- Tests : qualifier `fr` par défaut, tests du volet Langue, de la résolution et de la complétude des traductions.
