# Design

## Context

Voir `proposal.md`. `LauncherHome` compose dans un même `BoxWithConstraints` la colonne défilante de l'accueil (héro, dock, capsule, grille) puis `SettingsScreen` au-dessus. Les effets de l'accueil sont déjà coupés pendant les réglages : `HeroStage` reçoit `visible = false` (Ken Burns, défilement automatique, délais de visuel arrêtés, vidéo arrêtée sans libérer le lecteur), les zones ne sont pas focusables et la capsule est retirée. Seul le dessin continue. Aucune source de données n'est ajoutée ou modifiée.

## Goals / Non-Goals

**Goals :** ne plus dessiner l'accueil sous les réglages, garder son état pour un retour immédiat, ne plus flouter l'accueil dans le verre des réglages.

**Non-Goals :** démontage, navigation, nouvelle transition, changement du rendu des réglages.

## Decisions

### D1 — accueil mesuré mais non placé

Un modificateur de mise en page mesure la colonne de l'accueil et ne la place que si les réglages sont fermés ; l'état est lu dans le bloc de placement, donc ouvrir ou fermer les réglages n'invalide que le placement. Un nœud non placé n'est pas dessiné, n'est pas éligible à la recherche de focus et sort de l'arbre d'accessibilité ; sa composition, ses `remember`, le lecteur, le verre et le scroll de page sont conservés. Ses nœuds restent dans l'arbre sémantique des tests : les tests vérifient donc qu'ils existent sans être affichés, et qu'aucun nœud hors de `settings-screen` ne prend le focus.

Alternatives écartées : démonter l'accueil (état à sauvegarder, deuxième chargement des visuels, retour lent) ; alpha nulle (le placement et la recherche de focus restent actifs).

### D2 — vidéo en pause plutôt qu'arrêtée

`HeroStage` reçoit `keepVideo` pendant les réglages : si la vidéo courante est chargée, elle est mise en pause (`park`) au lieu d'être arrêtée. Au retour, `show` sur le même élément ne relance pas la préparation et la première image est déjà rendue : pas de fondu d'entrée. La zone grille arrête la vidéo sans libérer le lecteur ; le passage en arrière-plan libère toujours le lecteur.

### D3 — verre des réglages sans sources d'accueil

La page de réglages est la source d'un `HazeState` qui lui est propre, fourni par `LocalHazeState` aux réglages et au panneau du launcher système tant qu'elle est ouverte : la liste déroulante de « Position d'À suivre » (fenêtre popup) et le panneau floutent la page affichée derrière eux au lieu des calques du héro et de la grille, qui ne sont pas dessinés. Aucune surface verre n'est placée dans la page elle-même : Haze refuse qu'un effet floute une source dont il descend.

### D4 — licences lues une fois

Le modèle de licences est lu sur le répartiteur IO au premier affichage d'« À propos » puis gardé en mémoire pour le processus ; les visites suivantes l'affichent dès la première image, sans état de chargement.

### Pistes écartées

- Un `LazyListState` par catégorie : chaque catégorie compose déjà sa propre liste, l'allocation est négligeable, et la position de défilement d'une catégorie déjà visitée deviendrait visible.
- Pilules de focus à progression unique : un rendu identique en cas d'interruption exige de mémoriser les couleurs de départ (plus de code que les quatre animations actuelles), et les couleurs de texte restent lues en composition ; une ombre constante à alpha nulle coûterait un dessin d'ombre par ligne au repos.

## Risks / Trade-offs

- L'accueil reste en mémoire et continue d'être mesuré quand son état change pendant les réglages → comme avant le change ; seul le dessin est retiré.
- Le décodeur vidéo reste alloué, en pause, pendant les réglages → libéré au passage en arrière-plan comme aujourd'hui.

## Migration Plan

Aucune donnée persistée. Retour arrière par réversion du modificateur de placement et du paramètre `keepVideo`.

## Open Questions

Aucune.
