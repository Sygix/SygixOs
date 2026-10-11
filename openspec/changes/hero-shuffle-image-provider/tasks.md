# Tasks

## 1. Images d'autres applications

- [ ] 1.1 Récupérateur Coil à référence instable (D1) enregistré dans l'`ImageLoader` pour les URI `content://` d'applications tierces ; vérifié par des tests JUnit avec un appel synchrone simulé qui reste bloqué : avant de libérer le double, le résultat échoue dans le délai borné et le signal est annulé depuis l'échéancier indépendant ; après libération, tout descripteur tardif et le client sont fermés exactement une fois sans publier d'image. Vérifier aussi le fournisseur qui ignore l'annulation (worker quarantiné, pool borné, requête suivante échouant rapidement), succès, `DeadObjectException`, erreur de sécurité et fichier absent.
- [ ] 1.2 Recherche dans le code de toute autre ouverture de fournisseur tiers par `ContentResolver` (images, icônes, logos) et passage par le même chemin ; vérifié par revue du diff.

## 2. Ordre aléatoire du héro

- [ ] 2.1 Tri du héro dans le domaine (D2) : graine par processus, ordre de session stable, insertion aléatoire après le programme affiché, retrait sans réordonner ; vérifié par des tests JUnit avec un `Random` injecté (deux graines donnent deux ordres, rechargement sans changement, ajout, retrait).

## 3. Vérification

- [ ] 3.1 `./gradlew test` vert, puis sur la TV réelle avec l'APK release : serveur Jellyfin injoignable, ouverture d'un contenu Jellyfin depuis le héro puis ANR de Jellyfin : SygixOs reste lancé, images en espace réservé ; délai de D1 confirmé ou ajusté ; héro dans un ordre différent après deux relances. Résultats consignés dans `design.md`, sans donnée personnelle.

## Workflow follow-up

- Après merge et validation sur la TV : `openspec archive hero-shuffle-image-provider`.
