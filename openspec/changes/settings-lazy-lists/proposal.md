# Proposal

## Why

Les listes d'apps sources, d'éléments cachés et de licences sont déjà des `LazyColumn`. Leur conteneur et leur gestion de révélation/focus sont dispersés, avec des demandes de focus qui peuvent viser une ligne non composée. Le propriétaire demande un socle commun pour les longues listes, sans abstraction parallèle ni changement de présentation.

## What Changes

- Regrouper le conteneur lazy et la révélation d'une cible par clé dans un composant de réglages commun aux trois listes.
- Faire défiler et attendre l'attachement avant de demander le focus ; garder les règles métier de repli chez leurs propriétaires existants.
- Préserver les blocs hétérogènes d'À propos, les clés, les testTags, les hauteurs variables et les états des consommateurs.
- Vérifier les longs jeux de données et les suppressions hors fenêtre sans ajouter de seuil « petite/grande liste » ni de seconde autorité de scroll.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

Aucune modification de contrat : `skip_specs: true`. Les capabilities `settings`, `ui-testing` et `self-update` restent les sources de vérité pour le rendu, les données, les états et les replis. `launcher-shell` n'est pas modifiée. Les adaptations techniques des tests seront limitées aux hypothèses de composition, sans affaiblir les assertions métier.

## Impact

`SourcesContent`, `HiddenContent`, `AboutContent`, états de listes de `SettingsScreen`, helpers de focus/révélation et tests de longues listes/licences. Les trois consommateurs conservent leur modèle, leur tri et leur chargement. Aucun backend, permission ou dépendance nouvelle.

Base d'implémentation : #49 pour les schedulers et attentes déterministes. `settings-destination` est indépendant ; les deux familles doivent conserver un état de liste détenu par la destination. Les panneaux P7 (`p7-default-launcher`) et la mise à jour restent inchangés ; aucun delta ouvert n'est réécrit.

## Non-goals

- Déclarer les licences non virtualisées : elles le sont déjà.
- Créer un framework de formulaires, un schéma universel de réglages ou une pagination réseau : hors de cette phase.
- Factoriser toutes les lignes ou modifier les petits menus à deux options : hors périmètre ; réutiliser `PreferenceRow` si nécessaire, sans nouvelle famille de composants de lignes.
- Changer les états de chargement du parseur de licences, les règles de mise à jour ou le rendu du QR : autres corrections éventuelles, pas ce refactoring.

## Open Questions

Aucune décision produit ouverte. Le composant gère uniquement le scroll et les cibles composées ; il ne décide ni du tri, ni des actions, ni du repli métier.
