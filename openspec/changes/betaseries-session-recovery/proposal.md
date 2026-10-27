# Proposal

## Why

Une panne temporaire du Keystore peut supprimer le jeton BetaSeries, et un échec de chargement de l'identité affiche « Connecter BetaSeries » alors que la session est conservée. Un refus HTTP générique peut aussi bloquer les reprises comme si le jeton avait réellement expiré.

## What Changes

- Conserver le jeton et son fichier lors des erreurs temporaires du stockage sécurisé ; réserver leur suppression aux pertes définitives confirmées.
- Distinguer restauration de session, absence de session et refus explicite du jeton ; proposer un réessai avec la session conservée après une panne du service.
- Ne pas déduire une expiration du seul statut HTTP 401/403 ni d'une réponse non JSON ; conserver les délais et reprises bornés existants.
- Garder l'erreur de restauration visible sur l'accueil et dans les réglages, sans lancer d'association ni révoquer le jeton au réessai.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `betaseries-integration` : restauration du stockage et qualification des refus d'accès dans « Fin de session BetaSeries » et « Bornes réseau et délais ».
- `settings` : « Réglages du suivi de visionnage » distingue une session à restaurer d'une session absente.
- `launcher-shell` : « Indicateur d'indisponibilité du suivi » reste visible si l'identité n'a pas encore pu être récupérée.
- `ui-testing` : couverture des pertes de session et des réessais sans nouvelle association.

## Impact

Stockage Keystore, état et contrôleur de suivi, client API BetaSeries, réglages, indicateur de l'accueil, ressources des cinq langues, tests JUnit et Compose TV, README. Aucune nouvelle dépendance.

## Dependencies

Part de `main` après P9 et les langues de l'interface. Le change ouvert `tv-resource-budget` ne modifie aucune des exigences touchées : aucun ordre d'archivage imposé entre ces deux changes.

## Non-goals

- Refaire l'association OAuth, les lecteurs, les mutations ou le cache du compte.
- Ajouter une boucle de synchronisation au repos ou réémettre des mutations après une panne.
- Attribuer une cause certaine à l'incident de la TV sans son erreur d'origine.

## Questions ouvertes

Aucune décision produit restante : les trois corrections ont été acceptées par le propriétaire. La validation finale sur TV reste à effectuer après les tests de fin de correctif.
