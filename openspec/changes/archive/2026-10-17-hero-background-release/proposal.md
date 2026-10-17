# Proposal

## Why

Le lecteur vidéo du héro est libéré dès que le héro est masqué, y compris quand l'utilisateur descend simplement vers la grille : la libération d'ExoPlayer est bloquante et tourne sur le fil principal, au moment où l'accueil anime le défilement. À l'inverse, quand une autre app passe au premier plan, le lecteur du héro garde son décodeur vidéo et le diaporama continue de compter ses délais sans être vu.

## What Changes

- Héro masqué à l'intérieur du launcher (zone grille, page de réglages) : la lecture est interrompue sans libérer le lecteur.
- Launcher en arrière-plan : le lecteur est libéré, ce qui rend le décodeur vidéo à l'app au premier plan ; le défilement automatique et les délais du héro sont suspendus.
- Retour au premier plan : la vidéo courante est rechargée sans compter comme un échec ; une vidéo lente dont la préparation a été interrompue redevient éligible au diaporama.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : « Fond de la zone grille » (scénario « héro hors écran ») ; « Préchargement et mémoire » (scénario « économie de ressources », scénarios « launcher en arrière-plan » et « retour au premier plan après libération »).

Dépend de `artwork-tile-decode` et `hero-visual-retry`, qui modifient aussi « Préchargement et mémoire » (repris tels quels). Compatible avec `settings-destination`, qui met la vidéo du héro en pause à sa position sous la page de réglages.

## Impact

Cycle de vie du lecteur et minuteurs du héro (`HeroStage`, `HeroPlayer`), tests Compose. Aucune dépendance ajoutée, aucun changement de rendu au premier plan.

## Non-goals

- Garder la vidéo préparée pendant que le launcher est en arrière-plan.
- Changer le comportement du lecteur sous la page de réglages : porté par `settings-destination`.
