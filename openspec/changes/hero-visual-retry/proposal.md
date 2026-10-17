# Proposal

## Why

Un visuel du héro ou du panneau Top Shelf en échec n'est jamais redemandé pendant la session, quelle que soit la cause. Une coupure réseau passagère ou un serveur momentanément indisponible écarte donc des programmes jusqu'au redémarrage du launcher. À l'inverse, la vérification des affiches d'une app n'a lieu qu'une fois par app : interrompue ou devenue obsolète après un rechargement des programmes, elle n'est jamais reprise.

## What Changes

- Un visuel rejeté définitivement (trop étroit, erreur HTTP 4xx, erreur de décodage) n'est jamais redemandé pendant la session.
- Un visuel injoignable pour une raison réseau (erreur d'entrée-sortie, erreur HTTP 5xx) est vérifié de nouveau au prochain rafraîchissement du héro (retour à l'accueil) ou, pour une affiche du Top Shelf, au prochain focus prolongé sur son app.
- La vérification du Top Shelf porte sur les affiches candidates de l'app : une vérification interrompue reprend, de nouvelles affiches candidates sont vérifiées, une affiche déjà vérifiée ne l'est pas deux fois.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : « Diaporama héro » (visuels en échec) ; « Préchargement et mémoire » (scénario « validation à la demande » et scénarios « validation interrompue ou nouvelles affiches », « visuel injoignable », « visuel rejeté »).

Dépend de `artwork-tile-decode`, qui modifie aussi « Préchargement et mémoire » (scénario « taille de décodage », repris tel quel).

## Impact

Vérification des visuels (distinction des échecs passagers), rafraîchissement du héro et vérification du Top Shelf dans le ViewModel de l'accueil, tests JUnit. Aucune dépendance ajoutée, aucun changement de rendu.

## Non-goals

- Retenter une vidéo d'aperçu en échec : elle reste écartée pendant la session.
- Retenter un visuel injoignable sans action de l'utilisateur (minuteur, surveillance du réseau).
