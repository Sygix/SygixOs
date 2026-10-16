# Proposal

## Why

Les réglages sont actuellement composés au-dessus de l'accueil dans `LauncherHome`. Le propriétaire a choisi une destination distincte : retirer l'accueil de la composition pendant les réglages et conserver son état utile au retour, avec le même rendu et le contrat existant de retour au héro.

## What Changes

- Introduire un hôte de destinations accueil/réglages exclusives dans l'Activity existante.
- Démonter l'UI d'accueil et ses effets pendant les réglages, sans annuler les services applicatifs ni le comptage partagé nécessaire aux réglages.
- Conserver les données et ancres utiles hors des compositions démontées ; revenir au héro conformément à « Page de réglages » et conserver l'ancre de grille pour une descente ultérieure.
- Maintenir les panneaux du launcher système au-dessus de leur destination, les retours d'écrans système et le traitement de Home au premier plan.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `settings` : exclusivité de la destination et conservation du contrat existant d'entrée/sortie.
- `launcher-shell` : activité des consommateurs propres à l'accueil pendant son démontage, état de session conservé.
- `ui-testing` : tests de l'exclusivité, des durées de vie et de la navigation entre destinations.

## Impact

Hôte Compose de `MainActivity`/`HomeScreen`, `LauncherHome`, `SettingsScreen`, coordination de navigation, état du héro/grille et activation des effets de `HomeViewModel`. Une seule Activity ; pas de bibliothèque de navigation ajoutée pour deux destinations ni de nouvelle préférence.

Base d'implémentation : corrections #49, #50, #55 et #56 intégrées pour l'isolation des tests, la visibilité et l'annulation des travaux. Les contrats P7 viennent de `p7-default-launcher`, livré mais encore ouvert : conserver ses panneaux et ses séquences applicatives. Ce change ne réécrit pas ses exigences. `virtualized-home-grid` et `settings-lazy-lists` sont compatibles et non requis.

## Non-goals

- Nouvelle Activity, nouveau design, nouvelle catégorie, nouvelle transition visuelle : hors périmètre.
- Réinitialiser le catalogue, rejouer le splash, perdre le contenu sélectionné ou effacer les ancres : exclu du contrat.
- Virtualiser la grille et factoriser les listes des réglages : familles indépendantes.

## Open Questions

Aucune décision produit ouverte. La destination distincte et le maintien du rendu et du retour actuel ont été validés par le propriétaire ; les états conservés et leurs durées de vie sont explicités dans le design.
