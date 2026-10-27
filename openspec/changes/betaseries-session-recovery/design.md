# Design

## Context

Voir proposal.md pour le problème. `TrackingController` possède le jeton en mémoire ; `TrackingState.account` n'est rempli qu'après `members/infos`. Les réglages et l'accueil confondent aujourd'hui identité absente et session absente. `KeystoreTrackingSession` supprime le fichier sur toute `UnrecoverableKeyException`.

Sources vérifiées : le stockage est un fichier AES-GCM hors sauvegarde et une clé Android Keystore ; l'API fournit statut HTTP et `errors[].code`, mais un corps non JSON ne prouve aucune expiration. Dans Android 14, `AndroidKeyStoreProvider.loadAndroidKeyStoreKeyFromKeystore` enveloppe les erreurs Keystore autres que KEY_NOT_FOUND et KEY_PERMANENTLY_INVALIDATED dans `UnrecoverableKeyException` ; `KeyStore2` peut produire SYSTEM_ERROR après une perte de communication. Ces chemins sont vérifiés dans AOSP, pas observés sur la TV de référence.

- [AndroidKeyStoreProvider Android 14](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/keystore/java/android/security/keystore2/AndroidKeyStoreProvider.java#L368-L386)
- [KeyStore2 Android 14](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/keystore/java/android/security/KeyStore2.java#L104-L125)

## Goals / Non-Goals

**Goals:** appliquer les comportements acceptés avec un seul état de session, des frontières injectables et les délais existants.

**Non-Goals:** pas de cache de jeton supplémentaire, de migration cryptographique ou de synchronisation périodique. Les mutations restent sans reprise automatique.

## Decisions

### D1 — Clés et erreurs de stockage

Extraire la lecture, la création et la suppression des clés derrière une frontière injectable, pour exercer le vrai fichier chiffré en test. Une clé lue absente est une preuve distincte d'une exception de lecture. Conserver le fichier pour toute `UnrecoverableKeyException` de cause inconnue ou temporaire ; supprimer seulement si la clé est confirmée absente, définitivement invalidée ou si l'authentification GCM échoue. Ces pertes définitives retournent une absence de session, sans erreur de stockage qui bouclerait au réessai. Conserver l'exception temporaire comme STORAGE, sans détail secret.

Une invalidation permanente confirmée retire aussi la clé inutilisable avant de terminer la restauration, afin que la prochaine association puisse créer une nouvelle clé et enregistrer puis relire son jeton. Si cette suppression de clé échoue, conserver le fichier et retourner STORAGE : le nettoyage pourra être réessayé, sans annoncer une absence de session avant sa réussite. Une simple erreur GCM ne supprime pas la clé.

Alternative rejetée : tester seulement un faux `TrackingSessionStore` ne couvre pas le défaut d'effacement. Aucune interprétation du texte d'une exception Android ne sert de preuve.

### D2 — Restauration et réessai

Ajouter à `TrackingState` un état de session distinguant aucune session, restauration et jeton présent. Il décrit la disponibilité de la session, pas une seconde copie du compte. La restauration termine explicitement son état quand aucun fichier n'existe, conserve l'état réessayable lors d'une panne et passe à jeton présent avant `members/infos`. Une action commune de réessai appelle restore si le jeton n'est pas en mémoire, sinon refresh ; les protections de génération et le délai au retour au premier plan restent existants.

Alternative rejetée : persister l'identité pour masquer une panne ne réparerait ni le réessai ni la suppression du jeton.

### D3 — Refus API

L'API continue d'exploiter les codes structurés déjà reconnus et le renouvellement 1001 de configuration. Un 401/403 dépourvu d'une preuve structurée d'expiration est NETWORK, y compris non JSON ; il ne devient pas ACCESS par son seul statut. Les budgets du transport restent inchangés, et les prochains rafraîchissements restent autorisés. Un refus explicite continue de demander une association volontaire.

### D4 — Présentation

Réutiliser le contrôle `tracking-sync` pour chargement, réessai et reconnexion, y compris avant identité. Garder une ligne utilisable pour le focus pendant le chargement ; le contrôleur empêche le double envoi. Les réglages suivent les composants existants, sans nouvelle surface. Le message de stockage est traduit dans les cinq langues et explique la conservation de session. L'accueil ouvre le détail d'erreur sans exiger `account != null` si une restauration est en échec.

## Risks / Trade-offs

- Une erreur de clé permanente sans cause structurée reste conservée plutôt qu'effacée par hypothèse ; le réessai reste visible et la déconnexion explicite reste disponible.
- Un serveur qui refuse réellement un jeton sans erreur structurée expose une panne réessayable plutôt qu'une expiration présumée ; cela évite une association imposée sur une panne de protection HTTP.
- L'incident matériel exact n'est pas reproduit par les tests JVM ; une validation finale sur TV reste nécessaire et sera rapportée distinctement.

## Migration Plan

Réutiliser le format AES-GCM et l'alias existants, sans réécriture au démarrage. Une mise à jour conserve les sessions encore présentes. Un jeton déjà supprimé par l'ancienne version ne peut pas être récupéré. Retour arrière possible sans migration de données.
