## ADDED Requirements

### Requirement: Couverture de la conservation de session
La restauration BetaSeries SHALL être testée avec stockage chiffré, fournisseur de clés, transport et horloge contrôlés, puis en Compose à la taille TV. Les assertions SHALL distinguer absence de session, erreur temporaire et refus explicite, et compter les écritures, révocations et associations.

#### Scenario: panne du stockage sécurisé
- **WHEN** le fournisseur de clés retourne une erreur temporaire ou une exception de récupération de cause inconnue, puis redevient disponible
- **THEN** le fichier chiffré reste présent et le même jeton est relu ; une clé confirmée absente ou un déchiffrement définitivement invalide terminent au contraire la restauration sans boucle

#### Scenario: panne d'identité et reprise
- **WHEN** le premier chargement d'identité échoue, puis Réessayer est activé ou le launcher revient au premier plan après le délai prévu
- **THEN** le même jeton charge le compte et ses contenus sans écriture de remplacement, révocation ni association ; les réponses d'une ancienne session restent ignorées

#### Scenario: refus générique et refus explicite
- **WHEN** le transport renvoie 401/403 sans erreur BetaSeries de jeton, avec corps JSON ou non JSON, puis un refus explicite de jeton
- **THEN** seuls les derniers refus imposent la reconnexion ; une panne générique conserve les contenus déjà chargés et autorise leur rafraîchissement

#### Scenario: réessai dans les réglages
- **WHEN** le test Compose TV ouvre les réglages pendant la restauration, fait échouer l'identité puis presse OK sur Réessayer
- **THEN** le contrôle stable `tracking-sync` conserve le focus, `tracking-connect` et le code/QR sont absents ; après reprise le compte est affiché ; Retour et la navigation suivent settings

#### Scenario: indicateur sans identité
- **WHEN** une session ne peut être restaurée avant identification du compte
- **THEN** le parcours de détail depuis l'engrenage reste utilisable selon launcher-shell, même sans carte BetaSeries, et son réessai ne produit pas d'association
