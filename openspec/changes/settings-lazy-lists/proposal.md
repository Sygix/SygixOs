# Proposal

## Why

Les listes d'apps sources, d'éléments cachés et de licences sont déjà des `LazyColumn`. Leur conteneur et leur gestion de révélation/focus sont dispersés, avec des demandes de focus qui peuvent viser une ligne non composée (repli de Cachés vers une ligne hors fenêtre). Le propriétaire demande un socle commun pour les longues listes, réutilisable par la grille d'apps de l'accueil, sans abstraction parallèle ni changement de présentation.

## What Changes

- Ajouter au design system un registre de cibles de focus liées aux éléments composés (`FocusTargets`) avec « révéler puis focaliser », la révélation étant fournie par l'appelant ; indépendant des réglages et de `LazyListState`.
- Partager entre les trois listes des réglages la révélation d'une ligne par clé et position, avant toute demande de focus ; garder les règles métier de repli chez leurs propriétaires existants.
- Préserver les blocs hétérogènes d'À propos, les clés, les testTags, les hauteurs variables et les états des consommateurs.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

Aucune modification de contrat : `skip_specs: true`. Les capabilities `settings`, `ui-testing` et `self-update` restent les sources de vérité pour le rendu, les données, les états et les replis. `launcher-shell` n'est pas modifiée.

## Impact

`core/designsystem` (nouveau fichier de focus), `SourcesContent`, `HiddenContent`, `AboutContent`, tests de focus hors fenêtre. `ListReveal` est remplacé par la révélation partagée. Les trois consommateurs conservent leur modèle, leur tri et leur chargement. Aucun backend, permission ou dépendance nouvelle.

## Non-goals

- Créer un framework de formulaires, un schéma universel de réglages ou une pagination réseau : hors de cette phase.
- Factoriser toutes les lignes ou modifier les petits menus à deux options : hors périmètre.
- Changer les états de chargement du parseur de licences, les règles de mise à jour ou le rendu du QR.
- Migrer la grille de l'accueil : le change `virtualized-home-grid` réutilise la primitive, il n'est pas modifié ici.

## Open Questions

Aucune décision produit ouverte.
