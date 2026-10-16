# Design

## Context

Voir `proposal.md`. `SourcesContent`, `HiddenContent` et `AboutContent` ont déjà une `LazyColumn` et un `LazyListState` fourni par `SettingsScreen`. Sources fait défiler via `ListReveal` ; Cachés demande le focus après une frame sans garantir que la cible hors fenêtre soit composée. À propos regroupe plusieurs contrôles et le QR dans un seul item de liste, puis une ligne par licence (`uniqueId`). La grille de l'accueil (`TileFocus`) tient de son côté un registre package → `FocusRequester` ; elle reste une `Column` défilante dont les rangées hors fenêtre deviennent des espaceurs, donc la révélation y est un scroll de page, pas un `scrollToItem`.

Sources vérifiées : lignes sources identifiées par package ; `HiddenEntry` identifiées par `key` ; modèle `Library` avec `uniqueId` depuis `R.raw.aboutlibraries`. Le chargement de ce modèle n'est pas une pagination ; ce change ne le remplace pas et ne crée aucune lecture système/réseau.

## Goals / Non-Goals

**Goals :** une primitive de focus commune (registre de cibles composées + révélation puis focus) hors du package réglages, et une résolution de ligne commune aux trois listes des réglages.

**Non-Goals :** règle générique de tri/repli, formulaire universel, préchargement de vignettes, modification des statuts de licences ou de mise à jour, changement des petites listes fixes.

## Decisions

### D1 — une primitive de focus dans le design system

`FocusTargets` (package `core.designsystem`) : `bind(key, requester?)` enregistre, le temps de la composition de l'élément, le `FocusRequester` d'une cible logique (il retire l'entrée à la disposition) ; `focus(key)` demande le focus à la cible si elle est composée et renvoie le résultat ; `reveal(key) { … }` exécute d'abord la lambda suspendue de l'appelant qui rend la cible composée (défilement d'une `LazyColumn`, scroll de page ou fenêtre de rangées de la grille d'accueil), puis focalise. Pas de dépendance à `LazyListState`, pas de registre global, pas d'attente ni de délai : si la cible n'est pas composée après révélation, l'appel renvoie `false` et l'appelant applique son repli.

La primitive ne connaît ni les clés d'items ni les positions : chaque appelant résout l'index à partir de son propre ordre. Elle ne porte pas d'état « actif » : l'appelant ne lance une révélation que si sa zone a le focus. Aucune API réservée aux tests.

### D2 — une résolution de ligne commune aux réglages

Les trois listes restent des `LazyColumn` (un wrapper de conteneur n'économiserait rien). Elles ont un item d'en-tête avant leurs lignes : `FocusTargets.revealRow(list, key, position)` ajoute ce décalage à la position de la ligne dans l'ordre du consommateur, fait défiler la liste si l'item n'est pas visible (`scrollToItem` remesure de façon synchrone, l'item est donc composé et lié au retour) puis focalise la cible. Sources passe la position du package focalisé dans `order`, Cachés celle de l'entrée demandée, À propos la position du bloc de mise à jour (plusieurs cibles partagent cet item : `update-check`, `update-install`). Une position négative renvoie `false`.

Seules les cibles qu'une révélation peut viser sont liées : les lignes de licences n'en font pas partie.

### D3 — propriétaires conservés

`SettingsScreen` reste propriétaire des états de scroll par catégorie. Les ViewModels gardent le tri figé, les groupes et les résultats métier. Les en-têtes et états existants sont des items normaux. Les tests gardent les tags existants, dont `settings-sources`, `hidden-pane`, `settings-about` et `license-row-<uniqueId>`. Les chaînes extraites du code vont dans les ressources françaises sans changer leur texte.

### D4 — preuves

Tests Compose à taille TV : repli de Cachés vers une ligne initialement hors fenêtre (échoue sans révélation), source déplacée hors fenêtre par un retri, retour d'écran système dans À propos avec le bloc de mise à jour au-dessus de la fenêtre des licences ; primitive : révélation d'un item non composé puis focus, échec sur une cible non composée. Les suites existantes de D-pad, groupes, tri et géométrie du QR conservent leurs assertions.

## Risks / Trade-offs

- Abstraction sans valeur → `FocusTargets` et `revealRow` remplacent `ListReveal`, le registre local de Cachés et le requester d'installation d'À propos ; aucun wrapper de conteneur.
- Cible disparue entre la demande et la révélation → `focus` renvoie `false`, le consommateur applique son repli existant.
- Révélation avant la première mesure de la liste → la position est appliquée au premier layout, le focus n'est pas demandé ; aucun cas de ce type dans les réglages (les demandes suivent une action sur une liste déjà affichée).

## Migration Plan

Refactoring sans changement de schéma d'état ; rollback par réversion. La grille de l'accueil migre vers `FocusTargets` dans `virtualized-home-grid`.

Archive : aucune synchronisation de delta (`skip_specs: true`).

## Open Questions

Aucune.
