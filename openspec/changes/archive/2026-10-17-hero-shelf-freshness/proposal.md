# Proposal

## Why

Le héro et le Top Shelf ne relisent le TV Provider qu'au retour au premier plan : un programme retiré ou modifié par son app pendant que l'accueil est affiché reste à l'écran, avec son ancien titre et son ancien visuel, jusqu'à la prochaine reprise. Les ensembles de visuels vérifiés ne font que grossir pendant la session. Un visuel remplacé par l'app sous la même URL n'est jamais relu : le cache disque le sert indéfiniment. Le propriétaire veut que le héro et le Top Shelf ne montrent que ce que l'app publie encore, avec un rafraîchissement automatique borné.

## What Changes

- Le héro et le Top Shelf relisent le provider quand il signale un changement (`ContentObserver` partagé avec la rangée Up Next) et au plus tard toutes les 15 minutes tant que l'accueil est au premier plan ; rien en arrière-plan. La relecture à la reprise passe par cette observation.
- Un programme qui n'est plus publié disparaît du héro (y compris s'il est affiché : passage par fondu au programme qui prend sa place) et du Top Shelf ; un programme modifié met à jour ses textes et son visuel.
- Les ensembles de visuels vérifiés ne gardent que les visuels encore publiés ; un visuel injoignable est revérifié à chaque relecture du héro, y compris celles déclenchées par le provider ou le délai de 15 minutes.
- Une image servie depuis le cache disque depuis plus d'un jour est relue depuis le réseau avant d'être affichée ; la version en cache reste utilisée si le réseau échoue.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : « Contenu héro TV Provider » (rafraîchissement, retrait, modification), « Contenu du panneau » (affiche retirée), « Préchargement et mémoire » (scénarios « visuel injoignable » et « visuel rejeté » alignés sur la relecture automatique, scénario « fraîcheur du cache disque »).

Dépend de `artwork-tile-decode`, `hero-visual-retry`, `hero-background-release`, `virtualized-home-grid` et `visual-header-validation`, qui modifient aussi « Préchargement et mémoire » (repris tels quels) : les archiver avant celui-ci. `hero-shuffle-image-provider`, non implémenté, modifie « Contenu héro TV Provider » sur un autre point (ordre) : ce change part du texte courant (ordre déterministe) et le dernier archivé des deux reprend le texte de l'autre.

## Impact

Observation du provider partagée dans la couche données, `HomeViewModel` (observation, relecture périodique, purge), `HeroStage` (programme affiché retiré), intercepteur Coil de fraîcheur du cache disque, tests JUnit. Aucune dépendance ajoutée.

## Non-goals

- Relire le provider ou les images quand le launcher est en arrière-plan.
- Requêtes conditionnelles (ETag) : les apps sources ne publient pas d'en-têtes de cache fiables.
- Rafraîchir les bannières d'apps : elles suivent déjà les diffusions de paquets.
