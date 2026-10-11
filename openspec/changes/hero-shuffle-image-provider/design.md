# Design

## Context

Constats sur la TV de référence avec v0.1.0-rc.2 (voir `proposal.md`). Les visuels du héro, du Top Shelf et d'À suivre viennent du TV Provider : leurs URI peuvent être `https://` ou `content://` vers le fournisseur d'une application tierce (Jellyfin : `ImageProvider`, qui télécharge l'image depuis son serveur).

## Decisions

### D1. Récupérateur d'images à référence instable
Un récupérateur Coil dédié prend en charge les URI `content://` dont l'autorité n'appartient ni à SygixOs ni au système (le récupérateur par défaut reste utilisé pour les autres). Il acquiert un `ContentProviderClient` instable. L'appel bloquant `openTypedAssetFile(uri, "image/*", null, signal)` s'exécute sur un pool de workers borné, jamais directement dans la coroutine du chargeur : `withTimeout` seul ne peut pas interrompre un appel Binder synchrone. Un échéancier indépendant complète le résultat du chargement en échec après 8 s (à valider par la tâche 3.1), puis déclenche `CancellationSignal.cancel()` hors du worker bloqué. Le worker reste occupé jusqu'à la sortie réelle de l'appel et n'est pas réutilisé avant ; le pool ne crée pas de threads sans borne et refuse rapidement une requête s'il est saturé. Le worker ferme le descripteur et le client après la sortie de l'appel ; si un descripteur arrive après le délai, il est fermé sans être remis à Coil. Les erreurs `DeadObjectException`, `RemoteException`, `SecurityException`, `FileNotFoundException` et le délai deviennent un échec de chargement. Toute autre lecture de fournisseur tiers ajoutée plus tard suit la même règle ; les requêtes au TV Provider (fournisseur système) ne changent pas.
Alternative écartée : garder `ContentResolver.openInputStream`, qui passe à une référence stable dès que le descripteur est ouvert.

### D2. Ordre aléatoire du héro
Le tri du héro (domaine) reçoit une graine tirée au démarrage du processus (`Random` injectable pour les tests). L'ordre de session est mémorisé dans le domaine par identifiant de programme : les programmes déjà présents gardent leur rang ; un nouveau programme est inséré à une position aléatoire après le programme affiché ; un programme disparu est retiré. Rien n'est persisté : un nouveau processus tire un nouvel ordre.

## Risks / Trade-offs

- [Délai trop court sur un serveur lent] → Valeur à valider sur la TV (tâche 3.1) ; une image en échec n'est pas redemandée pendant la session (règle existante du « Diaporama héro »).
- [Mélange qui place un programme sans visuel en premier] → Règle existante du « Diaporama héro » : passage au programme suivant dont le visuel est prêt.

## Migration Plan

Aucune donnée migrée.
