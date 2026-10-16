# Design

## Context

Voir `proposal.md`. `HomeScreen` collecte déjà les états de deux features ; `LauncherHome` possède `settingsOpen`, les zones et leurs focusers. `SettingsScreen` est ajouté à son `Box` avec un z-index. Les ViewModels sont attachés à l'Activity, mais sélection du héro, ancres et erreurs visuelles vivent en partie dans les compositions. Les panneaux P7 s'appuient sur un contrôleur applicatif dont les séquences continuent après fermeture.

Sources vérifiées : états `HomeState`, `SettingsState` et `LauncherSystemState`, clés de programmes/apps, préférences DataStore et compteur d'intents Home. Aucun fournisseur, champ système ou permission nouveau. Les compteurs commencent à l'accueil et continuent pendant les réglages selon `settings`, à partir de leur observation unique.

## Goals / Non-Goals

**Goals:** exclusivité réelle, effets correctement possédés, conservation d'état utile et retour au héro sans nouveau splash.

**Non-Goals:** nouvelle Activity, navigation à pile illimitée, nouvelle bibliothèque, transition ou apparence ; arrêt de tous les services avec une composition.

## Decisions

### D1 — un hôte au-dessus des deux destinations

Extraire un hôte Compose dans l'Activity existante, avec une destination accueil/réglages et les actions de navigation. Les branches sont exclusives ; aucun `AnimatedContent` qui conserverait longtemps les deux UI, aucune copie transparente de l'accueil. Deux destinations ne justifient pas l'ajout d'un framework de navigation. Les tags `launcher-destination-home` et `launcher-destination-settings` identifient leurs racines, avec tous les tags de contrôles actuels conservés.

Déplacer l'observation des demandes Home et la coordination des panneaux P7 à cet hôte : elles doivent fonctionner même sans `LauncherHome`. Ne pas dupliquer le booléen de destination dans le ViewModel et dans Compose. L'état léger de navigation est sauvegardable ; les effets métier restent dans leurs propriétaires.

### D2 — état conservé, ressources libérées

Les ViewModels restent dans le store de l'Activity, obtenus hors de la branche de destination. Conserver les petits états nécessaires via un détenteur sauvegardable : identité du programme héro, ancres et dernière cible de grille, état utile des listes. Les erreurs visuelles de session restent au niveau de leur propriétaire de session ; les résultats lents dont la préparation est annulée redeviennent éligibles selon les règles existantes. Ne conserver ni lecteur, TextureView, FocusRequester, bitmap ou arbre Compose dans un état sauvegardé.

Fermer les réglages impose la zone héro et le scroll de page initial, mais ne supprime pas l'ancre profonde mémorisée pour une descente ultérieure. La sélection héro encore valide est restaurée ; une source décochée ou une cible disparue utilise le repli existant. Une réouverture explicite des réglages garde leur entrée actuelle sur la première catégorie ; un simple retour au premier plan conserve la catégorie active et ferme la liste de position selon son contrat.

Le splash est un état de session/Activity, pas un effet de montage de destination. Le remontage de l'accueil ne réclame pas un nouveau démarrage à froid.

### D3 — trois classes de durées de vie

- Rendu d'accueil : lecteur, animations, verre, observation Up Next et préparation visuelle ne sont actifs que pour la destination visible au premier plan. Ajouter une activation explicite aux producteurs concernés ; l'arrêt de collecte Compose et le délai de `WhileSubscribed` ne suffisent pas à couper immédiatement les effets déclenchés dans les projections.
- Données partagées : catalogue et compteurs nécessaires aux réglages restent détenus hors de l'UI d'accueil. Déplacer le propriétaire de l'unique observation des compteurs au niveau commun, sans seconde query ni second observer. Elle continue dans les réglages au premier plan et s'arrête en arrière-plan.
- Services applicatifs : mise à jour, opérations P7 et état système conservent leurs scopes et leurs contrats. Détacher les consommateurs visuels ne doit pas annuler une configuration en cours.

Au retour, l'activation de l'observation Up Next et le rafraîchissement d'Activity sont coalescés. Les préférences changent déjà dans les repositories ; remonter une destination ne doit pas forcer un rechargement supplémentaire de toutes les sources. Les `Result` et replis restent chez les adaptateurs, pas dans l'hôte de navigation.

### D4 — panneaux au-dessus de la bonne destination

Les panneaux P7 sont superposés à la destination active conformément à `p7-default-launcher` : parcours automatique au-dessus de l'accueil, panneau ouvert depuis les réglages au-dessus des réglages. Ne pas rouvrir le home pour dessiner le fond d'un panneau de réglages. Un Home reçu au premier plan utilise le chemin existant de fermeture des panneaux et annulation de déplacement avant de choisir l'accueil ; il ne se réduit pas à changer une enum. Un Home reçu depuis une autre app conserve les règles de retour existantes.

### D5 — vérification de la séparation

Compter montage/disposition des racines et démarrage/arrêt des producteurs avec doubles contrôlés. L'absence sémantique du home et ces compteurs prouvent l'exclusivité ; une alpha nulle ne la prouve pas. Rejouer tous les parcours P7 et les changements de sources/visibilité/position depuis les réglages, puis vérifier l'état au remontage. Mesurer CPU/mémoire et activité au repos avec les mêmes fixtures avant/après sur émulateur ; validation finale du rendu sur TV après release par le propriétaire.

## Risks / Trade-offs

- Perte de sélection au démontage → extraire les identités et erreurs de session avant de déplacer les branches ; tester programme exclu et disparition de tuile.
- Panneau dont les handlers vivaient dans `LauncherHome` → remonter la coordination avant de rendre les destinations exclusives et conserver les tests P7.
- Arrêt accidentel des compteurs dans les réglages → compteur d'abonnements et émissions contrôlées entre destinations.
- Gain mémoire au prix d'une reprise froide → conserver données/ancres et caches bornés, pas l'arbre graphique ; comparer le délai de retour et le pic mémoire.

## Migration Plan

Une PR dédiée après validation et intégration des bases de la proposal. Pas de migration persistée ; rollback par réversion de l'hôte et de la répartition des états. Les réglages et la grille restent compatibles avec leurs familles de refonte indépendantes.

Archive : après merge et validation TV, sans modifier les blocs P7 ouverts. `p7-default-launcher` peut être archivé avant ou après : ce change ne touche pas ses deltas. Les deux autres nouveaux changes n'ont pas d'ordre d'archive imposé.

## Open Questions

Aucune décision produit ouverte. Le retour affiché est toujours le héro ; « restaurer l'état utile » ne signifie pas rouvrir directement une ancienne zone grille.
