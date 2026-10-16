# Design

## Context

Voir `proposal.md`. `SourcesContent`, `HiddenContent` et `AboutContent` ont déjà une `LazyColumn` et un `LazyListState` fourni par `SettingsScreen`. Sources utilise `ListReveal` ; cachés demande parfois le focus après une frame sans garantir que la cible hors fenêtre soit composée. À propos regroupe plusieurs contrôles et le QR dans un seul item de liste, puis une ligne par licence (`uniqueId`).

Sources vérifiées : lignes sources identifiées par package ; `HiddenEntry` identifiées par `key` (actions, intertitre, apps, contenus et dépliage) ; modèle `Library` avec `uniqueId`, nom, version et ensemble de licences du fichier `R.raw.aboutlibraries`. Le chargement de ce modèle n'est pas une pagination ; son coût est distinct de la composition des lignes. Ce change ne le remplace pas et ne crée aucune lecture système/réseau.

## Goals / Non-Goals

**Goals:** un conteneur lazy commun et un chemin fiable révélation/attachement/focus, compatible avec des contenus hétérogènes et des hauteurs variables.

**Non-Goals:** règle générique de tri/repli, formulaire universel, préchargement de toutes les vignettes, modification des statuts de licences ou de mise à jour, changement des petites listes fixes.

## Decisions

### D1 — une coquille lazy, sans seuil automatique

Extraire un `SettingsLazyList` recevant état de liste, modifier/tag et contenu `LazyListScope`. Il fournit la taille bornée et une seule autorité de scroll ; chaque consommateur décrit ses items/headers avec clés et types stables. La virtualisation est systématique pour ces listes, sans branche `Column`/`LazyColumn` selon leur longueur : un seuil changerait le cycle de vie et le focus lors d'un ajout d'élément.

Garder les slots d'en-tête et le contenu de ligne chez les consommateurs : les espacements, tags, styles et `UpdateBlock`/QR restent identiques. Ne pas ajouter un renderer de modèle universel ; ne pas introduire une autre famille de lignes en concurrence avec `PreferenceRow`.

### D2 — révélation par identité, indépendamment du rendu des lignes

Associer une cible logique à la clé d'item lazy qui la contient. Plusieurs contrôles peuvent partager un item (`UpdateBlock`) ; ne pas les découper pour satisfaire le helper. Résoudre l'index à partir des clés et métadonnées courantes, faire défiler si nécessaire, attendre que le requester soit attaché, puis focaliser et contrôler le résultat. Les nouvelles demandes remplacent les anciennes ; une catégorie inactive ou destination quittée annule la demande.

Le registre de requesters ne garde que les cibles composées, et ses entrées sont retirées à la disposition. Le détenteur d'état connaît les clés/indices, pas les vues d'une liste entière. Réutiliser `ListReveal` et les règles pures existantes pour le repli ; la coquille ne décide pas de l'app/du contenu à choisir après suppression.

### D3 — propriétaires conservés

`SettingsScreen` reste propriétaire des états de scroll par catégorie. Les ViewModels gardent le tri figé, les groupes et les résultats métier ; la coquille ne charge pas les licences ni les compteurs et n'interprète pas une erreur comme une liste vide. Les en-têtes/états existants sont des items normaux ; les éléments non focusables ne deviennent pas des étapes de navigation.

Les tests gardent les tags existants, dont `settings-sources`, `hidden-pane`, `settings-about` et `license-row-<uniqueId>`. Les chaînes éventuellement extraites de code sont placées dans les ressources françaises sans changer leur texte.

### D4 — preuves utiles

Fixtures longues d'apps, de contenus cachés dépliés et de licences, avec identités fictives stables : vérifier présence de la fenêtre, absence initiale de lignes éloignées, navigation à la fin puis au début, retours catégorie/app, disparition du focus et cible de repli initialement hors fenêtre. Ne pas rendre toutes les lignes pour faciliter les tests.

Comparer le nombre de lignes attachées, le coût de première composition et les allocations à viewport identique. Aucun gain majeur de virtualisation n'est présumé : les trois listes étaient déjà lazy. Le bénéfice attendu est surtout une responsabilité unique de révélation/focus et la suppression des trois copies de conteneur. Le chargement du JSON complet est mesuré séparément et n'est pas présenté comme borné par le viewport.

## Risks / Trade-offs

- Wrapper sans valeur → la même coquille et le même helper de révélation sont réellement utilisés dans les trois consommateurs, sans copie locale du mécanisme.
- Cible disparue pendant le scroll → résoudre de nouveau via la règle métier du consommateur avant toute demande, sans boucle d'attente infinie.
- Régression du QR ou des hauteurs de ligne → conserver le bloc hétérogène existant et ses tests de position.
- Registre grossissant pendant les parcours → vérifier le nettoyage des cibles disposées et garder les identités sans conserver de requesters hors fenêtre.

## Migration Plan

PR de refactoring après validation : migrer les trois consommateurs et leurs tests, sans changer le schéma des états. Rollback par réversion, aucune migration de préférences. `settings-destination` peut être appliqué indépendamment, l'état de liste restant au niveau de la destination ; intégrer les corrections #49 avant vérification.

Archive : aucune synchronisation de delta (`skip_specs: true`). Clore après implémentation, merge et passe TV du propriétaire ; aucun ordre d'archive imposé avec les autres changes.

## Open Questions

Aucune décision produit ouverte ; aucune nouvelle erreur visible ni politique de tri n'est décidée par ce refactoring.
